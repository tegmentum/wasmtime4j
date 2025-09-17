package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
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
