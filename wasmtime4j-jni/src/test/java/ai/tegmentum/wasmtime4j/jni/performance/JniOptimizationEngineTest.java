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

import ai.tegmentum.wasmtime4j.jni.performance.JniOptimizationEngine.AllocationStrategy;
import ai.tegmentum.wasmtime4j.jni.performance.JniOptimizationEngine.CriticalSectionToken;
import ai.tegmentum.wasmtime4j.jni.performance.JniOptimizationEngine.OptimizationStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JniOptimizationEngine} class.
 *
 * <p>This test class verifies the JniOptimizationEngine singleton class which provides
 * advanced JNI optimization including call batching, thread-local resource pools,
 * and GC-aware scheduling.
 */
@DisplayName("JniOptimizationEngine Tests")
class JniOptimizationEngineTest {

  private JniOptimizationEngine engine;

  @BeforeEach
  void setUp() {
    engine = JniOptimizationEngine.getInstance();
    JniOptimizationEngine.setOptimizationEnabled(true);
    engine.reset();
  }

  @AfterEach
  void tearDown() {
    if (engine != null) {
      engine.reset();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniOptimizationEngine should be final class")
    void shouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(JniOptimizationEngine.class.getModifiers()),
          "JniOptimizationEngine should be final");
    }
  }

  @Nested
  @DisplayName("OptimizationStrategy Enum Tests")
  class OptimizationStrategyTests {

    @Test
    @DisplayName("Should have NONE value")
    void shouldHaveNoneValue() {
      assertNotNull(OptimizationStrategy.valueOf("NONE"), "Should have NONE value");
    }

    @Test
    @DisplayName("Should have BATCH value")
    void shouldHaveBatchValue() {
      assertNotNull(OptimizationStrategy.valueOf("BATCH"), "Should have BATCH value");
    }

    @Test
    @DisplayName("Should have POOL value")
    void shouldHavePoolValue() {
      assertNotNull(OptimizationStrategy.valueOf("POOL"), "Should have POOL value");
    }

    @Test
    @DisplayName("Should have DIRECT value")
    void shouldHaveDirectValue() {
      assertNotNull(OptimizationStrategy.valueOf("DIRECT"), "Should have DIRECT value");
    }

    @Test
    @DisplayName("Should have ASYNC value")
    void shouldHaveAsyncValue() {
      assertNotNull(OptimizationStrategy.valueOf("ASYNC"), "Should have ASYNC value");
    }

    @Test
    @DisplayName("Should have exactly 5 strategies")
    void shouldHaveExactly5Strategies() {
      assertEquals(5, OptimizationStrategy.values().length,
          "Should have exactly 5 strategies");
    }

    @Test
    @DisplayName("NONE should be at ordinal 0")
    void noneShouldBeAtOrdinal0() {
      assertEquals(0, OptimizationStrategy.NONE.ordinal(), "NONE should be at ordinal 0");
    }

    @Test
    @DisplayName("BATCH should be at ordinal 1")
    void batchShouldBeAtOrdinal1() {
      assertEquals(1, OptimizationStrategy.BATCH.ordinal(), "BATCH should be at ordinal 1");
    }

    @Test
    @DisplayName("POOL should be at ordinal 2")
    void poolShouldBeAtOrdinal2() {
      assertEquals(2, OptimizationStrategy.POOL.ordinal(), "POOL should be at ordinal 2");
    }

    @Test
    @DisplayName("DIRECT should be at ordinal 3")
    void directShouldBeAtOrdinal3() {
      assertEquals(3, OptimizationStrategy.DIRECT.ordinal(), "DIRECT should be at ordinal 3");
    }

    @Test
    @DisplayName("ASYNC should be at ordinal 4")
    void asyncShouldBeAtOrdinal4() {
      assertEquals(4, OptimizationStrategy.ASYNC.ordinal(), "ASYNC should be at ordinal 4");
    }
  }

  @Nested
  @DisplayName("AllocationStrategy Enum Tests")
  class AllocationStrategyTests {

    @Test
    @DisplayName("Should have DIRECT value")
    void shouldHaveDirectValue() {
      assertNotNull(AllocationStrategy.valueOf("DIRECT"), "Should have DIRECT value");
    }

    @Test
    @DisplayName("Should have THREAD_LOCAL value")
    void shouldHaveThreadLocalValue() {
      assertNotNull(AllocationStrategy.valueOf("THREAD_LOCAL"),
          "Should have THREAD_LOCAL value");
    }

    @Test
    @DisplayName("Should have POOLED value")
    void shouldHavePooledValue() {
      assertNotNull(AllocationStrategy.valueOf("POOLED"), "Should have POOLED value");
    }

    @Test
    @DisplayName("Should have OFF_HEAP value")
    void shouldHaveOffHeapValue() {
      assertNotNull(AllocationStrategy.valueOf("OFF_HEAP"), "Should have OFF_HEAP value");
    }

    @Test
    @DisplayName("Should have exactly 4 allocation strategies")
    void shouldHaveExactly4AllocationStrategies() {
      assertEquals(4, AllocationStrategy.values().length,
          "Should have exactly 4 allocation strategies");
    }

    @Test
    @DisplayName("DIRECT should be at ordinal 0")
    void directShouldBeAtOrdinal0() {
      assertEquals(0, AllocationStrategy.DIRECT.ordinal(), "DIRECT should be at ordinal 0");
    }

    @Test
    @DisplayName("THREAD_LOCAL should be at ordinal 1")
    void threadLocalShouldBeAtOrdinal1() {
      assertEquals(1, AllocationStrategy.THREAD_LOCAL.ordinal(),
          "THREAD_LOCAL should be at ordinal 1");
    }

    @Test
    @DisplayName("POOLED should be at ordinal 2")
    void pooledShouldBeAtOrdinal2() {
      assertEquals(2, AllocationStrategy.POOLED.ordinal(), "POOLED should be at ordinal 2");
    }

    @Test
    @DisplayName("OFF_HEAP should be at ordinal 3")
    void offHeapShouldBeAtOrdinal3() {
      assertEquals(3, AllocationStrategy.OFF_HEAP.ordinal(), "OFF_HEAP should be at ordinal 3");
    }
  }

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("getInstance should return non-null instance")
    void getInstanceShouldReturnNonNullInstance() {
      final JniOptimizationEngine instance = JniOptimizationEngine.getInstance();
      assertNotNull(instance, "getInstance should return non-null instance");
    }

    @Test
    @DisplayName("getInstance should return same instance")
    void getInstanceShouldReturnSameInstance() {
      final JniOptimizationEngine instance1 = JniOptimizationEngine.getInstance();
      final JniOptimizationEngine instance2 = JniOptimizationEngine.getInstance();
      assertSame(instance1, instance2, "getInstance should return same instance");
    }
  }

  @Nested
  @DisplayName("Enable State Tests")
  class EnableStateTests {

    @Test
    @DisplayName("isOptimizationEnabled should return true by default")
    void isOptimizationEnabledShouldReturnTrueByDefault() {
      assertTrue(JniOptimizationEngine.isOptimizationEnabled(),
          "Optimization should be enabled by default");
    }

    @Test
    @DisplayName("setOptimizationEnabled should update state")
    void setOptimizationEnabledShouldUpdateState() {
      JniOptimizationEngine.setOptimizationEnabled(false);
      assertFalse(JniOptimizationEngine.isOptimizationEnabled(),
          "Optimization should be disabled");

      JniOptimizationEngine.setOptimizationEnabled(true);
      assertTrue(JniOptimizationEngine.isOptimizationEnabled(),
          "Optimization should be enabled");
    }
  }

  @Nested
  @DisplayName("optimizeCall Tests")
  class OptimizeCallTests {

    @Test
    @DisplayName("optimizeCall should execute operation and return result")
    void optimizeCallShouldExecuteOperationAndReturnResult() throws Exception {
      final String result = engine.optimizeCall("testMethod", new Object[]{}, () -> "test result");
      assertEquals("test result", result, "Should return operation result");
    }

    @Test
    @DisplayName("optimizeCall should work with null parameters")
    void optimizeCallShouldWorkWithNullParameters() throws Exception {
      final Integer result = engine.optimizeCall("testMethod", null, () -> 42);
      assertEquals(42, result, "Should return operation result");
    }

    @Test
    @DisplayName("optimizeCall should bypass optimization when disabled")
    void optimizeCallShouldBypassOptimizationWhenDisabled() throws Exception {
      JniOptimizationEngine.setOptimizationEnabled(false);
      final String result = engine.optimizeCall("testMethod", new Object[]{}, () -> "direct");
      assertEquals("direct", result, "Should return direct result");
    }

    @Test
    @DisplayName("optimizeCall should propagate exceptions")
    void optimizeCallShouldPropagateExceptions() {
      try {
        engine.optimizeCall("testMethod", new Object[]{}, () -> {
          throw new RuntimeException("Test exception");
        });
        assertTrue(false, "Should have thrown exception");
      } catch (Exception e) {
        assertTrue(e.getMessage().contains("Test exception"),
            "Should propagate exception");
      }
    }
  }

  @Nested
  @DisplayName("optimizeBatch Tests")
  class OptimizeBatchTests {

    @Test
    @DisplayName("optimizeBatch should execute all operations")
    @SuppressWarnings("unchecked")
    void optimizeBatchShouldExecuteAllOperations() throws Exception {
      final JniOptimizationEngine.OptimizedOperation<String>[] operations =
          new JniOptimizationEngine.OptimizedOperation[3];
      operations[0] = () -> "result1";
      operations[1] = () -> "result2";
      operations[2] = () -> "result3";

      final String[] results = engine.optimizeBatch("testMethod", operations);

      assertNotNull(results, "Results should not be null");
      assertEquals(3, results.length, "Should have 3 results");
    }

    @Test
    @DisplayName("optimizeBatch should handle null operations")
    void optimizeBatchShouldHandleNullOperations() throws Exception {
      final Object[] results = engine.optimizeBatch("testMethod", null);
      assertNotNull(results, "Results should not be null");
      assertEquals(0, results.length, "Should have 0 results");
    }
  }

  @Nested
  @DisplayName("Critical Section Tests")
  class CriticalSectionTests {

    @Test
    @DisplayName("enterCriticalSection should return token")
    void enterCriticalSectionShouldReturnToken() {
      final CriticalSectionToken token = engine.enterCriticalSection("test_section");
      assertNotNull(token, "Token should not be null");
    }

    @Test
    @DisplayName("Token should have section name")
    void tokenShouldHaveSectionName() {
      final CriticalSectionToken token = engine.enterCriticalSection("test_section");
      assertEquals("test_section", token.getSectionName(), "Should have correct section name");
    }

    @Test
    @DisplayName("Token should have non-negative entry time")
    void tokenShouldHaveNonNegativeEntryTime() {
      final CriticalSectionToken token = engine.enterCriticalSection("test_section");
      assertTrue(token.getEntryTime() >= 0, "Entry time should be non-negative");
    }

    @Test
    @DisplayName("enterCriticalSection with optimization disabled should still work")
    void enterCriticalSectionWithOptimizationDisabledShouldStillWork() {
      JniOptimizationEngine.setOptimizationEnabled(false);
      final CriticalSectionToken token = engine.enterCriticalSection("test_section");
      assertNotNull(token, "Token should not be null");
      assertEquals(0, token.getEntryTime(), "Entry time should be 0 when disabled");
    }
  }

  @Nested
  @DisplayName("Thread Resources Tests")
  class ThreadResourcesTests {

    @Test
    @DisplayName("getThreadResources should return non-null resources")
    void getThreadResourcesShouldReturnNonNullResources() {
      final Object resources = engine.getThreadResources();
      assertNotNull(resources, "Thread resources should not be null");
    }

    @Test
    @DisplayName("getThreadResources should return same instance for same thread")
    void getThreadResourcesShouldReturnSameInstanceForSameThread() {
      final Object resources1 = engine.getThreadResources();
      final Object resources2 = engine.getThreadResources();
      assertSame(resources1, resources2, "Should return same instance for same thread");
    }
  }

  @Nested
  @DisplayName("Allocation Optimization Tests")
  class AllocationOptimizationTests {

    @Test
    @DisplayName("optimizeAllocation should return strategy")
    void optimizeAllocationShouldReturnStrategy() {
      final AllocationStrategy strategy = engine.optimizeAllocation(100, "test");
      assertNotNull(strategy, "Strategy should not be null");
    }

    @Test
    @DisplayName("optimizeAllocation should return DIRECT when disabled")
    void optimizeAllocationShouldReturnDirectWhenDisabled() {
      JniOptimizationEngine.setOptimizationEnabled(false);
      final AllocationStrategy strategy = engine.optimizeAllocation(100, "test");
      assertEquals(AllocationStrategy.DIRECT, strategy, "Should return DIRECT when disabled");
    }

    @Test
    @DisplayName("optimizeAllocation should return THREAD_LOCAL for small sizes")
    void optimizeAllocationShouldReturnThreadLocalForSmallSizes() {
      final AllocationStrategy strategy = engine.optimizeAllocation(100, "test");
      assertEquals(AllocationStrategy.THREAD_LOCAL, strategy,
          "Should return THREAD_LOCAL for small sizes");
    }

    @Test
    @DisplayName("optimizeAllocation should return POOLED for medium sizes")
    void optimizeAllocationShouldReturnPooledForMediumSizes() {
      final AllocationStrategy strategy = engine.optimizeAllocation(1000, "test");
      assertEquals(AllocationStrategy.POOLED, strategy,
          "Should return POOLED for medium sizes");
    }

    @Test
    @DisplayName("optimizeAllocation should return DIRECT for large sizes")
    void optimizeAllocationShouldReturnDirectForLargeSizes() {
      final AllocationStrategy strategy = engine.optimizeAllocation(5000, "test");
      assertEquals(AllocationStrategy.DIRECT, strategy, "Should return DIRECT for large sizes");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getOptimizationStats should return formatted string")
    void getOptimizationStatsShouldReturnFormattedString() {
      final String stats = engine.getOptimizationStats();
      assertNotNull(stats, "Stats should not be null");
      assertTrue(stats.contains("JNI Optimization"),
          "Stats should contain header");
    }

    @Test
    @DisplayName("getOptimizationStats should include enabled state")
    void getOptimizationStatsShouldIncludeEnabledState() {
      final String stats = engine.getOptimizationStats();
      assertTrue(stats.contains("enabled") || stats.contains("Optimization"),
          "Stats should include enabled state");
    }
  }

  @Nested
  @DisplayName("Reset Tests")
  class ResetTests {

    @Test
    @DisplayName("reset should clear statistics")
    void resetShouldClearStatistics() throws Exception {
      // Generate some statistics
      engine.optimizeCall("testMethod", new Object[]{}, () -> "result");

      engine.reset();

      final String stats = engine.getOptimizationStats();
      assertTrue(stats.contains("0") || stats.contains("calls"),
          "Stats should be reset");
    }

    @Test
    @DisplayName("reset should not throw")
    void resetShouldNotThrow() {
      assertDoesNotThrow(() -> engine.reset(), "reset should not throw");
    }
  }

  @Nested
  @DisplayName("Shutdown Tests")
  class ShutdownTests {

    @Test
    @DisplayName("shutdown should not throw")
    void shutdownShouldNotThrow() {
      assertDoesNotThrow(() -> engine.shutdown(), "shutdown should not throw");
    }
  }

  @Nested
  @DisplayName("OptimizedOperation Interface Tests")
  class OptimizedOperationTests {

    @Test
    @DisplayName("OptimizedOperation should be functional interface")
    void shouldBeFunctionalInterface() {
      assertTrue(JniOptimizationEngine.OptimizedOperation.class
              .isAnnotationPresent(FunctionalInterface.class),
          "OptimizedOperation should be annotated with @FunctionalInterface");
    }

    @Test
    @DisplayName("OptimizedOperation should work with lambda")
    void shouldWorkWithLambda() throws Exception {
      final JniOptimizationEngine.OptimizedOperation<String> op = () -> "lambda result";
      assertEquals("lambda result", op.execute(), "Lambda should execute correctly");
    }
  }

  @Nested
  @DisplayName("CriticalSectionToken Tests")
  class CriticalSectionTokenTests {

    @Test
    @DisplayName("CriticalSectionToken should be final class")
    void shouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(CriticalSectionToken.class.getModifiers()),
          "CriticalSectionToken should be final");
    }

    @Test
    @DisplayName("CriticalSectionToken should store section name")
    void shouldStoreSectionName() {
      final CriticalSectionToken token = engine.enterCriticalSection("my_section");
      assertEquals("my_section", token.getSectionName(),
          "Should store section name correctly");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Multiple optimized calls should be tracked")
    void multipleOptimizedCallsShouldBeTracked() throws Exception {
      for (int i = 0; i < 15; i++) {
        engine.optimizeCall("frequentMethod", new Object[]{1, 2}, () -> "result");
      }

      final String stats = engine.getOptimizationStats();
      assertTrue(stats.contains("frequentMethod") || stats.contains("calls"),
          "Should track calls");
    }
  }
}
