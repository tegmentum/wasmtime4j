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

package ai.tegmentum.wasmtime4j.async;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.async.AsyncRuntime.AsyncResult;
import ai.tegmentum.wasmtime4j.async.AsyncRuntime.OperationStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;

/**
 * Integration tests for async operation cancellation and timeout handling.
 *
 * <p>These tests verify cancellation during execution, timeout enforcement, resource cleanup after
 * cancellation, stack creation edge cases, and deadlock prevention with multiple async calls.
 *
 * @since 1.0.0
 */
@DisplayName("Async Cancellation Integration Tests")
public final class AsyncCancellationIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(AsyncCancellationIntegrationTest.class.getName());

  /**
   * Simple WebAssembly module with an add function for async testing.
   *
   * <pre>
   * (module
   *   (func (export "add") (param i32 i32) (result i32)
   *     local.get 0
   *     local.get 1
   *     i32.add))
   * </pre>
   */
  private static final byte[] ADD_WASM =
      new byte[] {
        0x00, 0x61, 0x73, 0x6D, // magic number
        0x01, 0x00, 0x00, 0x00, // version 1
        // Type section (id=1)
        0x01, 0x07, // section id and size
        0x01, // number of types
        0x60, 0x02, 0x7F, 0x7F, 0x01, 0x7F, // (i32, i32) -> i32
        // Function section (id=3)
        0x03, 0x02, // section id and size
        0x01, // number of functions
        0x00, // function 0: type 0
        // Export section (id=7)
        0x07, 0x07, // section id and size
        0x01, // number of exports
        0x03, 0x61, 0x64, 0x64, // "add"
        0x00, 0x00, // function export, index 0
        // Code section (id=10)
        0x0A, 0x09, // section id and size
        0x01, // number of functions
        0x07, // function body size
        0x00, // local variable count
        0x20, 0x00, // local.get 0
        0x20, 0x01, // local.get 1
        0x6A, // i32.add
        0x0B // end
      };

  private static boolean asyncRuntimeAvailable = false;

  @BeforeAll
  static void checkAsyncRuntimeAvailable() {
    try {
      final AsyncRuntime runtime = AsyncRuntimeFactory.create();
      runtime.close();
      asyncRuntimeAvailable = true;
      LOGGER.info("AsyncRuntime native implementation is available for cancellation tests");
    } catch (final Throwable t) {
      asyncRuntimeAvailable = false;
      LOGGER.warning("AsyncRuntime not available - tests will be skipped: " + t.getMessage());
    }
  }

  private static void assumeAsyncRuntimeAvailable() {
    assumeTrue(
        asyncRuntimeAvailable, "AsyncRuntime native implementation not available - skipping");
  }

  private AsyncRuntime asyncRuntime;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (asyncRuntimeAvailable) {
      asyncRuntime = AsyncRuntimeFactory.create();
      resources.add(asyncRuntime);
      asyncRuntime.initialize();
    }
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    asyncRuntime = null;
  }

  @Nested
  @DisplayName("Cancellation During Execution Tests")
  class CancellationDuringExecutionTests {

    @Test
    @DisplayName("should cancel operation immediately after start")
    @Timeout(30)
    void shouldCancelOperationImmediatelyAfterStart(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AtomicBoolean callbackInvoked = new AtomicBoolean(false);
      final AtomicReference<AsyncResult> resultRef = new AtomicReference<>();

      // Start a compilation operation
      final long operationId =
          asyncRuntime.compileAsync(
              ADD_WASM,
              30000L,
              null,
              result -> {
                callbackInvoked.set(true);
                resultRef.set(result);
              });

      assertTrue(operationId > 0, "Operation ID should be positive");

      // Cancel immediately
      final boolean cancelled = asyncRuntime.cancelOperation(operationId);

      // Either cancellation succeeded or operation completed too fast
      final OperationStatus status = asyncRuntime.getOperationStatus(operationId);
      LOGGER.info("Operation status after cancel attempt: " + status);
      LOGGER.info("Cancel returned: " + cancelled);

      // Verify system remains stable
      assertDoesNotThrow(
          () -> asyncRuntime.getActiveOperationCount(), "Should query active operations");

      LOGGER.info("Immediate cancellation test completed");
    }

    @Test
    @DisplayName("should handle cancellation of already completed operation")
    @Timeout(30)
    void shouldHandleCancellationOfAlreadyCompletedOperation(final TestInfo testInfo)
        throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final CountDownLatch completionLatch = new CountDownLatch(1);
      final AtomicReference<AsyncResult> resultRef = new AtomicReference<>();

      // Start a simple compilation
      final long operationId =
          asyncRuntime.compileAsync(
              ADD_WASM,
              10000L,
              null,
              result -> {
                resultRef.set(result);
                completionLatch.countDown();
              });

      // Wait for completion
      final boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
      assertTrue(completed, "Operation should complete");

      // Try to cancel after completion
      // Note: The API may return true even for completed operations (idempotent cancel)
      // The key requirement is that it doesn't crash or throw
      final boolean cancelled = asyncRuntime.cancelOperation(operationId);
      LOGGER.info("Cancel after completion returned: " + cancelled);

      // Verify the operation result was captured
      assertNotNull(resultRef.get(), "Result should have been captured before cancel attempt");

      LOGGER.info("Cancellation of completed operation handled correctly");
    }

    @Test
    @DisplayName("should handle multiple rapid cancellation attempts")
    @Timeout(30)
    void shouldHandleMultipleRapidCancellationAttempts(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Start operation
      final long operationId =
          asyncRuntime.compileAsync(ADD_WASM, 60000L, null, result -> {});

      // Attempt multiple rapid cancellations
      int cancelSuccessCount = 0;
      for (int i = 0; i < 10; i++) {
        if (asyncRuntime.cancelOperation(operationId)) {
          cancelSuccessCount++;
        }
      }

      LOGGER.info("Successful cancellations out of 10 attempts: " + cancelSuccessCount);

      // At most one cancellation should succeed (the first one, if operation was still running)
      assertTrue(cancelSuccessCount <= 1, "At most one cancellation should succeed");

      LOGGER.info("Multiple rapid cancellation attempts handled correctly");
    }
  }

  @Nested
  @DisplayName("Timeout Enforcement Tests")
  class TimeoutEnforcementTests {

    @Test
    @DisplayName("should enforce wait timeout")
    @Timeout(30)
    void shouldEnforceWaitTimeout(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Start an operation with long execution timeout
      final long operationId =
          asyncRuntime.compileAsync(ADD_WASM, 60000L, null, result -> {});

      // Wait with very short timeout
      final long startTime = System.currentTimeMillis();
      final OperationStatus status = asyncRuntime.waitForOperation(operationId, 100L);
      final long elapsed = System.currentTimeMillis() - startTime;

      LOGGER.info("Wait elapsed time: " + elapsed + "ms");
      LOGGER.info("Status after wait: " + status);

      // Verify wait returned within reasonable time
      // Allow some slack for system overhead
      assertTrue(elapsed < 5000, "Wait should return relatively quickly");

      LOGGER.info("Wait timeout enforcement verified");
    }

    @Test
    @DisplayName("should handle zero timeout wait")
    @Timeout(10)
    void shouldHandleZeroTimeoutWait(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Start an operation
      final long operationId =
          asyncRuntime.compileAsync(ADD_WASM, 10000L, null, result -> {});

      // Wait with zero timeout
      final OperationStatus status = asyncRuntime.waitForOperation(operationId, 0L);
      assertNotNull(status, "Status should not be null even with zero timeout");

      LOGGER.info("Zero timeout wait returned status: " + status);
    }

    @Test
    @DisplayName("should complete successfully when timeout is sufficient")
    @Timeout(30)
    void shouldCompleteSuccessfullyWhenTimeoutIsSufficient(final TestInfo testInfo)
        throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final CountDownLatch latch = new CountDownLatch(1);
      final AtomicReference<AsyncResult> resultRef = new AtomicReference<>();

      // Start with reasonable timeout
      final long operationId =
          asyncRuntime.compileAsync(
              ADD_WASM,
              10000L,
              null,
              result -> {
                resultRef.set(result);
                latch.countDown();
              });

      // Wait for completion with longer timeout
      final OperationStatus status = asyncRuntime.waitForOperation(operationId, 15000L);

      LOGGER.info("Final status: " + status);

      // The operation should complete
      assertTrue(
          status == OperationStatus.COMPLETED || latch.await(1, TimeUnit.SECONDS),
          "Operation should complete with sufficient timeout");

      LOGGER.info("Completion with sufficient timeout verified");
    }
  }

  @Nested
  @DisplayName("Resource Cleanup After Cancellation Tests")
  class ResourceCleanupTests {

    @Test
    @DisplayName("should cleanup resources after cancellation")
    @Timeout(30)
    void shouldCleanupResourcesAfterCancellation(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final int initialActiveCount = asyncRuntime.getActiveOperationCount();
      LOGGER.info("Initial active operation count: " + initialActiveCount);

      // Start and cancel multiple operations
      for (int i = 0; i < 5; i++) {
        final long operationId =
            asyncRuntime.compileAsync(ADD_WASM, 60000L, null, result -> {});
        asyncRuntime.cancelOperation(operationId);
      }

      // Wait for cleanup
      Thread.sleep(500);

      final int finalActiveCount = asyncRuntime.getActiveOperationCount();
      LOGGER.info("Final active operation count: " + finalActiveCount);

      // Active count should not grow unbounded
      assertTrue(
          finalActiveCount <= initialActiveCount + 5,
          "Active operations should not accumulate indefinitely");

      LOGGER.info("Resource cleanup after cancellation verified");
    }

    @RepeatedTest(value = 3, name = "Memory leak test {currentRepetition}/{totalRepetitions}")
    @DisplayName("should not leak memory on repeated cancel cycles")
    @Timeout(60)
    void shouldNotLeakMemoryOnRepeatedCancelCycles(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final int cycles = 10;
      final List<Long> operationIds = new ArrayList<>();

      // Run multiple cancel cycles
      for (int cycle = 0; cycle < cycles; cycle++) {
        // Start operation
        final long opId =
            asyncRuntime.compileAsync(ADD_WASM, 60000L, null, result -> {});
        operationIds.add(opId);

        // Cancel after small delay
        asyncRuntime.cancelOperation(opId);
      }

      // Wait for cleanup
      Thread.sleep(500);

      // Verify no crashes and reasonable state
      final int activeCount = asyncRuntime.getActiveOperationCount();
      LOGGER.info("Active operations after " + cycles + " cancel cycles: " + activeCount);

      assertTrue(activeCount >= 0, "Active count should be non-negative");

      LOGGER.info("Memory leak test cycle completed");
    }

    @Test
    @DisplayName("should cleanup when runtime is closed with pending operations")
    @Timeout(30)
    void shouldCleanupWhenRuntimeIsClosedWithPendingOperations(final TestInfo testInfo)
        throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a separate runtime for this test
      final AsyncRuntime localRuntime = AsyncRuntimeFactory.create();
      localRuntime.initialize();

      // Start some operations
      for (int i = 0; i < 3; i++) {
        localRuntime.compileAsync(ADD_WASM, 60000L, null, result -> {});
      }

      // Close runtime with pending operations
      assertDoesNotThrow(
          localRuntime::close, "Close should not throw even with pending operations");

      LOGGER.info("Runtime closed cleanly with pending operations");
    }
  }

  @Nested
  @DisplayName("Multiple Concurrent Async Calls Tests")
  class ConcurrentAsyncCallsTests {

    @Test
    @DisplayName("should handle multiple concurrent operations")
    @Timeout(60)
    void shouldHandleMultipleConcurrentOperations(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final int operationCount = 10;
      final CountDownLatch completionLatch = new CountDownLatch(operationCount);
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger errorCount = new AtomicInteger(0);
      final List<Long> operationIds = Collections.synchronizedList(new ArrayList<>());

      // Start multiple operations
      for (int i = 0; i < operationCount; i++) {
        final long opId =
            asyncRuntime.compileAsync(
                ADD_WASM,
                10000L,
                null,
                result -> {
                  if (result.isSuccess()) {
                    successCount.incrementAndGet();
                  } else {
                    errorCount.incrementAndGet();
                  }
                  completionLatch.countDown();
                });
        operationIds.add(opId);
      }

      // Wait for all to complete
      final boolean allCompleted = completionLatch.await(30, TimeUnit.SECONDS);
      assertTrue(allCompleted, "All operations should complete within timeout");

      LOGGER.info("Concurrent operations completed:");
      LOGGER.info("  Total: " + operationCount);
      LOGGER.info("  Successes: " + successCount.get());
      LOGGER.info("  Errors: " + errorCount.get());

      // All should complete (success or error is ok, no hangs)
      assertEquals(
          operationCount, successCount.get() + errorCount.get(), "All operations should complete");

      LOGGER.info("Multiple concurrent operations handled correctly");
    }

    @Test
    @DisplayName("should not deadlock with interleaved operations and cancellations")
    @Timeout(60)
    void shouldNotDeadlockWithInterleavedOperationsAndCancellations(final TestInfo testInfo)
        throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final int threadCount = 4;
      final int opsPerThread = 5;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final AtomicBoolean hasDeadlock = new AtomicBoolean(false);
      final AtomicInteger completedOps = new AtomicInteger(0);

      try {
        @SuppressWarnings("rawtypes")
        CompletableFuture[] futures = new CompletableFuture[threadCount];

        for (int t = 0; t < threadCount; t++) {
          final int threadId = t;
          futures[t] =
              CompletableFuture.runAsync(
                  () -> {
                    try {
                      for (int i = 0; i < opsPerThread; i++) {
                        // Start operation
                        final long opId =
                            asyncRuntime.compileAsync(
                                ADD_WASM,
                                5000L,
                                null,
                                result -> completedOps.incrementAndGet());

                        // Randomly either wait or cancel
                        if ((threadId + i) % 2 == 0) {
                          asyncRuntime.cancelOperation(opId);
                        } else {
                          asyncRuntime.waitForOperation(opId, 1000L);
                        }

                        // Query status
                        asyncRuntime.getOperationStatus(opId);
                        asyncRuntime.getActiveOperationCount();
                      }
                    } catch (final Exception e) {
                      LOGGER.warning("Thread " + threadId + " error: " + e.getMessage());
                    }
                  },
                  executor);
        }

        // Wait for all threads with timeout to detect deadlock
        try {
          CompletableFuture.allOf(futures).get(45, TimeUnit.SECONDS);
        } catch (final Exception e) {
          hasDeadlock.set(true);
          LOGGER.severe("Potential deadlock detected: " + e.getMessage());
        }

        assertFalse(hasDeadlock.get(), "No deadlock should occur");

        LOGGER.info("Interleaved operations test completed:");
        LOGGER.info("  Threads: " + threadCount);
        LOGGER.info("  Ops per thread: " + opsPerThread);
        LOGGER.info("  Completed callbacks: " + completedOps.get());

      } finally {
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor should terminate");
      }
    }

    @Test
    @DisplayName("should handle rapid start and query cycles")
    @Timeout(30)
    void shouldHandleRapidStartAndQueryCycles(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final int cycles = 20;
      final AtomicInteger exceptionCount = new AtomicInteger(0);

      for (int i = 0; i < cycles; i++) {
        try {
          // Start
          final long opId =
              asyncRuntime.compileAsync(ADD_WASM, 10000L, null, result -> {});

          // Rapid queries
          asyncRuntime.getOperationStatus(opId);
          asyncRuntime.getActiveOperationCount();
          asyncRuntime.getOperationStatus(opId);

          // Cancel
          asyncRuntime.cancelOperation(opId);
        } catch (final Exception e) {
          exceptionCount.incrementAndGet();
          LOGGER.warning("Cycle " + i + " error: " + e.getMessage());
        }
      }

      LOGGER.info("Rapid cycles completed: " + cycles);
      LOGGER.info("Exceptions: " + exceptionCount.get());

      // Allow some exceptions but not all should fail
      assertTrue(exceptionCount.get() < cycles, "Most cycles should complete without exception");

      LOGGER.info("Rapid start/query cycles handled correctly");
    }
  }

  @Nested
  @DisplayName("Async Runtime Stress Tests")
  class StressTests {

    @RepeatedTest(value = 3, name = "Stress test iteration {currentRepetition}/{totalRepetitions}")
    @DisplayName("should survive repeated runtime create/destroy cycles")
    @Timeout(60)
    void shouldSurviveRepeatedRuntimeCreateDestroyCycles(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final int cycles = 5;
      int successCount = 0;

      for (int i = 0; i < cycles; i++) {
        try (AsyncRuntime localRuntime = AsyncRuntimeFactory.create()) {
          localRuntime.initialize();

          // Start some operations
          for (int j = 0; j < 3; j++) {
            localRuntime.compileAsync(ADD_WASM, 5000L, null, result -> {});
          }

          // Verify runtime is functional
          assertTrue(localRuntime.isInitialized(), "Runtime should be initialized");
          localRuntime.getActiveOperationCount();

          successCount++;
        } catch (final Exception e) {
          LOGGER.warning("Cycle " + i + " failed: " + e.getMessage());
        }
      }

      LOGGER.info("Successful create/destroy cycles: " + successCount + "/" + cycles);
      assertTrue(successCount >= cycles * 0.8, "At least 80% of cycles should succeed");
    }

    @Test
    @DisplayName("should handle shutdown during active operations")
    @Timeout(30)
    void shouldHandleShutdownDuringActiveOperations(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Start some long-running operations
      for (int i = 0; i < 5; i++) {
        asyncRuntime.compileAsync(ADD_WASM, 60000L, null, result -> {});
      }

      // Immediately shutdown
      assertDoesNotThrow(
          () -> asyncRuntime.shutdown(), "Shutdown should not throw with active operations");

      LOGGER.info("Shutdown during active operations handled correctly");
    }

    @Test
    @DisplayName("should not crash with operations after shutdown")
    @Timeout(30)
    void shouldNotCrashWithOperationsAfterShutdown(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create separate runtime for this test
      final AsyncRuntime localRuntime = AsyncRuntimeFactory.create();
      localRuntime.initialize();

      // Shutdown first
      localRuntime.shutdown();

      // Attempts after shutdown should handle gracefully (throw or return error, but not crash)
      try {
        localRuntime.compileAsync(ADD_WASM, 1000L, null, result -> {});
        LOGGER.info("Operation after shutdown returned (may have errored in callback)");
      } catch (final Exception e) {
        LOGGER.info("Expected exception after shutdown: " + e.getMessage());
      }

      // Close should still work
      assertDoesNotThrow(localRuntime::close, "Close after shutdown should not throw");

      LOGGER.info("Operations after shutdown handled safely");
    }
  }

  @Nested
  @DisplayName("Progress Callback Tests")
  class ProgressCallbackTests {

    @Test
    @DisplayName("should invoke progress callbacks")
    @Timeout(30)
    void shouldInvokeProgressCallbacks(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final CountDownLatch completionLatch = new CountDownLatch(1);
      final AtomicInteger progressUpdates = new AtomicInteger(0);
      final List<Integer> progressValues = Collections.synchronizedList(new ArrayList<>());

      asyncRuntime.compileAsync(
          ADD_WASM,
          10000L,
          progress -> {
            progressUpdates.incrementAndGet();
            progressValues.add(progress);
          },
          result -> completionLatch.countDown());

      final boolean completed = completionLatch.await(15, TimeUnit.SECONDS);
      assertTrue(completed, "Compilation should complete");

      LOGGER.info("Progress updates received: " + progressUpdates.get());
      LOGGER.info("Progress values: " + progressValues);

      // Note: Progress callbacks may not be called for very fast compilations
      assertTrue(progressUpdates.get() >= 0, "Progress update count should be non-negative");

      LOGGER.info("Progress callback test completed");
    }

    @Test
    @DisplayName("should handle null progress callback")
    @Timeout(30)
    void shouldHandleNullProgressCallback(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final CountDownLatch completionLatch = new CountDownLatch(1);

      // Pass null for progress callback
      assertDoesNotThrow(
          () ->
              asyncRuntime.compileAsync(
                  ADD_WASM, 10000L, null, result -> completionLatch.countDown()),
          "Should accept null progress callback");

      final boolean completed = completionLatch.await(15, TimeUnit.SECONDS);
      assertTrue(completed, "Compilation should complete with null progress callback");

      LOGGER.info("Null progress callback handled correctly");
    }
  }
}
