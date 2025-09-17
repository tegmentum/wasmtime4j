/*
 * Copyright 2024 Tegmentum AI
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
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly store interface.
 *
 * <p>A Store is an execution context that holds the runtime state for WebAssembly instances. This
 * implementation uses Panama FFI with Arena-based resource lifecycle management for optimal
 * performance and automatic cleanup.
 *
 * <p>Each store maintains isolated linear memory, globals, and execution state through direct
 * native calls. Objects like instances, functions, and memories are tied to a specific store and
 * managed through the Arena resource system.
 */
public final class PanamaStore implements Store, AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(PanamaStore.class.getName());

  // Core infrastructure components
  private final ArenaResourceManager resourceManager;
  private final NativeFunctionBindings nativeFunctions;
  private final PanamaEngine engine;
  private final ArenaResourceManager.ManagedNativeResource storeResource;

  // Store state
  private final AtomicReference<Object> userData = new AtomicReference<>();
  private volatile boolean closed = false;

  /**
   * Creates a new Panama store instance using Stream 1 infrastructure.
   *
   * @param engine the engine instance that creates this store
   * @throws WasmException if the store cannot be created
   */
  public PanamaStore(final PanamaEngine engine) throws WasmException {
    this.engine = Objects.requireNonNull(engine, "Engine cannot be null");
    this.resourceManager = engine.getResourceManager();
    this.nativeFunctions = NativeFunctionBindings.getInstance();

    if (!nativeFunctions.isInitialized()) {
      throw new WasmException("Native function bindings not initialized");
    }

    try {
      // Create the native store through FFI
      MemorySegment storePtr = createNativeStore(engine.getEnginePointer());
      PanamaErrorHandler.requireValidPointer(storePtr, "storePtr");

      // Create managed resource with cleanup
      this.storeResource =
          resourceManager.manageNativeResource(
              storePtr, () -> destroyNativeStoreInternal(storePtr), "Wasmtime Store");

      LOGGER.fine("Created Panama store instance with managed resource");

    } catch (Exception e) {
      throw new WasmException("Failed to create store", e);
    }
  }

  /**
   * Creates a new Panama store with custom configuration.
   *
   * @param engine the engine instance that creates this store
   * @param fuelLimit the fuel limit (0 = no limit)
   * @param memoryLimitBytes the memory limit in bytes (0 = no limit)
   * @param executionTimeoutSecs the execution timeout in seconds (0 = no timeout)
   * @param maxInstances the maximum number of instances (0 = no limit)
   * @param maxTableElements the maximum table elements (0 = no limit)
   * @param maxFunctions the maximum functions (0 = no limit)
   * @throws WasmException if the store cannot be created
   */
  public PanamaStore(
      final PanamaEngine engine,
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSecs,
      final int maxInstances,
      final int maxTableElements,
      final int maxFunctions)
      throws WasmException {
    this.engine = Objects.requireNonNull(engine, "Engine cannot be null");
    this.resourceManager = engine.getResourceManager();
    this.nativeFunctions = NativeFunctionBindings.getInstance();

    if (!nativeFunctions.isInitialized()) {
      throw new WasmException("Native function bindings not initialized");
    }

    try {
      // Create the native store with configuration through FFI
      MemorySegment storePtr =
          createNativeStoreWithConfig(
              engine.getEnginePointer(),
              fuelLimit,
              memoryLimitBytes,
              executionTimeoutSecs,
              maxInstances,
              maxTableElements,
              maxFunctions);
      PanamaErrorHandler.requireValidPointer(storePtr, "storePtr");

      // Create managed resource with cleanup
      this.storeResource =
          resourceManager.manageNativeResource(
              storePtr, () -> destroyNativeStoreInternal(storePtr), "Wasmtime Store");

      LOGGER.fine("Created Panama store instance with custom configuration");

    } catch (Exception e) {
      throw new WasmException("Failed to create store with configuration", e);
    }
  }

  @Override
  public Engine getEngine() {
    ensureNotClosed();
    return engine;
  }

  @Override
  public Object getData() {
    ensureNotClosed();
    return userData.get();
  }

  @Override
  public void setData(final Object data) {
    ensureNotClosed();
    userData.set(data);
    LOGGER.finest(
        "Set store user data: " + (data != null ? data.getClass().getSimpleName() : "null"));
  }

  @Override
  public void setFuel(final long fuel) throws WasmException {
    ensureNotClosed();

    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel cannot be negative: " + fuel);
    }

    try {
      // Set fuel through native FFI call
      int result = nativeFunctions.storeSetFuel(storeResource.getNativePointer(), fuel);

      PanamaErrorHandler.safeCheckError(result, "Store set fuel", "Failed to set fuel to " + fuel);

      LOGGER.fine("Successfully set store fuel to: " + fuel);

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Fuel setting", "fuel=" + fuel, e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public long getFuel() throws WasmException {
    ensureNotClosed();

    try {
      // Allocate memory for the fuel value output
      ArenaResourceManager.ManagedMemorySegment fuelOutPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeGetFuel(storeResource.getNativePointer(), fuelOutPtr.getSegment());

      PanamaErrorHandler.safeCheckError(result, "Store get fuel", "Failed to get remaining fuel");

      // Extract the fuel value
      long fuel = (long) MemoryLayouts.C_SIZE_T.varHandle().get(fuelOutPtr.getSegment(), 0);

      LOGGER.fine("Successfully retrieved store fuel: " + fuel);
      return fuel;

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Fuel retrieval", "store=" + storeResource.getNativePointer(), e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public void addFuel(final long fuel) throws WasmException {
    ensureNotClosed();

    if (fuel < 0) {
      throw new IllegalArgumentException("Fuel cannot be negative: " + fuel);
    }

    try {
      // Add fuel through native FFI call
      int result = nativeFunctions.storeAddFuel(storeResource.getNativePointer(), fuel);

      PanamaErrorHandler.safeCheckError(result, "Store add fuel", "Failed to add fuel " + fuel);

      LOGGER.fine("Successfully added fuel to store: " + fuel);

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Fuel addition", "fuel=" + fuel, e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public void setEpochDeadline(final long ticks) throws WasmException {
    ensureNotClosed();

    if (ticks < 0) {
      throw new IllegalArgumentException("Epoch ticks cannot be negative: " + ticks);
    }

    try {
      // Set epoch deadline through native FFI call
      int result = nativeFunctions.storeSetEpochDeadline(storeResource.getNativePointer(), ticks);

      PanamaErrorHandler.safeCheckError(
          result, "Store set epoch deadline", "Failed to set epoch deadline to " + ticks);

      LOGGER.fine("Successfully set epoch deadline: " + ticks);

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Epoch deadline setting", "ticks=" + ticks, e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public boolean isValid() {
    if (isClosed()) {
      return false;
    }

    try {
      // Validate store through native FFI call
      int result = nativeFunctions.storeValidate(storeResource.getNativePointer());
      return result == 0; // Success code

    } catch (Exception e) {
      LOGGER.warning("Store validation failed: " + e.getMessage());
      return false;
    }
  }

  /**
   * Gets runtime information about this store.
   *
   * <p>This method provides diagnostic information about the store's current state, including
   * memory usage, number of active instances, and other runtime metrics that can be useful for
   * debugging and monitoring.
   *
   * @return a string containing store runtime information
   * @throws WasmException if information cannot be retrieved
   * @throws IllegalStateException if this store has been closed
   */
  public String getRuntimeInfo() throws WasmException {
    ensureNotClosed();

    try {
      final StringBuilder info = new StringBuilder();
      info.append("Store Runtime Information:\n");
      info.append("  Handle: ").append(getStorePointer()).append("\n");
      info.append("  Valid: ").append(validate()).append("\n");
      info.append("  Execution Count: ").append(getExecutionCount()).append("\n");
      info.append("  Execution Time (ms): ").append(getExecutionTime()).append("\n");
      info.append("  Fuel Remaining: ").append(getFuel()).append("\n");
      info.append("  Fuel Consumed: ").append(getTotalFuelConsumed()).append("\n");
      info.append("  Total Memory (bytes): ").append(getTotalMemoryBytes()).append("\n");
      info.append("  Used Memory (bytes): ").append(getUsedMemoryBytes()).append("\n");
      info.append("  Active Instances: ").append(getInstanceCount()).append("\n");
      info.append("  Fuel Limit: ").append(getFuelLimit()).append("\n");
      info.append("  Memory Limit (bytes): ").append(getMemoryLimit()).append("\n");
      info.append("  Execution Timeout (secs): ").append(getExecutionTimeout());

      return info.toString();
    } catch (final Exception e) {
      throw new WasmException("Failed to get store runtime information", e);
    }
  }

  /**
   * Performs garbage collection within this store.
   *
   * <p>This method triggers garbage collection of unused WebAssembly resources within this store
   * context. This can help reclaim memory from instances, functions, and other WebAssembly objects
   * that are no longer reachable.
   *
   * <p>Note: This is separate from Java's garbage collection and only affects WebAssembly-specific
   * resources managed by this store.
   *
   * @throws WasmException if garbage collection fails
   * @throws IllegalStateException if this store has been closed
   */
  public void gc() throws WasmException {
    ensureNotClosed();

    try {
      int result = nativeFunctions.storeGarbageCollect(storeResource.getNativePointer());
      PanamaErrorHandler.safeCheckError(
          result, "Store garbage collection", "Store garbage collection failed");

      LOGGER.fine("Performed garbage collection for store " + getStorePointer());
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error during store garbage collection", e);
    }
  }

  /**
   * Gets the execution count for this store.
   *
   * <p>This method returns the total number of WebAssembly function executions that have occurred
   * within this store context. This includes both exported function calls and internal function
   * calls within WebAssembly modules.
   *
   * @return the number of executions performed in this store
   * @throws WasmException if the execution count cannot be retrieved
   * @throws IllegalStateException if this store has been closed
   */
  public long getExecutionCount() throws WasmException {
    ensureNotClosed();

    try {
      // Allocate memory for output parameters
      ArenaResourceManager.ManagedMemorySegment executionCountPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr1 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr2 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeGetExecutionStats(
              storeResource.getNativePointer(),
              executionCountPtr.getSegment(),
              dummyPtr1.getSegment(),
              dummyPtr2.getSegment());

      PanamaErrorHandler.safeCheckError(
          result, "Execution count retrieval", "Failed to get execution count");

      return (Long) MemoryLayouts.C_SIZE_T.varHandle().get(executionCountPtr.getSegment(), 0);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get execution count", e);
    }
  }

  /**
   * Gets the total execution time for this store.
   *
   * <p>This method returns the cumulative time spent executing WebAssembly code within this store
   * context, measured in milliseconds for high precision timing.
   *
   * @return the total execution time in milliseconds
   * @throws WasmException if the execution time cannot be retrieved
   * @throws IllegalStateException if this store has been closed
   */
  public long getExecutionTime() throws WasmException {
    ensureNotClosed();

    try {
      // Allocate memory for output parameters
      ArenaResourceManager.ManagedMemorySegment dummyPtr1 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment totalExecutionTimeMsPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr2 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeGetExecutionStats(
              storeResource.getNativePointer(),
              dummyPtr1.getSegment(),
              totalExecutionTimeMsPtr.getSegment(),
              dummyPtr2.getSegment());

      PanamaErrorHandler.safeCheckError(
          result, "Execution time retrieval", "Failed to get execution time");

      return (Long) MemoryLayouts.C_SIZE_T.varHandle().get(totalExecutionTimeMsPtr.getSegment(), 0);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get execution time", e);
    }
  }

  /**
   * Gets the total fuel consumed by this store.
   *
   * <p>This method returns the cumulative amount of fuel consumed by all WebAssembly executions
   * within this store. Fuel consumption tracking must be enabled in the engine configuration for
   * this metric to be meaningful.
   *
   * @return the total fuel consumed
   * @throws WasmException if the fuel consumption cannot be retrieved
   * @throws IllegalStateException if this store has been closed
   */
  public long getTotalFuelConsumed() throws WasmException {
    ensureNotClosed();

    try {
      // Allocate memory for output parameters
      ArenaResourceManager.ManagedMemorySegment dummyPtr1 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr2 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment fuelConsumedPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeGetExecutionStats(
              storeResource.getNativePointer(),
              dummyPtr1.getSegment(),
              dummyPtr2.getSegment(),
              fuelConsumedPtr.getSegment());

      PanamaErrorHandler.safeCheckError(
          result, "Fuel consumption retrieval", "Failed to get total fuel consumed");

      return (Long) MemoryLayouts.C_SIZE_T.varHandle().get(fuelConsumedPtr.getSegment(), 0);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get total fuel consumed", e);
    }
  }

  /**
   * Gets the total memory allocated by this store.
   *
   * <p>This method returns the total amount of memory (in bytes) that has been allocated for
   * WebAssembly linear memories, tables, and other runtime structures within this store context.
   *
   * @return the total memory in bytes
   * @throws WasmException if the memory information cannot be retrieved
   * @throws IllegalStateException if this store has been closed
   */
  public long getTotalMemoryBytes() throws WasmException {
    ensureNotClosed();

    try {
      // Allocate memory for output parameters
      ArenaResourceManager.ManagedMemorySegment totalBytesPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr1 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr2 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeGetMemoryUsage(
              storeResource.getNativePointer(),
              totalBytesPtr.getSegment(),
              dummyPtr1.getSegment(),
              dummyPtr2.getSegment());

      PanamaErrorHandler.safeCheckError(
          result, "Memory usage retrieval", "Failed to get total memory bytes");

      return (Long) MemoryLayouts.C_SIZE_T.varHandle().get(totalBytesPtr.getSegment(), 0);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get total memory bytes", e);
    }
  }

  /**
   * Gets the currently used memory by this store.
   *
   * <p>This method returns the amount of memory (in bytes) currently in use by active WebAssembly
   * instances, linear memories, and other runtime structures within this store. This represents the
   * subset of total allocated memory that is actively being used.
   *
   * @return the used memory in bytes
   * @throws WasmException if the memory information cannot be retrieved
   * @throws IllegalStateException if this store has been closed
   */
  public long getUsedMemoryBytes() throws WasmException {
    ensureNotClosed();

    try {
      // Allocate memory for output parameters
      ArenaResourceManager.ManagedMemorySegment dummyPtr1 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment usedBytesPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr2 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeGetMemoryUsage(
              storeResource.getNativePointer(),
              dummyPtr1.getSegment(),
              usedBytesPtr.getSegment(),
              dummyPtr2.getSegment());

      PanamaErrorHandler.safeCheckError(
          result, "Memory usage retrieval", "Failed to get used memory bytes");

      return (Long) MemoryLayouts.C_SIZE_T.varHandle().get(usedBytesPtr.getSegment(), 0);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get used memory bytes", e);
    }
  }

  /**
   * Gets the number of active instances in this store.
   *
   * <p>This method returns the current number of active WebAssembly module instances that are
   * associated with this store. Each instance represents a separate instantiation of a compiled
   * module with its own state and memory.
   *
   * @return the number of active instances
   * @throws WasmException if the instance count cannot be retrieved
   * @throws IllegalStateException if this store has been closed
   */
  public long getInstanceCount() throws WasmException {
    ensureNotClosed();

    try {
      // Allocate memory for output parameters
      ArenaResourceManager.ManagedMemorySegment dummyPtr1 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr2 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment instanceCountPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeGetMemoryUsage(
              storeResource.getNativePointer(),
              dummyPtr1.getSegment(),
              dummyPtr2.getSegment(),
              instanceCountPtr.getSegment());

      PanamaErrorHandler.safeCheckError(
          result, "Instance count retrieval", "Failed to get instance count");

      return (Long) MemoryLayouts.C_SIZE_T.varHandle().get(instanceCountPtr.getSegment(), 0);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get instance count", e);
    }
  }

  /**
   * Gets the fuel limit for this store.
   *
   * <p>This method returns the maximum amount of fuel that can be consumed by WebAssembly execution
   * within this store. If fuel tracking is disabled or no limit is set, this method returns -1.
   *
   * @return the fuel limit, or -1 if no limit is set or fuel tracking is disabled
   * @throws WasmException if the fuel limit cannot be retrieved
   * @throws IllegalStateException if this store has been closed
   */
  public long getFuelLimit() throws WasmException {
    ensureNotClosed();

    try {
      // Allocate memory for output parameters
      ArenaResourceManager.ManagedMemorySegment fuelLimitPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr1 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr2 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr3 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeGetMetadata(
              storeResource.getNativePointer(),
              fuelLimitPtr.getSegment(),
              dummyPtr1.getSegment(),
              dummyPtr2.getSegment(),
              dummyPtr3.getSegment());

      PanamaErrorHandler.safeCheckError(result, "Fuel limit retrieval", "Failed to get fuel limit");

      long fuelLimit = (Long) MemoryLayouts.C_SIZE_T.varHandle().get(fuelLimitPtr.getSegment(), 0);
      return fuelLimit == 0 ? -1 : fuelLimit;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get fuel limit", e);
    }
  }

  /**
   * Gets the memory limit for this store.
   *
   * <p>This method returns the maximum amount of memory (in bytes) that can be allocated for
   * WebAssembly linear memories and runtime structures within this store. If no memory limit is
   * set, this method returns -1.
   *
   * @return the memory limit in bytes, or -1 if no limit is set
   * @throws WasmException if the memory limit cannot be retrieved
   * @throws IllegalStateException if this store has been closed
   */
  public long getMemoryLimit() throws WasmException {
    ensureNotClosed();

    try {
      // Allocate memory for output parameters
      ArenaResourceManager.ManagedMemorySegment dummyPtr1 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment memoryLimitBytesPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr2 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr3 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeGetMetadata(
              storeResource.getNativePointer(),
              dummyPtr1.getSegment(),
              memoryLimitBytesPtr.getSegment(),
              dummyPtr2.getSegment(),
              dummyPtr3.getSegment());

      PanamaErrorHandler.safeCheckError(
          result, "Memory limit retrieval", "Failed to get memory limit");

      long memoryLimit =
          (Long) MemoryLayouts.C_SIZE_T.varHandle().get(memoryLimitBytesPtr.getSegment(), 0);
      return memoryLimit == 0 ? -1 : memoryLimit;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get memory limit", e);
    }
  }

  /**
   * Gets the execution timeout for this store.
   *
   * <p>This method returns the maximum execution time (in seconds) allowed for WebAssembly
   * operations within this store. If no timeout is set, this method returns -1.
   *
   * @return the execution timeout in seconds, or -1 if no timeout is set
   * @throws WasmException if the execution timeout cannot be retrieved
   * @throws IllegalStateException if this store has been closed
   */
  public long getExecutionTimeout() throws WasmException {
    ensureNotClosed();

    try {
      // Allocate memory for output parameters
      ArenaResourceManager.ManagedMemorySegment dummyPtr1 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr2 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment executionTimeoutSecsPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment dummyPtr3 =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeGetMetadata(
              storeResource.getNativePointer(),
              dummyPtr1.getSegment(),
              dummyPtr2.getSegment(),
              executionTimeoutSecsPtr.getSegment(),
              dummyPtr3.getSegment());

      PanamaErrorHandler.safeCheckError(
          result, "Execution timeout retrieval", "Failed to get execution timeout");

      long timeout =
          (Long) MemoryLayouts.C_SIZE_T.varHandle().get(executionTimeoutSecsPtr.getSegment(), 0);
      return timeout == 0 ? -1 : timeout;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get execution timeout", e);
    }
  }

  /**
   * Consumes fuel from the store.
   *
   * <p>This method allows manual consumption of fuel from the store's fuel supply. This can be
   * useful for implementing custom fuel accounting or for testing fuel-related functionality.
   *
   * @param fuelAmount the amount of fuel to consume (must be positive)
   * @return true if the fuel was successfully consumed, false if insufficient fuel is available
   * @throws WasmException if fuel consumption fails
   * @throws IllegalStateException if this store has been closed
   * @throws IllegalArgumentException if fuelAmount is negative or zero
   */
  public boolean consumeFuel(final long fuelAmount) throws WasmException {
    if (fuelAmount <= 0) {
      throw new IllegalArgumentException("Fuel amount must be positive: " + fuelAmount);
    }
    ensureNotClosed();

    try {
      // Allocate memory for output parameter
      ArenaResourceManager.ManagedMemorySegment fuelConsumedPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeConsumeFuel(
              storeResource.getNativePointer(), fuelAmount, fuelConsumedPtr.getSegment());

      if (result == 0) {
        long actualConsumed =
            (Long) MemoryLayouts.C_SIZE_T.varHandle().get(fuelConsumedPtr.getSegment(), 0);
        boolean success = actualConsumed == fuelAmount;
        if (success) {
          LOGGER.fine("Consumed " + fuelAmount + " fuel from store " + getStorePointer());
        }
        return success;
      } else {
        // Non-zero result could mean insufficient fuel, not necessarily an error
        return false;
      }
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error consuming fuel", e);
    }
  }

  /**
   * Validates the store and checks its current state.
   *
   * <p>This method performs comprehensive validation of the store's internal state and verifies
   * that all associated resources are in a consistent, valid state. This is useful for debugging
   * and ensuring store integrity.
   *
   * @return true if the store is valid and all checks pass, false otherwise
   * @throws IllegalStateException if this store has been closed
   */
  public boolean validate() {
    if (isClosed() || storeResource.getNativePointer().equals(MemorySegment.NULL)) {
      return false;
    }

    try {
      int result = nativeFunctions.storeValidate(storeResource.getNativePointer());
      return result == 0;
    } catch (final Exception e) {
      LOGGER.warning("Error validating store: " + e.getMessage());
      return false;
    }
  }

  /**
   * Gets comprehensive diagnostic information about this store.
   *
   * <p>This method returns a formatted string containing detailed diagnostic information about the
   * store's current state, configuration, and runtime metrics. This information is useful for
   * debugging, monitoring, and performance analysis.
   *
   * @return formatted diagnostic information
   * @throws WasmException if diagnostic information cannot be retrieved
   * @throws IllegalStateException if this store has been closed
   */
  public String getDiagnosticInfo() throws WasmException {
    return getRuntimeInfo();
  }

  /**
   * Checks if the store is in a healthy state.
   *
   * <p>This method performs a health check that combines validation with runtime state analysis. A
   * store is considered healthy if it passes validation and all key metrics are within expected
   * ranges.
   *
   * @return true if the store is healthy, false otherwise
   */
  public boolean isHealthy() {
    if (!isValid()) {
      return false;
    }

    try {
      // Check if any critical resources are exhausted
      final long usedMemory = getUsedMemoryBytes();
      final long totalMemory = getTotalMemoryBytes();

      // Memory usage should not exceed total memory (this would indicate corruption)
      if (usedMemory > totalMemory && totalMemory > 0) {
        LOGGER.warning("Store health check failed: used memory exceeds total memory");
        return false;
      }

      // Check if fuel system is consistent
      final long fuelLimit = getFuelLimit();
      final long fuelRemaining = getFuel();

      // If fuel is enabled, remaining fuel should not exceed limit
      if (fuelLimit >= 0 && fuelRemaining > fuelLimit) {
        LOGGER.warning("Store health check failed: remaining fuel exceeds limit");
        return false;
      }

      return true;
    } catch (final Exception e) {
      LOGGER.warning("Store health check failed due to exception: " + e.getMessage());
      return false;
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    synchronized (this) {
      if (closed) {
        return;
      }

      try {
        // Close the managed native resource - this triggers automatic cleanup
        storeResource.close();

        LOGGER.fine("Closed Panama store instance");
      } catch (Exception e) {
        LOGGER.warning("Error during store closure: " + e.getMessage());
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Gets the native store pointer for internal use.
   *
   * @return the native store handle
   * @throws IllegalStateException if the store is closed
   */
  public MemorySegment getStorePointer() {
    ensureNotClosed();
    return storeResource.getNativePointer();
  }

  /**
   * Gets the resource manager for this store.
   *
   * @return the resource manager
   * @throws IllegalStateException if the store is closed
   */
  public ArenaResourceManager getResourceManager() {
    ensureNotClosed();
    return resourceManager;
  }

  /**
   * Checks if the store is closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed || storeResource.isClosed();
  }

  /**
   * Instantiates a WebAssembly module in this store context.
   *
   * @param module the module to instantiate
   * @return the created instance
   * @throws WasmException if instantiation fails
   */
  public PanamaInstance instantiateModule(final PanamaModule module) throws WasmException {
    ensureNotClosed();
    Objects.requireNonNull(module, "Module cannot be null");

    if (module.isClosed()) {
      throw new IllegalArgumentException("Module is closed");
    }

    try {
      // Create instance using the module's internal method
      return module.createInstance(
          storeResource.getNativePointer(), java.util.Collections.emptyList());

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Module instantiation", "module=" + module.getModulePointer(), e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public Instance createInstance(final Module module) throws WasmException {
    Objects.requireNonNull(module, "Module cannot be null");
    ensureNotClosed();

    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException("Module must be a PanamaModule instance for Panama store");
    }

    return instantiateModule((PanamaModule) module);
  }

  @Override
  public WasmFunction createHostFunction(
      final String name, final FunctionType functionType, final HostFunction implementation)
      throws WasmException {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(functionType, "functionType cannot be null");
    Objects.requireNonNull(implementation, "implementation cannot be null");
    ensureNotClosed();

    // TODO: Implement PanamaHostFunction when host function support is added
    throw new UnsupportedOperationException(
        "Host function creation is not yet implemented in Panama backend. "
            + "Use JNI backend for host function support.");
  }

  /**
   * Creates a new native store through optimized FFI calls.
   *
   * @param enginePtr the engine pointer for the store context
   * @return the native store handle
   * @throws WasmException if the store cannot be created
   */
  private MemorySegment createNativeStore(final MemorySegment enginePtr) throws WasmException {
    PanamaErrorHandler.requireValidPointer(enginePtr, "enginePtr");

    try {
      // Allocate memory for store pointer output
      ArenaResourceManager.ManagedMemorySegment storeOutPtr =
          resourceManager.allocate(MemoryLayouts.C_POINTER);

      // Call native store creation function
      int result = nativeFunctions.storeCreate(enginePtr, storeOutPtr.getSegment());

      // Check for creation errors
      PanamaErrorHandler.safeCheckError(
          result, "Store creation", "WebAssembly store creation failed");

      // Extract the created store pointer
      MemorySegment storePtr =
          (MemorySegment) MemoryLayouts.C_POINTER.varHandle().get(storeOutPtr.getSegment(), 0);

      PanamaErrorHandler.requireValidPointer(storePtr, "created store pointer");

      LOGGER.fine("Successfully created native store: " + storePtr);
      return storePtr;

    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Native store creation", "engine=" + enginePtr, e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  /**
   * Creates a new native store with custom configuration through optimized FFI calls.
   *
   * @param enginePtr the engine pointer for the store context
   * @param fuelLimit the fuel limit (0 = no limit)
   * @param memoryLimitBytes the memory limit in bytes (0 = no limit)
   * @param executionTimeoutSecs the execution timeout in seconds (0 = no timeout)
   * @param maxInstances the maximum number of instances (0 = no limit)
   * @param maxTableElements the maximum table elements (0 = no limit)
   * @param maxFunctions the maximum functions (0 = no limit)
   * @return the native store handle
   * @throws WasmException if the store cannot be created
   */
  private MemorySegment createNativeStoreWithConfig(
      final MemorySegment enginePtr,
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSecs,
      final int maxInstances,
      final int maxTableElements,
      final int maxFunctions)
      throws WasmException {
    PanamaErrorHandler.requireValidPointer(enginePtr, "enginePtr");

    try {
      // Allocate memory for store pointer output
      ArenaResourceManager.ManagedMemorySegment storeOutPtr =
          resourceManager.allocate(MemoryLayouts.C_POINTER);

      // Call native store creation function with configuration
      int result =
          nativeFunctions.storeCreateWithConfig(
              enginePtr,
              storeOutPtr.getSegment(),
              fuelLimit,
              memoryLimitBytes,
              executionTimeoutSecs,
              maxInstances,
              maxTableElements,
              maxFunctions);

      // Check for creation errors
      PanamaErrorHandler.safeCheckError(
          result,
          "Store creation with config",
          "WebAssembly store creation with configuration failed");

      // Extract the created store pointer
      MemorySegment storePtr =
          (MemorySegment) MemoryLayouts.C_POINTER.varHandle().get(storeOutPtr.getSegment(), 0);

      PanamaErrorHandler.requireValidPointer(storePtr, "created configured store pointer");

      LOGGER.fine("Successfully created native store with configuration: " + storePtr);
      return storePtr;

    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Native configured store creation",
              "engine=" + enginePtr + ", fuel=" + fuelLimit + ", memory=" + memoryLimitBytes,
              e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  /**
   * Internal cleanup method for native store destruction.
   *
   * @param storePtr the native store handle to destroy
   */
  private void destroyNativeStoreInternal(final MemorySegment storePtr) {
    try {
      if (storePtr != null && !storePtr.equals(MemorySegment.NULL)) {
        nativeFunctions.storeDestroy(storePtr);
        LOGGER.fine("Destroyed native store with pointer: " + storePtr);
      }
    } catch (Exception e) {
      // Log but don't throw - this is called during cleanup
      LOGGER.warning("Failed to destroy native store: " + e.getMessage());
    }
  }

  /**
   * Gets execution statistics for the store.
   *
   * @return execution statistics object containing counts and timing information
   * @throws WasmException if statistics retrieval fails
   */
  public ExecutionStats getExecutionStats() throws WasmException {
    ensureNotClosed();

    try {
      // Allocate memory for output values
      ArenaResourceManager.ManagedMemorySegment executionCountPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment fuelConsumedPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment totalExecutionTimeNsPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeGetExecutionStats(
              storeResource.getNativePointer(),
              executionCountPtr.getSegment(),
              fuelConsumedPtr.getSegment(),
              totalExecutionTimeNsPtr.getSegment());

      PanamaErrorHandler.safeCheckError(
          result, "Store execution stats", "Failed to get execution statistics");

      // Extract values
      long executionCount =
          (long) MemoryLayouts.C_SIZE_T.varHandle().get(executionCountPtr.getSegment(), 0);
      long fuelConsumed =
          (long) MemoryLayouts.C_SIZE_T.varHandle().get(fuelConsumedPtr.getSegment(), 0);
      long totalExecutionTimeNs =
          (long) MemoryLayouts.C_SIZE_T.varHandle().get(totalExecutionTimeNsPtr.getSegment(), 0);

      LOGGER.fine(
          "Retrieved execution statistics: count=" + executionCount + ", fuel=" + fuelConsumed);
      return new ExecutionStats(executionCount, fuelConsumed, totalExecutionTimeNs);

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Execution stats retrieval",
              "store=" + storeResource.getNativePointer(),
              e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  /**
   * Gets memory usage statistics for the store.
   *
   * @return memory usage statistics object
   * @throws WasmException if statistics retrieval fails
   */
  public MemoryUsage getMemoryUsage() throws WasmException {
    ensureNotClosed();

    try {
      // Allocate memory for output values
      ArenaResourceManager.ManagedMemorySegment totalBytesPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment usedBytesPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      ArenaResourceManager.ManagedMemorySegment instanceCountPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);

      int result =
          nativeFunctions.storeGetMemoryUsage(
              storeResource.getNativePointer(),
              totalBytesPtr.getSegment(),
              usedBytesPtr.getSegment(),
              instanceCountPtr.getSegment());

      PanamaErrorHandler.safeCheckError(
          result, "Store memory usage", "Failed to get memory usage statistics");

      // Extract values
      long totalBytes =
          (long) MemoryLayouts.C_SIZE_T.varHandle().get(totalBytesPtr.getSegment(), 0);
      long usedBytes = (long) MemoryLayouts.C_SIZE_T.varHandle().get(usedBytesPtr.getSegment(), 0);
      long instanceCount =
          (long) MemoryLayouts.C_SIZE_T.varHandle().get(instanceCountPtr.getSegment(), 0);

      LOGGER.fine(
          "Retrieved memory usage: total="
              + totalBytes
              + ", used="
              + usedBytes
              + ", instances="
              + instanceCount);
      return new MemoryUsage(totalBytes, usedBytes, instanceCount);

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Memory usage retrieval",
              "store=" + storeResource.getNativePointer(),
              e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  /**
   * Ensures that this store instance is not closed.
   *
   * @throws IllegalStateException if the store is closed
   */
  private void ensureNotClosed() {
    if (isClosed()) {
      throw new IllegalStateException("Store has been closed");
    }
  }

  /** Execution statistics data class. */
  public static class ExecutionStats {
    private final long executionCount;
    private final long fuelConsumed;
    private final long totalExecutionTimeNs;

    /**
     * Creates execution statistics instance.
     *
     * @param executionCount total number of executions
     * @param fuelConsumed total fuel consumed
     * @param totalExecutionTimeNs total execution time in nanoseconds
     */
    public ExecutionStats(
        final long executionCount, final long fuelConsumed, final long totalExecutionTimeNs) {
      this.executionCount = executionCount;
      this.fuelConsumed = fuelConsumed;
      this.totalExecutionTimeNs = totalExecutionTimeNs;
    }

    public long getExecutionCount() {
      return executionCount;
    }

    public long getFuelConsumed() {
      return fuelConsumed;
    }

    public long getTotalExecutionTimeNs() {
      return totalExecutionTimeNs;
    }

    @Override
    public String toString() {
      return String.format(
          "ExecutionStats{executions=%d, fuel=%d, time=%dns}",
          executionCount, fuelConsumed, totalExecutionTimeNs);
    }
  }

  /** Memory usage statistics data class. */
  public static class MemoryUsage {
    private final long totalBytes;
    private final long usedBytes;
    private final long instanceCount;

    /**
     * Creates memory usage statistics instance.
     *
     * @param totalBytes total bytes allocated
     * @param usedBytes bytes currently used
     * @param instanceCount number of active instances
     */
    public MemoryUsage(final long totalBytes, final long usedBytes, final long instanceCount) {
      this.totalBytes = totalBytes;
      this.usedBytes = usedBytes;
      this.instanceCount = instanceCount;
    }

    public long getTotalBytes() {
      return totalBytes;
    }

    public long getUsedBytes() {
      return usedBytes;
    }

    public long getInstanceCount() {
      return instanceCount;
    }

    @Override
    public String toString() {
      return String.format(
          "MemoryUsage{total=%d bytes, used=%d bytes, instances=%d}",
          totalBytes, usedBytes, instanceCount);
    }
  }
}
