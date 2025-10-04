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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for ProductionFeatures including feature flags, circuit breakers, and
 * graceful degradation.
 */
class ProductionFeaturesTest {

  private MonitoringConfigManager configManager;
  private ProductionFeatures productionFeatures;

  @BeforeEach
  void setUp() {
    configManager = MonitoringConfigManager.getInstance();
    productionFeatures = new ProductionFeatures(configManager);
  }

  @AfterEach
  void tearDown() {
    // ProductionFeatures doesn't have explicit cleanup
  }

  @Test
  void testFeatureFlagBasicOperations() {
    // Arrange
    final ProductionFeatures.FeatureFlag testFlag =
        new ProductionFeatures.FeatureFlag(
            "test.feature", "Test feature description", ProductionFeatures.FeatureState.ENABLED);

    // Act
    productionFeatures.addFeatureFlag(testFlag);
    final boolean isEnabled = productionFeatures.isFeatureEnabled("test.feature");
    final boolean isAvailable = productionFeatures.isFeatureAvailable("test.feature");
    final ProductionFeatures.FeatureFlag retrievedFlag =
        productionFeatures.getFeatureFlag("test.feature");

    // Assert
    assertTrue(isEnabled);
    assertTrue(isAvailable);
    assertNotNull(retrievedFlag);
    assertEquals("test.feature", retrievedFlag.getName());
    assertEquals("Test feature description", retrievedFlag.getDescription());
    assertEquals(ProductionFeatures.FeatureState.ENABLED, retrievedFlag.getState());
    assertEquals(2, retrievedFlag.getUsageCount()); // Called twice (isEnabled and isAvailable)
  }

  @Test
  void testFeatureFlagStateTransitions() {
    // Arrange
    final ProductionFeatures.FeatureFlag testFlag =
        new ProductionFeatures.FeatureFlag(
            "transition.feature",
            "Feature for testing transitions",
            ProductionFeatures.FeatureState.ENABLED);

    // Act & Assert
    productionFeatures.addFeatureFlag(testFlag);

    // Test ENABLED state
    assertTrue(productionFeatures.isFeatureEnabled("transition.feature"));
    assertTrue(productionFeatures.isFeatureAvailable("transition.feature"));

    // Change to DEGRADED
    productionFeatures.setFeatureState(
        "transition.feature", ProductionFeatures.FeatureState.DEGRADED);
    assertFalse(productionFeatures.isFeatureEnabled("transition.feature"));
    assertTrue(productionFeatures.isFeatureAvailable("transition.feature"));

    // Change to DISABLED
    productionFeatures.setFeatureState(
        "transition.feature", ProductionFeatures.FeatureState.DISABLED);
    assertFalse(productionFeatures.isFeatureEnabled("transition.feature"));
    assertFalse(productionFeatures.isFeatureAvailable("transition.feature"));

    // Change to TESTING
    productionFeatures.setFeatureState(
        "transition.feature", ProductionFeatures.FeatureState.TESTING);
    assertTrue(productionFeatures.isFeatureEnabled("transition.feature"));
    assertTrue(productionFeatures.isFeatureAvailable("transition.feature"));
  }

  @Test
  void testFeatureFlagRolloutPercentage() {
    // Arrange
    final ProductionFeatures.FeatureFlag rolloutFlag =
        new ProductionFeatures.FeatureFlag(
            "rollout.feature",
            "Feature with rollout percentage",
            ProductionFeatures.FeatureState.ENABLED,
            50); // 50% rollout

    // Act
    productionFeatures.addFeatureFlag(rolloutFlag);
    final ProductionFeatures.FeatureFlag retrievedFlag =
        productionFeatures.getFeatureFlag("rollout.feature");

    // Assert
    assertNotNull(retrievedFlag);
    assertEquals(50, retrievedFlag.getRolloutPercentage());

    // Test percentage bounds
    retrievedFlag.setRolloutPercentage(150); // Should be clamped to 100
    assertEquals(100, retrievedFlag.getRolloutPercentage());

    retrievedFlag.setRolloutPercentage(-10); // Should be clamped to 0
    assertEquals(0, retrievedFlag.getRolloutPercentage());
  }

  @Test
  void testCircuitBreakerBasicOperations() throws Exception {
    // Arrange
    final AtomicInteger operationCount = new AtomicInteger(0);
    final AtomicBoolean shouldFail = new AtomicBoolean(false);

    productionFeatures.addCircuitBreaker(
        "test.service", 3, Duration.ofSeconds(5), Duration.ofSeconds(2));
    final ProductionFeatures.CircuitBreaker circuitBreaker =
        productionFeatures.getCircuitBreaker("test.service");

    final ProductionFeatures.Operation<String> testOperation =
        () -> {
          operationCount.incrementAndGet();
          if (shouldFail.get()) {
            throw new RuntimeException("Operation failed");
          }
          return "success";
        };

    // Act & Assert - Test successful operations
    assertNotNull(circuitBreaker);
    assertEquals(ProductionFeatures.CircuitBreakerState.CLOSED, circuitBreaker.getState());

    final String result1 = circuitBreaker.execute(testOperation);
    assertEquals("success", result1);
    assertEquals(1, operationCount.get());

    // Test failures that trip the circuit
    shouldFail.set(true);
    for (int i = 0; i < 3; i++) {
      try {
        circuitBreaker.execute(testOperation);
        fail("Should have thrown exception");
      } catch (RuntimeException e) {
        assertEquals("Operation failed", e.getMessage());
      }
    }

    // Circuit should now be OPEN
    assertEquals(ProductionFeatures.CircuitBreakerState.OPEN, circuitBreaker.getState());
    assertEquals(3, circuitBreaker.getConsecutiveFailures());

    // Next call should fail fast
    try {
      circuitBreaker.execute(testOperation);
      fail("Should have thrown CircuitBreakerException");
    } catch (ProductionFeatures.CircuitBreakerException e) {
      assertTrue(e.getMessage().contains("Circuit breaker is OPEN"));
    }
  }

