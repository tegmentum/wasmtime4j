package ai.tegmentum.wasmtime4j.comparison.analyzers;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the enhanced DiscrepancyDetector with Wasmtime-specific detection
 * capabilities, zero discrepancy validation, and regression detection.
 *
 * @since 1.0.0
 */
@DisplayName("Enhanced DiscrepancyDetector Tests")
class EnhancedDiscrepancyDetectorTest {

  private DiscrepancyDetector discrepancyDetector;
  private ToleranceConfiguration toleranceConfig;

  @BeforeEach
  void setUp() {
    toleranceConfig = ToleranceConfiguration.defaultConfig();
    discrepancyDetector = new DiscrepancyDetector(toleranceConfig);
  }

  @Nested
  @DisplayName("Wasmtime Compatibility Validation")
  class WasmtimeCompatibilityTests {

    @Test
    @DisplayName("Should detect floating-point precision inconsistencies")
    void shouldDetectFloatingPointPrecisionInconsistencies() {
      // Given: Results with floating-point values that differ beyond Wasmtime tolerance
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          new HashMap<>();

      executionResults.put(
          RuntimeType.JNI, createSuccessfulResult(3.14159265358979323846, Duration.ofMillis(100)));
      executionResults.put(
          RuntimeType.PANAMA,
          createSuccessfulResult(3.14159265358970000000, Duration.ofMillis(102)));

      // When: Detecting discrepancies
      final List<BehavioralDiscrepancy> discrepancies =
          discrepancyDetector.detectDiscrepancies(executionResults);

      // Then: Should detect floating-point precision discrepancy
      assertFalse(discrepancies.isEmpty(), "Should detect floating-point precision discrepancy");
      assertTrue(
          discrepancies.stream()
              .anyMatch(
                  d ->
                      d.getType() == DiscrepancyType.RETURN_VALUE_MISMATCH
                          && d.getSeverity() == DiscrepancySeverity.CRITICAL
                          && d.getDetails().contains("IEEE 754")),
          "Should identify as critical floating-point precision issue");
    }

    @Test
    @DisplayName("Should validate equivalent floating-point values within Wasmtime tolerance")
    void shouldValidateEquivalentFloatingPointValues() {
      // Given: Results with floating-point values within Wasmtime tolerance
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          new HashMap<>();

      executionResults.put(
          RuntimeType.JNI, createSuccessfulResult(3.14159265358979323846, Duration.ofMillis(100)));
      executionResults.put(
          RuntimeType.PANAMA,
          createSuccessfulResult(3.14159265358979323847, Duration.ofMillis(102)));

      // When: Detecting discrepancies
      final List<BehavioralDiscrepancy> discrepancies =
          discrepancyDetector.detectDiscrepancies(executionResults);

      // Then: Should not detect any floating-point discrepancies
      assertTrue(
          discrepancies.stream()
              .noneMatch(
                  d ->
                      d.getType() == DiscrepancyType.RETURN_VALUE_MISMATCH
                          && d.getDetails().contains("floating-point")),
          "Should not detect discrepancies for values within Wasmtime tolerance");
    }

    @Test
    @DisplayName("Should detect memory usage variations beyond acceptable thresholds")
    void shouldDetectMemoryUsageVariations() {
      // Given: Results with significant memory usage differences
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          new HashMap<>();

      final BehavioralAnalyzer.MemoryUsage lowMemory =
          new BehavioralAnalyzer.MemoryUsage(1000, 500);
      final BehavioralAnalyzer.MemoryUsage highMemory =
          new BehavioralAnalyzer.MemoryUsage(2000, 600);

      executionResults.put(
          RuntimeType.JNI, createSuccessfulResultWithMemory(42, Duration.ofMillis(100), lowMemory));
      executionResults.put(
          RuntimeType.PANAMA,
          createSuccessfulResultWithMemory(42, Duration.ofMillis(102), highMemory));

      // When: Detecting discrepancies
      final List<BehavioralDiscrepancy> discrepancies =
          discrepancyDetector.detectDiscrepancies(executionResults);

      // Then: Should detect memory usage discrepancy
      assertTrue(
          discrepancies.stream()
              .anyMatch(
                  d ->
                      d.getType() == DiscrepancyType.MEMORY_USAGE_DEVIATION
                          && d.getSeverity() == DiscrepancySeverity.MAJOR
                          && d.getDetails().contains("varies by")),
          "Should detect memory usage variation as major discrepancy");
    }

