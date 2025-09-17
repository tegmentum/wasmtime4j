package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Individual template component representing a reusable report section.
 *
 * @since 1.0.0
 */
public final class TemplateComponent {

  /**
   * Loads a template from the classpath resources.
   *
   * @param templatePath the path to the template file
   * @return the template content as a string
   * @throws RuntimeException if the template cannot be loaded
   */
  private static String loadTemplate(final String templatePath) {
    try (final var inputStream = TemplateComponent.class.getResourceAsStream(templatePath)) {
      if (inputStream == null) {
        throw new RuntimeException("Template not found: " + templatePath);
      }
      return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    } catch (final java.io.IOException e) {
      throw new RuntimeException("Failed to load template: " + templatePath, e);
    }
  }

  private final String componentId;
  private final String componentName;
  private final ComponentType componentType;
  private final String templateContent;
  private final Map<String, Object> componentData;
  private final ComponentConfiguration configuration;
  private final List<TemplateComponent> childComponents;
  private final boolean required;

  private TemplateComponent(final Builder builder) {
    this.componentId = Objects.requireNonNull(builder.componentId, "componentId cannot be null");
    this.componentName =
        Objects.requireNonNull(builder.componentName, "componentName cannot be null");
    this.componentType =
        Objects.requireNonNull(builder.componentType, "componentType cannot be null");
    this.templateContent =
        Objects.requireNonNull(builder.templateContent, "templateContent cannot be null");
    this.componentData = Map.copyOf(builder.componentData);
    this.configuration =
        Objects.requireNonNull(builder.configuration, "configuration cannot be null");
    this.childComponents = List.copyOf(builder.childComponents);
    this.required = builder.required;
  }

  public String getComponentId() {
    return componentId;
  }

  public String getComponentName() {
    return componentName;
  }

  public ComponentType getComponentType() {
    return componentType;
  }

  public String getTemplateContent() {
    return templateContent;
  }

  public Map<String, Object> getComponentData() {
    return componentData;
  }

  public ComponentConfiguration getConfiguration() {
    return configuration;
  }

  public List<TemplateComponent> getChildComponents() {
    return childComponents;
  }

  public boolean isRequired() {
    return required;
  }

  /**
   * Checks if this component is compatible with the given report configuration.
   *
   * @param reportConfig the report configuration
   * @return true if compatible
   */
  public boolean isCompatibleWith(final ReportConfiguration reportConfig) {
    final ReportConfiguration.ContentConfiguration contentConfig = reportConfig.getContentConfig();

    return switch (componentType) {
      case SUMMARY -> contentConfig.isIncludeSummary();
      case METADATA -> contentConfig.isIncludeMetadata();
      case BEHAVIORAL -> contentConfig.isIncludeBehavioralAnalysis();
      case PERFORMANCE -> contentConfig.isIncludePerformanceAnalysis();
      case COVERAGE -> contentConfig.isIncludeCoverageAnalysis();
      case RECOMMENDATIONS -> contentConfig.isIncludeRecommendations();
      case HEADER,
          FOOTER,
          TITLE_PAGE,
          TABLE_OF_CONTENTS,
          EXECUTIVE_SUMMARY,
          INSIGHTS,
          APPENDIX,
          CUSTOM -> true; // Always compatible
    };
  }

  /** Creates a header component. */
  public static TemplateComponent createHeader(final String id, final String name) {
    return new Builder(id, name, ComponentType.HEADER)
        .templateContent(
            """
            <header class="report-header">
              <h1>${reportTitle}</h1>
              <div class="report-info">
                <span class="report-date">${reportDate}</span>
                <span class="report-version">${reportVersion}</span>
              </div>
            </header>
            """)
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
        .build();
  }

