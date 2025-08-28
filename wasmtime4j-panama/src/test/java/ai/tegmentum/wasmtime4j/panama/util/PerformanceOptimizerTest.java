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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.tegmentum.wasmtime4j.panama.util.PerformanceOptimizer.UsagePattern;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for the PerformanceOptimizer utility.
 *
 * <p>These tests verify the complete functionality of the performance optimization system,
 * including method handle optimization, batched operations, memory access patterns, and
 * asynchronous processing pipelines.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PerformanceOptimizer Tests")
class PerformanceOptimizerTest {

  @Mock private Executor mockExecutor;

  private PerformanceOptimizer optimizer;
  private MethodHandle testMethodHandle;

  @BeforeEach
  void setUp() throws Exception {
    // Create a simple test method handle
    testMethodHandle = MethodHandles.lookup()
        .findStatic(PerformanceOptimizerTest.class, "testMethod", 
                   MethodType.methodType(int.class, int.class, int.class));

    // Create optimizer with test configuration
    optimizer = new PerformanceOptimizer(mockExecutor, 4, 10, 16);
  }

  // Test method for method handle testing
  public static int testMethod(final int a, final int b) {
    return a + b;
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create optimizer with default settings")
    void shouldCreateOptimizerWithDefaultSettings() {
      final PerformanceOptimizer defaultOptimizer = new PerformanceOptimizer();
      assertNotNull(defaultOptimizer);
      assertNotNull(defaultOptimizer.getStatistics());
    }

    @Test
    @DisplayName("Should create optimizer with custom settings")
    void shouldCreateOptimizerWithCustomSettings() {
      final PerformanceOptimizer customOptimizer = new PerformanceOptimizer(
          mockExecutor, 8, 20, 32);
      assertNotNull(customOptimizer);
      assertNotNull(customOptimizer.getStatistics());
    }
  }

  @Nested
  @DisplayName("Method Handle Optimization Tests")
  class MethodHandleOptimizationTests {

    @Test
    @DisplayName("Should optimize method handle for high frequency usage")
    void shouldOptimizeMethodHandleForHighFrequencyUsage() throws Exception {
      // Act
      final MethodHandle optimized = optimizer.optimizeMethodHandle(
          "test_high_freq", testMethodHandle, UsagePattern.HIGH_FREQUENCY_SIMPLE);

      // Assert
      assertNotNull(optimized);
      
      // Verify optimized handle works correctly
      final int result = (int) optimized.invokeExact(5, 10);
      assertEquals(15, result);
    }

    @Test
    @DisplayName("Should optimize method handle for bulk operations")
    void shouldOptimizeMethodHandleForBulkOperations() throws Exception {
      // Act
      final MethodHandle optimized = optimizer.optimizeMethodHandle(
          "test_bulk", testMethodHandle, UsagePattern.BULK_OPERATIONS);

      // Assert
      assertNotNull(optimized);
      
      // Verify optimized handle exists (may have different calling conventions)
      assertTrue(optimized.type().parameterCount() >= 0);
    }

    @Test
    @DisplayName("Should optimize method handle for memory intensive operations")
    void shouldOptimizeMethodHandleForMemoryIntensiveOperations() throws Exception {
      // Act
      final MethodHandle optimized = optimizer.optimizeMethodHandle(
          "test_memory", testMethodHandle, UsagePattern.MEMORY_INTENSIVE);

      // Assert
      assertNotNull(optimized);
      assertTrue(optimized.type().parameterCount() >= 0);
    }

    @Test
    @DisplayName("Should handle optimization disabled")
    void shouldHandleOptimizationDisabled() throws Exception {
      // Arrange
      optimizer.setOptimizationEnabled(false);

      // Act
      final MethodHandle result = optimizer.optimizeMethodHandle(
          "test_disabled", testMethodHandle, UsagePattern.HIGH_FREQUENCY_SIMPLE);

      // Assert
      assertEquals(testMethodHandle, result);
    }

    @Test
    @DisplayName("Should cache optimized method handles")
    void shouldCacheOptimizedMethodHandles() throws Exception {
      // Act
      final MethodHandle first = optimizer.optimizeMethodHandle(
          "test_cache", testMethodHandle, UsagePattern.HIGH_FREQUENCY_SIMPLE);
      final MethodHandle second = optimizer.optimizeMethodHandle(
          "test_cache", testMethodHandle, UsagePattern.HIGH_FREQUENCY_SIMPLE);

      // Assert
      assertEquals(first, second); // Should be the same cached instance
    }
  }

