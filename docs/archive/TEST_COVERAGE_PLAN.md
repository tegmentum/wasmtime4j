# Test Coverage Implementation Plan

**Created:** 2024-12-14
**Updated:** 2024-12-19
**Status:** ROUND 1 COMPLETE - ROUND 2 IN PROGRESS

## Executive Summary

Round 1 test coverage implementation is complete (Phases 1-13). Round 2 focuses on critical coverage gaps identified through comprehensive analysis.

### Current Coverage Statistics (Updated 2024-12-19)

| Module | Source Files | Test Files | File Ratio |
|--------|--------------|------------|------------|
| wasmtime4j (Public API) | 663 | 174 | 26.2% |
| wasmtime4j-jni | 184 | 58 | 31.5% |
| wasmtime4j-panama | 186 | 54 | 29.0% |
| **TOTAL** | **1033** | **286** | **27.7%** |

---

# ROUND 2: Test Coverage Expansion

## Critical Gaps Identified

### wasmtime4j Module (663 source files)

**ZERO TEST COVERAGE (39 files across 10 packages):**
| Package | Source Files | Status |
|---------|--------------|--------|
| `compilation/` | 2 | ❌ 0% |
| `experimental/` | 6 | ❌ 0% |
| `wasi/cli/` | 4 | ❌ 0% |
| `wasi/clocks/` | 5 | ❌ 0% |
| `wasi/filesystem/` | 6 | ❌ 0% |
| `wasi/http/` | 5 | ❌ 0% |
| `wasi/io/` | 5 | ❌ 0% |
| `wasi/random/` | 1 | ❌ 0% |
| `wasi/threads/` | 5 | ❌ 0% |

**SEVERELY UNDERTESTED (<20% coverage):**
| Package | Source Files | Test Files | Coverage |
|---------|--------------|------------|----------|
| Root API (`ai.tegmentum.wasmtime4j`) | 266 | 35 | 13% |
| `gc/` | 34 | 5 | 14% |
| `wit/` | 28 | 5 | 17% |
| `performance/` | 14 | 1 | 7% |
| `crypto/` | 16 | 1 | 6% |

### wasmtime4j-jni Module (184 source files)

**ZERO TEST COVERAGE (21 files across 8 WASI packages):**
| Package | Source Files | Status |
|---------|--------------|--------|
| `wasi/cli/` | 3 | ❌ 0% |
| `wasi/clocks/` | 3 | ❌ 0% |
| `wasi/exception/` | 4 | ❌ 0% |
| `wasi/io/` | 3 | ❌ 0% |
| `wasi/keyvalue/` | 1 | ❌ 0% |
| `wasi/permission/` | 2 | ❌ 0% |
| `wasi/random/` | 1 | ❌ 0% |
| `wasi/threads/` | 4 | ❌ 0% |

**SEVERELY UNDERTESTED (<25% coverage):**
| Package | Source Files | Test Files | Coverage |
|---------|--------------|------------|----------|
| `util/` | 11 | 1 | 9.1% |
| `wasi/` (overall) | 81 | 7 | 8.6% |
| `debug/` | 7 | 1 | 14.3% |
| `type/` | 6 | 1 | 16.7% |

### wasmtime4j-panama Module (186 source files)

**ZERO TEST COVERAGE (22 files across 10 packages):**
| Package | Source Files | Status |
|---------|--------------|--------|
| `wasi/cli/` | 3 | ❌ 0% |
| `wasi/clocks/` | 3 | ❌ 0% |
| `wasi/exception/` | 4 | ❌ 0% |
| `wasi/threads/` | 4 | ❌ 0% |
| `wasi/keyvalue/` | 1 | ❌ 0% |
| `wasi/permission/` | 2 | ❌ 0% |
| `wasi/random/` | 1 | ❌ 0% |
| `wasi/security/` | 1 | ❌ 0% |
| `exception/` | 2 | ❌ 0% |
| `wit/` | 1 | ❌ 0% |

---

## Round 2 Implementation Plan

### Phase R2.1: Root API Package Expansion (P0 - Critical)
**Target: wasmtime4j root package (266 source files, 35 tests → 13% coverage)**

This is the most critical gap - the core API surface that every user interacts with.

