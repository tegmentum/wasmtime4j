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
 * Integration tests for PanamaPoolingAllocatorConfigBuilder.
 *
 * <p>These tests exercise actual code execution to improve JaCoCo coverage.
 */
@DisplayName("Panama Pooling Allocator Config Builder Integration Tests")
public class PanamaPoolingAllocatorConfigBuilderIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaPoolingAllocatorConfigBuilderIntegrationTest.class.getName());

  @Nested
  @DisplayName("Default Builder Tests")
  class DefaultBuilderTests {

    @Test
    @DisplayName("Should create builder with default values")
    void shouldCreateBuilderWithDefaultValues() {
      LOGGER.info("Testing default builder creation");

      final PanamaPoolingAllocatorConfigBuilder builder = new PanamaPoolingAllocatorConfigBuilder();
      assertNotNull(builder, "Builder should not be null");

      final PoolingAllocatorConfig config = builder.build();
      assertNotNull(config, "Built config should not be null");

      assertEquals(
          PoolingAllocatorConfig.DEFAULT_INSTANCE_POOL_SIZE,
          config.getInstancePoolSize(),
          "Should have default instance pool size");

      LOGGER.info("Default builder created successfully");
    }
  }

  @Nested
  @DisplayName("Instance Pool Size Tests")
  class InstancePoolSizeTests {

    @Test
    @DisplayName("Should set instance pool size")
    void shouldSetInstancePoolSize() {
      LOGGER.info("Testing instancePoolSize setter");

      final int customSize = 500;
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().instancePoolSize(customSize).build();

      assertEquals(customSize, config.getInstancePoolSize(), "Instance pool size should match");

      LOGGER.info("Instance pool size set to: " + config.getInstancePoolSize());
    }

    @Test
    @DisplayName("Should reject zero instance pool size")
    void shouldRejectZeroInstancePoolSize() {
      LOGGER.info("Testing zero instance pool size rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().instancePoolSize(0));

      LOGGER.info("Correctly rejected zero instance pool size");
    }

    @Test
    @DisplayName("Should reject negative instance pool size")
    void shouldRejectNegativeInstancePoolSize() {
      LOGGER.info("Testing negative instance pool size rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().instancePoolSize(-1));

      LOGGER.info("Correctly rejected negative instance pool size");
    }
  }

  @Nested
  @DisplayName("Max Memory Per Instance Tests")
  class MaxMemoryPerInstanceTests {

    @Test
    @DisplayName("Should set max memory per instance")
    void shouldSetMaxMemoryPerInstance() {
      LOGGER.info("Testing maxMemoryPerInstance setter");

      final long customMemory = 128 * 1024 * 1024L;
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().maxMemoryPerInstance(customMemory).build();

      assertEquals(
          customMemory, config.getMaxMemoryPerInstance(), "Max memory per instance should match");

      LOGGER.info("Max memory per instance set to: " + config.getMaxMemoryPerInstance());
    }

    @Test
    @DisplayName("Should reject zero max memory per instance")
    void shouldRejectZeroMaxMemoryPerInstance() {
      LOGGER.info("Testing zero max memory per instance rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().maxMemoryPerInstance(0L));

      LOGGER.info("Correctly rejected zero max memory per instance");
    }
  }

  @Nested
  @DisplayName("Stack Size Tests")
  class StackSizeTests {

    @Test
    @DisplayName("Should set stack size")
    void shouldSetStackSize() {
      LOGGER.info("Testing stackSize setter");

      final int customStack = 512 * 1024;
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().stackSize(customStack).build();

      assertEquals(customStack, config.getStackSize(), "Stack size should match");

      LOGGER.info("Stack size set to: " + config.getStackSize());
    }

    @Test
    @DisplayName("Should reject invalid stack size")
    void shouldRejectInvalidStackSize() {
      LOGGER.info("Testing invalid stack size rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().stackSize(0));

      LOGGER.info("Correctly rejected invalid stack size");
    }
  }

  @Nested
  @DisplayName("Max Stacks Tests")
  class MaxStacksTests {

    @Test
    @DisplayName("Should set max stacks")
    void shouldSetMaxStacks() {
      LOGGER.info("Testing maxStacks setter");

      final int customMax = 500;
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().maxStacks(customMax).build();

      assertEquals(customMax, config.getMaxStacks(), "Max stacks should match");

      LOGGER.info("Max stacks set to: " + config.getMaxStacks());
    }

    @Test
    @DisplayName("Should reject invalid max stacks")
    void shouldRejectInvalidMaxStacks() {
      LOGGER.info("Testing invalid max stacks rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().maxStacks(0));

      LOGGER.info("Correctly rejected invalid max stacks");
    }
  }

  @Nested
  @DisplayName("Max Tables Per Instance Tests")
  class MaxTablesPerInstanceTests {

    @Test
    @DisplayName("Should set max tables per instance")
    void shouldSetMaxTablesPerInstance() {
      LOGGER.info("Testing maxTablesPerInstance setter");

      final int customMax = 10;
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().maxTablesPerInstance(customMax).build();

      assertEquals(
          customMax, config.getMaxTablesPerInstance(), "Max tables per instance should match");

      LOGGER.info("Max tables per instance set to: " + config.getMaxTablesPerInstance());
    }

    @Test
    @DisplayName("Should accept zero max tables per instance")
    void shouldAcceptZeroMaxTablesPerInstance() {
      LOGGER.info("Testing zero max tables per instance");

      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().maxTablesPerInstance(0).build();

      assertEquals(
          0, config.getMaxTablesPerInstance(), "Zero max tables per instance should be accepted");

      LOGGER.info("Zero max tables per instance accepted");
    }

    @Test
    @DisplayName("Should reject negative max tables per instance")
    void shouldRejectNegativeMaxTablesPerInstance() {
      LOGGER.info("Testing negative max tables per instance rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().maxTablesPerInstance(-1));

      LOGGER.info("Correctly rejected negative max tables per instance");
    }
  }

  @Nested
  @DisplayName("Max Tables Tests")
  class MaxTablesTests {

    @Test
    @DisplayName("Should set max tables")
    void shouldSetMaxTables() {
      LOGGER.info("Testing maxTables setter");

      final int customMax = 5000;
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().maxTables(customMax).build();

      assertEquals(customMax, config.getMaxTables(), "Max tables should match");

      LOGGER.info("Max tables set to: " + config.getMaxTables());
    }

    @Test
    @DisplayName("Should reject invalid max tables")
    void shouldRejectInvalidMaxTables() {
      LOGGER.info("Testing invalid max tables rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().maxTables(0));

      LOGGER.info("Correctly rejected invalid max tables");
    }
  }

  @Nested
  @DisplayName("Memory Decommit Tests")
  class MemoryDecommitTests {

    @Test
    @DisplayName("Should enable memory decommit")
    void shouldEnableMemoryDecommit() {
      LOGGER.info("Testing memoryDecommitEnabled setter - enable");

      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().memoryDecommitEnabled(true).build();

      assertTrue(config.isMemoryDecommitEnabled(), "Memory decommit should be enabled");

      LOGGER.info("Memory decommit enabled: " + config.isMemoryDecommitEnabled());
    }

    @Test
    @DisplayName("Should disable memory decommit")
    void shouldDisableMemoryDecommit() {
      LOGGER.info("Testing memoryDecommitEnabled setter - disable");

      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().memoryDecommitEnabled(false).build();

      assertFalse(config.isMemoryDecommitEnabled(), "Memory decommit should be disabled");

      LOGGER.info("Memory decommit disabled");
    }
  }

  @Nested
  @DisplayName("Pool Warming Tests")
  class PoolWarmingTests {

    @Test
    @DisplayName("Should enable pool warming")
    void shouldEnablePoolWarming() {
      LOGGER.info("Testing poolWarmingEnabled setter - enable");

      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().poolWarmingEnabled(true).build();

      assertTrue(config.isPoolWarmingEnabled(), "Pool warming should be enabled");

      LOGGER.info("Pool warming enabled: " + config.isPoolWarmingEnabled());
    }

    @Test
    @DisplayName("Should disable pool warming")
    void shouldDisablePoolWarming() {
      LOGGER.info("Testing poolWarmingEnabled setter - disable");

      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().poolWarmingEnabled(false).build();

      assertFalse(config.isPoolWarmingEnabled(), "Pool warming should be disabled");

      LOGGER.info("Pool warming disabled");
    }
  }

  @Nested
  @DisplayName("Pool Warming Percentage Tests")
  class PoolWarmingPercentageTests {

    @Test
    @DisplayName("Should set pool warming percentage")
    void shouldSetPoolWarmingPercentage() {
      LOGGER.info("Testing poolWarmingPercentage setter");

      final float customPercentage = 0.75f;
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().poolWarmingPercentage(customPercentage).build();

      assertEquals(
          customPercentage,
          config.getPoolWarmingPercentage(),
          0.001f,
          "Pool warming percentage should match");

      LOGGER.info("Pool warming percentage set to: " + config.getPoolWarmingPercentage());
    }

    @Test
    @DisplayName("Should accept boundary percentage values")
    void shouldAcceptBoundaryPercentageValues() {
      LOGGER.info("Testing boundary pool warming percentage values");

      // Test 0.0
      final PoolingAllocatorConfig config0 =
          new PanamaPoolingAllocatorConfigBuilder().poolWarmingPercentage(0.0f).build();
      assertEquals(0.0f, config0.getPoolWarmingPercentage(), 0.001f, "0.0 should be accepted");

      // Test 1.0
      final PoolingAllocatorConfig config1 =
          new PanamaPoolingAllocatorConfigBuilder().poolWarmingPercentage(1.0f).build();
      assertEquals(1.0f, config1.getPoolWarmingPercentage(), 0.001f, "1.0 should be accepted");

      LOGGER.info("Boundary percentage values accepted");
    }

    @Test
    @DisplayName("Should reject negative pool warming percentage")
    void shouldRejectNegativePoolWarmingPercentage() {
      LOGGER.info("Testing negative pool warming percentage rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().poolWarmingPercentage(-0.1f));

      LOGGER.info("Correctly rejected negative pool warming percentage");
    }

    @Test
    @DisplayName("Should reject pool warming percentage greater than 1.0")
    void shouldRejectPoolWarmingPercentageGreaterThanOne() {
      LOGGER.info("Testing pool warming percentage > 1.0 rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().poolWarmingPercentage(1.1f));

      LOGGER.info("Correctly rejected pool warming percentage > 1.0");
    }
  }

  @Nested
  @DisplayName("Total Core Instances Tests")
  class TotalCoreInstancesTests {

    @Test
    @DisplayName("Should set total core instances")
    void shouldSetTotalCoreInstances() {
      LOGGER.info("Testing totalCoreInstances setter");

      final int customCount = 1000;
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().totalCoreInstances(customCount).build();

      assertEquals(
          customCount, config.getTotalCoreInstances(), "Total core instances should match");

      LOGGER.info("Total core instances set to: " + config.getTotalCoreInstances());
    }

    @Test
    @DisplayName("Should reject invalid total core instances")
    void shouldRejectInvalidTotalCoreInstances() {
      LOGGER.info("Testing invalid total core instances rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().totalCoreInstances(0));

      LOGGER.info("Correctly rejected invalid total core instances");
    }
  }

  @Nested
  @DisplayName("Total Component Instances Tests")
  class TotalComponentInstancesTests {

    @Test
    @DisplayName("Should set total component instances")
    void shouldSetTotalComponentInstances() {
      LOGGER.info("Testing totalComponentInstances setter");

      final int customCount = 200;
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().totalComponentInstances(customCount).build();

      assertEquals(
          customCount,
          config.getTotalComponentInstances(),
          "Total component instances should match");

      LOGGER.info("Total component instances set to: " + config.getTotalComponentInstances());
    }

    @Test
    @DisplayName("Should reject invalid total component instances")
    void shouldRejectInvalidTotalComponentInstances() {
      LOGGER.info("Testing invalid total component instances rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().totalComponentInstances(0));

      LOGGER.info("Correctly rejected invalid total component instances");
    }
  }

  @Nested
  @DisplayName("Max Core Instances Per Component Tests")
  class MaxCoreInstancesPerComponentTests {

    @Test
    @DisplayName("Should set max core instances per component")
    void shouldSetMaxCoreInstancesPerComponent() {
      LOGGER.info("Testing maxCoreInstancesPerComponent setter");

      final int customMax = 50;
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().maxCoreInstancesPerComponent(customMax).build();

      assertEquals(
          customMax,
          config.getMaxCoreInstancesPerComponent(),
          "Max core instances per component should match");

      LOGGER.info(
          "Max core instances per component set to: " + config.getMaxCoreInstancesPerComponent());
    }

    @Test
    @DisplayName("Should reject invalid max core instances per component")
    void shouldRejectInvalidMaxCoreInstancesPerComponent() {
      LOGGER.info("Testing invalid max core instances per component rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().maxCoreInstancesPerComponent(0));

      LOGGER.info("Correctly rejected invalid max core instances per component");
    }
  }

  @Nested
  @DisplayName("Total GC Heaps Tests")
  class TotalGcHeapsTests {

    @Test
    @DisplayName("Should set total GC heaps")
    void shouldSetTotalGcHeaps() {
      LOGGER.info("Testing totalGcHeaps setter");

      final int customCount = 200;
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().totalGcHeaps(customCount).build();

      assertEquals(customCount, config.getTotalGcHeaps(), "Total GC heaps should match");

      LOGGER.info("Total GC heaps set to: " + config.getTotalGcHeaps());
    }

    @Test
    @DisplayName("Should reject invalid total GC heaps")
    void shouldRejectInvalidTotalGcHeaps() {
      LOGGER.info("Testing invalid total GC heaps rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().totalGcHeaps(0));

      LOGGER.info("Correctly rejected invalid total GC heaps");
    }
  }

  @Nested
  @DisplayName("Max Memory Size Tests")
  class MaxMemorySizeTests {

    @Test
    @DisplayName("Should set max memory size")
    void shouldSetMaxMemorySize() {
      LOGGER.info("Testing maxMemorySize setter");

      final long customSize = 256 * 1024 * 1024L;
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder().maxMemorySize(customSize).build();

      assertEquals(customSize, config.getMaxMemorySize(), "Max memory size should match");

      LOGGER.info("Max memory size set to: " + config.getMaxMemorySize());
    }

    @Test
    @DisplayName("Should reject invalid max memory size")
    void shouldRejectInvalidMaxMemorySize() {
      LOGGER.info("Testing invalid max memory size rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaPoolingAllocatorConfigBuilder().maxMemorySize(0L));

      LOGGER.info("Correctly rejected invalid max memory size");
    }
  }

  @Nested
  @DisplayName("Extended Option Tests")
  class ExtendedOptionTests {

    @Test
    @DisplayName("Should allow setting extended options via builder API")
    void shouldAllowSettingExtendedOptionsViaBuilderApi() {
      LOGGER.info("Testing extended option setter methods exist and can be called");

      // Note: The build() method uses the primary constructor which doesn't
      // pass extended options through. This test verifies the builder API
      // accepts these calls without throwing exceptions.

      final PanamaPoolingAllocatorConfigBuilder builder = new PanamaPoolingAllocatorConfigBuilder();

      // These setters should not throw
      builder.maxUnusedWarmSlots(10);
      builder.decommitBatchSize(5);
      builder.linearMemoryKeepResident(4096L);
      builder.tableKeepResident(2048L);
      builder.asyncStackKeepResident(1024L);
      builder.totalMemories(500);
      builder.maxCoreInstanceSize(2 * 1024 * 1024L);
      builder.maxComponentInstanceSize(4 * 1024 * 1024L);
      builder.maxMemoriesPerModule(2);
      builder.maxMemoriesPerComponent(4);
      builder.tableElements(20000);
      builder.memoryProtectionKeysEnabled(true);
      builder.maxMemoryProtectionKeys(16);
      builder.pagemapScanEnabled(true);

      final PoolingAllocatorConfig config = builder.build();
      assertNotNull(config, "Config should be built successfully");

      // The build() method uses the primary constructor which uses defaults
      // for extended options, so we just verify the primary options were set
      LOGGER.info("Extended option setter methods verified");
    }

    @Test
    @DisplayName("Should return builder for method chaining")
    void shouldReturnBuilderForMethodChaining() {
      LOGGER.info("Testing method chaining for extended options");

      // All setters should return the builder for chaining
      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder()
              .maxUnusedWarmSlots(10)
              .decommitBatchSize(5)
              .linearMemoryKeepResident(4096L)
              .tableKeepResident(2048L)
              .asyncStackKeepResident(1024L)
              .totalMemories(500)
              .maxCoreInstanceSize(2 * 1024 * 1024L)
              .maxComponentInstanceSize(4 * 1024 * 1024L)
              .maxMemoriesPerModule(2)
              .maxMemoriesPerComponent(4)
              .tableElements(20000)
              .memoryProtectionKeysEnabled(true)
              .maxMemoryProtectionKeys(16)
              .pagemapScanEnabled(true)
              .instancePoolSize(100) // This one IS used in build()
              .build();

      assertEquals(100, config.getInstancePoolSize(), "Primary option should be set");

      LOGGER.info("Method chaining verified");
    }
  }

  @Nested
  @DisplayName("Full Build Tests")
  class FullBuildTests {

    @Test
    @DisplayName("Should build fully customized config")
    void shouldBuildFullyCustomizedConfig() {
      LOGGER.info("Testing fully customized config build");

      final PoolingAllocatorConfig config =
          new PanamaPoolingAllocatorConfigBuilder()
              .instancePoolSize(200)
              .maxMemoryPerInstance(128 * 1024 * 1024L)
              .stackSize(512 * 1024)
              .maxStacks(400)
              .maxTablesPerInstance(8)
              .maxTables(3000)
              .memoryDecommitEnabled(false)
              .poolWarmingEnabled(true)
              .poolWarmingPercentage(0.6f)
              .totalCoreInstances(800)
              .totalComponentInstances(80)
              .maxCoreInstancesPerComponent(20)
              .totalGcHeaps(150)
              .maxMemorySize(256 * 1024 * 1024L)
              .build();

      assertNotNull(config, "Built config should not be null");
      assertEquals(200, config.getInstancePoolSize(), "Instance pool size should match");
      assertEquals(
          128 * 1024 * 1024L,
          config.getMaxMemoryPerInstance(),
          "Max memory per instance should match");
      assertEquals(512 * 1024, config.getStackSize(), "Stack size should match");
      assertEquals(400, config.getMaxStacks(), "Max stacks should match");
      assertEquals(8, config.getMaxTablesPerInstance(), "Max tables per instance should match");
      assertEquals(3000, config.getMaxTables(), "Max tables should match");
      assertFalse(config.isMemoryDecommitEnabled(), "Memory decommit should be disabled");
      assertTrue(config.isPoolWarmingEnabled(), "Pool warming should be enabled");
      assertEquals(
          0.6f, config.getPoolWarmingPercentage(), 0.001f, "Pool warming percentage should match");
      assertEquals(800, config.getTotalCoreInstances(), "Total core instances should match");
      assertEquals(
          80, config.getTotalComponentInstances(), "Total component instances should match");
      assertEquals(
          20,
          config.getMaxCoreInstancesPerComponent(),
          "Max core instances per component should match");
      assertEquals(150, config.getTotalGcHeaps(), "Total GC heaps should match");
      assertEquals(256 * 1024 * 1024L, config.getMaxMemorySize(), "Max memory size should match");

      LOGGER.info("Fully customized config built successfully");
    }
  }
}
