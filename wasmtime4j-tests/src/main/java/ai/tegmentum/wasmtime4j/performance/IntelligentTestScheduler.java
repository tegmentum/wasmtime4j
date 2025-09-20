package ai.tegmentum.wasmtime4j.performance;

import ai.tegmentum.wasmtime4j.webassembly.WasmTestCase;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Intelligent test scheduler that optimizes parallel execution of Wasmtime test suites.
 *
 * <p>This scheduler implements several optimization strategies:
 * <ul>
 *   <li>Load balancing based on test complexity and historical execution times</li>
 *   <li>Adaptive work stealing for optimal resource utilization</li>
 *   <li>Memory-aware scheduling to prevent OOM conditions</li>
 *   <li>Dynamic thread pool sizing based on system resources</li>
 *   <li>Batching of similar tests for cache efficiency</li>
 * </ul>
 *
 * <p>The scheduler maintains historical performance data to make intelligent
 * decisions about test ordering and parallelization.
 */
public final class IntelligentTestScheduler {
    private static final Logger LOGGER = Logger.getLogger(IntelligentTestScheduler.class.getName());

    // Performance constants
    private static final int DEFAULT_PARALLELISM = Runtime.getRuntime().availableProcessors();
    private static final long MEMORY_CHECK_INTERVAL_MS = 5_000;
    private static final double MEMORY_PRESSURE_THRESHOLD = 0.85;
    private static final int MIN_BATCH_SIZE = 10;
    private static final int MAX_BATCH_SIZE = 100;

    // Scheduling metrics
    private final AtomicInteger activeTests = new AtomicInteger(0);
    private final AtomicLong totalScheduledTests = new AtomicLong(0);
    private final AtomicLong totalCompletedTests = new AtomicLong(0);
    private final TestExecutionHistory executionHistory;
    private final ResourceMonitor resourceMonitor;
    private final Instant schedulerStartTime;

    /**
     * Configuration for the intelligent scheduler.
     */
    public static final class SchedulerConfiguration {
        private final int maxParallelism;
        private final boolean enableAdaptiveScheduling;
        private final boolean enableMemoryManagement;
        private final boolean enableLoadBalancing;
        private final Duration maxTestTimeout;
        private final TestPriorityStrategy priorityStrategy;

        private SchedulerConfiguration(final Builder builder) {
            this.maxParallelism = builder.maxParallelism;
            this.enableAdaptiveScheduling = builder.enableAdaptiveScheduling;
            this.enableMemoryManagement = builder.enableMemoryManagement;
            this.enableLoadBalancing = builder.enableLoadBalancing;
            this.maxTestTimeout = builder.maxTestTimeout;
            this.priorityStrategy = builder.priorityStrategy;
        }

        public int getMaxParallelism() {
            return maxParallelism;
        }

        public boolean isAdaptiveSchedulingEnabled() {
            return enableAdaptiveScheduling;
        }

        public boolean isMemoryManagementEnabled() {
            return enableMemoryManagement;
        }

        public boolean isLoadBalancingEnabled() {
            return enableLoadBalancing;
        }

        public Duration getMaxTestTimeout() {
            return maxTestTimeout;
        }

        public TestPriorityStrategy getPriorityStrategy() {
            return priorityStrategy;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int maxParallelism = DEFAULT_PARALLELISM;
            private boolean enableAdaptiveScheduling = true;
            private boolean enableMemoryManagement = true;
            private boolean enableLoadBalancing = true;
            private Duration maxTestTimeout = Duration.ofMinutes(5);
            private TestPriorityStrategy priorityStrategy = TestPriorityStrategy.BALANCED;

            public Builder maxParallelism(final int parallelism) {
                this.maxParallelism = parallelism;
                return this;
            }

            public Builder enableAdaptiveScheduling(final boolean enable) {
                this.enableAdaptiveScheduling = enable;
                return this;
            }

            public Builder enableMemoryManagement(final boolean enable) {
                this.enableMemoryManagement = enable;
                return this;
            }

            public Builder enableLoadBalancing(final boolean enable) {
                this.enableLoadBalancing = enable;
                return this;
            }

            public Builder maxTestTimeout(final Duration timeout) {
                this.maxTestTimeout = timeout;
                return this;
            }

            public Builder priorityStrategy(final TestPriorityStrategy strategy) {
                this.priorityStrategy = strategy;
                return this;
            }

            public SchedulerConfiguration build() {
                return new SchedulerConfiguration(this);
            }
        }
    }

