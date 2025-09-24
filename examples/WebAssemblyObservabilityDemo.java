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

import ai.tegmentum.wasmtime4j.observability.ObservabilityConfiguration;
import ai.tegmentum.wasmtime4j.observability.ObservabilityHealthCheck;
import ai.tegmentum.wasmtime4j.observability.ObservabilityIntegration;
import ai.tegmentum.wasmtime4j.observability.OpenTelemetryObservabilityManager;
import ai.tegmentum.wasmtime4j.observability.WasmObservability;
import ai.tegmentum.wasmtime4j.observability.WasmRuntimeLogger;
import ai.tegmentum.wasmtime4j.observability.WasmRuntimeMetrics;
import ai.tegmentum.wasmtime4j.observability.WasmRuntimeTracer;
import io.opentelemetry.api.common.Attributes;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Comprehensive demonstration of WebAssembly observability features in wasmtime4j.
 *
 * <p>This example shows how to:
 * <ul>
 *   <li>Configure OpenTelemetry observability for different environments
 *   <li>Use distributed tracing for WebAssembly operations
 *   <li>Collect comprehensive metrics for performance monitoring
 *   <li>Implement structured logging with correlation
 *   <li>Set up health checks and monitoring
 *   <li>Export observability data to various backends
 * </ul>
 */
public final class WebAssemblyObservabilityDemo {

  private static final Logger LOGGER = Logger.getLogger(WebAssemblyObservabilityDemo.class.getName());

  public static void main(final String[] args) {
    System.out.println("=== WebAssembly Observability Demo ===\n");

    try {
      // 1. Demonstrate different configuration modes
      demonstrateConfigurationModes();

      // 2. Demonstrate basic observability features
      demonstrateBasicObservability();

      // 3. Demonstrate advanced tracing
      demonstrateAdvancedTracing();

      // 4. Demonstrate metrics collection
      demonstrateMetricsCollection();

      // 5. Demonstrate structured logging
      demonstrateStructuredLogging();

      // 6. Demonstrate health checks
      demonstrateHealthChecks();

      // 7. Demonstrate production scenario
      demonstrateProductionScenario();

      // 8. Demonstrate integration utilities
      demonstrateIntegrationUtilities();

    } catch (final Exception e) {
      LOGGER.severe("Demo failed: " + e.getMessage());
      e.printStackTrace();
    } finally {
      // Cleanup
      WasmObservability.getInstance().shutdown();
    }

    System.out.println("\n=== Demo Complete ===");
  }

  private static void demonstrateConfigurationModes() {
    System.out.println("1. Configuration Modes Demonstration\n");

    // Development configuration with logging exporters
    System.out.println("   Development Configuration:");
    final ObservabilityConfiguration devConfig = ObservabilityConfiguration.builder()
        .setResourceDetectionEnabled(true)
        .setUseBatchProcessors(false) // Use simple processors for immediate feedback
        .addLoggingExporters()
        .build();

    WasmObservability.initialize(devConfig);
    System.out.println("   ✓ Development mode initialized with logging exporters");
    WasmObservability.getInstance().shutdown();

    // Production configuration with OTLP
    System.out.println("   Production Configuration:");
    final String otlpEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
    if (otlpEndpoint != null) {
      final ObservabilityConfiguration prodConfig = ObservabilityConfiguration
          .createProduction(otlpEndpoint);

      WasmObservability.initialize(prodConfig);
      System.out.println("   ✓ Production mode initialized with OTLP endpoint: " + otlpEndpoint);
      WasmObservability.getInstance().shutdown();
    } else {
      System.out.println("   ⚠ OTEL_EXPORTER_OTLP_ENDPOINT not set, skipping production config");
    }

    // Auto-configuration mode
    System.out.println("   Auto-Configuration Mode:");
    final ObservabilityConfiguration autoConfig = ObservabilityConfiguration.createAutoConfigured();
    WasmObservability.initialize(autoConfig);
    System.out.println("   ✓ Auto-configuration mode initialized");

    System.out.println();
  }

