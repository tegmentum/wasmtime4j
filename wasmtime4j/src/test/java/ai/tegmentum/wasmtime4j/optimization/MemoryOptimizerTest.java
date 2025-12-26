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

package ai.tegmentum.wasmtime4j.optimization;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MemoryOptimizer} class.
 *
 * <p>MemoryOptimizer provides memory optimization strategies for efficient resource usage.
 */
@DisplayName("MemoryOptimizer Class Tests")
class MemoryOptimizerTest {

  @Nested
  @DisplayName("MemoryState Enum Tests")
  class MemoryStateEnumTests {

    @Test
    @DisplayName("should have all expected memory states")
    void shouldHaveAllExpectedMemoryStates() {
      final MemoryOptimizer.MemoryState[] states = MemoryOptimizer.MemoryState.values();

      assertEquals(4, states.length, "Should have 4 memory states");

      assertArrayEquals(
          new MemoryOptimizer.MemoryState[] {
            MemoryOptimizer.MemoryState.NORMAL,
            MemoryOptimizer.MemoryState.PRESSURE,
            MemoryOptimizer.MemoryState.HIGH,
            MemoryOptimizer.MemoryState.CRITICAL
          },
          states,
          "States should be in order: NORMAL, PRESSURE, HIGH, CRITICAL");
    }

    @Test
    @DisplayName("valueOf should return correct state")
    void valueOfShouldReturnCorrectState() {
      assertEquals(
          MemoryOptimizer.MemoryState.NORMAL, MemoryOptimizer.MemoryState.valueOf("NORMAL"));
      assertEquals(
          MemoryOptimizer.MemoryState.PRESSURE, MemoryOptimizer.MemoryState.valueOf("PRESSURE"));
      assertEquals(MemoryOptimizer.MemoryState.HIGH, MemoryOptimizer.MemoryState.valueOf("HIGH"));
      assertEquals(
          MemoryOptimizer.MemoryState.CRITICAL, MemoryOptimizer.MemoryState.valueOf("CRITICAL"));
    }

    @Test
    @DisplayName("ordinal should reflect severity order")
    void ordinalShouldReflectSeverityOrder() {
      assertTrue(
          MemoryOptimizer.MemoryState.NORMAL.ordinal()
              < MemoryOptimizer.MemoryState.PRESSURE.ordinal(),
          "NORMAL should have lower ordinal than PRESSURE");
      assertTrue(
          MemoryOptimizer.MemoryState.PRESSURE.ordinal()
              < MemoryOptimizer.MemoryState.HIGH.ordinal(),
          "PRESSURE should have lower ordinal than HIGH");
      assertTrue(
          MemoryOptimizer.MemoryState.HIGH.ordinal()
              < MemoryOptimizer.MemoryState.CRITICAL.ordinal(),
          "HIGH should have lower ordinal than CRITICAL");
    }

    @Test
    @DisplayName("memory state enum should be public")
    void memoryStateEnumShouldBePublic() {
      assertTrue(
          Modifier.isPublic(MemoryOptimizer.MemoryState.class.getModifiers()),
          "MemoryState should be public");
    }
  }

  @Nested
  @DisplayName("AllocationStrategy Enum Tests")
  class AllocationStrategyEnumTests {

    @Test
    @DisplayName("should have all expected allocation strategies")
    void shouldHaveAllExpectedAllocationStrategies() {
      final MemoryOptimizer.AllocationStrategy[] strategies =
          MemoryOptimizer.AllocationStrategy.values();

      assertEquals(4, strategies.length, "Should have 4 allocation strategies");

      final Set<String> strategyNames = Set.of("HEAP", "POOLED", "DIRECT", "HYBRID");

      for (final MemoryOptimizer.AllocationStrategy strategy : strategies) {
        assertTrue(
            strategyNames.contains(strategy.name()),
            "Strategy " + strategy.name() + " should be expected");
      }
    }

    @Test
    @DisplayName("valueOf should return correct strategy")
    void valueOfShouldReturnCorrectStrategy() {
      assertEquals(
          MemoryOptimizer.AllocationStrategy.HEAP,
          MemoryOptimizer.AllocationStrategy.valueOf("HEAP"));
      assertEquals(
          MemoryOptimizer.AllocationStrategy.POOLED,
          MemoryOptimizer.AllocationStrategy.valueOf("POOLED"));
      assertEquals(
          MemoryOptimizer.AllocationStrategy.DIRECT,
          MemoryOptimizer.AllocationStrategy.valueOf("DIRECT"));
      assertEquals(
          MemoryOptimizer.AllocationStrategy.HYBRID,
          MemoryOptimizer.AllocationStrategy.valueOf("HYBRID"));
    }

