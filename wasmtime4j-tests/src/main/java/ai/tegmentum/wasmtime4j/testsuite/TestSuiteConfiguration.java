package ai.tegmentum.wasmtime4j.testsuite;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Set;

/**
 * Configuration for WebAssembly test suite execution. Provides comprehensive configuration options
 * for test discovery, execution, and reporting.
 */
public final class TestSuiteConfiguration {

  private final boolean officialTestsEnabled;
  private final boolean wasmtimeTestsEnabled;
  private final boolean customTestsEnabled;
  private final Set<TestRuntime> enabledRuntimes;
  private final int maxConcurrentTests;
  private final int testTimeoutMinutes;
  private final boolean crossRuntimeComparisonEnabled;
  private final boolean performanceAnalysisEnabled;
  private final boolean coverageAnalysisEnabled;
  private final boolean consoleReportEnabled;
  private final boolean htmlReportEnabled;
  private final boolean jsonReportEnabled;
  private final boolean xmlReportEnabled;
  private final Path testSuiteBaseDirectory;
  private final Path outputDirectory;
  private final TestFilterConfiguration testFilters;
  private final RegressionDetectionConfiguration regressionConfig;

  private TestSuiteConfiguration(final Builder builder) {
    this.officialTestsEnabled = builder.officialTestsEnabled;
    this.wasmtimeTestsEnabled = builder.wasmtimeTestsEnabled;
    this.customTestsEnabled = builder.customTestsEnabled;
    this.enabledRuntimes = Set.copyOf(builder.enabledRuntimes);
    this.maxConcurrentTests = builder.maxConcurrentTests;
    this.testTimeoutMinutes = builder.testTimeoutMinutes;
    this.crossRuntimeComparisonEnabled = builder.crossRuntimeComparisonEnabled;
    this.performanceAnalysisEnabled = builder.performanceAnalysisEnabled;
    this.coverageAnalysisEnabled = builder.coverageAnalysisEnabled;
    this.consoleReportEnabled = builder.consoleReportEnabled;
    this.htmlReportEnabled = builder.htmlReportEnabled;
    this.jsonReportEnabled = builder.jsonReportEnabled;
    this.xmlReportEnabled = builder.xmlReportEnabled;
    this.testSuiteBaseDirectory = builder.testSuiteBaseDirectory;
    this.outputDirectory = builder.outputDirectory;
    this.testFilters = builder.testFilters;
    this.regressionConfig = builder.regressionConfig;
  }

  // Getters
  public boolean isOfficialTestsEnabled() {
    return officialTestsEnabled;
  }

  public boolean isWasmtimeTestsEnabled() {
    return wasmtimeTestsEnabled;
  }

  public boolean isCustomTestsEnabled() {
    return customTestsEnabled;
  }

  public Set<TestRuntime> getEnabledRuntimes() {
    return enabledRuntimes;
  }

  public int getMaxConcurrentTests() {
    return maxConcurrentTests;
  }

  public int getTestTimeoutMinutes() {
    return testTimeoutMinutes;
  }

  public boolean isCrossRuntimeComparisonEnabled() {
    return crossRuntimeComparisonEnabled;
  }

  public boolean isPerformanceAnalysisEnabled() {
    return performanceAnalysisEnabled;
  }

  public boolean isCoverageAnalysisEnabled() {
    return coverageAnalysisEnabled;
  }

  public boolean isConsoleReportEnabled() {
    return consoleReportEnabled;
  }

  public boolean isHtmlReportEnabled() {
    return htmlReportEnabled;
  }

  public boolean isJsonReportEnabled() {
    return jsonReportEnabled;
  }

  public boolean isXmlReportEnabled() {
    return xmlReportEnabled;
  }

  public Path getTestSuiteBaseDirectory() {
    return testSuiteBaseDirectory;
  }

  public Path getOutputDirectory() {
    return outputDirectory;
  }

  public TestFilterConfiguration getTestFilters() {
    return testFilters;
  }

  public RegressionDetectionConfiguration getRegressionConfig() {
    return regressionConfig;
  }

