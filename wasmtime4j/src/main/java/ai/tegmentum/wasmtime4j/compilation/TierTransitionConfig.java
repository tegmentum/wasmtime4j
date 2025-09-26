package ai.tegmentum.wasmtime4j.compilation;

import java.time.Duration;

/**
 * Configuration for tier transition thresholds and behavior.
 *
 * <p>This class defines the parameters that control when and how WebAssembly functions
 * transition between compilation tiers. These settings can be tuned based on application
 * characteristics and performance requirements.
 *
 * @since 1.0.0
 */
public final class TierTransitionConfig {

  // Execution count thresholds
  private final long mediumFrequencyThreshold;
  private final long highFrequencyThreshold;
  private final long hotPathExecutionThreshold;

  // Time-based thresholds
  private final long mediumTimeThreshold; // milliseconds
  private final long highTimeThreshold;   // milliseconds
  private final long hotPathTimeThreshold; // milliseconds
  private final double hotPathAvgTimeThreshold; // milliseconds

  // Performance thresholds
  private final double minPerformanceImprovement;
  private final double regressionThreshold;
  private final double maxCompilationOverhead;

  // Resource management
  private final int maxConcurrentCompilations;
  private final Duration compilationTimeout;
  private final long maxMemoryForCompilation; // bytes

  // Adaptive behavior
  private final boolean enableAdaptiveThresholds;
  private final double thresholdAdjustmentFactor;
  private final Duration performanceWindowSize;

  private TierTransitionConfig(final Builder builder) {
    this.mediumFrequencyThreshold = builder.mediumFrequencyThreshold;
    this.highFrequencyThreshold = builder.highFrequencyThreshold;
    this.hotPathExecutionThreshold = builder.hotPathExecutionThreshold;
    this.mediumTimeThreshold = builder.mediumTimeThreshold;
    this.highTimeThreshold = builder.highTimeThreshold;
    this.hotPathTimeThreshold = builder.hotPathTimeThreshold;
    this.hotPathAvgTimeThreshold = builder.hotPathAvgTimeThreshold;
    this.minPerformanceImprovement = builder.minPerformanceImprovement;
    this.regressionThreshold = builder.regressionThreshold;
    this.maxCompilationOverhead = builder.maxCompilationOverhead;
    this.maxConcurrentCompilations = builder.maxConcurrentCompilations;
    this.compilationTimeout = builder.compilationTimeout;
    this.maxMemoryForCompilation = builder.maxMemoryForCompilation;
    this.enableAdaptiveThresholds = builder.enableAdaptiveThresholds;
    this.thresholdAdjustmentFactor = builder.thresholdAdjustmentFactor;
    this.performanceWindowSize = builder.performanceWindowSize;
  }

  /**
   * Creates default configuration suitable for most applications.
   *
   * @return default configuration
   */
  public static TierTransitionConfig defaultConfig() {
    return new Builder().build();
  }

  /**
   * Creates configuration optimized for server applications.
   *
   * @return server-optimized configuration
   */
  public static TierTransitionConfig serverOptimized() {
    return new Builder()
        .mediumFrequencyThreshold(500)
        .highFrequencyThreshold(2000)
        .hotPathExecutionThreshold(10000)
        .hotPathTimeThreshold(20000)
        .maxConcurrentCompilations(4)
        .enableAdaptiveThresholds(true)
        .build();
  }

  /**
   * Creates configuration optimized for desktop applications.
   *
   * @return desktop-optimized configuration
   */
  public static TierTransitionConfig desktopOptimized() {
    return new Builder()
        .mediumFrequencyThreshold(100)
        .highFrequencyThreshold(500)
        .hotPathExecutionThreshold(2000)
        .maxConcurrentCompilations(2)
        .maxCompilationOverhead(0.1) // Lower overhead tolerance
        .build();
  }

  /**
   * Creates configuration optimized for mobile/embedded applications.
   *
   * @return mobile-optimized configuration
   */
  public static TierTransitionConfig mobileOptimized() {
    return new Builder()
        .mediumFrequencyThreshold(50)
        .highFrequencyThreshold(200)
        .hotPathExecutionThreshold(1000)
        .maxConcurrentCompilations(1)
        .maxCompilationOverhead(0.05) // Very low overhead tolerance
        .maxMemoryForCompilation(64 * 1024 * 1024) // 64MB limit
        .compilationTimeout(Duration.ofSeconds(5))
        .build();
  }

