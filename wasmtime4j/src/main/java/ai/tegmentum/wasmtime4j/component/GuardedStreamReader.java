package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A lifecycle-guarded wrapper around a {@link StreamReader}.
 *
 * <p>Provides use-after-close protection for StreamReader instances. All method calls on a closed
 * GuardedStreamReader throw {@link IllegalStateException}.
 *
 * @since 1.1.0
 */
public final class GuardedStreamReader implements StreamReader {

  private final StreamReader delegate;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a guarded wrapper around the given StreamReader.
   *
   * @param delegate the StreamReader to guard
   * @throws IllegalArgumentException if delegate is null
   */
  public GuardedStreamReader(final StreamReader delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("delegate cannot be null");
    }
    this.delegate = delegate;
  }

  @Override
  public long getHandle() {
    checkNotClosed();
    return delegate.getHandle();
  }

  @Override
  public CompletableFuture<StreamResult> readAsync(final int maxCount) throws WasmException {
    checkNotClosed();
    return delegate.readAsync(maxCount);
  }

  @Override
  public void cancelRead() throws WasmException {
    checkNotClosed();
    delegate.cancelRead();
  }

  @Override
  public boolean isClosed() {
    if (closed.get()) {
      return true;
    }
    return delegate.isClosed();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      delegate.close();
    }
  }

  /**
   * Checks if this guarded reader has been explicitly closed via {@link #close()}.
   *
   * @return true if closed via close()
   */
  public boolean isExplicitlyClosed() {
    return closed.get();
  }

  private void checkNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("StreamReader has been closed");
    }
  }
}
