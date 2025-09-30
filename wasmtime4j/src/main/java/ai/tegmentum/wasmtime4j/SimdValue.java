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

package ai.tegmentum.wasmtime4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * SIMD value types for WebAssembly vector operations.
 *
 * <p>This class provides comprehensive support for SIMD vector value types including:
 *
 * <ul>
 *   <li>V128 vectors (128-bit) - standard WebAssembly SIMD
 *   <li>V256 vectors (256-bit) - extended SIMD for AVX support
 *   <li>V512 vectors (512-bit) - extended SIMD for AVX-512 support
 *   <li>Platform capability detection and optimization
 *   <li>Type-safe vector operations and conversions
 * </ul>
 *
 * <p>All vector types support both signed and unsigned integer operations, floating-point
 * operations, and platform-specific optimizations when available.
 *
 * @since 1.0.0
 */
public final class SimdValue {

  /** Vector width enumeration for different SIMD vector sizes. */
  public enum VectorWidth {
    /** 128-bit vectors (v128) - Standard WebAssembly SIMD. */
    V128(128, 16),
    /** 256-bit vectors (v256) - Extended SIMD for AVX. */
    V256(256, 32),
    /** 512-bit vectors (v512) - Extended SIMD for AVX-512. */
    V512(512, 64);

    private final int bitWidth;
    private final int byteWidth;

    VectorWidth(final int bitWidth, final int byteWidth) {
      this.bitWidth = bitWidth;
      this.byteWidth = byteWidth;
    }

    /**
     * Gets the bit width of this vector type.
     *
     * @return bit width
     */
    public int getBitWidth() {
      return bitWidth;
    }

    /**
     * Gets the byte width of this vector type.
     *
     * @return byte width
     */
    public int getByteWidth() {
      return byteWidth;
    }
  }

  /** Lane type enumeration for different data types within vectors. */
  public enum LaneType {
    /** 8-bit signed integers. */
    I8(1, 8),
    /** 8-bit unsigned integers. */
    U8(1, 8),
    /** 16-bit signed integers. */
    I16(2, 16),
    /** 16-bit unsigned integers. */
    U16(2, 16),
    /** 32-bit signed integers. */
    I32(4, 32),
    /** 32-bit unsigned integers. */
    U32(4, 32),
    /** 64-bit signed integers. */
    I64(8, 64),
    /** 64-bit unsigned integers. */
    U64(8, 64),
    /** 32-bit floating-point. */
    F32(4, 32),
    /** 64-bit floating-point. */
    F64(8, 64);

    private final int byteSize;
    private final int bitSize;

    LaneType(final int byteSize, final int bitSize) {
      this.byteSize = byteSize;
      this.bitSize = bitSize;
    }

    /**
     * Gets the byte size of this lane type.
     *
     * @return byte size
     */
    public int getByteSize() {
      return byteSize;
    }

    /**
     * Gets the bit size of this lane type.
     *
     * @return bit size
     */
    public int getBitSize() {
      return bitSize;
    }

    /**
     * Calculates the number of lanes for this type in a given vector width.
     *
     * @param vectorWidth the vector width
     * @return number of lanes
     */
    public int getLaneCount(final VectorWidth vectorWidth) {
      return vectorWidth.getByteWidth() / this.byteSize;
    }
  }

  /** Abstract base class for SIMD vector values. */
  public abstract static class Vector {
    protected final byte[] data;
    protected final VectorWidth width;

    /**
     * Creates a new vector with the specified data and width.
     *
     * @param data the vector data
     * @param width the vector width
     * @throws IllegalArgumentException if data is null or incorrect size
     */
    protected Vector(final byte[] data, final VectorWidth width) {
      if (data == null) {
        throw new IllegalArgumentException("Vector data cannot be null");
      }
      if (data.length != width.getByteWidth()) {
        throw new IllegalArgumentException(
            String.format(
                "Vector data must be %d bytes, got %d", width.getByteWidth(), data.length));
      }
      this.data = data.clone();
      this.width = width;
    }

    /**
     * Gets the raw byte data of this vector.
     *
     * @return a copy of the vector data
     */
    public final byte[] getData() {
      return data.clone();
    }

