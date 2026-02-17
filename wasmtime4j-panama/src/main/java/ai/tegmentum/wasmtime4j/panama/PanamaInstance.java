package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ExternRef;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstanceState;
import ai.tegmentum.wasmtime4j.InstanceStatistics;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.TableType;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Instance.
 *
 * @since 1.0.0
 */
public final class PanamaInstance implements Instance {
  private static final Logger LOGGER = Logger.getLogger(PanamaInstance.class.getName());
  private static final NativeInstanceBindings NATIVE_INSTANCE_BINDINGS =
      NativeInstanceBindings.getInstance();
  private static final NativeEngineBindings NATIVE_ENGINE_BINDINGS =
      NativeEngineBindings.getInstance();
  private static final NativeMemoryBindings NATIVE_MEMORY_BINDINGS =
      NativeMemoryBindings.getInstance();

  // Thread-local arena pool for function calls - eliminates per-call Arena allocation
  private static final ThreadLocal<CallContext> CALL_CONTEXT =
      ThreadLocal.withInitial(CallContext::new);

  /** Minimum params buffer size (8 parameters * 20 bytes = 160 bytes). */
  private static final int MIN_PARAMS_BUFFER_SIZE = 160;

  /** Maximum results buffer size (32 results * 20 bytes = 640 bytes). */
  private static final int MAX_RESULTS_BUFFER_SIZE = 640;

  /**
   * Global registry for ExternRef objects to track them by ID for native interop. ExternRef objects
   * are stored here when marshalled to native and looked up when unmarshalled.
   */
  private static final ConcurrentHashMap<Long, ExternRef<?>> EXTERN_REF_REGISTRY =
      new ConcurrentHashMap<>();

  /**
   * Thread-local context for function call buffers. Reuses arena and pre-allocated buffers across
   * calls to avoid per-call allocation overhead.
   */
  private static final class CallContext {
    final Arena arena;
    MemorySegment paramsBuffer;
    final MemorySegment resultsBuffer;

    CallContext() {
      this.arena = Arena.ofConfined();
      // Pre-allocate results buffer for up to 16 results (most common case)
      this.resultsBuffer = arena.allocate(MAX_RESULTS_BUFFER_SIZE);
    }

    /**
     * Gets or grows the params buffer to fit the required number of parameters.
     *
     * @param paramCount number of parameters needed
     * @return memory segment for parameters
     */
    MemorySegment getParamsBuffer(final int paramCount) {
      final long needed = paramCount * 20L;
      if (paramsBuffer == null || paramsBuffer.byteSize() < needed) {
        // Allocate with minimum size to reduce future reallocations
        paramsBuffer = arena.allocate(Math.max(needed, MIN_PARAMS_BUFFER_SIZE));
      }
      return paramsBuffer;
    }
  }

  private final PanamaModule module;
  private final PanamaStore store;
  private final MemorySegment nativeInstance;
  private final long createdAtMicros;
  private final AtomicBoolean disposed = new AtomicBoolean(false);
  private final NativeResourceHandle resourceHandle;

  // Optimization: Shared arena for function calls (lazy initialized to avoid constructor overhead)
  private volatile Arena callArena;
  // Optimization: Cache function name MemorySegments to avoid repeated string encoding
  private final ConcurrentHashMap<String, MemorySegment> functionNameCache =
      new ConcurrentHashMap<>();
  // Track ExternRef IDs registered by this instance for cleanup
  private final Set<Long> registeredExternRefIds = ConcurrentHashMap.newKeySet();

  /**
   * Creates a new Panama instance.
   *
   * @param module the module to instantiate
   * @param store the store to create the instance in
   * @throws WasmException if instance creation fails
   */
  public PanamaInstance(final PanamaModule module, final PanamaStore store) throws WasmException {
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    if (!module.isValid()) {
      throw new IllegalStateException("Module is not valid");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (!store.isValid()) {
      throw new IllegalStateException("Store is not valid");
    }

    this.module = module;
    this.store = store;
    this.createdAtMicros = System.currentTimeMillis() * 1000L;

    // Create native instance via Panama FFI
    this.nativeInstance =
        NATIVE_INSTANCE_BINDINGS.instanceCreate(store.getNativeStore(), module.getNativeModule());

    if (this.nativeInstance == null || this.nativeInstance.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to create native instance");
    }

    this.resourceHandle = createResourceHandle();

    LOGGER.fine("Created Panama instance");
  }

  /**
   * Package-private constructor for wrapping an existing native instance pointer.
   *
   * @param nativeInstance the native instance pointer from Wasmtime
   * @param module the module this instance was created from
   * @param store the store that owns this instance
   */
  PanamaInstance(
      final MemorySegment nativeInstance, final PanamaModule module, final PanamaStore store) {
    if (nativeInstance == null || nativeInstance.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native instance pointer cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }

    this.nativeInstance = nativeInstance;
    this.module = module;
    this.store = store;
    this.createdAtMicros = System.currentTimeMillis() * 1000L;

    this.resourceHandle = createResourceHandle();

    LOGGER.fine("Wrapped native instance pointer");
  }

  /**
   * Gets the arena from the store for temporary allocations.
   *
   * @return the store's arena
   */
  private Arena getArena() {
    return store.getArena();
  }

  /**
   * Gets or creates the shared arena for caching function name segments. Uses double-checked
   * locking for thread-safe lazy initialization.
   *
   * @return the shared call arena
   */
  private Arena getCallArena() {
    Arena arena = callArena;
    if (arena == null) {
      synchronized (this) {
        arena = callArena;
        if (arena == null) {
          callArena = arena = Arena.ofShared();
        }
      }
    }
    return arena;
  }

  @Override
  public Optional<WasmFunction> getFunction(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    // Check if the export exists and is a function
    final MemorySegment modulePtr = module.getNativeModule();
    final MemorySegment namePtr =
        getArena().allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8);
    final int exportKind = NATIVE_ENGINE_BINDINGS.moduleGetExportKind(modulePtr, namePtr);

    // exportKind: 0=not found, 1=function, 2=global, 3=memory, 4=table
    if (exportKind != 1) {
      return Optional.empty();
    }

    final FunctionType functionType = lookupFunctionType(name);
    return Optional.of(new PanamaFunction(this, name, functionType));
  }

