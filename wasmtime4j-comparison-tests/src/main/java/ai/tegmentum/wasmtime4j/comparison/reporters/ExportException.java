package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * Exception thrown during export operations.
 *
 * @since 1.0.0
 */
public final class ExportException extends Exception {
  private final ExportFormat format;
  private final String phase;

  /**
   * Constructs a new ExportException with the specified message, format, and phase.
   *
   * @param message the exception message
   * @param format the export format
   * @param phase the export phase
   */
  public ExportException(final String message, final ExportFormat format, final String phase) {
    super(message);
    this.format = format;
    this.phase = phase;
  }

  /**
   * Constructs a new ExportException with the specified message, cause, format, and phase.
   *
   * @param message the exception message
   * @param cause the cause of the exception
   * @param format the export format
   * @param phase the export phase
   */
  public ExportException(
      final String message, final Throwable cause, final ExportFormat format, final String phase) {
    super(message, cause);
    this.format = format;
    this.phase = phase;
  }

  public ExportFormat getFormat() {
    return format;
  }

  public String getPhase() {
    return phase;
  }

  @Override
  public String toString() {
    return "ExportException{"
        + "format="
        + format
        + ", phase='"
        + phase
        + '\''
        + ", message='"
        + getMessage()
        + '\''
        + '}';
  }
}
