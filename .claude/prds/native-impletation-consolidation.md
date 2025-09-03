---
name: native-impletation-consolidation
description: Consolidate duplicate FFI implementations to eliminate code duplication and improve maintainability
status: backlog
created: 2025-09-03T12:35:43Z
---

# PRD: Native Implementation Consolidation

## Executive Summary

Consolidate the duplicate FFI implementations in `panama_ffi.rs` and `jni_bindings.rs` into a shared architecture that eliminates ~80% code duplication while maintaining type safety and improving overall maintainability. This refactoring will establish a unified foundation for both JNI and Panama Foreign Function Interface bindings.

## Problem Statement

### Current Issues
- **Massive Code Duplication**: ~80% of logic is duplicated between `panama_ffi.rs` (731 lines) and `jni_bindings.rs` (1094 lines)
- **Inconsistent Error Handling**: JNI returns 0 for errors while Panama returns -1, creating behavioral inconsistencies
- **Maintenance Burden**: Bug fixes and feature additions require changes in multiple locations
- **Testing Complexity**: Identical functionality must be tested through different interfaces
- **Performance Overhead**: Duplicate compilation and binary size impact

### Why Now?
- Greenfield project allows significant refactoring without legacy constraints
- Current duplication will compound as more Wasmtime APIs are added
- Consolidation is easier now before the codebase grows larger
- Foundation for future development velocity

## User Stories

### Primary Personas
1. **Library Maintainers** - Internal developers working on wasmtime4j
2. **Java Developers** - End users consuming the unified API
3. **Build Engineers** - Managing compilation and distribution

### Detailed User Journeys

**As a Library Maintainer**
- I want to implement new Wasmtime features once and have them automatically available in both JNI and Panama interfaces
- I want consistent error handling across all interfaces to avoid debugging interface-specific issues
- I want reduced compilation times and smaller binary sizes

**As a Java Developer**
- I want identical behavior whether my application uses JNI or Panama bindings
- I want consistent error messages and codes regardless of the underlying interface
- I want the consolidation to be transparent to my application code

**As a Build Engineer**
- I want faster compilation due to reduced duplicate code
- I want smaller artifacts due to shared implementations
- I want simplified testing that covers both interfaces comprehensively

## Requirements

### Functional Requirements

**FR1: Shared Implementation Architecture**
- Create `shared_ffi.rs` module containing all business logic
- Implement trait-based parameter conversion system
- Provide generic return value handling with consistent error codes

**FR2: Interface Layer Consolidation**
- Reduce `panama_ffi.rs` and `jni_bindings.rs` to thin wrapper layers
- Use Rust macros to generate interface-specific bindings from shared core functions
- Maintain identical functionality across both interfaces

**FR3: Error Standardization**
- Standardize error return values to -1 for errors across all interfaces
- Implement consistent error code mapping between native and Java layers
- Ensure error messages are identical between JNI and Panama

**FR4: Parameter Type Unification**
- Create trait-based conversion system for JNI types (`jlong`, `jint`) and C types (`*mut c_void`, `c_int`)
- Handle Java-specific type conversions (byte arrays, strings) through shared utilities
- Validate parameters consistently before core function calls

### Non-Functional Requirements

**NFR1: Performance**
- Maintain or improve current performance levels
- Zero-cost abstractions for shared implementations
- Minimize call overhead introduced by consolidation layer

**NFR2: Maintainability** 
- Reduce total lines of FFI code by 60-80%
- Enable single-point updates for business logic changes
- Improve code readability through consistent patterns

**NFR3: Type Safety**
- Maintain compile-time type safety across all interfaces
- Use Rust's type system to prevent interface-specific bugs
- Ensure memory safety for all pointer operations

**NFR4: Build System Integration**
- No changes to external Maven build process
- Maintain existing cargo feature flags (`jni-bindings`)
- Preserve cross-compilation support for all platforms

## Success Criteria

### Measurable Outcomes
- **Code Reduction**: 60-80% reduction in FFI-specific code lines
- **Test Coverage**: 100% compatibility with existing test suite
- **Performance**: No degradation in function call latency
- **Error Consistency**: 100% identical error codes between JNI and Panama
- **Build Time**: 10-20% improvement in Rust compilation time

### Key Metrics
- Lines of code in FFI modules (target: <400 total)
- Number of duplicate function implementations (target: 0)
- Test pass rate (target: 100% on existing test suite)
- Binary size reduction (target: 5-10% smaller native library)

## Constraints & Assumptions

### Technical Constraints
- Must maintain external Java API compatibility during transition
- Cannot introduce runtime performance regressions
- Must preserve existing cargo feature flag behavior
- Limited to JNI and Panama interfaces (no additional FFI types)

### Assumptions
- Core business logic in `engine::core`, `module::core`, etc. is correct and stable
- Existing test coverage is sufficient to validate consolidation
- Current error handling patterns in `ffi_utils` are adequate foundation
- Build system can handle new module structure without modifications

## Implementation Strategy

### Phase 1: Shared Architecture Design
- Design trait hierarchy for parameter conversion
- Define macro system for generating FFI bindings
- Establish error code standardization approach
- Create `shared_ffi.rs` module structure

### Phase 2: Core Logic Extraction
- Extract common business logic from both FFI modules
- Implement shared parameter validation and conversion
- Create generic return value handling system
- Establish macro-based binding generation

### Phase 3: Interface Layer Refactoring
- Refactor `panama_ffi.rs` to use shared implementations
- Refactor `jni_bindings.rs` to use shared implementations
- Update error handling to use standardized approach
- Ensure identical behavior across interfaces

### Phase 4: Testing & Validation
- Run existing test suite to verify compatibility
- Refactor tests to validate both interfaces comprehensively
- Performance testing to ensure no regressions
- Documentation updates reflecting new architecture

## Out of Scope

- Adding new Wasmtime API coverage beyond existing functionality
- Changing external Java API signatures or behavior
- Adding new FFI interfaces beyond JNI and Panama
- Modifying Maven build configuration or cargo cross-compilation setup
- Performance optimization beyond maintaining current levels

## Dependencies

### Technical Dependencies
- Existing `ffi_utils` module functionality
- Core modules (`engine::core`, `module::core`, etc.) stability
- Cargo feature system for conditional compilation
- Current test infrastructure and WebAssembly test files

### Team Dependencies
- Library maintainer availability for code review
- Testing validation across supported platforms
- Documentation review and approval

## Risk Assessment

### Low Risk
- Shared logic extraction (core functions are already isolated)
- Error code standardization (clear improvement)
- Macro-based binding generation (standard Rust practice)

### Medium Risk
- Large-scale refactoring across two major modules
- Potential for introducing subtle behavioral differences
- Testing complexity for validating both interfaces simultaneously

### Mitigation Strategies
- Incremental implementation with continuous testing
- Comprehensive test coverage before and after changes
- Performance benchmarking at each phase
- Rollback plan using git branch strategy

## Success Definition

The consolidation is successful when:
1. **Zero Functional Regression**: All existing functionality works identically
2. **Significant Code Reduction**: 60-80% less duplicate code in FFI layers
3. **Consistent Behavior**: Identical error handling and return values across interfaces
4. **Improved Maintainability**: Single point of change for business logic updates
5. **Performance Maintained**: No measurable performance degradation
6. **Future-Ready**: Architecture easily accommodates new Wasmtime API additions