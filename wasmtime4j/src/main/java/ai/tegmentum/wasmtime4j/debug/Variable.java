package ai.tegmentum.wasmtime4j.debug;

/**
 * Variable interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface Variable {

  /**
   * Gets the variable name.
   *
   * @return variable name
   */
  String getName();

  /**
   * Gets the variable type.
   *
   * @return variable type
   */
  String getType();

  /**
   * Gets the variable value.
   *
   * @return variable value
   */
  VariableValue getValue();

  /**
   * Gets the variable scope.
   *
   * @return variable scope
   */
  VariableScope getScope();

  /**
   * Checks if the variable is mutable.
   *
   * @return true if mutable
   */
  boolean isMutable();

  /** Variable scope enumeration. */
  enum VariableScope {
    /** Local variable. */
    LOCAL,
    /** Global variable. */
    GLOBAL,
    /** Parameter. */
    PARAMETER,
    /** Return value. */
    RETURN_VALUE
  }
}
