package ai.tegmentum.wasmtime4j.panama.diagnostics;

import ai.tegmentum.wasmtime4j.diagnostics.DiagnosticsCollector;
import ai.tegmentum.wasmtime4j.diagnostics.DiagnosticsEvent;
import ai.tegmentum.wasmtime4j.diagnostics.DiagnosticsLevel;
import ai.tegmentum.wasmtime4j.diagnostics.DiagnosticsReport;
import ai.tegmentum.wasmtime4j.diagnostics.SystemDiagnostics;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.exception.PanamaExceptionHandler;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.foreign.ValueLayout.*;

/**
 * Panama FFI implementation of comprehensive diagnostics collection for wasmtime4j.
 *
 * <p>This class provides Panama Foreign Function Interface-based diagnostics collection
 * with direct native integration, offering detailed error reporting, performance monitoring,
 * and system health diagnostics specifically optimized for Panama FFI-based WebAssembly operations.
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
public final class PanamaDiagnosticsCollector implements DiagnosticsCollector {

    private static final Logger LOGGER = Logger.getLogger(PanamaDiagnosticsCollector.class.getName());

    // Native function handles loaded via Panama FFI
    private static final MethodHandle CREATE_DIAGNOSTICS_COLLECTOR;
    private static final MethodHandle DESTROY_DIAGNOSTICS_COLLECTOR;
    private static final MethodHandle START_COLLECTION;
    private static final MethodHandle STOP_COLLECTION;
    private static final MethodHandle RECORD_EVENT;
    private static final MethodHandle RECORD_ERROR;
    private static final MethodHandle RECORD_PERFORMANCE_METRIC;
    private static final MethodHandle RECORD_RESOURCE_USAGE;
    private static final MethodHandle GENERATE_ANALYSIS;
    private static final MethodHandle CLEAR_EVENTS;
    private static final MethodHandle PERFORM_HEALTH_CHECK;

    // Memory layouts for native structures
    private static final GroupLayout DIAGNOSTICS_ANALYSIS_LAYOUT = MemoryLayout.structLayout(
        JAVA_DOUBLE.withName("average_execution_time"),
        JAVA_DOUBLE.withName("panama_call_overhead"),
        JAVA_DOUBLE.withName("memory_fragmentation"),
        JAVA_DOUBLE.withName("memory_usage_percent"),
        JAVA_DOUBLE.withName("thread_contention_rate"),
        JAVA_DOUBLE.withName("error_rate"),
        JAVA_INT.withName("native_crashes")
    );

    private static final GroupLayout HEALTH_CHECK_LAYOUT = MemoryLayout.structLayout(
        JAVA_DOUBLE.withName("overall_health"),
        JAVA_DOUBLE.withName("memory_health"),
        JAVA_DOUBLE.withName("thread_health"),
        JAVA_DOUBLE.withName("panama_health"),
        JAVA_LONG.withName("timestamp")
    );

    static {
        try {
            System.loadLibrary("wasmtime4j");
            final SymbolLookup nativeLib = SymbolLookup.loaderLookup();
            final Linker linker = Linker.nativeLinker();

            CREATE_DIAGNOSTICS_COLLECTOR = nativeLib.find("wasmtime4j_diagnostics_create")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.of(ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_diagnostics_create"));

            DESTROY_DIAGNOSTICS_COLLECTOR = nativeLib.find("wasmtime4j_diagnostics_destroy")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_diagnostics_destroy"));

            START_COLLECTION = nativeLib.find("wasmtime4j_diagnostics_start_collection")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_diagnostics_start_collection"));

            STOP_COLLECTION = nativeLib.find("wasmtime4j_diagnostics_stop_collection")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_diagnostics_stop_collection"));

            RECORD_EVENT = nativeLib.find("wasmtime4j_diagnostics_record_event")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT,
                        ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_diagnostics_record_event"));

            RECORD_ERROR = nativeLib.find("wasmtime4j_diagnostics_record_error")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS,
                        ADDRESS, ADDRESS, ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_diagnostics_record_error"));

            RECORD_PERFORMANCE_METRIC = nativeLib.find("wasmtime4j_diagnostics_record_performance_metric")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS,
                        ADDRESS, JAVA_DOUBLE, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_diagnostics_record_performance_metric"));

            RECORD_RESOURCE_USAGE = nativeLib.find("wasmtime4j_diagnostics_record_resource_usage")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS,
                        ADDRESS, JAVA_LONG, JAVA_LONG)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_diagnostics_record_resource_usage"));

            GENERATE_ANALYSIS = nativeLib.find("wasmtime4j_diagnostics_generate_analysis")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.of(JAVA_BOOLEAN,
                        ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_diagnostics_generate_analysis"));

            CLEAR_EVENTS = nativeLib.find("wasmtime4j_diagnostics_clear_events")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.ofVoid(ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_diagnostics_clear_events"));

            PERFORM_HEALTH_CHECK = nativeLib.find("wasmtime4j_diagnostics_perform_health_check")
                    .map(addr -> linker.downcallHandle(addr, FunctionDescriptor.of(JAVA_BOOLEAN,
                        ADDRESS, ADDRESS)))
                    .orElseThrow(() -> new UnsatisfiedLinkError("wasmtime4j_diagnostics_perform_health_check"));

            LOGGER.log(Level.FINE, "Loaded native diagnostics functions via Panama FFI");
        } catch (final Throwable e) {
            LOGGER.log(Level.SEVERE, "Failed to load native diagnostics library", e);
            throw new RuntimeException("Failed to load native diagnostics library", e);
        }
    }

    private final MemorySegment nativeDiagnosticsHandle;
    private final Arena arena;
    private final List<DiagnosticsEvent> events;
    private final Map<DiagnosticsLevel, AtomicLong> eventCounts;
    private final Map<String, Object> systemMetrics;
    private volatile boolean collecting = false;
    private volatile Instant collectionStartTime;
    private volatile boolean closed = false;

    /**
     * Creates a new Panama diagnostics collector.
     */
    public PanamaDiagnosticsCollector() {
        this.arena = Arena.ofConfined();
        this.events = new CopyOnWriteArrayList<>();
        this.eventCounts = new EnumMap<>(DiagnosticsLevel.class);
        this.systemMetrics = new ConcurrentHashMap<>();

        // Initialize event counters
        for (DiagnosticsLevel level : DiagnosticsLevel.values()) {
            eventCounts.put(level, new AtomicLong(0));
        }

        try {
            this.nativeDiagnosticsHandle = (MemorySegment) CREATE_DIAGNOSTICS_COLLECTOR.invokeExact();
            if (nativeDiagnosticsHandle.address() == 0L) {
                throw new WasmException("Failed to create native diagnostics collector");
            }

            LOGGER.log(Level.FINE, "Created Panama diagnostics collector with handle: {0}",
                      nativeDiagnosticsHandle.address());
        } catch (final Throwable e) {
            arena.close();
            throw PanamaExceptionHandler.wrapException(e, "Failed to create Panama diagnostics collector");
        }
    }

    @Override
    public void startCollection() {
        validateNotClosed();

        if (collecting) {
            throw new IllegalStateException("Diagnostics collection is already active");
        }

        try {
            final boolean started = (boolean) START_COLLECTION.invokeExact(nativeDiagnosticsHandle);
            if (!started) {
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

            LOGGER.log(Level.INFO, "Started Panama diagnostics collection");
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to start diagnostics collection");
        }
    }

    @Override
    public void stopCollection() {
        validateNotClosed();

        if (!collecting) {
            return;
        }

        try {
            STOP_COLLECTION.invokeExact(nativeDiagnosticsHandle);
            collecting = false;

            final Duration collectionDuration = Duration.between(collectionStartTime, Instant.now());
            LOGGER.log(Level.INFO, "Stopped Panama diagnostics collection after {0}", collectionDuration);
        } catch (final Throwable e) {
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
            final MemorySegment categoryStr = arena.allocateUtf8String(event.getCategory());
            final MemorySegment messageStr = arena.allocateUtf8String(event.getMessage());
            final MemorySegment threadNameStr = arena.allocateUtf8String(event.getThreadName());
            final MemorySegment contextStr = arena.allocateUtf8String(event.getContextMap().toString());

            RECORD_EVENT.invokeExact(nativeDiagnosticsHandle,
                event.getLevel().ordinal(),
                categoryStr,
                messageStr,
                event.getTimestamp().toEpochMilli(),
                threadNameStr,
                contextStr);

            LOGGER.log(Level.FINE, "Recorded diagnostics event: {0}", event);
        } catch (final Throwable e) {
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
            final MemorySegment categoryStr = arena.allocateUtf8String(category);
            final MemorySegment exceptionTypeStr = arena.allocateUtf8String(error.getClass().getName());
            final MemorySegment messageStr = arena.allocateUtf8String(error.getMessage());
            final MemorySegment stackTraceStr = arena.allocateUtf8String(getStackTraceString(error));

            RECORD_ERROR.invokeExact(nativeDiagnosticsHandle, categoryStr, exceptionTypeStr, messageStr, stackTraceStr);

            LOGGER.log(Level.FINE, "Recorded error diagnostics: {0}", error.getMessage());
        } catch (final Throwable e) {
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
            final MemorySegment nameStr = arena.allocateUtf8String(name);
            final MemorySegment unitStr = arena.allocateUtf8String(unit);

            RECORD_PERFORMANCE_METRIC.invokeExact(nativeDiagnosticsHandle, nameStr, value, unitStr);

            LOGGER.log(Level.FINE, "Recorded performance metric: {0} = {1} {2}",
                      new Object[]{name, value, unit});
        } catch (final Throwable e) {
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
            final MemorySegment resourceStr = arena.allocateUtf8String(resource);

            RECORD_RESOURCE_USAGE.invokeExact(nativeDiagnosticsHandle, resourceStr, used, total);

            LOGGER.log(Level.FINE, "Recorded resource usage: {0} = {1}/{2} ({3}%)",
                      new Object[]{resource, used, total, (double) used / total * 100.0});
        } catch (final Throwable e) {
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
            final PanamaDiagnosticsAnalysis panamaAnalysis = generateNativeAnalysis();

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
                .addRecommendation(generatePerformanceRecommendations(panamaAnalysis))
                .addRecommendation(generateResourceRecommendations(panamaAnalysis))
                .addRecommendation(generateErrorRecommendations(panamaAnalysis))
                .severity(calculateOverallSeverity())
                .build();

        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to generate diagnostics report");
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
            CLEAR_EVENTS.invokeExact(nativeDiagnosticsHandle);
            LOGGER.log(Level.FINE, "Cleared diagnostics events");
        } catch (final Throwable e) {
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

            if (nativeDiagnosticsHandle.address() != 0L) {
                DESTROY_DIAGNOSTICS_COLLECTOR.invokeExact(nativeDiagnosticsHandle);
            }

            arena.close();
            events.clear();
            systemMetrics.clear();
            closed = true;

            LOGGER.log(Level.FINE, "Closed Panama diagnostics collector");
        } catch (final Throwable e) {
            LOGGER.log(Level.WARNING, "Error closing diagnostics collector", e);
            closed = true; // Mark as closed even on error
        }
    }

    /**
     * Gets the native diagnostics handle.
     *
     * @return native handle
     */
    public MemorySegment getNativeHandle() {
        validateNotClosed();
        return nativeDiagnosticsHandle;
    }

    /**
     * Gets the memory arena for this collector.
     *
     * @return memory arena
     */
    public Arena getArena() {
        validateNotClosed();
        return arena;
    }

    /**
     * Performs a comprehensive health check of the Panama runtime.
     *
     * @return health check results
     */
    public PanamaHealthCheckResult performHealthCheck() {
        validateNotClosed();

        try {
            final MemorySegment healthCheckResult = arena.allocate(HEALTH_CHECK_LAYOUT);
            final boolean success = (boolean) PERFORM_HEALTH_CHECK.invokeExact(nativeDiagnosticsHandle, healthCheckResult);

            if (!success) {
                throw new WasmException("Failed to perform health check");
            }

            return new PanamaHealthCheckResult(
                healthCheckResult.get(JAVA_DOUBLE, 0),   // overall_health
                healthCheckResult.get(JAVA_DOUBLE, 8),   // memory_health
                healthCheckResult.get(JAVA_DOUBLE, 16),  // thread_health
                healthCheckResult.get(JAVA_DOUBLE, 24),  // panama_health
                healthCheckResult.get(JAVA_LONG, 32)     // timestamp
            );
        } catch (final Throwable e) {
            throw PanamaExceptionHandler.wrapException(e, "Failed to perform health check");
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

    private PanamaDiagnosticsAnalysis generateNativeAnalysis() throws Throwable {
        final MemorySegment analysisResult = arena.allocate(DIAGNOSTICS_ANALYSIS_LAYOUT);
        final boolean success = (boolean) GENERATE_ANALYSIS.invokeExact(nativeDiagnosticsHandle, analysisResult);

        if (!success) {
            throw new WasmException("Failed to generate native diagnostics analysis");
        }

        return new PanamaDiagnosticsAnalysis(
            analysisResult.get(JAVA_DOUBLE, 0),   // average_execution_time
            analysisResult.get(JAVA_DOUBLE, 8),   // panama_call_overhead
            analysisResult.get(JAVA_DOUBLE, 16),  // memory_fragmentation
            analysisResult.get(JAVA_DOUBLE, 24),  // memory_usage_percent
            analysisResult.get(JAVA_DOUBLE, 32),  // thread_contention_rate
            analysisResult.get(JAVA_DOUBLE, 40),  // error_rate
            analysisResult.get(JAVA_INT, 48)      // native_crashes
        );
    }

    private String getStackTraceString(final Throwable throwable) {
        final java.io.StringWriter sw = new java.io.StringWriter();
        final java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private List<String> generatePerformanceRecommendations(final PanamaDiagnosticsAnalysis analysis) {
        final List<String> recommendations = new ArrayList<>();

        if (analysis.averageExecutionTime > 100.0) {
            recommendations.add("Consider enabling compilation optimizations to reduce execution time");
        }

        if (analysis.panamaCallOverhead > 5.0) {
            recommendations.add("High Panama FFI call overhead detected - consider batching operations");
        }

        if (analysis.memoryFragmentation > 0.3) {
            recommendations.add("Memory fragmentation detected - consider arena management optimization");
        }

        return recommendations;
    }

    private List<String> generateResourceRecommendations(final PanamaDiagnosticsAnalysis analysis) {
        final List<String> recommendations = new ArrayList<>();

        if (analysis.memoryUsagePercent > 85.0) {
            recommendations.add("High memory usage detected - consider increasing heap size or optimizing memory arenas");
        }

        if (analysis.threadContentionRate > 0.1) {
            recommendations.add("High thread contention - consider reducing parallelism or improving coordination");
        }

        return recommendations;
    }

    private List<String> generateErrorRecommendations(final PanamaDiagnosticsAnalysis analysis) {
        final List<String> recommendations = new ArrayList<>();

        if (analysis.errorRate > 0.05) {
            recommendations.add("High error rate detected - review error logs for patterns");
        }

        if (analysis.nativeCrashes > 0) {
            recommendations.add("Native crashes detected - check for memory safety issues in Panama FFI calls");
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

    /**
     * Panama diagnostics analysis result.
     */
    public static final class PanamaDiagnosticsAnalysis {
        public final double averageExecutionTime;
        public final double panamaCallOverhead;
        public final double memoryFragmentation;
        public final double memoryUsagePercent;
        public final double threadContentionRate;
        public final double errorRate;
        public final int nativeCrashes;

        public PanamaDiagnosticsAnalysis(final double averageExecutionTime, final double panamaCallOverhead,
                                       final double memoryFragmentation, final double memoryUsagePercent,
                                       final double threadContentionRate, final double errorRate,
                                       final int nativeCrashes) {
            this.averageExecutionTime = averageExecutionTime;
            this.panamaCallOverhead = panamaCallOverhead;
            this.memoryFragmentation = memoryFragmentation;
            this.memoryUsagePercent = memoryUsagePercent;
            this.threadContentionRate = threadContentionRate;
            this.errorRate = errorRate;
            this.nativeCrashes = nativeCrashes;
        }
    }

    /**
     * Panama-specific health check result.
     */
    public static final class PanamaHealthCheckResult {
        private final double overallHealth;
        private final double memoryHealth;
        private final double threadHealth;
        private final double panamaHealth;
        private final Instant timestamp;

        public PanamaHealthCheckResult(final double overallHealth, final double memoryHealth,
                                     final double threadHealth, final double panamaHealth,
                                     final long timestamp) {
            this.overallHealth = overallHealth;
            this.memoryHealth = memoryHealth;
            this.threadHealth = threadHealth;
            this.panamaHealth = panamaHealth;
            this.timestamp = Instant.ofEpochMilli(timestamp);
        }

        public double getOverallHealth() { return overallHealth; }
        public double getMemoryHealth() { return memoryHealth; }
        public double getThreadHealth() { return threadHealth; }
        public double getPanamaHealth() { return panamaHealth; }
        public Instant getTimestamp() { return timestamp; }

        public boolean isHealthy() {
            return overallHealth > 0.8 && memoryHealth > 0.7 && threadHealth > 0.7 && panamaHealth > 0.8;
        }

        @Override
        public String toString() {
            return String.format("PanamaHealthCheckResult{overall=%.2f, memory=%.2f, thread=%.2f, panama=%.2f, healthy=%s}",
                overallHealth, memoryHealth, threadHealth, panamaHealth, isHealthy());
        }
    }
}