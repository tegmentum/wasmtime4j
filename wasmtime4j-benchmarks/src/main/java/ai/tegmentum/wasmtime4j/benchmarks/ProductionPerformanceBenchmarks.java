/*
 * Copyright 2024 Tegmentum AI Inc.
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
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmInstance;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.production.OptimizedEnginePool;
import ai.tegmentum.wasmtime4j.production.ProductionWasmtimeConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive JMH benchmarks for production performance optimization of WebAssembly operations.
 *
 * <p>Benchmarks cover:
 * - Module compilation performance across different sizes
 * - Function call overhead comparison between runtime implementations
 * - Engine pooling effectiveness
 * - Memory usage optimization
 * - Concurrent performance scaling
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {"-XX:+UseG1GC", "-Xmx2g", "-Xms1g"})
@State(Scope.Benchmark)
public class ProductionPerformanceBenchmarks {

  /** Module sizes for testing compilation performance. */
  @Param({"1KB", "10KB", "100KB", "1MB"})
  private String moduleSize;

  /** Runtime implementations to compare. */
  @Param({"JNI", "Panama"})
  private String runtime;

  /** Thread count for concurrency testing. */
  @Param({"1", "4", "8", "16"})
  private int threadCount;

  private Engine engine;
  private OptimizedEnginePool enginePool;
  private byte[] wasmBytes;
  private Module precompiledModule;
  private Store store;
  private WasmInstance instance;
  private WasmFunction testFunction;

  /** Sets up benchmark environment. */
  @Setup(Level.Trial)
  public void setupTrial() {
    // Create production-optimized configuration
    final ProductionWasmtimeConfig config = ProductionWasmtimeConfig.forProduction();

    // Initialize engine with production configuration
    engine = WasmRuntimeFactory.createEngine(runtime, config.getEngine());

    // Initialize optimized engine pool
    enginePool = OptimizedEnginePool.createHighThroughput();

    // Generate WASM module of specified size
    wasmBytes = generateWasmModule(parseModuleSize(moduleSize));

    // Pre-compile module for function call benchmarks
    precompiledModule = engine.compileModule(wasmBytes);
    store = new Store(engine);
    instance = new WasmInstance(store, precompiledModule);
    testFunction = instance.getFunction("test_function");

    System.out.printf("Benchmark setup complete: runtime=%s, moduleSize=%s, threads=%d%n",
        runtime, moduleSize, threadCount);
  }

  /** Cleans up benchmark environment. */
  @TearDown(Level.Trial)
  public void teardownTrial() {
    if (instance != null) {
      instance.close();
    }
    if (store != null) {
      store.close();
    }
    if (precompiledModule != null) {
      precompiledModule.close();
    }
    if (enginePool != null) {
      enginePool.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  /**
   * Benchmarks module compilation performance.
   *
   * @return the compiled module
   */
  @Benchmark
  public Module benchmarkModuleCompilation() {
    return engine.compileModule(wasmBytes);
  }

  /**
   * Benchmarks module compilation with pooled engine.
   *
   * @return the compiled module
   */
  @Benchmark
  public Module benchmarkPooledModuleCompilation() {
    try {
      final Engine pooledEngine = enginePool.acquireEngine();
      try {
        return pooledEngine.compileModule(wasmBytes);
      } finally {
        enginePool.returnEngine(pooledEngine);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Pooled compilation failed", e);
    }
  }

  /**
   * Benchmarks function call performance.
   *
   * @return the function result
   */
  @Benchmark
  public Object benchmarkFunctionCall() {
    return testFunction.call(new Object[]{42, 100});
  }

  /**
   * Benchmarks complete module lifecycle (compile + instantiate + call).
   *
   * @return the function result
   */
  @Benchmark
  public Object benchmarkCompleteLifecycle() {
    final Module module = engine.compileModule(wasmBytes);
    try {
      final Store localStore = new Store(engine);
      try {
        final WasmInstance localInstance = new WasmInstance(localStore, module);
        try {
          final WasmFunction function = localInstance.getFunction("test_function");
          return function.call(new Object[]{42, 100});
        } finally {
          localInstance.close();
        }
      } finally {
        localStore.close();
      }
    } finally {
      module.close();
    }
  }

  /**
   * Benchmarks pooled engine complete lifecycle.
   *
   * @return the function result
   */
  @Benchmark
  public Object benchmarkPooledCompleteLifecycle() {
    try {
      final Engine pooledEngine = enginePool.acquireEngine();
      try {
        final Module module = pooledEngine.compileModule(wasmBytes);
        try {
          final Store localStore = new Store(pooledEngine);
          try {
            final WasmInstance localInstance = new WasmInstance(localStore, module);
            try {
              final WasmFunction function = localInstance.getFunction("test_function");
              return function.call(new Object[]{42, 100});
            } finally {
              localInstance.close();
            }
          } finally {
            localStore.close();
          }
        } finally {
          module.close();
        }
      } finally {
        enginePool.returnEngine(pooledEngine);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Pooled lifecycle failed", e);
    }
  }

  /** Thread-local state for concurrent benchmarks. */
  @State(Scope.Thread)
  public static class ThreadState {
    private Engine threadEngine;
    private Store threadStore;
    private WasmInstance threadInstance;
    private WasmFunction threadFunction;

    @Setup(Level.Trial)
    public void setupThread(final ProductionPerformanceBenchmarks parent) {
      try {
        threadEngine = parent.enginePool.acquireEngine();
        final Module module = threadEngine.compileModule(parent.wasmBytes);
        threadStore = new Store(threadEngine);
        threadInstance = new WasmInstance(threadStore, module);
        threadFunction = threadInstance.getFunction("test_function");
      } catch (final Exception e) {
        throw new RuntimeException("Thread setup failed", e);
      }
    }

    @TearDown(Level.Trial)
    public void teardownThread(final ProductionPerformanceBenchmarks parent) {
      if (threadInstance != null) {
        threadInstance.close();
      }
      if (threadStore != null) {
        threadStore.close();
      }
      if (threadEngine != null) {
        parent.enginePool.returnEngine(threadEngine);
      }
    }
  }

  /**
   * Benchmarks concurrent function calls using thread-local instances.
   *
   * @param threadState the thread-local state
   * @return the function result
   */
  @Benchmark
  @Threads(Threads.MAX)
  public Object benchmarkConcurrentFunctionCalls(final ThreadState threadState) {
    return threadState.threadFunction.call(new Object[]{42, 100});
  }

  /** Memory usage state for memory benchmarks. */
  @State(Scope.Thread)
  public static class MemoryState {
    private long initialMemory;

    @Setup(Level.Invocation)
    public void setupInvocation() {
      System.gc(); // Force GC before measurement
      Thread.yield();
      initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public long getMemoryIncrease() {
      final long currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      return currentMemory - initialMemory;
    }
  }

  /**
   * Benchmarks memory usage for module compilation.
   *
   * @param memoryState the memory tracking state
   * @return the memory increase in bytes
   */
  @Benchmark
  public long benchmarkMemoryUsage(final MemoryState memoryState) {
    final Module module = engine.compileModule(wasmBytes);
    try {
      final Store localStore = new Store(engine);
      try {
        final WasmInstance localInstance = new WasmInstance(localStore, module);
        try {
          final WasmFunction function = localInstance.getFunction("test_function");
          function.call(new Object[]{42, 100});
          return memoryState.getMemoryIncrease();
        } finally {
          localInstance.close();
        }
      } finally {
        localStore.close();
      }
    } finally {
      module.close();
    }
  }

  /** Parses module size parameter into bytes. */
  private int parseModuleSize(final String sizeStr) {
    final String size = sizeStr.toUpperCase();
    if (size.endsWith("KB")) {
      return Integer.parseInt(size.substring(0, size.length() - 2)) * 1024;
    } else if (size.endsWith("MB")) {
      return Integer.parseInt(size.substring(0, size.length() - 2)) * 1024 * 1024;
    } else {
      return Integer.parseInt(size);
    }
  }

  /** Generates a WASM module of the specified size with a test function. */
  private byte[] generateWasmModule(final int targetSize) {
    // Start with a basic WASM module with test function
    final byte[] baseModule = {
        0x00, 0x61, 0x73, 0x6d, // magic
        0x01, 0x00, 0x00, 0x00, // version
        0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f, // type section: (i32, i32) -> i32
        0x03, 0x02, 0x01, 0x00, // function section
        0x07, 0x11, 0x01, 0x0d, 0x74, 0x65, 0x73, 0x74, 0x5f, 0x66, 0x75, 0x6e, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x00, 0x00, // export section: "test_function"
        0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b // code section: add two parameters
    };

    if (targetSize <= baseModule.length) {
      return baseModule;
    }

    // Pad with additional data section to reach target size
    final int paddingNeeded = targetSize - baseModule.length - 8; // Account for data section header
    final byte[] paddedModule = new byte[targetSize];

    // Copy base module
    System.arraycopy(baseModule, 0, paddedModule, 0, baseModule.length);

    // Add data section header
    paddedModule[baseModule.length] = 0x0b; // data section id
    paddedModule[baseModule.length + 1] = (byte) (paddingNeeded & 0x7f); // length (simplified for small sizes)
    paddedModule[baseModule.length + 2] = 0x01; // data count
    paddedModule[baseModule.length + 3] = 0x00; // memory index
    paddedModule[baseModule.length + 4] = 0x41; // i32.const
    paddedModule[baseModule.length + 5] = 0x00; // offset 0
    paddedModule[baseModule.length + 6] = 0x0b; // end
    paddedModule[baseModule.length + 7] = (byte) (paddingNeeded - 1); // data size

    // Fill remaining with padding data
    for (int i = baseModule.length + 8; i < paddedModule.length; i++) {
      paddedModule[i] = (byte) (i & 0xff);
    }

    return paddedModule;
  }

  /** Runs all benchmarks with optimization reports. */
  public static void main(final String[] args) throws RunnerException {
    final Options options = new OptionsBuilder()
        .include(ProductionPerformanceBenchmarks.class.getSimpleName())
        .forks(1)
        .warmupIterations(3)
        .measurementIterations(5)
        .addProfiler("gc") // GC profiler
        .addProfiler("stack") // Stack profiler for hotspots
        .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.CSV)
        .result("benchmark-results.csv")
        .build();

    new Runner(options).run();
  }
}

/**
 * Memory optimization benchmarks focusing on resource usage patterns.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
class MemoryOptimizationBenchmarks {

  @Param({"100", "1000", "10000"})
  private int operationCount;

  private Engine engine;
  private OptimizedEnginePool enginePool;
  private byte[] smallWasmModule;

  @Setup(Level.Trial)
  public void setup() {
    final ProductionWasmtimeConfig config = ProductionWasmtimeConfig.forProduction();
    engine = WasmRuntimeFactory.createEngine("JNI", config.getEngine());
    enginePool = OptimizedEnginePool.createDefault();
    smallWasmModule = createMinimalWasmModule();
  }

  @TearDown(Level.Trial)
  public void teardown() {
    enginePool.close();
    engine.close();
  }

  /**
   * Benchmarks memory allocation patterns for repeated operations.
   */
  @Benchmark
  public long benchmarkRepeatedAllocations() {
    final long startMemory = getUsedMemory();

    for (int i = 0; i < operationCount; i++) {
      final Module module = engine.compileModule(smallWasmModule);
      final Store store = new Store(engine);
      final WasmInstance instance = new WasmInstance(store, module);

      instance.close();
      store.close();
      module.close();
    }

    return getUsedMemory() - startMemory;
  }

  /**
   * Benchmarks memory usage with engine pooling.
   */
  @Benchmark
  public long benchmarkPooledAllocations() {
    final long startMemory = getUsedMemory();

    for (int i = 0; i < operationCount; i++) {
      try {
        final Engine pooledEngine = enginePool.acquireEngine();
        try {
          final Module module = pooledEngine.compileModule(smallWasmModule);
          final Store store = new Store(pooledEngine);
          final WasmInstance instance = new WasmInstance(store, module);

          instance.close();
          store.close();
          module.close();
        } finally {
          enginePool.returnEngine(pooledEngine);
        }
      } catch (final Exception e) {
        throw new RuntimeException("Pooled operation failed", e);
      }
    }

    return getUsedMemory() - startMemory;
  }

  private long getUsedMemory() {
    System.gc();
    Thread.yield();
    final Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  private byte[] createMinimalWasmModule() {
    return new byte[] {
        0x00, 0x61, 0x73, 0x6d, // magic
        0x01, 0x00, 0x00, 0x00, // version
        0x01, 0x04, 0x01, 0x60, 0x00, 0x00, // type section: () -> ()
        0x03, 0x02, 0x01, 0x00, // function section
        0x0a, 0x04, 0x01, 0x02, 0x00, 0x0b // code section: nop function
    };
  }
}