    /**
     * Strategy for test prioritization.
     */
    public enum TestPriorityStrategy {
        /** Balance execution time and memory usage. */
        BALANCED,
        /** Prioritize fast tests first. */
        FASTEST_FIRST,
        /** Prioritize slow tests first to run in parallel. */
        SLOWEST_FIRST,
        /** Group similar tests together for cache efficiency. */
        SIMILARITY_BASED,
        /** Prioritize by test complexity. */
        COMPLEXITY_BASED
    }

    /**
     * Represents a scheduled test execution with metadata.
     */
    public static final class ScheduledTest {
        private final WasmTestCase testCase;
        private final int priority;
        private final long estimatedDurationMs;
        private final long estimatedMemoryUsageMB;
        private final TestComplexity complexity;
        private final Instant scheduledTime;

        public ScheduledTest(final WasmTestCase testCase, final int priority,
                           final long estimatedDurationMs, final long estimatedMemoryUsageMB,
                           final TestComplexity complexity) {
            this.testCase = testCase;
            this.priority = priority;
            this.estimatedDurationMs = estimatedDurationMs;
            this.estimatedMemoryUsageMB = estimatedMemoryUsageMB;
            this.complexity = complexity;
            this.scheduledTime = Instant.now();
        }

        public WasmTestCase getTestCase() {
            return testCase;
        }

        public int getPriority() {
            return priority;
        }

        public long getEstimatedDurationMs() {
            return estimatedDurationMs;
        }

        public long getEstimatedMemoryUsageMB() {
            return estimatedMemoryUsageMB;
        }

        public TestComplexity getComplexity() {
            return complexity;
        }

        public Instant getScheduledTime() {
            return scheduledTime;
        }
    }

    /**
     * Test complexity classification.
     */
    public enum TestComplexity {
        SIMPLE(1),
        MODERATE(2),
        COMPLEX(3),
        VERY_COMPLEX(4);

        private final int weight;

        TestComplexity(final int weight) {
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }
    }

    /**
     * Execution result for a scheduled test.
     */
    public static final class TestExecutionResult {
        private final ScheduledTest scheduledTest;
        private final boolean successful;
        private final Duration actualDuration;
        private final long actualMemoryUsageMB;
        private final Exception exception;
        private final Instant completionTime;

        public TestExecutionResult(final ScheduledTest scheduledTest, final boolean successful,
                                 final Duration actualDuration, final long actualMemoryUsageMB,
                                 final Exception exception) {
            this.scheduledTest = scheduledTest;
            this.successful = successful;
            this.actualDuration = actualDuration;
            this.actualMemoryUsageMB = actualMemoryUsageMB;
            this.exception = exception;
            this.completionTime = Instant.now();
        }

        public ScheduledTest getScheduledTest() {
            return scheduledTest;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public Duration getActualDuration() {
            return actualDuration;
        }

        public long getActualMemoryUsageMB() {
            return actualMemoryUsageMB;
        }

        public Exception getException() {
            return exception;
        }

        public Instant getCompletionTime() {
            return completionTime;
        }

        /**
         * Gets the scheduling accuracy ratio.
         *
         * @return ratio of estimated to actual duration (1.0 = perfect prediction)
         */
        public double getSchedulingAccuracy() {
            if (actualDuration.toMillis() == 0) {
                return 1.0;
            }
            return (double) scheduledTest.getEstimatedDurationMs() / actualDuration.toMillis();
        }
    }

    /**
     * Creates a new intelligent test scheduler.
     *
     * @param configuration scheduler configuration
     */
    public IntelligentTestScheduler(final SchedulerConfiguration configuration) {
        this.executionHistory = new TestExecutionHistory();
        this.resourceMonitor = new ResourceMonitor();
        this.schedulerStartTime = Instant.now();

        LOGGER.info(String.format("Initialized IntelligentTestScheduler with %d max parallelism, "
                + "adaptive=%s, memory_mgmt=%s, load_balancing=%s",
                configuration.getMaxParallelism(),
                configuration.isAdaptiveSchedulingEnabled(),
                configuration.isMemoryManagementEnabled(),
                configuration.isLoadBalancingEnabled()));
    }

