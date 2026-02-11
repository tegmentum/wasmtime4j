package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.memory.Memory;

import ai.tegmentum.wasmtime4j.func.FunctionInfo;

import ai.tegmentum.wasmtime4j.type.WasmTypeKind;

import ai.tegmentum.wasmtime4j.type.WasmType;

import ai.tegmentum.wasmtime4j.type.TableType;

import ai.tegmentum.wasmtime4j.type.MemoryType;

import ai.tegmentum.wasmtime4j.type.ImportType;

import ai.tegmentum.wasmtime4j.type.GlobalType;

import ai.tegmentum.wasmtime4j.type.FuncType;

import ai.tegmentum.wasmtime4j.type.ExportType;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.Closeable;
import java.util.List;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import ai.tegmentum.wasmtime4j.validation.ImportValidation;
import ai.tegmentum.wasmtime4j.validation.ModuleValidationResult;

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
   * <p>This includes both imported functions and locally defined functions, providing metadata
   * about each function's index, name, type, and origin.
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
        java.util.Optional<FuncType> ft = getFunctionType(imp.getName());
        funcs.add(new FunctionInfo(index++, imp.getName(), ft.orElse(null), true));
      }
    }

    // Add exported functions
    for (ExportType exp : getExports()) {
      WasmType type = exp.getType();
      if (type != null && type.getKind() == WasmTypeKind.FUNCTION) {
        java.util.Optional<FuncType> ft = getFunctionType(exp.getName());
        funcs.add(new FunctionInfo(index++, exp.getName(), ft.orElse(null), false));
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
    return WasmRuntimeFactory.create().compileModule(engine, wasmBytes);
  }

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
    return WasmRuntimeFactory.create().deserializeModule(engine, bytes);
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
    return WasmRuntimeFactory.create().deserializeModuleFile(engine, path);
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

  // ===== Low-level/Unsafe Methods =====

  /**
   * Deserializes a module from a memory range without copying data.
   *
   * <p>This is a low-level method that deserializes directly from a memory range, avoiding data
   * copies. The provided memory must remain valid for the lifetime of the returned module.
   *
   * <p><b>Warning:</b> This method is unsafe because:
   *
   * <ul>
   *   <li>The caller must ensure the memory remains valid
   *   <li>The memory must contain valid serialized module data
   *   <li>Incorrect usage may cause undefined behavior or crashes
   * </ul>
   *
   * @param engine the engine to use for deserialization
   * @param address the starting memory address of the serialized data
   * @param length the length of the serialized data in bytes
   * @return the deserialized Module
   * @throws WasmException if deserialization fails
   * @throws IllegalArgumentException if engine is null or address/length are invalid
   * @since 1.1.0
   */
  static Module deserializeRaw(final Engine engine, final long address, final long length)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (address < 0) {
      throw new IllegalArgumentException("Address cannot be negative");
    }
    if (length <= 0) {
      throw new IllegalArgumentException("Length must be positive");
    }
    // Default implementation: read from address into byte array and use normal deserialize
    // Subclasses can provide more efficient implementations using native memory
    throw new UnsupportedOperationException("deserializeRaw not supported in this implementation");
  }

  /**
   * Gets the memory range of the compiled code for this module.
   *
   * <p>This method returns the start address and length of the compiled code region for this
   * module. This is useful for advanced use cases like:
   *
   * <ul>
   *   <li>Memory mapping optimizations
   *   <li>Custom caching implementations
   *   <li>Integration with native profilers
   * </ul>
   *
   * <p><b>Note:</b> The returned range is only valid while the module is alive.
   *
   * @return a ModuleImageRange containing the start address and length, or empty if unavailable
   * @since 1.1.0
   */
  default java.util.Optional<ModuleImageRange> imageRange() {
    return java.util.Optional.empty();
  }

  /**
   * Gets access to the compiled module data.
   *
   * <p>CompiledModule provides low-level access to the compiled native code and metadata for
   * advanced use cases such as:
   *
   * <ul>
   *   <li>Module caching and serialization
   *   <li>Code analysis and inspection
   *   <li>Custom loading and memory management
   *   <li>Debugging and profiling
   * </ul>
   *
   * @return an Optional containing the CompiledModule, or empty if not available
   * @since 1.1.0
   */
  default java.util.Optional<CompiledModule> getCompiledModule() {
    return java.util.Optional.empty();
  }

  /**
   * Represents a memory range for module compiled code.
   *
   * @since 1.1.0
   */
  final class ModuleImageRange {
    private final long startAddress;
    private final long length;

    /**
     * Creates a new module image range.
     *
     * @param startAddress the starting memory address
     * @param length the length in bytes
     */
    public ModuleImageRange(final long startAddress, final long length) {
      this.startAddress = startAddress;
      this.length = length;
    }

    /**
     * Gets the starting memory address.
     *
     * @return the start address
     */
    public long getStartAddress() {
      return startAddress;
    }

    /**
     * Gets the length of the image in bytes.
     *
     * @return the length
     */
    public long getLength() {
      return length;
    }

    /**
     * Gets the end address (startAddress + length).
     *
     * @return the end address
     */
    public long getEndAddress() {
      return startAddress + length;
    }
  }
}
