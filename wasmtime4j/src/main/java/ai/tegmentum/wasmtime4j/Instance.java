package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.TypedFunc;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.TableType;
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
    if (name == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (paramTypes == null) {
      throw new IllegalArgumentException("Parameter types cannot be null");
    }

    final Optional<WasmFunction> funcOpt = getFunction(name);
    if (funcOpt.isEmpty()) {
      return Optional.empty();
    }

    final WasmFunction func = funcOpt.get();
    final ai.tegmentum.wasmtime4j.type.FunctionType funcType = func.getFunctionType();
    final WasmValueType[] actualParams = funcType.getParamTypes();

    // Validate parameter count and types match
    if (actualParams.length != paramTypes.length) {
      return Optional.empty();
    }
    for (int i = 0; i < actualParams.length; i++) {
      if (actualParams[i] != paramTypes[i]) {
        return Optional.empty();
      }
    }

    // Build signature string: params->results (e.g., "ii->i")
    final StringBuilder sig = new StringBuilder();
    for (final WasmValueType pt : actualParams) {
      sig.append(wasmTypeToSignatureChar(pt));
    }
    sig.append("->");
    final WasmValueType[] resultTypes = funcType.getReturnTypes();
    if (resultTypes.length == 0) {
      sig.append("v");
    } else {
      for (final WasmValueType rt : resultTypes) {
        sig.append(wasmTypeToSignatureChar(rt));
      }
    }

    if (!(func instanceof TypedFunc.TypedFunctionSupport)) {
      return Optional.empty();
    }

    return Optional.of(TypedFunc.create(func, sig.toString()));
  }

  /**
   * Converts a WasmValueType to its signature character representation.
   *
   * @param type the value type
   * @return the signature character
   */
  private static char wasmTypeToSignatureChar(final WasmValueType type) {
    switch (type) {
      case I32:
        return 'i';
      case I64:
        return 'I';
      case F32:
        return 'f';
      case F64:
        return 'F';
      default:
        throw new IllegalArgumentException("Unsupported type for typed functions: " + type);
    }
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
   * @return true if cleanup was performed, false if already cleaned up
   * @throws WasmException if cleanup fails
   * @since 1.0.0
   * @deprecated Use {@link #dispose()} or {@link #close()} instead. This method provides no
   *     additional cleanup beyond dispose().
   */
  @Deprecated
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
