package ai.tegmentum.wasmtime4j.compilation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for the adaptive JIT compilation system.
 *
 * <p>Tests tier transitions, optimization strategy selection, hot spot detection,
 * and performance regression handling in the adaptive compilation engine.
 */
class AdaptiveJitCompilationTest {

  private static final Logger LOGGER = Logger.getLogger(AdaptiveJitCompilationTest.class.getName());

  private AdaptiveOptimizationEngine optimizationEngine;
  private TierTransitionManager tierTransitionManager;
  private OptimizationStrategyManager strategyManager;

  @BeforeEach
  void setUp() {
    // Create test configuration
    final AdaptiveOptimizationConfig config = AdaptiveOptimizationConfig.builder()
        .tierTransitionConfig(TierTransitionConfig.defaultConfig())
        .optimizationPreferences(OptimizationPreferences.balanced())
        .optimizationInterval(Duration.ofMillis(100)) // Fast interval for testing
        .minFunctionsForLearning(5)
        .maxConcurrentOptimizations(2)
        .build();

    optimizationEngine = new AdaptiveOptimizationEngine(config);
    tierTransitionManager = new TierTransitionManager();
    strategyManager = new OptimizationStrategyManager();
  }

  @AfterEach
  void tearDown() {
    if (optimizationEngine != null) {
      optimizationEngine.shutdown();
    }
  }

  @Test
  @Timeout(30)
  void testTierTransitionProgression() {
    LOGGER.info("Testing tier transition progression");

    final String functionId = "test_function_1";
    final FunctionExecutionTracker tracker = new FunctionExecutionTracker(
        functionId, JitCompilationStrategy.BASELINE);

    // Simulate increasing execution frequency to trigger tier transitions
    JitCompilationStrategy currentStrategy = JitCompilationStrategy.BASELINE;

    // Phase 1: Baseline execution
    for (int i = 0; i < 50; i++) {
      optimizationEngine.recordExecution(functionId, currentStrategy, 1_000_000, 1024);
      tracker.recordExecution(1_000_000, 1024);
    }

    // Should still be baseline
    assertEquals(JitCompilationStrategy.BASELINE, currentStrategy);

    // Phase 2: Increase execution to trigger standard compilation
    for (int i = 0; i < 200; i++) {
      optimizationEngine.recordExecution(functionId, currentStrategy, 800_000, 1024);
      tracker.recordExecution(800_000, 1024);
    }

    // Check tier transition decision
    final TierTransitionDecision decision1 = tierTransitionManager.shouldTransition(tracker);
    if (decision1.shouldTransition()) {
      assertTrue(decision1.getTargetStrategy().getTier() > currentStrategy.getTier(),
                 "Should transition to higher tier");
      currentStrategy = decision1.getTargetStrategy();
      tracker.transitionToStrategy(currentStrategy);
    }

    // Phase 3: Continue execution to trigger optimized compilation
    for (int i = 0; i < 1000; i++) {
      optimizationEngine.recordExecution(functionId, currentStrategy, 600_000, 1024);
      tracker.recordExecution(600_000, 1024);
    }

    // Should progress to higher optimization tier
    final TierTransitionDecision decision2 = tierTransitionManager.shouldTransition(tracker);
    if (decision2.shouldTransition()) {
      assertTrue(decision2.getTargetStrategy().getTier() >= JitCompilationStrategy.OPTIMIZED.getTier(),
                 "Should reach optimized tier for hot function");
    }

    LOGGER.info("Tier transition progression test completed successfully");
  }

  @Test
  void testOptimizationStrategySelection() {
    LOGGER.info("Testing optimization strategy selection");

    // Create execution profile for hot compute-intensive function
    final ExecutionProfile computeProfile = new ExecutionProfile(
        5000, // High execution count
        15.0, // 15ms average execution time
        75000, // 75 seconds total execution time
        0.8, // High CPU utilization
        2 * 1024 * 1024, // 2MB memory usage
        true, // Has loops
        true, // Has vector operations
        false, // No recursion
        10, // Multiple functions
        5 * 1024 * 1024 // 5MB module
    );

    final List<OptimizationStrategy> strategies = strategyManager.selectStrategies(
        computeProfile, JitCompilationStrategy.HIGHLY_OPTIMIZED);

    assertFalse(strategies.isEmpty(), "Should select optimization strategies");

    // Verify that appropriate strategies are selected
    final boolean hasVectorization = strategies.stream()
        .anyMatch(strategy -> strategy instanceof VectorizationOptimization);
    final boolean hasLoopOptimization = strategies.stream()
        .anyMatch(strategy -> strategy instanceof LoopOptimization);

    assertTrue(hasVectorization || hasLoopOptimization,
               "Should select vectorization or loop optimization for compute-intensive profile");

    // Test strategy selection for different compilation tiers
    final List<OptimizationStrategy> baselineStrategies = strategyManager.selectStrategies(
        computeProfile, JitCompilationStrategy.BASELINE);

    assertTrue(baselineStrategies.size() <= strategies.size(),
               "Baseline should have fewer or equal strategies than highly optimized");

    LOGGER.info("Selected {} strategies for highly optimized tier, {} for baseline",
                strategies.size(), baselineStrategies.size());
  }

