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
 *
 * <ul>
 *   <li>Advanced arithmetic operations (FMA, reciprocal, sqrt)
 *   <li>Advanced logical operations (popcount, variable shifts)
 *   <li>Vector reduction operations
 *   <li>Selection and blending operations
 *   <li>Relaxed SIMD operations
 * </ul>
 *
 * <p>Benchmarks compare performance across different runtime types to identify optimal
 * configurations.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class AdvancedSimdBenchmark {

  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  private WasmRuntime runtime;
  private SimdOperations simdOps;

  // Test vectors
  private SimdVector vectorA;
  private SimdVector vectorB;
  private SimdVector vectorC;
  private SimdVector maskVector;
  private SimdVector intVectorA;
  private SimdVector intVectorB;

  @Setup(Level.Trial)
  public void setupRuntime() throws WasmException {
    final RuntimeType type = RuntimeType.valueOf(runtimeTypeName);
    runtime = WasmRuntimeFactory.create(type);
    simdOps = runtime.getSimdOperations();
  }

  @Setup(Level.Iteration)
  public void setupVectors() {
    // Initialize float test vectors
    vectorA = V128.fromFloats(1.5f, 2.5f, 3.5f, 4.5f).toSimdVector(SimdLane.F32X4);
    vectorB = V128.fromFloats(0.5f, 1.0f, 1.5f, 2.0f).toSimdVector(SimdLane.F32X4);
    vectorC = V128.fromFloats(0.1f, 0.2f, 0.3f, 0.4f).toSimdVector(SimdLane.F32X4);

    // Initialize integer test vectors
    intVectorA = V128.fromInts(100, 200, 300, 400).toSimdVector(SimdLane.I32X4);
    intVectorB = V128.fromInts(1, 2, 3, 4).toSimdVector(SimdLane.I32X4);
    maskVector = V128.fromInts(-1, 0, -1, 0).toSimdVector(SimdLane.I32X4);
  }

  @TearDown(Level.Trial)
  public void teardownRuntime() throws Exception {
    if (runtime != null) {
      runtime.close();
    }
  }

  // ===== BASIC SIMD OPERATIONS (BASELINE) =====

  @Benchmark
  public void basicAdd(final Blackhole bh) throws WasmException {
    final SimdVector result = simdOps.add(vectorA, vectorB);
    bh.consume(result);
  }

  @Benchmark
  public void basicMultiply(final Blackhole bh) throws WasmException {
    final SimdVector result = simdOps.multiply(vectorA, vectorB);
    bh.consume(result);
  }

  @Benchmark
  public void basicMultiplyAddSeparate(final Blackhole bh) throws WasmException {
    final SimdVector temp = simdOps.multiply(vectorA, vectorB);
    final SimdVector result = simdOps.add(temp, vectorC);
    bh.consume(result);
  }

  // ===== ADVANCED ARITHMETIC OPERATIONS =====

  @Benchmark
  public void fusedMultiplyAdd(final Blackhole bh) throws WasmException {
    final SimdVector result = simdOps.fma(vectorA, vectorB, vectorC);
    bh.consume(result);
  }

  @Benchmark
  public void fusedMultiplySubtract(final Blackhole bh) throws WasmException {
    final SimdVector result = simdOps.fms(vectorA, vectorB, vectorC);
    bh.consume(result);
  }

  @Benchmark
  public void vectorReciprocal(final Blackhole bh) throws WasmException {
    final SimdVector result = simdOps.reciprocal(vectorA);
    bh.consume(result);
  }

  @Benchmark
  public void vectorSquareRoot(final Blackhole bh) throws WasmException {
    final SimdVector result = simdOps.sqrt(vectorA);
    bh.consume(result);
  }

  @Benchmark
  public void reciprocalSquareRoot(final Blackhole bh) throws WasmException {
    final SimdVector result = simdOps.rsqrt(vectorA);
    bh.consume(result);
  }

  // ===== ADVANCED LOGICAL OPERATIONS =====

  @Benchmark
  public void populationCount(final Blackhole bh) throws WasmException {
    final SimdVector intVector =
        V128.fromInts(0xFF00FF00, 0x0F0F0F0F, 0xAAAAAAAA, 0x55555555).toSimdVector(SimdLane.I32X4);
    final SimdVector result = simdOps.popcount(intVector);
    bh.consume(result);
  }

  @Benchmark
  public void variableShiftLeft(final Blackhole bh) throws WasmException {
    final SimdVector data = V128.fromInts(1, 2, 4, 8).toSimdVector(SimdLane.I32X4);
    final SimdVector shifts = V128.fromInts(1, 2, 3, 4).toSimdVector(SimdLane.I32X4);
    final SimdVector result = simdOps.shlVariable(data, shifts);
    bh.consume(result);
  }

  @Benchmark
  public void variableShiftRight(final Blackhole bh) throws WasmException {
    final SimdVector data = V128.fromInts(128, 64, 32, 16).toSimdVector(SimdLane.I32X4);
    final SimdVector shifts = V128.fromInts(1, 2, 3, 4).toSimdVector(SimdLane.I32X4);
    final SimdVector result = simdOps.shrVariable(data, shifts);
    bh.consume(result);
  }

  // ===== VECTOR REDUCTION OPERATIONS =====

  @Benchmark
  public void horizontalSum(final Blackhole bh) throws WasmException {
    final float result = simdOps.horizontalSum(vectorA);
    bh.consume(result);
  }

  @Benchmark
  public void horizontalMin(final Blackhole bh) throws WasmException {
    final float result = simdOps.horizontalMin(vectorA);
    bh.consume(result);
  }

  @Benchmark
  public void horizontalMax(final Blackhole bh) throws WasmException {
    final float result = simdOps.horizontalMax(vectorA);
    bh.consume(result);
  }

  // ===== SELECTION AND BLENDING OPERATIONS =====

  @Benchmark
  public void vectorSelect(final Blackhole bh) throws WasmException {
    // Convert float vectors to same type as mask for selection
    final SimdVector fMask =
        V128.fromFloats(Float.intBitsToFloat(-1), 0.0f, Float.intBitsToFloat(-1), 0.0f)
            .toSimdVector(SimdLane.F32X4);
    final SimdVector result = simdOps.select(fMask, vectorA, vectorB);
    bh.consume(result);
  }

  @Benchmark
  public void vectorBlend(final Blackhole bh) throws WasmException {
    final SimdVector result = simdOps.blend(vectorA, vectorB, 0xAA);
    bh.consume(result);
  }

  // ===== RELAXED SIMD OPERATIONS =====

  @Benchmark
  public void relaxedAdd(final Blackhole bh) throws WasmException {
    final SimdVector result = simdOps.relaxedAdd(vectorA, vectorB);
    bh.consume(result);
  }

  // ===== COMPOSITE OPERATIONS =====

  @Benchmark
  public void complexMathOperation(final Blackhole bh) throws WasmException {
    // Simulate a complex mathematical operation using multiple advanced SIMD ops
    // sqrt((a * b + c) / (a + b))
    final SimdVector fmaResult = simdOps.fma(vectorA, vectorB, vectorC);
    final SimdVector sum = simdOps.add(vectorA, vectorB);
    final SimdVector division = simdOps.divide(fmaResult, sum);
    final SimdVector result = simdOps.sqrt(division);
    bh.consume(result);
  }

  @Benchmark
  public void vectorProcessingPipeline(final Blackhole bh) throws WasmException {
    // Simulate a typical vector processing pipeline
    SimdVector result = simdOps.multiply(vectorA, vectorB);
    result = simdOps.add(result, vectorC);
    result = simdOps.sqrt(result);

    final float sum = simdOps.horizontalSum(result);
    bh.consume(sum);
  }

  // ===== PERFORMANCE COMPARISON OPERATIONS =====

  @Benchmark
  public void scalarEquivalentFma(final Blackhole bh) {
    // Scalar equivalent of FMA for comparison
    final V128 aV128 = V128.fromSimdVector(vectorA);
    final V128 bV128 = V128.fromSimdVector(vectorB);
    final V128 cV128 = V128.fromSimdVector(vectorC);

    final float[] a = aV128.getAsFloats();
    final float[] b = bV128.getAsFloats();
    final float[] c = cV128.getAsFloats();
    final float[] result = new float[4];

    for (int i = 0; i < 4; i++) {
      result[i] = Math.fma(a[i], b[i], c[i]);
    }

    bh.consume(result);
  }

  @Benchmark
  public void scalarEquivalentSqrt(final Blackhole bh) {
    // Scalar equivalent of sqrt for comparison
    final V128 aV128 = V128.fromSimdVector(vectorA);
    final float[] a = aV128.getAsFloats();
    final float[] result = new float[4];

    for (int i = 0; i < 4; i++) {
      result[i] = (float) Math.sqrt(a[i]);
    }

    bh.consume(result);
  }

  @Benchmark
  public void scalarEquivalentHorizontalSum(final Blackhole bh) {
    // Scalar equivalent of horizontal sum
    final V128 aV128 = V128.fromSimdVector(vectorA);
    final float[] a = aV128.getAsFloats();
    float sum = 0.0f;

    for (float value : a) {
      sum += value;
    }

    bh.consume(sum);
  }

  // ===== MEMORY-INTENSIVE OPERATIONS =====

  @Benchmark
  public void vectorCreationOverhead(final Blackhole bh) throws WasmException {
    // Measure the overhead of creating and manipulating vectors
    final SimdVector vec1 = V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f).toSimdVector(SimdLane.F32X4);
    final SimdVector vec2 = V128.fromFloats(0.5f, 1.5f, 2.5f, 3.5f).toSimdVector(SimdLane.F32X4);
    final SimdVector result = simdOps.add(vec1, vec2);
    bh.consume(result);
  }

  @Benchmark
  public void vectorDataExtraction(final Blackhole bh) throws WasmException {
    // Measure the cost of extracting data from vectors
    final SimdVector result = simdOps.multiply(vectorA, vectorB);
    final V128 v128 = V128.fromSimdVector(result);
    final float[] floats = v128.getAsFloats();
    final int[] ints = v128.getAsInts();
    bh.consume(floats);
    bh.consume(ints);
  }

  // ===== BRANCHING AND CONDITIONAL OPERATIONS =====

  @Benchmark
  public void conditionalSimdOperations(final Blackhole bh) throws WasmException {
    // Simulate conditional SIMD operations based on runtime characteristics
    SimdVector result;

    if (simdOps.isSimdSupported()) {
      // Use advanced operations when SIMD is supported
      result = simdOps.fma(vectorA, vectorB, vectorC);
    } else {
      // Fall back to basic operations
      final SimdVector temp = simdOps.multiply(vectorA, vectorB);
      result = simdOps.add(temp, vectorC);
    }

    bh.consume(result);
  }
}
