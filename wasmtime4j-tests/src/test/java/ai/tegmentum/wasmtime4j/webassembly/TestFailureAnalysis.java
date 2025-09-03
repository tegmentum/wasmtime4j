package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Detailed analysis of a single test failure, providing categorization, root cause analysis,
 * and actionable debugging recommendations.
 */
public final class TestFailureAnalysis {
  private final String testName;
  private final RuntimeType runtimeType;
  private final WasmTestFailureAnalyzer.FailureCategory category;
  private final String summary;
  private final String recommendation;
  private final String exceptionType;
  private final String exceptionMessage;
  private final String stackTrace;
  private final Duration executionTime;
  private final Instant analysisTime;

  private TestFailureAnalysis(final Builder builder) {
    this.testName = builder.testName;
    this.runtimeType = builder.runtimeType;
    this.category = builder.category;
    this.summary = builder.summary;
    this.recommendation = builder.recommendation;
    this.exceptionType = builder.exceptionType;
    this.exceptionMessage = builder.exceptionMessage;
    this.stackTrace = builder.stackTrace;
    this.executionTime = builder.executionTime;
    this.analysisTime = Instant.now();
  }

  /**
   * Gets the test name that failed.
   *
   * @return the test name
   */
  public String getTestName() {
    return testName;
  }

  /**
   * Gets the runtime type where the failure occurred.
   *
   * @return the runtime type
   */
  public RuntimeType getRuntimeType() {
    return runtimeType;
  }

  /**
   * Gets the failure category.
   *
   * @return the failure category
   */
  public WasmTestFailureAnalyzer.FailureCategory getCategory() {
    return category;
  }

  /**
   * Gets a brief summary of the failure.
   *
   * @return the failure summary
   */
  public String getSummary() {
    return summary;
  }

  /**
   * Gets the recommended action to resolve the failure.
   *
   * @return the recommendation
   */
  public String getRecommendation() {
    return recommendation;
  }

  /**
   * Gets the exception type if available.
   *
   * @return the exception type, or empty if not available
   */
  public Optional<String> getExceptionType() {
    return Optional.ofNullable(exceptionType);
  }

  /**
   * Gets the exception message if available.
   *
   * @return the exception message, or empty if not available
   */
  public Optional<String> getExceptionMessage() {
    return Optional.ofNullable(exceptionMessage);
  }

  /**
   * Gets the full stack trace if available.
   *
   * @return the stack trace, or empty if not available
   */
  public Optional<String> getStackTrace() {
    return Optional.ofNullable(stackTrace);
  }

  /**
   * Gets the execution time before failure.
   *
   * @return the execution time
   */
  public Duration getExecutionTime() {
    return executionTime;
  }

  /**
   * Gets the time when this analysis was performed.
   *
   * @return the analysis time
   */
  public Instant getAnalysisTime() {
    return analysisTime;
  }

  /**
   * Checks if this is a critical failure that requires immediate attention.
   *
   * @return true if the failure is critical
   */
  public boolean isCritical() {
    return category == WasmTestFailureAnalyzer.FailureCategory.NATIVE_ERROR ||
           category == WasmTestFailureAnalyzer.FailureCategory.MEMORY_ERROR ||
           category == WasmTestFailureAnalyzer.FailureCategory.CONFIGURATION_ERROR;
  }

  /**
   * Checks if this failure is likely due to a test environment issue.
   *
   * @return true if the failure is environment-related
   */
  public boolean isEnvironmentRelated() {
    return category == WasmTestFailureAnalyzer.FailureCategory.TIMEOUT ||
           category == WasmTestFailureAnalyzer.FailureCategory.NATIVE_ERROR ||
           category == WasmTestFailureAnalyzer.FailureCategory.CONFIGURATION_ERROR;
  }

  /**
   * Checks if this failure is related to the WebAssembly module itself.
   *
   * @return true if the failure is module-related
   */
  public boolean isModuleRelated() {
    return category == WasmTestFailureAnalyzer.FailureCategory.COMPILATION_ERROR ||
           category == WasmTestFailureAnalyzer.FailureCategory.VALIDATION_ERROR ||
           category == WasmTestFailureAnalyzer.FailureCategory.INSTANTIATION_ERROR;
  }

