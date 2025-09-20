# Analysis Framework API Reference

This document provides a comprehensive API reference for the wasmtime4j comparison test analysis frameworks, covering all classes, interfaces, and methods available for test analysis and validation.

## Overview

The analysis framework consists of several specialized analyzers that provide different types of validation and comparison capabilities:

- **Coverage Analyzers**: Analyze API coverage and feature completeness
- **Performance Analyzers**: Measure and compare performance metrics
- **Behavioral Analyzers**: Validate behavioral consistency across runtimes
- **Compatibility Analyzers**: Assess compatibility with Wasmtime specifications
- **Reporting Analyzers**: Generate reports and visualizations

## Package Structure

```
ai.tegmentum.wasmtime4j.comparison.analyzers
├── Coverage Analysis
│   ├── WasmtimeCoverageIntegrator
│   ├── WasmtimeCoverageAnalyzer
│   ├── CoverageMetrics
│   └── WasmtimeComprehensiveCoverageReport
├── Performance Analysis
│   ├── AdvancedPerformanceAnalyzer
│   ├── PerformanceAnalyzer
│   ├── PerformanceInsight
│   └── OptimizationRecommendation
├── Behavioral Analysis
│   ├── BehavioralAnalyzer
│   ├── ResultComparator
│   ├── DiscrepancyDetector
│   └── ValueComparisonResult
├── Compatibility Analysis
│   ├── WasmtimeRecommendation
│   ├── WasmtimeCompatibilityScore
│   ├── IssueCategory
│   └── IssueSeverity
└── Reporting Integration
    ├── WasiDashboardIntegration
    ├── InsightGenerator
    └── TrendAnalyzer
```

## Core Analysis APIs

### Coverage Analysis

#### WasmtimeCoverageIntegrator

Main entry point for comprehensive Wasmtime coverage analysis.

```java
public final class WasmtimeCoverageIntegrator {

    /**
     * Creates a new WasmtimeCoverageIntegrator with all necessary analyzers.
     */
    public WasmtimeCoverageIntegrator();

    /**
     * Runs comprehensive Wasmtime coverage analysis for all available test suites.
     *
     * @return comprehensive coverage analysis results
     * @throws IOException if test suites cannot be loaded
     */
    public WasmtimeComprehensiveCoverageReport runComprehensiveCoverageAnalysis()
        throws IOException;

    /**
     * Analyzes coverage for a specific test suite.
     *
     * @param suiteType the test suite type to analyze
     * @throws IOException if the test suite cannot be loaded
     */
    public void analyzeTestSuite(WasmTestSuiteLoader.TestSuiteType suiteType)
        throws IOException;

    /**
     * Analyzes coverage for a specific runtime type.
     *
     * @param runtimeType the runtime type to analyze
     * @param testCases the test cases to run
     * @return coverage metrics for the runtime
     */
    public CoverageMetrics analyzeRuntimeCoverage(RuntimeType runtimeType,
                                                 List<WasmTestCase> testCases);
}
```

#### WasmtimeComprehensiveCoverageReport

Comprehensive coverage report with Wasmtime-specific metrics.

```java
public final class WasmtimeComprehensiveCoverageReport {

    /**
     * Gets the category completeness metrics by feature area.
     *
     * @return map of category name to completeness percentage
     */
    public Map<String, Double> getWasmtimeCategoryCompleteness();

    /**
     * Gets the list of uncovered Wasmtime features.
     *
     * @return list of feature names that are not covered
     */
    public List<String> getUncoveredWasmtimeFeatures();

    /**
     * Gets compatibility scores by runtime type.
     *
     * @return map of runtime type to compatibility score (0-100)
     */
    public Map<RuntimeType, Double> getWasmtimeCompatibilityScores();

    /**
     * Gets recommendations for improving Wasmtime compatibility.
     *
     * @return list of actionable recommendations
     */
    public List<WasmtimeRecommendation> getWasmtimeRecommendations();

    /**
     * Gets detailed test suite coverage metrics.
     *
     * @return test suite coverage information
     */
    public WasmtimeTestSuiteCoverage getTestSuiteCoverage();

    /**
     * Checks if the report meets the 95% coverage target.
     *
     * @return true if coverage is 95% or higher
     */
    public boolean meets95PercentTarget();

    /**
     * Checks if all runtimes achieve full compatibility (95%+).
     *
     * @return true if all runtimes are fully compatible
     */
    public boolean isFullyCompatible();
}
```

