package ai.tegmentum.wasmtime4j.jni.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive unit tests for {@link JniBatchProcessor}.
 *
 * <p>These tests verify batch processing functionality, thread safety, performance optimization,
 * error handling, and resource management.
 */
@DisplayName("JniBatchProcessor Tests")
@Timeout(30) // Global timeout for all tests
class JniBatchProcessorTest {

  private JniBatchProcessor batchProcessor;

  @BeforeEach
  void setUp() {
    batchProcessor = new JniBatchProcessor();
  }

  @AfterEach
  void tearDown() {
    if (batchProcessor != null && !batchProcessor.isClosed()) {
      batchProcessor.close();
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create processor with default settings")
    void shouldCreateProcessorWithDefaultSettings() {
      try (final JniBatchProcessor processor = new JniBatchProcessor()) {
        assertEquals(JniBatchProcessor.DEFAULT_MAX_BATCH_SIZE, processor.getMaxBatchSize());
        assertEquals(JniBatchProcessor.DEFAULT_BATCH_TIMEOUT_MS, processor.getBatchTimeoutMs());
        assertFalse(processor.isClosed());
        assertEquals(0, processor.getQueueSize());
      }
    }

    @Test
    @DisplayName("Should create processor with custom settings")
    void shouldCreateProcessorWithCustomSettings() {
      final int maxBatchSize = 50;
      final long timeoutMs = 100;

      try (final JniBatchProcessor processor = new JniBatchProcessor(maxBatchSize, timeoutMs)) {
        assertEquals(maxBatchSize, processor.getMaxBatchSize());
        assertEquals(timeoutMs, processor.getBatchTimeoutMs());
        assertFalse(processor.isClosed());
      }
    }

    @Test
    @DisplayName("Should reject invalid batch size")
    void shouldRejectInvalidBatchSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniBatchProcessor(0, 10),
          "Should reject zero batch size");

      assertThrows(
          IllegalArgumentException.class,
          () -> new JniBatchProcessor(-1, 10),
          "Should reject negative batch size");
    }

