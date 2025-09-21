/*
 * Copyright 2024 Tegmentum AI Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.testing;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Comprehensive test report generator that creates detailed HTML and JSON reports.
 *
 * <p>This generator creates comprehensive validation reports including:
 *
 * <ul>
 *   <li>API coverage analysis and validation results
 *   <li>Functional testing results with detailed error information
 *   <li>JNI vs Panama parity analysis and violations
 *   <li>Real-world scenario testing outcomes
 *   <li>Performance benchmarking results and comparisons
 *   <li>Memory leak detection findings
 *   <li>Cross-platform compatibility validation
 *   <li>Executive summary and production readiness assessment
 * </ul>
 */
public final class TestReportGenerator {

  private static final Logger LOGGER = Logger.getLogger(TestReportGenerator.class.getName());

  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final Map<String, Object> reportData = new HashMap<>();
  private final Instant generationStart = Instant.now();

  public static TestReportGenerator create() {
    return new TestReportGenerator();
  }

  /**
   * Adds API coverage validation results to the report.
   *
   * @param coverageReport the coverage validation results
   * @return this generator for method chaining
   */
  public TestReportGenerator addCoverageReport(final CoverageReport coverageReport) {
    final Map<String, Object> coverage = new HashMap<>();
    coverage.put("totalCoverage", coverageReport.getTotalCoveragePercentage());
    coverage.put("coverageByModule", coverageReport.getCoverageByModule());
    coverage.put("implementedApis", coverageReport.getImplementedApis());
    coverage.put("missingApis", coverageReport.getMissingApis());
    coverage.put("partiallyImplementedApis", coverageReport.getPartiallyImplementedApis());
    coverage.put("isProductionReady", coverageReport.isProductionReady());
    coverage.put("detailedCoverage", extractDetailedCoverage(coverageReport));

    reportData.put("apiCoverage", coverage);
    return this;
  }

  /**
   * Adds functional testing results to the report.
   *
   * @param functionalResults the functional testing results
   * @return this generator for method chaining
   */
  public TestReportGenerator addFunctionalResults(final TestResults functionalResults) {
    final Map<String, Object> functional = new HashMap<>();
    functional.put("totalTests", functionalResults.getTotalTests());
    functional.put("passedTests", functionalResults.getPassedTests());
    functional.put("failedTests", functionalResults.getFailedTests());
    functional.put("successRate", functionalResults.getSuccessRate());
    functional.put("hasFailures", functionalResults.hasFailures());
    functional.put("failures", extractFailureDetails(functionalResults));
    functional.put("executionTime", functionalResults.getTotalExecutionTime().toMillis());

    reportData.put("functionalTesting", functional);
    return this;
  }

  /**
   * Adds JNI vs Panama parity validation results to the report.
   *
   * @param parityResults the parity validation results
   * @return this generator for method chaining
   */
  public TestReportGenerator addParityResults(final TestResults parityResults) {
    final Map<String, Object> parity = new HashMap<>();
    parity.put("totalTests", parityResults.getTotalTests());
    parity.put("passedTests", parityResults.getPassedTests());
    parity.put("failedTests", parityResults.getFailedTests());
    parity.put("successRate", parityResults.getSuccessRate());
    parity.put("violations", extractParityViolations(parityResults));
    parity.put("executionTime", parityResults.getTotalExecutionTime().toMillis());

    reportData.put("parityValidation", parity);
    return this;
  }

  /**
   * Adds real-world scenario testing results to the report.
   *
   * @param realWorldResults the real-world testing results
   * @return this generator for method chaining
   */
  public TestReportGenerator addRealWorldResults(final TestResults realWorldResults) {
    final Map<String, Object> realWorld = new HashMap<>();
    realWorld.put("totalTests", realWorldResults.getTotalTests());
    realWorld.put("passedTests", realWorldResults.getPassedTests());
    realWorld.put("failedTests", realWorldResults.getFailedTests());
    realWorld.put("successRate", realWorldResults.getSuccessRate());
    realWorld.put("scenarios", extractScenarioDetails(realWorldResults));
    realWorld.put("executionTime", realWorldResults.getTotalExecutionTime().toMillis());

    reportData.put("realWorldTesting", realWorld);
    return this;
  }

