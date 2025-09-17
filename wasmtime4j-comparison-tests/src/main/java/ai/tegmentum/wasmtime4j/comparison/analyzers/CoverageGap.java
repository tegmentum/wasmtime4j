package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a gap in feature coverage.
 *
 * @since 1.0.0
 */
public final class CoverageGap {
  private final CoverageGapType type;
  private final String description;
  private final Set<String> affectedFeatures;
  private final Set<RuntimeType> affectedRuntimes;
  private final GapSeverity severity;

  /**
   * Creates a new coverage gap.
   *
   * @param type the type of coverage gap
   * @param description the description of the gap
   * @param affectedFeatures the affected features
   * @param affectedRuntimes the affected runtimes
   * @param severity the severity level
   */
  public CoverageGap(
      final CoverageGapType type,
      final String description,
      final Set<String> affectedFeatures,
      final Set<RuntimeType> affectedRuntimes,
      final GapSeverity severity) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.affectedFeatures = Set.copyOf(affectedFeatures);
    this.affectedRuntimes = Set.copyOf(affectedRuntimes);
    this.severity = Objects.requireNonNull(severity, "severity cannot be null");
  }

  /**
   * Gets the coverage gap type.
   *
   * @return the gap type
   */
  public CoverageGapType getType() {
    return type;
  }

  /**
   * Gets the description of the gap.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the affected features.
   *
   * @return the set of affected features
   */
  public Set<String> getAffectedFeatures() {
    return affectedFeatures;
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
   * Gets the severity level.
   *
   * @return the severity
   */
  public GapSeverity getSeverity() {
    return severity;
  }

  /**
   * Gets the feature name for single-feature gaps.
   *
   * @return the feature name if single feature, or first feature name
   */
  public String getFeatureName() {
    return affectedFeatures.isEmpty() ? "" : affectedFeatures.iterator().next();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CoverageGap that = (CoverageGap) obj;
    return type == that.type
        && Objects.equals(description, that.description)
        && Objects.equals(affectedFeatures, that.affectedFeatures)
        && Objects.equals(affectedRuntimes, that.affectedRuntimes)
        && severity == that.severity;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, description, affectedFeatures, affectedRuntimes, severity);
  }

  @Override
  public String toString() {
    return "CoverageGap{"
        + "type="
        + type
        + ", severity="
        + severity
        + ", features="
        + affectedFeatures.size()
        + ", runtimes="
        + affectedRuntimes.size()
        + '}';
  }
}
