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

/**
 * Default implementation of TestCoverageInfo.
 *
 * <p>Provides information about test coverage for a specific API including unit tests, integration
 * tests, and code coverage metrics.
 */
final class DefaultTestCoverageInfo implements TestCoverageInfo {

  private final boolean hasUnitTests;
  private final boolean hasIntegrationTests;
  private final double codeCoverage;
  private final int testCount;

  DefaultTestCoverageInfo(
      final boolean hasUnitTests,
      final boolean hasIntegrationTests,
      final double codeCoverage,
      final int testCount) {
    this.hasUnitTests = hasUnitTests;
    this.hasIntegrationTests = hasIntegrationTests;
    this.codeCoverage = codeCoverage;
    this.testCount = testCount;
  }

  @Override
  public boolean hasUnitTests() {
    return hasUnitTests;
  }

  @Override
  public boolean hasIntegrationTests() {
    return hasIntegrationTests;
  }

  @Override
  public double getCodeCoverage() {
    return codeCoverage;
  }

  @Override
  public int getTestCount() {
    return testCount;
  }

  @Override
  public String toString() {
    return String.format(
        "TestCoverageInfo{unitTests=%s, integrationTests=%s, codeCoverage=%.2f%%, testCount=%d}",
        hasUnitTests, hasIntegrationTests, codeCoverage, testCount);
  }
}
