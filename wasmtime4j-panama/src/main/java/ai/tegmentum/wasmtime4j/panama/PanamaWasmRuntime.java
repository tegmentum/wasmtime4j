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
  public Linker createLinker(final Engine engine) throws WasmException {
    ensureNotClosed();

    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }

    try {
      if (!(engine instanceof PanamaEngine)) {
        throw new IllegalArgumentException(
            "Engine must be a PanamaEngine instance for Panama runtime");
      }

      return PanamaLinker.create(engine);
    } catch (Exception e) {
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
