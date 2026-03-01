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
 * Provides access to concurrent component function calls within an async scope.
 *
 * <p>An Accessor models the Rust {@code |accessor|} closure parameter from {@code run_concurrent}.
 * It allows calling component functions concurrently within a cooperative scheduling scope.
 *
 * <p>Accessors are only valid within the scope of an {@link AccessorTask} execution and must not be
 * used after the task completes.
 *
 * @since 1.1.0
 */
public interface Accessor {

  /**
   * Calls a component function concurrently.
   *
   * <p>The call is scheduled cooperatively within the current concurrent scope. Results are
   * returned when the function completes.
   *
   * @param func the component function to call
   * @param args the arguments to pass to the function
   * @return the function results
   * @throws WasmException if the call fails
   * @throws IllegalArgumentException if func or args is null
   */
  List<ComponentVal> callConcurrent(ComponentFunc func, List<ComponentVal> args)
      throws WasmException;
}
