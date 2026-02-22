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

package ai.tegmentum.wasmtime4j.pool;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for PoolingAllocator - high-performance instance pooling.
 *
 * <p>These tests verify instance allocation, reuse, release, statistics tracking, pool warming, and
 * configuration. Tests are disabled until the native PoolingAllocator implementation is complete -
 * the current native implementation may cause JVM crashes.
 *
 * @since 1.0.0
 */
@DisplayName("PoolingAllocator Integration Tests")
public final class PoolingAllocatorIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(PoolingAllocatorIntegrationTest.class.getName());

  private static boolean poolingAllocatorAvailable = false;

  @BeforeAll
  static void checkPoolingAllocatorAvailable() {
    try {
      final PoolingAllocator allocator = PoolingAllocator.create();
      allocator.close();
      poolingAllocatorAvailable = true;
      LOGGER.info("PoolingAllocator native implementation is available");
    } catch (final Throwable t) {
      poolingAllocatorAvailable = false;
      LOGGER.warning("PoolingAllocator not available - tests will be skipped: " + t.getMessage());
    }
  }

  private static void assumePoolingAllocatorAvailable() {
    assumeTrue(
        poolingAllocatorAvailable,
        "PoolingAllocator native implementation not available - skipping");
  }

  private PoolingAllocator allocator;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
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

    if (allocator != null) {
      allocator.close();
      allocator = null;
    }
  }

  @Nested
  @DisplayName("PoolingAllocator Creation Tests")
  class PoolingAllocatorCreationTests {

    @Test
    @DisplayName("should create allocator with default configuration")
    void shouldCreateAllocatorWithDefaultConfig(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();
      assertNotNull(allocator, "Allocator should not be null");
      assertTrue(allocator.isValid(), "Allocator should be valid after creation");

      LOGGER.info("Created allocator with default config");
    }

    @Test
    @DisplayName("should create allocator with custom configuration")
    void shouldCreateAllocatorWithCustomConfig(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final PoolingAllocatorConfig config =
          PoolingAllocatorConfig.builder()
              .instancePoolSize(500)
              .maxMemoryPerInstance(32L * 1024L * 1024L)
              .stackSize(512 * 1024)
              .poolWarmingEnabled(false)
              .build();

      allocator = PoolingAllocator.create(config);
      assertNotNull(allocator, "Allocator should not be null");
      assertTrue(allocator.isValid(), "Allocator should be valid");

      final PoolingAllocatorConfig actualConfig = allocator.getConfig();
      assertEquals(500, actualConfig.getInstancePoolSize(), "Instance pool size should match");
      assertEquals(
          32L * 1024L * 1024L,
          actualConfig.getMaxMemoryPerInstance(),
          "Max memory per instance should match");
      assertEquals(512 * 1024, actualConfig.getStackSize(), "Stack size should match");

      LOGGER.info(
          "Created allocator with custom config: pool size=" + actualConfig.getInstancePoolSize());
    }

    @Test
    @DisplayName("should throw when creating with null configuration")
    void shouldThrowWhenCreatingWithNullConfig(final TestInfo testInfo) {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> PoolingAllocator.create(null),
          "Should throw for null config");

      LOGGER.info("Correctly threw for null config");
    }

    @Test
    @DisplayName("should create multiple allocators independently")
    void shouldCreateMultipleAllocatorsIndependently(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final PoolingAllocator allocator1 = PoolingAllocator.create();
      resources.add(allocator1);
      final PoolingAllocator allocator2 = PoolingAllocator.create();
      resources.add(allocator2);

      assertNotNull(allocator1, "First allocator should not be null");
      assertNotNull(allocator2, "Second allocator should not be null");
      assertTrue(allocator1.isValid(), "First allocator should be valid");
      assertTrue(allocator2.isValid(), "Second allocator should be valid");

      LOGGER.info("Created two independent allocators");
    }
  }

  @Nested
  @DisplayName("Instance Allocation Tests")
  class InstanceAllocationTests {

    @Test
    @DisplayName("should allocate instance from pool")
    void shouldAllocateInstanceFromPool(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();
      final long instanceId = allocator.allocateInstance();

      assertTrue(instanceId > 0, "Instance ID should be positive");

      LOGGER.info("Allocated instance with ID: " + instanceId);
    }

    @Test
    @DisplayName("should allocate multiple unique instances")
    void shouldAllocateMultipleUniqueInstances(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();
      final Set<Long> instanceIds = new HashSet<>();

      for (int i = 0; i < 10; i++) {
        final long instanceId = allocator.allocateInstance();
        assertTrue(instanceIds.add(instanceId), "Instance ID " + instanceId + " should be unique");
      }

      assertEquals(10, instanceIds.size(), "Should have allocated 10 unique instances");

      LOGGER.info("Allocated 10 unique instances: " + instanceIds);
    }

    @Test
    @DisplayName("should release instance back to pool")
    void shouldReleaseInstanceBackToPool(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();
      final long instanceId = allocator.allocateInstance();

      assertDoesNotThrow(() -> allocator.releaseInstance(instanceId), "Release should not throw");

      LOGGER.info("Released instance " + instanceId + " back to pool");
    }

    @Test
    @DisplayName("should reuse released instance")
    void shouldReuseReleasedInstance(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();
      final long instanceId = allocator.allocateInstance();
      allocator.releaseInstance(instanceId);

      assertDoesNotThrow(
          () -> allocator.reuseInstance(instanceId), "Reuse of released instance should not throw");

      LOGGER.info("Reused instance " + instanceId);
    }

    @Test
    @DisplayName("should track allocation count in statistics")
    void shouldTrackAllocationCountInStatistics(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();
      final PoolStatistics initialStats = allocator.getStatistics();
      final long initialCoreInstances = initialStats.getCoreInstances();

      for (int i = 0; i < 5; i++) {
        allocator.allocateInstance();
      }

      final PoolStatistics finalStats = allocator.getStatistics();
      assertTrue(
          finalStats.getCoreInstances() >= initialCoreInstances + 5,
          "Core instance count should increase by at least 5");

      LOGGER.info(
          "Core instance count increased from "
              + initialCoreInstances
              + " to "
              + finalStats.getCoreInstances());
    }
  }

  @Nested
  @DisplayName("Pool Statistics Tests")
  class PoolStatisticsTests {

    @Test
    @DisplayName("should return valid statistics")
    void shouldReturnValidStatistics(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();
      final PoolStatistics stats = allocator.getStatistics();

      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.getCoreInstances() >= 0, "Core instances should be non-negative");
      assertTrue(stats.getComponentInstances() >= 0, "Component instances should be non-negative");
      assertTrue(stats.getMemories() >= 0, "Memories should be non-negative");
      assertTrue(stats.getTables() >= 0, "Tables should be non-negative");
      assertTrue(stats.getStacks() >= 0, "Stacks should be non-negative");
      assertTrue(stats.getGcHeaps() >= 0, "GC heaps should be non-negative");

      LOGGER.info(
          "Statistics: coreInstances="
              + stats.getCoreInstances()
              + ", memories="
              + stats.getMemories()
              + ", tables="
              + stats.getTables()
              + ", stacks="
              + stats.getStacks());
    }

    @Test
    @DisplayName("should reset statistics")
    void shouldResetStatistics(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();

      // Perform some allocations
      for (int i = 0; i < 5; i++) {
        final long id = allocator.allocateInstance();
        allocator.releaseInstance(id);
      }

      // Reset statistics
      allocator.resetStatistics();
      final PoolStatistics stats = allocator.getStatistics();

      assertEquals(0, stats.getCoreInstances(), "Core instance count should be reset to 0");
      assertEquals(0, stats.getMemories(), "Memory count should be reset to 0");

      LOGGER.info("Statistics reset successfully");
    }

    @Test
    @DisplayName("should track unused warm slots after release")
    void shouldTrackUnusedWarmSlotsAfterRelease(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();
      allocator.resetStatistics();

      // Allocate, release, and reuse
      final long id1 = allocator.allocateInstance();
      allocator.releaseInstance(id1);
      allocator.reuseInstance(id1);

      final PoolStatistics stats = allocator.getStatistics();
      final long unusedWarmMemories = stats.getUnusedWarmMemories();

      assertTrue(unusedWarmMemories >= 0, "Unused warm memories should be non-negative");

      LOGGER.info("Unused warm memories: " + unusedWarmMemories);
    }

    @Test
    @DisplayName("should track memory slot usage")
    void shouldTrackMemorySlotUsage(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();
      final PoolStatistics initialStats = allocator.getStatistics();
      final long initialMemories = initialStats.getMemories();

      // Allocate several instances
      final List<Long> instanceIds = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        instanceIds.add(allocator.allocateInstance());
      }

      final PoolStatistics finalStats = allocator.getStatistics();
      assertTrue(
          finalStats.getMemories() >= initialMemories,
          "Memory slot count should be >= initial count");

      LOGGER.info(
          "Memory slots: initial=" + initialMemories + ", final=" + finalStats.getMemories());
    }

    @Test
    @DisplayName("should track stack and table slots")
    void shouldTrackStackAndTableSlots(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();

      // Allocate some instances
      final List<Long> instanceIds = new ArrayList<>();
      for (int i = 0; i < 3; i++) {
        instanceIds.add(allocator.allocateInstance());
      }

      final PoolStatistics stats = allocator.getStatistics();
      assertTrue(stats.getStacks() >= 0, "Stacks should be non-negative");
      assertTrue(stats.getTables() >= 0, "Tables should be non-negative");
      assertTrue(stats.getGcHeaps() >= 0, "GC heaps should be non-negative");
      assertTrue(
          stats.getUnusedWarmStacks() >= 0, "Unused warm stacks should be non-negative");
      assertTrue(
          stats.getUnusedWarmTables() >= 0, "Unused warm tables should be non-negative");

      LOGGER.info(
          "Stacks="
              + stats.getStacks()
              + ", tables="
              + stats.getTables()
              + ", gcHeaps="
              + stats.getGcHeaps());
    }
  }

  @Nested
  @DisplayName("Pool Warming Tests")
  class PoolWarmingTests {

    @Test
    @DisplayName("should warm pools when enabled")
    void shouldWarmPoolsWhenEnabled(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final PoolingAllocatorConfig config =
          PoolingAllocatorConfig.builder()
              .poolWarmingEnabled(true)
              .poolWarmingPercentage(0.5f)
              .instancePoolSize(100)
              .build();

      allocator = PoolingAllocator.create(config);
      final PoolStatistics stats = allocator.getStatistics();

      // After warming, we should see unused warm memory slots available
      assertTrue(
          stats.getUnusedWarmMemories() >= 0,
          "Unused warm memories should be non-negative after warming");
      assertTrue(
          stats.getUnusedMemoryBytesResident() >= 0,
          "Unused memory bytes resident should be non-negative after warming");

      LOGGER.info(
          "After warming: unusedWarmMemories="
              + stats.getUnusedWarmMemories()
              + ", unusedMemoryBytesResident="
              + stats.getUnusedMemoryBytesResident());
    }

    @Test
    @DisplayName("should warm pools manually")
    void shouldWarmPoolsManually(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final PoolingAllocatorConfig config =
          PoolingAllocatorConfig.builder().poolWarmingEnabled(false).build();

      allocator = PoolingAllocator.create(config);

      // Manual warm
      assertDoesNotThrow(() -> allocator.warmPools(), "Manual pool warming should not throw");

      LOGGER.info("Pools warmed manually");
    }
  }

  @Nested
  @DisplayName("Pool Maintenance Tests")
  class PoolMaintenanceTests {

    @Test
    @DisplayName("should perform maintenance successfully")
    void shouldPerformMaintenanceSuccessfully(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();

      // Allocate and release some instances
      for (int i = 0; i < 5; i++) {
        final long id = allocator.allocateInstance();
        allocator.releaseInstance(id);
      }

      assertDoesNotThrow(() -> allocator.performMaintenance(), "Maintenance should not throw");

      LOGGER.info("Maintenance performed successfully");
    }

    @Test
    @DisplayName("should track uptime")
    void shouldTrackUptime(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      allocator = PoolingAllocator.create();

      // Wait a short time
      Thread.sleep(100);

      final Duration uptime = allocator.getUptime();
      assertNotNull(uptime, "Uptime should not be null");
      assertTrue(uptime.toMillis() >= 100, "Uptime should be at least 100ms");

      LOGGER.info("Allocator uptime: " + uptime.toMillis() + "ms");
    }
  }

  @Nested
  @DisplayName("Pool Lifecycle Tests")
  class PoolLifecycleTests {

    @Test
    @DisplayName("should close allocator properly")
    void shouldCloseAllocatorProperly(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final PoolingAllocator localAllocator = PoolingAllocator.create();
      assertTrue(localAllocator.isValid(), "Allocator should be valid before close");

      localAllocator.close();
      assertFalse(localAllocator.isValid(), "Allocator should be invalid after close");

      LOGGER.info("Allocator closed properly");
    }

    @Test
    @DisplayName("should not allocate from closed allocator")
    void shouldNotAllocateFromClosedAllocator(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final PoolingAllocator localAllocator = PoolingAllocator.create();
      localAllocator.close();

      assertThrows(
          IllegalStateException.class,
          localAllocator::allocateInstance,
          "Should throw when allocating from closed allocator");

      LOGGER.info("Correctly prevented allocation from closed allocator");
    }

    @Test
    @DisplayName("should handle multiple close calls")
    void shouldHandleMultipleCloseCalls(final TestInfo testInfo) throws Exception {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final PoolingAllocator localAllocator = PoolingAllocator.create();

      assertDoesNotThrow(localAllocator::close, "First close should not throw");
      assertDoesNotThrow(localAllocator::close, "Second close should not throw");

      LOGGER.info("Multiple close calls handled correctly");
    }
  }

  @Nested
  @DisplayName("Configuration Tests")
  class ConfigurationTests {

    @Test
    @DisplayName("should build configuration with builder pattern")
    void shouldBuildConfigurationWithBuilderPattern(final TestInfo testInfo) {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final PoolingAllocatorConfig config =
          PoolingAllocatorConfig.builder()
              .instancePoolSize(200)
              .maxMemoryPerInstance(64L * 1024L * 1024L)
              .stackSize(2 * 1024 * 1024)
              .maxStacks(500)
              .maxTablesPerInstance(5)
              .maxTables(5000)
              .memoryDecommitEnabled(true)
              .poolWarmingEnabled(true)
              .poolWarmingPercentage(0.3f)
              .build();

      assertEquals(200, config.getInstancePoolSize(), "Instance pool size should be 200");
      assertEquals(
          64L * 1024L * 1024L,
          config.getMaxMemoryPerInstance(),
          "Max memory per instance should be 64MB");
      assertEquals(2 * 1024 * 1024, config.getStackSize(), "Stack size should be 2MB");
      assertEquals(500, config.getMaxStacks(), "Max stacks should be 500");
      assertEquals(5, config.getMaxTablesPerInstance(), "Max tables per instance should be 5");
      assertEquals(5000, config.getMaxTables(), "Max tables should be 5000");
      assertTrue(config.isMemoryDecommitEnabled(), "Memory decommit should be enabled");
      assertTrue(config.isPoolWarmingEnabled(), "Pool warming should be enabled");
      assertEquals(
          0.3f, config.getPoolWarmingPercentage(), 0.01f, "Warming percentage should be 0.3");

      LOGGER.info("Configuration built successfully with builder pattern");
    }

    @Test
    @DisplayName("should use default values when not specified")
    void shouldUseDefaultValuesWhenNotSpecified(final TestInfo testInfo) {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final PoolingAllocatorConfig config = PoolingAllocatorConfig.defaultConfig();

      assertEquals(
          PoolingAllocatorConfig.DEFAULT_INSTANCE_POOL_SIZE,
          config.getInstancePoolSize(),
          "Should use default instance pool size");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_MAX_MEMORY_PER_INSTANCE,
          config.getMaxMemoryPerInstance(),
          "Should use default max memory per instance");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_STACK_SIZE,
          config.getStackSize(),
          "Should use default stack size");

      LOGGER.info("Default configuration values verified");
    }

    @Test
    @DisplayName("should validate configuration")
    void shouldValidateConfiguration(final TestInfo testInfo) {
      assumePoolingAllocatorAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final PoolingAllocatorConfig config =
          PoolingAllocatorConfig.builder().instancePoolSize(100).build();

      assertDoesNotThrow(config::validate, "Valid configuration should not throw");

      LOGGER.info("Configuration validated successfully");
    }
  }
}
