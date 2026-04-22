# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Version format: `{wasmtime-version}-{wasmtime4j-version}`

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
