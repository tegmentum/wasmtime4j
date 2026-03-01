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
package ai.tegmentum.wasmtime4j.gc;

/**
 * WebAssembly GC array instance.
 *
 * <p>Represents an instance of an array type with element values. Provides indexed access to
 * elements with bounds checking and type safety.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * ArrayInstance intArray = runtime.createArray(intArrayType, 10);
 * intArray.setElement(0, GcValue.i32(42));
 * intArray.setElement(1, GcValue.i32(100));
 *
 * int value = intArray.getElement(0).asInt();
 * int length = intArray.getLength();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ArrayInstance extends GcObject {

  /**
   * Gets the array type of this instance.
   *
   * @return the array type
   */
  ArrayType getType();

  /**
   * Gets the length of this array.
   *
   * @return the array length
   */
  int getLength();

  /**
   * Gets an element value by index.
   *
   * @param index the element index
   * @return the element value
   * @throws IndexOutOfBoundsException if index is invalid
   * @throws GcException if element access fails
   */
  GcValue getElement(int index) throws GcException;

  /**
   * Sets an element value by index.
   *
   * @param index the element index
   * @param value the new value
   * @throws IndexOutOfBoundsException if index is invalid
   * @throws IllegalArgumentException if array is immutable or value type is incompatible
   * @throws GcException if element assignment fails
   */
  void setElement(int index, GcValue value) throws GcException;
}
