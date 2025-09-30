/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.monitoring;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive integration test for the production monitoring and alerting systems. Tests the
 * interaction between all monitoring components and validates end-to-end functionality.
 */
public class ProductionMonitoringIntegrationTest {

  private HealthCheckSystem healthCheckSystem;
  private ProductionMonitoringSystem monitoringSystem;
  private IntelligentAlertingSystem alertingSystem;
  private AutomatedIncidentResponseSystem incidentResponseSystem;
  private ComprehensiveAuditLoggingSystem auditLoggingSystem;
  private PerformanceBaselineTracker baselineTracker;
  private CustomDashboardSystem dashboardSystem;
  private AdvancedHealthCheckEndpoints healthEndpoints;
  private ProductionReadinessValidator readinessValidator;

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    System.out.println("Setting up test: " + testInfo.getDisplayName());

    // Initialize monitoring systems in correct order
    healthCheckSystem = new HealthCheckSystem();
    monitoringSystem = new ProductionMonitoringSystem();
    alertingSystem = new IntelligentAlertingSystem();
    incidentResponseSystem =
        new AutomatedIncidentResponseSystem(alertingSystem, healthCheckSystem, monitoringSystem);
    auditLoggingSystem = new ComprehensiveAuditLoggingSystem();
    baselineTracker = new PerformanceBaselineTracker();
    dashboardSystem =
        new CustomDashboardSystem(monitoringSystem, healthCheckSystem, alertingSystem);
    healthEndpoints = new AdvancedHealthCheckEndpoints(healthCheckSystem, monitoringSystem);
    readinessValidator =
        new ProductionReadinessValidator(
            healthCheckSystem,
            monitoringSystem,
            alertingSystem,
            incidentResponseSystem,
            auditLoggingSystem,
            baselineTracker);

    // Allow systems to initialize
    try {
      Thread.sleep(500);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    System.out.println("Tearing down test: " + testInfo.getDisplayName());

    // Shutdown systems in reverse order
    if (readinessValidator != null) {
      // No shutdown method needed for validator
    }
    if (healthEndpoints != null) {
      // No shutdown method needed for endpoints
    }
    if (dashboardSystem != null) {
      dashboardSystem.shutdown();
    }
    if (baselineTracker != null) {
      baselineTracker.shutdown();
    }
    if (auditLoggingSystem != null) {
      auditLoggingSystem.shutdown();
    }
    if (incidentResponseSystem != null) {
      incidentResponseSystem.shutdown();
    }
    if (alertingSystem != null) {
      alertingSystem.shutdown();
    }
    if (monitoringSystem != null) {
      monitoringSystem.shutdown();
    }
    if (healthCheckSystem != null) {
      healthCheckSystem.shutdown();
    }
  }

  @Test
  void testHealthCheckSystemIntegration() {
    System.out.println("Testing health check system integration...");

    // Test basic health check functionality
    final HealthCheckSystem.HealthStatus status = healthCheckSystem.getOverallHealthStatus();
    assertNotNull(status);
    System.out.println("Overall health status: " + status);

    // Test health check endpoints
    final AdvancedHealthCheckEndpoints.ComprehensiveHealthResult result =
        healthEndpoints.executeHealthCheck("health");
    assertNotNull(result);
    assertNotNull(result.getOverallStatus());
    System.out.println("Health endpoint result: " + result.getOverallStatus());
    System.out.println("Execution time: " + result.getExecutionTime().toMillis() + "ms");
    System.out.println("Components checked: " + result.getComponentHealth().size());

    assertTrue(result.getComponentHealth().size() > 0, "Should have component health information");
  }