    /**
     * Schedules a list of test cases for optimal parallel execution.
     *
     * @param testCases list of test cases to schedule
     * @param configuration scheduler configuration
     * @return list of scheduled tests in optimal execution order
     */
    public List<ScheduledTest> scheduleTests(final List<WasmTestCase> testCases,
                                           final SchedulerConfiguration configuration) {
        LOGGER.info(String.format("Scheduling %d tests with strategy: %s",
                testCases.size(), configuration.getPriorityStrategy()));

        final List<ScheduledTest> scheduledTests = new ArrayList<>();

        // Analyze test cases and assign priorities
        for (final WasmTestCase testCase : testCases) {
            final TestComplexity complexity = analyzeTestComplexity(testCase);
            final long estimatedDuration = estimateExecutionTime(testCase, complexity);
            final long estimatedMemory = estimateMemoryUsage(testCase, complexity);
            final int priority = calculatePriority(testCase, complexity, configuration);

            scheduledTests.add(new ScheduledTest(testCase, priority, estimatedDuration,
                    estimatedMemory, complexity));
        }

        // Sort tests based on priority strategy
        sortTestsByStrategy(scheduledTests, configuration.getPriorityStrategy());

        // Apply load balancing if enabled
        if (configuration.isLoadBalancingEnabled()) {
            balanceTestLoad(scheduledTests, configuration);
        }

        totalScheduledTests.addAndGet(scheduledTests.size());
        LOGGER.info(String.format("Scheduled %d tests for execution", scheduledTests.size()));

        return scheduledTests;
    }

    /**
     * Executes scheduled tests in parallel with intelligent resource management.
     *
     * @param scheduledTests list of scheduled tests
     * @param configuration scheduler configuration
     * @param testExecutor function to execute individual tests
     * @return list of execution results
     */
    public <T> List<TestExecutionResult> executeTests(
            final List<ScheduledTest> scheduledTests,
            final SchedulerConfiguration configuration,
            final TestExecutor<T> testExecutor) {

        LOGGER.info(String.format("Starting parallel execution of %d tests", scheduledTests.size()));
        final Instant executionStart = Instant.now();

        // Create adaptive thread pool
        final ForkJoinPool executorPool = new ForkJoinPool(
                configuration.getMaxParallelism(),
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                null,
                true // Enable work stealing
        );

        try {
            // Start resource monitoring if enabled
            final CompletableFuture<Void> resourceMonitorFuture;
            if (configuration.isMemoryManagementEnabled()) {
                resourceMonitorFuture = startResourceMonitoring(configuration);
            } else {
                resourceMonitorFuture = CompletableFuture.completedFuture(null);
            }

            // Execute tests in batches for optimal memory usage
            final List<TestExecutionResult> results = new ArrayList<>();
            final List<List<ScheduledTest>> batches = createOptimalBatches(scheduledTests, configuration);

            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                final List<ScheduledTest> batch = batches.get(batchIndex);
                LOGGER.info(String.format("Executing batch %d/%d with %d tests",
                        batchIndex + 1, batches.size(), batch.size()));

                // Wait for memory pressure to subside if needed
                if (configuration.isMemoryManagementEnabled()) {
                    waitForMemoryAvailability();
                }

                // Execute batch in parallel
                final List<CompletableFuture<TestExecutionResult>> batchFutures = new ArrayList<>();

                for (final ScheduledTest scheduledTest : batch) {
                    final CompletableFuture<TestExecutionResult> future = CompletableFuture
                            .supplyAsync(() -> executeIndividualTest(scheduledTest, testExecutor), executorPool)
                            .orTimeout(configuration.getMaxTestTimeout().toMillis(),
                                    java.util.concurrent.TimeUnit.MILLISECONDS)
                            .exceptionally(throwable -> handleTestException(scheduledTest, throwable));

                    batchFutures.add(future);
                }

                // Wait for batch completion
                final CompletableFuture<Void> batchCompletion = CompletableFuture.allOf(
                        batchFutures.toArray(new CompletableFuture[0]));

                batchCompletion.join();

                // Collect results
                for (final CompletableFuture<TestExecutionResult> future : batchFutures) {
                    results.add(future.join());
                }

                // Optional GC between batches for memory management
                if (configuration.isMemoryManagementEnabled() && shouldTriggerGC()) {
                    System.gc();
                    Thread.yield();
                }
            }

            // Stop resource monitoring
            resourceMonitorFuture.cancel(true);

            final Duration totalExecutionTime = Duration.between(executionStart, Instant.now());
            totalCompletedTests.addAndGet(results.size());

            LOGGER.info(String.format("Completed execution of %d tests in %d ms. "
                    + "Success rate: %.1f%%, Average accuracy: %.2f",
                    results.size(),
                    totalExecutionTime.toMillis(),
                    calculateSuccessRate(results),
                    calculateSchedulingAccuracy(results)));

            // Update execution history for future optimizations
            updateExecutionHistory(results);

            return results;

        } finally {
            executorPool.shutdown();
            try {
                if (!executorPool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorPool.shutdownNow();
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                executorPool.shutdownNow();
            }
        }
    }

