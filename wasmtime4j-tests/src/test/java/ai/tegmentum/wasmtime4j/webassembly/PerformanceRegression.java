package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a performance regression detected by comparing current performance results against
 * baseline performance results.
 */
public final class PerformanceRegression {
  private final String testName;
  private final RuntimeType runtimeType;
  private final Duration baselinePerformance;
  private final Duration currentPerformance;
  private final double regressionPercentage;
  private final Instant detectionTime;

  /**
   * Creates a performance regression record.
   *
   * @param testName the name of the test that regressed
   * @param runtimeType the runtime type where regression was detected
   * @param baselinePerformance the baseline performance time
   * @param currentPerformance the current performance time
   * @param regressionPercentage the regression as a percentage (positive = slower)
   */
  public PerformanceRegression(
      final String testName,
      final RuntimeType runtimeType,
      final Duration baselinePerformance,
      final Duration currentPerformance,
      final double regressionPercentage) {
    this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    this.runtimeType = Objects.requireNonNull(runtimeType, "runtimeType cannot be null");
    this.baselinePerformance =
        Objects.requireNonNull(baselinePerformance, "baselinePerformance cannot be null");
    this.currentPerformance =
        Objects.requireNonNull(currentPerformance, "currentPerformance cannot be null");
    this.regressionPercentage = regressionPercentage;
    this.detectionTime = Instant.now();
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
   * Gets the runtime type where regression was detected.
   *
   * @return the runtime type
   */
  public RuntimeType getRuntimeType() {
    return runtimeType;
  }

  /**
   * Gets the baseline performance time.
   *
   * @return the baseline performance time
   */
  public Duration getBaselinePerformance() {
    return baselinePerformance;
  }

  /**
   * Gets the current performance time.
   *
   * @return the current performance time
   */
  public Duration getCurrentPerformance() {
    return currentPerformance;
  }

  /**
   * Gets the regression percentage.
   *
   * @return the regression as a percentage (positive = slower)
   */
  public double getRegressionPercentage() {
    return regressionPercentage;
  }

  /**
   * Gets the time when this regression was detected.
   *
   * @return the detection time
   */
  public Instant getDetectionTime() {
    return detectionTime;
  }

  /**
   * Gets the absolute performance difference.
   *
   * @return the absolute performance difference
   */
  public Duration getPerformanceDifference() {
    return currentPerformance.minus(baselinePerformance);
  }

  /**
   * Checks if this is a severe regression (>50% performance loss).
   *
   * @return true if this is a severe regression
   */
  public boolean isSevereRegression() {
    return regressionPercentage > 0.5;
  }

  /**
   * Checks if this is a moderate regression (20-50% performance loss).
   *
   * @return true if this is a moderate regression
   */
  public boolean isModerateRegression() {
    return regressionPercentage >= 0.2 && regressionPercentage <= 0.5;
  }

  /**
   * Checks if this is a minor regression (5-20% performance loss).
   *
   * @return true if this is a minor regression
   */
  public boolean isMinorRegression() {
    return regressionPercentage >= 0.05 && regressionPercentage < 0.2;
  }

  /**
   * Gets the severity level of this regression.
   *
   * @return the severity level
   */
  public RegressionSeverity getSeverity() {
    if (isSevereRegression()) {
      return RegressionSeverity.SEVERE;
    } else if (isModerateRegression()) {
      return RegressionSeverity.MODERATE;
    } else if (isMinorRegression()) {
      return RegressionSeverity.MINOR;
    } else {
      return RegressionSeverity.NEGLIGIBLE;
    }
  }

  /**
   * Creates a formatted report of this regression.
   *
   * @return a formatted report
   */
  public String createReport() {
    final StringBuilder report = new StringBuilder();
    report.append("Performance Regression Detected\n");
    report.append("===============================\n\n");

    report.append(String.format("Test: %s\n", testName));
    report.append(String.format("Runtime: %s\n", runtimeType.name()));
    report.append(String.format("Severity: %s\n", getSeverity().getDescription()));
    report.append(String.format("Detection Time: %s\n\n", detectionTime));

    report.append(
        String.format(
            "Baseline Performance: %.3fms\n", baselinePerformance.toNanos() / 1_000_000.0));
    report.append(
        String.format("Current Performance: %.3fms\n", currentPerformance.toNanos() / 1_000_000.0));
    report.append(
        String.format(
            "Performance Difference: +%.3fms\n",
            getPerformanceDifference().toNanos() / 1_000_000.0));
    report.append(String.format("Regression Percentage: +%.1f%%\n", regressionPercentage * 100));

    return report.toString();
  }

  /**
   * Creates a brief summary suitable for logging or alerts.
   *
   * @return a brief summary
   */
  public String createBriefSummary() {
    return String.format(
        "[%s] %s: %s regression (+%.1f%%) on %s - %.3fms → %.3fms",
        getSeverity().name(),
        testName,
        getSeverity().getDescription().toLowerCase(),
        regressionPercentage * 100,
        runtimeType.name(),
        baselinePerformance.toNanos() / 1_000_000.0,
        currentPerformance.toNanos() / 1_000_000.0);
  }

  @Override
  public String toString() {
    return String.format(
        "PerformanceRegression{test=%s, runtime=%s, regression=+%.1f%%}",
        testName, runtimeType.name(), regressionPercentage * 100);
  }

  /** Severity levels for performance regressions. */
  public enum RegressionSeverity {
    SEVERE("Severe regression requiring immediate attention"),
    MODERATE("Moderate regression requiring investigation"),
    MINOR("Minor regression - monitor for trends"),
    NEGLIGIBLE("Negligible regression - within normal variance");

    private final String description;

    RegressionSeverity(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }
}
