package ai.tegmentum.wasmtime4j.func;

import ai.tegmentum.wasmtime4j.Table;

import ai.tegmentum.wasmtime4j.ModuleExport;

import ai.tegmentum.wasmtime4j.Memory;

import ai.tegmentum.wasmtime4j.Global;

import ai.tegmentum.wasmtime4j.Export;

import ai.tegmentum.wasmtime4j.exception.WasmException;

import ai.tegmentum.wasmtime4j.Engine;

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
  Optional<Export> getExport(String name);

  /**
   * Gets an exported function by name from the calling instance.
   *
   * @param name the name of the function export
   * @return the function if it exists and is a function, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<Function<T>> getFunction(String name);

  /**
   * Gets an exported memory by name from the calling instance.
   *
   * @param name the name of the memory export
   * @return the memory if it exists and is a memory, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<Memory> getMemory(String name);

  /**
   * Gets the default memory export from the calling instance.
   *
   * <p>This is a convenience method that looks for a memory export named "memory", which is the
   * default export name for WebAssembly memory.
   *
   * @return the default memory if it exists, empty otherwise
   * @since 1.0.0
   */
  default Optional<Memory> getMemory() {
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
  Optional<Table> getTable(String name);

  /**
   * Gets an exported global by name from the calling instance.
   *
   * @param name the name of the global export
   * @return the global if it exists and is a global, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<Global> getGlobal(String name);

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
   * Gets the current fuel consumption if fuel metering is enabled.
   *
   * <p>Fuel metering allows limiting the execution time of WebAssembly code. This method returns
   * the amount of fuel consumed so far in the current call.
   *
   * @return the fuel consumed, or empty if fuel metering is not enabled
   * @since 1.0.0
   */
  Optional<Long> fuelConsumed();

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
   * Checks if an epoch deadline has been set for the current execution.
   *
   * <p>Epoch-based interruption provides another mechanism for limiting execution time and ensuring
   * responsiveness.
   *
   * @return true if an epoch deadline is active
   * @since 1.0.0
   */
  boolean hasEpochDeadline();

  /**
   * Gets the current epoch deadline if one is set.
   *
   * @return the epoch deadline, or empty if none is set
   * @since 1.0.0
   */
  Optional<Long> epochDeadline();

  /**
   * Sets an epoch deadline for the caller.
   *
   * <p>This allows host functions to control execution limits. When the epoch counter reaches or
   * exceeds the deadline, execution will be interrupted.
   *
   * @param deadline the epoch deadline to set
   * @throws WasmException if setting the epoch deadline fails
   * @since 1.0.0
   */
  void setEpochDeadline(long deadline) throws WasmException;

  // ===== Additional Caller Methods =====

  /**
   * Gets an export using a pre-computed ModuleExport reference.
   *
   * <p>This method provides faster export lookup compared to string-based lookup because it avoids
   * string comparison on every call. The ModuleExport can be obtained from the module's export list
   * and cached for repeated use.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * // Cache the export reference at initialization
   * ModuleExport memoryExport = module.getExports().stream()
   *     .filter(e -> e.getName().equals("memory"))
   *     .findFirst().orElseThrow();
   *
   * // Use it in hot path for faster lookup
   * Optional<Memory> memory = caller.getExportByModuleExport(memoryExport);
   * }</pre>
   *
   * @param moduleExport the pre-computed module export reference
   * @return the export if it exists, empty otherwise
   * @throws IllegalArgumentException if moduleExport is null
   * @since 1.0.0
   */
  Optional<Export> getExportByModuleExport(ModuleExport moduleExport);

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
   * Gets or sets the fuel async yield interval from within a host function.
   *
   * <p>This allows dynamic adjustment of the fuel-based async yielding interval during execution.
   * Setting a non-zero value enables fuel-based cooperative scheduling.
   *
   * @return the current fuel async yield interval
   * @since 1.0.0
   */
  Optional<Long> fuelAsyncYieldInterval();

  /**
   * Sets the fuel async yield interval from within a host function.
   *
   * @param interval the interval (0 to disable)
   * @throws WasmException if setting fails
   * @since 1.0.0
   */
  void setFuelAsyncYieldInterval(long interval) throws WasmException;
}
