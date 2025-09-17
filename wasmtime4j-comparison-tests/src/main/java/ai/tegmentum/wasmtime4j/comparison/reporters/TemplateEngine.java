package ai.tegmentum.wasmtime4j.comparison.reporters;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Template engine for processing report templates with FreeMarker integration. Provides template
 * compilation, caching, validation, and internationalization support.
 *
 * @since 1.0.0
 */
public final class TemplateEngine {
  private static final Logger LOGGER = Logger.getLogger(TemplateEngine.class.getName());

  private final Configuration freemarkerConfig;
  private final TemplateCache templateCache;
  private final MessageResolver messageResolver;
  private final TemplateValidator templateValidator;

  private TemplateEngine(final Builder builder) {
    this.freemarkerConfig = createFreemarkerConfiguration(builder.locale);
    this.templateCache = new TemplateCache(builder.cacheSize, builder.cacheExpirationMinutes);
    this.messageResolver = new MessageResolver(builder.resourceBundleName, builder.locale);
    this.templateValidator = new TemplateValidator();
  }

  /**
   * Processes a template with the given data model and configuration.
   *
   * @param reportTemplate the template to process
   * @param dataModel the data model for template processing
   * @param reportConfig the report configuration
   * @return the processed template output
   * @throws TemplateProcessingException if template processing fails
   */
  public String processTemplate(
      final ReportTemplate reportTemplate,
      final ComparisonReport dataModel,
      final ReportConfiguration reportConfig)
      throws TemplateProcessingException {

    Objects.requireNonNull(reportTemplate, "reportTemplate cannot be null");
    Objects.requireNonNull(dataModel, "dataModel cannot be null");
    Objects.requireNonNull(reportConfig, "reportConfig cannot be null");

    // Validate template before processing
    final TemplateValidationResult validation = reportTemplate.validate();
    if (!validation.isValid()) {
      throw new TemplateProcessingException(
          "Template validation failed: " + validation.getErrors());
    }

    // Check compatibility between template and configuration
    if (!reportTemplate.isCompatibleWith(reportConfig)) {
      throw new TemplateProcessingException(
          "Template is not compatible with the provided configuration");
    }

    try {
      return processTemplateInternal(reportTemplate, dataModel, reportConfig);
    } catch (final IOException | TemplateException e) {
      throw new TemplateProcessingException("Failed to process template: " + e.getMessage(), e);
    }
  }

  /**
   * Processes a single template component.
   *
   * @param component the component to process
   * @param dataModel the data model
   * @param reportConfig the report configuration
   * @return the processed component output
   * @throws TemplateProcessingException if processing fails
   */
  public String processComponent(
      final TemplateComponent component,
      final ComparisonReport dataModel,
      final ReportConfiguration reportConfig)
      throws TemplateProcessingException {

    Objects.requireNonNull(component, "component cannot be null");
    Objects.requireNonNull(dataModel, "dataModel cannot be null");
    Objects.requireNonNull(reportConfig, "reportConfig cannot be null");

    if (!component.isCompatibleWith(reportConfig)) {
      LOGGER.log(Level.FINE, "Skipping incompatible component: {0}", component.getComponentId());
      return "";
    }

    try {
      return processComponentInternal(component, dataModel, reportConfig);
    } catch (final IOException | TemplateException e) {
      throw new TemplateProcessingException(
          "Failed to process component " + component.getComponentId() + ": " + e.getMessage(), e);
    }
  }

  /**
   * Validates a template without processing it.
   *
   * @param reportTemplate the template to validate
   * @return validation result
   */
  public TemplateValidationResult validateTemplate(final ReportTemplate reportTemplate) {
    Objects.requireNonNull(reportTemplate, "reportTemplate cannot be null");
    return templateValidator.validate(reportTemplate);
  }

  /** Clears the template cache. */
  public void clearCache() {
    templateCache.clear();
  }

  /**
   * Gets cache statistics.
   *
   * @return cache statistics
   */
  public CacheStatistics getCacheStatistics() {
    return templateCache.getStatistics();
  }

  private String processTemplateInternal(
      final ReportTemplate reportTemplate,
      final ComparisonReport dataModel,
      final ReportConfiguration reportConfig)
      throws IOException, TemplateException {

    final StringBuilder output = new StringBuilder();
    final Map<String, Object> templateDataModel = createTemplateDataModel(dataModel, reportConfig);

    // Process each component in order
    final List<TemplateComponent> components = reportTemplate.getComponents();
    for (final TemplateComponent component : components) {
      if (component.isCompatibleWith(reportConfig)) {
        final String componentOutput = processComponentInternal(component, dataModel, reportConfig);
        output.append(componentOutput);
      } else {
        LOGGER.log(
            Level.FINE, "Skipping component {0} due to configuration", component.getComponentId());
      }
    }

    return output.toString();
  }

