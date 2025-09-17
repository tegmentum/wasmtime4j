package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for the ReportConfiguration system. Tests configuration creation, content
 * filtering, theme settings, and localization.
 */
class ReportConfigurationTest {

  @Nested
  @DisplayName("Configuration Creation")
  class ConfigurationCreation {

    @Test
    @DisplayName("Should create default configuration with expected settings")
    void shouldCreateDefaultConfiguration() {
      final ReportConfiguration config = ReportConfiguration.defaultConfiguration();

      assertThat(config.getConfigurationName()).isEqualTo("default");

      // Content configuration
      final ContentConfiguration content = config.getContentConfig();
      assertThat(content.isIncludeSummary()).isTrue();
      assertThat(content.isIncludeMetadata()).isTrue();
      assertThat(content.isIncludeBehavioralAnalysis()).isTrue();
      assertThat(content.isIncludePerformanceAnalysis()).isTrue();
      assertThat(content.isIncludeCoverageAnalysis()).isTrue();
      assertThat(content.isIncludeRecommendations()).isTrue();
      assertThat(content.isIncludeDetailedResults()).isTrue();
      assertThat(content.isIncludeRawData()).isFalse();
      assertThat(content.getMaxTestResults()).isEqualTo(1000);
      assertThat(content.getRecommendationLevel()).isEqualTo(RecommendationLevel.MEDIUM);

      // Output configuration
      final OutputConfiguration output = config.getOutputConfig();
      assertThat(output.isGenerateHtml()).isTrue();
      assertThat(output.isGenerateJson()).isTrue();
      assertThat(output.isGenerateCsv()).isFalse();
      assertThat(output.isGenerateConsole()).isTrue();

      // Theme configuration
      final ThemeConfiguration theme = config.getThemeConfig();
      assertThat(theme.getThemeName()).isEqualTo("default");
      assertThat(theme.isDarkMode()).isFalse();

      // Localization configuration
      final LocalizationConfiguration localization = config.getLocalizationConfig();
      assertThat(localization.getLocale()).isEqualTo(Locale.getDefault());
      assertThat(localization.getResourceBundleName()).isEqualTo("comparison-messages");
    }

    @Test
    @DisplayName("Should create minimal configuration with essential settings")
    void shouldCreateMinimalConfiguration() {
      final ReportConfiguration config = ReportConfiguration.minimalConfiguration();

      assertThat(config.getConfigurationName()).isEqualTo("minimal");

      final ContentConfiguration content = config.getContentConfig();
      assertThat(content.isIncludeSummary()).isTrue();
      assertThat(content.isIncludeMetadata()).isFalse();
      assertThat(content.isIncludeBehavioralAnalysis()).isFalse();
      assertThat(content.isIncludePerformanceAnalysis()).isFalse();
      assertThat(content.isIncludeCoverageAnalysis()).isFalse();
      assertThat(content.isIncludeRecommendations()).isTrue();
      assertThat(content.isIncludeDetailedResults()).isFalse();
      assertThat(content.getMaxTestResults()).isEqualTo(100);
      assertThat(content.getRecommendationLevel()).isEqualTo(RecommendationLevel.HIGH);
    }

    @Test
    @DisplayName("Should create comprehensive configuration with all features enabled")
    void shouldCreateComprehensiveConfiguration() {
      final ReportConfiguration config = ReportConfiguration.comprehensiveConfiguration();

      assertThat(config.getConfigurationName()).isEqualTo("comprehensive");

      final ContentConfiguration content = config.getContentConfig();
      assertThat(content.isIncludeSummary()).isTrue();
      assertThat(content.isIncludeMetadata()).isTrue();
      assertThat(content.isIncludeBehavioralAnalysis()).isTrue();
      assertThat(content.isIncludePerformanceAnalysis()).isTrue();
      assertThat(content.isIncludeCoverageAnalysis()).isTrue();
      assertThat(content.isIncludeRecommendations()).isTrue();
      assertThat(content.isIncludeDetailedResults()).isTrue();
      assertThat(content.isIncludeRawData()).isTrue();
      assertThat(content.getMaxTestResults()).isEqualTo(Integer.MAX_VALUE);
      assertThat(content.getRecommendationLevel()).isEqualTo(RecommendationLevel.ALL);

      final OutputConfiguration output = config.getOutputConfig();
      assertThat(output.isGenerateHtml()).isTrue();
      assertThat(output.isGenerateJson()).isTrue();
      assertThat(output.isGenerateCsv()).isTrue();
      assertThat(output.isGenerateConsole()).isTrue();
    }

