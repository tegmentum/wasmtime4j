package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.Closeable;
import java.util.List;

/**
 * Represents a compiled WebAssembly module.
 *
 * <p>A Module is the result of compiling WebAssembly bytecode and contains all the information
 * needed to create instances. Modules are immutable after compilation and can be instantiated
 * multiple times to create separate execution contexts.
 *
 * <p>Modules contain metadata about exports, imports, and the compiled code that can be queried for
 * introspection purposes.
 *
 * @since 1.0.0
 */
public interface Module extends Closeable {

  /**
   * Creates an instance of this module in the given store.
   *
   * <p>Each instance represents a separate execution context with its own linear memory, globals,
   * and runtime state.
   *
   * @param store the store to create the instance in
   * @return a new Instance of this module
   * @throws WasmException if instantiation fails
   * @throws IllegalArgumentException if store is null
   */
  Instance instantiate(final Store store) throws WasmException;

  /**
   * Creates an instance of this module with the provided imports.
   *
   * @param store the store to create the instance in
   * @param imports the import definitions for the module
   * @return a new Instance of this module with the specified imports
   * @throws WasmException if instantiation fails or imports don't match requirements
   * @throws IllegalArgumentException if store or imports is null
   */
  Instance instantiate(final Store store, final ImportMap imports) throws WasmException;

  /**
   * Gets the list of exports defined by this module.
   *
   * <p>Exports represent functions, globals, memories, and tables that this module makes available
   * to the host environment.
   *
   * @return an immutable list of export definitions
   */
  List<ExportType> getExports();

  /**
   * Gets the list of imports required by this module.
   *
   * <p>Imports represent functions, globals, memories, and tables that this module expects to be
   * provided by the host environment.
   *
   * @return an immutable list of import definitions
   */
  List<ImportType> getImports();

  /**
   * Gets detailed export descriptors with comprehensive type information.
   *
   * <p>This method provides more detailed type introspection than {@link #getExports()}, including
   * specific type information for functions, globals, memories, and tables.
   *
   * @return an immutable list of detailed export descriptors
   * @since 1.0.0
   */
  List<ExportDescriptor> getExportDescriptors();

  /**
   * Gets detailed import descriptors with comprehensive type information.
   *
   * <p>This method provides more detailed type introspection than {@link #getImports()}, including
   * specific type information for functions, globals, memories, and tables.
   *
   * @return an immutable list of detailed import descriptors
   * @since 1.0.0
   */
  List<ImportDescriptor> getImportDescriptors();

  /**
   * Gets the function type for a specific exported function.
   *
   * @param functionName the name of the exported function
   * @return the function type, or empty if the function doesn't exist or isn't a function
   * @throws IllegalArgumentException if functionName is null
   * @since 1.0.0
   */
  java.util.Optional<FuncType> getFunctionType(final String functionName);

  /**
   * Gets the global type for a specific exported global.
   *
   * @param globalName the name of the exported global
   * @return the global type, or empty if the global doesn't exist or isn't a global
   * @throws IllegalArgumentException if globalName is null
   * @since 1.0.0
   */
  java.util.Optional<GlobalType> getGlobalType(final String globalName);

  /**
   * Gets the memory type for a specific exported memory.
   *
   * @param memoryName the name of the exported memory
   * @return the memory type, or empty if the memory doesn't exist or isn't a memory
   * @throws IllegalArgumentException if memoryName is null
   * @since 1.0.0
   */
  java.util.Optional<MemoryType> getMemoryType(final String memoryName);

  /**
   * Gets the table type for a specific exported table.
   *
   * @param tableName the name of the exported table
   * @return the table type, or empty if the table doesn't exist or isn't a table
   * @throws IllegalArgumentException if tableName is null
   * @since 1.0.0
   */
  java.util.Optional<TableType> getTableType(final String tableName);

  /**
   * Checks if this module exports a specific item.
   *
   * @param name the name of the export to check
   * @return true if the module exports an item with this name
   * @throws IllegalArgumentException if name is null
   * @since 1.0.0
   */
  boolean hasExport(final String name);

  /**
   * Checks if this module requires a specific import.
   *
   * @param moduleName the module name of the import
   * @param fieldName the field name of the import
   * @return true if the module requires this import
   * @throws IllegalArgumentException if moduleName or fieldName is null
   * @since 1.0.0
   */
  boolean hasImport(final String moduleName, final String fieldName);

  /**
   * Gets the engine that was used to compile this module.
   *
   * @return the Engine used for compilation
   */
  Engine getEngine();

  /**
   * Validates that the provided imports satisfy this module's requirements.
   *
   * @param imports the import definitions to validate
   * @return true if the imports are valid for this module
   * @throws IllegalArgumentException if imports is null
   */
  boolean validateImports(final ImportMap imports);

  /**
   * Gets the name of this module if it has one.
   *
   * @return the module name, or null if unnamed
   */
  String getName();

  /**
   * Checks if the module is still valid and usable.
   *
   * @return true if the module is valid, false otherwise
   */
  boolean isValid();

  /**
   * Closes the module and releases associated resources.
   *
   * <p>After closing, the module becomes invalid and should not be used.
   */
  @Override
  void close();

  /**
   * Compiles WebAssembly bytecode into a Module.
   *
   * @param engine the engine to use for compilation
   * @param wasmBytes the WebAssembly bytecode
   * @return a compiled Module
   * @throws WasmException if compilation fails
   * @throws IllegalArgumentException if engine or wasmBytes is null
   */
  static Module compile(final Engine engine, final byte[] wasmBytes) throws WasmException {
    return WasmRuntimeFactory.create().compileModule(engine, wasmBytes);
  }
}
