package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.performance.CallBatch;
import ai.tegmentum.wasmtime4j.jni.performance.CompilationCache;
import ai.tegmentum.wasmtime4j.jni.performance.NativeObjectPool;
import ai.tegmentum.wasmtime4j.jni.performance.OptimizedMarshalling;
import ai.tegmentum.wasmtime4j.jni.performance.PerformanceMonitor;
import java.util.concurrent.CompletableFuture;
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
 * Benchmarks for performance optimization features including call batching,
 * memory pooling, optimized marshalling, and compilation caching.
 *
 * <p>This benchmark class measures the performance impact of various optimization
 * techniques implemented in the WebAssembly runtime.
 *
 * <p>Key optimizations benchmarked:
 * <ul>
 *   <li>Call batching for bulk operations</li>
 *   <li>Memory pooling for native objects</li>
 *   <li>Optimized parameter marshalling</li>
 *   <li>Compilation caching for startup performance</li>
 *   <li>Performance monitoring overhead</li>
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {
        "-Xms1g", "-Xmx1g", 
        "-Dwasmtime4j.performance.monitoring=true",
        "-Dwasmtime4j.cache.enabled=true"
    })
public class PerformanceOptimizationBenchmark extends BenchmarkBase {

    /** Runtime implementation to benchmark. */
    @Param({"JNI"}) // Focus on JNI since that's where optimizations are implemented
    private String runtimeTypeName;

    /** Batch size for call batching benchmarks. */
    @Param({"1", "8", "16", "32"})
    private int batchSize;

    /** WebAssembly runtime components. */
    private WasmRuntime runtime;
    private Engine engine;
    private Store store;
    private Module module;
    private Instance instance;
    private WasmFunction simpleFunction;
    private WasmFunction complexFunction;

    /** Test data for benchmarks. */
    private WasmValue[] testParams;
    private byte[] moduleBytes;
    private byte[][] multipleBytecodes;

    /** Performance optimization components. */
    private NativeObjectPool<byte[]> bufferPool;

    @Setup(Level.Trial)
    public void setupTrial() throws WasmException {
        // Enable all performance optimizations
        PerformanceMonitor.setEnabled(true);
        PerformanceMonitor.setProfilingEnabled(false); // Disable detailed profiling for benchmarks
        CompilationCache.setEnabled(true);

        // Initialize runtime components
        final RuntimeType runtimeType = RuntimeType.valueOf(runtimeTypeName);
        runtime = createRuntime(runtimeType);
        engine = createEngine(runtime);
        store = createStore(engine);

        // Create test module
        moduleBytes = SIMPLE_WASM_MODULE.clone();
        module = engine.compileModule(moduleBytes);
        instance = runtime.instantiate(module, store);

        // Get test functions
        simpleFunction = instance.getFunction("add").orElseThrow(
            () -> new RuntimeException("Test function 'add' not found"));
        complexFunction = instance.getFunction("fibonacci").orElse(simpleFunction);

        // Setup test parameters
        testParams = new WasmValue[] {
            WasmValue.i32(10),
            WasmValue.i32(20)
        };

        // Initialize object pools
        bufferPool = NativeObjectPool.getPool(
            byte[].class, 
            () -> new byte[1024],
            32
        );

        // Create multiple bytecodes for compilation cache testing
        multipleBytecodes = new byte[10][];
        for (int i = 0; i < multipleBytecodes.length; i++) {
            multipleBytecodes[i] = generateTestModule(i);
        }

        addMetric("optimization", "setup_complete");
    }

    @TearDown(Level.Trial) 
    public void tearDownTrial() {
        // Clean up resources
        if (bufferPool != null) {
            bufferPool.close();
        }
        
        // Print performance statistics
        System.out.println("\n=== Performance Optimization Statistics ===");
        System.out.println(PerformanceMonitor.getStatistics());
        System.out.println(CompilationCache.getStatistics());
        System.out.println(OptimizedMarshalling.getStatistics());
        System.out.println(NativeObjectPool.getAllPoolStats());
    }

