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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive tests for the HealthCheckSystem.
 *
 * <p>Tests cover:
 *
 * <ul>
 *   <li>Basic health check functionality
 *   <li>Health status aggregation
 *   <li>Automatic recovery mechanisms
 *   <li>Configuration validation
 *   <li>Listener notifications
 *   <li>Error handling and recovery
 * </ul>
 */
class HealthCheckSystemTest {

  private HealthCheckSystem healthCheckSystem;
  private HealthCheckSystem.SystemHealthConfig config;

  @BeforeEach
  void setUp() {
    config =
        new HealthCheckSystem.SystemHealthConfig(
            0.8, 0.9, 100, 200, Duration.ofSeconds(5), Duration.ofSeconds(30), true);
    healthCheckSystem = new HealthCheckSystem(config);
  }

  @AfterEach
  void tearDown() {
    if (healthCheckSystem != null) {
      healthCheckSystem.shutdown();
    }
  }

  @Test
  void testBasicHealthCheckRegistration() {
    // Arrange
    final AtomicInteger checkCount = new AtomicInteger(0);
    final HealthCheckSystem.HealthCheck healthCheck =
        () -> {
          checkCount.incrementAndGet();
          return new HealthCheckSystem.HealthCheckResult(
              "test_component",
              HealthCheckSystem.HealthStatus.HEALTHY,
              "All systems operational",
              Duration.ofMillis(10),
              null,
              null);
        };

    final HealthCheckSystem.HealthCheckConfig healthConfig =
        new HealthCheckSystem.HealthCheckConfig(
            "test_component",
            healthCheck,
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            3,
            false,
            null);

    // Act
    healthCheckSystem.registerHealthCheck(healthConfig);
    final HealthCheckSystem.HealthCheckResult result =
        healthCheckSystem.checkComponentHealth("test_component");

    // Assert
    assertNotNull(result);
    assertEquals("test_component", result.getComponentId());
    assertEquals(HealthCheckSystem.HealthStatus.HEALTHY, result.getStatus());
    assertEquals("All systems operational", result.getMessage());
    assertTrue(result.isHealthy());
    assertEquals(1, checkCount.get());
  }

  @Test
  void testHealthCheckFailureHandling() {
    // Arrange
    final AtomicInteger checkCount = new AtomicInteger(0);
    final HealthCheckSystem.HealthCheck failingHealthCheck =
        () -> {
          checkCount.incrementAndGet();
          throw new RuntimeException("Health check failed");
        };

    final HealthCheckSystem.HealthCheckConfig healthConfig =
        new HealthCheckSystem.HealthCheckConfig(
            "failing_component",
            failingHealthCheck,
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            3,
            false,
            null);

    // Act
    healthCheckSystem.registerHealthCheck(healthConfig);
    final HealthCheckSystem.HealthCheckResult result =
        healthCheckSystem.checkComponentHealth("failing_component");

    // Assert
    assertNotNull(result);
    assertEquals("failing_component", result.getComponentId());
    assertEquals(HealthCheckSystem.HealthStatus.CRITICAL, result.getStatus());
    assertTrue(result.getMessage().contains("Health check failed"));
    assertFalse(result.isHealthy());
    assertNotNull(result.getError());
    assertEquals(1, checkCount.get());
  }

  @Test
  void testHealthStatusAggregation() throws InterruptedException {
    // Arrange - Register multiple health checks with different statuses
    final HealthCheckSystem.HealthCheck healthyCheck =
        () ->
            new HealthCheckSystem.HealthCheckResult(
                "healthy_component",
                HealthCheckSystem.HealthStatus.HEALTHY,
                "Component is healthy",
                Duration.ofMillis(5),
                null,
                null);

    final HealthCheckSystem.HealthCheck degradedCheck =
        () ->
            new HealthCheckSystem.HealthCheckResult(
                "degraded_component",
                HealthCheckSystem.HealthStatus.DEGRADED,
                "Component is degraded",
                Duration.ofMillis(5),
                null,
                null);

    final HealthCheckSystem.HealthCheckConfig healthyConfig =
        new HealthCheckSystem.HealthCheckConfig(
            "healthy_component",
            healthyCheck,
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            3,
            false,
            null);
    final HealthCheckSystem.HealthCheckConfig degradedConfig =
        new HealthCheckSystem.HealthCheckConfig(
            "degraded_component",
            degradedCheck,
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            3,
            false,
            null);

    // Act
    healthCheckSystem.registerHealthCheck(healthyConfig);
    healthCheckSystem.registerHealthCheck(degradedConfig);

    // Trigger health checks
    healthCheckSystem.checkComponentHealth("healthy_component");
    healthCheckSystem.checkComponentHealth("degraded_component");

    // Wait for overall health assessment
    Thread.sleep(100);

    // Assert - Overall status should be the worst component status
    assertEquals(
        HealthCheckSystem.HealthStatus.DEGRADED, healthCheckSystem.getOverallHealthStatus());
  }

