package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Batch recommendation result for multiple tests.
 *
 * @since 1.0.0
 */
public final class BatchRecommendationResult {
  private final Map<String, RecommendationResult> testRecommendations;
  private final List<ActionableRecommendation> commonIssues;
  private final Map<IssueCategory, Integer> issueCategoryCounts;
  private final BatchRecommendationSummary summary;
  private final Instant analysisTime;

  /**
   * Creates a new batch recommendation result.
   *
   * @param testRecommendations the per-test recommendations
   * @param commonIssues the common issues across tests
   * @param issueCategoryCounts the issue category counts
   * @param summary the batch summary
   * @param analysisTime the analysis time
   */
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

  /**
   * Gets the test recommendations.
   *
   * @return the test recommendations
   */
  public Map<String, RecommendationResult> getTestRecommendations() {
    return testRecommendations;
  }

  /**
   * Gets the common issues.
   *
   * @return the list of common issues
   */
  public List<ActionableRecommendation> getCommonIssues() {
    return commonIssues;
  }

  /**
   * Gets the issue category counts.
   *
   * @return the issue category counts
   */
  public Map<IssueCategory, Integer> getIssueCategoryCounts() {
    return issueCategoryCounts;
  }

  /**
   * Gets the batch summary.
   *
   * @return the summary
   */
  public BatchRecommendationSummary getSummary() {
    return summary;
  }

  /**
   * Gets the analysis time.
   *
   * @return the analysis time
   */
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
