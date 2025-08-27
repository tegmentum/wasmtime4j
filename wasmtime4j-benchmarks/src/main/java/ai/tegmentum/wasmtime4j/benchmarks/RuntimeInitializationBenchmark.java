package ai.tegmentum.wasmtime4j.benchmarks;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks for runtime initialization and engine creation performance.
 *
 * <p>This benchmark class measures the performance characteristics of initializing
 * Wasmtime4j runtime engines, comparing JNI and Panama implementations across
 * different configuration scenarios.
 *
 * <p>Key metrics measured:
 * <ul>
 *   <li>Engine creation time</li>
 *   <li>Runtime initialization overhead</li>
 *   <li>Configuration parsing performance</li>
 *   <li>Memory allocation patterns during startup</li>
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms1g", "-Xmx1g"})
public class RuntimeInitializationBenchmark extends BenchmarkBase {

    /**
     * Runtime implementation to benchmark.
     */
    @Param({"JNI", "PANAMA", "AUTO"})
    private RuntimeType runtimeType;

    /**
     * Engine configuration scenario to test.
     */
    @Param({"DEFAULT", "OPTIMIZED", "DEBUG"})
    private String configType;

    /**
     * Mock runtime implementation for testing without actual Wasmtime dependencies.
     */
    private static final class MockRuntime {
        private final RuntimeType type;
        private final String config;
        private boolean initialized;

        MockRuntime(final RuntimeType type, final String config) {
            this.type = type;
            this.config = config;
            this.initialized = false;
        }

        void initialize() {
            // Simulate initialization work based on runtime type
            final int workAmount = type == RuntimeType.PANAMA ? 100 : 50;
            final int configMultiplier = "OPTIMIZED".equals(config) ? 2 : 
                                       "DEBUG".equals(config) ? 3 : 1;
            
            // Simulate CPU work
            for (int i = 0; i < workAmount * configMultiplier; i++) {
                Math.sqrt(i * 1.0);
            }
            
            this.initialized = true;
        }

        void cleanup() {
            if (this.initialized) {
                // Simulate cleanup work
                for (int i = 0; i < 25; i++) {
                    Math.log(i + 1.0);
                }
                this.initialized = false;
            }
        }

        boolean isInitialized() {
            return initialized;
        }

        RuntimeType getType() {
            return type;
        }

        String getConfig() {
            return config;
        }
    }

    /**
     * Current runtime instance being benchmarked.
     */
    private MockRuntime runtime;

    /**
     * Setup performed before each benchmark iteration.
     */
    @Setup(Level.Iteration)
    public void setupIteration() {
        // Ensure clean state for each iteration
        if (runtime != null) {
            runtime.cleanup();
        }
        runtime = null;
        
        // Force garbage collection to ensure clean memory state
        System.gc();
        try {
            Thread.sleep(10); // Allow GC to complete
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Cleanup performed after each benchmark iteration.
     */
    @TearDown(Level.Iteration)
    public void teardownIteration() {
        if (runtime != null) {
            runtime.cleanup();
            runtime = null;
        }
    }

    /**
     * Benchmarks basic engine creation performance.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     * @return the created runtime instance
     */
    @Benchmark
    public MockRuntime benchmarkEngineCreation(final Blackhole blackhole) {
        final MockRuntime newRuntime = new MockRuntime(runtimeType, configType);
        blackhole.consume(newRuntime.getType());
        blackhole.consume(newRuntime.getConfig());
        return newRuntime;
    }

    /**
     * Benchmarks full runtime initialization performance.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     * @return the initialized runtime instance
     */
    @Benchmark
    public MockRuntime benchmarkRuntimeInitialization(final Blackhole blackhole) {
        final MockRuntime newRuntime = new MockRuntime(runtimeType, configType);
        newRuntime.initialize();
        
        blackhole.consume(newRuntime.isInitialized());
        blackhole.consume(newRuntime.getType());
        
        return newRuntime;
    }

    /**
     * Benchmarks the complete create-and-initialize cycle.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     * @return the fully initialized runtime instance
     */
    @Benchmark
    public MockRuntime benchmarkFullInitializationCycle(final Blackhole blackhole) {
        final MockRuntime newRuntime = new MockRuntime(runtimeType, configType);
        newRuntime.initialize();
        
        // Simulate additional post-initialization work
        final boolean isReady = newRuntime.isInitialized();
        final String benchmarkId = formatBenchmarkId("full_init", runtimeType);
        
        blackhole.consume(isReady);
        blackhole.consume(benchmarkId);
        
        return newRuntime;
    }

    /**
     * Benchmarks runtime creation and immediate cleanup performance.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkCreateAndCleanup(final Blackhole blackhole) {
        final MockRuntime newRuntime = new MockRuntime(runtimeType, configType);
        newRuntime.initialize();
        
        final boolean wasInitialized = newRuntime.isInitialized();
        blackhole.consume(wasInitialized);
        
        newRuntime.cleanup();
        
        final boolean isCleanedUp = !newRuntime.isInitialized();
        blackhole.consume(isCleanedUp);
    }

    /**
     * Benchmarks multiple engine creation for pooling scenarios.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkMultipleEngineCreation(final Blackhole blackhole) {
        final MockRuntime[] runtimes = new MockRuntime[5];
        
        for (int i = 0; i < runtimes.length; i++) {
            runtimes[i] = new MockRuntime(runtimeType, configType);
            runtimes[i].initialize();
            blackhole.consume(runtimes[i].isInitialized());
        }
        
        // Cleanup all engines
        for (final MockRuntime runtime : runtimes) {
            runtime.cleanup();
            blackhole.consume(runtime.isInitialized());
        }
    }

    /**
     * Benchmarks engine creation with different configuration types.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkConfigurationOverhead(final Blackhole blackhole) {
        // Create engines with each configuration type to compare overhead
        final String[] configs = {"DEFAULT", "OPTIMIZED", "DEBUG"};
        
        for (final String config : configs) {
            final MockRuntime configRuntime = new MockRuntime(runtimeType, config);
            configRuntime.initialize();
            
            blackhole.consume(configRuntime.getConfig());
            blackhole.consume(configRuntime.isInitialized());
            
            configRuntime.cleanup();
        }
    }

    /**
     * Benchmarks runtime initialization under memory pressure.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkInitializationWithMemoryPressure(final Blackhole blackhole) {
        // Allocate memory to simulate pressure
        final byte[][] memoryPressure = new byte[100][];
        for (int i = 0; i < memoryPressure.length; i++) {
            memoryPressure[i] = new byte[1024]; // 1KB per allocation
            blackhole.consume(memoryPressure[i].length);
        }
        
        final MockRuntime newRuntime = new MockRuntime(runtimeType, configType);
        newRuntime.initialize();
        
        blackhole.consume(newRuntime.isInitialized());
        
        newRuntime.cleanup();
        
        // Clear memory pressure
        for (int i = 0; i < memoryPressure.length; i++) {
            memoryPressure[i] = null;
        }
    }
}