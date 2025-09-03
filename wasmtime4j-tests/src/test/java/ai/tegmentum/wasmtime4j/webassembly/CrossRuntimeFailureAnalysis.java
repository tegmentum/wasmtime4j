package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Analysis of test failures across multiple WebAssembly runtimes, identifying inconsistencies
 * and providing comparative analysis for debugging cross-runtime issues.
 */
public final class CrossRuntimeFailureAnalysis {
  private final String testName;
  private final List<RuntimeType> successfulRuntimes;
  private final List<RuntimeType> failedRuntimes;
  private final Map<RuntimeType, TestFailureAnalysis> runtimeAnalyses;
  private final WasmTestFailureAnalyzer.InconsistencyType inconsistencyType;
  private final String summary;
  private final Instant analysisTime;

  private CrossRuntimeFailureAnalysis(final Builder builder) {
    this.testName = builder.testName;
    this.successfulRuntimes = Collections.unmodifiableList(builder.successfulRuntimes);
    this.failedRuntimes = Collections.unmodifiableList(builder.failedRuntimes);
    this.runtimeAnalyses = Collections.unmodifiableMap(new HashMap<>(builder.runtimeAnalyses));
    this.inconsistencyType = builder.inconsistencyType;
    this.summary = builder.summary;
    this.analysisTime = Instant.now();
  }

  /**
   * Gets the test name.
   *
   * @return the test name
   */
  public String getTestName() {
    return testName;
  }

  /**
   * Gets the runtimes where the test succeeded.
   *
   * @return list of successful runtimes
   */
  public List<RuntimeType> getSuccessfulRuntimes() {
    return successfulRuntimes;
  }

  /**
   * Gets the runtimes where the test failed.
   *
   * @return list of failed runtimes
   */
  public List<RuntimeType> getFailedRuntimes() {
    return failedRuntimes;
  }

  /**
   * Gets the detailed failure analyses for each failed runtime.
   *
   * @return map of runtime failure analyses
   */
  public Map<RuntimeType, TestFailureAnalysis> getRuntimeAnalyses() {
    return runtimeAnalyses;
  }

  /**
   * Gets the failure analysis for a specific runtime.
   *
   * @param runtimeType the runtime type
   * @return the failure analysis, or null if runtime didn't fail or wasn't analyzed
   */
  public TestFailureAnalysis getAnalysisForRuntime(final RuntimeType runtimeType) {
    return runtimeAnalyses.get(runtimeType);
  }

  /**
   * Gets the type of inconsistency detected across runtimes.
   *
   * @return the inconsistency type
   */
  public WasmTestFailureAnalyzer.InconsistencyType getInconsistencyType() {
    return inconsistencyType;
  }

  /**
   * Gets a summary of the cross-runtime analysis.
   *
   * @return the analysis summary
   */
  public String getSummary() {
    return summary;
  }

  /**
   * Gets the time when this analysis was performed.
   *
   * @return the analysis time
   */
  public Instant getAnalysisTime() {
    return analysisTime;
  }

  /**
   * Checks if there are inconsistencies between runtimes.
   *
   * @return true if inconsistencies exist
   */
  public boolean hasInconsistencies() {
    return inconsistencyType != WasmTestFailureAnalyzer.InconsistencyType.NONE;
  }

  /**
   * Checks if this represents a critical cross-runtime issue.
   *
   * @return true if any runtime has a critical failure
   */
  public boolean isCritical() {
    return runtimeAnalyses.values().stream().anyMatch(TestFailureAnalysis::isCritical);
  }

  /**
   * Gets the set of all failure categories encountered across runtimes.
   *
   * @return set of failure categories
   */
  public Set<WasmTestFailureAnalyzer.FailureCategory> getFailureCategories() {
    return runtimeAnalyses.values().stream()
        .map(TestFailureAnalysis::getCategory)
        .collect(Collectors.toSet());
  }

