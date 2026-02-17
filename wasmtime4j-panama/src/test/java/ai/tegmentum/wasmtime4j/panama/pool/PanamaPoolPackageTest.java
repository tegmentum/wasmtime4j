/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j.panama.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Panama pool package.
 *
 * <p>This test covers behavioral tests for classes in the ai.tegmentum.wasmtime4j.panama.pool
 * package including PanamaPoolStatistics and PanamaPoolingAllocatorConfigBuilder.
 */
@DisplayName("Panama Pool Package Tests")
class PanamaPoolPackageTest {

  // ========================================================================
  // PanamaPoolStatistics Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaPoolStatistics Tests")
  class PanamaPoolStatisticsTests {

    @Test
    @DisplayName("PanamaPoolStatistics default constructor should create empty statistics")
    void panamaPoolStatisticsDefaultConstructorShouldCreateEmptyStats() {
      PanamaPoolStatistics stats = new PanamaPoolStatistics();

      assertNotNull(stats, "PanamaPoolStatistics should be created");
      assertEquals(0, stats.getInstancesAllocated(), "Instances allocated should be 0");
      assertEquals(0, stats.getInstancesReused(), "Instances reused should be 0");
      assertEquals(0, stats.getInstancesCreated(), "Instances created should be 0");
      assertEquals(0, stats.getMemoryPoolsAllocated(), "Memory pools allocated should be 0");
      assertEquals(0, stats.getMemoryPoolsReused(), "Memory pools reused should be 0");
      assertEquals(0, stats.getStackPoolsAllocated(), "Stack pools allocated should be 0");
      assertEquals(0, stats.getStackPoolsReused(), "Stack pools reused should be 0");
      assertEquals(0, stats.getTablePoolsAllocated(), "Table pools allocated should be 0");
      assertEquals(0, stats.getTablePoolsReused(), "Table pools reused should be 0");
      assertEquals(0, stats.getPeakMemoryUsage(), "Peak memory usage should be 0");
      assertEquals(0, stats.getCurrentMemoryUsage(), "Current memory usage should be 0");
      assertEquals(0, stats.getAllocationFailures(), "Allocation failures should be 0");
    }

    @Test
    @DisplayName("PanamaPoolStatistics full constructor should set all values")
    void panamaPoolStatisticsFullConstructorShouldSetAllValues() {
      PanamaPoolStatistics stats =
          new PanamaPoolStatistics(
              100, // instancesAllocated
              50, // instancesReused
              50, // instancesCreated
              200, // memoryPoolsAllocated
              100, // memoryPoolsReused
              300, // stackPoolsAllocated
              150, // stackPoolsReused
              400, // tablePoolsAllocated
              200, // tablePoolsReused
              1000000, // peakMemoryUsage
              500000, // currentMemoryUsage
              5, // allocationFailures
              1000000000, // poolWarmingTimeNanos (1 second)
              1000000 // averageAllocationTimeNanos (1 millisecond)
              );

      assertEquals(100, stats.getInstancesAllocated(), "Instances allocated should match");
      assertEquals(50, stats.getInstancesReused(), "Instances reused should match");
      assertEquals(50, stats.getInstancesCreated(), "Instances created should match");
      assertEquals(200, stats.getMemoryPoolsAllocated(), "Memory pools allocated should match");
      assertEquals(100, stats.getMemoryPoolsReused(), "Memory pools reused should match");
      assertEquals(300, stats.getStackPoolsAllocated(), "Stack pools allocated should match");
      assertEquals(150, stats.getStackPoolsReused(), "Stack pools reused should match");
      assertEquals(400, stats.getTablePoolsAllocated(), "Table pools allocated should match");
      assertEquals(200, stats.getTablePoolsReused(), "Table pools reused should match");
      assertEquals(1000000, stats.getPeakMemoryUsage(), "Peak memory usage should match");
      assertEquals(500000, stats.getCurrentMemoryUsage(), "Current memory usage should match");
      assertEquals(5, stats.getAllocationFailures(), "Allocation failures should match");
    }

    @Test
    @DisplayName("PanamaPoolStatistics getPoolWarmingTime should return Duration")
    void panamaPoolStatisticsGetPoolWarmingTimeShouldReturnDuration() {
      PanamaPoolStatistics stats =
          new PanamaPoolStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1000000000, 0);

      Duration warmingTime = stats.getPoolWarmingTime();
      assertNotNull(warmingTime, "Pool warming time should not be null");
      assertEquals(Duration.ofSeconds(1), warmingTime, "Pool warming time should be 1 second");
    }

