package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.FunctionInfo;
import ai.tegmentum.wasmtime4j.type.ExportType;
import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import ai.tegmentum.wasmtime4j.validation.ImportValidation;
import ai.tegmentum.wasmtime4j.validation.ModuleValidationResult;
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
   * Gets a specific export by name.
   *
   * @param name the export name to look up
   * @return the export type if found, or empty if no export with this name exists
   * @throws IllegalArgumentException if name is null
   * @since 1.1.0
   */
  default java.util.Optional<ExportType> getExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }
    return getExports().stream().filter(e -> name.equals(e.getName())).findFirst();
  }

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
   * Validates that the provided imports satisfy this module's requirements with detailed results.
   *
   * <p>This method performs comprehensive type checking for all imports required by the module,
   * including:
   *
   * <ul>
   *   <li>Verification that all required imports are present in the ImportMap
   *   <li>Type checking for globals (comparing GlobalType)
   *   <li>Type checking for tables (comparing TableType)
   *   <li>Type checking for memories (comparing MemoryType)
   *   <li>Type checking for functions (comparing FuncType)
   * </ul>
   *
   * <p>The returned ImportValidation object provides detailed information about any issues found,
   * including missing imports and type mismatches with expected vs. actual type details.
   *
   * @param imports the import definitions to validate
   * @return detailed validation results including issues, statistics, and validation time
   * @throws IllegalArgumentException if imports is null
   * @since 1.0.0
   */
  ImportValidation validateImportsDetailed(final ImportMap imports);

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
        WasmRuntime runtime = engine.getRuntime();
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
   * @return a map of custom section names to their binary data
   */
  java.util.Map<String, byte[]> getCustomSections();

  /**
   * Gets the resources required to instantiate this module.
   *
   * <p>This provides information about the memory, table, and other resources that will be needed
   * when instantiating the module. This can be used for resource planning and validation before
   * attempting instantiation.
   *
   * @return the resources required for instantiation
   * @since 1.1.0
   */
  default ResourcesRequired resourcesRequired() {
    // Default implementation computes from type information
    long minMemory = 0;
    long maxMemory = 0;
    long minTable = 0;
    long maxTable = 0;

    java.util.List<MemoryType> memTypes = getMemoryTypes();
    for (MemoryType mt : memTypes) {
      minMemory += mt.getMinimum() * 65536L;
      java.util.Optional<Long> max = mt.getMaximum();
      if (max.isEmpty()) {
        maxMemory = -1;
      } else if (maxMemory >= 0) {
        maxMemory += max.get() * 65536L;
      }
    }

    java.util.List<TableType> tableTypes = getTableTypes();
    for (TableType tt : tableTypes) {
      minTable += tt.getMinimum();
      java.util.Optional<Long> max = tt.getMaximum();
      if (max.isEmpty()) {
        maxTable = -1;
      } else if (maxTable >= 0) {
        maxTable += max.get();
      }
    }

    return new ResourcesRequired(
        minMemory,
        maxMemory,
        (int) minTable,
        (int) maxTable,
        memTypes.size(),
        tableTypes.size(),
        getGlobalTypes().size(),
        getFunctionTypes().size());
  }

  /**
   * Gets an iterable over all functions in this module.
   *
   * <p>This includes both imported functions and exported functions, providing metadata about each
   * function's index, name, type, and origin. Internal functions that are neither imported nor
   * exported are not included since module-level introspection does not expose them.
   *
   * @return an iterable of function information
   * @since 1.1.0
   */
  default Iterable<FunctionInfo> functions() {
    java.util.List<FunctionInfo> funcs = new java.util.ArrayList<>();
    int index = 0;

    // Add imported functions first
    for (ImportType imp : getImports()) {
      WasmType type = imp.getType();
      if (type != null && type.getKind() == WasmTypeKind.FUNCTION) {
        funcs.add(new FunctionInfo(index++, imp.getName(), (FuncType) type, true));
      }
    }

    // Add exported functions
    for (ExportType exp : getExports()) {
      WasmType type = exp.getType();
      if (type != null && type.getKind() == WasmTypeKind.FUNCTION) {
        funcs.add(new FunctionInfo(index++, exp.getName(), (FuncType) type, false));
      }
    }

    return funcs;
  }

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
    return engine.getRuntime().compileModule(engine, wasmBytes);
  }

  /**
   * Compiles a WebAssembly module from a file on disk.
   *
   * <p>This is a convenience method that reads the file and compiles it. The file can contain
   * either binary WebAssembly (.wasm) or WebAssembly text format (.wat).
   *
   * @param engine the engine to use for compilation
   * @param path the path to the WebAssembly file
   * @return a compiled Module
   * @throws WasmException if compilation fails or file cannot be read
   * @throws IllegalArgumentException if engine or path is null
   * @since 1.1.0
   */
  static Module fromFile(final Engine engine, final java.nio.file.Path path) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("engine cannot be null");
    }
    if (path == null) {
      throw new IllegalArgumentException("path cannot be null");
    }
    return engine.compileFromFile(path);
  }

  /**
   * Checks if this module and another module share the same underlying compiled code.
   *
   * <p>Two modules are considered the "same" if they were cloned from the same original compilation.
   * This is a pointer identity check on the underlying native module allocation.
   *
   * @param other the other module to compare against
   * @return true if both modules share the same underlying compiled code
   * @throws IllegalArgumentException if other is null
   * @since 1.1.0
   */
  boolean same(final Module other);

  /**
   * Gets the index of an export by name.
   *
   * <p>Returns the zero-based index of the export in the module's export list. This index
   * corresponds to the ordering of exports as returned by {@link #getExports()}.
   *
   * @param name the name of the export to find
   * @return the zero-based index, or -1 if no export with this name exists
   * @throws IllegalArgumentException if name is null
   * @since 1.1.0
   */
  int getExportIndex(final String name);

  /**
   * Serializes this compiled module to a byte array for caching or distribution.
   *
   * <p>Serialized modules can be stored to disk, sent over the network, or cached for faster
   * startup times. The serialized data includes the compiled code and all necessary metadata for
   * instantiation.
   *
   * @return the serialized module data
   * @throws WasmException if serialization fails
   * @since 1.0.0
   */
  byte[] serialize() throws WasmException;

  /**
   * Deserializes a module from previously serialized bytes.
   *
   * <p>This method can be used to quickly load a previously compiled module without going through
   * the compilation process again. The bytes must have been created by a compatible version of the
   * same engine.
   *
   * @param engine the engine to use for deserialization
   * @param bytes the serialized module data
   * @return the deserialized Module
   * @throws WasmException if deserialization fails or data is invalid
   * @throws IllegalArgumentException if engine or bytes is null
   * @since 1.0.0
   */
  static Module deserialize(final Engine engine, final byte[] bytes) throws WasmException {
    return engine.getRuntime().deserializeModule(engine, bytes);
  }

  /**
   * Deserializes a module from a file containing serialized module data.
   *
   * @param engine the engine to use for deserialization
   * @param path the path to the serialized module file
   * @return the deserialized Module
   * @throws WasmException if deserialization fails or file cannot be read
   * @throws IllegalArgumentException if engine or path is null
   * @since 1.0.0
   */
  static Module deserializeFile(final Engine engine, final java.nio.file.Path path)
      throws WasmException {
    return engine.getRuntime().deserializeModuleFile(engine, path);
  }

  /**
   * Checks if this module can be serialized.
   *
   * <p>Some modules may not be serializable depending on their compilation settings or the engine
   * configuration.
   *
   * @return true if this module can be serialized
   * @since 1.0.0
   */
  default boolean isSerializable() {
    return true;
  }
}
