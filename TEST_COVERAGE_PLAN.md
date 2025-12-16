# Test Coverage Implementation Plan

**Created:** 2024-12-14
**Status:** In Progress

## Executive Summary

The wasmtime4j project has significant test coverage gaps, particularly in the public API module. This document outlines a prioritized implementation plan to achieve comprehensive test coverage.

### Current Coverage Statistics

| Module | Source Classes | Test Files | Coverage |
|--------|---------------|------------|----------|
| wasmtime4j (Public API) | ~402 | 36 | ~9% |
| wasmtime4j-jni | ~100+ | 34 | ~34% |
| wasmtime4j-panama | ~100+ | 29 | ~29% |

---

## Phase 1: Core API (P0 - Critical)

These are the fundamental classes that every user interacts with. Testing these is essential for basic functionality validation.

### 1.1 Engine Interface
- [ ] `EngineTest.java` - wasmtime4j module
  - [ ] Test engine creation with default config
  - [ ] Test engine creation with custom config
  - [ ] Test engine close/resource cleanup
  - [ ] Test engine configuration options
  - [ ] Test engine singleton behavior (if applicable)

### 1.2 Module Interface
- [ ] `ModuleTest.java` - wasmtime4j module
  - [ ] Test module creation from bytes
  - [ ] Test module creation from file path
  - [ ] Test module validation
  - [ ] Test module serialization/deserialization
  - [ ] Test invalid module rejection
  - [ ] Test module imports enumeration
  - [ ] Test module exports enumeration

### 1.3 Store Interface
- [ ] `StoreTest.java` - wasmtime4j module
  - [ ] Test store creation with engine
  - [ ] Test store with custom data
  - [ ] Test store fuel management
  - [ ] Test store epoch deadline
  - [ ] Test store limits configuration
  - [ ] Test store close/resource cleanup

### 1.4 Instance Interface
- [ ] `InstanceTest.java` - wasmtime4j module
  - [ ] Test instance creation from module
  - [ ] Test instance with imports
  - [ ] Test instance export retrieval
  - [ ] Test instance function invocation
  - [ ] Test instance memory access
  - [ ] Test instance table access
  - [ ] Test instance global access

### 1.5 Function Interface
- [ ] `FunctionTest.java` - wasmtime4j module
  - [ ] Test function type inspection
  - [ ] Test function invocation with no params
  - [ ] Test function invocation with params
  - [ ] Test function invocation with return values
  - [ ] Test function invocation with multiple returns
  - [ ] Test function trap handling

### 1.6 Memory Interface
- [ ] `MemoryTest.java` - wasmtime4j module
  - [ ] Test memory creation
  - [ ] Test memory read operations (byte, short, int, long, float, double)
  - [ ] Test memory write operations
  - [ ] Test memory grow operation
  - [ ] Test memory size inspection
  - [ ] Test memory bounds checking
  - [ ] Test memory bulk operations

### 1.7 Table Interface
- [ ] `TableTest.java` - wasmtime4j module
  - [ ] Test table creation
  - [ ] Test table get/set operations
  - [ ] Test table grow operation
  - [ ] Test table size inspection
  - [ ] Test table element types

### 1.8 Global Interface
- [ ] `GlobalTest.java` - wasmtime4j module
  - [ ] Test global creation (mutable/immutable)
  - [ ] Test global get operation
  - [ ] Test global set operation (mutable)
  - [ ] Test global type inspection
  - [ ] Test immutable global set rejection

### 1.9 Linker Interface
- [ ] `LinkerTest.java` - wasmtime4j module
  - [ ] Test linker creation
  - [ ] Test define function
  - [ ] Test define memory
  - [ ] Test define table
  - [ ] Test define global
  - [ ] Test define module
  - [ ] Test instantiate with linker
  - [ ] Test WASI integration

### 1.10 HostFunction Interface
- [ ] `HostFunctionTest.java` - wasmtime4j module
  - [ ] Test host function creation
  - [ ] Test host function with no params
  - [ ] Test host function with params
  - [ ] Test host function with return values
  - [ ] Test host function exception handling
  - [ ] Test host function caller access

