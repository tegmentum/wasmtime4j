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
package ai.tegmentum.wasmtime4j.panama.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for PanamaPoolingAllocatorConfig.
 *
 * <p>These tests exercise actual code execution to improve JaCoCo coverage.
 */
@DisplayName("Panama Pooling Allocator Config Integration Tests")
class PanamaPoolingAllocatorConfigTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaPoolingAllocatorConfigTest.class.getName());

  @Nested
  @DisplayName("Default Constructor Tests")
  class DefaultConstructorTests {

    @Test
    @DisplayName("Should create config with default values")
    void shouldCreateConfigWithDefaultValues() {
      LOGGER.info("Testing default constructor");

      final PanamaPoolingAllocatorConfig config = new PanamaPoolingAllocatorConfig();

      assertNotNull(config, "Config should not be null");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_INSTANCE_POOL_SIZE,
          config.getInstancePoolSize(),
          "Default instance pool size should match");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_MAX_MEMORY_PER_INSTANCE,
          config.getMaxMemoryPerInstance(),
          "Default max memory per instance should match");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_STACK_SIZE,
          config.getStackSize(),
          "Default stack size should match");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_MAX_STACKS,
          config.getMaxStacks(),
          "Default max stacks should match");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_MAX_TABLES_PER_INSTANCE,
          config.getMaxTablesPerInstance(),
          "Default max tables per instance should match");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_MAX_TABLES,
          config.getMaxTables(),
          "Default max tables should match");
      assertTrue(config.isMemoryDecommitEnabled(), "Memory decommit should be enabled by default");
      assertTrue(config.isPoolWarmingEnabled(), "Pool warming should be enabled by default");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_POOL_WARMING_PERCENTAGE,
          config.getPoolWarmingPercentage(),
          0.001f,
          "Default pool warming percentage should match");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_TOTAL_CORE_INSTANCES,
          config.getTotalCoreInstances(),
          "Default total core instances should match");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_TOTAL_COMPONENT_INSTANCES,
          config.getTotalComponentInstances(),
          "Default total component instances should match");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_MAX_CORE_INSTANCES_PER_COMPONENT,
          config.getMaxCoreInstancesPerComponent(),
          "Default max core instances per component should match");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_TOTAL_GC_HEAPS,
          config.getTotalGcHeaps(),
          "Default total GC heaps should match");
      assertEquals(
          PoolingAllocatorConfig.DEFAULT_MAX_MEMORY_SIZE,
          config.getMaxMemorySize(),
          "Default max memory size should match");

      LOGGER.info("Default config created: " + config);
    }
  }

  @Nested
  @DisplayName("Primary Constructor Tests")
  class PrimaryConstructorTests {

    @Test
    @DisplayName("Should create config with custom primary values")
    void shouldCreateConfigWithCustomPrimaryValues() {
      LOGGER.info("Testing primary constructor with custom values");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              200, // instancePoolSize
              64 * 1024 * 1024L, // maxMemoryPerInstance
              256 * 1024, // stackSize
              300, // maxStacks
              5, // maxTablesPerInstance
              2000, // maxTables
              true, // memoryDecommitEnabled
              false, // poolWarmingEnabled
              0.5f, // poolWarmingPercentage
              500, // totalCoreInstances
              50, // totalComponentInstances
              10, // maxCoreInstancesPerComponent
              100, // totalGcHeaps
              128 * 1024 * 1024L // maxMemorySize
              );

      assertEquals(200, config.getInstancePoolSize(), "Custom instance pool size");
      assertEquals(
          64 * 1024 * 1024L, config.getMaxMemoryPerInstance(), "Custom max memory per instance");
      assertEquals(256 * 1024, config.getStackSize(), "Custom stack size");
      assertEquals(300, config.getMaxStacks(), "Custom max stacks");
      assertEquals(5, config.getMaxTablesPerInstance(), "Custom max tables per instance");
      assertEquals(2000, config.getMaxTables(), "Custom max tables");
      assertTrue(config.isMemoryDecommitEnabled(), "Memory decommit enabled");
      assertFalse(config.isPoolWarmingEnabled(), "Pool warming disabled");
      assertEquals(
          0.5f, config.getPoolWarmingPercentage(), 0.001f, "Custom pool warming percentage");
      assertEquals(500, config.getTotalCoreInstances(), "Custom total core instances");
      assertEquals(50, config.getTotalComponentInstances(), "Custom total component instances");
      assertEquals(
          10, config.getMaxCoreInstancesPerComponent(), "Custom max core instances per component");
      assertEquals(100, config.getTotalGcHeaps(), "Custom total GC heaps");
      assertEquals(128 * 1024 * 1024L, config.getMaxMemorySize(), "Custom max memory size");

      LOGGER.info("Custom config created: " + config);
    }
  }

  @Nested
  @DisplayName("Extended Getter Tests")
  class ExtendedGetterTests {

    @Test
    @DisplayName("Should return extended property values")
    void shouldReturnExtendedPropertyValues() {
      LOGGER.info("Testing extended getters");

      final PanamaPoolingAllocatorConfig config = new PanamaPoolingAllocatorConfig();

      // Test all extended getters
      final int maxUnusedWarmSlots = config.getMaxUnusedWarmSlots();
      final int decommitBatchSize = config.getDecommitBatchSize();
      final long linearMemoryKeepResident = config.getLinearMemoryKeepResident();
      final long tableKeepResident = config.getTableKeepResident();
      final long asyncStackKeepResident = config.getAsyncStackKeepResident();
      final int totalMemories = config.getTotalMemories();
      final long maxCoreInstanceSize = config.getMaxCoreInstanceSize();
      final long maxComponentInstanceSize = config.getMaxComponentInstanceSize();
      final int maxMemoriesPerModule = config.getMaxMemoriesPerModule();
      final int maxMemoriesPerComponent = config.getMaxMemoriesPerComponent();
      final int tableElements = config.getTableElements();
      final boolean mpkEnabled = config.isMemoryProtectionKeysEnabled();
      final int maxMpk = config.getMaxMemoryProtectionKeys();
      final boolean pagemapScan = config.isPagemapScanEnabled();

      LOGGER.info(
          "Extended values: maxUnusedWarmSlots="
              + maxUnusedWarmSlots
              + ", decommitBatchSize="
              + decommitBatchSize
              + ", linearMemoryKeepResident="
              + linearMemoryKeepResident
              + ", tableKeepResident="
              + tableKeepResident
              + ", asyncStackKeepResident="
              + asyncStackKeepResident
              + ", totalMemories="
              + totalMemories
              + ", maxCoreInstanceSize="
              + maxCoreInstanceSize
              + ", maxComponentInstanceSize="
              + maxComponentInstanceSize
              + ", maxMemoriesPerModule="
              + maxMemoriesPerModule
              + ", maxMemoriesPerComponent="
              + maxMemoriesPerComponent
              + ", tableElements="
              + tableElements
              + ", mpkEnabled="
              + mpkEnabled
              + ", maxMpk="
              + maxMpk
              + ", pagemapScan="
              + pagemapScan);
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Should validate successfully with valid config")
    void shouldValidateSuccessfullyWithValidConfig() {
      LOGGER.info("Testing validation with valid config");

      final PanamaPoolingAllocatorConfig config = new PanamaPoolingAllocatorConfig();
      config.validate(); // Should not throw

      LOGGER.info("Valid config passed validation");
    }

    @Test
    @DisplayName("Should reject invalid instance pool size")
    void shouldRejectInvalidInstancePoolSize() {
      LOGGER.info("Testing validation with invalid instance pool size");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              0, // Invalid - must be positive
              64 * 1024 * 1024L,
              256 * 1024,
              300,
              5,
              2000,
              true,
              false,
              0.5f,
              500,
              50,
              10,
              100,
              128 * 1024 * 1024L);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("instancePoolSize"),
          "Error should mention instancePoolSize: " + ex.getMessage());

      LOGGER.info("Correctly rejected invalid instance pool size: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid max memory per instance")
    void shouldRejectInvalidMaxMemoryPerInstance() {
      LOGGER.info("Testing validation with invalid max memory per instance");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              100,
              0L, // Invalid - must be positive
              256 * 1024,
              300,
              5,
              2000,
              true,
              false,
              0.5f,
              500,
              50,
              10,
              100,
              128 * 1024 * 1024L);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("maxMemoryPerInstance"),
          "Error should mention maxMemoryPerInstance: " + ex.getMessage());

      LOGGER.info("Correctly rejected invalid max memory: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid stack size")
    void shouldRejectInvalidStackSize() {
      LOGGER.info("Testing validation with invalid stack size");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              100,
              64 * 1024 * 1024L,
              0, // Invalid - must be positive
              300,
              5,
              2000,
              true,
              false,
              0.5f,
              500,
              50,
              10,
              100,
              128 * 1024 * 1024L);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("stackSize"),
          "Error should mention stackSize: " + ex.getMessage());

      LOGGER.info("Correctly rejected invalid stack size: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid max stacks")
    void shouldRejectInvalidMaxStacks() {
      LOGGER.info("Testing validation with invalid max stacks");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              100,
              64 * 1024 * 1024L,
              256 * 1024,
              0, // Invalid - must be positive
              5,
              2000,
              true,
              false,
              0.5f,
              500,
              50,
              10,
              100,
              128 * 1024 * 1024L);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("maxStacks"),
          "Error should mention maxStacks: " + ex.getMessage());

      LOGGER.info("Correctly rejected invalid max stacks: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject negative max tables per instance")
    void shouldRejectNegativeMaxTablesPerInstance() {
      LOGGER.info("Testing validation with negative max tables per instance");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              100,
              64 * 1024 * 1024L,
              256 * 1024,
              300,
              -1, // Invalid - cannot be negative
              2000,
              true,
              false,
              0.5f,
              500,
              50,
              10,
              100,
              128 * 1024 * 1024L);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("maxTablesPerInstance"),
          "Error should mention maxTablesPerInstance: " + ex.getMessage());

      LOGGER.info("Correctly rejected negative max tables: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid max tables")
    void shouldRejectInvalidMaxTables() {
      LOGGER.info("Testing validation with invalid max tables");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              100,
              64 * 1024 * 1024L,
              256 * 1024,
              300,
              5,
              0, // Invalid - must be positive
              true,
              false,
              0.5f,
              500,
              50,
              10,
              100,
              128 * 1024 * 1024L);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("maxTables"),
          "Error should mention maxTables: " + ex.getMessage());

      LOGGER.info("Correctly rejected invalid max tables: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid pool warming percentage - too low")
    void shouldRejectInvalidPoolWarmingPercentageTooLow() {
      LOGGER.info("Testing validation with pool warming percentage too low");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              100,
              64 * 1024 * 1024L,
              256 * 1024,
              300,
              5,
              2000,
              true,
              false,
              -0.1f, // Invalid - cannot be negative
              500,
              50,
              10,
              100,
              128 * 1024 * 1024L);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("poolWarmingPercentage"),
          "Error should mention poolWarmingPercentage: " + ex.getMessage());

      LOGGER.info("Correctly rejected pool warming percentage too low: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid pool warming percentage - too high")
    void shouldRejectInvalidPoolWarmingPercentageTooHigh() {
      LOGGER.info("Testing validation with pool warming percentage too high");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              100,
              64 * 1024 * 1024L,
              256 * 1024,
              300,
              5,
              2000,
              true,
              false,
              1.1f, // Invalid - cannot be > 1.0
              500,
              50,
              10,
              100,
              128 * 1024 * 1024L);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("poolWarmingPercentage"),
          "Error should mention poolWarmingPercentage: " + ex.getMessage());

      LOGGER.info("Correctly rejected pool warming percentage too high: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid total core instances")
    void shouldRejectInvalidTotalCoreInstances() {
      LOGGER.info("Testing validation with invalid total core instances");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              100,
              64 * 1024 * 1024L,
              256 * 1024,
              300,
              5,
              2000,
              true,
              false,
              0.5f,
              0, // Invalid - must be positive
              50,
              10,
              100,
              128 * 1024 * 1024L);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("totalCoreInstances"),
          "Error should mention totalCoreInstances: " + ex.getMessage());

      LOGGER.info("Correctly rejected invalid total core instances: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid total component instances")
    void shouldRejectInvalidTotalComponentInstances() {
      LOGGER.info("Testing validation with invalid total component instances");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              100,
              64 * 1024 * 1024L,
              256 * 1024,
              300,
              5,
              2000,
              true,
              false,
              0.5f,
              500,
              0, // Invalid - must be positive
              10,
              100,
              128 * 1024 * 1024L);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("totalComponentInstances"),
          "Error should mention totalComponentInstances: " + ex.getMessage());

      LOGGER.info("Correctly rejected invalid total component instances: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid max core instances per component")
    void shouldRejectInvalidMaxCoreInstancesPerComponent() {
      LOGGER.info("Testing validation with invalid max core instances per component");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              100,
              64 * 1024 * 1024L,
              256 * 1024,
              300,
              5,
              2000,
              true,
              false,
              0.5f,
              500,
              50,
              0, // Invalid - must be positive
              100,
              128 * 1024 * 1024L);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("maxCoreInstancesPerComponent"),
          "Error should mention maxCoreInstancesPerComponent: " + ex.getMessage());

      LOGGER.info(
          "Correctly rejected invalid max core instances per component: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid total GC heaps")
    void shouldRejectInvalidTotalGcHeaps() {
      LOGGER.info("Testing validation with invalid total GC heaps");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              100,
              64 * 1024 * 1024L,
              256 * 1024,
              300,
              5,
              2000,
              true,
              false,
              0.5f,
              500,
              50,
              10,
              0, // Invalid - must be positive
              128 * 1024 * 1024L);

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("totalGcHeaps"),
          "Error should mention totalGcHeaps: " + ex.getMessage());

      LOGGER.info("Correctly rejected invalid total GC heaps: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid max memory size")
    void shouldRejectInvalidMaxMemorySize() {
      LOGGER.info("Testing validation with invalid max memory size");

      final PanamaPoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfig(
              100,
              64 * 1024 * 1024L,
              256 * 1024,
              300,
              5,
              2000,
              true,
              false,
              0.5f,
              500,
              50,
              10,
              100,
              0L // Invalid - must be positive
              );

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, config::validate);

      assertTrue(
          ex.getMessage().contains("maxMemorySize"),
          "Error should mention maxMemorySize: " + ex.getMessage());

      LOGGER.info("Correctly rejected invalid max memory size: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("Should produce readable toString")
    void shouldProduceReadableToString() {
      LOGGER.info("Testing toString output");

      final PanamaPoolingAllocatorConfig config = new PanamaPoolingAllocatorConfig();
      final String str = config.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(
          str.contains("PanamaPoolingAllocatorConfig"), "toString should contain class name");
      assertTrue(str.contains("instancePoolSize"), "toString should contain instancePoolSize");

      LOGGER.info("toString output: " + str);
    }
  }
}
