package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.time.Instant;
import java.util.Objects;

/**
 * Summary of test execution results.
 *
 * @since 1.0.0
 */
public final class ExecutionSummary {
  private final int totalTests;
  private final int successfulTests;
  private final int failedTests;
  private final int skippedTests;
  private final java.time.Duration totalDuration;
  private final Instant startTime;
  private final Instant endTime;

  /**
   * Constructs a new ExecutionSummary with the specified test execution data.
   *
   * @param totalTests the total number of tests executed
   * @param successfulTests the number of tests that passed
   * @param failedTests the number of tests that failed
   * @param skippedTests the number of tests that were skipped
   * @param totalDuration the total duration of test execution
   * @param startTime the start time of test execution
   * @param endTime the end time of test execution
   */
  public ExecutionSummary(
      final int totalTests,
      final int successfulTests,
      final int failedTests,
      final int skippedTests,
      final java.time.Duration totalDuration,
      final Instant startTime,
      final Instant endTime) {
    this.totalTests = totalTests;
    this.successfulTests = successfulTests;
    this.failedTests = failedTests;
    this.skippedTests = skippedTests;
    this.totalDuration = Objects.requireNonNull(totalDuration, "totalDuration cannot be null");
    this.startTime = Objects.requireNonNull(startTime, "startTime cannot be null");
    this.endTime = Objects.requireNonNull(endTime, "endTime cannot be null");
  }

  public int getTotalTests() {
    return totalTests;
  }

  public int getSuccessfulTests() {
    return successfulTests;
  }

  public int getFailedTests() {
    return failedTests;
  }

  public int getSkippedTests() {
    return skippedTests;
  }

  public java.time.Duration getTotalDuration() {
    return totalDuration;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  /**
   * Gets the success rate as a percentage.
   *
   * @return success rate percentage
   */
  public double getSuccessRate() {
    return totalTests > 0 ? (double) successfulTests / totalTests * 100.0 : 0.0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ExecutionSummary that = (ExecutionSummary) obj;
    return totalTests == that.totalTests
        && successfulTests == that.successfulTests
        && failedTests == that.failedTests
        && skippedTests == that.skippedTests
        && Objects.equals(totalDuration, that.totalDuration)
        && Objects.equals(startTime, that.startTime)
        && Objects.equals(endTime, that.endTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        totalTests, successfulTests, failedTests, skippedTests, totalDuration, startTime, endTime);
  }

  @Override
  public String toString() {
    return "ExecutionSummary{"
        + "total="
        + totalTests
        + ", successful="
        + successfulTests
        + ", failed="
        + failedTests
        + ", duration="
        + totalDuration
        + '}';
  }
}
