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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcProfiler} interface.
 *
 * <p>GcProfiler provides performance profiling for WebAssembly GC operations, including allocation,
 * field access, array access, and garbage collection metrics.
 */
@DisplayName("GcProfiler Tests")
class GcProfilerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(GcProfiler.class.isInterface(), "GcProfiler should be an interface");
    }
  }

  @Nested
  @DisplayName("Core Method Tests")
  class CoreMethodTests {

    @Test
    @DisplayName("should have start method")
    void shouldHaveStartMethod() throws NoSuchMethodException {
      final Method method = GcProfiler.class.getMethod("start");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have stop method")
    void shouldHaveStopMethod() throws NoSuchMethodException {
      final Method method = GcProfiler.class.getMethod("stop");
      assertEquals(
          GcProfiler.GcProfilingResults.class,
          method.getReturnType(),
          "Should return GcProfilingResults");
    }

    @Test
    @DisplayName("should have isActive method")
    void shouldHaveIsActiveMethod() throws NoSuchMethodException {
      final Method method = GcProfiler.class.getMethod("isActive");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getProfilingDuration method")
    void shouldHaveGetProfilingDurationMethod() throws NoSuchMethodException {
      final Method method = GcProfiler.class.getMethod("getProfilingDuration");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have recordEvent method")
    void shouldHaveRecordEventMethod() throws NoSuchMethodException {
      final Method method =
          GcProfiler.class.getMethod("recordEvent", String.class, Duration.class, Map.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("GcProfilingResults Interface Tests")
  class GcProfilingResultsTests {

    @Test
    @DisplayName("should be a nested interface")
    void shouldBeNestedInterface() {
      assertTrue(
          GcProfiler.GcProfilingResults.class.isInterface(),
          "GcProfilingResults should be an interface");
    }

    @Test
    @DisplayName("should have getTotalDuration method")
    void shouldHaveGetTotalDurationMethod() throws NoSuchMethodException {
      final Method method = GcProfiler.GcProfilingResults.class.getMethod("getTotalDuration");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getSampleCount method")
    void shouldHaveGetSampleCountMethod() throws NoSuchMethodException {
      final Method method = GcProfiler.GcProfilingResults.class.getMethod("getSampleCount");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getAllocationStatistics method")
    void shouldHaveGetAllocationStatisticsMethod() throws NoSuchMethodException {
      final Method method =
          GcProfiler.GcProfilingResults.class.getMethod("getAllocationStatistics");
      assertEquals(
          GcProfiler.AllocationStatistics.class,
          method.getReturnType(),
          "Should return AllocationStatistics");
    }

    @Test
    @DisplayName("should have getFieldAccessStatistics method")
    void shouldHaveGetFieldAccessStatisticsMethod() throws NoSuchMethodException {
      final Method method =
          GcProfiler.GcProfilingResults.class.getMethod("getFieldAccessStatistics");
      assertEquals(
          GcProfiler.FieldAccessStatistics.class,
          method.getReturnType(),
          "Should return FieldAccessStatistics");
    }

    @Test
    @DisplayName("should have getArrayAccessStatistics method")
    void shouldHaveGetArrayAccessStatisticsMethod() throws NoSuchMethodException {
      final Method method =
          GcProfiler.GcProfilingResults.class.getMethod("getArrayAccessStatistics");
      assertEquals(
          GcProfiler.ArrayAccessStatistics.class,
          method.getReturnType(),
          "Should return ArrayAccessStatistics");
    }

    @Test
    @DisplayName("should have getReferenceOperationStatistics method")
    void shouldHaveGetReferenceOperationStatisticsMethod() throws NoSuchMethodException {
      final Method method =
          GcProfiler.GcProfilingResults.class.getMethod("getReferenceOperationStatistics");
      assertEquals(
          GcProfiler.ReferenceOperationStatistics.class,
          method.getReturnType(),
          "Should return ReferenceOperationStatistics");
    }

    @Test
    @DisplayName("should have getGcPerformanceStatistics method")
    void shouldHaveGetGcPerformanceStatisticsMethod() throws NoSuchMethodException {
      final Method method =
          GcProfiler.GcProfilingResults.class.getMethod("getGcPerformanceStatistics");
      assertEquals(
          GcProfiler.GcPerformanceStatistics.class,
          method.getReturnType(),
          "Should return GcPerformanceStatistics");
    }

    @Test
    @DisplayName("should have getHotspots method")
    void shouldHaveGetHotspotsMethod() throws NoSuchMethodException {
      final Method method = GcProfiler.GcProfilingResults.class.getMethod("getHotspots");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getTimeline method")
    void shouldHaveGetTimelineMethod() throws NoSuchMethodException {
      final Method method = GcProfiler.GcProfilingResults.class.getMethod("getTimeline");
      assertEquals(
          GcProfiler.ProfilingTimeline.class,
          method.getReturnType(),
          "Should return ProfilingTimeline");
    }
  }

  @Nested
  @DisplayName("AllocationStatistics Interface Tests")
  class AllocationStatisticsTests {

    @Test
    @DisplayName("should have all allocation statistics methods")
    void shouldHaveAllAllocationStatisticsMethods() {
      final String[] expectedMethods = {
        "getTotalAllocations",
        "getAverageAllocationTime",
        "getAllocationTimePercentiles",
        "getAllocationsByType",
        "getAllocationThroughput",
        "getMemoryThroughput"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcProfiler.AllocationStatistics.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("FieldAccessStatistics Interface Tests")
  class FieldAccessStatisticsTests {

    @Test
    @DisplayName("should have all field access statistics methods")
    void shouldHaveAllFieldAccessStatisticsMethods() {
      final String[] expectedMethods = {
        "getTotalFieldReads",
        "getTotalFieldWrites",
        "getAverageReadTime",
        "getAverageWriteTime",
        "getAccessTimePercentiles",
        "getAccessPatterns"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcProfiler.FieldAccessStatistics.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("ArrayAccessStatistics Interface Tests")
  class ArrayAccessStatisticsTests {

    @Test
    @DisplayName("should have all array access statistics methods")
    void shouldHaveAllArrayAccessStatisticsMethods() {
      final String[] expectedMethods = {
        "getTotalElementReads",
        "getTotalElementWrites",
        "getAverageReadTime",
        "getAverageWriteTime",
        "getAccessTimePercentiles",
        "getOperationThroughput"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcProfiler.ArrayAccessStatistics.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("GcPerformanceStatistics Interface Tests")
  class GcPerformanceStatisticsTests {

    @Test
    @DisplayName("should have all GC performance statistics methods")
    void shouldHaveAllGcPerformanceStatisticsMethods() {
      final String[] expectedMethods = {
        "getTotalCollections",
        "getTotalPauseTime",
        "getAveragePauseTime",
        "getMaxPauseTime",
        "getGcThroughput",
        "getCollectionEfficiency"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcProfiler.GcPerformanceStatistics.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("HotspotType Enum Tests")
  class HotspotTypeEnumTests {

    @Test
    @DisplayName("should have all hotspot types")
    void shouldHaveAllHotspotTypes() {
      final GcProfiler.HotspotType[] values = GcProfiler.HotspotType.values();
      assertEquals(6, values.length, "Should have 6 hotspot types");

      assertNotNull(GcProfiler.HotspotType.valueOf("ALLOCATION"));
      assertNotNull(GcProfiler.HotspotType.valueOf("FIELD_ACCESS"));
      assertNotNull(GcProfiler.HotspotType.valueOf("ARRAY_ACCESS"));
      assertNotNull(GcProfiler.HotspotType.valueOf("TYPE_CHECKING"));
      assertNotNull(GcProfiler.HotspotType.valueOf("GARBAGE_COLLECTION"));
      assertNotNull(GcProfiler.HotspotType.valueOf("REFERENCE_OPERATION"));
    }
  }

  @Nested
  @DisplayName("RegressionSeverity Enum Tests")
  class RegressionSeverityEnumTests {

    @Test
    @DisplayName("should have all regression severity levels")
    void shouldHaveAllRegressionSeverityLevels() {
      final GcProfiler.RegressionSeverity[] values = GcProfiler.RegressionSeverity.values();
      assertEquals(4, values.length, "Should have 4 regression severity levels");

      assertNotNull(GcProfiler.RegressionSeverity.valueOf("MINOR"));
      assertNotNull(GcProfiler.RegressionSeverity.valueOf("MODERATE"));
      assertNotNull(GcProfiler.RegressionSeverity.valueOf("MAJOR"));
      assertNotNull(GcProfiler.RegressionSeverity.valueOf("CRITICAL"));
    }
  }

  @Nested
  @DisplayName("PerformanceHotspot Interface Tests")
  class PerformanceHotspotTests {

    @Test
    @DisplayName("should have all hotspot methods")
    void shouldHaveAllHotspotMethods() {
      final String[] expectedMethods = {
        "getName",
        "getType",
        "getTotalTime",
        "getTimePercentage",
        "getOccurrenceCount",
        "getAverageTime",
        "getDetails"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcProfiler.PerformanceHotspot.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("ProfilingTimeline Interface Tests")
  class ProfilingTimelineTests {

    @Test
    @DisplayName("should have getEvents methods")
    void shouldHaveGetEventsMethods() throws NoSuchMethodException {
      // No-arg version
      final Method method1 = GcProfiler.ProfilingTimeline.class.getMethod("getEvents");
      assertEquals(List.class, method1.getReturnType(), "Should return List");

      // Time range version
      final Method method2 =
          GcProfiler.ProfilingTimeline.class.getMethod("getEvents", Instant.class, Instant.class);
      assertEquals(List.class, method2.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getSamplingInterval method")
    void shouldHaveGetSamplingIntervalMethod() throws NoSuchMethodException {
      final Method method = GcProfiler.ProfilingTimeline.class.getMethod("getSamplingInterval");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }
  }

  @Nested
  @DisplayName("ProfilingEvent Interface Tests")
  class ProfilingEventTests {

    @Test
    @DisplayName("should have all profiling event methods")
    void shouldHaveAllProfilingEventMethods() {
      final String[] expectedMethods = {
        "getTimestamp", "getEventType", "getDuration", "getThreadId", "getMetadata"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcProfiler.ProfilingEvent.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("FieldAccessPattern Interface Tests")
  class FieldAccessPatternTests {

    @Test
    @DisplayName("should have all field access pattern methods")
    void shouldHaveAllFieldAccessPatternMethods() {
      final String[] expectedMethods = {
        "getFieldIdentifier",
        "getAccessCount",
        "getReadWriteRatio",
        "getTemporalLocality",
        "getSpatialLocality"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcProfiler.FieldAccessPattern.class, methodName),
            "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support typical profiling workflow")
    void shouldSupportTypicalProfilingWorkflow() {
      // Documents usage:
      // profiler.start();
      // ... execute GC operations ...
      // GcProfilingResults results = profiler.stop();
      assertTrue(hasMethod(GcProfiler.class, "start"), "Need start method");
      assertTrue(hasMethod(GcProfiler.class, "stop"), "Need stop method");
      assertTrue(hasMethod(GcProfiler.class, "isActive"), "Need isActive method");
    }

    @Test
    @DisplayName("should support custom event recording")
    void shouldSupportCustomEventRecording() {
      // Documents usage:
      // profiler.recordEvent("custom_event", duration, metadata);
      assertTrue(hasMethod(GcProfiler.class, "recordEvent"), "Need recordEvent method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }
}