  @Test
  void testCircuitBreakerRecovery() throws Exception {
    // Arrange
    final AtomicBoolean shouldFail = new AtomicBoolean(true);
    productionFeatures.addCircuitBreaker(
        "recovery.service", 2, Duration.ofMillis(100), Duration.ofMillis(50));
    final ProductionFeatures.CircuitBreaker circuitBreaker =
        productionFeatures.getCircuitBreaker("recovery.service");

    final ProductionFeatures.Operation<String> recoveringOperation =
        () -> {
          if (shouldFail.get()) {
            throw new RuntimeException("Still failing");
          }
          return "recovered";
        };

    // Act - Trip the circuit
    for (int i = 0; i < 2; i++) {
      try {
        circuitBreaker.execute(recoveringOperation);
      } catch (RuntimeException ignored) {
        // Expected failures
      }
    }
    assertEquals(ProductionFeatures.CircuitBreakerState.OPEN, circuitBreaker.getState());

    // Wait for timeout period
    Thread.sleep(150);

    // Fix the operation
    shouldFail.set(false);

    // Next call should transition to HALF_OPEN and then CLOSED on success
    final String result = circuitBreaker.execute(recoveringOperation);

    // Assert
    assertEquals("recovered", result);
    assertEquals(ProductionFeatures.CircuitBreakerState.CLOSED, circuitBreaker.getState());
    assertEquals(0, circuitBreaker.getConsecutiveFailures());
  }

  @Test
  void testGracefulDegradation() throws Exception {
    // Arrange
    final AtomicInteger normalOperationCount = new AtomicInteger(0);
    final AtomicInteger degradedOperationCount = new AtomicInteger(0);
    final AtomicBoolean shouldFailNormal = new AtomicBoolean(false);

    productionFeatures.addGracefulDegradation("test.component");
    final ProductionFeatures.GracefulDegradation gracefulDegradation =
        productionFeatures.getGracefulDegradation("test.component");

    final ProductionFeatures.Operation<String> normalOperation =
        () -> {
          normalOperationCount.incrementAndGet();
          if (shouldFailNormal.get()) {
            throw new java.util.concurrent.TimeoutException("Normal operation timed out");
          }
          return "normal_result";
        };

    final ProductionFeatures.Operation<String> degradedOperation =
        () -> {
          degradedOperationCount.incrementAndGet();
          return "degraded_result";
        };

    // Act & Assert - Test normal operation
    assertNotNull(gracefulDegradation);
    assertFalse(gracefulDegradation.isDegraded());

    String result1 = gracefulDegradation.execute(normalOperation, degradedOperation);
    assertEquals("normal_result", result1);
    assertEquals(1, normalOperationCount.get());
    assertEquals(0, degradedOperationCount.get());

    // Test manual degradation
    gracefulDegradation.enterDegradedMode("Manual degradation for testing");
    assertTrue(gracefulDegradation.isDegraded());
    assertEquals("Manual degradation for testing", gracefulDegradation.getDegradationReason());

    String result2 = gracefulDegradation.execute(normalOperation, degradedOperation);
    assertEquals("degraded_result", result2);
    assertEquals(1, normalOperationCount.get()); // Should not increase
    assertEquals(1, degradedOperationCount.get());

    // Test exit degraded mode
    gracefulDegradation.exitDegradedMode();
    assertFalse(gracefulDegradation.isDegraded());

    String result3 = gracefulDegradation.execute(normalOperation, degradedOperation);
    assertEquals("normal_result", result3);
    assertEquals(2, normalOperationCount.get());
    assertEquals(1, degradedOperationCount.get());
  }

