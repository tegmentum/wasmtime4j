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

package ai.tegmentum.wasmtime4j.experimental;

import ai.tegmentum.wasmtime4j.WasmMemory;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Advanced SIMD (Single Instruction, Multiple Data) operations for WebAssembly.
 *
 * <p><strong>EXPERIMENTAL:</strong> This API is experimental and subject to change. It provides
 * access to advanced SIMD vector operations beyond the basic v128 support, including
 * platform-specific optimizations and relaxed SIMD operations.
 *
 * @since 1.0.0
 */
@ExperimentalApi(feature = ExperimentalFeatures.Feature.ADVANCED_SIMD)
public final class SimdOperations {

  /** 128-bit SIMD vector representation. */
  public static final class V128 {
    private final byte[] data;

    /**
     * Creates a new V128 vector from byte array.
     *
     * @param data the 16-byte array
     * @throws IllegalArgumentException if data is null or not 16 bytes
     */
    public V128(final byte[] data) {
      if (data == null) {
        throw new IllegalArgumentException("V128 data cannot be null");
      }
      if (data.length != 16) {
        throw new IllegalArgumentException(
            "V128 data must be exactly 16 bytes, got " + data.length);
      }
      this.data = Arrays.copyOf(data, 16);
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
      buffer.putFloat(f0).putFloat(f1).putFloat(f2).putFloat(f3);
      return new V128(buffer.array());
    }

    /**
     * Gets the raw byte data of this vector.
     *
     * @return a copy of the 16-byte array
     */
    public byte[] getData() {
      return Arrays.copyOf(data, 16);
    }

    /**
     * Gets the vector data as an array of 4 integers.
     *
     * @return array of 4 integers
     */
    public int[] getAsInts() {
      final ByteBuffer buffer = ByteBuffer.wrap(data);
      return new int[] {buffer.getInt(0), buffer.getInt(4), buffer.getInt(8), buffer.getInt(12)};
    }

    /**
     * Gets the vector data as an array of 4 floats.
     *
     * @return array of 4 floats
     */
    public float[] getAsFloats() {
      final ByteBuffer buffer = ByteBuffer.wrap(data);
      return new float[] {
        buffer.getFloat(0), buffer.getFloat(4), buffer.getFloat(8), buffer.getFloat(12)
      };
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final V128 v128 = (V128) obj;
      return Arrays.equals(data, v128.data);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
      return "V128{data=" + Arrays.toString(data) + '}';
    }
  }

  /** SIMD operation configuration. */
  public static final class SimdConfig {
    private final boolean enablePlatformOptimizations;
    private final boolean enableRelaxedOperations;
    private final boolean validateVectorOperands;
    private final int maxVectorWidth;

    private SimdConfig(final Builder builder) {
      this.enablePlatformOptimizations = builder.enablePlatformOptimizations;
      this.enableRelaxedOperations = builder.enableRelaxedOperations;
      this.validateVectorOperands = builder.validateVectorOperands;
      this.maxVectorWidth = builder.maxVectorWidth;
    }

    /**
     * Checks if platform optimizations are enabled.
     *
     * @return true if platform optimizations are enabled
     */
    public boolean isPlatformOptimizationsEnabled() {
      return enablePlatformOptimizations;
    }

    /**
     * Checks if relaxed operations are enabled.
     *
     * @return true if relaxed operations are enabled
     */
    public boolean isRelaxedOperationsEnabled() {
      return enableRelaxedOperations;
    }

    /**
     * Checks if vector operand validation is enabled.
     *
     * @return true if validation is enabled
     */
    public boolean isVectorOperandValidationEnabled() {
      return validateVectorOperands;
    }

    /**
     * Gets the maximum vector width.
     *
     * @return the maximum vector width in bits
     */
    public int getMaxVectorWidth() {
      return maxVectorWidth;
    }

    /**
     * Creates a new builder for SIMD configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
      return new Builder();
    }

    /** Builder for SIMD configuration. */
    public static final class Builder {
      private boolean enablePlatformOptimizations = true;
      private boolean enableRelaxedOperations = false;
      private boolean validateVectorOperands = true;
      private int maxVectorWidth = 128;

      private Builder() {}

      /**
       * Enables or disables platform-specific optimizations.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder enablePlatformOptimizations(final boolean enable) {
        this.enablePlatformOptimizations = enable;
        return this;
      }

      /**
       * Enables or disables relaxed SIMD operations.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder enableRelaxedOperations(final boolean enable) {
        this.enableRelaxedOperations = enable;
        return this;
      }

      /**
       * Enables or disables vector operand validation.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder validateVectorOperands(final boolean enable) {
        this.validateVectorOperands = enable;
        return this;
      }

      /**
       * Sets the maximum vector width.
       *
       * @param width the maximum vector width in bits (must be positive and multiple of 128)
       * @return this builder
       * @throws IllegalArgumentException if width is invalid
       */
      public Builder maxVectorWidth(final int width) {
        if (width <= 0 || width % 128 != 0) {
          throw new IllegalArgumentException(
              "Max vector width must be positive and multiple of 128");
        }
        this.maxVectorWidth = width;
        return this;
      }

      /**
       * Builds the SIMD configuration.
       *
       * @return the configuration
       */
      public SimdConfig build() {
        return new SimdConfig(this);
      }
    }
  }

  private final SimdConfig config;
  private final long nativeHandle;

