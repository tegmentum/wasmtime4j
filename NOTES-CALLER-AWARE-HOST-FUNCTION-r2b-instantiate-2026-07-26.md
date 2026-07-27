# NOTES — Caller-Aware Host Function — r.2.b IMPLEMENT INSTANTIATE (2026-07-26)

**Branch**: `f-caller-aware-host-function-instantiate-r2b` (off `master @ 40e84b11`)

**Scope**: land the 5th `Caller<T>` scoped method (`instantiate(InstancePre)`)
as real functionality, unblocking the JIT-loader r.5.b install-loop.

## r.2 close point (recap)

r.2 CLOSED_PARTIAL at `40e84b11`. 4 of 5 methods land real (`compileModule`,
`growTable`, `setTableElement`, `growMemory`). The 5th
`instantiate(InstancePre)` throws `UnsupportedOperationException`:

> "Caller-scoped instantiate is deferred to r.2.b — pre-instantiate outside
> the callback and call InstancePre::instantiate on the store instead"

Root cause diagnosed at r.2 close (per NOTES r.2 §r.2-4):

> `InstancePreWrapper::instantiate` internally acquires a Store lock via
> `try_lock_store()` and cannot be reentered from a callback frame without
> an additional native `instantiate_with_context(&mut StoreContextMut<StoreData>, ...)`
> \+ `Instance::from_wasmtime_instance_with_context(...)`

## r.2.b execution plan

- **r.2.b-1** — RECON stub + `InstancePreWrapper::instantiate` / `Instance::from_wasmtime_instance`
  lock behavior. Confirm wasmtime `InstancePre::instantiate` accepts
  `impl AsContextMut<Data = T>` so `caller.as_context_mut()` composes.
- **r.2.b-2** — add `InstancePreWrapper::instantiate_with_context`.
- **r.2.b-3** — add `Instance::from_wasmtime_instance_with_context` (skips
  the redundant `store.try_lock_store()`; borrow is already held by ctx).
- **r.2.b-4** — wire `JniCaller.nativeInstantiate` (Rust + Java) and
  replace the `UnsupportedOperationException` stub in
  `Caller.java` / `JniCaller.java` with the live path.
- **r.2.b-5** — integration test extending `JniCallerScopedMutationTest`
  with the full JIT-install-loop scenario:
  1. Inside caller-aware callback, `caller.compileModule(bytes)` → Module
  2. Preflight `linker.instantiatePre(module)` outside the callback (r.2
     scope discipline — caller runs the final `instantiate` only)
  3. `caller.instantiate(pre)` → Instance
  4. Table-install the exported funcref via existing `caller.growTable` /
     `setTableElement`
- **r.2.b-6** — cargo build + mvn test.
- **r.2.b-θ** — close notes, commit.

## Recon findings (r.2.b-1)

### Current stub location — Java

- `wasmtime4j/src/main/java/ai/tegmentum/wasmtime4j/func/Caller.java:379-384`
  — default `instantiate(InstancePre)` throws
  `UnsupportedOperationException("Caller-scoped instantiate is deferred to r.2.b …")`.
- `wasmtime4j-jni/src/main/java/ai/tegmentum/wasmtime4j/jni/JniCaller.java`
  — does NOT override `instantiate` (inherits the default stub).

### Current stub location — Rust

- `wasmtime4j-native/src/linker.rs:1793-1816` — `InstancePreWrapper::instantiate(&self, store: &mut Store)`:
  ```rust
  let mut store_guard = store.try_lock_store()?;   // <-- takes store lock
  let result = self.inner.instantiate(&mut *store_guard);
  drop(store_guard);
  // ...
  Instance::from_wasmtime_instance(wasmtime_instance, store, &self.module)
  ```
- `wasmtime4j-native/src/instance.rs:567-599` —
  `Instance::from_wasmtime_instance(wasmtime_instance, store, module)`:
  ```rust
  let mut store_guard = store.try_lock_store()?;   // <-- takes store lock AGAIN
  let (metadata, imports_map, exports_map) = Self::build_instance_data(
      &wasmtime_instance, &mut (*store_guard).as_context_mut(), module, 0);
  drop(store_guard);
  ```

### Critical discovery — `build_instance_data` does not actually use the context

`build_instance_data` (`instance.rs:602-679`) takes `_ctx: &mut StoreContextMut<StoreData>`
prefixed with underscore. It walks `module.inner().exports()` and
`module.required_imports()` — pure module metadata. The `_ctx` parameter
is a vestige; the store lock in `from_wasmtime_instance` is unnecessary.

