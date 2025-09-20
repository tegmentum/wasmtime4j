/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import ai.tegmentum.wasmtime4j.documentation.impl.DefaultApiDocumentationGenerator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Comprehensive validation tests for API documentation coverage and quality.
 *
 * <p>These tests validate that all public APIs have complete documentation and meet the project's
 * documentation quality standards.
 *
 * @since 1.0.0
 */
@DisplayName("API Documentation Validation")
class ApiDocumentationValidationTest {

  private ApiDocumentationGenerator documentationGenerator;

  @BeforeEach
  void setUp() {
    final String sourceRootPath = System.getProperty("user.dir") + "/src/main/java";
    final List<String> packagePrefixes =
        List.of(
            "ai.tegmentum.wasmtime4j",
            "ai.tegmentum.wasmtime4j.jni",
            "ai.tegmentum.wasmtime4j.panama");

    documentationGenerator = new DefaultApiDocumentationGenerator(sourceRootPath, packagePrefixes);
  }

  @Test
  @DisplayName("Should generate comprehensive documentation report")
  void shouldGenerateComprehensiveDocumentationReport() {
    assertThatNoException()
        .isThrownBy(
            () -> {
              final DocumentationReport report = documentationGenerator.generateReport();

              assertThat(report).isNotNull();
              assertThat(report.getCoverageStatistics()).isNotNull();
              assertThat(report.getParityReport()).isNotNull();
              assertThat(report.getValidatedExamples()).isNotNull();
            });
  }

  @Test
  @DisplayName("Should achieve 100% API documentation coverage")
  @EnabledIf("isStrictDocumentationRequired")
  void shouldAchieve100PercentDocumentationCoverage() {
    final DocumentationReport report = documentationGenerator.generateReport();
    final CoverageStatistics stats = report.getCoverageStatistics();

    assertThat(stats.getOverallCoveragePercentage())
        .withFailMessage(
            "API documentation coverage is %.2f%%, expected 100%%",
            stats.getOverallCoveragePercentage())
        .isEqualTo(100.0);

    assertThat(report.getUndocumentedEndpoints())
        .withFailMessage(
            "Found %d undocumented endpoints: %s",
            report.getUndocumentedEndpoints().size(), report.getUndocumentedEndpoints())
        .isEmpty();
  }

  @Test
  @DisplayName("Should meet minimum documentation coverage threshold")
  void shouldMeetMinimumDocumentationCoverageThreshold() {
    final DocumentationReport report = documentationGenerator.generateReport();
    final CoverageStatistics stats = report.getCoverageStatistics();

    final double minimumThreshold = 80.0; // 80% minimum coverage
    assertThat(stats.getOverallCoveragePercentage())
        .withFailMessage(
            "API documentation coverage is %.2f%%, expected at least %.2f%%",
            stats.getOverallCoveragePercentage(), minimumThreshold)
        .isGreaterThanOrEqualTo(minimumThreshold);
  }

  @Test
  @DisplayName("Should have documented endpoints for all critical API classes")
  void shouldHaveDocumentedEndpointsForCriticalApiClasses() {
    final DocumentationReport report = documentationGenerator.generateReport();
    final List<ApiEndpoint> documentedEndpoints = report.getDocumentedEndpoints();

    // Verify critical API classes have documented endpoints
    final List<String> criticalClasses =
        List.of(
            "Engine",
            "Module",
            "Instance",
            "Store",
            "Linker",
            "WasmMemory",
            "WasmTable",
            "WasmGlobal",
            "HostFunction");

    for (final String className : criticalClasses) {
      final boolean hasDocumentedEndpoints =
          documentedEndpoints.stream()
              .anyMatch(endpoint -> endpoint.getClassName().contains(className));

      assertThat(hasDocumentedEndpoints)
          .withFailMessage("Critical API class %s has no documented endpoints", className)
          .isTrue();
    }
  }

