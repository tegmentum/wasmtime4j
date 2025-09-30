package ai.tegmentum.wasmtime4j.compilation;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;

/**
 * Adaptive optimization engine with runtime profiling and hot spot detection.
 *
 * <p>This engine continuously monitors WebAssembly function execution patterns,
 * identifies hot spots, and triggers appropriate tier transitions and optimizations.
 * It implements sophisticated heuristics for performance prediction and adaptive
 * compilation scheduling.
 *
 * @since 1.0.0
 */
public final class AdaptiveOptimizationEngine {

  private static final Logger LOGGER = Logger.getLogger(AdaptiveOptimizationEngine.class.getName());

  /** Hot spot detection threshold (executions per second). */
  private static final double HOT_SPOT_THRESHOLD = 10.0;

  /** Performance regression detection threshold (15% slowdown). */
  private static final double REGRESSION_THRESHOLD = 0.15;

  /** Profiling window size for hot spot detection. */
  private static final Duration PROFILING_WINDOW = Duration.ofMinutes(2);

  /** Core components. */
  private final TierTransitionManager tierTransitionManager;
  private final OptimizationStrategyManager strategyManager;
  private final SystemPerformanceMonitor systemMonitor;
  private final HotSpotDetector hotSpotDetector;
  private final PerformancePredictor performancePredictor;

  /** Execution tracking and profiling. */
  private final ConcurrentHashMap<String, FunctionExecutionTracker> executionTrackers;
  private final ConcurrentHashMap<String, HotSpotProfile> hotSpotProfiles;

  /** Background optimization execution. */
  private final ScheduledExecutorService optimizationExecutor;
  private final CompletableFuture<Void> optimizationTask;

  /** Engine state and statistics. */
  private final AtomicReference<EngineState> engineState;
  private final AtomicLong totalOptimizations;
  private final AtomicLong successfulOptimizations;
  private final AtomicLong regressionDetections;

  /** Configuration. */
  private final AdaptiveOptimizationConfig config;

  /**
   * Engine operational states.
   */
  public enum EngineState {
    /** Engine is starting up and learning execution patterns. */
    LEARNING,
    /** Engine is actively optimizing based on learned patterns. */
    OPTIMIZING,
    /** Engine is paused due to high system load or other constraints. */
    PAUSED,
    /** Engine has been shut down. */
    SHUTDOWN
  }

  /**
   * Creates adaptive optimization engine with default configuration.
   */
  public AdaptiveOptimizationEngine() {
    this(AdaptiveOptimizationConfig.defaultConfig());
  }

  /**
   * Creates adaptive optimization engine with custom configuration.
   *
   * @param config adaptive optimization configuration
   */
  public AdaptiveOptimizationEngine(final AdaptiveOptimizationConfig config) {
    this.config = config;
    this.tierTransitionManager = new TierTransitionManager(config.getTierTransitionConfig());
    this.strategyManager = new OptimizationStrategyManager(config.getOptimizationPreferences());
    this.systemMonitor = new SystemPerformanceMonitor();
    this.hotSpotDetector = new HotSpotDetector(config.getHotSpotConfig());
    this.performancePredictor = new PerformancePredictor();

    this.executionTrackers = new ConcurrentHashMap<>();
    this.hotSpotProfiles = new ConcurrentHashMap<>();

    this.optimizationExecutor = Executors.newScheduledThreadPool(
        Math.max(1, Runtime.getRuntime().availableProcessors() / 4),
        r -> {
          final Thread t = new Thread(r, "AdaptiveOptimization");
          t.setDaemon(true);
          return t;
        });

    this.engineState = new AtomicReference<>(EngineState.LEARNING);
    this.totalOptimizations = new AtomicLong(0);
    this.successfulOptimizations = new AtomicLong(0);
    this.regressionDetections = new AtomicLong(0);

    // Start background optimization task
    this.optimizationTask = startBackgroundOptimization();

    LOGGER.info("Adaptive optimization engine started in " + engineState.get() + " state");
  }

  /**
   * Records function execution for adaptive optimization analysis.
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
    // Update execution tracker
    final FunctionExecutionTracker tracker = executionTrackers.computeIfAbsent(
        functionId, k -> new FunctionExecutionTracker(k, currentStrategy));
    tracker.recordExecution(executionTimeNs, memoryUsage);

    // Update tier transition manager
    tierTransitionManager.recordExecution(functionId, currentStrategy, executionTimeNs, memoryUsage);

    // Update hot spot detection
    hotSpotDetector.recordExecution(functionId, executionTimeNs);

    // Record system performance metrics
    systemMonitor.recordExecutionTime(executionTimeNs / 1_000_000); // Convert to milliseconds

    // Check for immediate optimization opportunities
    if (shouldTriggerImmediateOptimization(tracker)) {
      scheduleImmediateOptimization(functionId, tracker);
    }
  }

  /**
   * Determines if immediate optimization should be triggered.
   */
  private boolean shouldTriggerImmediateOptimization(final FunctionExecutionTracker tracker) {
    // Don't trigger immediate optimization during learning phase
    if (engineState.get() == EngineState.LEARNING) {
      return false;
    }

    // Check if function is a hot spot
    if (tracker.isHotPath()) {
      return true;
    }

    // Check for performance regression
    final ExecutionStatistics stats = tracker.getStatistics();
    if (stats.hasRecentPerformanceDegraded()) {
      return true;
    }

    return false;
  }

