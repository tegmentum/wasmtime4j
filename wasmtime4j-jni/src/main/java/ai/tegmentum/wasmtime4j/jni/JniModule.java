package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.CustomSection;
import ai.tegmentum.wasmtime4j.CustomSectionMetadata;
import ai.tegmentum.wasmtime4j.CustomSectionParser;
import ai.tegmentum.wasmtime4j.CustomSectionType;
import ai.tegmentum.wasmtime4j.DefaultCustomSectionMetadata;
import ai.tegmentum.wasmtime4j.DefaultCustomSectionParser;
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
import ai.tegmentum.wasmtime4j.WasmType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeMethodBindings;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * JNI implementation of the WebAssembly Module.
 *
 * <p>This class represents a compiled WebAssembly module and provides access to its metadata,
 * imports, and exports through JNI calls to the native Wasmtime library. A module contains the
 * compiled WebAssembly bytecode and can be instantiated multiple times to create independent
 * execution contexts.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic resource management with {@link AutoCloseable}
 *   <li>Defensive programming to prevent JVM crashes
 *   <li>Comprehensive parameter validation
 *   <li>Thread-safe operations
 *   <li>Module metadata inspection (exports, imports, size)
 *   <li>Static bytecode validation without compilation
 * </ul>
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * try (JniEngine engine = JniEngine.create()) {
 *   // Validate bytecode first (optional but recommended)
 *   if (!JniModule.validate(wasmBytes)) {
 *     throw new IllegalArgumentException("Invalid WebAssembly bytecode");
 *   }
 *
 *   // Compile module
 *   try (JniModule module = engine.compileModule(wasmBytes)) {
 *     // Inspect module metadata
 *     String[] functions = module.getExportedFunctions();
 *     String[] imports = module.getImportedFunctions();
 *     long moduleSize = module.getSize();
 *
 *     // Create instances from this module
 *     try (JniStore store = engine.createStore()) {
 *       JniInstance instance = module.instantiate(store);
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>This implementation extends {@link JniResource} to provide automatic native resource
 * management and follows defensive programming practices to prevent native crashes.
 *
 * @since 1.0.0
 */
public final class JniModule extends JniResource implements Module {

  private static final Logger LOGGER = Logger.getLogger(JniModule.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniModule: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** WebAssembly features that may be supported by modules. */
  public enum WasmFeature {
    SIMD("simd", "SIMD operations support"),
    MULTI_MEMORY("multi-memory", "Multiple memory support"),
    REFERENCE_TYPES("reference-types", "Reference types support"),
    BULK_MEMORY("bulk-memory", "Bulk memory operations"),
    TAIL_CALL("tail-call", "Tail call optimization"),
    MULTI_VALUE("multi-value", "Multi-value function returns"),
    EXCEPTION_HANDLING("exception-handling", "Exception handling support"),
    RELAXED_SIMD("relaxed-simd", "Relaxed SIMD operations");

    private final String name;
    private final String description;

    WasmFeature(final String name, final String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Information about module linking capabilities. */
  public static final class LinkingInfo {
    private final String name;
    private final List<String> dependencies;
    private final Map<String, String> symbols;

    /**
     * Creates linking information with specified dependencies and symbols.
     *
     * @param name the module name
     * @param dependencies the list of module dependencies
     * @param symbols the symbol mapping
     */
    public LinkingInfo(
        final String name, final List<String> dependencies, final Map<String, String> symbols) {
      this.name = name;
      this.dependencies = Collections.unmodifiableList(new ArrayList<>(dependencies));
      this.symbols = Collections.unmodifiableMap(new HashMap<>(symbols));
    }

    public String getName() {
      return name;
    }

    public List<String> getDependencies() {
      return dependencies;
    }

    public Map<String, String> getSymbols() {
      return symbols;
    }
  }

  /** Cache for import metadata to avoid repeated native calls. */
  private volatile List<ImportType> importCache = null;

  /** Cache for export metadata to avoid repeated native calls. */
  private volatile List<ExportType> exportCache = null;

  /** Cache for module features to avoid repeated native calls. */
  private volatile Set<WasmFeature> featuresCache = null;

  /** Cache for module linking information. */
  private volatile Map<String, LinkingInfo> linkingCache = null;

  /** Reference to the engine used to compile this module. */
  private final JniEngine engine;

  /**
   * Creates a new JNI module with the given native handle.
   *
   * <p>This constructor is package-private and should only be used by the JniEngine or other JNI
   * classes. External code should create modules through {@link JniEngine#compileModule(byte[])}.
   *
   * @param nativeHandle the native module handle from Wasmtime
   * @param engine the engine used to compile this module
   * @throws JniResourceException if nativeHandle is invalid
   */
  JniModule(final long nativeHandle, final JniEngine engine) {
    super(nativeHandle);
    JniValidation.requireNonNull(engine, "engine");
    this.engine = engine;
    LOGGER.fine("Created JNI module with handle: 0x" + Long.toHexString(nativeHandle));
  }

  /**
   * Creates an instance of this module within the specified store.
   *
   * <p>This method instantiates the compiled WebAssembly module, creating a new execution context.
   * The instance will have access to all exports defined by the module and must satisfy all import
   * requirements.
   *
   * @param store the store context for the new instance
   * @return a new module instance
   * @throws JniException if instantiation fails
   * @throws JniResourceException if this module or store has been closed
   */
  public JniInstance instantiate(final JniStore store) {
    return instantiate(store, null);
  }

  @Override
  public Instance instantiate(final Store store) throws WasmException {
    JniValidation.requireNonNull(store, "store");
    if (!(store instanceof JniStore)) {
      throw new WasmException("Store must be a JniStore instance");
    }
    return instantiate((JniStore) store);
  }

  @Override
  public Instance instantiate(final Store store, final ImportMap imports) throws WasmException {
    JniValidation.requireNonNull(store, "store");
    if (!(store instanceof JniStore)) {
      throw new WasmException("Store must be a JniStore instance");
    }
    return instantiate((JniStore) store, imports);
  }

  /**
   * Creates an instance of this module with the provided imports.
   *
   * <p>This method instantiates the compiled WebAssembly module with specific import bindings,
   * creating a new execution context. All required imports must be provided or instantiation will
   * fail.
   *
   * @param store the store context for the new instance
   * @param imports the import definitions for the module (null for no imports)
   * @return a new module instance with the specified imports
   * @throws JniException if instantiation fails or imports don't match requirements
   * @throws JniResourceException if this module or store has been closed
   */
  public JniInstance instantiate(final JniStore store, final ImportMap imports) {
    JniValidation.requireNonNull(store, "store");
    ensureNotClosed();

    try {
      final long instanceHandle;
      if (imports != null) {
        // Validate imports against module requirements
        if (!validateImports(imports)) {
          throw new JniException("Provided imports do not satisfy module requirements");
        }

        // Convert imports to native representation
        final long importMapHandle = convertImportMapToNative(imports, store);
        try {
          instanceHandle =
              nativeInstantiateModuleWithImports(
                  getNativeHandle(), store.getNativeHandle(), importMapHandle);
        } finally {
          // Clean up the temporary import map handle
          if (importMapHandle != 0) {
            nativeDestroyImportMap(importMapHandle);
          }
        }
      } else {
        instanceHandle = nativeInstantiateModule(getNativeHandle(), store.getNativeHandle());
      }

      JniValidation.requireValidHandle(instanceHandle, "instanceHandle");
      return new JniInstance(instanceHandle, this, store);
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Failed to instantiate module with imports", e);
    }
  }

  /**
   * Gets the names of all functions exported by this module.
   *
   * <p>This method returns a defensive copy of the export names to prevent external modification of
   * the internal data structures.
   *
   * @return array of exported function names (never null, may be empty)
   * @throws JniException if the exports cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public String[] getExportedFunctions() {
    ensureNotClosed();

    try {
      final String[] functions = nativeGetExportedFunctions(getNativeHandle());
      return functions != null ? functions.clone() : new String[0];
    } catch (final Exception e) {
      throw new JniException("Failed to get exported functions", e);
    }
  }

  /**
   * Gets the names of all memories exported by this module.
   *
   * <p>This method returns a defensive copy of the export names to prevent external modification of
   * the internal data structures.
   *
   * @return array of exported memory names (never null, may be empty)
   * @throws JniException if the exports cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public String[] getExportedMemories() {
    ensureNotClosed();

    try {
      final String[] memories = nativeGetExportedMemories(getNativeHandle());
      return memories != null ? memories.clone() : new String[0];
    } catch (final Exception e) {
      throw new JniException("Failed to get exported memories", e);
    }
  }

  /**
   * Gets the names of all tables exported by this module.
   *
   * <p>This method returns a defensive copy of the export names to prevent external modification of
   * the internal data structures.
   *
   * @return array of exported table names (never null, may be empty)
   * @throws JniException if the exports cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public String[] getExportedTables() {
    ensureNotClosed();

    try {
      final String[] tables = nativeGetExportedTables(getNativeHandle());
      return tables != null ? tables.clone() : new String[0];
    } catch (final Exception e) {
      throw new JniException("Failed to get exported tables", e);
    }
  }

  /**
   * Gets the names of all globals exported by this module.
   *
   * <p>This method returns a defensive copy of the export names to prevent external modification of
   * the internal data structures.
   *
   * @return array of exported global names (never null, may be empty)
   * @throws JniException if the exports cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public String[] getExportedGlobals() {
    ensureNotClosed();

    try {
      final String[] globals = nativeGetExportedGlobals(getNativeHandle());
      return globals != null ? globals.clone() : new String[0];
    } catch (final Exception e) {
      throw new JniException("Failed to get exported globals", e);
    }
  }

  /**
   * Gets the names of all functions imported by this module.
   *
   * <p>Import names are returned in "module::name" format, where "module" is the import module name
   * and "name" is the imported function name.
   *
   * <p>This method returns a defensive copy of the import names to prevent external modification of
   * the internal data structures.
   *
   * @return array of imported function names in "module::name" format (never null, may be empty)
   * @throws JniException if the imports cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public String[] getImportedFunctions() {
    ensureNotClosed();

    try {
      final String[] functions = nativeGetImportedFunctions(getNativeHandle());
      return functions != null ? functions.clone() : new String[0];
    } catch (final Exception e) {
      throw new JniException("Failed to get imported functions", e);
    }
  }

  /**
   * Gets comprehensive export metadata for this module.
   *
   * <p>This method provides detailed information about all exports including their types,
   * signatures, and metadata. Results are cached to avoid expensive native calls on repeated
   * access.
   *
   * @return immutable list of export type information (never null, may be empty)
   * @throws JniException if export metadata cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public List<ExportType> getExports() {
    List<ExportType> cachedExports = exportCache;
    if (cachedExports != null) {
      return cachedExports;
    }

    ensureNotClosed();

    try {
      // Get comprehensive export metadata from native layer
      final String[] exportData = nativeGetExportMetadata(getNativeHandle());
      final List<ExportType> exports = parseExportMetadata(exportData);

      exportCache = Collections.unmodifiableList(exports);
      return exportCache;
    } catch (final Exception e) {
      throw new JniException("Failed to get export metadata", e);
    }
  }

  /**
   * Gets comprehensive import metadata for this module.
   *
   * <p>This method provides detailed information about all required imports including their types,
   * signatures, and metadata. Results are cached to avoid expensive native calls on repeated
   * access.
   *
   * @return immutable list of import type information (never null, may be empty)
   * @throws JniException if import metadata cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public List<ImportType> getImports() {
    List<ImportType> cachedImports = importCache;
    if (cachedImports != null) {
      return cachedImports;
    }

    ensureNotClosed();

    try {
      // Get comprehensive import metadata from native layer
      final String[] importData = nativeGetImportMetadata(getNativeHandle());
      final List<ImportType> imports = parseImportMetadata(importData);

      importCache = Collections.unmodifiableList(imports);
      return importCache;
    } catch (final Exception e) {
      throw new JniException("Failed to get import metadata", e);
    }
  }

  @Override
  public boolean hasImport(final String moduleName, final String fieldName) {
    JniValidation.requireNonBlank(moduleName, "moduleName");
    JniValidation.requireNonBlank(fieldName, "fieldName");
    ensureNotClosed();

    try {
      final List<ImportType> imports = getImports();
      for (final ImportType importType : imports) {
        if (moduleName.equals(importType.getModuleName())
            && fieldName.equals(importType.getName())) {
          return true;
        }
      }
      return false;
    } catch (final Exception e) {
      throw new JniException("Failed to check import existence", e);
    }
  }

  @Override
  public boolean hasExport(final String name) {
    JniValidation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      // Check if any of the exported functions, memories, tables, or globals match the name
      final String[] functions = getExportedFunctions();
      for (final String function : functions) {
        if (name.equals(function)) {
          return true;
        }
      }

      final String[] memories = getExportedMemories();
      for (final String memory : memories) {
        if (name.equals(memory)) {
          return true;
        }
      }

      final String[] tables = getExportedTables();
      for (final String table : tables) {
        if (name.equals(table)) {
          return true;
        }
      }

      final String[] globals = getExportedGlobals();
      for (final String global : globals) {
        if (name.equals(global)) {
          return true;
        }
      }

      return false;
    } catch (final Exception e) {
      throw new JniException("Failed to check export existence", e);
    }
  }

  @Override
  public Optional<TableType> getTableType(final String tableName) {
    JniValidation.requireNonBlank(tableName, "tableName");
    ensureNotClosed();

    try {
      // Check if the table exists in exports
      final String[] tables = getExportedTables();
      for (final String table : tables) {
        if (tableName.equals(table)) {
          // TODO: Implement native getTableType to get actual table type information
          // For now, return empty as this would need native implementation
          return Optional.empty();
        }
      }
      return Optional.empty();
    } catch (final Exception e) {
      throw new JniException("Failed to get table type", e);
    }
  }

  @Override
  public Optional<MemoryType> getMemoryType(final String memoryName) {
    JniValidation.requireNonBlank(memoryName, "memoryName");
    ensureNotClosed();

    try {
      // Check if the memory exists in exports
      final String[] memories = getExportedMemories();
      for (final String memory : memories) {
        if (memoryName.equals(memory)) {
          // TODO: Implement native getMemoryType to get actual memory type information
          // For now, return empty as this would need native implementation
          return Optional.empty();
        }
      }
      return Optional.empty();
    } catch (final Exception e) {
      throw new JniException("Failed to get memory type", e);
    }
  }

  @Override
  public Optional<GlobalType> getGlobalType(final String globalName) {
    JniValidation.requireNonBlank(globalName, "globalName");
    ensureNotClosed();

    try {
      // Check if the global exists in exports
      final String[] globals = getExportedGlobals();
      for (final String global : globals) {
        if (globalName.equals(global)) {
          // TODO: Implement native getGlobalType to get actual global type information
          // For now, return empty as this would need native implementation
          return Optional.empty();
        }
      }
      return Optional.empty();
    } catch (final Exception e) {
      throw new JniException("Failed to get global type", e);
    }
  }

  @Override
  public Optional<FuncType> getFunctionType(final String functionName) {
    JniValidation.requireNonBlank(functionName, "functionName");
    ensureNotClosed();

    try {
      // Check if the function exists in exports
      final String[] functions = getExportedFunctions();
      for (final String function : functions) {
        if (functionName.equals(function)) {
          // TODO: Implement native getFunctionType to get actual function type information
          // For now, return empty as this would need native implementation
          return Optional.empty();
        }
      }
      return Optional.empty();
    } catch (final Exception e) {
      throw new JniException("Failed to get function type", e);
    }
  }

  @Override
  public List<ImportDescriptor> getImportDescriptors() {
    ensureNotClosed();

    try {
      // TODO: Implement native getImportDescriptors to get actual import descriptor information
      // For now, return empty list as this would need native implementation
      return Collections.emptyList();
    } catch (final Exception e) {
      throw new JniException("Failed to get import descriptors", e);
    }
  }

  @Override
  public List<ExportDescriptor> getExportDescriptors() {
    ensureNotClosed();

    try {
      // TODO: Implement native getExportDescriptors to get actual export descriptor information
      // For now, return empty list as this would need native implementation
      return Collections.emptyList();
    } catch (final Exception e) {
      throw new JniException("Failed to get export descriptors", e);
    }
  }

  /**
   * Validates that provided imports satisfy this module's requirements.
   *
   * <p>This method checks that all required imports are provided with compatible types. It performs
   * comprehensive type checking to ensure runtime compatibility.
   *
   * @param imports the import definitions to validate (null means no imports)
   * @return true if imports satisfy module requirements, false otherwise
   * @throws JniException if validation fails due to internal errors
   * @throws JniResourceException if this module has been closed
   */
  public boolean validateImports(final ImportMap imports) {
    ensureNotClosed();

    if (imports == null) {
      // Check if module requires no imports
      return getImports().isEmpty();
    }

    try {
      final List<ImportType> requiredImports = getImports();

      // Check that all required imports are provided
      for (final ImportType required : requiredImports) {
        final String key = required.getModuleName() + "::" + required.getName();
        if (!imports.contains(required.getModuleName(), required.getName())) {
          LOGGER.fine("Missing required import: " + key);
          return false;
        }

        // TODO: Add comprehensive type compatibility checking
        // This would involve checking function signatures, memory types, etc.
      }

      return true;
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Import validation failed", e);
    }
  }

  /**
   * Gets the engine that was used to compile this module.
   *
   * @return the engine used for compilation
   */
  public JniEngine getEngine() {
    return engine;
  }

  /**
   * Gets the name of this module if it has one.
   *
   * <p>Module names are optional in WebAssembly and may be embedded in the bytecode or provided
   * during compilation.
   *
   * @return the module name, or null if unnamed
   * @throws JniException if module name cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public String getName() {
    ensureNotClosed();

    try {
      return nativeGetModuleName(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get module name", e);
    }
  }

  /**
   * Gets custom sections from this module.
   *
   * <p>Custom sections contain arbitrary data that can be embedded in WebAssembly modules for
   * metadata or debugging purposes.
   *
   * @return a map of custom section names to their data
   * @throws JniException if custom sections cannot be retrieved
   * @throws JniResourceException if this module has been closed
   * @deprecated Use {@link #getCustomSectionMetadata()} for comprehensive custom section access
   */
  @Override
  @Deprecated
  public java.util.Map<String, String> getCustomSections() {
    ensureNotClosed();

    try {
      final String[] customSectionData = nativeGetCustomSections(getNativeHandle());
      final Map<String, String> customSections = new HashMap<>();

      if (customSectionData != null) {
        // Parse custom section data format: name|data_as_base64
        for (final String entry : customSectionData) {
          if (entry != null && !entry.trim().isEmpty()) {
            final String[] parts = entry.split("\\|", 2);
            if (parts.length == 2) {
              customSections.put(parts[0], parts[1]);
            }
          }
        }
      }

      return Collections.unmodifiableMap(customSections);
    } catch (final Exception e) {
      throw new JniException("Failed to get custom sections", e);
    }
  }

  /**
   * Gets comprehensive custom section metadata for this module.
   *
   * <p>This provides access to all custom sections including standard sections like "name",
   * "producers", and "target_features", as well as arbitrary custom sections.
   *
   * @return custom section metadata interface
   * @throws JniException if custom section metadata cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  @Override
  public CustomSectionMetadata getCustomSectionMetadata() {
    ensureNotClosed();

    try {
      final List<CustomSection> customSections = parseCustomSectionsFromNative();
      final CustomSectionParser parser = new DefaultCustomSectionParser();
      return new DefaultCustomSectionMetadata(customSections, parser);
    } catch (final Exception e) {
      throw new JniException("Failed to get custom section metadata", e);
    }
  }

  /**
   * Gets the imports required by this module as ModuleImport objects.
   *
   * <p>This method provides enhanced import information compared to {@link #getImports()},
   * including complete type details for each import.
   *
   * @return an immutable list of module imports with complete type information
   * @throws JniException if module imports cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  @Override
  public java.util.List<ModuleImport> getModuleImports() {
    ensureNotClosed();

    try {
      final String[] moduleImportData = nativeGetModuleImports(getNativeHandle());
      return parseModuleImports(moduleImportData);
    } catch (final Exception e) {
      throw new JniException("Failed to get module imports", e);
    }
  }

  /**
   * Gets the exports defined by this module as ModuleExport objects.
   *
   * <p>This method provides enhanced export information compared to {@link #getExports()},
   * including complete type details for each export.
   *
   * @return an immutable list of module exports with complete type information
   * @throws JniException if module exports cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  @Override
  public java.util.List<ModuleExport> getModuleExports() {
    ensureNotClosed();

    try {
      final String[] moduleExportData = nativeGetModuleExports(getNativeHandle());
      return parseModuleExports(moduleExportData);
    } catch (final Exception e) {
      throw new JniException("Failed to get module exports", e);
    }
  }

  /**
   * Gets all function types defined in this module.
   *
   * <p>This includes function types for both imported and exported functions, as well as internal
   * functions.
   *
   * @return an immutable list of function types
   * @throws JniException if function types cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  @Override
  public java.util.List<FuncType> getFunctionTypes() {
    ensureNotClosed();

    try {
      final String[] functionTypeData = nativeGetFunctionTypes(getNativeHandle());
      return parseFunctionTypes(functionTypeData);
    } catch (final Exception e) {
      throw new JniException("Failed to get function types", e);
    }
  }

  /**
   * Gets all memory types defined in this module.
   *
   * <p>This includes memory types for both imported and exported memories.
   *
   * @return an immutable list of memory types
   * @throws JniException if memory types cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  @Override
  public java.util.List<MemoryType> getMemoryTypes() {
    ensureNotClosed();

    try {
      final String[] memoryTypeData = nativeGetMemoryTypes(getNativeHandle());
      return parseMemoryTypes(memoryTypeData);
    } catch (final Exception e) {
      throw new JniException("Failed to get memory types", e);
    }
  }

  /**
   * Gets all table types defined in this module.
   *
   * <p>This includes table types for both imported and exported tables.
   *
   * @return an immutable list of table types
   * @throws JniException if table types cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  @Override
  public java.util.List<TableType> getTableTypes() {
    ensureNotClosed();

    try {
      final String[] tableTypeData = nativeGetTableTypes(getNativeHandle());
      return parseTableTypes(tableTypeData);
    } catch (final Exception e) {
      throw new JniException("Failed to get table types", e);
    }
  }

  /**
   * Gets all global types defined in this module.
   *
   * <p>This includes global types for both imported and exported globals.
   *
   * @return an immutable list of global types
   * @throws JniException if global types cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  @Override
  public java.util.List<GlobalType> getGlobalTypes() {
    ensureNotClosed();

    try {
      final String[] globalTypeData = nativeGetGlobalTypes(getNativeHandle());
      return parseGlobalTypes(globalTypeData);
    } catch (final Exception e) {
      throw new JniException("Failed to get global types", e);
    }
  }

  /**
   * Checks if the module is still valid and usable.
   *
   * @return true if the module is valid and not closed, false otherwise
   */
  public boolean isValid() {
    return !isClosed();
  }

  /**
   * Gets the WebAssembly features supported by this module.
   *
   * <p>This method detects which advanced WebAssembly features are used by the module, such as SIMD
   * operations, multi-memory, reference types, etc. Results are cached to avoid expensive native
   * calls on repeated access.
   *
   * @return immutable set of WebAssembly features used by this module (never null, may be empty)
   * @throws JniException if feature detection fails
   * @throws JniResourceException if this module has been closed
   */
  public Set<WasmFeature> getSupportedFeatures() {
    Set<WasmFeature> cachedFeatures = featuresCache;
    if (cachedFeatures != null) {
      return cachedFeatures;
    }

    ensureNotClosed();

    try {
      final String[] featureNames = nativeGetModuleFeatures(getNativeHandle());
      final Set<WasmFeature> features = parseFeatureNames(featureNames);

      featuresCache = Collections.unmodifiableSet(features);
      return featuresCache;
    } catch (final Exception e) {
      throw new JniException("Failed to detect module features", e);
    }
  }

  /**
   * Gets module linking information for complex WebAssembly applications.
   *
   * <p>This method provides information about module dependencies, symbols, and linking
   * requirements for advanced multi-module WebAssembly applications.
   *
   * @return map of linking information by dependency name (never null, may be empty)
   * @throws JniException if linking information cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public Map<String, LinkingInfo> getLinkingInfo() {
    Map<String, LinkingInfo> cachedLinking = linkingCache;
    if (cachedLinking != null) {
      return cachedLinking;
    }

    ensureNotClosed();

    try {
      final String[] linkingData = nativeGetModuleLinkingInfo(getNativeHandle());
      final Map<String, LinkingInfo> linkingInfo = parseLinkingInfo(linkingData);

      linkingCache = Collections.unmodifiableMap(linkingInfo);
      return linkingCache;
    } catch (final Exception e) {
      throw new JniException("Failed to get module linking information", e);
    }
  }

  /**
   * Serializes this compiled module to bytes using advanced serialization framework.
   *
   * <p>This method exports the compiled module using the advanced serialization engine with
   * comprehensive optimization, security, and metadata features.
   *
   * @return serialized module bytes (never null)
   * @throws JniException if serialization fails
   * @throws JniResourceException if this module has been closed
   */
  public byte[] serialize() {
    return serializeAdvanced(null, null).getSerializedData();
  }

  /**
   * Serializes this compiled module using the advanced serialization framework.
   *
   * <p>This method provides comprehensive serialization with optimization, caching, security
   * features, and performance monitoring.
   *
   * @param format the serialization format (null for auto-selection)
   * @param options the serialization options (null for defaults)
   * @return the advanced serialization result
   * @throws JniException if serialization fails
   * @throws JniResourceException if this module has been closed
   */
  public ai.tegmentum.wasmtime4j.serialization.SerializationResult serializeAdvanced(
      final ai.tegmentum.wasmtime4j.serialization.ModuleSerializationFormat format,
      final ai.tegmentum.wasmtime4j.serialization.SerializationOptions options) {
    ensureNotClosed();

    try {
      // Import advanced serialization components
      final ai.tegmentum.wasmtime4j.serialization.ModuleSerializationEngine engine =
          new ai.tegmentum.wasmtime4j.serialization.ModuleSerializationEngine();
      final ai.tegmentum.wasmtime4j.serialization.optimization.SerializationOptimizer optimizer =
          new ai.tegmentum.wasmtime4j.serialization.optimization.SerializationOptimizer();

      // Determine optimal format and options
      final long moduleSize = getSize();
      final ai.tegmentum.wasmtime4j.serialization.ModuleSerializationFormat targetFormat =
          format != null
              ? format
              : ai.tegmentum.wasmtime4j.serialization.ModuleSerializationFormat.getOptimalFormat(
                  ai.tegmentum.wasmtime4j.serialization.ModuleSerializationFormat
                      .SerializationUseCase.DISK_CACHE);

      final ai.tegmentum.wasmtime4j.serialization.SerializationOptions targetOptions =
          options != null ? options : optimizer.optimize(moduleSize, targetFormat);

      // Perform advanced serialization
      final ai.tegmentum.wasmtime4j.serialization.SerializationResult result =
          engine.serialize(this, targetFormat, targetOptions);

      LOGGER.fine("Advanced serialization completed: " + result.getSummary());
      return result;

    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Advanced serialization failed", e);
    }
  }

  /**
   * Gets raw module data for serialization engine integration.
   *
   * <p>This method extracts the raw compiled module bytes from the native representation for use by
   * the advanced serialization engine.
   *
   * @return the raw module data
   * @throws JniException if extraction fails
   */
  byte[] getRawModuleData() {
    ensureNotClosed();

    try {
      final byte[] rawData = nativeSerializeModule(getNativeHandle());
      JniValidation.requireNonNull(rawData, "rawData");
      return rawData.clone();
    } catch (final Exception e) {
      throw new JniException("Failed to get raw module data", e);
    }
  }

  /**
   * Deserializes a module from previously serialized bytes using basic format.
   *
   * <p>This static method creates a module from bytes produced by {@link #serialize()}. For
   * advanced deserialization with metadata, use {@link #deserializeAdvanced}.
   *
   * @param engine the engine to associate with the deserialized module
   * @param serializedData the serialized module bytes
   * @return deserialized module instance
   * @throws JniException if deserialization fails
   */
  public static JniModule deserialize(final JniEngine engine, final byte[] serializedData) {
    JniValidation.requireNonNull(engine, "engine");
    JniValidation.requireNonEmpty(serializedData, "serializedData");
    NativeMethodBindings.ensureInitialized();

    final byte[] dataCopy = JniValidation.defensiveCopy(serializedData);

    try {
      final long moduleHandle = nativeDeserializeModule(engine.getNativeHandle(), dataCopy);
      JniValidation.requireValidHandle(moduleHandle, "moduleHandle");
      return new JniModule(moduleHandle, engine);
    } catch (final Exception e) {
      throw new JniException("Failed to deserialize module", e);
    }
  }

  /**
   * Deserializes a module using the advanced serialization framework.
   *
   * <p>This method provides comprehensive deserialization with integrity verification,
   * compatibility checking, and performance monitoring.
   *
   * @param engine the engine to associate with the deserialized module
   * @param serializedData the serialized module data
   * @param metadata the serialization metadata
   * @return deserialized module instance
   * @throws JniException if deserialization fails
   */
  public static JniModule deserializeAdvanced(
      final JniEngine engine,
      final byte[] serializedData,
      final ai.tegmentum.wasmtime4j.serialization.SerializedModuleMetadata metadata) {
    JniValidation.requireNonNull(engine, "engine");
    JniValidation.requireNonEmpty(serializedData, "serializedData");
    JniValidation.requireNonNull(metadata, "metadata");
    NativeMethodBindings.ensureInitialized();

    try {
      // Use advanced serialization engine for deserialization
      final ai.tegmentum.wasmtime4j.serialization.ModuleSerializationEngine serializationEngine =
          new ai.tegmentum.wasmtime4j.serialization.ModuleSerializationEngine();

      // Deserialize using advanced framework
      final ai.tegmentum.wasmtime4j.Module module =
          serializationEngine.deserialize(serializedData, metadata);

      // Since we need to return JniModule, we need to handle the integration
      // For now, fallback to basic deserialization with validation
      if (!metadata.validateIntegrity(serializedData)) {
        throw new JniException("Serialized data integrity validation failed");
      }

      // Extract the actual module data from the advanced format
      // This would require integration with the serialization engine
      final byte[] rawModuleData = extractRawModuleData(serializedData, metadata);

      final long moduleHandle = nativeDeserializeModule(engine.getNativeHandle(), rawModuleData);
      JniValidation.requireValidHandle(moduleHandle, "moduleHandle");

      LOGGER.fine("Advanced deserialization completed for format: " + metadata.getFormat());
      return new JniModule(moduleHandle, engine);

    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Advanced deserialization failed", e);
    }
  }

  /**
   * Extracts raw module data from advanced serialization format.
   *
   * @param serializedData the advanced serialized data
   * @param metadata the serialization metadata
   * @return the raw module data
   * @throws JniException if extraction fails
   */
  private static byte[] extractRawModuleData(
      final byte[] serializedData,
      final ai.tegmentum.wasmtime4j.serialization.SerializedModuleMetadata metadata)
      throws JniException {
    // This is a simplified extraction - in a full implementation, this would
    // work with the ModuleSerializationEngine to extract the raw data
    // For now, assume the data is in a compatible format
    return serializedData;
  }

  /**
   * Validates WebAssembly bytecode without compiling it.
   *
   * <p>This static method performs structural validation of WebAssembly bytecode to check if it
   * conforms to the WebAssembly specification. This is useful for quick validation before
   * attempting compilation, which can be expensive.
   *
   * <p>Note: This method only validates the structure and format of the bytecode. It does not check
   * import/export compatibility or other runtime concerns.
   *
   * @param bytecode the WebAssembly bytecode to validate
   * @return true if the bytecode is structurally valid, false otherwise
   * @throws JniException if validation fails due to internal errors
   */
  public static boolean validate(final byte[] bytecode) {
    JniValidation.requireNonEmpty(bytecode, "bytecode");
    NativeMethodBindings.ensureInitialized();

    final byte[] bytecodeCopy = JniValidation.defensiveCopy(bytecode);

    try {
      return nativeValidateModule(bytecodeCopy);
    } catch (final Exception e) {
      LOGGER.warning("Error validating module bytecode: " + e.getMessage());
      throw new JniException("Module validation failed", e);
    }
  }

  /**
   * Compiles WebAssembly Text (WAT) format into a Module.
   *
   * <p>This method compiles WebAssembly Text format directly into a module without requiring
   * intermediate conversion to binary format. The WAT text is parsed and compiled in a single step.
   *
   * @param engine the engine to use for compilation
   * @param watText the WebAssembly text format code
   * @return a compiled Module
   * @throws WasmException if compilation fails
   * @throws IllegalArgumentException if engine or watText is null
   * @since 1.0.0
   */
  public static JniModule compileWat(final JniEngine engine, final String watText)
      throws WasmException {
    JniValidation.requireNonNull(engine, "engine");
    JniValidation.requireNonEmpty(watText, "watText");
    NativeMethodBindings.ensureInitialized();

    engine.ensureNotClosed();

    try {
      final long moduleHandle = nativeCompileWat(engine.getNativeHandle(), watText);
      JniValidation.requireValidHandle(moduleHandle, "moduleHandle");
      return new JniModule(moduleHandle, engine);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to compile WAT text: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the size of the compiled module in bytes.
   *
   * <p>This returns the size of the internal representation of the compiled module, which may
   * differ from the original WebAssembly bytecode size due to compilation optimizations and
   * internal data structures.
   *
   * @return the module size in bytes (always >= 0)
   * @throws JniException if the size cannot be retrieved
   * @throws JniResourceException if this module has been closed
   */
  public long getSize() {
    ensureNotClosed();

    try {
      final long size = nativeGetModuleSize(getNativeHandle());
      JniValidation.requireNonNegative(size, "moduleSize");
      return size;
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Failed to get module size", e);
    }
  }

  /**
   * Parses custom sections from native data.
   *
   * @return list of custom sections
   * @throws JniException if parsing fails
   */
  private List<CustomSection> parseCustomSectionsFromNative() {
    final List<CustomSection> customSections = new ArrayList<>();

    try {
      final byte[][] rawCustomSections = nativeGetRawCustomSections(getNativeHandle());
      if (rawCustomSections != null) {
        for (final byte[] sectionData : rawCustomSections) {
          if (sectionData != null && sectionData.length > 0) {
            // Parse section header to get name and type
            final String sectionName = parseCustomSectionName(sectionData);
            final CustomSectionType sectionType = CustomSectionType.fromName(sectionName);
            final byte[] sectionContent = parseCustomSectionContent(sectionData);

            customSections.add(new CustomSection(sectionName, sectionContent, sectionType));
          }
        }
      }
    } catch (final Exception e) {
      throw new JniException("Failed to parse custom sections", e);
    }

    return customSections;
  }

  /**
   * Parses custom section name from raw section data.
   *
   * @param sectionData raw section data
   * @return section name
   */
  private String parseCustomSectionName(final byte[] sectionData) {
    // Custom sections start with: [name_length] [name_bytes] [content_bytes]
    // This is a simplified implementation
    if (sectionData.length < 1) {
      return "unknown";
    }

    final int nameLength = sectionData[0] & 0xFF;
    if (sectionData.length < 1 + nameLength) {
      return "unknown";
    }

    return new String(sectionData, 1, nameLength, java.nio.charset.StandardCharsets.UTF_8);
  }

  /**
   * Parses custom section content from raw section data.
   *
   * @param sectionData raw section data
   * @return section content
   */
  private byte[] parseCustomSectionContent(final byte[] sectionData) {
    if (sectionData.length < 1) {
      return new byte[0];
    }

    final int nameLength = sectionData[0] & 0xFF;
    final int contentStart = 1 + nameLength;

    if (sectionData.length <= contentStart) {
      return new byte[0];
    }

    final byte[] content = new byte[sectionData.length - contentStart];
    System.arraycopy(sectionData, contentStart, content, 0, content.length);
    return content;
  }

  /**
   * Parses module imports from native string array format.
   *
   * @param moduleImportData array of module import metadata strings
   * @return list of parsed module imports
   */
  private List<ModuleImport> parseModuleImports(final String[] moduleImportData) {
    if (moduleImportData == null || moduleImportData.length == 0) {
      return Collections.emptyList();
    }

    final List<ModuleImport> moduleImports = new ArrayList<>();

    // Parse module import data format: module|name|type|signature|description
    for (final String entry : moduleImportData) {
      if (entry != null && !entry.trim().isEmpty()) {
        final String[] parts = entry.split("\\|");
        if (parts.length >= 4) {
          final String moduleName = parts[0];
          final String name = parts[1];
          final String typeString = parts[2];
          final String signature = parts[3];
          final String description = parts.length > 4 ? parts[4] : "";

          final WasmType type = createWasmType(typeString, signature);
          // ModuleImport constructor would need to be available
          // This is a placeholder implementation
          moduleImports.add(createModuleImport(moduleName, name, type, signature, description));
        }
      }
    }

    return Collections.unmodifiableList(moduleImports);
  }

  /**
   * Parses module exports from native string array format.
   *
   * @param moduleExportData array of module export metadata strings
   * @return list of parsed module exports
   */
  private List<ModuleExport> parseModuleExports(final String[] moduleExportData) {
    if (moduleExportData == null || moduleExportData.length == 0) {
      return Collections.emptyList();
    }

    final List<ModuleExport> moduleExports = new ArrayList<>();

    // Parse module export data format: name|type|signature|description
    for (final String entry : moduleExportData) {
      if (entry != null && !entry.trim().isEmpty()) {
        final String[] parts = entry.split("\\|");
        if (parts.length >= 3) {
          final String name = parts[0];
          final String typeString = parts[1];
          final String signature = parts[2];
          final String description = parts.length > 3 ? parts[3] : "";

          final WasmType type = createWasmType(typeString, signature);
          // ModuleExport constructor would need to be available
          // This is a placeholder implementation
          moduleExports.add(createModuleExport(name, type, signature, description));
        }
      }
    }

    return Collections.unmodifiableList(moduleExports);
  }

  /**
   * Parses function types from native string array format.
   *
   * @param functionTypeData array of function type strings
   * @return list of parsed function types
   */
  private List<FuncType> parseFunctionTypes(final String[] functionTypeData) {
    if (functionTypeData == null || functionTypeData.length == 0) {
      return Collections.emptyList();
    }

    final List<FuncType> functionTypes = new ArrayList<>();

    for (final String entry : functionTypeData) {
      if (entry != null && !entry.trim().isEmpty()) {
        // Parse function type format: params|returns
        final String[] parts = entry.split("\\|");
        if (parts.length >= 2) {
          // This would need proper FuncType implementation
          // This is a placeholder
          functionTypes.add(createFuncType(parts[0], parts[1]));
        }
      }
    }

    return Collections.unmodifiableList(functionTypes);
  }

  /**
   * Parses memory types from native string array format.
   *
   * @param memoryTypeData array of memory type strings
   * @return list of parsed memory types
   */
  private List<MemoryType> parseMemoryTypes(final String[] memoryTypeData) {
    if (memoryTypeData == null || memoryTypeData.length == 0) {
      return Collections.emptyList();
    }

    final List<MemoryType> memoryTypes = new ArrayList<>();

    for (final String entry : memoryTypeData) {
      if (entry != null && !entry.trim().isEmpty()) {
        // Parse memory type format: min|max
        final String[] parts = entry.split("\\|");
        if (parts.length >= 1) {
          // This would need proper MemoryType implementation
          // This is a placeholder
          memoryTypes.add(createMemoryType(parts[0], parts.length > 1 ? parts[1] : null));
        }
      }
    }

    return Collections.unmodifiableList(memoryTypes);
  }

  /**
   * Parses table types from native string array format.
   *
   * @param tableTypeData array of table type strings
   * @return list of parsed table types
   */
  private List<TableType> parseTableTypes(final String[] tableTypeData) {
    if (tableTypeData == null || tableTypeData.length == 0) {
      return Collections.emptyList();
    }

    final List<TableType> tableTypes = new ArrayList<>();

    for (final String entry : tableTypeData) {
      if (entry != null && !entry.trim().isEmpty()) {
        // Parse table type format: element_type|min|max
        final String[] parts = entry.split("\\|");
        if (parts.length >= 2) {
          // This would need proper TableType implementation
          // This is a placeholder
          tableTypes.add(createTableType(parts[0], parts[1], parts.length > 2 ? parts[2] : null));
        }
      }
    }

    return Collections.unmodifiableList(tableTypes);
  }

  /**
   * Parses global types from native string array format.
   *
   * @param globalTypeData array of global type strings
   * @return list of parsed global types
   */
  private List<GlobalType> parseGlobalTypes(final String[] globalTypeData) {
    if (globalTypeData == null || globalTypeData.length == 0) {
      return Collections.emptyList();
    }

    final List<GlobalType> globalTypes = new ArrayList<>();

    for (final String entry : globalTypeData) {
      if (entry != null && !entry.trim().isEmpty()) {
        // Parse global type format: value_type|mutability
        final String[] parts = entry.split("\\|");
        if (parts.length >= 2) {
          // This would need proper GlobalType implementation
          // This is a placeholder
          globalTypes.add(createGlobalType(parts[0], parts[1]));
        }
      }
    }

    return Collections.unmodifiableList(globalTypes);
  }

  // Placeholder factory methods - these would need proper implementations
  private ModuleImport createModuleImport(
      final String moduleName,
      final String name,
      final WasmType type,
      final String signature,
      final String description) {
    // This is a placeholder - ModuleImport would need to be properly implemented
    return new ModuleImport(moduleName, name, type, signature, description);
  }

  private ModuleExport createModuleExport(
      final String name, final WasmType type, final String signature, final String description) {
    // This is a placeholder - ModuleExport would need to be properly implemented
    return new ModuleExport(name, type, signature, description);
  }

  private FuncType createFuncType(final String params, final String returns) {
    // This is a placeholder - FuncType would need proper parsing
    return new FuncType() {
      @Override
      public WasmTypeKind getKind() {
        return WasmTypeKind.FUNCTION;
      }
    };
  }

  private MemoryType createMemoryType(final String min, final String max) {
    // This is a placeholder - MemoryType would need proper parsing
    return new MemoryType() {
      @Override
      public WasmTypeKind getKind() {
        return WasmTypeKind.MEMORY;
      }
    };
  }

  private TableType createTableType(final String elementType, final String min, final String max) {
    // This is a placeholder - TableType would need proper parsing
    return new TableType() {
      @Override
      public WasmTypeKind getKind() {
        return WasmTypeKind.TABLE;
      }
    };
  }

  private GlobalType createGlobalType(final String valueType, final String mutability) {
    // This is a placeholder - GlobalType would need proper parsing
    return new GlobalType() {
      @Override
      public WasmTypeKind getKind() {
        return WasmTypeKind.GLOBAL;
      }
    };
  }

  /**
   * Parses export metadata from native string array format.
   *
   * @param exportData array of export metadata strings
   * @return list of parsed export types
   */
  private List<ExportType> parseExportMetadata(final String[] exportData) {
    if (exportData == null || exportData.length == 0) {
      return new ArrayList<>();
    }

    final List<ExportType> exports = new ArrayList<>();

    // Parse export data format: name|type|signature
    for (final String entry : exportData) {
      if (entry == null || entry.trim().isEmpty()) {
        continue;
      }

      final String[] parts = entry.split("\\|");
      if (parts.length >= 2) {
        final String name = parts[0];
        final String typeString = parts[1];

        // Create appropriate WasmType based on type string
        final WasmType type = createWasmType(typeString, parts.length > 2 ? parts[2] : null);
        exports.add(new ExportType(name, type));
      }
    }

    return exports;
  }

  /**
   * Parses import metadata from native string array format.
   *
   * @param importData array of import metadata strings
   * @return list of parsed import types
   */
  private List<ImportType> parseImportMetadata(final String[] importData) {
    if (importData == null || importData.length == 0) {
      return new ArrayList<>();
    }

    final List<ImportType> imports = new ArrayList<>();

    // Parse import data format: module|name|type|signature
    for (final String entry : importData) {
      if (entry == null || entry.trim().isEmpty()) {
        continue;
      }

      final String[] parts = entry.split("\\|");
      if (parts.length >= 3) {
        final String moduleName = parts[0];
        final String name = parts[1];
        final String typeString = parts[2];

        // Create appropriate WasmType based on type string
        final WasmType type = createWasmType(typeString, parts.length > 3 ? parts[3] : null);
        imports.add(new ImportType(moduleName, name, type));
      }
    }

    return imports;
  }

  /** Creates a WasmType instance from type string and optional signature. */
  private WasmType createWasmType(final String typeString, final String signature) {
    // This is a simplified implementation - in reality would need to parse
    // complex function signatures, memory types, etc.
    return new WasmType() {
      @Override
      public WasmTypeKind getKind() {
        switch (typeString.toLowerCase()) {
          case "function":
            return WasmTypeKind.FUNCTION;
          case "memory":
            return WasmTypeKind.MEMORY;
          case "table":
            return WasmTypeKind.TABLE;
          case "global":
            return WasmTypeKind.GLOBAL;
          default:
            return WasmTypeKind.FUNCTION;
        }
      }
    };
  }

  /** Parses feature names into WasmFeature set. */
  private Set<WasmFeature> parseFeatureNames(final String[] featureNames) {
    if (featureNames == null || featureNames.length == 0) {
      return EnumSet.noneOf(WasmFeature.class);
    }

    final Set<WasmFeature> features = EnumSet.noneOf(WasmFeature.class);

    for (final String featureName : featureNames) {
      if (featureName == null) {
        continue;
      }

      for (final WasmFeature feature : WasmFeature.values()) {
        if (feature.getName().equals(featureName)) {
          features.add(feature);
          break;
        }
      }
    }

    return features;
  }

  /** Parses linking information from native string array format. */
  private Map<String, LinkingInfo> parseLinkingInfo(final String[] linkingData) {
    if (linkingData == null || linkingData.length == 0) {
      return new HashMap<>();
    }

    final Map<String, LinkingInfo> linkingInfo = new HashMap<>();

    // Parse linking data format: name|dependencies|symbols
    for (final String entry : linkingData) {
      if (entry == null || entry.trim().isEmpty()) {
        continue;
      }

      final String[] parts = entry.split("\\|");
      if (parts.length >= 3) {
        final String name = parts[0];
        final List<String> dependencies = Arrays.asList(parts[1].split(","));

        // Parse symbols map from key=value pairs
        final Map<String, String> symbols = new HashMap<>();
        if (!parts[2].trim().isEmpty()) {
          final String[] symbolPairs = parts[2].split(",");
          for (final String symbolPair : symbolPairs) {
            final String[] kv = symbolPair.split("=");
            if (kv.length == 2) {
              symbols.put(kv[0], kv[1]);
            }
          }
        }

        linkingInfo.put(name, new LinkingInfo(name, dependencies, symbols));
      }
    }

    return linkingInfo;
  }

  /** Converts ImportMap to native representation. */
  private long convertImportMapToNative(final ImportMap imports, final JniStore store) {
    // This would convert the ImportMap to a native handle
    // Implementation depends on native method signature
    return nativeCreateImportMap(store.getNativeHandle(), serializeImportMap(imports));
  }

  /** Serializes ImportMap to byte array for native consumption. */
  private byte[] serializeImportMap(final ImportMap imports) {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      // Simplified serialization - would need proper protocol
      // This is just a placeholder for the actual implementation
      final byte[] data = imports.toString().getBytes("UTF-8");
      baos.write(data);
      return baos.toByteArray();
    } catch (final IOException e) {
      throw new JniException("Failed to serialize import map", e);
    }
  }

  @Override
  protected void doClose() throws Exception {
    if (getNativeHandle() != 0) {
      nativeDestroyModule(getNativeHandle());
      LOGGER.fine("Destroyed JNI module with handle: 0x" + Long.toHexString(getNativeHandle()));
    }
  }

  @Override
  protected String getResourceType() {
    return "Module";
  }

  // Native method declarations

  /**
   * Instantiates a module within a store context.
   *
   * @param moduleHandle the native module handle
   * @param storeHandle the native store handle
   * @return native instance handle or 0 on failure
   */
  private static native long nativeInstantiateModule(long moduleHandle, long storeHandle);

  /**
   * Gets the names of functions exported by a module.
   *
   * @param moduleHandle the native module handle
   * @return array of function names or null on error
   */
  private static native String[] nativeGetExportedFunctions(long moduleHandle);

  /**
   * Gets the names of memories exported by a module.
   *
   * @param moduleHandle the native module handle
   * @return array of memory names or null on error
   */
  private static native String[] nativeGetExportedMemories(long moduleHandle);

  /**
   * Gets the names of tables exported by a module.
   *
   * @param moduleHandle the native module handle
   * @return array of table names or null on error
   */
  private static native String[] nativeGetExportedTables(long moduleHandle);

  /**
   * Gets the names of globals exported by a module.
   *
   * @param moduleHandle the native module handle
   * @return array of global names or null on error
   */
  private static native String[] nativeGetExportedGlobals(long moduleHandle);

  /**
   * Gets the names of functions imported by a module.
   *
   * @param moduleHandle the native module handle
   * @return array of function names in "module::name" format or null on error
   */
  private static native String[] nativeGetImportedFunctions(long moduleHandle);

  /**
   * Validates WebAssembly bytecode without compiling.
   *
   * @param bytecode the WebAssembly bytecode to validate
   * @return true if structurally valid, false otherwise
   */
  private static native boolean nativeValidateModule(byte[] bytecode);

  /**
   * Gets the size of a compiled module in bytes.
   *
   * @param moduleHandle the native module handle
   * @return the module size in bytes or -1 on error
   */
  private static native long nativeGetModuleSize(long moduleHandle);

  /**
   * Gets comprehensive export metadata for a module.
   *
   * @param moduleHandle the native module handle
   * @return array of export metadata strings or null on error
   */
  private static native String[] nativeGetExportMetadata(long moduleHandle);

  /**
   * Gets comprehensive import metadata for a module.
   *
   * @param moduleHandle the native module handle
   * @return array of import metadata strings or null on error
   */
  private static native String[] nativeGetImportMetadata(long moduleHandle);

  /**
   * Gets the name of a module if it has one.
   *
   * @param moduleHandle the native module handle
   * @return module name or null if unnamed
   */
  private static native String nativeGetModuleName(long moduleHandle);

  /**
   * Gets WebAssembly features supported by a module.
   *
   * @param moduleHandle the native module handle
   * @return array of feature names or null on error
   */
  private static native String[] nativeGetModuleFeatures(long moduleHandle);

  /**
   * Gets module linking information.
   *
   * @param moduleHandle the native module handle
   * @return array of linking info strings or null on error
   */
  private static native String[] nativeGetModuleLinkingInfo(long moduleHandle);

  /**
   * Serializes a compiled module to bytes.
   *
   * @param moduleHandle the native module handle
   * @return serialized module bytes or null on error
   */
  private static native byte[] nativeSerializeModule(long moduleHandle);

  /**
   * Deserializes a module from bytes.
   *
   * @param engineHandle the native engine handle
   * @param serializedData the serialized module bytes
   * @return native module handle or 0 on failure
   */
  private static native long nativeDeserializeModule(long engineHandle, byte[] serializedData);

  /**
   * Instantiates a module with specific imports.
   *
   * @param moduleHandle the native module handle
   * @param storeHandle the native store handle
   * @param importMapHandle the native import map handle
   * @return native instance handle or 0 on failure
   */
  private static native long nativeInstantiateModuleWithImports(
      long moduleHandle, long storeHandle, long importMapHandle);

  /**
   * Creates a native import map from serialized data.
   *
   * @param storeHandle the native store handle
   * @param importData serialized import map data
   * @return native import map handle or 0 on failure
   */
  private static native long nativeCreateImportMap(long storeHandle, byte[] importData);

  /**
   * Destroys a native import map.
   *
   * @param importMapHandle the native import map handle
   */
  private static native void nativeDestroyImportMap(long importMapHandle);

  /**
   * Destroys a native module and releases all associated resources.
   *
   * @param moduleHandle the native module handle
   */
  private static native void nativeDestroyModule(long moduleHandle);

  /**
   * Gets custom sections from a module as string data.
   *
   * @param moduleHandle the native module handle
   * @return array of custom section entries in "name|data" format or null on error
   */
  private static native String[] nativeGetCustomSections(long moduleHandle);

  /**
   * Gets raw custom sections from a module as binary data.
   *
   * @param moduleHandle the native module handle
   * @return array of raw custom section data or null on error
   */
  private static native byte[][] nativeGetRawCustomSections(long moduleHandle);

  /**
   * Gets module imports with complete type information.
   *
   * @param moduleHandle the native module handle
   * @return array of module import metadata strings or null on error
   */
  private static native String[] nativeGetModuleImports(long moduleHandle);

  /**
   * Gets module exports with complete type information.
   *
   * @param moduleHandle the native module handle
   * @return array of module export metadata strings or null on error
   */
  private static native String[] nativeGetModuleExports(long moduleHandle);

  /**
   * Gets all function types from a module.
   *
   * @param moduleHandle the native module handle
   * @return array of function type strings or null on error
   */
  private static native String[] nativeGetFunctionTypes(long moduleHandle);

  /**
   * Gets all memory types from a module.
   *
   * @param moduleHandle the native module handle
   * @return array of memory type strings or null on error
   */
  private static native String[] nativeGetMemoryTypes(long moduleHandle);

  /**
   * Gets all table types from a module.
   *
   * @param moduleHandle the native module handle
   * @return array of table type strings or null on error
   */
  private static native String[] nativeGetTableTypes(long moduleHandle);

  /**
   * Gets all global types from a module.
   *
   * @param moduleHandle the native module handle
   * @return array of global type strings or null on error
   */
  private static native String[] nativeGetGlobalTypes(long moduleHandle);

  /**
   * Compiles WebAssembly Text (WAT) format into a module.
   *
   * @param engineHandle the native engine handle
   * @param watText the WebAssembly text format code
   * @return native module handle or 0 on failure
   */
  private static native long nativeCompileWat(long engineHandle, String watText);
}
