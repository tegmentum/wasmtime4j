package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.List;
import java.util.Objects;

/**
 * Result of pagination operation.
 *
 * @since 1.0.0
 */
public final class PaginationResult {
  private final List<TestComparisonResult> results;
  private final int totalCount;
  private final int pageCount;

  /**
   * Creates a new pagination result.
   *
   * @param results the results for this page
   * @param totalCount the total number of results across all pages
   * @param pageCount the total number of pages
   */
  public PaginationResult(
      final List<TestComparisonResult> results, final int totalCount, final int pageCount) {
    this.results = List.copyOf(results);
    this.totalCount = totalCount;
    this.pageCount = pageCount;
  }

  public List<TestComparisonResult> getResults() {
    return results;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public int getPageCount() {
    return pageCount;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final PaginationResult that = (PaginationResult) obj;
    return totalCount == that.totalCount
        && pageCount == that.pageCount
        && Objects.equals(results, that.results);
  }

  @Override
  public int hashCode() {
    return Objects.hash(results, totalCount, pageCount);
  }

  @Override
  public String toString() {
    return "PaginationResult{"
        + "results="
        + results.size()
        + ", total="
        + totalCount
        + ", pages="
        + pageCount
        + '}';
  }
}
