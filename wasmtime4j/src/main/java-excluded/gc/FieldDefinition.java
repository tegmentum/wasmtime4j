package ai.tegmentum.wasmtime4j.gc;

import java.util.Objects;

/**
 * Definition of a field in a WebAssembly GC struct.
 *
 * <p>Represents a single field with its name, type, mutability, and position within the struct.
 * Fields can be named or unnamed, and can have different mutability characteristics.
 *
 * @since 1.0.0
 */
public final class FieldDefinition {
  private final String name;
  private final FieldType fieldType;
  private final boolean mutable;
  private final int index;

  /**
   * Create a new field definition.
   *
   * @param name the field name (can be null for unnamed fields)
   * @param fieldType the field type
   * @param mutable whether the field is mutable
   * @param index the field index within the struct
   */
  public FieldDefinition(
      final String name, final FieldType fieldType, final boolean mutable, final int index) {
    this.name = name;
    this.fieldType = Objects.requireNonNull(fieldType, "Field type cannot be null");
    this.mutable = mutable;
    this.index = index;

    if (index < 0) {
      throw new IllegalArgumentException("Field index cannot be negative");
    }
  }

  /**
   * Gets the field name.
   *
   * @return the field name, or null if unnamed
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the field type.
   *
   * @return the field type
   */
  public FieldType getFieldType() {
    return fieldType;
  }

  /**
   * Checks if the field is mutable.
   *
   * @return true if the field is mutable
   */
  public boolean isMutable() {
    return mutable;
  }

  /**
   * Gets the field index within the struct.
   *
   * @return the field index
   */
  public int getIndex() {
    return index;
  }

  /**
   * Checks if this field has a name.
   *
   * @return true if the field has a name
   */
  public boolean hasName() {
    return name != null;
  }

  /**
   * Gets the size of this field in bytes.
   *
   * @return the field size in bytes
   */
  public int getSizeBytes() {
    return fieldType.getSizeBytes();
  }

  /**
   * Checks if this field definition is compatible with another for subtyping.
   *
   * @param other the other field definition
   * @return true if compatible for subtyping
   */
  public boolean isCompatibleWith(final FieldDefinition other) {
    // Field types must be compatible
    if (!fieldType.isCompatibleWith(other.fieldType)) {
      return false;
    }

    // Mutability compatibility: mutable can substitute for immutable, not vice versa
    return !other.mutable || this.mutable;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final FieldDefinition that = (FieldDefinition) obj;
    return mutable == that.mutable
        && index == that.index
        && Objects.equals(name, that.name)
        && Objects.equals(fieldType, that.fieldType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, fieldType, mutable, index);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    if (name != null) {
      sb.append(name).append(": ");
    }

    sb.append(fieldType);

    if (mutable) {
      sb.append(" mut");
    }

    return sb.toString();
  }
}