#### CoverageMetrics

Detailed coverage metrics and analysis results.

```java
public final class CoverageMetrics {

    /**
     * Gets the overall coverage percentage.
     *
     * @return coverage percentage (0.0 to 100.0)
     */
    public double getCoveragePercentage();

    /**
     * Gets coverage metrics by feature category.
     *
     * @return map of category to coverage percentage
     */
    public Map<String, Double> getCoverageByCategory();

    /**
     * Gets the list of covered features.
     *
     * @return list of feature names that are covered
     */
    public List<String> getCoveredFeatures();

    /**
     * Gets the list of uncovered features with gap analysis.
     *
     * @return list of uncovered features with severity assessment
     */
    public List<FeatureGap> getUncoveredFeatures();

    /**
     * Gets the total number of tests executed.
     *
     * @return number of executed tests
     */
    public int getTotalTests();

    /**
     * Gets the number of passed tests.
     *
     * @return number of tests that passed
     */
    public int getPassedTests();

    /**
     * Gets the number of failed tests.
     *
     * @return number of tests that failed
     */
    public int getFailedTests();
}
```

### Performance Analysis

#### AdvancedPerformanceAnalyzer

Advanced performance analysis with trend detection and optimization recommendations.

```java
public final class AdvancedPerformanceAnalyzer {

    /**
     * Analyzes performance across multiple test runs.
     *
     * @param testResults results from multiple test executions
     * @return detailed performance analysis
     */
    public PerformanceAnalysisResult analyzePerformance(List<TestExecutionResult> testResults);

    /**
     * Compares performance between different runtimes.
     *
     * @param jniResults results from JNI runtime
     * @param panamaResults results from Panama runtime
     * @param nativeResults results from native Wasmtime
     * @return comparative performance analysis
     */
    public PerformanceComparisonResult compareRuntimePerformance(
        List<TestExecutionResult> jniResults,
        List<TestExecutionResult> panamaResults,
        List<TestExecutionResult> nativeResults);

    /**
     * Generates optimization recommendations based on performance analysis.
     *
     * @param analysisResult performance analysis results
     * @return list of optimization recommendations
     */
    public List<OptimizationRecommendation> generateOptimizationRecommendations(
        PerformanceAnalysisResult analysisResult);

    /**
     * Detects performance regressions between baseline and current results.
     *
     * @param baselineResults baseline performance results
     * @param currentResults current performance results
     * @return regression analysis results
     */
    public RegressionAnalysisResult detectRegressions(
        List<TestExecutionResult> baselineResults,
        List<TestExecutionResult> currentResults);
}
```

#### PerformanceInsight

Performance insights and trend analysis.

```java
public final class PerformanceInsight {

    /**
     * Gets the type of performance insight.
     *
     * @return insight type (e.g., OPTIMIZATION, REGRESSION, IMPROVEMENT)
     */
    public PerformanceInsightType getType();

    /**
     * Gets the insight message.
     *
     * @return human-readable insight description
     */
    public String getMessage();

    /**
     * Gets the severity of the insight.
     *
     * @return severity level (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public IssueSeverity getSeverity();

    /**
     * Gets associated performance metrics.
     *
     * @return map of metric names to values
     */
    public Map<String, Double> getMetrics();

    /**
     * Gets recommended actions for this insight.
     *
     * @return list of recommended actions
     */
    public List<String> getRecommendedActions();
}
```

### Behavioral Analysis

#### BehavioralAnalyzer

Analyzes behavioral consistency across runtime implementations.