#### R2.1.1 Core Classes Additional Tests
- [ ] Additional Store tests (fuel management, epoch handling)
- [ ] Additional Engine tests (configuration options, caching)
- [ ] Additional Module tests (serialization edge cases)
- [ ] Additional Instance tests (complex import/export scenarios)
- [ ] Additional Memory tests (bulk operations, shared memory)
- [ ] Additional Table tests (function references, element types)
- [ ] Additional Global tests (mutable/immutable combinations)

#### R2.1.2 Missing Root Package Tests
Identify and test untested classes in the root package.

### Phase R2.2: WASI Subsystem Tests (P0 - Critical)
**Target: Complete WASI coverage across all modules**

#### R2.2.1 wasmtime4j WASI Tests
- [ ] `WasiCliTest.java` - CLI argument/environment handling
- [ ] `WasiClocksTest.java` - Clock and time operations
- [ ] `WasiFilesystemTest.java` - Additional filesystem operations
- [ ] `WasiHttpTest.java` - HTTP request/response handling
- [ ] `WasiIoTest.java` - Stream and I/O operations
- [ ] `WasiRandomTest.java` - Random number generation
- [ ] `WasiThreadsTest.java` - Threading primitives

#### R2.2.2 wasmtime4j-jni WASI Tests
- [ ] `JniWasiCliTest.java`
- [ ] `JniWasiClocksTest.java`
- [ ] `JniWasiExceptionTest.java`
- [ ] `JniWasiIoTest.java`
- [ ] `JniWasiKeyvalueTest.java`
- [ ] `JniWasiPermissionTest.java`
- [ ] `JniWasiRandomTest.java`
- [ ] `JniWasiThreadsTest.java`

#### R2.2.3 wasmtime4j-panama WASI Tests
- [ ] `PanamaWasiCliTest.java`
- [ ] `PanamaWasiClocksTest.java`
- [ ] `PanamaWasiExceptionTest.java`
- [ ] `PanamaWasiThreadsTest.java`
- [ ] `PanamaWasiKeyvalueTest.java`
- [ ] `PanamaWasiPermissionTest.java`
- [ ] `PanamaWasiRandomTest.java`
- [ ] `PanamaWasiSecurityTest.java`

### Phase R2.3: Compilation & Experimental Packages (P1 - High)
**Target: compilation/ and experimental/ packages**

#### R2.3.1 Compilation Package
- [ ] `CompilationConfigTest.java`
- [ ] `CompilerOptionsTest.java`

#### R2.3.2 Experimental Package
- [ ] `ExperimentalFeatureTest.java`
- [ ] `ExperimentalConfigTest.java`
- [ ] `WasmGcTest.java`
- [ ] `TailCallTest.java`
- [ ] `MemoryControlTest.java`
- [ ] `BranchHintingTest.java`

### Phase R2.4: GC Runtime Tests (P1 - High)
**Target: gc/ package (34 files, 5 tests → 14% coverage)**

- [ ] Additional GcRuntimeTest.java scenarios
- [ ] `GcStructTest.java`
- [ ] `GcArrayTest.java`
- [ ] `GcRefCastTest.java`
- [ ] `GcSubtypingTest.java`
- [ ] `I31RefTest.java`
- [ ] `StructTypeAdditionalTest.java`
- [ ] `ArrayTypeAdditionalTest.java`

### Phase R2.5: WIT Component Interface Tests (P1 - High)
**Target: wit/ package (28 files, 5 tests → 17% coverage)**

- [ ] `WitTypeTest.java`
- [ ] `WitRecordTest.java`
- [ ] `WitVariantTest.java`
- [ ] `WitEnumTest.java`
- [ ] `WitFlagsTest.java`
- [ ] `WitListTest.java`
- [ ] `WitTupleTest.java`
- [ ] `WitOptionTest.java`
- [ ] `WitResultTest.java`
- [ ] `WitResourceTest.java`

### Phase R2.6: Performance Package Tests (P2 - Medium)
**Target: performance/ package (14 files, 1 test → 7% coverage)**

- [ ] `PerformanceMonitorTest.java`
- [ ] `MetricsCollectorTest.java`
- [ ] `PerformanceReportTest.java`
- [ ] `BenchmarkRunnerTest.java`
- [ ] `ProfilingConfigTest.java`

