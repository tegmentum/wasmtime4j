package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Results from Wasmtime-specific coverage analysis, extending base coverage analysis with Wasmtime
 * compatibility and feature coverage metrics.
 *
 * @since 1.0.0
 */
public final class WasmtimeCoverageAnalysisResult {
  private final String testName;
  private final WasmTestSuiteLoader.TestSuiteType suiteType;
  private final Set<String> wasmtimeFeatures;
  private final WasmtimeCompatibilityScore compatibilityScore;
  private final WasmtimeCoverageMetrics wasmtimeMetrics;
  private final List<WasmtimeCoverageGap> wasmtimeGaps;
  private final CrossImplementationAnalysis crossImplementationAnalysis;
  private final CoverageAnalysisResult baseCoverageResult;

  private WasmtimeCoverageAnalysisResult(
      final String testName,
      final WasmTestSuiteLoader.TestSuiteType suiteType,
      final Set<String> wasmtimeFeatures,
      final WasmtimeCompatibilityScore compatibilityScore,
      final WasmtimeCoverageMetrics wasmtimeMetrics,
      final List<WasmtimeCoverageGap> wasmtimeGaps,
      final CrossImplementationAnalysis crossImplementationAnalysis,
      final CoverageAnalysisResult baseCoverageResult) {
    this.testName = testName;
    this.suiteType = suiteType;
    this.wasmtimeFeatures = Set.copyOf(wasmtimeFeatures);
    this.compatibilityScore = compatibilityScore;
    this.wasmtimeMetrics = wasmtimeMetrics;
    this.wasmtimeGaps = List.copyOf(wasmtimeGaps);
    this.crossImplementationAnalysis = crossImplementationAnalysis;
    this.baseCoverageResult = baseCoverageResult;
  }

  public String getTestName() {
    return testName;
  }

  public WasmTestSuiteLoader.TestSuiteType getSuiteType() {
    return suiteType;
  }

  public Set<String> getWasmtimeFeatures() {
    return wasmtimeFeatures;
  }

  public WasmtimeCompatibilityScore getCompatibilityScore() {
    return compatibilityScore;
  }

  public WasmtimeCoverageMetrics getWasmtimeMetrics() {
    return wasmtimeMetrics;
  }

  public List<WasmtimeCoverageGap> getWasmtimeGaps() {
    return wasmtimeGaps;
  }

  public CrossImplementationAnalysis getCrossImplementationAnalysis() {
    return crossImplementationAnalysis;
  }

  public CoverageAnalysisResult getBaseCoverageResult() {
    return baseCoverageResult;
  }

  /** Builder for creating WasmtimeCoverageAnalysisResult instances. */
  public static final class Builder {
    private final String testName;
    private final WasmTestSuiteLoader.TestSuiteType suiteType;
    private Set<String> wasmtimeFeatures;
    private WasmtimeCompatibilityScore compatibilityScore;
    private WasmtimeCoverageMetrics wasmtimeMetrics;
    private List<WasmtimeCoverageGap> wasmtimeGaps;
    private CrossImplementationAnalysis crossImplementationAnalysis;
    private CoverageAnalysisResult baseCoverageResult;

    public Builder(final String testName, final WasmTestSuiteLoader.TestSuiteType suiteType) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
      this.suiteType = Objects.requireNonNull(suiteType, "suiteType cannot be null");
    }

    public Builder wasmtimeFeatures(final Set<String> wasmtimeFeatures) {
      this.wasmtimeFeatures = wasmtimeFeatures;
      return this;
    }

    public Builder compatibilityScore(final WasmtimeCompatibilityScore compatibilityScore) {
      this.compatibilityScore = compatibilityScore;
      return this;
    }

    public Builder wasmtimeMetrics(final WasmtimeCoverageMetrics wasmtimeMetrics) {
      this.wasmtimeMetrics = wasmtimeMetrics;
      return this;
    }

    public Builder wasmtimeGaps(final List<WasmtimeCoverageGap> wasmtimeGaps) {
      this.wasmtimeGaps = wasmtimeGaps;
      return this;
    }

    public Builder crossImplementationAnalysis(
        final CrossImplementationAnalysis crossImplementationAnalysis) {
      this.crossImplementationAnalysis = crossImplementationAnalysis;
      return this;
    }

    public Builder baseCoverageResult(final CoverageAnalysisResult baseCoverageResult) {
      this.baseCoverageResult = baseCoverageResult;
      return this;
    }

    /**
     * Builds the coverage analysis result.
     *
     * @return configured coverage analysis result
     */
    public WasmtimeCoverageAnalysisResult build() {
      return new WasmtimeCoverageAnalysisResult(
          testName,
          suiteType,
          wasmtimeFeatures,
          compatibilityScore,
          wasmtimeMetrics,
          wasmtimeGaps,
          crossImplementationAnalysis,
          baseCoverageResult);
    }
  }
}
