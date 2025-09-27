package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;

/**
 * Provides access to WebAssembly execution context from within host functions.
 *
 * <p>A Caller represents the calling WebAssembly instance's context and allows host functions
 * to inspect and manipulate the caller's state, including accessing exported memories, tables,
 * globals, and functions.
 *
 * <p>This interface is particularly useful for host functions that need to:
 * <ul>
 *   <li>Access the caller's linear memory for reading/writing data
 *   <li>Inspect or modify global variables from the calling instance
 *   <li>Access tables for function references or other data
 *   <li>Call back into WebAssembly functions from the same instance
 * </ul>
 *
 * <p>Caller instances are only valid within the scope of a host function call and should not
 * be stored or used outside of that context.
 *
 * <p>Example usage in a host function:
 * <pre>{@code
 * public class MyHostFunction implements HostFunction {
 *     public Object[] call(Caller caller, Object... args) throws WasmException {
 *         // Access the caller's memory
 *         Optional<Memory> memory = caller.getMemory("memory");
 *         if (memory.isPresent()) {
 *             // Read/write to WebAssembly memory
 *             byte[] data = memory.get().read(0, 100);
 *             // ...
 *         }
 *         return new Object[0];
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface Caller {

  /**
   * Gets an exported memory from the calling WebAssembly instance.
   *
   * <p>This method allows host functions to access the linear memory of the calling
   * WebAssembly instance. The memory can be used for reading and writing data
   * that is shared between the host and WebAssembly code.
   *
   * @param name the name of the memory export to retrieve
   * @return the WebAssembly memory if found, or empty if not exported
   * @throws WasmException if the export exists but is not a memory
   * @throws IllegalArgumentException if name is null
   */
  Optional<Memory> getMemory(final String name) throws WasmException;

  /**
   * Gets an exported table from the calling WebAssembly instance.
   *
   * <p>Tables store references to functions or other WebAssembly values and can be
   * accessed and modified by host functions.
   *
   * @param name the name of the table export to retrieve
   * @return the WebAssembly table if found, or empty if not exported
   * @throws WasmException if the export exists but is not a table
   * @throws IllegalArgumentException if name is null
   */
  Optional<Table> getTable(final String name) throws WasmException;

  /**
   * Gets an exported global from the calling WebAssembly instance.
   *
   * <p>Globals represent mutable or immutable values that can be shared between
   * the host and WebAssembly code.
   *
   * @param name the name of the global export to retrieve
   * @return the WebAssembly global if found, or empty if not exported
   * @throws WasmException if the export exists but is not a global
   * @throws IllegalArgumentException if name is null
   */
  Optional<Global> getGlobal(final String name) throws WasmException;

  /**
   * Gets an exported function from the calling WebAssembly instance.
   *
   * <p>This allows host functions to call back into WebAssembly functions from
   * the same instance, enabling complex interaction patterns.
   *
   * @param name the name of the function export to retrieve
   * @return the WebAssembly function if found, or empty if not exported
   * @throws WasmException if the export exists but is not a function
   * @throws IllegalArgumentException if name is null
   */
  Optional<Function> getFunction(final String name) throws WasmException;

  /**
   * Gets the default memory export from the calling WebAssembly instance.
   *
   * <p>This is a convenience method that looks for a memory export named "memory"
   * or returns the first memory export if available.
   *
   * @return the default memory if found, or empty if no memory exports exist
   * @throws WasmException if memory access fails
   */
  Optional<Memory> getDefaultMemory() throws WasmException;

  /**
   * Checks if the calling instance exports a specific item.
   *
   * @param name the name of the export to check
   * @return true if the calling instance exports an item with this name
   * @throws IllegalArgumentException if name is null
   */
  boolean hasExport(final String name);

  /**
   * Gets the type of a specific export from the calling instance.
   *
   * @param name the name of the export to check
   * @return the export type if found, or empty if not exported
   * @throws IllegalArgumentException if name is null
   */
  Optional<ExportType> getExportType(final String name);

  /**
   * Gets user data associated with the caller's store.
   *
   * <p>This allows host functions to access any custom data that was associated
   * with the store when it was created.
   *
   * @param <T> the type of the user data
   * @return the user data if present, or null if no data was associated
   */
  <T> T getStoreData();

  /**
   * Gets fuel remaining in the calling instance's store.
   *
   * <p>Fuel is used to limit the execution time of WebAssembly code. This method
   * allows host functions to check how much fuel remains before the instance
   * runs out and execution is terminated.
   *
   * @return the amount of fuel remaining, or empty if fuel is not configured
   * @throws WasmException if fuel information cannot be accessed
   */
  Optional<Long> getFuelRemaining() throws WasmException;

  /**
   * Consumes a specified amount of fuel from the calling instance's store.
   *
   * <p>This allows host functions to consume fuel as if they were performing
   * computational work, ensuring that long-running host functions are also
   * subject to fuel limits.
   *
   * @param amount the amount of fuel to consume
   * @throws WasmException if fuel consumption fails or insufficient fuel remains
   * @throws IllegalArgumentException if amount is negative
   */
  void consumeFuel(final long amount) throws WasmException;

  /**
   * Checks if this caller context is still valid.
   *
   * <p>Caller instances are only valid within the scope of a host function call.
   * Once the host function returns, the caller becomes invalid.
   *
   * @return true if the caller is valid and can be used, false otherwise
   */
  boolean isValid();

  /**
   * Enumeration of WebAssembly export types.
   */
  enum ExportType {
    /** Function export */
    FUNCTION,
    /** Memory export */
    MEMORY,
    /** Table export */
    TABLE,
    /** Global export */
    GLOBAL
  }
}