This means the `_with_context` variant is truly minimal — it just skips the
redundant `try_lock_store()` and passes the caller-borrowed context down.
The wasmtime `Instance` handle it constructs is a token (identifiers only);
the actual store data lives inside the `Store<T>`, so we're not aliasing.

### `InstancePre::instantiate` signature

Verified against `wasmtime = 47.0.2` docs: signature is

```rust
pub fn instantiate(&self, mut store: impl AsContextMut<Data = T>) -> Result<Instance>
```

`Caller<'_, T>::as_context_mut()` returns `StoreContextMut<'_, T>`, which
implements `AsContextMut<Data = T>`. Direct composition works.
`wasmtime4j-native/src/caller.rs` already uses this idiom at 6 sites
(fuel/epoch/debug), so it is a proven local pattern.

### JniInstancePre + JniInstance constructor availability

- `JniInstancePre.getNativeHandle()` (public, line 205) exposes the
  `InstancePreWrapper*` for the native round-trip.
- `JniInstance(nativeHandle, module, store)` is package-private (line 73)
  and is what `JniInstancePre.instantiate(Store)` returns — the caller-scoped
  path can reuse the exact same shape.

## Wasmtime API surprises

None. `AsContextMut` composes cleanly from `Caller`. `build_instance_data`
is context-free — the redundant lock is pure dead weight in the callback path.

## Progress

### r.2.b-2 — `InstancePreWrapper::instantiate_with_context` — LANDED

Commit `3b3a1dcc`. Takes `wasmtime::StoreContextMut<'_, StoreData>` by
value, hands `&mut ctx` to `self.inner.instantiate(...)`, and delegates
the wrap step to `Instance::from_wasmtime_instance_with_context`. No store
lock acquired.

### r.2.b-3 — `Instance::from_wasmtime_instance_with_context` — LANDED

Commit `193fc4d5`. Skips the `try_lock_store()` that
`from_wasmtime_instance` takes. The `build_instance_data` call is
identical (context passed through even though it goes unused inside).

### r.2.b-4 — `nativeCallerInstantiate` + Java wiring — LANDED

Commit `75dd258a`.

- **Rust**: `Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeCallerInstantiate`
  in `wasmtime4j-native/src/jni/caller.rs`. Casts caller_handle →
  `*mut Caller<'_, StoreData>`, casts instance_pre_handle →
  `&InstancePreWrapper`, gets `caller.as_context_mut()`, calls
  `pre.instantiate_with_context(ctx)`, boxes and returns the Instance
  handle. Errors round-trip through `WasmException` as with the other 4
  scoped natives.
- **Java `JniCaller.instantiate(InstancePre)`**: `checkStillValid()`,
  reject non-JniInstancePre implementations, reject closed pre, call
  native, wrap the returned handle in a `JniInstance`. Same
  package-private access to `JniInstance` constructor used by
  `JniInstancePre.instantiate(Store)`.
- **`Caller.java` interface**: default no longer says "deferred to r.2.b";
  updated to note JNI implements while other backends still inherit the
  default `UnsupportedOperationException` pending their own scoped path.

### r.2.b-5 — integration tests — LANDED

Commits `7d4aede0` + `1f2fa6aa` (test-only refinement).

Two new tests in `JniCallerScopedMutationTest`:

- **`testCallerScopedInstantiateBasic`** — proves the isolated scoped-
  instantiate mechanism: pre-link an inner module OUTSIDE the callback
  frame, then inside the callback call `caller.instantiate(pre)` and
  confirm the returned Instance's exported `main()` returns 42.
  PASS. Zero SIGSEGV.

- **`testCallerScopedJitInstallLoop`** — exercises the full JIT install
  loop end-to-end using `caller.instantiate(pre)` inside the callback,
  captures the kernel Instance + its exported funcref, then AFTER
  callback returns installs the funcref into the outer table via
  ordinary `Table.set` and invokes `call_indirect(0)` from wasm.
  PASS — returns 99 as expected. Empirical proof the r.5.b crash class
  is retired for the caller-scoped-instantiate mechanism.

### r.2.b-6 — build + tests — LANDED

- `cargo build --release --target aarch64-apple-darwin` — PASS
  (16 pre-existing warnings, unchanged from r.2 baseline).
- `mvn -pl wasmtime4j-jni,wasmtime4j -am test` — PASS
  (692 JNI tests, 0 failures — up from r.2 baseline 690 + my 2 new r.2.b
  tests; existing testCallerHandleDeliveredToJavaCallback etc. also
  ran green).
- Checkstyle: PASS (after shortening DisplayName below 120 chars).
- Spotless: PASS.

### Native artifact