    /**
     * Functional interface for test execution.
     *
     * @param <T> result type
     */
    @FunctionalInterface
    public interface TestExecutor<T> {
        T execute(WasmTestCase testCase) throws Exception;
    }

    /**
     * Analyzes the complexity of a test case.
     */
    private TestComplexity analyzeTestComplexity(final WasmTestCase testCase) {
        final int moduleSize = testCase.getModuleBytes().length;
        final String testName = testCase.getTestName().toLowerCase();

        // Use historical data if available
        final TestComplexity historical = executionHistory.getComplexity(testCase.getTestName());
        if (historical != null) {
            return historical;
        }

        // Heuristic-based complexity analysis
        if (moduleSize > 1024 * 1024) { // > 1MB
            return TestComplexity.VERY_COMPLEX;
        } else if (moduleSize > 100 * 1024 || testName.contains("memory") || testName.contains("loop")) {
            return TestComplexity.COMPLEX;
        } else if (moduleSize > 10 * 1024 || testName.contains("function") || testName.contains("call")) {
            return TestComplexity.MODERATE;
        } else {
            return TestComplexity.SIMPLE;
        }
    }

    /**
     * Estimates execution time for a test case.
     */
    private long estimateExecutionTime(final WasmTestCase testCase, final TestComplexity complexity) {
        // Use historical data if available
        final Long historical = executionHistory.getAverageExecutionTime(testCase.getTestName());
        if (historical != null) {
            return historical;
        }

        // Base estimation on complexity and module size
        final int moduleSize = testCase.getModuleBytes().length;
        final long baseTime;

        switch (complexity) {
            case SIMPLE:
                baseTime = 100;
                break;
            case MODERATE:
                baseTime = 500;
                break;
            case COMPLEX:
                baseTime = 2000;
                break;
            case VERY_COMPLEX:
                baseTime = 10000;
                break;
            default:
                baseTime = 1000;
        }

        // Adjust based on module size
        final long sizeMultiplier = Math.max(1, moduleSize / 10240); // 10KB chunks
        return baseTime * sizeMultiplier;
    }

    /**
     * Estimates memory usage for a test case.
     */
    private long estimateMemoryUsage(final WasmTestCase testCase, final TestComplexity complexity) {
        final int moduleSize = testCase.getModuleBytes().length;

        // Base memory usage estimation
        final long baseMemory = moduleSize * 4; // Assume 4x overhead for compilation
        final long complexityMemory = complexity.getWeight() * 10 * 1024 * 1024; // 10MB per complexity level

        return (baseMemory + complexityMemory) / (1024 * 1024); // Convert to MB
    }

    /**
     * Calculates priority for test execution.
     */
    private int calculatePriority(final WasmTestCase testCase, final TestComplexity complexity,
                                final SchedulerConfiguration configuration) {
        final TestPriorityStrategy strategy = configuration.getPriorityStrategy();
        final long estimatedDuration = estimateExecutionTime(testCase, complexity);
        final long estimatedMemory = estimateMemoryUsage(testCase, complexity);

        switch (strategy) {
            case FASTEST_FIRST:
                return (int) -estimatedDuration; // Lower duration = higher priority
            case SLOWEST_FIRST:
                return (int) estimatedDuration; // Higher duration = higher priority
            case COMPLEXITY_BASED:
                return complexity.getWeight() * 1000;
            case SIMILARITY_BASED:
                return calculateSimilarityScore(testCase);
            case BALANCED:
            default:
                // Balance execution time, memory usage, and complexity
                return (int) (estimatedDuration + estimatedMemory * 10 + complexity.getWeight() * 100);
        }
    }

