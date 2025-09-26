package ai.tegmentum.wasmtime4j.compilation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages compilation optimization strategies and their application.
 *
 * <p>This class orchestrates the selection and application of optimization strategies
 * based on execution profiles, system constraints, and performance requirements.
 * It handles strategy dependencies, conflicts, and provides intelligent optimization
 * pipeline construction.
 *
 * @since 1.0.0
 */
public final class OptimizationStrategyManager {

  private static final Logger LOGGER = Logger.getLogger(OptimizationStrategyManager.class.getName());

  /** Registry of available optimization strategies. */
  private final Map<String, OptimizationStrategy> strategies;

  /** Strategy selection preferences. */
  private final OptimizationPreferences preferences;

  /** Performance tracking for strategy effectiveness. */
  private final Map<String, StrategyPerformanceTracker> performanceTrackers;

  /**
   * Creates an optimization strategy manager with default strategies.
   */
  public OptimizationStrategyManager() {
    this(OptimizationPreferences.balanced());
  }

  /**
   * Creates an optimization strategy manager with custom preferences.
   *
   * @param preferences optimization preferences
   */
  public OptimizationStrategyManager(final OptimizationPreferences preferences) {
    this.preferences = preferences;
    this.strategies = new ConcurrentHashMap<>();
    this.performanceTrackers = new ConcurrentHashMap<>();

    // Register default optimization strategies
    registerDefaultStrategies();
  }

  /**
   * Registers default optimization strategies.
   */
  private void registerDefaultStrategies() {
    // Core optimizations
    registerStrategy(new DeadCodeEliminationOptimization());
    registerStrategy(new InliningOptimization());
    registerStrategy(new LoopOptimization());
    registerStrategy(new VectorizationOptimization());

    // Additional optimizations
    registerStrategy(new ConstantFoldingOptimization());
    registerStrategy(new CommonSubexpressionEliminationOptimization());
  }

  /**
   * Registers an optimization strategy.
   *
   * @param strategy optimization strategy to register
   */
  public void registerStrategy(final OptimizationStrategy strategy) {
    strategies.put(strategy.getName(), strategy);
    performanceTrackers.put(strategy.getName(), new StrategyPerformanceTracker(strategy.getName()));
    LOGGER.fine("Registered optimization strategy: " + strategy.getName());
  }

  /**
   * Unregisters an optimization strategy.
   *
   * @param strategyName name of strategy to unregister
   */
  public void unregisterStrategy(final String strategyName) {
    strategies.remove(strategyName);
    performanceTrackers.remove(strategyName);
    LOGGER.fine("Unregistered optimization strategy: " + strategyName);
  }

  /**
   * Selects optimal optimization strategies for given execution profile.
   *
   * @param executionProfile execution profile data
   * @param compilationStrategy compilation strategy context
   * @return ordered list of optimization strategies
   */
  public List<OptimizationStrategy> selectStrategies(final ExecutionProfile executionProfile,
                                                     final JitCompilationStrategy compilationStrategy) {
    // Filter applicable strategies
    final List<OptimizationStrategy> applicable = strategies.values().stream()
        .filter(strategy -> strategy.isApplicable(executionProfile))
        .filter(strategy -> isStrategyAllowedForTier(strategy, compilationStrategy))
        .collect(Collectors.toList());

    // Sort by effectiveness and compatibility
    final List<OptimizationStrategy> selected = selectOptimalCombination(applicable, executionProfile);

    // Resolve dependencies and conflicts
    final List<OptimizationStrategy> resolved = resolveDependenciesAndConflicts(selected);

    LOGGER.fine(String.format("Selected %d optimization strategies for %s compilation",
                              resolved.size(), compilationStrategy.getName()));

    return resolved;
  }

