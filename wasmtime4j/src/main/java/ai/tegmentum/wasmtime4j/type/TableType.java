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

  @Override
  default WasmTypeKind getKind() {
    return WasmTypeKind.TABLE;
  }
}
