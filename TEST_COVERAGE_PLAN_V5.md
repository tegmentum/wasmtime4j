# Test Coverage Improvement Plan V5

**Created:** 2025-12-26
**Status:** PLANNED
**Goal:** Increase test file coverage from ~74% to 90%+

## Executive Summary

Following V4's progress, this plan addresses the remaining 380 untested files. The project now has 761 test files for 1,033 source files.

### Current Coverage Statistics (Post-V4)

| Module | Source Files | Test Files | Coverage % | Untested |
|--------|-------------|------------|------------|----------|
| wasmtime4j | 663 | 467 | 70.4% | 244 |
| wasmtime4j-jni | 184 | 173 | 94.0% | 40 |
| wasmtime4j-panama | 186 | 121 | 65.1% | 96 |
| **TOTAL** | **1,033** | **761** | **73.7%** | **380** |

### Target Coverage (V5)

| Module | Current | Target | New Tests Needed |
|--------|---------|--------|------------------|
| wasmtime4j | 70.4% | 90% | ~130 test files |
| wasmtime4j-jni | 94.0% | 98% | ~10 test files |
| wasmtime4j-panama | 65.1% | 90% | ~50 test files |

---

## Priority Analysis

### Critical Gaps by Category

| Category | Untested Files | Priority |
|----------|---------------|----------|
| Component System (API) | ~50 | P0 |
| WASI Subsystems (API) | ~40 | P0 |
| Async/Concurrent (API) | ~15 | P0 |
| Panama WASI Implementation | ~45 | P1 |
| Panama Core/Adapters | ~25 | P1 |
| JNI WASI Implementation | ~20 | P1 |
| Core Runtime Types | ~30 | P2 |
| Advanced Features | ~20 | P2 |

---

## Phase 1: Core API Coverage (P0 - ~105 files)

### 1.1 Component System (50 tests)
```
ComponentBackupTest.java
ComponentCapabilityTest.java
ComponentDebuggingSystemTest.java
ComponentFuncTest.java
ComponentFunctionTest.java
ComponentHostFunctionTest.java
ComponentIdTest.java
ComponentInstanceStateTest.java
ComponentLifecycleStateTest.java
ComponentLoadConditionsTest.java
ComponentOptimizationResultTest.java
ComponentPipelineSpecTest.java
ComponentPipelineStreamTest.java
ComponentResourceDefinitionTest.java
ComponentResourceHandleTest.java
ComponentResourceLimitsTest.java
ComponentResultTest.java
ComponentSimpleTest.java
ComponentSpecificationTest.java
ComponentSwapResultTest.java
ComponentTypeDescriptorTest.java
ComponentTypedFuncTest.java
ComponentValFactoryTest.java
ComponentVariantTest.java
ComponentVersionTest.java
```

### 1.2 Async & Concurrent System (15 tests)
```
async/AsyncFunctionCallTest.java
async/AsyncHostFunctionTest.java
async/AsyncRuntimeTest.java
async/AsyncRuntimeFactoryTest.java
async/StackCreatorTest.java
concurrent/AccessorTest.java
concurrent/ConcurrentTaskTest.java
concurrent/JoinHandleTest.java
concurrent/SpawnableTaskTest.java
```

### 1.3 WASI Core API (40 tests)
```
wasi/WasiContextBuilderTest.java
wasi/WasiDirectoryHandleTest.java
wasi/WasiDirEntryTest.java
wasi/WasiFileHandleTest.java
wasi/WasiFilesystemTest.java
wasi/WasiNetworkTest.java
wasi/WasiTcpSocketTest.java
wasi/WasiUdpSocketTest.java
wasi/WasiHttpContextTest.java
wasi/WasiHttpConfigTest.java
wasi/WasiKeyValueTest.java
wasi/clocks/WasiTimezoneTest.java
wasi/nn/NnContextTest.java
wasi/nn/NnGraphTest.java
wasi/nn/NnTensorTest.java
```

---

## Phase 2: Implementation Coverage (P1 - ~90 files)

