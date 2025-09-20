package ai.tegmentum.wasmtime4j.performance.memory;

import ai.tegmentum.wasmtime4j.performance.ExportFormat;
import ai.tegmentum.wasmtime4j.performance.GcImpactMetrics;
import ai.tegmentum.wasmtime4j.performance.memory.MemoryAnalysisSession.AllocationAnalysis;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Comprehensive result of a memory analysis session.
 *
 * <p>This class contains detailed analysis of memory usage patterns, garbage collection impact,
 * and performance insights gathered during a memory analysis session. It provides both
 * raw metrics and actionable recommendations for optimization.
 *
 * <p>Key information includes:
 * <ul>
 *   <li>Memory usage deltas (heap and non-heap)</li>
 *   <li>Garbage collection impact analysis</li>
 *   <li>Allocation pattern analysis</li>
 *   <li>Performance issues and recommendations</li>
 *   <li>Export capabilities for reporting</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class MemoryAnalysisResult {
  private final String sessionName;
  private final Instant startTime;
  private final Instant endTime;
  private final MemoryAnalyzer.MemoryMetrics initialMemoryState;
  private final MemoryAnalyzer.MemoryMetrics finalMemoryState;
  private final long heapDelta;
  private final long nonHeapDelta;
  private final GcImpactMetrics gcImpact;
  private final AllocationAnalysis allocationAnalysis;
  private final int snapshotCount;
  private final List<String> issues;
  private final List<String> recommendations;

  MemoryAnalysisResult(final String sessionName,
                      final Instant startTime,
                      final Instant endTime,
                      final MemoryAnalyzer.MemoryMetrics initialMemoryState,
                      final MemoryAnalyzer.MemoryMetrics finalMemoryState,
                      final long heapDelta,
                      final long nonHeapDelta,
                      final GcImpactMetrics gcImpact,
                      final AllocationAnalysis allocationAnalysis,
                      final int snapshotCount,
                      final List<String> issues,
                      final List<String> recommendations) {
    this.sessionName = Objects.requireNonNull(sessionName, "sessionName cannot be null");
    this.startTime = Objects.requireNonNull(startTime, "startTime cannot be null");
    this.endTime = Objects.requireNonNull(endTime, "endTime cannot be null");
    this.initialMemoryState = Objects.requireNonNull(initialMemoryState, "initialMemoryState cannot be null");
    this.finalMemoryState = Objects.requireNonNull(finalMemoryState, "finalMemoryState cannot be null");
    this.heapDelta = heapDelta;
    this.nonHeapDelta = nonHeapDelta;
    this.gcImpact = Objects.requireNonNull(gcImpact, "gcImpact cannot be null");
    this.allocationAnalysis = Objects.requireNonNull(allocationAnalysis, "allocationAnalysis cannot be null");
    this.snapshotCount = snapshotCount;
    this.issues = List.copyOf(issues);
    this.recommendations = List.copyOf(recommendations);
  }

  /**
   * Gets the session name.
   *
   * @return session name
   */
  public String getSessionName() {
    return sessionName;
  }

  /**
   * Gets the session start time.
   *
   * @return start time
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Gets the session end time.
   *
   * @return end time
   */
  public Instant getEndTime() {
    return endTime;
  }

  /**
   * Gets the session duration.
   *
   * @return session duration
   */
  public Duration getSessionDuration() {
    return Duration.between(startTime, endTime);
  }

  /**
   * Gets the initial memory state.
   *
   * @return initial memory metrics
   */
  public MemoryAnalyzer.MemoryMetrics getInitialMemoryState() {
    return initialMemoryState;
  }

  /**
   * Gets the final memory state.
   *
   * @return final memory metrics
   */
  public MemoryAnalyzer.MemoryMetrics getFinalMemoryState() {
    return finalMemoryState;
  }

  /**
   * Gets the heap memory delta (change) during the session.
   *
   * @return heap memory delta in bytes
   */
  public long getHeapDelta() {
    return heapDelta;
  }

  /**
   * Gets the non-heap memory delta (change) during the session.
   *
   * @return non-heap memory delta in bytes
   */
  public long getNonHeapDelta() {
    return nonHeapDelta;
  }

  /**
   * Gets the garbage collection impact analysis.
   *
   * @return GC impact metrics
   */
  public GcImpactMetrics getGcImpact() {
    return gcImpact;
  }

  /**
   * Gets the allocation pattern analysis.
   *
   * @return allocation analysis
   */
  public AllocationAnalysis getAllocationAnalysis() {
    return allocationAnalysis;
  }

  /**
   * Gets the number of memory snapshots captured during the session.
   *
   * @return snapshot count
   */
  public int getSnapshotCount() {
    return snapshotCount;
  }

  /**
   * Gets the list of identified performance issues.
   *
   * @return list of issues
   */
  public List<String> getIssues() {
    return issues;
  }

  /**
   * Gets the list of optimization recommendations.
   *
   * @return list of recommendations
   */
  public List<String> getOptimizationRecommendations() {
    return recommendations;
  }

  /**
   * Checks if any performance issues were detected.
   *
   * @return true if issues were found
   */
  public boolean hasIssues() {
    return !issues.isEmpty();
  }

  /**
   * Gets the total memory impact (heap + non-heap delta).
   *
   * @return total memory delta in bytes
   */
  public long getTotalMemoryDelta() {
    return heapDelta + nonHeapDelta;
  }

  /**
   * Gets the memory efficiency score (0-100, higher is better).
   *
   * <p>The efficiency score considers:
   * <ul>
   *   <li>Memory growth relative to operation duration</li>
   *   <li>Garbage collection overhead</li>
   *   <li>Allocation rate efficiency</li>
   * </ul>
   *
   * @return efficiency score (0-100)
   */
  public double getMemoryEfficiencyScore() {
    double score = 100.0;

    // Penalize excessive memory growth
    final long sessionMs = getSessionDuration().toMillis();
    if (sessionMs > 0) {
      final double memoryGrowthRate = getTotalMemoryDelta() / (double) sessionMs; // bytes/ms
      if (memoryGrowthRate > 1000) { // > 1MB/s
        score -= Math.min(30, memoryGrowthRate / 1000 * 5);
      }
    }

    // Penalize GC overhead
    final double gcOverhead = gcImpact.getGcOverheadPercentage();
    if (gcOverhead > 1.0) {
      score -= Math.min(25, gcOverhead * 2);
    }

    // Penalize high allocation rates
    final double allocationRate = allocationAnalysis.getAllocationRate();
    if (allocationRate > 10.0) { // > 10 MB/s
      score -= Math.min(20, allocationRate);
    }

    // Apply bonus for clean execution
    if (gcOverhead < 1.0 && allocationRate < 5.0 && getTotalMemoryDelta() < 1024 * 1024) {
      score += 10;
    }

    return Math.max(0, Math.min(100, score));
  }

  /**
   * Gets a performance grade based on the efficiency score.
   *
   * @return performance grade (A-F)
   */
  public char getPerformanceGrade() {
    final double score = getMemoryEfficiencyScore();

    if (score >= 90) return 'A';
    if (score >= 80) return 'B';
    if (score >= 70) return 'C';
    if (score >= 60) return 'D';
    return 'F';
  }

  /**
   * Gets a human-readable summary of the analysis result.
   *
   * @return analysis summary
   */
  public String getSummary() {
    final StringBuilder summary = new StringBuilder();

    summary.append("Memory Analysis Summary for '").append(sessionName).append("'\n");
    summary.append("Duration: ").append(formatDuration(getSessionDuration())).append("\n");
    summary.append("Heap Delta: ").append(formatBytes(heapDelta)).append("\n");
    summary.append("Non-Heap Delta: ").append(formatBytes(nonHeapDelta)).append("\n");
    summary.append("GC Overhead: ").append(String.format("%.1f%%", gcImpact.getGcOverheadPercentage())).append("\n");
    summary.append("Allocation Rate: ").append(String.format("%.1f MB/s", allocationAnalysis.getAllocationRate())).append("\n");
    summary.append("Efficiency Score: ").append(String.format("%.1f/100 (Grade: %c)", getMemoryEfficiencyScore(), getPerformanceGrade())).append("\n");

    if (hasIssues()) {
      summary.append("\nIssues Found:\n");
      for (final String issue : issues) {
        summary.append("  - ").append(issue).append("\n");
      }
    }

    if (!recommendations.isEmpty()) {
      summary.append("\nRecommendations:\n");
      for (final String recommendation : recommendations) {
        summary.append("  - ").append(recommendation).append("\n");
      }
    }

    return summary.toString();
  }

  /**
   * Exports the analysis result in the specified format.
   *
   * @param format export format
   * @return exported data
   */
  public String export(final ExportFormat format) {
    switch (format) {
      case JSON:
        return exportAsJson();
      case CSV:
        return exportAsCsv();
      default:
        throw new IllegalArgumentException("Unsupported export format: " + format);
    }
  }

  /**
   * Compares this result with another memory analysis result.
   *
   * @param other the result to compare with
   * @return comparison result
   */
  public MemoryAnalysisComparison compareTo(final MemoryAnalysisResult other) {
    Objects.requireNonNull(other, "other cannot be null");
    return new MemoryAnalysisComparison(this, other);
  }

  private String exportAsJson() {
    return String.format("""
        {
          "sessionName": "%s",
          "startTime": "%s",
          "endTime": "%s",
          "duration": "%s",
          "memoryMetrics": {
            "heapDelta": %d,
            "nonHeapDelta": %d,
            "totalDelta": %d,
            "initialHeapUsed": %d,
            "finalHeapUsed": %d,
            "initialNonHeapUsed": %d,
            "finalNonHeapUsed": %d
          },
          "gcImpact": {
            "overheadPercentage": %.2f,
            "totalCollections": %d,
            "totalGcTime": "%s",
            "allocationRate": %.2f
          },
          "allocationAnalysis": {
            "totalAllocation": %d,
            "peakAllocation": %d,
            "averageAllocation": %.2f,
            "allocationRate": %.2f,
            "allocationEvents": %d
          },
          "performance": {
            "efficiencyScore": %.1f,
            "grade": "%c",
            "snapshotCount": %d
          },
          "issues": [%s],
          "recommendations": [%s]
        }""",
        sessionName, startTime, endTime, getSessionDuration(),
        heapDelta, nonHeapDelta, getTotalMemoryDelta(),
        initialMemoryState.getHeapUsed(), finalMemoryState.getHeapUsed(),
        initialMemoryState.getNonHeapUsed(), finalMemoryState.getNonHeapUsed(),
        gcImpact.getGcOverheadPercentage(), gcImpact.getTotalGcCollections(),
        gcImpact.getTotalGcTime(), gcImpact.getAllocationRate(),
        allocationAnalysis.getTotalAllocation(), allocationAnalysis.getPeakAllocation(),
        allocationAnalysis.getAverageAllocation(), allocationAnalysis.getAllocationRate(),
        allocationAnalysis.getAllocationEvents(),
        getMemoryEfficiencyScore(), getPerformanceGrade(), snapshotCount,
        formatJsonStringArray(issues), formatJsonStringArray(recommendations)
    );
  }

  private String exportAsCsv() {
    final StringBuilder csv = new StringBuilder();
    csv.append("SessionName,Duration,HeapDelta,NonHeapDelta,GcOverhead,AllocationRate,EfficiencyScore,Grade,Issues,Recommendations\n");
    csv.append(sessionName).append(",");
    csv.append(getSessionDuration()).append(",");
    csv.append(heapDelta).append(",");
    csv.append(nonHeapDelta).append(",");
    csv.append(String.format("%.2f", gcImpact.getGcOverheadPercentage())).append(",");
    csv.append(String.format("%.2f", allocationAnalysis.getAllocationRate())).append(",");
    csv.append(String.format("%.1f", getMemoryEfficiencyScore())).append(",");
    csv.append(getPerformanceGrade()).append(",");
    csv.append("\"").append(String.join("; ", issues)).append("\",");
    csv.append("\"").append(String.join("; ", recommendations)).append("\"");
    return csv.toString();
  }

  private String formatJsonStringArray(final List<String> strings) {
    return strings.stream()
        .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
        .reduce((a, b) -> a + ", " + b)
        .orElse("");
  }

  private String formatDuration(final Duration duration) {
    final long seconds = duration.getSeconds();
    final long millis = duration.toMillisPart();

    if (seconds == 0) {
      return millis + "ms";
    } else if (seconds < 60) {
      return String.format("%d.%03ds", seconds, millis);
    } else {
      final long minutes = seconds / 60;
      final long remainingSeconds = seconds % 60;
      return String.format("%dm %ds", minutes, remainingSeconds);
    }
  }

  private String formatBytes(final long bytes) {
    if (bytes == 0) {
      return "0 B";
    }

    final long absBytes = Math.abs(bytes);
    final String sign = bytes < 0 ? "-" : "";

    if (absBytes < 1024) {
      return sign + absBytes + " B";
    } else if (absBytes < 1024 * 1024) {
      return sign + String.format("%.1f KB", absBytes / 1024.0);
    } else {
      return sign + String.format("%.1f MB", absBytes / 1024.0 / 1024.0);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "MemoryAnalysisResult{session='%s', duration=%s, heapDelta=%s, efficiency=%.1f%%}",
        sessionName, formatDuration(getSessionDuration()), formatBytes(heapDelta), getMemoryEfficiencyScore()
    );
  }

  /**
   * Comparison result between two memory analysis results.
   */
  public static final class MemoryAnalysisComparison {
    private final MemoryAnalysisResult baseline;
    private final MemoryAnalysisResult comparison;
    private final long heapDeltaDifference;
    private final long nonHeapDeltaDifference;
    private final double gcOverheadDifference;
    private final double allocationRateDifference;
    private final double efficiencyScoreDifference;

    MemoryAnalysisComparison(final MemoryAnalysisResult baseline, final MemoryAnalysisResult comparison) {
      this.baseline = baseline;
      this.comparison = comparison;
      this.heapDeltaDifference = comparison.getHeapDelta() - baseline.getHeapDelta();
      this.nonHeapDeltaDifference = comparison.getNonHeapDelta() - baseline.getNonHeapDelta();
      this.gcOverheadDifference = comparison.getGcImpact().getGcOverheadPercentage() -
                                  baseline.getGcImpact().getGcOverheadPercentage();
      this.allocationRateDifference = comparison.getAllocationAnalysis().getAllocationRate() -
                                     baseline.getAllocationAnalysis().getAllocationRate();
      this.efficiencyScoreDifference = comparison.getMemoryEfficiencyScore() - baseline.getMemoryEfficiencyScore();
    }

    public MemoryAnalysisResult getBaseline() { return baseline; }
    public MemoryAnalysisResult getComparison() { return comparison; }
    public long getHeapDeltaDifference() { return heapDeltaDifference; }
    public long getNonHeapDeltaDifference() { return nonHeapDeltaDifference; }
    public double getGcOverheadDifference() { return gcOverheadDifference; }
    public double getAllocationRateDifference() { return allocationRateDifference; }
    public double getEfficiencyScoreDifference() { return efficiencyScoreDifference; }

    /**
     * Checks if the comparison shows improvement over the baseline.
     *
     * @return true if comparison is better than baseline
     */
    public boolean isImprovement() {
      return efficiencyScoreDifference > 0;
    }

    /**
     * Checks if the comparison shows regression from the baseline.
     *
     * @return true if comparison is worse than baseline
     */
    public boolean isRegression() {
      return efficiencyScoreDifference < -5.0; // 5 point threshold
    }

    /**
     * Gets a summary of the comparison.
     *
     * @return comparison summary
     */
    public String getSummary() {
      final StringBuilder summary = new StringBuilder();
      summary.append("Memory Analysis Comparison\n");
      summary.append("Baseline: ").append(baseline.getSessionName()).append("\n");
      summary.append("Comparison: ").append(comparison.getSessionName()).append("\n");
      summary.append("Efficiency Score: ").append(String.format("%.1f → %.1f (%.1f)",
          baseline.getMemoryEfficiencyScore(), comparison.getMemoryEfficiencyScore(), efficiencyScoreDifference)).append("\n");
      summary.append("Heap Delta: ").append(formatBytes(baseline.getHeapDelta())).append(" → ")
          .append(formatBytes(comparison.getHeapDelta())).append(" (").append(formatBytes(heapDeltaDifference)).append(")\n");
      summary.append("GC Overhead: ").append(String.format("%.1f%% → %.1f%% (%.1f%%)",
          baseline.getGcImpact().getGcOverheadPercentage(),
          comparison.getGcImpact().getGcOverheadPercentage(),
          gcOverheadDifference)).append("\n");

      if (isImprovement()) {
        summary.append("Result: IMPROVEMENT\n");
      } else if (isRegression()) {
        summary.append("Result: REGRESSION\n");
      } else {
        summary.append("Result: SIMILAR PERFORMANCE\n");
      }

      return summary.toString();
    }

    private String formatBytes(final long bytes) {
      if (bytes == 0) return "0 B";
      final long absBytes = Math.abs(bytes);
      final String sign = bytes < 0 ? "-" : "";
      if (absBytes < 1024) return sign + absBytes + " B";
      if (absBytes < 1024 * 1024) return sign + String.format("%.1f KB", absBytes / 1024.0);
      return sign + String.format("%.1f MB", absBytes / 1024.0 / 1024.0);
    }
  }
}