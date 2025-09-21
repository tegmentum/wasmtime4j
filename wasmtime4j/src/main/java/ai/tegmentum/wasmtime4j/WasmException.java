package ai.tegmentum.wasmtime4j;

import java.util.Arrays;

/**
 * Represents a WebAssembly exception that can be thrown and caught within WebAssembly modules.
 *
 * <p>WebAssembly exceptions are part of the exception handling proposal and provide structured
 * exception handling capabilities within WebAssembly execution. This class encapsulates exception
 * data and metadata according to the WebAssembly exception specification.
 *
 * @since 1.0.0
 */
public final class WasmException extends Exception {

  /** The exception type that defines the structure of this exception. */
  private final ExceptionHandling.ExceptionType exceptionType;

  /** The parameter values carried by this exception. */
  private final Object[] values;

  /** Optional stack trace information from WebAssembly execution. */
  private final String wasmStackTrace;

  /**
   * Creates a new WebAssembly exception.
   *
   * @param exceptionType the exception type
   * @param values the parameter values for the exception
   * @throws IllegalArgumentException if exceptionType is null, values is null, or parameter count
   *     mismatch
   */
  public WasmException(
      final ExceptionHandling.ExceptionType exceptionType, final Object... values) {
    this(exceptionType, null, null, values);
  }

  /**
   * Creates a new WebAssembly exception with a message.
   *
   * @param exceptionType the exception type
   * @param message the exception message
   * @param values the parameter values for the exception
   * @throws IllegalArgumentException if exceptionType is null, values is null, or parameter count
   *     mismatch
   */
  public WasmException(
      final ExceptionHandling.ExceptionType exceptionType,
      final String message,
      final Object... values) {
    this(exceptionType, message, null, values);
  }

  /**
   * Creates a new WebAssembly exception with a message and WebAssembly stack trace.
   *
   * @param exceptionType the exception type
   * @param message the exception message
   * @param wasmStackTrace the WebAssembly stack trace
   * @param values the parameter values for the exception
   * @throws IllegalArgumentException if exceptionType is null, values is null, or parameter count
   *     mismatch
   */
  public WasmException(
      final ExceptionHandling.ExceptionType exceptionType,
      final String message,
      final String wasmStackTrace,
      final Object... values) {
    super(buildMessage(exceptionType, message));

    if (exceptionType == null) {
      throw new IllegalArgumentException("Exception type cannot be null");
    }
    if (values == null) {
      throw new IllegalArgumentException("Values array cannot be null");
    }
    if (values.length != exceptionType.getParameters().size()) {
      throw new IllegalArgumentException(
          "Parameter count mismatch: expected "
              + exceptionType.getParameters().size()
              + ", got "
              + values.length);
    }

    this.exceptionType = exceptionType;
    this.values = Arrays.copyOf(values, values.length);
    this.wasmStackTrace = wasmStackTrace;
  }

  /**
   * Gets the exception type for this exception.
   *
   * @return the exception type
   */
  public ExceptionHandling.ExceptionType getExceptionType() {
    return exceptionType;
  }

  /**
   * Gets the parameter values for this exception.
   *
   * @return a copy of the parameter values array
   */
  public Object[] getValues() {
    return Arrays.copyOf(values, values.length);
  }

  /**
   * Gets the WebAssembly stack trace if available.
   *
   * @return the WebAssembly stack trace, or null if not available
   */
  public String getWasmStackTrace() {
    return wasmStackTrace;
  }

  /**
   * Checks if this exception has a WebAssembly stack trace.
   *
   * @return true if a WebAssembly stack trace is available, false otherwise
   */
  public boolean hasWasmStackTrace() {
    return wasmStackTrace != null && !wasmStackTrace.trim().isEmpty();
  }

  /**
   * Gets a specific parameter value by index.
   *
   * @param index the parameter index
   * @return the parameter value at the specified index
   * @throws IndexOutOfBoundsException if index is out of bounds
   */
  public Object getValue(final int index) {
    if (index < 0 || index >= values.length) {
      throw new IndexOutOfBoundsException(
          "Index " + index + " out of bounds for length " + values.length);
    }
    return values[index];
  }

  /**
   * Gets the number of parameter values in this exception.
   *
   * @return the number of parameter values
   */
  public int getValueCount() {
    return values.length;
  }

  /**
   * Builds a descriptive message for the exception.
   *
   * @param exceptionType the exception type
   * @param message the optional custom message
   * @return a formatted exception message
   */
  private static String buildMessage(
      final ExceptionHandling.ExceptionType exceptionType, final String message) {
    final StringBuilder sb = new StringBuilder();

    if (message != null && !message.trim().isEmpty()) {
      sb.append(message);
    } else {
      sb.append("WebAssembly exception");
    }

    if (exceptionType != null) {
      sb.append(" [type: ").append(exceptionType.getTag()).append("]");
      if (!exceptionType.getParameters().isEmpty()) {
        sb.append(" [parameters: ").append(exceptionType.getParameters().size()).append("]");
      }
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("WasmException{");
    sb.append("type=").append(exceptionType.getTag());
    sb.append(", valueCount=").append(values.length);
    if (hasWasmStackTrace()) {
      sb.append(", hasWasmTrace=true");
    }
    sb.append(", message='").append(getMessage()).append('\'');
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final WasmException that = (WasmException) obj;
    return exceptionType.equals(that.exceptionType)
        && Arrays.equals(values, that.values)
        && java.util.Objects.equals(wasmStackTrace, that.wasmStackTrace);
  }

  @Override
  public int hashCode() {
    int result = exceptionType.hashCode();
    result = 31 * result + Arrays.hashCode(values);
    result = 31 * result + (wasmStackTrace != null ? wasmStackTrace.hashCode() : 0);
    return result;
  }
}
