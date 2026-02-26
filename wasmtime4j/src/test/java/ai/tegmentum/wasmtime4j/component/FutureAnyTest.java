package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FutureAny} interface and its default implementation.
 *
 * <p>FutureAny is a type-erased handle for Component Model async futures.
 */
@DisplayName("FutureAny Tests")
class FutureAnyTest {

  /** A minimal FutureReader stub for testing FutureAny wrappers. */
  private static class StubFutureReader implements FutureReader {
    private final long handle;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    StubFutureReader(final long handle) {
      this.handle = handle;
    }

    @Override
    public long getHandle() {
      return handle;
    }

    @Override
    public CompletableFuture<Optional<ComponentVal>> readAsync() {
      return CompletableFuture.completedFuture(Optional.of(ComponentVal.s32(42)));
    }

    @Override
    public void cancelRead() {
      // no-op for stub
    }

    @Override
    public boolean isResolved() {
      return false;
    }

    @Override
    public void close() {
      closed.set(true);
    }

    boolean isClosed() {
      return closed.get();
    }
  }

  @Nested
  @DisplayName("tryFromFutureReader Tests")
  class TryFromFutureReaderTests {

    @Test
    @DisplayName("should create FutureAny from FutureReader")
    void shouldCreateFromFutureReader() {
      final StubFutureReader reader = new StubFutureReader(123L);
      final FutureAny futureAny = FutureAny.tryFromFutureReader(reader);
      assertNotNull(futureAny);
      assertEquals(123L, futureAny.getHandle());
    }

    @Test
    @DisplayName("should throw on null reader")
    void shouldThrowOnNullReader() {
      assertThrows(IllegalArgumentException.class, () -> FutureAny.tryFromFutureReader(null));
    }
  }

  @Nested
  @DisplayName("DefaultFutureAny Tests")
  class DefaultFutureAnyTests {

    @Test
    @DisplayName("should convert back to FutureReader")
    void shouldConvertBackToFutureReader() throws WasmException {
      final StubFutureReader reader = new StubFutureReader(456L);
      final FutureAny futureAny = FutureAny.tryFromFutureReader(reader);
      final FutureReader recovered = futureAny.tryIntoFutureReader();
      assertEquals(reader, recovered, "Should return the original reader");
    }

    @Test
    @DisplayName("should throw on tryIntoFutureReader after close")
    void shouldThrowOnTryIntoFutureReaderAfterClose() {
      final StubFutureReader reader = new StubFutureReader(789L);
      final FutureAny futureAny = FutureAny.tryFromFutureReader(reader);
      futureAny.close();
      assertThrows(WasmException.class, futureAny::tryIntoFutureReader);
    }

    @Test
    @DisplayName("close should delegate to reader")
    void closeShouldDelegateToReader() {
      final StubFutureReader reader = new StubFutureReader(100L);
      final FutureAny futureAny = FutureAny.tryFromFutureReader(reader);
      futureAny.close();
      assertTrue(reader.isClosed(), "Reader should be closed after FutureAny.close()");
    }

    @Test
    @DisplayName("double close should be safe")
    void doubleCloseShouldBeSafe() {
      final StubFutureReader reader = new StubFutureReader(200L);
      final FutureAny futureAny = FutureAny.tryFromFutureReader(reader);
      futureAny.close();
      futureAny.close(); // should not throw
      assertTrue(reader.isClosed());
    }
  }
}