    @Test
    @DisplayName("Should categorize exceptions according to Wasmtime specification")
    void shouldCategorizeExceptionsAccordingToWasmtimeSpec() {
      // Given: Results with different exception types that should be equivalent
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          new HashMap<>();

      final RuntimeException runtimeTrap = new RuntimeException("WebAssembly trap: out of bounds");
      final IllegalStateException wasmTrap = new IllegalStateException("WASM execution trap");

      executionResults.put(RuntimeType.JNI, createFailedResult(runtimeTrap, Duration.ofMillis(50)));
      executionResults.put(RuntimeType.PANAMA, createFailedResult(wasmTrap, Duration.ofMillis(52)));

      // When: Detecting discrepancies
      final List<BehavioralDiscrepancy> discrepancies =
          discrepancyDetector.detectDiscrepancies(executionResults);

      // Then: Should not detect exception type discrepancy for equivalent categories
      assertTrue(
          discrepancies.stream()
              .noneMatch(
                  d ->
                      d.getType() == DiscrepancyType.EXCEPTION_TYPE_MISMATCH
                          && d.getDetails().contains("categories")),
          "Should not detect discrepancy for exceptions in same Wasmtime category");
    }
  }

  @Nested
  @DisplayName("Zero Discrepancy Requirement Validation")
  class ZeroDiscrepancyValidationTests {

    @Test
    @DisplayName("Should validate zero discrepancy requirement compliance")
    void shouldValidateZeroDiscrepancyRequirementCompliance() {
      // Given: Results with no discrepancies
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          new HashMap<>();

      executionResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));
      executionResults.put(RuntimeType.PANAMA, createSuccessfulResult(42, Duration.ofMillis(102)));

      // When: Detecting discrepancies
      final List<BehavioralDiscrepancy> discrepancies =
          discrepancyDetector.detectDiscrepancies(executionResults);