    @Test
    @DisplayName("Should create custom configuration with builder")
    void shouldCreateCustomConfigurationWithBuilder() {
      final Map<String, Object> customProps =
          Map.of("debug", true, "maxMemory", 1024L, "outputDir", "/tmp/reports");

      final ReportConfiguration config =
          new ReportConfiguration.Builder("custom")
              .contentConfig(
                  new ContentConfiguration.Builder()
                      .includeSummary(true)
                      .includeMetadata(false)
                      .maxTestResults(500)
                      .build())
              .themeConfig(ThemeConfiguration.darkTheme())
              .customProperties(customProps)
              .build();

      assertThat(config.getConfigurationName()).isEqualTo("custom");
      assertThat(config.getContentConfig().isIncludeSummary()).isTrue();
      assertThat(config.getContentConfig().isIncludeMetadata()).isFalse();
      assertThat(config.getThemeConfig().isDarkMode()).isTrue();
      assertThat(config.getCustomProperties()).containsAllEntriesOf(customProps);
    }
  }

  @Nested
  @DisplayName("Content Configuration")
  class ContentConfigurationTests {

    @Test
    @DisplayName("Should filter tests based on excluded list")
    void shouldFilterTestsBasedOnExcludedList() {
      final ContentConfiguration config =
          new ContentConfiguration.Builder().excludedTests(Set.of("test1", "test2")).build();

      assertThat(config.shouldIncludeTest("test1")).isFalse();
      assertThat(config.shouldIncludeTest("test2")).isFalse();
      assertThat(config.shouldIncludeTest("test3")).isTrue();
    }

    @Test
    @DisplayName("Should include all tests when excluded list is empty")
    void shouldIncludeAllTestsWhenExcludedListEmpty() {
      final ContentConfiguration config = new ContentConfiguration.Builder().build();

      assertThat(config.shouldIncludeTest("any-test")).isTrue();
    }

    @Test
    @DisplayName("Should filter sections based on included list")
    void shouldFilterSectionsBasedOnIncludedList() {
      final ContentConfiguration config =
          new ContentConfiguration.Builder()
              .includedSections(Set.of("summary", "recommendations"))
              .build();

      assertThat(config.shouldIncludeSection("summary")).isTrue();
      assertThat(config.shouldIncludeSection("recommendations")).isTrue();
      assertThat(config.shouldIncludeSection("metadata")).isFalse();
    }

    @Test
    @DisplayName("Should include all sections when included list is empty")
    void shouldIncludeAllSectionsWhenIncludedListEmpty() {
      final ContentConfiguration config = new ContentConfiguration.Builder().build();

      assertThat(config.shouldIncludeSection("any-section")).isTrue();
    }

