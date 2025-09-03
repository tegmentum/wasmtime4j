---
name: native-implementations
status: completed
created: 2025-09-03T10:54:00Z
completed: 2025-09-03T23:17:47Z
progress: 100%
prd: .claude/prds/native-implementations.md
github: #140
---

# Epic: Native Implementations

## Overview

Implement a complete, production-ready shared native Rust library (`wasmtime4j-native`) that provides unified WebAssembly runtime bindings for both JNI and Panama FFI implementations. This consolidates all native functionality into a single library, eliminates code duplication, and delivers 100% Wasmtime API coverage with bulletproof error handling and cross-platform reliability.

## Architecture Decisions

**Shared Native Library Strategy**: Single `wasmtime4j-native` Rust crate with dual export paths - JNI bindings for Java 8-22 and Panama FFI for Java 23+. This eliminates code duplication and ensures identical functionality across both implementations.

**Error Handling Architecture**: Native-level error translation (Option A) with complete alignment between Rust error codes (-1 to -18) and Java exception mappers. All Wasmtime errors are caught in native code and translated to appropriate Java exception categories.

**Cross-Compilation Strategy**: Maven-integrated build pipeline that cross-compiles for all 6 target platforms (Linux/Windows/macOS × x86_64/ARM64) and packages native libraries into JARs during the build process.

**Defensive Programming First**: Every native function validates inputs, handles errors gracefully, and prevents JVM crashes through comprehensive parameter checking and resource management.

## Technical Approach

### Native Library Core (`wasmtime4j-native`)

**Rust Implementation Structure**:
- Core Wasmtime API wrappers with comprehensive error handling
- Separate JNI and Panama FFI export functions sharing common implementation
- Thread-safe resource management with automatic cleanup
- Parameter validation macros for all public functions

**API Coverage Strategy**:
- Engine management (creation, configuration, resource limits)
- Module compilation and validation (bytecode processing)
- Instance lifecycle (instantiation, imports, exports)
- WASI integration (filesystem, environment, networking)
- Memory operations (linear memory, growth, bounds checking)
- Host function registration and invocation
- Global and table operations

### Error Handling System

**Critical Bug Fixes**:
- Align Rust error codes (-1 to -18) with Java JNI mapper constants
- Implement proper JNI exception throwing instead of returning 0
- Complete Panama error pointer interpretation
- Add thread-safe error message extraction

**Implementation Approach**:
- Comprehensive `WasmtimeError` enum mapping to Java exceptions
- Thread-local error storage with proper cleanup
- Consistent error categorization across JNI and Panama paths

### Cross-Platform Build Pipeline

**Maven Integration**:
- Cross-compilation configuration for all target architectures
- Automated native library extraction and packaging
- Build-time validation across platforms
- Integration with existing Maven build lifecycle

**Build Strategy**:
- Leverage existing Rust cross-compilation tools
- Platform-specific library naming and packaging
- Automated testing on target platforms through CI/CD

## Implementation Strategy

**Development Phases**:
1. **Foundation**: Fix critical error handling bugs and establish build pipeline
2. **Core API**: Implement essential Wasmtime operations (Engine, Module, Instance)
3. **Extended API**: Add WASI support, host functions, and memory management
4. **Optimization**: Performance tuning and comprehensive testing

**Risk Mitigation**:
- Start with existing partially-implemented code to avoid greenfield risks
- Focus on fixing identified bugs before adding new functionality
- Incremental testing strategy to catch regressions early
- Cross-platform validation at each phase

**Testing Approach**:
- Leverage existing WebAssembly test suites and Wasmtime tests
- Comprehensive JNI and Panama path validation
- Platform-specific testing for all 6 target architectures
- Performance benchmarking with JMH integration

## Task Breakdown Preview

