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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * 128-bit SIMD vector value with convenient factory methods.
 *
 * <p>V128 represents a WebAssembly 128-bit SIMD vector that can be interpreted as different lane
 * configurations (4x i32, 4x f32, 2x i64, 2x f64, etc.). This class provides convenient factory
 * methods for creating vectors from Java primitive arrays.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create from integers
 * V128 intVector = V128.fromInts(1, 2, 3, 4);
 *
 * // Create from floats
 * V128 floatVector = V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
 *
 * // Extract values
 * int[] ints = intVector.getAsInts();
 * float[] floats = floatVector.getAsFloats();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class V128 {

  /** Size of a V128 vector in bytes. */
  public static final int SIZE_BYTES = 16;

  private final byte[] data;

  /**
   * Creates a V128 from raw bytes.
   *
   * @param data the vector data (must be exactly 16 bytes)
   * @throws IllegalArgumentException if data is null or not 16 bytes
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Defensive copy is made")
  public V128(final byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }
    if (data.length != SIZE_BYTES) {
      throw new IllegalArgumentException(
          "V128 data must be exactly 16 bytes, got " + data.length);
    }
    this.data = Arrays.copyOf(data, SIZE_BYTES);
  }

  /**
   * Private constructor for internal use without defensive copy.
   *
   * @param data the vector data (must be exactly 16 bytes, not copied)
   * @param trusted marker parameter to distinguish from public constructor
   */
  private V128(final byte[] data, final boolean trusted) {
    this.data = data;
  }

  /**
   * Creates a V128 from four 32-bit integers.
   *
   * @param a first lane value
   * @param b second lane value
   * @param c third lane value
   * @param d fourth lane value
   * @return new V128 with the specified integer lanes
   */
  public static V128 fromInts(final int a, final int b, final int c, final int d) {
    final ByteBuffer buffer = ByteBuffer.allocate(SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(a);
    buffer.putInt(b);
    buffer.putInt(c);
    buffer.putInt(d);
    return new V128(buffer.array(), true);
  }

  /**
   * Creates a V128 from an array of 32-bit integers.
   *
   * @param values the integer values (must be exactly 4 elements)
   * @return new V128 with the specified integer lanes
   * @throws IllegalArgumentException if values is null or not 4 elements
   */
  public static V128 fromInts(final int[] values) {
    if (values == null || values.length != 4) {
      throw new IllegalArgumentException("Values must be an array of exactly 4 integers");
    }
    return fromInts(values[0], values[1], values[2], values[3]);
  }

  /**
   * Creates a V128 from four 32-bit floats.
   *
   * @param a first lane value
   * @param b second lane value
   * @param c third lane value
   * @param d fourth lane value
   * @return new V128 with the specified float lanes
   */
  public static V128 fromFloats(final float a, final float b, final float c, final float d) {
    final ByteBuffer buffer = ByteBuffer.allocate(SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putFloat(a);
    buffer.putFloat(b);
    buffer.putFloat(c);
    buffer.putFloat(d);
    return new V128(buffer.array(), true);
  }

  /**
   * Creates a V128 from an array of 32-bit floats.
   *
   * @param values the float values (must be exactly 4 elements)
   * @return new V128 with the specified float lanes
   * @throws IllegalArgumentException if values is null or not 4 elements
   */
  public static V128 fromFloats(final float[] values) {
    if (values == null || values.length != 4) {
      throw new IllegalArgumentException("Values must be an array of exactly 4 floats");
    }
    return fromFloats(values[0], values[1], values[2], values[3]);
  }

  /**
   * Creates a V128 from two 64-bit integers.
   *
   * @param a first lane value
   * @param b second lane value
   * @return new V128 with the specified long lanes
   */
  public static V128 fromLongs(final long a, final long b) {
    final ByteBuffer buffer = ByteBuffer.allocate(SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(a);
    buffer.putLong(b);
    return new V128(buffer.array(), true);
  }

  /**
   * Creates a V128 from two 64-bit doubles.
   *
   * @param a first lane value
   * @param b second lane value
   * @return new V128 with the specified double lanes
   */
  public static V128 fromDoubles(final double a, final double b) {
    final ByteBuffer buffer = ByteBuffer.allocate(SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putDouble(a);
    buffer.putDouble(b);
    return new V128(buffer.array(), true);
  }

  /**
   * Creates a V128 from raw bytes.
   *
   * @param bytes the byte values (must be exactly 16 bytes)
   * @return new V128 with the specified bytes
   * @throws IllegalArgumentException if bytes is null or not 16 elements
   */
  public static V128 fromBytes(final byte[] bytes) {
    return new V128(bytes);
  }

  /**
   * Creates a V128 with all zeros.
   *
   * @return new V128 with all lanes set to zero
   */
  public static V128 zero() {
    return new V128(new byte[SIZE_BYTES], true);
  }

  /**
   * Creates a V128 by splatting an integer value across all lanes.
   *
   * @param value the value to splat
   * @return new V128 with value in all i32 lanes
   */
  public static V128 splatInt(final int value) {
    return fromInts(value, value, value, value);
  }

  /**
   * Creates a V128 by splatting a float value across all lanes.
   *
   * @param value the value to splat
   * @return new V128 with value in all f32 lanes
   */
  public static V128 splatFloat(final float value) {
    return fromFloats(value, value, value, value);
  }

  /**
   * Gets the vector data as four 32-bit integers.
   *
   * @return array of 4 integers
   */
  public int[] getAsInts() {
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return new int[] {buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt()};
  }

  /**
   * Gets the vector data as four 32-bit floats.
   *
   * @return array of 4 floats
   */
  public float[] getAsFloats() {
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return new float[] {buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat()};
  }

  /**
   * Gets the vector data as two 64-bit integers.
   *
   * @return array of 2 longs
   */
  public long[] getAsLongs() {
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return new long[] {buffer.getLong(), buffer.getLong()};
  }

  /**
   * Gets the vector data as two 64-bit doubles.
   *
   * @return array of 2 doubles
   */
  public double[] getAsDoubles() {
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return new double[] {buffer.getDouble(), buffer.getDouble()};
  }

  /**
   * Gets a defensive copy of the raw byte data.
   *
   * @return 16-byte array containing vector data
   */
  public byte[] getBytes() {
    return Arrays.copyOf(data, SIZE_BYTES);
  }

  /**
   * Gets a specific integer lane value.
   *
   * @param index the lane index (0-3)
   * @return the lane value as an integer
   * @throws IndexOutOfBoundsException if index is not 0-3
   */
  public int getIntLane(final int index) {
    if (index < 0 || index > 3) {
      throw new IndexOutOfBoundsException("Lane index must be 0-3, got " + index);
    }
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    buffer.position(index * 4);
    return buffer.getInt();
  }

  /**
   * Gets a specific float lane value.
   *
   * @param index the lane index (0-3)
   * @return the lane value as a float
   * @throws IndexOutOfBoundsException if index is not 0-3
   */
  public float getFloatLane(final int index) {
    if (index < 0 || index > 3) {
      throw new IndexOutOfBoundsException("Lane index must be 0-3, got " + index);
    }
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    buffer.position(index * 4);
    return buffer.getFloat();
  }

  /**
   * Creates a new V128 with a specific integer lane replaced.
   *
   * @param index the lane index (0-3)
   * @param value the new lane value
   * @return new V128 with the replaced lane
   * @throws IndexOutOfBoundsException if index is not 0-3
   */
  public V128 withIntLane(final int index, final int value) {
    if (index < 0 || index > 3) {
      throw new IndexOutOfBoundsException("Lane index must be 0-3, got " + index);
    }
    final byte[] newData = Arrays.copyOf(data, SIZE_BYTES);
    final ByteBuffer buffer = ByteBuffer.wrap(newData).order(ByteOrder.LITTLE_ENDIAN);
    buffer.position(index * 4);
    buffer.putInt(value);
    return new V128(newData, true);
  }

  /**
   * Creates a new V128 with a specific float lane replaced.
   *
   * @param index the lane index (0-3)
   * @param value the new lane value
   * @return new V128 with the replaced lane
   * @throws IndexOutOfBoundsException if index is not 0-3
   */
  public V128 withFloatLane(final int index, final float value) {
    if (index < 0 || index > 3) {
      throw new IndexOutOfBoundsException("Lane index must be 0-3, got " + index);
    }
    final byte[] newData = Arrays.copyOf(data, SIZE_BYTES);
    final ByteBuffer buffer = ByteBuffer.wrap(newData).order(ByteOrder.LITTLE_ENDIAN);
    buffer.position(index * 4);
    buffer.putFloat(value);
    return new V128(newData, true);
  }

  /**
   * Converts this V128 to a SimdVector with the specified lane type.
   *
   * @param lane the lane type for the SimdVector
   * @return a new SimdVector with the same data
   */
  public SimdVector toSimdVector(final SimdLane lane) {
    return new SimdVector(lane, data);
  }

  /**
   * Creates a V128 from a SimdVector.
   *
   * @param vector the source SimdVector
   * @return a new V128 with the same data
   * @throws IllegalArgumentException if vector is null
   */
  public static V128 fromSimdVector(final SimdVector vector) {
    if (vector == null) {
      throw new IllegalArgumentException("Vector cannot be null");
    }
    return new V128(vector.getData());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof V128)) {
      return false;
    }
    final V128 other = (V128) obj;
    return Arrays.equals(data, other.data);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }

  @Override
  public String toString() {
    final int[] ints = getAsInts();
    return String.format(
        "V128[i32x4: %d, %d, %d, %d]", ints[0], ints[1], ints[2], ints[3]);
  }

  /**
   * Returns a hexadecimal string representation of the vector data.
   *
   * @return hex string of the 16 bytes
   */
  public String toHexString() {
    final StringBuilder sb = new StringBuilder(SIZE_BYTES * 2);
    for (final byte b : data) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
