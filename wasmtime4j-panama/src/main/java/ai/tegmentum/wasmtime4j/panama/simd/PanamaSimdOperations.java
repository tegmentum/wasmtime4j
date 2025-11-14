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

package ai.tegmentum.wasmtime4j.panama.simd;

import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import ai.tegmentum.wasmtime4j.simd.SimdLane;
import ai.tegmentum.wasmtime4j.simd.SimdOperations;
import ai.tegmentum.wasmtime4j.simd.SimdVector;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/**
 * Panama FFI implementation of SIMD operations.
 *
 * @since 1.0.0
 */
public final class PanamaSimdOperations implements SimdOperations {
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();
  private static final int V128_SIZE = 16; // 128 bits = 16 bytes

  private final long runtimeHandle;

  /**
   * Creates a Panama SIMD operations instance.
   *
   * @param runtimeHandle the native runtime handle
   */
  public PanamaSimdOperations(final long runtimeHandle) {
    this.runtimeHandle = runtimeHandle;
  }

  /**
   * Creates a Panama SIMD operations instance with default runtime.
   *
   * <p>Note: This constructor creates a runtime with handle 0. Prefer using constructor with
   * explicit runtime handle.
   */
  public PanamaSimdOperations() {
    this(0L);
  }

  @Override
  public SimdVector add(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdAdd(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD add operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector subtract(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdSubtract(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD subtract operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector multiply(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdMultiply(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD multiply operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector divide(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdDivide(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD divide operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector addSaturated(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdAddSaturated(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD addSaturated operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector and(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdAnd(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD and operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector or(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdOr(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD or operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector xor(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdXor(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD xor operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector not(final SimdVector a) throws WasmException {
    Objects.requireNonNull(a, "vector cannot be null");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdNot(runtimeHandle, aData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD not operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector equals(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdEquals(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD equals operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector lessThan(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdLessThan(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD lessThan operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector greaterThan(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdGreaterThan(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD greaterThan operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector load(final WasmMemory memory, final int offset, final SimdLane lane)
      throws WasmException {
    Objects.requireNonNull(memory, "memory cannot be null");
    Objects.requireNonNull(lane, "lane cannot be null");

    if (!(memory instanceof ai.tegmentum.wasmtime4j.panama.PanamaMemory)) {
      throw new IllegalArgumentException("Memory must be Panama implementation");
    }
    final ai.tegmentum.wasmtime4j.panama.PanamaMemory panamaMemory =
        (ai.tegmentum.wasmtime4j.panama.PanamaMemory) memory;
    final MemorySegment memoryHandle = panamaMemory.getNativeMemory();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdLoad(runtimeHandle, memoryHandle, offset, resultData);
      if (status != 0) {
        throw new WasmException("SIMD load operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(lane, result);
    }
  }

  @Override
  public SimdVector loadAligned(final WasmMemory memory, final int offset, final SimdLane lane)
      throws WasmException {
    Objects.requireNonNull(memory, "memory cannot be null");
    Objects.requireNonNull(lane, "lane cannot be null");

    if (!(memory instanceof ai.tegmentum.wasmtime4j.panama.PanamaMemory)) {
      throw new IllegalArgumentException("Memory must be Panama implementation");
    }
    final ai.tegmentum.wasmtime4j.panama.PanamaMemory panamaMemory =
        (ai.tegmentum.wasmtime4j.panama.PanamaMemory) memory;
    final MemorySegment memoryHandle = panamaMemory.getNativeMemory();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status =
          NATIVE_BINDINGS.simdLoadAligned(runtimeHandle, memoryHandle, offset, resultData);
      if (status != 0) {
        throw new WasmException("SIMD loadAligned operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(lane, result);
    }
  }

  @Override
  public void store(final WasmMemory memory, final int offset, final SimdVector vector)
      throws WasmException {
    Objects.requireNonNull(memory, "memory cannot be null");
    Objects.requireNonNull(vector, "vector cannot be null");

    if (!(memory instanceof ai.tegmentum.wasmtime4j.panama.PanamaMemory)) {
      throw new IllegalArgumentException("Memory must be Panama implementation");
    }
    final ai.tegmentum.wasmtime4j.panama.PanamaMemory panamaMemory =
        (ai.tegmentum.wasmtime4j.panama.PanamaMemory) memory;
    final MemorySegment memoryHandle = panamaMemory.getNativeMemory();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      final int status = NATIVE_BINDINGS.simdStore(runtimeHandle, memoryHandle, offset, vectorData);
      if (status != 0) {
        throw new WasmException("SIMD store operation failed with status: " + status);
      }
    }
  }

  @Override
  public void storeAligned(final WasmMemory memory, final int offset, final SimdVector vector)
      throws WasmException {
    Objects.requireNonNull(memory, "memory cannot be null");
    Objects.requireNonNull(vector, "vector cannot be null");

    if (!(memory instanceof ai.tegmentum.wasmtime4j.panama.PanamaMemory)) {
      throw new IllegalArgumentException("Memory must be Panama implementation");
    }
    final ai.tegmentum.wasmtime4j.panama.PanamaMemory panamaMemory =
        (ai.tegmentum.wasmtime4j.panama.PanamaMemory) memory;
    final MemorySegment memoryHandle = panamaMemory.getNativeMemory();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      final int status =
          NATIVE_BINDINGS.simdStoreAligned(runtimeHandle, memoryHandle, offset, vectorData);
      if (status != 0) {
        throw new WasmException("SIMD storeAligned operation failed with status: " + status);
      }
    }
  }

  @Override
  public int extractLaneI32(final SimdVector vector, final int laneIndex) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (laneIndex < 0 || laneIndex > 3) {
      throw new IllegalArgumentException("Lane index must be between 0 and 3");
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      return NATIVE_BINDINGS.simdExtractLaneI32(runtimeHandle, vectorData, laneIndex);
    }
  }

  @Override
  public SimdVector replaceLaneI32(final SimdVector vector, final int laneIndex, final int value)
      throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (laneIndex < 0 || laneIndex > 3) {
      throw new IllegalArgumentException("Lane index must be between 0 and 3");
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status =
          NATIVE_BINDINGS.simdReplaceLaneI32(
              runtimeHandle, vectorData, laneIndex, value, resultData);
      if (status != 0) {
        throw new WasmException("SIMD replaceLaneI32 operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(vector.getLane(), result);
    }
  }

  @Override
  public SimdVector convertI32ToF32(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdConvertI32ToF32(runtimeHandle, vectorData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD convertI32ToF32 operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(SimdLane.F32X4, result);
    }
  }

  @Override
  public SimdVector convertF32ToI32(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdConvertF32ToI32(runtimeHandle, vectorData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD convertF32ToI32 operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(SimdLane.I32X4, result);
    }
  }

  @Override
  public SimdVector shuffle(final SimdVector a, final SimdVector b, final int[] indices)
      throws WasmException {
    validateSameLane(a, b);
    Objects.requireNonNull(indices, "indices cannot be null");
    if (indices.length != 16) {
      throw new IllegalArgumentException("Shuffle indices must be exactly 16 elements");
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());

      // Convert int[] to byte[] for indices
      final byte[] indicesBytes = new byte[16];
      for (int i = 0; i < 16; i++) {
        indicesBytes[i] = (byte) indices[i];
      }
      final MemorySegment indicesData = toMemorySegment(arena, indicesBytes);

      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status =
          NATIVE_BINDINGS.simdShuffle(runtimeHandle, aData, bData, indicesData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD shuffle operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector fma(final SimdVector a, final SimdVector b, final SimdVector c)
      throws WasmException {
    validateSameLane(a, b);
    validateSameLane(b, c);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment cData = toMemorySegment(arena, c.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdFma(runtimeHandle, aData, bData, cData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD fma operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector fms(final SimdVector a, final SimdVector b, final SimdVector c)
      throws WasmException {
    validateSameLane(a, b);
    validateSameLane(b, c);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment cData = toMemorySegment(arena, c.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdFms(runtimeHandle, aData, bData, cData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD fms operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector reciprocal(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdReciprocal(runtimeHandle, vectorData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD reciprocal operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(vector.getLane(), result);
    }
  }

  @Override
  public SimdVector sqrt(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdSqrt(runtimeHandle, vectorData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD sqrt operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(vector.getLane(), result);
    }
  }

  @Override
  public SimdVector rsqrt(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdRsqrt(runtimeHandle, vectorData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD rsqrt operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(vector.getLane(), result);
    }
  }

  @Override
  public SimdVector popcount(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdPopcount(runtimeHandle, vectorData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD popcount operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(vector.getLane(), result);
    }
  }

  @Override
  public SimdVector shlVariable(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdShlVariable(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD shlVariable operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector shrVariable(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdShrVariable(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD shrVariable operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public float horizontalSum(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      final int result = NATIVE_BINDINGS.simdHorizontalSumI32(runtimeHandle, vectorData);
      return (float) result;
    }
  }

  @Override
  public float horizontalMin(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      final int result = NATIVE_BINDINGS.simdHorizontalMinI32(runtimeHandle, vectorData);
      return (float) result;
    }
  }

  @Override
  public float horizontalMax(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment vectorData = toMemorySegment(arena, vector.getDataInternal());
      final int result = NATIVE_BINDINGS.simdHorizontalMaxI32(runtimeHandle, vectorData);
      return (float) result;
    }
  }

  @Override
  public SimdVector select(final SimdVector mask, final SimdVector a, final SimdVector b)
      throws WasmException {
    validateSameLane(a, b);
    validateSameLane(mask, a);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment maskData = toMemorySegment(arena, mask.getDataInternal());
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status =
          NATIVE_BINDINGS.simdSelect(runtimeHandle, maskData, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD select operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector blend(final SimdVector a, final SimdVector b, final int mask)
      throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdBlend(runtimeHandle, aData, bData, mask, resultData);
      if (status != 0) {
        throw new WasmException("SIMD blend operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public SimdVector relaxedAdd(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment aData = toMemorySegment(arena, a.getDataInternal());
      final MemorySegment bData = toMemorySegment(arena, b.getDataInternal());
      final MemorySegment resultData = arena.allocate(V128_SIZE);
      final int status = NATIVE_BINDINGS.simdRelaxedAdd(runtimeHandle, aData, bData, resultData);
      if (status != 0) {
        throw new WasmException("SIMD relaxedAdd operation failed with status: " + status);
      }
      final byte[] result = fromMemorySegment(resultData);
      return new SimdVector(a.getLane(), result);
    }
  }

  @Override
  public boolean isSimdSupported() {
    return true; // Full SIMD operations supported
  }

  @Override
  public String getSimdCapabilities() {
    return "Panama SIMD: Arithmetic (add, subtract, multiply, divide, addSaturated), Bitwise (and,"
        + " or, xor, not), Comparison (equals, lessThan, greaterThan), Memory operations (load,"
        + " loadAligned, store, storeAligned), Math (sqrt, reciprocal, rsqrt, fma, fms),"
        + " Lane operations (extractLaneI32, replaceLaneI32, popcount, shlVariable, shrVariable,"
        + " select, blend), Conversion (convertI32ToF32, convertF32ToI32), Reduction"
        + " (horizontalSum, horizontalMin, horizontalMax), Advanced (shuffle, relaxedAdd)."
        + " All 37 SIMD operations supported.";
  }

  /**
   * Validates that two vectors have the same lane type.
   *
   * @param a first vector
   * @param b second vector
   * @throws IllegalArgumentException if lane types don't match
   */
  private void validateSameLane(final SimdVector a, final SimdVector b) {
    Objects.requireNonNull(a, "first vector cannot be null");
    Objects.requireNonNull(b, "second vector cannot be null");
    if (a.getLane() != b.getLane()) {
      throw new IllegalArgumentException(
          "Lane type mismatch: " + a.getLane() + " vs " + b.getLane());
    }
  }

  /**
   * Converts a byte array to a MemorySegment for passing to native code.
   *
   * @param arena the arena to allocate in
   * @param data the byte array
   * @return a MemorySegment containing the data
   */
  private MemorySegment toMemorySegment(final Arena arena, final byte[] data) {
    if (data.length != V128_SIZE) {
      throw new IllegalArgumentException("SIMD vector must be exactly " + V128_SIZE + " bytes");
    }
    final MemorySegment segment = arena.allocate(V128_SIZE);
    MemorySegment.copy(data, 0, segment, ValueLayout.JAVA_BYTE, 0, V128_SIZE);
    return segment;
  }

  /**
   * Converts a MemorySegment from native code back to a byte array.
   *
   * @param segment the MemorySegment
   * @return a byte array containing the data
   */
  private byte[] fromMemorySegment(final MemorySegment segment) {
    final byte[] result = new byte[V128_SIZE];
    MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0, result, 0, V128_SIZE);
    return result;
  }
}
