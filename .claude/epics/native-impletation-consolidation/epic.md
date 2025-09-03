---
name: native-impletation-consolidation
status: backlog
created: 2025-09-03T23:19:32Z
progress: 0%
prd: .claude/prds/native-impletation-consolidation.md
github: [Will be updated when synced to GitHub]
---

# Epic: Native Implementation Consolidation

## Overview

Consolidate duplicate FFI implementations in `panama_ffi.rs` and `jni_bindings.rs` into a unified shared architecture. This refactoring eliminates 80% code duplication by extracting common business logic into a new `shared_ffi.rs` module, using trait-based parameter conversion and Rust macros to generate interface-specific bindings. The result is improved maintainability, consistent error handling, and reduced compilation overhead while preserving 100% functional compatibility.

## Architecture Decisions

- **Shared Implementation Module**: New `shared_ffi.rs` module contains all business logic, eliminating duplication between JNI and Panama interfaces
- **Trait-Based Conversion**: Generic parameter conversion system handles JNI types (`jlong`, `jint`) and C types (`*mut c_void`, `c_int`) through unified traits
- **Macro-Generated Bindings**: Rust macros generate interface-specific wrapper functions from shared core implementations
- **Standardized Error Handling**: Unified error return codes (-1 for errors) across all interfaces with consistent error mapping
- **Zero-Cost Abstractions**: Leverage Rust's type system for compile-time optimizations without runtime overhead

## Technical Approach

### Core Architecture
- **Shared Logic Layer**: Extract common business logic into `shared_ffi.rs` module
- **Trait System**: `ParameterConverter` and `ReturnValueConverter` traits for type-safe conversions
- **Error Unification**: Standardize all error returns to -1 with consistent error code mapping
- **Macro Framework**: Declarative macros for generating both JNI and Panama bindings from single source

### Parameter Conversion System
- Generic `ParameterConverter<T>` trait for enum conversions (Strategy, OptLevel, WasmFeature)
- Shared validation logic for complex parameter sets (engine configuration, memory limits)
- Type-safe pointer conversion utilities with comprehensive null checking
- Unified string and byte array handling for Java-specific types

### Interface Layer Refactoring
- Reduce `panama_ffi.rs` to thin C FFI wrapper calls using shared implementations
- Reduce `jni_bindings.rs` to thin JNI wrapper calls using shared implementations
- Maintain identical function signatures for external compatibility
- Preserve existing cargo feature flag behavior (`jni-bindings`)

## Implementation Strategy

### Phase 1: Foundation (Shared Architecture)
- Design and implement trait-based parameter conversion system
- Create `shared_ffi.rs` module with core function signatures
- Establish macro framework for generating interface bindings
- Implement unified error handling with standardized return codes

### Phase 2: Logic Extraction
- Extract engine operations logic from both FFI modules
- Extract module operations logic (compilation, validation, serialization)
- Extract store and instance management logic
- Implement shared component operations (WASI Preview 2)

### Phase 3: Interface Consolidation
- Refactor `panama_ffi.rs` to use shared implementations
- Refactor `jni_bindings.rs` to use shared implementations
- Validate identical behavior across both interfaces
- Update error handling to use standardized approach

## Task Breakdown Preview

High-level task categories that will be created:
- [ ] **Shared Architecture Design**: Create trait system and macro framework for unified FFI handling
- [ ] **Core Logic Extraction**: Extract common business logic from existing FFI modules into shared implementation
- [ ] **Error Standardization**: Implement unified error handling with consistent return codes across interfaces
- [ ] **Interface Refactoring**: Convert existing FFI modules to thin wrappers using shared implementations
- [ ] **Testing & Validation**: Ensure 100% functional compatibility and performance benchmarking
- [ ] **Documentation Updates**: Update architecture documentation and code comments for new design

## Dependencies

### Technical Dependencies
- Existing `ffi_utils` module for basic error handling utilities
- Core modules (`engine::core`, `module::core`, etc.) must remain stable during refactoring
- Cargo feature system for conditional JNI compilation (`jni-bindings` feature)
- Current test infrastructure to validate no functional regressions

### Build System Dependencies
- No changes required to Maven build process or cross-compilation setup
- Existing cargo features and conditional compilation must be preserved
- Native library loading mechanisms remain unchanged

## Success Criteria (Technical)

### Code Quality Metrics
- **Duplication Elimination**: Reduce FFI-specific code from 1800+ lines to <400 lines total
- **Zero Duplicate Functions**: All business logic implemented once in shared module
- **Consistent Error Handling**: 100% identical error codes between JNI and Panama interfaces

### Performance Benchmarks
- **No Performance Regression**: Function call latency must not exceed current baselines
- **Compilation Improvement**: 10-20% faster Rust compilation due to reduced code duplication
- **Binary Size Reduction**: 5-10% smaller native library due to shared implementations

### Compatibility Gates
- **100% Test Pass Rate**: All existing tests must pass with identical results
- **API Compatibility**: External Java API behavior remains unchanged
- **Feature Flag Compatibility**: Cargo features work identically to current implementation

## Estimated Effort

### Overall Timeline
- **Total Effort**: 6-8 development days
- **Critical Path**: Shared architecture design and core logic extraction (days 1-4)
- **Risk Buffer**: 2 days for testing validation and performance optimization

### Implementation Phases
- **Phase 1 (Foundation)**: 2 days - Trait system and macro framework
- **Phase 2 (Extraction)**: 3 days - Extract logic from existing FFI modules
- **Phase 3 (Consolidation)**: 2 days - Refactor interface layers
- **Phase 4 (Validation)**: 1 day - Testing and performance validation

### Resource Requirements
- **Primary Developer**: Full-time commitment for technical implementation
- **Code Review**: 2-3 hours from lead maintainer for architecture approval
- **Testing Validation**: Automated test suite execution across supported platforms