package ai.tegmentum.wasmtime4j.debug;

/**
 * Breakpoint interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface Breakpoint {

  /**
   * Gets the breakpoint ID.
   *
   * @return breakpoint ID
   */
  String getBreakpointId();

  /**
   * Gets the function name.
   *
   * @return function name
   */
  String getFunctionName();

  /**
   * Gets the line number.
   *
   * @return line number
   */
  int getLineNumber();

  /**
   * Gets the column number.
   *
   * @return column number
   */
  int getColumnNumber();

  /**
   * Checks if the breakpoint is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Sets the breakpoint enabled state.
   *
   * @param enabled enabled state
   */
  void setEnabled(boolean enabled);

  /**
   * Gets the breakpoint condition.
   *
   * @return condition expression or null
   */
  String getCondition();

  /**
   * Sets the breakpoint condition.
   *
   * @param condition condition expression
   */
  void setCondition(String condition);

  /**
   * Gets the hit count.
   *
   * @return number of times this breakpoint was hit
   */
  int getHitCount();

  /** Resets the hit count. */
  void resetHitCount();
}
