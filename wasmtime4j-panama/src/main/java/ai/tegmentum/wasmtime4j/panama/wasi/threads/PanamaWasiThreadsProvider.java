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
import ai.tegmentum.wasmtime4j.panama.NativeExecutionBindings;
import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.panama.PanamaLinker;
import ai.tegmentum.wasmtime4j.panama.PanamaModule;
import ai.tegmentum.wasmtime4j.panama.PanamaStore;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContextBuilder;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsProvider;
import java.lang.foreign.MemorySegment;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of {@link WasiThreadsProvider}.
 *
 * <p>This provider creates Panama-based WASI-Threads contexts for thread spawning support in
 * WebAssembly modules. It is discovered via the ServiceLoader mechanism.
 *
 * @since 1.0.0
 */
public final class PanamaWasiThreadsProvider implements WasiThreadsProvider {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiThreadsProvider.class.getName());

  /** Cached availability check result. */
  private static volatile Boolean available;

  /**
   * Creates a new Panama WASI-Threads provider.
   *
   * <p>This no-argument constructor is required for ServiceLoader discovery.
   */
  public PanamaWasiThreadsProvider() {
    // ServiceLoader requires a public no-arg constructor
  }

  @Override
  public boolean isAvailable() {
    if (available == null) {
      synchronized (PanamaWasiThreadsProvider.class) {
        if (available == null) {
          available = checkAvailability();
        }
      }
    }
    return available;
  }

  @Override
  public WasiThreadsContextBuilder createBuilder() {
    if (!isAvailable()) {
      throw new UnsupportedOperationException(
          "WASI-Threads is not available in this Panama runtime");
    }
    return new PanamaWasiThreadsContextBuilder();
  }

  @Override
  public void addToLinker(final Linker<?> linker, final Store store, final Module module)
      throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("Linker cannot be null");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }

    if (!isAvailable()) {
      throw new UnsupportedOperationException(
          "WASI-Threads is not available in this Panama runtime");
    }

    // Validate that we have Panama implementations
    if (!(linker instanceof PanamaLinker)) {
      throw new WasmException(
          "Expected Panama linker implementation, got: " + linker.getClass().getName());
    }
    if (!(store instanceof PanamaStore)) {
      throw new WasmException(
          "Expected Panama store implementation, got: " + store.getClass().getName());
    }
    if (!(module instanceof PanamaModule)) {
      throw new WasmException(
          "Expected Panama module implementation, got: " + module.getClass().getName());
    }

    try {
      final MemorySegment linkerSegment = ((PanamaLinker) linker).getNativeLinker();
      final MemorySegment storeSegment = ((PanamaStore) store).getNativeStore();
      final MemorySegment moduleSegment = ((PanamaModule) module).getNativeModule();

      LOGGER.fine(
          String.format(
              "Adding thread-spawn to linker: linker=%s, store=%s, module=%s",
              linkerSegment, storeSegment, moduleSegment));

      final NativeExecutionBindings bindings = NativeExecutionBindings.getInstance();
      bindings.wasiThreadsAddToLinker(linkerSegment, storeSegment, moduleSegment);

      LOGGER.info("Successfully added thread-spawn function to linker");
    } catch (final Exception e) {
      throw new WasmException("Failed to add thread-spawn to linker: " + e.getMessage(), e);
    }
  }

  /**
   * Checks if WASI-Threads support is available.
   *
   * @return true if available, false otherwise
   */
  private static boolean checkAvailability() {
    try {
      // First ensure native library is loaded
      if (!NativeLibraryLoader.getInstance().isLoaded()) {
        LOGGER.fine("Panama native library not loaded, WASI-Threads unavailable");
        return false;
      }

      // Check native support
      final NativeExecutionBindings bindings = NativeExecutionBindings.getInstance();
      final boolean supported = bindings.wasiThreadsIsSupported();
      LOGGER.info("WASI-Threads support check: " + supported);
      return supported;
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.log(Level.FINE, "WASI-Threads native methods not available: " + e.getMessage(), e);
      return false;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error checking WASI-Threads availability: " + e.getMessage(), e);
      return false;
    }
  }
}
