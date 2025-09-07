---
name: complete-async-support
status: completed
created: 2025-09-05T00:49:38Z
completed: 2025-09-06T16:12:15Z
progress: 100%
prd: .claude/prds/complete-async-support.md
github: https://github.com/tegmentum/wasmtime4j/issues/165
---

# Epic: complete-async-support

## Overview

Transform wasmtime4j from partial async support (currently ~25% coverage) to comprehensive native async WebAssembly execution. Build upon existing infrastructure: WasiAsyncFileOperations, JniConcurrencyManager, and native Tokio runtime to create consistent async APIs across all public interfaces.

**Current State**: WASI async file I/O exists, native Tokio runtime available, but core APIs (Engine, Module, WasmFunction) lack async methods.
**Target State**: Complete async API parity with sync operations, native performance through Tokio integration.

## Architecture Decisions

### 1. Leverage Existing Infrastructure
- **Tokio Runtime**: Use existing native Tokio setup (Cargo.toml:27,68,72) instead of building new async runtime
- **CompletableFuture Pattern**: Extend existing WasiAsyncFileOperations pattern to all async operations
- **Concurrency Management**: Build on existing JniConcurrencyManager and ConcurrentAccessCoordinator

### 2. Incremental API Addition
- Add async methods alongside existing sync APIs (no breaking changes)
- Follow existing naming pattern: `callAsync()` (matches WasiInstance.callAsync())
- Maintain API consistency across JNI and Panama implementations

### 3. Native-First Async Implementation
- Implement true async execution at Rust/native layer using Wasmtime's async engine
- Bridge native async operations through JNI/Panama callbacks
- Avoid Java-wrapper async (CompletableFuture.supplyAsync(() -> syncCall()))

## Technical Approach

### Core Components
**Public API Layer** (wasmtime4j module):
- Add `Engine.compileModuleAsync()` - asynchronous module compilation
- Add `WasmFunction.callAsync()` - asynchronous function invocation  
- Add `Module.instantiateAsync()` - asynchronous instance creation
- Extend existing async WASI operations for consistency

**Implementation Layer** (JNI/Panama):
- Create async bindings bridge to existing Tokio runtime
- Implement native-to-Java callback mechanisms
- Unify async thread management using existing concurrency infrastructure

**Native Layer** (Rust):
- Leverage existing Tokio async runtime
- Use Wasmtime's native async engine and store capabilities
- Implement async FFI bindings for JNI and Panama

### Integration Points
- **Existing WASI Async**: Align new core async APIs with WasiAsyncFileOperations patterns
- **Concurrency Management**: Extend existing JniConcurrencyManager for unified async execution
- **Testing Infrastructure**: Build on existing ConcurrencyBenchmark and test suites
- **Performance Monitoring**: Extend existing JMH benchmark infrastructure

## Implementation Strategy

### Phase-Based Development
1. **Core API Methods** (3 weeks): Add missing async methods to public interfaces
2. **Native Bridge** (3 weeks): Connect Java async APIs to existing Tokio runtime  
3. **Consistency & Enhancement** (3 weeks): Align all async patterns and extend WASI
4. **Testing & Optimization** (3 weeks): Comprehensive testing using existing infrastructure

### Risk Mitigation
- **Performance**: Continuous benchmarking using existing JMH infrastructure
- **Consistency**: Shared test suite across JNI/Panama implementations
- **Complexity**: Build incrementally on proven existing patterns

### Quality Approach
- Extend existing comprehensive test suites with async-specific tests
- Use existing static analysis and code quality tools
- Build on existing cross-platform testing infrastructure

## Task Breakdown Preview

High-level task categories that will be created:
- [ ] **Core Async API Implementation**: Add async methods to Engine, Module, WasmFunction interfaces
- [ ] **Native Async Bridge**: Connect Java async APIs to existing Tokio runtime via JNI/Panama
- [ ] **WASI Async Enhancement**: Extend existing WasiAsyncFileOperations to full WASI async support
- [ ] **Cross-Runtime Consistency**: Ensure JNI/Panama async behavior parity
- [ ] **Unified Async Framework**: Replace ad-hoc ExecutorService with consistent async patterns
- [ ] **Testing & Benchmarking**: Extend existing test and benchmark infrastructure for async operations
- [ ] **Documentation & Examples**: Update docs and examples with new async API usage

## Dependencies

### External Dependencies
- **Wasmtime 26.0.2+**: Native async engine capabilities (already available)
- **Tokio Runtime**: Async execution infrastructure (already configured in Cargo.toml)

### Internal Dependencies  
- **Existing Infrastructure**: WasiAsyncFileOperations, JniConcurrencyManager, ConcurrentAccessCoordinator
- **Native Library**: wasmtime4j-native Rust enhancements
- **Test Framework**: Existing JUnit 5 and JMH benchmark infrastructure

### Critical Path Items
1. **Public API Design**: Consistent async method signatures across all interfaces
2. **Native Async Bindings**: Bridge existing Tokio runtime to Java layer
3. **Cross-Runtime Parity**: Ensure JNI and Panama implementations behave identically
4. **Performance Validation**: Achieve target performance using existing benchmark infrastructure

## Success Criteria (Technical)

### Performance Benchmarks
- **Throughput**: >10,000 concurrent async function calls/second
- **Latency**: 50% improvement in P99 latency for concurrent operations  
- **Resource Efficiency**: 30% reduction in thread usage vs current approach
- **Native Performance**: >90% of native Wasmtime async performance

### Quality Gates
- **API Coverage**: 100% of sync APIs have async equivalents
- **Test Coverage**: >95% async API test coverage using existing test infrastructure
- **Cross-Runtime Parity**: 100% behavioral consistency between JNI/Panama
- **Memory Safety**: Zero async resource leaks in 24-hour stress tests

### Acceptance Criteria
- All sync APIs have corresponding async variants
- Async performance exceeds Java-wrapper async approach
- Consistent async behavior across JNI and Panama implementations
- Integration with existing Java async frameworks (Spring WebFlux, etc.)

## Estimated Effort

### Overall Timeline
**12 weeks total** (3 weeks per phase)

### Resource Requirements
- **1-2 senior developers** with Rust and Java expertise
- **Existing infrastructure**: Leverage current testing, CI/CD, and build systems

### Critical Path Items
1. **Week 1-3**: Core async API implementation and design consistency
2. **Week 4-6**: Native async bridge implementation using existing Tokio runtime
3. **Week 7-9**: Cross-runtime consistency and WASI async enhancement  
4. **Week 10-12**: Testing, performance optimization, and documentation

**Key Advantage**: Building on existing async infrastructure (WasiAsyncFileOperations, Tokio runtime, concurrency management) reduces implementation complexity and risk compared to ground-up async implementation.

## Tasks Created
- [ ] #166 - Add async methods to Engine interface (parallel: true)
- [ ] #167 - Add async methods to WasmFunction interface (parallel: true)
- [ ] #168 - Add async methods to Module interface (parallel: true)
- [ ] #169 - Implement JNI async bindings bridge (parallel: false)
- [ ] #170 - Implement Panama async bindings bridge (parallel: false)
- [ ] #171 - Extend WASI async operations for consistency (parallel: true)
- [ ] #172 - Replace ad-hoc ExecutorService with unified async framework (parallel: true)
- [ ] #173 - Extend async testing and benchmarking infrastructure (parallel: true)

Total tasks: 8
Parallel tasks: 6
Sequential tasks: 2
Estimated total effort: 108-148 hours (13-18.5 days)
