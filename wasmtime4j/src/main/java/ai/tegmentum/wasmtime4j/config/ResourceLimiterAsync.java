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
package ai.tegmentum.wasmtime4j.config;

import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous resource limiter for WebAssembly stores.
 *
 * <p>This is the async counterpart to {@link ResourceLimiter}. It provides the same callback-based
 * limiting but returns {@link CompletableFuture} to allow non-blocking decision making. This is
 * useful for limiters that need to consult external services (rate limiters, quota servers, etc.)
 * before allowing resource growth.
 *
 * <p>Requires the engine to be configured with {@code asyncSupport(true)}.
 *
 * <p>When a {@code ResourceLimiterAsync} is registered with a store via {@code
 * Store.setResourceLimiterAsync()}, all memory and table growth operations will use the async path.
 *
 * <p>Corresponds to Wasmtime's {@code ResourceLimiterAsync} trait.
 *
 * <h2>Example Usage:</h2>
 *
 * <pre>{@code
 * ResourceLimiterAsync limiter = new ResourceLimiterAsync() {
 *     @Override
 *     public CompletableFuture<Boolean> memoryGrowing(
 *         long currentBytes, long desiredBytes, long maximumBytes) {
 *         // Could consult an external quota service asynchronously
 *         return CompletableFuture.completedFuture(desiredBytes <= 10 * 1024 * 1024);
 *     }
 *
 *     @Override
 *     public CompletableFuture<Boolean> tableGrowing(
 *         int currentElements, int desiredElements, int maximumElements) {
 *         return CompletableFuture.completedFuture(desiredElements <= 10000);
 *     }
 * };
 *
 * store.setResourceLimiterAsync(limiter);
 * }</pre>
 *
 * @since 1.1.0
 * @see ResourceLimiter
 */
public interface ResourceLimiterAsync {

  /**
   * Called asynchronously when a WebAssembly linear memory is about to grow.
   *
   * <p>This method is invoked by the Wasmtime runtime each time a {@code memory.grow} instruction
   * is executed or when the runtime needs to grow memory for other reasons.
   *
   * @param currentBytes the current size of the memory in bytes
   * @param desiredBytes the desired new size of the memory in bytes
   * @param maximumBytes the maximum size of the memory in bytes as declared by the WebAssembly
   *     module, or {@link Long#MAX_VALUE} if no maximum is specified
   * @return a CompletableFuture that completes with {@code true} to allow the growth, {@code false}
   *     to deny it
   */
  CompletableFuture<Boolean> memoryGrowing(long currentBytes, long desiredBytes, long maximumBytes);

  /**
   * Called asynchronously when a WebAssembly table is about to grow.
   *
   * <p>This method is invoked by the Wasmtime runtime each time a {@code table.grow} instruction is
   * executed or when the runtime needs to grow a table for other reasons.
   *
   * @param currentElements the current number of elements in the table
   * @param desiredElements the desired new number of elements
   * @param maximumElements the maximum number of elements as declared by the WebAssembly module, or
   *     {@link Integer#MAX_VALUE} if no maximum is specified
   * @return a CompletableFuture that completes with {@code true} to allow the growth, {@code false}
   *     to deny it
   */
  CompletableFuture<Boolean> tableGrowing(
      int currentElements, int desiredElements, int maximumElements);

  /**
   * Called when a memory growth operation fails.
   *
   * <p>This is an optional notification callback. It is called after a memory growth that was
   * allowed by {@link #memoryGrowing} fails for some other reason (e.g., the system is out of
   * memory).
   *
   * <p>The default implementation does nothing.
   *
   * @param error a description of why the memory growth failed
   */
  default void memoryGrowFailed(final String error) {
    // Default: no-op
  }

  /**
   * Called when a table growth operation fails.
   *
   * <p>This is an optional notification callback. It is called after a table growth that was
   * allowed by {@link #tableGrowing} fails for some other reason.
   *
   * <p>The default implementation does nothing.
   *
   * @param error a description of why the table growth failed
   */
  default void tableGrowFailed(final String error) {
    // Default: no-op
  }
}
