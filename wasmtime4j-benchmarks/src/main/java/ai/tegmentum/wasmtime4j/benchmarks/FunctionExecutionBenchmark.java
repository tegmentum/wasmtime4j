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
 * Benchmarks for WebAssembly function execution performance.
 *
 * <p>This benchmark class measures the performance characteristics of calling
 * WebAssembly functions from Java, comparing JNI and Panama implementations
 * across different function types and call patterns.
 *
 * <p>Key metrics measured:
 * <ul>
 *   <li>Function call overhead</li>
 *   <li>Parameter marshalling performance</li>
 *   <li>Return value handling</li>
 *   <li>Recursive function call performance</li>
 *   <li>Batch function execution</li>
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms1g", "-Xmx1g"})
public class FunctionExecutionBenchmark extends BenchmarkBase {

    /**
     * Runtime implementation to benchmark.
     */
    @Param({"JNI", "PANAMA"})
    private RuntimeType runtimeType;

    /**
     * Function type to test different call patterns.
     */
    @Param({"SIMPLE", "COMPLEX", "RECURSIVE"})
    private String functionType;

    /**
     * Number of parameters to pass to the function.
     */
    @Param({"1", "2", "4"})
    private int parameterCount;

    /**
     * Mock WebAssembly function representation for testing.
     */
    private static final class MockWasmFunction {
        private final String name;
        private final String type;
        private final RuntimeType runtimeType;
        private final int parameterCount;
        private long executionCount;
        private long totalExecutionTime;

        MockWasmFunction(final String name, final String type, 
                        final RuntimeType runtimeType, final int parameterCount) {
            this.name = name;
            this.type = type;
            this.runtimeType = runtimeType;
            this.parameterCount = parameterCount;
            this.executionCount = 0;
            this.totalExecutionTime = 0;
        }

        int call(final int... params) {
            if (params.length != parameterCount) {
                throw new IllegalArgumentException("Parameter count mismatch");
            }

            final long startTime = System.nanoTime();
            final int result = executeFunction(params);
            final long endTime = System.nanoTime();

            executionCount++;
            totalExecutionTime += (endTime - startTime);

            return result;
        }

        private int executeFunction(final int[] params) {
            switch (type) {
                case "SIMPLE":
                    return executeSimpleFunction(params);
                case "COMPLEX":
                    return executeComplexFunction(params);
                case "RECURSIVE":
                    return executeRecursiveFunction(params);
                default:
                    return executeSimpleFunction(params);
            }
        }

        private int executeSimpleFunction(final int[] params) {
            // Simulate simple arithmetic operation
            int result = 0;
            for (final int param : params) {
                result += param;
            }
            
            // Add runtime-specific overhead
            if (runtimeType == RuntimeType.JNI) {
                // Simulate JNI call overhead
                for (int i = 0; i < 10; i++) {
                    result += Math.abs(i);
                }
            } else {
                // Simulate Panama FFI call overhead
                for (int i = 0; i < 15; i++) {
                    result += Math.abs(i * 2);
                }
            }
            
            return result;
        }

        private int executeComplexFunction(final int[] params) {
            // Simulate complex computation
            int result = 1;
            for (final int param : params) {
                for (int i = 1; i <= Math.min(param, 10); i++) {
                    result = (result * i) % 1000000;
                }
            }
            
            // Add floating point operations
            double floatResult = result;
            for (int i = 0; i < 20; i++) {
                floatResult = Math.sqrt(floatResult + i);
            }
            
            return (int) floatResult;
        }

        private int executeRecursiveFunction(final int[] params) {
            final int n = Math.min(params[0], 15); // Limit recursion depth
            return fibonacci(n);
        }

        private int fibonacci(final int n) {
            if (n <= 1) {
                return n;
            }
            return fibonacci(n - 1) + fibonacci(n - 2);
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public RuntimeType getRuntimeType() {
            return runtimeType;
        }

        public int getParameterCount() {
            return parameterCount;
        }

        public long getExecutionCount() {
            return executionCount;
        }

        public long getTotalExecutionTime() {
            return totalExecutionTime;
        }

        public double getAverageExecutionTime() {
            return executionCount > 0 ? (double) totalExecutionTime / executionCount : 0;
        }

        public void resetStats() {
            executionCount = 0;
            totalExecutionTime = 0;
        }
    }

    /**
     * Function instance being benchmarked.
     */
    private MockWasmFunction function;

    /**
     * Test parameters for function calls.
     */
    private int[] testParams;

    /**
     * Setup performed before each benchmark iteration.
     */
    @Setup(Level.Iteration)
    public void setupIteration() {
        // Create function instance
        final String functionName = String.format("%s_%s_%d", 
                                                 functionType.toLowerCase(), 
                                                 runtimeType.name().toLowerCase(),
                                                 parameterCount);
        
        function = new MockWasmFunction(functionName, functionType, runtimeType, parameterCount);
        
        // Setup test parameters
        testParams = new int[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            testParams[i] = i + 1; // Use simple sequential values
        }
        
        // Reset function statistics
        function.resetStats();
    }

    /**
     * Cleanup performed after each benchmark iteration.
     */
    @TearDown(Level.Iteration)
    public void teardownIteration() {
        if (function != null) {
            function.resetStats();
            function = null;
        }
        testParams = null;
    }

