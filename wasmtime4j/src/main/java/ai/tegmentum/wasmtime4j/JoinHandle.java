package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;

/**
 * Handle to a spawned concurrent task, providing the ability to join (await) its result.
 *
 * <p>A JoinHandle is returned by {@link Store#spawn(ConcurrentTask)} and represents a concurrent
 * computation running within the store. The handle can be used to:
 *
 * <ul>
 *   <li>Wait for the task to complete and retrieve its result via {@link #join()}
 *   <li>Obtain a future for the result via {@link #toFuture()}
 *   <li>Cancel the task if it is still running via {@link #cancel()}
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * JoinHandle<Integer> handle = store.spawn(s -> {
 *     Instance instance = s.createInstance(module);
 *     WasmFunction func = instance.getFunction("compute");
 *     return func.call(WasmValue.fromI32(100))[0].asI32();
 * });
 *
 * // Do other work...
 *
 * // Wait for the result
 * int result = handle.join();
 * }</pre>
 *
 * @param <T> the result type of the spawned task
 * @since 1.1.0
 */
public interface JoinHandle<T> {

  /**
   * Blocks until the spawned task completes and returns its result.
   *
   * <p>If the task has already completed, this returns immediately. If the task threw an exception,
   * this method rethrows it as a {@link WasmException}.
   *
   * @return the result of the spawned task
   * @throws WasmException if the task failed or was cancelled
   * @throws InterruptedException if the current thread was interrupted while waiting
   */
  T join() throws WasmException, InterruptedException;

  /**
   * Returns a {@link CompletableFuture} that completes with the task's result.
   *
   * <p>The future completes normally with the task's result, or completes exceptionally if the task
   * fails.
   *
   * @return a future representing the task's eventual result
   */
  CompletableFuture<T> toFuture();

  /**
   * Attempts to cancel the spawned task.
   *
   * <p>If the task has already completed, this method has no effect and returns false. If the task
   * is still running, it will be signalled for cancellation. Cancellation is cooperative — the task
   * may not be interrupted immediately.
   *
   * @return true if cancellation was successfully requested, false if the task already completed
   */
  boolean cancel();

  /**
   * Checks whether the spawned task has completed.
   *
   * @return true if the task has finished (either successfully, with an error, or cancelled)
   */
  boolean isDone();
}
