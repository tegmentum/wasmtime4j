package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/**
 * Configuration for report formatting including colors, timestamps, line numbers, and output style.
 *
 * @since 1.0.0
 */
public final class FormattingConfiguration {
  private final boolean useColorOutput;
  private final boolean includeTimestamps;
  private final boolean includeLineNumbers;
  private final DateTimeFormat dateTimeFormat;
  private final NumberFormat numberFormat;
  private final int maxLineLength;
  private final boolean compactOutput;
  private final boolean includeStackTraces;

  private FormattingConfiguration(final Builder builder) {
    this.useColorOutput = builder.useColorOutput;
    this.includeTimestamps = builder.includeTimestamps;
    this.includeLineNumbers = builder.includeLineNumbers;
    this.dateTimeFormat = builder.dateTimeFormat;
    this.numberFormat = builder.numberFormat;
    this.maxLineLength = builder.maxLineLength;
    this.compactOutput = builder.compactOutput;
    this.includeStackTraces = builder.includeStackTraces;
  }

  public boolean isUseColorOutput() {
    return useColorOutput;
  }

  public boolean isIncludeTimestamps() {
    return includeTimestamps;
  }

  public boolean isIncludeLineNumbers() {
    return includeLineNumbers;
  }

  public DateTimeFormat getDateTimeFormat() {
    return dateTimeFormat;
  }

  public NumberFormat getNumberFormat() {
    return numberFormat;
  }

  public int getMaxLineLength() {
    return maxLineLength;
  }

  public boolean isCompactOutput() {
    return compactOutput;
  }

  public boolean isIncludeStackTraces() {
    return includeStackTraces;
  }

  /** Creates default formatting configuration. */
  public static FormattingConfiguration defaultFormattingConfig() {
    return new Builder()
        .useColorOutput(true)
        .includeTimestamps(true)
        .includeLineNumbers(false)
        .dateTimeFormat(DateTimeFormat.ISO_8601)
        .numberFormat(NumberFormat.STANDARD)
        .maxLineLength(120)
        .compactOutput(false)
        .includeStackTraces(false)
        .build();
  }

  /** Creates compact formatting configuration. */
  public static FormattingConfiguration compactFormattingConfig() {
    return new Builder()
        .useColorOutput(false)
        .includeTimestamps(false)
        .includeLineNumbers(false)
        .dateTimeFormat(DateTimeFormat.SHORT)
        .numberFormat(NumberFormat.COMPACT)
        .maxLineLength(80)
        .compactOutput(true)
        .includeStackTraces(false)
        .build();
  }

  /** Creates detailed formatting configuration. */
  public static FormattingConfiguration detailedFormattingConfig() {
    return new Builder()
        .useColorOutput(true)
        .includeTimestamps(true)
        .includeLineNumbers(true)
        .dateTimeFormat(DateTimeFormat.FULL)
        .numberFormat(NumberFormat.DETAILED)
        .maxLineLength(150)
        .compactOutput(false)
        .includeStackTraces(true)
        .build();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final FormattingConfiguration that = (FormattingConfiguration) obj;
    return useColorOutput == that.useColorOutput
        && includeTimestamps == that.includeTimestamps
        && includeLineNumbers == that.includeLineNumbers
        && maxLineLength == that.maxLineLength
        && compactOutput == that.compactOutput
        && includeStackTraces == that.includeStackTraces
        && dateTimeFormat == that.dateTimeFormat
        && numberFormat == that.numberFormat;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        useColorOutput,
        includeTimestamps,
        includeLineNumbers,
        dateTimeFormat,
        numberFormat,
        maxLineLength,
        compactOutput,
        includeStackTraces);
  }

  @Override
  public String toString() {
    return "FormattingConfiguration{"
        + "colors="
        + useColorOutput
        + ", compact="
        + compactOutput
        + ", maxLength="
        + maxLineLength
        + ", dateFormat="
        + dateTimeFormat
        + '}';
  }

  /** Builder for FormattingConfiguration. */
  public static final class Builder {
    private boolean useColorOutput = true;
    private boolean includeTimestamps = true;
    private boolean includeLineNumbers = false;
    private DateTimeFormat dateTimeFormat = DateTimeFormat.ISO_8601;
    private NumberFormat numberFormat = NumberFormat.STANDARD;
    private int maxLineLength = 120;
    private boolean compactOutput = false;
    private boolean includeStackTraces = false;

    public Builder useColorOutput(final boolean useColorOutput) {
      this.useColorOutput = useColorOutput;
      return this;
    }

    public Builder includeTimestamps(final boolean includeTimestamps) {
      this.includeTimestamps = includeTimestamps;
      return this;
    }

    /**
     * Sets whether to include line numbers in the report.
     *
     * @param includeLineNumbers true to include line numbers
     * @return this builder instance
     */
    public Builder includeLineNumbers(final boolean includeLineNumbers) {
      this.includeLineNumbers = includeLineNumbers;
      return this;
    }

    /**
     * Sets the date time format for the report.
     *
     * @param dateTimeFormat the date time format to set
     * @return this builder instance
     */
    public Builder dateTimeFormat(final DateTimeFormat dateTimeFormat) {
      this.dateTimeFormat = Objects.requireNonNull(dateTimeFormat, "dateTimeFormat cannot be null");
      return this;
    }

    /**
     * Sets the number format for the report.
     *
     * @param numberFormat the number format to set
     * @return this builder instance
     */
    public Builder numberFormat(final NumberFormat numberFormat) {
      this.numberFormat = Objects.requireNonNull(numberFormat, "numberFormat cannot be null");
      return this;
    }

    /**
     * Sets the maximum line length for the report.
     *
     * @param maxLineLength the maximum line length to set
     * @return this builder instance
     */
    public Builder maxLineLength(final int maxLineLength) {
      if (maxLineLength <= 0) {
        throw new IllegalArgumentException("maxLineLength must be positive");
      }
      this.maxLineLength = maxLineLength;
      return this;
    }

    public Builder compactOutput(final boolean compactOutput) {
      this.compactOutput = compactOutput;
      return this;
    }

    public Builder includeStackTraces(final boolean includeStackTraces) {
      this.includeStackTraces = includeStackTraces;
      return this;
    }

    public FormattingConfiguration build() {
      return new FormattingConfiguration(this);
    }
  }
}
