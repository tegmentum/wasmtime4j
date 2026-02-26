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
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WaitResult;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.concurrent.ThreadLocalRandom;
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
    jvmArgs = {"-Xmx2g", "-XX:+UseG1GC", "--enable-native-access=ALL-UNNAMED"})
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class SharedMemoryBenchmark {

  /** WAT module with shared memory (requires threads feature). */
  private static final String SHARED_MEMORY_WAT =
      "(module\n" + "  (memory (export \"memory\") 1 16 shared))\n";

  /** WAT module with regular (non-shared) memory. */
  private static final String REGULAR_MEMORY_WAT =
      "(module\n" + "  (memory (export \"memory\") 1 16))\n";

  @Param({"JNI", "PANAMA"})
  private String runtimeType;

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private Instance sharedInstance;
  private Instance regularInstance;
  private WasmMemory sharedMemory;
  private WasmMemory regularMemory;

  /** Sets up the benchmark trial with shared and regular memory instances. */
  @Setup(Level.Trial)
  public void setupTrial() throws Exception {
    final RuntimeType type = RuntimeType.valueOf(runtimeType);
    if (!WasmRuntimeFactory.isRuntimeAvailable(type)) {
      throw new RuntimeException("Runtime not available: " + type);
    }
    runtime = WasmRuntimeFactory.create(type);

    // Create engine with threads support for shared memory
    final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
    engine = runtime.createEngine(config);
    store = engine.createStore();

    // Create shared memory instance
    final Module sharedModule = engine.compileWat(SHARED_MEMORY_WAT);
    sharedInstance = sharedModule.instantiate(store);
    sharedMemory =
        sharedInstance
            .getMemory("memory")
            .orElseThrow(() -> new RuntimeException("Shared memory not found"));
    sharedModule.close();

    // Create regular memory instance
    final Module regularModule = engine.compileWat(REGULAR_MEMORY_WAT);
    regularInstance = regularModule.instantiate(store);
    regularMemory =
        regularInstance
            .getMemory("memory")
            .orElseThrow(() -> new RuntimeException("Regular memory not found"));
    regularModule.close();

    System.out.println("Shared memory benchmark setup complete");
    System.out.println("Shared memory size: " + sharedMemory.getSize() + " bytes");
    System.out.println("Shared memory is shared: " + sharedMemory.isShared());
  }

  /** Cleans up all resources. */
  @TearDown(Level.Trial)
  public void tearDownTrial() throws Exception {
    if (sharedInstance != null) {
      sharedInstance.close();
    }
    if (regularInstance != null) {
      regularInstance.close();
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
  }

  // Atomic Operation Benchmarks

  /** Benchmarks atomic compare-and-swap on 32-bit values. */
  @Benchmark
  public int atomicCompareAndSwapInt() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3);
    final int expected = sharedMemory.atomicLoadInt(offset);
    final int newValue = ThreadLocalRandom.current().nextInt();
    return sharedMemory.atomicCompareAndSwapInt(offset, expected, newValue);
  }

  /** Benchmarks atomic compare-and-swap on 64-bit values. */
  @Benchmark
  public long atomicCompareAndSwapLong() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~7);
    final long expected = sharedMemory.atomicLoadLong(offset);
    final long newValue = ThreadLocalRandom.current().nextLong();
    return sharedMemory.atomicCompareAndSwapLong(offset, expected, newValue);
  }

  /** Benchmarks atomic load of 32-bit values. */
  @Benchmark
  public int atomicLoadInt() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3);
    return sharedMemory.atomicLoadInt(offset);
  }

  /** Benchmarks atomic store of 32-bit values. */
  @Benchmark
  public void atomicStoreInt() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3);
    final int value = ThreadLocalRandom.current().nextInt();
    sharedMemory.atomicStoreInt(offset, value);
  }

  /** Benchmarks atomic load of 64-bit values. */
  @Benchmark
  public long atomicLoadLong() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~7);
    return sharedMemory.atomicLoadLong(offset);
  }

  /** Benchmarks atomic store of 64-bit values. */
  @Benchmark
  public void atomicStoreLong() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~7);
    final long value = ThreadLocalRandom.current().nextLong();
    sharedMemory.atomicStoreLong(offset, value);
  }

  /** Benchmarks atomic add on 32-bit values. */
  @Benchmark
  public int atomicAddInt() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3);
    final int value = ThreadLocalRandom.current().nextInt(100);
    return sharedMemory.atomicAddInt(offset, value);
  }

  /** Benchmarks atomic add on 64-bit values. */
  @Benchmark
  public long atomicAddLong() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~7);
    final long value = ThreadLocalRandom.current().nextLong(100);
    return sharedMemory.atomicAddLong(offset, value);
  }

  /** Benchmarks atomic AND on 32-bit values. */
  @Benchmark
  public int atomicAndInt() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3);
    final int mask = ThreadLocalRandom.current().nextInt();
    return sharedMemory.atomicAndInt(offset, mask);
  }

  /** Benchmarks atomic OR on 32-bit values. */
  @Benchmark
  public int atomicOrInt() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3);
    final int mask = ThreadLocalRandom.current().nextInt();
    return sharedMemory.atomicOrInt(offset, mask);
  }

  /** Benchmarks atomic XOR on 32-bit values. */
  @Benchmark
  public int atomicXorInt() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3);
    final int mask = ThreadLocalRandom.current().nextInt();
    return sharedMemory.atomicXorInt(offset, mask);
  }

  /** Benchmarks atomic fence operation. */
  @Benchmark
  public void atomicFence() {
    sharedMemory.atomicFence();
  }

  // Comparison benchmarks with regular memory

  /** Benchmarks byte read from regular (non-shared) memory. */
  @Benchmark
  public byte regularMemoryRead() {
    final int offset = ThreadLocalRandom.current().nextInt(1024);
    return regularMemory.readByte(offset);
  }

  /** Benchmarks byte write to regular (non-shared) memory. */
  @Benchmark
  public void regularMemoryWrite() {
    final int offset = ThreadLocalRandom.current().nextInt(1024);
    final byte value = (byte) ThreadLocalRandom.current().nextInt();
    regularMemory.writeByte(offset, value);
  }

  /** Benchmarks byte read from shared memory. */
  @Benchmark
  public byte sharedMemoryRead() {
    final int offset = ThreadLocalRandom.current().nextInt(1024);
    return sharedMemory.readByte(offset);
  }

  /** Benchmarks byte write to shared memory. */
  @Benchmark
  public void sharedMemoryWrite() {
    final int offset = ThreadLocalRandom.current().nextInt(1024);
    final byte value = (byte) ThreadLocalRandom.current().nextInt();
    sharedMemory.writeByte(offset, value);
  }

  // Wait/Notify benchmarks

  /** Benchmarks atomic wait32 with immediate return (value mismatch). */
  @Benchmark
  public WaitResult atomicWait32Immediate() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3);
    final int current = sharedMemory.atomicLoadInt(offset);
    final int different = current + 1; // Ensure mismatch for immediate return
    return sharedMemory.atomicWait32(offset, different, 0L);
  }

  /** Benchmarks atomic notify operation. */
  @Benchmark
  public int atomicNotify() {
    final int offset = (ThreadLocalRandom.current().nextInt(1024) & ~3);
    final int count = ThreadLocalRandom.current().nextInt(1, 5);
    return sharedMemory.atomicNotify(offset, count);
  }

  // Concurrent access simulation

  /** Per-thread state for concurrent access benchmarks. */
  @State(Scope.Thread)
  public static class ThreadState {
    private int threadOffset;

    /** Assigns each thread its own offset range. */
    @Setup(Level.Trial)
    public void setup() {
      threadOffset = ThreadLocalRandom.current().nextInt(256) * 4;
    }
  }

  /** Benchmarks contended atomic increment (all threads on same location). */
  @Benchmark
  public int concurrentAtomicIncrement(final ThreadState threadState) {
    return sharedMemory.atomicAddInt(0, 1);
  }

  /** Benchmarks uncontended atomic increment (each thread on its own location). */
  @Benchmark
  public int threadLocalAtomicIncrement(final ThreadState threadState) {
    return sharedMemory.atomicAddInt(threadState.threadOffset, 1);
  }

  /** Main method to run the benchmark. */
  public static void main(final String[] args) throws RunnerException {
    final Options opt =
        new OptionsBuilder()
            .include(SharedMemoryBenchmark.class.getSimpleName())
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .build();

    new Runner(opt).run();
  }
}
