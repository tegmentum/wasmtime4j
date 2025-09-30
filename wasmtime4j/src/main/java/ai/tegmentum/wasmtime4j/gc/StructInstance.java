package ai.tegmentum.wasmtime4j.gc;

import java.util.List;
import java.util.Optional;

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
 * double x = point.getField("x").asDouble();
 * double y = point.getField(1).asDouble();
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
  GcValue getField(int index);

  /**
   * Sets a field value by index.
   *
   * @param index the field index
   * @param value the new value
   * @throws IndexOutOfBoundsException if index is invalid
   * @throws IllegalArgumentException if field is immutable or value type is incompatible
   * @throws GcException if field assignment fails
   */
  void setField(int index, GcValue value);
}