  /**
   * Creates a new SIMD operations handler.
   *
   * @param config the SIMD configuration
   * @throws IllegalArgumentException if config is null
   * @throws UnsupportedOperationException if advanced SIMD feature is not enabled
   */
  public SimdOperations(final SimdConfig config) {
    ExperimentalFeatures.validateFeatureSupport(ExperimentalFeatures.Feature.ADVANCED_SIMD);

    if (config == null) {
      throw new IllegalArgumentException("SIMD config cannot be null");
    }

    this.config = config;
    this.nativeHandle = createNativeSimdHandler(config);
  }

  /**
   * Loads a V128 vector from memory.
   *
   * @param memory the WebAssembly memory
   * @param offset the byte offset in memory
   * @return the loaded V128 vector
   * @throws IllegalArgumentException if memory is null or offset is invalid
   */
  public V128 load(final WasmMemory memory, final int offset) {
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("Memory offset cannot be negative");
    }

    // For experimental implementation, use memory buffer directly
    final byte[] data = new byte[16];
    memory.readBytes(offset, data, 0, 16);
    return new V128(data);
  }

  /**
   * Stores a V128 vector to memory.
   *
   * @param memory the WebAssembly memory
   * @param offset the byte offset in memory
   * @param vector the vector to store
   * @throws IllegalArgumentException if memory or vector is null, or offset is invalid
   */
  public void store(final WasmMemory memory, final int offset, final V128 vector) {
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }
    if (vector == null) {
      throw new IllegalArgumentException("Vector cannot be null");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("Memory offset cannot be negative");
    }

    // For experimental implementation, use memory buffer directly
    memory.writeBytes(offset, vector.getData(), 0, 16);
  }

  /**
   * Performs vector addition on two V128 vectors.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 add(final V128 a, final V128 b) {
    validateVectorOperands(a, b);
    final byte[] result = addNativeV128(nativeHandle, a.getData(), b.getData());
    return new V128(result);
  }

  /**
   * Performs vector subtraction on two V128 vectors.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 subtract(final V128 a, final V128 b) {
    validateVectorOperands(a, b);
    final byte[] result = subtractNativeV128(nativeHandle, a.getData(), b.getData());
    return new V128(result);
  }

  /**
   * Performs vector multiplication on two V128 vectors.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 multiply(final V128 a, final V128 b) {
    validateVectorOperands(a, b);
    final byte[] result = multiplyNativeV128(nativeHandle, a.getData(), b.getData());
    return new V128(result);
  }

  /**
   * Performs vector shuffle operation.
   *
   * @param a the first vector
   * @param b the second vector
   * @param indices the shuffle indices (16 bytes)
   * @return the shuffled vector
   * @throws IllegalArgumentException if any parameter is null or indices is not 16 bytes
   */
  public V128 shuffle(final V128 a, final V128 b, final byte[] indices) {
    validateVectorOperands(a, b);
    if (indices == null) {
      throw new IllegalArgumentException("Shuffle indices cannot be null");
    }
    if (indices.length != 16) {
      throw new IllegalArgumentException("Shuffle indices must be exactly 16 bytes");
    }

    final byte[] result = shuffleNativeV128(nativeHandle, a.getData(), b.getData(), indices);
    return new V128(result);
  }

  /**
   * Performs relaxed floating-point vector addition (if enabled).
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws IllegalArgumentException if either vector is null
   * @throws UnsupportedOperationException if relaxed operations are not enabled
   */
  public V128 relaxedAdd(final V128 a, final V128 b) {
    if (!config.isRelaxedOperationsEnabled()) {
      throw new UnsupportedOperationException("Relaxed SIMD operations are not enabled");
    }

    validateVectorOperands(a, b);
    final byte[] result = relaxedAddNativeV128(nativeHandle, a.getData(), b.getData());
    return new V128(result);
  }

  /**
   * Validates vector operands if validation is enabled.
   *
   * @param vectors the vectors to validate
   * @throws IllegalArgumentException if any vector is null and validation is enabled
   */
  private void validateVectorOperands(final V128... vectors) {
    if (config.isVectorOperandValidationEnabled()) {
      for (final V128 vector : vectors) {
        if (vector == null) {
          throw new IllegalArgumentException("Vector operand cannot be null");
        }
      }
    }
  }

  /**
   * Gets the SIMD configuration.
   *
   * @return the configuration
   */
  public SimdConfig getConfig() {
    return config;
  }

  /**
   * Gets the native handle for this SIMD operations handler.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  /** Closes this SIMD operations handler and releases native resources. */
  public void close() {
    if (nativeHandle != 0) {
      closeNativeSimdHandler(nativeHandle);
    }
  }

  // Native method declarations - implementations in wasmtime4j-native
  private static native long createNativeSimdHandler(SimdConfig config);

  private static native byte[] addNativeV128(long handlerHandle, byte[] a, byte[] b);

  private static native byte[] subtractNativeV128(long handlerHandle, byte[] a, byte[] b);

  private static native byte[] multiplyNativeV128(long handlerHandle, byte[] a, byte[] b);

  private static native byte[] shuffleNativeV128(
      long handlerHandle, byte[] a, byte[] b, byte[] indices);

  private static native byte[] relaxedAddNativeV128(long handlerHandle, byte[] a, byte[] b);

  private static native void closeNativeSimdHandler(long handle);

  @Override
  protected void finalize() throws Throwable {
    try {
      close();
    } finally {
      super.finalize();
    }
  }
}
