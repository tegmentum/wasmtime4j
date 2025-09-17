package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

/** Individual template component representing a reusable report section. */
final class TemplateComponent {
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
        .templateContent(
            """
            <section class="behavioral-analysis">
              <h2>Behavioral Analysis</h2>
              <#if testsWithBehavioralIssues?has_content>
                <div class="behavioral-issues">
                  <h3>Tests with Behavioral Issues</h3>
                  <#list testsWithBehavioralIssues as testName, testResult>
                    <div class="test-issue">
                      <h4>${testName}</h4>
                      <#if testResult.behavioralResults.present>
                        <div class="behavioral-verdict verdict-\
${testResult.behavioralResults.get().verdict?lower_case}">
                          ${testResult.behavioralResults.get().verdict.description}
                        </div>
                        <div class="consistency-score">
                          Consistency Score: ${testResult.behavioralResults.get().consistencyScore?string("0.00")}
                        </div>
                        <#if testResult.behavioralResults.get().discrepancies?has_content>
                          <div class="discrepancies">
                            <h5>Discrepancies</h5>
                            <#list testResult.behavioralResults.get().discrepancies as discrepancy>
                              <div class="discrepancy severity-${discrepancy.severity?lower_case}">
                                <span class="discrepancy-type">${discrepancy.discrepancyType}</span>
                                <span class="discrepancy-description">${discrepancy.description}</span>
                              </div>
                            </#list>
                          </div>
                        </#if>
                      </#if>
                    </div>
                  </#list>
                </div>
              <#else>
                <div class="no-issues">
                  <p>No behavioral issues detected in any tests.</p>
                </div>
              </#if>
            </section>
            """)
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

  public static TemplateComponent createExecutiveSummary(final String id, final String name) {
    return new Builder(id, name, ComponentType.EXECUTIVE_SUMMARY)
        .templateContent(
            """
            <section class="executive-summary">
              <h2>Executive Summary</h2>
              <div class="executive-content">
                <p>This report presents the results of comprehensive compatibility testing between
                WebAssembly runtime implementations. The analysis covers ${summary.totalTests} tests
                with an overall compatibility score of ${summary.compatibilityScore?string("0.00")}.</p>

                <#if summary.testsWithIssues gt 0>
                  <div class="executive-concerns">
                    <h3>Key Concerns</h3>
                    <ul>
                      <#if summary.testsWithBehavioralIssues gt 0>
                        <li>${summary.testsWithBehavioralIssues} tests showed behavioral compatibility issues</li>
                      </#if>
                      <#if summary.testsWithPerformanceIssues gt 0>
                        <li>${summary.testsWithPerformanceIssues} tests showed significant performance differences</li>
                      </#if>
                      <#if summary.testsWithCoverageGaps gt 0>
                        <li>${summary.testsWithCoverageGaps} tests revealed coverage gaps</li>
                      </#if>
                    </ul>
                  </div>
                </#if>

                <#if summary.highPriorityRecommendations gt 0>
                  <div class="executive-actions">
                    <h3>Immediate Actions Required</h3>
                    <p>${summary.highPriorityRecommendations} high-priority recommendations \
require immediate attention.</p>
                  </div>
                </#if>
              </div>
            </section>
            """)
        .configuration(ComponentConfiguration.defaultConfig())
        .required(false)
        .build();
  }

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

    public Builder(
        final String componentId, final String componentName, final ComponentType componentType) {
      this.componentId = Objects.requireNonNull(componentId, "componentId cannot be null");
      this.componentName = Objects.requireNonNull(componentName, "componentName cannot be null");
      this.componentType = Objects.requireNonNull(componentType, "componentType cannot be null");
    }

    public Builder templateContent(final String templateContent) {
      this.templateContent =
          Objects.requireNonNull(templateContent, "templateContent cannot be null");
      return this;
    }

    public Builder componentData(final Map<String, Object> componentData) {
      this.componentData = Objects.requireNonNull(componentData, "componentData cannot be null");
      return this;
    }

    public Builder configuration(final ComponentConfiguration configuration) {
      this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
      return this;
    }

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


/** Metadata about a template. */
final class TemplateMetadata {
  private final String version;
  private final String author;
  private final String description;
  private final List<String> supportedFormats;
  private final Map<String, String> customMetadata;

  private TemplateMetadata(final Builder builder) {
    this.version = Objects.requireNonNull(builder.version, "version cannot be null");
    this.author = Objects.requireNonNull(builder.author, "author cannot be null");
    this.description = Objects.requireNonNull(builder.description, "description cannot be null");
    this.supportedFormats = List.copyOf(builder.supportedFormats);
    this.customMetadata = Map.copyOf(builder.customMetadata);
  }