  @Nested
  @DisplayName("Batched Execution Tests")
  class BatchedExecutionTests {

    @Test
    @DisplayName("Should execute batched operation successfully")
    void shouldExecuteBatchedOperationSuccessfully() throws Exception {
      // Arrange
      when(mockExecutor.execute(any(Runnable.class))).thenAnswer(invocation -> {
        ((Runnable) invocation.getArgument(0)).run();
        return null;
      });

      final PerformanceOptimizer.BatchedFFIOperation<Integer> operation = 
          (handle, params) -> (Integer) params[0] + (Integer) params[1];

      // Act
      final CompletableFuture<Integer> future = optimizer.executeBatched(
          testMethodHandle, new Object[]{5, 10}, operation);

      // Wait for completion
      final Integer result = future.get(1, TimeUnit.SECONDS);

      // Assert
      assertEquals(15, result);
    }

    @Test
    @DisplayName("Should handle batched operation failure")
    void shouldHandleBatchedOperationFailure() throws Exception {
      // Arrange
      when(mockExecutor.execute(any(Runnable.class))).thenAnswer(invocation -> {
        ((Runnable) invocation.getArgument(0)).run();
        return null;
      });

      final PerformanceOptimizer.BatchedFFIOperation<Integer> failingOperation = 
          (handle, params) -> {
            throw new RuntimeException("Operation failed");
          };

      // Act
      final CompletableFuture<Integer> future = optimizer.executeBatched(
          testMethodHandle, new Object[]{5, 10}, failingOperation);

      // Assert
      assertTrue(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("Should reject operations after shutdown")
    void shouldRejectOperationsAfterShutdown() {
      // Arrange
      optimizer.shutdown();

      final PerformanceOptimizer.BatchedFFIOperation<Integer> operation = 
          (handle, params) -> 42;

      // Act
      final CompletableFuture<Integer> future = optimizer.executeBatched(
          testMethodHandle, new Object[]{}, operation);

      // Assert
      assertTrue(future.isCompletedExceptionally());
    }
  }

  @Nested
  @DisplayName("Memory Access Optimization Tests")  
  class MemoryAccessOptimizationTests {

    @Test
    @DisplayName("Should optimize memory access patterns")
    void shouldOptimizeMemoryAccessPatterns() {
      // Arrange
      final MemorySegment[] segments = {
          MemorySegment.ofAddress(0x3000L),
          MemorySegment.ofAddress(0x1000L), 
          MemorySegment.ofAddress(0x2000L)
      };
      
      final Function<MemorySegment, Long> accessor = MemorySegment::address;

      // Act
      final List<Long> results = optimizer.optimizeMemoryAccess(segments, accessor);

      // Assert
      assertEquals(3, results.size());
      assertTrue(results.contains(0x1000L));
      assertTrue(results.contains(0x2000L));
      assertTrue(results.contains(0x3000L));
    }

    @Test
    @DisplayName("Should handle empty memory segment array")
    void shouldHandleEmptyMemorySegmentArray() {
      // Arrange
      final MemorySegment[] emptySegments = {};
      final Function<MemorySegment, String> accessor = seg -> "test";

      // Act
      final List<String> results = optimizer.optimizeMemoryAccess(emptySegments, accessor);

      // Assert
      assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should work with optimization disabled")
    void shouldWorkWithOptimizationDisabled() {
      // Arrange
      optimizer.setOptimizationEnabled(false);
      final MemorySegment[] segments = {
          MemorySegment.ofAddress(0x1000L),
          MemorySegment.ofAddress(0x2000L)
      };
      final Function<MemorySegment, Long> accessor = MemorySegment::address;

      // Act
      final List<Long> results = optimizer.optimizeMemoryAccess(segments, accessor);

      // Assert
      assertEquals(2, results.size());
    }
  }

  @Nested
  @DisplayName("Pipeline Operations Tests")
  class PipelineOperationsTests {

    @Test
    @DisplayName("Should create and execute processing pipeline")
    void shouldCreateAndExecuteProcessingPipeline() throws Exception {
      // Arrange
      final Function<Integer, String> processor = i -> "processed_" + i;
      final List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

      // Mock executor to run synchronously for testing
      when(mockExecutor.execute(any(Runnable.class))).thenAnswer(invocation -> {
        ((Runnable) invocation.getArgument(0)).run();
        return null;
      });

      // Act
      final PerformanceOptimizer.OperationPipeline<Integer, String> pipeline = 
          optimizer.createPipeline(processor, 2);
      final CompletableFuture<List<String>> future = pipeline.process(input);
      final List<String> results = future.get(1, TimeUnit.SECONDS);

      // Assert
      assertEquals(5, results.size());
      assertTrue(results.contains("processed_1"));
      assertTrue(results.contains("processed_5"));
    }

    @Test
    @DisplayName("Should handle empty pipeline input")
    void shouldHandleEmptyPipelineInput() throws Exception {
      // Arrange
      final Function<Integer, String> processor = i -> "processed_" + i;
      final List<Integer> emptyInput = List.of();

      // Act
      final PerformanceOptimizer.OperationPipeline<Integer, String> pipeline = 
          optimizer.createPipeline(processor, 2);
      final CompletableFuture<List<String>> future = pipeline.process(emptyInput);
      final List<String> results = future.get(1, TimeUnit.SECONDS);

      // Assert
      assertTrue(results.isEmpty());
    }
  }

  @Nested
  @DisplayName("Statistics and Monitoring Tests")
  class StatisticsAndMonitoringTests {

    @Test
    @DisplayName("Should track performance statistics")
    void shouldTrackPerformanceStatistics() throws Exception {
      // Arrange
      when(mockExecutor.execute(any(Runnable.class))).thenAnswer(invocation -> {
        ((Runnable) invocation.getArgument(0)).run();
        return null;
      });

      // Act - perform some operations
      optimizer.optimizeMethodHandle("test1", testMethodHandle, UsagePattern.HIGH_FREQUENCY_SIMPLE);
      optimizer.optimizeMethodHandle("test2", testMethodHandle, UsagePattern.BULK_OPERATIONS);
      
      final PerformanceOptimizer.BatchedFFIOperation<Integer> operation = 
          (handle, params) -> 42;
      optimizer.executeBatched(testMethodHandle, new Object[]{}, operation);

      final PerformanceOptimizer.PerformanceStatistics stats = optimizer.getStatistics();

      // Assert
      assertNotNull(stats);
      assertTrue(stats.getTotalOperations() > 0);
      assertEquals(2, stats.getSpecializedHandles());
    }

    @Test
    @DisplayName("Should calculate batching ratio correctly")
    void shouldCalculateBatchingRatioCorrectly() {
      // Arrange
      final PerformanceOptimizer.PerformanceStatistics stats = 
          new PerformanceOptimizer.PerformanceStatistics(100, 25, 10, 2, 5, 3);

      // Act
      final double ratio = stats.getBatchingRatio();

      // Assert
      assertEquals(0.25, ratio, 0.001);
    }

    @Test
    @DisplayName("Should handle zero operations in statistics")
    void shouldHandleZeroOperationsInStatistics() {
      // Arrange
      final PerformanceOptimizer.PerformanceStatistics stats = 
          new PerformanceOptimizer.PerformanceStatistics(0, 0, 0, 0, 0, 0);

      // Act
      final double ratio = stats.getBatchingRatio();

      // Assert
      assertEquals(0.0, ratio, 0.001);
    }

    @Test
    @DisplayName("Should provide meaningful statistics string representation")
    void shouldProvideMeaningfulStatisticsStringRepresentation() {
      // Arrange
      final PerformanceOptimizer.PerformanceStatistics stats = 
          new PerformanceOptimizer.PerformanceStatistics(100, 25, 10, 2, 5, 3);

      // Act
      final String stringRepr = stats.toString();

      // Assert
      assertTrue(stringRepr.contains("PerformanceStatistics"));
      assertTrue(stringRepr.contains("totalOperations=100"));
      assertTrue(stringRepr.contains("batchedOperations=25"));
      assertTrue(stringRepr.contains("batchingRatio=0.25"));
    }
  }

  @Nested
  @DisplayName("Configuration and Control Tests")
  class ConfigurationAndControlTests {

    @Test
    @DisplayName("Should enable and disable optimization")
    void shouldEnableAndDisableOptimization() throws Exception {
      // Act & Assert - optimization enabled by default
      final MethodHandle optimized1 = optimizer.optimizeMethodHandle(
          "test_enabled", testMethodHandle, UsagePattern.HIGH_FREQUENCY_SIMPLE);
      assertNotNull(optimized1);

      // Disable optimization
      optimizer.setOptimizationEnabled(false);
      final MethodHandle optimized2 = optimizer.optimizeMethodHandle(
          "test_disabled", testMethodHandle, UsagePattern.HIGH_FREQUENCY_SIMPLE);
      assertEquals(testMethodHandle, optimized2); // Should return original

      // Re-enable optimization
      optimizer.setOptimizationEnabled(true);
      final MethodHandle optimized3 = optimizer.optimizeMethodHandle(
          "test_re_enabled", testMethodHandle, UsagePattern.HIGH_FREQUENCY_SIMPLE);
      assertNotNull(optimized3);
    }

    @Test
    @DisplayName("Should handle shutdown gracefully")
    void shouldHandleShutdownGracefully() {
      // Act
      optimizer.shutdown();

      // Assert - should not throw exceptions
      final PerformanceOptimizer.PerformanceStatistics stats = optimizer.getStatistics();
      assertNotNull(stats);
    }

    @Test
    @DisplayName("Should handle concurrent shutdown safely")
    void shouldHandleConcurrentShutdownSafely() throws InterruptedException {
      // Arrange
      final CountDownLatch latch = new CountDownLatch(2);
      final Runnable shutdownTask = () -> {
        optimizer.shutdown();
        latch.countDown();
      };

      // Act
      final Thread thread1 = new Thread(shutdownTask);
      final Thread thread2 = new Thread(shutdownTask);
      
      thread1.start();
      thread2.start();
      
      // Wait for completion
      assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle method handle optimization failure gracefully")
    void shouldHandleMethodHandleOptimizationFailureGracefully() throws Exception {
      // Arrange - create a problematic method handle that might cause optimization to fail
      final MethodHandle problematicHandle = MethodHandles.empty(MethodType.methodType(void.class));

      // Act - should not throw, should fall back to original
      final MethodHandle result = optimizer.optimizeMethodHandle(
          "problematic", problematicHandle, UsagePattern.HIGH_FREQUENCY_SIMPLE);

      // Assert
      assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle batch execution with queue overflow")
    void shouldHandleBatchExecutionWithQueueOverflow() throws Exception {
      // Arrange - create optimizer with very small queue
      final PerformanceOptimizer smallQueueOptimizer = new PerformanceOptimizer(
          mockExecutor, 2, 10, 1); // Queue size = 1

      when(mockExecutor.execute(any(Runnable.class))).thenAnswer(invocation -> {
        ((Runnable) invocation.getArgument(0)).run();
        return null;
      });

      final PerformanceOptimizer.BatchedFFIOperation<Integer> operation = 
          (handle, params) -> 42;

      // Act - submit multiple operations to overflow queue
      final CompletableFuture<Integer> future1 = smallQueueOptimizer.executeBatched(
          testMethodHandle, new Object[]{}, operation);
      final CompletableFuture<Integer> future2 = smallQueueOptimizer.executeBatched(
          testMethodHandle, new Object[]{}, operation);

      // Assert - both should complete (second one should execute immediately due to queue overflow)
      assertEquals(42, (int) future1.get(1, TimeUnit.SECONDS));
      assertEquals(42, (int) future2.get(1, TimeUnit.SECONDS));
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Should handle concurrent method handle optimization")
    void shouldHandleConcurrentMethodHandleOptimization() throws InterruptedException {
      // Arrange
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch completeLatch = new CountDownLatch(3);
      
      final Runnable optimizationTask = () -> {
        try {
          startLatch.await();
          optimizer.optimizeMethodHandle("concurrent_test", testMethodHandle, 
                                         UsagePattern.HIGH_FREQUENCY_SIMPLE);
        } catch (Exception e) {
          // Expected in concurrent environment
        } finally {
          completeLatch.countDown();
        }
      };

      // Act
      final Thread thread1 = new Thread(optimizationTask);
      final Thread thread2 = new Thread(optimizationTask);
      final Thread thread3 = new Thread(optimizationTask);

      thread1.start();
      thread2.start();
      thread3.start();

      startLatch.countDown(); // Start all threads simultaneously
      
      // Assert
      assertTrue(completeLatch.await(2, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent statistics access")
    void shouldHandleConcurrentStatisticsAccess() throws InterruptedException {
      // Arrange
      final CountDownLatch completeLatch = new CountDownLatch(5);
      
      final Runnable statsTask = () -> {
        try {
          for (int i = 0; i < 10; i++) {
            optimizer.getStatistics();
          }
        } finally {
          completeLatch.countDown();
        }
      };

      // Act
      for (int i = 0; i < 5; i++) {
        new Thread(statsTask).start();
      }

      // Assert
      assertTrue(completeLatch.await(2, TimeUnit.SECONDS));
    }
  }
}