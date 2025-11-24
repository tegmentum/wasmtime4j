# WASI Preview 2 Test Plan

## Overview

This document outlines the testing strategy for WASI Preview 2 implementation in wasmtime4j. The implementation is structurally complete with Java APIs, JNI bindings, and Panama FFI bindings. Testing will validate both the Java layer correctness and the native Rust implementation once completed.

## Test Categories

### 1. Unit Tests (Java Layer)

#### 1.1 wasi:io Tests
**Location:** `wasmtime4j-tests/src/test/java/ai/tegmentum/wasmtime4j/wasi/io/`

**WasiInputStream Tests:**
- Non-blocking read operations
- Blocking read operations
- Skip operations (blocking and non-blocking)
- Stream subscription and pollable creation
- Error handling (closed stream, invalid parameters)
- Resource cleanup and lifecycle

**WasiOutputStream Tests:**
- Check write capacity
- Non-blocking write operations
- Blocking write and flush
- Write zeroes operations
- Stream splicing between streams
- Flush operations (blocking and non-blocking)
- Stream subscription and pollable creation
- Error handling and resource cleanup

**WasiPollable Tests:**
- Block until ready
- Non-blocking ready check
- Multiple pollable coordination
- Error scenarios

#### 1.2 wasi:filesystem Tests
**Location:** `wasmtime4j-tests/src/test/java/ai/tegmentum/wasmtime4j/wasi/filesystem/`

**WasiDescriptor Tests:**
- Stream-based I/O (read, write, append)
- File operations (setSize, sync, syncData)
- Directory operations (openAt, createDirectoryAt, readDirectory)
- Path operations (renameAt, symlinkAt, linkAt, unlinkFileAt, removeDirectoryAt)
- Metadata operations (getDescriptorType, getFlags, isSameObject)
- Symbolic link operations (readLinkAt)
- Permission and flag handling
- Error scenarios for each operation
- Resource cleanup

**DescriptorType Tests:**
- Type enumeration completeness
- Type detection accuracy

**Flags Tests:**
- DescriptorFlags combinations
- PathFlags behavior
- OpenFlags validation

#### 1.3 wasi:cli Tests
**Location:** `wasmtime4j-tests/src/test/java/ai/tegmentum/wasmtime4j/wasi/cli/`

**WasiEnvironment Tests:**
- Get all environment variables
- Get single environment variable
- Get command-line arguments
- Get initial working directory
- Empty/null handling
- Large environment variable sets

**WasiStdio Tests:**
- Get stdin stream
- Get stdout stream
- Get stderr stream
- Stream validity
- Multiple get operations

**WasiExit Tests:**
- Exit with success code
- Exit with failure code
- Exit with custom codes
- Multiple exit attempts

### 2. Integration Tests (End-to-End)

#### 2.1 Component Model Integration
**Location:** `wasmtime4j-tests/src/test/java/ai/tegmentum/wasmtime4j/integration/`

**Prerequisites:**
- WebAssembly Component Model test components
- Rust toolchain with wasm32-wasi target
- wit-bindgen for component generation

**Test Scenarios:**
1. **Simple I/O Component**
   - Component that reads from stdin, processes, writes to stdout
   - Validates basic stream operations
   - Tests blocking and non-blocking modes

2. **Filesystem Component**
   - Component that creates/reads/writes files
   - Directory traversal
   - Permission validation
   - Error handling

3. **Environment Component**
   - Component that reads environment variables
   - Command-line argument processing
   - Working directory operations

4. **Multi-Stream Component**
   - Concurrent read/write operations
   - Stream coordination with pollables
   - Resource management under load

#### 2.2 Runtime Parity Tests
**Location:** `wasmtime4j-tests/src/test/java/ai/tegmentum/wasmtime4j/parity/`

**Objective:** Verify JNI and Panama implementations produce identical results

**Test Matrix:**
- Run all integration tests with both runtimes
- Compare outputs, performance, resource usage
- Validate error handling consistency
- Test runtime switching

### 3. Performance Tests

#### 3.1 Stream Performance
**Location:** `wasmtime4j-benchmarks/src/main/java/ai/tegmentum/wasmtime4j/benchmarks/`

**Benchmarks:**
- Sequential read throughput
- Sequential write throughput
- Random access patterns
- Small vs large buffer sizes
- Blocking vs non-blocking performance
- Stream splicing efficiency

#### 3.2 Filesystem Performance
**Benchmarks:**
- File creation/deletion
- Directory traversal
- Metadata operations
- Large file operations
- Concurrent file access

#### 3.3 Resource Management
**Benchmarks:**
- Handle allocation/deallocation
- Memory usage patterns
- GC pressure
- Resource leak detection

### 4. Test Components (WASM)

#### 4.1 Required Test Components

**basic-io.wasm**
```wit
world basic-io {
  import wasi:io/streams@0.2.0

  export test-read: func() -> result<list<u8>, string>
  export test-write: func(data: list<u8>) -> result<_, string>
}
```

**filesystem-ops.wasm**
```wit
world filesystem-ops {
  import wasi:filesystem/types@0.2.0

  export test-create-file: func(path: string) -> result<_, string>
  export test-read-file: func(path: string) -> result<list<u8>, string>
  export test-list-dir: func(path: string) -> result<list<string>, string>
}
```

**cli-access.wasm**
```wit
world cli-access {
  import wasi:cli/environment@0.2.0
  import wasi:cli/stdin@0.2.0
  import wasi:cli/stdout@0.2.0

  export test-env: func(key: string) -> result<string, string>
  export test-args: func() -> list<string>
  export test-echo: func(msg: string) -> result<_, string>
}
```

