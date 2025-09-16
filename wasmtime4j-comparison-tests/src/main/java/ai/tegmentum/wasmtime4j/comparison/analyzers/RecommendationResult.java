package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Comprehensive result of recommendation analysis containing actionable insights categorized by
 * type and prioritized by impact and urgency.
 *
 * @since 1.0.0
 */
public final class RecommendationResult {
  private final String testName;
  private final List<ActionableRecommendation> behavioralRecommendations;
  private final List<ActionableRecommendation> performanceRecommendations;
  private final List<ActionableRecommendation> coverageRecommendations;
  private final List<ActionableRecommendation> integrationRecommendations;
  private final List<ActionableRecommendation> prioritizedRecommendations;
  private final RecommendationSummary summary;
  private final Instant analysisTime;

  private RecommendationResult(final Builder builder) {
    this.testName = Objects.requireNonNull(builder.testName, "testName cannot be null");
    this.behavioralRecommendations = List.copyOf(builder.behavioralRecommendations);
    this.performanceRecommendations = List.copyOf(builder.performanceRecommendations);
    this.coverageRecommendations = List.copyOf(builder.coverageRecommendations);
    this.integrationRecommendations = List.copyOf(builder.integrationRecommendations);
    this.prioritizedRecommendations = List.copyOf(builder.prioritizedRecommendations);
    this.summary = Objects.requireNonNull(builder.summary, "summary cannot be null");
    this.analysisTime = Instant.now();
  }

  public String getTestName() {
    return testName;
  }

  public List<ActionableRecommendation> getBehavioralRecommendations() {
    return behavioralRecommendations;
  }

  public List<ActionableRecommendation> getPerformanceRecommendations() {
    return performanceRecommendations;
  }

  public List<ActionableRecommendation> getCoverageRecommendations() {
    return coverageRecommendations;
  }

  public List<ActionableRecommendation> getIntegrationRecommendations() {
    return integrationRecommendations;
  }

  public List<ActionableRecommendation> getPrioritizedRecommendations() {
    return prioritizedRecommendations;
  }

  public RecommendationSummary getSummary() {
    return summary;
  }

  public Instant getAnalysisTime() {
    return analysisTime;
  }

  /**
   * Gets high-priority recommendations that require immediate attention.
   *
   * @return list of high-priority recommendations
   */
  public List<ActionableRecommendation> getHighPriorityRecommendations() {
    return prioritizedRecommendations.stream()
        .filter(rec -> rec.getSeverity() == IssueSeverity.HIGH)
        .toList();
  }

  /**
   * Gets recommendations affecting a specific runtime.
   *
   * @param runtime the runtime type
   * @return list of recommendations affecting the runtime
   */
  public List<ActionableRecommendation> getRecommendationsForRuntime(final RuntimeType runtime) {
    return prioritizedRecommendations.stream()
        .filter(rec -> rec.getAffectedRuntimes().contains(runtime))
        .toList();
  }

