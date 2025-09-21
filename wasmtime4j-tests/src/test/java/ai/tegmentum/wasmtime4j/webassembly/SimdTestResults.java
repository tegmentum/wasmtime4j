package ai.tegmentum.wasmtime4j.webassembly;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Comprehensive results container for SIMD test execution, providing detailed statistics,
 * performance analysis, and coverage metrics for vector operation testing.
 *
 * <p>This class aggregates individual SIMD test results to provide insights into overall test
 * coverage, performance characteristics, and validation outcomes across different SIMD operation
 * categories.
 */
public final class SimdTestResults {
  private final List<SimdTestResult> results;
  private final Instant startTime;
  private final Duration totalDuration;
  private final int totalTestsExecuted;
  private final int successfulTests;
  private final int failedTests;
  private final boolean crossRuntimeValidationCompleted;
  private final boolean performanceBenchmarkingCompleted;
  private final SimdCoverageMetrics coverageMetrics;

  private SimdTestResults(final Builder builder) {
    this.results = Collections.unmodifiableList(new ArrayList<>(builder.results));
    this.startTime = builder.startTime;
    this.totalDuration = builder.totalDuration;
    this.totalTestsExecuted = builder.totalTestsExecuted;
    this.successfulTests = builder.successfulTests;
    this.failedTests = builder.failedTests;
    this.crossRuntimeValidationCompleted = builder.crossRuntimeValidationCompleted;
    this.performanceBenchmarkingCompleted = builder.performanceBenchmarkingCompleted;
    this.coverageMetrics = calculateCoverageMetrics();
  }

  /**
   * Gets all individual SIMD test results.
   *
   * @return immutable list of test results
   */
  public List<SimdTestResult> getResults() {
    return results;
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
   * Gets the total execution duration.
   *
   * @return the total duration
   */
  public Duration getTotalDuration() {
    return totalDuration;
  }

  /**
   * Gets the total number of tests executed.
   *
   * @return the total tests executed
   */
  public int getTotalTestsExecuted() {
    return totalTestsExecuted;
  }

  /**
   * Gets the number of successful tests.
   *
   * @return the number of successful tests
   */
  public int getSuccessfulTests() {
    return successfulTests;
  }

  /**
   * Gets the number of failed tests.
   *
   * @return the number of failed tests
   */
  public int getFailedTests() {
    return failedTests;
  }

  /**
   * Checks if cross-runtime validation was completed.
   *
   * @return true if cross-runtime validation was performed
   */
  public boolean isCrossRuntimeValidationCompleted() {
    return crossRuntimeValidationCompleted;
  }

  /**
   * Checks if performance benchmarking was completed.
   *
   * @return true if performance benchmarking was performed
   */
  public boolean isPerformanceBenchmarkingCompleted() {
    return performanceBenchmarkingCompleted;
  }

  /**
   * Gets the SIMD coverage metrics.
   *
   * @return the coverage metrics
   */
  public SimdCoverageMetrics getCoverageMetrics() {
    return coverageMetrics;
  }

  /**
   * Calculates the success rate as a percentage.
   *
   * @return the success rate percentage (0.0 to 100.0)
   */
  public double getSuccessRate() {
    if (totalTestsExecuted == 0) {
      return 0.0;
    }
    return (double) successfulTests / totalTestsExecuted * 100.0;
  }

  /**
   * Gets the average execution time per test.
   *
   * @return the average execution time
   */
  public Duration getAverageExecutionTime() {
    if (totalTestsExecuted == 0) {
      return Duration.ZERO;
    }

    final long totalMillis =
        results.stream().mapToLong(result -> result.getExecutionTime().toMillis()).sum();

    return Duration.ofMillis(totalMillis / totalTestsExecuted);
  }

  /**
   * Gets results filtered by category.
   *
   * @param category the category to filter by
   * @return filtered results
   */
  public List<SimdTestResult> getResultsByCategory(final String category) {
    return results.stream()
        .filter(result -> result.getCategory().equals(category))
        .collect(Collectors.toList());
  }

  /**
   * Gets only the failed test results.
   *
   * @return list of failed results
   */
  public List<SimdTestResult> getFailedResults() {
    return results.stream().filter(result -> !result.isSuccessful()).collect(Collectors.toList());
  }

  /**
   * Gets results with performance metrics.
   *
   * @return list of results with performance data
   */
  public List<SimdTestResult> getResultsWithPerformanceMetrics() {
    return results.stream()
        .filter(SimdTestResult::hasPerformanceMetrics)
        .collect(Collectors.toList());
  }

  /**
   * Gets a summary of test results by category.
   *
   * @return map of category to result counts
   */
  public Map<String, CategorySummary> getCategorySummary() {
    return results.stream()
        .collect(
            Collectors.groupingBy(
                SimdTestResult::getCategory,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    categoryResults ->
                        new CategorySummary(
                            categoryResults.size(),
                            (int)
                                categoryResults.stream()
                                    .filter(SimdTestResult::isSuccessful)
                                    .count(),
                            (int)
                                categoryResults.stream().filter(r -> !r.isSuccessful()).count()))));
  }

