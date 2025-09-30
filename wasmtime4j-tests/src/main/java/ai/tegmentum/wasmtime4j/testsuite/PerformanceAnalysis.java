package ai.tegmentum.wasmtime4j.testsuite;

import java.util.List;
import java.util.Map;

/** Analysis results for test performance metrics and regression detection. */
public final class PerformanceAnalysis {

  private final Map<TestRuntime, Double> averageExecutionTimes;
  private final List<String> performanceIssues;

  public PerformanceAnalysis(
      final Map<TestRuntime, Double> averageExecutionTimes, final List<String> performanceIssues) {
    this.averageExecutionTimes = Map.copyOf(averageExecutionTimes);
    this.performanceIssues = List.copyOf(performanceIssues);
  }

  public Map<TestRuntime, Double> getAverageExecutionTimes() {
    return averageExecutionTimes;
  }

  public List<String> getPerformanceIssues() {
    return performanceIssues;
  }

  /**
   * Checks if there are significant performance regressions.
   *
   * @return true if regressions exist
   */
  public boolean hasSignificantRegressions() {
    return !performanceIssues.isEmpty();
  }
}
