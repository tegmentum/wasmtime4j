package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of a filter operation.
 *
 * @since 1.0.0
 */
public final class FilterResult {
  private final List<TestComparisonResult> filteredResults;
  private final int totalCount;
  private final int pageCount;
  private final FilterCriteria appliedCriteria;
  private final long filterTimeMs;
  private final Map<String, Object> metadata;

  /**
   * Creates a new filter result.
   *
   * @param filteredResults the filtered results
   * @param totalCount the total count of filtered items
   * @param pageCount the number of pages
   * @param appliedCriteria the filter criteria that was applied
   * @param filterTimeMs the time taken to filter in milliseconds
   * @param metadata additional metadata about the filtering operation
   */
  public FilterResult(
      final List<TestComparisonResult> filteredResults,
      final int totalCount,
      final int pageCount,
      final FilterCriteria appliedCriteria,
      final long filterTimeMs,
      final Map<String, Object> metadata) {
    this.filteredResults = List.copyOf(filteredResults);
    this.totalCount = totalCount;
    this.pageCount = pageCount;
    this.appliedCriteria =
        Objects.requireNonNull(appliedCriteria, "appliedCriteria cannot be null");
    this.filterTimeMs = filterTimeMs;
    this.metadata = Map.copyOf(metadata);
  }

  public List<TestComparisonResult> getFilteredResults() {
    return filteredResults;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public int getPageCount() {
    return pageCount;
  }

  public FilterCriteria getAppliedCriteria() {
    return appliedCriteria;
  }

  public long getFilterTimeMs() {
    return filterTimeMs;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final FilterResult that = (FilterResult) obj;
    return totalCount == that.totalCount
        && pageCount == that.pageCount
        && filterTimeMs == that.filterTimeMs
        && Objects.equals(filteredResults, that.filteredResults)
        && Objects.equals(appliedCriteria, that.appliedCriteria)
        && Objects.equals(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        filteredResults, totalCount, pageCount, appliedCriteria, filterTimeMs, metadata);
  }

  @Override
  public String toString() {
    return "FilterResult{"
        + "results="
        + filteredResults.size()
        + ", total="
        + totalCount
        + ", pages="
        + pageCount
        + ", time="
        + filterTimeMs
        + "ms"
        + '}';
  }
}
