# Component-invoke marshalling: where the ~4.7 us goes, and the lever

Investigation + design + prototype for the per-call cost of crossing the
component boundary for `render(u64, string, string, string) -> string`
(the Svalinn log4j witness signature). Svalinn's own benchmark measured
~4.7 us/event; this reproduces that on wasmtime4j's own harness, decomposes
it, and prototypes the highest-value optimization.

Branch: `perf/component-marshalling` (git worktree). Native lib rebuilt in the
worktree (`wasmtime4j-native/.cargo-target/release/libwasmtime4j.dylib`).

## TL;DR

- The Java-side marshalling churn (creating `ComponentVal`/`WitValue`, UTF-8
  encoding, the `MarshalledValue` double-`clone()`, building `int[]`+`byte[][]`,
  decoding the return `String`) is **~3% of the cost (~130 ns)**. It is *not* the
  lever, despite being the most visible allocation churn.
- **~96% of the cost is inside the single native JNI call**
  `nativeComponentInvokeFunction`. Within it, the dominant cost was **building
  the Java reply object** `Object[]{Integer, byte[]}` (~1.58 us, 35% of total) —
  almost entirely `find_class` + constructor-id resolution done *on every call*.
- Prototype: cache those JNI class/method references process-globally.
  Result: **TOTAL 4.85 us -> 3.91 us (-19%)** from a ~40-line, behavior-preserving
  change. The reply phase alone dropped **-53%** (1.58 us -> 0.74 us).
- The actual WebAssembly execution (`Func::call`, i.e. lowering the 3 strings
  into guest linear memory, running the guest, lifting the result string out) is
  only **~1.0 us (22%)** and is largely intrinsic to the component ABI.

## How this was measured

- Micro-benchmark: `wasmtime4j-benchmarks/.../jni/MarshallDecompBench.java`
  (standalone `main`, manual warmup, min-of-5 trials, 1e6 iters/trial). Drives
  the real JNI path (`JniComponent.nativeComponentInvokeFunction`) against the
  Svalinn logging-core component (exports exactly
  `render(u64,string,string,string)->string`).
  It isolates four layers:
  - `TOTAL` — Java marshal + native invoke + Java unmarshal (mirrors
    `JniComponentFunc.call`).
  - `NATIVE_ONLY` — native invoke with pre-marshalled args (no Java marshal/unmarshal).
  - `JAVA_MARSHAL` — Java arg-marshal + return-unmarshal, no native call.
  - `PROBE` — `nativeComponentInstanceHasFunc` = JNI crossing + instances
    write-lock + HashMap lookup + `get_func`-by-name. A floor for "cross into
    Rust and find the instance/func, do nothing else".
- Native intra-call decomposition: env-gated phase timers
  (`W4J_PROFILE=1`) added to `nativeComponentInvokeFunction`
  (module `invoke_profile` in `wasmtime4j-native/src/jni/component.rs`). Zero
  cost when the env var is unset (one relaxed atomic load per call). Diagnostic
  instrumentation — kept for reproducibility, not a product feature.

Machine: Apple aarch64, JDK 25, wasmtime 46.0.1, release build.

## Where the ~4.7 us goes

Layer decomposition (ns/op):

| layer | ns/op | note |
|---|---:|---|
| TOTAL (full round trip) | 4848 | matches Svalinn's ~4.7 us |
| NATIVE_ONLY (invoke only) | 4539 | **~96% of TOTAL is the native call** |
| JAVA_MARSHAL (Java only) | 132 | arg marshal + return unmarshal = ~3% |
| PROBE (JNI + lock + lookup) | 190 | crossing/lock/lookup floor |

Native intra-call phases (`W4J_PROFILE`, ns/call), **before** optimization:

| phase | ns/call | % of native | what it is |
|---|---:|---:|---|
| setup | 638 | 14% | write-lock instances, `get_string(func_name)`, copy `int[]` discriminators |
| argdeser | 1137 | 25% | per-arg `get_object_array_element` + `convert_byte_array` (JNI) + `deserialize_to_val` (incl. string `to_string`), x4 |
| getfunc | 30 | <1% | `Instance::get_func` by name — cheap |
| ty | 40 | <1% | `func.ty().results().len()` + results `Vec` alloc — cheap |
| call | 1011 | 22% | `Func::call`: component ABI lower/lift + guest execution |
| **reply** | **1581** | **35%** | build `Object[]{Integer, byte[]}`: `find_class` x2, `Integer(int)` ctor, `new_object_array`, `byte_array_from_slice` |

Two things stand out and both contradict the naive "the Java marshalling is the
problem" framing:

1. **`get_func`-by-name and `func.ty()` are NOT the cost** (70 ns combined). The
   earlier suspicion that per-call export resolution dominates is wrong here.
2. **`reply` (building the Java return object) was the single biggest phase** and
   is almost pure JNI *lookup* overhead: `find_class("java/lang/Object")`,
   `find_class("java/lang/Integer")`, and the `Integer(int)` constructor-id
   resolution were re-done on *every* call. These handles never change for the
   life of the JVM.

