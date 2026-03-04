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
package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * A task that executes within a concurrent scope, receiving a {@link ConcurrentScope} for making
 * concurrent component function calls.
 *
 * <p>This functional interface is used with Wasmtime's concurrent execution model, corresponding to
 * the concept of a task that runs within a {@code run_concurrent} block with access to an {@code
 * Accessor}. The {@link ConcurrentScope} parameter provides scoped access to the store for making
 * concurrent function calls.
 *
 * <p>The scope is lifetime-bound to the task execution — it must not be stored or used after the
 * task returns. Any attempt to use the scope after the task completes will throw {@link
 * IllegalStateException}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * AccessorTask<List<ComponentVal>> task = scope -> {
 *     ComponentFunc func = instance.getFunction("compute");
 *     return scope.callConcurrent(func, List.of(ComponentVal.s32(42)));
 * };
 * }</pre>
 *
 * @param <T> the result type produced by this task
 * @since 1.1.0
 * @see ConcurrentScope
 * @see ComponentInstance#runConcurrent(java.util.List)
 */
@FunctionalInterface
public interface AccessorTask<T> {

  /**
   * Executes this task within the given concurrent scope.
   *
   * <p>The scope provides access to the store for making concurrent component function calls. The
   * scope is only valid during the execution of this method — it must not be stored or used after
   * this method returns.
   *
   * @param scope the concurrent scope providing store access
   * @return the result of the task
   * @throws WasmException if the task encounters an error during execution
   */
  T execute(ConcurrentScope scope) throws WasmException;
}
