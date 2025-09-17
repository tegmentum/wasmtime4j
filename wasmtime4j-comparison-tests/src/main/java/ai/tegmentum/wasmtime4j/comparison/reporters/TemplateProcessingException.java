package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * Exception thrown when template processing fails.
 *
 * @since 1.0.0
 */
public final class TemplateProcessingException extends Exception {
  public TemplateProcessingException(final String message) {
    super(message);
  }

  public TemplateProcessingException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
