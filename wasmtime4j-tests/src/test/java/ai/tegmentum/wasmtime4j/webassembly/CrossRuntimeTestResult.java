package ai.tegmentum.wasmtime4j.webassembly;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Result of executing a test across multiple WebAssembly runtimes. */
public final class CrossRuntimeTestResult {
  private final String testName;
  private final RuntimeTestExecution jniResult;
  private final RuntimeTestExecution panamaResult;
  private final Instant executionTime;

  private CrossRuntimeTestResult(final Builder builder) {
    this.testName = Objects.requireNonNull(builder.testName, "testName cannot be null");
    this.jniResult = Objects.requireNonNull(builder.jniResult, "jniResult cannot be null");
    this.panamaResult = builder.panamaResult;
    this.executionTime = builder.executionTime;
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
   * Gets the JNI runtime execution result.
   *
   * @return the JNI execution result
   */
  public RuntimeTestExecution getJniExecution() {
    return jniResult;
  }

  /**
   * Gets the Panama runtime execution result.
   *
   * @return the Panama execution result if available, null otherwise
   */
  public RuntimeTestExecution getPanamaExecution() {
    return panamaResult;
  }

  /**
   * Gets the execution time.
   *
   * @return the execution time
   */
  public Instant getExecutionTime() {
    return executionTime;
  }

  /**
   * Gets the JNI runtime execution result.
   *
   * @return the JNI execution result
   */
  public RuntimeTestExecution getJniResult() {
    return jniResult;
  }

  /**
   * Gets the Panama runtime execution result.
   *
   * @return the Panama execution result if available, null otherwise
   */
  public RuntimeTestExecution getPanamaResult() {
    return panamaResult;
  }

  /**
   * Checks if both runtimes executed successfully.
   *
   * @return true if both runtimes succeeded
   */
  public boolean bothSuccessful() {
    return jniResult.isSuccessful() && (panamaResult == null || panamaResult.isSuccessful());
  }

  /**
   * Checks if any runtime failed.
   *
   * @return true if any runtime failed
   */
  public boolean anyFailed() {
    return !jniResult.isSuccessful()
        || (panamaResult != null && !panamaResult.isSuccessful() && !panamaResult.isSkipped());
  }

  /**
   * Checks if Panama runtime was tested.
   *
   * @return true if Panama was tested
   */
  public boolean hasPanamaResult() {
    return panamaResult != null;
  }

  /**
   * Gets the total execution duration across all runtimes.
   *
   * @return the total duration
   */
  public Duration getTotalDuration() {
    Duration total = jniResult.getDuration();
    if (panamaResult != null) {
      total = total.plus(panamaResult.getDuration());
    }
    return total;
  }

  /**
   * Gets a summary of the test execution.
   *
   * @return the execution summary
   */
  public String getSummary() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Test: ").append(testName).append(" - ");

    if (bothSuccessful()) {
      sb.append("ALL PASSED");
    } else if (anyFailed()) {
      sb.append("SOME FAILED");
    } else {
      sb.append("MIXED RESULTS");
    }

    sb.append(" (Total time: ").append(getTotalDuration().toMillis()).append("ms)");
    return sb.toString();
  }

  /**
   * Gets a detailed report of the execution.
   *
   * @return the detailed report
   */
  public String getDetailedReport() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Cross-Runtime Test Result: ").append(testName).append('\n');
    sb.append("Execution Time: ").append(executionTime).append('\n');
    sb.append("Total Duration: ").append(getTotalDuration().toMillis()).append("ms\n");
    sb.append('\n');

    sb.append("JNI Runtime:\n");
    sb.append("  ").append(jniResult.getSummary()).append('\n');
    if (jniResult.getException().isPresent()) {
      sb.append("  Exception: ").append(jniResult.getException().get()).append('\n');
    }

    if (panamaResult != null) {
      sb.append('\n');
      sb.append("Panama Runtime:\n");
      sb.append("  ").append(panamaResult.getSummary()).append('\n');
      if (panamaResult.getException().isPresent()) {
        sb.append("  Exception: ").append(panamaResult.getException().get()).append('\n');
      }
    }

    sb.append('\n');
    sb.append("Overall Result: ").append(getSummary());

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

    final CrossRuntimeTestResult that = (CrossRuntimeTestResult) obj;
    return Objects.equals(testName, that.testName)
        && Objects.equals(jniResult, that.jniResult)
        && Objects.equals(panamaResult, that.panamaResult)
        && Objects.equals(executionTime, that.executionTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(testName, jniResult, panamaResult, executionTime);
  }

  @Override
  public String toString() {
    return "CrossRuntimeTestResult{"
        + "testName='"
        + testName
        + '\''
        + ", jniResult="
        + jniResult
        + ", panamaResult="
        + panamaResult
        + ", executionTime="
        + executionTime
        + '}';
  }

  /** Builder for creating cross-runtime test results. */
  public static final class Builder {
    private final String testName;
    private RuntimeTestExecution jniResult;
    private RuntimeTestExecution panamaResult;
    private final Instant executionTime;

    /**
     * Creates a new builder.
     *
     * @param testName the test name
     */
    public Builder(final String testName) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
      this.executionTime = Instant.now();
    }

    /**
     * Sets the JNI execution result.
     *
     * @param jniResult the JNI result
     * @return this builder
     */
    public Builder jniResult(final RuntimeTestExecution jniResult) {
      this.jniResult = Objects.requireNonNull(jniResult, "jniResult cannot be null");
      return this;
    }

    /**
     * Sets the Panama execution result.
     *
     * @param panamaResult the Panama result
     * @return this builder
     */
    public Builder panamaResult(final RuntimeTestExecution panamaResult) {
      this.panamaResult = panamaResult; // Can be null if Panama is not available
      return this;
    }

    /**
     * Builds the cross-runtime test result.
     *
     * @return the test result
     */
    public CrossRuntimeTestResult build() {
      if (jniResult == null) {
        throw new IllegalStateException("JNI result is required");
      }
      return new CrossRuntimeTestResult(this);
    }
  }
}
