package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.List;
import java.util.Objects;

/**
 * Executive insight for high-level strategic guidance.
 *
 * @since 1.0.0
 */
public final class ExecutiveInsight {
  private final ExecutiveInsightType type;
  private final String description;
  private final HealthStatus status;
  private final List<String> keyPoints;

  /**
   * Constructs a new ExecutiveInsight with the specified properties.
   *
   * @param type the insight type
   * @param description the insight description
   * @param status the status level
   * @param keyPoints the list of key points
   */
  public ExecutiveInsight(
      final ExecutiveInsightType type,
      final String description,
      final HealthStatus status,
      final List<String> keyPoints) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.status = Objects.requireNonNull(status, "status cannot be null");
    this.keyPoints = List.copyOf(keyPoints);
  }

  public ExecutiveInsightType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public HealthStatus getStatus() {
    return status;
  }

  public List<String> getKeyPoints() {
    return keyPoints;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ExecutiveInsight that = (ExecutiveInsight) obj;
    return type == that.type
        && Objects.equals(description, that.description)
        && status == that.status
        && Objects.equals(keyPoints, that.keyPoints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, description, status, keyPoints);
  }

  @Override
  public String toString() {
    return "ExecutiveInsight{" + "type=" + type + ", status=" + status + '}';
  }
}
