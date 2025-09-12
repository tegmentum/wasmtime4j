# Task: Host Function Binding System

## Description
Implement complete callback mechanism with type conversion and error propagation for host functions callable from WebAssembly.

## Implementation Details
- **Callback Mechanism**: Full host function callback system in wasmtime4j-native
- **Type Conversion**: Type-safe parameter marshaling between Java and WebAssembly types
- **Error Propagation**: Bidirectional error handling (Java ↔ WebAssembly)
- **JNI Host Functions**: Complete callback implementation for JNI runtime
- **Panama Host Functions**: Complete callback implementation for Panama runtime
- **Linker Integration**: Host function registration and namespace management

## Acceptance Criteria
- [ ] Host functions defined in Java can be called from WebAssembly modules
- [ ] Parameter conversion works correctly for all WebAssembly types (i32, i64, f32, f64)
- [ ] Return value handling works for all supported types
- [ ] Java exceptions properly propagate as WebAssembly traps
- [ ] WebAssembly traps properly convert to Java exceptions
- [ ] Host functions can access WebAssembly memory and globals
- [ ] Linker system works for import resolution and registration

## Dependencies
- Store Context Implementation (Task 001)
- Existing Module and Instance interfaces
- Native resource management patterns

## Definition of Done
- Host function integration passes end-to-end tests
- Type conversion handles all edge cases correctly
- Error propagation maintains exact Wasmtime semantics
- Performance meets acceptable callback overhead requirements