package ai.tegmentum.wasmtime4j.jni;

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
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.jni.util.JniConcurrencyManager;
import ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper;
import ai.tegmentum.wasmtime4j.jni.util.JniPhantomReferenceManager;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniResourceCache;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasmRuntime interface.
 *
 * <p>This class provides WebAssembly runtime functionality using Java Native Interface (JNI) to
 * communicate with the native Wasmtime library. It manages the lifecycle of native resources and
 * provides defensive programming to prevent JVM crashes.
 *
 * <p>This implementation is designed for Java 8+ compatibility and uses JNI calls to interact with
 * the shared wasmtime4j-native Rust library. It includes advanced features like resource caching,
 * concurrency management, and automatic cleanup via phantom references.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe concurrent access to WebAssembly resources
 *   <li>Automatic native resource cleanup and management
 *   <li>Performance optimizations through caching and batching
 *   <li>Comprehensive error handling and validation
 *   <li>Integration with public wasmtime4j API
 * </ul>
 */
public final class JniWasmRuntime extends JniResource implements WasmRuntime {

  private static final Logger LOGGER = Logger.getLogger(JniWasmRuntime.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasmRuntime: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Resource cache for engines and modules. */
  private final JniResourceCache<String, Object> resourceCache;

  /** Concurrency manager for thread-safe operations. */
  private final JniConcurrencyManager concurrencyManager;

  /** Phantom reference manager for automatic cleanup. */
  private final JniPhantomReferenceManager phantomManager;

  /**
   * Creates a new JNI WebAssembly runtime.
   *
   * @throws WasmException if the native library cannot be loaded or runtime cannot be initialized
   */
  public JniWasmRuntime() throws WasmException {
    super(initializeRuntime());

    try {
      // Initialize performance and resource management utilities
      this.resourceCache = new JniResourceCache<>(500); // Cache up to 500 resources
      this.concurrencyManager =
          new JniConcurrencyManager(20, 30000); // 20 concurrent ops, 30s timeout
      this.phantomManager = JniPhantomReferenceManager.getInstance();

      // Register this runtime for automatic cleanup
      this.phantomManager.register(this, nativeHandle, "nativeDestroyRuntime");
      this.concurrencyManager.registerResource(nativeHandle);

      LOGGER.fine(
          "Created JNI WebAssembly runtime with handle: 0x" + Long.toHexString(nativeHandle));
    } catch (final Exception e) {
      // Clean up if initialization fails
      try {
        nativeDestroyRuntime(nativeHandle);
      } catch (final Exception cleanupException) {
        LOGGER.warning(
            "Failed to cleanup runtime after initialization failure: "
                + cleanupException.getMessage());
      }
      throw new WasmException("Failed to initialize JNI runtime", e);
    }
  }

  /**
   * Initializes the native runtime and loads the native library.
   *
   * @return the native runtime handle
   * @throws WasmException if initialization fails
   */
  private static long initializeRuntime() throws WasmException {
    try {
      // Ensure native library is loaded
      NativeLibraryLoader.loadNativeLibrary();

      final long handle = nativeCreateRuntime();
      if (handle == 0) {
        throw new WasmException("Failed to create native runtime");
      }
      return handle;
    } catch (final UnsatisfiedLinkError e) {
      throw new WasmException("Native library not available", e);
    } catch (final Exception e) {
      throw new WasmException("Failed to initialize native runtime", e);
    }
  }

  @Override
  public Engine createEngine() throws WasmException {
    JniValidation.requireNonNull(this, "runtime");

    try {
      return concurrencyManager.executeWithReadLock(
          getNativeHandle(),
          () -> {
            try {
              final long engineHandle = nativeCreateEngine(nativeHandle);
              if (engineHandle == 0) {
                throw new WasmException("Failed to create engine");
              }

              final JniEngine engine = new JniEngine(engineHandle);

              // Register engine for concurrency management and cleanup
              concurrencyManager.registerResource(engineHandle);
              phantomManager.register(engine, engineHandle, "nativeDestroyEngine");

              // Cache the engine
              resourceCache.put("engine-" + engineHandle, engine);

              LOGGER.fine("Created engine with handle: 0x" + Long.toHexString(engineHandle));
              return engine;
            } catch (final WasmException e) {
              throw new RuntimeException(e);
            } catch (final Exception e) {
              throw new RuntimeException(new WasmException("Unexpected error creating engine", e));
            }
          });
    } catch (final RuntimeException e) {
      if (e.getCause() instanceof WasmException) {
        throw (WasmException) e.getCause();
      }
      throw new WasmException("Unexpected error creating engine", e);
    }
  }

  @Override
  public Engine createEngine(final EngineConfig config) throws WasmException {
    JniValidation.requireNonNull(config, "config");

    // For now, create engine with default config and log the custom config request
    LOGGER.fine(
        "Creating engine with custom config (config details will be implemented in future)");
    return createEngine();
  }

  @Override
  public Store createStore(final Engine engine) throws WasmException {
    JniValidation.requireNonNull(engine, "engine");
    if (!isValid()) {
      throw new IllegalStateException("JNI runtime is not valid or has been closed");
    }

    try {
      if (!(engine instanceof JniEngine)) {
        throw new IllegalArgumentException("Engine must be a JniEngine instance for JNI runtime");
      }

      JniEngine jniEngine = (JniEngine) engine;
      return jniEngine.createStore();
    } catch (Exception e) {
      throw JniExceptionMapper.mapException(e);
    }
  }

  @Override
  public Linker createLinker(final Engine engine) throws WasmException {
    JniValidation.requireNonNull(engine, "engine");
    if (!isValid()) {
      throw new IllegalStateException("JNI runtime is not valid or has been closed");
    }

    try {
      if (!(engine instanceof JniEngine)) {
        throw new IllegalArgumentException("Engine must be a JniEngine instance for JNI runtime");
      }

      return JniLinker.create(engine);
    } catch (Exception e) {
      throw JniExceptionMapper.mapException(e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.gc.GcRuntime createGcRuntime(final Engine engine)
      throws WasmException {
    JniValidation.requireNonNull(engine, "engine");
    if (!isValid()) {
      throw new IllegalStateException("JNI runtime is not valid or has been closed");
    }

    try {
      if (!(engine instanceof JniEngine)) {
        throw new IllegalArgumentException("Engine must be a JniEngine instance for JNI runtime");
      }

      final JniEngine jniEngine = (JniEngine) engine;
      final long engineHandle = jniEngine.getNativeHandle();

      return concurrencyManager.executeWithReadLock(
          getNativeHandle(),
          () -> {
            try {
              final JniGcRuntime gcRuntime = new JniGcRuntime(engineHandle);

              // Cache the GC runtime
              resourceCache.put("gc-runtime-" + engineHandle, gcRuntime);

              LOGGER.fine("Created GC runtime for engine: 0x" + Long.toHexString(engineHandle));
              return gcRuntime;
            } catch (final Exception e) {
              throw new RuntimeException(new WasmException("Failed to create GC runtime", e));
            }
          });
    } catch (final RuntimeException e) {
      if (e.getCause() instanceof WasmException) {
        throw (WasmException) e.getCause();
      }
      throw new WasmException("Unexpected error creating GC runtime", e);
    }
  }

  @Override
  public Module compileModule(final Engine engine, final byte[] wasmBytes) throws WasmException {
    JniValidation.requireNonNull(engine, "engine");
    JniValidation.requireNonNull(wasmBytes, "wasmBytes");

    if (wasmBytes.length == 0) {
      throw new WasmException("WebAssembly bytecode cannot be empty");
    }

    try {
      return concurrencyManager.executeWithReadLock(
          getNativeHandle(),
          () -> {
            try {
              final long moduleHandle = nativeCompileModule(nativeHandle, wasmBytes);
              if (moduleHandle == 0) {
                throw new WasmException("Failed to compile WebAssembly module");
              }

              final JniModule module = new JniModule(moduleHandle, (JniEngine) engine);

              // Register module for concurrency management and cleanup
              concurrencyManager.registerResource(moduleHandle);
              phantomManager.register(module, moduleHandle, "nativeDestroyModule");

              // Cache the module (using bytecode hash as key for potential reuse)
              final String cacheKey = "module-" + java.util.Arrays.hashCode(wasmBytes);
              resourceCache.put(cacheKey, module);

              LOGGER.fine("Compiled module with handle: 0x" + Long.toHexString(moduleHandle));
              return module;
            } catch (final WasmException e) {
              throw new RuntimeException(e);
            } catch (final Exception e) {
              throw new RuntimeException(new WasmException("Unexpected error compiling module", e));
            }
          });
    } catch (final RuntimeException e) {
      if (e.getCause() instanceof WasmException) {
        throw (WasmException) e.getCause();
      }
      throw new WasmException("Unexpected error compiling module", e);
    }
  }

  @Override
  public Instance instantiate(final Module module) throws WasmException {
    return instantiate(module, null);
  }

  @Override
  public Instance instantiate(final Module module, final ImportMap imports) throws WasmException {
    JniValidation.requireNonNull(module, "module");

    try {
      return concurrencyManager.executeWithWriteLock(
          getNativeHandle(),
          () -> {
            try {
              // For now, we'll create a basic instance without imports
              // Full import support will be added when ImportMap interface is implemented
              if (imports != null) {
                LOGGER.fine(
                    "Instantiating module with imports (import details will be implemented in"
                        + " future)");
              }

              // This is a simplified instantiation - actual implementation would use JniStore
              // and handle imports properly
              final long instanceHandle =
                  nativeInstantiateModule(nativeHandle, ((JniModule) module).getNativeHandle());
              if (instanceHandle == 0) {
                throw new WasmException("Failed to instantiate WebAssembly module");
              }

              final JniInstance instance = new JniInstance(instanceHandle, module, null);

              // Register instance for concurrency management and cleanup
              concurrencyManager.registerResource(instanceHandle);
              phantomManager.register(instance, instanceHandle, "nativeDestroyInstance");

              LOGGER.fine(
                  "Instantiated module with instance handle: 0x"
                      + Long.toHexString(instanceHandle));
              return instance;
            } catch (final WasmException e) {
              throw new RuntimeException(e);
            } catch (final Exception e) {
              throw new RuntimeException(
                  new WasmException("Unexpected error instantiating module", e));
            }
          });
    } catch (final RuntimeException e) {
      if (e.getCause() instanceof WasmException) {
        throw (WasmException) e.getCause();
      }
      throw new WasmException("Unexpected error instantiating module", e);
    }
  }

  @Override
  public RuntimeInfo getRuntimeInfo() {
    return concurrencyManager.executeWithReadLock(
        getNativeHandle(),
        () -> {
          try {
            final String version = nativeGetWasmtimeVersion();
            return new RuntimeInfo(
                "wasmtime4j-jni",
                "1.0.0-SNAPSHOT",
                version != null ? version : "unknown",
                RuntimeType.JNI,
                System.getProperty("java.version"),
                PlatformDetector.getPlatformDescription());
          } catch (final Exception e) {
            LOGGER.warning("Failed to get runtime info: " + e.getMessage());
            return new RuntimeInfo(
                "wasmtime4j-jni",
                "1.0.0-SNAPSHOT",
                "unknown",
                RuntimeType.JNI,
                System.getProperty("java.version"),
                PlatformDetector.getPlatformDescription());
          }
        });
  }

  @Override
  public boolean isValid() {
    return !isClosed() && nativeHandle != 0;
  }

  // ===== DEBUGGING OPERATIONS =====

  @Override
  public ai.tegmentum.wasmtime4j.debug.Debugger createDebugger(final Engine engine) throws WasmException {
    JniValidation.validateNotNull(engine, "engine");
    validateRuntimeState();

    try {
      return new JniDebugger(engine);
    } catch (final Exception e) {
      throw JniExceptionMapper.mapException(e, "Failed to create debugger");
    }
  }

  @Override
  public boolean isDebuggingSupported() {
    return true; // JNI implementation supports debugging
  }

  @Override
  public String getDebuggingCapabilities() {
    try {
      return "JNI debugging with breakpoints, step execution, variable inspection, " +
             "memory inspection, call stack analysis, profiling integration";
    } catch (final Exception e) {
      LOGGER.warning("Failed to get debugging capabilities: " + e.getMessage());
      return "Basic debugging support";
    }
  }

  @Override
  protected void doClose() throws Exception {
    // Close utility managers first
    if (resourceCache != null && !resourceCache.isClosed()) {
      try {
        resourceCache.close();
        LOGGER.fine("Closed resource cache");
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource cache: " + e.getMessage());
      }
    }

    if (concurrencyManager != null && !concurrencyManager.isClosed()) {
      try {
        concurrencyManager.unregisterResource(nativeHandle);
        concurrencyManager.close();
        LOGGER.fine("Closed concurrency manager");
      } catch (final Exception e) {
        LOGGER.warning("Error closing concurrency manager: " + e.getMessage());
      }
    }

    if (phantomManager != null && !phantomManager.isClosed()) {
      try {
        phantomManager.unregister(this);
      } catch (final Exception e) {
        LOGGER.warning("Error unregistering from phantom manager: " + e.getMessage());
      }
    }

    // Close native resource
    nativeDestroyRuntime(nativeHandle);
  }

  @Override
  protected String getResourceType() {
    return "WasmRuntime";
  }

  // ===== SIMD OPERATIONS =====

  @Override
  public boolean isSimdSupported() {
    validateRuntimeState();
    return concurrencyManager.executeWithReadLock(nativeHandle, 10000,
        () -> nativeIsSimdSupported(nativeHandle));
  }

  @Override
  public String getSimdCapabilities() {
    validateRuntimeState();
    return concurrencyManager.executeWithReadLock(nativeHandle, 10000,
        () -> nativeGetSimdCapabilities(nativeHandle));
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdAdd(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdAdd(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD add operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdSubtract(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdSubtract(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD subtract operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdMultiply(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdMultiply(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD multiply operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdDivide(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdDivide(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD divide operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdAddSaturated(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdAddSaturated(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD saturated add operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdAnd(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdAnd(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD AND operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdOr(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdOr(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD OR operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdXor(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdXor(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD XOR operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdNot(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdNot(nativeHandle, a.getData());
      if (result == null) {
        throw new WasmException("SIMD NOT operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdEquals(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdEquals(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD equals operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdLessThan(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdLessThan(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD less than operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdGreaterThan(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdGreaterThan(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD greater than operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdLoad(
      final ai.tegmentum.wasmtime4j.WasmMemory memory, final int offset) throws WasmException {
    JniValidation.requireNonNull(memory, "memory");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      // Assuming memory has a handle method - this may need adjustment based on actual implementation
      final long memoryHandle = ((ai.tegmentum.wasmtime4j.jni.JniMemory) memory).getNativeHandle();
      final byte[] result = nativeSimdLoad(nativeHandle, memoryHandle, offset);
      if (result == null) {
        throw new WasmException("SIMD load operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdLoadAligned(
      final ai.tegmentum.wasmtime4j.WasmMemory memory, final int offset, final int alignment) throws WasmException {
    JniValidation.requireNonNull(memory, "memory");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final long memoryHandle = ((ai.tegmentum.wasmtime4j.jni.JniMemory) memory).getNativeHandle();
      final byte[] result = nativeSimdLoadAligned(nativeHandle, memoryHandle, offset, alignment);
      if (result == null) {
        throw new WasmException("SIMD aligned load operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public void simdStore(
      final ai.tegmentum.wasmtime4j.WasmMemory memory, final int offset,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 vector) throws WasmException {
    JniValidation.requireNonNull(memory, "memory");
    JniValidation.requireNonNull(vector, "vector");
    validateRuntimeState();

    concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final long memoryHandle = ((ai.tegmentum.wasmtime4j.jni.JniMemory) memory).getNativeHandle();
      final boolean success = nativeSimdStore(nativeHandle, memoryHandle, offset, vector.getData());
      if (!success) {
        throw new WasmException("SIMD store operation failed");
      }
      return null;
    });
  }

  @Override
  public void simdStoreAligned(
      final ai.tegmentum.wasmtime4j.WasmMemory memory, final int offset,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 vector, final int alignment) throws WasmException {
    JniValidation.requireNonNull(memory, "memory");
    JniValidation.requireNonNull(vector, "vector");
    validateRuntimeState();

    concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final long memoryHandle = ((ai.tegmentum.wasmtime4j.jni.JniMemory) memory).getNativeHandle();
      final boolean success = nativeSimdStoreAligned(nativeHandle, memoryHandle, offset, vector.getData(), alignment);
      if (!success) {
        throw new WasmException("SIMD aligned store operation failed");
      }
      return null;
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdConvertI32ToF32(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 vector) throws WasmException {
    JniValidation.requireNonNull(vector, "vector");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdConvertI32ToF32(nativeHandle, vector.getData());
      if (result == null) {
        throw new WasmException("SIMD i32 to f32 conversion failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdConvertF32ToI32(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 vector) throws WasmException {
    JniValidation.requireNonNull(vector, "vector");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdConvertF32ToI32(nativeHandle, vector.getData());
      if (result == null) {
        throw new WasmException("SIMD f32 to i32 conversion failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public int simdExtractLaneI32(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 vector, final int lane) throws WasmException {
    JniValidation.requireNonNull(vector, "vector");
    validateRuntimeState();

    return concurrencyManager.executeWithReadLock(nativeHandle, 30000, () ->
        nativeSimdExtractLaneI32(nativeHandle, vector.getData(), lane));
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdReplaceLaneI32(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 vector, final int lane, final int value) throws WasmException {
    JniValidation.requireNonNull(vector, "vector");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdReplaceLaneI32(nativeHandle, vector.getData(), lane, value);
      if (result == null) {
        throw new WasmException("SIMD replace lane i32 operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdSplatI32(final int value) throws WasmException {
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdSplatI32(nativeHandle, value);
      if (result == null) {
        throw new WasmException("SIMD splat i32 operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdSplatF32(final float value) throws WasmException {
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdSplatF32(nativeHandle, value);
      if (result == null) {
        throw new WasmException("SIMD splat f32 operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdShuffle(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b,
      final byte[] indices) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    JniValidation.requireNonNull(indices, "indices");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdShuffle(nativeHandle, a.getData(), b.getData(), indices);
      if (result == null) {
        throw new WasmException("SIMD shuffle operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  // ===== ADVANCED SIMD OPERATIONS =====

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdFma(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 c) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    JniValidation.requireNonNull(c, "vector c");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdFma(nativeHandle, a.getData(), b.getData(), c.getData());
      if (result == null) {
        throw new WasmException("SIMD FMA operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdFms(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 c) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    JniValidation.requireNonNull(c, "vector c");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdFms(nativeHandle, a.getData(), b.getData(), c.getData());
      if (result == null) {
        throw new WasmException("SIMD FMS operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdReciprocal(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdReciprocal(nativeHandle, a.getData());
      if (result == null) {
        throw new WasmException("SIMD reciprocal operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdSqrt(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdSqrt(nativeHandle, a.getData());
      if (result == null) {
        throw new WasmException("SIMD sqrt operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdRsqrt(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdRsqrt(nativeHandle, a.getData());
      if (result == null) {
        throw new WasmException("SIMD rsqrt operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdPopcount(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdPopcount(nativeHandle, a.getData());
      if (result == null) {
        throw new WasmException("SIMD popcount operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdShlVariable(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdShlVariable(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD variable shift left operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdShrVariable(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdShrVariable(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD variable shift right operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public float simdHorizontalSum(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    validateRuntimeState();

    return concurrencyManager.executeWithReadLock(nativeHandle, 30000, () ->
        nativeSimdHorizontalSum(nativeHandle, a.getData()));
  }

  @Override
  public float simdHorizontalMin(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    validateRuntimeState();

    return concurrencyManager.executeWithReadLock(nativeHandle, 30000, () ->
        nativeSimdHorizontalMin(nativeHandle, a.getData()));
  }

  @Override
  public float simdHorizontalMax(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    validateRuntimeState();

    return concurrencyManager.executeWithReadLock(nativeHandle, 30000, () ->
        nativeSimdHorizontalMax(nativeHandle, a.getData()));
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdSelect(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 mask,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(mask, "mask");
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdSelect(nativeHandle, mask.getData(), a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD select operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdBlend(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b,
      final int mask) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdBlend(nativeHandle, a.getData(), b.getData(), mask);
      if (result == null) {
        throw new WasmException("SIMD blend operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  @Override
  public ai.tegmentum.wasmtime4j.SimdOperations.V128 simdRelaxedAdd(
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 a,
      final ai.tegmentum.wasmtime4j.SimdOperations.V128 b) throws WasmException {
    JniValidation.requireNonNull(a, "vector a");
    JniValidation.requireNonNull(b, "vector b");
    validateRuntimeState();

    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final byte[] result = nativeSimdRelaxedAdd(nativeHandle, a.getData(), b.getData());
      if (result == null) {
        throw new WasmException("SIMD relaxed add operation failed");
      }
      return new ai.tegmentum.wasmtime4j.SimdOperations.V128(result);
    });
  }

  // ===== WASI OPERATIONS =====

  @Override
  public ai.tegmentum.wasmtime4j.WasiContext createWasiContext() throws WasmException {
    validateRuntimeState();
    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final long wasiHandle = nativeCreateWasiContext(nativeHandle);
      if (wasiHandle == 0) {
        throw new WasmException("Failed to create WASI context");
      }
      return new ai.tegmentum.wasmtime4j.jni.JniWasiContextImpl(wasiHandle);
    });
  }

  @Override
  public <T> Linker<T> createLinker(Engine engine) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    validateRuntimeState();
    return concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final long engineHandle = ((ai.tegmentum.wasmtime4j.jni.JniEngine) engine).getNativeHandle();
      final long linkerHandle = nativeCreateLinker(nativeHandle, engineHandle);
      if (linkerHandle == 0) {
        throw new WasmException("Failed to create linker");
      }
      return new ai.tegmentum.wasmtime4j.jni.JniLinker<>(linkerHandle);
    });
  }

  @Override
  public void addWasiToLinker(Linker<ai.tegmentum.wasmtime4j.WasiContext> linker, ai.tegmentum.wasmtime4j.WasiContext context) throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    if (context == null) {
      throw new IllegalArgumentException("WasiContext cannot be null");
    }
    validateRuntimeState();

    concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final long linkerHandle = ((ai.tegmentum.wasmtime4j.jni.JniLinker<?>) linker).getNativeHandle();
      final long contextHandle = ((ai.tegmentum.wasmtime4j.jni.JniWasiContextImpl) context).getNativeHandle();

      final int result = nativeAddWasiToLinker(nativeHandle, linkerHandle, contextHandle);
      if (result != 0) {
        throw new WasmException("Failed to add WASI imports to linker");
      }
      return null;
    });
  }

  @Override
  public void addWasiPreview2ToLinker(Linker<ai.tegmentum.wasmtime4j.WasiContext> linker, ai.tegmentum.wasmtime4j.WasiContext context) throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    if (context == null) {
      throw new IllegalArgumentException("WasiContext cannot be null");
    }
    validateRuntimeState();

    concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final long linkerHandle = ((ai.tegmentum.wasmtime4j.jni.JniLinker<?>) linker).getNativeHandle();
      final long contextHandle = ((ai.tegmentum.wasmtime4j.jni.JniWasiContextImpl) context).getNativeHandle();

      final int result = nativeAddWasiPreview2ToLinker(nativeHandle, linkerHandle, contextHandle);
      if (result != 0) {
        throw new WasmException("Failed to add WASI Preview 2 imports to linker");
      }
      return null;
    });
  }

  @Override
  public void addComponentModelToLinker(Linker<ai.tegmentum.wasmtime4j.WasiContext> linker) throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    validateRuntimeState();

    concurrencyManager.executeWithWriteLock(nativeHandle, 30000, () -> {
      final long linkerHandle = ((ai.tegmentum.wasmtime4j.jni.JniLinker<?>) linker).getNativeHandle();

      final int result = nativeAddComponentModelToLinker(nativeHandle, linkerHandle);
      if (result != 0) {
        throw new WasmException("Failed to add Component Model imports to linker");
      }
      return null;
    });
  }

  @Override
  public boolean supportsComponentModel() {
    try {
      validateRuntimeState();
      return concurrencyManager.executeWithReadLock(nativeHandle, 5000, () -> {
        return nativeSupportsComponentModel(nativeHandle);
      });
    } catch (WasmException e) {
      return false;
    }
  }

  // ===== UTILITY METHODS =====

  /**
   * Validates the runtime state before performing operations.
   *
   * @throws WasmException if the runtime is in an invalid state
   */
  private void validateRuntimeState() throws WasmException {
    if (isClosed()) {
      throw new WasmException("Runtime has been closed");
    }
    if (nativeHandle == 0) {
      throw new WasmException("Runtime has invalid native handle");
    }
  }

  // Native method declarations - these will be implemented in the native library

  /**
   * Creates a new native runtime.
   *
   * @return native runtime handle or 0 on failure
   */
  private static native long nativeCreateRuntime();

  /**
   * Creates a new engine for the given runtime.
   *
   * @param runtimeHandle the native runtime handle
   * @return native engine handle or 0 on failure
   */
  private static native long nativeCreateEngine(long runtimeHandle);

  /**
   * Compiles a WebAssembly module.
   *
   * @param runtimeHandle the native runtime handle
   * @param bytecode the WebAssembly bytecode
   * @return native module handle or 0 on failure
   */
  private static native long nativeCompileModule(long runtimeHandle, byte[] bytecode);

  /**
   * Instantiates a WebAssembly module.
   *
   * @param runtimeHandle the native runtime handle
   * @param moduleHandle the native module handle
   * @return native instance handle or 0 on failure
   */
  private static native long nativeInstantiateModule(long runtimeHandle, long moduleHandle);

  /**
   * Gets the Wasmtime version string.
   *
   * @return the version string or null on error
   */
  private static native String nativeGetWasmtimeVersion();

  /**
   * Destroys a native runtime.
   *
   * @param runtimeHandle the native runtime handle
   */
  private static native void nativeDestroyRuntime(long runtimeHandle);

  // ===== SIMD NATIVE METHOD DECLARATIONS =====

  private static native boolean nativeIsSimdSupported(long runtimeHandle);
  private static native String nativeGetSimdCapabilities(long runtimeHandle);
  private static native byte[] nativeSimdAdd(long runtimeHandle, byte[] a, byte[] b);
  private static native byte[] nativeSimdSubtract(long runtimeHandle, byte[] a, byte[] b);
  private static native byte[] nativeSimdMultiply(long runtimeHandle, byte[] a, byte[] b);
  private static native byte[] nativeSimdDivide(long runtimeHandle, byte[] a, byte[] b);
  private static native byte[] nativeSimdAddSaturated(long runtimeHandle, byte[] a, byte[] b);
  private static native byte[] nativeSimdAnd(long runtimeHandle, byte[] a, byte[] b);
  private static native byte[] nativeSimdOr(long runtimeHandle, byte[] a, byte[] b);
  private static native byte[] nativeSimdXor(long runtimeHandle, byte[] a, byte[] b);
  private static native byte[] nativeSimdNot(long runtimeHandle, byte[] a);
  private static native byte[] nativeSimdEquals(long runtimeHandle, byte[] a, byte[] b);
  private static native byte[] nativeSimdLessThan(long runtimeHandle, byte[] a, byte[] b);
  private static native byte[] nativeSimdGreaterThan(long runtimeHandle, byte[] a, byte[] b);
  private static native byte[] nativeSimdLoad(long runtimeHandle, long memoryHandle, int offset);
  private static native byte[] nativeSimdLoadAligned(long runtimeHandle, long memoryHandle, int offset, int alignment);
  private static native boolean nativeSimdStore(long runtimeHandle, long memoryHandle, int offset, byte[] vector);
  private static native boolean nativeSimdStoreAligned(long runtimeHandle, long memoryHandle, int offset, byte[] vector, int alignment);
  private static native byte[] nativeSimdConvertI32ToF32(long runtimeHandle, byte[] vector);
  private static native byte[] nativeSimdConvertF32ToI32(long runtimeHandle, byte[] vector);
  private static native int nativeSimdExtractLaneI32(long runtimeHandle, byte[] vector, int lane);
  private static native byte[] nativeSimdReplaceLaneI32(long runtimeHandle, byte[] vector, int lane, int value);
  private static native byte[] nativeSimdSplatI32(long runtimeHandle, int value);
  private static native byte[] nativeSimdSplatF32(long runtimeHandle, float value);
  private static native byte[] nativeSimdShuffle(long runtimeHandle, byte[] a, byte[] b, byte[] indices);

  // ===== ADVANCED SIMD NATIVE METHOD DECLARATIONS =====

  private static native byte[] nativeSimdFma(long runtimeHandle, byte[] a, byte[] b, byte[] c);
  private static native byte[] nativeSimdFms(long runtimeHandle, byte[] a, byte[] b, byte[] c);
  private static native byte[] nativeSimdReciprocal(long runtimeHandle, byte[] a);
  private static native byte[] nativeSimdSqrt(long runtimeHandle, byte[] a);
  private static native byte[] nativeSimdRsqrt(long runtimeHandle, byte[] a);
  private static native byte[] nativeSimdPopcount(long runtimeHandle, byte[] a);
  private static native byte[] nativeSimdShlVariable(long runtimeHandle, byte[] a, byte[] b);
  private static native byte[] nativeSimdShrVariable(long runtimeHandle, byte[] a, byte[] b);
  private static native float nativeSimdHorizontalSum(long runtimeHandle, byte[] a);
  private static native float nativeSimdHorizontalMin(long runtimeHandle, byte[] a);
  private static native float nativeSimdHorizontalMax(long runtimeHandle, byte[] a);
  private static native byte[] nativeSimdSelect(long runtimeHandle, byte[] mask, byte[] a, byte[] b);
  private static native byte[] nativeSimdBlend(long runtimeHandle, byte[] a, byte[] b, int mask);
  private static native byte[] nativeSimdRelaxedAdd(long runtimeHandle, byte[] a, byte[] b);

  // ===== WASI NATIVE METHOD DECLARATIONS =====

  private static native long nativeCreateWasiContext(long runtimeHandle);

  private static native long nativeCreateLinker(long runtimeHandle, long engineHandle);

  private static native int nativeAddWasiToLinker(long runtimeHandle, long linkerHandle, long contextHandle);

  private static native int nativeAddWasiPreview2ToLinker(long runtimeHandle, long linkerHandle, long contextHandle);

  private static native int nativeAddComponentModelToLinker(long runtimeHandle, long linkerHandle);

  private static native boolean nativeSupportsComponentModel(long runtimeHandle);
}
