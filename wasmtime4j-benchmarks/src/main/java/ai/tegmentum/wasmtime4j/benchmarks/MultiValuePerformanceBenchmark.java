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
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Performance benchmarks comparing multi-value vs single-value WebAssembly function operations.
 *
 * <p>This benchmark suite measures the performance impact of the WebAssembly multi-value proposal
 * implementation across different scenarios: - Single-value vs multi-value function calls -
 * Parameter marshaling overhead - Return value unmarshaling overhead - Host function execution
 * overhead - Type validation overhead
 *
 * <p>Benchmarks use JMH (Java Microbenchmark Harness) for accurate performance measurement with
 * proper warm-up and statistical analysis.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms2g", "-Xmx2g"})
public class MultiValuePerformanceBenchmark {

  // Test data sets for different scenarios
  private WasmValue[] singleParam;
  private WasmValue[] twoParams;
  private WasmValue[] fourParams;
  private WasmValue[] eightParams;
  private WasmValue[] sixteenParams;

  private WasmValue[] singleResult;
  private WasmValue[] twoResults;
  private WasmValue[] fourResults;
  private WasmValue[] eightResults;

  // Host functions for different return value counts
  private HostFunction singleValueHostFunction;
  private HostFunction twoValueHostFunction;
  private HostFunction fourValueHostFunction;
  private HostFunction eightValueHostFunction;

  // Function types for validation benchmarks
  private FunctionType singleValueFunctionType;
  private FunctionType multiValueFunctionType;
  private FunctionType largeMultiValueFunctionType;