```java
public final class BehavioralAnalyzer {

    /**
     * Analyzes behavioral consistency across all runtimes.
     *
     * @param testResults test results from all runtimes
     * @return behavioral analysis results
     */
    public BehavioralAnalysisResult analyzeBehavior(Map<RuntimeType, List<TestExecutionResult>> testResults);

    /**
     * Compares behavior between two specific runtimes.
     *
     * @param runtime1 first runtime type
     * @param results1 results from first runtime
     * @param runtime2 second runtime type
     * @param results2 results from second runtime
     * @return behavioral comparison results
     */
    public BehavioralComparisonResult compareBehavior(
        RuntimeType runtime1, List<TestExecutionResult> results1,
        RuntimeType runtime2, List<TestExecutionResult> results2);

    /**
     * Validates output consistency across runtimes.
     *
     * @param testCase the test case to validate
     * @param outputs outputs from different runtimes
     * @return consistency validation results
     */
    public ConsistencyValidationResult validateConsistency(
        WasmTestCase testCase, Map<RuntimeType, Object> outputs);
}
```

#### ResultComparator

Detailed comparison of test execution results.

```java
public final class ResultComparator {

    /**
     * Compares two test execution results in detail.
     *
     * @param expected expected result
     * @param actual actual result
     * @return detailed comparison result
     */
    public ComparisonResult compare(TestExecutionResult expected, TestExecutionResult actual);

    /**
     * Compares memory usage between executions.
     *
     * @param baseline baseline memory usage
     * @param current current memory usage
     * @return memory comparison result
     */
    public MemoryComparisonResult compareMemoryUsage(MemoryMetrics baseline, MemoryMetrics current);

    /**
     * Compares execution values with tolerance.
     *
     * @param expected expected value
     * @param actual actual value
     * @param tolerance comparison tolerance
     * @return value comparison result
     */
    public ValueComparisonResult compareValues(Object expected, Object actual, double tolerance);
}
```

### Compatibility Analysis

#### WasmtimeCompatibilityScore

Calculates and tracks compatibility scores with Wasmtime.

```java
public final class WasmtimeCompatibilityScore {

    /**
     * Gets the overall compatibility score.
     *
     * @return compatibility score (0.0 to 100.0)
     */
    public double getOverallScore();

    /**
     * Gets compatibility scores by feature category.
     *
     * @return map of category to compatibility score
     */
    public Map<String, Double> getScoresByCategory();

    /**
     * Gets the runtime type this score applies to.
     *
     * @return runtime type
     */
    public RuntimeType getRuntimeType();

    /**
     * Gets detailed compatibility metrics.
     *
     * @return detailed compatibility information
     */
    public CompatibilityMetrics getDetailedMetrics();

    /**
     * Checks if the score meets the target threshold.
     *
     * @param threshold target threshold (0.0 to 100.0)
     * @return true if score meets or exceeds threshold
     */
    public boolean meetsThreshold(double threshold);
}
```

#### WasmtimeRecommendation

Provides actionable recommendations for improving compatibility.

```java
public final class WasmtimeRecommendation {

    /**
     * Gets the recommendation category.
     *
     * @return category (e.g., API_IMPLEMENTATION, PERFORMANCE, BEHAVIOR)
     */
    public IssueCategory getCategory();

    /**
     * Gets the recommendation priority.
     *
     * @return priority level (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public IssueSeverity getPriority();

    /**
     * Gets the recommendation title.
     *
     * @return brief title describing the recommendation
     */
    public String getTitle();

    /**
     * Gets the detailed recommendation description.
     *
     * @return detailed description of the recommendation
     */
    public String getDescription();

    /**
     * Gets specific action items for implementing the recommendation.
     *
     * @return list of actionable steps
     */
    public List<String> getActionItems();

    /**
     * Gets the estimated effort to implement the recommendation.
     *
     * @return effort estimate (TRIVIAL, LOW, MEDIUM, HIGH, VERY_HIGH)
     */
    public EffortLevel getEstimatedEffort();
}
```

## Reporting Integration APIs

### WasiDashboardIntegration

WASI-specific dashboard integration and reporting.

```java
public final class WasiDashboardIntegration {

    /**
     * Generates WASI-specific dashboard data.
     *
     * @param wasiTestResults results from WASI test execution
     * @return dashboard data for WASI features
     */
    public WasiDashboardData generateDashboardData(List<WasiTestExecutionResult> wasiTestResults);

    /**
     * Creates WASI compatibility report.
     *
     * @param compatibilityResults WASI compatibility analysis results
     * @return formatted WASI compatibility report
     */
    public WasiCompatibilityReport createCompatibilityReport(
        WasiCompatibilityAnalysisResult compatibilityResults);

    /**
     * Integrates WASI results with main dashboard.
     *
     * @param mainDashboard main comparison dashboard
     * @param wasiData WASI-specific data
     * @return integrated dashboard with WASI information
     */
    public IntegratedDashboard integrateWithMainDashboard(
        ComparisonDashboard mainDashboard, WasiDashboardData wasiData);
}
```

