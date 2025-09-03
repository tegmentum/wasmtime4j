package ai.tegmentum.wasmtime4j.webassembly;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Comprehensive failure report for an entire WebAssembly test suite, providing aggregated
 * statistics, failure pattern analysis, and actionable recommendations for resolving issues.
 */
public final class TestSuiteFailureReport {
  private final WasmTestSuiteLoader.TestSuiteType suiteType;
  private final int totalTests;
  private final int failedTestCount;
  private final int inconsistentTestCount;
  private final List<CrossRuntimeFailureAnalysis> failureAnalyses;
  private final Instant reportTime;

  private TestSuiteFailureReport(final Builder builder) {
    this.suiteType = builder.suiteType;
    this.totalTests = builder.totalTests;
    this.failedTestCount = builder.failedTestCount;
    this.inconsistentTestCount = builder.inconsistentTestCount;
    this.failureAnalyses = Collections.unmodifiableList(new ArrayList<>(builder.failureAnalyses));
    this.reportTime = Instant.now();
  }

  /**
   * Gets the test suite type.
   *
   * @return the test suite type
   */
  public WasmTestSuiteLoader.TestSuiteType getSuiteType() {
    return suiteType;
  }

  /**
   * Gets the total number of tests in the suite.
   *
   * @return the total number of tests
   */
  public int getTotalTests() {
    return totalTests;
  }

  /**
   * Gets the number of failed tests.
   *
   * @return the number of failed tests
   */
  public int getFailedTestCount() {
    return failedTestCount;
  }

  /**
   * Gets the number of tests with inconsistent results across runtimes.
   *
   * @return the number of inconsistent tests
   */
  public int getInconsistentTestCount() {
    return inconsistentTestCount;
  }

  /**
   * Gets all failure analyses for the test suite.
   *
   * @return list of failure analyses
   */
  public List<CrossRuntimeFailureAnalysis> getFailureAnalyses() {
    return failureAnalyses;
  }

  /**
   * Gets the time when this report was generated.
   *
   * @return the report generation time
   */
  public Instant getReportTime() {
    return reportTime;
  }

  /**
   * Gets the overall failure rate as a percentage.
   *
   * @return the failure rate (0.0 to 100.0)
   */
  public double getFailureRate() {
    if (totalTests == 0) {
      return 0.0;
    }
    return (double) failedTestCount / totalTests * 100.0;
  }

  /**
   * Gets the inconsistency rate as a percentage.
   *
   * @return the inconsistency rate (0.0 to 100.0)
   */
  public double getInconsistencyRate() {
    if (totalTests == 0) {
      return 0.0;
    }
    return (double) inconsistentTestCount / totalTests * 100.0;
  }

  /**
   * Gets failure analyses filtered by inconsistency type.
   *
   * @param inconsistencyType the inconsistency type to filter by
   * @return list of filtered failure analyses
   */
  public List<CrossRuntimeFailureAnalysis> getFailuresByInconsistencyType(
      final WasmTestFailureAnalyzer.InconsistencyType inconsistencyType) {
    return failureAnalyses.stream()
        .filter(analysis -> analysis.getInconsistencyType() == inconsistencyType)
        .collect(Collectors.toList());
  }

  /**
   * Gets failure analyses that are considered critical.
   *
   * @return list of critical failure analyses
   */
  public List<CrossRuntimeFailureAnalysis> getCriticalFailures() {
    return failureAnalyses.stream()
        .filter(CrossRuntimeFailureAnalysis::isCritical)
        .collect(Collectors.toList());
  }

  /**
   * Gets the distribution of failure categories across the test suite.
   *
   * @return map of failure categories to their occurrence counts
   */
  public Map<WasmTestFailureAnalyzer.FailureCategory, Long> getFailureCategoryDistribution() {
    return failureAnalyses.stream()
        .flatMap(analysis -> analysis.getFailureCategories().stream())
        .collect(Collectors.groupingBy(category -> category, Collectors.counting()));
  }

  /**
   * Gets the distribution of inconsistency types across the test suite.
   *
   * @return map of inconsistency types to their occurrence counts
   */
  public Map<WasmTestFailureAnalyzer.InconsistencyType, Long> getInconsistencyTypeDistribution() {
    return failureAnalyses.stream()
        .collect(Collectors.groupingBy(
            CrossRuntimeFailureAnalysis::getInconsistencyType, 
            Collectors.counting()));
  }

