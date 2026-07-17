# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Version format: `{wasmtime-version}-{wasmtime4j-version}`

## [46.0.1-1.4.3] - 2026-07-16

Wasmtime version unchanged (46.0.1). Build-only fix for the `-P wasi-nn`
Maven profile.

### Fixed

- **`-P wasi-nn` no longer suppresses the classifier-less main artifact.**
  The wasi-nn profile in `wasmtime4j-native/pom.xml` previously overrode
  the `default-jar` maven-jar-plugin execution to attach a `wasi-nn`
  classifier. That override removed the classifier-less main artifact
  entirely, so `mvn install -P wasi-nn` produced only classified jars and
  downstream tools that require a main artifact — notably
  `stardog-admin server plugin install`, which errors with "no main
  artifact" and hangs the classifier resolution — could not consume the
  install. The only workaround was to hand-copy the classifier jars from
  `target/` into `~/.m2/repository/ai/tegmentum/wasmtime4j-native/…/`
  plus a flattened pom.

  Fix: the wasi-nn profile no longer touches the `default-jar` execution.
  It now adds a NEW `wasi-nn-jar` execution alongside it. The base
  `default-jar` keeps producing the classifier-less main artifact under
  `-P wasi-nn` (packaged with the wasi-nn-linked native library, since
  the wasi-nn feature flag is what cargo built), and the new
  `wasi-nn-jar` execution produces the `-wasi-nn`-classified variant
  alongside. Per-platform executions still merge the profile's
  `wasi-nn-<platform>` classifier override onto the base
  `<platform>` executions and are unchanged by this fix.

  Verify with:
  ```
  mvn install -P wasi-nn -pl wasmtime4j-native -am -DskipTests
  ls ~/.m2/repository/ai/tegmentum/wasmtime4j-native/46.0.1-1.4.3/
  ```
  Both `wasmtime4j-native-46.0.1-1.4.3.jar` and
  `wasmtime4j-native-46.0.1-1.4.3-wasi-nn.jar` should be present, and
  `install-plugin` should succeed without any manual m2 copy.

## [46.0.1-1.4.1] - 2026-07-16

Wasmtime version unchanged (46.0.1). Patch release that finishes the two
wasi-nn follow-ups called out in 1.4.0:

  1. The `WasiNnConfig#namedModels` map is now wired end-to-end through
     JNI — registered `(name, bytes)` pairs are decoded once against the
     compiled backend and become resolvable via `wasi:nn/graph.load-by-
     name(name)` in the guest, without re-decoding on every store.

  2. A `wasi-nn` Maven profile ships wasi-nn-featured artifacts under a
     `wasi-nn` classifier so consumers can add wasi:nn to their coordinate
     without a local ORT build.

### Added

- `ComponentLinker::enable_wasi_nn_with_models(Vec<(String, Vec<u8>)>)`
  (Rust) and matching JNI export
  `Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeEnableWasiNnWithModels(long,
  String[], byte[][])`. Wired into
  `JniComponentLinker#enableWasiNn(WasiNnConfig)`; the plain
  `enableWasiNn()` path is unchanged (still calls `nativeEnableWasiNn`).
- `NamedGraphRegistry` in `wasmtime4j-native` — a minimal `GraphRegistry`
  impl over `HashMap<String, Graph>`, needed because
  `wasmtime_wasi_nn::InMemoryRegistry` only exposes `load(backend, path)`
  and can't accept pre-decoded graphs from bytes. Registry entries
  clone-on-instantiate (`Graph(Arc<dyn BackendGraph>)`) so per-store cost
  is one pointer bump per model, not a re-decode.
- `ComponentInstancePreWrapper::wasi_nn_named_models` — carries the
  loaded models forward so pre-instantiated components see the same
  named-registry results.

### Packaging

- **New `wasi-nn` Maven profile in `wasmtime4j-native`.** Activating
  `-P wasi-nn` builds the native lib with `--features default,wasi-nn`
  and packages every JAR under a `wasi-nn` classifier
  (`wasmtime4j-native-46.0.1-1.4.1-wasi-nn.jar`,
  `-wasi-nn-darwin-aarch64.jar`, etc.). Consumers depend on the
  classified variant to get wasi:nn; the plain classifier-less
  coordinate stays feature-parity with 1.4.0.
