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
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeInfo;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.ffi.WasmtimeBindings;
import ai.tegmentum.wasmtime4j.panama.util.PanamaExceptionMapper;
import ai.tegmentum.wasmtime4j.panama.util.PanamaMemoryManager;
import ai.tegmentum.wasmtime4j.panama.util.PanamaResourceTracker;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly runtime interface.
 *
 * <p>This implementation uses Java 23+ Panama Foreign Function API to provide high-performance,
 * type-safe access to the Wasmtime WebAssembly runtime. It leverages MemorySegment for efficient
 * native memory management and Arena for automatic resource cleanup.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Direct native function calls without JNI overhead
 *   <li>Zero-copy memory access using MemorySegment
 *   <li>Automatic resource management with Arena pattern
 *   <li>Type-safe native function signatures
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaWasmRuntime implements WasmRuntime {
  private static final Logger logger = Logger.getLogger(PanamaWasmRuntime.class.getName());

  private final Arena arena;
  private final SymbolLookup nativeLibrary;
  private final WasmtimeBindings bindings;
  private final PanamaMemoryManager memoryManager;
  private final PanamaResourceTracker resourceTracker;
  private final PanamaExceptionMapper exceptionMapper;

  private volatile boolean closed = false;

  @Override
  public boolean isValid() {
    return !closed;
  }

  /**
   * Creates a new Panama WebAssembly runtime instance.
   *
   * @throws WasmException if the native library cannot be loaded or initialized
   */
  public PanamaWasmRuntime() throws WasmException {
    logger.info("Initializing Panama WebAssembly runtime");

    Arena tempArena = null;
    try {
      tempArena = Arena.ofConfined();
      this.arena = tempArena;
      this.nativeLibrary = loadNativeLibrary();
      this.bindings = new WasmtimeBindings(nativeLibrary);
      this.memoryManager = new PanamaMemoryManager(arena);
      this.resourceTracker = new PanamaResourceTracker();
      this.exceptionMapper = new PanamaExceptionMapper();

      // Initialize the Wasmtime library
      initializeWasmtime();

      logger.info("Panama WebAssembly runtime initialized successfully");
    } catch (Exception e) {
      // Ensure cleanup if initialization fails
      try {
        if (tempArena != null) {
          tempArena.close();
        }
      } catch (Exception cleanupEx) {
        // Log but don't throw cleanup exception
        logger.warning(
            "Failed to cleanup arena during initialization failure: " + cleanupEx.getMessage());
      }
      throw new WasmException("Failed to initialize Panama WebAssembly runtime", e);
    }
  }

  @Override
  public Engine createEngine() throws WasmException {
    ensureNotClosed();

    try {
      // Create an arena-based resource manager for the engine
      Arena engineArena = Arena.ofConfined();
      ArenaResourceManager resourceManager = new ArenaResourceManager(engineArena, true);

      return new PanamaEngine(resourceManager);
    } catch (Exception e) {
      throw exceptionMapper.mapException(e);
    }
  }

  @Override
  public Engine createEngine(final EngineConfig config) throws WasmException {
    ensureNotClosed();

    if (config == null) {
      throw new IllegalArgumentException("EngineConfig cannot be null");
    }

    try {
      // Create an arena-based resource manager for the engine
      Arena engineArena = Arena.ofConfined();
      ArenaResourceManager resourceManager = new ArenaResourceManager(engineArena, true);

      // For now, ignore the config and create a default engine
      // TODO: Implement engine configuration support
      logger.warning("Engine configuration not yet supported, creating default engine");
      return new PanamaEngine(resourceManager);
    } catch (Exception e) {
      throw exceptionMapper.mapException(e);
    }
  }

  @Override
  public Store createStore(final Engine engine) throws WasmException {
    ensureNotClosed();

    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }

    try {
      if (!(engine instanceof PanamaEngine)) {
        throw new IllegalArgumentException(
            "Engine must be a PanamaEngine instance for Panama runtime");
      }

      PanamaEngine panamaEngine = (PanamaEngine) engine;
      return new PanamaStore(panamaEngine);
    } catch (Exception e) {
      throw exceptionMapper.mapException(e);
    }
  }


  @Override
  public ai.tegmentum.wasmtime4j.gc.GcRuntime createGcRuntime(final Engine engine)
      throws WasmException {
    ensureNotClosed();

    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }

    try {
      if (!(engine instanceof PanamaEngine)) {
        throw new IllegalArgumentException(
            "Engine must be a PanamaEngine instance for Panama runtime");
      }

      final PanamaEngine panamaEngine = (PanamaEngine) engine;
      final long engineHandle = panamaEngine.getNativeHandle();

      final PanamaGcRuntime gcRuntime = new PanamaGcRuntime(engineHandle);

      // Track the GC runtime for cleanup
      resourceTracker.track(gcRuntime, "PanamaGcRuntime");

      LOGGER.fine("Created GC runtime for engine: 0x" + Long.toHexString(engineHandle));
      return gcRuntime;
    } catch (final Exception e) {
      throw exceptionMapper.mapException(e);
    }
  }

  @Override
  public Module compileModule(final Engine engine, final byte[] wasmBytes) throws WasmException {
    ensureNotClosed();

    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (wasmBytes == null) {
      throw new IllegalArgumentException("WebAssembly bytes cannot be null");
    }
    if (wasmBytes.length == 0) {
      throw new IllegalArgumentException("WebAssembly bytes cannot be empty");
    }

    try {
      // Use the provided engine to compile the module
      if (!(engine instanceof PanamaEngine)) {
        throw new IllegalArgumentException(
            "Engine must be a PanamaEngine instance for Panama runtime");
      }

      PanamaEngine panamaEngine = (PanamaEngine) engine;
      return panamaEngine.compileModule(wasmBytes);
    } catch (Exception e) {
      throw exceptionMapper.mapException(e);
    }
  }

  /**
   * Compiles WebAssembly bytecode into a Module (convenience method).
   *
   * @param wasmBytes the WebAssembly bytecode
   * @return a compiled Module
   * @throws CompilationException if compilation fails
   * @throws WasmException if a runtime error occurs
   * @throws IllegalArgumentException if wasmBytes is null or empty
   */
  public Module compileModule(final byte[] wasmBytes) throws CompilationException, WasmException {
    ensureNotClosed();

    if (wasmBytes == null) {
      throw new IllegalArgumentException("WebAssembly bytes cannot be null");
    }
    if (wasmBytes.length == 0) {
      throw new IllegalArgumentException("WebAssembly bytes cannot be empty");
    }

    try {
      // Create a temporary engine for module compilation
      try (Engine engine = createEngine()) {
        return engine.compileModule(wasmBytes);
      }
    } catch (Exception e) {
      if (e instanceof CompilationException) {
        throw (CompilationException) e;
      }
      throw exceptionMapper.mapException(e);
    }
  }

  /**
   * Compiles WebAssembly bytecode from a ByteBuffer into a Module.
   *
   * @param wasmBuffer the WebAssembly bytecode buffer
   * @return a compiled Module
   * @throws CompilationException if compilation fails
   * @throws WasmException if a runtime error occurs
   * @throws IllegalArgumentException if wasmBuffer is null
   */
  public Module compileModule(final ByteBuffer wasmBuffer)
      throws CompilationException, WasmException {
    ensureNotClosed();

    if (wasmBuffer == null) {
      throw new IllegalArgumentException("WebAssembly buffer cannot be null");
    }
    if (!wasmBuffer.hasRemaining()) {
      throw new IllegalArgumentException("WebAssembly buffer cannot be empty");
    }

    try {
      // Convert ByteBuffer to byte array for now
      // TODO: Optimize to use direct MemorySegment access
      byte[] wasmBytes = new byte[wasmBuffer.remaining()];
      wasmBuffer.get(wasmBytes);
      return compileModule(wasmBytes);
    } catch (Exception e) {
      if (e instanceof CompilationException) {
        throw (CompilationException) e;
      }
      throw exceptionMapper.mapException(e);
    }
  }

  /**
   * Deserializes a module from serialized bytes.
   *
   * @param engine the engine to use for deserialization
   * @param bytes the serialized module bytes
   * @return the deserialized Module
   * @throws WasmException if deserialization fails
   */
  @Override
  public Module deserializeModule(final Engine engine, final byte[] bytes) throws WasmException {
    ensureNotClosed();

    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (bytes == null) {
      throw new IllegalArgumentException("Serialized bytes cannot be null");
    }
    if (bytes.length == 0) {
      throw new IllegalArgumentException("Serialized bytes cannot be empty");
    }

    try {
      // Get NativeFunctionBindings
      NativeFunctionBindings nativeFunctions = NativeFunctionBindings.getInstance();

      // Create a temporary serializer for this operation
      java.lang.foreign.MemorySegment serializerPtr = nativeFunctions.serializerNew();
      if (serializerPtr == null || serializerPtr.equals(java.lang.foreign.MemorySegment.NULL)) {
        throw new WasmException("Failed to create serializer");
      }

      try {
        // Get engine pointer from Panama engine
        if (!(engine instanceof PanamaEngine)) {
          throw new IllegalArgumentException("Engine must be a PanamaEngine instance");
        }
        java.lang.foreign.MemorySegment enginePtr = ((PanamaEngine) engine).getEnginePointer();

        // Allocate memory for the serialized bytes
        ArenaResourceManager resourceManager = ((PanamaEngine) engine).getResourceManager();
        ArenaResourceManager.ManagedMemorySegment serializedBytesSegment =
            resourceManager.allocate(bytes.length);
        serializedBytesSegment.getSegment().asByteBuffer().put(bytes);

        // Allocate output pointers
        ArenaResourceManager.ManagedMemorySegment resultBufferPtr =
            resourceManager.allocate(MemoryLayouts.C_POINTER);
        ArenaResourceManager.ManagedMemorySegment resultSizePtr =
            resourceManager.allocate(MemoryLayouts.C_SIZE_T);

        // Call native deserialization
        int result =
            nativeFunctions.serializerDeserialize(
                serializerPtr,
                enginePtr,
                serializedBytesSegment.getSegment(),
                bytes.length,
                resultBufferPtr.getSegment(),
                resultSizePtr.getSegment());

        if (result != 0) {
          throw new WasmException("Module deserialization failed with error code: " + result);
        }

        // Extract result
        java.lang.foreign.MemorySegment resultBuffer =
            (java.lang.foreign.MemorySegment)
                MemoryLayouts.C_POINTER.varHandle().get(resultBufferPtr.getSegment(), 0);
        long resultSize =
            (Long) MemoryLayouts.C_SIZE_T.varHandle().get(resultSizePtr.getSegment(), 0);

        if (resultBuffer == null
            || resultBuffer.equals(java.lang.foreign.MemorySegment.NULL)
            || resultSize == 0) {
          throw new WasmException("Deserialization returned empty result");
        }

        try {
          // Copy result bytes
          byte[] moduleBytes = new byte[(int) resultSize];
          resultBuffer.asByteBuffer().get(moduleBytes);

          // Compile the deserialized bytes into a module
          return compileModule(engine, moduleBytes);

        } finally {
          // Free the native buffer
          nativeFunctions.serializerFreeBuffer(resultBuffer, resultSize);
        }

      } finally {
        // Clean up the serializer
        nativeFunctions.serializerDestroy(serializerPtr);
      }

    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw exceptionMapper.mapException(e);
    }
  }

  /**
   * Deserializes a module from a file.
   *
   * @param engine the engine to use for deserialization
   * @param path the path to the serialized module file
   * @return the deserialized Module
   * @throws WasmException if deserialization fails
   */
  @Override
  public Module deserializeModuleFile(final Engine engine, final java.nio.file.Path path)
      throws WasmException {
    ensureNotClosed();

    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (path == null) {
      throw new IllegalArgumentException("Path cannot be null");
    }

    try {
      // Read the file
      byte[] bytes = java.nio.file.Files.readAllBytes(path);
      return deserializeModule(engine, bytes);

    } catch (java.io.IOException e) {
      throw new WasmException("Failed to read serialized module file: " + path, e);
    } catch (Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw exceptionMapper.mapException(e);
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    logger.info("Closing Panama WebAssembly runtime");

    synchronized (this) {
      if (closed) {
        return;
      }

      try {
        // Clean up resources in reverse order of creation
        if (resourceTracker != null) {
          resourceTracker.cleanup();
        }

        // Close the arena, which will free all associated memory
        if (arena != null) {
          arena.close();
        }

        logger.info("Panama WebAssembly runtime closed successfully");
      } catch (Exception e) {
        logger.severe("Failed to close Panama WebAssembly runtime: " + e.getMessage());
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Gets the native library symbol lookup for this runtime.
   *
   * @return the symbol lookup instance
   */
  public SymbolLookup getNativeLibrary() {
    ensureNotClosed();
    return nativeLibrary;
  }

  /**
   * Gets the Wasmtime bindings for this runtime.
   *
   * @return the bindings instance
   */
  public WasmtimeBindings getBindings() {
    ensureNotClosed();
    return bindings;
  }

  /**
   * Gets the memory manager for this runtime.
   *
   * @return the memory manager instance
   */
  public PanamaMemoryManager getMemoryManager() {
    ensureNotClosed();
    return memoryManager;
  }

  /**
   * Gets the resource tracker for this runtime.
   *
   * @return the resource tracker instance
   */
  public PanamaResourceTracker getResourceTracker() {
    ensureNotClosed();
    return resourceTracker;
  }

  /**
   * Gets the exception mapper for this runtime.
   *
   * @return the exception mapper instance
   */
  public PanamaExceptionMapper getExceptionMapper() {
    ensureNotClosed();
    return exceptionMapper;
  }

  /**
   * Loads the native Wasmtime library using Panama FFI.
   *
   * @return the symbol lookup for the loaded library
   * @throws WasmException if the library cannot be loaded
   */
  private SymbolLookup loadNativeLibrary() throws WasmException {
    try {
      // Use the dedicated Panama native library loader
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();

      if (!loader.isLoaded()) {
        String error = loader.getLoadingError().orElse("Unknown error");
        throw new WasmException("Native library loading failed: " + error);
      }

      // Use the symbol lookup from the library via SymbolLookup.loaderLookup()
      // The native library should already be loaded by the NativeLibraryLoader
      SymbolLookup lookup = SymbolLookup.loaderLookup();

      logger.info("Successfully loaded native library for Panama FFI");
      return lookup;
    } catch (Exception e) {
      throw new WasmException("Failed to load native Wasmtime library", e);
    }
  }

  /**
   * Initializes the Wasmtime library through Panama FFI calls.
   *
   * @throws WasmException if initialization fails
   */
  private void initializeWasmtime() throws WasmException {
    try {
      // Wasmtime doesn't require explicit initialization in most cases
      // The library is initialized when functions are first called
      // We can verify the library is working by attempting to get a basic function handle
      if (bindings.wasmtimeEngineNew() == null) {
        logger.warning("Failed to get wasmtime_engine_new function handle");
        throw new WasmException("Unable to access basic Wasmtime functions");
      }

      logger.info("Wasmtime library initialized successfully via Panama FFI");
    } catch (Exception e) {
      throw new WasmException("Failed to initialize Wasmtime library", e);
    }
  }

  @Override
  public Instance instantiate(final Module module) throws WasmException {
    ensureNotClosed();

    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }

    try {
      // Cast to PanamaModule to get native handle
      if (!(module instanceof PanamaModule)) {
        throw new IllegalArgumentException(
            "Module must be a PanamaModule instance for Panama runtime");
      }

      PanamaModule panamaModule = (PanamaModule) module;

      // Use the module to create an instance
      return panamaModule.instantiate();
    } catch (Exception e) {
      throw exceptionMapper.mapException(e);
    }
  }

  @Override
  public Instance instantiate(final Module module, final ImportMap imports) throws WasmException {
    ensureNotClosed();

    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    if (imports == null) {
      throw new IllegalArgumentException("ImportMap cannot be null");
    }

    try {
      // Cast to PanamaModule to get native handle
      if (!(module instanceof PanamaModule)) {
        throw new IllegalArgumentException(
            "Module must be a PanamaModule instance for Panama runtime");
      }

      PanamaModule panamaModule = (PanamaModule) module;

      // For now, ignoring imports - this would need more complex implementation
      // Use the module to create an instance
      return panamaModule.instantiate();
    } catch (Exception e) {
      throw exceptionMapper.mapException(e);
    }
  }

  // ===== WASI OPERATIONS =====

  @Override
  public ai.tegmentum.wasmtime4j.WasiContext createWasiContext() throws WasmException {
    ensureNotClosed();

    try (Arena tempArena = Arena.ofConfined()) {
      // Call native function to create WASI context
      var wasiHandle =
          WasmtimeBindings.wasi_preview2_context_new(
              memoryManager.getMainSegment(),
              true, // enable_networking
              true, // enable_filesystem
              true, // enable_process
              32, // max_async_operations
              30000, // default_timeout_ms
              true // enable_component_model
              );

      if (wasiHandle.address() == 0) {
        throw new WasmException("Failed to create WASI context");
      }

      return new PanamaWasiContextImpl(wasiHandle, memoryManager.createArena());
    } catch (Exception e) {
      throw exceptionMapper.mapException(e);
    }
  }

  @Override
  public <T> Linker<T> createLinker(Engine engine) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    ensureNotClosed();

    try (Arena tempArena = Arena.ofConfined()) {
      // Cast to PanamaEngine to get native handle
      if (!(engine instanceof PanamaEngine)) {
        throw new IllegalArgumentException(
            "Engine must be a PanamaEngine instance for Panama runtime");
      }

      PanamaEngine panamaEngine = (PanamaEngine) engine;
      var linkerHandle =
          WasmtimeBindings.wasmtime_linker_new(
              memoryManager.getMainSegment(), panamaEngine.getNativeHandle());

      if (linkerHandle.address() == 0) {
        throw new WasmException("Failed to create linker");
      }

      return new PanamaLinker<>(linkerHandle, memoryManager.createArena());
    } catch (Exception e) {
      throw exceptionMapper.mapException(e);
    }
  }

  @Override
  public void addWasiToLinker(
      Linker<ai.tegmentum.wasmtime4j.WasiContext> linker,
      ai.tegmentum.wasmtime4j.WasiContext context)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    if (context == null) {
      throw new IllegalArgumentException("WasiContext cannot be null");
    }
    ensureNotClosed();

    try (Arena tempArena = Arena.ofConfined()) {
      // Cast to Panama implementations
      if (!(linker instanceof PanamaLinker)) {
        throw new IllegalArgumentException(
            "Linker must be a PanamaLinker instance for Panama runtime");
      }
      if (!(context instanceof PanamaWasiContextImpl)) {
        throw new IllegalArgumentException(
            "WasiContext must be a PanamaWasiContextImpl instance for Panama runtime");
      }

      PanamaLinker<?> panamaLinker = (PanamaLinker<?>) linker;
      PanamaWasiContextImpl panamaContext = (PanamaWasiContextImpl) context;

      var result =
          WasmtimeBindings.wasmtime_linker_define_wasi(
              memoryManager.getMainSegment(),
              panamaLinker.getNativeHandle(),
              panamaContext.getNativeHandle());

      if (result != 0) {
        throw new WasmException("Failed to add WASI imports to linker");
      }
    } catch (Exception e) {
      throw exceptionMapper.mapException(e);
    }
  }

  @Override
  public void addWasiPreview2ToLinker(
      Linker<ai.tegmentum.wasmtime4j.WasiContext> linker,
      ai.tegmentum.wasmtime4j.WasiContext context)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    if (context == null) {
      throw new IllegalArgumentException("WasiContext cannot be null");
    }
    ensureNotClosed();

    try (Arena tempArena = Arena.ofConfined()) {
      // Cast to Panama implementations
      if (!(linker instanceof PanamaLinker)) {
        throw new IllegalArgumentException(
            "Linker must be a PanamaLinker instance for Panama runtime");
      }
      if (!(context instanceof PanamaWasiContextImpl)) {
        throw new IllegalArgumentException(
            "WasiContext must be a PanamaWasiContextImpl instance for Panama runtime");
      }

      PanamaLinker<?> panamaLinker = (PanamaLinker<?>) linker;
      PanamaWasiContextImpl panamaContext = (PanamaWasiContextImpl) context;

      var result =
          WasmtimeBindings.wasi_preview2_add_to_linker(
              memoryManager.getMainSegment(),
              panamaLinker.getNativeHandle(),
              panamaContext.getNativeHandle());

      if (result != 0) {
        throw new WasmException("Failed to add WASI Preview 2 imports to linker");
      }
    } catch (Exception e) {
      throw exceptionMapper.mapException(e);
    }
  }

  @Override
  public void addComponentModelToLinker(Linker<ai.tegmentum.wasmtime4j.WasiContext> linker)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    ensureNotClosed();

    try (Arena tempArena = Arena.ofConfined()) {
      // Cast to Panama implementation
      if (!(linker instanceof PanamaLinker)) {
        throw new IllegalArgumentException(
            "Linker must be a PanamaLinker instance for Panama runtime");
      }

      PanamaLinker<?> panamaLinker = (PanamaLinker<?>) linker;

      var result =
          WasmtimeBindings.wasmtime_linker_define_component_model(
              memoryManager.getMainSegment(), panamaLinker.getNativeHandle());

      if (result != 0) {
        throw new WasmException("Failed to add Component Model imports to linker");
      }
    } catch (Exception e) {
      throw exceptionMapper.mapException(e);
    }
  }

  @Override
  public boolean supportsComponentModel() {
    try {
      ensureNotClosed();
      // Query the native library for component model support
      return WasmtimeBindings.wasmtime_supports_component_model(memoryManager.getMainSegment());
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public RuntimeInfo getRuntimeInfo() {
    return new RuntimeInfo(
        "wasmtime4j-panama",
        "1.0.0-SNAPSHOT",
        "36.0.2", // Wasmtime version
        RuntimeType.PANAMA,
        System.getProperty("java.version"),
        System.getProperty("os.name") + " " + System.getProperty("os.arch"));
  }

  /**
   * Ensures that this runtime instance is not closed.
   *
   * @throws IllegalStateException if the runtime is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Panama WebAssembly runtime has been closed");
    }
  }
}
