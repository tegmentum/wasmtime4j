package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import ai.tegmentum.wasmtime4j.comparison.analyzers.ComprehensiveCoverageReport;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageTrend;
import ai.tegmentum.wasmtime4j.comparison.analyzers.DiscrepancySeverity;
import ai.tegmentum.wasmtime4j.comparison.analyzers.DiscrepancyType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.PerformanceAnalyzer;
import ai.tegmentum.wasmtime4j.comparison.analyzers.TrendDirection;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive integration test for the HTML dashboard functionality, testing report generation,
 * web server deployment, interactive features, and export capabilities.
 *
 * @since 1.0.0
 */
class HtmlDashboardIntegrationTest {

  @TempDir private Path tempDir;

  private DashboardGenerator dashboardGenerator;
  private ComparisonReport testReport;

  @BeforeEach
  void setUp() {
    // Create dashboard configuration
    final DashboardConfiguration dashboardConfig =
        DashboardConfiguration.builder()
            .port(8081) // Use different port to avoid conflicts
            .title("Test Dashboard")
            .theme("default")
            .enableRealTimeUpdates(false)
            .maxCachedReports(5)
            .build();

    dashboardGenerator = new DashboardGenerator(dashboardConfig);
    testReport = createTestComparisonReport();
  }

  @AfterEach
  void tearDown() throws IOException {
    if (dashboardGenerator != null && dashboardGenerator.isRunning()) {
      dashboardGenerator.stopDashboard();
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testStaticHtmlReportGeneration() throws IOException {
    // Test static HTML report generation
    final HtmlReporterConfiguration htmlConfig =
        HtmlReporterConfiguration.builder()
            .reportTitle("Test Report")
            .theme("default")
            .enableInteractiveFeatures(true)
            .enablePerformanceCharts(true)
            .enableCoverageAnalysis(true)
            .includeStaticResources(true)
            .verbosityLevel(VerbosityLevel.NORMAL)
            .build();

    final HtmlReporter htmlReporter = new HtmlReporter(htmlConfig);
    final Path outputPath = tempDir.resolve("test-report.html");

    // Generate HTML report
    htmlReporter.generateReport(testReport, outputPath);

    // Verify report was generated
    assertTrue(Files.exists(outputPath), "HTML report file should exist");
    assertTrue(Files.size(outputPath) > 0, "HTML report should not be empty");

    // Verify static resources were copied
    final Path resourcesDir = tempDir.resolve("resources");
    assertTrue(Files.exists(resourcesDir), "Resources directory should exist");
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testDashboardServerStartStop() throws IOException {
    // Test dashboard server lifecycle
    assertFalse(dashboardGenerator.isRunning(), "Dashboard should not be running initially");

    // Start dashboard
    final URI dashboardUri = dashboardGenerator.startDashboard(testReport);
    assertNotNull(dashboardUri, "Dashboard URI should not be null");
    assertTrue(dashboardGenerator.isRunning(), "Dashboard should be running after start");

    // Verify URI format
    assertTrue(dashboardUri.toString().contains("localhost"), "URI should contain localhost");
    assertTrue(dashboardUri.toString().contains("8081"), "URI should contain configured port");
    assertTrue(
        dashboardUri.toString().contains(testReport.getReportId()), "URI should contain report ID");

    // Stop dashboard
    dashboardGenerator.stopDashboard();
    assertFalse(dashboardGenerator.isRunning(), "Dashboard should not be running after stop");
  }

  @Test
  void testVisualizationBuilder() {
    // Test visualization data generation
    final VisualizationBuilder visualizationBuilder = new VisualizationBuilder();

    // Test performance chart data
    final Map<String, Object> performanceChart =
        visualizationBuilder.createPerformanceChartData(testReport);
    assertNotNull(performanceChart, "Performance chart data should not be null");
    assertEquals("bar", performanceChart.get("type"), "Chart type should be bar");
    assertTrue(performanceChart.containsKey("data"), "Chart should contain data");
    assertTrue(performanceChart.containsKey("options"), "Chart should contain options");

    // Test coverage chart data
    final Map<String, Object> coverageChart =
        visualizationBuilder.createCoverageChartData(testReport);
    assertNotNull(coverageChart, "Coverage chart data should not be null");
    assertEquals("doughnut", coverageChart.get("type"), "Chart type should be doughnut");

    // Test trend chart data
    final Map<String, Object> trendChart = visualizationBuilder.createTrendChartData(testReport);
    assertNotNull(trendChart, "Trend chart data should not be null");
    assertEquals("line", trendChart.get("type"), "Chart type should be line");

    // Test discrepancy visualization
    final Map<String, Object> discrepancyViz =
        visualizationBuilder.createDiscrepancyVisualization(testReport);
    assertNotNull(discrepancyViz, "Discrepancy visualization should not be null");
    assertTrue(
        discrepancyViz.containsKey("severityDistribution"), "Should contain severity distribution");

    // Test comparison table data
    final Map<String, Object> comparisonTable =
        visualizationBuilder.createComparisonTableData(testReport);
    assertNotNull(comparisonTable, "Comparison table data should not be null");
    assertTrue(comparisonTable.containsKey("headers"), "Should contain headers");
    assertTrue(comparisonTable.containsKey("rows"), "Should contain rows");
  }

  @Test
  void testComparisonViewBuilder() {
    // Test side-by-side comparison views
    final ComparisonViewBuilder viewBuilder = new ComparisonViewBuilder();
    final TestComparisonResult testResult = testReport.getTestResults().get(0);

    // Create side-by-side comparison
    final Map<String, Object> comparisonView = viewBuilder.createSideBySideComparison(testResult);
    assertNotNull(comparisonView, "Comparison view should not be null");

    // Verify view structure
    assertTrue(comparisonView.containsKey("testName"), "Should contain test name");
    assertTrue(comparisonView.containsKey("runtimePanels"), "Should contain runtime panels");
    assertTrue(comparisonView.containsKey("diffAnalysis"), "Should contain diff analysis");
    assertTrue(comparisonView.containsKey("discrepancies"), "Should contain discrepancies");
    assertTrue(
        comparisonView.containsKey("performanceComparison"),
        "Should contain performance comparison");

    // Verify runtime panels
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> runtimePanels =
        (List<Map<String, Object>>) comparisonView.get("runtimePanels");
    assertFalse(runtimePanels.isEmpty(), "Runtime panels should not be empty");

    // Verify diff analysis
    @SuppressWarnings("unchecked")
    final Map<String, Object> diffAnalysis =
        (Map<String, Object>) comparisonView.get("diffAnalysis");
    assertTrue(
        diffAnalysis.containsKey("hasDifferences"),
        "Diff analysis should indicate if differences exist");
  }

  @Test
  void testFilterEngine() {
    // Test filtering functionality
    final FilterEngine filterEngine = new FilterEngine(SearchConfiguration.defaultConfiguration());

    // Create filter criteria
    final FilterCriteria criteria =
        FilterCriteria.builder()
            .includeStatuses(Set.of(TestResultStatus.SUCCESS, TestResultStatus.WARNING))
            .includeRuntimes(Set.of(RuntimeType.JNI, RuntimeType.PANAMA), RuntimeMatchMode.ANY)
            .onlyCriticalIssues(false)
            .searchQuery("test")
            .sortBy("name", SortDirection.ASCENDING)
            .pagination(0, 10)
            .build();

    // Apply filters
    final FilterResult filterResult = filterEngine.filterReport(testReport, criteria);
    assertNotNull(filterResult, "Filter result should not be null");

    // Verify filter result structure
    assertNotNull(filterResult.getFilteredResults(), "Filtered results should not be null");
    assertEquals(
        criteria, filterResult.getAppliedCriteria(), "Applied criteria should match input");
    assertTrue(filterResult.getFilterTimeMs() >= 0, "Filter time should be non-negative");
    assertNotNull(filterResult.getMetadata(), "Metadata should not be null");
  }

  @Test
  void testExportManager() throws IOException {
    // Test export functionality
    final ExportConfiguration exportConfig =
        ExportConfiguration.builder()
            .reportTitle("Test Export")
            .theme("default")
            .includeStaticResources(true)
            .includeInteractiveFeatures(true)
            .includeCharts(true)
            .prettyPrintJson(true)
            .verbosityLevel(VerbosityLevel.NORMAL)
            .build();

    final ExportManager exportManager = new ExportManager(exportConfig);

    // Test HTML export
    final Path htmlExportPath = tempDir.resolve("export.html");
    exportManager.exportReport(testReport, ExportFormat.HTML, htmlExportPath);
    assertTrue(Files.exists(htmlExportPath), "HTML export should exist");
    assertTrue(Files.size(htmlExportPath) > 0, "HTML export should not be empty");

    // Test JSON export
    final Path jsonExportPath = tempDir.resolve("export.json");
    exportManager.exportReport(testReport, ExportFormat.JSON, jsonExportPath);
    assertTrue(Files.exists(jsonExportPath), "JSON export should exist");
    assertTrue(Files.size(jsonExportPath) > 0, "JSON export should not be empty");

    // Test CSV export
    final Path csvExportPath = tempDir.resolve("export.csv");
    exportManager.exportReport(testReport, ExportFormat.CSV, csvExportPath);
    assertTrue(Files.exists(csvExportPath), "CSV export should exist");
    assertTrue(Files.size(csvExportPath) > 0, "CSV export should not be empty");

    // Test bundle export
    final Path bundleExportPath = tempDir.resolve("export.zip");
    exportManager.exportReport(testReport, ExportFormat.BUNDLE, bundleExportPath);
    assertTrue(Files.exists(bundleExportPath), "Bundle export should exist");
    assertTrue(Files.size(bundleExportPath) > 0, "Bundle export should not be empty");

    // Test summary export
    final Path summaryExportPath = tempDir.resolve("summary.txt");
    exportManager.exportSummary(testReport, summaryExportPath);
    assertTrue(Files.exists(summaryExportPath), "Summary export should exist");
    assertTrue(Files.size(summaryExportPath) > 0, "Summary export should not be empty");

    // Test individual test export
    final TestComparisonResult testResult = testReport.getTestResults().get(0);
    final Path testExportPath = tempDir.resolve("test-export.json");
    exportManager.exportTestResult(testResult, ExportFormat.JSON, testExportPath);
    assertTrue(Files.exists(testExportPath), "Test export should exist");
    assertTrue(Files.size(testExportPath) > 0, "Test export should not be empty");
  }

  @Test
  void testEndToEndWorkflow() throws IOException {
    // Test complete end-to-end workflow
    // 1. Generate HTML report
    final HtmlReporter htmlReporter = new HtmlReporter(HtmlReporterConfiguration.builder().build());
    final Path reportPath = tempDir.resolve("workflow-report.html");
    htmlReporter.generateReport(testReport, reportPath);

    // 2. Apply filtering
    final FilterEngine filterEngine = new FilterEngine(SearchConfiguration.defaultConfiguration());
    final FilterCriteria criteria =
        FilterCriteria.builder()
            .searchQuery("sample")
            .sortBy("name", SortDirection.ASCENDING)
            .build();
    final FilterResult filterResult = filterEngine.filterReport(testReport, criteria);

    // 3. Export filtered results
    final ExportManager exportManager =
        new ExportManager(ExportConfiguration.defaultConfiguration());
    final Path filteredExportPath = tempDir.resolve("filtered-export.json");
    exportManager.exportFilteredResults(
        testReport, filterResult, ExportFormat.JSON, filteredExportPath);

    // 4. Verify all outputs
    assertTrue(Files.exists(reportPath), "HTML report should exist");
    assertTrue(Files.exists(filteredExportPath), "Filtered export should exist");
    assertNotNull(filterResult.getFilteredResults(), "Filtered results should exist");
  }

  /**
   * Creates a comprehensive test comparison report for testing purposes.
   *
   * @return test comparison report
   */
  private ComparisonReport createTestComparisonReport() {
    final Instant now = Instant.now();

    // Create metadata
    final ComparisonMetadata metadata =
        new ComparisonMetadata(
            "Test Suite",
            "1.0.0",
            Set.of(RuntimeType.JNI, RuntimeType.PANAMA),
            Map.of("environment", "test", "os", "linux"),
            "1.0.0");

    // Create execution summary
    final ExecutionSummary executionSummary =
        new ExecutionSummary(10, 8, 2, 0, Duration.ofMinutes(5), now.minusMinutes(5), now);

    // Create test results
    final List<TestComparisonResult> testResults =
        List.of(
            createTestComparisonResult("sample-test-1", TestResultStatus.SUCCESS),
            createTestComparisonResult("sample-test-2", TestResultStatus.WARNING),
            createTestComparisonResult("critical-test", TestResultStatus.CRITICAL));

    // Create coverage report
    final ComprehensiveCoverageReport coverageReport =
        new ComprehensiveCoverageReport(
            Map.of("category1", 85.0, "category2", 92.0),
            List.of("uncovered-feature-1", "uncovered-feature-2"),
            Map.of(RuntimeType.JNI, 88.0, RuntimeType.PANAMA, 90.0),
            List.of(),
            new CoverageTrend(89.0, 87.0, 2.0, TrendDirection.IMPROVING),
            testResults.size(),
            now);

    // Create performance summary
    final PerformanceAnalysisSummary performanceSummary =
        new PerformanceAnalysisSummary(
            1.2,
            2.5,
            Map.of(RuntimeType.JNI, 100.0, RuntimeType.PANAMA, 95.0),
            List.of("slow-test-1"),
            Map.of("trend1", 1.1, "trend2", 1.3));

    // Create behavioral discrepancies
    final List<BehavioralDiscrepancy> discrepancies =
        List.of(
            new BehavioralDiscrepancy(
                DiscrepancyType.OUTPUT_DIFFERENCE,
                DiscrepancySeverity.MEDIUM,
                "Output format difference",
                "Different output formatting between runtimes",
                "Normalize output formatting"));

    // Create statistics
    final ReportStatistics statistics =
        new ReportStatistics(
            testResults.size(), 1024 * 10, Map.of("success", 8, "warning", 1, "critical", 1), 95.0);

    // Build report
    return new ComparisonReport.Builder("test-report-" + now.getEpochSecond())
        .metadata(metadata)
        .executionSummary(executionSummary)
        .testResults(testResults)
        .coverageReport(coverageReport)
        .performanceSummary(performanceSummary)
        .behavioralDiscrepancies(discrepancies)
        .recommendations(List.of())
        .statistics(statistics)
        .build();
  }

  /**
   * Creates a test comparison result for testing purposes.
   *
   * @param testName the test name
   * @param status the test status
   * @return test comparison result
   */
  private TestComparisonResult createTestComparisonResult(
      final String testName, final TestResultStatus status) {
    // Create runtime results
    final Map<RuntimeType, TestExecutionResult> runtimeResults =
        Map.of(
            RuntimeType.JNI,
                new TestExecutionResult(
                    RuntimeType.JNI,
                    status == TestResultStatus.SUCCESS || status == TestResultStatus.WARNING,
                    "Test output for JNI runtime",
                    status == TestResultStatus.CRITICAL ? "Critical error occurred" : "",
                    Duration.ofMillis(100),
                    Map.of("memory", 1024, "cpu", 50)),
            RuntimeType.PANAMA,
                new TestExecutionResult(
                    RuntimeType.PANAMA,
                    status == TestResultStatus.SUCCESS,
                    "Test output for Panama runtime",
                    status != TestResultStatus.SUCCESS ? "Runtime error" : "",
                    Duration.ofMillis(95),
                    Map.of("memory", 1100, "cpu", 48)));

    // Create minimal coverage analysis
    final CoverageAnalysisResult coverageAnalysis =
        new CoverageAnalysisResult.Builder(testName)
            .detectedFeatures(Set.of("feature1", "feature2"))
            .runtimeFeatureCoverage(
                Map.of(
                    RuntimeType.JNI, Set.of("feature1"),
                    RuntimeType.PANAMA, Set.of("feature1", "feature2")))
            .coverageMetrics(
                new ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageMetrics(
                    2, 2, 100.0, Map.of(RuntimeType.JNI, 50.0, RuntimeType.PANAMA, 100.0), 100.0))
            .coverageGaps(List.of())
            .featureInteractionAnalysis(
                new ai.tegmentum.wasmtime4j.comparison.analyzers.FeatureInteractionAnalysis(
                    Map.of(), List.of(), 1.0))
            .build();

    // Create performance comparison
    final PerformanceAnalyzer.PerformanceComparisonResult performanceComparison =
        new PerformanceAnalyzer.PerformanceComparisonResult(
            Map.of(RuntimeType.JNI, 100L, RuntimeType.PANAMA, 95L),
            Map.of(RuntimeType.JNI, 1024L, RuntimeType.PANAMA, 1100L),
            1.05,
            false,
            RuntimeType.PANAMA);

    // Create discrepancies if not successful
    final List<BehavioralDiscrepancy> discrepancies =
        status == TestResultStatus.CRITICAL
            ? List.of(
                new BehavioralDiscrepancy(
                    DiscrepancyType.RUNTIME_ERROR,
                    DiscrepancySeverity.HIGH,
                    "Critical runtime error",
                    "Test failed with critical error",
                    "Investigate runtime configuration"))
            : List.of();

    return new TestComparisonResult(
        testName, runtimeResults, coverageAnalysis, performanceComparison, discrepancies, status);
  }
}
