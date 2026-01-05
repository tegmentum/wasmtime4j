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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the PoolingAllocatorConfigBuilder interface.
 *
 * <p>PoolingAllocatorConfigBuilder provides a fluent API for configuring pooling allocator
 * settings.
 */
@DisplayName("PoolingAllocatorConfigBuilder Interface Tests")
class PoolingAllocatorConfigBuilderTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          PoolingAllocatorConfigBuilder.class.isInterface(),
          "PoolingAllocatorConfigBuilder should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(PoolingAllocatorConfigBuilder.class.getModifiers()),
          "PoolingAllocatorConfigBuilder should be public");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interface")
    void shouldNotExtendAnyInterface() {
      assertEquals(
          0,
          PoolingAllocatorConfigBuilder.class.getInterfaces().length,
          "Should not extend any interface");
    }
  }

  // ========================================================================
  // Builder Method Tests - Pool Size Settings
  // ========================================================================

  @Nested
  @DisplayName("Pool Size Builder Method Tests")
  class PoolSizeBuilderMethodTests {

    @Test
    @DisplayName("should have instancePoolSize method")
    void shouldHaveInstancePoolSizeMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfigBuilder.class.getMethod("instancePoolSize", int.class);
      assertNotNull(method, "instancePoolSize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxStacks method")
    void shouldHaveMaxStacksMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfigBuilder.class.getMethod("maxStacks", int.class);
      assertNotNull(method, "maxStacks method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have totalCoreInstances method")
    void shouldHaveTotalCoreInstancesMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("totalCoreInstances", int.class);
      assertNotNull(method, "totalCoreInstances method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have totalComponentInstances method")
    void shouldHaveTotalComponentInstancesMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("totalComponentInstances", int.class);
      assertNotNull(method, "totalComponentInstances method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxCoreInstancesPerComponent method")
    void shouldHaveMaxCoreInstancesPerComponentMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("maxCoreInstancesPerComponent", int.class);
      assertNotNull(method, "maxCoreInstancesPerComponent method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  // ========================================================================
  // Builder Method Tests - Memory Settings
  // ========================================================================

  @Nested
  @DisplayName("Memory Builder Method Tests")
  class MemoryBuilderMethodTests {

    @Test
    @DisplayName("should have maxMemoryPerInstance method")
    void shouldHaveMaxMemoryPerInstanceMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("maxMemoryPerInstance", long.class);
      assertNotNull(method, "maxMemoryPerInstance method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxMemorySize method")
    void shouldHaveMaxMemorySizeMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfigBuilder.class.getMethod("maxMemorySize", long.class);
      assertNotNull(method, "maxMemorySize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have totalMemories method")
    void shouldHaveTotalMemoriesMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfigBuilder.class.getMethod("totalMemories", int.class);
      assertNotNull(method, "totalMemories method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxMemoriesPerModule method")
    void shouldHaveMaxMemoriesPerModuleMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("maxMemoriesPerModule", int.class);
      assertNotNull(method, "maxMemoriesPerModule method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxMemoriesPerComponent method")
    void shouldHaveMaxMemoriesPerComponentMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("maxMemoriesPerComponent", int.class);
      assertNotNull(method, "maxMemoriesPerComponent method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  // ========================================================================
  // Builder Method Tests - Stack Settings
  // ========================================================================

  @Nested
  @DisplayName("Stack Builder Method Tests")
  class StackBuilderMethodTests {

    @Test
    @DisplayName("should have stackSize method")
    void shouldHaveStackSizeMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfigBuilder.class.getMethod("stackSize", int.class);
      assertNotNull(method, "stackSize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have asyncStackKeepResident method")
    void shouldHaveAsyncStackKeepResidentMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("asyncStackKeepResident", long.class);
      assertNotNull(method, "asyncStackKeepResident method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  // ========================================================================
  // Builder Method Tests - Table Settings
  // ========================================================================

  @Nested
  @DisplayName("Table Builder Method Tests")
  class TableBuilderMethodTests {

    @Test
    @DisplayName("should have maxTablesPerInstance method")
    void shouldHaveMaxTablesPerInstanceMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("maxTablesPerInstance", int.class);
      assertNotNull(method, "maxTablesPerInstance method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxTables method")
    void shouldHaveMaxTablesMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfigBuilder.class.getMethod("maxTables", int.class);
      assertNotNull(method, "maxTables method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have tableElements method")
    void shouldHaveTableElementsMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfigBuilder.class.getMethod("tableElements", int.class);
      assertNotNull(method, "tableElements method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have tableKeepResident method")
    void shouldHaveTableKeepResidentMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("tableKeepResident", long.class);
      assertNotNull(method, "tableKeepResident method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  // ========================================================================
  // Builder Method Tests - Feature Toggles
  // ========================================================================

  @Nested
  @DisplayName("Feature Toggle Builder Method Tests")
  class FeatureToggleBuilderMethodTests {

    @Test
    @DisplayName("should have memoryDecommitEnabled method")
    void shouldHaveMemoryDecommitEnabledMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("memoryDecommitEnabled", boolean.class);
      assertNotNull(method, "memoryDecommitEnabled method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have poolWarmingEnabled method")
    void shouldHavePoolWarmingEnabledMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("poolWarmingEnabled", boolean.class);
      assertNotNull(method, "poolWarmingEnabled method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have poolWarmingPercentage method")
    void shouldHavePoolWarmingPercentageMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("poolWarmingPercentage", float.class);
      assertNotNull(method, "poolWarmingPercentage method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have memoryProtectionKeysEnabled method")
    void shouldHaveMemoryProtectionKeysEnabledMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod(
              "memoryProtectionKeysEnabled", boolean.class);
      assertNotNull(method, "memoryProtectionKeysEnabled method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have pagemapScanEnabled method")
    void shouldHavePagemapScanEnabledMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("pagemapScanEnabled", boolean.class);
      assertNotNull(method, "pagemapScanEnabled method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  // ========================================================================
  // Builder Method Tests - Advanced Settings
  // ========================================================================

  @Nested
  @DisplayName("Advanced Builder Method Tests")
  class AdvancedBuilderMethodTests {

    @Test
    @DisplayName("should have totalGcHeaps method")
    void shouldHaveTotalGcHeapsMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfigBuilder.class.getMethod("totalGcHeaps", int.class);
      assertNotNull(method, "totalGcHeaps method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxUnusedWarmSlots method")
    void shouldHaveMaxUnusedWarmSlotsMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("maxUnusedWarmSlots", int.class);
      assertNotNull(method, "maxUnusedWarmSlots method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have decommitBatchSize method")
    void shouldHaveDecommitBatchSizeMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfigBuilder.class.getMethod("decommitBatchSize", int.class);
      assertNotNull(method, "decommitBatchSize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have linearMemoryKeepResident method")
    void shouldHaveLinearMemoryKeepResidentMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("linearMemoryKeepResident", long.class);
      assertNotNull(method, "linearMemoryKeepResident method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxCoreInstanceSize method")
    void shouldHaveMaxCoreInstanceSizeMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("maxCoreInstanceSize", long.class);
      assertNotNull(method, "maxCoreInstanceSize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxComponentInstanceSize method")
    void shouldHaveMaxComponentInstanceSizeMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("maxComponentInstanceSize", long.class);
      assertNotNull(method, "maxComponentInstanceSize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have maxMemoryProtectionKeys method")
    void shouldHaveMaxMemoryProtectionKeysMethod() throws NoSuchMethodException {
      Method method =
          PoolingAllocatorConfigBuilder.class.getMethod("maxMemoryProtectionKeys", int.class);
      assertNotNull(method, "maxMemoryProtectionKeys method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }
  }

  // ========================================================================
  // Build Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Build Method Tests")
  class BuildMethodTests {

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfigBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          PoolingAllocatorConfig.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfig");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected builder methods")
    void shouldHaveAllExpectedBuilderMethods() {
      Set<String> expectedMethods =
          Set.of(
              "instancePoolSize",
              "maxMemoryPerInstance",
              "stackSize",
              "maxStacks",
              "maxTablesPerInstance",
              "maxTables",
              "memoryDecommitEnabled",
              "poolWarmingEnabled",
              "poolWarmingPercentage",
              "totalCoreInstances",
              "totalComponentInstances",
              "maxCoreInstancesPerComponent",
              "totalGcHeaps",
              "maxMemorySize",
              "maxUnusedWarmSlots",
              "decommitBatchSize",
              "linearMemoryKeepResident",
              "tableKeepResident",
              "asyncStackKeepResident",
              "totalMemories",
              "maxCoreInstanceSize",
              "maxComponentInstanceSize",
              "maxMemoriesPerModule",
              "maxMemoriesPerComponent",
              "tableElements",
              "memoryProtectionKeysEnabled",
              "maxMemoryProtectionKeys",
              "pagemapScanEnabled",
              "build");

      Set<String> actualMethods =
          Arrays.stream(PoolingAllocatorConfigBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 28 abstract methods")
    void shouldHaveAtLeast28AbstractMethods() {
      long abstractCount =
          Arrays.stream(PoolingAllocatorConfigBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertTrue(abstractCount >= 28, "Should have at least 28 abstract methods");
    }

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticCount =
          Arrays.stream(PoolingAllocatorConfigBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticCount, "Should have no static methods");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          PoolingAllocatorConfigBuilder.class.getDeclaredClasses().length,
          "PoolingAllocatorConfigBuilder should have no nested classes");
    }
  }
}
