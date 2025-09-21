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
 * Comprehensive results container for Threading and Atomic operations test execution, providing
 * detailed statistics, thread safety analysis, and performance metrics.
 *
 * <p>This class aggregates individual threading test results to provide insights into overall test
 * coverage, concurrency safety, and atomic operation performance across different threading
 * operation categories.
 */
public final class ThreadingTestResults {
  private final List<ThreadingTestResult> results;
  private final Instant startTime;
  private final Duration totalDuration;
  private final int totalTestsExecuted;
  private final int successfulTests;
  private final int failedTests;
  private final boolean crossRuntimeValidationCompleted;
  private final boolean performanceBenchmarkingCompleted;
  private final ThreadingCoverageMetrics coverageMetrics;

  private ThreadingTestResults(final Builder builder) {
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
   * Gets all individual threading test results.
   *
   * @return immutable list of test results
   */
  public List<ThreadingTestResult> getResults() {
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
   * Gets the threading coverage metrics.
   *
   * @return the coverage metrics
   */
  public ThreadingCoverageMetrics getCoverageMetrics() {
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
  public List<ThreadingTestResult> getResultsByCategory(final String category) {
    return results.stream()
        .filter(result -> result.getCategory().equals(category))
        .collect(Collectors.toList());
  }

  /**
   * Gets only the failed test results.
   *
   * @return list of failed results
   */
  public List<ThreadingTestResult> getFailedResults() {
    return results.stream().filter(result -> !result.isSuccessful()).collect(Collectors.toList());
  }

  /**
   * Gets results with performance metrics.
   *
   * @return list of results with performance data
   */
  public List<ThreadingTestResult> getResultsWithPerformanceMetrics() {
    return results.stream()
        .filter(ThreadingTestResult::hasPerformanceMetrics)
        .collect(Collectors.toList());
  }

  /**
   * Gets concurrent test results (thread safety validation).
   *
   * @return list of concurrent test results
   */
  public List<ThreadingTestResult> getConcurrentTestResults() {
    return results.stream()
        .filter(ThreadingTestResult::isConcurrentTest)
        .collect(Collectors.toList());
  }

  /**
   * Gets atomic operation test results.
   *
   * @return list of atomic operation test results
   */
  public List<ThreadingTestResult> getAtomicOperationResults() {
    return results.stream()
        .filter(ThreadingTestResult::isAtomicOperationTest)
        .collect(Collectors.toList());
  }

  /**
   * Gets shared memory test results.
   *
   * @return list of shared memory test results
   */
  public List<ThreadingTestResult> getSharedMemoryResults() {
    return results.stream()
        .filter(ThreadingTestResult::isSharedMemoryTest)
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
                ThreadingTestResult::getCategory,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    categoryResults ->
                        new CategorySummary(
                            categoryResults.size(),
                            (int)
                                categoryResults.stream()
                                    .filter(ThreadingTestResult::isSuccessful)
                                    .count(),
                            (int)
                                categoryResults.stream().filter(r -> !r.isSuccessful()).count()))));
  }