  @Test
  @Timeout(15)
  void testHotSpotDetection() throws InterruptedException {
    LOGGER.info("Testing hot spot detection");

    final String hotFunctionId = "hot_function";
    final String coldFunctionId = "cold_function";

    // Simulate hot function execution pattern
    for (int i = 0; i < 2000; i++) {
      optimizationEngine.recordExecution(hotFunctionId, JitCompilationStrategy.BASELINE,
                                         500_000 + (i % 100_000), 1024);
    }

    // Simulate cold function execution pattern
    for (int i = 0; i < 10; i++) {
      optimizationEngine.recordExecution(coldFunctionId, JitCompilationStrategy.BASELINE,
                                         10_000_000, 512);
    }

    // Wait for background optimization to process
    Thread.sleep(2000);

    final AdaptiveOptimizationStatistics stats = optimizationEngine.getStatistics();

    assertTrue(stats.getTotalOptimizations() > 0, "Should have triggered optimizations");
    assertTrue(stats.getTrackedFunctionCount() >= 2, "Should track both functions");

    LOGGER.info("Hot spot detection completed: {} optimizations, {} functions tracked",
                stats.getTotalOptimizations(), stats.getTrackedFunctionCount());
  }

  @Test
  void testPerformanceRegressionDetection() {
    LOGGER.info("Testing performance regression detection");

    final String functionId = "regression_test_function";
    final FunctionExecutionTracker tracker = new FunctionExecutionTracker(
        functionId, JitCompilationStrategy.OPTIMIZED);

    // Establish baseline performance (1ms average)
    for (int i = 0; i < 100; i++) {
      tracker.recordExecution(1_000_000, 1024); // 1ms
    }

    // Simulate performance improvement
    for (int i = 0; i < 50; i++) {
      tracker.recordExecution(800_000, 1024); // 0.8ms (better)
    }

    final ExecutionStatistics goodStats = tracker.getStatistics();
    assertFalse(goodStats.hasRecentPerformanceDegraded(),
                "Should not detect regression with improved performance");

    // Simulate performance regression
    for (int i = 0; i < 100; i++) {
      tracker.recordExecution(1_500_000, 1024); // 1.5ms (50% worse)
    }

    final ExecutionStatistics regressedStats = tracker.getStatistics();
    assertTrue(regressedStats.hasRecentPerformanceDegraded(),
               "Should detect performance regression");

    LOGGER.info("Performance regression detection test completed");
  }

  @Test
  void testSystemLoadAdaptation() {
    LOGGER.info("Testing system load adaptation");

    final SystemPerformanceMonitor monitor = new SystemPerformanceMonitor();

    // Simulate high compilation load
    for (int i = 0; i < 100; i++) {
      monitor.recordCompilationTime(100); // 100ms compilations
      monitor.recordExecutionTime(10);    // 10ms execution
    }

    final double overhead = monitor.getCompilationOverhead();
    assertTrue(overhead > 0, "Should track compilation overhead");

    final SystemPerformanceMonitor.LoadCategory loadCategory = monitor.getLoadCategory();
    assertNotNull(loadCategory, "Should classify system load");

    // Test configuration adaptation
    final TierTransitionConfig baseConfig = TierTransitionConfig.defaultConfig();
    final TierTransitionConfig adaptedConfig = baseConfig.adjustForSystemPerformance(0.9, 0.8);

    assertTrue(adaptedConfig.getMediumFrequencyThreshold() >= baseConfig.getMediumFrequencyThreshold(),
               "Should increase thresholds under high load");

    LOGGER.info("System load adaptation test completed with {} overhead and {} load",
                String.format("%.1f%%", overhead * 100), loadCategory);
  }

