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

import ai.tegmentum.wasmtime4j.exception.WasmtimeException;

/**
 * SIMD (Single Instruction, Multiple Data) operations for WebAssembly v128 vectors.
 *
 * <p>This class provides comprehensive support for WebAssembly SIMD operations including:
 *
 * <ul>
 *   <li>Arithmetic operations (add, subtract, multiply, divide)
 *   <li>Logical operations (and, or, xor, not)
 *   <li>Comparison operations (equals, less than, greater than)
 *   <li>Memory operations (load, store with alignment support)
 *   <li>Conversion operations between different data types
 *   <li>Lane manipulation (extract, replace, splat)
 *   <li>Vector shuffle operations
 * </ul>
 *
 * <p>All operations are performed on 128-bit vectors (v128) and support both integer and
 * floating-point data types. The implementation automatically uses platform-specific optimizations
 * (SSE, AVX) when available and falls back to scalar operations when needed.
 *
 * @since 1.0.0
 */
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
      this.data = data.clone();
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
      java.util.Arrays.fill(data, value);
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
      final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(16);
      buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
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
      final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(16);
      buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
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
      final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(16);
      buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
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
      final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(16);
      buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
      buffer.putDouble(d0).putDouble(d1);
      return new V128(buffer.array());
    }

    /**
     * Gets the raw byte data of this vector.
     *
     * @return a copy of the 16-byte array
     */
    public byte[] getData() {
      return data.clone();
    }

    /**
     * Gets the vector data as an array of 4 integers.
     *
     * @return array of 4 integers
     */
    public int[] getAsInts() {
      final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(data);
      buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
      return new int[] {buffer.getInt(0), buffer.getInt(4), buffer.getInt(8), buffer.getInt(12)};
    }

    /**
     * Gets the vector data as an array of 4 floats.
     *
     * @return array of 4 floats
     */
    public float[] getAsFloats() {
      final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(data);
      buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
      return new float[] {
        buffer.getFloat(0), buffer.getFloat(4), buffer.getFloat(8), buffer.getFloat(12)
      };
    }

    /**
     * Gets the vector data as an array of 2 longs.
     *
     * @return array of 2 longs
     */
    public long[] getAsLongs() {
      final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(data);
      buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
      return new long[] {buffer.getLong(0), buffer.getLong(8)};
    }

    /**
     * Gets the vector data as an array of 2 doubles.
     *
     * @return array of 2 doubles
     */
    public double[] getAsDoubles() {
      final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(data);
      buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
      return new double[] {buffer.getDouble(0), buffer.getDouble(8)};
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
      return java.util.Arrays.equals(data, v128.data);
    }

    @Override
    public int hashCode() {
      return java.util.Arrays.hashCode(data);
    }

    @Override
    public String toString() {
      return "V128{data=" + java.util.Arrays.toString(data) + '}';
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

    /**
     * Creates a default SIMD configuration.
     *
     * @return default configuration
     */
    public static SimdConfig defaultConfig() {
      return builder().build();
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

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final SimdConfig that = (SimdConfig) obj;
      return enablePlatformOptimizations == that.enablePlatformOptimizations
          && enableRelaxedOperations == that.enableRelaxedOperations
          && validateVectorOperands == that.validateVectorOperands
          && maxVectorWidth == that.maxVectorWidth;
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(
          enablePlatformOptimizations,
          enableRelaxedOperations,
          validateVectorOperands,
          maxVectorWidth);
    }

    @Override
    public String toString() {
      return String.format(
          "SimdConfig{platformOptimizations=%s, relaxedOperations=%s, validation=%s, maxWidth=%d}",
          enablePlatformOptimizations,
          enableRelaxedOperations,
          validateVectorOperands,
          maxVectorWidth);
    }
  }

  private final SimdConfig config;
  private final WasmRuntime runtime;

  /**
   * Creates a new SIMD operations handler.
   *
   * @param config the SIMD configuration
   * @param runtime the WebAssembly runtime
   * @throws IllegalArgumentException if config or runtime is null
   */
  public SimdOperations(final SimdConfig config, final WasmRuntime runtime) {
    if (config == null) {
      throw new IllegalArgumentException("SIMD config cannot be null");
    }
    if (runtime == null) {
      throw new IllegalArgumentException("WebAssembly runtime cannot be null");
    }

    this.config = config;
    this.runtime = runtime;
  }

  /**
   * Creates a new SIMD operations handler with default configuration.
   *
   * @param runtime the WebAssembly runtime
   * @return new SIMD operations handler
   * @throws IllegalArgumentException if runtime is null
   */
  public static SimdOperations create(final WasmRuntime runtime) {
    return new SimdOperations(SimdConfig.defaultConfig(), runtime);
  }

  // ===== ARITHMETIC OPERATIONS =====

  /**
   * Performs vector addition on two V128 vectors.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 add(final V128 a, final V128 b) throws WasmtimeException {
    validateVectorOperands(a, b);
    return runtime.simdAdd(a, b);
  }

  /**
   * Performs vector subtraction on two V128 vectors.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 subtract(final V128 a, final V128 b) throws WasmtimeException {
    validateVectorOperands(a, b);
    return runtime.simdSubtract(a, b);
  }

  /**
   * Performs vector multiplication on two V128 vectors.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 multiply(final V128 a, final V128 b) throws WasmtimeException {
    validateVectorOperands(a, b);
    return runtime.simdMultiply(a, b);
  }

  /**
   * Performs vector division on two V128 vectors.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 divide(final V128 a, final V128 b) throws WasmtimeException {
    validateVectorOperands(a, b);
    return runtime.simdDivide(a, b);
  }

  /**
   * Performs saturated vector addition (prevents overflow).
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 addSaturated(final V128 a, final V128 b) throws WasmtimeException {
    validateVectorOperands(a, b);
    return runtime.simdAddSaturated(a, b);
  }

  // ===== LOGICAL OPERATIONS =====

  /**
   * Performs vector bitwise AND operation.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 and(final V128 a, final V128 b) throws WasmtimeException {
    validateVectorOperands(a, b);
    return runtime.simdAnd(a, b);
  }

  /**
   * Performs vector bitwise OR operation.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 or(final V128 a, final V128 b) throws WasmtimeException {
    validateVectorOperands(a, b);
    return runtime.simdOr(a, b);
  }

  /**
   * Performs vector bitwise XOR operation.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 xor(final V128 a, final V128 b) throws WasmtimeException {
    validateVectorOperands(a, b);
    return runtime.simdXor(a, b);
  }

  /**
   * Performs vector bitwise NOT operation.
   *
   * @param a the vector to negate
   * @return the result vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if vector is null
   */
  public V128 not(final V128 a) throws WasmtimeException {
    validateVectorOperand(a);
    return runtime.simdNot(a);
  }

  // ===== COMPARISON OPERATIONS =====

  /**
   * Performs vector equality comparison.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector (lanes are all 1s if equal, all 0s if not)
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 equals(final V128 a, final V128 b) throws WasmtimeException {
    validateVectorOperands(a, b);
    return runtime.simdEquals(a, b);
  }

  /**
   * Performs vector less-than comparison.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector (lanes are all 1s if a < b, all 0s otherwise)
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 lessThan(final V128 a, final V128 b) throws WasmtimeException {
    validateVectorOperands(a, b);
    return runtime.simdLessThan(a, b);
  }

  /**
   * Performs vector greater-than comparison.
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector (lanes are all 1s if a > b, all 0s otherwise)
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 greaterThan(final V128 a, final V128 b) throws WasmtimeException {
    validateVectorOperands(a, b);
    return runtime.simdGreaterThan(a, b);
  }

  // ===== MEMORY OPERATIONS =====

  /**
   * Loads a V128 vector from memory.
   *
   * @param memory the WebAssembly memory
   * @param offset the byte offset in memory
   * @return the loaded vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if memory is null or offset is invalid
   */
  public V128 load(final WasmMemory memory, final int offset) throws WasmtimeException {
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("Memory offset cannot be negative");
    }
    return runtime.simdLoad(memory, offset);
  }

  /**
   * Loads a V128 vector from memory with alignment.
   *
   * @param memory the WebAssembly memory
   * @param offset the byte offset in memory
   * @param alignment the required alignment (must be power of 2 and <= 16)
   * @return the loaded vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  public V128 loadAligned(final WasmMemory memory, final int offset, final int alignment)
      throws WasmtimeException {
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("Memory offset cannot be negative");
    }
    validateAlignment(alignment);
    if (offset % alignment != 0) {
      throw new IllegalArgumentException(
          String.format("Memory offset %d is not aligned to %d bytes", offset, alignment));
    }
    return runtime.simdLoadAligned(memory, offset, alignment);
  }

  /**
   * Stores a V128 vector to memory.
   *
   * @param memory the WebAssembly memory
   * @param offset the byte offset in memory
   * @param vector the vector to store
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  public void store(final WasmMemory memory, final int offset, final V128 vector)
      throws WasmtimeException {
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }
    if (vector == null) {
      throw new IllegalArgumentException("Vector cannot be null");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("Memory offset cannot be negative");
    }
    runtime.simdStore(memory, offset, vector);
  }

  /**
   * Stores a V128 vector to memory with alignment.
   *
   * @param memory the WebAssembly memory
   * @param offset the byte offset in memory
   * @param vector the vector to store
   * @param alignment the required alignment (must be power of 2 and <= 16)
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  public void storeAligned(
      final WasmMemory memory, final int offset, final V128 vector, final int alignment)
      throws WasmtimeException {
    if (memory == null) {
      throw new IllegalArgumentException("Memory cannot be null");
    }
    if (vector == null) {
      throw new IllegalArgumentException("Vector cannot be null");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("Memory offset cannot be negative");
    }
    validateAlignment(alignment);
    if (offset % alignment != 0) {
      throw new IllegalArgumentException(
          String.format("Memory offset %d is not aligned to %d bytes", offset, alignment));
    }
    runtime.simdStoreAligned(memory, offset, vector, alignment);
  }

  // ===== CONVERSION OPERATIONS =====

  /**
   * Converts integer vector to float vector.
   *
   * @param vector the integer vector
   * @return the float vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if vector is null
   */
  public V128 convertI32ToF32(final V128 vector) throws WasmtimeException {
    validateVectorOperand(vector);
    return runtime.simdConvertI32ToF32(vector);
  }

  /**
   * Converts float vector to integer vector.
   *
   * @param vector the float vector
   * @return the integer vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if vector is null
   */
  public V128 convertF32ToI32(final V128 vector) throws WasmtimeException {
    validateVectorOperand(vector);
    return runtime.simdConvertF32ToI32(vector);
  }

  // ===== LANE OPERATIONS =====

  /**
   * Extracts a 32-bit integer lane from a vector.
   *
   * @param vector the vector
   * @param lane the lane index (0-3)
   * @return the extracted integer value
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  public int extractLaneI32(final V128 vector, final int lane) throws WasmtimeException {
    validateVectorOperand(vector);
    validateLaneIndex(lane, 4);
    return runtime.simdExtractLaneI32(vector, lane);
  }

  /**
   * Replaces a 32-bit integer lane in a vector.
   *
   * @param vector the vector
   * @param lane the lane index (0-3)
   * @param value the new value
   * @return the modified vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  public V128 replaceLaneI32(final V128 vector, final int lane, final int value)
      throws WasmtimeException {
    validateVectorOperand(vector);
    validateLaneIndex(lane, 4);
    return runtime.simdReplaceLaneI32(vector, lane, value);
  }

  /**
   * Creates a vector by splatting a 32-bit integer to all lanes.
   *
   * @param value the value to splat
   * @return the vector with all lanes set to the value
   * @throws WasmtimeException if the operation fails
   */
  public V128 splatI32(final int value) throws WasmtimeException {
    return runtime.simdSplatI32(value);
  }

  /**
   * Creates a vector by splatting a 32-bit float to all lanes.
   *
   * @param value the value to splat
   * @return the vector with all lanes set to the value
   * @throws WasmtimeException if the operation fails
   */
  public V128 splatF32(final float value) throws WasmtimeException {
    return runtime.simdSplatF32(value);
  }

  // ===== SHUFFLE OPERATIONS =====

  /**
   * Performs vector shuffle operation.
   *
   * @param a the first vector
   * @param b the second vector
   * @param indices the shuffle indices (16 bytes, each 0-31)
   * @return the shuffled vector
   * @throws WasmtimeException if the operation fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  public V128 shuffle(final V128 a, final V128 b, final byte[] indices) throws WasmtimeException {
    validateVectorOperands(a, b);
    if (indices == null) {
      throw new IllegalArgumentException("Shuffle indices cannot be null");
    }
    if (indices.length != 16) {
      throw new IllegalArgumentException("Shuffle indices must be exactly 16 bytes");
    }
    for (int i = 0; i < indices.length; i++) {
      if (indices[i] < 0 || indices[i] > 31) {
        throw new IllegalArgumentException(
            String.format("Shuffle index at position %d must be 0-31, got %d", i, indices[i]));
      }
    }
    return runtime.simdShuffle(a, b, indices);
  }

  // ===== ADVANCED ARITHMETIC OPERATIONS =====\n\n  /**\n   * Performs fused multiply-add operation (a * b + c).\n   *\n   * @param a the first vector\n   * @param b the second vector\n   * @param c the third vector\n   * @return the result vector\n   * @throws WasmtimeException if the operation fails\n   * @throws UnsupportedOperationException if FMA operations are not enabled\n   * @throws IllegalArgumentException if any vector is null\n   */\n  public V128 fma(final V128 a, final V128 b, final V128 c) throws WasmtimeException {\n    validateVectorOperands(a, b);\n    validateVectorOperand(c);\n    return runtime.simdFma(a, b, c);\n  }\n\n  /**\n   * Performs fused multiply-subtract operation (a * b - c).\n   *\n   * @param a the first vector\n   * @param b the second vector\n   * @param c the third vector\n   * @return the result vector\n   * @throws WasmtimeException if the operation fails\n   * @throws UnsupportedOperationException if FMA operations are not enabled\n   * @throws IllegalArgumentException if any vector is null\n   */\n  public V128 fms(final V128 a, final V128 b, final V128 c) throws WasmtimeException {\n    validateVectorOperands(a, b);\n    validateVectorOperand(c);\n    return runtime.simdFms(a, b, c);\n  }\n\n  /**\n   * Performs vector reciprocal approximation.\n   *\n   * @param a the vector\n   * @return the result vector\n   * @throws WasmtimeException if the operation fails\n   * @throws IllegalArgumentException if vector is null\n   */\n  public V128 reciprocal(final V128 a) throws WasmtimeException {\n    validateVectorOperand(a);\n    return runtime.simdReciprocal(a);\n  }\n\n  /**\n   * Performs vector square root.\n   *\n   * @param a the vector\n   * @return the result vector\n   * @throws WasmtimeException if the operation fails\n   * @throws IllegalArgumentException if vector is null\n   */\n  public V128 sqrt(final V128 a) throws WasmtimeException {\n    validateVectorOperand(a);\n    return runtime.simdSqrt(a);\n  }\n\n  /**\n   * Performs reciprocal square root approximation.\n   *\n   * @param a the vector\n   * @return the result vector\n   * @throws WasmtimeException if the operation fails\n   * @throws IllegalArgumentException if vector is null\n   */\n  public V128 rsqrt(final V128 a) throws WasmtimeException {\n    validateVectorOperand(a);\n    return runtime.simdRsqrt(a);\n  }\n\n  // ===== ADVANCED LOGICAL OPERATIONS =====\n\n  /**\n   * Performs bit population count (popcount) on vector.\n   *\n   * @param a the vector\n   * @return the result vector with popcount for each lane\n   * @throws WasmtimeException if the operation fails\n   * @throws IllegalArgumentException if vector is null\n   */\n  public V128 popcount(final V128 a) throws WasmtimeException {\n    validateVectorOperand(a);\n    return runtime.simdPopcount(a);\n  }\n\n  /**\n   * Performs variable bit shift left on vector lanes.\n   *\n   * @param a the vector to shift\n   * @param b the shift amounts vector\n   * @return the result vector\n   * @throws WasmtimeException if the operation fails\n   * @throws IllegalArgumentException if either vector is null\n   */\n  public V128 shlVariable(final V128 a, final V128 b) throws WasmtimeException {\n    validateVectorOperands(a, b);\n    return runtime.simdShlVariable(a, b);\n  }\n\n  /**\n   * Performs variable bit shift right on vector lanes.\n   *\n   * @param a the vector to shift\n   * @param b the shift amounts vector\n   * @return the result vector\n   * @throws WasmtimeException if the operation fails\n   * @throws IllegalArgumentException if either vector is null\n   */\n  public V128 shrVariable(final V128 a, final V128 b) throws WasmtimeException {\n    validateVectorOperands(a, b);\n    return runtime.simdShrVariable(a, b);\n  }\n\n  // ===== VECTOR REDUCTION OPERATIONS =====\n\n  /**\n   * Performs horizontal sum reduction on vector.\n   *\n   * @param a the vector\n   * @return the sum of all lanes as a scalar value\n   * @throws WasmtimeException if the operation fails\n   * @throws IllegalArgumentException if vector is null\n   */\n  public float horizontalSum(final V128 a) throws WasmtimeException {\n    validateVectorOperand(a);\n    return runtime.simdHorizontalSum(a);\n  }\n\n  /**\n   * Performs horizontal minimum reduction on vector.\n   *\n   * @param a the vector\n   * @return the minimum of all lanes as a scalar value\n   * @throws WasmtimeException if the operation fails\n   * @throws IllegalArgumentException if vector is null\n   */\n  public float horizontalMin(final V128 a) throws WasmtimeException {\n    validateVectorOperand(a);\n    return runtime.simdHorizontalMin(a);\n  }\n\n  /**\n   * Performs horizontal maximum reduction on vector.\n   *\n   * @param a the vector\n   * @return the maximum of all lanes as a scalar value\n   * @throws WasmtimeException if the operation fails\n   * @throws IllegalArgumentException if vector is null\n   */\n  public float horizontalMax(final V128 a) throws WasmtimeException {\n    validateVectorOperand(a);\n    return runtime.simdHorizontalMax(a);\n  }\n\n  // ===== ADVANCED COMPARISON AND SELECTION =====\n\n  /**\n   * Performs vector conditional selection based on mask.\n   *\n   * @param mask the mask vector (0 = select from b, non-zero = select from a)\n   * @param a the first source vector\n   * @param b the second source vector\n   * @return the result vector\n   * @throws WasmtimeException if the operation fails\n   * @throws IllegalArgumentException if any vector is null\n   */\n  public V128 select(final V128 mask, final V128 a, final V128 b) throws WasmtimeException {\n    validateVectorOperand(mask);\n    validateVectorOperands(a, b);\n    return runtime.simdSelect(mask, a, b);\n  }\n\n  /**\n   * Performs vector blending with immediate mask.\n   *\n   * @param a the first vector\n   * @param b the second vector\n   * @param mask the immediate mask (8 bits for 4 lanes, 2 bits per lane)\n   * @return the result vector\n   * @throws WasmtimeException if the operation fails\n   * @throws IllegalArgumentException if either vector is null\n   */\n  public V128 blend(final V128 a, final V128 b, final int mask) throws WasmtimeException {\n    validateVectorOperands(a, b);\n    if (mask < 0 || mask > 255) {\n      throw new IllegalArgumentException(\"Blend mask must be 0-255, got \" + mask);\n    }\n    return runtime.simdBlend(a, b, mask);\n  }\n\n  // ===== RELAXED OPERATIONS ====="}

  /**
   * Performs relaxed floating-point vector addition (if enabled).
   *
   * @param a the first vector
   * @param b the second vector
   * @return the result vector
   * @throws WasmtimeException if the operation fails
   * @throws UnsupportedOperationException if relaxed operations are not enabled
   * @throws IllegalArgumentException if either vector is null
   */
  public V128 relaxedAdd(final V128 a, final V128 b) throws WasmtimeException {
    if (!config.isRelaxedOperationsEnabled()) {
      throw new UnsupportedOperationException("Relaxed SIMD operations are not enabled");
    }
    validateVectorOperands(a, b);
    return runtime.simdRelaxedAdd(a, b);
  }

  // ===== UTILITY METHODS =====

  /**
   * Gets the SIMD configuration.
   *
   * @return the configuration
   */
  public SimdConfig getConfig() {
    return config;
  }

  /**
   * Gets the WebAssembly runtime.
   *
   * @return the runtime
   */
  public WasmRuntime getRuntime() {
    return runtime;
  }

  /**
   * Checks if the current platform supports SIMD operations.
   *
   * @return true if SIMD is supported
   */
  public boolean isSimdSupported() {
    return runtime.isSimdSupported();
  }

  /**
   * Gets information about the SIMD capabilities of the current platform.
   *
   * @return SIMD capability information
   */
  public String getSimdCapabilities() {
    return runtime.getSimdCapabilities();
  }

  // ===== VALIDATION METHODS =====

  private void validateVectorOperands(final V128 a, final V128 b) {
    if (config.isVectorOperandValidationEnabled()) {
      if (a == null) {
        throw new IllegalArgumentException("First vector operand cannot be null");
      }
      if (b == null) {
        throw new IllegalArgumentException("Second vector operand cannot be null");
      }
    }
  }

  private void validateVectorOperand(final V128 vector) {
    if (config.isVectorOperandValidationEnabled()) {
      if (vector == null) {
        throw new IllegalArgumentException("Vector operand cannot be null");
      }
    }
  }

  private void validateAlignment(final int alignment) {
    if (alignment <= 0 || !isPowerOfTwo(alignment) || alignment > 16) {
      throw new IllegalArgumentException(
          String.format("Invalid alignment: %d. Must be power of 2 and <= 16", alignment));
    }
  }

  private void validateLaneIndex(final int lane, final int maxLanes) {
    if (lane < 0 || lane >= maxLanes) {
      throw new IllegalArgumentException(
          String.format("Lane index %d out of bounds for %d lanes", lane, maxLanes));
    }
  }

  private static boolean isPowerOfTwo(final int n) {
    return n > 0 && (n & (n - 1)) == 0;
  }
}
