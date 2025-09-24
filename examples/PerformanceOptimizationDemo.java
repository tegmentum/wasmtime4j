package examples;

import ai.tegmentum.wasmtime4j.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.time.Duration;
import java.time.Instant;

/**
 * Performance optimization demonstration showing:
 * - Instance pooling for high-throughput scenarios
 * - Memory management optimization
 * - Concurrent execution patterns
 * - Compilation caching strategies
 * - Performance monitoring and profiling
 */
public class PerformanceOptimizationDemo {

    private final WasmRuntime runtime;
    private final Engine optimizedEngine;
    private final InstancePool instancePool;
    private final CompletableFuture<Void> warmupComplete;

    public PerformanceOptimizationDemo() throws Exception {
        System.out.println("Initializing Performance Optimization Demo...");

        // Create runtime with performance-focused configuration
        this.runtime = WasmRuntime.builder()
            .enableMetrics(true)
            .enableProfiling(true)
            .build();

        // Configure engine for maximum performance
        EngineConfig engineConfig = EngineConfig.builder()
            .optimizationLevel(OptimizationLevel.SPEED)
            .strategy(CompilationStrategy.CRANELIFT)
            .enableInstancePooling(true)
            .maxPooledInstances(100)
            .enableModuleCache(true)
            .moduleCacheSize(50)
            .maxMemorySize(1_000_000_000L) // 1GB
            .enableParallelCompilation(true)
            .build();

        this.optimizedEngine = runtime.createEngine(engineConfig);

        // Create instance pool for high-throughput operations
        byte[] moduleBytes = createPerformanceTestModule();
        this.instancePool = InstancePool.builder()
            .engine(optimizedEngine)
            .moduleBytes(moduleBytes)
            .initialSize(20)
            .maxSize(100)
            .build();

        // Perform warmup
        this.warmupComplete = performWarmup();

        System.out.println("Performance demo initialized successfully");
        System.out.println("Runtime type: " + runtime.getRuntimeType());
    }

