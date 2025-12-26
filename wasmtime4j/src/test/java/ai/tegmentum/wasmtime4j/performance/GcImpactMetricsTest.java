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

package ai.tegmentum.wasmtime4j.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcImpactMetrics} class and its nested classes.
 *
 * <p>GcImpactMetrics captures and analyzes the impact of garbage collection on WebAssembly
 * performance.
 */
@DisplayName("GcImpactMetrics Tests")
class GcImpactMetricsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(GcImpactMetrics.class.getModifiers()),
          "GcImpactMetrics should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(GcImpactMetrics.class.getModifiers()),
          "GcImpactMetrics should be public");
    }

    @Test
    @DisplayName("should have Snapshot nested class")
    void shouldHaveSnapshotNestedClass() {
      final Class<?>[] nestedClasses = GcImpactMetrics.class.getDeclaredClasses();
      boolean hasSnapshot = false;
      for (final Class<?> clazz : nestedClasses) {
        if (clazz.getSimpleName().equals("Snapshot")) {
          hasSnapshot = true;
          assertTrue(Modifier.isStatic(clazz.getModifiers()), "Snapshot should be static");
          assertTrue(Modifier.isFinal(clazz.getModifiers()), "Snapshot should be final");
          break;
        }
      }
      assertTrue(hasSnapshot, "Should have Snapshot nested class");
    }

    @Test
    @DisplayName("should have GcCollectorSnapshot nested class")
    void shouldHaveGcCollectorSnapshotNestedClass() {
      final Class<?>[] nestedClasses = GcImpactMetrics.class.getDeclaredClasses();
      boolean hasGcCollectorSnapshot = false;
      for (final Class<?> clazz : nestedClasses) {
        if (clazz.getSimpleName().equals("GcCollectorSnapshot")) {
          hasGcCollectorSnapshot = true;
          assertTrue(
              Modifier.isStatic(clazz.getModifiers()), "GcCollectorSnapshot should be static");
          assertTrue(Modifier.isFinal(clazz.getModifiers()), "GcCollectorSnapshot should be final");
          break;
        }
      }
      assertTrue(hasGcCollectorSnapshot, "Should have GcCollectorSnapshot nested class");
    }

    @Test
    @DisplayName("should have GcCollectorMetrics nested class")
    void shouldHaveGcCollectorMetricsNestedClass() {
      final Class<?>[] nestedClasses = GcImpactMetrics.class.getDeclaredClasses();
      boolean hasGcCollectorMetrics = false;
      for (final Class<?> clazz : nestedClasses) {
        if (clazz.getSimpleName().equals("GcCollectorMetrics")) {
          hasGcCollectorMetrics = true;
          assertTrue(
              Modifier.isStatic(clazz.getModifiers()), "GcCollectorMetrics should be static");
          assertTrue(Modifier.isFinal(clazz.getModifiers()), "GcCollectorMetrics should be final");
          break;
        }
      }
      assertTrue(hasGcCollectorMetrics, "Should have GcCollectorMetrics nested class");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have captureSnapshot static method")
    void shouldHaveCaptureSnapshotStaticMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("captureSnapshot");
      assertNotNull(method, "captureSnapshot method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "captureSnapshot should be static");
      assertEquals(
          GcImpactMetrics.Snapshot.class, method.getReturnType(), "Should return Snapshot");
    }

    @Test
    @DisplayName("should have calculate static method")
    void shouldHaveCalculateStaticMethod() throws NoSuchMethodException {
      final Method method =
          GcImpactMetrics.class.getMethod(
              "calculate",
              GcImpactMetrics.Snapshot.class,
              GcImpactMetrics.Snapshot.class,
              Duration.class);
      assertNotNull(method, "calculate method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "calculate should be static");
      assertEquals(GcImpactMetrics.class, method.getReturnType(), "Should return GcImpactMetrics");
    }

    @Test
    @DisplayName("should have measureForcedGc static method")
    void shouldHaveMeasureForcedGcStaticMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("measureForcedGc");
      assertNotNull(method, "measureForcedGc method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "measureForcedGc should be static");
      assertEquals(GcImpactMetrics.class, method.getReturnType(), "Should return GcImpactMetrics");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getTotalGcTime method")
    void shouldHaveGetTotalGcTimeMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("getTotalGcTime");
      assertNotNull(method, "getTotalGcTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getTotalGcTime should return Duration");
    }

    @Test
    @DisplayName("should have getTotalGcCollections method")
    void shouldHaveGetTotalGcCollectionsMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("getTotalGcCollections");
      assertNotNull(method, "getTotalGcCollections method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalGcCollections should return long");
    }

    @Test
    @DisplayName("should have getMemoryAllocated method")
    void shouldHaveGetMemoryAllocatedMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("getMemoryAllocated");
      assertNotNull(method, "getMemoryAllocated method should exist");
      assertEquals(long.class, method.getReturnType(), "getMemoryAllocated should return long");
    }

    @Test
    @DisplayName("should have getMemoryFreed method")
    void shouldHaveGetMemoryFreedMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("getMemoryFreed");
      assertNotNull(method, "getMemoryFreed method should exist");
      assertEquals(long.class, method.getReturnType(), "getMemoryFreed should return long");
    }

    @Test
    @DisplayName("should have getOperationDuration method")
    void shouldHaveGetOperationDurationMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("getOperationDuration");
      assertNotNull(method, "getOperationDuration method should exist");
      assertEquals(
          Duration.class, method.getReturnType(), "getOperationDuration should return Duration");
    }

    @Test
    @DisplayName("should have getGcOverheadPercentage method")
    void shouldHaveGetGcOverheadPercentageMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("getGcOverheadPercentage");
      assertNotNull(method, "getGcOverheadPercentage method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getGcOverheadPercentage should return double");
    }

    @Test
    @DisplayName("should have getAllocationRate method")
    void shouldHaveGetAllocationRateMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("getAllocationRate");
      assertNotNull(method, "getAllocationRate method should exist");
      assertEquals(double.class, method.getReturnType(), "getAllocationRate should return double");
    }

    @Test
    @DisplayName("should have getCollectionEfficiency method")
    void shouldHaveGetCollectionEfficiencyMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("getCollectionEfficiency");
      assertNotNull(method, "getCollectionEfficiency method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getCollectionEfficiency should return double");
    }

    @Test
    @DisplayName("should have getCaptureTime method")
    void shouldHaveGetCaptureTimeMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("getCaptureTime");
      assertNotNull(method, "getCaptureTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "getCaptureTime should return Instant");
    }

    @Test
    @DisplayName("should have getCollectorMetrics method")
    void shouldHaveGetCollectorMetricsMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("getCollectorMetrics");
      assertNotNull(method, "getCollectorMetrics method should exist");
      assertEquals(List.class, method.getReturnType(), "getCollectorMetrics should return List");
    }
  }

  @Nested
  @DisplayName("Analysis Method Tests")
  class AnalysisMethodTests {

    @Test
    @DisplayName("should have hasSignificantImpact method")
    void shouldHaveHasSignificantImpactMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("hasSignificantImpact");
      assertNotNull(method, "hasSignificantImpact method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "hasSignificantImpact should return boolean");
    }

    @Test
    @DisplayName("should have hasHighAllocationRate method")
    void shouldHaveHasHighAllocationRateMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("hasHighAllocationRate");
      assertNotNull(method, "hasHighAllocationRate method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "hasHighAllocationRate should return boolean");
    }

    @Test
    @DisplayName("should have getSummary method")
    void shouldHaveGetSummaryMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.class.getMethod("getSummary");
      assertNotNull(method, "getSummary method should exist");
      assertEquals(String.class, method.getReturnType(), "getSummary should return String");
    }
  }

  @Nested
  @DisplayName("Snapshot Class Tests")
  class SnapshotClassTests {

    @Test
    @DisplayName("should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.Snapshot.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(Instant.class, method.getReturnType(), "getTimestamp should return Instant");
    }

    @Test
    @DisplayName("should have getTotalGcTime method")
    void shouldHaveGetTotalGcTimeMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.Snapshot.class.getMethod("getTotalGcTime");
      assertNotNull(method, "getTotalGcTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getTotalGcTime should return Duration");
    }

    @Test
    @DisplayName("should have getTotalCollections method")
    void shouldHaveGetTotalCollectionsMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.Snapshot.class.getMethod("getTotalCollections");
      assertNotNull(method, "getTotalCollections method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalCollections should return long");
    }

    @Test
    @DisplayName("should have getHeapMemoryUsed method")
    void shouldHaveGetHeapMemoryUsedMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.Snapshot.class.getMethod("getHeapMemoryUsed");
      assertNotNull(method, "getHeapMemoryUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "getHeapMemoryUsed should return long");
    }

    @Test
    @DisplayName("should have getNonHeapMemoryUsed method")
    void shouldHaveGetNonHeapMemoryUsedMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.Snapshot.class.getMethod("getNonHeapMemoryUsed");
      assertNotNull(method, "getNonHeapMemoryUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "getNonHeapMemoryUsed should return long");
    }

    @Test
    @DisplayName("should have getMaxHeapMemory method")
    void shouldHaveGetMaxHeapMemoryMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.Snapshot.class.getMethod("getMaxHeapMemory");
      assertNotNull(method, "getMaxHeapMemory method should exist");
      assertEquals(long.class, method.getReturnType(), "getMaxHeapMemory should return long");
    }

    @Test
    @DisplayName("should have getCollectorSnapshots method")
    void shouldHaveGetCollectorSnapshotsMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.Snapshot.class.getMethod("getCollectorSnapshots");
      assertNotNull(method, "getCollectorSnapshots method should exist");
      assertEquals(List.class, method.getReturnType(), "getCollectorSnapshots should return List");
    }
  }

  @Nested
  @DisplayName("GcCollectorSnapshot Class Tests")
  class GcCollectorSnapshotClassTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.GcCollectorSnapshot.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("should have getCollectionCount method")
    void shouldHaveGetCollectionCountMethod() throws NoSuchMethodException {
      final Method method =
          GcImpactMetrics.GcCollectorSnapshot.class.getMethod("getCollectionCount");
      assertNotNull(method, "getCollectionCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getCollectionCount should return long");
    }

    @Test
    @DisplayName("should have getCollectionTime method")
    void shouldHaveGetCollectionTimeMethod() throws NoSuchMethodException {
      final Method method =
          GcImpactMetrics.GcCollectorSnapshot.class.getMethod("getCollectionTime");
      assertNotNull(method, "getCollectionTime method should exist");
      assertEquals(long.class, method.getReturnType(), "getCollectionTime should return long");
    }

    @Test
    @DisplayName("should have getMemoryPoolNames method")
    void shouldHaveGetMemoryPoolNamesMethod() throws NoSuchMethodException {
      final Method method =
          GcImpactMetrics.GcCollectorSnapshot.class.getMethod("getMemoryPoolNames");
      assertNotNull(method, "getMemoryPoolNames method should exist");
      assertEquals(
          String[].class, method.getReturnType(), "getMemoryPoolNames should return String[]");
    }
  }

  @Nested
  @DisplayName("GcCollectorMetrics Class Tests")
  class GcCollectorMetricsClassTests {

    @Test
    @DisplayName("should have getCollectorName method")
    void shouldHaveGetCollectorNameMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.GcCollectorMetrics.class.getMethod("getCollectorName");
      assertNotNull(method, "getCollectorName method should exist");
      assertEquals(String.class, method.getReturnType(), "getCollectorName should return String");
    }

    @Test
    @DisplayName("should have getCollections method")
    void shouldHaveGetCollectionsMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.GcCollectorMetrics.class.getMethod("getCollections");
      assertNotNull(method, "getCollections method should exist");
      assertEquals(long.class, method.getReturnType(), "getCollections should return long");
    }

    @Test
    @DisplayName("should have getTotalTime method")
    void shouldHaveGetTotalTimeMethod() throws NoSuchMethodException {
      final Method method = GcImpactMetrics.GcCollectorMetrics.class.getMethod("getTotalTime");
      assertNotNull(method, "getTotalTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "getTotalTime should return Duration");
    }

    @Test
    @DisplayName("should have getMemoryPoolNames method")
    void shouldHaveGetMemoryPoolNamesMethod() throws NoSuchMethodException {
      final Method method =
          GcImpactMetrics.GcCollectorMetrics.class.getMethod("getMemoryPoolNames");
      assertNotNull(method, "getMemoryPoolNames method should exist");
      assertEquals(
          String[].class, method.getReturnType(), "getMemoryPoolNames should return String[]");
    }

    @Test
    @DisplayName("should have getAverageCollectionTime method")
    void shouldHaveGetAverageCollectionTimeMethod() throws NoSuchMethodException {
      final Method method =
          GcImpactMetrics.GcCollectorMetrics.class.getMethod("getAverageCollectionTime");
      assertNotNull(method, "getAverageCollectionTime method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getAverageCollectionTime should return double");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should capture snapshot")
    void shouldCaptureSnapshot() {
      final GcImpactMetrics.Snapshot snapshot = GcImpactMetrics.captureSnapshot();

      assertNotNull(snapshot, "Snapshot should not be null");
      assertNotNull(snapshot.getTimestamp(), "Timestamp should not be null");
      assertNotNull(snapshot.getTotalGcTime(), "Total GC time should not be null");
      assertTrue(snapshot.getTotalCollections() >= 0, "Collections should be non-negative");
      assertTrue(snapshot.getHeapMemoryUsed() >= 0, "Heap memory should be non-negative");
      assertNotNull(snapshot.getCollectorSnapshots(), "Collector snapshots should not be null");
    }

    @Test
    @DisplayName("should calculate GC impact between snapshots")
    void shouldCalculateGcImpactBetweenSnapshots() {
      final GcImpactMetrics.Snapshot before = GcImpactMetrics.captureSnapshot();

      // Perform some operations
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 10000; i++) {
        sb.append("test");
      }

      final GcImpactMetrics.Snapshot after = GcImpactMetrics.captureSnapshot();
      final Duration operationDuration = Duration.ofMillis(100);

      final GcImpactMetrics metrics = GcImpactMetrics.calculate(before, after, operationDuration);

      assertNotNull(metrics, "Metrics should not be null");
      assertNotNull(metrics.getTotalGcTime(), "Total GC time should not be null");
      assertTrue(metrics.getTotalGcCollections() >= 0, "Collections should be non-negative");
      assertTrue(metrics.getMemoryAllocated() >= 0, "Memory allocated should be non-negative");
      assertNotNull(metrics.getOperationDuration(), "Operation duration should not be null");
      assertTrue(metrics.getGcOverheadPercentage() >= 0, "GC overhead should be non-negative");
      assertNotNull(metrics.getCaptureTime(), "Capture time should not be null");
      assertNotNull(metrics.getCollectorMetrics(), "Collector metrics should not be null");
      assertNotNull(metrics.getSummary(), "Summary should not be null");
    }

    @Test
    @DisplayName("should measure forced GC")
    void shouldMeasureForcedGc() {
      final GcImpactMetrics metrics = GcImpactMetrics.measureForcedGc();

      assertNotNull(metrics, "Metrics should not be null");
      assertNotNull(metrics.getTotalGcTime(), "Total GC time should not be null");
      assertNotNull(metrics.getOperationDuration(), "Operation duration should not be null");
    }
  }
}
