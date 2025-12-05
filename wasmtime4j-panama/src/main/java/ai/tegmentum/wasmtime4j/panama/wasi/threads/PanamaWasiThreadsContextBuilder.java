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

package ai.tegmentum.wasmtime4j.panama.wasi.threads;

import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import ai.tegmentum.wasmtime4j.panama.PanamaLinker;
import ai.tegmentum.wasmtime4j.panama.PanamaModule;
import ai.tegmentum.wasmtime4j.panama.PanamaStore;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContext;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContextBuilder;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of {@link WasiThreadsContextBuilder}.
 *
 * <p>This builder creates Panama-based WASI-Threads contexts for WebAssembly thread spawning.
 *
 * @since 1.0.0
 */
public final class PanamaWasiThreadsContextBuilder implements WasiThreadsContextBuilder {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaWasiThreadsContextBuilder.class.getName());

  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  /** The WebAssembly module for thread spawning. */
  private Module module;

  /** The linker with WASI imports. */
  private Linker<?> linker;

  /** The store for the main thread. */
  private Store store;

  /** Creates a new Panama WASI-Threads context builder. */
  public PanamaWasiThreadsContextBuilder() {
    // Default constructor
  }

  @Override
  public WasiThreadsContextBuilder withModule(final Module module) {
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    this.module = module;
    return this;
  }

  @Override
  public WasiThreadsContextBuilder withLinker(final Linker<?> linker) {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    this.linker = linker;
    return this;
  }

  @Override
  public WasiThreadsContextBuilder withStore(final Store store) {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    this.store = store;
    return this;
  }

  @Override
  public WasiThreadsContext build() throws WasmException {
    validateConfiguration();

    try {
      // Get native segments from Panama implementations
      final MemorySegment moduleSegment = getNativeSegment(module, "module");
      final MemorySegment linkerSegment = getNativeSegment(linker, "linker");
      final MemorySegment storeSegment = getNativeSegment(store, "store");

      LOGGER.fine(
          String.format(
              "Creating WASI-Threads context with module=%s, linker=%s, store=%s",
              moduleSegment, linkerSegment, storeSegment));

      // Create arena for the context
      final Arena arena = Arena.ofShared();

      // Create native WASI-Threads context
      final MemorySegment nativeContext =
          NATIVE_BINDINGS.wasiThreadsContextCreate(
              moduleSegment, linkerSegment, storeSegment, arena);

      // Determine if WASI-Threads is enabled
      final boolean enabled = NATIVE_BINDINGS.wasiThreadsIsSupported();

      LOGGER.info(
          String.format("Created WASI-Threads context: %s, enabled: %s", nativeContext, enabled));

      return new PanamaWasiThreadsContext(nativeContext, arena, enabled);
    } catch (final Exception e) {
      throw new WasmException("Failed to create WASI-Threads context: " + e.getMessage(), e);
    }
  }

  /**
   * Validates that all required components have been configured.
   *
   * @throws IllegalStateException if required components are missing
   */
  private void validateConfiguration() {
    if (module == null) {
      throw new IllegalStateException("Module must be set before building");
    }
    if (linker == null) {
      throw new IllegalStateException("Linker must be set before building");
    }
    if (store == null) {
      throw new IllegalStateException("Store must be set before building");
    }
  }

  /**
   * Gets the native memory segment from a Panama resource.
   *
   * @param resource the resource to get the segment from
   * @param name the name of the resource for error messages
   * @return the native memory segment
   * @throws WasmException if the resource is not a Panama implementation
   */
  private MemorySegment getNativeSegment(final Object resource, final String name)
      throws WasmException {
    if (resource instanceof PanamaModule) {
      return ((PanamaModule) resource).getNativeModule();
    } else if (resource instanceof PanamaLinker) {
      return ((PanamaLinker) resource).getNativeLinker();
    } else if (resource instanceof PanamaStore) {
      return ((PanamaStore) resource).getNativeStore();
    } else {
      throw new WasmException(
          String.format(
              "Expected Panama implementation for %s, got: %s",
              name, resource.getClass().getName()));
    }
  }
}