    /**
     * Sorts tests based on the specified strategy.
     */
    private void sortTestsByStrategy(final List<ScheduledTest> scheduledTests,
                                   final TestPriorityStrategy strategy) {
        final Comparator<ScheduledTest> comparator;

        switch (strategy) {
            case FASTEST_FIRST:
                comparator = Comparator.comparingLong(ScheduledTest::getEstimatedDurationMs);
                break;
            case SLOWEST_FIRST:
                comparator = Comparator.comparingLong(ScheduledTest::getEstimatedDurationMs).reversed();
                break;
            case COMPLEXITY_BASED:
                comparator = Comparator.comparing(t -> t.getComplexity().getWeight(), Comparator.reverseOrder());
                break;
            case SIMILARITY_BASED:
                comparator = Comparator.comparingInt(this::calculateTestSimilarityGroup);
                break;
            case BALANCED:
            default:
                comparator = Comparator.comparingInt(ScheduledTest::getPriority);
                break;
        }

        scheduledTests.sort(comparator);
    }

    /**
     * Applies load balancing to distribute work evenly.
     */
    private void balanceTestLoad(final List<ScheduledTest> scheduledTests,
                               final SchedulerConfiguration configuration) {
        // Implement work-stealing inspired load balancing
        final int parallelism = configuration.getMaxParallelism();

        // Calculate total estimated work
        final long totalWork = scheduledTests.stream()
                .mapToLong(ScheduledTest::getEstimatedDurationMs)
                .sum();

        final long targetWorkPerThread = totalWork / parallelism;

        LOGGER.fine(String.format("Load balancing %d tests across %d threads. "
                + "Total work: %d ms, Target per thread: %d ms",
                scheduledTests.size(), parallelism, totalWork, targetWorkPerThread));

        // Reorder tests to balance load (simplified round-robin approach)
        final List<List<ScheduledTest>> buckets = new ArrayList<>();
        for (int i = 0; i < parallelism; i++) {
            buckets.add(new ArrayList<>());
        }

        // Distribute tests in round-robin fashion
        for (int i = 0; i < scheduledTests.size(); i++) {
            buckets.get(i % parallelism).add(scheduledTests.get(i));
        }

        // Flatten buckets back to single list
        scheduledTests.clear();
        for (final List<ScheduledTest> bucket : buckets) {
            scheduledTests.addAll(bucket);
        }
    }

    /**
     * Creates optimal batches for memory-efficient execution.
     */
    private List<List<ScheduledTest>> createOptimalBatches(final List<ScheduledTest> scheduledTests,
                                                         final SchedulerConfiguration configuration) {
        final List<List<ScheduledTest>> batches = new ArrayList<>();

        if (!configuration.isMemoryManagementEnabled()) {
            // No batching if memory management is disabled
            batches.add(new ArrayList<>(scheduledTests));
            return batches;
        }

        final long availableMemoryMB = resourceMonitor.getAvailableMemoryMB();
        final long targetBatchMemoryMB = (long) (availableMemoryMB * 0.7); // Use 70% of available memory

        List<ScheduledTest> currentBatch = new ArrayList<>();
        long currentBatchMemory = 0;

        for (final ScheduledTest test : scheduledTests) {
            if (currentBatch.size() >= MAX_BATCH_SIZE ||
                (currentBatchMemory + test.getEstimatedMemoryUsageMB() > targetBatchMemoryMB &&
                 currentBatch.size() >= MIN_BATCH_SIZE)) {

                batches.add(new ArrayList<>(currentBatch));
                currentBatch.clear();
                currentBatchMemory = 0;
            }

            currentBatch.add(test);
            currentBatchMemory += test.getEstimatedMemoryUsageMB();
        }

        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }

        LOGGER.info(String.format("Created %d batches for optimal memory usage. "
                + "Target batch memory: %d MB", batches.size(), targetBatchMemoryMB));

        return batches;
    }

