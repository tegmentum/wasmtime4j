package ai.tegmentum.wasmtime4j.component;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model interface function parameters.
 *
 * <p>InterfaceParameter represents a parameter definition within an interface function, including
 * its name, type, optional default value, and documentation. Parameters define the input contract
 * for component interface functions.
 *
 * @since 1.0.0
 */
public interface InterfaceParameter {

  /**
   * Gets the name of this parameter.
   *
   * @return the parameter name
   */
  String getName();

  /**
   * Gets the type of this parameter.
   *
   * <p>Returns the component value type that defines what kind of data this parameter accepts.
   *
   * @return the parameter type
   */
  ComponentValueType getType();

  /**
   * Gets documentation for this parameter.
   *
   * @return parameter documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Checks if this parameter is optional.
   *
   * <p>Returns true if this parameter has a default value or can be omitted when calling the
   * function.
   *
   * @return true if the parameter is optional, false otherwise
   */
  boolean isOptional();

  /**
   * Gets the default value for this parameter if it has one.
   *
   * @return the default value, or empty if no default is specified
   */
  Optional<Object> getDefaultValue();

  /**
   * Checks if this parameter accepts variable arguments.
   *
   * <p>Returns true if this parameter can accept multiple values (variadic parameter).
   *
   * @return true if this is a variadic parameter, false otherwise
   */
  boolean isVariadic();

  /**
   * Gets validation constraints for this parameter.
   *
   * <p>Returns constraints such as value ranges, string patterns, or custom validation rules that
   * apply to this parameter.
   *
   * @return parameter constraints
   */
  ParameterConstraints getConstraints();

  /**
   * Gets additional attributes/annotations for this parameter.
   *
   * <p>Returns metadata such as performance hints, validation rules, and tool-specific annotations.
   *
   * @return parameter attributes
   */
  Map<String, Object> getAttributes();

  /**
   * Validates a value against this parameter's type and constraints.
   *
   * <p>Checks if the provided value is compatible with this parameter's type and satisfies any
   * defined constraints.
   *
   * @param value the value to validate
   * @return true if the value is valid for this parameter, false otherwise
   */
  boolean isValidValue(final Object value);

  /**
   * Validates this parameter definition for correctness.
   *
   * <p>Validation includes type checking, constraint consistency, and default value validation.
   *
   * @throws IllegalArgumentException if validation fails with details about specific issues
   */
  void validate();

  /**
   * Checks if this parameter is compatible with another parameter definition.
   *
   * <p>Compatibility checking considers type compatibility, optionality, and constraints.
   *
   * @param other the parameter to check compatibility with
   * @return true if parameters are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final InterfaceParameter other);
}