  /**
   * Adds performance benchmarking results to the report.
   *
   * @param performanceResults the performance testing results
   * @return this generator for method chaining
   */
  public TestReportGenerator addPerformanceResults(final TestResults performanceResults) {
    final Map<String, Object> performance = new HashMap<>();
    performance.put("totalTests", performanceResults.getTotalTests());
    performance.put("passedTests", performanceResults.getPassedTests());
    performance.put("failedTests", performanceResults.getFailedTests());
    performance.put("benchmarks", extractPerformanceBenchmarks(performanceResults));
    performance.put("executionTime", performanceResults.getTotalExecutionTime().toMillis());

    reportData.put("performanceTesting", performance);
    return this;
  }

  /**
   * Adds memory leak detection results to the report.
   *
   * @param memoryResults the memory leak detection results
   * @return this generator for method chaining
   */
  public TestReportGenerator addMemoryLeakResults(final TestResults memoryResults) {
    final Map<String, Object> memory = new HashMap<>();
    memory.put("totalTests", memoryResults.getTotalTests());
    memory.put("passedTests", memoryResults.getPassedTests());
    memory.put("failedTests", memoryResults.getFailedTests());
    memory.put("leakDetection", extractMemoryLeakDetails(memoryResults));
    memory.put("executionTime", memoryResults.getTotalExecutionTime().toMillis());

    reportData.put("memoryLeakDetection", memory);
    return this;
  }

  /**
   * Adds cross-platform compatibility results to the report.
   *
   * @param crossPlatformResults the cross-platform testing results
   * @return this generator for method chaining
   */
  public TestReportGenerator addCrossPlatformResults(final TestResults crossPlatformResults) {
    final Map<String, Object> crossPlatform = new HashMap<>();
    crossPlatform.put("totalTests", crossPlatformResults.getTotalTests());
    crossPlatform.put("passedTests", crossPlatformResults.getPassedTests());
    crossPlatform.put("failedTests", crossPlatformResults.getFailedTests());
    crossPlatform.put("platformCompatibility", extractPlatformCompatibility(crossPlatformResults));
    crossPlatform.put("executionTime", crossPlatformResults.getTotalExecutionTime().toMillis());

    reportData.put("crossPlatformTesting", crossPlatform);
    return this;
  }

  /**
   * Generates the comprehensive validation report.
   *
   * @param outputPath the path where the report should be saved
   */
  public void generateReport(final String outputPath) {
    LOGGER.info("Generating comprehensive validation report at: " + outputPath);

    try {
      // Prepare report metadata
      final Map<String, Object> metadata = new HashMap<>();
      metadata.put("generatedAt", LocalDateTime.now().format(TIMESTAMP_FORMAT));
      metadata.put("generationDuration", Duration.between(generationStart, Instant.now()).toMillis());
      metadata.put("javaVersion", System.getProperty("java.version"));
      metadata.put("osName", System.getProperty("os.name"));
      metadata.put("osArch", System.getProperty("os.arch"));
      reportData.put("metadata", metadata);

      // Calculate executive summary
      reportData.put("executiveSummary", generateExecutiveSummary());

      // Generate HTML report
      final Path htmlPath = Paths.get(outputPath);
      generateHtmlReport(htmlPath);

      // Generate JSON report
      final Path jsonPath = Paths.get(outputPath.replace(".html", ".json"));
      generateJsonReport(jsonPath);

      LOGGER.info("Comprehensive validation report generated successfully");

    } catch (final Exception e) {
      LOGGER.severe("Failed to generate validation report: " + e.getMessage());
      throw new RuntimeException("Report generation failed", e);
    }
  }

  // Private helper methods for data extraction

  private Map<String, Object> extractDetailedCoverage(final CoverageReport coverageReport) {
    final Map<String, Object> detailed = new HashMap<>();
    // Extract detailed coverage information
    detailed.put("totalApis", coverageReport.getCoverageByModule().size());
    detailed.put("implementedCount", coverageReport.getImplementedApis().size());
    detailed.put("missingCount", coverageReport.getMissingApis().size());
    detailed.put("partialCount", coverageReport.getPartiallyImplementedApis().size());
    return detailed;
  }

