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
 * Benchmarks for parameter/result marshalling overhead in the JNI call path.
 *
 * <p>Measures allocation and conversion costs for:
 *
 * <ul>
 *   <li>Empty array allocation avoidance for void/no-arg functions
 *   <li>WasmValue-to-native parameter conversion
 *   <li>Native result-to-WasmValue conversion
 *   <li>Binary marshal/unmarshal for host callback path
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1g", "-Xmx1g"})
public class MarshallingBenchmark {

  private static final WasmValue[] EMPTY_PARAMS = new WasmValue[0];
  private WasmValue[] oneI32Param;
  private WasmValue[] twoI32Params;
  private WasmValue[] fourI32Params;

  private Object[] emptyResults;
  private Object[] oneI32Result;
  private Object[] twoI32Results;

  private WasmValueType[] emptyTypes;
  private WasmValueType[] oneI32Type;
  private WasmValueType[] twoI32Types;
  private WasmValueType[] fourI32Types;

  // Binary marshal/unmarshal data
  private byte[] emptyParamsData;
  private byte[] oneI32ParamsData;
  private byte[] fourI32ParamsData;
  private WasmValue[] oneI32Results;
  private byte[] resultsBuffer;

  @Setup
  public void setup() {
    oneI32Param = new WasmValue[] {WasmValue.i32(42)};
    twoI32Params = new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2)};
    fourI32Params =
        new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3), WasmValue.i32(4)};

    emptyResults = new Object[0];
    oneI32Result = new Object[] {Integer.valueOf(42)};
    twoI32Results = new Object[] {Integer.valueOf(1), Integer.valueOf(2)};

    emptyTypes = new WasmValueType[0];
    oneI32Type = new WasmValueType[] {WasmValueType.I32};
    twoI32Types = new WasmValueType[] {WasmValueType.I32, WasmValueType.I32};
    fourI32Types =
        new WasmValueType[] {
          WasmValueType.I32, WasmValueType.I32, WasmValueType.I32, WasmValueType.I32
        };

    // Pre-build binary marshal data
    emptyParamsData = JniTypeConverter.marshalParameters(EMPTY_PARAMS);
    oneI32ParamsData = JniTypeConverter.marshalParameters(oneI32Param);
    fourI32ParamsData = JniTypeConverter.marshalParameters(fourI32Params);
    oneI32Results = new WasmValue[] {WasmValue.i32(99)};
    resultsBuffer = new byte[64];
  }

  // ==========================================
  // wasmValuesToNativeParams
  // ==========================================

  @Benchmark
  public void paramsToNativeEmpty(Blackhole bh) {
    bh.consume(JniTypeConverter.wasmValuesToNativeParams(EMPTY_PARAMS));
  }

  @Benchmark
  public void paramsToNativeOneI32(Blackhole bh) {
    bh.consume(JniTypeConverter.wasmValuesToNativeParams(oneI32Param));
  }

  @Benchmark
  public void paramsToNativeTwoI32(Blackhole bh) {
    bh.consume(JniTypeConverter.wasmValuesToNativeParams(twoI32Params));
  }

  @Benchmark
  public void paramsToNativeFourI32(Blackhole bh) {
    bh.consume(JniTypeConverter.wasmValuesToNativeParams(fourI32Params));
  }

  // ==========================================
  // nativeResultsToWasmValues
  // ==========================================

  @Benchmark
  public void resultsToWasmEmpty(Blackhole bh) {
    bh.consume(JniTypeConverter.nativeResultsToWasmValues(emptyResults, emptyTypes));
  }

  @Benchmark
  public void resultsToWasmOneI32(Blackhole bh) {
    bh.consume(JniTypeConverter.nativeResultsToWasmValues(oneI32Result, oneI32Type));
  }

  @Benchmark
  public void resultsToWasmTwoI32(Blackhole bh) {
    bh.consume(JniTypeConverter.nativeResultsToWasmValues(twoI32Results, twoI32Types));
  }

  // ==========================================
  // Binary unmarshal (host callback path)
  // ==========================================

  @Benchmark
  public void unmarshalParamsEmpty(Blackhole bh) {
    bh.consume(JniTypeConverter.unmarshalParameters(emptyParamsData, emptyTypes));
  }

  @Benchmark
  public void unmarshalParamsOneI32(Blackhole bh) {
    bh.consume(JniTypeConverter.unmarshalParameters(oneI32ParamsData, oneI32Type));
  }

  @Benchmark
  public void unmarshalParamsFourI32(Blackhole bh) {
    bh.consume(JniTypeConverter.unmarshalParameters(fourI32ParamsData, fourI32Types));
  }

  // ==========================================
  // Binary marshal results (host callback path)
  // ==========================================

  @Benchmark
  public void marshalResultsEmpty(Blackhole bh) {
    JniTypeConverter.marshalResults(EMPTY_PARAMS, resultsBuffer);
    bh.consume(resultsBuffer);
  }

  @Benchmark
  public void marshalResultsOneI32(Blackhole bh) {
    JniTypeConverter.marshalResults(oneI32Results, resultsBuffer);
    bh.consume(resultsBuffer);
  }

  // ==========================================
  // Full simulated call path (marshal + unmarshal)
  // ==========================================

  @Benchmark
  public void fullMarshalRoundTripEmpty(Blackhole bh) {
    // Simulate: call() with no params and void return
    Object[] nativeParams = JniTypeConverter.wasmValuesToNativeParams(EMPTY_PARAMS);
    bh.consume(nativeParams);
    WasmValue[] results = JniTypeConverter.nativeResultsToWasmValues(emptyResults, emptyTypes);
    bh.consume(results);
  }

  @Benchmark
  public void fullMarshalRoundTripOneI32(Blackhole bh) {
    // Simulate: call(i32) -> i32
    Object[] nativeParams = JniTypeConverter.wasmValuesToNativeParams(oneI32Param);
    bh.consume(nativeParams);
    WasmValue[] results = JniTypeConverter.nativeResultsToWasmValues(oneI32Result, oneI32Type);
    bh.consume(results);
  }

  @Benchmark
  public void fullHostCallbackRoundTripOneI32(Blackhole bh) {
    // Simulate: host callback with 1 i32 param, 1 i32 result
    WasmValue[] params = JniTypeConverter.unmarshalParameters(oneI32ParamsData, oneI32Type);
    bh.consume(params);
    JniTypeConverter.marshalResults(oneI32Results, resultsBuffer);
    bh.consume(resultsBuffer);
  }
}
