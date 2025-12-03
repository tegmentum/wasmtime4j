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

package ai.tegmentum.wasmtime4j.wasi.threads;

import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ServiceLoader;

/**
 * Factory for creating WASI-Threads contexts.
 *
 * <p>This factory uses the Java ServiceLoader mechanism to discover and instantiate the appropriate
 * WASI-Threads implementation based on the runtime environment (JNI or Panama).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a builder
 * WasiThreadsContextBuilder builder = WasiThreadsFactory.createBuilder();
 *
 * // Or check if WASI-Threads is supported
 * if (WasiThreadsFactory.isSupported()) {
 *     WasiThreadsContext ctx = WasiThreadsFactory.createContext(module, linker, store);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiThreadsFactory {

  private static final WasiThreadsProvider PROVIDER;

  static {
    WasiThreadsProvider found = null;
    for (WasiThreadsProvider provider : ServiceLoader.load(WasiThreadsProvider.class)) {
      if (provider.isAvailable()) {
        found = provider;
        break;
      }
    }
    PROVIDER = found;
  }

  private WasiThreadsFactory() {
    // Utility class, prevent instantiation
  }

  /**
   * Checks if WASI-Threads support is available in the current runtime.
   *
   * @return true if WASI-Threads is supported, false otherwise
   */
  public static boolean isSupported() {
    return PROVIDER != null && PROVIDER.isAvailable();
  }

  /**
   * Creates a new builder for configuring a WASI-Threads context.
   *
   * @return a new WasiThreadsContextBuilder
   * @throws UnsupportedOperationException if WASI-Threads is not supported
   */
  public static WasiThreadsContextBuilder createBuilder() {
    if (PROVIDER == null) {
      throw new UnsupportedOperationException(
          "WASI-Threads is not supported in this runtime environment");
    }
    return PROVIDER.createBuilder();
  }

  /**
   * Creates a WASI-Threads context with the specified module, linker, and store.
   *
   * <p>This is a convenience method that creates a builder, configures it, and builds the context
   * in one call.
   *
   * @param module the WebAssembly module with wasi_thread_start export
   * @param linker the linker with WASI imports
   * @param store the store for the main thread
   * @return the configured WasiThreadsContext
   * @throws WasmException if context creation fails
   * @throws UnsupportedOperationException if WASI-Threads is not supported
   * @throws IllegalArgumentException if any parameter is null
   */
  public static WasiThreadsContext createContext(
      final Module module, final Linker<?> linker, final Store store) throws WasmException {
    return createBuilder().withModule(module).withLinker(linker).withStore(store).build();
  }

  /**
   * Adds the WASI-Threads {@code thread-spawn} function to a linker.
   *
   * <p>This method registers the {@code wasi::thread-spawn} import function in the linker, which
   * WebAssembly modules call to spawn new threads.
   *
   * @param linker the linker to add the function to
   * @param store the store for the context
   * @param module the module that will use the thread-spawn function
   * @throws WasmException if adding to linker fails
   * @throws UnsupportedOperationException if WASI-Threads is not supported
   */
  public static void addToLinker(final Linker<?> linker, final Store store, final Module module)
      throws WasmException {
    if (PROVIDER == null) {
      throw new UnsupportedOperationException(
          "WASI-Threads is not supported in this runtime environment");
    }
    PROVIDER.addToLinker(linker, store, module);
  }
}
