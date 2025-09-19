package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * WebAssembly linker interface for defining host functions and resolving imports.
 *
 * <p>A Linker provides the mechanism to define host functions and bind imports before instantiating
 * WebAssembly modules. It serves as a pre-instantiation environment where you can register
 * functions, memories, tables, and globals that modules can import.
 *
 * <p>Linkers enable advanced WebAssembly integration patterns including:
 * <ul>
 *   <li>Host function binding - Define Java functions callable from WebAssembly</li>
 *   <li>Module linking - Connect multiple WebAssembly modules together</li>
 *   <li>WASI integration - Automatically provide WASI system interface functions</li>
 *   <li>Import resolution - Satisfy all module import requirements before instantiation</li>
 * </ul>
 *
 * <p>Linkers are thread-safe and can be reused across multiple module instantiations. They are
 * associated with a specific Engine and inherit its configuration.
 *
 * @since 1.0.0
 */
public interface Linker extends Closeable {

  /**
   * Defines a WebAssembly function that can be imported by WebAssembly modules.
   *
   * <p>The function will be available to any module instantiated through this linker that imports
   * a function with the specified module and name. The function type must match exactly.
   *
   * @param module the module name for the import (e.g., "env")
   * @param name the function name for the import
   * @param function the WebAssembly function to provide
   * @throws WasmException if the function cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void define(final String module, final String name, final WasmFunction function)
      throws WasmException;

  /**
   * Defines a host function that can be imported by WebAssembly modules.
   *
   * <p>The function will be available to any module instantiated through this linker that imports
   * a function with the specified module and name. The function type must match exactly.
   *
   * @param moduleName the module name for the import (e.g., "env")
   * @param name the function name for the import
   * @param functionType the WebAssembly function type signature
   * @param implementation the Java implementation of the function
   * @throws WasmException if the function cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineHostFunction(
      final String moduleName,
      final String name,
      final FunctionType functionType,
      final HostFunction implementation)
      throws WasmException;

  /**
   * Defines a host function that can be imported by WebAssembly modules.
   *
   * <p>This is a simplified version that infers the function type from the HostFunction
   * implementation. The function will be available to any module instantiated through this linker.
   *
   * @param module the module name for the import (e.g., "env")
   * @param name the function name for the import
   * @param function the Java implementation of the host function
   * @throws WasmException if the function cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineHostFunction(final String module, final String name, final HostFunction function)
      throws WasmException;

  /**
   * Defines a memory that can be imported by WebAssembly modules.
   *
   * <p>The memory will be available to any module instantiated through this linker that imports
   * a memory with the specified module and name.
   *
   * @param moduleName the module name for the import
   * @param name the memory name for the import
   * @param memory the WebAssembly memory to provide
   * @throws WasmException if the memory cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineMemory(final String moduleName, final String name, final WasmMemory memory)
      throws WasmException;

  /**
   * Defines a table that can be imported by WebAssembly modules.
   *
   * <p>The table will be available to any module instantiated through this linker that imports
   * a table with the specified module and name.
   *
   * @param moduleName the module name for the import
   * @param name the table name for the import
   * @param table the WebAssembly table to provide
   * @throws WasmException if the table cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineTable(final String moduleName, final String name, final WasmTable table)
      throws WasmException;

  /**
   * Defines a global that can be imported by WebAssembly modules.
   *
   * <p>The global will be available to any module instantiated through this linker that imports
   * a global with the specified module and name.
   *
   * @param moduleName the module name for the import
   * @param name the global name for the import
   * @param global the WebAssembly global to provide
   * @throws WasmException if the global cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineGlobal(final String moduleName, final String name, final WasmGlobal global)
      throws WasmException;

  /**
   * Defines an instance that can be imported by WebAssembly modules.
   *
   * <p>All exports from the instance will be available to any module instantiated through this
   * linker that imports from the specified module name.
   *
   * @param moduleName the module name for the import
   * @param instance the WebAssembly instance whose exports should be provided
   * @throws WasmException if the instance cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineInstance(final String moduleName, final Instance instance) throws WasmException;

  /**
   * Creates an alias for an export from one module to another.
   *
   * <p>This allows re-exporting functionality under different names or module namespaces.
   *
   * @param fromModule the source module name
   * @param fromName the source export name
   * @param toModule the destination module name
   * @param toName the destination export name
   * @throws WasmException if the alias cannot be created
   * @throws IllegalArgumentException if any parameter is null
   */
  void alias(final String fromModule, final String fromName, final String toModule, final String toName)
      throws WasmException;

