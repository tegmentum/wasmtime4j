# Performance Guide

This guide provides comprehensive performance optimization recommendations for Wasmtime4j, including benchmarking results, best practices, and troubleshooting tips.

## Table of Contents
- [Performance Overview](#performance-overview)
- [Runtime Selection Impact](#runtime-selection-impact)
- [Benchmarking Results](#benchmarking-results)
- [Optimization Strategies](#optimization-strategies)
- [Memory Management](#memory-management)
- [Function Call Optimization](#function-call-optimization)
- [Module Caching](#module-caching)
- [Concurrency and Scaling](#concurrency-and-scaling)
- [Profiling and Monitoring](#profiling-and-monitoring)
- [Performance Testing](#performance-testing)

## Performance Overview

Wasmtime4j provides two runtime implementations with different performance characteristics:

- **JNI Implementation**: Lower overhead, consistent performance across all Java versions
- **Panama FFI Implementation**: Slightly higher initialization overhead but better type safety and potentially better long-term performance on Java 23+

### Expected Performance Characteristics

Based on our benchmarking results (hardware-dependent):

| Operation Category | JNI Performance | Panama Performance | Notes |
|-------------------|-----------------|-------------------|-------|
| Runtime Initialization | 100-1K ops/sec | 50-500 ops/sec | Panama has higher startup cost |
| Function Calls | 1M-10M ops/sec | 800K-8M ops/sec | Depends on function complexity |
| Memory Operations | 100K-1M ops/sec | 80K-800K ops/sec | Bulk operations are faster |
| Module Compilation | 1K-10K ops/sec | 1K-10K ops/sec | Similar performance |

## Runtime Selection Impact

### Automatic Selection Performance

```java
// Let Wasmtime4j choose the optimal runtime
try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
    RuntimeInfo info = runtime.getRuntimeInfo();
    System.out.println("Selected runtime: " + info.getRuntimeType());
    System.out.println("Performance profile: " + info.getPerformanceProfile());
}
```

### Manual Runtime Selection

```java
// For maximum throughput on Java 8-22
WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI);

// For type safety and future performance on Java 23+
WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
```

## Benchmarking Results

### Running Performance Tests

Use the included benchmark suite to measure performance on your hardware:

```bash
# Quick performance check (30 seconds)
cd wasmtime4j-benchmarks
./run-benchmarks.sh all quick

# Comprehensive analysis (10-15 minutes)
./run-benchmarks.sh comparison comprehensive --output my-results.json

# Focus on specific operations
./run-benchmarks.sh function production
```

### Interpreting Results

Benchmark results are provided in operations per second (ops/sec):

```
Benchmark                                    Mode  Score    Error   Units
FunctionExecutionBenchmark.simpleFunctionCall  thrpt  5432.1 ± 123.4  ops/sec
MemoryOperationBenchmark.bulkMemoryRead        thrpt  1234.5 ± 45.6   ops/sec
```

Higher scores indicate better performance. The error margin represents the 99.9% confidence interval.

## Optimization Strategies

### 1. Runtime Warm-up

JIT compilation requires warm-up for optimal performance:

```java
public class OptimizedWebAssemblyExecutor {
    private final WasmRuntime runtime;
    private final Module module;
    private final Instance instance;
    
    public OptimizedWebAssemblyExecutor(byte[] moduleBytes) throws WasmException {
        this.runtime = WasmRuntimeFactory.create();
        
        // Create engine with optimization settings
        EngineConfig config = EngineConfig.builder()
            .optimizationLevel(OptimizationLevel.SPEED)
            .enableProfiling(true)  // For production monitoring
            .build();
            
        Engine engine = runtime.createEngine(config);
        this.module = runtime.compileModule(engine, moduleBytes);
        this.instance = runtime.instantiate(module);
        
        // Warm up critical functions
        warmUpFunctions();
    }
    
    private void warmUpFunctions() throws WasmException {
        WasmFunction criticalFunction = instance.getFunction("critical_function");
        
        // Execute function multiple times to trigger JIT compilation
        WasmValue[] warmupArgs = {WasmValue.i32(0)};
        for (int i = 0; i < 10000; i++) {
            criticalFunction.call(warmupArgs);
        }
    }
}
```

### 2. Module Caching Strategy

Avoid recompiling modules by implementing effective caching:

```java
public class HighPerformanceModuleManager {
    private final Map<String, Module> moduleCache = new ConcurrentHashMap<>();
    private final Map<String, Instance> instancePool = new ConcurrentHashMap<>();
    private final Engine optimizedEngine;
    
    public HighPerformanceModuleManager() throws WasmException {
        // Configure engine for maximum performance
        EngineConfig config = EngineConfig.builder()
            .optimizationLevel(OptimizationLevel.SPEED)
            .enableDebugInfo(false)  // Disable for production
            .enableProfiling(true)   // Enable for monitoring
            .build();
            
        WasmRuntime runtime = WasmRuntimeFactory.create();
        this.optimizedEngine = runtime.createEngine(config);
    }
    
    public Module getOrCompileModule(String moduleId, byte[] wasmBytes) throws WasmException {
        return moduleCache.computeIfAbsent(moduleId, id -> {
            try {
                long startTime = System.nanoTime();
                Module module = optimizedEngine.compileModule(wasmBytes);
                long compilationTime = System.nanoTime() - startTime;
                
                // Log compilation metrics
                System.out.printf("Module %s compiled in %.2f ms%n", 
                                moduleId, compilationTime / 1_000_000.0);
                
                return module;
            } catch (WasmException e) {
                throw new RuntimeException("Failed to compile module: " + moduleId, e);
            }
        });
    }
}
```

## Memory Management

### Efficient Memory Operations

```java
public class OptimizedMemoryOperations {
    private final WasmMemory memory;
    private final ByteBuffer directBuffer;
    
    public OptimizedMemoryOperations(WasmMemory memory) {
        this.memory = memory;
        // Get direct access for bulk operations
        this.directBuffer = memory.getBuffer();
    }
    
    // Bulk write - much faster than individual writes
    public void writeBulk(int offset, byte[] data) {
        if (data.length > 1024) {
            // Use direct buffer for large writes
            directBuffer.position(offset);
            directBuffer.put(data);
        } else {
            // Use standard API for small writes
            memory.write(offset, data);
        }
    }
    
    // Memory-mapped file loading
    public void loadFromFile(String filename, int offset) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filename, "r");
             FileChannel channel = file.getChannel()) {
            
            // Map file into memory for efficient reading
            MappedByteBuffer mappedBuffer = channel.map(
                FileChannel.MapMode.READ_ONLY, 0, file.length());
            
            // Direct copy to WebAssembly memory
            directBuffer.position(offset);
            directBuffer.put(mappedBuffer);
        }
    }
    
    // Zero-copy memory sharing (when possible)
    public ByteBuffer getSharedBuffer(int offset, int length) {
        return directBuffer.slice(offset, length);
    }
}
```

### Memory Growth Strategies

```java
// Pre-allocate memory to avoid growth overhead
MemoryConfig memoryConfig = MemoryConfig.builder()
    .initialPages(64)    // Start with 64 pages (4MB)
    .maximumPages(1024)  // Allow growth to 64MB
    .build();

WasmMemory memory = WasmMemory.create(memoryConfig);
```

## Function Call Optimization

### Batch Function Calls

```java
public class BatchFunctionExecutor {
    private final WasmFunction batchFunction;
    private final WasmMemory memory;
    private final ByteBuffer parameterBuffer;
    
    public BatchFunctionExecutor(Instance instance) throws WasmException {
        this.batchFunction = instance.getFunction("batch_process");
        this.memory = instance.getMemory("memory");
        
        // Pre-allocate parameter buffer
        this.parameterBuffer = ByteBuffer.allocateDirect(8192);
    }
    
    public int[] processBatch(int[] inputData) throws WasmException {
        // Write batch data to memory in one operation
        parameterBuffer.clear();
        for (int value : inputData) {
            parameterBuffer.putInt(value);
        }
        
        // Write buffer to WebAssembly memory
        int inputOffset = 0;
        memory.write(inputOffset, parameterBuffer.array(), 0, parameterBuffer.position());
        
        // Single function call for entire batch
        WasmValue[] args = {
            WasmValue.i32(inputOffset),      // Input data offset
            WasmValue.i32(inputData.length), // Input count
            WasmValue.i32(inputOffset + 4096) // Output offset
        };
        
        WasmValue[] results = batchFunction.call(args);
        int outputCount = results[0].asI32();
        
        // Read results in bulk
        byte[] outputBytes = memory.read(inputOffset + 4096, outputCount * 4);
        ByteBuffer outputBuffer = ByteBuffer.wrap(outputBytes);
        
        int[] output = new int[outputCount];
        for (int i = 0; i < outputCount; i++) {
            output[i] = outputBuffer.getInt();
        }
        
        return output;
    }
}
```

### Function Call Pooling

```java
public class PooledFunctionExecutor {
    private final BlockingQueue<FunctionContext> contextPool;
    
    private static class FunctionContext {
        final Instance instance;
        final WasmFunction function;
        final WasmMemory memory;
        
        FunctionContext(Module module) throws WasmException {
            WasmRuntime runtime = WasmRuntimeFactory.create();
            this.instance = runtime.instantiate(module);
            this.function = instance.getFunction("target_function");
            this.memory = instance.getMemory("memory");
        }
    }
    
    public PooledFunctionExecutor(Module module, int poolSize) throws WasmException {
        this.contextPool = new ArrayBlockingQueue<>(poolSize);
        
        // Pre-create instances for the pool
        for (int i = 0; i < poolSize; i++) {
            contextPool.offer(new FunctionContext(module));
        }
    }
    
    public WasmValue[] executeFunction(WasmValue[] args) throws InterruptedException, WasmException {
        FunctionContext context = contextPool.take();
        try {
            return context.function.call(args);
        } finally {
            contextPool.offer(context);
        }
    }
}
```

## Module Caching

### Advanced Caching with TTL

```java
public class TTLModuleCache {
    private final Map<String, CachedModule> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupService = 
        Executors.newSingleThreadScheduledExecutor();
    
    private static class CachedModule {
        final Module module;
        final long creationTime;
        final long ttlMillis;
        
        CachedModule(Module module, long ttlMillis) {
            this.module = module;
            this.creationTime = System.currentTimeMillis();
            this.ttlMillis = ttlMillis;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - creationTime > ttlMillis;
        }
    }
    
    public TTLModuleCache() {
        // Cleanup expired entries every 5 minutes
        cleanupService.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);
    }
    
    public Module getModule(String moduleId, byte[] wasmBytes, long ttlMillis) 
            throws WasmException {
        CachedModule cached = cache.get(moduleId);
        
        if (cached != null && !cached.isExpired()) {
            return cached.module;
        }
        
        // Compile new module
        WasmRuntime runtime = WasmRuntimeFactory.create();
        Engine engine = runtime.createEngine();
        Module module = runtime.compileModule(engine, wasmBytes);
        
        // Cache with TTL
        cache.put(moduleId, new CachedModule(module, ttlMillis));
        
        return module;
    }
    
    private void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
```

## Concurrency and Scaling

### WebAssembly Worker Pool

```java
public class WebAssemblyWorkerPool {
    private final ExecutorService executorService;
    private final List<WorkerContext> workers;
    private final AtomicInteger nextWorker = new AtomicInteger(0);
    
    private static class WorkerContext {
        final WasmRuntime runtime;
        final Engine engine;
        final Map<String, Module> moduleCache;
        
        WorkerContext() throws WasmException {
            this.runtime = WasmRuntimeFactory.create();
            
            EngineConfig config = EngineConfig.builder()
                .optimizationLevel(OptimizationLevel.SPEED)
                .build();
                
            this.engine = runtime.createEngine(config);
            this.moduleCache = new ConcurrentHashMap<>();
        }
    }
    
    public WebAssemblyWorkerPool(int workerCount) throws WasmException {
        this.executorService = Executors.newFixedThreadPool(workerCount);
        this.workers = new ArrayList<>(workerCount);
        
        // Create worker contexts
        for (int i = 0; i < workerCount; i++) {
            workers.add(new WorkerContext());
        }
    }
    
    public CompletableFuture<WasmValue[]> executeAsync(
            String moduleId, byte[] moduleBytes, String functionName, WasmValue[] args) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Round-robin worker selection
                int workerIndex = nextWorker.getAndIncrement() % workers.size();
                WorkerContext worker = workers.get(workerIndex);
                
                // Get or compile module
                Module module = worker.moduleCache.computeIfAbsent(moduleId, id -> {
                    try {
                        return worker.runtime.compileModule(worker.engine, moduleBytes);
                    } catch (WasmException e) {
                        throw new RuntimeException(e);
                    }
                });
                
                // Execute function
                Instance instance = worker.runtime.instantiate(module);
                WasmFunction function = instance.getFunction(functionName);
                return function.call(args);
                
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }
}
```

## Profiling and Monitoring

### Performance Metrics Collection

```java
public class PerformanceMonitor {
    private final MeterRegistry meterRegistry;
    private final Timer.Sample executionTimer;
    private final Counter errorCounter;
    private final Gauge memoryGauge;
    
    public PerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.errorCounter = Counter.builder("wasm.execution.errors")
            .description("WebAssembly execution errors")
            .register(meterRegistry);
            
        this.memoryGauge = Gauge.builder("wasm.memory.usage")
            .description("WebAssembly memory usage")
            .register(meterRegistry, this, PerformanceMonitor::getCurrentMemoryUsage);
    }
    
    public WasmValue[] executeWithMetrics(WasmFunction function, WasmValue[] args) {
        Timer.Sample timer = Timer.start(meterRegistry);
        
        try {
            WasmValue[] results = function.call(args);
            
            timer.stop(Timer.builder("wasm.function.execution")
                .description("WebAssembly function execution time")
                .register(meterRegistry));
                
            return results;
            
        } catch (Exception e) {
            errorCounter.increment();
            timer.stop(Timer.builder("wasm.function.execution.failed")
                .description("Failed WebAssembly function execution time")
                .register(meterRegistry));
            throw e;
        }
    }
    
    private double getCurrentMemoryUsage() {
        // Return current memory usage in MB
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) 
               / (1024.0 * 1024.0);
    }
}
```

### JVM Tuning

For optimal performance, configure the JVM appropriately:

```bash
# For high-throughput applications
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+UseStringDeduplication \
     -Xms4g -Xmx4g \
     -XX:+UnlockExperimentalVMOptions \
     -XX:+UseZGC \
     YourApplication

# For low-latency applications  
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=50 \
     -XX:G1HeapRegionSize=16m \
     -XX:+UseLargePages \
     -Xms8g -Xmx8g \
     YourApplication

# For Panama FFI (Java 23+)
java --enable-preview \
     --enable-native-access=ALL-UNNAMED \
     -XX:+UnlockExperimentalVMOptions \
     -XX:+UseZGC \
     YourApplication
```

## Performance Testing

### Automated Performance Regression Testing

```java
public class PerformanceRegressionTest {
    private static final double PERFORMANCE_THRESHOLD = 0.95; // Allow 5% degradation
    
    @Test
    public void testFunctionCallPerformance() throws Exception {
        // Load baseline performance data
        PerformanceBaseline baseline = loadBaseline();
        
        // Run current performance test
        double currentPerformance = measureFunctionCallPerformance();
        
        // Check for regression
        double performanceRatio = currentPerformance / baseline.getFunctionCallPerformance();
        
        assertThat(performanceRatio)
            .withFailMessage("Performance regression detected: %.2f%% of baseline", 
                           performanceRatio * 100)
            .isGreaterThan(PERFORMANCE_THRESHOLD);
    }
    
    private double measureFunctionCallPerformance() throws Exception {
        // Use JMH for accurate measurement
        Options opts = new OptionsBuilder()
            .include(FunctionExecutionBenchmark.class.getSimpleName())
            .mode(Mode.Throughput)
            .timeUnit(TimeUnit.SECONDS)
            .warmupIterations(5)
            .measurementIterations(10)
            .forks(1)
            .build();
            
        RunResult result = new Runner(opts).runSingle();
        return result.getPrimaryResult().getScore();
    }
}
```

## Best Practices Summary

1. **Choose the Right Runtime**: Use JNI for maximum performance, Panama for type safety
2. **Warm Up Critical Paths**: Pre-execute functions to trigger JIT compilation
3. **Cache Compiled Modules**: Avoid repeated compilation overhead
4. **Use Bulk Operations**: Batch memory operations and function calls
5. **Monitor Performance**: Track key metrics and watch for regressions
6. **Tune JVM Settings**: Configure garbage collector and memory settings appropriately
7. **Pool Resources**: Reuse instances and contexts when possible
8. **Profile Regularly**: Use the benchmark suite to measure performance on your workloads

## Troubleshooting Performance Issues

### Common Performance Problems

1. **Cold Start Performance**: Implement proper warm-up routines
2. **Memory Allocation Overhead**: Use direct buffers and pre-allocation
3. **Garbage Collection Pauses**: Tune GC settings and reduce allocation rate
4. **Function Call Overhead**: Batch operations and cache results
5. **Module Compilation Time**: Implement effective caching strategies

### Diagnostic Tools

```bash
# Profile with JVM tools
java -XX:+PrintGC -XX:+PrintGCDetails YourApp

# Use Java Flight Recorder
java -XX:+FlightRecorder -XX:StartFlightRecording=duration=60s YourApp

# Profile with external tools
java -javaagent:path/to/profiler.jar YourApp
```

For more detailed performance analysis, see our [benchmarking documentation](../wasmtime4j-benchmarks/README.md).