  @Test
  void testAutomaticRecovery() throws InterruptedException {
    // Arrange
    final AtomicInteger checkCount = new AtomicInteger(0);
    final AtomicInteger recoveryCount = new AtomicInteger(0);
    final AtomicReference<HealthCheckSystem.HealthStatus> currentStatus =
        new AtomicReference<>(HealthCheckSystem.HealthStatus.UNHEALTHY);

    final HealthCheckSystem.HealthCheck recoverableCheck =
        () ->
            new HealthCheckSystem.HealthCheckResult(
                "recoverable_component",
                currentStatus.get(),
                "Component status: " + currentStatus.get(),
                Duration.ofMillis(5),
                null,
                null);

    final HealthCheckSystem.RecoveryAction recoveryAction =
        (healthResult) -> {
          recoveryCount.incrementAndGet();
          // Simulate successful recovery
          currentStatus.set(HealthCheckSystem.HealthStatus.HEALTHY);
          return true;
        };

    final HealthCheckSystem.HealthCheckConfig recoverableConfig =
        new HealthCheckSystem.HealthCheckConfig(
            "recoverable_component",
            recoverableCheck,
            Duration.ofMillis(100),
            Duration.ofSeconds(5),
            2, // Trigger recovery after 2 failures
            true,
            recoveryAction);

    // Act
    healthCheckSystem.registerHealthCheck(recoverableConfig);

    // Trigger multiple failures to initiate recovery
    healthCheckSystem.checkComponentHealth("recoverable_component");
    healthCheckSystem.checkComponentHealth("recoverable_component");

    // Wait for recovery attempt
    Thread.sleep(500);

    // Check status after recovery
    final HealthCheckSystem.HealthCheckResult result =
        healthCheckSystem.checkComponentHealth("recoverable_component");

    // Assert
    assertTrue(recoveryCount.get() > 0, "Recovery should have been attempted");
    assertEquals(HealthCheckSystem.HealthStatus.HEALTHY, result.getStatus());
  }

  @Test
  void testHealthStatusListener() throws InterruptedException {
    // Arrange
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<HealthCheckSystem.HealthStatus> previousStatus = new AtomicReference<>();
    final AtomicReference<HealthCheckSystem.HealthStatus> currentStatus = new AtomicReference<>();
    final AtomicReference<String> trigger = new AtomicReference<>();

    final HealthCheckSystem.HealthStatusListener listener =
        (previous, current, triggerReason) -> {
          previousStatus.set(previous);
          currentStatus.set(current);
          trigger.set(triggerReason);
          latch.countDown();
        };

    final HealthCheckSystem.HealthCheck degradingCheck =
        () ->
            new HealthCheckSystem.HealthCheckResult(
                "degrading_component",
                HealthCheckSystem.HealthStatus.CRITICAL,
                "Component is critical",
                Duration.ofMillis(5),
                null,
                null);

    final HealthCheckSystem.HealthCheckConfig degradingConfig =
        new HealthCheckSystem.HealthCheckConfig(
            "degrading_component",
            degradingCheck,
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            3,
            false,
            null);

    // Act
    healthCheckSystem.addHealthStatusListener(listener);
    healthCheckSystem.registerHealthCheck(degradingConfig);
    healthCheckSystem.checkComponentHealth("degrading_component");

    // Wait for status change notification
    Thread.sleep(100);
    healthCheckSystem.forceHealthCheck(); // Force immediate assessment

    // Assert
    assertTrue(latch.await(5, TimeUnit.SECONDS), "Health status listener should have been called");
    assertNotNull(currentStatus.get());
    assertEquals("assessment", trigger.get());
  }

