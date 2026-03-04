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
import java.util.List;

/**
 * Provides scoped access to a store for making concurrent component function calls.
 *
 * <p>This interface corresponds to Wasmtime's {@code Accessor<T, D>} type, which provides scoped
 * access to a store within a {@code run_concurrent} block. A {@code ConcurrentScope} is
 * lifetime-bound to the concurrent execution context — it cannot be stored or used outside the
 * {@link AccessorTask} that receives it.
 *
 * <p>Within the scope, you can make concurrent function calls using {@link
 * #callConcurrent(ComponentFunc, List)}. Each call is dispatched to Wasmtime's concurrent
 * scheduler, which cooperatively interleaves execution with other concurrent tasks.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * List<ComponentVal> results = instance.runConcurrent(scope -> {
 *     ComponentFunc func = instance.getFunction("process");
 *     return scope.callConcurrent(func, List.of(ComponentVal.s32(42)));
 * });
 * }</pre>
 *
 * @since 1.1.0
 * @see AccessorTask
 * @see ComponentInstance#runConcurrent(List)
 */
public interface ConcurrentScope {

  /**
   * Makes a concurrent component function call within this scope.
   *
   * <p>The call is dispatched to Wasmtime's concurrent scheduler and may cooperatively interleave
   * with other concurrent calls. The method blocks until the call completes and returns the
   * results.
   *
   * @param func the component function to call
   * @param args the arguments to pass to the function
   * @return the list of return values from the function call
   * @throws WasmException if the function call fails
   * @throws IllegalArgumentException if func or args is null
   * @throws IllegalStateException if this scope has expired (used outside the AccessorTask)
   */
  List<ComponentVal> callConcurrent(ComponentFunc func, List<ComponentVal> args)
      throws WasmException;
}
