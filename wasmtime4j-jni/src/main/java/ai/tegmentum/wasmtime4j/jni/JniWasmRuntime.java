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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeInfo;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector;
import ai.tegmentum.wasmtime4j.type.TagType;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasmRuntime interface.
 *
 * <p>This class provides WebAssembly runtime functionality using Java Native Interface (JNI) to
 * communicate with the native Wasmtime library. It manages the lifecycle of native resources and
 * provides defensive programming to prevent JVM crashes.
 *
 * <p>This implementation is designed for Java 8+ compatibility and uses JNI calls to interact with
 * the shared wasmtime4j-native Rust library.
 *
 * <p>Key features:
 *
 * <ul>
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

  /** Cached default GC runtime for lazy initialization. */
  private volatile ai.tegmentum.wasmtime4j.gc.GcRuntime defaultGcRuntime;

  /** Engine backing the default GC runtime, must be closed when runtime is closed. */
  private volatile Engine gcRuntimeEngine;

  /** Lock object for GC runtime lazy initialization. */
  private final Object gcRuntimeLock = new Object();

  /**
   * Creates a new JNI WebAssembly runtime.
   *
   * @throws WasmException if the native library cannot be loaded or runtime cannot be initialized
   */
  public JniWasmRuntime() throws WasmException {
    super(initializeRuntime());

    LOGGER.fine("Created JNI WebAssembly runtime with handle: 0x" + Long.toHexString(nativeHandle));
  }

  /**
   * Initializes the native runtime and loads the native library.
   *
   * @return the native runtime handle
   * @throws WasmException if initialization fails
   */
  private static long initializeRuntime() throws WasmException {
    try {
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
    try {
      final long engineHandle = nativeCreateEngine(nativeHandle);
      if (engineHandle == 0) {
        throw new WasmException("Failed to create engine");
      }

      final JniEngine engine = new JniEngine(engineHandle, JniWasmRuntime.this);

      LOGGER.fine("Created engine with handle: 0x" + Long.toHexString(engineHandle));
      return engine;
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error creating engine", e);
    }
  }

  @Override
  public Engine createEngine(final EngineConfig config) throws WasmException {
    Validation.requireNonNull(config, "config");

    LOGGER.fine("Creating engine with custom config");
    return JniEngine.createWithConfig(config, this);
  }

  @Override
  public Store createStore(final Engine engine) throws WasmException {
    Validation.requireNonNull(engine, "engine");
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
  public Store tryCreateStore(final Engine engine) throws WasmException {
    Validation.requireNonNull(engine, "engine");
    if (!isValid()) {
      throw new IllegalStateException("JNI runtime is not valid or has been closed");
    }

    try {
      if (!(engine instanceof JniEngine)) {
        throw new IllegalArgumentException("Engine must be a JniEngine instance for JNI runtime");
      }

      final JniEngine jniEngine = (JniEngine) engine;
      final long storeHandle = JniStore.nativeTryCreateStore(jniEngine.getNativeHandle());
      if (storeHandle == 0) {
        throw new WasmException("Failed to allocate store (out of memory)");
      }
      return new JniStore(storeHandle, engine);
    } catch (Exception e) {
      throw JniExceptionMapper.mapException(e);
    }
  }

  @Override
  public Store createStore(
      final Engine engine, final ai.tegmentum.wasmtime4j.config.StoreLimits limits)
      throws WasmException {
    Validation.requireNonNull(engine, "engine");
    Validation.requireNonNull(limits, "limits");
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
              limits.getInstances(),
              limits.getTables(),
              limits.getMemories(),
              limits.isTrapOnGrowFailure());

      if (storeHandle == 0) {
        throw new WasmException("Failed to create store with limits");
      }

      final JniStore store = new JniStore(storeHandle, jniEngine);

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

    beginOperation();
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
    } finally {
      endOperation();
    }
  }

  @Override
  public Tag createTag(final Store store, final TagType tagType) throws WasmException {
    Validation.requireNonNull(store, "store");
    Validation.requireNonNull(tagType, "tagType");
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore instance for JNI runtime");
      }

      final JniStore jniStore = (JniStore) store;
      final long storeHandle = jniStore.getNativeHandle();
      final ai.tegmentum.wasmtime4j.type.FunctionType funcType = tagType.getFunctionType();

      // Convert function type to native format
      final ai.tegmentum.wasmtime4j.WasmValueType[] funcParamTypes = funcType.getParamTypes();
      final int[] paramTypes = new int[funcParamTypes.length];
      for (int i = 0; i < funcParamTypes.length; i++) {
        paramTypes[i] = funcParamTypes[i].toNativeTypeCode();
      }

      final ai.tegmentum.wasmtime4j.WasmValueType[] funcReturnTypes = funcType.getReturnTypes();
      final int[] returnTypes = new int[funcReturnTypes.length];
      for (int i = 0; i < funcReturnTypes.length; i++) {
        returnTypes[i] = funcReturnTypes[i].toNativeTypeCode();
      }

      final long tagHandle = nativeCreateTag(storeHandle, paramTypes, returnTypes);

      if (tagHandle == 0) {
        throw new WasmException("Failed to create tag");
      }

      final JniTag tag = new JniTag(tagHandle, storeHandle);

      LOGGER.fine("Created tag with handle: 0x" + Long.toHexString(tagHandle));
      return tag;
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Unexpected error creating tag", e);
    } finally {
      endOperation();
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
    Validation.requireNonNull(engine, "engine");
    beginOperation();
    try {
      if (!(engine instanceof JniEngine)) {
        throw new IllegalArgumentException("Engine must be a JniEngine instance for JNI runtime");
      }

      final JniEngine jniEngine = (JniEngine) engine;
      final long engineHandle = jniEngine.getNativeHandle();

      final JniGcRuntime gcRuntime = new JniGcRuntime(engineHandle);

      LOGGER.fine("Created GC runtime for engine: 0x" + Long.toHexString(engineHandle));
      return gcRuntime;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error creating GC runtime", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.gc.GcRuntime getGcRuntime() throws WasmException {
    beginOperation();
    try {

      // Double-checked locking for lazy initialization
      if (defaultGcRuntime == null) {
        synchronized (gcRuntimeLock) {
          if (defaultGcRuntime == null) {
            try {
              // Create a default engine for the GC runtime — store it so we can close it later
              final Engine engine = createEngine();
              gcRuntimeEngine = engine;
              defaultGcRuntime = createGcRuntime(engine);
              LOGGER.fine("Created default GC runtime with lazy initialization");
            } catch (final Exception e) {
              throw new WasmException("Failed to create default GC runtime", e);
            }
          }
        }
      }
      return defaultGcRuntime;
    } finally {
      endOperation();
    }
  }

  @Override
  public Module compileModuleWat(final Engine engine, final String watText) throws WasmException {
    Validation.requireNonNull(engine, "engine");
    Validation.requireNonNull(watText, "watText");

    if (watText.trim().isEmpty()) {
      throw new WasmException("WAT text cannot be empty");
    }

    if (!(engine instanceof JniEngine)) {
      throw new IllegalArgumentException("Engine must be a JniEngine instance for JNI runtime");
    }

    beginOperation();
    try {
      final JniEngine jniEngine = (JniEngine) engine;
      final Module module = jniEngine.compileWat(watText);

      LOGGER.fine(
          "Compiled WAT module with handle: 0x"
              + Long.toHexString(((JniModule) module).getNativeHandle()));

      return module;
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error compiling WAT module", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public Module compileModule(final Engine engine, final byte[] wasmBytes) throws WasmException {
    Validation.requireNonNull(engine, "engine");
    Validation.requireNonNull(wasmBytes, "wasmBytes");

    if (wasmBytes.length == 0) {
      throw new WasmException("WebAssembly bytecode cannot be empty");
    }

    // Validate engine type
    if (!(engine instanceof JniEngine)) {
      throw new IllegalArgumentException(
          "Engine must be a JniEngine instance, got: " + engine.getClass().getName());
    }

    // Delegate to the engine's compile method to ensure the module is compiled
    // with the correct engine instance and avoid cross-Engine instantiation errors
    return engine.compileModule(wasmBytes);
  }

  @Override
  public Instance instantiate(final Module module) throws WasmException {
    return instantiate(module, null);
  }

  @Override
  public Instance instantiate(final Module module, final ImportMap imports) throws WasmException {
    Validation.requireNonNull(module, "module");

    try {
      // For now, we'll create a basic instance without imports
      // Full import support will be added when ImportMap interface is implemented
      if (imports != null) {
        LOGGER.fine(
            "Instantiating module with imports (import details will be implemented in future)");
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

      LOGGER.fine(
          "Instantiated module with instance handle: 0x" + Long.toHexString(instanceHandle));
      return instance;
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error instantiating module", e);
    }
  }

  @Override
  public RuntimeInfo getRuntimeInfo() {
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
  }

  @Override
  public boolean isValid() {
    return !isClosed() && nativeHandle != 0;
  }

  @Override
  public ai.tegmentum.wasmtime4j.WasmFunction funcFromRawRef(
      final ai.tegmentum.wasmtime4j.Store store, final long raw)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore for JNI runtime");
    }

    final JniStore jniStore = (JniStore) store;
    final long funcHandle = JniFunction.nativeFuncFromRaw(jniStore.getNativeHandle(), raw);
    if (funcHandle == 0) {
      return null;
    }
    return new JniFunction(funcHandle, "<from-raw>", 0, jniStore);
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
  public Module moduleFromTrustedFile(final Engine engine, final java.nio.file.Path path)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (path == null) {
      throw new IllegalArgumentException("Path cannot be null");
    }
    if (!(engine instanceof JniEngine)) {
      throw new IllegalArgumentException("Engine must be a JniEngine for JNI runtime");
    }

    final JniEngine jniEngine = (JniEngine) engine;
    final long moduleHandle =
        JniModule.nativeFromTrustedFile(jniEngine.getNativeHandle(), path.toString());

    if (moduleHandle == 0) {
      throw new WasmException("Failed to load module from trusted file: " + path);
    }

    return new JniModule(moduleHandle, engine);
  }

  @Override
  public Module deserializeModuleRaw(final Engine engine, final byte[] bytes) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (bytes == null) {
      throw new IllegalArgumentException("Bytes cannot be null");
    }
    if (bytes.length == 0) {
      throw new WasmException("Bytes cannot be empty");
    }
    if (!(engine instanceof JniEngine)) {
      throw new IllegalArgumentException("Engine must be a JniEngine for JNI runtime");
    }

    final JniEngine jniEngine = (JniEngine) engine;
    final long moduleHandle = JniModule.nativeDeserializeRaw(jniEngine.getNativeHandle(), bytes);

    if (moduleHandle == 0) {
      throw new WasmException("Failed to deserialize module from raw bytes");
    }

    return new JniModule(moduleHandle, engine);
  }

  @Override
  public Module deserializeModuleOpenFile(final Engine engine, final int fd) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (fd < 0) {
      throw new IllegalArgumentException("File descriptor must be non-negative");
    }
    if (!(engine instanceof JniEngine)) {
      throw new IllegalArgumentException("Engine must be a JniEngine for JNI runtime");
    }

    final JniEngine jniEngine = (JniEngine) engine;
    final long moduleHandle = JniModule.nativeDeserializeOpenFile(jniEngine.getNativeHandle(), fd);

    if (moduleHandle == 0) {
      throw new WasmException("Failed to deserialize module from file descriptor: " + fd);
    }

    return new JniModule(moduleHandle, engine);
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
  public ai.tegmentum.wasmtime4j.component.ComponentEngine createComponentEngine()
      throws WasmException {
    return createComponentEngine(new ai.tegmentum.wasmtime4j.component.ComponentEngineConfig());
  }

  @Override
  public ai.tegmentum.wasmtime4j.component.ComponentEngine createComponentEngine(
      final ai.tegmentum.wasmtime4j.component.ComponentEngineConfig config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("Component engine config cannot be null");
    }

    beginOperation();
    try {
      final JniComponentEngine componentEngine = new JniComponentEngine(config);

      LOGGER.fine(
          "Created component engine with handle: 0x"
              + Long.toHexString(componentEngine.getNativeHandle()));

      return componentEngine;
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error creating component engine", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public void tlsEagerInitialize() throws WasmException {
    nativeTlsEagerInitialize();
  }

  @Override
  public boolean validateModule(final byte[] wasmBytes) {
    if (wasmBytes == null) {
      throw new IllegalArgumentException("WebAssembly bytes cannot be null");
    }
    if (wasmBytes.length == 0) {
      return false;
    }
    try {
      return JniModule.validateModuleBytes(wasmBytes);
    } catch (final Exception e) {
      return false;
    }
  }

  @Override
  protected void doClose() throws Exception {
    // Close the GC runtime engine if it was lazily created
    final Engine engine = gcRuntimeEngine;
    if (engine != null) {
      engine.close();
      gcRuntimeEngine = null;
    }
    defaultGcRuntime = null;
    nativeDestroyRuntime(nativeHandle);
  }

  @Override
  protected String getResourceType() {
    return "WasmRuntime";
  }

  // ===== WASI OPERATIONS =====

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiContext createWasiContext() throws WasmException {
    beginOperation();
    try {
      final long wasiHandle = nativeCreateWasiContext(nativeHandle);
      if (wasiHandle == 0) {
        throw new WasmException("Failed to create WASI context");
      }
      return new ai.tegmentum.wasmtime4j.jni.JniWasiContextImpl(wasiHandle);
    } finally {
      endOperation();
    }
  }

  @Override
  public <T> Linker<T> createLinker(Engine engine) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    beginOperation();
    try {
      final long engineHandle = ((ai.tegmentum.wasmtime4j.jni.JniEngine) engine).getNativeHandle();
      final long linkerHandle = nativeCreateLinker(nativeHandle, engineHandle);
      if (linkerHandle == 0) {
        throw new WasmException("Failed to create linker");
      }
      return new ai.tegmentum.wasmtime4j.jni.JniLinker<>(linkerHandle, engine);
    } finally {
      endOperation();
    }
  }

  @Override
  public <T> Linker<T> createLinker(
      final Engine engine, final boolean allowUnknownExports, final boolean allowShadowing)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }

    beginOperation();
    try {
      final long engineHandle = ((ai.tegmentum.wasmtime4j.jni.JniEngine) engine).getNativeHandle();
      final long linkerHandle =
          nativeCreateLinkerWithConfig(nativeHandle, engineHandle, allowShadowing);

      if (linkerHandle == 0) {
        throw new WasmException("Failed to create linker with configuration");
      }

      final ai.tegmentum.wasmtime4j.jni.JniLinker<T> linker =
          new ai.tegmentum.wasmtime4j.jni.JniLinker<>(linkerHandle, engine);
      linker.allowUnknownExports(allowUnknownExports);

      LOGGER.fine(
          "Created linker with config - allowUnknownExports: "
              + allowUnknownExports
              + ", allowShadowing: "
              + allowShadowing
              + ", handle: 0x"
              + Long.toHexString(linkerHandle));

      return linker;
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Unexpected error creating linker with configuration", e);
    } finally {
      endOperation();
    }
  }

  @Override
  public <T> ai.tegmentum.wasmtime4j.component.ComponentLinker<T> createComponentLinker(
      final Engine engine) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    beginOperation();
    try {
      final long engineHandle = ((ai.tegmentum.wasmtime4j.jni.JniEngine) engine).getNativeHandle();
      final long linkerHandle = nativeCreateComponentLinker(nativeHandle, engineHandle);
      if (linkerHandle == 0) {
        throw new WasmException("Failed to create component linker");
      }
      return new ai.tegmentum.wasmtime4j.jni.JniComponentLinker<>(linkerHandle, engine);
    } finally {
      endOperation();
    }
  }

  @Override
  public void addWasiToLinker(
      Linker<ai.tegmentum.wasmtime4j.wasi.WasiContext> linker,
      ai.tegmentum.wasmtime4j.wasi.WasiContext context)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    if (context == null) {
      throw new IllegalArgumentException("WasiContext cannot be null");
    }
    beginOperation();
    try {
      final JniLinker<?> jniLinker = (ai.tegmentum.wasmtime4j.jni.JniLinker<?>) linker;
      final long linkerHandle = jniLinker.getNativeHandle();
      final long contextHandle =
          ((ai.tegmentum.wasmtime4j.jni.JniWasiContextImpl) context).getNativeHandle();

      final int result = nativeAddWasiToLinker(nativeHandle, linkerHandle, contextHandle);
      if (result != 0) {
        throw new WasmException("Failed to add WASI imports to linker");
      }

      // Track WASI context on the linker for store association during instantiation
      jniLinker.setWasiContext((ai.tegmentum.wasmtime4j.jni.JniWasiContextImpl) context);

      // Track WASI imports for hasImport() checks
      for (final String[] entry : ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils.WASI_P1_IMPORTS) {
        jniLinker.addImport(entry[0], entry[1]);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void addWasiPreview2ToLinker(
      Linker<ai.tegmentum.wasmtime4j.wasi.WasiContext> linker,
      ai.tegmentum.wasmtime4j.wasi.WasiContext context)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    if (context == null) {
      throw new IllegalArgumentException("WasiContext cannot be null");
    }
    beginOperation();
    try {
      final JniLinker<?> jniLinker = (ai.tegmentum.wasmtime4j.jni.JniLinker<?>) linker;
      final long linkerHandle = jniLinker.getNativeHandle();
      final long contextHandle =
          ((ai.tegmentum.wasmtime4j.jni.JniWasiContextImpl) context).getNativeHandle();

      final int result = nativeAddWasiPreview2ToLinker(nativeHandle, linkerHandle, contextHandle);
      if (result != 0) {
        throw new WasmException("Failed to add WASI Preview 2 imports to linker");
      }

      // Track WASI context on the linker for store association during instantiation
      jniLinker.setWasiContext((ai.tegmentum.wasmtime4j.jni.JniWasiContextImpl) context);

      // Track WASI Preview 2 imports for hasImport() checks
      for (final String[] entry : ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils.WASI_P2_IMPORTS) {
        jniLinker.addImport(entry[0], entry[1]);
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public void addComponentModelToLinker(Linker<ai.tegmentum.wasmtime4j.wasi.WasiContext> linker)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    beginOperation();
    try {
      final long linkerHandle =
          ((ai.tegmentum.wasmtime4j.jni.JniLinker<?>) linker).getNativeHandle();

      final int result = nativeAddComponentModelToLinker(nativeHandle, linkerHandle);
      if (result != 0) {
        throw new WasmException("Failed to add Component Model imports to linker");
      }
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean supportsComponentModel() {
    beginOperation();
    try {
      return nativeSupportsComponentModel(nativeHandle);
    } finally {
      endOperation();
    }
  }

  // ===== WASI-NN OPERATIONS =====

  @Override
  public ai.tegmentum.wasmtime4j.wasi.nn.NnContext createNnContext()
      throws ai.tegmentum.wasmtime4j.wasi.nn.NnException {
    return new ai.tegmentum.wasmtime4j.jni.wasi.nn.JniNnContextFactory().createNnContext();
  }

  @Override
  public boolean isNnAvailable() {
    return new ai.tegmentum.wasmtime4j.jni.wasi.nn.JniNnContextFactory().isNnAvailable();
  }

  @Override
  public ExnRef createExnRef(final Store store, final Tag tag, final WasmValue[] fields)
      throws WasmException {
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore instance");
      }
      return JniExnRef.createExnRef((JniStore) store, tag, fields);
    } finally {
      endOperation();
    }
  }

  @Override
  public ExnRef exnRefFromRaw(final Store store, final long raw) throws WasmException {
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore instance");
      }
      return JniExnRef.fromRawExnRef((JniStore) store, raw);
    } finally {
      endOperation();
    }
  }

  @Override
  public long externRefToRaw(final Store store, final long externRefId) throws WasmException {
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore instance");
      }
      final long storeHandle = ((JniStore) store).getNativeHandle();
      final long result = nativeExternRefToRaw(storeHandle, externRefId);
      if (result == -1L) {
        throw new WasmException("Failed to convert ExternRef to raw");
      }
      return result;
    } finally {
      endOperation();
    }
  }

  @Override
  public long externRefFromRaw(final Store store, final long raw) throws WasmException {
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore instance");
      }
      final long storeHandle = ((JniStore) store).getNativeHandle();
      return nativeExternRefFromRaw(storeHandle, raw);
    } finally {
      endOperation();
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

  private static native void nativeTlsEagerInitialize();

  // ===== WASI NATIVE METHOD DECLARATIONS =====

  private static native long nativeCreateWasiContext(long runtimeHandle);

  private static native long nativeCreateLinker(long runtimeHandle, long engineHandle);

  private static native long nativeCreateLinkerWithConfig(
      long runtimeHandle, long engineHandle, boolean allowShadowing);

  private static native long nativeCreateComponentLinker(long runtimeHandle, long engineHandle);

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
      long engineHandle,
      long memorySize,
      long tableElements,
      long instances,
      long tables,
      long memories,
      boolean trapOnGrowFailure);

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

  // ===== TAG NATIVE METHOD DECLARATIONS =====

  /**
   * Create a new exception tag.
   *
   * @param storeHandle the store handle
   * @param paramTypes the parameter type codes
   * @param returnTypes the return type codes (typically empty for tags)
   * @return the native tag handle, or 0 on failure
   */
  private static native long nativeCreateTag(long storeHandle, int[] paramTypes, int[] returnTypes);

  // ===== EXTERNREF RAW CONVERSION NATIVE METHOD DECLARATIONS =====

  private static native long nativeExternRefToRaw(long storeHandle, long externRefData);

  private static native long nativeExternRefFromRaw(long storeHandle, long raw);
}