  @Setup
  public void setup() {
    // Initialize test parameters
    singleParam = new WasmValue[] {WasmValue.i32(42)};
    twoParams = new WasmValue[] {WasmValue.i32(10), WasmValue.i32(20)};
    fourParams =
        new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3), WasmValue.i32(4)};
    eightParams =
        new WasmValue[] {
          WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3), WasmValue.i32(4),
          WasmValue.i32(5), WasmValue.i32(6), WasmValue.i32(7), WasmValue.i32(8)
        };
    sixteenParams = new WasmValue[16];
    for (int i = 0; i < 16; i++) {
      sixteenParams[i] = WasmValue.i32(i + 1);
    }

    // Initialize test results
    singleResult = new WasmValue[] {WasmValue.i32(100)};
    twoResults = new WasmValue[] {WasmValue.i32(100), WasmValue.i32(200)};
    fourResults =
        new WasmValue[] {
          WasmValue.i32(100), WasmValue.i32(200), WasmValue.i32(300), WasmValue.i32(400)
        };
    eightResults =
        new WasmValue[] {
          WasmValue.i32(100), WasmValue.i32(200), WasmValue.i32(300), WasmValue.i32(400),
          WasmValue.i32(500), WasmValue.i32(600), WasmValue.i32(700), WasmValue.i32(800)
        };

    // Initialize host functions
    singleValueHostFunction =
        HostFunction.singleValue((params) -> WasmValue.i32(params[0].asInt() * 2));

    twoValueHostFunction =
        HostFunction.multiValue(
            (params) -> {
              int value = params[0].asInt();
              return WasmValue.multiValue(WasmValue.i32(value * 2), WasmValue.i32(value * 3));
            });

    fourValueHostFunction =
        HostFunction.multiValue(
            (params) -> {
              int base = params[0].asInt();
              return WasmValue.multiValue(
                  WasmValue.i32(base * 1),
                  WasmValue.i32(base * 2),
                  WasmValue.i32(base * 3),
                  WasmValue.i32(base * 4));
            });

    eightValueHostFunction =
        HostFunction.multiValue(
            (params) -> {
              int base = params[0].asInt();
              return WasmValue.multiValue(
                  WasmValue.i32(base * 1), WasmValue.i32(base * 2),
                  WasmValue.i32(base * 3), WasmValue.i32(base * 4),
                  WasmValue.i32(base * 5), WasmValue.i32(base * 6),
                  WasmValue.i32(base * 7), WasmValue.i32(base * 8));
            });

    // Initialize function types
    singleValueFunctionType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

    multiValueFunctionType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32, WasmValueType.I32});

    WasmValueType[] manyParams = new WasmValueType[8];
    WasmValueType[] manyReturns = new WasmValueType[8];
    for (int i = 0; i < 8; i++) {
      manyParams[i] = WasmValueType.I32;
      manyReturns[i] = WasmValueType.I32;
    }
    largeMultiValueFunctionType = new FunctionType(manyParams, manyReturns);
  }

  // =================== Parameter Creation Benchmarks ===================

  @Benchmark
  public WasmValue[] createSingleParameter(Blackhole bh) {
    return WasmValue.multiValue(WasmValue.i32(42));
  }

  @Benchmark
  public WasmValue[] createTwoParameters(Blackhole bh) {
    return WasmValue.multiValue(WasmValue.i32(10), WasmValue.i32(20));
  }

  @Benchmark
  public WasmValue[] createFourParameters(Blackhole bh) {
    return WasmValue.multiValue(
        WasmValue.i32(1), WasmValue.i32(2),
        WasmValue.i32(3), WasmValue.i32(4));
  }

  @Benchmark
  public WasmValue[] createEightParameters(Blackhole bh) {
    return WasmValue.multiValue(
        WasmValue.i32(1),
        WasmValue.i32(2),
        WasmValue.i32(3),
        WasmValue.i32(4),
        WasmValue.i32(5),
        WasmValue.i32(6),
        WasmValue.i32(7),
        WasmValue.i32(8));
  }

  // =================== Validation Benchmarks ===================

  @Benchmark
  public void validateSingleValue(Blackhole bh) {
    WasmValue.validateMultiValue(singleResult, new WasmValueType[] {WasmValueType.I32});
  }

  @Benchmark
  public void validateTwoValues(Blackhole bh) {
    WasmValue.validateMultiValue(
        twoResults, new WasmValueType[] {WasmValueType.I32, WasmValueType.I32});
  }

  @Benchmark
  public void validateFourValues(Blackhole bh) {
    WasmValue.validateMultiValue(
        fourResults,
        new WasmValueType[] {
          WasmValueType.I32, WasmValueType.I32,
          WasmValueType.I32, WasmValueType.I32
        });
  }

  @Benchmark
  public void validateEightValues(Blackhole bh) {
    WasmValue.validateMultiValue(
        eightResults,
        new WasmValueType[] {
          WasmValueType.I32, WasmValueType.I32, WasmValueType.I32, WasmValueType.I32,
          WasmValueType.I32, WasmValueType.I32, WasmValueType.I32, WasmValueType.I32
        });
  }

  // =================== FunctionType Benchmarks ===================

  @Benchmark
  public FunctionType createSingleValueFunctionType(Blackhole bh) {
    return new FunctionType(
        new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
  }

  @Benchmark
  public FunctionType createMultiValueFunctionType(Blackhole bh) {
    return FunctionType.of(
        new WasmValueType[] {WasmValueType.I32, WasmValueType.F64},
        new WasmValueType[] {WasmValueType.I32, WasmValueType.I64, WasmValueType.F32});
  }

  @Benchmark
  public void validateParametersSingle(Blackhole bh) {
    singleValueFunctionType.validateParameters(singleParam);
  }

  @Benchmark
  public void validateParametersMultiple(Blackhole bh) {
    multiValueFunctionType.validateParameters(twoParams);
  }

  @Benchmark
  public void validateReturnValuesSingle(Blackhole bh) {
    singleValueFunctionType.validateReturnValues(singleResult);
  }

  @Benchmark
  public void validateReturnValuesMultiple(Blackhole bh) {
    largeMultiValueFunctionType.validateReturnValues(eightResults);
  }

  @Benchmark
  public boolean isCompatibleWithPattern(Blackhole bh) {
    return multiValueFunctionType.isCompatibleWith(
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32, WasmValueType.I32}));
  }

  // =================== Host Function Execution Benchmarks ===================

  @Benchmark
  public WasmValue[] executeSingleValueHostFunction(Blackhole bh) throws WasmException {
    return singleValueHostFunction.execute(singleParam);
  }

  @Benchmark
  public WasmValue[] executeTwoValueHostFunction(Blackhole bh) throws WasmException {
    return twoValueHostFunction.execute(singleParam);
  }

  @Benchmark
  public WasmValue[] executeFourValueHostFunction(Blackhole bh) throws WasmException {
    return fourValueHostFunction.execute(singleParam);
  }

  @Benchmark
  public WasmValue[] executeEightValueHostFunction(Blackhole bh) throws WasmException {
    return eightValueHostFunction.execute(singleParam);
  }

  // =================== Comparison Benchmarks ===================

  /**
   * Compares single-value function call overhead vs multi-value function call overhead. This
   * benchmark simulates the difference in performance between calling a function that returns one
   * value vs calling a function that returns multiple values.
   */
  @Benchmark
  public int singleValueOperationSimulation(Blackhole bh) {
    // Simulate single value function call overhead
    WasmValue param = WasmValue.i32(42);
    WasmValue result = WasmValue.i32(param.asInt() * 2);
    bh.consume(result);
    return result.asInt();
  }

  /** Simulates multi-value function call overhead for comparison. */
  @Benchmark
  public WasmValue[] multiValueOperationSimulation(Blackhole bh) {
    // Simulate multi-value function call overhead
    WasmValue param = WasmValue.i32(42);
    int value = param.asInt();
    WasmValue[] results =
        WasmValue.multiValue(
            WasmValue.i32(value * 2), WasmValue.i32(value * 3), WasmValue.i32(value * 4));
    bh.consume(results);
    return results;
  }

  /**
   * Benchmarks the overhead of accessing the first value from a multi-value result vs accessing a
   * single-value result.
   */
  @Benchmark
  public int accessFirstValueFromMultiple(Blackhole bh) {
    WasmValue first = fourResults[0];
    bh.consume(first);
    return first.asInt();
  }

  @Benchmark
  public int accessSingleValue(Blackhole bh) {
    WasmValue single = singleResult[0];
    bh.consume(single);
    return single.asInt();
  }
}