  /**
   * Generates an executive summary of recommendations.
   *
   * @return formatted executive summary
   */
  public String getExecutiveSummary() {
    return String.format(
        "Recommendation Summary for %s%n"
            + "================================%n"
            + "Total Recommendations: %d%n"
            + "High Priority: %d%n"
            + "Medium Priority: %d%n"
            + "Low Priority: %d%n%n"
            + "By Category:%n"
            + "  Behavioral: %d%n"
            + "  Performance: %d%n"
            + "  Coverage: %d%n"
            + "  Integration: %d%n%n"
            + "Analysis Time: %s",
        testName,
        summary.getTotalRecommendations(),
        summary.getHighPriorityCount(),
        summary.getMediumPriorityCount(),
        summary.getLowPriorityCount(),
        behavioralRecommendations.size(),
        performanceRecommendations.size(),
        coverageRecommendations.size(),
        integrationRecommendations.size(),
        analysisTime);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final RecommendationResult that = (RecommendationResult) obj;
    return Objects.equals(testName, that.testName)
        && Objects.equals(behavioralRecommendations, that.behavioralRecommendations)
        && Objects.equals(performanceRecommendations, that.performanceRecommendations)
        && Objects.equals(coverageRecommendations, that.coverageRecommendations)
        && Objects.equals(integrationRecommendations, that.integrationRecommendations)
        && Objects.equals(prioritizedRecommendations, that.prioritizedRecommendations)
        && Objects.equals(summary, that.summary);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        testName,
        behavioralRecommendations,
        performanceRecommendations,
        coverageRecommendations,
        integrationRecommendations,
        prioritizedRecommendations,
        summary);
  }

  @Override
  public String toString() {
    return "RecommendationResult{"
        + "testName='"
        + testName
        + '\''
        + ", total="
        + summary.getTotalRecommendations()
        + ", highPriority="
        + summary.getHighPriorityCount()
        + '}';
  }

  /** Builder for RecommendationResult. */
  public static final class Builder {
    private final String testName;
    private List<ActionableRecommendation> behavioralRecommendations = Collections.emptyList();
    private List<ActionableRecommendation> performanceRecommendations = Collections.emptyList();
    private List<ActionableRecommendation> coverageRecommendations = Collections.emptyList();
    private List<ActionableRecommendation> integrationRecommendations = Collections.emptyList();
    private List<ActionableRecommendation> prioritizedRecommendations = Collections.emptyList();
    private RecommendationSummary summary;

    public Builder(final String testName) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    }

    public Builder behavioralRecommendations(final List<ActionableRecommendation> recommendations) {
      this.behavioralRecommendations =
          Objects.requireNonNull(recommendations, "recommendations cannot be null");
      return this;
    }

    public Builder performanceRecommendations(
        final List<ActionableRecommendation> recommendations) {
      this.performanceRecommendations =
          Objects.requireNonNull(recommendations, "recommendations cannot be null");
      return this;
    }

    public Builder coverageRecommendations(final List<ActionableRecommendation> recommendations) {
      this.coverageRecommendations =
          Objects.requireNonNull(recommendations, "recommendations cannot be null");
      return this;
    }

    public Builder integrationRecommendations(
        final List<ActionableRecommendation> recommendations) {
      this.integrationRecommendations =
          Objects.requireNonNull(recommendations, "recommendations cannot be null");
      return this;
    }

    public Builder prioritizedRecommendations(
        final List<ActionableRecommendation> recommendations) {
      this.prioritizedRecommendations =
          Objects.requireNonNull(recommendations, "recommendations cannot be null");
      return this;
    }

    public Builder summary(final RecommendationSummary summary) {
      this.summary = Objects.requireNonNull(summary, "summary cannot be null");
      return this;
    }

    public RecommendationResult build() {
      if (summary == null) {
        throw new IllegalStateException("summary must be set");
      }
      return new RecommendationResult(this);
    }
  }
}

/** Individual actionable recommendation with implementation guidance. */
final class ActionableRecommendation {
  private final String title;
  private final String description;
  private final List<String> implementationSteps;
  private final IssueCategory category;
  private final IssueSeverity severity;
  private final double priorityScore;
  private final Set<RuntimeType> affectedRuntimes;
  private final String issuePattern;

  public ActionableRecommendation(
      final String title,
      final String description,
      final List<String> implementationSteps,
      final IssueCategory category,
      final IssueSeverity severity,
      final double priorityScore,
      final Set<RuntimeType> affectedRuntimes,
      final String issuePattern) {
    this.title = Objects.requireNonNull(title, "title cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.implementationSteps = List.copyOf(implementationSteps);
    this.category = Objects.requireNonNull(category, "category cannot be null");
    this.severity = Objects.requireNonNull(severity, "severity cannot be null");
    this.priorityScore = priorityScore;
    this.affectedRuntimes = Set.copyOf(affectedRuntimes);
    this.issuePattern = Objects.requireNonNull(issuePattern, "issuePattern cannot be null");
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getImplementationSteps() {
    return implementationSteps;
  }

  public IssueCategory getCategory() {
    return category;
  }

  public IssueSeverity getSeverity() {
    return severity;
  }

  public double getPriorityScore() {
    return priorityScore;
  }

  public Set<RuntimeType> getAffectedRuntimes() {
    return affectedRuntimes;
  }

  public String getIssuePattern() {
    return issuePattern;
  }

  /**
   * Generates a formatted action plan for this recommendation.
   *
   * @return formatted action plan
   */
  public String getActionPlan() {
    final StringBuilder sb = new StringBuilder();
    sb.append(String.format("Recommendation: %s%n", title));
    sb.append(String.format("Priority: %s (Score: %.2f)%n", severity, priorityScore));
    sb.append(String.format("Category: %s%n", category));
    sb.append(String.format("Affected Runtimes: %s%n", affectedRuntimes));
    sb.append(String.format("Description: %s%n%n", description));
    sb.append("Implementation Steps:%n");
    for (int i = 0; i < implementationSteps.size(); i++) {
      sb.append(String.format("  %d. %s%n", i + 1, implementationSteps.get(i)));
    }
    return sb.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ActionableRecommendation that = (ActionableRecommendation) obj;
    return Double.compare(that.priorityScore, priorityScore) == 0
        && Objects.equals(title, that.title)
        && Objects.equals(description, that.description)
        && Objects.equals(implementationSteps, that.implementationSteps)
        && category == that.category
        && severity == that.severity
        && Objects.equals(affectedRuntimes, that.affectedRuntimes)
        && Objects.equals(issuePattern, that.issuePattern);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        title,
        description,
        implementationSteps,
        category,
        severity,
        priorityScore,
        affectedRuntimes,
        issuePattern);
  }

  @Override
  public String toString() {
    return "ActionableRecommendation{"
        + "title='"
        + title
        + '\''
        + ", category="
        + category
        + ", severity="
        + severity
        + ", score="
        + String.format("%.2f", priorityScore)
        + '}';
  }
}

/** Summary of recommendations with counts and breakdowns. */
final class RecommendationSummary {
  private final int totalRecommendations;
  private final int highPriorityCount;
  private final int mediumPriorityCount;
  private final int lowPriorityCount;
  private final Map<IssueCategory, Integer> categoryBreakdown;