- **Runtime prerequisite for wasi-nn consumers.** The wasi-nn classified
  artifact links against `wasmtime-wasi-nn` + `ort =2.0.0-rc.10` and
  requires `libonnxruntime.{so,dylib,dll}` on the loader path at run
  time. macOS: `brew install onnxruntime` places it under
  `/opt/homebrew/lib`. Linux: install `libonnxruntime` from the
  distribution package manager or ONNX Runtime GitHub releases.
- **Publishing (wired into CI).** `.github/workflows/release.yml` now
  builds and publishes the `wasi-nn` classifier variant alongside the
  plain artifacts on every `v*` tag:
    * New `build-native-wasi-nn` job (parallel to `build-native`, matrix
      over linux-x86_64, linux-aarch64, darwin-aarch64) installs ONNX
      Runtime 1.20.1 — Homebrew on macOS
      (`ORT_LIB_LOCATION=/opt/homebrew`), the Microsoft-published
      prebuilt tarball on Linux (`onnxruntime-linux-{x64,aarch64}-1.20.1`
      extracted to `$HOME/ort`, `ORT_LIB_LOCATION=$HOME/ort`) — and runs
      `cargo build --release --features wasi-nn` under
      `ORT_STRATEGY=system ORT_PREFER_DYNAMIC_LINK=1`. Windows is
      intentionally omitted; ort's windows-x86_64 story via
      `ORT_STRATEGY=system` is fragile and adding it can be a follow-up.
      `fail-fast: false` so a wasi-nn build failure on one platform
      doesn't sink the others.
    * New `publish-wasi-nn` job runs after `publish`, overlays the
      wasi-nn-linked natives into `wasmtime4j-native/src/main/resources/
      natives/<platform>/`, and runs `mvn deploy -P
      release,skip-native,wasi-nn -pl wasmtime4j-native -am` against
      both Maven Central and GitHub Packages. Only the
      `wasmtime4j-native` coordinate varies by wasi-nn; the other module
      poms are unchanged so publishing them again would collide.
    * `publish-wasi-nn` is guarded by
      `if: always() && needs.publish.result == 'success'` so a broken
      wasi-nn path never rolls back or blocks the plain release, and the
      classifier JARs are appended to the same GitHub Release via
      `softprops/action-gh-release@v3` (same tag).
- **Runtime prerequisite reminder for consumers.** Even with the
  classifier JAR published, the ONNX Runtime shared library must be on
  the loader path at run time (`libonnxruntime.so` /
  `libonnxruntime.dylib`). macOS: `brew install onnxruntime`. Linux:
  install `libonnxruntime` via distro package or the Microsoft
  onnxruntime GitHub release. See `README.md` (Platform Support).

### Docs / tests

- `WasiNnConfig` javadoc updated — the "reserved for a future release"
  note on `Builder#registerModel` is now the wire-through behaviour.
- `ComponentLinker#enableWasiNn(WasiNnConfig)` interface javadoc
  documents named-model behaviour.
- New JNI unit test `JniComponentLinkerWasiNnTest` — WasiNnConfig
  round-trip (empty config = same code path as `enableWasiNn`;
  non-empty config exercises `nativeEnableWasiNnWithModels`).

## [46.0.1-1.4.0] - 2026-07-16

Wasmtime version unchanged (46.0.1). Minor release that lands WASI-NN
(neural network inference) support on the Component Model linker so
JVM hosts embedding wasmtime4j can run guests that import
`wasi:nn/{graph, tensor, inference, errors}` — closing the gap that
kept JVM-embedded engines (Jena, RDF4J, Stardog plugins) from parity
with the Rust engines already carrying wasi-nn (oxigraph-wf,
qlever-wf @ substrate v0.4).

### Added

