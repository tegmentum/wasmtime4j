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

package ai.tegmentum.wasmtime4j.execution;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous resource limiter for controlling WebAssembly resource consumption.
 *
 * <p>This interface extends the resource limiting concept to support asynchronous operations. It is
 * particularly useful when resource allocation decisions need to consult external systems or
 * perform I/O operations.
 *
 * <p>Unlike {@link ResourceLimiter} which blocks until a decision is made, ResourceLimiterAsync
 * returns futures that can be composed with other async operations.
 *
 * <p>Example implementation:
 *
 * <pre>{@code
 * public class AsyncQuotaLimiter implements ResourceLimiterAsync {
 *     private final QuotaService quotaService;
 *
 *     public CompletableFuture<Boolean> allowMemoryGrowAsync(long current, long requested) {
 *         return quotaService.checkMemoryQuotaAsync(current + requested * 65536)
 *             .thenApply(QuotaResult::isAllowed);
 *     }
 *
 *     // ... other methods
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ResourceLimiterAsync extends AutoCloseable {

  /**
   * Gets the unique limiter ID.
   *
   * @return the limiter ID
   */
  long getId();

  /**
   * Gets the configuration for this limiter.
   *
   * @return a future completing with the limiter configuration
   */
  CompletableFuture<ResourceLimiterConfig> getConfigAsync();

  /**
   * Asynchronously checks if memory growth should be allowed.
   *
   * <p>This method is called when WebAssembly code attempts to grow a memory. The implementation
   * can perform async operations (e.g., check quotas, consult external services) before deciding.
   *
   * @param currentPages the current memory size in pages (64KB per page)
   * @param requestedPages the number of pages being requested to add
   * @return a future completing with true if growth should be allowed, false otherwise
   */
  CompletableFuture<Boolean> allowMemoryGrowAsync(long currentPages, long requestedPages);

  /**
   * Asynchronously checks if table growth should be allowed.
   *
   * <p>This method is called when WebAssembly code attempts to grow a table. The implementation can
   * perform async operations before deciding.
   *
   * @param currentElements the current table size in elements
   * @param requestedElements the number of elements being requested to add
   * @return a future completing with true if growth should be allowed, false otherwise
   */
  CompletableFuture<Boolean> allowTableGrowAsync(long currentElements, long requestedElements);

  /**
   * Asynchronously gets the current statistics for this limiter.
   *
   * @return a future completing with the current statistics
   */
  CompletableFuture<ResourceLimiterStats> getStatsAsync();

  /**
   * Asynchronously resets the statistics counters.
   *
   * @return a future completing when the reset is done
   */
  CompletableFuture<Void> resetStatsAsync();

  /**
   * Synchronous wrapper that checks if memory growth should be allowed.
   *
   * <p>This is a convenience method that blocks on the async result. Prefer using
   * {@link #allowMemoryGrowAsync} for non-blocking code.
   *
   * @param currentPages the current memory size in pages
   * @param requestedPages the number of pages being requested
   * @return true if growth should be allowed
   * @throws WasmException if the check fails
   */
  default boolean allowMemoryGrow(long currentPages, long requestedPages) throws WasmException {
    try {
      return allowMemoryGrowAsync(currentPages, requestedPages).join();
    } catch (Exception e) {
      throw new WasmException("Memory grow check failed: " + e.getMessage(), e);
    }
  }

  /**
   * Synchronous wrapper that checks if table growth should be allowed.
   *
   * @param currentElements the current table size in elements
   * @param requestedElements the number of elements being requested
   * @return true if growth should be allowed
   * @throws WasmException if the check fails
   */
  default boolean allowTableGrow(long currentElements, long requestedElements) throws WasmException {
    try {
      return allowTableGrowAsync(currentElements, requestedElements).join();
    } catch (Exception e) {
      throw new WasmException("Table grow check failed: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a ResourceLimiterAsync from a synchronous ResourceLimiter.
   *
   * <p>The returned async limiter wraps the synchronous calls in CompletableFuture.supplyAsync.
   *
   * @param syncLimiter the synchronous limiter to wrap
   * @return an async wrapper around the synchronous limiter
   */
  static ResourceLimiterAsync fromSync(final ResourceLimiter syncLimiter) {
    return new SyncToAsyncLimiterAdapter(syncLimiter);
  }

  /**
   * Closes this limiter and releases associated resources.
   *
   * @throws WasmException if closing fails
   */
  @Override
  void close() throws WasmException;
}
