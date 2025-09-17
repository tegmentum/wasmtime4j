package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Map;
import java.util.Objects;

/**
 * Summary of batch recommendation analysis.
 *
 * @since 1.0.0
 */
public final class BatchRecommendationSummary {
  private final int totalRecommendations;
  private final int totalHighPriority;
  private final Map<IssueCategory, Integer> categoryTotals;

  /**
   * Creates a new batch recommendation summary.
   *
   * @param totalRecommendations the total number of recommendations
   * @param totalHighPriority the total high priority count
   * @param categoryTotals the category totals
   */
  public BatchRecommendationSummary(
      final int totalRecommendations,
      final int totalHighPriority,
      final Map<IssueCategory, Integer> categoryTotals) {
    this.totalRecommendations = totalRecommendations;
    this.totalHighPriority = totalHighPriority;
    this.categoryTotals = Map.copyOf(categoryTotals);
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
   * Gets the total high priority count.
   *
   * @return the high priority count
   */
  public int getTotalHighPriority() {
    return totalHighPriority;
  }

  /**
   * Gets the category totals.
   *
   * @return the category totals
   */
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