### Phase R2.7: Crypto Package Tests (P2 - Medium)
**Target: crypto/ package (16 files, 1 test → 6% coverage)**

- [ ] `CryptoContextTest.java`
- [ ] `HashAlgorithmTest.java`
- [ ] `SymmetricKeyTest.java`
- [ ] `AsymmetricKeyTest.java`
- [ ] `SignatureTest.java`
- [ ] `CipherTest.java`
- [ ] `SecureRandomTest.java`

### Phase R2.8: JNI Implementation Gaps (P2 - Medium)
**Target: wasmtime4j-jni undertested packages**

#### R2.8.1 JNI Util Package (11 files, 1 test → 9.1%)
- [ ] `JniPointerTest.java`
- [ ] `JniMemoryTest.java`
- [ ] `JniTypeConverterTest.java`
- [ ] `JniErrorMapperTest.java`
- [ ] `JniResourceTrackerTest.java`

#### R2.8.2 JNI Debug Package (7 files, 1 test → 14.3%)
- [ ] `JniDebuggerTest.java`
- [ ] `JniDebugSessionTest.java`
- [ ] `JniBreakpointTest.java`
- [ ] `JniStackFrameTest.java`

#### R2.8.3 JNI Type Package (6 files, 1 test → 16.7%)
- [ ] `JniValTypeTest.java`
- [ ] `JniFuncTypeTest.java`
- [ ] `JniMemoryTypeTest.java`
- [ ] `JniTableTypeTest.java`
- [ ] `JniGlobalTypeTest.java`

### Phase R2.9: Panama Implementation Gaps (P2 - Medium)
**Target: wasmtime4j-panama undertested packages**

#### R2.9.1 Panama Exception Package (2 files, 0 tests)
- [ ] `PanamaTrapExceptionTest.java`
- [ ] `PanamaRuntimeExceptionTest.java`

#### R2.9.2 Panama WIT Package (1 file, 0 tests)
- [ ] `PanamaWitBindingTest.java`

---

## Round 2 Progress Tracking

### Phase R2.1 Progress: 0/? complete ⏳
### Phase R2.2 Progress: 0/23 complete ⏳
### Phase R2.3 Progress: 0/8 complete ⏳
### Phase R2.4 Progress: 0/8 complete ⏳
### Phase R2.5 Progress: 0/10 complete ⏳
### Phase R2.6 Progress: 0/5 complete ⏳
### Phase R2.7 Progress: 0/7 complete ⏳
### Phase R2.8 Progress: 0/14 complete ⏳
### Phase R2.9 Progress: 0/3 complete ⏳

**Total Round 2 Tests Planned: ~78+ new test files**
**Target Coverage After Round 2: 35%+ file ratio**

---

# ROUND 1 COMPLETION SUMMARY (Phases 1-13)

### Completed Phases (Sessions 64-150+)

| Phase | Focus | Tests Created | Status |
|-------|-------|---------------|--------|
| Phase 1 | Core API | ~200 | ✅ COMPLETE |
| Phase 2 | Type System | ~90 | ✅ COMPLETE |
| Phase 3 | Exceptions | 268 | ✅ COMPLETE |
| Phase 4 | Exception Classes | 372 total | ✅ COMPLETE |
| Phase 5 | WASI | 160 | ✅ COMPLETE |
| Phase 6 | Advanced (Component/GC/Execution/Async/Debug) | 686 | ✅ COMPLETE |
| Phase 7 | JNI Implementation | 95 | ✅ COMPLETE |
| Phase 8 | Panama Implementation | 150 | ✅ COMPLETE |
| Phase 9.1 | Utility Package | ~30 | ✅ COMPLETE |
| Phase 9.2 | Debug Package Additional | ~50 | ✅ COMPLETE |
| Phase 10 | CoreDump Package | 176 | ✅ COMPLETE |
| Phase 11 | Pool & Cache Packages | 241 | ✅ COMPLETE |
| Phase 12 | Profiler & Reactive Packages | 53 | ✅ COMPLETE |
| Phase 13.1 | SIMD, Concurrent, Ref, Factory, SPI, Validation | 272 | ✅ COMPLETE |
| Phase 13.2 | Chaos, Disaster, Optimization, Distribution, Performance | 359 | ✅ COMPLETE |

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

