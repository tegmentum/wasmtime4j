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

/**
 * Represents the data types supported for tensor elements in WASI-NN.
 *
 * <p>These types correspond to the tensor_type enum in the WASI-NN specification. Each type has an
 * associated byte size for memory layout calculations.
 *
 * @since 1.0.0
 */
public enum NnTensorType {

  /** 16-bit floating point (half precision). */
  FP16(2, "fp16"),

  /** 32-bit floating point (single precision). */
  FP32(4, "fp32"),

  /** 64-bit floating point (double precision). */
  FP64(8, "fp64"),

  /** Brain float 16-bit floating point. */
  BF16(2, "bf16"),

  /** 8-bit unsigned integer. */
  U8(1, "u8"),

  /** 32-bit signed integer. */
  I32(4, "i32"),

  /** 64-bit signed integer. */
  I64(8, "i64");

  private final int byteSize;
  private final String wasiName;

  NnTensorType(final int byteSize, final String wasiName) {
    this.byteSize = byteSize;
    this.wasiName = wasiName;
  }

  /**
   * Gets the byte size of a single element of this type.
   *
   * @return the number of bytes per element
   */
  public int getByteSize() {
    return byteSize;
  }

  /**
   * Gets the WASI-NN specification name for this type.
   *
   * @return the WASI-NN type name
   */
  public String getWasiName() {
    return wasiName;
  }

  /**
   * Calculates the total byte size for an array of elements with given dimensions.
   *
   * @param dimensions the tensor dimensions (shape)
   * @return the total byte size
   * @throws IllegalArgumentException if any dimension is negative
   */
  public long calculateByteSize(final int[] dimensions) {
    if (dimensions == null || dimensions.length == 0) {
      return 0;
    }
    long total = byteSize;
    for (final int dim : dimensions) {
      if (dim < 0) {
        throw new IllegalArgumentException("Dimensions cannot be negative: " + dim);
      }
      total *= dim;
    }
    return total;
  }

  /**
   * Parses a tensor type from its WASI-NN name.
   *
   * @param wasiName the WASI-NN type name
   * @return the corresponding tensor type
   * @throws IllegalArgumentException if the name is not recognized
   */
  public static NnTensorType fromWasiName(final String wasiName) {
    for (final NnTensorType type : values()) {
      if (type.wasiName.equals(wasiName)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown tensor type: " + wasiName);
  }

  /**
   * Gets the native code value for this tensor type.
   *
   * @return the native code (ordinal)
   */
  public int getNativeCode() {
    return ordinal();
  }

  /**
   * Creates a tensor type from a native code.
   *
   * @param code the native code
   * @return the corresponding tensor type
   * @throws IllegalArgumentException if the code is invalid
   */
  public static NnTensorType fromNativeCode(final int code) {
    final NnTensorType[] values = values();
    if (code < 0 || code >= values.length) {
      throw new IllegalArgumentException("Invalid tensor type code: " + code);
    }
    return values[code];
  }
}
