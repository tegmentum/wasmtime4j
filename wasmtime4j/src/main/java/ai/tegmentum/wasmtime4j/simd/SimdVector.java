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

package ai.tegmentum.wasmtime4j.simd;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable SIMD vector value.
 *
 * <p>Represents a 128-bit SIMD vector with a specific lane configuration. Vectors are immutable
 * value objects - all operations return new vector instances.
 *
 * @since 1.0.0
 */
public final class SimdVector {
  private final SimdLane lane;
  private final byte[] data;

  /**
   * Creates a SIMD vector from raw bytes.
   *
   * @param lane the lane type
   * @param data the vector data (must be exactly 16 bytes)
   * @throws IllegalArgumentException if data is not 16 bytes
   */
  public SimdVector(final SimdLane lane, final byte[] data) {
    Objects.requireNonNull(lane, "lane cannot be null");
    Objects.requireNonNull(data, "data cannot be null");

    if (data.length != 16) {
      throw new IllegalArgumentException(
          "SIMD vector data must be exactly 16 bytes, got " + data.length);
    }

    this.lane = lane;
    this.data = Arrays.copyOf(data, 16); // Defensive copy for immutability
  }

  /**
   * Gets the lane type of this vector.
   *
   * @return the lane type
   */
  public SimdLane getLane() {
    return lane;
  }

  /**
   * Gets a defensive copy of the vector data.
   *
   * @return 16-byte array containing vector data
   */
  public byte[] getData() {
    return Arrays.copyOf(data, 16);
  }

  /**
   * Gets the vector data without defensive copy (for internal use).
   *
   * <p><strong>Warning:</strong> This method returns the internal array. Callers must not modify
   * it.
   *
   * @return the internal data array (do not modify!)
   */
  byte[] getDataInternal() {
    return data;
  }

  /**
   * Creates a vector by splatting a 32-bit integer across all lanes.
   *
   * @param lane the lane type (must be integer type)
   * @param value the value to splat
   * @return new vector with value in all lanes
   * @throws IllegalArgumentException if lane is not an integer type
   */
  public static SimdVector splatI32(final SimdLane lane, final int value) {
    if (!lane.isIntegerType()) {
      throw new IllegalArgumentException("Lane must be integer type for splatI32");
    }

    final byte[] data = new byte[16];
    final int bytesPerElement = lane.getBytesPerElement();

    for (int i = 0; i < 16; i += bytesPerElement) {
      switch (bytesPerElement) {
        case 1:
          data[i] = (byte) value;
          break;
        case 2:
          data[i] = (byte) value;
          data[i + 1] = (byte) (value >> 8);
          break;
        case 4:
          data[i] = (byte) value;
          data[i + 1] = (byte) (value >> 8);
          data[i + 2] = (byte) (value >> 16);
          data[i + 3] = (byte) (value >> 24);
          break;
        case 8:
          // For 64-bit, sign-extend the 32-bit value
          data[i] = (byte) value;
          data[i + 1] = (byte) (value >> 8);
          data[i + 2] = (byte) (value >> 16);
          data[i + 3] = (byte) (value >> 24);
          final byte signByte = (byte) (value >> 31);
          data[i + 4] = signByte;
          data[i + 5] = signByte;
          data[i + 6] = signByte;
          data[i + 7] = signByte;
          break;
        default:
          throw new IllegalStateException("Unexpected bytes per element: " + bytesPerElement);
      }
    }

    return new SimdVector(lane, data);
  }

  /**
   * Creates a vector by splatting a 32-bit float across all lanes.
   *
   * @param lane the lane type (must be F32X4)
   * @param value the value to splat
   * @return new vector with value in all lanes
   * @throws IllegalArgumentException if lane is not F32X4
   */
  public static SimdVector splatF32(final SimdLane lane, final float value) {
    if (lane != SimdLane.F32X4) {
      throw new IllegalArgumentException("Lane must be F32X4 for splatF32");
    }

    final byte[] data = new byte[16];
    final int bits = Float.floatToRawIntBits(value);

    for (int i = 0; i < 16; i += 4) {
      data[i] = (byte) bits;
      data[i + 1] = (byte) (bits >> 8);
      data[i + 2] = (byte) (bits >> 16);
      data[i + 3] = (byte) (bits >> 24);
    }

    return new SimdVector(lane, data);
  }

  /**
   * Creates a vector by splatting a 64-bit float across all lanes.
   *
   * @param lane the lane type (must be F64X2)
   * @param value the value to splat
   * @return new vector with value in all lanes
   * @throws IllegalArgumentException if lane is not F64X2
   */
  public static SimdVector splatF64(final SimdLane lane, final double value) {
    if (lane != SimdLane.F64X2) {
      throw new IllegalArgumentException("Lane must be F64X2 for splatF64");
    }

    final byte[] data = new byte[16];
    final long bits = Double.doubleToRawLongBits(value);

    for (int i = 0; i < 16; i += 8) {
      data[i] = (byte) bits;
      data[i + 1] = (byte) (bits >> 8);
      data[i + 2] = (byte) (bits >> 16);
      data[i + 3] = (byte) (bits >> 24);
      data[i + 4] = (byte) (bits >> 32);
      data[i + 5] = (byte) (bits >> 40);
      data[i + 6] = (byte) (bits >> 48);
      data[i + 7] = (byte) (bits >> 56);
    }

    return new SimdVector(lane, data);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SimdVector)) {
      return false;
    }
    final SimdVector other = (SimdVector) obj;
    return lane == other.lane && Arrays.equals(data, other.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lane, Arrays.hashCode(data));
  }

  @Override
  public String toString() {
    return String.format("SimdVector[%s, data=%s]", lane, bytesToHex(data));
  }

  private static String bytesToHex(final byte[] bytes) {
    final StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (final byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
