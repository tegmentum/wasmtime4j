package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExternRef;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiter;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
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
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

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

  private final PanamaEngine engine;
  private final Arena arena;
  private final MemorySegment nativeStore;
  private final AtomicReference<Object> userData = new AtomicReference<>();
  private final ArenaResourceManager resourceManager;
  private final PanamaErrorHandler errorHandler;
  private volatile PanamaCallbackRegistry callbackRegistry; // Lazy initialized
  private volatile boolean closed = false;

  // Epoch callback ID for this store (0 = no callback registered)
  private volatile long epochCallbackId = 0;

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

    // Use singleton error handler (callbackRegistry is lazy initialized)
    this.errorHandler = PanamaErrorHandler.getInstance();

    LOGGER.fine("Created Panama store");
  }

  /**
   * Creates a new Panama store with resource limits.
   *
   * @param engine the engine that owns this store
   * @param limits the resource limits to apply
   * @throws WasmException if store creation fails
   */
  public PanamaStore(final PanamaEngine engine, final ai.tegmentum.wasmtime4j.config.StoreLimits limits)
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
      throw new WasmException("Failed to create native store with limits: error code " + result);
    }

    this.nativeStore = storePtr.get(ValueLayout.ADDRESS, 0);

    if (this.nativeStore == null || this.nativeStore.equals(MemorySegment.NULL)) {
      arena.close();
      throw new WasmException("Failed to create native store with limits");
    }

    // Create resource manager for host functions and other managed resources
    this.resourceManager = new ArenaResourceManager(arena, false);

    // Use singleton error handler (callbackRegistry is lazy initialized)
    this.errorHandler = PanamaErrorHandler.getInstance();

    LOGGER.fine("Created Panama store with limits: " + limits);
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

    // Use singleton error handler (callbackRegistry is lazy initialized)
    this.errorHandler = PanamaErrorHandler.getInstance();

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
  public long getRemainingFuel() throws WasmException {
    return getFuel();
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

    // Use singleton error handler for this host function
    final PanamaErrorHandler errorHandler = PanamaErrorHandler.getInstance();

    // Create the Panama host function with implementation and store reference for caller context
    final PanamaHostFunction hostFunction =
        new PanamaHostFunction(
            name, functionType, callback, implementation, this, resourceManager, errorHandler);

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
      final MethodHandle createHandle = NATIVE_BINDINGS.getPanamaGlobalCreate();
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
        throw new WasmException("Failed to create global");
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
      final MethodHandle createHandle = NATIVE_BINDINGS.getPanamaTableCreate();
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
      final MethodHandle createHandle = NATIVE_BINDINGS.getPanamaMemoryCreateWithConfig();
      if (createHandle == null) {
        throw new WasmException("Panama memory creation function not available");
      }

      final int maximumPages = (maxPages == -1) ? 0 : maxPages;
      final int isShared = 0; // Not shared
      final int memoryIndex = 0; // Not used for direct creation

      final MemorySegment memoryPtr = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              createHandle.invoke(
                  nativeStore,
                  initialPages,
                  maximumPages,
                  isShared,
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
      final MethodHandle createHandle = NATIVE_BINDINGS.getPanamaMemoryCreateWithConfig();
      if (createHandle == null) {
        throw new WasmException("Panama memory creation function not available");
      }

      final int isShared = 1; // Shared memory
      final int memoryIndex = 0; // Not used for direct creation

      final MemorySegment memoryPtr = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              createHandle.invoke(
                  nativeStore,
                  initialPages,
                  maxPages,
                  isShared,
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
    PanamaValidation.requireNonNull(implementation, "implementation");
    PanamaValidation.requireNonNull(functionType, "functionType");

    return new PanamaFunctionReference(
        implementation, functionType, this, resourceManager, errorHandler);
  }

  @Override
  public ai.tegmentum.wasmtime4j.func.FunctionReference createFunctionReference(
      final ai.tegmentum.wasmtime4j.WasmFunction function) throws WasmException {
    ensureNotClosed();
    PanamaValidation.requireNonNull(function, "function");

    // Create a host function wrapper that delegates to the wasm function
    final ai.tegmentum.wasmtime4j.func.HostFunction wrapper = function::call;

    return new PanamaFunctionReference(
        wrapper, function.getFunctionType(), this, resourceManager, errorHandler);
  }

  @Override
  public ai.tegmentum.wasmtime4j.func.CallbackRegistry getCallbackRegistry() {
    ensureNotClosed();
    // Lazy initialization of callback registry
    if (callbackRegistry == null) {
      synchronized (this) {
        if (callbackRegistry == null) {
          callbackRegistry = new PanamaCallbackRegistry(this, resourceManager, errorHandler);
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
      final MethodHandle createHandle = NATIVE_BINDINGS.getPanamaInstanceCreate();
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
    return !closed;
  }

  @Override
  public void limiter(final ResourceLimiter limiter) throws WasmException {
    // Resource limiter not currently supported in Panama implementation
    throw new UnsupportedOperationException(
        "Resource limiter not yet supported in Panama implementation");
  }

  @Override
  public void limiterAsync(final AsyncResourceLimiter limiter) throws WasmException {
    // Async resource limiter not currently supported in Panama implementation
    throw new UnsupportedOperationException(
        "Async resource limiter not yet supported in Panama implementation");
  }

  @Override
  public ResourceLimiter getLimiter() {
    // Panama implementation does not currently support resource limiter retrieval
    return null;
  }

  @Override
  public long getTotalFuelConsumed() throws WasmException {
    ensureNotClosed();
    final ExecutionStats stats = getExecutionStats();
    return stats.fuelConsumed;
  }

  @Override
  public long getExecutionCount() {
    if (closed) {
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
    if (closed) {
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
    if (closed) {
      return;
    }
    // Mark as closed immediately to prevent reuse during cleanup
    closed = true;

    try {
      // Clean up epoch callback from static registry
      if (epochCallbackId != 0) {
        EPOCH_CALLBACKS.remove(epochCallbackId);
        epochCallbackId = 0;
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
      LOGGER.fine("Closed Panama store");
    } catch (final Exception e) {
      LOGGER.warning("Error closing store: " + e.getMessage());
    }
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
      final MemorySegment totalExecutionTimeMsSegment = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment fuelConsumedSegment = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int)
              getStatsHandle.invoke(
                  nativeStore,
                  executionCountSegment,
                  totalExecutionTimeMsSegment,
                  fuelConsumedSegment);

      if (result != 0) {
        throw new WasmException("Failed to get execution statistics");
      }

      final long executionCount = executionCountSegment.get(ValueLayout.JAVA_LONG, 0);
      final long totalExecutionTimeMs = totalExecutionTimeMsSegment.get(ValueLayout.JAVA_LONG, 0);
      final long fuelConsumed = fuelConsumedSegment.get(ValueLayout.JAVA_LONG, 0);

      return new ExecutionStats(executionCount, totalExecutionTimeMs * 1000L, fuelConsumed);
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
        throw new RuntimeException("Failed to capture backtrace: error code " + result);
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
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to perform garbage collection: error code " + result);
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
    if (closed) {
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
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to set epoch deadline async yield: error code " + result);
    }
  }

  @Override
  public void epochDeadlineTrap() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.storeEpochDeadlineTrap(nativeStore);
    if (result != 0) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to set epoch deadline trap: error code " + result);
    }
  }

  @Override
  public void epochDeadlineCallback(
      final ai.tegmentum.wasmtime4j.Store.EpochDeadlineCallback callback)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();

    LOGGER.fine("epochDeadlineCallback called, callback=" + callback);

    // Remove existing callback if any
    if (epochCallbackId != 0) {
      EPOCH_CALLBACKS.remove(epochCallbackId);
      epochCallbackId = 0;
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
      epochCallbackId = newCallbackId;

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
        epochCallbackId = 0;
      }
    }

    if (result != 0) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to configure epoch deadline callback: error code " + result);
    }
  }

  // Callback holder to prevent garbage collection
  private ai.tegmentum.wasmtime4j.Store.EpochDeadlineCallback epochDeadlineCallback;

  // Call hook support
  private ai.tegmentum.wasmtime4j.func.CallHookHandler callHookHandler;
  private ai.tegmentum.wasmtime4j.Store.AsyncCallHookHandler asyncCallHookHandler;

  @Override
  public void setCallHook(final ai.tegmentum.wasmtime4j.func.CallHookHandler handler)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    this.callHookHandler = handler;
    int result;
    if (handler == null) {
      result = NATIVE_BINDINGS.storeClearCallHook(nativeStore);
    } else {
      result = NATIVE_BINDINGS.storeSetCallHook(nativeStore);
    }
    if (result != 0) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to configure call hook: error code " + result);
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
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Failed to configure async call hook: error code " + result);
    }
  }

  /**
   * Ensures the store is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Store has been closed");
    }
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

  // ===== Concurrency Methods =====

  @Override
  public <T, R> R runConcurrent(final ai.tegmentum.wasmtime4j.concurrent.ConcurrentTask<T, R> task)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    if (task == null) {
      throw new IllegalArgumentException("Task cannot be null");
    }
    // Create accessor wrapper
    @SuppressWarnings("unchecked")
    final PanamaAccessor<T> accessor =
        new PanamaAccessor<>((T) userData.get(), nativeStore.address());
    try {
      return task.execute(accessor);
    } finally {
      accessor.invalidate();
    }
  }

  @Override
  public <R> ai.tegmentum.wasmtime4j.concurrent.JoinHandle<R> spawn(
      final ai.tegmentum.wasmtime4j.concurrent.SpawnableTask<R> task)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    if (task == null) {
      throw new IllegalArgumentException("Task cannot be null");
    }
    return new PanamaJoinHandle<>(task);
  }

  // ===== Debug Methods =====

  @Override
  public java.util.List<ai.tegmentum.wasmtime4j.debug.DebugFrame> debugFrames()
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    // Return empty list - debug frames require native implementation
    return java.util.Collections.emptyList();
  }

  private ai.tegmentum.wasmtime4j.debug.DebugHandler debugHandler;

  @Override
  public void setDebugHandler(final ai.tegmentum.wasmtime4j.debug.DebugHandler handler)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    this.debugHandler = handler;
    // Native binding would be called here
  }

  // ===== Fuel Async Methods =====

  private long fuelAsyncYieldInterval = 0;

  @Override
  public void setFuelAsyncYieldInterval(final long interval)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    ensureNotClosed();
    if (interval < 0) {
      throw new IllegalArgumentException("Interval cannot be negative");
    }
    this.fuelAsyncYieldInterval = interval;
    // Native binding would be called here: NATIVE_BINDINGS.storeSetFuelAsyncYieldInterval(...)
  }

  @Override
  public long getFuelAsyncYieldInterval() {
    return fuelAsyncYieldInterval;
  }

  // ===== Inner Classes =====

  /** Panama implementation of Accessor for concurrent store access. */
  private static final class PanamaAccessor<T>
      implements ai.tegmentum.wasmtime4j.concurrent.Accessor<T> {
    private final T data;
    private final long storeId;
    private volatile boolean valid = true;

    PanamaAccessor(final T data, final long storeId) {
      this.data = data;
      this.storeId = storeId;
    }

    @Override
    public T getData() {
      return data;
    }

    @Override
    public boolean isValid() {
      return valid;
    }

    @Override
    public long getStoreId() {
      return storeId;
    }

    @Override
    public void complete() throws ai.tegmentum.wasmtime4j.exception.WasmException {
      valid = false;
    }

    @Override
    public void fail(final Throwable error) throws ai.tegmentum.wasmtime4j.exception.WasmException {
      valid = false;
    }

    void invalidate() {
      valid = false;
    }
  }

  /** Panama implementation of JoinHandle for spawned tasks. */
  private static final class PanamaJoinHandle<T>
      implements ai.tegmentum.wasmtime4j.concurrent.JoinHandle<T> {
    private static final java.util.concurrent.atomic.AtomicLong ID_COUNTER =
        new java.util.concurrent.atomic.AtomicLong(0);

    private final long id;
    private final java.util.concurrent.CompletableFuture<T> future;

    PanamaJoinHandle(final ai.tegmentum.wasmtime4j.concurrent.SpawnableTask<T> task) {
      this.id = ID_COUNTER.incrementAndGet();
      this.future =
          java.util.concurrent.CompletableFuture.supplyAsync(
              () -> {
                try {
                  return task.run();
                } catch (ai.tegmentum.wasmtime4j.exception.WasmException e) {
                  throw new RuntimeException(e);
                }
              });
    }

    @Override
    public T join() throws ai.tegmentum.wasmtime4j.exception.WasmException, InterruptedException {
      try {
        return future.get();
      } catch (java.util.concurrent.ExecutionException e) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Task failed: " + e.getCause().getMessage(), e.getCause());
      }
    }

    @Override
    public T join(final long timeout, final java.util.concurrent.TimeUnit unit)
        throws ai.tegmentum.wasmtime4j.exception.WasmException, InterruptedException {
      try {
        return future.get(timeout, unit);
      } catch (java.util.concurrent.ExecutionException e) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Task failed: " + e.getCause().getMessage(), e.getCause());
      } catch (java.util.concurrent.TimeoutException e) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException("Task timed out", e);
      }
    }

    @Override
    public java.util.concurrent.CompletableFuture<T> toFuture() {
      return future;
    }

    @Override
    public boolean isDone() {
      return future.isDone();
    }

    @Override
    public boolean isCancelled() {
      return future.isCancelled();
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
      return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public long getId() {
      return id;
    }

    @Override
    public ai.tegmentum.wasmtime4j.concurrent.JoinHandle.TaskStatus getStatus() {
      if (future.isCancelled()) {
        return ai.tegmentum.wasmtime4j.concurrent.JoinHandle.TaskStatus.CANCELLED;
      }
      if (future.isDone()) {
        if (future.isCompletedExceptionally()) {
          return ai.tegmentum.wasmtime4j.concurrent.JoinHandle.TaskStatus.FAILED;
        }
        return ai.tegmentum.wasmtime4j.concurrent.JoinHandle.TaskStatus.COMPLETED;
      }
      return ai.tegmentum.wasmtime4j.concurrent.JoinHandle.TaskStatus.RUNNING;
    }
  }
}