  /** Creates a summary component. */
  public static TemplateComponent createSummary(final String id, final String name) {
    return new Builder(id, name, ComponentType.SUMMARY)
        .templateContent(
            """
            <section class="summary">
              <h2>Summary</h2>
              <div class="summary-stats">
                <div class="stat">
                  <span class="stat-value">${summary.totalTests}</span>
                  <span class="stat-label">Total Tests</span>
                </div>
                <div class="stat">
                  <span class="stat-value">${summary.successRate?string.percent}</span>
                  <span class="stat-label">Success Rate</span>
                </div>
                <div class="stat">
                  <span class="stat-value">${summary.compatibilityScore?string("0.00")}</span>
                  <span class="stat-label">Compatibility Score</span>
                </div>
              </div>
              <div class="summary-status status-${summary.status?lower_case}">
                <span class="status-icon"></span>
                <span class="status-text">${summary.status.description}</span>
              </div>
            </section>
            """)
        .configuration(ComponentConfiguration.defaultConfig())
        .required(true)
        .build();
  }

  /** Creates a metadata component. */
  public static TemplateComponent createMetadata(final String id, final String name) {
    return new Builder(id, name, ComponentType.METADATA)
        .templateContent(
            """
            <section class="metadata">
              <h2>Report Metadata</h2>
              <table class="metadata-table">
                <tr><td>Version</td><td>${metadata.version}</td></tr>
                <tr><td>Execution Start</td><td>${metadata.executionStart?datetime}</td></tr>
                <tr><td>Execution End</td><td>${metadata.executionEnd?datetime}</td></tr>
                <tr><td>Duration</td><td>${metadata.executionDurationMillis} ms</td></tr>
                <tr><td>Targets</td><td><#list metadata.targets as target>${target}<#sep>, </#list></td></tr>
              </table>
            </section>
            """)
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
        .build();
  }

  /** Creates a behavioral analysis component. */
  public static TemplateComponent createBehavioralSection(final String id, final String name) {
    return new Builder(id, name, ComponentType.BEHAVIORAL)
        .templateContent(loadTemplate("/templates/behavioral-analysis.ftl"))
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
        .build();
  }

  /** Creates a performance analysis component. */
  public static TemplateComponent createPerformanceSection(final String id, final String name) {
    return new Builder(id, name, ComponentType.PERFORMANCE)
        .templateContent(
            """
            <section class="performance-analysis">
              <h2>Performance Analysis</h2>
              <#if testsWithPerformanceIssues?has_content>
                <div class="performance-issues">
                  <h3>Tests with Performance Issues</h3>
                  <#list testsWithPerformanceIssues as testName, testResult>
                    <div class="test-issue">
                      <h4>${testName}</h4>
                      <#if testResult.performanceResults.present>
                        <div class="performance-metrics">
                          <#list testResult.performanceResults.get().significantDifferences as diff>
                            <div class="performance-diff significance-${diff.significance?lower_case}">
                              <span class="metric-name">${diff.metricName}</span>
                              <span class="difference-value">${diff.differencePercentage?string("0.0")}%</span>
                            </div>
                          </#list>
                        </div>
                      </#if>
                    </div>
                  </#list>
                </div>
              <#else>
                <div class="no-issues">
                  <p>No significant performance issues detected.</p>
                </div>
              </#if>
            </section>
            """)
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
        .build();
  }

  /** Creates a coverage analysis component. */
  public static TemplateComponent createCoverageSection(final String id, final String name) {
    return new Builder(id, name, ComponentType.COVERAGE)
        .templateContent(
            """
            <section class="coverage-analysis">
              <h2>Coverage Analysis</h2>
              <#if testsWithCoverageGaps?has_content>
                <div class="coverage-gaps">
                  <h3>Tests with Coverage Gaps</h3>
                  <#list testsWithCoverageGaps as testName, testResult>
                    <div class="test-issue">
                      <h4>${testName}</h4>
                      <#if testResult.coverageResults.present>
                        <div class="coverage-metrics">
                          <div class="gap-count">
                            Coverage Gaps: ${testResult.coverageResults.get().gapCount}
                          </div>
                        </div>
                      </#if>
                    </div>
                  </#list>
                </div>
              <#else>
                <div class="no-issues">
                  <p>No significant coverage gaps detected.</p>
                </div>
              </#if>
            </section>
            """)
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
        .build();
  }