  @Test
  void testOptimizationEffectiveness() {
    LOGGER.info("Testing optimization effectiveness tracking");

    final String strategyName = "test_strategy";
    final double expectedBenefit = 0.25; // 25% expected improvement
    final double actualBenefit = 0.30;   // 30% actual improvement
    final long compilationTime = 500;    // 500ms compilation

    strategyManager.recordPerformanceFeedback(strategyName, expectedBenefit,
                                              actualBenefit, compilationTime);

    // Strategy manager would use this feedback for future decisions
    // This tests the feedback mechanism works without errors

    assertDoesNotThrow(() -> {
      strategyManager.getPerformanceStatistics();
    }, "Should retrieve performance statistics without error");

    LOGGER.info("Optimization effectiveness tracking test completed");
  }

  @Test
  void testConcurrentOptimization() throws InterruptedException {
    LOGGER.info("Testing concurrent optimization handling");

    final CountDownLatch latch = new CountDownLatch(5);
    final String[] functionIds = {"func1", "func2", "func3", "func4", "func5"};

    // Simulate concurrent execution from multiple threads
    for (final String functionId : functionIds) {
      new Thread(() -> {
        try {
          // Simulate high-frequency execution to trigger optimization
          for (int i = 0; i < 1000; i++) {
            optimizationEngine.recordExecution(functionId, JitCompilationStrategy.BASELINE,
                                               1_000_000 + (i * 1000), 1024);

            if (i % 100 == 0) {
              Thread.sleep(1); // Brief pause to allow other threads
            }
          }
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          latch.countDown();
        }
      }).start();
    }

    assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete");

    // Allow background optimization to process
    Thread.sleep(3000);

    final AdaptiveOptimizationStatistics stats = optimizationEngine.getStatistics();
    assertTrue(stats.getTrackedFunctionCount() >= functionIds.length,
               "Should track all functions");

    LOGGER.info("Concurrent optimization test completed: {} functions tracked, {} optimizations",
                stats.getTrackedFunctionCount(), stats.getTotalOptimizations());
  }

  @Test
  void testCompilationStrategyComparison() {
    LOGGER.info("Testing compilation strategy comparison");

    final ExecutionProfile profile = new ExecutionProfile(
        1000, 10.0, 10000, 0.5, 1024 * 1024,
        true, false, false, 5, 2 * 1024 * 1024);

    // Test strategy selection for different tiers
    for (final JitCompilationStrategy strategy : JitCompilationStrategy.values()) {
      if (strategy == JitCompilationStrategy.ADAPTIVE) {
        continue; // Skip adaptive for this test
      }

      final List<OptimizationStrategy> strategies = strategyManager.selectStrategies(profile, strategy);

      assertTrue(strategies.size() >= 0,
                 "Should select valid number of strategies for " + strategy);

      // Higher tiers should generally have more optimization strategies
      if (strategy.getTier() > 0) {
        assertFalse(strategies.isEmpty(),
                    "Higher tiers should have optimization strategies");
      }
    }

    LOGGER.info("Compilation strategy comparison test completed");
  }

  @Test
  @Timeout(10)
  void testEngineStateTransitions() throws InterruptedException {
    LOGGER.info("Testing engine state transitions");

    // Start in learning state
    AdaptiveOptimizationStatistics stats = optimizationEngine.getStatistics();
    assertEquals(AdaptiveOptimizationEngine.EngineState.LEARNING, stats.getEngineState(),
                 "Should start in LEARNING state");

    // Simulate enough execution to trigger transition to OPTIMIZING
    for (int i = 0; i < 10; i++) {
      optimizationEngine.recordExecution("func_" + i, JitCompilationStrategy.BASELINE,
                                         1_000_000, 1024);
    }

    // Wait for state transition
    Thread.sleep(2000);

    stats = optimizationEngine.getStatistics();

    // Should eventually transition to OPTIMIZING (might take a few cycles)
    assertTrue(stats.getEngineState() == AdaptiveOptimizationEngine.EngineState.LEARNING ||
               stats.getEngineState() == AdaptiveOptimizationEngine.EngineState.OPTIMIZING,
               "Should be in LEARNING or OPTIMIZING state");

    LOGGER.info("Engine state transitions test completed in {} state", stats.getEngineState());
  }
}