    @Test
    @DisplayName("PanamaPoolStatistics getAverageAllocationTime should return Duration")
    void panamaPoolStatisticsGetAverageAllocationTimeShouldReturnDuration() {
      PanamaPoolStatistics stats =
          new PanamaPoolStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1000000);

      Duration avgTime = stats.getAverageAllocationTime();
      assertNotNull(avgTime, "Average allocation time should not be null");
      assertEquals(
          Duration.ofMillis(1), avgTime, "Average allocation time should be 1 millisecond");
    }

    @Test
    @DisplayName("PanamaPoolStatistics getReuseRatio should calculate correctly")
    void panamaPoolStatisticsGetReuseRatioShouldCalculateCorrectly() {
      // Formula: reused / (allocated + reused) = 100 / (100 + 100) = 0.5
      PanamaPoolStatistics stats =
          new PanamaPoolStatistics(100, 100, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

      double reuseRatio = stats.getReuseRatio();
      assertEquals(0.5, reuseRatio, 0.001, "Reuse ratio should be 0.5 (100/200)");
    }

    @Test
    @DisplayName("PanamaPoolStatistics getReuseRatio should handle zero allocations")
    void panamaPoolStatisticsGetReuseRatioShouldHandleZeroAllocations() {
      PanamaPoolStatistics stats = new PanamaPoolStatistics();

      double reuseRatio = stats.getReuseRatio();
      assertEquals(0.0, reuseRatio, "Reuse ratio should be 0 when no allocations");
    }

    @Test
    @DisplayName("PanamaPoolStatistics getMemoryUtilization should calculate correctly")
    void panamaPoolStatisticsGetMemoryUtilizationShouldCalculateCorrectly() {
      PanamaPoolStatistics stats =
          new PanamaPoolStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 1000000, 500000, 0, 0, 0);

      double utilization = stats.getMemoryUtilization();
      assertEquals(0.5, utilization, 0.001, "Memory utilization should be 0.5 (500000/1000000)");
    }

    @Test
    @DisplayName("PanamaPoolStatistics getMemoryUtilization should handle zero peak")
    void panamaPoolStatisticsGetMemoryUtilizationShouldHandleZeroPeak() {
      PanamaPoolStatistics stats = new PanamaPoolStatistics();

      double utilization = stats.getMemoryUtilization();
      assertEquals(0.0, utilization, "Memory utilization should be 0 when no peak usage");
    }

    @Test
    @DisplayName("PanamaPoolStatistics toString should contain relevant info")
    void panamaPoolStatisticsToStringShouldContainRelevantInfo() {
      PanamaPoolStatistics stats =
          new PanamaPoolStatistics(
              100, 50, 50, 200, 100, 300, 150, 400, 200, 1000000, 500000, 5, 0, 0);

      String str = stats.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("PanamaPoolStatistics"), "Should contain class name");
      assertTrue(str.contains("instancesAllocated"), "Should contain instancesAllocated");
      assertTrue(str.contains("instancesReused"), "Should contain instancesReused");
      assertTrue(str.contains("reuseRatio"), "Should contain reuseRatio");
      assertTrue(str.contains("memoryUtilization"), "Should contain memoryUtilization");
    }
  }

  // ========================================================================
  // PanamaPoolingAllocatorConfigBuilder Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaPoolingAllocatorConfigBuilder Tests")
  class PanamaPoolingAllocatorConfigBuilderTests {

    @Test
    @DisplayName("PanamaPoolingAllocatorConfigBuilder build should create config")
    void panamaPoolingAllocatorConfigBuilderBuildShouldCreateConfig() {
      PanamaPoolingAllocatorConfigBuilder builder = new PanamaPoolingAllocatorConfigBuilder();
      PoolingAllocatorConfig config = builder.build();

      assertNotNull(config, "Build should create config");
      assertTrue(
          config instanceof PanamaPoolingAllocatorConfig,
          "Build should create PanamaPoolingAllocatorConfig");
    }

    @Test
    @DisplayName("PanamaPoolingAllocatorConfigBuilder should support method chaining")
    void panamaPoolingAllocatorConfigBuilderShouldSupportMethodChaining() {
      PanamaPoolingAllocatorConfigBuilder builder = new PanamaPoolingAllocatorConfigBuilder();

      PoolingAllocatorConfig config =
          builder.instancePoolSize(1000).maxMemorySize(1024L * 1024 * 100).build();

      assertNotNull(config, "Chained build should create config");
      assertEquals(1000, config.getInstancePoolSize(), "Instance pool size should be set");
      assertEquals(1024L * 1024 * 100, config.getMaxMemorySize(), "Max memory size should be set");
    }
  }
}
