package ai.tegmentum.wasmtime4j.benchmarks;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Base class for all Wasmtime4j benchmarks providing common configuration and utilities.
 *
 * <p>This class establishes standard benchmark parameters and provides utility methods
 * for consistent benchmark execution across different implementation types.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2g", "-Xmx2g"})
public abstract class BenchmarkBase {

    /**
     * Enumeration of supported runtime implementations for comparison benchmarks.
     */
    public enum RuntimeType {
        /** JNI-based implementation for Java 8+ compatibility. */
        JNI,
        
        /** Panama Foreign Function API implementation for Java 23+. */
        PANAMA,
        
        /** Auto-selection based on Java version and availability. */
        AUTO
    }

    /**
     * Sample WebAssembly module for basic arithmetic operations.
     * This simple module adds two i32 values and returns the result.
     */
    protected static final byte[] SIMPLE_WASM_MODULE = {
        0x00, 0x61, 0x73, 0x6d, // WASM magic number
        0x01, 0x00, 0x00, 0x00, // WASM version
        0x01, 0x07,             // Type section
        0x01,                   // 1 type
        0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f, // (i32, i32) -> i32
        0x03, 0x02,             // Function section
        0x01, 0x00,             // 1 function, type 0
        0x07, 0x07,             // Export section
        0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00, // export "add" as function 0
        0x0a, 0x09,             // Code section
        0x01, 0x07, 0x00,       // 1 function, 7 bytes, 0 locals
        0x20, 0x00,             // local.get 0
        0x20, 0x01,             // local.get 1
        0x6a,                   // i32.add
        0x0b                    // end
    };

    /**
     * More complex WebAssembly module for performance testing with loops and memory operations.
     * This module includes memory allocation and a loop-based computation.
     */
    protected static final byte[] COMPLEX_WASM_MODULE = {
        0x00, 0x61, 0x73, 0x6d, // WASM magic number
        0x01, 0x00, 0x00, 0x00, // WASM version
        0x01, 0x0a,             // Type section
        0x02,                   // 2 types
        0x60, 0x01, 0x7f, 0x01, 0x7f, // Type 0: (i32) -> i32
        0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f, // Type 1: (i32, i32) -> i32
        0x03, 0x03,             // Function section
        0x02, 0x00, 0x01,       // 2 functions, types 0 and 1
        0x05, 0x03,             // Memory section
        0x01, 0x00, 0x01,       // 1 memory, min 1 page
        0x07, 0x15,             // Export section
        0x02,                   // 2 exports
        0x08, 0x66, 0x69, 0x62, 0x6f, 0x6e, 0x61, 0x63, 0x69, 0x00, 0x00, // "fibonacci" function 0
        0x06, 0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x00, // "memory" memory 0
        0x0a, 0x20,             // Code section
        0x02,                   // 2 functions
        // Function 0: fibonacci(n)
        0x1d, 0x00,             // 29 bytes, 0 locals
        0x20, 0x00,             // local.get 0
        0x41, 0x02,             // i32.const 2
        0x49,                   // i32.lt_s
        0x04, 0x7f,             // if i32
        0x20, 0x00,             // local.get 0
        0x05,                   // else
        0x20, 0x00,             // local.get 0
        0x41, 0x01,             // i32.const 1
        0x6b,                   // i32.sub
        0x10, 0x00,             // call 0 (recursive)
        0x20, 0x00,             // local.get 0
        0x41, 0x02,             // i32.const 2
        0x6b,                   // i32.sub
        0x10, 0x00,             // call 0 (recursive)
        0x6a,                   // i32.add
        0x0b,                   // end if
        0x0b,                   // end function
        // Function 1: sum(start, end)
        0x02, 0x00,             // 2 bytes, 0 locals (placeholder)
        0x41, 0x00,             // i32.const 0
        0x0b                    // end function
    };

    /**
     * Gets the current Java version for runtime selection logic.
     *
     * @return the major Java version number
     */
    protected static int getJavaVersion() {
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

    /**
     * Determines the appropriate runtime type based on system capabilities.
     *
     * @return the recommended runtime type for the current environment
     */
    protected static RuntimeType getRecommendedRuntime() {
        final int javaVersion = getJavaVersion();
        if (javaVersion >= 23) {
            // Check if Panama is available
            try {
                Class.forName("java.lang.foreign.MemorySegment");
                return RuntimeType.PANAMA;
            } catch (final ClassNotFoundException e) {
                return RuntimeType.JNI;
            }
        } else {
            return RuntimeType.JNI;
        }
    }

    /**
     * Validates that a WebAssembly module byte array is not null and has minimum expected size.
     *
     * @param wasmModule the WebAssembly module bytes to validate
     * @throws IllegalArgumentException if the module is invalid
     */
    protected static void validateWasmModule(final byte[] wasmModule) {
        if (wasmModule == null) {
            throw new IllegalArgumentException("WASM module cannot be null");
        }
        if (wasmModule.length < 8) {
            throw new IllegalArgumentException("WASM module too small");
        }
        // Check WASM magic number
        if (wasmModule[0] != 0x00 || wasmModule[1] != 0x61 || 
            wasmModule[2] != 0x73 || wasmModule[3] != 0x6d) {
            throw new IllegalArgumentException("Invalid WASM magic number");
        }
    }

    /**
     * Creates a formatted benchmark identifier for result tracking.
     *
     * @param operation the operation being benchmarked
     * @param runtime the runtime implementation being tested
     * @return formatted benchmark identifier
     */
    protected static String formatBenchmarkId(final String operation, final RuntimeType runtime) {
        return String.format("%s_%s_%d", operation, runtime.name().toLowerCase(), 
                           System.currentTimeMillis() % 10000);
    }

    /**
     * Performs a simple blackhole operation to prevent dead code elimination.
     * This ensures the JIT compiler doesn't optimize away our benchmark code.
     *
     * @param value the value to consume
     * @return the same value (prevents optimization)
     */
    protected static int preventOptimization(final int value) {
        return value;
    }

    /**
     * Performs a simple blackhole operation for byte arrays.
     *
     * @param value the byte array to consume
     * @return the length of the array (prevents optimization)
     */
    protected static int preventOptimization(final byte[] value) {
        return value != null ? value.length : 0;
    }
}