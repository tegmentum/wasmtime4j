package ai.tegmentum.wasmtime4j.webassembly;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Consolidated results from all advanced WebAssembly feature testing, providing comprehensive
 * analysis and reporting across SIMD, Threading, and Exception handling tests.
 *
 * <p>This class aggregates results from individual feature test executors to provide overall
 * insights into advanced feature coverage, performance, and cross-runtime consistency.
 */
public final class AdvancedFeatureTestResults {
  private final Instant startTime;
  private final Duration totalDuration;
  private final Optional<SimdTestResults> simdResults;
  private final Optional<ThreadingTestResults> threadingResults;
  private final Optional<ExceptionTestResults> exceptionResults;

  private AdvancedFeatureTestResults(final Builder builder) {
    this.startTime = builder.startTime;
    this.totalDuration = builder.totalDuration;
    this.simdResults = builder.simdResults;
    this.threadingResults = builder.threadingResults;
    this.exceptionResults = builder.exceptionResults;
  }

  /**
   * Gets the test execution start time.
   *
   * @return the start time
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Gets the total execution duration across all advanced features.
   *
   * @return the total duration
   */
  public Duration getTotalDuration() {
    return totalDuration;
  }

  /**
   * Gets the SIMD test results if SIMD testing was performed.
   *
   * @return optional SIMD test results
   */
  public Optional<SimdTestResults> getSimdResults() {
    return simdResults;
  }

  /**
   * Gets the Threading test results if Threading testing was performed.
   *
   * @return optional Threading test results
   */
  public Optional<ThreadingTestResults> getThreadingResults() {
    return threadingResults;
  }

  /**
   * Gets the Exception test results if Exception testing was performed.
   *
   * @return optional Exception test results
   */
  public Optional<ExceptionTestResults> getExceptionResults() {
    return exceptionResults;
  }

  /**
   * Checks if SIMD test results are available.
   *
   * @return true if SIMD tests were executed
   */
  public boolean hasSimdResults() {
    return simdResults.isPresent();
  }

  /**
   * Checks if Threading test results are available.
   *
   * @return true if Threading tests were executed
   */
  public boolean hasThreadingResults() {
    return threadingResults.isPresent();
  }

  /**
   * Checks if Exception test results are available.
   *
   * @return true if Exception tests were executed
   */
  public boolean hasExceptionResults() {
    return exceptionResults.isPresent();
  }

  /**
   * Gets the total number of tests executed across all advanced features.
   *
   * @return the total tests executed
   */
  public int getTotalTestsExecuted() {
    int total = 0;
    if (simdResults.isPresent()) {
      total += simdResults.get().getTotalTestsExecuted();
    }
    if (threadingResults.isPresent()) {
      total += threadingResults.get().getTotalTestsExecuted();
    }
    if (exceptionResults.isPresent()) {
      total += exceptionResults.get().getTotalTestsExecuted();
    }
    return total;
  }

  /**
   * Gets the total number of successful tests across all advanced features.
   *
   * @return the total successful tests
   */
  public int getTotalSuccessfulTests() {
    int total = 0;
    if (simdResults.isPresent()) {
      total += simdResults.get().getSuccessfulTests();
    }
    if (threadingResults.isPresent()) {
      total += threadingResults.get().getSuccessfulTests();
    }
    if (exceptionResults.isPresent()) {
      total += exceptionResults.get().getSuccessfulTests();
    }
    return total;
  }

  /**
   * Gets the total number of failed tests across all advanced features.
   *
   * @return the total failed tests
   */
  public int getTotalFailedTests() {
    int total = 0;
    if (simdResults.isPresent()) {
      total += simdResults.get().getFailedTests();
    }
    if (threadingResults.isPresent()) {
      total += threadingResults.get().getFailedTests();
    }
    if (exceptionResults.isPresent()) {
      total += exceptionResults.get().getFailedTests();
    }
    return total;
  }

  /**
   * Calculates the overall success rate across all advanced feature tests.
   *
   * @return the overall success rate percentage (0.0 to 100.0)
   */
  public double getOverallSuccessRate() {
    final int totalTests = getTotalTestsExecuted();
    if (totalTests == 0) {
      return 0.0;
    }
    return (double) getTotalSuccessfulTests() / totalTests * 100.0;
  }