  /** Creates a recommendations component. */
  public static TemplateComponent createRecommendations(final String id, final String name) {
    return new Builder(id, name, ComponentType.RECOMMENDATIONS)
        .templateContent(
            """
            <section class="recommendations">
              <h2>Recommendations</h2>
              <#if globalRecommendations.present>
                <div class="global-recommendations">
                  <#list globalRecommendations.get().prioritizedRecommendations as recommendation>
                    <div class="recommendation priority-${recommendation.severity?lower_case}">
                      <h3>${recommendation.title}</h3>
                      <div class="recommendation-meta">
                        <span class="priority">Priority: ${recommendation.severity}</span>
                        <span class="category">Category: ${recommendation.category}</span>
                        <span class="score">Score: ${recommendation.priorityScore?string("0.00")}</span>
                      </div>
                      <p class="description">${recommendation.description}</p>
                      <#if recommendation.implementationSteps?has_content>
                        <div class="implementation-steps">
                          <h4>Implementation Steps:</h4>
                          <ol>
                            <#list recommendation.implementationSteps as step>
                              <li>${step}</li>
                            </#list>
                          </ol>
                        </div>
                      </#if>
                      <#if recommendation.affectedRuntimes?has_content>
                        <div class="affected-runtimes">
                          <strong>Affected Runtimes:</strong>
                          <#list recommendation.affectedRuntimes as runtime>
                            <span class="runtime">${runtime}</span><#sep>, </#sep>
                          </#list>
                        </div>
                      </#if>
                    </div>
                  </#list>
                </div>
              <#else>
                <div class="no-recommendations">
                  <p>No recommendations available at this time.</p>
                </div>
              </#if>
            </section>
            """)
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
        .build();
  }

  /** Creates a footer component. */
  public static TemplateComponent createFooter(final String id, final String name) {
    return new Builder(id, name, ComponentType.FOOTER)
        .templateContent(
            """
            <footer class="report-footer">
              <div class="footer-content">
                <div class="generated-info">
                  Generated on ${generationTime?datetime} by Wasmtime4j Comparison Suite
                </div>
                <div class="footer-links">
                  <a href="#top">Back to Top</a>
                </div>
              </div>
            </footer>
            """)
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
        .build();
  }

  /** Creates additional template components for comprehensive reports. */
  public static TemplateComponent createTitlePage(final String id, final String name) {
    return new Builder(id, name, ComponentType.TITLE_PAGE)
        .templateContent(
            """
            <div class="title-page">
              <h1 class="report-title">${reportTitle}</h1>
              <h2 class="report-subtitle">${testSuiteName} Comparison Analysis</h2>
              <div class="title-metadata">
                <p><strong>Report ID:</strong> ${reportId}</p>
                <p><strong>Generated:</strong> ${generationTime?datetime}</p>
                <p><strong>Version:</strong> ${metadata.version}</p>
              </div>
            </div>
            """)
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
        .build();
  }

  /** Creates a table of contents component. */
  public static TemplateComponent createTableOfContents(final String id, final String name) {
    return new Builder(id, name, ComponentType.TABLE_OF_CONTENTS)
        .templateContent(
            """
            <div class="table-of-contents">
              <h2>Table of Contents</h2>
              <ul class="toc-list">
                <li><a href="#summary">Summary</a></li>
                <li><a href="#behavioral-analysis">Behavioral Analysis</a></li>
                <li><a href="#performance-analysis">Performance Analysis</a></li>
                <li><a href="#coverage-analysis">Coverage Analysis</a></li>
                <li><a href="#recommendations">Recommendations</a></li>
                <li><a href="#metadata">Metadata</a></li>
              </ul>
            </div>
            """)
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
        .build();
  }

