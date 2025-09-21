package ai.tegmentum.wasmtime4j.monitoring;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageMetrics;
import ai.tegmentum.wasmtime4j.comparison.analyzers.GlobalCoverageStatistics;
import ai.tegmentum.wasmtime4j.monitoring.CoverageRegressionDetector.RegressionAnalysisResult;
import ai.tegmentum.wasmtime4j.monitoring.ExecutiveDashboard.ExecutiveDashboardResult;
import ai.tegmentum.wasmtime4j.monitoring.ExecutiveDashboard.ExecutiveReport;
import ai.tegmentum.wasmtime4j.monitoring.PredictiveCoverageAnalytics.PredictiveAnalysisResult;
import ai.tegmentum.wasmtime4j.monitoring.RealTimeCoverageMonitor.CoverageSnapshot;
import ai.tegmentum.wasmtime4j.monitoring.RealTimeCoverageMonitor.MonitoringStatistics;
import ai.tegmentum.wasmtime4j.monitoring.StakeholderNotificationSystem.NotificationDeliveryResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive integration test for the monitoring framework that validates the complete
 * coverage monitoring, regression detection, executive reporting, and notification system.
 */
public class ComprehensiveMonitoringIntegrationTest {

  private RealTimeCoverageMonitor coverageMonitor;
  private CoverageRegressionDetector regressionDetector;
  private ExecutiveDashboard executiveDashboard;
  private PredictiveCoverageAnalytics predictiveAnalytics;
  private AutomatedTestSuiteMaintenance maintenanceSystem;
  private StakeholderNotificationSystem notificationSystem;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    System.out.println("Setting up comprehensive monitoring integration test: " + testInfo.getDisplayName());

