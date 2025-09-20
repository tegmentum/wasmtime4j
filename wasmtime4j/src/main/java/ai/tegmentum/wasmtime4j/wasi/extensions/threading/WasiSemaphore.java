package ai.tegmentum.wasmtime4j.wasi.extensions.threading;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.TimeUnit;

/**
 * A counting semaphore for controlling access to limited resources in WASI threading.
 *
 * <p>A semaphore maintains a count of permits that represent available resources.
 * Threads can acquire permits to access resources and release permits when done.
 * If no permits are available, threads will block until permits become available.
 *
 * <p>Semaphores are useful for:
 * <ul>
 *   <li>Limiting the number of threads that can access a resource simultaneously</li>
 *   <li>Implementing producer-consumer patterns</li>
 *   <li>Rate limiting and throttling</li>
 *   <li>Resource pooling</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a semaphore with 3 permits (e.g., for a connection pool of size 3)
 * WasiSemaphore semaphore = threading.createSemaphore(3);
 *
 * // Acquire a permit
 * semaphore.acquire();
 * try {
 *     // Use the limited resource
 *     useResource();
 * } finally {
 *     // Always release the permit
 *     semaphore.release();
 * }
 *
 * // Or use try-with-resources
 * try (WasiSemaphore.Permit permit = semaphore.acquirePermit()) {
 *     useResource();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiSemaphore {

  /**
   * Acquires a permit, blocking if necessary until one becomes available.
   *
   * <p>If a permit is available, it is acquired and the method returns immediately.
   * If no permit is available, the thread blocks until a permit is released
   * by another thread or the thread is interrupted.
   *
   * @throws WasmException if permit acquisition fails
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  void acquire() throws WasmException, InterruptedException;

  /**
   * Acquires the specified number of permits, blocking if necessary.
   *
   * <p>This method acquires the given number of permits atomically. If fewer
   * permits are available, the thread blocks until enough permits become available.
   *
   * @param permits the number of permits to acquire (must be positive)
   * @throws WasmException if permit acquisition fails
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws IllegalArgumentException if permits is not positive
   */
  void acquire(final int permits) throws WasmException, InterruptedException;

  /**
   * Acquires a permit without blocking.
   *
   * <p>If a permit is available, it is acquired and the method returns true.
   * If no permit is available, the method returns false immediately without blocking.
   *
   * @return true if a permit was acquired, false otherwise
   * @throws WasmException if permit acquisition fails
   */
  boolean tryAcquire() throws WasmException;

  /**
   * Attempts to acquire the specified number of permits without blocking.
   *
   * @param permits the number of permits to acquire (must be positive)
   * @return true if the permits were acquired, false otherwise
   * @throws WasmException if permit acquisition fails
   * @throws IllegalArgumentException if permits is not positive
   */
  boolean tryAcquire(final int permits) throws WasmException;

  /**
   * Attempts to acquire a permit within the specified time.
   *
   * <p>If a permit is available immediately, it is acquired and the method
   * returns true. Otherwise, the thread waits up to the specified time
   * for a permit to become available.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout
   * @return true if a permit was acquired within the timeout, false otherwise
   * @throws WasmException if permit acquisition fails
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws IllegalArgumentException if timeout is negative or unit is null
   */
  boolean tryAcquire(final long timeout, final TimeUnit unit) throws WasmException, InterruptedException;

  /**
   * Attempts to acquire the specified number of permits within the specified time.
   *
   * @param permits the number of permits to acquire (must be positive)
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout
   * @return true if the permits were acquired within the timeout, false otherwise
   * @throws WasmException if permit acquisition fails
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws IllegalArgumentException if permits is not positive, timeout is negative, or unit is null
   */
  boolean tryAcquire(final int permits, final long timeout, final TimeUnit unit)
      throws WasmException, InterruptedException;

  /**
   * Acquires a permit and returns a Permit object for use with try-with-resources.
   *
   * <p>This method is equivalent to {@link #acquire()} but returns a Permit
   * object that implements AutoCloseable, allowing for automatic permit release.
   *
   * @return a Permit object that will automatically release the permit when closed
   * @throws WasmException if permit acquisition fails
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  Permit acquirePermit() throws WasmException, InterruptedException;

  /**
   * Attempts to acquire a permit and returns a Permit object if successful.
   *
   * @return a Permit object if acquisition succeeded, null otherwise
   * @throws WasmException if permit acquisition fails
   */
  Permit tryAcquirePermit() throws WasmException;

  /**
   * Releases a permit, making it available for other threads.
   *
   * <p>This method increases the number of available permits by one.
   * If threads are waiting for permits, one will be awakened.
   *
   * @throws WasmException if permit release fails
   */
  void release() throws WasmException;

  /**
   * Releases the specified number of permits.
   *
   * <p>This method increases the number of available permits by the specified amount.
   * Waiting threads will be awakened as permits become available.
   *
   * @param permits the number of permits to release (must be positive)
   * @throws WasmException if permit release fails
   * @throws IllegalArgumentException if permits is not positive
   */
  void release(final int permits) throws WasmException;

  /**
   * Gets the current number of available permits.
   *
   * <p>This method returns the current number of permits available for acquisition.
   * The value may change immediately after this method returns due to concurrent operations.
   *
   * @return the number of available permits
   */
  int availablePermits();

  /**
   * Acquires all available permits and returns the number acquired.
   *
   * <p>This method acquires all permits that are currently available
   * and returns the number of permits that were acquired.
   *
   * @return the number of permits acquired
   * @throws WasmException if permit acquisition fails
   */
  int drainPermits() throws WasmException;

  /**
   * Gets the number of threads currently waiting to acquire permits.
   *
   * <p>This is an estimate and may not be accurate in highly concurrent scenarios.
   *
   * @return the approximate number of waiting threads
   */
  int getQueueLength();

  /**
   * Checks if any threads are currently waiting to acquire permits.
   *
   * @return true if threads are waiting, false otherwise
   */
  boolean hasQueuedThreads();

  /**
   * Gets statistics for this semaphore.
   *
   * <p>Returns information about permit usage, waiting patterns,
   * and performance metrics.
   *
   * @return semaphore statistics object
   * @throws WasmException if statistics retrieval fails
   */
  WasiSemaphoreStats getStats() throws WasmException;

  /**
   * Represents a permit that can be released automatically.
   *
   * <p>This interface extends AutoCloseable to support try-with-resources
   * syntax, ensuring that permits are properly released even if exceptions occur.
   */
  interface Permit extends AutoCloseable {

    /**
     * Checks if this permit is still valid and held.
     *
     * @return true if the permit is still held, false if already released
     */
    boolean isValid();

    /**
     * Gets the time when this permit was acquired.
     *
     * @return the acquisition timestamp in milliseconds since epoch
     */
    long getAcquisitionTime();

    /**
     * Gets the thread that holds this permit.
     *
     * @return the thread holding the permit
     */
    WasiThread getHolder();

    /**
     * Releases the permit.
     *
     * <p>After calling this method, the permit becomes invalid and should
     * not be used. This method can be called multiple times safely.
     */
    @Override
    void close();
  }
}