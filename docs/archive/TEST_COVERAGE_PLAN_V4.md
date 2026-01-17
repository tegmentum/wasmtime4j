# Test Coverage Improvement Plan V4

**Created:** 2025-12-26
**Status:** IN PROGRESS
**Goal:** Increase test file coverage from ~62% to 80%+

## Executive Summary

Following V3's successful expansion (22% -> 62%), this plan addresses remaining gaps. The project now has 639 test files for 1,034 source files. This plan targets 395 remaining untested files.

### Current Coverage Statistics (Post-V3)

| Module | Source Files | Test Files | Coverage % | Gap |
|--------|-------------|------------|------------|-----|
| wasmtime4j | 663 | 332 | 50.1% | 331 |
| wasmtime4j-jni | 184 | 175 | 95.1% | 9 |
| wasmtime4j-panama | 187 | 132 | 70.6% | 55 |
| **TOTAL** | **1,034** | **639** | **61.8%** | **395** |

### Target Coverage (V4)

| Module | Current | Target | New Tests Needed |
|--------|---------|--------|------------------|
| wasmtime4j | 50.1% | 80% | ~200 test files |
| wasmtime4j-jni | 95.1% | 98% | ~5 test files |
| wasmtime4j-panama | 70.6% | 85% | ~30 test files |

---

## Priority Analysis

### Critical Gaps by Category

| Category | Source Files | Test Files | Gap | Priority |
|----------|-------------|------------|-----|----------|
| Component System | 81 | 13 | 68 | P0 |
| WASI Subsystems | 139 | 50 | 89 | P0 |
| GC Extensions | 34 | 17 | 17 | P1 |
| WIT Interface Types | 28 | 13 | 15 | P1 |
| Execution/Resources | 33 | 34 | -1 | DONE |
| Serialization | 11 | 2 | 9 | P2 |
| Pool Allocators | 8 | 0 | 8 | P2 |

---

## Phase 1: Component System (P0 - 68 files)

### 1.1 Component Core (15 tests)
```
ComponentInstanceConfigTest.java
ComponentFutureTest.java
ComponentCompatibilityTest.java
ComponentEngineHealthTest.java
ComponentDebugInfoTest.java
ComponentEngineConfigTest.java
ComponentEngineStatisticsTest.java
ComponentRegistryStatisticsTest.java
ComponentEventConfigTest.java
ComponentGarbageCollectionResultTest.java
ComponentEngineHealthCheckConfigTest.java
ComponentResourceSharingManagerTest.java
ComponentValidationResultTest.java
ComponentImportValidationTest.java
ComponentTypeTest.java
```

### 1.2 Component Configuration (12 tests)
```
ComponentResourceDefinitionTest.java
ComponentOrchestrationConfigTest.java
ComponentHostFunctionTest.java
ComponentLinkInfoTest.java
ComponentValidationConfigTest.java
ComponentEngineHealthCheckResultTest.java
ComponentGarbageCollectionConfigTest.java
ComponentStateTransitionConfigTest.java
ComponentPipelineConfigTest.java
ComponentLifecycleManagerTest.java
ComponentMonitoringConfigTest.java
ComponentEngineResourceLimitsTest.java
```

### 1.3 Component Advanced (15 tests)
```
ComponentBackupTest.java
ComponentDebuggingSystemTest.java
ComponentAuditLogTest.java
ComponentSwapTest.java
ComponentVersionCompatibilityCheckerTest.java
ComponentResourceQuotaTest.java
ComponentResourcePoolTest.java
ComponentDependencyGraphTest.java
ComponentHealthMonitorTest.java
ComponentMetricsCollectorTest.java
ComponentSecurityPolicyTest.java
ComponentIsolationConfigTest.java
ComponentSharedMemoryTest.java
ComponentMessagePassingTest.java
ComponentEventBusTest.java
```

