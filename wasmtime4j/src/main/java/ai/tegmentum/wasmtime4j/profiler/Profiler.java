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

package ai.tegmentum.wasmtime4j.profiler;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;

/**
 * Performance profiler for WebAssembly execution.
 *
 * <p>This interface provides methods for profiling WebAssembly function execution, tracking memory
 * usage, and collecting compilation statistics.
 *
 * @since 1.0.0
 */
public interface Profiler extends AutoCloseable {

  /**
   * Starts performance profiling.
   *
   * @throws WasmException if profiling cannot be started
   */
  void startProfiling() throws WasmException;

  /**
   * Stops performance profiling.
   *
   * @throws WasmException if profiling cannot be stopped
   */
  void stopProfiling() throws WasmException;

  /**
   * Checks if profiling is currently active.
   *
   * @return true if profiling is active
   */
  boolean isProfiling();

  /**
   * Records a function execution for profiling.
   *
   * @param functionName the name of the function
   * @param executionTime the execution duration
   * @param memoryDelta the change in memory usage (positive for allocations, negative for
   *     deallocations)
   * @throws WasmException if recording fails
   */
  void recordFunctionExecution(String functionName, Duration executionTime, long memoryDelta)
      throws WasmException;

  /**
   * Records a module compilation event.
   *
   * @param compilationTime the compilation duration
   * @param bytecodeSize the size of the compiled bytecode
   * @param cached whether the compilation result was cached
   * @param optimized whether the module was optimized
   * @throws WasmException if recording fails
   */
  void recordCompilation(
      Duration compilationTime, long bytecodeSize, boolean cached, boolean optimized)
      throws WasmException;

  /**
   * Gets the total number of modules compiled.
   *
   * @return the number of compiled modules
   */
  long getModulesCompiled();

  /**
   * Gets the total compilation time.
   *
   * @return the total compilation time
   */
  Duration getTotalCompilationTime();

  /**
   * Gets the average compilation time per module.
   *
   * @return the average compilation time
   */
  Duration getAverageCompilationTime();

  /**
   * Gets the total bytes of bytecode compiled.
   *
   * @return the total bytes compiled
   */
  long getBytesCompiled();

  /**
   * Gets the number of compilation cache hits.
   *
   * @return the cache hit count
   */
  long getCacheHits();

  /**
   * Gets the number of compilation cache misses.
   *
   * @return the cache miss count
   */
  long getCacheMisses();

  /**
   * Gets the number of optimized modules.
   *
   * @return the optimized module count
   */
  long getOptimizedModules();

  /**
   * Gets the current memory usage in bytes.
   *
   * @return the current memory usage
   */
  long getCurrentMemoryBytes();

  /**
   * Gets the peak memory usage in bytes.
   *
   * @return the peak memory usage
   */
  long getPeakMemoryBytes();

  /**
   * Gets the profiler uptime.
   *
   * @return the uptime duration
   */
  Duration getUptime();

  /**
   * Gets the function calls per second rate.
   *
   * @return the function calls per second
   */
  double getFunctionCallsPerSecond();

  /**
   * Gets the total number of function calls.
   *
   * @return the total function call count
   */
  long getTotalFunctionCalls();

  /**
   * Gets the total execution time across all profiled functions.
   *
   * @return the total execution time
   */
  Duration getTotalExecutionTime();

  /**
   * Resets all profiling statistics.
   *
   * @throws WasmException if reset fails
   */
  void reset() throws WasmException;

  /**
   * Closes this profiler and releases any resources.
   *
   * @throws WasmException if closing fails
   */
  @Override
  void close() throws WasmException;
}
