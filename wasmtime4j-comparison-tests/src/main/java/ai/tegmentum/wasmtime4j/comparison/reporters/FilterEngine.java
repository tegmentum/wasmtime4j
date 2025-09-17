package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Advanced filtering and search engine for comparison results with support for complex queries,
 * full-text search, faceted filtering, and real-time result updates. Optimized for handling large
 * result sets with efficient indexing and caching mechanisms.
 *
 * @since 1.0.0
 */
public final class FilterEngine {

  private final Map<String, Object> indexCache;
  private final SearchConfiguration configuration;

  public FilterEngine(final SearchConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
    this.indexCache = new HashMap<>();
  }

  /**
   * Applies filtering and search criteria to a comparison report.
   *
   * @param report the comparison report to filter
   * @param filterCriteria the filtering criteria to apply
   * @return filtered results with metadata
   */
  public FilterResult filterReport(
      final ComparisonReport report, final FilterCriteria filterCriteria) {
    Objects.requireNonNull(report, "report cannot be null");
    Objects.requireNonNull(filterCriteria, "filterCriteria cannot be null");

    final long startTime = System.currentTimeMillis();

    // Apply filters to test results
    List<TestComparisonResult> filteredTests =
        filterTestResults(report.getTestResults(), filterCriteria);

    // Apply search query if provided
    if (filterCriteria.getSearchQuery() != null
        && !filterCriteria.getSearchQuery().trim().isEmpty()) {
      filteredTests = searchTestResults(filteredTests, filterCriteria.getSearchQuery());
    }

    // Apply sorting
    filteredTests = sortTestResults(filteredTests, filterCriteria.getSortCriteria());

    // Apply pagination
    final PaginationResult paginationResult =
        paginateResults(filteredTests, filterCriteria.getPagination());

    final long filterTime = System.currentTimeMillis() - startTime;

    // Create filter result with metadata
    return new FilterResult(
        paginationResult.getResults(),
        paginationResult.getTotalCount(),
        paginationResult.getPageCount(),
        filterCriteria,
        filterTime,
        createFilterMetadata(report, filteredTests, filterCriteria));
  }

  /**
   * Filters test results based on the provided criteria.
   *
   * @param testResults the test results to filter
   * @param criteria the filtering criteria
   * @return filtered test results
   */
  private List<TestComparisonResult> filterTestResults(
      final List<TestComparisonResult> testResults, final FilterCriteria criteria) {

    return testResults.stream()
        .filter(createStatusFilter(criteria.getStatusFilter()))
        .filter(createRuntimeFilter(criteria.getRuntimeFilter()))
        .filter(createSeverityFilter(criteria.getSeverityFilter()))
        .filter(createTimeRangeFilter(criteria.getTimeRangeFilter()))
        .filter(createCriticalIssuesFilter(criteria.isOnlyCriticalIssues()))
        .filter(createCustomFilter(criteria.getCustomFilters()))
        .collect(Collectors.toList());
  }

  /**
   * Creates a status filter predicate.
   *
   * @param statusFilter the status filter configuration
   * @return predicate for filtering by status
   */
  private Predicate<TestComparisonResult> createStatusFilter(final StatusFilter statusFilter) {
    if (statusFilter == null || statusFilter.getIncludedStatuses().isEmpty()) {
      return test -> true; // No filter
    }

    return test -> statusFilter.getIncludedStatuses().contains(test.getOverallStatus());
  }

  /**
   * Creates a runtime filter predicate.
   *
   * @param runtimeFilter the runtime filter configuration
   * @return predicate for filtering by runtime
   */
  private Predicate<TestComparisonResult> createRuntimeFilter(final RuntimeFilter runtimeFilter) {
    if (runtimeFilter == null || runtimeFilter.getIncludedRuntimes().isEmpty()) {
      return test -> true; // No filter
    }

    return test -> {
      final Collection<RuntimeType> testRuntimes = test.getRuntimeResults().keySet();
      return switch (runtimeFilter.getMatchMode()) {
        case ANY -> testRuntimes.stream().anyMatch(runtimeFilter.getIncludedRuntimes()::contains);
        case ALL -> testRuntimes.containsAll(runtimeFilter.getIncludedRuntimes());
        case EXACT -> testRuntimes.equals(runtimeFilter.getIncludedRuntimes());
      };
    };
  }