### 1.4 Component Registry & Orchestration (10 tests)
```
ComponentRegistryTest.java
ComponentCatalogTest.java
ComponentDiscoveryTest.java
ComponentResolverTest.java
ComponentLoaderTest.java
ComponentUnloaderTest.java
ComponentUpdateManagerTest.java
ComponentRollbackManagerTest.java
ComponentMigrationTest.java
ComponentSchedulerTest.java
```

---

## Phase 2: WASI Subsystems (P0 - 89 files)

### 2.1 WASI Network/Sockets (15 tests)
```
wasi/sockets/WasiNetworkTest.java
wasi/sockets/WasiTcpSocketTest.java
wasi/sockets/WasiUdpSocketTest.java
wasi/sockets/WasiIpNameLookupTest.java
wasi/sockets/WasiSocketAddressTest.java
wasi/sockets/WasiSocketOptionsTest.java
wasi/sockets/WasiTcpListenerTest.java
wasi/sockets/WasiNetworkErrorTest.java
wasi/sockets/WasiDnsResolverTest.java
wasi/sockets/WasiSocketBufferTest.java
wasi/sockets/WasiSocketTimeoutTest.java
wasi/sockets/WasiAddressInfoTest.java
wasi/sockets/WasiIpAddressTest.java
wasi/sockets/WasiPortTest.java
wasi/sockets/WasiSocketPollTest.java
```

### 2.2 WASI Neural Networks (12 tests)
```
wasi/nn/NnContextTest.java
wasi/nn/NnGraphTest.java
wasi/nn/NnTensorTest.java
wasi/nn/NnGraphExecutionContextTest.java
wasi/nn/NnGraphBuilderTest.java
wasi/nn/NnTensorTypeTest.java
wasi/nn/NnBackendTest.java
wasi/nn/NnDeviceTest.java
wasi/nn/NnModelFormatTest.java
wasi/nn/NnExecutionFlagsTest.java
wasi/nn/NnErrorTest.java
wasi/nn/NnResourceTest.java
```

### 2.3 WASI Key-Value Store (10 tests)
```
wasi/keyvalue/WasiKeyValueTest.java
wasi/keyvalue/WasiKeyValueBucketTest.java
wasi/keyvalue/WasiKeyValueAtomicsTest.java
wasi/keyvalue/WasiKeyValueBatchTest.java
wasi/keyvalue/WasiKeyValueCursorTest.java
wasi/keyvalue/WasiKeyValueTransactionTest.java
wasi/keyvalue/WasiKeyValueConfigTest.java
wasi/keyvalue/WasiKeyValueConsistencyTest.java
wasi/keyvalue/WasiKeyValueTtlTest.java
wasi/keyvalue/WasiKeyValueWatchTest.java
```

### 2.4 WASI HTTP (8 tests)
```
wasi/http/WasiHttpContextTest.java
wasi/http/WasiHttpConfigTest.java
wasi/http/WasiHttpRequestTest.java
wasi/http/WasiHttpResponseTest.java
wasi/http/WasiHttpHeadersTest.java
wasi/http/WasiHttpBodyTest.java
wasi/http/WasiHttpClientTest.java
wasi/http/WasiHttpServerTest.java
```

### 2.5 WASI Filesystem Extensions (10 tests)
```
wasi/filesystem/WasiDescriptorTest.java
wasi/filesystem/WasiFileStatTest.java
wasi/filesystem/WasiPathTest.java
wasi/filesystem/WasiDirectoryEntryTest.java
wasi/filesystem/WasiFilesystemErrorTest.java
wasi/filesystem/WasiOpenFlagsTest.java
wasi/filesystem/WasiDescriptorFlagsTest.java
wasi/filesystem/WasiFileAdviceTest.java
wasi/filesystem/WasiFileLockTest.java
wasi/filesystem/WasiMetadataHashTest.java
```

### 2.6 WASI Clocks & Time (6 tests)
```
wasi/clocks/WasiMonotonicClockTest.java
wasi/clocks/WasiWallClockTest.java
wasi/clocks/WasiTimezoneTest.java
wasi/clocks/WasiTimezoneDisplayTest.java
wasi/clocks/WasiInstantTest.java
wasi/clocks/WasiDurationTest.java
```

