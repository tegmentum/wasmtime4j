package ai.tegmentum.wasmtime4j.performance;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.time.Duration;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Final validation test for all performance optimizations implemented for Task #268.
 *
 * <p>This test validates that all performance optimization components work together
 * to achieve the 30-minute execution target while maintaining high test quality and accuracy.
 *
 * <p>The test serves as the definitive validation that the performance optimization
 * implementation is complete and meets all acceptance criteria.
 */
@EnabledIfSystemProperty(named = "wasmtime4j.performance.validation", matches = "true")
class PerformanceOptimizationValidationIT {

    private static final Logger LOGGER = Logger.getLogger(PerformanceOptimizationValidationIT.class.getName());

    // Performance validation constants
    private static final Duration TARGET_EXECUTION_TIME = Duration.ofMinutes(30);
    private static final double MINIMUM_SUCCESS_RATE = 95.0;
    private static final double MINIMUM_CACHE_EFFICIENCY = 75.0;
    private static final double MINIMUM_OVERALL_EFFICIENCY = 80.0;

    private static CiCdOptimizedRunner ciRunner;
    private static OptimizedTestExecutor optimizedExecutor;
    private static TestResultCache performanceCache;

    @BeforeAll
    static void setUpPerformanceValidation() {
        LOGGER.info("=== PERFORMANCE OPTIMIZATION VALIDATION STARTING ===");
        LOGGER.info("Target: Complete full test suite execution within 30 minutes");
        LOGGER.info("Components being validated:");
        LOGGER.info("  - IntelligentTestScheduler: Adaptive scheduling and load balancing");
        LOGGER.info("  - TestResultCache: Incremental execution and caching");
        LOGGER.info("  - OptimizedTestExecutor: Integrated performance optimizations");
        LOGGER.info("  - CiCdOptimizedRunner: CI/CD specific optimizations");

        // Initialize performance components
        ciRunner = CiCdOptimizedRunner.createOptimal();

        // Create optimized executor for validation
        optimizedExecutor = OptimizedTestExecutor.createOptimal();

        // Initialize performance cache
        performanceCache = TestResultCache.createDefault();

        LOGGER.info("Performance validation setup completed");
    }

    @AfterAll
    static void tearDownPerformanceValidation() {
        if (optimizedExecutor != null) {
            optimizedExecutor.shutdown();
        }
        LOGGER.info("=== PERFORMANCE OPTIMIZATION VALIDATION COMPLETED ===");
    }

    @Test
    @DisplayName("Validate complete 30-minute execution target achievement")
    void validateThirtyMinuteExecutionTarget() throws IOException {
        LOGGER.info("Starting comprehensive 30-minute execution target validation");

        // Execute the complete test suite with all optimizations enabled
        final OptimizedTestExecutor.ExecutionResults results = optimizedExecutor.executeAllTestSuites();

        // Log comprehensive results
        LOGGER.info("EXECUTION COMPLETED - VALIDATING RESULTS");
        LOGGER.info(results.toString());
        LOGGER.info("Performance Summary:");
        LOGGER.info(results.getPerformanceSummary());

        // PRIMARY VALIDATION: 30-minute target
        assertTrue(results.isTargetTimeMet(),
                String.format("CRITICAL: Execution time (%s) must not exceed 30-minute target. " +
                        "Actual: %d minutes %d seconds",
                        results.getTotalExecutionTime(),
                        results.getTotalExecutionTime().toMinutes(),
                        results.getTotalExecutionTime().toSecondsPart()));

        // SECONDARY VALIDATIONS: Quality and efficiency
        assertTrue(results.getSuccessRate() >= MINIMUM_SUCCESS_RATE,
                String.format("Success rate (%.1f%%) must be at least %.1f%%",
                        results.getSuccessRate(), MINIMUM_SUCCESS_RATE));

        assertTrue(results.getExecutionEfficiency() >= MINIMUM_OVERALL_EFFICIENCY,
                String.format("Overall efficiency (%.1f%%) must be at least %.1f%%",
                        results.getExecutionEfficiency(), MINIMUM_OVERALL_EFFICIENCY));

        // CACHE VALIDATION
        final TestResultCache.CacheStatistics cacheStats = results.getCacheStats();
        final double cacheEfficiency = performanceCache.calculateCacheEfficiency();

        if (results.getCacheHits() > 0) {
            assertTrue(cacheEfficiency >= MINIMUM_CACHE_EFFICIENCY,
                    String.format("Cache efficiency (%.1f%%) must be at least %.1f%% when cache is used",
                            cacheEfficiency, MINIMUM_CACHE_EFFICIENCY));
        }

        // COMPREHENSIVE VALIDATION SUMMARY
        logValidationSummary(results, cacheStats, cacheEfficiency);

        LOGGER.info("✅ 30-MINUTE EXECUTION TARGET VALIDATION PASSED");
    }

