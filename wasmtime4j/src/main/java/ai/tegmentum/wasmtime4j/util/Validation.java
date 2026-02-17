package ai.tegmentum.wasmtime4j.util;

import java.nio.charset.StandardCharsets;

/**
 * Utility class providing defensive programming validation methods.
 *
 * <p>This class implements comprehensive parameter validation to prevent JVM crashes by ensuring
 * all parameters are valid before making native calls. This is part of the defensive programming
 * strategy where JVM crash prevention is the highest priority.
 *
 * <p>All validation methods throw {@link IllegalArgumentException} with descriptive error messages
 * when validation fails.
 *
 * @since 1.0.0
 */
public final class Validation {

  /** Private constructor to prevent instantiation of utility class. */
  private Validation() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Validates that an object is not null.
   *
   * @param obj the object to validate
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the object is null
   */
  public static void requireNonNull(final Object obj, final String parameterName) {
    if (obj == null) {
      throw new IllegalArgumentException("Parameter '" + parameterName + "' must not be null");
    }
  }

  /**
   * Validates that a string is not null or empty.
   *
   * @param str the string to validate
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the string is null or empty
   */
  public static void requireNonEmpty(final String str, final String parameterName) {
    requireNonNull(str, parameterName);
    if (str.isEmpty()) {
      throw new IllegalArgumentException("Parameter '" + parameterName + "' must not be empty");
    }
  }

  /**
   * Validates that an array is not null or empty.
   *
   * @param array the array to validate
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the array is null or empty
   */
  public static void requireNonEmpty(final byte[] array, final String parameterName) {
    requireNonNull(array, parameterName);
    if (array.length == 0) {
      throw new IllegalArgumentException("Parameter '" + parameterName + "' must not be empty");
    }
  }

  /**
   * Validates that a string is not null, empty, or only whitespace.
   *
   * @param str the string to validate
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the string is null, empty, or only whitespace
   */
  public static void requireNonBlank(final String str, final String parameterName) {
    requireNonNull(str, parameterName);
    if (str.trim().isEmpty()) {
      throw new IllegalArgumentException(
          "Parameter '" + parameterName + "' must not be empty or whitespace-only");
    }
  }

  /**
   * Validates that an integer value is within the specified range.
   *
   * @param value the value to validate
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the value is out of range
   */
  public static void requireInRange(
      final int value, final int min, final int max, final String parameterName) {
    if (value < min || value > max) {
      throw new IllegalArgumentException(
          String.format(
              "Parameter '%s' must be in range [%d, %d], got %d", parameterName, min, max, value));
    }
  }

  /**
   * Validates that a long value is within the specified range.
   *
   * @param value the value to validate
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the value is out of range
   */
  public static void requireInRange(
      final long value, final long min, final long max, final String parameterName) {
    if (value < min || value > max) {
      throw new IllegalArgumentException(
          String.format(
              "Parameter '%s' must be in range [%d, %d], got %d", parameterName, min, max, value));
    }
  }

  /**
   * Validates that a value is positive (greater than zero).
   *
   * @param value the value to validate
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the value is not positive
   */
  public static void requirePositive(final int value, final String parameterName) {
    if (value <= 0) {
      throw new IllegalArgumentException(
          "Parameter '" + parameterName + "' must be positive, got " + value);
    }
  }

  /**
   * Validates that a value is positive (greater than zero).
   *
   * @param value the value to validate
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the value is not positive
   */
  public static void requirePositive(final long value, final String parameterName) {
    if (value <= 0L) {
      throw new IllegalArgumentException(
          "Parameter '" + parameterName + "' must be positive, got " + value);
    }
  }

  /**
   * Validates that a value is non-negative (greater than or equal to zero).
   *
   * @param value the value to validate
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the value is negative
   */
  public static void requireNonNegative(final int value, final String parameterName) {
    if (value < 0) {
      throw new IllegalArgumentException(
          "Parameter '" + parameterName + "' must be non-negative, got " + value);
    }
  }

  /**
   * Validates that a value is non-negative (greater than or equal to zero).
   *
   * @param value the value to validate
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the value is negative
   */
  public static void requireNonNegative(final long value, final String parameterName) {
    if (value < 0L) {
      throw new IllegalArgumentException(
          "Parameter '" + parameterName + "' must be non-negative, got " + value);
    }
  }

  /**
   * Validates that a native handle (pointer) is valid (not zero or negative).
   *
   * @param handle the native handle to validate
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the handle is invalid
   */
  public static void requireValidHandle(final long handle, final String parameterName) {
    if (handle == 0L) {
      throw new IllegalArgumentException(
          "Parameter '" + parameterName + "' is an invalid native handle (null pointer)");
    }
    if (handle < 0L) {
      throw new IllegalArgumentException(
          "Parameter '" + parameterName + "' is an invalid native handle (negative value)");
    }
  }

  /**
   * Validates array bounds for access operations.
   *
   * @param array the array being accessed
   * @param offset the offset into the array
   * @param length the number of elements to access
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the bounds are invalid
   */
  public static void requireValidBounds(
      final byte[] array, final int offset, final int length, final String parameterName) {
    requireNonNull(array, parameterName);
    requireNonNegative(offset, "offset");
    requireNonNegative(length, "length");

    if (offset > array.length) {
      throw new IllegalArgumentException(
          String.format("Offset %d exceeds array length %d", offset, array.length));
    }

    if (offset + length > array.length) {
      throw new IllegalArgumentException(
          String.format(
              "Offset %d + length %d exceeds array length %d", offset, length, array.length));
    }
  }

  /**
   * Validates that a condition is true.
   *
   * @param condition the condition to validate
   * @param message the error message if the condition is false
   * @throws IllegalArgumentException if the condition is false
   */
  public static void require(final boolean condition, final String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Creates a defensive copy of a byte array to prevent external modification.
   *
   * @param original the original array
   * @return a defensive copy of the array, or null if the original is null
   */
  public static byte[] defensiveCopy(final byte[] original) {
    return original == null ? null : original.clone();
  }

  /**
   * Safely converts a string to bytes with null checking.
   *
   * @param str the string to convert
   * @param parameterName the parameter name for error messages
   * @return the byte array representation of the string
   * @throws IllegalArgumentException if the string is null
   */
  public static byte[] toBytes(final String str, final String parameterName) {
    requireNonNull(str, parameterName);
    return str.getBytes(StandardCharsets.UTF_8);
  }
}