  /**
   * Calculates coverage metrics based on test results.
   *
   * @return the calculated coverage metrics
   */
  private ThreadingCoverageMetrics calculateCoverageMetrics() {
    final Map<String, CategorySummary> categorySummary = getCategorySummary();

    final int atomicTests =
        categorySummary.getOrDefault("atomic-operations", CategorySummary.EMPTY).total;
    final int casTests =
        categorySummary.getOrDefault("compare-and-swap", CategorySummary.EMPTY).total;
    final int sharedMemoryTests =
        categorySummary.getOrDefault("shared-memory", CategorySummary.EMPTY).total;
    final int memoryOrderingTests =
        categorySummary.getOrDefault("memory-ordering", CategorySummary.EMPTY).total;
    final int threadSafetyTests =
        categorySummary.getOrDefault("thread-safety", CategorySummary.EMPTY).total;

    final int atomicSuccessful =
        categorySummary.getOrDefault("atomic-operations", CategorySummary.EMPTY).successful;
    final int casSuccessful =
        categorySummary.getOrDefault("compare-and-swap", CategorySummary.EMPTY).successful;
    final int sharedMemorySuccessful =
        categorySummary.getOrDefault("shared-memory", CategorySummary.EMPTY).successful;
    final int memoryOrderingSuccessful =
        categorySummary.getOrDefault("memory-ordering", CategorySummary.EMPTY).successful;
    final int threadSafetySuccessful =
        categorySummary.getOrDefault("thread-safety", CategorySummary.EMPTY).successful;

    // Calculate coverage percentages based on successful tests
    final double atomicCoverage =
        calculateCoveragePercentage(atomicSuccessful, 16); // 16 atomic operations
    final double casCoverage = calculateCoveragePercentage(casSuccessful, 4); // 4 CAS operations
    final double sharedMemoryCoverage =
        calculateCoveragePercentage(sharedMemorySuccessful, 6); // 6 shared memory operations
    final double memoryOrderingCoverage =
        calculateCoveragePercentage(memoryOrderingSuccessful, 4); // 4 ordering types
    final double threadSafetyCoverage =
        calculateCoveragePercentage(threadSafetySuccessful, 10); // 10 thread safety scenarios

    return new ThreadingCoverageMetrics(
        atomicTests,
        atomicSuccessful,
        atomicCoverage,
        casTests,
        casSuccessful,
        casCoverage,
        sharedMemoryTests,
        sharedMemorySuccessful,
        sharedMemoryCoverage,
        memoryOrderingTests,
        memoryOrderingSuccessful,
        memoryOrderingCoverage,
        threadSafetyTests,
        threadSafetySuccessful,
        threadSafetyCoverage);
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
   * Checks if the threading testing meets the target coverage goals.
   *
   * @return true if target coverage is achieved (50-60%)
   */
  public boolean meetsTargetCoverage() {
    final double overallCoverage = coverageMetrics.getOverallCoverage();
    return overallCoverage >= 50.0 && overallCoverage <= 100.0;
  }

  @Override
  public String toString() {
    return String.format(
        "ThreadingTestResults{totalTests=%d, successful=%d, failed=%d, successRate=%.1f%%, "
            + "duration=%ds, overallCoverage=%.1f%%, concurrentTests=%d}",
        totalTestsExecuted,
        successfulTests,
        failedTests,
        getSuccessRate(),
        totalDuration.toSeconds(),
        coverageMetrics.getOverallCoverage(),
        getConcurrentTestResults().size());
  }

  /** Builder class for creating ThreadingTestResults instances. */
  public static final class Builder {
    private final List<ThreadingTestResult> results = new ArrayList<>();
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
    public Builder addResult(final ThreadingTestResult result) {
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
     * Builds the ThreadingTestResults.
     *
     * @return the configured ThreadingTestResults
     */
    public ThreadingTestResults build() {
      return new ThreadingTestResults(this);
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

  /** Detailed coverage metrics for threading and atomic operations. */
  public static final class ThreadingCoverageMetrics {
    private final int atomicTests;
    private final int atomicSuccessful;
    private final double atomicCoverage;
    private final int casTests;
    private final int casSuccessful;
    private final double casCoverage;
    private final int sharedMemoryTests;
    private final int sharedMemorySuccessful;
    private final double sharedMemoryCoverage;
    private final int memoryOrderingTests;
    private final int memoryOrderingSuccessful;
    private final double memoryOrderingCoverage;
    private final int threadSafetyTests;
    private final int threadSafetySuccessful;
    private final double threadSafetyCoverage;

    public ThreadingCoverageMetrics(
        final int atomicTests,
        final int atomicSuccessful,
        final double atomicCoverage,
        final int casTests,
        final int casSuccessful,
        final double casCoverage,
        final int sharedMemoryTests,
        final int sharedMemorySuccessful,
        final double sharedMemoryCoverage,
        final int memoryOrderingTests,
        final int memoryOrderingSuccessful,
        final double memoryOrderingCoverage,
        final int threadSafetyTests,
        final int threadSafetySuccessful,
        final double threadSafetyCoverage) {

      this.atomicTests = atomicTests;
      this.atomicSuccessful = atomicSuccessful;
      this.atomicCoverage = atomicCoverage;
      this.casTests = casTests;
      this.casSuccessful = casSuccessful;
      this.casCoverage = casCoverage;
      this.sharedMemoryTests = sharedMemoryTests;
      this.sharedMemorySuccessful = sharedMemorySuccessful;
      this.sharedMemoryCoverage = sharedMemoryCoverage;
      this.memoryOrderingTests = memoryOrderingTests;
      this.memoryOrderingSuccessful = memoryOrderingSuccessful;
      this.memoryOrderingCoverage = memoryOrderingCoverage;
      this.threadSafetyTests = threadSafetyTests;
      this.threadSafetySuccessful = threadSafetySuccessful;
      this.threadSafetyCoverage = threadSafetyCoverage;
    }

    public double getAtomicCoverage() {
      return atomicCoverage;
    }

    public double getCasCoverage() {
      return casCoverage;
    }

    public double getSharedMemoryCoverage() {
      return sharedMemoryCoverage;
    }

    public double getMemoryOrderingCoverage() {
      return memoryOrderingCoverage;
    }

    public double getThreadSafetyCoverage() {
      return threadSafetyCoverage;
    }

    public double getOverallCoverage() {
      return (atomicCoverage
              + casCoverage
              + sharedMemoryCoverage
              + memoryOrderingCoverage
              + threadSafetyCoverage)
          / 5.0;
    }

    @Override
    public String toString() {
      return String.format(
          "ThreadingCoverageMetrics{atomic=%.1f%%, cas=%.1f%%, sharedMem=%.1f%%, ordering=%.1f%%,"
              + " safety=%.1f%%, overall=%.1f%%}",
          atomicCoverage,
          casCoverage,
          sharedMemoryCoverage,
          memoryOrderingCoverage,
          threadSafetyCoverage,
          getOverallCoverage());
    }
  }
}
