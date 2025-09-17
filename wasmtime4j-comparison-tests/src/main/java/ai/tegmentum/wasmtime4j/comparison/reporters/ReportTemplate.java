package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Component-based template system for generating customizable reports with reusable sections.
 * Supports hierarchical template composition and validation.
 *
 * @since 1.0.0
 */
public final class ReportTemplate {
  private final String templateId;
  private final String templateName;
  private final TemplateType templateType;
  private final List<TemplateComponent> components;
  private final Map<String, Object> templateData;
  private final TemplateMetadata metadata;
  private final Optional<ReportTemplate> parentTemplate;

  private ReportTemplate(final Builder builder) {
    this.templateId = Objects.requireNonNull(builder.templateId, "templateId cannot be null");
    this.templateName = Objects.requireNonNull(builder.templateName, "templateName cannot be null");
    this.templateType = Objects.requireNonNull(builder.templateType, "templateType cannot be null");
    this.components = List.copyOf(builder.components);
    this.templateData = Map.copyOf(builder.templateData);
    this.metadata = Objects.requireNonNull(builder.metadata, "metadata cannot be null");
    this.parentTemplate = builder.parentTemplate;
  }

  public String getTemplateId() {
    return templateId;
  }

  public String getTemplateName() {
    return templateName;
  }

  public TemplateType getTemplateType() {
    return templateType;
  }

  public List<TemplateComponent> getComponents() {
    return components;
  }

  public Map<String, Object> getTemplateData() {
    return templateData;
  }

  public TemplateMetadata getMetadata() {
    return metadata;
  }

  public Optional<ReportTemplate> getParentTemplate() {
    return parentTemplate;
  }

  /**
   * Gets a component by its ID.
   *
   * @param componentId the component ID
   * @return the component if found
   */
  public Optional<TemplateComponent> getComponent(final String componentId) {
    return components.stream()
        .filter(component -> component.getComponentId().equals(componentId))
        .findFirst();
  }

  /**
   * Gets all components of a specific type.
   *
   * @param componentType the component type
   * @return list of components of the specified type
   */
  public List<TemplateComponent> getComponentsByType(final ComponentType componentType) {
    return components.stream()
        .filter(component -> component.getComponentType() == componentType)
        .toList();
  }

  /**
   * Validates the template structure and components.
   *
   * @return validation result with any errors found
   */
  public TemplateValidationResult validate() {
    final TemplateValidator validator = new TemplateValidator();
    return validator.validate(this);
  }

  /**
   * Checks if the template is compatible with a given configuration.
   *
   * @param configuration the report configuration
   * @return true if compatible
   */
  public boolean isCompatibleWith(final ReportConfiguration configuration) {
    // Check if template type supports the configuration's output types
    if (templateType == TemplateType.HTML && !configuration.getOutputConfig().isGenerateHtml()) {
      return false;
    }
    if (templateType == TemplateType.JSON && !configuration.getOutputConfig().isGenerateJson()) {
      return false;
    }
    if (templateType == TemplateType.CSV && !configuration.getOutputConfig().isGenerateCsv()) {
      return false;
    }
    if (templateType == TemplateType.CONSOLE
        && !configuration.getOutputConfig().isGenerateConsole()) {
      return false;
    }

    // Check if all required components are enabled in configuration
    return components.stream().allMatch(component -> component.isCompatibleWith(configuration));
  }

  /**
   * Creates a default HTML template with standard components.
   *
   * @return default HTML template
   */
  public static ReportTemplate defaultHtmlTemplate() {
    return new Builder("default-html", "Default HTML Report Template")
        .templateType(TemplateType.HTML)
        .components(
            List.of(
                TemplateComponent.createHeader("header", "Report Header"),
                TemplateComponent.createSummary("summary", "Executive Summary"),
                TemplateComponent.createMetadata("metadata", "Report Metadata"),
                TemplateComponent.createBehavioralSection("behavioral", "Behavioral Analysis"),
                TemplateComponent.createPerformanceSection("performance", "Performance Analysis"),
                TemplateComponent.createCoverageSection("coverage", "Coverage Analysis"),
                TemplateComponent.createRecommendations("recommendations", "Recommendations"),
                TemplateComponent.createFooter("footer", "Report Footer")))
        .metadata(TemplateMetadata.defaultMetadata())
        .build();
  }

