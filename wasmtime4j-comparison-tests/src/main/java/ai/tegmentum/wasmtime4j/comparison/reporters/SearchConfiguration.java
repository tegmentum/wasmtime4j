package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;
import java.util.Set;

/**
 * Configuration for search operations.
 *
 * @since 1.0.0
 */
public final class SearchConfiguration {
  private final boolean caseSensitive;
  private final boolean useRegex;
  private final Set<String> searchFields;
  private final int maxResults;
  private final boolean highlightMatches;

  public SearchConfiguration(
      final boolean caseSensitive,
      final boolean useRegex,
      final Set<String> searchFields,
      final int maxResults,
      final boolean highlightMatches) {
    this.caseSensitive = caseSensitive;
    this.useRegex = useRegex;
    this.searchFields = Set.copyOf(Objects.requireNonNull(searchFields, "searchFields cannot be null"));
    this.maxResults = maxResults;
    this.highlightMatches = highlightMatches;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public boolean isUseRegex() {
    return useRegex;
  }

  public Set<String> getSearchFields() {
    return searchFields;
  }

  public int getMaxResults() {
    return maxResults;
  }

  public boolean isHighlightMatches() {
    return highlightMatches;
  }

  /** Creates a default search configuration. */
  public static SearchConfiguration defaultConfiguration() {
    return new SearchConfiguration(
        false, // case sensitive
        false, // use regex
        Set.of("testName", "description", "error"), // search fields
        100, // max results
        true // highlight matches
    );
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final SearchConfiguration that = (SearchConfiguration) obj;
    return caseSensitive == that.caseSensitive
        && useRegex == that.useRegex
        && maxResults == that.maxResults
        && highlightMatches == that.highlightMatches
        && Objects.equals(searchFields, that.searchFields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caseSensitive, useRegex, searchFields, maxResults, highlightMatches);
  }

  @Override
  public String toString() {
    return "SearchConfiguration{"
        + "caseSensitive="
        + caseSensitive
        + ", useRegex="
        + useRegex
        + ", fields="
        + searchFields.size()
        + ", maxResults="
        + maxResults
        + ", highlight="
        + highlightMatches
        + '}';
  }
}