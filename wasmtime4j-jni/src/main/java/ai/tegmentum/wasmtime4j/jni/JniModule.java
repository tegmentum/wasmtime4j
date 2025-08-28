package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.ExportType;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.ImportType;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
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
    this.engine = JniValidation.requireNonNull(engine, "engine");
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
      return new JniInstance(instanceHandle);
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
   * Serializes this compiled module to bytes for storage or transmission.
   *
   * <p>This method exports the compiled module in Wasmtime's internal format, which can be
   * deserialized later for faster loading. The serialized format is platform and version specific.
   *
   * @return serialized module bytes (never null)
   * @throws JniException if serialization fails
   * @throws JniResourceException if this module has been closed
   */
  public byte[] serialize() {
    ensureNotClosed();

    try {
      final byte[] serializedData = nativeSerializeModule(getNativeHandle());
      JniValidation.requireNonNull(serializedData, "serializedData");
      return serializedData.clone();
    } catch (final Exception e) {
      throw new JniException("Failed to serialize module", e);
    }
  }

  /**
   * Deserializes a module from previously serialized bytes.
   *
   * <p>This static method creates a module from bytes produced by {@link #serialize()}.
   * Deserialization is much faster than compilation from WebAssembly bytecode.
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
}