  @Override
  public Optional<WasmFunction> getFunction(final int index) {
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative");
    }
    ensureNotClosed();

    // Get all export names and find the index-th function export
    final String[] exportNames = getExportNames();
    int functionIndex = 0;

    for (final String exportName : exportNames) {
      final Optional<WasmFunction> function = getFunction(exportName);
      if (function.isPresent()) {
        if (functionIndex == index) {
          return function;
        }
        functionIndex++;
      }
    }

    return Optional.empty();
  }

  /**
   * Looks up the function type for a named export from the module metadata.
   *
   * @param name the export function name
   * @return the function type, or an empty FunctionType if lookup fails
   */
  private FunctionType lookupFunctionType(final String name) {
    final Optional<FuncType> funcType = module.getFunctionType(name);
    if (funcType.isPresent()) {
      final FuncType ft = funcType.get();
      final WasmValueType[] params = ft.getParams().toArray(new WasmValueType[0]);
      final WasmValueType[] results = ft.getResults().toArray(new WasmValueType[0]);
      return new FunctionType(params, results);
    }
    return new FunctionType(new WasmValueType[0], new WasmValueType[0]);
  }

  @Override
  public Optional<WasmGlobal> getGlobal(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    // Check if the global export exists by calling native
    final MemorySegment nameSegment =
        getArena().allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8);
    final int result =
        NATIVE_INSTANCE_BINDINGS.instanceHasGlobalExport(
            nativeInstance, store.getNativeStore(), nameSegment);

    // Result is 0 on success (global exists), non-zero on error/not found
    if (result != 0) {
      return Optional.empty();
    }

    // Query the global's type and mutability from the instance
    final MemorySegment valueTypeOut = getArena().allocate(ValueLayout.JAVA_INT);
    final MemorySegment isMutableOut = getArena().allocate(ValueLayout.JAVA_INT);
    final int typeResult =
        NATIVE_INSTANCE_BINDINGS.instanceGetGlobalType(
            nativeInstance, store.getNativeStore(), nameSegment, valueTypeOut, isMutableOut);

    if (typeResult != 0) {
      return Optional.empty();
    }

    final int typeCode = valueTypeOut.get(ValueLayout.JAVA_INT, 0);
    final boolean mutable = isMutableOut.get(ValueLayout.JAVA_INT, 0) != 0;
    final WasmValueType type = mapNativeTypeToWasmValueType(typeCode);

    // Return an instance-bound global that uses instance-specific methods
    return Optional.of(new PanamaInstanceGlobal(this, store, name, type, mutable));
  }

  @Override
  public Optional<WasmGlobal> getGlobal(final int index) {
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative");
    }
    ensureNotClosed();

    // Iterate through exports and find the nth global
    final String[] exportNames = getExportNames();
    int globalCount = 0;
    for (final String name : exportNames) {
      final Optional<WasmGlobal> global = getGlobal(name);
      if (global.isPresent()) {
        if (globalCount == index) {
          return global;
        }
        globalCount++;
      }
    }

    return Optional.empty();
  }

  @Override
  public Optional<WasmMemory> getMemory(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    // Check if the export exists and is a memory using moduleGetExportKind
    // Note: This is more reliable than instanceHasMemoryExport for shared memories
    // because shared memory exports are Extern::SharedMemory, not Extern::Memory
    try (final Arena checkArena = Arena.ofConfined()) {
      final int exportKind =
          NATIVE_ENGINE_BINDINGS.moduleGetExportKind(
              module.getNativeModule(),
              checkArena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));

      // exportKind: 0=not found, 1=function, 2=global, 3=memory, 4=table
      if (exportKind != 3) {
        return Optional.empty();
      }
    }

    // Return a PanamaMemory that stores just the name
    // The actual memory will be looked up fresh for each operation
    return Optional.of(new PanamaMemory(name, this));
  }

  @Override
  public Optional<WasmMemory> getMemory(final int index) {
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative");
    }
    ensureNotClosed();

    // Iterate through exports and find the nth memory
    final String[] exportNames = getExportNames();
    int memoryCount = 0;
    for (final String name : exportNames) {
      final Optional<WasmMemory> memory = getMemory(name);
      if (memory.isPresent()) {
        if (memoryCount == index) {
          return memory;
        }
        memoryCount++;
      }
    }

    return Optional.empty();
  }

  @Override
  public Optional<WasmTable> getTable(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    final MemorySegment nameSegment =
        getArena().allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8);
    final MemorySegment tablePtr =
        NATIVE_INSTANCE_BINDINGS.instanceGetTableByName(
            nativeInstance, store.getNativeStore(), nameSegment);

    if (tablePtr == null || tablePtr.equals(MemorySegment.NULL)) {
      return Optional.empty();
    }

    return Optional.of(new PanamaTable(tablePtr, this));
  }

  @Override
  public Optional<WasmTable> getTable(final int index) {
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative");
    }
    ensureNotClosed();

    // Iterate through exports and find the nth table
    final String[] exportNames = getExportNames();
    int tableCount = 0;
    for (final String name : exportNames) {
      final Optional<WasmTable> table = getTable(name);
      if (table.isPresent()) {
        if (tableCount == index) {
          return table;
        }
        tableCount++;
      }
    }

    return Optional.empty();
  }

  @Override
  public Optional<WasmMemory> getDefaultMemory() {
    ensureNotClosed();

    // First, try to get memory named "memory" (most common convention)
    final Optional<WasmMemory> namedMemory = getMemory("memory");
    if (namedMemory.isPresent()) {
      return namedMemory;
    }

    // If not found, try to get the first memory export
    final String[] exportNames = getExportNames();
    for (final String exportName : exportNames) {
      final Optional<WasmMemory> memory = getMemory(exportName);
      if (memory.isPresent()) {
        return memory;
      }
    }

    // No memory exports found
    return Optional.empty();
  }

  @Override
  public String[] getExportNames() {
    ensureNotClosed();

    // Get the export count
    final long exportCount = NATIVE_ENGINE_BINDINGS.moduleExportCount(module.getNativeModule());
    if (exportCount <= 0) {
      return new String[0];
    }

    try (final Arena exportArena = Arena.ofConfined()) {
      // Allocate array of pointers to hold C string pointers
      final MemorySegment namesArray = exportArena.allocate(ValueLayout.ADDRESS, (int) exportCount);

      // Call native function to populate the array
      final long actualCount =
          NATIVE_ENGINE_BINDINGS.moduleGetExportNames(
              module.getNativeModule(), namesArray, exportCount);

      if (actualCount <= 0) {
        return new String[0];
      }

      // Convert C strings to Java strings
      final String[] exportNames = new String[(int) actualCount];
      for (int i = 0; i < actualCount; i++) {
        final MemorySegment cStringPtr = namesArray.getAtIndex(ValueLayout.ADDRESS, i);
        if (cStringPtr != null && !cStringPtr.equals(MemorySegment.NULL)) {
          // Reinterpret as unbounded segment to read null-terminated string
          final MemorySegment unboundedPtr = cStringPtr.reinterpret(Long.MAX_VALUE);
          exportNames[i] = unboundedPtr.getString(0);
          // Free the C string allocated by Rust
          NATIVE_MEMORY_BINDINGS.freeString(cStringPtr);
        }
      }

      return exportNames;
    }
  }

  @Override
  public Optional<FuncType> getFunctionType(final String functionName) {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();
    // Delegate to module's type lookup - module knows all export types
    return module.getFunctionType(functionName);
  }

  @Override
  public Optional<GlobalType> getGlobalType(final String globalName) {
    if (globalName == null) {
      throw new IllegalArgumentException("Global name cannot be null");
    }
    ensureNotClosed();
    // Delegate to module's type lookup - module knows all export types
    return module.getGlobalType(globalName);
  }

  @Override
  public Optional<MemoryType> getMemoryType(final String memoryName) {
    if (memoryName == null) {
      throw new IllegalArgumentException("Memory name cannot be null");
    }
    ensureNotClosed();
    // Delegate to module's type lookup - module knows all export types
    return module.getMemoryType(memoryName);
  }

  @Override
  public Optional<TableType> getTableType(final String tableName) {
    if (tableName == null) {
      throw new IllegalArgumentException("Table name cannot be null");
    }
    ensureNotClosed();
    // Delegate to module's type lookup - module knows all export types
    return module.getTableType(tableName);
  }

  @Override
  public boolean hasExport(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    try (final Arena checkArena = Arena.ofConfined()) {
      final int exportKind =
          NATIVE_ENGINE_BINDINGS.moduleGetExportKind(
              module.getNativeModule(),
              checkArena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));
      // exportKind: 0=not found, 1=function, 2=global, 3=memory, 4=table
      return exportKind > 0;
    }
  }

  /**
   * Checks if the instance has a function export with the given name.
   *
   * @param name the name to check
   * @return true if a function export with this name exists, false otherwise
   * @throws IllegalArgumentException if name is null
   */
  public boolean hasFunction(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    try (final Arena checkArena = Arena.ofConfined()) {
      final int exportKind =
          NATIVE_ENGINE_BINDINGS.moduleGetExportKind(
              module.getNativeModule(),
              checkArena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));
      // exportKind: 0=not found, 1=function, 2=global, 3=memory, 4=table
      return exportKind == 1;
    }
  }

  /**
   * Checks if the instance has a memory export with the given name.
   *
   * @param name the name to check
   * @return true if a memory export with this name exists, false otherwise
   * @throws IllegalArgumentException if name is null
   */
  public boolean hasMemory(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    try (final Arena checkArena = Arena.ofConfined()) {
      final int exportKind =
          NATIVE_ENGINE_BINDINGS.moduleGetExportKind(
              module.getNativeModule(),
              checkArena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));
      // exportKind: 0=not found, 1=function, 2=global, 3=memory, 4=table
      return exportKind == 3;
    }
  }

  /**
   * Checks if the instance has a table export with the given name.
   *
   * @param name the name to check
   * @return true if a table export with this name exists, false otherwise
   * @throws IllegalArgumentException if name is null
   */
  public boolean hasTable(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    try (final Arena checkArena = Arena.ofConfined()) {
      final int exportKind =
          NATIVE_ENGINE_BINDINGS.moduleGetExportKind(
              module.getNativeModule(),
              checkArena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));
      // exportKind: 0=not found, 1=function, 2=global, 3=memory, 4=table
      return exportKind == 4;
    }
  }

  /**
   * Checks if the instance has a global export with the given name.
   *
   * @param name the name to check
   * @return true if a global export with this name exists, false otherwise
   * @throws IllegalArgumentException if name is null
   */
  public boolean hasGlobal(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    try (final Arena checkArena = Arena.ofConfined()) {
      final int exportKind =
          NATIVE_ENGINE_BINDINGS.moduleGetExportKind(
              module.getNativeModule(),
              checkArena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));
      // exportKind: 0=not found, 1=function, 2=global, 3=memory, 4=table
      return exportKind == 2;
    }
  }

  @Override
  public Module getModule() {
    return module;
  }

  @Override
  public Store getStore() {
    return store;
  }

  /**
   * Gets the native store memory segment for FFI operations.
   *
   * @return the native store memory segment
   */
  MemorySegment getNativeStore() {
    return store.getNativeStore();
  }

  /**
   * Gets the native instance pointer.
   *
   * @return the native instance pointer
   */
  public MemorySegment getNativeInstance() {
    return nativeInstance;
  }

  @Override
  public WasmValue[] callFunction(final String functionName, final WasmValue... params)
      throws WasmException {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    // Maximum possible results (matches MAX_RESULTS_BUFFER_SIZE / 20)
    final int maxResults = 32;

    // Get or create cached function name segment (avoids repeated string encoding)
    final MemorySegment functionNameSegment =
        functionNameCache.computeIfAbsent(
            functionName,
            name -> getCallArena().allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));

    // Get thread-local call context (reuses arena and buffers across calls)
    final CallContext ctx = CALL_CONTEXT.get();

    // Get or allocate params buffer from thread-local context
    final MemorySegment paramsSegment;
    final int paramCount = (params != null) ? params.length : 0;
    if (paramCount > 0) {
      paramsSegment = ctx.getParamsBuffer(paramCount);
      for (int i = 0; i < paramCount; i++) {
        marshalWasmValue(params[i], paramsSegment, i);
      }
    } else {
      paramsSegment = MemorySegment.NULL;
    }

    // Use pre-allocated results buffer from thread-local context
    final MemorySegment resultsSegment = ctx.resultsBuffer;

    // Call native function using invokeExact fast path
    final long resultCount =
        NATIVE_INSTANCE_BINDINGS.instanceCallFunctionFast(
            nativeInstance,
            store.getNativeStore(),
            functionNameSegment,
            paramsSegment,
            paramCount,
            resultsSegment,
            maxResults);

    if (resultCount < 0) {
      final String errorMsg = PanamaErrorMapper.retrieveNativeErrorMessage();
      throw new WasmException(
          errorMsg != null ? errorMsg : "Failed to call function: " + functionName);
    }

    // Unmarshal results - allocate new array each time to avoid aliasing bugs
    // (returning pooled arrays caused results to be overwritten by subsequent calls)
    final int count = (int) resultCount;
    final WasmValue[] results = new WasmValue[count];
    for (int i = 0; i < count; i++) {
      results[i] = unmarshalWasmValue(resultsSegment, i);
    }

    return results;
  }

  @Override
  public Map<String, Object> getAllExports() {
    ensureNotClosed();

    final Map<String, Object> exports = new java.util.HashMap<>();
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      // Try each export type
      final Optional<WasmFunction> function = getFunction(name);
      if (function.isPresent()) {
        exports.put(name, function.get());
        continue;
      }

      final Optional<WasmMemory> memory = getMemory(name);
      if (memory.isPresent()) {
        exports.put(name, memory.get());
        continue;
      }

      final Optional<WasmTable> table = getTable(name);
      if (table.isPresent()) {
        exports.put(name, table.get());
        continue;
      }

      final Optional<WasmGlobal> global = getGlobal(name);
      if (global.isPresent()) {
        exports.put(name, global.get());
      }
    }

    return Collections.unmodifiableMap(exports);
  }

  /**
   * Gets all exported function names.
   *
   * @return list of exported function names
   */
  public List<String> getFunctionNames() {
    ensureNotClosed();
    final List<String> functionNames = new java.util.ArrayList<>();
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmFunction> function = getFunction(name);
      if (function.isPresent()) {
        functionNames.add(name);
      }
    }

    return Collections.unmodifiableList(functionNames);
  }

  /**
   * Gets all exported memory names.
   *
   * @return list of exported memory names
   */
  public List<String> getMemoryNames() {
    ensureNotClosed();
    final List<String> memoryNames = new java.util.ArrayList<>();
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmMemory> memory = getMemory(name);
      if (memory.isPresent()) {
        memoryNames.add(name);
      }
    }

    return Collections.unmodifiableList(memoryNames);
  }

  /**
   * Gets all exported table names.
   *
   * @return list of exported table names
   */
  public List<String> getTableNames() {
    ensureNotClosed();
    final List<String> tableNames = new java.util.ArrayList<>();
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmTable> table = getTable(name);
      if (table.isPresent()) {
        tableNames.add(name);
      }
    }

    return Collections.unmodifiableList(tableNames);
  }

  /**
   * Gets all exported global names.
   *
   * @return list of exported global names
   */
  public List<String> getGlobalNames() {
    ensureNotClosed();
    final List<String> globalNames = new java.util.ArrayList<>();
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmGlobal> global = getGlobal(name);
      if (global.isPresent()) {
        globalNames.add(name);
      }
    }

    return Collections.unmodifiableList(globalNames);
  }

  /**
   * Gets the count of exported functions.
   *
   * @return number of function exports
   */
  public int getFunctionCount() {
    ensureNotClosed();
    int count = 0;
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmFunction> function = getFunction(name);
      if (function.isPresent()) {
        count++;
      }
    }

    return count;
  }

  /**
   * Gets the count of exported memories.
   *
   * @return number of memory exports
   */
  public int getMemoryCount() {
    ensureNotClosed();
    int count = 0;
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmMemory> memory = getMemory(name);
      if (memory.isPresent()) {
        count++;
      }
    }

    return count;
  }

  /**
   * Gets the count of exported tables.
   *
   * @return number of table exports
   */
  public int getTableCount() {
    ensureNotClosed();
    int count = 0;
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmTable> table = getTable(name);
      if (table.isPresent()) {
        count++;
      }
    }

    return count;
  }

  /**
   * Gets the count of exported globals.
   *
   * @return number of global exports
   */
  public int getGlobalCount() {
    ensureNotClosed();
    int count = 0;
    final String[] exportNames = getExportNames();

    for (final String name : exportNames) {
      final Optional<WasmGlobal> global = getGlobal(name);
      if (global.isPresent()) {
        count++;
      }
    }

    return count;
  }

  @Override
  public void setImports(final Map<String, Object> imports) throws WasmException {
    if (imports == null) {
      throw new IllegalArgumentException("Imports cannot be null");
    }
    ensureNotClosed();
    // Imports cannot be set after instantiation - this matches JNI behavior
    throw new UnsupportedOperationException(
        "Setting imports is not supported for instantiated instances");
  }

  @Override
  public InstanceStatistics getStatistics() throws WasmException {
    throw new UnsupportedOperationException("getStatistics not yet implemented");
  }

  @Override
  public InstanceState getState() {
    if (resourceHandle.isClosed() || disposed.get()) {
      return InstanceState.DISPOSED;
    }
    return InstanceState.CREATED;
  }

  @Override
  public boolean cleanup() throws WasmException {
    if (disposed.get()) {
      return false;
    }
    close();
    return true;
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed() && !disposed.get();
  }

  @Override
  public boolean dispose() throws WasmException {
    if (disposed.getAndSet(true)) {
      return false;
    }
    close();
    return true;
  }

  @Override
  public boolean isDisposed() {
    return disposed.get();
  }

  @Override
  public long getCreatedAtMicros() {
    return createdAtMicros;
  }

  @Override
  public int getMetadataExportCount() {
    ensureNotClosed();
    final long count = NATIVE_ENGINE_BINDINGS.moduleExportCount(module.getNativeModule());
    return (int) count;
  }

  @Override
  public int callI32Function(final String functionName, final int... params) throws WasmException {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    // Convert int parameters to WasmValue array
    final WasmValue[] wasmParams;
    if (params != null && params.length > 0) {
      wasmParams = new WasmValue[params.length];
      for (int i = 0; i < params.length; i++) {
        wasmParams[i] = WasmValue.i32(params[i]);
      }
    } else {
      wasmParams = new WasmValue[0];
    }

    // Call the function
    final WasmValue[] results = callFunction(functionName, wasmParams);

    // Validate and extract result
    if (results == null || results.length == 0) {
      throw new WasmException("Function " + functionName + " did not return a value");
    }
    if (results.length > 1) {
      throw new WasmException(
          "Function " + functionName + " returned multiple values, expected single i32");
    }
    if (results[0].getType() != WasmValueType.I32) {
      throw new WasmException(
          "Function " + functionName + " returned " + results[0].getType() + ", expected I32");
    }

    return results[0].asI32();
  }

  @Override
  public int callI32Function(final String functionName) throws WasmException {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    // Call the function with no parameters
    final WasmValue[] results = callFunction(functionName);

    // Validate and extract result
    if (results == null || results.length == 0) {
      throw new WasmException("Function " + functionName + " did not return a value");
    }
    if (results.length > 1) {
      throw new WasmException(
          "Function " + functionName + " returned multiple values, expected single i32");
    }
    if (results[0].getType() != WasmValueType.I32) {
      throw new WasmException(
          "Function " + functionName + " returned " + results[0].getType() + ", expected I32");
    }

    return results[0].asI32();
  }

  // ===== Optimized Typed Function Call Methods =====
  // These methods bypass WasmValue boxing/unboxing for maximum performance.

  /**
   * Fast-path call for functions with two i32 parameters returning i32. Bypasses WasmValue boxing
   * entirely by writing primitives directly to native memory.
   *
   * @param functionName the name of the function to call
   * @param arg1 the first i32 argument
   * @param arg2 the second i32 argument
   * @return the i32 result
   * @throws WasmException if function execution fails
   */
  public int callI32I32ToI32Fast(final String functionName, final int arg1, final int arg2)
      throws WasmException {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    // Get or create cached function name segment
    final MemorySegment functionNameSegment =
        functionNameCache.computeIfAbsent(
            functionName,
            name -> getCallArena().allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));

    // Get thread-local call context
    final CallContext ctx = CALL_CONTEXT.get();
    final MemorySegment params = ctx.getParamsBuffer(2);

    // Direct primitive writes - no WasmValue objects created
    // Tag 0 = I32, value at offset+4
    params.set(ValueLayout.JAVA_INT, 0, 0); // Tag: I32
    params.set(ValueLayout.JAVA_INT, 4, arg1); // Value 1
    params.set(ValueLayout.JAVA_INT, 20, 0); // Tag: I32
    params.set(ValueLayout.JAVA_INT, 24, arg2); // Value 2

    final long resultCount =
        NATIVE_INSTANCE_BINDINGS.instanceCallFunctionFast(
            nativeInstance,
            store.getNativeStore(),
            functionNameSegment,
            params,
            2,
            ctx.resultsBuffer,
            1);

    if (resultCount < 0) {
      final String errorMsg = PanamaErrorMapper.retrieveNativeErrorMessage();
      throw new WasmException(
          errorMsg != null ? errorMsg : "Failed to call function: " + functionName);
    }
    if (resultCount != 1) {
      throw new WasmException("Expected 1 result, got " + resultCount);
    }

    // Direct primitive read - no WasmValue creation
    return ctx.resultsBuffer.get(ValueLayout.JAVA_INT, 4);
  }

  /**
   * Fast-path call for functions with one i32 parameter returning i32. Bypasses WasmValue boxing
   * entirely by writing primitives directly to native memory.
   *
   * @param functionName the name of the function to call
   * @param arg the i32 argument
   * @return the i32 result
   * @throws WasmException if function execution fails
   */
  public int callI32ToI32Fast(final String functionName, final int arg) throws WasmException {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    final MemorySegment functionNameSegment =
        functionNameCache.computeIfAbsent(
            functionName,
            name -> getCallArena().allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));

    final CallContext ctx = CALL_CONTEXT.get();
    final MemorySegment params = ctx.getParamsBuffer(1);

    params.set(ValueLayout.JAVA_INT, 0, 0); // Tag: I32
    params.set(ValueLayout.JAVA_INT, 4, arg); // Value

    final long resultCount =
        NATIVE_INSTANCE_BINDINGS.instanceCallFunctionFast(
            nativeInstance,
            store.getNativeStore(),
            functionNameSegment,
            params,
            1,
            ctx.resultsBuffer,
            1);

    if (resultCount < 0) {
      final String errorMsg = PanamaErrorMapper.retrieveNativeErrorMessage();
      throw new WasmException(
          errorMsg != null ? errorMsg : "Failed to call function: " + functionName);
    }
    if (resultCount != 1) {
      throw new WasmException("Expected 1 result, got " + resultCount);
    }

    return ctx.resultsBuffer.get(ValueLayout.JAVA_INT, 4);
  }

  /**
   * Fast-path call for functions with no parameters returning i32. Bypasses WasmValue boxing
   * entirely.
   *
   * @param functionName the name of the function to call
   * @return the i32 result
   * @throws WasmException if function execution fails
   */
  public int callToI32Fast(final String functionName) throws WasmException {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    final MemorySegment functionNameSegment =
        functionNameCache.computeIfAbsent(
            functionName,
            name -> getCallArena().allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));

    final CallContext ctx = CALL_CONTEXT.get();

    final long resultCount =
        NATIVE_INSTANCE_BINDINGS.instanceCallFunctionFast(
            nativeInstance,
            store.getNativeStore(),
            functionNameSegment,
            MemorySegment.NULL,
            0,
            ctx.resultsBuffer,
            1);

    if (resultCount < 0) {
      final String errorMsg = PanamaErrorMapper.retrieveNativeErrorMessage();
      throw new WasmException(
          errorMsg != null ? errorMsg : "Failed to call function: " + functionName);
    }
    if (resultCount != 1) {
      throw new WasmException("Expected 1 result, got " + resultCount);
    }

    return ctx.resultsBuffer.get(ValueLayout.JAVA_INT, 4);
  }

  /**
   * Fast-path call for functions with no parameters and no return value. Bypasses WasmValue boxing
   * entirely.
   *
   * @param functionName the name of the function to call
   * @throws WasmException if function execution fails
   */
  public void callVoidFast(final String functionName) throws WasmException {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    final MemorySegment functionNameSegment =
        functionNameCache.computeIfAbsent(
            functionName,
            name -> getCallArena().allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));

    final CallContext ctx = CALL_CONTEXT.get();

    final long resultCount =
        NATIVE_INSTANCE_BINDINGS.instanceCallFunctionFast(
            nativeInstance,
            store.getNativeStore(),
            functionNameSegment,
            MemorySegment.NULL,
            0,
            ctx.resultsBuffer,
            0);

    if (resultCount < 0) {
      final String errorMsg = PanamaErrorMapper.retrieveNativeErrorMessage();
      throw new WasmException(
          errorMsg != null ? errorMsg : "Failed to call function: " + functionName);
    }
  }

  /**
   * Fast-path call for functions with one i64 parameter returning i64. Bypasses WasmValue boxing
   * entirely.
   *
   * @param functionName the name of the function to call
   * @param arg the i64 argument
   * @return the i64 result
   * @throws WasmException if function execution fails
   */
  public long callI64ToI64Fast(final String functionName, final long arg) throws WasmException {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    final MemorySegment functionNameSegment =
        functionNameCache.computeIfAbsent(
            functionName,
            name -> getCallArena().allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));

    final CallContext ctx = CALL_CONTEXT.get();
    final MemorySegment params = ctx.getParamsBuffer(1);

    params.set(ValueLayout.JAVA_INT, 0, 1); // Tag: I64
    params.set(ValueLayout.JAVA_LONG_UNALIGNED, 4, arg); // Value

    final long resultCount =
        NATIVE_INSTANCE_BINDINGS.instanceCallFunctionFast(
            nativeInstance,
            store.getNativeStore(),
            functionNameSegment,
            params,
            1,
            ctx.resultsBuffer,
            1);

    if (resultCount < 0) {
      final String errorMsg = PanamaErrorMapper.retrieveNativeErrorMessage();
      throw new WasmException(
          errorMsg != null ? errorMsg : "Failed to call function: " + functionName);
    }
    if (resultCount != 1) {
      throw new WasmException("Expected 1 result, got " + resultCount);
    }

    return ctx.resultsBuffer.get(ValueLayout.JAVA_LONG_UNALIGNED, 4);
  }

  /**
   * Fast-path call for functions with one f64 parameter returning f64. Bypasses WasmValue boxing
   * entirely.
   *
   * @param functionName the name of the function to call
   * @param arg the f64 argument
   * @return the f64 result
   * @throws WasmException if function execution fails
   */
  public double callF64ToF64Fast(final String functionName, final double arg) throws WasmException {
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    final MemorySegment functionNameSegment =
        functionNameCache.computeIfAbsent(
            functionName,
            name -> getCallArena().allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8));

    final CallContext ctx = CALL_CONTEXT.get();
    final MemorySegment params = ctx.getParamsBuffer(1);

    params.set(ValueLayout.JAVA_INT, 0, 3); // Tag: F64
    params.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, 4, arg); // Value

    final long resultCount =
        NATIVE_INSTANCE_BINDINGS.instanceCallFunctionFast(
            nativeInstance,
            store.getNativeStore(),
            functionNameSegment,
            params,
            1,
            ctx.resultsBuffer,
            1);

    if (resultCount < 0) {
      final String errorMsg = PanamaErrorMapper.retrieveNativeErrorMessage();
      throw new WasmException(
          errorMsg != null ? errorMsg : "Failed to call function: " + functionName);
    }
    if (resultCount != 1) {
      throw new WasmException("Expected 1 result, got " + resultCount);
    }

    return ctx.resultsBuffer.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, 4);
  }


  @Override
  public void close() {
    resourceHandle.close();
  }

  /**
   * Marshals a WasmValue to native memory.
   *
   * @param value the WasmValue to marshal
   * @param ptr pointer to the array
   * @param index index in the array
   */
  private void marshalWasmValue(
      final WasmValue value, final MemorySegment ptr, final int index) {
    final long offset = index * 20L;

    switch (value.getType()) {
      case I32:
        ptr.set(ValueLayout.JAVA_INT, offset, 0); // tag
        ptr.set(ValueLayout.JAVA_INT, offset + 4, value.asI32());
        break;

      case I64:
        ptr.set(ValueLayout.JAVA_INT, offset, 1); // tag
        ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + 4, value.asI64());
        break;

      case F32:
        ptr.set(ValueLayout.JAVA_INT, offset, 2); // tag
        ptr.set(ValueLayout.JAVA_FLOAT, offset + 4, value.asF32());
        break;

      case F64:
        ptr.set(ValueLayout.JAVA_INT, offset, 3); // tag
        ptr.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset + 4, value.asF64());
        break;

      case V128:
        ptr.set(ValueLayout.JAVA_INT, offset, 4); // tag
        final byte[] v128Bytes = value.asV128();
        for (int i = 0; i < 16; i++) {
          ptr.set(ValueLayout.JAVA_BYTE, offset + 4 + i, v128Bytes[i]);
        }
        break;

      case FUNCREF:
        ptr.set(ValueLayout.JAVA_INT, offset, 5); // tag - FuncRef uses tag 5
        final Object funcValue = value.getValue();
        if (funcValue == null) {
          // Null funcref - use 0 as null sentinel
          ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + 4, 0L);
        } else if (funcValue instanceof FunctionReference) {
          final FunctionReference funcRef = (FunctionReference) funcValue;
          ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + 4, funcRef.getId());
        } else if (funcValue instanceof Long) {
          // Already an ID
          ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + 4, (Long) funcValue);
        } else {
          throw new IllegalArgumentException(
              "FUNCREF value must be FunctionReference or Long, got: " + funcValue.getClass());
        }
        break;

      case EXTERNREF:
        ptr.set(ValueLayout.JAVA_INT, offset, 6); // tag - ExternRef uses tag 6
        final Object externValue = value.getValue();
        if (externValue == null) {
          // Null externref - use 0 as null sentinel
          ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + 4, 0L);
        } else if (externValue instanceof ExternRef) {
          final ExternRef<?> externRef = (ExternRef<?>) externValue;
          final long externId = externRef.getId();
          // Register in global registry for later lookup
          EXTERN_REF_REGISTRY.put(externId, externRef);
          registeredExternRefIds.add(externId);
          ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + 4, externId);
        } else {
          // Wrap raw object in ExternRef
          final ExternRef<Object> newRef = ExternRef.of(externValue);
          final long externId = newRef.getId();
          EXTERN_REF_REGISTRY.put(externId, newRef);
          registeredExternRefIds.add(externId);
          ptr.set(ValueLayout.JAVA_LONG_UNALIGNED, offset + 4, externId);
        }
        break;

      default:
        throw new IllegalArgumentException("Unsupported WasmValue type: " + value.getType());
    }
  }

  /**
   * Unmarshals a WasmValue from native memory.
   *
   * @param ptr pointer to the array
   * @param index index in the array
   * @return the unmarshaled WasmValue
   */
  private static WasmValue unmarshalWasmValue(final MemorySegment ptr, final int index) {
    final long offset = index * 20L;
    final int tag = ptr.get(ValueLayout.JAVA_INT, offset);

    switch (tag) {
      case 0: // I32
        final int i32Val = ptr.get(ValueLayout.JAVA_INT, offset + 4);
        return WasmValue.i32(i32Val);

      case 1: // I64
        final long i64Val = ptr.get(ValueLayout.JAVA_LONG_UNALIGNED, offset + 4);
        return WasmValue.i64(i64Val);

      case 2: // F32
        final float f32Val = ptr.get(ValueLayout.JAVA_FLOAT, offset + 4);
        return WasmValue.f32(f32Val);

      case 3: // F64
        final double f64Val = ptr.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset + 4);
        return WasmValue.f64(f64Val);

      case 4: // V128
        final byte[] v128Bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
          v128Bytes[i] = ptr.get(ValueLayout.JAVA_BYTE, offset + 4 + i);
        }
        return WasmValue.v128(v128Bytes);

      case 5: // FUNCREF
        final long funcId = ptr.get(ValueLayout.JAVA_LONG_UNALIGNED, offset + 4);
        if (funcId == 0L) {
          // Null funcref
          return WasmValue.funcref((Object) null);
        }
        // Look up in PanamaFunctionReference registry
        final FunctionReference funcRef = PanamaFunctionReference.getFunctionReferenceById(funcId);
        if (funcRef != null) {
          return WasmValue.funcref(funcRef);
        }
        // Return ID if not found in registry (may be a table index from native)
        return WasmValue.funcref(funcId);

      case 6: // EXTERNREF
        final long externId = ptr.get(ValueLayout.JAVA_LONG_UNALIGNED, offset + 4);
        if (externId == 0L) {
          // Null externref
          return WasmValue.externref((Object) null);
        }
        // Look up in registry
        final ExternRef<?> externRef = EXTERN_REF_REGISTRY.get(externId);
        if (externRef != null) {
          // Return the wrapped value, not the ExternRef wrapper itself
          // This ensures round-trip consistency for comparison
          return WasmValue.externref(externRef.get());
        }
        // Return ID if not found in registry (may be from native side)
        return WasmValue.externref(externId);

      default:
        throw new IllegalArgumentException("Unknown WasmValue tag: " + tag);
    }
  }

  /**
   * Creates the resource handle with the instance's cleanup logic.
   *
   * @return the resource handle
   */
  private NativeResourceHandle createResourceHandle() {
    final MemorySegment instanceHandle = this.nativeInstance;
    return new NativeResourceHandle(
        "PanamaInstance",
        () -> {
          disposed.set(true);
          functionNameCache.clear();
          // Clean up ExternRef entries registered by this instance
          for (final Long id : registeredExternRefIds) {
            EXTERN_REF_REGISTRY.remove(id);
          }
          registeredExternRefIds.clear();
          if (callArena != null && callArena.scope().isAlive()) {
            callArena.close();
          }
          if (nativeInstance != null && !nativeInstance.equals(MemorySegment.NULL)) {
            NATIVE_INSTANCE_BINDINGS.instanceDestroy(nativeInstance);
          }
        },
        this,
        () -> {
          if (instanceHandle != null && !instanceHandle.equals(MemorySegment.NULL)) {
            NATIVE_INSTANCE_BINDINGS.instanceDestroy(instanceHandle);
          }
        });
  }

  /**
   * Ensures the instance is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
    if (disposed.get()) {
      throw new IllegalStateException("Instance has been disposed");
    }
  }

  /**
   * Maps native type code to WasmValueType.
   *
   * @param typeCode the type code from native
   * @return the WasmValueType
   */
  private static WasmValueType mapNativeTypeToWasmValueType(final int typeCode) {
    switch (typeCode) {
      case 0:
        return WasmValueType.I32;
      case 1:
        return WasmValueType.I64;
      case 2:
        return WasmValueType.F32;
      case 3:
        return WasmValueType.F64;
      case 4:
        return WasmValueType.V128;
      case 5:
        return WasmValueType.FUNCREF;
      case 6:
        return WasmValueType.EXTERNREF;
      default:
        LOGGER.warning("Unknown type code: " + typeCode);
        return WasmValueType.I32; // Default fallback
    }
  }

  // Memory delegation methods - called by PanamaMemory to ensure single store context

  /**
   * Gets the size of a memory in pages.
   *
   * @param memory the memory to query
   * @return size in pages
   */
  long getMemorySize(final PanamaMemory memory) {
    ensureNotClosed();
    try (final Arena tempArena = Arena.ofConfined()) {
      final MemorySegment nameSegment =
          tempArena.allocateFrom(memory.getMemoryName(), java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment sizeOut = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          NATIVE_INSTANCE_BINDINGS.instanceGetMemorySizePages(
              nativeInstance, store.getNativeStore(), nameSegment, sizeOut);

      if (result != 0) {
        throw new RuntimeException(
            "Failed to get memory size: " + PanamaErrorMapper.getErrorDescription(result));
      }

      return sizeOut.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  /**
   * Gets the maximum size of a memory in pages.
   *
   * @param memory the memory to query
   * @return maximum size in pages, or -1 if unlimited
   */
  int getMemoryMaxSize(final PanamaMemory memory) {
    ensureNotClosed();
    try (final Arena tempArena = Arena.ofConfined()) {
      final MemorySegment nameSegment =
          tempArena.allocateFrom(memory.getMemoryName(), java.nio.charset.StandardCharsets.UTF_8);

      // First get the memory pointer
      final MemorySegment memoryPtr =
          NATIVE_INSTANCE_BINDINGS.instanceGetMemoryByName(
              nativeInstance, store.getNativeStore(), nameSegment);

      if (memoryPtr == null || memoryPtr.equals(MemorySegment.NULL)) {
        return -1; // Memory not found
      }

      // Now get the max size
      final MemorySegment maxSizeOut = tempArena.allocate(ValueLayout.JAVA_LONG);
      final int result =
          NATIVE_MEMORY_BINDINGS.panamaMemoryGetMaximum(
              memoryPtr, store.getNativeStore(), maxSizeOut);

      if (result != 0) {
        return -1; // Failed to get max size
      }

      final long maxSize = maxSizeOut.get(ValueLayout.JAVA_LONG, 0);
      // Return as int, clamping if necessary (though unlikely for page counts)
      return maxSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxSize;
    }
  }

  /**
   * Grows a memory by the specified number of pages.
   *
   * @param memory the memory to grow
   * @param pages number of pages to grow
   * @return previous size in pages, or -1 if growth failed
   */
  long growMemory(final PanamaMemory memory, final long pages) {
    ensureNotClosed();
    try (final Arena tempArena = Arena.ofConfined()) {
      final MemorySegment nameSegment =
          tempArena.allocateFrom(memory.getMemoryName(), java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment previousPagesOut = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          NATIVE_INSTANCE_BINDINGS.instanceGrowMemory(
              nativeInstance, store.getNativeStore(), nameSegment, pages, previousPagesOut);

      if (result != 0) {
        return -1L; // Growth failed
      }

      return previousPagesOut.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  /**
   * Reads bytes from memory.
   *
   * @param memory the memory to read from
   * @param offset offset in memory
   * @param dest destination array
   * @param destOffset offset in destination array
   * @param length number of bytes to read
   */
  void readMemoryBytes(
      final PanamaMemory memory,
      final int offset,
      final byte[] dest,
      final int destOffset,
      final int length)
  {
    ensureNotClosed();
    try (final Arena tempArena = Arena.ofConfined()) {
      final MemorySegment nameSegment =
          tempArena.allocateFrom(memory.getMemoryName(), java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment buffer = tempArena.allocate(length);

      final int result =
          NATIVE_INSTANCE_BINDINGS.instanceReadMemoryBytes(
              nativeInstance, store.getNativeStore(), nameSegment, offset, length, buffer);

      if (result != 0) {
        throw new RuntimeException(
            "Failed to read bytes: " + PanamaErrorMapper.getErrorDescription(result));
      }

      // Copy from native buffer to Java array
      MemorySegment.copy(buffer, ValueLayout.JAVA_BYTE, 0, dest, destOffset, length);
    }
  }

  /**
   * Writes bytes to memory.
   *
   * @param memory the memory to write to
   * @param offset offset in memory
   * @param src source array
   * @param srcOffset offset in source array
   * @param length number of bytes to write
   */
  void writeMemoryBytes(
      final PanamaMemory memory,
      final int offset,
      final byte[] src,
      final int srcOffset,
      final int length)
  {
    ensureNotClosed();
    try (final Arena tempArena = Arena.ofConfined()) {
      final MemorySegment nameSegment =
          tempArena.allocateFrom(memory.getMemoryName(), java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment buffer = tempArena.allocate(length);

      // Copy from Java array to native buffer
      MemorySegment.copy(src, srcOffset, buffer, ValueLayout.JAVA_BYTE, 0, length);

      final int result =
          NATIVE_INSTANCE_BINDINGS.instanceWriteMemoryBytes(
              nativeInstance, store.getNativeStore(), nameSegment, offset, length, buffer);

      if (result != 0) {
        throw new RuntimeException(
            "Failed to write bytes: " + PanamaErrorMapper.getErrorDescription(result));
      }
    }
  }

  /**
   * Gets a ByteBuffer view of memory.
   *
   * @param memory the memory to get buffer for
   * @return ByteBuffer view
   */
  ByteBuffer getMemoryBuffer(final PanamaMemory memory) {
    ensureNotClosed();
    try (final Arena tempArena = Arena.ofConfined()) {
      final MemorySegment nameSegment =
          tempArena.allocateFrom(memory.getMemoryName(), java.nio.charset.StandardCharsets.UTF_8);
      // Get memory size in bytes
      final MemorySegment sizeOut = tempArena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          NATIVE_INSTANCE_BINDINGS.instanceGetMemorySizeBytes(
              nativeInstance, store.getNativeStore(), nameSegment, sizeOut);

      if (result != 0) {
        throw new RuntimeException(
            "Failed to get memory size: " + PanamaErrorMapper.getErrorDescription(result));
      }

      final long sizeBytes = sizeOut.get(ValueLayout.JAVA_LONG, 0);

      // Allocate a buffer and read all memory into it
      final byte[] buffer = new byte[(int) sizeBytes];
      readMemoryBytes(memory, 0, buffer, 0, (int) sizeBytes);

      return java.nio.ByteBuffer.wrap(buffer).asReadOnlyBuffer();
    }
  }

}
