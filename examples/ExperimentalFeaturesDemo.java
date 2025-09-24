import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.jni.JniExperimentalFeatures;
import ai.tegmentum.wasmtime4j.panama.PanamaExperimentalFeatures;

import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Comprehensive demonstration of experimental WebAssembly features in wasmtime4j.
 *
 * <p>This example showcases how to use cutting-edge WebAssembly proposals
 * that are currently in committee stage, including:
 * <ul>
 *   <li>Stack switching for coroutines and fibers</li>
 *   <li>Call/CC (call-with-current-continuation) support</li>
 *   <li>Extended constant expressions</li>
 *   <li>Memory64 extensions</li>
 *   <li>Custom page sizes</li>
 *   <li>Shared-everything threads</li>
 *   <li>Type and string imports</li>
 *   <li>Resource types and interface types</li>
 *   <li>Flexible vectors</li>
 * </ul>
 *
 * <p><strong>WARNING:</strong> These features are experimental and subject to change.
 * They should only be used for testing and development purposes.
 *
 * @since 1.0.0
 */
public class ExperimentalFeaturesDemo {

    private static final Logger logger = Logger.getLogger(ExperimentalFeaturesDemo.class.getName());

    public static void main(String[] args) {
        logger.info("Starting Experimental WebAssembly Features Demonstration");
        logger.info("===============================================");

        try {
            // Demonstrate engine configuration with experimental features
            demonstrateEngineConfigurationWithExperimentalFeatures();

            // Demonstrate JNI experimental features
            demonstrateJniExperimentalFeatures();

            // Demonstrate Panama experimental features (Java 23+)
            demonstratePanamaExperimentalFeatures();

            // Demonstrate feature support detection
            demonstrateFeatureSupportDetection();

            // Demonstrate cross-runtime compatibility
            demonstrateCrossRuntimeCompatibility();

            // Demonstrate performance considerations
            demonstratePerformanceConsiderations();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in experimental features demonstration", e);
            System.exit(1);
        }

        logger.info("Experimental WebAssembly Features Demonstration completed successfully");
    }

