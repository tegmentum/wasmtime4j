package ai.tegmentum.wasmtime4j.compilation;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Manages tier transitions for JIT compilation optimization.
 *
 * <p>This class implements sophisticated logic for determining when and how to transition
 * WebAssembly modules between different compilation tiers. It tracks execution patterns,
 * performance characteristics, and compilation costs to make optimal tier transition decisions.
 *
 * <p>Key features:
 * <ul>
 *   <li>Automatic tier progression based on execution frequency</li>
 *   <li>Performance-based tier selection with cost-benefit analysis</li>
 *   <li>Adaptive thresholds that adjust based on system performance</li>
 *   <li>Deoptimization support for performance regressions</li>
 *   <li>Resource-aware compilation scheduling</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class TierTransitionManager {

  private static final Logger LOGGER = Logger.getLogger(TierTransitionManager.class.getName());

  /** Default execution count threshold for tier progression. */
  private static final long DEFAULT_EXECUTION_THRESHOLD = 100;

  /** Default time threshold for tier progression (5 seconds). */
  private static final Duration DEFAULT_TIME_THRESHOLD = Duration.ofSeconds(5);

  /** Maximum compilation overhead percentage before delaying transitions. */
  private static final double MAX_COMPILATION_OVERHEAD = 0.15; // 15%

  /** Minimum performance improvement required for tier transition. */
  private static final double MIN_PERFORMANCE_IMPROVEMENT = 0.10; // 10%

  /** Function execution tracking data. */
  private final ConcurrentHashMap<String, FunctionExecutionTracker> executionTrackers;

  /** System performance monitor for adaptive thresholds. */
  private final SystemPerformanceMonitor performanceMonitor;

  /** Compilation scheduler for managing compilation resources. */
  private final CompilationScheduler compilationScheduler;

  /** Configuration for tier transition behavior. */
  private final TierTransitionConfig config;

  /** Statistics tracking. */
  private final AtomicLong totalTransitions = new AtomicLong(0);
  private final AtomicLong successfulTransitions = new AtomicLong(0);
  private final AtomicLong failedTransitions = new AtomicLong(0);
  private final AtomicLong deoptimizations = new AtomicLong(0);

  /**
   * Creates a new tier transition manager with default configuration.
   */
  public TierTransitionManager() {
    this(TierTransitionConfig.defaultConfig());
  }

  /**
   * Creates a new tier transition manager with custom configuration.
   *
   * @param config tier transition configuration
   */
  public TierTransitionManager(final TierTransitionConfig config) {
    this.config = config;
    this.executionTrackers = new ConcurrentHashMap<>();
    this.performanceMonitor = new SystemPerformanceMonitor();
    this.compilationScheduler = new CompilationScheduler();
  }

  /**
   * Records function execution for tier transition analysis.
   *
   * @param functionId unique function identifier
   * @param currentStrategy current compilation strategy
   * @param executionTimeNs execution time in nanoseconds
   * @param memoryUsage memory usage during execution
   */
  public void recordExecution(final String functionId,
                              final JitCompilationStrategy currentStrategy,
                              final long executionTimeNs,
                              final long memoryUsage) {
    final FunctionExecutionTracker tracker = executionTrackers.computeIfAbsent(
        functionId, k -> new FunctionExecutionTracker(functionId, currentStrategy));

    tracker.recordExecution(executionTimeNs, memoryUsage);

    // Check for tier transition opportunity
    final TierTransitionDecision decision = shouldTransition(tracker);
    if (decision.shouldTransition()) {
      scheduleTransition(functionId, tracker, decision);
    }
  }

  /**
   * Determines if a function should transition to a different compilation tier.
   *
   * @param tracker function execution tracker
   * @return tier transition decision
   */
  public TierTransitionDecision shouldTransition(final FunctionExecutionTracker tracker) {
    final JitCompilationStrategy currentStrategy = tracker.getCurrentStrategy();
    final ExecutionStatistics stats = tracker.getStatistics();

    // Check execution count threshold
    if (stats.getExecutionCount() < getAdaptiveExecutionThreshold()) {
      return TierTransitionDecision.noTransition("Insufficient execution count");
    }

    // Check time threshold
    if (stats.getTotalExecutionTime().compareTo(getAdaptiveTimeThreshold()) < 0) {
      return TierTransitionDecision.noTransition("Insufficient execution time");
    }

    // Determine target tier based on execution patterns
    final JitCompilationStrategy targetStrategy = selectTargetTier(stats);
    if (targetStrategy == currentStrategy) {
      return TierTransitionDecision.noTransition("Already at optimal tier");
    }

    // Analyze cost-benefit of transition
    final CostBenefitAnalysis analysis = analyzeCostBenefit(currentStrategy, targetStrategy, stats);
    if (!analysis.isBeneficial()) {
      return TierTransitionDecision.noTransition(analysis.getReason());
    }

    // Check system resources for compilation
    if (!hasAvailableCompilationResources(targetStrategy)) {
      return TierTransitionDecision.delay("Insufficient compilation resources",
                                         Duration.ofMinutes(1));
    }

    return TierTransitionDecision.transition(targetStrategy, analysis.getEstimatedBenefit());
  }

  /**
   * Schedules a tier transition for execution.
   *
   * @param functionId function identifier
   * @param tracker execution tracker
   * @param decision transition decision
   */
  private void scheduleTransition(final String functionId,
                                  final FunctionExecutionTracker tracker,
                                  final TierTransitionDecision decision) {
    final CompilationTask task = new CompilationTask(
        functionId,
        tracker.getCurrentStrategy(),
        decision.getTargetStrategy(),
        decision.getPriority()
    );

    compilationScheduler.scheduleCompilation(task, (result) -> {
      handleCompilationResult(functionId, tracker, decision, result);
    });

    totalTransitions.incrementAndGet();
    LOGGER.fine(String.format("Scheduled tier transition for %s from %s to %s",
                              functionId, tracker.getCurrentStrategy(),
                              decision.getTargetStrategy()));
  }

  /**
   * Handles compilation result and updates tracking state.
   */
  private void handleCompilationResult(final String functionId,
                                       final FunctionExecutionTracker tracker,
                                       final TierTransitionDecision decision,
                                       final CompilationResult result) {
    if (result.isSuccess()) {
      tracker.transitionToStrategy(decision.getTargetStrategy());
      successfulTransitions.incrementAndGet();

      LOGGER.info(String.format("Successful tier transition for %s to %s (%.2f%% expected improvement)",
                                functionId, decision.getTargetStrategy(),
                                decision.getEstimatedBenefit() * 100));
    } else {
      failedTransitions.incrementAndGet();
      tracker.recordTransitionFailure();

      LOGGER.warning(String.format("Failed tier transition for %s: %s",
                                   functionId, result.getErrorMessage()));
    }
  }

  /**
   * Selects the target compilation tier based on execution statistics.
   */
  private JitCompilationStrategy selectTargetTier(final ExecutionStatistics stats) {
    final long executionCount = stats.getExecutionCount();
    final long totalTimeMs = stats.getTotalExecutionTime().toMillis();
    final double avgTimeMs = stats.getAverageExecutionTime().toNanos() / 1_000_000.0;

    // Hot path detection
    if (isHotPath(stats)) {
      return JitCompilationStrategy.HIGHLY_OPTIMIZED;
    }

    // Frequent execution with good total time
    if (executionCount > config.getHighFrequencyThreshold() ||
        totalTimeMs > config.getHighTimeThreshold()) {
      return JitCompilationStrategy.OPTIMIZED;
    }

    // Moderate execution
    if (executionCount > config.getMediumFrequencyThreshold() ||
        totalTimeMs > config.getMediumTimeThreshold()) {
      return JitCompilationStrategy.STANDARD;
    }

    // Low execution remains baseline
    return JitCompilationStrategy.BASELINE;
  }

  /**
   * Determines if execution statistics indicate a hot path.
   */
  private boolean isHotPath(final ExecutionStatistics stats) {
    final long executionCount = stats.getExecutionCount();
    final double avgTimeMs = stats.getAverageExecutionTime().toNanos() / 1_000_000.0;
    final long totalTimeMs = stats.getTotalExecutionTime().toMillis();

    return executionCount > config.getHotPathExecutionThreshold() ||
           totalTimeMs > config.getHotPathTimeThreshold() ||
           (avgTimeMs > config.getHotPathAvgTimeThreshold() && executionCount > 50);
  }

  /**
   * Analyzes cost-benefit of tier transition.
   */
  private CostBenefitAnalysis analyzeCostBenefit(final JitCompilationStrategy current,
                                                 final JitCompilationStrategy target,
                                                 final ExecutionStatistics stats) {
    // Estimate performance improvement
    final double performanceGain =
        (target.getPerformanceScore() - current.getPerformanceScore()) / 100.0;

    if (performanceGain < MIN_PERFORMANCE_IMPROVEMENT) {
      return CostBenefitAnalysis.notBeneficial("Insufficient performance improvement");
    }

    // Estimate compilation cost
    final long compilationCostMs = target.getTypicalCompilationTimeMs();
    final double avgExecutionMs = stats.getAverageExecutionTime().toNanos() / 1_000_000.0;
    final long executionCount = stats.getExecutionCount();

    // Estimate future benefit
    final long estimatedFutureExecutions = Math.max(executionCount / 2, 100);
    final double timeSavingsPerExecution = avgExecutionMs * performanceGain;
    final double totalTimeSavings = timeSavingsPerExecution * estimatedFutureExecutions;

    // Check if benefits outweigh costs
    if (totalTimeSavings > compilationCostMs * 2.0) {
      return CostBenefitAnalysis.beneficial(performanceGain, totalTimeSavings - compilationCostMs);
    } else {
      return CostBenefitAnalysis.notBeneficial("Cost exceeds estimated benefit");
    }
  }

  /**
   * Gets adaptive execution count threshold based on system performance.
   */
  private long getAdaptiveExecutionThreshold() {
    final double systemLoad = performanceMonitor.getCpuUtilization();
    if (systemLoad > 0.8) {
      return DEFAULT_EXECUTION_THRESHOLD * 2; // Require more executions under high load
    } else if (systemLoad < 0.3) {
      return DEFAULT_EXECUTION_THRESHOLD / 2; // Allow earlier optimization under low load
    }
    return DEFAULT_EXECUTION_THRESHOLD;
  }

  /**
   * Gets adaptive time threshold based on system performance.
   */
  private Duration getAdaptiveTimeThreshold() {
    final double memoryPressure = performanceMonitor.getMemoryPressure();
    if (memoryPressure > 0.8) {
      return DEFAULT_TIME_THRESHOLD.multipliedBy(2);
    } else if (memoryPressure < 0.3) {
      return DEFAULT_TIME_THRESHOLD.dividedBy(2);
    }
    return DEFAULT_TIME_THRESHOLD;
  }

  /**
   * Checks if system has available resources for compilation.
   */
  private boolean hasAvailableCompilationResources(final JitCompilationStrategy strategy) {
    final double cpuUtilization = performanceMonitor.getCpuUtilization();
    final double memoryPressure = performanceMonitor.getMemoryPressure();
    final int activeCompilations = compilationScheduler.getActiveCompilationCount();

    // Check CPU availability
    if (cpuUtilization > 0.9) {
      return false;
    }

    // Check memory pressure
    if (memoryPressure > 0.85) {
      return false;
    }

    // Check compilation queue
    final int maxConcurrentCompilations = Runtime.getRuntime().availableProcessors() / 2;
    if (activeCompilations >= maxConcurrentCompilations) {
      return false;
    }

    return true;
  }

  /**
   * Handles deoptimization when performance regresses after tier transition.
   *
   * @param functionId function identifier
   * @param reason reason for deoptimization
   */
  public void deoptimize(final String functionId, final String reason) {
    final FunctionExecutionTracker tracker = executionTrackers.get(functionId);
    if (tracker != null) {
      final JitCompilationStrategy fallbackStrategy =
          tracker.getCurrentStrategy().getPreviousTier();

      if (fallbackStrategy != null) {
        tracker.transitionToStrategy(fallbackStrategy);
        deoptimizations.incrementAndGet();

        LOGGER.warning(String.format("Deoptimized %s to %s: %s",
                                     functionId, fallbackStrategy, reason));
      }
    }
  }

  /**
   * Gets tier transition statistics.
   *
   * @return transition statistics
   */
  public TierTransitionStatistics getStatistics() {
    return new TierTransitionStatistics(
        totalTransitions.get(),
        successfulTransitions.get(),
        failedTransitions.get(),
        deoptimizations.get(),
        executionTrackers.size(),
        getSuccessRate()
    );
  }

  /**
   * Gets the tier transition success rate.
   */
  private double getSuccessRate() {
    final long total = totalTransitions.get();
    if (total == 0) {
      return 0.0;
    }
    return (double) successfulTransitions.get() / total;
  }

  /**
   * Clears all execution tracking data and resets statistics.
   */
  public void reset() {
    executionTrackers.clear();
    totalTransitions.set(0);
    successfulTransitions.set(0);
    failedTransitions.set(0);
    deoptimizations.set(0);

    LOGGER.info("Tier transition manager reset");
  }
}