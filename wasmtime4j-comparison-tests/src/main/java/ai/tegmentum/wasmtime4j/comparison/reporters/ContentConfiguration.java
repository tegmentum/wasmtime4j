package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for report content inclusion controlling which sections and data are included.
 *
 * @since 1.0.0
 */
public final class ContentConfiguration {
  private final boolean includeSummary;
  private final boolean includeMetadata;
  private final boolean includeBehavioralAnalysis;
  private final boolean includePerformanceAnalysis;
  private final boolean includeCoverageAnalysis;
  private final boolean includeRecommendations;
  private final boolean includeDetailedResults;
  private final boolean includeRawData;
  private final Set<String> excludedTests;
  private final Set<String> includedSections;
  private final int maxTestResults;
  private final RecommendationLevel recommendationLevel;

  private ContentConfiguration(final Builder builder) {
    this.includeSummary = builder.includeSummary;
    this.includeMetadata = builder.includeMetadata;
    this.includeBehavioralAnalysis = builder.includeBehavioralAnalysis;
    this.includePerformanceAnalysis = builder.includePerformanceAnalysis;
    this.includeCoverageAnalysis = builder.includeCoverageAnalysis;
    this.includeRecommendations = builder.includeRecommendations;
    this.includeDetailedResults = builder.includeDetailedResults;
    this.includeRawData = builder.includeRawData;
    this.excludedTests = Set.copyOf(builder.excludedTests);
    this.includedSections = Set.copyOf(builder.includedSections);
    this.maxTestResults = builder.maxTestResults;
    this.recommendationLevel = builder.recommendationLevel;
  }

  public boolean isIncludeSummary() {
    return includeSummary;
  }

  public boolean isIncludeMetadata() {
    return includeMetadata;
  }

  public boolean isIncludeBehavioralAnalysis() {
    return includeBehavioralAnalysis;
  }

  public boolean isIncludePerformanceAnalysis() {
    return includePerformanceAnalysis;
  }

  public boolean isIncludeCoverageAnalysis() {
    return includeCoverageAnalysis;
  }

  public boolean isIncludeRecommendations() {
    return includeRecommendations;
  }

  public boolean isIncludeDetailedResults() {
    return includeDetailedResults;
  }

  public boolean isIncludeRawData() {
    return includeRawData;
  }

  public Set<String> getExcludedTests() {
    return excludedTests;
  }

  public Set<String> getIncludedSections() {
    return includedSections;
  }

  public int getMaxTestResults() {
    return maxTestResults;
  }

  public RecommendationLevel getRecommendationLevel() {
    return recommendationLevel;
  }

  /**
   * Checks if a test should be included in the report.
   *
   * @param testName the test name
   * @return true if test should be included
   */
  public boolean shouldIncludeTest(final String testName) {
    return !excludedTests.contains(testName);
  }

  /**
   * Checks if a section should be included in the report.
   *
   * @param sectionName the section name
   * @return true if section should be included
   */
  public boolean shouldIncludeSection(final String sectionName) {
    return includedSections.isEmpty() || includedSections.contains(sectionName);
  }

  /** Creates default content configuration. */
  public static ContentConfiguration defaultContentConfig() {
    return new Builder()
        .includeSummary(true)
        .includeMetadata(true)
        .includeBehavioralAnalysis(true)
        .includePerformanceAnalysis(true)
        .includeCoverageAnalysis(true)
        .includeRecommendations(true)
        .includeDetailedResults(true)
        .includeRawData(false)
        .maxTestResults(1000)
        .recommendationLevel(RecommendationLevel.MEDIUM)
        .build();
  }

  /** Creates minimal content configuration. */
  public static ContentConfiguration minimalContentConfig() {
    return new Builder()
        .includeSummary(true)
        .includeMetadata(false)
        .includeBehavioralAnalysis(false)
        .includePerformanceAnalysis(false)
        .includeCoverageAnalysis(false)
        .includeRecommendations(true)
        .includeDetailedResults(false)
        .includeRawData(false)
        .maxTestResults(100)
        .recommendationLevel(RecommendationLevel.HIGH)
        .build();
  }

