package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.jni.JniExperimentalFeatures;
import ai.tegmentum.wasmtime4j.panama.PanamaExperimentalFeatures;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Performance benchmarks for experimental WebAssembly features.
 *
 * <p>This benchmark suite measures the performance impact and overhead of
 * enabling experimental features compared to standard WebAssembly execution.
 *
 * <p>The benchmarks cover:
 * <ul>
 *   <li>Configuration creation and setup overhead</li>
 *   <li>Memory usage with experimental features enabled</li>
 *   <li>Runtime performance comparison</li>
 *   <li>Cross-runtime performance (JNI vs Panama)</li>
 * </ul>
 *
 * <p><strong>Note:</strong> Since most experimental features are not yet
 * supported by Wasmtime, these benchmarks primarily measure configuration
 * overhead and setup costs.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class ExperimentalFeaturesPerformanceBenchmark {

    private static final Logger logger = Logger.getLogger(ExperimentalFeaturesPerformanceBenchmark.class.getName());

    // Benchmark state for configuration objects
    @State(Scope.Benchmark)
    public static class ConfigurationState {
        public EngineConfig standardConfig;
        public EngineConfig experimentalConfig;
        public EngineConfig allExperimentalConfig;

        @Setup(Level.Trial)
        public void setup() {
            logger.info("Setting up experimental features benchmark configurations");

            // Standard configuration
            standardConfig = new EngineConfig()
                .addWasmFeature(WasmFeature.SIMD)
                .addWasmFeature(WasmFeature.MULTI_VALUE)
                .addWasmFeature(WasmFeature.BULK_MEMORY)
                .optimizationLevel(EngineConfig.OptimizationLevel.SPEED);

            // Experimental configuration with selective features
            experimentalConfig = new EngineConfig()
                .addWasmFeature(WasmFeature.SIMD)
                .addWasmFeature(WasmFeature.MULTI_VALUE)
                .addWasmFeature(WasmFeature.BULK_MEMORY)
                .addWasmFeature(WasmFeature.STACK_SWITCHING)
                .addWasmFeature(WasmFeature.CALL_CC)
                .addWasmFeature(WasmFeature.EXTENDED_CONST_EXPRESSIONS)
                .optimizationLevel(EngineConfig.OptimizationLevel.SPEED);

            // All experimental features configuration
            allExperimentalConfig = EngineConfig.forExperimentalFeatures();

            logger.info("Experimental features benchmark setup completed");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            logger.info("Tearing down experimental features benchmark configurations");
        }
    }

    // Benchmark state for JNI experimental features
    @State(Scope.Thread)
    public static class JniExperimentalState {
        public JniExperimentalFeatures standardExperimentalConfig;
        public JniExperimentalFeatures allExperimentalConfig;

        @Setup(Level.Iteration)
        public void setup() {
            standardExperimentalConfig = new JniExperimentalFeatures();
            allExperimentalConfig = JniExperimentalFeatures.allExperimentalEnabled();
        }

        @TearDown(Level.Iteration)
        public void tearDown() {
            if (standardExperimentalConfig != null) {
                standardExperimentalConfig.dispose();
            }
            if (allExperimentalConfig != null) {
                allExperimentalConfig.dispose();
            }
        }
    }

    // Benchmark state for Panama experimental features
    @State(Scope.Thread)
    public static class PanamaExperimentalState {
        public PanamaExperimentalFeatures standardExperimentalConfig;
        public PanamaExperimentalFeatures allExperimentalConfig;

        @Setup(Level.Iteration)
        public void setup() {
            standardExperimentalConfig = new PanamaExperimentalFeatures();
            allExperimentalConfig = PanamaExperimentalFeatures.allExperimentalEnabled();
        }

        @TearDown(Level.Iteration)
        public void tearDown() {
            if (standardExperimentalConfig != null) {
                standardExperimentalConfig.close();
            }
            if (allExperimentalConfig != null) {
                allExperimentalConfig.close();
            }
        }
    }

    /**
     * Baseline benchmark: Engine creation with standard configuration.
     */
    @Benchmark
    public void baselineEngineCreation(ConfigurationState state, Blackhole bh) {
        try (WasmRuntime runtime = WasmRuntimeFactory.createRuntime(state.standardConfig)) {
            bh.consume(runtime);
        } catch (Exception e) {
            // Expected for some configurations that might not be supported
            bh.consume(e);
        }
    }

    /**
     * Benchmark: Engine creation with experimental features enabled.
     */
    @Benchmark
    public void experimentalEngineCreation(ConfigurationState state, Blackhole bh) {
        try (WasmRuntime runtime = WasmRuntimeFactory.createRuntime(state.experimentalConfig)) {
            bh.consume(runtime);
        } catch (Exception e) {
            // Expected for experimental features that might not be supported
            bh.consume(e);
        }
    }

    /**
     * Benchmark: Engine creation with all experimental features enabled.
     */
    @Benchmark
    public void allExperimentalEngineCreation(ConfigurationState state, Blackhole bh) {
        try (WasmRuntime runtime = WasmRuntimeFactory.createRuntime(state.allExperimentalConfig)) {
            bh.consume(runtime);
        } catch (Exception e) {
            // Expected for experimental features that might not be supported
            bh.consume(e);
        }
    }

    /**
     * Benchmark: JNI experimental features configuration creation.
     */
    @Benchmark
    public void jniExperimentalConfigCreation(Blackhole bh) {
        try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
            bh.consume(config);
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /**
     * Benchmark: JNI all-experimental features configuration creation.
     */
    @Benchmark
    public void jniAllExperimentalConfigCreation(Blackhole bh) {
        try (JniExperimentalFeatures config = JniExperimentalFeatures.allExperimentalEnabled()) {
            bh.consume(config);
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /**
     * Benchmark: Panama experimental features configuration creation.
     */
    @Benchmark
    public void panamaExperimentalConfigCreation(Blackhole bh) {
        try (PanamaExperimentalFeatures config = new PanamaExperimentalFeatures()) {
            bh.consume(config);
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /**
     * Benchmark: Panama all-experimental features configuration creation.
     */
    @Benchmark
    public void panamaAllExperimentalConfigCreation(Blackhole bh) {
        try (PanamaExperimentalFeatures config = PanamaExperimentalFeatures.allExperimentalEnabled()) {
            bh.consume(config);
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /**
     * Benchmark: JNI stack switching configuration overhead.
     */
    @Benchmark
    public void jniStackSwitchingConfiguration(JniExperimentalState state, Blackhole bh) {
        try {
            JniExperimentalFeatures config = state.standardExperimentalConfig;
            config.enableStackSwitching(64 * 1024, 100);
            bh.consume(config.isValid());
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /**
     * Benchmark: JNI call/cc configuration overhead.
     */
    @Benchmark
    public void jniCallCcConfiguration(JniExperimentalState state, Blackhole bh) {
        try {
            JniExperimentalFeatures config = state.standardExperimentalConfig;
            config.enableCallCc(1000, JniExperimentalFeatures.ContinuationStorageStrategy.HYBRID);
            bh.consume(config.isValid());
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /**
     * Benchmark: JNI extended constant expressions configuration overhead.
     */
    @Benchmark
    public void jniExtendedConstExpressionsConfiguration(JniExperimentalState state, Blackhole bh) {
        try {
            JniExperimentalFeatures config = state.standardExperimentalConfig;
            config.enableExtendedConstExpressions(
                true,
                true,
                JniExperimentalFeatures.ConstantFoldingLevel.AGGRESSIVE
            );
            bh.consume(config.isValid());
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /**
     * Benchmark: Panama stack switching configuration overhead.
     */
    @Benchmark
    public void panamaStackSwitchingConfiguration(PanamaExperimentalState state, Blackhole bh) {
        try {
            PanamaExperimentalFeatures config = state.standardExperimentalConfig;
            config.enableStackSwitching(64 * 1024, 100);
            bh.consume(config.isValid());
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /**
     * Benchmark: Panama call/cc configuration overhead.
     */
    @Benchmark
    public void panamaCallCcConfiguration(PanamaExperimentalState state, Blackhole bh) {
        try {
            PanamaExperimentalFeatures config = state.standardExperimentalConfig;
            config.enableCallCc(1000, PanamaExperimentalFeatures.ContinuationStorageStrategy.HYBRID);
            bh.consume(config.isValid());
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /**
     * Benchmark: Panama extended constant expressions configuration overhead.
     */
    @Benchmark
    public void panamaExtendedConstExpressionsConfiguration(PanamaExperimentalState state, Blackhole bh) {
        try {
            PanamaExperimentalFeatures config = state.standardExperimentalConfig;
            config.enableExtendedConstExpressions(
                true,
                true,
                PanamaExperimentalFeatures.ConstantFoldingLevel.AGGRESSIVE
            );
            bh.consume(config.isValid());
        } catch (Exception e) {
            bh.consume(e);
        }
    }

    /**
     * Benchmark: Configuration feature set manipulation performance.
     */
    @Benchmark
    public void configurationFeatureManipulation(Blackhole bh) {
        EngineConfig config = new EngineConfig();

        // Add experimental features one by one
        config.addWasmFeature(WasmFeature.STACK_SWITCHING);
        config.addWasmFeature(WasmFeature.CALL_CC);
        config.addWasmFeature(WasmFeature.EXTENDED_CONST_EXPRESSIONS);
        config.addWasmFeature(WasmFeature.MEMORY64_EXTENDED);
        config.addWasmFeature(WasmFeature.CUSTOM_PAGE_SIZES);
        config.addWasmFeature(WasmFeature.SHARED_EVERYTHING_THREADS);
        config.addWasmFeature(WasmFeature.TYPE_IMPORTS);
        config.addWasmFeature(WasmFeature.STRING_IMPORTS);
        config.addWasmFeature(WasmFeature.RESOURCE_TYPES);
        config.addWasmFeature(WasmFeature.INTERFACE_TYPES);
        config.addWasmFeature(WasmFeature.FLEXIBLE_VECTORS);

        // Check feature states
        boolean stackSwitching = config.isWasmStackSwitching();
        boolean callCc = config.isWasmCallCc();
        boolean extendedConst = config.isWasmExtendedConstExpressions();
        boolean memory64Extended = config.isWasmMemory64Extended();
        boolean customPageSizes = config.isWasmCustomPageSizes();
        boolean sharedEverythingThreads = config.isWasmSharedEverythingThreads();
        boolean typeImports = config.isWasmTypeImports();
        boolean stringImports = config.isWasmStringImports();
        boolean resourceTypes = config.isWasmResourceTypes();
        boolean interfaceTypes = config.isWasmInterfaceTypes();
        boolean flexibleVectors = config.isWasmFlexibleVectors();

        bh.consume(stackSwitching);
        bh.consume(callCc);
        bh.consume(extendedConst);
        bh.consume(memory64Extended);
        bh.consume(customPageSizes);
        bh.consume(sharedEverythingThreads);
        bh.consume(typeImports);
        bh.consume(stringImports);
        bh.consume(resourceTypes);
        bh.consume(interfaceTypes);
        bh.consume(flexibleVectors);
    }

    /**
     * Benchmark: Cross-runtime experimental features comparison.
     */
    @Benchmark
    public void crossRuntimeExperimentalFeaturesComparison(Blackhole bh) {
        // Compare JNI and Panama experimental feature support queries
        boolean jniStackSwitching = JniExperimentalFeatures.isStackSwitchingSupported();
        boolean panamaStackSwitching = PanamaExperimentalFeatures.isStackSwitchingSupported();

        boolean jniCallCc = JniExperimentalFeatures.isCallCcSupported();
        boolean panamaCallCc = PanamaExperimentalFeatures.isCallCcSupported();

        var jniSupportedFeatures = JniExperimentalFeatures.getSupportedExperimentalFeatures();
        var panamaSupportedFeatures = PanamaExperimentalFeatures.getSupportedExperimentalFeatures();

        bh.consume(jniStackSwitching);
        bh.consume(panamaStackSwitching);
        bh.consume(jniCallCc);
        bh.consume(panamaCallCc);
        bh.consume(jniSupportedFeatures);
        bh.consume(panamaSupportedFeatures);
    }

    /**
     * Main method to run the benchmarks.
     */
    public static void main(String[] args) throws RunnerException {
        logger.info("Starting experimental features performance benchmarks");

        Options opt = new OptionsBuilder()
            .include(ExperimentalFeaturesPerformanceBenchmark.class.getSimpleName())
            .forks(2)
            .warmupIterations(3)
            .warmupTime(org.openjdk.jmh.runner.options.TimeValue.seconds(2))
            .measurementIterations(5)
            .measurementTime(org.openjdk.jmh.runner.options.TimeValue.seconds(3))
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.MICROSECONDS)
            .shouldFailOnError(false) // Don't fail on expected errors for unsupported features
            .addProfiler("gc") // Include GC profiling
            .jvmArgs("-Xmx2g", "-XX:+UseG1GC")
            .build();

        new Runner(opt).run();

        logger.info("Experimental features performance benchmarks completed");
    }

    /**
     * Memory usage benchmark for experimental features.
     */
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(iterations = 10)
    @Warmup(iterations = 3)
    public void memoryUsageBenchmark(Blackhole bh) {
        // Create multiple experimental configurations to measure memory overhead
        for (int i = 0; i < 1000; i++) {
            try (JniExperimentalFeatures jniConfig = new JniExperimentalFeatures();
                 PanamaExperimentalFeatures panamaConfig = new PanamaExperimentalFeatures()) {

                if (i % 100 == 0) {
                    // Configure some of them with experimental features
                    jniConfig.enableStackSwitching(32 * 1024, 50);
                    panamaConfig.enableCallCc(500, PanamaExperimentalFeatures.ContinuationStorageStrategy.STACK);
                }

                bh.consume(jniConfig.isValid());
                bh.consume(panamaConfig.isValid());
            } catch (Exception e) {
                bh.consume(e);
            }
        }
    }

    /**
     * Scalability benchmark: Test performance with increasing numbers of experimental features.
     */
    @Benchmark
    @OperationsPerInvocation(11) // We're adding 11 experimental features
    public void experimentalFeaturesScalabilityBenchmark(Blackhole bh) {
        EngineConfig config = new EngineConfig();

        // Add experimental features incrementally and measure cumulative overhead
        config.addWasmFeature(WasmFeature.STACK_SWITCHING);
        bh.consume(config.getWasmFeatures().size());

        config.addWasmFeature(WasmFeature.CALL_CC);
        bh.consume(config.getWasmFeatures().size());

        config.addWasmFeature(WasmFeature.EXTENDED_CONST_EXPRESSIONS);
        bh.consume(config.getWasmFeatures().size());

        config.addWasmFeature(WasmFeature.MEMORY64_EXTENDED);
        bh.consume(config.getWasmFeatures().size());

        config.addWasmFeature(WasmFeature.CUSTOM_PAGE_SIZES);
        bh.consume(config.getWasmFeatures().size());

        config.addWasmFeature(WasmFeature.SHARED_EVERYTHING_THREADS);
        bh.consume(config.getWasmFeatures().size());

        config.addWasmFeature(WasmFeature.TYPE_IMPORTS);
        bh.consume(config.getWasmFeatures().size());

        config.addWasmFeature(WasmFeature.STRING_IMPORTS);
        bh.consume(config.getWasmFeatures().size());

        config.addWasmFeature(WasmFeature.RESOURCE_TYPES);
        bh.consume(config.getWasmFeatures().size());

        config.addWasmFeature(WasmFeature.INTERFACE_TYPES);
        bh.consume(config.getWasmFeatures().size());

        config.addWasmFeature(WasmFeature.FLEXIBLE_VECTORS);
        bh.consume(config.getWasmFeatures().size());
    }
}