  /**
   * Calculates the weighted average coverage across all executed advanced features.
   *
   * @return the weighted average coverage percentage
   */
  public double getWeightedAverageCoverage() {
    double totalWeightedCoverage = 0.0;
    int totalWeight = 0;

    if (simdResults.isPresent()) {
      final double simdCoverage = simdResults.get().getCoverageMetrics().getOverallCoverage();
      final int simdWeight = simdResults.get().getTotalTestsExecuted();
      totalWeightedCoverage += simdCoverage * simdWeight;
      totalWeight += simdWeight;
    }

    if (threadingResults.isPresent()) {
      final double threadingCoverage =
          threadingResults.get().getCoverageMetrics().getOverallCoverage();
      final int threadingWeight = threadingResults.get().getTotalTestsExecuted();
      totalWeightedCoverage += threadingCoverage * threadingWeight;
      totalWeight += threadingWeight;
    }

    if (exceptionResults.isPresent()) {
      final double exceptionCoverage =
          exceptionResults.get().getCoverageMetrics().getOverallCoverage();
      final int exceptionWeight = exceptionResults.get().getTotalTestsExecuted();
      totalWeightedCoverage += exceptionCoverage * exceptionWeight;
      totalWeight += exceptionWeight;
    }

    return totalWeight == 0 ? 0.0 : totalWeightedCoverage / totalWeight;
  }

  /**
   * Checks if all executed features meet their individual target coverage goals.
   *
   * @return true if all features meet their targets
   */
  public boolean meetsOverallTargets() {
    boolean allTargetsMet = true;

    if (simdResults.isPresent()) {
      allTargetsMet &= simdResults.get().meetsTargetCoverage(); // 60-70% target
    }

    if (threadingResults.isPresent()) {
      allTargetsMet &= threadingResults.get().meetsTargetCoverage(); // 50-60% target
    }

    if (exceptionResults.isPresent()) {
      allTargetsMet &= exceptionResults.get().meetsTargetCoverage(); // 70-80% target
    }

    return allTargetsMet;
  }

  /**
   * Estimates the overall test coverage improvement from advanced features. This represents the
   * 8-12% improvement target mentioned in the specification.
   *
   * @return estimated coverage improvement percentage
   */
  public double estimatedCoverageImprovement() {
    // Calculate based on the weighted coverage and number of features tested
    final double weightedCoverage = getWeightedAverageCoverage();
    final int featuresCount = getExecutedFeaturesCount();

    // Estimate improvement based on feature coverage and breadth
    // Each feature contributes proportionally to overall improvement
    return (weightedCoverage / 100.0) * (featuresCount * 4.0); // ~4% per feature at full coverage
  }

  /**
   * Gets the number of advanced features that were executed.
   *
   * @return the count of executed features
   */
  public int getExecutedFeaturesCount() {
    int count = 0;
    if (simdResults.isPresent()) count++;
    if (threadingResults.isPresent()) count++;
    if (exceptionResults.isPresent()) count++;
    return count;
  }

  /**
   * Checks if cross-runtime validation was completed for all executed features.
   *
   * @return true if cross-runtime validation was performed for all features
   */
  public boolean isCrossRuntimeValidationCompleted() {
    boolean allCompleted = true;

    if (simdResults.isPresent()) {
      allCompleted &= simdResults.get().isCrossRuntimeValidationCompleted();
    }

    if (threadingResults.isPresent()) {
      allCompleted &= threadingResults.get().isCrossRuntimeValidationCompleted();
    }

    if (exceptionResults.isPresent()) {
      allCompleted &= exceptionResults.get().isCrossRuntimeValidationCompleted();
    }

    return allCompleted;
  }

  /**
   * Checks if performance benchmarking was completed for all executed features.
   *
   * @return true if performance benchmarking was performed for all features
   */
  public boolean isPerformanceBenchmarkingCompleted() {
    boolean allCompleted = true;

    if (simdResults.isPresent()) {
      allCompleted &= simdResults.get().isPerformanceBenchmarkingCompleted();
    }

    if (threadingResults.isPresent()) {
      allCompleted &= threadingResults.get().isPerformanceBenchmarkingCompleted();
    }

    if (exceptionResults.isPresent()) {
      allCompleted &= exceptionResults.get().isPerformanceBenchmarkingCompleted();
    }

    return allCompleted;
  }

  /**
   * Gets a summary of the advanced feature testing execution.
   *
   * @return execution summary
   */
  public AdvancedFeatureExecutionSummary getExecutionSummary() {
    return new AdvancedFeatureExecutionSummary(
        getExecutedFeaturesCount(),
        getTotalTestsExecuted(),
        getTotalSuccessfulTests(),
        getTotalFailedTests(),
        getOverallSuccessRate(),
        getWeightedAverageCoverage(),
        estimatedCoverageImprovement(),
        meetsOverallTargets(),
        isCrossRuntimeValidationCompleted(),
        isPerformanceBenchmarkingCompleted(),
        totalDuration);
  }

  @Override
  public String toString() {
    return String.format(
        "AdvancedFeatureTestResults{features=%d, totalTests=%d, successful=%d, failed=%d, "
            + "successRate=%.1f%%, avgCoverage=%.1f%%, improvement=%.1f%%, duration=%ds}",
        getExecutedFeaturesCount(),
        getTotalTestsExecuted(),
        getTotalSuccessfulTests(),
        getTotalFailedTests(),
        getOverallSuccessRate(),
        getWeightedAverageCoverage(),
        estimatedCoverageImprovement(),
        totalDuration.toSeconds());
  }

