package ai.tegmentum.wasmtime4j.performance.insights;

import ai.tegmentum.wasmtime4j.performance.ExportFormat;
import ai.tegmentum.wasmtime4j.performance.insights.PerformanceInsightsEngine.EstimatedImpact;
import ai.tegmentum.wasmtime4j.performance.insights.PerformanceInsightsEngine.IssueCategory;
import ai.tegmentum.wasmtime4j.performance.insights.PerformanceInsightsEngine.IssueSeverity;
import ai.tegmentum.wasmtime4j.performance.insights.PerformanceInsightsEngine.RecommendationPriority;
import ai.tegmentum.wasmtime4j.performance.insights.PerformanceInsightsEngine.RecommendationType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprehensive performance insights containing issues, recommendations, and optimization
 * opportunities.
 *
 * <p>This class represents the complete analysis result from the PerformanceInsightsEngine,
 * providing structured access to performance findings and actionable guidance.
 *
 * @since 1.0.0
 */
public final class PerformanceInsights {
  private final Instant generatedAt;
  private final List<PerformanceIssue> issues;
  private final List<PerformanceRecommendation> recommendations;
  private final List<OptimizationOpportunity> opportunities;
  private final PerformanceSummary summary;
  private final Map<String, Integer> dataSources;

  /**
   * Creates a new PerformanceInsights instance.
   *
   * @param generatedAt the timestamp when these insights were generated
   * @param issues the list of performance issues
   * @param recommendations the list of performance recommendations
   * @param opportunities the list of optimization opportunities
   * @param summary the performance summary
   * @param dataSources the data sources used for analysis
   */
  public PerformanceInsights(
      final Instant generatedAt,
      final List<PerformanceIssue> issues,
      final List<PerformanceRecommendation> recommendations,
      final List<OptimizationOpportunity> opportunities,
      final PerformanceSummary summary,
      final Map<String, Integer> dataSources) {
    this.generatedAt = generatedAt;
    this.issues = List.copyOf(issues);
    this.recommendations = List.copyOf(recommendations);
    this.opportunities = List.copyOf(opportunities);
    this.summary = summary;
    this.dataSources = Map.copyOf(dataSources);
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public List<PerformanceIssue> getIssues() {
    return issues;
  }

  public List<PerformanceRecommendation> getRecommendations() {
    return recommendations;
  }

  public List<OptimizationOpportunity> getOpportunities() {
    return opportunities;
  }

  public PerformanceSummary getSummary() {
    return summary;
  }

  public Map<String, Integer> getDataSources() {
    return dataSources;
  }

  /** Gets issues filtered by severity. */
  public List<PerformanceIssue> getIssuesBySeverity(final IssueSeverity severity) {
    return issues.stream()
        .filter(issue -> issue.getSeverity() == severity)
        .collect(Collectors.toList());
  }

  /** Gets recommendations filtered by priority. */
  public List<PerformanceRecommendation> getRecommendationsByPriority(
      final RecommendationPriority priority) {
    return recommendations.stream()
        .filter(rec -> rec.getPriority() == priority)
        .collect(Collectors.toList());
  }

  /** Gets high-priority actionable items. */
  public List<String> getHighPriorityActions() {
    return recommendations.stream()
        .filter(
            rec ->
                rec.getPriority() == RecommendationPriority.HIGH
                    || rec.getPriority() == RecommendationPriority.CRITICAL)
        .flatMap(rec -> rec.getActionItems().stream())
        .collect(Collectors.toList());
  }

  /** Exports insights in the specified format. */
  public String export(final ExportFormat format) {
    switch (format) {
      case JSON:
        return exportAsJson();
      case CSV:
        return exportAsCsv();
      default:
        throw new IllegalArgumentException("Unsupported format: " + format);
    }
  }

  private String exportAsJson() {
    final StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"generatedAt\": \"").append(generatedAt).append("\",\n");
    json.append("  \"summary\": ").append(escapeJson(summary.getText())).append(",\n");
    json.append("  \"issues\": [\n");

    for (int i = 0; i < issues.size(); i++) {
      json.append("    ").append(issues.get(i).toJson());
      if (i < issues.size() - 1) {
        json.append(",");
      }
      json.append("\n");
    }

    json.append("  ],\n");
    json.append("  \"recommendations\": [\n");

    for (int i = 0; i < recommendations.size(); i++) {
      json.append("    ").append(recommendations.get(i).toJson());
      if (i < recommendations.size() - 1) {
        json.append(",");
      }
      json.append("\n");
    }

    json.append("  ],\n");
    json.append("  \"opportunities\": [\n");

    for (int i = 0; i < opportunities.size(); i++) {
      json.append("    ").append(opportunities.get(i).toJson());
      if (i < opportunities.size() - 1) {
        json.append(",");
      }
      json.append("\n");
    }

    json.append("  ]\n");
    json.append("}");
    return json.toString();
  }

