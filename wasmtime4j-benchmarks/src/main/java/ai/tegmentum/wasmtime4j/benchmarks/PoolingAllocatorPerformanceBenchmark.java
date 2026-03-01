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
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Comprehensive benchmark to validate pooling allocator performance claims.
 *
 * <p>This benchmark validates the claimed >10x performance improvement of the pooling allocator by
 * comparing allocation performance with and without pooling across various workload scenarios.
 *
 * <p>Test scenarios include:
 *
 * <ul>
 *   <li>Single-threaded instance allocation (pooled vs non-pooled)
 *   <li>Multi-threaded concurrent allocation stress testing
 *   <li>High-frequency allocation/deallocation patterns
 *   <li>Memory-intensive workloads with large instance pools
 *   <li>Real-world simulation with mixed allocation patterns
 * </ul>
 *
 * <p>Performance targets based on specification requirements:
 *
 * <ul>
 *   <li>Pooled allocation should be >10x faster than standard allocation
 *   <li>Pool warming should provide immediate allocation performance benefits
 *   <li>Multi-threaded performance should scale linearly with pool efficiency
 *   <li>Memory usage should be optimized through pool reuse
 * </ul>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"--enable-native-access=ALL-UNNAMED"})
@State(Scope.Benchmark)
public class PoolingAllocatorPerformanceBenchmark extends BenchmarkBase {

