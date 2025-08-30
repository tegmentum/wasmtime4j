package ai.tegmentum.wasmtime4j.jni.exception;

/**
 * Exception thrown when parameter validation fails before making JNI calls.
 *
 * <p>This exception is thrown when:
 *
 * <ul>
 *   <li>Required parameters are null or invalid
 *   <li>Parameter values are out of valid range
 *   <li>Parameter combinations are invalid
 *   <li>Buffer sizes or array lengths are invalid
 *   <li>Native handles are invalid or expired
 * </ul>
 *
 * <p>This exception is part of the defensive programming strategy to prevent JVM crashes by
 * validating all parameters before making native calls.
 *
 * @since 1.0.0
 */
public final class JniValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The parameter name that failed validation. */
  private final String parameterName;

  /** The invalid parameter value. */
  private final transient Object parameterValue;

  /**
   * Creates a new JNI validation exception with the specified message.
   *
   * @param message the error message
   */
  public JniValidationException(final String message) {
    super(message);
    this.parameterName = null;
    this.parameterValue = null;
  }

  /**
   * Creates a new JNI validation exception with the specified message and parameter details.
   *
   * @param message the error message
   * @param parameterName the name of the parameter that failed validation
   * @param parameterValue the invalid parameter value
   */
  public JniValidationException(
      final String message, final String parameterName, final Object parameterValue) {
    super(message);
    this.parameterName = parameterName;
    this.parameterValue = parameterValue;
  }

  /**
   * Gets the name of the parameter that failed validation.
   *
   * @return the parameter name, or null if not specified
   */
  public String getParameterName() {
    return parameterName;
  }

  /**
   * Gets the invalid parameter value.
   *
   * @return the parameter value, or null if not specified
   */
  public Object getParameterValue() {
    return parameterValue;
  }

  /**
   * Checks if this exception has parameter details.
   *
   * @return true if parameter name and value are available, false otherwise
   */
  public boolean hasParameterDetails() {
    return parameterName != null;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(super.toString());
    if (hasParameterDetails()) {
      sb.append(" (parameter: ")
          .append(parameterName)
          .append(" = ")
          .append(parameterValue)
          .append(")");
    }
    return sb.toString();
  }
}
