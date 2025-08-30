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
   * <p>This method provides an alternative interface for compilation when the WebAssembly
   * bytecode is available as a ByteBuffer. This can be more efficient for direct ByteBuffer
   * operations.
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
    // TODO: Implement store creation
    throw new UnsupportedOperationException("Store creation not yet implemented");
  }

  @Override
  public Store createStore(final Object data) throws WasmException {
    // TODO: Implement store creation with data
    throw new UnsupportedOperationException("Store creation with data not yet implemented");
  }

  @Override
  public EngineConfig getConfig() {
    // TODO: Implement config retrieval
    throw new UnsupportedOperationException("Config retrieval not yet implemented");
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
    try {
      // Use optimized native function bindings from Stream 1
      MemorySegment enginePtr = nativeFunctions.engineCreate();

      if (enginePtr == null || enginePtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Native engine creation returned null pointer");
      }

      LOGGER.fine("Created native engine with pointer: " + enginePtr);
      return enginePtr;

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Native engine creation", "using NativeFunctionBindings", e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
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
