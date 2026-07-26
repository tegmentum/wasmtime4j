# NOTES — Caller-Aware Host Function — r.1 RECON (2026-07-26)

Static reconnaissance for the fijivm-coordinated charter
`F-Wasmtime4j-Caller-Aware-Host-Function` (spec at
`/Users/zacharywhitley/git/fijivm/doctrine/specs/f-wasmtime4j-caller-aware-host-function-charter-2026-07-26.md`).

**Repo tip at recon**: `95789b27` (master, ahead of origin by 1). No code changes in r.1.

**Version reality**: repo is at `47.0.2-1.5.1` (Cargo + Maven agreed via
`wasmtime-version.properties`), NOT the charter's assumed `46.0.1-1.3.0`.
Post-close bump target is `47.0.2-1.6.0` (minor — feature additions),
supersedes the charter's `1.4.0` recommendation.

---

## Executive summary — the charter's premise needs to be re-scoped

**Reconnaissance overturns the charter's implicit "the interfaces don't
exist yet" premise.** wasmtime4j **already ships** a full Java-side
Caller-aware surface — but the JNI dispatch does not propagate the
wasmtime `Caller<'_, StoreData>` from the Rust closure to Java, and the
`CallerContextProvider` SPI path silently degrades to
`UnsupportedOperationException` (which the existing integration tests
CATCH and swallow with "caller not available" log lines).

- `ai.tegmentum.wasmtime4j.func.Caller<T>` — EXISTS at api layer, richer
  than the charter's proposal (adds fuel, gc, engine, epoch, debug
  frames on top of getExport/getMemory/getTable/etc.).
- `ai.tegmentum.wasmtime4j.func.HostFunction.CallerAwareHostFunction<T>`
  — EXISTS as a NESTED CLASS (not a top-level interface) in
  `HostFunction.java`. Factories `voidFunctionWithCaller`,
  `singleValueWithCaller`, `multiValueWithCaller` all present.
- `ai.tegmentum.wasmtime4j.spi.CallerContextProvider` — EXISTS.
- `JniCallerContextProvider` + `JniCaller<T>` (JNI impl) — EXIST.
- `PanamaCallerContextProvider` + `PanamaCaller<T>` (Panama impl) —
  EXIST.

**What is broken**: the JNI callback bridge
(`native → JniLinker.invokeHostFunctionCallback → Java HostFunction`)
does NOT carry a caller handle. The static Java entry point
`JniHostFunction.hostFunctionCallback(long id, long callerHandle, ...)`
that DOES set the `CALLER_CONTEXT` ThreadLocal appears to be dead code
— no matching native call site exists in the current codebase.

**Corollary**: the charter's `Linker.defineCallerAwareHostFunction(...)`
addition is not needed. `Linker.defineHostFunction(...)` already accepts
any `HostFunction`, including `CallerAwareHostFunction` (it implements
`HostFunction`). The dispatch layer just needs to WIRE the caller
through.

**r.2 real scope**: fix the JNI dispatch to propagate the wasmtime
`Caller<'_, StoreData>` all the way to Java, then add a
`Caller.getStore()` (or scoped store-mutation methods) so the JIT-loader
r.5.b reentrant-mutation pattern becomes safe. Panama has the same wire
break — analogous fix but symmetric.

---

## r.1-1 — native JNI thunk architecture (Rust side)

**Layer stack**, top to bottom:

1. `wasmtime4j-jni/…/JniLinker.defineHostFunction(module, name, type,
   impl)` — creates a `HostFunctionWrapper`, assigns a callback id,
   stores in `HOST_FUNCTION_CALLBACKS` `AtomicReferenceArray<Wrapper>`,
   then calls `native nativeDefineHostFunction(linkerHandle, module,
   name, paramTypes, returnTypes, callbackId)`.
   (`wasmtime4j-jni/src/main/java/ai/tegmentum/wasmtime4j/jni/JniLinker.java:664-670,
   715-754`.)

2. Rust
   `Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineHostFunction`
   (`wasmtime4j-native/src/jni/linker.rs:1092-1203`) — builds a
   `JniHostFunctionCallback { jvm, callback_id, is_function_reference:
   false }`, wraps it in a `crate::hostfunc::HostFunction`, and calls
   `linker.define_host_function(module, name, func_type, host_func)`.

