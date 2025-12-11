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

import ai.tegmentum.wasmtime4j.WasmValueType;

/**
 * Represents a WebAssembly heap type for reference types.
 *
 * <p>Heap types are used in the WebAssembly GC proposal to define the types
 * of values that can be stored in references. This interface provides a common
 * abstraction for all heap types including:
 *
 * <ul>
 *   <li>func - Function references</li>
 *   <li>extern - External references</li>
 *   <li>any - Any GC reference</li>
 *   <li>eq - Equality-comparable references</li>
 *   <li>i31 - 31-bit integers</li>
 *   <li>struct - Struct references</li>
 *   <li>array - Array references</li>
 *   <li>none, nofunc, noextern - Bottom types</li>
 * </ul>
 *
 * @since 1.1.0
 */
public interface HeapType {

  /**
   * Gets the corresponding WasmValueType for this heap type.
   *
   * @return the WasmValueType
   */
  WasmValueType getValueType();

  /**
   * Checks if this heap type is nullable.
   *
   * @return true if nullable
   */
  boolean isNullable();

  /**
   * Checks if this is a bottom type (none, nofunc, noextern).
   *
   * @return true if this is a bottom type
   */
  default boolean isBottom() {
    return false;
  }

  /**
   * Checks if this type is a subtype of another heap type.
   *
   * @param other the potential supertype
   * @return true if this type is a subtype of other
   */
  boolean isSubtypeOf(HeapType other);

  /**
   * Gets the name of this heap type.
   *
   * @return the type name
   */
  String getTypeName();

  /**
   * Creates a nullable version of this heap type.
   *
   * @return a nullable heap type
   */
  default HeapType asNullable() {
    return this; // Default implementation, subclasses may override
  }

  /**
   * Creates a non-nullable version of this heap type.
   *
   * @return a non-nullable heap type
   */
  default HeapType asNonNullable() {
    return this; // Default implementation, subclasses may override
  }

  // ===== Factory methods for bottom types =====

  /**
   * Gets the none heap type (bottom of any/eq hierarchy).
   *
   * @return the NoneRef instance
   */
  static HeapType none() {
    return NoneRef.INSTANCE;
  }

  /**
   * Gets the nofunc heap type (bottom of func hierarchy).
   *
   * @return the NoFunc instance
   */
  static HeapType nofunc() {
    return NoFunc.INSTANCE;
  }

  /**
   * Gets the noextern heap type (bottom of extern hierarchy).
   *
   * @return the NoExtern instance
   */
  static HeapType noextern() {
    return NoExtern.INSTANCE;
  }

  /**
   * Creates a null function reference.
   *
   * @return a null FuncRef
   */
  static FuncRef funcNull() {
    return FuncRef.nullRef();
  }

  /**
   * Creates a function reference from a WebAssembly function.
   *
   * @param function the WebAssembly function
   * @return a FuncRef wrapping the function
   * @throws IllegalArgumentException if function is null
   */
  static FuncRef func(final ai.tegmentum.wasmtime4j.WasmFunction function) {
    return FuncRef.fromFunction(function);
  }
}
