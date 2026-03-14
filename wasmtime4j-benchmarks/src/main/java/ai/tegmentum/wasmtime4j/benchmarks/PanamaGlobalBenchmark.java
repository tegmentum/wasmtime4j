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
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
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
 * Benchmarks measuring per-operation overhead of Panama global variable access.
 *
 * <p>This benchmark measures the overhead of global get/set operations through the Panama FFI path,
 * focusing on Arena allocation and MemorySegment allocation costs per operation.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class PanamaGlobalBenchmark extends BenchmarkBase {

  private static final String GLOBAL_WAT_MODULE =
      "(module\n"
          + "  (global $counter (export \"counter\") (mut i32) (i32.const 0))\n"
          + "  (global $limit (export \"limit\") i32 (i32.const 100))\n"
          + "  (global $ratio (export \"ratio\") (mut f64) (f64.const 3.14)))\n";

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private Module module;
  private Instance instance;
  private WasmGlobal mutableGlobal;
  private WasmGlobal immutableGlobal;
  private WasmGlobal doubleGlobal;

  @Setup(Level.Trial)
  public void setup() throws WasmException {
    runtime = createRuntime(RuntimeType.PANAMA);
    engine = createEngine(runtime);
    store = createStore(engine);
    module = compileWatModule(engine, GLOBAL_WAT_MODULE);
    instance = instantiateModule(store, module);
    mutableGlobal =
        instance
            .getGlobal("counter")
            .orElseThrow(() -> new WasmException("counter global not found"));
    immutableGlobal =
        instance
            .getGlobal("limit")
            .orElseThrow(() -> new WasmException("limit global not found"));
    doubleGlobal =
        instance
            .getGlobal("ratio")
            .orElseThrow(() -> new WasmException("ratio global not found"));
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
  public WasmValue getIntValue(Blackhole bh) {
    return mutableGlobal.get();
  }

  @Benchmark
  public void setIntValue(Blackhole bh) {
    mutableGlobal.set(WasmValue.i32(42));
  }

  @Benchmark
  public WasmValue getImmutableIntValue(Blackhole bh) {
    return immutableGlobal.get();
  }

  @Benchmark
  public WasmValue getDoubleValue(Blackhole bh) {
    return doubleGlobal.get();
  }

  @Benchmark
  public void setDoubleValue(Blackhole bh) {
    doubleGlobal.set(WasmValue.f64(2.718));
  }

  @Benchmark
  public void getIntBurst(Blackhole bh) {
    bh.consume(mutableGlobal.get());
    bh.consume(mutableGlobal.get());
    bh.consume(mutableGlobal.get());
    bh.consume(mutableGlobal.get());
    bh.consume(mutableGlobal.get());
    bh.consume(mutableGlobal.get());
    bh.consume(mutableGlobal.get());
    bh.consume(mutableGlobal.get());
    bh.consume(mutableGlobal.get());
    bh.consume(mutableGlobal.get());
  }

  @Benchmark
  public void setIntBurst(Blackhole bh) {
    mutableGlobal.set(WasmValue.i32(0));
    mutableGlobal.set(WasmValue.i32(1));
    mutableGlobal.set(WasmValue.i32(2));
    mutableGlobal.set(WasmValue.i32(3));
    mutableGlobal.set(WasmValue.i32(4));
    mutableGlobal.set(WasmValue.i32(5));
    mutableGlobal.set(WasmValue.i32(6));
    mutableGlobal.set(WasmValue.i32(7));
    mutableGlobal.set(WasmValue.i32(8));
    mutableGlobal.set(WasmValue.i32(9));
  }
}
