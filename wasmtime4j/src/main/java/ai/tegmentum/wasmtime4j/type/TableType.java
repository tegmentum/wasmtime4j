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
package ai.tegmentum.wasmtime4j.type;

import ai.tegmentum.wasmtime4j.WasmValueType;

/**
 * Represents the type information of a WebAssembly table.
 *
 * <p>This interface provides access to table type metadata including element type and size
 * constraints. Tables store references to functions or external objects.
 *
 * @since 1.0.0
 */
public interface TableType extends WasmType {

  /**
   * Gets the element type stored in this table.
   *
   * @return the element type (FUNCREF or EXTERNREF)
   */
  WasmValueType getElementType();

  /**
   * Gets the minimum number of elements required.
   *
   * @return the minimum element count
   */
  long getMinimum();

  /**
   * Gets the maximum number of elements allowed.
   *
   * @return the maximum element count, or empty if unlimited
   */
  java.util.Optional<Long> getMaximum();

  /**
   * Checks if this table uses 64-bit indices.
   *
   * <p>64-bit tables are part of the WebAssembly Memory64 proposal, allowing tables with more than
   * 2^32 elements using 64-bit indices.
   *
   * @return true if this table uses 64-bit indices, false if 32-bit
   * @since 1.1.0
   */
  default boolean is64Bit() {
    return false;
  }

  /**
   * Gets the element reference type for this table.
   *
   * <p>This returns the full {@link RefType} for the element, providing heap type and nullability
   * information. For tables created with only a {@link WasmValueType}, the returned RefType is
   * derived from the element type.
   *
   * @return the element reference type
   * @since 1.1.0
   */
  default RefType getElement() {
    final WasmValueType elementType = getElementType();
    final ValType valType = ValType.from(elementType);
    return valType.asRef().orElse(RefType.FUNCREF);
  }

  /**
   * Creates a TableType with the specified element type and size constraints.
   *
   * @param elementType the element type (e.g., FUNCREF or EXTERNREF)
   * @param min the minimum number of elements
   * @param max the maximum number of elements, or empty if unlimited
   * @return a new TableType
   * @throws IllegalArgumentException if elementType is null or min is negative
   */
  static TableType of(
      final WasmValueType elementType, final long min, final java.util.OptionalLong max) {
    return DefaultTableType.create(elementType, min, max);
  }

  /**
   * Creates a TableType with 64-bit indices and the specified element type and size constraints.
   *
   * <p>64-bit tables are part of the WebAssembly Memory64 proposal, allowing tables with more than
   * 2^32 elements using 64-bit indices.
   *
   * @param elementType the element type (e.g., FUNCREF or EXTERNREF)
   * @param min the minimum number of elements
   * @param max the maximum number of elements, or empty if unlimited
   * @return a new 64-bit TableType
   * @throws IllegalArgumentException if elementType is null or min is negative
   * @since 1.1.0
   */
  static TableType of64(
      final WasmValueType elementType, final long min, final java.util.OptionalLong max) {
    return DefaultTableType.create64(elementType, min, max);
  }

  @Override
  default WasmTypeKind getKind() {
    return WasmTypeKind.TABLE;
  }
}
