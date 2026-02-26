package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GuardedStreamReader} class.
 *
 * <p>GuardedStreamReader wraps a StreamReader with use-after-close protection.
 */
@DisplayName("GuardedStreamReader Tests")
class GuardedStreamReaderTest {

  /** Configurable StreamReader stub for testing delegation and lifecycle. */
  private static class TestStreamReader implements StreamReader {
    private final long handle;
    private boolean delegateClosed;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicInteger readCount = new AtomicInteger(0);
    private final AtomicInteger cancelCount = new AtomicInteger(0);

    TestStreamReader(final long handle) {
      this.handle = handle;
    }

    @Override
    public long getHandle() {
      return handle;
    }

    @Override
    public CompletableFuture<StreamResult> readAsync(final int maxCount) {
      readCount.incrementAndGet();
      return CompletableFuture.completedFuture(new StreamResult(Collections.emptyList(), false));
    }

    @Override
    public void cancelRead() {
      cancelCount.incrementAndGet();
    }

    @Override
    public boolean isClosed() {
      return delegateClosed;
    }

    void setDelegateClosed(final boolean delegateClosed) {
      this.delegateClosed = delegateClosed;
    }

    @Override
    public void close() {
      closed.set(true);
    }

    boolean isActuallyClosed() {
      return closed.get();
    }

    int getReadCount() {
      return readCount.get();
    }

    int getCancelCount() {
      return cancelCount.get();
    }
  }

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("should create guarded reader from delegate")
    void shouldCreateFromDelegate() {
      final TestStreamReader delegate = new TestStreamReader(100L);
      final GuardedStreamReader guarded = new GuardedStreamReader(delegate);
      assertFalse(
          guarded.isExplicitlyClosed(), "New guarded reader should not be explicitly closed");
    }

    @Test
    @DisplayName("should throw on null delegate")
    void shouldThrowOnNullDelegate() {
      assertThrows(IllegalArgumentException.class, () -> new GuardedStreamReader(null));
    }
  }

  @Nested
  @DisplayName("Delegation Tests")
  class DelegationTests {

    @Test
    @DisplayName("getHandle should delegate")
    void getHandleShouldDelegate() {
      final TestStreamReader delegate = new TestStreamReader(555L);
      final GuardedStreamReader guarded = new GuardedStreamReader(delegate);
      assertEquals(555L, guarded.getHandle());
    }

    @Test
    @DisplayName("readAsync should delegate")
    void readAsyncShouldDelegate() throws WasmException {
      final TestStreamReader delegate = new TestStreamReader(100L);
      final GuardedStreamReader guarded = new GuardedStreamReader(delegate);
      guarded.readAsync(10);
      assertEquals(1, delegate.getReadCount(), "readAsync should have been delegated");
    }

    @Test
    @DisplayName("cancelRead should delegate")
    void cancelReadShouldDelegate() throws WasmException {
      final TestStreamReader delegate = new TestStreamReader(100L);
      final GuardedStreamReader guarded = new GuardedStreamReader(delegate);
      guarded.cancelRead();
      assertEquals(1, delegate.getCancelCount(), "cancelRead should have been delegated");
    }

    @Test
    @DisplayName("isClosed should reflect delegate state when not explicitly closed")
    void isClosedShouldReflectDelegateState() {
      final TestStreamReader delegate = new TestStreamReader(100L);
      final GuardedStreamReader guarded = new GuardedStreamReader(delegate);
      assertFalse(guarded.isClosed(), "Should not be closed initially");

      delegate.setDelegateClosed(true);
      assertTrue(guarded.isClosed(), "Should reflect delegate closed state");
    }
  }

  @Nested
  @DisplayName("Use-After-Close Protection Tests")
  class UseAfterCloseProtectionTests {

    @Test
    @DisplayName("getHandle should throw after close")
    void getHandleShouldThrowAfterClose() {
      final TestStreamReader delegate = new TestStreamReader(100L);
      final GuardedStreamReader guarded = new GuardedStreamReader(delegate);
      guarded.close();
      assertThrows(IllegalStateException.class, guarded::getHandle);
    }

    @Test
    @DisplayName("readAsync should throw after close")
    void readAsyncShouldThrowAfterClose() {
      final TestStreamReader delegate = new TestStreamReader(100L);
      final GuardedStreamReader guarded = new GuardedStreamReader(delegate);
      guarded.close();
      assertThrows(IllegalStateException.class, () -> guarded.readAsync(10));
    }

    @Test
    @DisplayName("cancelRead should throw after close")
    void cancelReadShouldThrowAfterClose() {
      final TestStreamReader delegate = new TestStreamReader(100L);
      final GuardedStreamReader guarded = new GuardedStreamReader(delegate);
      guarded.close();
      assertThrows(IllegalStateException.class, guarded::cancelRead);
    }
  }

  @Nested
  @DisplayName("Close Behavior Tests")
  class CloseBehaviorTests {

    @Test
    @DisplayName("close should delegate to underlying reader")
    void closeShouldDelegate() {
      final TestStreamReader delegate = new TestStreamReader(100L);
      final GuardedStreamReader guarded = new GuardedStreamReader(delegate);
      guarded.close();
      assertTrue(delegate.isActuallyClosed(), "Close should delegate to underlying reader");
      assertTrue(guarded.isExplicitlyClosed(), "Should be marked as explicitly closed");
    }

    @Test
    @DisplayName("isClosed should return true after explicit close")
    void isClosedShouldReturnTrueAfterExplicitClose() {
      final TestStreamReader delegate = new TestStreamReader(100L);
      final GuardedStreamReader guarded = new GuardedStreamReader(delegate);
      guarded.close();
      assertTrue(guarded.isClosed(), "isClosed should return true after close");
    }

    @Test
    @DisplayName("double close should be safe")
    void doubleCloseShouldBeSafe() {
      final TestStreamReader delegate = new TestStreamReader(100L);
      final GuardedStreamReader guarded = new GuardedStreamReader(delegate);
      guarded.close();
      guarded.close(); // should not throw
      assertTrue(guarded.isExplicitlyClosed());
    }
  }
}