  /**
   * Calculates coverage metrics based on test results.
   *
   * @return the calculated coverage metrics
   */
  private SimdCoverageMetrics calculateCoverageMetrics() {
    final Map<String, CategorySummary> categorySummary = getCategorySummary();

    final int arithmeticTests =
        categorySummary.getOrDefault("arithmetic", CategorySummary.EMPTY).total;
    final int memoryTests = categorySummary.getOrDefault("memory", CategorySummary.EMPTY).total;
    final int manipulationTests =
        categorySummary.getOrDefault("manipulation", CategorySummary.EMPTY).total;

    final int arithmeticSuccessful =
        categorySummary.getOrDefault("arithmetic", CategorySummary.EMPTY).successful;
    final int memorySuccessful =
        categorySummary.getOrDefault("memory", CategorySummary.EMPTY).successful;
    final int manipulationSuccessful =
        categorySummary.getOrDefault("manipulation", CategorySummary.EMPTY).successful;

    // Calculate coverage percentages based on successful tests
    final double arithmeticCoverage =
        calculateCoveragePercentage(arithmeticSuccessful, 12); // 12 arithmetic operations
    final double memoryCoverage =
        calculateCoveragePercentage(memorySuccessful, 8); // 8 memory operations
    final double manipulationCoverage =
        calculateCoveragePercentage(manipulationSuccessful, 15); // 15 manipulation operations

    return new SimdCoverageMetrics(
        arithmeticTests,
        arithmeticSuccessful,
        arithmeticCoverage,
        memoryTests,
        memorySuccessful,
        memoryCoverage,
        manipulationTests,
        manipulationSuccessful,
        manipulationCoverage);
  }

  /**
   * Calculates coverage percentage with a maximum cap.
   *
   * @param successful number of successful tests
   * @param maxExpected maximum expected tests for this category
   * @return coverage percentage (0.0 to 100.0)
   */
  private double calculateCoveragePercentage(final int successful, final int maxExpected) {
    if (maxExpected == 0) {
      return 0.0;
    }
    return Math.min(100.0, (double) successful / maxExpected * 100.0);
  }

  /**
   * Checks if the SIMD testing meets the target coverage goals.
   *
   * @return true if target coverage is achieved (60-70%)
   */
  public boolean meetsTargetCoverage() {
    final double overallCoverage = coverageMetrics.getOverallCoverage();
    return overallCoverage >= 60.0 && overallCoverage <= 100.0;
  }

  @Override
  public String toString() {
    return String.format(
        "SimdTestResults{totalTests=%d, successful=%d, failed=%d, successRate=%.1f%%, "
            + "duration=%ds, overallCoverage=%.1f%%}",
        totalTestsExecuted,
        successfulTests,
        failedTests,
        getSuccessRate(),
        totalDuration.toSeconds(),
        coverageMetrics.getOverallCoverage());
  }

  /** Builder class for creating SimdTestResults instances. */
  public static final class Builder {
    private final List<SimdTestResult> results = new ArrayList<>();
    private Instant startTime = Instant.now();
    private Duration totalDuration = Duration.ZERO;
    private int totalTestsExecuted = 0;
    private int successfulTests = 0;
    private int failedTests = 0;
    private boolean crossRuntimeValidationCompleted = false;
    private boolean performanceBenchmarkingCompleted = false;

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
     * Adds a test result.
     *
     * @param result the test result to add
     * @return this builder
     */
    public Builder addResult(final SimdTestResult result) {
      this.results.add(Objects.requireNonNull(result, "result cannot be null"));
      return this;
    }

