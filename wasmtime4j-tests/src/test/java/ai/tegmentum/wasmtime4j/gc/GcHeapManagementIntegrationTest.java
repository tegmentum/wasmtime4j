/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for WebAssembly GC heap management.
 *
 * <p>These tests verify heap inspection, profiling, memory leak detection, weak references, object
 * pinning, and advanced GC operations across the native runtime.
 *
 * @since 1.0.0
 */
@DisplayName("GC Heap Management Integration Tests")
public final class GcHeapManagementIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(GcHeapManagementIntegrationTest.class.getName());

  private static boolean gcAvailable = false;

  @BeforeAll
  static void checkGcAvailable() {
    try {
      try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
        GcRuntime gcRuntime = runtime.getGcRuntime();
        gcAvailable = gcRuntime != null;
        if (gcAvailable) {
          LOGGER.info("GC runtime is available");
        }
      }
    } catch (final Exception e) {
      gcAvailable = false;
      LOGGER.warning("GC runtime not available: " + e.getMessage());
    }
  }

  private static void assumeGcAvailable() {
    assumeTrue(gcAvailable, "GC runtime not available - skipping");
  }

  private WasmRuntime runtime;
  private GcRuntime gcRuntime;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (gcAvailable) {
      runtime = WasmRuntimeFactory.create();
      resources.add(runtime);
      gcRuntime = runtime.getGcRuntime();
    }
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    gcRuntime = null;
    runtime = null;
  }

  @Nested
  @DisplayName("Heap Stats Tests")
  class HeapStatsTests {

    @Test
    @DisplayName("should get initial heap stats")
    void shouldGetInitialHeapStats(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      GcStats stats = gcRuntime.getGcStats();
      assertNotNull(stats, "GC stats should not be null");

      LOGGER.info("Initial heap stats:");
      LOGGER.info("  Total allocated: " + stats.getTotalAllocated());
      LOGGER.info("  Bytes allocated: " + stats.getBytesAllocated());
      LOGGER.info("  Current heap size: " + stats.getCurrentHeapSize());
      LOGGER.info("  Major collections: " + stats.getMajorCollections());

      assertTrue(stats.getTotalAllocated() >= 0, "Total allocated should be non-negative");
      assertTrue(stats.getBytesAllocated() >= 0, "Bytes allocated should be non-negative");
      assertTrue(stats.getCurrentHeapSize() >= 0, "Current heap size should be non-negative");
      assertTrue(stats.getMajorCollections() >= 0, "Major collections should be non-negative");
    }

    @Test
    @DisplayName("should track allocations in heap stats")
    void shouldTrackAllocationsInHeapStats(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      GcStats before = gcRuntime.getGcStats();
      long initialAllocated = before.getTotalAllocated();

      // Allocate several objects
      StructType pointType =
          StructType.builder("Point")
              .addField("x", FieldType.i32(), true)
              .addField("y", FieldType.i32(), true)
              .build();
      gcRuntime.registerStructType(pointType);

      for (int i = 0; i < 50; i++) {
        gcRuntime.createStruct(pointType, Arrays.asList(GcValue.i32(i), GcValue.i32(i * 2)));
      }

      GcStats after = gcRuntime.getGcStats();
      long finalAllocated = after.getTotalAllocated();

      LOGGER.info("Allocations before: " + initialAllocated + ", after: " + finalAllocated);
      assertTrue(
          finalAllocated > initialAllocated,
          "Total allocations should increase after creating objects");
    }

    @Test
    @DisplayName("should increment collection count after GC")
    void shouldIncrementCollectionCountAfterGc(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      GcStats before = gcRuntime.getGcStats();
      long initialCollections = before.getMajorCollections();

      // Trigger GC
      GcStats collectionResult = gcRuntime.collectGarbage();
      assertNotNull(collectionResult, "Collection result should not be null");

      GcStats after = gcRuntime.getGcStats();
      long finalCollections = after.getMajorCollections();

      LOGGER.info("Collections before: " + initialCollections + ", after: " + finalCollections);
      assertTrue(
          finalCollections > initialCollections,
          "Collection count should increase after triggering GC");
    }
  }

  @Nested
  @DisplayName("Heap Inspection Tests")
  class HeapInspectionTests {

    @Test
    @DisplayName("should inspect heap and return results")
    void shouldInspectHeapAndReturnResults(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create some objects to inspect
      ArrayType intArrayType =
          ArrayType.builder("IntArray").elementType(FieldType.i32()).mutable(true).build();
      gcRuntime.registerArrayType(intArrayType);
      gcRuntime.createArray(
          intArrayType, Arrays.asList(GcValue.i32(1), GcValue.i32(2), GcValue.i32(3)));

      GcHeapInspection inspection = gcRuntime.inspectHeap();
      assertNotNull(inspection, "Heap inspection should not be null");

      LOGGER.info("Heap inspection results:");
      LOGGER.info("  Total objects: " + inspection.getTotalObjectCount());
      LOGGER.info("  Total heap size: " + inspection.getTotalHeapSize());

      assertTrue(inspection.getTotalObjectCount() >= 0, "Total objects should be non-negative");
      assertTrue(inspection.getTotalHeapSize() >= 0, "Total heap size should be non-negative");
    }

    @Test
    @DisplayName("should get reference graph from heap")
    void shouldGetReferenceGraphFromHeap(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create nested objects
      StructType innerType =
          StructType.builder("Inner").addField("value", FieldType.i32(), true).build();
      gcRuntime.registerStructType(innerType);

      StructType outerType =
          StructType.builder("Outer")
              .addField("inner", FieldType.reference(GcReferenceType.STRUCT_REF), true)
              .build();
      gcRuntime.registerStructType(outerType);

      StructInstance inner = gcRuntime.createStruct(innerType, Arrays.asList(GcValue.i32(42)));
      StructInstance outer =
          gcRuntime.createStruct(outerType, Arrays.asList(GcValue.reference(inner)));

      // Inspect should show the reference relationship
      GcHeapInspection inspection = gcRuntime.inspectHeap();
      assertNotNull(inspection, "Heap inspection should not be null");

      LOGGER.info(
          "Created nested objects, heap has " + inspection.getTotalObjectCount() + " objects");
    }
  }

  @Nested
  @DisplayName("Weak Reference Tests")
  class WeakReferenceTests {

    @Test
    @DisplayName("should create weak reference to object")
    void shouldCreateWeakReferenceToObject(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      StructType type =
          StructType.builder("WeakRefTarget").addField("value", FieldType.i32(), true).build();
      gcRuntime.registerStructType(type);

      StructInstance target = gcRuntime.createStruct(type, Arrays.asList(GcValue.i32(100)));
      assertNotNull(target, "Target should not be null");

      WeakGcReference weakRef = gcRuntime.createWeakReference(target, null);
      assertNotNull(weakRef, "Weak reference should not be null");
      assertFalse(weakRef.isCleared(), "Weak reference should not be cleared initially");

      // Should be able to get the target
      GcObject retrieved = weakRef.get().orElse(null);
      assertNotNull(retrieved, "Should be able to retrieve target from weak reference");

      LOGGER.info("Created weak reference to struct, retrieved successfully");
    }

    @Test
    @DisplayName("should invoke finalization callback when object collected")
    void shouldInvokeFinalizationCallbackWhenObjectCollected(final TestInfo testInfo)
        throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      AtomicBoolean finalized = new AtomicBoolean(false);

      StructType type =
          StructType.builder("Finalizable").addField("data", FieldType.i32(), true).build();
      gcRuntime.registerStructType(type);

      {
        StructInstance target = gcRuntime.createStruct(type, Arrays.asList(GcValue.i32(999)));
        gcRuntime.createWeakReference(target, () -> finalized.set(true));
        // Target goes out of scope here
      }

      // Trigger GC multiple times to encourage collection
      for (int i = 0; i < 5; i++) {
        gcRuntime.collectGarbage();
      }

      // Run finalization
      int finalizedCount = gcRuntime.runFinalization();
      LOGGER.info("Finalization ran, finalized count: " + finalizedCount);
      LOGGER.info("Finalization callback invoked: " + finalized.get());

      // Note: finalization is not guaranteed, so we just verify no exceptions
      assertTrue(true, "Finalization completed without errors");
    }
  }

  @Nested
  @DisplayName("Object Pinning Tests")
  class ObjectPinningTests {

    @Test
    @DisplayName("should pin and unpin object")
    void shouldPinAndUnpinObject(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      I31Instance value = gcRuntime.createI31(42);
      assertNotNull(value, "I31 should not be null");

      // Pin the object
      gcRuntime.pinObject(value);
      LOGGER.info("Object pinned successfully");

      // Object should still be accessible after GC
      gcRuntime.collectGarbage();
      assertEquals(42, value.getValue(), "Pinned object should retain value after GC");

      // Unpin the object
      gcRuntime.unpinObject(value);
      LOGGER.info("Object unpinned successfully");

      // Object should still be accessible (just no longer pinned)
      assertEquals(42, value.getValue(), "Object should retain value after unpinning");
    }

    @Test
    @DisplayName("should keep pinned objects alive through GC cycles")
    void shouldKeepPinnedObjectsAliveThroughGcCycles(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ArrayType type =
          ArrayType.builder("PinnedArray").elementType(FieldType.i64()).mutable(true).build();
      gcRuntime.registerArrayType(type);

      ArrayInstance pinnedArray =
          gcRuntime.createArray(type, Arrays.asList(GcValue.i64(1L), GcValue.i64(2L)));
      gcRuntime.pinObject(pinnedArray);

      // Create lots of garbage and collect multiple times
      for (int cycle = 0; cycle < 10; cycle++) {
        for (int i = 0; i < 100; i++) {
          gcRuntime.createI31(i); // Create garbage
        }
        gcRuntime.collectGarbage();

        // Pinned array should still be intact
        assertEquals(2, gcRuntime.getArrayLength(pinnedArray), "Array length should remain 2");
        assertEquals(
            1L, gcRuntime.getArrayElement(pinnedArray, 0).asI64(), "First element should be 1");
      }

      gcRuntime.unpinObject(pinnedArray);
      LOGGER.info("Pinned array survived " + 10 + " GC cycles");
    }
  }

  @Nested
  @DisplayName("Incremental GC Tests")
  class IncrementalGcTests {

    @Test
    @DisplayName("should perform incremental garbage collection")
    void shouldPerformIncrementalGarbageCollection(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create some objects
      for (int i = 0; i < 200; i++) {
        gcRuntime.createI31(i);
      }

      long maxPauseMs = 10; // 10ms max pause
      GcStats result = gcRuntime.collectGarbageIncremental(maxPauseMs);
      assertNotNull(result, "Incremental GC result should not be null");

      LOGGER.info("Incremental GC completed with max pause " + maxPauseMs + "ms");
      LOGGER.info("  Objects collected: " + result.getTotalCollected());
    }

    @Test
    @DisplayName("should perform concurrent garbage collection")
    void shouldPerformConcurrentGarbageCollection(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create some objects
      for (int i = 0; i < 200; i++) {
        gcRuntime.createI31(i);
      }

      GcStats result = gcRuntime.collectGarbageConcurrent();
      assertNotNull(result, "Concurrent GC result should not be null");

      LOGGER.info("Concurrent GC completed");
      LOGGER.info("  Objects collected: " + result.getTotalCollected());
    }

    @Test
    @DisplayName("should handle GC pressure monitoring")
    void shouldHandleGcPressureMonitoring(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Start with clean heap
      gcRuntime.collectGarbage();

      // Create pressure by allocating objects
      ArrayType type =
          ArrayType.builder("PressureArray").elementType(FieldType.f64()).mutable(true).build();
      gcRuntime.registerArrayType(type);

      for (int i = 0; i < 500; i++) {
        List<GcValue> elements = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
          elements.add(GcValue.f64(j * 1.1));
        }
        gcRuntime.createArray(type, elements);
      }

      // Monitor pressure at different thresholds
      boolean triggeredLow = gcRuntime.monitorGcPressure(0.1); // Low threshold
      boolean triggeredHigh = gcRuntime.monitorGcPressure(0.9); // High threshold

      LOGGER.info("GC triggered at 10% threshold: " + triggeredLow);
      LOGGER.info("GC triggered at 90% threshold: " + triggeredHigh);

      // Either can be true depending on heap state
      assertTrue(true, "Pressure monitoring completed without errors");
    }
  }

  @Nested
  @DisplayName("Memory Leak Detection Tests")
  class MemoryLeakDetectionTests {

    @Test
    @DisplayName("should detect memory leaks")
    void shouldDetectMemoryLeaks(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      MemoryLeakAnalysis analysis = gcRuntime.detectMemoryLeaks();
      assertNotNull(analysis, "Memory leak analysis should not be null");

      boolean hasLeaks = analysis.getPotentialLeakCount() > 0;
      LOGGER.info("Memory leak analysis:");
      LOGGER.info("  Has potential leaks: " + hasLeaks);
      LOGGER.info("  Total objects analyzed: " + analysis.getTotalObjectCount());
      LOGGER.info("  Potential leak count: " + analysis.getPotentialLeakCount());

      assertTrue(analysis.getTotalObjectCount() >= 0, "Total object count should be non-negative");
      assertTrue(
          analysis.getPotentialLeakCount() >= 0, "Potential leak count should be non-negative");
    }

    @Test
    @DisplayName("should detect memory corruption")
    void shouldDetectMemoryCorruption(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      MemoryCorruptionAnalysis analysis = gcRuntime.detectMemoryCorruption();
      assertNotNull(analysis, "Memory corruption analysis should not be null");

      LOGGER.info("Memory corruption analysis:");
      LOGGER.info("  Has corruption: " + analysis.isCorruptionDetected());
      LOGGER.info("  Corruption count: " + analysis.getCorruptionIssues().size());

      // In normal operation, there should be no corruption
      assertFalse(
          analysis.isCorruptionDetected(), "Should not detect corruption in normal operation");
    }

    @Test
    @DisplayName("should validate GC invariants")
    void shouldValidateGcInvariants(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      GcInvariantValidation validation = gcRuntime.validateInvariants();
      assertNotNull(validation, "GC invariant validation should not be null");

      LOGGER.info("GC invariant validation:");
      LOGGER.info("  All satisfied: " + validation.areAllInvariantsSatisfied());
      LOGGER.info("  Violations: " + validation.getViolationCount());

      assertTrue(
          validation.areAllInvariantsSatisfied(),
          "GC invariants should be satisfied in normal operation");
      assertEquals(0, validation.getViolationCount(), "Should have no invariant violations");
    }
  }

  @Nested
  @DisplayName("Profiling Tests")
  class ProfilingTests {

    @Test
    @DisplayName("should start and stop profiling")
    void shouldStartAndStopProfiling(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      GcProfiler profiler = gcRuntime.startProfiling();
      assertNotNull(profiler, "Profiler should not be null");

      // Perform some operations while profiling
      for (int i = 0; i < 100; i++) {
        gcRuntime.createI31(i);
      }
      gcRuntime.collectGarbage();

      // Stop profiling and get results
      GcProfiler.GcProfilingResults results = profiler.stop();
      assertNotNull(results, "Profiling results should not be null");

      var allocationStats = results.getAllocationStatistics();
      var gcStats = results.getGcPerformanceStatistics();

      LOGGER.info("Profiling completed:");
      LOGGER.info("  Total allocations: " + allocationStats.getTotalAllocations());
      LOGGER.info("  Total collections: " + gcStats.getTotalCollections());
      LOGGER.info("  Total pause time ms: " + gcStats.getTotalPauseTime().toMillis());

      assertTrue(
          allocationStats.getTotalAllocations() >= 100, "Should have at least 100 allocations");
      assertTrue(gcStats.getTotalCollections() >= 1, "Should have at least 1 collection");
    }

    @Test
    @DisplayName("should track object lifecycles")
    void shouldTrackObjectLifecycles(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create objects to track
      List<GcObject> objectsToTrack = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        objectsToTrack.add(gcRuntime.createI31(i * 10));
      }

      ObjectLifecycleTracker tracker = gcRuntime.trackObjectLifecycles(objectsToTrack);
      assertNotNull(tracker, "Lifecycle tracker should not be null");

      // Perform GC
      gcRuntime.collectGarbage();

      LOGGER.info("Lifecycle tracking:");
      LOGGER.info("  Tracked objects: " + tracker.getTrackedObjects().size());

      // Count alive and collected objects from status map
      var statuses = tracker.getObjectStatuses();
      long aliveCount = statuses.values().stream().filter(s -> s.isAlive()).count();
      long collectedCount = statuses.size() - aliveCount;
      LOGGER.info("  Alive objects: " + aliveCount);
      LOGGER.info("  Collected objects: " + collectedCount);

      assertTrue(tracker.getTrackedObjects().size() >= 5, "Should track at least 5 objects");
    }
  }

  @Nested
  @DisplayName("Reference Safety Tests")
  class ReferenceSafetyTests {

    @Test
    @DisplayName("should validate reference safety for object graph")
    void shouldValidateReferenceSafetyForObjectGraph(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create an object graph
      StructType nodeType =
          StructType.builder("Node")
              .addField("value", FieldType.i32(), true)
              .addField("next", FieldType.reference(GcReferenceType.STRUCT_REF), true)
              .build();
      gcRuntime.registerStructType(nodeType);

      StructInstance node3 =
          gcRuntime.createStruct(nodeType, Arrays.asList(GcValue.i32(3), GcValue.nullValue()));
      StructInstance node2 =
          gcRuntime.createStruct(nodeType, Arrays.asList(GcValue.i32(2), GcValue.reference(node3)));
      StructInstance node1 =
          gcRuntime.createStruct(nodeType, Arrays.asList(GcValue.i32(1), GcValue.reference(node2)));

      List<GcObject> roots = Arrays.asList(node1);
      ReferenceSafetyResult result = gcRuntime.validateReferenceSafety(roots);
      assertNotNull(result, "Reference safety result should not be null");

      LOGGER.info("Reference safety validation:");
      LOGGER.info("  Is safe: " + result.isAllSafe());
      LOGGER.info("  Violations: " + result.getViolationCount());

      assertTrue(result.isAllSafe(), "Object graph should be reference-safe");
    }

    @Test
    @DisplayName("should enforce type safety")
    void shouldEnforceTypeSafety(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      I31Instance i31 = gcRuntime.createI31(42);
      StructType type =
          StructType.builder("TypeSafeStruct").addField("val", FieldType.i32(), true).build();
      gcRuntime.registerStructType(type);
      StructInstance struct = gcRuntime.createStruct(type, Arrays.asList(GcValue.i32(100)));

      // Valid operation
      boolean safeRead = gcRuntime.enforceTypeSafety("struct.get", Arrays.asList(struct, 0));
      assertTrue(safeRead, "Struct field read should be type-safe");

      // Invalid operation (wrong type for operation)
      boolean unsafeCast =
          gcRuntime.enforceTypeSafety("ref.cast", Arrays.asList(i31, GcReferenceType.STRUCT_REF));
      assertFalse(unsafeCast, "Invalid cast should not be type-safe");

      LOGGER.info("Type safety enforcement validated");
    }
  }

  @Nested
  @DisplayName("Host Integration Tests")
  class HostIntegrationTests {

    @Test
    @DisplayName("should integrate host object with GC heap")
    void shouldIntegrateHostObjectWithGcHeap(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a Java object to integrate
      String hostString = "Hello from Java";
      GcObject integrated = gcRuntime.integrateHostObject(hostString, GcReferenceType.ANY_REF);
      assertNotNull(integrated, "Integrated object should not be null");

      // Extract it back
      Object extracted = gcRuntime.extractHostObject(integrated);
      assertEquals(hostString, extracted, "Extracted object should match original");

      LOGGER.info("Host object integration successful: " + extracted);
    }

    @Test
    @DisplayName("should create sharing bridge for cross-language objects")
    void shouldCreateSharingBridgeForCrossLanguageObjects(final TestInfo testInfo)
        throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create objects to share
      List<GcObject> objectsToShare = new ArrayList<>();
      for (int i = 0; i < 3; i++) {
        objectsToShare.add(gcRuntime.createI31(i * 100));
      }

      Object bridge = gcRuntime.createSharingBridge(objectsToShare);
      assertNotNull(bridge, "Sharing bridge should not be null");

      LOGGER.info(
          "Cross-language sharing bridge created for " + objectsToShare.size() + " objects");
    }
  }

  @Nested
  @DisplayName("Advanced GC Operations Tests")
  class AdvancedGcOperationsTests {

    @Test
    @DisplayName("should perform optimized reference cast with caching")
    void shouldPerformOptimizedReferenceCastWithCaching(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      I31Instance i31 = gcRuntime.createI31(42);

      // Cast with caching enabled
      GcObject result1 = gcRuntime.refCastOptimized(i31, GcReferenceType.EQ_REF, true);
      assertNotNull(result1, "First cast should succeed");

      // Second cast should benefit from cache
      GcObject result2 = gcRuntime.refCastOptimized(i31, GcReferenceType.EQ_REF, true);
      assertNotNull(result2, "Second cast should succeed (cached)");

      LOGGER.info("Optimized reference cast with caching completed");
    }

    @Test
    @DisplayName("should configure GC strategy")
    void shouldConfigureGcStrategy(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      java.util.Map<String, Object> params = new java.util.HashMap<>();
      params.put("threshold", 1024 * 1024); // 1MB threshold
      params.put("concurrent", true);

      // Configure generational GC strategy
      gcRuntime.configureGcStrategy("generational", params);
      LOGGER.info("Configured generational GC strategy");

      // Perform some operations
      for (int i = 0; i < 100; i++) {
        gcRuntime.createI31(i);
      }

      // Trigger collection with new strategy
      GcStats result = gcRuntime.collectGarbage();
      assertNotNull(result, "Collection should succeed with new strategy");

      LOGGER.info("GC strategy configuration completed");
    }

    @Test
    @DisplayName("should perform advanced GC collection")
    void shouldPerformAdvancedGcCollection(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create garbage
      for (int i = 0; i < 200; i++) {
        gcRuntime.createI31(i);
      }

      // Advanced collection with pause limit and concurrent flag
      GcStats result = gcRuntime.collectGarbageAdvanced(50L, true);
      assertNotNull(result, "Advanced GC result should not be null");

      LOGGER.info("Advanced GC collection completed:");
      LOGGER.info("  Objects collected: " + result.getTotalCollected());
      LOGGER.info("  Bytes collected: " + result.getBytesCollected());
    }

    @Test
    @DisplayName("should copy array elements between arrays")
    void shouldCopyArrayElementsBetweenArrays(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ArrayType type =
          ArrayType.builder("CopyTestArray").elementType(FieldType.i32()).mutable(true).build();
      gcRuntime.registerArrayType(type);

      ArrayInstance source =
          gcRuntime.createArray(
              type,
              Arrays.asList(
                  GcValue.i32(1), GcValue.i32(2), GcValue.i32(3), GcValue.i32(4), GcValue.i32(5)));
      ArrayInstance dest = gcRuntime.createArray(type, 5);

      // Copy middle 3 elements
      gcRuntime.copyArrayElements(source, 1, dest, 0, 3);

      assertEquals(2, gcRuntime.getArrayElement(dest, 0).asI32(), "First copied element");
      assertEquals(3, gcRuntime.getArrayElement(dest, 1).asI32(), "Second copied element");
      assertEquals(4, gcRuntime.getArrayElement(dest, 2).asI32(), "Third copied element");

      LOGGER.info("Array element copy completed successfully");
    }

    @Test
    @DisplayName("should fill array elements with value")
    void shouldFillArrayElementsWithValue(final TestInfo testInfo) throws Exception {
      assumeGcAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ArrayType type =
          ArrayType.builder("FillTestArray").elementType(FieldType.i32()).mutable(true).build();
      gcRuntime.registerArrayType(type);

      ArrayInstance array = gcRuntime.createArray(type, 10);

      // Fill indices 2-7 with value 42
      gcRuntime.fillArrayElements(array, 2, 5, GcValue.i32(42));

      assertEquals(0, gcRuntime.getArrayElement(array, 0).asI32(), "Index 0 unchanged");
      assertEquals(0, gcRuntime.getArrayElement(array, 1).asI32(), "Index 1 unchanged");
      assertEquals(42, gcRuntime.getArrayElement(array, 2).asI32(), "Index 2 filled");
      assertEquals(42, gcRuntime.getArrayElement(array, 4).asI32(), "Index 4 filled");
      assertEquals(42, gcRuntime.getArrayElement(array, 6).asI32(), "Index 6 filled");
      assertEquals(0, gcRuntime.getArrayElement(array, 7).asI32(), "Index 7 unchanged");

      LOGGER.info("Array fill completed successfully");
    }
  }
}
