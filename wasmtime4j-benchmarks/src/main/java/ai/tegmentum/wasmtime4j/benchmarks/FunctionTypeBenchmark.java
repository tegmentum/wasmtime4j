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

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for FunctionType access patterns.
 *
 * <p>Measures the cost of array cloning in getParamTypes()/getReturnTypes() versus
 * non-cloning alternatives like getParamCount()/getReturnCount() and cached return types.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1g", "-Xmx1g"})
public class FunctionTypeBenchmark {

  private FunctionType twoParamType;
  private FunctionType sixParamType;
  private WasmValueType[] cachedReturnTypes;

  @Setup
  public void setup() {
    twoParamType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});
    sixParamType =
        new FunctionType(
            new WasmValueType[] {
              WasmValueType.I32, WasmValueType.I64, WasmValueType.F32,
              WasmValueType.F64, WasmValueType.I32, WasmValueType.I64
            },
            new WasmValueType[] {WasmValueType.I32, WasmValueType.F64});
    cachedReturnTypes = sixParamType.getReturnTypes();
  }

  // ==========================================
  // Cloning vs non-cloning access patterns
  // ==========================================

  @Benchmark
  public void getParamTypesClone(Blackhole bh) {
    bh.consume(twoParamType.getParamTypes());
  }

  @Benchmark
  public void getParamCount(Blackhole bh) {
    bh.consume(twoParamType.getParamCount());
  }

  @Benchmark
  public void getReturnTypesClone(Blackhole bh) {
    bh.consume(twoParamType.getReturnTypes());
  }

  @Benchmark
  public void getReturnCount(Blackhole bh) {
    bh.consume(twoParamType.getReturnCount());
  }

  // ==========================================
  // Simulated call() hot path patterns
  // ==========================================

  @Benchmark
  public void simulatedCallCloning(Blackhole bh) {
    // Before optimization: clone paramTypes for length check + clone returnTypes for unmarshal
    bh.consume(twoParamType.getParamTypes().length);
    bh.consume(twoParamType.getReturnTypes());
  }

  @Benchmark
  public void simulatedCallOptimized(Blackhole bh) {
    // After optimization: getParamCount() + cached return types
    bh.consume(twoParamType.getParamCount());
    bh.consume(cachedReturnTypes);
  }

  @Benchmark
  public void simulatedCallSixParamCloning(Blackhole bh) {
    bh.consume(sixParamType.getParamTypes().length);
    bh.consume(sixParamType.getReturnTypes());
  }

  @Benchmark
  public void simulatedCallSixParamOptimized(Blackhole bh) {
    bh.consume(sixParamType.getParamCount());
    bh.consume(cachedReturnTypes);
  }

  // ==========================================
  // Cached List access (getParams/getResults)
  // ==========================================

  @Benchmark
  public void getParamsList(Blackhole bh) {
    bh.consume(twoParamType.getParams());
  }

  @Benchmark
  public void getResultsList(Blackhole bh) {
    bh.consume(twoParamType.getResults());
  }
}
