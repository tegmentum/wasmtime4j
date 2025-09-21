/*
 * Copyright 2024 Tegmentum AI Inc.
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

package ai.tegmentum.wasmtime4j.testing;

import java.util.List;

/**
 * Report of parity validation between JNI and Panama implementations.
 *
 * <p>This report provides comprehensive analysis of behavioral and functional consistency between
 * different implementation backends.
 */
public interface ParityReport {

  /**
   * Gets the overall parity percentage between implementations.
   *
   * @return parity percentage from 0.0 to 100.0
   */
  double getParityPercentage();

  /**
   * Gets the total number of APIs tested for parity.
   *
   * @return total APIs tested
   */
  int getTotalApisTested();

  /**
   * Gets the number of parity violations found.
   *
   * @return violation count
   */
  int getViolationCount();

  /**
   * Gets detailed information about all parity violations.
   *
   * @return list of parity violations
   */
  List<ParityViolation> getViolations();

  /**
   * Gets violations grouped by severity level.
   *
   * @param severity the severity level
   * @return list of violations at the specified severity
   */
  List<ParityViolation> getViolationsBySeverity(ParityViolation.Severity severity);

  /**
   * Gets APIs that are missing in JNI implementation.
   *
   * @return list of missing JNI APIs
   */
  List<String> getMissingJniApis();

  /**
   * Gets APIs that are missing in Panama implementation.
   *
   * @return list of missing Panama APIs
   */
  List<String> getMissingPanamaApis();

  /**
   * Checks if implementations have acceptable parity (95%+).
   *
   * @return true if parity is acceptable
   */
  default boolean hasAcceptableParity() {
    return getParityPercentage() >= 95.0 && getCriticalViolations().isEmpty();
  }

  /**
   * Gets critical parity violations that must be resolved.
   *
   * @return list of critical violations
   */
  default List<ParityViolation> getCriticalViolations() {
    return getViolationsBySeverity(ParityViolation.Severity.CRITICAL);
  }

  /**
   * Gets high-severity parity violations.
   *
   * @return list of high-severity violations
   */
  default List<ParityViolation> getHighSeverityViolations() {
    return getViolationsBySeverity(ParityViolation.Severity.HIGH);
  }

  /**
   * Gets a summary of the parity validation.
   *
   * @return human-readable parity summary
   */
  default String getSummary() {
    return String.format(
        "Parity: %.2f%% (%d/%d APIs tested, %d violations, %d critical)",
        getParityPercentage(),
        getTotalApisTested() - getViolationCount(),
        getTotalApisTested(),
        getViolationCount(),
        getCriticalViolations().size());
  }
}
