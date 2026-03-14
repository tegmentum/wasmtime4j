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
import ai.tegmentum.wasmtime4j.WasmTable;
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
 * Benchmarks measuring per-operation overhead of Panama table access.
 *
 * <p>This benchmark measures the overhead of table get/set/size operations through the Panama FFI
 * path, focusing on MethodHandle lookup, arena allocation, and invokeExact optimization costs.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class PanamaTableBenchmark extends BenchmarkBase {

  private static final String TABLE_WAT_MODULE =
      "(module\n"
          + "  (table (export \"table\") 10 funcref)\n"
          + "  (func $dummy (export \"dummy\"))\n"
          + "  (elem (i32.const 0) $dummy))\n";

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private Module module;
  private Instance instance;
  private WasmTable table;

  @Setup(Level.Trial)
  public void setup() throws WasmException {
    runtime = createRuntime(RuntimeType.PANAMA);
    engine = createEngine(runtime);
    store = createStore(engine);
    module = compileWatModule(engine, TABLE_WAT_MODULE);
    instance = instantiateModule(store, module);
    table = instance.getTable("table").orElseThrow(() -> new WasmException("table not found"));
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
  public Object getElement(Blackhole bh) {
    return table.get(0);
  }

  @Benchmark
  public int getSize(Blackhole bh) {
    return table.getSize();
  }

  @Benchmark
  public void getElementBurst(Blackhole bh) {
    bh.consume(table.get(0));
    bh.consume(table.get(1));
    bh.consume(table.get(2));
    bh.consume(table.get(3));
    bh.consume(table.get(4));
    bh.consume(table.get(5));
    bh.consume(table.get(6));
    bh.consume(table.get(7));
    bh.consume(table.get(8));
    bh.consume(table.get(9));
  }
}
