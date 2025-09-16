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
import java.util.ResourceBundle;
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
    final ThemeConfiguration theme = reportConfig.getThemeConfig();
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

    public Builder resourceBundleName(final String resourceBundleName) {
      this.resourceBundleName =
          Objects.requireNonNull(resourceBundleName, "resourceBundleName cannot be null");
      return this;
    }

    public Builder cacheSize(final int cacheSize) {
      if (cacheSize < 0) {
        throw new IllegalArgumentException("cacheSize cannot be negative");
      }
      this.cacheSize = cacheSize;
      return this;
    }

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

/** Exception thrown when template processing fails. */
final class TemplateProcessingException extends Exception {
  public TemplateProcessingException(final String message) {
    super(message);
  }

  public TemplateProcessingException(final String message, final Throwable cause) {
    super(message, cause);
  }
}

/** Simple template cache with expiration support. */
final class TemplateCache {
  private final Map<String, CacheEntry> cache;
  private final int maxSize;
  private final long expirationMillis;
  private long hits = 0;
  private long misses = 0;

  public TemplateCache(final int maxSize, final long expirationMinutes) {
    this.cache = new java.util.concurrent.ConcurrentHashMap<>();
    this.maxSize = maxSize;
    this.expirationMillis = expirationMinutes * 60 * 1000;
  }

  public Template get(final String key) {
    final CacheEntry entry = cache.get(key);
    if (entry != null && !entry.isExpired()) {
      hits++;
      return entry.template;
    }

    if (entry != null) {
      cache.remove(key);
    }

    misses++;
    return null;
  }

  public void put(final String key, final Template template) {
    if (cache.size() >= maxSize) {
      evictOldest();
    }

    cache.put(key, new CacheEntry(template, System.currentTimeMillis() + expirationMillis));
  }

  public void clear() {
    cache.clear();
  }

  public CacheStatistics getStatistics() {
    return new CacheStatistics(cache.size(), hits, misses);
  }

  private void evictOldest() {
    // Simple LRU-like eviction - remove expired entries first
    cache.entrySet().removeIf(entry -> entry.getValue().isExpired());

    // If still too large, remove some entries
    if (cache.size() >= maxSize) {
      final java.util.Iterator<Map.Entry<String, CacheEntry>> iterator =
          cache.entrySet().iterator();
      for (int i = 0; i < maxSize / 4 && iterator.hasNext(); i++) {
        iterator.next();
        iterator.remove();
      }
    }
  }

  private static final class CacheEntry {
    final Template template;
    final long expirationTime;

    CacheEntry(final Template template, final long expirationTime) {
      this.template = template;
      this.expirationTime = expirationTime;
    }

    boolean isExpired() {
      return System.currentTimeMillis() > expirationTime;
    }
  }
}

/** Cache statistics. */
final class CacheStatistics {
  private final int size;
  private final long hits;
  private final long misses;

  public CacheStatistics(final int size, final long hits, final long misses) {
    this.size = size;
    this.hits = hits;
    this.misses = misses;
  }

  public int getSize() {
    return size;
  }

  public long getHits() {
    return hits;
  }

  public long getMisses() {
    return misses;
  }

  public double getHitRatio() {
    final long total = hits + misses;
    return total == 0 ? 0.0 : (double) hits / total;
  }

  @Override
  public String toString() {
    return "CacheStatistics{"
        + "size="
        + size
        + ", hits="
        + hits
        + ", misses="
        + misses
        + ", hitRatio="
        + String.format("%.2f%%", getHitRatio() * 100)
        + '}';
  }
}

/** Message resolver for internationalization support. */
final class MessageResolver {
  private final ResourceBundle resourceBundle;
  private final Locale locale;

  public MessageResolver(final String bundleName, final Locale locale) {
    this.locale = locale;
    try {
      this.resourceBundle = ResourceBundle.getBundle(bundleName, locale);
    } catch (final java.util.MissingResourceException e) {
      throw new IllegalArgumentException("Resource bundle not found: " + bundleName, e);
    }
  }

  /**
   * Gets a localized message.
   *
   * @param key the message key
   * @return the localized message
   */
  public String getMessage(final String key) {
    try {
      return resourceBundle.getString(key);
    } catch (final java.util.MissingResourceException e) {
      return "!" + key + "!"; // Return key with markers if not found
    }
  }

  /**
   * Gets a localized message with parameters.
   *
   * @param key the message key
   * @param params the parameters
   * @return the formatted localized message
   */
  public String getMessage(final String key, final Object... params) {
    final String pattern = getMessage(key);
    return java.text.MessageFormat.format(pattern, params);
  }

  public Locale getLocale() {
    return locale;
  }
}

/** Utility functions available in templates. */
final class TemplateUtilities {

  /**
   * Formats a number as a percentage.
   *
   * @param value the value to format
   * @return formatted percentage string
   */
  public String formatPercentage(final double value) {
    return String.format("%.1f%%", value * 100);
  }

  /**
   * Formats a duration in milliseconds to human-readable format.
   *
   * @param millis the duration in milliseconds
   * @return formatted duration string
   */
  public String formatDuration(final long millis) {
    if (millis < 1000) {
      return millis + " ms";
    } else if (millis < 60000) {
      return String.format("%.1f sec", millis / 1000.0);
    } else if (millis < 3600000) {
      return String.format("%.1f min", millis / 60000.0);
    } else {
      return String.format("%.1f hr", millis / 3600000.0);
    }
  }

  /**
   * Truncates a string to the specified length.
   *
   * @param value the string to truncate
   * @param maxLength the maximum length
   * @return truncated string
   */
  public String truncate(final String value, final int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength - 3) + "...";
  }

  /**
   * Escapes HTML special characters.
   *
   * @param value the string to escape
   * @return escaped string
   */
  public String escapeHtml(final String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  /**
   * Capitalizes the first letter of a string.
   *
   * @param value the string to capitalize
   * @return capitalized string
   */
  public String capitalize(final String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
  }

  /**
   * Converts a value to CSS color format.
   *
   * @param value the color value (hex, rgb, or name)
   * @return CSS color string
   */
  public String toCssColor(final String value) {
    if (value == null || value.isEmpty()) {
      return "#000000";
    }

    // If it's already a valid CSS color, return as-is
    if (value.startsWith("#") || value.startsWith("rgb") || value.startsWith("hsl")) {
      return value;
    }

    // If it's a hex color without #, add it
    if (value.matches("[0-9a-fA-F]{6}")) {
      return "#" + value;
    }

    // Otherwise return as-is (might be a named color)
    return value;
  }
}
