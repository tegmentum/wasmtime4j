# Task: Global Variables Completion

## Description
Complete mutable/immutable global support with cross-module sharing and type validation.

## Implementation Details
- **Mutable Globals**: Complete support for mutable global variables
- **Immutable Globals**: Proper handling of immutable global constants
- **Type Validation**: Global type validation and conversion
- **Cross-Module Sharing**: Global variable sharing between module instances
- **JNI Global Completion**: Complete Global native method implementations
- **Panama Global Completion**: Complete Global foreign function bindings

## Acceptance Criteria
- [ ] Mutable global variables can be read and written correctly
- [ ] Immutable globals prevent modification attempts with proper errors
- [ ] Global type validation enforces WebAssembly type constraints
- [ ] Cross-module global sharing works correctly
- [ ] Global value conversion handles all WebAssembly types
- [ ] Both JNI and Panama implementations provide identical functionality
- [ ] Global cleanup properly manages resources

## Dependencies
- Store Context Implementation (Task 001)
- Instance Management Completion (Task 004)
- Existing Global interface definitions

## Definition of Done
- Global operations pass type validation tests
- Mutability constraints are properly enforced
- Cross-module sharing works as expected
- Resource management prevents leaks