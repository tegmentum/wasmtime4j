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
}