### 2.1 Panama WASI Implementation (45 tests)
```
panama/wasi/PanamaWasiConfigTest.java
panama/wasi/PanamaWasiConfigBuilderTest.java
panama/wasi/PanamaWasiLinkerTest.java
panama/wasi/WasiContextTest.java
panama/wasi/WasiContextBuilderTest.java
panama/wasi/WasiContextIsolationValidatorTest.java
panama/wasi/WasiDirectoryAccessControlTest.java
panama/wasi/WasiDirectoryEntryTest.java
panama/wasi/WasiEventTest.java
panama/wasi/WasiFileHandleTest.java
panama/wasi/WasiFileHandleManagerTest.java
panama/wasi/WasiFileMetadataTest.java
panama/wasi/WasiFileOperationTest.java
panama/wasi/WasiFileStatTest.java
panama/wasi/WasiFileSystemTest.java
panama/wasi/WasiResourceUsageTrackerTest.java
panama/wasi/WasiSubscriptionTest.java
panama/wasi/WasiAdvancedNetworkOperationsTest.java
panama/wasi/clocks/PanamaWasiTimezoneTest.java
panama/wasi/filesystem/PanamaWasiDescriptorTest.java
panama/wasi/http/PanamaWasiHttpConfigTest.java
panama/wasi/http/PanamaWasiHttpConfigBuilderTest.java
panama/wasi/http/PanamaWasiHttpContextTest.java
panama/wasi/http/PanamaWasiHttpStatsTest.java
panama/wasi/io/PanamaWasiPollableTest.java
panama/wasi/keyvalue/PanamaWasiKeyValueTest.java
panama/wasi/nn/PanaNnContextTest.java
panama/wasi/nn/PanaNnContextFactoryTest.java
panama/wasi/nn/PanaNnGraphTest.java
panama/wasi/nn/PanaNnGraphExecutionContextTest.java
panama/wasi/permission/WasiPermissionManagerTest.java
panama/wasi/permission/WasiResourceLimitsTest.java
panama/wasi/security/WasiSecurityValidatorTest.java
panama/wasi/sockets/PanamaResolveAddressStreamTest.java
panama/wasi/sockets/PanamaWasiIpNameLookupTest.java
panama/wasi/sockets/PanamaWasiNetworkTest.java
panama/wasi/sockets/PanamaWasiTcpSocketTest.java
panama/wasi/sockets/PanamaWasiUdpSocketTest.java
panama/wasi/threads/PanamaWasiThreadsContextTest.java
panama/wasi/threads/PanamaWasiThreadsContextBuilderTest.java
panama/wasi/threads/PanamaWasiThreadsProviderTest.java
panama/wasi/exception/WasiExceptionTest.java
panama/wasi/exception/WasiFileSystemExceptionTest.java
panama/wasi/exception/WasiPermissionExceptionTest.java
panama/wit/PanamaWitValueMarshallerTest.java
```

### 2.2 Panama Core & Adapters (25 tests)
```
panama/adapter/WasmFunctionToFunctionAdapterTest.java
panama/adapter/WasmGlobalToGlobalAdapterTest.java
panama/adapter/WasmMemoryToMemoryAdapterTest.java
panama/adapter/WasmTableToTableAdapterTest.java
panama/debug/PanamaDebugSessionTest.java
panama/execution/PanamaFuelCallbackHandlerTest.java
panama/execution/PanamaResourceLimiterTest.java
panama/GlobalRegistryTest.java
panama/NativeLibraryLoaderTest.java
panama/PanamaCallbackRegistryTest.java
panama/PanamaCallerContextProviderTest.java
panama/PanamaCallerFunctionTest.java
panama/PanamaComponentAuditLogTest.java
panama/PanamaComponentDebugInfoTest.java
panama/PanamaComponentMetricsTest.java
panama/PanamaComponentRegistryTest.java
panama/PanamaComponentResourceLimitsTest.java
panama/PanamaComponentSimpleTest.java
panama/PanamaErrorHandlerTest.java
panama/PanamaExnRefTest.java
panama/PanamaExperimentalFeaturesTest.java
panama/PanamaExternFuncTest.java
panama/PanamaExternGlobalTest.java
panama/PanamaExternMemoryTest.java
panama/PanamaExternTableTest.java
```

