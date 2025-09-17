package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for CSV export operations.
 *
 * @since 1.0.0
 */
public final class CsvConfiguration extends ExportConfiguration {
  private final CsvLayout layout;
  private final String delimiter;
  private final String quoteChar;
  private final String lineEnding;
  private final List<String> customColumns;
  private final boolean includeHeaders;

  private CsvConfiguration(final Builder builder) {
    super(builder.format, builder.includeMetadata, builder.compressOutput, builder.bufferSize);
    this.layout = builder.layout;
    this.delimiter = builder.delimiter;
    this.quoteChar = builder.quoteChar;
    this.lineEnding = builder.lineEnding;
    this.customColumns = List.copyOf(builder.customColumns);
    this.includeHeaders = builder.includeHeaders;
  }

  public CsvLayout getLayout() {
    return layout;
  }

  public String getDelimiter() {
    return delimiter;
  }

  public String getQuoteChar() {
    return quoteChar;
  }

  public String getLineEnding() {
    return lineEnding;
  }

  public List<String> getCustomColumns() {
    return customColumns;
  }

  public boolean isIncludeHeaders() {
    return includeHeaders;
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

    final CsvConfiguration that = (CsvConfiguration) obj;
    return includeHeaders == that.includeHeaders
        && layout == that.layout
        && Objects.equals(delimiter, that.delimiter)
        && Objects.equals(quoteChar, that.quoteChar)
        && Objects.equals(lineEnding, that.lineEnding)
        && Objects.equals(customColumns, that.customColumns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(), layout, delimiter, quoteChar, lineEnding, customColumns, includeHeaders);
  }

  /**
   * Creates a new builder for CsvConfiguration.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for CsvConfiguration. */
  public static final class Builder {
    private final ExportFormat format = ExportFormat.CSV;
    private boolean includeMetadata = true;
    private boolean compressOutput = false;
    private int bufferSize = 8192;
    private CsvLayout layout = CsvLayout.SUMMARY;
    private String delimiter = ",";
    private String quoteChar = "\"";
    private String lineEnding = "\n";
    private Set<String> customColumns = new LinkedHashSet<>();
    private boolean includeHeaders = true;

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

    public Builder layout(final CsvLayout layout) {
      this.layout = Objects.requireNonNull(layout, "layout cannot be null");
      return this;
    }

    public Builder delimiter(final String delimiter) {
      this.delimiter = Objects.requireNonNull(delimiter, "delimiter cannot be null");
      return this;
    }

    public Builder quoteChar(final String quoteChar) {
      this.quoteChar = Objects.requireNonNull(quoteChar, "quoteChar cannot be null");
      return this;
    }

    public Builder lineEnding(final String lineEnding) {
      this.lineEnding = Objects.requireNonNull(lineEnding, "lineEnding cannot be null");
      return this;
    }

    public Builder customColumns(final List<String> customColumns) {
      this.customColumns = new LinkedHashSet<>(customColumns);
      return this;
    }

    public Builder addCustomColumn(final String column) {
      this.customColumns.add(Objects.requireNonNull(column, "column cannot be null"));
      return this;
    }

    public Builder includeHeaders(final boolean includeHeaders) {
      this.includeHeaders = includeHeaders;
      return this;
    }

    public CsvConfiguration build() {
      return new CsvConfiguration(this);
    }
  }
}
