---
name: native-ffi-utility-sharing
status: backlog
created: 2025-09-05T01:16:07Z
progress: 0%
prd: .claude/prds/native-ffi-utility-sharing.md
github: https://github.com/tegmentum/wasmtime4j/issues/174
---

# Epic: Native FFI Utility Sharing

## Overview

Eliminate code duplication between JNI and Panama FFI implementations using simple shared utility functions. This pragmatic approach creates a `ffi_common` module with parameter conversion, error handling, and memory management utilities that both interfaces can use while maintaining compilation integrity and avoiding complex abstractions.

## Architecture Decisions

- **Simple Utility Functions**: Use plain functions instead of complex trait systems based on lessons from failed consolidation epic
- **Incremental Implementation**: Make small, testable changes maintaining compilation at every step
- **Module Separation**: Keep JNI and Panama implementations separate while sharing common utilities
- **Zero Abstraction Penalty**: Ensure utility functions can be inlined for zero performance overhead
- **Memory Safety First**: All shared utilities must maintain Rust's memory safety guarantees

## Technical Approach

### Shared Utility Module Structure
```rust
// wasmtime4j-native/src/ffi_common.rs
pub mod parameter_conversion {
    // Engine configuration conversions
    // Module parameter conversions  
    // Store/instance parameter conversions
}

pub mod error_handling {
    // Consistent error reporting utilities
    // Pointer validation functions
    // Error code standardization
}

pub mod memory_utils {
    // Safe pointer dereferencing
    // Memory lifecycle management
    // Bounds checking utilities
}
```

### Integration Strategy
- **JNI Bindings**: Replace duplicated conversion logic with `ffi_common` function calls
- **Panama FFI**: Replace duplicated conversion logic with `ffi_common` function calls
- **Core Modules**: Existing engine, module, store modules remain unchanged
- **Error Module**: Enhanced to work with shared utilities

### Code Transformation Pattern
Transform duplicated parameter conversion from:
```rust
// Duplicated in both JNI and Panama
let strategy_opt = match strategy {
    0 => Some(Strategy::Cranelift),
    _ => None,
};
```

To shared utility:
```rust
// In ffi_common.rs
pub fn convert_strategy(strategy: i32) -> Option<Strategy> { ... }

// In both interfaces
let strategy_opt = ffi_common::parameter_conversion::convert_strategy(strategy);
```

## Implementation Strategy

### Phase-Based Development
1. **Foundation**: Create module structure without breaking existing code
2. **Parameter Conversion**: Extract and share parameter handling logic
3. **Error Handling**: Consolidate error reporting and validation
4. **Memory Utilities**: Share memory management functions
5. **Testing**: Comprehensive validation and performance verification

### Risk Mitigation
- **Compilation Safety**: Test compilation after every change
- **Functional Preservation**: Maintain identical behavior between interfaces
- **Performance Monitoring**: Ensure no regressions through benchmarking
- **Incremental Rollback**: Each phase can be reverted independently if issues arise

## Task Breakdown Preview

High-level task categories for implementation:
- [ ] **Foundation Module**: Create `ffi_common.rs` with basic structure and module integration
- [ ] **Parameter Conversion**: Extract engine, module, and store parameter conversion utilities
- [ ] **Error Handling**: Consolidate error reporting and pointer validation functions
- [ ] **Memory Utilities**: Share memory management and lifecycle functions  
- [ ] **Integration & Testing**: Replace duplicated code and validate identical behavior

## Dependencies

- **Internal**: Access to existing JNI and Panama FFI implementations
- **External**: No external service dependencies
- **Prerequisite**: Understanding of current duplication patterns from failed consolidation epic analysis
- **Compiler**: Rust compiler with inline optimization support

## Success Criteria (Technical)

### Performance Benchmarks
- **Zero Overhead**: Shared utilities must inline to equivalent machine code
- **Memory Usage**: No increase in memory footprint
- **Call Overhead**: Less than 1% performance impact on FFI operations

### Quality Gates
- **Compilation**: 100% compilation success throughout development
- **Test Coverage**: All existing tests pass with identical behavior
- **Code Reduction**: 20-30% reduction in duplicated lines of code
- **Documentation**: All shared utilities fully documented with examples

### Acceptance Criteria
- JNI and Panama implementations produce identical results for all operations
- No regressions in functionality, performance, or memory safety
- Code changes only required in one place for future FFI operations
- Simple, maintainable architecture that avoids over-engineering

## Estimated Effort

- **Overall Timeline**: 5 weeks
- **Resource Requirements**: 1 Rust developer with FFI experience
- **Critical Path**: Sequential implementation phases to maintain compilation integrity
- **Total Complexity**: Low-medium (simple utility extraction vs. architectural overhaul)

**Phase Breakdown**:
- Week 1: Foundation and structure (Low risk)
- Week 2: Parameter conversion (Medium risk - core logic changes)
- Week 3: Error handling (Low risk - utility consolidation)
- Week 4: Memory utilities (Medium risk - safety critical)
- Week 5: Testing and validation (Low risk - verification)

**Risk Assessment**: Low overall risk due to incremental approach and lessons learned from failed complex consolidation attempt.