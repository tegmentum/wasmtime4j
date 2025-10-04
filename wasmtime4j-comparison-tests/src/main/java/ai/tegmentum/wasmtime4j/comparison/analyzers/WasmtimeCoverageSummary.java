package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.List;

/**
 * Summary of Wasmtime coverage analysis for quick assessment and reporting.
 *
 * @since 1.0.0
 */
public final class WasmtimeCoverageSummary {
  private final String overallStatus;
  private final double actualCoveragePercentage;
  private final double actualCompatibilityScore;
  private final boolean meetsTargets;
  private final List<String> keyFindings;
  private final List<String> recommendations;

  /**
   * Creates a coverage summary with key metrics and findings.
   *
   * @param overallStatus overall status assessment
   * @param actualCoveragePercentage actual coverage percentage achieved
   * @param actualCompatibilityScore actual compatibility score achieved
   * @param meetsTargets true if coverage targets are met
   * @param keyFindings list of key findings from analysis
   * @param recommendations list of recommendations for improvement
   */
  public WasmtimeCoverageSummary(
      final String overallStatus,
      final double actualCoveragePercentage,
      final double actualCompatibilityScore,
      final boolean meetsTargets,
      final List<String> keyFindings,
      final List<String> recommendations) {
    this.overallStatus = overallStatus;
    this.actualCoveragePercentage = actualCoveragePercentage;
    this.actualCompatibilityScore = actualCompatibilityScore;
    this.meetsTargets = meetsTargets;
    this.keyFindings = List.copyOf(keyFindings);
    this.recommendations = List.copyOf(recommendations);
  }

  public String getOverallStatus() {
    return overallStatus;
  }

  public double getActualCoveragePercentage() {
    return actualCoveragePercentage;
  }

  public double getActualCompatibilityScore() {
    return actualCompatibilityScore;
  }

  public boolean meetsTargets() {
    return meetsTargets;
  }

  public List<String> getKeyFindings() {
    return keyFindings;
  }

  public List<String> getRecommendations() {
    return recommendations;
  }

  public boolean isCompliant() {
    return "COMPLIANT".equals(overallStatus) || "FULLY_COMPATIBLE".equals(overallStatus);
  }

  /**
   * Gets a detailed description of the current status.
   *
   * @return human-readable status description
   */
  public String getStatusDescription() {
    return switch (overallStatus) {
      case "COMPLIANT", "FULLY_COMPATIBLE" -> "All targets met - full compliance achieved";
      case "COVERAGE_ACHIEVED" -> "95% coverage target met, working on compatibility";
      case "COMPATIBILITY_ACHIEVED" -> "100% compatibility achieved, working on coverage";
      case "NEEDS_IMPROVEMENT" -> "Both coverage and compatibility need improvement";
      default -> "Status assessment in progress";
    };
  }
}
