/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Native function bindings for SIMD vector operations.
 *
 * <p>Provides type-safe wrappers for all WASM SIMD native functions including arithmetic, bitwise,
 * comparison, conversion, lane manipulation, memory operations, and relaxed SIMD operations.
 */
public final class NativeSimdBindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeSimdBindings.class.getName());

  private static volatile NativeSimdBindings instance;
  private static final Object INSTANCE_LOCK = new Object();

  private NativeSimdBindings() {
    super();
    initializeBindings();
    markInitialized();
    LOGGER.fine("Initialized NativeSimdBindings successfully");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeSimdBindings getInstance() {
    NativeSimdBindings result = instance;
    if (result == null) {
      synchronized (INSTANCE_LOCK) {
        result = instance;
        if (result == null) {
          instance = result = new NativeSimdBindings();
        }
      }
    }
    return result;
  }

  private void initializeBindings() {
    addFunctionBinding(
        "wasmtime4j_panama_simd_add",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_subtract",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_multiply",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_divide",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_add_saturated",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_and",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_or",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_xor",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_not",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_sqrt",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_reciprocal",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_rsqrt",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_fma",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_fms",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_extract_lane_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_simd_replace_lane_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_convert_i32_to_f32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_convert_f32_to_i32",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_horizontal_sum_i32",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_horizontal_min_i32",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_horizontal_max_i32",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_relaxed_add",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_shuffle",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_equals",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_less_than",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_greater_than",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_load",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_load_aligned",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_store",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_store_aligned",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_popcount",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_shl_variable",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_shr_variable",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_select",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_simd_blend",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS));
  }

  // ===== Arithmetic Operations =====

  /**
   * SIMD vector addition.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdAdd(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_add", Integer.class, runtimeHandle, vectorA, vectorB, resultData);
  }

  /**
   * SIMD vector subtraction.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdSubtract(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_subtract",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD vector multiplication.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdMultiply(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_multiply",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD vector division.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdDivide(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_divide",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD saturated addition.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdAddSaturated(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_add_saturated",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  // ===== Bitwise Operations =====

  /**
   * SIMD bitwise AND.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdAnd(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_and", Integer.class, runtimeHandle, vectorA, vectorB, resultData);
  }

  /**
   * SIMD bitwise OR.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdOr(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_or", Integer.class, runtimeHandle, vectorA, vectorB, resultData);
  }

  /**
   * SIMD bitwise XOR.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdXor(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_xor", Integer.class, runtimeHandle, vectorA, vectorB, resultData);
  }

  /**
   * SIMD bitwise NOT.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdNot(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_not", Integer.class, runtimeHandle, vector, resultData);
  }

  // ===== Comparison Operations =====

  /**
   * SIMD equality comparison.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdEquals(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_equals",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD less than comparison.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdLessThan(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_less_than",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  /**
   * SIMD greater than comparison.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdGreaterThan(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_greater_than",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  // ===== Math Operations =====

  /**
   * SIMD square root.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdSqrt(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_sqrt", Integer.class, runtimeHandle, vector, resultData);
  }

  /**
   * SIMD reciprocal.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdReciprocal(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_reciprocal", Integer.class, runtimeHandle, vector, resultData);
  }

  /**
   * SIMD reciprocal square root.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdRsqrt(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_rsqrt", Integer.class, runtimeHandle, vector, resultData);
  }

  /**
   * SIMD fused multiply-add.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param vectorC pointer to third vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdFma(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment vectorC,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_fma",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        vectorC,
        resultData);
  }

  /**
   * SIMD fused multiply-subtract.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param vectorC pointer to third vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdFms(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment vectorC,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_fms",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        vectorC,
        resultData);
  }

  // ===== Shuffle and Relaxed Operations =====

  /**
   * SIMD shuffle.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param indices pointer to indices bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdShuffle(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment indices,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_shuffle",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        indices,
        resultData);
  }

  /**
   * SIMD relaxed addition.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA pointer to first vector bytes
   * @param vectorB pointer to second vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdRelaxedAdd(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_relaxed_add",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        resultData);
  }

  // ===== Lane Operations =====

  /**
   * SIMD extract lane i32.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @param laneIndex lane index (0-3)
   * @return extracted lane value
   */
  public int simdExtractLaneI32(
      final long runtimeHandle, final MemorySegment vector, final int laneIndex) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_extract_lane_i32", Integer.class, runtimeHandle, vector, laneIndex);
  }

  /**
   * SIMD replace lane i32.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @param laneIndex lane index (0-3)
   * @param value value to insert
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdReplaceLaneI32(
      final long runtimeHandle,
      final MemorySegment vector,
      final int laneIndex,
      final int value,
      final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_replace_lane_i32",
        Integer.class,
        runtimeHandle,
        vector,
        laneIndex,
        value,
        resultData);
  }

  // ===== Conversion Operations =====

  /**
   * SIMD convert i32 to f32.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdConvertI32ToF32(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_convert_i32_to_f32",
        Integer.class,
        runtimeHandle,
        vector,
        resultData);
  }

  /**
   * SIMD convert f32 to i32.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @param resultData pointer to result buffer
   * @return status code (0 for success)
   */
  public int simdConvertF32ToI32(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment resultData) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_convert_f32_to_i32",
        Integer.class,
        runtimeHandle,
        vector,
        resultData);
  }

  // ===== Reduction Operations =====

  /**
   * SIMD horizontal sum reduction.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @return sum of all i32 lanes
   */
  public int simdHorizontalSumI32(final long runtimeHandle, final MemorySegment vector) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_horizontal_sum_i32", Integer.class, runtimeHandle, vector);
  }

  /**
   * SIMD horizontal min reduction.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @return minimum of all i32 lanes
   */
  public int simdHorizontalMinI32(final long runtimeHandle, final MemorySegment vector) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_horizontal_min_i32", Integer.class, runtimeHandle, vector);
  }

  /**
   * SIMD horizontal max reduction.
   *
   * @param runtimeHandle the runtime handle
   * @param vector pointer to vector bytes
   * @return maximum of all i32 lanes
   */
  public int simdHorizontalMaxI32(final long runtimeHandle, final MemorySegment vector) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_horizontal_max_i32", Integer.class, runtimeHandle, vector);
  }

  // ===== Memory Operations =====

  /**
   * SIMD load from memory.
   *
   * @param runtimeHandle the runtime handle
   * @param memoryHandle the memory handle
   * @param offset the offset in memory
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdLoad(
      final long runtimeHandle,
      final MemorySegment memoryHandle,
      final int offset,
      final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_load", Integer.class, runtimeHandle, memoryHandle, offset, result);
  }

  /**
   * SIMD aligned load from memory.
   *
   * @param runtimeHandle the runtime handle
   * @param memoryHandle the memory handle
   * @param offset the offset in memory
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdLoadAligned(
      final long runtimeHandle,
      final MemorySegment memoryHandle,
      final int offset,
      final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_load_aligned",
        Integer.class,
        runtimeHandle,
        memoryHandle,
        offset,
        result);
  }

  /**
   * SIMD store to memory.
   *
   * @param runtimeHandle the runtime handle
   * @param memoryHandle the memory handle
   * @param offset the offset in memory
   * @param vector the vector data
   * @return status code (0 for success)
   */
  public int simdStore(
      final long runtimeHandle,
      final MemorySegment memoryHandle,
      final int offset,
      final MemorySegment vector) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_store", Integer.class, runtimeHandle, memoryHandle, offset, vector);
  }

  /**
   * SIMD aligned store to memory.
   *
   * @param runtimeHandle the runtime handle
   * @param memoryHandle the memory handle
   * @param offset the offset in memory
   * @param vector the vector data
   * @return status code (0 for success)
   */
  public int simdStoreAligned(
      final long runtimeHandle,
      final MemorySegment memoryHandle,
      final int offset,
      final MemorySegment vector) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_store_aligned",
        Integer.class,
        runtimeHandle,
        memoryHandle,
        offset,
        vector);
  }

  // ===== Bit Manipulation Operations =====

  /**
   * SIMD popcount operation.
   *
   * @param runtimeHandle the runtime handle
   * @param vector the vector data
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdPopcount(
      final long runtimeHandle, final MemorySegment vector, final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_popcount", Integer.class, runtimeHandle, vector, result);
  }

  /**
   * SIMD variable shift left.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA the first vector
   * @param vectorB the shift amounts vector
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdShlVariable(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_shl_variable",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        result);
  }

  /**
   * SIMD variable shift right.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA the first vector
   * @param vectorB the shift amounts vector
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdShrVariable(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_shr_variable",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        result);
  }

  /**
   * SIMD select operation.
   *
   * @param runtimeHandle the runtime handle
   * @param mask the mask vector
   * @param vectorA the first vector
   * @param vectorB the second vector
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdSelect(
      final long runtimeHandle,
      final MemorySegment mask,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_select",
        Integer.class,
        runtimeHandle,
        mask,
        vectorA,
        vectorB,
        result);
  }

  /**
   * SIMD blend operation.
   *
   * @param runtimeHandle the runtime handle
   * @param vectorA the first vector
   * @param vectorB the second vector
   * @param mask the blend mask
   * @param result the result buffer
   * @return status code (0 for success)
   */
  public int simdBlend(
      final long runtimeHandle,
      final MemorySegment vectorA,
      final MemorySegment vectorB,
      final int mask,
      final MemorySegment result) {
    return callNativeFunction(
        "wasmtime4j_panama_simd_blend",
        Integer.class,
        runtimeHandle,
        vectorA,
        vectorB,
        mask,
        result);
  }
}