  /**
   * Schedules immediate optimization for a function.
   */
  private void scheduleImmediateOptimization(final String functionId,
                                             final FunctionExecutionTracker tracker) {
    optimizationExecutor.submit(() -> {
      try {
        optimizeFunction(functionId, tracker, OptimizationTrigger.HOT_SPOT);
      } catch (final Exception e) {
        LOGGER.warning("Failed to optimize function " + functionId + ": " + e.getMessage());
      }
    });
  }

  /**
   * Starts background optimization task.
   */
  private CompletableFuture<Void> startBackgroundOptimization() {
    return CompletableFuture.runAsync(() -> {
      while (!Thread.currentThread().isInterrupted() && engineState.get() != EngineState.SHUTDOWN) {
        try {
          performBackgroundOptimization();
          Thread.sleep(config.getOptimizationInterval().toMillis());
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        } catch (final Exception e) {
          LOGGER.warning("Background optimization error: " + e.getMessage());
        }
      }
    }, optimizationExecutor);
  }

  /**
   * Performs background optimization tasks.
   */
  private void performBackgroundOptimization() {
    // Update engine state based on system conditions
    updateEngineState();

    if (engineState.get() != EngineState.OPTIMIZING) {
      return; // Skip optimization if not in optimizing state
    }

    // Detect hot spots
    final List<HotSpotCandidate> hotSpots = hotSpotDetector.detectHotSpots(executionTrackers.values());

    // Process tier transitions
    processTierTransitions();

    // Optimize hot spots
    optimizeHotSpots(hotSpots);

    // Detect and handle performance regressions
    detectAndHandleRegressions();

    // Cleanup old tracking data
    cleanupOldTrackingData();
  }

  /**
   * Updates engine state based on system conditions.
   */
  private void updateEngineState() {
    final EngineState currentState = engineState.get();
    final SystemPerformanceMonitor.SystemPerformanceMetrics metrics = systemMonitor.getMetrics();

    switch (currentState) {
      case LEARNING:
        // Transition to optimizing after learning period
        if (hasLearningPeriodCompleted()) {
          engineState.set(EngineState.OPTIMIZING);
          LOGGER.info("Adaptive optimization engine transitioned to OPTIMIZING state");
        }
        break;

      case OPTIMIZING:
        // Pause if system is under high load
        if (metrics.getLoadCategory() == SystemPerformanceMonitor.LoadCategory.HIGH) {
          engineState.set(EngineState.PAUSED);
          LOGGER.info("Adaptive optimization engine paused due to high system load");
        }
        break;

      case PAUSED:
        // Resume if system load decreases
        if (metrics.getLoadCategory() != SystemPerformanceMonitor.LoadCategory.HIGH) {
          engineState.set(EngineState.OPTIMIZING);
          LOGGER.info("Adaptive optimization engine resumed optimization");
        }
        break;

      case SHUTDOWN:
        // No state transitions from shutdown
        break;
    }
  }

  /**
   * Determines if learning period has completed.
   */
  private boolean hasLearningPeriodCompleted() {
    return executionTrackers.size() >= config.getMinFunctionsForLearning() ||
           totalOptimizations.get() >= config.getMinExecutionsForLearning();
  }

  /**
   * Processes tier transitions for tracked functions.
   */
  private void processTierTransitions() {
    executionTrackers.values().parallelStream()
        .filter(tracker -> tracker.getStatistics().getExecutionCount() > 10)
        .forEach(tracker -> {
          final TierTransitionDecision decision = tierTransitionManager.shouldTransition(tracker);
          if (decision.shouldTransition()) {
            scheduleOptimization(tracker.getFunctionId(), tracker, OptimizationTrigger.TIER_TRANSITION);
          }
        });
  }

  /**
   * Optimizes detected hot spots.
   */
  private void optimizeHotSpots(final List<HotSpotCandidate> hotSpots) {
    hotSpots.stream()
        .limit(config.getMaxConcurrentOptimizations())
        .forEach(hotSpot -> {
          final FunctionExecutionTracker tracker = executionTrackers.get(hotSpot.getFunctionId());
          if (tracker != null) {
            scheduleOptimization(hotSpot.getFunctionId(), tracker, OptimizationTrigger.HOT_SPOT);
          }
        });
  }

  /**
   * Schedules optimization for a function.
   */
  private void scheduleOptimization(final String functionId,
                                    final FunctionExecutionTracker tracker,
                                    final OptimizationTrigger trigger) {
    optimizationExecutor.submit(() -> {
      try {
        optimizeFunction(functionId, tracker, trigger);
      } catch (final Exception e) {
        LOGGER.warning("Failed to optimize function " + functionId + ": " + e.getMessage());
      }
    });
  }

