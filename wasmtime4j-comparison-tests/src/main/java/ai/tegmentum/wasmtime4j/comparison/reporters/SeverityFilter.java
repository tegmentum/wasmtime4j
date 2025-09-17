package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;
import java.util.Set;

/**
 * Filter for discrepancy severities.
 *
 * @since 1.0.0
 */
public final class SeverityFilter {
  private final Set<String> includedSeverities;

  public SeverityFilter(final Set<String> includedSeverities) {
    this.includedSeverities = Set.copyOf(includedSeverities);
  }

  public Set<String> getIncludedSeverities() {
    return includedSeverities;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final SeverityFilter that = (SeverityFilter) obj;
    return Objects.equals(includedSeverities, that.includedSeverities);
  }

  @Override
  public int hashCode() {
    return Objects.hash(includedSeverities);
  }

  @Override
  public String toString() {
    return "SeverityFilter{" + "severities=" + includedSeverities + '}';
  }
}
