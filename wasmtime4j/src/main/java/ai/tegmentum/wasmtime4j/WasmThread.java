package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Represents a WebAssembly thread for multi-threaded WebAssembly execution.
 *
 * <p>This interface provides the abstraction for WebAssembly threads, which enable concurrent
 * execution of WebAssembly code with shared memory and atomic operations. WebAssembly threads are
 * implemented using the WebAssembly threads proposal, which includes:
 *
 * <ul>
 *   <li>Shared linear memory between threads
 *   <li>Atomic memory operations for synchronization
 *   <li>Thread spawning and lifecycle management
 *   <li>Wait/notify operations for thread coordination
 * </ul>
 *
 * <p>WebAssembly threads are distinct from Java threads - they represent execution contexts within
 * the WebAssembly runtime that can share memory and coordinate through WebAssembly-specific atomic
 * operations.
 *
 * @since 1.0.0
 */
public interface WasmThread extends AutoCloseable {

  /**
   * Gets the unique identifier for this WebAssembly thread.
   *
   * @return the thread ID
   */
  long getThreadId();

  /**
   * Gets the current state of this WebAssembly thread.
   *
   * @return the thread state
   */
  WasmThreadState getState();

  /**
   * Executes a function in this WebAssembly thread.
   *
   * <p>The function is executed asynchronously in the WebAssembly thread context, with access to
   * shared memory and the ability to coordinate with other WebAssembly threads.
   *
   * @param function the WebAssembly function to execute
   * @param args the function arguments
   * @return a future representing the function execution result
   * @throws WasmException if the function execution fails
   * @throws IllegalStateException if the thread is not in a runnable state
   */
  Future<WasmValue[]> executeFunction(WasmFunction function, WasmValue... args)
      throws WasmException;

  /**
   * Executes a callable operation in this WebAssembly thread context.
   *
   * <p>This method allows executing arbitrary operations within the thread's execution context,
   * providing access to the thread's memory and synchronization capabilities.
   *
   * @param <T> the return type of the operation
   * @param operation the operation to execute
   * @return a future representing the operation result
   * @throws WasmException if the operation execution fails
   * @throws IllegalStateException if the thread is not in a runnable state
   */
  <T> Future<T> executeOperation(Supplier<T> operation) throws WasmException;

  /**
   * Waits for this thread to complete execution.
   *
   * <p>This method blocks until the WebAssembly thread has finished executing or encounters an
   * error. It's equivalent to joining a Java thread.
   *
   * @throws WasmException if the thread execution fails
   * @throws InterruptedException if the wait is interrupted
   */
  void join() throws WasmException, InterruptedException;

  /**
   * Waits for this thread to complete execution with a timeout.
   *
   * <p>This method blocks until the WebAssembly thread has finished executing, encounters an error,
   * or the specified timeout elapses.
   *
   * @param timeoutMs the maximum time to wait in milliseconds
   * @return true if the thread completed, false if the timeout elapsed
   * @throws WasmException if the thread execution fails
   * @throws InterruptedException if the wait is interrupted
   */
  boolean join(long timeoutMs) throws WasmException, InterruptedException;

  /**
   * Requests termination of this WebAssembly thread.
   *
   * <p>This is a cooperative termination request. The thread may not terminate immediately and
   * should check for termination requests during its execution.
   *
   * @return a future that completes when the thread has terminated
   */
  CompletableFuture<Void> terminate();

  /**
   * Forces termination of this WebAssembly thread.
   *
   * <p>This method attempts to forcibly terminate the thread. Use with caution as it may leave
   * shared resources in an inconsistent state.
   *
   * @throws WasmException if the termination fails
   */
  void forceTerminate() throws WasmException;

  /**
   * Gets the shared memory instance accessible to this thread.
   *
   * <p>All WebAssembly threads in the same runtime share access to the same linear memory, enabling
   * communication and synchronization between threads.
   *
   * @return the shared memory instance
   */
  WasmMemory getSharedMemory();

  /**
   * Gets thread-local storage for this WebAssembly thread.
   *
   * <p>Each WebAssembly thread maintains its own thread-local storage area that is not shared with
   * other threads.
   *
   * @return the thread-local storage
   */
  WasmThreadLocalStorage getThreadLocalStorage();

  /**
   * Checks if this thread is still alive and executing.
   *
   * @return true if the thread is alive, false otherwise
   */
  boolean isAlive();

  /**
   * Checks if termination has been requested for this thread.
   *
   * @return true if termination has been requested, false otherwise
   */
  boolean isTerminationRequested();

  /**
   * Gets performance statistics for this WebAssembly thread.
   *
   * @return thread performance statistics
   */
  WasmThreadStatistics getStatistics();

  /**
   * Closes this WebAssembly thread and releases its resources.
   *
   * <p>This method ensures that the thread is properly terminated and all associated resources are
   * cleaned up. It's equivalent to calling {@link #terminate()} and waiting for completion.
   *
   * @throws WasmException if closing the thread fails
   */
  @Override
  void close() throws WasmException;
}
