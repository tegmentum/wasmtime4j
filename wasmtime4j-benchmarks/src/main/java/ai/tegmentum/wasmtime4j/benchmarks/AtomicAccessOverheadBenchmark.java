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
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks measuring per-operation overhead of atomic memory access operations.
 *
 * <p>This benchmark isolates the overhead of individual atomic operations to measure the impact of
 * removing redundant validateOffset and checkSharedMemory JNI calls.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class AtomicAccessOverheadBenchmark extends BenchmarkBase {

  private static final String SHARED_MEMORY_WAT =
      "(module\n" + "  (memory (export \"memory\") 1 16 shared))\n";

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private Module module;
  private Instance instance;
  private WasmMemory memory;

  @Setup(Level.Trial)
  public void setup() throws WasmException {
    runtime = createRuntime(RuntimeType.JNI);
    final EngineConfig config =
        new EngineConfig().addWasmFeature(WasmFeature.THREADS).sharedMemory(true);
    engine = runtime.createEngine(config);
    store = createStore(engine);
    module = engine.compileWat(SHARED_MEMORY_WAT);
    instance = instantiateModule(store, module);
    memory = instance.getMemory("memory").orElseThrow(() -> new WasmException("Memory not found"));
  }

  @TearDown(Level.Trial)
  public void teardown() {
    closeQuietly(instance);
    closeQuietly(module);
    closeQuietly(store);
    closeQuietly(engine);
    closeQuietly(runtime);
  }

  @Benchmark
  public int atomicLoadInt(Blackhole bh) {
    return memory.atomicLoadInt(0);
  }

  @Benchmark
  public void atomicStoreInt(Blackhole bh) {
    memory.atomicStoreInt(0, 42);
  }

  @Benchmark
  public int atomicAddInt(Blackhole bh) {
    return memory.atomicAddInt(0, 1);
  }

  @Benchmark
  public int atomicCompareAndSwapInt(Blackhole bh) {
    return memory.atomicCompareAndSwapInt(0, 0, 1);
  }

  @Benchmark
  public long atomicLoadLong(Blackhole bh) {
    return memory.atomicLoadLong(0);
  }

  @Benchmark
  public void atomicStoreLong(Blackhole bh) {
    memory.atomicStoreLong(0, 42L);
  }

  @Benchmark
  public void atomicLoadIntBurst(Blackhole bh) {
    bh.consume(memory.atomicLoadInt(0));
    bh.consume(memory.atomicLoadInt(4));
    bh.consume(memory.atomicLoadInt(8));
    bh.consume(memory.atomicLoadInt(12));
    bh.consume(memory.atomicLoadInt(16));
    bh.consume(memory.atomicLoadInt(20));
    bh.consume(memory.atomicLoadInt(24));
    bh.consume(memory.atomicLoadInt(28));
    bh.consume(memory.atomicLoadInt(32));
    bh.consume(memory.atomicLoadInt(36));
  }

  @Benchmark
  public void atomicStoreIntBurst(Blackhole bh) {
    memory.atomicStoreInt(0, 0);
    memory.atomicStoreInt(4, 1);
    memory.atomicStoreInt(8, 2);
    memory.atomicStoreInt(12, 3);
    memory.atomicStoreInt(16, 4);
    memory.atomicStoreInt(20, 5);
    memory.atomicStoreInt(24, 6);
    memory.atomicStoreInt(28, 7);
    memory.atomicStoreInt(32, 8);
    memory.atomicStoreInt(36, 9);
  }
}
