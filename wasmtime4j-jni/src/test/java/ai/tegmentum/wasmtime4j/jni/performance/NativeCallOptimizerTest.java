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

package ai.tegmentum.wasmtime4j.jni.performance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.performance.NativeCallOptimizer.OptimizationLevel;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link NativeCallOptimizer} class.
 *
 * <p>This test class verifies the NativeCallOptimizer singleton class which provides
 * advanced JNI optimization strategies including call batching, async execution,
 * and thread-local resource pooling.
 */
@DisplayName("NativeCallOptimizer Tests")
class NativeCallOptimizerTest {

  private NativeCallOptimizer optimizer;

  @BeforeEach
  void setUp() {
    optimizer = NativeCallOptimizer.getInstance();
    NativeCallOptimizer.setEnabled(true);
    optimizer.reset();
  }

  @AfterEach
  void tearDown() {
    if (optimizer != null) {
      optimizer.reset();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("NativeCallOptimizer should be final class")
    void shouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(NativeCallOptimizer.class.getModifiers()),
          "NativeCallOptimizer should be final");
    }
  }

  @Nested
  @DisplayName("OptimizationLevel Enum Tests")
  class OptimizationLevelTests {

    @Test
    @DisplayName("Should have NONE value")
    void shouldHaveNoneValue() {
      assertNotNull(OptimizationLevel.valueOf("NONE"), "Should have NONE value");
    }

    @Test
    @DisplayName("Should have FAST_PATH value")
    void shouldHaveFastPathValue() {
      assertNotNull(OptimizationLevel.valueOf("FAST_PATH"), "Should have FAST_PATH value");
    }

    @Test
    @DisplayName("Should have BATCH value")
    void shouldHaveBatchValue() {
      assertNotNull(OptimizationLevel.valueOf("BATCH"), "Should have BATCH value");
    }

    @Test
    @DisplayName("Should have ASYNC value")
    void shouldHaveAsyncValue() {
      assertNotNull(OptimizationLevel.valueOf("ASYNC"), "Should have ASYNC value");
    }

    @Test
    @DisplayName("Should have HYBRID value")
    void shouldHaveHybridValue() {
      assertNotNull(OptimizationLevel.valueOf("HYBRID"), "Should have HYBRID value");
    }

    @Test
    @DisplayName("Should have exactly 5 optimization levels")
    void shouldHaveExactly5OptimizationLevels() {
      assertEquals(5, OptimizationLevel.values().length,
          "Should have exactly 5 optimization levels");
    }

    @Test
    @DisplayName("NONE should be at ordinal 0")
    void noneShouldBeAtOrdinal0() {
      assertEquals(0, OptimizationLevel.NONE.ordinal(), "NONE should be at ordinal 0");
    }

    @Test
    @DisplayName("FAST_PATH should be at ordinal 1")
    void fastPathShouldBeAtOrdinal1() {
      assertEquals(1, OptimizationLevel.FAST_PATH.ordinal(), "FAST_PATH should be at ordinal 1");
    }

    @Test
    @DisplayName("BATCH should be at ordinal 2")
    void batchShouldBeAtOrdinal2() {
      assertEquals(2, OptimizationLevel.BATCH.ordinal(), "BATCH should be at ordinal 2");
    }

    @Test
    @DisplayName("ASYNC should be at ordinal 3")
    void asyncShouldBeAtOrdinal3() {
      assertEquals(3, OptimizationLevel.ASYNC.ordinal(), "ASYNC should be at ordinal 3");
    }

    @Test
    @DisplayName("HYBRID should be at ordinal 4")
    void hybridShouldBeAtOrdinal4() {
      assertEquals(4, OptimizationLevel.HYBRID.ordinal(), "HYBRID should be at ordinal 4");
    }
  }

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("getInstance should return non-null instance")
    void getInstanceShouldReturnNonNullInstance() {
      final NativeCallOptimizer instance = NativeCallOptimizer.getInstance();
      assertNotNull(instance, "getInstance should return non-null instance");
    }

    @Test
    @DisplayName("getInstance should return same instance")
    void getInstanceShouldReturnSameInstance() {
      final NativeCallOptimizer instance1 = NativeCallOptimizer.getInstance();
      final NativeCallOptimizer instance2 = NativeCallOptimizer.getInstance();
      assertSame(instance1, instance2, "getInstance should return same instance");
    }
  }

  @Nested
  @DisplayName("Enabled State Tests")
  class EnabledStateTests {

    @Test
    @DisplayName("isEnabled should return true by default")
    void isEnabledShouldReturnTrueByDefault() {
      assertTrue(NativeCallOptimizer.isEnabled(), "Optimizer should be enabled by default");
    }

    @Test
    @DisplayName("setEnabled should update enabled state")
    void setEnabledShouldUpdateEnabledState() {
      NativeCallOptimizer.setEnabled(false);
      assertFalse(NativeCallOptimizer.isEnabled(), "Optimizer should be disabled");

      NativeCallOptimizer.setEnabled(true);
      assertTrue(NativeCallOptimizer.isEnabled(), "Optimizer should be enabled");
    }
  }

  @Nested
  @DisplayName("optimizeCall Tests")
  class OptimizeCallTests {

    @Test
    @DisplayName("optimizeCall should execute operation and return result")
    void optimizeCallShouldExecuteOperationAndReturnResult() throws Exception {
      final String result = optimizer.optimizeCall("testMethod", new Object[]{},
          () -> "test result");
      assertEquals("test result", result, "Should return operation result");
    }

    @Test
    @DisplayName("optimizeCall should work with null parameters")
    void optimizeCallShouldWorkWithNullParameters() throws Exception {
      final Integer result = optimizer.optimizeCall("testMethod", null, () -> 42);
      assertEquals(42, result, "Should return operation result");
    }

    @Test
    @DisplayName("optimizeCall should propagate exceptions")
    void optimizeCallShouldPropagateExceptions() {
      try {
        optimizer.optimizeCall("testMethod", new Object[]{},
            () -> {
              throw new RuntimeException("Test exception");
            });
        assertTrue(false, "Should have thrown exception");
      } catch (Exception e) {
        assertTrue(e.getMessage().contains("Test exception"),
            "Should propagate exception");
      }
    }

    @Test
    @DisplayName("optimizeCall should bypass optimization when disabled")
    void optimizeCallShouldBypassOptimizationWhenDisabled() throws Exception {
      NativeCallOptimizer.setEnabled(false);
      final String result = optimizer.optimizeCall("testMethod", new Object[]{},
          () -> "direct result");
      assertEquals("direct result", result, "Should return direct result");
    }
  }

  @Nested
  @DisplayName("optimizeBatch Tests")
  class OptimizeBatchTests {

    @Test
    @DisplayName("optimizeBatch should execute all operations")
    @SuppressWarnings("unchecked")
    void optimizeBatchShouldExecuteAllOperations() throws Exception {
      final NativeCallOptimizer.OptimizedOperation<String>[] operations =
          new NativeCallOptimizer.OptimizedOperation[3];
      operations[0] = () -> "result1";
      operations[1] = () -> "result2";
      operations[2] = () -> "result3";

      final String[] results = optimizer.optimizeBatch("testMethod", operations,
          ops -> new String[]{"result1", "result2", "result3"});

      assertNotNull(results, "Results should not be null");
      assertEquals(3, results.length, "Should have 3 results");
    }

    @Test
    @DisplayName("optimizeBatch should handle null operations")
    void optimizeBatchShouldHandleNullOperations() throws Exception {
      final Object[] results = optimizer.optimizeBatch("testMethod", null, ops -> new Object[0]);
      assertNotNull(results, "Results should not be null");
      assertEquals(0, results.length, "Should have 0 results");
    }

    @Test
    @DisplayName("optimizeBatch should handle empty operations")
    @SuppressWarnings("unchecked")
    void optimizeBatchShouldHandleEmptyOperations() throws Exception {
      final NativeCallOptimizer.OptimizedOperation<String>[] operations =
          new NativeCallOptimizer.OptimizedOperation[0];

      final String[] results = optimizer.optimizeBatch("testMethod", operations,
          ops -> new String[0]);

      assertNotNull(results, "Results should not be null");
      assertEquals(0, results.length, "Should have 0 results");
    }
  }

  @Nested
  @DisplayName("optimizeAsync Tests")
  class OptimizeAsyncTests {

    @Test
    @DisplayName("optimizeAsync should return Future")
    void optimizeAsyncShouldReturnFuture() {
      final Future<String> future = optimizer.optimizeAsync("testMethod",
          () -> "async result");
      assertNotNull(future, "Should return non-null Future");
    }

    @Test
    @DisplayName("optimizeAsync result should be obtainable")
    void optimizeAsyncResultShouldBeObtainable() throws Exception {
      final Future<String> future = optimizer.optimizeAsync("testMethod",
          () -> "async result");
      final String result = future.get();
      assertEquals("async result", result, "Should get correct result");
    }
  }

  @Nested
  @DisplayName("Buffer Optimization Tests")
  class BufferOptimizationTests {

    @Test
    @DisplayName("getOptimizedBuffer should return non-null buffer")
    void getOptimizedBufferShouldReturnNonNullBuffer() {
      final ByteBuffer buffer = optimizer.getOptimizedBuffer(100);
      assertNotNull(buffer, "Buffer should not be null");
      assertTrue(buffer.capacity() >= 100, "Buffer capacity should be >= requested size");
    }

    @Test
    @DisplayName("returnOptimizedBuffer should not throw for valid buffer")
    void returnOptimizedBufferShouldNotThrowForValidBuffer() {
      final ByteBuffer buffer = optimizer.getOptimizedBuffer(100);
      assertDoesNotThrow(() -> optimizer.returnOptimizedBuffer(buffer),
          "Should not throw for valid buffer");
    }

    @Test
    @DisplayName("returnOptimizedBuffer should not throw for null buffer")
    void returnOptimizedBufferShouldNotThrowForNullBuffer() {
      assertDoesNotThrow(() -> optimizer.returnOptimizedBuffer(null),
          "Should not throw for null buffer");
    }

    @Test
    @DisplayName("getOptimizedBuffer should return heap buffer when disabled")
    void getOptimizedBufferShouldReturnHeapBufferWhenDisabled() {
      NativeCallOptimizer.setEnabled(false);
      final ByteBuffer buffer = optimizer.getOptimizedBuffer(100);
      assertNotNull(buffer, "Buffer should not be null");
      assertFalse(buffer.isDirect(), "Should return heap buffer when disabled");
    }
  }

  @Nested
  @DisplayName("executeWithOptimizedMemory Tests")
  class ExecuteWithOptimizedMemoryTests {

    @Test
    @DisplayName("executeWithOptimizedMemory should provide buffer to operation")
    void executeWithOptimizedMemoryShouldProvideBufferToOperation() throws Exception {
      final Integer result = optimizer.executeWithOptimizedMemory(100, buffer -> {
        assertNotNull(buffer, "Buffer should be provided");
        assertTrue(buffer.capacity() >= 100, "Buffer capacity should be sufficient");
        return 42;
      });
      assertEquals(42, result, "Should return operation result");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getStatistics should return formatted string when enabled")
    void getStatisticsShouldReturnFormattedStringWhenEnabled() {
      final String stats = optimizer.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.contains("Native Call Optimization"),
          "Statistics should contain header");
    }

    @Test
    @DisplayName("getStatistics should indicate disabled when disabled")
    void getStatisticsShouldIndicateDisabledWhenDisabled() {
      NativeCallOptimizer.setEnabled(false);
      final String stats = optimizer.getStatistics();
      assertTrue(stats.contains("disabled"), "Should indicate disabled state");
    }

    @Test
    @DisplayName("getTotalTimeSavedNs should return non-negative value")
    void getTotalTimeSavedNsShouldReturnNonNegativeValue() {
      assertTrue(optimizer.getTotalTimeSavedNs() >= 0,
          "Total time saved should be non-negative");
    }

    @Test
    @DisplayName("getOptimizationEffectiveness should return value between 0 and 1")
    void getOptimizationEffectivenessShouldReturnValueBetween0And1() {
      final double effectiveness = optimizer.getOptimizationEffectiveness();
      assertTrue(effectiveness >= 0.0 && effectiveness <= 1.0,
          "Effectiveness should be between 0 and 1");
    }
  }

  @Nested
  @DisplayName("Reset Tests")
  class ResetTests {

    @Test
    @DisplayName("reset should clear statistics")
    void resetShouldClearStatistics() throws Exception {
      // Generate some statistics
      optimizer.optimizeCall("testMethod", new Object[]{}, () -> "result");

      optimizer.reset();

      assertEquals(0, optimizer.getTotalTimeSavedNs(),
          "Time saved should be 0 after reset");
    }

    @Test
    @DisplayName("reset should not throw")
    void resetShouldNotThrow() {
      assertDoesNotThrow(() -> optimizer.reset(),
          "reset should not throw");
    }
  }

  @Nested
  @DisplayName("Shutdown Tests")
  class ShutdownTests {

    @Test
    @DisplayName("shutdown should not throw")
    void shutdownShouldNotThrow() {
      assertDoesNotThrow(() -> optimizer.shutdown(),
          "shutdown should not throw");
    }
  }

  @Nested
  @DisplayName("OptimizedOperation Interface Tests")
  class OptimizedOperationTests {

    @Test
    @DisplayName("OptimizedOperation should be functional interface")
    void shouldBeFunctionalInterface() {
      assertTrue(NativeCallOptimizer.OptimizedOperation.class
              .isAnnotationPresent(FunctionalInterface.class),
          "OptimizedOperation should be annotated with @FunctionalInterface");
    }

    @Test
    @DisplayName("OptimizedOperation should work with lambda")
    void shouldWorkWithLambda() throws Exception {
      final NativeCallOptimizer.OptimizedOperation<String> op = () -> "lambda result";
      assertEquals("lambda result", op.execute(), "Lambda should execute correctly");
    }

    @Test
    @DisplayName("OptimizedOperation should work with method reference")
    void shouldWorkWithMethodReference() throws Exception {
      final NativeCallOptimizer.OptimizedOperation<String> op = this::helperMethod;
      assertEquals("method reference result", op.execute(),
          "Method reference should execute correctly");
    }

    private String helperMethod() {
      return "method reference result";
    }
  }

  @Nested
  @DisplayName("BatchOperation Interface Tests")
  class BatchOperationTests {

    @Test
    @DisplayName("BatchOperation should be functional interface")
    void shouldBeFunctionalInterface() {
      assertTrue(NativeCallOptimizer.BatchOperation.class
              .isAnnotationPresent(FunctionalInterface.class),
          "BatchOperation should be annotated with @FunctionalInterface");
    }

    @Test
    @DisplayName("BatchOperation should work with lambda")
    void shouldWorkWithLambda() throws Exception {
      final NativeCallOptimizer.BatchOperation<String> op = ops -> new String[]{"result"};
      final String[] results = op.execute(new Object[]{});
      assertEquals(1, results.length, "Lambda should execute correctly");
      assertEquals("result", results[0], "Lambda should return correct result");
    }
  }

  @Nested
  @DisplayName("Call Pattern Analysis Tests")
  class CallPatternAnalysisTests {

    @Test
    @DisplayName("Multiple calls should be tracked for optimization")
    void multipleCallsShouldBeTrackedForOptimization() throws Exception {
      // Make several calls to build up pattern data
      for (int i = 0; i < 15; i++) {
        optimizer.optimizeCall("frequentMethod", new Object[]{1, 2},
            () -> "result");
      }

      final String stats = optimizer.getStatistics();
      assertTrue(stats.contains("frequentMethod") || stats.contains("optimized"),
          "Statistics should track frequent methods");
    }
  }
}
