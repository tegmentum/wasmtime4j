package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for the RegressionDetector automated regression detection system.
 *
 * @since 1.0.0
 */
@DisplayName("RegressionDetector Tests")
class RegressionDetectorTest {

  private RegressionDetector regressionDetector;

  @BeforeEach
  void setUp() {
    regressionDetector = new RegressionDetector();
  }

  @Nested
  @DisplayName("Performance Regression Detection")
  class PerformanceRegressionTests {

    @Test
    @DisplayName("Should detect performance regression when execution time degrades significantly")
    void shouldDetectPerformanceRegression() {
      // Given: Historical performance data showing regression
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> baselineResults = new HashMap<>();
      baselineResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));

      // Establish baseline with multiple data points
      for (int i = 0; i < 6; i++) {
        regressionDetector.detectRegressions(baselineResults);
      }

      // When: Current execution shows significant performance degradation
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> currentResults = new HashMap<>();
      currentResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(250))); // 2.5x slower

      final List<BehavioralDiscrepancy> regressions = regressionDetector.detectRegressions(currentResults);

      // Then: Should detect performance regression
      assertTrue(
          regressions.stream().anyMatch(r ->
              r.getType() == DiscrepancyType.PERFORMANCE_DEVIATION &&
              r.getDescription().contains("Performance regression detected") &&
              r.getSeverity() == DiscrepancySeverity.MAJOR),
          "Should detect performance regression for significant execution time increase");
    }

    @Test
    @DisplayName("Should not detect regression for minor performance variations")
    void shouldNotDetectRegressionForMinorVariations() {
      // Given: Historical performance data
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> baselineResults = new HashMap<>();
      baselineResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));

      // Establish baseline
      for (int i = 0; i < 6; i++) {
        regressionDetector.detectRegressions(baselineResults);
      }

      // When: Current execution shows minor variation (within threshold)
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> currentResults = new HashMap<>();
      currentResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(115))); // 15% increase

      final List<BehavioralDiscrepancy> regressions = regressionDetector.detectRegressions(currentResults);

      // Then: Should not detect regression for minor variations
      assertTrue(
          regressions.stream().noneMatch(r ->
              r.getType() == DiscrepancyType.PERFORMANCE_DEVIATION &&
              r.getDescription().contains("Performance regression")),
          "Should not detect regression for minor performance variations within threshold");
    }

    @Test
    @DisplayName("Should track performance across multiple runtimes independently")
    void shouldTrackPerformanceAcrossMultipleRuntimes() {
      // Given: Historical data for both runtimes
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> baselineResults = new HashMap<>();
      baselineResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));
      baselineResults.put(RuntimeType.PANAMA, createSuccessfulResult(42, Duration.ofMillis(90)));

      // Establish baseline
      for (int i = 0; i < 6; i++) {
        regressionDetector.detectRegressions(baselineResults);
      }

      // When: Only JNI shows regression, Panama remains stable
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> currentResults = new HashMap<>();
      currentResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(250))); // Regression
      currentResults.put(RuntimeType.PANAMA, createSuccessfulResult(42, Duration.ofMillis(95))); // Stable

      final List<BehavioralDiscrepancy> regressions = regressionDetector.detectRegressions(currentResults);

      // Then: Should detect regression only for JNI runtime
      assertTrue(
          regressions.stream().anyMatch(r ->
              r.getDescription().contains("Performance regression") &&
              r.getAffectedRuntimes().contains(RuntimeType.JNI) &&
              !r.getAffectedRuntimes().contains(RuntimeType.PANAMA)),
          "Should detect regression only for affected runtime");
    }
  }

  @Nested
  @DisplayName("Behavioral Regression Detection")
  class BehavioralRegressionTests {

    @Test
    @DisplayName("Should detect behavioral regression when success rate drops")
    void shouldDetectBehavioralRegressionWhenSuccessRateDrops() {
      // Given: Historical success data
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> successResults = new HashMap<>();
      successResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));

      // Establish successful baseline
      for (int i = 0; i < 6; i++) {
        regressionDetector.detectRegressions(successResults);
      }

      // When: Current execution starts failing
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> failureResults = new HashMap<>();
      failureResults.put(RuntimeType.JNI, createFailedResult(
          new RuntimeException("New failure"), Duration.ofMillis(100)));

      final List<BehavioralDiscrepancy> regressions = regressionDetector.detectRegressions(failureResults);

      // Then: Should detect behavioral regression
      assertTrue(
          regressions.stream().anyMatch(r ->
              r.getType() == DiscrepancyType.SYSTEMATIC_PATTERN &&
              r.getDescription().contains("Behavioral regression detected") &&
              r.getSeverity() == DiscrepancySeverity.CRITICAL),
          "Should detect behavioral regression when success rate drops significantly");
    }

    @Test
    @DisplayName("Should detect improvement in behavioral patterns")
    void shouldDetectImprovementInBehavioralPatterns() {
      // Given: Historical failure data
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> failureResults = new HashMap<>();
      failureResults.put(RuntimeType.JNI, createFailedResult(
          new RuntimeException("Historical failure"), Duration.ofMillis(100)));

      // Establish failure baseline
      for (int i = 0; i < 6; i++) {
        regressionDetector.detectRegressions(failureResults);
      }

      // When: Current execution starts succeeding
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> successResults = new HashMap<>();
      successResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));

      final List<BehavioralDiscrepancy> regressions = regressionDetector.detectRegressions(successResults);

      // Then: Should detect behavioral improvement (positive change)
      assertTrue(
          regressions.stream().anyMatch(r ->
              r.getDescription().contains("Behavioral regression") &&
              r.getSeverity() == DiscrepancySeverity.MODERATE), // Improvement is moderate, not critical
          "Should detect behavioral change (improvement) with appropriate severity");
    }
  }

  @Nested
  @DisplayName("Systematic Regression Detection")
  class SystematicRegressionTests {

    @Test
    @DisplayName("Should detect systematic performance regression across multiple runtimes")
    void shouldDetectSystematicPerformanceRegression() {
      // Given: Historical data for multiple runtimes showing good performance
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> baselineResults = new HashMap<>();
      baselineResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));
      baselineResults.put(RuntimeType.PANAMA, createSuccessfulResult(42, Duration.ofMillis(90)));

      // Establish baseline
      for (int i = 0; i < 6; i++) {
        regressionDetector.detectRegressions(baselineResults);
      }

      // When: Both runtimes show performance regression (systematic issue)
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> regressedResults = new HashMap<>();
      regressedResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(250)));
      regressedResults.put(RuntimeType.PANAMA, createSuccessfulResult(42, Duration.ofMillis(225)));

      final List<BehavioralDiscrepancy> regressions = regressionDetector.detectRegressions(regressedResults);

      // Then: Should detect systematic performance regression
      assertTrue(
          regressions.stream().anyMatch(r ->
              r.getType() == DiscrepancyType.SYSTEMATIC_PATTERN &&
              r.getDescription().contains("Systematic performance regression") &&
              r.getSeverity() == DiscrepancySeverity.CRITICAL),
          "Should detect systematic performance regression affecting multiple runtimes");
    }

    @Test
    @DisplayName("Should detect systematic behavioral regression across multiple runtimes")
    void shouldDetectSystematicBehavioralRegression() {
      // Given: Historical success data for multiple runtimes
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> successResults = new HashMap<>();
      successResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));
      successResults.put(RuntimeType.PANAMA, createSuccessfulResult(42, Duration.ofMillis(90)));

      // Establish baseline
      for (int i = 0; i < 6; i++) {
        regressionDetector.detectRegressions(successResults);
      }

      // When: Both runtimes start failing (systematic behavioral issue)
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> failureResults = new HashMap<>();
      failureResults.put(RuntimeType.JNI, createFailedResult(
          new RuntimeException("Systematic failure"), Duration.ofMillis(100)));
      failureResults.put(RuntimeType.PANAMA, createFailedResult(
          new RuntimeException("Systematic failure"), Duration.ofMillis(90)));

      final List<BehavioralDiscrepancy> regressions = regressionDetector.detectRegressions(failureResults);

      // Then: Should detect systematic behavioral regression
      assertTrue(
          regressions.stream().anyMatch(r ->
              r.getType() == DiscrepancyType.SYSTEMATIC_PATTERN &&
              r.getDescription().contains("Systematic behavioral regression") &&
              r.getSeverity() == DiscrepancySeverity.CRITICAL),
          "Should detect systematic behavioral regression affecting multiple runtimes");
    }
  }

  @Nested
  @DisplayName("Baseline Management")
  class BaselineManagementTests {

    @Test
    @DisplayName("Should establish and use baselines for regression detection")
    void shouldEstablishAndUseBaselinesForRegressionDetection() {
      // Given: Establish a baseline
      regressionDetector.establishBaseline("test", RuntimeType.JNI, "execution_time", 100.0, 0.2);

      // When: Getting baselines
      final Map<String, RegressionDetector.RegressionBaseline> baselines = regressionDetector.getBaselines();

      // Then: Should contain the established baseline
      assertFalse(baselines.isEmpty(), "Should contain established baseline");
      assertTrue(baselines.containsKey("test_JNI_execution_time"), "Should use correct baseline key");

      final RegressionDetector.RegressionBaseline baseline = baselines.get("test_JNI_execution_time");
      assertEquals(100.0, baseline.getBaselineValue(), 0.001, "Should store correct baseline value");
      assertEquals(0.2, baseline.getTolerance(), 0.001, "Should store correct tolerance");
    }

    @Test
    @DisplayName("Should detect regression against established baseline")
    void shouldDetectRegressionAgainstEstablishedBaseline() {
      // Given: Establish a baseline with tight tolerance
      regressionDetector.establishBaseline("test", RuntimeType.JNI, "execution_time", 100.0, 0.1);
      final RegressionDetector.RegressionBaseline baseline = regressionDetector.getBaselines()
          .get("test_JNI_execution_time");

      // When: Testing values against baseline
      final boolean isRegression1 = baseline.isRegression(130.0); // 30% increase
      final boolean isRegression2 = baseline.isRegression(105.0); // 5% increase

      // Then: Should correctly identify regressions
      assertTrue(isRegression1, "Should detect regression for significant deviation");
      assertFalse(isRegression2, "Should not detect regression within tolerance");
    }

    @Test
    @DisplayName("Should clear history and baselines")
    void shouldClearHistoryAndBaselines() {
      // Given: Established baselines and history
      regressionDetector.establishBaseline("test", RuntimeType.JNI, "execution_time", 100.0, 0.2);

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> results = new HashMap<>();
      results.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));
      regressionDetector.detectRegressions(results);

      // When: Clearing history
      regressionDetector.clearHistory();

      // Then: Should clear all data
      assertTrue(regressionDetector.getBaselines().isEmpty(), "Should clear all baselines");
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle insufficient historical data gracefully")
    void shouldHandleInsufficientHistoricalDataGracefully() {
      // Given: Minimal historical data (below threshold)
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> results = new HashMap<>();
      results.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));

      // When: Detecting regressions with insufficient data
      final List<BehavioralDiscrepancy> regressions = regressionDetector.detectRegressions(results);

      // Then: Should not crash and may return empty results
      assertNotNull(regressions, "Should return non-null result");
      // May or may not have regressions, but should not throw exceptions
    }

    @Test
    @DisplayName("Should handle empty execution results")
    void shouldHandleEmptyExecutionResults() {
      // Given: Empty execution results
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> emptyResults = new HashMap<>();

      // When: Detecting regressions
      final List<BehavioralDiscrepancy> regressions = regressionDetector.detectRegressions(emptyResults);

      // Then: Should handle gracefully
      assertNotNull(regressions, "Should return non-null result");
      // Should not crash with empty input
    }

    @Test
    @DisplayName("Should handle mixed success and failure patterns")
    void shouldHandleMixedSuccessAndFailurePatterns() {
      // Given: Mixed execution results
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> mixedResults = new HashMap<>();
      mixedResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));
      mixedResults.put(RuntimeType.PANAMA, createFailedResult(
          new RuntimeException("Test failure"), Duration.ofMillis(90)));

      // When: Detecting regressions multiple times
      assertDoesNotThrow(() -> {
        for (int i = 0; i < 10; i++) {
          regressionDetector.detectRegressions(mixedResults);
        }
      }, "Should handle mixed results without throwing exceptions");

      // Then: Should maintain data integrity
      final Map<String, RegressionDetector.RegressionBaseline> baselines = regressionDetector.getBaselines();
      assertNotNull(baselines, "Should maintain baseline integrity");
    }
  }

  // Helper methods for creating test data

  private BehavioralAnalyzer.TestExecutionResult createSuccessfulResult(final Object returnValue, final Duration executionTime) {
    return new BehavioralAnalyzer.TestExecutionResult(
        true, false, returnValue, null, executionTime, null);
  }

  private BehavioralAnalyzer.TestExecutionResult createFailedResult(final Exception exception, final Duration executionTime) {
    return new BehavioralAnalyzer.TestExecutionResult(
        false, false, null, exception, executionTime, null);
  }

  private BehavioralAnalyzer.TestExecutionResult createSkippedResult(final Duration executionTime) {
    return new BehavioralAnalyzer.TestExecutionResult(
        false, true, null, null, executionTime, null);
  }
}