package ai.tegmentum.wasmtime4j.component;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model record field definitions.
 *
 * <p>ComponentField represents a field within a record type, including its name, type,
 * optionality, and metadata. Fields define the structure and constraints for record data.
 *
 * @since 1.0.0
 */
public interface ComponentField {

  /**
   * Gets the name of this field.
   *
   * @return the field name
   */
  String getName();

  /**
   * Gets the type of this field.
   *
   * @return the field type
   */
  ComponentValueType getType();

  /**
   * Checks if this field is optional.
   *
   * <p>Optional fields can be omitted when creating record values.
   *
   * @return true if the field is optional, false otherwise
   */
  boolean isOptional();

  /**
   * Gets the default value for this field if it has one.
   *
   * @return the default value, or empty if no default is specified
   */
  Optional<Object> getDefaultValue();

  /**
   * Gets documentation for this field.
   *
   * @return field documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Gets additional attributes/annotations for this field.
   *
   * @return field attributes
   */
  Map<String, Object> getAttributes();

  /**
   * Validates a value against this field's type and constraints.
   *
   * @param value the value to validate
   * @return true if the value is valid for this field, false otherwise
   */
  boolean isValidValue(final Object value);

  /**
   * Checks if this field is compatible with another field definition.
   *
   * @param other the field to check compatibility with
   * @return true if fields are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final ComponentField other);
}