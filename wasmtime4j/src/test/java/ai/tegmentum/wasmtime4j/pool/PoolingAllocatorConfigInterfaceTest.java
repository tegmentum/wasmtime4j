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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PoolingAllocatorConfig} interface.
 *
 * <p>PoolingAllocatorConfig provides configuration for the pooling allocator.
 */
@DisplayName("PoolingAllocatorConfig Interface Tests")
class PoolingAllocatorConfigInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          PoolingAllocatorConfig.class.isInterface(),
          "PoolingAllocatorConfig should be an interface");
    }

    @Test
    @DisplayName("should have getInstancePoolSize method")
    void shouldHaveGetInstancePoolSizeMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("getInstancePoolSize");
      assertNotNull(method, "getInstancePoolSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxMemoryPerInstance method")
    void shouldHaveGetMaxMemoryPerInstanceMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("getMaxMemoryPerInstance");
      assertNotNull(method, "getMaxMemoryPerInstance method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStackSize method")
    void shouldHaveGetStackSizeMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("getStackSize");
      assertNotNull(method, "getStackSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxStacks method")
    void shouldHaveGetMaxStacksMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("getMaxStacks");
      assertNotNull(method, "getMaxStacks method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxTablesPerInstance method")
    void shouldHaveGetMaxTablesPerInstanceMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("getMaxTablesPerInstance");
      assertNotNull(method, "getMaxTablesPerInstance method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxTables method")
    void shouldHaveGetMaxTablesMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("getMaxTables");
      assertNotNull(method, "getMaxTables method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isMemoryDecommitEnabled method")
    void shouldHaveIsMemoryDecommitEnabledMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("isMemoryDecommitEnabled");
      assertNotNull(method, "isMemoryDecommitEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isPoolWarmingEnabled method")
    void shouldHaveIsPoolWarmingEnabledMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("isPoolWarmingEnabled");
      assertNotNull(method, "isPoolWarmingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getPoolWarmingPercentage method")
    void shouldHaveGetPoolWarmingPercentageMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("getPoolWarmingPercentage");
      assertNotNull(method, "getPoolWarmingPercentage method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }

    @Test
    @DisplayName("should have getTotalCoreInstances method")
    void shouldHaveGetTotalCoreInstancesMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("getTotalCoreInstances");
      assertNotNull(method, "getTotalCoreInstances method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getTotalComponentInstances method")
    void shouldHaveGetTotalComponentInstancesMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("getTotalComponentInstances");
      assertNotNull(method, "getTotalComponentInstances method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxCoreInstancesPerComponent method")
    void shouldHaveGetMaxCoreInstancesPerComponentMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorConfig.class.getMethod("getMaxCoreInstancesPerComponent");
      assertNotNull(method, "getMaxCoreInstancesPerComponent method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getTotalGcHeaps method")
    void shouldHaveGetTotalGcHeapsMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("getTotalGcHeaps");
      assertNotNull(method, "getTotalGcHeaps method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxMemorySize method")
    void shouldHaveGetMaxMemorySizeMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("getMaxMemorySize");
      assertNotNull(method, "getMaxMemorySize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Default Constant Tests")
  class DefaultConstantTests {

    @Test
    @DisplayName("should have DEFAULT_INSTANCE_POOL_SIZE constant")
    void shouldHaveDefaultInstancePoolSizeConstant() throws NoSuchFieldException {
      final Field field = PoolingAllocatorConfig.class.getField("DEFAULT_INSTANCE_POOL_SIZE");
      assertNotNull(field, "DEFAULT_INSTANCE_POOL_SIZE should exist");
      assertEquals(int.class, field.getType(), "Should be int type");
    }

    @Test
    @DisplayName("should have DEFAULT_MAX_MEMORY_PER_INSTANCE constant")
    void shouldHaveDefaultMaxMemoryPerInstanceConstant() throws NoSuchFieldException {
      final Field field = PoolingAllocatorConfig.class.getField("DEFAULT_MAX_MEMORY_PER_INSTANCE");
      assertNotNull(field, "DEFAULT_MAX_MEMORY_PER_INSTANCE should exist");
      assertEquals(long.class, field.getType(), "Should be long type");
    }

    @Test
    @DisplayName("should have DEFAULT_STACK_SIZE constant")
    void shouldHaveDefaultStackSizeConstant() throws NoSuchFieldException {
      final Field field = PoolingAllocatorConfig.class.getField("DEFAULT_STACK_SIZE");
      assertNotNull(field, "DEFAULT_STACK_SIZE should exist");
      assertEquals(int.class, field.getType(), "Should be int type");
    }

    @Test
    @DisplayName("should have DEFAULT_MAX_STACKS constant")
    void shouldHaveDefaultMaxStacksConstant() throws NoSuchFieldException {
      final Field field = PoolingAllocatorConfig.class.getField("DEFAULT_MAX_STACKS");
      assertNotNull(field, "DEFAULT_MAX_STACKS should exist");
      assertEquals(int.class, field.getType(), "Should be int type");
    }

    @Test
    @DisplayName("should have DEFAULT_POOL_WARMING_PERCENTAGE constant")
    void shouldHaveDefaultPoolWarmingPercentageConstant() throws NoSuchFieldException {
      final Field field = PoolingAllocatorConfig.class.getField("DEFAULT_POOL_WARMING_PERCENTAGE");
      assertNotNull(field, "DEFAULT_POOL_WARMING_PERCENTAGE should exist");
      assertEquals(float.class, field.getType(), "Should be float type");
    }

    @Test
    @DisplayName("DEFAULT_INSTANCE_POOL_SIZE should be 1000")
    void defaultInstancePoolSizeShouldBe1000() {
      assertEquals(
          1000,
          PoolingAllocatorConfig.DEFAULT_INSTANCE_POOL_SIZE,
          "DEFAULT_INSTANCE_POOL_SIZE should be 1000");
    }

    @Test
    @DisplayName("DEFAULT_STACK_SIZE should be 1MB")
    void defaultStackSizeShouldBe1MB() {
      assertEquals(
          1024 * 1024,
          PoolingAllocatorConfig.DEFAULT_STACK_SIZE,
          "DEFAULT_STACK_SIZE should be 1MB");
    }

    @Test
    @DisplayName("DEFAULT_POOL_WARMING_PERCENTAGE should be 0.2")
    void defaultPoolWarmingPercentageShouldBe0Point2() {
      assertEquals(
          0.2f,
          PoolingAllocatorConfig.DEFAULT_POOL_WARMING_PERCENTAGE,
          0.001f,
          "DEFAULT_POOL_WARMING_PERCENTAGE should be 0.2");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("builder");
      assertNotNull(method, "builder() method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "builder() should be static");
    }

    @Test
    @DisplayName("should have static defaultConfig method")
    void shouldHaveStaticDefaultConfigMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfig.class.getMethod("defaultConfig");
      assertNotNull(method, "defaultConfig() method should exist");
      assertEquals(
          PoolingAllocatorConfig.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfig");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "defaultConfig() should be static");
    }
  }
}
