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
 * Comprehensive results container for Exception handling test execution, providing detailed
 * statistics, exception flow analysis, and performance metrics.
 *
 * <p>This class aggregates individual exception test results to provide insights into overall test
 * coverage, exception handling reliability, and performance characteristics across different
 * exception operation categories.
 */
public final class ExceptionTestResults {
  private final List<ExceptionTestResult> results;
  private final Instant startTime;
  private final Duration totalDuration;
  private final int totalTestsExecuted;
  private final int successfulTests;
  private final int failedTests;
  private final boolean crossRuntimeValidationCompleted;
  private final boolean performanceBenchmarkingCompleted;
  private final ExceptionCoverageMetrics coverageMetrics;

  private ExceptionTestResults(final Builder builder) {
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
   * Gets all individual exception test results.
   *
   * @return immutable list of test results
   */
  public List<ExceptionTestResult> getResults() {
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
   * Gets the exception coverage metrics.
   *
   * @return the coverage metrics
   */
  public ExceptionCoverageMetrics getCoverageMetrics() {
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
  public List<ExceptionTestResult> getResultsByCategory(final String category) {
    return results.stream()
        .filter(result -> result.getCategory().equals(category))
        .collect(Collectors.toList());
  }

  /**
   * Gets only the failed test results.
   *
   * @return list of failed results
   */
  public List<ExceptionTestResult> getFailedResults() {
    return results.stream().filter(result -> !result.isSuccessful()).collect(Collectors.toList());
  }

  /**
   * Gets results with performance metrics.
   *
   * @return list of results with performance data
   */
  public List<ExceptionTestResult> getResultsWithPerformanceMetrics() {
    return results.stream()
        .filter(ExceptionTestResult::hasPerformanceMetrics)
        .collect(Collectors.toList());
  }

  /**
   * Gets basic exception test results (try/catch/throw).
   *
   * @return list of basic exception test results
   */
  public List<ExceptionTestResult> getBasicExceptionResults() {
    return results.stream()
        .filter(ExceptionTestResult::isBasicExceptionTest)
        .collect(Collectors.toList());
  }

  /**
   * Gets exception type test results.
   *
   * @return list of exception type test results
   */
  public List<ExceptionTestResult> getExceptionTypeResults() {
    return results.stream()
        .filter(ExceptionTestResult::isExceptionTypeTest)
        .collect(Collectors.toList());
  }

  /**
   * Gets nested exception test results.
   *
   * @return list of nested exception test results
   */
  public List<ExceptionTestResult> getNestedExceptionResults() {
    return results.stream()
        .filter(ExceptionTestResult::isNestedExceptionTest)
        .collect(Collectors.toList());
  }

  /**
   * Gets cross-module exception test results.
   *
   * @return list of cross-module exception test results
   */
  public List<ExceptionTestResult> getCrossModuleResults() {
    return results.stream()
        .filter(ExceptionTestResult::isCrossModuleTest)
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
                ExceptionTestResult::getCategory,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    categoryResults ->
                        new CategorySummary(
                            categoryResults.size(),
                            (int)
                                categoryResults.stream()
                                    .filter(ExceptionTestResult::isSuccessful)
                                    .count(),
                            (int)
                                categoryResults.stream().filter(r -> !r.isSuccessful()).count()))));
  }

