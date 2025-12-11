package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.DependencyResolution;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.ImportInfo;
import ai.tegmentum.wasmtime4j.ImportIssue;
import ai.tegmentum.wasmtime4j.ImportValidation;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Linker.
 *
 * @param <T> the type of user data associated with stores
 * @since 1.0.0
 */
public final class PanamaLinker<T> implements ai.tegmentum.wasmtime4j.Linker<T> {
  private static final Logger LOGGER = Logger.getLogger(PanamaLinker.class.getName());
  private static final ConcurrentHashMap<Long, HostFunctionWrapper> HOST_FUNCTION_CALLBACKS =
      new ConcurrentHashMap<>();
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final PanamaEngine engine;
  private final Arena arena;
  private final MemorySegment nativeLinker;
  private volatile boolean closed = false;
  private final Set<String> imports = new HashSet<>();
  private final java.util.Map<String, ai.tegmentum.wasmtime4j.ImportInfo> importRegistry =
      new java.util.concurrent.ConcurrentHashMap<>();
  private final Set<Long> registeredCallbackIds = new HashSet<>();
  private volatile PanamaWasiContext wasiContext = null;

  /**
   * Creates a new Panama linker.
   *
   * @param engine the engine to create the linker for
   * @throws WasmException if linker creation fails
   */
  public PanamaLinker(final PanamaEngine engine) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    this.engine = engine;
    this.arena = Arena.ofShared();

    // Create native linker via Panama FFI
    final MemorySegment enginePtr = engine.getNativeEngine();
    if (enginePtr == null || enginePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Engine has invalid native handle");
    }

    this.nativeLinker = NATIVE_BINDINGS.panamaLinkerCreate(enginePtr);
    if (this.nativeLinker == null || this.nativeLinker.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to create native linker");
    }

