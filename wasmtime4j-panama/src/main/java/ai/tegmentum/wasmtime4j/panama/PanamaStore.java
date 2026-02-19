package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExternRef;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.config.ResourceLimiter;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Store.
 *
 * <p>A Store represents an execution context for WebAssembly instances.
 *
 * @since 1.0.0
 */
public final class PanamaStore implements Store {
  private static final Logger LOGGER = Logger.getLogger(PanamaStore.class.getName());
  private static final NativeStoreBindings NATIVE_BINDINGS = NativeStoreBindings.getInstance();
  private static final NativeInstanceBindings INSTANCE_BINDINGS =
      NativeInstanceBindings.getInstance();
  private static final NativeMemoryBindings MEMORY_BINDINGS = NativeMemoryBindings.getInstance();

  // Epoch callback infrastructure
  private static final AtomicLong EPOCH_CALLBACK_ID_COUNTER = new AtomicLong(1);
  private static final ConcurrentHashMap<Long, EpochDeadlineCallback> EPOCH_CALLBACKS =
      new ConcurrentHashMap<>();
  private static final MemorySegment EPOCH_CALLBACK_STUB;
  private static final FunctionDescriptor EPOCH_CALLBACK_DESCRIPTOR =
      FunctionDescriptor.of(
          ValueLayout.JAVA_LONG, // return: delta (positive) or trap (negative)
          ValueLayout.JAVA_LONG, // callback_id
          ValueLayout.JAVA_LONG); // epoch (unused, reserved)

  static {
    MemorySegment stub = null;
    try {
      // Create the upcall stub for epoch deadline callbacks
      final MethodHandle epochCallbackHandle =
          MethodHandles.lookup()
              .findStatic(
                  PanamaStore.class,
                  "epochDeadlineCallbackStub",
                  MethodType.methodType(long.class, long.class, long.class));
      stub =
          Linker.nativeLinker()
              .upcallStub(epochCallbackHandle, EPOCH_CALLBACK_DESCRIPTOR, Arena.global());
    } catch (final NoSuchMethodException | IllegalAccessException e) {
      LOGGER.severe("Failed to create epoch callback stub: " + e.getMessage());
    }
    EPOCH_CALLBACK_STUB = stub;
  }

  /**
   * Static callback method invoked from native code when epoch deadline is reached.
   *
   * @param callbackId the callback identifier
   * @param epoch the current epoch value (reserved for future use)
   * @return positive delta to continue execution, or negative value to trap
   */
  private static long epochDeadlineCallbackStub(final long callbackId, final long epoch) {
    LOGGER.fine("Epoch callback stub invoked: callbackId=" + callbackId + ", epoch=" + epoch);
    final EpochDeadlineCallback callback = EPOCH_CALLBACKS.get(callbackId);
    if (callback == null) {
      LOGGER.warning("Epoch callback not found for ID: " + callbackId);
      return -1; // Trap - callback not found
    }

    try {
      final EpochDeadlineAction action = callback.onEpochDeadline(epoch);
      if (action == null) {
        LOGGER.fine("Epoch callback returned null action - trapping");
        return -1; // Trap on null action
      }
      if (action.shouldContinue()) {
        final long delta = action.getDeltaTicks();
        LOGGER.fine("Epoch callback returning continue with delta=" + delta);
        return delta; // Continue with this delta
      } else {
        LOGGER.fine("Epoch callback returning trap");
        return -1; // Trap
      }
    } catch (final Exception e) {
      LOGGER.warning("Epoch callback threw exception: " + e.getMessage());
      return -1; // Trap on exception
    }
  }

  // Call hook callback infrastructure
  private static final AtomicLong CALL_HOOK_CALLBACK_ID_COUNTER = new AtomicLong(1);
  private static final ConcurrentHashMap<Long, ai.tegmentum.wasmtime4j.func.CallHookHandler>
      CALL_HOOK_HANDLERS = new ConcurrentHashMap<>();
  private static final MemorySegment CALL_HOOK_STUB;
  private static final FunctionDescriptor CALL_HOOK_DESCRIPTOR =
      FunctionDescriptor.of(
          ValueLayout.JAVA_INT, // return: 0=OK, non-zero=trap
          ValueLayout.JAVA_LONG, // callback_id
          ValueLayout.JAVA_INT); // hook_type (0-3)

  static {
    MemorySegment callHookStub = null;
    try {
      final MethodHandle callHookHandle =
          MethodHandles.lookup()
              .findStatic(
                  PanamaStore.class,
                  "callHookCallbackStub",
                  MethodType.methodType(int.class, long.class, int.class));
      callHookStub =
          Linker.nativeLinker().upcallStub(callHookHandle, CALL_HOOK_DESCRIPTOR, Arena.global());
    } catch (final NoSuchMethodException | IllegalAccessException e) {
      LOGGER.severe("Failed to create call hook callback stub: " + e.getMessage());
    }
    CALL_HOOK_STUB = callHookStub;
  }

  /**
   * Static callback method invoked from native code on call hook transitions.
   *
   * @param callbackId the callback identifier
   * @param hookType the hook type (0=CallingWasm, 1=ReturningFromWasm, 2=CallingHost,
   *     3=ReturningFromHost)
   * @return 0 to continue execution, non-zero to trap
   */
  private static int callHookCallbackStub(final long callbackId, final int hookType) {
    LOGGER.fine("Call hook stub invoked: callbackId=" + callbackId + ", hookType=" + hookType);
    final ai.tegmentum.wasmtime4j.func.CallHookHandler handler = CALL_HOOK_HANDLERS.get(callbackId);
    if (handler == null) {
      LOGGER.warning("Call hook handler not found for ID: " + callbackId);
      return -1; // Trap - handler not found
    }

    try {
      final ai.tegmentum.wasmtime4j.func.CallHook hook =
          ai.tegmentum.wasmtime4j.func.CallHook.fromValue(hookType);
      handler.onCallHook(hook);
      return 0; // Continue execution
    } catch (final ai.tegmentum.wasmtime4j.exception.TrapException e) {
      LOGGER.fine("Call hook handler requested trap: " + e.getMessage());
      return -1; // Trap
    } catch (final Exception e) {
      LOGGER.warning("Call hook handler threw exception: " + e.getMessage());
      return -1; // Trap on unexpected exception
    }
  }

  // Resource limiter callback infrastructure
  private static final AtomicLong LIMITER_CALLBACK_ID_COUNTER = new AtomicLong(1);
  private static final ConcurrentHashMap<Long, ResourceLimiter> RESOURCE_LIMITERS =
      new ConcurrentHashMap<>();
  private static final MemorySegment MEMORY_GROWING_STUB;
  private static final MemorySegment TABLE_GROWING_STUB;
  private static final MemorySegment MEMORY_GROW_FAILED_STUB;
  private static final MemorySegment TABLE_GROW_FAILED_STUB;

