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
 * @param <T> the type of user data associated with stores used with this linker
 * @since 1.0.0
 */
public interface Linker<T> extends Closeable {

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
   * @param store the store context (required for wasmtime's type system)
   * @param moduleName the module name for the import
   * @param name the memory name for the import
   * @param memory the WebAssembly memory to provide
   * @throws WasmException if the memory cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineMemory(final Store store, final String moduleName, final String name, final WasmMemory memory)
      throws WasmException;

  /**
   * Defines a table that can be imported by WebAssembly modules.
   *
   * <p>The table will be available to any module instantiated through this linker that imports a
   * table with the specified module and name.
   *
   * @param store the store context (required for wasmtime's type system)
   * @param moduleName the module name for the import
   * @param name the table name for the import
   * @param table the WebAssembly table to provide
   * @throws WasmException if the table cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineTable(final Store store, final String moduleName, final String name, final WasmTable table)
      throws WasmException;

  /**
   * Defines a global that can be imported by WebAssembly modules.
   *
   * <p>The global will be available to any module instantiated through this linker that imports a
   * global with the specified module and name.
   *
   * @param store the store context (required for wasmtime's type system)
   * @param moduleName the module name for the import
   * @param name the global name for the import
   * @param global the WebAssembly global to provide
   * @throws WasmException if the global cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   */
  void defineGlobal(final Store store, final String moduleName, final String name, final WasmGlobal global)
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
   * Checks if a specific import has been defined in this linker.
   *
   * <p>This method can be used to verify that required imports have been registered before
   * attempting module instantiation.
   *
   * @param moduleName the module name of the import
   * @param name the name of the import
   * @return true if the import is defined, false otherwise
   * @throws IllegalArgumentException if moduleName or name is null
   * @since 1.0.0
   */
  boolean hasImport(String moduleName, String name);

  /**
   * Resolves and validates dependencies for a set of modules.
   *
   * <p>This method analyzes the import/export relationships between modules and determines the
   * optimal instantiation order. It also validates that all dependencies can be satisfied and
   * detects circular dependencies.
   *
   * @param modules the modules to analyze for dependencies
   * @return a dependency resolution result with instantiation order and validation details
   * @throws WasmException if dependency resolution fails or circular dependencies are detected
   * @throws IllegalArgumentException if modules is null or empty
   * @since 1.0.0
   */
  DependencyResolution resolveDependencies(Module... modules) throws WasmException;

  /**
   * Validates that all imports for the given modules can be satisfied by this linker.
   *
   * <p>This method performs comprehensive validation including:
   *
   * <ul>
   *   <li>Type compatibility checking for all imports
   *   <li>Availability verification for all required imports
   *   <li>Cross-module dependency validation
   *   <li>Host function signature validation
   * </ul>
   *
   * @param modules the modules to validate imports for
   * @return a validation result with detailed information about any issues
   * @throws IllegalArgumentException if modules is null or empty
   * @since 1.0.0
   */
  ImportValidation validateImports(Module... modules);

  /**
   * Gets detailed information about all imports currently defined in this linker.
   *
   * <p>This provides comprehensive metadata about registered functions, memories, tables, globals,
   * and instances that are available for import resolution.
   *
   * @return an unmodifiable list of import information
   * @since 1.0.0
   */
  java.util.List<ImportInfo> getImportRegistry();

  /**
   * Creates an instantiation plan for multiple interdependent modules.
   *
   * <p>This method analyzes the dependency relationships and creates an optimized plan for
   * instantiating all modules in the correct order. The plan can be executed incrementally and
   * provides detailed progress tracking.
   *
   * @param modules the modules to create an instantiation plan for
   * @return an instantiation plan with ordered steps and dependency information
   * @throws WasmException if planning fails due to unresolvable dependencies
   * @throws IllegalArgumentException if modules is null or empty
   * @since 1.0.0
   */
  InstantiationPlan createInstantiationPlan(Module... modules) throws WasmException;

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
   * @param <T> the type of user data associated with stores used with this linker
   * @param engine the engine to create the linker for
   * @return a new Linker instance
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine is null
   */
  static <T> Linker<T> create(final Engine engine) throws WasmException {
    return WasmRuntimeFactory.create().createLinker(engine);
  }
}