    LOGGER.fine("Created Panama linker");
  }

  @Override
  public void defineHostFunction(
      final String moduleName,
      final String name,
      final FunctionType functionType,
      final HostFunction implementation)
      throws WasmException {
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (functionType == null) {
      throw new IllegalArgumentException("Function type cannot be null");
    }
    if (implementation == null) {
      throw new IllegalArgumentException("Implementation cannot be null");
    }
    ensureNotClosed();

    // Convert FunctionType to native representation
    final int[] paramTypes = toNativeTypes(functionType.getParamTypes());
    final int[] returnTypes = toNativeTypes(functionType.getReturnTypes());

    // Register callback and get ID
    final long callbackId =
        registerHostFunctionCallback(moduleName, name, implementation, functionType);

    try {
      // Create upcall stub for the callback function
      final MemorySegment callbackStub = createCallbackStub();

      // Allocate native memory for strings and arrays
      final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
      final MemorySegment namePtr = arena.allocateFrom(name);

      // Allocate and copy parameter types
      final MemorySegment paramTypesPtr = arena.allocate(ValueLayout.JAVA_INT, paramTypes.length);
      for (int i = 0; i < paramTypes.length; i++) {
        paramTypesPtr.setAtIndex(ValueLayout.JAVA_INT, i, paramTypes[i]);
      }

      // Allocate and copy return types
      final MemorySegment returnTypesPtr = arena.allocate(ValueLayout.JAVA_INT, returnTypes.length);
      for (int i = 0; i < returnTypes.length; i++) {
        returnTypesPtr.setAtIndex(ValueLayout.JAVA_INT, i, returnTypes[i]);
      }

      // Call native function to define host function
      final int result =
          NATIVE_BINDINGS.panamaLinkerDefineHostFunction(
              nativeLinker,
              moduleNamePtr,
              namePtr,
              paramTypesPtr,
              paramTypes.length,
              returnTypesPtr,
              returnTypes.length,
              callbackStub,
              callbackId);

      if (result != 0) {
        throw new WasmException("Failed to define host function: " + moduleName + "::" + name);
      }

      addImportWithMetadata(
          moduleName,
          name,
          ai.tegmentum.wasmtime4j.ImportInfo.ImportType.FUNCTION,
          functionType.toString());

      LOGGER.fine(
          "Defined host function: "
              + moduleName
              + "::"
              + name
              + " (callback ID: "
              + callbackId
              + ")");
    } catch (final Exception e) {
      // Unregister callback on failure
      HOST_FUNCTION_CALLBACKS.remove(callbackId);
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Error defining host function: " + moduleName + "::" + name, e);
    }
  }

  @Override
  public void defineMemory(
      final Store store, final String moduleName, final String name, final WasmMemory memory)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }
    ensureNotClosed();

    // Ensure we have Panama implementations
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("Memory must be a PanamaMemory");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaMemory panamaMemory = (PanamaMemory) memory;

    // Allocate C strings for module name and memory name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
    final MemorySegment namePtr = arena.allocateFrom(name);

    // Call native function to define memory
    final int result =
        NATIVE_BINDINGS.panamaLinkerDefineMemory(
            nativeLinker,
            panamaStore.getNativeStore(),
            moduleNamePtr,
            namePtr,
            panamaMemory.getNativeMemory());

    if (result != 0) {
      throw new WasmException(
          "Failed to define memory: " + moduleName + "::" + name + " (error code: " + result + ")");
    }

    LOGGER.fine("Defined memory: " + moduleName + "::" + name);
  }

  @Override
  public void defineTable(
      final Store store, final String moduleName, final String name, final WasmTable table)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    if (table == null) {
      throw new IllegalArgumentException("Table cannot be null");
    }
    ensureNotClosed();

    // Ensure we have Panama implementations
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(table instanceof PanamaTable)) {
      throw new IllegalArgumentException("Table must be a PanamaTable");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaTable panamaTable = (PanamaTable) table;

    // Allocate C strings for module name and table name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
    final MemorySegment namePtr = arena.allocateFrom(name);

    // Call native function to define table
    final int result =
        NATIVE_BINDINGS.panamaLinkerDefineTable(
            nativeLinker,
            panamaStore.getNativeStore(),
            moduleNamePtr,
            namePtr,
            panamaTable.getNativeTable());

    if (result != 0) {
      throw new WasmException(
          "Failed to define table: " + moduleName + "::" + name + " (error code: " + result + ")");
    }

    LOGGER.fine("Defined table: " + moduleName + "::" + name);
  }

  @Override
  public void defineGlobal(
      final Store store, final String moduleName, final String name, final WasmGlobal global)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    if (global == null) {
      throw new IllegalArgumentException("Global cannot be null");
    }
    ensureNotClosed();

    // Ensure we have Panama implementations
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(global instanceof PanamaGlobal)) {
      throw new IllegalArgumentException("Global must be a PanamaGlobal");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaGlobal panamaGlobal = (PanamaGlobal) global;

    // Allocate C strings for module name and global name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
    final MemorySegment namePtr = arena.allocateFrom(name);

    // Call native function to define global
    final int result =
        NATIVE_BINDINGS.panamaLinkerDefineGlobal(
            nativeLinker,
            panamaStore.getNativeStore(),
            moduleNamePtr,
            namePtr,
            panamaGlobal.getNativeGlobal());

    if (result != 0) {
      throw new WasmException(
          "Failed to define global: " + moduleName + "::" + name + " (error code: " + result + ")");
    }

    LOGGER.fine("Defined global: " + moduleName + "::" + name);
  }

  @Override
  public void defineInstance(final String moduleName, final Instance instance)
      throws WasmException {
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    ensureNotClosed();

    // Ensure we have Panama implementation
    if (!(instance instanceof PanamaInstance)) {
      throw new IllegalArgumentException("Instance must be a PanamaInstance");
    }

    final PanamaInstance panamaInstance = (PanamaInstance) instance;

    // Get the store from the instance
    final Store store = panamaInstance.getStore();
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    final PanamaStore panamaStore = (PanamaStore) store;

    // Allocate C string for module name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);

    // Call native function to define instance
    final int result =
        NATIVE_BINDINGS.panamaLinkerDefineInstance(
            nativeLinker,
            panamaStore.getNativeStore(),
            moduleNamePtr,
            panamaInstance.getNativeInstance());

    if (result != 0) {
      throw new WasmException(
          "Failed to define instance: " + moduleName + " (error code: " + result + ")");
    }

    LOGGER.fine("Defined instance: " + moduleName);
  }

  @Override
  public void alias(
      final String fromModule, final String fromName, final String toModule, final String toName)
      throws WasmException {
    if (fromModule == null) {
      throw new IllegalArgumentException("From module cannot be null");
    }
    if (fromName == null) {
      throw new IllegalArgumentException("From name cannot be null");
    }
    if (toModule == null) {
      throw new IllegalArgumentException("To module cannot be null");
    }
    if (toName == null) {
      throw new IllegalArgumentException("To name cannot be null");
    }
    ensureNotClosed();

    // Allocate C strings
    final MemorySegment fromModulePtr = arena.allocateFrom(fromModule);
    final MemorySegment fromNamePtr = arena.allocateFrom(fromName);
    final MemorySegment toModulePtr = arena.allocateFrom(toModule);
    final MemorySegment toNamePtr = arena.allocateFrom(toName);

    // Call native function to create alias
    final int result =
        NATIVE_BINDINGS.panamaLinkerAlias(
            nativeLinker, fromModulePtr, fromNamePtr, toModulePtr, toNamePtr);

    if (result != 0) {
      throw new WasmException(
          "Failed to create alias from "
              + fromModule
              + "::"
              + fromName
              + " to "
              + toModule
              + "::"
              + toName
              + " (error code: "
              + result
              + ")");
    }

    LOGGER.fine(
        "Created alias from " + fromModule + "::" + fromName + " to " + toModule + "::" + toName);
  }

  @Override
  public Instance instantiate(final Store store, final Module module) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    // Ensure we have Panama implementations
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaModule panamaModule = (PanamaModule) module;

    // If we have a WASI context, attach it to the store before instantiation
    if (wasiContext != null) {
      final int hasWasi = NATIVE_BINDINGS.storeHasWasiContext(panamaStore.getNativeStore());
      if (hasWasi == 0) {
        // Store doesn't have WASI context yet, attach it
        final int result =
            NATIVE_BINDINGS.storeSetWasiContext(
                panamaStore.getNativeStore(), wasiContext.getNativeContext());
        if (result != 0) {
          throw new WasmException(
              "Failed to attach WASI context to store (error code: " + result + ")");
        }
        LOGGER.fine("Attached WASI context to store before instantiation");
      }
    }

    // Call native function to instantiate module using linker
    final MemorySegment instancePtr =
        NATIVE_BINDINGS.panamaLinkerInstantiate(
            nativeLinker, panamaStore.getNativeStore(), panamaModule.getNativeModule());

    if (instancePtr == null || instancePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to instantiate module via linker");
    }

    // Wrap the native instance pointer
    return new PanamaInstance(instancePtr, panamaModule, panamaStore);
  }

  @Override
  public Instance instantiate(final Store store, final String moduleName, final Module module)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    // Ensure we have Panama implementations
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaModule panamaModule = (PanamaModule) module;

    // Instantiate the module using the linker
    final Instance instance = instantiate(store, module);

    // Define the instance in the linker under the specified module name
    // This allows other modules to import from this instance
    defineInstance(moduleName, instance);

    LOGGER.fine("Instantiated and registered module: " + moduleName);

    return instance;
  }

  @Override
  public ai.tegmentum.wasmtime4j.InstancePre instantiatePre(final Module module)
      throws WasmException {
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule for Panama linker");
    }

    final PanamaModule panamaModule = (PanamaModule) module;

    // Call native function to create InstancePre
    final MemorySegment instancePrePtr =
        NATIVE_BINDINGS.linkerInstantiatePre(nativeLinker, panamaModule.getNativeModule());

    if (instancePrePtr == null || instancePrePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to create InstancePre for module");
    }

    LOGGER.fine("Created InstancePre for module");

    return new PanamaInstancePre(instancePrePtr, module, engine);
  }

  @Override
  public void enableWasi() throws WasmException {
    ensureNotClosed();

    // Add WASI Preview 1 imports to the linker
    final int result = NATIVE_BINDINGS.linkerAddWasi(nativeLinker);

    if (result != 0) {
      throw new WasmException("Failed to enable WASI (error code: " + result + ")");
    }

    LOGGER.fine("Enabled WASI for linker");
  }

  /**
   * Sets the WASI context for this linker.
   *
   * <p>The WASI context will be automatically attached to the store during instantiation.
   *
   * @param wasiCtx the WASI context to use
   */
  public void setWasiContext(final PanamaWasiContext wasiCtx) {
    this.wasiContext = wasiCtx;
  }

  /**
   * Gets the WASI context set on this linker.
   *
   * @return the WASI context, or null if not set
   */
  public PanamaWasiContext getWasiContext() {
    return this.wasiContext;
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public boolean isValid() {
    return !closed;
  }

  @Override
  public boolean hasImport(final String moduleName, final String name) {
    if (moduleName == null || moduleName.isEmpty()) {
      throw new IllegalArgumentException("Module name cannot be null or empty");
    }
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Import name cannot be null or empty");
    }
    ensureNotClosed();
    return imports.contains(moduleName + "::" + name);
  }

  @Override
  public DependencyResolution resolveDependencies(final Module... modules) throws WasmException {
    if (modules == null) {
      throw new IllegalArgumentException("Modules cannot be null");
    }
    if (modules.length == 0) {
      throw new IllegalArgumentException("At least one module must be provided");
    }
    ensureNotClosed();

    final long startTime = System.nanoTime();

    try {
      // Step 1: Build maps of imports and exports
      final java.util.Map<String, Module> exportProviders = new java.util.HashMap<>();
      final java.util.Map<Module, java.util.List<ai.tegmentum.wasmtime4j.ImportType>>
          moduleImports = new java.util.HashMap<>();
      final java.util.List<ai.tegmentum.wasmtime4j.DependencyEdge> dependencies =
          new java.util.ArrayList<>();

      // Collect exports from all modules
      // Note: Exports don't have module names - they're provided by the module itself
      // We map export names to the providing module
      for (final Module module : modules) {
        final java.util.List<ai.tegmentum.wasmtime4j.ExportType> exports = module.getExports();
        for (final ai.tegmentum.wasmtime4j.ExportType export : exports) {
          // Just use the export name as the key
          exportProviders.put(export.getName(), module);
        }
      }

      // Analyze imports for each module
      int resolvedCount = 0;
      for (final Module module : modules) {
        final java.util.List<ai.tegmentum.wasmtime4j.ImportType> imports = module.getImports();
        moduleImports.put(module, imports);

        for (final ai.tegmentum.wasmtime4j.ImportType importType : imports) {
          // Import has both a module name (like "env") and a field name (like "memory")
          // We need to check if the linker or another module provides this
          final boolean resolvedByLinker =
              hasImport(importType.getModuleName(), importType.getName());
          final Module provider = exportProviders.get(importType.getName());
          final boolean resolvedByModule = provider != null;
          final boolean resolved = resolvedByLinker || resolvedByModule;

          if (resolved) {
            resolvedCount++;
          }

          // Create dependency edge
          if (resolvedByModule) {
            final ai.tegmentum.wasmtime4j.DependencyEdge.DependencyType depType =
                mapImportTypeToDependencyType(importType);
            dependencies.add(
                new ai.tegmentum.wasmtime4j.DependencyEdge(
                    module,
                    provider,
                    importType.getModuleName(),
                    importType.getName(),
                    depType,
                    true));
          }
        }
      }

      // Step 2: Detect circular dependencies
      final CircularDependencyResult circularResult = detectCircularDependencies(dependencies);

      // Step 3: Perform topological sort for instantiation order
      final java.util.List<Module> instantiationOrder =
          circularResult.hasCircular
              ? new java.util.ArrayList<>()
              : topologicalSort(modules, dependencies);

      // Step 4: Build result
      final long endTime = System.nanoTime();
      final java.time.Duration analysisTime = java.time.Duration.ofNanos(endTime - startTime);

      final boolean successful =
          !circularResult.hasCircular && resolvedCount == countTotalImports(moduleImports);

      return new ai.tegmentum.wasmtime4j.DependencyResolution(
          instantiationOrder,
          dependencies,
          circularResult.hasCircular,
          circularResult.chains,
          modules.length,
          resolvedCount,
          analysisTime,
          successful);

    } catch (final Exception e) {
      throw new WasmException("Failed to resolve dependencies: " + e.getMessage(), e);
    }
  }

  @Override
  public ImportValidation validateImports(final Module... modules) {
    if (modules == null) {
      throw new IllegalArgumentException("Modules cannot be null");
    }
    if (modules.length == 0) {
      throw new IllegalArgumentException("At least one module must be provided");
    }
    ensureNotClosed();

    final long startTime = System.nanoTime();

    final List<ImportIssue> issues = new ArrayList<>();
    final List<ImportInfo> validatedImports = new ArrayList<>();

    int totalImports = 0;
    int validImports = 0;

    // Validate each module's imports
    for (final Module module : modules) {
      final List<ai.tegmentum.wasmtime4j.ImportType> moduleImports = module.getImports();
      totalImports += moduleImports.size();

      for (final ai.tegmentum.wasmtime4j.ImportType importType : moduleImports) {
        final String moduleName = importType.getModuleName();
        final String importName = importType.getName();

        // Check if import is defined in linker
        final boolean isDefined = hasImport(moduleName, importName);

        if (isDefined) {
          validImports++;

          // Create ImportInfo for valid import
          final ImportInfo.ImportType infoType = mapImportTypeToInfoType(importType);
          final Optional<String> typeSignature = Optional.of(importType.getType().toString());

          final ImportInfo info =
              new ImportInfo(
                  moduleName,
                  importName,
                  infoType,
                  typeSignature,
                  java.time.Instant.now(),
                  true, // All imports in linker are host functions in this simplified
                  // implementation
                  Optional.of("Defined in linker"));

          validatedImports.add(info);
        } else {
          // Create ImportIssue for missing import
          final ImportIssue issue =
              new ImportIssue(
                  ImportIssue.Severity.ERROR,
                  ImportIssue.Type.MISSING_IMPORT,
                  moduleName,
                  importName,
                  "Import not defined in linker",
                  importType.getType().toString(),
                  null);

          issues.add(issue);
        }
      }
    }

    final long endTime = System.nanoTime();
    final Duration validationTime = Duration.ofNanos(endTime - startTime);

    final boolean valid = issues.isEmpty();

    return new ImportValidation(
        valid, issues, validatedImports, totalImports, validImports, validationTime);
  }

  /**
   * Maps ImportType to ImportInfo.ImportType.
   *
   * @param importType the import type from module
   * @return the corresponding ImportInfo.ImportType
   */
  private ImportInfo.ImportType mapImportTypeToInfoType(
      final ai.tegmentum.wasmtime4j.ImportType importType) {
    final WasmTypeKind kind = importType.getType().getKind();

    switch (kind) {
      case FUNCTION:
        return ImportInfo.ImportType.FUNCTION;
      case MEMORY:
        return ImportInfo.ImportType.MEMORY;
      case TABLE:
        return ImportInfo.ImportType.TABLE;
      case GLOBAL:
        return ImportInfo.ImportType.GLOBAL;
      default:
        // Default to FUNCTION if we can't determine
        return ImportInfo.ImportType.FUNCTION;
    }
  }

  @Override
  public List<ImportInfo> getImportRegistry() {
    ensureNotClosed();
    return new ArrayList<>(importRegistry.values());
  }

  /**
   * Maps an ImportType to a DependencyType.
   *
   * @param importType the import type
   * @return the corresponding dependency type
   */
  private ai.tegmentum.wasmtime4j.DependencyEdge.DependencyType mapImportTypeToDependencyType(
      final ai.tegmentum.wasmtime4j.ImportType importType) {
    // For now, we infer from the import type string
    // A more robust implementation would use actual type inspection
    return ai.tegmentum.wasmtime4j.DependencyEdge.DependencyType.FUNCTION;
  }

  /**
   * Counts total imports across all modules.
   *
   * @param moduleImports map of modules to their imports
   * @return total import count
   */
  private int countTotalImports(
      final java.util.Map<Module, java.util.List<ai.tegmentum.wasmtime4j.ImportType>>
          moduleImports) {
    return moduleImports.values().stream().mapToInt(java.util.List::size).sum();
  }

  /**
   * Detects circular dependencies in the dependency graph.
   *
   * @param dependencies list of dependency edges
   * @return circular dependency detection result
   */
  private CircularDependencyResult detectCircularDependencies(
      final java.util.List<ai.tegmentum.wasmtime4j.DependencyEdge> dependencies) {
    final java.util.Map<Module, java.util.Set<Module>> graph = new java.util.HashMap<>();
    final java.util.Set<Module> visited = new java.util.HashSet<>();
    final java.util.Set<Module> recursionStack = new java.util.HashSet<>();
    final java.util.List<String> chains = new java.util.ArrayList<>();

    // Build adjacency list
    for (final ai.tegmentum.wasmtime4j.DependencyEdge edge : dependencies) {
      graph
          .computeIfAbsent(edge.getDependent(), k -> new java.util.HashSet<>())
          .add(edge.getDependency());
    }

    // DFS to detect cycles
    for (final Module module : graph.keySet()) {
      if (!visited.contains(module)) {
        final java.util.List<Module> path = new java.util.ArrayList<>();
        if (hasCycleDFS(module, graph, visited, recursionStack, path, chains)) {
          return new CircularDependencyResult(true, chains);
        }
      }
    }

    return new CircularDependencyResult(false, java.util.Collections.emptyList());
  }

  /**
   * DFS helper for cycle detection.
   *
   * @param node current node
   * @param graph dependency graph
   * @param visited visited nodes
   * @param recursionStack current recursion stack
   * @param path current path
   * @param chains detected cycle descriptions
   * @return true if cycle detected
   */
  private boolean hasCycleDFS(
      final Module node,
      final java.util.Map<Module, java.util.Set<Module>> graph,
      final java.util.Set<Module> visited,
      final java.util.Set<Module> recursionStack,
      final java.util.List<Module> path,
      final java.util.List<String> chains) {
    visited.add(node);
    recursionStack.add(node);
    path.add(node);

    final java.util.Set<Module> neighbors =
        graph.getOrDefault(node, java.util.Collections.emptySet());
    for (final Module neighbor : neighbors) {
      if (!visited.contains(neighbor)) {
        if (hasCycleDFS(neighbor, graph, visited, recursionStack, path, chains)) {
          return true;
        }
      } else if (recursionStack.contains(neighbor)) {
        // Found a cycle
        final int cycleStart = path.indexOf(neighbor);
        final StringBuilder chain = new StringBuilder();
        for (int i = cycleStart; i < path.size(); i++) {
          chain.append("Module");
          if (i < path.size() - 1) {
            chain.append(" -> ");
          }
        }
        chain.append(" -> Module");
        chains.add(chain.toString());
        return true;
      }
    }

    path.remove(path.size() - 1);
    recursionStack.remove(node);
    return false;
  }

  /**
   * Performs topological sort to determine instantiation order.
   *
   * @param modules array of modules
   * @param dependencies list of dependency edges
   * @return ordered list of modules
   */
  private java.util.List<Module> topologicalSort(
      final Module[] modules,
      final java.util.List<ai.tegmentum.wasmtime4j.DependencyEdge> dependencies) {
    final java.util.Map<Module, Integer> inDegree = new java.util.HashMap<>();
    final java.util.Map<Module, java.util.List<Module>> graph = new java.util.HashMap<>();

    // Initialize
    for (final Module module : modules) {
      inDegree.put(module, 0);
      graph.put(module, new java.util.ArrayList<>());
    }

    // Build graph and calculate in-degrees
    for (final ai.tegmentum.wasmtime4j.DependencyEdge edge : dependencies) {
      final Module dependent = edge.getDependent();
      final Module dependency = edge.getDependency();

      graph.get(dependency).add(dependent);
      inDegree.put(dependent, inDegree.get(dependent) + 1);
    }

    // Queue modules with no dependencies
    final java.util.Queue<Module> queue = new java.util.LinkedList<>();
    for (final Module module : modules) {
      if (inDegree.get(module) == 0) {
        queue.offer(module);
      }
    }

    // Process queue
    final java.util.List<Module> result = new java.util.ArrayList<>();
    while (!queue.isEmpty()) {
      final Module current = queue.poll();
      result.add(current);

      for (final Module dependent : graph.get(current)) {
        final int newInDegree = inDegree.get(dependent) - 1;
        inDegree.put(dependent, newInDegree);
        if (newInDegree == 0) {
          queue.offer(dependent);
        }
      }
    }

    return result;
  }

  /** Helper class for circular dependency detection results. */
  private static final class CircularDependencyResult {
    final boolean hasCircular;
    final java.util.List<String> chains;

    CircularDependencyResult(final boolean hasCircular, final java.util.List<String> chains) {
      this.hasCircular = hasCircular;
      this.chains = chains;
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    try {
      cleanupHostFunctionCallbacks();
      // Destroy native linker
      if (nativeLinker != null && !nativeLinker.equals(MemorySegment.NULL)) {
        NATIVE_BINDINGS.panamaLinkerDestroy(nativeLinker);
      }
      arena.close();
      closed = true;
      LOGGER.fine("Closed Panama linker");
    } catch (final Exception e) {
      LOGGER.warning("Error closing linker: " + e.getMessage());
    }
  }

  /** Cleans up host function callbacks registered by this linker instance. */
  private void cleanupHostFunctionCallbacks() {
    for (final Long callbackId : registeredCallbackIds) {
      HOST_FUNCTION_CALLBACKS.remove(callbackId);
    }
    registeredCallbackIds.clear();
  }

  /**
   * Gets the native linker pointer.
   *
   * @return native linker memory segment
   */
  public MemorySegment getNativeLinker() {
    return nativeLinker;
  }

  /**
   * Creates an upcall stub for the host function callback.
   *
   * @return memory segment pointing to the callback function
   */
  private MemorySegment createCallbackStub() {
    try {
      // Define the function descriptor for the callback
      // int callback(long callbackId, void* paramsPtr, int paramsLen, void* resultsPtr, int
      // resultsLen)
      final FunctionDescriptor callbackDescriptor =
          FunctionDescriptor.of(
              ValueLayout.JAVA_INT, // return int
              ValueLayout.JAVA_LONG, // callbackId
              ValueLayout.ADDRESS, // paramsPtr
              ValueLayout.JAVA_INT, // paramsLen
              ValueLayout.ADDRESS, // resultsPtr
              ValueLayout.JAVA_INT); // resultsLen

      // Get the method handle for invokeHostFunctionCallback
      final java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup();
      final java.lang.invoke.MethodHandle callbackHandle =
          lookup.findStatic(
              PanamaLinker.class,
              "invokeHostFunctionCallback",
              java.lang.invoke.MethodType.methodType(
                  int.class,
                  long.class,
                  MemorySegment.class,
                  int.class,
                  MemorySegment.class,
                  int.class));

      // Create the upcall stub
      final java.lang.foreign.Linker nativeLinker = java.lang.foreign.Linker.nativeLinker();
      return nativeLinker.upcallStub(callbackHandle, callbackDescriptor, arena);

    } catch (final Exception e) {
      throw new IllegalStateException("Failed to create callback upcall stub", e);
    }
  }

  /**
   * Adds an import to the registry with full metadata.
   *
   * @param moduleName the module name
   * @param name the import name
   * @param importType the import type
   * @param typeSignature the type signature (optional)
   */
  void addImportWithMetadata(
      final String moduleName,
      final String name,
      final ai.tegmentum.wasmtime4j.ImportInfo.ImportType importType,
      final String typeSignature) {
    imports.add(moduleName + "::" + name);
    final String key = moduleName + "::" + name;
    final ai.tegmentum.wasmtime4j.ImportInfo info =
        new ai.tegmentum.wasmtime4j.ImportInfo(
            moduleName,
            name,
            importType,
            java.util.Optional.ofNullable(typeSignature),
            java.time.Instant.now(),
            true, // All imports registered via define* methods are host-provided
            java.util.Optional.of("Host-provided import"));
    importRegistry.put(key, info);
  }

  /**
   * Adds an import to the registry for tracking purposes.
   *
   * @param moduleName the module name
   * @param name the import name
   */
  void addImport(final String moduleName, final String name) {
    imports.add(moduleName + "::" + name);
  }

  /**
   * Ensures the linker is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Linker has been closed");
    }
  }

  /**
   * Converts WasmValueTypes to native type codes.
   *
   * @param types the value types
   * @return array of native type codes
   */
  private int[] toNativeTypes(final WasmValueType[] types) {
    if (types == null || types.length == 0) {
      return new int[0];
    }

    final int[] nativeTypes = new int[types.length];
    for (int i = 0; i < types.length; i++) {
      nativeTypes[i] = types[i].toNativeTypeCode();
    }
    return nativeTypes;
  }

  /**
   * Registers a host function callback.
   *
   * @param moduleName the module name
   * @param name the function name
   * @param implementation the implementation
   * @param functionType the function type
   * @return callback ID for native code to invoke
   */
  private long registerHostFunctionCallback(
      final String moduleName,
      final String name,
      final HostFunction implementation,
      final FunctionType functionType) {
    final HostFunctionWrapper wrapper =
        new HostFunctionWrapper(moduleName, name, implementation, functionType);
    final long id = wrapper.getId();
    HOST_FUNCTION_CALLBACKS.put(id, wrapper);
    registeredCallbackIds.add(id);
    return id;
  }

  /**
   * Callback function invoked from native code when a host function is called.
   *
   * <p>This method is called via Panama FFI function pointer.
   *
   * @param callbackId the callback ID
   * @param paramsPtr pointer to parameter array
   * @param paramsLen number of parameters
   * @param resultsPtr pointer to results buffer
   * @param resultsLen expected number of results
   * @return 0 on success, non-zero on error
   */
  @SuppressWarnings("unused") // Called from native code via function pointer
  private static int invokeHostFunctionCallback(
      final long callbackId,
      final MemorySegment paramsPtr,
      final int paramsLen,
      final MemorySegment resultsPtr,
      final int resultsLen) {
    try {
      LOGGER.info(
          "invokeHostFunctionCallback - Called with callbackId="
              + callbackId
              + ", paramsLen="
              + paramsLen);

      final HostFunctionWrapper wrapper = HOST_FUNCTION_CALLBACKS.get(callbackId);
      if (wrapper == null) {
        LOGGER.severe("Host function callback not found for callbackId=" + callbackId);
        return -1; // Error
      }

      // Unmarshal parameters from native memory
      final WasmValue[] params = new WasmValue[paramsLen];
      for (int i = 0; i < paramsLen; i++) {
        // Each WasmValue in native memory is represented as a tagged union
        // For now, we'll need to read the structure from native memory
        // TODO: This requires understanding the WasmValue native layout
        params[i] = unmarshalWasmValue(paramsPtr, i);
      }

      // Call the host function
      LOGGER.fine("Executing host function: " + wrapper.moduleName + "::" + wrapper.name);
      final WasmValue[] results = wrapper.getImplementation().execute(params);

      // Validate result count
      if (results.length != resultsLen) {
        LOGGER.severe(
            "Host function returned " + results.length + " values but expected " + resultsLen);
        return -3; // Error: wrong number of results
      }

      // Marshal results to native memory
      for (int i = 0; i < results.length; i++) {
        marshalWasmValue(results[i], resultsPtr, i);
      }

      LOGGER.info(
          "invokeHostFunctionCallback - Completed successfully with "
              + results.length
              + " results");
      return 0; // Success
    } catch (final Exception e) {
      LOGGER.log(java.util.logging.Level.SEVERE, "Host function execution failed", e);
      return -2; // Error
    }
  }

  /**
   * Unmarshals a WasmValue from native memory.
   *
   * @param ptr pointer to the WasmValue array
   * @param index index in the array
   * @return the unmarshaled WasmValue
   */
  private static WasmValue unmarshalWasmValue(final MemorySegment ptr, final int index) {
    // WasmValue native layout (from Rust):
    // - tag (int): 0=I32, 1=I64, 2=F32, 3=F64, 4=V128
    // - value (union of i32, i64, f32, f64, or 16 bytes for v128)
    // Total size: 4 (tag) + 16 (largest value) = 20 bytes per WasmValue

    final long offset = index * 20L;
    final int tag = ptr.get(ValueLayout.JAVA_INT, offset);

    switch (tag) {
      case 0: // I32
        final int i32Val = ptr.get(ValueLayout.JAVA_INT, offset + 4);
        return WasmValue.i32(i32Val);

      case 1: // I64
        final long i64Val = ptr.get(ValueLayout.JAVA_LONG, offset + 4);
        return WasmValue.i64(i64Val);

      case 2: // F32
        final float f32Val = ptr.get(ValueLayout.JAVA_FLOAT, offset + 4);
        return WasmValue.f32(f32Val);

      case 3: // F64
        final double f64Val = ptr.get(ValueLayout.JAVA_DOUBLE, offset + 4);
        return WasmValue.f64(f64Val);

      case 4: // V128
        final byte[] v128Bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
          v128Bytes[i] = ptr.get(ValueLayout.JAVA_BYTE, offset + 4 + i);
        }
        return WasmValue.v128(v128Bytes);

      default:
        throw new IllegalArgumentException("Unknown WasmValue tag: " + tag);
    }
  }

  /**
   * Marshals a WasmValue to native memory.
   *
   * @param value the WasmValue to marshal
   * @param ptr pointer to the results array
   * @param index index in the array
   */
  private static void marshalWasmValue(
      final WasmValue value, final MemorySegment ptr, final int index) {
    final long offset = index * 20L;

    switch (value.getType()) {
      case I32:
        ptr.set(ValueLayout.JAVA_INT, offset, 0); // tag
        ptr.set(ValueLayout.JAVA_INT, offset + 4, value.asI32());
        break;

      case I64:
        ptr.set(ValueLayout.JAVA_INT, offset, 1); // tag
        ptr.set(ValueLayout.JAVA_LONG, offset + 4, value.asI64());
        break;

      case F32:
        ptr.set(ValueLayout.JAVA_INT, offset, 2); // tag
        ptr.set(ValueLayout.JAVA_FLOAT, offset + 4, value.asF32());
        break;

      case F64:
        ptr.set(ValueLayout.JAVA_INT, offset, 3); // tag
        ptr.set(ValueLayout.JAVA_DOUBLE, offset + 4, value.asF64());
        break;

      case V128:
        ptr.set(ValueLayout.JAVA_INT, offset, 4); // tag
        final byte[] v128Bytes = value.asV128();
        for (int i = 0; i < 16; i++) {
          ptr.set(ValueLayout.JAVA_BYTE, offset + 4 + i, v128Bytes[i]);
        }
        break;

      default:
        throw new IllegalArgumentException("Unsupported WasmValue type: " + value.getType());
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.Linker<T> allowShadowing(final boolean allow) {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.linkerAllowShadowing(nativeLinker, allow ? 1 : 0);
    if (result != 0) {
      LOGGER.warning("Failed to set allow shadowing: error code " + result);
    }
    return this;
  }

  @Override
  public ai.tegmentum.wasmtime4j.Linker<T> allowUnknownExports(final boolean allow) {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.linkerAllowUnknownExports(nativeLinker, allow ? 1 : 0);
    if (result != 0) {
      LOGGER.warning("Failed to set allow unknown exports: error code " + result);
    }
    return this;
  }

  @Override
  public void defineUnknownImportsAsTraps(final Store store, final Module module)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaModule panamaModule = (PanamaModule) module;

    final int result =
        NATIVE_BINDINGS.linkerDefineUnknownImportsAsTraps(
            nativeLinker, panamaStore.getNativeStore(), panamaModule.getNativeModule());

    if (result != 0) {
      throw new WasmException("Failed to define unknown imports as traps: error code " + result);
    }

    LOGGER.fine("Defined unknown imports as traps");
  }

  @Override
  public void defineUnknownImportsAsDefaultValues(final Store store, final Module module)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaModule panamaModule = (PanamaModule) module;

    final int result =
        NATIVE_BINDINGS.linkerDefineUnknownImportsAsDefaultValues(
            nativeLinker, panamaStore.getNativeStore(), panamaModule.getNativeModule());

    if (result != 0) {
      throw new WasmException(
          "Failed to define unknown imports as default values: error code " + result);
    }

    LOGGER.fine("Defined unknown imports as default values");
  }

  @Override
  public void funcNewUnchecked(
      final Store store,
      final String moduleName,
      final String name,
      final FunctionType functionType,
      final HostFunction implementation)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (functionType == null) {
      throw new IllegalArgumentException("Function type cannot be null");
    }
    if (implementation == null) {
      throw new IllegalArgumentException("Implementation cannot be null");
    }
    ensureNotClosed();

    // For unchecked version, we simply call defineHostFunction
    // The native layer handles the unchecked semantics
    defineHostFunction(moduleName, name, functionType, implementation);
    LOGGER.fine("Defined unchecked function: " + moduleName + "::" + name);
  }

  @Override
  public Iterable<ai.tegmentum.wasmtime4j.Linker.LinkerDefinition> iter() {
    ensureNotClosed();

    final java.util.List<ai.tegmentum.wasmtime4j.Linker.LinkerDefinition> definitions =
        new java.util.ArrayList<>();

    // Convert import registry to LinkerDefinition objects
    for (final ImportInfo info : importRegistry.values()) {
      final ai.tegmentum.wasmtime4j.ExternType externType;
      switch (info.getImportType()) {
        case FUNCTION:
          externType = ai.tegmentum.wasmtime4j.ExternType.FUNC;
          break;
        case MEMORY:
          externType = ai.tegmentum.wasmtime4j.ExternType.MEMORY;
          break;
        case TABLE:
          externType = ai.tegmentum.wasmtime4j.ExternType.TABLE;
          break;
        case GLOBAL:
          externType = ai.tegmentum.wasmtime4j.ExternType.GLOBAL;
          break;
        default:
          externType = ai.tegmentum.wasmtime4j.ExternType.FUNC;
      }

      definitions.add(
          new ai.tegmentum.wasmtime4j.Linker.LinkerDefinition(
              info.getModuleName(), info.getImportName(), externType));
    }

    return definitions;
  }

  @Override
  public ai.tegmentum.wasmtime4j.Extern getByImport(
      final Store store, final String moduleName, final String name) {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }

    final PanamaStore panamaStore = (PanamaStore) store;

    // Allocate C strings for module name and item name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);
    final MemorySegment namePtr = arena.allocateFrom(name);

    // Call native function to get the extern
    final MemorySegment externPtr =
        NATIVE_BINDINGS.linkerGetByImport(
            nativeLinker, panamaStore.getNativeStore(), moduleNamePtr, namePtr);

    if (externPtr == null || externPtr.equals(MemorySegment.NULL)) {
      return null;
    }

    // Determine extern type and wrap appropriately
    final int externTypeCode = NATIVE_BINDINGS.externGetType(externPtr);
    switch (externTypeCode) {
      case 0: // FUNC
        return new PanamaExternFunc(externPtr, panamaStore);
      case 1: // TABLE
        return new PanamaExternTable(externPtr, panamaStore);
      case 2: // MEMORY
        return new PanamaExternMemory(externPtr, panamaStore);
      case 3: // GLOBAL
        return new PanamaExternGlobal(externPtr, panamaStore);
      default:
        LOGGER.warning("Unknown extern type code: " + externTypeCode);
        return null;
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmFunction getDefault(
      final Store store, final String moduleName) {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore");
    }

    final PanamaStore panamaStore = (PanamaStore) store;

    // Allocate C string for module name
    final MemorySegment moduleNamePtr = arena.allocateFrom(moduleName);

    // Call native function to get the default function
    final MemorySegment funcPtr =
        NATIVE_BINDINGS.linkerGetDefault(nativeLinker, panamaStore.getNativeStore(), moduleNamePtr);

    if (funcPtr == null || funcPtr.equals(MemorySegment.NULL)) {
      return null;
    }

    // Cannot create a PanamaFunction without an instance context
    // The function handle is valid but requires an instance for call operations
    return null;
  }

  /** Wrapper for host function callbacks. */
  private static class HostFunctionWrapper {
    private static final AtomicLong nextId = new AtomicLong(1);

    private final long id;
    private final String moduleName;
    private final String name;
    private final HostFunction implementation;
    private final FunctionType functionType;

    HostFunctionWrapper(
        final String moduleName,
        final String name,
        final HostFunction implementation,
        final FunctionType functionType) {
      this.id = nextId.getAndIncrement();
      this.moduleName = moduleName;
      this.name = name;
      this.implementation = implementation;
      this.functionType = functionType;
    }

    long getId() {
      return id;
    }

    HostFunction getImplementation() {
      return implementation;
    }

    FunctionType getFunctionType() {
      return functionType;
    }
  }
}
