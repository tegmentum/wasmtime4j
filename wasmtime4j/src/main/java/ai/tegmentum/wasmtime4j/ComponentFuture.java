/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents a Component Model future value.
 *
 * <p>A ComponentFuture represents an asynchronous value that will be available at some point in the
 * future. This corresponds to the {@code future<T>} type in the WebAssembly Component Model.
 *
 * <p>Unlike Java's {@link java.util.concurrent.Future}, ComponentFuture is designed to integrate
 * with the WASI polling model, allowing components to wait on multiple futures simultaneously using
 * the poll operation.
 *
 * <p>Key characteristics of Component Model futures:
 *
 * <ul>
 *   <li>Single-shot: A future can only be resolved once
 *   <li>Pollable: Futures can be subscribed to for polling
 *   <li>Resource-backed: Futures are managed as component resources
 *   <li>Type-safe: Each future has a defined payload type
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Get a future from an async operation
 * ComponentFuture<String> future = asyncOperation();
 *
 * // Check if ready without blocking
 * if (future.isReady()) {
 *     String result = future.get().orElseThrow();
 * }
 *
 * // Or subscribe and poll
 * WasiPollable pollable = future.subscribe();
 * pollable.block();
 * String result = future.get().orElseThrow();
 *
 * // Or convert to CompletableFuture for Java async patterns
 * CompletableFuture<String> javaFuture = future.toCompletableFuture();
 * javaFuture.thenAccept(result -> System.out.println(result));
 * }</pre>
 *
 * @param <T> the type of the future's payload
 * @since 1.0.0
 */
public interface ComponentFuture<T> extends AutoCloseable {

  /**
   * Gets the WIT type of this future's payload.
   *
   * @return the payload type
   */
  WitType getPayloadType();

  /**
   * Checks if the future has completed and a value is ready.
   *
   * <p>This is a non-blocking check that returns immediately. If this returns true, {@link #get()}
   * will return a present value.
   *
   * @return true if the future has completed, false otherwise
   * @throws WasmException if the readiness check fails
   * @throws IllegalStateException if the future has been closed
   */
  boolean isReady() throws WasmException;

  /**
   * Attempts to get the future's value without blocking.
   *
   * <p>If the future has completed, returns the value wrapped in an Optional. If the future has not
   * completed yet, returns empty. This operation consumes the future - subsequent calls will return
   * empty.
   *
   * @return the value if ready, or empty if not yet completed
   * @throws WasmException if retrieving the value fails
   * @throws IllegalStateException if the future has been closed
   */
  Optional<T> get() throws WasmException;

  /**
   * Gets the future's value, blocking up to the specified timeout.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout
   * @return the value if completed within timeout, or empty if timed out
   * @throws WasmException if retrieving the value fails
   * @throws IllegalStateException if the future has been closed
   */
  Optional<T> get(long timeout, TimeUnit unit) throws WasmException;

  /**
   * Gets the future's value, blocking until available.
   *
   * <p>This method blocks indefinitely until the future completes. For bounded waiting, use {@link
   * #get(long, TimeUnit)} or the polling approach with {@link #subscribe()}.
   *
   * @return the value
   * @throws WasmException if retrieving the value fails
   * @throws IllegalStateException if the future has been closed
   */
  T getBlocking() throws WasmException;

  /**
   * Creates a pollable handle for this future.
   *
   * <p>The returned pollable can be used with WASI poll operations to wait for the future to
   * complete alongside other pollable resources (streams, timers, etc.).
   *
   * @return a pollable that becomes ready when this future completes
   * @throws WasmException if subscription fails
   * @throws IllegalStateException if the future has been closed
   */
  WasiPollable subscribe() throws WasmException;

  /**
   * Checks if the future has been completed (either successfully or with an error).
   *
   * @return true if the future is completed
   */
  boolean isDone();

  /**
   * Checks if the future completed exceptionally.
   *
   * @return true if the future completed with an error
   */
  boolean isCompletedExceptionally();

  /**
   * Gets the exception if the future completed exceptionally.
   *
   * @return the exception, or empty if not completed exceptionally
   */
  Optional<Throwable> getException();

  /**
   * Converts this ComponentFuture to a Java CompletableFuture.
   *
   * <p>This allows integration with Java async programming patterns. The returned future will
   * complete when this ComponentFuture completes.
   *
   * <p>Note: The returned CompletableFuture runs on a background thread that polls this
   * ComponentFuture. Resource cleanup is handled automatically.
   *
   * @return a CompletableFuture that completes with the same result
   */
  CompletableFuture<T> toCompletableFuture();

  /**
   * Closes the future and releases associated resources.
   *
   * <p>After closing, no further operations can be performed on this future. If the future has not
   * completed, closing it will cancel the pending operation.
   */
  @Override
  void close();

  /** The state of a ComponentFuture. */
  enum State {
    /** The future is pending and has not completed. */
    PENDING,

    /** The future has completed successfully with a value. */
    COMPLETED,

    /** The future has completed exceptionally. */
    FAILED,

    /** The future has been cancelled. */
    CANCELLED,

    /** The future has been closed. */
    CLOSED
  }

  /**
   * Gets the current state of this future.
   *
   * @return the future state
   */
  State getState();
}
