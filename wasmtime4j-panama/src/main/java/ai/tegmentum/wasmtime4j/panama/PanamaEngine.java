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
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.util.StreamUtils;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Engine.
 *
 * @since 1.0.0
 */
public final class PanamaEngine implements Engine {
  private static final Logger LOGGER = Logger.getLogger(PanamaEngine.class.getName());
  private static final NativeEngineBindings NATIVE_BINDINGS = NativeEngineBindings.getInstance();

  private final Arena arena;
  private final MemorySegment nativeEngine;
  private final EngineConfig config;
  private final WasmRuntime runtime;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama engine with default configuration.
   *
   * <p>This constructor is intended for unit tests. Production code should use {@link
   * #PanamaEngine(EngineConfig, WasmRuntime)}.
   *
   * @throws WasmException if engine creation fails
   */
  public PanamaEngine() throws WasmException {
    this(new EngineConfig(), null);
  }

  /**
   * Creates a new Panama engine with specified configuration.
   *
   * <p>This constructor is intended for unit tests. Production code should use {@link
   * #PanamaEngine(EngineConfig, WasmRuntime)}.
   *
   * @param config the engine configuration
   * @throws WasmException if engine creation fails
   */
  public PanamaEngine(final EngineConfig config) throws WasmException {
    this(config, null);
  }

  /**
   * Creates a new Panama engine with specified configuration and runtime reference.
   *
   * @param config the engine configuration
   * @param runtime the runtime that owns this engine
   * @throws WasmException if engine creation fails
   */
  public PanamaEngine(final EngineConfig config, final WasmRuntime runtime) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }
    this.config = config;
    this.runtime = runtime;
    this.arena = Arena.ofShared();

    // Create native engine via Panama FFI with config (with optional extension traits)
    if (config.getIncrementalCacheStore() != null
        || config.getMemoryCreator() != null
        || config.getStackCreator() != null
        || config.getCustomCodeMemory() != null) {
      this.nativeEngine = NATIVE_BINDINGS.engineCreateWithExtensions(config);
    } else {
      this.nativeEngine = NATIVE_BINDINGS.engineCreateWithConfig(config);
    }

    if (this.nativeEngine == null || this.nativeEngine.equals(MemorySegment.NULL)) {
      arena.close();
      final String nativeError = PanamaErrorMapper.retrieveNativeErrorMessage();
      if (nativeError != null && !nativeError.isEmpty()) {
        throw new WasmException("Failed to create native engine: " + nativeError);
      }
      throw new WasmException("Failed to create native engine");
    }

