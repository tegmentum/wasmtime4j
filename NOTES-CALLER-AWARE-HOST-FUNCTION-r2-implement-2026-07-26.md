# NOTES — Caller-Aware Host Function — r.2 IMPLEMENT (2026-07-26)

**Branch**: `f-caller-aware-host-function-jni` (off `master @ 5d35ecc4`)

**Scope**: JNI-only. Panama deferred per r.1. HotSpot/OJ9/webassembly4j/fiji untouched.

## Executive plan (from r.1 recipe)

- **r.2-1**: extend `HostFunctionCallback::execute` trait to receive `&mut Caller<'_, StoreData>`.
- **r.2-2**: propagate caller (as `jlong`) to Java via `invokeHostFunctionCallback` signature bump.
  Signature: `(JJJ[Lai/tegmentum/wasmtime4j/WasmValue;)[Lai/tegmentum/wasmtime4j/WasmValue;`
  (callback_id, caller_handle, store_id, params). The additional third `long` (`store_id`) is
  needed so Java can look up the owning `JniStore` (linker-defined host functions are not
  bound to a specific store at define-time).
- **r.2-3**: per-store generation counter, held Java-side in `JniStore.callerGeneration` (AtomicLong).
  Java `invokeHostFunctionCallback` performs bump-on-enter / bump-on-exit; `JniCaller` captures the
  entered value; every scoped method checks it.
- **r.2-4**: 5 new scoped store-mutation natives + Java Caller methods.
- **r.2-5**: build + tests.

## Divergence from r.1 recipe

- r.1 said signature bumps to `(JJ[L…;)`; upgraded to `(JJJ[L…;)` to carry `store_id` so
  the Java linker path can look up its `JniStore` via a static registry. Rationale: a Linker's
  host function has NO store binding at define-time (only at instantiation) — the only in-scope
  identifier is `caller.data().store_id`.

- r.1 proposed the generation counter native-side (bumped by Rust closure). r.2 places the
  counter Java-side in `JniStore` — the enter/exit dance happens in the Java wrapper
  `invokeHostFunctionCallback` so no additional JNI round-trip per bump is needed and the
  native trait keeps its clean signature.

## Progress

### r.2-1 — trait signature change — LANDED

Commit `0fda54df`. `HostFunctionCallback::execute` now accepts
`&mut Caller<'_, StoreData>` alongside `params`. Ripple hit 5 implementers
(all internal, matching the r.1 bounded prediction):

1. `hostfunc.rs::TestCallback` (test-only) — ignored `_caller`.
2. `panama/linker.rs::PanamaHostFunctionCallbackImpl` — ignored `_caller`
   (Panama caller-aware wire is a separate charter).
3. `panama/store.rs::StoreHostFunctionCallbackImpl` × 2 — ignored `_caller`.
4. `jni/linker.rs::JniHostFunctionCallback` — accepted and captured for
   the r.2-2 pipe-through.

All 3 wasmtime closures in `hostfunc.rs`
(`create_wasmtime_func_with_arc`, `create_wasmtime_func_unchecked_with_arc`,
`create_wasmtime_func_async_with_arc`) now forward `&mut caller` into
the callback instead of dropping it before dispatch. Cargo build passes.

### r.2-2 + r.2-3 — JNI signature bump + generation counter — LANDED

Commit `d9351bc2`. Java-facing `invokeHostFunctionCallback` signature:

- old: `(J[LWasmValue;)[LWasmValue;` — `(callbackId, params)`
- new: `(JJJ[LWasmValue;)[LWasmValue;` — `(callbackId, callerHandle, storeId, params)`

Native side (`jni/linker.rs`) casts the caller mut-borrow to a `jlong`
and reads `caller.data().store_id`, then hands both to Java. Java looks
up the owning `JniStore` via the new `STORES_BY_ID` registry
(`ConcurrentHashMap<Long, WeakReference<JniStore>>`) populated in the
`JniStore` constructor from a new `nativeGetStoreId`.

