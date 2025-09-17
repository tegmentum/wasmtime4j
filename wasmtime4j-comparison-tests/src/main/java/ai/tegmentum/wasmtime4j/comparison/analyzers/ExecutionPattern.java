package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;

/**
 * Represents the execution pattern observed across runtimes.
 *
 * @since 1.0.0
 */
public final class ExecutionPattern {
  private final int successCount;
  private final int failureCount;
  private final int skipCount;
  private final int uniqueReturnValues;
  private final int uniqueExceptionTypes;
  private final double timingVariance;

  /**
   * Constructs a new ExecutionPattern with the specified execution data.
   *
   * @param successCount the number of successful executions
   * @param failureCount the number of failed executions
   * @param skipCount the number of skipped executions
   * @param uniqueReturnValues the number of unique return values observed
   * @param uniqueExceptionTypes the number of unique exception types observed
   * @param timingVariance the variance in execution timing
   */
  public ExecutionPattern(
      final int successCount,
      final int failureCount,
      final int skipCount,
      final int uniqueReturnValues,
      final int uniqueExceptionTypes,
      final double timingVariance) {
    this.successCount = successCount;
    this.failureCount = failureCount;
    this.skipCount = skipCount;
    this.uniqueReturnValues = uniqueReturnValues;
    this.uniqueExceptionTypes = uniqueExceptionTypes;
    this.timingVariance = timingVariance;
  }

  public int getSuccessCount() {
    return successCount;
  }

  public int getFailureCount() {
    return failureCount;
  }

  public int getSkipCount() {
    return skipCount;
  }

  public int getUniqueReturnValues() {
    return uniqueReturnValues;
  }

  public int getUniqueExceptionTypes() {
    return uniqueExceptionTypes;
  }

  public double getTimingVariance() {
    return timingVariance;
  }

  public int getTotalRuntimes() {
    return successCount + failureCount + skipCount;
  }

  public double getSuccessRate() {
    final int total = getTotalRuntimes();
    return total == 0 ? 0.0 : (double) successCount / total;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ExecutionPattern that = (ExecutionPattern) obj;
    return successCount == that.successCount
        && failureCount == that.failureCount
        && skipCount == that.skipCount
        && uniqueReturnValues == that.uniqueReturnValues
        && uniqueExceptionTypes == that.uniqueExceptionTypes
        && Double.compare(that.timingVariance, timingVariance) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        successCount,
        failureCount,
        skipCount,
        uniqueReturnValues,
        uniqueExceptionTypes,
        timingVariance);
  }

  @Override
  public String toString() {
    return "ExecutionPattern{"
        + "success="
        + successCount
        + ", failure="
        + failureCount
        + ", skip="
        + skipCount
        + ", uniqueValues="
        + uniqueReturnValues
        + ", uniqueExceptions="
        + uniqueExceptionTypes
        + ", timingVar="
        + String.format("%.2f", timingVariance)
        + '}';
  }
}
