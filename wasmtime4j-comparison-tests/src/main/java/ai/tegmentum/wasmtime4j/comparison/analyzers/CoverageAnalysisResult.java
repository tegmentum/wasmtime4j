package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Comprehensive result of coverage analysis for a specific test, including feature detection,
 * runtime coverage mapping, gap analysis, and feature interaction assessment.
 *
 * @since 1.0.0
 */
public final class CoverageAnalysisResult {
  private final String testName;
  private final Set<String> detectedFeatures;
  private final Map<RuntimeType, Set<String>> runtimeFeatureCoverage;
  private final CoverageMetrics coverageMetrics;
  private final List<CoverageGap> coverageGaps;
  private final FeatureInteractionAnalysis featureInteractionAnalysis;
  private final Instant analysisTime;

  private CoverageAnalysisResult(final Builder builder) {
    this.testName = Objects.requireNonNull(builder.testName, "testName cannot be null");
    this.detectedFeatures = Set.copyOf(builder.detectedFeatures);
    this.runtimeFeatureCoverage = Map.copyOf(builder.runtimeFeatureCoverage);
    this.coverageMetrics =
        Objects.requireNonNull(builder.coverageMetrics, "coverageMetrics cannot be null");
    this.coverageGaps = List.copyOf(builder.coverageGaps);
    this.featureInteractionAnalysis =
        Objects.requireNonNull(
            builder.featureInteractionAnalysis, "featureInteractionAnalysis cannot be null");
    this.analysisTime = Instant.now();
  }

  public String getTestName() {
    return testName;
  }

  public Set<String> getDetectedFeatures() {
    return detectedFeatures;
  }

  public Map<RuntimeType, Set<String>> getRuntimeFeatureCoverage() {
    return runtimeFeatureCoverage;
  }

  public CoverageMetrics getCoverageMetrics() {
    return coverageMetrics;
  }

  public List<CoverageGap> getCoverageGaps() {
    return coverageGaps;
  }

  public FeatureInteractionAnalysis getFeatureInteractionAnalysis() {
    return featureInteractionAnalysis;
  }

  public Instant getAnalysisTime() {
    return analysisTime;
  }

  /**
   * Checks if the test has comprehensive coverage across all runtimes.
   *
   * @return true if coverage is comprehensive
   */
  public boolean hasComprehensiveCoverage() {
    return coverageMetrics.getOverallCoveragePercentage() >= 90.0
        && coverageGaps.stream().noneMatch(gap -> gap.getSeverity() == GapSeverity.HIGH);
  }

  /**
   * Gets the number of high-severity coverage gaps.
   *
   * @return count of high-severity gaps
   */
  public long getHighSeverityGapCount() {
    return coverageGaps.stream().filter(gap -> gap.getSeverity() == GapSeverity.HIGH).count();
  }

