package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for the ReportTemplate system. Tests template creation, validation, component
 * management, and configuration compatibility.
 */
class ReportTemplateTest {

  @Nested
  @DisplayName("Template Creation")
  class TemplateCreation {

    @Test
    @DisplayName("Should create default HTML template with all standard components")
    void shouldCreateDefaultHtmlTemplate() {
      final ReportTemplate template = ReportTemplate.defaultHtmlTemplate();

      assertThat(template.getTemplateId()).isEqualTo("default-html");
      assertThat(template.getTemplateName()).isEqualTo("Default HTML Report Template");
      assertThat(template.getTemplateType()).isEqualTo(TemplateType.HTML);
      assertThat(template.getComponents()).hasSize(8);
      assertThat(template.getParentTemplate()).isEmpty();

      // Verify expected components are present
      assertThat(template.getComponent("header")).isPresent();
      assertThat(template.getComponent("summary")).isPresent();
      assertThat(template.getComponent("metadata")).isPresent();
      assertThat(template.getComponent("behavioral")).isPresent();
      assertThat(template.getComponent("performance")).isPresent();
      assertThat(template.getComponent("coverage")).isPresent();
      assertThat(template.getComponent("recommendations")).isPresent();
      assertThat(template.getComponent("footer")).isPresent();
    }

    @Test
    @DisplayName("Should create minimal template with only essential components")
    void shouldCreateMinimalTemplate() {
      final ReportTemplate template = ReportTemplate.minimalTemplate();

      assertThat(template.getTemplateId()).isEqualTo("minimal");
      assertThat(template.getTemplateName()).isEqualTo("Minimal Report Template");
      assertThat(template.getTemplateType()).isEqualTo(TemplateType.HTML);
      assertThat(template.getComponents()).hasSize(2);

      // Verify only essential components are present
      assertThat(template.getComponent("summary")).isPresent();
      assertThat(template.getComponent("recommendations")).isPresent();
    }

    @Test
    @DisplayName("Should create comprehensive template with all available components")
    void shouldCreateComprehensiveTemplate() {
      final ReportTemplate template = ReportTemplate.comprehensiveTemplate();

      assertThat(template.getTemplateId()).isEqualTo("comprehensive");
      assertThat(template.getTemplateName()).isEqualTo("Comprehensive Report Template");
      assertThat(template.getTemplateType()).isEqualTo(TemplateType.HTML);
      assertThat(template.getComponents()).hasSize(13);

      // Verify comprehensive components are present
      assertThat(template.getComponent("title")).isPresent();
      assertThat(template.getComponent("toc")).isPresent();
      assertThat(template.getComponent("executive")).isPresent();
      assertThat(template.getComponent("insights")).isPresent();
      assertThat(template.getComponent("appendix")).isPresent();
    }

    @Test
    @DisplayName("Should create custom template with builder")
    void shouldCreateCustomTemplateWithBuilder() {
      final TemplateComponent customComponent =
          new TemplateComponent.Builder("custom", "Custom Component", ComponentType.CUSTOM)
              .templateContent("<div>Custom content</div>")
              .required(true)
              .build();

      final ReportTemplate template =
          new ReportTemplate.Builder("custom-template", "Custom Template")
              .templateType(TemplateType.JSON)
              .components(List.of(customComponent))
              .templateData(Map.of("customData", "value"))
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      assertThat(template.getTemplateId()).isEqualTo("custom-template");
      assertThat(template.getTemplateName()).isEqualTo("Custom Template");
      assertThat(template.getTemplateType()).isEqualTo(TemplateType.JSON);
      assertThat(template.getComponents()).hasSize(1);
      assertThat(template.getTemplateData()).containsEntry("customData", "value");
    }

    @Test
    @DisplayName("Should fail to build template without required metadata")
    void shouldFailToBuildTemplateWithoutMetadata() {
      assertThatThrownBy(() -> new ReportTemplate.Builder("test", "Test Template").build())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("metadata must be set");
    }
  }

  @Nested
  @DisplayName("Template Validation")
  class TemplateValidation {

