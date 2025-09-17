package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Individual actionable recommendation with implementation guidance.
 *
 * @since 1.0.0
 */
public final class ActionableRecommendation {
  private final String title;
  private final String description;
  private final List<String> implementationSteps;
  private final IssueCategory category;
  private final IssueSeverity severity;
  private final double priorityScore;
  private final Set<RuntimeType> affectedRuntimes;
  private final String issuePattern;

  /**
   * Creates a new actionable recommendation.
   *
   * @param title the title of the recommendation
   * @param description the description of the recommendation
   * @param implementationSteps the steps to implement the recommendation
   * @param category the issue category
   * @param severity the issue severity
   * @param priorityScore the priority score
   * @param affectedRuntimes the set of affected runtimes
   * @param issuePattern the issue pattern
   */
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

  /**
   * Gets the recommendation title.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the recommendation description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the implementation steps.
   *
   * @return the list of implementation steps
   */
  public List<String> getImplementationSteps() {
    return implementationSteps;
  }

  /**
   * Gets the issue category.
   *
   * @return the category
   */
  public IssueCategory getCategory() {
    return category;
  }

  /**
   * Gets the issue severity.
   *
   * @return the severity
   */
  public IssueSeverity getSeverity() {
    return severity;
  }

  /**
   * Gets the priority score.
   *
   * @return the priority score
   */
  public double getPriorityScore() {
    return priorityScore;
  }

  /**
   * Gets the affected runtimes.
   *
   * @return the set of affected runtimes
   */
  public Set<RuntimeType> getAffectedRuntimes() {
    return affectedRuntimes;
  }

  /**
   * Gets the issue pattern.
   *
   * @return the issue pattern
   */
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
