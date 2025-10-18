package ai.tegmentum.wasmtime4j.jni;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Results from executing a complete WAST file.
 *
 * <p>This class contains comprehensive information about the execution of a WAST (WebAssembly Test)
 * file, including overall pass/fail status, individual directive results, and any execution errors
 * that occurred.
 *
 * <p>WAST files are the standard test format used by the WebAssembly specification and Wasmtime
 * runtime. They contain module definitions and test assertions that verify WebAssembly behavior.
 */
public final class WastExecutionResult {

  private final String filePath;
  private final int totalDirectives;
  private final int passedDirectives;
  private final int failedDirectives;
  private final String executionError;
  private final WastDirectiveResult[] directiveResults;

  /**
   * Creates a new WAST execution result.
   *
   * @param filePath the path to the WAST file that was executed
   * @param totalDirectives the total number of directives in the file
   * @param passedDirectives the number of directives that passed
   * @param failedDirectives the number of directives that failed
   * @param executionError overall execution error message, or null if execution succeeded
   * @param directiveResults array of individual directive results
   */
  public WastExecutionResult(
      final String filePath,
      final int totalDirectives,
      final int passedDirectives,
      final int failedDirectives,
      final String executionError,
      final WastDirectiveResult[] directiveResults) {
    this.filePath = filePath;
    this.totalDirectives = totalDirectives;
    this.passedDirectives = passedDirectives;
    this.failedDirectives = failedDirectives;
    this.executionError = executionError;
    this.directiveResults =
        directiveResults != null ? directiveResults : new WastDirectiveResult[0];
  }

  /**
   * Gets the path to the WAST file that was executed.
   *
   * @return the file path
   */
  public String getFilePath() {
    return filePath;
  }

  /**
   * Gets the total number of directives in the WAST file.
   *
   * @return the total directive count
   */
  public int getTotalDirectives() {
    return totalDirectives;
  }

  /**
   * Gets the number of directives that passed.
   *
   * @return the passed directive count
   */
  public int getPassedDirectives() {
    return passedDirectives;
  }

  /**
   * Gets the number of directives that failed.
   *
   * @return the failed directive count
   */
  public int getFailedDirectives() {
    return failedDirectives;
  }

  /**
   * Gets the overall execution error message, if any.
   *
   * @return the execution error message, or null if execution succeeded
   */
  public String getExecutionError() {
    return executionError;
  }

  /**
   * Gets the individual directive results.
   *
   * @return an unmodifiable list of directive results
   */
  public List<WastDirectiveResult> getDirectiveResults() {
    return Collections.unmodifiableList(Arrays.asList(directiveResults));
  }

  /**
   * Checks if all directives passed.
   *
   * @return true if all directives passed and there were no execution errors
   */
  public boolean allPassed() {
    return executionError == null && failedDirectives == 0;
  }

  /**
   * Gets the pass rate as a percentage.
   *
   * @return the pass rate (0.0 to 100.0)
   */
  public double getPassRate() {
    if (totalDirectives == 0) {
      return 100.0;
    }
    return (passedDirectives * 100.0) / totalDirectives;
  }

  @Override
  public String toString() {
    if (allPassed()) {
      return String.format(
          "WastExecutionResult[file=%s, passed=%d/%d (100%%)]",
          filePath, passedDirectives, totalDirectives);
    } else if (executionError != null) {
      return String.format("WastExecutionResult[file=%s, error=%s]", filePath, executionError);
    } else {
      return String.format(
          "WastExecutionResult[file=%s, passed=%d/%d (%.1f%%), failed=%d]",
          filePath, passedDirectives, totalDirectives, getPassRate(), failedDirectives);
    }
  }
}
