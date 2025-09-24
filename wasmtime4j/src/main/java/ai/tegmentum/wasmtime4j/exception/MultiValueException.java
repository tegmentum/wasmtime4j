package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when multi-value WebAssembly operations fail.
 *
 * <p>This exception is specifically for errors related to the WebAssembly multi-value proposal,
 * such as type mismatches in multi-value returns, validation failures, or marshaling errors with
 * multiple values.
 *
 * @since 1.0.0
 */
public class MultiValueException extends WasmException {

  private static final long serialVersionUID = 1L;

  /** The expected number of values. */
  private final int expectedCount;

  /** The actual number of values. */
  private final int actualCount;

  /** The operation that failed. */
  private final String operation;

  /**
   * Creates a new multi-value exception.
   *
   * @param message the error message
   */
  public MultiValueException(final String message) {
    super(message);
    this.expectedCount = -1;
    this.actualCount = -1;
    this.operation = null;
  }

  /**
   * Creates a new multi-value exception with a cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public MultiValueException(final String message, final Throwable cause) {
    super(message, cause);
    this.expectedCount = -1;
    this.actualCount = -1;
    this.operation = null;
  }

  /**
   * Creates a new multi-value exception with count information.
   *
   * @param message the error message
   * @param expectedCount the expected number of values
   * @param actualCount the actual number of values
   */
  public MultiValueException(final String message, final int expectedCount, final int actualCount) {
    super(message);
    this.expectedCount = expectedCount;
    this.actualCount = actualCount;
    this.operation = null;
  }

  /**
   * Creates a new multi-value exception with full details.
   *
   * @param message the error message
   * @param expectedCount the expected number of values
   * @param actualCount the actual number of values
   * @param operation the operation that failed
   */
  public MultiValueException(
      final String message,
      final int expectedCount,
      final int actualCount,
      final String operation) {
    super(message);
    this.expectedCount = expectedCount;
    this.actualCount = actualCount;
    this.operation = operation;
  }

  /**
   * Creates a new multi-value exception with full details and cause.
   *
   * @param message the error message
   * @param expectedCount the expected number of values
   * @param actualCount the actual number of values
   * @param operation the operation that failed
   * @param cause the underlying cause
   */
  public MultiValueException(
      final String message,
      final int expectedCount,
      final int actualCount,
      final String operation,
      final Throwable cause) {
    super(message, cause);
    this.expectedCount = expectedCount;
    this.actualCount = actualCount;
    this.operation = operation;
  }

  /**
   * Gets the expected number of values.
   *
   * @return the expected count, or -1 if not available
   */
  public int getExpectedCount() {
    return expectedCount;
  }

  /**
   * Gets the actual number of values.
   *
   * @return the actual count, or -1 if not available
   */
  public int getActualCount() {
    return actualCount;
  }

  /**
   * Gets the operation that failed.
   *
   * @return the operation name, or null if not available
   */
  public String getOperation() {
    return operation;
  }

  /**
   * Checks if this exception has count information.
   *
   * @return true if expected and actual counts are available
   */
  public boolean hasCountInfo() {
    return expectedCount >= 0 && actualCount >= 0;
  }

  /**
   * Checks if this exception has operation information.
   *
   * @return true if operation name is available
   */
  public boolean hasOperationInfo() {
    return operation != null && !operation.isEmpty();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
    sb.append(": ").append(getMessage());

    if (hasCountInfo()) {
      sb.append(" (expected: ")
          .append(expectedCount)
          .append(", actual: ")
          .append(actualCount)
          .append(")");
    }

    if (hasOperationInfo()) {
      sb.append(" [operation: ").append(operation).append("]");
    }

    return sb.toString();
  }

  // Factory methods for common multi-value error scenarios

  /**
   * Creates a count mismatch exception.
   *
   * @param expected the expected number of values
   * @param actual the actual number of values
   * @return a new MultiValueException
   */
  public static MultiValueException countMismatch(final int expected, final int actual) {
    return new MultiValueException(
        String.format("Value count mismatch: expected %d, got %d", expected, actual),
        expected,
        actual);
  }

  /**
   * Creates a count mismatch exception for a specific operation.
   *
   * @param expected the expected number of values
   * @param actual the actual number of values
   * @param operation the operation that failed
   * @return a new MultiValueException
   */
  public static MultiValueException countMismatch(
      final int expected, final int actual, final String operation) {
    return new MultiValueException(
        String.format(
            "Value count mismatch in %s: expected %d, got %d", operation, expected, actual),
        expected,
        actual,
        operation);
  }

  /**
   * Creates a type validation exception.
   *
   * @param index the index of the value with type error
   * @param expectedType the expected type
   * @param actualType the actual type
   * @return a new MultiValueException
   */
  public static MultiValueException typeValidationError(
      final int index, final String expectedType, final String actualType) {
    return new MultiValueException(
        String.format(
            "Type validation failed at index %d: expected %s, got %s",
            index, expectedType, actualType));
  }

  /**
   * Creates a marshaling exception.
   *
   * @param operation the marshaling operation that failed
   * @param valueCount the number of values being marshaled
   * @param cause the underlying cause
   * @return a new MultiValueException
   */
  public static MultiValueException marshalingError(
      final String operation, final int valueCount, final Throwable cause) {
    return new MultiValueException(
        String.format("Marshaling failed for %s with %d values", operation, valueCount),
        -1,
        valueCount,
        operation,
        cause);
  }

  /**
   * Creates an exception for exceeding multi-value limits.
   *
   * @param actualCount the number of values that exceeded the limit
   * @param maxAllowed the maximum number of values allowed
   * @return a new MultiValueException
   */
  public static MultiValueException limitExceeded(final int actualCount, final int maxAllowed) {
    return new MultiValueException(
        String.format(
            "Multi-value limit exceeded: %d values (max allowed: %d)", actualCount, maxAllowed),
        maxAllowed,
        actualCount);
  }

  /**
   * Creates a validation exception for null or invalid multi-value arrays.
   *
   * @param operation the operation being performed
   * @return a new MultiValueException
   */
  public static MultiValueException invalidValueArray(final String operation) {
    return new MultiValueException(
        String.format("Invalid value array for operation: %s", operation), -1, -1, operation);
  }
}