  /** Simple WebAssembly module for allocation testing. */
  private static final byte[] TEST_WASM_SIMPLE =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // WASM magic
        0x01,
        0x00,
        0x00,
        0x00, // Version
        0x01,
        0x04,
        0x01,
        0x60,
        0x00,
        0x00, // Type section: () -> ()
        0x03,
        0x02,
        0x01,
        0x00, // Function section: function 0 has type 0
        0x0a,
        0x04,
        0x01,
        0x02,
        0x00,
        0x0b // Code section: function 0 is empty
      };

  /** Memory-intensive WebAssembly module for stress testing. */
  private static final byte[] TEST_WASM_MEMORY_INTENSIVE =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // WASM magic
        0x01,
        0x00,
        0x00,
        0x00, // Version
        0x01,
        0x04,
        0x01,
        0x60,
        0x00,
        0x00, // Type section: () -> ()
        0x03,
        0x02,
        0x01,
        0x00, // Function section: function 0 has type 0
        0x05,
        0x03,
        0x01,
        0x00,
        0x10, // Memory section: 16 pages (1MB)
        0x0a,
        0x04,
        0x01,
        0x02,
        0x00,
        0x0b // Code section: function 0 is empty
      };

  @Param({"true", "false"})
  private boolean usePooling;

  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  private Engine enginePooled;
  private Engine engineStandard;
  private Module moduleSimple;
  private Module moduleMemoryIntensive;

  @Setup(Level.Trial)
  public void setupTrial() throws WasmException {
    super.setupRuntime(runtimeTypeName);

    // Create engines with different allocation strategies
    final EngineConfig pooledConfig =
        Engine.builder()
            .setPoolingAllocatorEnabled(true)
            .setInstancePoolSize(1000)
            .setMaxMemoryPerInstance(1024 * 1024 * 1024L); // 1GB
    enginePooled = runtime.createEngine(pooledConfig);

    final EngineConfig standardConfig = Engine.builder().setPoolingAllocatorEnabled(false);
    engineStandard = runtime.createEngine(standardConfig);

    // Compile test modules
    moduleSimple = enginePooled.compileModule(TEST_WASM_SIMPLE);
    moduleMemoryIntensive = enginePooled.compileModule(TEST_WASM_MEMORY_INTENSIVE);

    logInfo(
        "Benchmark setup completed for runtime: " + runtimeTypeName + ", pooling: " + usePooling);
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() throws Exception {
    closeQuietly(moduleMemoryIntensive);
    closeQuietly(moduleSimple);
    closeQuietly(engineStandard);
    closeQuietly(enginePooled);

    super.tearDownRuntime();
    logInfo("Benchmark teardown completed for runtime: " + runtimeTypeName);
  }

  /**
   * Benchmarks basic instance allocation performance. This is the primary test for validating the
   * >10x improvement claim.
   */
  @Benchmark
  public Instance benchmarkBasicInstanceAllocation() throws WasmException {
    final Engine engine = usePooling ? enginePooled : engineStandard;
    final Store store = engine.createStore();

    try {
      return moduleSimple.instantiate(store);
    } finally {
      closeQuietly(store);
    }
  }

  /**
   * Benchmarks memory-intensive instance allocation. Tests pooling performance with larger memory
   * allocations.
   */
  @Benchmark
  public Instance benchmarkMemoryIntensiveAllocation() throws WasmException {
    final Engine engine = usePooling ? enginePooled : engineStandard;
    final Store store = engine.createStore();

    try {
      return moduleMemoryIntensive.instantiate(store);
    } finally {
      closeQuietly(store);
    }
  }

  /**
   * Benchmarks rapid allocation/deallocation cycles. Simulates high-frequency usage patterns where
   * pooling should excel.
   */
  @Benchmark
  public void benchmarkRapidAllocationDeallocation() throws WasmException {
    final Engine engine = usePooling ? enginePooled : engineStandard;

    for (int i = 0; i < 10; i++) {
      final Store store = engine.createStore();
      try {
        final Instance instance = moduleSimple.instantiate(store);
        closeQuietly(instance);
      } finally {
        closeQuietly(store);
      }
    }
  }

  /**
   * Benchmarks concurrent allocation performance. Tests pooling efficiency under multi-threaded
   * load.
   */
  @Benchmark
  @Threads(4)
  public Instance benchmarkConcurrentAllocation() throws WasmException {
    final Engine engine = usePooling ? enginePooled : engineStandard;
    final Store store = engine.createStore();

    try {
      return moduleSimple.instantiate(store);
    } finally {
      closeQuietly(store);
    }
  }

  /**
   * Benchmarks high-concurrency allocation stress test. Maximum stress test for pooling allocator
   * performance.
   */
  @Benchmark
  @Threads(16)
  public Instance benchmarkHighConcurrencyAllocation() throws WasmException {
    final Engine engine = usePooling ? enginePooled : engineStandard;
    final Store store = engine.createStore();

    try {
      return moduleSimple.instantiate(store);
    } finally {
      closeQuietly(store);
    }
  }

  /**
   * Benchmarks batch allocation performance. Tests bulk allocation scenarios where pooling provides
   * advantages.
   */
  @Benchmark
  public void benchmarkBatchAllocation() throws WasmException {
    final Engine engine = usePooling ? enginePooled : engineStandard;
    final Instance[] instances = new Instance[50];
    final Store[] stores = new Store[50];

    try {
      for (int i = 0; i < instances.length; i++) {
        stores[i] = engine.createStore();
        instances[i] = moduleSimple.instantiate(stores[i]);
      }
    } finally {
      for (int i = 0; i < instances.length; i++) {
        closeQuietly(instances[i]);
        closeQuietly(stores[i]);
      }
    }
  }

  /**
   * Benchmarks mixed workload allocation patterns. Simulates realistic application usage with
   * varied allocation patterns.
   */
  @Benchmark
  public void benchmarkMixedWorkload() throws WasmException {
    final Engine engine = usePooling ? enginePooled : engineStandard;

    // Pattern 1: Single allocation
    Store store1 = engine.createStore();
    try {
      Instance instance1 = moduleSimple.instantiate(store1);
      closeQuietly(instance1);
    } finally {
      closeQuietly(store1);
    }

    // Pattern 2: Memory-intensive allocation
    Store store2 = engine.createStore();
    try {
      Instance instance2 = moduleMemoryIntensive.instantiate(store2);
      closeQuietly(instance2);
    } finally {
      closeQuietly(store2);
    }

    // Pattern 3: Rapid sequence
    for (int i = 0; i < 3; i++) {
      Store store = engine.createStore();
      try {
        Instance instance = moduleSimple.instantiate(store);
        closeQuietly(instance);
      } finally {
        closeQuietly(store);
      }
    }
  }

  /**
   * Benchmarks long-running allocation patterns. Tests pooling efficiency over extended periods.
   */
  @Benchmark
  public void benchmarkLongRunningAllocation() throws WasmException {
    final Engine engine = usePooling ? enginePooled : engineStandard;

    // Simulate long-running application with periodic allocations
    for (int batch = 0; batch < 5; batch++) {
      // Allocate batch
      final Instance[] instances = new Instance[10];
      final Store[] stores = new Store[10];

      try {
        for (int i = 0; i < instances.length; i++) {
          stores[i] = engine.createStore();
          instances[i] = moduleSimple.instantiate(stores[i]);
        }

        // Simulate some work (minimal delay)
        Thread.yield();

      } catch (Exception e) {
        // Handle any allocation exceptions
        logError("Allocation failed in long-running test: " + e.getMessage());
        throw new WasmException("Long-running allocation benchmark failed", e);
      } finally {
        for (int i = 0; i < instances.length; i++) {
          closeQuietly(instances[i]);
          closeQuietly(stores[i]);
        }
      }
    }
  }

  /**
   * Benchmarks resource cleanup efficiency. Tests pooling allocator resource management
   * performance.
   */
  @Benchmark
  public void benchmarkResourceCleanupEfficiency() throws WasmException {
    final Engine engine = usePooling ? enginePooled : engineStandard;

    // Create and immediately cleanup resources to test pool efficiency
    for (int i = 0; i < 20; i++) {
      final Store store = engine.createStore();
      final Instance instance;
      try {
        instance = moduleSimple.instantiate(store);
      } finally {
        closeQuietly(store);
      }
      // Resource should be returned to pool for reuse
      closeQuietly(instance);
    }
  }

  /**
   * Benchmarks allocator performance under memory pressure. Tests pooling behavior when approaching
   * pool limits.
   */
  @Benchmark
  public void benchmarkMemoryPressureAllocation() throws WasmException {
    final Engine engine = usePooling ? enginePooled : engineStandard;

    // Allocate up to pool capacity to test behavior under pressure
    final int targetAllocations = usePooling ? 50 : 20; // Adjust based on pooling
    final Instance[] instances = new Instance[targetAllocations];
    final Store[] stores = new Store[targetAllocations];

    try {
      for (int i = 0; i < targetAllocations; i++) {
        stores[i] = engine.createStore();
        instances[i] = moduleMemoryIntensive.instantiate(stores[i]);
      }
    } finally {
      for (int i = 0; i < targetAllocations; i++) {
        closeQuietly(instances[i]);
        closeQuietly(stores[i]);
      }
    }
  }
}
