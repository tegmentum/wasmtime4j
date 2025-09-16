package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration class that defines tolerance levels for different types of value comparisons in the
 * behavioral analysis framework. Provides fine-grained control over what constitutes "equivalent"
 * values across different data types.
 *
 * @since 1.0.0
 */
public final class ToleranceConfiguration {
  private static final double DEFAULT_DOUBLE_TOLERANCE = 1e-9;
  private static final float DEFAULT_FLOAT_TOLERANCE = 1e-6f;
  private static final BigDecimal DEFAULT_BIGDECIMAL_TOLERANCE = new BigDecimal("1e-15");
  private static final Duration DEFAULT_TIMING_TOLERANCE = Duration.ofMillis(50);
  private static final long DEFAULT_MEMORY_TOLERANCE_BYTES = 1024L; // 1KB

  private final double doubleTolerance;
  private final float floatTolerance;
  private final BigDecimal bigDecimalTolerance;
  private final Duration timingTolerance;
  private final long memoryToleranceBytes;

  private ToleranceConfiguration(final Builder builder) {
    this.doubleTolerance = builder.doubleTolerance;
    this.floatTolerance = builder.floatTolerance;
    this.bigDecimalTolerance = builder.bigDecimalTolerance;
    this.timingTolerance = builder.timingTolerance;
    this.memoryToleranceBytes = builder.memoryToleranceBytes;
  }

  /**
   * Creates a default tolerance configuration suitable for most use cases.
   *
   * @return default tolerance configuration
   */
  public static ToleranceConfiguration defaultConfig() {
    return new Builder().build();
  }

  /**
   * Creates a strict tolerance configuration with minimal tolerance levels.
   *
   * @return strict tolerance configuration
   */
  public static ToleranceConfiguration strictConfig() {
    return new Builder()
        .doubleTolerance(1e-12)
        .floatTolerance(1e-9f)
        .bigDecimalTolerance(new BigDecimal("1e-18"))
        .timingTolerance(Duration.ofMillis(10))
        .memoryToleranceBytes(256L)
        .build();
  }

  /**
   * Creates a lenient tolerance configuration with higher tolerance levels.
   *
   * @return lenient tolerance configuration
   */
  public static ToleranceConfiguration lenientConfig() {
    return new Builder()
        .doubleTolerance(1e-6)
        .floatTolerance(1e-3f)
        .bigDecimalTolerance(new BigDecimal("1e-12"))
        .timingTolerance(Duration.ofMillis(200))
        .memoryToleranceBytes(4096L)
        .build();
  }

  public double getDoubleTolerance() {
    return doubleTolerance;
  }

  public float getFloatTolerance() {
    return floatTolerance;
  }

  public BigDecimal getBigDecimalTolerance() {
    return bigDecimalTolerance;
  }

  public Duration getTimingTolerance() {
    return timingTolerance;
  }

  public long getMemoryToleranceBytes() {
    return memoryToleranceBytes;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ToleranceConfiguration that = (ToleranceConfiguration) obj;
    return Double.compare(that.doubleTolerance, doubleTolerance) == 0
        && Float.compare(that.floatTolerance, floatTolerance) == 0
        && memoryToleranceBytes == that.memoryToleranceBytes
        && Objects.equals(bigDecimalTolerance, that.bigDecimalTolerance)
        && Objects.equals(timingTolerance, that.timingTolerance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        doubleTolerance,
        floatTolerance,
        bigDecimalTolerance,
        timingTolerance,
        memoryToleranceBytes);
  }

  @Override
  public String toString() {
    return "ToleranceConfiguration{"
        + "doubleTolerance="
        + doubleTolerance
        + ", floatTolerance="
        + floatTolerance
        + ", bigDecimalTolerance="
        + bigDecimalTolerance
        + ", timingTolerance="
        + timingTolerance
        + ", memoryToleranceBytes="
        + memoryToleranceBytes
        + '}';
  }

  /** Builder for ToleranceConfiguration. */
  public static final class Builder {
    private double doubleTolerance = DEFAULT_DOUBLE_TOLERANCE;
    private float floatTolerance = DEFAULT_FLOAT_TOLERANCE;
    private BigDecimal bigDecimalTolerance = DEFAULT_BIGDECIMAL_TOLERANCE;
    private Duration timingTolerance = DEFAULT_TIMING_TOLERANCE;
    private long memoryToleranceBytes = DEFAULT_MEMORY_TOLERANCE_BYTES;

    public Builder doubleTolerance(final double doubleTolerance) {
      if (doubleTolerance < 0) {
        throw new IllegalArgumentException("doubleTolerance must be non-negative");
      }
      this.doubleTolerance = doubleTolerance;
      return this;
    }

    public Builder floatTolerance(final float floatTolerance) {
      if (floatTolerance < 0) {
        throw new IllegalArgumentException("floatTolerance must be non-negative");
      }
      this.floatTolerance = floatTolerance;
      return this;
    }

    public Builder bigDecimalTolerance(final BigDecimal bigDecimalTolerance) {
      Objects.requireNonNull(bigDecimalTolerance, "bigDecimalTolerance cannot be null");
      if (bigDecimalTolerance.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException("bigDecimalTolerance must be non-negative");
      }
      this.bigDecimalTolerance = bigDecimalTolerance;
      return this;
    }

    public Builder timingTolerance(final Duration timingTolerance) {
      Objects.requireNonNull(timingTolerance, "timingTolerance cannot be null");
      if (timingTolerance.isNegative()) {
        throw new IllegalArgumentException("timingTolerance must be non-negative");
      }
      this.timingTolerance = timingTolerance;
      return this;
    }

    public Builder memoryToleranceBytes(final long memoryToleranceBytes) {
      if (memoryToleranceBytes < 0) {
        throw new IllegalArgumentException("memoryToleranceBytes must be non-negative");
      }
      this.memoryToleranceBytes = memoryToleranceBytes;
      return this;
    }

    public ToleranceConfiguration build() {
      return new ToleranceConfiguration(this);
    }
  }
}
