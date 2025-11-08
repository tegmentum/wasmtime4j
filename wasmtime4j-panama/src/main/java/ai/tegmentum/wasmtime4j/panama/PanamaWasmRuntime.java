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

import ai.tegmentum.wasmtime4j.ComponentEngine;
import ai.tegmentum.wasmtime4j.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeInfo;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Serializer;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.StoreLimits;
import ai.tegmentum.wasmtime4j.WasiContext;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.io.IOException;
import java.lang.foreign.Arena;
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
  private volatile boolean closed;

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
      this.closed = false;

      LOGGER.fine("Created Panama WebAssembly runtime");
    } catch (final Exception e) {
      throw new WasmException("Failed to initialize Panama runtime", e);
    }
  }

  @Override
  public Engine createEngine() throws WasmException {
    ensureNotClosed();
    return new PanamaEngine();
  }

  @Override
  public Engine createEngine(final EngineConfig config) throws WasmException {
    PanamaValidation.requireNonNull(config, "config");
    ensureNotClosed();
    return new PanamaEngine(config);
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

    // TODO: Implement store with resource limits for Panama
    // For now, create a regular store and log the resource limits
    LOGGER.fine(
        "Creating store with resource limits - fuel: "
            + fuelLimit
            + ", memory: "
            + memoryLimitBytes
            + " bytes, timeout: "
            + executionTimeoutSeconds
            + "s");

    return engine.createStore();
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

    // TODO: Implement store with StoreLimits for Panama
    LOGGER.fine("Creating store with limits: " + limits);

    return engine.createStore();
  }

  @Override
  public <T> Linker<T> createLinker(final Engine engine) throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    ensureNotClosed();

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException(
          "Engine must be a PanamaEngine instance for Panama runtime");
    }

    return new PanamaLinker<>(engine);
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

    // TODO: Implement linker configuration for Panama
    LOGGER.fine(
        "Creating linker with config - allowUnknownExports: "
            + allowUnknownExports
            + ", allowShadowing: "
            + allowShadowing);

    return new PanamaLinker<>(engine);
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

    return panamaModule.instantiate(store, new ImportMap());
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

    return new PanamaComponentEngine(config);
  }

  @Override
  public RuntimeInfo getRuntimeInfo() {
    return new RuntimeInfo(RuntimeType.PANAMA, "1.0.0", "Wasmtime 36.0.2");
  }

  @Override
  public boolean isValid() {
    return !closed;
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

    // TODO: Implement module deserialization for Panama
    throw new UnsupportedOperationException(
        "Module deserialization not yet implemented for Panama");
  }

  @Override
  public Serializer createSerializer() throws WasmException {
    ensureNotClosed();

    // TODO: Implement serializer for Panama
    throw new UnsupportedOperationException("Serializer not yet implemented for Panama");
  }

  @Override
  public Serializer createSerializer(
      final long maxCacheSize, final boolean enableCompression, final int compressionLevel)
      throws WasmException {
    if (compressionLevel < 0 || compressionLevel > 9) {
      throw new IllegalArgumentException("Compression level must be between 0 and 9");
    }

    ensureNotClosed();

    // TODO: Implement serializer with config for Panama
    throw new UnsupportedOperationException("Serializer not yet implemented for Panama");
  }

  @Override
  public String getDebuggingCapabilities() {
    return "Panama debugging with basic support";
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

    // TODO: Implement WASI linker integration for Panama
    throw new UnsupportedOperationException(
        "WASI linker integration not yet implemented for Panama");
  }

  @Override
  public void addWasiPreview2ToLinker(final Linker<WasiContext> linker, final WasiContext context)
      throws WasmException {
    PanamaValidation.requireNonNull(linker, "linker");
    PanamaValidation.requireNonNull(context, "context");
    ensureNotClosed();

    // TODO: Implement WASI Preview 2 for Panama
    throw new UnsupportedOperationException("WASI Preview 2 not yet implemented for Panama");
  }

  @Override
  public void addComponentModelToLinker(final Linker<WasiContext> linker) throws WasmException {
    PanamaValidation.requireNonNull(linker, "linker");
    ensureNotClosed();

    // TODO: Implement Component Model for Panama
    throw new UnsupportedOperationException("Component Model not yet implemented for Panama");
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiLinker createWasiLinker(final Engine engine)
      throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    ensureNotClosed();

    // TODO: Implement WASI linker for Panama
    throw new UnsupportedOperationException("WASI linker not yet implemented for Panama");
  }

  @Override
  public ai.tegmentum.wasmtime4j.wasi.WasiLinker createWasiLinker(
      final Engine engine, final ai.tegmentum.wasmtime4j.wasi.WasiConfig config)
      throws WasmException {
    PanamaValidation.requireNonNull(engine, "engine");
    PanamaValidation.requireNonNull(config, "config");
    ensureNotClosed();

    // TODO: Implement WASI linker with config for Panama
    throw new UnsupportedOperationException("WASI linker not yet implemented for Panama");
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

      final Engine engine = createEngine();
      defaultGcRuntime = new PanamaGcRuntime(engine);

      LOGGER.fine("Initialized default GC runtime for Panama");
      return defaultGcRuntime;
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
  public void close() {
    if (!closed) {
      closed = true;

      try {
        if (defaultGcRuntime != null) {
          defaultGcRuntime.close();
        }
      } catch (final Exception e) {
        LOGGER.warning("Failed to close GC runtime: " + e.getMessage());
      }

      try {
        if (arena != null) {
          arena.close();
        }
      } catch (final Exception e) {
        LOGGER.warning("Failed to close arena: " + e.getMessage());
      }

      LOGGER.fine("Closed Panama WebAssembly runtime");
    }
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Panama runtime has been closed");
    }
  }
}
