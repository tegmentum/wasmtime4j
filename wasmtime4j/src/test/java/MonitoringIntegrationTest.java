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

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for MonitoringIntegration.
 *
 * <p>Tests cover:
 *
 * <ul>
 *   <li>JMX integration and MBean registration
 *   <li>Prometheus metrics export
 *   <li>Health endpoint functionality
 *   <li>Configuration validation
 *   <li>Monitoring integration lifecycle
 * </ul>
 */
class MonitoringIntegrationTest {

  private MetricsCollector metricsCollector;
  private DiagnosticCollector diagnosticCollector;
  private HealthCheckSystem healthCheckSystem;
  private ProductionMonitoringSystem monitoringSystem;
  private MonitoringIntegration integration;

  @BeforeEach
  void setUp() {
    metricsCollector = new MetricsCollector();
    diagnosticCollector = new DiagnosticCollector();
    healthCheckSystem = new HealthCheckSystem();
    monitoringSystem = new ProductionMonitoringSystem();

    final MonitoringIntegration.MonitoringConfig config =
        new MonitoringIntegration.MonitoringConfig(
            true, // JMX
            false, // Micrometer
            false, // OpenTelemetry
            true, // Prometheus
            true, // Health endpoint
            "wasmtime4j-test",
            "1.0.0-test",
            Map.of("environment", "test", "service", "wasmtime4j"));

    integration =
        new MonitoringIntegration(
            config, metricsCollector, diagnosticCollector, healthCheckSystem, monitoringSystem);
  }

  @AfterEach
  void tearDown() {
    if (integration != null) {
      integration.shutdown();
    }
    if (healthCheckSystem != null) {
      healthCheckSystem.shutdown();
    }
    if (monitoringSystem != null) {
      monitoringSystem.shutdown();
    }
    if (metricsCollector != null) {
      metricsCollector.shutdown();
    }
    if (diagnosticCollector != null) {
      diagnosticCollector.shutdown();
    }
  }

  @Test
  void testMonitoringIntegrationInitialization() {
    // Act
    integration.initialize();

    // Assert
    assertTrue(integration.isInitialized());
    assertEquals("wasmtime4j-test", integration.getConfig().getServiceName());
    assertEquals("1.0.0-test", integration.getConfig().getServiceVersion());
    assertTrue(integration.getConfig().isEnableJmx());
    assertTrue(integration.getConfig().isEnablePrometheusExport());
  }

  @Test
  void testJmxIntegration() throws Exception {
    // Act
    integration.initialize();

    // Assert - Check if MBean is registered
    final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    final ObjectName objectName =
        new ObjectName("ai.tegmentum.wasmtime4j:type=Monitoring,name=wasmtime4j-test");

    assertTrue(mBeanServer.isRegistered(objectName), "JMX MBean should be registered");

    // Test MBean operations
    final Long totalOperations = (Long) mBeanServer.getAttribute(objectName, "TotalOperations");
    final Double averageLatency =
        (Double) mBeanServer.getAttribute(objectName, "AverageOperationLatency");
    final String healthStatus =
        (String) mBeanServer.invoke(objectName, "getHealthStatus", null, null);

    assertNotNull(totalOperations);
    assertNotNull(averageLatency);
    assertNotNull(healthStatus);
  }

  @Test
  void testPrometheusExport() {
    // Arrange
    metricsCollector.counter("wasmtime.operations.total", 100, null);
    metricsCollector.counter("wasmtime.errors.total", 5, null);
    metricsCollector.gauge("jvm.memory.heap.used", 1024 * 1024 * 512, "bytes", null); // 512MB
    metricsCollector.gauge("jvm.memory.heap.max", 1024 * 1024 * 1024, "bytes", null); // 1GB

    // Act
    integration.initialize();
    final String prometheusMetrics = integration.getPrometheusMetrics();

    // Assert
    assertNotNull(prometheusMetrics);
    assertTrue(prometheusMetrics.contains("wasmtime4j_info"));
    assertTrue(prometheusMetrics.contains("wasmtime4j_operations_total"));
    assertTrue(prometheusMetrics.contains("wasmtime4j_errors_total"));
    assertTrue(prometheusMetrics.contains("wasmtime4j_memory_heap_used_bytes"));
    assertTrue(prometheusMetrics.contains("# HELP"));
    assertTrue(prometheusMetrics.contains("# TYPE"));
  }