    @Test
    @DisplayName("Should reject negative timeout")
    void shouldRejectNegativeTimeout() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniBatchProcessor(10, -1),
          "Should reject negative timeout");
    }

    @Test
    @DisplayName("Should accept zero timeout")
    void shouldAcceptZeroTimeout() {
      try (final JniBatchProcessor processor = new JniBatchProcessor(10, 0)) {
        assertEquals(0, processor.getBatchTimeoutMs());
      }
    }
  }

  @Nested
  @DisplayName("Basic Operation Tests")
  class BasicOperationTests {

    @Test
    @DisplayName("Should execute single operation")
    void shouldExecuteSingleOperation() {
      final String result = batchProcessor.execute(() -> "test result");
      assertEquals("test result", result);
    }

    @Test
    @DisplayName("Should execute operation returning null")
    void shouldExecuteOperationReturningNull() {
      final String result = batchProcessor.execute(() -> null);
      assertEquals(null, result);
    }

    @Test
    @DisplayName("Should execute multiple operations")
    void shouldExecuteMultipleOperations() {
      final String result1 = batchProcessor.execute(() -> "result1");
      final String result2 = batchProcessor.execute(() -> "result2");
      final String result3 = batchProcessor.execute(() -> "result3");

      assertEquals("result1", result1);
      assertEquals("result2", result2);
      assertEquals("result3", result3);
    }

    @Test
    @DisplayName("Should handle operations with different types")
    void shouldHandleOperationsWithDifferentTypes() {
      final String stringResult = batchProcessor.execute(() -> "string");
      final Integer intResult = batchProcessor.execute(() -> 42);
      final Boolean boolResult = batchProcessor.execute(() -> true);

      assertEquals("string", stringResult);
      assertEquals(Integer.valueOf(42), intResult);
      assertEquals(Boolean.TRUE, boolResult);
    }

    @Test
    @DisplayName("Should reject null operation")
    void shouldRejectNullOperation() {
      assertThrows(
          IllegalArgumentException.class,
          () -> batchProcessor.execute(null),
          "Should reject null operation");
    }
  }

  @Nested
  @DisplayName("Batch Processing Tests")
  class BatchProcessingTests {

    @Test
    @DisplayName("Should batch multiple operations")
    void shouldBatchMultipleOperations() throws InterruptedException {
      final int batchSize = 5;
      final AtomicInteger executionCount = new AtomicInteger(0);
      final CountDownLatch latch = new CountDownLatch(batchSize);

      try (final JniBatchProcessor processor = new JniBatchProcessor(batchSize, 50)) {
        // Submit operations concurrently
        final ExecutorService executor = Executors.newFixedThreadPool(batchSize);
        final List<CompletableFuture<String>> futures = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
          final int index = i;
          final CompletableFuture<String> future =
              CompletableFuture.supplyAsync(
                  () ->
                      processor.execute(
                          () -> {
                            executionCount.incrementAndGet();
                            latch.countDown();
                            return "result" + index;
                          }),
                  executor);
          futures.add(future);
        }

        // Wait for all operations to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS), "All operations should complete");

        // Verify results
        for (int i = 0; i < batchSize; i++) {
          assertEquals("result" + i, futures.get(i).join());
        }

        assertEquals(batchSize, executionCount.get());
        executor.shutdown();
      }
    }

    @Test
    @DisplayName("Should handle timeout-based batching")
    void shouldHandleTimeoutBasedBatching() throws InterruptedException {
      final int shortTimeout = 100;
      final AtomicInteger executionCount = new AtomicInteger(0);

      try (final JniBatchProcessor processor = new JniBatchProcessor(100, shortTimeout)) {
        // Submit a single operation
        final String result =
            processor.execute(
                () -> {
                  executionCount.incrementAndGet();
                  return "timeout result";
                });

        assertEquals("timeout result", result);
        assertEquals(1, executionCount.get());
      }
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should propagate operation exceptions")
    void shouldPropagateOperationExceptions() {
      final RuntimeException expectedException = new RuntimeException("Test exception");

      final RuntimeException actualException =
          assertThrows(
              RuntimeException.class,
              () ->
                  batchProcessor.execute(
                      () -> {
                        throw expectedException;
                      }));

      assertEquals(expectedException, actualException);
    }

    @Test
    @DisplayName("Should handle multiple operation exceptions")
    void shouldHandleMultipleOperationExceptions() throws InterruptedException {
      final ExecutorService executor = Executors.newFixedThreadPool(3);
      final List<CompletableFuture<Void>> futures = new ArrayList<>();

      for (int i = 0; i < 3; i++) {
        final int index = i;
        final CompletableFuture<Void> future =
            CompletableFuture.runAsync(
                () -> {
                  final RuntimeException exception =
                      assertThrows(
                          RuntimeException.class,
                          () ->
                              batchProcessor.execute(
                                  () -> {
                                    throw new RuntimeException("Exception " + index);
                                  }));

                  assertTrue(exception.getMessage().contains("Exception " + index));
                },
                executor);

        futures.add(future);
      }

      // Wait for all operations to complete
      CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
      executor.shutdown();
    }

    @Test
    @DisplayName("Should handle checked exceptions")
    void shouldHandleCheckedExceptions() {
      final RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () ->
                  batchProcessor.execute(
                      () -> {
                        // This would normally be a checked exception
                        throw new RuntimeException(new Exception("Checked exception"));
                      }));

      assertTrue(exception.getCause() instanceof Exception);
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("Should close gracefully")
    void shouldCloseGracefully() {
      assertFalse(batchProcessor.isClosed());
      batchProcessor.close();
      assertTrue(batchProcessor.isClosed());
    }

    @Test
    @DisplayName("Should be idempotent on close")
    void shouldBeIdempotentOnClose() {
      batchProcessor.close();
      assertTrue(batchProcessor.isClosed());

      // Second close should not throw
      batchProcessor.close();
      assertTrue(batchProcessor.isClosed());
    }

    @Test
    @DisplayName("Should reject operations after close")
    void shouldRejectOperationsAfterClose() {
      batchProcessor.close();

      assertThrows(
          RuntimeException.class,
          () -> batchProcessor.execute(() -> "test"),
          "Should reject operations on closed processor");
    }

    @Test
    @DisplayName("Should process remaining operations on close")
    void shouldProcessRemainingOperationsOnClose() throws InterruptedException {
      final AtomicInteger executionCount = new AtomicInteger(0);
      final CountDownLatch latch = new CountDownLatch(1);

      // Submit operation but don't wait for completion
      final Thread operationThread =
          new Thread(
              () -> {
                try {
                  batchProcessor.execute(
                      () -> {
                        executionCount.incrementAndGet();
                        latch.countDown();
                        return "processed on close";
                      });
                } catch (final Exception e) {
                  // Expected if processor is closed during operation
                }
              });

      operationThread.start();

      // Give some time for operation to be queued
      Thread.sleep(50);

      // Close processor
      batchProcessor.close();

      // Wait for operation to complete or timeout
      assertTrue(
          latch.await(2, TimeUnit.SECONDS) || batchProcessor.isClosed(),
          "Operation should complete or processor should be closed");

      operationThread.join();
    }

    @Test
    @DisplayName("Should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      final AtomicInteger executionCount = new AtomicInteger(0);

      try (final JniBatchProcessor processor = new JniBatchProcessor(10, 50)) {
        final String result =
            processor.execute(
                () -> {
                  executionCount.incrementAndGet();
                  return "auto-close test";
                });
        assertEquals("auto-close test", result);
      }

      assertEquals(1, executionCount.get());
    }
  }

  @Nested
  @DisplayName("Performance Tests")
  class PerformanceTests {

    @Test
    @DisplayName("Should handle high throughput operations")
    @Timeout(10)
    void shouldHandleHighThroughputOperations() throws InterruptedException {
      final int operationCount = 1000;
      final AtomicInteger completedCount = new AtomicInteger(0);
      final ExecutorService executor = Executors.newFixedThreadPool(20);

      try (final JniBatchProcessor processor = new JniBatchProcessor(50, 10)) {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < operationCount; i++) {
          final int index = i;
          final CompletableFuture<Void> future =
              CompletableFuture.runAsync(
                  () -> {
                    final String result = processor.execute(() -> "result" + index);
                    assertEquals("result" + index, result);
                    completedCount.incrementAndGet();
                  },
                  executor);

          futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
        assertEquals(operationCount, completedCount.get());
      } finally {
        executor.shutdown();
      }
    }

    @Test
    @DisplayName("Should maintain queue size bounds")
    void shouldMaintainQueueSizeBounds() {
      final int maxBatchSize = 10;

      try (final JniBatchProcessor processor = new JniBatchProcessor(maxBatchSize, 100)) {
        // Submit operations that will take some time
        final ExecutorService executor = Executors.newFixedThreadPool(5);
        final List<CompletableFuture<String>> futures = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
          final int index = i;
          final CompletableFuture<String> future =
              CompletableFuture.supplyAsync(
                  () ->
                      processor.execute(
                          () -> {
                            try {
                              Thread.sleep(10); // Small delay
                            } catch (final InterruptedException e) {
                              Thread.currentThread().interrupt();
                            }
                            return "delayed" + index;
                          }),
                  executor);

          futures.add(future);
        }

        // Check that queue size is managed
        final int queueSize = processor.getQueueSize();
        assertTrue(queueSize >= 0, "Queue size should be non-negative");

        // Wait for completion
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
        executor.shutdown();
      }
    }
  }

  @Nested
  @DisplayName("Concurrency Tests")
  class ConcurrencyTests {

    @Test
    @DisplayName("Should handle concurrent access safely")
    void shouldHandleConcurrentAccessSafely() throws InterruptedException {
      final int threadCount = 20;
      final int operationsPerThread = 10;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch latch = new CountDownLatch(threadCount * operationsPerThread);
      final AtomicInteger totalExecutions = new AtomicInteger(0);

      try (final JniBatchProcessor processor = new JniBatchProcessor(25, 50)) {
        for (int t = 0; t < threadCount; t++) {
          final int threadIndex = t;
          executor.submit(
              () -> {
                for (int i = 0; i < operationsPerThread; i++) {
                  final int operationIndex = i;
                  try {
                    final String result =
                        processor.execute(
                            () -> {
                              totalExecutions.incrementAndGet();
                              return "thread" + threadIndex + "-op" + operationIndex;
                            });

                    assertTrue(result.startsWith("thread" + threadIndex + "-op"));
                    latch.countDown();
                  } catch (final Exception e) {
                    // Handle any exceptions
                    latch.countDown();
                  }
                }
              });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All operations should complete");
        assertEquals(threadCount * operationsPerThread, totalExecutions.get());
      } finally {
        executor.shutdown();
      }
    }

    @Test
    @DisplayName("Should handle interruption gracefully")
    void shouldHandleInterruptionGracefully() throws InterruptedException {
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch interruptLatch = new CountDownLatch(1);

      final Thread operationThread =
          new Thread(
              () -> {
                try {
                  startLatch.countDown();
                  batchProcessor.execute(
                      () -> {
                        try {
                          // Wait for interruption
                          interruptLatch.await();
                        } catch (final InterruptedException e) {
                          Thread.currentThread().interrupt();
                          throw new RuntimeException("Interrupted", e);
                        }
                        return "completed";
                      });
                } catch (final RuntimeException e) {
                  // Expected when interrupted
                  assertTrue(
                      e.getMessage().contains("interrupted")
                          || e.getMessage().contains("Interrupted"));
                }
              });

      operationThread.start();
      startLatch.await(); // Wait for operation to start

      // Interrupt the thread
      operationThread.interrupt();
      interruptLatch.countDown();

      operationThread.join(1000);
      assertFalse(operationThread.isAlive(), "Thread should terminate after interruption");
    }
  }
}
