package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;
import java.util.Set;

/**
 * Filter for test result statuses.
 *
 * @since 1.0.0
 */
public final class StatusFilter {
  private final Set<TestResultStatus> includedStatuses;

  public StatusFilter(final Set<TestResultStatus> includedStatuses) {
    this.includedStatuses = Set.copyOf(includedStatuses);
  }

  public Set<TestResultStatus> getIncludedStatuses() {
    return includedStatuses;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final StatusFilter that = (StatusFilter) obj;
    return Objects.equals(includedStatuses, that.includedStatuses);
  }

  @Override
  public int hashCode() {
    return Objects.hash(includedStatuses);
  }

  @Override
  public String toString() {
    return "StatusFilter{" + "statuses=" + includedStatuses + '}';
  }
}
