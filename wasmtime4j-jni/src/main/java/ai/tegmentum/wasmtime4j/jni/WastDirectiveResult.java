/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.jni;

/**
 * Result of executing a single WAST directive.
 *
 * <p>Represents the outcome of a single test assertion in a WAST script, including whether it
 * passed and any error message if it failed.
 *
 * @since 1.0.0
 */
public final class WastDirectiveResult {

  private final int lineNumber;
  private final boolean passed;
  private final String errorMessage;

  /**
   * Creates a new WAST directive result.
   *
   * @param lineNumber the line number in the WAST file where this directive appears
   * @param passed whether the directive passed
   * @param errorMessage the error message if the directive failed, null otherwise
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
   * @return the line number
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Checks if the directive passed.
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
      return String.format("Line %d: PASSED", lineNumber);
    } else {
      return String.format("Line %d: FAILED - %s", lineNumber, errorMessage);
    }
  }
}
