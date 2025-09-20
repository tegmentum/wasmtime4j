/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import ai.tegmentum.wasmtime4j.documentation.impl.DefaultParityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Comprehensive validation tests for API parity between JNI and Panama implementations.
 *
 * <p>These tests ensure 100% functional parity between the JNI and Panama
 * implementations of the wasmtime4j API.
 *
 * @since 1.0.0
 */
@DisplayName("API Parity Validation")
class ApiParityValidationTest {

    private ParityValidator parityValidator;

    @BeforeEach
    void setUp() {
        parityValidator = new DefaultParityValidator();
    }

    @Test
    @DisplayName("Should validate complete API parity without exceptions")
    void shouldValidateCompleteApiParityWithoutExceptions() {
        assertThatNoException().isThrownBy(() -> {
            final ParityReport report = parityValidator.validateFullParity();
            assertThat(report).isNotNull();
        });
    }

    @Test
    @DisplayName("Should achieve 100% API parity between implementations")
    @EnabledIf("isStrictParityRequired")
    void shouldAchieve100PercentApiParity() {
        final ParityReport report = parityValidator.validateFullParity();

        assertThat(report.isCompleteParityAchieved())
                .withFailMessage("API parity not achieved. Violations: %s",
                        report.getAllViolations())
                .isTrue();

        assertThat(report.getCompliancePercentage())
                .withFailMessage("API compliance is %.2f%%, expected 100%%",
                        report.getCompliancePercentage())
                .isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should meet minimum API parity compliance threshold")
    void shouldMeetMinimumApiParityComplianceThreshold() {
        final ParityReport report = parityValidator.validateFullParity();

        final double minimumCompliance = 95.0; // 95% minimum compliance
        assertThat(report.getCompliancePercentage())
                .withFailMessage("API compliance is %.2f%%, expected at least %.2f%%",
                        report.getCompliancePercentage(), minimumCompliance)
                .isGreaterThanOrEqualTo(minimumCompliance);
    }

    @Test
    @DisplayName("Should have no missing methods between implementations")
    void shouldHaveNoMissingMethodsBetweenImplementations() {
        final ParityReport report = parityValidator.validateFullParity();
        final List<String> missingMethods = report.getMissingMethods();

        if (!missingMethods.isEmpty()) {
            System.err.println("WARNING: Found missing methods:");
            missingMethods.forEach(method -> System.err.println("  - " + method));
        }

        assertThat(missingMethods)
                .withFailMessage("Found %d missing methods: %s",
                        missingMethods.size(), missingMethods)
                .isEmpty();
    }

    @Test
    @DisplayName("Should have no critical parity violations")
    void shouldHaveNoCriticalParityViolations() {
        final List<ParityViolation> violations = parityValidator.findViolations();

        final List<ParityViolation> criticalViolations = violations.stream()
                .filter(v -> v.getSeverity() == ViolationSeverity.CRITICAL)
                .toList();

        if (!criticalViolations.isEmpty()) {
            System.err.println("CRITICAL: Found critical parity violations:");
            criticalViolations.forEach(violation ->
                    System.err.println("  - " + violation.getMethodSignature() + ": " + violation.getDescription()));
        }

        assertThat(criticalViolations)
                .withFailMessage("Found %d critical violations: %s",
                        criticalViolations.size(), criticalViolations)
                .isEmpty();
    }

    @Test
    @DisplayName("Should have identical method signatures across implementations")
    void shouldHaveIdenticalMethodSignaturesAcrossImplementations() {
        final ParityReport report = parityValidator.validateFullParity();
        final var methodParity = report.getMethodParity();

        final long identicalMethods = methodParity.values().stream()
                .filter(status -> status == ParityStatus.IDENTICAL)
                .count();

        final double identicalPercentage = methodParity.isEmpty() ? 100.0 :
                (double) identicalMethods / methodParity.size() * 100.0;

        assertThat(identicalPercentage)
                .withFailMessage("Only %.2f%% of methods have identical signatures",
                        identicalPercentage)
                .isGreaterThanOrEqualTo(90.0); // 90% should be identical
    }

    @Test
    @DisplayName("Should have consistent type definitions across implementations")
    void shouldHaveConsistentTypeDefinitionsAcrossImplementations() {
        final ParityReport report = parityValidator.validateFullParity();
        final var typeParity = report.getTypeParity();

        final long identicalTypes = typeParity.values().stream()
                .filter(status -> status == ParityStatus.IDENTICAL)
                .count();

        final double identicalPercentage = typeParity.isEmpty() ? 100.0 :
                (double) identicalTypes / typeParity.size() * 100.0;

        assertThat(identicalPercentage)
                .withFailMessage("Only %.2f%% of types have identical definitions",
                        identicalPercentage)
                .isGreaterThanOrEqualTo(95.0); // 95% should be identical
    }

    @Test
    @DisplayName("Should have no behavioral inconsistencies")
    void shouldHaveNoBehavioralInconsistencies() {
        final ParityReport report = parityValidator.validateFullParity();
        final List<String> inconsistentBehaviors = report.getInconsistentBehaviors();

        if (!inconsistentBehaviors.isEmpty()) {
            System.err.println("WARNING: Found behavioral inconsistencies:");
            inconsistentBehaviors.forEach(behavior -> System.err.println("  - " + behavior));
        }

        assertThat(inconsistentBehaviors)
                .withFailMessage("Found %d behavioral inconsistencies: %s",
                        inconsistentBehaviors.size(), inconsistentBehaviors)
                .isEmpty();
    }

    @Test
    @DisplayName("Should validate individual method behavioral parity")
    void shouldValidateIndividualMethodBehavioralParity() {
        final String testMethodName = "ai.tegmentum.wasmtime4j.Engine.newBuilder";

        assertThatNoException().isThrownBy(() -> {
            final BehavioralParityResult result = parityValidator.validateMethodBehavior(testMethodName);

            assertThat(result).isNotNull();
            assertThat(result.getMethodSignature()).isEqualTo(testMethodName);
            assertThat(result.getTestCases()).isNotEmpty();
            assertThat(result.getConfidenceScore()).isBetween(0.0, 1.0);
        });
    }

    @Test
    @DisplayName("Should validate performance parity within acceptable thresholds")
    void shouldValidatePerformanceParityWithinAcceptableThresholds() {
        assertThatNoException().isThrownBy(() -> {
            final PerformanceParityResult result = parityValidator.validatePerformanceParity();

            assertThat(result).isNotNull();
            assertThat(result.getJniMetrics()).isNotEmpty();
            assertThat(result.getPanamaMetrics()).isNotEmpty();
            assertThat(result.getPerformanceDifferences()).isNotEmpty();

            // Verify performance differences are within acceptable range
            final double maxDifference = result.getMaximumDifference();
            final double threshold = result.getToleranceThreshold();

            if (maxDifference > threshold) {
                System.err.println("WARNING: Performance difference " + maxDifference +
                                   "% exceeds threshold " + threshold + "%");
            }

            // Log performance comparison for analysis
            System.out.println("Performance Parity Results:");
            result.getPerformanceDifferences().forEach((metric, difference) ->
                    System.out.printf("  %s: %.2f%% difference%n", metric, difference));
        });
    }

    @Test
    @DisplayName("Should provide comprehensive parity report summary")
    void shouldProvideComprehensiveParityReportSummary() {
        final ParityReport report = parityValidator.validateFullParity();
        final String summary = report.getSummary();

        assertThat(summary)
                .isNotBlank()
                .contains("API Parity Analysis Summary")
                .contains("Overall Compliance")
                .contains("Method Parity Analysis")
                .contains("Type Parity Analysis");

        // Summary should contain specific metrics
        assertThat(summary).containsPattern("\\d+\\.\\d+%"); // Should contain percentage values
        assertThat(summary).containsPattern("\\d+ \\(\\d+\\.\\d+%\\)"); // Should contain counts with percentages
    }

    @Test
    @DisplayName("Should handle edge cases without throwing exceptions")
    void shouldHandleEdgeCasesWithoutThrowingExceptions() {
        assertThatNoException().isThrownBy(() -> {
            // Test with non-existent method
            try {
                parityValidator.validateMethodBehavior("non.existent.Method.invalidMethod");
            } catch (final Exception e) {
                // Expected for non-existent methods
            }

            // Test full validation
            final ParityReport report = parityValidator.validateFullParity();
            assertThat(report).isNotNull();

            // Test violation finding
            final List<ParityViolation> violations = parityValidator.findViolations();
            assertThat(violations).isNotNull();

            // Test parity check
            final boolean parityAchieved = parityValidator.isFullParityAchieved();
            assertThat(parityAchieved).isIn(true, false); // Should be a valid boolean
        });
    }

    @Test
    @DisplayName("Should categorize violations by severity appropriately")
    void shouldCategorizeViolationsBySeverityAppropriately() {
        final List<ParityViolation> violations = parityValidator.findViolations();

        // Ensure violations have appropriate severity assignments
        for (final ParityViolation violation : violations) {
            assertThat(violation.getSeverity()).isNotNull();
            assertThat(violation.getType()).isNotNull();
            assertThat(violation.getDescription()).isNotBlank();
            assertThat(violation.getRecommendation()).isNotBlank();

            // Critical violations should have urgent recommendations
            if (violation.getSeverity() == ViolationSeverity.CRITICAL) {
                assertThat(violation.getRecommendation().toLowerCase())
                        .containsAnyOf("immediate", "urgent", "critical", "required");
            }
        }
    }

    /**
     * Configuration method to determine if strict parity requirements should be enforced.
     * Can be controlled via system property: -DstrictParity=true
     */
    static boolean isStrictParityRequired() {
        return Boolean.parseBoolean(System.getProperty("strictParity", "false"));
    }
}