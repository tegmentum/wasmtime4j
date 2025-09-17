package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Map;
import java.util.Objects;

/**
 * Result of test execution on a specific runtime.
 *
 * @since 1.0.0
 */
public final class TestExecutionResult {
  private final RuntimeType runtime;
  private final boolean successful;
  private final String output;
  private final String errorMessage;
  private final java.time.Duration executionTime;
  private final Map<String, Object> metrics;

  /**
   * Creates a new test execution result.
   *
   * @param runtime the runtime that executed the test
   * @param successful whether the test was successful
   * @param output the test output
   * @param errorMessage the error message if failed
   * @param executionTime the execution time
   * @param metrics additional metrics collected
   */
  public TestExecutionResult(
      final RuntimeType runtime,
      final boolean successful,
      final String output,
      final String errorMessage,
      final java.time.Duration executionTime,
      final Map<String, Object> metrics) {
    this.runtime = Objects.requireNonNull(runtime, "runtime cannot be null");
    this.successful = successful;
    this.output = output != null ? output : "";
    this.errorMessage = errorMessage != null ? errorMessage : "";
    this.executionTime = Objects.requireNonNull(executionTime, "executionTime cannot be null");
    this.metrics = Map.copyOf(metrics);
  }

  public RuntimeType getRuntime() {
    return runtime;
  }

  public boolean isSuccessful() {
    return successful;
  }

  public String getOutput() {
    return output;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public java.time.Duration getExecutionTime() {
    return executionTime;
  }

  public Map<String, Object> getMetrics() {
    return metrics;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final TestExecutionResult that = (TestExecutionResult) obj;
    return successful == that.successful
        && runtime == that.runtime
        && Objects.equals(output, that.output)
        && Objects.equals(errorMessage, that.errorMessage)
        && Objects.equals(executionTime, that.executionTime)
        && Objects.equals(metrics, that.metrics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(runtime, successful, output, errorMessage, executionTime, metrics);
  }

  @Override
  public String toString() {
    return "TestExecutionResult{"
        + "runtime="
        + runtime
        + ", successful="
        + successful
        + ", executionTime="
        + executionTime
        + '}';
  }
}
