package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Default implementation of {@link JoinHandle} backed by a {@link CompletableFuture}.
 *
 * @param <T> the result type of the spawned task
 */
final class DefaultJoinHandle<T> implements JoinHandle<T> {

  private final CompletableFuture<T> future;

  DefaultJoinHandle(final CompletableFuture<T> future) {
    if (future == null) {
      throw new IllegalArgumentException("future cannot be null");
    }
    this.future = future;
  }

  @Override
  public T join() throws WasmException, InterruptedException {
    try {
      return future.get();
    } catch (final ExecutionException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof WasmException) {
        throw (WasmException) cause;
      }
      if (cause instanceof CompletionException && cause.getCause() instanceof WasmException) {
        throw (WasmException) cause.getCause();
      }
      throw new WasmException("Concurrent task failed: " + cause.getMessage(), cause);
    } catch (final CancellationException e) {
      throw new WasmException("Concurrent task was cancelled", e);
    }
  }

  @Override
  public CompletableFuture<T> toFuture() {
    return future;
  }

  @Override
  public boolean cancel() {
    return future.cancel(true);
  }

  @Override
  public boolean isDone() {
    return future.isDone();
  }
}
