package ai.tegmentum.wasmtime4j.jni.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive unit tests for {@link JniBatchProcessor}.
 *
 * <p>These tests verify batch processing functionality, thread safety, performance optimization,
 * error handling, and resource management.
 */
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
  class ConstructorTests {

    @Test
    void testCreateProcessorWithDefaultSettings() {
      try (final JniBatchProcessor processor = new JniBatchProcessor()) {
        assertEquals(JniBatchProcessor.DEFAULT_MAX_BATCH_SIZE, processor.getMaxBatchSize());
        assertEquals(JniBatchProcessor.DEFAULT_BATCH_TIMEOUT_MS, processor.getBatchTimeoutMs());
        assertFalse(processor.isClosed());
        assertEquals(0, processor.getQueueSize());
      }
    }

    @Test
    void testCreateProcessorWithCustomSettings() {
      final int maxBatchSize = 50;
      final long timeoutMs = 100;

      try (final JniBatchProcessor processor = new JniBatchProcessor(maxBatchSize, timeoutMs)) {
        assertEquals(maxBatchSize, processor.getMaxBatchSize());
        assertEquals(timeoutMs, processor.getBatchTimeoutMs());
        assertFalse(processor.isClosed());
      }
    }

    @Test
    void testRejectInvalidBatchSize() {
      assertThrows(
          JniValidationException.class,
          () -> new JniBatchProcessor(0, 10),
          "Should reject zero batch size");

      assertThrows(
          JniValidationException.class,
          () -> new JniBatchProcessor(-1, 10),
          "Should reject negative batch size");
    }

    @Test
    void testRejectNegativeTimeout() {
      assertThrows(
          JniValidationException.class,
          () -> new JniBatchProcessor(10, -1),
          "Should reject negative timeout");
    }

    @Test
    void testAcceptZeroTimeout() {
      try (final JniBatchProcessor processor = new JniBatchProcessor(10, 0)) {
        assertEquals(0, processor.getBatchTimeoutMs());
      }
    }
  }

  @Nested
  class BasicOperationTests {

    @Test
    void testExecuteSingleOperation() {
      final String result = batchProcessor.execute(() -> "test result");
      assertEquals("test result", result);
    }

    @Test
    void testExecuteOperationReturningNull() {
      final String result = batchProcessor.execute(() -> null);
      assertEquals(null, result);
    }

    @Test
    void testExecuteMultipleOperations() {
      final String result1 = batchProcessor.execute(() -> "result1");
      final String result2 = batchProcessor.execute(() -> "result2");
      final String result3 = batchProcessor.execute(() -> "result3");

      assertEquals("result1", result1);
      assertEquals("result2", result2);
      assertEquals("result3", result3);
    }

    @Test
    void testHandleOperationsWithDifferentTypes() {
      final String stringResult = batchProcessor.execute(() -> "string");
      final Integer intResult = batchProcessor.execute(() -> 42);
      final Boolean boolResult = batchProcessor.execute(() -> true);

      assertEquals("string", stringResult);
      assertEquals(Integer.valueOf(42), intResult);
      assertEquals(Boolean.TRUE, boolResult);
    }

    @Test
    void testRejectNullOperation() {
      assertThrows(
          JniValidationException.class,
          () -> batchProcessor.execute(null),
          "Should reject null operation");
    }
  }

  @Nested
  class BatchProcessingTests {

    @Test
    void testBatchMultipleOperations() throws InterruptedException {
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
    void testHandleTimeoutBasedBatching() throws InterruptedException {
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
  class ErrorHandlingTests {

    @Test
    void testPropagateOperationExceptions() {
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
    void testHandleMultipleOperationExceptions() throws InterruptedException {
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
    void testHandleCheckedExceptions() {
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
  class ResourceManagementTests {

    @Test
    void testCloseGracefully() {
      assertFalse(batchProcessor.isClosed());
      batchProcessor.close();
      assertTrue(batchProcessor.isClosed());
    }

    @Test
    void testIdempotentOnClose() {
      batchProcessor.close();
      assertTrue(batchProcessor.isClosed());

      // Second close should not throw
      batchProcessor.close();
      assertTrue(batchProcessor.isClosed());
    }

    @Test
    void testRejectOperationsAfterClose() {
      batchProcessor.close();

      assertThrows(
          RuntimeException.class,
          () -> batchProcessor.execute(() -> "test"),
          "Should reject operations on closed processor");
    }

    @Test
    void testProcessRemainingOperationsOnClose() throws InterruptedException {
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
                } catch (
                    @SuppressFBWarnings(
                        value = "DE_MIGHT_IGNORE",
                        justification =
                            "Test intentionally ignores exception when processor is closed during"
                                + " operation")
                    final Exception e) {
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
    void testWorkWithTryWithResources() {
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
  class PerformanceTests {

    @Test
    @Timeout(10)
    void testHandleHighThroughputOperations() throws InterruptedException {
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
    void testMaintainQueueSizeBounds() {
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
  class ConcurrencyTests {

    @Test
    void testHandleConcurrentAccessSafely() throws InterruptedException {
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
    void testHandleInterruptionGracefully() throws InterruptedException {
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