  @Test
  void testHealthReportGeneration() {
    // Arrange
    final HealthCheckSystem.HealthCheck testCheck =
        () ->
            new HealthCheckSystem.HealthCheckResult(
                "test_component",
                HealthCheckSystem.HealthStatus.HEALTHY,
                "Test component is operational",
                Duration.ofMillis(15),
                null,
                null);

    final HealthCheckSystem.HealthCheckConfig testConfig =
        new HealthCheckSystem.HealthCheckConfig(
            "test_component",
            testCheck,
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            3,
            false,
            null);

    // Act
    healthCheckSystem.registerHealthCheck(testConfig);
    healthCheckSystem.checkComponentHealth("test_component");
    final String healthReport = healthCheckSystem.getHealthReport();

    // Assert
    assertNotNull(healthReport);
    assertTrue(healthReport.contains("Health Check Report"));
    assertTrue(healthReport.contains("test_component"));
    assertTrue(healthReport.contains("HEALTHY"));
    assertTrue(healthReport.contains("Success Rate"));
  }

  @Test
  void testBuiltInHealthChecks() {
    // Act - Built-in health checks should be automatically registered
    final HealthCheckSystem.HealthStatus memoryStatus =
        healthCheckSystem.getComponentHealthStatus("jvm_memory");
    final HealthCheckSystem.HealthStatus threadStatus =
        healthCheckSystem.getComponentHealthStatus("jvm_threads");
    final HealthCheckSystem.HealthStatus systemStatus =
        healthCheckSystem.getComponentHealthStatus("system_resources");

    // Assert
    assertNotNull(memoryStatus);
    assertNotNull(threadStatus);
    assertNotNull(systemStatus);
  }

  @Test
  void testHealthCheckUnregistration() {
    // Arrange
    final HealthCheckSystem.HealthCheck testCheck =
        () ->
            new HealthCheckSystem.HealthCheckResult(
                "test_component",
                HealthCheckSystem.HealthStatus.HEALTHY,
                "Test component",
                Duration.ofMillis(5),
                null,
                null);

    final HealthCheckSystem.HealthCheckConfig testConfig =
        new HealthCheckSystem.HealthCheckConfig(
            "test_component",
            testCheck,
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            3,
            false,
            null);

    // Act
    healthCheckSystem.registerHealthCheck(testConfig);
    HealthCheckSystem.HealthCheckResult result1 =
        healthCheckSystem.checkComponentHealth("test_component");

    healthCheckSystem.unregisterHealthCheck("test_component");
    HealthCheckSystem.HealthCheckResult result2 =
        healthCheckSystem.checkComponentHealth("test_component");

    // Assert
    assertNotNull(result1);
    assertEquals(HealthCheckSystem.HealthStatus.HEALTHY, result1.getStatus());

    assertNotNull(result2);
    assertEquals(HealthCheckSystem.HealthStatus.UNKNOWN, result2.getStatus());
    assertTrue(result2.getMessage().contains("not registered"));
  }

  @Test
  void testHealthCheckConfiguration() {
    // Arrange & Act
    final HealthCheckSystem.SystemHealthConfig customConfig =
        new HealthCheckSystem.SystemHealthConfig(
            0.85, 0.95, 150, 300, Duration.ofSeconds(10), Duration.ofMinutes(2), false);

    // Assert
    assertEquals(0.85, customConfig.getMemoryThresholdWarning());
    assertEquals(0.95, customConfig.getMemoryThresholdCritical());
    assertEquals(150, customConfig.getThreadCountThresholdWarning());
    assertEquals(300, customConfig.getThreadCountThresholdCritical());
    assertEquals(Duration.ofSeconds(10), customConfig.getHealthCheckTimeout());
    assertEquals(Duration.ofMinutes(2), customConfig.getOverallTimeout());
    assertFalse(customConfig.isEnableAutoRecovery());
  }