## The optimization (prototype, production-plausible)

Cache the reply-construction JNI references once, process-globally, and reuse:

- `java/lang/Object` class (array element class) as a `GlobalRef`
- `java/lang/Integer` class as a `GlobalRef`
- the `Integer(int)` constructor `JMethodID`

Then build the reply with `new_object_unchecked` + a `JClass` reconstructed from
the cached global ref, instead of `find_class` x2 + name/sig-based `new_object`.

Code: `wasmtime4j-native/src/jni/component.rs`, module `reply_cache` +
the reply section of `nativeComponentInvokeFunction`. ~40 lines. `GlobalRef` and
`JMethodID` are `Send + Sync` (the jni 0.21 crate declares the latter safe to
cache), stored in a `OnceLock`.

### Before / after

| metric (ns) | before | after | delta |
|---|---:|---:|---:|
| reply phase | 1581 | 739 | **-53%** |
| NATIVE_ONLY | 4539 | 3710 | -18% |
| **TOTAL** | **4848** | **3908** | **-19%** |
| JAVA_MARSHAL (control) | 132 | 133 | ~0 (unchanged, as expected) |
| PROBE (control) | 190 | 190 | ~0 (unchanged) |

Phases after optimization: setup 626, argdeser 1132, getfunc 30, ty 42,
call 1002, reply **739**. Only `reply` moved — the other phases are within noise,
confirming the win is isolated to the cached references. The residual 739 ns of
`reply` is genuine allocation/copy (`new_object_array`, the `Integer` and
`byte[]` allocations, `serialize_from_val`), not lookups.

### Correctness

- `render(...)` returns the correct string across 1e7+ calls (string reply,
  discriminator 6).
- `add(5,7) -> 12` over 1e5 calls against `add.wasm` (s32 reply, discriminator 2)
  — exercises the same `reply_cache` path with a different type. PASS.
- The change is value-type-agnostic (it only touches how the `Object[]`/`Integer`
  wrapper is built), so both the string and scalar reply shapes are covered.

## What a full production version would entail

The prototype is the reply-reference cache, which is itself shippable. To take
the *whole* path to production and squeeze the rest:

1. **Ship the reply-reference cache** (this change). Low risk; handles are
   JVM-stable. Consider initializing them in `JNI_OnLoad` rather than lazily, and
   holding them in a small process-global JNI-refs struct alongside any other
   hot classes.
2. **argdeser (1.13 us, next biggest reducible):** pass all arguments in a single
   flat `byte[]` (length-prefixed records) instead of a `byte[][]`. That replaces
   4x`get_object_array_element` + 4x`convert_byte_array` with one
   `convert_byte_array` + in-place slicing, and removes the per-arg `Vec<u8>`
   copies. Requires a coordinated Java-side change in `JniComponentFunc` /
   `WitValueMarshaller` (and drops the two defensive `data.clone()` copies in
   `MarshalledValue`, which are pure waste on the call path). Estimated a few
   hundred ns.
3. **reply residual (0.74 us):** flatten the return so no `Integer` box and no
   `Object[]` are needed — e.g. return the discriminator as `byte[0]` of a single
   `byte[]`, or provide a typed fast-path (`...InvokeReturningString`) that hands
   back a `jstring` directly and skips the length-prefixed `byte[]` entirely for
   the common string-return case. Java-side ABI change.
4. **setup (0.63 us):** the per-call `env.get_string(function_name)` and the
   `int[]` discriminator copy are avoidable if the caller passes a cached
   `ComponentExportIndex`/func handle (the codebase already has
   `nativeComponentInstanceHasFuncByIndex` and export-index plumbing). A
   pre-resolved per-`ComponentFunc` handle removes the name marshalling and the
   name-based `get_func` from the hot path. The instances **write** lock could
   also be a read lock or a per-instance lock to stop serializing all calls.
5. **call (1.0 us, intrinsic):** the component-model ABI cost of lowering 3
   strings into guest memory + guest exec + lifting the result. Only reducible by
   moving off the untyped `Val` path to a specialized `TypedFunc<...>`
   (compile-time-known signature, no `Val` boxing/dynamic type checks), which is a
   substantial API addition. This is the real floor for a string-heavy call and
   is the honest justification for Svalinn's "keep args scalar, do work host-side"
   guidance: strings across the boundary cost ~1 us of ABI work *plus* the JNI
   marshalling on both ends.

### Honest assessment

- Prototype-quality but shippable: the `reply_cache` change. It is the correct
  fix for the #1 cost and is self-contained.
- Prototype-only / diagnostic: the `W4J_PROFILE` phase timers (guarded, but
  product code would gate this behind a feature or remove it) and the
  `MarshallDecompBench` harness (a plain `main`, not wired into the JMH suite).
- Negative result worth stating: the Java-layer allocation churn everyone points
  at (double `clone()`, `ByteBuffer` double-buffering, boxing) is real but is only
  ~3% of the per-call cost. Optimizing it in isolation would not have moved the
  Svalinn number meaningfully. The lever was on the native reply path.