- `ComponentLinker#enableWasiNn()` and
  `ComponentLinker#enableWasiNn(WasiNnConfig)` (`ai.tegmentum.wasmtime4j.component`
  and `ai.tegmentum.wasmtime4j.wasi.nn` respectively). Wires
  `wasi:nn/{graph, tensor, inference, errors}` (component-model ABI)
  onto the linker; a fresh `WasiNnCtx` (auto-detected backends, empty
  in-memory registry) is attached to `ComponentStoreData` at each
  instantiation so the generated host bindings can resolve.
  Idempotent; independent of `enableWasiPreview2`.
- `WasiNnConfig` operator-facing config type. Empty/default
  configuration matches the zero-arg overload and yields the
  auto-detected backend set. `WasiNnConfig.Builder#registerModel(String,
  byte[])` is present so future named-model registry support lands
  additively — the current native binding calls `InMemoryRegistry::new()`
  and silently ignores registry entries.
- JNI binding
  `Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeEnableWasiNn`
  and Rust `ComponentLinker::enable_wasi_nn`.
- New `ComponentStoreData::wasi_nn_ctx` field (feature-gated).

### Backend / Cargo

- Workspace `wasmtime-wasi-nn` now pinned to
  `{ default-features = false, features = ["onnx"] }` matching the
  Rust substrate engines (oxigraph-wf, qlever-wf). Default features
  pull `openvino + winml` — dropped so a stock Linux/macOS host with
  ONNX Runtime installed via Homebrew is enough.
- Workspace `ort = "=2.0.0-rc.10"` exact pin. `wasmtime-wasi-nn 46.0.1`
  is source-compatible only with the rc.10 API surface; later ort
  releases (rc.12) break at compile time (`ort::Error` shape change).
- `wasi-nn` remains **opt-in** in `wasmtime4j-native` features
  (not part of the default set). Build with
  `cargo build --features wasi-nn` and the environment
  `ORT_STRATEGY=system ORT_PREFER_DYNAMIC_LINK=1 ORT_LIB_LOCATION=<prefix>`
  (on macOS: `/opt/homebrew` after `brew install onnxruntime`).

### Notes

- The Maven-published `wasmtime4j-native` artifact for 46.0.1-1.4.0
  ships the default feature set (no wasi-nn). Downstream projects
  that need wasi-nn build the native lib themselves and drop the
  resulting `libwasmtime4j.{so,dylib}` alongside the shipped natives.
  A wasi-nn-enabled release channel is a follow-up.

## [46.0.1-1.2.0] - 2026-07-05

Wasmtime version unchanged (46.0.1). This is a wasmtime4j minor release
that lands end-to-end Component Model invocation from Java, per-instance
resource caps (fuel / memory / epoch), a batch of WIT marshalling
fixes surfaced by driving a real component (Fiji JVM) end-to-end, and
a fix for the empty `wasmtime4j-panama` jar shipped to Maven Central.

### Packaging

- **wasmtime4j-panama Maven Central artifact now contains class files.**
  The Publish job previously ran on JDK 21, which activated the
  `panama-unavailable` profile in `wasmtime4j-panama/pom.xml` and
  skipped compilation — the jar shipped to Maven Central and GitHub
  Packages was empty, and `WasmRuntimeFactory.create(PANAMA)` threw
  "runtime type PANAMA is not available and no suitable fallback
  found". Publish now runs on JDK 23 (matches the CI matrix upper
  bound; ≥ 22 is required for the finalized Foreign Function & Memory
  API). Fixes #339. Thanks to @rapus95 (#340) for lowering the profile
  activation range to `[22,)` to match the module's actual JDK 22
  compiler settings.

### Added

- **Component Model invocation, end-to-end.** The component path can now
  drive a real component from Java: composite argument marshalling
  (record / list / option / variant / result / tuple / enum / flags),
  WASI-configured instantiation, interface-nested exports (resolved by
  `iface#func`), and resource handles (`own` = take, `borrow` = peek so
  a borrowed handle survives reuse across calls). Empty lists no longer
  require an explicit element type.
