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
 * visualizations, responsive design, and detailed comparison analysis views. Supports both static
 * HTML files and embedded web server deployment.
 *
 * @since 1.0.0
 */
public final class HtmlReporter {
  private static final Logger LOGGER = Logger.getLogger(HtmlReporter.class.getName());
  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final HtmlReporterConfiguration configuration;
  private final TemplateEngine templateEngine;
  private final VisualizationBuilder visualizationBuilder;

  /**
   * Creates a new HTML reporter with the specified configuration.
   *
   * @param configuration the configuration for the HTML reporter
   */
  public HtmlReporter(final HtmlReporterConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
    this.templateEngine = TemplateEngine.createDefault();
    this.visualizationBuilder = new VisualizationBuilder();
  }

  /**
   * Generates a comprehensive HTML report from the comparison results.
   *
   * @param report the comparison report to generate HTML for
   * @param outputPath the path where the HTML report should be written
   * @throws IOException if the report cannot be written
   */
  public void generateReport(final ComparisonReport report, final Path outputPath)
      throws IOException {
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
  public void generateReport(final ComparisonReport report, final OutputStream outputStream)
      throws IOException {
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
    try {
      return templateEngine.processTemplate(
          ReportTemplate.defaultHtmlTemplate(), report, ReportConfiguration.defaultConfiguration());
    } catch (final TemplateProcessingException e) {
      throw new RuntimeException("Failed to generate HTML content", e);
    }
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
    context.put(
        "generatedAt",
        java.time.LocalDateTime.ofInstant(report.getGeneratedAt(), java.time.ZoneId.systemDefault())
            .format(TIMESTAMP_FORMATTER));
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
  private void copyResourceFile(final String resourcePath, final Path targetPath)
      throws IOException {
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