    /**
     * Executes an individual test with monitoring.
     */
    private <T> TestExecutionResult executeIndividualTest(final ScheduledTest scheduledTest,
                                                        final TestExecutor<T> testExecutor) {
        activeTests.incrementAndGet();
        final Instant startTime = Instant.now();
        final long startMemory = resourceMonitor.getCurrentMemoryUsageMB();

        try {
            testExecutor.execute(scheduledTest.getTestCase());

            final Duration actualDuration = Duration.between(startTime, Instant.now());
            final long actualMemory = resourceMonitor.getCurrentMemoryUsageMB() - startMemory;

            return new TestExecutionResult(scheduledTest, true, actualDuration,
                    Math.max(0, actualMemory), null);

        } catch (final Exception e) {
            final Duration actualDuration = Duration.between(startTime, Instant.now());
            final long actualMemory = resourceMonitor.getCurrentMemoryUsageMB() - startMemory;

            return new TestExecutionResult(scheduledTest, false, actualDuration,
                    Math.max(0, actualMemory), e);
        } finally {
            activeTests.decrementAndGet();
        }
    }

    /**
     * Handles test execution exceptions.
     */
    private TestExecutionResult handleTestException(final ScheduledTest scheduledTest,
                                                  final Throwable throwable) {
        LOGGER.warning(String.format("Test %s failed with exception: %s",
                scheduledTest.getTestCase().getTestName(), throwable.getMessage()));

        return new TestExecutionResult(scheduledTest, false, Duration.ZERO, 0,
                throwable instanceof Exception ? (Exception) throwable : new RuntimeException(throwable));
    }