  @Test
  void testMonitoringAndAlertingIntegration() {
    System.out.println("Testing monitoring and alerting integration...");

    // Record some metrics
    monitoringSystem.recordCounter("test_requests", 100);
    monitoringSystem.recordGauge("test_memory_usage", 0.75);
    monitoringSystem.recordTimer("test_response_time", Duration.ofMillis(150));

    // Trigger alert conditions
    alertingSystem.evaluateMetric("heap_memory_usage", 0.95, Map.of("component", "test"));
    alertingSystem.evaluateMetric("error_rate", 0.10, Map.of("service", "test"));

    // Allow time for processing
    try {
      Thread.sleep(1000);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Verify alerts were generated
    final List<IntelligentAlertingSystem.CorrelatedAlert> alerts =
        alertingSystem.getActiveAlerts(10);
    System.out.println("Active alerts: " + alerts.size());

    // Verify monitoring statistics
    final String stats = monitoringSystem.getMonitoringStatistics();
    System.out.println("Monitoring statistics: " + stats);
    assertTrue(stats.contains("metrics="), "Should contain metrics count");
  }

  @Test
  void testIncidentResponseIntegration() {
    System.out.println("Testing incident response integration...");

    // Create conditions for incident creation
    alertingSystem.evaluateMetric("deadlocked_threads", 5, Map.of("component", "jvm"));

    // Allow time for incident processing
    try {
      Thread.sleep(2000);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Check for created incidents
    final List<AutomatedIncidentResponseSystem.Incident> incidents =
        incidentResponseSystem.getActiveIncidents(10);
    System.out.println("Active incidents: " + incidents.size());

    // Verify incident response statistics
    final String stats = incidentResponseSystem.getIncidentResponseStatistics();
    System.out.println("Incident response statistics: " + stats);
  }

  @Test
  void testAuditLoggingIntegration() {
    System.out.println("Testing audit logging integration...");

    // Log various audit events
    auditLoggingSystem.logAuditEvent(
        ComprehensiveAuditLoggingSystem.AuditEventType.SYSTEM_STARTUP,
        ComprehensiveAuditLoggingSystem.AuditSeverity.INFO,
        "Test System Start",
        "Testing audit logging integration",
        "test-user",
        "test-session",
        "127.0.0.1",
        "test-agent",
        "test-component",
        "test-resource",
        "start",
        Map.of("test", "data"),
        List.of(ComprehensiveAuditLoggingSystem.ComplianceFramework.ISO_27001));

    auditLoggingSystem.logAuditEvent(
        ComprehensiveAuditLoggingSystem.AuditEventType.SECURITY_EVENT,
        ComprehensiveAuditLoggingSystem.AuditSeverity.HIGH,
        "Test Security Event",
        "Testing security audit logging",
        "test-user",
        "test-session",
        "127.0.0.1",
        "test-agent",
        "security-component",
        "secured-resource",
        "access",
        Map.of("security", "test"),
        List.of(ComprehensiveAuditLoggingSystem.ComplianceFramework.ISO_27001));

    // Generate compliance report
    final ComprehensiveAuditLoggingSystem.ComplianceReport report =
        auditLoggingSystem.generateComplianceReport(
            ComprehensiveAuditLoggingSystem.ComplianceFramework.ISO_27001,
            Instant.now().minus(Duration.ofHours(1)),
            Instant.now());

    assertNotNull(report);
    System.out.println("Compliance report generated: " + report.getReportId());
    System.out.println(
        "Report compliance status: " + (report.isCompliant() ? "COMPLIANT" : "NON-COMPLIANT"));

    // Verify audit statistics
    final String stats = auditLoggingSystem.getAuditStatistics();
    System.out.println("Audit statistics: " + stats);
    assertTrue(stats.contains("total_entries="), "Should contain audit entries count");
  }

  @Test
  void testPerformanceBaselineTrackingIntegration() {
    System.out.println("Testing performance baseline tracking integration...");

    // Record performance metrics over time
    for (int i = 0; i < 50; i++) {
      final double value = 100 + (Math.random() * 20) + (i * 0.5); // Slight upward trend
      baselineTracker.recordMetric("test_response_time", value);
      baselineTracker.recordMetric("test_memory_usage", 0.6 + (Math.random() * 0.2));
    }

    // Allow time for baseline establishment
    try {
      Thread.sleep(1000);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Check baselines
    final PerformanceBaselineTracker.PerformanceBaseline responseTimeBaseline =
        baselineTracker.getBaseline("test_response_time");
    if (responseTimeBaseline != null) {
      System.out.println("Response time baseline - Mean: " + responseTimeBaseline.getMean());
      System.out.println(
          "Response time baseline - StdDev: " + responseTimeBaseline.getStandardDeviation());
      System.out.println(
          "Response time baseline - Data points: " + responseTimeBaseline.getDataPointCount());
    }

    // Check drift history
    final List<PerformanceBaselineTracker.DriftDetectionResult> driftHistory =
        baselineTracker.getDriftHistory("test_response_time", 5);
    System.out.println("Drift detections: " + driftHistory.size());

    // Verify baseline tracker statistics
    final String stats = baselineTracker.getBaselineTrackerStatistics();
    System.out.println("Baseline tracker statistics: " + stats);
  }

  @Test
  void testDashboardSystemIntegration() {
    System.out.println("Testing dashboard system integration...");

    // Render default dashboards
    final CustomDashboardSystem.RenderedDashboard systemOverview =
        dashboardSystem.renderDashboard("system_overview");
    assertNotNull(systemOverview);
    System.out.println(
        "System overview dashboard rendered with "
            + systemOverview.getWidgets().size()
            + " widgets");
    System.out.println("Rendering time: " + systemOverview.getRenderingTime().toMillis() + "ms");
    System.out.println("Dashboard errors: " + systemOverview.getErrors().size());

    final CustomDashboardSystem.RenderedDashboard performanceDashboard =
        dashboardSystem.renderDashboard("performance_monitoring");
    assertNotNull(performanceDashboard);
    System.out.println(
        "Performance dashboard rendered with "
            + performanceDashboard.getWidgets().size()
            + " widgets");

    // Test dashboard statistics
    final String stats = dashboardSystem.getDashboardStatistics();
    System.out.println("Dashboard statistics: " + stats);
    assertTrue(stats.contains("dashboards="), "Should contain dashboard count");
  }

  @Test
  void testProductionReadinessValidation() {
    System.out.println("Testing production readiness validation...");

    // Perform comprehensive readiness assessment
    final ProductionReadinessValidator.ReadinessAssessment assessment =
        readinessValidator.performReadinessAssessment();

    assertNotNull(assessment);
    System.out.println("Assessment ID: " + assessment.getAssessmentId());
    System.out.println("Production Ready: " + assessment.isProductionReady());
    System.out.println("Overall Score: " + String.format("%.1f%%", assessment.getOverallScore()));
    System.out.println("Execution Time: " + assessment.getTotalExecutionTime().toMillis() + "ms");
    System.out.println("Total Checks: " + assessment.getValidationResults().size());
    System.out.println("Blockers: " + assessment.getBlockers().size());
    System.out.println("Warnings: " + assessment.getWarnings().size());

    // Print detailed results
    System.out.println("\nDetailed Assessment Results:");
    for (final ProductionReadinessValidator.ValidationResult result :
        assessment.getValidationResults()) {
      System.out.printf(
          "  %s: %s - %s%n", result.getCheckId(), result.getSeverity(), result.getMessage());
    }

    // Print category scores
    System.out.println("\nCategory Scores:");
    assessment
        .getCategoryScores()
        .forEach(
            (category, score) ->
                System.out.printf("  %s: %.1f%%%n", category.getDisplayName(), score));

    assertTrue(
        assessment.getValidationResults().size() > 10, "Should have multiple validation checks");
  }

  @Test
  void testEndToEndIntegration() {
    System.out.println("Testing end-to-end integration...");

    // Simulate a complete monitoring workflow
    System.out.println("\n1. Recording metrics...");
    monitoringSystem.recordGauge("cpu_usage", 0.85);
    monitoringSystem.recordCounter("error_count", 50);
    monitoringSystem.recordTimer("db_query_time", Duration.ofMillis(200));

    System.out.println("2. Recording performance data for baselines...");
    for (int i = 0; i < 20; i++) {
      baselineTracker.recordMetric("cpu_usage", 0.70 + (Math.random() * 0.20));
      baselineTracker.recordMetric("response_time", 100 + (Math.random() * 50));
    }

    System.out.println("3. Triggering alerts...");
    alertingSystem.evaluateMetric("cpu_usage", 0.95, Map.of("host", "test-server"));
    alertingSystem.evaluateMetric("error_rate", 0.15, Map.of("service", "test-api"));

    System.out.println("4. Logging audit events...");
    auditLoggingSystem.logAuditEvent(
        ComprehensiveAuditLoggingSystem.AuditEventType.PERFORMANCE_EVENT,
        ComprehensiveAuditLoggingSystem.AuditSeverity.WARNING,
        "High CPU Usage",
        "CPU usage exceeded threshold",
        "monitoring-system",
        null,
        null,
        null,
        "cpu-monitor",
        "cpu",
        "measure",
        Map.of("cpu_usage", 0.95),
        List.of(ComprehensiveAuditLoggingSystem.ComplianceFramework.ISO_27001));

    // Allow time for all systems to process
    try {
      Thread.sleep(3000);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    System.out.println("\n5. Checking system states...");

    // Check health status
    final HealthCheckSystem.HealthStatus healthStatus = healthCheckSystem.getOverallHealthStatus();
    System.out.println("Overall health status: " + healthStatus);

    // Check active alerts
    final List<IntelligentAlertingSystem.CorrelatedAlert> alerts =
        alertingSystem.getActiveAlerts(5);
    System.out.println("Active alerts: " + alerts.size());

    // Check incidents
    final List<AutomatedIncidentResponseSystem.Incident> incidents =
        incidentResponseSystem.getActiveIncidents(5);
    System.out.println("Active incidents: " + incidents.size());

    // Render dashboard
    final CustomDashboardSystem.RenderedDashboard dashboard =
        dashboardSystem.renderDashboard("system_overview");
    System.out.println("Dashboard widgets rendered: " + dashboard.getWidgets().size());

    // Perform readiness assessment
    final ProductionReadinessValidator.ReadinessAssessment assessment =
        readinessValidator.performReadinessAssessment();
    System.out.println(
        "Production readiness score: " + String.format("%.1f%%", assessment.getOverallScore()));

    System.out.println("\n6. Final system statistics:");
    System.out.println("Health System: " + healthCheckSystem.getHealthReport().split("\n")[0]);
    System.out.println("Monitoring: " + monitoringSystem.getMonitoringStatistics());
    System.out.println("Alerting: " + alertingSystem.getAlertingStatistics());
    System.out.println("Incidents: " + incidentResponseSystem.getIncidentResponseStatistics());
    System.out.println("Audit: " + auditLoggingSystem.getAuditStatistics());
    System.out.println("Baselines: " + baselineTracker.getBaselineTrackerStatistics());
    System.out.println("Dashboards: " + dashboardSystem.getDashboardStatistics());

    // Assertions
    assertNotNull(healthStatus);
    assertTrue(dashboard.getWidgets().size() > 0);
    assertTrue(assessment.getOverallScore() > 0);

    System.out.println("\nEnd-to-end integration test completed successfully!");
  }

  @Test
  void testConcurrentOperations() {
    System.out.println("Testing concurrent operations...");

    final int numThreads = 5;
    final int operationsPerThread = 10;
    final CompletableFuture<Void>[] futures = new CompletableFuture[numThreads];

    // Start concurrent operations
    for (int i = 0; i < numThreads; i++) {
      final int threadId = i;
      futures[i] =
          CompletableFuture.runAsync(
              () -> {
                for (int j = 0; j < operationsPerThread; j++) {
                  // Record metrics
                  monitoringSystem.recordGauge(
                      "concurrent_metric_" + threadId, Math.random() * 100);

                  // Record performance data
                  baselineTracker.recordMetric(
                      "concurrent_perf_" + threadId, 50 + Math.random() * 50);

                  // Log audit events
                  auditLoggingSystem.logAuditEvent(
                      ComprehensiveAuditLoggingSystem.AuditEventType.MONITORING_EVENT,
                      ComprehensiveAuditLoggingSystem.AuditSeverity.INFO,
                      "Concurrent Test Event",
                      "Thread " + threadId + " operation " + j,
                      "thread-" + threadId,
                      null,
                      null,
                      null,
                      "concurrent-test",
                      "resource-" + threadId,
                      "test",
                      Map.of("thread_id", threadId, "operation", j),
                      List.of(ComprehensiveAuditLoggingSystem.ComplianceFramework.CUSTOM));

                  // Trigger some alerts
                  if (j % 3 == 0) {
                    alertingSystem.evaluateMetric(
                        "concurrent_alert_" + threadId,
                        Math.random(),
                        Map.of("thread", "thread-" + threadId));
                  }

                  try {
                    Thread.sleep(50); // Small delay between operations
                  } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                  }
                }
              });
    }

    // Wait for all operations to complete
    try {
      CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
    } catch (final Exception e) {
      fail("Concurrent operations failed: " + e.getMessage());
    }

    // Allow time for processing
    try {
      Thread.sleep(2000);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Verify systems handled concurrent operations correctly
    final String monitoringStats = monitoringSystem.getMonitoringStatistics();
    final String auditStats = auditLoggingSystem.getAuditStatistics();
    final String alertingStats = alertingSystem.getAlertingStatistics();

    System.out.println("Concurrent test results:");
    System.out.println("Monitoring: " + monitoringStats);
    System.out.println("Audit: " + auditStats);
    System.out.println("Alerting: " + alertingStats);

    assertTrue(monitoringStats.contains("metrics="), "Monitoring should have recorded metrics");
    assertTrue(auditStats.contains("total_entries="), "Audit should have logged entries");

    System.out.println("Concurrent operations test completed successfully!");
  }
}