### 1.11 Caller Interface
- [ ] `CallerTest.java` - wasmtime4j module
  - [ ] Test caller store access
  - [ ] Test caller export lookup
  - [ ] Test caller memory access
  - [ ] Test caller data access

### 1.12 WasmRuntime Interface
- [ ] `WasmRuntimeTest.java` - wasmtime4j module
  - [ ] Test runtime type detection
  - [ ] Test runtime version info
  - [ ] Test runtime capabilities

---

## Phase 2: Type System (P1 - High Priority)

### 2.1 Value Types
- [ ] `ValTypeTest.java`
  - [ ] Test all primitive types (i32, i64, f32, f64)
  - [ ] Test reference types (funcref, externref)
  - [ ] Test SIMD types (v128)
  - [ ] Test type equality and hashCode

### 2.2 Function Types
- [ ] `FuncTypeTest.java`
  - [ ] Test function type creation
  - [ ] Test parameter types inspection
  - [ ] Test result types inspection
  - [ ] Test function type equality

### 2.3 Memory Types
- [ ] `MemoryTypeTest.java`
  - [ ] Test memory type with limits
  - [ ] Test 32-bit vs 64-bit memory
  - [ ] Test shared memory type

### 2.4 Table Types
- [ ] `TableTypeTest.java`
  - [ ] Test table type with limits
  - [ ] Test element type inspection

### 2.5 Global Types
- [ ] `GlobalTypeTest.java`
  - [ ] Test global type creation
  - [ ] Test mutability inspection
  - [ ] Test value type inspection

### 2.6 Import/Export Descriptors
- [ ] `ImportDescriptorTest.java`
  - [ ] Test import module/name inspection
  - [ ] Test import type inspection
- [ ] `ExportDescriptorTest.java`
  - [ ] Test export name inspection
  - [ ] Test export type inspection

---

## Phase 3: Component Model (P1 - High Priority)

### 3.1 Component Interface
- [ ] `ComponentTest.java`
  - [ ] Test component creation from bytes
  - [ ] Test component validation
  - [ ] Test component imports/exports

### 3.2 ComponentLinker Interface
- [ ] `ComponentLinkerTest.java`
  - [ ] Test component linker creation
  - [ ] Test define root functions
  - [ ] Test define instance
  - [ ] Test instantiate component

### 3.3 ComponentInstance Interface
- [ ] `ComponentInstanceTest.java`
  - [ ] Test component instance creation
  - [ ] Test export retrieval
  - [ ] Test function invocation

### 3.4 Component Values
- [ ] `ComponentValTest.java`
  - [ ] Test all primitive component values
  - [ ] Test string values
  - [ ] Test list values
  - [ ] Test record values
  - [ ] Test variant values
  - [ ] Test option values
  - [ ] Test result values

---

## Phase 4: Exception Classes (P2 - Medium Priority)

### 4.1 Core Exceptions
- [ ] `TrapExceptionTest.java`
  - [ ] Test trap code inspection
  - [ ] Test trap message
  - [ ] Test trap backtrace
- [ ] `CompilationExceptionTest.java`
  - [ ] Test compilation error details
- [ ] `ValidationExceptionTest.java`
  - [ ] Test validation error details
- [ ] `LinkingExceptionTest.java`
  - [ ] Test linking error details
- [ ] `InstantiationExceptionTest.java`
  - [ ] Test instantiation error details
- [ ] `WasmtimeExceptionTest.java`
  - [ ] Test base exception behavior

---

## Phase 5: WASI (P2 - Medium Priority)

### 5.1 WASI Core
- [ ] `WasiContextTest.java`
  - [ ] Test context creation
  - [ ] Test context configuration
- [ ] `WasiConfigTest.java`
  - [ ] Test config builder
  - [ ] Test environment variables
  - [ ] Test arguments
  - [ ] Test preopened directories
  - [ ] Test stdin/stdout/stderr configuration

