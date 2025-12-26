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

package ai.tegmentum.wasmtime4j.jni.pool;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link JniPoolingAllocatorConfig}.
 */
@DisplayName("JniPoolingAllocatorConfig Tests")
class JniPoolingAllocatorConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniPoolingAllocatorConfig should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniPoolingAllocatorConfig.class.getModifiers()),
          "JniPoolingAllocatorConfig should be final");
    }

    @Test
    @DisplayName("JniPoolingAllocatorConfig should implement PoolingAllocatorConfig")
    void shouldImplementPoolingAllocatorConfig() {
      assertTrue(
          PoolingAllocatorConfig.class.isAssignableFrom(JniPoolingAllocatorConfig.class),
          "JniPoolingAllocatorConfig should implement PoolingAllocatorConfig");
    }
  }

  @Nested
  @DisplayName("Default Constructor Tests")
  class DefaultConstructorTests {

    @Test
    @DisplayName("Default constructor should use default values")
    void defaultConstructorShouldUseDefaultValues() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig();

      assertEquals(PoolingAllocatorConfig.DEFAULT_INSTANCE_POOL_SIZE, config.getInstancePoolSize(),
          "Should use default instance pool size");
      assertEquals(PoolingAllocatorConfig.DEFAULT_MAX_MEMORY_PER_INSTANCE,
          config.getMaxMemoryPerInstance(),
          "Should use default max memory per instance");
      assertEquals(PoolingAllocatorConfig.DEFAULT_STACK_SIZE, config.getStackSize(),
          "Should use default stack size");
      assertEquals(PoolingAllocatorConfig.DEFAULT_MAX_STACKS, config.getMaxStacks(),
          "Should use default max stacks");
      assertEquals(PoolingAllocatorConfig.DEFAULT_MAX_TABLES_PER_INSTANCE,
          config.getMaxTablesPerInstance(),
          "Should use default max tables per instance");
      assertEquals(PoolingAllocatorConfig.DEFAULT_MAX_TABLES, config.getMaxTables(),
          "Should use default max tables");
      assertTrue(config.isMemoryDecommitEnabled(), "Memory decommit should be enabled by default");
      assertTrue(config.isPoolWarmingEnabled(), "Pool warming should be enabled by default");
      assertEquals(PoolingAllocatorConfig.DEFAULT_POOL_WARMING_PERCENTAGE,
          config.getPoolWarmingPercentage(),
          "Should use default pool warming percentage");
    }

    @Test
    @DisplayName("Default config should be valid")
    void defaultConfigShouldBeValid() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig();

      assertDoesNotThrow(config::validate, "Default config should be valid");
    }
  }

  @Nested
  @DisplayName("Basic Constructor Tests")
  class BasicConstructorTests {

    @Test
    @DisplayName("Basic constructor should set all provided values")
    void basicConstructorShouldSetAllProvidedValues() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          200,       // instancePoolSize
          64L * 1024 * 1024, // maxMemoryPerInstance (64MB)
          128 * 1024, // stackSize (128KB)
          50,        // maxStacks
          5,         // maxTablesPerInstance
          200,       // maxTables
          false,     // memoryDecommitEnabled
          false,     // poolWarmingEnabled
          0.5f,      // poolWarmingPercentage
          100,       // totalCoreInstances
          50,        // totalComponentInstances
          10,        // maxCoreInstancesPerComponent
          50,        // totalGcHeaps
          128L * 1024 * 1024  // maxMemorySize (128MB)
      );

      assertEquals(200, config.getInstancePoolSize(), "Instance pool size should match");
      assertEquals(64L * 1024 * 1024, config.getMaxMemoryPerInstance(),
          "Max memory per instance should match");
      assertEquals(128 * 1024, config.getStackSize(), "Stack size should match");
      assertEquals(50, config.getMaxStacks(), "Max stacks should match");
      assertEquals(5, config.getMaxTablesPerInstance(), "Max tables per instance should match");
      assertEquals(200, config.getMaxTables(), "Max tables should match");
      assertFalse(config.isMemoryDecommitEnabled(), "Memory decommit should be disabled");
      assertFalse(config.isPoolWarmingEnabled(), "Pool warming should be disabled");
      assertEquals(0.5f, config.getPoolWarmingPercentage(), "Pool warming percentage should match");
      assertEquals(100, config.getTotalCoreInstances(), "Total core instances should match");
      assertEquals(50, config.getTotalComponentInstances(), "Total component instances should match");
      assertEquals(10, config.getMaxCoreInstancesPerComponent(),
          "Max core instances per component should match");
      assertEquals(50, config.getTotalGcHeaps(), "Total GC heaps should match");
      assertEquals(128L * 1024 * 1024, config.getMaxMemorySize(), "Max memory size should match");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("validate should throw on zero instancePoolSize")
    void validateShouldThrowOnZeroInstancePoolSize() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          0, 1024, 1024, 1, 1, 1, true, true, 0.5f, 1, 1, 1, 1, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on zero instancePoolSize");
    }

    @Test
    @DisplayName("validate should throw on negative instancePoolSize")
    void validateShouldThrowOnNegativeInstancePoolSize() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          -1, 1024, 1024, 1, 1, 1, true, true, 0.5f, 1, 1, 1, 1, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on negative instancePoolSize");
    }

    @Test
    @DisplayName("validate should throw on zero maxMemoryPerInstance")
    void validateShouldThrowOnZeroMaxMemoryPerInstance() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 0, 1024, 1, 1, 1, true, true, 0.5f, 1, 1, 1, 1, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on zero maxMemoryPerInstance");
    }

    @Test
    @DisplayName("validate should throw on zero stackSize")
    void validateShouldThrowOnZeroStackSize() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 0, 1, 1, 1, true, true, 0.5f, 1, 1, 1, 1, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on zero stackSize");
    }

    @Test
    @DisplayName("validate should throw on zero maxStacks")
    void validateShouldThrowOnZeroMaxStacks() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 0, 1, 1, true, true, 0.5f, 1, 1, 1, 1, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on zero maxStacks");
    }

    @Test
    @DisplayName("validate should allow zero maxTablesPerInstance")
    void validateShouldAllowZeroMaxTablesPerInstance() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 1, 0, 1, true, true, 0.5f, 1, 1, 1, 1, 1024);

      assertDoesNotThrow(config::validate,
          "Should allow zero maxTablesPerInstance");
    }

    @Test
    @DisplayName("validate should throw on negative maxTablesPerInstance")
    void validateShouldThrowOnNegativeMaxTablesPerInstance() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 1, -1, 1, true, true, 0.5f, 1, 1, 1, 1, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on negative maxTablesPerInstance");
    }

    @Test
    @DisplayName("validate should throw on zero maxTables")
    void validateShouldThrowOnZeroMaxTables() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 1, 1, 0, true, true, 0.5f, 1, 1, 1, 1, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on zero maxTables");
    }

    @Test
    @DisplayName("validate should throw on poolWarmingPercentage less than 0")
    void validateShouldThrowOnPoolWarmingPercentageLessThanZero() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 1, 1, 1, true, true, -0.1f, 1, 1, 1, 1, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on poolWarmingPercentage < 0");
    }

    @Test
    @DisplayName("validate should throw on poolWarmingPercentage greater than 1")
    void validateShouldThrowOnPoolWarmingPercentageGreaterThanOne() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 1, 1, 1, true, true, 1.1f, 1, 1, 1, 1, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on poolWarmingPercentage > 1");
    }

    @Test
    @DisplayName("validate should accept poolWarmingPercentage of 0.0")
    void validateShouldAcceptPoolWarmingPercentageOfZero() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 1, 1, 1, true, true, 0.0f, 1, 1, 1, 1, 1024);

      assertDoesNotThrow(config::validate, "Should accept 0.0 poolWarmingPercentage");
    }

    @Test
    @DisplayName("validate should accept poolWarmingPercentage of 1.0")
    void validateShouldAcceptPoolWarmingPercentageOfOne() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 1, 1, 1, true, true, 1.0f, 1, 1, 1, 1, 1024);

      assertDoesNotThrow(config::validate, "Should accept 1.0 poolWarmingPercentage");
    }

    @Test
    @DisplayName("validate should throw on zero totalCoreInstances")
    void validateShouldThrowOnZeroTotalCoreInstances() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 1, 1, 1, true, true, 0.5f, 0, 1, 1, 1, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on zero totalCoreInstances");
    }

    @Test
    @DisplayName("validate should throw on zero totalComponentInstances")
    void validateShouldThrowOnZeroTotalComponentInstances() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 1, 1, 1, true, true, 0.5f, 1, 0, 1, 1, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on zero totalComponentInstances");
    }

    @Test
    @DisplayName("validate should throw on zero maxCoreInstancesPerComponent")
    void validateShouldThrowOnZeroMaxCoreInstancesPerComponent() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 1, 1, 1, true, true, 0.5f, 1, 1, 0, 1, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on zero maxCoreInstancesPerComponent");
    }

    @Test
    @DisplayName("validate should throw on zero totalGcHeaps")
    void validateShouldThrowOnZeroTotalGcHeaps() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 1, 1, 1, true, true, 0.5f, 1, 1, 1, 0, 1024);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on zero totalGcHeaps");
    }

    @Test
    @DisplayName("validate should throw on zero maxMemorySize")
    void validateShouldThrowOnZeroMaxMemorySize() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          1, 1024, 1024, 1, 1, 1, true, true, 0.5f, 1, 1, 1, 1, 0);

      assertThrows(IllegalArgumentException.class, config::validate,
          "Should throw on zero maxMemorySize");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include all configuration values")
    void toStringShouldIncludeAllConfigurationValues() {
      final JniPoolingAllocatorConfig config = new JniPoolingAllocatorConfig(
          100, 1024L * 1024, 64 * 1024, 10, 5, 50, true, true, 0.5f, 50, 25, 5, 10, 4L * 1024 * 1024
      );

      final String str = config.toString();

      assertTrue(str.contains("instancePoolSize=100"), "Should contain instancePoolSize");
      assertTrue(str.contains("maxMemoryPerInstance=1048576"), "Should contain maxMemoryPerInstance");
      assertTrue(str.contains("stackSize=65536"), "Should contain stackSize");
      assertTrue(str.contains("maxStacks=10"), "Should contain maxStacks");
      assertTrue(str.contains("maxTablesPerInstance=5"), "Should contain maxTablesPerInstance");
      assertTrue(str.contains("maxTables=50"), "Should contain maxTables");
      assertTrue(str.contains("memoryDecommitEnabled=true"), "Should contain memoryDecommitEnabled");
      assertTrue(str.contains("poolWarmingEnabled=true"), "Should contain poolWarmingEnabled");
      assertTrue(str.contains("poolWarmingPercentage=0.5"), "Should contain poolWarmingPercentage");
      assertTrue(str.contains("totalCoreInstances=50"), "Should contain totalCoreInstances");
      assertTrue(str.contains("totalComponentInstances=25"), "Should contain totalComponentInstances");
    }
  }
}