- **`ComponentInstance.invokeWit`** — symmetric typed round-trip variant
  of `invoke()`. Same argument marshalling, but returns the raw
  `WitValue` tree instead of a lossy `toJava()`'d Java-shape value
  (preserves `u64` width, enum discriminants, and option inner types).
  `invoke()` now delegates to `invokeWit` so the two entry points cannot
  drift.
- **Per-instance fuel cap on component invocations.** The shared
  component engine now enables `consume_fuel`; every component store
  gets a fuel budget on creation. `JniComponentLinker.instantiate` reads
  the cap from the supplied store (`getFuel`; `>0` = real cap, `0` /
  non-metered = unlimited) and threads it through to the native side.
  Runaway component code under a fuel cap now traps in milliseconds
  instead of hanging.
- **Per-instance memory and epoch caps on component invocations.**
  Memory is enforced via a wasmtime `StoreLimits` limiter built from the
  configured max. Epoch enforcement enables `epoch_interruption` on the
  shared component engine with a daemon ticker advancing the epoch
  ~1/ms; stores set a relative deadline in those ticks (an unset
  deadline uses a finite sentinel to avoid `current+ticks` overflow
  wrapping to an already-passed deadline). Threaded through a new
  `ComponentLinker.setComponentResourceLimits(maxMemoryBytes, epochDeadline)`
  hook and two new native params. Unbudgeted paths remain unlimited.
- **Per-call re-arm of fuel and epoch budgets.** The invoke path now
  re-applies the configured fuel / epoch budgets immediately before each
  `func.call`, so a `fuel_per_call` / `deadline_ms` reading is
  per-call, not a per-instance-lifetime budget that traps every later
  call once the first one exhausts it. Only the high-level WASI
  instantiate path opts in; the non-WASI and native `ComponentLinker`
  paths keep their instantiation-time behavior.

### Fixed

- **`nativeInstantiateWithLinker` now really instantiates.** It was a
  stub that returned a monotonically incrementing phantom instance ID
  without instantiating the component or registering it in the engine's
  instance map, so the follow-up invoke failed with "Instance ID N not
  found in engine". The JNI call now routes through
  `EnhancedComponentEngine::instantiate_component`, which instantiates,
  creates the store on the component's engine, and inserts the handle
  into the engine's instances map. Adds an `engine_handle` param to the
  JNI signature.
- **`defineFunction` host functions on the WASI instantiation path.**
  When a `WasiPreview2Config` was set, the component was instantiated
  against a fresh Linker with only WASI added, silently dropping every
  host function defined via `defineFunction`. Component imports meant
  to be satisfied by those functions then failed as "not found in the
  linker" even though the function was registered on the
  `ComponentLinker`'s own linker. The full versioned interface path is
  now stored on each `ComponentHostFunctionEntry`, and both instantiate
  paths re-register global-registry host functions onto the linker
  after `add_to_linker_sync`. `defineFunction` + WASI now compose.
- **Variant validation uses structural type compatibility.**
  `WitVariant.validate` compared payload types by strict equals, so a
  record built via `WitRecord.builder()` (which synthesises the generic
  name "record") could never match a variant case whose declared inner
  type had a specific name — even when the field shape matched exactly.
  Now uses `WitType.isCompatibleWith`, which is structural for records,
  variants, enums, flags, lists, and options — matching the equality
  WIT's canonical ABI uses on the wire.
- **Heterogeneous `list<value>` variant cases accepted.**
  `componentValToWitValue` builds a single-case `WitType.variant` per
  value, so a `WitList` inferred its element type from element 0 and
  rejected a differently-cased element 1. The list case now unifies
  every element under one shared variant `WitType` (the union of active
  cases) and re-types each `WitVariant` to it. Unblocks multi-arg
  component calls with mixed cases (e.g. `fiji:jvm invoke-static` of
  `(String, int)`).
- **`WitValueDeserializer.deserializeOption` derives element type from
  the decoded value.** It previously hardcoded the element type to
  `bool` when a value was present, so any `some(non-bool)` payload was
  rejected by `WitOption.validate` with "Option value has type X but
  expected bool". The `none` arm was unaffected, which hid the bug
  until a component returned `some(string)` (WASI probe read-env).

