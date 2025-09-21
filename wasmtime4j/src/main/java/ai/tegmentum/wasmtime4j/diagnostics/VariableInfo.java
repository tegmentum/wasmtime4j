package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.Optional;

/**
 * Information about a WebAssembly local variable or parameter.
 *
 * <p>This interface provides debugging information about variables in WebAssembly functions,
 * including their names, types, and current values.
 *
 * @since 1.0.0
 */
public interface VariableInfo {

  /** Variable types in WebAssembly context. */
  enum VariableType {
    /** Function parameter */
    PARAMETER,
    /** Local variable */
    LOCAL,
    /** Global variable */
    GLOBAL,
    /** Temporary value on the stack */
    TEMPORARY
  }

  /**
   * Gets the variable name.
   *
   * @return the variable name, or empty if not available
   */
  Optional<String> getName();

  /**
   * Gets the variable index within its scope.
   *
   * @return the variable index
   */
  int getIndex();

  /**
   * Gets the variable type.
   *
   * @return the variable type
   */
  VariableType getVariableType();

  /**
   * Gets the WebAssembly value type.
   *
   * @return the value type
   */
  String getValueType();

  /**
   * Gets the current value if available.
   *
   * @return the current value, or empty if not available
   */
  Optional<Object> getCurrentValue();

  /**
   * Gets the value as a formatted string.
   *
   * @return the formatted value, or empty if not available
   */
  Optional<String> getFormattedValue();

  /**
   * Gets the scope where this variable is accessible.
   *
   * @return the scope information, or empty if not available
   */
  Optional<VariableScope> getScope();

  /**
   * Checks if the variable is currently in scope.
   *
   * @return true if the variable is in scope
   */
  boolean isInScope();

  /**
   * Checks if the variable has been optimized away.
   *
   * @return true if the variable has been optimized away
   */
  boolean isOptimizedAway();

  /**
   * Gets additional debug properties for this variable.
   *
   * @return the debug properties, or empty if not available
   */
  Optional<String> getDebugProperties();

  /**
   * Creates a builder for constructing VariableInfo instances.
   *
   * @return a new variable info builder
   */
  static VariableInfoBuilder builder() {
    return new VariableInfoBuilder();
  }

  /**
   * Creates a simple VariableInfo with basic information.
   *
   * @param name the variable name
   * @param index the variable index
   * @param type the variable type
   * @param valueType the WebAssembly value type
   * @return the variable info
   */
  static VariableInfo of(
      final String name, final int index, final VariableType type, final String valueType) {
    return builder().name(name).index(index).variableType(type).valueType(valueType).build();
  }
}
