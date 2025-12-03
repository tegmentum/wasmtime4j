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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;

/**
 * Context for WASI-Threads support, enabling WebAssembly modules to spawn threads.
 *
 * <p>This interface provides thread spawning capabilities for WebAssembly modules that use the
 * wasi-threads proposal. The context maintains a pre-instantiated module and manages thread IDs for
 * spawned threads.
 *
 * <p><strong>Important:</strong> This is an experimental feature with the following limitation: A
 * trap or WASI exit in one thread will exit the entire process. This makes it unsuitable for
 * multi-tenant embeddings.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (WasiThreadsContext ctx = WasiThreadsContextBuilder.create()
 *         .withModule(module)
 *         .withLinker(linker)
 *         .build()) {
 *     // Spawn a thread with an argument
 *     int threadId = ctx.spawn(42);
 *     System.out.println("Spawned thread: " + threadId);
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see WasiThreadsContextBuilder
 */
public interface WasiThreadsContext extends Closeable {

  /**
   * Spawns a new thread that executes the module's {@code wasi_thread_start} export function.
   *
   * <p>The spawned thread will call the module's {@code wasi_thread_start} function with the
   * provided argument. The function signature must be {@code (i32, i32) -> ()} where the first
   * parameter is the thread ID and the second is the user-provided argument.
   *
   * @param threadStartArg the argument to pass to the thread start function
   * @return a positive thread ID on success (range: 1 to 0x1FFFFFFF), or -1 on failure
   * @throws WasmException if thread spawning fails due to an internal error
   */
  int spawn(int threadStartArg) throws WasmException;

  /**
   * Gets the current thread count, including the main thread and all spawned threads.
   *
   * @return the number of active threads
   */
  int getThreadCount();

  /**
   * Checks if WASI-Threads support is enabled for this context.
   *
   * @return true if WASI-Threads is enabled, false otherwise
   */
  boolean isEnabled();

  /**
   * Gets the maximum thread ID that has been assigned.
   *
   * <p>Thread IDs are assigned sequentially starting from 1. This method returns the highest thread
   * ID that has been assigned, which may be useful for debugging or monitoring.
   *
   * @return the maximum assigned thread ID, or 0 if no threads have been spawned
   */
  int getMaxThreadId();

  /**
   * Checks if this context is still valid and usable.
   *
   * @return true if the context is valid, false otherwise
   */
  boolean isValid();

  /**
   * Closes the WASI-Threads context and releases associated resources.
   *
   * <p>After calling this method, the context becomes invalid and should not be used. Any spawned
   * threads should complete or be terminated before closing.
   */
  @Override
  void close();
}