  /**
   * Creates a minimal template with only essential components.
   *
   * @return minimal template
   */
  public static ReportTemplate minimalTemplate() {
    return new Builder("minimal", "Minimal Report Template")
        .templateType(TemplateType.HTML)
        .components(
            List.of(
                TemplateComponent.createSummary("summary", "Summary"),
                TemplateComponent.createRecommendations("recommendations", "Key Recommendations")))
        .metadata(TemplateMetadata.defaultMetadata())
        .build();
  }

  /**
   * Creates a comprehensive template with all available components.
   *
   * @return comprehensive template
   */
  public static ReportTemplate comprehensiveTemplate() {
    return new Builder("comprehensive", "Comprehensive Report Template")
        .templateType(TemplateType.HTML)
        .components(
            List.of(
                TemplateComponent.createHeader("header", "Report Header"),
                TemplateComponent.createTitlePage("title", "Title Page"),
                TemplateComponent.createTableOfContents("toc", "Table of Contents"),
                TemplateComponent.createExecutiveSummary("executive", "Executive Summary"),
                TemplateComponent.createSummary("summary", "Summary"),
                TemplateComponent.createMetadata("metadata", "Report Metadata"),
                TemplateComponent.createBehavioralSection("behavioral", "Behavioral Analysis"),
                TemplateComponent.createPerformanceSection("performance", "Performance Analysis"),
                TemplateComponent.createCoverageSection("coverage", "Coverage Analysis"),
                TemplateComponent.createInsights("insights", "Key Insights"),
                TemplateComponent.createRecommendations("recommendations", "Recommendations"),
                TemplateComponent.createAppendix("appendix", "Appendix"),
                TemplateComponent.createFooter("footer", "Report Footer")))
        .metadata(TemplateMetadata.defaultMetadata())
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

    final ReportTemplate that = (ReportTemplate) obj;
    return Objects.equals(templateId, that.templateId)
        && Objects.equals(templateName, that.templateName)
        && templateType == that.templateType
        && Objects.equals(components, that.components)
        && Objects.equals(templateData, that.templateData)
        && Objects.equals(metadata, that.metadata)
        && Objects.equals(parentTemplate, that.parentTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        templateId, templateName, templateType, components, templateData, metadata, parentTemplate);
  }

  @Override
  public String toString() {
    return "ReportTemplate{"
        + "id='"
        + templateId
        + '\''
        + ", name='"
        + templateName
        + '\''
        + ", type="
        + templateType
        + ", components="
        + components.size()
        + '}';
  }

  /** Builder for ReportTemplate. */
  public static final class Builder {
    private final String templateId;
    private final String templateName;
    private TemplateType templateType = TemplateType.HTML;
    private List<TemplateComponent> components = Collections.emptyList();
    private Map<String, Object> templateData = Collections.emptyMap();
    private TemplateMetadata metadata = TemplateMetadata.defaultMetadata();
    private Optional<ReportTemplate> parentTemplate = Optional.empty();

    public Builder(final String templateId, final String templateName) {
      this.templateId = Objects.requireNonNull(templateId, "templateId cannot be null");
      this.templateName = Objects.requireNonNull(templateName, "templateName cannot be null");
    }

    public Builder templateType(final TemplateType templateType) {
      this.templateType = Objects.requireNonNull(templateType, "templateType cannot be null");
      return this;
    }

    public Builder components(final List<TemplateComponent> components) {
      this.components = Objects.requireNonNull(components, "components cannot be null");
      return this;
    }

    public Builder templateData(final Map<String, Object> templateData) {
      this.templateData = Objects.requireNonNull(templateData, "templateData cannot be null");
      return this;
    }

    public Builder metadata(final TemplateMetadata metadata) {
      this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");
      return this;
    }

    public Builder parentTemplate(final ReportTemplate parentTemplate) {
      this.parentTemplate = Optional.ofNullable(parentTemplate);
      return this;
    }

    public ReportTemplate build() {
      return new ReportTemplate(this);
    }
  }
}
