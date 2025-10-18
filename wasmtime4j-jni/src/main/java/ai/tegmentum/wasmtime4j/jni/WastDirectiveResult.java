package ai.tegmentum.wasmtime4j.jni;

/**
 * Result of executing a single WAST directive.
 *
 * <p>This class represents the outcome of executing one test assertion or directive within a WAST
 * file. Each WAST file may contain multiple directives (e.g., assert_return, assert_trap, module
 * definitions).
 */
public final class WastDirectiveResult {

  private final int lineNumber;
  private final boolean passed;
  private final String errorMessage;

  /**
   * Creates a new WAST directive result.
   *
   * @param lineNumber the line number in the WAST file where this directive appears
   * @param passed whether the directive passed (true) or failed (false)
   * @param errorMessage the error message if the directive failed, or null if it passed
   */
  public WastDirectiveResult(
      final int lineNumber, final boolean passed, final String errorMessage) {
    this.lineNumber = lineNumber;
    this.passed = passed;
    this.errorMessage = errorMessage;
  }

  /**
   * Gets the line number where this directive appears in the WAST file.
   *
   * @return the line number (0-based)
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Checks if this directive passed.
   *
   * @return true if the directive passed, false otherwise
   */
  public boolean isPassed() {
    return passed;
  }

  /**
   * Gets the error message if the directive failed.
   *
   * @return the error message, or null if the directive passed
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public String toString() {
    if (passed) {
      return String.format("WastDirectiveResult[line=%d, passed=true]", lineNumber);
    } else {
      return String.format(
          "WastDirectiveResult[line=%d, passed=false, error=%s]", lineNumber, errorMessage);
    }
  }
}