  /**
   * Optimizes a specific function.
   */
  private void optimizeFunction(final String functionId,
                                final FunctionExecutionTracker tracker,
                                final OptimizationTrigger trigger) {
    totalOptimizations.incrementAndGet();

    final ExecutionStatistics stats = tracker.getStatistics();
    final ExecutionProfile profile = createExecutionProfile(stats);

    // Select optimization strategies
    final List<OptimizationStrategy> strategies = strategyManager.selectStrategies(
        profile, tracker.getCurrentStrategy());

    // Predict performance improvement
    final double predictedImprovement = performancePredictor.predictImprovement(
        profile, strategies, tracker.getCurrentStrategy());

    // Apply optimizations (this would integrate with the native compilation system)
    final boolean success = applyOptimizations(functionId, strategies, profile);

    if (success) {
      successfulOptimizations.incrementAndGet();
      LOGGER.fine(String.format("Successfully optimized %s with %d strategies (predicted %.1f%% improvement)",
                                functionId, strategies.size(), predictedImprovement * 100));
    } else {
      LOGGER.warning("Failed to apply optimizations for function: " + functionId);
    }

    // Update hot spot profile
    updateHotSpotProfile(functionId, tracker, strategies, success);
  }

  /**
   * Creates execution profile from statistics.
   */
  private ExecutionProfile createExecutionProfile(final ExecutionStatistics stats) {
    // This would typically include more sophisticated profiling data
    return new ExecutionProfile(
        stats.getExecutionCount(),
        stats.getAverageExecutionTime().toNanos() / 1_000_000.0, // Convert to milliseconds
        stats.getTotalExecutionTime().toMillis(),
        systemMonitor.getCpuUtilization(),
        stats.getTotalMemoryUsage(),
        true, // Assume loops present (would be determined by bytecode analysis)
        false, // Vector operations (would be determined by bytecode analysis)
        false, // Recursion (would be determined by call graph analysis)
        1, // Function count (would be from module metadata)
        1024 * 1024 // Module size estimate (would be from actual module)
    );
  }

  /**
   * Applies optimizations to a function (integration point with native system).
   */
  private boolean applyOptimizations(final String functionId,
                                     final List<OptimizationStrategy> strategies,
                                     final ExecutionProfile profile) {
    // This would integrate with the native compilation system
    // For now, simulate success based on system conditions
    return !systemMonitor.isHighLoad() && !strategies.isEmpty();
  }

  /**
   * Updates hot spot profile after optimization attempt.
   */
  private void updateHotSpotProfile(final String functionId,
                                    final FunctionExecutionTracker tracker,
                                    final List<OptimizationStrategy> strategies,
                                    final boolean success) {
    final HotSpotProfile profile = hotSpotProfiles.computeIfAbsent(
        functionId, k -> new HotSpotProfile(k));

    profile.recordOptimizationAttempt(strategies, success, tracker.getCurrentStrategy());
  }

  /**
   * Detects and handles performance regressions.
   */
  private void detectAndHandleRegressions() {
    executionTrackers.values().stream()
        .filter(tracker -> tracker.getStatistics().hasRecentPerformanceDegraded())
        .forEach(tracker -> {
          regressionDetections.incrementAndGet();
          tierTransitionManager.deoptimize(tracker.getFunctionId(),
                                           "Performance regression detected");
          LOGGER.warning("Performance regression detected for function: " + tracker.getFunctionId());
        });
  }

  /**
   * Cleans up old tracking data to prevent memory leaks.
   */
  private void cleanupOldTrackingData() {
    final Instant cutoff = Instant.now().minus(config.getTrackingDataRetention());

    executionTrackers.entrySet().removeIf(entry -> {
      final ExecutionStatistics stats = entry.getValue().getStatistics();
      return stats.getLastExecution() != null &&
             stats.getLastExecution().toInstant().isBefore(cutoff);
    });

    hotSpotProfiles.entrySet().removeIf(entry ->
        entry.getValue().getLastUpdate().isBefore(cutoff));
  }

  /**
   * Gets adaptive optimization statistics.
   *
   * @return optimization statistics
   */
  public AdaptiveOptimizationStatistics getStatistics() {
    return new AdaptiveOptimizationStatistics(
        engineState.get(),
        executionTrackers.size(),
        hotSpotProfiles.size(),
        totalOptimizations.get(),
        successfulOptimizations.get(),
        regressionDetections.get(),
        getSuccessRate(),
        systemMonitor.getMetrics()
    );
  }

  /**
   * Gets optimization success rate.
   */
  private double getSuccessRate() {
    final long total = totalOptimizations.get();
    return total > 0 ? (double) successfulOptimizations.get() / total : 0.0;
  }

  /**
   * Shuts down the adaptive optimization engine.
   */
  public void shutdown() {
    engineState.set(EngineState.SHUTDOWN);
    optimizationTask.cancel(true);
    optimizationExecutor.shutdown();

    try {
      if (!optimizationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        optimizationExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      optimizationExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    LOGGER.info("Adaptive optimization engine shut down");
  }

  /**
   * Optimization trigger types.
   */
  public enum OptimizationTrigger {
    HOT_SPOT,
    TIER_TRANSITION,
    REGRESSION_RECOVERY,
    SCHEDULED
  }
}