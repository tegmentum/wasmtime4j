package ai.tegmentum.wasmtime4j.gc;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.TagType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a WebAssembly GC exception type.
 *
 * <p>An ExnType wraps a {@link TagType} and provides field-level access to the exception payload
 * types. Each field corresponds to a parameter type in the underlying tag's function signature.
 *
 * <p>This maps to Wasmtime's {@code ExnType} which provides introspection over the types of values
 * carried by GC exception references.
 *
 * @since 1.1.0
 */
public final class ExnType {

  private final TagType tagType;
  private final List<WasmValueType> fields;

  /**
   * Creates a new ExnType from a TagType.
   *
   * @param tagType the tag type describing the exception signature
   * @throws IllegalArgumentException if tagType is null
   */
  public ExnType(final TagType tagType) {
    if (tagType == null) {
      throw new IllegalArgumentException("tagType cannot be null");
    }
    this.tagType = tagType;

    final FunctionType funcType = tagType.getFunctionType();
    final WasmValueType[] paramTypes = funcType.getParamTypes();
    final List<WasmValueType> fieldList = new ArrayList<>(paramTypes.length);
    for (final WasmValueType paramType : paramTypes) {
      fieldList.add(paramType);
    }
    this.fields = Collections.unmodifiableList(fieldList);
  }

  /**
   * Gets the underlying tag type.
   *
   * @return the tag type
   */
  public TagType tagType() {
    return tagType;
  }

  /**
   * Gets the type of a specific field by index.
   *
   * @param index the zero-based field index
   * @return the field's value type
   * @throws IndexOutOfBoundsException if index is out of range
   */
  public WasmValueType field(final int index) {
    return fields.get(index);
  }

  /**
   * Gets all field types of this exception type.
   *
   * @return an unmodifiable list of field value types
   */
  public List<WasmValueType> fields() {
    return fields;
  }

  /**
   * Gets the number of fields in this exception type.
   *
   * @return the field count
   */
  public int fieldCount() {
    return fields.size();
  }

  /**
   * Checks if this exception type matches another for compatibility.
   *
   * <p>Two ExnTypes match if they have the same number of fields and each field type matches.
   *
   * @param other the other exception type
   * @return true if the types match
   */
  public boolean matches(final ExnType other) {
    if (other == null) {
      return false;
    }
    return fields.equals(other.fields);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ExnType exnType = (ExnType) obj;
    return Objects.equals(fields, exnType.fields);
  }

  @Override
  public int hashCode() {
    return fields.hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("exn(");
    for (int i = 0; i < fields.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(fields.get(i).name().toLowerCase(java.util.Locale.ROOT));
    }
    sb.append(')');
    return sb.toString();
  }
}
