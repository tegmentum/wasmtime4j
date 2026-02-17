package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.ModuleExport;
import ai.tegmentum.wasmtime4j.ModuleImport;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.type.ExportType;
import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
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
  private static final NativeEngineBindings NATIVE_BINDINGS = NativeEngineBindings.getInstance();

  private final PanamaEngine engine;
  private final Arena arena;
  private final MemorySegment nativeModule;
  private final byte[] wasmBytes;
  private final NativeResourceHandle resourceHandle;

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

    final MemorySegment moduleHandle = this.nativeModule;
    final Arena moduleArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaModule",
            () -> {
              if (nativeModule != null && !nativeModule.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.moduleDestroy(nativeModule);
              }
              arena.close();
            },
            this,
            () -> {
              if (moduleHandle != null && !moduleHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.moduleDestroy(moduleHandle);
              }
              moduleArena.close();
            });

    LOGGER.fine("Created Panama module");
  }

  /**
   * Creates a new PanamaModule from an existing native module pointer. Package-private constructor
   * for use by PanamaEngine.compileWat().
   *
   * @param engine the engine to use
   * @param nativeModulePtr the native module pointer
   * @throws WasmException if module is invalid
   */
  PanamaModule(final PanamaEngine engine, final MemorySegment nativeModulePtr)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (!engine.isValid()) {
      throw new IllegalStateException("Engine is not valid");
    }
    if (nativeModulePtr == null || nativeModulePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Native module pointer is null");
    }

    this.engine = engine;
    this.wasmBytes = null; // WAT modules don't have original bytes
    this.arena = Arena.ofShared();
    this.nativeModule = nativeModulePtr;

    final MemorySegment moduleHandle = this.nativeModule;
    final Arena moduleArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaModule",
            () -> {
              if (nativeModule != null && !nativeModule.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.moduleDestroy(nativeModule);
              }
              arena.close();
            },
            this,
            () -> {
              if (moduleHandle != null && !moduleHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.moduleDestroy(moduleHandle);
              }
              moduleArena.close();
            });

    LOGGER.fine("Created Panama module from native pointer");
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
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore instance");
    }
    ensureNotClosed();

    final PanamaStore panamaStore = (PanamaStore) store;
    // For now, use simple instantiation - proper ImportMap support can be added later
    // This matches the JNI implementation which also doesn't fully utilize ImportMap yet
    return new PanamaInstance(this, panamaStore);
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
  public Optional<FuncType> getFunctionType(final String functionName) {
    if (functionName == null) {
      return Optional.empty();
    }
    final List<ExportType> exports = getExports();
    for (final ExportType export : exports) {
      if (export.getName().equals(functionName)
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.FUNCTION) {
        return Optional.of((ai.tegmentum.wasmtime4j.type.FuncType) export.getType());
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
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.GLOBAL) {
        return Optional.of((ai.tegmentum.wasmtime4j.type.GlobalType) export.getType());
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
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.MEMORY) {
        return Optional.of((ai.tegmentum.wasmtime4j.type.MemoryType) export.getType());
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
          && export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.TABLE) {
        return Optional.of((ai.tegmentum.wasmtime4j.type.TableType) export.getType());
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
  public ai.tegmentum.wasmtime4j.validation.ImportValidation validateImportsDetailed(
      final ai.tegmentum.wasmtime4j.validation.ImportMap imports) {
    if (imports == null) {
      throw new IllegalArgumentException("imports cannot be null");
    }
    ensureNotClosed();

    final long startTime = System.nanoTime();
    final java.util.List<ai.tegmentum.wasmtime4j.validation.ImportIssue> issues =
        new java.util.ArrayList<>();
    final java.util.List<ai.tegmentum.wasmtime4j.validation.ImportInfo> validatedImports =
        new java.util.ArrayList<>();
    final java.util.Map<String, java.util.Map<String, Object>> importsMap = imports.getImports();

    // Get all module imports and validate each one
    final List<ai.tegmentum.wasmtime4j.ModuleImport> moduleImports = getModuleImports();
    int validCount = 0;

    for (final ai.tegmentum.wasmtime4j.ModuleImport moduleImport : moduleImports) {
      final ai.tegmentum.wasmtime4j.type.ImportType importType = moduleImport.getImportType();
      final String moduleName = importType.getModuleName();
      final String fieldName = importType.getName();
      final ai.tegmentum.wasmtime4j.type.WasmType expectedType = importType.getType();

      // Check if import exists
      if (!imports.contains(moduleName, fieldName)) {
        issues.add(
            new ai.tegmentum.wasmtime4j.validation.ImportIssue(
                ai.tegmentum.wasmtime4j.validation.ImportIssue.Severity.ERROR,
                ai.tegmentum.wasmtime4j.validation.ImportIssue.Type.MISSING_IMPORT,
                moduleName,
                fieldName,
                "Required import is missing from ImportMap"));
        continue;
      }

      // Get actual import object and validate type
      final java.util.Map<String, Object> moduleMap = importsMap.get(moduleName);
      if (moduleMap == null) {
        issues.add(
            new ai.tegmentum.wasmtime4j.validation.ImportIssue(
                ai.tegmentum.wasmtime4j.validation.ImportIssue.Severity.ERROR,
                ai.tegmentum.wasmtime4j.validation.ImportIssue.Type.MODULE_NOT_FOUND,
                moduleName,
                fieldName,
                "Module not found in ImportMap"));
        continue;
      }

      final Object actualImport = moduleMap.get(fieldName);
      if (actualImport == null) {
        issues.add(
            new ai.tegmentum.wasmtime4j.validation.ImportIssue(
                ai.tegmentum.wasmtime4j.validation.ImportIssue.Severity.ERROR,
                ai.tegmentum.wasmtime4j.validation.ImportIssue.Type.EXPORT_NOT_FOUND,
                moduleName,
                fieldName,
                "Import field not found in module"));
        continue;
      }

      // Type check based on expected type kind
      final ai.tegmentum.wasmtime4j.type.WasmTypeKind expectedKind = expectedType.getKind();
      LOGGER.fine(
          "Checking "
              + moduleName
              + "."
              + fieldName
              + ": expectedKind="
              + expectedKind
              + ", actualImport="
              + actualImport.getClass().getName());
      boolean typeMatches = true;
      String expectedTypeStr = expectedKind.toString();
      String actualTypeStr = actualImport.getClass().getSimpleName();

      switch (expectedKind) {
        case GLOBAL:
          LOGGER.fine(
              "GLOBAL case: actualImport instanceof WasmGlobal = "
                  + (actualImport instanceof ai.tegmentum.wasmtime4j.WasmGlobal));
          if (actualImport instanceof ai.tegmentum.wasmtime4j.WasmGlobal) {
            final ai.tegmentum.wasmtime4j.WasmGlobal global =
                (ai.tegmentum.wasmtime4j.WasmGlobal) actualImport;
            final ai.tegmentum.wasmtime4j.type.GlobalType actualGlobalType = global.getGlobalType();
            final ai.tegmentum.wasmtime4j.type.GlobalType expectedGlobalType =
                (ai.tegmentum.wasmtime4j.type.GlobalType) expectedType;
            LOGGER.fine(
                "expected: "
                    + formatGlobalType(expectedGlobalType)
                    + ", actual: "
                    + formatGlobalType(actualGlobalType));

            if (!typesMatch(expectedGlobalType, actualGlobalType)) {
              typeMatches = false;
              expectedTypeStr = formatGlobalType(expectedGlobalType);
              actualTypeStr = formatGlobalType(actualGlobalType);
            }
          } else {
            typeMatches = false;
          }
          break;

        case TABLE:
          LOGGER.fine(
              "TABLE case: actualImport instanceof WasmTable = "
                  + (actualImport instanceof ai.tegmentum.wasmtime4j.WasmTable));
          if (actualImport instanceof ai.tegmentum.wasmtime4j.WasmTable) {
            final ai.tegmentum.wasmtime4j.WasmTable table =
                (ai.tegmentum.wasmtime4j.WasmTable) actualImport;
            final ai.tegmentum.wasmtime4j.type.TableType actualTableType = table.getTableType();
            final ai.tegmentum.wasmtime4j.type.TableType expectedTableType =
                (ai.tegmentum.wasmtime4j.type.TableType) expectedType;
            LOGGER.fine(
                "expected: "
                    + formatTableType(expectedTableType)
                    + ", actual: "
                    + formatTableType(actualTableType));

            if (!typesMatch(expectedTableType, actualTableType)) {
              typeMatches = false;
              expectedTypeStr = formatTableType(expectedTableType);
              actualTypeStr = formatTableType(actualTableType);
            }
          } else {
            typeMatches = false;
          }
          break;

        case MEMORY:
          LOGGER.fine(
              "MEMORY case: actualImport instanceof WasmMemory = "
                  + (actualImport instanceof ai.tegmentum.wasmtime4j.WasmMemory));
          if (actualImport instanceof ai.tegmentum.wasmtime4j.WasmMemory) {
            final ai.tegmentum.wasmtime4j.WasmMemory memory =
                (ai.tegmentum.wasmtime4j.WasmMemory) actualImport;
            final ai.tegmentum.wasmtime4j.type.MemoryType actualMemoryType = memory.getMemoryType();
            final ai.tegmentum.wasmtime4j.type.MemoryType expectedMemoryType =
                (ai.tegmentum.wasmtime4j.type.MemoryType) expectedType;
            LOGGER.fine(
                "expected: "
                    + formatMemoryType(expectedMemoryType)
                    + ", actual: "
                    + formatMemoryType(actualMemoryType));

            if (!typesMatch(expectedMemoryType, actualMemoryType)) {
              typeMatches = false;
              expectedTypeStr = formatMemoryType(expectedMemoryType);
              actualTypeStr = formatMemoryType(actualMemoryType);
            }
          } else {
            typeMatches = false;
          }
          break;

        case FUNCTION:
          if (actualImport instanceof ai.tegmentum.wasmtime4j.WasmFunction) {
            // Function type checking would go here
            // For now, accept any WasmFunction as matching
            typeMatches = true;
          } else {
            typeMatches = false;
          }
          break;

        default:
          typeMatches = false;
          expectedTypeStr = "Unknown type: " + expectedKind;
      }

      if (!typeMatches) {
        issues.add(
            new ai.tegmentum.wasmtime4j.validation.ImportIssue(
                ai.tegmentum.wasmtime4j.validation.ImportIssue.Severity.ERROR,
                ai.tegmentum.wasmtime4j.validation.ImportIssue.Type.TYPE_MISMATCH,
                moduleName,
                fieldName,
                "Import type does not match expected type",
                expectedTypeStr,
                actualTypeStr));
      } else {
        validCount++;
        // Determine ImportInfo.ImportKind from WasmTypeKind
        final ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind infoType;
        switch (expectedKind) {
          case GLOBAL:
            infoType = ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.GLOBAL;
            break;
          case TABLE:
            infoType = ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.TABLE;
            break;
          case MEMORY:
            infoType = ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.MEMORY;
            break;
          case FUNCTION:
            infoType = ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.FUNCTION;
            break;
          default:
            infoType = ai.tegmentum.wasmtime4j.validation.ImportInfo.ImportKind.FUNCTION;
        }

        validatedImports.add(
            new ai.tegmentum.wasmtime4j.validation.ImportInfo(
                moduleName,
                fieldName,
                infoType,
                java.util.Optional.of(actualTypeStr),
                java.time.Instant.now(),
                actualImport instanceof ai.tegmentum.wasmtime4j.WasmFunction,
                java.util.Optional.of("Provided via ImportMap")));
      }
    }

    final long endTime = System.nanoTime();
    final java.time.Duration validationTime = java.time.Duration.ofNanos(endTime - startTime);

    LOGGER.fine("Validation complete: validCount=" + validCount + ", issues=" + issues.size());
    for (final ai.tegmentum.wasmtime4j.validation.ImportIssue issue : issues) {
      LOGGER.fine(
          "Issue: "
              + issue.getModuleName()
              + "."
              + issue.getImportName()
              + " type="
              + issue.getType()
              + " msg="
              + issue.getMessage());
    }

    return new ai.tegmentum.wasmtime4j.validation.ImportValidation(
        issues.isEmpty(),
        issues,
        validatedImports,
        moduleImports.size(),
        validCount,
        validationTime);
  }

  private boolean typesMatch(
      final ai.tegmentum.wasmtime4j.type.GlobalType expected,
      final ai.tegmentum.wasmtime4j.type.GlobalType actual) {
    return expected.getValueType() == actual.getValueType()
        && expected.isMutable() == actual.isMutable();
  }

  private boolean typesMatch(
      final ai.tegmentum.wasmtime4j.type.TableType expected,
      final ai.tegmentum.wasmtime4j.type.TableType actual) {
    return expected.getElementType() == actual.getElementType()
        && expected.getMinimum() <= actual.getMinimum()
        && (!expected.getMaximum().isPresent()
            || (actual.getMaximum().isPresent()
                && expected.getMaximum().get() >= actual.getMaximum().get()));
  }

  private boolean typesMatch(
      final ai.tegmentum.wasmtime4j.type.MemoryType expected,
      final ai.tegmentum.wasmtime4j.type.MemoryType actual) {
    return expected.getMinimum() <= actual.getMinimum()
        && expected.is64Bit() == actual.is64Bit()
        && expected.isShared() == actual.isShared()
        && (!expected.getMaximum().isPresent()
            || (actual.getMaximum().isPresent()
                && expected.getMaximum().get() >= actual.getMaximum().get()));
  }

  private String formatGlobalType(final ai.tegmentum.wasmtime4j.type.GlobalType type) {
    return String.format(
        "Global(%s, %s)", type.getValueType(), type.isMutable() ? "mutable" : "immutable");
  }

  private String formatTableType(final ai.tegmentum.wasmtime4j.type.TableType type) {
    return String.format(
        "Table(%s, min=%d, max=%s)",
        type.getElementType(),
        type.getMinimum(),
        type.getMaximum().map(String::valueOf).orElse("none"));
  }

  private String formatMemoryType(final ai.tegmentum.wasmtime4j.type.MemoryType type) {
    return String.format(
        "Memory(min=%d, max=%s, %s, %s)",
        type.getMinimum(),
        type.getMaximum().map(String::valueOf).orElse("none"),
        type.is64Bit() ? "64-bit" : "32-bit",
        type.isShared() ? "shared" : "not-shared");
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
    final List<ai.tegmentum.wasmtime4j.type.FuncType> functionTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.FUNCTION) {
        functionTypes.add((ai.tegmentum.wasmtime4j.type.FuncType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(functionTypes);
  }

  @Override
  public List<MemoryType> getMemoryTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.type.MemoryType> memoryTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.MEMORY) {
        memoryTypes.add((ai.tegmentum.wasmtime4j.type.MemoryType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(memoryTypes);
  }

  @Override
  public List<TableType> getTableTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.type.TableType> tableTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.TABLE) {
        tableTypes.add((ai.tegmentum.wasmtime4j.type.TableType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(tableTypes);
  }

  @Override
  public List<GlobalType> getGlobalTypes() {
    final List<ExportType> exports = getExports();
    final List<ai.tegmentum.wasmtime4j.type.GlobalType> globalTypes = new java.util.ArrayList<>();
    for (final ExportType export : exports) {
      if (export.getType().getKind() == ai.tegmentum.wasmtime4j.type.WasmTypeKind.GLOBAL) {
        globalTypes.add((ai.tegmentum.wasmtime4j.type.GlobalType) export.getType());
      }
    }
    return java.util.Collections.unmodifiableList(globalTypes);
  }

  @Override
  public Map<String, byte[]> getCustomSections() {
    ensureNotClosed();

    // Get custom sections as JSON from native code
    final MemorySegment jsonPtr = NATIVE_BINDINGS.moduleGetCustomSections(nativeModule);
    if (jsonPtr == null || jsonPtr.equals(MemorySegment.NULL)) {
      return Collections.emptyMap();
    }

    try {
      // Convert C string to Java String
      final String jsonString = jsonPtr.reinterpret(Long.MAX_VALUE).getString(0);

      if (jsonString == null || jsonString.equals("{}")) {
        return Collections.emptyMap();
      }

      // Parse JSON object {"name1":"base64data1","name2":"base64data2"} using Gson
      final Map<String, String> parsed =
          new Gson().fromJson(jsonString, new TypeToken<Map<String, String>>() {}.getType());

      if (parsed == null || parsed.isEmpty()) {
        return Collections.emptyMap();
      }

      // Decode Base64 values to byte arrays
      final Map<String, byte[]> result = new HashMap<>();
      for (final Map.Entry<String, String> entry : parsed.entrySet()) {
        try {
          final byte[] decoded = java.util.Base64.getDecoder().decode(entry.getValue());
          result.put(entry.getKey(), decoded);
        } catch (final IllegalArgumentException e) {
          // If not valid Base64, store as UTF-8 bytes
          result.put(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8));
        }
      }

      return Collections.unmodifiableMap(result);
    } finally {
      // Free the native string
      NATIVE_BINDINGS.moduleFreeString(jsonPtr);
    }
  }

  @Override
  public String getName() {
    ensureNotClosed();
    // Return module identifier based on native pointer
    // Matches JNI backend pattern which returns "jni-module-{handle}"
    return "panama-module-" + System.identityHashCode(nativeModule);
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
  }

  @Override
  public byte[] serialize() throws WasmException {
    ensureNotClosed();

    // Allocate memory for output pointers
    final MemorySegment dataPtrPtr = arena.allocate(ValueLayout.ADDRESS);
    final MemorySegment lenPtr = arena.allocate(ValueLayout.JAVA_LONG);

    // Call native serialize function
    final int result = NATIVE_BINDINGS.moduleSerialize(nativeModule, dataPtrPtr, lenPtr);

    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(result, "Failed to serialize module");
    }

    // Get the data pointer and length
    final long length = lenPtr.get(ValueLayout.JAVA_LONG, 0);
    final MemorySegment rawDataPtr = dataPtrPtr.get(ValueLayout.ADDRESS, 0);

    if (rawDataPtr == null || rawDataPtr.equals(MemorySegment.NULL) || length == 0) {
      // Return empty array for empty serialization
      return new byte[0];
    }

    // Reinterpret the pointer with the correct size for safe access
    final MemorySegment dataPtr = rawDataPtr.reinterpret(length);

    // Copy the serialized data into a byte array
    final byte[] serialized = new byte[(int) length];
    MemorySegment.copy(dataPtr, ValueLayout.JAVA_BYTE, 0, serialized, 0, (int) length);

    LOGGER.fine("Serialized module to " + length + " bytes");
    return serialized;
  }

  @Override
  public void close() {
    resourceHandle.close();
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
    if (wasmBytes == null) {
      return null;
    }
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
    final com.google.gson.JsonArray jsonArray =
        gson.fromJson(jsonString, com.google.gson.JsonArray.class);

    final List<ModuleImport> imports = new java.util.ArrayList<>();

    for (final com.google.gson.JsonElement element : jsonArray) {
      final com.google.gson.JsonObject importObj = element.getAsJsonObject();
      final String moduleName = importObj.get("module").getAsString();
      final String fieldName = importObj.get("name").getAsString();
      final com.google.gson.JsonObject importTypeObj = importObj.getAsJsonObject("import_type");

      final ai.tegmentum.wasmtime4j.type.WasmType wasmType = parseImportTypeJson(importTypeObj);
      final ai.tegmentum.wasmtime4j.type.ImportType importType =
          new ai.tegmentum.wasmtime4j.type.ImportType(moduleName, fieldName, wasmType);
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
    final com.google.gson.JsonArray jsonArray =
        gson.fromJson(jsonString, com.google.gson.JsonArray.class);

    final List<ModuleExport> exports = new java.util.ArrayList<>();

    for (final com.google.gson.JsonElement element : jsonArray) {
      final com.google.gson.JsonObject exportObj = element.getAsJsonObject();
      final String name = exportObj.get("name").getAsString();
      final com.google.gson.JsonObject exportTypeObj = exportObj.getAsJsonObject("export_type");

      final ai.tegmentum.wasmtime4j.type.WasmType wasmType = parseExportTypeJson(exportTypeObj);
      final ai.tegmentum.wasmtime4j.type.ExportType exportType =
          new ai.tegmentum.wasmtime4j.type.ExportType(name, wasmType);
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
  private ai.tegmentum.wasmtime4j.type.WasmType parseImportTypeJson(
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
  private ai.tegmentum.wasmtime4j.type.WasmType parseExportTypeJson(
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
  private ai.tegmentum.wasmtime4j.type.FuncType parseFunctionType(
      final com.google.gson.JsonObject funcObj) {
    final com.google.gson.JsonArray paramsArray = funcObj.getAsJsonArray("params");
    final com.google.gson.JsonArray returnsArray = funcObj.getAsJsonArray("returns");

    final List<ai.tegmentum.wasmtime4j.WasmValueType> params = parseValueTypeArray(paramsArray);
    final List<ai.tegmentum.wasmtime4j.WasmValueType> results = parseValueTypeArray(returnsArray);

    return ai.tegmentum.wasmtime4j.panama.type.PanamaFuncType.of(params, results);
  }

  /**
   * Parses global type JSON.
   *
   * @param globalArray JSON array [valueType, isMutable]
   * @return GlobalType instance
   */
  private ai.tegmentum.wasmtime4j.type.GlobalType parseGlobalType(
      final com.google.gson.JsonArray globalArray) {
    final String valueTypeStr = globalArray.get(0).getAsString();
    final boolean isMutable = globalArray.get(1).getAsBoolean();

    final ai.tegmentum.wasmtime4j.WasmValueType valueType = parseValueType(valueTypeStr);

    return ai.tegmentum.wasmtime4j.panama.type.PanamaGlobalType.of(valueType, isMutable);
  }

  /**
   * Parses memory type JSON.
   *
   * @param memoryArray JSON array [min, max(optional), isShared]
   * @return MemoryType instance
   */
  private ai.tegmentum.wasmtime4j.type.MemoryType parseMemoryType(
      final com.google.gson.JsonArray memoryArray) {
    final long minimum = memoryArray.get(0).getAsLong();
    final com.google.gson.JsonElement maxElement = memoryArray.get(1);
    final Long maximum = maxElement.isJsonNull() ? null : maxElement.getAsLong();
    final boolean isShared = memoryArray.get(2).getAsBoolean();

    return new ai.tegmentum.wasmtime4j.panama.type.PanamaMemoryType(
        minimum, maximum, false, isShared);
  }

  /**
   * Parses table type JSON.
   *
   * @param tableArray JSON array [elementType, min, max(optional)]
   * @return TableType instance
   */
  private ai.tegmentum.wasmtime4j.type.TableType parseTableType(
      final com.google.gson.JsonArray tableArray) {
    final String elementTypeStr = tableArray.get(0).getAsString();
    final long minimum = tableArray.get(1).getAsLong();
    final com.google.gson.JsonElement maxElement = tableArray.get(2);
    final Long maximum = maxElement.isJsonNull() ? null : maxElement.getAsLong();

    final ai.tegmentum.wasmtime4j.WasmValueType elementType = parseValueType(elementTypeStr);

    return ai.tegmentum.wasmtime4j.panama.type.PanamaTableType.of(elementType, minimum, maximum);
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
    resourceHandle.ensureNotClosed();
  }
}
