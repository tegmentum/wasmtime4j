package ai.tegmentum.wasmtime4j.wasi.extensions;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.extensions.threading.WasiConditionVariable;
import ai.tegmentum.wasmtime4j.wasi.extensions.threading.WasiMutex;
import ai.tegmentum.wasmtime4j.wasi.extensions.threading.WasiSemaphore;
import ai.tegmentum.wasmtime4j.wasi.extensions.threading.WasiThread;
import ai.tegmentum.wasmtime4j.wasi.extensions.threading.WasiThreadPool;
import java.util.concurrent.CompletableFuture;

/**
 * WASI Threading extension interface providing thread management and synchronization capabilities.
 *
 * <p>This interface extends WASI with comprehensive threading support including:
 * <ul>
 *   <li>Thread creation and management</li>
 *   <li>Synchronization primitives (mutexes, condition variables, semaphores)</li>
 *   <li>Thread pools and async execution</li>
 *   <li>Inter-thread communication</li>
 * </ul>
 *
 * <p>All threading operations respect capability-based security and require appropriate
 * permissions to be granted through the WASI security policy. Thread creation and
 * synchronization operations are subject to resource limits.
 *
 * <p>Example usage:
 * <pre>{@code
 * try (WasiContext context = WasiFactory.createContext()) {
 *     WasiThreading threading = context.getThreading();
 *
 *     // Create a mutex for shared resource protection
 *     WasiMutex mutex = threading.createMutex();
 *
 *     // Create and start a worker thread
 *     WasiThread worker = threading.createThread(() -> {
 *         // Worker thread logic
 *         System.out.println("Worker thread executing");
 *     });
 *     worker.start();
 *
 *     // Wait for completion
 *     worker.join();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiThreading {

  /**
   * Creates a new thread that will execute the specified target when started.
   *
   * <p>The thread is created in a suspended state and must be explicitly started
   * using {@link WasiThread#start()}. The target Runnable will be executed in
   * the context of the new thread.
   *
   * @param target the code to execute in the new thread
   * @return a new WasiThread instance
   * @throws WasmException if thread creation fails or permission is denied
   * @throws IllegalArgumentException if target is null
   */
  WasiThread createThread(final Runnable target) throws WasmException;

  /**
   * Creates a new thread with the specified name.
   *
   * <p>Named threads are useful for debugging and monitoring. The name
   * should be descriptive of the thread's purpose.
   *
   * @param target the code to execute in the new thread
   * @param name the name for the new thread
   * @return a new WasiThread instance
   * @throws WasmException if thread creation fails or permission is denied
   * @throws IllegalArgumentException if target or name is null
   */
  WasiThread createThread(final Runnable target, final String name) throws WasmException;

  /**
   * Creates a new mutex for protecting shared resources.
   *
   * <p>Mutexes provide exclusive access to shared resources, ensuring that
   * only one thread can hold the lock at a time. They support both blocking
   * and non-blocking lock acquisition.
   *
   * @return a new WasiMutex instance
   * @throws WasmException if mutex creation fails or permission is denied
   */
  WasiMutex createMutex() throws WasmException;

  /**
   * Creates a new condition variable for thread coordination.
   *
   * <p>Condition variables allow threads to wait for specific conditions
   * and be notified when those conditions are met. They must be used in
   * conjunction with a mutex.
   *
   * @return a new WasiConditionVariable instance
   * @throws WasmException if condition variable creation fails or permission is denied
   */
  WasiConditionVariable createConditionVariable() throws WasmException;

  /**
   * Creates a new semaphore with the specified number of permits.
   *
   * <p>Semaphores control access to a limited number of resources by
   * maintaining a count of available permits. Threads can acquire
   * and release permits to access the protected resources.
   *
   * @param permits the initial number of permits (must be non-negative)
   * @return a new WasiSemaphore instance
   * @throws WasmException if semaphore creation fails or permission is denied
   * @throws IllegalArgumentException if permits is negative
   */
  WasiSemaphore createSemaphore(final int permits) throws WasmException;

  /**
   * Creates a new thread pool with the specified core and maximum sizes.
   *
   * <p>Thread pools manage a collection of worker threads that can execute
   * tasks submitted to them. They provide efficient task execution by
   * reusing threads rather than creating new ones for each task.
   *
   * @param coreSize the number of threads to keep in the pool (must be positive)
   * @param maxSize the maximum number of threads allowed in the pool
   * @return a new WasiThreadPool instance
   * @throws WasmException if thread pool creation fails or permission is denied
   * @throws IllegalArgumentException if sizes are invalid (coreSize < 1 or maxSize < coreSize)
   */
  WasiThreadPool createThreadPool(final int coreSize, final int maxSize) throws WasmException;

  /**
   * Executes a task asynchronously using the default thread pool.
   *
   * <p>This method provides a convenient way to execute tasks asynchronously
   * without explicitly managing thread pools. The task is executed in a
   * background thread and the returned CompletableFuture can be used to
   * track completion.
   *
   * @param task the task to execute asynchronously
   * @return a CompletableFuture that will complete when the task finishes
   * @throws IllegalArgumentException if task is null
   * @throws IllegalStateException if threading is not available
   */
  CompletableFuture<Void> executeAsync(final Runnable task);

  /**
   * Executes a task asynchronously and returns a result.
   *
   * <p>Similar to {@link #executeAsync(Runnable)} but for tasks that return
   * a value. The result is available through the returned CompletableFuture.
   *
   * @param <T> the type of the result
   * @param task the task to execute that returns a value
   * @return a CompletableFuture containing the task result
   * @throws IllegalArgumentException if task is null
   * @throws IllegalStateException if threading is not available
   */
  <T> CompletableFuture<T> executeAsync(final java.util.concurrent.Callable<T> task);

  /**
   * Gets the current thread as a WasiThread.
   *
   * <p>This method returns a WasiThread representation of the currently
   * executing thread, allowing access to WASI-specific thread operations.
   *
   * @return the current thread
   * @throws WasmException if current thread access fails
   */
  WasiThread getCurrentThread() throws WasmException;

  /**
   * Gets the number of available processor cores.
   *
   * <p>This information can be used to determine optimal thread pool sizes
   * and parallelism levels for the current system.
   *
   * @return the number of available processor cores
   */
  int getAvailableProcessors();

  /**
   * Gets the maximum number of threads that can be created.
   *
   * <p>This limit is imposed by the WASI security policy and system resources.
   * Applications should respect this limit to avoid resource exhaustion.
   *
   * @return the maximum thread count, or -1 if unlimited
   */
  int getMaxThreadCount();

  /**
   * Gets the current number of active threads.
   *
   * <p>This includes all threads created through this threading interface
   * that are currently alive (started but not yet terminated).
   *
   * @return the current active thread count
   */
  int getActiveThreadCount();

  /**
   * Checks if threading capabilities are available and properly initialized.
   *
   * <p>This method can be used to verify that the threading extension is
   * functional and that the necessary permissions have been granted.
   *
   * @return true if threading is available, false otherwise
   */
  boolean isAvailable();

  /**
   * Gets threading statistics and usage information.
   *
   * <p>This method returns information about thread usage, synchronization
   * statistics, and performance metrics for the threading subsystem.
   *
   * @return threading statistics object
   * @throws WasmException if statistics retrieval fails
   */
  WasiThreadingStats getStats() throws WasmException;

  /**
   * Causes the currently executing thread to sleep for the specified duration.
   *
   * <p>This is a convenience method for thread sleep operations that respects
   * WASI timing constraints and interruption policies.
   *
   * @param millis the number of milliseconds to sleep
   * @throws WasmException if sleep operation fails
   * @throws IllegalArgumentException if millis is negative
   * @throws InterruptedException if the thread is interrupted while sleeping
   */
  void sleep(final long millis) throws WasmException, InterruptedException;

  /**
   * Yields the current thread's execution to allow other threads to run.
   *
   * <p>This is a hint to the thread scheduler that the current thread is
   * willing to give up its current use of the processor.
   */
  void yield();
}