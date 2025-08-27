package ai.tegmentum.wasmtime4j.jni.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniResource}.
 */
class JniResourceTest {

  /**
   * Test implementation of JniResource for testing purposes.
   */
  private static class TestResource extends JniResource {
    private final AtomicBoolean cleanupCalled = new AtomicBoolean(false);
    private final String resourceType;
    private final boolean throwOnCleanup;

    TestResource(final long nativeHandle, final String resourceType) {
      this(nativeHandle, resourceType, false);
    }

    TestResource(final long nativeHandle, final String resourceType, final boolean throwOnCleanup) {
      super(nativeHandle);
      this.resourceType = resourceType;
      this.throwOnCleanup = throwOnCleanup;
    }

    @Override
    protected void doClose() throws Exception {
      cleanupCalled.set(true);
      if (throwOnCleanup) {
        throw new RuntimeException("Cleanup failed for testing");
      }
    }

    @Override
    protected String getResourceType() {
      return resourceType;
    }

    boolean isCleanupCalled() {
      return cleanupCalled.get();
    }
  }

  @Test
  void testConstructorWithValidHandle() {
    final long validHandle = 12345L;
    final TestResource resource = new TestResource(validHandle, "Test");

    assertThat(resource.getNativeHandle()).isEqualTo(validHandle);
    assertThat(resource.getResourceType()).isEqualTo("Test");
    assertFalse(resource.isClosed());
    assertFalse(resource.isCleanupCalled());
  }

  @Test
  void testConstructorWithInvalidHandle() {
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> new TestResource(0L, "Test"));

    assertThat(exception.getMessage()).contains("nativeHandle");
    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testGetNativeHandleWhenOpen() {
    final long handle = 12345L;
    final TestResource resource = new TestResource(handle, "Test");

    assertThat(resource.getNativeHandle()).isEqualTo(handle);
  }

  @Test
  void testGetNativeHandleWhenClosed() {
    final TestResource resource = new TestResource(12345L, "Test");
    resource.close();

    final JniResourceException exception = assertThrows(JniResourceException.class,
        resource::getNativeHandle);

    assertThat(exception.getMessage()).contains("Test resource has been closed");
    assertThat(exception.getMessage()).contains("0x" + Long.toHexString(12345L));
  }

  @Test
  void testIsClosedInitialState() {
    final TestResource resource = new TestResource(12345L, "Test");
    assertFalse(resource.isClosed());
  }

  @Test
  void testIsClosedAfterClose() {
    final TestResource resource = new TestResource(12345L, "Test");
    resource.close();
    assertTrue(resource.isClosed());
  }

  @Test
  void testCloseCallsDoClose() {
    final TestResource resource = new TestResource(12345L, "Test");
    assertFalse(resource.isCleanupCalled());

    resource.close();

    assertTrue(resource.isCleanupCalled());
    assertTrue(resource.isClosed());
  }

  @Test
  void testCloseIsIdempotent() {
    final TestResource resource = new TestResource(12345L, "Test");

    resource.close();
    assertTrue(resource.isCleanupCalled());
    assertTrue(resource.isClosed());

    // Reset cleanup flag to test idempotent behavior
    resource.cleanupCalled.set(false);

    resource.close(); // Second call should not call doClose again

    assertFalse(resource.isCleanupCalled());
    assertTrue(resource.isClosed());
  }

  @Test
  void testCloseWithException() {
    final TestResource resource = new TestResource(12345L, "Test", true);

    // Should not throw exception even if doClose throws
    assertDoesNotThrow(resource::close);

    assertTrue(resource.isCleanupCalled());
    assertTrue(resource.isClosed());
  }

  @Test
  void testEnsureNotClosedWhenOpen() {
    final TestResource resource = new TestResource(12345L, "Test");

    assertDoesNotThrow(resource::ensureNotClosed);
  }

  @Test
  void testEnsureNotClosedWhenClosed() {
    final TestResource resource = new TestResource(12345L, "Test");
    resource.close();

    final JniResourceException exception = assertThrows(JniResourceException.class,
        resource::ensureNotClosed);

    assertThat(exception.getMessage()).contains("Test resource has been closed");
  }

  @Test
  void testToString() {
    final TestResource resource = new TestResource(0xABCDEFL, "TestResource");
    final String toString = resource.toString();

    assertThat(toString).contains("TestResource");
    assertThat(toString).contains("handle=0xabcdef");
    assertThat(toString).contains("closed=false");

    resource.close();
    final String toStringAfterClose = resource.toString();
    assertThat(toStringAfterClose).contains("closed=true");
  }

  @Test
  void testTryWithResourcesPattern() {
    final AtomicBoolean cleanupCalled = new AtomicBoolean(false);

    try (TestResource resource = new TestResource(12345L, "Test")) {
      assertFalse(resource.isClosed());
      assertThat(resource.getNativeHandle()).isEqualTo(12345L);
    }

    // Should be automatically closed due to try-with-resources
    // Note: We can't easily test this because we can't access the resource after the try block
    // but the compiler ensures close() is called
  }

  @Test
  void testMultipleResourcesWithTryWithResources() {
    assertDoesNotThrow(() -> {
      try (TestResource resource1 = new TestResource(11111L, "Test1");
           TestResource resource2 = new TestResource(22222L, "Test2")) {

        assertFalse(resource1.isClosed());
        assertFalse(resource2.isClosed());
        assertThat(resource1.getNativeHandle()).isEqualTo(11111L);
        assertThat(resource2.getNativeHandle()).isEqualTo(22222L);
      }
    });
  }

  @Test
  void testResourceTypeInErrorMessages() {
    final TestResource resource = new TestResource(12345L, "CustomType");
    resource.close();

    final JniResourceException exception = assertThrows(JniResourceException.class,
        resource::getNativeHandle);

    assertThat(exception.getMessage()).contains("CustomType resource has been closed");
  }

  @Test
  void testNativeHandleFormatting() {
    final TestResource resource = new TestResource(0x12345ABCL, "Test");
    final String toString = resource.toString();

    assertThat(toString).contains("handle=0x12345abc");
  }

  @Test
  void testConcurrentClose() {
    final TestResource resource = new TestResource(12345L, "Test");

    // Test concurrent closing doesn't cause issues
    final Thread[] threads = new Thread[5];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread(resource::close);
    }

    for (Thread thread : threads) {
      thread.start();
    }

    for (Thread thread : threads) {
      assertDoesNotThrow(() -> thread.join());
    }

    assertTrue(resource.isClosed());
  }

  @Test
  void testResourceCreationWithDifferentHandleValues() {
    // Test with various handle values
    final long[] testHandles = {1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 0xDEADBEEFL};

    for (long handle : testHandles) {
      if (handle == 0L) {
        continue; // Skip invalid handle
      }

      final TestResource resource = new TestResource(handle, "Test");
      assertThat(resource.getNativeHandle()).isEqualTo(handle);
      assertFalse(resource.isClosed());

      resource.close();
      assertTrue(resource.isClosed());
    }
  }
}