  private List<Map<String, Object>> extractFailureDetails(final TestResults results) {
    final List<Map<String, Object>> failures = new ArrayList<>();
    for (final TestFailure failure : results.getFailures()) {
      final Map<String, Object> failureDetail = new HashMap<>();
      failureDetail.put("testName", failure.getTestName());
      failureDetail.put("errorMessage", failure.getErrorMessage());
      failureDetail.put("executionTime", failure.getExecutionTime().toMillis());
      failures.add(failureDetail);
    }
    return failures;
  }

  private List<Map<String, Object>> extractParityViolations(final TestResults results) {
    final List<Map<String, Object>> violations = new ArrayList<>();
    // Extract parity violations from test results
    // This would normally access specific parity violation data
    return violations;
  }

  private Map<String, Object> extractScenarioDetails(final TestResults results) {
    final Map<String, Object> scenarios = new HashMap<>();
    scenarios.put("webServicePlugin", extractScenarioResult(results, "web_service"));
    scenarios.put("dataProcessingPipeline", extractScenarioResult(results, "data_processing"));
    scenarios.put("serverlessExecution", extractScenarioResult(results, "serverless"));
    scenarios.put("highLoadScenarios", extractScenarioResult(results, "high_load"));
    return scenarios;
  }

  private Map<String, Object> extractScenarioResult(final TestResults results, final String scenarioPrefix) {
    final Map<String, Object> scenario = new HashMap<>();
    final long scenarioTests = results.getFailures().stream()
        .filter(f -> f.getTestName().startsWith(scenarioPrefix))
        .count();
    scenario.put("testsRun", scenarioTests);
    scenario.put("successful", scenarioTests == 0);
    return scenario;
  }

  private Map<String, Object> extractPerformanceBenchmarks(final TestResults results) {
    final Map<String, Object> benchmarks = new HashMap<>();
    benchmarks.put("moduleCompilation", extractBenchmarkResult(results, "module_compilation"));
    benchmarks.put("functionCalls", extractBenchmarkResult(results, "function_calls"));
    benchmarks.put("memoryOperations", extractBenchmarkResult(results, "memory_operations"));
    benchmarks.put("concurrentExecution", extractBenchmarkResult(results, "concurrent_execution"));
    return benchmarks;
  }

  private Map<String, Object> extractBenchmarkResult(final TestResults results, final String benchmarkName) {
    final Map<String, Object> benchmark = new HashMap<>();
    final boolean passed = results.getFailures().stream()
        .noneMatch(f -> f.getTestName().contains(benchmarkName));
    benchmark.put("passed", passed);
    benchmark.put("executionTime", results.getTotalExecutionTime().toMillis());
    return benchmark;
  }

  private Map<String, Object> extractMemoryLeakDetails(final TestResults results) {
    final Map<String, Object> leakDetails = new HashMap<>();
    leakDetails.put("moduleLifecycleLeaks", extractLeakResult(results, "module_lifecycle"));
    leakDetails.put("instanceLifecycleLeaks", extractLeakResult(results, "instance_lifecycle"));
    leakDetails.put("asyncOperationLeaks", extractLeakResult(results, "async_operation"));
    leakDetails.put("wasiContextLeaks", extractLeakResult(results, "wasi_context"));
    return leakDetails;
  }

  private Map<String, Object> extractLeakResult(final TestResults results, final String leakType) {
    final Map<String, Object> leak = new HashMap<>();
    final boolean hasLeaks = results.getFailures().stream()
        .anyMatch(f -> f.getTestName().contains(leakType));
    leak.put("hasLeaks", hasLeaks);
    leak.put("testPassed", !hasLeaks);
    return leak;
  }

  private Map<String, Object> extractPlatformCompatibility(final TestResults results) {
    final Map<String, Object> compatibility = new HashMap<>();
    compatibility.put("currentPlatform", extractPlatformResult(results, "current_platform"));
    compatibility.put("serialization", extractPlatformResult(results, "serialization"));
    compatibility.put("nativeLibraryLoading", extractPlatformResult(results, "native_library"));
    return compatibility;
  }