  /**
   * Determines if a strategy is allowed for the given compilation tier.
   */
  private boolean isStrategyAllowedForTier(final OptimizationStrategy strategy,
                                           final JitCompilationStrategy tier) {
    switch (tier) {
      case BASELINE:
        // Only lightweight optimizations for baseline
        return strategy.getOptimizationLevel() <= 4;

      case STANDARD:
        // Moderate optimizations for standard
        return strategy.getOptimizationLevel() <= 6;

      case OPTIMIZED:
        // Most optimizations allowed
        return strategy.getOptimizationLevel() <= 8;

      case HIGHLY_OPTIMIZED:
        // All optimizations allowed
        return true;

      case ADAPTIVE:
        // Depends on current adaptive state
        return strategy.getOptimizationLevel() <= 7;

      default:
        return true;
    }
  }

  /**
   * Selects optimal combination of strategies based on cost-benefit analysis.
   */
  private List<OptimizationStrategy> selectOptimalCombination(final List<OptimizationStrategy> candidates,
                                                              final ExecutionProfile profile) {
    final List<StrategyEvaluation> evaluations = candidates.stream()
        .map(strategy -> evaluateStrategy(strategy, profile))
        .sorted((e1, e2) -> Double.compare(e2.getEffectivenessScore(), e1.getEffectivenessScore()))
        .collect(Collectors.toList());

    final List<OptimizationStrategy> selected = new ArrayList<>();
    double totalOverhead = 1.0;
    final double maxAllowedOverhead = preferences.getMaxCompilationOverhead();

    for (final StrategyEvaluation evaluation : evaluations) {
      final OptimizationStrategy strategy = evaluation.getStrategy();
      final double strategyOverhead = strategy.estimateCompilationOverhead(profile.getModuleSize());
      final double newTotalOverhead = totalOverhead * strategyOverhead;

      // Check if adding this strategy exceeds overhead budget
      if (newTotalOverhead <= maxAllowedOverhead) {
        selected.add(strategy);
        totalOverhead = newTotalOverhead;

        // Record strategy selection
        performanceTrackers.get(strategy.getName()).recordSelection(evaluation.getBenefitScore());
      } else {
        LOGGER.fine(String.format("Skipping strategy %s due to overhead limit (%.2f > %.2f)",
                                  strategy.getName(), newTotalOverhead, maxAllowedOverhead));
        break;
      }
    }

    return selected;
  }

  /**
   * Evaluates a strategy's effectiveness for the given profile.
   */
  private StrategyEvaluation evaluateStrategy(final OptimizationStrategy strategy,
                                              final ExecutionProfile profile) {
    final double benefit = strategy.estimatePerformanceImprovement(profile);
    final double overhead = strategy.estimateCompilationOverhead(profile.getModuleSize());
    final double historicalEffectiveness = getHistoricalEffectiveness(strategy.getName());

    // Calculate effectiveness score combining benefit, overhead, and history
    final double effectivenessScore = calculateEffectivenessScore(benefit, overhead, historicalEffectiveness);

    return new StrategyEvaluation(strategy, benefit, overhead, effectivenessScore);
  }

  /**
   * Calculates effectiveness score for strategy selection.
   */
  private double calculateEffectivenessScore(final double benefit, final double overhead,
                                             final double historicalEffectiveness) {
    // Base score from benefit-to-overhead ratio
    double score = benefit / Math.max(overhead, 1.0);

    // Apply historical effectiveness
    score *= (0.7 + 0.3 * historicalEffectiveness); // 70% current, 30% historical

    // Apply preferences
    if (preferences.favorPerformance()) {
      score *= 1.2; // 20% bonus for performance preference
    } else if (preferences.favorCompileSpeed()) {
      score /= overhead; // Penalize high overhead more heavily
    }

    return score;
  }

  /**
   * Gets historical effectiveness of a strategy.
   */
  private double getHistoricalEffectiveness(final String strategyName) {
    final StrategyPerformanceTracker tracker = performanceTrackers.get(strategyName);
    return tracker != null ? tracker.getEffectivenessScore() : 0.5; // Default to neutral
  }

  /**
   * Resolves strategy dependencies and conflicts.
   */
  private List<OptimizationStrategy> resolveDependenciesAndConflicts(final List<OptimizationStrategy> strategies) {
    final Set<String> selectedNames = strategies.stream()
        .map(OptimizationStrategy::getName)
        .collect(Collectors.toSet());

    final List<OptimizationStrategy> resolved = new ArrayList<>();
    final Set<String> added = new HashSet<>();

    // Add dependencies first
    for (final OptimizationStrategy strategy : strategies) {
      addStrategyWithDependencies(strategy, resolved, added, selectedNames);
    }

    // Remove conflicts
    return removeConflicts(resolved);
  }

