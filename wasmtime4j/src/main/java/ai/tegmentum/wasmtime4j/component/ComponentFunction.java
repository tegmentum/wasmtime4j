/*
 * Copyright 2024 Tegmentum AI
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
 * Represents a WebAssembly Component Model function that can be invoked.
 *
 * <p>This interface provides a first-class function object for component functions, allowing
 * functions to be retrieved once and called multiple times without name lookup overhead.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ComponentInstance instance = ...;
 * ComponentFunction addFunc = instance.getFunc("add")
 *     .orElseThrow(() -> new IllegalStateException("Function not found"));
 *
 * // Call the function multiple times
 * Object result1 = addFunc.call(1, 2);
 * Object result2 = addFunc.call(3, 4);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentFunction {

  /**
   * Gets the name of this function.
   *
   * @return the function name
   */
  String getName();

  /**
   * Invokes this function with the provided arguments.
   *
   * <p>Arguments are automatically converted to WIT values using the standard Java-to-WIT type
   * mapping. Supported types include:
   *
   * <ul>
   *   <li>{@link Boolean} - converts to WIT bool
   *   <li>{@link Integer} - converts to WIT s32
   *   <li>{@link Long} - converts to WIT s64
   *   <li>{@link Float}, {@link Double} - converts to WIT float64
   *   <li>{@link Character} - converts to WIT char
   *   <li>{@link String} - converts to WIT string
   *   <li>{@link java.util.Map} - converts to WIT record
   *   <li>WitValue subclasses - used directly
   * </ul>
   *
   * @param args the arguments to pass to the function
   * @return the result of the function invocation, or null if the function returns void
   * @throws WasmException if function invocation fails or arguments are invalid
   */
  Object call(Object... args) throws WasmException;

  /**
   * Checks if this function is still valid and can be called.
   *
   * <p>A function becomes invalid when its parent component instance is closed.
   *
   * @return true if the function is valid and can be called, false otherwise
   */
  boolean isValid();

  /**
   * Gets the parent component instance that this function belongs to.
   *
   * @return the parent component instance
   */
  ComponentInstance getInstance();
}
