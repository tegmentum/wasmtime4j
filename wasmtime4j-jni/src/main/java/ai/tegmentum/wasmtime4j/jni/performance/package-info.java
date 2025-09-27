/**
 * Performance optimization and monitoring components for the WebAssembly JNI runtime.
 *
 * <p>This package provides comprehensive performance optimization features designed to minimize
 * native call overhead, reduce memory allocation pressure, and improve overall WebAssembly
 * execution performance.
 *
 * <h2>Performance Targets</h2>
 *
 * <ul>
 *   <li><strong>Native Call Overhead:</strong> <100 nanoseconds for simple operations
 *   <li><strong>Memory Efficiency:</strong> >50% reduction in GC pressure through pooling
 *   <li><strong>Bulk Operations:</strong> >3x performance improvement via call batching
 *   <li><strong>Startup Time:</strong> Significant improvement through compilation caching
 * </ul>
 *
 * <h2>Core Components</h2>
 *
 * <h3>Call Batching ({@link ai.tegmentum.wasmtime4j.jni.performance.CallBatch})</h3>
 *
 * <p>Reduces JNI call overhead by batching multiple operations into single native calls:
 *
 * <pre>{@code
 * try (CallBatch batch = new CallBatch()) {
 *     CompletableFuture<WasmValue[]> result1 = batch.addFunctionCall(func1, params1);
 *     CompletableFuture<WasmValue[]> result2 = batch.addFunctionCall(func2, params2);
 *
 *     batch.execute();
 *
 *     WasmValue[] values1 = result1.get();
 *     WasmValue[] values2 = result2.get();
 * }
 * }</pre>
 *
 * <h3>Memory Pooling ({@link ai.tegmentum.wasmtime4j.jni.performance.NativeObjectPool})</h3>
 *
 * <p>Reduces allocation overhead and GC pressure through object reuse:
 *
 * <pre>{@code
 * NativeObjectPool<byte[]> pool = NativeObjectPool.getPool(
 *     byte[].class, () -> new byte[1024], 16);
 *
 * byte[] buffer = pool.borrow();
 * try {
 *     // Use buffer...
 * } finally {
 *     pool.returnObject(buffer);
 * }
 * }</pre>
 *
 * <h3>Optimized Marshalling ({@link ai.tegmentum.wasmtime4j.jni.performance.OptimizedMarshalling})
 * </h3>
 *
 * <p>Minimizes parameter copying between Java and native code:
 *
 * <ul>
 *   <li>Direct ByteBuffer usage for zero-copy transfers
 *   <li>Bulk marshalling for arrays of similar types
 *   <li>Cached marshalling plans for repeated patterns
 *   <li>Type-specific optimization paths
 * </ul>
 *
 * <h3>Compilation Caching ({@link ai.tegmentum.wasmtime4j.jni.performance.CompilationCache})</h3>
 *
 * <p>Improves startup performance through persistent module caching:
 *
 * <ul>
 *   <li>SHA-256 content-addressed caching
 *   <li>Platform and version aware cache keys
 *   <li>Automatic cache size management and eviction
 *   <li>High cache hit rates for repeated module loading
 * </ul>
 *
 * <h3>Performance Monitoring ({@link ai.tegmentum.wasmtime4j.jni.performance.PerformanceMonitor})
 * </h3>
 *
 * <p>Comprehensive performance tracking and analysis:
 *
 * <pre>{@code
 * long startTime = PerformanceMonitor.startOperation("function_call", "add_numbers");
 * try {
 *     return function.call(parameters);
 * } finally {
 *     PerformanceMonitor.endOperation("function_call", startTime);
 * }
 *
 * // Get performance statistics
 * String stats = PerformanceMonitor.getStatistics();
 * }</pre>
 *
 * <h2>Configuration Options</h2>
 *
 * <h3>System Properties</h3>
 *
 * <table>
 * <tr><th>Property</th><th>Default</th><th>Description</th></tr>
 * <tr><td>wasmtime4j.performance.monitoring</td><td>true</td><td>Enable performance monitoring</td></tr>
 * <tr><td>wasmtime4j.performance.profiling</td><td>false</td><td>Enable detailed profiling</td></tr>
 * <tr><td>wasmtime4j.cache.enabled</td><td>true</td><td>Enable compilation caching</td></tr>
 * <tr><td>wasmtime4j.cache.dir</td><td>~/.wasmtime4j/cache</td><td>Cache directory</td></tr>
 * <tr><td>wasmtime4j.cache.maxSize</td><td>268435456</td><td>Max cache size (256MB)</td></tr>
 * <tr><td>wasmtime4j.cache.maxModules</td><td>1000</td><td>Max cached modules</td></tr>
 * </table>
 *
 * <h2>Performance Best Practices</h2>
 *
 * <h3>Function Calls</h3>
 *
 * <ul>
 *   <li><strong>Batch Operations:</strong> Use {@code CallBatch} for multiple function calls
 *   <li><strong>Parameter Reuse:</strong> Reuse {@code WasmValue} arrays when possible
 *   <li><strong>Avoid Frequent Calls:</strong> Minimize JNI crossing for tight loops
 * </ul>
 *
 * <h3>Memory Management</h3>
 *
 * <ul>
 *   <li><strong>Use Object Pools:</strong> Pool frequently allocated objects
 *   <li><strong>Buffer Reuse:</strong> Reuse ByteBuffers for data transfers
 *   <li><strong>Lazy Initialization:</strong> Defer expensive object creation
 * </ul>
 *
 * <h3>Module Compilation</h3>
 *
 * <ul>
 *   <li><strong>Enable Caching:</strong> Keep compilation cache enabled
 *   <li><strong>Warm Cache:</strong> Pre-compile frequently used modules
 *   <li><strong>Cache Maintenance:</strong> Periodically clean old cache entries
 * </ul>
 *
 * <h3>Monitoring and Profiling</h3>
 *
 * <ul>
 *   <li><strong>Monitor Key Paths:</strong> Track performance-critical operations
 *   <li><strong>Set Performance Targets:</strong> Define acceptable overhead thresholds
 *   <li><strong>Regular Analysis:</strong> Review performance statistics periodically
 * </ul>
 *
 * <h2>Performance Analysis</h2>
 *
 * <h3>JNI Call Overhead Analysis</h3>
 *
 * <p>Monitor native call overhead to ensure it stays below the 100ns target:
 *
 * <pre>{@code
 * double avgOverhead = PerformanceMonitor.getAverageJniOverhead();
 * boolean meetsTarget = PerformanceMonitor.meetsPerformanceTarget();
 *
 * if (!meetsTarget) {
 *     System.out.println("Performance issue detected: " +
 *                        PerformanceMonitor.getPerformanceIssues());
 * }
 * }</pre>
 *
 * <h3>Memory Allocation Analysis</h3>
 *
 * <p>Track allocation patterns to identify GC pressure:
 *
 * <pre>{@code
 * String poolStats = NativeObjectPool.getAllPoolStats();
 * System.out.println(poolStats);
 *
 * // Check for memory leaks
 * String issues = PerformanceMonitor.getPerformanceIssues();
 * if (issues != null) {
 *     System.out.println("Memory issues: " + issues);
 * }
 * }</pre>
 *
 * <h3>Cache Performance Analysis</h3>
 *
 * <p>Monitor compilation cache effectiveness:
 *
 * <pre>{@code
 * double hitRate = CompilationCache.getHitRate();
 * String cacheStats = CompilationCache.getStatistics();
 *
 * if (hitRate < 80.0) {
 *     System.out.println("Low cache hit rate: " + hitRate + "%");
 *     System.out.println(cacheStats);
 * }
 * }</pre>
 *
 * <h2>Benchmarking</h2>
 *
 * <p>The package includes comprehensive JMH benchmarks in the benchmarks module:
 *
 * <ul>
 *   <li>{@code PerformanceOptimizationBenchmark} - Core optimization features
 *   <li>{@code FunctionExecutionBenchmark} - Function call performance
 *   <li>{@code MemoryOperationBenchmark} - Memory operation efficiency
 * </ul>
 *
 * <p>Run benchmarks to validate performance improvements:
 *
 * <pre>{@code
 * ./mvnw exec:java -pl wasmtime4j-benchmarks \
 *   -Dexec.mainClass=org.openjdk.jmh.Main \
 *   -Dexec.args="PerformanceOptimizationBenchmark"
 * }</pre>
 *
 * <h2>Troubleshooting Performance Issues</h2>
 *
 * <h3>High JNI Overhead</h3>
 *
 * <ul>
 *   <li>Enable call batching for bulk operations
 *   <li>Check for excessive parameter marshalling
 *   <li>Verify optimized marshalling is being used
 *   <li>Review function call patterns for optimization opportunities
 * </ul>
 *
 * <h3>High GC Pressure</h3>
 *
 * <ul>
 *   <li>Increase object pool sizes
 *   <li>Check for object pool leaks
 *   <li>Verify proper resource cleanup
 *   <li>Analyze allocation patterns in performance monitor
 * </ul>
 *
 * <h3>Poor Cache Performance</h3>
 *
 * <ul>
 *   <li>Verify cache is enabled and writable
 *   <li>Check cache size limits and eviction policies
 *   <li>Ensure consistent module bytecode
 *   <li>Review cache key generation for uniqueness
 * </ul>
 *
 * <h3>Monitoring Overhead</h3>
 *
 * <ul>
 *   <li>Disable detailed profiling in production
 *   <li>Reduce monitoring granularity for hot paths
 *   <li>Use selective monitoring for critical operations only
 *   <li>Consider disabling monitoring entirely if overhead is excessive
 * </ul>
 *
 * @since 1.0.0
 */
package ai.tegmentum.wasmtime4j.jni.performance;
