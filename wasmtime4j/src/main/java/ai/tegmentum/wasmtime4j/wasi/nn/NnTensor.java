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
package ai.tegmentum.wasmtime4j.wasi.nn;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a tensor for WASI-NN inference operations.
 *
 * <p>A tensor contains multi-dimensional numerical data with a specific data type. Tensors are used
 * as inputs and outputs for neural network inference operations.
 *
 * <p>Data is stored in row-major order (C-style) as per the WASI-NN specification.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a 1x3x224x224 FP32 tensor for image input
 * int[] shape = {1, 3, 224, 224};
 * float[] imageData = loadImageData(); // 150528 floats
 * NnTensor input = NnTensor.fromFloatArray(shape, imageData);
 *
 * // Or create with a name for named tensor operations
 * NnTensor namedInput = NnTensor.fromFloatArray("image_input", shape, imageData);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class NnTensor {

  private final String name;
  private final int[] dimensions;
  private final NnTensorType type;
  private final byte[] data;

  private NnTensor(
      final String name, final int[] dimensions, final NnTensorType type, final byte[] data) {
    this.name = name;
    this.dimensions = dimensions.clone();
    this.type = type;
    this.data = data.clone();
  }

  /**
   * Creates a tensor from raw byte data.
   *
   * @param dimensions the tensor shape
   * @param type the element data type
   * @param data the raw byte data in row-major order
   * @return a new tensor
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if dimensions are invalid or data size doesn't match
   */
  public static NnTensor fromBytes(
      final int[] dimensions, final NnTensorType type, final byte[] data) {
    return fromBytes(null, dimensions, type, data);
  }

  /**
   * Creates a named tensor from raw byte data.
   *
   * @param name the tensor name (for named tensor operations)
   * @param dimensions the tensor shape
   * @param type the element data type
   * @param data the raw byte data in row-major order
   * @return a new tensor
   * @throws NullPointerException if dimensions, type, or data is null
   * @throws IllegalArgumentException if dimensions are invalid or data size doesn't match
   */
  public static NnTensor fromBytes(
      final String name, final int[] dimensions, final NnTensorType type, final byte[] data) {
    Objects.requireNonNull(dimensions, "dimensions cannot be null");
    Objects.requireNonNull(type, "type cannot be null");
    Objects.requireNonNull(data, "data cannot be null");

    validateDimensions(dimensions);
    final long expectedSize = type.calculateByteSize(dimensions);
    if (data.length != expectedSize) {
      throw new IllegalArgumentException(
          "Data size mismatch: expected " + expectedSize + " bytes, got " + data.length);
    }

    return new NnTensor(name, dimensions, type, data);
  }

  /**
   * Creates a tensor from a float array.
   *
   * @param dimensions the tensor shape
   * @param data the float data in row-major order
   * @return a new FP32 tensor
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if dimensions are invalid or data size doesn't match
   */
  public static NnTensor fromFloatArray(final int[] dimensions, final float[] data) {
    return fromFloatArray(null, dimensions, data);
  }

  /**
   * Creates a named tensor from a float array.
   *
   * @param name the tensor name (for named tensor operations)
   * @param dimensions the tensor shape
   * @param data the float data in row-major order
   * @return a new FP32 tensor
   * @throws NullPointerException if dimensions or data is null
   * @throws IllegalArgumentException if dimensions are invalid or data size doesn't match
   */
  public static NnTensor fromFloatArray(
      final String name, final int[] dimensions, final float[] data) {
    Objects.requireNonNull(dimensions, "dimensions cannot be null");
    Objects.requireNonNull(data, "data cannot be null");

    validateDimensions(dimensions);
    final long expectedElements = calculateElementCount(dimensions);
    if (data.length != expectedElements) {
      throw new IllegalArgumentException(
          "Data size mismatch: expected " + expectedElements + " elements, got " + data.length);
    }

    final ByteBuffer buffer = ByteBuffer.allocate(data.length * 4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.asFloatBuffer().put(data);

    return new NnTensor(name, dimensions, NnTensorType.FP32, buffer.array());
  }

  /**
   * Creates a tensor from a byte array (U8 type).
   *
   * @param dimensions the tensor shape
   * @param data the byte data in row-major order
   * @return a new U8 tensor
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if dimensions are invalid or data size doesn't match
   */
  public static NnTensor fromByteArray(final int[] dimensions, final byte[] data) {
    return fromByteArray(null, dimensions, data);
  }

  /**
   * Creates a named tensor from a byte array (U8 type).
   *
   * @param name the tensor name (for named tensor operations)
   * @param dimensions the tensor shape
   * @param data the byte data in row-major order
   * @return a new U8 tensor
   * @throws NullPointerException if dimensions or data is null
   * @throws IllegalArgumentException if dimensions are invalid or data size doesn't match
   */
  public static NnTensor fromByteArray(
      final String name, final int[] dimensions, final byte[] data) {
    Objects.requireNonNull(dimensions, "dimensions cannot be null");
    Objects.requireNonNull(data, "data cannot be null");

    validateDimensions(dimensions);
    final long expectedElements = calculateElementCount(dimensions);
    if (data.length != expectedElements) {
      throw new IllegalArgumentException(
          "Data size mismatch: expected " + expectedElements + " elements, got " + data.length);
    }

    return new NnTensor(name, dimensions, NnTensorType.U8, data);
  }

  /**
   * Creates a tensor from a 32-bit integer array.
   *
   * @param dimensions the tensor shape
   * @param data the int data in row-major order
   * @return a new I32 tensor
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if dimensions are invalid or data size doesn't match
   */
  public static NnTensor fromIntArray(final int[] dimensions, final int[] data) {
    return fromIntArray(null, dimensions, data);
  }

  /**
   * Creates a named tensor from a 32-bit integer array.
   *
   * @param name the tensor name (for named tensor operations)
   * @param dimensions the tensor shape
   * @param data the int data in row-major order
   * @return a new I32 tensor
   * @throws NullPointerException if dimensions or data is null
   * @throws IllegalArgumentException if dimensions are invalid or data size doesn't match
   */
  public static NnTensor fromIntArray(final String name, final int[] dimensions, final int[] data) {
    Objects.requireNonNull(dimensions, "dimensions cannot be null");
    Objects.requireNonNull(data, "data cannot be null");

    validateDimensions(dimensions);
    final long expectedElements = calculateElementCount(dimensions);
    if (data.length != expectedElements) {
      throw new IllegalArgumentException(
          "Data size mismatch: expected " + expectedElements + " elements, got " + data.length);
    }

    final ByteBuffer buffer = ByteBuffer.allocate(data.length * 4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.asIntBuffer().put(data);

    return new NnTensor(name, dimensions, NnTensorType.I32, buffer.array());
  }

  /**
   * Creates a tensor from a 64-bit integer array.
   *
   * @param dimensions the tensor shape
   * @param data the long data in row-major order
   * @return a new I64 tensor
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if dimensions are invalid or data size doesn't match
   */
  public static NnTensor fromLongArray(final int[] dimensions, final long[] data) {
    return fromLongArray(null, dimensions, data);
  }

  /**
   * Creates a named tensor from a 64-bit integer array.
   *
   * @param name the tensor name (for named tensor operations)
   * @param dimensions the tensor shape
   * @param data the long data in row-major order
   * @return a new I64 tensor
   * @throws NullPointerException if dimensions or data is null
   * @throws IllegalArgumentException if dimensions are invalid or data size doesn't match
   */
  public static NnTensor fromLongArray(
      final String name, final int[] dimensions, final long[] data) {
    Objects.requireNonNull(dimensions, "dimensions cannot be null");
    Objects.requireNonNull(data, "data cannot be null");

    validateDimensions(dimensions);
    final long expectedElements = calculateElementCount(dimensions);
    if (data.length != expectedElements) {
      throw new IllegalArgumentException(
          "Data size mismatch: expected " + expectedElements + " elements, got " + data.length);
    }

    final ByteBuffer buffer = ByteBuffer.allocate(data.length * 8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.asLongBuffer().put(data);

    return new NnTensor(name, dimensions, NnTensorType.I64, buffer.array());
  }

  /**
   * Gets the tensor name.
   *
   * @return the name, or null if unnamed
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the tensor dimensions (shape).
   *
   * @return a copy of the dimensions array
   */
  public int[] getDimensions() {
    return dimensions.clone();
  }

  /**
   * Gets the number of dimensions (rank) of the tensor.
   *
   * @return the number of dimensions
   */
  public int getRank() {
    return dimensions.length;
  }

  /**
   * Gets the data type of the tensor elements.
   *
   * @return the tensor type
   */
  public NnTensorType getType() {
    return type;
  }

  /**
   * Gets the raw byte data.
   *
   * @return a copy of the raw data
   */
  public byte[] getData() {
    return data.clone();
  }

  /**
   * Gets the total number of elements in the tensor.
   *
   * @return the element count
   */
  public long getElementCount() {
    return calculateElementCount(dimensions);
  }

  /**
   * Gets the total size of the tensor data in bytes.
   *
   * @return the byte size
   */
  public int getByteSize() {
    return data.length;
  }

  /**
   * Gets the data as a float array. Only valid for FP32 tensors.
   *
   * @return the float data
   * @throws IllegalStateException if the tensor type is not FP32
   */
  public float[] toFloatArray() {
    if (type != NnTensorType.FP32) {
      throw new IllegalStateException("Cannot convert " + type + " tensor to float array");
    }

    final FloatBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
    final float[] result = new float[buffer.remaining()];
    buffer.get(result);
    return result;
  }

  /**
   * Gets the data as a byte array. Only valid for U8 tensors.
   *
   * @return the byte data
   * @throws IllegalStateException if the tensor type is not U8
   */
  public byte[] toByteArray() {
    if (type != NnTensorType.U8) {
      throw new IllegalStateException("Cannot convert " + type + " tensor to byte array");
    }
    return data.clone();
  }

  /**
   * Gets the data as an int array. Only valid for I32 tensors.
   *
   * @return the int data
   * @throws IllegalStateException if the tensor type is not I32
   */
  public int[] toIntArray() {
    if (type != NnTensorType.I32) {
      throw new IllegalStateException("Cannot convert " + type + " tensor to int array");
    }

    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    final int[] result = new int[data.length / 4];
    buffer.asIntBuffer().get(result);
    return result;
  }

  /**
   * Gets the data as a long array. Only valid for I64 tensors.
   *
   * @return the long data
   * @throws IllegalStateException if the tensor type is not I64
   */
  public long[] toLongArray() {
    if (type != NnTensorType.I64) {
      throw new IllegalStateException("Cannot convert " + type + " tensor to long array");
    }

    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    final long[] result = new long[data.length / 8];
    buffer.asLongBuffer().get(result);
    return result;
  }

  /**
   * Checks if this tensor has a name.
   *
   * @return true if the tensor is named
   */
  public boolean isNamed() {
    return name != null;
  }

  private static void validateDimensions(final int[] dimensions) {
    if (dimensions.length == 0) {
      throw new IllegalArgumentException("Dimensions array cannot be empty");
    }
    for (final int dim : dimensions) {
      if (dim < 0) {
        throw new IllegalArgumentException("Dimensions cannot be negative: " + dim);
      }
    }
  }

  private static long calculateElementCount(final int[] dimensions) {
    long count = 1;
    for (final int dim : dimensions) {
      count *= dim;
    }
    return count;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final NnTensor other = (NnTensor) obj;
    return Objects.equals(name, other.name)
        && Arrays.equals(dimensions, other.dimensions)
        && type == other.type
        && Arrays.equals(data, other.data);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(name, type);
    result = 31 * result + Arrays.hashCode(dimensions);
    result = 31 * result + Arrays.hashCode(data);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("NnTensor{");
    if (name != null) {
      sb.append("name='").append(name).append("', ");
    }
    sb.append("dimensions=").append(Arrays.toString(dimensions));
    sb.append(", type=").append(type);
    sb.append(", bytes=").append(data.length);
    sb.append('}');
    return sb.toString();
  }
}