  /**
   * Adds strategy and its dependencies recursively.
   */
  private void addStrategyWithDependencies(final OptimizationStrategy strategy,
                                           final List<OptimizationStrategy> result,
                                           final Set<String> added,
                                           final Set<String> originalSelection) {
    if (added.contains(strategy.getName())) {
      return; // Already added
    }

    // Add dependencies first
    for (final String dependencyName : strategy.getDependencies()) {
      final OptimizationStrategy dependency = strategies.get(dependencyName);
      if (dependency != null && (originalSelection.contains(dependencyName) || preferences.includeImplicitDependencies())) {
        addStrategyWithDependencies(dependency, result, added, originalSelection);
      }
    }

    // Add the strategy itself
    result.add(strategy);
    added.add(strategy.getName());
  }

  /**
   * Removes conflicting strategies, keeping higher priority ones.
   */
  private List<OptimizationStrategy> removeConflicts(final List<OptimizationStrategy> strategies) {
    final List<OptimizationStrategy> result = new ArrayList<>();
    final Set<String> conflicts = new HashSet<>();

    for (final OptimizationStrategy strategy : strategies) {
      if (!conflicts.contains(strategy.getName())) {
        result.add(strategy);
        conflicts.addAll(strategy.getConflicts());
      } else {
        LOGGER.fine("Removed conflicting strategy: " + strategy.getName());
      }
    }

    return result;
  }

  /**
   * Records performance feedback for strategy effectiveness tracking.
   *
   * @param strategyName strategy name
   * @param expectedBenefit expected performance benefit
   * @param actualBenefit actual performance benefit measured
   * @param compilationTime actual compilation time
   */
  public void recordPerformanceFeedback(final String strategyName,
                                        final double expectedBenefit,
                                        final double actualBenefit,
                                        final long compilationTime) {
    final StrategyPerformanceTracker tracker = performanceTrackers.get(strategyName);
    if (tracker != null) {
      tracker.recordPerformance(expectedBenefit, actualBenefit, compilationTime);
      LOGGER.fine(String.format("Recorded feedback for %s: expected=%.2f, actual=%.2f",
                                strategyName, expectedBenefit, actualBenefit));
    }
  }

  /**
   * Gets performance statistics for all strategies.
   *
   * @return map of strategy performance statistics
   */
  public Map<String, StrategyPerformanceStats> getPerformanceStatistics() {
    return performanceTrackers.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().getStatistics()
        ));
  }

  /**
   * Gets available optimization strategies.
   *
   * @return collection of available strategies
   */
  public Collection<OptimizationStrategy> getAvailableStrategies() {
    return Collections.unmodifiableCollection(strategies.values());
  }

  /**
   * Gets optimization strategy by name.
   *
   * @param name strategy name
   * @return strategy or null if not found
   */
  public OptimizationStrategy getStrategy(final String name) {
    return strategies.get(name);
  }

  /**
   * Resets performance tracking data.
   */
  public void resetPerformanceTracking() {
    performanceTrackers.values().forEach(StrategyPerformanceTracker::reset);
    LOGGER.info("Reset optimization strategy performance tracking");
  }

  /**
   * Strategy evaluation result.
   */
  private static final class StrategyEvaluation {
    private final OptimizationStrategy strategy;
    private final double benefitScore;
    private final double overheadScore;
    private final double effectivenessScore;

    StrategyEvaluation(final OptimizationStrategy strategy,
                       final double benefitScore,
                       final double overheadScore,
                       final double effectivenessScore) {
      this.strategy = strategy;
      this.benefitScore = benefitScore;
      this.overheadScore = overheadScore;
      this.effectivenessScore = effectivenessScore;
    }

    OptimizationStrategy getStrategy() { return strategy; }
    double getBenefitScore() { return benefitScore; }
    double getOverheadScore() { return overheadScore; }
    double getEffectivenessScore() { return effectivenessScore; }
  }
}