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
 * Service provider interface for WASI-Threads implementations.
 *
 * <p>This interface is used by the {@link WasiThreadsFactory} to discover and instantiate
 * WASI-Threads implementations using the Java ServiceLoader mechanism.
 *
 * <p>Implementations should be registered in {@code
 * META-INF/services/ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsProvider}.
 *
 * @since 1.0.0
 */
public interface WasiThreadsProvider {

  /**
   * Checks if this provider is available for use.
   *
   * <p>This method should return true only if the native library and all required dependencies are
   * available and properly initialized.
   *
   * @return true if the provider is available, false otherwise
   */
  boolean isAvailable();

  /**
   * Creates a new builder for configuring a WASI-Threads context.
   *
   * @return a new WasiThreadsContextBuilder
   */
  WasiThreadsContextBuilder createBuilder();

  /**
   * Adds the WASI-Threads {@code thread-spawn} function to a linker.
   *
   * @param linker the linker to add the function to
   * @param store the store for the context
   * @param module the module that will use the thread-spawn function
   * @throws WasmException if adding to linker fails
   */
  void addToLinker(Linker<?> linker, Store store, Module module) throws WasmException;
}
