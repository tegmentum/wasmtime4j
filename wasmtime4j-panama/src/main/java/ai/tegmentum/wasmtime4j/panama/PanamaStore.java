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
package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExternRef;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.config.ResourceLimiter;
import ai.tegmentum.wasmtime4j.config.ResourceLimiterAsync;
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
      return 0; // Trap - callback not found
    }

    try {
      final EpochDeadlineAction action = callback.onEpochDeadline(epoch);
      if (action == null) {
        LOGGER.fine("Epoch callback returned null action - trapping");
        return 0; // Trap on null action
      }
      if (action.shouldContinue()) {
        final long delta = action.getDeltaTicks();
        LOGGER.fine("Epoch callback returning continue with delta=" + delta);
        return delta; // Positive = continue with this delta
      }
      if (action.shouldYield()) {
        final long delta = action.getDeltaTicks();
        LOGGER.fine("Epoch callback returning yield with delta=" + delta);
        return -delta; // Negative = yield with this delta
      }
      LOGGER.fine("Epoch callback returning trap");
      return 0; // Trap
    } catch (final Exception e) {
      LOGGER.warning("Epoch callback threw exception: " + e.getMessage());
      return 0; // Trap on exception
    }
  }

  // Debug handler callback infrastructure
  private static final AtomicLong DEBUG_HANDLER_ID_COUNTER = new AtomicLong(1);
  private static final ConcurrentHashMap<Long, ai.tegmentum.wasmtime4j.debug.DebugHandler>
      DEBUG_HANDLERS = new ConcurrentHashMap<>();
  private static final MemorySegment DEBUG_HANDLER_STUB;
  private static final FunctionDescriptor DEBUG_HANDLER_DESCRIPTOR =
      FunctionDescriptor.ofVoid(
          ValueLayout.JAVA_LONG, // callback_id
          ValueLayout.JAVA_INT); // event_code

  static {
    MemorySegment debugStub = null;
    try {
      final MethodHandle debugCallbackHandle =
          MethodHandles.lookup()
              .findStatic(
                  PanamaStore.class,
                  "debugHandlerCallbackStub",
                  MethodType.methodType(void.class, long.class, int.class));
      debugStub =
          Linker.nativeLinker()
              .upcallStub(debugCallbackHandle, DEBUG_HANDLER_DESCRIPTOR, Arena.global());
    } catch (final NoSuchMethodException | IllegalAccessException e) {
      LOGGER.severe("Failed to create debug handler callback stub: " + e.getMessage());
    }
    DEBUG_HANDLER_STUB = debugStub;
  }

  /**
   * Static callback method invoked from native code when a debug event occurs.
   *
   * @param callbackId the callback identifier
   * @param eventCode the debug event type code
   */
  private static void debugHandlerCallbackStub(final long callbackId, final int eventCode) {
    final ai.tegmentum.wasmtime4j.debug.DebugHandler handler = DEBUG_HANDLERS.get(callbackId);
    if (handler == null) {
      return;
    }
    final ai.tegmentum.wasmtime4j.debug.DebugEvent event =
        ai.tegmentum.wasmtime4j.debug.DebugEvent.fromCode(eventCode);
    if (event != null) {
      handler.handle(event, java.util.Collections.emptyList());
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
  private static final ConcurrentHashMap<Long, ResourceLimiterAsync> RESOURCE_LIMITERS_ASYNC =
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
    if (limiter != null) {
      try {
        return limiter.memoryGrowing(currentBytes, desiredBytes, maximumBytes) ? 1 : 0;
      } catch (final Exception e) {
        LOGGER.warning("memoryGrowing callback threw exception: " + e.getMessage());
        return 0;
      }
    }
    final ResourceLimiterAsync asyncLimiter = RESOURCE_LIMITERS_ASYNC.get(callbackId);
    if (asyncLimiter != null) {
      try {
        return asyncLimiter.memoryGrowing(currentBytes, desiredBytes, maximumBytes).join() ? 1 : 0;
      } catch (final Exception e) {
        LOGGER.warning("Async memoryGrowing callback threw exception: " + e.getMessage());
        return 0;
      }
    }
    LOGGER.warning("Resource limiter not found for ID: " + callbackId);
    return 0;
  }

  private static int tableGrowingStub(
      final long callbackId,
      final int currentElements,
      final int desiredElements,
      final int maximumElements) {
    final ResourceLimiter limiter = RESOURCE_LIMITERS.get(callbackId);
    if (limiter != null) {
      try {
        return limiter.tableGrowing(currentElements, desiredElements, maximumElements) ? 1 : 0;
      } catch (final Exception e) {
        LOGGER.warning("tableGrowing callback threw exception: " + e.getMessage());
        return 0;
      }
    }
    final ResourceLimiterAsync asyncLimiter = RESOURCE_LIMITERS_ASYNC.get(callbackId);
    if (asyncLimiter != null) {
      try {
        return asyncLimiter.tableGrowing(currentElements, desiredElements, maximumElements).join()
            ? 1
            : 0;
      } catch (final Exception e) {
        LOGGER.warning("Async tableGrowing callback threw exception: " + e.getMessage());
        return 0;
      }
    }
    LOGGER.warning("Resource limiter not found for ID: " + callbackId);
    return 0;
  }

  private static void memoryGrowFailedStub(final long callbackId, final MemorySegment errorMsg) {
    try {
      final String error =
          (errorMsg != null && !errorMsg.equals(MemorySegment.NULL))
              ? errorMsg.reinterpret(Long.MAX_VALUE).getString(0)
              : "unknown error";
      final ResourceLimiter limiter = RESOURCE_LIMITERS.get(callbackId);
      if (limiter != null) {
        limiter.memoryGrowFailed(error);
        return;
      }
      final ResourceLimiterAsync asyncLimiter = RESOURCE_LIMITERS_ASYNC.get(callbackId);
      if (asyncLimiter != null) {
        asyncLimiter.memoryGrowFailed(error);
      }
    } catch (final Exception e) {
      LOGGER.warning("memoryGrowFailed callback threw exception: " + e.getMessage());
    }
  }

  private static void tableGrowFailedStub(final long callbackId, final MemorySegment errorMsg) {
    try {
      final String error =
          (errorMsg != null && !errorMsg.equals(MemorySegment.NULL))
              ? errorMsg.reinterpret(Long.MAX_VALUE).getString(0)
              : "unknown error";
      final ResourceLimiter limiter = RESOURCE_LIMITERS.get(callbackId);
      if (limiter != null) {
        limiter.tableGrowFailed(error);
        return;
      }
      final ResourceLimiterAsync asyncLimiter = RESOURCE_LIMITERS_ASYNC.get(callbackId);
      if (asyncLimiter != null) {
        asyncLimiter.tableGrowFailed(error);
      }
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

  // Tracked WASI context reference for getWasiContext() and reapplyWasiContext()
  private volatile PanamaWasiContext trackedWasiContext;

  // Debug handler callback ID for this store (0 = no handler registered)
  // AtomicLong so the safety net lambda can capture it without preventing GC of PanamaStore
  private final AtomicLong debugHandlerCallbackId = new AtomicLong(0);
  private ai.tegmentum.wasmtime4j.debug.DebugHandler debugHandler;

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
            (int) limits.getTables(),
            (int) limits.getMemories(),
            limits.isTrapOnGrowFailure() ? 1 : 0,
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
            0, // max tables - 0 means no limit
            0, // max memories - 0 means no limit
            0, // trap on grow failure - 0 means false
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

  /**
   * Tries to create a store using OOM-safe allocation.
   *
   * @param engine the engine to create the store for
   * @return a new PanamaStore
   * @throws WasmException if allocation fails
   */
  static PanamaStore tryCreate(final PanamaEngine engine) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (!engine.isValid()) {
      throw new IllegalStateException("Engine is not valid");
    }

    try {
      final MethodHandle tryCreateHandle = NATIVE_BINDINGS.getPanamaStoreTryCreate();
      if (tryCreateHandle == null) {
        throw new WasmException("Panama store try_create function not available");
      }

      final Arena tempArena = Arena.ofConfined();
      final MemorySegment storePtr = tempArena.allocate(ValueLayout.ADDRESS);

      final int result = (int) tryCreateHandle.invoke(engine.getNativeEngine(), storePtr);

      if (result != 0) {
        tempArena.close();
        throw new WasmException("Failed to allocate store (out of memory)");
      }

      final MemorySegment storeHandle = storePtr.get(ValueLayout.ADDRESS, 0);
      tempArena.close();

      if (storeHandle == null || storeHandle.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to allocate store (null handle)");
      }

      return new PanamaStore(engine, storeHandle);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error creating store: " + e.getMessage(), e);
    }
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
  public void setResourceLimiterAsync(final ResourceLimiterAsync limiter) throws WasmException {
    if (limiter == null) {
      throw new IllegalArgumentException("ResourceLimiterAsync cannot be null");
    }
    ensureNotClosed();

    // Remove any previously registered sync limiter
    final long oldLimiterId = limiterCallbackId.get();
    if (oldLimiterId != 0) {
      RESOURCE_LIMITERS.remove(oldLimiterId);
      RESOURCE_LIMITERS_ASYNC.remove(oldLimiterId);
      limiterCallbackId.set(0);
    }

    // Check if upcall stubs were created successfully
    if (MEMORY_GROWING_STUB == null || TABLE_GROWING_STUB == null) {
      throw new WasmException("Resource limiter callback infrastructure not initialized");
    }

    final long newCallbackId = LIMITER_CALLBACK_ID_COUNTER.getAndIncrement();
    RESOURCE_LIMITERS_ASYNC.put(newCallbackId, limiter);
    limiterCallbackId.set(newCallbackId);

    final int result =
        NATIVE_BINDINGS.storeSetResourceLimiterAsync(
            nativeStore,
            newCallbackId,
            MEMORY_GROWING_STUB,
            TABLE_GROWING_STUB,
            MEMORY_GROW_FAILED_STUB != null ? MEMORY_GROW_FAILED_STUB : MemorySegment.NULL,
            TABLE_GROW_FAILED_STUB != null ? TABLE_GROW_FAILED_STUB : MemorySegment.NULL);

    if (result != 0) {
      RESOURCE_LIMITERS_ASYNC.remove(newCallbackId);
      limiterCallbackId.set(0);
      throw PanamaErrorMapper.mapNativeError(result, "Failed to configure async resource limiter");
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
  public long hostcallFuel() throws WasmException {
    ensureNotClosed();

    try {
      final MethodHandle getHostcallFuelHandle = NATIVE_BINDINGS.getPanamaStoreGetHostcallFuel();
      if (getHostcallFuelHandle == null) {
        throw new WasmException("Panama store get hostcall fuel function not available");
      }

      final MemorySegment fuelSegment = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = (int) getHostcallFuelHandle.invoke(nativeStore, fuelSegment);

      if (result != 0) {
        throw new WasmException("Failed to get hostcall fuel");
      }

      return fuelSegment.get(ValueLayout.JAVA_LONG, 0);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error getting hostcall fuel: " + e.getMessage(), e);
    }
  }

  @Override
  public void setHostcallFuel(final long fuel) throws WasmException {
    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel cannot be negative");
    }
    ensureNotClosed();

    try {
      final MethodHandle setHostcallFuelHandle = NATIVE_BINDINGS.getPanamaStoreSetHostcallFuel();
      if (setHostcallFuelHandle == null) {
        throw new WasmException("Panama store set hostcall fuel function not available");
      }

      final int result = (int) setHostcallFuelHandle.invoke(nativeStore, fuel);

      if (result != 0) {
        throw new WasmException("Failed to set hostcall fuel");
      }
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error setting hostcall fuel: " + e.getMessage(), e);
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
  public ai.tegmentum.wasmtime4j.WasmFunction createHostFunctionUnchecked(
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

    final PanamaHostFunction.HostFunctionCallback callback = implementation::execute;

    final PanamaHostFunction hostFunction =
        new PanamaHostFunction(
            name, functionType, callback, implementation, this, resourceManager, true);

    LOGGER.fine("Created unchecked host function '" + name + "' in store");
    return hostFunction;
  }

  /**
   * Sets the tracked WASI context reference for this store.
   *
   * <p>Called internally when a WASI context is applied to this store during instantiation.
   *
   * @param wasiCtx the WASI context to track
   */
  void setTrackedWasiContext(final PanamaWasiContext wasiCtx) {
    this.trackedWasiContext = wasiCtx;
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.wasi.WasiContext> getWasiContext() {
    return java.util.Optional.ofNullable(trackedWasiContext);
  }

  @Override
  public void reapplyWasiContext() throws WasmException {
    final PanamaWasiContext wasiCtx = trackedWasiContext;
    if (wasiCtx == null) {
      throw new WasmException("No WASI context is configured for this store");
    }
    ensureNotClosed();
    final int result =
        NativeStoreBindings.getInstance()
            .storeSetWasiContext(nativeStore, wasiCtx.getNativeContext());
    if (result != 0) {
      throw new WasmException("Failed to re-apply WASI context to store");
    }
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
    if (maxSize != -1 && maxSize < initialSize) {
      throw new IllegalArgumentException(
          "Max size (" + maxSize + ") cannot be less than initial size (" + initialSize + ")");
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
  public ai.tegmentum.wasmtime4j.WasmTable createTable(
      final ai.tegmentum.wasmtime4j.WasmValueType elementType,
      final int initialSize,
      final int maxSize,
      final ai.tegmentum.wasmtime4j.WasmValue initValue)
      throws WasmException {
    if (elementType == null) {
      throw new IllegalArgumentException("Element type cannot be null");
    }
    if (initValue == null) {
      throw new IllegalArgumentException("Init value cannot be null");
    }
    if (initialSize < 0) {
      throw new IllegalArgumentException("Initial size cannot be negative");
    }
    if (maxSize < -1) {
      throw new IllegalArgumentException("Max size must be -1 (unlimited) or non-negative");
    }
    if (maxSize != -1 && maxSize < initialSize) {
      throw new IllegalArgumentException(
          "Max size (" + maxSize + ") cannot be less than initial size (" + initialSize + ")");
    }
    ensureNotClosed();

    try {
      final MethodHandle createHandle = MEMORY_BINDINGS.getPanamaTableCreateWithInit();
      if (createHandle == null) {
        throw new WasmException("Panama table creation with init function not available");
      }

      final int elementTypeCode;
      if (elementType == ai.tegmentum.wasmtime4j.WasmValueType.FUNCREF) {
        elementTypeCode = 5;
      } else if (elementType == ai.tegmentum.wasmtime4j.WasmValueType.EXTERNREF) {
        elementTypeCode = 6;
      } else {
        throw new IllegalArgumentException("Unsupported table element type: " + elementType);
      }

      final int hasMaximum = (maxSize == -1) ? 0 : 1;
      final int maximumSize = (maxSize == -1) ? 0 : maxSize;
      final long initRefId = wasmValueToRefId(initValue);

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
                  initRefId,
                  tablePtr);

      if (result != 0) {
        throw new WasmException("Failed to create table with init value");
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
      throw new WasmException("Error creating table with init value: " + e.getMessage(), e);
    }
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
    if (maxPages != -1 && maxPages < initialPages) {
      throw new IllegalArgumentException(
          "Max pages (" + maxPages + ") cannot be less than initial pages (" + initialPages + ")");
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
  public ai.tegmentum.wasmtime4j.WasmMemory createSharedMemory(
      final int initialPages, final int maxPages) throws WasmException {
    if (initialPages < 0) {
      throw new IllegalArgumentException("Initial pages cannot be negative");
    }
    if (maxPages < 1) {
      throw new IllegalArgumentException("Shared memory requires a positive maximum page count");
    }
    if (maxPages < initialPages) {
      throw new IllegalArgumentException(
          "Max pages (" + maxPages + ") cannot be less than initial pages (" + initialPages + ")");
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
  public ai.tegmentum.wasmtime4j.WasmMemory createMemory(
      final ai.tegmentum.wasmtime4j.type.MemoryType memoryType) throws WasmException {
    if (memoryType == null) {
      throw new IllegalArgumentException("Memory type cannot be null");
    }
    final long minPages = memoryType.getMinimum();
    final long maxPages = memoryType.getMaximum().orElse(-1L);
    if (minPages < 0) {
      throw new IllegalArgumentException("Minimum pages cannot be negative: " + minPages);
    }
    if (maxPages != -1 && maxPages < minPages) {
      throw new IllegalArgumentException(
          "Maximum pages (" + maxPages + ") cannot be less than minimum pages (" + minPages + ")");
    }
    if (memoryType.isShared() && maxPages < 1) {
      throw new IllegalArgumentException("Shared memory requires a positive maximum page count");
    }
    ensureNotClosed();

    try {
      final MethodHandle createHandle = MEMORY_BINDINGS.getPanamaMemoryCreateWithConfig();
      if (createHandle == null) {
        throw new WasmException("Panama memory creation function not available");
      }

      final long maximumPages = (maxPages == -1) ? 0L : maxPages;
      final int isShared = memoryType.isShared() ? 1 : 0;
      final int is64 = memoryType.is64Bit() ? 1 : 0;
      final int memoryIndex = 0;

      final MemorySegment memoryPtr = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              createHandle.invoke(
                  nativeStore,
                  minPages,
                  maximumPages,
                  isShared,
                  is64,
                  memoryIndex,
                  MemorySegment.NULL, // name = null
                  memoryPtr);

      if (result != 0) {
        throw new WasmException("Failed to create memory from type");
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
      throw new WasmException("Error creating memory from type: " + e.getMessage(), e);
    }
  }

  @Override
  public java.util.concurrent.CompletableFuture<ai.tegmentum.wasmtime4j.WasmMemory>
      createMemoryAsync(final ai.tegmentum.wasmtime4j.type.MemoryType memoryType) {
    return java.util.concurrent.CompletableFuture.supplyAsync(
        () -> {
          try {
            return createMemoryAsyncInternal(memoryType);
          } catch (final WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  private ai.tegmentum.wasmtime4j.WasmMemory createMemoryAsyncInternal(
      final ai.tegmentum.wasmtime4j.type.MemoryType memoryType) throws WasmException {
    if (memoryType == null) {
      throw new IllegalArgumentException("Memory type cannot be null");
    }
    final long minPages = memoryType.getMinimum();
    final long maxPages = memoryType.getMaximum().orElse(-1L);
    if (minPages < 0) {
      throw new IllegalArgumentException("Minimum pages cannot be negative: " + minPages);
    }
    if (maxPages != -1 && maxPages < minPages) {
      throw new IllegalArgumentException(
          "Maximum pages (" + maxPages + ") cannot be less than minimum pages (" + minPages + ")");
    }
    if (memoryType.isShared()) {
      throw new IllegalArgumentException("Async creation not supported for shared memory");
    }
    ensureNotClosed();

    try {
      final MethodHandle createHandle = MEMORY_BINDINGS.getPanamaMemoryCreateAsync();
      if (createHandle == null) {
        // Fallback to sync path if async not available
        return createMemory(memoryType);
      }

      final long maximumPages = (maxPages == -1) ? 0L : maxPages;
      final int isShared = 0; // Shared not supported for async
      final int is64 = memoryType.is64Bit() ? 1 : 0;
      final int memoryIndex = 0;

      final MemorySegment memoryPtr = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              createHandle.invoke(
                  nativeStore,
                  minPages,
                  maximumPages,
                  isShared,
                  is64,
                  memoryIndex,
                  MemorySegment.NULL, // name = null
                  memoryPtr);

      if (result != 0) {
        throw new WasmException("Failed to create memory asynchronously");
      }

      final MemorySegment nativeMemoryPtr = memoryPtr.get(ValueLayout.ADDRESS, 0);
      if (nativeMemoryPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Async created memory pointer is null");
      }

      return new PanamaMemory(nativeMemoryPtr, this);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error creating memory asynchronously: " + e.getMessage(), e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmTable createTable(
      final ai.tegmentum.wasmtime4j.type.TableType tableType) throws WasmException {
    if (tableType == null) {
      throw new IllegalArgumentException("Table type cannot be null");
    }
    final ai.tegmentum.wasmtime4j.WasmValueType elementType = tableType.getElementType();
    final long minSize = tableType.getMinimum();
    final long maxSize = tableType.getMaximum().orElse(-1L);
    final boolean is64 = tableType.is64Bit();

    if (elementType != ai.tegmentum.wasmtime4j.WasmValueType.FUNCREF
        && elementType != ai.tegmentum.wasmtime4j.WasmValueType.EXTERNREF) {
      throw new IllegalArgumentException(
          "Element type must be FUNCREF or EXTERNREF, got: " + elementType);
    }
    if (minSize < 0) {
      throw new IllegalArgumentException("Minimum size cannot be negative: " + minSize);
    }
    if (maxSize != -1 && maxSize < minSize) {
      throw new IllegalArgumentException(
          "Maximum size (" + maxSize + ") cannot be less than minimum size (" + minSize + ")");
    }
    ensureNotClosed();

    try {
      final int elementTypeCode;
      if (elementType == ai.tegmentum.wasmtime4j.WasmValueType.FUNCREF) {
        elementTypeCode = 5;
      } else {
        elementTypeCode = 6;
      }

      final MemorySegment tablePtr = arena.allocate(ValueLayout.ADDRESS);
      final int result;

      if (is64) {
        final MethodHandle create64Handle = MEMORY_BINDINGS.getPanamaTableCreate64();
        if (create64Handle == null) {
          throw new WasmException("Panama 64-bit table creation function not available");
        }

        final int hasMaximum = (maxSize == -1) ? 0 : 1;
        final long maximumSize = (maxSize == -1) ? 0L : maxSize;

        result =
            (int)
                create64Handle.invoke(
                    nativeStore,
                    elementTypeCode,
                    minSize,
                    hasMaximum,
                    maximumSize,
                    MemorySegment.NULL, // name = null
                    tablePtr);
      } else {
        final MethodHandle createHandle = MEMORY_BINDINGS.getPanamaTableCreate();
        if (createHandle == null) {
          throw new WasmException("Panama table creation function not available");
        }

        final int hasMaximum = (maxSize == -1) ? 0 : 1;
        final int maximumSize = (maxSize == -1) ? 0 : (int) maxSize;

        result =
            (int)
                createHandle.invoke(
                    nativeStore,
                    elementTypeCode,
                    (int) minSize,
                    hasMaximum,
                    maximumSize,
                    MemorySegment.NULL, // name = null
                    tablePtr);
      }

      if (result != 0) {
        throw new WasmException("Failed to create table from type");
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
      throw new WasmException("Error creating table from type: " + e.getMessage(), e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmTable createTable(
      final ai.tegmentum.wasmtime4j.type.TableType tableType,
      final ai.tegmentum.wasmtime4j.WasmValue initValue)
      throws WasmException {
    if (tableType == null) {
      throw new IllegalArgumentException("Table type cannot be null");
    }
    if (initValue == null) {
      throw new IllegalArgumentException("Init value cannot be null");
    }
    final ai.tegmentum.wasmtime4j.WasmValueType elementType = tableType.getElementType();
    final long minSize = tableType.getMinimum();
    final long maxSize = tableType.getMaximum().orElse(-1L);

    if (elementType != ai.tegmentum.wasmtime4j.WasmValueType.FUNCREF
        && elementType != ai.tegmentum.wasmtime4j.WasmValueType.EXTERNREF) {
      throw new IllegalArgumentException(
          "Element type must be FUNCREF or EXTERNREF, got: " + elementType);
    }
    if (minSize < 0) {
      throw new IllegalArgumentException("Minimum size cannot be negative: " + minSize);
    }
    if (maxSize != -1 && maxSize < minSize) {
      throw new IllegalArgumentException(
          "Maximum size (" + maxSize + ") cannot be less than minimum size (" + minSize + ")");
    }
    ensureNotClosed();

    try {
      final MethodHandle createHandle = MEMORY_BINDINGS.getPanamaTableCreateWithInit();
      if (createHandle == null) {
        throw new WasmException("Panama table creation with init function not available");
      }

      final int elementTypeCode =
          (elementType == ai.tegmentum.wasmtime4j.WasmValueType.FUNCREF) ? 5 : 6;
      final int hasMaximum = (maxSize == -1) ? 0 : 1;
      final int maximumSize = (maxSize == -1) ? 0 : (int) maxSize;
      final long initRefId = wasmValueToRefId(initValue);

      final MemorySegment tablePtr = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              createHandle.invoke(
                  nativeStore,
                  elementTypeCode,
                  (int) minSize,
                  hasMaximum,
                  maximumSize,
                  MemorySegment.NULL,
                  initRefId,
                  tablePtr);

      if (result != 0) {
        throw new WasmException("Failed to create table from type with init value");
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
      throw new WasmException(
          "Error creating table from type with init value: " + e.getMessage(), e);
    }
  }

  @Override
  public java.util.concurrent.CompletableFuture<ai.tegmentum.wasmtime4j.WasmTable> createTableAsync(
      final ai.tegmentum.wasmtime4j.type.TableType tableType) {
    return java.util.concurrent.CompletableFuture.supplyAsync(
        () -> {
          try {
            return createTableAsyncInternal(tableType);
          } catch (final WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  private long wasmValueToRefId(final ai.tegmentum.wasmtime4j.WasmValue initValue) {
    if (initValue.getValue() == null) {
      return -1L;
    }
    final Object val = initValue.getValue();
    if (val instanceof Long) {
      return (Long) val;
    }
    // Null ref for unsupported value types
    return -1L;
  }

  private ai.tegmentum.wasmtime4j.WasmTable createTableAsyncInternal(
      final ai.tegmentum.wasmtime4j.type.TableType tableType) throws WasmException {
    if (tableType == null) {
      throw new IllegalArgumentException("Table type cannot be null");
    }
    final ai.tegmentum.wasmtime4j.WasmValueType elementType = tableType.getElementType();
    final long minSize = tableType.getMinimum();
    final long maxSize = tableType.getMaximum().orElse(-1L);

    if (elementType != ai.tegmentum.wasmtime4j.WasmValueType.FUNCREF
        && elementType != ai.tegmentum.wasmtime4j.WasmValueType.EXTERNREF) {
      throw new IllegalArgumentException(
          "Element type must be FUNCREF or EXTERNREF, got: " + elementType);
    }
    if (minSize < 0) {
      throw new IllegalArgumentException("Minimum size cannot be negative: " + minSize);
    }
    if (maxSize != -1 && maxSize < minSize) {
      throw new IllegalArgumentException(
          "Maximum size (" + maxSize + ") cannot be less than minimum size (" + minSize + ")");
    }
    ensureNotClosed();

    try {
      final MethodHandle createHandle = MEMORY_BINDINGS.getPanamaTableCreateAsync();
      if (createHandle == null) {
        // Fallback to sync path if async not available
        return createTable(tableType);
      }

      final int elementTypeCode =
          (elementType == ai.tegmentum.wasmtime4j.WasmValueType.FUNCREF) ? 5 : 6;

      final MemorySegment tablePtr = arena.allocate(ValueLayout.ADDRESS);
      final int hasMaximum = (maxSize == -1) ? 0 : 1;
      final int maximumSize = (maxSize == -1) ? 0 : (int) maxSize;

      final int result =
          (int)
              createHandle.invoke(
                  nativeStore,
                  elementTypeCode,
                  (int) minSize,
                  hasMaximum,
                  maximumSize,
                  MemorySegment.NULL, // name = null
                  tablePtr);

      if (result != 0) {
        throw new WasmException("Failed to create table asynchronously");
      }

      final MemorySegment nativeTablePtr = tablePtr.get(ValueLayout.ADDRESS, 0);
      if (nativeTablePtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Async created table pointer is null");
      }

      return new PanamaTable(nativeTablePtr, elementType, this);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error creating table asynchronously: " + e.getMessage(), e);
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
  public ai.tegmentum.wasmtime4j.Instance createInstance(
      final ai.tegmentum.wasmtime4j.Module module, final ai.tegmentum.wasmtime4j.Extern[] imports)
      throws WasmException {
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    if (imports == null) {
      throw new IllegalArgumentException("Imports cannot be null");
    }
    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule");
    }
    ensureNotClosed();

    final PanamaModule panamaModule = (PanamaModule) module;

    if (imports.length == 0) {
      return createInstance(module);
    }

    try (final Arena importArena = Arena.ofConfined()) {
      final MemorySegment externPtrs = importArena.allocate(ValueLayout.ADDRESS, imports.length);
      final MemorySegment externTypes = importArena.allocate(ValueLayout.JAVA_INT, imports.length);

      for (int i = 0; i < imports.length; i++) {
        final ai.tegmentum.wasmtime4j.Extern ext = imports[i];
        if (ext == null) {
          throw new IllegalArgumentException("Import at index " + i + " is null");
        }
        externPtrs.setAtIndex(ValueLayout.ADDRESS, i, extractExternHandle(ext));
        externTypes.setAtIndex(ValueLayout.JAVA_INT, i, externTypeToNativeCode(ext.getType()));
      }

      final MemorySegment instanceOut = importArena.allocate(ValueLayout.ADDRESS);

      final int result =
          INSTANCE_BINDINGS.panamaInstanceCreateWithImports(
              nativeStore,
              panamaModule.getNativeModule(),
              externPtrs,
              externTypes,
              imports.length,
              instanceOut);

      if (result != 0) {
        throw new WasmException("Failed to create instance with imports");
      }

      final MemorySegment instancePtr = instanceOut.get(ValueLayout.ADDRESS, 0);
      if (instancePtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Created instance pointer is null");
      }

      return new PanamaInstance(instancePtr, panamaModule, this);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error creating instance with imports: " + e.getMessage(), e);
    }
  }

  /**
   * Extracts the native handle from a Panama Extern wrapper.
   *
   * @param ext the extern to extract the handle from
   * @return the native memory segment handle
   */
  private static MemorySegment extractExternHandle(final ai.tegmentum.wasmtime4j.Extern ext) {
    if (ext instanceof PanamaExternFunc) {
      return ((PanamaExternFunc) ext).getNativeHandle();
    } else if (ext instanceof PanamaExternGlobal) {
      return ((PanamaExternGlobal) ext).getNativeHandle();
    } else if (ext instanceof PanamaExternTable) {
      return ((PanamaExternTable) ext).getNativeHandle();
    } else if (ext instanceof PanamaExternMemory) {
      return ((PanamaExternMemory) ext).getNativeHandle();
    }
    throw new IllegalArgumentException("Unsupported extern type: " + ext.getClass().getName());
  }

  /**
   * Converts a Java ExternType to the native type code used by the Rust FFI layer.
   *
   * <p>Native codes: 0=Func, 1=Global, 2=Table, 3=Memory, 4=SharedMemory, 5=Tag
   *
   * @param type the Java ExternType
   * @return the native type code
   */
  private static int externTypeToNativeCode(final ai.tegmentum.wasmtime4j.type.ExternType type) {
    switch (type) {
      case FUNC:
        return 0;
      case GLOBAL:
        return 1;
      case TABLE:
        return 2;
      case MEMORY:
        return 3;
      case SHARED_MEMORY:
        return 4;
      case TAG:
        return 5;
      default:
        throw new IllegalArgumentException("Unknown extern type: " + type);
    }
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
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

  @Override
  public void gc() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.storeGc(nativeStore);
    if (result != 0) {
      throw PanamaErrorMapper.mapNativeError(result, "Failed to perform garbage collection");
    }
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
      throw PanamaErrorMapper.mapNativeError(result, "Failed to set epoch deadline async yield");
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
      throw PanamaErrorMapper.mapNativeError(result, "Failed to configure epoch deadline callback");
    }
  }

  // Callback holder to prevent garbage collection
  private ai.tegmentum.wasmtime4j.Store.EpochDeadlineCallback epochDeadlineCallback;

  @Override
  public void setDebugHandler(final ai.tegmentum.wasmtime4j.debug.DebugHandler handler) {
    if (handler == null) {
      throw new NullPointerException("handler cannot be null");
    }
    ensureNotClosed();

    // Remove previous debug handler registration if any
    final long oldDebugId = debugHandlerCallbackId.get();
    if (oldDebugId != 0) {
      DEBUG_HANDLERS.remove(oldDebugId);
      debugHandlerCallbackId.set(0);
    }

    this.debugHandler = handler;

    if (DEBUG_HANDLER_STUB == null) {
      throw new IllegalStateException("Debug handler callback infrastructure not initialized");
    }

    // Generate a unique callback ID and register the handler
    final long newDebugId = DEBUG_HANDLER_ID_COUNTER.getAndIncrement();
    DEBUG_HANDLERS.put(newDebugId, handler);
    debugHandlerCallbackId.set(newDebugId);

    final int result =
        NATIVE_BINDINGS.storeSetDebugHandlerFn(nativeStore, DEBUG_HANDLER_STUB, newDebugId);

    if (result != 0) {
      // Cleanup on failure
      DEBUG_HANDLERS.remove(newDebugId);
      debugHandlerCallbackId.set(0);
      throw new IllegalStateException("Failed to set debug handler: native error " + result);
    }
  }

  @Override
  public void clearDebugHandler() {
    ensureNotClosed();

    // Remove debug handler registration
    final long oldDebugId = debugHandlerCallbackId.get();
    if (oldDebugId != 0) {
      DEBUG_HANDLERS.remove(oldDebugId);
      debugHandlerCallbackId.set(0);
    }

    this.debugHandler = null;

    final int result = NATIVE_BINDINGS.storeClearDebugHandler(nativeStore);
    if (result != 0) {
      throw new IllegalStateException("Failed to clear debug handler: native error " + result);
    }
  }

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
    final AtomicLong debugId = this.debugHandlerCallbackId;
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
            RESOURCE_LIMITERS_ASYNC.remove(limiterVal);
          }

          // Clean up debug handler from static registry
          final long debugVal = debugHandlerCallbackId.getAndSet(0);
          if (debugVal != 0) {
            DEBUG_HANDLERS.remove(debugVal);
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
            RESOURCE_LIMITERS_ASYNC.remove(limiterVal);
          }
          final long debugVal = debugId.getAndSet(0);
          if (debugVal != 0) {
            DEBUG_HANDLERS.remove(debugVal);
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

  // ===== Backtrace API =====

  @Override
  public ai.tegmentum.wasmtime4j.debug.WasmBacktrace captureBacktrace()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    return captureBacktraceInternal(false);
  }

  @Override
  public ai.tegmentum.wasmtime4j.debug.WasmBacktrace forceCaptureBacktrace()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    return captureBacktraceInternal(true);
  }

  private ai.tegmentum.wasmtime4j.debug.WasmBacktrace captureBacktraceInternal(final boolean force)
      throws WasmException {
    try (final java.lang.foreign.Arena tempArena = java.lang.foreign.Arena.ofConfined()) {
      final java.lang.foreign.MemorySegment bufferOutPtr =
          tempArena.allocate(java.lang.foreign.ValueLayout.ADDRESS);
      final java.lang.foreign.MemorySegment bufferLenOutPtr =
          tempArena.allocate(java.lang.foreign.ValueLayout.JAVA_INT);

      final int result;
      if (force) {
        result =
            NATIVE_BINDINGS.storeForceCaptureBacktrace(nativeStore, bufferOutPtr, bufferLenOutPtr);
      } else {
        result = NATIVE_BINDINGS.storeCaptureBacktrace(nativeStore, bufferOutPtr, bufferLenOutPtr);
      }

      if (result < 0) {
        return new ai.tegmentum.wasmtime4j.debug.WasmBacktrace(
            java.util.Collections.emptyList(), force);
      }

      final java.lang.foreign.MemorySegment bufferPtr =
          bufferOutPtr.get(java.lang.foreign.ValueLayout.ADDRESS, 0);
      final int bufferLen = bufferLenOutPtr.get(java.lang.foreign.ValueLayout.JAVA_INT, 0);

      if (bufferPtr.equals(java.lang.foreign.MemorySegment.NULL) || bufferLen <= 0) {
        return new ai.tegmentum.wasmtime4j.debug.WasmBacktrace(
            java.util.Collections.emptyList(), force);
      }

      final java.lang.foreign.MemorySegment buffer = bufferPtr.reinterpret(bufferLen);
      final byte[] data = buffer.toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);

      // Free the native buffer
      NATIVE_BINDINGS.freeBuffer(bufferPtr, bufferLen);

      return ai.tegmentum.wasmtime4j.panama.util.BacktraceDeserializer.deserialize(data);
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error capturing backtrace: " + e.getMessage(), e);
    }
  }

  // ===== Debugging API =====

  @Override
  public boolean isSingleStep() {
    ensureNotClosed();
    try {
      final java.lang.invoke.MethodHandle handle = NATIVE_BINDINGS.getStoreIsSingleStep();
      if (handle == null) {
        return false;
      }
      final int result = (int) handle.invoke(nativeStore);
      return result == 1;
    } catch (final Throwable e) {
      return false;
    }
  }

  @Override
  public java.util.List<ai.tegmentum.wasmtime4j.debug.FrameHandle> debugExitFrames()
      throws WasmException {
    ensureNotClosed();
    try {
      final java.lang.invoke.MethodHandle handle = NATIVE_BINDINGS.getStoreDebugExitFrames();
      if (handle == null) {
        throw new WasmException("Panama store debug exit frames function not available");
      }

      // Phase 1: get frame count with null data pointer
      final java.lang.foreign.MemorySegment countSegment = arena.allocate(ValueLayout.JAVA_INT);
      final int countResult =
          (int) handle.invoke(nativeStore, java.lang.foreign.MemorySegment.NULL, countSegment);
      if (countResult == -1) {
        return java.util.Collections.emptyList(); // debugging not enabled
      }
      if (countResult < 0) {
        throw new WasmException("Failed to get debug exit frames: error code " + countResult);
      }
      final int frameCount = countSegment.get(ValueLayout.JAVA_INT, 0);
      if (frameCount <= 0) {
        return java.util.Collections.emptyList();
      }

      // Phase 2: allocate buffer and get frame data
      final java.lang.foreign.MemorySegment dataSegment =
          arena.allocate(ValueLayout.JAVA_INT, (long) frameCount * 4);
      final int dataResult = (int) handle.invoke(nativeStore, dataSegment, countSegment);
      if (dataResult < 0) {
        throw new WasmException("Failed to get debug exit frame data: error code " + dataResult);
      }

      // Parse frame data: 4 ints per frame [func_index, pc, num_locals, num_stacks]
      final java.util.List<ai.tegmentum.wasmtime4j.debug.FrameHandle> frames =
          new java.util.ArrayList<>(frameCount);
      for (int i = 0; i < frameCount; i++) {
        final long base = (long) i * 4 * ValueLayout.JAVA_INT.byteSize();
        frames.add(
            new ai.tegmentum.wasmtime4j.debug.FrameHandle(
                0L, // no native ptr for snapshot approach
                dataSegment.get(ValueLayout.JAVA_INT, base), // functionIndex
                dataSegment.get(ValueLayout.JAVA_INT, base + ValueLayout.JAVA_INT.byteSize()), // pc
                dataSegment.get(ValueLayout.JAVA_INT, base + 2 * ValueLayout.JAVA_INT.byteSize()),
                dataSegment.get(ValueLayout.JAVA_INT, base + 3 * ValueLayout.JAVA_INT.byteSize()),
                null, // instance (not available in snapshot)
                null)); // module (not available in snapshot)
      }
      return frames;
    } catch (final Throwable e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Error getting debug exit frames: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean isAsync() {
    ensureNotClosed();
    try {
      final java.lang.invoke.MethodHandle handle = NATIVE_BINDINGS.getStoreIsAsync();
      if (handle == null) {
        return false;
      }
      final int result = (int) handle.invoke(nativeStore);
      return result == 1;
    } catch (final Throwable e) {
      return false;
    }
  }

  @Override
  public java.util.Optional<java.util.List<ai.tegmentum.wasmtime4j.debug.Breakpoint>>
      breakpoints() {
    ensureNotClosed();
    try {
      final java.lang.invoke.MethodHandle handle = NATIVE_BINDINGS.getStoreBreakpointCount();
      if (handle == null) {
        return java.util.Optional.empty();
      }
      final int result = (int) handle.invoke(nativeStore);
      if (result == -1) {
        return java.util.Optional.empty(); // debugging not enabled
      }
      if (result < 0) {
        return java.util.Optional.empty(); // error
      }
      // We can return the count but not individual breakpoint details without iteration support
      return java.util.Optional.of(java.util.Collections.emptyList());
    } catch (final Throwable e) {
      return java.util.Optional.empty();
    }
  }

  @Override
  public java.util.Optional<ai.tegmentum.wasmtime4j.debug.BreakpointEditor> editBreakpoints() {
    ensureNotClosed();
    final java.lang.invoke.MethodHandle addHandle = NATIVE_BINDINGS.getStoreAddBreakpoint();
    final java.lang.invoke.MethodHandle removeHandle = NATIVE_BINDINGS.getStoreRemoveBreakpoint();
    final java.lang.invoke.MethodHandle singleStepHandle = NATIVE_BINDINGS.getStoreSetSingleStep();
    if (addHandle == null || removeHandle == null || singleStepHandle == null) {
      return java.util.Optional.empty();
    }
    final java.lang.foreign.MemorySegment storeRef = nativeStore;
    return java.util.Optional.of(
        new ai.tegmentum.wasmtime4j.debug.BreakpointEditor() {
          @Override
          public ai.tegmentum.wasmtime4j.debug.BreakpointEditor addBreakpoint(
              final ai.tegmentum.wasmtime4j.Module module, final int pc) {
            java.util.Objects.requireNonNull(module, "module cannot be null");
            if (pc < 0) {
              throw new IllegalArgumentException("pc cannot be negative: " + pc);
            }
            try {
              final java.lang.foreign.MemorySegment modulePtr =
                  ((PanamaModule) module).getNativeModule();
              addHandle.invoke(storeRef, modulePtr, pc);
            } catch (final Throwable e) {
              throw new RuntimeException("Failed to add breakpoint: " + e.getMessage(), e);
            }
            return this;
          }

          @Override
          public ai.tegmentum.wasmtime4j.debug.BreakpointEditor removeBreakpoint(
              final ai.tegmentum.wasmtime4j.Module module, final int pc) {
            java.util.Objects.requireNonNull(module, "module cannot be null");
            if (pc < 0) {
              throw new IllegalArgumentException("pc cannot be negative: " + pc);
            }
            try {
              final java.lang.foreign.MemorySegment modulePtr =
                  ((PanamaModule) module).getNativeModule();
              removeHandle.invoke(storeRef, modulePtr, pc);
            } catch (final Throwable e) {
              throw new RuntimeException("Failed to remove breakpoint: " + e.getMessage(), e);
            }
            return this;
          }

          @Override
          public ai.tegmentum.wasmtime4j.debug.BreakpointEditor singleStep(final boolean enabled) {
            try {
              singleStepHandle.invoke(storeRef, enabled ? 1 : 0);
            } catch (final Throwable e) {
              throw new RuntimeException("Failed to set single step: " + e.getMessage(), e);
            }
            return this;
          }

          @Override
          public void apply() {
            // Breakpoint edits are applied immediately via native calls
          }
        });
  }
}