    @Test
    @DisplayName("Should validate default template as valid")
    void shouldValidateDefaultTemplateAsValid() {
      final ReportTemplate template = ReportTemplate.defaultHtmlTemplate();
      final TemplateValidationResult result = template.validate();

      assertThat(result.isValid()).isTrue();
      assertThat(result.getErrors()).isEmpty();
      assertThat(result.hasIssues()).isFalse();
    }

    @Test
    @DisplayName("Should detect duplicate component IDs as error")
    void shouldDetectDuplicateComponentIds() {
      final TemplateComponent component1 =
          new TemplateComponent.Builder("duplicate", "Component 1", ComponentType.SUMMARY)
              .templateContent("<div>Content 1</div>")
              .build();

      final TemplateComponent component2 =
          new TemplateComponent.Builder("duplicate", "Component 2", ComponentType.METADATA)
              .templateContent("<div>Content 2</div>")
              .build();

      final ReportTemplate template =
          new ReportTemplate.Builder("test", "Test Template")
              .components(List.of(component1, component2))
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      final TemplateValidationResult result = template.validate();

      assertThat(result.isValid()).isFalse();
      assertThat(result.getErrors()).hasSize(1);
      assertThat(result.getErrors().get(0)).contains("Duplicate component ID: duplicate");
    }

    @Test
    @DisplayName("Should detect malformed template syntax as error")
    void shouldDetectMalformedTemplateSyntax() {
      final TemplateComponent component =
          new TemplateComponent.Builder("malformed", "Malformed Component", ComponentType.SUMMARY)
              .templateContent("<div>${unclosedVariable</div>")
              .build();

      final ReportTemplate template =
          new ReportTemplate.Builder("test", "Test Template")
              .components(List.of(component))
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      final TemplateValidationResult result = template.validate();

      assertThat(result.isValid()).isFalse();
      assertThat(result.getErrors()).hasSize(1);
      assertThat(result.getErrors().get(0))
          .contains("Malformed template syntax in component: malformed");
    }

    @Test
    @DisplayName("Should warn when template has no components")
    void shouldWarnWhenTemplateHasNoComponents() {
      final ReportTemplate template =
          new ReportTemplate.Builder("empty", "Empty Template")
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      final TemplateValidationResult result = template.validate();

      assertThat(result.isValid()).isTrue();
      assertThat(result.getWarnings()).hasSize(1);
      assertThat(result.getWarnings().get(0)).contains("Template has no components");
      assertThat(result.hasIssues()).isTrue();
    }

    @Test
    @DisplayName("Should warn when template has no summary component")
    void shouldWarnWhenTemplateHasNoSummaryComponent() {
      final TemplateComponent component =
          new TemplateComponent.Builder("header", "Header", ComponentType.HEADER)
              .templateContent("<header>Test</header>")
              .build();

      final ReportTemplate template =
          new ReportTemplate.Builder("no-summary", "No Summary Template")
              .components(List.of(component))
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      final TemplateValidationResult result = template.validate();

      assertThat(result.isValid()).isTrue();
      assertThat(result.getWarnings()).hasSize(1);
      assertThat(result.getWarnings().get(0))
          .contains("Template should include a summary component");
    }
  }

  @Nested
  @DisplayName("Component Management")
  class ComponentManagement {

    @Test
    @DisplayName("Should retrieve component by ID")
    void shouldRetrieveComponentById() {
      final ReportTemplate template = ReportTemplate.defaultHtmlTemplate();

      final Optional<TemplateComponent> summaryComponent = template.getComponent("summary");

      assertThat(summaryComponent).isPresent();
      assertThat(summaryComponent.get().getComponentId()).isEqualTo("summary");
      assertThat(summaryComponent.get().getComponentType()).isEqualTo(ComponentType.SUMMARY);
    }

    @Test
    @DisplayName("Should return empty when component ID not found")
    void shouldReturnEmptyWhenComponentNotFound() {
      final ReportTemplate template = ReportTemplate.defaultHtmlTemplate();

      final Optional<TemplateComponent> component = template.getComponent("non-existent");

      assertThat(component).isEmpty();
    }

