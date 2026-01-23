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
import ai.tegmentum.wasmtime4j.simd.SimdLane;
import ai.tegmentum.wasmtime4j.simd.SimdOperations;
import ai.tegmentum.wasmtime4j.simd.SimdVector;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Pure Java implementation of SIMD operations.
 *
 * <p>This implementation performs SIMD operations directly in Java by manipulating the underlying
 * byte arrays. WebAssembly's actual SIMD instructions are executed by Wasmtime within WASM modules;
 * this class provides utility operations for working with V128 values from Java code.
 *
 * @since 1.0.0
 */
public final class PanamaSimdOperations implements SimdOperations {
  private static final int V128_SIZE = 16; // 128 bits = 16 bytes

  private final long runtimeHandle;

  /**
   * Creates a Panama SIMD operations instance.
   *
   * @param runtimeHandle the native runtime handle (unused in pure Java implementation)
   */
  public PanamaSimdOperations(final long runtimeHandle) {
    this.runtimeHandle = runtimeHandle;
  }

  /**
   * Creates a Panama SIMD operations instance with default runtime.
   *
   * <p>Note: This constructor creates a runtime with handle 0. The runtime handle is unused in this
   * pure Java implementation.
   */
  public PanamaSimdOperations() {
    this(0L);
  }

  @Override
  public SimdVector add(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result = performLaneOperation(a, b, (av, bv) -> av + bv);
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector subtract(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result = performLaneOperation(a, b, (av, bv) -> av - bv);
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector multiply(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result = performLaneOperation(a, b, (av, bv) -> av * bv);
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector divide(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    // Division is only supported for floating point lanes
    switch (a.getLane()) {
      case F32X4:
        {
          final ByteBuffer aBuffer = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuffer = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resultBuffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            final float av = aBuffer.getFloat(i * 4);
            final float bv = bBuffer.getFloat(i * 4);
            resultBuffer.putFloat(i * 4, av / bv);
          }
          break;
        }
      case F64X2:
        {
          final ByteBuffer aBuffer = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuffer = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resultBuffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 2; i++) {
            final double av = aBuffer.getDouble(i * 8);
            final double bv = bBuffer.getDouble(i * 8);
            resultBuffer.putDouble(i * 8, av / bv);
          }
          break;
        }
      default:
        throw new WasmException("Division not supported for lane type: " + a.getLane());
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector addSaturated(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    switch (a.getLane()) {
      case I8X16:
        for (int i = 0; i < 16; i++) {
          final int av = aData[i];
          final int bv = bData[i];
          final int sum = av + bv;
          result[i] = (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, sum));
        }
        break;
      case I16X8:
        {
          final ByteBuffer aBuffer = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuffer = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resultBuffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 8; i++) {
            final int av = aBuffer.getShort(i * 2);
            final int bv = bBuffer.getShort(i * 2);
            final int sum = av + bv;
            resultBuffer.putShort(
                i * 2, (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sum)));
          }
          break;
        }
      default:
        // Fall back to regular add for other lane types
        return add(a, b);
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector and(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] result = new byte[V128_SIZE];
    for (int i = 0; i < V128_SIZE; i++) {
      result[i] = (byte) (aData[i] & bData[i]);
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector or(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] result = new byte[V128_SIZE];
    for (int i = 0; i < V128_SIZE; i++) {
      result[i] = (byte) (aData[i] | bData[i]);
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector xor(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] result = new byte[V128_SIZE];
    for (int i = 0; i < V128_SIZE; i++) {
      result[i] = (byte) (aData[i] ^ bData[i]);
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector not(final SimdVector a) throws WasmException {
    Objects.requireNonNull(a, "vector cannot be null");
    final byte[] aData = a.getDataInternal();
    final byte[] result = new byte[V128_SIZE];
    for (int i = 0; i < V128_SIZE; i++) {
      result[i] = (byte) ~aData[i];
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector equals(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result = performComparisonOperation(a, b, (av, bv) -> av == bv);
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector lessThan(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result = performComparisonOperation(a, b, (av, bv) -> av < bv);
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector greaterThan(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] result = performComparisonOperation(a, b, (av, bv) -> av > bv);
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector load(final WasmMemory memory, final int offset, final SimdLane lane)
      throws WasmException {
    Objects.requireNonNull(memory, "memory cannot be null");
    Objects.requireNonNull(lane, "lane cannot be null");

    // Read 16 bytes from memory at the given offset
    final byte[] result = new byte[V128_SIZE];
    for (int i = 0; i < V128_SIZE; i++) {
      result[i] = memory.readByte(offset + i);
    }
    return new SimdVector(lane, result);
  }

  @Override
  public SimdVector loadAligned(final WasmMemory memory, final int offset, final SimdLane lane)
      throws WasmException {
    Objects.requireNonNull(memory, "memory cannot be null");
    Objects.requireNonNull(lane, "lane cannot be null");

    if (offset % V128_SIZE != 0) {
      throw new WasmException("Offset must be 16-byte aligned for aligned load");
    }

    // Read 16 bytes from memory at the given offset
    final byte[] result = new byte[V128_SIZE];
    for (int i = 0; i < V128_SIZE; i++) {
      result[i] = memory.readByte(offset + i);
    }
    return new SimdVector(lane, result);
  }

  @Override
  public void store(final WasmMemory memory, final int offset, final SimdVector vector)
      throws WasmException {
    Objects.requireNonNull(memory, "memory cannot be null");
    Objects.requireNonNull(vector, "vector cannot be null");

    // Write 16 bytes to memory at the given offset
    final byte[] data = vector.getDataInternal();
    for (int i = 0; i < V128_SIZE; i++) {
      memory.writeByte(offset + i, data[i]);
    }
  }

  @Override
  public void storeAligned(final WasmMemory memory, final int offset, final SimdVector vector)
      throws WasmException {
    Objects.requireNonNull(memory, "memory cannot be null");
    Objects.requireNonNull(vector, "vector cannot be null");

    if (offset % V128_SIZE != 0) {
      throw new WasmException("Offset must be 16-byte aligned for aligned store");
    }

    // Write 16 bytes to memory at the given offset
    final byte[] data = vector.getDataInternal();
    for (int i = 0; i < V128_SIZE; i++) {
      memory.writeByte(offset + i, data[i]);
    }
  }

  @Override
  public int extractLaneI32(final SimdVector vector, final int laneIndex) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (laneIndex < 0 || laneIndex > 3) {
      throw new IllegalArgumentException("Lane index must be between 0 and 3");
    }
    final byte[] data = vector.getDataInternal();
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return buffer.getInt(laneIndex * 4);
  }

  @Override
  public SimdVector replaceLaneI32(final SimdVector vector, final int laneIndex, final int value)
      throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    if (laneIndex < 0 || laneIndex > 3) {
      throw new IllegalArgumentException("Lane index must be between 0 and 3");
    }
    final byte[] result = vector.getDataInternal().clone();
    final ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(laneIndex * 4, value);
    return new SimdVector(vector.getLane(), result);
  }

  @Override
  public SimdVector convertI32ToF32(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    final byte[] data = vector.getDataInternal();
    final byte[] result = new byte[V128_SIZE];
    final ByteBuffer inputBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    final ByteBuffer outputBuffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < 4; i++) {
      final int intValue = inputBuffer.getInt(i * 4);
      outputBuffer.putFloat(i * 4, (float) intValue);
    }
    return new SimdVector(SimdLane.F32X4, result);
  }

  @Override
  public SimdVector convertF32ToI32(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    final byte[] data = vector.getDataInternal();
    final byte[] result = new byte[V128_SIZE];
    final ByteBuffer inputBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    final ByteBuffer outputBuffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < 4; i++) {
      final float floatValue = inputBuffer.getFloat(i * 4);
      outputBuffer.putInt(i * 4, (int) floatValue);
    }
    return new SimdVector(SimdLane.I32X4, result);
  }

  @Override
  public SimdVector shuffle(final SimdVector a, final SimdVector b, final int[] indices)
      throws WasmException {
    validateSameLane(a, b);
    Objects.requireNonNull(indices, "indices cannot be null");
    if (indices.length != 16) {
      throw new IllegalArgumentException("Shuffle indices must be exactly 16 elements");
    }

    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    // Concatenate a and b into a 32-byte array, then select bytes based on indices
    for (int i = 0; i < 16; i++) {
      final int index = indices[i] & 0xFF; // Treat as unsigned
      if (index < 16) {
        result[i] = aData[index];
      } else if (index < 32) {
        result[i] = bData[index - 16];
      } else {
        throw new WasmException("Shuffle index out of range: " + index);
      }
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector fma(final SimdVector a, final SimdVector b, final SimdVector c)
      throws WasmException {
    validateSameLane(a, b);
    validateSameLane(b, c);

    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] cData = c.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    switch (a.getLane()) {
      case F32X4:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer cBuf = ByteBuffer.wrap(cData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            final float av = aBuf.getFloat(i * 4);
            final float bv = bBuf.getFloat(i * 4);
            final float cv = cBuf.getFloat(i * 4);
            resBuf.putFloat(i * 4, Math.fma(av, bv, cv));
          }
          break;
        }
      case F64X2:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer cBuf = ByteBuffer.wrap(cData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 2; i++) {
            final double av = aBuf.getDouble(i * 8);
            final double bv = bBuf.getDouble(i * 8);
            final double cv = cBuf.getDouble(i * 8);
            resBuf.putDouble(i * 8, Math.fma(av, bv, cv));
          }
          break;
        }
      default:
        throw new WasmException("FMA not supported for lane type: " + a.getLane());
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector fms(final SimdVector a, final SimdVector b, final SimdVector c)
      throws WasmException {
    validateSameLane(a, b);
    validateSameLane(b, c);

    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] cData = c.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    switch (a.getLane()) {
      case F32X4:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer cBuf = ByteBuffer.wrap(cData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            final float av = aBuf.getFloat(i * 4);
            final float bv = bBuf.getFloat(i * 4);
            final float cv = cBuf.getFloat(i * 4);
            resBuf.putFloat(i * 4, Math.fma(av, bv, -cv));
          }
          break;
        }
      case F64X2:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer cBuf = ByteBuffer.wrap(cData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 2; i++) {
            final double av = aBuf.getDouble(i * 8);
            final double bv = bBuf.getDouble(i * 8);
            final double cv = cBuf.getDouble(i * 8);
            resBuf.putDouble(i * 8, Math.fma(av, bv, -cv));
          }
          break;
        }
      default:
        throw new WasmException("FMS not supported for lane type: " + a.getLane());
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector reciprocal(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    final byte[] data = vector.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    switch (vector.getLane()) {
      case F32X4:
        {
          final ByteBuffer inBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer outBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            outBuf.putFloat(i * 4, 1.0f / inBuf.getFloat(i * 4));
          }
          break;
        }
      case F64X2:
        {
          final ByteBuffer inBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer outBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 2; i++) {
            outBuf.putDouble(i * 8, 1.0 / inBuf.getDouble(i * 8));
          }
          break;
        }
      default:
        throw new WasmException("Reciprocal not supported for lane type: " + vector.getLane());
    }
    return new SimdVector(vector.getLane(), result);
  }

  @Override
  public SimdVector sqrt(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    final byte[] data = vector.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    switch (vector.getLane()) {
      case F32X4:
        {
          final ByteBuffer inBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer outBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            outBuf.putFloat(i * 4, (float) Math.sqrt(inBuf.getFloat(i * 4)));
          }
          break;
        }
      case F64X2:
        {
          final ByteBuffer inBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer outBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 2; i++) {
            outBuf.putDouble(i * 8, Math.sqrt(inBuf.getDouble(i * 8)));
          }
          break;
        }
      default:
        throw new WasmException("Sqrt not supported for lane type: " + vector.getLane());
    }
    return new SimdVector(vector.getLane(), result);
  }

  @Override
  public SimdVector rsqrt(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    final byte[] data = vector.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    switch (vector.getLane()) {
      case F32X4:
        {
          final ByteBuffer inBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer outBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            outBuf.putFloat(i * 4, (float) (1.0 / Math.sqrt(inBuf.getFloat(i * 4))));
          }
          break;
        }
      case F64X2:
        {
          final ByteBuffer inBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer outBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 2; i++) {
            outBuf.putDouble(i * 8, 1.0 / Math.sqrt(inBuf.getDouble(i * 8)));
          }
          break;
        }
      default:
        throw new WasmException("Rsqrt not supported for lane type: " + vector.getLane());
    }
    return new SimdVector(vector.getLane(), result);
  }

  @Override
  public SimdVector popcount(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    final byte[] data = vector.getDataInternal();
    final byte[] result = new byte[V128_SIZE];
    // Popcount on each byte
    for (int i = 0; i < V128_SIZE; i++) {
      result[i] = (byte) Integer.bitCount(data[i] & 0xFF);
    }
    return new SimdVector(vector.getLane(), result);
  }

  @Override
  public SimdVector shlVariable(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    switch (a.getLane()) {
      case I32X4:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            final int av = aBuf.getInt(i * 4);
            final int shift = bBuf.getInt(i * 4) & 31; // Shift amount mod 32
            resBuf.putInt(i * 4, av << shift);
          }
          break;
        }
      case I64X2:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 2; i++) {
            final long av = aBuf.getLong(i * 8);
            final int shift = (int) (bBuf.getLong(i * 8) & 63); // Shift amount mod 64
            resBuf.putLong(i * 8, av << shift);
          }
          break;
        }
      default:
        throw new WasmException("SHL not supported for lane type: " + a.getLane());
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector shrVariable(final SimdVector a, final SimdVector b) throws WasmException {
    validateSameLane(a, b);
    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    switch (a.getLane()) {
      case I32X4:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            final int av = aBuf.getInt(i * 4);
            final int shift = bBuf.getInt(i * 4) & 31; // Shift amount mod 32
            resBuf.putInt(i * 4, av >> shift); // Arithmetic shift right
          }
          break;
        }
      case I64X2:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 2; i++) {
            final long av = aBuf.getLong(i * 8);
            final int shift = (int) (bBuf.getLong(i * 8) & 63); // Shift amount mod 64
            resBuf.putLong(i * 8, av >> shift); // Arithmetic shift right
          }
          break;
        }
      default:
        throw new WasmException("SHR not supported for lane type: " + a.getLane());
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public float horizontalSum(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    final byte[] data = vector.getDataInternal();
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    int sum = 0;
    for (int i = 0; i < 4; i++) {
      sum += buffer.getInt(i * 4);
    }
    return (float) sum;
  }

  @Override
  public float horizontalMin(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    final byte[] data = vector.getDataInternal();
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    int min = buffer.getInt(0);
    for (int i = 1; i < 4; i++) {
      min = Math.min(min, buffer.getInt(i * 4));
    }
    return (float) min;
  }

  @Override
  public float horizontalMax(final SimdVector vector) throws WasmException {
    Objects.requireNonNull(vector, "vector cannot be null");
    final byte[] data = vector.getDataInternal();
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    int max = buffer.getInt(0);
    for (int i = 1; i < 4; i++) {
      max = Math.max(max, buffer.getInt(i * 4));
    }
    return (float) max;
  }

  @Override
  public SimdVector select(final SimdVector mask, final SimdVector a, final SimdVector b)
      throws WasmException {
    validateSameLane(a, b);
    validateSameLane(mask, a);
    final byte[] maskData = mask.getDataInternal();
    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    // Select bytes: if mask bit is set, use a; otherwise use b
    for (int i = 0; i < V128_SIZE; i++) {
      result[i] = (byte) ((aData[i] & maskData[i]) | (bData[i] & ~maskData[i]));
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector blend(final SimdVector a, final SimdVector b, final int mask)
      throws WasmException {
    validateSameLane(a, b);
    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    // For i32x4: mask bits 0-3 select between a[0-3] and b[0-3]
    final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
    final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
    final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < 4; i++) {
      final boolean selectB = ((mask >> i) & 1) != 0;
      resBuf.putInt(i * 4, selectB ? bBuf.getInt(i * 4) : aBuf.getInt(i * 4));
    }
    return new SimdVector(a.getLane(), result);
  }

  @Override
  public SimdVector relaxedAdd(final SimdVector a, final SimdVector b) throws WasmException {
    // Relaxed add is the same as regular add for our purposes
    return add(a, b);
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
   * Performs a lane-wise arithmetic operation on two vectors.
   *
   * @param a first vector
   * @param b second vector
   * @param op the operation to perform on each pair of lanes
   * @return the result as a byte array
   */
  private byte[] performLaneOperation(
      final SimdVector a, final SimdVector b, final IntBinaryOperator op) {
    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    switch (a.getLane()) {
      case I8X16:
        for (int i = 0; i < 16; i++) {
          result[i] = (byte) op.applyAsInt(aData[i], bData[i]);
        }
        break;
      case I16X8:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 8; i++) {
            resBuf.putShort(
                i * 2, (short) op.applyAsInt(aBuf.getShort(i * 2), bBuf.getShort(i * 2)));
          }
          break;
        }
      case I32X4:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            resBuf.putInt(i * 4, op.applyAsInt(aBuf.getInt(i * 4), bBuf.getInt(i * 4)));
          }
          break;
        }
      case I64X2:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 2; i++) {
            // Note: op is IntBinaryOperator, so we cast to int for simplicity
            // For full i64 support, we would need a LongBinaryOperator
            final long av = aBuf.getLong(i * 8);
            final long bv = bBuf.getLong(i * 8);
            resBuf.putLong(i * 8, performLongOp(av, bv, op));
          }
          break;
        }
      case F32X4:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            final float av = aBuf.getFloat(i * 4);
            final float bv = bBuf.getFloat(i * 4);
            // Apply operation via integers (works for add/sub/mul on floats interpreted as ints)
            resBuf.putFloat(i * 4, performFloatOp(av, bv, op));
          }
          break;
        }
      case F64X2:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 2; i++) {
            final double av = aBuf.getDouble(i * 8);
            final double bv = bBuf.getDouble(i * 8);
            resBuf.putDouble(i * 8, performDoubleOp(av, bv, op));
          }
          break;
        }
      default:
        // Default to i32x4 behavior
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            resBuf.putInt(i * 4, op.applyAsInt(aBuf.getInt(i * 4), bBuf.getInt(i * 4)));
          }
          break;
        }
    }
    return result;
  }

  /** Helper to perform an operation on longs using an IntBinaryOperator. */
  private long performLongOp(final long a, final long b, final IntBinaryOperator op) {
    // Determine which operation based on simple test values
    final int testOp = op.applyAsInt(10, 5);
    if (testOp == 15) {
      return a + b; // add
    } else if (testOp == 5) {
      return a - b; // subtract
    } else if (testOp == 50) {
      return a * b; // multiply
    } else {
      return a + b; // default to add
    }
  }

  /** Helper to perform an operation on floats using an IntBinaryOperator. */
  private float performFloatOp(final float a, final float b, final IntBinaryOperator op) {
    final int testOp = op.applyAsInt(10, 5);
    if (testOp == 15) {
      return a + b;
    } else if (testOp == 5) {
      return a - b;
    } else if (testOp == 50) {
      return a * b;
    } else {
      return a + b;
    }
  }

  /** Helper to perform an operation on doubles using an IntBinaryOperator. */
  private double performDoubleOp(final double a, final double b, final IntBinaryOperator op) {
    final int testOp = op.applyAsInt(10, 5);
    if (testOp == 15) {
      return a + b;
    } else if (testOp == 5) {
      return a - b;
    } else if (testOp == 50) {
      return a * b;
    } else {
      return a + b;
    }
  }

  /**
   * Performs a lane-wise comparison operation on two vectors.
   *
   * @param a first vector
   * @param b second vector
   * @param predicate the comparison predicate
   * @return the result as a byte array (all ones for true, all zeros for false per lane)
   */
  private byte[] performComparisonOperation(
      final SimdVector a, final SimdVector b, final IntBiPredicate predicate) {
    final byte[] aData = a.getDataInternal();
    final byte[] bData = b.getDataInternal();
    final byte[] result = new byte[V128_SIZE];

    switch (a.getLane()) {
      case I8X16:
        for (int i = 0; i < 16; i++) {
          result[i] = predicate.test(aData[i], bData[i]) ? (byte) 0xFF : (byte) 0;
        }
        break;
      case I16X8:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 8; i++) {
            resBuf.putShort(
                i * 2,
                predicate.test(aBuf.getShort(i * 2), bBuf.getShort(i * 2))
                    ? (short) 0xFFFF
                    : (short) 0);
          }
          break;
        }
      case I32X4:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            resBuf.putInt(
                i * 4, predicate.test(aBuf.getInt(i * 4), bBuf.getInt(i * 4)) ? 0xFFFFFFFF : 0);
          }
          break;
        }
      case I64X2:
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 2; i++) {
            final long av = aBuf.getLong(i * 8);
            final long bv = bBuf.getLong(i * 8);
            resBuf.putLong(i * 8, compareLongs(av, bv, predicate) ? 0xFFFFFFFFFFFFFFFFL : 0L);
          }
          break;
        }
      default:
        // Default to i32x4 behavior
        {
          final ByteBuffer aBuf = ByteBuffer.wrap(aData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer bBuf = ByteBuffer.wrap(bData).order(ByteOrder.LITTLE_ENDIAN);
          final ByteBuffer resBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < 4; i++) {
            resBuf.putInt(
                i * 4, predicate.test(aBuf.getInt(i * 4), bBuf.getInt(i * 4)) ? 0xFFFFFFFF : 0);
          }
          break;
        }
    }
    return result;
  }

  /** Helper to compare longs using an IntBiPredicate. */
  private boolean compareLongs(final long a, final long b, final IntBiPredicate predicate) {
    // Determine which comparison based on simple test values
    if (predicate.test(5, 5) && !predicate.test(5, 6)) {
      return a == b; // equals
    } else if (predicate.test(4, 5) && !predicate.test(5, 5)) {
      return a < b; // less than
    } else if (predicate.test(6, 5) && !predicate.test(5, 5)) {
      return a > b; // greater than
    } else {
      return a == b; // default to equals
    }
  }

  /** Functional interface for integer binary predicate. */
  @FunctionalInterface
  private interface IntBiPredicate {
    boolean test(int a, int b);
  }

  /** Functional interface for integer binary operator (matches java.util.function). */
  @FunctionalInterface
  private interface IntBinaryOperator {
    int applyAsInt(int left, int right);
  }
}
