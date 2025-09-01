package ai.tegmentum.wasmtime4j.wasi;

import java.util.List;
import java.util.Optional;

/**
 * Metadata information about a WASI component function.
 *
 * <p>Function metadata provides comprehensive information about function signatures including
 * parameter types, return types, and documentation. This is essential for dynamic invocation and
 * tooling support.
 *
 * @since 1.0.0
 */
public interface WasiFunctionMetadata {

  /**
   * Gets the name of the function.
   *
   * @return the function name
   */
  String getName();

  /**
   * Gets the documentation for this function.
   *
   * @return function documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Gets the parameter definitions for this function.
   *
   * @return list of parameter metadata in order
   */
  List<WasiParameterMetadata> getParameters();

  /**
   * Gets the return type for this function.
   *
   * @return return type metadata, or empty for void functions
   */
  Optional<WasiTypeMetadata> getReturnType();

  /**
   * Checks if this function can throw exceptions.
   *
   * @return true if the function can throw exceptions, false otherwise
   */
  boolean canThrow();

  /**
   * Gets the list of exception types this function can throw.
   *
   * @return list of exception type names
   */
  List<String> getThrownExceptionTypes();

  /**
   * Validates parameter types for a function call.
   *
   * @param parameters the parameters to validate
   * @throws IllegalArgumentException if parameters don't match the function signature
   */
  void validateParameters(final Object... parameters);
}
