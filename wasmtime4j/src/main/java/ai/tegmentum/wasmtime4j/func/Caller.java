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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.ModuleExport;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;

/**
 * Provides access to the calling WebAssembly instance context within host functions.
 *
 * <p>The Caller interface allows host functions to access exports from the calling WebAssembly
 * instance, including memory, tables, globals, and functions. This enables host functions to
 * interact with the WebAssembly module's state and resources.
 *
 * <p>Caller instances are passed to host functions that are defined with caller context support and
 * provide safe access to the execution environment.
 *
 * @param <T> the type of user data associated with the store
 * @since 1.0.0
 */
public interface Caller<T> {

  /**
   * Gets the user data associated with the store.
   *
   * <p>This is the same data that was provided when creating the store and can be used to maintain
   * state across host function calls.
   *
   * @return the store's user data
   * @since 1.0.0
   */
  T data();

  /**
   * Gets an exported item by name from the calling instance.
   *
   * <p>This method provides access to any export (function, memory, table, or global) that the
   * calling WebAssembly instance has made available.
   *
   * @param name the name of the export to retrieve
   * @return the export if it exists, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<Extern> getExport(String name);

  /**
   * Gets an exported item using a pre-resolved {@link ModuleExport} handle for O(1) lookup.
   *
   * <p>This method provides fast export access by using a cached index handle obtained from {@link
   * Module#getModuleExport(String)}.
   *
   * @param moduleExport the pre-resolved export handle
   * @return the export if it exists, empty otherwise
   * @throws IllegalArgumentException if moduleExport is null
   * @since 1.1.0
   */
  Optional<Extern> getExport(ModuleExport moduleExport);

  /**
   * Gets an exported function by name from the calling instance.
   *
   * @param name the name of the function export
   * @return the function if it exists and is a function, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<WasmFunction> getFunction(String name);

  /**
   * Gets an exported memory by name from the calling instance.
   *
   * @param name the name of the memory export
   * @return the memory if it exists and is a memory, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<WasmMemory> getMemory(String name);

  /**
   * Gets the default memory export from the calling instance.
   *
   * <p>This is a convenience method that looks for a memory export named "memory", which is the
   * default export name for WebAssembly memory.
   *
   * @return the default memory if it exists, empty otherwise
   * @since 1.0.0
   */
  default Optional<WasmMemory> getMemory() {
    return getMemory("memory");
  }

  /**
   * Gets an exported table by name from the calling instance.
   *
   * @param name the name of the table export
   * @return the table if it exists and is a table, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<WasmTable> getTable(String name);

  /**
   * Gets an exported global by name from the calling instance.
   *
   * @param name the name of the global export
   * @return the global if it exists and is a global, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<WasmGlobal> getGlobal(String name);

  /**
   * Checks if the calling instance has an export with the given name.
   *
   * @param name the name to check for
   * @return true if an export with that name exists
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  boolean hasExport(String name);

  /**
   * Gets the fuel remaining in the caller if fuel metering is enabled.
   *
   * <p>This method returns the amount of fuel remaining for the current execution. When fuel is
   * exhausted, the WebAssembly execution will be interrupted.
   *
   * @return the fuel remaining, or empty if fuel metering is not enabled
   * @since 1.0.0
   */
  Optional<Long> fuelRemaining();

  /**
   * Adds fuel to the caller's fuel tank.
   *
   * <p>This allows extending the execution time during host function calls. The added fuel becomes
   * immediately available for continued execution.
   *
   * @param fuel the amount of fuel to add
   * @throws WasmException if fuel metering is not enabled or if adding fuel fails
   * @throws IllegalArgumentException if fuel is negative
   * @since 1.0.0
   */
  void addFuel(long fuel) throws WasmException;

  /**
   * Sets the fuel level to a specific value.
   *
   * <p>This replaces the current fuel amount rather than adding to it. The fuel value becomes
   * immediately effective for continued execution.
   *
   * @param fuel the fuel level to set
   * @throws WasmException if fuel metering is not enabled or if setting fuel fails
   * @throws IllegalArgumentException if fuel is negative
   * @since 1.0.0
   */
  void setFuel(long fuel) throws WasmException;

  /**
   * Gets the engine associated with the caller's store.
   *
   * <p>This provides access to engine configuration during host function execution, which can be
   * useful for checking enabled features or accessing shared engine state.
   *
   * @return the Engine associated with this caller
   * @since 1.0.0
   */
  Engine engine();

  /**
   * Triggers garbage collection from within a host function.
   *
   * <p>This is useful for managing memory during long-running host operations that may have
   * accumulated many unreferenced GC objects. Unlike the synchronous GC call, this method is
   * designed for use within host function contexts.
   *
   * <p><b>Note:</b> GC support must be enabled in the engine configuration for this method to have
   * any effect.
   *
   * @throws WasmException if the GC operation fails
   * @since 1.0.0
   */
  void gc() throws WasmException;

  /**
   * Performs garbage collection asynchronously.
   *
   * <p>This is the async variant of {@link #gc()} that cooperatively yields during collection.
   * Requires the store to have async support enabled.
   *
   * @return a future that completes when GC is finished
   * @since 1.1.0
   */
  default java.util.concurrent.CompletableFuture<Void> gcAsync() {
    return java.util.concurrent.CompletableFuture.runAsync(
        () -> {
          try {
            gc();
          } catch (final ai.tegmentum.wasmtime4j.exception.WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  /**
   * Configures the fuel-based async yield interval for this caller's store.
   *
   * <p>When both fuel consumption and async support are enabled, this controls how frequently the
   * WebAssembly execution yields back to the async executor. A value of 0 disables automatic
   * yielding.
   *
   * @param interval the fuel interval between yields, or 0 to disable
   * @throws WasmException if the configuration fails
   * @throws IllegalArgumentException if interval is negative
   * @since 1.0.0
   */
  void setFuelAsyncYieldInterval(long interval) throws WasmException;

  /**
   * Gets the debug exit frames from the caller's execution context.
   *
   * <p>This provides a snapshot of the WebAssembly call stack, including function indices, program
   * counters, local variable counts, and operand stack depths. Requires the engine to be configured
   * with {@code guestDebug(true)}.
   *
   * @return a list of frame handles, innermost first
   * @throws UnsupportedOperationException if guest debugging is not enabled
   * @throws WasmException if frame retrieval fails
   * @since 1.1.0
   */
  default java.util.List<ai.tegmentum.wasmtime4j.debug.FrameHandle> debugExitFrames()
      throws WasmException {
    throw new UnsupportedOperationException(
        "debugExitFrames requires guest debugging to be enabled via Config.guestDebug(true)");
  }
}