    /**
     * Gets the vector width.
     *
     * @return the vector width
     */
    public final VectorWidth getWidth() {
      return width;
    }

    /**
     * Gets the vector data as integers for the specified lane type.
     *
     * @param laneType the lane type to interpret as
     * @return array of integers
     * @throws IllegalArgumentException if lane type is not supported for integers
     */
    public final int[] getAsInts(final LaneType laneType) {
      final int laneCount = laneType.getLaneCount(width);
      final int[] result = new int[laneCount];
      final ByteBuffer buffer = ByteBuffer.wrap(data);
      buffer.order(ByteOrder.LITTLE_ENDIAN);

      switch (laneType) {
        case I8:
          for (int i = 0; i < laneCount; i++) {
            result[i] = buffer.get(i);
          }
          break;
        case U8:
          for (int i = 0; i < laneCount; i++) {
            result[i] = Byte.toUnsignedInt(buffer.get(i));
          }
          break;
        case I16:
          for (int i = 0; i < laneCount; i++) {
            result[i] = buffer.getShort(i * 2);
          }
          break;
        case U16:
          for (int i = 0; i < laneCount; i++) {
            result[i] = Short.toUnsignedInt(buffer.getShort(i * 2));
          }
          break;
        case I32:
        case U32:
          for (int i = 0; i < laneCount; i++) {
            result[i] = buffer.getInt(i * 4);
          }
          break;
        default:
          throw new IllegalArgumentException(
              "Lane type " + laneType + " is not supported for integers");
      }

      return result;
    }

    /**
     * Gets the vector data as longs for the specified lane type.
     *
     * @param laneType the lane type to interpret as
     * @return array of longs
     * @throws IllegalArgumentException if lane type is not supported for longs
     */
    public final long[] getAsLongs(final LaneType laneType) {
      final int laneCount = laneType.getLaneCount(width);
      final long[] result = new long[laneCount];
      final ByteBuffer buffer = ByteBuffer.wrap(data);
      buffer.order(ByteOrder.LITTLE_ENDIAN);

      switch (laneType) {
        case I64:
        case U64:
          for (int i = 0; i < laneCount; i++) {
            result[i] = buffer.getLong(i * 8);
          }
          break;
        default:
          throw new IllegalArgumentException(
              "Lane type " + laneType + " is not supported for longs");
      }

      return result;
    }

    /**
     * Gets the vector data as floats for the specified lane type.
     *
     * @param laneType the lane type to interpret as
     * @return array of floats
     * @throws IllegalArgumentException if lane type is not supported for floats
     */
    public final float[] getAsFloats(final LaneType laneType) {
      if (laneType != LaneType.F32) {
        throw new IllegalArgumentException(
            "Lane type " + laneType + " is not supported for floats");
      }

      final int laneCount = laneType.getLaneCount(width);
      final float[] result = new float[laneCount];
      final ByteBuffer buffer = ByteBuffer.wrap(data);
      buffer.order(ByteOrder.LITTLE_ENDIAN);

      for (int i = 0; i < laneCount; i++) {
        result[i] = buffer.getFloat(i * 4);
      }

      return result;
    }

    /**
     * Gets the vector data as doubles for the specified lane type.
     *
     * @param laneType the lane type to interpret as
     * @return array of doubles
     * @throws IllegalArgumentException if lane type is not supported for doubles
     */
    public final double[] getAsDoubles(final LaneType laneType) {
      if (laneType != LaneType.F64) {
        throw new IllegalArgumentException(
            "Lane type " + laneType + " is not supported for doubles");
      }

      final int laneCount = laneType.getLaneCount(width);
      final double[] result = new double[laneCount];
      final ByteBuffer buffer = ByteBuffer.wrap(data);
      buffer.order(ByteOrder.LITTLE_ENDIAN);

      for (int i = 0; i < laneCount; i++) {
        result[i] = buffer.getDouble(i * 8);
      }

      return result;
    }

    @Override
    public final boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final Vector vector = (Vector) obj;
      return width == vector.width && Arrays.equals(data, vector.data);
    }

    @Override
    public final int hashCode() {
      return Objects.hash(width, Arrays.hashCode(data));
    }

