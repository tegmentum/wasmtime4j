package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Summary of cross-runtime test execution results. */
public final class CrossRuntimeExecutionSummary {
  private final int totalTests;
  private final Map<RuntimeType, Integer> successCounts;
  private final Map<RuntimeType, Integer> failureCounts;
  private final Map<RuntimeType, Integer> skipCounts;
  private final Map<RuntimeType, Duration> totalDurations;
  private final List<CrossRuntimeTestResult> allResults;
  private final int consistentResults;
  private final int inconsistentResults;

  private CrossRuntimeExecutionSummary(final Builder builder) {
    this.totalTests = builder.totalTests;
    this.successCounts = Collections.unmodifiableMap(new EnumMap<>(builder.successCounts));
    this.failureCounts = Collections.unmodifiableMap(new EnumMap<>(builder.failureCounts));
    this.skipCounts = Collections.unmodifiableMap(new EnumMap<>(builder.skipCounts));
    this.totalDurations = Collections.unmodifiableMap(new EnumMap<>(builder.totalDurations));
    this.allResults = Collections.unmodifiableList(new ArrayList<>(builder.allResults));
    this.consistentResults = builder.consistentResults;
    this.inconsistentResults = builder.inconsistentResults;
  }

  /**
   * Gets the total number of tests executed.
   *
   * @return the total test count
   */
  public int getTotalTests() {
    return totalTests;
  }

  /**
   * Gets the success count for a runtime.
   *
   * @param runtimeType the runtime type
   * @return the success count
   */
  public int getSuccessCount(final RuntimeType runtimeType) {
    return successCounts.getOrDefault(runtimeType, 0);
  }

  /**
   * Gets the failure count for a runtime.
   *
   * @param runtimeType the runtime type
   * @return the failure count
   */
  public int getFailureCount(final RuntimeType runtimeType) {
    return failureCounts.getOrDefault(runtimeType, 0);
  }

  /**
   * Gets the skip count for a runtime.
   *
   * @param runtimeType the runtime type
   * @return the skip count
   */
  public int getSkipCount(final RuntimeType runtimeType) {
    return skipCounts.getOrDefault(runtimeType, 0);
  }

  /**
   * Gets the total execution duration for a runtime.
   *
   * @param runtimeType the runtime type
   * @return the total duration
   */
  public Duration getTotalDuration(final RuntimeType runtimeType) {
    return totalDurations.getOrDefault(runtimeType, Duration.ZERO);
  }

  /**
   * Gets the number of consistent results between runtimes.
   *
   * @return the consistent results count
   */
  public int getConsistentResults() {
    return consistentResults;
  }

  /**
   * Gets the number of inconsistent results between runtimes.
   *
   * @return the inconsistent results count
   */
  public int getInconsistentResults() {
    return inconsistentResults;
  }

  /**
   * Gets all test results.
   *
   * @return the list of all results
   */
  public List<CrossRuntimeTestResult> getAllResults() {
    return allResults;
  }

  /**
   * Gets the success rate for a runtime.
   *
   * @param runtimeType the runtime type
   * @return the success rate as a percentage
   */
  public double getSuccessRate(final RuntimeType runtimeType) {
    final int total = getSuccessCount(runtimeType) + getFailureCount(runtimeType);
    if (total == 0) {
      return 0.0;
    }
    return (double) getSuccessCount(runtimeType) / total * 100.0;
  }

  /**
   * Gets the consistency rate between runtimes.
   *
   * @return the consistency rate as a percentage
   */
  public double getConsistencyRate() {
    final int total = consistentResults + inconsistentResults;
    if (total == 0) {
      return 0.0;
    }
    return (double) consistentResults / total * 100.0;
  }

  /**
   * Creates a formatted report.
   *
   * @return the formatted report
   */
  public String createReport() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Cross-Runtime Test Execution Summary\n");
    sb.append("=====================================\n\n");

    sb.append("Total Tests: ").append(totalTests).append('\n');
    sb.append("Consistent Results: ").append(consistentResults).append('\n');
    sb.append("Inconsistent Results: ").append(inconsistentResults).append('\n');
    sb.append("Consistency Rate: ")
        .append(String.format("%.1f%%", getConsistencyRate()))
        .append('\n');
    sb.append('\n');

