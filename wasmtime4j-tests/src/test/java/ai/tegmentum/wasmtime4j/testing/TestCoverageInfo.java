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
 * Test coverage information for an API.
 *
 * <p>This interface provides detailed information about test coverage for a specific API, including
 * unit tests, integration tests, and functional validation.
 */
public interface TestCoverageInfo {

  /**
   * Gets the test coverage percentage for this API.
   *
   * @return coverage percentage from 0.0 to 100.0
   */
  double getCoveragePercentage();

  /**
   * Gets the number of unit tests covering this API.
   *
   * @return unit test count
   */
  int getUnitTestCount();

  /**
   * Gets the number of integration tests covering this API.
   *
   * @return integration test count
   */
  int getIntegrationTestCount();

  /**
   * Gets the number of functional tests covering this API.
   *
   * @return functional test count
   */
  int getFunctionalTestCount();

  /**
   * Gets list of test methods that cover this API.
   *
   * @return list of test method names
   */
  List<String> getTestMethods();

  /**
   * Gets list of uncovered scenarios for this API.
   *
   * @return list of uncovered scenario descriptions
   */
  List<String> getUncoveredScenarios();

  /**
   * Checks if this API has adequate test coverage.
   *
   * @return true if test coverage meets quality standards
   */
  boolean hasAdequateCoverage();

  /**
   * Gets the total number of test cases for this API.
   *
   * @return total test case count
   */
  default int getTotalTestCount() {
    return getUnitTestCount() + getIntegrationTestCount() + getFunctionalTestCount();
  }

  /**
   * Checks if this API has any tests.
   *
   * @return true if at least one test exists
   */
  default boolean hasTests() {
    return getTotalTestCount() > 0;
  }

  /**
   * Gets test coverage summary as formatted string.
   *
   * @return human-readable test coverage summary
   */
  default String getSummary() {
    return String.format(
        "Test Coverage: %.2f%% (%d tests: %d unit, %d integration, %d functional)",
        getCoveragePercentage(),
        getTotalTestCount(),
        getUnitTestCount(),
        getIntegrationTestCount(),
        getFunctionalTestCount());
  }
}
