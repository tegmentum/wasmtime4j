# PRD: Native FFI Utility Sharing

## Overview

Create a simple, pragmatic approach to eliminate code duplication between JNI and Panama FFI implementations while maintaining compilation integrity and code clarity.

## Background

### Previous Attempt Analysis
The `native-impletation-consolidation` epic attempted a complex trait-based shared architecture that resulted in:
- ✅ **Design Success**: Successfully identified and analyzed code duplication
- ❌ **Implementation Failure**: 135+ compilation errors due to over-engineered trait system
- ❌ **Integration Failure**: Complex architecture incompatible with existing codebase structure

### Key Learnings
1. **Complex trait systems** create more problems than they solve for FFI code sharing
2. **Incremental changes** are safer than wholesale architectural rewrites
3. **Simple utility functions** are more maintainable than complex abstractions
4. **Compilation integrity** must be maintained throughout development

## Problem Statement

The wasmtime4j native module contains significant code duplication between:
- `wasmtime4j-native/src/jni_bindings.rs` - JNI interface (Java 8-22 compatibility)
- `wasmtime4j-native/src/panama_ffi.rs` - Panama FFI interface (Java 23+)

**Specific Duplication Examples:**
- Engine configuration parameter conversion logic (44+ lines duplicated)
- Module compilation parameter handling 
- Store and instance creation patterns
- Error handling and validation code
- Memory management utilities

## Success Criteria

### Primary Goals
1. **Eliminate Code Duplication**: Remove duplicate logic between JNI and Panama implementations
2. **Maintain Compilation**: Code must compile successfully at every step
3. **Preserve Functionality**: No regressions in existing behavior
4. **Simple Architecture**: Use straightforward utility functions, not complex traits

### Secondary Goals
1. **Improve Maintainability**: Changes only need to be made in one place
2. **Reduce Complexity**: Simpler code is easier to understand and debug
3. **Enable Future Growth**: Make it easier to add new FFI operations

## Proposed Solution

### Architecture: Shared Utility Functions

Create a simple `ffi_common` module with utility functions that both JNI and Panama implementations can use:

```rust
// wasmtime4j-native/src/ffi_common.rs
pub mod parameter_conversion {
    // Simple utility functions for common parameter conversions
    pub fn convert_engine_config(/* params */) -> EngineConfig { /* ... */ }
    pub fn convert_module_params(/* params */) -> ModuleParams { /* ... */ }
    pub fn convert_store_params(/* params */) -> StoreParams { /* ... */ }
}

pub mod error_handling {
    // Common error handling utilities
    pub fn handle_wasmtime_error(error: WasmtimeError) -> ErrorInfo { /* ... */ }
    pub fn validate_pointer<T>(ptr: *const T, name: &str) -> Result<(), Error> { /* ... */ }
}

pub mod memory_utils {
    // Memory management utilities
    pub fn safe_deref<T>(ptr: *mut T, name: &str) -> Result<&mut T, Error> { /* ... */ }
    pub fn safe_box_from_raw<T>(ptr: *mut T) -> Result<Box<T>, Error> { /* ... */ }
}
```

### Implementation Strategy

#### Phase 1: Foundation (1 week)
**Issue**: Create shared utility module structure
- Create `wasmtime4j-native/src/ffi_common.rs`
- Add basic module structure with empty functions
- Update `lib.rs` to include the new module
- Verify compilation passes

#### Phase 2: Parameter Conversion (1 week)
**Issue**: Extract common parameter conversion functions
- Identify duplicated parameter conversion logic
- Create shared conversion functions in `ffi_common::parameter_conversion`
- Replace duplicated code in JNI bindings with function calls
- Replace duplicated code in Panama FFI with function calls
- Test that both implementations work identically

#### Phase 3: Error Handling (1 week)  
**Issue**: Extract common error handling utilities
- Create shared error handling functions in `ffi_common::error_handling`
- Replace duplicated error handling code
- Ensure consistent error reporting across both interfaces

#### Phase 4: Memory Utilities (1 week)
**Issue**: Extract common memory management functions  
- Create shared memory utilities in `ffi_common::memory_utils`
- Replace duplicated pointer validation and memory management
- Ensure memory safety is maintained

#### Phase 5: Testing & Validation (1 week)
**Issue**: Comprehensive testing of shared utilities
- Run full test suite for both JNI and Panama implementations
- Validate that behavior is identical
- Performance testing to ensure no regressions
- Documentation updates

