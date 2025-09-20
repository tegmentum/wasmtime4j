/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation.impl;

import ai.tegmentum.wasmtime4j.documentation.ApiEndpoint;
import ai.tegmentum.wasmtime4j.documentation.CodeExample;
import ai.tegmentum.wasmtime4j.documentation.CoverageStatistics;
import ai.tegmentum.wasmtime4j.documentation.DocumentationReport;
import ai.tegmentum.wasmtime4j.documentation.ParityReport;

import java.util.List;
import java.util.Objects;

/**
 * Default implementation of documentation report.
 *
 * <p>Provides comprehensive documentation analysis results including
 * coverage statistics, parity validation, and quality metrics.
 *
 * @since 1.0.0
 */
public final class DefaultDocumentationReport implements DocumentationReport {

    private final List<ApiEndpoint> documentedEndpoints;
    private final List<ApiEndpoint> undocumentedEndpoints;
    private final ParityReport parityReport;
    private final CoverageStatistics coverageStatistics;
    private final List<CodeExample> validatedExamples;

    /**
     * Creates a new documentation report.
     *
     * @param documentedEndpoints list of documented API endpoints
     * @param undocumentedEndpoints list of undocumented API endpoints
     * @param parityReport API parity analysis report
     * @param coverageStatistics documentation coverage statistics
     * @param validatedExamples list of validated code examples
     */
    public DefaultDocumentationReport(final List<ApiEndpoint> documentedEndpoints,
                                      final List<ApiEndpoint> undocumentedEndpoints,
                                      final ParityReport parityReport,
                                      final CoverageStatistics coverageStatistics,
                                      final List<CodeExample> validatedExamples) {
        this.documentedEndpoints = List.copyOf(Objects.requireNonNull(documentedEndpoints, "documentedEndpoints"));
        this.undocumentedEndpoints = List.copyOf(Objects.requireNonNull(undocumentedEndpoints, "undocumentedEndpoints"));
        this.parityReport = Objects.requireNonNull(parityReport, "parityReport");
        this.coverageStatistics = Objects.requireNonNull(coverageStatistics, "coverageStatistics");
        this.validatedExamples = List.copyOf(Objects.requireNonNull(validatedExamples, "validatedExamples"));
    }

    @Override
    public List<ApiEndpoint> getDocumentedEndpoints() {
        return documentedEndpoints;
    }

    @Override
    public List<ApiEndpoint> getUndocumentedEndpoints() {
        return undocumentedEndpoints;
    }

    @Override
    public ParityReport getParityReport() {
        return parityReport;
    }

    @Override
    public CoverageStatistics getCoverageStatistics() {
        return coverageStatistics;
    }

    @Override
    public List<CodeExample> getValidatedExamples() {
        return validatedExamples;
    }

    @Override
    public double getQualityScore() {
        // Calculate quality score based on multiple factors
        final double documentationWeight = 0.4;
        final double parityWeight = 0.3;
        final double exampleWeight = 0.2;
        final double consistencyWeight = 0.1;

        final double documentationScore = coverageStatistics.getOverallCoveragePercentage();
        final double parityScore = parityReport.getCompliancePercentage();
        final double exampleScore = calculateExampleScore();
        final double consistencyScore = calculateConsistencyScore();

        return (documentationScore * documentationWeight)
                + (parityScore * parityWeight)
                + (exampleScore * exampleWeight)
                + (consistencyScore * consistencyWeight);
    }

    @Override
    public String getSummary() {
        final StringBuilder summary = new StringBuilder();

        summary.append("=== API Documentation Analysis Summary ===\n\n");

        // Coverage Statistics
        summary.append(String.format("Documentation Coverage: %.2f%% (%d/%d endpoints)\n",
                coverageStatistics.getOverallCoveragePercentage(),
                coverageStatistics.getDocumentedEndpoints(),
                coverageStatistics.getTotalEndpoints()));

        // Parity Status
        summary.append(String.format("API Parity Compliance: %.2f%% (%s)\n",
                parityReport.getCompliancePercentage(),
                parityReport.isCompleteParityAchieved() ? "ACHIEVED" : "VIOLATIONS DETECTED"));

        // Example Validation
        final long validatedExampleCount = validatedExamples.stream()
                .filter(CodeExample::isFullyValidated)
                .count();
        summary.append(String.format("Code Examples: %d total, %d validated (%.2f%%)\n",
                validatedExamples.size(),
                validatedExampleCount,
                validatedExamples.isEmpty() ? 0.0 : (double) validatedExampleCount / validatedExamples.size() * 100.0));

        // Overall Quality Score
        summary.append(String.format("Overall Quality Score: %.2f/100.0\n\n",
                getQualityScore()));

        // Critical Issues
        summary.append("=== Critical Issues ===\n");
        if (undocumentedEndpoints.isEmpty()) {
            summary.append("✓ No undocumented endpoints\n");
        } else {
            summary.append(String.format("⚠ %d undocumented endpoints require attention\n",
                    undocumentedEndpoints.size()));
        }

        if (parityReport.isCompleteParityAchieved()) {
            summary.append("✓ Complete API parity achieved\n");
        } else {
            summary.append(String.format("⚠ %d parity violations detected\n",
                    parityReport.getAllViolations().size()));
        }

        // Recommendations
        summary.append("\n=== Recommendations ===\n");
        if (!undocumentedEndpoints.isEmpty()) {
            summary.append("1. Add Javadoc documentation for undocumented endpoints\n");
        }
        if (!parityReport.isCompleteParityAchieved()) {
            summary.append("2. Resolve API parity violations between implementations\n");
        }
        if (validatedExampleCount < validatedExamples.size()) {
            summary.append("3. Fix failing code examples\n");
        }
        if (getQualityScore() < 90.0) {
            summary.append("4. Improve documentation quality and consistency\n");
        }

        return summary.toString();
    }

    private double calculateExampleScore() {
        if (validatedExamples.isEmpty()) {
            return 0.0;
        }

        final long validatedCount = validatedExamples.stream()
                .filter(CodeExample::isFullyValidated)
                .count();

        return (double) validatedCount / validatedExamples.size() * 100.0;
    }

    private double calculateConsistencyScore() {
        // Simplified consistency calculation based on documentation quality distribution
        final var qualityDistribution = coverageStatistics.getQualityDistribution();
        final int totalEndpoints = documentedEndpoints.size() + undocumentedEndpoints.size();

        if (totalEndpoints == 0) {
            return 100.0;
        }

        // Higher score for consistent high-quality documentation
        final int excellentCount = qualityDistribution.getOrDefault(
                ai.tegmentum.wasmtime4j.documentation.DocumentationQuality.EXCELLENT, 0);
        final int goodCount = qualityDistribution.getOrDefault(
                ai.tegmentum.wasmtime4j.documentation.DocumentationQuality.GOOD, 0);

        final double highQualityRatio = (double) (excellentCount + goodCount) / totalEndpoints;
        return highQualityRatio * 100.0;
    }

    @Override
    public String toString() {
        return "DefaultDocumentationReport{"
                + "documentedEndpoints=" + documentedEndpoints.size()
                + ", undocumentedEndpoints=" + undocumentedEndpoints.size()
                + ", coveragePercentage=" + String.format("%.2f%%", coverageStatistics.getOverallCoveragePercentage())
                + ", parityCompliance=" + String.format("%.2f%%", parityReport.getCompliancePercentage())
                + ", qualityScore=" + String.format("%.2f", getQualityScore())
                + '}';
    }
}