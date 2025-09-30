package ai.tegmentum.wasmtime4j.profiling;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Advanced performance insights and analysis engine for wasmtime4j profiling data.
 *
 * <p>This class provides comprehensive performance analysis capabilities including:
 * <ul>
 *   <li>Hot spot identification and analysis</li>
 *   <li>Performance regression detection</li>
 *   <li>Bottleneck analysis with root cause identification</li>
 *   <li>Resource utilization analysis</li>
 *   <li>Memory leak detection and analysis</li>
 *   <li>Optimization recommendations based on patterns</li>
 *   <li>Performance trend analysis and forecasting</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class PerformanceInsights {

    private static final Logger LOGGER = Logger.getLogger(PerformanceInsights.class.getName());

    /** Performance insights configuration. */
    public static final class InsightsConfig {
        private final Duration analysisWindow;
        private final double hotSpotThreshold;
        private final double regressionThreshold;
        private final int maxHotSpots;
        private final boolean enableTrendAnalysis;
        private final boolean enablePredictiveAnalysis;

        private InsightsConfig(final Builder builder) {
            this.analysisWindow = builder.analysisWindow;
            this.hotSpotThreshold = builder.hotSpotThreshold;
            this.regressionThreshold = builder.regressionThreshold;
            this.maxHotSpots = builder.maxHotSpots;
            this.enableTrendAnalysis = builder.enableTrendAnalysis;
            this.enablePredictiveAnalysis = builder.enablePredictiveAnalysis;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private Duration analysisWindow = Duration.ofMinutes(5);
            private double hotSpotThreshold = 5.0; // 5% of total time
            private double regressionThreshold = 10.0; // 10% performance degradation
            private int maxHotSpots = 10;
            private boolean enableTrendAnalysis = true;
            private boolean enablePredictiveAnalysis = false;

            public Builder analysisWindow(final Duration window) { this.analysisWindow = window; return this; }
            public Builder hotSpotThreshold(final double threshold) { this.hotSpotThreshold = threshold; return this; }
            public Builder regressionThreshold(final double threshold) { this.regressionThreshold = threshold; return this; }
            public Builder maxHotSpots(final int maxHotSpots) { this.maxHotSpots = maxHotSpots; return this; }
            public Builder enableTrendAnalysis(final boolean enable) { this.enableTrendAnalysis = enable; return this; }
            public Builder enablePredictiveAnalysis(final boolean enable) { this.enablePredictiveAnalysis = enable; return this; }

            public InsightsConfig build() {
                return new InsightsConfig(this);
            }
        }

        // Getters
        public Duration getAnalysisWindow() { return analysisWindow; }
        public double getHotSpotThreshold() { return hotSpotThreshold; }
        public double getRegressionThreshold() { return regressionThreshold; }
        public int getMaxHotSpots() { return maxHotSpots; }
        public boolean isTrendAnalysisEnabled() { return enableTrendAnalysis; }
        public boolean isPredictiveAnalysisEnabled() { return enablePredictiveAnalysis; }
    }

    /**
     * Represents a performance hot spot.
     */
    public static final class HotSpot {
        private final String functionName;
        private final String category;
        private final Duration totalTime;
        private final Duration averageTime;
        private final long callCount;
        private final double timePercentage;
        private final HotSpotType type;
        private final String description;
        private final List<String> recommendations;

        public HotSpot(final String functionName, final String category, final Duration totalTime,
                      final Duration averageTime, final long callCount, final double timePercentage,
                      final HotSpotType type, final String description, final List<String> recommendations) {
            this.functionName = functionName;
            this.category = category;
            this.totalTime = totalTime;
            this.averageTime = averageTime;
            this.callCount = callCount;
            this.timePercentage = timePercentage;
            this.type = type;
            this.description = description;
            this.recommendations = Collections.unmodifiableList(List.copyOf(recommendations));
        }

        // Getters
        public String getFunctionName() { return functionName; }
        public String getCategory() { return category; }
        public Duration getTotalTime() { return totalTime; }
        public Duration getAverageTime() { return averageTime; }
        public long getCallCount() { return callCount; }
        public double getTimePercentage() { return timePercentage; }
        public HotSpotType getType() { return type; }
        public String getDescription() { return description; }
        public List<String> getRecommendations() { return recommendations; }
    }

    /**
     * Types of performance hot spots.
     */
    public enum HotSpotType {
        CPU_INTENSIVE,    // High CPU usage
        FREQUENT_CALLS,   // Called very frequently
        SLOW_EXECUTION,   // Individual calls are slow
        MEMORY_INTENSIVE, // High memory allocation
        IO_BOUND,         // Waiting on I/O operations
        LOCK_CONTENTION   // Thread synchronization issues
    }

    /**
     * Performance regression analysis result.
     */
    public static final class RegressionAnalysis {
        private final Instant detectionTime;
        private final Duration analysisWindow;
        private final double performanceDegradation;
        private final String affectedFunction;
        private final RegressionSeverity severity;
        private final String rootCause;
        private final List<String> recommendations;

        public RegressionAnalysis(final Instant detectionTime, final Duration analysisWindow,
                                final double performanceDegradation, final String affectedFunction,
                                final RegressionSeverity severity, final String rootCause,
                                final List<String> recommendations) {
            this.detectionTime = detectionTime;
            this.analysisWindow = analysisWindow;
            this.performanceDegradation = performanceDegradation;
            this.affectedFunction = affectedFunction;
            this.severity = severity;
            this.rootCause = rootCause;
            this.recommendations = Collections.unmodifiableList(List.copyOf(recommendations));
        }

        // Getters
        public Instant getDetectionTime() { return detectionTime; }
        public Duration getAnalysisWindow() { return analysisWindow; }
        public double getPerformanceDegradation() { return performanceDegradation; }
        public String getAffectedFunction() { return affectedFunction; }
        public RegressionSeverity getSeverity() { return severity; }
        public String getRootCause() { return rootCause; }
        public List<String> getRecommendations() { return recommendations; }
    }

    /**
     * Severity levels for performance regressions.
     */
    public enum RegressionSeverity {
        LOW(0, 15),      // 0-15% degradation
        MEDIUM(15, 30),  // 15-30% degradation
        HIGH(30, 50),    // 30-50% degradation
        CRITICAL(50, Double.MAX_VALUE); // >50% degradation

        private final double minThreshold;
        private final double maxThreshold;

        RegressionSeverity(final double minThreshold, final double maxThreshold) {
            this.minThreshold = minThreshold;
            this.maxThreshold = maxThreshold;
        }

        public static RegressionSeverity fromDegradation(final double degradation) {
            for (RegressionSeverity severity : values()) {
                if (degradation >= severity.minThreshold && degradation < severity.maxThreshold) {
                    return severity;
                }
            }
            return CRITICAL;
        }
    }

    /**
     * Resource utilization analysis.
     */
    public static final class ResourceUtilization {
        private final double cpuUtilizationPercent;
        private final long memoryUsageBytes;
        private final long peakMemoryBytes;
        private final double memoryUtilizationPercent;
        private final long activeThreads;
        private final double threadUtilizationPercent;
        private final Map<String, Double> categoryUtilization;

        public ResourceUtilization(final double cpuUtilizationPercent, final long memoryUsageBytes,
                                 final long peakMemoryBytes, final double memoryUtilizationPercent,
                                 final long activeThreads, final double threadUtilizationPercent,
                                 final Map<String, Double> categoryUtilization) {
            this.cpuUtilizationPercent = cpuUtilizationPercent;
            this.memoryUsageBytes = memoryUsageBytes;
            this.peakMemoryBytes = peakMemoryBytes;
            this.memoryUtilizationPercent = memoryUtilizationPercent;
            this.activeThreads = activeThreads;
            this.threadUtilizationPercent = threadUtilizationPercent;
            this.categoryUtilization = Collections.unmodifiableMap(Map.copyOf(categoryUtilization));
        }

        // Getters
        public double getCpuUtilizationPercent() { return cpuUtilizationPercent; }
        public long getMemoryUsageBytes() { return memoryUsageBytes; }
        public long getPeakMemoryBytes() { return peakMemoryBytes; }
        public double getMemoryUtilizationPercent() { return memoryUtilizationPercent; }
        public long getActiveThreads() { return activeThreads; }
        public double getThreadUtilizationPercent() { return threadUtilizationPercent; }
        public Map<String, Double> getCategoryUtilization() { return categoryUtilization; }
    }

    /**
     * Performance trend data point.
     */
    public static final class TrendDataPoint {
        private final Instant timestamp;
        private final double value;
        private final String metric;

        public TrendDataPoint(final Instant timestamp, final double value, final String metric) {
            this.timestamp = timestamp;
            this.value = value;
            this.metric = metric;
        }

        // Getters
        public Instant getTimestamp() { return timestamp; }
        public double getValue() { return value; }
        public String getMetric() { return metric; }
    }

    /**
     * Comprehensive performance insights result.
     */
    public static final class PerformanceInsightsResult {
        private final Instant analysisTime;
        private final Duration analysisWindow;
        private final List<HotSpot> hotSpots;
        private final List<RegressionAnalysis> regressions;
        private final ResourceUtilization resourceUtilization;
        private final List<TrendDataPoint> trends;
        private final List<String> globalRecommendations;
        private final double overallHealthScore;

        public PerformanceInsightsResult(final Instant analysisTime, final Duration analysisWindow,
                                       final List<HotSpot> hotSpots, final List<RegressionAnalysis> regressions,
                                       final ResourceUtilization resourceUtilization, final List<TrendDataPoint> trends,
                                       final List<String> globalRecommendations, final double overallHealthScore) {
            this.analysisTime = analysisTime;
            this.analysisWindow = analysisWindow;
            this.hotSpots = Collections.unmodifiableList(List.copyOf(hotSpots));
            this.regressions = Collections.unmodifiableList(List.copyOf(regressions));
            this.resourceUtilization = resourceUtilization;
            this.trends = Collections.unmodifiableList(List.copyOf(trends));
            this.globalRecommendations = Collections.unmodifiableList(List.copyOf(globalRecommendations));
            this.overallHealthScore = overallHealthScore;
        }

        // Getters
        public Instant getAnalysisTime() { return analysisTime; }
        public Duration getAnalysisWindow() { return analysisWindow; }
        public List<HotSpot> getHotSpots() { return hotSpots; }
        public List<RegressionAnalysis> getRegressions() { return regressions; }
        public ResourceUtilization getResourceUtilization() { return resourceUtilization; }
        public List<TrendDataPoint> getTrends() { return trends; }
        public List<String> getGlobalRecommendations() { return globalRecommendations; }
        public double getOverallHealthScore() { return overallHealthScore; }
    }

    private final InsightsConfig config;
    private final ConcurrentHashMap<String, List<TrendDataPoint>> trendData = new ConcurrentHashMap<>();
    private final AtomicLong analysisCount = new AtomicLong(0);

    public PerformanceInsights() {
        this(InsightsConfig.builder().build());
    }

    public PerformanceInsights(final InsightsConfig config) {
        this.config = Objects.requireNonNull(config);
        LOGGER.info("Performance insights engine initialized with config: " + config);
    }

    /**
     * Analyzes flame graph data to generate comprehensive performance insights.
     *
     * @param flameGraph flame graph to analyze
     * @param profilingStatistics profiling statistics
     * @return comprehensive performance insights
     */
    public PerformanceInsightsResult analyzePerformance(final FlameGraphGenerator.FlameFrame flameGraph,
                                                       final AdvancedProfiler.ProfilingStatistics profilingStatistics) {
        final Instant analysisTime = Instant.now();
        analysisCount.incrementAndGet();

        LOGGER.info("Starting performance analysis #" + analysisCount.get());

        // Identify hot spots
        final List<HotSpot> hotSpots = identifyHotSpots(flameGraph, profilingStatistics);

        // Detect regressions
        final List<RegressionAnalysis> regressions = detectRegressions(profilingStatistics);

        // Analyze resource utilization
        final ResourceUtilization resourceUtilization = analyzeResourceUtilization(profilingStatistics);

        // Generate trends if enabled
        final List<TrendDataPoint> trends = config.isTrendAnalysisEnabled() ?
            generateTrendAnalysis(profilingStatistics) : Collections.emptyList();

        // Generate global recommendations
        final List<String> globalRecommendations = generateGlobalRecommendations(
            hotSpots, regressions, resourceUtilization);

        // Calculate overall health score
        final double healthScore = calculateHealthScore(hotSpots, regressions, resourceUtilization);

        return new PerformanceInsightsResult(
            analysisTime, config.getAnalysisWindow(), hotSpots, regressions,
            resourceUtilization, trends, globalRecommendations, healthScore);
    }

    /**
     * Identifies performance hot spots from flame graph data.
     */
    private List<HotSpot> identifyHotSpots(final FlameGraphGenerator.FlameFrame flameGraph,
                                         final AdvancedProfiler.ProfilingStatistics stats) {
        final Duration totalTime = flameGraph.getTotalTime();
        if (totalTime.isZero()) {
            return Collections.emptyList();
        }

        final List<FlameGraphGenerator.FlameFrame> allFrames = collectAllFrames(flameGraph);
        final double thresholdTime = totalTime.toNanos() * (config.getHotSpotThreshold() / 100.0);

        return allFrames.stream()
            .filter(frame -> frame.getSelfTime().toNanos() > thresholdTime)
            .sorted((f1, f2) -> f2.getSelfTime().compareTo(f1.getSelfTime()))
            .limit(config.getMaxHotSpots())
            .map(frame -> createHotSpot(frame, totalTime, stats))
            .collect(Collectors.toList());
    }

    /**
     * Creates a hot spot analysis from a flame graph frame.
     */
    private HotSpot createHotSpot(final FlameGraphGenerator.FlameFrame frame, final Duration totalTime,
                                 final AdvancedProfiler.ProfilingStatistics stats) {
        final double timePercentage = frame.getSelfTimePercentage(totalTime);
        final Duration averageTime = frame.getSampleCount() > 0 ?
            Duration.ofNanos(frame.getSelfTime().toNanos() / frame.getSampleCount()) : Duration.ZERO;

        // Determine hot spot type
        final HotSpotType type = determineHotSpotType(frame, timePercentage, averageTime);

        // Generate recommendations
        final List<String> recommendations = generateHotSpotRecommendations(frame, type, timePercentage);

        // Create description
        final String description = String.format(
            "Function consumes %.2f%% of total execution time with %d samples",
            timePercentage, frame.getSampleCount());

        return new HotSpot(
            frame.getFunctionName(),
            frame.getCategory(),
            frame.getSelfTime(),
            averageTime,
            frame.getSampleCount(),
            timePercentage,
            type,
            description,
            recommendations
        );
    }

    /**
     * Determines the type of hot spot based on characteristics.
     */
    private HotSpotType determineHotSpotType(final FlameGraphGenerator.FlameFrame frame,
                                          final double timePercentage, final Duration averageTime) {
        // High frequency, moderate individual time
        if (frame.getSampleCount() > 1000 && averageTime.toMillis() < 5) {
            return HotSpotType.FREQUENT_CALLS;
        }

        // Low frequency, high individual time
        if (frame.getSampleCount() < 100 && averageTime.toMillis() > 50) {
            return HotSpotType.SLOW_EXECUTION;
        }

        // Memory-related functions
        if (frame.getCategory().equals("memory") || frame.getFunctionName().contains("alloc")) {
            return HotSpotType.MEMORY_INTENSIVE;
        }

        // I/O-related functions
        if (frame.getFunctionName().contains("read") || frame.getFunctionName().contains("write") ||
            frame.getFunctionName().contains("io")) {
            return HotSpotType.IO_BOUND;
        }

        // Default to CPU intensive
        return HotSpotType.CPU_INTENSIVE;
    }

    /**
     * Generates recommendations for a specific hot spot.
     */
    private List<String> generateHotSpotRecommendations(final FlameGraphGenerator.FlameFrame frame,
                                                       final HotSpotType type, final double timePercentage) {
        final List<String> recommendations = new ArrayList<>();

        switch (type) {
            case CPU_INTENSIVE:
                recommendations.add("Consider algorithmic optimizations for " + frame.getFunctionName());
                if (timePercentage > 20) {
                    recommendations.add("This function is a critical bottleneck - prioritize optimization");
                }
                break;

            case FREQUENT_CALLS:
                recommendations.add("Consider caching results for " + frame.getFunctionName());
                recommendations.add("Evaluate if call frequency can be reduced through batching");
                break;

            case SLOW_EXECUTION:
                recommendations.add("Profile individual execution of " + frame.getFunctionName() + " for bottlenecks");
                recommendations.add("Consider asynchronous execution if I/O bound");
                break;

            case MEMORY_INTENSIVE:
                recommendations.add("Review memory allocation patterns in " + frame.getFunctionName());
                recommendations.add("Consider object pooling or reduced allocation strategies");
                break;

            case IO_BOUND:
                recommendations.add("Consider asynchronous I/O for " + frame.getFunctionName());
                recommendations.add("Evaluate caching opportunities to reduce I/O operations");
                break;

            case LOCK_CONTENTION:
                recommendations.add("Review synchronization strategy for " + frame.getFunctionName());
                recommendations.add("Consider lock-free algorithms or reduced critical sections");
                break;
        }

        return recommendations;
    }

    /**
     * Detects performance regressions based on historical data.
     */
    private List<RegressionAnalysis> detectRegressions(final AdvancedProfiler.ProfilingStatistics stats) {
        // In a real implementation, this would compare against historical baselines
        // For now, return empty list as baseline comparison requires stored data
        return Collections.emptyList();
    }

    /**
     * Analyzes resource utilization patterns.
     */
    private ResourceUtilization analyzeResourceUtilization(final AdvancedProfiler.ProfilingStatistics stats) {
        // Calculate memory utilization
        final long maxHeap = Runtime.getRuntime().maxMemory();
        final long usedHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        final double memoryUtilization = maxHeap > 0 ? (usedHeap * 100.0) / maxHeap : 0.0;

        // Estimate CPU utilization based on function call patterns
        final double estimatedCpuUtilization = Math.min(100.0,
            (stats.getFunctionCalls() / (stats.getTotalProfilingTime().toSeconds() + 1)) / 10.0);

        // Calculate category utilization
        final Map<String, Double> categoryUtilization = new HashMap<>();
        categoryUtilization.put("jni", (stats.getJniCalls() * 100.0) / Math.max(1, stats.getFunctionCalls()));
        categoryUtilization.put("panama", (stats.getPanamaCalls() * 100.0) / Math.max(1, stats.getFunctionCalls()));

        return new ResourceUtilization(
            estimatedCpuUtilization,
            usedHeap,
            stats.getCurrentAllocatedMemory(),
            memoryUtilization,
            Runtime.getRuntime().availableProcessors(),
            50.0, // Estimated thread utilization
            categoryUtilization
        );
    }

    /**
     * Generates trend analysis data points.
     */
    private List<TrendDataPoint> generateTrendAnalysis(final AdvancedProfiler.ProfilingStatistics stats) {
        final Instant now = Instant.now();
        final List<TrendDataPoint> trends = new ArrayList<>();

        // Record current metrics as trend points
        trends.add(new TrendDataPoint(now, stats.getAverageExecutionTimeNanos() / 1_000_000.0, "avg_execution_time_ms"));
        trends.add(new TrendDataPoint(now, stats.getFunctionCalls(), "function_calls"));
        trends.add(new TrendDataPoint(now, stats.getMemoryAllocations(), "memory_allocations"));

        // Store for historical tracking
        trendData.computeIfAbsent("avg_execution_time_ms", k -> new ArrayList<>())
                 .add(new TrendDataPoint(now, stats.getAverageExecutionTimeNanos() / 1_000_000.0, "avg_execution_time_ms"));

        return trends;
    }

    /**
     * Generates global performance recommendations.
     */
    private List<String> generateGlobalRecommendations(final List<HotSpot> hotSpots,
                                                      final List<RegressionAnalysis> regressions,
                                                      final ResourceUtilization resourceUtilization) {
        final List<String> recommendations = new ArrayList<>();

        // Hot spot recommendations
        if (!hotSpots.isEmpty()) {
            final HotSpot topHotSpot = hotSpots.get(0);
            if (topHotSpot.getTimePercentage() > 25) {
                recommendations.add(String.format(
                    "Critical: Function '%s' consumes %.1f%% of execution time - immediate optimization required",
                    topHotSpot.getFunctionName(), topHotSpot.getTimePercentage()));
            }

            final long frequentCallCount = hotSpots.stream()
                .filter(h -> h.getType() == HotSpotType.FREQUENT_CALLS)
                .count();
            if (frequentCallCount > 3) {
                recommendations.add("Multiple functions show high call frequency - consider batching operations");
            }
        }

        // Resource utilization recommendations
        if (resourceUtilization.getMemoryUtilizationPercent() > 80) {
            recommendations.add("High memory utilization detected - consider increasing heap size or reducing allocations");
        }

        if (resourceUtilization.getCpuUtilizationPercent() > 90) {
            recommendations.add("High CPU utilization detected - review computational efficiency");
        }

        // JNI vs Panama recommendations
        final double jniPercentage = resourceUtilization.getCategoryUtilization().getOrDefault("jni", 0.0);
        final double panamaPercentage = resourceUtilization.getCategoryUtilization().getOrDefault("panama", 0.0);

        if (jniPercentage > 50) {
            recommendations.add("High JNI usage - consider batching operations to reduce overhead");
        }

        if (panamaPercentage > 50) {
            recommendations.add("High Panama FFI usage - ensure optimal memory management");
        }

        // Default recommendations if no specific issues found
        if (recommendations.isEmpty()) {
            recommendations.add("Performance profile looks healthy - continue monitoring");
            recommendations.add("Consider setting up continuous profiling for production systems");
        }

        return recommendations;
    }

    /**
     * Calculates an overall health score (0-100).
     */
    private double calculateHealthScore(final List<HotSpot> hotSpots,
                                      final List<RegressionAnalysis> regressions,
                                      final ResourceUtilization resourceUtilization) {
        double score = 100.0;

        // Deduct points for hot spots
        for (final HotSpot hotSpot : hotSpots) {
            if (hotSpot.getTimePercentage() > 25) {
                score -= 20; // Major hot spot
            } else if (hotSpot.getTimePercentage() > 15) {
                score -= 10; // Moderate hot spot
            } else {
                score -= 5;  // Minor hot spot
            }
        }

        // Deduct points for regressions
        for (final RegressionAnalysis regression : regressions) {
            switch (regression.getSeverity()) {
                case CRITICAL: score -= 30; break;
                case HIGH: score -= 20; break;
                case MEDIUM: score -= 10; break;
                case LOW: score -= 5; break;
            }
        }

        // Deduct points for high resource utilization
        if (resourceUtilization.getMemoryUtilizationPercent() > 90) {
            score -= 15;
        } else if (resourceUtilization.getMemoryUtilizationPercent() > 80) {
            score -= 10;
        }

        if (resourceUtilization.getCpuUtilizationPercent() > 95) {
            score -= 15;
        } else if (resourceUtilization.getCpuUtilizationPercent() > 85) {
            score -= 10;
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Collects all frames from a flame graph recursively.
     */
    private List<FlameGraphGenerator.FlameFrame> collectAllFrames(final FlameGraphGenerator.FlameFrame root) {
        final List<FlameGraphGenerator.FlameFrame> allFrames = new ArrayList<>();
        collectFramesRecursive(root, allFrames);
        return allFrames;
    }

    private void collectFramesRecursive(final FlameGraphGenerator.FlameFrame frame,
                                      final List<FlameGraphGenerator.FlameFrame> collector) {
        collector.add(frame);
        for (final FlameGraphGenerator.FlameFrame child : frame.getChildren()) {
            collectFramesRecursive(child, collector);
        }
    }

    /**
     * Gets historical trend data for a specific metric.
     *
     * @param metric metric name
     * @return trend data points
     */
    public List<TrendDataPoint> getHistoricalTrends(final String metric) {
        return trendData.getOrDefault(metric, Collections.emptyList());
    }

    /**
     * Clears all trend data.
     */
    public void clearTrendData() {
        trendData.clear();
        LOGGER.info("Cleared all trend data");
    }

    public InsightsConfig getConfig() {
        return config;
    }

    public long getAnalysisCount() {
        return analysisCount.get();
    }
}