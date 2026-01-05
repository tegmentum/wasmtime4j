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

import ai.tegmentum.wasmtime4j.EngineConfig;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the PoolingAllocator interface.
 *
 * <p>PoolingAllocator provides high-performance pooling allocation for WebAssembly execution.
 */
@DisplayName("PoolingAllocator Interface Tests")
class PoolingAllocatorTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(PoolingAllocator.class.isInterface(), "PoolingAllocator should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(PoolingAllocator.class.getModifiers()),
          "PoolingAllocator should be public");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(PoolingAllocator.class),
          "PoolingAllocator should extend Closeable");
    }

    @Test
    @DisplayName("should have exactly 1 super interface")
    void shouldHaveExactly1SuperInterface() {
      assertEquals(
          1, PoolingAllocator.class.getInterfaces().length, "Should extend exactly 1 interface");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static create() method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("create");
      assertNotNull(method, "create() method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          PoolingAllocator.class, method.getReturnType(), "Should return PoolingAllocator");
    }

    @Test
    @DisplayName("should have static create(PoolingAllocatorConfig) method")
    void shouldHaveStaticCreateWithConfigMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("create", PoolingAllocatorConfig.class);
      assertNotNull(method, "create(PoolingAllocatorConfig) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          PoolingAllocator.class, method.getReturnType(), "Should return PoolingAllocator");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          PoolingAllocatorConfig.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfig");
    }

    @Test
    @DisplayName("should have allocateInstance method")
    void shouldHaveAllocateInstanceMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("allocateInstance");
      assertNotNull(method, "allocateInstance method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have reuseInstance method")
    void shouldHaveReuseInstanceMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("reuseInstance", long.class);
      assertNotNull(method, "reuseInstance method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have releaseInstance method")
    void shouldHaveReleaseInstanceMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("releaseInstance", long.class);
      assertNotNull(method, "releaseInstance method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(PoolStatistics.class, method.getReturnType(), "Should return PoolStatistics");
    }

    @Test
    @DisplayName("should have resetStatistics method")
    void shouldHaveResetStatisticsMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("resetStatistics");
      assertNotNull(method, "resetStatistics method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have warmPools method")
    void shouldHaveWarmPoolsMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("warmPools");
      assertNotNull(method, "warmPools method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have performMaintenance method")
    void shouldHavePerformMaintenanceMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("performMaintenance");
      assertNotNull(method, "performMaintenance method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have configureEngine method")
    void shouldHaveConfigureEngineMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("configureEngine", EngineConfig.class);
      assertNotNull(method, "configureEngine method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getUptime method")
    void shouldHaveGetUptimeMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("getUptime");
      assertNotNull(method, "getUptime method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method from Closeable")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have getMetrics default method")
    void shouldHaveGetMetricsDefaultMethod() throws NoSuchMethodException {
      Method method = PoolingAllocator.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
      assertTrue(method.isDefault(), "getMetrics should be a default method");
      assertEquals(
          PoolingAllocatorMetrics.class,
          method.getReturnType(),
          "Should return PoolingAllocatorMetrics");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected abstract methods")
    void shouldHaveAllExpectedAbstractMethods() {
      Set<String> expectedAbstractMethods =
          Set.of(
              "getConfig",
              "allocateInstance",
              "reuseInstance",
              "releaseInstance",
              "getStatistics",
              "resetStatistics",
              "warmPools",
              "performMaintenance",
              "configureEngine",
              "getUptime",
              "isValid",
              "close");

      Set<String> actualAbstractMethods =
          Arrays.stream(PoolingAllocator.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedAbstractMethods) {
        assertTrue(actualAbstractMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 2 static factory methods")
    void shouldHaveExactly2StaticMethods() {
      long staticCount =
          Arrays.stream(PoolingAllocator.class.getDeclaredMethods())
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
          PoolingAllocator.class.getDeclaredClasses().length,
          "PoolingAllocator should have no nested classes");
    }
  }
}