  /**
   * Gets a summary of the coverage analysis.
   *
   * @return formatted summary
   */
  public String getCoverageSummary() {
    return String.format(
        "Coverage Analysis for %s:%n"
            + "Features Detected: %d%n"
            + "Overall Coverage: %.1f%%%n"
            + "Coverage Gaps: %d (High: %d, Medium: %d, Low: %d)%n"
            + "Feature Interactions: %d combinations analyzed%n"
            + "Analysis Time: %s",
        testName,
        detectedFeatures.size(),
        coverageMetrics.getOverallCoveragePercentage(),
        coverageGaps.size(),
        getHighSeverityGapCount(),
        coverageGaps.stream()
            .mapToLong(gap -> gap.getSeverity() == GapSeverity.MEDIUM ? 1 : 0)
            .sum(),
        coverageGaps.stream().mapToLong(gap -> gap.getSeverity() == GapSeverity.LOW ? 1 : 0).sum(),
        featureInteractionAnalysis.getFeatureCombinations().size(),
        analysisTime);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CoverageAnalysisResult that = (CoverageAnalysisResult) obj;
    return Objects.equals(testName, that.testName)
        && Objects.equals(detectedFeatures, that.detectedFeatures)
        && Objects.equals(runtimeFeatureCoverage, that.runtimeFeatureCoverage)
        && Objects.equals(coverageMetrics, that.coverageMetrics)
        && Objects.equals(coverageGaps, that.coverageGaps)
        && Objects.equals(featureInteractionAnalysis, that.featureInteractionAnalysis);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        testName,
        detectedFeatures,
        runtimeFeatureCoverage,
        coverageMetrics,
        coverageGaps,
        featureInteractionAnalysis);
  }

  @Override
  public String toString() {
    return "CoverageAnalysisResult{"
        + "testName='"
        + testName
        + '\''
        + ", features="
        + detectedFeatures.size()
        + ", coverage="
        + String.format("%.1f%%", coverageMetrics.getOverallCoveragePercentage())
        + ", gaps="
        + coverageGaps.size()
        + '}';
  }

  /**
   * Gets the overall coverage score as a percentage.
   *
   * @return coverage score (0.0 to 100.0)
   */
  public double getCoverageScore() {
    return coverageMetrics.getOverallCoveragePercentage();
  }

  /**
   * Gets the number of features implemented.
   *
   * @return number of implemented features
   */
  public int getFeaturesImplemented() {
    return detectedFeatures.size();
  }

  /**
   * Gets the total number of features available.
   *
   * @return total feature count
   */
  public int getTotalFeatures() {
    return getFeaturesImplemented() + coverageGaps.size();
  }

  /**
   * Gets the list of missing features.
   *
   * @return list of missing feature names
   */
  public List<String> getMissingFeatures() {
    return coverageGaps.stream()
        .flatMap(gap -> gap.getAffectedFeatures().stream())
        .distinct()
        .toList();
  }

  /**
   * Gets the overall coverage percentage.
   *
   * @return coverage percentage (0.0 to 100.0)
   */
  public double getOverallCoveragePercentage() {
    return coverageMetrics.getOverallCoveragePercentage();
  }

  /** Builder for CoverageAnalysisResult. */
  public static final class Builder {
    private final String testName;
    private Set<String> detectedFeatures = Collections.emptySet();
    private Map<RuntimeType, Set<String>> runtimeFeatureCoverage = Collections.emptyMap();
    private CoverageMetrics coverageMetrics;
    private List<CoverageGap> coverageGaps = Collections.emptyList();
    private FeatureInteractionAnalysis featureInteractionAnalysis;

    public Builder(final String testName) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    }

    /**
     * Sets the detected features for this coverage analysis.
     *
     * @param detectedFeatures set of detected features
     * @return this builder
     */
    public Builder detectedFeatures(final Set<String> detectedFeatures) {
      this.detectedFeatures =
          Objects.requireNonNull(detectedFeatures, "detectedFeatures cannot be null");
      return this;
    }

    /**
     * Sets the runtime feature coverage mapping.
     *
     * @param runtimeFeatureCoverage mapping of runtime types to their covered features
     * @return this builder
     */
    public Builder runtimeFeatureCoverage(
        final Map<RuntimeType, Set<String>> runtimeFeatureCoverage) {
      this.runtimeFeatureCoverage =
          Objects.requireNonNull(runtimeFeatureCoverage, "runtimeFeatureCoverage cannot be null");
      return this;
    }

    /**
     * Sets the coverage metrics for this analysis result.
     *
     * @param coverageMetrics coverage metrics
     * @return this builder
     */
    public Builder coverageMetrics(final CoverageMetrics coverageMetrics) {
      this.coverageMetrics =
          Objects.requireNonNull(coverageMetrics, "coverageMetrics cannot be null");
      return this;
    }

    public Builder coverageGaps(final List<CoverageGap> coverageGaps) {
      this.coverageGaps = Objects.requireNonNull(coverageGaps, "coverageGaps cannot be null");
      return this;
    }

    /**
     * Sets the feature interaction analysis.
     *
     * @param featureInteractionAnalysis feature interaction analysis
     * @return this builder
     */
    public Builder featureInteractionAnalysis(
        final FeatureInteractionAnalysis featureInteractionAnalysis) {
      this.featureInteractionAnalysis =
          Objects.requireNonNull(
              featureInteractionAnalysis, "featureInteractionAnalysis cannot be null");
      return this;
    }

    /**
     * Builds the coverage analysis result.
     *
     * @return coverage analysis result
     * @throws IllegalStateException if required fields are not set
     */
    public CoverageAnalysisResult build() {
      if (coverageMetrics == null) {
        throw new IllegalStateException("coverageMetrics must be set");
      }
      if (featureInteractionAnalysis == null) {
        throw new IllegalStateException("featureInteractionAnalysis must be set");
      }
      return new CoverageAnalysisResult(this);
    }
  }
}
