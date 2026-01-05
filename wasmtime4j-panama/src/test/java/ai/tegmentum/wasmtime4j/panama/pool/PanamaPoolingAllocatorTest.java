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

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.pool.PoolStatistics;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocator;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaPoolingAllocator} class.
 *
 * <p>PanamaPoolingAllocator provides Panama implementation of pooling allocation.
 */
@DisplayName("PanamaPoolingAllocator Tests")
class PanamaPoolingAllocatorTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaPoolingAllocator.class.getModifiers()),
          "PanamaPoolingAllocator should be public");
      assertTrue(
          Modifier.isFinal(PanamaPoolingAllocator.class.getModifiers()),
          "PanamaPoolingAllocator should be final");
    }

    @Test
    @DisplayName("should implement PoolingAllocator interface")
    void shouldImplementPoolingAllocatorInterface() {
      assertTrue(
          PoolingAllocator.class.isAssignableFrom(PanamaPoolingAllocator.class),
          "PanamaPoolingAllocator should implement PoolingAllocator");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with PoolingAllocatorConfig")
    void shouldHaveConfigConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaPoolingAllocator.class.getConstructor(PoolingAllocatorConfig.class);
      assertNotNull(constructor, "Constructor with PoolingAllocatorConfig should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Config Method Tests")
  class ConfigMethodTests {

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocator.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          PoolingAllocatorConfig.class,
          method.getReturnType(),
          "Should return PoolingAllocatorConfig");
    }
  }

  @Nested
  @DisplayName("Instance Allocation Method Tests")
  class InstanceAllocationMethodTests {

    @Test
    @DisplayName("should have allocateInstance method")
    void shouldHaveAllocateInstanceMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocator.class.getMethod("allocateInstance");
      assertNotNull(method, "allocateInstance method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have reuseInstance method")
    void shouldHaveReuseInstanceMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocator.class.getMethod("reuseInstance", long.class);
      assertNotNull(method, "reuseInstance method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have releaseInstance method")
    void shouldHaveReleaseInstanceMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocator.class.getMethod("releaseInstance", long.class);
      assertNotNull(method, "releaseInstance method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocator.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(PoolStatistics.class, method.getReturnType(), "Should return PoolStatistics");
    }

    @Test
    @DisplayName("should have resetStatistics method")
    void shouldHaveResetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocator.class.getMethod("resetStatistics");
      assertNotNull(method, "resetStatistics method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Pool Maintenance Method Tests")
  class PoolMaintenanceMethodTests {

    @Test
    @DisplayName("should have warmPools method")
    void shouldHaveWarmPoolsMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocator.class.getMethod("warmPools");
      assertNotNull(method, "warmPools method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have performMaintenance method")
    void shouldHavePerformMaintenanceMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocator.class.getMethod("performMaintenance");
      assertNotNull(method, "performMaintenance method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Engine Configuration Method Tests")
  class EngineConfigurationMethodTests {

    @Test
    @DisplayName("should have configureEngine method")
    void shouldHaveConfigureEngineMethod() throws NoSuchMethodException {
      final Method method =
          PanamaPoolingAllocator.class.getMethod("configureEngine", EngineConfig.class);
      assertNotNull(method, "configureEngine method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Status Method Tests")
  class StatusMethodTests {

    @Test
    @DisplayName("should have getUptime method")
    void shouldHaveGetUptimeMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocator.class.getMethod("getUptime");
      assertNotNull(method, "getUptime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocator.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Native Allocator Method Tests")
  class NativeAllocatorMethodTests {

    @Test
    @DisplayName("should have getNativeAllocator method")
    void shouldHaveGetNativeAllocatorMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocator.class.getMethod("getNativeAllocator");
      assertNotNull(method, "getNativeAllocator method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaPoolingAllocator.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
