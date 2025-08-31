---
name: implement-native-code
status: backlog
created: 2025-08-31T02:58:51Z
progress: 0%
prd: .claude/prds/implement-native-code.md
github: https://github.com/tegmentum/wasmtime4j/issues/28
---

# Epic: implement-native-code

## Overview

Implement a unified Rust native library (`wasmtime4j-native`) that provides complete Wasmtime 36.0.2 WebAssembly runtime functionality with 100% API parity. This single native library will export both JNI and Panama FFI interfaces, serving as the foundation for Java WebAssembly execution across all platforms with zero tolerance for JVM crashes and sub-millisecond performance targets.

## Architecture Decisions

**Single Native Library Strategy**
- Build one consolidated Rust library with dual interface exports (JNI + Panama FFI)
- Leverage Wasmtime's C API as the foundation with Rust wrapper optimizations
- Use conditional compilation for JNI vs Panama-specific optimizations while maintaining identical functionality

**Cross-Platform Build Strategy**
- Integrate Rust compilation directly into Maven build using cargo-maven-plugin
- Target 6 platform/architecture combinations: Linux/Windows/macOS on x86_64/ARM64  
- Package platform-specific native libraries into separate JARs for efficient distribution

**Defensive Programming Architecture**
- All native calls wrapped with comprehensive input validation and error handling
- Resource lifecycle management with automatic cleanup and leak prevention
- Thread-safe design supporting concurrent WebAssembly execution

**Performance Optimization Strategy**
- Minimize JNI/Panama call overhead through intelligent batching and caching
- Direct memory management to reduce GC pressure
- Pre-validate and batch operations where possible

## Technical Approach

### Native Library Core (`wasmtime4j-native`)

**Rust Implementation**
- Single Cargo project using Wasmtime 36.0.2 crate as primary dependency
- Export both `cdylib` (for JNI) and `staticlib` (for Panama) in same build
- Implement defensive wrappers around all Wasmtime C API calls
- Comprehensive error handling with detailed error codes and messages

**API Coverage Implementation**
- Engine: Configuration, creation, resource management
- Module: Compilation, validation, serialization/deserialization, caching
- Instance: Creation, function invocation, memory/table/global access
- WASI: Complete WasiCtx support with filesystem/environment controls
- Host Functions: Type-safe bidirectional data marshaling with callback support
- Memory: Direct access, growth operations, bounds checking
- Advanced: Component model integration, multi-memory support

### Build Integration

**Maven Cross-Compilation**
- Configure cargo-maven-plugin to build for all 6 target platforms during `compile` phase
- Set up cross-compilation toolchains and target configurations
- Generate platform-specific JAR artifacts with embedded native libraries
- Implement native library extraction and loading mechanism

**Static Analysis Integration**
- Rust: cargo clippy, cargo audit for security scanning
- Java: Existing Checkstyle, SpotBugs, PMD, Spotless integration
- Cross-boundary validation for all native method signatures

### Testing Strategy

**Comprehensive Test Coverage**
- Unit tests for every native API method using both JNI and Panama paths
- Integration tests with official WebAssembly test suite
- Cross-platform validation tests for all 6 target combinations
- Memory leak detection and resource cleanup validation
- Performance benchmarks with JMH targeting sub-millisecond latency

**Test Data and Infrastructure**
- WebAssembly test modules covering all Wasmtime features
- WASI test scenarios for filesystem, environment, I/O operations
- Host function integration test scenarios
- Malformed input and edge case validation

## Implementation Strategy

**Phase 1: Foundation & Core API (40% of effort)**
- Set up Rust project structure and Maven integration
- Implement core Engine, Module, Instance APIs with defensive programming
- Basic JNI and Panama FFI exports with identical functionality
- Cross-platform build pipeline and native library packaging

**Phase 2: WASI & Host Functions (30% of effort)**
- Complete WASI implementation with security controls
- Host function integration with type-safe marshaling
- Advanced memory management and resource cleanup
- Comprehensive error handling and logging

**Phase 3: Advanced Features & Optimization (20% of effort)**  
- Component model support and multi-memory features
- Performance optimizations and call overhead reduction
- Advanced WebAssembly proposals integration
- Benchmarking and performance validation

**Phase 4: Testing & Quality Assurance (10% of effort)**
- Comprehensive test suite completion
- Cross-platform validation and CI integration
- Documentation and example creation
- Performance profiling and optimization

