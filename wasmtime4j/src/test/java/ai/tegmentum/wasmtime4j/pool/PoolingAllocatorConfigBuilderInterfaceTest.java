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

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PoolingAllocatorConfigBuilder} interface.
 *
 * <p>PoolingAllocatorConfigBuilder provides a fluent API for creating PoolingAllocatorConfig.
 */
@DisplayName("PoolingAllocatorConfigBuilder Interface Tests")
class PoolingAllocatorConfigBuilderInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          PoolingAllocatorConfigBuilder.class.isInterface(),
          "PoolingAllocatorConfigBuilder should be an interface");
    }

    @Test
    @DisplayName("should have instancePoolSize method")
    void shouldHaveInstancePoolSizeMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("instancePoolSize", int.class);
      assertNotNull(method, "instancePoolSize method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have maxMemoryPerInstance method")
    void shouldHaveMaxMemoryPerInstanceMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("maxMemoryPerInstance", long.class);
      assertNotNull(method, "maxMemoryPerInstance method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have stackSize method")
    void shouldHaveStackSizeMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfigBuilder.class.getMethod("stackSize", int.class);
      assertNotNull(method, "stackSize method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have maxStacks method")
    void shouldHaveMaxStacksMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfigBuilder.class.getMethod("maxStacks", int.class);
      assertNotNull(method, "maxStacks method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have maxTablesPerInstance method")
    void shouldHaveMaxTablesPerInstanceMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("maxTablesPerInstance", int.class);
      assertNotNull(method, "maxTablesPerInstance method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have maxTables method")
    void shouldHaveMaxTablesMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfigBuilder.class.getMethod("maxTables", int.class);
      assertNotNull(method, "maxTables method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have memoryDecommitEnabled method")
    void shouldHaveMemoryDecommitEnabledMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("memoryDecommitEnabled", boolean.class);
      assertNotNull(method, "memoryDecommitEnabled method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have poolWarmingEnabled method")
    void shouldHavePoolWarmingEnabledMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("poolWarmingEnabled", boolean.class);
      assertNotNull(method, "poolWarmingEnabled method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have poolWarmingPercentage method")
    void shouldHavePoolWarmingPercentageMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("poolWarmingPercentage", float.class);
      assertNotNull(method, "poolWarmingPercentage method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have totalCoreInstances method")
    void shouldHaveTotalCoreInstancesMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("totalCoreInstances", int.class);
      assertNotNull(method, "totalCoreInstances method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have totalComponentInstances method")
    void shouldHaveTotalComponentInstancesMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("totalComponentInstances", int.class);
      assertNotNull(method, "totalComponentInstances method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have maxMemorySize method")
    void shouldHaveMaxMemorySizeMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("maxMemorySize", long.class);
      assertNotNull(method, "maxMemorySize method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have memoryProtectionKeysEnabled method")
    void shouldHaveMemoryProtectionKeysEnabledMethod() throws NoSuchMethodException {
      final Method method =
          PoolingAllocatorConfigBuilder.class.getMethod(
              "memoryProtectionKeysEnabled", boolean.class);
      assertNotNull(method, "memoryProtectionKeysEnabled method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder for chaining");
    }

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocatorConfigBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          PoolingAllocatorConfig.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfig");
    }
  }
}
