package ai.tegmentum.wasmtime4j.performance;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.Objects;

public final class PerformanceRegression {
  private final RuntimeType runtimeType;
  private final Duration baselineDuration;
  private final Duration currentDuration;
  private final long regressionMs;
  private final double regressionPercent;

  public PerformanceRegression(
      final RuntimeType runtimeType,
      final Duration baselineDuration,
      final Duration currentDuration,
      final long regressionMs,
      final double regressionPercent) {
    this.runtimeType = Objects.requireNonNull(runtimeType);
    this.baselineDuration = Objects.requireNonNull(baselineDuration);
    this.currentDuration = Objects.requireNonNull(currentDuration);
    this.regressionMs = regressionMs;
    this.regressionPercent = regressionPercent;
  }

  public RuntimeType getRuntimeType() {
    return runtimeType;
  }

  public Duration getBaselineDuration() {
    return baselineDuration;
  }

  public Duration getCurrentDuration() {
    return currentDuration;
  }

  public long getRegressionMs() {
    return regressionMs;
  }

  public double getRegressionPercent() {
    return regressionPercent;
  }

  @Override
  public String toString() {
    return "PerformanceRegression{runtimeType="
        + runtimeType
        + ", regressionPercent="
        + String.format("%.1f%%", regressionPercent)
        + ", regressionMs="
        + regressionMs
        + "ms}";
  }
}