    @Override
    public final String toString() {
      return String.format(
          "%s{width=%s, data=%s}", getClass().getSimpleName(), width, Arrays.toString(data));
    }
  }

  /** 128-bit SIMD vector (v128) - Standard WebAssembly SIMD. */
  public static final class V128 extends Vector implements SimdOperations.V128 {

    /**
     * Creates a new V128 vector from byte array.
     *
     * @param data the 16-byte array
     * @throws IllegalArgumentException if data is null or not 16 bytes
     */
    public V128(final byte[] data) {
      super(data, VectorWidth.V128);
    }

    @Override
    public byte[] toByteArray() {
      return getData();
    }

    @Override
    public int[] toIntArray() {
      return getAsInts(LaneType.I32);
    }

    @Override
    public float[] toFloatArray() {
      return getAsFloats(LaneType.F32);
    }

    @Override
    public long[] toLongArray() {
      return getAsLongs(LaneType.I64);
    }

    @Override
    public double[] toDoubleArray() {
      return getAsDoubles(LaneType.F64);
    }

    /**
     * Creates a new V128 vector with all bytes set to zero.
     *
     * @return a zero-initialized V128 vector
     */
    public static V128 zero() {
      return new V128(new byte[16]);
    }

    /**
     * Creates a new V128 vector with all bytes set to the given value.
     *
     * @param value the byte value to fill with
     * @return a V128 vector filled with the given value
     */
    public static V128 splat(final byte value) {
      final byte[] data = new byte[16];
      Arrays.fill(data, value);
      return new V128(data);
    }

    /**
     * Creates a V128 vector from 4 32-bit integers.
     *
     * @param i0 first integer
     * @param i1 second integer
     * @param i2 third integer
     * @param i3 fourth integer
     * @return the V128 vector
     */
    public static V128 fromInts(final int i0, final int i1, final int i2, final int i3) {
      final ByteBuffer buffer = ByteBuffer.allocate(16);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(i0).putInt(i1).putInt(i2).putInt(i3);
      return new V128(buffer.array());
    }

    /**
     * Creates a V128 vector from 4 32-bit floats.
     *
     * @param f0 first float
     * @param f1 second float
     * @param f2 third float
     * @param f3 fourth float
     * @return the V128 vector
     */
    public static V128 fromFloats(final float f0, final float f1, final float f2, final float f3) {
      final ByteBuffer buffer = ByteBuffer.allocate(16);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      buffer.putFloat(f0).putFloat(f1).putFloat(f2).putFloat(f3);
      return new V128(buffer.array());
    }

    /**
     * Creates a V128 vector from 2 64-bit longs.
     *
     * @param l0 first long
     * @param l1 second long
     * @return the V128 vector
     */
    public static V128 fromLongs(final long l0, final long l1) {
      final ByteBuffer buffer = ByteBuffer.allocate(16);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      buffer.putLong(l0).putLong(l1);
      return new V128(buffer.array());
    }

    /**
     * Creates a V128 vector from 2 64-bit doubles.
     *
     * @param d0 first double
     * @param d1 second double
     * @return the V128 vector
     */
    public static V128 fromDoubles(final double d0, final double d1) {
      final ByteBuffer buffer = ByteBuffer.allocate(16);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      buffer.putDouble(d0).putDouble(d1);
      return new V128(buffer.array());
    }
  }

  /** 256-bit SIMD vector (v256) - Extended SIMD for AVX support. */
  public static final class V256 extends Vector {

    /**
     * Creates a new V256 vector from byte array.
     *
     * @param data the 32-byte array
     * @throws IllegalArgumentException if data is null or not 32 bytes
     */
    public V256(final byte[] data) {
      super(data, VectorWidth.V256);
    }

    /**
     * Creates a new V256 vector with all bytes set to zero.
     *
     * @return a zero-initialized V256 vector
     */
    public static V256 zero() {
      return new V256(new byte[32]);
    }

    /**
     * Creates a new V256 vector with all bytes set to the given value.
     *
     * @param value the byte value to fill with
     * @return a V256 vector filled with the given value
     */
    public static V256 splat(final byte value) {
      final byte[] data = new byte[32];
      Arrays.fill(data, value);
      return new V256(data);
    }

    /**
     * Creates a V256 vector from 8 32-bit integers.
     *
     * @param values array of 8 integers
     * @return the V256 vector
     * @throws IllegalArgumentException if values array is not length 8
     */
    public static V256 fromInts(final int[] values) {
      if (values.length != 8) {
        throw new IllegalArgumentException(
            "V256 requires exactly 8 integers, got " + values.length);
      }
      final ByteBuffer buffer = ByteBuffer.allocate(32);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      for (final int value : values) {
        buffer.putInt(value);
      }
      return new V256(buffer.array());
    }

    /**
     * Creates a V256 vector from 8 32-bit floats.
     *
     * @param values array of 8 floats
     * @return the V256 vector
     * @throws IllegalArgumentException if values array is not length 8
     */
    public static V256 fromFloats(final float[] values) {
      if (values.length != 8) {
        throw new IllegalArgumentException("V256 requires exactly 8 floats, got " + values.length);
      }
      final ByteBuffer buffer = ByteBuffer.allocate(32);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      for (final float value : values) {
        buffer.putFloat(value);
      }
      return new V256(buffer.array());
    }

    /**
     * Creates a V256 vector from 4 64-bit doubles.
     *
     * @param values array of 4 doubles
     * @return the V256 vector
     * @throws IllegalArgumentException if values array is not length 4
     */
    public static V256 fromDoubles(final double[] values) {
      if (values.length != 4) {
        throw new IllegalArgumentException("V256 requires exactly 4 doubles, got " + values.length);
      }
      final ByteBuffer buffer = ByteBuffer.allocate(32);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      for (final double value : values) {
        buffer.putDouble(value);
      }
      return new V256(buffer.array());
    }
  }

  /** 512-bit SIMD vector (v512) - Extended SIMD for AVX-512 support. */
  public static final class V512 extends Vector {

    /**
     * Creates a new V512 vector from byte array.
     *
     * @param data the 64-byte array
     * @throws IllegalArgumentException if data is null or not 64 bytes
     */
    public V512(final byte[] data) {
      super(data, VectorWidth.V512);
    }

    /**
     * Creates a new V512 vector with all bytes set to zero.
     *
     * @return a zero-initialized V512 vector
     */
    public static V512 zero() {
      return new V512(new byte[64]);
    }

    /**
     * Creates a new V512 vector with all bytes set to the given value.
     *
     * @param value the byte value to fill with
     * @return a V512 vector filled with the given value
     */
    public static V512 splat(final byte value) {
      final byte[] data = new byte[64];
      Arrays.fill(data, value);
      return new V512(data);
    }

    /**
     * Creates a V512 vector from 16 32-bit integers.
     *
     * @param values array of 16 integers
     * @return the V512 vector
     * @throws IllegalArgumentException if values array is not length 16
     */
    public static V512 fromInts(final int[] values) {
      if (values.length != 16) {
        throw new IllegalArgumentException(
            "V512 requires exactly 16 integers, got " + values.length);
      }
      final ByteBuffer buffer = ByteBuffer.allocate(64);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      for (final int value : values) {
        buffer.putInt(value);
      }
      return new V512(buffer.array());
    }

    /**
     * Creates a V512 vector from 16 32-bit floats.
     *
     * @param values array of 16 floats
     * @return the V512 vector
     * @throws IllegalArgumentException if values array is not length 16
     */
    public static V512 fromFloats(final float[] values) {
      if (values.length != 16) {
        throw new IllegalArgumentException("V512 requires exactly 16 floats, got " + values.length);
      }
      final ByteBuffer buffer = ByteBuffer.allocate(64);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      for (final float value : values) {
        buffer.putFloat(value);
      }
      return new V512(buffer.array());
    }

    /**
     * Creates a V512 vector from 8 64-bit doubles.
     *
     * @param values array of 8 doubles
     * @return the V512 vector
     * @throws IllegalArgumentException if values array is not length 8
     */
    public static V512 fromDoubles(final double[] values) {
      if (values.length != 8) {
        throw new IllegalArgumentException("V512 requires exactly 8 doubles, got " + values.length);
      }
      final ByteBuffer buffer = ByteBuffer.allocate(64);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      for (final double value : values) {
        buffer.putDouble(value);
      }
      return new V512(buffer.array());
    }
  }

  /** Platform capabilities for SIMD operations. */
  public static final class PlatformCapabilities {
    private final boolean hasSSE41;
    private final boolean hasAVX;
    private final boolean hasAVX2;
    private final boolean hasAVX512F;
    private final boolean hasAVX512BW;
    private final boolean hasFMA;
    private final boolean hasNeon;
    private final boolean hasSVE;
    private final int maxVectorWidth;

    private PlatformCapabilities(final Builder builder) {
      this.hasSSE41 = builder.hasSSE41;
      this.hasAVX = builder.hasAVX;
      this.hasAVX2 = builder.hasAVX2;
      this.hasAVX512F = builder.hasAVX512F;
      this.hasAVX512BW = builder.hasAVX512BW;
      this.hasFMA = builder.hasFMA;
      this.hasNeon = builder.hasNeon;
      this.hasSVE = builder.hasSVE;
      this.maxVectorWidth = builder.maxVectorWidth;
    }

    // Getters
    public boolean hasSSE41() {
      return hasSSE41;
    }

    public boolean hasAVX() {
      return hasAVX;
    }

    public boolean hasAVX2() {
      return hasAVX2;
    }

    public boolean hasAVX512F() {
      return hasAVX512F;
    }

    public boolean hasAVX512BW() {
      return hasAVX512BW;
    }

    public boolean hasFMA() {
      return hasFMA;
    }

    public boolean hasNeon() {
      return hasNeon;
    }

    public boolean hasSVE() {
      return hasSVE;
    }

    public int getMaxVectorWidth() {
      return maxVectorWidth;
    }

    /**
     * Gets the maximum supported vector width enum.
     *
     * @return the maximum vector width
     */
    public VectorWidth getMaxVectorWidthEnum() {
      if (maxVectorWidth >= 512) {
        return VectorWidth.V512;
      } else if (maxVectorWidth >= 256) {
        return VectorWidth.V256;
      } else {
        return VectorWidth.V128;
      }
    }

    /**
     * Checks if the specified vector width is supported.
     *
     * @param width the vector width to check
     * @return true if supported
     */
    public boolean isVectorWidthSupported(final VectorWidth width) {
      return width.getBitWidth() <= maxVectorWidth;
    }

    public static Builder builder() {
      return new Builder();
    }

    /** Builder for SIMD capabilities configuration. */
    public static final class Builder {
      private boolean hasSSE41 = false;
      private boolean hasAVX = false;
      private boolean hasAVX2 = false;
      private boolean hasAVX512F = false;
      private boolean hasAVX512BW = false;
      private boolean hasFMA = false;
      private boolean hasNeon = false;
      private boolean hasSVE = false;
      private int maxVectorWidth = 128;

      private Builder() {}

      public Builder hasSSE41(final boolean has) {
        this.hasSSE41 = has;
        return this;
      }

      public Builder hasAVX(final boolean has) {
        this.hasAVX = has;
        return this;
      }

      public Builder hasAVX2(final boolean has) {
        this.hasAVX2 = has;
        return this;
      }

      public Builder hasAVX512F(final boolean has) {
        this.hasAVX512F = has;
        return this;
      }

      public Builder hasAVX512BW(final boolean has) {
        this.hasAVX512BW = has;
        return this;
      }

      public Builder hasFMA(final boolean has) {
        this.hasFMA = has;
        return this;
      }

      public Builder hasNeon(final boolean has) {
        this.hasNeon = has;
        return this;
      }

      public Builder hasSVE(final boolean has) {
        this.hasSVE = has;
        return this;
      }

      public Builder maxVectorWidth(final int width) {
        this.maxVectorWidth = width;
        return this;
      }

      public PlatformCapabilities build() {
        return new PlatformCapabilities(this);
      }
    }

    @Override
    public String toString() {
      return String.format(
          "PlatformCapabilities{SSE4.1=%s, AVX=%s, AVX2=%s, AVX512F=%s, AVX512BW=%s, FMA=%s,"
              + " NEON=%s, SVE=%s, maxWidth=%d}",
          hasSSE41,
          hasAVX,
          hasAVX2,
          hasAVX512F,
          hasAVX512BW,
          hasFMA,
          hasNeon,
          hasSVE,
          maxVectorWidth);
    }
  }

  private SimdValue() {
    // Utility class - prevent instantiation
  }
}
