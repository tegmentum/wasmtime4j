package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Map;
import java.util.Objects;

/**
 * A component of a report template.
 *
 * @since 1.0.0
 */
public final class TemplateComponent {
  private final String componentId;
  private final String templateContent;
  private final Map<String, Object> componentData;
  private final boolean conditional;
  private final String conditionExpression;

  public TemplateComponent(
      final String componentId,
      final String templateContent,
      final Map<String, Object> componentData,
      final boolean conditional,
      final String conditionExpression) {
    this.componentId = Objects.requireNonNull(componentId, "componentId cannot be null");
    this.templateContent = Objects.requireNonNull(templateContent, "templateContent cannot be null");
    this.componentData = Map.copyOf(Objects.requireNonNull(componentData, "componentData cannot be null"));
    this.conditional = conditional;
    this.conditionExpression = conditionExpression;
  }

  public String getComponentId() {
    return componentId;
  }

  public String getTemplateContent() {
    return templateContent;
  }

  public Map<String, Object> getComponentData() {
    return componentData;
  }

  public boolean isConditional() {
    return conditional;
  }

  public String getConditionExpression() {
    return conditionExpression;
  }

  /**
   * Checks if this component is compatible with the given report configuration.
   *
   * @param config the report configuration
   * @return true if compatible
   */
  public boolean isCompatibleWith(final ReportConfiguration config) {
    Objects.requireNonNull(config, "config cannot be null");

    if (!conditional) {
      return true;
    }

    // Simple condition evaluation based on configuration
    if (conditionExpression == null) {
      return true;
    }

    // Basic condition matching
    final String condition = conditionExpression.toLowerCase();
    if (condition.contains("summary")) {
      return config.getContentConfig().isIncludeSummary();
    }
    if (condition.contains("metadata")) {
      return config.getContentConfig().isIncludeMetadata();
    }
    if (condition.contains("behavioral")) {
      return config.getContentConfig().isIncludeBehavioralAnalysis();
    }
    if (condition.contains("performance")) {
      return config.getContentConfig().isIncludePerformanceAnalysis();
    }
    if (condition.contains("coverage")) {
      return config.getContentConfig().isIncludeCoverageAnalysis();
    }
    if (condition.contains("recommendations")) {
      return config.getContentConfig().isIncludeRecommendations();
    }

    return true;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final TemplateComponent that = (TemplateComponent) obj;
    return conditional == that.conditional
        && Objects.equals(componentId, that.componentId)
        && Objects.equals(templateContent, that.templateContent)
        && Objects.equals(componentData, that.componentData)
        && Objects.equals(conditionExpression, that.conditionExpression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentId, templateContent, componentData, conditional, conditionExpression);
  }

  @Override
  public String toString() {
    return "TemplateComponent{"
        + "id='"
        + componentId
        + '\''
        + ", conditional="
        + conditional
        + ", dataSize="
        + componentData.size()
        + '}';
  }

  // Static factory methods for creating standard template components

  public static TemplateComponent createHeader(final String templateName, final String templateContent) {
    return new TemplateComponent("header", templateContent, Map.of("templateName", templateName), false, null);
  }

  public static TemplateComponent createExecutiveSummary(final String templateName, final String templateContent) {
    return new TemplateComponent("executive-summary", templateContent, Map.of("templateName", templateName), false, null);
  }

  public static TemplateComponent createSummary(final String templateName, final String templateContent) {
    return new TemplateComponent("summary", templateContent, Map.of("templateName", templateName), false, null);
  }

  public static TemplateComponent createMetadata(final String templateName, final String templateContent) {
    return new TemplateComponent("metadata", templateContent, Map.of("templateName", templateName), false, null);
  }

  public static TemplateComponent createBehavioralSection(final String templateName, final String templateContent) {
    return new TemplateComponent("behavioral", templateContent, Map.of("templateName", templateName), true, "behavioral");
  }

  public static TemplateComponent createPerformanceSection(final String templateName, final String templateContent) {
    return new TemplateComponent("performance", templateContent, Map.of("templateName", templateName), true, "performance");
  }

  public static TemplateComponent createCoverageSection(final String templateName, final String templateContent) {
    return new TemplateComponent("coverage", templateContent, Map.of("templateName", templateName), true, "coverage");
  }

  public static TemplateComponent createInsights(final String templateName, final String templateContent) {
    return new TemplateComponent("insights", templateContent, Map.of("templateName", templateName), false, null);
  }

  public static TemplateComponent createRecommendations(final String templateName, final String templateContent) {
    return new TemplateComponent("recommendations", templateContent, Map.of("templateName", templateName), true, "recommendations");
  }

  public static TemplateComponent createAppendix(final String templateName, final String templateContent) {
    return new TemplateComponent("appendix", templateContent, Map.of("templateName", templateName), false, null);
  }

  public static TemplateComponent createFooter(final String templateName, final String templateContent) {
    return new TemplateComponent("footer", templateContent, Map.of("templateName", templateName), false, null);
  }
}