  private static final FunctionDescriptor MEMORY_GROWING_DESCRIPTOR =
      FunctionDescriptor.of(
          ValueLayout.JAVA_INT, // return: non-zero to allow, zero to deny
          ValueLayout.JAVA_LONG, // callback_id
          ValueLayout.JAVA_LONG, // current_bytes
          ValueLayout.JAVA_LONG, // desired_bytes
          ValueLayout.JAVA_LONG); // maximum_bytes

  private static final FunctionDescriptor TABLE_GROWING_DESCRIPTOR =
      FunctionDescriptor.of(
          ValueLayout.JAVA_INT, // return: non-zero to allow, zero to deny
          ValueLayout.JAVA_LONG, // callback_id
          ValueLayout.JAVA_INT, // current_elements
          ValueLayout.JAVA_INT, // desired_elements
          ValueLayout.JAVA_INT); // maximum_elements

  private static final FunctionDescriptor GROW_FAILED_DESCRIPTOR =
      FunctionDescriptor.ofVoid(
          ValueLayout.JAVA_LONG, // callback_id
          ValueLayout.ADDRESS); // error message (C string)

  static {
    MemorySegment memGrowingStub = null;
    MemorySegment tblGrowingStub = null;
    MemorySegment memFailedStub = null;
    MemorySegment tblFailedStub = null;
    try {
      final MethodHandles.Lookup lookup = MethodHandles.lookup();
      final Linker linker = Linker.nativeLinker();

      memGrowingStub =
          linker.upcallStub(
              lookup.findStatic(
                  PanamaStore.class,
                  "memoryGrowingStub",
                  MethodType.methodType(int.class, long.class, long.class, long.class, long.class)),
              MEMORY_GROWING_DESCRIPTOR,
              Arena.global());

      tblGrowingStub =
          linker.upcallStub(
              lookup.findStatic(
                  PanamaStore.class,
                  "tableGrowingStub",
                  MethodType.methodType(int.class, long.class, int.class, int.class, int.class)),
              TABLE_GROWING_DESCRIPTOR,
              Arena.global());

      memFailedStub =
          linker.upcallStub(
              lookup.findStatic(
                  PanamaStore.class,
                  "memoryGrowFailedStub",
                  MethodType.methodType(void.class, long.class, MemorySegment.class)),
              GROW_FAILED_DESCRIPTOR,
              Arena.global());

      tblFailedStub =
          linker.upcallStub(
              lookup.findStatic(
                  PanamaStore.class,
                  "tableGrowFailedStub",
                  MethodType.methodType(void.class, long.class, MemorySegment.class)),
              GROW_FAILED_DESCRIPTOR,
              Arena.global());
    } catch (final NoSuchMethodException | IllegalAccessException e) {
      LOGGER.severe("Failed to create resource limiter callback stubs: " + e.getMessage());
    }
    MEMORY_GROWING_STUB = memGrowingStub;
    TABLE_GROWING_STUB = tblGrowingStub;
    MEMORY_GROW_FAILED_STUB = memFailedStub;
    TABLE_GROW_FAILED_STUB = tblFailedStub;
  }

  private static int memoryGrowingStub(
      final long callbackId,
      final long currentBytes,
      final long desiredBytes,
      final long maximumBytes) {
    final ResourceLimiter limiter = RESOURCE_LIMITERS.get(callbackId);
    if (limiter == null) {
      LOGGER.warning("Resource limiter not found for ID: " + callbackId);
      return 0; // Deny if limiter not found
    }
    try {
      return limiter.memoryGrowing(currentBytes, desiredBytes, maximumBytes) ? 1 : 0;
    } catch (final Exception e) {
      LOGGER.warning("memoryGrowing callback threw exception: " + e.getMessage());
      return 0; // Deny on exception
    }
  }

  private static int tableGrowingStub(
      final long callbackId,
      final int currentElements,
      final int desiredElements,
      final int maximumElements) {
    final ResourceLimiter limiter = RESOURCE_LIMITERS.get(callbackId);
    if (limiter == null) {
      LOGGER.warning("Resource limiter not found for ID: " + callbackId);
      return 0; // Deny if limiter not found
    }
    try {
      return limiter.tableGrowing(currentElements, desiredElements, maximumElements) ? 1 : 0;
    } catch (final Exception e) {
      LOGGER.warning("tableGrowing callback threw exception: " + e.getMessage());
      return 0; // Deny on exception
    }
  }

  private static void memoryGrowFailedStub(final long callbackId, final MemorySegment errorMsg) {
    final ResourceLimiter limiter = RESOURCE_LIMITERS.get(callbackId);
    if (limiter == null) {
      return;
    }
    try {
      final String error =
          (errorMsg != null && !errorMsg.equals(MemorySegment.NULL))
              ? errorMsg.reinterpret(Long.MAX_VALUE).getString(0)
              : "unknown error";
      limiter.memoryGrowFailed(error);
    } catch (final Exception e) {
      LOGGER.warning("memoryGrowFailed callback threw exception: " + e.getMessage());
    }
  }

  private static void tableGrowFailedStub(final long callbackId, final MemorySegment errorMsg) {
    final ResourceLimiter limiter = RESOURCE_LIMITERS.get(callbackId);
    if (limiter == null) {
      return;
    }
    try {
      final String error =
          (errorMsg != null && !errorMsg.equals(MemorySegment.NULL))
              ? errorMsg.reinterpret(Long.MAX_VALUE).getString(0)
              : "unknown error";
      limiter.tableGrowFailed(error);
    } catch (final Exception e) {
      LOGGER.warning("tableGrowFailed callback threw exception: " + e.getMessage());
    }
  }

  private final PanamaEngine engine;
  private final Arena arena;
  private final MemorySegment nativeStore;
  private final AtomicReference<Object> userData = new AtomicReference<>();
  private final ArenaResourceManager resourceManager;
  private volatile PanamaCallbackRegistry callbackRegistry; // Lazy initialized
  private final NativeResourceHandle resourceHandle;

  // Epoch callback ID for this store (0 = no callback registered)
  // AtomicLong so the safety net lambda can capture it without preventing GC of PanamaStore
  private final AtomicLong epochCallbackId = new AtomicLong(0);

  // Resource limiter callback ID for this store (0 = no limiter registered)
  // AtomicLong so the safety net lambda can capture it without preventing GC of PanamaStore
  private final AtomicLong limiterCallbackId = new AtomicLong(0);

  /**
   * Creates a new Panama store.
   *
   * @param engine the engine that owns this store
   * @throws WasmException if store creation fails
   */
  public PanamaStore(final PanamaEngine engine) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (!engine.isValid()) {
      throw new IllegalStateException("Engine is not valid");
    }

