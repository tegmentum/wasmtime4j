/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.util.Map;
import java.util.Objects;

/**
 * Statistical analysis of API documentation coverage.
 *
 * <p>Provides comprehensive metrics about the state of documentation
 * across the wasmtime4j API surface.
 *
 * @since 1.0.0
 */
public final class CoverageStatistics {

    private final int totalEndpoints;
    private final int documentedEndpoints;
    private final double overallCoveragePercentage;
    private final Map<String, Double> coverageByModule;
    private final Map<String, Double> coverageByPackage;
    private final Map<DocumentationQuality, Integer> qualityDistribution;
    private final int totalMethods;
    private final int totalClasses;
    private final int totalInterfaces;

    /**
     * Creates new coverage statistics.
     *
     * @param totalEndpoints total number of API endpoints analyzed
     * @param documentedEndpoints number of endpoints with complete documentation
     * @param overallCoveragePercentage overall documentation coverage percentage
     * @param coverageByModule map of module names to coverage percentages
     * @param coverageByPackage map of package names to coverage percentages
     * @param qualityDistribution distribution of documentation quality levels
     * @param totalMethods total number of methods analyzed
     * @param totalClasses total number of classes analyzed
     * @param totalInterfaces total number of interfaces analyzed
     */
    public CoverageStatistics(final int totalEndpoints,
                              final int documentedEndpoints,
                              final double overallCoveragePercentage,
                              final Map<String, Double> coverageByModule,
                              final Map<String, Double> coverageByPackage,
                              final Map<DocumentationQuality, Integer> qualityDistribution,
                              final int totalMethods,
                              final int totalClasses,
                              final int totalInterfaces) {
        this.totalEndpoints = totalEndpoints;
        this.documentedEndpoints = documentedEndpoints;
        this.overallCoveragePercentage = overallCoveragePercentage;
        this.coverageByModule = Map.copyOf(Objects.requireNonNull(coverageByModule, "coverageByModule"));
        this.coverageByPackage = Map.copyOf(Objects.requireNonNull(coverageByPackage, "coverageByPackage"));
        this.qualityDistribution = Map.copyOf(Objects.requireNonNull(qualityDistribution, "qualityDistribution"));
        this.totalMethods = totalMethods;
        this.totalClasses = totalClasses;
        this.totalInterfaces = totalInterfaces;
    }

    /**
     * Returns the total number of API endpoints analyzed.
     *
     * @return total endpoint count
     */
    public int getTotalEndpoints() {
        return totalEndpoints;
    }

    /**
     * Returns the number of endpoints with complete documentation.
     *
     * @return documented endpoint count
     */
    public int getDocumentedEndpoints() {
        return documentedEndpoints;
    }

    /**
     * Returns the number of endpoints missing documentation.
     *
     * @return undocumented endpoint count
     */
    public int getUndocumentedEndpoints() {
        return totalEndpoints - documentedEndpoints;
    }

    /**
     * Returns the overall documentation coverage percentage.
     *
     * @return coverage percentage from 0.0 to 100.0
     */
    public double getOverallCoveragePercentage() {
        return overallCoveragePercentage;
    }

    /**
     * Returns documentation coverage percentages by module.
     *
     * @return immutable map of module names to coverage percentages
     */
    public Map<String, Double> getCoverageByModule() {
        return coverageByModule;
    }

    /**
     * Returns documentation coverage percentages by package.
     *
     * @return immutable map of package names to coverage percentages
     */
    public Map<String, Double> getCoverageByPackage() {
        return coverageByPackage;
    }

    /**
     * Returns distribution of documentation quality levels.
     *
     * @return immutable map of quality levels to endpoint counts
     */
    public Map<DocumentationQuality, Integer> getQualityDistribution() {
        return qualityDistribution;
    }

    /**
     * Returns the total number of methods analyzed.
     *
     * @return total method count
     */
    public int getTotalMethods() {
        return totalMethods;
    }

    /**
     * Returns the total number of classes analyzed.
     *
     * @return total class count
     */
    public int getTotalClasses() {
        return totalClasses;
    }

    /**
     * Returns the total number of interfaces analyzed.
     *
     * @return total interface count
     */
    public int getTotalInterfaces() {
        return totalInterfaces;
    }

    /**
     * Checks if the coverage meets the target threshold.
     *
     * @param targetPercentage the target coverage percentage
     * @return {@code true} if coverage meets or exceeds target, {@code false} otherwise
     */
    public boolean meetsTarget(final double targetPercentage) {
        return overallCoveragePercentage >= targetPercentage;
    }

    @Override
    public String toString() {
        return "CoverageStatistics{"
                + "totalEndpoints=" + totalEndpoints
                + ", documentedEndpoints=" + documentedEndpoints
                + ", coveragePercentage=" + String.format("%.2f%%", overallCoveragePercentage)
                + ", totalMethods=" + totalMethods
                + ", totalClasses=" + totalClasses
                + ", totalInterfaces=" + totalInterfaces
                + '}';
    }
}