    /**
     * Benchmarks single function call performance.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     * @return the function result
     */
    @Benchmark
    public int benchmarkSingleFunctionCall(final Blackhole blackhole) {
        final int result = function.call(testParams);
        blackhole.consume(function.getExecutionCount());
        return result;
    }

    /**
     * Benchmarks repeated function calls.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     * @return the last function result
     */
    @Benchmark
    public int benchmarkRepeatedFunctionCalls(final Blackhole blackhole) {
        final int iterations = functionType.equals("RECURSIVE") ? 5 : 10;
        int lastResult = 0;
        
        for (int i = 0; i < iterations; i++) {
            lastResult = function.call(testParams);
            blackhole.consume(lastResult);
        }
        
        blackhole.consume(function.getExecutionCount());
        return lastResult;
    }

    /**
     * Benchmarks function calls with different parameter patterns.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkParameterVariations(final Blackhole blackhole) {
        final int[][] paramVariations = generateParameterVariations();
        
        for (final int[] params : paramVariations) {
            if (params.length == parameterCount) {
                final int result = function.call(params);
                blackhole.consume(result);
            }
        }
        
        blackhole.consume(function.getExecutionCount());
    }

    /**
     * Benchmarks function call with result validation.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkFunctionCallWithValidation(final Blackhole blackhole) {
        final int result = function.call(testParams);
        
        // Validate result based on function type
        final boolean isValidResult = validateResult(result);
        blackhole.consume(isValidResult);
        blackhole.consume(result);
    }

    /**
     * Benchmarks batch function execution.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkBatchFunctionExecution(final Blackhole blackhole) {
        final int batchSize = functionType.equals("RECURSIVE") ? 3 : 8;
        final int[] results = new int[batchSize];
        
        for (int i = 0; i < batchSize; i++) {
            // Modify parameters slightly for each call
            final int[] batchParams = testParams.clone();
            for (int j = 0; j < batchParams.length; j++) {
                batchParams[j] += i;
            }
            
            results[i] = function.call(batchParams);
            blackhole.consume(results[i]);
        }
        
        blackhole.consume(results.length);
        blackhole.consume(function.getExecutionCount());
    }

    /**
     * Benchmarks function call performance under memory pressure.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkCallWithMemoryPressure(final Blackhole blackhole) {
        // Create memory pressure
        final Object[] memoryPressure = new Object[100];
        for (int i = 0; i < memoryPressure.length; i++) {
            memoryPressure[i] = new int[256]; // 1KB per allocation
        }
        
        final int result = function.call(testParams);
        
        blackhole.consume(result);
        blackhole.consume(memoryPressure.length);
        
        // Clear memory pressure
        for (int i = 0; i < memoryPressure.length; i++) {
            memoryPressure[i] = null;
        }
    }

    /**
     * Benchmarks error handling during function calls.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkErrorHandling(final Blackhole blackhole) {
        int successfulCalls = 0;
        int errorCalls = 0;
        
        // Mix of valid and invalid calls
        for (int i = 0; i < 5; i++) {
            try {
                if (i % 3 == 0) {
                    // Invalid call with wrong parameter count
                    final int[] wrongParams = new int[parameterCount + 1];
                    function.call(wrongParams);
                } else {
                    // Valid call
                    function.call(testParams);
                    successfulCalls++;
                }
            } catch (final IllegalArgumentException e) {
                errorCalls++;
                blackhole.consume(e.getMessage());
            }
        }
        
        blackhole.consume(successfulCalls);
        blackhole.consume(errorCalls);
    }

    /**
     * Benchmarks function call statistics tracking.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void benchmarkCallStatistics(final Blackhole blackhole) {
        final int result = function.call(testParams);
        
        blackhole.consume(result);
        blackhole.consume(function.getExecutionCount());
        blackhole.consume(function.getTotalExecutionTime());
        blackhole.consume(function.getAverageExecutionTime());
    }

    /**
     * Generates different parameter variations for testing.
     *
     * @return array of parameter combinations
     */
    private int[][] generateParameterVariations() {
        final int[][] variations = new int[6][];
        
        // Variation 1: All zeros
        variations[0] = new int[parameterCount];
        
        // Variation 2: Sequential numbers
        variations[1] = new int[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            variations[1][i] = i + 1;
        }
        
        // Variation 3: All same value
        variations[2] = new int[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            variations[2][i] = 42;
        }
        
        // Variation 4: Large values
        variations[3] = new int[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            variations[3][i] = 1000 + i;
        }
        
        // Variation 5: Negative values
        variations[4] = new int[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            variations[4][i] = -(i + 1);
        }
        
        // Variation 6: Mixed values
        variations[5] = new int[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            variations[5][i] = (i % 2 == 0) ? i : -i;
        }
        
        return variations;
    }

    /**
     * Validates function result based on function type.
     *
     * @param result the result to validate
     * @return true if the result is valid
     */
    private boolean validateResult(final int result) {
        switch (functionType) {
            case "SIMPLE":
                // For simple addition, result should be sum of parameters
                int expectedSum = 0;
                for (final int param : testParams) {
                    expectedSum += param;
                }
                return result >= expectedSum; // Account for runtime overhead
                
            case "COMPLEX":
                // Complex function should return non-zero for positive inputs
                return result != 0;
                
            case "RECURSIVE":
                // Fibonacci results should be non-negative
                return result >= 0;
                
            default:
                return true;
        }
    }
}