  private static void demonstrateBasicObservability() {
    System.out.println("2. Basic Observability Features\n");

    final WasmObservability observability = WasmObservability.getInstance();

    // Simple operation observation
    final String result = observability.observeOperation("demo_operation", () -> {
      simulateWork(Duration.ofMillis(50));
      return "operation_completed";
    });

    System.out.println("   ✓ Observed simple operation: " + result);

    // Engine operation observation
    final String engineResult = observability.observeEngineOperation("demo", "initialize", () -> {
      simulateWork(Duration.ofMillis(100));
      return "engine_initialized";
    });

    System.out.println("   ✓ Observed engine operation: " + engineResult);

    // Async operation observation
    final CompletableFuture<String> asyncResult = observability.observeAsyncOperation("async_demo", () -> {
      return CompletableFuture.supplyAsync(() -> {
        simulateWork(Duration.ofMillis(200));
        return "async_completed";
      });
    });

    try {
      System.out.println("   ✓ Observed async operation: " + asyncResult.get());
    } catch (final Exception e) {
      System.out.println("   ✗ Async operation failed: " + e.getMessage());
    }

    System.out.println();
  }

  private static void demonstrateAdvancedTracing() {
    System.out.println("3. Advanced Distributed Tracing\n");

    final WasmRuntimeTracer tracer = WasmObservability.getInstance().getTracer();

    // Nested spans with different operation types
    try (final WasmRuntimeTracer.TracedOperation moduleOp =
             tracer.startModuleOperation("compile", "demo_module", 4096L)) {

      moduleOp.setAttribute(WasmRuntimeTracer.WASM_OPTIMIZATION_LEVEL, "speed")
              .setAttribute(WasmRuntimeTracer.WASM_COMPILER_BACKEND, "cranelift")
              .recordEvent("compilation_started");

      simulateWork(Duration.ofMillis(150));

      try (final WasmRuntimeTracer.TracedOperation instanceOp =
               tracer.startInstanceOperation("create", "demo_instance", "demo_module")) {

        instanceOp.recordEvent("instance_creation_started");
        simulateWork(Duration.ofMillis(75));

        try (final WasmRuntimeTracer.TracedOperation functionOp =
                 tracer.startFunctionOperation("calculate", "(i32, i32) -> i32", false)) {

          functionOp.recordEvent("function_call_started");
          simulateWork(Duration.ofMillis(25));
          functionOp.recordEvent("function_call_completed");
        }

        instanceOp.recordEvent("instance_creation_completed");
      }

      moduleOp.recordEvent("compilation_completed");
      moduleOp.setOk();
    }

    System.out.println("   ✓ Created nested trace spans for module -> instance -> function");

    // WASI operation tracing
    try (final WasmRuntimeTracer.TracedOperation wasiOp =
             tracer.startWasiOperation("file_read", "/tmp/demo.txt", null)) {

      wasiOp.recordEvent("file_read_started");
      simulateWork(Duration.ofMillis(30));
      wasiOp.recordEvent("file_read_completed");
    }

    System.out.println("   ✓ Traced WASI file operation");

    // Error handling in tracing
    try (final WasmRuntimeTracer.TracedOperation errorOp =
             tracer.startEngineOperation("failing_operation", "demo")) {

      try {
        simulateWork(Duration.ofMillis(20));
        throw new RuntimeException("Simulated error for demo");
      } catch (final RuntimeException e) {
        errorOp.recordException(e);
        System.out.println("   ✓ Recorded exception in trace span");
      }
    }

    System.out.println();
  }