  private Map<String, Object> extractPlatformResult(final TestResults results, final String platformTest) {
    final Map<String, Object> platform = new HashMap<>();
    final boolean passed = results.getFailures().stream()
        .noneMatch(f -> f.getTestName().contains(platformTest));
    platform.put("passed", passed);
    return platform;
  }

  private Map<String, Object> generateExecutiveSummary() {
    final Map<String, Object> summary = new HashMap<>();

    // Calculate overall health score
    double overallScore = 0.0;
    int categoryCount = 0;

    // API Coverage Score (40% weight)
    final Object apiCoverage = reportData.get("apiCoverage");
    if (apiCoverage instanceof Map) {
      final Map<?, ?> coverage = (Map<?, ?>) apiCoverage;
      final Object totalCoverage = coverage.get("totalCoverage");
      if (totalCoverage instanceof Number) {
        overallScore += ((Number) totalCoverage).doubleValue() * 0.4;
        categoryCount++;
      }
    }

    // Functional Testing Score (20% weight)
    final Object functional = reportData.get("functionalTesting");
    if (functional instanceof Map) {
      final Map<?, ?> functionalMap = (Map<?, ?>) functional;
      final Object successRate = functionalMap.get("successRate");
      if (successRate instanceof Number) {
        overallScore += ((Number) successRate).doubleValue() * 0.2;
        categoryCount++;
      }
    }

    // Parity Validation Score (15% weight)
    final Object parity = reportData.get("parityValidation");
    if (parity instanceof Map) {
      final Map<?, ?> parityMap = (Map<?, ?>) parity;
      final Object successRate = parityMap.get("successRate");
      if (successRate instanceof Number) {
        overallScore += ((Number) successRate).doubleValue() * 0.15;
        categoryCount++;
      }
    }

    // Real-world Testing Score (10% weight)
    final Object realWorld = reportData.get("realWorldTesting");
    if (realWorld instanceof Map) {
      final Map<?, ?> realWorldMap = (Map<?, ?>) realWorld;
      final Object successRate = realWorldMap.get("successRate");
      if (successRate instanceof Number) {
        overallScore += ((Number) successRate).doubleValue() * 0.1;
        categoryCount++;
      }
    }

    // Performance Testing Score (10% weight)
    final Object performance = reportData.get("performanceTesting");
    if (performance instanceof Map) {
      final Map<?, ?> performanceMap = (Map<?, ?>) performance;
      final Object successRate = performanceMap.get("successRate");
      if (successRate instanceof Number) {
        overallScore += ((Number) successRate).doubleValue() * 0.1;
        categoryCount++;
      }
    }

    // Memory Leak Detection Score (5% weight)
    final Object memory = reportData.get("memoryLeakDetection");
    if (memory instanceof Map) {
      final Map<?, ?> memoryMap = (Map<?, ?>) memory;
      final Object successRate = memoryMap.get("successRate");
      if (successRate instanceof Number) {
        overallScore += ((Number) successRate).doubleValue() * 0.05;
        categoryCount++;
      }
    }

    summary.put("overallScore", overallScore);
    summary.put("isProductionReady", overallScore >= 95.0);
    summary.put("qualityGrade", calculateQualityGrade(overallScore));
    summary.put("recommendations", generateRecommendations(overallScore));

    return summary;
  }

  private String calculateQualityGrade(final double score) {
    if (score >= 98.0) return "A+";
    if (score >= 95.0) return "A";
    if (score >= 90.0) return "A-";
    if (score >= 85.0) return "B+";
    if (score >= 80.0) return "B";
    if (score >= 75.0) return "B-";
    if (score >= 70.0) return "C+";
    if (score >= 65.0) return "C";
    if (score >= 60.0) return "C-";
    return "F";
  }

  private List<String> generateRecommendations(final double score) {
    final List<String> recommendations = new ArrayList<>();

    if (score < 95.0) {
      recommendations.add("Address failing tests before production deployment");
    }
    if (score < 90.0) {
      recommendations.add("Review and improve API coverage to meet production standards");
    }
    if (score < 85.0) {
      recommendations.add("Investigate performance bottlenecks and optimization opportunities");
    }
    if (score < 80.0) {
      recommendations.add("Fix critical parity violations between JNI and Panama implementations");
    }
    if (score < 75.0) {
      recommendations.add("Address memory leaks and resource management issues");
    }

    if (recommendations.isEmpty()) {
      recommendations.add("Wasmtime4j meets all production readiness criteria");
      recommendations.add("Continue monitoring and testing in production environments");
    }

    return recommendations;
  }