  public String getVersion() {
    return version;
  }

  public String getAuthor() {
    return author;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getSupportedFormats() {
    return supportedFormats;
  }

  public Map<String, String> getCustomMetadata() {
    return customMetadata;
  }

  /** Creates default template metadata. */
  public static TemplateMetadata defaultMetadata() {
    return new Builder()
        .version("1.0.0")
        .author("Wasmtime4j Comparison Suite")
        .description("Default template for comparison reports")
        .supportedFormats(List.of("HTML", "JSON", "CSV", "CONSOLE"))
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

    final TemplateMetadata that = (TemplateMetadata) obj;
    return Objects.equals(version, that.version)
        && Objects.equals(author, that.author)
        && Objects.equals(description, that.description)
        && Objects.equals(supportedFormats, that.supportedFormats)
        && Objects.equals(customMetadata, that.customMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, author, description, supportedFormats, customMetadata);
  }

  @Override
  public String toString() {
    return "TemplateMetadata{"
        + "version='"
        + version
        + '\''
        + ", author='"
        + author
        + '\''
        + ", formats="
        + supportedFormats
        + '}';
  }

  /** Builder for TemplateMetadata. */
  public static final class Builder {
    private String version = "1.0.0";
    private String author = "Unknown";
    private String description = "";
    private List<String> supportedFormats = Collections.emptyList();
    private Map<String, String> customMetadata = Collections.emptyMap();

    public Builder version(final String version) {
      this.version = Objects.requireNonNull(version, "version cannot be null");
      return this;
    }

    public Builder author(final String author) {
      this.author = Objects.requireNonNull(author, "author cannot be null");
      return this;
    }

    public Builder description(final String description) {
      this.description = Objects.requireNonNull(description, "description cannot be null");
      return this;
    }

    public Builder supportedFormats(final List<String> supportedFormats) {
      this.supportedFormats =
          Objects.requireNonNull(supportedFormats, "supportedFormats cannot be null");
      return this;
    }

    public Builder customMetadata(final Map<String, String> customMetadata) {
      this.customMetadata = Objects.requireNonNull(customMetadata, "customMetadata cannot be null");
      return this;
    }

    public TemplateMetadata build() {
      return new TemplateMetadata(this);
    }
  }
}

/** Template validation result. */
final class TemplateValidationResult {
  private final boolean valid;
  private final List<String> errors;
  private final List<String> warnings;

  public TemplateValidationResult(
      final boolean valid, final List<String> errors, final List<String> warnings) {
    this.valid = valid;
    this.errors = List.copyOf(errors);
    this.warnings = List.copyOf(warnings);
  }

  public boolean isValid() {
    return valid;
  }

  public List<String> getErrors() {
    return errors;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public boolean hasIssues() {
    return !errors.isEmpty() || !warnings.isEmpty();
  }

  @Override
  public String toString() {
    return "TemplateValidationResult{"
        + "valid="
        + valid
        + ", errors="
        + errors.size()
        + ", warnings="
        + warnings.size()
        + '}';
  }
}

/** Validator for template structure and content. */
final class TemplateValidator {
  public TemplateValidationResult validate(final ReportTemplate template) {
    final List<String> errors = new java.util.ArrayList<>();
    final List<String> warnings = new java.util.ArrayList<>();

    // Validate template structure
    if (template.getComponents().isEmpty()) {
      warnings.add("Template has no components");
    }

    // Validate component IDs are unique
    final Set<String> componentIds = new java.util.HashSet<>();
    for (final TemplateComponent component : template.getComponents()) {
      if (!componentIds.add(component.getComponentId())) {
        errors.add("Duplicate component ID: " + component.getComponentId());
      }
    }

    // Validate required components
    final boolean hasSummary =
        template.getComponents().stream()
            .anyMatch(component -> component.getComponentType() == ComponentType.SUMMARY);
    if (!hasSummary) {
      warnings.add("Template should include a summary component");
    }

    // Validate template content syntax (basic check)
    for (final TemplateComponent component : template.getComponents()) {
      final String content = component.getTemplateContent();
      if (content.contains("${") && !content.contains("}")) {
        errors.add("Malformed template syntax in component: " + component.getComponentId());
      }
    }

    final boolean isValid = errors.isEmpty();
    return new TemplateValidationResult(isValid, errors, warnings);
  }
}
