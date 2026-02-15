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

package ai.tegmentum.wasmtime4j.config;

/**
 * Dynamic, callback-based resource limiter for WebAssembly stores.
 *
 * <p>Unlike {@link StoreLimits}, which sets static limits at store creation time, {@code
 * ResourceLimiter} provides dynamic, callback-based limiting. The Wasmtime runtime calls back into
 * the limiter each time a memory or table needs to grow, allowing the embedder to make per-request
 * decisions about resource allocation.
 *
 * <p>This is useful for advanced use cases such as:
 *
 * <ul>
 *   <li>Per-request memory budgets in a web server
 *   <li>Async resource metering with external quota services
 *   <li>Adaptive limiting based on current system load
 *   <li>Detailed resource usage tracking and logging
 * </ul>
 *
 * <p>Corresponds to Wasmtime's {@code ResourceLimiter} trait.
 *
 * <h2>Example Usage:</h2>
 *
 * <pre>{@code
 * ResourceLimiter limiter = new ResourceLimiter() {
 *     private long totalAllocated = 0;
 *     private static final long MAX_BYTES = 10 * 1024 * 1024; // 10 MB
 *
 *     @Override
 *     public boolean memoryGrowing(long currentBytes, long desiredBytes, long maximumBytes) {
 *         if (desiredBytes > MAX_BYTES) {
 *             return false; // deny
 *         }
 *         totalAllocated = desiredBytes;
 *         return true; // allow
 *     }
 *
 *     @Override
 *     public boolean tableGrowing(int currentElements, int desiredElements, int maximumElements) {
 *         return desiredElements <= 10000;
 *     }
 * };
 *
 * Store store = Store.builder(engine)
 *     .withResourceLimiter(limiter)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @see StoreLimits
 */
public interface ResourceLimiter {

  /**
   * Called when a WebAssembly linear memory is about to grow.
   *
   * <p>This method is invoked by the Wasmtime runtime each time a {@code memory.grow} instruction
   * is executed or when the runtime needs to grow memory for other reasons.
   *
   * @param currentBytes the current size of the memory in bytes
   * @param desiredBytes the desired new size of the memory in bytes
   * @param maximumBytes the maximum size of the memory in bytes as declared by the WebAssembly
   *     module, or {@link Long#MAX_VALUE} if no maximum is specified
   * @return {@code true} to allow the growth, {@code false} to deny it (which will cause the
   *     {@code memory.grow} instruction to return -1)
   */
  boolean memoryGrowing(long currentBytes, long desiredBytes, long maximumBytes);

  /**
   * Called when a WebAssembly table is about to grow.
   *
   * <p>This method is invoked by the Wasmtime runtime each time a {@code table.grow} instruction is
   * executed or when the runtime needs to grow a table for other reasons.
   *
   * @param currentElements the current number of elements in the table
   * @param desiredElements the desired new number of elements
   * @param maximumElements the maximum number of elements as declared by the WebAssembly module, or
   *     {@link Integer#MAX_VALUE} if no maximum is specified
   * @return {@code true} to allow the growth, {@code false} to deny it (which will cause the
   *     {@code table.grow} instruction to return -1)
   */
  boolean tableGrowing(int currentElements, int desiredElements, int maximumElements);

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
   * <p>This is an optional notification callback. It is called after a table growth that was allowed
   * by {@link #tableGrowing} fails for some other reason.
   *
   * <p>The default implementation does nothing.
   *
   * @param error a description of why the table growth failed
   */
  default void tableGrowFailed(final String error) {
    // Default: no-op
  }
}
