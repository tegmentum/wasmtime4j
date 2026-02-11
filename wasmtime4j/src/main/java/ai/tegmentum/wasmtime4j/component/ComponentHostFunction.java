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
import ai.tegmentum.wasmtime4j.func.Caller;
import java.util.List;

/**
 * Represents a host function that can be called from WebAssembly components.
 *
 * <p>ComponentHostFunction differs from the core module {@link HostFunction} in that it works with
 * Component Model values ({@link ComponentVal}) which support a richer type system including
 * strings, lists, records, variants, options, results, and resources.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Simple function that takes a string and returns nothing
 * ComponentHostFunction print = ComponentHostFunction.create(params -> {
 *     String message = params.get(0).asString();
 *     System.out.println(message);
 *     return List.of();
 * });
 *
 * // Function that takes two numbers and returns their sum
 * ComponentHostFunction add = ComponentHostFunction.create(params -> {
 *     int a = params.get(0).asS32();
 *     int b = params.get(1).asS32();
 *     return List.of(ComponentVal.s32(a + b));
 * });
 *
 * // Function that returns a result type
 * ComponentHostFunction divide = ComponentHostFunction.create(params -> {
 *     int a = params.get(0).asS32();
 *     int b = params.get(1).asS32();
 *     if (b == 0) {
 *         return List.of(ComponentVal.err(ComponentVal.string("division by zero")));
 *     }
 *     return List.of(ComponentVal.ok(ComponentVal.s32(a / b)));
 * });
 * }</pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface ComponentHostFunction {

  /**
   * Executes the host function with the given parameters.
   *
   * @param params the parameters passed from the component
   * @return the results to return to the component
   * @throws WasmException if function execution fails
   */
  List<ComponentVal> execute(List<ComponentVal> params) throws WasmException;

  /**
   * Creates a component host function from the given implementation.
   *
   * @param impl the function implementation
   * @return a new ComponentHostFunction
   */
  static ComponentHostFunction create(final ComponentHostFunction impl) {
    return impl;
  }

  /**
   * Creates a component host function that takes no parameters and returns no values.
   *
   * @param impl the void implementation
   * @return a new ComponentHostFunction
   */
  static ComponentHostFunction voidFunction(final VoidComponentHostFunction impl) {
    return params -> {
      impl.execute();
      return List.of();
    };
  }

  /**
   * Creates a component host function that takes parameters but returns no values.
   *
   * @param impl the void implementation with parameters
   * @return a new ComponentHostFunction
   */
  static ComponentHostFunction voidFunctionWithParams(
      final VoidComponentHostFunctionWithParams impl) {
    return params -> {
      impl.execute(params);
      return List.of();
    };
  }

  /**
   * Creates a component host function that returns a single value.
   *
   * @param impl the single-value implementation
   * @return a new ComponentHostFunction
   */
  static ComponentHostFunction singleValue(final SingleValueComponentHostFunction impl) {
    return params -> {
      final ComponentVal result = impl.execute(params);
      return result != null ? List.of(result) : List.of();
    };
  }

  /**
   * Creates a component host function with caller context.
   *
   * @param <T> the type of user data associated with the store
   * @param impl the implementation that receives caller context
   * @return a new ComponentHostFunction
   */
  static <T> ComponentHostFunction withCaller(final ComponentHostFunctionWithCaller<T> impl) {
    return new CallerAwareComponentHostFunction<>(impl);
  }

  /** Functional interface for void component host functions. */
  @FunctionalInterface
  interface VoidComponentHostFunction {
    /**
     * Executes the function.
     *
     * @throws WasmException if execution fails
     */
    void execute() throws WasmException;
  }

  /** Functional interface for void component host functions with parameters. */
  @FunctionalInterface
  interface VoidComponentHostFunctionWithParams {
    /**
     * Executes the function with the given parameters.
     *
     * @param params the function parameters
     * @throws WasmException if execution fails
     */
    void execute(List<ComponentVal> params) throws WasmException;
  }

  /** Functional interface for single-value component host functions. */
  @FunctionalInterface
  interface SingleValueComponentHostFunction {
    /**
     * Executes the function and returns a single value.
     *
     * @param params the function parameters
     * @return the result value
     * @throws WasmException if execution fails
     */
    ComponentVal execute(List<ComponentVal> params) throws WasmException;
  }

  /** Functional interface for component host functions with caller context. */
  @FunctionalInterface
  interface ComponentHostFunctionWithCaller<T> {
    /**
     * Executes the function with caller context.
     *
     * @param caller the caller context
     * @param params the function parameters
     * @return the results
     * @throws WasmException if execution fails
     */
    List<ComponentVal> execute(Caller<T> caller, List<ComponentVal> params) throws WasmException;
  }

  /** Wrapper for component host functions that need caller context. */
  class CallerAwareComponentHostFunction<T> implements ComponentHostFunction {
    private final ComponentHostFunctionWithCaller<T> implementation;

    /**
     * Creates a caller-aware component host function.
     *
     * @param impl the implementation
     */
    public CallerAwareComponentHostFunction(final ComponentHostFunctionWithCaller<T> impl) {
      this.implementation = impl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ComponentVal> execute(final List<ComponentVal> params) throws WasmException {
      final Caller<T> caller = getCurrentCaller();
      return implementation.execute(caller, params);
    }

    @SuppressWarnings("unchecked")
    private Caller<T> getCurrentCaller() {
      final java.util.ServiceLoader<ai.tegmentum.wasmtime4j.spi.CallerContextProvider> loader =
          java.util.ServiceLoader.load(ai.tegmentum.wasmtime4j.spi.CallerContextProvider.class);

      final java.util.Iterator<ai.tegmentum.wasmtime4j.spi.CallerContextProvider> iterator =
          loader.iterator();
      if (iterator.hasNext()) {
        return iterator.next().getCurrentCaller();
      }

      throw new UnsupportedOperationException(
          "No CallerContextProvider found - ensure runtime module is on classpath");
    }

    /**
     * Gets the underlying implementation.
     *
     * @return the implementation
     */
    public ComponentHostFunctionWithCaller<T> getImplementation() {
      return implementation;
    }
  }
}
