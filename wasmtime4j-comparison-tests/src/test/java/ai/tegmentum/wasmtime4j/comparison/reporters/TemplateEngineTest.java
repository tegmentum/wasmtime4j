package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for the TemplateEngine system. Tests template processing, caching,
 * validation, and internationalization.
 */
class TemplateEngineTest {

  private TemplateEngine templateEngine;
  private ComparisonReport sampleReport;
  private ReportConfiguration defaultConfig;

  @BeforeEach
  void setUp() {
    templateEngine = TemplateEngine.createDefault();
    defaultConfig = ReportConfiguration.defaultConfiguration();
    sampleReport = createSampleReport();
  }

  @Nested
  @DisplayName("Template Engine Creation")
  class TemplateEngineCreation {

    @Test
    @DisplayName("Should create default template engine")
    void shouldCreateDefaultTemplateEngine() {
      final TemplateEngine engine = TemplateEngine.createDefault();

      assertThat(engine).isNotNull();
      assertThat(engine.getCacheStatistics().getSize()).isZero();
    }

    @Test
    @DisplayName("Should create template engine with custom settings")
    void shouldCreateTemplateEngineWithCustomSettings() {
      final TemplateEngine engine =
          TemplateEngine.builder()
              .locale(Locale.FRENCH)
              .resourceBundleName("custom-messages")
              .cacheSize(50)
              .cacheExpirationMinutes(30)
              .build();

      assertThat(engine).isNotNull();
      assertThat(engine.getCacheStatistics().getSize()).isZero();
    }

