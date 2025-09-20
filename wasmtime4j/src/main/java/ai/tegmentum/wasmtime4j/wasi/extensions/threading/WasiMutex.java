package ai.tegmentum.wasmtime4j.wasi.extensions.threading;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.TimeUnit;

/**
 * A mutual exclusion primitive for protecting shared resources in WASI threading.
 *
 * <p>A WasiMutex provides exclusive access to shared resources, ensuring that only one thread can
 * hold the lock at a time. This prevents race conditions and ensures data consistency in
 * multi-threaded applications.
 *
 * <p>Mutexes support both blocking and non-blocking lock acquisition, as well as timed lock
 * attempts. They should be used with try-with-resources or explicit unlock calls to ensure proper
 * resource management.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiMutex mutex = threading.createMutex();
 *
 * // Option 1: Try-with-resources (recommended)
 * try (WasiMutex.Lock lock = mutex.lock()) {
 *     // Critical section - only one thread can execute this
 *     sharedResource.modify();
 * }
 *
 * // Option 2: Manual lock/unlock
 * mutex.lock();
 * try {
 *     sharedResource.modify();
 * } finally {
 *     mutex.unlock();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiMutex {

  /**
   * Acquires the lock, blocking if necessary until it becomes available.
   *
   * <p>If the lock is not available, the calling thread will block until the lock is released by
   * another thread. If the current thread is interrupted while waiting, an InterruptedException is
   * thrown.
   *
   * @return a Lock object that implements AutoCloseable for use with try-with-resources
   * @throws WasmException if lock acquisition fails
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  Lock lock() throws WasmException, InterruptedException;

  /**
   * Acquires the lock without blocking.
   *
   * <p>If the lock is available, it is acquired immediately and the method returns a Lock object.
   * If the lock is not available, the method returns null immediately without blocking.
   *
   * @return a Lock object if the lock was acquired, null otherwise
   * @throws WasmException if lock acquisition fails
   */
  Lock tryLock() throws WasmException;

  /**
   * Attempts to acquire the lock within the specified time.
   *
   * <p>If the lock is available immediately, it is acquired and the method returns a Lock object.
   * Otherwise, the thread waits up to the specified time for the lock to become available.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout
   * @return a Lock object if the lock was acquired within the timeout, null otherwise
   * @throws WasmException if lock acquisition fails
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws IllegalArgumentException if timeout is negative or unit is null
   */
  Lock tryLock(final long timeout, final TimeUnit unit) throws WasmException, InterruptedException;

  /**
   * Releases the lock.
   *
   * <p>This method releases the lock held by the current thread. If the current thread does not
   * hold the lock, an exception is thrown.
   *
   * <p><strong>Note:</strong> It is recommended to use the Lock object returned by lock() methods
   * instead of calling this method directly.
   *
   * @throws WasmException if unlock fails or current thread doesn't hold the lock
   * @throws IllegalStateException if the current thread doesn't hold the lock
   */
  void unlock() throws WasmException;

  /**
   * Checks if the lock is currently held by any thread.
   *
   * @return true if the lock is held, false otherwise
   */
  boolean isLocked();

  /**
   * Checks if the lock is held by the current thread.
   *
   * @return true if the current thread holds the lock, false otherwise
   */
  boolean isHeldByCurrentThread();

  /**
   * Gets the number of threads currently waiting to acquire this lock.
   *
   * <p>This is an estimate and may not be accurate in highly concurrent scenarios.
   *
   * @return the approximate number of waiting threads
   */
  int getQueueLength();

  /**
   * Gets the thread that currently holds this lock.
   *
   * @return the thread holding the lock, or null if not locked
   */
  WasiThread getOwner();

  /**
   * Gets statistics for this mutex.
   *
   * <p>Returns information about lock usage, contention, and performance metrics.
   *
   * @return mutex statistics object
   * @throws WasmException if statistics retrieval fails
   */
  WasiMutexStats getStats() throws WasmException;

  /**
   * Represents a held lock that can be released automatically.
   *
   * <p>This interface extends AutoCloseable to support try-with-resources syntax, ensuring that
   * locks are properly released even if exceptions occur.
   */
  interface Lock extends AutoCloseable {

    /**
     * Checks if this lock is still valid and held.
     *
     * @return true if the lock is still held, false if already released
     */
    boolean isValid();

    /**
     * Gets the time when this lock was acquired.
     *
     * @return the acquisition timestamp in milliseconds since epoch
     */
    long getAcquisitionTime();

    /**
     * Gets the thread that holds this lock.
     *
     * @return the thread holding the lock
     */
    WasiThread getHolder();

    /**
     * Releases the lock.
     *
     * <p>After calling this method, the lock becomes invalid and should not be used. This method
     * can be called multiple times safely.
     */
    @Override
    void close();
  }
}
