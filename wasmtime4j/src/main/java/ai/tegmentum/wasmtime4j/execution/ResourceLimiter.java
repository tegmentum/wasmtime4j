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

/**
 * Resource limiter for controlling WebAssembly resource consumption.
 *
 * <p>A resource limiter allows controlling memory growth, table growth, and other resource
 * allocation to prevent runaway resource consumption by WebAssembly modules.
 *
 * <p>Implementations track resource allocation and can deny requests that would exceed configured
 * limits.
 *
 * @since 1.0.0
 */
public interface ResourceLimiter extends AutoCloseable {

  /**
   * Gets the unique limiter ID.
   *
   * @return the limiter ID
   */
  long getId();

  /**
   * Gets the configuration for this limiter.
   *
   * @return the limiter configuration
   * @throws WasmException if retrieving configuration fails
   */
  ResourceLimiterConfig getConfig() throws WasmException;

  /**
   * Checks if memory growth should be allowed.
   *
   * @param currentPages the current memory size in pages (64KB per page)
   * @param requestedPages the number of pages being requested
   * @return true if growth should be allowed, false otherwise
   * @throws WasmException if the check fails
   */
  boolean allowMemoryGrow(long currentPages, long requestedPages) throws WasmException;

  /**
   * Checks if table growth should be allowed.
   *
   * @param currentElements the current table size in elements
   * @param requestedElements the number of elements being requested
   * @return true if growth should be allowed, false otherwise
   * @throws WasmException if the check fails
   */
  boolean allowTableGrow(long currentElements, long requestedElements) throws WasmException;

  /**
   * Gets the current statistics for this limiter.
   *
   * @return the current statistics
   * @throws WasmException if retrieving statistics fails
   */
  ResourceLimiterStats getStats() throws WasmException;

  /**
   * Resets the statistics counters.
   *
   * @throws WasmException if resetting fails
   */
  void resetStats() throws WasmException;

  /**
   * Closes this limiter and releases associated resources.
   *
   * @throws WasmException if closing fails
   */
  @Override
  void close() throws WasmException;
}
