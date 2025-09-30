package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the Linker interface.
 *
 * <p>This class provides a WebAssembly linker implementation using JNI calls to the native Wasmtime
 * library. The linker enables defining host functions and resolving imports before instantiating
 * WebAssembly modules.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 */
public final class JniLinker extends JniResource implements Linker {

  @Override
  protected String getResourceType() {
    return "Linker";
  }

  private static final Logger LOGGER = Logger.getLogger(JniLinker.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniLinker: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Flag to track if this linker has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Reference to the engine this linker was created for. */
  private final Engine engine;

  /**
   * Creates a new JNI linker with the given native handle and engine.
   *
   * @param nativeHandle the native linker handle
   * @param engine the engine this linker was created for
   * @throws IllegalArgumentException if nativeHandle is 0
   * @throws ai.tegmentum.wasmtime4j.jni.exception.JniValidationException if engine is null
   */
  JniLinker(final long nativeHandle, final Engine engine) {
    super(nativeHandle);
    JniValidation.requireNonNull(engine, "engine");
    this.engine = engine;
    LOGGER.fine("Created JNI linker with handle: " + nativeHandle);
  }

  /**
   * Creates a new linker for the given engine.
   *
   * @param engine the engine to create the linker for
   * @return a new JniLinker instance
   * @throws WasmException if linker creation fails
   * @throws ai.tegmentum.wasmtime4j.jni.exception.JniValidationException if engine is null
   */
  public static JniLinker create(final Engine engine) throws WasmException {
    JniValidation.requireNonNull(engine, "engine");

    if (!(engine instanceof JniEngine)) {
      throw new IllegalArgumentException("Engine must be a JNI engine instance");
    }

    final JniEngine jniEngine = (JniEngine) engine;
    final long nativeHandle = nativeCreate(jniEngine.getNativeHandle());
    if (nativeHandle == 0) {
      throw new WasmException("Failed to create native linker");
    }

    return new JniLinker(nativeHandle, engine);
  }

  @Override
  public void defineHostFunction(
      final String moduleName,
      final String name,
      final FunctionType functionType,
      final HostFunction implementation)
      throws WasmException {
    JniValidation.requireNonBlank(moduleName, "moduleName");
    JniValidation.requireNonBlank(name, "name");
    JniValidation.requireNonNull(functionType, "functionType");
    JniValidation.requireNonNull(implementation, "implementation");
    ensureNotClosed();

    try {
      // Create host function wrapper
      // Note: JniHostFunction constructor requires name, type, implementation and store
      // For linker context, we'll need to defer creation until instantiation
      // For now, we'll handle this in the native layer

      // Convert FunctionType parameters and returns to native representation
      final int[] paramTypesArray = convertToNativeTypes(functionType.getParamTypes());
      final int[] returnTypesArray = convertToNativeTypes(functionType.getReturnTypes());

      // Store host function implementation for later use during instantiation
      // The native layer will handle creating the actual host function binding
      final boolean success =
          nativeDefineHostFunction(
              getNativeHandle(),
              moduleName,
              name,
              paramTypesArray,
              returnTypesArray,
              implementation); // Pass the Java implementation directly

      if (!success) {
        throw new WasmException("Failed to define host function: " + moduleName + "::" + name);
      }

      LOGGER.fine("Defined host function " + moduleName + "::" + name);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error defining host function: " + e.getMessage(), e);
    }
  }

  @Override
  public void defineMemory(final String moduleName, final String name, final WasmMemory memory)
      throws WasmException {
    JniValidation.requireNonBlank(moduleName, "moduleName");
    JniValidation.requireNonBlank(name, "name");
    JniValidation.requireNonNull(memory, "memory");
    ensureNotClosed();

    try {
      if (!(memory instanceof JniMemory)) {
        throw new IllegalArgumentException("Memory must be a JNI memory instance");
      }

      final JniMemory jniMemory = (JniMemory) memory;
      final boolean success =
          nativeDefineMemory(getNativeHandle(), moduleName, name, jniMemory.getNativeHandle());

      if (!success) {
        throw new WasmException("Failed to define memory: " + moduleName + "::" + name);
      }

      LOGGER.fine("Defined memory " + moduleName + "::" + name);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error defining memory: " + e.getMessage(), e);
    }
  }

  @Override
  public void defineTable(final String moduleName, final String name, final WasmTable table)
      throws WasmException {
    JniValidation.requireNonBlank(moduleName, "moduleName");
    JniValidation.requireNonBlank(name, "name");
    JniValidation.requireNonNull(table, "table");
    ensureNotClosed();

    try {
      if (!(table instanceof JniTable)) {
        throw new IllegalArgumentException("Table must be a JNI table instance");
      }

      final JniTable jniTable = (JniTable) table;
      final boolean success =
          nativeDefineTable(getNativeHandle(), moduleName, name, jniTable.getNativeHandle());

      if (!success) {
        throw new WasmException("Failed to define table: " + moduleName + "::" + name);
      }

      LOGGER.fine("Defined table " + moduleName + "::" + name);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error defining table: " + e.getMessage(), e);
    }
  }

  @Override
  public void defineGlobal(final String moduleName, final String name, final WasmGlobal global)
      throws WasmException {
    JniValidation.requireNonBlank(moduleName, "moduleName");
    JniValidation.requireNonBlank(name, "name");
    JniValidation.requireNonNull(global, "global");
    ensureNotClosed();

    try {
      if (!(global instanceof JniGlobal)) {
        throw new IllegalArgumentException("Global must be a JNI global instance");
      }

      final JniGlobal jniGlobal = (JniGlobal) global;
      final boolean success =
          nativeDefineGlobal(getNativeHandle(), moduleName, name, jniGlobal.getNativeHandle());

      if (!success) {
        throw new WasmException("Failed to define global: " + moduleName + "::" + name);
      }

      LOGGER.fine("Defined global " + moduleName + "::" + name);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error defining global: " + e.getMessage(), e);
    }
  }

  @Override
  public void defineInstance(final String moduleName, final Instance instance)
      throws WasmException {
    JniValidation.requireNonBlank(moduleName, "moduleName");
    JniValidation.requireNonNull(instance, "instance");
    ensureNotClosed();

    try {
      if (!(instance instanceof JniInstance)) {
        throw new IllegalArgumentException("Instance must be a JNI instance");
      }

      final JniInstance jniInstance = (JniInstance) instance;
      final boolean success =
          nativeDefineInstance(getNativeHandle(), moduleName, jniInstance.getNativeHandle());

      if (!success) {
        throw new WasmException("Failed to define instance: " + moduleName);
      }

      LOGGER.fine("Defined instance for module " + moduleName);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error defining instance: " + e.getMessage(), e);
    }
  }

  @Override
  public void alias(
      final String fromModule, final String fromName, final String toModule, final String toName)
      throws WasmException {
    JniValidation.requireNonBlank(fromModule, "fromModule");
    JniValidation.requireNonBlank(fromName, "fromName");
    JniValidation.requireNonBlank(toModule, "toModule");
    JniValidation.requireNonBlank(toName, "toName");
    ensureNotClosed();

    try {
      final boolean success =
          nativeAlias(getNativeHandle(), fromModule, fromName, toModule, toName);

      if (!success) {
        throw new WasmException(
            "Failed to create alias: "
                + fromModule
                + "::"
                + fromName
                + " -> "
                + toModule
                + "::"
                + toName);
      }

      LOGGER.fine(
          "Created alias " + fromModule + "::" + fromName + " -> " + toModule + "::" + toName);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error creating alias: " + e.getMessage(), e);
    }
  }

  @Override
  public Instance instantiate(final Store store, final Module module) throws WasmException {
    JniValidation.requireNonNull(store, "store");
    JniValidation.requireNonNull(module, "module");
    ensureNotClosed();

    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JNI store instance");
      }
      if (!(module instanceof JniModule)) {
        throw new IllegalArgumentException("Module must be a JNI module instance");
      }

      final JniStore jniStore = (JniStore) store;
      final JniModule jniModule = (JniModule) module;

      final long instanceHandle =
          nativeInstantiate(
              getNativeHandle(), jniStore.getNativeHandle(), jniModule.getNativeHandle());

      if (instanceHandle == 0) {
        throw new WasmException("Failed to instantiate module");
      }

      final JniInstance instance = new JniInstance(instanceHandle, module, store);
      LOGGER.fine("Successfully instantiated module");
      return instance;
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error instantiating module: " + e.getMessage(), e);
    }
  }

  @Override
  public Instance instantiate(final Store store, final String moduleName, final Module module)
      throws WasmException {
    final Instance instance = instantiate(store, module);

    try {
      defineInstance(moduleName, instance);
      LOGGER.fine("Instantiated and registered module as '" + moduleName + "'");
      return instance;
    } catch (final WasmException e) {
      // If we can't register the instance, still return it but close it
      instance.close();
      throw e;
    }
  }

  @Override
  public void enableWasi() throws WasmException {
    ensureNotClosed();

    try {
      final boolean success = nativeEnableWasi(getNativeHandle());
      if (!success) {
        throw new WasmException("Failed to enable WASI");
      }

      LOGGER.fine("WASI support enabled");
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error enabling WASI: " + e.getMessage(), e);
    }
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public boolean isValid() {
    return !closed.get() && getNativeHandle() != 0;
  }

  @Override
  public boolean hasImport(final String moduleName, final String name) {
    JniValidation.requireNonBlank(moduleName, "moduleName");
    JniValidation.requireNonBlank(name, "name");
    ensureNotClosed();

    try {
      final int result = nativeHasImport(getNativeHandle(), moduleName, name);
      return result == 1;
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error checking import: " + e.getMessage(), e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.DependencyResolution resolveDependencies(
      final ai.tegmentum.wasmtime4j.Module... modules) throws WasmException {
    JniValidation.requireNonNull(modules, "modules");
    if (modules.length == 0) {
      throw new IllegalArgumentException("modules array cannot be empty");
    }
    ensureNotClosed();

    try {
      // Convert modules to native handles
      final long[] moduleHandles = new long[modules.length];
      for (int i = 0; i < modules.length; i++) {
        if (!(modules[i] instanceof JniModule)) {
          throw new IllegalArgumentException("All modules must be JNI module instances");
        }
        moduleHandles[i] = ((JniModule) modules[i]).getNativeHandle();
      }

      final long graphHandle = nativeResolveDependencies(getNativeHandle(), moduleHandles);
      if (graphHandle == 0) {
        throw new WasmException("Failed to resolve dependencies");
      }

      // Convert native dependency graph to Java objects
      final ai.tegmentum.wasmtime4j.DependencyResolution result =
          convertDependencyGraph(graphHandle, modules);

      // Clean up native graph
      nativeDestroyDependencyGraph(graphHandle);

      return result;
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error resolving dependencies: " + e.getMessage(), e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.ImportValidation validateImports(
      final ai.tegmentum.wasmtime4j.Module... modules) {
    JniValidation.requireNonNull(modules, "modules");
    if (modules.length == 0) {
      throw new IllegalArgumentException("modules array cannot be empty");
    }
    ensureNotClosed();

    try {
      // Convert modules to native handles
      final long[] moduleHandles = new long[modules.length];
      for (int i = 0; i < modules.length; i++) {
        if (!(modules[i] instanceof JniModule)) {
          throw new IllegalArgumentException("All modules must be JNI module instances");
        }
        moduleHandles[i] = ((JniModule) modules[i]).getNativeHandle();
      }

      final ValidationResult validationResult =
          nativeValidateImports(getNativeHandle(), moduleHandles);
      if (validationResult == null) {
        throw new WasmException("Failed to validate imports");
      }

      return convertValidationResult(validationResult, modules);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error validating imports: " + e.getMessage(), e);
    }
  }

  @Override
  public java.util.List<ai.tegmentum.wasmtime4j.ImportInfo> getImportRegistry() {
    ensureNotClosed();

    try {
      final ImportRegistryInfo[] registryArray = nativeGetImportRegistry(getNativeHandle());
      if (registryArray == null) {
        return java.util.Collections.emptyList();
      }

      return java.util.Arrays.stream(registryArray)
          .map(this::convertImportRegistryInfo)
          .collect(java.util.stream.Collectors.toList());
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting import registry: " + e.getMessage(), e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.InstantiationPlan createInstantiationPlan(
      final ai.tegmentum.wasmtime4j.Module... modules) throws WasmException {
    final ai.tegmentum.wasmtime4j.DependencyResolution resolution = resolveDependencies(modules);

    if (!resolution.isResolutionSuccessful()) {
      throw new WasmException("Cannot create instantiation plan: dependency resolution failed");
    }

    // Create instantiation steps based on the resolved order
    final java.util.List<ai.tegmentum.wasmtime4j.InstantiationStep> steps =
        new java.util.ArrayList<>();
    final java.util.List<ai.tegmentum.wasmtime4j.Module> instantiationOrder =
        resolution.getInstantiationOrder();

    for (int i = 0; i < instantiationOrder.size(); i++) {
      final ai.tegmentum.wasmtime4j.Module module = instantiationOrder.get(i);

      // Extract import/export information for this module
      final java.util.List<String> requiredImports = extractRequiredImports(module);
      final java.util.List<String> providedExports = extractProvidedExports(module);

      final String moduleName = module.getName();
      final String displayName = (moduleName != null && !moduleName.isEmpty()) ? moduleName : "unnamed module";
      final ai.tegmentum.wasmtime4j.InstantiationStep step =
          new ai.tegmentum.wasmtime4j.InstantiationStep(
              i + 1, // 1-based step number
              module,
              java.util.Optional.of("module_" + i), // Generate a default name
              requiredImports,
              providedExports,
              "Instantiate " + displayName);

      steps.add(step);
    }

    final ai.tegmentum.wasmtime4j.InstantiationPlan plan =
        new ai.tegmentum.wasmtime4j.InstantiationPlan(
            steps,
            resolution,
            resolution.getAnalysisTime(), // Use same duration for planning
            true // executable since resolution was successful
            );

    return plan;
  }

  @Override
  protected void doClose() {
    try {
      nativeDestroy(getNativeHandle());
      LOGGER.fine("Closed JNI linker");
    } catch (final Exception e) {
      LOGGER.warning("Error during linker cleanup: " + e.getMessage());
    }
  }

  /**
   * Converts WasmValueType array to native type representation.
   *
   * @param types the WasmValueType array to convert
   * @return array of native type constants
   */
  private int[] convertToNativeTypes(final WasmValueType[] types) {
    final int[] nativeTypes = new int[types.length];
    for (int i = 0; i < types.length; i++) {
      switch (types[i]) {
        case I32:
          nativeTypes[i] = 0;
          break;
        case I64:
          nativeTypes[i] = 1;
          break;
        case F32:
          nativeTypes[i] = 2;
          break;
        case F64:
          nativeTypes[i] = 3;
          break;
        case V128:
          nativeTypes[i] = 4;
          break;
        case FUNCREF:
          nativeTypes[i] = 5;
          break;
        case EXTERNREF:
          nativeTypes[i] = 6;
          break;
        default:
          throw new IllegalArgumentException("Unknown WebAssembly value type: " + types[i]);
      }
    }
    return nativeTypes;
  }

  /** Converts native dependency graph to Java DependencyResolution object. */
  private ai.tegmentum.wasmtime4j.DependencyResolution convertDependencyGraph(
      final long graphHandle, final ai.tegmentum.wasmtime4j.Module[] modules) {

    // For now, create a simplified version
    // In a full implementation, this would extract data from the native graph
    final java.util.List<ai.tegmentum.wasmtime4j.Module> instantiationOrder =
        java.util.Arrays.asList(modules);
    final java.util.List<ai.tegmentum.wasmtime4j.DependencyEdge> dependencies =
        java.util.Collections.emptyList();
    final java.time.Duration analysisTime = java.time.Duration.ofMillis(1);

    return new ai.tegmentum.wasmtime4j.DependencyResolution(
        instantiationOrder,
        dependencies,
        false, // hasCircularDependencies
        java.util.Collections.emptyList(), // circularDependencyChains
        modules.length,
        0, // resolvedDependencies
        analysisTime,
        true // resolutionSuccessful
        );
  }

  /** Converts native validation result to Java ImportValidation object. */
  private ai.tegmentum.wasmtime4j.ImportValidation convertValidationResult(
      final ValidationResult validationResult, final ai.tegmentum.wasmtime4j.Module[] modules) {

    final java.util.List<ai.tegmentum.wasmtime4j.ImportIssue> issues =
        java.util.Collections.emptyList();
    final java.util.List<ai.tegmentum.wasmtime4j.ImportInfo> validatedImports =
        java.util.Collections.emptyList();
    final java.time.Duration validationTime = java.time.Duration.ofMillis(1);

    return new ai.tegmentum.wasmtime4j.ImportValidation(
        validationResult.valid,
        issues,
        validatedImports,
        validationResult.totalImports,
        validationResult.validImports,
        validationTime);
  }

  /** Converts native import registry info to Java ImportInfo object. */
  private ai.tegmentum.wasmtime4j.ImportInfo convertImportRegistryInfo(
      final ImportRegistryInfo info) {
    final ai.tegmentum.wasmtime4j.ImportInfo.ImportType importType =
        convertNativeImportType(info.importType);

    return new ai.tegmentum.wasmtime4j.ImportInfo(
        info.moduleName,
        info.importName,
        importType,
        java.util.Optional.ofNullable(info.typeSignature),
        info.definedAt,
        info.isHostFunction,
        java.util.Optional.ofNullable(info.sourceDescription));
  }

  /** Converts native import type to Java ImportType enum. */
  private ai.tegmentum.wasmtime4j.ImportInfo.ImportType convertNativeImportType(
      final int nativeType) {
    switch (nativeType) {
      case 0:
        return ai.tegmentum.wasmtime4j.ImportInfo.ImportType.FUNCTION;
      case 1:
        return ai.tegmentum.wasmtime4j.ImportInfo.ImportType.MEMORY;
      case 2:
        return ai.tegmentum.wasmtime4j.ImportInfo.ImportType.TABLE;
      case 3:
        return ai.tegmentum.wasmtime4j.ImportInfo.ImportType.GLOBAL;
      case 4:
        return ai.tegmentum.wasmtime4j.ImportInfo.ImportType.INSTANCE;
      default:
        throw new IllegalArgumentException("Unknown native import type: " + nativeType);
    }
  }

  /** Extracts required imports from a module. */
  private java.util.List<String> extractRequiredImports(
      final ai.tegmentum.wasmtime4j.Module module) {
    // This would be implemented by querying the module's imports
    // For now, return empty list
    return java.util.Collections.emptyList();
  }

  /** Extracts provided exports from a module. */
  private java.util.List<String> extractProvidedExports(
      final ai.tegmentum.wasmtime4j.Module module) {
    // This would be implemented by querying the module's exports
    // For now, return empty list
    return java.util.Collections.emptyList();
  }

  // Helper classes for native data structures

  private static class ValidationResult {
    final boolean valid;
    final int totalImports;
    final int validImports;

    ValidationResult(final boolean valid, final int totalImports, final int validImports) {
      this.valid = valid;
      this.totalImports = totalImports;
      this.validImports = validImports;
    }
  }

  private static class ImportRegistryInfo {
    final String moduleName;
    final String importName;
    final int importType;
    final String typeSignature;
    final java.time.Instant definedAt;
    final boolean isHostFunction;
    final String sourceDescription;

    ImportRegistryInfo(
        final String moduleName,
        final String importName,
        final int importType,
        final String typeSignature,
        final java.time.Instant definedAt,
        final boolean isHostFunction,
        final String sourceDescription) {
      this.moduleName = moduleName;
      this.importName = importName;
      this.importType = importType;
      this.typeSignature = typeSignature;
      this.definedAt = definedAt;
      this.isHostFunction = isHostFunction;
      this.sourceDescription = sourceDescription;
    }
  }

  // Native method declarations

  /**
   * Creates a new native linker.
   *
   * @param engineHandle the native engine handle
   * @return the native linker handle, or 0 on failure
   */
  private static native long nativeCreate(final long engineHandle);

  /**
   * Defines a host function in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param moduleName the module name for the import
   * @param functionName the function name for the import
   * @param paramTypes array of parameter type constants
   * @param returnTypes array of return type constants
   * @param implementation the Java host function implementation
   * @return true if successful, false otherwise
   */
  private static native boolean nativeDefineHostFunction(
      final long linkerHandle,
      final String moduleName,
      final String functionName,
      final int[] paramTypes,
      final int[] returnTypes,
      final HostFunction implementation);

  /**
   * Defines a memory in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param moduleName the module name for the import
   * @param memoryName the memory name for the import
   * @param memoryHandle the native memory handle
   * @return true if successful, false otherwise
   */
  private static native boolean nativeDefineMemory(
      final long linkerHandle,
      final String moduleName,
      final String memoryName,
      final long memoryHandle);

  /**
   * Defines a table in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param moduleName the module name for the import
   * @param tableName the table name for the import
   * @param tableHandle the native table handle
   * @return true if successful, false otherwise
   */
  private static native boolean nativeDefineTable(
      final long linkerHandle,
      final String moduleName,
      final String tableName,
      final long tableHandle);

  /**
   * Defines a global in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param moduleName the module name for the import
   * @param globalName the global name for the import
   * @param globalHandle the native global handle
   * @return true if successful, false otherwise
   */
  private static native boolean nativeDefineGlobal(
      final long linkerHandle,
      final String moduleName,
      final String globalName,
      final long globalHandle);

  /**
   * Defines an instance in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param moduleName the module name for the import
   * @param instanceHandle the native instance handle
   * @return true if successful, false otherwise
   */
  private static native boolean nativeDefineInstance(
      final long linkerHandle, final String moduleName, final long instanceHandle);

  /**
   * Creates an alias in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param fromModule the source module name
   * @param fromName the source export name
   * @param toModule the destination module name
   * @param toName the destination export name
   * @return true if successful, false otherwise
   */
  private static native boolean nativeAlias(
      final long linkerHandle,
      final String fromModule,
      final String fromName,
      final String toModule,
      final String toName);

  /**
   * Instantiates a module using the native linker.
   *
   * @param linkerHandle the native linker handle
   * @param storeHandle the native store handle
   * @param moduleHandle the native module handle
   * @return the native instance handle, or 0 on failure
   */
  private static native long nativeInstantiate(
      final long linkerHandle, final long storeHandle, final long moduleHandle);

  /**
   * Enables WASI support in the native linker.
   *
   * @param linkerHandle the native linker handle
   * @return true if successful, false otherwise
   */
  private static native boolean nativeEnableWasi(final long linkerHandle);

  /**
   * Destroys the native linker.
   *
   * @param linkerHandle the native linker handle
   */
  private static native void nativeDestroy(final long linkerHandle);

  /**
   * Checks if linker has a specific import.
   *
   * @param linkerHandle the native linker handle
   * @param moduleName the module name
   * @param importName the import name
   * @return 1 if import exists, 0 otherwise, -1 on error
   */
  private static native int nativeHasImport(
      final long linkerHandle, final String moduleName, final String importName);

  /**
   * Resolves dependencies for a set of modules.
   *
   * @param linkerHandle the native linker handle
   * @param moduleHandles array of native module handles
   * @return handle to dependency graph, or 0 on failure
   */
  private static native long nativeResolveDependencies(
      final long linkerHandle, final long[] moduleHandles);

  /**
   * Validates imports for a set of modules.
   *
   * @param linkerHandle the native linker handle
   * @param moduleHandles array of native module handles
   * @return validation result object, or null on failure
   */
  private static native ValidationResult nativeValidateImports(
      final long linkerHandle, final long[] moduleHandles);

  /**
   * Gets the import registry information.
   *
   * @param linkerHandle the native linker handle
   * @return array of import registry info, or null on failure
   */
  private static native ImportRegistryInfo[] nativeGetImportRegistry(final long linkerHandle);

  /**
   * Destroys a native dependency graph.
   *
   * @param graphHandle the dependency graph handle
   */
  private static native void nativeDestroyDependencyGraph(final long graphHandle);
}
