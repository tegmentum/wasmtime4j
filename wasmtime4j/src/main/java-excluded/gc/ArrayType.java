package ai.tegmentum.wasmtime4j.gc;

import java.util.Objects;

/**
 * WebAssembly GC array type definition.
 *
 * <p>Represents an array type with a uniform element type and mutability characteristics. Array
 * types define homogeneous collections of elements in WebAssembly GC.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Mutable array of 32-bit integers
 * ArrayType intArray = ArrayType.builder("IntArray")
 *     .elementType(FieldType.i32())
 *     .mutable(true)
 *     .build();
 *
 * // Immutable array of struct references
 * ArrayType pointArray = ArrayType.builder("PointArray")
 *     .elementType(FieldType.structRef())
 *     .mutable(false)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class ArrayType {
  private final String name;
  private final FieldType elementType;
  private final boolean mutable;
  private final int typeId;

  private ArrayType(
      final String name, final FieldType elementType, final boolean mutable, final int typeId) {
    this.name = name;
    this.elementType = elementType;
    this.mutable = mutable;
    this.typeId = typeId;
  }

  /**
   * Create a new array type builder.
   *
   * @param name the array type name
   * @return a new builder
   */
  public static Builder builder(final String name) {
    return new Builder(name);
  }

  /**
   * Create a simple array type with default settings.
   *
   * @param name the array type name
   * @param elementType the element type
   * @return a new array type
   */
  public static ArrayType of(final String name, final FieldType elementType) {
    return builder(name).elementType(elementType).build();
  }

  /**
   * Gets the array type name.
   *
   * @return the array type name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the element type.
   *
   * @return the element type
   */
  public FieldType getElementType() {
    return elementType;
  }

  /**
   * Checks if the array is mutable.
   *
   * @return true if the array is mutable
   */
  public boolean isMutable() {
    return mutable;
  }

  /**
   * Gets the type ID assigned by the runtime.
   *
   * @return the type ID
   */
  public int getTypeId() {
    return typeId;
  }

  /**
   * Gets the size of each element in bytes.
   *
   * @return the element size in bytes
   */
  public int getElementSizeBytes() {
    return elementType.getSizeBytes();
  }

  /**
   * Calculates the size of an array with the given length.
   *
   * @param length the array length
   * @return the total array size in bytes
   */
  public long getArraySizeBytes(final int length) {
    if (length < 0) {
      throw new IllegalArgumentException("Array length cannot be negative");
    }
    return (long) length * getElementSizeBytes();
  }

  /**
   * Checks if this array type is a subtype of another.
   *
   * <p>Array subtyping follows these rules:
   *
   * <ul>
   *   <li>Element types must be compatible
   *   <li>Immutable arrays are subtypes of mutable arrays (covariance)
   *   <li>Mutable arrays are not subtypes of immutable arrays
   * </ul>
   *
   * @param other the potential supertype
   * @return true if this is a subtype of other
   */
  public boolean isSubtypeOf(final ArrayType other) {
    if (this.equals(other)) {
      return true;
    }

    // Element types must be compatible
    if (!this.elementType.isCompatibleWith(other.elementType)) {
      return false;
    }

    // Mutability compatibility: immutable arrays are subtypes of mutable arrays
    return !this.mutable || other.mutable;
  }

  /**
   * Checks if this array type is compatible for element assignment.
   *
   * @param elementValue the element type to check
   * @return true if the element can be assigned to this array
   */
  public boolean canAssignElement(final FieldType elementValue) {
    return elementValue.isCompatibleWith(this.elementType);
  }

  /**
   * Validates that this array type definition is correct.
   *
   * @throws IllegalStateException if the array type is invalid
   */
  public void validate() {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalStateException("Array type name cannot be null or empty");
    }

    if (elementType == null) {
      throw new IllegalStateException("Element type cannot be null");
    }

    // Additional validation for specific element types could be added here
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ArrayType arrayType = (ArrayType) obj;
    return typeId == arrayType.typeId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(typeId);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("array ").append(name).append(" {");
    sb.append(elementType);
    if (mutable) {
      sb.append(" mut");
    }
    sb.append("}");
    return sb.toString();
  }

  /** Builder for creating array types. */
  public static final class Builder {
    private final String name;
    private FieldType elementType;
    private boolean mutable = true; // Default to mutable

    private Builder(final String name) {
      this.name = Objects.requireNonNull(name, "Array name cannot be null");
    }

    /**
     * Set the element type.
     *
     * @param elementType the element type
     * @return this builder
     */
    public Builder elementType(final FieldType elementType) {
      this.elementType = Objects.requireNonNull(elementType, "Element type cannot be null");
      return this;
    }

    /**
     * Set the mutability.
     *
     * @param mutable whether the array is mutable
     * @return this builder
     */
    public Builder mutable(final boolean mutable) {
      this.mutable = mutable;
      return this;
    }

    /**
     * Make the array immutable.
     *
     * @return this builder
     */
    public Builder immutable() {
      return mutable(false);
    }

    /**
     * Build the array type.
     *
     * @return the array type
     * @throws IllegalStateException if the array definition is invalid
     */
    public ArrayType build() {
      if (elementType == null) {
        throw new IllegalStateException("Element type must be specified");
      }

      final ArrayType arrayType = new ArrayType(name, elementType, mutable, 0);
      arrayType.validate();
      return arrayType;
    }
  }
}