    @Test
    @DisplayName("Should retrieve components by type")
    void shouldRetrieveComponentsByType() {
      final ReportTemplate template = ReportTemplate.comprehensiveTemplate();

      final List<TemplateComponent> summaryComponents =
          template.getComponentsByType(ComponentType.SUMMARY);

      assertThat(summaryComponents).hasSize(1);
      assertThat(summaryComponents.get(0).getComponentId()).isEqualTo("summary");
    }

    @Test
    @DisplayName("Should return empty list when no components of type found")
    void shouldReturnEmptyListWhenNoComponentsOfTypeFound() {
      final ReportTemplate template = ReportTemplate.minimalTemplate();

      final List<TemplateComponent> metadataComponents =
          template.getComponentsByType(ComponentType.METADATA);

      assertThat(metadataComponents).isEmpty();
    }
  }

  @Nested
  @DisplayName("Configuration Compatibility")
  class ConfigurationCompatibility {

    @Test
    @DisplayName("Should be compatible with default configuration")
    void shouldBeCompatibleWithDefaultConfiguration() {
      final ReportTemplate template = ReportTemplate.defaultHtmlTemplate();
      final ReportConfiguration config = ReportConfiguration.defaultConfiguration();

      assertThat(template.isCompatibleWith(config)).isTrue();
    }

    @Test
    @DisplayName("Should be compatible with comprehensive configuration")
    void shouldBeCompatibleWithComprehensiveConfiguration() {
      final ReportTemplate template = ReportTemplate.comprehensiveTemplate();
      final ReportConfiguration config = ReportConfiguration.comprehensiveConfiguration();

      assertThat(template.isCompatibleWith(config)).isTrue();
    }

    @Test
    @DisplayName("Should be incompatible when output format not supported")
    void shouldBeIncompatibleWhenOutputFormatNotSupported() {
      final ReportTemplate htmlTemplate = ReportTemplate.defaultHtmlTemplate();
      final ReportConfiguration jsonOnlyConfig =
          new ReportConfiguration.Builder("json-only")
              .outputConfig(
                  new OutputConfiguration.Builder()
                      .generateHtml(false)
                      .generateJson(true)
                      .generateCsv(false)
                      .generateConsole(false)
                      .build())
              .contentConfig(ContentConfiguration.defaultContentConfig())
              .formattingConfig(FormattingConfiguration.defaultFormattingConfig())
              .themeConfig(ThemeConfiguration.defaultTheme())
              .localizationConfig(LocalizationConfiguration.defaultLocalization())
              .build();

      assertThat(htmlTemplate.isCompatibleWith(jsonOnlyConfig)).isFalse();
    }

    @Test
    @DisplayName("Should be compatible when all required components are enabled")
    void shouldBeCompatibleWhenRequiredComponentsEnabled() {
      final ReportTemplate template = ReportTemplate.minimalTemplate();
      final ReportConfiguration config = ReportConfiguration.minimalConfiguration();

      assertThat(template.isCompatibleWith(config)).isTrue();
    }
  }

  @Nested
  @DisplayName("Template Data")
  class TemplateData {

    @Test
    @DisplayName("Should store and retrieve template data")
    void shouldStoreAndRetrieveTemplateData() {
      final Map<String, Object> data =
          Map.of(
              "title", "Custom Report",
              "version", "1.0.0",
              "debug", true);

      final ReportTemplate template =
          new ReportTemplate.Builder("custom", "Custom Template")
              .templateData(data)
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      assertThat(template.getTemplateData()).containsAllEntriesOf(data);
    }

