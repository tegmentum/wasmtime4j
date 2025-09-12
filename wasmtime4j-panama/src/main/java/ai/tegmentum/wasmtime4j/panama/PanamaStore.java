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
      
      PanamaErrorHandler.safeCheckError(
          result, "Store set fuel", "Failed to set fuel to " + fuel);

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
      // Get fuel through native FFI call
      ArenaResourceManager.ManagedMemorySegment fuelOutPtr =
          resourceManager.allocate(MemoryLayouts.C_SIZE_T);
      
      int result = nativeFunctions.storeGetFuel(
          storeResource.getNativePointer(), fuelOutPtr.getSegment());
      
      PanamaErrorHandler.safeCheckError(
          result, "Store get fuel", "Failed to get remaining fuel");

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
      
      PanamaErrorHandler.safeCheckError(
          result, "Store add fuel", "Failed to add fuel " + fuel);

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
   * Triggers garbage collection for the store.
   *
   * @throws WasmException if garbage collection fails
   */
  public void gc() throws WasmException {
    ensureNotClosed();

    try {
      int result = nativeFunctions.storeGc(storeResource.getNativePointer());
      
      PanamaErrorHandler.safeCheckError(
          result, "Store garbage collection", "Failed to trigger garbage collection");

      LOGGER.fine("Successfully triggered garbage collection for store");

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Garbage collection", "store=" + storeResource.getNativePointer(), e.getMessage());
      throw new WasmException(detailedMessage, e);
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

      int result = nativeFunctions.storeGetExecutionStats(
          storeResource.getNativePointer(),
          executionCountPtr.getSegment(),
          fuelConsumedPtr.getSegment(),
          totalExecutionTimeNsPtr.getSegment());

      PanamaErrorHandler.safeCheckError(
          result, "Store execution stats", "Failed to get execution statistics");

      // Extract values
      long executionCount = (long) MemoryLayouts.C_SIZE_T.varHandle().get(executionCountPtr.getSegment(), 0);
      long fuelConsumed = (long) MemoryLayouts.C_SIZE_T.varHandle().get(fuelConsumedPtr.getSegment(), 0);
      long totalExecutionTimeNs =
          (long) MemoryLayouts.C_SIZE_T.varHandle().get(totalExecutionTimeNsPtr.getSegment(), 0);

      LOGGER.fine("Retrieved execution statistics: count=" + executionCount + ", fuel=" + fuelConsumed);
      return new ExecutionStats(executionCount, fuelConsumed, totalExecutionTimeNs);

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Execution stats retrieval", "store=" + storeResource.getNativePointer(), e.getMessage());
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

      int result = nativeFunctions.storeGetMemoryUsage(
          storeResource.getNativePointer(),
          totalBytesPtr.getSegment(),
          usedBytesPtr.getSegment(),
          instanceCountPtr.getSegment());

      PanamaErrorHandler.safeCheckError(
          result, "Store memory usage", "Failed to get memory usage statistics");

      // Extract values
      long totalBytes = (long) MemoryLayouts.C_SIZE_T.varHandle().get(totalBytesPtr.getSegment(), 0);
      long usedBytes = (long) MemoryLayouts.C_SIZE_T.varHandle().get(usedBytesPtr.getSegment(), 0);
      long instanceCount = (long) MemoryLayouts.C_SIZE_T.varHandle().get(instanceCountPtr.getSegment(), 0);

      LOGGER.fine(
          "Retrieved memory usage: total=" + totalBytes + ", used=" + usedBytes + ", instances=" + instanceCount);
      return new MemoryUsage(totalBytes, usedBytes, instanceCount);

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Memory usage retrieval", "store=" + storeResource.getNativePointer(), e.getMessage());
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

  /**
   * Execution statistics data class.
   */
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
    public ExecutionStats(final long executionCount, final long fuelConsumed, final long totalExecutionTimeNs) {
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
      return String.format("ExecutionStats{executions=%d, fuel=%d, time=%dns}", 
          executionCount, fuelConsumed, totalExecutionTimeNs);
    }
  }

  /**
   * Memory usage statistics data class.
   */
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
      return String.format("MemoryUsage{total=%d bytes, used=%d bytes, instances=%d}", 
          totalBytes, usedBytes, instanceCount);
    }
  }
}
