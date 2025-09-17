package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Comprehensive configuration for report generation controlling content inclusion, formatting,
 * themes, and localization settings.
 *
 * @since 1.0.0
 */
public final class ReportConfiguration {
  private final String configurationName;
  private final ContentConfiguration contentConfig;
  private final FormattingConfiguration formattingConfig;
  private final ThemeConfiguration themeConfig;
  private final LocalizationConfiguration localizationConfig;
  private final OutputConfiguration outputConfig;
  private final Map<String, Object> customProperties;

  private ReportConfiguration(final Builder builder) {
    this.configurationName =
        Objects.requireNonNull(builder.configurationName, "configurationName cannot be null");
    this.contentConfig =
        Objects.requireNonNull(builder.contentConfig, "contentConfig cannot be null");
    this.formattingConfig =
        Objects.requireNonNull(builder.formattingConfig, "formattingConfig cannot be null");
    this.themeConfig = Objects.requireNonNull(builder.themeConfig, "themeConfig cannot be null");
    this.localizationConfig =
        Objects.requireNonNull(builder.localizationConfig, "localizationConfig cannot be null");
    this.outputConfig = Objects.requireNonNull(builder.outputConfig, "outputConfig cannot be null");
    this.customProperties = Map.copyOf(builder.customProperties);
  }

  public String getConfigurationName() {
    return configurationName;
  }

  public ContentConfiguration getContentConfig() {
    return contentConfig;
  }

  public FormattingConfiguration getFormattingConfig() {
    return formattingConfig;
  }

  public ThemeConfiguration getThemeConfig() {
    return themeConfig;
  }

  public LocalizationConfiguration getLocalizationConfig() {
    return localizationConfig;
  }

  public OutputConfiguration getOutputConfig() {
    return outputConfig;
  }

  public Map<String, Object> getCustomProperties() {
    return customProperties;
  }

  /**
   * Gets a custom property value with type safety.
   *
   * @param key the property key
   * @param clazz the expected type
   * @param <T> the property type
   * @return the property value or empty if not found or wrong type
   */
  @SuppressWarnings("unchecked")
  public <T> java.util.Optional<T> getCustomProperty(final String key, final Class<T> clazz) {
    final Object value = customProperties.get(key);
    if (value != null && clazz.isInstance(value)) {
      return java.util.Optional.of((T) value);
    }
    return java.util.Optional.empty();
  }

  /**
   * Creates a default configuration suitable for most use cases.
   *
   * @return default report configuration
   */
  public static ReportConfiguration defaultConfiguration() {
    return new Builder("default")
        .contentConfig(ContentConfiguration.defaultContentConfig())
        .formattingConfig(FormattingConfiguration.defaultFormattingConfig())
        .themeConfig(ThemeConfiguration.defaultTheme())
        .localizationConfig(LocalizationConfiguration.defaultLocalization())
        .outputConfig(OutputConfiguration.defaultOutputConfig())
        .build();
  }

  /**
   * Creates a minimal configuration with only essential content.
   *
   * @return minimal report configuration
   */
  public static ReportConfiguration minimalConfiguration() {
    return new Builder("minimal")
        .contentConfig(ContentConfiguration.minimalContentConfig())
        .formattingConfig(FormattingConfiguration.compactFormattingConfig())
        .themeConfig(ThemeConfiguration.minimalTheme())
        .localizationConfig(LocalizationConfiguration.defaultLocalization())
        .outputConfig(OutputConfiguration.fastOutputConfig())
        .build();
  }

  /**
   * Creates a comprehensive configuration with all content sections enabled.
   *
   * @return comprehensive report configuration
   */
  public static ReportConfiguration comprehensiveConfiguration() {
    return new Builder("comprehensive")
        .contentConfig(ContentConfiguration.comprehensiveContentConfig())
        .formattingConfig(FormattingConfiguration.detailedFormattingConfig())
        .themeConfig(ThemeConfiguration.defaultTheme())
        .localizationConfig(LocalizationConfiguration.defaultLocalization())
        .outputConfig(OutputConfiguration.detailedOutputConfig())
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

    final ReportConfiguration that = (ReportConfiguration) obj;
    return Objects.equals(configurationName, that.configurationName)
        && Objects.equals(contentConfig, that.contentConfig)
        && Objects.equals(formattingConfig, that.formattingConfig)
        && Objects.equals(themeConfig, that.themeConfig)
        && Objects.equals(localizationConfig, that.localizationConfig)
        && Objects.equals(outputConfig, that.outputConfig)
        && Objects.equals(customProperties, that.customProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        configurationName,
        contentConfig,
        formattingConfig,
        themeConfig,
        localizationConfig,
        outputConfig,
        customProperties);
  }

