package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Comprehensive filtering criteria for comparison results with support for multiple filter types,
 * search queries, sorting, and pagination. Provides a fluent builder API for constructing complex
 * filter combinations.
 *
 * @since 1.0.0
 */
public final class FilterCriteria {
  private final StatusFilter statusFilter;
  private final RuntimeFilter runtimeFilter;
  private final SeverityFilter severityFilter;
  private final TimeRangeFilter timeRangeFilter;
  private final boolean onlyCriticalIssues;
  private final String searchQuery;
  private final SortCriteria sortCriteria;
  private final PaginationConfig pagination;
  private final Map<String, Object> customFilters;

  private FilterCriteria(final Builder builder) {
    this.statusFilter = builder.statusFilter;
    this.runtimeFilter = builder.runtimeFilter;
    this.severityFilter = builder.severityFilter;
    this.timeRangeFilter = builder.timeRangeFilter;
    this.onlyCriticalIssues = builder.onlyCriticalIssues;
    this.searchQuery = builder.searchQuery;
    this.sortCriteria = builder.sortCriteria;
    this.pagination = builder.pagination;
    this.customFilters =
        builder.customFilters != null ? Map.copyOf(builder.customFilters) : Map.of();
  }

  public StatusFilter getStatusFilter() {
    return statusFilter;
  }

  public RuntimeFilter getRuntimeFilter() {
    return runtimeFilter;
  }

  public SeverityFilter getSeverityFilter() {
    return severityFilter;
  }

  public TimeRangeFilter getTimeRangeFilter() {
    return timeRangeFilter;
  }

  public boolean isOnlyCriticalIssues() {
    return onlyCriticalIssues;
  }

  public String getSearchQuery() {
    return searchQuery;
  }

  public SortCriteria getSortCriteria() {
    return sortCriteria;
  }

  public PaginationConfig getPagination() {
    return pagination;
  }

  public Map<String, Object> getCustomFilters() {
    return customFilters;
  }

  /**
   * Checks if any filters are applied.
   *
   * @return true if any filters are active
   */
  public boolean hasActiveFilters() {
    return statusFilter != null
        || runtimeFilter != null
        || severityFilter != null
        || timeRangeFilter != null
        || onlyCriticalIssues
        || (searchQuery != null && !searchQuery.trim().isEmpty())
        || !customFilters.isEmpty();
  }

  /** Creates a new builder for filter criteria. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for FilterCriteria. */
  public static final class Builder {
    private StatusFilter statusFilter;
    private RuntimeFilter runtimeFilter;
    private SeverityFilter severityFilter;
    private TimeRangeFilter timeRangeFilter;
    private boolean onlyCriticalIssues = false;
    private String searchQuery;
    private SortCriteria sortCriteria;
    private PaginationConfig pagination;
    private Map<String, Object> customFilters;

    public Builder statusFilter(final StatusFilter statusFilter) {
      this.statusFilter = statusFilter;
      return this;
    }

    public Builder includeStatuses(final Set<TestResultStatus> statuses) {
      this.statusFilter = new StatusFilter(statuses);
      return this;
    }

    public Builder runtimeFilter(final RuntimeFilter runtimeFilter) {
      this.runtimeFilter = runtimeFilter;
      return this;
    }

    public Builder includeRuntimes(
        final Set<RuntimeType> runtimes, final RuntimeMatchMode matchMode) {
      this.runtimeFilter = new RuntimeFilter(runtimes, matchMode);
      return this;
    }

    public Builder severityFilter(final SeverityFilter severityFilter) {
      this.severityFilter = severityFilter;
      return this;
    }

    public Builder includeSeverities(final Set<String> severities) {
      this.severityFilter = new SeverityFilter(severities);
      return this;
    }

    public Builder timeRangeFilter(final TimeRangeFilter timeRangeFilter) {
      this.timeRangeFilter = timeRangeFilter;
      return this;
    }

    public Builder timeRange(final Instant startTime, final Instant endTime) {
      this.timeRangeFilter = new TimeRangeFilter(startTime, endTime);
      return this;
    }

    public Builder onlyCriticalIssues(final boolean onlyCriticalIssues) {
      this.onlyCriticalIssues = onlyCriticalIssues;
      return this;
    }

    public Builder searchQuery(final String searchQuery) {
      this.searchQuery = searchQuery;
      return this;
    }

    public Builder sortCriteria(final SortCriteria sortCriteria) {
      this.sortCriteria = sortCriteria;
      return this;
    }

    public Builder sortBy(final String field, final SortDirection direction) {
      this.sortCriteria = new SortCriteria(field, direction);
      return this;
    }

    public Builder pagination(final PaginationConfig pagination) {
      this.pagination = pagination;
      return this;
    }

    public Builder pagination(final int pageNumber, final int pageSize) {
      this.pagination = new PaginationConfig(pageNumber, pageSize);
      return this;
    }

    public Builder customFilters(final Map<String, Object> customFilters) {
      this.customFilters = customFilters;
      return this;
    }

    public FilterCriteria build() {
      return new FilterCriteria(this);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final FilterCriteria that = (FilterCriteria) obj;
    return onlyCriticalIssues == that.onlyCriticalIssues
        && Objects.equals(statusFilter, that.statusFilter)
        && Objects.equals(runtimeFilter, that.runtimeFilter)
        && Objects.equals(severityFilter, that.severityFilter)
        && Objects.equals(timeRangeFilter, that.timeRangeFilter)
        && Objects.equals(searchQuery, that.searchQuery)
        && Objects.equals(sortCriteria, that.sortCriteria)
        && Objects.equals(pagination, that.pagination)
        && Objects.equals(customFilters, that.customFilters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        statusFilter,
        runtimeFilter,
        severityFilter,
        timeRangeFilter,
        onlyCriticalIssues,
        searchQuery,
        sortCriteria,
        pagination,
        customFilters);
  }

