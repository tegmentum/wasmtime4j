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

package ai.tegmentum.wasmtime4j.panama.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CompilationCache} class.
 *
 * <p>CompilationCache provides Panama-optimized compilation cache for WebAssembly modules.
 */
@DisplayName("CompilationCache Tests")
class CompilationCacheTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(CompilationCache.class.getModifiers()),
          "CompilationCache should be public");
      assertTrue(
          Modifier.isFinal(CompilationCache.class.getModifiers()),
          "CompilationCache should be final");
    }
  }

  @Nested
  @DisplayName("Cache Operation Method Tests")
  class CacheOperationMethodTests {

    @Test
    @DisplayName("should have loadFromCache method")
    void shouldHaveLoadFromCacheMethod() throws NoSuchMethodException {
      final Method method =
          CompilationCache.class.getMethod(
              "loadFromCache", MemorySegment.class, String.class, Arena.class);
      assertNotNull(method, "loadFromCache method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have storeInCache method")
    void shouldHaveStoreInCacheMethod() throws NoSuchMethodException {
      final Method method =
          CompilationCache.class.getMethod(
              "storeInCache", MemorySegment.class, MemorySegment.class, String.class, long.class);
      assertNotNull(method, "storeInCache method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = CompilationCache.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = CompilationCache.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have getHitRate method")
    void shouldHaveGetHitRateMethod() throws NoSuchMethodException {
      final Method method = CompilationCache.class.getMethod("getHitRate");
      assertNotNull(method, "getHitRate method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have getPerformanceMetrics method")
    void shouldHaveGetPerformanceMetricsMethod() throws NoSuchMethodException {
      final Method method = CompilationCache.class.getMethod("getPerformanceMetrics");
      assertNotNull(method, "getPerformanceMetrics method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Control Method Tests")
  class ControlMethodTests {

    @Test
    @DisplayName("should have setEnabled method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      final Method method = CompilationCache.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = CompilationCache.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("should have getCacheDirectory method")
    void shouldHaveGetCacheDirectoryMethod() throws NoSuchMethodException {
      final Method method = CompilationCache.class.getMethod("getCacheDirectory");
      assertNotNull(method, "getCacheDirectory method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }
}