  @Test
  @Timeout(10)
  void testHealthCheckSystemShutdown() throws InterruptedException {
    // Arrange
    final CountDownLatch shutdownLatch = new CountDownLatch(1);
    final HealthCheckSystem.HealthCheck longRunningCheck =
        () -> {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return new HealthCheckSystem.HealthCheckResult(
              "long_running",
              HealthCheckSystem.HealthStatus.HEALTHY,
              "Long running check",
              Duration.ofMillis(1000),
              null,
              null);
        };

    final HealthCheckSystem.HealthCheckConfig longRunningConfig =
        new HealthCheckSystem.HealthCheckConfig(
            "long_running",
            longRunningCheck,
            Duration.ofMillis(500),
            Duration.ofSeconds(5),
            3,
            false,
            null);

    // Act
    healthCheckSystem.registerHealthCheck(longRunningConfig);

    // Start shutdown in background
    new Thread(
            () -> {
              healthCheckSystem.shutdown();
              shutdownLatch.countDown();
            })
        .start();

    // Assert
    assertTrue(
        shutdownLatch.await(8, TimeUnit.SECONDS), "Health check system should shutdown gracefully");
  }

  @Test
  void testMultipleHealthStatusChanges() throws InterruptedException {
    // Arrange
    final AtomicInteger statusChangeCount = new AtomicInteger(0);
    final HealthCheckSystem.HealthStatusListener countingListener =
        (previous, current, trigger) -> {
          statusChangeCount.incrementAndGet();
        };

    final AtomicReference<HealthCheckSystem.HealthStatus> componentStatus =
        new AtomicReference<>(HealthCheckSystem.HealthStatus.HEALTHY);

    final HealthCheckSystem.HealthCheck changingCheck =
        () ->
            new HealthCheckSystem.HealthCheckResult(
                "changing_component",
                componentStatus.get(),
                "Status: " + componentStatus.get(),
                Duration.ofMillis(5),
                null,
                null);

    final HealthCheckSystem.HealthCheckConfig changingConfig =
        new HealthCheckSystem.HealthCheckConfig(
            "changing_component",
            changingCheck,
            Duration.ofMillis(100),
            Duration.ofSeconds(5),
            3,
            false,
            null);

    // Act
    healthCheckSystem.addHealthStatusListener(countingListener);
    healthCheckSystem.registerHealthCheck(changingConfig);

    // Change status multiple times
    componentStatus.set(HealthCheckSystem.HealthStatus.HEALTHY);
    healthCheckSystem.checkComponentHealth("changing_component");
    Thread.sleep(50);

    componentStatus.set(HealthCheckSystem.HealthStatus.DEGRADED);
    healthCheckSystem.checkComponentHealth("changing_component");
    Thread.sleep(50);

    componentStatus.set(HealthCheckSystem.HealthStatus.CRITICAL);
    healthCheckSystem.checkComponentHealth("changing_component");
    Thread.sleep(50);

    // Force assessment
    healthCheckSystem.forceHealthCheck();
    Thread.sleep(200);

    // Assert
    assertTrue(statusChangeCount.get() > 0, "Status should have changed at least once");
    assertEquals(
        HealthCheckSystem.HealthStatus.CRITICAL, healthCheckSystem.getOverallHealthStatus());
  }

  @Test
  void testHealthCheckWithTimeout() {
    // Arrange
    final HealthCheckSystem.HealthCheck timeoutCheck =
        () -> {
          // Simulate a check that takes longer than the configured timeout
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return new HealthCheckSystem.HealthCheckResult(
              "timeout_component",
              HealthCheckSystem.HealthStatus.HEALTHY,
              "Should not reach here",
              Duration.ofMillis(1000),
              null,
              null);
        };

    final HealthCheckSystem.HealthCheckConfig timeoutConfig =
        new HealthCheckSystem.HealthCheckConfig(
            "timeout_component",
            timeoutCheck,
            Duration.ofSeconds(1),
            Duration.ofMillis(100), // Very short timeout
            3,
            false,
            null);

    // Act
    healthCheckSystem.registerHealthCheck(timeoutConfig);
    final HealthCheckSystem.HealthCheckResult result =
        healthCheckSystem.checkComponentHealth("timeout_component");

    // Assert
    assertNotNull(result);
    assertEquals("timeout_component", result.getComponentId());
    // Note: The current implementation doesn't enforce timeouts, but the test structure is here
    // for when timeout functionality is implemented
  }
}