  /**
   * Creates a formatted report of this failure analysis.
   *
   * @return a formatted report
   */
  public String createReport() {
    final StringBuilder report = new StringBuilder();
    report.append("Test Failure Analysis\n");
    report.append("====================\n\n");
    
    report.append(String.format("Test Name: %s\n", testName));
    report.append(String.format("Runtime: %s\n", runtimeType.name()));
    report.append(String.format("Category: %s\n", category.getDescription()));
    report.append(String.format("Summary: %s\n", summary));
    report.append(String.format("Recommendation: %s\n", recommendation));
    report.append(String.format("Execution Time: %.3fs\n", executionTime.toMillis() / 1000.0));
    report.append(String.format("Analysis Time: %s\n\n", analysisTime));

    getExceptionType().ifPresent(type -> report.append(String.format("Exception Type: %s\n", type)));
    getExceptionMessage().ifPresent(message -> report.append(String.format("Exception Message: %s\n", message)));
    
    getStackTrace().ifPresent(stack -> {
      report.append("\nStack Trace:\n");
      report.append("------------\n");
      report.append(stack);
    });

    return report.toString();
  }

  /**
   * Creates a brief summary suitable for logging or console output.
   *
   * @return a brief summary
   */
  public String createBriefSummary() {
    return String.format("[%s] %s: %s (%s)", 
        runtimeType.name(), 
        testName, 
        category.getDescription(),
        summary);
  }

  @Override
  public String toString() {
    return String.format("TestFailureAnalysis{test=%s, runtime=%s, category=%s}", 
        testName, runtimeType.name(), category.name());
  }

  /**
   * Builder for TestFailureAnalysis.
   */
  public static final class Builder {
    private final String testName;
    private RuntimeType runtimeType;
    private WasmTestFailureAnalyzer.FailureCategory category = WasmTestFailureAnalyzer.FailureCategory.UNKNOWN;
    private String summary = "Unknown failure";
    private String recommendation = "No specific recommendation available";
    private String exceptionType;
    private String exceptionMessage;
    private String stackTrace;
    private Duration executionTime = Duration.ZERO;

    /**
     * Creates a builder for the specified test name.
     *
     * @param testName the test name
     */
    public Builder(final String testName) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    }

    /**
     * Sets the runtime type.
     *
     * @param runtimeType the runtime type
     * @return this builder
     */
    public Builder runtimeType(final RuntimeType runtimeType) {
      this.runtimeType = runtimeType;
      return this;
    }

    /**
     * Sets the failure category.
     *
     * @param category the failure category
     * @return this builder
     */
    public Builder category(final WasmTestFailureAnalyzer.FailureCategory category) {
      this.category = Objects.requireNonNull(category, "category cannot be null");
      return this;
    }

    /**
     * Sets the failure summary.
     *
     * @param summary the failure summary
     * @return this builder
     */
    public Builder summary(final String summary) {
      this.summary = Objects.requireNonNull(summary, "summary cannot be null");
      return this;
    }

    /**
     * Sets the failure recommendation.
     *
     * @param recommendation the failure recommendation
     * @return this builder
     */
    public Builder recommendation(final String recommendation) {
      this.recommendation = Objects.requireNonNull(recommendation, "recommendation cannot be null");
      return this;
    }

    /**
     * Sets the exception type.
     *
     * @param exceptionType the exception type
     * @return this builder
     */
    public Builder exceptionType(final String exceptionType) {
      this.exceptionType = exceptionType;
      return this;
    }

    /**
     * Sets the exception message.
     *
     * @param exceptionMessage the exception message
     * @return this builder
     */
    public Builder exceptionMessage(final String exceptionMessage) {
      this.exceptionMessage = exceptionMessage;
      return this;
    }

    /**
     * Sets the stack trace.
     *
     * @param stackTrace the stack trace
     * @return this builder
     */
    public Builder stackTrace(final String stackTrace) {
      this.stackTrace = stackTrace;
      return this;
    }

    /**
     * Sets the execution time.
     *
     * @param executionTime the execution time
     * @return this builder
     */
    public Builder executionTime(final Duration executionTime) {
      this.executionTime = Objects.requireNonNull(executionTime, "executionTime cannot be null");
      return this;
    }

    /**
     * Builds the test failure analysis.
     *
     * @return the test failure analysis
     */
    public TestFailureAnalysis build() {
      Objects.requireNonNull(runtimeType, "runtimeType must be set");
      return new TestFailureAnalysis(this);
    }
  }
}