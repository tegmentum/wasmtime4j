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

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.simd.SimdLane;
import ai.tegmentum.wasmtime4j.simd.SimdOperations;
import ai.tegmentum.wasmtime4j.simd.SimdVector;
import java.util.Objects;

/**
 * JNI implementation of SIMD operations.
 *
 * @since 1.0.0
 */
public final class JniSimdOperations implements SimdOperations {
  private final long runtimeHandle;

  /**
   * Creates a JNI SIMD operations instance.
   *
   * @param runtimeHandle the native runtime handle
   */
  public JniSimdOperations(final long runtimeHandle) {
    this.runtimeHandle = runtimeHandle;
  }

  @Override
  public SimdVector add(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result =
        JniWasmRuntime.nativeSimdAdd(runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector subtract(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result =
        JniWasmRuntime.nativeSimdSubtract(runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector multiply(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result =
        JniWasmRuntime.nativeSimdMultiply(runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector divide(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result =
        JniWasmRuntime.nativeSimdDivide(runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector addSaturated(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    if (!a.getLane().isIntegerType()) {
      throw new IllegalArgumentException("Saturated operations require integer lane types");
    }
    final byte[] result =
        JniWasmRuntime.nativeSimdAddSaturated(
            runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector and(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result =
        JniWasmRuntime.nativeSimdAnd(runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector or(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result =
        JniWasmRuntime.nativeSimdOr(runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector xor(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result =
        JniWasmRuntime.nativeSimdXor(runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector not(final SimdVector a) throws WasmException {
    Objects.requireNonNull(a, "vector cannot be null");
    final byte[] result = JniWasmRuntime.nativeSimdNot(runtimeHandle, a.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector equals(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result =
        JniWasmRuntime.nativeSimdEquals(runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector lessThan(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result =
        JniWasmRuntime.nativeSimdLessThan(runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector greaterThan(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result =
        JniWasmRuntime.nativeSimdGreaterThan(
            runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector load(final WasmMemory memory, final int offset, final SimdLane lane)
      throws WasmException {
    Objects.requireNonNull(memory, "memory cannot be null");
    Objects.requireNonNull(lane, "lane cannot be null");
    final long memoryHandle = extractMemoryHandle(memory);
    final byte[] result = JniWasmRuntime.nativeSimdLoad(runtimeHandle, memoryHandle, offset);
    return new SimdVector(lane, result);
  }

  @Override
  public SimdVector loadAligned(final WasmMemory memory, final int offset, final SimdLane lane)
      throws WasmException {
    Objects.requireNonNull(memory, "memory cannot be null");
    Objects.requireNonNull(lane, "lane cannot be null");
    final long memoryHandle = extractMemoryHandle(memory);
    final byte[] result =
        JniWasmRuntime.nativeSimdLoadAligned(runtimeHandle, memoryHandle, offset, 16);
    return new SimdVector(lane, result);
  }

  @Override
  public void store(final WasmMemory memory, final int offset, final SimdVector vector)
      throws WasmException {
    Objects.requireNonNull(memory, "memory cannot be null");
    Objects.requireNonNull(vector, "vector cannot be null");
    final long memoryHandle = extractMemoryHandle(memory);
    final boolean success =
        JniWasmRuntime.nativeSimdStore(
            runtimeHandle, memoryHandle, offset, vector.getDataInternal());
    if (!success) {
      throw new WasmException("Failed to store SIMD vector to memory at offset " + offset);
    }
  }

  @Override
  public void storeAligned(final WasmMemory memory, final int offset, final SimdVector vector)
      throws WasmException {
    Objects.requireNonNull(memory, "memory cannot be null");
    Objects.requireNonNull(vector, "vector cannot be null");
    final long memoryHandle = extractMemoryHandle(memory);
    final boolean success =
        JniWasmRuntime.nativeSimdStoreAligned(
            runtimeHandle, memoryHandle, offset, vector.getDataInternal(), 16);
    if (!success) {
      throw new WasmException("Failed to store SIMD vector to aligned memory at offset " + offset);
    }
  }

  @Override
  public int extractLaneI32(final SimdVector vector, final int laneIndex) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (!vector.getLane().isIntegerType()) {
      throw new IllegalArgumentException("Vector must be integer type for extractLaneI32");
    }
    if (laneIndex < 0 || laneIndex >= vector.getLane().getLaneCount()) {
      throw new IllegalArgumentException(
          "Lane index " + laneIndex + " out of bounds for " + vector.getLane());
    }
    return JniWasmRuntime.nativeSimdExtractLaneI32(
        runtimeHandle, vector.getDataInternal(), laneIndex);
  }

  @Override
  public SimdVector replaceLaneI32(final SimdVector vector, final int laneIndex, final int value)
      throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (!vector.getLane().isIntegerType()) {
      throw new IllegalArgumentException("Vector must be integer type for replaceLaneI32");
    }
    if (laneIndex < 0 || laneIndex >= vector.getLane().getLaneCount()) {
      throw new IllegalArgumentException(
          "Lane index " + laneIndex + " out of bounds for " + vector.getLane());
    }
    final byte[] result =
        JniWasmRuntime.nativeSimdReplaceLaneI32(
            runtimeHandle, vector.getDataInternal(), laneIndex, value);
    return new SimdVector(vector.getLane(), result);
  }

  @Override
  public SimdVector convertI32ToF32(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (vector.getLane() != SimdLane.I32X4) {
      throw new IllegalArgumentException("Vector must be I32X4 for convertI32ToF32");
    }
    final byte[] result =
        JniWasmRuntime.nativeSimdConvertI32ToF32(runtimeHandle, vector.getDataInternal());
    return new SimdVector(SimdLane.F32X4, result);
  }

  @Override
  public SimdVector convertF32ToI32(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (vector.getLane() != SimdLane.F32X4) {
      throw new IllegalArgumentException("Vector must be F32X4 for convertF32ToI32");
    }
    final byte[] result =
        JniWasmRuntime.nativeSimdConvertF32ToI32(runtimeHandle, vector.getDataInternal());
    return new SimdVector(SimdLane.I32X4, result);
  }

  @Override
  public SimdVector shuffle(final SimdVector a, final SimdVector b, final int[] indices)
      throws WasmException {
    validateSameLane(a, b);
    Objects.requireNonNull(indices, "indices cannot be null");
    if (indices.length != 16) {
      throw new IllegalArgumentException(
          "Shuffle indices must be exactly 16 elements, got " + indices.length);
    }
    // Convert int[] to byte[] for native method
    final byte[] byteIndices = new byte[16];
    for (int i = 0; i < 16; i++) {
      byteIndices[i] = (byte) indices[i];
    }
    final byte[] result =
        JniWasmRuntime.nativeSimdShuffle(
            runtimeHandle, a.getDataInternal(), b.getDataInternal(), byteIndices);
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector fma(final SimdVector a, final SimdVector b, final SimdVector c)
      throws WasmException {
    validateSameLane(a, b);
    validateSameLane(a, c);
    if (!a.getLane().isFloatType()) {
      throw new IllegalArgumentException("FMA requires float lane types");
    }
    final byte[] result =
        JniWasmRuntime.nativeSimdFma(
            runtimeHandle, a.getDataInternal(), b.getDataInternal(), c.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector fms(final SimdVector a, final SimdVector b, final SimdVector c)
      throws WasmException {
    validateSameLane(a, b);
    validateSameLane(a, c);
    if (!a.getLane().isFloatType()) {
      throw new IllegalArgumentException("FMS requires float lane types");
    }
    final byte[] result =
        JniWasmRuntime.nativeSimdFms(
            runtimeHandle, a.getDataInternal(), b.getDataInternal(), c.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector reciprocal(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (!vector.getLane().isFloatType()) {
      throw new IllegalArgumentException("Reciprocal requires float lane type");
    }
    final byte[] result =
        JniWasmRuntime.nativeSimdReciprocal(runtimeHandle, vector.getDataInternal());
    return new SimdVector(vector.getLane(), result);
  }

  @Override
  public SimdVector sqrt(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (!vector.getLane().isFloatType()) {
      throw new IllegalArgumentException("Sqrt requires float lane type");
    }
    final byte[] result = JniWasmRuntime.nativeSimdSqrt(runtimeHandle, vector.getDataInternal());
    return new SimdVector(vector.getLane(), result);
  }

  @Override
  public SimdVector rsqrt(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (!vector.getLane().isFloatType()) {
      throw new IllegalArgumentException("Rsqrt requires float lane type");
    }
    final byte[] result = JniWasmRuntime.nativeSimdRsqrt(runtimeHandle, vector.getDataInternal());
    return new SimdVector(vector.getLane(), result);
  }

  @Override
  public SimdVector popcount(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (!vector.getLane().isIntegerType()) {
      throw new IllegalArgumentException("Popcount requires integer lane type");
    }
    final byte[] result =
        JniWasmRuntime.nativeSimdPopcount(runtimeHandle, vector.getDataInternal());
    return new SimdVector(vector.getLane(), result);
  }

  @Override
  public SimdVector shlVariable(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    if (!a.getLane().isIntegerType()) {
      throw new IllegalArgumentException("Variable shift requires integer lane types");
    }
    final byte[] result =
        JniWasmRuntime.nativeSimdShlVariable(
            runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector shrVariable(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    if (!a.getLane().isIntegerType()) {
      throw new IllegalArgumentException("Variable shift requires integer lane types");
    }
    final byte[] result =
        JniWasmRuntime.nativeSimdShrVariable(
            runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public float horizontalSum(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (!vector.getLane().isFloatType()) {
      throw new IllegalArgumentException("Horizontal sum requires float lane type");
    }
    return JniWasmRuntime.nativeSimdHorizontalSum(runtimeHandle, vector.getDataInternal());
  }

  @Override
  public float horizontalMin(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (!vector.getLane().isFloatType()) {
      throw new IllegalArgumentException("Horizontal min requires float lane type");
    }
    return JniWasmRuntime.nativeSimdHorizontalMin(runtimeHandle, vector.getDataInternal());
  }

  @Override
  public float horizontalMax(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (!vector.getLane().isFloatType()) {
      throw new IllegalArgumentException("Horizontal max requires float lane type");
    }
    return JniWasmRuntime.nativeSimdHorizontalMax(runtimeHandle, vector.getDataInternal());
  }

  @Override
  public SimdVector select(final SimdVector mask, final SimdVector a, final SimdVector b)
      throws WasmException {
    validateSameLane(a, b);
    validateSameLane(mask, a);
    final byte[] result =
        JniWasmRuntime.nativeSimdSelect(
            runtimeHandle, mask.getDataInternal(), a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector blend(final SimdVector a, final SimdVector b, final int mask)
      throws WasmException {
    validateSameLane(a, b);
    final byte[] result =
        JniWasmRuntime.nativeSimdBlend(
            runtimeHandle, a.getDataInternal(), b.getDataInternal(), mask);
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector relaxedAdd(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result =
        JniWasmRuntime.nativeSimdRelaxedAdd(
            runtimeHandle, a.getDataInternal(), b.getDataInternal());
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public boolean isSimdSupported() {
    try {
      return JniWasmRuntime.nativeIsSimdSupported(runtimeHandle);
    } catch (final Exception e) {
      return false;
    }
  }

  @Override
  public String getSimdCapabilities() {
    try {
      return JniWasmRuntime.nativeGetSimdCapabilities(runtimeHandle);
    } catch (final Exception e) {
      return "SIMD not available: " + e.getMessage();
    }
  }

  private void validateSameLane(final SimdVector a, final SimdVector b) {
    Objects.requireNonNull(a, "first vector cannot be null");
    Objects.requireNonNull(b, "second vector cannot be null");
    if (a.getLane() != b.getLane()) {
      throw new IllegalArgumentException(
          "Vectors must have same lane type: " + a.getLane() + " vs " + b.getLane());
    }
  }

  private long extractMemoryHandle(final WasmMemory memory) {
    // Check if the memory is backed by JNI implementation
    // Note: Memory and WasmMemory are separate interfaces, but JNI memory instances
    // will implement WasmMemory through JniMemory
    if (memory instanceof JniMemory) {
      return ((JniMemory) memory).getNativeHandle();
    }
    throw new IllegalArgumentException(
        "Memory must be JniMemory instance for JNI SIMD operations. Got: "
            + memory.getClass().getName());
  }
}
