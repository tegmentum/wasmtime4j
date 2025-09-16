package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a behavioral discrepancy detected during comparison of test execution results across
 * different WebAssembly runtime implementations. Provides categorization, severity assessment, and
 * actionable recommendations for addressing the discrepancy.
 *
 * @since 1.0.0
 */
public final class BehavioralDiscrepancy {
  private final DiscrepancyType type;
  private final DiscrepancySeverity severity;
  private final String description;
  private final String details;
  private final String recommendation;
  private final Instant detectedAt;

  /**
   * Creates a new behavioral discrepancy.
   *
   * @param type the type of discrepancy
   * @param severity the severity level
   * @param description brief description of the discrepancy
   * @param details detailed information about the discrepancy
   * @param recommendation suggested action to address the discrepancy
   */
  public BehavioralDiscrepancy(
      final DiscrepancyType type,
      final DiscrepancySeverity severity,
      final String description,
      final String details,
      final String recommendation) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.severity = Objects.requireNonNull(severity, "severity cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.details = Objects.requireNonNull(details, "details cannot be null");
    this.recommendation = Objects.requireNonNull(recommendation, "recommendation cannot be null");
    this.detectedAt = Instant.now();
  }

  public DiscrepancyType getType() {
    return type;
  }

  public DiscrepancySeverity getSeverity() {
    return severity;
  }

  public String getDescription() {
    return description;
  }

  public String getDetails() {
    return details;
  }

  public String getRecommendation() {
    return recommendation;
  }

  public Instant getDetectedAt() {
    return detectedAt;
  }

  /**
   * Checks if this discrepancy is critical and requires immediate attention.
   *
   * @return true if the discrepancy is critical
   */
  public boolean isCritical() {
    return severity == DiscrepancySeverity.CRITICAL;
  }

  /**
   * Gets a formatted summary of the discrepancy.
   *
   * @return formatted summary
   */
  public String getSummary() {
    return String.format("[%s] %s: %s", severity, type, description);
  }

  /**
   * Gets a detailed report of the discrepancy.
   *
   * @return detailed report
   */
  public String getDetailedReport() {
    return String.format(
        "Discrepancy Report:%n"
            + "Type: %s%n"
            + "Severity: %s%n"
            + "Description: %s%n"
            + "Details: %s%n"
            + "Recommendation: %s%n"
            + "Detected: %s",
        type, severity, description, details, recommendation, detectedAt);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final BehavioralDiscrepancy that = (BehavioralDiscrepancy) obj;
    return type == that.type
        && severity == that.severity
        && Objects.equals(description, that.description)
        && Objects.equals(details, that.details)
        && Objects.equals(recommendation, that.recommendation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, severity, description, details, recommendation);
  }

  @Override
  public String toString() {
    return "BehavioralDiscrepancy{"
        + "type="
        + type
        + ", severity="
        + severity
        + ", description='"
        + description
        + '\''
        + ", detectedAt="
        + detectedAt
        + '}';
  }
}

