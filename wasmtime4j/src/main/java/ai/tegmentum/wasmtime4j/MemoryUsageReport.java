package ai.tegmentum.wasmtime4j;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Comprehensive memory usage report for WebAssembly linear memory analysis.
 *
 * <p>This class provides a detailed snapshot of memory usage patterns, statistics, and
 * recommendations for optimization. It combines multiple data sources to provide actionable
 * insights for enterprise memory management.
 *
 * @since 1.0.0
 */
public final class MemoryUsageReport {

  private final long reportTimestamp;
  private final MemoryStatistics statistics;
  private final List<MemorySegment> segments;
  private final List<String> recommendations;
  private final List<String> warnings;
  private final long reportGenerationTimeNanos;

  /**
   * Creates a new memory usage report.
   *
   * @param reportTimestamp when this report was generated (milliseconds since epoch)
   * @param statistics the current memory statistics
   * @param segments the list of memory segments
   * @param recommendations optimization recommendations
   * @param warnings any memory-related warnings
   * @param reportGenerationTimeNanos time taken to generate this report in nanoseconds
   * @throws IllegalArgumentException if any required parameter is null
   */
  public MemoryUsageReport(
      final long reportTimestamp,
      final MemoryStatistics statistics,
      final List<MemorySegment> segments,
      final List<String> recommendations,
      final List<String> warnings,
      final long reportGenerationTimeNanos) {
    if (statistics == null) {
      throw new IllegalArgumentException("statistics cannot be null");
    }
    if (segments == null) {
      throw new IllegalArgumentException("segments cannot be null");
    }
    if (recommendations == null) {
      throw new IllegalArgumentException("recommendations cannot be null");
    }
    if (warnings == null) {
      throw new IllegalArgumentException("warnings cannot be null");
    }

    this.reportTimestamp = reportTimestamp;
    this.statistics = statistics;
    this.segments = Collections.unmodifiableList(segments);
    this.recommendations = Collections.unmodifiableList(recommendations);
    this.warnings = Collections.unmodifiableList(warnings);
    this.reportGenerationTimeNanos = reportGenerationTimeNanos;
  }

  /**
   * Gets the timestamp when this report was generated.
   *
   * @return the report timestamp in milliseconds since epoch
   */
  public long getReportTimestamp() {
    return reportTimestamp;
  }

  /**
   * Gets the memory statistics included in this report.
   *
   * @return the memory statistics
   */
  public MemoryStatistics getStatistics() {
    return statistics;
  }

  /**
   * Gets the list of memory segments at the time of report generation.
   *
   * @return an unmodifiable list of memory segments
   */
  public List<MemorySegment> getSegments() {
    return segments;
  }

  /**
   * Gets optimization recommendations based on the current memory usage patterns.
   *
   * @return an unmodifiable list of recommendations
   */
  public List<String> getRecommendations() {
    return recommendations;
  }

  /**
   * Gets any warnings about potential memory issues.
   *
   * @return an unmodifiable list of warnings
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * Gets the time taken to generate this report.
   *
   * @return the report generation time in nanoseconds
   */
  public long getReportGenerationTimeNanos() {
    return reportGenerationTimeNanos;
  }

  /**
   * Gets the number of active memory segments.
   *
   * @return the count of active segments
   */
  public int getActiveSegmentCount() {
    return (int) segments.stream().filter(MemorySegment::isActive).count();
  }

  /**
   * Gets the number of read-only memory segments.
   *
   * @return the count of read-only segments
   */
  public int getReadOnlySegmentCount() {
    return (int) segments.stream().filter(MemorySegment::isReadOnly).count();
  }

  /**
   * Gets the number of executable memory segments.
   *
   * @return the count of executable segments
   */
  public int getExecutableSegmentCount() {
    return (int) segments.stream().filter(MemorySegment::isExecutable).count();
  }

  /**
   * Gets the total size of all memory segments.
   *
   * @return the total segment size in bytes
   */
  public long getTotalSegmentSize() {
    return segments.stream().mapToLong(MemorySegment::getSize).sum();
  }

  /**
   * Gets the largest memory segment by size.
   *
   * @return the largest segment, or null if no segments exist
   */
  public MemorySegment getLargestSegment() {
    return segments.stream().max((s1, s2) -> Long.compare(s1.getSize(), s2.getSize())).orElse(null);
  }

  /**
   * Gets the oldest memory segment by creation time.
   *
   * @return the oldest segment, or null if no segments exist
   */
  public MemorySegment getOldestSegment() {
    return segments.stream()
        .min((s1, s2) -> Long.compare(s1.getCreationTimestamp(), s2.getCreationTimestamp()))
        .orElse(null);
  }

  /**
   * Gets the average segment size.
   *
   * @return the average segment size in bytes, or 0 if no segments exist
   */
  public double getAverageSegmentSize() {
    return segments.isEmpty() ? 0.0 : (double) getTotalSegmentSize() / segments.size();
  }

  /**
   * Checks if there are any critical warnings in this report.
   *
   * @return true if there are warnings, false otherwise
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Checks if there are any optimization recommendations in this report.
   *
   * @return true if there are recommendations, false otherwise
   */
  public boolean hasRecommendations() {
    return !recommendations.isEmpty();
  }

  /**
   * Gets a summary string of this memory usage report.
   *
   * @return a concise summary of the report
   */
  public String getSummary() {
    return String.format(
        "MemoryUsageReport: %d segments (%d active), %.2f MB total, %.1f%% utilization, %d"
            + " warnings, %d recommendations",
        segments.size(),
        getActiveSegmentCount(),
        getTotalSegmentSize() / (1024.0 * 1024.0),
        statistics.getUtilizationEfficiency() * 100,
        warnings.size(),
        recommendations.size());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final MemoryUsageReport that = (MemoryUsageReport) obj;
    return reportTimestamp == that.reportTimestamp
        && reportGenerationTimeNanos == that.reportGenerationTimeNanos
        && Objects.equals(statistics, that.statistics)
        && Objects.equals(segments, that.segments)
        && Objects.equals(recommendations, that.recommendations)
        && Objects.equals(warnings, that.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        reportTimestamp,
        statistics,
        segments,
        recommendations,
        warnings,
        reportGenerationTimeNanos);
  }

  @Override
  public String toString() {
    return "MemoryUsageReport{"
        + "reportTimestamp="
        + reportTimestamp
        + ", segmentCount="
        + segments.size()
        + ", activeSegments="
        + getActiveSegmentCount()
        + ", totalSize="
        + getTotalSegmentSize()
        + ", utilization="
        + String.format("%.1f%%", statistics.getUtilizationEfficiency() * 100)
        + ", warnings="
        + warnings.size()
        + ", recommendations="
        + recommendations.size()
        + ", generationTime="
        + reportGenerationTimeNanos
        + "ns"
        + '}';
  }
}