  public RecommendationSummary(
      final int totalRecommendations,
      final int highPriorityCount,
      final int mediumPriorityCount,
      final int lowPriorityCount,
      final Map<IssueCategory, Integer> categoryBreakdown) {
    this.totalRecommendations = totalRecommendations;
    this.highPriorityCount = highPriorityCount;
    this.mediumPriorityCount = mediumPriorityCount;
    this.lowPriorityCount = lowPriorityCount;
    this.categoryBreakdown = Map.copyOf(categoryBreakdown);
  }

  public int getTotalRecommendations() {
    return totalRecommendations;
  }

  public int getHighPriorityCount() {
    return highPriorityCount;
  }

  public int getMediumPriorityCount() {
    return mediumPriorityCount;
  }

  public int getLowPriorityCount() {
    return lowPriorityCount;
  }

  public Map<IssueCategory, Integer> getCategoryBreakdown() {
    return categoryBreakdown;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final RecommendationSummary that = (RecommendationSummary) obj;
    return totalRecommendations == that.totalRecommendations
        && highPriorityCount == that.highPriorityCount
        && mediumPriorityCount == that.mediumPriorityCount
        && lowPriorityCount == that.lowPriorityCount
        && Objects.equals(categoryBreakdown, that.categoryBreakdown);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        totalRecommendations,
        highPriorityCount,
        mediumPriorityCount,
        lowPriorityCount,
        categoryBreakdown);
  }

  @Override
  public String toString() {
    return "RecommendationSummary{"
        + "total="
        + totalRecommendations
        + ", high="
        + highPriorityCount
        + ", medium="
        + mediumPriorityCount
        + ", low="
        + lowPriorityCount
        + '}';
  }
}

/** Categories of issues that can be identified and addressed. */
enum IssueCategory {
  /** Behavioral compatibility issues between runtimes. */
  BEHAVIORAL,

  /** Performance-related issues and optimization opportunities. */
  PERFORMANCE,

  /** Test coverage gaps and missing feature validation. */
  COVERAGE,

  /** Integration issues spanning multiple categories. */
  INTEGRATION
}

/** Severity levels for identified issues. */
enum IssueSeverity {
  /** Low severity issue that can be addressed when convenient. */
  LOW,

  /** Medium severity issue that should be addressed in near future. */
  MEDIUM,

  /** High severity issue requiring immediate attention. */
  HIGH
}

/** Batch recommendation result for multiple tests. */
final class BatchRecommendationResult {
  private final Map<String, RecommendationResult> testRecommendations;
  private final List<ActionableRecommendation> commonIssues;
  private final Map<IssueCategory, Integer> issueCategoryCounts;
  private final BatchRecommendationSummary summary;
  private final Instant analysisTime;

  public BatchRecommendationResult(
      final Map<String, RecommendationResult> testRecommendations,
      final List<ActionableRecommendation> commonIssues,
      final Map<IssueCategory, Integer> issueCategoryCounts,
      final BatchRecommendationSummary summary,
      final Instant analysisTime) {
    this.testRecommendations = Map.copyOf(testRecommendations);
    this.commonIssues = List.copyOf(commonIssues);
    this.issueCategoryCounts = Map.copyOf(issueCategoryCounts);
    this.summary = Objects.requireNonNull(summary, "summary cannot be null");
    this.analysisTime = Objects.requireNonNull(analysisTime, "analysisTime cannot be null");
  }

