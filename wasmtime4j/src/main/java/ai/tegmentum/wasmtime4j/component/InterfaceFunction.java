package ai.tegmentum.wasmtime4j.component;

import java.util.List;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model interface function definitions.
 *
 * <p>InterfaceFunction represents a function definition within a WIT interface, including its name,
 * parameter types, return type, and other metadata. Interface functions define the callable
 * operations available within component interfaces.
 *
 * <p>Function definitions support complex parameter and return types including structured data,
 * resources, and interface-specific types.
 *
 * @since 1.0.0
 */
public interface InterfaceFunction {

  /**
   * Gets the name of this function.
   *
   * @return the function name
   */
  String getName();

  /**
   * Gets the parameter definitions for this function.
   *
   * <p>Returns an ordered list of parameters including their names, types, and optional
   * documentation.
   *
   * @return list of function parameters
   */
  List<InterfaceParameter> getParameters();

  /**
   * Gets a specific parameter by name.
   *
   * @param name the parameter name to look up
   * @return the parameter definition, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<InterfaceParameter> getParameter(final String name);

  /**
   * Gets the return type of this function.
   *
   * <p>Returns the type information for the function's return value, or empty if the function
   * returns void.
   *
   * @return the return type, or empty if void
   */
  Optional<ComponentValueType> getReturnType();

  /**
   * Gets the error type for this function if it can fail.
   *
   * <p>Returns the type of error that this function can return if it uses result types for error
   * handling.
   *
   * @return the error type, or empty if function cannot fail
   */
  Optional<ComponentValueType> getErrorType();

  /**
   * Gets documentation for this function.
   *
   * @return function documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Checks if this function is async/suspendable.
   *
   * <p>Returns true if this function can be suspended and resumed, supporting asynchronous
   * execution patterns.
   *
   * @return true if the function is async, false otherwise
   */
  boolean isAsync();

  /**
   * Checks if this function is a resource method.
   *
   * <p>Returns true if this function is defined as a method on a resource type rather than a
   * standalone interface function.
   *
   * @return true if this is a resource method, false otherwise
   */
  boolean isResourceMethod();

  /**
   * Gets the resource type if this is a resource method.
   *
   * @return the resource type this method belongs to, or empty if not a resource method
   */
  Optional<InterfaceResource> getResourceType();

  /**
   * Checks if this function is a constructor.
   *
   * <p>Returns true if this function creates and returns a new instance of a resource type.
   *
   * @return true if this is a constructor function, false otherwise
   */
  boolean isConstructor();

  /**
   * Checks if this function is static.
   *
   * <p>Returns true if this function does not require a resource instance and can be called
   * directly on the resource type.
   *
   * @return true if this is a static function, false otherwise
   */
  boolean isStatic();

  /**
   * Gets the function's visibility/accessibility.
   *
   * @return the function visibility level
   */
  InterfaceFunctionVisibility getVisibility();

  /**
   * Gets additional attributes/annotations for this function.
   *
   * <p>Returns metadata such as performance hints, deprecation warnings, and tool-specific
   * annotations.
   *
   * @return function attributes
   */
  java.util.Map<String, Object> getAttributes();

  /**
   * Validates this function definition for correctness.
   *
   * <p>Validation includes parameter type checking, return type validation, and attribute
   * consistency.
   *
   * @throws IllegalArgumentException if validation fails with details about specific issues
   */
  void validate();

  /**
   * Checks if this function is compatible with another function definition.
   *
   * <p>Compatibility checking considers parameter types, return types, and calling conventions.
   *
   * @param other the function to check compatibility with
   * @return true if functions are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final InterfaceFunction other);

  /**
   * Gets metadata about this function.
   *
   * <p>Returns information about complexity, performance characteristics, and usage patterns.
   *
   * @return function metadata
   */
  InterfaceFunctionMetadata getMetadata();
}