  // Getters
  public long getMediumFrequencyThreshold() { return mediumFrequencyThreshold; }
  public long getHighFrequencyThreshold() { return highFrequencyThreshold; }
  public long getHotPathExecutionThreshold() { return hotPathExecutionThreshold; }
  public long getMediumTimeThreshold() { return mediumTimeThreshold; }
  public long getHighTimeThreshold() { return highTimeThreshold; }
  public long getHotPathTimeThreshold() { return hotPathTimeThreshold; }
  public double getHotPathAvgTimeThreshold() { return hotPathAvgTimeThreshold; }
  public double getMinPerformanceImprovement() { return minPerformanceImprovement; }
  public double getRegressionThreshold() { return regressionThreshold; }
  public double getMaxCompilationOverhead() { return maxCompilationOverhead; }
  public int getMaxConcurrentCompilations() { return maxConcurrentCompilations; }
  public Duration getCompilationTimeout() { return compilationTimeout; }
  public long getMaxMemoryForCompilation() { return maxMemoryForCompilation; }
  public boolean isAdaptiveThresholdsEnabled() { return enableAdaptiveThresholds; }
  public double getThresholdAdjustmentFactor() { return thresholdAdjustmentFactor; }
  public Duration getPerformanceWindowSize() { return performanceWindowSize; }

  /**
   * Adjusts thresholds based on system performance if adaptive thresholds are enabled.
   *
   * @param cpuUtilization current CPU utilization (0.0 to 1.0)
   * @param memoryPressure current memory pressure (0.0 to 1.0)
   * @return adjusted configuration
   */
  public TierTransitionConfig adjustForSystemPerformance(final double cpuUtilization,
                                                         final double memoryPressure) {
    if (!enableAdaptiveThresholds) {
      return this;
    }

    final double adjustmentFactor = calculateAdjustmentFactor(cpuUtilization, memoryPressure);

    return new Builder(this)
        .mediumFrequencyThreshold((long) (mediumFrequencyThreshold * adjustmentFactor))
        .highFrequencyThreshold((long) (highFrequencyThreshold * adjustmentFactor))
        .hotPathExecutionThreshold((long) (hotPathExecutionThreshold * adjustmentFactor))
        .mediumTimeThreshold((long) (mediumTimeThreshold * adjustmentFactor))
        .highTimeThreshold((long) (highTimeThreshold * adjustmentFactor))
        .hotPathTimeThreshold((long) (hotPathTimeThreshold * adjustmentFactor))
        .build();
  }

  private double calculateAdjustmentFactor(final double cpuUtilization, final double memoryPressure) {
    // Under high load, increase thresholds to delay expensive optimizations
    if (cpuUtilization > 0.8 || memoryPressure > 0.8) {
      return 1.0 + thresholdAdjustmentFactor;
    }

    // Under low load, decrease thresholds to enable earlier optimizations
    if (cpuUtilization < 0.3 && memoryPressure < 0.3) {
      return 1.0 - (thresholdAdjustmentFactor * 0.5);
    }

    // Normal load, no adjustment
    return 1.0;
  }

  @Override
  public String toString() {
    return String.format(
        "TierTransitionConfig{medium=%d, high=%d, hotPath=%d, " +
        "adaptive=%b, maxConcurrent=%d, timeout=%s}",
        mediumFrequencyThreshold,
        highFrequencyThreshold,
        hotPathExecutionThreshold,
        enableAdaptiveThresholds,
        maxConcurrentCompilations,
        compilationTimeout
    );
  }

  /**
   * Builder for creating tier transition configurations.
   */
  public static final class Builder {
    private long mediumFrequencyThreshold = 100;
    private long highFrequencyThreshold = 1000;
    private long hotPathExecutionThreshold = 5000;
    private long mediumTimeThreshold = 1000; // 1 second
    private long highTimeThreshold = 10000;  // 10 seconds
    private long hotPathTimeThreshold = 30000; // 30 seconds
    private double hotPathAvgTimeThreshold = 10.0; // 10ms average
    private double minPerformanceImprovement = 0.10; // 10%
    private double regressionThreshold = 0.15; // 15%
    private double maxCompilationOverhead = 0.20; // 20%
    private int maxConcurrentCompilations = Runtime.getRuntime().availableProcessors() / 2;
    private Duration compilationTimeout = Duration.ofMinutes(2);
    private long maxMemoryForCompilation = 256 * 1024 * 1024; // 256MB
    private boolean enableAdaptiveThresholds = true;
    private double thresholdAdjustmentFactor = 0.5; // 50% adjustment
    private Duration performanceWindowSize = Duration.ofMinutes(5);

