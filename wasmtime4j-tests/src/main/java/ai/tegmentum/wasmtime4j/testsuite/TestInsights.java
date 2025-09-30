package ai.tegmentum.wasmtime4j.testsuite;

import java.util.List;

/** Insights and recommendations generated from test result analysis. */
public final class TestInsights {

  private final List<String> insights;
  private final List<String> recommendations;

  public TestInsights(final List<String> insights, final List<String> recommendations) {
    this.insights = List.copyOf(insights);
    this.recommendations = List.copyOf(recommendations);
  }

  public List<String> getInsights() {
    return insights;
  }

  public List<String> getRecommendations() {
    return recommendations;
  }

  /**
   * Checks if there are any insights available.
   *
   * @return true if insights exist
   */
  public boolean hasInsights() {
    return !insights.isEmpty();
  }

  /**
   * Checks if there are any recommendations available.
   *
   * @return true if recommendations exist
   */
  public boolean hasRecommendations() {
    return !recommendations.isEmpty();
  }
}