  private String processComponentInternal(
      final TemplateComponent component,
      final ComparisonReport dataModel,
      final ReportConfiguration reportConfig)
      throws IOException, TemplateException {

    final String cacheKey = createCacheKey(component, reportConfig);
    Template template = templateCache.get(cacheKey);

    if (template == null) {
      template = createFreemarkerTemplate(component);
      templateCache.put(cacheKey, template);
    }

    final Map<String, Object> templateDataModel = createTemplateDataModel(dataModel, reportConfig);

    // Add component-specific data
    templateDataModel.putAll(component.getComponentData());

    // Add internationalization support
    templateDataModel.put("messages", messageResolver);

    final StringWriter writer = new StringWriter();
    template.process(templateDataModel, writer);
    return writer.toString();
  }

  private Map<String, Object> createTemplateDataModel(
      final ComparisonReport dataModel, final ReportConfiguration reportConfig) {

    final Map<String, Object> templateData = new HashMap<>();

    // Add report data
    templateData.put("reportId", dataModel.getReportId());
    templateData.put("testSuiteName", dataModel.getTestSuiteName());
    templateData.put("testResults", dataModel.getTestResults());
    templateData.put("metadata", dataModel.getMetadata());
    templateData.put("summary", dataModel.getSummary());
    templateData.put("globalRecommendations", dataModel.getGlobalRecommendations());
    templateData.put("generationTime", dataModel.getGenerationTime());

    // Add filtered views for convenience
    templateData.put("testsWithBehavioralIssues", dataModel.getTestsWithBehavioralIssues());
    templateData.put("testsWithPerformanceIssues", dataModel.getTestsWithPerformanceIssues());
    templateData.put("testsWithCoverageGaps", dataModel.getTestsWithCoverageGaps());

    // Add configuration
    templateData.put("config", reportConfig);

    // Add theme data
    final ReportConfiguration.ThemeConfiguration theme = reportConfig.getThemeConfig();
    templateData.put("theme", theme);
    templateData.put("colors", theme.getColors());
    templateData.put("fonts", theme.getFonts());
    templateData.put("customCss", theme.getCustomCss());
    templateData.put("darkMode", theme.isDarkMode());

    // Add formatting helpers
    templateData.put("formatting", reportConfig.getFormattingConfig());

    // Add utility functions
    templateData.put("util", new TemplateUtilities());

    return templateData;
  }

  private Template createFreemarkerTemplate(final TemplateComponent component) throws IOException {
    final String templateContent = component.getTemplateContent();
    final String templateName = component.getComponentId() + ".ftl";

    return new Template(templateName, new StringReader(templateContent), freemarkerConfig);
  }

  private String createCacheKey(
      final TemplateComponent component, final ReportConfiguration reportConfig) {
    return component.getComponentId()
        + ":"
        + reportConfig.getConfigurationName()
        + ":"
        + reportConfig.getThemeConfig().getThemeName();
  }

  private Configuration createFreemarkerConfiguration(final Locale locale) {
    final Configuration config = new Configuration(Configuration.VERSION_2_3_32);

    // Set locale for number and date formatting
    config.setLocale(locale);

    // Set template exception handler
    config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

    // Set default encoding
    config.setDefaultEncoding("UTF-8");

    // Set whitespace stripping
    config.setWhitespaceStripping(true);

    // Enable auto-escaping for HTML
    config.setAutoEscapingPolicy(Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY);

    return config;
  }

  /** Creates a new TemplateEngine builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Creates a TemplateEngine with default settings. */
  public static TemplateEngine createDefault() {
    return builder().build();
  }

  /** Builder for TemplateEngine. */
  public static final class Builder {
    private Locale locale = Locale.getDefault();
    private String resourceBundleName = "comparison-messages";
    private int cacheSize = 100;
    private long cacheExpirationMinutes = 60;

    public Builder locale(final Locale locale) {
      this.locale = Objects.requireNonNull(locale, "locale cannot be null");
      return this;
    }

    /**
     * Sets the resource bundle name for internationalization.
     *
     * @param resourceBundleName the resource bundle name
     * @return this builder
     */
    public Builder resourceBundleName(final String resourceBundleName) {
      this.resourceBundleName =
          Objects.requireNonNull(resourceBundleName, "resourceBundleName cannot be null");
      return this;
    }

    /**
     * Sets the cache size for template caching.
     *
     * @param cacheSize the cache size
     * @return this builder
     */
    public Builder cacheSize(final int cacheSize) {
      if (cacheSize < 0) {
        throw new IllegalArgumentException("cacheSize cannot be negative");
      }
      this.cacheSize = cacheSize;
      return this;
    }

    /**
     * Sets the cache expiration time in minutes.
     *
     * @param cacheExpirationMinutes the cache expiration time in minutes
     * @return this builder
     */
    public Builder cacheExpirationMinutes(final long cacheExpirationMinutes) {
      if (cacheExpirationMinutes < 0) {
        throw new IllegalArgumentException("cacheExpirationMinutes cannot be negative");
      }
      this.cacheExpirationMinutes = cacheExpirationMinutes;
      return this;
    }

    public TemplateEngine build() {
      return new TemplateEngine(this);
    }
  }
}
