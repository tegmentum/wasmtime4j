package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.TypedFunc;
import ai.tegmentum.wasmtime4j.memory.Tag;
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
   * <p>This is a convenience method that first tries the conventional name "memory", then falls
   * back to returning the first memory export found. This matches Wasmtime's behavior of returning
   * the first exported memory.
   *
   * @return the default memory, or empty if no memory is exported
   */
  default Optional<WasmMemory> getDefaultMemory() {
    final Optional<WasmMemory> namedMemory = getMemory("memory");
    if (namedMemory.isPresent()) {
      return namedMemory;
    }
    for (final String exportName : getExportNames()) {
      final Optional<WasmMemory> memory = getMemory(exportName);
      if (memory.isPresent()) {
        return memory;
      }
    }
    return Optional.empty();
  }

  /**
   * Gets an exported shared memory by name.
   *
   * <p>Shared memories are memories that can be accessed concurrently from multiple threads. They
   * are distinct from regular memories in that they use the {@code shared} attribute in their
   * memory type definition.
   *
   * <p>Requires that the engine was configured with {@code wasmThreads(true)}.
   *
   * @param name the name of the exported shared memory
   * @return the exported shared memory, or empty if not found or not a shared memory
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  Optional<WasmMemory> getSharedMemory(final String name);

  /**
   * Gets all export names from this instance.
   *
   * @return an array of export names
   */
  String[] getExportNames();

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
   * Gets an exported item by name from this instance.
   *
   * <p>This method provides a unified accessor for any export type (function, memory, table, or
   * global). Use the returned {@link Extern} to determine the type and access the value.
   *
   * @param name the name of the export to retrieve
   * @return the export if it exists, empty otherwise
   * @throws IllegalArgumentException if name is null
   * @since 1.1.0
   */
  Optional<Extern> getExport(final String name);

  /**
   * Gets an exported item using a pre-resolved {@link ModuleExport} handle for O(1) lookup.
   *
   * <p>This method provides fast export access by using a cached index handle obtained from {@link
   * Module#getModuleExport(String)}. The ModuleExport must have been obtained from the same Module
   * that this Instance was created from.
   *
   * @param store the store containing this instance
   * @param moduleExport the pre-resolved export handle
   * @return the export if it exists, empty otherwise
   * @throws IllegalArgumentException if store or moduleExport is null
   * @throws WasmException if the lookup fails
   * @since 1.1.0
   */
  Optional<Extern> getExport(final Store store, final ModuleExport moduleExport)
      throws WasmException;

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
   * Gets all exports from this instance as typed {@link Extern} values.
   *
   * <p>This method corresponds to Wasmtime's {@code Instance::exports()} which returns an iterator
   * of {@code (name, Extern)} pairs. Each entry contains the export name and its typed Extern
   * value, allowing callers to inspect or cast exports without additional lookups.
   *
   * @return a list of name-to-Extern entries for all exports in this instance
   * @since 1.1.0
   */
  default java.util.List<java.util.Map.Entry<String, Extern>> getExports() {
    final String[] names = getExportNames();
    final java.util.List<java.util.Map.Entry<String, Extern>> result =
        new java.util.ArrayList<>(names.length);
    for (final String name : names) {
      final java.util.Optional<Extern> ext = getExport(name);
      ext.ifPresent(e -> result.add(java.util.Map.entry(name, e)));
    }
    return result;
  }

  /**
   * Debug: gets a function by its internal module index.
   *
   * <p>This method requires that the engine was configured with {@code debugGuest(true)}. Returns
   * empty if debug instrumentation is not enabled or if the index is out of bounds.
   *
   * @param functionIndex the internal function index
   * @return the function at the given index, or empty if not found
   * @since 1.1.0
   */
  default Optional<WasmFunction> debugFunction(final int functionIndex) {
    return Optional.empty();
  }

  /**
   * Debug: gets a global by its internal module index.
   *
   * <p>This method requires that the engine was configured with {@code debugGuest(true)}. Returns
   * empty if debug instrumentation is not enabled or if the index is out of bounds.
   *
   * @param globalIndex the internal global index
   * @return the global at the given index, or empty if not found
   * @since 1.1.0
   */
  default Optional<WasmGlobal> debugGlobal(final int globalIndex) {
    return Optional.empty();
  }

  /**
   * Debug: gets a memory by its internal module index.
   *
   * <p>This method requires that the engine was configured with {@code debugGuest(true)}. Returns
   * empty if debug instrumentation is not enabled or if the index is out of bounds.
   *
   * @param memoryIndex the internal memory index
   * @return the memory at the given index, or empty if not found
   * @since 1.1.0
   */
  default Optional<WasmMemory> debugMemory(final int memoryIndex) {
    return Optional.empty();
  }

  /**
   * Debug: gets a shared memory by its internal module index.
   *
   * <p>This method requires that the engine was configured with {@code debugGuest(true)}. Returns
   * empty if debug instrumentation is not enabled or if the index is out of bounds. Returns empty
   * for any unshared memory (use {@link #debugMemory(int)} instead).
   *
   * @param memoryIndex the internal memory index
   * @return the shared memory at the given index, or empty if not found or not shared
   * @since 1.1.0
   */
  default Optional<WasmMemory> debugSharedMemory(final int memoryIndex) {
    return Optional.empty();
  }

  /**
   * Debug: gets a table by its internal module index.
   *
   * <p>This method requires that the engine was configured with {@code debugGuest(true)}. Returns
   * empty if debug instrumentation is not enabled or if the index is out of bounds.
   *
   * @param tableIndex the internal table index
   * @return the table at the given index, or empty if not found
   * @since 1.1.0
   */
  default Optional<WasmTable> debugTable(final int tableIndex) {
    return Optional.empty();
  }

  /**
   * Debug: gets a tag by its internal module index.
   *
   * <p>This method requires that the engine was configured with {@code debugGuest(true)}. Returns
   * empty if debug instrumentation is not enabled or if the index is out of bounds.
   *
   * @param tagIndex the internal tag index
   * @return the tag at the given index, or empty if not found
   * @since 1.1.0
   */
  default Optional<Tag> debugTag(final int tagIndex) {
    return Optional.empty();
  }

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

  /**
   * Creates an instance of a WebAssembly module with explicit imports.
   *
   * <p>The imports array must contain extern values in the same order as the module's import
   * declarations. This is the low-level instantiation API that bypasses the linker.
   *
   * @param store the store to create the instance in
   * @param module the compiled module to instantiate
   * @param imports the array of extern values to satisfy the module's imports, in order
   * @return a new Instance of the module
   * @throws WasmException if instantiation fails or imports don't match
   * @throws IllegalArgumentException if any argument is null
   */
  static Instance create(final Store store, final Module module, final Extern[] imports)
      throws WasmException {
    return store.createInstance(module, imports);
  }

  /**
   * Asynchronously creates an instance of a WebAssembly module in the given store.
   *
   * <p>This is the async variant of {@link #create(Store, Module)}. The default implementation
   * delegates to the synchronous method on the ForkJoinPool. Implementations may override to use
   * native async instantiation via Wasmtime's {@code Instance::new_async()}.
   *
   * @param store the store to create the instance in
   * @param module the compiled module to instantiate
   * @return a future that completes with a new Instance of the module
   * @throws IllegalArgumentException if store or module is null
   * @since 1.1.0
   */
  static java.util.concurrent.CompletableFuture<Instance> createAsync(
      final Store store, final Module module) {
    return java.util.concurrent.CompletableFuture.supplyAsync(
        () -> {
          try {
            return store.createInstance(module);
          } catch (final WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  /**
   * Asynchronously creates an instance of a WebAssembly module with explicit imports.
   *
   * <p>This is the async variant of {@link #create(Store, Module, Extern[])}. The default
   * implementation delegates to the synchronous method on the ForkJoinPool.
   *
   * @param store the store to create the instance in
   * @param module the compiled module to instantiate
   * @param imports the array of extern values to satisfy the module's imports, in order
   * @return a future that completes with a new Instance of the module
   * @throws IllegalArgumentException if any argument is null
   * @since 1.1.0
   */
  static java.util.concurrent.CompletableFuture<Instance> createAsync(
      final Store store, final Module module, final Extern[] imports) {
    return java.util.concurrent.CompletableFuture.supplyAsync(
        () -> {
          try {
            return store.createInstance(module, imports);
          } catch (final WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }
}