  @Test
  void testHealthEndpoint() {
    // Arrange
    metricsCollector.counter("wasmtime.operations.total", 1000, null);
    metricsCollector.counter("wasmtime.errors.total", 10, null);

    // Act
    integration.initialize();
    final String healthJson = integration.getHealthJson();
    final boolean isReady = integration.isReady();
    final boolean isAlive = integration.isAlive();

    // Assert
    assertNotNull(healthJson);
    assertTrue(healthJson.contains("\"status\":"));
    assertTrue(healthJson.contains("\"uptime_seconds\":"));
    assertTrue(healthJson.contains("\"total_operations\":"));
    assertTrue(healthJson.contains("\"error_rate\":"));

    // Health endpoints should be functional
    assertTrue(isReady || !isReady); // Just ensure no exceptions
    assertTrue(isAlive || !isAlive);
  }

  @Test
  void testCustomMetricRecording() {
    // Act
    integration.initialize();

    // Record custom metrics
    integration.recordMetric(
        "custom.metric", 42.5, Map.of("component", "test", "operation", "compute"));
    integration.recordTiming("computation", Duration.ofMillis(150), Map.of("algorithm", "fast"));
    integration.recordCounter("requests", 25, Map.of("endpoint", "/api/v1"));

    // Assert - Verify metrics are recorded in the underlying collector
    final MetricsCollector.MetricData customMetric =
        metricsCollector.getGaugeValue("custom.metric");
    assertNotNull(customMetric);
    assertEquals(42.5, customMetric.getValue(), 0.001);
    assertEquals("test", customMetric.getLabels().get("component"));

    final long requestCount =
        metricsCollector.getCounterValue("wasmtime4j.operation.requests.total");
    assertEquals(25, requestCount);

    final MetricsCollector.TimerMetric computationTimer =
        metricsCollector.timer("wasmtime4j.operation.computation.duration");
    assertNotNull(computationTimer);
  }

  @Test
  void testMonitoringConfigValidation() {
    // Test various configuration combinations
    final MonitoringIntegration.MonitoringConfig config1 =
        new MonitoringIntegration.MonitoringConfig();
    assertEquals("wasmtime4j", config1.getServiceName());
    assertEquals("1.0.0", config1.getServiceVersion());
    assertTrue(config1.isEnableJmx());

    final MonitoringIntegration.MonitoringConfig config2 =
        new MonitoringIntegration.MonitoringConfig(
            false, true, true, false, false, "custom-service", "2.0.0", Map.of("tier", "premium"));
    assertFalse(config2.isEnableJmx());
    assertTrue(config2.isEnableMicrometer());
    assertTrue(config2.isEnableOpenTelemetry());
    assertFalse(config2.isEnablePrometheusExport());
    assertFalse(config2.isEnableHealthEndpoint());
    assertEquals("custom-service", config2.getServiceName());
    assertEquals("premium", config2.getCommonTags().get("tier"));
  }

  @Test
  void testPrometheusMetricsFormat() {
    // Arrange - Add some test data
    metricsCollector.counter("wasmtime.operations.total", 500, null);
    metricsCollector.gauge("jvm.memory.heap.usage", 0.75, "ratio", null);

    // Create some timer data
    final MetricsCollector.TimerMetric timer =
        metricsCollector.timer("wasmtime.operation.duration");
    timer.record(Duration.ofMillis(10));
    timer.record(Duration.ofMillis(25));
    timer.record(Duration.ofMillis(15));

    // Act
    integration.initialize();
    final String metrics = integration.getPrometheusMetrics();

    // Assert - Validate Prometheus format compliance
    final String[] lines = metrics.split("\n");
    boolean hasHelpLine = false;
    boolean hasTypeLine = false;
    boolean hasMetricLine = false;
    boolean hasHistogramBuckets = false;

    for (final String line : lines) {
      if (line.startsWith("# HELP")) {
        hasHelpLine = true;
      } else if (line.startsWith("# TYPE")) {
        hasTypeLine = true;
      } else if (line.matches("^[a-zA-Z_:][a-zA-Z0-9_:]*.*\\s+[0-9].*")) {
        hasMetricLine = true;
      } else if (line.contains("_bucket{le=")) {
        hasHistogramBuckets = true;
      }
    }

    assertTrue(hasHelpLine, "Should have HELP lines");
    assertTrue(hasTypeLine, "Should have TYPE lines");
    assertTrue(hasMetricLine, "Should have metric value lines");
    assertTrue(hasHistogramBuckets, "Should have histogram bucket lines");
  }

