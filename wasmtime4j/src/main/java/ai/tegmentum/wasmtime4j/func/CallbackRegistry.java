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
package ai.tegmentum.wasmtime4j.func;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.concurrent.CompletableFuture;

/**
 * Registry for managing callback functions and asynchronous operations.
 *
 * <p>The callback registry provides a centralized mechanism for managing function references,
 * callback invocations, and asynchronous operations between Java and WebAssembly. It ensures thread
 * safety, proper resource cleanup, and efficient callback dispatching.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe callback registration and invocation
 *   <li>Asynchronous callback support with CompletableFuture
 *   <li>Automatic resource cleanup and lifecycle management
 *   <li>Performance monitoring and metrics collection
 *   <li>Error handling and recovery mechanisms
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * CallbackRegistry registry = store.getCallbackRegistry();
 *
 * // Register a synchronous callback
 * CallbackHandle handle = registry.registerCallback("my_callback",
 *     (params) -> {
 *         // Process callback
 *         return new WasmValue[] { WasmValue.i32(42) };
 *     }, functionType);
 *
 * // Register an asynchronous callback
 * AsyncCallbackHandle asyncHandle = registry.registerAsyncCallback("async_callback",
 *     (params) -> CompletableFuture.supplyAsync(() -> {
 *         // Async processing
 *         return new WasmValue[] { WasmValue.i32(100) };
 *     }), functionType);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface CallbackRegistry {

  /**
   * Registers a synchronous callback function.
   *
   * @param name the callback name for identification
   * @param callback the callback implementation
   * @param functionType the function type signature
   * @return a handle for managing the callback
   * @throws WasmException if registration fails
   */
  CallbackHandle registerCallback(
      final String name, final HostFunction callback, final FunctionType functionType)
      throws WasmException;

  /**
   * Registers an asynchronous callback function.
   *
   * @param name the callback name for identification
   * @param callback the asynchronous callback implementation
   * @param functionType the function type signature
   * @return a handle for managing the async callback
   * @throws WasmException if registration fails
   */
  AsyncCallbackHandle registerAsyncCallback(
      final String name, final AsyncHostFunction callback, final FunctionType functionType)
      throws WasmException;

  /**
   * Creates a function reference from a callback handle.
   *
   * @param handle the callback handle
   * @return a function reference for the callback
   * @throws WasmException if function reference creation fails
   */
  FunctionReference createFunctionReference(final CallbackHandle handle) throws WasmException;

  /**
   * Unregisters a callback and releases its resources.
   *
   * @param handle the callback handle to unregister
   * @throws WasmException if unregistration fails
   */
  void unregisterCallback(final CallbackHandle handle) throws WasmException;

  /**
   * Invokes a callback by its handle with the given parameters.
   *
   * @param handle the callback handle
   * @param params the parameters to pass to the callback
   * @return the results from the callback
   * @throws WasmException if callback invocation fails
   */
  WasmValue[] invokeCallback(final CallbackHandle handle, final WasmValue... params)
      throws WasmException;

  /**
   * Invokes an asynchronous callback and returns a CompletableFuture.
   *
   * @param handle the async callback handle
   * @param params the parameters to pass to the callback
   * @return a CompletableFuture containing the callback results
   * @throws WasmException if callback invocation fails
   */
  CompletableFuture<WasmValue[]> invokeAsyncCallback(
      final AsyncCallbackHandle handle, final WasmValue... params) throws WasmException;

  /**
   * Gets callback performance metrics.
   *
   * @return performance metrics for all registered callbacks
   */
  CallbackMetrics getMetrics();

  /**
   * Gets the number of currently registered callbacks.
   *
   * @return the callback count
   */
  int getCallbackCount();

  /**
   * Checks if a callback is registered with the given name.
   *
   * @param name the callback name
   * @return true if a callback with that name is registered
   */
  boolean hasCallback(final String name);

  /**
   * Closes the registry and releases all resources.
   *
   * <p>This method should be called when the registry is no longer needed. After calling this
   * method, the registry cannot be used for new operations.
   *
   * @throws WasmException if cleanup fails
   */
  void close() throws WasmException;

  /** Handle for managing synchronous callbacks. */
  interface CallbackHandle {
    /**
     * Gets the callback ID.
     *
     * @return the unique callback identifier
     */
    long getId();

    /**
     * Gets the callback name.
     *
     * @return the callback name
     */
    String getName();

    /**
     * Gets the function type.
     *
     * @return the function type signature
     */
    FunctionType getFunctionType();

    /**
     * Checks if the callback is still valid.
     *
     * @return true if the callback is valid
     */
    boolean isValid();
  }

  /** Handle for managing asynchronous callbacks. */
  interface AsyncCallbackHandle extends CallbackHandle {
    /**
     * Gets the execution timeout for async operations.
     *
     * @return the timeout in milliseconds
     */
    long getTimeoutMillis();

    /**
     * Sets the execution timeout for async operations.
     *
     * @param timeoutMillis the timeout in milliseconds
     */
    void setTimeoutMillis(final long timeoutMillis);
  }

  /** Interface for asynchronous host functions. */
  @FunctionalInterface
  interface AsyncHostFunction {
    /**
     * Executes the asynchronous host function.
     *
     * @param params the parameters passed from WebAssembly
     * @return a CompletableFuture containing the results
     */
    CompletableFuture<WasmValue[]> executeAsync(final WasmValue[] params);
  }

  /** Performance metrics for callback operations. */
  interface CallbackMetrics {
    /**
     * Gets the total number of callback invocations.
     *
     * @return the invocation count
     */
    long getTotalInvocations();

    /**
     * Gets the average callback execution time in nanoseconds.
     *
     * @return the average execution time
     */
    double getAverageExecutionTimeNanos();

    /**
     * Gets the total callback execution time in nanoseconds.
     *
     * @return the total execution time
     */
    long getTotalExecutionTimeNanos();

    /**
     * Gets the number of failed callback invocations.
     *
     * @return the failure count
     */
    long getFailureCount();

    /**
     * Gets the number of timed-out async callbacks.
     *
     * @return the timeout count
     */
    long getTimeoutCount();
  }
}