High-level task categories that will be created:
- [ ] **Fix Critical Error Handling**: Align error codes, implement proper JNI exception throwing
- [ ] **Establish Cross-Compilation Pipeline**: Maven integration for all target platforms  
- [ ] **Complete Core API Implementation**: Engine, Module, Instance with full validation
- [ ] **Implement WASI Support**: Filesystem, networking, environment access
- [ ] **Add Host Function System**: Registration, invocation, type checking
- [ ] **Implement Memory Management**: Linear memory, growth operations, bounds checking
- [ ] **Add Global and Table Operations**: Variable access, function references
- [ ] **Performance Optimization**: Call overhead reduction, memory allocation patterns
- [ ] **Comprehensive Testing**: Platform validation, WebAssembly test suites
- [ ] **Documentation and Integration**: API docs, build instructions, CI/CD setup

## Dependencies

**External Dependencies**:
- Wasmtime Rust crate (version 36.0.2)
- Rust stable toolchain with cross-compilation targets
- Platform-specific build tools (GCC, MSVC, Clang)

**Internal Dependencies**:
- Existing wasmtime4j public API interfaces (must remain compatible)
- JNI and Panama implementation modules (error mapping classes)
- Maven build system configuration

**Blocking Dependencies**:
- Resolution of error code misalignment between Rust and Java
- Maven cross-compilation configuration
- CI/CD environment setup for multi-platform testing

## Success Criteria (Technical)

**Performance Benchmarks**:
- Native call overhead <100 nanoseconds for simple operations
- Memory allocation patterns optimized to reduce GC pressure
- WebAssembly module instantiation <10ms for typical modules

**Quality Gates**:
- Zero JVM crashes in production workloads
- 100% Wasmtime API surface implemented and tested
- >95% line coverage for all native implementation code
- Zero Rust clippy warnings in CI

**Acceptance Criteria**:
- Native library builds successfully on all 6 target platforms
- Identical functionality between JNI and Panama implementations  
- All WebAssembly test suites pass on both implementation paths
- Performance benchmarks meet or exceed established baselines

## Estimated Effort

**Overall Timeline**: 6-8 weeks for complete implementation

**Resource Requirements**: 
- 1 developer with Rust/JNI expertise
- Access to cross-platform build environments
- CI/CD infrastructure for automated testing

**Critical Path Items**:
1. Error handling bug fixes (Week 1-2)
2. Cross-compilation pipeline setup (Week 2-3)  
3. Core API implementation (Week 3-5)
4. Extended features and optimization (Week 5-7)
5. Testing and validation (Week 6-8)

**Risk Factors**:
- Cross-platform build complexity may extend timeline
- Performance optimization may require additional iteration
- Integration testing across Java versions may reveal compatibility issues

## Tasks Created
- [ ] 001.md - Fix critical error handling bugs in Rust-Java mapping (parallel: false)
- [ ] 002.md - Establish cross-compilation pipeline in Maven build (parallel: true)
- [ ] 003.md - Consolidate native library structure in wasmtime4j-native (parallel: true)
- [ ] 004.md - Implement Engine management API with configuration (parallel: true)
- [ ] 005.md - Implement Module compilation and validation system (parallel: true)
- [ ] 006.md - Implement Instance lifecycle and import/export management (parallel: false)
- [ ] 007.md - Implement WASI support with filesystem and environment access (parallel: true)
- [ ] 008.md - Implement host function registration and invocation system (parallel: true)
- [ ] 009.md - Implement linear memory management with bounds checking (parallel: true)
- [ ] 010.md - Implement Global and Table operations for WebAssembly references (parallel: true)
- [ ] 011.md - Implement performance optimization and call overhead reduction (parallel: true)
- [ ] 012.md - Create comprehensive unit test suite for all native functions (parallel: true)
- [ ] 013.md - Implement WebAssembly test suite integration and validation (parallel: true)
- [ ] 014.md - Implement cross-platform validation and CI/CD integration (parallel: false)
- [ ] 015.md - Create performance benchmarking with JMH integration (parallel: true)

Total tasks: 15
Parallel tasks: 12
Sequential tasks: 3
Estimated total effort: 254-318 hours (6.35-7.95 weeks)