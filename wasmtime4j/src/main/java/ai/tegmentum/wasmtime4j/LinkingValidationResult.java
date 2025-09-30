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

package ai.tegmentum.wasmtime4j;

/**
 * Result of component linking validation operation.
 *
 * <p>This class provides information about the validation of a component linking configuration,
 * including validation status and any issues found.
 *
 * @since 1.0.0
 */
public final class LinkingValidationResult {

  private final boolean valid;
  private final String details;

  /**
   * Creates a new linking validation result.
   *
   * @param valid whether the linking configuration is valid
   * @param details detailed information about the validation
   */
  public LinkingValidationResult(final boolean valid, final String details) {
    this.valid = valid;
    this.details = details != null ? details : "";
  }

  /**
   * Creates a valid result with details.
   *
   * @param details validation details
   * @return a valid result
   */
  public static LinkingValidationResult valid(final String details) {
    return new LinkingValidationResult(true, details);
  }

  /**
   * Creates an invalid result with details.
   *
   * @param details validation failure details
   * @return an invalid result
   */
  public static LinkingValidationResult invalid(final String details) {
    return new LinkingValidationResult(false, details);
  }

  /**
   * Checks if the linking configuration is valid.
   *
   * @return true if valid, false otherwise
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * Gets the validation details.
   *
   * @return the validation details
   */
  public String getDetails() {
    return details;
  }

  @Override
  public String toString() {
    return "LinkingValidationResult{valid=" + valid + ", details='" + details + "'}";
  }
}
