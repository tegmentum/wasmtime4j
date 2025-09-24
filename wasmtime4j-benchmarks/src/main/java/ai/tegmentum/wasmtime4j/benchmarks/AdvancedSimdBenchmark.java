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

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.SimdOperations;
import ai.tegmentum.wasmtime4j.SimdOperations.V128;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmtimeException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
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
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for advanced SIMD operations.
 *
 * <p>This benchmark suite evaluates the performance of:
 * - Advanced arithmetic operations (FMA, reciprocal, sqrt)
 * - Advanced logical operations (popcount, variable shifts)
 * - Vector reduction operations
 * - Selection and blending operations
 * - Platform-specific optimizations
 *
 * <p>Benchmarks compare performance across different vector widths and
 * platform optimizations to identify optimal configurations.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class AdvancedSimdBenchmark {

  @Param({"128", "256", "512"})
  private int vectorWidth;

  @Param({"true", "false"})
  private boolean platformOptimizations;

  @Param({"true", "false"})
  private boolean relaxedOperations;

  private WasmRuntime runtime;
  private SimdOperations simdOps;

  // Test vectors
  private V128 vectorA;
  private V128 vectorB;
  private V128 vectorC;
  private V128 maskVector;

  @Setup(Level.Trial)
  public void setupRuntime() throws WasmtimeException {
    final EngineConfig engineConfig = EngineConfig.builder()
        .enableSimd(true)
        .enableRelaxedSimd(relaxedOperations)
        .optimizationLevel(EngineConfig.OptimizationLevel.SPEED)
        .build();

    runtime = WasmRuntimeFactory.createRuntime(engineConfig);

    final SimdOperations.SimdConfig simdConfig = SimdOperations.SimdConfig.builder()
        .enablePlatformOptimizations(platformOptimizations)
        .enableRelaxedOperations(relaxedOperations)
        .maxVectorWidth(vectorWidth)
        .build();

    simdOps = new SimdOperations(simdConfig, runtime);
  }

  @Setup(Level.Iteration)
  public void setupVectors() {
    // Initialize test vectors with meaningful data
    vectorA = V128.fromFloats(1.5f, 2.5f, 3.5f, 4.5f);
    vectorB = V128.fromFloats(0.5f, 1.0f, 1.5f, 2.0f);
    vectorC = V128.fromFloats(0.1f, 0.2f, 0.3f, 0.4f);
    maskVector = V128.fromInts(-1, 0, -1, 0);
  }

  @TearDown(Level.Trial)
  public void teardownRuntime() throws Exception {
    if (runtime != null) {
      runtime.close();
    }
  }

  // ===== BASIC SIMD OPERATIONS (BASELINE) =====

  @Benchmark
  public void basicAdd(final Blackhole bh) throws WasmtimeException {
    final V128 result = simdOps.add(vectorA, vectorB);
    bh.consume(result);
  }

  @Benchmark
  public void basicMultiply(final Blackhole bh) throws WasmtimeException {
    final V128 result = simdOps.multiply(vectorA, vectorB);
    bh.consume(result);
  }

  @Benchmark
  public void basicMultiplyAddSeparate(final Blackhole bh) throws WasmtimeException {
    final V128 temp = simdOps.multiply(vectorA, vectorB);
    final V128 result = simdOps.add(temp, vectorC);
    bh.consume(result);
  }

  // ===== ADVANCED ARITHMETIC OPERATIONS =====

  @Benchmark
  public void fusedMultiplyAdd(final Blackhole bh) throws WasmtimeException {
    final V128 result = simdOps.fma(vectorA, vectorB, vectorC);
    bh.consume(result);
  }

  @Benchmark
  public void fusedMultiplySubtract(final Blackhole bh) throws WasmtimeException {
    final V128 result = simdOps.fms(vectorA, vectorB, vectorC);
    bh.consume(result);
  }

  @Benchmark
  public void vectorReciprocal(final Blackhole bh) throws WasmtimeException {
    final V128 result = simdOps.reciprocal(vectorA);
    bh.consume(result);
  }

  @Benchmark
  public void vectorSquareRoot(final Blackhole bh) throws WasmtimeException {
    final V128 result = simdOps.sqrt(vectorA);
    bh.consume(result);
  }

  @Benchmark
  public void reciprocalSquareRoot(final Blackhole bh) throws WasmtimeException {
    final V128 result = simdOps.rsqrt(vectorA);
    bh.consume(result);
  }

  // ===== ADVANCED LOGICAL OPERATIONS =====

  @Benchmark
  public void populationCount(final Blackhole bh) throws WasmtimeException {
    final V128 intVector = V128.fromInts(0xFF00FF00, 0x0F0F0F0F, 0xAAAAAAAA, 0x55555555);
    final V128 result = simdOps.popcount(intVector);
    bh.consume(result);
  }

  @Benchmark
  public void variableShiftLeft(final Blackhole bh) throws WasmtimeException {
    final V128 data = V128.fromInts(1, 2, 4, 8);
    final V128 shifts = V128.fromInts(1, 2, 3, 4);
    final V128 result = simdOps.shlVariable(data, shifts);
    bh.consume(result);
  }

  @Benchmark
  public void variableShiftRight(final Blackhole bh) throws WasmtimeException {
    final V128 data = V128.fromInts(128, 64, 32, 16);
    final V128 shifts = V128.fromInts(1, 2, 3, 4);
    final V128 result = simdOps.shrVariable(data, shifts);
    bh.consume(result);
  }

  // ===== VECTOR REDUCTION OPERATIONS =====

  @Benchmark
  public void horizontalSum(final Blackhole bh) throws WasmtimeException {
    final float result = simdOps.horizontalSum(vectorA);
    bh.consume(result);
  }

  @Benchmark
  public void horizontalMin(final Blackhole bh) throws WasmtimeException {
    final float result = simdOps.horizontalMin(vectorA);
    bh.consume(result);
  }

  @Benchmark
  public void horizontalMax(final Blackhole bh) throws WasmtimeException {
    final float result = simdOps.horizontalMax(vectorA);
    bh.consume(result);
  }

  // ===== SELECTION AND BLENDING OPERATIONS =====

  @Benchmark
  public void vectorSelect(final Blackhole bh) throws WasmtimeException {
    final V128 result = simdOps.select(maskVector, vectorA, vectorB);
    bh.consume(result);
  }

  @Benchmark
  public void vectorBlend(final Blackhole bh) throws WasmtimeException {
    final V128 result = simdOps.blend(vectorA, vectorB, 0xAA); // Alternating pattern
    bh.consume(result);
  }

  // ===== RELAXED SIMD OPERATIONS =====

  @Benchmark
  public void relaxedAdd(final Blackhole bh) throws WasmtimeException {
    final V128 result = simdOps.relaxedAdd(vectorA, vectorB);
    bh.consume(result);
  }

  // ===== COMPOSITE OPERATIONS =====

  @Benchmark
  public void complexMathOperation(final Blackhole bh) throws WasmtimeException {
    // Simulate a complex mathematical operation using multiple advanced SIMD ops
    // Example: sqrt((a * b + c) / (a + b))
    final V128 fmaResult = simdOps.fma(vectorA, vectorB, vectorC);
    final V128 sum = simdOps.add(vectorA, vectorB);
    final V128 division = simdOps.divide(fmaResult, sum);
    final V128 result = simdOps.sqrt(division);
    bh.consume(result);
  }

  @Benchmark
  public void vectorProcessingPipeline(final Blackhole bh) throws WasmtimeException {
    // Simulate a typical vector processing pipeline
    V128 result = simdOps.multiply(vectorA, vectorB);
    result = simdOps.add(result, vectorC);
    result = simdOps.sqrt(result);

    final float sum = simdOps.horizontalSum(result);
    bh.consume(sum);
  }

  // ===== PERFORMANCE COMPARISON OPERATIONS =====

  @Benchmark
  public void scalarEquivalentFma(final Blackhole bh) {
    // Scalar equivalent of FMA for comparison
    final float[] a = vectorA.getAsFloats();
    final float[] b = vectorB.getAsFloats();
    final float[] c = vectorC.getAsFloats();
    final float[] result = new float[4];

    for (int i = 0; i < 4; i++) {
      result[i] = Math.fma(a[i], b[i], c[i]);
    }

    bh.consume(result);
  }

  @Benchmark
  public void scalarEquivalentSqrt(final Blackhole bh) {
    // Scalar equivalent of sqrt for comparison
    final float[] a = vectorA.getAsFloats();
    final float[] result = new float[4];

    for (int i = 0; i < 4; i++) {
      result[i] = (float) Math.sqrt(a[i]);
    }

    bh.consume(result);
  }

  @Benchmark
  public void scalarEquivalentHorizontalSum(final Blackhole bh) {
    // Scalar equivalent of horizontal sum
    final float[] a = vectorA.getAsFloats();
    float sum = 0.0f;

    for (float value : a) {
      sum += value;
    }

    bh.consume(sum);
  }

  // ===== MEMORY-INTENSIVE OPERATIONS =====

  @Benchmark
  public void vectorCreationOverhead(final Blackhole bh) {
    // Measure the overhead of creating and manipulating vectors
    final V128 vec1 = V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
    final V128 vec2 = V128.fromFloats(0.5f, 1.5f, 2.5f, 3.5f);
    final V128 result = simdOps.add(vec1, vec2);
    bh.consume(result);
  }

  @Benchmark
  public void vectorDataExtraction(final Blackhole bh) {
    // Measure the cost of extracting data from vectors
    final V128 result = simdOps.multiply(vectorA, vectorB);
    final float[] floats = result.getAsFloats();
    final int[] ints = result.getAsInts();
    bh.consume(floats);
    bh.consume(ints);
  }

  // ===== BRANCHING AND CONDITIONAL OPERATIONS =====

  @Benchmark
  public void conditionalSimdOperations(final Blackhole bh) throws WasmtimeException {
    // Simulate conditional SIMD operations based on configuration
    V128 result;

    if (platformOptimizations) {
      // Use advanced operations when optimizations are enabled
      result = simdOps.fma(vectorA, vectorB, vectorC);
    } else {
      // Fall back to basic operations
      final V128 temp = simdOps.multiply(vectorA, vectorB);
      result = simdOps.add(temp, vectorC);
    }

    bh.consume(result);
  }

  // ===== UTILITY METHODS =====

  public static void main(final String[] args) throws Exception {
    // Simple main method to run a subset of benchmarks for quick testing
    final AdvancedSimdBenchmark benchmark = new AdvancedSimdBenchmark();
    benchmark.vectorWidth = 128;
    benchmark.platformOptimizations = true;
    benchmark.relaxedOperations = false;

    try {
      benchmark.setupRuntime();
      benchmark.setupVectors();

      // Run a few representative benchmarks
      final Blackhole bh = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");

      System.out.println("Running Advanced SIMD Benchmarks...");

      long start = System.nanoTime();
      for (int i = 0; i < 10000; i++) {
        benchmark.fusedMultiplyAdd(bh);
      }
      long end = System.nanoTime();
      System.out.println("FMA: " + ((end - start) / 10000) + " ns/op");

      start = System.nanoTime();
      for (int i = 0; i < 10000; i++) {
        benchmark.basicMultiplyAddSeparate(bh);
      }
      end = System.nanoTime();
      System.out.println("Separate Mul+Add: " + ((end - start) / 10000) + " ns/op");

      start = System.nanoTime();
      for (int i = 0; i < 10000; i++) {
        benchmark.vectorSquareRoot(bh);
      }
      end = System.nanoTime();
      System.out.println("Vector Sqrt: " + ((end - start) / 10000) + " ns/op");

      start = System.nanoTime();
      for (int i = 0; i < 10000; i++) {
        benchmark.scalarEquivalentSqrt(bh);
      }
      end = System.nanoTime();
      System.out.println("Scalar Sqrt: " + ((end - start) / 10000) + " ns/op");

    } finally {
      benchmark.teardownRuntime();
    }
  }
}