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

package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaBatchProcessor} class.
 *
 * <p>PanamaBatchProcessor provides Panama-optimized batch processing utilities.
 */
@DisplayName("PanamaBatchProcessor Tests")
class PanamaBatchProcessorTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaBatchProcessor.class.getModifiers()),
          "PanamaBatchProcessor should be public");
      assertTrue(
          Modifier.isFinal(PanamaBatchProcessor.class.getModifiers()),
          "PanamaBatchProcessor should be final");
    }

    @Test
    @DisplayName("should have DEFAULT_BATCH_SIZE constant")
    void shouldHaveDefaultBatchSizeConstant() throws NoSuchFieldException {
      final Field field = PanamaBatchProcessor.class.getField("DEFAULT_BATCH_SIZE");
      assertNotNull(field, "DEFAULT_BATCH_SIZE should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int");
    }

    @Test
    @DisplayName("should have MAX_BATCH_SIZE constant")
    void shouldHaveMaxBatchSizeConstant() throws NoSuchFieldException {
      final Field field = PanamaBatchProcessor.class.getField("MAX_BATCH_SIZE");
      assertNotNull(field, "MAX_BATCH_SIZE should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      var constructor = PanamaBatchProcessor.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
    }

    @Test
    @DisplayName("should have constructor with Arena parameter")
    void shouldHaveArenaConstructor() throws NoSuchMethodException {
      var constructor = PanamaBatchProcessor.class.getConstructor(Arena.class);
      assertNotNull(constructor, "Arena constructor should exist");
    }

    @Test
    @DisplayName("should have constructor with long parameter")
    void shouldHaveLongConstructor() throws NoSuchMethodException {
      var constructor = PanamaBatchProcessor.class.getConstructor(long.class);
      assertNotNull(constructor, "long constructor should exist");
    }
  }

  @Nested
  @DisplayName("Batch Processing Method Tests")
  class BatchProcessingMethodTests {

    @Test
    @DisplayName("should have processBatch method with batch size")
    void shouldHaveProcessBatchWithSizeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaBatchProcessor.class.getMethod(
              "processBatch", Collection.class, Function.class, int.class);
      assertNotNull(method, "processBatch method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have processBatch method without batch size")
    void shouldHaveProcessBatchDefaultMethod() throws NoSuchMethodException {
      final Method method =
          PanamaBatchProcessor.class.getMethod("processBatch", Collection.class, Function.class);
      assertNotNull(method, "processBatch method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have processBatchAsync method with batch size")
    void shouldHaveProcessBatchAsyncWithSizeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaBatchProcessor.class.getMethod(
              "processBatchAsync", Collection.class, Function.class, int.class);
      assertNotNull(method, "processBatchAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have processBatchAsync method without batch size")
    void shouldHaveProcessBatchAsyncDefaultMethod() throws NoSuchMethodException {
      final Method method =
          PanamaBatchProcessor.class.getMethod(
              "processBatchAsync", Collection.class, Function.class);
      assertNotNull(method, "processBatchAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have processNativeBatch method")
    void shouldHaveProcessNativeBatchMethod() throws NoSuchMethodException {
      final Method method =
          PanamaBatchProcessor.class.getMethod(
              "processNativeBatch", MemorySegment[].class, MethodHandle.class);
      assertNotNull(method, "processNativeBatch method should exist");
      assertEquals(MemorySegment[].class, method.getReturnType(), "Should return MemorySegment[]");
    }
  }

  @Nested
  @DisplayName("Optimization Method Tests")
  class OptimizationMethodTests {

    @Test
    @DisplayName("should have optimizeBatchSize method")
    void shouldHaveOptimizeBatchSizeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaBatchProcessor.class.getMethod("optimizeBatchSize", int.class, int.class);
      assertNotNull(method, "optimizeBatchSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PanamaBatchProcessor.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getPerformanceMetrics method")
    void shouldHaveGetPerformanceMetricsMethod() throws NoSuchMethodException {
      final Method method = PanamaBatchProcessor.class.getMethod("getPerformanceMetrics");
      assertNotNull(method, "getPerformanceMetrics method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have resetStatistics method")
    void shouldHaveResetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PanamaBatchProcessor.class.getMethod("resetStatistics");
      assertNotNull(method, "resetStatistics method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaBatchProcessor.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have isActive method")
    void shouldHaveIsActiveMethod() throws NoSuchMethodException {
      final Method method = PanamaBatchProcessor.class.getMethod("isActive");
      assertNotNull(method, "isActive method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getArena method")
    void shouldHaveGetArenaMethod() throws NoSuchMethodException {
      final Method method = PanamaBatchProcessor.class.getMethod("getArena");
      assertNotNull(method, "getArena method should exist");
      assertEquals(Arena.class, method.getReturnType(), "Should return Arena");
    }
  }
}
