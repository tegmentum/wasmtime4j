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

(section fills as work lands)
