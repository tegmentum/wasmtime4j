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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the PoolingAllocatorConfig interface.
 *
 * <p>PoolingAllocatorConfig defines configuration for the pooling allocator.
 */
@DisplayName("PoolingAllocatorConfig Interface Tests")
class PoolingAllocatorConfigTest {

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
          PoolingAllocatorConfig.class.isInterface(),
          "PoolingAllocatorConfig should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(PoolingAllocatorConfig.class.getModifiers()),
          "PoolingAllocatorConfig should be public");
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
          PoolingAllocatorConfig.class.getInterfaces().length,
          "Should not extend any interface");
    }
  }

  // ========================================================================
  // Constant Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Constant Field Tests")
  class ConstantFieldTests {

    @Test
    @DisplayName("should have DEFAULT_INSTANCE_POOL_SIZE constant")
    void shouldHaveDefaultInstancePoolSizeConstant() throws NoSuchFieldException {
      Field field = PoolingAllocatorConfig.class.getField("DEFAULT_INSTANCE_POOL_SIZE");
      assertNotNull(field, "DEFAULT_INSTANCE_POOL_SIZE should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int");
    }

    @Test
    @DisplayName("should have DEFAULT_MAX_MEMORY_PER_INSTANCE constant")
    void shouldHaveDefaultMaxMemoryPerInstanceConstant() throws NoSuchFieldException {
      Field field = PoolingAllocatorConfig.class.getField("DEFAULT_MAX_MEMORY_PER_INSTANCE");
      assertNotNull(field, "DEFAULT_MAX_MEMORY_PER_INSTANCE should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(long.class, field.getType(), "Should be long");
    }

    @Test
    @DisplayName("should have DEFAULT_STACK_SIZE constant")
    void shouldHaveDefaultStackSizeConstant() throws NoSuchFieldException {
      Field field = PoolingAllocatorConfig.class.getField("DEFAULT_STACK_SIZE");
      assertNotNull(field, "DEFAULT_STACK_SIZE should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int");
    }

    @Test
    @DisplayName("should have DEFAULT_MAX_STACKS constant")
    void shouldHaveDefaultMaxStacksConstant() throws NoSuchFieldException {
      Field field = PoolingAllocatorConfig.class.getField("DEFAULT_MAX_STACKS");
      assertNotNull(field, "DEFAULT_MAX_STACKS should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("should have DEFAULT_POOL_WARMING_PERCENTAGE constant")
    void shouldHaveDefaultPoolWarmingPercentageConstant() throws NoSuchFieldException {
      Field field = PoolingAllocatorConfig.class.getField("DEFAULT_POOL_WARMING_PERCENTAGE");
      assertNotNull(field, "DEFAULT_POOL_WARMING_PERCENTAGE should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(float.class, field.getType(), "Should be float");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static builder() method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfig.class.getMethod("builder");
      assertNotNull(method, "builder() method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          PoolingAllocatorConfigBuilder.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfigBuilder");
    }

    @Test
    @DisplayName("should have static defaultConfig() method")
    void shouldHaveStaticDefaultConfigMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfig.class.getMethod("defaultConfig");
      assertNotNull(method, "defaultConfig() method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          PoolingAllocatorConfig.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfig");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getInstancePoolSize method")
    void shouldHaveGetInstancePoolSizeMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfig.class.getMethod("getInstancePoolSize");
      assertNotNull(method, "getInstancePoolSize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxMemoryPerInstance method")
    void shouldHaveGetMaxMemoryPerInstanceMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfig.class.getMethod("getMaxMemoryPerInstance");
      assertNotNull(method, "getMaxMemoryPerInstance method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStackSize method")
    void shouldHaveGetStackSizeMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfig.class.getMethod("getStackSize");
      assertNotNull(method, "getStackSize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isMemoryDecommitEnabled method")
    void shouldHaveIsMemoryDecommitEnabledMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfig.class.getMethod("isMemoryDecommitEnabled");
      assertNotNull(method, "isMemoryDecommitEnabled method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isPoolWarmingEnabled method")
    void shouldHaveIsPoolWarmingEnabledMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfig.class.getMethod("isPoolWarmingEnabled");
      assertNotNull(method, "isPoolWarmingEnabled method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getPoolWarmingPercentage method")
    void shouldHaveGetPoolWarmingPercentageMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfig.class.getMethod("getPoolWarmingPercentage");
      assertNotNull(method, "getPoolWarmingPercentage method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }

    @Test
    @DisplayName("should have getTotalCoreInstances method")
    void shouldHaveGetTotalCoreInstancesMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfig.class.getMethod("getTotalCoreInstances");
      assertNotNull(method, "getTotalCoreInstances method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxMemorySize method")
    void shouldHaveGetMaxMemorySizeMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfig.class.getMethod("getMaxMemorySize");
      assertNotNull(method, "getMaxMemorySize method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      Method method = PoolingAllocatorConfig.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have at least 20 abstract methods")
    void shouldHaveAtLeast20AbstractMethods() {
      long abstractCount =
          Arrays.stream(PoolingAllocatorConfig.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertTrue(abstractCount >= 20, "Should have at least 20 abstract methods");
    }

    @Test
    @DisplayName("should have exactly 2 static factory methods")
    void shouldHaveExactly2StaticMethods() {
      long staticCount =
          Arrays.stream(PoolingAllocatorConfig.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(2, staticCount, "Should have exactly 2 static factory methods");
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
          PoolingAllocatorConfig.class.getDeclaredClasses().length,
          "PoolingAllocatorConfig should have no nested classes");
    }
  }
}
