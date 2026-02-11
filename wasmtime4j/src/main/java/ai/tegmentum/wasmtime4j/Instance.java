package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.func.TypedFunc;

import ai.tegmentum.wasmtime4j.type.TableType;

import ai.tegmentum.wasmtime4j.type.MemoryType;

import ai.tegmentum.wasmtime4j.type.GlobalType;

import ai.tegmentum.wasmtime4j.type.FuncType;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
   * Gets an exported function by index.
   *
   * <p>Functions are indexed in the order they appear in the module's export section.
   *
   * @param index the index of the function
   * @return the exported function, or empty if not found or not a function
   * @throws IllegalArgumentException if index is negative
   */
  java.util.Optional<WasmFunction> getFunction(final int index);

  /**
   * Gets an exported tag by name.
   *
   * <p>Tags are used in the WebAssembly exception handling proposal to identify exception types.
   * Returns a tag that can be used for exception handling operations.
   *
   * @param name the name of the exported tag
   * @return the exported tag, or empty if not found
   * @throws IllegalArgumentException if name is null
   * @since 1.1.0
   */
  default Optional<Tag> getTag(final String name) {
    return Optional.empty();
  }

  /**
   * Gets an exported typed function by name with signature validation.
   *
   * <p>This method returns a TypedFunc that provides compile-time type safety for function calls.
   * The function signature is validated against the expected parameter and result types.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * TypedFunc<Integer, Integer> addFunc = instance.getTypedFunc("add",
   *     WasmValueType.I32, WasmValueType.I32);
   * int result = addFunc.call(10, 20);
   * }</pre>
   *
   * @param name the name of the exported function
   * @param paramTypes the expected parameter types
   * @return the typed function, or empty if not found or signature mismatch
   * @throws IllegalArgumentException if name or paramTypes is null
   * @since 1.1.0
   */
  default Optional<TypedFunc> getTypedFunc(final String name, final WasmValueType... paramTypes) {
    return Optional.empty();
  }

  /**
   * Gets an exported global by name.
   *
   * @param name the name of the exported global
   * @return the exported global, or empty if not found
   * @throws IllegalArgumentException if name is null
   */
  Optional<WasmGlobal> getGlobal(final String name);

  /**
   * Gets an exported global by index.
   *
   * <p>Globals are indexed in the order they appear in the module's export section.
   *
   * @param index the index of the global
   * @return the exported global, or empty if not found or not a global
   * @throws IllegalArgumentException if index is negative
   */
  java.util.Optional<WasmGlobal> getGlobal(final int index);

  /**
   * Gets an exported memory by name.
   *
   * @param name the name of the exported memory
   * @return the exported memory, or empty if not found
   * @throws IllegalArgumentException if name is null
   */
  Optional<WasmMemory> getMemory(final String name);

  /**
   * Gets an exported memory by index.
   *
   * <p>Memories are indexed in the order they appear in the module's export section.
   *
   * @param index the index of the memory
   * @return the exported memory, or empty if not found or not a memory
   * @throws IllegalArgumentException if index is negative
   */
  java.util.Optional<WasmMemory> getMemory(final int index);

  /**
   * Gets an exported table by name.
   *
   * @param name the name of the exported table
   * @return the exported table, or empty if not found
   * @throws IllegalArgumentException if name is null
   */
  Optional<WasmTable> getTable(final String name);

  /**
   * Gets an exported table by index.
   *
   * <p>Tables are indexed in the order they appear in the module's export section.
   *
   * @param index the index of the table
   * @return the exported table, or empty if not found or not a table
   * @throws IllegalArgumentException if index is negative
   */
  java.util.Optional<WasmTable> getTable(final int index);

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
   * Gets all exports from this instance as a map.
   *
   * <p>This method provides a convenient way to access all exports at once. The map keys are export
   * names and values are the exported objects.
   *
   * @return a map of export names to exported objects
   */
  java.util.Map<String, Object> getAllExports();

  /**
   * Sets imports for this instance (used during instantiation).
   *
   * <p>This method is typically used internally during module instantiation to provide the required
   * imports.
   *
   * @param imports a map of import names to import objects
   * @throws IllegalArgumentException if imports is null
   * @throws WasmException if setting imports fails
   */
  void setImports(final java.util.Map<String, Object> imports) throws WasmException;

  /**
   * Gets runtime statistics for this instance.
   *
   * <p>Statistics include information about execution time, memory usage, function calls, and other
   * runtime metrics.
   *
   * @return the instance statistics
   * @throws WasmException if statistics collection fails
   */
  InstanceStatistics getStatistics() throws WasmException;

  /**
   * Gets the current lifecycle state of this instance.
   *
   * <p>Instance state tracking provides visibility into the instance lifecycle for proper resource
   * management and debugging.
   *
   * @return the current instance state
   * @since 1.0.0
   */
  InstanceState getState();

  /**
   * Performs comprehensive resource cleanup for this instance.
   *
   * <p>This method goes beyond dispose() to perform deep cleanup of all associated resources,
   * including native memory, function references, and internal state. Unlike dispose(), this method
   * ensures complete resource cleanup and can be safely called multiple times.
   *
   * @return true if cleanup was performed, false if already cleaned up
   * @throws WasmException if cleanup fails
   * @since 1.0.0
   */
  boolean cleanup() throws WasmException;

  /**
   * Checks if the instance is still valid and usable.
   *
   * @return true if the instance is valid, false otherwise
   */
  boolean isValid();

  /**
   * Disposes of this instance, releasing resources immediately.
   *
   * <p>This method provides explicit resource cleanup, allowing instances to be disposed of before
   * the store is closed. Once disposed, the instance becomes invalid and should not be used.
   *
   * @return true if disposal was successful, false if already disposed
   * @throws WasmException if disposal fails
   * @since 1.0.0
   */
  boolean dispose() throws WasmException;

  /**
   * Checks if this instance has been disposed.
   *
   * <p>Disposed instances are no longer usable and will throw exceptions if operations are
   * attempted on them.
   *
   * @return true if the instance has been disposed, false otherwise
   * @since 1.0.0
   */
  boolean isDisposed();

  /**
   * Gets the creation timestamp of this instance in microseconds.
   *
   * <p>This timestamp represents when the instance was created, measured from the Unix epoch in
   * microseconds.
   *
   * @return the creation timestamp in microseconds since Unix epoch
   * @since 1.0.0
   */
  long getCreatedAtMicros();

  /**
   * Gets the count of metadata exports in this instance.
   *
   * <p>Metadata exports include debugging information, custom sections, and other non-executable
   * exports that provide information about the module structure.
   *
   * @return the number of metadata exports
   * @since 1.0.0
   */
  int getMetadataExportCount();

  /**
   * Calls a 32-bit integer function with parameters.
   *
   * <p>This is an optimized calling convention for functions that take 32-bit integer parameters
   * and return a 32-bit integer result.
   *
   * @param functionName the name of the function to call
   * @param params array of 32-bit integer parameters
   * @return the 32-bit integer result
   * @throws WasmException if the function call fails or function doesn't exist
   * @throws IllegalArgumentException if functionName is null
   * @since 1.0.0
   */
  int callI32Function(final String functionName, final int... params) throws WasmException;

  /**
   * Calls a 32-bit integer function with no parameters.
   *
   * <p>This is an optimized calling convention for functions that take no parameters and return a
   * 32-bit integer result.
   *
   * @param functionName the name of the function to call
   * @return the 32-bit integer result
   * @throws WasmException if the function call fails or function doesn't exist
   * @throws IllegalArgumentException if functionName is null
   * @since 1.0.0
   */
  int callI32Function(final String functionName) throws WasmException;

  /**
   * Closes the instance and releases associated resources.
   *
   * <p>After closing, the instance becomes invalid and should not be used.
   */
  @Override
  void close();

  // ===== Async Instance Creation Methods =====

  /**
   * Creates an instance of a WebAssembly module asynchronously.
   *
   * <p>This method performs module instantiation in an async context, allowing the operation to
   * yield during start function execution if the module uses async features.
   *
   * <p>This is useful when:
   *
   * <ul>
   *   <li>The module's start function may perform async operations
   *   <li>Resource limiting with async callbacks is enabled
   *   <li>The instantiation may take significant time
   * </ul>
   *
   * <p><b>Note:</b> The async feature must be enabled in the engine configuration.
   *
   * @param store the store to create the instance in
   * @param module the compiled module to instantiate
   * @return a future that completes with the new Instance
   * @throws IllegalArgumentException if store or module is null
   * @since 1.1.0
   */
  static CompletableFuture<Instance> createAsync(final Store store, final Module module) {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return store.createInstance(module);
          } catch (WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

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
