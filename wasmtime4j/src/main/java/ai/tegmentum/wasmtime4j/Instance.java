package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.util.Optional;

/**
 * Represents an instantiated WebAssembly module.
 *
 * <p>An Instance is a running instance of a WebAssembly module with its own execution state,
 * including linear memory, globals, and function contexts. Instances provide access to exported
 * functions that can be called from Java.
 *
 * <p>Each instance maintains separate state even when created from the same module, allowing for
 * isolated execution contexts.
 *
 * @since 1.0.0
 */
public interface Instance extends Closeable {

  /**
   * Gets an exported function by name.
   *
   * <p>Returns a function that can be called from Java code. The function maintains a reference to
   * this instance's execution context.
   *
   * @param name the name of the exported function
   * @return the exported function, or empty if not found
   * @throws IllegalArgumentException if name is null
   */
  Optional<WasmFunction> getFunction(final String name);

  /**
   * Gets an exported global by name.
   *
   * @param name the name of the exported global
   * @return the exported global, or empty if not found
   * @throws IllegalArgumentException if name is null
   */
  Optional<WasmGlobal> getGlobal(final String name);

  /**
   * Gets an exported memory by name.
   *
   * @param name the name of the exported memory
   * @return the exported memory, or empty if not found
   * @throws IllegalArgumentException if name is null
   */
  Optional<WasmMemory> getMemory(final String name);

  /**
   * Gets an exported table by name.
   *
   * @param name the name of the exported table
   * @return the exported table, or empty if not found
   * @throws IllegalArgumentException if name is null
   */
  Optional<WasmTable> getTable(final String name);

  /**
   * Gets the default linear memory if the module exports one.
   *
   * <p>This is a convenience method that returns the first exported memory, which is typically
   * named "memory" in WebAssembly modules.
   *
   * @return the default memory, or empty if no memory is exported
   */
  Optional<WasmMemory> getDefaultMemory();

  /**
   * Gets all export names from this instance.
   *
   * @return an array of export names
   */
  String[] getExportNames();

  /**
   * Gets runtime type information for all exports.
   *
   * <p>This method provides comprehensive type introspection for all exported items, including
   * their specific type information at runtime.
   *
   * @return an immutable list of export descriptors with runtime type information
   * @since 1.0.0
   */
  java.util.List<ExportDescriptor> getExportDescriptors();

  /**
   * Gets runtime type information for a specific export.
   *
   * @param name the name of the export
   * @return the export descriptor with type information, or empty if not found
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  java.util.Optional<ExportDescriptor> getExportDescriptor(final String name);

  /**
   * Gets the runtime function type for a specific exported function.
   *
   * @param functionName the name of the exported function
   * @return the function type, or empty if the function doesn't exist or isn't a function
   * @throws IllegalArgumentException if functionName is null
   * @since 1.0.0
   */
  java.util.Optional<FuncType> getFunctionType(final String functionName);

  /**
   * Gets the runtime global type for a specific exported global.
   *
   * @param globalName the name of the exported global
   * @return the global type, or empty if the global doesn't exist or isn't a global
   * @throws IllegalArgumentException if globalName is null
   * @since 1.0.0
   */
  java.util.Optional<GlobalType> getGlobalType(final String globalName);

  /**
   * Gets the runtime memory type for a specific exported memory.
   *
   * @param memoryName the name of the exported memory
   * @return the memory type, or empty if the memory doesn't exist or isn't a memory
   * @throws IllegalArgumentException if memoryName is null
   * @since 1.0.0
   */
  java.util.Optional<MemoryType> getMemoryType(final String memoryName);

  /**
   * Gets the runtime table type for a specific exported table.
   *
   * @param tableName the name of the exported table
   * @return the table type, or empty if the table doesn't exist or isn't a table
   * @throws IllegalArgumentException if tableName is null
   * @since 1.0.0
   */
  java.util.Optional<TableType> getTableType(final String tableName);

  /**
   * Checks if this instance exports a specific item at runtime.
   *
   * @param name the name of the export to check
   * @return true if the instance exports an item with this name
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  boolean hasExport(final String name);

  /**
   * Gets the module that this instance was created from.
   *
   * @return the Module used to create this instance
   */
  Module getModule();

  /**
   * Gets the store that contains this instance.
   *
   * @return the Store containing this instance
   */
  Store getStore();

  /**
   * Calls a WebAssembly function with the given parameters.
   *
   * <p>This is a convenience method that combines function lookup and invocation.
   *
   * @param functionName the name of the function to call
   * @param params the parameters to pass to the function
   * @return the result of the function call
   * @throws WasmException if the function doesn't exist or execution fails
   * @throws IllegalArgumentException if functionName is null
   */
  WasmValue[] callFunction(final String functionName, final WasmValue... params)
      throws WasmException;

  /**
   * Checks if the instance is still valid and usable.
   *
   * @return true if the instance is valid, false otherwise
   */
  boolean isValid();

  /**
   * Closes the instance and releases associated resources.
   *
   * <p>After closing, the instance becomes invalid and should not be used.
   */
  @Override
  void close();

  /**
   * Creates an instance of a WebAssembly module in the given store.
   *
   * @param store the store to create the instance in
   * @param module the compiled module to instantiate
   * @return a new Instance of the module
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if store or module is null
   */
  static Instance create(final Store store, final Module module) throws WasmException {
    return store.createInstance(module);
  }
}
