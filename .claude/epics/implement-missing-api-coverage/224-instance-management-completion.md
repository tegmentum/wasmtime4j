# Task: Instance Management Completion

## Description
Complete instantiation with import resolution, export binding, and host function integration.

## Implementation Details
- **Import Resolution**: Complete module import resolution with type checking
- **Export Binding**: Full export binding with proper type validation
- **Host Function Integration**: Instance-level host function binding
- **Resource Management**: Instance lifecycle management and cleanup
- **JNI Instance Completion**: Complete Instance native method implementations
- **Panama Instance Completion**: Complete Instance foreign function bindings

## Acceptance Criteria
- [ ] Module instantiation works with proper import resolution
- [ ] Export binding provides access to all module exports
- [ ] Host function imports are resolved correctly during instantiation
- [ ] Instance cleanup properly manages all native resources
- [ ] Type validation prevents invalid import/export bindings
- [ ] Error handling covers all instantiation failure modes
- [ ] Both JNI and Panama implementations provide identical functionality

## Dependencies
- Store Context Implementation (Task 001)
- Host Function Binding System (Task 002)
- Module Operations Completion (Task 003)

## Definition of Done
- Instance creation and management passes all test scenarios
- Import/export binding works for all supported types
- Resource management prevents memory leaks
- Error handling matches Wasmtime behavior exactly