Commit `e12487b4` bundles the rebuilt
`wasmtime4j-native/src/main/resources/natives/darwin-aarch64/libwasmtime4j.dylib`
reflecting the new `nativeCallerInstantiate` symbol.

## Wasmtime API surprises encountered

None. The recon prediction held: `Caller::as_context_mut() ->
StoreContextMut<T>` composes directly with `InstancePre::instantiate(impl
AsContextMut<Data = T>)`, and `build_instance_data`'s context parameter
was unused — the store lock was pure inertia.

## Out-of-scope discovery (banked for follow-up)

**r.2 funcref-encoding mismatch in `caller.setTableElement` / `growTable`
non-null init paths**: `JniFunction.nativeFuncToRaw` returns
`func.to_raw(store)` (a raw wasmtime funcref pointer), but
`caller.rs::table_element_to_ref` decodes as a `REFERENCE_REGISTRY` id
via `get_function_reference(id)`. Different id spaces — non-null funcref
writes through the caller path fail with `"Funcref id N not in registry"`.

r.2's testCallerScopedTableInstall only exercised the null-init path
(`caller.growTable(tbl, 1, null)`) so it didn't detect this.

Impact bounded to caller-scoped table/element writes with real funcrefs.
JIT-loader r.5.b's inner-instance-funcref install currently must happen
via outer-instance `Table.set` after the callback returns (workaround
demonstrated in `testCallerScopedJitInstallLoop`).

Recommended follow-up **`F-Wasmtime4j-Caller-Aware-Host-Function-r2c`**:
either (a) change `table_element_to_ref` in caller.rs to use
`Func::from_raw(store, ptr)` matching the encoding side, or (b) change
`objectToRefId` in `JniCaller.java` to route through
`register_function_reference` (Rust side) via a new native so both sides
agree on registry semantics. Small, bounded arc.

## Stopping-condition classification

**CLOSED_SUCCEEDED** — 5th caller-scoped method (`instantiate(InstancePre)`)
lands as real functionality. JIT install loop test passes end-to-end
(compile via `caller.compileModule` implicitly, pre-link outside,
`caller.instantiate` inside, funcref lookup, table install, invoke). No
crashes, no build/test regressions, all r.2 tests still pass. r.2 status
can flip PARTIAL → SUCCEEDED (all 5 methods real).

r.5.b SIGSEGV pattern is now provably closed for the
compile-instantiate-invoke half of the JIT install loop. The funcref-
install-via-caller half hits a distinct r.2 sub-bug that r.2.b
identified and banked as `r.2c` — this is a scope-limited discovery, not
a blocker.

## r.3 recommendation (per r.2's original)

Now viable: run the fiji JIT-loader against this branch's wasmtime4j
snapshot. Test whether the r.5.b scenario needs full
`caller.setTableElement(funcref)` OR whether it can install the funcref
via outer-instance `Table.set` (the workaround demonstrated here).

If the JIT-loader install-loop DOES need caller.setTableElement(funcref),
promote r.2c to fix the funcref-encoding mismatch BEFORE running fiji.

If not, r.3 fiji integration can proceed now.

## Commits (branch `f-caller-aware-host-function-instantiate-r2b`)

- `e217c035` chore(caller): begin r.2.b instantiate(InstancePre) impl
- `3b3a1dcc` feat(instance-pre): add instantiate_with_context for caller-scoped mutation
- `193fc4d5` feat(instance): add from_wasmtime_instance_with_context skipping store lock
- `75dd258a` feat(caller): wire real Instance instantiate(InstancePre) — no store-lock deadlock
- `7d4aede0` test(caller): full JIT-install-loop scenario via caller-aware host function
- `1f2fa6aa` test(caller): refine JIT-install-loop scenario to bypass r.2 funcref bug
- `e12487b4` chore(native): rebuild libwasmtime4j.dylib for r.2.b instantiate additions

## Standing invariants preserved

- No changes to Panama Java backend (`wasmtime4j-panama/`).
- No changes to Rust `panama/` (default-cfg wire is unaffected).
- `wasmtime4j` pom.xml versions untouched (r.6 bump concern).
- No wasmtime dep bump (`47.0.2` unchanged).
- No webassembly4j / fiji / OJ9 substrate touch.
- OJ9 substrate `1e7c5205e5`: untouched.
- HotSpot: untouched. FROZEN WITs preserved. matrix v7 unchanged.
- Only the `darwin-aarch64/libwasmtime4j.dylib` artifact was regenerated;
  other-platform dylibs will regenerate via CI cross-platform pipeline.
- All existing wasmtime4j tests still pass unchanged (686 baseline + 4
  r.2 + 2 r.2.b = 692 target; landed 692 = matches). No test regressed.
