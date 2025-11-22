/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wit;

import ai.tegmentum.wasmtime4j.WitType;
import ai.tegmentum.wasmtime4j.exception.WitValueException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a WIT record value with named fields.
 *
 * <p>Records are composite types with named fields, similar to structs in other languages. Each
 * field has a name and a WIT value. Field order is preserved using LinkedHashMap.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WitRecord person = WitRecord.builder()
 *     .field("name", WitString.of("Alice"))
 *     .field("age", WitS32.of(30))
 *     .field("email", WitString.of("alice@example.com"))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WitRecord extends WitValue {

  private final Map<String, WitValue> fields;

  /**
   * Creates a new WIT record value.
   *
   * @param fields the record fields (must not be null or empty)
   */
  private WitRecord(final Map<String, WitValue> fields) {
    super(createRecordType(fields));
    if (fields == null || fields.isEmpty()) {
      throw new IllegalArgumentException("Record fields cannot be null or empty");
    }
    // Defensive copy and validate all fields
    this.fields = new LinkedHashMap<>(fields);
    validate();
  }

  /**
   * Creates a WIT type for this record based on its fields.
   *
   * @param fields the record fields
   * @return a WIT type representing this record
   */
  private static WitType createRecordType(final Map<String, WitValue> fields) {
    // Build field types map
    final Map<String, WitType> fieldTypes = new LinkedHashMap<>();
    for (final Map.Entry<String, WitValue> entry : fields.entrySet()) {
      fieldTypes.put(entry.getKey(), entry.getValue().getType());
    }
    return WitType.record("record", fieldTypes);
  }

  /**
   * Creates a WIT record from a map of field names to values.
   *
   * @param fields the record fields
   * @return a WIT record value
   * @throws IllegalArgumentException if fields is null or empty
   */
  public static WitRecord of(final Map<String, WitValue> fields) {
    return new WitRecord(fields);
  }

  /**
   * Creates a builder for constructing WIT records.
   *
   * @return a new record builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the value of a field by name.
   *
   * @param fieldName the field name
   * @return the field value, or null if field doesn't exist
   */
  public WitValue getField(final String fieldName) {
    return fields.get(fieldName);
  }

  /**
   * Gets all fields in this record.
   *
   * @return an unmodifiable map of field names to values
   */
  public Map<String, WitValue> getFields() {
    return Collections.unmodifiableMap(fields);
  }

  /**
   * Gets the number of fields in this record.
   *
   * @return the field count
   */
  public int getFieldCount() {
    return fields.size();
  }

  /**
   * Checks if this record has a field with the given name.
   *
   * @param fieldName the field name to check
   * @return true if the field exists
   */
  public boolean hasField(final String fieldName) {
    return fields.containsKey(fieldName);
  }

  @Override
  public Map<String, Object> toJava() {
    final Map<String, Object> result = new LinkedHashMap<>(fields.size());
    for (final Map.Entry<String, WitValue> entry : fields.entrySet()) {
      result.put(entry.getKey(), entry.getValue().toJava());
    }
    return result;
  }

  @Override
  protected void validate() {
    // Validate that all fields are non-null
    for (final Map.Entry<String, WitValue> entry : fields.entrySet()) {
      if (entry.getKey() == null || entry.getKey().isEmpty()) {
        throw new IllegalArgumentException("Record field name cannot be null or empty");
      }
      if (entry.getValue() == null) {
        throw new IllegalArgumentException(
            "Record field value cannot be null for field: " + entry.getKey());
      }
    }
  }

  @Override
  public String toString() {
    return "WitRecord" + fields;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitRecord)) {
      return false;
    }
    final WitRecord other = (WitRecord) obj;
    return fields.equals(other.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), fields);
  }

  /** Builder for constructing WIT records with a fluent API. */
  public static final class Builder {
    private final Map<String, WitValue> fields = new LinkedHashMap<>();

    private Builder() {}

    /**
     * Adds a field to the record.
     *
     * @param name the field name (must not be null or empty)
     * @param value the field value (must not be null)
     * @return this builder
     * @throws IllegalArgumentException if name or value is invalid
     */
    public Builder field(final String name, final WitValue value) {
      if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException("Field name cannot be null or empty");
      }
      if (value == null) {
        throw new IllegalArgumentException("Field value cannot be null");
      }
      fields.put(name, value);
      return this;
    }

    /**
     * Builds the WIT record.
     *
     * @return the constructed WIT record
     * @throws IllegalArgumentException if no fields have been added
     */
    public WitRecord build() {
      return new WitRecord(fields);
    }
  }
}