  @Test
  @DisplayName("Should achieve high documentation quality score")
  void shouldAchieveHighDocumentationQualityScore() {
    final DocumentationReport report = documentationGenerator.generateReport();
    final double qualityScore = report.getQualityScore();

    final double minimumQualityScore = 85.0; // 85% minimum quality
    assertThat(qualityScore)
        .withFailMessage(
            "Documentation quality score is %.2f, expected at least %.2f",
            qualityScore, minimumQualityScore)
        .isGreaterThanOrEqualTo(minimumQualityScore);
  }

  @Test
  @DisplayName("Should have consistent documentation quality across modules")
  void shouldHaveConsistentDocumentationQualityAcrossModules() {
    final DocumentationReport report = documentationGenerator.generateReport();
    final CoverageStatistics stats = report.getCoverageStatistics();

    final var coverageByModule = stats.getCoverageByModule();
    final double minimumModuleCoverage = 75.0; // 75% minimum per module

    for (final var entry : coverageByModule.entrySet()) {
      final String moduleName = entry.getKey();
      final double coverage = entry.getValue();

      assertThat(coverage)
          .withFailMessage(
              "Module %s has %.2f%% coverage, expected at least %.2f%%",
              moduleName, coverage, minimumModuleCoverage)
          .isGreaterThanOrEqualTo(minimumModuleCoverage);
    }
  }

  @Test
  @DisplayName("Should generate and validate code examples")
  void shouldGenerateAndValidateCodeExamples() {
    assertThatNoException()
        .isThrownBy(
            () -> {
              documentationGenerator.generateExamples();
              documentationGenerator.validateExamples();
            });

    final DocumentationReport report = documentationGenerator.generateReport();
    final List<CodeExample> examples = report.getValidatedExamples();

    assertThat(examples).isNotEmpty();

    final long validatedExampleCount =
        examples.stream().filter(CodeExample::isFullyValidated).count();

    assertThat(validatedExampleCount)
        .withFailMessage(
            "Only %d of %d examples are fully validated", validatedExampleCount, examples.size())
        .isEqualTo(examples.size());
  }

  @Test
  @DisplayName("Should provide actionable documentation report summary")
  void shouldProvideActionableDocumentationReportSummary() {
    final DocumentationReport report = documentationGenerator.generateReport();
    final String summary = report.getSummary();

    assertThat(summary)
        .isNotBlank()
        .contains("Documentation Coverage")
        .contains("API Parity Compliance")
        .contains("Overall Quality Score");

    // Summary should provide specific metrics
    assertThat(summary).containsPattern("\\d+\\.\\d+%"); // Should contain percentage values
  }

  @Test
  @DisplayName("Should detect and report undocumented API endpoints")
  void shouldDetectAndReportUndocumentedApiEndpoints() {
    final DocumentationReport report = documentationGenerator.generateReport();
    final List<ApiEndpoint> undocumentedEndpoints = report.getUndocumentedEndpoints();

    // If there are undocumented endpoints, they should have proper metadata
    for (final ApiEndpoint endpoint : undocumentedEndpoints) {
      assertThat(endpoint.isDocumented()).isFalse();
      assertThat(endpoint.getMissingDocumentation()).isNotEmpty();
      assertThat(endpoint.getQuality()).isNotNull();
    }

    // Log undocumented endpoints for manual review
    if (!undocumentedEndpoints.isEmpty()) {
      System.out.println(
          "WARNING: Found " + undocumentedEndpoints.size() + " undocumented endpoints:");
      undocumentedEndpoints.forEach(
          endpoint -> System.out.println("  - " + endpoint.getFullyQualifiedName()));
    }
  }

  /**
   * Configuration method to determine if strict documentation requirements should be enforced. Can
   * be controlled via system property: -DstrictDocumentation=true
   */
  static boolean isStrictDocumentationRequired() {
    return Boolean.parseBoolean(System.getProperty("strictDocumentation", "false"));
  }
}
