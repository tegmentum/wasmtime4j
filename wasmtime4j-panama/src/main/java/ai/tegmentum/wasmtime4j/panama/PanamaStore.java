package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
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

  private final PanamaEngine engine;
  private final Arena arena;
  private final MemorySegment nativeStore;
  private final AtomicReference<Object> userData = new AtomicReference<>();
  private final ArenaResourceManager resourceManager;
  private volatile boolean closed = false;

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
    this.resourceManager = new ArenaResourceManager(arena, true);

    LOGGER.fine("Created Panama store");
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
      final ai.tegmentum.wasmtime4j.FunctionType functionType,
      final ai.tegmentum.wasmtime4j.HostFunction implementation)
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

    // Create error handler for this host function
    final PanamaErrorHandler errorHandler = new PanamaErrorHandler();

    // Create the Panama host function
    final PanamaHostFunction hostFunction =
        new PanamaHostFunction(name, functionType, callback, resourceManager, errorHandler);

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
        case FUNCREF:
        case EXTERNREF:
          if (initialValue.getValue() != null) {
            refIdPresent = 1;
            refId = (Long) initialValue.getValue();
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
  public ai.tegmentum.wasmtime4j.FunctionReference createFunctionReference(
      final ai.tegmentum.wasmtime4j.HostFunction implementation,
      final ai.tegmentum.wasmtime4j.FunctionType functionType)
      throws WasmException {
    ensureNotClosed();
    // TODO: Implement function reference creation from host function
    throw new UnsupportedOperationException(
        "Function reference from host function not yet implemented - requires callback"
            + " infrastructure");
  }

  @Override
  public ai.tegmentum.wasmtime4j.FunctionReference createFunctionReference(
      final ai.tegmentum.wasmtime4j.WasmFunction function) throws WasmException {
    ensureNotClosed();
    // TODO: Implement function reference creation from WASM function
    throw new UnsupportedOperationException(
        "Function reference from WASM function not yet implemented - requires callback"
            + " infrastructure");
  }

  @Override
  public ai.tegmentum.wasmtime4j.CallbackRegistry getCallbackRegistry() {
    // TODO: Implement callback registry - requires arena manager and error handler infrastructure
    throw new UnsupportedOperationException(
        "Callback registry not yet implemented - requires callback infrastructure");
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

    try {
      // Close resource manager first (cleans up host functions and managed resources)
      if (resourceManager != null) {
        resourceManager.close();
      }

      // Destroy native store
      if (nativeStore != null && !nativeStore.equals(MemorySegment.NULL)) {
        NATIVE_BINDINGS.storeDestroy(nativeStore);
      }
      arena.close();
      closed = true;
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
  public ai.tegmentum.wasmtime4j.WasmBacktrace captureBacktrace() {
    ensureNotClosed();
    return captureBacktraceInternal(false);
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmBacktrace forceCaptureBacktrace() {
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
  private ai.tegmentum.wasmtime4j.WasmBacktrace captureBacktraceInternal(
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
        return new ai.tegmentum.wasmtime4j.WasmBacktrace(new ArrayList<>(), forceCapture);
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
}