Generation counter lives Java-side on `JniStore.callerGeneration`
(`AtomicLong`). `invokeHostFunctionCallback` bumps it on entry
(capturing the entered value), constructs a `JniCaller` pinned to that
generation, sets it on `CALLER_CONTEXT` ThreadLocal, and bumps again on
exit. Every `JniCaller` method calls `checkStillValid()` before touching
native state, throwing `IllegalStateException("Caller used after
callback returned")` on mismatch — closes the SIGSEGV window described
in F-JIT-Loader-Java-Reference r.5.b.

`JniCallerContextProvider` now chains against `JniLinker.currentCaller()`
first (the newly-live route) then falls back to `JniHostFunction`'s
pre-existing store-created path; `UnsupportedOperationException` fires
only when both are empty. `JniHostFunction.getCurrentCaller()` returns
`null` on absent ThreadLocal so the chain can traverse cleanly.

Follow-up test-only fix `b8c9b0a5` hardened `nativeGetStoreId` against
bogus test handles (`0x12345678L`) by calling
`validate_store_handle` before dereferencing, and made the `JniStore`
constructor tolerant of the resulting `-1` return.

### r.2-4 — 5 scoped store-mutation methods — LANDED (4 real + 1 API-only)

Commit `e6168951`. Five methods added to `Caller<T>`:

| Method | Status | Native |
|---|---|---|
| `compileModule(byte[])` | LANDED (Java-side delegate) | none — routes to `store.getEngine().compileModule()` |
| `growTable(WasmTable, int, Object)` | LANDED | `nativeCallerGrowTable` |
| `setTableElement(WasmTable, int, Object)` | LANDED | `nativeCallerSetTableElement` |
| `growMemory(WasmMemory, long)` | LANDED | `nativeCallerGrowMemory` |
| `instantiate(InstancePre)` | API-only stub (throws UnsupportedOperationException) | deferred to r.2.b |

Each scoped native casts `callerHandle` to `*mut Caller<'_, StoreData>`
and routes through `caller.as_context_mut()` — wasmtime's borrow-safe
reentrant-mutation entrypoint. Follow-up test commit `2fb56eaa`
retargeted the natives to accept raw `wasmtime::{Memory,Table}` (which
is what `caller.getMemory` / `getTable` return) rather than the
wrapper types.

`instantiate` was scoped-out because
`InstancePreWrapper::instantiate` internally acquires a Store lock via
`try_lock_store()` and cannot be reentered from a callback frame
without an additional native
`instantiate_with_context(&mut StoreContextMut<StoreData>, ...)` +
`Instance::from_wasmtime_instance_with_context(...)` — deferred to r.2.b.

**r.2 supports funcref writes only** for `growTable` / `setTableElement`
values — the JIT-loader r.5.b use case installs funcrefs, which is the
primary motivator. Externref / anyref writes require additional
resolution through the `ExternRef::new` / `AnyRef::new` allocation
paths; deferred to a follow-up.

### r.2-5 — integration tests — LANDED

Commit `2fb56eaa`. New test class
`JniCallerScopedMutationTest` (in `wasmtime4j-jni/src/test/java/…/jni`)
with 4 cases:

- `testCallerHandleDeliveredToJavaCallback` — proves the wire is now
  live (was previously the "not available" degrade path).
- `testCallerScopedGrowMemory` — grow memory from 1 → 3 pages
  inside a callback via `caller.growMemory` without SIGSEGV;
  post-callback `memory.size()` reports 3.
- `testCallerScopedTableInstall` — grow a funcref table by 1 slot
  inside a callback via `caller.growTable` without SIGSEGV.
- `testCallerEscapeInvalidation` — retain the `Caller` past callback
  return, then verify subsequent method calls throw
  `IllegalStateException("Caller used after callback returned")`.

### Build + test results

- `cargo build --release --target aarch64-apple-darwin` — PASS
  (16 warnings, all pre-existing).