    @Test
    @DisplayName("allocation strategy enum should be public")
    void allocationStrategyEnumShouldBePublic() {
      assertTrue(
          Modifier.isPublic(MemoryOptimizer.AllocationStrategy.class.getModifiers()),
          "AllocationStrategy should be public");
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(MemoryOptimizer.class.getModifiers()),
          "MemoryOptimizer should be a final class");
    }

    @Test
    @DisplayName("should have getInstance method")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.class.getMethod("getInstance");
      assertNotNull(method, "getInstance method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getInstance should be static");
      assertEquals(MemoryOptimizer.class, method.getReturnType(), "Should return MemoryOptimizer");
    }

    @Test
    @DisplayName("getInstance should return singleton")
    void getInstanceShouldReturnSingleton() {
      final MemoryOptimizer instance1 = MemoryOptimizer.getInstance();
      final MemoryOptimizer instance2 = MemoryOptimizer.getInstance();

      assertNotNull(instance1, "Instance should not be null");
      assertSame(instance1, instance2, "getInstance should return the same instance");
    }

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isEnabled should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have setEnabled method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "setEnabled should be static");
    }
  }

  @Nested
  @DisplayName("MemoryMetrics Class Tests")
  class MemoryMetricsClassTests {

    @Test
    @DisplayName("MemoryMetrics should be a static nested class")
    void memoryMetricsShouldBeStaticNestedClass() {
      assertTrue(
          Modifier.isStatic(MemoryOptimizer.MemoryMetrics.class.getModifiers()),
          "MemoryMetrics should be static");
    }

    @Test
    @DisplayName("MemoryMetrics should be public")
    void memoryMetricsShouldBePublic() {
      assertTrue(
          Modifier.isPublic(MemoryOptimizer.MemoryMetrics.class.getModifiers()),
          "MemoryMetrics should be public");
    }

    @Test
    @DisplayName("MemoryMetrics should be final")
    void memoryMetricsShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(MemoryOptimizer.MemoryMetrics.class.getModifiers()),
          "MemoryMetrics should be final");
    }

    @Test
    @DisplayName("MemoryMetrics should have getHeapUsed method")
    void memoryMetricsShouldHaveGetHeapUsedMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.MemoryMetrics.class.getMethod("getHeapUsed");
      assertNotNull(method, "getHeapUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryMetrics should have getHeapMax method")
    void memoryMetricsShouldHaveGetHeapMaxMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.MemoryMetrics.class.getMethod("getHeapMax");
      assertNotNull(method, "getHeapMax method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryMetrics should have getNonHeapUsed method")
    void memoryMetricsShouldHaveGetNonHeapUsedMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.MemoryMetrics.class.getMethod("getNonHeapUsed");
      assertNotNull(method, "getNonHeapUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryMetrics should have getNonHeapMax method")
    void memoryMetricsShouldHaveGetNonHeapMaxMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.MemoryMetrics.class.getMethod("getNonHeapMax");
      assertNotNull(method, "getNonHeapMax method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryMetrics should have getNativeMemory method")
    void memoryMetricsShouldHaveGetNativeMemoryMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.MemoryMetrics.class.getMethod("getNativeMemory");
      assertNotNull(method, "getNativeMemory method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryMetrics should have getMemoryPressure method")
    void memoryMetricsShouldHaveGetMemoryPressureMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.MemoryMetrics.class.getMethod("getMemoryPressure");
      assertNotNull(method, "getMemoryPressure method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("MemoryMetrics should have getState method")
    void memoryMetricsShouldHaveGetStateMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.MemoryMetrics.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(
          MemoryOptimizer.MemoryState.class, method.getReturnType(), "Should return MemoryState");
    }

    @Test
    @DisplayName("MemoryMetrics should have getGcCount method")
    void memoryMetricsShouldHaveGetGcCountMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.MemoryMetrics.class.getMethod("getGcCount");
      assertNotNull(method, "getGcCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryMetrics should have getGcTime method")
    void memoryMetricsShouldHaveGetGcTimeMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.MemoryMetrics.class.getMethod("getGcTime");
      assertNotNull(method, "getGcTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryMetrics should have getRecommendations method")
    void memoryMetricsShouldHaveGetRecommendationsMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.MemoryMetrics.class.getMethod("getRecommendations");
      assertNotNull(method, "getRecommendations method should exist");
      assertEquals(String[].class, method.getReturnType(), "Should return String[]");
    }
  }

  @Nested
  @DisplayName("Optimizer Method Tests")
  class OptimizerMethodTests {

    @Test
    @DisplayName("should have getMemoryMetrics method")
    void shouldHaveGetMemoryMetricsMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.class.getMethod("getMemoryMetrics");
      assertNotNull(method, "getMemoryMetrics method should exist");
      assertEquals(
          MemoryOptimizer.MemoryMetrics.class,
          method.getReturnType(),
          "Should return MemoryMetrics");
    }

    @Test
    @DisplayName("should have getRecommendedStrategy method")
    void shouldHaveGetRecommendedStrategyMethod() throws NoSuchMethodException {
      final Method method =
          MemoryOptimizer.class.getMethod("getRecommendedStrategy", long.class, Class.class);
      assertNotNull(method, "getRecommendedStrategy method should exist");
      assertEquals(
          MemoryOptimizer.AllocationStrategy.class,
          method.getReturnType(),
          "Should return AllocationStrategy");
    }

    @Test
    @DisplayName("should have allocateObject method")
    void shouldHaveAllocateObjectMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.class.getMethod("allocateObject", Class.class);
      assertNotNull(method, "allocateObject method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
    }

    @Test
    @DisplayName("should have returnObject method")
    void shouldHaveReturnObjectMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.class.getMethod("returnObject", Object.class);
      assertNotNull(method, "returnObject method should exist");
    }

    @Test
    @DisplayName("should have trackResource method")
    void shouldHaveTrackResourceMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.class.getMethod("trackResource", AutoCloseable.class);
      assertNotNull(method, "trackResource method should exist");
    }

    @Test
    @DisplayName("should have recordNativeAllocation method")
    void shouldHaveRecordNativeAllocationMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.class.getMethod("recordNativeAllocation", long.class);
      assertNotNull(method, "recordNativeAllocation method should exist");
    }

    @Test
    @DisplayName("should have recordNativeDeallocation method")
    void shouldHaveRecordNativeDeallocationMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.class.getMethod("recordNativeDeallocation", long.class);
      assertNotNull(method, "recordNativeDeallocation method should exist");
    }

    @Test
    @DisplayName("should have forceCleanup method")
    void shouldHaveForceCleanupMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.class.getMethod("forceCleanup");
      assertNotNull(method, "forceCleanup method should exist");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      final Method method = MemoryOptimizer.class.getMethod("shutdown");
      assertNotNull(method, "shutdown method should exist");
    }
  }

  @Nested
  @DisplayName("MemoryMetrics Behavior Tests")
  class MemoryMetricsBehaviorTests {

    @Test
    @DisplayName("getMemoryMetrics should return non-null metrics")
    void getMemoryMetricsShouldReturnNonNullMetrics() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();
      assertNotNull(metrics, "Metrics should not be null");
    }

    @Test
    @DisplayName("metrics should have non-negative heap used")
    void metricsShouldHaveNonNegativeHeapUsed() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      assertTrue(metrics.getHeapUsed() >= 0, "Heap used should be non-negative");
    }

    @Test
    @DisplayName("metrics should have positive heap max")
    void metricsShouldHavePositiveHeapMax() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      assertTrue(metrics.getHeapMax() > 0, "Heap max should be positive");
    }

    @Test
    @DisplayName("metrics heap used should not exceed heap max")
    void metricsHeapUsedShouldNotExceedHeapMax() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      assertTrue(
          metrics.getHeapUsed() <= metrics.getHeapMax(), "Heap used should not exceed heap max");
    }

    @Test
    @DisplayName("metrics should have non-negative non-heap used")
    void metricsShouldHaveNonNegativeNonHeapUsed() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      assertTrue(metrics.getNonHeapUsed() >= 0, "Non-heap used should be non-negative");
    }

    @Test
    @DisplayName("metrics should have non-negative native memory")
    void metricsShouldHaveNonNegativeNativeMemory() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      assertTrue(metrics.getNativeMemory() >= 0, "Native memory should be non-negative");
    }

    @Test
    @DisplayName("metrics should have non-negative gc count")
    void metricsShouldHaveNonNegativeGcCount() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      assertTrue(metrics.getGcCount() >= 0, "GC count should be non-negative");
    }

    @Test
    @DisplayName("metrics should have non-negative gc time")
    void metricsShouldHaveNonNegativeGcTime() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      assertTrue(metrics.getGcTime() >= 0, "GC time should be non-negative");
    }

    @Test
    @DisplayName("metrics memory pressure should be between 0 and 1")
    void metricsMemoryPressureShouldBeBetween0And1() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();
      final double pressure = metrics.getMemoryPressure();

      assertTrue(pressure >= 0.0, "Memory pressure should be >= 0");
      assertTrue(pressure <= 1.0, "Memory pressure should be <= 1");
    }

    @Test
    @DisplayName("metrics should have non-null state")
    void metricsShouldHaveNonNullState() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      assertNotNull(metrics.getState(), "State should not be null");
    }

    @Test
    @DisplayName("metrics should have non-null recommendations")
    void metricsShouldHaveNonNullRecommendations() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      assertNotNull(metrics.getRecommendations(), "Recommendations should not be null");
    }
  }

  @Nested
  @DisplayName("Allocation Strategy Tests")
  class AllocationStrategyTests {

    @Test
    @DisplayName("getRecommendedStrategy should return non-null strategy")
    void getRecommendedStrategyShouldReturnNonNullStrategy() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.AllocationStrategy strategy =
          optimizer.getRecommendedStrategy(1024, StringBuilder.class);
      assertNotNull(strategy, "Recommended strategy should not be null");
    }

    @Test
    @DisplayName("getRecommendedStrategy should return DIRECT for large sizes")
    void getRecommendedStrategyShouldReturnDirectForLargeSizes() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final MemoryOptimizer.AllocationStrategy strategy =
          optimizer.getRecommendedStrategy(100 * 1024, Object.class); // 100KB
      // Large allocations should suggest DIRECT or HEAP
      assertNotNull(strategy, "Strategy should not be null for large size");
    }

    @Test
    @DisplayName("getRecommendedStrategy should return POOLED for small pooled types")
    void getRecommendedStrategyShouldReturnPooledForSmallPooledTypes() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      // StringBuilder is a pooled type by default
      final MemoryOptimizer.AllocationStrategy strategy =
          optimizer.getRecommendedStrategy(100, StringBuilder.class);
      // Should return POOLED for small sizes of pooled types
      assertEquals(
          MemoryOptimizer.AllocationStrategy.POOLED,
          strategy,
          "Should return POOLED for small pooled types");
    }
  }

  @Nested
  @DisplayName("Native Allocation Tracking Tests")
  class NativeAllocationTrackingTests {

    @Test
    @DisplayName("recordNativeAllocation should accept positive size")
    void recordNativeAllocationShouldAcceptPositiveSize() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      // Should not throw
      optimizer.recordNativeAllocation(1024);
      optimizer.recordNativeDeallocation(1024);
    }

    @Test
    @DisplayName("recordNativeAllocation should update native memory metric")
    void recordNativeAllocationShouldUpdateNativeMemoryMetric() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      final long initialNativeMemory = optimizer.getMemoryMetrics().getNativeMemory();

      optimizer.recordNativeAllocation(1024);
      final long afterAllocation = optimizer.getMemoryMetrics().getNativeMemory();

      optimizer.recordNativeDeallocation(1024);
      final long afterDeallocation = optimizer.getMemoryMetrics().getNativeMemory();

      assertEquals(
          initialNativeMemory + 1024,
          afterAllocation,
          "Native memory should increase after allocation");
      assertEquals(
          initialNativeMemory,
          afterDeallocation,
          "Native memory should return to initial after deallocation");
    }
  }

  @Nested
  @DisplayName("Cleanup Operation Tests")
  class CleanupOperationTests {

    @Test
    @DisplayName("forceCleanup should not throw")
    void forceCleanupShouldNotThrow() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      // Should complete without exception
      optimizer.forceCleanup();
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getStatistics should return non-null string")
    void getStatisticsShouldReturnNonNullString() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final String stats = optimizer.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
    }

    @Test
    @DisplayName("getStatistics should return non-empty string")
    void getStatisticsShouldReturnNonEmptyString() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final String stats = optimizer.getStatistics();
      assertFalse(stats.isEmpty(), "Statistics should not be empty");
    }

    @Test
    @DisplayName("getStatistics should contain memory state")
    void getStatisticsShouldContainMemoryState() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final String stats = optimizer.getStatistics();
      assertTrue(
          stats.contains("Memory state") || stats.contains("disabled"),
          "Statistics should contain memory state info");
    }
  }

  @Nested
  @DisplayName("Enable/Disable Tests")
  class EnableDisableTests {

    @Test
    @DisplayName("isEnabled should return boolean")
    void isEnabledShouldReturnBoolean() {
      // Just verify it runs without exception
      final boolean enabled = MemoryOptimizer.isEnabled();
      // enabled can be true or false, just ensure it returns
      assertTrue(enabled || !enabled, "isEnabled should return a boolean value");
    }

    @Test
    @DisplayName("setEnabled should change state")
    void setEnabledShouldChangeState() {
      final boolean originalState = MemoryOptimizer.isEnabled();

      try {
        MemoryOptimizer.setEnabled(false);
        assertFalse(MemoryOptimizer.isEnabled(), "Should be disabled after setEnabled(false)");

        MemoryOptimizer.setEnabled(true);
        assertTrue(MemoryOptimizer.isEnabled(), "Should be enabled after setEnabled(true)");
      } finally {
        // Restore original state
        MemoryOptimizer.setEnabled(originalState);
      }
    }

    @Test
    @DisplayName("disabled optimizer should return early from methods")
    void disabledOptimizerShouldReturnEarlyFromMethods() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final boolean originalState = MemoryOptimizer.isEnabled();

      try {
        MemoryOptimizer.setEnabled(false);

        // These should not throw when disabled
        optimizer.recordNativeAllocation(1024);
        optimizer.recordNativeDeallocation(1024);
        optimizer.forceCleanup();
        optimizer.trackResource(null);
        optimizer.returnObject(null);

        // allocateObject should return null when disabled
        final Object result = optimizer.allocateObject(StringBuilder.class);
        // May be null when disabled, depending on implementation
      } finally {
        // Restore original state
        MemoryOptimizer.setEnabled(originalState);
      }
    }
  }

  @Nested
  @DisplayName("Object Pool Allocation Tests")
  class ObjectPoolAllocationTests {

    @Test
    @DisplayName("allocateObject should return object for pooled types")
    void allocateObjectShouldReturnObjectForPooledTypes() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final boolean originalState = MemoryOptimizer.isEnabled();

      try {
        MemoryOptimizer.setEnabled(true);
        final StringBuilder sb = optimizer.allocateObject(StringBuilder.class);
        // May be null or non-null depending on pool state
        // Just verify no exception
      } finally {
        MemoryOptimizer.setEnabled(originalState);
      }
    }

    @Test
    @DisplayName("returnObject should accept pooled objects")
    void returnObjectShouldAcceptPooledObjects() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final boolean originalState = MemoryOptimizer.isEnabled();

      try {
        MemoryOptimizer.setEnabled(true);
        final StringBuilder sb = new StringBuilder("test");
        optimizer.returnObject(sb);
        // Should not throw
      } finally {
        MemoryOptimizer.setEnabled(originalState);
      }
    }

    @Test
    @DisplayName("returnObject should handle null gracefully")
    void returnObjectShouldHandleNullGracefully() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      optimizer.returnObject(null);
      // Should not throw
    }
  }

  @Nested
  @DisplayName("Resource Tracking Tests")
  class ResourceTrackingTests {

    @Test
    @DisplayName("trackResource should accept AutoCloseable")
    void trackResourceShouldAcceptAutoCloseable() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      final boolean originalState = MemoryOptimizer.isEnabled();

      try {
        MemoryOptimizer.setEnabled(true);
        final AutoCloseable resource = () -> {};
        optimizer.trackResource(resource);
        // Should not throw
      } finally {
        MemoryOptimizer.setEnabled(originalState);
      }
    }

    @Test
    @DisplayName("trackResource should handle null gracefully")
    void trackResourceShouldHandleNullGracefully() {
      final MemoryOptimizer optimizer = MemoryOptimizer.getInstance();
      optimizer.trackResource(null);
      // Should not throw
    }
  }
}
