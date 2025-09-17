package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Map;
import java.util.Objects;

/**
 * Summary of recommendations with counts and breakdowns.
 *
 * @since 1.0.0
 */
public final class RecommendationSummary {
  private final int totalRecommendations;
  private final int highPriorityCount;
  private final int mediumPriorityCount;
  private final int lowPriorityCount;
  private final Map<IssueCategory, Integer> categoryBreakdown;

  /**
   * Creates a new recommendation summary.
   *
   * @param totalRecommendations the total number of recommendations
   * @param highPriorityCount the count of high priority recommendations
   * @param mediumPriorityCount the count of medium priority recommendations
   * @param lowPriorityCount the count of low priority recommendations
   * @param categoryBreakdown the breakdown by category
   */
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

  /**
   * Gets the total number of recommendations.
   *
   * @return the total count
   */
  public int getTotalRecommendations() {
    return totalRecommendations;
  }

  /**
   * Gets the high priority count.
   *
   * @return the high priority count
   */
  public int getHighPriorityCount() {
    return highPriorityCount;
  }

  /**
   * Gets the medium priority count.
   *
   * @return the medium priority count
   */
  public int getMediumPriorityCount() {
    return mediumPriorityCount;
  }

  /**
   * Gets the low priority count.
   *
   * @return the low priority count
   */
  public int getLowPriorityCount() {
    return lowPriorityCount;
  }

  /**
   * Gets the category breakdown.
   *
   * @return the breakdown by category
   */
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
