package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.Map;
import java.util.Optional;

/**
 * Represents the performance impact of a WebAssembly error or issue.
 *
 * <p>This interface provides metrics and analysis about how an error affects system performance,
 * resource usage, and execution characteristics.
 *
 * @since 1.0.0
 */
public interface PerformanceImpact {

  /** Performance impact severity levels. */
  enum Severity {
    /** Minimal performance impact */
    MINIMAL,
    /** Low performance impact */
    LOW,
    /** Moderate performance impact */
    MODERATE,
    /** High performance impact */
    HIGH,
    /** Severe performance impact */
    SEVERE,
    /** Critical performance impact requiring immediate attention */
    CRITICAL
  }

  /** Categories of performance impact. */
  enum Category {
    /** CPU usage impact */
    CPU,
    /** Memory usage impact */
    MEMORY,
    /** I/O operations impact */
    IO,
    /** Network operations impact */
    NETWORK,
    /** Compilation time impact */
    COMPILATION,
    /** Execution time impact */
    EXECUTION,
    /** Startup time impact */
    STARTUP,
    /** Throughput impact */
    THROUGHPUT,
    /** Latency impact */
    LATENCY,
    /** Resource allocation impact */
    RESOURCE_ALLOCATION
  }

  /**
   * Gets the performance impact severity.
   *
   * @return the impact severity
   */
  Severity getSeverity();

  /**
   * Gets the performance impact category.
   *
   * @return the impact category
   */
  Category getCategory();

  /**
   * Gets the estimated percentage impact on performance.
   *
   * @return the performance impact percentage (0-100), or empty if not measurable
   */
  Optional<Double> getImpactPercentage();

  /**
   * Gets the estimated time overhead caused by this issue.
   *
   * @return the time overhead in milliseconds, or empty if not measurable
   */
  Optional<Long> getTimeOverheadMs();

  /**
   * Gets the estimated memory overhead caused by this issue.
   *
   * @return the memory overhead in bytes, or empty if not measurable
   */
  Optional<Long> getMemoryOverheadBytes();

  /**
   * Gets the estimated CPU overhead caused by this issue.
   *
   * @return the CPU overhead as a percentage (0-100), or empty if not measurable
   */
  Optional<Double> getCpuOverheadPercentage();

  /**
   * Gets the baseline performance metrics before the issue.
   *
   * @return the baseline metrics, or empty if not available
   */
  Optional<PerformanceMetrics> getBaselineMetrics();

  /**
   * Gets the current performance metrics with the issue present.
   *
   * @return the current metrics, or empty if not available
   */
  Optional<PerformanceMetrics> getCurrentMetrics();

  /**
   * Gets the projected performance metrics after fixing the issue.
   *
   * @return the projected metrics, or empty if not available
   */
  Optional<PerformanceMetrics> getProjectedMetrics();

  /**
   * Gets the performance degradation trends over time.
   *
   * @return the degradation trends, or empty if not available
   */
  Optional<PerformanceTrend> getDegradationTrend();

  /**
   * Gets detailed performance analysis.
   *
   * @return the performance analysis
   */
  String getAnalysis();

  /**
   * Gets performance optimization recommendations.
   *
   * @return the optimization recommendations
   */
  String getOptimizationRecommendations();

  /**
   * Gets additional performance-related properties.
   *
   * @return map of performance properties
   */
  Map<String, Object> getProperties();

  /**
   * Checks if the performance impact is considered acceptable.
   *
   * @return true if the impact is within acceptable limits
   */
  boolean isAcceptable();

  /**
   * Checks if immediate action is required due to performance impact.
   *
   * @return true if immediate action is required
   */
  boolean requiresImmediateAction();

  /**
   * Gets the performance impact assessment timestamp.
   *
   * @return the assessment timestamp
   */
  long getAssessmentTimestamp();

  /**
   * Creates a builder for constructing PerformanceImpact instances.
   *
   * @return a new performance impact builder
   */
  static PerformanceImpactBuilder builder() {
    return new PerformanceImpactBuilder();
  }

  /**
   * Creates a PerformanceImpact with basic information.
   *
   * @param severity the impact severity
   * @param category the impact category
   * @param analysis the performance analysis
   * @return the performance impact
   */
  static PerformanceImpact of(
      final Severity severity, final Category category, final String analysis) {
    return builder().severity(severity).category(category).analysis(analysis).build();
  }
}
