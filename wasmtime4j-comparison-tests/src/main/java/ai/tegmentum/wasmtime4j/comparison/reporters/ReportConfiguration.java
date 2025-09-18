package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

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
}
