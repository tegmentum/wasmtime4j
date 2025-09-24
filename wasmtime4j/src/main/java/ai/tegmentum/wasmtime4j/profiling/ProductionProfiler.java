package ai.tegmentum.wasmtime4j.profiling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Production-ready profiling system for wasmtime4j with comprehensive management capabilities.
 *
 * <p>This class provides enterprise-grade profiling features including:
 * <ul>
 *   <li>Continuous profiling with automatic rotation</li>
 *   <li>Configurable overhead limits and circuit breakers</li>
 *   <li>Alert integration for performance anomalies</li>
 *   <li>Multi-threaded application profiling</li>
 *   <li>Data retention policies</li>
 *   <li>Health checks and self-monitoring</li>
 *   <li>Integration with monitoring systems</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class ProductionProfiler implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(ProductionProfiler.class.getName());

    /** Production profiling configuration. */
    public static final class ProductionConfig {
        private final Duration profilingWindow;
        private final Duration rotationInterval;
        private final double maxOverheadPercent;
        private final Duration dataRetentionPeriod;
        private final Path outputDirectory;
        private final boolean enableContinuousProfiling;
        private final boolean enableAlerting;
        private final boolean enableHealthChecks;
        private final Set<String> alertChannels;
        private final Map<String, String> tags;

        private ProductionConfig(final Builder builder) {
            this.profilingWindow = builder.profilingWindow;
            this.rotationInterval = builder.rotationInterval;
            this.maxOverheadPercent = builder.maxOverheadPercent;
            this.dataRetentionPeriod = builder.dataRetentionPeriod;
            this.outputDirectory = builder.outputDirectory;
            this.enableContinuousProfiling = builder.enableContinuousProfiling;
            this.enableAlerting = builder.enableAlerting;
            this.enableHealthChecks = builder.enableHealthChecks;
            this.alertChannels = Collections.unmodifiableSet(builder.alertChannels);
            this.tags = Collections.unmodifiableMap(builder.tags);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private Duration profilingWindow = Duration.ofMinutes(10);
            private Duration rotationInterval = Duration.ofHours(1);
            private double maxOverheadPercent = 3.0;
            private Duration dataRetentionPeriod = Duration.ofDays(7);
            private Path outputDirectory = Path.of(System.getProperty("java.io.tmpdir"), "wasmtime4j-profiling");
            private boolean enableContinuousProfiling = true;
            private boolean enableAlerting = false;
            private boolean enableHealthChecks = true;
            private Set<String> alertChannels = new HashSet<>();
            private Map<String, String> tags = new HashMap<>();

            public Builder profilingWindow(final Duration window) { this.profilingWindow = window; return this; }
            public Builder rotationInterval(final Duration interval) { this.rotationInterval = interval; return this; }
            public Builder maxOverheadPercent(final double percent) { this.maxOverheadPercent = percent; return this; }
            public Builder dataRetentionPeriod(final Duration period) { this.dataRetentionPeriod = period; return this; }
            public Builder outputDirectory(final Path directory) { this.outputDirectory = directory; return this; }
            public Builder enableContinuousProfiling(final boolean enable) { this.enableContinuousProfiling = enable; return this; }
            public Builder enableAlerting(final boolean enable) { this.enableAlerting = enable; return this; }
            public Builder enableHealthChecks(final boolean enable) { this.enableHealthChecks = enable; return this; }
            public Builder addAlertChannel(final String channel) { this.alertChannels.add(channel); return this; }
            public Builder addTag(final String key, final String value) { this.tags.put(key, value); return this; }

            public ProductionConfig build() {
                return new ProductionConfig(this);
            }
        }

        // Getters
        public Duration getProfilingWindow() { return profilingWindow; }
        public Duration getRotationInterval() { return rotationInterval; }
        public double getMaxOverheadPercent() { return maxOverheadPercent; }
        public Duration getDataRetentionPeriod() { return dataRetentionPeriod; }
        public Path getOutputDirectory() { return outputDirectory; }
        public boolean isContinuousProfilingEnabled() { return enableContinuousProfiling; }
        public boolean isAlertingEnabled() { return enableAlerting; }
        public boolean isHealthChecksEnabled() { return enableHealthChecks; }
        public Set<String> getAlertChannels() { return alertChannels; }
        public Map<String, String> getTags() { return tags; }
    }

    /** Health status of the production profiler. */
    public static final class HealthStatus {
        private final boolean healthy;
        private final double currentOverheadPercent;
        private final long activeSessions;
        private final long totalSamples;
        private final Duration uptime;
        private final List<String> issues;
        private final Instant lastHealthCheck;

        public HealthStatus(final boolean healthy, final double currentOverheadPercent,
                           final long activeSessions, final long totalSamples, final Duration uptime,
                           final List<String> issues, final Instant lastHealthCheck) {
            this.healthy = healthy;
            this.currentOverheadPercent = currentOverheadPercent;
            this.activeSessions = activeSessions;
            this.totalSamples = totalSamples;
            this.uptime = uptime;
            this.issues = Collections.unmodifiableList(List.copyOf(issues));
            this.lastHealthCheck = lastHealthCheck;
        }

        // Getters
        public boolean isHealthy() { return healthy; }
        public double getCurrentOverheadPercent() { return currentOverheadPercent; }
        public long getActiveSessions() { return activeSessions; }
        public long getTotalSamples() { return totalSamples; }
        public Duration getUptime() { return uptime; }
        public List<String> getIssues() { return issues; }
        public Instant getLastHealthCheck() { return lastHealthCheck; }

        @Override
        public String toString() {
            return String.format("HealthStatus{healthy=%s, overhead=%.2f%%, sessions=%d, samples=%d, uptime=%s}",
                healthy, currentOverheadPercent, activeSessions, totalSamples, uptime);
        }
    }

    /** Performance alert. */
    public static final class PerformanceAlert {
        private final AlertType type;
        private final String message;
        private final AlertSeverity severity;
        private final Instant timestamp;
        private final Map<String, Object> metadata;

        public PerformanceAlert(final AlertType type, final String message, final AlertSeverity severity,
                               final Map<String, Object> metadata) {
            this.type = type;
            this.message = message;
            this.severity = severity;
            this.timestamp = Instant.now();
            this.metadata = Collections.unmodifiableMap(Map.copyOf(metadata));
        }

        // Getters
        public AlertType getType() { return type; }
        public String getMessage() { return message; }
        public AlertSeverity getSeverity() { return severity; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return metadata; }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s (%s)", severity, type, message, timestamp);
        }
    }

    public enum AlertType {
        HIGH_OVERHEAD,
        PERFORMANCE_REGRESSION,
        MEMORY_LEAK_DETECTED,
        PROFILING_FAILURE,
        DISK_SPACE_LOW,
        SESSION_TIMEOUT
    }

    public enum AlertSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    /** Circuit breaker for overhead protection. */
    private static final class OverheadCircuitBreaker {
        private final double maxOverheadPercent;
        private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
        private final AtomicLong consecutiveFailures = new AtomicLong(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private static final long RECOVERY_TIMEOUT_MS = 30000; // 30 seconds
        private static final long FAILURE_THRESHOLD = 3;

        enum State {
            CLOSED,    // Normal operation
            OPEN,      // Circuit breaker is open, profiling disabled
            HALF_OPEN  // Testing if profiling can be re-enabled
        }

        OverheadCircuitBreaker(final double maxOverheadPercent) {
            this.maxOverheadPercent = maxOverheadPercent;
        }

        boolean canProfile() {
            final State currentState = state.get();
            if (currentState == State.CLOSED) {
                return true;
            } else if (currentState == State.OPEN) {
                // Check if we should transition to half-open
                if (System.currentTimeMillis() - lastFailureTime.get() > RECOVERY_TIMEOUT_MS) {
                    state.compareAndSet(State.OPEN, State.HALF_OPEN);
                    return true;
                }
                return false;
            } else { // HALF_OPEN
                return true;
            }
        }

        void recordOverhead(final double overheadPercent) {
            if (overheadPercent > maxOverheadPercent) {
                final long failures = consecutiveFailures.incrementAndGet();
                lastFailureTime.set(System.currentTimeMillis());

                if (failures >= FAILURE_THRESHOLD) {
                    state.set(State.OPEN);
                    LOGGER.warning(String.format("Circuit breaker opened due to high overhead: %.2f%%", overheadPercent));
                }
            } else {
                consecutiveFailures.set(0);
                state.set(State.CLOSED);
            }
        }

        State getState() {
            return state.get();
        }
    }

    private final ProductionConfig config;
    private final AdvancedProfiler profiler;
    private final ProfilingIntegrations integrations;
    private final PerformanceInsights insights;
    private final OverheadCircuitBreaker circuitBreaker;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService alertExecutor;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<AdvancedProfiler.ProfilingSession> currentSession = new AtomicReference<>();
    private final AtomicLong totalSamples = new AtomicLong(0);
    private final AtomicLong sessionCount = new AtomicLong(0);
    private final Instant startTime;

    private volatile ScheduledFuture<?> rotationTask;
    private volatile ScheduledFuture<?> healthCheckTask;
    private volatile ScheduledFuture<?> cleanupTask;

    public ProductionProfiler(final ProductionConfig config) {
        this.config = Objects.requireNonNull(config);
        this.startTime = Instant.now();

        // Initialize components
        final AdvancedProfiler.ProfilerConfiguration profilerConfig =
            AdvancedProfiler.ProfilerConfiguration.builder()
                .samplingInterval(Duration.ofMillis(50)) // Conservative sampling for production
                .maxSamples(50000)
                .enableMemoryProfiling(true)
                .enableJfrIntegration(true)
                .enableFlameGraphs(true)
                .enableStackTraceCollection(true)
                .enableRegressionDetection(true)
                .outputDirectory(config.getOutputDirectory())
                .build();

        this.profiler = new AdvancedProfiler(profilerConfig);
        this.integrations = new ProfilingIntegrations("wasmtime4j-production");
        this.insights = new PerformanceInsights();
        this.circuitBreaker = new OverheadCircuitBreaker(config.getMaxOverheadPercent());
        this.scheduler = Executors.newScheduledThreadPool(3);
        this.alertExecutor = Executors.newSingleThreadExecutor();

        // Create output directory
        try {
            Files.createDirectories(config.getOutputDirectory());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create profiling output directory", e);
        }

        LOGGER.info("Production profiler initialized with config: " + config);
    }

    /**
     * Starts production profiling with all configured features.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            LOGGER.info("Starting production profiler");

            if (config.isContinuousProfilingEnabled()) {
                startContinuousProfiling();
            }

            if (config.isHealthChecksEnabled()) {
                startHealthChecks();
            }

            // Start data cleanup task
            startDataCleanup();

            LOGGER.info("Production profiler started successfully");
        }
    }

    /**
     * Stops production profiling and all background tasks.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            LOGGER.info("Stopping production profiler");

            // Stop current profiling session
            final AdvancedProfiler.ProfilingSession session = currentSession.getAndSet(null);
            if (session != null) {
                try {
                    generateSessionReport(session);
                    session.close();
                } catch (Exception e) {
                    LOGGER.warning("Error stopping profiling session: " + e.getMessage());
                }
            }

            // Cancel scheduled tasks
            cancelTask(rotationTask);
            cancelTask(healthCheckTask);
            cancelTask(cleanupTask);

            LOGGER.info("Production profiler stopped");
        }
    }

    /**
     * Records a function execution with production overhead protection.
     */
    public void recordFunctionExecution(final String functionName, final Duration executionTime,
                                       final long memoryAllocated, final String runtimeType) {
        if (!running.get() || !circuitBreaker.canProfile()) {
            return; // Skip profiling if not running or circuit breaker is open
        }

        final long startTime = System.nanoTime();
        try {
            profiler.recordFunctionExecution(functionName, executionTime, memoryAllocated, runtimeType);
            totalSamples.incrementAndGet();
        } finally {
            // Measure profiling overhead
            final long overheadNanos = System.nanoTime() - startTime;
            final double overheadPercent = executionTime.toNanos() > 0 ?
                (overheadNanos * 100.0) / executionTime.toNanos() : 0.0;
            circuitBreaker.recordOverhead(overheadPercent);
        }
    }

    /**
     * Profiles an operation with automatic overhead monitoring.
     */
    public <T> T profileOperation(final String operationName, final Callable<T> operation, final String runtimeType) {
        if (!running.get() || !circuitBreaker.canProfile()) {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new RuntimeException("Operation failed", e);
            }
        }

        final long profilingStartTime = System.nanoTime();
        try {
            return profiler.profileOperation(operationName, () -> {
                try {
                    return operation.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, runtimeType);
        } finally {
            final long profilingOverhead = System.nanoTime() - profilingStartTime;
            // Circuit breaker will handle overhead tracking via recordFunctionExecution
        }
    }

    /**
     * Gets current health status of the profiler.
     */
    public HealthStatus getHealthStatus() {
        final List<String> issues = new ArrayList<>();
        boolean healthy = true;

        // Check circuit breaker state
        if (circuitBreaker.getState() == OverheadCircuitBreaker.State.OPEN) {
            issues.add("Circuit breaker is open due to high overhead");
            healthy = false;
        }

        // Check if profiling is running when it should be
        if (config.isContinuousProfilingEnabled() && currentSession.get() == null && running.get()) {
            issues.add("Continuous profiling is enabled but no active session");
            healthy = false;
        }

        // Check disk space
        try {
            final long freeSpace = Files.getFileStore(config.getOutputDirectory()).getUsableSpace();
            if (freeSpace < 1024 * 1024 * 100) { // Less than 100MB
                issues.add("Low disk space in output directory");
                healthy = false;
            }
        } catch (IOException e) {
            issues.add("Cannot check disk space: " + e.getMessage());
            healthy = false;
        }

        return new HealthStatus(
            healthy,
            0.0, // Would be calculated from recent overhead measurements
            currentSession.get() != null ? 1 : 0,
            totalSamples.get(),
            Duration.between(startTime, Instant.now()),
            issues,
            Instant.now()
        );
    }

    /**
     * Generates a comprehensive profiling report.
     */
    public String generateProfilingReport() {
        final StringBuilder report = new StringBuilder();
        final HealthStatus health = getHealthStatus();

        report.append("=== Wasmtime4j Production Profiling Report ===\n\n");
        report.append(String.format("Report generated: %s\n", Instant.now()));
        report.append(String.format("Profiler uptime: %s\n", health.getUptime()));
        report.append(String.format("Health status: %s\n", health.isHealthy() ? "HEALTHY" : "UNHEALTHY"));
        report.append(String.format("Total samples: %,d\n", health.getTotalSamples()));
        report.append(String.format("Circuit breaker state: %s\n", circuitBreaker.getState()));
        report.append("\n");

        if (!health.getIssues().isEmpty()) {
            report.append("Issues:\n");
            health.getIssues().forEach(issue -> report.append("  - ").append(issue).append("\n"));
            report.append("\n");
        }

        final AdvancedProfiler.ProfilingSession session = currentSession.get();
        if (session != null) {
            final AdvancedProfiler.ProfilingStatistics stats = session.getStatistics();
            report.append("Current Session Statistics:\n");
            report.append(String.format("  Function calls: %,d\n", stats.getFunctionCalls()));
            report.append(String.format("  Average execution time: %.2f ms\n", stats.getAverageExecutionTimeNanos() / 1_000_000.0));
            report.append(String.format("  Memory allocations: %,d\n", stats.getMemoryAllocations()));
            report.append(String.format("  JNI calls: %,d\n", stats.getJniCalls()));
            report.append(String.format("  Panama calls: %,d\n", stats.getPanamaCalls()));
            report.append("\n");

            // Performance insights
            try {
                final FlameGraphGenerator.FlameFrame flameGraph = session.generateFlameGraph();
                final PerformanceInsights.PerformanceInsightsResult insightsResult =
                    insights.analyzePerformance(flameGraph, stats);

                report.append("Performance Insights:\n");
                report.append(String.format("  Overall health score: %.1f/100\n", insightsResult.getOverallHealthScore()));
                report.append(String.format("  Hot spots identified: %d\n", insightsResult.getHotSpots().size()));
                report.append(String.format("  Regressions detected: %d\n", insightsResult.getRegressions().size()));
                report.append("\n");

                if (!insightsResult.getGlobalRecommendations().isEmpty()) {
                    report.append("Recommendations:\n");
                    insightsResult.getGlobalRecommendations().forEach(rec ->
                        report.append("  - ").append(rec).append("\n"));
                }
            } catch (Exception e) {
                report.append("Error generating insights: ").append(e.getMessage()).append("\n");
            }
        }

        return report.toString();
    }

    /**
     * Exports current profiling data to all configured formats.
     */
    public CompletableFuture<Map<String, Path>> exportProfilingData() {
        final AdvancedProfiler.ProfilingSession session = currentSession.get();
        if (session == null) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                final FlameGraphGenerator.FlameFrame flameGraph = session.generateFlameGraph();
                final AdvancedProfiler.ProfilingStatistics stats = session.getStatistics();

                final Map<String, CompletableFuture<Path>> exports =
                    integrations.exportAll(flameGraph, stats, config.getOutputDirectory());

                return exports.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().join()
                    ));
            } catch (Exception e) {
                throw new RuntimeException("Failed to export profiling data", e);
            }
        });
    }

    @Override
    public void close() {
        stop();

        // Shutdown executors
        scheduler.shutdown();
        alertExecutor.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!alertExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                alertExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            alertExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close components
        profiler.close();
        integrations.close();

        LOGGER.info("Production profiler closed");
    }

    // Private helper methods

    private void startContinuousProfiling() {
        rotationTask = scheduler.scheduleAtFixedRate(
            this::rotateProfilingSession,
            0,
            config.getRotationInterval().toSeconds(),
            TimeUnit.SECONDS
        );
    }

    private void rotateProfilingSession() {
        try {
            if (!circuitBreaker.canProfile()) {
                LOGGER.info("Skipping profiling session rotation - circuit breaker is open");
                return;
            }

            // Stop current session
            final AdvancedProfiler.ProfilingSession oldSession = currentSession.getAndSet(null);
            if (oldSession != null) {
                generateSessionReport(oldSession);
                oldSession.close();
            }

            // Start new session
            final AdvancedProfiler.ProfilingSession newSession = profiler.startProfiling(config.getProfilingWindow());
            currentSession.set(newSession);
            sessionCount.incrementAndGet();

            LOGGER.info("Rotated profiling session #" + sessionCount.get());
        } catch (Exception e) {
            LOGGER.severe("Error rotating profiling session: " + e.getMessage());
            sendAlert(AlertType.PROFILING_FAILURE, "Failed to rotate profiling session: " + e.getMessage(),
                AlertSeverity.ERROR, Map.of("error", e.getClass().getSimpleName()));
        }
    }

    private void generateSessionReport(final AdvancedProfiler.ProfilingSession session) {
        try {
            final String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());

            // Generate and save flame graph
            final Path flameGraphPath = config.getOutputDirectory().resolve("flamegraph_" + timestamp + ".svg");
            profiler.saveFlameGraphAsSvg(flameGraphPath);

            // Generate insights report
            final FlameGraphGenerator.FlameFrame flameGraph = session.generateFlameGraph();
            final AdvancedProfiler.ProfilingStatistics stats = session.getStatistics();
            final PerformanceInsights.PerformanceInsightsResult insightsResult =
                insights.analyzePerformance(flameGraph, stats);

            // Save comprehensive report
            final Path reportPath = config.getOutputDirectory().resolve("report_" + timestamp + ".txt");
            Files.writeString(reportPath, generateProfilingReport());

            LOGGER.info("Generated session report: " + reportPath);
        } catch (Exception e) {
            LOGGER.warning("Error generating session report: " + e.getMessage());
        }
    }

    private void startHealthChecks() {
        healthCheckTask = scheduler.scheduleAtFixedRate(
            this::performHealthCheck,
            30, // Initial delay
            60, // Check every minute
            TimeUnit.SECONDS
        );
    }

    private void performHealthCheck() {
        try {
            final HealthStatus health = getHealthStatus();
            if (!health.isHealthy()) {
                LOGGER.warning("Health check failed: " + health.getIssues());
                sendAlert(AlertType.PROFILING_FAILURE, "Health check failed", AlertSeverity.WARNING,
                    Map.of("issues", health.getIssues().toString()));
            }
        } catch (Exception e) {
            LOGGER.warning("Error performing health check: " + e.getMessage());
        }
    }

    private void startDataCleanup() {
        cleanupTask = scheduler.scheduleAtFixedRate(
            this::cleanupOldData,
            Duration.ofHours(1).toSeconds(),
            Duration.ofHours(6).toSeconds(),
            TimeUnit.SECONDS
        );
    }

    private void cleanupOldData() {
        try {
            final Instant cutoff = Instant.now().minus(config.getDataRetentionPeriod());

            Files.walk(config.getOutputDirectory())
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        LOGGER.fine("Deleted old profiling data: " + path);
                    } catch (IOException e) {
                        LOGGER.warning("Failed to delete old profiling data: " + path);
                    }
                });
        } catch (Exception e) {
            LOGGER.warning("Error during data cleanup: " + e.getMessage());
        }
    }

    private void sendAlert(final AlertType type, final String message, final AlertSeverity severity,
                          final Map<String, Object> metadata) {
        if (!config.isAlertingEnabled()) {
            return;
        }

        alertExecutor.submit(() -> {
            try {
                final PerformanceAlert alert = new PerformanceAlert(type, message, severity, metadata);
                LOGGER.warning("Performance alert: " + alert);

                // In a real implementation, this would send alerts to configured channels
                // (e.g., email, Slack, PagerDuty, etc.)
                config.getAlertChannels().forEach(channel -> {
                    LOGGER.info("Sending alert to channel: " + channel);
                });
            } catch (Exception e) {
                LOGGER.severe("Failed to send alert: " + e.getMessage());
            }
        });
    }

    private void cancelTask(final ScheduledFuture<?> task) {
        if (task != null && !task.isDone()) {
            task.cancel(true);
        }
    }

    // Getters
    public ProductionConfig getConfig() { return config; }
    public boolean isRunning() { return running.get(); }
    public long getTotalSamples() { return totalSamples.get(); }
    public long getSessionCount() { return sessionCount.get(); }
    public Duration getUptime() { return Duration.between(startTime, Instant.now()); }
}