  /**
   * Gets the most common failure category in the test suite.
   *
   * @return the most common failure category, or null if no failures
   */
  public WasmTestFailureAnalyzer.FailureCategory getMostCommonFailureCategory() {
    return getFailureCategoryDistribution().entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /**
   * Gets the most common inconsistency type in the test suite.
   *
   * @return the most common inconsistency type, or null if no inconsistencies
   */
  public WasmTestFailureAnalyzer.InconsistencyType getMostCommonInconsistencyType() {
    return getInconsistencyTypeDistribution().entrySet().stream()
        .filter(entry -> entry.getKey() != WasmTestFailureAnalyzer.InconsistencyType.NONE)
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /**
   * Gets aggregated recommendations for resolving test suite issues.
   *
   * @return list of prioritized recommendations
   */
  public List<String> getAggregatedRecommendations() {
    final List<String> recommendations = new ArrayList<>();

    // Add recommendations based on failure patterns
    final WasmTestFailureAnalyzer.FailureCategory mostCommonCategory = getMostCommonFailureCategory();
    if (mostCommonCategory != null) {
      recommendations.add(String.format(
          "Primary focus: Address %s issues (most common failure category)",
          mostCommonCategory.getDescription().toLowerCase()));
    }

    final WasmTestFailureAnalyzer.InconsistencyType mostCommonInconsistency = getMostCommonInconsistencyType();
    if (mostCommonInconsistency != null) {
      recommendations.add(String.format(
          "Cross-runtime focus: Investigate %s patterns",
          mostCommonInconsistency.getDescription().toLowerCase()));
    }

    // Add critical failure recommendations
    final List<CrossRuntimeFailureAnalysis> criticalFailures = getCriticalFailures();
    if (!criticalFailures.isEmpty()) {
      recommendations.add(String.format(
          "Immediate attention: %d critical failures require urgent resolution",
          criticalFailures.size()));
    }

    // Add suite-specific recommendations
    final double failureRate = getFailureRate();
    if (failureRate > 50.0) {
      recommendations.add("High failure rate suggests systematic issues - review test environment and module validity");
    } else if (failureRate > 20.0) {
      recommendations.add("Moderate failure rate - focus on most common failure patterns first");
    }

    final double inconsistencyRate = getInconsistencyRate();
    if (inconsistencyRate > 10.0) {
      recommendations.add("High inconsistency rate suggests runtime compatibility issues");
    }

    // Collect unique recommendations from individual failure analyses
    final List<String> individualRecommendations = failureAnalyses.stream()
        .flatMap(analysis -> analysis.getRecommendations().stream())
        .distinct()
        .collect(Collectors.toList());

    recommendations.addAll(individualRecommendations);

    return recommendations;
  }

  /**
   * Creates a comprehensive report of the test suite failures.
   *
   * @return a formatted report
   */
  public String createComprehensiveReport() {
    final StringBuilder report = new StringBuilder();
    report.append("Test Suite Failure Report\n");
    report.append("=========================\n\n");
    
    report.append(String.format("Suite: %s\n", suiteType.name()));
    report.append(String.format("Report Generated: %s\n", reportTime));
    report.append(String.format("Total Tests: %d\n", totalTests));
    report.append(String.format("Failed Tests: %d (%.1f%%)\n", failedTestCount, getFailureRate()));
    report.append(String.format("Inconsistent Tests: %d (%.1f%%)\n", inconsistentTestCount, getInconsistencyRate()));
    report.append("\n");

    // Failure category distribution
    final Map<WasmTestFailureAnalyzer.FailureCategory, Long> categoryDistribution = 
        getFailureCategoryDistribution();
    if (!categoryDistribution.isEmpty()) {
      report.append("Failure Category Distribution:\n");
      report.append("------------------------------\n");
      categoryDistribution.entrySet().stream()
          .sorted(Map.Entry.<WasmTestFailureAnalyzer.FailureCategory, Long>comparingByValue().reversed())
          .forEach(entry -> report.append(String.format("  %s: %d occurrences\n", 
              entry.getKey().getDescription(), entry.getValue())));
      report.append("\n");
    }

    // Inconsistency type distribution
    final Map<WasmTestFailureAnalyzer.InconsistencyType, Long> inconsistencyDistribution = 
        getInconsistencyTypeDistribution();
    if (!inconsistencyDistribution.isEmpty()) {
      report.append("Inconsistency Type Distribution:\n");
      report.append("--------------------------------\n");
      inconsistencyDistribution.entrySet().stream()
          .sorted(Map.Entry.<WasmTestFailureAnalyzer.InconsistencyType, Long>comparingByValue().reversed())
          .forEach(entry -> report.append(String.format("  %s: %d occurrences\n", 
              entry.getKey().getDescription(), entry.getValue())));
      report.append("\n");
    }

    // Critical failures
    final List<CrossRuntimeFailureAnalysis> criticalFailures = getCriticalFailures();
    if (!criticalFailures.isEmpty()) {
      report.append("Critical Failures (Requiring Immediate Attention):\n");
      report.append("--------------------------------------------------\n");
      for (final CrossRuntimeFailureAnalysis analysis : criticalFailures) {
        report.append(String.format("  %s: %s\n", 
            analysis.getTestName(), 
            analysis.getSummary()));
      }
      report.append("\n");
    }

    // Aggregated recommendations
    final List<String> recommendations = getAggregatedRecommendations();
    if (!recommendations.isEmpty()) {
      report.append("Recommendations (Prioritized):\n");
      report.append("------------------------------\n");
      for (int i = 0; i < recommendations.size(); i++) {
        report.append(String.format("%d. %s\n", i + 1, recommendations.get(i)));
      }
      report.append("\n");
    }

    // Individual failure details (summary only for space)
    if (!failureAnalyses.isEmpty()) {
      report.append("Individual Failure Summary:\n");
      report.append("---------------------------\n");
      for (final CrossRuntimeFailureAnalysis analysis : failureAnalyses) {
        report.append(String.format("  %s\n", analysis.createBriefSummary()));
      }
    }

    return report.toString();
  }

  /**
   * Creates a brief summary suitable for logging or console output.
   *
   * @return a brief summary
   */
  public String createBriefSummary() {
    return String.format("[%s] %d/%d tests failed (%.1f%%), %d inconsistent", 
        suiteType.name(), 
        failedTestCount, 
        totalTests, 
        getFailureRate(),
        inconsistentTestCount);
  }

  @Override
  public String toString() {
    return String.format("TestSuiteFailureReport{suite=%s, total=%d, failed=%d, inconsistent=%d}", 
        suiteType.name(), totalTests, failedTestCount, inconsistentTestCount);
  }

  /**
   * Builder for TestSuiteFailureReport.
   */
  public static final class Builder {
    private final WasmTestSuiteLoader.TestSuiteType suiteType;
    private int totalTests = 0;
    private int failedTestCount = 0;
    private int inconsistentTestCount = 0;
    private final List<CrossRuntimeFailureAnalysis> failureAnalyses = new ArrayList<>();

    /**
     * Creates a builder for the specified test suite type.
     *
     * @param suiteType the test suite type
     */
    public Builder(final WasmTestSuiteLoader.TestSuiteType suiteType) {
      this.suiteType = Objects.requireNonNull(suiteType, "suiteType cannot be null");
    }

    /**
     * Sets the total number of tests.
     *
     * @param totalTests the total number of tests
     * @return this builder
     */
    public Builder totalTests(final int totalTests) {
      if (totalTests < 0) {
        throw new IllegalArgumentException("totalTests cannot be negative");
      }
      this.totalTests = totalTests;
      return this;
    }

    /**
     * Sets the number of failed tests.
     *
     * @param failedTestCount the number of failed tests
     * @return this builder
     */
    public Builder failedTestCount(final int failedTestCount) {
      if (failedTestCount < 0) {
        throw new IllegalArgumentException("failedTestCount cannot be negative");
      }
      this.failedTestCount = failedTestCount;
      return this;
    }

    /**
     * Sets the number of inconsistent tests.
     *
     * @param inconsistentTestCount the number of inconsistent tests
     * @return this builder
     */
    public Builder inconsistentTestCount(final int inconsistentTestCount) {
      if (inconsistentTestCount < 0) {
        throw new IllegalArgumentException("inconsistentTestCount cannot be negative");
      }
      this.inconsistentTestCount = inconsistentTestCount;
      return this;
    }

    /**
     * Adds a failure analysis to the report.
     *
     * @param analysis the failure analysis to add
     * @return this builder
     */
    public Builder addFailureAnalysis(final CrossRuntimeFailureAnalysis analysis) {
      Objects.requireNonNull(analysis, "analysis cannot be null");
      this.failureAnalyses.add(analysis);
      return this;
    }

    /**
     * Builds the test suite failure report.
     *
     * @return the test suite failure report
     */
    public TestSuiteFailureReport build() {
      return new TestSuiteFailureReport(this);
    }
  }
}