    for (final RuntimeType runtimeType : RuntimeType.values()) {
      if (successCounts.containsKey(runtimeType)) {
        sb.append(runtimeType).append(" Runtime:\n");
        sb.append("  Success: ").append(getSuccessCount(runtimeType)).append('\n');
        sb.append("  Failure: ").append(getFailureCount(runtimeType)).append('\n');
        sb.append("  Skipped: ").append(getSkipCount(runtimeType)).append('\n');
        sb.append("  Success Rate: ")
            .append(String.format("%.1f%%", getSuccessRate(runtimeType)))
            .append('\n');
        sb.append("  Total Duration: ")
            .append(getTotalDuration(runtimeType).toMillis())
            .append("ms\n");
        sb.append('\n');
      }
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    return "CrossRuntimeExecutionSummary{"
        + "totalTests="
        + totalTests
        + ", consistentResults="
        + consistentResults
        + ", inconsistentResults="
        + inconsistentResults
        + ", consistencyRate="
        + String.format("%.1f%%", getConsistencyRate())
        + '}';
  }

  /** Builder for execution summaries. */
  public static final class Builder {
    private int totalTests = 0;
    private final Map<RuntimeType, Integer> successCounts = new EnumMap<>(RuntimeType.class);
    private final Map<RuntimeType, Integer> failureCounts = new EnumMap<>(RuntimeType.class);
    private final Map<RuntimeType, Integer> skipCounts = new EnumMap<>(RuntimeType.class);
    private final Map<RuntimeType, Duration> totalDurations = new EnumMap<>(RuntimeType.class);
    private final List<CrossRuntimeTestResult> allResults = new ArrayList<>();
    private int consistentResults = 0;
    private int inconsistentResults = 0;

    /** Creates a new Builder instance. */
    public Builder() {
      // Initialize counts for all runtime types
      for (final RuntimeType runtimeType : RuntimeType.values()) {
        successCounts.put(runtimeType, 0);
        failureCounts.put(runtimeType, 0);
        skipCounts.put(runtimeType, 0);
        totalDurations.put(runtimeType, Duration.ZERO);
      }
    }

    /**
     * Adds a test result to the summary being built.
     *
     * @param result the test result to add
     * @return this builder for method chaining
     */
    public Builder addTestResult(final CrossRuntimeTestResult result) {
      totalTests++;
      allResults.add(result);

      // Process JNI result
      processRuntimeResult(RuntimeType.JNI, result.getJniResult());

      // Process Panama result if available
      if (result.hasPanamaResult()) {
        processRuntimeResult(RuntimeType.PANAMA, result.getPanamaResult());

        // Check consistency
        if (result.bothSuccessful()) {
          // Both successful - check if results are consistent
          final Object jniResult = result.getJniResult().getResult();
          final Object panamaResult = result.getPanamaResult().getResult();

          if (java.util.Objects.equals(jniResult, panamaResult)) {
            consistentResults++;
          } else {
            inconsistentResults++;
          }
        } else if (result.getJniResult().isSuccessful()
            == result.getPanamaResult().isSuccessful()) {
          // Both failed or both skipped - consider consistent
          consistentResults++;
        } else {
          // One succeeded, one failed - inconsistent
          inconsistentResults++;
        }
      }

      return this;
    }

    private void processRuntimeResult(
        final RuntimeType runtimeType, final RuntimeTestExecution execution) {
      if (execution.isSkipped()) {
        skipCounts.put(runtimeType, skipCounts.get(runtimeType) + 1);
      } else if (execution.isSuccessful()) {
        successCounts.put(runtimeType, successCounts.get(runtimeType) + 1);
      } else {
        failureCounts.put(runtimeType, failureCounts.get(runtimeType) + 1);
      }

      // Add duration
      final Duration currentDuration = totalDurations.get(runtimeType);
      totalDurations.put(runtimeType, currentDuration.plus(execution.getDuration()));
    }

    public CrossRuntimeExecutionSummary build() {
      return new CrossRuntimeExecutionSummary(this);
    }
  }
}
