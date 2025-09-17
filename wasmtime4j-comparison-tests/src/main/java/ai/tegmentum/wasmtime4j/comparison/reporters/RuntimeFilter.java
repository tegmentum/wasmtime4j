package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Objects;
import java.util.Set;

/**
 * Filter for runtime types.
 *
 * @since 1.0.0
 */
public final class RuntimeFilter {
  private final Set<RuntimeType> includedRuntimes;
  private final RuntimeMatchMode matchMode;

  public RuntimeFilter(final Set<RuntimeType> includedRuntimes, final RuntimeMatchMode matchMode) {
    this.includedRuntimes = Set.copyOf(includedRuntimes);
    this.matchMode = Objects.requireNonNull(matchMode, "matchMode cannot be null");
  }

  public Set<RuntimeType> getIncludedRuntimes() {
    return includedRuntimes;
  }

  public RuntimeMatchMode getMatchMode() {
    return matchMode;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final RuntimeFilter that = (RuntimeFilter) obj;
    return Objects.equals(includedRuntimes, that.includedRuntimes) && matchMode == that.matchMode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(includedRuntimes, matchMode);
  }

  @Override
  public String toString() {
    return "RuntimeFilter{" + "runtimes=" + includedRuntimes + ", mode=" + matchMode + '}';
  }
}