## [46.0.1-1.1.7] - 2026-06-24

### Changed

- **Wasmtime upgraded from 45.0.2 to 46.0.1.** This is a major upstream
  release. The native library was rebuilt against wasmtime 46.0.1 and all 794
  wasmtime behavior tests pass (6 pre-existing skips).
- **Minimum Rust version raised to 1.94.0** (from 1.93.0), as required by
  wasmtime 46.
- The wasmtime cargo feature `component-model-async-bytes` was renamed
  upstream to `component-model-bytes`; the workspace dependency was updated
  accordingly. No effect on the public Java API.

### Fixed

- Adapted to the wasmtime 46 component-type reflection API: `ComponentType`
  import/export iterators now yield a `ComponentExtern` wrapper whose
  `ComponentItem` is accessed via its `ty` field. Internal metadata extraction
  was updated to match.

### Deprecated

- `DebugEvent.UNCAUGHT_EXCEPTION_THROWN` is no longer emitted. Wasmtime 46
  merged the previous caught/uncaught exception debug events into a single
  exception event delivered at throw time (before the handler stack has been
  searched), so the runtime can no longer distinguish the two. The native
  layer now maps that event to `DebugEvent.CAUGHT_EXCEPTION_THROWN`, which
  fires for any thrown exception regardless of whether it is later caught.

## [45.0.2-1.1.6] - 2026-06-22

### Security

- **Wasmtime upgraded from 45.0.1 to 45.0.2** — upstream security patch
  release ([GHSA-3p27-qvp9-27qf](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-3p27-qvp9-27qf)).
  Fixes a file-descriptor leak in the WASIp1 `fd_renumber` implementation,
  which only updated the WASIp1 descriptor table and not the underlying
  host descriptor table. A guest making repeated `fd_renumber` calls could
  exhaust host file descriptors until the `Store` was dropped (low-severity
  resource exhaustion, CVSS 2.3).

No wasmtime4j source changes were required for this upgrade. The native
library was rebuilt against wasmtime 45.0.2.

## [45.0.1-1.1.5] - 2026-06-07

### Changed

- **Wasmtime upgraded from 45.0.0 to 45.0.1** — upstream patch release.
  Fixes a WASIp2 regression where zero-delay clocks/timers would not make
  progress on repeat calls to `.ready()`.

No wasmtime4j source changes were required for this upgrade. The native
library was rebuilt against wasmtime 45.0.1.

## [45.0.0-1.1.4] - 2026-05-22

### Changed

- **Wasmtime upgraded from 44.0.1 to 45.0.0.** Rust MSRV is now 1.93.0 to
  match the new wasmtime requirement. The `wast` (248.0.0), `wasmparser`
  (0.248), and `rand_core` (0.10) dependencies were bumped accordingly.
- `Config::wasm_component_model_async_builtins` was renamed upstream to
  `wasm_component_model_more_async_builtins`; the engine builder now calls
  the new name. No Java-facing API change.
- `Config::compiler_inlining` now takes an `Inlining` enum instead of a
  `bool`. The wrapper maps the existing boolean to `Inlining::Yes` /
  `Inlining::No`, preserving the previous contract.
- `CallbackRng` now implements `rand_core::TryRng<Error = Infallible>`
  instead of the old `RngCore`. rand_core 0.10 (pulled in by wasmtime-wasi
  45 via rand 0.10) inverted its trait hierarchy so that `Rng` is the core
  trait — this is what `WasiCtxBuilder::secure_random` / `insecure_random`
  now require.

### Removed

- Removed the explicit `component::Func::post_return` calls. wasmtime 45
  performs post-return cleanup automatically and the method is now a
  deprecated no-op.

### Fixed

