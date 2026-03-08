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
package ai.tegmentum.wasmtime4j.jni.util;

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
 * Tests for {@link JniResource}.
 *
 * <p>Verifies constructor validation, close lifecycle idempotency, state after close, thread
 * safety, and static utility methods. Uses a concrete test subclass with a tracked {@code
 * doClose()} to test lifecycle without native calls.
 */
@DisplayName("JniResource Tests")
class JniResourceTest {

  private static final long VALID_HANDLE = 0x12345678L;

  /**
   * Concrete test subclass of JniResource that tracks doClose() calls without requiring native
   * resources.
   */
  private static final class TestJniResource extends JniResource {
    private final AtomicInteger closeCount = new AtomicInteger(0);
    private volatile RuntimeException closeException;

    TestJniResource(final long nativeHandle) {
      super(nativeHandle);
    }

    @Override
    protected void doClose() {
      closeCount.incrementAndGet();
      if (closeException != null) {
        throw closeException;
      }
    }

    @Override
    protected String getResourceType() {
      return "TestResource";
    }

    int getCloseCount() {
      return closeCount.get();
    }

    void setCloseException(final RuntimeException exception) {
      this.closeException = exception;
    }

    void exposedBeginOperation() {
      beginOperation();
    }

    void exposedEndOperation() {
      endOperation();
    }

    boolean exposedTryBeginOperation() {
      return tryBeginOperation();
    }
  }

  @Nested
  @DisplayName("Constructor Validation")
  class ConstructorValidation {

