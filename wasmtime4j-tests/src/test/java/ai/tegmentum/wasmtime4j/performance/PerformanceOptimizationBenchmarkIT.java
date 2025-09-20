package ai.tegmentum.wasmtime4j.performance;

import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive performance optimization benchmark testing the 30-minute execution goal.
 *
 * <p>This test validates the performance optimizations implemented in:
 * <ul>
 *   <li>{@link IntelligentTestScheduler} - Intelligent test scheduling and parallel execution</li>
 *   <li>{@link TestResultCache} - Test result caching and incremental execution</li>
 *   <li>{@link OptimizedTestExecutor} - Integrated optimized test execution</li>
 * </ul>
 *
 * <p>The test is designed to run the full Wasmtime test suite and validate that
 * it completes within the 30-minute target while maintaining high accuracy and
 * comprehensive coverage.
 */
@EnabledIfSystemProperty(named = "wasmtime4j.performance.benchmark", matches = "true")
class PerformanceOptimizationBenchmarkIT {

    private static final Logger LOGGER = Logger.getLogger(PerformanceOptimizationBenchmarkIT.class.getName());

    // Test configuration
    private static final Duration TARGET_EXECUTION_TIME = Duration.ofMinutes(30);
    private static final Duration WARNING_THRESHOLD = Duration.ofMinutes(25);
    private static final double MINIMUM_SUCCESS_RATE = 95.0;
    private static final double MINIMUM_CACHE_EFFICIENCY = 80.0;

    private OptimizedTestExecutor executor;
    private TestResultCache cache;

    @BeforeEach
    void setUp(final TestInfo testInfo) {
        LOGGER.info("Setting up performance benchmark: " + testInfo.getDisplayName());

        // Create optimized cache configuration for performance testing
        final TestResultCache.CacheConfiguration cacheConfig =
                TestResultCache.CacheConfiguration.builder()
                        .enabled(true)
                        .persistToDisk(true)
                        .cacheTtl(Duration.ofHours(6)) // 6-hour TTL for benchmark tests
                        .maxEntries(50_000)
                        .maxSizeMB(1_000) // 1GB cache for performance testing
                        .build();

        // Create intelligent scheduler configuration for optimal performance
        final IntelligentTestScheduler.SchedulerConfiguration schedulerConfig =
                IntelligentTestScheduler.SchedulerConfiguration.builder()
                        .maxParallelism(Runtime.getRuntime().availableProcessors())
                        .enableAdaptiveScheduling(true)
                        .enableMemoryManagement(true)
                        .enableLoadBalancing(true)
                        .priorityStrategy(IntelligentTestScheduler.TestPriorityStrategy.BALANCED)
                        .maxTestTimeout(Duration.ofMinutes(5))
                        .build();

        // Create optimized executor configuration
        final OptimizedTestExecutor.ExecutorConfiguration executorConfig =
                OptimizedTestExecutor.ExecutorConfiguration.builder()
                        .enableCaching(true)
                        .enableMemoryManagement(true)
                        .enablePerformanceMonitoring(true)
                        .enableIncrementalExecution(true)
                        .targetExecutionTime(TARGET_EXECUTION_TIME)
                        .maxParallelism(Runtime.getRuntime().availableProcessors())
                        .cacheConfig(cacheConfig)
                        .schedulerConfig(schedulerConfig)
                        .build();

        this.cache = new TestResultCache(cacheConfig);
        this.executor = new OptimizedTestExecutor(executorConfig);

        LOGGER.info(String.format("Initialized performance benchmark with %d processors, " +
                "target time: %s, cache enabled: %s",
                Runtime.getRuntime().availableProcessors(),
                TARGET_EXECUTION_TIME,
                cacheConfig.isEnabled()));
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
        LOGGER.info("Performance benchmark teardown completed");
    }

