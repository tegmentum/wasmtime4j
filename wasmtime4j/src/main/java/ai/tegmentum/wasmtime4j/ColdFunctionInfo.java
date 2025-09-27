package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.util.Objects;

/**
 * Information about a cold (infrequently called) function.
 *
 * <p>Contains metrics and analysis data for functions that are called
 * infrequently during execution, which may be candidates for different
 * optimization strategies.
 *
 * @since 1.0.0
 */
public final class ColdFunctionInfo {

  private final String functionName;
  private final long totalCallCount;
  private final Duration totalExecutionTime;
  private final Duration averageExecutionTime;
  private final double callFrequency;
  private final long lastCallTimestamp;
  private final boolean isCandidate;
  private final String reason;

  private ColdFunctionInfo(final String functionName,
                           final long totalCallCount,
                           final Duration totalExecutionTime,
                           final Duration averageExecutionTime,
                           final double callFrequency,
                           final long lastCallTimestamp,
                           final boolean isCandidate,
                           final String reason) {
    this.functionName = Objects.requireNonNull(functionName);
    this.totalCallCount = totalCallCount;
    this.totalExecutionTime = Objects.requireNonNull(totalExecutionTime);
    this.averageExecutionTime = Objects.requireNonNull(averageExecutionTime);
    this.callFrequency = callFrequency;
    this.lastCallTimestamp = lastCallTimestamp;
    this.isCandidate = isCandidate;
    this.reason = reason;
  }

  /**
   * Creates a new builder for cold function info.
   *
   * @param functionName the function name
   * @return new builder
   */
  public static Builder builder(final String functionName) {
    return new Builder(functionName);
  }

  /**
   * Creates cold function info for a candidate function.
   *
   * @param functionName the function name
   * @param totalCallCount the total call count
   * @param totalExecutionTime the total execution time
   * @param callFrequency the call frequency
   * @param reason the reason for being cold
   * @return cold function info
   */
  public static ColdFunctionInfo candidate(final String functionName,
                                           final long totalCallCount,
                                           final Duration totalExecutionTime,
                                           final double callFrequency,
                                           final String reason) {
    final Duration avgTime = totalCallCount > 0 ?
        Duration.ofNanos(totalExecutionTime.toNanos() / totalCallCount) : Duration.ZERO;

    return new ColdFunctionInfo(functionName, totalCallCount, totalExecutionTime, avgTime,
                                callFrequency, System.nanoTime(), true, reason);
  }

  /**
   * Gets the function name.
   *
   * @return function name
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the total call count.
   *
   * @return total number of calls
   */
  public long getTotalCallCount() {
    return totalCallCount;
  }

  /**
   * Gets the total execution time.
   *
   * @return total execution time
   */
  public Duration getTotalExecutionTime() {
    return totalExecutionTime;
  }

  /**
   * Gets the average execution time per call.
   *
   * @return average execution time
   */
  public Duration getAverageExecutionTime() {
    return averageExecutionTime;
  }

  /**
   * Gets the call frequency (calls per second).
   *
   * @return call frequency
   */
  public double getCallFrequency() {
    return callFrequency;
  }

  /**
   * Gets the timestamp of the last call.
   *
   * @return last call timestamp (nanoseconds)
   */
  public long getLastCallTimestamp() {
    return lastCallTimestamp;
  }

  /**
   * Checks if this function is a candidate for cold optimization.
   *
   * @return true if candidate for cold optimization
   */
  public boolean isCandidate() {
    return isCandidate;
  }

  /**
   * Gets the reason why this function is considered cold.
   *
   * @return reason description
   */
  public String getReason() {
    return reason;
  }

  /**
   * Calculates the percentage of total execution time this function represents.
   *
   * @param totalProgramTime the total program execution time
   * @return percentage of total time (0-100)
   */
  public double getTimePercentage(final Duration totalProgramTime) {
    if (totalProgramTime.isZero()) return 0.0;
    return (double) totalExecutionTime.toNanos() / totalProgramTime.toNanos() * 100.0;
  }

  /**
   * Builder for cold function information.
   */
  public static final class Builder {
    private final String functionName;
    private long totalCallCount = 0;
    private Duration totalExecutionTime = Duration.ZERO;
    private Duration averageExecutionTime = Duration.ZERO;
    private double callFrequency = 0.0;
    private long lastCallTimestamp = 0;
    private boolean isCandidate = false;
    private String reason = "";

    private Builder(final String functionName) {
      this.functionName = Objects.requireNonNull(functionName);
    }

    public Builder totalCallCount(final long totalCallCount) {
      this.totalCallCount = totalCallCount;
      return this;
    }

    public Builder totalExecutionTime(final Duration totalExecutionTime) {
      this.totalExecutionTime = Objects.requireNonNull(totalExecutionTime);
      return this;
    }

    public Builder averageExecutionTime(final Duration averageExecutionTime) {
      this.averageExecutionTime = Objects.requireNonNull(averageExecutionTime);
      return this;
    }

    public Builder callFrequency(final double callFrequency) {
      this.callFrequency = callFrequency;
      return this;
    }

    public Builder lastCallTimestamp(final long lastCallTimestamp) {
      this.lastCallTimestamp = lastCallTimestamp;
      return this;
    }

    public Builder candidate(final boolean isCandidate) {
      this.isCandidate = isCandidate;
      return this;
    }

    public Builder reason(final String reason) {
      this.reason = reason;
      return this;
    }

    public ColdFunctionInfo build() {
      return new ColdFunctionInfo(functionName, totalCallCount, totalExecutionTime,
                                  averageExecutionTime, callFrequency, lastCallTimestamp,
                                  isCandidate, reason);
    }
  }

  @Override
  public String toString() {
    return String.format("ColdFunctionInfo{name='%s', calls=%d, totalTime=%s, frequency=%.2f/s, candidate=%s}",
        functionName, totalCallCount, totalExecutionTime, callFrequency, isCandidate);
  }
}