    final MemorySegment engineHandle = this.nativeEngine;
    final Arena engineArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaEngine",
            () -> {
              if (nativeEngine != null && !nativeEngine.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.engineDestroy(nativeEngine);
              }
              arena.close();
            },
            this,
            () -> {
              if (engineHandle != null && !engineHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.engineDestroy(engineHandle);
              }
              engineArena.close();
            });

    LOGGER.fine("Created Panama engine");
  }

  /**
   * Creates a Panama engine wrapping an existing native engine pointer.
   *
   * <p>This constructor is package-private and intended for use by {@link PanamaWeakEngine} when
   * upgrading a weak reference to a strong engine.
   *
   * @param config the engine configuration
   * @param runtime the runtime that owns this engine
   * @param existingNativeEngine an already-created native engine pointer (ownership transferred)
   */
  PanamaEngine(
      final EngineConfig config,
      final WasmRuntime runtime,
      final MemorySegment existingNativeEngine)
      throws WasmException {
    if (existingNativeEngine == null || existingNativeEngine.equals(MemorySegment.NULL)) {
      throw new WasmException("existingNativeEngine cannot be null");
    }
    this.config = config != null ? config : new EngineConfig();
    this.runtime = runtime;
    this.arena = Arena.ofShared();
    this.nativeEngine = existingNativeEngine;

    final MemorySegment engineHandle = this.nativeEngine;
    final Arena engineArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaEngine",
            () -> {
              if (nativeEngine != null && !nativeEngine.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.engineDestroy(nativeEngine);
              }
              arena.close();
            },
            this,
            () -> {
              if (engineHandle != null && !engineHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.engineDestroy(engineHandle);
              }
              engineArena.close();
            });

    LOGGER.fine("Created Panama engine from existing native pointer");
  }

  @Override
  public Store createStore() throws WasmException {
    resourceHandle.beginOperation();
    try {
      return new PanamaStore(this);
    } finally {
      resourceHandle.endOperation();
    }
  }

  /**
   * Tries to create a store using OOM-safe allocation.
   *
   * @return a new PanamaStore
   * @throws WasmException if allocation fails
   */
  Store tryCreateStore() throws WasmException {
    resourceHandle.beginOperation();
    try {
      return PanamaStore.tryCreate(this);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public Store createStore(final Object data) throws WasmException {
    final Store store = createStore();
    store.setData(data);
    return store;
  }

  @Override
  public WasmRuntime getRuntime() {
    return runtime;
  }

  @Override
  public Module compileModule(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("WASM bytes cannot be null or empty");
    }
    resourceHandle.beginOperation();
    try {
      return new PanamaModule(this, wasmBytes);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public Module compileModuleWithDwarf(final byte[] wasmBytes, final byte[] dwarfPackage)
      throws WasmException {
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("WASM bytes cannot be null or empty");
    }
    if (dwarfPackage == null || dwarfPackage.length == 0) {
      throw new IllegalArgumentException("DWARF package cannot be null or empty");
    }
    resourceHandle.beginOperation();
    try {

      try (Arena tempArena = Arena.ofConfined()) {
        final MemorySegment wasmSegment = tempArena.allocate(wasmBytes.length);
        wasmSegment.copyFrom(MemorySegment.ofArray(wasmBytes));

        final MemorySegment dwarfSegment = tempArena.allocate(dwarfPackage.length);
        dwarfSegment.copyFrom(MemorySegment.ofArray(dwarfPackage));

        final MemorySegment modulePtr = tempArena.allocate(ValueLayout.ADDRESS);

        final int result =
            NATIVE_BINDINGS.moduleCompileWithDwarf(
                nativeEngine,
                wasmSegment,
                wasmBytes.length,
                dwarfSegment,
                dwarfPackage.length,
                modulePtr);

        if (result != 0) {
          final String nativeError = PanamaErrorMapper.retrieveNativeErrorMessage();
          if (nativeError != null && !nativeError.isEmpty()) {
            throw new WasmException("Failed to compile module with DWARF: " + nativeError);
          }
          throw PanamaErrorMapper.mapNativeError(
              result, "Failed to compile module with DWARF package");
        }

        final MemorySegment nativeModule = modulePtr.get(ValueLayout.ADDRESS, 0);
        return new PanamaModule(this, nativeModule);
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public Module compileWat(final String wat) throws WasmException {
    if (wat == null) {
      throw new IllegalArgumentException("wat cannot be null");
    }
    if (wat.isEmpty()) {
      throw new IllegalArgumentException("wat cannot be empty");
    }
    resourceHandle.beginOperation();
    try {

      // Allocate C string for WAT text
      final MemorySegment watSegment = arena.allocateFrom(wat);

      // Allocate pointer for output module
      final MemorySegment modulePtr = arena.allocate(ValueLayout.ADDRESS);

      // Call native function
      final int result = NATIVE_BINDINGS.moduleCompileWat(nativeEngine, watSegment, modulePtr);

      if (result != 0) {
        final String nativeError = PanamaErrorMapper.retrieveNativeErrorMessage();
        if (nativeError != null && !nativeError.isEmpty()) {
          throw new WasmException("Failed to compile WAT: " + nativeError);
        }
        throw PanamaErrorMapper.mapNativeError(result, "Failed to compile WAT");
      }

      // Get the module pointer
      final MemorySegment nativeModulePtr = modulePtr.get(ValueLayout.ADDRESS, 0);

      if (nativeModulePtr == null || nativeModulePtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Native WAT compilation returned null module pointer");
      }

      return new PanamaModule(this, nativeModulePtr);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public byte[] precompileModule(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null) {
      throw new IllegalArgumentException("wasmBytes cannot be null");
    }
    if (wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be empty");
    }
    resourceHandle.beginOperation();
    try {

      return NATIVE_BINDINGS.enginePrecompileModule(nativeEngine, wasmBytes);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.CodeBuilder codeBuilder() throws WasmException {
    resourceHandle.beginOperation();
    try {
      return new PanamaCodeBuilder(nativeEngine, this);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public byte[] precompileComponent(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null) {
      throw new IllegalArgumentException("wasmBytes cannot be null");
    }
    if (wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be empty");
    }
    resourceHandle.beginOperation();
    try {

      return NATIVE_BINDINGS.enginePrecompileComponent(nativeEngine, wasmBytes);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.pool.PoolStatistics getPoolingAllocatorMetrics() {
    if (!resourceHandle.tryBeginOperation()) {
      return null;
    }
    try {
      final long[] metrics = NATIVE_BINDINGS.enginePoolingAllocatorMetrics(nativeEngine);
      if (metrics == null) {
        return null;
      }
      return new ai.tegmentum.wasmtime4j.pool.DefaultPoolStatistics(metrics);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public Module compileFromStream(final InputStream stream) throws WasmException, IOException {
    if (stream == null) {
      throw new IllegalArgumentException("stream cannot be null");
    }

    // Read entire stream into byte array outside the lock
    // Wasmtime requires complete bytecode before compilation
    final byte[] wasmBytes = StreamUtils.readAllBytes(stream);

    if (wasmBytes.length == 0) {
      throw new WasmException("Stream contained no data");
    }

    return compileModule(wasmBytes);
  }

  @Override
  public Module compileFromFile(final java.nio.file.Path path) throws WasmException {
    if (path == null) {
      throw new IllegalArgumentException("path cannot be null");
    }
    resourceHandle.beginOperation();
    try {

      // Allocate C string for file path
      final MemorySegment pathSegment = arena.allocateFrom(path.toString());

      // Allocate pointer for output module
      final MemorySegment modulePtr = arena.allocate(ValueLayout.ADDRESS);

      // Call native function
      final int result =
          NATIVE_BINDINGS.moduleCompileFromFile(nativeEngine, pathSegment, modulePtr);

      if (result != 0) {
        final String nativeError =
            ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper.retrieveNativeErrorMessage();
        if (nativeError != null && !nativeError.isEmpty()) {
          throw new WasmException("Failed to compile module from file: " + nativeError);
        }
        throw new WasmException("Failed to compile module from file: " + path);
      }

      // Get the module pointer
      final MemorySegment nativeModulePtr = modulePtr.get(ValueLayout.ADDRESS, 0);

      if (nativeModulePtr == null || nativeModulePtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Native file compilation returned null module pointer");
      }

      return new PanamaModule(this, nativeModulePtr);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public EngineConfig getConfig() {
    return config;
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
  }

  @Override
  public boolean supportsFeature(final WasmFeature feature) {
    if (feature == null) {
      return false;
    }
    resourceHandle.beginOperation();
    try {
      return NATIVE_BINDINGS.engineSupportsFeature(nativeEngine, feature.name());
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public java.util.Optional<Boolean> detectHostFeature(final String feature) {
    if (feature == null) {
      throw new IllegalArgumentException("feature cannot be null");
    }
    return java.util.Optional.of(NATIVE_BINDINGS.engineDetectHostFeature(feature) == 1);
  }

  @Override
  public long getStackSizeLimit() {
    resourceHandle.beginOperation();
    try {
      final long limit = NATIVE_BINDINGS.engineStackSizeLimit(nativeEngine);
      return limit == -1 ? 0 : limit;
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean isFuelEnabled() {
    resourceHandle.beginOperation();
    try {
      return NATIVE_BINDINGS.engineFuelEnabled(nativeEngine);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean isEpochInterruptionEnabled() {
    resourceHandle.beginOperation();
    try {
      return NATIVE_BINDINGS.engineEpochInterruptionEnabled(nativeEngine);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean isCoredumpOnTrapEnabled() {
    resourceHandle.beginOperation();
    try {
      return NATIVE_BINDINGS.engineCoredumpOnTrapEnabled(nativeEngine);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void incrementEpoch() {
    resourceHandle.beginOperation();
    try {
      NATIVE_BINDINGS.engineIncrementEpoch(nativeEngine);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void unloadProcessHandlers() throws ai.tegmentum.wasmtime4j.exception.WasmException {
    resourceHandle.beginOperation();
    try {
      int result = NATIVE_BINDINGS.engineUnloadProcessHandlers(nativeEngine);
      if (result != 0) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Failed to unload process handlers: other references to this engine may still exist");
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  /**
   * Gets the native engine pointer.
   *
   * @return native engine memory segment
   * @throws IllegalStateException if the engine has been closed
   */
  public MemorySegment getNativeEngine() {
    resourceHandle.beginOperation();
    try {
      return nativeEngine;
    } finally {
      resourceHandle.endOperation();
    }
  }

  /**
   * Gets the unique identifier for this engine.
   *
   * @return the engine ID
   */
  public long getId() {
    return System.identityHashCode(this);
  }

  @Override
  public boolean isPulley() {
    if (!resourceHandle.tryBeginOperation()) {
      return false;
    }
    try {
      try {
        return NATIVE_BINDINGS.engineIsPulley(nativeEngine);
      } catch (final Exception e) {
        return false;
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public byte[] precompileCompatibilityHash() {
    if (!resourceHandle.tryBeginOperation()) {
      return new byte[0];
    }
    try {
      try {
        final byte[] hash = NATIVE_BINDINGS.enginePrecompileCompatibilityHash(nativeEngine);
        return hash != null ? hash : new byte[0];
      } catch (final Exception e) {
        return new byte[0];
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.Precompiled detectPrecompiled(final byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    if (bytes.length == 0) {
      return null;
    }
    resourceHandle.beginOperation();
    try {

      try (final java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
        final java.lang.foreign.MemorySegment bytesSegment = arena.allocate(bytes.length);
        bytesSegment.copyFrom(java.lang.foreign.MemorySegment.ofArray(bytes));
        final int result =
            NATIVE_BINDINGS.engineDetectPrecompiled(nativeEngine, bytesSegment, bytes.length);
        // -1 means not precompiled, 0 = MODULE, 1 = COMPONENT
        if (result < 0) {
          return null;
        }
        return ai.tegmentum.wasmtime4j.Precompiled.fromValue(result);
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean same(final ai.tegmentum.wasmtime4j.Engine other) {
    if (other == null) {
      throw new IllegalArgumentException("other cannot be null");
    }
    if (!resourceHandle.tryBeginOperation()) {
      return false;
    }
    try {
      if (!(other instanceof PanamaEngine)) {
        return false;
      }
      final PanamaEngine otherEngine = (PanamaEngine) other;
      if (otherEngine.nativeEngine == null
          || otherEngine.nativeEngine.equals(java.lang.foreign.MemorySegment.NULL)) {
        return false;
      }
      return NATIVE_BINDINGS.engineSame(this.nativeEngine, otherEngine.nativeEngine);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean isAsync() {
    if (!resourceHandle.tryBeginOperation()) {
      return false;
    }
    try {
      return NATIVE_BINDINGS.engineIsAsync(nativeEngine);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean isRecording() {
    if (!resourceHandle.tryBeginOperation()) {
      return false;
    }
    try {
      return NATIVE_BINDINGS.engineIsRecording(nativeEngine);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean isReplaying() {
    if (!resourceHandle.tryBeginOperation()) {
      return false;
    }
    try {
      return NATIVE_BINDINGS.engineIsReplaying(nativeEngine);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public WasmMemory createSharedMemory(final int initialPages, final int maxPages)
      throws WasmException {
    if (initialPages < 0) {
      throw new IllegalArgumentException("Initial pages cannot be negative: " + initialPages);
    }
    if (maxPages < 1) {
      throw new IllegalArgumentException("Shared memory requires a positive maximum page count");
    }
    if (maxPages < initialPages) {
      throw new IllegalArgumentException(
          "Max pages (" + maxPages + ") cannot be less than initial pages (" + initialPages + ")");
    }
    resourceHandle.beginOperation();
    try {

      final MemorySegment memoryPtr =
          NATIVE_BINDINGS.engineCreateSharedMemory(nativeEngine, initialPages, maxPages);
      if (memoryPtr == null || memoryPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to create shared memory");
      }
      return new PanamaMemory(memoryPtr);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.debug.GuestProfiler createGuestProfiler(
      final String moduleName,
      final java.time.Duration interval,
      final java.util.Map<String, ai.tegmentum.wasmtime4j.Module> modules)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (moduleName == null || moduleName.isEmpty()) {
      throw new IllegalArgumentException("moduleName cannot be null or empty");
    }
    if (interval == null) {
      throw new IllegalArgumentException("interval cannot be null");
    }
    if (modules == null || modules.isEmpty()) {
      throw new IllegalArgumentException("modules cannot be null or empty");
    }
    resourceHandle.beginOperation();
    try {

      return new PanamaGuestProfiler(nativeEngine, moduleName, interval.toNanos(), modules);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.debug.GuestProfiler createComponentGuestProfiler(
      final String componentName,
      final java.time.Duration interval,
      final ai.tegmentum.wasmtime4j.component.Component component,
      final java.util.Map<String, ai.tegmentum.wasmtime4j.Module> extraModules)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (componentName == null || componentName.isEmpty()) {
      throw new IllegalArgumentException("componentName cannot be null or empty");
    }
    if (interval == null) {
      throw new IllegalArgumentException("interval cannot be null");
    }
    if (component == null) {
      throw new IllegalArgumentException("component cannot be null");
    }
    if (!(component instanceof PanamaComponentImpl)) {
      throw new IllegalArgumentException("Component must be a Panama component");
    }
    resourceHandle.beginOperation();
    try {

      final java.lang.foreign.MemorySegment componentPtr =
          ((PanamaComponentImpl) component).getNativeHandle();
      if (componentPtr == null || componentPtr.equals(java.lang.foreign.MemorySegment.NULL)) {
        throw new ai.tegmentum.wasmtime4j.exception.WasmException(
            "Component has invalid native handle");
      }

      return new PanamaGuestProfiler(
          nativeEngine,
          componentName,
          interval.toNanos(),
          componentPtr,
          extraModules != null ? extraModules : java.util.Collections.emptyMap());
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.WeakEngine weak() {
    resourceHandle.beginOperation();
    try {
      final MemorySegment weakPtr = NATIVE_BINDINGS.engineCreateWeak(nativeEngine);
      if (weakPtr == null || weakPtr.equals(MemorySegment.NULL)) {
        throw new IllegalStateException("Failed to create weak engine reference");
      }
      return new PanamaWeakEngine(weakPtr, this);
    } finally {
      resourceHandle.endOperation();
    }
  }
}