  public Map<String, RecommendationResult> getTestRecommendations() {
    return testRecommendations;
  }

  public List<ActionableRecommendation> getCommonIssues() {
    return commonIssues;
  }

  public Map<IssueCategory, Integer> getIssueCategoryCounts() {
    return issueCategoryCounts;
  }

  public BatchRecommendationSummary getSummary() {
    return summary;
  }

  public Instant getAnalysisTime() {
    return analysisTime;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final BatchRecommendationResult that = (BatchRecommendationResult) obj;
    return Objects.equals(testRecommendations, that.testRecommendations)
        && Objects.equals(commonIssues, that.commonIssues)
        && Objects.equals(issueCategoryCounts, that.issueCategoryCounts)
        && Objects.equals(summary, that.summary)
        && Objects.equals(analysisTime, that.analysisTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        testRecommendations, commonIssues, issueCategoryCounts, summary, analysisTime);
  }

  @Override
  public String toString() {
    return "BatchRecommendationResult{"
        + "tests="
        + testRecommendations.size()
        + ", commonIssues="
        + commonIssues.size()
        + ", totalRecommendations="
        + summary.getTotalRecommendations()
        + '}';
  }
}

/** Summary of batch recommendation analysis. */
final class BatchRecommendationSummary {
  private final int totalRecommendations;
  private final int totalHighPriority;
  private final Map<IssueCategory, Integer> categoryTotals;

  public BatchRecommendationSummary(
      final int totalRecommendations,
      final int totalHighPriority,
      final Map<IssueCategory, Integer> categoryTotals) {
    this.totalRecommendations = totalRecommendations;
    this.totalHighPriority = totalHighPriority;
    this.categoryTotals = Map.copyOf(categoryTotals);
  }

  public int getTotalRecommendations() {
    return totalRecommendations;
  }

  public int getTotalHighPriority() {
    return totalHighPriority;
  }

  public Map<IssueCategory, Integer> getCategoryTotals() {
    return categoryTotals;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final BatchRecommendationSummary that = (BatchRecommendationSummary) obj;
    return totalRecommendations == that.totalRecommendations
        && totalHighPriority == that.totalHighPriority
        && Objects.equals(categoryTotals, that.categoryTotals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalRecommendations, totalHighPriority, categoryTotals);
  }

  @Override
  public String toString() {
    return "BatchRecommendationSummary{"
        + "total="
        + totalRecommendations
        + ", highPriority="
        + totalHighPriority
        + '}';
  }
}

/** Container for all analysis results for a single test. */
final class TestAnalysisResults {
  private final BehavioralAnalysisResult behavioralResults;
  private final PerformanceAnalyzer.PerformanceComparisonResult performanceResults;
  private final CoverageAnalysisResult coverageResults;

  public TestAnalysisResults(
      final BehavioralAnalysisResult behavioralResults,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults,
      final CoverageAnalysisResult coverageResults) {
    this.behavioralResults =
        Objects.requireNonNull(behavioralResults, "behavioralResults cannot be null");
    this.performanceResults =
        Objects.requireNonNull(performanceResults, "performanceResults cannot be null");
    this.coverageResults =
        Objects.requireNonNull(coverageResults, "coverageResults cannot be null");
  }

  public BehavioralAnalysisResult getBehavioralResults() {
    return behavioralResults;
  }

  public PerformanceAnalyzer.PerformanceComparisonResult getPerformanceResults() {
    return performanceResults;
  }

  public CoverageAnalysisResult getCoverageResults() {
    return coverageResults;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final TestAnalysisResults that = (TestAnalysisResults) obj;
    return Objects.equals(behavioralResults, that.behavioralResults)
        && Objects.equals(performanceResults, that.performanceResults)
        && Objects.equals(coverageResults, that.coverageResults);
  }

  @Override
  public int hashCode() {
    return Objects.hash(behavioralResults, performanceResults, coverageResults);
  }

  @Override
  public String toString() {
    return "TestAnalysisResults{"
        + "behavioral="
        + behavioralResults
        + ", performance="
        + performanceResults
        + ", coverage="
        + coverageResults
        + '}';
  }
}
