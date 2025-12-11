package ai.tegmentum.wasmtime4j.concurrent;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A handle to a spawned concurrent task.
 *
 * <p>JoinHandle represents a spawned asynchronous task and provides methods to wait for
 * its completion, cancel it, or check its status. This corresponds to wasmtime's
 * {@code JoinHandle<T>} type.
 *
 * <p>Example usage:
 * <pre>{@code
 * JoinHandle<Integer> handle = store.spawn(() -> {
 *     // Concurrent work
 *     return computeResult();
 * });
 *
 * // Do other work while the task runs...
 *
 * // Wait for the result
 * Integer result = handle.join();
 * }</pre>
 *
 * @param <T> the type of the task result
 * @since 1.0.0
 */
public interface JoinHandle<T> {

  /**
   * Waits for the task to complete and returns the result.
   *
   * <p>This method blocks until the task completes, either successfully or with an error.
   *
   * @return the task result
   * @throws WasmException if the task failed or was cancelled
   * @throws InterruptedException if the current thread was interrupted while waiting
   * @since 1.0.0
   */
  T join() throws WasmException, InterruptedException;

  /**
   * Waits for the task to complete with a timeout.
   *
   * <p>This method blocks until the task completes or the timeout expires.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @return the task result
   * @throws WasmException if the task failed, was cancelled, or timed out
   * @throws InterruptedException if the current thread was interrupted while waiting
   * @since 1.0.0
   */
  T join(final long timeout, final TimeUnit unit) throws WasmException, InterruptedException;

  /**
   * Returns a CompletableFuture representing this task.
   *
   * <p>The returned future will complete when the task completes, allowing integration
   * with Java's async programming model.
   *
   * @return a CompletableFuture for the task result
   * @since 1.0.0
   */
  CompletableFuture<T> toFuture();

  /**
   * Checks if the task has completed.
   *
   * <p>A task is considered done when it has finished (successfully or with an error),
   * or has been cancelled.
   *
   * @return true if the task is done, false if still running
   * @since 1.0.0
   */
  boolean isDone();

  /**
   * Checks if the task was cancelled.
   *
   * @return true if the task was cancelled
   * @since 1.0.0
   */
  boolean isCancelled();

  /**
   * Attempts to cancel the task.
   *
   * <p>If the task has not yet started, it will be prevented from running. If the task
   * is already running, the cancellation attempt may or may not succeed depending on
   * the task's implementation.
   *
   * @param mayInterruptIfRunning if true, attempt to interrupt running tasks
   * @return true if the task was successfully cancelled
   * @since 1.0.0
   */
  boolean cancel(final boolean mayInterruptIfRunning);

  /**
   * Gets the unique identifier for this task handle.
   *
   * <p>This ID can be used for logging and tracking purposes.
   *
   * @return the task handle ID
   * @since 1.0.0
   */
  long getId();

  /**
   * Gets the current status of the task.
   *
   * @return the task status
   * @since 1.0.0
   */
  TaskStatus getStatus();

  /**
   * Represents the status of a concurrent task.
   *
   * @since 1.0.0
   */
  enum TaskStatus {
    /** The task is waiting to be executed. */
    PENDING,
    /** The task is currently running. */
    RUNNING,
    /** The task completed successfully. */
    COMPLETED,
    /** The task failed with an error. */
    FAILED,
    /** The task was cancelled. */
    CANCELLED
  }
}
