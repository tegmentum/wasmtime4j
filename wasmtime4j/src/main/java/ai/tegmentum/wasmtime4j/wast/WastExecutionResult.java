/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.wast;

/**
 * Results from executing a complete WAST file.
 *
 * <p>Contains aggregate statistics and detailed results for all directives in a WAST script. WAST
 * (WebAssembly Script) is the test format used by the WebAssembly specification.
 *
 * @since 1.0.0
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
   * @param executionError an overall execution error message, or null if no error
   * @param directiveResults detailed results for each directive
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
    this.directiveResults = directiveResults;
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
   * Gets the overall execution error message if the WAST execution failed.
   *
   * @return the execution error message, or null if no error occurred
   */
  public String getExecutionError() {
    return executionError;
  }

  /**
   * Gets detailed results for each directive in the WAST file.
   *
   * @return array of directive results
   */
  public WastDirectiveResult[] getDirectiveResults() {
    return directiveResults;
  }

  /**
   * Checks if all directives passed.
   *
   * @return true if all directives passed and there was no execution error
   */
  public boolean allPassed() {
    return executionError == null && failedDirectives == 0;
  }

  /**
   * Gets the pass rate as a percentage.
   *
   * @return the pass rate (0-100)
   */
  public double getPassRate() {
    if (totalDirectives == 0) {
      return 100.0;
    }
    return (passedDirectives * 100.0) / totalDirectives;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("WAST Execution Result: ").append(filePath).append('\n');
    sb.append("Total: ").append(totalDirectives);
    sb.append(", Passed: ").append(passedDirectives);
    sb.append(", Failed: ").append(failedDirectives);
    sb.append(" (").append(String.format("%.1f", getPassRate())).append("%)\n");

    if (executionError != null) {
      sb.append("Execution Error: ").append(executionError).append('\n');
    }

    if (directiveResults != null && directiveResults.length > 0) {
      sb.append("Directive Results:\n");
      for (final WastDirectiveResult result : directiveResults) {
        sb.append("  ").append(result).append('\n');
      }
    }

    return sb.toString();
  }
}