    /**
     * Demonstrates high-throughput processing using instance pooling
     */
    public void demonstrateHighThroughput() throws Exception {
        System.out.println("\n=== High Throughput Demonstration ===");

        // Wait for warmup to complete
        warmupComplete.get();

        int numOperations = 10_000;
        int numThreads = Runtime.getRuntime().availableProcessors();

        System.out.println("Processing " + numOperations + " operations with " + numThreads + " threads");

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicLong totalOperations = new AtomicLong(0);
        AtomicLong totalTime = new AtomicLong(0);

        Instant start = Instant.now();

        // Submit tasks
        CompletableFuture<?>[] futures = new CompletableFuture[numOperations];
        for (int i = 0; i < numOperations; i++) {
            final int operationId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    performHighThroughputOperation(operationId, totalOperations, totalTime);
                } catch (Exception e) {
                    System.err.println("Operation " + operationId + " failed: " + e.getMessage());
                }
            }, executor);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures).get();

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        System.out.println("Completed " + totalOperations.get() + " operations");
        System.out.println("Total time: " + duration.toMillis() + " ms");
        System.out.println("Operations per second: " + (totalOperations.get() * 1000.0 / duration.toMillis()));
        System.out.println("Average operation time: " + (totalTime.get() / totalOperations.get()) + " μs");

        executor.shutdown();
    }

    /**
     * Demonstrates memory-efficient processing patterns
     */
    public void demonstrateMemoryOptimization() throws Exception {
        System.out.println("\n=== Memory Optimization Demonstration ===");

        try (Store store = optimizedEngine.createStore()) {
            // Configure store for memory efficiency
            store.setMemoryLimit(100 * 1024 * 1024); // 100MB limit
            store.setFuelLimit(10_000_000); // Computation limit

            byte[] moduleBytes = createMemoryTestModule();
            Module module = Module.fromBytes(optimizedEngine, moduleBytes);
            Instance instance = Instance.create(store, module);

            Memory memory = instance.getMemory("memory");
            if (memory != null) {
                System.out.println("Initial memory size: " + memory.size() + " pages");

                // Demonstrate memory growth
                Function growMemory = instance.getFunction("grow_memory");
                if (growMemory != null) {
                    for (int i = 0; i < 5; i++) {
                        Value[] result = growMemory.call(Value.i32(10)); // Grow by 10 pages
                        System.out.println("Memory growth " + (i + 1) + ": " + result[0].asI32() + " pages");
                    }
                }

                // Demonstrate bulk memory operations
                demonstrateBulkMemoryOperations(memory);

                // Show memory statistics
                showMemoryStatistics(store);
            }
        }
    }

    /**
     * Demonstrates concurrent execution with thread safety
     */
    public void demonstrateConcurrentExecution() throws Exception {
        System.out.println("\n=== Concurrent Execution Demonstration ===");

        int numThreads = 8;
        int operationsPerThread = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numThreads);

        AtomicLong totalExecutions = new AtomicLong(0);
        Instant start = Instant.now();

        // Start concurrent workers
        for (int threadId = 0; threadId < numThreads; threadId++) {
            final int id = threadId;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    for (int op = 0; op < operationsPerThread; op++) {
                        performConcurrentOperation(id, op);
                        totalExecutions.incrementAndGet();
                    }

                } catch (Exception e) {
                    System.err.println("Thread " + id + " failed: " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for completion
        completionLatch.await();
        Instant end = Instant.now();

        Duration duration = Duration.between(start, end);
        System.out.println("Concurrent execution completed:");
        System.out.println("Total executions: " + totalExecutions.get());
        System.out.println("Duration: " + duration.toMillis() + " ms");
        System.out.println("Executions per second: " + (totalExecutions.get() * 1000.0 / duration.toMillis()));

        executor.shutdown();
    }

    /**
     * Demonstrates performance profiling and metrics collection
     */
    public void demonstratePerformanceProfiling() throws Exception {
        System.out.println("\n=== Performance Profiling Demonstration ===");

        if (!runtime.supportsMetrics()) {
            System.out.println("Metrics not supported in this runtime");
            return;
        }

        RuntimeMetrics beforeMetrics = runtime.getMetrics();

        // Perform various operations to generate metrics
        for (int i = 0; i < 100; i++) {
            try (PooledInstance instance = instancePool.acquire()) {
                Function benchmarkFunc = instance.getFunction("benchmark_operation");
                if (benchmarkFunc != null) {
                    benchmarkFunc.call(Value.i32(i));
                }
            }
        }

        RuntimeMetrics afterMetrics = runtime.getMetrics();

        // Display performance metrics
        System.out.println("=== Performance Metrics ===");
        System.out.println("Compilations: " + (afterMetrics.getTotalCompilations() - beforeMetrics.getTotalCompilations()));
        System.out.println("Instantiations: " + (afterMetrics.getTotalInstantiations() - beforeMetrics.getTotalInstantiations()));
        System.out.println("Cache hits: " + (afterMetrics.getCacheHits() - beforeMetrics.getCacheHits()));
        System.out.println("Memory usage delta: " + (afterMetrics.getMemoryUsage() - beforeMetrics.getMemoryUsage()) + " bytes");

        if (runtime.supportsProfiling()) {
            ProfilingData profiling = runtime.getProfilingData();
            System.out.println("=== Profiling Data ===");
            System.out.println("Hot functions: " + profiling.getHotFunctions().size());
            System.out.println("Total samples: " + profiling.getTotalSamples());
        }
    }

    private CompletableFuture<Void> performWarmup() {
        return CompletableFuture.runAsync(() -> {
            System.out.println("Performing warmup...");
            try {
                // Warmup instance pool
                for (int i = 0; i < 10; i++) {
                    try (PooledInstance instance = instancePool.acquire()) {
                        Function warmupFunc = instance.getFunction("warmup");
                        if (warmupFunc != null) {
                            warmupFunc.call();
                        }
                    }
                }
                System.out.println("Warmup completed");
            } catch (Exception e) {
                System.err.println("Warmup failed: " + e.getMessage());
            }
        });
    }

    private void performHighThroughputOperation(int operationId, AtomicLong totalOps, AtomicLong totalTime) throws Exception {
        Instant start = Instant.now();

        try (PooledInstance instance = instancePool.acquire()) {
            Function operation = instance.getFunction("fast_operation");
            if (operation != null) {
                Value[] result = operation.call(Value.i32(operationId));
                // Process result...
            }
        }

        Instant end = Instant.now();
        long duration = Duration.between(start, end).toNanos() / 1000; // microseconds

        totalOps.incrementAndGet();
        totalTime.addAndGet(duration);
    }

    private void performConcurrentOperation(int threadId, int operationId) throws Exception {
        try (PooledInstance instance = instancePool.acquire()) {
            Function concurrentOp = instance.getFunction("concurrent_operation");
            if (concurrentOp != null) {
                concurrentOp.call(Value.i32(threadId), Value.i32(operationId));
            }
        }
    }

    private void demonstrateBulkMemoryOperations(Memory memory) {
        System.out.println("=== Bulk Memory Operations ===");

        // Large data transfer test
        byte[] largeData = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        Instant start = Instant.now();

        // Write in optimized chunks
        int chunkSize = 64 * 1024; // 64KB chunks
        for (int offset = 0; offset < largeData.length; offset += chunkSize) {
            int length = Math.min(chunkSize, largeData.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(largeData, offset, chunk, 0, length);
            memory.write(offset, chunk);
        }

        Instant end = Instant.now();
        long writeTime = Duration.between(start, end).toMillis();

        System.out.println("Bulk write (1MB): " + writeTime + " ms");
        System.out.println("Write throughput: " + (largeData.length / writeTime) + " KB/ms");

        // Read performance test
        start = Instant.now();
        byte[] readData = memory.read(0, largeData.length);
        end = Instant.now();
        long readTime = Duration.between(start, end).toMillis();

        System.out.println("Bulk read (1MB): " + readTime + " ms");
        System.out.println("Read throughput: " + (readData.length / readTime) + " KB/ms");
    }

    private void showMemoryStatistics(Store store) throws WasmException {
        if (store.supportsMetrics()) {
            StoreMetrics metrics = store.getMetrics();
            System.out.println("=== Memory Statistics ===");
            System.out.println("Total memory usage: " + metrics.getTotalMemoryUsage() + " bytes");
            System.out.println("Peak memory usage: " + metrics.getPeakMemoryUsage() + " bytes");
            System.out.println("Memory allocations: " + metrics.getMemoryAllocations());
            System.out.println("Memory deallocations: " + metrics.getMemoryDeallocations());
        }
    }

    private byte[] createPerformanceTestModule() {
        // In practice, this would be a real performance test module
        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // WASM magic
            0x01, 0x00, 0x00, 0x00  // WASM version
        };
    }

    private byte[] createMemoryTestModule() {
        // In practice, this would be a real memory test module
        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // WASM magic
            0x01, 0x00, 0x00, 0x00  // WASM version
        };
    }

    public void cleanup() throws Exception {
        if (instancePool != null) {
            instancePool.close();
        }
        if (optimizedEngine != null) {
            optimizedEngine.close();
        }
        if (runtime != null) {
            runtime.close();
        }
    }

    public static void main(String[] args) {
        PerformanceOptimizationDemo demo = null;

        try {
            demo = new PerformanceOptimizationDemo();

            // Run performance demonstrations
            demo.demonstrateHighThroughput();
            demo.demonstrateMemoryOptimization();
            demo.demonstrateConcurrentExecution();
            demo.demonstratePerformanceProfiling();

            System.out.println("\n=== Performance optimization demonstrations completed ===");

        } catch (Exception e) {
            System.err.println("Performance demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (demo != null) {
                try {
                    demo.cleanup();
                } catch (Exception e) {
                    System.err.println("Cleanup failed: " + e.getMessage());
                }
            }
        }
    }
}