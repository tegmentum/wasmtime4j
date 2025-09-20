package ai.tegmentum.wasmtime4j.panama.util;

/**
 * Utility class providing defensive programming validation methods for Panama FFI operations.
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
public final class PanamaValidation {

  /** Private constructor to prevent instantiation of utility class. */
  private PanamaValidation() {
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
   * Validates that a string is not null, empty, or blank (whitespace only).
   *
   * @param str the string to validate
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the string is null, empty, or blank
   */
  public static void requireNonBlank(final String str, final String parameterName) {
    requireNonNull(str, parameterName);
    if (str.trim().isEmpty()) {
      throw new IllegalArgumentException("Parameter '" + parameterName + "' must not be blank");
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
   * Validates that a value is non-negative.
   *
   * @param value the value to validate
   * @param parameterName the parameter name for error messages
   * @throws IllegalArgumentException if the value is negative
   */
  public static void requireNonNegative(final long value, final String parameterName) {
    if (value < 0) {
      throw new IllegalArgumentException(
          "Parameter '" + parameterName + "' must be non-negative, got " + value);
    }
  }

  /**
   * Validates that a native handle is valid (non-zero).
   *
   * @param handle the native handle to validate
   * @param handleName the handle name for error messages
   * @throws IllegalArgumentException if the handle is invalid
   */
  public static void requireValidHandle(final long handle, final String handleName) {
    if (handle == 0) {
      throw new IllegalArgumentException(
          "Native handle '" + handleName + "' is invalid (null pointer)");
    }
  }

  /**
   * Validates that a native memory segment handle is valid (not NULL).
   *
   * @param handle the memory segment handle to validate
   * @param handleName the handle name for error messages
   * @throws IllegalArgumentException if the handle is invalid
   */
  public static void requireValidHandle(
      final java.lang.foreign.MemorySegment handle, final String handleName) {
    requireNonNull(handle, handleName);
    if (handle.equals(java.lang.foreign.MemorySegment.NULL)) {
      throw new IllegalArgumentException(
          "Native handle '" + handleName + "' is invalid (null pointer)");
    }
  }

  /**
   * Creates a defensive copy of a byte array.
   *
   * @param array the array to copy
   * @return a defensive copy of the array
   * @throws IllegalArgumentException if the array is null
   */
  public static byte[] defensiveCopy(final byte[] array) {
    requireNonNull(array, "array");
    return array.clone();
  }

  /**
   * Validates that an integer value is positive.
   *
   * @param value the value to validate
   * @param parameterName the parameter name for error messages
   * @return the validated value
   * @throws IllegalArgumentException if the value is not positive
   */
  public static int requirePositive(final int value, final String parameterName) {
    if (value <= 0) {
      throw new IllegalArgumentException(
          "Parameter '" + parameterName + "' must be positive: " + value);
    }
    return value;
  }

  /**
   * Validates that a long value is positive.
   *
   * @param value the value to validate
   * @param parameterName the parameter name for error messages
   * @return the validated value
   * @throws IllegalArgumentException if the value is not positive
   */
  public static long requirePositive(final long value, final String parameterName) {
    if (value <= 0) {
      throw new IllegalArgumentException(
          "Parameter '" + parameterName + "' must be positive: " + value);
    }
    return value;
  }
}