  /**
   * Creates a new builder for test suite configuration.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for TestSuiteConfiguration. */
  public static final class Builder {
    private boolean officialTestsEnabled = true;
    private boolean wasmtimeTestsEnabled = true;
    private boolean customTestsEnabled = true;
    private Set<TestRuntime> enabledRuntimes = EnumSet.allOf(TestRuntime.class);
    private int maxConcurrentTests = Runtime.getRuntime().availableProcessors();
    private int testTimeoutMinutes = 30;
    private boolean crossRuntimeComparisonEnabled = true;
    private boolean performanceAnalysisEnabled = true;
    private boolean coverageAnalysisEnabled = true;
    private boolean consoleReportEnabled = true;
    private boolean htmlReportEnabled = true;
    private boolean jsonReportEnabled = true;
    private boolean xmlReportEnabled = true;
    private Path testSuiteBaseDirectory = Paths.get("target/test-classes/wasm");
    private Path outputDirectory = Paths.get("target/test-reports");
    private TestFilterConfiguration testFilters;
    private RegressionDetectionConfiguration regressionConfig;

    public Builder enableOfficialTests(final boolean enabled) {
      this.officialTestsEnabled = enabled;
      return this;
    }

    public Builder enableWasmtimeTests(final boolean enabled) {
      this.wasmtimeTestsEnabled = enabled;
      return this;
    }

    public Builder enableCustomTests(final boolean enabled) {
      this.customTestsEnabled = enabled;
      return this;
    }

    public Builder enabledRuntimes(final Set<TestRuntime> runtimes) {
      if (runtimes == null || runtimes.isEmpty()) {
        throw new IllegalArgumentException("Enabled runtimes cannot be null or empty");
      }
      this.enabledRuntimes = EnumSet.copyOf(runtimes);
      return this;
    }

    public Builder maxConcurrentTests(final int maxConcurrentTests) {
      if (maxConcurrentTests < 1) {
        throw new IllegalArgumentException("Max concurrent tests must be at least 1");
      }
      this.maxConcurrentTests = maxConcurrentTests;
      return this;
    }

    public Builder testTimeoutMinutes(final int timeoutMinutes) {
      if (timeoutMinutes < 1) {
        throw new IllegalArgumentException("Test timeout must be at least 1 minute");
      }
      this.testTimeoutMinutes = timeoutMinutes;
      return this;
    }

    public Builder enableCrossRuntimeComparison(final boolean enabled) {
      this.crossRuntimeComparisonEnabled = enabled;
      return this;
    }

    public Builder enablePerformanceAnalysis(final boolean enabled) {
      this.performanceAnalysisEnabled = enabled;
      return this;
    }

    public Builder enableCoverageAnalysis(final boolean enabled) {
      this.coverageAnalysisEnabled = enabled;
      return this;
    }

    public Builder enableConsoleReport(final boolean enabled) {
      this.consoleReportEnabled = enabled;
      return this;
    }

    public Builder enableHtmlReport(final boolean enabled) {
      this.htmlReportEnabled = enabled;
      return this;
    }

    public Builder enableJsonReport(final boolean enabled) {
      this.jsonReportEnabled = enabled;
      return this;
    }

    public Builder enableXmlReport(final boolean enabled) {
      this.xmlReportEnabled = enabled;
      return this;
    }

    public Builder testSuiteBaseDirectory(final Path directory) {
      if (directory == null) {
        throw new IllegalArgumentException("Test suite base directory cannot be null");
      }
      this.testSuiteBaseDirectory = directory;
      return this;
    }

    public Builder outputDirectory(final Path directory) {
      if (directory == null) {
        throw new IllegalArgumentException("Output directory cannot be null");
      }
      this.outputDirectory = directory;
      return this;
    }

    public Builder testFilters(final TestFilterConfiguration filters) {
      this.testFilters = filters;
      return this;
    }

    public Builder regressionConfig(final RegressionDetectionConfiguration config) {
      this.regressionConfig = config;
      return this;
    }

    public TestSuiteConfiguration build() {
      // Initialize default configurations if not provided
      if (testFilters == null) {
        testFilters = TestFilterConfiguration.builder().build();
      }
      if (regressionConfig == null) {
        regressionConfig = RegressionDetectionConfiguration.defaultConfig();
      }

      return new TestSuiteConfiguration(this);
    }
  }
}
