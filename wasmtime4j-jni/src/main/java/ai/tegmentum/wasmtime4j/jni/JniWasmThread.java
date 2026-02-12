package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.concurrent.WasmThread;
import ai.tegmentum.wasmtime4j.concurrent.WasmThreadLocalStorage;
import ai.tegmentum.wasmtime4j.concurrent.WasmThreadState;
import ai.tegmentum.wasmtime4j.concurrent.WasmThreadStatistics;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * JNI implementation of WebAssembly thread for multi-threaded WebAssembly execution.
 *
 * <p>This implementation provides WebAssembly threading capabilities using JNI to interface with
 * the native Wasmtime threading runtime. It supports:
 *
 * <ul>
 *   <li>Thread spawning and lifecycle management
 *   <li>Shared memory access with atomic operations
 *   <li>Thread synchronization primitives
 *   <li>Thread-local storage
 *   <li>Performance monitoring and statistics
 * </ul>
 *
 * <p>All operations are implemented with comprehensive error handling and resource cleanup to
 * prevent memory leaks and ensure robust operation in production environments.
 *
 * @since 1.0.0
 */
public final class JniWasmThread implements WasmThread {

  /** Native thread handle pointer. */
  private final long nativeHandle;

  /** Thread identifier. */
  private final long threadId;

  /** Shared memory instance. */
  private final WasmMemory sharedMemory;

  /** Thread-local storage implementation. */
  private final WasmThreadLocalStorage threadLocalStorage;

  /** Whether this thread has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Current thread state. */
  private final AtomicReference<WasmThreadState> currentState =
      new AtomicReference<>(WasmThreadState.NEW);

  static {
    JniLibraryLoader.ensureLoaded();
  }

  /**
   * Creates a new JNI WebAssembly thread.
   *
   * @param nativeHandle native thread handle pointer
   * @param threadId unique thread identifier
   * @param sharedMemory shared memory instance
   * @throws WasmException if thread creation fails
   */
  public JniWasmThread(final long nativeHandle, final long threadId, final WasmMemory sharedMemory)
      throws WasmException {
    if (nativeHandle == 0) {
      throw new WasmException("Native thread handle cannot be null");
    }
    if (sharedMemory == null) {
      throw new WasmException("Shared memory cannot be null");
    }

    this.nativeHandle = nativeHandle;
    this.threadId = threadId;
    this.sharedMemory = sharedMemory;
    this.threadLocalStorage = new JniWasmThreadLocalStorage(nativeHandle);

    // Register for cleanup
    // TODO: JniResourceRegistry.register(this, nativeHandle);
  }

  @Override
  public long getThreadId() {
    return threadId;
  }

  @Override
  public WasmThreadState getState() {
    ensureNotClosed();
    try {
      final int stateValue = nativeGetState(nativeHandle);
      return WasmThreadState.values()[stateValue];
    } catch (final Exception e) {
      return WasmThreadState.ERROR;
    }
  }

