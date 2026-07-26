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

None so far. `AsContextMut` composes cleanly from `Caller`. `build_instance_data`
is context-free — the redundant lock is pure dead weight in the callback path.
