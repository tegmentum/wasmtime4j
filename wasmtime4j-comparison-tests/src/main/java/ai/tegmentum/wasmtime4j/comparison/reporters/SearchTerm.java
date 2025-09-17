package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/**
 * Search term representation.
 *
 * @since 1.0.0
 */
public final class SearchTerm {
  private final String field;
  private final String text;
  private final boolean isRegex;
  private final boolean isNegated;

  /**
   * Creates a new search term.
   *
   * @param field the field to search in
   * @param text the search text
   * @param isRegex whether the text is a regex pattern
   * @param isNegated whether this is a negated search
   */
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
