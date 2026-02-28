package ai.tegmentum.wasmtime4j.jni.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
  }

  @Nested
  @DisplayName("Constructor Validation")
  class ConstructorValidation {

    @Test
    @DisplayName("Zero handle should be rejected as null pointer")
    void zeroHandleShouldBeRejected() {
      assertThatThrownBy(() -> new TestJniResource(0L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("nativeHandle")
          .hasMessageContaining("null pointer");
    }

    @Test
    @DisplayName("Negative handle should be rejected as invalid")
    void negativeHandleShouldBeRejected() {
      assertThatThrownBy(() -> new TestJniResource(-1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("nativeHandle")
          .hasMessageContaining("negative value");
    }

    @Test
    @DisplayName("Valid handle should create resource successfully")
    void validHandleShouldCreateResource() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      assertThat(resource.getNativeHandle()).isEqualTo(VALID_HANDLE);
      assertThat(resource.isClosed()).isFalse();
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
      assertThat(resource.getCloseCount())
          .as("doClose() should be called exactly once")
          .isEqualTo(1);
    }

    @Test
    @DisplayName("close() should set isClosed() to true")
    void closeShouldSetClosedFlag() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      assertThat(resource.isClosed()).isFalse();
      resource.close();
      assertThat(resource.isClosed()).isTrue();
    }

    @Test
    @DisplayName("Double close should call doClose() only once (idempotent)")
    void doubleCloseShouldBeIdempotent() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.close();
      resource.close();
      resource.close();
      assertThat(resource.getCloseCount())
          .as("doClose() should still be called exactly once after multiple close() calls")
          .isEqualTo(1);
    }

    @Test
    @DisplayName("close() should swallow doClose() exceptions")
    void closeShouldSwallowExceptions() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.setCloseException(new RuntimeException("Simulated cleanup failure"));

      assertThatCode(resource::close)
          .as("close() should not propagate exceptions from doClose()")
          .doesNotThrowAnyException();
      assertThat(resource.isClosed()).isTrue();
    }

    @Test
    @DisplayName("markClosedForTesting() should set closed without calling doClose()")
    void markClosedForTestingShouldNotCallDoClose() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.markClosedForTesting();
      assertThat(resource.isClosed()).isTrue();
      assertThat(resource.getCloseCount())
          .as("doClose() should not be called by markClosedForTesting()")
          .isZero();
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

      assertThatThrownBy(resource::getNativeHandle)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("TestResource")
          .hasMessageContaining("closed")
          .hasMessageContaining(String.format("0x%x", VALID_HANDLE));
    }

    @Test
    @DisplayName("ensureNotClosed() should throw IllegalStateException after close")
    void ensureNotClosedShouldThrowAfterClose() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);
      resource.close();

      assertThatThrownBy(resource::ensureNotClosed)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("TestResource")
          .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("ensureNotClosed() should not throw when resource is open")
    void ensureNotClosedShouldNotThrowWhenOpen() {
      final TestJniResource resource = new TestJniResource(VALID_HANDLE);

      assertThatCode(resource::ensureNotClosed)
          .as("ensureNotClosed() should not throw for an open resource")
          .doesNotThrowAnyException();

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
      assertThat(doneLatch.await(10, TimeUnit.SECONDS))
          .as("All threads should complete within timeout")
          .isTrue();
      assertThat(resource.getCloseCount())
          .as("doClose() should be called exactly once despite concurrent close()")
          .isEqualTo(1);
      assertThat(resource.isClosed()).isTrue();
    }
  }

  @Nested
  @DisplayName("Static Utility Methods")
  class StaticUtilityMethods {

    @Test
    @DisplayName("isNativeHandleReasonable should return true at 4GB threshold")
    void isReasonableAtThreshold() {
      assertThat(JniResource.isNativeHandleReasonable(0x100000000L))
          .as("Handle at 4GB threshold (0x100000000) should be considered reasonable")
          .isTrue();
    }

    @Test
    @DisplayName("isNativeHandleReasonable should return false below 4GB threshold")
    void isNotReasonableBelowThreshold() {
      assertThat(JniResource.isNativeHandleReasonable(0xFFFFFFFFL))
          .as("Handle below 4GB threshold (0xFFFFFFFF) should not be considered reasonable")
          .isFalse();
    }

    @Test
    @DisplayName("isNativeHandleReasonable should return false for zero")
    void isNotReasonableForZero() {
      assertThat(JniResource.isNativeHandleReasonable(0L))
          .as("Zero handle should not be considered reasonable")
          .isFalse();
    }

    @Test
    @DisplayName("isNativeHandleReasonable should return true for large handle")
    void isReasonableForLargeHandle() {
      assertThat(JniResource.isNativeHandleReasonable(0x7F00_0000_0000L))
          .as("Large handle well above threshold should be considered reasonable")
          .isTrue();
    }

    @Test
    @DisplayName("isNativeHandleReasonable should return false for small test handles")
    void isNotReasonableForSmallTestHandles() {
      assertThat(JniResource.isNativeHandleReasonable(0x1111L))
          .as("Small test handle 0x1111 should not be considered reasonable")
          .isFalse();
      assertThat(JniResource.isNativeHandleReasonable(0x12345678L))
          .as("Test handle 0x12345678 should not be considered reasonable (below 4GB)")
          .isFalse();
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

      assertThat(str)
          .contains("TestResource")
          .contains(String.format("0x%x", VALID_HANDLE))
          .contains("closed=false");

      resource.close();
      final String closedStr = resource.toString();
      assertThat(closedStr).contains("closed=true");
    }
  }
}
