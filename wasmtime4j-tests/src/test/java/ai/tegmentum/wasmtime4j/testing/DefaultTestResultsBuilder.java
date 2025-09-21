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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of TestResultsBuilder.
 *
 * <p>This builder accumulates test results and provides statistical analysis for comprehensive test
 * reporting and validation.
 */
final class DefaultTestResultsBuilder implements TestResultsBuilder {

  private final List<TestFailure> failures = new ArrayList<>();
  private final List<String> skippedTests = new ArrayList<>();
  private final List<Duration> executionTimes = new ArrayList<>();

  private int totalTests = 0;
  private int passedTests = 0;
  private int failedTests = 0;
  private int skippedTestsCount = 0;
  private Duration totalExecutionTime = Duration.ZERO;
  private TestStatistics statistics;

  DefaultTestResultsBuilder() {}

  @Override
  public TestResultsBuilder addResults(final TestResults results) {
    Objects.requireNonNull(results, "Results cannot be null");

    this.totalTests += results.getTotalTests();
    this.passedTests += results.getPassedTests();
    this.failedTests += results.getFailedTests();
    this.skippedTestsCount += results.getSkippedTests();
    this.totalExecutionTime = this.totalExecutionTime.plus(results.getTotalExecutionTime());

    this.failures.addAll(results.getFailures());

    return this;
  }

  @Override
  public TestResultsBuilder addSuccess(final String testName, final Duration executionTime) {
    Objects.requireNonNull(testName, "Test name cannot be null");
    Objects.requireNonNull(executionTime, "Execution time cannot be null");

    this.totalTests++;
    this.passedTests++;
    this.totalExecutionTime = this.totalExecutionTime.plus(executionTime);
    this.executionTimes.add(executionTime);

    return this;
  }

  @Override
  public TestResultsBuilder addFailure(final TestFailure failure) {
    Objects.requireNonNull(failure, "Failure cannot be null");

    this.totalTests++;
    this.failedTests++;
    this.totalExecutionTime = this.totalExecutionTime.plus(failure.getExecutionTime());
    this.failures.add(failure);
    this.executionTimes.add(failure.getExecutionTime());

    return this;
  }

  @Override
  public TestResultsBuilder addFailure(
      final String testName, final String errorMessage, final Duration executionTime) {
    Objects.requireNonNull(testName, "Test name cannot be null");
    Objects.requireNonNull(errorMessage, "Error message cannot be null");
    Objects.requireNonNull(executionTime, "Execution time cannot be null");

    final TestFailure failure =
        TestFailure.builder(testName)
            .withErrorMessage(errorMessage)
            .withExecutionTime(executionTime)
            .build();

    return addFailure(failure);
  }

  @Override
  public TestResultsBuilder addSkipped(final String testName, final String reason) {
    Objects.requireNonNull(testName, "Test name cannot be null");

    this.totalTests++;
    this.skippedTestsCount++;
    this.skippedTests.add(testName + (reason != null ? ": " + reason : ""));

    return this;
  }

  @Override
  public TestResultsBuilder withStatistics(final TestStatistics statistics) {
    this.statistics = statistics;
    return this;
  }

  @Override
  public TestResults build() {
    // Calculate statistics if not provided
    if (statistics == null && !executionTimes.isEmpty()) {
      statistics = calculateStatistics();
    }

    return new DefaultTestResults(
        totalTests,
        passedTests,
        failedTests,
        skippedTestsCount,
        new ArrayList<>(failures),
        totalExecutionTime,
        statistics != null ? statistics : createEmptyStatistics());
  }

  private TestStatistics calculateStatistics() {
    if (executionTimes.isEmpty()) {
      return createEmptyStatistics();
    }

    final Duration minTime = executionTimes.stream().min(Duration::compareTo).orElse(Duration.ZERO);
    final Duration maxTime = executionTimes.stream().max(Duration::compareTo).orElse(Duration.ZERO);

    final long averageNanos =
        executionTimes.stream().mapToLong(Duration::toNanos).sum() / executionTimes.size();
    final Duration averageTime = Duration.ofNanos(averageNanos);

    // Calculate median
    final List<Duration> sortedTimes = new ArrayList<>(executionTimes);
    sortedTimes.sort(Duration::compareTo);
    final Duration medianTime = sortedTimes.get(sortedTimes.size() / 2);

    // Memory statistics (basic)
    final Runtime runtime = Runtime.getRuntime();
    final long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
    final long peakMemory = runtime.maxMemory();

    return TestStatistics.builder()
        .withMinExecutionTime(minTime)
        .withMaxExecutionTime(maxTime)
        .withAverageExecutionTime(averageTime)
        .withMedianExecutionTime(medianTime)
        .withMemoryUsedBytes(memoryUsed)
        .withPeakMemoryUsageBytes(peakMemory)
        .withTestMethodCount(totalTests)
        .withTestClassCount(1) // Default to 1 for now
        .build();
  }

  private TestStatistics createEmptyStatistics() {
    return TestStatistics.builder().build();
  }

  /** Default implementation of TestResults. */
  private static final class DefaultTestResults implements TestResults {

    private final int totalTests;
    private final int passedTests;
    private final int failedTests;
    private final int skippedTests;
    private final List<TestFailure> failures;
    private final Duration totalExecutionTime;
    private final TestStatistics statistics;

    DefaultTestResults(
        final int totalTests,
        final int passedTests,
        final int failedTests,
        final int skippedTests,
        final List<TestFailure> failures,
        final Duration totalExecutionTime,
        final TestStatistics statistics) {
      this.totalTests = totalTests;
      this.passedTests = passedTests;
      this.failedTests = failedTests;
      this.skippedTests = skippedTests;
      this.failures = failures;
      this.totalExecutionTime = totalExecutionTime;
      this.statistics = statistics;
    }

    @Override
    public int getTotalTests() {
      return totalTests;
    }

    @Override
    public int getPassedTests() {
      return passedTests;
    }

    @Override
    public int getFailedTests() {
      return failedTests;
    }

    @Override
    public int getSkippedTests() {
      return skippedTests;
    }

    @Override
    public List<TestFailure> getFailures() {
      return new ArrayList<>(failures);
    }

    @Override
    public Duration getTotalExecutionTime() {
      return totalExecutionTime;
    }

    @Override
    public TestStatistics getStatistics() {
      return statistics;
    }
  }
}