  /**
   * Creates a severity filter predicate.
   *
   * @param severityFilter the severity filter configuration
   * @return predicate for filtering by discrepancy severity
   */
  private Predicate<TestComparisonResult> createSeverityFilter(
      final SeverityFilter severityFilter) {
    if (severityFilter == null || severityFilter.getIncludedSeverities().isEmpty()) {
      return test -> true; // No filter
    }

    return test ->
        test.getDiscrepancies().stream()
            .anyMatch(
                discrepancy ->
                    severityFilter
                        .getIncludedSeverities()
                        .contains(discrepancy.getSeverity().toString()));
  }

  /**
   * Creates a time range filter predicate.
   *
   * @param timeRangeFilter the time range filter configuration
   * @return predicate for filtering by time range
   */
  private Predicate<TestComparisonResult> createTimeRangeFilter(
      final TimeRangeFilter timeRangeFilter) {
    if (timeRangeFilter == null) {
      return test -> true; // No filter
    }

    return test -> {
      // For this example, we'll filter based on discrepancy detection time
      // In a real implementation, this would be based on test execution time
      return test.getDiscrepancies().stream()
          .anyMatch(
              discrepancy -> {
                final Instant detectedAt = discrepancy.getDetectedAt();
                return (timeRangeFilter.getStartTime() == null
                        || !detectedAt.isBefore(timeRangeFilter.getStartTime()))
                    && (timeRangeFilter.getEndTime() == null
                        || !detectedAt.isAfter(timeRangeFilter.getEndTime()));
              });
    };
  }

  /**
   * Creates a critical issues filter predicate.
   *
   * @param onlyCriticalIssues whether to show only tests with critical issues
   * @return predicate for filtering by critical issues
   */
  private Predicate<TestComparisonResult> createCriticalIssuesFilter(
      final boolean onlyCriticalIssues) {
    if (!onlyCriticalIssues) {
      return test -> true; // No filter
    }

    return TestComparisonResult::hasCriticalIssues;
  }

  /**
   * Creates a custom filter predicate from map-based criteria.
   *
   * @param customFilters the custom filter criteria
   * @return predicate for custom filtering
   */
  private Predicate<TestComparisonResult> createCustomFilter(
      final Map<String, Object> customFilters) {
    if (customFilters == null || customFilters.isEmpty()) {
      return test -> true; // No filter
    }

    return test -> {
      // Example custom filters
      if (customFilters.containsKey("minExecutionTime")) {
        final long minTime = ((Number) customFilters.get("minExecutionTime")).longValue();
        final boolean hasMinTime =
            test.getRuntimeResults().values().stream()
                .anyMatch(result -> result.getExecutionTime().toMillis() >= minTime);
        if (!hasMinTime) {
          return false;
        }
      }

      if (customFilters.containsKey("hasOutput")) {
        final boolean requiresOutput = (Boolean) customFilters.get("hasOutput");
        final boolean hasOutput =
            test.getRuntimeResults().values().stream()
                .anyMatch(result -> !result.getOutput().trim().isEmpty());
        if (requiresOutput && !hasOutput) {
          return false;
        }
      }

      return true;
    };
  }

  /**
   * Performs full-text search on test results.
   *
   * @param testResults the test results to search
   * @param searchQuery the search query
   * @return filtered test results matching the search query
   */
  private List<TestComparisonResult> searchTestResults(
      final List<TestComparisonResult> testResults, final String searchQuery) {

    final SearchQueryParser parser = new SearchQueryParser(searchQuery, configuration);
    final List<SearchTerm> searchTerms = parser.parseQuery();

    return testResults.stream()
        .filter(test -> matchesSearchTerms(test, searchTerms))
        .collect(Collectors.toList());
  }

