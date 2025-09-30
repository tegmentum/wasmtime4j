package ai.tegmentum.wasmtime4j.gc;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.List;
import java.util.Objects;
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
public final class StructInstance implements GcObject {
  private final long objectId;
  private final StructType structType;
  private final GcRuntime runtime;

  /**
   * Create a new struct instance.
   *
   * @param objectId the object ID
   * @param structType the struct type
   * @param runtime the GC runtime
   */
  public StructInstance(final long objectId, final StructType structType, final GcRuntime runtime) {
    this.objectId = objectId;
    this.structType = Objects.requireNonNull(structType, "Struct type cannot be null");
    this.runtime = Objects.requireNonNull(runtime, "Runtime cannot be null");
  }

  @Override
  public long getObjectId() {
    return objectId;
  }

  @Override
  public GcReferenceType getReferenceType() {
    return GcReferenceType.STRUCT_REF;
  }

  @Override
  public boolean isNull() {
    return false; // Struct instances are never null
  }

  @Override
  public boolean isOfType(final GcReferenceType type) {
    return type.equals(GcReferenceType.STRUCT_REF)
        || type.equals(GcReferenceType.EQ_REF)
        || type.equals(GcReferenceType.ANY_REF);
  }

  @Override
  public GcObject castTo(final GcReferenceType type) {
    if (!isOfType(type)) {
      throw new ClassCastException("Cannot cast struct to " + type);
    }
    return this;
  }

  @Override
  public boolean refEquals(final GcObject other) {
    return other != null && this.objectId == other.getObjectId();
  }

  @Override
  public int getSizeBytes() {
    return structType.getSizeBytes();
  }

  @Override
  public WasmValue toWasmValue() {
    return WasmValue.externRef(this);
  }

  /**
   * Gets the struct type of this instance.
   *
   * @return the struct type
   */
  public StructType getStructType() {
    return structType;
  }

  /**
   * Gets the number of fields in this struct.
   *
   * @return the field count
   */
  public int getFieldCount() {
    return structType.getFieldCount();
  }

  /**
   * Gets a field value by index.
   *
   * @param index the field index
   * @return the field value
   * @throws IndexOutOfBoundsException if index is invalid
   * @throws GcException if field access fails
   */
  public GcValue getField(final int index) {
    if (index < 0 || index >= structType.getFieldCount()) {
      throw new IndexOutOfBoundsException("Field index " + index + " out of bounds");
    }

    return runtime.getStructField(this, index);
  }

  /**
   * Gets a field value by name.
   *
   * @param name the field name
   * @return the field value
   * @throws IllegalArgumentException if field name is not found
   * @throws GcException if field access fails
   */
  public GcValue getField(final String name) {
    final int index = structType.getFieldIndex(name);
    if (index == -1) {
      throw new IllegalArgumentException(
          "Field '" + name + "' not found in struct " + structType.getName());
    }
    return getField(index);
  }

  /**
   * Sets a field value by index.
   *
   * @param index the field index
   * @param value the new value
   * @return this instance for method chaining
   * @throws IndexOutOfBoundsException if index is invalid
   * @throws IllegalArgumentException if field is immutable or value type is incompatible
   * @throws GcException if field assignment fails
   */
  public StructInstance setField(final int index, final GcValue value) {
    if (index < 0 || index >= structType.getFieldCount()) {
      throw new IndexOutOfBoundsException("Field index " + index + " out of bounds");
    }

    final FieldDefinition field = structType.getField(index);
    if (!field.isMutable()) {
      throw new IllegalArgumentException("Field " + index + " is immutable");
    }

    runtime.setStructField(this, index, value);
    return this;
  }

  /**
   * Sets a field value by name.
   *
   * @param name the field name
   * @param value the new value
   * @return this instance for method chaining
   * @throws IllegalArgumentException if field name is not found, field is immutable, or value type
   *     is incompatible
   * @throws GcException if field assignment fails
   */
  public StructInstance setField(final String name, final GcValue value) {
    final int index = structType.getFieldIndex(name);
    if (index == -1) {
      throw new IllegalArgumentException(
          "Field '" + name + "' not found in struct " + structType.getName());
    }
    return setField(index, value);
  }

  /**
   * Sets a field value by index with automatic type conversion.
   *
   * @param index the field index
   * @param value the new value (will be converted to appropriate GcValue)
   * @return this instance for method chaining
   */
  public StructInstance setField(final int index, final Object value) {
    return setField(index, GcValue.fromObject(value));
  }

  /**
   * Sets a field value by name with automatic type conversion.
   *
   * @param name the field name
   * @param value the new value (will be converted to appropriate GcValue)
   * @return this instance for method chaining
   */
  public StructInstance setField(final String name, final Object value) {
    return setField(name, GcValue.fromObject(value));
  }

  /**
   * Gets all field values.
   *
   * @return list of all field values
   */
  public List<GcValue> getAllFields() {
    final var fields = new java.util.ArrayList<GcValue>();
    for (int i = 0; i < getFieldCount(); i++) {
      fields.add(getField(i));
    }
    return fields;
  }

  /**
   * Checks if this struct is an instance of the specified struct type.
   *
   * @param type the struct type to check
   * @return true if this struct is an instance of the type
   */
  public boolean isInstanceOf(final StructType type) {
    return structType.isSubtypeOf(type);
  }

  /**
   * Gets the field definition for a given index.
   *
   * @param index the field index
   * @return the field definition
   */
  public FieldDefinition getFieldDefinition(final int index) {
    return structType.getField(index);
  }

  /**
   * Gets the field definition for a given name.
   *
   * @param name the field name
   * @return the field definition, or empty if not found
   */
  public Optional<FieldDefinition> getFieldDefinition(final String name) {
    return structType.getField(name);
  }

  /**
   * Creates a shallow copy of this struct with the same field values.
   *
   * @return a new struct instance with copied values
   */
  public StructInstance copy() {
    return runtime.createStruct(structType, getAllFields());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final StructInstance that = (StructInstance) obj;
    return objectId == that.objectId;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(objectId);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(structType.getName()).append(" {");

    for (int i = 0; i < getFieldCount(); i++) {
      if (i > 0) {
        sb.append(", ");
      }

      final FieldDefinition fieldDef = getFieldDefinition(i);
      if (fieldDef.hasName()) {
        sb.append(fieldDef.getName()).append(": ");
      }

      try {
        sb.append(getField(i));
      } catch (final Exception e) {
        sb.append("<error>");
      }
    }

    sb.append("}");
    return sb.toString();
  }
}