### 2.3 JNI WASI Implementation (20 tests)
```
jni/wasi/WasiAdvancedFileOperationsTest.java
jni/wasi/WasiAdvancedNetworkingTest.java
jni/wasi/WasiAdvancedNetworkOperationsTest.java
jni/wasi/WasiFileOperationTest.java
jni/wasi/WasiProcessOperationsTest.java
jni/wasi/WasiRandomOperationsPreview2Test.java
jni/wasi/WasiTimeOperationsPreview2Test.java
jni/wasi/clocks/JniWasiTimezoneTest.java
jni/wasi/filesystem/JniWasiDescriptorTest.java
jni/wasi/http/JniWasiHttpConfigTest.java
jni/wasi/http/JniWasiHttpConfigBuilderTest.java
jni/wasi/http/JniWasiHttpContextTest.java
jni/wasi/http/JniWasiHttpStatsTest.java
jni/wasi/io/JniWasiPollableTest.java
jni/wasi/keyvalue/JniWasiKeyValueTest.java
jni/wasi/nn/JniNnContextTest.java
jni/wasi/nn/JniNnContextFactoryTest.java
jni/wasi/nn/JniNnGraphTest.java
jni/wasi/nn/JniNnGraphExecutionContextTest.java
jni/wasi/permission/WasiPermissionManagerTest.java
```

---

## Phase 3: Core Types & Advanced (P2 - ~80 files)

### 3.1 Core Runtime Types (30 tests)
```
AdaptationConfigTest.java
AdvancedWasmFeatureTest.java
AnalysisMethodTest.java
BranchHintingInstructionsTest.java
CallGraphAnalysisTest.java
CallHookTest.java
CallHookHandlerTest.java
CallSiteTest.java
CdnStrategyTest.java
CodeBuilderTest.java
ColdFunctionInfoTest.java
CollectorTest.java
CompatibilityRequirementsTest.java
CompiledModuleTest.java
ComplexMarshalingServiceTest.java
ConfigPropertiesTest.java
CustomSectionMetadataTest.java
CustomSectionParserTest.java
CustomSectionValidationResultTest.java
DebugFrameTest.java
DefaultCustomSectionMetadataTest.java
DefaultCustomSectionParserTest.java
DefaultInstanceManagerConfigTest.java
DefaultInstanceManagerConfigBuilderTest.java
DefaultTagTypeTest.java
DefaultWasmExecutionContextTest.java
DependencyEdgeTest.java
DependencyInjectionConfigTest.java
DependencyResolutionTest.java
EventTopicConfigTest.java
```

### 3.2 Type System (25 tests)
```
EvolutionValidationResultTest.java
ExnRefTest.java
ExportTest.java
ExportDescriptorTest.java
ExtendedReferenceTypeTest.java
ExternTest.java
ExternTypeTest.java
FinalityTest.java
FrameInfoTest.java
FrameSymbolTest.java
FunctionTest.java
FunctionContextTest.java
FunctionInfoTest.java
FunctionReferenceTest.java
FunctionTypeMetadataTest.java
FunctionTypeValidatorTest.java
FuncTypeTest.java
HeapTypeTest.java
HotnessReasonTest.java
HotSwapStrategyTest.java
ImportDescriptorTest.java
ImportInfoTest.java
ImportIssueTest.java
ImportMapTest.java
ImportValidationTest.java
```

### 3.3 Instance & Memory (25 tests)
```
InstanceAllocationStrategyTest.java
InstanceManagerTest.java
InstancePreTest.java
InstanceStateTest.java
InstanceStatisticsTest.java
InterfaceEvolutionStrategyTest.java
cache/ModuleCacheTest.java
coredump/CoreDumpFrameTest.java
coredump/CoreDumpGlobalTest.java
coredump/CoreDumpInstanceTest.java
coredump/CoreDumpMemoryTest.java
```

---

## Phase 4: Panama Advanced Features (P2 - ~30 files)

### 4.1 Panama Instance & Component (15 tests)
```
panama/PanamaInstanceGlobalTest.java
panama/PanamaModuleCacheTest.java
panama/PanamaModuleCacheProviderTest.java
panama/PanamaNativeLibraryTest.java
panama/PanamaProfilerTest.java
panama/PanamaProfilerProviderTest.java
panama/PanamaTagTest.java
panama/PanamaWasiComponentContextTest.java
panama/PanamaWasiDirectoryHandleImplTest.java
panama/PanamaWasiDirEntryImplTest.java
panama/PanamaWasiFileHandleImplTest.java
panama/PanamaWasiTcpSocketTest.java
panama/PanamaWasiUdpSocketTest.java
panama/PanamaWasmRuntimeTest.java
panama/PanamaWasmThreadTest.java
```

