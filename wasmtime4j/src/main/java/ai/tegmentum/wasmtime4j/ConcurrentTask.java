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
package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a task that can execute concurrently within a WebAssembly store.
 *
 * <p>Concurrent tasks are part of the shared-everything-threads proposal, enabling multiple
 * WebAssembly computations to cooperatively interleave on the same store. Unlike simple
 * thread-based parallelism, concurrent tasks share the store's state and use cooperative
 * scheduling.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ConcurrentTask<String> task = store -> {
 *     Instance instance = store.createInstance(module);
 *     WasmFunction func = instance.getFunction("process");
 *     WasmValue[] results = func.call(WasmValue.fromI32(42));
 *     return results[0].asString();
 * };
 *
 * CompletableFuture<String> future = store.runConcurrent(task);
 * String result = future.join();
 * }</pre>
 *
 * @param <T> the result type produced by this task
 * @since 1.1.0
 */
@FunctionalInterface
public interface ConcurrentTask<T> {

  /**
   * Executes the concurrent task within the given store context.
   *
   * <p>The store is exclusively available to this task during execution. The task should perform
   * its work and return a result. If the task encounters an error, it should throw a {@link
   * WasmException}.
   *
   * @param store the store context for this task's execution
   * @return the result of the task
   * @throws WasmException if the task encounters an error during execution
   */
  T execute(Store store) throws WasmException;
}