  /**
   * Calculates coverage metrics based on test results.
   *
   * @return the calculated coverage metrics
   */
  private ExceptionCoverageMetrics calculateCoverageMetrics() {
    final Map<String, CategorySummary> categorySummary = getCategorySummary();

    final int basicTests =
        categorySummary.getOrDefault("basic-exceptions", CategorySummary.EMPTY).total;
    final int typeTests =
        categorySummary.getOrDefault("exception-types", CategorySummary.EMPTY).total;
    final int nestedTests =
        categorySummary.getOrDefault("nested-exceptions", CategorySummary.EMPTY).total;
    final int crossModuleTests =
        categorySummary.getOrDefault("cross-module-exceptions", CategorySummary.EMPTY).total;

    final int basicSuccessful =
        categorySummary.getOrDefault("basic-exceptions", CategorySummary.EMPTY).successful;
    final int typeSuccessful =
        categorySummary.getOrDefault("exception-types", CategorySummary.EMPTY).successful;
    final int nestedSuccessful =
        categorySummary.getOrDefault("nested-exceptions", CategorySummary.EMPTY).successful;
    final int crossModuleSuccessful =
        categorySummary.getOrDefault("cross-module-exceptions", CategorySummary.EMPTY).successful;

    // Calculate coverage percentages based on successful tests
    final double basicCoverage =
        calculateCoveragePercentage(basicSuccessful, 8); // 8 basic exception operations
    final double typeCoverage =
        calculateCoveragePercentage(typeSuccessful, 4); // 4 exception type operations
    final double nestedCoverage =
        calculateCoveragePercentage(nestedSuccessful, 6); // 6 nested scenarios
    final double crossModuleCoverage =
        calculateCoveragePercentage(crossModuleSuccessful, 4); // 4 cross-module scenarios

    return new ExceptionCoverageMetrics(
        basicTests,
        basicSuccessful,
        basicCoverage,
        typeTests,
        typeSuccessful,
        typeCoverage,
        nestedTests,
        nestedSuccessful,
        nestedCoverage,
        crossModuleTests,
        crossModuleSuccessful,
        crossModuleCoverage);
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
   * Checks if the exception testing meets the target coverage goals.
   *
   * @return true if target coverage is achieved (70-80%)
   */
  public boolean meetsTargetCoverage() {
    final double overallCoverage = coverageMetrics.getOverallCoverage();
    return overallCoverage >= 70.0 && overallCoverage <= 100.0;
  }

  @Override
  public String toString() {
    return String.format(
        "ExceptionTestResults{totalTests=%d, successful=%d, failed=%d, successRate=%.1f%%, "
            + "duration=%ds, overallCoverage=%.1f%%, performanceTests=%d}",
        totalTestsExecuted,
        successfulTests,
        failedTests,
        getSuccessRate(),
        totalDuration.toSeconds(),
        coverageMetrics.getOverallCoverage(),
        getResultsWithPerformanceMetrics().size());
  }

  /** Builder class for creating ExceptionTestResults instances. */
  public static final class Builder {
    private final List<ExceptionTestResult> results = new ArrayList<>();
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
    public Builder addResult(final ExceptionTestResult result) {
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
     * Builds the ExceptionTestResults.
     *
     * @return the configured ExceptionTestResults
     */
    public ExceptionTestResults build() {
      return new ExceptionTestResults(this);
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

  /** Detailed coverage metrics for exception handling operations. */
  public static final class ExceptionCoverageMetrics {
    private final int basicTests;
    private final int basicSuccessful;
    private final double basicCoverage;
    private final int typeTests;
    private final int typeSuccessful;
    private final double typeCoverage;
    private final int nestedTests;
    private final int nestedSuccessful;
    private final double nestedCoverage;
    private final int crossModuleTests;
    private final int crossModuleSuccessful;
    private final double crossModuleCoverage;

    public ExceptionCoverageMetrics(
        final int basicTests,
        final int basicSuccessful,
        final double basicCoverage,
        final int typeTests,
        final int typeSuccessful,
        final double typeCoverage,
        final int nestedTests,
        final int nestedSuccessful,
        final double nestedCoverage,
        final int crossModuleTests,
        final int crossModuleSuccessful,
        final double crossModuleCoverage) {

      this.basicTests = basicTests;
      this.basicSuccessful = basicSuccessful;
      this.basicCoverage = basicCoverage;
      this.typeTests = typeTests;
      this.typeSuccessful = typeSuccessful;
      this.typeCoverage = typeCoverage;
      this.nestedTests = nestedTests;
      this.nestedSuccessful = nestedSuccessful;
      this.nestedCoverage = nestedCoverage;
      this.crossModuleTests = crossModuleTests;
      this.crossModuleSuccessful = crossModuleSuccessful;
      this.crossModuleCoverage = crossModuleCoverage;
    }

    public double getBasicCoverage() {
      return basicCoverage;
    }

    public double getTypeCoverage() {
      return typeCoverage;
    }

    public double getNestedCoverage() {
      return nestedCoverage;
    }

    public double getCrossModuleCoverage() {
      return crossModuleCoverage;
    }

    public double getOverallCoverage() {
      return (basicCoverage + typeCoverage + nestedCoverage + crossModuleCoverage) / 4.0;
    }

    @Override
    public String toString() {
      return String.format(
          "ExceptionCoverageMetrics{basic=%.1f%%, types=%.1f%%, nested=%.1f%%, crossModule=%.1f%%,"
              + " overall=%.1f%%}",
          basicCoverage, typeCoverage, nestedCoverage, crossModuleCoverage, getOverallCoverage());
    }
  }
}
