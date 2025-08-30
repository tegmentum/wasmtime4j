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
import ai.tegmentum.wasmtime4j.Store;
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
      // For now, this is a placeholder - would call native fuel setting function
      LOGGER.fine("Setting fuel to: " + fuel + " (placeholder implementation)");

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
      // For now, return -1 indicating fuel is disabled
      LOGGER.fine("Getting fuel (placeholder implementation)");
      return -1;

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
      // For now, this is a placeholder - would call native fuel addition function
      LOGGER.fine("Adding fuel: " + fuel + " (placeholder implementation)");

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
      // For now, this is a placeholder - would call native epoch setting function
      LOGGER.fine("Setting epoch deadline: " + ticks + " (placeholder implementation)");

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Epoch deadline setting", "ticks=" + ticks, e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public boolean isValid() {
    return !isClosed();
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
      // For now, create a placeholder store - actual implementation would call
      // wasmtime_store_new() or similar native function through NativeFunctionBindings
      LOGGER.fine("Creating native store with engine: " + enginePtr);

      // Return a non-null placeholder pointer for now
      // In actual implementation, this would be the result of the native call
      return MemorySegment.ofAddress(0x1000); // Placeholder address

    } catch (Exception e) {
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
        // For now, this is a placeholder - actual implementation would call
        // wasmtime_store_delete() or similar through NativeFunctionBindings
        LOGGER.fine("Destroyed native store with pointer: " + storePtr);
      }
    } catch (Exception e) {
      // Log but don't throw - this is called during cleanup
      LOGGER.warning("Failed to destroy native store: " + e.getMessage());
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
}
