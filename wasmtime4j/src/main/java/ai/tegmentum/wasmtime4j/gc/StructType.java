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

import ai.tegmentum.wasmtime4j.Engine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * WebAssembly GC struct type definition.
 *
 * <p>Represents a struct type with named fields, supporting type validation, subtyping, and field
 * access patterns. Struct types define the layout and types of structured data in WebAssembly GC.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * StructType pointType = StructType.builder("Point")
 *     .addField("x", FieldType.f64(), true)
 *     .addField("y", FieldType.f64(), true)
 *     .build();
 *
 * StructType point3DType = StructType.builder("Point3D")
 *     .extend(pointType)
 *     .addField("z", FieldType.f64(), true)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class StructType {
  private final String name;
  private final List<FieldDefinition> fields;
  private final StructType supertype;
  private final int typeId;
  private final Finality finality;
  private final Engine engine;

  private StructType(
      final String name,
      final List<FieldDefinition> fields,
      final StructType supertype,
      final int typeId,
      final Finality finality,
      final Engine engine) {
    this.name = name;
    this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
    this.supertype = supertype;
    this.typeId = typeId;
    this.finality = finality;
    this.engine = engine;
  }

  /**
   * Create a new struct type builder.
   *
   * @param name the struct type name
   * @return a new builder
   */
  public static Builder builder(final String name) {
    return new Builder(name);
  }

  /**
   * Gets the struct type name.
   *
   * @return the struct type name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the field definitions.
   *
   * @return an unmodifiable list of field definitions
   */
  public List<FieldDefinition> getFields() {
    return fields;
  }

  /**
   * Gets the supertype if this struct extends another.
   *
   * @return the supertype, or empty if no supertype
   */
  public Optional<StructType> getSupertype() {
    return Optional.ofNullable(supertype);
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
   * Gets the finality of this struct type.
   *
   * <p>A final struct type cannot be subtyped. Non-final types may have subtypes.
   *
   * @return the finality (defaults to {@link Finality#FINAL})
   * @since 1.1.0
   */
  public Finality getFinality() {
    return finality;
  }

  /**
   * Gets the engine this struct type was created with, if any.
   *
   * <p>Returns empty if the type was built without an engine reference.
   *
   * @return the engine, or empty if not bound to an engine
   * @since 1.1.0
   */
  public Optional<Engine> getEngine() {
    return Optional.ofNullable(engine);
  }

  /**
   * Gets the number of fields in this struct.
   *
   * @return the field count
   */
  public int getFieldCount() {
    return fields.size();
  }

  /**
   * Gets a field definition by index.
   *
   * @param index the field index
   * @return the field definition
   * @throws IndexOutOfBoundsException if index is invalid
   */
  public FieldDefinition getField(final int index) {
    return fields.get(index);
  }

  /**
   * Gets a field definition by name.
   *
   * @param name the field name
   * @return the field definition, or empty if not found
   */
  public Optional<FieldDefinition> getField(final String name) {
    return fields.stream().filter(field -> Objects.equals(field.getName(), name)).findFirst();
  }

  /**
   * Gets the index of a field by name.
   *
   * @param name the field name
   * @return the field index, or -1 if not found
   */
  public int getFieldIndex(final String name) {
    for (int i = 0; i < fields.size(); i++) {
      if (Objects.equals(fields.get(i).getName(), name)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Checks if this struct type is a subtype of another.
   *
   * @param other the potential supertype
   * @return true if this is a subtype of other
   */
  public boolean isSubtypeOf(final StructType other) {
    if (this.equals(other)) {
      return true;
    }

    // Check direct inheritance
    if (supertype != null && supertype.isSubtypeOf(other)) {
      return true;
    }

    // Check structural subtyping
    return isStructuralSubtypeOf(other);
  }

  /**
   * Validates that this struct type has all required fields for compatibility.
   *
   * @throws IllegalStateException if the struct type is invalid
   */
  public void validate() {
    // Check for duplicate field names
    final java.util.HashSet<String> fieldNames = new java.util.HashSet<String>();
    for (final FieldDefinition field : fields) {
      if (field.getName() != null && !fieldNames.add(field.getName())) {
        throw new IllegalStateException("Duplicate field name: " + field.getName());
      }
    }

    // Validate field indices
    for (int i = 0; i < fields.size(); i++) {
      if (fields.get(i).getIndex() != i) {
        throw new IllegalStateException(
            "Field index mismatch at position "
                + i
                + ": expected "
                + i
                + ", got "
                + fields.get(i).getIndex());
      }
    }

    // Validate supertype compatibility
    if (supertype != null) {
      validateSupertypeCompatibility();
    }
  }

  /**
   * Calculates the size of this struct in bytes.
   *
   * @return the struct size in bytes
   */
  public int getSizeBytes() {
    int size = 0;
    for (final FieldDefinition field : fields) {
      size += field.getFieldType().getSizeBytes();
    }
    return size;
  }

  private boolean isStructuralSubtypeOf(final StructType other) {
    // Structural subtyping: this must have at least all fields of other
    if (this.fields.size() < other.fields.size()) {
      return false;
    }

    // Check that all fields of other are compatible in this
    for (int i = 0; i < other.fields.size(); i++) {
      final FieldDefinition otherField = other.fields.get(i);
      final FieldDefinition thisField = this.fields.get(i);

      // Field types must be compatible
      if (!thisField.getFieldType().isCompatibleWith(otherField.getFieldType())) {
        return false;
      }

      // Mutability must be compatible (mutable can substitute for immutable, not vice versa)
      if (otherField.isMutable() && !thisField.isMutable()) {
        return false;
      }
    }

    return true;
  }

  private void validateSupertypeCompatibility() {
    // This struct must be a structural subtype of its declared supertype
    if (!isStructuralSubtypeOf(supertype)) {
      throw new IllegalStateException(
          "Struct type " + name + " is not compatible with supertype " + supertype.name);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final StructType that = (StructType) obj;

    // When both have runtime-assigned type IDs (non-zero), use them as tiebreaker
    if (typeId != 0 && that.typeId != 0) {
      return typeId == that.typeId;
    }

    // Structural comparison for builder-created types (typeId=0)
    return Objects.equals(name, that.name)
        && Objects.equals(fields, that.fields)
        && finality == that.finality;
  }

  @Override
  public int hashCode() {
    if (typeId != 0) {
      return Objects.hash(typeId);
    }
    return Objects.hash(name, fields, finality);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("struct ").append(name);

    if (supertype != null) {
      sb.append(" extends ").append(supertype.name);
    }

    sb.append(" {");
    for (int i = 0; i < fields.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(fields.get(i));
    }
    sb.append("}");

    return sb.toString();
  }

  /** Builder for creating struct types. */
  public static final class Builder {
    private final String name;
    private final List<FieldDefinition> fields = new ArrayList<>();
    private StructType supertype;
    private Finality finality = Finality.FINAL;
    private Engine engine;

    private Builder(final String name) {
      this.name = Objects.requireNonNull(name, "Struct name cannot be null");
    }

    /**
     * Add a field to the struct.
     *
     * @param name the field name (can be null for unnamed fields)
     * @param fieldType the field type
     * @param mutable whether the field is mutable
     * @return this builder
     */
    public Builder addField(final String name, final FieldType fieldType, final boolean mutable) {
      Objects.requireNonNull(fieldType, "Field type cannot be null");
      final int index = fields.size();
      fields.add(new FieldDefinition(name, fieldType, mutable, index));
      return this;
    }

    /**
     * Add an immutable field to the struct.
     *
     * @param name the field name
     * @param fieldType the field type
     * @return this builder
     */
    public Builder addField(final String name, final FieldType fieldType) {
      return addField(name, fieldType, false);
    }

    /**
     * Add an unnamed field to the struct.
     *
     * @param fieldType the field type
     * @param mutable whether the field is mutable
     * @return this builder
     */
    public Builder addField(final FieldType fieldType, final boolean mutable) {
      return addField(null, fieldType, mutable);
    }

    /**
     * Add an immutable unnamed field to the struct.
     *
     * @param fieldType the field type
     * @return this builder
     */
    public Builder addField(final FieldType fieldType) {
      return addField(null, fieldType, false);
    }

    /**
     * Set the supertype for inheritance.
     *
     * @param supertype the supertype
     * @return this builder
     */
    public Builder extend(final StructType supertype) {
      this.supertype = supertype;
      return this;
    }

    /**
     * Set the finality of this struct type.
     *
     * <p>Defaults to {@link Finality#FINAL}.
     *
     * @param finality the finality
     * @return this builder
     * @since 1.1.0
     */
    public Builder withFinality(final Finality finality) {
      this.finality = Objects.requireNonNull(finality, "Finality cannot be null");
      return this;
    }

    /**
     * Set the engine for this struct type.
     *
     * @param engine the engine
     * @return this builder
     * @since 1.1.0
     */
    public Builder withEngine(final Engine engine) {
      this.engine = Objects.requireNonNull(engine, "Engine cannot be null");
      return this;
    }

    /**
     * Build the struct type.
     *
     * @return the struct type
     * @throws IllegalStateException if the struct definition is invalid
     */
    public StructType build() {
      if (fields.isEmpty()) {
        throw new IllegalStateException("Struct must have at least one field");
      }

      final StructType structType = new StructType(name, fields, supertype, 0, finality, engine);
      structType.validate();
      return structType;
    }
  }
}
