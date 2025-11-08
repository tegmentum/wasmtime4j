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

/**
 * SIMD lane types representing different vector configurations.
 *
 * <p>Each lane type defines the element type and number of elements in a 128-bit SIMD vector.
 *
 * @since 1.0.0
 */
public enum SimdLane {
  /** 16 lanes of 8-bit signed integers. */
  I8X16(1, 16, "i8x16"),

  /** 8 lanes of 16-bit signed integers. */
  I16X8(2, 8, "i16x8"),

  /** 4 lanes of 32-bit signed integers. */
  I32X4(4, 4, "i32x4"),

  /** 2 lanes of 64-bit signed integers. */
  I64X2(8, 2, "i64x2"),

  /** 4 lanes of 32-bit floating-point numbers. */
  F32X4(4, 4, "f32x4"),

  /** 2 lanes of 64-bit floating-point numbers. */
  F64X2(8, 2, "f64x2");

  /** Number of bytes per lane element. */
  private final int bytesPerElement;

  /** Number of lanes in the vector. */
  private final int laneCount;

  /** String representation for debugging. */
  private final String displayName;

  /**
   * Creates a SIMD lane type.
   *
   * @param bytesPerElement number of bytes per element
   * @param laneCount number of lanes
   * @param displayName display name
   */
  SimdLane(final int bytesPerElement, final int laneCount, final String displayName) {
    this.bytesPerElement = bytesPerElement;
    this.laneCount = laneCount;
    this.displayName = displayName;
  }

  /**
   * Gets the number of bytes per lane element.
   *
   * @return bytes per element
   */
  public int getBytesPerElement() {
    return bytesPerElement;
  }

  /**
   * Gets the number of lanes in the vector.
   *
   * @return lane count
   */
  public int getLaneCount() {
    return laneCount;
  }

  /**
   * Gets the total vector size in bytes (always 128 bits = 16 bytes).
   *
   * @return vector size in bytes
   */
  public int getVectorSizeBytes() {
    return 16;
  }

  /**
   * Gets the display name for this lane type.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if this is an integer lane type.
   *
   * @return true if integer type, false if floating-point
   */
  public boolean isIntegerType() {
    return this == I8X16 || this == I16X8 || this == I32X4 || this == I64X2;
  }

  /**
   * Checks if this is a floating-point lane type.
   *
   * @return true if floating-point type, false if integer
   */
  public boolean isFloatType() {
    return this == F32X4 || this == F64X2;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
