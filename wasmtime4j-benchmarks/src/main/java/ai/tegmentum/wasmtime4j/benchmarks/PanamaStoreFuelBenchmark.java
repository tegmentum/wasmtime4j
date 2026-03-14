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
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
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
 * Benchmarks measuring per-operation overhead of Panama store fuel/epoch operations.
 *
 * <p>This benchmark measures the overhead of setFuel, getFuel, addFuel, consumeFuel, and
 * setEpochDeadline operations through the Panama FFI path.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class PanamaStoreFuelBenchmark extends BenchmarkBase {

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  @Setup(Level.Trial)
  public void setup() throws WasmException {
    runtime = createRuntime(RuntimeType.PANAMA);
    final EngineConfig config = new EngineConfig().consumeFuel(true).epochInterruption(true);
    engine = runtime.createEngine(config);
    store = engine.createStore();
    store.setFuel(1_000_000_000L);
  }

  @TearDown(Level.Trial)
  public void teardown() {
    closeQuietly(store);
    closeQuietly(engine);
    closeQuietly(runtime);
  }

  @Benchmark
  public long getFuel(Blackhole bh) throws WasmException {
    return store.getFuel();
  }

  @Benchmark
  public void setFuel(Blackhole bh) throws WasmException {
    store.setFuel(1_000_000L);
  }

  @Benchmark
  public void addFuel(Blackhole bh) throws WasmException {
    store.addFuel(100L);
  }

  @Benchmark
  public long consumeFuel(Blackhole bh) throws WasmException {
    store.addFuel(1000L);
    return store.consumeFuel(100L);
  }

  @Benchmark
  public void setEpochDeadline(Blackhole bh) throws WasmException {
    store.setEpochDeadline(100L);
  }

  @Benchmark
  public void fuelBurst(Blackhole bh) throws WasmException {
    bh.consume(store.getFuel());
    store.addFuel(100L);
    bh.consume(store.getFuel());
    store.addFuel(100L);
    bh.consume(store.getFuel());
    store.addFuel(100L);
    bh.consume(store.getFuel());
    store.addFuel(100L);
    bh.consume(store.getFuel());
  }
}
