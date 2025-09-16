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

/** Base configuration for data export operations. */
abstract class ExportConfiguration {
  private final ExportFormat format;
  private final boolean includeMetadata;
  private final boolean compressOutput;
  private final int bufferSize;

  protected ExportConfiguration(
      final ExportFormat format,
      final boolean includeMetadata,
      final boolean compressOutput,
      final int bufferSize) {
    this.format = Objects.requireNonNull(format, "format cannot be null");
    this.includeMetadata = includeMetadata;
    this.compressOutput = compressOutput;
    this.bufferSize = Math.max(1024, bufferSize); // Minimum 1KB buffer
  }

  public ExportFormat getFormat() {
    return format;
  }

  public boolean isIncludeMetadata() {
    return includeMetadata;
  }

  public boolean isCompressOutput() {
    return compressOutput;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ExportConfiguration that = (ExportConfiguration) obj;
    return includeMetadata == that.includeMetadata
        && compressOutput == that.compressOutput
        && bufferSize == that.bufferSize
        && format == that.format;
  }

  @Override
  public int hashCode() {
    return Objects.hash(format, includeMetadata, compressOutput, bufferSize);
  }
}

/** Supported export formats. */
enum ExportFormat {
  JSON("application/json", ".json"),
  CSV("text/csv", ".csv"),
  XML("application/xml", ".xml"),
  HTML("text/html", ".html");

  private final String mimeType;
  private final String fileExtension;

  ExportFormat(final String mimeType, final String fileExtension) {
    this.mimeType = mimeType;
    this.fileExtension = fileExtension;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getFileExtension() {
    return fileExtension;
  }
}

/** Schema definition for export formats with versioning support. */
final class ExportSchema {
  private final ExportFormat format;
  private final String version;
  private final String description;
  private final String schemaDefinition;

  public ExportSchema(
      final ExportFormat format,
      final String version,
      final String description,
      final String schemaDefinition) {
    this.format = Objects.requireNonNull(format, "format cannot be null");
    this.version = Objects.requireNonNull(version, "version cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.schemaDefinition =
        Objects.requireNonNull(schemaDefinition, "schemaDefinition cannot be null");
  }

  public ExportFormat getFormat() {
    return format;
  }

  public String getVersion() {
    return version;
  }

  public String getDescription() {
    return description;
  }

  public String getSchemaDefinition() {
    return schemaDefinition;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ExportSchema that = (ExportSchema) obj;
    return format == that.format
        && Objects.equals(version, that.version)
        && Objects.equals(description, that.description)
        && Objects.equals(schemaDefinition, that.schemaDefinition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(format, version, description, schemaDefinition);
  }

  @Override
  public String toString() {
    return "ExportSchema{"
        + "format="
        + format
        + ", version='"
        + version
        + '\''
        + ", description='"
        + description
        + '\''
        + '}';
  }
}

/** Exception thrown during export operations. */
final class ExportException extends Exception {
  private final ExportFormat format;
  private final String phase;

  public ExportException(final String message, final ExportFormat format, final String phase) {
    super(message);
    this.format = format;
    this.phase = phase;
  }

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
