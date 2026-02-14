package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.async.AsyncHostFunction;
import ai.tegmentum.wasmtime4j.config.DependencyResolution;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.ExternType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.validation.ImportInfo;
import ai.tegmentum.wasmtime4j.validation.ImportValidation;
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
  void defineMemory(
      final Store store, final String moduleName, final String name, final WasmMemory memory)
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
  void defineTable(
      final Store store, final String moduleName, final String name, final WasmTable table)
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
  void defineGlobal(
      final Store store, final String moduleName, final String name, final WasmGlobal global)
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
   * Creates a pre-instantiated module optimized for fast repeated instantiation.
   *
   * <p>Pre-instantiation performs most of the expensive setup work once, allowing subsequent
   * instantiations to be significantly faster. This is particularly useful for serverless
   * functions, request handlers, or any scenario where the same module needs to be instantiated
   * multiple times.
   *
   * <p>The returned InstancePre contains a reference to the module and can create new instances
   * very quickly since import resolution and validation have been completed upfront.
   *
   * @param module the compiled module to pre-instantiate
   * @return an InstancePre that can be used to quickly create instances
   * @throws WasmException if pre-instantiation fails or imports cannot be resolved
   * @throws IllegalArgumentException if module is null
   * @since 1.0.0
   */
  InstancePre instantiatePre(final Module module) throws WasmException;

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
   * Allows subsequent definitions to shadow prior definitions.
   *
   * <p>By default, the linker will return an error if the same item is defined twice. Calling this
   * method allows later definitions to shadow earlier ones.
   *
   * @param allow true to allow shadowing, false to prohibit it (default)
   * @return this linker for method chaining
   * @since 1.0.0
   */
  Linker<T> allowShadowing(boolean allow);

  /**
   * Allows unknown exports from modules.
   *
   * <p>When set to true, the linker will not report errors for exports that are not expected or
   * defined in the linker namespace.
   *
   * @param allow true to allow unknown exports
   * @return this linker for method chaining
   * @since 1.0.0
   */
  Linker<T> allowUnknownExports(boolean allow);

  /**
   * Defines all undefined imports as trapping functions.
   *
   * <p>For any import that hasn't been explicitly defined, this method will create a function that
   * traps immediately when called. This is useful for linking modules where some imports are not
   * needed for the specific use case.
   *
   * @param store the store context for creating trap functions
   * @param module the module to define trap functions for
   * @throws WasmException if trap functions cannot be defined
   * @since 1.0.0
   */
  void defineUnknownImportsAsTraps(Store store, Module module) throws WasmException;

  /**
   * Defines all undefined imports with default values.
   *
   * <p>For any import that hasn't been explicitly defined, this method will create a function or
   * value using a sensible default (e.g., functions return default values, globals are initialized
   * to zero).
   *
   * @param store the store context for creating default values
   * @param module the module to define defaults for
   * @throws WasmException if defaults cannot be defined
   * @since 1.0.0
   */
  void defineUnknownImportsAsDefaultValues(Store store, Module module) throws WasmException;

  /**
   * Defines a function without type checking.
   *
   * <p>This is a performance optimization that skips type validation when defining host functions.
   * The caller must ensure that the function type matches exactly, as type mismatches will result
   * in undefined behavior or runtime errors.
   *
   * <p><b>Warning:</b> Use this method only when you are certain the types are correct and need the
   * performance benefit of skipping validation.
   *
   * @param store the store context
   * @param moduleName the module name for the import
   * @param name the function name for the import
   * @param functionType the WebAssembly function type signature
   * @param implementation the Java implementation of the function
   * @throws WasmException if the function cannot be defined
   * @since 1.0.0
   */
  void funcNewUnchecked(
      Store store,
      String moduleName,
      String name,
      FunctionType functionType,
      HostFunction implementation)
      throws WasmException;

  /**
   * Iterates over all definitions in this linker.
   *
   * <p>Returns an iterator over all module/name pairs that have been defined.
   *
   * @return an iterable of module/name pairs
   * @since 1.0.0
   */
  Iterable<LinkerDefinition> iter();

  /**
   * Gets a definition by its import specifier.
   *
   * <p>Looks up a specific definition by module name and item name.
   *
   * @param store the store context
   * @param moduleName the module name
   * @param name the item name
   * @return the extern value, or null if not found
   * @since 1.0.0
   */
  Extern getByImport(Store store, String moduleName, String name);

  /**
   * Gets the default function for a module.
   *
   * <p>Returns the default export from a module, which is typically the main entry point (usually
   * named "" or "_start").
   *
   * @param store the store context
   * @param moduleName the module name
   * @return the default function, or null if none
   * @since 1.0.0
   */
  WasmFunction getDefault(Store store, String moduleName);

  /**
   * Defines an async host function that can be imported by WebAssembly modules.
   *
   * <p>Async functions allow for non-blocking operations during WebAssembly execution. When the
   * function yields, the WebAssembly execution is paused until the async operation completes.
   *
   * @param moduleName the module name for the import
   * @param name the function name for the import
   * @param functionType the WebAssembly function type signature
   * @param implementation the async Java implementation
   * @throws WasmException if the function cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   * @since 1.1.0
   */
  default void funcNewAsync(
      final String moduleName,
      final String name,
      final FunctionType functionType,
      final AsyncHostFunction implementation)
      throws WasmException {
    throw new UnsupportedOperationException("Async functions not supported in this implementation");
  }

  /**
   * Wraps an async function with the given implementation.
   *
   * <p>This is a convenience method for defining async host functions with automatic type inference
   * from the implementation.
   *
   * @param moduleName the module name for the import
   * @param name the function name for the import
   * @param implementation the async Java implementation
   * @throws WasmException if the function cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   * @since 1.1.0
   */
  default void funcWrapAsync(
      final String moduleName, final String name, final AsyncHostFunction implementation)
      throws WasmException {
    throw new UnsupportedOperationException("Async functions not supported in this implementation");
  }

  /**
   * Creates an alias for an entire module's exports.
   *
   * <p>This makes all exports from the source module available under the destination module name,
   * allowing for namespace renaming.
   *
   * @param fromModule the source module name
   * @param toModule the destination module name
   * @throws WasmException if the alias cannot be created
   * @throws IllegalArgumentException if any parameter is null
   * @since 1.1.0
   */
  default void aliasModule(final String fromModule, final String toModule) throws WasmException {
    throw new UnsupportedOperationException("Module aliasing not supported in this implementation");
  }

  // ===== Top-Level Name Definition =====

  /**
   * Defines an item at the top-level namespace without a module prefix.
   *
   * <p>This method allows defining items that can be imported with a single-level name, which is
   * useful for default module resolution or when working with modules that use simple import names.
   *
   * <p>This corresponds to wasmtime's single-level naming feature where items can be defined
   * without the traditional "module::name" hierarchy.
   *
   * @param store the store context for the definition
   * @param name the name for the definition (at top level, no module prefix)
   * @param extern the extern value to define (function, memory, table, or global)
   * @throws WasmException if the definition cannot be created
   * @throws IllegalArgumentException if any parameter is null
   * @since 1.0.0
   */
  void defineName(Store store, String name, Extern extern) throws WasmException;

  /**
   * Defines a host function at the top-level namespace without a module prefix.
   *
   * <p>This is a convenience method for defining host functions with single-level names.
   *
   * @param name the name for the function (at top level)
   * @param functionType the WebAssembly function type signature
   * @param implementation the Java implementation of the function
   * @throws WasmException if the function cannot be defined
   * @throws IllegalArgumentException if any parameter is null
   * @since 1.0.0
   */
  default void defineName(
      final String name, final FunctionType functionType, final HostFunction implementation)
      throws WasmException {
    defineHostFunction("", name, functionType, implementation);
  }

  /**
   * Closes the linker and releases associated resources.
   *
   * <p>After closing, the linker becomes invalid and should not be used.
   */
  @Override
  void close();

  /**
   * Represents a definition in the linker.
   *
   * @since 1.0.0
   */
  final class LinkerDefinition {
    private final String moduleName;
    private final String name;
    private final ExternType type;

    /**
     * Creates a new linker definition.
     *
     * @param moduleName the module name
     * @param name the item name
     * @param type the extern type
     */
    public LinkerDefinition(final String moduleName, final String name, final ExternType type) {
      this.moduleName = moduleName;
      this.name = name;
      this.type = type;
    }

    /**
     * Gets the module name.
     *
     * @return the module name
     */
    public String getModuleName() {
      return moduleName;
    }

    /**
     * Gets the item name.
     *
     * @return the item name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the extern type.
     *
     * @return the extern type
     */
    public ExternType getType() {
      return type;
    }
  }

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
    return engine.getRuntime().createLinker(engine);
  }
}
