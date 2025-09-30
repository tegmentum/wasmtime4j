package ai.tegmentum.wasmtime4j.debug;

/**
 * Stack frame interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface StackFrame {

  /**
   * Gets the frame index.
   *
   * @return frame index
   */
  int getFrameIndex();

  /**
   * Gets the function name.
   *
   * @return function name
   */
  String getFunctionName();

  /**
   * Gets the source location.
   *
   * @return source location or null
   */
  SourceLocation getSourceLocation();

  /**
   * Gets the local variables.
   *
   * @return list of variables
   */
  java.util.List<Variable> getVariables();

  /**
   * Gets the frame depth.
   *
   * @return depth in call stack
   */
  int getDepth();

  /** Source location interface. */
  interface SourceLocation {
    /**
     * Gets the file path.
     *
     * @return file path
     */
    String getFilePath();

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
  }
}
