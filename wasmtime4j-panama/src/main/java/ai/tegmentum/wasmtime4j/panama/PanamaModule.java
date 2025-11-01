package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExportDescriptor;
import ai.tegmentum.wasmtime4j.ExportType;
import ai.tegmentum.wasmtime4j.FuncType;
import ai.tegmentum.wasmtime4j.GlobalType;
import ai.tegmentum.wasmtime4j.ImportDescriptor;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.ImportType;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.MemoryType;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.ModuleExport;
import ai.tegmentum.wasmtime4j.ModuleImport;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.TableType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Module.
 *
 * @since 1.0.0
 */
public final class PanamaModule implements Module {
  private static final Logger LOGGER = Logger.getLogger(PanamaModule.class.getName());
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final PanamaEngine engine;
  private final Arena arena;
  private final MemorySegment nativeModule;
  private final byte[] wasmBytes;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama module from compiled bytecode.
   *
   * @param engine the engine used for compilation
   * @param wasmBytes the WebAssembly bytecode
   * @throws WasmException if module creation fails
   */
  public PanamaModule(final PanamaEngine engine, final byte[] wasmBytes) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (!engine.isValid()) {
      throw new IllegalStateException("Engine is not valid");
    }
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("WASM bytes cannot be null or empty");
    }

    this.engine = engine;
    this.wasmBytes = wasmBytes.clone();
    this.arena = Arena.ofShared();

    // Allocate native memory for WASM bytes
    final MemorySegment bytesSegment = arena.allocate(wasmBytes.length);
    bytesSegment.copyFrom(MemorySegment.ofArray(wasmBytes));

    // Create native module via Panama FFI
    this.nativeModule =
        NATIVE_BINDINGS.moduleCreate(engine.getNativeEngine(), bytesSegment, wasmBytes.length);

    if (this.nativeModule == null || this.nativeModule.equals(MemorySegment.NULL)) {
      arena.close();
      throw new WasmException("Failed to compile WASM module");
    }

    LOGGER.fine("Created Panama module");
  }

  @Override
  public Instance instantiate(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    ensureNotClosed();
    return new PanamaInstance(this, (PanamaStore) store);
  }

  @Override
  public Instance instantiate(final Store store, final ImportMap imports) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (imports == null) {
      throw new IllegalArgumentException("Imports cannot be null");
    }
    ensureNotClosed();
    // TODO: Implement instantiation with imports
    throw new UnsupportedOperationException("Instantiation with imports not yet implemented");
  }

  @Override
  public List<ExportType> getExports() {
    final List<ai.tegmentum.wasmtime4j.ModuleExport> moduleExports = getModuleExports();
    final List<ExportType> exports = new java.util.ArrayList<>(moduleExports.size());
    for (final ai.tegmentum.wasmtime4j.ModuleExport moduleExport : moduleExports) {
      exports.add(moduleExport.getExportType());
    }
    return java.util.Collections.unmodifiableList(exports);
  }

  @Override
  public List<ImportType> getImports() {
    final List<ai.tegmentum.wasmtime4j.ModuleImport> moduleImports = getModuleImports();
    final List<ImportType> imports = new java.util.ArrayList<>(moduleImports.size());
    for (final ai.tegmentum.wasmtime4j.ModuleImport moduleImport : moduleImports) {
      imports.add(moduleImport.getImportType());
    }
    return java.util.Collections.unmodifiableList(imports);
  }

  @Override
  public List<ExportDescriptor> getExportDescriptors() {
    final List<ai.tegmentum.wasmtime4j.ModuleExport> moduleExports = getModuleExports();
    final List<ai.tegmentum.wasmtime4j.ExportDescriptor> descriptors =
        new java.util.ArrayList<>(moduleExports.size());

    for (final ai.tegmentum.wasmtime4j.ModuleExport moduleExport : moduleExports) {
      final ai.tegmentum.wasmtime4j.ExportType exportType = moduleExport.getExportType();
      descriptors.add(
          new ai.tegmentum.wasmtime4j.panama.type.PanamaExportDescriptor(
              exportType.getName(), exportType.getType()));
    }

    return java.util.Collections.unmodifiableList(descriptors);
  }

  @Override
  public List<ImportDescriptor> getImportDescriptors() {
    final List<ai.tegmentum.wasmtime4j.ModuleImport> moduleImports = getModuleImports();
    final List<ai.tegmentum.wasmtime4j.ImportDescriptor> descriptors =
        new java.util.ArrayList<>(moduleImports.size());

    for (final ai.tegmentum.wasmtime4j.ModuleImport moduleImport : moduleImports) {
      final ai.tegmentum.wasmtime4j.ImportType importType = moduleImport.getImportType();
      descriptors.add(
          new ai.tegmentum.wasmtime4j.panama.type.PanamaImportDescriptor(
              importType.getModuleName(), importType.getName(), importType.getType()));
    }

    return java.util.Collections.unmodifiableList(descriptors);
  }

  @Override
  public Optional<FuncType> getFunctionType(final String functionName) {
    if (functionName == null) {
      return Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(functionName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.FUNCTION) {
        return Optional.of((ai.tegmentum.wasmtime4j.FuncType) export.getType());
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<GlobalType> getGlobalType(final String globalName) {
    if (globalName == null) {
      return Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(globalName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.GLOBAL) {
        return Optional.of((ai.tegmentum.wasmtime4j.GlobalType) export.getType());
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<MemoryType> getMemoryType(final String memoryName) {
    if (memoryName == null) {
      return Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(memoryName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.MEMORY) {
        return Optional.of((ai.tegmentum.wasmtime4j.MemoryType) export.getType());
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<TableType> getTableType(final String tableName) {
    if (tableName == null) {
      return Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(tableName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.TABLE) {
        return Optional.of((ai.tegmentum.wasmtime4j.TableType) export.getType());
      }
    }
    return Optional.empty();
  }

  @Override
  public boolean hasExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();
    final List<ModuleExport> exports = getModuleExports();
    for (final ModuleExport export : exports) {
      if (export.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasImport(final String moduleName, final String fieldName) {
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (fieldName == null) {
      throw new IllegalArgumentException("Field name cannot be null");
    }
    ensureNotClosed();
    final List<ModuleImport> imports = getModuleImports();
    for (final ModuleImport moduleImport : imports) {
      if (moduleImport.getModuleName().equals(moduleName)
          && moduleImport.getFieldName().equals(fieldName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public boolean validateImports(final ImportMap imports) {
    if (imports == null) {
      throw new IllegalArgumentException("Imports cannot be null");
    }
    ensureNotClosed();
    final List<ModuleImport> requiredImports = getModuleImports();
    for (final ModuleImport requiredImport : requiredImports) {
      if (!imports.contains(requiredImport.getModuleName(), requiredImport.getFieldName())) {
        LOGGER.fine(
            String.format(
                "Missing required import: %s.%s",
                requiredImport.getModuleName(), requiredImport.getFieldName()));
        return false;
      }
    }
    return true;
  }

  @Override
  public List<ModuleImport> getModuleImports() {
    ensureNotClosed();

    final long importCount = NATIVE_BINDINGS.moduleImportsLen(nativeModule);
    if (importCount == 0) {
      return Collections.emptyList();
    }

    // Get imports as JSON from native code
    final MemorySegment jsonPtr = NATIVE_BINDINGS.moduleGetImportsJson(nativeModule);
    if (jsonPtr == null || jsonPtr.equals(MemorySegment.NULL)) {
      LOGGER.warning("Failed to retrieve module imports");
      return Collections.emptyList();
    }

    try {
      // Convert C string to Java String
      final String jsonString = jsonPtr.reinterpret(Long.MAX_VALUE).getString(0);

      // Parse JSON and create ModuleImport objects
      final List<ModuleImport> imports = parseImportsJson(jsonString);

      return Collections.unmodifiableList(imports);
    } finally {
      // Free the native string
      NATIVE_BINDINGS.moduleFreeString(jsonPtr);
    }
  }

  @Override
  public List<ModuleExport> getModuleExports() {
    ensureNotClosed();

    final long exportCount = NATIVE_BINDINGS.moduleExportsLen(nativeModule);
    if (exportCount == 0) {
      return Collections.emptyList();
    }

    // Get exports as JSON from native code
    final MemorySegment jsonPtr = NATIVE_BINDINGS.moduleGetExportsJson(nativeModule);
    if (jsonPtr == null || jsonPtr.equals(MemorySegment.NULL)) {
      LOGGER.warning("Failed to retrieve module exports");
      return Collections.emptyList();
    }

    try {
      // Convert C string to Java String
      final String jsonString = jsonPtr.reinterpret(Long.MAX_VALUE).getString(0);

      // Parse JSON and create ModuleExport objects
      final List<ModuleExport> exports = parseExportsJson(jsonString);

      return Collections.unmodifiableList(exports);
    } finally {
      // Free the native string
      NATIVE_BINDINGS.moduleFreeString(jsonPtr);
    }
  }

  @Override
  public List<FuncType> getFunctionTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.FuncType> functionTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.FUNCTION) {
        functionTypes.add((ai.tegmentum.wasmtime4j.FuncType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(functionTypes);
  }

  @Override
  public List<MemoryType> getMemoryTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.MemoryType> memoryTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.MEMORY) {
        memoryTypes.add((ai.tegmentum.wasmtime4j.MemoryType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(memoryTypes);
  }

  @Override
  public List<TableType> getTableTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.TableType> tableTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.TABLE) {
        tableTypes.add((ai.tegmentum.wasmtime4j.TableType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(tableTypes);
  }

  @Override
  public List<GlobalType> getGlobalTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.GlobalType> globalTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.WasmTypeKind.GLOBAL) {
        globalTypes.add((ai.tegmentum.wasmtime4j.GlobalType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(globalTypes);
  }

  @Override
  public Map<String, String> getCustomSections() {
    ensureNotClosed();
    // TODO: Implement custom sections extraction
    return Collections.emptyMap();
  }

  @Override
  public String getName() {
    ensureNotClosed();
    // TODO: Implement module name extraction
    return null;
  }

  @Override
  public boolean isValid() {
    return !closed;
  }

  @Override
  public byte[] serialize() throws WasmException {
    ensureNotClosed();
    // TODO: Implement module serialization
    throw new UnsupportedOperationException("Serialization not yet implemented");
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    try {
      // Destroy native module
      if (nativeModule != null && !nativeModule.equals(MemorySegment.NULL)) {
        NATIVE_BINDINGS.moduleDestroy(nativeModule);
      }
      arena.close();
      closed = true;
      LOGGER.fine("Closed Panama module");
    } catch (final Exception e) {
      LOGGER.warning("Error closing module: " + e.getMessage());
    }
  }

  /**
   * Gets the native module pointer.
   *
   * @return native module memory segment
   */
  public MemorySegment getNativeModule() {
    return nativeModule;
  }

  /**
   * Gets the original WebAssembly bytecode.
   *
   * @return a copy of the WebAssembly bytecode
   */
  public byte[] getWasmBytes() {
    return wasmBytes.clone();
  }

  /**
   * Parses JSON string containing module imports.
   *
   * @param jsonString JSON string from native code
   * @return list of ModuleImport objects
   */
  private List<ModuleImport> parseImportsJson(final String jsonString) {
    final com.google.gson.Gson gson = new com.google.gson.Gson();
    final com.google.gson.JsonArray jsonArray = gson.fromJson(jsonString, com.google.gson.JsonArray.class);

    final List<ModuleImport> imports = new java.util.ArrayList<>();

    for (final com.google.gson.JsonElement element : jsonArray) {
      final com.google.gson.JsonObject importObj = element.getAsJsonObject();
      final String moduleName = importObj.get("module").getAsString();
      final String fieldName = importObj.get("name").getAsString();
      final com.google.gson.JsonObject importTypeObj = importObj.getAsJsonObject("import_type");

      final ai.tegmentum.wasmtime4j.WasmType wasmType = parseImportTypeJson(importTypeObj);
      final ai.tegmentum.wasmtime4j.ImportType importType =
          new ai.tegmentum.wasmtime4j.ImportType(moduleName, fieldName, wasmType);
      imports.add(new ModuleImport(moduleName, fieldName, importType));
    }

    return imports;
  }

  /**
   * Parses JSON string containing module exports.
   *
   * @param jsonString JSON string from native code
   * @return list of ModuleExport objects
   */
  private List<ModuleExport> parseExportsJson(final String jsonString) {
    final com.google.gson.Gson gson = new com.google.gson.Gson();
    final com.google.gson.JsonArray jsonArray = gson.fromJson(jsonString, com.google.gson.JsonArray.class);

    final List<ModuleExport> exports = new java.util.ArrayList<>();

    for (final com.google.gson.JsonElement element : jsonArray) {
      final com.google.gson.JsonObject exportObj = element.getAsJsonObject();
      final String name = exportObj.get("name").getAsString();
      final com.google.gson.JsonObject exportTypeObj = exportObj.getAsJsonObject("export_type");

      final ai.tegmentum.wasmtime4j.WasmType wasmType = parseExportTypeJson(exportTypeObj);
      final ai.tegmentum.wasmtime4j.ExportType exportType =
          new ai.tegmentum.wasmtime4j.ExportType(name, wasmType);
      exports.add(new ModuleExport(name, exportType));
    }

    return exports;
  }

  /**
   * Parses import type JSON object to WasmType.
   *
   * @param typeObj JSON object containing type information
   * @return WasmType instance
   */
  private ai.tegmentum.wasmtime4j.WasmType parseImportTypeJson(
      final com.google.gson.JsonObject typeObj) {
    // The type object will have one key indicating the variant
    final String typeKind = typeObj.keySet().iterator().next();

    return switch (typeKind) {
      case "Function" -> parseFunctionType(typeObj.getAsJsonObject("Function"));
      case "Global" -> parseGlobalType(typeObj.getAsJsonArray("Global"));
      case "Memory" -> parseMemoryType(typeObj.getAsJsonArray("Memory"));
      case "Table" -> parseTableType(typeObj.getAsJsonArray("Table"));
      default -> throw new IllegalArgumentException("Unknown import type: " + typeKind);
    };
  }

  /**
   * Parses export type JSON object to WasmType.
   *
   * @param typeObj JSON object containing type information
   * @return WasmType instance
   */
  private ai.tegmentum.wasmtime4j.WasmType parseExportTypeJson(
      final com.google.gson.JsonObject typeObj) {
    // The type object will have one key indicating the variant
    final String typeKind = typeObj.keySet().iterator().next();

    return switch (typeKind) {
      case "Function" -> parseFunctionType(typeObj.getAsJsonObject("Function"));
      case "Global" -> parseGlobalType(typeObj.getAsJsonArray("Global"));
      case "Memory" -> parseMemoryType(typeObj.getAsJsonArray("Memory"));
      case "Table" -> parseTableType(typeObj.getAsJsonArray("Table"));
      default -> throw new IllegalArgumentException("Unknown export type: " + typeKind);
    };
  }

  /**
   * Parses function type JSON.
   *
   * @param funcObj JSON object with params and returns arrays
   * @return FuncType instance
   */
  private ai.tegmentum.wasmtime4j.FuncType parseFunctionType(
      final com.google.gson.JsonObject funcObj) {
    final com.google.gson.JsonArray paramsArray = funcObj.getAsJsonArray("params");
    final com.google.gson.JsonArray returnsArray = funcObj.getAsJsonArray("returns");

    final List<ai.tegmentum.wasmtime4j.WasmValueType> params =
        parseValueTypeArray(paramsArray);
    final List<ai.tegmentum.wasmtime4j.WasmValueType> results =
        parseValueTypeArray(returnsArray);

    return new ai.tegmentum.wasmtime4j.panama.type.PanamaFuncType(
        params, results, arena, MemorySegment.NULL);
  }

  /**
   * Parses global type JSON.
   *
   * @param globalArray JSON array [valueType, isMutable]
   * @return GlobalType instance
   */
  private ai.tegmentum.wasmtime4j.GlobalType parseGlobalType(
      final com.google.gson.JsonArray globalArray) {
    final String valueTypeStr = globalArray.get(0).getAsString();
    final boolean isMutable = globalArray.get(1).getAsBoolean();

    final ai.tegmentum.wasmtime4j.WasmValueType valueType = parseValueType(valueTypeStr);

    return new ai.tegmentum.wasmtime4j.panama.type.PanamaGlobalType(
        valueType, isMutable, arena, MemorySegment.NULL);
  }

  /**
   * Parses memory type JSON.
   *
   * @param memoryArray JSON array [min, max(optional), isShared]
   * @return MemoryType instance
   */
  private ai.tegmentum.wasmtime4j.MemoryType parseMemoryType(
      final com.google.gson.JsonArray memoryArray) {
    final long minimum = memoryArray.get(0).getAsLong();
    final com.google.gson.JsonElement maxElement = memoryArray.get(1);
    final Long maximum = maxElement.isJsonNull() ? null : maxElement.getAsLong();
    final boolean isShared = memoryArray.get(2).getAsBoolean();

    return new ai.tegmentum.wasmtime4j.panama.type.PanamaMemoryType(
        minimum, maximum, false, isShared, arena, MemorySegment.NULL);
  }

  /**
   * Parses table type JSON.
   *
   * @param tableArray JSON array [elementType, min, max(optional)]
   * @return TableType instance
   */
  private ai.tegmentum.wasmtime4j.TableType parseTableType(
      final com.google.gson.JsonArray tableArray) {
    final String elementTypeStr = tableArray.get(0).getAsString();
    final long minimum = tableArray.get(1).getAsLong();
    final com.google.gson.JsonElement maxElement = tableArray.get(2);
    final Long maximum = maxElement.isJsonNull() ? null : maxElement.getAsLong();

    final ai.tegmentum.wasmtime4j.WasmValueType elementType = parseValueType(elementTypeStr);

    return new ai.tegmentum.wasmtime4j.panama.type.PanamaTableType(
        elementType, minimum, maximum, arena, MemorySegment.NULL);
  }

  /**
   * Parses array of value types.
   *
   * @param array JSON array of value type strings
   * @return list of WasmValueType
   */
  private List<ai.tegmentum.wasmtime4j.WasmValueType> parseValueTypeArray(
      final com.google.gson.JsonArray array) {
    final List<ai.tegmentum.wasmtime4j.WasmValueType> types = new java.util.ArrayList<>();
    for (final com.google.gson.JsonElement element : array) {
      types.add(parseValueType(element.getAsString()));
    }
    return types;
  }

  /**
   * Parses value type string to enum.
   *
   * @param typeStr value type string from Rust (e.g., "I32", "FuncRef")
   * @return WasmValueType enum value
   */
  private ai.tegmentum.wasmtime4j.WasmValueType parseValueType(final String typeStr) {
    return switch (typeStr) {
      case "I32" -> ai.tegmentum.wasmtime4j.WasmValueType.I32;
      case "I64" -> ai.tegmentum.wasmtime4j.WasmValueType.I64;
      case "F32" -> ai.tegmentum.wasmtime4j.WasmValueType.F32;
      case "F64" -> ai.tegmentum.wasmtime4j.WasmValueType.F64;
      case "V128" -> ai.tegmentum.wasmtime4j.WasmValueType.V128;
      case "FuncRef" -> ai.tegmentum.wasmtime4j.WasmValueType.FUNCREF;
      case "ExternRef" -> ai.tegmentum.wasmtime4j.WasmValueType.EXTERNREF;
      default -> throw new IllegalArgumentException("Unknown value type: " + typeStr);
    };
  }

  /**
   * Ensures the module is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Module has been closed");
    }
  }
}
