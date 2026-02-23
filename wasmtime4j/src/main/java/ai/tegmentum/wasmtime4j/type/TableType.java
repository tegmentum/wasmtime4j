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

  @Override
  default WasmTypeKind getKind() {
    return WasmTypeKind.TABLE;
  }
}