    @Test
    @DisplayName("Should validate cache size is not negative")
    void shouldValidateCacheSizeIsNotNegative() {
      assertThatThrownBy(() -> TemplateEngine.builder().cacheSize(-1).build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cacheSize cannot be negative");
    }

    @Test
    @DisplayName("Should validate cache expiration is not negative")
    void shouldValidateCacheExpirationIsNotNegative() {
      assertThatThrownBy(() -> TemplateEngine.builder().cacheExpirationMinutes(-1).build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cacheExpirationMinutes cannot be negative");
    }
  }

  @Nested
  @DisplayName("Template Processing")
  class TemplateProcessing {

    @Test
    @DisplayName("Should process default HTML template successfully")
    void shouldProcessDefaultHtmlTemplateSuccessfully() throws TemplateProcessingException {
      final ReportTemplate template = ReportTemplate.defaultHtmlTemplate();

      final String result = templateEngine.processTemplate(template, sampleReport, defaultConfig);

      assertThat(result).isNotEmpty();
      assertThat(result).contains("report-header");
      assertThat(result).contains("Test Report");
      assertThat(result).contains("summary");
      assertThat(result).contains("100"); // Total tests
      assertThat(result).contains("95.0%"); // Success rate
    }

    @Test
    @DisplayName("Should process minimal template successfully")
    void shouldProcessMinimalTemplateSuccessfully() throws TemplateProcessingException {
      final ReportTemplate template = ReportTemplate.minimalTemplate();

      final String result = templateEngine.processTemplate(template, sampleReport, defaultConfig);

      assertThat(result).isNotEmpty();
      assertThat(result).contains("summary");
      assertThat(result).contains("recommendations");
      assertThat(result).doesNotContain("metadata"); // Not included in minimal template
    }

    @Test
    @DisplayName("Should process individual component successfully")
    void shouldProcessIndividualComponentSuccessfully() throws TemplateProcessingException {
      final TemplateComponent summaryComponent =
          TemplateComponent.createSummary("summary", "Summary");

      final String result =
          templateEngine.processComponent(summaryComponent, sampleReport, defaultConfig);

      assertThat(result).isNotEmpty();
      assertThat(result).contains("summary");
      assertThat(result).contains("100"); // Total tests
      assertThat(result).contains("SUCCESS"); // Status
    }

    @Test
    @DisplayName("Should skip incompatible components")
    void shouldSkipIncompatibleComponents() throws TemplateProcessingException {
      final ReportConfiguration configWithoutMetadata =
          new ReportConfiguration.Builder("no-metadata")
              .contentConfig(new ContentConfiguration.Builder().includeMetadata(false).build())
              .formattingConfig(FormattingConfiguration.defaultFormattingConfig())
              .themeConfig(ThemeConfiguration.defaultTheme())
              .localizationConfig(LocalizationConfiguration.defaultLocalization())
              .outputConfig(OutputConfiguration.defaultOutputConfig())
              .build();

      final TemplateComponent metadataComponent =
          TemplateComponent.createMetadata("metadata", "Metadata");

      final String result =
          templateEngine.processComponent(metadataComponent, sampleReport, configWithoutMetadata);

      assertThat(result).isEmpty(); // Should be skipped
    }

    @Test
    @DisplayName("Should throw exception for null parameters")
    void shouldThrowExceptionForNullParameters() {
      final ReportTemplate template = ReportTemplate.defaultHtmlTemplate();

      assertThatThrownBy(() -> templateEngine.processTemplate(null, sampleReport, defaultConfig))
          .isInstanceOf(NullPointerException.class);

      assertThatThrownBy(() -> templateEngine.processTemplate(template, null, defaultConfig))
          .isInstanceOf(NullPointerException.class);

      assertThatThrownBy(() -> templateEngine.processTemplate(template, sampleReport, null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should throw exception for incompatible template")
    void shouldThrowExceptionForIncompatibleTemplate() {
      final ReportTemplate htmlTemplate = ReportTemplate.defaultHtmlTemplate();
      final ReportConfiguration jsonOnlyConfig =
          new ReportConfiguration.Builder("json-only")
              .outputConfig(
                  new OutputConfiguration.Builder().generateHtml(false).generateJson(true).build())
              .contentConfig(ContentConfiguration.defaultContentConfig())
              .formattingConfig(FormattingConfiguration.defaultFormattingConfig())
              .themeConfig(ThemeConfiguration.defaultTheme())
              .localizationConfig(LocalizationConfiguration.defaultLocalization())
              .build();

      assertThatThrownBy(
              () -> templateEngine.processTemplate(htmlTemplate, sampleReport, jsonOnlyConfig))
          .isInstanceOf(TemplateProcessingException.class)
          .hasMessageContaining("Template is not compatible with the provided configuration");
    }
  }

  @Nested
  @DisplayName("Template Validation")
  class TemplateValidation {

    @Test
    @DisplayName("Should validate correct template as valid")
    void shouldValidateCorrectTemplateAsValid() {
      final ReportTemplate template = ReportTemplate.defaultHtmlTemplate();

      final TemplateValidationResult result = templateEngine.validateTemplate(template);

      assertThat(result.isValid()).isTrue();
      assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should validate template with errors as invalid")
    void shouldValidateTemplateWithErrorsAsInvalid() {
      final TemplateComponent brokenComponent =
          new TemplateComponent.Builder("broken", "Broken", ComponentType.SUMMARY)
              .templateContent("<div>${unclosedVariable</div>")
              .build();

      final ReportTemplate template =
          new ReportTemplate.Builder("broken", "Broken Template")
              .components(List.of(brokenComponent))
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      final TemplateValidationResult result = templateEngine.validateTemplate(template);

      assertThat(result.isValid()).isFalse();
      assertThat(result.getErrors()).hasSize(1);
      assertThat(result.getErrors().get(0)).contains("Malformed template syntax");
    }

    @Test
    @DisplayName("Should throw exception when processing invalid template")
    void shouldThrowExceptionWhenProcessingInvalidTemplate() {
      final TemplateComponent brokenComponent =
          new TemplateComponent.Builder("broken", "Broken", ComponentType.SUMMARY)
              .templateContent("<div>${unclosedVariable</div>")
              .build();

      final ReportTemplate template =
          new ReportTemplate.Builder("broken", "Broken Template")
              .components(List.of(brokenComponent))
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      assertThatThrownBy(
              () -> templateEngine.processTemplate(template, sampleReport, defaultConfig))
          .isInstanceOf(TemplateProcessingException.class)
          .hasMessageContaining("Template validation failed");
    }
  }

  @Nested
  @DisplayName("Template Caching")
  class TemplateCaching {

    @Test
    @DisplayName("Should cache processed templates")
    void shouldCacheProcessedTemplates() throws TemplateProcessingException {
      final TemplateComponent component = TemplateComponent.createSummary("summary", "Summary");

      // First call - should miss cache
      templateEngine.processComponent(component, sampleReport, defaultConfig);
      CacheStatistics stats = templateEngine.getCacheStatistics();
      assertThat(stats.getMisses()).isEqualTo(1);
      assertThat(stats.getHits()).isEqualTo(0);

      // Second call - should hit cache
      templateEngine.processComponent(component, sampleReport, defaultConfig);
      stats = templateEngine.getCacheStatistics();
      assertThat(stats.getMisses()).isEqualTo(1);
      assertThat(stats.getHits()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should clear cache when requested")
    void shouldClearCacheWhenRequested() throws TemplateProcessingException {
      final TemplateComponent component = TemplateComponent.createSummary("summary", "Summary");

      // Process template to populate cache
      templateEngine.processComponent(component, sampleReport, defaultConfig);
      assertThat(templateEngine.getCacheStatistics().getSize()).isGreaterThan(0);

      // Clear cache
      templateEngine.clearCache();
      assertThat(templateEngine.getCacheStatistics().getSize()).isZero();
    }

    @Test
    @DisplayName("Should provide cache statistics")
    void shouldProvideCacheStatistics() throws TemplateProcessingException {
      final TemplateComponent component1 = TemplateComponent.createSummary("summary", "Summary");
      final TemplateComponent component2 = TemplateComponent.createMetadata("metadata", "Metadata");

      templateEngine.processComponent(component1, sampleReport, defaultConfig);
      templateEngine.processComponent(component2, sampleReport, defaultConfig);
      templateEngine.processComponent(component1, sampleReport, defaultConfig); // Should hit cache

      final CacheStatistics stats = templateEngine.getCacheStatistics();
      assertThat(stats.getSize()).isEqualTo(2);
      assertThat(stats.getMisses()).isEqualTo(2);
      assertThat(stats.getHits()).isEqualTo(1);
      assertThat(stats.getHitRatio()).isEqualTo(1.0 / 3.0);
    }
  }

  @Nested
  @DisplayName("Theme Integration")
  class ThemeIntegration {

    @Test
    @DisplayName("Should include theme data in template processing")
    void shouldIncludeThemeDataInTemplateProcessing() throws TemplateProcessingException {
      final TemplateComponent customComponent =
          new TemplateComponent.Builder("themed", "Themed Component", ComponentType.CUSTOM)
              .templateContent(
                  "<div style=\"color: ${colors.primary}; background:"
                      + " ${colors.background}\">${theme.themeName}</div>")
              .build();

      final ReportTemplate template =
          new ReportTemplate.Builder("themed", "Themed Template")
              .components(List.of(customComponent))
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      final String result = templateEngine.processTemplate(template, sampleReport, defaultConfig);

      assertThat(result).contains("color: #007bff"); // Primary color from default theme
      assertThat(result).contains("background: #ffffff"); // Background color from default theme
      assertThat(result).contains("default"); // Theme name
    }

    @Test
    @DisplayName("Should process dark theme correctly")
    void shouldProcessDarkThemeCorrectly() throws TemplateProcessingException {
      final ReportConfiguration darkConfig =
          new ReportConfiguration.Builder("dark")
              .themeConfig(ThemeConfiguration.darkTheme())
              .contentConfig(ContentConfiguration.defaultContentConfig())
              .formattingConfig(FormattingConfiguration.defaultFormattingConfig())
              .localizationConfig(LocalizationConfiguration.defaultLocalization())
              .outputConfig(OutputConfiguration.defaultOutputConfig())
              .build();

      final TemplateComponent customComponent =
          new TemplateComponent.Builder(
                  "dark-themed", "Dark Themed Component", ComponentType.CUSTOM)
              .templateContent(
                  "<div class=\"${darkMode?then('dark-mode', 'light-mode')}\">Theme:"
                      + " ${theme.themeName}</div>")
              .build();

      final ReportTemplate template =
          new ReportTemplate.Builder("dark", "Dark Template")
              .components(List.of(customComponent))
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      final String result = templateEngine.processTemplate(template, sampleReport, darkConfig);

      assertThat(result).contains("dark-mode");
      assertThat(result).contains("Theme: dark");
    }
  }

  @Nested
  @DisplayName("Template Utilities")
  class TemplateUtilities {

    @Test
    @DisplayName("Should provide utility functions in templates")
    void shouldProvideUtilityFunctionsInTemplates() throws TemplateProcessingException {
      final TemplateComponent utilComponent =
          new TemplateComponent.Builder("util", "Utility Component", ComponentType.CUSTOM)
              .templateContent(
                  """
              <div>
                <span>Percentage: ${util.formatPercentage(0.95)}</span>
                <span>Duration: ${util.formatDuration(5000)}</span>
                <span>Truncated: ${util.truncate("Very long text that should be truncated", 10)}</span>
                <span>Capitalized: ${util.capitalize("hello world")}</span>
              </div>
              """)
              .build();

      final ReportTemplate template =
          new ReportTemplate.Builder("util", "Utility Template")
              .components(List.of(utilComponent))
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      final String result = templateEngine.processTemplate(template, sampleReport, defaultConfig);

      assertThat(result).contains("Percentage: 95.0%");
      assertThat(result).contains("Duration: 5.0 sec");
      assertThat(result).contains("Truncated: Very lo...");
      assertThat(result).contains("Capitalized: Hello world");
    }
  }

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandling {

    @Test
    @DisplayName("Should handle template processing errors gracefully")
    void shouldHandleTemplateProcessingErrorsGracefully() {
      final TemplateComponent errorComponent =
          new TemplateComponent.Builder("error", "Error Component", ComponentType.CUSTOM)
              .templateContent("<div>${nonExistentMethod()}</div>")
              .build();

      final ReportTemplate template =
          new ReportTemplate.Builder("error", "Error Template")
              .components(List.of(errorComponent))
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      assertThatThrownBy(
              () -> templateEngine.processTemplate(template, sampleReport, defaultConfig))
          .isInstanceOf(TemplateProcessingException.class)
          .hasMessageContaining("Failed to process template");
    }

    @Test
    @DisplayName("Should handle component processing errors gracefully")
    void shouldHandleComponentProcessingErrorsGracefully() {
      final TemplateComponent errorComponent =
          new TemplateComponent.Builder("error", "Error Component", ComponentType.CUSTOM)
              .templateContent("<div>${badExpression</div>")
              .build();

      assertThatThrownBy(
              () -> templateEngine.processComponent(errorComponent, sampleReport, defaultConfig))
          .isInstanceOf(TemplateProcessingException.class)
          .hasMessageContaining("Failed to process component error");
    }
  }

  private ComparisonReport createSampleReport() {
    final ReportMetadata metadata =
        new ReportMetadata(
            "1.0.0",
            Instant.now().minusSeconds(3600),
            Instant.now(),
            Map.of("java.version", "23", "os.name", "Linux"),
            Map.of("suite", "smoke", "timeout", "1800"),
            List.of("jni", "panama"));

    final ReportSummary summary =
        new ReportSummary(
            100, // total tests
            5, // behavioral issues
            3, // performance issues
            2, // coverage gaps
            4, // high priority recommendations
            0.85, // compatibility score
            ReportStatus.SUCCESS);

    final TestResults sampleTestResult =
        new TestResults(
            "sample-test",
            Optional.empty(), // No behavioral results for this test
            Optional.empty(), // No performance results for this test
            Optional.empty(), // No coverage results for this test
            Optional.empty(), // No insight results for this test
            Optional.empty() // No recommendation results for this test
            );

    return new ComparisonReport.Builder("test-report-001", "Test Report")
        .testResults(Map.of("sample-test", sampleTestResult))
        .metadata(metadata)
        .summary(summary)
        .build();
  }
}