  @Test
  void testGracefulDegradationAutoTrigger() throws Exception {
    // Arrange
    final AtomicInteger normalCount = new AtomicInteger(0);
    final AtomicInteger degradedCount = new AtomicInteger(0);

    productionFeatures.addGracefulDegradation("auto.component");
    final ProductionFeatures.GracefulDegradation gracefulDegradation =
        productionFeatures.getGracefulDegradation("auto.component");

    final ProductionFeatures.Operation<String> failingOperation =
        () -> {
          normalCount.incrementAndGet();
          throw new java.util.concurrent.TimeoutException("Auto degradation trigger");
        };

    final ProductionFeatures.Operation<String> fallbackOperation =
        () -> {
          degradedCount.incrementAndGet();
          return "fallback_result";
        };

    // Act
    final String result = gracefulDegradation.execute(failingOperation, fallbackOperation);

    // Assert
    assertEquals("fallback_result", result);
    assertEquals(1, normalCount.get());
    assertEquals(1, degradedCount.get());
    assertTrue(gracefulDegradation.isDegraded());
    assertTrue(gracefulDegradation.getDegradationReason().contains("Error in normal operation"));
  }

  @Test
  void testProductionStatusReport() {
    // Arrange
    productionFeatures.addFeatureFlag(
        new ProductionFeatures.FeatureFlag(
            "status.test", "Test feature for status", ProductionFeatures.FeatureState.ENABLED));
    productionFeatures.addCircuitBreaker(
        "status.breaker", 5, Duration.ofMinutes(1), Duration.ofSeconds(30));
    productionFeatures.addGracefulDegradation("status.component");

    // Act
    final String status = productionFeatures.getProductionStatus();

    // Assert
    assertNotNull(status);
    assertTrue(status.contains("Production Features Status"));
    assertTrue(status.contains("Feature Flags:"));
    assertTrue(status.contains("Circuit Breakers:"));
    assertTrue(status.contains("Graceful Degradation:"));
    assertTrue(status.contains("status.test"));
    assertTrue(status.contains("status.breaker"));
    assertTrue(status.contains("status.component"));
  }

  @Test
  void testDefaultFeatureInitialization() {
    // Assert - Check that default features are properly initialized
    assertTrue(productionFeatures.getAllFeatureFlags().size() > 0);
    assertTrue(productionFeatures.getAllGracefulDegradation().size() > 0);

    // Check specific default features
    assertNotNull(productionFeatures.getFeatureFlag("monitoring.enabled"));
    assertNotNull(productionFeatures.getFeatureFlag("metrics.collection"));
    assertNotNull(productionFeatures.getFeatureFlag("health.checks"));
    assertNotNull(productionFeatures.getGracefulDegradation("monitoring.system"));
  }

  @Test
  void testCircuitBreakerMetrics() throws Exception {
    // Arrange
    productionFeatures.addCircuitBreaker(
        "metrics.test", 3, Duration.ofSeconds(5), Duration.ofSeconds(2));
    final ProductionFeatures.CircuitBreaker circuitBreaker =
        productionFeatures.getCircuitBreaker("metrics.test");

    final ProductionFeatures.Operation<String> operation = () -> "success";

    // Act - Execute successful operations
    circuitBreaker.execute(operation);
    circuitBreaker.execute(operation);
    circuitBreaker.execute(operation);

    // Assert
    assertEquals(3, circuitBreaker.getTotalRequests());
    assertEquals(3, circuitBreaker.getSuccessCount());
    assertEquals(0, circuitBreaker.getConsecutiveFailures());
    assertEquals(0.0, circuitBreaker.getFailureRate(), 0.001);
  }

  @Test
  void testConcurrentFeatureFlagAccess() throws InterruptedException {
    // Arrange
    final ProductionFeatures.FeatureFlag concurrentFlag =
        new ProductionFeatures.FeatureFlag(
            "concurrent.test", "Concurrent access test", ProductionFeatures.FeatureState.ENABLED);
    productionFeatures.addFeatureFlag(concurrentFlag);

    final int threadCount = 10;
    final int operationsPerThread = 100;
    final CountDownLatch latch = new CountDownLatch(threadCount);
    final AtomicInteger totalChecks = new AtomicInteger(0);

    // Act - Access feature flag from multiple threads
    for (int i = 0; i < threadCount; i++) {
      new Thread(
              () -> {
                try {
                  for (int j = 0; j < operationsPerThread; j++) {
                    if (productionFeatures.isFeatureEnabled("concurrent.test")) {
                      totalChecks.incrementAndGet();
                    }
                  }
                } finally {
                  latch.countDown();
                }
              })
          .start();
    }

    // Assert
    assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
    assertEquals(threadCount * operationsPerThread, totalChecks.get());
    assertEquals(threadCount * operationsPerThread, concurrentFlag.getUsageCount());
  }

  @Test
  void testFeatureFlagTimestamps() throws InterruptedException {
    // Arrange
    final ProductionFeatures.FeatureFlag timestampFlag =
        new ProductionFeatures.FeatureFlag(
            "timestamp.test", "Timestamp test", ProductionFeatures.FeatureState.ENABLED);

    // Act
    productionFeatures.addFeatureFlag(timestampFlag);
    final java.time.Instant createdTime = timestampFlag.getCreatedAt();

    Thread.sleep(100); // Small delay

    timestampFlag.setState(ProductionFeatures.FeatureState.DEGRADED);
    final java.time.Instant modifiedTime = timestampFlag.getLastModified();

    // Assert
    assertNotNull(createdTime);
    assertNotNull(modifiedTime);
    assertTrue(modifiedTime.isAfter(createdTime));
  }
}