  @Override
  public String toString() {
    return "ReportConfiguration{"
        + "name='"
        + configurationName
        + '\''
        + ", content="
        + contentConfig
        + ", theme="
        + themeConfig.getThemeName()
        + ", locale="
        + localizationConfig.getLocale()
        + '}';
  }

  /** Builder for ReportConfiguration. */
  public static final class Builder {
    private final String configurationName;
    private ContentConfiguration contentConfig = ContentConfiguration.defaultContentConfig();
    private FormattingConfiguration formattingConfig =
        FormattingConfiguration.defaultFormattingConfig();
    private ThemeConfiguration themeConfig = ThemeConfiguration.defaultTheme();
    private LocalizationConfiguration localizationConfig =
        LocalizationConfiguration.defaultLocalization();
    private OutputConfiguration outputConfig = OutputConfiguration.defaultOutputConfig();
    private Map<String, Object> customProperties = Collections.emptyMap();

    public Builder(final String configurationName) {
      this.configurationName =
          Objects.requireNonNull(configurationName, "configurationName cannot be null");
    }

    public Builder contentConfig(final ContentConfiguration contentConfig) {
      this.contentConfig = Objects.requireNonNull(contentConfig, "contentConfig cannot be null");
      return this;
    }

    /**
     * Sets the formatting configuration for the report.
     *
     * @param formattingConfig the formatting configuration
     * @return this builder
     */
    public Builder formattingConfig(final FormattingConfiguration formattingConfig) {
      this.formattingConfig =
          Objects.requireNonNull(formattingConfig, "formattingConfig cannot be null");
      return this;
    }

    public Builder themeConfig(final ThemeConfiguration themeConfig) {
      this.themeConfig = Objects.requireNonNull(themeConfig, "themeConfig cannot be null");
      return this;
    }

    /**
     * Sets the localization configuration for the report.
     *
     * @param localizationConfig the localization configuration
     * @return this builder
     */
    public Builder localizationConfig(final LocalizationConfiguration localizationConfig) {
      this.localizationConfig =
          Objects.requireNonNull(localizationConfig, "localizationConfig cannot be null");
      return this;
    }

    public Builder outputConfig(final OutputConfiguration outputConfig) {
      this.outputConfig = Objects.requireNonNull(outputConfig, "outputConfig cannot be null");
      return this;
    }

    /**
     * Sets custom properties for the report configuration.
     *
     * @param customProperties the custom properties map
     * @return this builder
     */
    public Builder customProperties(final Map<String, Object> customProperties) {
      this.customProperties =
          Objects.requireNonNull(customProperties, "customProperties cannot be null");
      return this;
    }

    public ReportConfiguration build() {
      return new ReportConfiguration(this);
    }
  }

  /** Configuration for report content inclusion and exclusion. */
  public static final class ContentConfiguration {
    private final boolean includeSummary;
    private final boolean includeMetadata;
    private final boolean includeBehavioralAnalysis;
    private final boolean includePerformanceAnalysis;
    private final boolean includeCoverageAnalysis;
    private final boolean includeRecommendations;
    private final boolean includeDetailedResults;
    private final boolean includeRawData;
    private final Set<String> excludedTests;
    private final Set<String> includedSections;
    private final int maxTestResults;
    private final RecommendationLevel recommendationLevel;

    private ContentConfiguration(final Builder builder) {
      this.includeSummary = builder.includeSummary;
      this.includeMetadata = builder.includeMetadata;
      this.includeBehavioralAnalysis = builder.includeBehavioralAnalysis;
      this.includePerformanceAnalysis = builder.includePerformanceAnalysis;
      this.includeCoverageAnalysis = builder.includeCoverageAnalysis;
      this.includeRecommendations = builder.includeRecommendations;
      this.includeDetailedResults = builder.includeDetailedResults;
      this.includeRawData = builder.includeRawData;
      this.excludedTests = Set.copyOf(builder.excludedTests);
      this.includedSections = Set.copyOf(builder.includedSections);
      this.maxTestResults = builder.maxTestResults;
      this.recommendationLevel = builder.recommendationLevel;
    }

    public boolean isIncludeSummary() {
      return includeSummary;
    }

    public boolean isIncludeMetadata() {
      return includeMetadata;
    }