### Phase 1 Progress: 12/12 complete ✅
### Phase 2 Progress: 6/6 complete ✅
### Phase 3 Progress: 4/4 complete ✅
### Phase 4 Progress: 6/6 complete ✅
### Phase 5 Progress: 4/4 complete ✅
### Phase 6 Progress: 8/8 complete ✅
### Phase 7 Progress: 5/5 complete ✅
### Phase 8 Progress: 5/5 complete ✅
### Phase 9.1 Progress: 5/5 complete ✅
### Phase 9.2 Progress: 21/21 complete ✅
### Phase 10 Progress: 10/10 complete ✅
### Phase 11 Progress: 12/12 complete ✅
### Phase 12 Progress: 5/5 complete ✅
### Phase 13 Progress: 8/8 complete ✅

**Total Progress: ALL PHASES COMPLETE (1-13)**

---

## Phase 9: Utility & Infrastructure (P1 - High Priority)

The following packages have 0% test coverage and represent critical infrastructure:

### 9.1 Utility Package (5 source files, 0 tests)
- [ ] `LibraryValidatorTest.java`
  - [ ] Test library validation logic
  - [ ] Test version checking
  - [ ] Test platform compatibility checks
- [ ] `HealthCheckTest.java`
  - [ ] Test health check endpoints
  - [ ] Test status reporting
- [ ] `ErrorRecoveryManagerTest.java`
  - [ ] Test error recovery strategies
  - [ ] Test fallback mechanisms
- [ ] `UnifiedExceptionMapperTest.java`
  - [ ] Test exception mapping logic
  - [ ] Test error code translation
- [ ] `StackTracePreserverTest.java`
  - [ ] Test stack trace preservation
  - [ ] Test native stack integration

### 9.2 Debug Package Additional (27 source files, 3 tests - 11% coverage)
Missing tests for:
- [ ] `DebuggerTest.java` - Core debugger interface
- [ ] `DebugSessionTest.java` - Debug session management
- [ ] `BreakpointTest.java` - Breakpoint creation/management
- [ ] `StackFrameTest.java` - Stack frame inspection
- [ ] `VariableTest.java` - Variable inspection
- [ ] `VariableValueTest.java` - Variable value types
- [ ] `ExecutionStateTest.java` - Execution state tracking
- [ ] `ExecutionTracerTest.java` - Execution tracing
- [ ] `WasmCoreDumpTest.java` - Core dump creation
- [ ] `GuestProfilerTest.java` - Guest profiling
- [ ] `DebugCapabilitiesTest.java` - Debug capability detection
- [ ] `DebugOptionsTest.java` - Debug option configuration
- [ ] `DebugInfoTest.java` - Debug info parsing
- [ ] `DwarfDebugInfoTest.java` - DWARF debug info
- [ ] `SourceMapIntegrationTest.java` - Source map integration
- [ ] `MemoryInfoTest.java` - Memory debugging info
- [ ] `EvaluationResultTest.java` - Expression evaluation
- [ ] `ExecutionResultTest.java` - Execution result handling
- [ ] `DebugHandlerTest.java` - Debug event handling
- [ ] `DebugFrameCursorTest.java` - Frame cursor navigation
- [ ] `ProfilingDataTest.java` - Profiling data collection

---

## Phase 10: CoreDump Package (P1 - High Priority)

10 source files, 0 tests - 0% coverage

- [ ] `WasmCoreDumpTest.java` (interface)
- [ ] `DefaultWasmCoreDumpTest.java` (implementation)
- [ ] `CoreDumpFrameTest.java`
- [ ] `DefaultCoreDumpFrameTest.java`
- [ ] `CoreDumpInstanceTest.java`
- [ ] `DefaultCoreDumpInstanceTest.java`
- [ ] `CoreDumpMemoryTest.java`
- [ ] `DefaultCoreDumpMemoryTest.java`
- [ ] `CoreDumpGlobalTest.java`
- [ ] `DefaultCoreDumpGlobalTest.java`

---

## Phase 11: Pool & Cache Packages (P2 - Medium Priority)

