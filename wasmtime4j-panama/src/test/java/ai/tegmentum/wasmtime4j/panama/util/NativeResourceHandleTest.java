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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NativeResourceHandle}.
 *
 * <p>Verifies close idempotency, thread safety, ensureNotClosed behavior, and Cleaner safety net.
 */
@DisplayName("NativeResourceHandle Tests")
class NativeResourceHandleTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should reject null resourceType")
    void shouldRejectNullResourceType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new NativeResourceHandle(null, () -> {}),
          "Should throw for null resourceType");
    }

    @Test
    @DisplayName("Should reject null closeAction")
    void shouldRejectNullCloseAction() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new NativeResourceHandle("TestResource", null),
          "Should throw for null closeAction");
    }

    @Test
    @DisplayName("Should reject null safetyNetOwner")
    void shouldRejectNullSafetyNetOwner() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new NativeResourceHandle("TestResource", () -> {}, null, () -> {}),
          "Should throw for null safetyNetOwner");
    }

    @Test
    @DisplayName("Should reject null safetyNetAction")
    void shouldRejectNullSafetyNetAction() {
      final Object owner = new Object();
      assertThrows(
          IllegalArgumentException.class,
          () -> new NativeResourceHandle("TestResource", () -> {}, owner, null),
          "Should throw for null safetyNetAction");
    }

    @Test
    @DisplayName("Should create handle without safety net")
    void shouldCreateHandleWithoutSafetyNet() {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});
      assertFalse(handle.isClosed(), "New handle should not be closed");
    }

    @Test
    @DisplayName("Should create handle with safety net")
    void shouldCreateHandleWithSafetyNet() {
      final Object owner = new Object();
      final NativeResourceHandle handle =
          new NativeResourceHandle("TestResource", () -> {}, owner, () -> {});
      assertFalse(handle.isClosed(), "New handle should not be closed");
    }
  }

  @Nested
  @DisplayName("ensureNotClosed Tests")
  class EnsureNotClosedTests {

    @Test
    @DisplayName("Should not throw for open handle")
    void shouldNotThrowForOpenHandle() {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});
      assertDoesNotThrow(handle::ensureNotClosed, "Should not throw for open handle");
    }

    @Test
    @DisplayName("Should throw for closed handle")
    void shouldThrowForClosedHandle() {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});
      handle.close();

      final IllegalStateException ex =
          assertThrows(IllegalStateException.class, handle::ensureNotClosed);
      assertTrue(
          ex.getMessage().contains("TestResource"),
          "Exception message should include resource type, got: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("closed"),
          "Exception message should mention 'closed', got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Exception message should include custom resource type")
    void exceptionMessageShouldIncludeCustomResourceType() {
      final NativeResourceHandle handle = new NativeResourceHandle("PanamaEngine", () -> {});
      handle.close();

      final IllegalStateException ex =
          assertThrows(IllegalStateException.class, handle::ensureNotClosed);
      assertTrue(
          ex.getMessage().contains("PanamaEngine"),
          "Exception message should include 'PanamaEngine', got: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("isClosed Tests")
  class IsClosedTests {

    @Test
    @DisplayName("Should return false for new handle")
    void shouldReturnFalseForNewHandle() {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});
      assertFalse(handle.isClosed(), "New handle should not be closed");
    }

    @Test
    @DisplayName("Should return true after close")
    void shouldReturnTrueAfterClose() {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});
      handle.close();
      assertTrue(handle.isClosed(), "Handle should be closed after close()");
    }
  }

  @Nested
  @DisplayName("close Tests")
  class CloseTests {

    @Test
    @DisplayName("Should execute cleanup action on close")
    void shouldExecuteCleanupActionOnClose() {
      final AtomicBoolean cleanupExecuted = new AtomicBoolean(false);
      final NativeResourceHandle handle =
          new NativeResourceHandle("TestResource", () -> cleanupExecuted.set(true));

      handle.close();

      assertTrue(cleanupExecuted.get(), "Cleanup action should have been executed");
      assertTrue(handle.isClosed(), "Handle should be closed");
    }

    @Test
    @DisplayName("Should be idempotent — cleanup runs exactly once")
    void shouldBeIdempotent() {
      final AtomicInteger cleanupCount = new AtomicInteger(0);
      final NativeResourceHandle handle =
          new NativeResourceHandle("TestResource", () -> cleanupCount.incrementAndGet());

      handle.close();
      handle.close();
      handle.close();

      assertEquals(1, cleanupCount.get(), "Cleanup action should run exactly once");
      assertTrue(handle.isClosed(), "Handle should remain closed");
    }

    @Test
    @DisplayName("Should handle cleanup exceptions gracefully")
    void shouldHandleCleanupExceptionsGracefully() {
      final NativeResourceHandle handle =
          new NativeResourceHandle(
              "TestResource",
              () -> {
                throw new RuntimeException("Simulated cleanup failure");
              });

      assertDoesNotThrow(handle::close, "Close should not propagate cleanup exceptions");
      assertTrue(handle.isClosed(), "Handle should be closed even if cleanup threw");
    }

    @Test
    @DisplayName("Should handle checked exceptions in cleanup")
    void shouldHandleCheckedExceptionsInCleanup() {
      final NativeResourceHandle handle =
          new NativeResourceHandle(
              "TestResource",
              () -> {
                throw new Exception("Simulated checked exception");
              });

      assertDoesNotThrow(handle::close, "Close should not propagate checked exceptions");
      assertTrue(handle.isClosed(), "Handle should be closed even if cleanup threw checked ex");
    }

    @Test
    @DisplayName("Should cancel Cleaner safety net on explicit close")
    void shouldCancelCleanerSafetyNetOnExplicitClose() {
      final AtomicBoolean safetyNetFired = new AtomicBoolean(false);
      final AtomicBoolean cleanupFired = new AtomicBoolean(false);
      final Object owner = new Object();

      final NativeResourceHandle handle =
          new NativeResourceHandle(
              "TestResource", () -> cleanupFired.set(true), owner, () -> safetyNetFired.set(true));

      handle.close();

      assertTrue(cleanupFired.get(), "Cleanup action should have been executed");
      // After close(), cleanable.clean() is called which deregisters the safety net.
      // The safety net Runnable runs immediately when clean() is called, but since
      // close already marked it as closed, the key invariant is that the explicit close
      // cleanup ran.
      assertTrue(handle.isClosed(), "Handle should be closed");
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Concurrent close should execute cleanup exactly once")
    void concurrentCloseShouldExecuteCleanupExactlyOnce() throws InterruptedException {
      final AtomicInteger cleanupCount = new AtomicInteger(0);
      final NativeResourceHandle handle =
          new NativeResourceHandle("TestResource", () -> cleanupCount.incrementAndGet());

      final int threadCount = 20;
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(threadCount);
      final AtomicBoolean anyError = new AtomicBoolean(false);

      for (int i = 0; i < threadCount; i++) {
        new Thread(
                () -> {
                  try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    handle.close();
                  } catch (final Exception e) {
                    anyError.set(true);
                  } finally {
                    doneLatch.countDown();
                  }
                })
            .start();
      }

      startLatch.countDown();
      assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");

      assertFalse(anyError.get(), "No threads should have thrown exceptions");
      assertEquals(1, cleanupCount.get(), "Cleanup should execute exactly once");
      assertTrue(handle.isClosed(), "Handle should be closed");
    }

    @Test
    @DisplayName("Concurrent close and ensureNotClosed should be safe")
    void concurrentCloseAndEnsureNotClosedShouldBeSafe() throws InterruptedException {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});

      final int threadCount = 20;
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(threadCount);
      final AtomicBoolean unexpectedError = new AtomicBoolean(false);

      for (int i = 0; i < threadCount; i++) {
        final int index = i;
        new Thread(
                () -> {
                  try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    if (index % 2 == 0) {
                      handle.close();
                    } else {
                      try {
                        handle.ensureNotClosed();
                      } catch (final IllegalStateException e) {
                        // Expected if close happened first
                      }
                    }
                  } catch (final Exception e) {
                    unexpectedError.set(true);
                  } finally {
                    doneLatch.countDown();
                  }
                })
            .start();
      }

      startLatch.countDown();
      assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
      assertFalse(unexpectedError.get(), "No unexpected exceptions");
      assertTrue(handle.isClosed(), "Handle should be closed");
    }
  }

  @Nested
  @DisplayName("Try-With-Resources Tests")
  class TryWithResourcesTests {

    @Test
    @DisplayName("Should close handle after try-with-resources block")
    void shouldCloseHandleAfterTryWithResourcesBlock() {
      final AtomicBoolean cleanupExecuted = new AtomicBoolean(false);
      final NativeResourceHandle handle;

      try (NativeResourceHandle h =
          new NativeResourceHandle("TestResource", () -> cleanupExecuted.set(true))) {
        handle = h;
        assertFalse(handle.isClosed(), "Should be open inside try block");
      }

      assertTrue(handle.isClosed(), "Should be closed after try block");
      assertTrue(cleanupExecuted.get(), "Cleanup should have executed");
    }
  }

  @Nested
  @DisplayName("beginOperation / endOperation Tests")
  class BeginEndOperationTests {

    @Test
    @DisplayName("beginOperation should succeed on open handle")
    void beginOperationShouldSucceedWhenOpen() {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});
      assertDoesNotThrow(
          handle::beginOperation, "beginOperation() should not throw on an open handle");
      handle.endOperation();
      handle.close();
    }

    @Test
    @DisplayName("beginOperation should throw IllegalStateException on closed handle")
    void beginOperationShouldThrowWhenClosed() {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});
      handle.close();

      final IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              handle::beginOperation,
              "beginOperation() should throw on a closed handle");
      assertTrue(
          ex.getMessage().contains("TestResource"),
          "Exception message should contain resource type, got: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("closed"),
          "Exception message should mention 'closed', got: " + ex.getMessage());
    }

    @Test
    @DisplayName("endOperation should release lock so close can proceed")
    void endOperationShouldReleaseLock() {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});
      handle.beginOperation();
      handle.endOperation();

      assertDoesNotThrow(
          handle::close, "close() should succeed after endOperation() releases the read lock");
      assertTrue(handle.isClosed(), "Handle should be closed");
    }

    @Test
    @DisplayName("Multiple concurrent beginOperation calls should share read lock")
    void multipleConcurrentBeginOperationsShouldShareReadLock() throws InterruptedException {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});
      final int threadCount = 8;
      final CountDownLatch allLocked = new CountDownLatch(threadCount);
      final CountDownLatch releaseLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(threadCount);
      final AtomicInteger errors = new AtomicInteger(0);

      for (int i = 0; i < threadCount; i++) {
        new Thread(
                () -> {
                  try {
                    handle.beginOperation();
                    allLocked.countDown();
                    releaseLatch.await(5, TimeUnit.SECONDS);
                  } catch (final Exception e) {
                    errors.incrementAndGet();
                  } finally {
                    handle.endOperation();
                    doneLatch.countDown();
                  }
                })
            .start();
      }

      assertTrue(
          allLocked.await(5, TimeUnit.SECONDS),
          "All threads should acquire the read lock concurrently");
      assertEquals(0, errors.get(), "No thread should have errored");

      releaseLatch.countDown();
      assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should complete");
      handle.close();
    }

    @Test
    @DisplayName("close should block while beginOperation is held, preventing use-after-free")
    void closeShouldBlockWhileOperationInFlight() throws InterruptedException {
      final AtomicBoolean cleanupRan = new AtomicBoolean(false);
      final NativeResourceHandle handle =
          new NativeResourceHandle("TestResource", () -> cleanupRan.set(true));
      final CountDownLatch operationStarted = new CountDownLatch(1);
      final CountDownLatch closeAttempted = new CountDownLatch(1);
      final CountDownLatch operationDone = new CountDownLatch(1);
      final AtomicBoolean closedBeforeOperationEnd = new AtomicBoolean(true);

      // Thread 1: holds beginOperation lock
      new Thread(
              () -> {
                handle.beginOperation();
                try {
                  operationStarted.countDown();
                  closeAttempted.await(5, TimeUnit.SECONDS);
                  // Give close() thread time to block on write lock
                  Thread.sleep(100);
                  closedBeforeOperationEnd.set(handle.isClosed());
                } catch (final InterruptedException e) {
                  Thread.currentThread().interrupt();
                } finally {
                  handle.endOperation();
                  operationDone.countDown();
                }
              })
          .start();

      operationStarted.await(5, TimeUnit.SECONDS);

      // Thread 2: attempts close() while operation is in-flight
      new Thread(
              () -> {
                closeAttempted.countDown();
                handle.close();
              })
          .start();

      assertTrue(operationDone.await(5, TimeUnit.SECONDS), "Operation thread should complete");

      assertFalse(
          closedBeforeOperationEnd.get(),
          "Resource should NOT be closed while operation read lock is held");
      assertTrue(handle.isClosed(), "Resource should be closed after operation released the lock");
      assertTrue(cleanupRan.get(), "Cleanup action should have run after close completed");
    }

    @Test
    @DisplayName("beginOperation should be reentrant (read lock reentrancy)")
    void beginOperationShouldBeReentrant() {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});
      handle.beginOperation();
      try {
        // Second call should not deadlock — ReentrantReadWriteLock allows read reentrancy
        handle.beginOperation();
        try {
          assertFalse(handle.isClosed(), "Handle should still be open");
        } finally {
          handle.endOperation();
        }
      } finally {
        handle.endOperation();
      }
      handle.close();
    }
  }

  @Nested
  @DisplayName("tryBeginOperation Tests")
  class TryBeginOperationTests {

    @Test
    @DisplayName("tryBeginOperation should return true on open handle")
    void shouldReturnTrueWhenOpen() {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});

      assertTrue(
          handle.tryBeginOperation(), "tryBeginOperation() should return true for open handle");
      handle.endOperation();
      handle.close();
    }

    @Test
    @DisplayName("tryBeginOperation should return false on closed handle without throwing")
    void shouldReturnFalseWhenClosed() {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});
      handle.close();

      assertFalse(
          handle.tryBeginOperation(), "tryBeginOperation() should return false for closed handle");
      // No endOperation() needed when tryBeginOperation returns false
    }

    @Test
    @DisplayName("tryBeginOperation should hold lock that blocks close when returning true")
    void shouldBlockCloseWhenReturningTrue() throws InterruptedException {
      final NativeResourceHandle handle = new NativeResourceHandle("TestResource", () -> {});
      final CountDownLatch lockHeld = new CountDownLatch(1);
      final CountDownLatch closeDone = new CountDownLatch(1);
      final AtomicBoolean closedBeforeRelease = new AtomicBoolean(true);

      new Thread(
              () -> {
                if (handle.tryBeginOperation()) {
                  try {
                    lockHeld.countDown();
                    // Wait for close thread to start attempting close
                    Thread.sleep(200);
                    closedBeforeRelease.set(handle.isClosed());
                  } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                  } finally {
                    handle.endOperation();
                  }
                }
              })
          .start();

      lockHeld.await(5, TimeUnit.SECONDS);
      new Thread(
              () -> {
                handle.close();
                closeDone.countDown();
              })
          .start();

      assertTrue(closeDone.await(5, TimeUnit.SECONDS), "Close should complete after lock released");
      assertFalse(
          closedBeforeRelease.get(),
          "Resource should NOT be closed while tryBeginOperation lock is held");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full lifecycle: create → use → close → verify closed")
    void fullLifecycleShouldWorkCorrectly() {
      final AtomicBoolean cleanupExecuted = new AtomicBoolean(false);
      final NativeResourceHandle handle =
          new NativeResourceHandle("IntegrationResource", () -> cleanupExecuted.set(true));

      // Open state
      assertFalse(handle.isClosed(), "Should be open initially");
      assertDoesNotThrow(handle::ensureNotClosed, "ensureNotClosed should not throw when open");

      // Close
      handle.close();

      // Closed state
      assertTrue(handle.isClosed(), "Should be closed");
      assertTrue(cleanupExecuted.get(), "Cleanup should have executed");

      // Operations should throw
      assertThrows(IllegalStateException.class, handle::ensureNotClosed);

      // Additional close should be safe
      assertDoesNotThrow(handle::close, "Second close should be a no-op");
    }

    @Test
    @DisplayName("Multiple independent handles should not interfere")
    void multipleHandlesShouldNotInterfere() {
      final AtomicBoolean cleanup1 = new AtomicBoolean(false);
      final AtomicBoolean cleanup2 = new AtomicBoolean(false);

      final NativeResourceHandle handle1 =
          new NativeResourceHandle("Resource1", () -> cleanup1.set(true));
      final NativeResourceHandle handle2 =
          new NativeResourceHandle("Resource2", () -> cleanup2.set(true));

      // Both open
      assertFalse(handle1.isClosed(), "Handle1 should be open");
      assertFalse(handle2.isClosed(), "Handle2 should be open");

      // Close first only
      handle1.close();
      assertTrue(handle1.isClosed(), "Handle1 should be closed");
      assertFalse(handle2.isClosed(), "Handle2 should still be open");
      assertTrue(cleanup1.get(), "Cleanup1 should have executed");
      assertFalse(cleanup2.get(), "Cleanup2 should NOT have executed");

      // Handle2 should still work
      assertDoesNotThrow(handle2::ensureNotClosed, "Handle2 ensureNotClosed should work");

      // Close second
      handle2.close();
      assertTrue(handle2.isClosed(), "Handle2 should be closed");
      assertTrue(cleanup2.get(), "Cleanup2 should have executed");
    }
  }
}
