package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/**
 * Configuration for report output formats and caching options.
 *
 * @since 1.0.0
 */
public final class OutputConfiguration {
  private final boolean generateHtml;
  private final boolean generateJson;
  private final boolean generateCsv;
  private final boolean generateConsole;
  private final boolean enableCaching;
  private final int maxCacheSize;
  private final long cacheExpirationMinutes;
  private final boolean streamOutput;
  private final int outputBufferSize;

  private OutputConfiguration(final Builder builder) {
    this.generateHtml = builder.generateHtml;
    this.generateJson = builder.generateJson;
    this.generateCsv = builder.generateCsv;
    this.generateConsole = builder.generateConsole;
    this.enableCaching = builder.enableCaching;
    this.maxCacheSize = builder.maxCacheSize;
    this.cacheExpirationMinutes = builder.cacheExpirationMinutes;
    this.streamOutput = builder.streamOutput;
    this.outputBufferSize = builder.outputBufferSize;
  }

  public boolean isGenerateHtml() {
    return generateHtml;
  }

  public boolean isGenerateJson() {
    return generateJson;
  }

  public boolean isGenerateCsv() {
    return generateCsv;
  }

  public boolean isGenerateConsole() {
    return generateConsole;
  }

  public boolean isEnableCaching() {
    return enableCaching;
  }

  public int getMaxCacheSize() {
    return maxCacheSize;
  }

  public long getCacheExpirationMinutes() {
    return cacheExpirationMinutes;
  }

  public boolean isStreamOutput() {
    return streamOutput;
  }

  public int getOutputBufferSize() {
    return outputBufferSize;
  }

  /** Creates default output configuration. */
  public static OutputConfiguration defaultOutputConfig() {
    return new Builder()
        .generateHtml(true)
        .generateJson(true)
        .generateCsv(false)
        .generateConsole(true)
        .enableCaching(true)
        .maxCacheSize(100)
        .cacheExpirationMinutes(60)
        .streamOutput(false)
        .outputBufferSize(8192)
        .build();
  }

  /** Creates fast output configuration optimized for speed. */
  public static OutputConfiguration fastOutputConfig() {
    return new Builder()
        .generateHtml(false)
        .generateJson(true)
        .generateCsv(false)
        .generateConsole(true)
        .enableCaching(false)
        .streamOutput(true)
        .outputBufferSize(4096)
        .build();
  }

  /** Creates detailed output configuration with all formats. */
  public static OutputConfiguration detailedOutputConfig() {
    return new Builder()
        .generateHtml(true)
        .generateJson(true)
        .generateCsv(true)
        .generateConsole(true)
        .enableCaching(true)
        .maxCacheSize(500)
        .cacheExpirationMinutes(120)
        .streamOutput(false)
        .outputBufferSize(16384)
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

    final OutputConfiguration that = (OutputConfiguration) obj;
    return generateHtml == that.generateHtml
        && generateJson == that.generateJson
        && generateCsv == that.generateCsv
        && generateConsole == that.generateConsole
        && enableCaching == that.enableCaching
        && maxCacheSize == that.maxCacheSize
        && cacheExpirationMinutes == that.cacheExpirationMinutes
        && streamOutput == that.streamOutput
        && outputBufferSize == that.outputBufferSize;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        generateHtml,
        generateJson,
        generateCsv,
        generateConsole,
        enableCaching,
        maxCacheSize,
        cacheExpirationMinutes,
        streamOutput,
        outputBufferSize);
  }

  @Override
  public String toString() {
    return "OutputConfiguration{"
        + "html="
        + generateHtml
        + ", json="
        + generateJson
        + ", csv="
        + generateCsv
        + ", console="
        + generateConsole
        + ", caching="
        + enableCaching
        + '}';
  }

  /** Builder for OutputConfiguration. */
  public static final class Builder {
    private boolean generateHtml = true;
    private boolean generateJson = true;
    private boolean generateCsv = false;
    private boolean generateConsole = true;
    private boolean enableCaching = true;
    private int maxCacheSize = 100;
    private long cacheExpirationMinutes = 60;
    private boolean streamOutput = false;
    private int outputBufferSize = 8192;

    /**
     * Sets whether to generate HTML output.
     *
     * @param generateHtml true to generate HTML output
     * @return this builder instance
     */
    public Builder generateHtml(final boolean generateHtml) {
      this.generateHtml = generateHtml;
      return this;
    }

    /**
     * Sets whether to generate JSON output.
     *
     * @param generateJson true to generate JSON output
     * @return this builder instance
     */
    public Builder generateJson(final boolean generateJson) {
      this.generateJson = generateJson;
      return this;
    }

    /**
     * Sets whether to generate CSV output.
     *
     * @param generateCsv true to generate CSV output
     * @return this builder instance
     */
    public Builder generateCsv(final boolean generateCsv) {
      this.generateCsv = generateCsv;
      return this;
    }

    /**
     * Sets whether to generate console output.
     *
     * @param generateConsole true to generate console output
     * @return this builder instance
     */
    public Builder generateConsole(final boolean generateConsole) {
      this.generateConsole = generateConsole;
      return this;
    }

    public Builder enableCaching(final boolean enableCaching) {
      this.enableCaching = enableCaching;
      return this;
    }

    /**
     * Sets the maximum cache size.
     *
     * @param maxCacheSize the maximum cache size to set
     * @return this builder instance
     */
    public Builder maxCacheSize(final int maxCacheSize) {
      if (maxCacheSize < 0) {
        throw new IllegalArgumentException("maxCacheSize cannot be negative");
      }
      this.maxCacheSize = maxCacheSize;
      return this;
    }

    /**
     * Sets the cache expiration time in minutes.
     *
     * @param cacheExpirationMinutes the cache expiration time to set
     * @return this builder instance
     */
    public Builder cacheExpirationMinutes(final long cacheExpirationMinutes) {
      if (cacheExpirationMinutes < 0) {
        throw new IllegalArgumentException("cacheExpirationMinutes cannot be negative");
      }
      this.cacheExpirationMinutes = cacheExpirationMinutes;
      return this;
    }

    /**
     * Sets whether to use streaming output.
     *
     * @param streamOutput true to enable streaming output
     * @return this builder instance
     */
    public Builder streamOutput(final boolean streamOutput) {
      this.streamOutput = streamOutput;
      return this;
    }

    /**
     * Sets the output buffer size.
     *
     * @param outputBufferSize the output buffer size to set
     * @return this builder instance
     */
    public Builder outputBufferSize(final int outputBufferSize) {
      if (outputBufferSize <= 0) {
        throw new IllegalArgumentException("outputBufferSize must be positive");
      }
      this.outputBufferSize = outputBufferSize;
      return this;
    }

    public OutputConfiguration build() {
      return new OutputConfiguration(this);
    }
  }
}