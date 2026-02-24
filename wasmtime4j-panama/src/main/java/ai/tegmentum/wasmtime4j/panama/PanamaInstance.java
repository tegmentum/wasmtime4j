package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.ExternRef;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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

  /**
   * Thread-local arena pool for function calls — eliminates per-call Arena allocation.
   *
   * <p><strong>Lifecycle:</strong> Each thread lazily creates a confined {@link Arena} on first WASM
   * call. The arena is reused across all subsequent calls on that thread. Because {@code
   * ThreadLocal} entries are only reclaimed when the thread terminates, long-lived threads (e.g.
   * thread-pool workers) retain their arena indefinitely. This is a deliberate performance
   * trade-off: the per-thread memory footprint is small (typically &lt;1 KB) and the allocation
   * savings are significant in high-throughput call loops.
   *
   * <p>Call {@link #clearCallContext()} to explicitly release a thread's arena when the thread is
   * known to be done with WASM calls (e.g., before returning a thread to a pool).
   */
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

  /**
   * Releases the current thread's call context arena, freeing its off-heap memory.
   *
   * <p>This is useful when a thread-pool worker is about to be returned to the pool and will not
   * make further WASM calls for a while. A new context will be lazily created on the next call.
   */
  public static void clearCallContext() {
    final CallContext ctx = CALL_CONTEXT.get();
    if (ctx != null) {
      ctx.arena.close();
      CALL_CONTEXT.remove();
    }
  }

  private final PanamaModule module;
  private final PanamaStore store;
  private final MemorySegment nativeInstance;
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
    final WasmValueType type = WasmValueType.fromNativeTypeCode(typeCode);

    // Return an instance-bound global that uses instance-specific methods
    return Optional.of(new PanamaInstanceGlobal(this, store, name, type, mutable));
  }

  @Override
  public Optional<Tag> getTag(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    try (final Arena tagArena = Arena.ofConfined()) {
      final MemorySegment nameSegment =
          tagArena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment tagPtr =
          NATIVE_INSTANCE_BINDINGS.instanceGetTagByName(
              nativeInstance, store.getNativeStore(), nameSegment);
      if (tagPtr == null || tagPtr.equals(MemorySegment.NULL)) {
        return Optional.empty();
      }
      return Optional.of(new PanamaTag(tagPtr, store.getNativeStore()));
    }
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
  public Optional<WasmMemory> getSharedMemory(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    ensureNotClosed();

    try (final Arena arena = Arena.ofConfined()) {
      final java.lang.foreign.MemorySegment nameSegment =
          arena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8);
      final java.lang.foreign.MemorySegment result =
          NATIVE_INSTANCE_BINDINGS.instanceGetSharedMemoryByName(
              nativeInstance, store.getNativeStore(), nameSegment);
      if (result == null || result.equals(java.lang.foreign.MemorySegment.NULL)) {
        return Optional.empty();
      }
      return Optional.of(new PanamaMemory(name, this));
    }
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

  @Override
  public Optional<Extern> getExport(final String name) {
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
      switch (exportKind) {
        case 1: // Function
          return getFunction(name).map(f -> new PanamaExternFunc(MemorySegment.NULL, store));
        case 2: // Global
          return getGlobal(name).map(g -> new PanamaExternGlobal(MemorySegment.NULL, store));
        case 3: // Memory
          return getMemory(name).map(m -> new PanamaExternMemory(MemorySegment.NULL, store));
        case 4: // Table
          return getTable(name).map(t -> new PanamaExternTable(MemorySegment.NULL, store));
        default:
          return Optional.empty();
      }
    } catch (final Exception e) {
      LOGGER.warning("Error getting export: " + name + " - " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<ai.tegmentum.wasmtime4j.Extern> getExport(
      final ai.tegmentum.wasmtime4j.Store store,
      final ai.tegmentum.wasmtime4j.ModuleExport moduleExport)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (moduleExport == null) {
      throw new IllegalArgumentException("ModuleExport cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalStateException("Store must be a PanamaStore instance");
    }

    final PanamaStore panamaStore = (PanamaStore) store;

    try (final Arena exportArena = Arena.ofConfined()) {
      final MemorySegment outHandle = exportArena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outType = exportArena.allocate(ValueLayout.JAVA_INT);

      final MemorySegment moduleExportPtr =
          MemorySegment.ofAddress(moduleExport.nativeHandle());

      final int result =
          NATIVE_INSTANCE_BINDINGS.panamaInstanceGetModuleExport(
              nativeInstance, panamaStore.getNativeStore(), moduleExportPtr, outHandle, outType);

      if (result != 0) {
        return Optional.empty();
      }

      final MemorySegment externHandle = outHandle.get(ValueLayout.ADDRESS, 0);
      final int externType = outType.get(ValueLayout.JAVA_INT, 0);

      if (externHandle.equals(MemorySegment.NULL) || externHandle.address() == 0
          || externType < 0) {
        return Optional.empty();
      }

      return Optional.of(createExternFromNative(externHandle, externType, panamaStore));
    } catch (final Exception e) {
      LOGGER.warning("Error getting module export: " + e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Creates the appropriate Extern wrapper from a native handle and type code.
   *
   * @param handle the native extern handle
   * @param nativeType the native type code (0=Func, 1=Global, 2=Table, 3=Memory)
   * @param panamaStore the Panama store
   * @return the Extern wrapper
   */
  private static ai.tegmentum.wasmtime4j.Extern createExternFromNative(
      final MemorySegment handle, final int nativeType, final PanamaStore panamaStore) {
    switch (nativeType) {
      case 0:
        return new PanamaExternFunc(handle, panamaStore);
      case 1:
        return new PanamaExternGlobal(handle, panamaStore);
      case 2:
        return new PanamaExternTable(handle, panamaStore);
      case 3:
        return new PanamaExternMemory(handle, panamaStore);
      default:
        LOGGER.warning("Unknown native extern type: " + nativeType);
        return new PanamaExternFunc(handle, panamaStore);
    }
  }

  /**
   * Checks if the instance has a function export with the given name.
   *
   * @param name the name to check
   * @return true if a function export with this name exists, false otherwise
   * @throws IllegalArgumentException if name is null
   */
  boolean hasFunction(final String name) {
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
  boolean hasMemory(final String name) {
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
  boolean hasTable(final String name) {
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
  boolean hasGlobal(final String name) {
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
        WasmValueMarshaller.marshalWasmValue(
            params[i], paramsSegment, i, this::registerExternRef);
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
      results[i] = WasmValueMarshaller.unmarshalWasmValue(resultsSegment, i, EXTERN_REF_REGISTRY::get);
    }

    return results;
  }

  /**
   * Gets all exported function names.
   *
   * @return list of exported function names
   */
  List<String> getFunctionNames() {
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
  List<String> getMemoryNames() {
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
  List<String> getTableNames() {
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
  List<String> getGlobalNames() {
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
  int getFunctionCount() {
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
  int getMemoryCount() {
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
  int getTableCount() {
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
  int getGlobalCount() {
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
  public boolean isValid() {
    return !resourceHandle.isClosed() && !disposed.get();
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
   * Registers an ExternRef in the global registry and tracks it for cleanup.
   *
   * @param id the ExternRef identifier
   * @param ref the ExternRef to register
   */
  private void registerExternRef(final Long id, final ExternRef<?> ref) {
    EXTERN_REF_REGISTRY.put(id, ref);
    registeredExternRefIds.add(id);
  }

  /**
   * Creates the resource handle with the instance's cleanup logic.
   *
   * @return the resource handle
   */
  private NativeResourceHandle createResourceHandle() {
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
          // Note: Wasmtime Instance is a Copy type (index into Store slab).
          // No native destructor is needed — the Store owns the instance data.
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
   * Grows memory asynchronously through the async resource limiter.
   *
   * @param memory the memory to grow
   * @param pages the number of pages to grow by
   * @return the previous size in pages, or -1 if growth failed
   */
  long growMemoryAsync(final PanamaMemory memory, final long pages) {
    ensureNotClosed();
    try (final Arena tempArena = Arena.ofConfined()) {
      final MemorySegment nameSegment =
          tempArena.allocateFrom(memory.getMemoryName(), java.nio.charset.StandardCharsets.UTF_8);

      // Get the native memory ptr from the instance export
      final MemorySegment memPtr = NATIVE_INSTANCE_BINDINGS.instanceGetMemoryByName(
          nativeInstance, store.getNativeStore(), nameSegment);
      if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
        return -1L;
      }

      final MemorySegment previousPagesOut = tempArena.allocate(ValueLayout.JAVA_LONG);
      final NativeMemoryBindings memBindings = NativeMemoryBindings.getInstance();
      final int result = memBindings.panamaMemoryGrowAsync(
          memPtr, store.getNativeStore(), pages, previousPagesOut);

      if (result != 0) {
        return -1L;
      }
      return previousPagesOut.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  /**
   * Converts a named function export to its raw funcref pointer value.
   *
   * @param functionName the name of the function export
   * @return the raw funcref value
   * @throws ai.tegmentum.wasmtime4j.exception.WasmException if conversion fails
   */
  long funcToRaw(final String functionName) throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    try (final Arena tempArena = Arena.ofConfined()) {
      final MemorySegment nameSegment =
          tempArena.allocateFrom(functionName, java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment funcPtrOut = tempArena.allocate(ValueLayout.ADDRESS);

      // Get the func ptr from instance export
      final int getResult = NATIVE_INSTANCE_BINDINGS.funcGet(
          nativeInstance, store.getNativeStore(), nameSegment, funcPtrOut);
      if (getResult != 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Failed to get function pointer for: " + functionName);
      }
      final MemorySegment funcPtr = funcPtrOut.get(ValueLayout.ADDRESS, 0);

      try {
        // Convert to raw funcref
        return NATIVE_INSTANCE_BINDINGS.funcToRaw(funcPtr, store.getNativeStore());
      } finally {
        // Clean up the function handle
        NATIVE_INSTANCE_BINDINGS.funcDestroy(funcPtr);
      }
    }
  }

}
