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
package ai.tegmentum.wasmtime4j.gc;

/**
 * WebAssembly GC I31 type utilities.
 *
 * <p>The I31 type represents immediate 31-bit signed integers stored as references, providing
 * efficient storage for small integers without heap allocation. I31 values are always non-null and
 * support equality comparison.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Check if a value fits in I31
 * if (I31Type.isValidValue(42)) {
 *     // Value can be stored as I31
 * }
 *
 * // Get the range of valid I31 values
 * int min = I31Type.getMinValue();
 * int max = I31Type.getMaxValue();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class I31Type {
  /** The minimum value that can be stored in an I31 reference. */
  public static final int MIN_VALUE = -(1 << 30); // -2^30

  /** The maximum value that can be stored in an I31 reference. */
  public static final int MAX_VALUE = (1 << 30) - 1; // 2^30 - 1

  /** The number of bits used for I31 values (31 bits, signed). */
  public static final int BIT_WIDTH = 31;

  private I31Type() {
    // Utility class - no instances
  }

  /**
   * Checks if a value can be stored as an I31 reference.
   *
   * @param value the value to check
   * @return true if the value fits in 31 bits (signed)
   */
  public static boolean isValidValue(final int value) {
    return value >= MIN_VALUE && value <= MAX_VALUE;
  }

  /**
   * Checks if a long value can be stored as an I31 reference.
   *
   * @param value the value to check
   * @return true if the value fits in 31 bits (signed)
   */
  public static boolean isValidValue(final long value) {
    return value >= MIN_VALUE && value <= MAX_VALUE;
  }

  /**
   * Validates that a value can be stored as I31 and returns it.
   *
   * @param value the value to validate
   * @return the value if valid
   * @throws IllegalArgumentException if the value is out of range
   */
  public static int validateValue(final int value) {
    if (!isValidValue(value)) {
      throw new IllegalArgumentException(
          "Value "
              + value
              + " is out of range for I31 (valid range: "
              + MIN_VALUE
              + " to "
              + MAX_VALUE
              + ")");
    }
    return value;
  }

  /**
   * Validates that a long value can be stored as I31 and returns it as int.
   *
   * @param value the value to validate
   * @return the value as int if valid
   * @throws IllegalArgumentException if the value is out of range
   */
  public static int validateValue(final long value) {
    if (!isValidValue(value)) {
      throw new IllegalArgumentException(
          "Value "
              + value
              + " is out of range for I31 (valid range: "
              + MIN_VALUE
              + " to "
              + MAX_VALUE
              + ")");
    }
    return (int) value;
  }

  /**
   * Clamps a value to the I31 range.
   *
   * @param value the value to clamp
   * @return the clamped value
   */
  public static int clampValue(final int value) {
    if (value < MIN_VALUE) {
      return MIN_VALUE;
    }
    if (value > MAX_VALUE) {
      return MAX_VALUE;
    }
    return value;
  }

  /**
   * Clamps a long value to the I31 range.
   *
   * @param value the value to clamp
   * @return the clamped value as int
   */
  public static int clampValue(final long value) {
    if (value < MIN_VALUE) {
      return MIN_VALUE;
    }
    if (value > MAX_VALUE) {
      return MAX_VALUE;
    }
    return (int) value;
  }

  /**
   * Gets the minimum value that can be stored in an I31 reference.
   *
   * @return the minimum I31 value
   */
  public static int getMinValue() {
    return MIN_VALUE;
  }

  /**
   * Gets the maximum value that can be stored in an I31 reference.
   *
   * @return the maximum I31 value
   */
  public static int getMaxValue() {
    return MAX_VALUE;
  }

  /**
   * Gets the range of valid I31 values.
   *
   * @return the range as a human-readable string
   */
  public static String getRange() {
    return "[" + MIN_VALUE + ", " + MAX_VALUE + "]";
  }

  /**
   * Gets the bit width of I31 values.
   *
   * @return the bit width (31)
   */
  public static int getBitWidth() {
    return BIT_WIDTH;
  }

  /**
   * Converts an I31 value to unsigned representation.
   *
   * <p>This masks the value to 31 bits, treating the result as unsigned.
   *
   * @param value the signed I31 value
   * @return the unsigned representation
   */
  public static int toUnsigned(final int value) {
    validateValue(value);
    return value & 0x7FFFFFFF; // Mask to 31 bits
  }

  /**
   * Converts an unsigned 31-bit value to signed representation.
   *
   * @param unsignedValue the unsigned value (must be in range [0, 2^31-1])
   * @return the signed representation
   * @throws IllegalArgumentException if the unsigned value is out of range
   */
  public static int fromUnsigned(final int unsignedValue) {
    if (unsignedValue < 0 || unsignedValue > ((1L << 31) - 1)) {
      throw new IllegalArgumentException(
          "Unsigned value " + unsignedValue + " is out of range for 31-bit unsigned integer");
    }

    // If the 31st bit is set, this represents a negative number in 31-bit signed
    if ((unsignedValue & (1 << 30)) != 0) {
      // Sign extend from 31 bits to 32 bits
      return unsignedValue | 0x80000000;
    }

    return unsignedValue;
  }

  /**
   * Checks if two I31 values are equal.
   *
   * <p>This is equivalent to regular integer equality, but provided for consistency with the I31
   * type semantics.
   *
   * @param value1 the first value
   * @param value2 the second value
   * @return true if the values are equal
   */
  public static boolean equals(final int value1, final int value2) {
    validateValue(value1);
    validateValue(value2);
    return value1 == value2;
  }

  /**
   * Compares two I31 values.
   *
   * @param value1 the first value
   * @param value2 the second value
   * @return negative if value1 < value2, zero if equal, positive if value1 > value2
   */
  public static int compare(final int value1, final int value2) {
    validateValue(value1);
    validateValue(value2);
    return Integer.compare(value1, value2);
  }

  /**
   * Returns a string representation of an I31 value.
   *
   * @param value the I31 value
   * @return string representation
   */
  public static String toString(final int value) {
    validateValue(value);
    return "i31(" + value + ")";
  }

  /** Utility class for I31 value boxing/unboxing. */
  public static final class I31Value {
    private final int value;

    private I31Value(final int value) {
      this.value = validateValue(value);
    }

    /**
     * Create an I31 value wrapper.
     *
     * @param value the integer value
     * @return wrapped I31 value
     * @throws IllegalArgumentException if value is out of range
     */
    public static I31Value of(final int value) {
      return new I31Value(value);
    }

    /**
     * Create an I31 value wrapper from a long.
     *
     * @param value the long value
     * @return wrapped I31 value
     * @throws IllegalArgumentException if value is out of range
     */
    public static I31Value of(final long value) {
      return new I31Value(validateValue(value));
    }

    /**
     * Gets the integer value.
     *
     * @return the integer value
     */
    public int getValue() {
      return value;
    }

    /**
     * Gets the unsigned representation.
     *
     * @return the unsigned value
     */
    public int getUnsigned() {
      return toUnsigned(value);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final I31Value i31Value = (I31Value) obj;
      return value == i31Value.value;
    }

    @Override
    public int hashCode() {
      return Integer.hashCode(value);
    }

    @Override
    public String toString() {
      return I31Type.toString(value);
    }
  }
}
