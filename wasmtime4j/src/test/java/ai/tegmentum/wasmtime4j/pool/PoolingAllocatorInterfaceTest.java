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
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PoolingAllocator} interface.
 *
 * <p>PoolingAllocator provides high-performance pooling for WebAssembly execution.
 */
@DisplayName("PoolingAllocator Interface Tests")
class PoolingAllocatorInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(PoolingAllocator.class.isInterface(), "PoolingAllocator should be an interface");
    }

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(PoolingAllocator.class),
          "PoolingAllocator should extend Closeable");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          PoolingAllocatorConfig.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfig");
    }

    @Test
    @DisplayName("should have allocateInstance method")
    void shouldHaveAllocateInstanceMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("allocateInstance");
      assertNotNull(method, "allocateInstance method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertTrue(
          java.util.Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "Should throw WasmException");
    }

    @Test
    @DisplayName("should have reuseInstance method")
    void shouldHaveReuseInstanceMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("reuseInstance", long.class);
      assertNotNull(method, "reuseInstance method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have releaseInstance method")
    void shouldHaveReleaseInstanceMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("releaseInstance", long.class);
      assertNotNull(method, "releaseInstance method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(PoolStatistics.class, method.getReturnType(), "Should return PoolStatistics");
    }

    @Test
    @DisplayName("should have getMetrics default method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
      assertEquals(
          PoolingAllocatorMetrics.class,
          method.getReturnType(),
          "Should return PoolingAllocatorMetrics");
      assertTrue(method.isDefault(), "getMetrics should be a default method");
    }

    @Test
    @DisplayName("should have resetStatistics method")
    void shouldHaveResetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("resetStatistics");
      assertNotNull(method, "resetStatistics method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have warmPools method")
    void shouldHaveWarmPoolsMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("warmPools");
      assertNotNull(method, "warmPools method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have performMaintenance method")
    void shouldHavePerformMaintenanceMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("performMaintenance");
      assertNotNull(method, "performMaintenance method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have configureEngine method")
    void shouldHaveConfigureEngineMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("configureEngine", EngineConfig.class);
      assertNotNull(method, "configureEngine method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getUptime method")
    void shouldHaveGetUptimeMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("getUptime");
      assertNotNull(method, "getUptime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static create method with no args")
    void shouldHaveStaticCreateMethodNoArgs() throws NoSuchMethodException {
      final Method method = PoolingAllocator.class.getMethod("create");
      assertNotNull(method, "create() method should exist");
      assertEquals(
          PoolingAllocator.class, method.getReturnType(), "Should return PoolingAllocator");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()), "create() should be static");
    }

    @Test
    @DisplayName("should have static create method with config")
    void shouldHaveStaticCreateMethodWithConfig() throws NoSuchMethodException {
      final Method method =
          PoolingAllocator.class.getMethod("create", PoolingAllocatorConfig.class);
      assertNotNull(method, "create(config) method should exist");
      assertEquals(
          PoolingAllocator.class, method.getReturnType(), "Should return PoolingAllocator");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "create(config) should be static");
    }
  }
}