  /** Creates an executive summary component. */
  public static TemplateComponent createExecutiveSummary(final String id, final String name) {
    return new Builder(id, name, ComponentType.EXECUTIVE_SUMMARY)
        .templateContent(loadTemplate("/templates/executive-summary.ftl"))
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
        .build();
  }

  /** Creates an insights component. */
  public static TemplateComponent createInsights(final String id, final String name) {
    return new Builder(id, name, ComponentType.INSIGHTS)
        .templateContent(
            """
            <section class="insights">
              <h2>Key Insights</h2>
              <div class="insights-content">
                <!-- Insights will be populated dynamically based on analysis results -->
                <p>Detailed insights and patterns discovered during analysis.</p>
              </div>
            </section>
            """)
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
        .build();
  }

  /** Creates an appendix component. */
  public static TemplateComponent createAppendix(final String id, final String name) {
    return new Builder(id, name, ComponentType.APPENDIX)
        .templateContent(
            """
            <section class="appendix">
              <h2>Appendix</h2>
              <div class="appendix-content">
                <h3>Raw Data</h3>
                <p>Additional data and detailed logs are available upon request.</p>
              </div>
            </section>
            """)
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
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

    final TemplateComponent that = (TemplateComponent) obj;
    return required == that.required
        && Objects.equals(componentId, that.componentId)
        && Objects.equals(componentName, that.componentName)
        && componentType == that.componentType
        && Objects.equals(templateContent, that.templateContent)
        && Objects.equals(componentData, that.componentData)
        && Objects.equals(configuration, that.configuration)
        && Objects.equals(childComponents, that.childComponents);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        componentId,
        componentName,
        componentType,
        templateContent,
        componentData,
        configuration,
        childComponents,
        required);
  }

  @Override
  public String toString() {
    return "TemplateComponent{"
        + "id='"
        + componentId
        + '\''
        + ", name='"
        + componentName
        + '\''
        + ", type="
        + componentType
        + ", required="
        + required
        + '}';
  }

  /** Builder for TemplateComponent. */
  public static final class Builder {
    private final String componentId;
    private final String componentName;
    private final ComponentType componentType;
    private String templateContent = "";
    private Map<String, Object> componentData = Collections.emptyMap();
    private ComponentConfiguration configuration = ComponentConfiguration.defaultConfig();
    private List<TemplateComponent> childComponents = Collections.emptyList();
    private boolean required = false;

    /**
     * Constructs a new Builder.
     *
     * @param componentId the component ID
     * @param componentName the component name
     * @param componentType the component type
     */
    public Builder(
        final String componentId, final String componentName, final ComponentType componentType) {
      this.componentId = Objects.requireNonNull(componentId, "componentId cannot be null");
      this.componentName = Objects.requireNonNull(componentName, "componentName cannot be null");
      this.componentType = Objects.requireNonNull(componentType, "componentType cannot be null");
    }

    /**
     * Sets the template content.
     *
     * @param templateContent the template content
     * @return this builder
     */
    public Builder templateContent(final String templateContent) {
      this.templateContent =
          Objects.requireNonNull(templateContent, "templateContent cannot be null");
      return this;
    }

    /**
     * Sets the component data.
     *
     * @param componentData the component data
     * @return this builder
     */
    public Builder componentData(final Map<String, Object> componentData) {
      this.componentData = Objects.requireNonNull(componentData, "componentData cannot be null");
      return this;
    }

    /**
     * Sets the component configuration.
     *
     * @param configuration the component configuration
     * @return this builder
     */
    public Builder configuration(final ComponentConfiguration configuration) {
      this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
      return this;
    }

    /**
     * Sets the child components.
     *
     * @param childComponents the child components
     * @return this builder
     */
    public Builder childComponents(final List<TemplateComponent> childComponents) {
      this.childComponents =
          Objects.requireNonNull(childComponents, "childComponents cannot be null");
      return this;
    }

    public Builder required(final boolean required) {
      this.required = required;
      return this;
    }

    public TemplateComponent build() {
      return new TemplateComponent(this);
    }
  }
}