  @Override
  public Future<WasmValue[]> executeFunction(final WasmFunction function, final WasmValue... args)
      throws WasmException {
    ensureNotClosed();
    validateNotNull(function, "function");
    validateNotNull(args, "args");

    if (getState() != WasmThreadState.NEW && getState() != WasmThreadState.TERMINATED) {
      throw new IllegalStateException(
          "Thread must be in NEW or TERMINATED state to execute function");
    }

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            currentState.set(WasmThreadState.RUNNING);

            // Execute function using the WasmFunction.call() API which handles
            // argument marshalling and result conversion internally
            final WasmValue[] result = function.call(args);

            currentState.set(WasmThreadState.TERMINATED);
            return result;

          } catch (final Exception e) {
            currentState.set(WasmThreadState.ERROR);
            throw new RuntimeException("Function execution failed", e);
          }
        });
  }

  @Override
  public <T> Future<T> executeOperation(final Supplier<T> operation) throws WasmException {
    ensureNotClosed();
    validateNotNull(operation, "operation");

    if (getState() != WasmThreadState.NEW && getState() != WasmThreadState.TERMINATED) {
      throw new IllegalStateException(
          "Thread must be in NEW or TERMINATED state to execute operation");
    }

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            currentState.set(WasmThreadState.RUNNING);
            final T result = operation.get();
            currentState.set(WasmThreadState.TERMINATED);
            return result;
          } catch (final Exception e) {
            currentState.set(WasmThreadState.ERROR);
            throw new RuntimeException("Operation execution failed", e);
          }
        });
  }

  @Override
  public void join() throws WasmException, InterruptedException {
    ensureNotClosed();

    try {
      nativeJoin(nativeHandle);
    } catch (final Exception e) {
      if (e instanceof InterruptedException) {
        throw (InterruptedException) e;
      }
      throw new WasmException("Thread join failed: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean join(final long timeoutMs) throws WasmException, InterruptedException {
    ensureNotClosed();

    if (timeoutMs < 0) {
      throw new IllegalArgumentException("Timeout must be non-negative");
    }

    try {
      return nativeJoinTimeout(nativeHandle, timeoutMs);
    } catch (final Exception e) {
      if (e instanceof InterruptedException) {
        throw (InterruptedException) e;
      }
      throw new WasmException("Thread join with timeout failed: " + e.getMessage(), e);
    }
  }

  @Override
  public CompletableFuture<Void> terminate() {
    if (closed.get()) {
      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.runAsync(
        () -> {
          try {
            nativeRequestTermination(nativeHandle);

            // Wait for termination to complete
            final long timeout = 30000; // 30 seconds
            final long startTime = System.currentTimeMillis();

            while (isAlive() && (System.currentTimeMillis() - startTime) < timeout) {
              Thread.sleep(100);
            }

            if (isAlive()) {
              // Force termination if cooperative termination failed
              forceTerminate();
            }

          } catch (final Exception e) {
            throw new RuntimeException("Thread termination failed", e);
          }
        });
  }

  @Override
  public void forceTerminate() throws WasmException {
    ensureNotClosed();

    try {
      nativeForceTerminate(nativeHandle);
      currentState.set(WasmThreadState.KILLED);
    } catch (final Exception e) {
      throw new WasmException("Force termination failed: " + e.getMessage(), e);
    }
  }

  @Override
  public WasmMemory getSharedMemory() {
    return sharedMemory;
  }

  @Override
  public WasmThreadLocalStorage getThreadLocalStorage() {
    return threadLocalStorage;
  }

  @Override
  public boolean isAlive() {
    if (closed.get()) {
      return false;
    }

    final WasmThreadState state = getState();
    return state != WasmThreadState.TERMINATED
        && state != WasmThreadState.ERROR
        && state != WasmThreadState.KILLED;
  }

  @Override
  public boolean isTerminationRequested() {
    ensureNotClosed();

    try {
      return nativeIsTerminationRequested(nativeHandle);
    } catch (final Exception e) {
      return false;
    }
  }

  @Override
  public WasmThreadStatistics getStatistics() {
    ensureNotClosed();

    try {
      final long[] stats = nativeGetStatistics(nativeHandle);
      return new WasmThreadStatistics(
          stats[0], // functionsExecuted
          stats[1], // totalExecutionTime
          stats[2], // atomicOperations
          stats[3], // memoryAccesses
          stats[4], // waitNotifyOperations
          stats[5] // peakMemoryUsage
          );
    } catch (final Exception e) {
      return new WasmThreadStatistics(0, 0, 0, 0, 0, 0);
    }
  }

  @Override
  public void close() throws WasmException {
    if (!closed.compareAndSet(false, true)) {
      return; // Already closed
    }

    try {
      // Request termination first
      if (isAlive()) {
        final CompletableFuture<Void> termination = terminate();
        try {
          termination.get(10, TimeUnit.SECONDS);
        } catch (final Exception e) {
          // Force termination if graceful termination failed
          try {
            forceTerminate();
          } catch (final Exception ignored) {
            // Ignore errors during force termination
          }
        }
      }

      // Clean up native resources
      nativeDestroy(nativeHandle);

      // Unregister from cleanup
      // TODO: JniResourceRegistry.unregister(nativeHandle);

    } catch (final Exception e) {
      throw new WasmException("Failed to close thread: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the native handle for this thread (internal use only).
   *
   * @return native handle pointer
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  /**
   * Ensures this thread has not been closed.
   *
   * @throws IllegalStateException if the thread has been closed
   */
  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("Thread has been closed");
    }
  }

  /**
   * Validates that an object is not null.
   *
   * @param obj the object to validate
   * @param name the parameter name for error messages
   * @throws IllegalArgumentException if the object is null
   */
  private void validateNotNull(final Object obj, final String name) {
    if (obj == null) {
      throw new IllegalArgumentException(name + " cannot be null");
    }
  }

  // Native method declarations

  /**
   * Get the current state of the native thread.
   *
   * @param handle native thread handle
   * @return state value as integer
   */
  private static native int nativeGetState(long handle);

  /**
   * Join the native thread.
   *
   * @param handle native thread handle
   * @throws InterruptedException if the wait is interrupted
   */
  private static native void nativeJoin(long handle) throws InterruptedException;

  /**
   * Join the native thread with timeout.
   *
   * @param handle native thread handle
   * @param timeoutMs timeout in milliseconds
   * @return true if thread completed, false if timeout elapsed
   * @throws InterruptedException if the wait is interrupted
   */
  private static native boolean nativeJoinTimeout(long handle, long timeoutMs)
      throws InterruptedException;

  /**
   * Request termination of the native thread.
   *
   * @param handle native thread handle
   */
  private static native void nativeRequestTermination(long handle);

  /**
   * Force terminate the native thread.
   *
   * @param handle native thread handle
   */
  private static native void nativeForceTerminate(long handle);

  /**
   * Check if termination has been requested.
   *
   * @param handle native thread handle
   * @return true if termination requested
   */
  private static native boolean nativeIsTerminationRequested(long handle);

  /**
   * Get thread statistics.
   *
   * @param handle native thread handle
   * @return statistics array
   */
  private static native long[] nativeGetStatistics(long handle);

  /**
   * Destroy the native thread resources.
   *
   * @param handle native thread handle
   */
  private static native void nativeDestroy(long handle);
}