- **`memory.grow` behavior on i32 custom-page-size-1 memories**: wasmtime
  45 resolved
  [WebAssembly/custom-page-sizes#45](https://github.com/WebAssembly/custom-page-sizes/issues/45)
  so that growth past 4 GiB returns -1 again instead of trapping (the trap
  added in wasmtime 44 was a conservative stopgap). The generated
  `MemoryCombosTest` assertions for `grow_m3`, `grow_m4`, and `grow_m8`
  with delta -1 were reverted to expect -1.
- **Hardened native pointer validation**: the `validate_ptr_not_null!` and
  `validate_ptr_not_null_c!` macros now reject obviously-invalid pointers
  (addresses below the first page, or carrying the fake/test-pointer magic
  prefix) in addition to null, reusing the same `is_fake_pointer` heuristic
  as `safe_destroy`. A bogus native handle such as `1` previously passed
  the null check and was then dereferenced, causing a SIGSEGV that aborted
  the JVM; it now produces a recoverable `IllegalArgumentException`.

All 794 wasmtime behavior tests pass (6 pre-existing skips).

## [44.0.1-1.1.3] - 2026-05-05

### Security

- **Wasmtime upgraded from 44.0.0 to 44.0.1** — upstream security patch
  release. Users are encouraged to upgrade.
  - Panic when allocating a table exceeding the size of the host's
    address space
    ([GHSA-p8xm-42r7-89xg](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-p8xm-42r7-89xg))

No wasmtime4j source changes were required for this upgrade. All 794
wasmtime behavior tests pass (6 pre-existing skips).

## [44.0.0-1.1.2] - 2026-04-22

### Changed

- **Wasmtime upgraded from 43.0.1 to 44.0.0.** Rust MSRV is now 1.92.0
  to match the new wasmtime requirement.
- `wasmtime::ModulePC` is now a newtype instead of a raw `u32`. All
  internal debug-frame and breakpoint call sites were updated to use
  `ModulePC::new(pc)` / `pc.raw()`. No Java-facing API change.
- `Linker::get` now returns `Result<Extern>` upstream. The wrapper maps
  the "missing definition" error back to `None` so the Java
  `Optional<Extern>` contract is preserved.
- `EngineConfig.craneliftPcc(boolean)` is retained for source
  compatibility but is now a no-op — wasmtime 44 removed
  `Config::cranelift_pcc` and PCC validation is no longer available
  from the engine.

### Added

- **Component model `map<K, V>` type handling**: wasmtime 44 added
  experimental `Type::Map` / `Val::Map` variants. Reported as
  `ComponentValueType::Type("map")` via the introspection surface;
  serialized as an unsupported-parameter error by the concurrent-call
  JSON codec. Wasmtime4j does not currently enable this experimental
  feature; handlers exist to keep match expressions exhaustive.

### Fixed

- **`memory.grow` behavior on i32 custom-page-size-1 memories**:
  wasmtime 44 now traps instead of returning -1 when such a memory
  would grow past 4 GiB (upstream FIXME tied to
  [WebAssembly/custom-page-sizes#45](https://github.com/WebAssembly/custom-page-sizes/issues/45)).
  The generated `MemoryCombosTest` assertions for `grow_m3`, `grow_m4`,
  and `grow_m8` with delta -1 were updated to expect the trap.
- **`Dependency Updates` workflow**: now builds the native library for
  `linux-x86_64` before running tests, so bumped Maven dependencies are
  actually exercised against the JNI runtime. Previously the workflow
  silently discarded dependency bumps for 10+ consecutive weekly runs
  because `JniWasmRuntime` could not load the missing native library.

## [43.0.1-1.1.1] - 2026-04-17

### Security

- **Wasmtime upgraded from 43.0.0 to 43.0.1** — upstream security patch
  release addressing multiple advisories. Users are encouraged to upgrade.
  Affected areas include:
  - Sandbox escape on aarch64 Cranelift via miscompiled guest heap access
    ([GHSA-jhxm-h53p-jm7w](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-jhxm-h53p-jm7w))
  - Sandbox-escaping memory access with the Winch compiler backend
    ([GHSA-xx5w-cvp6-jv83](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-xx5w-cvp6-jv83))
  - Out-of-bounds write / crash in component model string transcoding
    ([GHSA-394w-hwhg-8vgm](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-394w-hwhg-8vgm))
  - Host panic on Winch `table.fill`
    ([GHSA-q49f-xg75-m9xw](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-q49f-xg75-m9xw))
  - Segfault / out-of-sandbox load with `f64x2.splat` on x86-64
    ([GHSA-qqfj-4vcm-26hv](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-qqfj-4vcm-26hv))
  - Improperly masked `table.grow` return value with Winch
    ([GHSA-f984-pcp8-v2p7](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-f984-pcp8-v2p7))
  - Panic transcoding misaligned utf-16 strings
    ([GHSA-jxhv-7h78-9775](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-jxhv-7h78-9775))
  - Panic lifting `flags` component value
    ([GHSA-m758-wjhj-p3jq](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-m758-wjhj-p3jq))
  - Heap OOB read in UTF-16 to latin1+utf16 transcoding
    ([GHSA-hx6p-xpx3-jvvv](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-hx6p-xpx3-jvvv))
  - Use-after-free after cloning `wasmtime::Linker`
    ([GHSA-hfr4-7c6c-48w2](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-hfr4-7c6c-48w2))
  - Data leakage between pooling allocator instances
    ([GHSA-6wgr-89rj-399p](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-6wgr-89rj-399p))
  - Host data leakage with 64-bit tables and Winch
    ([GHSA-m9w2-8782-2946](https://github.com/bytecodealliance/wasmtime/security/advisories/GHSA-m9w2-8782-2946))

## [43.0.0-1.1.1] - 2026-04-09

### Fixed

- **Linux glibc compatibility**: Linux native libraries are now built on
  AlmaLinux 8 (glibc 2.28) instead of Ubuntu 24.04 (glibc 2.39), restoring
  compatibility with RHEL 8, CentOS 8, Rocky Linux 8, Ubuntu 20.04, Debian 10,
  and other distributions with glibc >= 2.28.
- Increased Maven Central publish wait timeout from the default to 60 minutes
  to prevent release failures during artifact validation and propagation.

### Dependencies

- spotbugs-annotations 4.8.6 -> 4.9.8
- awaitility 4.2.0 -> 4.3.0
- jackson-databind 2.15.2/2.16.1 -> 2.21.2
- jackson-datatype-jsr310 2.15.2/2.16.1 -> 2.21.2

## [43.0.0-1.1.0] - 2026-03-27

### Changed

- **Wasmtime upgraded from 42.0.1 to 43.0.0**
  - WasiHttpView trait migrated to `p2` submodule with new `http()` method
  - `debug_frames()` cursor API replaced with `debug_exit_frames()` iterator
  - `Config::wasm_backtrace` deprecated; migrated to `wasm_backtrace_max_frames`
  - Serialized modules from 42.0.1 are not compatible with 43.0.0

### Added

- **OperatorCost configuration**: Per-operator fuel cost control (0-255 per operator).
  Configure via `EngineConfig.operatorCost(OperatorCost.defaults().set("Call", 5))`.
  Only meaningful when `consumeFuel` is enabled.
- **Store debug introspection APIs**: `Store.debugInstanceCount()` and
  `Store.debugModuleCount()` for runtime introspection of active instances and
  modules when guest debugging is enabled.
- **Experimental WASI P3 support**: `ComponentLinker.enableWasiP3()` and
  `enableWasiHttpP3()` behind opt-in `wasi-p3` feature flag. P3 is experimental
  and unstable per the wasmtime project.
- **FuncType::try_new**: Graceful OOM handling in host function creation. Allocation
  failures now propagate as `WasmException` instead of panicking.
- **ExternRef/FuncRef JNI support**: JNI host function callbacks now handle
  ExternRef and FuncRef parameter types.
- **Stack overflow protection**: Default `max_wasm_stack(512 KiB)` prevents
  recursive Wasm code from causing SIGSEGV.

### Fixed

- Arithmetic overflow in WIT value deserializer when parsing malformed resource data
- JNI phantom reference cleanup crash (SIGABRT) on JVM shutdown with fake test handles
- Flaky JniResourceTest threading tests checking `isClosed()` instead of `getCloseCount()`
- CI pipeline fully green for the first time (17/17 jobs across 4 platforms, 3 Java versions)
- Multiple CI workflow fixes for Java 8/21/23 compatibility, checkstyle, SpotBugs, spotless
- CodeQL, fuzz testing, dependency update, and security workflows all passing

### Dependencies

- Wasmtime 43.0.0 (upgraded from 42.0.1)
- Java 8+ (JNI), Java 23+ (Panama)
- Rust stable toolchain
- Maven 3.6+

## [42.0.1-1.0.0] - 2025-03-08

Initial public release of Wasmtime4j, providing complete Java bindings for
the [Wasmtime](https://wasmtime.dev/) WebAssembly runtime (v42.0.1).

### Added

#### Core Runtime
- Full Wasmtime API: Engine, Module, Instance, Store, Linker
- Dual runtime architecture: JNI (Java 8+) and Panama FFI (Java 23+)
- Automatic runtime selection based on Java version
- Manual override via `-Dwasmtime4j.runtime=jni` or `=panama`
- Factory-based loading with `WasmRuntimeFactory`
- Cross-platform: Linux, macOS, Windows on x86_64 and ARM64

#### WebAssembly Features
- Module compilation from bytes, files, and WAT text format
- Module validation, serialization, and deserialization
- Module introspection: exports, imports, types, custom sections
- Host function callbacks (WASM-to-Java)
- Memory read/write/grow operations
- Table get/set/grow/copy operations
- Global get/set for mutable and immutable globals
- Fuel-based metering and epoch-based interruption
- Store-level resource limits (memory, tables, instances)

#### Advanced WebAssembly Proposals
- Multi-memory, multi-value, bulk memory operations
- Reference types and function references
- Tail calls
- SIMD and relaxed SIMD (v128 values)
- GC types: StructRef, ArrayRef, ExternRef, AnyRef, ExnRef, I31Ref
- Custom page sizes and wide arithmetic
- Exception handling
- Stack switching types (ContRef, ContType)

#### Component Model
- Component compilation and instantiation
- Component Linker with WIT-based interface binding
- Typed component function calls with full WIT type mapping
- Async component model: streams, futures, error context
- Concurrent component calls via `runConcurrent`

#### WASI
- WASI Preview 1: stdin/stdout/stderr, environment variables,
  arguments, filesystem (sandboxed), clock, random
- WASI Preview 2: streams, pollable I/O
- WASI-NN: host-side neural network inference bindings
- Configurable WASI contexts per Store

#### Typed Fast-Path Calls
- Zero-boxing typed function calls for common signatures
- Panama: direct native calls bypassing WasmValue tagged union entirely
- JNI: primitive native methods reducing boundary crossings from ~6 to 1
- Supported: `void`, `()->i32`, `i32->i32`, `(i32,i32)->i32`,
  `i64->i64`, `f64->f64`, `i32->void`, `(i32,i32)->void`,
  `(i32,i32,i32)->i32`

#### Engine Configuration
- Optimization levels (None, Speed, SpeedAndSize)
- Parallel compilation
- Fuel consumption and epoch interruption
- Debug info and address map control
- Custom memory creators, stack creators, and code memory
- Cache store interface
- Engine pooling

#### Testing
- WAST test runner using Wasmtime's native parser
- Unit tests for all API surfaces (JNI and Panama)
- Integration test suite with WebAssembly test files
- JMH performance benchmarks (PanamaVsJniBenchmark)

### Performance

- Captured `Arc<HostFunction>` directly in closures, eliminating
  per-callback mutex lookup on the global host function registry
- Removed `clear_last_error()` from FFI success paths
- Replaced `Mutex<u64>` store ID counter with `AtomicU64`
- Panama: volatile `ensureNotClosed()` replaces read-write lock
  acquisition on every fast-path call
- Panama: `PanamaTypedFunc` delegates to `WasmFunction` fast-path
  methods instead of boxing through generic `call()`
- Func handle caching on Instance avoids repeated export lookups

### Dependencies

- Wasmtime 42.0.1
- Java 8+ (JNI), Java 23+ (Panama)
- Rust stable toolchain
- Maven 3.6+