3. `crate::hostfunc::HostFunction::create_wasmtime_func_with_arc`
   (`wasmtime4j-native/src/hostfunc.rs:313-375`) builds the actual
   wasmtime `Func::new(store, func_type, move |mut caller, params,
   results| { … })`. **This closure OWNS the `Caller<'_,
   StoreData>` for the callback's entire scope** — real wasmtime type.
   The closure:
   - calls `marshal_params_from_wasmtime(params, store_id, &caller)`
     (uses `&caller` for ExternRef marshalling, doesn't propagate);
   - if `requires_caller` is set (default true, per line 263), calls
     `create_optimized_caller_context(&mut caller, usage)` and
     **immediately discards** the result into `_context` (line 350) —
     effectively a no-op today;
   - calls `host_function.callback.execute(&wasm_params)` — WHICH IS
     WHERE THE `Caller<'_, StoreData>` IS DROPPED FROM THE PIPELINE.

4. `impl HostFunctionCallback for JniHostFunctionCallback` in
   `wasmtime4j-native/src/jni/linker.rs:79-194`. Signature: `fn
   execute(&self, params: &[WasmValue]) -> WasmtimeResult<Vec<WasmValue>>`
   — **NO CALLER PARAMETER**. Attaches to the JVM, marshals params to a
   `WasmValue[]`, calls Java static
   `JniLinker.invokeHostFunctionCallback(callback_id, params) →
   WasmValue[]` (JNI signature `(J[LWasmValue;)[LWasmValue;`), unpacks
   the result.

5. Java `JniLinker.invokeHostFunctionCallback(long callbackId,
   WasmValue[] params)`
   (`wasmtime4j-jni/src/…/JniLinker.java:715-754`) — looks up the
   `HostFunctionWrapper` and calls
   `wrapper.getImplementation().execute(params)`. If the implementation
   is a `HostFunction.CallerAwareHostFunction`, its `execute(params)`
   calls the inner `getCurrentCaller()` which uses `ServiceLoader` to
   find `JniCallerContextProvider`, which in turn reads
   `JniHostFunction.CALLER_CONTEXT` ThreadLocal —
   **which is null in this path, so `UnsupportedOperationException` is
   thrown**.

6. The `CALLER_CONTEXT` ThreadLocal is only written by
   `JniHostFunction.hostFunctionCallback(long id, long callerHandle,
   byte[] paramsData, byte[] resultsBuffer)`
   (`wasmtime4j-jni/src/…/JniHostFunction.java:420-490`). Grep across
   `wasmtime4j-native/` finds NO native call site that dispatches to
   this Java method. It is orphaned code from an earlier design.

**JNI-rs crate**: `jni = ...` (workspace dep in `Cargo.toml`;
wasmtime4j uses jni-rs, not raw `extern "C"`). Callbacks marshal via
`env.call_static_method(class, name, sig, args)`.

**State-across-callback storage**:
- Rust side: `HOST_FUNCTION_REGISTRY` `HashMap<u64, Arc<HostFunction>>`
  and `NEXT_HOST_FUNCTION_ID` counter
  (`wasmtime4j-native/src/hostfunc.rs:132-139`).
- Java side (JniLinker):
  `HOST_FUNCTION_CALLBACKS` `AtomicReferenceArray<HostFunctionWrapper>`
  and `NEXT_HOST_FUNCTION_ID` (`JniLinker.java:52-56, 1228-1266,
  1267-1300`).
- Java side (JniHostFunction — separate registry for store-created
  host-fns via `store.createHostFunction`): analogous array-based
  registry (`JniHostFunction.java:79-85`).
- `StoreData` (native): `store_id: u64` + `epoch_interruption_enabled:
  bool` etc.; no per-callback registry stored inside it in the current
  layout.

**Why the plain (non-CallerAware) path avoids crashes for the NORMAL
case**: the Rust closure holds the wasmtime `Caller<'_>` for the
callback's whole duration, but the Java callback body simply computes
its return value from its input params and does NOT touch the store.
The mutable borrow held by the closure isn't aliased. The r.5.b crash
is EXACTLY the anti-pattern — Java code holding a `JniStore` reference
outside the callback and calling store-mutating methods on it during
the callback. Wasmtime's `Store` sees two active mutation paths on the
same underlying data — the current callback's `Caller<'_>` and the
Java-driven `Store::instantiate/…` call — and corrupts state at the
next reentry (result marshaling calls `store_untyped_results`, which
touches now-corrupted `StoreData`, resulting in SIGSEGV at
`libwasmtime4j.dylib+0x126334`).

---

## r.1-2 — wasmtime `Caller<'_, T>` capability audit

Wasmtime crate pinned at `47.0.2` (workspace root `Cargo.toml:10`).
`wasmtime::Caller` is the primary caller-context type; features enabled
include `async`, `component-model`, `gc`, `call-hook`, `debug`, etc.
The wasmtime4j-native `caller.rs` module already exercises the type
extensively (`wasmtime4j-native/src/caller.rs:1-209`), giving a proof
inventory of what's available for `T = StoreData`:

- `Caller::data()` / `Caller::data_mut()` — read/mutate the store's
  associated data object. Already used by
  `caller_has_epoch_deadline` (line 78) and
  `caller_debug_exit_frames` (line 174).
- `Caller::get_fuel()` / `Caller::set_fuel()` — fuel metering.
  `caller.rs:14-57`.
- `Caller::get_export(name) -> Option<Extern>` — look up caller's
  exports. `caller.rs:83-89`.
- `Caller::as_context_mut()` — DEREF to `StoreContextMut<T>`. This is
  the reentrant-mutation gateway:
  - `caller.rs:67` uses it for `set_epoch_deadline`;
  - `caller.rs:161-163` uses it for `fuel_async_yield_interval`;
  - `caller.rs:176-193` uses it for
    `debug_exit_frames`/`wasm_function_index_and_pc`/`num_locals`/`num_stacks`.
- Not currently used here but confirmed by wasmtime 47 docs:
  `Instance::new(store: impl AsContextMut, module, imports)`,
  `Table::grow(&mut StoreContextMut, delta, init)`, `Memory::grow`,
  `Global::set`, `Module::new(engine, bytes)` (`Module` compilation is
  engine-scoped, doesn't need Store — so it works freely inside a
  callback). All these accept `impl AsContextMut<Data = T>` which
  `Caller<'_, T>` provides via deref.

**Reentrant limits documented by wasmtime**: none for the operations
above IF you go through `Caller::as_context_mut()` — the borrow-checker
enforces the aliasing rule at compile time. The r.5.b crash is
precisely what happens when JNI code bypasses this by holding an
independent Store handle and mutating it while a callback frame is
live.

**Conclusion**: Rust wasmtime 47.0.2 offers everything we need. The
gap is purely the JNI wire-up + a Java-side scoped-store surface.

---

## r.1-3 — Java interface design (finalize signatures)

**Recommendation: keep the existing `Caller<T>` interface unchanged.
Add a new scoped-store method group for reentrant mutation.** The
charter's proposed `defineCallerAwareHostFunction(...)` addition is
NOT needed — `Linker.defineHostFunction(String, String, FunctionType,
HostFunction)` already accepts any `HostFunction`, and
`HostFunction.CallerAwareHostFunction<T>` implements `HostFunction`.
Consumers use factories `HostFunction.singleValueWithCaller(...)`,
`voidFunctionWithCaller(...)`, `multiValueWithCaller(...)` (all present
today).

### 3.a — additions to `ai.tegmentum.wasmtime4j.func.Caller<T>`

The charter's `Caller.getStore()` proposal is UNSAFE for the JNI path
— a `Store` reference outside the callback scope would be a
use-after-return handle. Instead, expose scoped-store methods that
INTERNALLY use the caller-borrowed path (Rust
`Caller::as_context_mut()`) — a strictly-narrower API that closes the
r.5.b gap:

```java
public interface Caller<T> {
    // existing: data(), getExport(...), getFunction(...), getMemory(...),
    //           getTable(...), getGlobal(...), hasExport(...),
    //           fuelRemaining(), addFuel(...), setFuel(...), engine(),
    //           gc(), setFuelAsyncYieldInterval(...), debugExitFrames()

    // NEW — reentrant scoped-store mutation. All operations must go
    // through the caller's native handle; any use after callback return
    // throws IllegalStateException.

    /** Compile a Module inside the callback's borrow scope. */
    Module compileModule(byte[] wasmBytes) throws WasmException;

    /** Instantiate a module against the caller's store using the given
        linker/imports; the resulting Instance lives in the caller's store. */
    Instance instantiate(Module module, LinkingContext imports)
        throws WasmException;

    /** Grow a table exported by the caller by delta, filling new slots
        with `initFunc` (funcref) or null (externref/anyref). */
    int growTable(WasmTable table, int delta, Object init)
        throws WasmException;

    /** Set an element in a caller-owned table. */
    void setTableElement(WasmTable table, int index, Object value)
        throws WasmException;

    /** Grow a memory exported by the caller by delta pages. */
    long growMemory(WasmMemory memory, long deltaPages)
        throws WasmException;
}
```

Rationale for a scoped-method API rather than `getStore()`:

- Every scoped method has a natural native down-call that receives BOTH
  the caller-handle AND the target object (table/memory/module); Rust
  can implement each via `caller.as_context_mut()`, satisfying wasmtime's
  borrow rules by construction.
- A returned `Store` handle would need synthetic lifetime enforcement in
  every existing `Store` method — a much larger surface to make
  scope-safe.
- Matches the wasmtime Rust idiom: users don't reach for
  `Caller::as_context_mut()` casually; they use methods on the store
  handle they're given.

### 3.b — `CallerAwareHostFunction` promotion

Charter proposes a top-level `CallerAwareHostFunction<T>` interface.
Existing code has `HostFunction.CallerAwareHostFunction<T>` as a nested
class. **Recommend leaving the nested class as-is** — the factories
already provide the ergonomic surface. If a top-level interface is
desired for API clarity, we can add:

```java
package ai.tegmentum.wasmtime4j.func;

@FunctionalInterface
public interface CallerAwareHostFunction<T> {
    WasmValue[] execute(Caller<T> caller, WasmValue[] params)
        throws WasmException;
}
```

…as an alias for `HostFunction.MultiValueHostFunctionWithCaller<T>`.
It's cosmetic — the wire-up is what matters.

### 3.c — `Linker` — NO NEW METHOD REQUIRED

`Linker.defineHostFunction(module, name, type, HostFunction impl)`
already accepts caller-aware implementations. Charter's proposed
`defineCallerAwareHostFunction(...)` is redundant.

---

## r.1-4 — use-after-return safety design

**Recommendation: Option 1 (generation counter) — per-store, incremented
on every callback entry and exit.**

### Options considered

| Option | Mechanism | Overhead | Complexity | Assessment |
|---|---|---|---|---|
| 1. Generation counter | JniCaller captures a per-store `long gen` at construction; every method checks `store.currentGen() == this.gen`; else throw. Native side bumps gen on callback enter/exit. | Atomic read + comparison per method | Low | **CHOSEN** |
| 2. ThreadLocal invalidation | Native clears a per-thread ThreadLocal on callback exit; every method checks it | Cheap read | Medium (ThreadLocal weirdness across nested callbacks) | Fragile under nested/async |
| 3. `AutoCloseable` + explicit close | Wrapper closes JniCaller on return | Zero when closed | Medium | Awkward — callbacks return values, not through try-with-resources |
| 4. `AtomicBoolean valid` flag on JniCaller | Flip false on callback exit; every method checks | Volatile read | Low | Similar to Option 1 but per-caller instead of per-store; loses "invalidate all callers on outermost return" property |

### Why Option 1 wins

- Handles NESTED callbacks correctly. If wasm A calls host-fn α (which
  reenters wasm B via `caller.instantiate` + call), and B calls host-fn
  β, generations stack: β's JniCaller has gen=N+1, α's has gen=N.
  When β returns, gen resets to N — α's JniCaller is still valid.
  When α returns, gen goes to N+2 (or arbitrary next value) —
  further use of either caller after their respective callback returns
  fails cleanly.
- Atomic long compare-and-check is cheaper than a ThreadLocal
  `get()` on modern JVMs.
- Trivial to reason about — one counter per store, monotonic.

### Concrete mechanism

- `JniStore` gains `private final AtomicLong callerGeneration = new AtomicLong(0);`
- Native JNI dispatch (r.2): on callback ENTRY, bump gen and remember
  the entered value; on callback EXIT, bump gen again (post-increment
  ensures the entered-value never re-appears while any earlier caller
  might still be alive).
- `JniCaller.<init>` captures `this.gen = store.currentGen();`
- Every JniCaller method: `if (store.currentGen() != this.gen) throw
  new IllegalStateException("Caller used after callback returned");`
- The `callerHandle` (native `*mut WasmtimeCaller<'_, StoreData>`)
  becomes immaterial for safety — the generation check gates every
  dereference, so a stale pointer is never followed.

### Non-negotiable

The current `JniCaller` stores `callerHandle: jlong` as a raw pointer
with NO lifetime enforcement (`JniCaller.java:53-76`). ANY escape of a
JniCaller reference from a callback + any subsequent method invocation
= native use-after-return = potential SIGSEGV. The generation counter
CLOSES this class of bug.

### Doctrine to bank at close

`doctrine-jni-callback-scoped-caller-handle-generation-counter-2026-07-26`
— when a JNI-side handle wraps a Rust `&mut` with lifetime `'callback`,
enforce validity via a monotonic generation counter incremented on
callback enter/exit. Store the entered generation on the wrapper;
compare-on-use; throw `IllegalStateException` on mismatch. Never trust
the raw pointer's non-zeroness as validity.

---

## r.1-5 — Panama backend recon

**Panama backend has parallel infrastructure with an equivalent break
and an extra structural error.**

- `PanamaCaller<T>` — EXISTS
  (`wasmtime4j-panama/src/main/java/ai/tegmentum/wasmtime4j/panama/PanamaCaller.java:45`).
- `PanamaCallerContextProvider` — EXISTS + registered via
  `META-INF/services/ai.tegmentum.wasmtime4j.spi.CallerContextProvider`
  (`wasmtime4j-panama/src/main/resources/…`).
- `PanamaHostFunction.nativeCallback(long hostFunctionId, Object... nativeParams)`
  (`PanamaHostFunction.java:900-964`) is the FFM upcall entry. When the
  implementation is `CallerAwareHostFunction`, it constructs a
  `PanamaCaller` with `store.getNativeStore().address()` as the "caller
  handle" (`PanamaHostFunction.java:924-926`), sets the `CALLER_CONTEXT`
  ThreadLocal, dispatches, clears on return.

**Structural bug** (independent of r.5.b): the Panama "caller handle"
is the STORE address, not a wasmtime `Caller<'_, StoreData>` reference.
Semantically this is a HANDLE FOR THE STORE, not for the caller frame.
Operations like `PanamaCaller.getMemory("memory")` currently succeed
only because the store-scoped native functions happen to accept a Store
handle; NONE of the reentrant-mutation calls we'd add would be safe if
the "handle" is a raw Store pointer with no borrow-frame anchor.

**Rust-side asymmetry**: the JNI hostfunc callback path exposes the
wasmtime `Caller<'_, StoreData>` inside a Rust closure and drops it
before calling Java. The Panama path REGISTERS a wasmtime-invoked
callback via wasm-c-api-style native bindings — need to verify (r.5-b
recon) whether wasm-c-api gives us `wasmtime_caller_t*` or drops it
before the FFM upcall. If wasm-c-api drops it, Panama parity may
require a different mechanism (e.g., a native-side thread-local
holding the `Caller<'_>` for the duration of the FFM upcall).

**Scope recommendation**: **defer Panama fix to a follow-up charter
(`F-Wasmtime4j-Caller-Aware-Host-Function-Panama`)**. The empirical
trigger (Fiji JIT-loader) uses the JNI backend. JNI-only closure of
this charter unblocks r.5.b immediately. Panama parity is an
additional 2-3 slice arc that doesn't help Fiji today and has an
open sub-question (wasm-c-api caller propagation).

---

## r.1-6 — gap analysis + r.2 preview

### The concrete gap

1. **JNI Rust closure drops the `Caller<'_, StoreData>` before dispatching to Java.**
   Fix location: `wasmtime4j-native/src/hostfunc.rs:322-372`
   (`create_wasmtime_func_with_arc`) and
   `wasmtime4j-native/src/jni/linker.rs:79-194`
   (`impl HostFunctionCallback for JniHostFunctionCallback`).

2. **Java-side JNI callback receives no caller.**
   Fix location: `wasmtime4j-jni/…/JniLinker.java:715-754`
   (`invokeHostFunctionCallback`).

3. **`CallerContextProvider` ThreadLocal path is broken.** The linker
   dispatch never sets `JniHostFunction.CALLER_CONTEXT`. Either
   consolidate on ONE ThreadLocal (move it to `JniLinker` or a shared
   holder) or bypass the SPI entirely by passing the caller as an
   argument.

4. **`JniCaller` has no reentrant-mutation methods** and no
   use-after-return safety.

5. **Panama has the same wire break plus a structural handle mismatch**
   (defers).

### r.2 concrete recipe (native Rust — JNI-only)

**Step 2.a — Extend `HostFunctionCallback` trait to accept the caller
handle.** Change signature from
`fn execute(&self, params: &[WasmValue]) -> WasmtimeResult<Vec<WasmValue>>`
to
`fn execute(&self, caller: &mut Caller<'_, StoreData>, params: &[WasmValue]) -> WasmtimeResult<Vec<WasmValue>>`.
Ripple effect: every implementer must be updated; only
`JniHostFunctionCallback` (linker.rs) and any test/dummy impls.

**Step 2.b — Propagate caller to Java in `JniHostFunctionCallback::execute`.**
Change the Java method call from
```rust
env.call_static_method(
    linker_class, "invokeHostFunctionCallback",
    "(J[Lai/tegmentum/wasmtime4j/WasmValue;)[Lai/tegmentum/wasmtime4j/WasmValue;",
    &[Long(callback_id), Object(&java_params)],
)
```
to include a caller handle:
```rust
let caller_handle = caller as *mut Caller<'_, StoreData> as jlong;
env.call_static_method(
    linker_class, "invokeHostFunctionCallback",
    "(JJ[Lai/tegmentum/wasmtime4j/WasmValue;)[Lai/tegmentum/wasmtime4j/WasmValue;",
    &[Long(callback_id), Long(caller_handle), Object(&java_params)],
)
```

**Step 2.c — Generation-counter enter/exit hooks.** Wrap the callback
body in the closure at `create_wasmtime_func_with_arc` with paired
`bump_caller_generation(store_id)` calls (both branches — entry and
exit + drop path). Store the counter in `StoreData` (`store.rs`), which
already lives in `Caller::data_mut()`.

**Step 2.d — Add native functions for reentrant-mutation methods.**
Six new `#[no_mangle] extern "system" fn Java_…_JniCaller_native*`
functions in `wasmtime4j-native/src/jni/caller.rs`:
- `nativeCompileModule(callerHandle, wasmBytes) -> long` — engine-scoped,
  no `Caller` needed for compile itself but returns Module bound to the
  caller's engine.
- `nativeInstantiate(callerHandle, moduleHandle, importsMarshal) -> long`
  — uses `caller.as_context_mut()`.
- `nativeGrowTable(callerHandle, tableHandle, delta, initFuncref) -> int`.
- `nativeSetTableElement(callerHandle, tableHandle, index, value) -> void`.
- `nativeGrowMemory(callerHandle, memoryHandle, deltaPages) -> long`.

Each function follows the existing pattern in
`wasmtime4j-native/src/jni/caller.rs:14-263`
(cast `callerHandle as *mut WasmtimeCaller<'_, StoreData>`, deref
inside `unsafe`, call the wasmtime API through `caller.as_context_mut()`).

### r.2 open sub-questions

- **Q1 — Trait signature ripple**: does the wasmtime `Func::new_async`
  path in `hostfunc.rs:475-527` compose with a caller-passing trait? The
  async future closure borrows `caller` differently; may need a
  separate trait method or `Arc<Mutex<Caller>>` workaround. Verify in
  r.2 before wiring.
- **Q2 — Component-model callbacks**: `component_core.rs` may have its
  own callback trait. Out-of-scope per charter ("Component-model callers
  are a distinct wasmtime type; separate charter").
- **Q3 — Native-side generation counter storage**: `StoreData` is the
  most natural home, but any store-scoped access from
  `caller.data_mut()` requires the T param to be `StoreData`. It is
  today (all wasmtime4j stores use `Store<StoreData>`), so this is
  fine.

### r.3 (Java side) recipe

- Update `JniLinker.invokeHostFunctionCallback` signature to
  `(long callbackId, long callerHandle, WasmValue[] params)`.
- Construct `JniCaller<>(callerHandle, store, generation)` inside the
  method, put it into a ThreadLocal (or pass it into the dispatch as
  the new pathway if consolidating on argument-passing).
- Add the five reentrant-mutation methods to `Caller<T>` interface;
  implement in `JniCaller` calling the new natives.
- (Optional) add top-level `CallerAwareHostFunction<T>` interface as an
  alias.
- Update `JniStore` with `AtomicLong callerGeneration` + native
  `nativeCurrentCallerGeneration` if needed for cross-callback checks.

### r.4 tests

- `CallerReentrantModuleInstallTest` — mirrors r.5.b's crashing
  scenario: outer wasm invokes host-fn, host-fn compiles + instantiates
  an inner module using shared caller memory/table + grows the caller's
  table by 1 slot to install the inner's exported `main` funcref, returns
  its slot index; wasm calls the newly-installed funcref via
  `call_indirect`. Should PASS (currently crashes).
- `CallerUseAfterReturnTest` — save a `Caller<Void>` reference from a
  callback, use it AFTER return, assert
  `IllegalStateException("Caller used after callback returned")`.
- Existing `CallerMemoryAccessTest` / `CallerFuelTrackingTest` /
  `CallerEpochAndAdvancedAccessTest` — the JNI branch should now
  ACTUALLY exercise the caller path (remove or invert the
  `isCallerUnavailable` swallow).

### r.5 Panama

Deferred to `F-Wasmtime4j-Caller-Aware-Host-Function-Panama` charter.
Blocker sub-question: does wasm-c-api give us
`wasmtime_caller_t*` in the FFM upcall path, or do we need a
native-side ThreadLocal holding the wasmtime `Caller<'_, StoreData>`?

### r.6 close + version bump

`47.0.2-1.5.1 → 47.0.2-1.6.0` (feature-adding, additive on Java + JNI
signature change on the internal Rust trait — the JNI ABI to Java also
changes: `invokeHostFunctionCallback` signature bumps from `(J[L…;)`
to `(JJ[L…;)`. This is an INTERNAL contract; existing consumer code
(other host functions, WASI, etc.) doesn't call it, but any downstream
mock or fake would need to update). Consider `47.0.2-2.0.0` if that
internal-ABI change is deemed significant enough for a minor bump per
CHANGELOG convention.

### Blockers found

None hard. Two soft items to resolve inside r.2:

- **Q1** (async trait ripple) — must be answered before landing.
- **Trait-implementer ripple** — every `HostFunctionCallback` implementer
  needs a signature update; recon shows only `JniHostFunctionCallback`
  and internal test doubles, so this is bounded.

---

## Standing invariants preserved (r.1 recon)

- No code changes to wasmtime4j (r.1 is static reading only).
- No `mvn` invocations.
- No `cargo build`.
- No branch creation. Commits go to `master` (charter said "main"; repo
  actual branch is `master`).
- No push.
- Working-tree dylib change (`wasmtime4j-native/src/main/resources/…/libwasmtime4j.dylib`)
  is a pre-existing modification unrelated to this recon; not touched.
- webassembly4j: untouched.
- fiji: untouched.
- OJ9 substrate `1e7c5205e5`: untouched.
- HotSpot: untouched. FROZEN WITs preserved. matrix v7 unchanged.

## Commits

- (this file, initial draft): `docs(caller): r.1 recon draft (in progress)`
- (progressive fills): commits per section as land.
- (close): `docs(caller): r.1 recon CLOSED_SUCCEEDED_RECON — dispatch wire + scoped-store methods`
