package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive performance analysis report for WebAssembly execution.
 *
 * <p>PerformanceReport provides detailed analysis of WebAssembly execution performance including
 * statistical summaries, trend analysis, bottleneck identification, and optimization
 * recommendations.
 *
 * <p>Reports are generated from performance monitoring data and provide insights for optimization
 * and capacity planning.
 *
 * @since 1.0.0
 */
public interface PerformanceReport {

  /**
   * Gets the time period covered by this performance report.
   *
   * @return monitoring period start time
   */
  Instant getMonitoringStartTime();

  /**
   * Gets the end time of the monitoring period.
   *
   * @return monitoring period end time
   */
  Instant getMonitoringEndTime();

  /**
   * Gets the total duration of the monitoring period.
   *
   * @return monitoring period duration
   */
  Duration getMonitoringDuration();

  /**
   * Gets the overall execution metrics summary for the monitoring period.
   *
   * <p>This provides aggregated metrics covering the entire monitoring session.
   *
   * @return overall execution metrics
   */
  ExecutionMetrics getOverallMetrics();

  /**
   * Gets performance metrics broken down by time intervals.
   *
   * <p>This provides a time-series view of performance metrics, useful for identifying performance
   * trends and patterns over time.
   *
   * @return list of time-interval metrics in chronological order
   */
  List<TimestampedMetrics> getTimeSeriesMetrics();

  /**
   * Gets performance statistics broken down by function.
   *
   * <p>This provides detailed analysis of individual function performance including call counts,
   * execution times, and resource usage.
   *
   * @return map of function names to their performance statistics
   */
  Map<String, FunctionPerformanceStats> getFunctionStatistics();

  /**
   * Gets memory usage analysis and statistics.
   *
   * <p>This includes allocation patterns, peak usage, garbage collection impact, and memory
   * efficiency metrics.
   *
   * @return memory performance analysis
   */
  MemoryPerformanceAnalysis getMemoryAnalysis();

  /**
   * Gets JIT compilation performance analysis.
   *
   * <p>This provides insights into compilation performance including compilation times,
   * optimization levels achieved, and compilation efficiency.
   *
   * @return JIT compilation analysis
   */
  JitCompilationAnalysis getJitAnalysis();

  /**
   * Identifies performance bottlenecks and areas for optimization.
   *
   * <p>This analyzes the collected performance data to identify functions, operations, or patterns
   * that are consuming disproportionate resources.
   *
   * @return list of identified performance bottlenecks
   */
  List<PerformanceBottleneck> getBottlenecks();

  /**
   * Gets recommendations for performance optimization.
   *
   * <p>Based on the performance analysis, this provides actionable recommendations for improving
   * WebAssembly execution performance.
   *
   * @return list of optimization recommendations
   */
  List<OptimizationRecommendation> getOptimizationRecommendations();

  /**
   * Gets performance comparison with previous reports if available.
   *
   * <p>This provides regression analysis by comparing current performance with previous monitoring
   * sessions.
   *
   * @return performance comparison analysis, or null if no previous data available
   */
  PerformanceComparison getPerformanceComparison();

  /**
   * Gets resource utilization statistics.
   *
   * <p>This includes CPU usage, memory efficiency, and other resource utilization metrics during
   * WebAssembly execution.
   *
   * @return resource utilization statistics
   */
  ResourceUtilizationStats getResourceUtilization();

  /**
   * Gets error and exception statistics that may impact performance.
   *
   * <p>This includes WebAssembly traps, host function errors, and other exceptions that occurred
   * during the monitoring period.
   *
   * @return error statistics
   */
  ErrorPerformanceImpact getErrorImpact();

  /**
   * Gets the confidence level of the performance measurements.
   *
   * <p>This indicates the statistical reliability of the performance data based on sample size,
   * measurement variance, and monitoring duration.
   *
   * @return confidence level percentage (0.0 to 100.0)
   */
  double getConfidenceLevel();

  /**
   * Exports the performance report in the specified format.
   *
   * <p>Supported formats may include JSON, CSV, HTML, or other structured formats suitable for
   * external analysis tools.
   *
   * @param format export format specification
   * @return exported report data
   * @throws IllegalArgumentException if format is not supported
   */
  String exportReport(final ReportFormat format);

  /**
   * Creates a summary string representation of the performance report.
   *
   * <p>The summary includes key findings and metrics in a human-readable format suitable for
   * logging or quick analysis.
   *
   * @return formatted report summary
   */
  String getSummary();

  /**
   * Gets the monitoring overhead impact on the reported performance.
   *
   * <p>This provides transparency about the performance cost of monitoring itself and its impact on
   * the measured results.
   *
   * @return monitoring overhead analysis
   */
  MonitoringOverheadAnalysis getMonitoringOverheadAnalysis();
}