  private void generateHtmlReport(final Path outputPath) throws IOException {
    final StringBuilder html = new StringBuilder();

    html.append("<!DOCTYPE html>\n");
    html.append("<html lang=\"en\">\n");
    html.append("<head>\n");
    html.append("    <meta charset=\"UTF-8\">\n");
    html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
    html.append("    <title>Wasmtime4j Comprehensive Validation Report</title>\n");
    html.append("    <style>\n");
    html.append(generateCss());
    html.append("    </style>\n");
    html.append("</head>\n");
    html.append("<body>\n");

    // Header
    html.append("    <header class=\"header\">\n");
    html.append("        <h1>Wasmtime4j Comprehensive Validation Report</h1>\n");
    html.append("        <p class=\"subtitle\">Complete API Coverage and Production Readiness Assessment</p>\n");
    final Object metadata = reportData.get("metadata");
    if (metadata instanceof Map) {
      final Map<?, ?> meta = (Map<?, ?>) metadata;
      html.append("        <p class=\"timestamp\">Generated: ").append(meta.get("generatedAt")).append("</p>\n");
    }
    html.append("    </header>\n");

    // Executive Summary
    html.append(generateExecutiveSummaryHtml());

    // Main content sections
    html.append("    <main class=\"main-content\">\n");
    html.append(generateApiCoverageHtml());
    html.append(generateFunctionalTestingHtml());
    html.append(generateParityValidationHtml());
    html.append(generateRealWorldTestingHtml());
    html.append(generatePerformanceTestingHtml());
    html.append(generateMemoryLeakDetectionHtml());
    html.append(generateCrossPlatformTestingHtml());
    html.append("    </main>\n");

    // Footer
    html.append("    <footer class=\"footer\">\n");
    html.append("        <p>Generated by Wasmtime4j Comprehensive Test Suite</p>\n");
    html.append("    </footer>\n");

    html.append("</body>\n");
    html.append("</html>\n");

    Files.write(outputPath, html.toString().getBytes());
  }

  private void generateJsonReport(final Path outputPath) throws IOException {
    // Simple JSON generation (in production, use a proper JSON library)
    final StringBuilder json = new StringBuilder();
    json.append("{\n");

    boolean first = true;
    for (final Map.Entry<String, Object> entry : reportData.entrySet()) {
      if (!first) {
        json.append(",\n");
      }
      json.append("  \"").append(entry.getKey()).append("\": ");
      json.append(toJsonString(entry.getValue()));
      first = false;
    }

    json.append("\n}");

    Files.write(outputPath, json.toString().getBytes());
  }

  private String toJsonString(final Object obj) {
    if (obj == null) {
      return "null";
    } else if (obj instanceof String) {
      return "\"" + obj.toString().replace("\"", "\\\"") + "\"";
    } else if (obj instanceof Number || obj instanceof Boolean) {
      return obj.toString();
    } else if (obj instanceof Map) {
      final StringBuilder sb = new StringBuilder();
      sb.append("{");
      boolean first = true;
      for (final Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
        if (!first) sb.append(",");
        sb.append("\"").append(entry.getKey()).append("\":").append(toJsonString(entry.getValue()));
        first = false;
      }
      sb.append("}");
      return sb.toString();
    } else if (obj instanceof Collection) {
      final StringBuilder sb = new StringBuilder();
      sb.append("[");
      boolean first = true;
      for (final Object item : ((Collection<?>) obj)) {
        if (!first) sb.append(",");
        sb.append(toJsonString(item));
        first = false;
      }
      sb.append("]");
      return sb.toString();
    } else {
      return "\"" + obj.toString().replace("\"", "\\\"") + "\"";
    }
  }

  // HTML generation methods

