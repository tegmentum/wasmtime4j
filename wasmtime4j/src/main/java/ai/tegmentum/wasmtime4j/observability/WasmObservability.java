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

package ai.tegmentum.wasmtime4j.observability;

import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Central observability facade for WebAssembly runtime operations.
 *
 * <p>This class provides a unified interface for all observability concerns in wasmtime4j,
 * integrating tracing, metrics, and structured logging with OpenTelemetry.
 *
 * <ul>
 *   <li>Unified observability API for all runtime components
 *   <li>Automatic instrumentation of WebAssembly operations
 *   <li>Context propagation and correlation across operations
 *   <li>Performance monitoring with minimal overhead
 *   <li>Production-ready observability features
 *   <li>Integration with existing monitoring infrastructure
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasmObservability {

  private static final Logger LOGGER = Logger.getLogger(WasmObservability.class.getName());

  /** Singleton instance. */
  private static volatile WasmObservability instance;

  /** Initialization lock. */
  private static final Object LOCK = new Object();

  /** OpenTelemetry observability manager. */
  private final OpenTelemetryObservabilityManager manager;

  /** Runtime tracer for distributed tracing. */
  private volatile WasmRuntimeTracer tracer;

  /** Runtime metrics collector. */
  private volatile WasmRuntimeMetrics metrics;

  /** Structured logger. */
  private volatile WasmRuntimeLogger logger;

  /** Initialization status. */
  private final AtomicBoolean initialized = new AtomicBoolean(false);

  /**
   * Private constructor for singleton pattern.
   */
  private WasmObservability() {
    this.manager = OpenTelemetryObservabilityManager.getInstance();
  }

  /**
   * Gets the singleton instance of the observability facade.
   *
   * @return observability instance
   */
  public static WasmObservability getInstance() {
    if (instance == null) {
      synchronized (LOCK) {
        if (instance == null) {
          instance = new WasmObservability();
          instance.ensureInitialized();
        }
      }
    }
    return instance;
  }

  /**
   * Initializes observability with the specified configuration.
   *
   * @param config observability configuration
   * @return this instance for method chaining
   */
  public static WasmObservability initialize(final ObservabilityConfiguration config) {
    final WasmObservability observability = getInstance();
    observability.manager.initialize(config);
    observability.initializeComponents();
    return observability;
  }

  /**
   * Initializes observability with default configuration.
   *
   * @return observability instance
   */
  public static WasmObservability initialize() {
    return initialize(ObservabilityConfiguration.createDefault());
  }

  /** Ensures the observability system is initialized with defaults if not already done. */
  private void ensureInitialized() {
    if (!initialized.get() && !manager.isInitialized()) {
      manager.initialize(ObservabilityConfiguration.createDefault());
      initializeComponents();
    }
  }

  /** Initializes observability components. */
  private void initializeComponents() {
    if (initialized.getAndSet(true)) {
      return; // Already initialized
    }

    try {
      // Initialize tracer
      this.tracer = new WasmRuntimeTracer(
          manager.getTracerProvider()
              .get(OpenTelemetryObservabilityManager.INSTRUMENTATION_NAME,
                   OpenTelemetryObservabilityManager.INSTRUMENTATION_VERSION));

      // Initialize metrics
      this.metrics = new WasmRuntimeMetrics(
          manager.getMeterProvider()
              .get(OpenTelemetryObservabilityManager.INSTRUMENTATION_NAME,
                   OpenTelemetryObservabilityManager.INSTRUMENTATION_VERSION));

      // Initialize logger
      this.logger = new WasmRuntimeLogger(
          manager.getLoggerProvider()
              .get(OpenTelemetryObservabilityManager.INSTRUMENTATION_NAME,
                   OpenTelemetryObservabilityManager.INSTRUMENTATION_VERSION),
          WasmObservability.class.getName());

      LOGGER.info("WebAssembly observability components initialized successfully");

    } catch (final Exception e) {
      initialized.set(false);
      LOGGER.severe("Failed to initialize observability components: " + e.getMessage());
      throw new RuntimeException("Observability initialization failed", e);
    }
  }

  /**
   * Gets the runtime tracer for distributed tracing.
   *
   * @return runtime tracer
   */
  public WasmRuntimeTracer getTracer() {
    ensureInitialized();
    return tracer;
  }

  /**
   * Gets the runtime metrics collector.
   *
   * @return runtime metrics
   */
  public WasmRuntimeMetrics getMetrics() {
    ensureInitialized();
    return metrics;
  }

  /**
   * Gets the structured logger.
   *
   * @return runtime logger
   */
  public WasmRuntimeLogger getLogger() {
    ensureInitialized();
    return logger;
  }

  /**
   * Gets the underlying OpenTelemetry observability manager.
   *
   * @return observability manager
   */
  public OpenTelemetryObservabilityManager getManager() {
    return manager;
  }

  /**
   * Traces and measures a WebAssembly operation.
   *
   * @param <T> return type
   * @param operationName operation name
   * @param supplier operation to execute
   * @return operation result
   */
  public <T> T observeOperation(final String operationName, final Supplier<T> supplier) {
    ensureInitialized();

    final String timingId = "operation_" + System.nanoTime();
    metrics.startOperation(timingId);

    try (final WasmRuntimeTracer.TracedOperation tracedOp =
             tracer.startEngineOperation(operationName, "unknown")) {

      logger.debug(WasmRuntimeLogger.LogComponent.ENGINE, operationName,
                  "Starting WebAssembly operation", null);

      final T result = supplier.get();

      final Duration duration = metrics.endOperation(timingId);
      tracedOp.setAttribute(WasmRuntimeTracer.WASM_RUNTIME, "wasmtime")
              .recordEvent("operation_completed");

      logger.debug(WasmRuntimeLogger.LogComponent.ENGINE, operationName,
                  "Completed WebAssembly operation in " + duration.toMillis() + "ms", null);

      return result;

    } catch (final Exception e) {
      final Duration duration = metrics.endOperation(timingId);
      logger.error(WasmRuntimeLogger.LogComponent.ENGINE, operationName,
                  "WebAssembly operation failed after " + duration.toMillis() + "ms", e, null);
      throw e;
    }
  }

  /**
   * Observes an asynchronous WebAssembly operation.
   *
   * @param <T> return type
   * @param operationName operation name
   * @param supplier async operation to execute
   * @return completable future with result
   */
  public <T> CompletableFuture<T> observeAsyncOperation(final String operationName,
                                                       final Supplier<CompletableFuture<T>> supplier) {
    ensureInitialized();

    final String timingId = "async_operation_" + System.nanoTime();
    metrics.startOperation(timingId);

    try (final WasmRuntimeTracer.TracedOperation tracedOp =
             tracer.startEngineOperation(operationName, "unknown")) {

      logger.debug(WasmRuntimeLogger.LogComponent.ENGINE, operationName,
                  "Starting async WebAssembly operation", null);

      return supplier.get()
          .whenComplete((result, throwable) -> {
            final Duration duration = metrics.endOperation(timingId);

            if (throwable != null) {
              tracedOp.recordException(throwable);
              logger.error(WasmRuntimeLogger.LogComponent.ENGINE, operationName,
                          "Async WebAssembly operation failed after " + duration.toMillis() + "ms",
                          throwable, null);
            } else {
              tracedOp.recordEvent("async_operation_completed");
              logger.debug(WasmRuntimeLogger.LogComponent.ENGINE, operationName,
                          "Completed async WebAssembly operation in " + duration.toMillis() + "ms", null);
            }
          });

    } catch (final Exception e) {
      final Duration duration = metrics.endOperation(timingId);
      logger.error(WasmRuntimeLogger.LogComponent.ENGINE, operationName,
                  "Async WebAssembly operation failed to start after " + duration.toMillis() + "ms", e, null);
      throw e;
    }
  }

  /**
   * Observes engine operations with automatic context setup.
   *
   * @param <T> return type
   * @param engineType engine type (JNI, Panama)
   * @param operationName operation name
   * @param supplier operation to execute
   * @return operation result
   */
  public <T> T observeEngineOperation(final String engineType,
                                     final String operationName,
                                     final Supplier<T> supplier) {
    ensureInitialized();

    final String timingId = "engine_" + System.nanoTime();
    metrics.startOperation(timingId);

    try (final WasmRuntimeTracer.TracedOperation tracedOp =
             tracer.startEngineOperation(operationName, engineType)) {

      logger.logEngineEvent(operationName, engineType,
          "Starting " + operationName + " on " + engineType + " engine");

      final T result = supplier.get();

      final Duration duration = metrics.endOperation(timingId);
      metrics.recordEngineCreated(engineType); // Record appropriate metrics based on operation

      tracedOp.setAttribute(WasmRuntimeTracer.WASM_ENGINE_TYPE, engineType)
              .recordEvent("engine_operation_completed");

      logger.logEngineEvent(operationName, engineType,
          "Completed " + operationName + " in " + duration.toMillis() + "ms");

      return result;

    } catch (final Exception e) {
      final Duration duration = metrics.endOperation(timingId);
      logger.logError(WasmRuntimeLogger.LogComponent.ENGINE, operationName, "EngineException",
                     "Engine operation failed: " + e.getMessage(), e, null);
      throw e;
    }
  }

  /**
   * Observes module operations with automatic context setup.
   *
   * @param <T> return type
   * @param operationName operation name (compile, validate, etc.)
   * @param moduleName module name
   * @param moduleSize module size in bytes
   * @param supplier operation to execute
   * @return operation result
   */
  public <T> T observeModuleOperation(final String operationName,
                                     final String moduleName,
                                     final Long moduleSize,
                                     final Supplier<T> supplier) {
    ensureInitialized();

    final String timingId = "module_" + System.nanoTime();
    metrics.startOperation(timingId);

    try (final WasmRuntimeTracer.TracedOperation tracedOp =
             tracer.startModuleOperation(operationName, moduleName, moduleSize)) {

      logger.logModuleEvent(operationName, moduleName,
          moduleSize != null ? moduleSize : 0L, 0L,
          "Starting module " + operationName);

      final T result = supplier.get();

      final Duration duration = metrics.endOperation(timingId);

      // Record appropriate metrics based on operation
      if ("compile".equals(operationName)) {
        metrics.recordModuleCompiled(moduleName, moduleSize != null ? moduleSize : 0L, duration);
      } else if ("validate".equals(operationName)) {
        metrics.recordModuleValidated(moduleName);
      }

      tracedOp.setAttribute(WasmRuntimeTracer.WASM_MODULE_NAME, moduleName)
              .recordEvent("module_operation_completed");

      logger.logModuleEvent(operationName, moduleName,
          moduleSize != null ? moduleSize : 0L, duration.toMillis(),
          "Completed module " + operationName);

      return result;

    } catch (final Exception e) {
      final Duration duration = metrics.endOperation(timingId);

      // Record error metrics
      if ("compile".equals(operationName)) {
        metrics.recordModuleCompilationError(moduleName, e.getClass().getSimpleName());
      }

      logger.logError(WasmRuntimeLogger.LogComponent.MODULE, operationName, "ModuleException",
                     "Module operation failed: " + e.getMessage(), e, null);
      throw e;
    }
  }

  /**
   * Observes function call operations with automatic context setup.
   *
   * @param <T> return type
   * @param functionName function name
   * @param functionType function signature
   * @param isHostFunction whether this is a host function
   * @param supplier operation to execute
   * @return operation result
   */
  public <T> T observeFunctionCall(final String functionName,
                                  final String functionType,
                                  final boolean isHostFunction,
                                  final Supplier<T> supplier) {
    ensureInitialized();

    final String timingId = "function_" + System.nanoTime();
    metrics.startOperation(timingId);

    try (final WasmRuntimeTracer.TracedOperation tracedOp =
             tracer.startFunctionOperation(functionName, functionType, isHostFunction)) {

      final T result = supplier.get();

      final Duration duration = metrics.endOperation(timingId);

      // Record metrics
      if (isHostFunction) {
        metrics.recordHostFunctionCall(functionName, functionType, duration);
      } else {
        metrics.recordFunctionCall(functionName, functionType, duration);
      }

      tracedOp.recordEvent("function_call_completed");

      logger.logFunctionCall(functionName, functionType, isHostFunction,
                            duration.toNanos() / 1000, // Convert to microseconds
                            "Function call completed");

      return result;

    } catch (final Exception e) {
      final Duration duration = metrics.endOperation(timingId);

      // Record error metrics
      metrics.recordFunctionCallError(functionName, functionType, e.getClass().getSimpleName());

      logger.logError(WasmRuntimeLogger.LogComponent.FUNCTION, "call", "FunctionException",
                     "Function call failed: " + e.getMessage(), e, null);
      throw e;
    }
  }

  /**
   * Gets observability statistics and health information.
   *
   * @return observability status report
   */
  public String getObservabilityStatus() {
    final StringBuilder sb = new StringBuilder("=== WebAssembly Observability Status ===\n");

    sb.append("Initialization Status:\n");
    sb.append("  - Manager Initialized: ").append(manager.isInitialized()).append("\n");
    sb.append("  - Components Initialized: ").append(initialized.get()).append("\n");
    sb.append("  - Configuration Mode: ")
      .append(manager.getConfiguration().isAutoConfigurationEnabled() ? "Auto" : "Manual").append("\n");

    sb.append("\nProviders Status:\n");
    sb.append("  - Tracer Provider: ").append(manager.getTracerProvider().getClass().getSimpleName()).append("\n");
    sb.append("  - Meter Provider: ").append(manager.getMeterProvider().getClass().getSimpleName()).append("\n");
    sb.append("  - Logger Provider: ").append(manager.getLoggerProvider().getClass().getSimpleName()).append("\n");

    sb.append("\nExporters:\n");
    sb.append("  - Span Exporters: ").append(manager.getConfiguration().getSpanExporters().keySet()).append("\n");
    sb.append("  - Metric Exporters: ").append(manager.getConfiguration().getMetricExporters().keySet()).append("\n");
    sb.append("  - Log Exporters: ").append(manager.getConfiguration().getLogExporters().keySet()).append("\n");

    return sb.toString();
  }

  /**
   * Checks if observability is properly initialized and healthy.
   *
   * @return true if observability is healthy
   */
  public boolean isHealthy() {
    return manager.isInitialized() && initialized.get() && !manager.isShutdown()
        && tracer != null && metrics != null && logger != null;
  }

  /**
   * Shuts down the observability system.
   */
  public void shutdown() {
    if (initialized.getAndSet(false)) {
      manager.shutdown();
      LOGGER.info("WebAssembly observability system shutdown completed");
    }
  }
}