### 5.2 WASI Filesystem
- [ ] `WasiFilesystemTest.java`
  - [ ] Test file open/close
  - [ ] Test file read/write
  - [ ] Test directory operations
  - [ ] Test path resolution

### 5.3 WASI Linker
- [ ] `WasiLinkerTest.java`
  - [ ] Test WASI preview1 imports
  - [ ] Test WASI preview2 imports

---

## Phase 6: Advanced Features (P3 - Lower Priority)

### 6.1 GC/Reference Types
- [ ] `GcRuntimeTest.java`
- [ ] `GcRefTest.java`
- [ ] `StructTypeTest.java`
- [ ] `ArrayTypeTest.java`

### 6.2 Execution Control
- [ ] `ResourceLimiterTest.java`
- [ ] `FuelCallbackHandlerTest.java`
- [ ] `ExecutionControllerTest.java`

### 6.3 Async/Threading
- [ ] `AsyncRuntimeTest.java`
- [ ] `WasmThreadTest.java`

### 6.4 Debug/Profile
- [ ] `DebuggerTest.java`
- [ ] `ProfilerTest.java`

---

## Phase 7: JNI Implementation Tests (P2)

### 7.1 Missing JNI Tests
- [ ] `JniEngineTest.java`
- [ ] `JniModuleTest.java`
- [ ] `JniComponentTest.java`
- [ ] `JniSerializerTest.java`
- [ ] `JniTypedFuncTest.java`

---

## Phase 8: Panama Implementation Tests (P2)

### 8.1 Missing Panama Tests
- [ ] `PanamaComponentTest.java`
- [ ] `PanamaWasiComponentTest.java`
- [ ] `PanamaSerializerTest.java`
- [ ] `PanamaTypedFuncTest.java`
- [ ] `PanamaWasiContextTest.java`

---

## Implementation Guidelines

### Test Structure

Each test class should follow this structure:

```java
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ClassName Tests")
class ClassNameTest {

    @BeforeEach
    void setUp() {
        // Setup code
    }

    @AfterEach
    void tearDown() {
        // Cleanup code
    }

    @Nested
    @DisplayName("Feature Group")
    class FeatureGroup {
        @Test
        @DisplayName("should do something when condition")
        void shouldDoSomethingWhenCondition() {
            // Arrange
            // Act
            // Assert
        }
    }
}
```

### Test Naming Convention

- Test classes: `{ClassName}Test.java`
- Test methods: `should{ExpectedBehavior}When{Condition}`
- Use `@DisplayName` for readable test names

### Test Categories

Use JUnit 5 tags for categorization:
- `@Tag("unit")` - Unit tests (fast, no external dependencies)
- `@Tag("integration")` - Integration tests (may require native library)
- `@Tag("slow")` - Slow tests

### Assertions

- Use AssertJ for fluent assertions where beneficial
- Always include meaningful failure messages
- Test both positive and negative cases

### Resource Management

- Always clean up native resources in `@AfterEach`
- Use try-with-resources where applicable
- Test resource cleanup explicitly

---

## Progress Tracking

### Phase 1 Progress: 0/12 complete
### Phase 2 Progress: 0/6 complete
### Phase 3 Progress: 0/4 complete
### Phase 4 Progress: 0/6 complete
### Phase 5 Progress: 0/4 complete
### Phase 6 Progress: 0/8 complete
### Phase 7 Progress: 0/5 complete
### Phase 8 Progress: 0/5 complete

**Total Progress: 0/50 test classes**

---

## Next Steps

1. Start with Phase 1 (Core API) - these tests are most critical
2. Begin with `EngineTest.java` as it's the entry point
3. Progress through Module -> Store -> Instance -> Function flow
4. Each test should compile and pass before moving to next

---

## Notes

- The public API module (`wasmtime4j`) contains interfaces, not implementations
- Tests should verify interface contracts without depending on specific implementations
- Consider using test fixtures for common WASM module bytes
- Integration tests may require the native library to be available
