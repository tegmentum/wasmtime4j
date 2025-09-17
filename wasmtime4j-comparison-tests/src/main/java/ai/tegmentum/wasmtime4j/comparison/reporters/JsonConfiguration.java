package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/**
 * Configuration for JSON export operations.
 *
 * @since 1.0.0
 */
public final class JsonConfiguration extends ExportConfiguration {
  private final JsonDetailLevel detailLevel;
  private final boolean streamingMode;
  private final boolean prettyPrint;

  private JsonConfiguration(final Builder builder) {
    super(builder.format, builder.includeMetadata, builder.compressOutput, builder.bufferSize);
    this.detailLevel = builder.detailLevel;
    this.streamingMode = builder.streamingMode;
    this.prettyPrint = builder.prettyPrint;
  }

  public JsonDetailLevel getDetailLevel() {
    return detailLevel;
  }

  public boolean isStreamingMode() {
    return streamingMode;
  }

  public boolean isPrettyPrint() {
    return prettyPrint;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }

    final JsonConfiguration that = (JsonConfiguration) obj;
    return streamingMode == that.streamingMode
        && prettyPrint == that.prettyPrint
        && detailLevel == that.detailLevel;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), detailLevel, streamingMode, prettyPrint);
  }

  /**
   * Creates a new builder for JsonConfiguration.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for JsonConfiguration.
   *
   * @since 1.0.0
   */
  public static final class Builder {
    private final ExportFormat format = ExportFormat.JSON;
    private boolean includeMetadata = true;
    private boolean compressOutput = false;
    private int bufferSize = 8192;
    private JsonDetailLevel detailLevel = JsonDetailLevel.SUMMARY;
    private boolean streamingMode = false;
    private boolean prettyPrint = false;

    public Builder includeMetadata(final boolean includeMetadata) {
      this.includeMetadata = includeMetadata;
      return this;
    }

    public Builder compressOutput(final boolean compressOutput) {
      this.compressOutput = compressOutput;
      return this;
    }

    public Builder bufferSize(final int bufferSize) {
      this.bufferSize = bufferSize;
      return this;
    }

    public Builder detailLevel(final JsonDetailLevel detailLevel) {
      this.detailLevel = Objects.requireNonNull(detailLevel, "detailLevel cannot be null");
      return this;
    }

    public Builder streamingMode(final boolean streamingMode) {
      this.streamingMode = streamingMode;
      return this;
    }

    public Builder prettyPrint(final boolean prettyPrint) {
      this.prettyPrint = prettyPrint;
      return this;
    }

    public JsonConfiguration build() {
      return new JsonConfiguration(this);
    }
  }
}
