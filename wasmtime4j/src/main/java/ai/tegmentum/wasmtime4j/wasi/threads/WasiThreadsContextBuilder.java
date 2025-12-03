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

/**
 * Builder interface for creating {@link WasiThreadsContext} instances.
 *
 * <p>This builder allows configuring the WASI-Threads context with the required module, linker, and
 * store. The module must export a {@code wasi_thread_start} function with the signature {@code
 * (i32, i32) -> ()}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiThreadsContext ctx = WasiThreadsContextBuilder.create()
 *     .withModule(module)
 *     .withLinker(linker)
 *     .withStore(store)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiThreadsContextBuilder {

  /**
   * Sets the WebAssembly module that will be used for thread spawning.
   *
   * <p>The module must contain a {@code wasi_thread_start} export with the signature {@code (i32,
   * i32) -> ()}.
   *
   * @param module the WebAssembly module
   * @return this builder for chaining
   * @throws IllegalArgumentException if module is null
   */
  WasiThreadsContextBuilder withModule(Module module);

  /**
   * Sets the linker that contains the WASI imports.
   *
   * <p>The linker should have WASI imports already configured. The WASI-Threads implementation will
   * add the {@code wasi::thread-spawn} function to this linker.
   *
   * @param linker the linker with WASI imports
   * @return this builder for chaining
   * @throws IllegalArgumentException if linker is null
   */
  WasiThreadsContextBuilder withLinker(Linker<?> linker);

  /**
   * Sets the store that will be used for the main thread.
   *
   * <p>Each spawned thread will create its own store instance, but this store is used as the
   * template for configuration.
   *
   * @param store the store for the main thread
   * @return this builder for chaining
   * @throws IllegalArgumentException if store is null
   */
  WasiThreadsContextBuilder withStore(Store store);

  /**
   * Builds the {@link WasiThreadsContext} with the configured settings.
   *
   * <p>This method validates that all required components (module, linker, store) have been
   * provided and creates the WASI-Threads context.
   *
   * @return the configured WasiThreadsContext
   * @throws WasmException if context creation fails
   * @throws IllegalStateException if required components are not set
   */
  WasiThreadsContext build() throws WasmException;
}
