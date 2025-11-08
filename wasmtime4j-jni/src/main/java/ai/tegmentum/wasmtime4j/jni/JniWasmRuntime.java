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

  /** Cached default GC runtime for lazy initialization. */
  private volatile ai.tegmentum.wasmtime4j.gc.GcRuntime defaultGcRuntime;

  /** Lock object for GC runtime lazy initialization. */
  private final Object gcRuntimeLock = new Object();

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
  public Store createStore(final Engine engine, final ai.tegmentum.wasmtime4j.StoreLimits limits)
      throws WasmException {
    JniValidation.requireNonNull(engine, "engine");
    JniValidation.requireNonNull(limits, "limits");
    if (!isValid()) {
      throw new IllegalStateException("JNI runtime is not valid or has been closed");
    }

    try {
      if (!(engine instanceof JniEngine)) {
        throw new IllegalArgumentException("Engine must be a JniEngine instance for JNI runtime");
      }

      final JniEngine jniEngine = (JniEngine) engine;
      final long engineHandle = jniEngine.getNativeHandle();

      // Call native method with StoreLimits
      final long storeHandle =
          nativeCreateStoreWithLimits(
              engineHandle,
              limits.getMemorySize(),
              limits.getTableElements(),
              limits.getInstances());

      if (storeHandle == 0) {
        throw new WasmException("Failed to create store with limits");
      }

      final JniStore store = new JniStore(storeHandle, jniEngine);

      // Register store for concurrency management and cleanup
      concurrencyManager.registerResource(storeHandle);
      phantomManager.register(store, storeHandle, "nativeDestroyStore");

      LOGGER.fine(
          "Created store with limits - memory: "
              + limits.getMemorySize()
              + " bytes, tables: "
              + limits.getTableElements()
              + ", instances: "
              + limits.getInstances()
              + ", handle: 0x"
              + Long.toHexString(storeHandle));

      return store;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Unexpected error creating store with limits", e);
    }
  }

  @Override
  public Store createStore(
      final Engine engine,
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSeconds)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (fuelLimit < 0) {
      throw new IllegalArgumentException("Fuel limit cannot be negative");
    }
    if (memoryLimitBytes < 0) {
      throw new IllegalArgumentException("Memory limit cannot be negative");
    }
    if (executionTimeoutSeconds < 0) {
      throw new IllegalArgumentException("Execution timeout cannot be negative");
    }

    validateRuntimeState();

    try {
      if (!(engine instanceof JniEngine)) {
        throw new IllegalArgumentException("Engine must be a JniEngine instance for JNI runtime");
      }

      final JniEngine jniEngine = (JniEngine) engine;
      final long engineHandle = jniEngine.getNativeHandle();

      // Call native method with resource limits
      final long storeHandle =
          nativeCreateStoreWithResourceLimits(
              engineHandle, fuelLimit, memoryLimitBytes, executionTimeoutSeconds);

      if (storeHandle == 0) {
        throw new WasmException("Failed to create store with resource limits");
      }

      final JniStore store = new JniStore(storeHandle, jniEngine);

      // Register store for concurrency management and cleanup
      concurrencyManager.registerResource(storeHandle);
      phantomManager.register(store, storeHandle, "nativeDestroyStore");

      LOGGER.fine(
          "Created store with resource limits - fuel: "
              + fuelLimit
              + ", memory: "
              + memoryLimitBytes
              + " bytes, timeout: "
              + executionTimeoutSeconds
              + "s, handle: 0x"
              + Long.toHexString(storeHandle));

      return store;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Unexpected error creating store with resource limits", e);
    }
  }

  /**
   * Creates a GC runtime for the given engine.
   *
   * @param engine the engine to create the GC runtime for
   * @return the GC runtime
   * @throws WasmException if the GC runtime cannot be created
   */
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
  public ai.tegmentum.wasmtime4j.gc.GcRuntime getGcRuntime() throws WasmException {
    if (!isValid()) {
      throw new IllegalStateException("JNI runtime is not valid or has been closed");
    }

    // Double-checked locking for lazy initialization
    if (defaultGcRuntime == null) {
      synchronized (gcRuntimeLock) {
        if (defaultGcRuntime == null) {
          try {
            // Create a default engine for the GC runtime
            final Engine engine = createEngine();
            defaultGcRuntime = createGcRuntime(engine);
            LOGGER.fine("Created default GC runtime with lazy initialization");
          } catch (final Exception e) {
            throw new WasmException("Failed to create default GC runtime", e);
          }
        }
      }
    }
    return defaultGcRuntime;
  }

  @Override
  public Module compileModuleWat(final Engine engine, final String watText) throws WasmException {
    JniValidation.requireNonNull(engine, "engine");
    JniValidation.requireNonNull(watText, "watText");

    if (watText.trim().isEmpty()) {
      throw new WasmException("WAT text cannot be empty");
    }

    if (!(engine instanceof JniEngine)) {
      throw new IllegalArgumentException("Engine must be a JniEngine instance for JNI runtime");
    }

    validateRuntimeState();

    try {
      final JniEngine jniEngine = (JniEngine) engine;
      final Module module = jniEngine.compileWat(watText);

      // Register module for resource management
      concurrencyManager.registerResource(((JniModule) module).getNativeHandle());
      phantomManager.register(
          module, ((JniModule) module).getNativeHandle(), "nativeDestroyModule");

      // Cache the module (using WAT hash as key for potential reuse)
      final String cacheKey = "wat-module-" + watText.hashCode();
      resourceCache.put(cacheKey, module);

      LOGGER.fine(
          "Compiled WAT module with handle: 0x"
              + Long.toHexString(((JniModule) module).getNativeHandle()));

      return module;
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error compiling WAT module", e);
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

              // Create a Store for the instance
              final Engine engine = module.getEngine();
              final Store store = createStore(engine);

              // Instantiate the module with the Store
              final long instanceHandle =
                  nativeInstantiateModule(nativeHandle, ((JniModule) module).getNativeHandle());
              if (instanceHandle == 0) {
                throw new WasmException("Failed to instantiate WebAssembly module");
              }

              final JniInstance instance = new JniInstance(instanceHandle, module, store);

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

  /**
   * Creates a debugger for the given engine.
   *
   * @param engine the engine to create the debugger for
   * @return the debugger
   * @throws WasmException if the debugger cannot be created
   */
  public ai.tegmentum.wasmtime4j.debug.Debugger createDebugger(final Engine engine)
      throws WasmException {
    JniValidation.requireNonNull(engine, "engine");
    validateRuntimeState();

    if (!(engine instanceof JniEngine)) {
      throw new IllegalArgumentException("Engine must be a JniEngine instance for JNI runtime");
    }

    try {
      final JniDebugger debugger = new JniDebugger(engine);

      LOGGER.fine(
          "Created debugger for engine: 0x"
              + Long.toHexString(((JniEngine) engine).getNativeHandle()));

      return debugger;
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error creating debugger", e);
    }
  }

  public boolean isDebuggingSupported() {
    return true; // JNI implementation supports debugging
  }

  @Override
  public String getDebuggingCapabilities() {
    try {
      return "JNI debugging with breakpoints, step execution, variable inspection, "
          + "memory inspection, call stack analysis, profiling integration";
    } catch (final Exception e) {
      LOGGER.warning("Failed to get debugging capabilities: " + e.getMessage());
      return "Basic debugging support";
    }
  }

  @Override
  public Module deserializeModule(final Engine engine, final byte[] serializedBytes)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (serializedBytes == null) {
      throw new IllegalArgumentException("Serialized bytes cannot be null");
    }
    if (serializedBytes.length == 0) {
      throw new WasmException("Serialized bytes cannot be empty");
    }
    if (!(engine instanceof JniEngine)) {
      throw new IllegalArgumentException("Engine must be a JniEngine for JNI runtime");
    }

    final JniEngine jniEngine = (JniEngine) engine;
    final long moduleHandle = nativeDeserializeModule(jniEngine.getNativeHandle(), serializedBytes);

    if (moduleHandle == 0) {
      throw new WasmException("Failed to deserialize module");
    }

    return new JniModule(moduleHandle, engine);
  }

  @Override
  public Module deserializeModuleFile(final Engine engine, final java.nio.file.Path path)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (path == null) {
      throw new IllegalArgumentException("Path cannot be null");
    }

    try {
      final byte[] serializedBytes = java.nio.file.Files.readAllBytes(path);
      return deserializeModule(engine, serializedBytes);
    } catch (final java.io.IOException e) {
      throw new WasmException("Failed to read module file: " + path, e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiLinker createWasiLinker(final Engine engine)
      throws WasmException {
    return createWasiLinker(engine, ai.tegmentum.wasmtime4j.wasi.WasiConfig.defaultConfig());
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiLinker createWasiLinker(
      final Engine engine, final ai.tegmentum.wasmtime4j.wasi.WasiConfig config)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }
    if (!(engine instanceof ai.tegmentum.wasmtime4j.jni.JniEngine)) {
      throw new IllegalArgumentException("Engine must be a JniEngine for JNI runtime");
    }

    final ai.tegmentum.wasmtime4j.jni.JniEngine jniEngine =
        (ai.tegmentum.wasmtime4j.jni.JniEngine) engine;

    final long linkerHandle = nativeCreateWasiLinker(jniEngine.getNativeHandle());

    if (linkerHandle == 0) {
      throw new WasmException("Failed to create WASI linker");
    }

    return new ai.tegmentum.wasmtime4j.jni.wasi.JniWasiLinker(linkerHandle, jniEngine, config);
  }

  @Override
  public ai.tegmentum.wasmtime4j.ComponentEngine createComponentEngine() throws WasmException {
    return createComponentEngine(new ai.tegmentum.wasmtime4j.ComponentEngineConfig());
  }

  @Override
  public ai.tegmentum.wasmtime4j.ComponentEngine createComponentEngine(
      final ai.tegmentum.wasmtime4j.ComponentEngineConfig config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("Component engine config cannot be null");
    }

    validateRuntimeState();

    try {
      final JniComponentEngine componentEngine = new JniComponentEngine(config);

      // Register component engine for resource management
      concurrencyManager.registerResource(componentEngine.getNativeHandle());
      phantomManager.register(
          componentEngine, componentEngine.getNativeHandle(), "nativeDestroyComponentEngine");

      LOGGER.fine(
          "Created component engine with handle: 0x"
              + Long.toHexString(componentEngine.getNativeHandle()));

      return componentEngine;
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error creating component engine", e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.Serializer createSerializer() throws WasmException {
    validateRuntimeState();
    return concurrencyManager.executeWithWriteLock(
        nativeHandle,
        () -> {
          final long serializerHandle = nativeCreateSerializer();
          if (serializerHandle == 0) {
            throw new RuntimeException("Failed to create serializer");
          }
          return new JniSerializer(serializerHandle);
        });
  }

  @Override
  public ai.tegmentum.wasmtime4j.Serializer createSerializer(
      final long maxCacheSize, final boolean enableCompression, final int compressionLevel)
      throws WasmException {
    if (compressionLevel < 0 || compressionLevel > 9) {
      throw new IllegalArgumentException("Compression level must be between 0 and 9");
    }
    validateRuntimeState();
    return concurrencyManager.executeWithWriteLock(
        nativeHandle,
        () -> {
          final long serializerHandle =
              nativeCreateSerializerWithConfig(maxCacheSize, enableCompression, compressionLevel);
          if (serializerHandle == 0) {
            throw new RuntimeException("Failed to create serializer with configuration");
          }
          return new JniSerializer(serializerHandle);
        });
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

  /**
   * Checks if SIMD operations are supported.
   *
   * @return true if SIMD is supported
   * @throws WasmException if the operation fails
   */
  public boolean isSimdSupported() throws WasmException {
    validateRuntimeState();
    return concurrencyManager.executeWithReadLock(
        nativeHandle, () -> nativeIsSimdSupported(nativeHandle));
  }

  /**
   * Gets SIMD capabilities information.
   *
   * @return SIMD capabilities as a string
   * @throws WasmException if the operation fails
   */
  public String getSimdCapabilities() throws WasmException {
    validateRuntimeState();
    return concurrencyManager.executeWithReadLock(
        nativeHandle, () -> nativeGetSimdCapabilities(nativeHandle));
  }

  // ===== WASI OPERATIONS =====

  @Override
  public ai.tegmentum.wasmtime4j.WasiContext createWasiContext() throws WasmException {
    validateRuntimeState();
    return concurrencyManager.executeWithWriteLock(
        nativeHandle,
        () -> {
          final long wasiHandle = nativeCreateWasiContext(nativeHandle);
          if (wasiHandle == 0) {
            throw new RuntimeException("Failed to create WASI context");
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
    return concurrencyManager.executeWithWriteLock(
        nativeHandle,
        () -> {
          final long engineHandle =
              ((ai.tegmentum.wasmtime4j.jni.JniEngine) engine).getNativeHandle();
          final long linkerHandle = nativeCreateLinker(nativeHandle, engineHandle);
          if (linkerHandle == 0) {
            throw new RuntimeException("Failed to create linker");
          }
          return new ai.tegmentum.wasmtime4j.jni.JniLinker<>(linkerHandle, engine);
        });
  }

  @Override
  public <T> Linker<T> createLinker(
      final Engine engine, final boolean allowUnknownExports, final boolean allowShadowing)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }

    validateRuntimeState();

    try {
      return concurrencyManager.executeWithWriteLock(
          nativeHandle,
          () -> {
            final long engineHandle =
                ((ai.tegmentum.wasmtime4j.jni.JniEngine) engine).getNativeHandle();
            final long linkerHandle =
                nativeCreateLinkerWithConfig(nativeHandle, engineHandle, allowShadowing);

            if (linkerHandle == 0) {
              throw new RuntimeException("Failed to create linker with configuration");
            }

            final ai.tegmentum.wasmtime4j.jni.JniLinker<T> linker =
                new ai.tegmentum.wasmtime4j.jni.JniLinker<>(linkerHandle, engine);

            // Register linker for resource management
            concurrencyManager.registerResource(linkerHandle);
            phantomManager.register(linker, linkerHandle, "nativeDestroyLinker");

            LOGGER.fine(
                "Created linker with config - allowUnknownExports: "
                    + allowUnknownExports
                    + ", allowShadowing: "
                    + allowShadowing
                    + ", handle: 0x"
                    + Long.toHexString(linkerHandle));

            return linker;
          });
    } catch (final RuntimeException e) {
      if (e.getCause() instanceof WasmException) {
        throw (WasmException) e.getCause();
      }
      throw new WasmException("Unexpected error creating linker with configuration", e);
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
    validateRuntimeState();

    concurrencyManager.executeWithWriteLock(
        nativeHandle,
        () -> {
          final JniLinker<?> jniLinker = (ai.tegmentum.wasmtime4j.jni.JniLinker<?>) linker;
          final long linkerHandle = jniLinker.getNativeHandle();
          final long contextHandle =
              ((ai.tegmentum.wasmtime4j.jni.JniWasiContextImpl) context).getNativeHandle();

          final int result = nativeAddWasiToLinker(nativeHandle, linkerHandle, contextHandle);
          if (result != 0) {
            throw new RuntimeException("Failed to add WASI imports to linker");
          }

          // Track WASI imports for hasImport() checks
          jniLinker.addImport("wasi_snapshot_preview1", "fd_write");
          jniLinker.addImport("wasi_snapshot_preview1", "proc_exit");
          jniLinker.addImport("wasi_snapshot_preview1", "fd_read");
          jniLinker.addImport("wasi_snapshot_preview1", "fd_close");
          jniLinker.addImport("wasi_snapshot_preview1", "environ_get");
          jniLinker.addImport("wasi_snapshot_preview1", "environ_sizes_get");
          jniLinker.addImport("wasi_snapshot_preview1", "args_get");
          jniLinker.addImport("wasi_snapshot_preview1", "args_sizes_get");

          return null;
        });
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
    validateRuntimeState();

    concurrencyManager.executeWithWriteLock(
        nativeHandle,
        () -> {
          final JniLinker<?> jniLinker = (ai.tegmentum.wasmtime4j.jni.JniLinker<?>) linker;
          final long linkerHandle = jniLinker.getNativeHandle();
          final long contextHandle =
              ((ai.tegmentum.wasmtime4j.jni.JniWasiContextImpl) context).getNativeHandle();

          final int result =
              nativeAddWasiPreview2ToLinker(nativeHandle, linkerHandle, contextHandle);
          if (result != 0) {
            throw new RuntimeException("Failed to add WASI Preview 2 imports to linker");
          }

          // Track WASI Preview 2 imports for hasImport() checks
          jniLinker.addImport("wasi:filesystem/types", "filesystem");
          jniLinker.addImport("wasi:io/streams", "input-stream");
          jniLinker.addImport("wasi:sockets/network", "network");

          return null;
        });
  }

  @Override
  public void addComponentModelToLinker(Linker<ai.tegmentum.wasmtime4j.WasiContext> linker)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    validateRuntimeState();

    concurrencyManager.executeWithWriteLock(
        nativeHandle,
        () -> {
          final long linkerHandle =
              ((ai.tegmentum.wasmtime4j.jni.JniLinker<?>) linker).getNativeHandle();

          final int result = nativeAddComponentModelToLinker(nativeHandle, linkerHandle);
          if (result != 0) {
            throw new RuntimeException("Failed to add Component Model imports to linker");
          }
          return null;
        });
  }

  @Override
  public boolean supportsComponentModel() {
    try {
      validateRuntimeState();
      return concurrencyManager.executeWithReadLock(
          nativeHandle,
          () -> {
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
  // Package-private to allow access from JniSimdOperations

  static native boolean nativeIsSimdSupported(long runtimeHandle);

  static native String nativeGetSimdCapabilities(long runtimeHandle);

  static native byte[] nativeSimdAdd(long runtimeHandle, byte[] a, byte[] b);

  static native byte[] nativeSimdSubtract(long runtimeHandle, byte[] a, byte[] b);

  static native byte[] nativeSimdMultiply(long runtimeHandle, byte[] a, byte[] b);

  static native byte[] nativeSimdDivide(long runtimeHandle, byte[] a, byte[] b);

  static native byte[] nativeSimdAddSaturated(long runtimeHandle, byte[] a, byte[] b);

  static native byte[] nativeSimdAnd(long runtimeHandle, byte[] a, byte[] b);

  static native byte[] nativeSimdOr(long runtimeHandle, byte[] a, byte[] b);

  static native byte[] nativeSimdXor(long runtimeHandle, byte[] a, byte[] b);

  static native byte[] nativeSimdNot(long runtimeHandle, byte[] a);

  static native byte[] nativeSimdEquals(long runtimeHandle, byte[] a, byte[] b);

  static native byte[] nativeSimdLessThan(long runtimeHandle, byte[] a, byte[] b);

  static native byte[] nativeSimdGreaterThan(long runtimeHandle, byte[] a, byte[] b);

  static native byte[] nativeSimdLoad(long runtimeHandle, long memoryHandle, int offset);

  static native byte[] nativeSimdLoadAligned(
      long runtimeHandle, long memoryHandle, int offset, int alignment);

  static native boolean nativeSimdStore(
      long runtimeHandle, long memoryHandle, int offset, byte[] vector);

  static native boolean nativeSimdStoreAligned(
      long runtimeHandle, long memoryHandle, int offset, byte[] vector, int alignment);

  static native byte[] nativeSimdConvertI32ToF32(long runtimeHandle, byte[] vector);

  static native byte[] nativeSimdConvertF32ToI32(long runtimeHandle, byte[] vector);

  static native int nativeSimdExtractLaneI32(long runtimeHandle, byte[] vector, int lane);

  static native byte[] nativeSimdReplaceLaneI32(
      long runtimeHandle, byte[] vector, int lane, int value);

  static native byte[] nativeSimdSplatI32(long runtimeHandle, int value);

  static native byte[] nativeSimdSplatF32(long runtimeHandle, float value);

  static native byte[] nativeSimdShuffle(long runtimeHandle, byte[] a, byte[] b, byte[] indices);

  // ===== ADVANCED SIMD NATIVE METHOD DECLARATIONS =====

  static native byte[] nativeSimdFma(long runtimeHandle, byte[] a, byte[] b, byte[] c);

  static native byte[] nativeSimdFms(long runtimeHandle, byte[] a, byte[] b, byte[] c);

  static native byte[] nativeSimdReciprocal(long runtimeHandle, byte[] a);

  static native byte[] nativeSimdSqrt(long runtimeHandle, byte[] a);

  static native byte[] nativeSimdRsqrt(long runtimeHandle, byte[] a);

  static native byte[] nativeSimdPopcount(long runtimeHandle, byte[] a);

  static native byte[] nativeSimdShlVariable(long runtimeHandle, byte[] a, byte[] b);

  static native byte[] nativeSimdShrVariable(long runtimeHandle, byte[] a, byte[] b);

  static native float nativeSimdHorizontalSum(long runtimeHandle, byte[] a);

  static native float nativeSimdHorizontalMin(long runtimeHandle, byte[] a);

  static native float nativeSimdHorizontalMax(long runtimeHandle, byte[] a);

  static native byte[] nativeSimdSelect(long runtimeHandle, byte[] mask, byte[] a, byte[] b);

  static native byte[] nativeSimdBlend(long runtimeHandle, byte[] a, byte[] b, int mask);

  static native byte[] nativeSimdRelaxedAdd(long runtimeHandle, byte[] a, byte[] b);

  // ===== WASI NATIVE METHOD DECLARATIONS =====

  private static native long nativeCreateWasiContext(long runtimeHandle);

  private static native long nativeCreateLinker(long runtimeHandle, long engineHandle);

  private static native long nativeCreateLinkerWithConfig(
      long runtimeHandle, long engineHandle, boolean allowShadowing);

  private static native int nativeAddWasiToLinker(
      long runtimeHandle, long linkerHandle, long contextHandle);

  private static native int nativeAddWasiPreview2ToLinker(
      long runtimeHandle, long linkerHandle, long contextHandle);

  private static native int nativeAddComponentModelToLinker(long runtimeHandle, long linkerHandle);

  private static native boolean nativeSupportsComponentModel(long runtimeHandle);

  /**
   * Creates a new store with resource limits.
   *
   * @param engineHandle the native engine handle
   * @param memorySize the memory size limit in bytes (0 = unlimited)
   * @param tableElements the table elements limit (0 = unlimited)
   * @param instances the instances limit (0 = unlimited)
   * @return the native store handle, or 0 on failure
   */
  private static native long nativeCreateStoreWithLimits(
      long engineHandle, long memorySize, long tableElements, long instances);

  /**
   * Deserialize a module from bytes.
   *
   * @param engineHandle the native engine handle
   * @param serializedBytes the serialized module bytes
   * @return the native module handle, or 0 on failure
   */
  private static native long nativeDeserializeModule(long engineHandle, byte[] serializedBytes);

  /**
   * Create a WASI-enabled linker.
   *
   * @param engineHandle the native engine handle
   * @return the native linker handle, or 0 on failure
   */
  private static native long nativeCreateWasiLinker(long engineHandle);

  /**
   * Creates a new store with comprehensive resource limits.
   *
   * @param engineHandle the native engine handle
   * @param fuelLimit the fuel limit (0 = no limit)
   * @param memoryLimitBytes the memory limit in bytes (0 = no limit)
   * @param executionTimeoutSeconds the execution timeout in seconds (0 = no timeout)
   * @return the native store handle, or 0 on failure
   */
  private static native long nativeCreateStoreWithResourceLimits(
      long engineHandle, long fuelLimit, long memoryLimitBytes, long executionTimeoutSeconds);

  /**
   * Create a new module serializer with default configuration.
   *
   * @return the native serializer handle, or 0 on failure
   */
  private static native long nativeCreateSerializer();

  /**
   * Create a new module serializer with custom configuration.
   *
   * @param maxCacheSize the maximum cache size in bytes (0 = unlimited)
   * @param enableCompression whether to enable compression
   * @param compressionLevel the compression level (0-9)
   * @return the native serializer handle, or 0 on failure
   */
  private static native long nativeCreateSerializerWithConfig(
      long maxCacheSize, boolean enableCompression, int compressionLevel);
}
