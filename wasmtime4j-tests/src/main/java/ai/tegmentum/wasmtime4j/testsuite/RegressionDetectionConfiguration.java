package ai.tegmentum.wasmtime4j.testsuite;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Configuration for regression detection in WebAssembly test suite. */
public final class RegressionDetectionConfiguration {

  private final boolean enabled;
  private final Path baselineDirectory;
  private final double performanceThresholdPercent;
  private final boolean failOnRegression;
  private final boolean generateDiffReports;
  private final int historyRetentionDays;

  private RegressionDetectionConfiguration(final Builder builder) {
    this.enabled = builder.enabled;
    this.baselineDirectory = builder.baselineDirectory;
    this.performanceThresholdPercent = builder.performanceThresholdPercent;
    this.failOnRegression = builder.failOnRegression;
    this.generateDiffReports = builder.generateDiffReports;
    this.historyRetentionDays = builder.historyRetentionDays;
  }

  // Getters
  public boolean isEnabled() {
    return enabled;
  }

  public Path getBaselineDirectory() {
    return baselineDirectory;
  }

  public double getPerformanceThresholdPercent() {
    return performanceThresholdPercent;
  }

  public boolean isFailOnRegression() {
    return failOnRegression;
  }

  public boolean isGenerateDiffReports() {
    return generateDiffReports;
  }

  public int getHistoryRetentionDays() {
    return historyRetentionDays;
  }

  /**
   * Creates a default regression detection configuration.
   *
   * @return default configuration
   */
  public static RegressionDetectionConfiguration defaultConfig() {
    return builder().build();
  }

  /**
   * Creates a new builder for RegressionDetectionConfiguration.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for RegressionDetectionConfiguration. */
  public static final class Builder {
    private boolean enabled = false;
    private Path baselineDirectory = Paths.get("target/test-baselines");
    private double performanceThresholdPercent = 10.0; // 10% performance degradation threshold
    private boolean failOnRegression = false;
    private boolean generateDiffReports = true;
    private int historyRetentionDays = 30;

    public Builder enabled(final boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the baseline directory.
     *
     * @param baselineDirectory baseline directory path
     * @return this builder
     */
    public Builder baselineDirectory(final Path baselineDirectory) {
      if (baselineDirectory == null) {
        throw new IllegalArgumentException("Baseline directory cannot be null");
      }
      this.baselineDirectory = baselineDirectory;
      return this;
    }

    /**
     * Sets the performance threshold percentage.
     *
     * @param performanceThresholdPercent threshold percentage
     * @return this builder
     */
    public Builder performanceThresholdPercent(final double performanceThresholdPercent) {
      if (performanceThresholdPercent < 0) {
        throw new IllegalArgumentException("Performance threshold must be non-negative");
      }
      this.performanceThresholdPercent = performanceThresholdPercent;
      return this;
    }

    /**
     * Sets whether to fail on regression.
     *
     * @param failOnRegression true to fail on regression
     * @return this builder
     */
    public Builder failOnRegression(final boolean failOnRegression) {
      this.failOnRegression = failOnRegression;
      return this;
    }

    /**
     * Sets whether to generate diff reports.
     *
     * @param generateDiffReports true to generate diff reports
     * @return this builder
     */
    public Builder generateDiffReports(final boolean generateDiffReports) {
      this.generateDiffReports = generateDiffReports;
      return this;
    }

    /**
     * Sets the history retention days.
     *
     * @param historyRetentionDays number of days to retain history
     * @return this builder
     */
    public Builder historyRetentionDays(final int historyRetentionDays) {
      if (historyRetentionDays < 1) {
        throw new IllegalArgumentException("History retention must be at least 1 day");
      }
      this.historyRetentionDays = historyRetentionDays;
      return this;
    }

    public RegressionDetectionConfiguration build() {
      return new RegressionDetectionConfiguration(this);
    }
  }
}
