package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Simple search query parser.
 *
 * @since 1.0.0
 */
public final class SearchQueryParser {
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
