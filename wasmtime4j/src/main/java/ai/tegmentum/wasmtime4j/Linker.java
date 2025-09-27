package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.Closeable;

/**
 * WebAssembly linker interface for defining host functions and resolving imports.
 *
 * <p>A Linker provides the mechanism to define host functions and bind imports before instantiating
 * WebAssembly modules. It serves as a pre-instantiation environment where you can register
 * functions, memories, tables, and globals that modules can import.
 *
 * <p>Linkers enable advanced WebAssembly integration patterns including:
 *
 * <ul>
 *   <li>Host function binding - Define Java functions callable from WebAssembly
 *   <li>Module linking - Connect multiple WebAssembly modules together
 *   <li>WASI integration - Automatically provide WASI system interface functions
 *   <li>Import resolution - Satisfy all module import requirements before instantiation
 * </ul>
 *
 * <p>Linkers are thread-safe and can be reused across multiple module instantiations. They are
 * associated with a specific Engine and inherit its configuration.
 *
 * @since 1.0.0
 */
public interface Linker extends Closeable {

  /**
   * Defines a host function that can be imported by WebAssembly modules.
   *
   * <p>The function will be available to any module instantiated through this linker that imports a
   * function with the specified module and name. The function type must match exactly.
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
   * Defines a memory that can be imported by WebAssembly modules.
   *
   * <p>The memory will be available to any module instantiated through this linker that imports a
   * memory with the specified module and name.
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
   * <p>The table will be available to any module instantiated through this linker that imports a
   * table with the specified module and name.
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
   * <p>The global will be available to any module instantiated through this linker that imports a
   * global with the specified module and name.
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
  void alias(
      final String fromModule, final String fromName, final String toModule, final String toName)
      throws WasmException;

  /**
   * Instantiates a WebAssembly module using this linker to resolve imports.
   *
   * <p>The linker will provide all defined functions, memories, tables, and globals to satisfy the
   * module's import requirements. If any imports cannot be satisfied, instantiation will fail.
   *
   * @param store the store to instantiate the module in
   * @param module the compiled module to instantiate
   * @return a new Instance of the module with all imports resolved
   * @throws WasmException if instantiation fails or imports cannot be satisfied
   * @throws IllegalArgumentException if store or module is null
   */
  Instance instantiate(final Store store, final Module module) throws WasmException;

  /**
   * Instantiates a WebAssembly module with a specific name in the linker namespace.
   *
   * <p>The instantiated module's exports will be available for linking with other modules under the
   * specified name. This enables module-to-module linking scenarios.
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
   * Enables WASI (WebAssembly System Interface) support for modules instantiated through this
   * linker.
   *
   * <p>This automatically defines all WASI functions that modules can import, providing system
   * interface capabilities like file I/O, environment access, and process control.
   *
   * @throws WasmException if WASI cannot be enabled
   */
  void enableWasi() throws WasmException;

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
   * Disposes of this linker, releasing resources immediately.
   *
   * <p>This method provides explicit resource cleanup, allowing linkers to be disposed of before
   * being closed. Once disposed, the linker becomes invalid and should not be used.
   *
   * @return true if disposal was successful, false if already disposed
   * @throws WasmException if disposal fails
   * @since 1.0.0
   */
  boolean dispose() throws WasmException;

  /**
   * Gets the number of host functions defined in this linker.
   *
   * <p>Returns the count of host functions that have been registered with this linker and are
   * available for import by modules.
   *
   * @return the number of host functions
   * @since 1.0.0
   */
  int getHostFunctionCount();

  /**
   * Gets the number of import definitions available in this linker.
   *
   * <p>Returns the total count of all imports (functions, globals, memories, tables) that this
   * linker can provide to modules.
   *
   * @return the number of available imports
   * @since 1.0.0
   */
  int getImportCount();

  /**
   * Gets the number of successful instantiations performed by this linker.
   *
   * <p>This counter tracks how many times this linker has successfully instantiated modules.
   *
   * @return the number of successful instantiations
   * @since 1.0.0
   */
  long getInstantiationCount();

  /**
   * Checks if WASI support is enabled for this linker.
   *
   * <p>Returns true if WASI functions have been made available through this linker for modules to
   * import.
   *
   * @return true if WASI is enabled, false otherwise
   * @since 1.0.0
   */
  boolean isWasiEnabled();

  /**
   * Gets the creation timestamp of this linker in microseconds.
   *
   * <p>This timestamp represents when the linker was created, measured from the Unix epoch in
   * microseconds.
   *
   * @return the creation timestamp in microseconds since Unix epoch
   * @since 1.0.0
   */
  long getCreatedAtMicros();

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

  /**
   * Creates a new Linker with custom configuration.
   *
   * <p>This factory method allows creating a linker with specific settings such as allowing unknown
   * exports and enabling import shadowing.
   *
   * @param engine the engine to create the linker for
   * @param allowUnknownExports whether to allow modules with unknown exports
   * @param allowShadowing whether to allow import shadowing
   * @return a new Linker instance with the specified configuration
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine is null
   * @since 1.0.0
   */
  static Linker create(
      final Engine engine, final boolean allowUnknownExports, final boolean allowShadowing)
      throws WasmException {
    return WasmRuntimeFactory.create().createLinker(engine, allowUnknownExports, allowShadowing);
  }
}
