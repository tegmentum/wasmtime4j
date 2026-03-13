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

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.jni.util.JniTypeConverter;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks for WasmValue creation patterns, measuring the impact of
 * flyweight caching for common values (small integers, zero values, null refs).
 *
 * <p>These benchmarks target the allocation hot path that runs on every Wasm
 * function call returning small integer results (booleans, error codes, counters).
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1g", "-Xmx1g"})
public class WasmValueCacheBenchmark {

  // ==========================================
  // i32 creation benchmarks (most common type)
  // ==========================================

  @Benchmark
  public void i32CreateZero(Blackhole bh) {
    bh.consume(WasmValue.i32(0));
  }

  @Benchmark
  public void i32CreateOne(Blackhole bh) {
    bh.consume(WasmValue.i32(1));
  }

  @Benchmark
  public void i32CreateSmallRange(Blackhole bh) {
    // Simulate typical return values: 0, 1, -1, small error codes
    bh.consume(WasmValue.i32(0));
    bh.consume(WasmValue.i32(1));
    bh.consume(WasmValue.i32(-1));
    bh.consume(WasmValue.i32(42));
    bh.consume(WasmValue.i32(100));
  }

  @Benchmark
  public void i32CreateLargeValue(Blackhole bh) {
    // Outside any cache range — always allocates
    bh.consume(WasmValue.i32(1_000_000));
  }

  // ==========================================
  // fromBoxed benchmarks (result path)
  // ==========================================

  @Benchmark
  public void fromBoxedI32Zero(Blackhole bh) {
    bh.consume(WasmValue.fromBoxed(WasmValueType.I32, Integer.valueOf(0)));
  }

  @Benchmark
  public void fromBoxedI32SmallRange(Blackhole bh) {
    bh.consume(WasmValue.fromBoxed(WasmValueType.I32, Integer.valueOf(0)));
    bh.consume(WasmValue.fromBoxed(WasmValueType.I32, Integer.valueOf(1)));
    bh.consume(WasmValue.fromBoxed(WasmValueType.I32, Integer.valueOf(-1)));
    bh.consume(WasmValue.fromBoxed(WasmValueType.I32, Integer.valueOf(42)));
  }

  // ==========================================
  // nativeResultToWasmValue (full result path)
  // ==========================================

  @Benchmark
  public void resultPathI32Zero(Blackhole bh) {
    bh.consume(JniTypeConverter.nativeResultToWasmValue(Integer.valueOf(0), WasmValueType.I32));
  }

  @Benchmark
  public void resultPathI32One(Blackhole bh) {
    bh.consume(JniTypeConverter.nativeResultToWasmValue(Integer.valueOf(1), WasmValueType.I32));
  }

  @Benchmark
  public void resultPathI32Large(Blackhole bh) {
    bh.consume(JniTypeConverter.nativeResultToWasmValue(Integer.valueOf(999999), WasmValueType.I32));
  }

  @Benchmark
  public void resultPathF64(Blackhole bh) {
    bh.consume(JniTypeConverter.nativeResultToWasmValue(Double.valueOf(3.14), WasmValueType.F64));
  }

  // ==========================================
  // Null reference creation (singleton candidates)
  // ==========================================

  @Benchmark
  public void nullFuncrefCreation(Blackhole bh) {
    bh.consume(WasmValue.nullFuncref());
  }

  @Benchmark
  public void nullExternrefCreation(Blackhole bh) {
    bh.consume(WasmValue.nullExternref());
  }

  // ==========================================
  // Simulated function call result patterns
  // ==========================================

  @Benchmark
  public void simulatedBooleanReturn(Blackhole bh) {
    // Wasm boolean: returns 0 or 1
    bh.consume(WasmValue.i32(1));
    bh.consume(WasmValue.i32(0));
  }

  @Benchmark
  public void simulatedErrorCodeReturn(Blackhole bh) {
    // Common error code pattern: 0=success, negative=error
    bh.consume(WasmValue.i32(0));
    bh.consume(WasmValue.i32(-1));
    bh.consume(WasmValue.i32(-2));
  }

  @Benchmark
  public void simulatedMultiValueReturn(Blackhole bh) {
    // Function returning (i32, i32) where values are small
    WasmValue[] results = new WasmValue[] {WasmValue.i32(0), WasmValue.i32(42)};
    bh.consume(results[0].asInt());
    bh.consume(results[1].asInt());
  }

  // ==========================================
  // Default value creation (used in initialization)
  // ==========================================

  @Benchmark
  public void defaultI32(Blackhole bh) {
    bh.consume(WasmValue.defaultForType(WasmValueType.I32));
  }

  @Benchmark
  public void defaultI64(Blackhole bh) {
    bh.consume(WasmValue.defaultForType(WasmValueType.I64));
  }

  @Benchmark
  public void defaultF64(Blackhole bh) {
    bh.consume(WasmValue.defaultForType(WasmValueType.F64));
  }
}