    @Test
    @DisplayName("Validate 30-minute execution target for full test suite")
    void testFullTestSuitePerformanceTarget() throws IOException {
        LOGGER.info("Starting full test suite performance benchmark");

        // Execute all test suites with optimization
        final OptimizedTestExecutor.ExecutionResults results = executor.executeAllTestSuites();

        // Log comprehensive results
        LOGGER.info("Performance benchmark completed");
        LOGGER.info(results.toString());
        LOGGER.info(results.getPerformanceSummary());

        // Validate primary performance goals
        assertTargetTimeMet(results);
        assertHighSuccessRate(results);
        assertEffectiveCaching(results);
        assertResourceEfficiency(results);

        // Log final performance summary
        logPerformanceSummary(results);
    }

    @Test
    @DisplayName("Benchmark custom test suite for focused performance analysis")
    void testCustomTestSuitePerformance() throws IOException {
        LOGGER.info("Starting custom test suite performance benchmark");

        // Execute only custom tests for focused analysis
        final List<WasmTestSuiteLoader.TestSuiteType> customSuites = Arrays.asList(
                WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS
        );

        final OptimizedTestExecutor.ExecutionResults results = executor.executeTestSuites(customSuites);

        LOGGER.info("Custom test suite benchmark completed");
        LOGGER.info(results.toString());

        // Validate custom test performance
        assertTrue(results.getTotalExecutionTime().toMinutes() <= 10,
                "Custom test suite should complete within 10 minutes");
        assertTrue(results.getSuccessRate() >= MINIMUM_SUCCESS_RATE,
                "Success rate should be at least " + MINIMUM_SUCCESS_RATE + "%");

        logPerformanceSummary(results);
    }

    @Test
    @DisplayName("Test cache effectiveness with incremental execution")
    void testIncrementalExecutionPerformance() throws IOException {
        LOGGER.info("Testing incremental execution performance");

        // First run to populate cache
        LOGGER.info("First run: populating cache");
        final OptimizedTestExecutor.ExecutionResults firstRun = executor.executeTestSuites(Arrays.asList(
                WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS
        ));

        // Second run to test cache effectiveness
        LOGGER.info("Second run: testing cache effectiveness");
        final OptimizedTestExecutor.ExecutionResults secondRun = executor.executeTestSuites(Arrays.asList(
                WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS
        ));

        // Validate cache effectiveness
        assertTrue(secondRun.getCacheHits() > 0,
                "Second run should have cache hits");
        assertTrue(secondRun.getTotalExecutionTime().compareTo(firstRun.getTotalExecutionTime()) < 0,
                "Second run should be faster due to caching");

        final double speedup = (double) firstRun.getTotalExecutionTime().toMillis() /
                secondRun.getTotalExecutionTime().toMillis();
        assertTrue(speedup >= 2.0,
                "Cache should provide at least 2x speedup, actual: " + speedup);

        LOGGER.info(String.format("Incremental execution speedup: %.2fx", speedup));
        logPerformanceSummary(secondRun);
    }

    @Test
    @DisplayName("Benchmark memory management during large test execution")
    void testMemoryManagementPerformance() throws IOException {
        LOGGER.info("Testing memory management performance");

        // Monitor memory usage during execution
        final long initialMemory = getCurrentMemoryUsage();
        LOGGER.info(String.format("Initial memory usage: %d MB", initialMemory));

        final OptimizedTestExecutor.ExecutionResults results = executor.executeAllTestSuites();

        final long finalMemory = getCurrentMemoryUsage();
        LOGGER.info(String.format("Final memory usage: %d MB", finalMemory));

        // Validate memory management
        final long memoryIncrease = finalMemory - initialMemory;
        final long maxExpectedIncrease = 2048; // 2GB max increase

        assertTrue(memoryIncrease <= maxExpectedIncrease,
                String.format("Memory increase (%d MB) should not exceed %d MB",
                        memoryIncrease, maxExpectedIncrease));

        // Validate no significant memory leaks
        final double memoryEfficiency = 1.0 - ((double) memoryIncrease / maxExpectedIncrease);
        assertTrue(memoryEfficiency >= 0.7,
                "Memory efficiency should be at least 70%, actual: " + memoryEfficiency);

        LOGGER.info(String.format("Memory management efficiency: %.1f%%", memoryEfficiency * 100));
        logPerformanceSummary(results);
    }

