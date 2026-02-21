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
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a typed function exported by a WebAssembly component.
 *
 * <p>ComponentFunc provides type-safe access to component exports, allowing direct invocation with
 * Component Model values. Unlike core WebAssembly functions, component functions use the rich
 * Component Model type system.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Get a function from a component instance
 * ComponentFunc greet = instance.getFunc("greet");
 *
 * // Check function signature
 * System.out.println("Parameters: " + greet.getParameterTypes());
 * System.out.println("Results: " + greet.getResultTypes());
 *
 * // Call the function
 * List<ComponentVal> results = greet.call(ComponentVal.string("World"));
 * String greeting = results.get(0).asString();
 * System.out.println(greeting); // "Hello, World!"
 *
 * // Call with multiple parameters
 * ComponentFunc add = instance.getFunc("add");
 * List<ComponentVal> sum = add.call(ComponentVal.s32(10), ComponentVal.s32(20));
 * System.out.println(sum.get(0).asS32()); // 30
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentFunc {

  /**
   * Gets the function name.
   *
   * @return the function name
   */
  String getName();

  /**
   * Gets the parameter type information.
   *
   * @return list of parameter type descriptors
   */
  List<ComponentTypeDescriptor> getParameterTypes();

  /**
   * Gets the result type information.
   *
   * @return list of result type descriptors
   */
  List<ComponentTypeDescriptor> getResultTypes();

  /**
   * Gets the number of parameters this function expects.
   *
   * @return the parameter count
   */
  default int getParameterCount() {
    return getParameterTypes().size();
  }

  /**
   * Gets the number of results this function returns.
   *
   * @return the result count
   */
  default int getResultCount() {
    return getResultTypes().size();
  }

  /**
   * Calls the function with the given arguments.
   *
   * @param args the function arguments
   * @return the function results
   * @throws WasmException if the call fails
   * @throws IllegalArgumentException if arguments don't match expected types
   */
  List<ComponentVal> call(ComponentVal... args) throws WasmException;

  /**
   * Calls the function with a list of arguments.
   *
   * @param args the function arguments
   * @return the function results
   * @throws WasmException if the call fails
   * @throws IllegalArgumentException if arguments don't match expected types
   */
  List<ComponentVal> call(List<ComponentVal> args) throws WasmException;

  /**
   * Calls the function with no arguments.
   *
   * @return the function results
   * @throws WasmException if the call fails
   */
  default List<ComponentVal> call() throws WasmException {
    return call(List.of());
  }

  /**
   * Asynchronously calls the function with the given arguments.
   *
   * <p>The call is executed on the default ForkJoinPool. The returned future completes with the
   * function results, or completes exceptionally if the call fails.
   *
   * @param args the function arguments
   * @return a CompletableFuture that will contain the function results
   */
  default CompletableFuture<List<ComponentVal>> callAsync(final ComponentVal... args) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return call(args);
          } catch (final WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  /**
   * Asynchronously calls the function with a list of arguments.
   *
   * <p>The call is executed on the default ForkJoinPool. The returned future completes with the
   * function results, or completes exceptionally if the call fails.
   *
   * @param args the function arguments
   * @return a CompletableFuture that will contain the function results
   */
  default CompletableFuture<List<ComponentVal>> callAsync(final List<ComponentVal> args) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return call(args);
          } catch (final WasmException e) {
            throw new RuntimeException(e);
          }
        });
  }

  /**
   * Checks if this function is still valid.
   *
   * @return true if the function is valid
   */
  boolean isValid();
}