  private static void demonstrateMetricsCollection() {
    System.out.println("4. Comprehensive Metrics Collection\n");

    final WasmRuntimeMetrics metrics = WasmObservability.getInstance().getMetrics();

    // Engine metrics
    metrics.recordEngineCreated("demo_engine");
    System.out.println("   ✓ Recorded engine creation metric");

    // Module metrics
    metrics.recordModuleCompiled("demo_module", 4096L, Duration.ofMillis(150));
    metrics.recordModuleValidated("demo_module");
    System.out.println("   ✓ Recorded module compilation and validation metrics");

    // Instance metrics
    metrics.recordInstanceCreated("demo_instance", "demo_module", Duration.ofMillis(75));
    System.out.println("   ✓ Recorded instance creation metric");

    // Function call metrics
    metrics.recordFunctionCall("calculate", "(i32, i32) -> i32", Duration.ofMicroseconds(500));
    metrics.recordHostFunctionCall("log", "(i32)", Duration.ofMicroseconds(200));
    System.out.println("   ✓ Recorded function call metrics");

    // Memory metrics
    metrics.recordMemoryAllocation(65536L); // 1 page
    metrics.recordMemoryPages(1L);
    metrics.recordMemoryGrow(1L, 2L, Duration.ofMicroseconds(100));
    System.out.println("   ✓ Recorded memory operation metrics");

    // WASI metrics
    metrics.recordWasiOperation("file_read", Duration.ofMillis(30));
    metrics.recordWasiFileOperation("read", "/tmp/demo.txt");
    System.out.println("   ✓ Recorded WASI operation metrics");

    // Performance metrics
    metrics.recordCompilationCacheHitRate(0.85);
    metrics.recordGcTriggered("mark_sweep", Duration.ofMillis(5));
    System.out.println("   ✓ Recorded performance and GC metrics");

    // Error metrics
    metrics.recordTrapOccurred("division_by_zero", "calculate");
    System.out.println("   ✓ Recorded error metrics");

    System.out.println();
  }

  private static void demonstrateStructuredLogging() {
    System.out.println("5. Structured Logging with Correlation\n");

    final WasmRuntimeLogger logger = WasmObservability.getInstance().getLogger();

    // Engine logging
    logger.logEngineEvent("create", "demo_engine", "Demo engine created successfully");
    System.out.println("   ✓ Logged engine event");

    // Module logging with attributes
    logger.logModuleEvent("compile", "demo_module", 4096L, 150L, "Module compiled successfully");
    System.out.println("   ✓ Logged module event with attributes");

    // Instance logging
    logger.logInstanceEvent("create", "demo_instance", "demo_module", "Instance created");
    System.out.println("   ✓ Logged instance event");

    // Function call logging
    logger.logFunctionCall("calculate", "(i32, i32) -> i32", false, 500L, "Function called");
    System.out.println("   ✓ Logged function call");

    // Memory operation logging
    logger.logMemoryEvent("grow", 2L, 65536L, "Memory grown to 2 pages");
    System.out.println("   ✓ Logged memory operation");

    // WASI operation logging
    logger.logWasiEvent("file_read", "/tmp/demo.txt", 30000L, "File read completed");
    System.out.println("   ✓ Logged WASI operation");

    // Security event logging
    logger.logSecurityEvent("file_access", "file", true, "File access granted");
    System.out.println("   ✓ Logged security event");

    // Performance event logging
    logger.logPerformanceEvent("measurement", "execution_time", 125.5, 100.0,
                              "Execution time exceeded threshold");
    System.out.println("   ✓ Logged performance event");

    // Error logging with context
    final Exception demoException = new RuntimeException("Demo error");
    logger.logError(WasmRuntimeLogger.LogComponent.ENGINE, "operation", "RuntimeError",
                   "Demo error occurred", demoException,
                   Attributes.builder().put("context", "demo").build());
    System.out.println("   ✓ Logged error with context");

    System.out.println();
  }

  private static void demonstrateHealthChecks() {
    System.out.println("6. Observability Health Monitoring\n");

    final OpenTelemetryObservabilityManager manager = WasmObservability.getInstance().getManager();
    final ObservabilityHealthCheck healthCheck = new ObservabilityHealthCheck(
        manager, manager.getMeterProvider().get("health_check"));

    // Perform health check
    final ObservabilityHealthCheck.HealthCheckResult result = healthCheck.performHealthCheck();
    System.out.println("   Health Check Result:");
    System.out.println("     Status: " + result.getStatus());
    System.out.println("     Message: " + result.getMessage());
    System.out.println("     Duration: " + result.getCheckDuration().toMillis() + "ms");
    System.out.println("     Details: " + result.getDetails().replaceAll("\n", "\n     "));

    // Get health statistics
    System.out.println("   " + healthCheck.getHealthStatistics());

    // Get observability status
    System.out.println("   Observability Status:");
    final String status = WasmObservability.getInstance().getObservabilityStatus();
    System.out.println(status.replaceAll("\n", "\n   "));

    healthCheck.shutdown();
    System.out.println();
  }

