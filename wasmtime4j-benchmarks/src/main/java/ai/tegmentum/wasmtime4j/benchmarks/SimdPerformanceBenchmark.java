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

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.simd.SimdLane;
import ai.tegmentum.wasmtime4j.simd.SimdOperations;
import ai.tegmentum.wasmtime4j.simd.SimdVector;
import ai.tegmentum.wasmtime4j.simd.V128;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Comprehensive performance benchmarks for SIMD operations.
 *
 * <p>This benchmark suite compares SIMD vectorized operations against scalar equivalents to
 * demonstrate the performance benefits of SIMD instruction usage. The benchmarks cover:
 *
 * <ul>
 *   <li>Arithmetic operations (add, subtract, multiply, divide)
 *   <li>Logical operations (and, or, xor, not)
 *   <li>Comparison operations (equals, less than, greater than)
 *   <li>Conversion operations between data types
 *   <li>Lane manipulation operations
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class SimdPerformanceBenchmark {

  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  private WasmRuntime runtime;
  private SimdOperations simdOps;
  private SimdVector vectorA;
  private SimdVector vectorB;
  private SimdVector vectorC;
  private SimdVector vectorD;

  // Scalar equivalents for comparison
  private int[] scalarIntsA;
  private int[] scalarIntsB;
  private float[] scalarFloatsA;
  private float[] scalarFloatsB;

  private static final int VECTOR_SIZE = 4;
  private static final int OPERATION_COUNT = 10000;

  @Setup(Level.Trial)
  public void setupTrial() throws WasmException {
    System.out.println("Setting up SIMD performance benchmark suite...");

    final RuntimeType type = RuntimeType.valueOf(runtimeTypeName);
    runtime = WasmRuntimeFactory.create(type);
    simdOps = runtime.getSimdOperations();

    // Initialize test vectors with varied data using V128 factory methods
    vectorA = V128.fromInts(100, 200, 300, 400).toSimdVector(SimdLane.I32X4);
    vectorB = V128.fromInts(10, 20, 30, 40).toSimdVector(SimdLane.I32X4);
    vectorC = V128.fromFloats(1.5f, 2.5f, 3.5f, 4.5f).toSimdVector(SimdLane.F32X4);
    vectorD = V128.fromFloats(0.5f, 1.0f, 1.5f, 2.0f).toSimdVector(SimdLane.F32X4);

    // Scalar equivalents
    scalarIntsA = new int[] {100, 200, 300, 400};
    scalarIntsB = new int[] {10, 20, 30, 40};
    scalarFloatsA = new float[] {1.5f, 2.5f, 3.5f, 4.5f};
    scalarFloatsB = new float[] {0.5f, 1.0f, 1.5f, 2.0f};

    System.out.println("SIMD benchmark setup complete");
    System.out.println("  - Runtime: " + runtimeTypeName);
    System.out.println("  - SIMD supported: " + simdOps.isSimdSupported());
    System.out.println("  - SIMD capabilities: " + simdOps.getSimdCapabilities());
  }

  @TearDown(Level.Trial)
  public void teardownTrial() throws Exception {
    if (runtime != null) {
      runtime.close();
    }
    System.out.println("SIMD benchmark cleanup complete");
  }

  // ===== ARITHMETIC OPERATIONS BENCHMARKS =====

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdAddition() throws Exception {
    SimdVector result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.add(result, vectorB);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public int[] scalarAddition() {
    int[] result = scalarIntsA.clone();
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] += scalarIntsB[j];
      }
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdSubtraction() throws Exception {
    SimdVector result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.subtract(result, vectorB);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public int[] scalarSubtraction() {
    int[] result = scalarIntsA.clone();
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] -= scalarIntsB[j];
      }
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdMultiplication() throws Exception {
    SimdVector result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.multiply(result, vectorB);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public int[] scalarMultiplication() {
    int[] result = scalarIntsA.clone();
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] *= scalarIntsB[j];
      }
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdDivision() throws Exception {
    SimdVector result = vectorC;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.divide(result, vectorD);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public float[] scalarDivision() {
    float[] result = scalarFloatsA.clone();
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] /= scalarFloatsB[j];
      }
    }
    return result;
  }

  // ===== LOGICAL OPERATIONS BENCHMARKS =====

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdBitwiseAnd() throws Exception {
    SimdVector result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.and(result, vectorB);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public int[] scalarBitwiseAnd() {
    int[] result = scalarIntsA.clone();
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] &= scalarIntsB[j];
      }
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdBitwiseOr() throws Exception {
    SimdVector result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.or(result, vectorB);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public int[] scalarBitwiseOr() {
    int[] result = scalarIntsA.clone();
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] |= scalarIntsB[j];
      }
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdBitwiseXor() throws Exception {
    SimdVector result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.xor(result, vectorB);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public int[] scalarBitwiseXor() {
    int[] result = scalarIntsA.clone();
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] ^= scalarIntsB[j];
      }
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdBitwiseNot() throws Exception {
    SimdVector result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.not(result);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public int[] scalarBitwiseNot() {
    int[] result = scalarIntsA.clone();
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] = ~result[j];
      }
    }
    return result;
  }

  // ===== COMPARISON OPERATIONS BENCHMARKS =====

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdEquals() throws Exception {
    SimdVector result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.equals(vectorA, vectorB);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public boolean[] scalarEquals() {
    boolean[] result = new boolean[VECTOR_SIZE];
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] = scalarIntsA[j] == scalarIntsB[j];
      }
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdLessThan() throws Exception {
    SimdVector result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.lessThan(vectorA, vectorB);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public boolean[] scalarLessThan() {
    boolean[] result = new boolean[VECTOR_SIZE];
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] = scalarIntsA[j] < scalarIntsB[j];
      }
    }
    return result;
  }

  // ===== CONVERSION OPERATIONS BENCHMARKS =====

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdIntToFloatConversion() throws Exception {
    SimdVector result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.convertI32ToF32(vectorA);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public float[] scalarIntToFloatConversion() {
    float[] result = new float[VECTOR_SIZE];
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] = (float) scalarIntsA[j];
      }
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdFloatToIntConversion() throws Exception {
    SimdVector result = vectorC;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.convertF32ToI32(vectorC);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public int[] scalarFloatToIntConversion() {
    int[] result = new int[VECTOR_SIZE];
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] = (int) scalarFloatsA[j];
      }
    }
    return result;
  }

  // ===== LANE OPERATIONS BENCHMARKS =====

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public int simdExtractLane() throws Exception {
    int result = 0;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.extractLaneI32(vectorA, i % VECTOR_SIZE);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public int scalarExtractElement() {
    int result = 0;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = scalarIntsA[i % VECTOR_SIZE];
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdReplaceLane() throws Exception {
    SimdVector result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.replaceLaneI32(result, i % VECTOR_SIZE, i);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public int[] scalarReplaceElement() {
    int[] result = scalarIntsA.clone();
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result[i % VECTOR_SIZE] = i;
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdVector simdSplat() throws Exception {
    SimdVector result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = SimdVector.splatI32(SimdLane.I32X4, i);
    }
    return result;
  }

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public int[] scalarSplat() {
    int[] result = new int[VECTOR_SIZE];
    for (int i = 0; i < OPERATION_COUNT; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] = i;
      }
    }
    return result;
  }

  // ===== MIXED WORKLOAD BENCHMARKS =====

  /** Benchmark that combines multiple SIMD operations to simulate real-world usage. */
  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT / 4)
  public SimdVector simdMixedWorkload() throws Exception {
    SimdVector result = vectorA;

    for (int i = 0; i < OPERATION_COUNT / 4; i++) {
      result = simdOps.add(result, vectorB);
      result = simdOps.multiply(result, vectorB);
      result = simdOps.and(result, vectorA);
      result = simdOps.replaceLaneI32(result, 0, i);
    }
    return result;
  }

  /** Scalar equivalent of the mixed workload for comparison. */
  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT / 4)
  public int[] scalarMixedWorkload() {
    int[] result = scalarIntsA.clone();

    for (int i = 0; i < OPERATION_COUNT / 4; i++) {
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] += scalarIntsB[j];
        result[j] *= scalarIntsB[j];
        result[j] &= scalarIntsA[j];
      }
      result[0] = i;
    }
    return result;
  }
}