  private String generateCss() {
    return """
        body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 2rem; text-align: center; }
        .header h1 { margin: 0; font-size: 2.5rem; font-weight: 300; }
        .subtitle { font-size: 1.2rem; margin: 0.5rem 0; opacity: 0.9; }
        .timestamp { font-size: 1rem; margin: 0; opacity: 0.8; }
        .executive-summary { background: white; margin: 2rem auto; padding: 2rem; max-width: 1200px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .score-card { display: flex; justify-content: space-around; margin: 2rem 0; }
        .score-item { text-align: center; padding: 1rem; }
        .score-value { font-size: 3rem; font-weight: bold; color: #667eea; }
        .score-label { font-size: 1rem; color: #666; }
        .main-content { max-width: 1200px; margin: 0 auto; padding: 0 2rem; }
        .section { background: white; margin: 2rem 0; padding: 2rem; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .section h2 { color: #333; border-bottom: 2px solid #667eea; padding-bottom: 0.5rem; }
        .test-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1rem; margin: 1rem 0; }
        .test-card { background: #f8f9fa; padding: 1rem; border-radius: 6px; border-left: 4px solid #667eea; }
        .status-pass { border-left-color: #28a745; }
        .status-fail { border-left-color: #dc3545; }
        .status-warning { border-left-color: #ffc107; }
        .metric { display: flex; justify-content: space-between; margin: 0.5rem 0; }
        .metric-label { color: #666; }
        .metric-value { font-weight: bold; }
        .footer { background: #333; color: white; text-align: center; padding: 1rem; margin-top: 2rem; }
        .grade-a { color: #28a745; }
        .grade-b { color: #ffc107; }
        .grade-c { color: #fd7e14; }
        .grade-f { color: #dc3545; }
        """;
  }

  private String generateExecutiveSummaryHtml() {
    final StringBuilder html = new StringBuilder();
    final Object summaryObj = reportData.get("executiveSummary");

    if (summaryObj instanceof Map) {
      final Map<?, ?> summary = (Map<?, ?>) summaryObj;

      html.append("    <div class=\"executive-summary\">\n");
      html.append("        <h2>Executive Summary</h2>\n");

      // Score card
      html.append("        <div class=\"score-card\">\n");
      html.append("            <div class=\"score-item\">\n");
      html.append("                <div class=\"score-value\">").append(String.format("%.1f", summary.get("overallScore"))).append("%</div>\n");
      html.append("                <div class=\"score-label\">Overall Score</div>\n");
      html.append("            </div>\n");
      html.append("            <div class=\"score-item\">\n");
      html.append("                <div class=\"score-value grade-").append(getGradeClass(summary.get("qualityGrade"))).append("\">").append(summary.get("qualityGrade")).append("</div>\n");
      html.append("                <div class=\"score-label\">Quality Grade</div>\n");
      html.append("            </div>\n");
      html.append("            <div class=\"score-item\">\n");
      html.append("                <div class=\"score-value\">").append(Boolean.TRUE.equals(summary.get("isProductionReady")) ? "✓" : "✗").append("</div>\n");
      html.append("                <div class=\"score-label\">Production Ready</div>\n");
      html.append("            </div>\n");
      html.append("        </div>\n");

      // Recommendations
      final Object recommendations = summary.get("recommendations");
      if (recommendations instanceof Collection) {
        html.append("        <h3>Recommendations</h3>\n");
        html.append("        <ul>\n");
        for (final Object rec : (Collection<?>) recommendations) {
          html.append("            <li>").append(rec.toString()).append("</li>\n");
        }
        html.append("        </ul>\n");
      }

      html.append("    </div>\n");
    }

    return html.toString();
  }

