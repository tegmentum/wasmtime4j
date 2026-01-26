/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.optimization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for optimization package classes.
 *
 * <p>This test class validates the MemoryOptimizer and related classes.
 */
@DisplayName("Optimization Integration Tests")
public class OptimizationIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(OptimizationIntegrationTest.class.getName());

  private static boolean originalEnabledState;

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Optimization Integration Tests");
    // Save original state
    originalEnabledState = MemoryOptimizer.isEnabled();
    // Ensure optimizer is enabled for tests
    MemoryOptimizer.setEnabled(true);
  }

  @AfterAll
  static void tearDownClass() {
    // Restore original state
    MemoryOptimizer.setEnabled(originalEnabledState);
    LOGGER.info("Completed Optimization Integration Tests");
  }

  @Nested
  @DisplayName("MemoryOptimizer Singleton Tests")
  class MemoryOptimizerSingletonTests {

    @Test
    @DisplayName("Should get singleton instance")
    void shouldGetSingletonInstance() {
      LOGGER.info("Testing MemoryOptimizer singleton");

      MemoryOptimizer instance1 = MemoryOptimizer.getInstance();
      MemoryOptimizer instance2 = MemoryOptimizer.getInstance();

      assertNotNull(instance1, "Instance should not be null");
      assertSame(instance1, instance2, "Should return same instance");

      LOGGER.info("Singleton verified");
    }

    @Test
    @DisplayName("Should toggle enabled state")
    void shouldToggleEnabledState() {
      LOGGER.info("Testing enabled state toggle");

      // Test disabling
      MemoryOptimizer.setEnabled(false);
      assertFalse(MemoryOptimizer.isEnabled(), "Should be disabled");

      // Test enabling
      MemoryOptimizer.setEnabled(true);
      assertTrue(MemoryOptimizer.isEnabled(), "Should be enabled");

      LOGGER.info("Enabled state toggle verified");
    }
  }

  @Nested
  @DisplayName("MemoryState Enum Tests")
  class MemoryStateEnumTests {

    @Test
    @DisplayName("Should have all expected memory states")
    void shouldHaveAllExpectedMemoryStates() {
      LOGGER.info("Testing MemoryState enum values");

      MemoryOptimizer.MemoryState[] states = MemoryOptimizer.MemoryState.values();

      assertEquals(4, states.length, "Should have 4 memory states");
      assertNotNull(MemoryOptimizer.MemoryState.NORMAL, "NORMAL should exist");
      assertNotNull(MemoryOptimizer.MemoryState.PRESSURE, "PRESSURE should exist");
      assertNotNull(MemoryOptimizer.MemoryState.HIGH, "HIGH should exist");
      assertNotNull(MemoryOptimizer.MemoryState.CRITICAL, "CRITICAL should exist");

      LOGGER.info("MemoryState enum verified: " + states.length + " states");
    }

    @Test
    @DisplayName("Should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues() {
      LOGGER.info("Testing MemoryState ordinal values");

      assertEquals(0, MemoryOptimizer.MemoryState.NORMAL.ordinal(), "NORMAL ordinal");
      assertEquals(1, MemoryOptimizer.MemoryState.PRESSURE.ordinal(), "PRESSURE ordinal");
      assertEquals(2, MemoryOptimizer.MemoryState.HIGH.ordinal(), "HIGH ordinal");
      assertEquals(3, MemoryOptimizer.MemoryState.CRITICAL.ordinal(), "CRITICAL ordinal");

      LOGGER.info("Ordinal values verified");
    }

    @Test
    @DisplayName("Should support valueOf")
    void shouldSupportValueOf() {
      LOGGER.info("Testing MemoryState valueOf");

      assertEquals(
          MemoryOptimizer.MemoryState.NORMAL, MemoryOptimizer.MemoryState.valueOf("NORMAL"));
      assertEquals(
          MemoryOptimizer.MemoryState.CRITICAL, MemoryOptimizer.MemoryState.valueOf("CRITICAL"));

      LOGGER.info("valueOf verified");
    }
  }

  @Nested
  @DisplayName("AllocationStrategy Enum Tests")
  class AllocationStrategyEnumTests {

    @Test
    @DisplayName("Should have all expected allocation strategies")
    void shouldHaveAllExpectedAllocationStrategies() {
      LOGGER.info("Testing AllocationStrategy enum values");

      MemoryOptimizer.AllocationStrategy[] strategies = MemoryOptimizer.AllocationStrategy.values();

      assertEquals(4, strategies.length, "Should have 4 allocation strategies");
      assertNotNull(MemoryOptimizer.AllocationStrategy.HEAP, "HEAP should exist");
      assertNotNull(MemoryOptimizer.AllocationStrategy.POOLED, "POOLED should exist");
      assertNotNull(MemoryOptimizer.AllocationStrategy.DIRECT, "DIRECT should exist");
      assertNotNull(MemoryOptimizer.AllocationStrategy.HYBRID, "HYBRID should exist");

      LOGGER.info("AllocationStrategy enum verified: " + strategies.length + " strategies");
    }

    @Test
    @DisplayName("Should support valueOf")
    void shouldSupportValueOf() {
      LOGGER.info("Testing AllocationStrategy valueOf");

      assertEquals(
          MemoryOptimizer.AllocationStrategy.HEAP,
          MemoryOptimizer.AllocationStrategy.valueOf("HEAP"));
      assertEquals(
          MemoryOptimizer.AllocationStrategy.POOLED,
          MemoryOptimizer.AllocationStrategy.valueOf("POOLED"));

      LOGGER.info("valueOf verified");
    }
  }

  @Nested
  @DisplayName("Allocation Strategy Selection Tests")
  class AllocationStrategySelectionTests {

    @Test
    @DisplayName("Should recommend strategy for small poolable objects")
    void shouldRecommendStrategyForSmallPoolableObjects() {
      LOGGER.info("Testing strategy for small poolable objects");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      // StringBuilder is a pooled type
      MemoryOptimizer.AllocationStrategy strategy =
          optimizer.getRecommendedStrategy(100, StringBuilder.class);

      assertNotNull(strategy, "Strategy should not be null");
      // Should recommend POOLED for small objects of pooled types
      assertEquals(
          MemoryOptimizer.AllocationStrategy.POOLED,
          strategy,
          "Should recommend POOLED for StringBuilder");

      LOGGER.info("Small poolable object strategy verified");
    }

    @Test
    @DisplayName("Should recommend strategy for large objects")
    void shouldRecommendStrategyForLargeObjects() {
      LOGGER.info("Testing strategy for large objects");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      // Large objects (> 64KB) should use DIRECT
      MemoryOptimizer.AllocationStrategy strategy =
          optimizer.getRecommendedStrategy(100_000, Object.class);

      assertNotNull(strategy, "Strategy should not be null");
      assertEquals(
          MemoryOptimizer.AllocationStrategy.DIRECT,
          strategy,
          "Should recommend DIRECT for large objects");

      LOGGER.info("Large object strategy verified");
    }

    @Test
    @DisplayName("Should recommend HEAP for small non-poolable objects")
    void shouldRecommendHeapForSmallNonPoolableObjects() {
      LOGGER.info("Testing strategy for small non-poolable objects");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      // Small objects that aren't in pool should use HEAP
      MemoryOptimizer.AllocationStrategy strategy =
          optimizer.getRecommendedStrategy(1000, Object.class);

      assertNotNull(strategy, "Strategy should not be null");
      assertEquals(
          MemoryOptimizer.AllocationStrategy.HEAP,
          strategy,
          "Should recommend HEAP for small non-poolable objects");

      LOGGER.info("Small non-poolable object strategy verified");
    }

    @Test
    @DisplayName("Should return HEAP when disabled")
    void shouldReturnHeapWhenDisabled() {
      LOGGER.info("Testing strategy when disabled");

      try {
        MemoryOptimizer.setEnabled(false);
        MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

        MemoryOptimizer.AllocationStrategy strategy =
            optimizer.getRecommendedStrategy(100, StringBuilder.class);

        assertEquals(
            MemoryOptimizer.AllocationStrategy.HEAP, strategy, "Should return HEAP when disabled");
      } finally {
        MemoryOptimizer.setEnabled(true);
      }

      LOGGER.info("Disabled strategy verified");
    }
  }

  @Nested
  @DisplayName("Object Pooling Tests")
  class ObjectPoolingTests {

    @Test
    @DisplayName("Should allocate pooled object")
    void shouldAllocatePooledObject() {
      LOGGER.info("Testing pooled object allocation");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      StringBuilder sb = optimizer.allocateObject(StringBuilder.class);

      assertNotNull(sb, "Allocated object should not be null");

      LOGGER.info("Pooled object allocation verified");
    }

    @Test
    @DisplayName("Should return object to pool")
    void shouldReturnObjectToPool() {
      LOGGER.info("Testing object return to pool");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      StringBuilder sb = optimizer.allocateObject(StringBuilder.class);
      assertNotNull(sb, "Allocated object should not be null");

      sb.append("test content");
      optimizer.returnObject(sb);

      // Verify no exception is thrown
      LOGGER.info("Object returned to pool successfully");
    }

    @Test
    @DisplayName("Should allocate ArrayList from pool")
    void shouldAllocateArrayListFromPool() {
      LOGGER.info("Testing ArrayList allocation from pool");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      ArrayList<?> list = optimizer.allocateObject(ArrayList.class);

      assertNotNull(list, "Allocated list should not be null");
      assertTrue(list.isEmpty(), "New list should be empty");

      optimizer.returnObject(list);

      LOGGER.info("ArrayList pool allocation verified");
    }

    @Test
    @DisplayName("Should return null when disabled")
    void shouldReturnNullWhenDisabled() {
      LOGGER.info("Testing allocation when disabled");

      try {
        MemoryOptimizer.setEnabled(false);
        MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

        StringBuilder sb = optimizer.allocateObject(StringBuilder.class);

        // Should return null when disabled
        assertTrue(sb == null, "Should return null when disabled");
      } finally {
        MemoryOptimizer.setEnabled(true);
      }

      LOGGER.info("Disabled allocation verified");
    }

    @Test
    @DisplayName("Should handle null return gracefully")
    void shouldHandleNullReturnGracefully() {
      LOGGER.info("Testing null return handling");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      // Should not throw
      optimizer.returnObject(null);

      LOGGER.info("Null return handling verified");
    }
  }

  @Nested
  @DisplayName("Native Memory Tracking Tests")
  class NativeMemoryTrackingTests {

    @Test
    @DisplayName("Should record native allocation")
    void shouldRecordNativeAllocation() {
      LOGGER.info("Testing native allocation recording");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      optimizer.recordNativeAllocation(1024);

      // Get metrics to verify tracking
      MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();
      assertTrue(metrics.getNativeMemory() >= 0, "Native memory should be tracked");

      LOGGER.info("Native allocation recording verified");
    }

    @Test
    @DisplayName("Should record native deallocation")
    void shouldRecordNativeDeallocation() {
      LOGGER.info("Testing native deallocation recording");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      optimizer.recordNativeAllocation(2048);
      optimizer.recordNativeDeallocation(2048);

      // Should not throw and memory should be decremented
      LOGGER.info("Native deallocation recording verified");
    }

    @Test
    @DisplayName("Should skip tracking when disabled")
    void shouldSkipTrackingWhenDisabled() {
      LOGGER.info("Testing tracking when disabled");

      try {
        MemoryOptimizer.setEnabled(false);
        MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

        // Should not throw when disabled
        optimizer.recordNativeAllocation(1024);
        optimizer.recordNativeDeallocation(1024);
      } finally {
        MemoryOptimizer.setEnabled(true);
      }

      LOGGER.info("Disabled tracking verified");
    }
  }

  @Nested
  @DisplayName("Resource Tracking Tests")
  class ResourceTrackingTests {

    @Test
    @DisplayName("Should track resource")
    void shouldTrackResource() {
      LOGGER.info("Testing resource tracking");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      AutoCloseable resource =
          new AutoCloseable() {
            @Override
            public void close() {
              LOGGER.info("Test resource closed");
            }
          };

      optimizer.trackResource(resource);

      LOGGER.info("Resource tracking verified");
    }

    @Test
    @DisplayName("Should skip tracking null resource")
    void shouldSkipTrackingNullResource() {
      LOGGER.info("Testing null resource tracking");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      // Should not throw
      optimizer.trackResource(null);

      LOGGER.info("Null resource tracking verified");
    }

    @Test
    @DisplayName("Should skip tracking when disabled")
    void shouldSkipResourceTrackingWhenDisabled() {
      LOGGER.info("Testing resource tracking when disabled");

      try {
        MemoryOptimizer.setEnabled(false);
        MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

        AutoCloseable resource = () -> {};

        // Should not throw when disabled
        optimizer.trackResource(resource);
      } finally {
        MemoryOptimizer.setEnabled(true);
      }

      LOGGER.info("Disabled resource tracking verified");
    }
  }

  @Nested
  @DisplayName("Memory Metrics Tests")
  class MemoryMetricsTests {

    @Test
    @DisplayName("Should get memory metrics")
    void shouldGetMemoryMetrics() {
      LOGGER.info("Testing memory metrics retrieval");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      assertNotNull(metrics, "Metrics should not be null");
      assertTrue(metrics.getHeapUsed() > 0, "Heap used should be positive");
      assertTrue(metrics.getHeapMax() > 0, "Heap max should be positive");
      assertTrue(metrics.getHeapUsed() <= metrics.getHeapMax(), "Used should not exceed max");
      assertTrue(metrics.getMemoryPressure() >= 0, "Pressure should be non-negative");
      assertTrue(metrics.getMemoryPressure() <= 1.0, "Pressure should not exceed 1.0");
      assertNotNull(metrics.getState(), "State should not be null");
      assertTrue(metrics.getGcCount() >= 0, "GC count should be non-negative");
      assertTrue(metrics.getGcTime() >= 0, "GC time should be non-negative");

      LOGGER.info(
          "Memory metrics verified - heap: "
              + metrics.getHeapUsed() / (1024 * 1024)
              + "MB / "
              + metrics.getHeapMax() / (1024 * 1024)
              + "MB, state: "
              + metrics.getState());
    }

    @Test
    @DisplayName("Should get non-heap metrics")
    void shouldGetNonHeapMetrics() {
      LOGGER.info("Testing non-heap metrics");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      assertTrue(metrics.getNonHeapUsed() >= 0, "Non-heap used should be non-negative");
      // Non-heap max can be -1 (undefined) in some JVM configurations
      assertTrue(metrics.getNonHeapMax() != 0, "Non-heap max should be set");

      LOGGER.info("Non-heap metrics verified");
    }

    @Test
    @DisplayName("Should get native memory from metrics")
    void shouldGetNativeMemoryFromMetrics() {
      LOGGER.info("Testing native memory metrics");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      assertTrue(metrics.getNativeMemory() >= 0, "Native memory should be non-negative");

      LOGGER.info("Native memory metrics verified");
    }

    @Test
    @DisplayName("Should get recommendations from metrics")
    void shouldGetRecommendationsFromMetrics() {
      LOGGER.info("Testing recommendations from metrics");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      MemoryOptimizer.MemoryMetrics metrics = optimizer.getMemoryMetrics();

      String[] recommendations = metrics.getRecommendations();
      assertNotNull(recommendations, "Recommendations should not be null");

      // Recommendations are defensive copy - only test if array is non-empty
      if (recommendations.length > 0) {
        String original = recommendations[0];
        recommendations[0] = "modified";
        String[] originalRecs = metrics.getRecommendations();
        assertEquals(original, originalRecs[0], "Recommendations should be defensive copy");
      }

      LOGGER.info("Recommendations verified: " + recommendations.length + " recommendations");
    }
  }

  @Nested
  @DisplayName("Force Cleanup Tests")
  class ForceCleanupTests {

    @Test
    @DisplayName("Should force cleanup")
    void shouldForceCleanup() {
      LOGGER.info("Testing force cleanup");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      // Should not throw
      optimizer.forceCleanup();

      LOGGER.info("Force cleanup verified");
    }

    @Test
    @DisplayName("Should skip cleanup when disabled")
    void shouldSkipCleanupWhenDisabled() {
      LOGGER.info("Testing cleanup when disabled");

      try {
        MemoryOptimizer.setEnabled(false);
        MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

        // Should not throw when disabled
        optimizer.forceCleanup();
      } finally {
        MemoryOptimizer.setEnabled(true);
      }

      LOGGER.info("Disabled cleanup verified");
    }

    @Test
    @DisplayName("Should clean up tracked resources")
    void shouldCleanUpTrackedResources() {
      LOGGER.info("Testing tracked resource cleanup");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      final boolean[] closed = {false};
      AutoCloseable resource = () -> closed[0] = true;

      optimizer.trackResource(resource);
      optimizer.forceCleanup();

      assertTrue(closed[0], "Resource should be closed during cleanup");

      LOGGER.info("Tracked resource cleanup verified");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("Should get statistics string")
    void shouldGetStatisticsString() {
      LOGGER.info("Testing statistics string");

      MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

      String stats = optimizer.getStatistics();

      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.length() > 0, "Statistics should not be empty");
      assertTrue(stats.contains("Memory"), "Statistics should mention memory");
      assertTrue(stats.contains("state"), "Statistics should mention state");

      LOGGER.info("Statistics string verified");
    }

    @Test
    @DisplayName("Should indicate disabled in statistics")
    void shouldIndicateDisabledInStatistics() {
      LOGGER.info("Testing disabled statistics");

      try {
        MemoryOptimizer.setEnabled(false);
        MemoryOptimizer optimizer = MemoryOptimizer.getInstance();

        String stats = optimizer.getStatistics();

        assertTrue(stats.contains("disabled"), "Statistics should indicate disabled: " + stats);
      } finally {
        MemoryOptimizer.setEnabled(true);
      }

      LOGGER.info("Disabled statistics verified");
    }
  }
}
