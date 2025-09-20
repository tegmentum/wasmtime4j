package ai.tegmentum.wasmtime4j.wasi.extensions.threading;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.TimeUnit;

/**
 * A condition variable for thread coordination in WASI threading.
 *
 * <p>A condition variable allows threads to wait for specific conditions to be met and to be
 * notified when those conditions change. Condition variables must be used in conjunction with a
 * mutex to ensure proper synchronization.
 *
 * <p>The typical usage pattern is:
 *
 * <ol>
 *   <li>Acquire the associated mutex
 *   <li>Check the condition
 *   <li>If condition is not met, wait on the condition variable
 *   <li>When notified, re-check the condition (spurious wakeups possible)
 *   <li>Process the condition change
 *   <li>Release the mutex
 * </ol>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiMutex mutex = threading.createMutex();
 * WasiConditionVariable condition = threading.createConditionVariable();
 * boolean dataReady = false;
 *
 * // Consumer thread
 * try (WasiMutex.Lock lock = mutex.lock()) {
 *     while (!dataReady) {
 *         condition.await(lock);
 *     }
 *     // Process data
 * }
 *
 * // Producer thread
 * try (WasiMutex.Lock lock = mutex.lock()) {
 *     // Prepare data
 *     dataReady = true;
 *     condition.signal();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiConditionVariable {

  /**
   * Waits until signaled or interrupted.
   *
   * <p>This method causes the current thread to wait until it is signaled or interrupted. The
   * associated mutex lock is automatically released while waiting and re-acquired before returning.
   *
   * <p><strong>Important:</strong> This method should always be called in a loop that checks the
   * condition, as spurious wakeups are possible.
   *
   * @param mutexLock the mutex lock that must be held by the current thread
   * @throws WasmException if wait operation fails
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws IllegalArgumentException if mutexLock is null
   * @throws IllegalStateException if the current thread doesn't hold the mutex lock
   */
  void await(final WasiMutex.Lock mutexLock) throws WasmException, InterruptedException;

  /**
   * Waits until signaled, interrupted, or the specified timeout elapses.
   *
   * <p>This method is similar to {@link #await(WasiMutex.Lock)} but will return false if the
   * timeout elapses before a signal is received.
   *
   * @param mutexLock the mutex lock that must be held by the current thread
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout
   * @return true if signaled before timeout, false if timeout elapsed
   * @throws WasmException if wait operation fails
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws IllegalArgumentException if mutexLock is null, timeout is negative, or unit is null
   * @throws IllegalStateException if the current thread doesn't hold the mutex lock
   */
  boolean await(final WasiMutex.Lock mutexLock, final long timeout, final TimeUnit unit)
      throws WasmException, InterruptedException;

  /**
   * Waits until signaled, interrupted, or the specified deadline.
   *
   * <p>This method waits until signaled or until the specified absolute time (in milliseconds since
   * epoch) is reached.
   *
   * @param mutexLock the mutex lock that must be held by the current thread
   * @param deadlineMillis the absolute time to wait until (milliseconds since epoch)
   * @return true if signaled before deadline, false if deadline reached
   * @throws WasmException if wait operation fails
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws IllegalArgumentException if mutexLock is null or deadline is in the past
   * @throws IllegalStateException if the current thread doesn't hold the mutex lock
   */
  boolean awaitUntil(final WasiMutex.Lock mutexLock, final long deadlineMillis)
      throws WasmException, InterruptedException;

  /**
   * Waits until signaled or interrupted, ignoring interrupts.
   *
   * <p>This method is similar to {@link #await(WasiMutex.Lock)} but will not throw
   * InterruptedException. If the thread is interrupted while waiting, the interrupted status is
   * restored when the method returns.
   *
   * @param mutexLock the mutex lock that must be held by the current thread
   * @throws WasmException if wait operation fails
   * @throws IllegalArgumentException if mutexLock is null
   * @throws IllegalStateException if the current thread doesn't hold the mutex lock
   */
  void awaitUninterruptibly(final WasiMutex.Lock mutexLock) throws WasmException;

  /**
   * Signals one waiting thread.
   *
   * <p>This method wakes up one thread that is waiting on this condition variable. If multiple
   * threads are waiting, only one is awakened. The choice of which thread to wake up is
   * implementation-dependent.
   *
   * <p><strong>Note:</strong> The calling thread should hold the associated mutex when calling this
   * method, though it's not strictly required.
   *
   * @throws WasmException if signal operation fails
   */
  void signal() throws WasmException;

  /**
   * Signals all waiting threads.
   *
   * <p>This method wakes up all threads that are currently waiting on this condition variable. All
   * waiting threads will attempt to re-acquire the associated mutex when they wake up.
   *
   * @throws WasmException if broadcast operation fails
   */
  void signalAll() throws WasmException;

  /**
   * Gets the number of threads currently waiting on this condition variable.
   *
   * <p>This is an estimate and may not be accurate in highly concurrent scenarios.
   *
   * @return the approximate number of waiting threads
   */
  int getWaitingCount();

  /**
   * Checks if any threads are currently waiting on this condition variable.
   *
   * @return true if threads are waiting, false otherwise
   */
  boolean hasWaiters();

  /**
   * Gets statistics for this condition variable.
   *
   * <p>Returns information about waiting patterns, signal counts, and performance metrics.
   *
   * @return condition variable statistics object
   * @throws WasmException if statistics retrieval fails
   */
  WasiConditionVariableStats getStats() throws WasmException;
}
