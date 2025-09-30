package ai.tegmentum.wasmtime4j.testsuite;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Container for WebAssembly test execution results across multiple runtimes. */
public final class TestExecutionResults {

  private final Map<TestRuntime, List<TestResult>> runtimeResults;
  private final Instant executionStartTime;
  private final Instant executionEndTime;
  private final long totalExecutionTimeMs;

  private TestExecutionResults(final Builder builder) {
    this.runtimeResults = Map.copyOf(builder.runtimeResults);
    this.executionStartTime =
        builder.executionStartTime != null ? builder.executionStartTime : Instant.now();
    this.executionEndTime =
        builder.executionEndTime != null ? builder.executionEndTime : Instant.now();
    this.totalExecutionTimeMs =
        builder.totalExecutionTimeMs > 0
            ? builder.totalExecutionTimeMs
            : (executionEndTime.toEpochMilli() - executionStartTime.toEpochMilli());
  }

  // Getters
  public Map<TestRuntime, List<TestResult>> getRuntimeResults() {
    return runtimeResults;
  }

  public Instant getExecutionStartTime() {
    return executionStartTime;
  }

  public Instant getExecutionEndTime() {
    return executionEndTime;
  }

  public long getTotalExecutionTimeMs() {
    return totalExecutionTimeMs;
  }

  /**
   * Gets test results for a specific runtime.
   *
   * @param runtime runtime to get results for
   * @return list of test results, or empty list if runtime not found
   */
  public List<TestResult> getResultsForRuntime(final TestRuntime runtime) {
    return runtimeResults.getOrDefault(runtime, Collections.emptyList());
  }

  /**
   * Gets the total number of test cases executed across all runtimes.
   *
   * @return total test count
   */
  public int getTotalTestCount() {
    return runtimeResults.values().stream().mapToInt(List::size).sum();
  }

  /**
   * Gets the total number of passed tests across all runtimes.
   *
   * @return total passed count
   */
  public int getTotalPassedCount() {
    return runtimeResults.values().stream()
        .flatMap(List::stream)
        .mapToInt(result -> result.isSuccess() ? 1 : 0)
        .sum();
  }

  /**
   * Gets the total number of failed tests across all runtimes.
   *
   * @return total failed count
   */
  public int getTotalFailedCount() {
    return runtimeResults.values().stream()
        .flatMap(List::stream)
        .mapToInt(result -> result.isFailure() ? 1 : 0)
        .sum();
  }

  /**
   * Gets all test results flattened across all runtimes.
   *
   * @return list of all test results
   */
  public List<TestResult> getAllResults() {
    return runtimeResults.values().stream().flatMap(List::stream).collect(Collectors.toList());
  }

  /**
   * Gets test results grouped by test case ID across all runtimes.
   *
   * @return map of test case ID to list of results (one per runtime)
   */
  public Map<String, List<TestResult>> getResultsByTestCase() {
    final Map<String, List<TestResult>> resultsByTestCase = new HashMap<>();

    for (final List<TestResult> results : runtimeResults.values()) {
      for (final TestResult result : results) {
        final String testId = result.getTestCase().getTestId();
        resultsByTestCase.computeIfAbsent(testId, k -> new ArrayList<>()).add(result);
      }
    }

    return Map.copyOf(resultsByTestCase);
  }

  /**
   * Checks if all tests passed across all runtimes.
   *
   * @return true if all tests passed
   */
  public boolean isAllTestsPassed() {
    return getTotalFailedCount() == 0 && getTotalTestCount() > 0;
  }

  /**
   * Gets the pass rate as a percentage.
   *
   * @return pass rate (0.0 to 100.0)
   */
  public double getPassRate() {
    final int totalTests = getTotalTestCount();
    if (totalTests == 0) {
      return 0.0;
    }
    return (double) getTotalPassedCount() / totalTests * 100.0;
  }

  /**
   * Creates a new builder for TestExecutionResults.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String toString() {
    return String.format(
        "TestExecutionResults{runtimes=%d, total=%d, passed=%d, failed=%d, passRate=%.1f%%,"
            + " duration=%dms}",
        runtimeResults.size(),
        getTotalTestCount(),
        getTotalPassedCount(),
        getTotalFailedCount(),
        getPassRate(),
        totalExecutionTimeMs);
  }

  /** Builder for TestExecutionResults. */
  public static final class Builder {
    private final Map<TestRuntime, List<TestResult>> runtimeResults = new HashMap<>();
    private Instant executionStartTime;
    private Instant executionEndTime;
    private long totalExecutionTimeMs;

    public Builder addRuntimeResults(final TestRuntime runtime, final List<TestResult> results) {
      if (runtime == null) {
        throw new IllegalArgumentException("Runtime cannot be null");
      }
      if (results == null) {
        throw new IllegalArgumentException("Results cannot be null");
      }
      this.runtimeResults.put(runtime, List.copyOf(results));
      return this;
    }

    public Builder executionStartTime(final Instant executionStartTime) {
      this.executionStartTime = executionStartTime;
      return this;
    }

    public Builder executionEndTime(final Instant executionEndTime) {
      this.executionEndTime = executionEndTime;
      return this;
    }

    public Builder totalExecutionTimeMs(final long totalExecutionTimeMs) {
      this.totalExecutionTimeMs = totalExecutionTimeMs;
      return this;
    }

    public TestExecutionResults build() {
      if (runtimeResults.isEmpty()) {
        throw new IllegalStateException("At least one runtime result must be provided");
      }
      return new TestExecutionResults(this);
    }
  }
}
