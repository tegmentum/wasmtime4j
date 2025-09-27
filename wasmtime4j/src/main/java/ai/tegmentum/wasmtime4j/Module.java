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
   * Validates WebAssembly bytecode and returns detailed validation results.
   *
   * <p>This method performs comprehensive validation of WebAssembly bytecode without compiling it,
   * providing detailed error and warning information.
   *
   * @param engine the engine to use for validation
   * @param wasmBytes the WebAssembly bytecode to validate
   * @return detailed validation results
   * @throws IllegalArgumentException if engine or wasmBytes is null
   */
  static ModuleValidationResult validate(final Engine engine, final byte[] wasmBytes) {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (wasmBytes == null) {
      throw new IllegalArgumentException("WebAssembly bytes cannot be null");
    }
    if (wasmBytes.length == 0) {
      return ModuleValidationResult.failure(List.of("WebAssembly bytecode cannot be empty"));
    }

    try {
      // Basic WebAssembly magic number validation
      if (wasmBytes.length < 8) {
        return ModuleValidationResult.failure(
            List.of("WebAssembly bytecode too short (minimum 8 bytes required)"));
      }

      // Check WebAssembly magic number (0x00 0x61 0x73 0x6D)
      if (wasmBytes[0] != 0x00
          || wasmBytes[1] != 0x61
          || wasmBytes[2] != 0x73
          || wasmBytes[3] != 0x6D) {
        return ModuleValidationResult.failure(List.of("Invalid WebAssembly magic number"));
      }

      // Check WebAssembly version (0x01 0x00 0x00 0x00 for version 1)
      if (wasmBytes[4] != 0x01
          || wasmBytes[5] != 0x00
          || wasmBytes[6] != 0x00
          || wasmBytes[7] != 0x00) {
        return ModuleValidationResult.failure(List.of("Unsupported WebAssembly version"));
      }

      // Basic structural validation - try to compile with engine
      // This delegates to the actual engine implementation for deeper validation
      try {
        WasmRuntime runtime = WasmRuntimeFactory.create();
        Module testModule = runtime.compileModule(engine, wasmBytes);
        testModule.close();
        return ModuleValidationResult.success();
      } catch (Exception e) {
        // If compilation fails, return validation failure with the error message
        return ModuleValidationResult.failure(
            List.of("Compilation validation failed: " + e.getMessage()));
      }

    } catch (Exception e) {
      return ModuleValidationResult.failure(List.of("Validation error: " + e.getMessage()));
    }
  }

  /**
   * Gets the imports required by this module as ModuleImport objects.
   *
   * <p>This method provides enhanced import information compared to {@link #getImports()},
   * including complete type details for each import.
   *
   * @return an immutable list of module imports with complete type information
   */
  java.util.List<ModuleImport> getModuleImports();

  /**
   * Gets the exports defined by this module as ModuleExport objects.
   *
   * <p>This method provides enhanced export information compared to {@link #getExports()},
   * including complete type details for each export.
   *
   * @return an immutable list of module exports with complete type information
   */
  java.util.List<ModuleExport> getModuleExports();

  /**
   * Gets all function types defined in this module.
   *
   * <p>This includes function types for both imported and exported functions, as well as internal
   * functions.
   *
   * @return an immutable list of function types
   */
  java.util.List<FuncType> getFunctionTypes();

  /**
   * Gets all memory types defined in this module.
   *
   * <p>This includes memory types for both imported and exported memories.
   *
   * @return an immutable list of memory types
   */
  java.util.List<MemoryType> getMemoryTypes();

  /**
   * Gets all table types defined in this module.
   *
   * <p>This includes table types for both imported and exported tables.
   *
   * @return an immutable list of table types
   */
  java.util.List<TableType> getTableTypes();

  /**
   * Gets all global types defined in this module.
   *
   * <p>This includes global types for both imported and exported globals.
   *
   * @return an immutable list of global types
   */
  java.util.List<GlobalType> getGlobalTypes();

  /**
   * Gets custom sections from this module.
   *
   * <p>Custom sections contain arbitrary data that can be embedded in WebAssembly modules for
   * metadata or debugging purposes.
   *
   * @return a map of custom section names to their data
   */
  java.util.Map<String, String> getCustomSections();

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
   * Gets the number of import definitions in this module.
   *
   * <p>Returns the total count of all imports (functions, globals, memories, tables) required by
   * this module.
   *
   * @return the number of imports
   * @since 1.0.0
   */
  int getImportCount();

  /**
   * Gets the number of export definitions in this module.
   *
   * <p>Returns the total count of all exports (functions, globals, memories, tables) provided by
   * this module.
   *
   * @return the number of exports
   * @since 1.0.0
   */
  int getExportCount();

  /**
   * Gets the number of function exports in this module.
   *
   * <p>Returns the count of exported functions only, excluding other export types.
   *
   * @return the number of function exports
   * @since 1.0.0
   */
  int getFunctionExportCount();

  /**
   * Gets the number of memory exports in this module.
   *
   * <p>Returns the count of exported memories only, excluding other export types.
   *
   * @return the number of memory exports
   * @since 1.0.0
   */
  int getMemoryExportCount();

  /**
   * Gets the number of table exports in this module.
   *
   * <p>Returns the count of exported tables only, excluding other export types.
   *
   * @return the number of table exports
   * @since 1.0.0
   */
  int getTableExportCount();

  /**
   * Gets the number of global exports in this module.
   *
   * <p>Returns the count of exported globals only, excluding other export types.
   *
   * @return the number of global exports
   * @since 1.0.0
   */
  int getGlobalExportCount();

  /**
   * Gets the size of this compiled module in bytes.
   *
   * <p>Returns the total size of the compiled module data, which may be different from the original
   * WebAssembly bytecode size due to compilation optimizations and metadata.
   *
   * @return the module size in bytes
   * @since 1.0.0
   */
  long getSizeBytes();

  /**
   * Serializes this compiled module to bytes for caching or distribution.
   *
   * <p>The serialized data contains the compiled module in a format that can be stored to disk,
   * transmitted over network, or cached for later deserialization. The exact format is
   * implementation-specific and tied to the engine configuration.
   *
   * <p>Serialized modules can only be deserialized with an engine that has the same configuration
   * as the original engine used for compilation.
   *
   * @return the serialized module data
   * @throws WasmException if serialization fails
   * @throws UnsupportedOperationException if the module cannot be serialized
   * @since 1.0.0
   */
  byte[] serialize() throws WasmException;

  /**
   * Checks if this module can be serialized.
   *
   * <p>Some modules may not support serialization due to engine configuration or module
   * characteristics.
   *
   * @return true if the module can be serialized, false otherwise
   * @since 1.0.0
   */
  boolean isSerializable();

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

  /**
   * Compiles WebAssembly Text (WAT) format into a Module.
   *
   * <p>This method accepts WebAssembly modules in text format and compiles them to executable
   * bytecode. WAT compilation includes parsing, validation, and compilation to the same optimized
   * form as binary WebAssembly.
   *
   * @param engine the engine to use for compilation
   * @param watText the WebAssembly text format source
   * @return a compiled Module
   * @throws WasmException if compilation fails due to syntax errors or invalid WAT
   * @throws IllegalArgumentException if engine or watText is null
   * @since 1.0.0
   */
  static Module compileWat(final Engine engine, final String watText) throws WasmException {
    return WasmRuntimeFactory.create().compileModuleWat(engine, watText);
  }

  /**
   * Deserializes a previously serialized Module using the provided engine.
   *
   * <p>The serialized data must have been created by a module compiled with an engine that has the
   * same configuration as the provided engine. Deserialization is typically much faster than
   * compilation from WebAssembly bytecode.
   *
   * <p>This method enables efficient caching and distribution of compiled modules, allowing
   * applications to avoid recompilation overhead when loading previously compiled modules.
   *
   * @param engine the engine to use for deserialization (must match original engine config)
   * @param serializedBytes the serialized module data
   * @return a deserialized Module ready for instantiation
   * @throws WasmException if deserialization fails
   * @throws IllegalArgumentException if engine or serializedBytes is null
   * @since 1.0.0
   */
  static Module deserialize(final Engine engine, final byte[] serializedBytes)
      throws WasmException {
    return WasmRuntimeFactory.create().deserializeModule(engine, serializedBytes);
  }
}
