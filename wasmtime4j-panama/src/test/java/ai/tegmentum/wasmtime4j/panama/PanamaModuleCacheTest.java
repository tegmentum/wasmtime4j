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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.cache.ModuleCache;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheConfig;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheStatistics;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaModuleCache} class.
 *
 * <p>PanamaModuleCache provides caching for compiled WebAssembly modules.
 */
@DisplayName("PanamaModuleCache Tests")
class PanamaModuleCacheTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaModuleCache.class.getModifiers()),
          "PanamaModuleCache should be public");
      assertTrue(
          Modifier.isFinal(PanamaModuleCache.class.getModifiers()),
          "PanamaModuleCache should be final");
    }

    @Test
    @DisplayName("should implement ModuleCache interface")
    void shouldImplementModuleCacheInterface() {
      assertTrue(
          ModuleCache.class.isAssignableFrom(PanamaModuleCache.class),
          "PanamaModuleCache should implement ModuleCache");
    }
  }

  @Nested
  @DisplayName("Core Cache Method Tests")
  class CoreCacheMethodTests {

    @Test
    @DisplayName("should have getOrCompile method")
    void shouldHaveGetOrCompileMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("getOrCompile", byte[].class);
      assertNotNull(method, "getOrCompile method should exist");
      assertEquals(Module.class, method.getReturnType(), "Should return Module");
    }

    @Test
    @DisplayName("should have precompile method")
    void shouldHavePrecompileMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("precompile", byte[].class);
      assertNotNull(method, "precompile method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have contains method")
    void shouldHaveContainsMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("contains", byte[].class);
      assertNotNull(method, "contains method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have remove method")
    void shouldHaveRemoveMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("remove", byte[].class);
      assertNotNull(method, "remove method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Cache Management Method Tests")
  class CacheManagementMethodTests {

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have performMaintenance method")
    void shouldHavePerformMaintenanceMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("performMaintenance");
      assertNotNull(method, "performMaintenance method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ModuleCacheStatistics.class,
          method.getReturnType(),
          "Should return ModuleCacheStatistics");
    }

    @Test
    @DisplayName("should have getEntryCount method")
    void shouldHaveGetEntryCountMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("getEntryCount");
      assertNotNull(method, "getEntryCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStorageBytesUsed method")
    void shouldHaveGetStorageBytesUsedMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("getStorageBytesUsed");
      assertNotNull(method, "getStorageBytesUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getHitCount method")
    void shouldHaveGetHitCountMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("getHitCount");
      assertNotNull(method, "getHitCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMissCount method")
    void shouldHaveGetMissCountMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("getMissCount");
      assertNotNull(method, "getMissCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          ModuleCacheConfig.class, method.getReturnType(), "Should return ModuleCacheConfig");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with PanamaEngine and ModuleCacheConfig")
    void shouldHaveConstructorWithEngineAndConfig() {
      try {
        PanamaModuleCache.class.getConstructor(PanamaEngine.class, ModuleCacheConfig.class);
      } catch (NoSuchMethodException e) {
        throw new AssertionError("Constructor should exist", e);
      }
    }
  }

  @Nested
  @DisplayName("ToString Method Tests")
  class ToStringMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = PanamaModuleCache.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
