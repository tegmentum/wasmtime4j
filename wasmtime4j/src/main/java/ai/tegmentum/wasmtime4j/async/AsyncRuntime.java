/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.util.function.Consumer;

/**
 * Interface for managing asynchronous WebAssembly operations.
 *
 * <p>AsyncRuntime provides the infrastructure for executing WebAssembly functions asynchronously,
 * including timeout handling, cancellation support, and progress callbacks.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (AsyncRuntime runtime = AsyncRuntimeFactory.create()) {
 *     // Execute a function asynchronously
 *     AsyncOperation operation = runtime.executeAsync(
 *         instance,
 *         "process_data",
 *         new Object[] { data },
 *         Duration.ofSeconds(30)
 *     );
 *
 *     // Wait for completion
 *     Object result = operation.await();
 *
 *     // Or get the CompletableFuture
 *     CompletableFuture<Object> future = operation.toCompletableFuture();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface AsyncRuntime extends Closeable {

  /**
   * Status of an async operation.
   *
   * @since 1.0.0
   */
  enum OperationStatus {
    /** Operation is pending execution. */
    PENDING,
    /** Operation is currently running. */
    RUNNING,
    /** Operation completed successfully. */
    COMPLETED,
    /** Operation failed with error. */
    FAILED,
    /** Operation was cancelled. */
    CANCELLED,
    /** Operation timed out. */
    TIMED_OUT
  }

  /**
   * Initializes the async runtime.
   *
   * <p>This method must be called before any async operations. It is safe to call multiple times.
   *
   * @throws WasmException if initialization fails
   */
  void initialize() throws WasmException;

  /**
   * Checks if the runtime is initialized and ready for use.
   *
   * @return true if initialized, false otherwise
   */
  boolean isInitialized();

  /**
   * Gets runtime information.
   *
   * @return runtime information string
   */
  String getRuntimeInfo();

  /**
   * Executes a WebAssembly function asynchronously.
   *
   * @param instancePtr native pointer to the instance
   * @param functionName name of the function to call
   * @param arguments function arguments
   * @param timeoutMs timeout in milliseconds
   * @param callback completion callback
   * @return operation ID for tracking
   * @throws WasmException if the operation cannot be started
   */
  long executeAsync(
      long instancePtr,
      String functionName,
      Object[] arguments,
      long timeoutMs,
      Consumer<AsyncResult> callback)
      throws WasmException;

  /**
   * Compiles a WebAssembly module asynchronously.
   *
   * @param wasmBytes module bytecode
   * @param timeoutMs timeout in milliseconds
   * @param progressCallback progress callback (may be null)
   * @param completionCallback completion callback
   * @return operation ID for tracking
   * @throws WasmException if compilation cannot be started
   */
  long compileAsync(
      byte[] wasmBytes,
      long timeoutMs,
      Consumer<Integer> progressCallback,
      Consumer<AsyncResult> completionCallback)
      throws WasmException;

  /**
   * Cancels an async operation.
   *
   * @param operationId the operation to cancel
   * @return true if cancelled successfully, false if already completed or not found
   */
  boolean cancelOperation(long operationId);

  /**
   * Gets the status of an async operation.
   *
   * @param operationId the operation to check
   * @return current status
   */
  OperationStatus getOperationStatus(long operationId);

  /**
   * Waits for an async operation to complete.
   *
   * @param operationId the operation to wait for
   * @param timeoutMs maximum time to wait in milliseconds
   * @return the operation status after waiting
   * @throws WasmException if waiting fails
   */
  OperationStatus waitForOperation(long operationId, long timeoutMs) throws WasmException;

  /**
   * Gets the number of currently active async operations.
   *
   * @return count of active operations
   */
  int getActiveOperationCount();

  /**
   * Shuts down the async runtime gracefully.
   *
   * <p>After shutdown, no new async operations can be started. In-progress operations will be
   * cancelled.
   */
  void shutdown();

  /**
   * Closes the async runtime and releases resources.
   *
   * <p>This is equivalent to calling shutdown().
   */
  @Override
  void close();

  /**
   * Result of an async operation.
   *
   * @since 1.0.0
   */
  final class AsyncResult {
    private final OperationStatus status;
    private final int statusCode;
    private final String message;
    private final Object result;

    /**
     * Creates a new AsyncResult.
     *
     * @param status the operation status
     * @param statusCode the status code
     * @param message the message
     * @param result the result value (may be null)
     */
    public AsyncResult(
        final OperationStatus status,
        final int statusCode,
        final String message,
        final Object result) {
      this.status = status;
      this.statusCode = statusCode;
      this.message = message;
      this.result = result;
    }

    /**
     * Gets the operation status.
     *
     * @return the status
     */
    public OperationStatus getStatus() {
      return status;
    }

    /**
     * Gets the status code.
     *
     * @return the status code
     */
    public int getStatusCode() {
      return statusCode;
    }

    /**
     * Gets the message.
     *
     * @return the message
     */
    public String getMessage() {
      return message;
    }

    /**
     * Gets the result value.
     *
     * @return the result, or null if none
     */
    public Object getResult() {
      return result;
    }

    /**
     * Checks if the operation was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
      return status == OperationStatus.COMPLETED && statusCode == 0;
    }

    @Override
    public String toString() {
      return "AsyncResult{status="
          + status
          + ", statusCode="
          + statusCode
          + ", message='"
          + message
          + "'}";
    }
  }
}