    /**
     * Increments the executed test counter.
     *
     * @return this builder
     */
    public Builder incrementExecuted() {
      this.totalTestsExecuted++;
      return this;
    }

    /**
     * Increments the successful test counter.
     *
     * @return this builder
     */
    public Builder incrementSuccessful() {
      this.successfulTests++;
      return this;
    }

    /**
     * Increments the failed test counter.
     *
     * @return this builder
     */
    public Builder incrementFailed() {
      this.failedTests++;
      return this;
    }

    /**
     * Sets cross-runtime validation completion status.
     *
     * @param completed true if completed
     * @return this builder
     */
    public Builder setCrossRuntimeValidationCompleted(final boolean completed) {
      this.crossRuntimeValidationCompleted = completed;
      return this;
    }

    /**
     * Sets performance benchmarking completion status.
     *
     * @param completed true if completed
     * @return this builder
     */
    public Builder setPerformanceBenchmarkingCompleted(final boolean completed) {
      this.performanceBenchmarkingCompleted = completed;
      return this;
    }

    /**
     * Builds the SimdTestResults.
     *
     * @return the configured SimdTestResults
     */
    public SimdTestResults build() {
      return new SimdTestResults(this);
    }
  }

  /** Summary statistics for a specific test category. */
  public static final class CategorySummary {
    public static final CategorySummary EMPTY = new CategorySummary(0, 0, 0);

    private final int total;
    private final int successful;
    private final int failed;

    public CategorySummary(final int total, final int successful, final int failed) {
      this.total = total;
      this.successful = successful;
      this.failed = failed;
    }

    public int getTotal() {
      return total;
    }

    public int getSuccessful() {
      return successful;
    }

    public int getFailed() {
      return failed;
    }

    public double getSuccessRate() {
      return total == 0 ? 0.0 : (double) successful / total * 100.0;
    }

    @Override
    public String toString() {
      return String.format(
          "CategorySummary{total=%d, successful=%d, failed=%d, rate=%.1f%%}",
          total, successful, failed, getSuccessRate());
    }
  }

  /** Detailed coverage metrics for SIMD operations. */
  public static final class SimdCoverageMetrics {
    private final int arithmeticTests;
    private final int arithmeticSuccessful;
    private final double arithmeticCoverage;
    private final int memoryTests;
    private final int memorySuccessful;
    private final double memoryCoverage;
    private final int manipulationTests;
    private final int manipulationSuccessful;
    private final double manipulationCoverage;

    public SimdCoverageMetrics(
        final int arithmeticTests,
        final int arithmeticSuccessful,
        final double arithmeticCoverage,
        final int memoryTests,
        final int memorySuccessful,
        final double memoryCoverage,
        final int manipulationTests,
        final int manipulationSuccessful,
        final double manipulationCoverage) {

      this.arithmeticTests = arithmeticTests;
      this.arithmeticSuccessful = arithmeticSuccessful;
      this.arithmeticCoverage = arithmeticCoverage;
      this.memoryTests = memoryTests;
      this.memorySuccessful = memorySuccessful;
      this.memoryCoverage = memoryCoverage;
      this.manipulationTests = manipulationTests;
      this.manipulationSuccessful = manipulationSuccessful;
      this.manipulationCoverage = manipulationCoverage;
    }

    public double getArithmeticCoverage() {
      return arithmeticCoverage;
    }

    public double getMemoryCoverage() {
      return memoryCoverage;
    }

    public double getManipulationCoverage() {
      return manipulationCoverage;
    }

    public double getOverallCoverage() {
      return (arithmeticCoverage + memoryCoverage + manipulationCoverage) / 3.0;
    }

    @Override
    public String toString() {
      return String.format(
          "SimdCoverageMetrics{arithmetic=%.1f%%, memory=%.1f%%, manipulation=%.1f%%,"
              + " overall=%.1f%%}",
          arithmeticCoverage, memoryCoverage, manipulationCoverage, getOverallCoverage());
    }
  }
}
