package ai.tegmentum.wasmtime4j.jni.diagnostics;

import ai.tegmentum.wasmtime4j.diagnostics.DiagnosticsCollector;
import ai.tegmentum.wasmtime4j.diagnostics.DiagnosticsEvent;
import ai.tegmentum.wasmtime4j.diagnostics.DiagnosticsLevel;
import ai.tegmentum.wasmtime4j.diagnostics.DiagnosticsReport;
import ai.tegmentum.wasmtime4j.diagnostics.SystemDiagnostics;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniExceptionHandler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of comprehensive diagnostics collection for wasmtime4j.
 *
 * <p>This class provides JNI-based diagnostics collection with native integration,
 * offering detailed error reporting, performance monitoring, and system health
 * diagnostics specifically optimized for JNI-based WebAssembly operations.
 *
 * <p>Features include:
 * <ul>
 *   <li>Comprehensive error tracking and categorization</li>
 *   <li>Performance bottleneck identification</li>
 *   <li>Resource usage monitoring</li>
 *   <li>Native crash reporting and analysis</li>
 *   <li>Historical diagnostics data collection</li>
 *   <li>Automated health checks and alerts</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniDiagnosticsCollector implements DiagnosticsCollector {

    private static final Logger LOGGER = Logger.getLogger(JniDiagnosticsCollector.class.getName());

    private final long nativeDiagnosticsHandle;
    private final List<DiagnosticsEvent> events;
    private final Map<DiagnosticsLevel, AtomicLong> eventCounts;
    private final Map<String, Object> systemMetrics;
    private volatile boolean collecting = false;
    private volatile Instant collectionStartTime;
    private volatile boolean closed = false;

    /**
     * Creates a new JNI diagnostics collector.
     */
    public JniDiagnosticsCollector() {
        this.events = new CopyOnWriteArrayList<>();
        this.eventCounts = new EnumMap<>(DiagnosticsLevel.class);
        this.systemMetrics = new ConcurrentHashMap<>();

        // Initialize event counters
        for (DiagnosticsLevel level : DiagnosticsLevel.values()) {
            eventCounts.put(level, new AtomicLong(0));
        }

        try {
            this.nativeDiagnosticsHandle = nativeCreateDiagnosticsCollector();
            if (nativeDiagnosticsHandle == 0) {
                throw new WasmException("Failed to create native diagnostics collector");
            }

            LOGGER.log(Level.FINE, "Created JNI diagnostics collector with handle: {0}", nativeDiagnosticsHandle);
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to create JNI diagnostics collector");
        }
    }

    @Override
    public void startCollection() {
        validateNotClosed();

        if (collecting) {
            throw new IllegalStateException("Diagnostics collection is already active");
        }

        try {
            if (!nativeStartCollection(nativeDiagnosticsHandle)) {
                throw new WasmException("Failed to start native diagnostics collection");
            }

            collecting = true;
            collectionStartTime = Instant.now();

            // Clear previous data
            events.clear();
            for (AtomicLong counter : eventCounts.values()) {
                counter.set(0);
            }
            systemMetrics.clear();

            LOGGER.log(Level.INFO, "Started JNI diagnostics collection");
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to start diagnostics collection");
        }
    }

    @Override
    public void stopCollection() {
        validateNotClosed();

        if (!collecting) {
            return;
        }

        try {
            nativeStopCollection(nativeDiagnosticsHandle);
            collecting = false;

            final Duration collectionDuration = Duration.between(collectionStartTime, Instant.now());
            LOGGER.log(Level.INFO, "Stopped JNI diagnostics collection after {0}", collectionDuration);
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error stopping diagnostics collection", e);
        }
    }

    @Override
    public boolean isCollecting() {
        return collecting && !closed;
    }

    @Override
    public void recordEvent(final DiagnosticsEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
        validateNotClosed();

        if (!collecting) {
            return;
        }

        try {
            // Record in Java layer
            events.add(event);
            eventCounts.get(event.getLevel()).incrementAndGet();

            // Record in native layer for enhanced analysis
            nativeRecordEvent(nativeDiagnosticsHandle,
                event.getLevel().ordinal(),
                event.getCategory(),
                event.getMessage(),
                event.getTimestamp().toEpochMilli(),
                event.getThreadName(),
                event.getContextMap().toString());

            LOGGER.log(Level.FINE, "Recorded diagnostics event: {0}", event);
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to record diagnostics event", e);
        }
    }

    @Override
    public void recordError(final String category, final Throwable error) {
        Objects.requireNonNull(category, "category cannot be null");
        Objects.requireNonNull(error, "error cannot be null");
        validateNotClosed();

        if (!collecting) {
            return;
        }

        try {
            // Create detailed error event
            final DiagnosticsEvent errorEvent = DiagnosticsEvent.builder()
                .level(DiagnosticsLevel.ERROR)
                .category(category)
                .message(error.getMessage())
                .timestamp(Instant.now())
                .threadName(Thread.currentThread().getName())
                .addContext("exception_type", error.getClass().getSimpleName())
                .addContext("stack_trace", getStackTraceString(error))
                .build();

            recordEvent(errorEvent);

            // Record in native layer with additional error context
            nativeRecordError(nativeDiagnosticsHandle,
                category,
                error.getClass().getName(),
                error.getMessage(),
                getStackTraceString(error));

            LOGGER.log(Level.FINE, "Recorded error diagnostics: {0}", error.getMessage());
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to record error diagnostics", e);
        }
    }

    @Override
    public void recordPerformanceMetric(final String name, final double value, final String unit) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(unit, "unit cannot be null");
        validateNotClosed();

        if (!collecting) {
            return;
        }

        try {
            // Record performance metric
            systemMetrics.put(name, value);

            // Record in native layer
            nativeRecordPerformanceMetric(nativeDiagnosticsHandle, name, value, unit);

            LOGGER.log(Level.FINE, "Recorded performance metric: {0} = {1} {2}",
                      new Object[]{name, value, unit});
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to record performance metric", e);
        }
    }

    @Override
    public void recordResourceUsage(final String resource, final long used, final long total) {
        Objects.requireNonNull(resource, "resource cannot be null");
        validateNotClosed();

        if (!collecting) {
            return;
        }

        try {
            // Record resource usage
            systemMetrics.put(resource + "_used", used);
            systemMetrics.put(resource + "_total", total);
            systemMetrics.put(resource + "_usage_percent", (double) used / total * 100.0);

            // Record in native layer
            nativeRecordResourceUsage(nativeDiagnosticsHandle, resource, used, total);

            LOGGER.log(Level.FINE, "Recorded resource usage: {0} = {1}/{2} ({3}%)",
                      new Object[]{resource, used, total, (double) used / total * 100.0});
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to record resource usage", e);
        }
    }

    @Override
    public DiagnosticsReport generateReport() {
        validateNotClosed();

        try {
            // Collect system diagnostics
            final SystemDiagnostics systemDiagnostics = collectSystemDiagnostics();

            // Get native diagnostics analysis
            final NativeDiagnosticsAnalysis nativeAnalysis = nativeGenerateAnalysis(nativeDiagnosticsHandle);

            // Calculate summary statistics
            final Map<DiagnosticsLevel, Long> eventCountsMap = new EnumMap<>(DiagnosticsLevel.class);
            for (Map.Entry<DiagnosticsLevel, AtomicLong> entry : eventCounts.entrySet()) {
                eventCountsMap.put(entry.getKey(), entry.getValue().get());
            }

            final Duration collectionDuration = collectionStartTime != null ?
                Duration.between(collectionStartTime, Instant.now()) : Duration.ZERO;

            return DiagnosticsReport.builder()
                .timestamp(Instant.now())
                .collectionDuration(collectionDuration)
                .events(List.copyOf(events))
                .eventCounts(eventCountsMap)
                .systemDiagnostics(systemDiagnostics)
                .systemMetrics(Map.copyOf(systemMetrics))
                .addRecommendation(generatePerformanceRecommendations(nativeAnalysis))
                .addRecommendation(generateResourceRecommendations(nativeAnalysis))
                .addRecommendation(generateErrorRecommendations(nativeAnalysis))
                .severity(calculateOverallSeverity())
                .build();

        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to generate diagnostics report");
        }
    }

    @Override
    public List<DiagnosticsEvent> getEvents() {
        return List.copyOf(events);
    }

    @Override
    public List<DiagnosticsEvent> getEvents(final DiagnosticsLevel level) {
        Objects.requireNonNull(level, "level cannot be null");

        return events.stream()
            .filter(event -> event.getLevel() == level)
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<DiagnosticsEvent> getEvents(final String category) {
        Objects.requireNonNull(category, "category cannot be null");

        return events.stream()
            .filter(event -> category.equals(event.getCategory()))
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public long getEventCount(final DiagnosticsLevel level) {
        Objects.requireNonNull(level, "level cannot be null");
        return eventCounts.get(level).get();
    }

    @Override
    public void clearEvents() {
        validateNotClosed();

        events.clear();
        for (AtomicLong counter : eventCounts.values()) {
            counter.set(0);
        }
        systemMetrics.clear();

        try {
            nativeClearEvents(nativeDiagnosticsHandle);
            LOGGER.log(Level.FINE, "Cleared diagnostics events");
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to clear native diagnostics events", e);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        try {
            stopCollection();

            if (nativeDiagnosticsHandle != 0) {
                nativeDestroyDiagnosticsCollector(nativeDiagnosticsHandle);
            }

            events.clear();
            systemMetrics.clear();
            closed = true;

            LOGGER.log(Level.FINE, "Closed JNI diagnostics collector");
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error closing diagnostics collector", e);
            closed = true; // Mark as closed even on error
        }
    }

    /**
     * Gets the native diagnostics handle.
     *
     * @return native handle
     */
    public long getNativeHandle() {
        validateNotClosed();
        return nativeDiagnosticsHandle;
    }

    /**
     * Performs a comprehensive health check of the JNI runtime.
     *
     * @return health check results
     */
    public JniHealthCheckResult performHealthCheck() {
        validateNotClosed();

        try {
            final NativeHealthCheckResult nativeResult = nativePerformHealthCheck(nativeDiagnosticsHandle);

            return new JniHealthCheckResult(
                nativeResult.overallHealth,
                nativeResult.memoryHealth,
                nativeResult.threadHealth,
                nativeResult.jniHealth,
                nativeResult.recommendations,
                nativeResult.warnings,
                nativeResult.timestamp
            );
        } catch (final Exception e) {
            throw JniExceptionHandler.wrapException(e, "Failed to perform health check");
        }
    }

    // Private helper methods

    private void validateNotClosed() {
        if (closed) {
            throw new IllegalStateException("Diagnostics collector is closed");
        }
    }

    private SystemDiagnostics collectSystemDiagnostics() {
        final Runtime runtime = Runtime.getRuntime();
        final long totalMemory = runtime.totalMemory();
        final long freeMemory = runtime.freeMemory();
        final long usedMemory = totalMemory - freeMemory;
        final long maxMemory = runtime.maxMemory();

        return SystemDiagnostics.builder()
            .javaVersion(System.getProperty("java.version"))
            .jvmName(System.getProperty("java.vm.name"))
            .osName(System.getProperty("os.name"))
            .osVersion(System.getProperty("os.version"))
            .osArch(System.getProperty("os.arch"))
            .availableProcessors(runtime.availableProcessors())
            .totalMemory(totalMemory)
            .freeMemory(freeMemory)
            .usedMemory(usedMemory)
            .maxMemory(maxMemory)
            .memoryUsagePercent((double) usedMemory / maxMemory * 100.0)
            .timestamp(Instant.now())
            .build();
    }

    private String getStackTraceString(final Throwable throwable) {
        final java.io.StringWriter sw = new java.io.StringWriter();
        final java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private List<String> generatePerformanceRecommendations(final NativeDiagnosticsAnalysis analysis) {
        final List<String> recommendations = new ArrayList<>();

        if (analysis.averageExecutionTime > 100.0) {
            recommendations.add("Consider enabling compilation optimizations to reduce execution time");
        }

        if (analysis.jniCallOverhead > 10.0) {
            recommendations.add("High JNI call overhead detected - consider batching operations");
        }

        if (analysis.memoryFragmentation > 0.3) {
            recommendations.add("Memory fragmentation detected - consider periodic garbage collection");
        }

        return recommendations;
    }

    private List<String> generateResourceRecommendations(final NativeDiagnosticsAnalysis analysis) {
        final List<String> recommendations = new ArrayList<>();

        if (analysis.memoryUsagePercent > 85.0) {
            recommendations.add("High memory usage detected - consider increasing heap size");
        }

        if (analysis.threadContentionRate > 0.1) {
            recommendations.add("High thread contention - consider reducing parallelism or improving synchronization");
        }

        return recommendations;
    }

    private List<String> generateErrorRecommendations(final NativeDiagnosticsAnalysis analysis) {
        final List<String> recommendations = new ArrayList<>();

        if (analysis.errorRate > 0.05) {
            recommendations.add("High error rate detected - review error logs for patterns");
        }

        if (analysis.nativeCrashes > 0) {
            recommendations.add("Native crashes detected - check for memory safety issues");
        }

        return recommendations;
    }

    private DiagnosticsLevel calculateOverallSeverity() {
        final long errorCount = eventCounts.get(DiagnosticsLevel.ERROR).get();
        final long warningCount = eventCounts.get(DiagnosticsLevel.WARNING).get();
        final long totalCount = events.size();

        if (errorCount > 0 || (totalCount > 0 && (double) errorCount / totalCount > 0.1)) {
            return DiagnosticsLevel.ERROR;
        } else if (warningCount > 0 || (totalCount > 0 && (double) warningCount / totalCount > 0.3)) {
            return DiagnosticsLevel.WARNING;
        } else {
            return DiagnosticsLevel.INFO;
        }
    }

    // Native method declarations

    private static native long nativeCreateDiagnosticsCollector();
    private static native void nativeDestroyDiagnosticsCollector(long handle);
    private static native boolean nativeStartCollection(long handle);
    private static native void nativeStopCollection(long handle);
    private static native void nativeRecordEvent(long handle, int level, String category, String message,
                                               long timestamp, String threadName, String context);
    private static native void nativeRecordError(long handle, String category, String exceptionType,
                                               String message, String stackTrace);
    private static native void nativeRecordPerformanceMetric(long handle, String name, double value, String unit);
    private static native void nativeRecordResourceUsage(long handle, String resource, long used, long total);
    private static native NativeDiagnosticsAnalysis nativeGenerateAnalysis(long handle);
    private static native void nativeClearEvents(long handle);
    private static native NativeHealthCheckResult nativePerformHealthCheck(long handle);

    /**
     * Native diagnostics analysis result.
     */
    public static final class NativeDiagnosticsAnalysis {
        public final double averageExecutionTime;
        public final double jniCallOverhead;
        public final double memoryFragmentation;
        public final double memoryUsagePercent;
        public final double threadContentionRate;
        public final double errorRate;
        public final int nativeCrashes;

        public NativeDiagnosticsAnalysis(final double averageExecutionTime, final double jniCallOverhead,
                                       final double memoryFragmentation, final double memoryUsagePercent,
                                       final double threadContentionRate, final double errorRate,
                                       final int nativeCrashes) {
            this.averageExecutionTime = averageExecutionTime;
            this.jniCallOverhead = jniCallOverhead;
            this.memoryFragmentation = memoryFragmentation;
            this.memoryUsagePercent = memoryUsagePercent;
            this.threadContentionRate = threadContentionRate;
            this.errorRate = errorRate;
            this.nativeCrashes = nativeCrashes;
        }
    }

    /**
     * Native health check result.
     */
    public static final class NativeHealthCheckResult {
        public final double overallHealth;
        public final double memoryHealth;
        public final double threadHealth;
        public final double jniHealth;
        public final String[] recommendations;
        public final String[] warnings;
        public final long timestamp;

        public NativeHealthCheckResult(final double overallHealth, final double memoryHealth,
                                     final double threadHealth, final double jniHealth,
                                     final String[] recommendations, final String[] warnings,
                                     final long timestamp) {
            this.overallHealth = overallHealth;
            this.memoryHealth = memoryHealth;
            this.threadHealth = threadHealth;
            this.jniHealth = jniHealth;
            this.recommendations = recommendations;
            this.warnings = warnings;
            this.timestamp = timestamp;
        }
    }

    /**
     * JNI-specific health check result.
     */
    public static final class JniHealthCheckResult {
        private final double overallHealth;
        private final double memoryHealth;
        private final double threadHealth;
        private final double jniHealth;
        private final String[] recommendations;
        private final String[] warnings;
        private final Instant timestamp;

        public JniHealthCheckResult(final double overallHealth, final double memoryHealth,
                                  final double threadHealth, final double jniHealth,
                                  final String[] recommendations, final String[] warnings,
                                  final long timestamp) {
            this.overallHealth = overallHealth;
            this.memoryHealth = memoryHealth;
            this.threadHealth = threadHealth;
            this.jniHealth = jniHealth;
            this.recommendations = recommendations;
            this.warnings = warnings;
            this.timestamp = Instant.ofEpochMilli(timestamp);
        }

        public double getOverallHealth() { return overallHealth; }
        public double getMemoryHealth() { return memoryHealth; }
        public double getThreadHealth() { return threadHealth; }
        public double getJniHealth() { return jniHealth; }
        public List<String> getRecommendations() { return List.of(recommendations); }
        public List<String> getWarnings() { return List.of(warnings); }
        public Instant getTimestamp() { return timestamp; }

        public boolean isHealthy() {
            return overallHealth > 0.8 && memoryHealth > 0.7 && threadHealth > 0.7 && jniHealth > 0.8;
        }

        @Override
        public String toString() {
            return String.format("JniHealthCheckResult{overall=%.2f, memory=%.2f, thread=%.2f, jni=%.2f, healthy=%s}",
                overallHealth, memoryHealth, threadHealth, jniHealth, isHealthy());
        }
    }

    // Static initialization
    static {
        try {
            System.loadLibrary("wasmtime4j");
            LOGGER.log(Level.FINE, "Loaded native library for JNI diagnostics collector");
        } catch (final UnsatisfiedLinkError e) {
            LOGGER.log(Level.SEVERE, "Failed to load native library", e);
            throw new RuntimeException("Failed to load native diagnostics library", e);
        }
    }
}