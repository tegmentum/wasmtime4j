package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Strategy interface for exporting ComparisonReport data in different formats.
 *
 * <p>Implementations provide specific export functionality for JSON, CSV, HTML, and other formats
 * with configurable output options and streaming support for large datasets.
 *
 * @param <T> the configuration type for this exporter
 * @since 1.0.0
 */
public interface DataExporter<T extends ExportConfiguration> {

  /**
   * Exports the comparison report using the specified configuration to the output stream.
   *
   * @param report the comparison report to export
   * @param configuration the export configuration
   * @param output the output stream to write to
   * @throws IOException if an I/O error occurs during export
   * @throws ExportException if export-specific error occurs
   */
  void export(ComparisonReport report, T configuration, OutputStream output)
      throws IOException, ExportException;

  /**
   * Gets the export format supported by this exporter.
   *
   * @return the export format
   */
  ExportFormat getFormat();

  /**
   * Gets the schema definition for this export format.
   *
   * @return the schema definition
   */
  ExportSchema getSchema();

  /**
   * Validates that the configuration is compatible with this exporter.
   *
   * @param configuration the configuration to validate
   * @throws IllegalArgumentException if configuration is invalid
   */
  default void validateConfiguration(final T configuration) {
    Objects.requireNonNull(configuration, "configuration cannot be null");
    if (!configuration.getFormat().equals(getFormat())) {
      throw new IllegalArgumentException(
          "Configuration format "
              + configuration.getFormat()
              + " does not match exporter format "
              + getFormat());
    }
  }

  /**
   * Estimates the output size for the given report and configuration.
   *
   * <p>Used for memory management and progress reporting.
   *
   * @param report the report to analyze
   * @param configuration the export configuration
   * @return estimated output size in bytes, or -1 if cannot estimate
   */
  default long estimateOutputSize(final ComparisonReport report, final T configuration) {
    return -1; // Default: cannot estimate
  }

  /**
   * Checks if this exporter supports streaming for large datasets.
   *
   * @return true if streaming is supported
   */
  default boolean supportsStreaming() {
    return false;
  }
}
