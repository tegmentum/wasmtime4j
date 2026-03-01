/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.util.OptionalInt;

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
   * @return the function type, or empty if the function doesn't exist, isn't a function, or name is
   *     null
   * @since 1.0.0
   */
  default java.util.Optional<FuncType> getFunctionType(final String functionName) {
    if (functionName == null) {
      return java.util.Optional.empty();
    }
    for (final ExportType export : getExports()) {
      if (export.getName().equals(functionName)
          && export.getType().getKind() == WasmTypeKind.FUNCTION) {
        return java.util.Optional.of((FuncType) export.getType());
      }
    }
    return java.util.Optional.empty();
  }

  /**
   * Gets the global type for a specific exported global.
   *
   * @param globalName the name of the exported global
   * @return the global type, or empty if the global doesn't exist, isn't a global, or name is null
   * @since 1.0.0
   */
  default java.util.Optional<GlobalType> getGlobalType(final String globalName) {
    if (globalName == null) {
      return java.util.Optional.empty();
    }
    for (final ExportType export : getExports()) {
      if (export.getName().equals(globalName)
          && export.getType().getKind() == WasmTypeKind.GLOBAL) {
        return java.util.Optional.of((GlobalType) export.getType());
      }
    }
    return java.util.Optional.empty();
  }

  /**
   * Gets the memory type for a specific exported memory.
   *
   * @param memoryName the name of the exported memory
   * @return the memory type, or empty if the memory doesn't exist, isn't a memory, or name is null
   * @since 1.0.0
   */
  default java.util.Optional<MemoryType> getMemoryType(final String memoryName) {
    if (memoryName == null) {
      return java.util.Optional.empty();
    }
    for (final ExportType export : getExports()) {
      if (export.getName().equals(memoryName)
          && export.getType().getKind() == WasmTypeKind.MEMORY) {
        return java.util.Optional.of((MemoryType) export.getType());
      }
    }
    return java.util.Optional.empty();
  }

  /**
   * Gets the table type for a specific exported table.
   *
   * @param tableName the name of the exported table
   * @return the table type, or empty if the table doesn't exist, isn't a table, or name is null
   * @since 1.0.0
   */
  default java.util.Optional<TableType> getTableType(final String tableName) {
    if (tableName == null) {
      return java.util.Optional.empty();
    }
    for (final ExportType export : getExports()) {
      if (export.getName().equals(tableName) && export.getType().getKind() == WasmTypeKind.TABLE) {
        return java.util.Optional.of((TableType) export.getType());
      }
    }
    return java.util.Optional.empty();
  }

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
   * including header checks (magic number and version) followed by full structural and semantic
   * validation via the native Wasmtime engine.
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

    // Basic WebAssembly magic number validation (fast fail before native call)
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

    // Full structural and semantic validation via the provided engine's runtime
    try {
      final boolean valid = engine.getRuntime().validateModule(wasmBytes);
      if (valid) {
        return ModuleValidationResult.success();
      }
      return ModuleValidationResult.failure(
          List.of("WebAssembly validation failed (structural or semantic error)"));
    } catch (final Exception e) {
      return ModuleValidationResult.failure(
          List.of("WebAssembly validation error: " + e.getMessage()));
    }
  }

  /**
   * Gets all exported function types from this module.
   *
   * @return an immutable list of function types
   */
  default java.util.List<FuncType> getFunctionTypes() {
    final java.util.List<FuncType> types = new java.util.ArrayList<>();
    for (final ExportType export : getExports()) {
      if (export.getType().getKind() == WasmTypeKind.FUNCTION) {
        types.add((FuncType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(types);
  }

  /**
   * Gets all exported memory types from this module.
   *
   * @return an immutable list of memory types
   */
  default java.util.List<MemoryType> getMemoryTypes() {
    final java.util.List<MemoryType> types = new java.util.ArrayList<>();
    for (final ExportType export : getExports()) {
      if (export.getType().getKind() == WasmTypeKind.MEMORY) {
        types.add((MemoryType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(types);
  }

  /**
   * Gets all exported table types from this module.
   *
   * @return an immutable list of table types
   */
  default java.util.List<TableType> getTableTypes() {
    final java.util.List<TableType> types = new java.util.ArrayList<>();
    for (final ExportType export : getExports()) {
      if (export.getType().getKind() == WasmTypeKind.TABLE) {
        types.add((TableType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(types);
  }

  /**
   * Gets all exported global types from this module.
   *
   * @return an immutable list of global types
   */
  default java.util.List<GlobalType> getGlobalTypes() {
    final java.util.List<GlobalType> types = new java.util.ArrayList<>();
    for (final ExportType export : getExports()) {
      if (export.getType().getKind() == WasmTypeKind.GLOBAL) {
        types.add((GlobalType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(types);
  }

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
   * Pre-initializes this module's copy-on-write image for faster instantiation.
   *
   * <p>When using copy-on-write memory initialization (the default), this method eagerly creates
   * the memory-mapped image used to initialize linear memories during instantiation. Without this
   * call, the image is created lazily on first instantiation.
   *
   * <p>Calling this is beneficial for server-side use cases where the first instantiation latency
   * matters and you want to front-load the cost during module compilation/loading.
   *
   * @throws WasmException if creating the copy-on-write image fails
   * @since 1.1.0
   */
  default void initializeCopyOnWriteImage() throws WasmException {
    // Default no-op; runtime implementations override with native calls
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
   * Gets the compiled machine code text section of this module.
   *
   * <p>Returns a defensive copy of the raw executable code bytes produced by the compiler. This is
   * useful for debugging, disassembly, or performance analysis.
   *
   * @return a copy of the compiled machine code bytes
   * @throws WasmException if the module is no longer valid
   * @since 1.1.0
   */
  byte[] text() throws WasmException;

  /**
   * Gets the address map for this module, mapping compiled code offsets to original WebAssembly
   * bytecode offsets.
   *
   * <p>The address map is useful for debugging and profiling, allowing tools to correlate compiled
   * native code positions back to positions in the original WebAssembly module.
   *
   * <p>Returns an empty list if the engine was configured with {@code generateAddressMap(false)}.
   *
   * @return an immutable list of address mappings from compiled code to wasm bytecode
   * @throws WasmException if the module is no longer valid
   * @since 1.1.0
   */
  List<AddressMapping> addressMap() throws WasmException;

  /**
   * Represents a mapping from a compiled code offset to an original WebAssembly bytecode offset.
   *
   * <p>Each entry maps a position in the compiled machine code (as returned by {@link #text()}) to
   * a position in the original WebAssembly module bytecode.
   *
   * @since 1.1.0
   */
  final class AddressMapping {

    private final long codeOffset;
    private final OptionalInt wasmOffset;

    /**
     * Creates a new address mapping.
     *
     * @param codeOffset the offset in the compiled machine code
     * @param wasmOffset the corresponding offset in the original WebAssembly bytecode, or empty if
     *     the code does not correspond to a specific wasm instruction
     */
    public AddressMapping(final long codeOffset, final OptionalInt wasmOffset) {
      this.codeOffset = codeOffset;
      this.wasmOffset = wasmOffset;
    }

    /**
     * Gets the offset in the compiled machine code.
     *
     * @return the code offset
     */
    public long getCodeOffset() {
      return codeOffset;
    }

    /**
     * Gets the corresponding offset in the original WebAssembly bytecode.
     *
     * @return the wasm bytecode offset, or empty if the code does not correspond to a specific wasm
     *     instruction
     */
    public OptionalInt getWasmOffset() {
      return wasmOffset;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof AddressMapping)) {
        return false;
      }
      AddressMapping other = (AddressMapping) obj;
      return codeOffset == other.codeOffset && wasmOffset.equals(other.wasmOffset);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(codeOffset, wasmOffset);
    }

    @Override
    public String toString() {
      return "AddressMapping{codeOffset="
          + codeOffset
          + ", wasmOffset="
          + (wasmOffset.isPresent() ? wasmOffset.getAsInt() : "none")
          + "}";
    }
  }

  /**
   * Gets the memory address range of the compiled image for this module.
   *
   * <p>This returns the range in the process's virtual address space where the compiled machine
   * code for this module resides. This is useful for tools that need to identify which addresses
   * belong to JIT-compiled WebAssembly code.
   *
   * @return the image range containing start and end addresses
   * @throws WasmException if the module is no longer valid or the operation fails
   * @since 1.1.0
   */
  ImageRange imageRange() throws WasmException;

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
   * Compiles WebAssembly bytecode into a Module with an associated DWARF debug package.
   *
   * <p>The DWARF package ({@code .dwp} file contents) provides additional debug information that is
   * merged into the compiled module. This enables enhanced debugging and profiling capabilities.
   *
   * <p>The WASM bytes are compiled using Wasmtime's {@code CodeBuilder} with the DWARF package
   * attached, then serialized and deserialized internally. The engine must have debug info enabled
   * for the DWARF data to take effect.
   *
   * @param engine the engine to use for compilation
   * @param wasmBytes the WebAssembly bytecode
   * @param dwarfPackage the DWARF debug package bytes ({@code .dwp} file contents)
   * @return a compiled Module with debug information
   * @throws WasmException if compilation fails
   * @throws IllegalArgumentException if any argument is null or empty
   * @since 1.1.0
   */
  static Module compile(final Engine engine, final byte[] wasmBytes, final byte[] dwarfPackage)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("engine cannot be null");
    }
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be null or empty");
    }
    if (dwarfPackage == null || dwarfPackage.length == 0) {
      throw new IllegalArgumentException("dwarfPackage cannot be null or empty");
    }
    return engine.compileModuleWithDwarf(wasmBytes, dwarfPackage);
  }

  /**
   * Compiles a WebAssembly module from binary wasm bytes.
   *
   * <p>Unlike {@link #compile(Engine, byte[])} which auto-detects and accepts both binary wasm and
   * WAT text format, this method only accepts pre-compiled binary wasm bytes. The bytes must begin
   * with the WebAssembly binary magic number ({@code \0asm}).
   *
   * <p>This corresponds to Wasmtime's {@code Module::from_binary()}.
   *
   * @param engine the engine to use for compilation
   * @param wasmBytes the binary WebAssembly bytecode (must not be WAT)
   * @return a compiled Module
   * @throws WasmException if compilation fails
   * @throws IllegalArgumentException if engine or wasmBytes is null, or if the bytes are not valid
   *     binary wasm (e.g. WAT text format)
   * @since 1.1.0
   */
  static Module fromBinary(final Engine engine, final byte[] wasmBytes) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("engine cannot be null");
    }
    if (wasmBytes == null) {
      throw new IllegalArgumentException("wasmBytes cannot be null");
    }
    if (wasmBytes.length < 4) {
      throw new IllegalArgumentException(
          "Binary wasm bytes too short (minimum 4 bytes for magic number)");
    }
    // Validate WebAssembly binary magic number: \0asm (0x00 0x61 0x73 0x6D)
    if (wasmBytes[0] != 0x00
        || wasmBytes[1] != 0x61
        || wasmBytes[2] != 0x73
        || wasmBytes[3] != 0x6D) {
      throw new IllegalArgumentException(
          "Not binary wasm: missing WebAssembly magic number. "
              + "Use Module.compile() for WAT text format.");
    }
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
   * <p>Two modules are considered the "same" if they were cloned from the same original
   * compilation. This is a pointer identity check on the underlying native module allocation.
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
   * Gets a pre-resolved export handle for O(1) export lookups.
   *
   * <p>This returns an opaque {@link ModuleExport} handle that can be passed to {@link
   * Instance#getExport(Store, ModuleExport)} for fast export access without string hashing.
   *
   * @param name the name of the export to resolve
   * @return the module export handle, or empty if no export with this name exists
   * @throws IllegalArgumentException if name is null
   * @since 1.1.0
   */
  java.util.Optional<ModuleExport> getModuleExport(final String name);

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
   * Loads a module from a trusted file, skipping WebAssembly validation.
   *
   * <p>This is faster than normal compilation because validation is skipped, but the file must be
   * from a trusted source. Using an untrusted file can result in undefined behavior.
   *
   * @param engine the engine to use
   * @param path the path to the trusted WebAssembly file
   * @return the compiled Module
   * @throws WasmException if loading fails
   * @throws IllegalArgumentException if engine or path is null
   * @since 1.1.0
   */
  static Module fromTrustedFile(final Engine engine, final java.nio.file.Path path)
      throws WasmException {
    return engine.getRuntime().moduleFromTrustedFile(engine, path);
  }

  /**
   * Deserializes a module from raw bytes without the standard file format wrapper.
   *
   * <p>Unlike {@link #deserialize(Engine, byte[])}, this method expects raw serialized bytes
   * without Wasmtime's file format header. The bytes must be from a compatible Wasmtime version.
   *
   * @param engine the engine to use for deserialization
   * @param bytes the raw serialized module data
   * @return the deserialized Module
   * @throws WasmException if deserialization fails
   * @throws IllegalArgumentException if engine or bytes is null
   * @since 1.1.0
   */
  static Module deserializeRaw(final Engine engine, final byte[] bytes) throws WasmException {
    return engine.getRuntime().deserializeModuleRaw(engine, bytes);
  }

  /**
   * Deserializes a module from an already-open file descriptor.
   *
   * <p>This method is only available on Unix-like platforms (Linux, macOS). It allows
   * deserialization from an already-open file descriptor, which can be useful for sandboxed
   * environments or when the file has been opened with specific permissions.
   *
   * @param engine the engine to use for deserialization
   * @param fd the open file descriptor
   * @return the deserialized Module
   * @throws WasmException if deserialization fails
   * @throws IllegalArgumentException if engine is null
   * @throws UnsupportedOperationException on non-Unix platforms
   * @since 1.1.0
   */
  static Module deserializeOpenFile(final Engine engine, final int fd) throws WasmException {
    return engine.getRuntime().deserializeModuleOpenFile(engine, fd);
  }
}