    this.engine = engine;
    this.arena = Arena.ofConfined();

    // Create native store via Panama FFI
    this.nativeStore = NATIVE_BINDINGS.storeCreate(engine.getNativeEngine());

    if (this.nativeStore == null || this.nativeStore.equals(MemorySegment.NULL)) {
      arena.close();
      throw new WasmException("Failed to create native store");
    }

    // Create resource manager for host functions and other managed resources
    this.resourceManager = new ArenaResourceManager(arena, false);

    this.resourceHandle = createResourceHandle();

    LOGGER.fine("Created Panama store");
  }

  /**
   * Creates a new Panama store with resource limits.
   *
   * @param engine the engine that owns this store
   * @param limits the resource limits to apply
   * @throws WasmException if store creation fails
   */
  public PanamaStore(
      final PanamaEngine engine, final ai.tegmentum.wasmtime4j.config.StoreLimits limits)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (limits == null) {
      throw new IllegalArgumentException("Limits cannot be null");
    }
    if (!engine.isValid()) {
      throw new IllegalStateException("Engine is not valid");
    }

    this.engine = engine;
    this.arena = Arena.ofConfined();

    // Create native store via Panama FFI with limits
    final MemorySegment storePtr = arena.allocate(ValueLayout.ADDRESS);
    final int result =
        NATIVE_BINDINGS.storeCreateWithConfig(
            engine.getNativeEngine(),
            0, // fuel limit - 0 means no limit, can be set later with setFuel
            limits.getMemorySize(),
            0, // execution timeout - 0 means no timeout
            (int) limits.getInstances(),
            (int) limits.getTableElements(),
            0, // max functions - 0 means no limit
            storePtr);

    if (result != 0) {
      arena.close();
      throw PanamaErrorMapper.mapNativeError(result, "Failed to create native store with limits");
    }

    this.nativeStore = storePtr.get(ValueLayout.ADDRESS, 0);

    if (this.nativeStore == null || this.nativeStore.equals(MemorySegment.NULL)) {
      arena.close();
      throw new WasmException("Failed to create native store with limits");
    }

    // Create resource manager for host functions and other managed resources
    this.resourceManager = new ArenaResourceManager(arena, false);

    this.resourceHandle = createResourceHandle();

    LOGGER.fine("Created Panama store with limits: " + limits);
  }

  /**
   * Creates a new Panama store with explicit resource limits.
   *
   * @param engine the engine that owns this store
   * @param fuelLimit the initial fuel limit (0 means no limit)
   * @param memoryLimitBytes the maximum memory in bytes (0 means no limit)
   * @param executionTimeoutSeconds the execution timeout in seconds (0 means no timeout)
   * @throws WasmException if store creation fails
   */
  PanamaStore(
      final PanamaEngine engine,
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSeconds)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (!engine.isValid()) {
      throw new IllegalStateException("Engine is not valid");
    }

    this.engine = engine;
    this.arena = Arena.ofConfined();

    // Create native store via Panama FFI with resource limits
    final MemorySegment storePtr = arena.allocate(ValueLayout.ADDRESS);
    final int result =
        NATIVE_BINDINGS.storeCreateWithConfig(
            engine.getNativeEngine(),
            fuelLimit,
            memoryLimitBytes,
            executionTimeoutSeconds,
            0, // instances - 0 means no limit
            0, // table elements - 0 means no limit
            0, // max functions - 0 means no limit
            storePtr);

    if (result != 0) {
      arena.close();
      throw PanamaErrorMapper.mapNativeError(
          result, "Failed to create native store with resource limits");
    }

    this.nativeStore = storePtr.get(ValueLayout.ADDRESS, 0);

    if (this.nativeStore == null || this.nativeStore.equals(MemorySegment.NULL)) {
      arena.close();
      throw new WasmException("Failed to create native store with resource limits");
    }

    // Create resource manager for host functions and other managed resources
    this.resourceManager = new ArenaResourceManager(arena, false);

    this.resourceHandle = createResourceHandle();

    LOGGER.fine(
        "Created Panama store with resource limits - fuel: "
            + fuelLimit
            + ", memory: "
            + memoryLimitBytes
            + " bytes, timeout: "
            + executionTimeoutSeconds
            + "s");
  }

  /**
   * Private constructor for creating a store with a pre-created native store handle.
   *
   * @param engine the engine that owns this store
   * @param nativeStoreHandle the pre-created native store handle
   * @throws WasmException if store initialization fails
   */
  private PanamaStore(final PanamaEngine engine, final MemorySegment nativeStoreHandle)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (nativeStoreHandle == null || nativeStoreHandle.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native store handle cannot be null");
    }

    this.engine = engine;
    this.arena = Arena.ofConfined();
    this.nativeStore = nativeStoreHandle;

    // Create resource manager for host functions and other managed resources
    this.resourceManager = new ArenaResourceManager(arena, false);

    this.resourceHandle = createResourceHandle();

    LOGGER.fine("Created Panama store from pre-created native handle");
  }

  /**
   * Creates a new store that is compatible with the given module.
   *
   * <p><b>CRITICAL:</b> This method creates a Store that shares the exact same Engine Arc as the
   * module. This is required because Wasmtime's Instance::new() uses Arc::ptr_eq() to verify that
   * the Store and Module were created from the same Engine. Using {@link
   * PanamaEngine#createStore()} will create a Store with a different Arc clone, causing
   * "cross-Engine instantiation is not currently supported" errors.
   *
   * <p>Usage Example:
   *
   * <pre>{@code
   * try (PanamaEngine engine = new PanamaEngine()) {
   *   PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
   *
   *   // CORRECT: Use forModule to create a compatible store
   *   try (PanamaStore store = PanamaStore.forModule(module)) {
   *     PanamaInstance instance = (PanamaInstance) module.instantiate(store);
   *     // Instance created successfully
   *   }
   * }
   * }</pre>
   *
   * @param module the module to create a compatible store for
   * @return a new store that shares the same Engine Arc as the module
   * @throws WasmException if store creation fails
   * @throws IllegalArgumentException if module is null or not a PanamaModule
   */
  public static PanamaStore forModule(final ai.tegmentum.wasmtime4j.Module module)
      throws WasmException {
    if (module == null) {
      throw new IllegalArgumentException("module cannot be null");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("module must be a PanamaModule instance");
    }

    final PanamaModule panamaModule = (PanamaModule) module;
    if (!panamaModule.isValid()) {
      throw new IllegalStateException("Module is not valid");
    }

    final MemorySegment storeHandle =
        NATIVE_BINDINGS.storeCreateForModule(panamaModule.getNativeModule());
    if (storeHandle == null || storeHandle.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to create store for module");
    }

    final ai.tegmentum.wasmtime4j.Engine moduleEngine = panamaModule.getEngine();
    if (!(moduleEngine instanceof PanamaEngine)) {
      throw new WasmException("Module engine is not a PanamaEngine");
    }

    return new PanamaStore((PanamaEngine) moduleEngine, storeHandle);
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public Object getData() {
    return userData.get();
  }

  @Override
  public void setData(final Object data) {
    userData.set(data);
  }

  @Override
  public void setResourceLimiter(final ResourceLimiter limiter) throws WasmException {
    if (limiter == null) {
      throw new IllegalArgumentException("ResourceLimiter cannot be null");
    }
    ensureNotClosed();

    // Remove any previously registered limiter
    final long oldLimiterId = limiterCallbackId.get();
    if (oldLimiterId != 0) {
      RESOURCE_LIMITERS.remove(oldLimiterId);
      limiterCallbackId.set(0);
    }

    // Check if upcall stubs were created successfully
    if (MEMORY_GROWING_STUB == null || TABLE_GROWING_STUB == null) {
      throw new WasmException("Resource limiter callback infrastructure not initialized");
    }

    final long newCallbackId = LIMITER_CALLBACK_ID_COUNTER.getAndIncrement();
    RESOURCE_LIMITERS.put(newCallbackId, limiter);
    limiterCallbackId.set(newCallbackId);

    final int result =
        NATIVE_BINDINGS.storeSetResourceLimiter(
            nativeStore,
            newCallbackId,
            MEMORY_GROWING_STUB,
            TABLE_GROWING_STUB,
            MEMORY_GROW_FAILED_STUB != null ? MEMORY_GROW_FAILED_STUB : MemorySegment.NULL,
            TABLE_GROW_FAILED_STUB != null ? TABLE_GROW_FAILED_STUB : MemorySegment.NULL);

    if (result != 0) {
      RESOURCE_LIMITERS.remove(newCallbackId);
      limiterCallbackId.set(0);
      throw PanamaErrorMapper.mapNativeError(result, "Failed to configure resource limiter");
    }
  }

  @Override
  public void setFuel(final long fuel) throws WasmException {
    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel cannot be negative");
    }
    ensureNotClosed();

    try {
      final MethodHandle setFuelHandle = NATIVE_BINDINGS.getPanamaStoreSetFuel();
      if (setFuelHandle == null) {
        throw new WasmException("Panama store set fuel function not available");
      }

      final int result = (int) setFuelHandle.invoke(nativeStore, fuel);

      if (result != 0) {
        throw new WasmException("Failed to set fuel");
      }
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error setting fuel: " + e.getMessage(), e);
    }
  }

  @Override
  public long getFuel() throws WasmException {
    ensureNotClosed();

    try {
      final MethodHandle getFuelHandle = NATIVE_BINDINGS.getPanamaStoreGetFuel();
      if (getFuelHandle == null) {
        throw new WasmException("Panama store get fuel function not available");
      }

      final MemorySegment fuelSegment = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = (int) getFuelHandle.invoke(nativeStore, fuelSegment);

      if (result != 0) {
        throw new WasmException("Failed to get fuel");
      }

      return fuelSegment.get(ValueLayout.JAVA_LONG, 0);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error getting fuel: " + e.getMessage(), e);
    }
  }

  @Override
  public void addFuel(final long fuel) throws WasmException {
    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel cannot be negative");
    }
    ensureNotClosed();

    try {
      final MethodHandle addFuelHandle = NATIVE_BINDINGS.getPanamaStoreAddFuel();
      if (addFuelHandle == null) {
        throw new WasmException("Panama store add fuel function not available");
      }

      final int result = (int) addFuelHandle.invoke(nativeStore, fuel);

      if (result != 0) {
        throw new WasmException("Failed to add fuel");
      }
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error adding fuel: " + e.getMessage(), e);
    }
  }

  @Override
  public void setEpochDeadline(final long ticks) throws WasmException {
    ensureNotClosed();

    try {
      final MethodHandle setEpochDeadlineHandle = NATIVE_BINDINGS.getPanamaStoreSetEpochDeadline();
      if (setEpochDeadlineHandle == null) {
        throw new WasmException("Panama store set epoch deadline function not available");
      }

      final int result = (int) setEpochDeadlineHandle.invoke(nativeStore, ticks);

      if (result != 0) {
        throw new WasmException("Failed to set epoch deadline");
      }
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error setting epoch deadline: " + e.getMessage(), e);
    }
  }

  @Override
  public long consumeFuel(final long fuel) throws WasmException {
    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel cannot be negative");
    }
    ensureNotClosed();

    try {
      final MethodHandle consumeFuelHandle = NATIVE_BINDINGS.getPanamaStoreConsumeFuel();
      if (consumeFuelHandle == null) {
        throw new WasmException("Panama store consume fuel function not available");
      }

      final MemorySegment consumedSegment = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = (int) consumeFuelHandle.invoke(nativeStore, fuel, consumedSegment);

      if (result != 0) {
        throw new WasmException("Failed to consume fuel");
      }

      return consumedSegment.get(ValueLayout.JAVA_LONG, 0);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error consuming fuel: " + e.getMessage(), e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmFunction createHostFunction(
      final String name,
      final ai.tegmentum.wasmtime4j.type.FunctionType functionType,
      final ai.tegmentum.wasmtime4j.func.HostFunction implementation)
      throws WasmException {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    if (functionType == null) {
      throw new IllegalArgumentException("functionType cannot be null");
    }
    if (implementation == null) {
      throw new IllegalArgumentException("implementation cannot be null");
    }
    ensureNotClosed();

    // Adapt the HostFunction interface to PanamaHostFunction.HostFunctionCallback
    final PanamaHostFunction.HostFunctionCallback callback = implementation::execute;

    // Create the Panama host function with implementation and store reference for caller context
    final PanamaHostFunction hostFunction =
        new PanamaHostFunction(name, functionType, callback, implementation, this, resourceManager);

    LOGGER.fine("Created host function '" + name + "' in store");
    return hostFunction;
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmGlobal createGlobal(
      final ai.tegmentum.wasmtime4j.WasmValueType valueType,
      final boolean isMutable,
      final ai.tegmentum.wasmtime4j.WasmValue initialValue)
      throws WasmException {
    if (valueType == null) {
      throw new IllegalArgumentException("Value type cannot be null");
    }
    if (initialValue == null) {
      throw new IllegalArgumentException("Initial value cannot be null");
    }
    if (initialValue.getType() != valueType) {
      throw new IllegalArgumentException(
          "Initial value type "
              + initialValue.getType()
              + " does not match global type "
              + valueType);
    }
    ensureNotClosed();

    try {
      final MethodHandle createHandle = MEMORY_BINDINGS.getPanamaGlobalCreate();
      if (createHandle == null) {
        throw new WasmException("Panama global creation function not available");
      }

      // Extract values based on type
      int i32Value = 0;
      long i64Value = 0L;
      double f32Value = 0.0;
      double f64Value = 0.0;
      int refIdPresent = 0;
      long refId = 0L;
      MemorySegment v128BytesPtr = MemorySegment.NULL;

      switch (valueType) {
        case I32:
          i32Value = (Integer) initialValue.getValue();
          break;
        case I64:
          i64Value = (Long) initialValue.getValue();
          break;
        case F32:
          f32Value = ((Float) initialValue.getValue()).doubleValue();
          break;
        case F64:
          f64Value = (Double) initialValue.getValue();
          break;
        case V128:
          final byte[] v128Bytes = (byte[]) initialValue.getValue();
          if (v128Bytes != null && v128Bytes.length == 16) {
            v128BytesPtr = arena.allocate(16);
            MemorySegment.copy(v128Bytes, 0, v128BytesPtr, ValueLayout.JAVA_BYTE, 0, 16);
          }
          break;
        case FUNCREF:
          if (initialValue.getValue() != null) {
            refIdPresent = 1;
            final Object funcVal = initialValue.getValue();
            if (funcVal instanceof FunctionReference) {
              refId = ((FunctionReference) funcVal).getId();
            } else if (funcVal instanceof Long) {
              refId = (Long) funcVal;
            } else {
              throw new IllegalArgumentException(
                  "FUNCREF value must be FunctionReference or Long, got: " + funcVal.getClass());
            }
          }
          break;
        case EXTERNREF:
          if (initialValue.getValue() != null) {
            refIdPresent = 1;
            final Object externVal = initialValue.getValue();
            if (externVal instanceof ExternRef) {
              refId = ((ExternRef<?>) externVal).getId();
            } else if (externVal instanceof Long) {
              refId = (Long) externVal;
            } else {
              throw new IllegalArgumentException(
                  "EXTERNREF value must be ExternRef or Long, got: " + externVal.getClass());
            }
          }
          break;
        case ANYREF:
        case EQREF:
        case I31REF:
        case STRUCTREF:
        case ARRAYREF:
        case NULLREF:
        case NULLFUNCREF:
        case NULLEXTERNREF:
          // GC reference types: null initial value (refIdPresent defaults to 0)
          break;
        default:
          throw new IllegalArgumentException("Unsupported global type: " + valueType);
      }

      final MemorySegment globalPtr = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              createHandle.invoke(
                  nativeStore,
                  valueType.toNativeTypeCode(),
                  isMutable ? 1 : 0,
                  i32Value,
                  i64Value,
                  f32Value,
                  f64Value,
                  refIdPresent,
                  refId,
                  v128BytesPtr,
                  MemorySegment.NULL, // name = null
                  globalPtr);

      if (result != 0) {
        final String nativeError =
            ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper.retrieveNativeErrorMessage();
        throw new WasmException(
            "Failed to create global"
                + (nativeError != null ? ": " + nativeError : " (error code: " + result + ")"));
      }

      final MemorySegment nativeGlobalPtr = globalPtr.get(ValueLayout.ADDRESS, 0);
      if (nativeGlobalPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Created global pointer is null");
      }

      return new PanamaGlobal(nativeGlobalPtr, this);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error creating global: " + e.getMessage(), e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmTable createTable(
      final ai.tegmentum.wasmtime4j.WasmValueType elementType,
      final int initialSize,
      final int maxSize)
      throws WasmException {
    if (elementType == null) {
      throw new IllegalArgumentException("Element type cannot be null");
    }
    if (initialSize < 0) {
      throw new IllegalArgumentException("Initial size cannot be negative");
    }
    if (maxSize < -1) {
      throw new IllegalArgumentException("Max size must be -1 (unlimited) or non-negative");
    }
    ensureNotClosed();

    try {
      final MethodHandle createHandle = MEMORY_BINDINGS.getPanamaTableCreate();
      if (createHandle == null) {
        throw new WasmException("Panama table creation function not available");
      }

      // Convert element type to native code
      final int elementTypeCode;
      if (elementType == ai.tegmentum.wasmtime4j.WasmValueType.FUNCREF) {
        elementTypeCode = 5; // FUNCREF
      } else if (elementType == ai.tegmentum.wasmtime4j.WasmValueType.EXTERNREF) {
        elementTypeCode = 6; // EXTERNREF
      } else {
        throw new IllegalArgumentException("Unsupported table element type: " + elementType);
      }

      final int hasMaximum = (maxSize == -1) ? 0 : 1;
      final int maximumSize = (maxSize == -1) ? 0 : maxSize;

      final MemorySegment tablePtr = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              createHandle.invoke(
                  nativeStore,
                  elementTypeCode,
                  initialSize,
                  hasMaximum,
                  maximumSize,
                  MemorySegment.NULL, // name = null
                  tablePtr);

      if (result != 0) {
        throw new WasmException("Failed to create table");
      }

      final MemorySegment nativeTablePtr = tablePtr.get(ValueLayout.ADDRESS, 0);
      if (nativeTablePtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Created table pointer is null");
      }

      return new PanamaTable(nativeTablePtr, elementType, this);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error creating table: " + e.getMessage(), e);
    }
  }

  @Override
  public java.util.concurrent.CompletableFuture<ai.tegmentum.wasmtime4j.WasmTable> createTableAsync(
      final ai.tegmentum.wasmtime4j.WasmValueType elementType,
      final int initialSize,
      final int maxSize) {
    return java.util.concurrent.CompletableFuture.supplyAsync(
        () -> {
          try {
            return createTable(elementType, initialSize, maxSize);
          } catch (WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmMemory createMemory(final int initialPages, final int maxPages)
      throws WasmException {
    if (initialPages < 0) {
      throw new IllegalArgumentException("Initial pages cannot be negative");
    }
    if (maxPages < -1) {
      throw new IllegalArgumentException("Max pages must be -1 (unlimited) or non-negative");
    }
    ensureNotClosed();

    try {
      final MethodHandle createHandle = MEMORY_BINDINGS.getPanamaMemoryCreateWithConfig();
      if (createHandle == null) {
        throw new WasmException("Panama memory creation function not available");
      }

      final long maximumPages = (maxPages == -1) ? 0L : (long) maxPages;
      final int isShared = 0; // Not shared
      final int is64 = 0; // Standard 32-bit memory
      final int memoryIndex = 0; // Not used for direct creation

      final MemorySegment memoryPtr = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              createHandle.invoke(
                  nativeStore,
                  (long) initialPages,
                  maximumPages,
                  isShared,
                  is64,
                  memoryIndex,
                  MemorySegment.NULL, // name = null
                  memoryPtr);

      if (result != 0) {
        throw new WasmException("Failed to create memory");
      }

      final MemorySegment nativeMemoryPtr = memoryPtr.get(ValueLayout.ADDRESS, 0);
      if (nativeMemoryPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Created memory pointer is null");
      }

      return new PanamaMemory(nativeMemoryPtr, this);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error creating memory: " + e.getMessage(), e);
    }
  }

  @Override
  public java.util.concurrent.CompletableFuture<ai.tegmentum.wasmtime4j.WasmMemory>
      createMemoryAsync(final int initialPages, final int maxPages) {
    return java.util.concurrent.CompletableFuture.supplyAsync(
        () -> {
          try {
            return createMemory(initialPages, maxPages);
          } catch (WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmMemory createSharedMemory(
      final int initialPages, final int maxPages) throws WasmException {
    if (initialPages < 0) {
      throw new IllegalArgumentException("Initial pages cannot be negative");
    }
    if (maxPages < 1) {
      throw new IllegalArgumentException("Shared memory requires a positive maximum page count");
    }
    ensureNotClosed();

    try {
      final MethodHandle createHandle = MEMORY_BINDINGS.getPanamaMemoryCreateWithConfig();
      if (createHandle == null) {
        throw new WasmException("Panama memory creation function not available");
      }

      final int isShared = 1; // Shared memory
      final int is64 = 0; // Standard 32-bit memory
      final int memoryIndex = 0; // Not used for direct creation

      final MemorySegment memoryPtr = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              createHandle.invoke(
                  nativeStore,
                  (long) initialPages,
                  (long) maxPages,
                  isShared,
                  is64,
                  memoryIndex,
                  MemorySegment.NULL, // name = null
                  memoryPtr);

      if (result != 0) {
        throw new WasmException("Failed to create shared memory");
      }

      final MemorySegment nativeMemoryPtr = memoryPtr.get(ValueLayout.ADDRESS, 0);
      if (nativeMemoryPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Created shared memory pointer is null");
      }

      return new PanamaMemory(nativeMemoryPtr, this);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error creating shared memory: " + e.getMessage(), e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.func.FunctionReference createFunctionReference(
      final ai.tegmentum.wasmtime4j.func.HostFunction implementation,
      final ai.tegmentum.wasmtime4j.type.FunctionType functionType)
      throws WasmException {
    ensureNotClosed();
    Validation.requireNonNull(implementation, "implementation");
    Validation.requireNonNull(functionType, "functionType");

    return new PanamaFunctionReference(implementation, functionType, this, resourceManager);
  }

  @Override
  public ai.tegmentum.wasmtime4j.func.FunctionReference createFunctionReference(
      final ai.tegmentum.wasmtime4j.WasmFunction function) throws WasmException {
    ensureNotClosed();
    Validation.requireNonNull(function, "function");

    // Create a host function wrapper that delegates to the wasm function
    final ai.tegmentum.wasmtime4j.func.HostFunction wrapper = function::call;

    return new PanamaFunctionReference(wrapper, function.getFunctionType(), this, resourceManager);
  }

  @Override
  public ai.tegmentum.wasmtime4j.func.CallbackRegistry getCallbackRegistry() {
    ensureNotClosed();
    // Lazy initialization of callback registry
    if (callbackRegistry == null) {
      synchronized (this) {
        if (callbackRegistry == null) {
          callbackRegistry = new PanamaCallbackRegistry(this, resourceManager);
        }
      }
    }
    return callbackRegistry;
  }

  @Override
  public ai.tegmentum.wasmtime4j.Instance createInstance(
      final ai.tegmentum.wasmtime4j.Module module) throws WasmException {
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule");
    }
    ensureNotClosed();

    try {
      final MethodHandle createHandle = INSTANCE_BINDINGS.getPanamaInstanceCreate();
      if (createHandle == null) {
        throw new WasmException("Panama instance creation function not available");
      }

      final PanamaModule panamaModule = (PanamaModule) module;
      final MemorySegment instancePtr = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int) createHandle.invoke(nativeStore, panamaModule.getNativeModule(), instancePtr);

      if (result != 0) {
        throw new WasmException("Failed to create instance");
      }

      final MemorySegment nativeInstancePtr = instancePtr.get(ValueLayout.ADDRESS, 0);
      if (nativeInstancePtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Created instance pointer is null");
      }

      return new PanamaInstance(nativeInstancePtr, panamaModule, this);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error creating instance: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
  }

  @Override
  public long getTotalFuelConsumed() throws WasmException {
    ensureNotClosed();
    final ExecutionStats stats = getExecutionStats();
    return stats.fuelConsumed;
  }

  @Override
  public long getExecutionCount() {
    if (resourceHandle.isClosed()) {
      return 0;
    }
    try {
      final ExecutionStats stats = getExecutionStats();
      return stats.executionCount;
    } catch (final WasmException e) {
      LOGGER.warning("Error getting execution count: " + e.getMessage());
      return 0;
    }
  }

  @Override
  public long getTotalExecutionTimeMicros() {
    if (resourceHandle.isClosed()) {
      return 0;
    }
    try {
      final ExecutionStats stats = getExecutionStats();
      return stats.totalExecutionTimeMicros;
    } catch (final WasmException e) {
      LOGGER.warning("Error getting total execution time: " + e.getMessage());
      return 0;
    }
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  /**
   * Gets the native store pointer.
   *
   * @return native store memory segment
   */
  public MemorySegment getNativeStore() {
    return nativeStore;
  }

  /**
   * Gets the resource manager for this store.
   *
   * @return the arena resource manager
   */
  public ArenaResourceManager getResourceManager() {
    ensureNotClosed();
    return resourceManager;
  }

  /**
   * Gets the arena for this store. Package-private for use by child objects (instances, functions).
   *
   * @return the arena
   */
  Arena getArena() {
    return arena;
  }

  /**
   * Gets execution statistics from the native store.
   *
   * @return execution statistics
   * @throws WasmException if failed to get stats
   */
  private ExecutionStats getExecutionStats() throws WasmException {
    try {
      final MethodHandle getStatsHandle = NATIVE_BINDINGS.getPanamaStoreGetExecutionStats();
      if (getStatsHandle == null) {
        throw new WasmException("Panama store get execution stats function not available");
      }

      final MemorySegment executionCountSegment = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment totalExecutionTimeUsSegment = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment fuelConsumedSegment = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int)
              getStatsHandle.invoke(
                  nativeStore,
                  executionCountSegment,
                  totalExecutionTimeUsSegment,
                  fuelConsumedSegment);

      if (result != 0) {
        throw new WasmException("Failed to get execution statistics");
      }

      final long executionCount = executionCountSegment.get(ValueLayout.JAVA_LONG, 0);
      final long totalExecutionTimeUs = totalExecutionTimeUsSegment.get(ValueLayout.JAVA_LONG, 0);
      final long fuelConsumed = fuelConsumedSegment.get(ValueLayout.JAVA_LONG, 0);

      return new ExecutionStats(executionCount, totalExecutionTimeUs, fuelConsumed);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error getting execution statistics: " + e.getMessage(), e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.debug.WasmBacktrace captureBacktrace() {
    ensureNotClosed();
    return captureBacktraceInternal(false);
  }

  @Override
  public ai.tegmentum.wasmtime4j.debug.WasmBacktrace forceCaptureBacktrace() {
    ensureNotClosed();
    return captureBacktraceInternal(true);
  }

  /**
   * Internal method to capture backtrace.
   *
   * @param forceCapture whether to force capture
   * @return the captured backtrace
   * @throws WasmException if backtrace capture fails
   */
  private ai.tegmentum.wasmtime4j.debug.WasmBacktrace captureBacktraceInternal(
      final boolean forceCapture) {
    try {
      // Allocate output parameters
      final MemorySegment bufferOutPtr = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment bufferLenPtr = arena.allocate(ValueLayout.JAVA_INT);

      // Call native function
      final int result;
      if (forceCapture) {
        result =
            NATIVE_BINDINGS.storeForceCaptureBacktrace(nativeStore, bufferOutPtr, bufferLenPtr);
      } else {
        result = NATIVE_BINDINGS.storeCaptureBacktrace(nativeStore, bufferOutPtr, bufferLenPtr);
      }

      if (result != 0) {
        throw PanamaErrorMapper.mapNativeError(result, "Failed to capture backtrace");
      }

      // Read the buffer pointer and length
      final MemorySegment bufferPtr = bufferOutPtr.get(ValueLayout.ADDRESS, 0);
      final int bufferLen = bufferLenPtr.get(ValueLayout.JAVA_INT, 0);

      if (bufferPtr == null || bufferPtr.equals(MemorySegment.NULL) || bufferLen <= 0) {
        // Empty backtrace
        return new ai.tegmentum.wasmtime4j.debug.WasmBacktrace(new ArrayList<>(), forceCapture);
      }

      // Copy buffer data to Java byte array
      final byte[] data = new byte[bufferLen];
      final MemorySegment dataSegment = bufferPtr.reinterpret(bufferLen);
      MemorySegment.copy(dataSegment, ValueLayout.JAVA_BYTE, 0, data, 0, bufferLen);

      // Deserialize backtrace
      return ai.tegmentum.wasmtime4j.panama.util.BacktraceDeserializer.deserialize(data);

    } catch (final Exception e) {
      throw new RuntimeException("Error capturing backtrace: " + e.getMessage(), e);
    }
  }

  @Override
  public void gc() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.storeGc(nativeStore);
    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(result, "Failed to perform garbage collection");
    }
  }

  @Override
  public java.util.concurrent.CompletableFuture<Void> gcAsync()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    return java.util.concurrent.CompletableFuture.supplyAsync(
        () -> {
          try {
            gc();
          } catch (ai.tegmentum.wasmtime4j.exception.WasmException e) {
            throw new RuntimeException(e);
          }
          return null;
        });
  }

  @Override
  public <R> R throwException(final ai.tegmentum.wasmtime4j.ExnRef exceptionRef)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    if (exceptionRef == null) {
      throw new IllegalArgumentException("exceptionRef cannot be null");
    }
    if (!(exceptionRef instanceof PanamaExnRef)) {
      throw new IllegalArgumentException("ExnRef must be a PanamaExnRef instance");
    }

    final PanamaExnRef panamaExnRef = (PanamaExnRef) exceptionRef;
    NATIVE_BINDINGS.storeThrowException(nativeStore, panamaExnRef.getNativeSegment());
    // This should never be reached - the native call always throws
    throw new ai.tegmentum.wasmtime4j.exception.WasmException("Exception was thrown");
  }

  @Override
  public ai.tegmentum.wasmtime4j.ExnRef takePendingException()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    final java.lang.foreign.MemorySegment exnRefPtr =
        NATIVE_BINDINGS.storeTakePendingException(nativeStore);
    if (exnRefPtr == null || exnRefPtr.equals(java.lang.foreign.MemorySegment.NULL)) {
      return null;
    }
    return new PanamaExnRef(exnRefPtr, nativeStore);
  }

  @Override
  public boolean hasPendingException() {
    if (resourceHandle.isClosed()) {
      return false;
    }
    return NATIVE_BINDINGS.storeHasPendingException(nativeStore) != 0;
  }

  @Override
  public void epochDeadlineAsyncYieldAndUpdate(final long deltaTicks)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    if (deltaTicks < 0) {
      throw new IllegalArgumentException("deltaTicks cannot be negative");
    }
    final int result =
        NATIVE_BINDINGS.storeEpochDeadlineAsyncYieldAndUpdate(nativeStore, deltaTicks);
    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(
          result, "Failed to set epoch deadline async yield");
    }
  }

  @Override
  public void epochDeadlineTrap() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.storeEpochDeadlineTrap(nativeStore);
    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(result, "Failed to set epoch deadline trap");
    }
  }

  @Override
  public void epochDeadlineCallback(
      final ai.tegmentum.wasmtime4j.Store.EpochDeadlineCallback callback)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();

    LOGGER.fine("epochDeadlineCallback called, callback=" + callback);

    // Remove existing callback if any
    final long oldEpochId = epochCallbackId.get();
    if (oldEpochId != 0) {
      EPOCH_CALLBACKS.remove(oldEpochId);
      epochCallbackId.set(0);
    }

    // Store callback reference to prevent GC
    this.epochDeadlineCallback = callback;

    int result;
    if (callback == null) {
      // Clear the callback
      result = NATIVE_BINDINGS.storeClearEpochDeadlineCallback(nativeStore);
    } else {
      // Check if upcall stub was created successfully
      if (EPOCH_CALLBACK_STUB == null) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Epoch callback infrastructure not initialized");
      }

      // Generate a unique callback ID and register the callback
      final long newCallbackId = EPOCH_CALLBACK_ID_COUNTER.getAndIncrement();
      EPOCH_CALLBACKS.put(newCallbackId, callback);
      epochCallbackId.set(newCallbackId);

      LOGGER.fine(
          "Calling native storeSetEpochDeadlineCallbackFn: callbackId="
              + newCallbackId
              + ", stub="
              + EPOCH_CALLBACK_STUB);

      // Set the callback with function pointer
      result =
          NATIVE_BINDINGS.storeSetEpochDeadlineCallbackFn(
              nativeStore, EPOCH_CALLBACK_STUB, newCallbackId);

      LOGGER.fine("Native call returned: " + result);

      if (result != 0) {
        // Cleanup on failure
        EPOCH_CALLBACKS.remove(newCallbackId);
        epochCallbackId.set(0);
      }
    }

    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(
          result, "Failed to configure epoch deadline callback");
    }
  }

  // Callback holder to prevent garbage collection
  private ai.tegmentum.wasmtime4j.Store.EpochDeadlineCallback epochDeadlineCallback;

  // Call hook support
  private ai.tegmentum.wasmtime4j.func.CallHookHandler callHookHandler;
  // AtomicLong so the safety net lambda can capture it without preventing GC of PanamaStore
  private final AtomicLong callHookCallbackId = new AtomicLong(0);
  private ai.tegmentum.wasmtime4j.Store.AsyncCallHookHandler asyncCallHookHandler;

  @Override
  public void setCallHook(final ai.tegmentum.wasmtime4j.func.CallHookHandler handler)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();

    // Remove previous callback registration if any
    final long oldHookId = callHookCallbackId.get();
    if (oldHookId != 0) {
      CALL_HOOK_HANDLERS.remove(oldHookId);
      callHookCallbackId.set(0);
    }

    this.callHookHandler = handler;
    int result;
    if (handler == null) {
      result = NATIVE_BINDINGS.storeClearCallHook(nativeStore);
    } else if (CALL_HOOK_STUB == null) {
      // Fallback to no-op hook if stub creation failed
      LOGGER.warning("Call hook stub not available, installing no-op hook");
      result = NATIVE_BINDINGS.storeSetCallHook(nativeStore);
    } else {
      // Register callback and install hook with function pointer
      final long id = CALL_HOOK_CALLBACK_ID_COUNTER.getAndIncrement();
      CALL_HOOK_HANDLERS.put(id, handler);
      callHookCallbackId.set(id);
      result = INSTANCE_BINDINGS.storeSetCallHookFn(nativeStore, CALL_HOOK_STUB, id);
    }
    if (result != 0) {
      // Clean up on failure
      final long failedHookId = callHookCallbackId.get();
      if (failedHookId != 0) {
        CALL_HOOK_HANDLERS.remove(failedHookId);
        callHookCallbackId.set(0);
      }
      throw PanamaErrorMapper.mapNativeError(result, "Failed to configure call hook");
    }
  }

  @Override
  public void setCallHookAsync(final ai.tegmentum.wasmtime4j.Store.AsyncCallHookHandler handler)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    this.asyncCallHookHandler = handler;
    int result;
    if (handler == null) {
      result = NATIVE_BINDINGS.storeClearCallHookAsync(nativeStore);
    } else {
      result = NATIVE_BINDINGS.storeSetCallHookAsync(nativeStore);
    }
    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(result, "Failed to configure async call hook");
    }
  }

  /**
   * Creates the resource handle with the store's cleanup logic.
   *
   * @return the resource handle
   */
  private NativeResourceHandle createResourceHandle() {
    final MemorySegment storeHandle = this.nativeStore;
    final Arena storeArena = this.arena;
    // Capture AtomicLong references for the safety net (separate objects, won't prevent GC)
    final AtomicLong epochId = this.epochCallbackId;
    final AtomicLong hookId = this.callHookCallbackId;
    final AtomicLong limiterId = this.limiterCallbackId;
    return new NativeResourceHandle(
        "PanamaStore",
        () -> {
          // Clean up epoch callback from static registry
          final long epochVal = epochCallbackId.getAndSet(0);
          if (epochVal != 0) {
            EPOCH_CALLBACKS.remove(epochVal);
          }

          // Clean up call hook callback from static registry
          final long hookVal = callHookCallbackId.getAndSet(0);
          if (hookVal != 0) {
            CALL_HOOK_HANDLERS.remove(hookVal);
          }

          // Clean up resource limiter from static registry
          final long limiterVal = limiterCallbackId.getAndSet(0);
          if (limiterVal != 0) {
            RESOURCE_LIMITERS.remove(limiterVal);
          }

          // Close callback registry first (cleans up function references)
          if (callbackRegistry != null) {
            try {
              callbackRegistry.close();
            } catch (final Exception e) {
              LOGGER.warning("Error closing callback registry: " + e.getMessage());
            }
          }

          // Close resource manager (cleans up host functions and managed resources)
          if (resourceManager != null) {
            resourceManager.close();
          }

          // Destroy native store
          if (nativeStore != null && !nativeStore.equals(MemorySegment.NULL)) {
            NATIVE_BINDINGS.storeDestroy(nativeStore);
          }
          arena.close();
        },
        this,
        () -> {
          // Safety net: clean up static callback maps and destroy native store
          // Captures AtomicLong references (not 'this') to avoid preventing GC
          final long epochVal = epochId.getAndSet(0);
          if (epochVal != 0) {
            EPOCH_CALLBACKS.remove(epochVal);
          }
          final long hookVal = hookId.getAndSet(0);
          if (hookVal != 0) {
            CALL_HOOK_HANDLERS.remove(hookVal);
          }
          final long limiterVal = limiterId.getAndSet(0);
          if (limiterVal != 0) {
            RESOURCE_LIMITERS.remove(limiterVal);
          }
          if (storeHandle != null && !storeHandle.equals(MemorySegment.NULL)) {
            NATIVE_BINDINGS.storeDestroy(storeHandle);
          }
          storeArena.close();
        });
  }

  /**
   * Ensures the store is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  /** Holds execution statistics from the native store. */
  private static final class ExecutionStats {
    final long executionCount;
    final long totalExecutionTimeMicros;
    final long fuelConsumed;

    ExecutionStats(
        final long executionCount, final long totalExecutionTimeMicros, final long fuelConsumed) {
      this.executionCount = executionCount;
      this.totalExecutionTimeMicros = totalExecutionTimeMicros;
      this.fuelConsumed = fuelConsumed;
    }
  }

  // ===== Fuel Async Methods =====

  private long fuelAsyncYieldInterval = 0;

  @Override
  public void setFuelAsyncYieldInterval(final long interval) throws WasmException {
    if (interval < 0) {
      throw new IllegalArgumentException("Interval cannot be negative");
    }
    ensureNotClosed();
    try {
      final MethodHandle handle = NATIVE_BINDINGS.getPanamaStoreSetFuelAsyncYieldInterval();
      if (handle == null) {
        throw new WasmException(
            "Panama store set fuel async yield interval function not available");
      }
      final int result = (int) handle.invoke(nativeStore, interval);
      if (result != 0) {
        throw new WasmException("Failed to set fuel async yield interval");
      }
      this.fuelAsyncYieldInterval = interval;
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error setting fuel async yield interval: " + e.getMessage(), e);
    }
  }

  @Override
  public long getFuelAsyncYieldInterval() {
    return fuelAsyncYieldInterval;
  }
}
