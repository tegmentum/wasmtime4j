package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a divergence in execution paths between different runtime implementations, indicating
 * where control flow or execution behavior differs across runtimes.
 *
 * @since 1.0.0
 */
public final class ExecutionPathDivergence {
  private final Set<RuntimeType> runtimeGroup1;
  private final String executionPath1;
  private final Set<RuntimeType> runtimeGroup2;
  private final String executionPath2;
  private final DiscrepancySeverity severity;
  private final String description;

  /**
   * Creates a new execution path divergence with the specified details.
   *
   * @param runtimeGroup1 first group of runtimes with same execution path
   * @param executionPath1 execution path for first group
   * @param runtimeGroup2 second group of runtimes with different execution path
   * @param executionPath2 execution path for second group
   * @param severity severity level of this divergence
   */
  public ExecutionPathDivergence(
      final Set<RuntimeType> runtimeGroup1,
      final String executionPath1,
      final Set<RuntimeType> runtimeGroup2,
      final String executionPath2,
      final DiscrepancySeverity severity) {
    this.runtimeGroup1 = Set.copyOf(runtimeGroup1);
    this.executionPath1 = Objects.requireNonNull(executionPath1, "executionPath1 cannot be null");
    this.runtimeGroup2 = Set.copyOf(runtimeGroup2);
    this.executionPath2 = Objects.requireNonNull(executionPath2, "executionPath2 cannot be null");
    this.severity = Objects.requireNonNull(severity, "severity cannot be null");
    this.description = generateDescription();
  }

  public Set<RuntimeType> getRuntimeGroup1() {
    return runtimeGroup1;
  }

  public String getExecutionPath1() {
    return executionPath1;
  }

  public Set<RuntimeType> getRuntimeGroup2() {
    return runtimeGroup2;
  }

  public String getExecutionPath2() {
    return executionPath2;
  }

  public DiscrepancySeverity getSeverity() {
    return severity;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Checks if this divergence affects JNI vs Panama runtimes.
   *
   * @return true if JNI and Panama follow different execution paths
   */
  public boolean affectsJniPanamaEquivalence() {
    final boolean group1HasJni = runtimeGroup1.contains(RuntimeType.JNI);
    final boolean group1HasPanama = runtimeGroup1.contains(RuntimeType.PANAMA);
    final boolean group2HasJni = runtimeGroup2.contains(RuntimeType.JNI);
    final boolean group2HasPanama = runtimeGroup2.contains(RuntimeType.PANAMA);

    return (group1HasJni && group2HasPanama) || (group1HasPanama && group2HasJni);
  }

  /** Generates a human-readable description of the divergence. */
  private String generateDescription() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Execution path divergence: ");
    sb.append(runtimeGroup1).append(" follows path '").append(executionPath1).append("', ");
    sb.append("while ")
        .append(runtimeGroup2)
        .append(" follows path '")
        .append(executionPath2)
        .append("'");

    if (affectsJniPanamaEquivalence()) {
      sb.append(" (affects JNI/Panama equivalence)");
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

    final ExecutionPathDivergence that = (ExecutionPathDivergence) obj;
    return Objects.equals(runtimeGroup1, that.runtimeGroup1)
        && Objects.equals(executionPath1, that.executionPath1)
        && Objects.equals(runtimeGroup2, that.runtimeGroup2)
        && Objects.equals(executionPath2, that.executionPath2)
        && severity == that.severity;
  }

  @Override
  public int hashCode() {
    return Objects.hash(runtimeGroup1, executionPath1, runtimeGroup2, executionPath2, severity);
  }

  @Override
  public String toString() {
    return "ExecutionPathDivergence{"
        + "severity="
        + severity
        + ", affectsJniPanama="
        + affectsJniPanamaEquivalence()
        + ", description='"
        + description
        + '\''
        + '}';
  }
}
