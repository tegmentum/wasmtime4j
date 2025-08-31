package ai.tegmentum.wasmtime4j.performance;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Performance baseline for a specific test and runtime combination. */
public final class PerformanceBaseline {
  private final String testName;
  private final RuntimeType runtimeType;
  private final Duration baselineDuration;
  private final Instant lastUpdated;
  private final int measurementCount;

  /**
   * Creates a new performance baseline.
   *
   * @param testName the name of the test
   * @param runtimeType the runtime type
   * @param baselineDuration the baseline duration
   * @param lastUpdated when the baseline was last updated
   * @param measurementCount the number of measurements
   */
  public PerformanceBaseline(
      final String testName,
      final RuntimeType runtimeType,
      final Duration baselineDuration,
      final Instant lastUpdated,
      final int measurementCount) {
    this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    this.runtimeType = Objects.requireNonNull(runtimeType, "runtimeType cannot be null");
    this.baselineDuration =
        Objects.requireNonNull(baselineDuration, "baselineDuration cannot be null");
    this.lastUpdated = Objects.requireNonNull(lastUpdated, "lastUpdated cannot be null");
    this.measurementCount = measurementCount;
  }

  public String getTestName() {
    return testName;
  }

  public RuntimeType getRuntimeType() {
    return runtimeType;
  }

  public Duration getBaselineDuration() {
    return baselineDuration;
  }

  public Instant getLastUpdated() {
    return lastUpdated;
  }

  public int getMeasurementCount() {
    return measurementCount;
  }

  /** Creates a new baseline with an additional measurement using exponential moving average. */
  public PerformanceBaseline withNewMeasurement(final PerformanceMeasurement measurement) {
    // Use exponential moving average with alpha = 0.1 (gives more weight to recent measurements)
    final double alpha = 0.1;
    final long currentMs = baselineDuration.toMillis();
    final long newMs = measurement.getDuration().toMillis();
    final long updatedMs = (long) (alpha * newMs + (1 - alpha) * currentMs);

    return new PerformanceBaseline(
        testName,
        runtimeType,
        Duration.ofMillis(updatedMs),
        measurement.getMeasurementTime(),
        measurementCount + 1);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final PerformanceBaseline that = (PerformanceBaseline) obj;
    return measurementCount == that.measurementCount
        && Objects.equals(testName, that.testName)
        && runtimeType == that.runtimeType
        && Objects.equals(baselineDuration, that.baselineDuration)
        && Objects.equals(lastUpdated, that.lastUpdated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(testName, runtimeType, baselineDuration, lastUpdated, measurementCount);
  }

  @Override
  public String toString() {
    return "PerformanceBaseline{"
        + "testName='"
        + testName
        + '\''
        + ", runtimeType="
        + runtimeType
        + ", baselineDuration="
        + baselineDuration.toMillis()
        + "ms"
        + ", measurementCount="
        + measurementCount
        + ", lastUpdated="
        + lastUpdated
        + '}';
  }
}
