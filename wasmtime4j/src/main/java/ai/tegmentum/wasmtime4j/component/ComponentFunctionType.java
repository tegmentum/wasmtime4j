package ai.tegmentum.wasmtime4j.component;

import java.util.List;
import java.util.Optional;

/**
 * Type information for WebAssembly component functions.
 *
 * <p>ComponentFunctionType describes the signature and calling conventions of component functions,
 * including parameter types, return types, and component-model-specific type information. This
 * extends beyond basic WebAssembly function types to include structured data and interface types.
 *
 * @since 1.0.0
 */
public interface ComponentFunctionType {

  /**
   * Gets the function name.
   *
   * <p>Returns the name of the function as defined in the component interface.
   *
   * @return the function name
   */
  String getName();

  /**
   * Gets the parameter types for this function.
   *
   * <p>Returns an ordered list of parameter types that must be provided when calling this
   * function. Each parameter includes name, type, and constraint information.
   *
   * @return list of parameter types
   */
  List<ComponentParameterType> getParameterTypes();

  /**
   * Gets the return type for this function.
   *
   * <p>Returns the type information for the value returned by this function, or empty if the
   * function returns no value.
   *
   * @return return type, or empty if void function
   */
  Optional<ComponentValueType> getReturnType();

  /**
   * Gets the calling convention for this function.
   *
   * <p>Returns information about how this function should be called, including parameter passing
   * conventions and memory management requirements.
   *
   * @return calling convention information
   */
  ComponentCallingConvention getCallingConvention();

  /**
   * Checks if this function can raise exceptions.
   *
   * <p>Returns true if this function may throw exceptions or trigger error conditions that need
   * to be handled by the caller.
   *
   * @return true if the function can raise exceptions, false otherwise
   */
  boolean canRaiseExceptions();

  /**
   * Gets the exception types that this function can raise.
   *
   * <p>Returns a list of exception or error types that may be thrown by this function. Empty if
   * the function cannot raise exceptions.
   *
   * @return list of exception types
   */
  List<ComponentExceptionType> getExceptionTypes();

  /**
   * Checks if this function is asynchronous.
   *
   * <p>Returns true if this function supports asynchronous execution and may not complete
   * immediately.
   *
   * @return true if the function is asynchronous, false otherwise
   */
  boolean isAsynchronous();

  /**
   * Checks if this function is pure (side-effect free).
   *
   * <p>Returns true if this function has no observable side effects and always returns the same
   * result for the same inputs.
   *
   * @return true if the function is pure, false otherwise
   */
  boolean isPure();

  /**
   * Gets resource requirements for calling this function.
   *
   * <p>Returns information about resources that must be available or may be consumed when
   * calling this function.
   *
   * @return resource requirements
   */
  ComponentResourceRequirements getResourceRequirements();

  /**
   * Gets performance characteristics for this function.
   *
   * <p>Returns hints about the expected performance profile of this function, useful for
   * optimization and scheduling decisions.
   *
   * @return performance characteristics
   */
  ComponentFunctionPerformanceProfile getPerformanceProfile();

  /**
   * Validates parameter types against this function signature.
   *
   * <p>Checks if the provided parameter types are compatible with this function's expected
   * parameters.
   *
   * @param parameterTypes the parameter types to validate
   * @return true if parameters are compatible, false otherwise
   * @throws IllegalArgumentException if parameterTypes is null
   */
  boolean validateParameters(final List<ComponentValueType> parameterTypes);

  /**
   * Checks if this function type is compatible with another function type.
   *
   * <p>Determines if functions of this type can be used where the other type is expected,
   * following component model subtyping rules.
   *
   * @param other the function type to check compatibility with
   * @return true if types are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final ComponentFunctionType other);

  /**
   * Gets documentation for this function.
   *
   * <p>Returns human-readable documentation that was embedded with this function's definition,
   * if available.
   *
   * @return function documentation, or empty if none available
   */
  Optional<String> getDocumentation();
}