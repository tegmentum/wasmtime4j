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

package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Interface for profiling WebAssembly guest execution.
 *
 * <p>GuestProfiler provides instrumentation and profiling capabilities for WebAssembly execution.
 * It can collect various metrics including:
 *
 * <ul>
 *   <li>Function call counts and timing
 *   <li>Memory allocation patterns
 *   <li>Instruction counts
 *   <li>Stack depth statistics
 * </ul>
 *
 * <p>Profiling data can be exported in various formats for analysis with external tools.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (GuestProfiler profiler = GuestProfiler.create(store)) {
 *     profiler.start();
 *
 *     // Execute WebAssembly code...
 *     instance.call("main");
 *
 *     profiler.stop();
 *
 *     // Export profile data
 *     profiler.exportTo(Path.of("profile.json"), ProfileFormat.JSON);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface GuestProfiler extends AutoCloseable {

  /**
   * Creates a new guest profiler for the given store.
   *
   * @param store the store to profile
   * @return a new GuestProfiler
   * @throws WasmException if profiler creation fails
   */
  static GuestProfiler create(final Store store) throws WasmException {
    return new DefaultGuestProfiler(store);
  }

  /**
   * Creates a new guest profiler with specific configuration.
   *
   * @param store the store to profile
   * @param config the profiler configuration
   * @return a new GuestProfiler
   * @throws WasmException if profiler creation fails
   */
  static GuestProfiler create(final Store store, final ProfilerConfig config) throws WasmException {
    return new DefaultGuestProfiler(store, config);
  }

  /**
   * Starts profiling.
   *
   * <p>Profiling data is collected from this point until {@link #stop()} is called.
   *
   * @throws WasmException if profiling cannot be started
   * @throws IllegalStateException if already profiling
   */
  void start() throws WasmException;

  /**
   * Stops profiling.
   *
   * @throws WasmException if profiling cannot be stopped
   * @throws IllegalStateException if not currently profiling
   */
  void stop() throws WasmException;

  /**
   * Checks if profiling is currently active.
   *
   * @return true if profiling is active
   */
  boolean isProfiling();

  /**
   * Gets the collected profile data.
   *
   * @return the profile data
   * @throws IllegalStateException if profiling has not been stopped
   */
  ProfileData getProfileData();

  /**
   * Exports profile data to a file.
   *
   * @param path the output file path
   * @param format the output format
   * @throws WasmException if export fails
   */
  void exportTo(Path path, ProfileFormat format) throws WasmException;

  /**
   * Exports profile data to an output stream.
   *
   * @param outputStream the output stream
   * @param format the output format
   * @throws WasmException if export fails
   */
  void exportTo(OutputStream outputStream, ProfileFormat format) throws WasmException;

  /**
   * Resets the profiler, clearing all collected data.
   */
  void reset();

  /**
   * Closes this profiler and releases associated resources.
   *
   * @throws WasmException if closing fails
   */
  @Override
  void close() throws WasmException;

  /**
   * Profile output formats.
   */
  enum ProfileFormat {
    /** JSON format for general consumption. */
    JSON,
    /** Flamegraph-compatible format. */
    FLAMEGRAPH,
    /** Chrome DevTools trace format. */
    CHROME_TRACE,
    /** pprof format for Go tooling. */
    PPROF
  }
}