    @Test
    @DisplayName("Zero handle should be rejected as null pointer")
    void zeroHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> new TestJniResource(0L));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("null pointer"), "Expected message to contain: null pointer");
    }

    @Test
    @DisplayName("Negative handle should be rejected as invalid")
    void negativeHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> new TestJniResource(-1L));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("negative value"), "Expected message to contain: negative value");
    }

    @Test
    @DisplayName("Valid handle should create resource successfully")
    void validHandleShouldCreateResource() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      assertEquals(VALID_HANDLE, resource.getNativeHandle());
      assertFalse(resource.isClosed());
      resource.markClosedForTesting();
    }
  }

  @Nested
  @DisplayName("Close Lifecycle")
  class CloseLifecycle {

    @Test
    @DisplayName("close() should call doClose() exactly once")
    void closeShouldCallDoCloseOnce() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.close();
      assertEquals(1, resource.getCloseCount(), "doClose() should be called exactly once");
    }

    @Test
    @DisplayName("close() should set isClosed() to true")
    void closeShouldSetClosedFlag() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      assertFalse(resource.isClosed());
      resource.close();
      assertTrue(resource.isClosed());
    }

    @Test
    @DisplayName("Double close should call doClose() only once (idempotent)")
    void doubleCloseShouldBeIdempotent() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.close();
      resource.close();
      resource.close();
      assertEquals(
          1,
          resource.getCloseCount(),
          "doClose() should still be called exactly once after multiple close() calls");
    }

    @Test
    @DisplayName("close() should swallow doClose() exceptions")
    void closeShouldSwallowExceptions() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.setCloseException(new RuntimeException("Simulated cleanup failure"));

      assertDoesNotThrow(resource::close, "close() should not propagate exceptions from doClose()");
      assertTrue(resource.isClosed());
    }

    @Test
    @DisplayName("markClosedForTesting() should set closed without calling doClose()")
    void markClosedForTestingShouldNotCallDoClose() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.markClosedForTesting();
      assertTrue(resource.isClosed());
      assertEquals(
          0, resource.getCloseCount(), "doClose() should not be called by markClosedForTesting()");
    }
  }

  @Nested
  @DisplayName("State After Close")
  class StateAfterClose {

    @Test
    @DisplayName("getNativeHandle() should throw IllegalStateException after close")
    void getNativeHandleShouldThrowAfterClose() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.close();

      IllegalStateException e =
          assertThrows(IllegalStateException.class, resource::getNativeHandle);
      assertTrue(
          e.getMessage().contains("TestResource"), "Expected message to contain: TestResource");
      assertTrue(e.getMessage().contains("closed"), "Expected message to contain: closed");
      assertTrue(
          e.getMessage().contains(String.format("0x%x", VALID_HANDLE)),
          "Expected message to contain: 0x" + Long.toHexString(VALID_HANDLE));
    }

    @Test
    @DisplayName("ensureNotClosed() should throw IllegalStateException after close")
    void ensureNotClosedShouldThrowAfterClose() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.close();

      IllegalStateException e =
          assertThrows(IllegalStateException.class, resource::ensureNotClosed);
      assertTrue(
          e.getMessage().contains("TestResource"), "Expected message to contain: TestResource");
      assertTrue(e.getMessage().contains("closed"), "Expected message to contain: closed");
    }

    @Test
    @DisplayName("ensureNotClosed() should not throw when resource is open")
    void ensureNotClosedShouldNotThrowWhenOpen() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);

      assertDoesNotThrow(
          resource::ensureNotClosed, "ensureNotClosed() should not throw for an open resource");

      resource.markClosedForTesting();
    }
  }

  @Nested
  @DisplayName("Thread Safety")
  class ThreadSafety {

    @Test
    @DisplayName("Concurrent close from multiple threads should call doClose() exactly once")
    void concurrentCloseShouldCallDoCloseOnce() throws InterruptedException {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      final int threadCount = 8;
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(threadCount);

      for (int i = 0; i < threadCount; i++) {
        new Thread(
                () -> {
                  try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    resource.close();
                  } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                  } finally {
                    doneLatch.countDown();
                  }
                })
            .start();
      }

      startLatch.countDown();
      assertTrue(
          doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete within timeout");
      assertEquals(
          1,
          resource.getCloseCount(),
          "doClose() should be called exactly once despite concurrent close()");
      assertTrue(resource.isClosed());
    }
  }

  @Nested
  @DisplayName("Static Utility Methods")
  class StaticUtilityMethods {

    @Test
    @DisplayName("isNativeHandleReasonable should return true at 4GB threshold")
    void isReasonableAtThreshold() {
      assertTrue(
          JniResource.isNativeHandleReasonable(0x100000000L),
          "Handle at 4GB threshold (0x100000000) should be considered reasonable");
    }

    @Test
    @DisplayName("isNativeHandleReasonable should return false below 4GB threshold")
    void isNotReasonableBelowThreshold() {
      assertFalse(
          JniResource.isNativeHandleReasonable(0xFFFFFFFFL),
          "Handle below 4GB threshold (0xFFFFFFFF) should not be considered reasonable");
    }

    @Test
    @DisplayName("isNativeHandleReasonable should return false for zero")
    void isNotReasonableForZero() {
      assertFalse(
          JniResource.isNativeHandleReasonable(0L),
          "Zero handle should not be considered reasonable");
    }

    @Test
    @DisplayName("isNativeHandleReasonable should return true for large handle")
    void isReasonableForLargeHandle() {
      assertTrue(
          JniResource.isNativeHandleReasonable(0x7F00_0000_0000L),
          "Large handle well above threshold should be considered reasonable");
    }

    @Test
    @DisplayName("isNativeHandleReasonable should return false for small test handles")
    void isNotReasonableForSmallTestHandles() {
      assertFalse(
          JniResource.isNativeHandleReasonable(0x1111L),
          "Small test handle 0x1111 should not be considered reasonable");
      assertFalse(
          JniResource.isNativeHandleReasonable(0x12345678L),
          "Test handle 0x12345678 should not be considered reasonable (below 4GB)");
    }
  }

  @Nested
  @DisplayName("beginOperation / endOperation")
  class BeginEndOperationTests {

    @Test
    @DisplayName("beginOperation should succeed on open resource")
    void beginOperationShouldSucceedWhenOpen() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      assertDoesNotThrow(
          resource::exposedBeginOperation, "beginOperation() should not throw on an open resource");
      resource.exposedEndOperation();
      resource.close();
    }

    @Test
    @DisplayName("beginOperation should throw IllegalStateException on closed resource")
    void beginOperationShouldThrowWhenClosed() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.close();

      IllegalStateException e =
          assertThrows(IllegalStateException.class, resource::exposedBeginOperation);
      assertTrue(
          e.getMessage().contains("TestResource"), "Expected message to contain: TestResource");
      assertTrue(e.getMessage().contains("closed"), "Expected message to contain: closed");
      assertTrue(
          e.getMessage().contains(String.format("0x%x", VALID_HANDLE)),
          "Expected message to contain: 0x" + Long.toHexString(VALID_HANDLE));
    }

    @Test
    @DisplayName("endOperation should release lock so close can proceed")
    void endOperationShouldReleaseLock() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.exposedBeginOperation();
      resource.exposedEndOperation();

      assertDoesNotThrow(
          resource::close, "close() should succeed after endOperation() releases the read lock");
      assertTrue(resource.isClosed());
    }

    @Test
    @DisplayName("Multiple concurrent beginOperation calls should share read lock")
    void multipleConcurrentBeginOperationsShouldShareReadLock() throws InterruptedException {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      final int threadCount = 8;
      final CountDownLatch allLocked = new CountDownLatch(threadCount);
      final CountDownLatch releaseLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(threadCount);
      final AtomicInteger errors = new AtomicInteger(0);

      for (int i = 0; i < threadCount; i++) {
        new Thread(
                () -> {
                  try {
                    resource.exposedBeginOperation();
                    allLocked.countDown();
                    releaseLatch.await(5, TimeUnit.SECONDS);
                  } catch (final Exception e) {
                    errors.incrementAndGet();
                  } finally {
                    resource.exposedEndOperation();
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
      assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
      resource.close();
    }

    @Test
    @DisplayName("close should block while beginOperation is held, preventing use-after-free")
    void closeShouldBlockWhileOperationInFlight() throws InterruptedException {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      final CountDownLatch operationStarted = new CountDownLatch(1);
      final CountDownLatch closeAttempted = new CountDownLatch(1);
      final CountDownLatch operationDone = new CountDownLatch(1);
      final AtomicBoolean closedBeforeOperationEnd = new AtomicBoolean(true);

      // Thread 1: holds beginOperation lock
      new Thread(
              () -> {
                resource.exposedBeginOperation();
                try {
                  operationStarted.countDown();
                  closeAttempted.await(5, TimeUnit.SECONDS);
                  // Give close() thread time to block on write lock
                  Thread.sleep(100);
                  closedBeforeOperationEnd.set(resource.isClosed());
                } catch (final InterruptedException e) {
                  Thread.currentThread().interrupt();
                } finally {
                  resource.exposedEndOperation();
                  operationDone.countDown();
                }
              })
          .start();

      operationStarted.await(5, TimeUnit.SECONDS);

      // Thread 2: attempts close() while operation is in-flight
      new Thread(
              () -> {
                closeAttempted.countDown();
                resource.close();
              })
          .start();

      assertTrue(operationDone.await(5, TimeUnit.SECONDS), "Operation thread should complete");

      assertFalse(
          closedBeforeOperationEnd.get(),
          "Resource should NOT be closed while operation read lock is held");
      assertTrue(
          resource.isClosed(), "Resource should be closed after operation released the lock");
    }

    @Test
    @DisplayName("beginOperation should be reentrant (read lock reentrancy)")
    void beginOperationShouldBeReentrant() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.exposedBeginOperation();
      try {
        // Second call should not deadlock — ReentrantReadWriteLock allows read reentrancy
        resource.exposedBeginOperation();
        try {
          assertFalse(resource.isClosed());
        } finally {
          resource.exposedEndOperation();
        }
      } finally {
        resource.exposedEndOperation();
      }
      resource.close();
    }
  }

  @Nested
  @DisplayName("tryBeginOperation")
  class TryBeginOperationTests {

    @Test
    @DisplayName("tryBeginOperation should return true on open resource")
    void shouldReturnTrueWhenOpen() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);

      final boolean result = resource.exposedTryBeginOperation();

      assertTrue(result, "tryBeginOperation() should return true for open resource");
      resource.exposedEndOperation();
      resource.close();
    }

    @Test
    @DisplayName("tryBeginOperation should return false on closed resource without throwing")
    void shouldReturnFalseWhenClosed() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.close();

      final boolean result = resource.exposedTryBeginOperation();

      assertFalse(result, "tryBeginOperation() should return false for closed resource");
      // No endOperation() needed when tryBeginOperation returns false
    }

    @Test
    @DisplayName("tryBeginOperation should hold lock that blocks close when returning true")
    void shouldBlockCloseWhenReturningTrue() throws InterruptedException {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      final CountDownLatch lockHeld = new CountDownLatch(1);
      final CountDownLatch closeDone = new CountDownLatch(1);
      final AtomicBoolean closedBeforeRelease = new AtomicBoolean(true);

      new Thread(
              () -> {
                if (resource.exposedTryBeginOperation()) {
                  try {
                    lockHeld.countDown();
                    // Wait for close thread to start attempting close
                    Thread.sleep(200);
                    closedBeforeRelease.set(resource.isClosed());
                  } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                  } finally {
                    resource.exposedEndOperation();
                  }
                }
              })
          .start();

      lockHeld.await(5, TimeUnit.SECONDS);
      new Thread(
              () -> {
                resource.close();
                closeDone.countDown();
              })
          .start();

      assertTrue(closeDone.await(5, TimeUnit.SECONDS));
      assertFalse(
          closedBeforeRelease.get(),
          "Resource should NOT be closed while tryBeginOperation lock is held");
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTests {

    @Test
    @DisplayName("toString should include resource type, handle, and closed state")
    void toStringShouldIncludeDetails() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      final String str = resource.toString();

      assertTrue(str.contains("TestResource"), "Expected string to contain: TestResource");
      assertTrue(
          str.contains(String.format("0x%x", VALID_HANDLE)),
          "Expected string to contain: 0x" + Long.toHexString(VALID_HANDLE));
      assertTrue(str.contains("closed=false"), "Expected string to contain: closed=false");

      resource.close();
      final String closedStr = resource.toString();
      assertTrue(closedStr.contains("closed=true"), "Expected string to contain: closed=true");
    }
  }
}
