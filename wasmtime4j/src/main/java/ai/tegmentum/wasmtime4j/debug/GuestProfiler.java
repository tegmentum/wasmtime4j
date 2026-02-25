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
import ai.tegmentum.wasmtime4j.func.CallHook;
import java.io.Closeable;
import java.time.Duration;

/**
 * Sampling-based guest profiler for WebAssembly execution.
 *
 * <p>Wraps Wasmtime's {@code GuestProfiler} to collect stack samples during WebAssembly execution
 * and produce profiles in the Firefox Processed Profile Format (JSON). The resulting profile can be
 * loaded at <a href="https://profiler.firefox.com/">profiler.firefox.com</a>.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * try (GuestProfiler profiler = engine.createGuestProfiler("my-module",
 *         Duration.ofMillis(1), Map.of("main", module))) {
 *     // ... execute wasm functions, calling profiler.sample() periodically ...
 *     profiler.sample(store, Duration.ZERO);
 *     byte[] profileJson = profiler.finish();
 *     // Write profileJson to a file and load in profiler.firefox.com
 * }
 * }</pre>
 *
 * <p>The profiler should be used in conjunction with epoch-based interrupts. Register an epoch
 * deadline callback on the store that calls {@link #sample(Store, Duration)} at each interrupt,
 * then call {@link #finish()} when profiling is complete.
 *
 * <p>After {@link #finish()} is called, the profiler is consumed and further calls to {@link
 * #sample(Store, Duration)} or {@link #callHook(Store, CallHook)} will throw.
 *
 * @since 1.0.0
 */
public interface GuestProfiler extends Closeable {

  /**
   * Collects a stack sample from the current execution state.
   *
   * <p>Should be called periodically during execution, typically from an epoch deadline callback.
   * The {@code delta} parameter indicates the CPU time elapsed since the previous sample; use
   * {@link Duration#ZERO} if unknown.
   *
   * @param store the store to sample from
   * @param delta CPU time since previous sample
   * @throws WasmException if sampling fails or the profiler has been finished
   * @throws IllegalStateException if the profiler has already been finished
   */
  void sample(Store store, Duration delta) throws WasmException;

  /**
   * Records a call hook transition marker in the profile.
   *
   * <p>This is optional and adds host/wasm transition markers to the profile for more detailed
   * analysis. Typically called from a {@link ai.tegmentum.wasmtime4j.func.CallHookHandler}
   * registered on the store.
   *
   * @param store the store context
   * @param hook the call hook transition type
   * @throws WasmException if recording fails or the profiler has been finished
   * @throws IllegalStateException if the profiler has already been finished
   */
  void callHook(Store store, CallHook hook) throws WasmException;

  /**
   * Finishes profiling and returns the profile data as JSON bytes.
   *
   * <p>The returned bytes are in the Firefox Processed Profile Format, viewable at <a
   * href="https://profiler.firefox.com/">profiler.firefox.com</a>.
   *
   * <p>This method consumes the profiler. After calling this, {@link #sample(Store, Duration)} and
   * {@link #callHook(Store, CallHook)} will throw.
   *
   * @return the profile data as JSON bytes
   * @throws WasmException if finishing fails
   * @throws IllegalStateException if the profiler has already been finished
   */
  byte[] finish() throws WasmException;

  /**
   * Returns whether this profiler is still active (not yet finished or closed).
   *
   * @return true if the profiler can still collect samples
   */
  boolean isActive();

  /**
   * Closes the profiler, releasing native resources.
   *
   * <p>If {@link #finish()} has not been called, the profiler is silently discarded.
   */
  @Override
  void close();
}