#### 4.2 Test Component Build System

**Location:** `wasmtime4j-tests/wasm-components/`

**Structure:**
```
wasm-components/
├── Cargo.toml (workspace)
├── basic-io/
│   ├── Cargo.toml
│   ├── wit/
│   │   └── world.wit
│   └── src/
│       └── lib.rs
├── filesystem-ops/
│   ├── Cargo.toml
│   ├── wit/
│   │   └── world.wit
│   └── src/
│       └── lib.rs
└── cli-access/
    ├── Cargo.toml
    ├── wit/
    │   └── world.wit
    └── src/
        └── lib.rs
```

**Build Integration:**
- Maven exec plugin to build components during test phase
- Pre-built components checked into git for CI
- Validation that components match source

### 5. Error Handling Tests

#### 5.1 Native Error Scenarios
- Invalid handles
- Null pointers
- Out of bounds access
- Resource exhaustion
- Concurrent access violations
- Platform-specific errors

#### 5.2 Java Error Mapping
- WasmException propagation
- Error message clarity
- Stack trace preservation
- Resource cleanup on error
- Error recovery

### 6. Resource Management Tests

#### 6.1 Lifecycle Tests
- Handle creation and cleanup
- Phantom reference cleanup
- Finalization behavior
- Explicit close vs GC cleanup
- Double-close safety

#### 6.2 Leak Detection
- Long-running tests with monitoring
- Allocation tracking
- Memory profiling
- Native memory monitoring

### 7. Platform-Specific Tests

#### 7.1 Cross-Platform Matrix
- Linux (x86_64, aarch64)
- macOS (x86_64, aarch64/Apple Silicon)
- Windows (x86_64)

#### 7.2 Platform-Specific Behaviors
- Path handling differences
- Line ending handling
- File permission models
- Symbolic link support

### 8. Backwards Compatibility Tests

#### 8.1 WASI Preview 1 Compatibility
**Location:** `wasmtime4j-tests/src/test/java/ai/tegmentum/wasmtime4j/tests/WasiBackwardCompatibilityTest.java`

**Scenarios:**
- Preview 1 modules on Preview 2 runtime
- Migration path validation
- Feature detection

## Test Execution Strategy

### Phase 1: Java Layer Validation (Current)
- Unit tests for all Java APIs
- Mock native implementations for testing
- Validate parameter validation
- Test error handling

### Phase 2: Native Implementation (After Rust Implementation Complete)
- Integration tests with real Wasmtime
- Component model tests
- Resource management validation

### Phase 3: Performance Optimization
- Benchmark baseline establishment
- Performance regression detection
- Optimization validation

### Phase 4: Production Readiness
- Long-running stability tests
- Memory leak detection
- Cross-platform validation
- CI/CD integration

## Test Coverage Goals

- **Java API Layer:** 100% line coverage
- **JNI Implementations:** 100% line coverage
- **Panama Implementations:** 100% line coverage
- **Integration Tests:** All major WASI operations covered
- **Error Paths:** All error conditions tested

## CI/CD Integration

### Test Profiles

**Fast Tests (default):**
```bash
./mvnw test -q
```
- Unit tests only
- No native compilation
- < 2 minutes

**Integration Tests:**
```bash
./mvnw test -P integration-tests -q
```
- Full integration tests
- Native compilation
- Component loading
- < 10 minutes

**Full Test Suite:**
```bash
./mvnw verify -P integration-tests,benchmarks -q
```
- All tests
- Benchmarks
- Coverage reports
- < 30 minutes

### Test Execution Matrix

**Per Commit:**
- Fast tests (all platforms)
- Basic integration tests

**Per PR:**
- Full test suite (all platforms)
- Performance regression check
- Memory leak detection

**Nightly:**
- Extended stability tests
- Cross-platform validation
- Fuzzing tests

## Test Data and Fixtures

### Test Resources
**Location:** `wasmtime4j-tests/src/test/resources/`

**Structure:**
```
resources/
├── wasm/
│   ├── components/
│   │   ├── basic-io.wasm
│   │   ├── filesystem-ops.wasm
│   │   └── cli-access.wasm
│   └── modules/
│       └── (existing WASM modules)
├── fixtures/
│   ├── test-files/
│   ├── test-dirs/
│   └── env-vars.properties
└── expected/
    ├── output-samples/
    └── error-messages/
```

## Next Steps

1. **Immediate (No Native Implementation Required):**
   - Implement Java unit tests with mock natives
   - Create test fixtures and resources
   - Set up test infrastructure

2. **Short-term (After Native Implementation Begins):**
   - Create basic test components
   - Implement integration tests
   - Set up CI/CD test automation

3. **Medium-term (During Native Implementation):**
   - Add comprehensive error handling tests
   - Implement performance benchmarks
   - Cross-platform testing

4. **Long-term (Production Readiness):**
   - Extended stability testing
   - Memory leak detection automation
   - Performance regression tracking

## Current Status

- ✅ Test plan documented
- ⬜ Java unit tests implementation
- ⬜ Test component creation
- ⬜ Integration test implementation
- ⬜ Performance benchmarks
- ⬜ CI/CD integration

## References

- WASI Preview 2 Specification: https://github.com/WebAssembly/WASI/tree/main/preview2
- Component Model Specification: https://github.com/WebAssembly/component-model
- Wasmtime Documentation: https://docs.wasmtime.dev/