    public boolean isIncludeBehavioralAnalysis() {
      return includeBehavioralAnalysis;
    }

    public boolean isIncludePerformanceAnalysis() {
      return includePerformanceAnalysis;
    }

    public boolean isIncludeCoverageAnalysis() {
      return includeCoverageAnalysis;
    }

    public boolean isIncludeRecommendations() {
      return includeRecommendations;
    }

    public boolean isIncludeDetailedResults() {
      return includeDetailedResults;
    }

    public boolean isIncludeRawData() {
      return includeRawData;
    }

    public Set<String> getExcludedTests() {
      return excludedTests;
    }

    public Set<String> getIncludedSections() {
      return includedSections;
    }

    public int getMaxTestResults() {
      return maxTestResults;
    }

    public RecommendationLevel getRecommendationLevel() {
      return recommendationLevel;
    }

    /**
     * Checks if a test should be included in the report.
     *
     * @param testName the test name
     * @return true if test should be included
     */
    public boolean shouldIncludeTest(final String testName) {
      return !excludedTests.contains(testName);
    }

    /**
     * Checks if a section should be included in the report.
     *
     * @param sectionName the section name
     * @return true if section should be included
     */
    public boolean shouldIncludeSection(final String sectionName) {
      return includedSections.isEmpty() || includedSections.contains(sectionName);
    }

    /** Creates default content configuration. */
    public static ContentConfiguration defaultContentConfig() {
      return new Builder()
          .includeSummary(true)
          .includeMetadata(true)
          .includeBehavioralAnalysis(true)
          .includePerformanceAnalysis(true)
          .includeCoverageAnalysis(true)
          .includeRecommendations(true)
          .includeDetailedResults(true)
          .includeRawData(false)
          .maxTestResults(1000)
          .recommendationLevel(RecommendationLevel.MEDIUM)
          .build();
    }

    /** Creates minimal content configuration. */
    public static ContentConfiguration minimalContentConfig() {
      return new Builder()
          .includeSummary(true)
          .includeMetadata(false)
          .includeBehavioralAnalysis(false)
          .includePerformanceAnalysis(false)
          .includeCoverageAnalysis(false)
          .includeRecommendations(true)
          .includeDetailedResults(false)
          .includeRawData(false)
          .maxTestResults(100)
          .recommendationLevel(RecommendationLevel.HIGH)
          .build();
    }

    /** Creates comprehensive content configuration. */
    public static ContentConfiguration comprehensiveContentConfig() {
      return new Builder()
          .includeSummary(true)
          .includeMetadata(true)
          .includeBehavioralAnalysis(true)
          .includePerformanceAnalysis(true)
          .includeCoverageAnalysis(true)
          .includeRecommendations(true)
          .includeDetailedResults(true)
          .includeRawData(true)
          .maxTestResults(Integer.MAX_VALUE)
          .recommendationLevel(RecommendationLevel.ALL)
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

      final ContentConfiguration that = (ContentConfiguration) obj;
      return includeSummary == that.includeSummary
          && includeMetadata == that.includeMetadata
          && includeBehavioralAnalysis == that.includeBehavioralAnalysis
          && includePerformanceAnalysis == that.includePerformanceAnalysis
          && includeCoverageAnalysis == that.includeCoverageAnalysis
          && includeRecommendations == that.includeRecommendations
          && includeDetailedResults == that.includeDetailedResults
          && includeRawData == that.includeRawData
          && maxTestResults == that.maxTestResults
          && Objects.equals(excludedTests, that.excludedTests)
          && Objects.equals(includedSections, that.includedSections)
          && recommendationLevel == that.recommendationLevel;
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          includeSummary,
          includeMetadata,
          includeBehavioralAnalysis,
          includePerformanceAnalysis,
          includeCoverageAnalysis,
          includeRecommendations,
          includeDetailedResults,
          includeRawData,
          excludedTests,
          includedSections,
          maxTestResults,
          recommendationLevel);
    }

    @Override
    public String toString() {
      return "ContentConfiguration{"
          + "summary="
          + includeSummary
          + ", behavioral="
          + includeBehavioralAnalysis
          + ", performance="
          + includePerformanceAnalysis
          + ", coverage="
          + includeCoverageAnalysis
          + ", recommendations="
          + includeRecommendations
          + ", maxResults="
          + maxTestResults
          + '}';
    }

