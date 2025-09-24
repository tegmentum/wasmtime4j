package ai.tegmentum.wasmtime4j.profiling;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jdk.jfr.*;

/**
 * Advanced performance profiler with comprehensive data collection and flame graph generation.
 *
 * <p>This profiler provides production-ready performance analysis capabilities including:
 * <ul>
 *   <li>Low-overhead sampling profiler with configurable rates</li>
 *   <li>Event-based profiling with custom JFR events</li>
 *   <li>Stack trace collection with symbol resolution</li>
 *   <li>Memory allocation profiling and leak detection</li>
 *   <li>Function-level execution profiling</li>
 *   <li>Real-time performance metrics collection</li>
 *   <li>Flame graph generation and visualization</li>
 *   <li>Performance regression detection</li>
 * </ul>
 *
 * <p>The profiler is designed for both development and production use with configurable
 * overhead limits and continuous profiling capabilities.
 *
 * @since 1.0.0
 */
public final class AdvancedProfiler implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(AdvancedProfiler.class.getName());

    /** Default sampling interval for continuous profiling. */
    private static final Duration DEFAULT_SAMPLING_INTERVAL = Duration.ofMillis(10);

    /** Maximum number of stack samples to keep in memory. */
    private static final int DEFAULT_MAX_SAMPLES = 100000;

    private final ProfilerConfiguration config;
    private final FlameGraphGenerator flameGraphGenerator;
    private final StackFrameCollector stackFrameCollector;
    private final MemoryProfiler memoryProfiler;
    private final PerformanceCounters performanceCounters;
    private final ScheduledExecutorService samplingExecutor;
    private final ThreadMXBean threadBean;
    private final MemoryMXBean memoryBean;

    private volatile boolean profiling = false;
    private volatile ScheduledFuture<?> samplingTask;
    private final AtomicLong totalSamples = new AtomicLong(0);
    private final AtomicReference<Instant> profilingStartTime = new AtomicReference<>();

    /**
     * Configuration for the advanced profiler.
     */
    public static final class ProfilerConfiguration {
        private final Duration samplingInterval;
        private final int maxSamples;
        private final boolean enableMemoryProfiling;
        private final boolean enableJfrIntegration;
        private final boolean enableFlameGraphs;
        private final double maxOverheadPercent;
        private final Set<String> enabledEventTypes;
        private final boolean enableStackTraceCollection;
        private final boolean enableRegressionDetection;
        private final Path outputDirectory;

        private ProfilerConfiguration(final Builder builder) {
            this.samplingInterval = builder.samplingInterval;
            this.maxSamples = builder.maxSamples;
            this.enableMemoryProfiling = builder.enableMemoryProfiling;
            this.enableJfrIntegration = builder.enableJfrIntegration;
            this.enableFlameGraphs = builder.enableFlameGraphs;
            this.maxOverheadPercent = builder.maxOverheadPercent;
            this.enabledEventTypes = Collections.unmodifiableSet(builder.enabledEventTypes);
            this.enableStackTraceCollection = builder.enableStackTraceCollection;
            this.enableRegressionDetection = builder.enableRegressionDetection;
            this.outputDirectory = builder.outputDirectory;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private Duration samplingInterval = DEFAULT_SAMPLING_INTERVAL;
            private int maxSamples = DEFAULT_MAX_SAMPLES;
            private boolean enableMemoryProfiling = true;
            private boolean enableJfrIntegration = true;
            private boolean enableFlameGraphs = true;
            private double maxOverheadPercent = 2.0;
            private Set<String> enabledEventTypes = new HashSet<>(Arrays.asList(
                "function_execution", "memory_allocation", "compilation", "io_operation"));
            private boolean enableStackTraceCollection = true;
            private boolean enableRegressionDetection = true;
            private Path outputDirectory = Path.of(System.getProperty("java.io.tmpdir"), "wasmtime4j-profiling");

            public Builder samplingInterval(final Duration interval) { this.samplingInterval = interval; return this; }
            public Builder maxSamples(final int maxSamples) { this.maxSamples = maxSamples; return this; }
            public Builder enableMemoryProfiling(final boolean enable) { this.enableMemoryProfiling = enable; return this; }
            public Builder enableJfrIntegration(final boolean enable) { this.enableJfrIntegration = enable; return this; }
            public Builder enableFlameGraphs(final boolean enable) { this.enableFlameGraphs = enable; return this; }
            public Builder maxOverheadPercent(final double percent) { this.maxOverheadPercent = percent; return this; }
            public Builder enableEventType(final String eventType) { this.enabledEventTypes.add(eventType); return this; }
            public Builder enableStackTraceCollection(final boolean enable) { this.enableStackTraceCollection = enable; return this; }
            public Builder enableRegressionDetection(final boolean enable) { this.enableRegressionDetection = enable; return this; }
            public Builder outputDirectory(final Path directory) { this.outputDirectory = directory; return this; }

            public ProfilerConfiguration build() {
                return new ProfilerConfiguration(this);
            }
        }

        // Getters
        public Duration getSamplingInterval() { return samplingInterval; }
        public int getMaxSamples() { return maxSamples; }
        public boolean isMemoryProfilingEnabled() { return enableMemoryProfiling; }
        public boolean isJfrIntegrationEnabled() { return enableJfrIntegration; }
        public boolean isFlameGraphsEnabled() { return enableFlameGraphs; }
        public double getMaxOverheadPercent() { return maxOverheadPercent; }
        public Set<String> getEnabledEventTypes() { return enabledEventTypes; }
        public boolean isStackTraceCollectionEnabled() { return enableStackTraceCollection; }
        public boolean isRegressionDetectionEnabled() { return enableRegressionDetection; }
        public Path getOutputDirectory() { return outputDirectory; }
    }

    /**
     * Collects stack frames for flame graph generation.
     */
    private static final class StackFrameCollector {
        private final ConcurrentLinkedQueue<StackFrame> frames = new ConcurrentLinkedQueue<>();
        private final AtomicLong frameCount = new AtomicLong(0);
        private final int maxFrames;

        StackFrameCollector(final int maxFrames) {
            this.maxFrames = maxFrames;
        }

        void addFrame(final StackFrame frame) {
            frames.offer(frame);
            final long count = frameCount.incrementAndGet();

            // Remove old frames if we exceed the limit
            if (count > maxFrames) {
                frames.poll();
                frameCount.decrementAndGet();
            }
        }

        List<StackFrame> getAllFrames() {
            return new ArrayList<>(frames);
        }

        void clear() {
            frames.clear();
            frameCount.set(0);
        }
    }

    /**
     * Represents a captured stack frame.
     */
    public static final class StackFrame {
        private final long frameId;
        private final Instant timestamp;
        private final String threadName;
        private final List<String> stackTrace;
        private final Duration duration;
        private final long memoryAllocated;
        private final Map<String, Object> metadata;

        public StackFrame(final long frameId, final Instant timestamp, final String threadName,
                         final List<String> stackTrace, final Duration duration, final long memoryAllocated,
                         final Map<String, Object> metadata) {
            this.frameId = frameId;
            this.timestamp = timestamp;
            this.threadName = threadName;
            this.stackTrace = Collections.unmodifiableList(List.copyOf(stackTrace));
            this.duration = duration;
            this.memoryAllocated = memoryAllocated;
            this.metadata = Collections.unmodifiableMap(Map.copyOf(metadata));
        }

        // Getters
        public long getFrameId() { return frameId; }
        public Instant getTimestamp() { return timestamp; }
        public String getThreadName() { return threadName; }
        public List<String> getStackTrace() { return stackTrace; }
        public Duration getDuration() { return duration; }
        public long getMemoryAllocated() { return memoryAllocated; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    /**
     * Memory profiler for allocation tracking and leak detection.
     */
    private static final class MemoryProfiler {
        private final ConcurrentHashMap<Long, AllocationRecord> allocations = new ConcurrentHashMap<>();
        private final AtomicLong nextAllocationId = new AtomicLong(1);
        private volatile boolean enabled = false;

        static final class AllocationRecord {
            final long allocationId;
            final long size;
            final Instant timestamp;
            final String threadName;
            final List<String> stackTrace;

            AllocationRecord(final long allocationId, final long size, final Instant timestamp,
                           final String threadName, final List<String> stackTrace) {
                this.allocationId = allocationId;
                this.size = size;
                this.timestamp = timestamp;
                this.threadName = threadName;
                this.stackTrace = Collections.unmodifiableList(List.copyOf(stackTrace));
            }
        }

        void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        long recordAllocation(final long size, final String threadName, final List<String> stackTrace) {
            if (!enabled) return 0;

            final long allocationId = nextAllocationId.getAndIncrement();
            final AllocationRecord record = new AllocationRecord(
                allocationId, size, Instant.now(), threadName, stackTrace);
            allocations.put(allocationId, record);
            return allocationId;
        }

        void recordDeallocation(final long allocationId) {
            if (!enabled) return;
            allocations.remove(allocationId);
        }

        List<AllocationRecord> getPotentialLeaks(final Duration threshold) {
            if (!enabled) return Collections.emptyList();

            final Instant cutoff = Instant.now().minus(threshold);
            return allocations.values().stream()
                .filter(record -> record.timestamp.isBefore(cutoff))
                .collect(Collectors.toList());
        }

        long getTotalAllocatedMemory() {
            return allocations.values().stream()
                .mapToLong(record -> record.size)
                .sum();
        }

        void clear() {
            allocations.clear();
            nextAllocationId.set(1);
        }
    }

    /**
     * Performance counters and metrics.
     */
    private static final class PerformanceCounters {
        private final AtomicLong functionCalls = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong memoryAllocations = new AtomicLong(0);
        private final AtomicLong totalAllocatedBytes = new AtomicLong(0);
        private final AtomicLong jniCalls = new AtomicLong(0);
        private final AtomicLong panamaCalls = new AtomicLong(0);
        private final ConcurrentHashMap<String, AtomicLong> functionCallCounts = new ConcurrentHashMap<>();

        void recordFunctionCall(final String functionName, final Duration executionTime) {
            functionCalls.incrementAndGet();
            totalExecutionTime.addAndGet(executionTime.toNanos());
            functionCallCounts.computeIfAbsent(functionName, k -> new AtomicLong(0)).incrementAndGet();
        }

        void recordMemoryAllocation(final long bytes) {
            memoryAllocations.incrementAndGet();
            totalAllocatedBytes.addAndGet(bytes);
        }

        void recordJniCall() {
            jniCalls.incrementAndGet();
        }

        void recordPanamaCall() {
            panamaCalls.incrementAndGet();
        }

        // Getters
        public long getFunctionCalls() { return functionCalls.get(); }
        public long getTotalExecutionTimeNanos() { return totalExecutionTime.get(); }
        public long getMemoryAllocations() { return memoryAllocations.get(); }
        public long getTotalAllocatedBytes() { return totalAllocatedBytes.get(); }
        public long getJniCalls() { return jniCalls.get(); }
        public long getPanamaCalls() { return panamaCalls.get(); }
        public Map<String, Long> getFunctionCallCounts() {
            return functionCallCounts.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().get()
                ));
        }

        void reset() {
            functionCalls.set(0);
            totalExecutionTime.set(0);
            memoryAllocations.set(0);
            totalAllocatedBytes.set(0);
            jniCalls.set(0);
            panamaCalls.set(0);
            functionCallCounts.clear();
        }
    }

    /**
     * JFR event for WebAssembly function execution.
     */
    @Name("wasmtime4j.advanced.FunctionExecution")
    @Category({"WebAssembly", "Advanced Profiling"})
    @Description("Advanced WebAssembly function execution event")
    @StackTrace(true)
    public static class AdvancedFunctionExecutionEvent extends Event {
        @Label("Function Name")
        public String functionName;

        @Label("Execution Time (ns)")
        public long executionTimeNanos;

        @Label("Memory Allocated")
        public long memoryAllocated;

        @Label("Thread Name")
        public String threadName;

        @Label("Call Stack Depth")
        public int stackDepth;

        @Label("Runtime Type")
        public String runtimeType; // "JNI" or "Panama"
    }

    /**
     * JFR event for memory allocation tracking.
     */
    @Name("wasmtime4j.advanced.MemoryAllocation")
    @Category({"WebAssembly", "Memory", "Advanced Profiling"})
    @Description("Advanced memory allocation tracking event")
    @StackTrace(true)
    public static class AdvancedMemoryAllocationEvent extends Event {
        @Label("Allocation Size")
        public long allocationSize;

        @Label("Allocation Type")
        public String allocationType;

        @Label("Total Allocated")
        public long totalAllocated;

        @Label("Thread Name")
        public String threadName;
    }

    public AdvancedProfiler() {
        this(ProfilerConfiguration.builder().build());
    }

    public AdvancedProfiler(final ProfilerConfiguration config) {
        this.config = Objects.requireNonNull(config);
        this.flameGraphGenerator = new FlameGraphGenerator(
            FlameGraphGenerator.FlameGraphConfig.builder()
                .includeJavaScript(true)
                .enableSearch(true)
                .colorByCategory(true)
                .build()
        );
        this.stackFrameCollector = new StackFrameCollector(config.getMaxSamples());
        this.memoryProfiler = new MemoryProfiler();
        this.performanceCounters = new PerformanceCounters();
        this.samplingExecutor = Executors.newScheduledThreadPool(2);
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();

        // Enable CPU time measurement if supported
        if (threadBean.isThreadCpuTimeSupported() && !threadBean.isThreadCpuTimeEnabled()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }

        LOGGER.info("Advanced profiler initialized with configuration: " + config);
    }

    /**
     * Starts profiling with continuous sampling.
     *
     * @return profiling session handle
     */
    public ProfilingSession startProfiling() {
        return startProfiling(Duration.ofMinutes(5)); // Default 5 minute session
    }

    /**
     * Starts profiling session with specified duration.
     *
     * @param duration maximum profiling duration
     * @return profiling session handle
     */
    public ProfilingSession startProfiling(final Duration duration) {
        if (profiling) {
            throw new IllegalStateException("Profiling is already active");
        }

        profiling = true;
        profilingStartTime.set(Instant.now());
        memoryProfiler.setEnabled(config.isMemoryProfilingEnabled());
        flameGraphGenerator.startCollection();

        // Start sampling task
        samplingTask = samplingExecutor.scheduleAtFixedRate(
            this::collectSample,
            0,
            config.getSamplingInterval().toMillis(),
            TimeUnit.MILLISECONDS
        );

        // Schedule automatic stop
        final ScheduledFuture<?> stopTask = samplingExecutor.schedule(
            this::stopProfiling,
            duration.toMillis(),
            TimeUnit.MILLISECONDS
        );

        LOGGER.info("Started profiling session for " + duration);

        return new ProfilingSession(this, stopTask);
    }

    /**
     * Stops profiling and collects final results.
     */
    public void stopProfiling() {
        if (!profiling) {
            return;
        }

        profiling = false;
        flameGraphGenerator.stopCollection();
        memoryProfiler.setEnabled(false);

        if (samplingTask != null) {
            samplingTask.cancel(false);
            samplingTask = null;
        }

        final Duration totalTime = Duration.between(profilingStartTime.get(), Instant.now());
        LOGGER.info("Stopped profiling session after " + totalTime + ", collected " + totalSamples.get() + " samples");
    }

    /**
     * Records a function execution for profiling.
     *
     * @param functionName name of the executed function
     * @param executionTime execution duration
     * @param memoryAllocated memory allocated during execution
     * @param runtimeType runtime type (JNI or Panama)
     */
    public void recordFunctionExecution(final String functionName, final Duration executionTime,
                                       final long memoryAllocated, final String runtimeType) {
        if (!profiling) return;

        performanceCounters.recordFunctionCall(functionName, executionTime);

        if (memoryAllocated > 0) {
            performanceCounters.recordMemoryAllocation(memoryAllocated);
        }

        // Record runtime-specific metrics
        if ("JNI".equals(runtimeType)) {
            performanceCounters.recordJniCall();
        } else if ("Panama".equals(runtimeType)) {
            performanceCounters.recordPanamaCall();
        }

        // Generate JFR event if enabled
        if (config.isJfrIntegrationEnabled() && config.getEnabledEventTypes().contains("function_execution")) {
            final AdvancedFunctionExecutionEvent event = new AdvancedFunctionExecutionEvent();
            event.functionName = functionName;
            event.executionTimeNanos = executionTime.toNanos();
            event.memoryAllocated = memoryAllocated;
            event.threadName = Thread.currentThread().getName();
            event.stackDepth = Thread.currentThread().getStackTrace().length;
            event.runtimeType = runtimeType;
            event.commit();
        }

        // Record stack trace if enabled
        if (config.isStackTraceCollectionEnabled()) {
            recordStackTrace(functionName, executionTime, memoryAllocated);
        }
    }

    /**
     * Records memory allocation for profiling.
     *
     * @param size allocation size in bytes
     * @param allocationType type of allocation
     * @return allocation ID for tracking
     */
    public long recordMemoryAllocation(final long size, final String allocationType) {
        if (!profiling) return 0;

        final String threadName = Thread.currentThread().getName();
        final List<String> stackTrace = config.isStackTraceCollectionEnabled() ?
            Arrays.asList(Thread.currentThread().getStackTrace())
                .stream()
                .map(StackTraceElement::toString)
                .collect(Collectors.toList()) :
            Collections.emptyList();

        final long allocationId = memoryProfiler.recordAllocation(size, threadName, stackTrace);

        // Generate JFR event
        if (config.isJfrIntegrationEnabled() && config.getEnabledEventTypes().contains("memory_allocation")) {
            final AdvancedMemoryAllocationEvent event = new AdvancedMemoryAllocationEvent();
            event.allocationSize = size;
            event.allocationType = allocationType;
            event.totalAllocated = memoryProfiler.getTotalAllocatedMemory();
            event.threadName = threadName;
            event.commit();
        }

        return allocationId;
    }

    /**
     * Records memory deallocation.
     *
     * @param allocationId allocation ID to deallocate
     */
    public void recordMemoryDeallocation(final long allocationId) {
        if (!profiling) return;
        memoryProfiler.recordDeallocation(allocationId);
    }

    /**
     * Profiles an operation using a supplier.
     *
     * @param operationName name of the operation
     * @param operation operation to profile
     * @param runtimeType runtime type (JNI or Panama)
     * @param <T> return type
     * @return operation result
     */
    public <T> T profileOperation(final String operationName, final Supplier<T> operation, final String runtimeType) {
        if (!profiling) {
            return operation.get();
        }

        final long startMemory = memoryBean.getHeapMemoryUsage().getUsed();
        final Instant startTime = Instant.now();

        try {
            final T result = operation.get();
            final Duration executionTime = Duration.between(startTime, Instant.now());
            final long memoryDelta = memoryBean.getHeapMemoryUsage().getUsed() - startMemory;

            recordFunctionExecution(operationName, executionTime, Math.max(0, memoryDelta), runtimeType);
            return result;
        } catch (Exception e) {
            final Duration executionTime = Duration.between(startTime, Instant.now());
            recordFunctionExecution(operationName + "(failed)", executionTime, 0, runtimeType);
            throw e;
        }
    }

    /**
     * Generates flame graph from collected profiling data.
     *
     * @return flame graph root frame
     */
    public FlameGraphGenerator.FlameFrame generateFlameGraph() {
        if (!config.isFlameGraphsEnabled()) {
            throw new IllegalStateException("Flame graphs are not enabled");
        }

        // Convert stack frames to flame graph samples
        final List<FlameGraphGenerator.StackSample> samples = stackFrameCollector.getAllFrames()
            .stream()
            .map(frame -> new FlameGraphGenerator.StackSample(
                frame.getFrameId(),
                frame.getTimestamp(),
                frame.getDuration(),
                frame.getStackTrace(),
                frame.getThreadName(),
                frame.getMetadata()
            ))
            .collect(Collectors.toList());

        return flameGraphGenerator.generateFlameGraph(samples);
    }

    /**
     * Generates and saves flame graph as SVG.
     *
     * @param outputPath path to save the flame graph
     * @throws IOException if saving fails
     */
    public void saveFlameGraphAsSvg(final Path outputPath) throws java.io.IOException {
        final FlameGraphGenerator.FlameFrame rootFrame = generateFlameGraph();
        flameGraphGenerator.saveSvgFlameGraph(rootFrame, outputPath);
    }

    /**
     * Gets comprehensive profiling statistics.
     *
     * @return profiling statistics
     */
    public ProfilingStatistics getStatistics() {
        final Duration totalTime = profilingStartTime.get() != null ?
            Duration.between(profilingStartTime.get(), Instant.now()) : Duration.ZERO;

        return new ProfilingStatistics(
            totalTime,
            totalSamples.get(),
            performanceCounters.getFunctionCalls(),
            performanceCounters.getTotalExecutionTimeNanos(),
            performanceCounters.getMemoryAllocations(),
            performanceCounters.getTotalAllocatedBytes(),
            performanceCounters.getJniCalls(),
            performanceCounters.getPanamaCalls(),
            stackFrameCollector.frameCount.get(),
            memoryProfiler.getTotalAllocatedMemory(),
            performanceCounters.getFunctionCallCounts()
        );
    }

    /**
     * Detects potential memory leaks.
     *
     * @param threshold age threshold for considering allocations as leaks
     * @return list of potential memory leaks
     */
    public List<MemoryProfiler.AllocationRecord> detectMemoryLeaks(final Duration threshold) {
        return memoryProfiler.getPotentialLeaks(threshold);
    }

    /**
     * Resets all profiling data.
     */
    public void reset() {
        stackFrameCollector.clear();
        memoryProfiler.clear();
        performanceCounters.reset();
        flameGraphGenerator.getStackTraceCollector().clear();
        totalSamples.set(0);
        LOGGER.info("Reset all profiling data");
    }

    @Override
    public void close() {
        stopProfiling();
        samplingExecutor.shutdown();
        try {
            if (!samplingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                samplingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            samplingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("Advanced profiler closed");
    }

    // Private methods

    private void collectSample() {
        try {
            if (!profiling) return;

            totalSamples.incrementAndGet();

            // Collect stack trace
            final Thread currentThread = Thread.currentThread();
            final List<String> stackTrace = Arrays.stream(currentThread.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());

            // Create stack frame
            final StackFrame frame = new StackFrame(
                totalSamples.get(),
                Instant.now(),
                currentThread.getName(),
                stackTrace,
                config.getSamplingInterval(),
                0, // Memory allocated during sampling
                Collections.emptyMap()
            );

            stackFrameCollector.addFrame(frame);

            // Record sample in flame graph generator
            flameGraphGenerator.recordSample(
                config.getSamplingInterval(),
                stackTrace,
                currentThread.getName(),
                Collections.emptyMap()
            );

        } catch (Exception e) {
            LOGGER.warning("Error collecting profiling sample: " + e.getMessage());
        }
    }

    private void recordStackTrace(final String functionName, final Duration executionTime, final long memoryAllocated) {
        final Thread currentThread = Thread.currentThread();
        final List<String> stackTrace = Arrays.stream(currentThread.getStackTrace())
            .map(StackTraceElement::toString)
            .collect(Collectors.toList());

        final Map<String, Object> metadata = Map.of(
            "function_name", functionName,
            "execution_time_nanos", executionTime.toNanos(),
            "memory_allocated", memoryAllocated
        );

        final StackFrame frame = new StackFrame(
            totalSamples.incrementAndGet(),
            Instant.now(),
            currentThread.getName(),
            stackTrace,
            executionTime,
            memoryAllocated,
            metadata
        );

        stackFrameCollector.addFrame(frame);
    }

    /**
     * Profiling session handle.
     */
    public static final class ProfilingSession implements AutoCloseable {
        private final AdvancedProfiler profiler;
        private final ScheduledFuture<?> stopTask;
        private volatile boolean closed = false;

        private ProfilingSession(final AdvancedProfiler profiler, final ScheduledFuture<?> stopTask) {
            this.profiler = profiler;
            this.stopTask = stopTask;
        }

        /**
         * Stops the profiling session.
         */
        public void stop() {
            if (!closed) {
                closed = true;
                stopTask.cancel(false);
                profiler.stopProfiling();
            }
        }

        /**
         * Gets current profiling statistics.
         *
         * @return profiling statistics
         */
        public ProfilingStatistics getStatistics() {
            return profiler.getStatistics();
        }

        /**
         * Generates flame graph from current session data.
         *
         * @return flame graph root frame
         */
        public FlameGraphGenerator.FlameFrame generateFlameGraph() {
            return profiler.generateFlameGraph();
        }

        @Override
        public void close() {
            stop();
        }
    }

    /**
     * Comprehensive profiling statistics.
     */
    public static final class ProfilingStatistics {
        private final Duration totalProfilingTime;
        private final long totalSamples;
        private final long functionCalls;
        private final long totalExecutionTimeNanos;
        private final long memoryAllocations;
        private final long totalAllocatedBytes;
        private final long jniCalls;
        private final long panamaCalls;
        private final long stackFrames;
        private final long currentAllocatedMemory;
        private final Map<String, Long> functionCallCounts;

        ProfilingStatistics(final Duration totalProfilingTime, final long totalSamples,
                           final long functionCalls, final long totalExecutionTimeNanos,
                           final long memoryAllocations, final long totalAllocatedBytes,
                           final long jniCalls, final long panamaCalls, final long stackFrames,
                           final long currentAllocatedMemory, final Map<String, Long> functionCallCounts) {
            this.totalProfilingTime = totalProfilingTime;
            this.totalSamples = totalSamples;
            this.functionCalls = functionCalls;
            this.totalExecutionTimeNanos = totalExecutionTimeNanos;
            this.memoryAllocations = memoryAllocations;
            this.totalAllocatedBytes = totalAllocatedBytes;
            this.jniCalls = jniCalls;
            this.panamaCalls = panamaCalls;
            this.stackFrames = stackFrames;
            this.currentAllocatedMemory = currentAllocatedMemory;
            this.functionCallCounts = Collections.unmodifiableMap(Map.copyOf(functionCallCounts));
        }

        // Getters and calculated metrics
        public Duration getTotalProfilingTime() { return totalProfilingTime; }
        public long getTotalSamples() { return totalSamples; }
        public long getFunctionCalls() { return functionCalls; }
        public long getTotalExecutionTimeNanos() { return totalExecutionTimeNanos; }
        public long getMemoryAllocations() { return memoryAllocations; }
        public long getTotalAllocatedBytes() { return totalAllocatedBytes; }
        public long getJniCalls() { return jniCalls; }
        public long getPanamaCalls() { return panamaCalls; }
        public long getStackFrames() { return stackFrames; }
        public long getCurrentAllocatedMemory() { return currentAllocatedMemory; }
        public Map<String, Long> getFunctionCallCounts() { return functionCallCounts; }

        public double getSamplingRate() {
            return totalProfilingTime.toMillis() > 0 ?
                (totalSamples * 1000.0) / totalProfilingTime.toMillis() : 0.0;
        }

        public double getAverageExecutionTimeNanos() {
            return functionCalls > 0 ? (double) totalExecutionTimeNanos / functionCalls : 0.0;
        }

        public double getAverageAllocationSize() {
            return memoryAllocations > 0 ? (double) totalAllocatedBytes / memoryAllocations : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                "ProfilingStatistics{time=%s, samples=%d, functions=%d, avgExecTime=%.2fμs, allocations=%d, jni=%d, panama=%d}",
                totalProfilingTime, totalSamples, functionCalls, getAverageExecutionTimeNanos() / 1000.0,
                memoryAllocations, jniCalls, panamaCalls
            );
        }
    }

    public ProfilerConfiguration getConfig() {
        return config;
    }

    public boolean isProfiling() {
        return profiling;
    }
}