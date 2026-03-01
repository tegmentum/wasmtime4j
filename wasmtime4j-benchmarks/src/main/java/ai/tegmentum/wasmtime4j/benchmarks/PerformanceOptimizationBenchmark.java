/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
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
 * Comprehensive performance benchmarks measuring memory allocation patterns, GC pressure,
 * throughput, and latency for WebAssembly operations.
 *
 * <p>This benchmark class focuses on the core JMH requirements for performance analysis:
 *
 * <ul>
 *   <li>Memory allocation and GC pressure measurement
 *   <li>Throughput and latency benchmarking for critical paths
 *   <li>Performance comparison between JNI and Panama implementations
 *   <li>Host function call overhead analysis
 *   <li>Comprehensive performance regression detection
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1g", "-Xmx1g", "-XX:+UseG1GC", "-XX:+UnlockExperimentalVMOptions"})
public class PerformanceOptimizationBenchmark extends BenchmarkBase {

  /** Runtime implementation to benchmark. */
  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  /** Operation count for batch testing. */
  @Param({"1", "10", "100", "1000"})
  private int operationCount;

  /** WebAssembly runtime components. */
  private WasmRuntime runtime;

  private Engine engine;
  private Store store;
  private Module module;
  private Instance instance;
  private WasmFunction addFunction;

  /** Test data for benchmarks. */
  private WasmValue[] testParams;

  private List<byte[]> allocatedBuffers;

  /** Performance tracking fields. */
  private MemoryMXBean memoryBean;

  private List<GarbageCollectorMXBean> gcBeans;

  /**
   * Sets up the benchmark trial by initializing runtime components and performance monitoring.
   *
   * @throws WasmException if initialization fails
   */
  @Setup(Level.Trial)
  public void setupTrial() throws WasmException {
    // Initialize performance monitoring
    memoryBean = ManagementFactory.getMemoryMXBean();
    gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    // Initialize runtime components
    final RuntimeType runtimeType = RuntimeType.valueOf(runtimeTypeName);
    runtime = createRuntime(runtimeType);
    engine = createEngine(runtime);
    store = createStore(engine);

    // Create test module from WAT
    module = engine.compileWat(SIMPLE_WAT_MODULE);
    instance = module.instantiate(store);

    // Get test function
    addFunction =
        instance
            .getFunction("add")
            .orElseThrow(() -> new RuntimeException("Test function 'add' not found"));

    // Setup test parameters
    testParams = new WasmValue[] {WasmValue.i32(10), WasmValue.i32(20)};

    // Initialize buffer list for memory allocation testing
    allocatedBuffers = new ArrayList<>();

    System.out.printf(
        "Setup completed for %s runtime with %d operations%n", runtimeType, operationCount);
  }

  /**
   * Tears down the benchmark trial by cleaning up resources and printing performance statistics.
   */
  @TearDown(Level.Trial)
  public void tearDownTrial() throws Exception {
    // Clean up allocated buffers
    if (allocatedBuffers != null) {
      allocatedBuffers.clear();
    }

    // Clean up WebAssembly resources
    if (instance != null) {
      instance.close();
    }
    if (module != null) {
      module.close();
    }
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    if (runtime != null) {
      runtime.close();
    }

    // Print GC statistics
    System.out.println("\n=== Performance Statistics ===");
    printMemoryStatistics();
    printGcStatistics();
  }

  /** Benchmarks native call overhead for simple operations. Target: <100 nanoseconds per call. */
  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public WasmValue[] benchmarkNativeCallOverhead(final Blackhole bh) throws WasmException {
    final WasmValue[] result = addFunction.call(testParams);
    bh.consume(result);
    return result;
  }

  /** Benchmarks memory allocation patterns to measure GC pressure. */
  @Benchmark
  public byte[][] benchmarkMemoryAllocationPressure(final Blackhole bh) {
    final MemoryUsage beforeHeap = memoryBean.getHeapMemoryUsage();
    final long beforeGc = getTotalGcCollections();

    final byte[][] results = new byte[operationCount][];

    // Allocate memory to simulate GC pressure
    for (int i = 0; i < operationCount; i++) {
      results[i] = new byte[1024]; // 1KB allocations
      results[i][0] = (byte) i;
      bh.consume(results[i]);

      // Add to tracking list
      allocatedBuffers.add(results[i]);
    }

    // Measure GC impact
    final MemoryUsage afterHeap = memoryBean.getHeapMemoryUsage();
    final long afterGc = getTotalGcCollections();

    // Force some cleanup periodically
    if (allocatedBuffers.size() > 10000) {
      allocatedBuffers.clear();
    }

    bh.consume(afterHeap.getUsed() - beforeHeap.getUsed());
    bh.consume(afterGc - beforeGc);

    return results;
  }

  /** Benchmarks bulk function calls measuring throughput and latency. */
  @Benchmark
  public WasmValue[] benchmarkBulkFunctionCalls(final Blackhole bh) throws WasmException {
    final long startTime = System.nanoTime();
    WasmValue[] lastResult = null;

    for (int i = 0; i < operationCount; i++) {
      final WasmValue[] params = new WasmValue[] {WasmValue.i32(i), WasmValue.i32(i + 1)};
      final WasmValue[] result = addFunction.call(params);
      bh.consume(result);
      lastResult = result;
    }

    final long duration = System.nanoTime() - startTime;
    bh.consume(duration);

    return lastResult;
  }

  /** Benchmarks host function call overhead patterns. */
  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public WasmValue[] benchmarkHostFunctionCallOverhead(final Blackhole bh) throws WasmException {
    // Measure just the FFI boundary crossing overhead
    final WasmValue a = WasmValue.i32(42);
    final WasmValue b = WasmValue.i32(24);

    final WasmValue[] result = addFunction.call(a, b);
    bh.consume(result);

    return result;
  }