    @Test
    @DisplayName("Validate scheduler optimization effectiveness")
    void testSchedulerOptimizationEffectiveness() throws IOException {
        LOGGER.info("Testing scheduler optimization effectiveness");

        // Test different scheduling strategies
        final IntelligentTestScheduler.TestPriorityStrategy[] strategies = {
                IntelligentTestScheduler.TestPriorityStrategy.FASTEST_FIRST,
                IntelligentTestScheduler.TestPriorityStrategy.BALANCED,
                IntelligentTestScheduler.TestPriorityStrategy.COMPLEXITY_BASED
        };

        OptimizedTestExecutor.ExecutionResults bestResults = null;
        IntelligentTestScheduler.TestPriorityStrategy bestStrategy = null;

        for (final IntelligentTestScheduler.TestPriorityStrategy strategy : strategies) {
            LOGGER.info("Testing scheduling strategy: " + strategy);

            // Create executor with specific strategy
            final IntelligentTestScheduler.SchedulerConfiguration schedulerConfig =
                    IntelligentTestScheduler.SchedulerConfiguration.builder()
                            .priorityStrategy(strategy)
                            .build();

            final OptimizedTestExecutor.ExecutorConfiguration executorConfig =
                    OptimizedTestExecutor.ExecutorConfiguration.builder()
                            .schedulerConfig(schedulerConfig)
                            .build();

            try (final var strategyExecutor = new OptimizedTestExecutor(executorConfig)) {
                final OptimizedTestExecutor.ExecutionResults results =
                        strategyExecutor.executeTestSuites(Arrays.asList(
                                WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS
                        ));

                if (bestResults == null ||
                    results.getTotalExecutionTime().compareTo(bestResults.getTotalExecutionTime()) < 0) {
                    bestResults = results;
                    bestStrategy = strategy;
                }

                LOGGER.info(String.format("Strategy %s: %s execution time, %.1f%% efficiency",
                        strategy,
                        results.getTotalExecutionTime(),
                        results.getExecutionEfficiency()));
            }
        }

        assertNotNull(bestResults, "Should have execution results");
        assertNotNull(bestStrategy, "Should identify best strategy");

        LOGGER.info(String.format("Best scheduling strategy: %s with %.1f%% efficiency",
                bestStrategy, bestResults.getExecutionEfficiency()));

        assertTrue(bestResults.getExecutionEfficiency() >= 80.0,
                "Best strategy should achieve at least 80% efficiency");

        logPerformanceSummary(bestResults);
    }

    @Test
    @DisplayName("Stress test parallel execution with maximum concurrency")
    @Disabled("Enable for stress testing only")
    void testMaximumConcurrencyStress() throws IOException {
        LOGGER.info("Starting maximum concurrency stress test");

        // Configure for maximum parallelism
        final IntelligentTestScheduler.SchedulerConfiguration schedulerConfig =
                IntelligentTestScheduler.SchedulerConfiguration.builder()
                        .maxParallelism(Runtime.getRuntime().availableProcessors() * 2)
                        .enableLoadBalancing(true)
                        .build();

        final OptimizedTestExecutor.ExecutorConfiguration executorConfig =
                OptimizedTestExecutor.ExecutorConfiguration.builder()
                        .maxParallelism(Runtime.getRuntime().availableProcessors() * 2)
                        .schedulerConfig(schedulerConfig)
                        .build();

        try (final var stressExecutor = new OptimizedTestExecutor(executorConfig)) {
            final OptimizedTestExecutor.ExecutionResults results = stressExecutor.executeAllTestSuites();

            // Validate stress test results
            assertTrue(results.getSuccessRate() >= MINIMUM_SUCCESS_RATE,
                    "Stress test should maintain high success rate");
            assertTrue(results.isTargetTimeMet(),
                    "Stress test should still meet target time");

            LOGGER.info("Maximum concurrency stress test completed successfully");
            logPerformanceSummary(results);
        }
    }

