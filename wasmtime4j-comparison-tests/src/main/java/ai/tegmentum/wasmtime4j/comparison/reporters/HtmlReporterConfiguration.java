package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/**
 * Configuration for HTML report generation.
 *
 * @since 1.0.0
 */
public final class HtmlReporterConfiguration {
  private final String reportTitle;
  private final String theme;
  private final boolean includeStaticResources;
  private final boolean enableInteractiveFeatures;
  private final boolean enablePerformanceCharts;
  private final boolean enableCoverageAnalysis;
  private final VerbosityLevel verbosityLevel;

  private HtmlReporterConfiguration(final Builder builder) {
    this.reportTitle = Objects.requireNonNull(builder.reportTitle, "reportTitle cannot be null");
    this.theme = Objects.requireNonNull(builder.theme, "theme cannot be null");
    this.includeStaticResources = builder.includeStaticResources;
    this.enableInteractiveFeatures = builder.enableInteractiveFeatures;
    this.enablePerformanceCharts = builder.enablePerformanceCharts;
    this.enableCoverageAnalysis = builder.enableCoverageAnalysis;
    this.verbosityLevel =
        Objects.requireNonNull(builder.verbosityLevel, "verbosityLevel cannot be null");
  }

  public String getReportTitle() {
    return reportTitle;
  }

  public String getTheme() {
    return theme;
  }

  public boolean includeStaticResources() {
    return includeStaticResources;
  }

  public boolean isInteractiveFeaturesEnabled() {
    return enableInteractiveFeatures;
  }

  public boolean isPerformanceChartsEnabled() {
    return enablePerformanceCharts;
  }

  public boolean isCoverageAnalysisEnabled() {
    return enableCoverageAnalysis;
  }

  public VerbosityLevel getVerbosityLevel() {
    return verbosityLevel;
  }

  /** Creates a new builder with default configuration. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for HtmlReporterConfiguration. */
  public static final class Builder {
    private String reportTitle = "Wasmtime4j Comparison Report";
    private String theme = "default";
    private boolean includeStaticResources = true;
    private boolean enableInteractiveFeatures = true;
    private boolean enablePerformanceCharts = true;
    private boolean enableCoverageAnalysis = true;
    private VerbosityLevel verbosityLevel = VerbosityLevel.NORMAL;

    public Builder reportTitle(final String reportTitle) {
      this.reportTitle = Objects.requireNonNull(reportTitle, "reportTitle cannot be null");
      return this;
    }

    public Builder theme(final String theme) {
      this.theme = Objects.requireNonNull(theme, "theme cannot be null");
      return this;
    }

    public Builder includeStaticResources(final boolean includeStaticResources) {
      this.includeStaticResources = includeStaticResources;
      return this;
    }

    public Builder enableInteractiveFeatures(final boolean enableInteractiveFeatures) {
      this.enableInteractiveFeatures = enableInteractiveFeatures;
      return this;
    }

    public Builder enablePerformanceCharts(final boolean enablePerformanceCharts) {
      this.enablePerformanceCharts = enablePerformanceCharts;
      return this;
    }

    public Builder enableCoverageAnalysis(final boolean enableCoverageAnalysis) {
      this.enableCoverageAnalysis = enableCoverageAnalysis;
      return this;
    }

    public Builder verbosityLevel(final VerbosityLevel verbosityLevel) {
      this.verbosityLevel = Objects.requireNonNull(verbosityLevel, "verbosityLevel cannot be null");
      return this;
    }

    public HtmlReporterConfiguration build() {
      return new HtmlReporterConfiguration(this);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final HtmlReporterConfiguration that = (HtmlReporterConfiguration) obj;
    return includeStaticResources == that.includeStaticResources
        && enableInteractiveFeatures == that.enableInteractiveFeatures
        && enablePerformanceCharts == that.enablePerformanceCharts
        && enableCoverageAnalysis == that.enableCoverageAnalysis
        && Objects.equals(reportTitle, that.reportTitle)
        && Objects.equals(theme, that.theme)
        && verbosityLevel == that.verbosityLevel;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        reportTitle,
        theme,
        includeStaticResources,
        enableInteractiveFeatures,
        enablePerformanceCharts,
        enableCoverageAnalysis,
        verbosityLevel);
  }

  @Override
  public String toString() {
    return "HtmlReporterConfiguration{"
        + "reportTitle='"
        + reportTitle
        + '\''
        + ", theme='"
        + theme
        + '\''
        + ", interactive="
        + enableInteractiveFeatures
        + ", verbosity="
        + verbosityLevel
        + '}';
  }
}