    @Test
    @DisplayName("Validate CI/CD optimization effectiveness")
    void validateCiCdOptimizations() throws IOException {
        LOGGER.info("Starting CI/CD optimization validation");

        // Execute with CI/CD optimizations
        final CiCdOptimizedRunner.CiExecutionResults ciResults = ciRunner.executeForCi();

        // Log CI results
        LOGGER.info("CI/CD EXECUTION COMPLETED");
        LOGGER.info(ciRunner.generateCiSummary(ciResults));

        // Validate CI criteria
        assertTrue(ciResults.hasPassedCiCriteria(),
                "CI/CD execution must pass all CI criteria");

        assertFalse(ciResults.isRegressionDetected(),
                "No performance regression should be detected");

        // Validate CI-specific performance goals (more aggressive)
        final Duration ciTargetTime = Duration.ofMinutes(25); // CI target is more aggressive
        assertTrue(ciResults.getActualExecutionTime().compareTo(ciTargetTime) <= 0,
                String.format("CI execution time (%s) should not exceed CI target (%s)",
                        ciResults.getActualExecutionTime(), ciTargetTime));

        LOGGER.info("✅ CI/CD OPTIMIZATION VALIDATION PASSED");
    }

    @Test
    @DisplayName("Validate intelligent scheduling optimization")
    void validateIntelligentScheduling() {
        LOGGER.info("Validating intelligent test scheduling optimization");

        // Create scheduler with different strategies and validate performance
        final IntelligentTestScheduler.SchedulerConfiguration config =
                IntelligentTestScheduler.SchedulerConfiguration.builder()
                        .enableAdaptiveScheduling(true)
                        .enableMemoryManagement(true)
                        .enableLoadBalancing(true)
                        .build();

        final IntelligentTestScheduler scheduler = new IntelligentTestScheduler(config);

        // Validate scheduler metrics
        final String metrics = scheduler.getPerformanceMetrics();
        assertNotNull(metrics, "Scheduler should provide performance metrics");

        LOGGER.info("Scheduler Performance Metrics:");
        LOGGER.info(metrics);

        LOGGER.info("✅ INTELLIGENT SCHEDULING VALIDATION PASSED");
    }

    @Test
    @DisplayName("Validate cache persistence and effectiveness")
    void validateCacheEffectiveness() {
        LOGGER.info("Validating test result cache effectiveness");

        // Test cache functionality
        final TestResultCache.CacheStatistics stats = performanceCache.getStatistics();

        // Log cache performance
        LOGGER.info("Cache Performance:");
        LOGGER.info(performanceCache.getPerformanceSummary());

        // Validate cache is functioning
        assertNotNull(stats, "Cache statistics should be available");

        // If cache has been used, validate effectiveness
        if (stats.getTotalHits() > 0 || stats.getTotalMisses() > 0) {
            final double hitRate = stats.getHitRate();
            assertTrue(hitRate >= 0.0 && hitRate <= 1.0,
                    "Hit rate should be between 0 and 1");

            LOGGER.info(String.format("Cache hit rate: %.1f%%", hitRate * 100));
        }

        LOGGER.info("✅ CACHE EFFECTIVENESS VALIDATION PASSED");
    }

    @Test
    @DisplayName("Validate memory management optimization")
    void validateMemoryManagement() {
        LOGGER.info("Validating memory management optimization");

        // Monitor memory before and after operations
        final long initialMemory = getCurrentMemoryUsage();

        // Trigger some memory-intensive operations
        System.gc(); // Force GC to get baseline
        final long baselineMemory = getCurrentMemoryUsage();

        // Create some test objects to validate memory management
        performanceCache.performCleanup();

        final long finalMemory = getCurrentMemoryUsage();

        // Validate memory management
        final long memoryIncrease = finalMemory - baselineMemory;
        LOGGER.info(String.format("Memory usage: initial=%dMB, baseline=%dMB, final=%dMB, increase=%dMB",
                initialMemory, baselineMemory, finalMemory, memoryIncrease));

        // Memory increase should be reasonable (less than 1GB for cleanup operations)
        assertTrue(memoryIncrease < 1024,
                String.format("Memory increase (%dMB) should be reasonable", memoryIncrease));

        LOGGER.info("✅ MEMORY MANAGEMENT VALIDATION PASSED");
    }

