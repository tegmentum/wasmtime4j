package ai.tegmentum.wasmtime4j.compilation;

import java.time.Duration;
import java.util.Objects;

/**
 * Preferences for optimization strategies and compilation behavior.
 *
 * <p>Defines user and system preferences for how aggressive optimization
 * should be, balancing compilation time against execution performance.
 *
 * @since 1.0.0
 */
public final class OptimizationPreferences {

  private final OptimizationMode mode;
  private final Duration maxCompilationTime;
  private final double performanceThreshold;
  private final int executionCountThreshold;
  private final boolean enableSpeculativeOptimization;
  private final boolean enableProfileGuidedOptimization;
  private final boolean enableAdaptiveOptimization;
  private final double memoryUsageLimit;
  private final int maxConcurrentCompilations;

  private OptimizationPreferences(final Builder builder) {
    this.mode = builder.mode;
    this.maxCompilationTime = builder.maxCompilationTime;
    this.performanceThreshold = builder.performanceThreshold;
    this.executionCountThreshold = builder.executionCountThreshold;
    this.enableSpeculativeOptimization = builder.enableSpeculativeOptimization;
    this.enableProfileGuidedOptimization = builder.enableProfileGuidedOptimization;
    this.enableAdaptiveOptimization = builder.enableAdaptiveOptimization;
    this.memoryUsageLimit = builder.memoryUsageLimit;
    this.maxConcurrentCompilations = builder.maxConcurrentCompilations;
  }

  /**
   * Creates a new builder for optimization preferences.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates default optimization preferences.
   *
   * @return default preferences
   */
  public static OptimizationPreferences defaultPreferences() {
    return builder().build();
  }

  /**
   * Creates optimization preferences optimized for speed.
   *
   * @return speed-optimized preferences
   */
  public static OptimizationPreferences forSpeed() {
    return builder()
        .mode(OptimizationMode.AGGRESSIVE)
        .maxCompilationTime(Duration.ofSeconds(10))
        .performanceThreshold(1.5)
        .executionCountThreshold(100)
        .enableSpeculativeOptimization(true)
        .enableProfileGuidedOptimization(true)
        .enableAdaptiveOptimization(true)
        .build();
  }

  /**
   * Creates optimization preferences optimized for fast startup.
   *
   * @return startup-optimized preferences
   */
  public static OptimizationPreferences forStartup() {
    return builder()
        .mode(OptimizationMode.CONSERVATIVE)
        .maxCompilationTime(Duration.ofMillis(100))
        .performanceThreshold(2.0)
        .executionCountThreshold(1000)
        .enableSpeculativeOptimization(false)
        .enableProfileGuidedOptimization(false)
        .enableAdaptiveOptimization(true)
        .build();
  }

  /**
   * Creates optimization preferences optimized for memory usage.
   *
   * @return memory-optimized preferences
   */
  public static OptimizationPreferences forMemory() {
    return builder()
        .mode(OptimizationMode.BALANCED)
        .maxCompilationTime(Duration.ofSeconds(5))
        .memoryUsageLimit(0.8)
        .maxConcurrentCompilations(2)
        .build();
  }

  // Getters
  public OptimizationMode getMode() { return mode; }
  public Duration getMaxCompilationTime() { return maxCompilationTime; }
  public double getPerformanceThreshold() { return performanceThreshold; }
  public int getExecutionCountThreshold() { return executionCountThreshold; }
  public boolean isSpeculativeOptimizationEnabled() { return enableSpeculativeOptimization; }
  public boolean isProfileGuidedOptimizationEnabled() { return enableProfileGuidedOptimization; }
  public boolean isAdaptiveOptimizationEnabled() { return enableAdaptiveOptimization; }
  public double getMemoryUsageLimit() { return memoryUsageLimit; }
  public int getMaxConcurrentCompilations() { return maxConcurrentCompilations; }

  /**
   * Builder for optimization preferences.
   */
  public static final class Builder {
    private OptimizationMode mode = OptimizationMode.BALANCED;
    private Duration maxCompilationTime = Duration.ofSeconds(5);
    private double performanceThreshold = 2.0;
    private int executionCountThreshold = 500;
    private boolean enableSpeculativeOptimization = true;
    private boolean enableProfileGuidedOptimization = true;
    private boolean enableAdaptiveOptimization = true;
    private double memoryUsageLimit = 0.9;
    private int maxConcurrentCompilations = 4;

    public Builder mode(final OptimizationMode mode) {
      this.mode = Objects.requireNonNull(mode);
      return this;
    }

    public Builder maxCompilationTime(final Duration maxCompilationTime) {
      this.maxCompilationTime = Objects.requireNonNull(maxCompilationTime);
      return this;
    }

    public Builder performanceThreshold(final double performanceThreshold) {
      this.performanceThreshold = performanceThreshold;
      return this;
    }

    public Builder executionCountThreshold(final int executionCountThreshold) {
      this.executionCountThreshold = executionCountThreshold;
      return this;
    }

    public Builder enableSpeculativeOptimization(final boolean enable) {
      this.enableSpeculativeOptimization = enable;
      return this;
    }

    public Builder enableProfileGuidedOptimization(final boolean enable) {
      this.enableProfileGuidedOptimization = enable;
      return this;
    }

    public Builder enableAdaptiveOptimization(final boolean enable) {
      this.enableAdaptiveOptimization = enable;
      return this;
    }

    public Builder memoryUsageLimit(final double memoryUsageLimit) {
      this.memoryUsageLimit = memoryUsageLimit;
      return this;
    }

    public Builder maxConcurrentCompilations(final int maxConcurrentCompilations) {
      this.maxConcurrentCompilations = maxConcurrentCompilations;
      return this;
    }

    public OptimizationPreferences build() {
      return new OptimizationPreferences(this);
    }
  }

  /**
   * Optimization modes defining the overall strategy.
   */
  public enum OptimizationMode {
    /** Conservative optimization - prioritize compilation speed */
    CONSERVATIVE,

    /** Balanced optimization - balance compilation time and performance */
    BALANCED,

    /** Aggressive optimization - prioritize execution performance */
    AGGRESSIVE
  }
}