package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Represents the execution result of a test against a specific WebAssembly runtime. */
public final class RuntimeTestExecution {
  private final RuntimeType runtimeType;
  private final boolean successful;
  private final Object result;
  private final Exception exception;
  private final Duration duration;
  private final boolean skipped;
  private final String skipReason;

  private RuntimeTestExecution(
      final RuntimeType runtimeType,
      final boolean successful,
      final Object result,
      final Exception exception,
      final Duration duration,
      final boolean skipped,
      final String skipReason) {
    this.runtimeType = Objects.requireNonNull(runtimeType, "runtimeType cannot be null");
    this.successful = successful;
    this.result = result;
    this.exception = exception;
    this.duration = Objects.requireNonNull(duration, "duration cannot be null");
    this.skipped = skipped;
    this.skipReason = skipReason;
  }

  /**
   * Creates a successful test execution result.
   *
   * @param runtimeType the runtime type
   * @param result the test result
   * @param duration the execution duration
   * @return the execution result
   */
  public static RuntimeTestExecution successful(
      final RuntimeType runtimeType, final Object result, final Duration duration) {
    return new RuntimeTestExecution(runtimeType, true, result, null, duration, false, null);
  }

  /**
   * Creates a failed test execution result.
   *
   * @param runtimeType the runtime type
   * @param exception the failure exception
   * @param duration the execution duration
   * @return the execution result
   */
  public static RuntimeTestExecution failed(
      final RuntimeType runtimeType, final Exception exception, final Duration duration) {
    return new RuntimeTestExecution(runtimeType, false, null, exception, duration, false, null);
  }

  /**
   * Creates a skipped test execution result.
   *
   * @param runtimeType the runtime type
   * @param skipReason the reason for skipping
   * @return the execution result
   */
  public static RuntimeTestExecution skipped(
      final RuntimeType runtimeType, final String skipReason) {
    return new RuntimeTestExecution(
        runtimeType, false, null, null, Duration.ZERO, true, skipReason);
  }

  /**
   * Gets the runtime type.
   *
   * @return the runtime type
   */
  public RuntimeType getRuntimeType() {
    return runtimeType;
  }

  /**
   * Checks if the test execution was successful.
   *
   * @return true if successful
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets the test result.
   *
   * @return the test result if successful, null otherwise
   */
  public Object getResult() {
    return result;
  }

  /**
   * Gets the failure exception.
   *
   * @return the exception if failed, empty otherwise
   */
  public Optional<Exception> getException() {
    return Optional.ofNullable(exception);
  }

  /**
   * Gets the execution duration.
   *
   * @return the execution duration
   */
  public Duration getDuration() {
    return duration;
  }

  /**
   * Checks if the test was skipped.
   *
   * @return true if skipped
   */
  public boolean isSkipped() {
    return skipped;
  }

  /**
   * Gets the skip reason.
   *
   * @return the skip reason if skipped, empty otherwise
   */
  public Optional<String> getSkipReason() {
    return Optional.ofNullable(skipReason);
  }

  /**
   * Gets the execution status as a string.
   *
   * @return the status string
   */
  public String getStatus() {
    if (skipped) {
      return "SKIPPED";
    } else if (successful) {
      return "SUCCESS";
    } else {
      return "FAILED";
    }
  }

  /**
   * Gets a summary of the execution.
   *
   * @return the execution summary
   */
  public String getSummary() {
    final StringBuilder sb = new StringBuilder();
    sb.append(runtimeType).append(": ").append(getStatus());

    if (skipped) {
      sb.append(" (").append(skipReason).append(")");
    } else {
      sb.append(" in ").append(duration.toMillis()).append("ms");
      if (!successful && exception != null) {
        sb.append(" - ")
            .append(exception.getClass().getSimpleName())
            .append(": ")
            .append(exception.getMessage());
      }
    }

    return sb.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final RuntimeTestExecution that = (RuntimeTestExecution) obj;
    return successful == that.successful
        && skipped == that.skipped
        && runtimeType == that.runtimeType
        && Objects.equals(result, that.result)
        && Objects.equals(exception, that.exception)
        && Objects.equals(duration, that.duration)
        && Objects.equals(skipReason, that.skipReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(runtimeType, successful, result, exception, duration, skipped, skipReason);
  }

  @Override
  public String toString() {
    return "RuntimeTestExecution{"
        + "runtimeType="
        + runtimeType
        + ", successful="
        + successful
        + ", result="
        + result
        + ", exception="
        + exception
        + ", duration="
        + duration
        + ", skipped="
        + skipped
        + ", skipReason='"
        + skipReason
        + '\''
        + '}';
  }
}
