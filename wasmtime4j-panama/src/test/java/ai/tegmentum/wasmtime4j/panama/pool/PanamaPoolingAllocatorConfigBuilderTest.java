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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfigBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaPoolingAllocatorConfigBuilder} class.
 *
 * <p>PanamaPoolingAllocatorConfigBuilder provides builder for pooling allocator configuration.
 */
@DisplayName("PanamaPoolingAllocatorConfigBuilder Tests")
class PanamaPoolingAllocatorConfigBuilderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaPoolingAllocatorConfigBuilder.class.getModifiers()),
          "PanamaPoolingAllocatorConfigBuilder should be public");
      assertTrue(
          Modifier.isFinal(PanamaPoolingAllocatorConfigBuilder.class.getModifiers()),
          "PanamaPoolingAllocatorConfigBuilder should be final");
    }

    @Test
    @DisplayName("should implement PoolingAllocatorConfigBuilder interface")
    void shouldImplementPoolingAllocatorConfigBuilderInterface() {
      assertTrue(
          PoolingAllocatorConfigBuilder.class.isAssignableFrom(
              PanamaPoolingAllocatorConfigBuilder.class),
          "PanamaPoolingAllocatorConfigBuilder should implement PoolingAllocatorConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = PanamaPoolingAllocatorConfigBuilder.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Instance Pool Method Tests")
  class InstancePoolMethodTests {

    @Test
    @DisplayName("should have instancePoolSize method")
    void shouldHaveInstancePoolSizeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("instancePoolSize", int.class);
      assertNotNull(method, "instancePoolSize method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxMemoryPerInstance method")
    void shouldHaveMaxMemoryPerInstanceMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("maxMemoryPerInstance", long.class);
      assertNotNull(method, "maxMemoryPerInstance method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Stack Method Tests")
  class StackMethodTests {

    @Test
    @DisplayName("should have stackSize method")
    void shouldHaveStackSizeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("stackSize", int.class);
      assertNotNull(method, "stackSize method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxStacks method")
    void shouldHaveMaxStacksMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("maxStacks", int.class);
      assertNotNull(method, "maxStacks method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Table Method Tests")
  class TableMethodTests {

    @Test
    @DisplayName("should have maxTablesPerInstance method")
    void shouldHaveMaxTablesPerInstanceMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("maxTablesPerInstance", int.class);
      assertNotNull(method, "maxTablesPerInstance method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxTables method")
    void shouldHaveMaxTablesMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("maxTables", int.class);
      assertNotNull(method, "maxTables method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have tableElements method")
    void shouldHaveTableElementsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("tableElements", int.class);
      assertNotNull(method, "tableElements method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Memory Method Tests")
  class MemoryMethodTests {

    @Test
    @DisplayName("should have memoryDecommitEnabled method")
    void shouldHaveMemoryDecommitEnabledMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod(
              "memoryDecommitEnabled", boolean.class);
      assertNotNull(method, "memoryDecommitEnabled method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxMemorySize method")
    void shouldHaveMaxMemorySizeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("maxMemorySize", long.class);
      assertNotNull(method, "maxMemorySize method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have totalMemories method")
    void shouldHaveTotalMemoriesMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("totalMemories", int.class);
      assertNotNull(method, "totalMemories method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Pool Warming Method Tests")
  class PoolWarmingMethodTests {

    @Test
    @DisplayName("should have poolWarmingEnabled method")
    void shouldHavePoolWarmingEnabledMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("poolWarmingEnabled", boolean.class);
      assertNotNull(method, "poolWarmingEnabled method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have poolWarmingPercentage method")
    void shouldHavePoolWarmingPercentageMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("poolWarmingPercentage", float.class);
      assertNotNull(method, "poolWarmingPercentage method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Core Instances Method Tests")
  class CoreInstancesMethodTests {

    @Test
    @DisplayName("should have totalCoreInstances method")
    void shouldHaveTotalCoreInstancesMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("totalCoreInstances", int.class);
      assertNotNull(method, "totalCoreInstances method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have totalComponentInstances method")
    void shouldHaveTotalComponentInstancesMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("totalComponentInstances", int.class);
      assertNotNull(method, "totalComponentInstances method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxCoreInstancesPerComponent method")
    void shouldHaveMaxCoreInstancesPerComponentMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod(
              "maxCoreInstancesPerComponent", int.class);
      assertNotNull(method, "maxCoreInstancesPerComponent method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  @Nested
  @DisplayName("GC Heaps Method Tests")
  class GcHeapsMethodTests {

    @Test
    @DisplayName("should have totalGcHeaps method")
    void shouldHaveTotalGcHeapsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocatorConfigBuilder.class.getMethod("totalGcHeaps", int.class);
      assertNotNull(method, "totalGcHeaps method should exist");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Build Method Tests")
  class BuildMethodTests {

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocatorConfigBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          PoolingAllocatorConfig.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfig");
    }
  }
}