    /** Builder for ContentConfiguration. */
    public static final class Builder {
      private boolean includeSummary = true;
      private boolean includeMetadata = true;
      private boolean includeBehavioralAnalysis = true;
      private boolean includePerformanceAnalysis = true;
      private boolean includeCoverageAnalysis = true;
      private boolean includeRecommendations = true;
      private boolean includeDetailedResults = true;
      private boolean includeRawData = false;
      private Set<String> excludedTests = Collections.emptySet();
      private Set<String> includedSections = Collections.emptySet();
      private int maxTestResults = 1000;
      private RecommendationLevel recommendationLevel = RecommendationLevel.MEDIUM;

      public Builder includeSummary(final boolean includeSummary) {
        this.includeSummary = includeSummary;
        return this;
      }

      public Builder includeMetadata(final boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
        return this;
      }

      public Builder includeBehavioralAnalysis(final boolean includeBehavioralAnalysis) {
        this.includeBehavioralAnalysis = includeBehavioralAnalysis;
        return this;
      }

      public Builder includePerformanceAnalysis(final boolean includePerformanceAnalysis) {
        this.includePerformanceAnalysis = includePerformanceAnalysis;
        return this;
      }

      public Builder includeCoverageAnalysis(final boolean includeCoverageAnalysis) {
        this.includeCoverageAnalysis = includeCoverageAnalysis;
        return this;
      }

      public Builder includeRecommendations(final boolean includeRecommendations) {
        this.includeRecommendations = includeRecommendations;
        return this;
      }

      public Builder includeDetailedResults(final boolean includeDetailedResults) {
        this.includeDetailedResults = includeDetailedResults;
        return this;
      }

      public Builder includeRawData(final boolean includeRawData) {
        this.includeRawData = includeRawData;
        return this;
      }

      public Builder excludedTests(final Set<String> excludedTests) {
        this.excludedTests = Objects.requireNonNull(excludedTests, "excludedTests cannot be null");
        return this;
      }

      /**
       * Sets the included sections.
       *
       * @param includedSections the sections to include
       * @return this builder
       */
      public Builder includedSections(final Set<String> includedSections) {
        this.includedSections =
            Objects.requireNonNull(includedSections, "includedSections cannot be null");
        return this;
      }

      /**
       * Sets the maximum number of test results.
       *
       * @param maxTestResults the maximum test results
       * @return this builder
       */
      public Builder maxTestResults(final int maxTestResults) {
        if (maxTestResults <= 0) {
          throw new IllegalArgumentException("maxTestResults must be positive");
        }
        this.maxTestResults = maxTestResults;
        return this;
      }

      /** Sets the recommendation level for the report configuration.
       *
       * @param recommendationLevel the recommendation level to set
       * @return this builder instance
       */
      public Builder recommendationLevel(final RecommendationLevel recommendationLevel) {
        this.recommendationLevel =
            Objects.requireNonNull(recommendationLevel, "recommendationLevel cannot be null");
        return this;
      }

