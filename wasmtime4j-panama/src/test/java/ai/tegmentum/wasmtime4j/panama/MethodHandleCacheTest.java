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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MethodHandleCache} class.
 *
 * <p>MethodHandleCache provides caching for method handles to optimize repeated FFI calls.
 */
@DisplayName("MethodHandleCache Tests")
class MethodHandleCacheTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(MethodHandleCache.class.getModifiers()),
          "MethodHandleCache should be public");
      assertTrue(
          Modifier.isFinal(MethodHandleCache.class.getModifiers()),
          "MethodHandleCache should be final");
    }

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = MethodHandleCache.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with max size and statistics flag")
    void shouldHaveConstructorWithMaxSizeAndStats() throws NoSuchMethodException {
      final Constructor<?> constructor =
          MethodHandleCache.class.getConstructor(int.class, boolean.class);
      assertNotNull(constructor, "Constructor with max size and stats should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Cache Operation Method Tests")
  class CacheOperationMethodTests {

    @Test
    @DisplayName("should have getOrCreate method")
    void shouldHaveGetOrCreateMethod() throws NoSuchMethodException {
      final Method method =
          MethodHandleCache.class.getMethod(
              "getOrCreate", String.class, MemorySegment.class, FunctionDescriptor.class);
      assertNotNull(method, "getOrCreate method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getOrCreate should return Optional");
    }

    @Test
    @DisplayName("should have get method")
    void shouldHaveGetMethod() throws NoSuchMethodException {
      final Method method =
          MethodHandleCache.class.getMethod("get", String.class, FunctionDescriptor.class);
      assertNotNull(method, "get method should exist");
      assertEquals(Optional.class, method.getReturnType(), "get should return Optional");
    }

    @Test
    @DisplayName("should have invalidate method")
    void shouldHaveInvalidateMethod() throws NoSuchMethodException {
      final Method method =
          MethodHandleCache.class.getMethod("invalidate", String.class, FunctionDescriptor.class);
      assertNotNull(method, "invalidate method should exist");
      assertEquals(boolean.class, method.getReturnType(), "invalidate should return boolean");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = MethodHandleCache.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "clear should return void");
    }
  }

  @Nested
  @DisplayName("Cache State Method Tests")
  class CacheStateMethodTests {

    @Test
    @DisplayName("should have size method")
    void shouldHaveSizeMethod() throws NoSuchMethodException {
      final Method method = MethodHandleCache.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertEquals(int.class, method.getReturnType(), "size should return int");
    }

    @Test
    @DisplayName("should have getMaxSize method")
    void shouldHaveGetMaxSizeMethod() throws NoSuchMethodException {
      final Method method = MethodHandleCache.class.getMethod("getMaxSize");
      assertNotNull(method, "getMaxSize method should exist");
      assertEquals(int.class, method.getReturnType(), "getMaxSize should return int");
    }

    @Test
    @DisplayName("should have isEmpty method")
    void shouldHaveIsEmptyMethod() throws NoSuchMethodException {
      final Method method = MethodHandleCache.class.getMethod("isEmpty");
      assertNotNull(method, "isEmpty method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isEmpty should return boolean");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = MethodHandleCache.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getStatistics should return Optional");
    }

    @Test
    @DisplayName("should have resetStatistics method")
    void shouldHaveResetStatisticsMethod() throws NoSuchMethodException {
      final Method method = MethodHandleCache.class.getMethod("resetStatistics");
      assertNotNull(method, "resetStatistics method should exist");
      assertEquals(void.class, method.getReturnType(), "resetStatistics should return void");
    }
  }

  @Nested
  @DisplayName("Nested Class Tests")
  class NestedClassTests {

    @Test
    @DisplayName("should have CacheStatistics nested class")
    void shouldHaveCacheStatisticsNestedClass() {
      final Class<?>[] declaredClasses = MethodHandleCache.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("CacheStatistics")) {
          found = true;
          assertTrue(Modifier.isPublic(clazz.getModifiers()), "CacheStatistics should be public");
          assertTrue(Modifier.isFinal(clazz.getModifiers()), "CacheStatistics should be final");
          assertTrue(Modifier.isStatic(clazz.getModifiers()), "CacheStatistics should be static");
          break;
        }
      }
      assertTrue(found, "CacheStatistics nested class should exist");
    }
  }

  @Nested
  @DisplayName("CacheStatistics Method Tests")
  class CacheStatisticsMethodTests {

    @Test
    @DisplayName("CacheStatistics should have getHitCount method")
    void cacheStatisticsShouldHaveGetHitCountMethod() throws NoSuchMethodException {
      final Class<?> statsClass = MethodHandleCache.CacheStatistics.class;
      final Method method = statsClass.getMethod("getHitCount");
      assertNotNull(method, "getHitCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getHitCount should return long");
    }

    @Test
    @DisplayName("CacheStatistics should have getMissCount method")
    void cacheStatisticsShouldHaveGetMissCountMethod() throws NoSuchMethodException {
      final Class<?> statsClass = MethodHandleCache.CacheStatistics.class;
      final Method method = statsClass.getMethod("getMissCount");
      assertNotNull(method, "getMissCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getMissCount should return long");
    }

    @Test
    @DisplayName("CacheStatistics should have getEvictionCount method")
    void cacheStatisticsShouldHaveGetEvictionCountMethod() throws NoSuchMethodException {
      final Class<?> statsClass = MethodHandleCache.CacheStatistics.class;
      final Method method = statsClass.getMethod("getEvictionCount");
      assertNotNull(method, "getEvictionCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getEvictionCount should return long");
    }

    @Test
    @DisplayName("CacheStatistics should have getTotalLoadTime method")
    void cacheStatisticsShouldHaveGetTotalLoadTimeMethod() throws NoSuchMethodException {
      final Class<?> statsClass = MethodHandleCache.CacheStatistics.class;
      final Method method = statsClass.getMethod("getTotalLoadTime");
      assertNotNull(method, "getTotalLoadTime method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalLoadTime should return long");
    }

    @Test
    @DisplayName("CacheStatistics should have getCurrentSize method")
    void cacheStatisticsShouldHaveGetCurrentSizeMethod() throws NoSuchMethodException {
      final Class<?> statsClass = MethodHandleCache.CacheStatistics.class;
      final Method method = statsClass.getMethod("getCurrentSize");
      assertNotNull(method, "getCurrentSize method should exist");
      assertEquals(int.class, method.getReturnType(), "getCurrentSize should return int");
    }

    @Test
    @DisplayName("CacheStatistics should have getHitRate method")
    void cacheStatisticsShouldHaveGetHitRateMethod() throws NoSuchMethodException {
      final Class<?> statsClass = MethodHandleCache.CacheStatistics.class;
      final Method method = statsClass.getMethod("getHitRate");
      assertNotNull(method, "getHitRate method should exist");
      assertEquals(double.class, method.getReturnType(), "getHitRate should return double");
    }

    @Test
    @DisplayName("CacheStatistics should have getAverageLoadTime method")
    void cacheStatisticsShouldHaveGetAverageLoadTimeMethod() throws NoSuchMethodException {
      final Class<?> statsClass = MethodHandleCache.CacheStatistics.class;
      final Method method = statsClass.getMethod("getAverageLoadTime");
      assertNotNull(method, "getAverageLoadTime method should exist");
      assertEquals(double.class, method.getReturnType(), "getAverageLoadTime should return double");
    }
  }
}