    @Test
    @DisplayName("Should handle empty template data")
    void shouldHandleEmptyTemplateData() {
      final ReportTemplate template =
          new ReportTemplate.Builder("empty-data", "Empty Data Template")
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      assertThat(template.getTemplateData()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Template Metadata")
  class TemplateMetadataTests {

    @Test
    @DisplayName("Should store template metadata correctly")
    void shouldStoreTemplateMetadata() {
      final TemplateMetadata metadata =
          new TemplateMetadata.Builder()
              .version("2.0.0")
              .author("Test Author")
              .description("Test template for validation")
              .supportedFormats(List.of("HTML", "JSON"))
              .customMetadata(Map.of("customKey", "customValue"))
              .build();

      final ReportTemplate template =
          new ReportTemplate.Builder("test", "Test Template").metadata(metadata).build();

      assertThat(template.getMetadata()).isEqualTo(metadata);
      assertThat(template.getMetadata().getVersion()).isEqualTo("2.0.0");
      assertThat(template.getMetadata().getAuthor()).isEqualTo("Test Author");
      assertThat(template.getMetadata().getSupportedFormats()).containsExactly("HTML", "JSON");
    }

    @Test
    @DisplayName("Should use default metadata when not specified")
    void shouldUseDefaultMetadata() {
      final TemplateMetadata defaultMetadata = TemplateMetadata.defaultMetadata();

      assertThat(defaultMetadata.getVersion()).isEqualTo("1.0.0");
      assertThat(defaultMetadata.getAuthor()).isEqualTo("Wasmtime4j Comparison Suite");
      assertThat(defaultMetadata.getDescription())
          .isEqualTo("Default template for comparison reports");
      assertThat(defaultMetadata.getSupportedFormats())
          .containsExactly("HTML", "JSON", "CSV", "CONSOLE");
    }
  }

  @Nested
  @DisplayName("Parent Template Inheritance")
  class ParentTemplateInheritance {

    @Test
    @DisplayName("Should support parent template hierarchy")
    void shouldSupportParentTemplateHierarchy() {
      final ReportTemplate baseTemplate = ReportTemplate.defaultHtmlTemplate();

      final ReportTemplate childTemplate =
          new ReportTemplate.Builder("child", "Child Template")
              .parentTemplate(baseTemplate)
              .metadata(TemplateMetadata.defaultMetadata())
              .build();

      assertThat(childTemplate.getParentTemplate()).isPresent();
      assertThat(childTemplate.getParentTemplate().get()).isEqualTo(baseTemplate);
    }

    @Test
    @DisplayName("Should handle templates without parent")
    void shouldHandleTemplatesWithoutParent() {
      final ReportTemplate template = ReportTemplate.defaultHtmlTemplate();

      assertThat(template.getParentTemplate()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Equality and Hash Code")
  class EqualityAndHashCode {

    @Test
    @DisplayName("Should be equal when all properties match")
    void shouldBeEqualWhenAllPropertiesMatch() {
      final ReportTemplate template1 = ReportTemplate.defaultHtmlTemplate();
      final ReportTemplate template2 = ReportTemplate.defaultHtmlTemplate();

      assertThat(template1).isEqualTo(template2);
      assertThat(template1.hashCode()).isEqualTo(template2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when properties differ")
    void shouldNotBeEqualWhenPropertiesDiffer() {
      final ReportTemplate template1 = ReportTemplate.defaultHtmlTemplate();
      final ReportTemplate template2 = ReportTemplate.minimalTemplate();

      assertThat(template1).isNotEqualTo(template2);
    }

    @Test
    @DisplayName("Should not be equal to null or different class")
    void shouldNotBeEqualToNullOrDifferentClass() {
      final ReportTemplate template = ReportTemplate.defaultHtmlTemplate();

      assertThat(template).isNotEqualTo(null);
      assertThat(template).isNotEqualTo("string");
    }
  }

  @Nested
  @DisplayName("String Representation")
  class StringRepresentation {

    @Test
    @DisplayName("Should provide meaningful toString")
    void shouldProvideMeaningfulToString() {
      final ReportTemplate template = ReportTemplate.defaultHtmlTemplate();

      final String toString = template.toString();

      assertThat(toString).contains("ReportTemplate");
      assertThat(toString).contains("id='default-html'");
      assertThat(toString).contains("name='Default HTML Report Template'");
      assertThat(toString).contains("type=HTML");
      assertThat(toString).contains("components=8");
    }
  }
}
