package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A lifecycle-guarded wrapper around a {@link FutureReader}.
 *
 * <p>Provides use-after-close protection for FutureReader instances. All method calls on a closed
 * GuardedFutureReader throw {@link IllegalStateException}.
 *
 * @since 1.1.0
 */
public final class GuardedFutureReader implements FutureReader {

  private final FutureReader delegate;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a guarded wrapper around the given FutureReader.
   *
   * @param delegate the FutureReader to guard
   * @throws IllegalArgumentException if delegate is null
   */
  public GuardedFutureReader(final FutureReader delegate) {
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
  public CompletableFuture<Optional<ComponentVal>> readAsync() throws WasmException {
    checkNotClosed();
    return delegate.readAsync();
  }

  @Override
  public void cancelRead() throws WasmException {
    checkNotClosed();
    delegate.cancelRead();
  }

  @Override
  public boolean isResolved() {
    checkNotClosed();
    return delegate.isResolved();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      delegate.close();
    }
  }

  /**
   * Checks if this guarded reader has been closed.
   *
   * @return true if closed
   */
  public boolean isClosed() {
    return closed.get();
  }

  private void checkNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("FutureReader has been closed");
    }
  }
}