    public Builder() {}

    public Builder(final TierTransitionConfig config) {
      this.mediumFrequencyThreshold = config.mediumFrequencyThreshold;
      this.highFrequencyThreshold = config.highFrequencyThreshold;
      this.hotPathExecutionThreshold = config.hotPathExecutionThreshold;
      this.mediumTimeThreshold = config.mediumTimeThreshold;
      this.highTimeThreshold = config.highTimeThreshold;
      this.hotPathTimeThreshold = config.hotPathTimeThreshold;
      this.hotPathAvgTimeThreshold = config.hotPathAvgTimeThreshold;
      this.minPerformanceImprovement = config.minPerformanceImprovement;
      this.regressionThreshold = config.regressionThreshold;
      this.maxCompilationOverhead = config.maxCompilationOverhead;
      this.maxConcurrentCompilations = config.maxConcurrentCompilations;
      this.compilationTimeout = config.compilationTimeout;
      this.maxMemoryForCompilation = config.maxMemoryForCompilation;
      this.enableAdaptiveThresholds = config.enableAdaptiveThresholds;
      this.thresholdAdjustmentFactor = config.thresholdAdjustmentFactor;
      this.performanceWindowSize = config.performanceWindowSize;
    }

    public Builder mediumFrequencyThreshold(final long threshold) {
      this.mediumFrequencyThreshold = threshold;
      return this;
    }

    public Builder highFrequencyThreshold(final long threshold) {
      this.highFrequencyThreshold = threshold;
      return this;
    }

    public Builder hotPathExecutionThreshold(final long threshold) {
      this.hotPathExecutionThreshold = threshold;
      return this;
    }

    public Builder mediumTimeThreshold(final long threshold) {
      this.mediumTimeThreshold = threshold;
      return this;
    }

    public Builder highTimeThreshold(final long threshold) {
      this.highTimeThreshold = threshold;
      return this;
    }

    public Builder hotPathTimeThreshold(final long threshold) {
      this.hotPathTimeThreshold = threshold;
      return this;
    }

    public Builder hotPathAvgTimeThreshold(final double threshold) {
      this.hotPathAvgTimeThreshold = threshold;
      return this;
    }

    public Builder minPerformanceImprovement(final double improvement) {
      this.minPerformanceImprovement = improvement;
      return this;
    }

    public Builder regressionThreshold(final double threshold) {
      this.regressionThreshold = threshold;
      return this;
    }

    public Builder maxCompilationOverhead(final double overhead) {
      this.maxCompilationOverhead = overhead;
      return this;
    }

    public Builder maxConcurrentCompilations(final int count) {
      this.maxConcurrentCompilations = count;
      return this;
    }

    public Builder compilationTimeout(final Duration timeout) {
      this.compilationTimeout = timeout;
      return this;
    }

    public Builder maxMemoryForCompilation(final long bytes) {
      this.maxMemoryForCompilation = bytes;
      return this;
    }

    public Builder enableAdaptiveThresholds(final boolean enable) {
      this.enableAdaptiveThresholds = enable;
      return this;
    }

    public Builder thresholdAdjustmentFactor(final double factor) {
      this.thresholdAdjustmentFactor = factor;
      return this;
    }

    public Builder performanceWindowSize(final Duration windowSize) {
      this.performanceWindowSize = windowSize;
      return this;
    }

    public TierTransitionConfig build() {
      validateConfiguration();
      return new TierTransitionConfig(this);
    }

    private void validateConfiguration() {
      if (mediumFrequencyThreshold <= 0) {
        throw new IllegalArgumentException("Medium frequency threshold must be positive");
      }
      if (highFrequencyThreshold <= mediumFrequencyThreshold) {
        throw new IllegalArgumentException("High frequency threshold must be greater than medium");
      }
      if (hotPathExecutionThreshold <= highFrequencyThreshold) {
        throw new IllegalArgumentException("Hot path threshold must be greater than high frequency");
      }
      if (minPerformanceImprovement <= 0 || minPerformanceImprovement > 1) {
        throw new IllegalArgumentException("Performance improvement must be between 0 and 1");
      }
      if (maxConcurrentCompilations <= 0) {
        throw new IllegalArgumentException("Max concurrent compilations must be positive");
      }
    }
  }
}