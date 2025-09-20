/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.util.List;

/**
 * Comprehensive report containing API documentation analysis results.
 *
 * <p>This report provides detailed information about the state of API documentation, including
 * coverage statistics, parity analysis, and quality metrics.
 *
 * @since 1.0.0
 */
public interface DocumentationReport {

  /**
   * Returns list of all documented API endpoints.
   *
   * <p>An endpoint is considered documented if it has:
   *
   * <ul>
   *   <li>Complete Javadoc with description
   *   <li>Parameter documentation for all parameters
   *   <li>Return value documentation
   *   <li>Exception documentation for all thrown exceptions
   * </ul>
   *
   * @return immutable list of fully documented API endpoints
   */
  List<ApiEndpoint> getDocumentedEndpoints();

  /**
   * Returns list of API endpoints missing documentation.
   *
   * <p>An endpoint is considered undocumented if it lacks:
   *
   * <ul>
   *   <li>Javadoc comments
   *   <li>Parameter descriptions
   *   <li>Return value descriptions
   *   <li>Exception documentation
   * </ul>
   *
   * @return immutable list of undocumented API endpoints
   */
  List<ApiEndpoint> getUndocumentedEndpoints();

  /**
   * Returns detailed API parity analysis between implementations.
   *
   * <p>The parity report includes:
   *
   * <ul>
   *   <li>Method-level parity status
   *   <li>Type compatibility analysis
   *   <li>Behavioral consistency validation
   *   <li>Documentation consistency checks
   * </ul>
   *
   * @return comprehensive parity analysis report
   */
  ParityReport getParityReport();

  /**
   * Returns statistical analysis of documentation coverage.
   *
   * <p>Statistics include:
   *
   * <ul>
   *   <li>Overall documentation coverage percentage
   *   <li>Coverage by module and package
   *   <li>Method vs class documentation ratios
   *   <li>Quality metrics and trends
   * </ul>
   *
   * @return comprehensive coverage statistics
   */
  CoverageStatistics getCoverageStatistics();

  /**
   * Returns list of generated code examples with validation results.
   *
   * <p>Each example includes:
   *
   * <ul>
   *   <li>Source code and compilation status
   *   <li>Runtime execution results
   *   <li>Expected vs actual output comparison
   *   <li>Cross-platform compatibility status
   * </ul>
   *
   * @return immutable list of validated code examples
   */
  List<CodeExample> getValidatedExamples();

  /**
   * Returns overall quality score for the API documentation.
   *
   * <p>The quality score is calculated based on:
   *
   * <ul>
   *   <li>Documentation completeness (40%)
   *   <li>API parity compliance (30%)
   *   <li>Example coverage and validation (20%)
   *   <li>Documentation quality and consistency (10%)
   * </ul>
   *
   * @return quality score from 0.0 to 100.0, where 100.0 represents perfect quality
   */
  double getQualityScore();

  /**
   * Returns human-readable summary of the documentation analysis.
   *
   * <p>The summary includes:
   *
   * <ul>
   *   <li>Key metrics and coverage percentages
   *   <li>Critical issues requiring attention
   *   <li>Improvement recommendations
   *   <li>Compliance status summary
   * </ul>
   *
   * @return formatted summary report for review
   */
  String getSummary();
}