      // Then: Should not report zero discrepancy requirement violations
      assertTrue(
          discrepancies.stream()
              .noneMatch(d -> d.getDescription().contains("Zero discrepancy requirement violated")),
          "Should not violate zero discrepancy requirement for equivalent results");
    }

    @Test
    @DisplayName("Should flag zero discrepancy requirement violations")
    void shouldFlagZeroDiscrepancyRequirementViolations() {
      // Given: Results with critical discrepancies
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          new HashMap<>();

      executionResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));
      executionResults.put(RuntimeType.PANAMA, createSuccessfulResult(43, Duration.ofMillis(102)));

      // When: Detecting discrepancies (this should create a critical discrepancy)
      final List<BehavioralDiscrepancy> discrepancies =
          discrepancyDetector.detectDiscrepancies(executionResults);

      // Then: Should detect zero discrepancy requirement violation
      assertTrue(
          discrepancies.stream()
              .anyMatch(
                  d ->
                      d.getDescription().contains("Zero discrepancy requirement violated")
                          && d.getSeverity() == DiscrepancySeverity.CRITICAL),
          "Should flag zero discrepancy requirement violation when critical discrepancies exist");
    }

    @Test
    @DisplayName("Should detect JNI vs Panama behavioral divergence")
    void shouldDetectJniVsPanamaBehavioralDivergence() {
      // Given: Results with different success status between JNI and Panama
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          new HashMap<>();

      executionResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));
      executionResults.put(
          RuntimeType.PANAMA,
          createFailedResult(new RuntimeException("Execution failed"), Duration.ofMillis(95)));

      // When: Detecting discrepancies
      final List<BehavioralDiscrepancy> discrepancies =
          discrepancyDetector.detectDiscrepancies(executionResults);

      // Then: Should detect JNI vs Panama divergence
      assertTrue(
          discrepancies.stream()
              .anyMatch(
                  d ->
                      d.getDescription().contains("JNI vs Panama behavioral divergence")
                          && d.getSeverity() == DiscrepancySeverity.CRITICAL
                          && d.getAffectedRuntimes()
                              .containsAll(Set.of(RuntimeType.JNI, RuntimeType.PANAMA))),
          "Should detect critical JNI vs Panama behavioral divergence");
    }
  }

  @Nested
  @DisplayName("Regression Detection")
  class RegressionDetectionTests {

    @Test
    @DisplayName("Should detect performance regressions")
    void shouldDetectPerformanceRegressions() {
      // Given: Results with significantly degraded performance
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          new HashMap<>();

      // Simulate a performance regression with 3x slower execution
      executionResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(300)));
      executionResults.put(RuntimeType.PANAMA, createSuccessfulResult(42, Duration.ofMillis(100)));

      // When: Detecting discrepancies
      final List<BehavioralDiscrepancy> discrepancies =
          discrepancyDetector.detectDiscrepancies(executionResults);

      // Then: Should detect performance regression
      assertTrue(
          discrepancies.stream()
              .anyMatch(
                  d ->
                      d.getType() == DiscrepancyType.PERFORMANCE_DEVIATION
                          && d.getDescription().toLowerCase().contains("regression")),
          "Should detect performance regression when execution time degrades significantly");
    }

    @Test
    @DisplayName("Should detect systematic failure patterns")
    void shouldDetectSystematicFailurePatterns() {
      // Given: Results indicating systematic issues
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          new HashMap<>();

      // Create a pattern that suggests systematic issues
      final RuntimeException systematicError = new RuntimeException("Systematic failure pattern");
      executionResults.put(
          RuntimeType.JNI, createFailedResult(systematicError, Duration.ofMillis(50)));
      executionResults.put(
          RuntimeType.PANAMA, createFailedResult(systematicError, Duration.ofMillis(52)));

      // When: Detecting discrepancies
      final List<BehavioralDiscrepancy> discrepancies =
          discrepancyDetector.detectDiscrepancies(executionResults);

      // Then: Should detect systematic patterns
      assertTrue(
          discrepancies.stream().anyMatch(d -> d.getType() == DiscrepancyType.SYSTEMATIC_PATTERN),
          "Should detect systematic failure patterns when multiple runtimes show similar issues");
    }
  }

  @Nested
  @DisplayName("Comprehensive Discrepancy Detection")
  class ComprehensiveDetectionTests {

    @Test
    @DisplayName("Should detect multiple types of discrepancies in single analysis")
    void shouldDetectMultipleTypesOfDiscrepancies() {
      // Given: Results with multiple types of issues
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          new HashMap<>();

      // Different return values (behavioral discrepancy)
      executionResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));
      executionResults.put(
          RuntimeType.PANAMA,
          createSuccessfulResult(43, Duration.ofMillis(500))); // Also performance issue

      // When: Detecting discrepancies
      final List<BehavioralDiscrepancy> discrepancies =
          discrepancyDetector.detectDiscrepancies(executionResults);

      // Then: Should detect both behavioral and performance discrepancies
      assertTrue(
          discrepancies.stream()
              .anyMatch(d -> d.getType() == DiscrepancyType.RETURN_VALUE_MISMATCH),
          "Should detect return value mismatch");
      assertTrue(
          discrepancies.stream()
              .anyMatch(d -> d.getType() == DiscrepancyType.PERFORMANCE_DEVIATION),
          "Should detect performance deviation");
    }

    @Test
    @DisplayName("Should provide detailed discrepancy information for analysis")
    void shouldProvideDetailedDiscrepancyInformation() {
      // Given: Results with a clear discrepancy
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          new HashMap<>();

      executionResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));
      executionResults.put(RuntimeType.PANAMA, createSuccessfulResult(43, Duration.ofMillis(102)));

      // When: Detecting discrepancies
      final List<BehavioralDiscrepancy> discrepancies =
          discrepancyDetector.detectDiscrepancies(executionResults);

      // Then: Should provide detailed information
      assertFalse(discrepancies.isEmpty(), "Should detect discrepancy");

      final BehavioralDiscrepancy discrepancy = discrepancies.get(0);
      assertNotNull(discrepancy.getDescription(), "Should have description");
      assertNotNull(discrepancy.getDetails(), "Should have details");
      assertNotNull(discrepancy.getRecommendation(), "Should have recommendation");
      assertNotNull(discrepancy.getType(), "Should have type");
      assertNotNull(discrepancy.getSeverity(), "Should have severity");
      assertNotNull(discrepancy.getDetectedAt(), "Should have detection timestamp");
    }

    @Test
    @DisplayName("Should handle edge cases gracefully")
    void shouldHandleEdgeCasesGracefully() {
      // Given: Edge case scenarios
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults =
          new HashMap<>();

      // Empty results
      List<BehavioralDiscrepancy> discrepancies =
          discrepancyDetector.detectDiscrepancies(executionResults);
      assertTrue(discrepancies.isEmpty(), "Should handle empty results gracefully");

      // Single runtime result
      executionResults.put(RuntimeType.JNI, createSuccessfulResult(42, Duration.ofMillis(100)));
      discrepancies = discrepancyDetector.detectDiscrepancies(executionResults);
      // May have zero discrepancy validation issues but should not crash

      // Null return values
      executionResults.put(
          RuntimeType.PANAMA, createSuccessfulResult(null, Duration.ofMillis(102)));
      discrepancies = discrepancyDetector.detectDiscrepancies(executionResults);
      // Should handle null values without throwing exceptions

      assertDoesNotThrow(
          () -> discrepancyDetector.detectDiscrepancies(executionResults),
          "Should not throw exceptions for edge cases");
    }
  }

  // Helper methods for creating test data

  private BehavioralAnalyzer.TestExecutionResult createSuccessfulResult(
      final Object returnValue, final Duration executionTime) {
    return new BehavioralAnalyzer.TestExecutionResult(
        true, false, returnValue, null, executionTime, null);
  }

  private BehavioralAnalyzer.TestExecutionResult createSuccessfulResultWithMemory(
      final Object returnValue,
      final Duration executionTime,
      final BehavioralAnalyzer.MemoryUsage memoryUsage) {
    return new BehavioralAnalyzer.TestExecutionResult(
        true, false, returnValue, null, executionTime, memoryUsage);
  }

  private BehavioralAnalyzer.TestExecutionResult createFailedResult(
      final Exception exception, final Duration executionTime) {
    return new BehavioralAnalyzer.TestExecutionResult(
        false, false, null, exception, executionTime, null);
  }

  private BehavioralAnalyzer.TestExecutionResult createSkippedResult(final Duration executionTime) {
    return new BehavioralAnalyzer.TestExecutionResult(false, true, null, null, executionTime, null);
  }
}
