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

/** Types of behavioral discrepancies that can be detected. */
enum DiscrepancyType {
  /** Execution status differs between runtimes (success vs failure). */
  EXECUTION_STATUS_MISMATCH("Execution status inconsistency"),

  /** Return values differ between successful executions. */
  RETURN_VALUE_MISMATCH("Return value inconsistency"),

  /** Exception types differ between failed executions. */
  EXCEPTION_TYPE_MISMATCH("Exception type inconsistency"),

  /** Exception messages differ between failed executions. */
  EXCEPTION_MESSAGE_MISMATCH("Exception message inconsistency"),

  /** Significant performance differences between runtimes. */
  PERFORMANCE_DEVIATION("Performance deviation"),

  /** Memory usage differs significantly between runtimes. */
  MEMORY_USAGE_DEVIATION("Memory usage deviation"),

  /** Some runtimes skip test while others execute it. */
  SKIP_INCONSISTENCY("Skip behavior inconsistency"),

  /** Systematic pattern of failures detected. */
  SYSTEMATIC_PATTERN("Systematic failure pattern"),

  /** Other unclassified discrepancy. */
  OTHER("Other discrepancy");

  private final String description;

  DiscrepancyType(final String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}

/** Severity levels for behavioral discrepancies. */
enum DiscrepancySeverity {
  /** Critical discrepancy requiring immediate attention. */
  CRITICAL("Critical", 4),

  /** Major discrepancy that should be addressed. */
  MAJOR("Major", 3),

  /** Moderate discrepancy worth investigating. */
  MODERATE("Moderate", 2),

  /** Minor discrepancy that may be acceptable. */
  MINOR("Minor", 1);

  private final String displayName;
  private final int priority;

  DiscrepancySeverity(final String displayName, final int priority) {
    this.displayName = displayName;
    this.priority = priority;
  }

  public String getDisplayName() {
    return displayName;
  }

  public int getPriority() {
    return priority;
  }

  /**
   * Checks if this severity is higher than another.
   *
   * @param other the other severity
   * @return true if this severity is higher
   */
  public boolean isHigherThan(final DiscrepancySeverity other) {
    return this.priority > other.priority;
  }
}