    /**
     * Starts resource monitoring in background.
     */
    private CompletableFuture<Void> startResourceMonitoring(final SchedulerConfiguration configuration) {
        return CompletableFuture.runAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    resourceMonitor.updateMetrics();

                    if (resourceMonitor.isMemoryPressureHigh()) {
                        LOGGER.warning("High memory pressure detected. Consider reducing parallelism.");
                    }

                    Thread.sleep(MEMORY_CHECK_INTERVAL_MS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * Waits for memory to become available.
     */
    private void waitForMemoryAvailability() {
        while (resourceMonitor.getMemoryPressure() > MEMORY_PRESSURE_THRESHOLD) {
            try {
                LOGGER.info("Waiting for memory pressure to subside...");
                Thread.sleep(1000);
                resourceMonitor.updateMetrics();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Determines if GC should be triggered.
     */
    private boolean shouldTriggerGC() {
        return resourceMonitor.getMemoryPressure() > 0.7;
    }

    /**
     * Calculates success rate from execution results.
     */
    private double calculateSuccessRate(final List<TestExecutionResult> results) {
        final long successful = results.stream().mapToLong(r -> r.isSuccessful() ? 1 : 0).sum();
        return results.size() > 0 ? (successful * 100.0) / results.size() : 0.0;
    }

    /**
     * Calculates average scheduling accuracy.
     */
    private double calculateSchedulingAccuracy(final List<TestExecutionResult> results) {
        return results.stream()
                .filter(TestExecutionResult::isSuccessful)
                .mapToDouble(TestExecutionResult::getSchedulingAccuracy)
                .average()
                .orElse(0.0);
    }

    /**
     * Updates execution history for future optimizations.
     */
    private void updateExecutionHistory(final List<TestExecutionResult> results) {
        for (final TestExecutionResult result : results) {
            executionHistory.recordExecution(
                    result.getScheduledTest().getTestCase().getTestName(),
                    result.getScheduledTest().getComplexity(),
                    result.getActualDuration().toMillis(),
                    result.getActualMemoryUsageMB(),
                    result.isSuccessful()
            );
        }
    }

    /**
     * Calculates similarity score for test grouping.
     */
    private int calculateSimilarityScore(final WasmTestCase testCase) {
        final String testName = testCase.getTestName();

        // Group tests by type (simple heuristic)
        if (testName.contains("memory")) {
            return 1000;
        } else if (testName.contains("function")) {
            return 2000;
        } else if (testName.contains("import")) {
            return 3000;
        } else if (testName.contains("table")) {
            return 4000;
        } else {
            return 5000;
        }
    }

    /**
     * Calculates test similarity group for sorting.
     */
    private int calculateTestSimilarityGroup(final ScheduledTest test) {
        return calculateSimilarityScore(test.getTestCase());
    }

    /**
     * Gets scheduling performance metrics.
     *
     * @return performance metrics summary
     */
    public String getPerformanceMetrics() {
        final Duration uptime = Duration.between(schedulerStartTime, Instant.now());
        final long scheduled = totalScheduledTests.get();
        final long completed = totalCompletedTests.get();
        final int active = activeTests.get();

        return String.format(
                "Scheduler Performance Metrics:\n" +
                "  Uptime: %d ms\n" +
                "  Tests scheduled: %d\n" +
                "  Tests completed: %d\n" +
                "  Tests active: %d\n" +
                "  Completion rate: %.1f%%\n" +
                "  Memory pressure: %.1f%%\n" +
                "  Scheduling accuracy: %.2f",
                uptime.toMillis(),
                scheduled,
                completed,
                active,
                scheduled > 0 ? (completed * 100.0) / scheduled : 0.0,
                resourceMonitor.getMemoryPressure() * 100,
                executionHistory.getAverageSchedulingAccuracy()
        );
    }

    /**
     * Simple test execution history tracker.
     */
    private static final class TestExecutionHistory {
        private final java.util.concurrent.ConcurrentHashMap<String, TestStats> testStats =
                new java.util.concurrent.ConcurrentHashMap<>();

        void recordExecution(final String testName, final TestComplexity complexity,
                           final long durationMs, final long memoryMB, final boolean successful) {
            testStats.computeIfAbsent(testName, k -> new TestStats()).addExecution(
                    complexity, durationMs, memoryMB, successful);
        }

        TestComplexity getComplexity(final String testName) {
            final TestStats stats = testStats.get(testName);
            return stats != null ? stats.getAverageComplexity() : null;
        }

        Long getAverageExecutionTime(final String testName) {
            final TestStats stats = testStats.get(testName);
            return stats != null ? stats.getAverageExecutionTime() : null;
        }

        double getAverageSchedulingAccuracy() {
            return testStats.values().stream()
                    .mapToDouble(TestStats::getSchedulingAccuracy)
                    .average()
                    .orElse(1.0);
        }

        private static final class TestStats {
            private final List<TestComplexity> complexities = new ArrayList<>();
            private final List<Long> executionTimes = new ArrayList<>();
            private final List<Long> memoryUsages = new ArrayList<>();
            private int successCount = 0;
            private int totalCount = 0;

            synchronized void addExecution(final TestComplexity complexity, final long durationMs,
                                         final long memoryMB, final boolean successful) {
                complexities.add(complexity);
                executionTimes.add(durationMs);
                memoryUsages.add(memoryMB);
                if (successful) {
                    successCount++;
                }
                totalCount++;
            }

            synchronized TestComplexity getAverageComplexity() {
                if (complexities.isEmpty()) {
                    return null;
                }
                final int avgWeight = (int) complexities.stream()
                        .mapToInt(c -> c.getWeight())
                        .average()
                        .orElse(1);
                for (final TestComplexity complexity : TestComplexity.values()) {
                    if (complexity.getWeight() >= avgWeight) {
                        return complexity;
                    }
                }
                return TestComplexity.SIMPLE;
            }

            synchronized Long getAverageExecutionTime() {
                return executionTimes.isEmpty() ? null :
                        (long) executionTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            }

            synchronized double getSchedulingAccuracy() {
                // Simplified accuracy calculation
                return totalCount > 0 ? (double) successCount / totalCount : 1.0;
            }
        }
    }

    /**
     * Simple resource monitor for memory and CPU tracking.
     */
    private static final class ResourceMonitor {
        private volatile long currentMemoryUsageMB;
        private volatile long availableMemoryMB;
        private volatile double memoryPressure;

        ResourceMonitor() {
            updateMetrics();
        }

        void updateMetrics() {
            final Runtime runtime = Runtime.getRuntime();
            final long totalMemory = runtime.totalMemory();
            final long freeMemory = runtime.freeMemory();
            final long maxMemory = runtime.maxMemory();

            currentMemoryUsageMB = (totalMemory - freeMemory) / (1024 * 1024);
            availableMemoryMB = (maxMemory - totalMemory + freeMemory) / (1024 * 1024);
            memoryPressure = (double) (totalMemory - freeMemory) / maxMemory;
        }

        long getCurrentMemoryUsageMB() {
            return currentMemoryUsageMB;
        }

        long getAvailableMemoryMB() {
            return availableMemoryMB;
        }

        double getMemoryPressure() {
            return memoryPressure;
        }

        boolean isMemoryPressureHigh() {
            return memoryPressure > MEMORY_PRESSURE_THRESHOLD;
        }
    }
}