    /**
     * Benchmarks native call overhead for simple operations.
     * Target: <100 nanoseconds per call.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public WasmValue[] benchmarkNativeCallOverhead(final Blackhole bh) throws WasmException {
        final WasmValue[] result = simpleFunction.call(testParams);
        bh.consume(result);
        return result;
    }

    /**
     * Benchmarks optimized parameter marshalling performance.
     */
    @Benchmark
    public Object[] benchmarkOptimizedMarshalling(final Blackhole bh) {
        // Test different parameter patterns
        final WasmValue[] params1 = new WasmValue[] { WasmValue.i32(1) };
        final WasmValue[] params2 = new WasmValue[] { WasmValue.i32(1), WasmValue.i32(2) };
        final WasmValue[] params4 = new WasmValue[] { 
            WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3), WasmValue.i32(4) 
        };

        final Object[] result1 = OptimizedMarshalling.marshalParameters(params1);
        final Object[] result2 = OptimizedMarshalling.marshalParameters(params2);  
        final Object[] result4 = OptimizedMarshalling.marshalParameters(params4);

        bh.consume(result1);
        bh.consume(result2);
        bh.consume(result4);

        return result4;
    }

    /**
     * Benchmarks memory pool allocation performance.
     */
    @Benchmark
    public byte[] benchmarkMemoryPooling(final Blackhole bh) {
        // Test pooled vs non-pooled allocations
        final byte[] buffer1 = bufferPool.borrow();
        final byte[] buffer2 = bufferPool.borrow();
        
        // Simulate some work
        if (buffer1 != null) {
            buffer1[0] = 1;
        }
        if (buffer2 != null) {
            buffer2[0] = 2;
        }

        bh.consume(buffer1);
        bh.consume(buffer2);

        // Return to pool
        if (buffer1 != null) {
            bufferPool.returnObject(buffer1);
        }
        if (buffer2 != null) {
            bufferPool.returnObject(buffer2);
        }

        return buffer1;
    }

    /**
     * Benchmarks call batching performance for bulk operations.
     */
    @Benchmark
    public WasmValue[] benchmarkCallBatching(final Blackhole bh) throws Exception {
        try (final CallBatch batch = new CallBatch(batchSize, 0)) {
            // Add function calls to batch
            final CompletableFuture<WasmValue[]>[] futures = new CompletableFuture[batchSize];
            
            for (int i = 0; i < batchSize; i++) {
                // This is a simulation - in reality would use actual function handles
                futures[i] = batch.addFunctionCall(12345L, new Object[] { i, i + 1 }, "test_call_" + i);
            }

            // Execute batch
            batch.execute();

            // Collect results
            WasmValue[] lastResult = null;
            for (final CompletableFuture<WasmValue[]> future : futures) {
                final WasmValue[] result = future.get();
                bh.consume(result);
                lastResult = result;
            }

            return lastResult;
        }
    }

    /**
     * Benchmarks compilation caching impact on startup performance.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public Module benchmarkCompilationCaching(final Blackhole bh) throws WasmException {
        // Test cache hit vs miss scenarios
        final byte[] bytecode = multipleBytecodes[0]; // Use first bytecode for cache hits
        
        // Check cache first
        final byte[] cached = CompilationCache.loadFromCache(bytecode, "default");
        if (cached != null) {
            bh.consume(cached);
        }

        // Compile module (will be cached if not found)
        final Module compiledModule = engine.compileModule(bytecode);
        
        // Store in cache
        CompilationCache.storeInCache(bytecode, bytecode, "default"); // Simplified for benchmark
        
        bh.consume(compiledModule);
        return compiledModule;
    }

    /**
     * Benchmarks performance monitoring overhead.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public WasmValue[] benchmarkMonitoringOverhead(final Blackhole bh) throws WasmException {
        final long startTime = PerformanceMonitor.startOperation("benchmark_call", "simple_add");
        try {
            final WasmValue[] result = simpleFunction.call(testParams);
            bh.consume(result);
            return result;
        } finally {
            PerformanceMonitor.endOperation("benchmark_call", startTime);
        }
    }

    /**
     * Benchmarks bulk function calls without batching (baseline).
     */
    @Benchmark
    public WasmValue[] benchmarkBulkCallsBaseline(final Blackhole bh) throws WasmException {
        WasmValue[] lastResult = null;
        
        for (int i = 0; i < batchSize; i++) {
            final WasmValue[] params = new WasmValue[] { 
                WasmValue.i32(i), 
                WasmValue.i32(i + 1) 
            };
            final WasmValue[] result = simpleFunction.call(params);
            bh.consume(result);
            lastResult = result;
        }
        
        return lastResult;
    }

    /**
     * Benchmarks parameter marshalling with different strategies.
     */
    @Benchmark
    public Object[] benchmarkMarshallingStrategies(final Blackhole bh) {
        // Test different marshalling patterns
        final WasmValue[] singleParam = new WasmValue[] { WasmValue.i32(42) };
        final WasmValue[] multipleParams = new WasmValue[] { 
            WasmValue.i32(1), WasmValue.i64(2L), WasmValue.f32(3.0f), WasmValue.f64(4.0) 
        };
        final WasmValue[] bulkSameType = new WasmValue[] {
            WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3), 
            WasmValue.i32(4), WasmValue.i32(5), WasmValue.i32(6)
        };

        final Object[] result1 = OptimizedMarshalling.marshalParameters(singleParam);
        final Object[] result2 = OptimizedMarshalling.marshalParameters(multipleParams);
        final Object[] result3 = OptimizedMarshalling.marshalParameters(bulkSameType);

        bh.consume(result1);
        bh.consume(result2);
        bh.consume(result3);

        return result3;
    }

    /**
     * Benchmarks compilation cache with different cache states.
     */
    @Benchmark 
    public Module[] benchmarkCacheVariedContent(final Blackhole bh) throws WasmException {
        final Module[] modules = new Module[3];
        
        // Test cache miss, hit, and eviction scenarios
        for (int i = 0; i < 3; i++) {
            final byte[] bytecode = multipleBytecodes[i % multipleBytecodes.length];
            
            // Try cache first
            final byte[] cached = CompilationCache.loadFromCache(bytecode, "test_config");
            if (cached != null) {
                // Cache hit - simulate faster loading
                bh.consume(cached);
            }
            
            // Compile normally
            modules[i] = engine.compileModule(bytecode);
            
            // Store in cache
            CompilationCache.storeInCache(bytecode, bytecode, "test_config");
        }

        bh.consume(modules);
        return modules;
    }

    /**
     * Generates a test WebAssembly module with unique content.
     */
    private byte[] generateTestModule(final int seed) {
        // Simple WAT module that adds two numbers
        // In reality, this would generate different modules
        // For now, just return the base module with a slight variation
        final byte[] baseModule = SIMPLE_WASM_MODULE.clone();
        if (baseModule.length > 10) {
            // Modify a byte to make it unique
            baseModule[baseModule.length - 1] = (byte) (baseModule[baseModule.length - 1] ^ seed);
        }
        return baseModule;
    }

    /**
     * Benchmarks the complete optimization pipeline.
     */
    @Benchmark
    public WasmValue[] benchmarkOptimizedPipeline(final Blackhole bh) throws WasmException {
        // This benchmark tests the full optimization pipeline:
        // 1. Performance monitoring
        // 2. Optimized marshalling
        // 3. Memory pooling (simulated)
        // 4. Native call with reduced overhead
        
        final long startTime = PerformanceMonitor.startOperation("optimized_pipeline", "full_test");
        try {
            // Get buffer from pool
            final byte[] buffer = bufferPool.borrow();
            
            try {
                // Marshal parameters optimally
                final Object[] marshalledParams = OptimizedMarshalling.marshalParameters(testParams);
                bh.consume(marshalledParams);
                
                // Perform the actual function call
                final WasmValue[] result = simpleFunction.call(testParams);
                bh.consume(result);
                
                return result;
                
            } finally {
                if (buffer != null) {
                    bufferPool.returnObject(buffer);
                }
            }
            
        } finally {
            PerformanceMonitor.endOperation("optimized_pipeline", startTime);
        }
    }

    /**
     * Benchmarks memory allocation patterns to measure GC pressure reduction.
     */
    @Benchmark
    public byte[][] benchmarkAllocationPatterns(final Blackhole bh) {
        final byte[][] results = new byte[batchSize][];
        
        // Test pooled vs non-pooled allocations
        for (int i = 0; i < batchSize; i++) {
            if (i % 2 == 0) {
                // Use pool
                results[i] = bufferPool.borrow();
            } else {
                // Direct allocation
                results[i] = new byte[1024];
            }
            
            if (results[i] != null) {
                results[i][0] = (byte) i;
                bh.consume(results[i]);
            }
        }
        
        // Return pooled objects
        for (int i = 0; i < batchSize; i += 2) {
            if (results[i] != null) {
                bufferPool.returnObject(results[i]);
            }
        }
        
        return results;
    }
}