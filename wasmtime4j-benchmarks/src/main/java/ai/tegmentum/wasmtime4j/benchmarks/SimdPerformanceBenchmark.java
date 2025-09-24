/*
 * Copyright 2024 Tegmentum Technology, Inc.
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

import ai.tegmentum.wasmtime4j.SimdOperations;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

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
 *   <li>Memory operations (load, store with alignment)
 *   <li>Conversion operations between data types
 *   <li>Lane manipulation operations
 * </ul>
 *
 * <p>Run with: {@code mvn clean compile exec:java -Dexec.mainClass="org.openjdk.jmh.Main"
 * -Dexec.args="SimdPerformanceBenchmark"}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class SimdPerformanceBenchmark {

  private WasmRuntime runtime;
  private SimdOperations simdOps;
  private SimdOperations.V128 vectorA;
  private SimdOperations.V128 vectorB;
  private SimdOperations.V128 vectorC;
  private SimdOperations.V128 vectorD;

  // Scalar equivalents for comparison
  private int[] scalarIntsA;
  private int[] scalarIntsB;
  private float[] scalarFloatsA;
  private float[] scalarFloatsB;

  private static final int VECTOR_SIZE = 4; // Number of elements in a v128 vector
  private static final int OPERATION_COUNT = 10000; // Number of operations per benchmark

  @Setup(Level.Trial)
  public void setupTrial() {
    System.out.println("Setting up SIMD performance benchmark suite...");

    runtime = WasmRuntimeFactory.create();
    simdOps = SimdOperations.create(runtime);

    // Initialize test vectors with varied data
    vectorA = SimdOperations.V128.fromInts(100, 200, 300, 400);
    vectorB = SimdOperations.V128.fromInts(10, 20, 30, 40);
    vectorC = SimdOperations.V128.fromFloats(1.5f, 2.5f, 3.5f, 4.5f);
    vectorD = SimdOperations.V128.fromFloats(0.5f, 1.0f, 1.5f, 2.0f);

    // Scalar equivalents
    scalarIntsA = new int[] {100, 200, 300, 400};
    scalarIntsB = new int[] {10, 20, 30, 40};
    scalarFloatsA = new float[] {1.5f, 2.5f, 3.5f, 4.5f};
    scalarFloatsB = new float[] {0.5f, 1.0f, 1.5f, 2.0f};

    System.out.println("✓ SIMD benchmark setup complete");
    System.out.println("  - SIMD supported: " + simdOps.isSimdSupported());
    System.out.println("  - SIMD capabilities: " + simdOps.getSimdCapabilities());
  }

  @TearDown(Level.Trial)
  public void teardownTrial() {
    if (runtime != null) {
      runtime.close();
    }
    System.out.println("✓ SIMD benchmark cleanup complete");
  }

  // ===== ARITHMETIC OPERATIONS BENCHMARKS =====

  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT)
  public SimdOperations.V128 simdAddition() throws Exception {
    SimdOperations.V128 result = vectorA;
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
  public SimdOperations.V128 simdSubtraction() throws Exception {
    SimdOperations.V128 result = vectorA;
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
  public SimdOperations.V128 simdMultiplication() throws Exception {
    SimdOperations.V128 result = vectorA;
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
  public SimdOperations.V128 simdDivision() throws Exception {
    SimdOperations.V128 result = vectorC;
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
  public SimdOperations.V128 simdBitwiseAnd() throws Exception {
    SimdOperations.V128 result = vectorA;
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
  public SimdOperations.V128 simdBitwiseOr() throws Exception {
    SimdOperations.V128 result = vectorA;
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
  public SimdOperations.V128 simdBitwiseXor() throws Exception {
    SimdOperations.V128 result = vectorA;
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
  public SimdOperations.V128 simdBitwiseNot() throws Exception {
    SimdOperations.V128 result = vectorA;
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
  public SimdOperations.V128 simdEquals() throws Exception {
    SimdOperations.V128 result = vectorA;
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
  public SimdOperations.V128 simdLessThan() throws Exception {
    SimdOperations.V128 result = vectorA;
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
  public SimdOperations.V128 simdIntToFloatConversion() throws Exception {
    SimdOperations.V128 result = vectorA;
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
  public SimdOperations.V128 simdFloatToIntConversion() throws Exception {
    SimdOperations.V128 result = vectorC;
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
  public SimdOperations.V128 simdReplaceLane() throws Exception {
    SimdOperations.V128 result = vectorA;
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
  public SimdOperations.V128 simdSplat() throws Exception {
    SimdOperations.V128 result = vectorA;
    for (int i = 0; i < OPERATION_COUNT; i++) {
      result = simdOps.splatI32(i);
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
  public SimdOperations.V128 simdMixedWorkload() throws Exception {
    SimdOperations.V128 result = vectorA;

    for (int i = 0; i < OPERATION_COUNT / 4; i++) {
      // Perform a series of operations that might occur in real applications
      result = simdOps.add(result, vectorB); // Vector addition
      result = simdOps.multiply(result, vectorB); // Vector multiplication
      result = simdOps.and(result, vectorA); // Bitwise operations
      result = simdOps.replaceLaneI32(result, 0, i); // Lane manipulation
    }
    return result;
  }

  /** Scalar equivalent of the mixed workload for comparison. */
  @Benchmark
  @OperationsPerInvocation(OPERATION_COUNT / 4)
  public int[] scalarMixedWorkload() {
    int[] result = scalarIntsA.clone();

    for (int i = 0; i < OPERATION_COUNT / 4; i++) {
      // Perform equivalent scalar operations
      for (int j = 0; j < VECTOR_SIZE; j++) {
        result[j] += scalarIntsB[j]; // Addition
        result[j] *= scalarIntsB[j]; // Multiplication
        result[j] &= scalarIntsA[j]; // Bitwise AND
      }
      result[0] = i; // Element replacement
    }
    return result;
  }

  // ===== PERFORMANCE ANALYSIS METHODS =====

  /**
   * Utility method to analyze and print performance differences. This is not a benchmark itself but
   * helps with result interpretation.
   */
  public void printPerformanceAnalysis() {
    System.out.println("\n=== SIMD Performance Analysis ===");
    System.out.println("This benchmark suite compares SIMD vectorized operations");
    System.out.println("against their scalar equivalents to demonstrate performance gains.");
    System.out.println("\nKey metrics to look for:");
    System.out.println("• Lower average time indicates better performance");
    System.out.println("• SIMD operations should show significant speedup over scalar");
    System.out.println("• Speedup varies by operation type and platform SIMD support");
    System.out.println("\nExpected results:");
    System.out.println("• Arithmetic operations: 2-4x speedup with SIMD");
    System.out.println("• Logical operations: 3-5x speedup with SIMD");
    System.out.println("• Conversion operations: 2-3x speedup with SIMD");
    System.out.println("• Mixed workloads: 2-4x overall speedup with SIMD");
    System.out.println("\nNote: Actual performance depends on:");
    System.out.println("• CPU architecture and SIMD instruction support");
    System.out.println("• JVM optimizations and warmup behavior");
    System.out.println("• Memory access patterns and cache efficiency");
    System.out.println("=====================================\n");
  }

  /** Main method for running benchmarks manually. */
  public static void main(final String[] args) throws Exception {
    System.out.println("SIMD Performance Benchmark Suite");
    System.out.println("=================================");

    final SimdPerformanceBenchmark benchmark = new SimdPerformanceBenchmark();
    benchmark.setupTrial();
    benchmark.printPerformanceAnalysis();

    System.out.println("To run full benchmarks, use:");
    System.out.println(
        "mvn clean compile exec:java -Dexec.mainClass=\"org.openjdk.jmh.Main\""
            + " -Dexec.args=\"SimdPerformanceBenchmark\"");

    benchmark.teardownTrial();
  }
}
