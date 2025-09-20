/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.util.List;

/**
 * Comprehensive report containing API documentation analysis results.
 *
 * <p>This report provides detailed information about the state of API documentation,
 * including coverage statistics, parity analysis, and quality metrics.
 *
 * @since 1.0.0
 */
public interface DocumentationReport {

    /**
     * Returns list of all documented API endpoints.
     *
     * <p>An endpoint is considered documented if it has:
     * <ul>
     *   <li>Complete Javadoc with description</li>
     *   <li>Parameter documentation for all parameters</li>
     *   <li>Return value documentation</li>
     *   <li>Exception documentation for all thrown exceptions</li>
     * </ul>
     *
     * @return immutable list of fully documented API endpoints
     */
    List<ApiEndpoint> getDocumentedEndpoints();

    /**
     * Returns list of API endpoints missing documentation.
     *
     * <p>An endpoint is considered undocumented if it lacks:
     * <ul>
     *   <li>Javadoc comments</li>
     *   <li>Parameter descriptions</li>
     *   <li>Return value descriptions</li>
     *   <li>Exception documentation</li>
     * </ul>
     *
     * @return immutable list of undocumented API endpoints
     */
    List<ApiEndpoint> getUndocumentedEndpoints();

    /**
     * Returns detailed API parity analysis between implementations.
     *
     * <p>The parity report includes:
     * <ul>
     *   <li>Method-level parity status</li>
     *   <li>Type compatibility analysis</li>
     *   <li>Behavioral consistency validation</li>
     *   <li>Documentation consistency checks</li>
     * </ul>
     *
     * @return comprehensive parity analysis report
     */
    ParityReport getParityReport();

    /**
     * Returns statistical analysis of documentation coverage.
     *
     * <p>Statistics include:
     * <ul>
     *   <li>Overall documentation coverage percentage</li>
     *   <li>Coverage by module and package</li>
     *   <li>Method vs class documentation ratios</li>
     *   <li>Quality metrics and trends</li>
     * </ul>
     *
     * @return comprehensive coverage statistics
     */
    CoverageStatistics getCoverageStatistics();

    /**
     * Returns list of generated code examples with validation results.
     *
     * <p>Each example includes:
     * <ul>
     *   <li>Source code and compilation status</li>
     *   <li>Runtime execution results</li>
     *   <li>Expected vs actual output comparison</li>
     *   <li>Cross-platform compatibility status</li>
     * </ul>
     *
     * @return immutable list of validated code examples
     */
    List<CodeExample> getValidatedExamples();

    /**
     * Returns overall quality score for the API documentation.
     *
     * <p>The quality score is calculated based on:
     * <ul>
     *   <li>Documentation completeness (40%)</li>
     *   <li>API parity compliance (30%)</li>
     *   <li>Example coverage and validation (20%)</li>
     *   <li>Documentation quality and consistency (10%)</li>
     * </ul>
     *
     * @return quality score from 0.0 to 100.0, where 100.0 represents perfect quality
     */
    double getQualityScore();

    /**
     * Returns human-readable summary of the documentation analysis.
     *
     * <p>The summary includes:
     * <ul>
     *   <li>Key metrics and coverage percentages</li>
     *   <li>Critical issues requiring attention</li>
     *   <li>Improvement recommendations</li>
     *   <li>Compliance status summary</li>
     * </ul>
     *
     * @return formatted summary report for review
     */
    String getSummary();
}