  /** Benchmarks module compilation throughput. */
  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public Module benchmarkModuleCompilation(final Blackhole bh) throws WasmException {
    // Compile the same module repeatedly to measure compilation throughput
    final Module compiledModule = engine.compileWat(SIMPLE_WAT_MODULE);
    bh.consume(compiledModule);

    // Clean up immediately to avoid resource leaks
    compiledModule.close();

    return compiledModule;
  }

  /** Benchmarks instance creation and instantiation performance. */
  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void benchmarkInstanceCreation(final Blackhole bh) throws WasmException {
    // Use a fresh store per call to avoid instance count resource limits
    try (final Store freshStore = createStore(engine);
        final Instance testInstance = module.instantiate(freshStore)) {
      bh.consume(testInstance);
    }
  }

  /** Benchmarks parameter marshalling overhead with different value types. */
  @Benchmark
  public WasmValue[] benchmarkParameterMarshalling(final Blackhole bh) throws WasmException {
    // Test different parameter patterns
    final WasmValue[] singleI32 = new WasmValue[] {WasmValue.i32(42)};
    final WasmValue[] mixedTypes =
        new WasmValue[] {
          WasmValue.i32(1), WasmValue.i64(2L), WasmValue.f32(3.0f), WasmValue.f64(4.0)
        };

    // Use single parameter function for now since we only have "add"
    WasmValue[] result1 = null;
    WasmValue[] result2 = null;

    try {
      result1 = addFunction.call(singleI32[0], WasmValue.i32(0));
      result2 = addFunction.call(mixedTypes[0], mixedTypes[1]);
    } catch (final Exception e) {
      // Fallback to basic call if parameter types don't match
      result1 = addFunction.call(testParams);
      result2 = result1;
    }

    bh.consume(result1);
    bh.consume(result2);

    return result2;
  }

  /** Benchmarks memory usage patterns and cleanup efficiency. */
  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public long benchmarkMemoryManagement(final Blackhole bh) throws WasmException {
    final MemoryUsage before = memoryBean.getHeapMemoryUsage();
    final long beforeUsed = before.getUsed();

    // Create temporary modules and instances
    for (int i = 0; i < Math.min(operationCount, 50); i++) {
      try (final Module tempModule = engine.compileWat(SIMPLE_WAT_MODULE);
          final Instance tempInstance = tempModule.instantiate(store)) {

        final WasmFunction tempFunc = tempInstance.getFunction("add").orElse(addFunction);
        final WasmValue[] result = tempFunc.call(testParams);
        bh.consume(result);
      }
    }

    // Suggest garbage collection
    System.gc();

    final MemoryUsage after = memoryBean.getHeapMemoryUsage();
    final long memoryDelta = after.getUsed() - beforeUsed;

    bh.consume(memoryDelta);
    return memoryDelta;
  }

  /** Benchmarks concurrent-like access patterns within single thread. */
  @Benchmark
  public WasmValue[] benchmarkConcurrentAccessSimulation(final Blackhole bh) throws WasmException {
    // Simulate concurrent access by interleaving different operations
    WasmValue[] lastResult = null;

    for (int i = 0; i < operationCount; i++) {
      switch (i % 3) {
        case 0:
          // Function call
          lastResult = addFunction.call(testParams);
          break;
        case 1:
          // Different parameters
          lastResult = addFunction.call(WasmValue.i32(i), WasmValue.i32(i * 2));
          break;
        case 2:
          // Yet another pattern
          lastResult = addFunction.call(WasmValue.i32(i * 3), WasmValue.i32(i * 4));
          break;
        default:
          // Fallback case
          lastResult = addFunction.call(testParams);
          break;
      }
      bh.consume(lastResult);
    }

    return lastResult;
  }

  /** Benchmarks the complete performance pipeline end-to-end. */
  @Benchmark
  public WasmValue[] benchmarkCompletePerformancePipeline(final Blackhole bh) throws WasmException {
    final long startTime = System.nanoTime();
    final MemoryUsage memBefore = memoryBean.getHeapMemoryUsage();

    try {
      // Perform operations
      WasmValue[] result = null;
      for (int i = 0; i < Math.min(operationCount, 100); i++) {
        result = addFunction.call(WasmValue.i32(i), WasmValue.i32(i + 1));
        bh.consume(result);
      }

      return result;

    } finally {
      final long duration = System.nanoTime() - startTime;
      final MemoryUsage memAfter = memoryBean.getHeapMemoryUsage();

      bh.consume(duration);
      bh.consume(memAfter.getUsed() - memBefore.getUsed());
    }
  }

  /** Helper method to get total GC collections across all collectors. */
  private long getTotalGcCollections() {
    return gcBeans.stream().mapToLong(bean -> Math.max(0, bean.getCollectionCount())).sum();
  }

  /** Helper method to print memory statistics. */
  private void printMemoryStatistics() {
    final MemoryUsage heap = memoryBean.getHeapMemoryUsage();
    final MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

    System.out.printf(
        "Heap Memory - Used: %d MB, Max: %d MB%n",
        heap.getUsed() / (1024 * 1024), heap.getMax() / (1024 * 1024));
    System.out.printf(
        "Non-Heap Memory - Used: %d MB, Max: %d MB%n",
        nonHeap.getUsed() / (1024 * 1024), nonHeap.getMax() / (1024 * 1024));
  }

  /** Helper method to print GC statistics. */
  private void printGcStatistics() {
    for (final GarbageCollectorMXBean gcBean : gcBeans) {
      System.out.printf(
          "GC %s - Collections: %d, Time: %d ms%n",
          gcBean.getName(), gcBean.getCollectionCount(), gcBean.getCollectionTime());
    }
  }
}