### 4.2 Panama Performance & Pool (15 tests)
```
panama/PanamaWasmThreadLocalStorageTest.java
panama/performance/AdvancedArenaManagerTest.java
panama/performance/CompilationCacheTest.java
panama/performance/PanamaOptimizationEngineTest.java
panama/performance/PanaNativeObjectPoolTest.java
panama/pool/PanamaPoolingAllocatorTest.java
panama/pool/PanamaPoolingAllocatorConfigBuilderTest.java
panama/simd/PanamaSimdOperationsTest.java
panama/util/PanamaBatchProcessorTest.java
```

---

## Phase 5: JNI Remaining (P3 - ~20 files)

### 5.1 JNI Core (10 tests)
```
jni/execution/JniResourceLimiterTest.java
jni/experimental/JniExceptionHandlerImplTest.java
jni/JniAsyncRuntimeProviderTest.java
jni/JniCallerContextProviderTest.java
jni/JniExternFuncTest.java
jni/JniExternGlobalTest.java
jni/JniExternMemoryTest.java
jni/JniExternTableTest.java
jni/JniModuleCacheProviderTest.java
jni/JniProfilerProviderTest.java
```

### 5.2 JNI Utilities (10 tests)
```
jni/memory/PlatformMemoryManagerTest.java
jni/nativelib/NativeMethodBindingsTest.java
jni/pool/JniPoolingAllocatorTest.java
jni/wasi/permission/WasiResourceLimitsTest.java
jni/wasi/security/WasiSecurityPolicyEngineTest.java
jni/WastDirectiveResultTest.java
jni/WastExecutionResultTest.java
```

---

## Implementation Strategy

### Test File Template
Each test file should follow this structure:
```java
/**
 * Tests for {@link ClassName}.
 */
@DisplayName("ClassName Tests")
class ClassNameTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {
    // Class modifiers, inheritance, interfaces
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {
    // All constructors
  }

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {
    // Public methods grouped logically
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {
    // Null handling, boundary conditions
  }
}
```

### Priority Order
1. **Phase 1** (P0): Core API - Component, Async, WASI API (~105 tests)
2. **Phase 2** (P1): Implementation - Panama/JNI WASI (~90 tests)
3. **Phase 3** (P2): Core Types - Runtime types and system (~80 tests)
4. **Phase 4** (P2): Panama Advanced - Performance, pooling (~30 tests)
5. **Phase 5** (P3): JNI Remaining - Complete coverage (~20 tests)

### Estimated New Test Files
- Phase 1: 105 tests (Core API)
- Phase 2: 90 tests (Implementation)
- Phase 3: 80 tests (Core Types)
- Phase 4: 30 tests (Panama Advanced)
- Phase 5: 20 tests (JNI Remaining)
- **Total: ~325 new test files**

### Target Coverage After V5
| Module | Current | After V5 | Improvement |
|--------|---------|----------|-------------|
| wasmtime4j | 70.4% | 95%+ | +25% |
| wasmtime4j-jni | 94.0% | 98%+ | +4% |
| wasmtime4j-panama | 65.1% | 95%+ | +30% |
| **TOTAL** | **73.7%** | **95%+** | **+21%** |

---

## Progress Tracking

### Phase 1: Core API Coverage
- [ ] 1.1 Component System (50 tests)
- [ ] 1.2 Async & Concurrent System (15 tests)
- [ ] 1.3 WASI Core API (40 tests)

### Phase 2: Implementation Coverage
- [ ] 2.1 Panama WASI Implementation (45 tests)
- [ ] 2.2 Panama Core & Adapters (25 tests)
- [ ] 2.3 JNI WASI Implementation (20 tests)

### Phase 3: Core Types & Advanced
- [ ] 3.1 Core Runtime Types (30 tests)
- [ ] 3.2 Type System (25 tests)
- [ ] 3.3 Instance & Memory (25 tests)

### Phase 4: Panama Advanced Features
- [ ] 4.1 Panama Instance & Component (15 tests)
- [ ] 4.2 Panama Performance & Pool (15 tests)

### Phase 5: JNI Remaining
- [ ] 5.1 JNI Core (10 tests)
- [ ] 5.2 JNI Utilities (10 tests)

---

## Notes

- All tests should compile without requiring native library loading
- Use reflection-based testing for interface verification
- Use stub implementations for integration testing where needed
- Follow Google Java Style Guide strictly
- Each test class must have comprehensive Javadoc
- Skip package-info.java files (no tests needed)