### 11.1 Pool Package (6 source files, 0 tests)
- [ ] `PoolingAllocatorTest.java`
- [ ] `PoolingAllocatorConfigTest.java`
- [ ] `PoolingAllocatorConfigBuilderTest.java`
- [ ] `PoolStatisticsTest.java`
- [ ] `PoolingAllocatorMetricsTest.java`
- [ ] `PoolingAllocatorPlatformSupportTest.java`

### 11.2 Cache Package (6 source files, 0 tests)
- [ ] `ModuleCacheTest.java`
- [ ] `ModuleCacheConfigTest.java`
- [ ] `ModuleCacheFactoryTest.java`
- [ ] `ModuleCacheStatisticsTest.java`
- [ ] `TypeValidationCacheTest.java`
- [ ] `MetadataCacheTest.java`

---

## Phase 12: Profiler & Reactive Packages (P2 - Medium Priority)

### 12.1 Profiler Package (2 source files, 0 tests)
- [ ] `ProfilerTest.java` (interface)
- [ ] `ProfilerFactoryTest.java`

### 12.2 Reactive Package (3 source files, 0 tests)
- [ ] `PublisherTest.java`
- [ ] `SubscriberTest.java`
- [ ] `SubscriptionTest.java`

---

## Phase 13: Specialized Packages (P3 - Lower Priority)

### 13.1 Chaos Engineering (1 source file, 0 tests)
- [ ] `ChaosEngineeringFrameworkTest.java`

### 13.2 Disaster Recovery (1 source file, 0 tests)
- [ ] `DisasterRecoverySystemTest.java`

### 13.3 Optimization (1 source file, 0 tests)
- [ ] `MemoryOptimizerTest.java`

---

## Coverage Gap Summary

| Package | Source Files | Test Files | Priority | Status |
|---------|--------------|------------|----------|--------|
| debug/ | 27 | 24+ | P1 | ✅ COMPLETE |
| util/ | 5 | 5+ | P1 | ✅ COMPLETE |
| coredump/ | 10 | 10+ | P1 | ✅ COMPLETE |
| pool/ | 6 | 6+ | P2 | ✅ COMPLETE |
| cache/ | 6 | 6+ | P2 | ✅ COMPLETE |
| profiler/ | 2 | 2+ | P2 | ✅ COMPLETE |
| reactive/ | 3 | 3+ | P2 | ✅ COMPLETE |
| chaos/ | 1 | 1 | P3 | ✅ COMPLETE |
| disaster/ | 1 | 1 | P3 | ✅ COMPLETE |
| optimization/ | 1 | 1 | P3 | ✅ COMPLETE |
| simd/ | - | 1 | P3 | ✅ COMPLETE |
| concurrent/ | - | 1 | P3 | ✅ COMPLETE |
| ref/ | - | 1 | P3 | ✅ COMPLETE |
| factory/ | - | 1 | P3 | ✅ COMPLETE |
| spi/ | - | 1 | P3 | ✅ COMPLETE |
| validation/ | - | 1 | P3 | ✅ COMPLETE |
| distribution/ | - | 1 | P3 | ✅ COMPLETE |
| performance/ | - | 1 | P3 | ✅ COMPLETE |

**All test classes implemented!**
**Final Coverage: 21.7% file ratio (from 12.4%)**

---

## Implementation Priority Order (ALL COMPLETE)

1. **Phase 9.1** - util/ package (5 tests) - ✅ COMPLETE
2. **Phase 10** - coredump/ package (10 tests) - ✅ COMPLETE
3. **Phase 9.2** - debug/ additional (21 tests) - ✅ COMPLETE
4. **Phase 11** - pool/ and cache/ (12 tests) - ✅ COMPLETE
5. **Phase 12** - profiler/ and reactive/ (5 tests) - ✅ COMPLETE
6. **Phase 13** - Specialized packages (8 tests) - ✅ COMPLETE

---

## Notes

- The public API module (`wasmtime4j`) contains interfaces, not implementations
- Tests should verify interface contracts without depending on specific implementations
- Consider using test fixtures for common WASM module bytes
- Integration tests may require the native library to be available
- Debug/coredump tests require careful handling to avoid JVM crashes
- Some tests may need to skip when native library is not available
