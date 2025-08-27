package ai.tegmentum.wasmtime4j.examples;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;

/**
 * Example demonstrating how to compare JNI and Panama implementations.
 *
 * <p>This example shows:
 * - Manual runtime selection
 * - Performance comparison between implementations
 * - Runtime capability detection
 * - Error handling for unavailable runtimes
 */
public final class RuntimeComparison {

    /** Private constructor to prevent instantiation. */
    private RuntimeComparison() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Main entry point for the example.
     *
     * @param args command line arguments (not used)
     */
    public static void main(final String[] args) {
        try {
            System.out.println("=== Wasmtime4j Runtime Comparison ===\n");

            // Test automatic selection
            testAutomaticSelection();

            // Test JNI runtime
            testJniRuntime();

            // Test Panama runtime (if available)
            testPanamaRuntime();

            // Compare performance
            comparePerformance();

        } catch (final Exception e) {
            System.err.println("Error during runtime comparison: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tests automatic runtime selection.
     */
    private static void testAutomaticSelection() {
        System.out.println("--- Automatic Runtime Selection ---");
        
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            final RuntimeInfo info = runtime.getRuntimeInfo();
            System.out.println("Selected runtime: " + info.getRuntimeType());
            System.out.println("Runtime name: " + info.getRuntimeName());
            System.out.println("Runtime version: " + info.getRuntimeVersion());
            System.out.println("Valid: " + runtime.isValid());
        } catch (final Exception e) {
            System.err.println("Failed to create automatic runtime: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Tests JNI runtime specifically.
     */
    private static void testJniRuntime() {
        System.out.println("--- JNI Runtime ---");
        
        try (WasmRuntime runtime = WasmRuntimeFactory.createJni()) {
            final RuntimeInfo info = runtime.getRuntimeInfo();
            System.out.println("JNI runtime created successfully");
            System.out.println("Runtime type: " + info.getRuntimeType());
            System.out.println("Wasmtime version: " + info.getWasmtimeVersion());
            System.out.println("Java version: " + info.getJavaVersion());
            System.out.println("Platform: " + info.getPlatformInfo());
            
            // Test basic functionality
            testBasicFunctionality(runtime);
            
        } catch (final Exception e) {
            System.err.println("JNI runtime not available: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Tests Panama runtime if available.
     */
    private static void testPanamaRuntime() {
        System.out.println("--- Panama Runtime ---");
        
        try (WasmRuntime runtime = WasmRuntimeFactory.createPanama()) {
            final RuntimeInfo info = runtime.getRuntimeInfo();
            System.out.println("Panama runtime created successfully");
            System.out.println("Runtime type: " + info.getRuntimeType());
            System.out.println("Wasmtime version: " + info.getWasmtimeVersion());
            System.out.println("Java version: " + info.getJavaVersion());
            System.out.println("Platform: " + info.getPlatformInfo());
            
            // Test basic functionality
            testBasicFunctionality(runtime);
            
        } catch (final Exception e) {
            System.err.println("Panama runtime not available: " + e.getMessage());
            if (getJavaVersion() < 23) {
                System.err.println("Note: Panama FFI requires Java 23 or later");
            }
        }
        
        System.out.println();
    }

    /**
     * Tests basic functionality with the given runtime.
     *
     * @param runtime the runtime to test
     */
    private static void testBasicFunctionality(final WasmRuntime runtime) {
        try (Engine engine = runtime.createEngine()) {
            // Simple test: create and configure engine
            final EngineConfig config = engine.getConfig();
            System.out.println("Engine created successfully with config: " + config);
            
            try (Store store = engine.createStore()) {
                System.out.println("Store created successfully");
                System.out.println("Basic functionality test: PASSED");
            }
        } catch (final Exception e) {
            System.err.println("Basic functionality test: FAILED - " + e.getMessage());
        }
    }

    /**
     * Compares performance between available runtimes.
     */
    private static void comparePerformance() {
        System.out.println("--- Performance Comparison ---");
        
        final byte[] simpleWasm = createSimpleWasmModule();
        
        // Test JNI performance
        final long jniTime = measureRuntimePerformance("JNI", () -> {
            try {
                return WasmRuntimeFactory.createJni();
            } catch (final Exception e) {
                return null;
            }
        }, simpleWasm);

        // Test Panama performance (if available)
        final long panamaTime = measureRuntimePerformance("Panama", () -> {
            try {
                return WasmRuntimeFactory.createPanama();
            } catch (final Exception e) {
                return null;
            }
        }, simpleWasm);

        // Compare results
        if (jniTime > 0 && panamaTime > 0) {
            final double ratio = (double) jniTime / panamaTime;
            System.out.printf("Performance ratio (JNI/Panama): %.2f%n", ratio);
            if (ratio > 1.1) {
                System.out.println("Panama is faster by " + String.format("%.1f", (ratio - 1) * 100) + "%");
            } else if (ratio < 0.9) {
                System.out.println("JNI is faster by " + String.format("%.1f", (1 / ratio - 1) * 100) + "%");
            } else {
                System.out.println("Performance is roughly equivalent");
            }
        }
        
        System.out.println();
    }

    /**
     * Measures runtime performance for a given runtime supplier.
     *
     * @param runtimeName name of the runtime being tested
     * @param runtimeSupplier supplier that creates the runtime
     * @param wasmBytes WebAssembly bytecode to test with
     * @return execution time in milliseconds, or -1 if runtime unavailable
     */
    private static long measureRuntimePerformance(
            final String runtimeName,
            final java.util.function.Supplier<WasmRuntime> runtimeSupplier,
            final byte[] wasmBytes) {
        
        System.out.println("Testing " + runtimeName + " performance...");
        
        try {
            final long startTime = System.currentTimeMillis();
            
            // Perform multiple operations to get meaningful timing
            for (int i = 0; i < 100; i++) {
                try (WasmRuntime runtime = runtimeSupplier.get()) {
                    if (runtime == null) {
                        System.out.println(runtimeName + " runtime not available");
                        return -1;
                    }
                    
                    try (Engine engine = runtime.createEngine();
                         Store store = engine.createStore();
                         Module module = engine.compileModule(wasmBytes);
                         Instance instance = module.instantiate(store)) {
                        
                        // Simple operation to ensure everything works
                        final String[] exports = instance.getExportNames();
                        if (exports.length == 0) {
                            throw new RuntimeException("No exports found");
                        }
                    }
                }
            }
            
            final long endTime = System.currentTimeMillis();
            final long totalTime = endTime - startTime;
            
            System.out.println(runtimeName + " completed 100 operations in " + totalTime + "ms");
            return totalTime;
            
        } catch (final Exception e) {
            System.err.println(runtimeName + " performance test failed: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Creates a simple WebAssembly module for testing.
     *
     * @return WebAssembly bytecode for a simple module
     */
    private static byte[] createSimpleWasmModule() {
        // Simple module with a single function
        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // WASM magic number
            0x01, 0x00, 0x00, 0x00, // WASM version
            0x01, 0x04,             // Type section
            0x01,                   // 1 type
            0x60, 0x00, 0x00,       // () -> ()
            0x03, 0x02,             // Function section
            0x01, 0x00,             // 1 function, type 0
            0x07, 0x09,             // Export section
            0x01, 0x05, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x00, 0x00, // export "hello" as function 0
            0x0a, 0x04,             // Code section
            0x01, 0x02, 0x00,       // 1 function, 2 bytes, 0 locals
            0x0b                    // end
        };
    }

    /**
     * Gets the current Java major version.
     *
     * @return the Java major version
     */
    private static int getJavaVersion() {
        final String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, 3));
        } else {
            final int dot = version.indexOf(".");
            if (dot != -1) {
                return Integer.parseInt(version.substring(0, dot));
            } else {
                return Integer.parseInt(version);
            }
        }
    }
}