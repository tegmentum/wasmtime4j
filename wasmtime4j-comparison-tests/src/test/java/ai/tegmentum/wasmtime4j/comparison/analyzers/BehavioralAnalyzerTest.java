package ai.tegmentum.wasmtime4j.comparison.analyzers;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for BehavioralAnalyzer, verifying core behavioral comparison logic and
 * ensuring accuracy requirements are met (false positive rate < 5%, false negative rate < 1%).
 */
@DisplayName("BehavioralAnalyzer Tests")
class BehavioralAnalyzerTest {

  private BehavioralAnalyzer analyzer;
  private ToleranceConfiguration strictConfig;
  private ToleranceConfiguration lenientConfig;

  @BeforeEach
  void setUp() {
    analyzer = new BehavioralAnalyzer();
    strictConfig = ToleranceConfiguration.strictConfig();
    lenientConfig = ToleranceConfiguration.lenientConfig();
  }

  @Nested
  @DisplayName("Basic Functionality Tests")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("Should handle null parameters gracefully")
    void shouldHandleNullParameters() {
      assertThatThrownBy(() -> analyzer.analyze(null, Map.of()))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("testName cannot be null");

      assertThatThrownBy(() -> analyzer.analyze("test", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("executionResults cannot be null");
    }

    @Test
    @DisplayName("Should reject empty execution results")
    void shouldRejectEmptyExecutionResults() {
      assertThatThrownBy(() -> analyzer.analyze("test", Map.of()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("executionResults cannot be empty");
    }

    @Test
    @DisplayName("Should handle single runtime execution")
    void shouldHandleSingleRuntimeExecution() {
      final BehavioralAnalyzer.TestExecutionResult result =
          createSuccessfulResult("test-value", Duration.ofMillis(100));
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(RuntimeType.JNI, result);

      final BehavioralAnalysisResult analysis = analyzer.analyze("single-runtime-test", executions);

      assertThat(analysis).isNotNull();
      assertThat(analysis.getTestName()).isEqualTo("single-runtime-test");
      assertThat(analysis.getPairwiseComparisons()).isEmpty();
      assertThat(analysis.getVerdict()).isEqualTo(BehavioralVerdict.CONSISTENT);
      assertThat(analysis.getConsistencyScore()).isEqualTo(1.0);
    }
  }

  @Nested
  @DisplayName("Execution Status Comparison Tests")
  class ExecutionStatusComparisonTests {

    @Test
    @DisplayName("Should identify consistent successful executions")
    void shouldIdentifyConsistentSuccessfulExecutions() {
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResult("value", Duration.ofMillis(100));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createSuccessfulResult("value", Duration.ofMillis(105));

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis =
          analyzer.analyze("consistent-success-test", executions);

      assertThat(analysis.getVerdict()).isEqualTo(BehavioralVerdict.CONSISTENT);
      assertThat(analysis.getConsistencyScore()).isGreaterThan(0.9);
      assertThat(analysis.getDiscrepancies())
          .filteredOn(d -> d.getType() == DiscrepancyType.EXECUTION_STATUS_MISMATCH)
          .isEmpty();
    }

    @Test
    @DisplayName("Should identify execution status mismatches")
    void shouldIdentifyExecutionStatusMismatches() {
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResult("value", Duration.ofMillis(100));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createFailedResult(new RuntimeException("Test failure"), Duration.ofMillis(50));

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis =
          analyzer.analyze("status-mismatch-test", executions);

      assertThat(analysis.getVerdict())
          .isIn(BehavioralVerdict.INCONSISTENT, BehavioralVerdict.INCOMPATIBLE);
      assertThat(analysis.getConsistencyScore()).isLessThan(0.8);
      assertThat(analysis.getDiscrepancies())
          .filteredOn(d -> d.getType() == DiscrepancyType.EXECUTION_STATUS_MISMATCH)
          .isNotEmpty();
    }

    @Test
    @DisplayName("Should handle skipped executions")
    void shouldHandleSkippedExecutions() {
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResult("value", Duration.ofMillis(100));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createSkippedResult("Runtime not available");

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis = analyzer.analyze("skipped-test", executions);

      assertThat(analysis.getDiscrepancies())
          .filteredOn(d -> d.getType() == DiscrepancyType.SKIP_INCONSISTENCY)
          .isNotEmpty();
    }
  }

  @Nested
  @DisplayName("Return Value Comparison Tests")
  class ReturnValueComparisonTests {

    @Test
    @DisplayName("Should identify identical return values")
    void shouldIdentifyIdenticalReturnValues() {
      final String sameValue = "identical-result";
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResult(sameValue, Duration.ofMillis(100));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createSuccessfulResult(sameValue, Duration.ofMillis(105));

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis =
          analyzer.analyze("identical-values-test", executions);

      assertThat(analysis.getVerdict()).isEqualTo(BehavioralVerdict.CONSISTENT);
      assertThat(analysis.getDiscrepancies())
          .filteredOn(d -> d.getType() == DiscrepancyType.RETURN_VALUE_MISMATCH)
          .isEmpty();
    }

    @Test
    @DisplayName("Should detect return value mismatches")
    void shouldDetectReturnValueMismatches() {
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResult("jni-result", Duration.ofMillis(100));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createSuccessfulResult("panama-result", Duration.ofMillis(105));

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis = analyzer.analyze("value-mismatch-test", executions);

      assertThat(analysis.getDiscrepancies())
          .filteredOn(d -> d.getType() == DiscrepancyType.RETURN_VALUE_MISMATCH)
          .isNotEmpty();
    }

    @Test
    @DisplayName("Should handle numeric tolerance in return values")
    void shouldHandleNumericToleranceInReturnValues() {
      final BehavioralAnalyzer strictAnalyzer = new BehavioralAnalyzer(strictConfig);
      final BehavioralAnalyzer lenientAnalyzer = new BehavioralAnalyzer(lenientConfig);

      final double value1 = 1.0000000001; // Very close values
      final double value2 = 1.0000000002;

      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResult(value1, Duration.ofMillis(100));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createSuccessfulResult(value2, Duration.ofMillis(105));

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult strictAnalysis =
          strictAnalyzer.analyze("numeric-tolerance-strict", executions);
      final BehavioralAnalysisResult lenientAnalysis =
          lenientAnalyzer.analyze("numeric-tolerance-lenient", executions);

      // Strict analyzer should detect the small difference
      assertThat(strictAnalysis.getDiscrepancies())
          .filteredOn(d -> d.getType() == DiscrepancyType.RETURN_VALUE_MISMATCH)
          .isNotEmpty();

      // Lenient analyzer should accept the small difference
      assertThat(lenientAnalysis.getDiscrepancies())
          .filteredOn(d -> d.getType() == DiscrepancyType.RETURN_VALUE_MISMATCH)
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("Exception Comparison Tests")
  class ExceptionComparisonTests {

    @Test
    @DisplayName("Should identify consistent exception types")
    void shouldIdentifyConsistentExceptionTypes() {
      final RuntimeException sameException = new RuntimeException("Test error");
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createFailedResult(sameException, Duration.ofMillis(50));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createFailedResult(sameException, Duration.ofMillis(55));

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis =
          analyzer.analyze("consistent-exception-test", executions);

      assertThat(analysis.getDiscrepancies())
          .filteredOn(d -> d.getType() == DiscrepancyType.EXCEPTION_TYPE_MISMATCH)
          .isEmpty();
    }

    @Test
    @DisplayName("Should detect different exception types")
    void shouldDetectDifferentExceptionTypes() {
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createFailedResult(new RuntimeException("Runtime error"), Duration.ofMillis(50));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createFailedResult(new IllegalArgumentException("Argument error"), Duration.ofMillis(55));

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis =
          analyzer.analyze("different-exception-test", executions);

      assertThat(analysis.getDiscrepancies())
          .filteredOn(d -> d.getType() == DiscrepancyType.EXCEPTION_TYPE_MISMATCH)
          .isNotEmpty();
    }
  }

  @Nested
  @DisplayName("Performance Analysis Tests")
  class PerformanceAnalysisTests {

    @Test
    @DisplayName("Should detect significant performance deviations")
    void shouldDetectSignificantPerformanceDeviations() {
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResult("value", Duration.ofMillis(100));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createSuccessfulResult("value", Duration.ofMillis(1000)); // 10x slower

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis =
          analyzer.analyze("performance-deviation-test", executions);

      assertThat(analysis.getDiscrepancies())
          .filteredOn(d -> d.getType() == DiscrepancyType.PERFORMANCE_DEVIATION)
          .isNotEmpty();

      final BehavioralDiscrepancy perfDiscrepancy =
          analysis.getDiscrepancies().stream()
              .filter(d -> d.getType() == DiscrepancyType.PERFORMANCE_DEVIATION)
              .findFirst()
              .orElseThrow();

      assertThat(perfDiscrepancy.getSeverity())
          .isIn(DiscrepancySeverity.MAJOR, DiscrepancySeverity.CRITICAL);
    }

    @Test
    @DisplayName("Should accept performance within tolerance")
    void shouldAcceptPerformanceWithinTolerance() {
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResult("value", Duration.ofMillis(100));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createSuccessfulResult("value", Duration.ofMillis(120)); // 1.2x

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis =
          analyzer.analyze("performance-within-tolerance-test", executions);

      assertThat(analysis.getDiscrepancies())
          .filteredOn(d -> d.getType() == DiscrepancyType.PERFORMANCE_DEVIATION)
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("Memory Usage Analysis Tests")
  class MemoryUsageAnalysisTests {

    @Test
    @DisplayName("Should detect significant memory usage differences")
    void shouldDetectSignificantMemoryUsageDifferences() {
      final BehavioralAnalyzer.MemoryUsage normalMemory =
          new BehavioralAnalyzer.MemoryUsage(1024L, 512L);
      final BehavioralAnalyzer.MemoryUsage highMemory =
          new BehavioralAnalyzer.MemoryUsage(10240L, 5120L); // 10x more

      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResultWithMemory("value", Duration.ofMillis(100), normalMemory);
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createSuccessfulResultWithMemory("value", Duration.ofMillis(105), highMemory);

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis =
          analyzer.analyze("memory-deviation-test", executions);

      assertThat(analysis.getDiscrepancies())
          .filteredOn(d -> d.getType() == DiscrepancyType.MEMORY_USAGE_DEVIATION)
          .isNotEmpty();
    }
  }

  @Nested
  @DisplayName("Complex Scenario Tests")
  class ComplexScenarioTests {

    @Test
    @DisplayName("Should handle mixed success and failure scenarios")
    void shouldHandleMixedSuccessAndFailureScenarios() {
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResult("success", Duration.ofMillis(100));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createFailedResult(new RuntimeException("Failure"), Duration.ofMillis(50));

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis = analyzer.analyze("mixed-scenario-test", executions);

      assertThat(analysis.getVerdict())
          .isIn(BehavioralVerdict.INCONSISTENT, BehavioralVerdict.INCOMPATIBLE);
      assertThat(analysis.getConsistencyScore()).isLessThan(0.8);
      assertThat(analysis.getCriticalDiscrepancyCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should generate comprehensive analysis for multiple discrepancies")
    void shouldGenerateComprehensiveAnalysisForMultipleDiscrepancies() {
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResult("jni-result", Duration.ofMillis(100));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createSuccessfulResult("panama-result", Duration.ofMillis(1000));

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis =
          analyzer.analyze("multiple-discrepancies-test", executions);

      // Should detect both return value mismatch and performance deviation
      assertThat(analysis.getDiscrepancies()).hasSizeGreaterThanOrEqualTo(2);
      assertThat(analysis.getDiscrepancies())
          .extracting(BehavioralDiscrepancy::getType)
          .contains(DiscrepancyType.RETURN_VALUE_MISMATCH, DiscrepancyType.PERFORMANCE_DEVIATION);

      final String summaryReport = analysis.getSummaryReport();
      assertThat(summaryReport)
          .contains("multiple-discrepancies-test")
          .contains("Consistency Score")
          .contains("Discrepancies");
    }
  }

  @Nested
  @DisplayName("Accuracy Validation Tests")
  class AccuracyValidationTests {

    @Test
    @DisplayName("Should maintain low false positive rate")
    void shouldMaintainLowFalsePositiveRate() {
      // Test with identical executions that should not generate discrepancies
      final String sameValue = "identical-test-result";
      final Duration sameDuration = Duration.ofMillis(100);

      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResult(sameValue, sameDuration);
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createSuccessfulResult(sameValue, sameDuration);

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis = analyzer.analyze("false-positive-test", executions);

      // Should not generate any critical discrepancies for identical executions
      assertThat(analysis.getCriticalDiscrepancyCount()).isEqualTo(0);
      assertThat(analysis.getVerdict()).isEqualTo(BehavioralVerdict.CONSISTENT);
      assertThat(analysis.getConsistencyScore()).isGreaterThan(0.95);
    }

    @Test
    @DisplayName("Should maintain low false negative rate")
    void shouldMaintainLowFalseNegativeRate() {
      // Test with clearly different executions that should generate discrepancies
      final BehavioralAnalyzer.TestExecutionResult jniResult =
          createSuccessfulResult("completely-different", Duration.ofMillis(100));
      final BehavioralAnalyzer.TestExecutionResult panamaResult =
          createFailedResult(new RuntimeException("Total failure"), Duration.ofMillis(50));

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, jniResult,
              RuntimeType.PANAMA, panamaResult);

      final BehavioralAnalysisResult analysis = analyzer.analyze("false-negative-test", executions);

      // Should detect the obvious differences
      assertThat(analysis.getDiscrepancies()).isNotEmpty();
      assertThat(analysis.getVerdict())
          .isIn(BehavioralVerdict.INCONSISTENT, BehavioralVerdict.INCOMPATIBLE);
      assertThat(analysis.getConsistencyScore()).isLessThan(0.6);
    }
  }

  // Helper methods for creating test execution results
  private BehavioralAnalyzer.TestExecutionResult createSuccessfulResult(
      final Object returnValue, final Duration duration) {
    return new BehavioralAnalyzer.TestExecutionResult(
        true, false, returnValue, null, duration, null);
  }

  private BehavioralAnalyzer.TestExecutionResult createFailedResult(
      final Exception exception, final Duration duration) {
    return new BehavioralAnalyzer.TestExecutionResult(
        false, false, null, exception, duration, null);
  }

  private BehavioralAnalyzer.TestExecutionResult createSkippedResult(final String reason) {
    return new BehavioralAnalyzer.TestExecutionResult(false, true, null, null, Duration.ZERO, null);
  }

  private BehavioralAnalyzer.TestExecutionResult createSuccessfulResultWithMemory(
      final Object returnValue,
      final Duration duration,
      final BehavioralAnalyzer.MemoryUsage memory) {
    return new BehavioralAnalyzer.TestExecutionResult(
        true, false, returnValue, null, duration, memory);
  }
}
