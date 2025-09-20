package ai.tegmentum.wasmtime4j.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model record types.
 *
 * <p>ComponentRecord represents a structured data type with named fields, similar to structs in
 * other programming languages. Records enable complex data composition and type-safe data exchange
 * between components.
 *
 * <p>Records support nested types, optional fields, and comprehensive type validation according to
 * the Component Model specification.
 *
 * <p>Example record definition:
 *
 * <pre>{@code
 * record person {
 *   name: string,
 *   age: u32,
 *   email: option<string>
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentRecord {

  /**
   * Gets the name of this record type.
   *
   * @return the record type name, or empty if anonymous
   */
  Optional<String> getName();

  /**
   * Gets all fields defined in this record.
   *
   * <p>Returns an ordered list of fields including their names, types, and metadata.
   *
   * @return list of record fields
   */
  List<ComponentField> getFields();

  /**
   * Gets a specific field by name.
   *
   * @param name the field name to look up
   * @return the field definition, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentField> getField(final String name);

  /**
   * Gets the names of all fields in declaration order.
   *
   * @return list of field names
   */
  List<String> getFieldNames();

  /**
   * Checks if a field with the specified name exists.
   *
   * @param name the field name to check
   * @return true if the field exists, false otherwise
   * @throws IllegalArgumentException if name is null or empty
   */
  boolean hasField(final String name);

  /**
   * Gets the type of a specific field.
   *
   * @param name the field name
   * @return the field type, or empty if field doesn't exist
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentValueType> getFieldType(final String name);

  /**
   * Gets the number of fields in this record.
   *
   * @return the field count
   */
  int getFieldCount();

  /**
   * Checks if this record has any optional fields.
   *
   * @return true if any fields are optional, false otherwise
   */
  boolean hasOptionalFields();

  /**
   * Gets all required (non-optional) fields.
   *
   * @return list of required fields
   */
  List<ComponentField> getRequiredFields();

  /**
   * Gets all optional fields.
   *
   * @return list of optional fields
   */
  List<ComponentField> getOptionalFields();

  /**
   * Creates a new record value with the specified field values.
   *
   * <p>Validates that all required fields are provided and that field values match their declared
   * types.
   *
   * @param fieldValues map of field names to their values
   * @return a new record value
   * @throws IllegalArgumentException if required fields are missing or values are invalid
   */
  ComponentRecordValue createValue(final Map<String, Object> fieldValues);

  /**
   * Validates a map of field values against this record definition.
   *
   * <p>Checks that all required fields are present and that all values are compatible with their
   * declared types.
   *
   * @param fieldValues the field values to validate
   * @return true if the values are valid for this record, false otherwise
   * @throws IllegalArgumentException if fieldValues is null
   */
  boolean isValidValue(final Map<String, Object> fieldValues);

  /**
   * Gets documentation for this record type.
   *
   * @return record documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Gets additional attributes/annotations for this record.
   *
   * <p>Returns metadata such as serialization hints, validation rules, and tool-specific
   * annotations.
   *
   * @return record attributes
   */
  Map<String, Object> getAttributes();

  /**
   * Validates this record definition for correctness.
   *
   * <p>Validation includes field type checking, name uniqueness, and structural consistency.
   *
   * @throws IllegalArgumentException if validation fails with details about specific issues
   */
  void validate();

  /**
   * Checks if this record is compatible with another record definition.
   *
   * <p>Compatibility checking considers field names, types, optionality, and structural
   * equivalence.
   *
   * @param other the record to check compatibility with
   * @return true if records are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final ComponentRecord other);

  /**
   * Gets metadata about this record type.
   *
   * <p>Returns information about size, complexity, and performance characteristics.
   *
   * @return record metadata
   */
  ComponentRecordMetadata getMetadata();
}