## Configuration and Extensibility

### Analysis Configuration

All analyzers support configuration through a common configuration interface:

```java
public interface AnalyzerConfiguration {

    /**
     * Gets the configuration property value.
     *
     * @param key property key
     * @return property value, or null if not set
     */
    String getProperty(String key);

    /**
     * Gets the configuration property value with default.
     *
     * @param key property key
     * @param defaultValue default value if property not set
     * @return property value or default
     */
    String getProperty(String key, String defaultValue);

    /**
     * Gets all configuration properties.
     *
     * @return map of all properties
     */
    Map<String, String> getAllProperties();

    /**
     * Validates the configuration.
     *
     * @return validation result
     */
    ValidationResult validate();
}
```

### Custom Analyzer Development

To create custom analyzers, implement the base analyzer interface:

```java
public interface CustomAnalyzer {

    /**
     * Gets the analyzer name.
     *
     * @return unique analyzer name
     */
    String getName();

    /**
     * Gets the analyzer version.
     *
     * @return analyzer version
     */
    String getVersion();

    /**
     * Configures the analyzer.
     *
     * @param configuration analyzer configuration
     */
    void configure(AnalyzerConfiguration configuration);

    /**
     * Performs analysis on the provided test results.
     *
     * @param testResults test results to analyze
     * @return analysis results
     */
    AnalysisResult analyze(List<TestExecutionResult> testResults);

    /**
     * Gets the supported result types.
     *
     * @return list of supported result types
     */
    List<Class<?>> getSupportedResultTypes();
}
```

## Error Handling

All analysis framework APIs use a consistent error handling approach:

```java
public class AnalysisException extends Exception {

    /**
     * Creates a new analysis exception.
     *
     * @param message error message
     */
    public AnalysisException(String message);

    /**
     * Creates a new analysis exception with cause.
     *
     * @param message error message
     * @param cause underlying cause
     */
    public AnalysisException(String message, Throwable cause);

    /**
     * Gets the analysis context where the error occurred.
     *
     * @return analysis context information
     */
    public AnalysisContext getAnalysisContext();
}
```

## Usage Examples

### Basic Coverage Analysis

```java
// Create coverage integrator
WasmtimeCoverageIntegrator integrator = new WasmtimeCoverageIntegrator();

// Run comprehensive analysis
WasmtimeComprehensiveCoverageReport report = integrator.runComprehensiveCoverageAnalysis();

// Check results
if (report.meets95PercentTarget()) {
    System.out.println("Coverage target achieved!");
} else {
    System.out.println("Coverage gaps: " + report.getUncoveredWasmtimeFeatures());
}
```

### Performance Analysis

```java
// Create performance analyzer
AdvancedPerformanceAnalyzer analyzer = new AdvancedPerformanceAnalyzer();

// Analyze performance
PerformanceAnalysisResult result = analyzer.analyzePerformance(testResults);

// Get recommendations
List<OptimizationRecommendation> recommendations =
    analyzer.generateOptimizationRecommendations(result);

for (OptimizationRecommendation rec : recommendations) {
    System.out.println(rec.getTitle() + ": " + rec.getDescription());
}
```

### Behavioral Validation

```java
// Create behavioral analyzer
BehavioralAnalyzer analyzer = new BehavioralAnalyzer();

// Compare runtime behavior
BehavioralComparisonResult result = analyzer.compareBehavior(
    RuntimeType.JNI, jniResults,
    RuntimeType.PANAMA, panamaResults);

// Check for discrepancies
if (result.hasDiscrepancies()) {
    System.out.println("Behavioral differences detected:");
    result.getDiscrepancies().forEach(System.out::println);
}
```

This API reference provides comprehensive documentation for all analysis framework components, enabling developers to effectively use and extend the comparison test system.