    // Helper methods for assertions and validation

    private void assertTargetTimeMet(final OptimizedTestExecutor.ExecutionResults results) {
        final Duration executionTime = results.getTotalExecutionTime();

        assertTrue(results.isTargetTimeMet(),
                String.format("Execution time (%d minutes %d seconds) should not exceed target (%d minutes)",
                        executionTime.toMinutes(), executionTime.toSecondsPart(),
                        TARGET_EXECUTION_TIME.toMinutes()));

        if (executionTime.compareTo(WARNING_THRESHOLD) > 0) {
            LOGGER.warning(String.format("Execution time (%s) is approaching target limit (%s)",
                    executionTime, TARGET_EXECUTION_TIME));
        }
    }

    private void assertHighSuccessRate(final OptimizedTestExecutor.ExecutionResults results) {
        assertTrue(results.getSuccessRate() >= MINIMUM_SUCCESS_RATE,
                String.format("Success rate (%.1f%%) should be at least %.1f%%",
                        results.getSuccessRate(), MINIMUM_SUCCESS_RATE));
    }

    private void assertEffectiveCaching(final OptimizedTestExecutor.ExecutionResults results) {
        final TestResultCache.CacheStatistics cacheStats = results.getCacheStats();
        final double cacheEfficiency = cache.calculateCacheEfficiency();

        assertTrue(cacheEfficiency >= MINIMUM_CACHE_EFFICIENCY,
                String.format("Cache efficiency (%.1f%%) should be at least %.1f%%",
                        cacheEfficiency, MINIMUM_CACHE_EFFICIENCY));

        if (results.getCacheHits() > 0) {
            assertTrue(cacheStats.getHitRate() > 0.0,
                    "Cache hit rate should be positive when cache hits are present");
        }
    }

    private void assertResourceEfficiency(final OptimizedTestExecutor.ExecutionResults results) {
        final double executionEfficiency = results.getExecutionEfficiency();

        assertTrue(executionEfficiency >= 75.0,
                String.format("Overall execution efficiency (%.1f%%) should be at least 75%%",
                        executionEfficiency));
    }

    private void logPerformanceSummary(final OptimizedTestExecutor.ExecutionResults results) {
        LOGGER.info("=== PERFORMANCE BENCHMARK SUMMARY ===");
        LOGGER.info(String.format("Target Time Met: %s", results.isTargetTimeMet() ? "✓" : "✗"));
        LOGGER.info(String.format("Execution Time: %d:%02d (target: %d:%02d)",
                results.getTotalExecutionTime().toMinutes(),
                results.getTotalExecutionTime().toSecondsPart(),
                TARGET_EXECUTION_TIME.toMinutes(),
                TARGET_EXECUTION_TIME.toSecondsPart()));
        LOGGER.info(String.format("Success Rate: %.1f%% (target: %.1f%%)",
                results.getSuccessRate(), MINIMUM_SUCCESS_RATE));
        LOGGER.info(String.format("Cache Efficiency: %.1f%% (target: %.1f%%)",
                cache.calculateCacheEfficiency(), MINIMUM_CACHE_EFFICIENCY));
        LOGGER.info(String.format("Execution Efficiency: %.1f%%", results.getExecutionEfficiency()));
        LOGGER.info(String.format("Tests Executed: %d", results.getTestsExecuted()));
        LOGGER.info(String.format("Tests Cached: %d", results.getTestsSkipped()));
        LOGGER.info(String.format("Cache Hits: %d", results.getCacheHits()));
        LOGGER.info("=====================================");
    }

    private long getCurrentMemoryUsage() {
        final Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }
}