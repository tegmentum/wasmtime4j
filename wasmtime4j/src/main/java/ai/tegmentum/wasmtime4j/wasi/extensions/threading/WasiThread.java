package ai.tegmentum.wasmtime4j.wasi.extensions.threading;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a thread in the WASI threading system.
 *
 * <p>A WasiThread provides control over thread execution, lifecycle management,
 * and state monitoring. Threads are created through the {@link ai.tegmentum.wasmtime4j.wasi.extensions.WasiThreading}
 * interface and can be started, joined, and interrupted.
 *
 * <p>Thread lifecycle:
 * <ol>
 *   <li>NEW - Thread created but not started</li>
 *   <li>RUNNABLE - Thread is executing or ready to execute</li>
 *   <li>BLOCKED - Thread is blocked waiting for a monitor lock</li>
 *   <li>WAITING - Thread is waiting indefinitely for another thread</li>
 *   <li>TIMED_WAITING - Thread is waiting for a specified period</li>
 *   <li>TERMINATED - Thread has completed execution</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * WasiThread thread = threading.createThread(() -> {
 *     System.out.println("Thread is running");
 *     // Perform work...
 * }, "WorkerThread");
 *
 * thread.start();
 * thread.join(); // Wait for completion
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiThread {

  /**
   * Starts the execution of this thread.
   *
   * <p>The thread begins executing the target Runnable that was specified
   * when the thread was created. A thread can only be started once.
   *
   * @throws WasmException if thread start fails
   * @throws IllegalStateException if thread has already been started
   */
  void start() throws WasmException;

  /**
   * Waits for this thread to terminate.
   *
   * <p>This method blocks the calling thread until this thread completes
   * execution. If this thread has already terminated, this method returns
   * immediately.
   *
   * @throws WasmException if join operation fails
   * @throws InterruptedException if the calling thread is interrupted while waiting
   */
  void join() throws WasmException, InterruptedException;

  /**
   * Waits at most the specified time for this thread to terminate.
   *
   * <p>This method blocks the calling thread for up to the specified timeout
   * period waiting for this thread to complete. If the timeout elapses
   * before the thread terminates, the method returns false.
   *
   * @param timeoutMillis the maximum time to wait in milliseconds
   * @return true if the thread terminated within the timeout, false if timeout elapsed
   * @throws WasmException if join operation fails
   * @throws InterruptedException if the calling thread is interrupted while waiting
   * @throws IllegalArgumentException if timeoutMillis is negative
   */
  boolean join(final long timeoutMillis) throws WasmException, InterruptedException;

  /**
   * Interrupts this thread.
   *
   * <p>This method sets the interrupted status of the thread and may cause
   * the thread to throw an InterruptedException if it's blocked in certain
   * operations (sleep, wait, join, etc.).
   *
   * @throws WasmException if interrupt operation fails
   */
  void interrupt() throws WasmException;

  /**
   * Tests whether this thread has been interrupted.
   *
   * <p>This method returns the interrupted status of the thread but does
   * not clear the interrupted status.
   *
   * @return true if this thread has been interrupted, false otherwise
   */
  boolean isInterrupted();

  /**
   * Gets the current state of this thread.
   *
   * @return the thread state
   */
  ThreadState getState();

  /**
   * Gets the name of this thread.
   *
   * @return the thread name
   */
  String getName();

  /**
   * Sets the name of this thread.
   *
   * <p>The thread name is used for debugging and monitoring purposes.
   * It should be descriptive of the thread's purpose.
   *
   * @param name the new thread name
   * @throws IllegalArgumentException if name is null
   * @throws WasmException if name change fails
   */
  void setName(final String name) throws WasmException;

  /**
   * Gets the unique identifier for this thread.
   *
   * @return the thread ID
   */
  long getId();

  /**
   * Gets the priority of this thread.
   *
   * <p>Thread priority is a hint to the thread scheduler about the relative
   * importance of threads. Higher priority threads may receive more CPU time.
   *
   * @return the thread priority (1-10, where 5 is normal)
   */
  int getPriority();

  /**
   * Sets the priority of this thread.
   *
   * <p>Priority changes may not take effect immediately and the actual
   * scheduling behavior depends on the underlying system.
   *
   * @param priority the new thread priority (1-10)
   * @throws IllegalArgumentException if priority is not between 1 and 10
   * @throws WasmException if priority change fails
   */
  void setPriority(final int priority) throws WasmException;

  /**
   * Tests whether this thread is alive.
   *
   * <p>A thread is alive if it has been started and has not yet terminated.
   * This includes threads that are currently running, blocked, or waiting.
   *
   * @return true if this thread is alive, false otherwise
   */
  boolean isAlive();

  /**
   * Tests whether this thread is a daemon thread.
   *
   * <p>Daemon threads do not prevent the JVM from exiting when all
   * non-daemon threads have terminated.
   *
   * @return true if this thread is a daemon thread, false otherwise
   */
  boolean isDaemon();

  /**
   * Sets whether this thread should be a daemon thread.
   *
   * <p>This method must be called before the thread is started.
   * Daemon threads do not prevent application termination.
   *
   * @param daemon true to make this thread a daemon, false otherwise
   * @throws IllegalStateException if this thread has already been started
   * @throws WasmException if daemon status change fails
   */
  void setDaemon(final boolean daemon) throws WasmException;

  /**
   * Gets the thread group that this thread belongs to.
   *
   * @return the thread group name, or null if not in a group
   */
  String getThreadGroup();

  /**
   * Gets the stack size for this thread.
   *
   * <p>Returns the approximate stack size in bytes. A value of 0 indicates
   * that the stack size is system-dependent or unknown.
   *
   * @return the stack size in bytes, or 0 if unknown
   */
  long getStackSize();

  /**
   * Gets statistics for this thread.
   *
   * <p>Returns detailed information about thread execution including
   * CPU time, blocked time, and other performance metrics.
   *
   * @return thread statistics object
   * @throws WasmException if statistics retrieval fails
   */
  WasiThreadStats getStats() throws WasmException;

  /**
   * Gets the exception handler for uncaught exceptions in this thread.
   *
   * @return the uncaught exception handler, or null if none set
   */
  Thread.UncaughtExceptionHandler getUncaughtExceptionHandler();

  /**
   * Sets the exception handler for uncaught exceptions in this thread.
   *
   * <p>The handler will be called when this thread terminates due to
   * an uncaught exception.
   *
   * @param handler the uncaught exception handler
   * @throws WasmException if handler setting fails
   */
  void setUncaughtExceptionHandler(final Thread.UncaughtExceptionHandler handler) throws WasmException;
}