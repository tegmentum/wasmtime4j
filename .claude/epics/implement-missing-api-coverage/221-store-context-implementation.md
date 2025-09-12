# Task: Store Context Implementation

## Description
Complete execution context with resource management for both JNI and Panama runtimes. The Store is the critical blocker preventing all WebAssembly execution.

## Implementation Details
- **Native Store Context**: Implement complete Wasmtime Store wrapper in wasmtime4j-native Rust library
- **JNI Integration**: Complete Store native method implementations for wasmtime4j-jni
- **Panama Integration**: Complete Store foreign function bindings for wasmtime4j-panama
- **Resource Management**: Proper lifetime management, resource tracking, and GC integration
- **Thread Safety**: Ensure Store contexts are thread-safe with proper synchronization

## Acceptance Criteria
- [ ] Native Store implementation handles resource lifecycle correctly
- [ ] JNI Store methods work with proper parameter validation and error handling
- [ ] Panama Store bindings function identically to JNI implementation
- [ ] Store can be created, used for module instantiation, and properly cleaned up
- [ ] All error conditions map to exact Wasmtime error semantics
- [ ] Comprehensive tests for both runtimes with realistic usage scenarios

## Dependencies
- Wasmtime 36.0.2 Store C API
- Existing wasmtime4j-native build system
- Current JNI and Panama module structure

## Definition of Done
- Store implementation passes 100% of test cases
- Both JNI and Panama achieve identical functionality
- Memory management prevents resource leaks
- Performance within 20% of native Wasmtime Store operations