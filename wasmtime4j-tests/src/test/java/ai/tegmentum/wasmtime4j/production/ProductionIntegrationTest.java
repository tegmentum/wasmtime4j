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

package ai.tegmentum.wasmtime4j.production;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmInstance;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.resilience.CircuitBreaker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for production-grade WebAssembly runtime features.
 *
 * <p>Tests cover:
 * - Production configuration and environment setup
 * - Engine pooling and resource management
 * - Error handling and resilience patterns
 * - Memory optimization and pressure handling
 * - Monitoring and observability
 * - Performance under load
 * - Security and sandboxing
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Execution(ExecutionMode.CONCURRENT)
class ProductionIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ProductionIntegrationTest.class.getName());

  private static ProductionWasmtimeConfig productionConfig;
  private static OptimizedEnginePool enginePool;
  private static ProductionErrorHandler errorHandler;
  private static ProductionMonitoringService monitoringService;
  private static ProductionResourceManager resourceManager;
  private static MemoryOptimizationService memoryOptimizer;
  private static byte[] testWasmModule;

  @BeforeAll
  static void setupProduction() {
    LOGGER.info("Setting up production environment for integration tests");

    // Create production configuration
    productionConfig = ProductionWasmtimeConfig.forProduction();
    productionConfig.validate();

    // Initialize all production services
    enginePool = OptimizedEnginePool.createHighThroughput();
    errorHandler = ProductionErrorHandler.createDefault();
    monitoringService = ProductionMonitoringService.createDefault();
    resourceManager = ProductionResourceManager.createDefault();
    memoryOptimizer = MemoryOptimizationService.createHighThroughput();

    // Create test WASM module
    testWasmModule = createTestWasmModule();

    LOGGER.info("Production environment setup complete");
  }

  @AfterAll
  static void teardownProduction() {
    LOGGER.info("Tearing down production environment");

    // Clean shutdown of all services
    if (memoryOptimizer != null) {
      memoryOptimizer.close();
    }
    if (resourceManager != null) {
      resourceManager.close();
    }
    if (monitoringService != null) {
      monitoringService.close();
    }
    if (enginePool != null) {
      enginePool.close();
    }

    LOGGER.info("Production environment teardown complete");
  }

  @Test
  @Order(1)
  @DisplayName("Production Configuration Validation")
  void testProductionConfigurationValidation() {
    assertDoesNotThrow(() -> productionConfig.validate());

    // Verify production-specific settings
    assertEquals(ProductionWasmtimeConfig.Environment.PRODUCTION, productionConfig.getEnvironment());
    assertEquals(ProductionWasmtimeConfig.SecurityLevel.MAXIMUM, productionConfig.getSecurity().getSecurityLevel());
    assertTrue(productionConfig.getSecurity().isEnableSandboxing());
    assertTrue(productionConfig.getMonitoring().isEnableMetrics());
    assertTrue(productionConfig.getMonitoring().isEnableAlerting());

    LOGGER.info("Production configuration validation passed");
  }

  @Test
  @Order(2)
  @DisplayName("Engine Pool Production Performance")
  void testEnginePoolProductionPerformance() throws Exception {
    final int requestCount = 1000;
    final ExecutorService executor = Executors.newFixedThreadPool(10);
    final CountDownLatch latch = new CountDownLatch(requestCount);
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger errorCount = new AtomicInteger(0);

    final long startTime = System.currentTimeMillis();

    for (int i = 0; i < requestCount; i++) {
      executor.submit(() -> {
        try {
          final Engine engine = enginePool.acquireEngine();
          try {
            final Module module = engine.compileModule(testWasmModule);
            final Store store = new Store(engine);
            final WasmInstance instance = new WasmInstance(store, module);
            final WasmFunction function = instance.getFunction("add");
            final Object result = function.call(new Object[]{1, 2});

            assertEquals(3, result);
            successCount.incrementAndGet();

            instance.close();
            store.close();
            module.close();
          } finally {
            enginePool.returnEngine(engine);
          }
        } catch (final Exception e) {
          errorCount.incrementAndGet();
          LOGGER.warning("Engine pool operation failed: " + e.getMessage());
        } finally {
          latch.countDown();
        }
      });
    }

    assertTrue(latch.await(60, TimeUnit.SECONDS), "Engine pool performance test timed out");

    final long endTime = System.currentTimeMillis();
    final long duration = endTime - startTime;

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    // Performance assertions
    assertEquals(requestCount, successCount.get() + errorCount.get());
    assertTrue(successCount.get() > requestCount * 0.95, "Success rate should be above 95%");
    assertTrue(duration < 30000, "Should complete within 30 seconds");

    final OptimizedEnginePool.PoolStatistics stats = enginePool.getStatistics();
    LOGGER.info(String.format("Pool performance: %d requests in %dms, success rate: %.1f%%, %s",
        requestCount, duration, (double) successCount.get() / requestCount * 100, stats));
  }

  @Test
  @Order(3)
  @DisplayName("Error Handling and Circuit Breaker")
  void testErrorHandlingAndCircuitBreaker() {
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger failureCount = new AtomicInteger(0);

    // Test successful operations
    for (int i = 0; i < 5; i++) {
      assertDoesNotThrow(() -> {
        final Object result = errorHandler.handleWithRecovery(() -> {
          // Simulate successful operation
          return "success";
        }, "test-operation");
        assertEquals("success", result);
        successCount.incrementAndGet();
      });
    }

    // Test error handling with retry
    assertThrows(WasmException.class, () -> {
      errorHandler.handleWithRecovery(() -> {
        failureCount.incrementAndGet();
        throw new RuntimeException("Simulated failure");
      }, "failing-operation");
    });

    // Verify circuit breaker is healthy
    assertTrue(errorHandler.isHealthy(), "Error handler should be healthy");

    // Verify metrics
    final ProductionErrorHandler.MetricsCollector metrics = errorHandler.getMetrics();
    assertNotNull(metrics);
    assertTrue(metrics.getTotalOperations() > 0);

    LOGGER.info(String.format("Error handling test: %d successes, %d failures, %s",
        successCount.get(), failureCount.get(), metrics));
  }

  @Test
  @Order(4)
  @DisplayName("Resource Management and Cleanup")
  void testResourceManagementAndCleanup() throws Exception {
    final String testGroup = "integration-test";
    final int resourceCount = 100;

    // Register multiple resources
    for (int i = 0; i < resourceCount; i++) {
      final Engine engine = WasmRuntimeFactory.createEngine(productionConfig.getEngine());
      final ProductionResourceManager.ManagedResource<Engine> managedEngine =
          resourceManager.registerResource(testGroup, engine);

      assertNotNull(managedEngine);
      assertFalse(managedEngine.isClosed());
    }

    // Verify resources are tracked
    final ProductionResourceManager.ResourceStatistics stats = resourceManager.getStatistics();
    assertTrue(stats.getTotalResources() >= resourceCount);

    // Perform cleanup
    final int cleanedUp = resourceManager.cleanupResourceGroup(testGroup);
    assertEquals(resourceCount, cleanedUp);

    // Verify cleanup completed
    assertFalse(resourceManager.getResourceGroups().contains(testGroup));

    LOGGER.info(String.format("Resource management test: %d resources created and cleaned up", cleanedUp));
  }

  @Test
  @Order(5)
  @DisplayName("Memory Optimization and Pressure Handling")
  void testMemoryOptimizationAndPressureHandling() {
    final AtomicBoolean pressureHandlerCalled = new AtomicBoolean(false);

    // Register custom pressure handler
    memoryOptimizer.registerPressureHandler("test-handler", event -> {
      pressureHandlerCalled.set(true);
      LOGGER.info(String.format("Memory pressure detected: %.1f%% (%s)",
          event.getUsagePercentage(), event.getPressureLevel()));
    });

    // Check current memory status
    final MemoryOptimizationService.MemoryUsageEvent currentUsage = memoryOptimizer.getCurrentMemoryUsage();
    assertNotNull(currentUsage);
    assertTrue(currentUsage.getUsagePercentage() >= 0);
    assertTrue(currentUsage.getUsagePercentage() <= 100);

    // Force memory pressure check
    final MemoryOptimizationService.MemoryPressureLevel pressureLevel = memoryOptimizer.checkMemoryPressure();
    assertNotNull(pressureLevel);

    // Simulate memory stress and check handling
    final Engine engine = WasmRuntimeFactory.createEngine(productionConfig.getEngine());
    memoryOptimizer.optimizeMemoryUsage(engine);

    // Verify emergency cleanup works
    assertTrue(memoryOptimizer.performEmergencyCleanup());

    // Get final statistics
    final MemoryOptimizationService.MemoryStatistics memStats = memoryOptimizer.getStatistics();
    assertNotNull(memStats);
    assertTrue(memStats.getTotalMemoryEvents() > 0);

    engine.close();

    LOGGER.info(String.format("Memory optimization test: %s, pressure handler called: %s",
        memStats, pressureHandlerCalled.get()));
  }

  @Test
  @Order(6)
  @DisplayName("Monitoring and Observability")
  void testMonitoringAndObservability() throws Exception {
    final AtomicBoolean alertReceived = new AtomicBoolean(false);

    // Add alert listener
    monitoringService.addAlertListener(alert -> {
      alertReceived.set(true);
      LOGGER.info(String.format("Alert received: %s", alert));
    });

    // Create and configure engine
    final Engine engine = WasmRuntimeFactory.createEngine(productionConfig.getEngine());
    monitoringService.configureEngineMonitoring(engine);

    // Perform monitored operations
    final Module module = engine.compileModule(testWasmModule);
    monitoringService.recordModuleCompilation(module, Duration.ofMillis(50));

    final Store store = new Store(engine);
    final WasmInstance instance = new WasmInstance(store, module);
    final WasmFunction function = instance.getFunction("add");

    final long startTime = System.nanoTime();
    final Object result = function.call(new Object[]{1, 2});
    final Duration callDuration = Duration.ofNanos(System.nanoTime() - startTime);

    monitoringService.recordFunctionCall(function, new Object[]{1, 2}, result, callDuration);

    // Record an error for testing
    monitoringService.recordError("test-operation", new RuntimeException("Test error"), Duration.ofMillis(10));

    // Get monitoring statistics
    final ProductionMonitoringService.MonitoringStatistics stats = monitoringService.getStatistics();
    assertNotNull(stats);
    assertTrue(stats.isHealthy());

    // Verify metrics registry
    final ProductionMonitoringService.MetricsRegistry registry = monitoringService.getMetricsRegistry();
    assertNotNull(registry);
    assertFalse(registry.getCounters().isEmpty());

    // Cleanup
    instance.close();
    store.close();
    module.close();
    engine.close();

    LOGGER.info(String.format("Monitoring test: %s, alert received: %s", stats, alertReceived.get()));
  }

  @Test
  @Order(7)
  @DisplayName("Concurrent Load Testing")
  void testConcurrentLoadTesting() throws Exception {
    final int threadCount = 20;
    final int operationsPerThread = 50;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch completionLatch = new CountDownLatch(threadCount);
    final AtomicInteger totalOperations = new AtomicInteger(0);
    final AtomicInteger totalErrors = new AtomicInteger(0);

    final long testStartTime = System.currentTimeMillis();

    // Create worker threads
    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      executor.submit(() -> {
        try {
          startLatch.await(); // Wait for coordinated start

          for (int i = 0; i < operationsPerThread; i++) {
            try {
              final Object result = errorHandler.handleWithRecovery(() -> {
                final Engine engine = enginePool.acquireEngine();
                try {
                  final Module module = engine.compileModule(testWasmModule);
                  final Store store = new Store(engine);
                  final WasmInstance instance = new WasmInstance(store, module);
                  final WasmFunction function = instance.getFunction("add");
                  final Object funcResult = function.call(new Object[]{threadId, i});

                  instance.close();
                  store.close();
                  module.close();
                  return funcResult;
                } finally {
                  enginePool.returnEngine(engine);
                }
              }, "concurrent-load-test");

              assertEquals(threadId + i, result);
              totalOperations.incrementAndGet();

            } catch (final Exception e) {
              totalErrors.incrementAndGet();
              LOGGER.warning(String.format("Thread %d operation %d failed: %s", threadId, i, e.getMessage()));
            }
          }
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          completionLatch.countDown();
        }
      });
    }

    // Start all threads simultaneously
    startLatch.countDown();

    // Wait for completion with timeout
    assertTrue(completionLatch.await(120, TimeUnit.SECONDS), "Concurrent load test timed out");

    final long testDuration = System.currentTimeMillis() - testStartTime;

    executor.shutdown();
    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    // Verify results
    final int expectedOperations = threadCount * operationsPerThread;
    final double successRate = (double) totalOperations.get() / expectedOperations * 100;

    assertTrue(successRate > 95.0, String.format("Success rate should be above 95%%, got %.1f%%", successRate));
    assertTrue(testDuration < 60000, "Should complete within 60 seconds");

    // Get final statistics
    final OptimizedEnginePool.PoolStatistics poolStats = enginePool.getStatistics();
    final MemoryOptimizationService.MemoryStatistics memStats = memoryOptimizer.getStatistics();

    LOGGER.info(String.format("Concurrent load test: %d/%d operations succeeded (%.1f%%) in %dms",
        totalOperations.get(), expectedOperations, successRate, testDuration));
    LOGGER.info(String.format("Final pool stats: %s", poolStats));
    LOGGER.info(String.format("Final memory stats: %s", memStats));
  }

  @Test
  @Order(8)
  @DisplayName("Security and Sandboxing Validation")
  void testSecurityAndSandboxingValidation() throws Exception {
    // Verify security configuration
    assertTrue(productionConfig.getSecurity().isEnableSandboxing());
    assertEquals(ProductionWasmtimeConfig.SecurityLevel.MAXIMUM, productionConfig.getSecurity().getSecurityLevel());

    // Test resource limits
    final Engine engine = WasmRuntimeFactory.createEngine(productionConfig.getEngine());
    final Store store = new Store(engine);

    // Test memory limits (if supported)
    assertDoesNotThrow(() -> {
      final Module module = engine.compileModule(testWasmModule);
      final WasmInstance instance = new WasmInstance(store, module);

      // Verify instance can be created with security constraints
      assertNotNull(instance);

      instance.close();
      module.close();
    });

    // Test execution time limits
    final long maxExecutionTime = productionConfig.getSecurity().getMaxExecutionTime().toMillis();
    assertTrue(maxExecutionTime > 0 && maxExecutionTime <= 60000, "Execution time should be reasonable");

    store.close();
    engine.close();

    LOGGER.info("Security and sandboxing validation completed");
  }

  @Test
  @Order(9)
  @DisplayName("Production Readiness Assessment")
  void testProductionReadinessAssessment() {
    // Create and run production readiness assessment
    final ai.tegmentum.wasmtime4j.epic.ProductionReadinessAssessment assessment =
        new ai.tegmentum.wasmtime4j.epic.ProductionReadinessAssessment();

    final ai.tegmentum.wasmtime4j.epic.ProductionReadinessAssessment.ReadinessReport report =
        assessment.assessReadiness();

    assertNotNull(report);

    // Log assessment results
    LOGGER.info(String.format("Production readiness assessment: %s", report.getSummary()));

    if (!report.isProductionReady()) {
      LOGGER.warning("Production readiness issues found:");
      for (final ai.tegmentum.wasmtime4j.epic.ProductionReadinessAssessment.ProductionIssue issue : report.getCriticalIssues()) {
        LOGGER.warning(String.format("CRITICAL: %s", issue));
      }
    }

    // Verify core production capabilities
    assertTrue(report.getPerformanceAssessment().meetsPerformanceTargets() ||
               report.getPerformanceAssessment().getAvgOperationTime() < 10.0,
               "Performance should meet targets or be under 10ms");

    assertTrue(report.getStabilityAssessment().getStabilityScore() >= 75.0,
               "Stability score should be at least 75%");

    LOGGER.info("Production readiness assessment completed");
  }

  /** Creates a simple test WASM module for integration testing. */
  private static byte[] createTestWasmModule() {
    // Simple add function WASM module
    return new byte[] {
        0x00, 0x61, 0x73, 0x6d, // magic
        0x01, 0x00, 0x00, 0x00, // version
        0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f, // type section: (i32, i32) -> i32
        0x03, 0x02, 0x01, 0x00, // function section
        0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00, // export section: "add"
        0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b // code section: add two parameters
    };
  }
}