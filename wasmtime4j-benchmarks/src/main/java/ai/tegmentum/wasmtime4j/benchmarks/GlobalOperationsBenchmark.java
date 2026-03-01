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
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
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

/**
 * JMH benchmarks for WebAssembly global variable operations.
 *
 * <p>This benchmark suite measures the performance characteristics of global variable creation,
 * access, and mutation across different value types and runtime implementations.
 *
 * <p>Key performance metrics measured:
 *
 * <ul>
 *   <li>Global creation overhead for different value types
 *   <li>Get/set operation latency for mutable globals
 *   <li>Cross-implementation performance comparison
 *   <li>Type conversion overhead
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 1,
    jvmArgs = {"--enable-native-access=ALL-UNNAMED"})
public class GlobalOperationsBenchmark {

  @Param({"JNI", "PANAMA"})
  private String runtimeType;

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  // Pre-created globals for get/set benchmarks
  private WasmGlobal i32Global;
  private WasmGlobal i64Global;
  private WasmGlobal f32Global;
  private WasmGlobal f64Global;
  private WasmGlobal v128Global;

  // Pre-created values for set operations
  private WasmValue i32Value;
  private WasmValue i64Value;
  private WasmValue f32Value;
  private WasmValue f64Value;
  private WasmValue v128Value;

  @Setup(Level.Trial)
  public void setupTrial() throws WasmException {
    RuntimeType type = RuntimeType.valueOf(runtimeType);
    if (!WasmRuntimeFactory.isRuntimeAvailable(type)) {
      throw new RuntimeException("Runtime not available: " + type);
    }
    runtime = WasmRuntimeFactory.create(type);
    engine = runtime.createEngine();
    store = engine.createStore();

    // Create globals for get/set benchmarks
    i32Global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(42));
    i64Global = store.createGlobal(WasmValueType.I64, true, WasmValue.i64(123456789L));
    f32Global = store.createGlobal(WasmValueType.F32, true, WasmValue.f32(3.14f));
    f64Global = store.createGlobal(WasmValueType.F64, true, WasmValue.f64(2.718));

    // Create V128 vector data
    final byte[] vectorData = new byte[16];
    for (int i = 0; i < 16; i++) {
      vectorData[i] = (byte) (i * 17);
    }
    v128Global = store.createGlobal(WasmValueType.V128, true, WasmValue.v128(vectorData));

    // Pre-create values for benchmarks
    i32Value = WasmValue.i32(100);
    i64Value = WasmValue.i64(987654321L);
    f32Value = WasmValue.f32(1.41f);
    f64Value = WasmValue.f64(1.618);
    v128Value = WasmValue.v128(vectorData);
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() throws Exception {
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

  // Benchmarks for global creation

  @Benchmark
  public WasmGlobal createI32Global() throws WasmException {
    return store.createGlobal(WasmValueType.I32, true, WasmValue.i32(42));
  }

  @Benchmark
  public WasmGlobal createI64Global() throws WasmException {
    return store.createGlobal(WasmValueType.I64, true, WasmValue.i64(123456789L));
  }

  @Benchmark
  public WasmGlobal createF32Global() throws WasmException {
    return store.createGlobal(WasmValueType.F32, true, WasmValue.f32(3.14f));
  }

  @Benchmark
  public WasmGlobal createF64Global() throws WasmException {
    return store.createGlobal(WasmValueType.F64, true, WasmValue.f64(2.718));
  }

  @Benchmark
  public WasmGlobal createV128Global() throws WasmException {
    final byte[] vectorData = new byte[16];
    for (int i = 0; i < 16; i++) {
      vectorData[i] = (byte) i;
    }
    return store.createGlobal(WasmValueType.V128, true, WasmValue.v128(vectorData));
  }

  @Benchmark
  public WasmGlobal createImmutableI32Global() throws WasmException {
    return store.createImmutableGlobal(WasmValueType.I32, WasmValue.i32(42));
  }

  @Benchmark
  public WasmGlobal createMutableI32Global() throws WasmException {
    return store.createMutableGlobal(WasmValueType.I32, WasmValue.i32(42));
  }

  // Benchmarks for global get operations

  @Benchmark
  public WasmValue getI32Global() {
    return i32Global.get();
  }

  @Benchmark
  public WasmValue getI64Global() {
    return i64Global.get();
  }

  @Benchmark
  public WasmValue getF32Global() {
    return f32Global.get();
  }

  @Benchmark
  public WasmValue getF64Global() {
    return f64Global.get();
  }

  @Benchmark
  public WasmValue getV128Global() {
    return v128Global.get();
  }

  // Benchmarks for global set operations

  @Benchmark
  public void setI32Global() {
    i32Global.set(i32Value);
  }

  @Benchmark
  public void setI64Global() {
    i64Global.set(i64Value);
  }

  @Benchmark
  public void setF32Global() {
    f32Global.set(f32Value);
  }

  @Benchmark
  public void setF64Global() {
    f64Global.set(f64Value);
  }

  @Benchmark
  public void setV128Global() {
    v128Global.set(v128Value);
  }

  // Benchmarks for combined get/set operations

  @Benchmark
  public WasmValue getSetI32Global() {
    i32Global.set(i32Value);
    return i32Global.get();
  }

  @Benchmark
  public WasmValue getSetI64Global() {
    i64Global.set(i64Value);
    return i64Global.get();
  }

  @Benchmark
  public WasmValue getSetF32Global() {
    f32Global.set(f32Value);
    return f32Global.get();
  }

  @Benchmark
  public WasmValue getSetF64Global() {
    f64Global.set(f64Value);
    return f64Global.get();
  }

  // Benchmarks for type introspection

  @Benchmark
  public WasmValueType getGlobalType() {
    return i32Global.getType();
  }

  @Benchmark
  public boolean isGlobalMutable() {
    return i32Global.isMutable();
  }

  // Benchmarks for value creation (overhead measurement)

  @Benchmark
  public WasmValue createI32Value() {
    return WasmValue.i32(42);
  }

  @Benchmark
  public WasmValue createI64Value() {
    return WasmValue.i64(123456789L);
  }

  @Benchmark
  public WasmValue createF32Value() {
    return WasmValue.f32(3.14f);
  }

  @Benchmark
  public WasmValue createF64Value() {
    return WasmValue.f64(2.718);
  }

  @Benchmark
  public WasmValue createV128Value() {
    final byte[] vectorData = new byte[16];
    for (int i = 0; i < 16; i++) {
      vectorData[i] = (byte) i;
    }
    return WasmValue.v128(vectorData);
  }

  // Benchmarks for type conversion overhead

  @Benchmark
  public int extractI32Value() {
    return i32Global.get().asInt();
  }

  @Benchmark
  public long extractI64Value() {
    return i64Global.get().asLong();
  }

  @Benchmark
  public float extractF32Value() {
    return f32Global.get().asFloat();
  }

  @Benchmark
  public double extractF64Value() {
    return f64Global.get().asDouble();
  }

  @Benchmark
  public byte[] extractV128Value() {
    return v128Global.get().asV128();
  }

  // Benchmarks for increment operations (common pattern)

  @Benchmark
  public int incrementI32Global() {
    final int currentValue = i32Global.get().asInt();
    final int newValue = currentValue + 1;
    i32Global.set(WasmValue.i32(newValue));
    return newValue;
  }

  @Benchmark
  public long incrementI64Global() {
    final long currentValue = i64Global.get().asLong();
    final long newValue = currentValue + 1;
    i64Global.set(WasmValue.i64(newValue));
    return newValue;
  }

  @Benchmark
  public float addF32Global() {
    final float currentValue = f32Global.get().asFloat();
    final float newValue = currentValue + 1.0f;
    f32Global.set(WasmValue.f32(newValue));
    return newValue;
  }

  @Benchmark
  public double addF64Global() {
    final double currentValue = f64Global.get().asDouble();
    final double newValue = currentValue + 1.0;
    f64Global.set(WasmValue.f64(newValue));
    return newValue;
  }

  // Benchmark for bulk operations

  @Benchmark
  public void bulkGlobalOperations() throws WasmException {
    // Create multiple globals
    final WasmGlobal g1 = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(1));
    final WasmGlobal g2 = store.createGlobal(WasmValueType.I64, true, WasmValue.i64(2L));
    final WasmGlobal g3 = store.createGlobal(WasmValueType.F32, true, WasmValue.f32(3.0f));

    // Perform operations
    g1.set(WasmValue.i32(g1.get().asInt() + 10));
    g2.set(WasmValue.i64(g2.get().asLong() + 20));
    g3.set(WasmValue.f32(g3.get().asFloat() + 30.0f));

    // Read final values
    g1.get();
    g2.get();
    g3.get();
  }
}