  private static void demonstrateProductionScenario() {
    System.out.println("7. Production Scenario Simulation\n");

    final WasmObservability observability = WasmObservability.getInstance();

    // Simulate a production workload with multiple concurrent operations
    System.out.println("   Simulating production workload...");

    final CompletableFuture<?>[] futures = new CompletableFuture[10];
    for (int i = 0; i < 10; i++) {
      final int workerId = i;
      futures[i] = observability.observeAsyncOperation("worker_" + workerId, () -> {
        return CompletableFuture.supplyAsync(() -> {
          // Simulate multiple operations per worker
          for (int j = 0; j < 5; j++) {
            final String operationName = "operation_" + workerId + "_" + j;
            observability.observeOperation(operationName, () -> {
              simulateWork(Duration.ofMillis(ThreadLocalRandom.current().nextInt(10, 100)));
              return "completed";
            });
          }
          return "worker_" + workerId + "_completed";
        });
      });
    }

    // Wait for all workers to complete
    CompletableFuture.allOf(futures).join();
    System.out.println("   ✓ Completed production workload simulation with 50 operations");

    // Simulate some errors
    for (int i = 0; i < 3; i++) {
      try {
        observability.observeOperation("error_simulation_" + i, () -> {
          if (ThreadLocalRandom.current().nextBoolean()) {
            throw new RuntimeException("Simulated production error " + i);
          }
          return "success";
        });
      } catch (final RuntimeException e) {
        System.out.println("   ⚠ Simulated error: " + e.getMessage());
      }
    }

    System.out.println("   ✓ Production scenario simulation completed");
    System.out.println();
  }

  private static void demonstrateIntegrationUtilities() {
    System.out.println("8. Integration Utilities for Runtime Implementations\n");

    // Demonstrate how JNI/Panama implementations would integrate observability

    // Engine operations
    final String engineResult = ObservabilityIntegration.instrumentEngineOperation(
        "jni", "create", () -> {
          simulateWork(Duration.ofMillis(50));
          return "engine_created";
        });
    System.out.println("   ✓ Instrumented engine operation: " + engineResult);

    // Record lifecycle events
    ObservabilityIntegration.recordEngineEvent("jni", "create", "JNI engine created");
    ObservabilityIntegration.recordStoreEvent("store_1", "create", "Store created");
    ObservabilityIntegration.recordInstanceEvent("instance_1", "demo_module", "create",
                                                 Duration.ofMillis(75));

    // Memory operations
    ObservabilityIntegration.recordMemoryOperation("grow", 2L, 65536L, Duration.ofMicroseconds(100));

    // WASI operations
    ObservabilityIntegration.recordWasiOperation("file_read", "/tmp/test.txt",
                                                Duration.ofMillis(25), true);

    // Error recording
    ObservabilityIntegration.recordError("module", "compile", "ValidationError",
                                        "Invalid module format", null);

    // Performance metrics
    ObservabilityIntegration.recordPerformanceMetric("compilation_speed", 1250.0, 1000.0);

    // Manual span creation
    try (final WasmRuntimeTracer.TracedOperation span = ObservabilityIntegration.startSpan(
             "custom_operation",
             Attributes.builder().put("operation.type", "manual").build())) {

      span.recordEvent("custom_event");
      simulateWork(Duration.ofMillis(30));
      span.recordEvent("operation_completed");
    }

    System.out.println("   ✓ Demonstrated integration utilities for runtime implementations");
    System.out.println();
  }

  /** Simulates work by sleeping for the specified duration. */
  private static void simulateWork(final Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}