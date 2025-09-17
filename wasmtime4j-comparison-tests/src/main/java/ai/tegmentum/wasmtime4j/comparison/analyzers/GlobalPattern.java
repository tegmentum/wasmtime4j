package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;

/**
 * Global pattern identified across multiple tests.
 *
 * @since 1.0.0
 */
public final class GlobalPattern {
  private final GlobalPatternType type;
  private final String patternId;
  private final String description;
  private final int occurrenceCount;
  private final double frequency;

  /**
   * Constructs a new GlobalPattern with the specified properties.
   *
   * @param type the pattern type
   * @param patternId the pattern identifier
   * @param description the pattern description
   * @param occurrenceCount the number of occurrences
   * @param frequency the frequency of occurrence
   */
  public GlobalPattern(
      final GlobalPatternType type,
      final String patternId,
      final String description,
      final int occurrenceCount,
      final double frequency) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.patternId = Objects.requireNonNull(patternId, "patternId cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.occurrenceCount = occurrenceCount;
    this.frequency = frequency;
  }

  public GlobalPatternType getType() {
    return type;
  }

  public String getPatternId() {
    return patternId;
  }

  public String getDescription() {
    return description;
  }

  public int getOccurrenceCount() {
    return occurrenceCount;
  }

  public double getFrequency() {
    return frequency;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final GlobalPattern that = (GlobalPattern) obj;
    return occurrenceCount == that.occurrenceCount
        && Double.compare(that.frequency, frequency) == 0
        && type == that.type
        && Objects.equals(patternId, that.patternId)
        && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, patternId, description, occurrenceCount, frequency);
  }

  @Override
  public String toString() {
    return "GlobalPattern{"
        + "type="
        + type
        + ", id='"
        + patternId
        + '\''
        + ", frequency="
        + String.format("%.1f%%", frequency * 100)
        + '}';
  }
}
