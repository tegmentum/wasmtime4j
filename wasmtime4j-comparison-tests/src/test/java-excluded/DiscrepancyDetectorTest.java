package ai.tegmentum.wasmtime4j.comparison.analyzers;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for DiscrepancyDetector, verifying accurate detection of meaningful
 * differences and pattern recognition capabilities.
 */
@DisplayName("DiscrepancyDetector Tests")
class DiscrepancyDetectorTest {

  private DiscrepancyDetector detector;
  private ToleranceConfiguration defaultConfig;

  @BeforeEach
  void setUp() {
    defaultConfig = ToleranceConfiguration.defaultConfig();
    detector = new DiscrepancyDetector(defaultConfig);
  }

  @Nested
  @DisplayName("Execution Status Discrepancy Tests")
  class ExecutionStatusDiscrepancyTests {

    @Test
    @DisplayName("Should detect execution status mismatches")
    void shouldDetectExecutionStatusMismatches() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult("success", Duration.ofMillis(100)),
              RuntimeType.PANAMA,
                  createFailedResult(new RuntimeException("Failed"), Duration.ofMillis(50)));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.EXECUTION_STATUS_MISMATCH)
          .hasSize(1);

      final BehavioralDiscrepancy statusDiscrepancy =
          discrepancies.stream()
              .filter(d -> d.getType() == DiscrepancyType.EXECUTION_STATUS_MISMATCH)
              .findFirst()
              .orElseThrow();

      assertThat(statusDiscrepancy.getSeverity()).isEqualTo(DiscrepancySeverity.CRITICAL);
      assertThat(statusDiscrepancy.getDescription()).contains("Execution status inconsistency");
    }

    @Test
    @DisplayName("Should not detect discrepancies for consistent executions")
    void shouldNotDetectDiscrepanciesForConsistentExecutions() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult("success", Duration.ofMillis(100)),
              RuntimeType.PANAMA, createSuccessfulResult("success", Duration.ofMillis(105)));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.EXECUTION_STATUS_MISMATCH)
          .isEmpty();
    }

    @Test
    @DisplayName("Should detect skip inconsistencies")
    void shouldDetectSkipInconsistencies() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult("success", Duration.ofMillis(100)),
              RuntimeType.PANAMA, createSkippedResult("Runtime not available"));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.SKIP_INCONSISTENCY)
          .hasSize(1);

      final BehavioralDiscrepancy skipDiscrepancy =
          discrepancies.stream()
              .filter(d -> d.getType() == DiscrepancyType.SKIP_INCONSISTENCY)
              .findFirst()
              .orElseThrow();

      assertThat(skipDiscrepancy.getSeverity()).isEqualTo(DiscrepancySeverity.MODERATE);
    }
  }

  @Nested
  @DisplayName("Return Value Discrepancy Tests")
  class ReturnValueDiscrepancyTests {

    @Test
    @DisplayName("Should detect return value mismatches")
    void shouldDetectReturnValueMismatches() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult("jni-result", Duration.ofMillis(100)),
              RuntimeType.PANAMA, createSuccessfulResult("panama-result", Duration.ofMillis(105)));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.RETURN_VALUE_MISMATCH)
          .hasSize(1);

      final BehavioralDiscrepancy valueDiscrepancy =
          discrepancies.stream()
              .filter(d -> d.getType() == DiscrepancyType.RETURN_VALUE_MISMATCH)
              .findFirst()
              .orElseThrow();

      assertThat(valueDiscrepancy.getDescription()).contains("Return value inconsistency");
    }

    @Test
    @DisplayName("Should not detect discrepancies for equivalent values")
    void shouldNotDetectDiscrepanciesForEquivalentValues() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult("same-result", Duration.ofMillis(100)),
              RuntimeType.PANAMA, createSuccessfulResult("same-result", Duration.ofMillis(105)));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.RETURN_VALUE_MISMATCH)
          .isEmpty();
    }

    @Test
    @DisplayName("Should handle tolerance for numeric values")
    void shouldHandleToleranceForNumericValues() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult(1.0000000001, Duration.ofMillis(100)),
              RuntimeType.PANAMA, createSuccessfulResult(1.0000000002, Duration.ofMillis(105)));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      // Should not detect discrepancy due to tolerance
      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.RETURN_VALUE_MISMATCH)
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("Exception Discrepancy Tests")
  class ExceptionDiscrepancyTests {

    @Test
    @DisplayName("Should detect different exception types")
    void shouldDetectDifferentExceptionTypes() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI,
                  createFailedResult(new RuntimeException("Error"), Duration.ofMillis(50)),
              RuntimeType.PANAMA,
                  createFailedResult(new IllegalArgumentException("Error"), Duration.ofMillis(55)));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.EXCEPTION_TYPE_MISMATCH)
          .hasSize(1);

      final BehavioralDiscrepancy exceptionDiscrepancy =
          discrepancies.stream()
              .filter(d -> d.getType() == DiscrepancyType.EXCEPTION_TYPE_MISMATCH)
              .findFirst()
              .orElseThrow();

      assertThat(exceptionDiscrepancy.getSeverity()).isEqualTo(DiscrepancySeverity.MAJOR);
    }

    @Test
    @DisplayName("Should detect different exception messages")
    void shouldDetectDifferentExceptionMessages() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI,
                  createFailedResult(
                      new RuntimeException("JNI error message"), Duration.ofMillis(50)),
              RuntimeType.PANAMA,
                  createFailedResult(
                      new RuntimeException("Panama error message"), Duration.ofMillis(55)));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.EXCEPTION_MESSAGE_MISMATCH)
          .hasSize(1);

      final BehavioralDiscrepancy messageDiscrepancy =
          discrepancies.stream()
              .filter(d -> d.getType() == DiscrepancyType.EXCEPTION_MESSAGE_MISMATCH)
              .findFirst()
              .orElseThrow();

      assertThat(messageDiscrepancy.getSeverity()).isEqualTo(DiscrepancySeverity.MODERATE);
    }

    @Test
    @DisplayName("Should not detect discrepancies for same exceptions")
    void shouldNotDetectDiscrepanciesForSameExceptions() {
      final RuntimeException sameException = new RuntimeException("Same error");
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, createFailedResult(sameException, Duration.ofMillis(50)),
              RuntimeType.PANAMA, createFailedResult(sameException, Duration.ofMillis(55)));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.EXCEPTION_TYPE_MISMATCH)
          .isEmpty();
      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.EXCEPTION_MESSAGE_MISMATCH)
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("Performance Discrepancy Tests")
  class PerformanceDiscrepancyTests {

    @Test
    @DisplayName("Should detect significant performance deviations")
    void shouldDetectSignificantPerformanceDeviations() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult("result", Duration.ofMillis(100)),
              RuntimeType.PANAMA,
                  createSuccessfulResult("result", Duration.ofMillis(1000)) // 10x slower
              );

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.PERFORMANCE_DEVIATION)
          .hasSize(1);

      final BehavioralDiscrepancy perfDiscrepancy =
          discrepancies.stream()
              .filter(d -> d.getType() == DiscrepancyType.PERFORMANCE_DEVIATION)
              .findFirst()
              .orElseThrow();

      assertThat(perfDiscrepancy.getSeverity())
          .isIn(DiscrepancySeverity.MAJOR, DiscrepancySeverity.CRITICAL);
      assertThat(perfDiscrepancy.getDescription()).contains("performance variation");
    }

    @Test
    @DisplayName("Should not detect discrepancies for similar performance")
    void shouldNotDetectDiscrepanciesForSimilarPerformance() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult("result", Duration.ofMillis(100)),
              RuntimeType.PANAMA, createSuccessfulResult("result", Duration.ofMillis(120)) // 1.2x
              );

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.PERFORMANCE_DEVIATION)
          .isEmpty();
    }

    @Test
    @DisplayName("Should calculate appropriate severity for performance deviations")
    void shouldCalculateAppropriateSeverityForPerformanceDeviations() {
      // Test different performance ratios
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions3x =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult("result", Duration.ofMillis(100)),
              RuntimeType.PANAMA, createSuccessfulResult("result", Duration.ofMillis(300)) // 3x
              );

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions15x =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult("result", Duration.ofMillis(100)),
              RuntimeType.PANAMA, createSuccessfulResult("result", Duration.ofMillis(1500)) // 15x
              );

      final List<BehavioralDiscrepancy> discrepancies3x =
          detector.detectDiscrepancies(executions3x);
      final List<BehavioralDiscrepancy> discrepancies15x =
          detector.detectDiscrepancies(executions15x);

      final BehavioralDiscrepancy perfDiscrepancy3x =
          discrepancies3x.stream()
              .filter(d -> d.getType() == DiscrepancyType.PERFORMANCE_DEVIATION)
              .findFirst()
              .orElseThrow();

      final BehavioralDiscrepancy perfDiscrepancy15x =
          discrepancies15x.stream()
              .filter(d -> d.getType() == DiscrepancyType.PERFORMANCE_DEVIATION)
              .findFirst()
              .orElseThrow();

      // 15x deviation should have higher severity than 3x
      assertThat(perfDiscrepancy15x.getSeverity().isHigherThan(perfDiscrepancy3x.getSeverity()))
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Memory Usage Discrepancy Tests")
  class MemoryUsageDiscrepancyTests {

    @Test
    @DisplayName("Should detect significant memory usage differences")
    void shouldDetectSignificantMemoryUsageDifferences() {
      final BehavioralAnalyzer.MemoryUsage normalMemory =
          new BehavioralAnalyzer.MemoryUsage(1024L, 512L);
      final BehavioralAnalyzer.MemoryUsage highMemory =
          new BehavioralAnalyzer.MemoryUsage(10240L, 5120L); // 10x more

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI,
                  createSuccessfulResultWithMemory("result", Duration.ofMillis(100), normalMemory),
              RuntimeType.PANAMA,
                  createSuccessfulResultWithMemory("result", Duration.ofMillis(105), highMemory));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.MEMORY_USAGE_DEVIATION)
          .hasSize(1);

      final BehavioralDiscrepancy memoryDiscrepancy =
          discrepancies.stream()
              .filter(d -> d.getType() == DiscrepancyType.MEMORY_USAGE_DEVIATION)
              .findFirst()
              .orElseThrow();

      assertThat(memoryDiscrepancy.getSeverity()).isEqualTo(DiscrepancySeverity.MODERATE);
    }

    @Test
    @DisplayName("Should not detect discrepancies for similar memory usage")
    void shouldNotDetectDiscrepanciesForSimilarMemoryUsage() {
      final BehavioralAnalyzer.MemoryUsage memory1 =
          new BehavioralAnalyzer.MemoryUsage(1024L, 512L);
      final BehavioralAnalyzer.MemoryUsage memory2 =
          new BehavioralAnalyzer.MemoryUsage(1100L, 550L); // Similar

      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI,
                  createSuccessfulResultWithMemory("result", Duration.ofMillis(100), memory1),
              RuntimeType.PANAMA,
                  createSuccessfulResultWithMemory("result", Duration.ofMillis(105), memory2));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies)
          .filteredOn(d -> d.getType() == DiscrepancyType.MEMORY_USAGE_DEVIATION)
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("Edge Cases and Validation Tests")
  class EdgeCasesAndValidationTests {

    @Test
    @DisplayName("Should handle single runtime gracefully")
    void shouldHandleSingleRuntimeGracefully() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(RuntimeType.JNI, createSuccessfulResult("result", Duration.ofMillis(100)));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      assertThat(discrepancies).isEmpty();
    }

    @Test
    @DisplayName("Should require non-null parameters")
    void shouldRequireNonNullParameters() {
      assertThatThrownBy(() -> detector.detectDiscrepancies(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("executionResults cannot be null");
    }

    @Test
    @DisplayName("Should handle empty execution results")
    void shouldHandleEmptyExecutionResults() {
      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(Map.of());
      assertThat(discrepancies).isEmpty();
    }

    @Test
    @DisplayName("Should handle complex scenarios with multiple discrepancies")
    void shouldHandleComplexScenariosWithMultipleDiscrepancies() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult("jni-result", Duration.ofMillis(100)),
              RuntimeType.PANAMA,
                  createFailedResult(
                      new RuntimeException("Panama failure"), Duration.ofMillis(1000)));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      // Should detect both execution status mismatch and potentially performance deviation
      assertThat(discrepancies).hasSizeGreaterThanOrEqualTo(1);
      assertThat(discrepancies)
          .extracting(BehavioralDiscrepancy::getType)
          .contains(DiscrepancyType.EXECUTION_STATUS_MISMATCH);
    }
  }

  @Nested
  @DisplayName("Recommendation Generation Tests")
  class RecommendationGenerationTests {

    @Test
    @DisplayName("Should generate appropriate recommendations for status mismatches")
    void shouldGenerateAppropriateRecommendationsForStatusMismatches() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult("success", Duration.ofMillis(100)),
              RuntimeType.PANAMA,
                  createFailedResult(new RuntimeException("Failed"), Duration.ofMillis(50)));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      final BehavioralDiscrepancy statusDiscrepancy =
          discrepancies.stream()
              .filter(d -> d.getType() == DiscrepancyType.EXECUTION_STATUS_MISMATCH)
              .findFirst()
              .orElseThrow();

      assertThat(statusDiscrepancy.getRecommendation()).isNotBlank().contains("Investigate");
    }

    @Test
    @DisplayName("Should provide detailed information in discrepancy reports")
    void shouldProvideDetailedInformationInDiscrepancyReports() {
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executions =
          Map.of(
              RuntimeType.JNI, createSuccessfulResult("jni-value", Duration.ofMillis(100)),
              RuntimeType.PANAMA, createSuccessfulResult("panama-value", Duration.ofMillis(105)));

      final List<BehavioralDiscrepancy> discrepancies = detector.detectDiscrepancies(executions);

      final BehavioralDiscrepancy valueDiscrepancy =
          discrepancies.stream()
              .filter(d -> d.getType() == DiscrepancyType.RETURN_VALUE_MISMATCH)
              .findFirst()
              .orElseThrow();

      final String detailedReport = valueDiscrepancy.getDetailedReport();
      assertThat(detailedReport)
          .contains("Type:")
          .contains("Severity:")
          .contains("Description:")
          .contains("Details:")
          .contains("Recommendation:");
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