  /** Builder class for creating AdvancedFeatureTestResults instances. */
  public static final class Builder {
    private Instant startTime = Instant.now();
    private Duration totalDuration = Duration.ZERO;
    private Optional<SimdTestResults> simdResults = Optional.empty();
    private Optional<ThreadingTestResults> threadingResults = Optional.empty();
    private Optional<ExceptionTestResults> exceptionResults = Optional.empty();

    /**
     * Sets the start time.
     *
     * @param startTime the start time
     * @return this builder
     */
    public Builder startTime(final Instant startTime) {
      this.startTime = Objects.requireNonNull(startTime, "startTime cannot be null");
      return this;
    }

    /**
     * Sets the total duration.
     *
     * @param totalDuration the total duration
     * @return this builder
     */
    public Builder totalDuration(final Duration totalDuration) {
      this.totalDuration = Objects.requireNonNull(totalDuration, "totalDuration cannot be null");
      return this;
    }

    /**
     * Sets the SIMD test results.
     *
     * @param simdResults the SIMD test results
     * @return this builder
     */
    public Builder simdResults(final SimdTestResults simdResults) {
      this.simdResults =
          Optional.of(Objects.requireNonNull(simdResults, "simdResults cannot be null"));
      return this;
    }

    /**
     * Sets the Threading test results.
     *
     * @param threadingResults the Threading test results
     * @return this builder
     */
    public Builder threadingResults(final ThreadingTestResults threadingResults) {
      this.threadingResults =
          Optional.of(Objects.requireNonNull(threadingResults, "threadingResults cannot be null"));
      return this;
    }

    /**
     * Sets the Exception test results.
     *
     * @param exceptionResults the Exception test results
     * @return this builder
     */
    public Builder exceptionResults(final ExceptionTestResults exceptionResults) {
      this.exceptionResults =
          Optional.of(Objects.requireNonNull(exceptionResults, "exceptionResults cannot be null"));
      return this;
    }

    /**
     * Builds the AdvancedFeatureTestResults.
     *
     * @return the configured AdvancedFeatureTestResults
     */
    public AdvancedFeatureTestResults build() {
      return new AdvancedFeatureTestResults(this);
    }
  }

  /** Summary of advanced feature test execution for high-level reporting. */
  public static final class AdvancedFeatureExecutionSummary {
    private final int featuresExecuted;
    private final int totalTests;
    private final int successfulTests;
    private final int failedTests;
    private final double successRate;
    private final double averageCoverage;
    private final double coverageImprovement;
    private final boolean meetsTargets;
    private final boolean crossRuntimeValidated;
    private final boolean performanceBenchmarked;
    private final Duration executionTime;

    public AdvancedFeatureExecutionSummary(
        final int featuresExecuted,
        final int totalTests,
        final int successfulTests,
        final int failedTests,
        final double successRate,
        final double averageCoverage,
        final double coverageImprovement,
        final boolean meetsTargets,
        final boolean crossRuntimeValidated,
        final boolean performanceBenchmarked,
        final Duration executionTime) {

      this.featuresExecuted = featuresExecuted;
      this.totalTests = totalTests;
      this.successfulTests = successfulTests;
      this.failedTests = failedTests;
      this.successRate = successRate;
      this.averageCoverage = averageCoverage;
      this.coverageImprovement = coverageImprovement;
      this.meetsTargets = meetsTargets;
      this.crossRuntimeValidated = crossRuntimeValidated;
      this.performanceBenchmarked = performanceBenchmarked;
      this.executionTime = executionTime;
    }

    // Getters for all fields
    public int getFeaturesExecuted() {
      return featuresExecuted;
    }

    public int getTotalTests() {
      return totalTests;
    }

    public int getSuccessfulTests() {
      return successfulTests;
    }

    public int getFailedTests() {
      return failedTests;
    }

    public double getSuccessRate() {
      return successRate;
    }

    public double getAverageCoverage() {
      return averageCoverage;
    }

    public double getCoverageImprovement() {
      return coverageImprovement;
    }

    public boolean meetsTargets() {
      return meetsTargets;
    }

    public boolean isCrossRuntimeValidated() {
      return crossRuntimeValidated;
    }

    public boolean isPerformanceBenchmarked() {
      return performanceBenchmarked;
    }

    public Duration getExecutionTime() {
      return executionTime;
    }

    @Override
    public String toString() {
      return String.format(
          "AdvancedFeatureExecutionSummary{features=%d, tests=%d, success=%.1f%%, "
              + "coverage=%.1f%%, improvement=%.1f%%, targets=%s, time=%ds}",
          featuresExecuted,
          totalTests,
          successRate,
          averageCoverage,
          coverageImprovement,
          meetsTargets ? "✓" : "✗",
          executionTime.toSeconds());
    }
  }
}
