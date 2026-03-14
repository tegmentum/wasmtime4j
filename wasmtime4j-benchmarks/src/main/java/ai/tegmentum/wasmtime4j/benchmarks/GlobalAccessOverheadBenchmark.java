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
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.JniGlobal;
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
 * Benchmarks measuring per-operation overhead of global variable access.
 *
 * <p>This benchmark isolates the overhead of individual getIntValue/setIntValue
 * operations to measure the impact of removing redundant validateMutable JNI calls
 * (isMutable) and streamlining the guard pattern in JniGlobal.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class GlobalAccessOverheadBenchmark extends BenchmarkBase {

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
  private JniGlobal mutableGlobal;
  private JniGlobal immutableGlobal;
  private JniGlobal doubleGlobal;

  @Setup(Level.Trial)
  public void setup() throws WasmException {
    runtime = createRuntime(RuntimeType.JNI);
    engine = createEngine(runtime);
    store = createStore(engine);
    module = compileWatModule(engine, GLOBAL_WAT_MODULE);
    instance = instantiateModule(store, module);
    mutableGlobal = (JniGlobal) instance.getGlobal("counter")
        .orElseThrow(() -> new WasmException("counter global not found"));
    immutableGlobal = (JniGlobal) instance.getGlobal("limit")
        .orElseThrow(() -> new WasmException("limit global not found"));
    doubleGlobal = (JniGlobal) instance.getGlobal("ratio")
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
  public int getIntValue(Blackhole bh) {
    return mutableGlobal.getIntValue();
  }

  @Benchmark
  public void setIntValue(Blackhole bh) {
    mutableGlobal.setIntValue(42);
  }

  @Benchmark
  public int getImmutableIntValue(Blackhole bh) {
    return immutableGlobal.getIntValue();
  }

  @Benchmark
  public double getDoubleValue(Blackhole bh) {
    return doubleGlobal.getDoubleValue();
  }

  @Benchmark
  public void setDoubleValue(Blackhole bh) {
    doubleGlobal.setDoubleValue(2.718);
  }

  @Benchmark
  public void setIntBurst(Blackhole bh) {
    mutableGlobal.setIntValue(0);
    mutableGlobal.setIntValue(1);
    mutableGlobal.setIntValue(2);
    mutableGlobal.setIntValue(3);
    mutableGlobal.setIntValue(4);
    mutableGlobal.setIntValue(5);
    mutableGlobal.setIntValue(6);
    mutableGlobal.setIntValue(7);
    mutableGlobal.setIntValue(8);
    mutableGlobal.setIntValue(9);
  }

  @Benchmark
  public void getIntBurst(Blackhole bh) {
    bh.consume(mutableGlobal.getIntValue());
    bh.consume(mutableGlobal.getIntValue());
    bh.consume(mutableGlobal.getIntValue());
    bh.consume(mutableGlobal.getIntValue());
    bh.consume(mutableGlobal.getIntValue());
    bh.consume(mutableGlobal.getIntValue());
    bh.consume(mutableGlobal.getIntValue());
    bh.consume(mutableGlobal.getIntValue());
    bh.consume(mutableGlobal.getIntValue());
    bh.consume(mutableGlobal.getIntValue());
  }
}