  /**
   * Checks if a test result matches the search terms.
   *
   * @param test the test result to check
   * @param searchTerms the parsed search terms
   * @return true if the test matches the search criteria
   */
  private boolean matchesSearchTerms(
      final TestComparisonResult test, final List<SearchTerm> searchTerms) {
    for (final SearchTerm term : searchTerms) {
      if (!matchesSearchTerm(test, term)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if a test result matches a single search term.
   *
   * @param test the test result to check
   * @param term the search term
   * @return true if the test matches the search term
   */
  private boolean matchesSearchTerm(final TestComparisonResult test, final SearchTerm term) {
    final String searchText = term.getText().toLowerCase();

    return switch (term.getField()) {
      case "name", "testName" -> test.getTestName().toLowerCase().contains(searchText);
      case "status" -> test.getOverallStatus().toString().toLowerCase().contains(searchText);
      case "output" -> test.getRuntimeResults().values().stream()
          .anyMatch(result -> result.getOutput().toLowerCase().contains(searchText));
      case "error" -> test.getRuntimeResults().values().stream()
          .anyMatch(result -> result.getErrorMessage().toLowerCase().contains(searchText));
      case "discrepancy" -> test.getDiscrepancies().stream()
          .anyMatch(
              disc ->
                  disc.getDescription().toLowerCase().contains(searchText)
                      || disc.getDetails().toLowerCase().contains(searchText));
      case "all", "*" -> matchesAnyField(test, searchText);
      default -> false;
    };
  }

  /**
   * Checks if a test result matches search text in any field.
   *
   * @param test the test result to check
   * @param searchText the search text
   * @return true if any field contains the search text
   */
  private boolean matchesAnyField(final TestComparisonResult test, final String searchText) {
    // Search in test name
    if (test.getTestName().toLowerCase().contains(searchText)) {
      return true;
    }

    // Search in status
    if (test.getOverallStatus().toString().toLowerCase().contains(searchText)) {
      return true;
    }

    // Search in runtime outputs
    if (test.getRuntimeResults().values().stream()
        .anyMatch(
            result ->
                result.getOutput().toLowerCase().contains(searchText)
                    || result.getErrorMessage().toLowerCase().contains(searchText))) {
      return true;
    }

    // Search in discrepancies
    return test.getDiscrepancies().stream()
        .anyMatch(
            disc ->
                disc.getDescription().toLowerCase().contains(searchText)
                    || disc.getDetails().toLowerCase().contains(searchText));
  }

  /**
   * Sorts test results based on the sort criteria.
   *
   * @param testResults the test results to sort
   * @param sortCriteria the sort criteria
   * @return sorted test results
   */
  private List<TestComparisonResult> sortTestResults(
      final List<TestComparisonResult> testResults, final SortCriteria sortCriteria) {

    if (sortCriteria == null) {
      return testResults;
    }

    return testResults.stream()
        .sorted(
            (a, b) -> {
              int result =
                  switch (sortCriteria.getSortField()) {
                  case "name" -> a.getTestName().compareTo(b.getTestName());
                  case "status" -> a.getOverallStatus().compareTo(b.getOverallStatus());
                  case "critical" -> Boolean.compare(
                      b.hasCriticalIssues(), a.hasCriticalIssues());
                  case "discrepancies" -> Integer.compare(
                      b.getDiscrepancies().size(), a.getDiscrepancies().size());
                  case "executionTime" -> {
                    final long avgTimeA =
                        (long)
                            a.getRuntimeResults().values().stream()
                                .mapToLong(r -> r.getExecutionTime().toMillis())
                                .average()
                                .orElse(0.0);
                    final long avgTimeB =
                        (long)
                            b.getRuntimeResults().values().stream()
                                .mapToLong(r -> r.getExecutionTime().toMillis())
                                .average()
                                .orElse(0.0);
                    yield Long.compare(avgTimeA, avgTimeB);
                  }
                  default -> 0;
                  };

              return sortCriteria.getSortDirection() == SortDirection.DESCENDING ? -result : result;
            })
        .collect(Collectors.toList());
  }

  /**
   * Applies pagination to the filtered results.
   *
   * @param testResults the filtered test results
   * @param pagination the pagination configuration
   * @return paginated results with metadata
   */
  private PaginationResult paginateResults(
      final List<TestComparisonResult> testResults, final PaginationConfig pagination) {

    if (pagination == null) {
      return new PaginationResult(testResults, testResults.size(), 1);
    }

    final int totalCount = testResults.size();
    final int pageSize = pagination.getPageSize();
    final int pageNumber = pagination.getPageNumber();
    final int totalPages = (totalCount + pageSize - 1) / pageSize;

    final int startIndex = pageNumber * pageSize;
    final int endIndex = Math.min(startIndex + pageSize, totalCount);

    final List<TestComparisonResult> pageResults =
        startIndex < totalCount ? testResults.subList(startIndex, endIndex) : List.of();

    return new PaginationResult(pageResults, totalCount, totalPages);
  }

  /**
   * Creates metadata about the filtering operation.
   *
   * @param originalReport the original report
   * @param filteredTests the filtered test results
   * @param criteria the filter criteria used
   * @return filter metadata
   */
  private Map<String, Object> createFilterMetadata(
      final ComparisonReport originalReport,
      final List<TestComparisonResult> filteredTests,
      final FilterCriteria criteria) {

    final Map<String, Object> metadata = new HashMap<>();

    metadata.put("originalTestCount", originalReport.getTestResults().size());
    metadata.put("filteredTestCount", filteredTests.size());
    metadata.put("filterReduction", originalReport.getTestResults().size() - filteredTests.size());

    // Status distribution in filtered results
    final Map<String, Long> statusDistribution =
        filteredTests.stream()
            .collect(
                Collectors.groupingBy(
                    test -> test.getOverallStatus().toString(), Collectors.counting()));
    metadata.put("statusDistribution", statusDistribution);

    // Runtime distribution in filtered results
    final Map<String, Long> runtimeDistribution =
        filteredTests.stream()
            .flatMap(test -> test.getRuntimeResults().keySet().stream())
            .collect(Collectors.groupingBy(RuntimeType::toString, Collectors.counting()));
    metadata.put("runtimeDistribution", runtimeDistribution);

    // Critical issues count
    final long criticalIssuesCount =
        filteredTests.stream().mapToLong(test -> test.hasCriticalIssues() ? 1 : 0).sum();
    metadata.put("criticalIssuesCount", criticalIssuesCount);

    // Applied filters summary
    metadata.put("appliedFilters", createAppliedFiltersSummary(criteria));

    return metadata;
  }

  /**
   * Creates a summary of applied filters.
   *
   * @param criteria the filter criteria
   * @return summary of applied filters
   */
  private Map<String, Object> createAppliedFiltersSummary(final FilterCriteria criteria) {
    final Map<String, Object> summary = new HashMap<>();

    if (criteria.getStatusFilter() != null
        && !criteria.getStatusFilter().getIncludedStatuses().isEmpty()) {
      summary.put("statusFilter", criteria.getStatusFilter().getIncludedStatuses());
    }

    if (criteria.getRuntimeFilter() != null
        && !criteria.getRuntimeFilter().getIncludedRuntimes().isEmpty()) {
      summary.put("runtimeFilter", criteria.getRuntimeFilter().getIncludedRuntimes());
    }

    if (criteria.getSeverityFilter() != null
        && !criteria.getSeverityFilter().getIncludedSeverities().isEmpty()) {
      summary.put("severityFilter", criteria.getSeverityFilter().getIncludedSeverities());
    }

    if (criteria.getTimeRangeFilter() != null) {
      summary.put(
          "timeRangeFilter",
          Map.of(
              "start", criteria.getTimeRangeFilter().getStartTime(),
              "end", criteria.getTimeRangeFilter().getEndTime()));
    }

    if (criteria.isOnlyCriticalIssues()) {
      summary.put("onlyCriticalIssues", true);
    }

    if (criteria.getSearchQuery() != null && !criteria.getSearchQuery().trim().isEmpty()) {
      summary.put("searchQuery", criteria.getSearchQuery());
    }

    if (criteria.getCustomFilters() != null && !criteria.getCustomFilters().isEmpty()) {
      summary.put("customFilters", criteria.getCustomFilters());
    }

    return summary;
  }

  /**
   * Gets the current search configuration.
   *
   * @return search configuration
   */
  public SearchConfiguration getConfiguration() {
    return configuration;
  }
}
