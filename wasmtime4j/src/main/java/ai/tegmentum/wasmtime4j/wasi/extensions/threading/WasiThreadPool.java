package ai.tegmentum.wasmtime4j.wasi.extensions.threading;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A thread pool for managing and executing tasks in WASI threading.
 *
 * <p>A thread pool maintains a collection of worker threads that can execute submitted tasks. This
 * provides efficient task execution by reusing threads rather than creating new ones for each task,
 * reducing the overhead of thread creation and destruction.
 *
 * <p>Thread pools support:
 *
 * <ul>
 *   <li>Configurable core and maximum pool sizes
 *   <li>Task queuing when all threads are busy
 *   <li>Automatic thread lifecycle management
 *   <li>Graceful shutdown with pending task completion
 *   <li>Both synchronous and asynchronous task execution
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiThreadPool pool = threading.createThreadPool(4, 8);
 *
 * // Submit a task for asynchronous execution
 * CompletableFuture<String> future = pool.submit(() -> {
 *     // Perform some computation
 *     return "Result";
 * });
 *
 * // Get the result
 * String result = future.get();
 *
 * // Submit a task without return value
 * pool.execute(() -> {
 *     System.out.println("Task executed");
 * });
 *
 * // Shutdown when done
 * pool.shutdown();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiThreadPool extends AutoCloseable {

  /**
   * Executes a task without returning a result.
   *
   * <p>The task is submitted for execution and will be run by one of the worker threads when
   * available. This method returns immediately without waiting for task completion.
   *
   * @param task the task to execute
   * @throws WasmException if task submission fails
   * @throws IllegalArgumentException if task is null
   * @throws IllegalStateException if the thread pool is shut down
   */
  void execute(final Runnable task) throws WasmException;

  /**
   * Submits a task for execution and returns a CompletableFuture.
   *
   * <p>The task is submitted for execution and a CompletableFuture is returned that will be
   * completed when the task finishes. The future can be used to retrieve the result or handle
   * exceptions.
   *
   * @param task the task to submit
   * @return a CompletableFuture that will be completed when the task finishes
   * @throws IllegalArgumentException if task is null
   * @throws IllegalStateException if the thread pool is shut down
   */
  CompletableFuture<Void> submit(final Runnable task);

  /**
   * Submits a task that returns a result.
   *
   * <p>The task is submitted for execution and a CompletableFuture is returned that will contain
   * the result when the task completes successfully.
   *
   * @param <T> the type of the result
   * @param task the task to submit
   * @return a CompletableFuture containing the result
   * @throws IllegalArgumentException if task is null
   * @throws IllegalStateException if the thread pool is shut down
   */
  <T> CompletableFuture<T> submit(final Callable<T> task);

  /**
   * Submits a task with a specific result value.
   *
   * <p>The task is executed and the provided result value is returned in the CompletableFuture upon
   * successful completion.
   *
   * @param <T> the type of the result
   * @param task the task to submit
   * @param result the result value to return upon completion
   * @return a CompletableFuture containing the result
   * @throws IllegalArgumentException if task is null
   * @throws IllegalStateException if the thread pool is shut down
   */
  <T> CompletableFuture<T> submit(final Runnable task, final T result);

  /**
   * Initiates an orderly shutdown of the thread pool.
   *
   * <p>Previously submitted tasks are executed, but no new tasks are accepted. This method does not
   * wait for previously submitted tasks to complete. Use {@link #awaitTermination(long, TimeUnit)}
   * to wait for completion.
   *
   * @throws WasmException if shutdown initiation fails
   */
  void shutdown() throws WasmException;

  /**
   * Attempts to stop all actively executing tasks and returns a list of waiting tasks.
   *
   * <p>This method does not guarantee that actively executing tasks will stop, but makes a best
   * effort attempt to interrupt them. Tasks that were submitted but not yet started are returned.
   *
   * @return list of tasks that were waiting to be executed
   * @throws WasmException if shutdown fails
   */
  java.util.List<Runnable> shutdownNow() throws WasmException;

  /**
   * Blocks until all tasks have completed after a shutdown request.
   *
   * <p>This method blocks until either all tasks complete, the timeout occurs, or the current
   * thread is interrupted, whichever happens first.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout
   * @return true if termination completed within timeout, false if timeout elapsed
   * @throws WasmException if await operation fails
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws IllegalArgumentException if timeout is negative or unit is null
   */
  boolean awaitTermination(final long timeout, final TimeUnit unit)
      throws WasmException, InterruptedException;

  /**
   * Checks if the thread pool has been shut down.
   *
   * @return true if shutdown has been initiated, false otherwise
   */
  boolean isShutdown();

  /**
   * Checks if all tasks have completed after shutdown.
   *
   * @return true if all tasks completed after shutdown, false otherwise
   */
  boolean isTerminated();

  /**
   * Gets the core number of threads in the pool.
   *
   * <p>The core size represents the minimum number of threads that are kept alive even when idle.
   *
   * @return the core pool size
   */
  int getCorePoolSize();

  /**
   * Sets the core number of threads in the pool.
   *
   * @param corePoolSize the new core pool size (must be positive)
   * @throws IllegalArgumentException if corePoolSize is not positive
   * @throws WasmException if the change fails
   */
  void setCorePoolSize(final int corePoolSize) throws WasmException;

  /**
   * Gets the maximum allowed number of threads in the pool.
   *
   * @return the maximum pool size
   */
  int getMaximumPoolSize();

  /**
   * Sets the maximum allowed number of threads in the pool.
   *
   * @param maximumPoolSize the new maximum pool size
   * @throws IllegalArgumentException if maximumPoolSize is less than core pool size
   * @throws WasmException if the change fails
   */
  void setMaximumPoolSize(final int maximumPoolSize) throws WasmException;

  /**
   * Gets the current number of threads in the pool.
   *
   * @return the current pool size
   */
  int getPoolSize();

  /**
   * Gets the approximate number of threads that are actively executing tasks.
   *
   * @return the active thread count
   */
  int getActiveCount();

  /**
   * Gets the largest number of threads that have ever been in the pool.
   *
   * @return the largest pool size
   */
  int getLargestPoolSize();

  /**
   * Gets the approximate total number of tasks that have been submitted.
   *
   * @return the task count
   */
  long getTaskCount();

  /**
   * Gets the approximate total number of tasks that have completed execution.
   *
   * @return the completed task count
   */
  long getCompletedTaskCount();

  /**
   * Gets the current number of tasks waiting to be executed.
   *
   * @return the queue size
   */
  int getQueueSize();

  /**
   * Gets the keep-alive time for idle threads.
   *
   * <p>Threads that are idle for longer than this time may be terminated if the pool size exceeds
   * the core size.
   *
   * @param unit the time unit for the return value
   * @return the keep-alive time in the specified unit
   */
  long getKeepAliveTime(final TimeUnit unit);

  /**
   * Sets the keep-alive time for idle threads.
   *
   * @param time the new keep-alive time
   * @param unit the time unit
   * @throws IllegalArgumentException if time is negative or unit is null
   * @throws WasmException if the change fails
   */
  void setKeepAliveTime(final long time, final TimeUnit unit) throws WasmException;

  /**
   * Gets statistics for this thread pool.
   *
   * <p>Returns detailed information about thread pool usage, task execution patterns, and
   * performance metrics.
   *
   * @return thread pool statistics object
   * @throws WasmException if statistics retrieval fails
   */
  WasiThreadPoolStats getStats() throws WasmException;

  /**
   * Closes the thread pool and releases all resources.
   *
   * <p>This method calls {@link #shutdown()} and then waits for termination. It's provided for
   * AutoCloseable support in try-with-resources.
   */
  @Override
  void close();
}
