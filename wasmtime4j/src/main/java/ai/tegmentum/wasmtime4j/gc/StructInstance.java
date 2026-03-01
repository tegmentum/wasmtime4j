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
 * WebAssembly GC struct instance.
 *
 * <p>Represents an instance of a struct type with field values. Provides access to individual
 * fields by index or name, with type safety and mutability checking.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * StructInstance point = runtime.createStruct(pointType)
 *     .setField("x", 10.0)
 *     .setField("y", 20.0);
 *
 * double x = point.getField("x").asF64();
 * double y = point.getField(1).asF64();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface StructInstance extends GcObject {

  /**
   * Gets the struct type of this instance.
   *
   * @return the struct type
   */
  StructType getType();

  /**
   * Gets the number of fields in this struct.
   *
   * @return the field count
   */
  int getFieldCount();

  /**
   * Gets a field value by index.
   *
   * @param index the field index
   * @return the field value
   * @throws IndexOutOfBoundsException if index is invalid
   * @throws GcException if field access fails
   */
  GcValue getField(int index) throws GcException;

  /**
   * Sets a field value by index.
   *
   * @param index the field index
   * @param value the new value
   * @throws IndexOutOfBoundsException if index is invalid
   * @throws IllegalArgumentException if field is immutable or value type is incompatible
   * @throws GcException if field assignment fails
   */
  void setField(int index, GcValue value) throws GcException;
}
