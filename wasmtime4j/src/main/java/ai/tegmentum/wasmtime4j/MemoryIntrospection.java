package ai.tegmentum.wasmtime4j;

import java.util.List;

/**
 * Advanced memory introspection capabilities for WebAssembly linear memory.
 *
 * <p>This interface provides comprehensive memory analysis, monitoring, and diagnostic
 * capabilities for enterprise applications that require detailed memory management insights.
 * All operations are designed with minimal performance overhead while providing maximum
 * visibility into memory behavior.
 *
 * @since 1.0.0
 */
public interface MemoryIntrospection {

  /**
   * Gets comprehensive memory statistics for the current memory instance.
   *
   * <p>This provides real-time statistics including usage, fragmentation, and performance
   * metrics with minimal overhead through optimized native implementations.
   *
   * @return comprehensive memory statistics
   * @throws RuntimeException if statistics cannot be retrieved
   */
  MemoryStatistics getStatistics();

  /**
   * Gets detailed information about all memory segments.
   *
   * <p>Memory segments provide insight into how memory is organized and used,
   * including protection flags and access patterns.
   *
   * @return an unmodifiable list of memory segments
   * @throws RuntimeException if segment information cannot be retrieved
   */
  List<MemorySegment> getSegments();

  /**
   * Generates a comprehensive memory usage report with analysis and recommendations.
   *
   * <p>This operation performs deep analysis of memory usage patterns and provides
   * actionable recommendations for optimization and troubleshooting.
   *
   * @return a detailed memory usage report
   * @throws RuntimeException if the report cannot be generated
   */
  MemoryUsageReport generateUsageReport();

  /**
   * Enables performance tracking for memory operations.
   *
   * <p>When enabled, the system will collect detailed performance metrics for all
   * memory operations. This has minimal overhead but provides valuable insights
   * for optimization.
   *
   * @throws RuntimeException if performance tracking cannot be enabled
   */
  void enablePerformanceTracking();

  /**
   * Disables performance tracking for memory operations.
   *
   * <p>This stops the collection of performance metrics to minimize any overhead
   * in performance-critical applications.
   *
   * @throws RuntimeException if performance tracking cannot be disabled
   */
  void disablePerformanceTracking();

  /**
   * Checks if performance tracking is currently enabled.
   *
   * @return true if performance tracking is enabled, false otherwise
   */
  boolean isPerformanceTrackingEnabled();

  /**
   * Gets current performance metrics for memory operations.
   *
   * <p>This provides detailed performance data including operation counts, timings,
   * throughput, and cache statistics. Only available when performance tracking is enabled.
   *
   * @return current performance metrics
   * @throws IllegalStateException if performance tracking is not enabled
   * @throws RuntimeException if metrics cannot be retrieved
   */
  MemoryPerformanceMetrics getPerformanceMetrics();

  /**
   * Resets all performance metrics and statistics to their initial state.
   *
   * <p>This allows for measurement of memory behavior over specific time periods
   * by clearing historical data while preserving structural information.
   *
   * @throws RuntimeException if metrics cannot be reset
   */
  void resetMetrics();

  /**
   * Analyzes memory access patterns and returns optimization suggestions.
   *
   * <p>This performs advanced analysis of memory usage patterns to identify
   * opportunities for performance optimization and memory efficiency improvements.
   *
   * @return a list of optimization recommendations
   * @throws RuntimeException if analysis cannot be performed
   */
  List<String> analyzeAccessPatterns();

  /**
   * Detects potential memory leaks and fragmentation issues.
   *
   * <p>This operation analyzes memory usage trends and patterns to identify
   * potential issues before they become critical problems.
   *
   * @return a list of potential issues and warnings
   * @throws RuntimeException if leak detection cannot be performed
   */
  List<String> detectMemoryIssues();

  /**
   * Gets detailed information about a specific memory region.
   *
   * <p>This provides comprehensive analysis of a specific memory range including
   * access patterns, protection flags, and usage statistics.
   *
   * @param offset the starting offset of the region to analyze
   * @param length the length of the region to analyze
   * @return detailed region information
   * @throws IllegalArgumentException if offset or length is invalid
   * @throws IndexOutOfBoundsException if the region extends beyond memory bounds
   * @throws RuntimeException if region analysis cannot be performed
   */
  MemorySegment analyzeRegion(final int offset, final int length);

  /**
   * Validates the integrity of memory structures and data.
   *
   * <p>This performs comprehensive validation of memory integrity including
   * corruption detection and structural consistency checks.
   *
   * @return true if memory integrity is valid, false if issues are detected
   * @throws RuntimeException if integrity validation cannot be performed
   */
  boolean validateMemoryIntegrity();

  /**
   * Gets the memory layout and organization information.
   *
   * <p>This provides insight into how memory is organized internally including
   * page boundaries, segment alignment, and allocation strategies.
   *
   * @return a description of the memory layout
   * @throws RuntimeException if layout information cannot be retrieved
   */
  String getMemoryLayout();

  /**
   * Estimates the cost of a potential memory operation without performing it.
   *
   * <p>This allows applications to make informed decisions about memory operations
   * by understanding their performance implications before execution.
   *
   * @param operationType the type of operation to estimate
   * @param offset the memory offset for the operation
   * @param length the length of data involved in the operation
   * @return estimated operation cost in nanoseconds
   * @throws IllegalArgumentException if operationType is null or parameters are invalid
   * @throws RuntimeException if cost estimation cannot be performed
   */
  long estimateOperationCost(final String operationType, final int offset, final int length);
}