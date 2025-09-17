package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/**
 * Sort criteria for ordering results.
 *
 * @since 1.0.0
 */
public final class SortCriteria {
  private final String sortField;
  private final SortDirection sortDirection;

  public SortCriteria(final String sortField, final SortDirection sortDirection) {
    this.sortField = Objects.requireNonNull(sortField, "sortField cannot be null");
    this.sortDirection = Objects.requireNonNull(sortDirection, "sortDirection cannot be null");
  }

  public String getSortField() {
    return sortField;
  }

  public SortDirection getSortDirection() {
    return sortDirection;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final SortCriteria that = (SortCriteria) obj;
    return Objects.equals(sortField, that.sortField) && sortDirection == that.sortDirection;
  }

  @Override
  public int hashCode() {
    return Objects.hash(sortField, sortDirection);
  }

  @Override
  public String toString() {
    return "SortCriteria{" + "field='" + sortField + '\'' + ", direction=" + sortDirection + '}';
  }
}