  /**
   * Creates an alias for a module instance in the linker namespace.
   *
   * <p>This makes all exports from the instance available under the specified module name,
   * enabling module-to-module linking scenarios.
   *
   * @param name the module name to assign to the instance
   * @param instance the instance whose exports should be aliased
   * @throws WasmException if the alias cannot be created
   * @throws IllegalArgumentException if any parameter is null
   */
  void aliasModule(final String name, final Instance instance) throws WasmException;

  /**
   * Instantiates a WebAssembly module using this linker to resolve imports.
   *
   * <p>The linker will provide all defined functions, memories, tables, and globals to satisfy
   * the module's import requirements. If any imports cannot be satisfied, instantiation will fail.
   *
   * @param store the store to instantiate the module in
   * @param module the compiled module to instantiate
   * @return a new Instance of the module with all imports resolved
   * @throws WasmException if instantiation fails or imports cannot be satisfied
   * @throws IllegalArgumentException if store or module is null
   */
  Instance instantiate(final Store store, final Module module) throws WasmException;

  /**
   * Instantiates a WebAssembly module asynchronously using this linker to resolve imports.
   *
   * <p>This method performs instantiation asynchronously and returns a CompletableFuture
   * that will complete with the instance or complete exceptionally if instantiation fails.
   *
   * @param store the store to instantiate the module in
   * @param module the compiled module to instantiate
   * @return a CompletableFuture that will complete with the instantiated module
   * @throws IllegalArgumentException if store or module is null
   */
  CompletableFuture<Instance> instantiateAsync(final Store store, final Module module);

  /**
   * Instantiates a WebAssembly module with a specific name in the linker namespace.
   *
   * <p>The instantiated module's exports will be available for linking with other modules
   * under the specified name. This enables module-to-module linking scenarios.
   *
   * @param store the store to instantiate the module in
   * @param moduleName the name to assign to this module in the linker
   * @param module the compiled module to instantiate
   * @return a new Instance of the module with all imports resolved
   * @throws WasmException if instantiation fails or imports cannot be satisfied
   * @throws IllegalArgumentException if any parameter is null
   */
  Instance instantiate(final Store store, final String moduleName, final Module module)
      throws WasmException;

  /**
   * Enables WASI (WebAssembly System Interface) support for modules instantiated through this linker.
   *
   * <p>This automatically defines all WASI functions that modules can import, providing
   * system interface capabilities like file I/O, environment access, and process control.
   *
   * @throws WasmException if WASI cannot be enabled
   */
  void enableWasi() throws WasmException;

  /**
   * Defines WASI (WebAssembly System Interface) support with specific configuration.
   *
   * <p>This automatically defines all WASI functions that modules can import, providing
   * system interface capabilities like file I/O, environment access, and process control
   * using the specified configuration.
   *
   * @param config the WASI configuration to use
   * @throws WasmException if WASI cannot be defined
   * @throws IllegalArgumentException if config is null
   */
  void defineWasi(final WasiConfig config) throws WasmException;

  /**
   * Gets the engine associated with this linker.
   *
   * @return the Engine that created this linker
   */
  Engine getEngine();

  /**
   * Checks if the linker is still valid and usable.
   *
   * @return true if the linker is valid, false otherwise
   */
  boolean isValid();

  /**
   * Closes the linker and releases associated resources.
   *
   * <p>After closing, the linker becomes invalid and should not be used.
   */
  @Override
  void close();

  /**
   * Creates a new Linker for the given engine.
   *
   * @param engine the engine to create the linker for
   * @return a new Linker instance
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine is null
   */
  static Linker create(final Engine engine) throws WasmException {
    return WasmRuntimeFactory.create().createLinker(engine);
  }
}