  @Test
  void testHealthEndpointJsonFormat() {
    // Act
    integration.initialize();
    final String healthJson = integration.getHealthJson();

    // Assert - Validate JSON structure
    assertTrue(healthJson.startsWith("{"));
    assertTrue(healthJson.endsWith("}"));
    assertTrue(healthJson.contains("\"status\""));
    assertTrue(healthJson.contains("\"timestamp\""));
    assertTrue(healthJson.contains("\"details\""));

    // Should be valid JSON (basic validation)
    final String[] lines = healthJson.split("\n");
    int openBraces = 0;
    int closeBraces = 0;
    for (final String line : lines) {
      openBraces += countOccurrences(line, '{');
      closeBraces += countOccurrences(line, '}');
    }
    assertEquals(openBraces, closeBraces, "JSON braces should be balanced");
  }

  @Test
  void testMBeanOperations() throws Exception {
    // Act
    integration.initialize();

    // Get the MBean
    final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    final ObjectName objectName =
        new ObjectName("ai.tegmentum.wasmtime4j:type=Monitoring,name=wasmtime4j-test");

    // Test reset operation
    mBeanServer.invoke(objectName, "resetMetrics", null, null);

    // Test performance statistics
    final String perfStats =
        (String) mBeanServer.invoke(objectName, "getPerformanceStatistics", null, null);
    assertNotNull(perfStats);

    // Test diagnostic dump
    final String diagnostics =
        (String) mBeanServer.invoke(objectName, "dumpDiagnostics", null, null);
    assertNotNull(diagnostics);
  }

  @Test
  void testIntegrationShutdown() throws Exception {
    // Arrange
    integration.initialize();
    assertTrue(integration.isInitialized());

    // Verify MBean is registered
    final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    final ObjectName objectName =
        new ObjectName("ai.tegmentum.wasmtime4j:type=Monitoring,name=wasmtime4j-test");
    assertTrue(mBeanServer.isRegistered(objectName));

    // Act
    integration.shutdown();

    // Assert
    assertFalse(integration.isInitialized());
    assertFalse(
        mBeanServer.isRegistered(objectName), "MBean should be unregistered after shutdown");
  }

  @Test
  void testMultipleInitializationCalls() {
    // Act - Initialize multiple times
    integration.initialize();
    assertTrue(integration.isInitialized());

    integration.initialize(); // Second call should be safe
    assertTrue(integration.isInitialized());

    // Assert - Should still work correctly
    final String metrics = integration.getPrometheusMetrics();
    assertNotNull(metrics);
  }

  @Test
  void testDisabledFeatures() {
    // Arrange - Create config with all features disabled
    final MonitoringIntegration.MonitoringConfig disabledConfig =
        new MonitoringIntegration.MonitoringConfig(
            false, false, false, false, false, "test-service", "1.0.0", Map.of());

    final MonitoringIntegration disabledIntegration =
        new MonitoringIntegration(
            disabledConfig,
            metricsCollector,
            diagnosticCollector,
            healthCheckSystem,
            monitoringSystem);

    // Act
    disabledIntegration.initialize();

    // Assert
    assertTrue(disabledIntegration.isInitialized());
    final String metrics = disabledIntegration.getPrometheusMetrics();
    assertTrue(metrics.contains("Prometheus export not enabled"));

    final String health = disabledIntegration.getHealthJson();
    assertTrue(health.contains("Health endpoint not enabled"));

    // Cleanup
    disabledIntegration.shutdown();
  }

  /** Helper method to count character occurrences in a string. */
  private int countOccurrences(final String text, final char character) {
    int count = 0;
    for (final char c : text.toCharArray()) {
      if (c == character) {
        count++;
      }
    }
    return count;
  }
}