    /**
     * Demonstrates how to configure the engine with experimental WebAssembly features.
     */
    private static void demonstrateEngineConfigurationWithExperimentalFeatures() {
        logger.info("\n1. Engine Configuration with Experimental Features");
        logger.info("--------------------------------------------------");

        try {
            // Create a standard configuration
            EngineConfig standardConfig = new EngineConfig()
                .optimizationLevel(EngineConfig.OptimizationLevel.SPEED)
                .addWasmFeature(WasmFeature.SIMD)
                .addWasmFeature(WasmFeature.MULTI_VALUE)
                .addWasmFeature(WasmFeature.BULK_MEMORY);

            logger.info("Standard configuration features: " + standardConfig.getWasmFeatures());

            // Create an experimental configuration
            EngineConfig experimentalConfig = new EngineConfig()
                .optimizationLevel(EngineConfig.OptimizationLevel.SPEED)
                .addWasmFeature(WasmFeature.SIMD)
                .addWasmFeature(WasmFeature.MULTI_VALUE)
                .addWasmFeature(WasmFeature.BULK_MEMORY)
                // Add experimental features
                .addWasmFeature(WasmFeature.STACK_SWITCHING)
                .addWasmFeature(WasmFeature.CALL_CC)
                .addWasmFeature(WasmFeature.EXTENDED_CONST_EXPRESSIONS)
                .addWasmFeature(WasmFeature.MEMORY64_EXTENDED)
                .addWasmFeature(WasmFeature.FLEXIBLE_VECTORS);

            logger.info("Experimental configuration features: " + experimentalConfig.getWasmFeatures());

            // Use pre-configured experimental configurations
            EngineConfig allExperimentalConfig = EngineConfig.forExperimentalFeatures();
            logger.info("All-experimental configuration features: " + allExperimentalConfig.getWasmFeatures());

            EngineConfig threadingConfig = EngineConfig.forExperimentalThreading();
            logger.info("Experimental threading configuration features: " + threadingConfig.getWasmFeatures());

            EngineConfig componentsConfig = EngineConfig.forExperimentalComponents();
            logger.info("Experimental components configuration features: " + componentsConfig.getWasmFeatures());

            // Test experimental feature getters
            logger.info("Feature states in experimental config:");
            logger.info("  Stack Switching: " + experimentalConfig.isWasmStackSwitching());
            logger.info("  Call/CC: " + experimentalConfig.isWasmCallCc());
            logger.info("  Extended Const Expressions: " + experimentalConfig.isWasmExtendedConstExpressions());
            logger.info("  Memory64 Extended: " + experimentalConfig.isWasmMemory64Extended());
            logger.info("  Custom Page Sizes: " + experimentalConfig.isWasmCustomPageSizes());
            logger.info("  Shared Everything Threads: " + experimentalConfig.isWasmSharedEverythingThreads());
            logger.info("  Type Imports: " + experimentalConfig.isWasmTypeImports());
            logger.info("  String Imports: " + experimentalConfig.isWasmStringImports());
            logger.info("  Resource Types: " + experimentalConfig.isWasmResourceTypes());
            logger.info("  Interface Types: " + experimentalConfig.isWasmInterfaceTypes());
            logger.info("  Flexible Vectors: " + experimentalConfig.isWasmFlexibleVectors());

            // Try to create runtime with experimental configuration
            // Note: This may fail if the features are not supported by Wasmtime
            try (WasmRuntime runtime = WasmRuntimeFactory.createRuntime(experimentalConfig)) {
                logger.info("Successfully created runtime with experimental features");
            } catch (Exception e) {
                logger.warning("Could not create runtime with experimental features (expected): " + e.getMessage());
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error demonstrating engine configuration", e);
        }
    }

    /**
     * Demonstrates JNI experimental features configuration.
     */
    private static void demonstrateJniExperimentalFeatures() {
        logger.info("\n2. JNI Experimental Features Configuration");
        logger.info("-----------------------------------------");

        try {
            // Create standard JNI experimental configuration
            try (JniExperimentalFeatures standardConfig = new JniExperimentalFeatures()) {
                logger.info("Created standard JNI experimental configuration");
                logger.info("Configuration valid: " + standardConfig.isValid());

                // Enable stack switching
                try {
                    standardConfig.enableStackSwitching(64 * 1024, 100); // 64KB stacks, max 100
                    logger.info("Enabled stack switching: 64KB stacks, max 100 concurrent");
                } catch (Exception e) {
                    logger.warning("Could not enable stack switching: " + e.getMessage());
                }

                // Enable call/cc
                try {
                    standardConfig.enableCallCc(1000, JniExperimentalFeatures.ContinuationStorageStrategy.HYBRID);
                    logger.info("Enabled call/cc: max 1000 continuations, hybrid storage");
                } catch (Exception e) {
                    logger.warning("Could not enable call/cc: " + e.getMessage());
                }

                // Enable extended constant expressions
                try {
                    standardConfig.enableExtendedConstExpressions(
                        true,  // import-based expressions
                        true,  // global dependencies
                        JniExperimentalFeatures.ConstantFoldingLevel.AGGRESSIVE
                    );
                    logger.info("Enabled extended constant expressions with aggressive folding");
                } catch (Exception e) {
                    logger.warning("Could not enable extended constant expressions: " + e.getMessage());
                }

                logger.info("Final configuration valid: " + standardConfig.isValid());
            }

            // Create all-experimental JNI configuration
            try (JniExperimentalFeatures allExperimentalConfig = JniExperimentalFeatures.allExperimentalEnabled()) {
                logger.info("Created all-experimental JNI configuration");
                logger.info("All-experimental configuration valid: " + allExperimentalConfig.isValid());
            }

            // Test parameter validation
            logger.info("Testing parameter validation...");
            try (JniExperimentalFeatures testConfig = new JniExperimentalFeatures()) {
                // These should throw exceptions
                try {
                    testConfig.enableStackSwitching(2048, 100); // Too small stack size
                    logger.warning("Parameter validation failed - small stack size should be rejected");
                } catch (IllegalArgumentException e) {
                    logger.info("Correctly rejected small stack size: " + e.getMessage());
                }

                try {
                    testConfig.enableCallCc(0, JniExperimentalFeatures.ContinuationStorageStrategy.STACK);
                    logger.warning("Parameter validation failed - zero continuations should be rejected");
                } catch (IllegalArgumentException e) {
                    logger.info("Correctly rejected zero continuations: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error demonstrating JNI experimental features", e);
        }
    }

    /**
     * Demonstrates Panama experimental features configuration (Java 23+).
     */
    private static void demonstratePanamaExperimentalFeatures() {
        logger.info("\n3. Panama Experimental Features Configuration");
        logger.info("--------------------------------------------");

        // Check if Panama is available
        int javaVersion = getJavaVersion();
        if (javaVersion < 23) {
            logger.info("Panama FFI requires Java 23+. Current version: " + javaVersion + ". Skipping Panama demo.");
            return;
        }

        try {
            // Create standard Panama experimental configuration
            try (PanamaExperimentalFeatures standardConfig = new PanamaExperimentalFeatures()) {
                logger.info("Created standard Panama experimental configuration");
                logger.info("Configuration valid: " + standardConfig.isValid());

                // Enable stack switching
                try {
                    standardConfig.enableStackSwitching(64 * 1024, 100); // 64KB stacks, max 100
                    logger.info("Enabled stack switching: 64KB stacks, max 100 concurrent");
                } catch (Exception e) {
                    logger.warning("Could not enable stack switching: " + e.getMessage());
                }

                // Enable call/cc
                try {
                    standardConfig.enableCallCc(1000, PanamaExperimentalFeatures.ContinuationStorageStrategy.HYBRID);
                    logger.info("Enabled call/cc: max 1000 continuations, hybrid storage");
                } catch (Exception e) {
                    logger.warning("Could not enable call/cc: " + e.getMessage());
                }

                // Enable extended constant expressions
                try {
                    standardConfig.enableExtendedConstExpressions(
                        true,  // import-based expressions
                        true,  // global dependencies
                        PanamaExperimentalFeatures.ConstantFoldingLevel.AGGRESSIVE
                    );
                    logger.info("Enabled extended constant expressions with aggressive folding");
                } catch (Exception e) {
                    logger.warning("Could not enable extended constant expressions: " + e.getMessage());
                }

                logger.info("Final configuration valid: " + standardConfig.isValid());
            }

            // Create all-experimental Panama configuration
            try (PanamaExperimentalFeatures allExperimentalConfig = PanamaExperimentalFeatures.allExperimentalEnabled()) {
                logger.info("Created all-experimental Panama configuration");
                logger.info("All-experimental configuration valid: " + allExperimentalConfig.isValid());
            }

            // Test parameter validation (similar to JNI)
            logger.info("Testing parameter validation...");
            try (PanamaExperimentalFeatures testConfig = new PanamaExperimentalFeatures()) {
                try {
                    testConfig.enableStackSwitching(2048, 100); // Too small stack size
                    logger.warning("Parameter validation failed - small stack size should be rejected");
                } catch (IllegalArgumentException e) {
                    logger.info("Correctly rejected small stack size: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error demonstrating Panama experimental features", e);
        }
    }

    /**
     * Demonstrates feature support detection capabilities.
     */
    private static void demonstrateFeatureSupportDetection() {
        logger.info("\n4. Feature Support Detection");
        logger.info("----------------------------");

        try {
            // Check JNI feature support
            logger.info("JNI Experimental Feature Support:");
            boolean jniStackSwitching = JniExperimentalFeatures.isStackSwitchingSupported();
            boolean jniCallCc = JniExperimentalFeatures.isCallCcSupported();
            Set<WasmFeature> jniSupportedFeatures = JniExperimentalFeatures.getSupportedExperimentalFeatures();

            logger.info("  Stack Switching: " + jniStackSwitching);
            logger.info("  Call/CC: " + jniCallCc);
            logger.info("  Supported Features: " + jniSupportedFeatures);

            // Check Panama feature support
            logger.info("Panama Experimental Feature Support:");
            boolean panamaStackSwitching = PanamaExperimentalFeatures.isStackSwitchingSupported();
            boolean panamaCallCc = PanamaExperimentalFeatures.isCallCcSupported();
            Set<WasmFeature> panamaSupportedFeatures = PanamaExperimentalFeatures.getSupportedExperimentalFeatures();

            logger.info("  Stack Switching: " + panamaStackSwitching);
            logger.info("  Call/CC: " + panamaCallCc);
            logger.info("  Supported Features: " + panamaSupportedFeatures);

            // Check for consistency
            if (jniStackSwitching == panamaStackSwitching && jniCallCc == panamaCallCc) {
                logger.info("Feature support is consistent between JNI and Panama implementations");
            } else {
                logger.warning("Feature support differs between JNI and Panama implementations");
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error demonstrating feature support detection", e);
        }
    }

    /**
     * Demonstrates cross-runtime compatibility between JNI and Panama.
     */
    private static void demonstrateCrossRuntimeCompatibility() {
        logger.info("\n5. Cross-Runtime Compatibility");
        logger.info("------------------------------");

        try {
            // Test with both runtime types
            logger.info("Testing experimental features with auto runtime selection...");

            EngineConfig config = EngineConfig.forExperimentalFeatures();
            try (WasmRuntime runtime = WasmRuntimeFactory.createRuntime(config)) {
                logger.info("Successfully created runtime with experimental features");
                logger.info("Runtime implementation: " + runtime.getClass().getSimpleName());
            } catch (Exception e) {
                logger.warning("Could not create runtime with experimental features: " + e.getMessage());
            }

            // Test configuration compatibility
            logger.info("Testing configuration compatibility...");
            EngineConfig[] configs = {
                EngineConfig.forExperimentalFeatures(),
                EngineConfig.forExperimentalThreading(),
                EngineConfig.forExperimentalComponents()
            };

            for (int i = 0; i < configs.length; i++) {
                EngineConfig testConfig = configs[i];
                String configType = i == 0 ? "All Experimental" : i == 1 ? "Threading" : "Components";
                logger.info("Testing " + configType + " configuration...");

                try (WasmRuntime runtime = WasmRuntimeFactory.createRuntime(testConfig)) {
                    logger.info("  Successfully created runtime for " + configType);
                } catch (Exception e) {
                    logger.info("  Could not create runtime for " + configType + " (expected): " + e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error demonstrating cross-runtime compatibility", e);
        }
    }

    /**
     * Demonstrates performance considerations when using experimental features.
     */
    private static void demonstratePerformanceConsiderations() {
        logger.info("\n6. Performance Considerations");
        logger.info("-----------------------------");

        try {
            // Measure configuration creation overhead
            logger.info("Measuring configuration creation overhead...");

            long standardConfigTime = measureConfigCreationTime(() -> new EngineConfig());
            long experimentalConfigTime = measureConfigCreationTime(EngineConfig::forExperimentalFeatures);

            logger.info("Standard configuration creation time: " + standardConfigTime + " ns");
            logger.info("Experimental configuration creation time: " + experimentalConfigTime + " ns");

            if (experimentalConfigTime > standardConfigTime) {
                double overhead = ((double) experimentalConfigTime / standardConfigTime - 1) * 100;
                logger.info("Experimental configuration overhead: " + String.format("%.2f%%", overhead));
            }

            // Measure experimental features configuration overhead
            logger.info("Measuring experimental features configuration overhead...");

            long jniConfigTime = measureTime(() -> {
                try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
                    config.enableStackSwitching(32 * 1024, 50);
                    config.enableCallCc(500, JniExperimentalFeatures.ContinuationStorageStrategy.STACK);
                    return config.isValid();
                } catch (Exception e) {
                    return false;
                }
            });

            logger.info("JNI experimental configuration time: " + jniConfigTime + " ns");

            if (getJavaVersion() >= 23) {
                long panamaConfigTime = measureTime(() -> {
                    try (PanamaExperimentalFeatures config = new PanamaExperimentalFeatures()) {
                        config.enableStackSwitching(32 * 1024, 50);
                        config.enableCallCc(500, PanamaExperimentalFeatures.ContinuationStorageStrategy.STACK);
                        return config.isValid();
                    } catch (Exception e) {
                        return false;
                    }
                });

                logger.info("Panama experimental configuration time: " + panamaConfigTime + " ns");

                if (jniConfigTime != 0 && panamaConfigTime != 0) {
                    double ratio = (double) panamaConfigTime / jniConfigTime;
                    logger.info("Panama/JNI performance ratio: " + String.format("%.2f", ratio));
                }
            }

            // Memory usage demonstration
            logger.info("Testing memory usage with experimental features...");
            Runtime runtime = Runtime.getRuntime();
            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

            // Create multiple experimental configurations
            for (int i = 0; i < 100; i++) {
                try (JniExperimentalFeatures config = new JniExperimentalFeatures()) {
                    if (i % 10 == 0) {
                        config.enableStackSwitching(16 * 1024, 25);
                    }
                } catch (Exception ignored) {
                }
            }

            System.gc(); // Suggest garbage collection
            Thread.sleep(100); // Give GC time to run

            long afterMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryDelta = afterMemory - beforeMemory;

            logger.info("Memory usage delta: " + memoryDelta + " bytes");

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error demonstrating performance considerations", e);
        }
    }

    /**
     * Measures the time taken to create a configuration.
     */
    private static long measureConfigCreationTime(java.util.function.Supplier<EngineConfig> configSupplier) {
        // Warmup
        for (int i = 0; i < 10; i++) {
            configSupplier.get();
        }

        // Measure
        long startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            configSupplier.get();
        }
        long endTime = System.nanoTime();

        return (endTime - startTime) / 100; // Average per configuration
    }

    /**
     * Measures the time taken to execute a function.
     */
    private static long measureTime(java.util.function.Supplier<Boolean> function) {
        // Warmup
        for (int i = 0; i < 5; i++) {
            function.get();
        }

        // Measure
        long startTime = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            function.get();
        }
        long endTime = System.nanoTime();

        return (endTime - startTime) / 10; // Average per execution
    }

    /**
     * Gets the major Java version number.
     */
    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }
}