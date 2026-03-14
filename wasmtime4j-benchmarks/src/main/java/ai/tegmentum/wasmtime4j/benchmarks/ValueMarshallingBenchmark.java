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
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for value marshalling operations in the JNI type conversion pipeline.
 *
 * <p>These benchmarks measure the critical hot path for every Wasm function call: converting
 * between Java WasmValue objects and native representations. Run with:
 *
 * <pre>
 * ./mvnw compile exec:java -pl wasmtime4j-benchmarks -P skip-native \
 *   -Djmh.benchmarks=".*ValueMarshallingBenchmark.*"
 * </pre>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1g", "-Xmx1g"})
public class ValueMarshallingBenchmark {

  // --- Pre-allocated test data ---

  private WasmValue i32Val;
  private WasmValue i64Val;
  private WasmValue f32Val;
  private WasmValue f64Val;
  private WasmValue[] mixedParams;
  private WasmValueType[] mixedTypes;
  private byte[] marshalledParams;
  private WasmValueType[] paramTypeSignature;
  private WasmValueType[] resultTypeSignature;
  private String[] typeStrings;

  @Setup
  public void setup() {
    i32Val = WasmValue.i32(42);
    i64Val = WasmValue.i64(123456789L);
    f32Val = WasmValue.f32(3.14f);
    f64Val = WasmValue.f64(2.71828);

    // Typical function signature: add(i32, i32) -> i32
    mixedParams = new WasmValue[] {WasmValue.i32(10), WasmValue.i32(20)};
    mixedTypes = new WasmValueType[] {WasmValueType.I32, WasmValueType.I32};

    // Larger signature: multi-param(i32, i64, f32, f64, i32, i64) -> (i32, f64)
    paramTypeSignature =
        new WasmValueType[] {
          WasmValueType.I32, WasmValueType.I64, WasmValueType.F32,
          WasmValueType.F64, WasmValueType.I32, WasmValueType.I64
        };
    resultTypeSignature = new WasmValueType[] {WasmValueType.I32, WasmValueType.F64};

    typeStrings = new String[] {"i32", "i64", "f32", "f64", "i32", "i64"};

    // Pre-marshal for unmarshal benchmarks
    WasmValue[] largeParams =
        new WasmValue[] {
          WasmValue.i32(1), WasmValue.i64(2L), WasmValue.f32(3.0f),
          WasmValue.f64(4.0), WasmValue.i32(5), WasmValue.i64(6L)
        };
    marshalledParams = JniTypeConverter.marshalParameters(largeParams);
  }

  // ==========================================
  // WasmValue factory method benchmarks
  // ==========================================

  @Benchmark
  public void wasmValueI32Creation(Blackhole bh) {
    bh.consume(WasmValue.i32(42));
  }

  @Benchmark
  public void wasmValueI64Creation(Blackhole bh) {
    bh.consume(WasmValue.i64(123456789L));
  }

  @Benchmark
  public void wasmValueF32Creation(Blackhole bh) {
    bh.consume(WasmValue.f32(3.14f));
  }

  @Benchmark
  public void wasmValueF64Creation(Blackhole bh) {
    bh.consume(WasmValue.f64(2.71828));
  }

  @Benchmark
  public void wasmValueMixedCreation(Blackhole bh) {
    bh.consume(WasmValue.i32(10));
    bh.consume(WasmValue.i64(20L));
    bh.consume(WasmValue.f32(30.0f));
    bh.consume(WasmValue.f64(40.0));
  }

  // ==========================================
  // Type string conversion benchmarks
  // ==========================================

  @Benchmark
  public void typeToStringSingle(Blackhole bh) {
    bh.consume(JniTypeConverter.typeToString(WasmValueType.I32));
  }

  @Benchmark
  public void typeToStringAll(Blackhole bh) {
    bh.consume(JniTypeConverter.typeToString(WasmValueType.I32));
    bh.consume(JniTypeConverter.typeToString(WasmValueType.I64));
    bh.consume(JniTypeConverter.typeToString(WasmValueType.F32));
    bh.consume(JniTypeConverter.typeToString(WasmValueType.F64));
    bh.consume(JniTypeConverter.typeToString(WasmValueType.FUNCREF));
    bh.consume(JniTypeConverter.typeToString(WasmValueType.EXTERNREF));
  }

  @Benchmark
  public void stringToTypeSingle(Blackhole bh) {
    bh.consume(JniTypeConverter.stringToType("i32"));
  }

