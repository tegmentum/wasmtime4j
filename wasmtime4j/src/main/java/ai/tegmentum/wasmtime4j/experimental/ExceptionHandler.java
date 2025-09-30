package ai.tegmentum.wasmtime4j.experimental;

/**
 * Experimental exception handler interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExceptionHandler {

  /** Exception handling result enumeration. */
  enum HandlingResult {
    /** Exception was handled successfully. */
    HANDLED,
    /** Exception was not handled. */
    NOT_HANDLED,
    /** Exception handling failed. */
    FAILED
  }

  /**
   * Handles the given exception.
   *
   * @param exception the exception to handle
   * @return handling result
   */
  HandlingResult handle(Throwable exception);

  /**
   * Gets the handler name.
   *
   * @return handler name
   */
  String getHandlerName();

  /**
   * Checks if the handler is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /** Exception tag for experimental exception handling. */
  interface ExceptionTag {
    /**
     * Gets the tag name.
     *
     * @return tag name
     */
    String getTagName();

    /**
     * Gets the tag type.
     *
     * @return tag type
     */
    String getTagType();
  }

  /** Exception handling configuration. */
  interface ExceptionHandlingConfig {
    /**
     * Gets the handler timeout in milliseconds.
     *
     * @return timeout
     */
    long getTimeout();

    /**
     * Checks if retry is enabled.
     *
     * @return true if retry is enabled
     */
    boolean isRetryEnabled();

    /**
     * Gets the maximum retry attempts.
     *
     * @return max retry attempts
     */
    int getMaxRetries();
  }
}
