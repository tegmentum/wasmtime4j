package ai.tegmentum.wasmtime4j.wasi;

/**
 * Represents an error that occurred during a WASI polling operation.
 *
 * <p>This class contains information about errors that occur when waiting for events, such as
 * file descriptor errors or timer failures.
 *
 * @since 1.0.0
 */
public final class WasiEventError {

  private final int errorCode;
  private final String message;

  /**
   * Creates a new event error.
   *
   * @param errorCode the WASI error code
   * @param message a human-readable error message
   */
  public WasiEventError(final int errorCode, final String message) {
    this.errorCode = errorCode;
    this.message = message;
  }

  /**
   * Gets the WASI error code.
   *
   * <p>This corresponds to the WASI errno values defined in the WASI specification.
   *
   * @return the error code
   */
  public int getErrorCode() {
    return errorCode;
  }

  /**
   * Gets the human-readable error message.
   *
   * @return the error message
   */
  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return String.format("WasiEventError{code=%d, message='%s'}", errorCode, message);
  }
}