  private String exportAsCsv() {
    final StringBuilder csv = new StringBuilder();
    csv.append("Type,Severity/Priority,Category/Type,Title,Description,Source\n");

    for (final PerformanceIssue issue : issues) {
      csv.append("Issue,")
          .append(issue.getSeverity())
          .append(",")
          .append(issue.getCategory())
          .append(",")
          .append(escapeCSV(issue.getTitle()))
          .append(",")
          .append(escapeCSV(issue.getDescription()))
          .append(",")
          .append(escapeCSV(issue.getSource()))
          .append("\n");
    }

    for (final PerformanceRecommendation rec : recommendations) {
      csv.append("Recommendation,")
          .append(rec.getPriority())
          .append(",")
          .append(rec.getType())
          .append(",")
          .append(escapeCSV(rec.getTitle()))
          .append(",")
          .append(escapeCSV(rec.getDescription()))
          .append(",")
          .append("Analysis\n");
    }

    return csv.toString();
  }

  private String escapeJson(final String text) {
    return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
  }

  private String escapeCSV(final String text) {
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }

  /** Represents a performance issue identified during analysis. */
  static final class PerformanceIssue {
    private final IssueSeverity severity;
    private final IssueCategory category;
    private final String title;
    private final String description;
    private final String source;

    public PerformanceIssue(
        final IssueSeverity severity,
        final IssueCategory category,
        final String title,
        final String description,
        final String source) {
      this.severity = severity;
      this.category = category;
      this.title = title;
      this.description = description;
      this.source = source;
    }

    public IssueSeverity getSeverity() {
      return severity;
    }

    public IssueCategory getCategory() {
      return category;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public String getSource() {
      return source;
    }

    public String toJson() {
      return String.format(
          "{\"severity\": \"%s\", \"category\": \"%s\", \"title\": \"%s\", \"description\": \"%s\","
              + " \"source\": \"%s\"}",
          severity, category, title, description, source);
    }
  }

  /** Represents a performance optimization recommendation. */
  static final class PerformanceRecommendation {
    private final RecommendationPriority priority;
    private final RecommendationType type;
    private final String title;
    private final String description;
    private final List<String> actionItems;
    private final EstimatedImpact estimatedImpact;

    public PerformanceRecommendation(
        final RecommendationPriority priority,
        final RecommendationType type,
        final String title,
        final String description,
        final List<String> actionItems,
        final EstimatedImpact estimatedImpact) {
      this.priority = priority;
      this.type = type;
      this.title = title;
      this.description = description;
      this.actionItems = List.copyOf(actionItems);
      this.estimatedImpact = estimatedImpact;
    }

    public RecommendationPriority getPriority() {
      return priority;
    }

    public RecommendationType getType() {
      return type;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public List<String> getActionItems() {
      return actionItems;
    }

    public EstimatedImpact getEstimatedImpact() {
      return estimatedImpact;
    }

    public String toJson() {
      final String actionsJson =
          actionItems.stream()
              .map(action -> "\"" + action + "\"")
              .collect(Collectors.joining(", "));

      return String.format(
          "{\"priority\": \"%s\", \"type\": \"%s\", \"title\": \"%s\", \"description\": \"%s\","
              + " \"actionItems\": [%s], \"estimatedImpact\": \"%s\"}",
          priority, type, title, description, actionsJson, estimatedImpact);
    }
  }

  /** Represents an optimization opportunity. */
  static final class OptimizationOpportunity {
    private final String title;
    private final String description;
    private final EstimatedImpact impact;
    private final List<String> suggestedActions;

    public OptimizationOpportunity(
        final String title,
        final String description,
        final EstimatedImpact impact,
        final List<String> suggestedActions) {
      this.title = title;
      this.description = description;
      this.impact = impact;
      this.suggestedActions = List.copyOf(suggestedActions);
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public EstimatedImpact getImpact() {
      return impact;
    }

    public List<String> getSuggestedActions() {
      return suggestedActions;
    }

    public String toJson() {
      final String actionsJson =
          suggestedActions.stream()
              .map(action -> "\"" + action + "\"")
              .collect(Collectors.joining(", "));

      return String.format(
          "{\"title\": \"%s\", \"description\": \"%s\", \"impact\": \"%s\", \"suggestedActions\":"
              + " [%s]}",
          title, description, impact, actionsJson);
    }
  }

  /** Performance analysis summary. */
  static final class PerformanceSummary {
    private final String text;

    public PerformanceSummary(final String text) {
      this.text = text;
    }

    public String getText() {
      return text;
    }
  }
}
