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
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly engine interface.
 *
 * <p>The engine is responsible for configuring the WebAssembly execution environment and compiling
 * WebAssembly modules. This implementation uses Panama FFI with optimized method handles and
 * Arena-based resource management for direct native calls to Wasmtime's engine APIs.
 *
 * <p>Engine instances are lightweight and can be shared across multiple module compilations. The
 * engine manages compilation settings, optimization levels, and runtime configuration through
 * direct FFI calls.
 */
public final class PanamaEngine implements Engine, AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(PanamaEngine.class.getName());

  // Core infrastructure components
  private final ArenaResourceManager resourceManager;
  private final NativeFunctionBindings nativeFunctions;
  private final ArenaResourceManager.ManagedNativeResource engineResource;

  // Engine state
  private volatile boolean closed = false;

  @Override
  public boolean isValid() {
    return !closed;
  }

  /**
   * Creates a new Panama engine instance using Stream 1 infrastructure.
   *
   * @param resourceManager the arena resource manager for lifecycle management
   * @throws WasmException if the engine cannot be created
   */
  public PanamaEngine(final ArenaResourceManager resourceManager) throws WasmException {
    this.resourceManager =
        Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
    this.nativeFunctions = NativeFunctionBindings.getInstance();

    if (!nativeFunctions.isInitialized()) {
      throw new WasmException("Native function bindings not initialized");
    }

    try {
      // Create the native engine through FFI
      MemorySegment enginePtr = createNativeEngine();
      PanamaErrorHandler.requireValidPointer(enginePtr, "enginePtr");

      // Create managed resource with cleanup
      this.engineResource =
          resourceManager.manageNativeResource(
              enginePtr, () -> destroyNativeEngineInternal(enginePtr), "Wasmtime Engine");

      LOGGER.fine("Created Panama engine instance with managed resource");

    } catch (Exception e) {
      throw new WasmException("Failed to create engine", e);
    }
  }

  @Override
  public Module compileModule(final byte[] wasmBytes) throws CompilationException, WasmException {
    ensureNotClosed();

    // Parameter validation with defensive programming
    Objects.requireNonNull(wasmBytes, "WebAssembly bytes cannot be null");
    PanamaErrorHandler.requirePositive(wasmBytes.length, "wasmBytes.length");

    try {
      // Allocate memory segment for WASM bytes with zero-copy approach
      ArenaResourceManager.ManagedMemorySegment wasmMemory =
          resourceManager.allocate(wasmBytes.length);

      MemorySegment wasmData = wasmMemory.getSegment();
      wasmData.copyFrom(MemorySegment.ofArray(wasmBytes));

      // Compile the module through optimized FFI call
      MemorySegment modulePtr =
          compileModuleNative(engineResource.getNativePointer(), wasmData, wasmBytes.length);

      // Create managed module with proper resource tracking
      return new PanamaModule(modulePtr, resourceManager, this);

    } catch (Exception e) {
      if (e instanceof CompilationException) {
        throw e;
      }
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Module compilation", "size=" + wasmBytes.length + " bytes", e.getMessage());
      throw new CompilationException(detailedMessage, e);
    }
  }

  /**
   * Compiles WebAssembly bytecode from a ByteBuffer into a module using this engine.
   *
   * <p>This method provides an alternative interface for compilation when the WebAssembly bytecode
   * is available as a ByteBuffer. This can be more efficient for direct ByteBuffer operations.
   *
   * @param wasmBuffer the WebAssembly bytecode buffer to compile
   * @return a compiled Module
   * @throws CompilationException if compilation fails due to invalid bytecode
   * @throws WasmException if engine issues occur during compilation
   * @throws IllegalArgumentException if wasmBuffer is null or empty
   */
  public Module compileModule(final ByteBuffer wasmBuffer)
      throws CompilationException, WasmException {
    ensureNotClosed();

    // Parameter validation with defensive programming
    Objects.requireNonNull(wasmBuffer, "WebAssembly buffer cannot be null");
    if (!wasmBuffer.hasRemaining()) {
      throw new IllegalArgumentException("WebAssembly buffer cannot be empty");
    }

    try {
      final int bufferSize = wasmBuffer.remaining();

      if (wasmBuffer.isDirect()) {
        // Optimize for direct ByteBuffer with zero-copy MemorySegment access
        MemorySegment bufferSegment = MemorySegment.ofBuffer(wasmBuffer);

        // Compile directly from the buffer segment
        MemorySegment modulePtr =
            compileModuleNative(engineResource.getNativePointer(), bufferSegment, bufferSize);

        return new PanamaModule(modulePtr, resourceManager, this);

      } else {
        // For heap ByteBuffer, copy to managed memory segment
        final byte[] wasmBytes = new byte[bufferSize];
        wasmBuffer.get(wasmBytes);
        wasmBuffer.position(wasmBuffer.position() - bufferSize); // Reset position
        return compileModule(wasmBytes);
      }

    } catch (Exception e) {
      if (e instanceof CompilationException) {
        throw e;
      }
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "ByteBuffer module compilation",
              "size=" + wasmBuffer.remaining() + " bytes, isDirect=" + wasmBuffer.isDirect(),
              e.getMessage());
      throw new CompilationException(detailedMessage, e);
    }
  }

  @Override
  public Store createStore() throws WasmException {
    ensureNotClosed();
    return new PanamaStore(this);
  }

  @Override
  public Store createStore(final Object data) throws WasmException {
    ensureNotClosed();
    PanamaStore store = new PanamaStore(this);
    store.setData(data);
    return store;
  }

  /**
   * Creates a new store with custom configuration.
   *
   * <p>This method allows fine-grained control over store behavior including resource limits,
   * execution timeouts, and other advanced settings.
   *
   * @param fuelLimit the fuel limit (0 = no limit)
   * @param memoryLimitBytes the memory limit in bytes (0 = no limit)
   * @param executionTimeoutSecs the execution timeout in seconds (0 = no timeout)
   * @param maxInstances the maximum number of instances (0 = no limit)
   * @param maxTableElements the maximum table elements (0 = no limit)
   * @param maxFunctions the maximum functions (0 = no limit)
   * @return a new configured store instance
   * @throws WasmException if store creation fails
   * @throws IllegalStateException if this engine has been closed
   */
  public Store createStoreWithConfig(
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSecs,
      final int maxInstances,
      final int maxTableElements,
      final int maxFunctions)
      throws WasmException {
    ensureNotClosed();

    try {
      return new PanamaStore(
          this,
          fuelLimit,
          memoryLimitBytes,
          executionTimeoutSecs,
          maxInstances,
          maxTableElements,
          maxFunctions);
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create store with configuration", e);
    }
  }

  @Override
  public EngineConfig getConfig() {
    ensureNotClosed();

    try {
      // Create config using existing methods to get actual configuration
      final EngineConfig config = new EngineConfig();

      // Set debug info using existing method
      config.debugInfo(isDebugInfo());

      // Set optimization level using existing method
      final int optLevel = getOptimizationLevel();
      final OptimizationLevel optimizationLevel;
      switch (optLevel) {
        case 0:
          optimizationLevel = OptimizationLevel.NONE;
          break;
        case 1:
          optimizationLevel = OptimizationLevel.SPEED;
          break;
        case 2:
          optimizationLevel = OptimizationLevel.SPEED_AND_SIZE;
          break;
        default:
          // Default to SPEED for unknown values
          optimizationLevel = OptimizationLevel.SPEED;
          break;
      }
      config.optimizationLevel(optimizationLevel);

      return config;
    } catch (final Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException("Failed to retrieve engine configuration", e);
    }
  }

  /**
   * Sets the optimization level for this engine.
   *
   * <p>This method controls how aggressively the Wasmtime engine optimizes compiled WebAssembly
   * code. Higher optimization levels produce faster code but may increase compilation time.
   *
   * @param level the optimization level:
   *     <ul>
   *       <li>0 = No optimization (fastest compilation)
   *       <li>1 = Optimize for speed
   *       <li>2 = Optimize for both speed and size
   *     </ul>
   *
   * @throws WasmException if the configuration cannot be changed
   * @throws IllegalStateException if this engine has been closed
   * @throws IllegalArgumentException if level is not in range 0-2
   */
  public void setOptimizationLevel(final int level) throws WasmException {
    ensureNotClosed();

    if (level < 0 || level > 2) {
      throw new IllegalArgumentException("Optimization level must be 0-2, got: " + level);
    }

    try {
      int result =
          nativeFunctions.engineSetOptimizationLevel(engineResource.getNativePointer(), level);
      PanamaErrorHandler.safeCheckError(
          result, "Set optimization level", "Failed to set optimization level to " + level);

      LOGGER.fine("Set optimization level to " + level + " for Panama engine");
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error setting optimization level", e);
    }
  }

  /**
   * Gets the current optimization level for this engine.
   *
   * @return the optimization level (0-2)
   * @throws WasmException if the configuration cannot be retrieved
   * @throws IllegalStateException if this engine has been closed
   */
  public int getOptimizationLevel() throws WasmException {
    ensureNotClosed();

    try {
      return nativeFunctions.engineGetOptimizationLevel(engineResource.getNativePointer());
    } catch (Exception e) {
      throw new WasmException("Failed to get optimization level", e);
    }
  }

  /**
   * Enables or disables debug information generation.
   *
   * <p>When enabled, the engine will generate additional debug information during compilation which
   * can be useful for debugging WebAssembly modules but may increase compilation time and memory
   * usage.
   *
   * @param enabled true to enable debug information, false to disable
   * @throws WasmException if the configuration cannot be changed
   * @throws IllegalStateException if this engine has been closed
   */
  public void setDebugInfo(final boolean enabled) throws WasmException {
    ensureNotClosed();

    try {
      int result = nativeFunctions.engineSetDebugInfo(engineResource.getNativePointer(), enabled);
      PanamaErrorHandler.safeCheckError(
          result,
          "Set debug info",
          "Failed to " + (enabled ? "enable" : "disable") + " debug information");

      LOGGER.fine((enabled ? "Enabled" : "Disabled") + " debug info for Panama engine");
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Unexpected error setting debug info", e);
    }
  }

  /**
   * Checks if debug information generation is enabled.
   *
   * @return true if debug information is enabled, false otherwise
   * @throws WasmException if the configuration cannot be retrieved
   * @throws IllegalStateException if this engine has been closed
   */
  public boolean isDebugInfo() throws WasmException {
    ensureNotClosed();

    try {
      return nativeFunctions.engineIsDebugInfo(engineResource.getNativePointer());
    } catch (Exception e) {
      throw new WasmException("Failed to get debug info configuration", e);
    }
  }

  /**
   * Validates that this engine is still functional.
   *
   * @throws WasmException if the engine is invalid or has been corrupted
   */
  public void validate() throws WasmException {
    ensureNotClosed();

    try {
      int result = nativeFunctions.engineValidate(engineResource.getNativePointer());
      if (result != 0) {
        throw new WasmException("Engine validation failed with error code: " + result);
      }
    } catch (Exception e) {
      throw new WasmException("Failed to validate engine", e);
    }
  }

  /**
   * Checks if the engine supports a specific WebAssembly feature.
   *
   * @param feature the feature to check (0=Threads, 1=ReferenceTypes, 2=Simd, 3=BulkMemory,
   *     4=MultiValue)
   * @return true if the feature is supported
   * @throws WasmException if the check fails
   */
  public boolean supportsFeature(final int feature) throws WasmException {
    ensureNotClosed();

    try {
      int result =
          nativeFunctions.engineSupportsFeature(engineResource.getNativePointer(), feature);
      if (result < 0) {
        throw new WasmException("Feature check failed with error code: " + result);
      }
      return result == 1;
    } catch (Exception e) {
      throw new WasmException("Failed to check feature support", e);
    }
  }

  /**
   * Gets the memory limit in pages for this engine.
   *
   * @return the memory limit in pages, or 0 if no limit is set
   * @throws WasmException if the query fails
   */
  public int getMemoryLimitPages() throws WasmException {
    ensureNotClosed();

    try {
      return nativeFunctions.engineMemoryLimitPages(engineResource.getNativePointer());
    } catch (Exception e) {
      throw new WasmException("Failed to get memory limit", e);
    }
  }

  /**
   * Gets the stack size limit for this engine.
   *
   * @return the stack size limit in bytes, or 0 if no limit is set
   * @throws WasmException if the query fails
   */
  public long getStackSizeLimit() throws WasmException {
    ensureNotClosed();

    try {
      return nativeFunctions.engineStackSizeLimit(engineResource.getNativePointer());
    } catch (Exception e) {
      throw new WasmException("Failed to get stack size limit", e);
    }
  }

  /**
   * Checks if fuel consumption tracking is enabled for this engine.
   *
   * @return true if fuel tracking is enabled
   * @throws WasmException if the query fails
   */
  public boolean isFuelEnabled() throws WasmException {
    ensureNotClosed();

    try {
      int result = nativeFunctions.engineFuelEnabled(engineResource.getNativePointer());
      if (result < 0) {
        throw new WasmException("Fuel enabled check failed with error code: " + result);
      }
      return result == 1;
    } catch (Exception e) {
      throw new WasmException("Failed to check fuel enabled status", e);
    }
  }

  /**
   * Checks if epoch-based interruption is enabled for this engine.
   *
   * @return true if epoch interruption is enabled
   * @throws WasmException if the query fails
   */
  public boolean isEpochInterruptionEnabled() throws WasmException {
    ensureNotClosed();

    try {
      int result =
          nativeFunctions.engineEpochInterruptionEnabled(engineResource.getNativePointer());
      if (result < 0) {
        throw new WasmException("Epoch interruption check failed with error code: " + result);
      }
      return result == 1;
    } catch (Exception e) {
      throw new WasmException("Failed to check epoch interruption status", e);
    }
  }

  /**
   * Gets the maximum number of instances allowed for this engine.
   *
   * @return the maximum number of instances, or 0 if no limit is set
   * @throws WasmException if the query fails
   */
  public int getMaxInstances() throws WasmException {
    ensureNotClosed();

    try {
      return nativeFunctions.engineMaxInstances(engineResource.getNativePointer());
    } catch (Exception e) {
      throw new WasmException("Failed to get max instances", e);
    }
  }

  /**
   * Gets the reference count for this engine (for debugging).
   *
   * @return the reference count
   * @throws WasmException if the query fails
   */
  public long getReferenceCount() throws WasmException {
    ensureNotClosed();

    try {
      return nativeFunctions.engineReferenceCount(engineResource.getNativePointer());
    } catch (Exception e) {
      throw new WasmException("Failed to get reference count", e);
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
        engineResource.close();

        LOGGER.fine("Closed Panama engine instance");
      } catch (Exception e) {
        LOGGER.severe("Failed to close engine: " + e.getMessage());
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Gets the native engine pointer for internal use.
   *
   * @return the native engine handle
   * @throws IllegalStateException if the engine is closed
   */
  public MemorySegment getEnginePointer() {
    ensureNotClosed();
    return engineResource.getNativePointer();
  }

  /**
   * Gets the resource manager for this engine.
   *
   * @return the resource manager
   * @throws IllegalStateException if the engine is closed
   */
  public ArenaResourceManager getResourceManager() {
    ensureNotClosed();
    return resourceManager;
  }

  /**
   * Checks if the engine is closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed || engineResource.isClosed();
  }

  /**
   * Creates a new native engine through optimized FFI calls.
   *
   * @return the native engine handle
   * @throws WasmException if the engine cannot be created
   */
  private MemorySegment createNativeEngine() throws WasmException {
    return createNativeEngineWithRetry(2); // Try twice: original attempt + 1 retry
  }

  private MemorySegment createNativeEngineWithRetry(int maxAttempts) throws WasmException {
    WasmException lastException = null;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        LOGGER.fine(
            "Attempting native engine creation (attempt " + attempt + "/" + maxAttempts + ")");

        // Clear any previous error state before attempting creation
        if (attempt > 1) {
          LOGGER.fine("Clearing native error state before retry");
          nativeFunctions.clearErrorState();
        }

        MemorySegment enginePtr = nativeFunctions.engineCreate();

        if (enginePtr == null || enginePtr.equals(MemorySegment.NULL)) {
          // Get detailed error message from native library
          String nativeError = getNativeErrorMessage();
          String errorMessage =
              nativeError != null
                  ? "Native engine creation failed: " + nativeError
                  : "Native engine creation returned null pointer";

          lastException =
              new WasmException(errorMessage + " (attempt " + attempt + "/" + maxAttempts + ")");

          if (attempt < maxAttempts) {
            LOGGER.warning(
                "Engine creation attempt " + attempt + " failed, retrying: " + errorMessage);
            // Add a small delay before retry to allow system state to settle
            try {
              Thread.sleep(10); // 10ms delay
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              throw new WasmException("Engine creation interrupted during retry", ie);
            }
            continue; // Try again
          } else {
            throw lastException; // Final attempt failed
          }
        }

        LOGGER.fine(
            "Successfully created native engine with pointer: "
                + enginePtr
                + " (attempt "
                + attempt
                + ")");
        return enginePtr;

      } catch (Exception e) {
        if (e instanceof WasmException) {
          lastException = (WasmException) e;
        } else {
          String detailedMessage =
              PanamaErrorHandler.createDetailedErrorMessage(
                  "Native engine creation", "attempt " + attempt, e.getMessage());
          lastException = new WasmException(detailedMessage, e);
        }

        if (attempt < maxAttempts) {
          LOGGER.warning(
              "Engine creation attempt "
                  + attempt
                  + " failed with exception, retrying: "
                  + e.getMessage());
          continue;
        }
      }
    }

    // If we get here, all attempts failed
    throw lastException != null
        ? lastException
        : new WasmException("Engine creation failed after " + maxAttempts + " attempts");
  }

  /**
   * Gets the last error message from the native library.
   *
   * @return the error message string, or null if no error
   */
  private String getNativeErrorMessage() {
    try {
      LOGGER.fine("Attempting to retrieve native error message");
      MemorySegment errorPtr = nativeFunctions.getLastErrorMessage();
      LOGGER.fine("Native error pointer: " + errorPtr);
      if (errorPtr != null && !errorPtr.equals(MemorySegment.NULL)) {
        // Convert C string to Java string
        String errorMessage = errorPtr.getString(0);
        LOGGER.fine("Retrieved native error message: " + errorMessage);
        // Free the native error message
        nativeFunctions.freeErrorMessage(errorPtr);
        return errorMessage;
      } else {
        LOGGER.fine("No native error message available (null pointer)");
      }
    } catch (Exception e) {
      LOGGER.warning("Failed to retrieve native error message: " + e.getMessage());
    }
    return null;
  }

  /**
   * Compiles a WebAssembly module through optimized native FFI calls.
   *
   * @param enginePtr the native engine handle
   * @param wasmData the WebAssembly bytecode memory segment
   * @param length the length of the bytecode
   * @return the compiled module handle
   * @throws CompilationException if compilation fails
   * @throws WasmException if a native error occurs
   */
  private MemorySegment compileModuleNative(
      final MemorySegment enginePtr, final MemorySegment wasmData, final long length)
      throws CompilationException, WasmException {
    // Defensive parameter validation
    PanamaErrorHandler.requireValidPointer(enginePtr, "enginePtr");
    PanamaErrorHandler.requireValidPointer(wasmData, "wasmData");
    PanamaErrorHandler.requirePositive(length, "length");

    try {
      // Allocate memory for module pointer output
      ArenaResourceManager.ManagedMemorySegment moduleOutPtr =
          resourceManager.allocate(MemoryLayouts.C_POINTER);

      // Call native compilation function with type-safe parameters
      int result =
          nativeFunctions.moduleCompile(enginePtr, wasmData, length, moduleOutPtr.getSegment());

      // Check for compilation errors using comprehensive error handling
      PanamaErrorHandler.safeCheckError(
          result, "Module compilation", "WebAssembly module compilation failed");

      // Extract the compiled module pointer
      MemorySegment modulePtr =
          (MemorySegment) MemoryLayouts.C_POINTER.varHandle().get(moduleOutPtr.getSegment(), 0);

      PanamaErrorHandler.requireValidPointer(modulePtr, "compiled module pointer");

      LOGGER.fine("Successfully compiled WebAssembly module, size=" + length + " bytes");
      return modulePtr;

    } catch (Exception e) {
      if (e instanceof CompilationException) {
        throw e;
      }
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Module compilation", "engine=" + enginePtr + ", size=" + length, e.getMessage());
      throw new CompilationException(detailedMessage, e);
    }
  }

  /**
   * Internal cleanup method for native engine destruction.
   *
   * @param enginePtr the native engine handle to destroy
   */
  private void destroyNativeEngineInternal(final MemorySegment enginePtr) {
    try {
      if (enginePtr != null && !enginePtr.equals(MemorySegment.NULL)) {
        nativeFunctions.engineDestroy(enginePtr);
        LOGGER.fine("Destroyed native engine with pointer: " + enginePtr);
      }
    } catch (Exception e) {
      // Log but don't throw - this is called during cleanup
      LOGGER.warning("Failed to destroy native engine: " + e.getMessage());
    }
  }

  /**
   * Ensures that this engine instance is not closed.
   *
   * @throws IllegalStateException if the engine is closed
   */
  private void ensureNotClosed() {
    if (isClosed()) {
      throw new IllegalStateException("Engine has been closed");
    }
  }
}
