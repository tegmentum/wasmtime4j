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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.cache.ModuleCache;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniModuleCache} class.
 *
 * <p>JniModuleCache provides JNI implementation of the module caching functionality.
 */
@DisplayName("JniModuleCache Tests")
class JniModuleCacheTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(Modifier.isPublic(JniModuleCache.class.getModifiers()), "JniModuleCache should be public");
      assertTrue(Modifier.isFinal(JniModuleCache.class.getModifiers()), "JniModuleCache should be final");
    }

    @Test
    @DisplayName("should implement ModuleCache interface")
    void shouldImplementModuleCacheInterface() {
      assertTrue(
          ModuleCache.class.isAssignableFrom(JniModuleCache.class),
          "JniModuleCache should implement ModuleCache");
    }

    @Test
    @DisplayName("should have constructor with engine and config parameters")
    void shouldHaveConstructorWithEngineAndConfig() throws NoSuchMethodException {
      final Constructor<?> constructor =
          JniModuleCache.class.getConstructor(
              JniEngine.class, ai.tegmentum.wasmtime4j.cache.ModuleCacheConfig.class);
      assertNotNull(constructor, "Constructor with engine and config should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getOrCompile method")
    void shouldHaveGetOrCompileMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("getOrCompile", byte[].class);
      assertNotNull(method, "getOrCompile method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.Module.class, method.getReturnType(), "getOrCompile should return Module");
    }

    @Test
    @DisplayName("should have precompile method")
    void shouldHavePrecompileMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("precompile", byte[].class);
      assertNotNull(method, "precompile method should exist");
      assertEquals(String.class, method.getReturnType(), "precompile should return String");
    }

    @Test
    @DisplayName("should have contains method")
    void shouldHaveContainsMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("contains", byte[].class);
      assertNotNull(method, "contains method should exist");
      assertEquals(boolean.class, method.getReturnType(), "contains should return boolean");
    }

    @Test
    @DisplayName("should have remove method")
    void shouldHaveRemoveMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("remove", byte[].class);
      assertNotNull(method, "remove method should exist");
      assertEquals(boolean.class, method.getReturnType(), "remove should return boolean");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "clear should return void");
    }

    @Test
    @DisplayName("should have performMaintenance method")
    void shouldHavePerformMaintenanceMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("performMaintenance");
      assertNotNull(method, "performMaintenance method should exist");
      assertEquals(void.class, method.getReturnType(), "performMaintenance should return void");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.cache.ModuleCacheStatistics.class,
          method.getReturnType(),
          "getStatistics should return ModuleCacheStatistics");
    }

    @Test
    @DisplayName("should have getEntryCount method")
    void shouldHaveGetEntryCountMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("getEntryCount");
      assertNotNull(method, "getEntryCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getEntryCount should return long");
    }

    @Test
    @DisplayName("should have getStorageBytesUsed method")
    void shouldHaveGetStorageBytesUsedMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("getStorageBytesUsed");
      assertNotNull(method, "getStorageBytesUsed method should exist");
      assertEquals(long.class, method.getReturnType(), "getStorageBytesUsed should return long");
    }

    @Test
    @DisplayName("should have getHitCount method")
    void shouldHaveGetHitCountMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("getHitCount");
      assertNotNull(method, "getHitCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getHitCount should return long");
    }

    @Test
    @DisplayName("should have getMissCount method")
    void shouldHaveGetMissCountMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("getMissCount");
      assertNotNull(method, "getMissCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getMissCount should return long");
    }

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertEquals(ai.tegmentum.wasmtime4j.Engine.class, method.getReturnType(), "getEngine should return Engine");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.cache.ModuleCacheConfig.class,
          method.getReturnType(),
          "getConfig should return ModuleCacheConfig");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = JniModuleCache.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }
}
