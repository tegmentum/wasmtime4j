# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Version format: `{wasmtime-version}-{wasmtime4j-version}`

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