  @Benchmark
  public void stringToTypeAll(Blackhole bh) {
    bh.consume(JniTypeConverter.stringToType("i32"));
    bh.consume(JniTypeConverter.stringToType("i64"));
    bh.consume(JniTypeConverter.stringToType("f32"));
    bh.consume(JniTypeConverter.stringToType("f64"));
    bh.consume(JniTypeConverter.stringToType("funcref"));
    bh.consume(JniTypeConverter.stringToType("externref"));
  }

  @Benchmark
  public void typesToStringsSignature(Blackhole bh) {
    bh.consume(JniTypeConverter.typesToStrings(paramTypeSignature));
  }

  @Benchmark
  public void stringsToTypesSignature(Blackhole bh) {
    bh.consume(JniTypeConverter.stringsToTypes(typeStrings));
  }

  // ==========================================
  // Binary marshalling benchmarks
  // ==========================================

  @Benchmark
  public void marshalTwoI32Params(Blackhole bh) {
    bh.consume(JniTypeConverter.marshalParameters(mixedParams));
  }

  @Benchmark
  public void marshalSixMixedParams(Blackhole bh) {
    WasmValue[] params = new WasmValue[] {i32Val, i64Val, f32Val, f64Val, i32Val, i64Val};
    bh.consume(JniTypeConverter.marshalParameters(params));
  }

  @Benchmark
  public void unmarshalSixMixedParams(Blackhole bh) {
    bh.consume(JniTypeConverter.unmarshalParameters(marshalledParams, paramTypeSignature));
  }

  // ==========================================
  // Value extraction benchmarks
  // ==========================================

  @Benchmark
  public void extractI32Value(Blackhole bh) {
    bh.consume(i32Val.asInt());
  }

  @Benchmark
  public void extractI64Value(Blackhole bh) {
    bh.consume(i64Val.asLong());
  }

  @Benchmark
  public void wasmValueToNativeParamI32(Blackhole bh) {
    bh.consume(JniTypeConverter.wasmValueToNativeParam(i32Val));
  }

  @Benchmark
  public void wasmValueToNativeParamF64(Blackhole bh) {
    bh.consume(JniTypeConverter.wasmValueToNativeParam(f64Val));
  }

  @Benchmark
  public void wasmValuesToNativeParamsMixed(Blackhole bh) {
    bh.consume(JniTypeConverter.wasmValuesToNativeParams(mixedParams));
  }

  // ==========================================
  // Native result to WasmValue benchmarks
  // ==========================================

  @Benchmark
  public void nativeResultToWasmValueI32(Blackhole bh) {
    bh.consume(JniTypeConverter.nativeResultToWasmValue(Integer.valueOf(42), WasmValueType.I32));
  }

  @Benchmark
  public void nativeResultToWasmValueF64(Blackhole bh) {
    bh.consume(
        JniTypeConverter.nativeResultToWasmValue(Double.valueOf(2.71828), WasmValueType.F64));
  }

  // ==========================================
  // Full round-trip benchmarks (most realistic)
  // ==========================================

  @Benchmark
  public void fullRoundTripTwoI32(Blackhole bh) {
    // Simulates: create params -> marshal -> unmarshal -> extract
    WasmValue[] params = new WasmValue[] {WasmValue.i32(10), WasmValue.i32(20)};
    byte[] marshalled = JniTypeConverter.marshalParameters(params);
    WasmValue[] results = JniTypeConverter.unmarshalResults(marshalled, mixedTypes);
    bh.consume(results[0].asInt());
    bh.consume(results[1].asInt());
  }

  @Benchmark
  public void getMarshalSizeSingle(Blackhole bh) {
    bh.consume(WasmValueType.I32.getMarshalSize());
  }

  @Benchmark
  public void getMarshalSizeSignature(Blackhole bh) {
    for (WasmValueType type : paramTypeSignature) {
      bh.consume(type.getMarshalSize());
    }
  }

  @Benchmark
  public void fullRoundTripSixMixed(Blackhole bh) {
    WasmValue[] params =
        new WasmValue[] {
          WasmValue.i32(1), WasmValue.i64(2L), WasmValue.f32(3.0f),
          WasmValue.f64(4.0), WasmValue.i32(5), WasmValue.i64(6L)
        };
    byte[] marshalled = JniTypeConverter.marshalParameters(params);
    WasmValue[] results = JniTypeConverter.unmarshalParameters(marshalled, paramTypeSignature);
    for (WasmValue result : results) {
      bh.consume(result.getValue());
    }
  }
}
