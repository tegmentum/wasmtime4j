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

package ai.tegmentum.wasmtime4j.ref;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Represents a WebAssembly function reference (funcref).
 *
 * <p>FuncRef is a first-class reference to a WebAssembly function that can be stored in tables,
 * passed as arguments, or returned from functions. Unlike ExternRef, FuncRef references are
 * typed and can only hold references to functions.
 *
 * <p>FuncRef is part of the WebAssembly reference types proposal and provides:
 * <ul>
 *   <li>First-class function references that can be passed around</li>
 *   <li>Typed function pointers for indirect calls</li>
 *   <li>Null function references for optional function values</li>
 *   <li>Type-safe function invocation</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Get a function reference from a table or export
 * FuncRef funcRef = table.get(store, 0, FuncRef.class);
 *
 * // Check if the reference is valid
 * if (!funcRef.isNull()) {
 *     // Get the function type
 *     FunctionType type = funcRef.getType(store);
 *
 *     // Invoke the function
 *     List<WasmValue> results = funcRef.call(store, args);
 * }
 *
 * // Create a null function reference
 * FuncRef nullRef = FuncRef.nullRef();
 * }</pre>
 *
 * <p>Type hierarchy:
 * <pre>
 *       func (top)
 *         |
 *      funcref
 *         |
 *      nofunc (bottom)
 * </pre>
 *
 * @since 1.1.0
 */
public interface FuncRef extends HeapType {

  /**
   * Creates a null function reference.
   *
   * <p>A null funcref represents the absence of a function reference. It can be used
   * as a default value in tables or to indicate an optional function.
   *
   * @return a null function reference
   */
  static FuncRef nullRef() {
    return NullFuncRefImpl.INSTANCE;
  }

  /**
   * Creates a function reference from a WebAssembly function.
   *
   * @param function the WebAssembly function to wrap
   * @return a function reference to the given function
   * @throws IllegalArgumentException if function is null
   */
  static FuncRef fromFunction(final WasmFunction function) {
    if (function == null) {
      throw new IllegalArgumentException("function cannot be null");
    }
    return new WasmFunctionRefImpl(function);
  }

  /**
   * Checks if this function reference is null.
   *
   * @return true if this is a null reference
   */
  boolean isNull();

  /**
   * Gets the underlying WebAssembly function, if present.
   *
   * @return an Optional containing the function, or empty if this is a null reference
   */
  Optional<WasmFunction> getFunction();

  /**
   * Gets the function type of this reference.
   *
   * @return the function type, or null if this is a null reference
   */
  FunctionType getFunctionType();

  /**
   * Calls the referenced function with the given arguments.
   *
   * @param args the arguments to pass to the function
   * @return the results from the function call
   * @throws IllegalStateException if this is a null reference
   * @throws WasmException if the call fails
   */
  List<WasmValue> call(List<WasmValue> args) throws WasmException;

  /**
   * Calls the referenced function with the given arguments.
   *
   * @param args the arguments to pass to the function
   * @return the results from the function call
   * @throws IllegalStateException if this is a null reference
   * @throws WasmException if the call fails
   */
  default List<WasmValue> call(final WasmValue... args) throws WasmException {
    return call(args == null ? List.of() : Arrays.asList(args));
  }

  @Override
  default WasmValueType getValueType() {
    return WasmValueType.FUNCREF;
  }

  @Override
  default boolean isBottom() {
    return false;
  }

  @Override
  default String getTypeName() {
    return "funcref";
  }

  /**
   * Null function reference implementation.
   */
  final class NullFuncRefImpl implements FuncRef {

    static final NullFuncRefImpl INSTANCE = new NullFuncRefImpl();

    private NullFuncRefImpl() {
      // Singleton
    }

    @Override
    public boolean isNull() {
      return true;
    }

    @Override
    public Optional<WasmFunction> getFunction() {
      return Optional.empty();
    }

    @Override
    public FunctionType getFunctionType() {
      return null;
    }

    @Override
    public List<WasmValue> call(final List<WasmValue> args) {
      throw new IllegalStateException("Cannot call a null function reference");
    }

    @Override
    public boolean isNullable() {
      return true;
    }

    @Override
    public boolean isSubtypeOf(final HeapType other) {
      // Null funcref is a subtype of funcref and nofunc
      return other instanceof FuncRef || other instanceof NoFunc;
    }

    @Override
    public String toString() {
      return "funcref.null";
    }

    @Override
    public boolean equals(final Object obj) {
      return obj instanceof NullFuncRefImpl;
    }

    @Override
    public int hashCode() {
      return NullFuncRefImpl.class.hashCode();
    }
  }

  /**
   * Function reference wrapping a WebAssembly function.
   */
  final class WasmFunctionRefImpl implements FuncRef {

    private final WasmFunction function;

    WasmFunctionRefImpl(final WasmFunction function) {
      this.function = function;
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public Optional<WasmFunction> getFunction() {
      return Optional.of(function);
    }

    @Override
    public FunctionType getFunctionType() {
      return function.getFunctionType();
    }

    @Override
    public List<WasmValue> call(final List<WasmValue> args) throws WasmException {
      WasmValue[] argsArray = args == null ? new WasmValue[0] : args.toArray(new WasmValue[0]);
      WasmValue[] results = function.call(argsArray);
      return Arrays.asList(results);
    }

    @Override
    public boolean isNullable() {
      return false;
    }

    @Override
    public boolean isSubtypeOf(final HeapType other) {
      // FuncRef is a subtype of itself and func types
      if (other instanceof FuncRef) {
        return true;
      }
      return other.getValueType() == WasmValueType.FUNCREF;
    }

    @Override
    public String toString() {
      return "funcref(" + function + ")";
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof WasmFunctionRefImpl)) {
        return false;
      }
      final WasmFunctionRefImpl other = (WasmFunctionRefImpl) obj;
      return function.equals(other.function);
    }

    @Override
    public int hashCode() {
      return function.hashCode();
    }
  }
}