## Key Principles

### 1. **Incremental Approach**
- Make small, testable changes
- Maintain compilation at every step  
- One utility module at a time

### 2. **Simple Design**
- Use plain functions, not complex traits
- Clear function names and parameters
- Minimal abstractions

### 3. **Interface Preservation**
- Keep JNI and Panama implementations separate
- Don't change external APIs
- Maintain existing calling conventions

### 4. **Safety First**
- All shared utilities must be memory-safe
- Comprehensive error handling
- Defensive programming practices

## Implementation Details

### Module Structure
```
wasmtime4j-native/src/
├── ffi_common.rs              # New shared utilities
├── jni_bindings.rs            # Uses shared utilities  
├── panama_ffi.rs              # Uses shared utilities
├── engine.rs                  # Core engine logic (unchanged)
├── module.rs                  # Core module logic (unchanged)  
├── store.rs                   # Core store logic (unchanged)
└── error.rs                   # Enhanced with shared utilities
```

### Example Transformation

**Before (Duplicated):**
```rust
// In jni_bindings.rs
let strategy_opt = match strategy {
    0 => Some(Strategy::Cranelift),
    _ => None,
};

// In panama_ffi.rs  
let strategy_opt = match strategy {
    0 => Some(Strategy::Cranelift),
    _ => None,
};
```

**After (Shared):**
```rust  
// In ffi_common.rs
pub fn convert_strategy(strategy: i32) -> Option<Strategy> {
    match strategy {
        0 => Some(Strategy::Cranelift),
        _ => None,
    }
}

// In both jni_bindings.rs and panama_ffi.rs
let strategy_opt = ffi_common::parameter_conversion::convert_strategy(strategy);
```

## Risk Mitigation

### Technical Risks
1. **Compilation Issues**: Mitigated by incremental approach and continuous testing
2. **Performance Regression**: Mitigated by keeping functions simple and inline-able  
3. **Memory Safety**: Mitigated by comprehensive testing and defensive programming

### Development Risks
1. **Scope Creep**: Mitigated by focusing only on elimination of duplication
2. **Over-Engineering**: Mitigated by principle of simple utility functions
3. **Integration Issues**: Mitigated by preserving existing interfaces

## Testing Strategy

### Compilation Testing
- Verify compilation passes after each change
- Test both JNI and Panama feature flags
- Cross-platform compilation testing

### Functional Testing
- Run existing test suites after each phase
- Verify identical behavior between JNI and Panama
- Edge case and error path testing

### Integration Testing  
- Test with actual Java applications
- Performance benchmarking
- Memory usage analysis

## Success Metrics

### Quantitative Metrics
- **Lines of Code Reduction**: Target 20-30% reduction in duplicated code
- **Compilation**: Must compile successfully throughout development
- **Test Pass Rate**: 100% of existing tests must continue to pass
- **Performance**: No more than 5% performance regression

### Qualitative Metrics
- **Maintainability**: Code changes only need to be made in one place
- **Code Clarity**: Shared utilities are well-documented and easy to understand
- **Architecture Simplicity**: No complex abstractions or traits

## Timeline

**Total Duration**: 5 weeks

- **Week 1**: Foundation and module structure
- **Week 2**: Parameter conversion utilities
- **Week 3**: Error handling utilities  
- **Week 4**: Memory management utilities
- **Week 5**: Testing, validation, and documentation

## Approval Criteria

This PRD is ready for implementation when:
1. ✅ Architecture approach is simple and pragmatic
2. ✅ Implementation plan is incremental and safe
3. ✅ Risk mitigation strategies are comprehensive
4. ✅ Success criteria are measurable and achievable

## Lessons Applied

From the failed `native-impletation-consolidation` epic:
- ❌ **Don't**: Create complex trait-based architectures
- ❌ **Don't**: Make wholesale changes that break compilation
- ❌ **Don't**: Over-engineer solutions for simple problems
- ✅ **Do**: Use simple utility functions  
- ✅ **Do**: Make incremental, testable changes
- ✅ **Do**: Maintain compilation integrity throughout development
- ✅ **Do**: Focus on practical code sharing over architectural purity

---

**Next Steps**: Review and approve this PRD, then proceed with implementation using the incremental approach outlined above.