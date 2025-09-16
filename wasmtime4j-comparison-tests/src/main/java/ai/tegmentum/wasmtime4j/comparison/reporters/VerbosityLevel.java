package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * Verbosity levels for controlling the amount of output in console reporting. Each level includes
 * all output from lower levels plus additional details.
 *
 * @since 1.0.0
 */
public enum VerbosityLevel {
  /** Minimal output - only critical errors and final results. */
  QUIET(0, "Quiet"),

  /** Standard output - summary information and important warnings. */
  NORMAL(1, "Normal"),

  /** Detailed output - progress updates and detailed analysis results. */
  VERBOSE(2, "Verbose"),

  /** Debug output - all available information including internal processing details. */
  DEBUG(3, "Debug");

  private final int level;
  private final String displayName;

  VerbosityLevel(final int level, final String displayName) {
    this.level = level;
    this.displayName = displayName;
  }

  /**
   * Gets the numeric level for comparison purposes.
   *
   * @return numeric level (0-3)
   */
  public int getLevel() {
    return level;
  }

  /**
   * Gets the display name for this verbosity level.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if this level includes output from the specified level.
   *
   * @param other the level to check
   * @return true if this level includes the other level's output
   */
  public boolean includes(final VerbosityLevel other) {
    return this.level >= other.level;
  }

  /**
   * Parses a verbosity level from a string representation.
   *
   * @param value the string value to parse
   * @return the corresponding verbosity level
   * @throws IllegalArgumentException if the value is not recognized
   */
  public static VerbosityLevel fromString(final String value) {
    if (value == null || value.trim().isEmpty()) {
      return NORMAL;
    }

    final String normalized = value.trim().toLowerCase();
    return switch (normalized) {
      case "quiet", "q", "0" -> QUIET;
      case "normal", "n", "1" -> NORMAL;
      case "verbose", "v", "2" -> VERBOSE;
      case "debug", "d", "3" -> DEBUG;
      default -> throw new IllegalArgumentException("Unknown verbosity level: " + value);
    };
  }
}
