package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of execution path validation analysis, tracking path consistency, divergences, and control
 * flow patterns across different runtimes to ensure behavioral equivalence.
 *
 * @since 1.0.0
 */
public final class ExecutionPathValidationResult {
  private final Map<RuntimeType, String> executionPaths;
  private final List<ExecutionPathDivergence> divergences;
  private final double pathConsistencyScore;
  private final boolean pathsConsistent;

  private ExecutionPathValidationResult(final Builder builder) {
    this.executionPaths = Map.copyOf(builder.executionPaths);
    this.divergences = List.copyOf(builder.divergences);
    this.pathConsistencyScore = builder.pathConsistencyScore;
    this.pathsConsistent = builder.pathsConsistent;
  }

  public Map<RuntimeType, String> getExecutionPaths() {
    return executionPaths;
  }

  public List<ExecutionPathDivergence> getDivergences() {
    return divergences;
  }

  public double getPathConsistencyScore() {
    return pathConsistencyScore;
  }

  public boolean arePathsConsistent() {
    return pathsConsistent;
  }

  /**
   * Checks if execution paths meet zero divergence requirement.
   *
   * @return true if no critical path divergences exist
   */
  public boolean meetsZeroDivergenceRequirement() {
    return divergences.stream().noneMatch(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL);
  }

  /**
   * Gets the number of critical path divergences.
   *
   * @return count of critical divergences
   */
  public long getCriticalDivergenceCount() {
    return divergences.stream()
        .filter(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL)
        .count();
  }

  /**
   * Gets the number of unique execution paths.
   *
   * @return count of unique paths
   */
  public long getUniquePathCount() {
    return executionPaths.values().stream().distinct().count();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ExecutionPathValidationResult that = (ExecutionPathValidationResult) obj;
    return Double.compare(that.pathConsistencyScore, pathConsistencyScore) == 0
        && pathsConsistent == that.pathsConsistent
        && Objects.equals(executionPaths, that.executionPaths)
        && Objects.equals(divergences, that.divergences);
  }

  @Override
  public int hashCode() {
    return Objects.hash(executionPaths, divergences, pathConsistencyScore, pathsConsistent);
  }

  @Override
  public String toString() {
    return "ExecutionPathValidationResult{"
        + "pathsConsistent="
        + pathsConsistent
        + ", consistencyScore="
        + String.format("%.2f", pathConsistencyScore)
        + ", uniquePaths="
        + getUniquePathCount()
        + ", divergences="
        + divergences.size()
        + ", criticalDivergences="
        + getCriticalDivergenceCount()
        + '}';
  }

  /** Builder for ExecutionPathValidationResult. */
  public static final class Builder {
    private Map<RuntimeType, String> executionPaths = Map.of();
    private List<ExecutionPathDivergence> divergences = List.of();
    private double pathConsistencyScore = 1.0;
    private boolean pathsConsistent = true;

    public Builder executionPaths(final Map<RuntimeType, String> executionPaths) {
      this.executionPaths = Objects.requireNonNull(executionPaths, "executionPaths cannot be null");
      return this;
    }

    public Builder divergences(final List<ExecutionPathDivergence> divergences) {
      this.divergences = Objects.requireNonNull(divergences, "divergences cannot be null");
      return this;
    }

    public Builder pathConsistencyScore(final double pathConsistencyScore) {
      if (pathConsistencyScore < 0.0 || pathConsistencyScore > 1.0) {
        throw new IllegalArgumentException("pathConsistencyScore must be between 0.0 and 1.0");
      }
      this.pathConsistencyScore = pathConsistencyScore;
      return this;
    }

    public Builder pathsConsistent(final boolean pathsConsistent) {
      this.pathsConsistent = pathsConsistent;
      return this;
    }

    public ExecutionPathValidationResult build() {
      return new ExecutionPathValidationResult(this);
    }
  }
}