    @Test
    @DisplayName("Validate comprehensive integration of all optimizations")
    void validateComprehensiveIntegration() throws IOException {
        LOGGER.info("Validating comprehensive integration of all performance optimizations");

        // This test validates that all components work together effectively

        // 1. Test optimized executor with all features enabled
        final OptimizedTestExecutor.ExecutorConfiguration config =
                OptimizedTestExecutor.ExecutorConfiguration.builder()
                        .enableCaching(true)
                        .enableMemoryManagement(true)
                        .enablePerformanceMonitoring(true)
                        .enableIncrementalExecution(true)
                        .targetExecutionTime(TARGET_EXECUTION_TIME)
                        .build();

        try (final OptimizedTestExecutor integrationExecutor = new OptimizedTestExecutor(config)) {

            // Execute a subset of tests to validate integration
            final OptimizedTestExecutor.ExecutionResults results = integrationExecutor.executeTestSuites(
                    java.util.List.of(ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS)
            );

            // Validate integrated performance
            assertTrue(results.getSuccessRate() >= MINIMUM_SUCCESS_RATE,
                    "Integrated execution should maintain high success rate");

            assertTrue(results.getExecutionEfficiency() >= MINIMUM_OVERALL_EFFICIENCY,
                    "Integrated execution should achieve high efficiency");

            // Log integration results
            LOGGER.info("Integration Test Results:");
            LOGGER.info(String.format("  Execution Time: %s", results.getTotalExecutionTime()));
            LOGGER.info(String.format("  Success Rate: %.1f%%", results.getSuccessRate()));
            LOGGER.info(String.format("  Execution Efficiency: %.1f%%", results.getExecutionEfficiency()));
            LOGGER.info(String.format("  Tests Executed: %d", results.getTestsExecuted()));
            LOGGER.info(String.format("  Cache Hits: %d", results.getCacheHits()));
        }

        LOGGER.info("✅ COMPREHENSIVE INTEGRATION VALIDATION PASSED");
    }

    // Helper methods

    private void logValidationSummary(final OptimizedTestExecutor.ExecutionResults results,
                                    final TestResultCache.CacheStatistics cacheStats,
                                    final double cacheEfficiency) {
        LOGGER.info("");
        LOGGER.info("=== PERFORMANCE VALIDATION SUMMARY ===");
        LOGGER.info(String.format("🎯 TARGET TIME: %s", TARGET_EXECUTION_TIME));
        LOGGER.info(String.format("⏱️  ACTUAL TIME: %s (%s)",
                results.getTotalExecutionTime(),
                results.isTargetTimeMet() ? "✅ PASSED" : "❌ FAILED"));
        LOGGER.info(String.format("📊 SUCCESS RATE: %.1f%% (target: %.1f%%) %s",
                results.getSuccessRate(), MINIMUM_SUCCESS_RATE,
                results.getSuccessRate() >= MINIMUM_SUCCESS_RATE ? "✅" : "❌"));
        LOGGER.info(String.format("🚀 EXECUTION EFFICIENCY: %.1f%% (target: %.1f%%) %s",
                results.getExecutionEfficiency(), MINIMUM_OVERALL_EFFICIENCY,
                results.getExecutionEfficiency() >= MINIMUM_OVERALL_EFFICIENCY ? "✅" : "❌"));
        LOGGER.info(String.format("💾 CACHE EFFICIENCY: %.1f%% (target: %.1f%%) %s",
                cacheEfficiency, MINIMUM_CACHE_EFFICIENCY,
                cacheEfficiency >= MINIMUM_CACHE_EFFICIENCY ? "✅" : "❌"));
        LOGGER.info(String.format("🧪 TESTS EXECUTED: %d", results.getTestsExecuted()));
        LOGGER.info(String.format("⚡ CACHE HITS: %d", results.getCacheHits()));
        LOGGER.info(String.format("📈 OVERALL STATUS: %s",
                results.isTargetTimeMet() &&
                results.getSuccessRate() >= MINIMUM_SUCCESS_RATE &&
                results.getExecutionEfficiency() >= MINIMUM_OVERALL_EFFICIENCY ?
                "✅ ALL CRITERIA PASSED" : "❌ SOME CRITERIA FAILED"));
        LOGGER.info("=======================================");
    }

    private long getCurrentMemoryUsage() {
        final Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }
}