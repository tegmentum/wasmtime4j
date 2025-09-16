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

    public Builder detectedFeatures(final Set<String> detectedFeatures) {
      this.detectedFeatures =
          Objects.requireNonNull(detectedFeatures, "detectedFeatures cannot be null");
      return this;
    }

    public Builder runtimeFeatureCoverage(
        final Map<RuntimeType, Set<String>> runtimeFeatureCoverage) {
      this.runtimeFeatureCoverage =
          Objects.requireNonNull(runtimeFeatureCoverage, "runtimeFeatureCoverage cannot be null");
      return this;
    }

    public Builder coverageMetrics(final CoverageMetrics coverageMetrics) {
      this.coverageMetrics =
          Objects.requireNonNull(coverageMetrics, "coverageMetrics cannot be null");
      return this;
    }

    public Builder coverageGaps(final List<CoverageGap> coverageGaps) {
      this.coverageGaps = Objects.requireNonNull(coverageGaps, "coverageGaps cannot be null");
      return this;
    }

    public Builder featureInteractionAnalysis(
        final FeatureInteractionAnalysis featureInteractionAnalysis) {
      this.featureInteractionAnalysis =
          Objects.requireNonNull(
              featureInteractionAnalysis, "featureInteractionAnalysis cannot be null");
      return this;
    }

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

/** Metrics related to feature coverage analysis. */
final class CoverageMetrics {
  private final int totalDetectedFeatures;
  private final int coveredFeatures;
  private final double overallCoveragePercentage;
  private final Map<RuntimeType, Double> runtimeCoveragePercentages;
  private final double successRate;

  public CoverageMetrics(
      final int totalDetectedFeatures,
      final int coveredFeatures,
      final double overallCoveragePercentage,
      final Map<RuntimeType, Double> runtimeCoveragePercentages,
      final double successRate) {
    this.totalDetectedFeatures = totalDetectedFeatures;
    this.coveredFeatures = coveredFeatures;
    this.overallCoveragePercentage = overallCoveragePercentage;
    this.runtimeCoveragePercentages = Map.copyOf(runtimeCoveragePercentages);
    this.successRate = successRate;
  }

  public int getTotalDetectedFeatures() {
    return totalDetectedFeatures;
  }

  public int getCoveredFeatures() {
    return coveredFeatures;
  }

  public double getOverallCoveragePercentage() {
    return overallCoveragePercentage;
  }

  public Map<RuntimeType, Double> getRuntimeCoveragePercentages() {
    return runtimeCoveragePercentages;
  }

  public double getSuccessRate() {
    return successRate;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CoverageMetrics that = (CoverageMetrics) obj;
    return totalDetectedFeatures == that.totalDetectedFeatures
        && coveredFeatures == that.coveredFeatures
        && Double.compare(that.overallCoveragePercentage, overallCoveragePercentage) == 0
        && Double.compare(that.successRate, successRate) == 0
        && Objects.equals(runtimeCoveragePercentages, that.runtimeCoveragePercentages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        totalDetectedFeatures,
        coveredFeatures,
        overallCoveragePercentage,
        runtimeCoveragePercentages,
        successRate);
  }

  @Override
  public String toString() {
    return "CoverageMetrics{"
        + "total="
        + totalDetectedFeatures
        + ", covered="
        + coveredFeatures
        + ", overall="
        + String.format("%.1f%%", overallCoveragePercentage)
        + ", success="
        + String.format("%.1f%%", successRate)
        + '}';
  }
}

/** Represents a gap in feature coverage. */
final class CoverageGap {
  private final CoverageGapType type;
  private final String description;
  private final Set<String> affectedFeatures;
  private final Set<RuntimeType> affectedRuntimes;
  private final GapSeverity severity;

  public CoverageGap(
      final CoverageGapType type,
      final String description,
      final Set<String> affectedFeatures,
      final Set<RuntimeType> affectedRuntimes,
      final GapSeverity severity) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.affectedFeatures = Set.copyOf(affectedFeatures);
    this.affectedRuntimes = Set.copyOf(affectedRuntimes);
    this.severity = Objects.requireNonNull(severity, "severity cannot be null");
  }

  public CoverageGapType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public Set<String> getAffectedFeatures() {
    return affectedFeatures;
  }

  public Set<RuntimeType> getAffectedRuntimes() {
    return affectedRuntimes;
  }

  public GapSeverity getSeverity() {
    return severity;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CoverageGap that = (CoverageGap) obj;
    return type == that.type
        && Objects.equals(description, that.description)
        && Objects.equals(affectedFeatures, that.affectedFeatures)
        && Objects.equals(affectedRuntimes, that.affectedRuntimes)
        && severity == that.severity;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, description, affectedFeatures, affectedRuntimes, severity);
  }

  @Override
  public String toString() {
    return "CoverageGap{"
        + "type="
        + type
        + ", severity="
        + severity
        + ", features="
        + affectedFeatures.size()
        + ", runtimes="
        + affectedRuntimes.size()
        + '}';
  }
}

/** Types of coverage gaps. */
enum CoverageGapType {
  /** A runtime is missing from testing. */
  RUNTIME_MISSING,

  /** Feature coverage is incomplete for a runtime. */
  FEATURE_INCOMPLETE,

  /** An entire category of features is untested. */
  CATEGORY_UNTESTED,

  /** Feature interaction testing is insufficient. */
  INTERACTION_INCOMPLETE
}

/** Severity levels for coverage gaps. */
enum GapSeverity {
  /** Low severity gap that should be addressed eventually. */
  LOW,

  /** Medium severity gap that should be addressed soon. */
  MEDIUM,

  /** High severity gap that requires immediate attention. */
  HIGH
}

/** Analysis of feature interactions and combinations. */
final class FeatureInteractionAnalysis {
  private final Map<String, Set<String>> featureCombinations;
  private final List<String> problematicInteractions;
  private final double interactionComplexity;

  public FeatureInteractionAnalysis(
      final Map<String, Set<String>> featureCombinations,
      final List<String> problematicInteractions,
      final double interactionComplexity) {
    this.featureCombinations = Map.copyOf(featureCombinations);
    this.problematicInteractions = List.copyOf(problematicInteractions);
    this.interactionComplexity = interactionComplexity;
  }

  public Map<String, Set<String>> getFeatureCombinations() {
    return featureCombinations;
  }

  public List<String> getProblematicInteractions() {
    return problematicInteractions;
  }

  public double getInteractionComplexity() {
    return interactionComplexity;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final FeatureInteractionAnalysis that = (FeatureInteractionAnalysis) obj;
    return Double.compare(that.interactionComplexity, interactionComplexity) == 0
        && Objects.equals(featureCombinations, that.featureCombinations)
        && Objects.equals(problematicInteractions, that.problematicInteractions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(featureCombinations, problematicInteractions, interactionComplexity);
  }

  @Override
  public String toString() {
    return "FeatureInteractionAnalysis{"
        + "combinations="
        + featureCombinations.size()
        + ", problematic="
        + problematicInteractions.size()
        + ", complexity="
        + String.format("%.2f", interactionComplexity)
        + '}';
  }
}
