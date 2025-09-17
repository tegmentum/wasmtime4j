package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/**
 * Base configuration for data export operations.
 *
 * @since 1.0.0
 */
public abstract class ExportConfiguration {
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
