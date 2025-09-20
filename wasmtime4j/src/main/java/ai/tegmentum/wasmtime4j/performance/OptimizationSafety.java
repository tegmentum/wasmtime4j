package ai.tegmentum.wasmtime4j.performance;

/**
 * Safety levels for optimization strategies.
 *
 * <p>Optimization safety indicates the risk level and thoroughness of analysis required before
 * applying an optimization strategy. Higher safety levels require more careful consideration and
 * testing.
 *
 * @since 1.0.0
 */
public enum OptimizationSafety {

  /**
   * Safe optimizations with minimal risk.
   *
   * <p>These optimizations are well-established, thoroughly tested, and unlikely to introduce
   * correctness issues. They can be applied with confidence.
   */
  SAFE("Safe"),

  /**
   * Moderate risk optimizations requiring some analysis.
   *
   * <p>These optimizations are generally safe but may require analysis of specific code patterns or
   * usage scenarios to ensure correctness.
   */
  MODERATE("Moderate"),

  /**
   * Aggressive optimizations with higher risk.
   *
   * <p>These optimizations can provide significant performance benefits but require careful
   * analysis, extensive testing, and may have edge cases or limitations.
   */
  AGGRESSIVE("Aggressive"),

  /**
   * Experimental optimizations with unknown risk.
   *
   * <p>These optimizations are experimental, unproven, or highly specialized. They should only be
   * used with thorough testing and understanding of potential risks.
   */
  EXPERIMENTAL("Experimental");

  private final String displayName;

  OptimizationSafety(final String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets the human-readable display name for this safety level.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if this safety level is considered safe for general use.
   *
   * @return true if this safety level is safe or moderate
   */
  public boolean isGenerallySafe() {
    return this == SAFE || this == MODERATE;
  }

  /**
   * Checks if this safety level requires extensive testing.
   *
   * @return true if this safety level requires extensive testing
   */
  public boolean requiresExtensiveTesting() {
    return this == AGGRESSIVE || this == EXPERIMENTAL;
  }

  /**
   * Gets the minimum confidence level required for applying optimizations at this safety level.
   *
   * @return confidence threshold (0.0 to 1.0)
   */
  public double getRequiredConfidenceLevel() {
    switch (this) {
      case SAFE:
        return 0.7;
      case MODERATE:
        return 0.8;
      case AGGRESSIVE:
        return 0.9;
      case EXPERIMENTAL:
        return 0.95;
      default:
        return 0.8;
    }
  }

  /**
   * Gets the recommended testing level for this safety category.
   *
   * @return testing level description
   */
  public String getRecommendedTestingLevel() {
    switch (this) {
      case SAFE:
        return "Basic regression testing";
      case MODERATE:
        return "Moderate testing with edge case coverage";
      case AGGRESSIVE:
        return "Extensive testing with performance validation";
      case EXPERIMENTAL:
        return "Comprehensive testing with careful monitoring";
      default:
        return "Standard testing";
    }
  }

  /**
   * Gets the risk level associated with this safety category.
   *
   * @return risk level (1 = lowest risk, 4 = highest risk)
   */
  public int getRiskLevel() {
    switch (this) {
      case SAFE:
        return 1;
      case MODERATE:
        return 2;
      case AGGRESSIVE:
        return 3;
      case EXPERIMENTAL:
        return 4;
      default:
        return 2;
    }
  }
}