  /** Creates comprehensive content configuration. */
  public static ContentConfiguration comprehensiveContentConfig() {
    return new Builder()
        .includeSummary(true)
        .includeMetadata(true)
        .includeBehavioralAnalysis(true)
        .includePerformanceAnalysis(true)
        .includeCoverageAnalysis(true)
        .includeRecommendations(true)
        .includeDetailedResults(true)
        .includeRawData(true)
        .maxTestResults(Integer.MAX_VALUE)
        .recommendationLevel(RecommendationLevel.ALL)
        .build();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ContentConfiguration that = (ContentConfiguration) obj;
    return includeSummary == that.includeSummary
        && includeMetadata == that.includeMetadata
        && includeBehavioralAnalysis == that.includeBehavioralAnalysis
        && includePerformanceAnalysis == that.includePerformanceAnalysis
        && includeCoverageAnalysis == that.includeCoverageAnalysis
        && includeRecommendations == that.includeRecommendations
        && includeDetailedResults == that.includeDetailedResults
        && includeRawData == that.includeRawData
        && maxTestResults == that.maxTestResults
        && Objects.equals(excludedTests, that.excludedTests)
        && Objects.equals(includedSections, that.includedSections)
        && recommendationLevel == that.recommendationLevel;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        includeSummary,
        includeMetadata,
        includeBehavioralAnalysis,
        includePerformanceAnalysis,
        includeCoverageAnalysis,
        includeRecommendations,
        includeDetailedResults,
        includeRawData,
        excludedTests,
        includedSections,
        maxTestResults,
        recommendationLevel);
  }

  @Override
  public String toString() {
    return "ContentConfiguration{"
        + "summary="
        + includeSummary
        + ", behavioral="
        + includeBehavioralAnalysis
        + ", performance="
        + includePerformanceAnalysis
        + ", coverage="
        + includeCoverageAnalysis
        + ", recommendations="
        + includeRecommendations
        + ", maxResults="
        + maxTestResults
        + '}';
  }

  /** Builder for ContentConfiguration. */
  public static final class Builder {
    private boolean includeSummary = true;
    private boolean includeMetadata = true;
    private boolean includeBehavioralAnalysis = true;
    private boolean includePerformanceAnalysis = true;
    private boolean includeCoverageAnalysis = true;
    private boolean includeRecommendations = true;
    private boolean includeDetailedResults = true;
    private boolean includeRawData = false;
    private Set<String> excludedTests = Collections.emptySet();
    private Set<String> includedSections = Collections.emptySet();
    private int maxTestResults = 1000;
    private RecommendationLevel recommendationLevel = RecommendationLevel.MEDIUM;

    public Builder includeSummary(final boolean includeSummary) {
      this.includeSummary = includeSummary;
      return this;
    }

    public Builder includeMetadata(final boolean includeMetadata) {
      this.includeMetadata = includeMetadata;
      return this;
    }

    public Builder includeBehavioralAnalysis(final boolean includeBehavioralAnalysis) {
      this.includeBehavioralAnalysis = includeBehavioralAnalysis;
      return this;
    }

    public Builder includePerformanceAnalysis(final boolean includePerformanceAnalysis) {
      this.includePerformanceAnalysis = includePerformanceAnalysis;
      return this;
    }

    public Builder includeCoverageAnalysis(final boolean includeCoverageAnalysis) {
      this.includeCoverageAnalysis = includeCoverageAnalysis;
      return this;
    }

    public Builder includeRecommendations(final boolean includeRecommendations) {
      this.includeRecommendations = includeRecommendations;
      return this;
    }

    public Builder includeDetailedResults(final boolean includeDetailedResults) {
      this.includeDetailedResults = includeDetailedResults;
      return this;
    }

    public Builder includeRawData(final boolean includeRawData) {
      this.includeRawData = includeRawData;
      return this;
    }

    public Builder excludedTests(final Set<String> excludedTests) {
      this.excludedTests = Objects.requireNonNull(excludedTests, "excludedTests cannot be null");
      return this;
    }

    /**
     * Sets the included sections.
     *
     * @param includedSections the sections to include
     * @return this builder
     */
    public Builder includedSections(final Set<String> includedSections) {
      this.includedSections =
          Objects.requireNonNull(includedSections, "includedSections cannot be null");
      return this;
    }

    /**
     * Sets the maximum number of test results.
     *
     * @param maxTestResults the maximum test results
     * @return this builder
     */
    public Builder maxTestResults(final int maxTestResults) {
      if (maxTestResults <= 0) {
        throw new IllegalArgumentException("maxTestResults must be positive");
      }
      this.maxTestResults = maxTestResults;
      return this;
    }

    /**
     * Sets the recommendation level for the report configuration.
     *
     * @param recommendationLevel the recommendation level to set
     * @return this builder instance
     */
    public Builder recommendationLevel(final RecommendationLevel recommendationLevel) {
      this.recommendationLevel =
          Objects.requireNonNull(recommendationLevel, "recommendationLevel cannot be null");
      return this;
    }

    public ContentConfiguration build() {
      return new ContentConfiguration(this);
    }
  }
}