  /**
   * Gets the most common failure category across runtimes.
   *
   * @return the most common failure category, or null if no failures
   */
  public WasmTestFailureAnalyzer.FailureCategory getMostCommonFailureCategory() {
    if (runtimeAnalyses.isEmpty()) {
      return null;
    }

    return runtimeAnalyses.values().stream()
        .collect(Collectors.groupingBy(TestFailureAnalysis::getCategory, Collectors.counting()))
        .entrySet()
        .stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(null);
  }

  /**
   * Checks if all failed runtimes have the same failure category.
   *
   * @return true if failure categories are consistent
   */
  public boolean hasConsistentFailureCategories() {
    final Set<WasmTestFailureAnalyzer.FailureCategory> categories = getFailureCategories();
    return categories.size() <= 1;
  }

  /**
   * Gets recommendations for resolving the cross-runtime issues.
   *
   * @return list of recommendations
   */
  public List<String> getRecommendations() {
    final List<String> recommendations = runtimeAnalyses.values().stream()
        .map(TestFailureAnalysis::getRecommendation)
        .distinct()
        .collect(Collectors.toList());

    // Add cross-runtime specific recommendations
    if (hasInconsistencies()) {
      switch (inconsistencyType) {
        case PARTIAL_FAILURE:
          recommendations.add("Compare successful runtime behavior with failed runtimes");
          recommendations.add("Check for runtime-specific configuration differences");
          break;
        case DIFFERENT_FAILURES:
          recommendations.add("Investigate runtime implementation differences");
          recommendations.add("Consider WebAssembly specification compatibility issues");
          break;
        case CONSISTENT_FAILURE:
          recommendations.add("Focus on the WebAssembly module or test case itself");
          recommendations.add("The issue is likely not runtime-specific");
          break;
        default:
          break;
      }
    }

    return recommendations;
  }