    // Initialize monitoring components
    coverageMonitor = new RealTimeCoverageMonitor(
        Duration.ofMinutes(1), 1.0, false); // Disable real-time mode for testing
    regressionDetector = new CoverageRegressionDetector();
    executiveDashboard = new ExecutiveDashboard(coverageMonitor, regressionDetector);
    predictiveAnalytics = new PredictiveCoverageAnalytics();
    maintenanceSystem = new AutomatedTestSuiteMaintenance(coverageMonitor, regressionDetector);
    notificationSystem = new StakeholderNotificationSystem();
  }

  @AfterEach
  void tearDown(TestInfo testInfo) {
    System.out.println("Tearing down comprehensive monitoring integration test: " + testInfo.getDisplayName());

    // Shutdown all systems gracefully
    if (notificationSystem != null) {
      notificationSystem.shutdown();
    }
    if (maintenanceSystem != null) {
      maintenanceSystem.shutdown();
    }
    if (executiveDashboard != null) {
      executiveDashboard.shutdown();
    }
    if (coverageMonitor != null) {
      coverageMonitor.shutdown();
    }
  }

  @Test
  @DisplayName("Real-time coverage monitoring should track and analyze coverage data accurately")
  void testRealTimeCoverageMonitoring() {
    System.out.println("Testing real-time coverage monitoring functionality");

    // Create test coverage data
    final CoverageAnalysisResult coverageResult = createTestCoverageResult(95.5, 98.0);

    // Record coverage result
    coverageMonitor.recordCoverageResult("test-comprehensive-coverage", coverageResult);

    // Verify current statistics
    final GlobalCoverageStatistics stats = coverageMonitor.getCurrentStatistics();
    assertNotNull(stats, "Coverage statistics should be available");
    assertEquals(95.5, stats.getOverallCoveragePercentage(), 0.1,
        "Coverage percentage should match recorded value");

    // Verify trend analysis
    final RealTimeCoverageMonitor.CoverageTrendAnalysis trend =
        coverageMonitor.analyzeTrend(Duration.ofHours(1));
    assertNotNull(trend, "Trend analysis should be available");
    assertEquals(RealTimeCoverageMonitor.TrendDirection.STABLE, trend.getDirection(),
        "Initial trend should be stable");

    // Verify health assessment
    final RealTimeCoverageMonitor.CoverageHealthAssessment health =
        coverageMonitor.assessCoverageHealth();
    assertNotNull(health, "Health assessment should be available");
    assertEquals(RealTimeCoverageMonitor.HealthStatus.HEALTHY, health.getStatus(),
        "Health status should be healthy for 95.5% coverage");

    // Verify monitoring statistics
    final MonitoringStatistics monitoringStats = coverageMonitor.getMonitoringStatistics();
    assertNotNull(monitoringStats, "Monitoring statistics should be available");
    assertEquals(1, monitoringStats.getTotalTests(), "Should have recorded one test");
    assertTrue(monitoringStats.isHealthy(), "Monitor should be healthy");

    System.out.println("Real-time coverage monitoring test completed successfully");
  }

  @Test
  @DisplayName("Regression detection should identify and classify coverage regressions accurately")
  void testRegressionDetection() {
    System.out.println("Testing regression detection functionality");

    // Create historical data with stable coverage
    final List<CoverageSnapshot> historicalData = createHistoricalCoverageData(96.0, 10);

    // Create current snapshot with regression
    final CoverageSnapshot currentSnapshot = new CoverageSnapshot(
        Instant.now(),
        92.0, // 4% drop
        Map.of(RuntimeType.JNI, 92.0, RuntimeType.PANAMA, 92.0),
        "test-regression",
        50,
        95.0
    );

    // Analyze for regressions
    final RegressionAnalysisResult regressionResult =
        regressionDetector.analyzeForRegressions(currentSnapshot, historicalData);

    assertNotNull(regressionResult, "Regression analysis result should be available");
    assertFalse(regressionResult.getRegressions().isEmpty(),
        "Should detect regression with 4% coverage drop");

    final CoverageRegressionDetector.RegressionEvent coverageRegression =
        regressionResult.getRegressions().stream()
            .filter(event -> event.getType() == CoverageRegressionDetector.RegressionType.COVERAGE)
            .findFirst()
            .orElse(null);

    assertNotNull(coverageRegression, "Should detect coverage regression");
    assertTrue(coverageRegression.getSeverityScore() > 1.0,
        "Regression severity score should indicate significance");

    // Verify adaptive thresholds
    final CoverageRegressionDetector.AdaptiveThresholds thresholds =
        regressionDetector.getAdaptiveThresholds();
    assertNotNull(thresholds, "Adaptive thresholds should be available");
    assertTrue(thresholds.getCoverageThreshold() > 0,
        "Coverage threshold should be positive");

    System.out.println("Regression detection test completed successfully");
  }

  @Test
  @DisplayName("Executive dashboard should generate comprehensive strategic insights and reports")
  void testExecutiveDashboard() {
    System.out.println("Testing executive dashboard functionality");

    // Record some coverage data
    final CoverageAnalysisResult coverageResult = createTestCoverageResult(94.0, 97.0);
    coverageMonitor.recordCoverageResult("test-executive-dashboard", coverageResult);

    // Generate real-time dashboard
    final ExecutiveDashboardResult dashboardResult = executiveDashboard.generateRealTimeDashboard();

    assertNotNull(dashboardResult, "Dashboard result should be available");
    assertNotNull(dashboardResult.getKpis(), "Executive KPIs should be available");
    assertNotNull(dashboardResult.getInsights(), "Strategic insights should be available");
    assertNotNull(dashboardResult.getActionItems(), "Action items should be available");

    // Verify KPIs
    final ExecutiveDashboard.ExecutiveKPIs kpis = dashboardResult.getKpis();
    assertEquals(94.0, kpis.getCoveragePercentage(), 0.1,
        "KPI coverage should match recorded value");
    assertTrue(kpis.getQualityScore() > 0, "Quality score should be positive");
    assertTrue(kpis.getAutomationHealth() >= 0, "Automation health should be non-negative");

    // Generate executive report
    final ExecutiveReport executiveReport = executiveDashboard.generateExecutiveReport();

    assertNotNull(executiveReport, "Executive report should be available");
    assertNotNull(executiveReport.getDashboard(), "Report should contain dashboard data");
    assertNotNull(executiveReport.getRecommendations(), "Report should contain recommendations");
    assertNotNull(executiveReport.getExecutiveSummary(), "Report should contain executive summary");

    // Verify dashboard statistics
    final ExecutiveDashboard.DashboardStatistics dashboardStats =
        executiveDashboard.getDashboardStatistics();
    assertNotNull(dashboardStats, "Dashboard statistics should be available");
    assertTrue(dashboardStats.getTotalReports() >= 1, "Should have generated at least one report");

    System.out.println("Executive dashboard test completed successfully");
  }

  @Test
  @DisplayName("Predictive analytics should forecast trends and detect anomalies accurately")
  void testPredictiveAnalytics() {
    System.out.println("Testing predictive analytics functionality");

    // Create sufficient historical data for prediction
    final List<CoverageSnapshot> historicalData = createHistoricalCoverageData(95.0, 25);

    // Perform predictive analysis
    final PredictiveAnalysisResult analysisResult =
        predictiveAnalytics.performPredictiveAnalysis(historicalData);

    assertNotNull(analysisResult, "Predictive analysis result should be available");
    assertNotNull(analysisResult.getForecast(), "Coverage forecast should be available");
    assertNotNull(analysisResult.getAnomalies(), "Anomaly detection should be available");
    assertNotNull(analysisResult.getRiskAssessment(), "Risk assessment should be available");
    assertNotNull(analysisResult.getRecommendations(), "Strategic recommendations should be available");

    // Verify forecast quality
    final PredictiveCoverageAnalytics.CoverageForecast forecast = analysisResult.getForecast();
    assertFalse(forecast.getForecastPoints().isEmpty(), "Forecast should contain prediction points");
    assertTrue(forecast.getOverallConfidence() > 0, "Forecast should have positive confidence");

    // Test real-time alerts
    final CoverageSnapshot currentSnapshot = historicalData.get(historicalData.size() - 1);
    final PredictiveCoverageAnalytics.RealTimeAlertResult alertResult =
        predictiveAnalytics.generateRealTimeAlerts(currentSnapshot, analysisResult);

    assertNotNull(alertResult, "Real-time alert result should be available");
    // Alerts may or may not be present depending on data, but result should be valid

    // Verify strategic insights
    final PredictiveCoverageAnalytics.StrategicPredictiveInsights insights =
        predictiveAnalytics.getStrategicInsights();
    assertNotNull(insights, "Strategic insights should be available");
    assertNotNull(insights.getLongTermTrends(), "Long-term trends should be available");
    assertNotNull(insights.getQualityForecast(), "Quality forecast should be available");

    System.out.println("Predictive analytics test completed successfully");
  }

  @Test
  @DisplayName("Automated maintenance should analyze and maintain test suite health")
  void testAutomatedMaintenance() {
    System.out.println("Testing automated test suite maintenance functionality");

    // Perform maintenance analysis
    final AutomatedTestSuiteMaintenance.MaintenanceResult maintenanceResult =
        maintenanceSystem.performMaintenance();

    assertNotNull(maintenanceResult, "Maintenance result should be available");
    assertNotNull(maintenanceResult.getActionsPerformed(), "Actions performed should be tracked");
    assertNotNull(maintenanceResult.getRecommendations(), "Maintenance recommendations should be available");
    assertNotNull(maintenanceResult.getMetrics(), "Maintenance metrics should be available");

    // Verify maintenance was successful or partially successful
    assertTrue(
        maintenanceResult.getStatus() == AutomatedTestSuiteMaintenance.MaintenanceStatus.SUCCESS ||
        maintenanceResult.getStatus() == AutomatedTestSuiteMaintenance.MaintenanceStatus.PARTIAL_SUCCESS,
        "Maintenance should complete successfully");

    // Analyze test suite health
    final AutomatedTestSuiteMaintenance.TestSuiteHealthAnalysis healthAnalysis =
        maintenanceSystem.analyzeTestSuiteHealth();

    assertNotNull(healthAnalysis, "Test suite health analysis should be available");
    assertTrue(healthAnalysis.getTotalTests() >= 0, "Total tests should be non-negative");
    assertTrue(healthAnalysis.getOverallHealthScore() >= 0 && healthAnalysis.getOverallHealthScore() <= 100,
        "Health score should be between 0 and 100");

    // Verify maintenance statistics
    final AutomatedTestSuiteMaintenance.MaintenanceStatistics maintenanceStats =
        maintenanceSystem.getMaintenanceStatistics();
    assertNotNull(maintenanceStats, "Maintenance statistics should be available");
    assertTrue(maintenanceStats.getTotalMaintenanceSessions() >= 1,
        "Should have performed at least one maintenance session");

    System.out.println("Automated maintenance test completed successfully");
  }

  @Test
  @DisplayName("Stakeholder notification system should deliver alerts through multiple channels")
  void testStakeholderNotifications() {
    System.out.println("Testing stakeholder notification system functionality");

    // Create test notification
    final ExecutiveDashboard.StakeholderNotification notification =
        new ExecutiveDashboard.StakeholderNotification(
            "TEST_NOTIFICATION_" + System.currentTimeMillis(),
            ExecutiveDashboard.NotificationType.WARNING,
            "Test Coverage Alert",
            "This is a test coverage monitoring alert for integration testing",
            List.of("QA Director", "Development Lead"),
            Instant.now()
        );

    // Send notification
    final NotificationDeliveryResult deliveryResult = notificationSystem.sendNotification(notification);

    assertNotNull(deliveryResult, "Notification delivery result should be available");
    assertTrue(deliveryResult.getTotalAttempts() > 0, "Should have attempted delivery");
    assertEquals(2, deliveryResult.getTotalAttempts(),
        "Should have attempted delivery to 2 recipients");

    // Test regression alert
    final CoverageRegressionDetector.RegressionEvent regressionEvent =
        new CoverageRegressionDetector.RegressionEvent(
            CoverageRegressionDetector.RegressionType.COVERAGE,
            Instant.now(),
            CoverageRegressionDetector.RegressionSeverity.MAJOR,
            2.5,
            "Test regression for integration testing",
            Map.of("baseline", 96.0, "current", 93.5, "drop", 2.5)
        );

    final NotificationDeliveryResult regressionResult =
        notificationSystem.sendRegressionAlert(regressionEvent);
    assertNotNull(regressionResult, "Regression alert delivery should be successful");

    // Verify notification statistics
    final StakeholderNotificationSystem.NotificationStatistics notificationStats =
        notificationSystem.getNotificationStatistics();
    assertNotNull(notificationStats, "Notification statistics should be available");
    assertTrue(notificationStats.getTotalNotifications() >= 2,
        "Should have sent at least 2 notifications");

    System.out.println("Stakeholder notification test completed successfully");
  }

  @Test
  @DisplayName("Integrated monitoring workflow should handle complete coverage monitoring lifecycle")
  void testIntegratedMonitoringWorkflow() {
    System.out.println("Testing integrated monitoring workflow");

    // Step 1: Record coverage data
    final CoverageAnalysisResult initialCoverage = createTestCoverageResult(96.5, 98.5);
    coverageMonitor.recordCoverageResult("workflow-test-1", initialCoverage);

    // Step 2: Record regression scenario
    final CoverageAnalysisResult regressionCoverage = createTestCoverageResult(91.0, 92.0);
    coverageMonitor.recordCoverageResult("workflow-test-regression", regressionCoverage);

    // Step 3: Analyze for regressions
    final List<CoverageSnapshot> snapshots = List.of(
        new CoverageSnapshot(Instant.now().minus(Duration.ofMinutes(5)), 96.5,
            Map.of(RuntimeType.JNI, 96.5, RuntimeType.PANAMA, 96.5), "workflow-test-1", 50, 98.5),
        new CoverageSnapshot(Instant.now(), 91.0,
            Map.of(RuntimeType.JNI, 91.0, RuntimeType.PANAMA, 91.0), "workflow-test-regression", 50, 92.0)
    );

    final RegressionAnalysisResult regressionAnalysis =
        regressionDetector.analyzeForRegressions(snapshots.get(1), List.of(snapshots.get(0)));

    // Step 4: Generate executive dashboard
    final ExecutiveDashboardResult dashboard = executiveDashboard.generateRealTimeDashboard();

    // Step 5: Perform predictive analysis
    final PredictiveAnalysisResult predictiveAnalysis =
        predictiveAnalytics.performPredictiveAnalysis(snapshots);

    // Step 6: Send notifications if regression detected
    if (!regressionAnalysis.getRegressions().isEmpty()) {
      for (final CoverageRegressionDetector.RegressionEvent regression : regressionAnalysis.getRegressions()) {
        final NotificationDeliveryResult notificationResult =
            notificationSystem.sendRegressionAlert(regression);
        assertTrue(notificationResult.getTotalAttempts() > 0,
            "Should attempt to deliver regression notifications");
      }
    }

    // Step 7: Verify complete workflow integration
    assertNotNull(dashboard, "Dashboard should be generated");
    assertNotNull(predictiveAnalysis, "Predictive analysis should be performed");
    assertTrue(regressionAnalysis.getRegressions().size() > 0,
        "Should detect regression in workflow");

    // Verify health assessment reflects the regression
    final RealTimeCoverageMonitor.CoverageHealthAssessment health =
        coverageMonitor.assessCoverageHealth();
    assertTrue(
        health.getStatus() == RealTimeCoverageMonitor.HealthStatus.WARNING ||
        health.getStatus() == RealTimeCoverageMonitor.HealthStatus.CRITICAL,
        "Health status should reflect coverage issues");

    System.out.println("Integrated monitoring workflow test completed successfully");
  }

  @Test
  @DisplayName("Monitoring system should handle concurrent operations and maintain data consistency")
  void testConcurrentOperations() {
    System.out.println("Testing concurrent monitoring operations");

    // Simulate concurrent coverage recordings
    final List<Thread> threads = new ArrayList<>();

    for (int i = 0; i < 5; i++) {
      final int testId = i;
      threads.add(new Thread(() -> {
        final CoverageAnalysisResult result = createTestCoverageResult(95.0 + testId, 97.0 + testId);
        coverageMonitor.recordCoverageResult("concurrent-test-" + testId, result);
      }));
    }

    // Start all threads
    threads.forEach(Thread::start);

    // Wait for completion
    threads.forEach(thread -> {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        fail("Thread interrupted during concurrent test");
      }
    });

    // Verify data consistency
    final MonitoringStatistics stats = coverageMonitor.getMonitoringStatistics();
    assertEquals(5, stats.getTotalTests(), "Should have recorded 5 concurrent tests");

    final GlobalCoverageStatistics coverageStats = coverageMonitor.getCurrentStatistics();
    assertNotNull(coverageStats, "Coverage statistics should be consistent");
    assertTrue(coverageStats.getOverallCoveragePercentage() > 0,
        "Coverage percentage should be valid after concurrent operations");

    System.out.println("Concurrent operations test completed successfully");
  }

  // Helper methods

  private CoverageAnalysisResult createTestCoverageResult(final double overallCoverage, final double successRate) {
    final CoverageMetrics metrics = new CoverageMetrics(
        50, // total features
        (int) (50 * overallCoverage / 100), // covered features
        overallCoverage,
        Map.of(RuntimeType.JNI, overallCoverage - 1.0, RuntimeType.PANAMA, overallCoverage + 1.0),
        successRate
    );

    return new CoverageAnalysisResult.Builder("test-coverage")
        .coverageMetrics(metrics)
        .detectedFeatures(Set.of("memory_operations", "function_calls", "control_flow"))
        .build();
  }

  private List<CoverageSnapshot> createHistoricalCoverageData(final double baseCoverage, final int count) {
    final List<CoverageSnapshot> snapshots = new ArrayList<>();
    final Instant baseTime = Instant.now().minus(Duration.ofHours(count));

    for (int i = 0; i < count; i++) {
      final double coverage = baseCoverage + (Math.random() - 0.5) * 2; // ±1% variation
      final Instant timestamp = baseTime.plus(Duration.ofHours(i));

      snapshots.add(new CoverageSnapshot(
          timestamp,
          coverage,
          Map.of(RuntimeType.JNI, coverage - 0.5, RuntimeType.PANAMA, coverage + 0.5),
          "historical-test-" + i,
          50,
          96.0 + (Math.random() - 0.5) * 4 // ±2% variation
      ));
    }

    return snapshots;
  }
}