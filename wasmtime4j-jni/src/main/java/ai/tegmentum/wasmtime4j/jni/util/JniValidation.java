package ai.tegmentum.wasmtime4j.jni.util;

import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import ai.tegmentum.wasmtime4j.util.Validation;

/**
 * Utility class providing defensive programming validation methods for JNI operations.
 *
 * <p>This class implements comprehensive parameter validation to prevent JVM crashes by ensuring
 * all parameters are valid before making native calls. This is part of the defensive programming
 * strategy where JVM crash prevention is the highest priority.
 *
 * <p>All validation methods throw {@link JniValidationException} with descriptive error messages
 * when validation fails.
 *
 * @since 1.0.0
 */
public final class JniValidation {

  /** Private constructor to prevent instantiation of utility class. */
  private JniValidation() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Validates that an object is not null.
   *
   * @param obj the object to validate
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the object is null
   */
  public static void requireNonNull(final Object obj, final String parameterName) {
    try {
      Validation.requireNonNull(obj, parameterName);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), parameterName, obj);
    }
  }

  /**
   * Validates that a string is not null or empty.
   *
   * @param str the string to validate
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the string is null or empty
   */
  public static void requireNonEmpty(final String str, final String parameterName) {
    try {
      Validation.requireNonEmpty(str, parameterName);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), parameterName, str);
    }
  }

  /**
   * Validates that an array is not null or empty.
   *
   * @param array the array to validate
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the array is null or empty
   */
  public static void requireNonEmpty(final byte[] array, final String parameterName) {
    try {
      Validation.requireNonEmpty(array, parameterName);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), parameterName, "byte[0]");
    }
  }

  /**
   * Validates that a string is not null, empty, or only whitespace.
   *
   * @param str the string to validate
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the string is null, empty, or only whitespace
   */
  public static void requireNonBlank(final String str, final String parameterName) {
    try {
      Validation.requireNonBlank(str, parameterName);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), parameterName, str);
    }
  }

  /**
   * Validates that an integer value is within the specified range.
   *
   * @param value the value to validate
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the value is out of range
   */
  public static void requireInRange(
      final int value, final int min, final int max, final String parameterName) {
    try {
      Validation.requireInRange(value, min, max, parameterName);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), parameterName, value);
    }
  }

  /**
   * Validates that a long value is within the specified range.
   *
   * @param value the value to validate
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the value is out of range
   */
  public static void requireInRange(
      final long value, final long min, final long max, final String parameterName) {
    try {
      Validation.requireInRange(value, min, max, parameterName);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), parameterName, value);
    }
  }

  /**
   * Validates that a value is positive (greater than zero).
   *
   * @param value the value to validate
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the value is not positive
   */
  public static void requirePositive(final int value, final String parameterName) {
    try {
      Validation.requirePositive(value, parameterName);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), parameterName, value);
    }
  }

  /**
   * Validates that a value is positive (greater than zero).
   *
   * @param value the value to validate
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the value is not positive
   */
  public static void requirePositive(final long value, final String parameterName) {
    try {
      Validation.requirePositive(value, parameterName);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), parameterName, value);
    }
  }

  /**
   * Validates that a value is non-negative (greater than or equal to zero).
   *
   * @param value the value to validate
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the value is negative
   */
  public static void requireNonNegative(final int value, final String parameterName) {
    try {
      Validation.requireNonNegative(value, parameterName);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), parameterName, value);
    }
  }

  /**
   * Validates that a value is non-negative (greater than or equal to zero).
   *
   * @param value the value to validate
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the value is negative
   */
  public static void requireNonNegative(final long value, final String parameterName) {
    try {
      Validation.requireNonNegative(value, parameterName);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), parameterName, value);
    }
  }

  /**
   * Validates that a native handle (pointer) is valid (not zero).
   *
   * @param handle the native handle to validate
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the handle is invalid (zero)
   */
  public static void requireValidHandle(final long handle, final String parameterName) {
    try {
      Validation.requireValidHandle(handle, parameterName);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), parameterName, handle);
    }
  }

  /**
   * Validates array bounds for access operations.
   *
   * @param array the array being accessed
   * @param offset the offset into the array
   * @param length the number of elements to access
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the bounds are invalid
   */
  public static void requireValidBounds(
      final byte[] array, final int offset, final int length, final String parameterName) {
    try {
      Validation.requireValidBounds(array, offset, length, parameterName);
    } catch (IllegalArgumentException e) {
      // Extract which parameter failed from the exception message
      String failedParam = e.getMessage().contains("Offset") ? "offset" : "length";
      Object failedValue = failedParam.equals("offset") ? offset : length;
      throw new JniValidationException(e.getMessage(), failedParam, failedValue);
    }
  }

  /**
   * Validates that a condition is true.
   *
   * @param condition the condition to validate
   * @param message the error message if the condition is false
   * @throws JniValidationException if the condition is false
   */
  public static void require(final boolean condition, final String message) {
    try {
      Validation.require(condition, message);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage());
    }
  }

  /**
   * Validates that a condition is true with parameter details.
   *
   * @param condition the condition to validate
   * @param message the error message if the condition is false
   * @param parameterName the parameter name for error messages
   * @param parameterValue the parameter value for error messages
   * @throws JniValidationException if the condition is false
   */
  public static void require(
      final boolean condition,
      final String message,
      final String parameterName,
      final Object parameterValue) {
    if (!condition) {
      throw new JniValidationException(message, parameterName, parameterValue);
    }
  }

  /**
   * Validates that a port number is valid (1-65535).
   *
   * @param port the port number to validate
   * @throws JniValidationException if the port is invalid
   */
  public static void requireValidPort(final int port) {
    try {
      Validation.requireValidPort(port);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), "port", port);
    }
  }

  /**
   * Validates that a connection ID is valid and exists in the provided map.
   *
   * @param connectionId the connection ID to validate
   * @param activeConnections the map of active connections
   * @throws JniValidationException if the connection ID is invalid or not found
   */
  public static void requireValidConnectionId(
      final long connectionId, final java.util.Map<Long, ?> activeConnections) {
    try {
      Validation.requireValidConnectionId(connectionId, activeConnections);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), "connectionId", connectionId);
    }
  }

  /**
   * Validates that a string is non-null and non-empty.
   *
   * @param str the string to validate
   * @param parameterName the parameter name for error messages
   * @throws JniValidationException if the string is null or empty
   */
  public static void requireValidString(final String str, final String parameterName) {
    requireNonNull(str, parameterName);
    require(!str.trim().isEmpty(), "String cannot be empty", parameterName, str);
  }

  /**
   * Creates a defensive copy of a byte array to prevent external modification.
   *
   * @param original the original array
   * @return a defensive copy of the array, or null if the original is null
   */
  public static byte[] defensiveCopy(final byte[] original) {
    return Validation.defensiveCopy(original);
  }

  /**
   * Safely converts a string to bytes with null checking.
   *
   * @param str the string to convert
   * @param parameterName the parameter name for error messages
   * @return the byte array representation of the string
   * @throws JniValidationException if the string is null
   */
  public static byte[] toBytes(final String str, final String parameterName) {
    try {
      return Validation.toBytes(str, parameterName);
    } catch (IllegalArgumentException e) {
      throw new JniValidationException(e.getMessage(), parameterName, str);
    }
  }
}