### 2.7 WASI Crypto (8 tests)
```
wasi/crypto/WasiCryptoTest.java
wasi/crypto/WasiHashTest.java
wasi/crypto/WasiSignatureTest.java
wasi/crypto/WasiEncryptionTest.java
wasi/crypto/WasiKeyExchangeTest.java
wasi/crypto/WasiKeyDerivationTest.java
wasi/crypto/WasiSecretKeyTest.java
wasi/crypto/WasiPublicKeyTest.java
```

---

## Phase 3: GC & WIT Extensions (P1 - 32 files)

### 3.1 GC Runtime (10 tests)
```
gc/GcRuntimeTest.java
gc/GcRefTest.java
gc/GcHeapTest.java
gc/GcCollectorTest.java
gc/GcAllocationTest.java
gc/GcFinalizerTest.java
gc/GcRootSetTest.java
gc/GcBarrierTest.java
gc/GcStatsTest.java
gc/GcPolicyTest.java
```

### 3.2 GC Types (7 tests)
```
gc/StructRefTest.java
gc/ArrayRefTest.java
gc/AnyRefTest.java
gc/EqRefTest.java
gc/I31RefTest.java
gc/NullRefTest.java
gc/ExternRefTest.java
```

### 3.3 WIT Marshalling (8 tests)
```
wit/WitMarshallerTest.java
wit/WitSerializerTest.java
wit/WitDeserializerTest.java
wit/WitTypeValidatorTest.java
wit/WitBindingGeneratorTest.java
wit/WitInterfaceResolverTest.java
wit/WitWorldBuilderTest.java
wit/WitPackageTest.java
```

### 3.4 WIT Types (7 tests)
```
wit/WitOptionTest.java
wit/WitResultTest.java
wit/WitVariantTest.java
wit/WitFlagsTest.java
wit/WitResourceTest.java
wit/WitHandleTest.java
wit/WitStreamTest.java
```

---

## Phase 4: Serialization & Pooling (P2 - 17 files)

### 4.1 Serialization (9 tests)
```
serialization/ModuleSerializerTest.java
serialization/ModuleDeserializerTest.java
serialization/SerializationCacheTest.java
serialization/SerializationSecurityTest.java
serialization/SerializationVersionTest.java
serialization/SerializationMetricsTest.java
serialization/SerializationConfigTest.java
serialization/CacheEvictionPolicyTest.java
serialization/CacheStorageTest.java
```

### 4.2 Pool Allocators (8 tests)
```
pool/PoolAllocatorTest.java
pool/PoolAllocatorConfigTest.java
pool/PooledMemoryTest.java
pool/PoolMetricsTest.java
pool/PoolResizeStrategyTest.java
pool/PoolCleanupTest.java
pool/SharedPoolTest.java
pool/PoolStripingTest.java
```

---

## Phase 5: Panama Implementation (P2 - 30 files)

### 5.1 Panama Core (10 tests)
```
panama/PanamaEngineTest.java
panama/PanamaStoreTest.java
panama/PanamaModuleTest.java
panama/PanamaInstanceTest.java
panama/PanamaLinkerTest.java
panama/PanamaFuncTest.java
panama/PanamaMemoryTest.java
panama/PanamaTableTest.java
panama/PanamaGlobalTest.java
panama/PanamaCallerTest.java
```

### 5.2 Panama WASI (10 tests)
```
panama/wasi/PanamaWasiContextTest.java
panama/wasi/PanamaWasiConfigTest.java
panama/wasi/PanamaWasiFilesystemTest.java
panama/wasi/PanamaWasiSocketsTest.java
panama/wasi/PanamaWasiHttpTest.java
panama/wasi/PanamaWasiKeyValueTest.java
panama/wasi/PanamaWasiRandomTest.java
panama/wasi/PanamaWasiClocksTest.java
panama/wasi/PanamaWasiIoTest.java
panama/wasi/PanamaWasiCliTest.java
```