    @Test
    @DisplayName("Should validate max test results is positive")
    void shouldValidateMaxTestResultsIsPositive() {
      assertThatThrownBy(() -> new ContentConfiguration.Builder().maxTestResults(0).build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("maxTestResults must be positive");

      assertThatThrownBy(() -> new ContentConfiguration.Builder().maxTestResults(-1).build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("maxTestResults must be positive");
    }
  }

  @Nested
  @DisplayName("Formatting Configuration")
  class FormattingConfigurationTests {

    @Test
    @DisplayName("Should create default formatting configuration")
    void shouldCreateDefaultFormattingConfiguration() {
      final FormattingConfiguration config = FormattingConfiguration.defaultFormattingConfig();

      assertThat(config.isUseColorOutput()).isTrue();
      assertThat(config.isIncludeTimestamps()).isTrue();
      assertThat(config.isIncludeLineNumbers()).isFalse();
      assertThat(config.getDateTimeFormat()).isEqualTo(DateTimeFormat.ISO_8601);
      assertThat(config.getNumberFormat()).isEqualTo(NumberFormat.STANDARD);
      assertThat(config.getMaxLineLength()).isEqualTo(120);
      assertThat(config.isCompactOutput()).isFalse();
      assertThat(config.isIncludeStackTraces()).isFalse();
    }

    @Test
    @DisplayName("Should create compact formatting configuration")
    void shouldCreateCompactFormattingConfiguration() {
      final FormattingConfiguration config = FormattingConfiguration.compactFormattingConfig();

      assertThat(config.isUseColorOutput()).isFalse();
      assertThat(config.isIncludeTimestamps()).isFalse();
      assertThat(config.isCompactOutput()).isTrue();
      assertThat(config.getMaxLineLength()).isEqualTo(80);
      assertThat(config.getDateTimeFormat()).isEqualTo(DateTimeFormat.SHORT);
      assertThat(config.getNumberFormat()).isEqualTo(NumberFormat.COMPACT);
    }

    @Test
    @DisplayName("Should create detailed formatting configuration")
    void shouldCreateDetailedFormattingConfiguration() {
      final FormattingConfiguration config = FormattingConfiguration.detailedFormattingConfig();

      assertThat(config.isIncludeLineNumbers()).isTrue();
      assertThat(config.isIncludeStackTraces()).isTrue();
      assertThat(config.getMaxLineLength()).isEqualTo(150);
      assertThat(config.getDateTimeFormat()).isEqualTo(DateTimeFormat.FULL);
      assertThat(config.getNumberFormat()).isEqualTo(NumberFormat.DETAILED);
    }

    @Test
    @DisplayName("Should validate max line length is positive")
    void shouldValidateMaxLineLengthIsPositive() {
      assertThatThrownBy(() -> new FormattingConfiguration.Builder().maxLineLength(0).build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("maxLineLength must be positive");
    }
  }

  @Nested
  @DisplayName("Theme Configuration")
  class ThemeConfigurationTests {

    @Test
    @DisplayName("Should create default theme")
    void shouldCreateDefaultTheme() {
      final ThemeConfiguration theme = ThemeConfiguration.defaultTheme();

      assertThat(theme.getThemeName()).isEqualTo("default");
      assertThat(theme.isDarkMode()).isFalse();
      assertThat(theme.getColors()).containsKey("primary");
      assertThat(theme.getColors()).containsKey("background");
      assertThat(theme.getFonts()).containsKey("body");
      assertThat(theme.getFonts()).containsKey("monospace");
    }

    @Test
    @DisplayName("Should create dark theme")
    void shouldCreateDarkTheme() {
      final ThemeConfiguration theme = ThemeConfiguration.darkTheme();

      assertThat(theme.getThemeName()).isEqualTo("dark");
      assertThat(theme.isDarkMode()).isTrue();
      assertThat(theme.getColors().get("background")).isEqualTo("#1a1a1a");
      assertThat(theme.getColors().get("text")).isEqualTo("#ffffff");
    }

    @Test
    @DisplayName("Should create minimal theme")
    void shouldCreateMinimalTheme() {
      final ThemeConfiguration theme = ThemeConfiguration.minimalTheme();

      assertThat(theme.getThemeName()).isEqualTo("minimal");
      assertThat(theme.getColors()).hasSize(3); // Only essential colors
      assertThat(theme.getFonts()).hasSize(2); // Only essential fonts
    }

    @Test
    @DisplayName("Should support custom colors and fonts")
    void shouldSupportCustomColorsAndFonts() {
      final Map<String, String> customColors =
          Map.of(
              "brand", "#ff5722",
              "accent", "#03a9f4");

      final Map<String, String> customFonts =
          Map.of(
              "heading", "Georgia, serif",
              "body", "Arial, sans-serif");

      final ThemeConfiguration theme =
          new ThemeConfiguration.Builder("custom")
              .colors(customColors)
              .fonts(customFonts)
              .brandingLogo("logo.png")
              .brandingFooter("© 2023 Custom Company")
              .build();

      assertThat(theme.getColors()).containsAllEntriesOf(customColors);
      assertThat(theme.getFonts()).containsAllEntriesOf(customFonts);
      assertThat(theme.getBrandingLogo()).isEqualTo("logo.png");
      assertThat(theme.getBrandingFooter()).isEqualTo("© 2023 Custom Company");
    }
  }

  @Nested
  @DisplayName("Localization Configuration")
  class LocalizationConfigurationTests {

    @Test
    @DisplayName("Should create default localization configuration")
    void shouldCreateDefaultLocalizationConfiguration() {
      final LocalizationConfiguration config = LocalizationConfiguration.defaultLocalization();

      assertThat(config.getLocale()).isEqualTo(Locale.getDefault());
      assertThat(config.getTimeZone()).isEqualTo(java.time.ZoneId.systemDefault().getId());
      assertThat(config.getResourceBundleName()).isEqualTo("comparison-messages");
    }

    @Test
    @DisplayName("Should create English localization configuration")
    void shouldCreateEnglishLocalizationConfiguration() {
      final LocalizationConfiguration config = LocalizationConfiguration.englishLocalization();

      assertThat(config.getLocale()).isEqualTo(Locale.ENGLISH);
      assertThat(config.getTimeZone()).isEqualTo("UTC");
      assertThat(config.getResourceBundleName()).isEqualTo("comparison-messages");
    }

    @Test
    @DisplayName("Should support custom locale and timezone")
    void shouldSupportCustomLocaleAndTimezone() {
      final Map<String, String> customMessages =
          Map.of(
              "welcome", "Willkommen",
              "goodbye", "Auf Wiedersehen");

      final LocalizationConfiguration config =
          new LocalizationConfiguration.Builder(Locale.GERMAN)
              .timeZone("Europe/Berlin")
              .customMessages(customMessages)
              .resourceBundleName("custom-messages")
              .build();

      assertThat(config.getLocale()).isEqualTo(Locale.GERMAN);
      assertThat(config.getTimeZone()).isEqualTo("Europe/Berlin");
      assertThat(config.getCustomMessages()).containsAllEntriesOf(customMessages);
      assertThat(config.getResourceBundleName()).isEqualTo("custom-messages");
    }
  }

  @Nested
  @DisplayName("Output Configuration")
  class OutputConfigurationTests {

    @Test
    @DisplayName("Should create default output configuration")
    void shouldCreateDefaultOutputConfiguration() {
      final OutputConfiguration config = OutputConfiguration.defaultOutputConfig();

      assertThat(config.isGenerateHtml()).isTrue();
      assertThat(config.isGenerateJson()).isTrue();
      assertThat(config.isGenerateCsv()).isFalse();
      assertThat(config.isGenerateConsole()).isTrue();
      assertThat(config.isEnableCaching()).isTrue();
      assertThat(config.getMaxCacheSize()).isEqualTo(100);
      assertThat(config.getCacheExpirationMinutes()).isEqualTo(60);
      assertThat(config.isStreamOutput()).isFalse();
      assertThat(config.getOutputBufferSize()).isEqualTo(8192);
    }

    @Test
    @DisplayName("Should create fast output configuration")
    void shouldCreateFastOutputConfiguration() {
      final OutputConfiguration config = OutputConfiguration.fastOutputConfig();

      assertThat(config.isGenerateHtml()).isFalse();
      assertThat(config.isGenerateJson()).isTrue();
      assertThat(config.isEnableCaching()).isFalse();
      assertThat(config.isStreamOutput()).isTrue();
      assertThat(config.getOutputBufferSize()).isEqualTo(4096);
    }

    @Test
    @DisplayName("Should create detailed output configuration")
    void shouldCreateDetailedOutputConfiguration() {
      final OutputConfiguration config = OutputConfiguration.detailedOutputConfig();

      assertThat(config.isGenerateHtml()).isTrue();
      assertThat(config.isGenerateJson()).isTrue();
      assertThat(config.isGenerateCsv()).isTrue();
      assertThat(config.isGenerateConsole()).isTrue();
      assertThat(config.getMaxCacheSize()).isEqualTo(500);
      assertThat(config.getCacheExpirationMinutes()).isEqualTo(120);
      assertThat(config.getOutputBufferSize()).isEqualTo(16384);
    }

    @Test
    @DisplayName("Should validate cache size is not negative")
    void shouldValidateCacheSizeIsNotNegative() {
      assertThatThrownBy(() -> new OutputConfiguration.Builder().maxCacheSize(-1).build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("maxCacheSize cannot be negative");
    }

    @Test
    @DisplayName("Should validate output buffer size is positive")
    void shouldValidateOutputBufferSizeIsPositive() {
      assertThatThrownBy(() -> new OutputConfiguration.Builder().outputBufferSize(0).build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("outputBufferSize must be positive");
    }
  }

  @Nested
  @DisplayName("Custom Properties")
  class CustomProperties {

    @Test
    @DisplayName("Should store and retrieve custom properties")
    void shouldStoreAndRetrieveCustomProperties() {
      final Map<String, Object> customProps =
          Map.of("stringProp", "value", "intProp", 42, "boolProp", true, "doubleProp", 3.14);

      final ReportConfiguration config =
          new ReportConfiguration.Builder("custom").customProperties(customProps).build();

      assertThat(config.getCustomProperties()).containsAllEntriesOf(customProps);
      assertThat(config.getCustomProperty("stringProp", String.class)).hasValue("value");
      assertThat(config.getCustomProperty("intProp", Integer.class)).hasValue(42);
      assertThat(config.getCustomProperty("boolProp", Boolean.class)).hasValue(true);
      assertThat(config.getCustomProperty("doubleProp", Double.class)).hasValue(3.14);
    }

    @Test
    @DisplayName("Should return empty for non-existent properties")
    void shouldReturnEmptyForNonExistentProperties() {
      final ReportConfiguration config = ReportConfiguration.defaultConfiguration();

      assertThat(config.getCustomProperty("nonExistent", String.class)).isEmpty();
    }

    @Test
    @DisplayName("Should return empty for wrong type properties")
    void shouldReturnEmptyForWrongTypeProperties() {
      final Map<String, Object> customProps = Map.of("stringProp", "value");

      final ReportConfiguration config =
          new ReportConfiguration.Builder("custom").customProperties(customProps).build();

      assertThat(config.getCustomProperty("stringProp", Integer.class)).isEmpty();
    }
  }

  @Nested
  @DisplayName("Equality and Hash Code")
  class EqualityAndHashCode {

    @Test
    @DisplayName("Should be equal when all properties match")
    void shouldBeEqualWhenAllPropertiesMatch() {
      final ReportConfiguration config1 = ReportConfiguration.defaultConfiguration();
      final ReportConfiguration config2 = ReportConfiguration.defaultConfiguration();

      assertThat(config1).isEqualTo(config2);
      assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when properties differ")
    void shouldNotBeEqualWhenPropertiesDiffer() {
      final ReportConfiguration config1 = ReportConfiguration.defaultConfiguration();
      final ReportConfiguration config2 = ReportConfiguration.minimalConfiguration();

      assertThat(config1).isNotEqualTo(config2);
    }

    @Test
    @DisplayName("Should not be equal to null or different class")
    void shouldNotBeEqualToNullOrDifferentClass() {
      final ReportConfiguration config = ReportConfiguration.defaultConfiguration();

      assertThat(config).isNotEqualTo(null);
      assertThat(config).isNotEqualTo("string");
    }
  }

  @Nested
  @DisplayName("String Representation")
  class StringRepresentation {

    @Test
    @DisplayName("Should provide meaningful toString")
    void shouldProvideMeaningfulToString() {
      final ReportConfiguration config = ReportConfiguration.defaultConfiguration();

      final String toString = config.toString();

      assertThat(toString).contains("ReportConfiguration");
      assertThat(toString).contains("name='default'");
      assertThat(toString).contains("theme=default");
      assertThat(toString).contains("locale=");
    }
  }
}
