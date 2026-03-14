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
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.TypedFunc;
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
 * Benchmarks measuring per-operation overhead of Panama typed function calls.
 *
 * <p>This benchmark mirrors TypedFuncOverheadBenchmark but uses the Panama FFI path to measure
 * the performance of Panama typed function calls vs JNI.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class PanamaFuncBenchmark extends BenchmarkBase {

  private static final String TYPED_FUNC_WAT =
      "(module\n"
          + "  (func $add (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add)\n"
          + "  (func $identity (export \"identity\") (param i32) (result i32)\n"
          + "    local.get 0)\n"
          + "  (func $noop (export \"noop\")\n"
          + "    nop))\n";

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private Module module;
  private Instance instance;
  private TypedFunc typedAdd;
  private TypedFunc typedIdentity;
  private TypedFunc typedNoop;

  @Setup(Level.Trial)
  public void setup() throws WasmException {
    runtime = createRuntime(RuntimeType.PANAMA);
    engine = createEngine(runtime);
    store = createStore(engine);
    module = compileWatModule(engine, TYPED_FUNC_WAT);
    instance = instantiateModule(store, module);

    final WasmFunction addFunc =
        instance.getFunction("add").orElseThrow(() -> new WasmException("add not found"));
    final WasmFunction identityFunc =
        instance
            .getFunction("identity")
            .orElseThrow(() -> new WasmException("identity not found"));
    final WasmFunction noopFunc =
        instance.getFunction("noop").orElseThrow(() -> new WasmException("noop not found"));

    typedAdd = addFunc.typed("ii->i");
    typedIdentity = identityFunc.typed("i->i");
    typedNoop = noopFunc.typed("->v");
  }

  @TearDown(Level.Trial)
  public void teardown() {
    closeQuietly(typedAdd);
    closeQuietly(typedIdentity);
    closeQuietly(typedNoop);
    closeQuietly(instance);
    closeQuietly(module);
    closeQuietly(store);
    closeQuietly(engine);
    closeQuietly(runtime);
  }

  @Benchmark
  public int callI32I32ToI32(Blackhole bh) throws WasmException {
    return typedAdd.callI32I32ToI32(10, 20);
  }

  @Benchmark
  public int callI32ToI32(Blackhole bh) throws WasmException {
    return typedIdentity.callI32ToI32(42);
  }

  @Benchmark
  public void callVoidToVoid(Blackhole bh) throws WasmException {
    typedNoop.callVoidToVoid();
  }

  @Benchmark
  public void callI32I32ToI32Burst(Blackhole bh) throws WasmException {
    bh.consume(typedAdd.callI32I32ToI32(0, 1));
    bh.consume(typedAdd.callI32I32ToI32(1, 2));
    bh.consume(typedAdd.callI32I32ToI32(2, 3));
    bh.consume(typedAdd.callI32I32ToI32(3, 4));
    bh.consume(typedAdd.callI32I32ToI32(4, 5));
    bh.consume(typedAdd.callI32I32ToI32(5, 6));
    bh.consume(typedAdd.callI32I32ToI32(6, 7));
    bh.consume(typedAdd.callI32I32ToI32(7, 8));
    bh.consume(typedAdd.callI32I32ToI32(8, 9));
    bh.consume(typedAdd.callI32I32ToI32(9, 10));
  }

  @Benchmark
  public void callVoidToVoidBurst(Blackhole bh) throws WasmException {
    typedNoop.callVoidToVoid();
    typedNoop.callVoidToVoid();
    typedNoop.callVoidToVoid();
    typedNoop.callVoidToVoid();
    typedNoop.callVoidToVoid();
    typedNoop.callVoidToVoid();
    typedNoop.callVoidToVoid();
    typedNoop.callVoidToVoid();
    typedNoop.callVoidToVoid();
    typedNoop.callVoidToVoid();
  }
}