  private String generateApiCoverageHtml() {
    final StringBuilder html = new StringBuilder();
    final Object coverageObj = reportData.get("apiCoverage");

    if (coverageObj instanceof Map) {
      final Map<?, ?> coverage = (Map<?, ?>) coverageObj;

      html.append("        <div class=\"section\">\n");
      html.append("            <h2>API Coverage Validation</h2>\n");
      html.append("            <div class=\"test-grid\">\n");

      html.append("                <div class=\"test-card status-").append(getStatusClass(coverage.get("totalCoverage"), 95.0)).append("\">\n");
      html.append("                    <h3>Total Coverage</h3>\n");
      html.append("                    <div class=\"metric\">\n");
      html.append("                        <span class=\"metric-label\">Coverage Percentage:</span>\n");
      html.append("                        <span class=\"metric-value\">").append(String.format("%.2f%%", coverage.get("totalCoverage"))).append("</span>\n");
      html.append("                    </div>\n");
      html.append("                    <div class=\"metric\">\n");
      html.append("                        <span class=\"metric-label\">Production Ready:</span>\n");
      html.append("                        <span class=\"metric-value\">").append(coverage.get("isProductionReady")).append("</span>\n");
      html.append("                    </div>\n");
      html.append("                </div>\n");

      html.append("            </div>\n");
      html.append("        </div>\n");
    }

    return html.toString();
  }

  private String generateFunctionalTestingHtml() {
    return generateTestSectionHtml("functionalTesting", "Functional Testing");
  }

  private String generateParityValidationHtml() {
    return generateTestSectionHtml("parityValidation", "JNI vs Panama Parity Validation");
  }

  private String generateRealWorldTestingHtml() {
    return generateTestSectionHtml("realWorldTesting", "Real-World Scenario Testing");
  }

  private String generatePerformanceTestingHtml() {
    return generateTestSectionHtml("performanceTesting", "Performance Testing");
  }

  private String generateMemoryLeakDetectionHtml() {
    return generateTestSectionHtml("memoryLeakDetection", "Memory Leak Detection");
  }

  private String generateCrossPlatformTestingHtml() {
    return generateTestSectionHtml("crossPlatformTesting", "Cross-Platform Compatibility");
  }

  private String generateTestSectionHtml(final String sectionKey, final String title) {
    final StringBuilder html = new StringBuilder();
    final Object sectionObj = reportData.get(sectionKey);

    if (sectionObj instanceof Map) {
      final Map<?, ?> section = (Map<?, ?>) sectionObj;

      html.append("        <div class=\"section\">\n");
      html.append("            <h2>").append(title).append("</h2>\n");
      html.append("            <div class=\"test-grid\">\n");

      html.append("                <div class=\"test-card status-").append(getStatusClass(section.get("successRate"), 95.0)).append("\">\n");
      html.append("                    <h3>Test Results</h3>\n");
      html.append("                    <div class=\"metric\">\n");
      html.append("                        <span class=\"metric-label\">Total Tests:</span>\n");
      html.append("                        <span class=\"metric-value\">").append(section.get("totalTests")).append("</span>\n");
      html.append("                    </div>\n");
      html.append("                    <div class=\"metric\">\n");
      html.append("                        <span class=\"metric-label\">Passed:</span>\n");
      html.append("                        <span class=\"metric-value\">").append(section.get("passedTests")).append("</span>\n");
      html.append("                    </div>\n");
      html.append("                    <div class=\"metric\">\n");
      html.append("                        <span class=\"metric-label\">Failed:</span>\n");
      html.append("                        <span class=\"metric-value\">").append(section.get("failedTests")).append("</span>\n");
      html.append("                    </div>\n");
      html.append("                    <div class=\"metric\">\n");
      html.append("                        <span class=\"metric-label\">Success Rate:</span>\n");
      html.append("                        <span class=\"metric-value\">").append(String.format("%.2f%%", section.get("successRate"))).append("</span>\n");
      html.append("                    </div>\n");
      html.append("                </div>\n");

      html.append("            </div>\n");
      html.append("        </div>\n");
    }

    return html.toString();
  }

  private String getStatusClass(final Object value, final double threshold) {
    if (value instanceof Number) {
      final double numValue = ((Number) value).doubleValue();
      if (numValue >= threshold) return "pass";
      if (numValue >= threshold * 0.8) return "warning";
      return "fail";
    }
    return "warning";
  }

  private String getGradeClass(final Object grade) {
    if (grade == null) return "f";
    final String gradeStr = grade.toString().toLowerCase();
    if (gradeStr.startsWith("a")) return "a";
    if (gradeStr.startsWith("b")) return "b";
    if (gradeStr.startsWith("c")) return "c";
    return "f";
  }
}