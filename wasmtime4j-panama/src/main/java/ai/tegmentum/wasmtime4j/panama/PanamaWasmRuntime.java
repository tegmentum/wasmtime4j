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
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeInfo;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.config.StoreLimits;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.TagType;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Panama Foreign Function Interface implementation of the WasmRuntime interface.
 *
 * <p>This class provides WebAssembly runtime functionality using Java 23+ Panama Foreign Function
 * API to communicate with the native Wasmtime library. It manages the lifecycle of native resources
 * using Panama's Arena-based memory management.
 *
 * <p>This implementation is designed for Java 23+ compatibility and uses Panama FFI to interact
 * with the shared wasmtime4j-native Rust library.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Native memory management through Panama Arena API
 *   <li>Type-safe foreign function calls
 *   <li>Automatic resource cleanup with try-with-resources
 *   <li>Comprehensive error handling and validation
 *   <li>Integration with public wasmtime4j API
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaWasmRuntime implements WasmRuntime {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasmRuntime.class.getName());

  private final Arena arena;
  private final NativeResourceHandle resourceHandle;

  /** Cached default GC runtime for lazy initialization. */
  private volatile ai.tegmentum.wasmtime4j.gc.GcRuntime defaultGcRuntime;

  /** Lock object for GC runtime lazy initialization. */
  private final Object gcRuntimeLock = new Object();


  /**
   * Creates a new Panama WebAssembly runtime.
   *
   * @throws WasmException if the native library cannot be loaded or runtime cannot be initialized
   */
  public PanamaWasmRuntime() throws WasmException {
    try {
      this.arena = Arena.ofShared();
      this.resourceHandle =
          new NativeResourceHandle(
              "PanamaWasmRuntime",
              () -> {
                // Note: GcRuntime does not have a close() method - it relies on engine lifecycle
                defaultGcRuntime = null;

                try {
                  if (arena != null) {
                    arena.close();
                  }
                } catch (final Exception e) {
                  LOGGER.warning("Failed to close arena: " + e.getMessage());
                }

                LOGGER.fine("Closed Panama WebAssembly runtime");
              });

      LOGGER.fine("Created Panama WebAssembly runtime");
    } catch (final Exception e) {
      throw new WasmException("Failed to initialize Panama runtime", e);
    }
  }

  @Override
  public Engine createEngine() throws WasmException {
    ensureNotClosed();
    return new PanamaEngine(new EngineConfig(), this);
  }

  @Override
  public Engine createEngine(final EngineConfig config) throws WasmException {
    PanamaValidation.requireNonNull(config, "config");
    ensureNotClosed();
    return new PanamaEngine(config, this);
  }

  @Override
  public Module compileModule(final Engine engine, final byte[] wasmBytes) throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    PanamaValidation.requireNonNull(wasmBytes, "wasmBytes");
    ensureNotClosed();

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException(
          "Engine must be a PanamaEngine instance for Panama runtime");
    }

    return engine.compileModule(wasmBytes);
  }

  @Override
  public Module compileModuleWat(final Engine engine, final String watText) throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    PanamaValidation.requireNonNull(watText, "watText");

    if (watText.trim().isEmpty()) {
      throw new WasmException("WAT text cannot be empty");
    }

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException(
          "Engine must be a PanamaEngine instance for Panama runtime");
    }

    ensureNotClosed();

    return engine.compileWat(watText);
  }

  @Override
  public Store createStore(final Engine engine) throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    ensureNotClosed();

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException(
          "Engine must be a PanamaEngine instance for Panama runtime");
    }

    return engine.createStore();
  }

  @Override
  public Store createStore(
      final Engine engine,
      final long fuelLimit,
      final long memoryLimitBytes,
      final long executionTimeoutSeconds)
      throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");

    if (fuelLimit < 0) {
      throw new IllegalArgumentException("Fuel limit cannot be negative");
    }
    if (memoryLimitBytes < 0) {
      throw new IllegalArgumentException("Memory limit cannot be negative");
    }
    if (executionTimeoutSeconds < 0) {
      throw new IllegalArgumentException("Execution timeout cannot be negative");
    }

    ensureNotClosed();

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException(
          "Engine must be a PanamaEngine instance for Panama runtime");
    }

    final PanamaEngine panamaEngine = (PanamaEngine) engine;
    return new PanamaStore(panamaEngine, fuelLimit, memoryLimitBytes, executionTimeoutSeconds);
  }

  @Override
  public Store createStore(final Engine engine, final StoreLimits limits) throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    PanamaValidation.requireNonNull(limits, "limits");
    ensureNotClosed();

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException(
          "Engine must be a PanamaEngine instance for Panama runtime");
    }

    final PanamaEngine panamaEngine = (PanamaEngine) engine;
    return new PanamaStore(panamaEngine, limits);
  }

  @Override
  public Tag createTag(final Store store, final TagType tagType) throws WasmException {
    PanamaValidation.requireNonNull(store, "store");
    PanamaValidation.requireNonNull(tagType, "tagType");
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be a PanamaStore instance for Panama runtime");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final java.lang.foreign.MemorySegment storeHandle = panamaStore.getNativeStore();
    final FunctionType funcType = tagType.getFunctionType();

    // Convert function type to native format
    final WasmValueType[] funcParamTypes = funcType.getParamTypes();
    final int[] paramTypes = new int[funcParamTypes.length];
    for (int i = 0; i < funcParamTypes.length; i++) {
      paramTypes[i] = funcParamTypes[i].toNativeTypeCode();
    }

    final WasmValueType[] funcReturnTypes = funcType.getReturnTypes();
    final int[] returnTypes = new int[funcReturnTypes.length];
    for (int i = 0; i < funcReturnTypes.length; i++) {
      returnTypes[i] = funcReturnTypes[i].toNativeTypeCode();
    }

    final java.lang.foreign.MemorySegment tagPtr =
        NativeInstanceBindings.getInstance().tagCreate(storeHandle, paramTypes, returnTypes);

    if (tagPtr == null || tagPtr.equals(java.lang.foreign.MemorySegment.NULL)) {
      throw new WasmException("Failed to create tag");
    }

    return new PanamaTag(tagPtr, storeHandle);
  }

  @Override
  public <T> Linker<T> createLinker(final Engine engine) throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    ensureNotClosed();

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException(
          "Engine must be a PanamaEngine instance for Panama runtime");
    }

    return new PanamaLinker<>((PanamaEngine) engine);
  }

  @Override
  public <T> Linker<T> createLinker(
      final Engine engine, final boolean allowUnknownExports, final boolean allowShadowing)
      throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    ensureNotClosed();

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException(
          "Engine must be a PanamaEngine instance for Panama runtime");
    }

    final PanamaLinker<T> linker = new PanamaLinker<>((PanamaEngine) engine);
    linker.allowUnknownExports(allowUnknownExports);
    linker.allowShadowing(allowShadowing);
    return linker;
  }

  @Override
  public <T> ai.tegmentum.wasmtime4j.component.ComponentLinker<T> createComponentLinker(
      final Engine engine) throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    ensureNotClosed();

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException(
          "Engine must be a PanamaEngine instance for Panama runtime");
    }

    LOGGER.fine("Creating component linker for engine");
    return new PanamaComponentLinker<>((PanamaEngine) engine);
  }

  @Override
  public Instance instantiate(final Module module) throws WasmException {
    PanamaValidation.requireNonNull(module, "module");
    ensureNotClosed();

    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException(
          "Module must be a PanamaModule instance for Panama runtime");
    }

    final PanamaModule panamaModule = (PanamaModule) module;
    final Engine engine = panamaModule.getEngine();
    final Store store = createStore(engine);

    return panamaModule.instantiate(store, ImportMap.empty());
  }

  @Override
  public Instance instantiate(final Module module, final ImportMap imports) throws WasmException {
    PanamaValidation.requireNonNull(module, "module");
    PanamaValidation.requireNonNull(imports, "imports");
    ensureNotClosed();

    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException(
          "Module must be a PanamaModule instance for Panama runtime");
    }

    final PanamaModule panamaModule = (PanamaModule) module;
    final Engine engine = panamaModule.getEngine();
    final Store store = createStore(engine);

    return panamaModule.instantiate(store, imports);
  }

  @Override
  public ComponentEngine createComponentEngine() throws WasmException {
    return createComponentEngine(new ComponentEngineConfig());
  }

  @Override
  public ComponentEngine createComponentEngine(final ComponentEngineConfig config)
      throws WasmException {
    PanamaValidation.requireNonNull(config, "config");
    ensureNotClosed();

    LOGGER.fine("Creating component engine with config: " + config);
    return new PanamaComponentEngine(config);
  }

  @Override
  public RuntimeInfo getRuntimeInfo() {
    return new RuntimeInfo(
        "Panama FFI Runtime",
        RuntimeInfo.getBindingsVersion(),
        RuntimeInfo.getWasmtimeLibraryVersion(),
        RuntimeType.PANAMA,
        System.getProperty("java.version"),
        System.getProperty("os.name") + " " + System.getProperty("os.arch"));
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
  }

  @Override
  public Module deserializeModule(final Engine engine, final byte[] serializedBytes)
      throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    PanamaValidation.requireNonNull(serializedBytes, "serializedBytes");
    ensureNotClosed();

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException(
          "Engine must be a PanamaEngine instance for Panama runtime");
    }

    final PanamaEngine panamaEngine = (PanamaEngine) engine;

    try (Arena arena = Arena.ofConfined()) {
      // Allocate output parameter for module pointer
      final MemorySegment modulePtrPtr = arena.allocate(ValueLayout.ADDRESS);

      // Copy serialized bytes to native memory
      final MemorySegment dataSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, serializedBytes);

      // Call native deserialize
      final int result =
          NativeEngineBindings.getInstance()
              .moduleDeserialize(
                  panamaEngine.getNativeEngine(),
                  dataSegment,
                  serializedBytes.length,
                  modulePtrPtr);

      if (result != 0) {
        throw new WasmException("Failed to deserialize module: error code " + result);
      }

      // Get the module pointer
      final MemorySegment modulePtr = modulePtrPtr.get(ValueLayout.ADDRESS, 0);

      if (modulePtr == null || modulePtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Module deserialization returned null");
      }

      return new PanamaModule(panamaEngine, modulePtr);
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Error deserializing module: " + e.getMessage(), e);
    }
  }

  @Override
  public WasiContext createWasiContext() throws WasmException {
    ensureNotClosed();
    return new PanamaWasiContext();
  }

  @Override
  public void addWasiToLinker(final Linker<WasiContext> linker, final WasiContext context)
      throws WasmException {
    PanamaValidation.requireNonNull(linker, "linker");
    PanamaValidation.requireNonNull(context, "context");
    ensureNotClosed();

    // Cast to Panama implementation to access enableWasi()
    if (!(linker instanceof PanamaLinker)) {
      throw new IllegalArgumentException("Linker must be a PanamaLinker instance");
    }
    if (!(context instanceof PanamaWasiContext)) {
      throw new IllegalArgumentException("WasiContext must be a PanamaWasiContext instance");
    }

    @SuppressWarnings("unchecked")
    final PanamaLinker<WasiContext> panamaLinker = (PanamaLinker<WasiContext>) linker;
    final PanamaWasiContext panamaWasiContext = (PanamaWasiContext) context;

    // Enable WASI on the linker - this adds all WASI imports
    panamaLinker.enableWasi();

    // Store the WASI context on the linker so it can be attached to the store during instantiation
    panamaLinker.setWasiContext(panamaWasiContext);

    // Track WASI imports for hasImport() checks
    panamaLinker.addImport("wasi_snapshot_preview1", "fd_write");
    panamaLinker.addImport("wasi_snapshot_preview1", "proc_exit");
    panamaLinker.addImport("wasi_snapshot_preview1", "fd_read");
    panamaLinker.addImport("wasi_snapshot_preview1", "fd_close");
    panamaLinker.addImport("wasi_snapshot_preview1", "environ_get");
    panamaLinker.addImport("wasi_snapshot_preview1", "environ_sizes_get");
    panamaLinker.addImport("wasi_snapshot_preview1", "args_get");
    panamaLinker.addImport("wasi_snapshot_preview1", "args_sizes_get");

    LOGGER.fine("Added WASI imports to Panama linker");
  }

  @Override
  public void addWasiPreview2ToLinker(final Linker<WasiContext> linker, final WasiContext context)
      throws WasmException {
    PanamaValidation.requireNonNull(linker, "linker");
    PanamaValidation.requireNonNull(context, "context");
    ensureNotClosed();

    if (!(linker instanceof PanamaLinker)) {
      throw new IllegalArgumentException("Linker must be a PanamaLinker instance");
    }
    if (!(context instanceof PanamaWasiContext)) {
      throw new IllegalArgumentException("Context must be a PanamaWasiContext instance");
    }

    final PanamaLinker<?> panamaLinker = (PanamaLinker<?>) linker;
    final PanamaWasiContext panamaContext = (PanamaWasiContext) context;

    // Enable WASI Preview 1 on the linker (which supports Preview 2 module patterns)
    panamaLinker.enableWasi();

    // Set the WASI context on the linker for use during instantiation
    panamaLinker.setWasiContext(panamaContext);

    // Track WASI Preview 2 imports for hasImport() checks
    // Note: These are marker imports - full Preview 2 component model requires Component Linker
    panamaLinker.addImport("wasi:filesystem/types", "filesystem");
    panamaLinker.addImport("wasi:io/streams", "input-stream");
    panamaLinker.addImport("wasi:sockets/network", "network");

    LOGGER.fine("Added WASI Preview 2 support to linker");
  }

  @Override
  public void addComponentModelToLinker(final Linker<WasiContext> linker) throws WasmException {
    PanamaValidation.requireNonNull(linker, "linker");
    ensureNotClosed();

    if (!(linker instanceof PanamaLinker)) {
      throw new IllegalArgumentException("Linker must be a PanamaLinker instance");
    }

    // Component Model requires using Component Linker API, not the regular Linker
    // For regular modules that want component-like features, enable WASI which provides
    // the standard interface bindings
    final PanamaLinker<?> panamaLinker = (PanamaLinker<?>) linker;
    panamaLinker.enableWasi();

    LOGGER.fine("Enabled Component Model compatible imports on linker");
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiLinker createWasiLinker(final Engine engine)
      throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    ensureNotClosed();

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException("Engine must be a PanamaEngine instance");
    }

    final PanamaEngine panamaEngine = (PanamaEngine) engine;

    // Create a linker with WASI support
    @SuppressWarnings("unchecked")
    final PanamaLinker<Object> linker = new PanamaLinker<>(panamaEngine);

    return new ai.tegmentum.wasmtime4j.panama.wasi.PanamaWasiLinker(linker, panamaEngine, null);
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiLinker createWasiLinker(
      final Engine engine, final ai.tegmentum.wasmtime4j.wasi.WasiConfig config)
      throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    PanamaValidation.requireNonNull(config, "config");
    ensureNotClosed();

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException("Engine must be a PanamaEngine instance");
    }

    final PanamaEngine panamaEngine = (PanamaEngine) engine;

    // Create a linker with WASI support and config
    @SuppressWarnings("unchecked")
    final PanamaLinker<Object> linker = new PanamaLinker<>(panamaEngine);

    return new ai.tegmentum.wasmtime4j.panama.wasi.PanamaWasiLinker(linker, panamaEngine, config);
  }

  @Override
  public boolean supportsComponentModel() {
    return true;
  }

  @Override
  public ai.tegmentum.wasmtime4j.gc.GcRuntime getGcRuntime() throws WasmException {
    ensureNotClosed();

    if (defaultGcRuntime != null) {
      return defaultGcRuntime;
    }

    synchronized (gcRuntimeLock) {
      if (defaultGcRuntime != null) {
        return defaultGcRuntime;
      }

      try {
        final Engine engine = createEngine();
        final long engineHandle = ((PanamaEngine) engine).getNativeEngine().address();
        defaultGcRuntime = new PanamaGcRuntime(engineHandle);

        LOGGER.fine("Initialized default GC runtime for Panama");
        return defaultGcRuntime;
      } catch (final ai.tegmentum.wasmtime4j.panama.exception.PanamaException e) {
        throw new WasmException("Failed to create Panama GC runtime", e);
      }
    }
  }

  @Override
  public Module deserializeModuleFile(final Engine engine, final Path path) throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    PanamaValidation.requireNonNull(path, "path");
    ensureNotClosed();

    try {
      final byte[] serializedBytes = Files.readAllBytes(path);
      return deserializeModule(engine, serializedBytes);
    } catch (final IOException e) {
      throw new WasmException("Failed to read serialized module file: " + path, e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.nn.NnContext createNnContext()
      throws ai.tegmentum.wasmtime4j.wasi.nn.NnException {
    ensureNotClosed();
    final ai.tegmentum.wasmtime4j.panama.wasi.nn.PanaNnContextFactory factory =
        new ai.tegmentum.wasmtime4j.panama.wasi.nn.PanaNnContextFactory();
    return factory.createNnContext();
  }

  @Override
  public boolean isNnAvailable() {
    if (resourceHandle.isClosed()) {
      return false;
    }
    final ai.tegmentum.wasmtime4j.panama.wasi.nn.PanaNnContextFactory factory =
        new ai.tegmentum.wasmtime4j.panama.wasi.nn.PanaNnContextFactory();
    return factory.isNnAvailable();
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }
}
