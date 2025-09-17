package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    /**
     * Sets behavioral recommendations for this result.
     *
     * @param recommendations list of behavioral recommendations
     * @return this builder
     */
    public Builder behavioralRecommendations(final List<ActionableRecommendation> recommendations) {
      this.behavioralRecommendations =
          Objects.requireNonNull(recommendations, "recommendations cannot be null");
      return this;
    }

    /**
     * Sets performance recommendations for this result.
     *
     * @param recommendations list of performance recommendations
     * @return this builder
     */
    public Builder performanceRecommendations(
        final List<ActionableRecommendation> recommendations) {
      this.performanceRecommendations =
          Objects.requireNonNull(recommendations, "recommendations cannot be null");
      return this;
    }

    /**
     * Sets coverage recommendations for this result.
     *
     * @param recommendations list of coverage recommendations
     * @return this builder
     */
    public Builder coverageRecommendations(final List<ActionableRecommendation> recommendations) {
      this.coverageRecommendations =
          Objects.requireNonNull(recommendations, "recommendations cannot be null");
      return this;
    }

    /**
     * Sets integration recommendations for this result.
     *
     * @param recommendations list of integration recommendations
     * @return this builder
     */
    public Builder integrationRecommendations(
        final List<ActionableRecommendation> recommendations) {
      this.integrationRecommendations =
          Objects.requireNonNull(recommendations, "recommendations cannot be null");
      return this;
    }

    /**
     * Sets prioritized recommendations for this result.
     *
     * @param recommendations list of prioritized recommendations
     * @return this builder
     */
    public Builder prioritizedRecommendations(
        final List<ActionableRecommendation> recommendations) {
      this.prioritizedRecommendations =
          Objects.requireNonNull(recommendations, "recommendations cannot be null");
      return this;
    }

    /**
     * Sets the recommendation summary.
     *
     * @param summary recommendation summary
     * @return this builder
     */
    public Builder summary(final RecommendationSummary summary) {
      this.summary = Objects.requireNonNull(summary, "summary cannot be null");
      return this;
    }

    /**
     * Builds the recommendation result.
     *
     * @return new RecommendationResult instance
     * @throws IllegalStateException if summary is not set
     */
    public RecommendationResult build() {
      if (summary == null) {
        throw new IllegalStateException("summary must be set");
      }
      return new RecommendationResult(this);
    }
  }
}
