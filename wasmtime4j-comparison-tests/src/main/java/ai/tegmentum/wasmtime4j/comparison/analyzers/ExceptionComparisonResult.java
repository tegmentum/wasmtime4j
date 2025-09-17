package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;

/**
 * Result of comparing exceptions between two executions.
 *
 * @since 1.0.0
 */
public final class ExceptionComparisonResult {
  private final boolean typeMatch;
  private final boolean messageMatch;
  private final boolean semanticMatch;
  private final double score;

  /**
   * Constructs a new ExceptionComparisonResult with the specified comparison data.
   *
   * @param typeMatch whether the exception types match
   * @param messageMatch whether the exception messages match
   * @param semanticMatch whether the exceptions are semantically equivalent
   * @param score the comparison score
   */
  public ExceptionComparisonResult(
      final boolean typeMatch,
      final boolean messageMatch,
      final boolean semanticMatch,
      final double score) {
    this.typeMatch = typeMatch;
    this.messageMatch = messageMatch;
    this.semanticMatch = semanticMatch;
    this.score = Math.max(0.0, Math.min(1.0, score));
  }

  public boolean isTypeMatch() {
    return typeMatch;
  }

  public boolean isMessageMatch() {
    return messageMatch;
  }

  public boolean isSemanticMatch() {
    return semanticMatch;
  }

  public double getScore() {
    return score;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ExceptionComparisonResult that = (ExceptionComparisonResult) obj;
    return typeMatch == that.typeMatch
        && messageMatch == that.messageMatch
        && semanticMatch == that.semanticMatch
        && Double.compare(that.score, score) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(typeMatch, messageMatch, semanticMatch, score);
  }

  @Override
  public String toString() {
    return "ExceptionComparisonResult{"
        + "score="
        + String.format("%.2f", score)
        + ", typeMatch="
        + typeMatch
        + ", messageMatch="
        + messageMatch
        + ", semanticMatch="
        + semanticMatch
        + '}';
  }
}