  @Override
  public String toString() {
    return "FilterCriteria{"
        + "hasFilters="
        + hasActiveFilters()
        + ", search='"
        + searchQuery
        + '\''
        + ", onlyCritical="
        + onlyCriticalIssues
        + '}';
  }
}

/** Filter for test result statuses. */
final class StatusFilter {
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

/** Filter for runtime types. */
final class RuntimeFilter {
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

/** Runtime matching modes for filtering. */
enum RuntimeMatchMode {
  /** Test must include ANY of the specified runtimes. */
  ANY,

  /** Test must include ALL of the specified runtimes. */
  ALL,

  /** Test must include EXACTLY the specified runtimes. */
  EXACT
}

/** Filter for discrepancy severities. */
final class SeverityFilter {
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

/** Filter for time ranges. */
final class TimeRangeFilter {
  private final Instant startTime;
  private final Instant endTime;

  public TimeRangeFilter(final Instant startTime, final Instant endTime) {
    this.startTime = startTime;
    this.endTime = endTime;

    if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
      throw new IllegalArgumentException("Start time cannot be after end time");
    }
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final TimeRangeFilter that = (TimeRangeFilter) obj;
    return Objects.equals(startTime, that.startTime) && Objects.equals(endTime, that.endTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startTime, endTime);
  }

  @Override
  public String toString() {
    return "TimeRangeFilter{" + "start=" + startTime + ", end=" + endTime + '}';
  }
}

/** Sort criteria for ordering results. */
final class SortCriteria {
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

/** Sort direction enum. */
enum SortDirection {
  /** Ascending sort order. */
  ASCENDING,

  /** Descending sort order. */
  DESCENDING
}

/** Pagination configuration. */
final class PaginationConfig {
  private final int pageNumber;
  private final int pageSize;

  public PaginationConfig(final int pageNumber, final int pageSize) {
    if (pageNumber < 0) {
      throw new IllegalArgumentException("Page number cannot be negative");
    }
    if (pageSize <= 0) {
      throw new IllegalArgumentException("Page size must be positive");
    }

    this.pageNumber = pageNumber;
    this.pageSize = pageSize;
  }

  public int getPageNumber() {
    return pageNumber;
  }

  public int getPageSize() {
    return pageSize;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final PaginationConfig that = (PaginationConfig) obj;
    return pageNumber == that.pageNumber && pageSize == that.pageSize;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pageNumber, pageSize);
  }

  @Override
  public String toString() {
    return "PaginationConfig{" + "page=" + pageNumber + ", size=" + pageSize + '}';
  }
}

/** Result of a filter operation. */
final class FilterResult {
  private final List<TestComparisonResult> filteredResults;
  private final int totalCount;
  private final int pageCount;
  private final FilterCriteria appliedCriteria;
  private final long filterTimeMs;
  private final Map<String, Object> metadata;

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

/** Result of pagination operation. */
final class PaginationResult {
  private final List<TestComparisonResult> results;
  private final int totalCount;
  private final int pageCount;

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

/** Search term representation. */
final class SearchTerm {
  private final String field;
  private final String text;
  private final boolean isRegex;
  private final boolean isNegated;

  public SearchTerm(
      final String field, final String text, final boolean isRegex, final boolean isNegated) {
    this.field = Objects.requireNonNull(field, "field cannot be null");
    this.text = Objects.requireNonNull(text, "text cannot be null");
    this.isRegex = isRegex;
    this.isNegated = isNegated;
  }

  public String getField() {
    return field;
  }

  public String getText() {
    return text;
  }

  public boolean isRegex() {
    return isRegex;
  }

  public boolean isNegated() {
    return isNegated;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final SearchTerm that = (SearchTerm) obj;
    return isRegex == that.isRegex
        && isNegated == that.isNegated
        && Objects.equals(field, that.field)
        && Objects.equals(text, that.text);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, text, isRegex, isNegated);
  }

  @Override
  public String toString() {
    return "SearchTerm{"
        + "field='"
        + field
        + '\''
        + ", text='"
        + text
        + '\''
        + ", regex="
        + isRegex
        + ", negated="
        + isNegated
        + '}';
  }
}

/** Simple search query parser. */
final class SearchQueryParser {
  private final String query;
  private final SearchConfiguration configuration;

  public SearchQueryParser(final String query, final SearchConfiguration configuration) {
    this.query = Objects.requireNonNull(query, "query cannot be null");
    this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
  }

  /**
   * Parses the search query into individual search terms.
   *
   * @return list of parsed search terms
   */
  public List<SearchTerm> parseQuery() {
    final List<SearchTerm> terms = new ArrayList<>();

    // Simple parsing - in a real implementation this would be more sophisticated
    final String[] parts = query.trim().split("\\s+");

    for (final String part : parts) {
      if (part.contains(":")) {
        // Field-specific search: field:term
        final String[] fieldParts = part.split(":", 2);
        final String field = fieldParts[0];
        final String text = fieldParts.length > 1 ? fieldParts[1] : "";
        terms.add(new SearchTerm(field, text, false, false));
      } else {
        // Global search
        terms.add(new SearchTerm("all", part, false, false));
      }
    }

    return terms;
  }
}