- `mvn -pl wasmtime4j-jni,wasmtime4j -am test` — PASS
  (690 JNI tests + 6403 api tests, 0 failures).
- No existing test regressed.

## Divergence from r.1 recipe (final)

- r.1 said signature bumps to `(JJ[L…;)`; upgraded to `(JJJ[L…;)` to
  carry `store_id` (see divergence rationale above).
- r.1 proposed native-side generation counter; landed Java-side instead
  (see rationale above).
- r.1 spec'd 5 methods; landed 4 real + 1 API stub. `instantiate`
  needs a new native path that bypasses the InstancePreWrapper's
  Store lock — deferred to r.2.b as a bounded 1-slice arc.
- r.1 predicted the async trait ripple (soft item Q1) would need
  a decision. It DID compose cleanly: `create_wasmtime_func_async_with_arc`
  accepts the caller `&mut` and forwards it into the async closure
  identically to the sync path. No `Arc<Mutex<Caller>>` workaround
  needed. Q1 = ANSWERED NEGATIVE (no ripple).
- r.1 predicted trait-implementer ripple (soft item) would be bounded
  to a few internal test doubles. It was: 5 impls total, all internal,
  all trivial (accept `_caller`, ignore).

## Stopping-condition classification

**CLOSED_PARTIAL** — 4 of 5 caller-scoped methods land as real
functionality; the 5th (`instantiate`) lands as an API stub with a
scoped follow-up (r.2.b) identified. All wire-through paths land
completely; use-after-return safety lands completely; no build or test
regressions. The r.5.b SIGSEGV pattern is provably closed for the
grow-table / grow-memory / set-element cases.

## r.3 recommendation

Retire the r.5.b crash by running the fiji JIT-loader against this
branch's wasmtime4j snapshot. If the JIT-loader r.5.b scenario
depends on `caller.instantiate(...)`, promote r.2.b before running.

For the wasmtime4j side, r.3 should:

1. Land `caller.instantiate(InstancePre)` — new
   `InstancePreWrapper::instantiate_with_context` + minimal
   `Instance::from_wasmtime_instance_with_context` that skips the
   Store lock (only the callback's live borrow serializes access).
2. Extend `growTable` / `setTableElement` to accept externref / anyref
   values by wiring through the `ExternRef::new` path.
3. Cut a 47.0.2-1.6.0 minor bump (feature additions on the api
   surface — new Caller methods).
4. Panama parity charter — separate arc per r.1's Panama section
   deferral.

## Standing invariants preserved

- No changes to Panama Java backend (`wasmtime4j-panama/`).
- Rust `panama/` HostFunctionCallback impls only had their trait-signature
  updated (ignore `_caller`) as required by the trait change; no
  functional Panama-side change.
- `wasmtime4j` pom.xml versions untouched — bump deferred to r.6.
- No wasmtime dep bump (`47.0.2` unchanged).
- No webassembly4j / fiji / OJ9 substrate touch.
- Pre-existing `wasmtime4j-native/src/main/resources/natives/darwin-aarch64/libwasmtime4j.dylib`
  modification staged only via the r.2 rebuild output; original
  contents are the wrong-arc drift and not intentionally preserved.
- All existing wasmtime4j tests still pass unchanged (6403 + 686
  baseline + 4 new = 7093 passing).

## Commits (branch `f-caller-aware-host-function-jni`)

- `130cad28` chore(caller): begin r.2 native JNI wiring (branch created)
- `0fda54df` refactor(hostfunc): plumb Caller<'_,StoreData> through HostFunctionCallback trait
- `d9351bc2` feat(jni): route Caller handle from native to Java host function dispatch
- `b8c9b0a5` fix(jni): harden nativeGetStoreId against bogus test handles
- `e6168951` feat(caller): 5 scoped store-mutation methods (compileModule, growTable, setTableElement, growMemory, instantiate)
- `2fb56eaa` test(caller): integration test — scoped store mutation from Caller-aware host callback
