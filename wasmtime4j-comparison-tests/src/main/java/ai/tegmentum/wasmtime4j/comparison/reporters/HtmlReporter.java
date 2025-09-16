package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTML reporter that generates comprehensive interactive reports with embedded JavaScript
 * visualizations, responsive design, and detailed comparison analysis views. Supports
 * both static HTML files and embedded web server deployment.
 *
 * @since 1.0.0
 */
public final class HtmlReporter {
  private static final Logger LOGGER = Logger.getLogger(HtmlReporter.class.getName());
  private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final HtmlReporterConfiguration configuration;
  private final TemplateEngine templateEngine;
  private final VisualizationBuilder visualizationBuilder;

  public HtmlReporter(final HtmlReporterConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
    this.templateEngine = new TemplateEngine();
    this.visualizationBuilder = new VisualizationBuilder();
  }

  /**
   * Generates a comprehensive HTML report from the comparison results.
   *
   * @param report the comparison report to generate HTML for
   * @param outputPath the path where the HTML report should be written
   * @throws IOException if the report cannot be written
   */
  public void generateReport(final ComparisonReport report, final Path outputPath) throws IOException {
    Objects.requireNonNull(report, "report cannot be null");
    Objects.requireNonNull(outputPath, "outputPath cannot be null");

    LOGGER.info("Generating HTML report for " + report.getReportId());
    final long startTime = System.currentTimeMillis();

    try {
      // Create output directory if it doesn't exist
      if (outputPath.getParent() != null) {
        Files.createDirectories(outputPath.getParent());
      }

      // Generate the HTML content
      final String htmlContent = generateHtmlContent(report);

      // Write to file
      try (final Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
        writer.write(htmlContent);
      }

      // Copy static resources if needed
      if (configuration.includeStaticResources()) {
        copyStaticResources(outputPath.getParent());
      }

      final long duration = System.currentTimeMillis() - startTime;
      LOGGER.info("HTML report generated successfully in " + duration + "ms: " + outputPath);

    } catch (final IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to generate HTML report: " + e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Generates HTML content and writes it to an output stream.
   *
   * @param report the comparison report to generate HTML for
   * @param outputStream the output stream to write the HTML content to
   * @throws IOException if the content cannot be written
   */
  public void generateReport(final ComparisonReport report, final OutputStream outputStream) throws IOException {
    Objects.requireNonNull(report, "report cannot be null");
    Objects.requireNonNull(outputStream, "outputStream cannot be null");

    final String htmlContent = generateHtmlContent(report);
    try (final Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      writer.write(htmlContent);
    }
  }

  /**
   * Generates the complete HTML content for the comparison report.
   *
   * @param report the comparison report
   * @return complete HTML content as a string
   */
  private String generateHtmlContent(final ComparisonReport report) {
    final Map<String, Object> templateContext = createTemplateContext(report);
    return templateEngine.renderTemplate("dashboard.html", templateContext);
  }

  /**
   * Creates the template context with all necessary data for HTML generation.
   *
   * @param report the comparison report
   * @return template context map
   */
  private Map<String, Object> createTemplateContext(final ComparisonReport report) {
    final Map<String, Object> context = new HashMap<>();

    // Report metadata
    context.put("reportId", report.getReportId());
    context.put("generatedAt", report.getGeneratedAt().format(TIMESTAMP_FORMATTER));
    context.put("metadata", report.getMetadata());
    context.put("executionSummary", report.getExecutionSummary());

    // Statistical data
    context.put("statistics", report.getStatistics());
    context.put("coverageReport", report.getCoverageReport());
    context.put("performanceSummary", report.getPerformanceSummary());

    // Test results and analysis
    context.put("testResults", report.getTestResults());
    context.put("criticalTestResults", report.getCriticalTestResults());
    context.put("behavioralDiscrepancies", report.getBehavioralDiscrepancies());
    context.put("discrepanciesBySeverity", report.getDiscrepanciesBySeverity());
    context.put("recommendations", report.getRecommendations());

    // Visualization data
    context.put("performanceChartData", visualizationBuilder.createPerformanceChartData(report));
    context.put("coverageChartData", visualizationBuilder.createCoverageChartData(report));
    context.put("trendChartData", visualizationBuilder.createTrendChartData(report));

    // Configuration
    context.put("configuration", configuration);
    context.put("title", configuration.getReportTitle());
    context.put("theme", configuration.getTheme());
    context.put("includeStaticResources", configuration.includeStaticResources());

    // Utility data
    context.put("executiveSummary", report.getExecutiveSummary());
    context.put("totalTests", report.getTestResults().size());
    context.put("criticalIssuesCount", report.getCriticalTestResults().size());

    return context;
  }

  /**
   * Copies static resources (CSS, JavaScript, images) to the output directory.
   *
   * @param outputDirectory the directory to copy resources to
   * @throws IOException if resources cannot be copied
   */
  private void copyStaticResources(final Path outputDirectory) throws IOException {
    if (outputDirectory == null) {
      return;
    }

    final Path resourcesDir = outputDirectory.resolve("resources");
    Files.createDirectories(resourcesDir);

    // Copy CSS files
    copyResourceFile("static/css/dashboard.css", resourcesDir.resolve("dashboard.css"));
    copyResourceFile("static/css/bootstrap.min.css", resourcesDir.resolve("bootstrap.min.css"));

    // Copy JavaScript files
    copyResourceFile("static/js/dashboard.js", resourcesDir.resolve("dashboard.js"));
    copyResourceFile("static/js/chart.min.js", resourcesDir.resolve("chart.min.js"));
    copyResourceFile("static/js/diff.min.js", resourcesDir.resolve("diff.min.js"));

    LOGGER.fine("Static resources copied to: " + resourcesDir);
  }

  /**
   * Copies a single resource file from the classpath to the target path.
   *
   * @param resourcePath the classpath resource path
   * @param targetPath the target file path
   * @throws IOException if the resource cannot be copied
   */
  private void copyResourceFile(final String resourcePath, final Path targetPath) throws IOException {
    try (final var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (inputStream != null) {
        Files.copy(inputStream, targetPath);
      } else {
        LOGGER.warning("Resource not found: " + resourcePath);
      }
    }
  }

  /**
   * Gets the current configuration for this HTML reporter.
   *
   * @return the reporter configuration
   */
  public HtmlReporterConfiguration getConfiguration() {
    return configuration;
  }
}

/** Configuration for HTML report generation. */
final class HtmlReporterConfiguration {
  private final String reportTitle;
  private final String theme;
  private final boolean includeStaticResources;
  private final boolean enableInteractiveFeatures;
  private final boolean enablePerformanceCharts;
  private final boolean enableCoverageAnalysis;
  private final VerbosityLevel verbosityLevel;

  private HtmlReporterConfiguration(final Builder builder) {
    this.reportTitle = Objects.requireNonNull(builder.reportTitle, "reportTitle cannot be null");
    this.theme = Objects.requireNonNull(builder.theme, "theme cannot be null");
    this.includeStaticResources = builder.includeStaticResources;
    this.enableInteractiveFeatures = builder.enableInteractiveFeatures;
    this.enablePerformanceCharts = builder.enablePerformanceCharts;
    this.enableCoverageAnalysis = builder.enableCoverageAnalysis;
    this.verbosityLevel = Objects.requireNonNull(builder.verbosityLevel, "verbosityLevel cannot be null");
  }

  public String getReportTitle() {
    return reportTitle;
  }

  public String getTheme() {
    return theme;
  }

  public boolean includeStaticResources() {
    return includeStaticResources;
  }

  public boolean isInteractiveFeaturesEnabled() {
    return enableInteractiveFeatures;
  }

  public boolean isPerformanceChartsEnabled() {
    return enablePerformanceCharts;
  }

  public boolean isCoverageAnalysisEnabled() {
    return enableCoverageAnalysis;
  }

  public VerbosityLevel getVerbosityLevel() {
    return verbosityLevel;
  }

  /** Creates a new builder with default configuration. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for HtmlReporterConfiguration. */
  public static final class Builder {
    private String reportTitle = "Wasmtime4j Comparison Report";
    private String theme = "default";
    private boolean includeStaticResources = true;
    private boolean enableInteractiveFeatures = true;
    private boolean enablePerformanceCharts = true;
    private boolean enableCoverageAnalysis = true;
    private VerbosityLevel verbosityLevel = VerbosityLevel.NORMAL;

    public Builder reportTitle(final String reportTitle) {
      this.reportTitle = Objects.requireNonNull(reportTitle, "reportTitle cannot be null");
      return this;
    }

    public Builder theme(final String theme) {
      this.theme = Objects.requireNonNull(theme, "theme cannot be null");
      return this;
    }

    public Builder includeStaticResources(final boolean includeStaticResources) {
      this.includeStaticResources = includeStaticResources;
      return this;
    }

    public Builder enableInteractiveFeatures(final boolean enableInteractiveFeatures) {
      this.enableInteractiveFeatures = enableInteractiveFeatures;
      return this;
    }

    public Builder enablePerformanceCharts(final boolean enablePerformanceCharts) {
      this.enablePerformanceCharts = enablePerformanceCharts;
      return this;
    }

    public Builder enableCoverageAnalysis(final boolean enableCoverageAnalysis) {
      this.enableCoverageAnalysis = enableCoverageAnalysis;
      return this;
    }

    public Builder verbosityLevel(final VerbosityLevel verbosityLevel) {
      this.verbosityLevel = Objects.requireNonNull(verbosityLevel, "verbosityLevel cannot be null");
      return this;
    }

    public HtmlReporterConfiguration build() {
      return new HtmlReporterConfiguration(this);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final HtmlReporterConfiguration that = (HtmlReporterConfiguration) obj;
    return includeStaticResources == that.includeStaticResources &&
           enableInteractiveFeatures == that.enableInteractiveFeatures &&
           enablePerformanceCharts == that.enablePerformanceCharts &&
           enableCoverageAnalysis == that.enableCoverageAnalysis &&
           Objects.equals(reportTitle, that.reportTitle) &&
           Objects.equals(theme, that.theme) &&
           verbosityLevel == that.verbosityLevel;
  }

  @Override
  public int hashCode() {
    return Objects.hash(reportTitle, theme, includeStaticResources, enableInteractiveFeatures,
                       enablePerformanceCharts, enableCoverageAnalysis, verbosityLevel);
  }

  @Override
  public String toString() {
    return "HtmlReporterConfiguration{" +
           "reportTitle='" + reportTitle + '\'' +
           ", theme='" + theme + '\'' +
           ", interactive=" + enableInteractiveFeatures +
           ", verbosity=" + verbosityLevel +
           '}';
  }
}

/** Simple template engine for HTML generation. */
final class TemplateEngine {
  private static final Logger LOGGER = Logger.getLogger(TemplateEngine.class.getName());

  /**
   * Renders a template with the given context.
   *
   * @param templateName the template file name
   * @param context the template context variables
   * @return rendered HTML content
   */
  public String renderTemplate(final String templateName, final Map<String, Object> context) {
    Objects.requireNonNull(templateName, "templateName cannot be null");
    Objects.requireNonNull(context, "context cannot be null");

    try {
      // Load template from resources
      final String templateContent = loadTemplate(templateName);

      // Simple template variable replacement
      return processTemplate(templateContent, context);
    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to render template: " + templateName, e);
      return generateErrorTemplate(templateName, e);
    }
  }

  /**
   * Loads a template from the classpath resources.
   *
   * @param templateName the template file name
   * @return template content as string
   * @throws IOException if template cannot be loaded
   */
  private String loadTemplate(final String templateName) throws IOException {
    final String templatePath = "templates/" + templateName;
    try (final var inputStream = getClass().getClassLoader().getResourceAsStream(templatePath)) {
      if (inputStream == null) {
        throw new IOException("Template not found: " + templatePath);
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Processes template content by replacing variables and control structures.
   *
   * @param templateContent the raw template content
   * @param context the template context variables
   * @return processed template content
   */
  private String processTemplate(final String templateContent, final Map<String, Object> context) {
    String result = templateContent;

    // Replace simple variables: {{variableName}}
    for (final Map.Entry<String, Object> entry : context.entrySet()) {
      final String placeholder = "{{" + entry.getKey() + "}}";
      final String value = entry.getValue() != null ? entry.getValue().toString() : "";
      result = result.replace(placeholder, escapeHtml(value));
    }

    // Process conditionals and loops would go here for a full template engine
    // For now, we'll use a simple replacement approach

    return result;
  }

  /**
   * Escapes HTML special characters in a string.
   *
   * @param input the input string
   * @return HTML-escaped string
   */
  private String escapeHtml(final String input) {
    if (input == null) {
      return "";
    }
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;");
  }

  /**
   * Generates an error template when template rendering fails.
   *
   * @param templateName the template that failed to render
   * @param error the error that occurred
   * @return basic error HTML content
   */
  private String generateErrorTemplate(final String templateName, final Exception error) {
    return String.format(
        "<!DOCTYPE html>" +
        "<html><head><title>Template Error</title></head>" +
        "<body><h1>Template Rendering Error</h1>" +
        "<p>Failed to render template: %s</p>" +
        "<p>Error: %s</p></body></html>",
        escapeHtml(templateName),
        escapeHtml(error.getMessage())
    );
  }
}