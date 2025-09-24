package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.profiling.AdvancedProfiler;
import ai.tegmentum.wasmtime4j.profiling.FlameGraphGenerator;
import ai.tegmentum.wasmtime4j.profiling.PerformanceInsights;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive benchmark to measure profiling overhead across different scenarios.
 *
 * <p>This benchmark measures the performance impact of profiling on various operations
 * to ensure that the profiling infrastructure doesn't significantly degrade application
 * performance in production environments.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class ProfilingOverheadBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        public AdvancedProfiler profiler;
        public AdvancedProfiler.ProfilingSession session;
        public FlameGraphGenerator flameGraphGenerator;
        public PerformanceInsights performanceInsights;

        @Setup(Level.Trial)
        public void setup() {
            final AdvancedProfiler.ProfilerConfiguration config =
                AdvancedProfiler.ProfilerConfiguration.builder()
                    .samplingInterval(Duration.ofMicroseconds(100)) // High frequency for overhead testing
                    .maxSamples(100000)
                    .enableMemoryProfiling(true)
                    .enableJfrIntegration(false) // Disable JFR for consistent results
                    .enableFlameGraphs(true)
                    .enableStackTraceCollection(true)
                    .build();

            profiler = new AdvancedProfiler(config);
            session = profiler.startProfiling(Duration.ofMinutes(10));
            flameGraphGenerator = new FlameGraphGenerator();
            performanceInsights = new PerformanceInsights();
        }

        @TearDown(Level.Trial)
        public void teardown() {
            if (session != null) {
                session.close();
            }
            if (profiler != null) {
                profiler.close();
            }
        }
    }

    /**
     * Baseline: Simple function call without any profiling.
     */
    @Benchmark
    public void baselineSimpleOperation(Blackhole blackhole) {
        final int result = performSimpleComputation(100);
        blackhole.consume(result);
    }

    /**
     * Simple operation with profiling enabled.
     */
    @Benchmark
    public void profiledSimpleOperation(BenchmarkState state, Blackhole blackhole) {
        final int result = state.profiler.profileOperation(
            "simple_computation",
            () -> performSimpleComputation(100),
            "JNI"
        );
        blackhole.consume(result);
    }

    /**
     * Baseline: Memory allocation without profiling.
     */
    @Benchmark
    public void baselineMemoryAllocation(Blackhole blackhole) {
        final byte[] data = new byte[1024];
        blackhole.consume(data);
    }

    /**
     * Memory allocation with profiling enabled.
     */
    @Benchmark
    public void profiledMemoryAllocation(BenchmarkState state, Blackhole blackhole) {
        final long allocationId = state.profiler.recordMemoryAllocation(1024, "benchmark_allocation");
        final byte[] data = new byte[1024];
        state.profiler.recordMemoryDeallocation(allocationId);
        blackhole.consume(data);
    }

    /**
     * Baseline: Nested function calls without profiling.
     */
    @Benchmark
    public void baselineNestedOperations(Blackhole blackhole) {
        final int result = performNestedComputations(5);
        blackhole.consume(result);
    }

    /**
     * Nested function calls with profiling enabled.
     */
    @Benchmark
    public void profiledNestedOperations(BenchmarkState state, Blackhole blackhole) {
        final int result = state.profiler.profileOperation("nested_root", () -> {
            return state.profiler.profileOperation("nested_level_1", () -> {
                return state.profiler.profileOperation("nested_level_2", () -> {
                    return state.profiler.profileOperation("nested_level_3", () -> {
                        return performSimpleComputation(50);
                    }, "JNI");
                }, "JNI");
            }, "JNI");
        }, "JNI");
        blackhole.consume(result);
    }

    /**
     * Baseline: Concurrent operations without profiling.
     */
    @Benchmark
    @Threads(4)
    public void baselineConcurrentOperations(Blackhole blackhole) {
        final int result = performSimpleComputation(Thread.currentThread().hashCode() % 100);
        blackhole.consume(result);
    }

    /**
     * Concurrent operations with profiling enabled.
     */
    @Benchmark
    @Threads(4)
    public void profiledConcurrentOperations(BenchmarkState state, Blackhole blackhole) {
        final String threadName = Thread.currentThread().getName();
        final int result = state.profiler.profileOperation(
            "concurrent_operation_" + threadName,
            () -> performSimpleComputation(Thread.currentThread().hashCode() % 100),
            "JNI"
        );
        blackhole.consume(result);
    }

    /**
     * Tests overhead of function execution recording.
     */
    @Benchmark
    public void functionExecutionRecording(BenchmarkState state, Blackhole blackhole) {
        state.profiler.recordFunctionExecution(
            "benchmark_function",
            Duration.ofNanos(1000),
            256,
            "JNI"
        );
        blackhole.consume(true);
    }

    /**
     * Tests overhead of memory tracking.
     */
    @Benchmark
    public void memoryTrackingOverhead(BenchmarkState state, Blackhole blackhole) {
        final long allocationId = state.profiler.recordMemoryAllocation(512, "benchmark_memory");
        state.profiler.recordMemoryDeallocation(allocationId);
        blackhole.consume(allocationId);
    }

    /**
     * Tests overhead of flame graph data collection.
     */
    @Benchmark
    public void flameGraphDataCollection(BenchmarkState state, Blackhole blackhole) {
        final long sampleId = state.flameGraphGenerator.recordSample(
            Duration.ofNanos(1500),
            java.util.Arrays.asList("function_a", "function_b", "function_c"),
            "benchmark_thread",
            java.util.Map.of("operation", "benchmark")
        );
        blackhole.consume(sampleId);
    }

    /**
     * Tests overhead of stack trace collection.
     */
    @Benchmark
    public void stackTraceCollection(BenchmarkState state, Blackhole blackhole) {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        state.profiler.recordFunctionExecution(
            "stack_trace_test",
            Duration.ofNanos(2000),
            0,
            "JNI"
        );
        blackhole.consume(stackTrace.length);
    }

    /**
     * Measures overhead of profiling statistics generation.
     */
    @Benchmark
    public void profilingStatisticsGeneration(BenchmarkState state, Blackhole blackhole) {
        final AdvancedProfiler.ProfilingStatistics stats = state.session.getStatistics();
        blackhole.consume(stats.getFunctionCalls());
    }

    /**
     * Tests performance insights analysis overhead.
     */
    @Benchmark
    public void performanceInsightsAnalysis(BenchmarkState state, Blackhole blackhole) {
        // Generate some sample data first
        state.profiler.recordFunctionExecution("insight_test", Duration.ofMillis(1), 1024, "JNI");

        final FlameGraphGenerator.FlameFrame flameGraph = state.session.generateFlameGraph();
        final AdvancedProfiler.ProfilingStatistics stats = state.session.getStatistics();

        final PerformanceInsights.PerformanceInsightsResult insights =
            state.performanceInsights.analyzePerformance(flameGraph, stats);

        blackhole.consume(insights.getHotSpots().size());
    }

    /**
     * Comprehensive profiling overhead test with all features enabled.
     */
    @Benchmark
    public void comprehensiveProfilingOverhead(BenchmarkState state, Blackhole blackhole) {
        // Record memory allocation
        final long allocationId = state.profiler.recordMemoryAllocation(1024, "comprehensive_test");

        // Profile nested operations
        final int result = state.profiler.profileOperation("comprehensive_root", () -> {
            return state.profiler.profileOperation("comprehensive_child", () -> {
                // Perform some computation
                int sum = 0;
                for (int i = 0; i < 100; i++) {
                    sum += i * i;
                }
                return sum;
            }, "JNI");
        }, "JNI");

        // Record deallocation
        state.profiler.recordMemoryDeallocation(allocationId);

        blackhole.consume(result);
    }

    /**
     * Tests overhead when profiling is disabled.
     */
    @Benchmark
    public void disabledProfilingOverhead() {
        // Create a profiler but don't start a session
        final AdvancedProfiler.ProfilerConfiguration config =
            AdvancedProfiler.ProfilerConfiguration.builder()
                .enableMemoryProfiling(false)
                .enableJfrIntegration(false)
                .enableFlameGraphs(false)
                .build();

        try (final AdvancedProfiler disabledProfiler = new AdvancedProfiler(config)) {
            // This should have minimal overhead since profiling is not active
            disabledProfiler.recordFunctionExecution("disabled_test", Duration.ofNanos(1000), 0, "JNI");
        }
    }

    // Helper methods for simulating work

    private int performSimpleComputation(final int iterations) {
        int result = 0;
        for (int i = 0; i < iterations; i++) {
            result += i * i;
        }
        return result;
    }

    private int performNestedComputations(final int depth) {
        if (depth <= 0) {
            return performSimpleComputation(10);
        }
        return performNestedComputations(depth - 1) + performSimpleComputation(depth);
    }

    /**
     * Custom benchmark to measure profiling overhead percentage.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void measureOverheadPercentage(BenchmarkState state, Blackhole blackhole) {
        // This benchmark will be compared with baselineSimpleOperation
        // to calculate the actual overhead percentage
        final int iterations = 1000;

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            state.profiler.profileOperation(
                "overhead_measurement",
                () -> performSimpleComputation(10),
                "JNI"
            );
        }
        long endTime = System.nanoTime();

        blackhole.consume(endTime - startTime);
    }

    /**
     * Benchmark for flame graph generation overhead.
     */
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3)
    public void flameGraphGenerationOverhead(BenchmarkState state, Blackhole blackhole) {
        // Generate a substantial amount of profiling data
        for (int i = 0; i < 1000; i++) {
            state.profiler.profileOperation("flamegraph_test_" + (i % 10), () -> {
                return performSimpleComputation(i % 50 + 10);
            }, "JNI");
        }

        // Measure flame graph generation time
        final FlameGraphGenerator.FlameFrame flameGraph = state.session.generateFlameGraph();
        blackhole.consume(flameGraph.getTotalTime().toNanos());
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(ProfilingOverheadBenchmark.class.getSimpleName())
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .jvmArgs("-server")
            .build();

        new Runner(opt).run();
    }
}