package ai.tegmentum.wasmtime4j.compilation;

import java.time.Duration;
import java.util.Objects;

/**
 * Analysis of compilation costs versus expected benefits.
 *
 * <p>Evaluates whether compilation to a higher tier is worthwhile based on
 * execution frequency, compilation costs, and expected performance gains.
 *
 * @since 1.0.0
 */
public final class CostBenefitAnalysis {

  private final String functionName;
  private final CompilationTier currentTier;
  private final CompilationTier proposedTier;
  private final long executionCount;
  private final Duration totalExecutionTime;
  private final Duration estimatedCompilationTime;
  private final double expectedPerformanceGain;
  private final double compilationCost;
  private final double expectedBenefit;
  private final boolean recommended;

  /**
   * Creates a new cost-benefit analysis.
   *
   * @param functionName the function being analyzed
   * @param currentTier the current compilation tier
   * @param proposedTier the proposed compilation tier
   * @param executionCount the number of times the function has been executed
   * @param totalExecutionTime the total time spent executing the function
   * @param estimatedCompilationTime estimated time for compilation to proposed tier
   * @param expectedPerformanceGain expected performance improvement (multiplier)
   */
  public CostBenefitAnalysis(final String functionName,
                             final CompilationTier currentTier,
                             final CompilationTier proposedTier,
                             final long executionCount,
                             final Duration totalExecutionTime,
                             final Duration estimatedCompilationTime,
                             final double expectedPerformanceGain) {
    this.functionName = Objects.requireNonNull(functionName);
    this.currentTier = Objects.requireNonNull(currentTier);
    this.proposedTier = Objects.requireNonNull(proposedTier);
    this.executionCount = executionCount;
    this.totalExecutionTime = Objects.requireNonNull(totalExecutionTime);
    this.estimatedCompilationTime = Objects.requireNonNull(estimatedCompilationTime);
    this.expectedPerformanceGain = expectedPerformanceGain;

    // Calculate cost and benefit
    this.compilationCost = calculateCompilationCost();
    this.expectedBenefit = calculateExpectedBenefit();
    this.recommended = expectedBenefit > compilationCost;
  }

  /**
   * Gets the function name.
   *
   * @return the function name
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the current compilation tier.
   *
   * @return the current tier
   */
  public CompilationTier getCurrentTier() {
    return currentTier;
  }

  /**
   * Gets the proposed compilation tier.
   *
   * @return the proposed tier
   */
  public CompilationTier getProposedTier() {
    return proposedTier;
  }

  /**
   * Gets the execution count.
   *
   * @return the number of executions
   */
  public long getExecutionCount() {
    return executionCount;
  }

  /**
   * Gets the total execution time.
   *
   * @return total time spent executing
   */
  public Duration getTotalExecutionTime() {
    return totalExecutionTime;
  }

  /**
   * Gets the estimated compilation time.
   *
   * @return estimated compilation time
   */
  public Duration getEstimatedCompilationTime() {
    return estimatedCompilationTime;
  }

  /**
   * Gets the expected performance gain.
   *
   * @return expected performance multiplier
   */
  public double getExpectedPerformanceGain() {
    return expectedPerformanceGain;
  }

  /**
   * Gets the compilation cost score.
   *
   * @return compilation cost
   */
  public double getCompilationCost() {
    return compilationCost;
  }

  /**
   * Gets the expected benefit score.
   *
   * @return expected benefit
   */
  public double getExpectedBenefit() {
    return expectedBenefit;
  }

  /**
   * Checks if compilation to the proposed tier is recommended.
   *
   * @return true if recommended, false otherwise
   */
  public boolean isRecommended() {
    return recommended;
  }

  /**
   * Gets the benefit-to-cost ratio.
   *
   * @return ratio of benefit to cost
   */
  public double getBenefitCostRatio() {
    return compilationCost == 0 ? Double.POSITIVE_INFINITY : expectedBenefit / compilationCost;
  }

  /**
   * Gets the payback period - how long it takes for benefits to exceed costs.
   *
   * @return estimated payback period
   */
  public Duration getPaybackPeriod() {
    if (expectedPerformanceGain <= 1.0 || executionCount == 0) {
      return Duration.ofDays(365); // Essentially never
    }

    final double avgExecutionTimeMs = (double) totalExecutionTime.toMillis() / executionCount;
    final double timesSavedPerExecution = avgExecutionTimeMs * (1.0 - (1.0 / expectedPerformanceGain));
    final long compilationTimeMs = estimatedCompilationTime.toMillis();

    if (timesSavedPerExecution <= 0) {
      return Duration.ofDays(365);
    }

    final long executionsNeeded = (long) (compilationTimeMs / timesSavedPerExecution);
    final long avgTimeBetweenExecutions = executionCount > 1 ?
        totalExecutionTime.toMillis() / (executionCount - 1) : 1000; // Assume 1s if only one execution

    return Duration.ofMillis(executionsNeeded * avgTimeBetweenExecutions);
  }

  private double calculateCompilationCost() {
    // Cost is based on compilation time and resource usage
    final long compilationTimeMs = estimatedCompilationTime.toMillis();

    // Base cost is proportional to compilation time
    double cost = compilationTimeMs;

    // Higher tiers have exponentially higher costs
    final double tierMultiplier = switch (proposedTier) {
      case BASELINE -> 1.0;
      case OPTIMIZED -> 5.0;
      case HIGHLY_OPTIMIZED -> 20.0;
    };

    return cost * tierMultiplier;
  }

  private double calculateExpectedBenefit() {
    if (expectedPerformanceGain <= 1.0) {
      return 0.0;
    }

    // Benefit is based on future execution time savings
    final double avgExecutionTimeMs = executionCount > 0 ?
        (double) totalExecutionTime.toMillis() / executionCount : 0.0;

    // Time saved per execution
    final double timeSavedPerExecution = avgExecutionTimeMs * (1.0 - (1.0 / expectedPerformanceGain));

    // Projected future executions (simple heuristic)
    final double projectedFutureExecutions = Math.max(executionCount * 2.0, 100.0);

    return timeSavedPerExecution * projectedFutureExecutions;
  }

  @Override
  public String toString() {
    return String.format("CostBenefitAnalysis{function='%s', %s->%s, executions=%d, " +
            "cost=%.2f, benefit=%.2f, ratio=%.2f, recommended=%s}",
        functionName, currentTier, proposedTier, executionCount,
        compilationCost, expectedBenefit, getBenefitCostRatio(), recommended);
  }
}