### 5.3 Panama FFI & Utils (10 tests)
```
panama/ffi/PanamaFfiTest.java
panama/ffi/PanamaArenaTest.java
panama/ffi/PanamaMemoryLayoutTest.java
panama/util/PanamaMemoryManagerTest.java
panama/util/PanamaResourceTrackerTest.java
panama/util/PanamaErrorMapperTest.java
panama/util/PanamaTypeConverterTest.java
panama/util/PanamaCallbackHandlerTest.java
panama/util/PanamaNativeLibraryTest.java
panama/util/PanamaCleanerTest.java
```

---

## Phase 6: JNI Remaining (P3 - 9 files)

### 6.1 JNI Extern Wrappers (4 tests)
```
jni/JniExternFuncTest.java
jni/JniExternGlobalTest.java
jni/JniExternMemoryTest.java
jni/JniExternTableTest.java
```

### 6.2 JNI WASI & Advanced (5 tests)
```
jni/wasi/JniWasiDescriptorTest.java
jni/wasi/JniWasiTimezoneTest.java
jni/wasi/JniWasiHttpConfigTest.java
jni/wasi/JniWasiHttpContextTest.java
jni/wasi/JniWasiKeyValueTest.java
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
1. **Phase 1 & 2** (P0): Component + WASI - Core functionality
2. **Phase 3** (P1): GC + WIT - WebAssembly extensions
3. **Phase 4 & 5** (P2): Serialization + Panama - Production features
4. **Phase 6** (P3): JNI remaining - Complete coverage

### Estimated New Test Files
- Phase 1: 52 tests (Component System)
- Phase 2: 69 tests (WASI Subsystems)
- Phase 3: 32 tests (GC + WIT)
- Phase 4: 17 tests (Serialization + Pooling)
- Phase 5: 30 tests (Panama)
- Phase 6: 9 tests (JNI)
- **Total: 209 new test files**

### Target Coverage After V4
| Module | Current | After V4 | Improvement |
|--------|---------|----------|-------------|
| wasmtime4j | 50.1% | 80%+ | +30% |
| wasmtime4j-jni | 95.1% | 98%+ | +3% |
| wasmtime4j-panama | 70.6% | 90%+ | +20% |
| **TOTAL** | **61.8%** | **83%+** | **+21%** |

---

## Progress Tracking

### Phase 1: Component System
- [ ] 1.1 Component Core (15 tests)
- [ ] 1.2 Component Configuration (12 tests)
- [ ] 1.3 Component Advanced (15 tests)
- [ ] 1.4 Component Registry & Orchestration (10 tests)

### Phase 2: WASI Subsystems
- [ ] 2.1 WASI Network/Sockets (15 tests)
- [ ] 2.2 WASI Neural Networks (12 tests)
- [ ] 2.3 WASI Key-Value Store (10 tests)
- [ ] 2.4 WASI HTTP (8 tests)
- [ ] 2.5 WASI Filesystem Extensions (10 tests)
- [ ] 2.6 WASI Clocks & Time (6 tests)
- [ ] 2.7 WASI Crypto (8 tests)

### Phase 3: GC & WIT Extensions
- [ ] 3.1 GC Runtime (10 tests)
- [ ] 3.2 GC Types (7 tests)
- [ ] 3.3 WIT Marshalling (8 tests)
- [ ] 3.4 WIT Types (7 tests)

### Phase 4: Serialization & Pooling
- [ ] 4.1 Serialization (9 tests)
- [ ] 4.2 Pool Allocators (8 tests)

### Phase 5: Panama Implementation
- [ ] 5.1 Panama Core (10 tests)
- [ ] 5.2 Panama WASI (10 tests)
- [ ] 5.3 Panama FFI & Utils (10 tests)

### Phase 6: JNI Remaining
- [ ] 6.1 JNI Extern Wrappers (4 tests)
- [ ] 6.2 JNI WASI & Advanced (5 tests)

---

## Notes

- All tests should compile without requiring native library loading
- Use reflection-based testing for interface verification
- Use stub implementations for integration testing where needed
- Follow Google Java Style Guide strictly
- Each test class must have comprehensive Javadoc
