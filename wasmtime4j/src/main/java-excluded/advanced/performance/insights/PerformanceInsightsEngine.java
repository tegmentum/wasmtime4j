package ai.tegmentum.wasmtime4j.performance.insights;

import ai.tegmentum.wasmtime4j.performance.GcImpactMetrics;
import ai.tegmentum.wasmtime4j.performance.ProfileSnapshot;
import ai.tegmentum.wasmtime4j.performance.insights.PerformanceInsights.OptimizationOpportunity;
import ai.tegmentum.wasmtime4j.performance.insights.PerformanceInsights.PerformanceIssue;
import ai.tegmentum.wasmtime4j.performance.insights.PerformanceInsights.PerformanceRecommendation;
import ai.tegmentum.wasmtime4j.performance.insights.PerformanceInsights.PerformanceSummary;
import ai.tegmentum.wasmtime4j.performance.memory.MemoryAnalysisResult;
import ai.tegmentum.wasmtime4j.performance.microbench.BenchmarkSuite;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Intelligent performance insights engine that analyzes performance data and provides actionable
 * recommendations for WebAssembly optimization.
 *
 * <p>This engine combines multiple data sources to provide comprehensive performance insights:
 *
 * <ul>
 *   <li>Memory analysis results and patterns
 *   <li>Garbage collection impact analysis
 *   <li>Performance benchmark results
 *   <li>Profile snapshots and trends
 *   <li>Runtime-specific characteristics
 * </ul>
 *
 * <p>The engine applies machine learning-like pattern recognition and rule-based analysis to
 * identify optimization opportunities and generate specific, actionable recommendations.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * PerformanceInsightsEngine engine = PerformanceInsightsEngine.builder()
 *     .withMemoryAnalysis(true)
 *     .withGcAnalysis(true)
 *     .withBenchmarkAnalysis(true)
 *     .build();
 *
 * // Add performance data
 * engine.addMemoryAnalysis(memoryResult);
 * engine.addGcMetrics(gcMetrics);
 * engine.addBenchmarkResults(benchmarkResults);
 *
 * // Generate insights
 * PerformanceInsights insights = engine.generateInsights();
 *
 * // Get recommendations
 * List<PerformanceRecommendation> recommendations = insights.getRecommendations();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class PerformanceInsightsEngine {
  private static final Logger LOGGER = Logger.getLogger(PerformanceInsightsEngine.class.getName());

  private static final double HIGH_GC_OVERHEAD_THRESHOLD = 5.0; // 5%
  private static final double HIGH_ALLOCATION_RATE_THRESHOLD = 50.0; // 50 MB/s
  private static final double LOW_EFFICIENCY_THRESHOLD = 70.0; // 70/100
  private static final double SIGNIFICANT_PERFORMANCE_DIFFERENCE = 0.15; // 15%

  private final boolean memoryAnalysisEnabled;
  private final boolean gcAnalysisEnabled;
  private final boolean benchmarkAnalysisEnabled;
  private final boolean runtimeComparisonEnabled;

  private final List<MemoryAnalysisResult> memoryResults;
  private final List<GcImpactMetrics> gcMetrics;
  private final List<BenchmarkSuite.BenchmarkResults> benchmarkResults;
  private final List<ProfileSnapshot> profileSnapshots;
  private final Map<String, Object> runtimeCharacteristics;

  private PerformanceInsightsEngine(final Builder builder) {
    this.memoryAnalysisEnabled = builder.memoryAnalysisEnabled;
    this.gcAnalysisEnabled = builder.gcAnalysisEnabled;
    this.benchmarkAnalysisEnabled = builder.benchmarkAnalysisEnabled;
    this.runtimeComparisonEnabled = builder.runtimeComparisonEnabled;

    this.memoryResults = new ArrayList<>();
    this.gcMetrics = new ArrayList<>();
    this.benchmarkResults = new ArrayList<>();
    this.profileSnapshots = new ArrayList<>();
    this.runtimeCharacteristics = new HashMap<>();
  }

  /**
   * Creates a new insights engine builder.
   *
   * @return builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates an insights engine with default configuration.
   *
   * @return insights engine instance
   */
  public static PerformanceInsightsEngine create() {
    return builder().build();
  }

  /**
   * Adds memory analysis results to the insights engine.
   *
   * @param result memory analysis result
   */
  public void addMemoryAnalysis(final MemoryAnalysisResult result) {
    Objects.requireNonNull(result, "result cannot be null");
    if (memoryAnalysisEnabled) {
      memoryResults.add(result);
      LOGGER.fine("Added memory analysis result: " + result.getSessionName());
    }
  }

  /**
   * Adds garbage collection metrics to the insights engine.
   *
   * @param metrics GC impact metrics
   */
  public void addGcMetrics(final GcImpactMetrics metrics) {
    Objects.requireNonNull(metrics, "metrics cannot be null");
    if (gcAnalysisEnabled) {
      gcMetrics.add(metrics);
      LOGGER.fine("Added GC metrics");
    }
  }

  /**
   * Adds benchmark results to the insights engine.
   *
   * @param results benchmark results
   */
  public void addBenchmarkResults(final BenchmarkSuite.BenchmarkResults results) {
    Objects.requireNonNull(results, "results cannot be null");
    if (benchmarkAnalysisEnabled) {
      benchmarkResults.add(results);
      LOGGER.fine("Added benchmark results");
    }
  }

  /**
   * Adds a performance profile snapshot to the insights engine.
   *
   * @param snapshot profile snapshot
   */
  public void addProfileSnapshot(final ProfileSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot cannot be null");
    profileSnapshots.add(snapshot);
    LOGGER.fine("Added profile snapshot");
  }

  /**
   * Adds runtime-specific characteristics for analysis.
   *
   * @param runtimeType the runtime type
   * @param characteristics runtime characteristics map
   */
  public void addRuntimeCharacteristics(
      final String runtimeType, final Map<String, Object> characteristics) {
    Objects.requireNonNull(runtimeType, "runtimeType cannot be null");
    Objects.requireNonNull(characteristics, "characteristics cannot be null");
    runtimeCharacteristics.put(runtimeType, characteristics);
    LOGGER.fine("Added runtime characteristics for: " + runtimeType);
  }

  /**
   * Generates comprehensive performance insights based on collected data.
   *
   * @return performance insights
   */
  public PerformanceInsights generateInsights() {
    LOGGER.info("Generating performance insights from collected data");

    final List<PerformanceIssue> issues = identifyPerformanceIssues();
    final List<PerformanceRecommendation> recommendations = generateRecommendations(issues);
    final List<OptimizationOpportunity> opportunities = identifyOptimizationOpportunities();
    final PerformanceSummary summary = generateSummary();

    return new PerformanceInsights(
        Instant.now(), issues, recommendations, opportunities, summary, getDataSources());
  }

  /** Clears all collected performance data. */
  public void clearData() {
    memoryResults.clear();
    gcMetrics.clear();
    benchmarkResults.clear();
    profileSnapshots.clear();
    runtimeCharacteristics.clear();
    LOGGER.info("Cleared all performance data");
  }

  /**
   * Gets the current data collection status.
   *
   * @return data collection status
   */
  public DataCollectionStatus getStatus() {
    return new DataCollectionStatus(
        memoryResults.size(),
        gcMetrics.size(),
        benchmarkResults.size(),
        profileSnapshots.size(),
        runtimeCharacteristics.size());
  }

  private List<PerformanceIssue> identifyPerformanceIssues() {
    final List<PerformanceIssue> issues = new ArrayList<>();

    // Analyze memory issues
    if (memoryAnalysisEnabled) {
      issues.addAll(identifyMemoryIssues());
    }

    // Analyze GC issues
    if (gcAnalysisEnabled) {
      issues.addAll(identifyGcIssues());
    }

    // Analyze benchmark issues
    if (benchmarkAnalysisEnabled) {
      issues.addAll(identifyBenchmarkIssues());
    }

    // Analyze runtime comparison issues
    if (runtimeComparisonEnabled) {
      issues.addAll(identifyRuntimeIssues());
    }

    return issues;
  }

  private List<PerformanceIssue> identifyMemoryIssues() {
    final List<PerformanceIssue> issues = new ArrayList<>();

    for (final MemoryAnalysisResult result : memoryResults) {
      // Check for low efficiency
      if (result.getMemoryEfficiencyScore() < LOW_EFFICIENCY_THRESHOLD) {
        issues.add(
            new PerformanceIssue(
                IssueSeverity.MEDIUM,
                IssueCategory.MEMORY,
                "Low memory efficiency",
                String.format(
                    "Memory efficiency score of %.1f is below recommended threshold of %.1f",
                    result.getMemoryEfficiencyScore(), LOW_EFFICIENCY_THRESHOLD),
                result.getSessionName()));
      }

      // Check for excessive memory growth
      if (result.getHeapDelta() > 100 * 1024 * 1024) { // 100MB
        issues.add(
            new PerformanceIssue(
                IssueSeverity.HIGH,
                IssueCategory.MEMORY,
                "Excessive memory growth",
                String.format(
                    "Heap memory increased by %s during operation",
                    formatBytes(result.getHeapDelta())),
                result.getSessionName()));
      }

      // Check for high allocation rates
      if (result.getAllocationAnalysis().getAllocationRate() > HIGH_ALLOCATION_RATE_THRESHOLD) {
        issues.add(
            new PerformanceIssue(
                IssueSeverity.MEDIUM,
                IssueCategory.MEMORY,
                "High allocation rate",
                String.format(
                    "Allocation rate of %.1f MB/s exceeds recommended threshold",
                    result.getAllocationAnalysis().getAllocationRate()),
                result.getSessionName()));
      }
    }

    return issues;
  }

  private List<PerformanceIssue> identifyGcIssues() {
    final List<PerformanceIssue> issues = new ArrayList<>();

    for (final GcImpactMetrics metrics : gcMetrics) {
      // Check for high GC overhead
      if (metrics.getGcOverheadPercentage() > HIGH_GC_OVERHEAD_THRESHOLD) {
        issues.add(
            new PerformanceIssue(
                IssueSeverity.HIGH,
                IssueCategory.GARBAGE_COLLECTION,
                "High GC overhead",
                String.format(
                    "Garbage collection overhead of %.1f%% exceeds recommended threshold of %.1f%%",
                    metrics.getGcOverheadPercentage(), HIGH_GC_OVERHEAD_THRESHOLD),
                "GC Analysis"));
      }

      // Check for frequent collections
      if (metrics.getTotalGcCollections() > 100) {
        issues.add(
            new PerformanceIssue(
                IssueSeverity.MEDIUM,
                IssueCategory.GARBAGE_COLLECTION,
                "Frequent GC collections",
                String.format(
                    "High number of GC collections (%d) indicates potential memory pressure",
                    metrics.getTotalGcCollections()),
                "GC Analysis"));
      }
    }

    return issues;
  }

  private List<PerformanceIssue> identifyBenchmarkIssues() {
    final List<PerformanceIssue> issues = new ArrayList<>();

    // Analyze benchmark performance variations
    for (final BenchmarkSuite.BenchmarkResults results : benchmarkResults) {
      final Map<String, BenchmarkSuite.BenchmarkResult> resultMap = results.getResults();

      // Look for performance inconsistencies
      final var scores =
          resultMap.values().stream()
              .mapToDouble(BenchmarkSuite.BenchmarkResult::getScore)
              .toArray();

      if (scores.length > 1) {
        final double mean = java.util.Arrays.stream(scores).average().orElse(0);
        final double variance =
            java.util.Arrays.stream(scores)
                .map(score -> Math.pow(score - mean, 2))
                .average()
                .orElse(0);
        final double stdDev = Math.sqrt(variance);
        final double cv = mean > 0 ? (stdDev / mean) * 100 : 0;

        if (cv > 20) { // 20% coefficient of variation
          issues.add(
              new PerformanceIssue(
                  IssueSeverity.MEDIUM,
                  IssueCategory.PERFORMANCE_VARIATION,
                  "High performance variation",
                  String.format("Benchmark results show high variation (CV: %.1f%%)", cv),
                  "Benchmark Analysis"));
        }
      }
    }

    return issues;
  }

  private List<PerformanceIssue> identifyRuntimeIssues() {
    final List<PerformanceIssue> issues = new ArrayList<>();

    // Compare runtime characteristics if multiple runtimes are present
    if (runtimeCharacteristics.size() > 1) {
      // This would analyze runtime-specific performance differences
      // Simplified implementation for now
      issues.add(
          new PerformanceIssue(
              IssueSeverity.LOW,
              IssueCategory.RUNTIME_COMPARISON,
              "Runtime performance analysis available",
              "Multiple runtimes detected - consider analyzing performance differences",
              "Runtime Comparison"));
    }

    return issues;
  }

  private List<PerformanceRecommendation> generateRecommendations(
      final List<PerformanceIssue> issues) {
    final List<PerformanceRecommendation> recommendations = new ArrayList<>();

    for (final PerformanceIssue issue : issues) {
      recommendations.addAll(generateRecommendationsForIssue(issue));
    }

    // Add general recommendations based on overall patterns
    recommendations.addAll(generateGeneralRecommendations());

    return recommendations;
  }

  private List<PerformanceRecommendation> generateRecommendationsForIssue(
      final PerformanceIssue issue) {
    final List<PerformanceRecommendation> recommendations = new ArrayList<>();

    switch (issue.getCategory()) {
      case MEMORY:
        if (issue.getTitle().contains("efficiency")) {
          recommendations.add(
              new PerformanceRecommendation(
                  RecommendationPriority.HIGH,
                  RecommendationType.OPTIMIZATION,
                  "Optimize memory usage patterns",
                  "Review object lifecycle management and consider object pooling for frequently"
                      + " allocated objects",
                  List.of(
                      "Implement object pooling",
                      "Reduce temporary object creation",
                      "Optimize data structures"),
                  EstimatedImpact.MEDIUM));
        }
        if (issue.getTitle().contains("growth")) {
          recommendations.add(
              new PerformanceRecommendation(
                  RecommendationPriority.HIGH,
                  RecommendationType.CONFIGURATION,
                  "Investigate memory leaks",
                  "Excessive memory growth may indicate memory leaks or inefficient memory usage",
                  List.of(
                      "Use memory profiler",
                      "Review object retention",
                      "Check for circular references"),
                  EstimatedImpact.HIGH));
        }
        if (issue.getTitle().contains("allocation")) {
          recommendations.add(
              new PerformanceRecommendation(
                  RecommendationPriority.MEDIUM,
                  RecommendationType.OPTIMIZATION,
                  "Reduce allocation rate",
                  "High allocation rates can cause GC pressure and performance degradation",
                  List.of(
                      "Reuse objects where possible",
                      "Use primitive collections",
                      "Optimize hot paths"),
                  EstimatedImpact.MEDIUM));
        }
        break;

      case GARBAGE_COLLECTION:
        if (issue.getTitle().contains("overhead")) {
          recommendations.add(
              new PerformanceRecommendation(
                  RecommendationPriority.HIGH,
                  RecommendationType.CONFIGURATION,
                  "Tune garbage collection settings",
                  "High GC overhead can be reduced through heap sizing and GC algorithm tuning",
                  List.of("Increase heap size", "Consider G1GC or ZGC", "Tune GC parameters"),
                  EstimatedImpact.HIGH));
        }
        if (issue.getTitle().contains("frequent")) {
          recommendations.add(
              new PerformanceRecommendation(
                  RecommendationPriority.MEDIUM,
                  RecommendationType.OPTIMIZATION,
                  "Reduce allocation pressure",
                  "Frequent GC collections indicate high allocation rates",
                  List.of(
                      "Optimize allocation patterns", "Increase heap size", "Use off-heap storage"),
                  EstimatedImpact.MEDIUM));
        }
        break;

      case PERFORMANCE_VARIATION:
        recommendations.add(
            new PerformanceRecommendation(
                RecommendationPriority.MEDIUM,
                RecommendationType.INVESTIGATION,
                "Investigate performance inconsistencies",
                "High performance variation suggests unstable performance characteristics",
                List.of(
                    "Profile hot paths",
                    "Check for resource contention",
                    "Analyze JIT compilation"),
                EstimatedImpact.MEDIUM));
        break;

      case RUNTIME_COMPARISON:
        recommendations.add(
            new PerformanceRecommendation(
                RecommendationPriority.LOW,
                RecommendationType.ANALYSIS,
                "Analyze runtime performance differences",
                "Compare performance characteristics across different runtime implementations",
                List.of("Run comparative benchmarks", "Analyze runtime-specific optimizations"),
                EstimatedImpact.LOW));
        break;

      default:
        // For unknown issue categories, provide general performance guidance
        recommendations.add(
            new PerformanceRecommendation(
                RecommendationPriority.LOW,
                RecommendationType.ANALYSIS,
                "General performance analysis",
                "Perform comprehensive performance analysis to identify optimization opportunities",
                List.of("Run performance profiling", "Review resource usage patterns"),
                EstimatedImpact.LOW));
        break;
    }

    return recommendations;
  }

  private List<PerformanceRecommendation> generateGeneralRecommendations() {
    final List<PerformanceRecommendation> recommendations = new ArrayList<>();

    // Recommendation based on data availability
    if (memoryResults.isEmpty() && memoryAnalysisEnabled) {
      recommendations.add(
          new PerformanceRecommendation(
              RecommendationPriority.LOW,
              RecommendationType.MONITORING,
              "Enable memory monitoring",
              "No memory analysis data available - consider enabling memory monitoring for better"
                  + " insights",
              List.of("Start memory analysis sessions", "Configure memory profiling"),
              EstimatedImpact.LOW));
    }

    if (gcMetrics.isEmpty() && gcAnalysisEnabled) {
      recommendations.add(
          new PerformanceRecommendation(
              RecommendationPriority.LOW,
              RecommendationType.MONITORING,
              "Enable GC monitoring",
              "No GC analysis data available - enable GC monitoring to identify optimization"
                  + " opportunities",
              List.of("Capture GC metrics", "Enable GC logging"),
              EstimatedImpact.LOW));
    }

    return recommendations;
  }

  private List<OptimizationOpportunity> identifyOptimizationOpportunities() {
    final List<OptimizationOpportunity> opportunities = new ArrayList<>();

    // Identify opportunities based on collected data patterns
    if (!memoryResults.isEmpty()) {
      // Look for consistent memory patterns across sessions
      final double avgEfficiency =
          memoryResults.stream()
              .mapToDouble(MemoryAnalysisResult::getMemoryEfficiencyScore)
              .average()
              .orElse(0);

      if (avgEfficiency < 80) {
        opportunities.add(
            new OptimizationOpportunity(
                "Memory Efficiency Improvement",
                String.format(
                    "Average memory efficiency is %.1f%% - optimization could improve performance",
                    avgEfficiency),
                EstimatedImpact.MEDIUM,
                List.of(
                    "Profile memory allocation patterns",
                    "Implement object pooling",
                    "Optimize data structures")));
      }
    }

    if (!gcMetrics.isEmpty()) {
      final double avgGcOverhead =
          gcMetrics.stream()
              .mapToDouble(GcImpactMetrics::getGcOverheadPercentage)
              .average()
              .orElse(0);

      if (avgGcOverhead > 2.0) {
        opportunities.add(
            new OptimizationOpportunity(
                "Garbage Collection Optimization",
                String.format(
                    "Average GC overhead is %.1f%% - tuning could reduce performance impact",
                    avgGcOverhead),
                EstimatedImpact.HIGH,
                List.of("Increase heap size", "Tune GC algorithm", "Reduce allocation rate")));
      }
    }

    return opportunities;
  }

  private PerformanceSummary generateSummary() {
    final StringBuilder summary = new StringBuilder();

    summary.append("Performance Analysis Summary\n");
    summary.append("Generated: ").append(Instant.now()).append("\n\n");

    if (!memoryResults.isEmpty()) {
      final double avgEfficiency =
          memoryResults.stream()
              .mapToDouble(MemoryAnalysisResult::getMemoryEfficiencyScore)
              .average()
              .orElse(0);
      summary.append(
          String.format(
              "Memory Efficiency: %.1f/100 (avg across %d sessions)\n",
              avgEfficiency, memoryResults.size()));
    }

    if (!gcMetrics.isEmpty()) {
      final double avgGcOverhead =
          gcMetrics.stream()
              .mapToDouble(GcImpactMetrics::getGcOverheadPercentage)
              .average()
              .orElse(0);
      summary.append(
          String.format(
              "GC Overhead: %.1f%% (avg across %d measurements)\n",
              avgGcOverhead, gcMetrics.size()));
    }

    if (!benchmarkResults.isEmpty()) {
      summary.append(
          String.format(
              "Benchmark Results: %d benchmark suites analyzed\n", benchmarkResults.size()));
    }

    return new PerformanceSummary(summary.toString());
  }

  private Map<String, Integer> getDataSources() {
    final Map<String, Integer> sources = new HashMap<>();
    sources.put("memoryAnalysis", memoryResults.size());
    sources.put("gcMetrics", gcMetrics.size());
    sources.put("benchmarkResults", benchmarkResults.size());
    sources.put("profileSnapshots", profileSnapshots.size());
    sources.put("runtimeCharacteristics", runtimeCharacteristics.size());
    return sources;
  }

  private String formatBytes(final long bytes) {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
    return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
  }

  /** Builder for PerformanceInsightsEngine. */
  public static final class Builder {
    private boolean memoryAnalysisEnabled = true;
    private boolean gcAnalysisEnabled = true;
    private boolean benchmarkAnalysisEnabled = true;
    private boolean runtimeComparisonEnabled = true;

    public Builder withMemoryAnalysis(final boolean enabled) {
      this.memoryAnalysisEnabled = enabled;
      return this;
    }

    public Builder withGcAnalysis(final boolean enabled) {
      this.gcAnalysisEnabled = enabled;
      return this;
    }

    public Builder withBenchmarkAnalysis(final boolean enabled) {
      this.benchmarkAnalysisEnabled = enabled;
      return this;
    }

    public Builder withRuntimeComparison(final boolean enabled) {
      this.runtimeComparisonEnabled = enabled;
      return this;
    }

    public PerformanceInsightsEngine build() {
      return new PerformanceInsightsEngine(this);
    }
  }

  /** Data collection status. */
  public static final class DataCollectionStatus {
    private final int memoryResults;
    private final int gcMetrics;
    private final int benchmarkResults;
    private final int profileSnapshots;
    private final int runtimeCharacteristics;

    public DataCollectionStatus(
        final int memoryResults,
        final int gcMetrics,
        final int benchmarkResults,
        final int profileSnapshots,
        final int runtimeCharacteristics) {
      this.memoryResults = memoryResults;
      this.gcMetrics = gcMetrics;
      this.benchmarkResults = benchmarkResults;
      this.profileSnapshots = profileSnapshots;
      this.runtimeCharacteristics = runtimeCharacteristics;
    }

    public int getMemoryResults() {
      return memoryResults;
    }

    public int getGcMetrics() {
      return gcMetrics;
    }

    public int getBenchmarkResults() {
      return benchmarkResults;
    }

    public int getProfileSnapshots() {
      return profileSnapshots;
    }

    public int getRuntimeCharacteristics() {
      return runtimeCharacteristics;
    }

    public int getTotalDataPoints() {
      return memoryResults
          + gcMetrics
          + benchmarkResults
          + profileSnapshots
          + runtimeCharacteristics;
    }
  }

  // Enums for categorization
  public enum IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  public enum IssueCategory {
    MEMORY,
    GARBAGE_COLLECTION,
    PERFORMANCE_VARIATION,
    RUNTIME_COMPARISON
  }

  public enum RecommendationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  public enum RecommendationType {
    OPTIMIZATION,
    CONFIGURATION,
    INVESTIGATION,
    MONITORING,
    ANALYSIS
  }

  public enum EstimatedImpact {
    LOW,
    MEDIUM,
    HIGH
  }
}