  /**
   * Creates a comprehensive report of the cross-runtime analysis.
   *
   * @return a formatted report
   */
  public String createReport() {
    final StringBuilder report = new StringBuilder();
    report.append("Cross-Runtime Failure Analysis\n");
    report.append("==============================\n\n");
    
    report.append(String.format("Test Name: %s\n", testName));
    report.append(String.format("Inconsistency Type: %s\n", inconsistencyType.getDescription()));
    report.append(String.format("Summary: %s\n", summary));
    report.append(String.format("Analysis Time: %s\n\n", analysisTime));

    // Runtime status overview
    report.append("Runtime Status:\n");
    report.append("---------------\n");
    if (!successfulRuntimes.isEmpty()) {
      report.append(String.format("Successful: %s\n", 
          successfulRuntimes.stream().map(Enum::name).collect(Collectors.joining(", "))));
    }
    if (!failedRuntimes.isEmpty()) {
      report.append(String.format("Failed: %s\n", 
          failedRuntimes.stream().map(Enum::name).collect(Collectors.joining(", "))));
    }
    report.append("\n");

    // Detailed failure analysis for each runtime
    if (!runtimeAnalyses.isEmpty()) {
      report.append("Detailed Failure Analysis:\n");
      report.append("--------------------------\n");
      for (final Map.Entry<RuntimeType, TestFailureAnalysis> entry : runtimeAnalyses.entrySet()) {
        final RuntimeType runtime = entry.getKey();
        final TestFailureAnalysis analysis = entry.getValue();
        
        report.append(String.format("\n%s Runtime:\n", runtime.name()));
        report.append(String.format("  Category: %s\n", analysis.getCategory().getDescription()));
        report.append(String.format("  Summary: %s\n", analysis.getSummary()));
        report.append(String.format("  Execution Time: %.3fs\n", analysis.getExecutionTime().toMillis() / 1000.0));
        
        analysis.getExceptionType().ifPresent(type -> 
            report.append(String.format("  Exception: %s\n", type)));
        analysis.getExceptionMessage().ifPresent(message -> 
            report.append(String.format("  Message: %s\n", message)));
      }
      report.append("\n");
    }

    // Recommendations
    final List<String> recommendations = getRecommendations();
    if (!recommendations.isEmpty()) {
      report.append("Recommendations:\n");
      report.append("----------------\n");
      for (int i = 0; i < recommendations.size(); i++) {
        report.append(String.format("%d. %s\n", i + 1, recommendations.get(i)));
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
    final String status;
    if (!successfulRuntimes.isEmpty() && !failedRuntimes.isEmpty()) {
      status = String.format("Mixed results: %d successful, %d failed", 
          successfulRuntimes.size(), failedRuntimes.size());
    } else if (!failedRuntimes.isEmpty()) {
      status = String.format("Failed on all %d runtimes", failedRuntimes.size());
    } else {
      status = "All runtimes successful";
    }
    
    return String.format("[Cross-Runtime] %s: %s", testName, status);
  }

  @Override
  public String toString() {
    return String.format("CrossRuntimeFailureAnalysis{test=%s, inconsistency=%s, successful=%d, failed=%d}", 
        testName, inconsistencyType.name(), successfulRuntimes.size(), failedRuntimes.size());
  }

  /**
   * Builder for CrossRuntimeFailureAnalysis.
   */
  public static final class Builder {
    private final String testName;
    private List<RuntimeType> successfulRuntimes = Collections.emptyList();
    private List<RuntimeType> failedRuntimes = Collections.emptyList();
    private final Map<RuntimeType, TestFailureAnalysis> runtimeAnalyses = new HashMap<>();
    private WasmTestFailureAnalyzer.InconsistencyType inconsistencyType = 
        WasmTestFailureAnalyzer.InconsistencyType.NONE;
    private String summary = "No analysis summary available";

    /**
     * Creates a builder for the specified test name.
     *
     * @param testName the test name
     */
    public Builder(final String testName) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    }

    /**
     * Sets the successful runtimes.
     *
     * @param successfulRuntimes the successful runtimes
     * @return this builder
     */
    public Builder successfulRuntimes(final List<RuntimeType> successfulRuntimes) {
      this.successfulRuntimes = Objects.requireNonNull(successfulRuntimes, "successfulRuntimes cannot be null");
      return this;
    }

    /**
     * Sets the failed runtimes.
     *
     * @param failedRuntimes the failed runtimes
     * @return this builder
     */
    public Builder failedRuntimes(final List<RuntimeType> failedRuntimes) {
      this.failedRuntimes = Objects.requireNonNull(failedRuntimes, "failedRuntimes cannot be null");
      return this;
    }

    /**
     * Sets the runtime analyses.
     *
     * @param runtimeAnalyses the runtime analyses
     * @return this builder
     */
    public Builder runtimeAnalyses(final Map<RuntimeType, TestFailureAnalysis> runtimeAnalyses) {
      Objects.requireNonNull(runtimeAnalyses, "runtimeAnalyses cannot be null");
      this.runtimeAnalyses.clear();
      this.runtimeAnalyses.putAll(runtimeAnalyses);
      return this;
    }

    /**
     * Sets the inconsistency type.
     *
     * @param inconsistencyType the inconsistency type
     * @return this builder
     */
    public Builder inconsistencyType(final WasmTestFailureAnalyzer.InconsistencyType inconsistencyType) {
      this.inconsistencyType = Objects.requireNonNull(inconsistencyType, "inconsistencyType cannot be null");
      return this;
    }

    /**
     * Sets the analysis summary.
     *
     * @param summary the analysis summary
     * @return this builder
     */
    public Builder summary(final String summary) {
      this.summary = Objects.requireNonNull(summary, "summary cannot be null");
      return this;
    }

    /**
     * Builds the cross-runtime failure analysis.
     *
     * @return the cross-runtime failure analysis
     */
    public CrossRuntimeFailureAnalysis build() {
      return new CrossRuntimeFailureAnalysis(this);
    }
  }
}