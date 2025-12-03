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

/**
 * WASI-Threads support for WebAssembly thread spawning.
 *
 * <p>This package provides support for the wasi-threads proposal, enabling WebAssembly modules to
 * spawn threads. The main entry point is {@link
 * ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsFactory}.
 *
 * <p><strong>Important limitations:</strong>
 *
 * <ul>
 *   <li>This is an experimental feature
 *   <li>A trap or WASI exit in one thread will exit the entire process
 *   <li>Not suitable for multi-tenant embeddings
 *   <li>Requires WASI Preview 1 (not compatible with WASI 0.2)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Check if WASI-Threads is supported
 * if (WasiThreadsFactory.isSupported()) {
 *     // Create a WASI-Threads context
 *     try (WasiThreadsContext ctx = WasiThreadsFactory.createContext(module, linker, store)) {
 *         // Spawn a thread
 *         int threadId = ctx.spawn(42);
 *         System.out.println("Spawned thread: " + threadId);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsFactory
 * @see ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContext
 */
package ai.tegmentum.wasmtime4j.wasi.threads;