## Task Breakdown Preview

High-level task categories that will be created:
- [ ] **Native Library Foundation**: Rust project setup, Maven integration, cross-compilation pipeline
- [ ] **Core Wasmtime API**: Engine, Module, Instance implementation with defensive programming
- [ ] **WASI Integration**: Complete WASI support with security controls and I/O management
- [ ] **Host Function System**: Bidirectional marshaling, type safety, callback support
- [ ] **Memory & Resource Management**: Direct memory access, cleanup, leak prevention
- [ ] **Advanced WebAssembly Features**: Component model, multi-memory, emerging proposals
- [ ] **Cross-Platform Build System**: All 6 platform targets, automated testing, packaging
- [ ] **Dual Interface Implementation**: JNI and Panama FFI exports with identical functionality
- [ ] **Comprehensive Testing**: Unit tests, integration tests, WebAssembly test suites, benchmarks
- [ ] **Performance & Security Validation**: Sub-millisecond benchmarks, security scanning, resource testing

## Dependencies

**External Dependencies**
- Wasmtime 36.0.2 Rust crate and C API stability
- Cross-compilation toolchains: rust-cross, cross-platform build environments
- cargo-maven-plugin or equivalent for Maven-Rust integration
- Platform-specific testing infrastructure and CI environments

**Internal Dependencies**
- Existing `wasmtime4j` unified API interfaces (must remain compatible)
- Maven build system configuration and cross-compilation setup
- JUnit 5 testing framework and WebAssembly test suites
- Static analysis toolchain integration (Checkstyle, SpotBugs, etc.)

**Critical Path Dependencies**
- Maven-Rust build integration must be established before core development
- Cross-platform toolchains required for comprehensive testing
- WebAssembly test suite integration needed for validation

## Success Criteria (Technical)

**Performance Benchmarks**
- Function invocation latency < 1 millisecond (99th percentile)
- Support > 1 million function calls per second under load
- Memory overhead < 10MB for typical WebAssembly module execution
- Native library size < 50MB per platform for reasonable JAR packaging

**Quality Gates**
- 100% Wasmtime API coverage with comprehensive test validation
- >90% test coverage across all native code paths
- Zero memory leaks detected in 24-hour stress testing
- All 6 platform builds succeed automatically in CI/CD
- Zero JVM crashes under extensive fuzzing and stress testing

**Integration Requirements**
- Identical functionality across JNI and Panama implementations
- Seamless integration with existing wasmtime4j unified API
- Successful native library loading on all supported platforms
- Error messages provide actionable feedback for all failure scenarios

## Estimated Effort

**Overall Timeline**: 8-10 weeks for complete implementation

**Resource Requirements**
- 1 senior developer with Rust + JNI experience (full-time)
- Cross-platform build environment access (Linux/Windows/macOS)
- WebAssembly domain expertise for advanced features

**Critical Path Items**
- Maven-Rust build integration setup (Week 1-2)
- Core Wasmtime API implementation (Week 2-5)
- WASI and host function integration (Week 5-7)
- Cross-platform testing and validation (Week 7-8)
- Performance optimization and benchmarking (Week 8-10)

**Risk Mitigation**
- Early cross-platform build validation to catch toolchain issues
- Incremental API implementation with continuous testing
- Performance benchmarking throughout development, not just at the end
- Defensive programming from day one to prevent late-stage crash issues

## Tasks Created
- [ ] #63 - Native Library Foundation (parallel: true)
- [ ] #66 - Maven-Rust Build Integration (parallel: false)
- [ ] #69 - Core Engine API Implementation (parallel: false)
- [ ] #71 - Module API Implementation (parallel: false)
- [ ] #72 - Instance API Implementation (parallel: false)
- [ ] #64 - WASI Integration System (parallel: false)
- [ ] #65 - Host Function Integration System (parallel: false)
- [ ] #67 - Memory & Resource Management (parallel: true)
- [ ] #68 - Cross-Platform Testing & Validation (parallel: false)
- [ ] #70 - Performance Optimization & Benchmarking (parallel: false)

Total tasks: 10
Parallel tasks: 3 (#63, #67, and #65/#67 after #72)
Sequential tasks: 7
Estimated total effort: 190-240 hours
