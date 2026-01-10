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
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmMemory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH benchmarks for WebAssembly shared memory operations.
 *
 * <p>This benchmark suite measures the performance of: - Atomic compare-and-swap operations -
 * Atomic load/store operations - Atomic arithmetic operations - Memory fences - Concurrent access
 * patterns
 *
 * <p>Run with: mvn exec:java
 * -Dexec.mainClass="ai.tegmentum.wasmtime4j.benchmarks.SharedMemoryBenchmark"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(
    value = 1,
    jvmArgs = {"-Xmx2g", "-XX:+UseG1GC"})
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class SharedMemoryBenchmark {

  private Engine engine;
  private Store store;
  private Instance instance;
  private WasmMemory sharedMemory;
  private WasmMemory regularMemory;

  @Setup(Level.Trial)
  public void setupTrial() throws Exception {
    // Create engine with threads support for shared memory
    EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
    engine = Engine.create(config);
    store = engine.createStore();

    // Create instances with shared and regular memory
    byte[] sharedMemoryWasm = createSharedMemoryModule();
    Module sharedModule = engine.compileModule(sharedMemoryWasm);
    instance = store.createInstance(sharedModule);
    sharedMemory =
        instance
            .getMemory("memory")
            .orElseThrow(() -> new RuntimeException("Shared memory not found"));

    byte[] regularMemoryWasm = createRegularMemoryModule();
    Module regularModule = engine.compileModule(regularMemoryWasm);
    Instance regularInstance = store.createInstance(regularModule);
    regularMemory =
        regularInstance
            .getMemory("memory")
            .orElseThrow(() -> new RuntimeException("Regular memory not found"));

    System.out.println("Shared memory benchmark setup complete");
    System.out.println("Shared memory size: " + sharedMemory.getSize() + " pages");
    System.out.println("Shared memory is shared: " + sharedMemory.isShared());
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  // Atomic Operation Benchmarks

  @Benchmark
  public int atomicCompareAndSwapInt() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3); // Align to 4-byte boundary
    int expected = sharedMemory.atomicLoadInt(offset);
    int newValue = ThreadLocalRandom.current().nextInt();
    return sharedMemory.atomicCompareAndSwapInt(offset, expected, newValue);
  }

  @Benchmark
  public long atomicCompareAndSwapLong() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~7); // Align to 8-byte boundary
    long expected = sharedMemory.atomicLoadLong(offset);
    long newValue = ThreadLocalRandom.current().nextLong();
    return sharedMemory.atomicCompareAndSwapLong(offset, expected, newValue);
  }

  @Benchmark
  public int atomicLoadInt() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3); // Align to 4-byte boundary
    return sharedMemory.atomicLoadInt(offset);
  }

  @Benchmark
  public void atomicStoreInt() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3); // Align to 4-byte boundary
    int value = ThreadLocalRandom.current().nextInt();
    sharedMemory.atomicStoreInt(offset, value);
  }

  @Benchmark
  public long atomicLoadLong() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~7); // Align to 8-byte boundary
    return sharedMemory.atomicLoadLong(offset);
  }

  @Benchmark
  public void atomicStoreLong() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~7); // Align to 8-byte boundary
    long value = ThreadLocalRandom.current().nextLong();
    sharedMemory.atomicStoreLong(offset, value);
  }

  @Benchmark
  public int atomicAddInt() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3); // Align to 4-byte boundary
    int value = ThreadLocalRandom.current().nextInt(100);
    return sharedMemory.atomicAddInt(offset, value);
  }

  @Benchmark
  public long atomicAddLong() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~7); // Align to 8-byte boundary
    long value = ThreadLocalRandom.current().nextLong(100);
    return sharedMemory.atomicAddLong(offset, value);
  }

  @Benchmark
  public int atomicAndInt() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3); // Align to 4-byte boundary
    int mask = ThreadLocalRandom.current().nextInt();
    return sharedMemory.atomicAndInt(offset, mask);
  }

  @Benchmark
  public int atomicOrInt() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3); // Align to 4-byte boundary
    int mask = ThreadLocalRandom.current().nextInt();
    return sharedMemory.atomicOrInt(offset, mask);
  }

  @Benchmark
  public int atomicXorInt() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3); // Align to 4-byte boundary
    int mask = ThreadLocalRandom.current().nextInt();
    return sharedMemory.atomicXorInt(offset, mask);
  }

  @Benchmark
  public void atomicFence() {
    sharedMemory.atomicFence();
  }

  // Comparison benchmarks with regular memory

  @Benchmark
  public byte regularMemoryRead() {
    int offset = ThreadLocalRandom.current().nextInt(1024);
    return regularMemory.readByte(offset);
  }

  @Benchmark
  public void regularMemoryWrite() {
    int offset = ThreadLocalRandom.current().nextInt(1024);
    byte value = (byte) ThreadLocalRandom.current().nextInt();
    regularMemory.writeByte(offset, value);
  }

  @Benchmark
  public byte sharedMemoryRead() {
    int offset = ThreadLocalRandom.current().nextInt(1024);
    return sharedMemory.readByte(offset);
  }

  @Benchmark
  public void sharedMemoryWrite() {
    int offset = ThreadLocalRandom.current().nextInt(1024);
    byte value = (byte) ThreadLocalRandom.current().nextInt();
    sharedMemory.writeByte(offset, value);
  }

  // Wait/Notify benchmarks (simplified)

  @Benchmark
  public int atomicWait32Immediate() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3); // Align to 4-byte boundary
    int current = sharedMemory.atomicLoadInt(offset);
    int different = current + 1; // Ensure mismatch for immediate return
    return sharedMemory.atomicWait32(offset, different, 0L);
  }

  @Benchmark
  public int atomicNotify() {
    int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3); // Align to 4-byte boundary
    int count = ThreadLocalRandom.current().nextInt(1, 5);
    return sharedMemory.atomicNotify(offset, count);
  }

  // Concurrent access simulation

  @State(Scope.Thread)
  public static class ThreadState {
    private int threadOffset;

    @Setup(Level.Trial)
    public void setup() {
      // Give each thread its own offset range to reduce contention for some benchmarks
      threadOffset = ThreadLocalRandom.current().nextInt(256) * 4; // 4-byte aligned
    }
  }

  @Benchmark
  public int concurrentAtomicIncrement(ThreadState threadState) {
    // All threads increment the same location for maximum contention
    return sharedMemory.atomicAddInt(0, 1);
  }

  @Benchmark
  public int threadLocalAtomicIncrement(ThreadState threadState) {
    // Each thread increments its own location
    return sharedMemory.atomicAddInt(threadState.threadOffset, 1);
  }

  // Utility methods

  /**
   * Creates a WebAssembly module with shared memory. In a real implementation, this would contain
   * actual WASM bytecode.
   */
  private byte[] createSharedMemoryModule() {
    // Placeholder - in practice this would be actual WASM bytecode with shared memory
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // magic number
      0x01, 0x00, 0x00, 0x00, // version
      // Memory section with shared memory would be defined here
    };
  }

  /** Creates a WebAssembly module with regular (non-shared) memory. */
  private byte[] createRegularMemoryModule() {
    // Placeholder - in practice this would be actual WASM bytecode with regular memory
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // magic number
      0x01, 0x00, 0x00, 0x00, // version
      // Memory section with regular memory would be defined here
    };
  }

  /** Main method to run the benchmark. */
  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(SharedMemoryBenchmark.class.getSimpleName())
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .build();

    new Runner(opt).run();
  }
}
