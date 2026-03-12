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
 * JMH benchmarks for WasmValueType operations in the hot path.
 *
 * <p>These benchmarks measure type code conversion and type classification operations that execute
 * on every Wasm value marshalled or unmarshalled.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1g", "-Xmx1g"})
public class WasmValueTypeBenchmark {

  private WasmValueType[] allTypes;
  private WasmValueType[] commonTypes;
  private int[] allTypeCodes;
  private int[] commonTypeCodes;

  @Setup
  public void setup() {
    allTypes = WasmValueType.values();
    commonTypes =
        new WasmValueType[] {
          WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64
        };
    allTypeCodes = new int[allTypes.length];
    for (int i = 0; i < allTypes.length; i++) {
      allTypeCodes[i] = allTypes[i].toNativeTypeCode();
    }
    commonTypeCodes = new int[] {0, 1, 2, 3};
  }

  // ==========================================
  // fromNativeTypeCode benchmarks
  // ==========================================

  @Benchmark
  public void fromNativeTypeCodeSingle(Blackhole bh) {
    bh.consume(WasmValueType.fromNativeTypeCode(0));
  }

  @Benchmark
  public void fromNativeTypeCodeCommon(Blackhole bh) {
    for (int code : commonTypeCodes) {
      bh.consume(WasmValueType.fromNativeTypeCode(code));
    }
  }

  @Benchmark
  public void fromNativeTypeCodeAll(Blackhole bh) {
    for (int code : allTypeCodes) {
      bh.consume(WasmValueType.fromNativeTypeCode(code));
    }
  }

  // ==========================================
  // toNativeTypeCode benchmarks
  // ==========================================

  @Benchmark
  public void toNativeTypeCodeSingle(Blackhole bh) {
    bh.consume(WasmValueType.I32.toNativeTypeCode());
  }

  @Benchmark
  public void toNativeTypeCodeCommon(Blackhole bh) {
    for (WasmValueType type : commonTypes) {
      bh.consume(type.toNativeTypeCode());
    }
  }

  @Benchmark
  public void toNativeTypeCodeAll(Blackhole bh) {
    for (WasmValueType type : allTypes) {
      bh.consume(type.toNativeTypeCode());
    }
  }

  // ==========================================
  // Type classification benchmarks
  // ==========================================

  @Benchmark
  public void isReferenceAll(Blackhole bh) {
    for (WasmValueType type : allTypes) {
      bh.consume(type.isReference());
    }
  }

  @Benchmark
  public void isGcReferenceAll(Blackhole bh) {
    for (WasmValueType type : allTypes) {
      bh.consume(type.isGcReference());
    }
  }

  @Benchmark
  public void isNullableReferenceAll(Blackhole bh) {
    for (WasmValueType type : allTypes) {
      bh.consume(type.isNullableReference());
    }
  }

  @Benchmark
  public void allClassificationChecks(Blackhole bh) {
    for (WasmValueType type : allTypes) {
      bh.consume(type.isReference());
      bh.consume(type.isGcReference());
      bh.consume(type.isNullableReference());
      bh.consume(type.isNumeric());
    }
  }

  // ==========================================
  // Round-trip benchmark (most realistic)
  // ==========================================

  @Benchmark
  public void roundTripTypeCodeCommon(Blackhole bh) {
    for (WasmValueType type : commonTypes) {
      int code = type.toNativeTypeCode();
      bh.consume(WasmValueType.fromNativeTypeCode(code));
    }
  }

  @Benchmark
  public void roundTripTypeCodeAll(Blackhole bh) {
    for (WasmValueType type : allTypes) {
      int code = type.toNativeTypeCode();
      bh.consume(WasmValueType.fromNativeTypeCode(code));
    }
  }
}