      public ContentConfiguration build() {
        return new ContentConfiguration(this);
      }
    }
  }

  /** Configuration for report formatting and display options. */
  public static final class FormattingConfiguration {
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

      /** Sets whether to include line numbers in the report.
       *
       * @param includeLineNumbers true to include line numbers
       * @return this builder instance
       */
      public Builder includeLineNumbers(final boolean includeLineNumbers) {
        this.includeLineNumbers = includeLineNumbers;
        return this;
      }

      /** Sets the date time format for the report.
       *
       * @param dateTimeFormat the date time format to set
       * @return this builder instance
       */
      public Builder dateTimeFormat(final DateTimeFormat dateTimeFormat) {
        this.dateTimeFormat =
            Objects.requireNonNull(dateTimeFormat, "dateTimeFormat cannot be null");
        return this;
      }

      /** Sets the number format for the report.
       *
       * @param numberFormat the number format to set
       * @return this builder instance
       */
      public Builder numberFormat(final NumberFormat numberFormat) {
        this.numberFormat = Objects.requireNonNull(numberFormat, "numberFormat cannot be null");
        return this;
      }

      /** Sets the maximum line length for the report.
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

  /** Configuration for theme and visual styling. */
  public static final class ThemeConfiguration {
    private final String themeName;
    private final Map<String, String> colors;
    private final Map<String, String> fonts;
    private final Map<String, String> customCss;
    private final boolean darkMode;
    private final String brandingLogo;
    private final String brandingFooter;

    private ThemeConfiguration(final Builder builder) {
      this.themeName = Objects.requireNonNull(builder.themeName, "themeName cannot be null");
      this.colors = Map.copyOf(builder.colors);
      this.fonts = Map.copyOf(builder.fonts);
      this.customCss = Map.copyOf(builder.customCss);
      this.darkMode = builder.darkMode;
      this.brandingLogo = builder.brandingLogo;
      this.brandingFooter = builder.brandingFooter;
    }

    public String getThemeName() {
      return themeName;
    }

    public Map<String, String> getColors() {
      return colors;
    }

    public Map<String, String> getFonts() {
      return fonts;
    }

    public Map<String, String> getCustomCss() {
      return customCss;
    }

    public boolean isDarkMode() {
      return darkMode;
    }

    public String getBrandingLogo() {
      return brandingLogo;
    }

    public String getBrandingFooter() {
      return brandingFooter;
    }

    /** Creates default theme configuration. */
    public static ThemeConfiguration defaultTheme() {
      return new Builder("default")
          .colors(
              Map.of(
                  "primary", "#007bff",
                  "success", "#28a745",
                  "warning", "#ffc107",
                  "danger", "#dc3545",
                  "background", "#ffffff",
                  "text", "#333333"))
          .fonts(
              Map.of(
                  "heading", "system-ui, sans-serif",
                  "body", "system-ui, sans-serif",
                  "monospace", "Consolas, Monaco, monospace"))
          .darkMode(false)
          .build();
    }

    /** Creates minimal theme configuration. */
    public static ThemeConfiguration minimalTheme() {
      return new Builder("minimal")
          .colors(
              Map.of(
                  "primary", "#000000",
                  "background", "#ffffff",
                  "text", "#333333"))
          .fonts(
              Map.of(
                  "body", "system-ui, sans-serif",
                  "monospace", "monospace"))
          .darkMode(false)
          .build();
    }

    /** Creates dark theme configuration. */
    public static ThemeConfiguration darkTheme() {
      return new Builder("dark")
          .colors(
              Map.of(
                  "primary", "#17a2b8",
                  "success", "#28a745",
                  "warning", "#ffc107",
                  "danger", "#dc3545",
                  "background", "#1a1a1a",
                  "text", "#ffffff"))
          .fonts(
              Map.of(
                  "heading", "system-ui, sans-serif",
                  "body", "system-ui, sans-serif",
                  "monospace", "Consolas, Monaco, monospace"))
          .darkMode(true)
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

      final ThemeConfiguration that = (ThemeConfiguration) obj;
      return darkMode == that.darkMode
          && Objects.equals(themeName, that.themeName)
          && Objects.equals(colors, that.colors)
          && Objects.equals(fonts, that.fonts)
          && Objects.equals(customCss, that.customCss)
          && Objects.equals(brandingLogo, that.brandingLogo)
          && Objects.equals(brandingFooter, that.brandingFooter);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          themeName, colors, fonts, customCss, darkMode, brandingLogo, brandingFooter);
    }

    @Override
    public String toString() {
      return "ThemeConfiguration{"
          + "name='"
          + themeName
          + '\''
          + ", darkMode="
          + darkMode
          + ", colors="
          + colors.size()
          + ", fonts="
          + fonts.size()
          + '}';
    }

    /** Builder for ThemeConfiguration. */
    public static final class Builder {
      private final String themeName;
      private Map<String, String> colors = Collections.emptyMap();
      private Map<String, String> fonts = Collections.emptyMap();
      private Map<String, String> customCss = Collections.emptyMap();
      private boolean darkMode = false;
      private String brandingLogo;
      private String brandingFooter;

      public Builder(final String themeName) {
        this.themeName = Objects.requireNonNull(themeName, "themeName cannot be null");
      }

      public Builder colors(final Map<String, String> colors) {
        this.colors = Objects.requireNonNull(colors, "colors cannot be null");
        return this;
      }

      public Builder fonts(final Map<String, String> fonts) {
        this.fonts = Objects.requireNonNull(fonts, "fonts cannot be null");
        return this;
      }

      public Builder customCss(final Map<String, String> customCss) {
        this.customCss = Objects.requireNonNull(customCss, "customCss cannot be null");
        return this;
      }

      public Builder darkMode(final boolean darkMode) {
        this.darkMode = darkMode;
        return this;
      }

      public Builder brandingLogo(final String brandingLogo) {
        this.brandingLogo = brandingLogo;
        return this;
      }

      public Builder brandingFooter(final String brandingFooter) {
        this.brandingFooter = brandingFooter;
        return this;
      }

      public ThemeConfiguration build() {
        return new ThemeConfiguration(this);
      }
    }
  }

  /** Configuration for localization and internationalization. */
  public static final class LocalizationConfiguration {
    private final Locale locale;
    private final String timeZone;
    private final Map<String, String> customMessages;
    private final String resourceBundleName;

    private LocalizationConfiguration(final Builder builder) {
      this.locale = Objects.requireNonNull(builder.locale, "locale cannot be null");
      this.timeZone = Objects.requireNonNull(builder.timeZone, "timeZone cannot be null");
      this.customMessages = Map.copyOf(builder.customMessages);
      this.resourceBundleName = builder.resourceBundleName;
    }

    public Locale getLocale() {
      return locale;
    }

    public String getTimeZone() {
      return timeZone;
    }

    public Map<String, String> getCustomMessages() {
      return customMessages;
    }

    public String getResourceBundleName() {
      return resourceBundleName;
    }

    /** Creates default localization configuration. */
    public static LocalizationConfiguration defaultLocalization() {
      return new Builder(Locale.getDefault())
          .timeZone(java.time.ZoneId.systemDefault().getId())
          .resourceBundleName("comparison-messages")
          .build();
    }

    /** Creates English localization configuration. */
    public static LocalizationConfiguration englishLocalization() {
      return new Builder(Locale.ENGLISH)
          .timeZone("UTC")
          .resourceBundleName("comparison-messages")
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

      final LocalizationConfiguration that = (LocalizationConfiguration) obj;
      return Objects.equals(locale, that.locale)
          && Objects.equals(timeZone, that.timeZone)
          && Objects.equals(customMessages, that.customMessages)
          && Objects.equals(resourceBundleName, that.resourceBundleName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(locale, timeZone, customMessages, resourceBundleName);
    }

    @Override
    public String toString() {
      return "LocalizationConfiguration{"
          + "locale="
          + locale
          + ", timeZone='"
          + timeZone
          + '\''
          + ", resourceBundle='"
          + resourceBundleName
          + '\''
          + '}';
    }

    /** Builder for LocalizationConfiguration. */
    public static final class Builder {
      private final Locale locale;
      private String timeZone = "UTC";
      private Map<String, String> customMessages = Collections.emptyMap();
      private String resourceBundleName = "comparison-messages";

      /** Creates a new builder for LocalizationConfiguration.
       *
       * @param locale the locale to use
       */
      public Builder(final Locale locale) {
        this.locale = Objects.requireNonNull(locale, "locale cannot be null");
      }

      /** Sets the time zone for localization.
       *
       * @param timeZone the time zone to set
       * @return this builder instance
       */
      public Builder timeZone(final String timeZone) {
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone cannot be null");
        return this;
      }

      /** Sets custom messages for localization.
       *
       * @param customMessages the custom messages map to set
       * @return this builder instance
       */
      public Builder customMessages(final Map<String, String> customMessages) {
        this.customMessages =
            Objects.requireNonNull(customMessages, "customMessages cannot be null");
        return this;
      }

      public Builder resourceBundleName(final String resourceBundleName) {
        this.resourceBundleName = resourceBundleName;
        return this;
      }

      public LocalizationConfiguration build() {
        return new LocalizationConfiguration(this);
      }
    }
  }

  /** Configuration for output generation options. */
  public static final class OutputConfiguration {
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

      /** Sets whether to generate HTML output.
       *
       * @param generateHtml true to generate HTML output
       * @return this builder instance
       */
      public Builder generateHtml(final boolean generateHtml) {
        this.generateHtml = generateHtml;
        return this;
      }

      /** Sets whether to generate JSON output.
       *
       * @param generateJson true to generate JSON output
       * @return this builder instance
       */
      public Builder generateJson(final boolean generateJson) {
        this.generateJson = generateJson;
        return this;
      }

      /** Sets whether to generate CSV output.
       *
       * @param generateCsv true to generate CSV output
       * @return this builder instance
       */
      public Builder generateCsv(final boolean generateCsv) {
        this.generateCsv = generateCsv;
        return this;
      }

      /** Sets whether to generate console output.
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

      /** Sets the maximum cache size.
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

      /** Sets the cache expiration time in minutes.
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

      /** Sets whether to use streaming output.
       *
       * @param streamOutput true to enable streaming output
       * @return this builder instance
       */
      public Builder streamOutput(final boolean streamOutput) {
        this.streamOutput = streamOutput;
        return this;
      }

      /** Sets the output buffer size.
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
}
