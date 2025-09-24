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

import io.opentelemetry.api.common.Attributes;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Integration utilities for adding observability to WebAssembly runtime implementations.
 *
 * <p>This class provides utility methods that can be easily integrated into JNI and Panama
 * implementations to add comprehensive observability without code duplication.
 *
 * @since 1.0.0
 */
public final class ObservabilityIntegration {

  private static final Logger LOGGER = Logger.getLogger(ObservabilityIntegration.class.getName());

  /** Private constructor - utility class. */
  private ObservabilityIntegration() {
    // Utility class
  }

  /**
   * Wraps engine operations with observability.
   *
   * @param <T> return type
   * @param engineType engine type (JNI, Panama)
   * @param operationName operation name
   * @param operation operation to execute
   * @return operation result
   */
  public static <T> T instrumentEngineOperation(final String engineType,
                                               final String operationName,
                                               final Supplier<T> operation) {
    try {
      final WasmObservability observability = WasmObservability.getInstance();
      return observability.observeEngineOperation(engineType, operationName, operation);
    } catch (final Exception e) {
      // Fallback to non-instrumented execution if observability fails
      LOGGER.fine("Observability not available for engine operation: " + e.getMessage());
      return operation.get();
    }
  }

  /**
   * Wraps module operations with observability.
   *
   * @param <T> return type
   * @param operationName operation name
   * @param moduleName module name (optional)
   * @param moduleSize module size (optional)
   * @param operation operation to execute
   * @return operation result
   */
  public static <T> T instrumentModuleOperation(final String operationName,
                                               final String moduleName,
                                               final Long moduleSize,
                                               final Supplier<T> operation) {
    try {
      final WasmObservability observability = WasmObservability.getInstance();
      return observability.observeModuleOperation(operationName, moduleName, moduleSize, operation);
    } catch (final Exception e) {
      LOGGER.fine("Observability not available for module operation: " + e.getMessage());
      return operation.get();
    }
  }

  /**
   * Wraps function calls with observability.
   *
   * @param <T> return type
   * @param functionName function name
   * @param functionType function signature (optional)
   * @param isHostFunction whether this is a host function
   * @param operation operation to execute
   * @return operation result
   */
  public static <T> T instrumentFunctionCall(final String functionName,
                                            final String functionType,
                                            final boolean isHostFunction,
                                            final Supplier<T> operation) {
    try {
      final WasmObservability observability = WasmObservability.getInstance();
      return observability.observeFunctionCall(functionName, functionType, isHostFunction, operation);
    } catch (final Exception e) {
      LOGGER.fine("Observability not available for function call: " + e.getMessage());
      return operation.get();
    }
  }

  /**
   * Records engine lifecycle events.
   *
   * @param engineType engine type
   * @param event lifecycle event (create, destroy, etc.)
   * @param details additional details
   */
  public static void recordEngineEvent(final String engineType,
                                      final String event,
                                      final String details) {
    try {
      final WasmObservability observability = WasmObservability.getInstance();
      final WasmRuntimeLogger logger = observability.getLogger();
      final WasmRuntimeMetrics metrics = observability.getMetrics();

      logger.logEngineEvent(event, engineType, details);

      if ("create".equals(event)) {
        metrics.recordEngineCreated(engineType);
      } else if ("destroy".equals(event)) {
        metrics.recordEngineDestroyed(engineType);
      }
    } catch (final Exception e) {
      LOGGER.fine("Could not record engine event: " + e.getMessage());
    }
  }

  /**
   * Records store lifecycle events.
   *
   * @param storeId store identifier
   * @param event lifecycle event (create, destroy, etc.)
   * @param details additional details
   */
  public static void recordStoreEvent(final String storeId,
                                     final String event,
                                     final String details) {
    try {
      final WasmObservability observability = WasmObservability.getInstance();
      final WasmRuntimeLogger logger = observability.getLogger();
      final WasmRuntimeMetrics metrics = observability.getMetrics();

      logger.logStoreEvent(event, storeId, details);

      if ("create".equals(event)) {
        metrics.recordStoreCreated(storeId);
      } else if ("destroy".equals(event)) {
        metrics.recordStoreDestroyed(storeId);
      }
    } catch (final Exception e) {
      LOGGER.fine("Could not record store event: " + e.getMessage());
    }
  }

  /**
   * Records instance lifecycle events.
   *
   * @param instanceId instance identifier
   * @param moduleName associated module name
   * @param event lifecycle event (create, destroy, etc.)
   * @param duration operation duration (optional)
   */
  public static void recordInstanceEvent(final String instanceId,
                                        final String moduleName,
                                        final String event,
                                        final Duration duration) {
    try {
      final WasmObservability observability = WasmObservability.getInstance();
      final WasmRuntimeLogger logger = observability.getLogger();
      final WasmRuntimeMetrics metrics = observability.getMetrics();

      logger.logInstanceEvent(event, instanceId, moduleName, event + " completed");

      if ("create".equals(event) && duration != null) {
        metrics.recordInstanceCreated(instanceId, moduleName, duration);
      } else if ("destroy".equals(event)) {
        metrics.recordInstanceDestroyed(instanceId, moduleName);
      }
    } catch (final Exception e) {
      LOGGER.fine("Could not record instance event: " + e.getMessage());
    }
  }

  /**
   * Records memory operations.
   *
   * @param operation memory operation (grow, read, write, etc.)
   * @param pages memory pages (optional)
   * @param bytes bytes involved (optional)
   * @param duration operation duration (optional)
   */
  public static void recordMemoryOperation(final String operation,
                                          final Long pages,
                                          final Long bytes,
                                          final Duration duration) {
    try {
      final WasmObservability observability = WasmObservability.getInstance();
      final WasmRuntimeLogger logger = observability.getLogger();
      final WasmRuntimeMetrics metrics = observability.getMetrics();

      logger.logMemoryEvent(operation,
                           pages != null ? pages : 0L,
                           bytes != null ? bytes : 0L,
                           operation + " completed");

      if ("allocate".equals(operation) && bytes != null) {
        metrics.recordMemoryAllocation(bytes);
      } else if ("deallocate".equals(operation) && bytes != null) {
        metrics.recordMemoryDeallocation(bytes);
      } else if ("grow".equals(operation) && pages != null && duration != null) {
        metrics.recordMemoryGrow(0L, pages, duration); // Simplified - would need actual old pages
      }

      if (pages != null) {
        metrics.recordMemoryPages(pages);
      }
    } catch (final Exception e) {
      LOGGER.fine("Could not record memory operation: " + e.getMessage());
    }
  }

  /**
   * Records WASI operations.
   *
   * @param operation WASI operation name
   * @param filePath file path for file operations (optional)
   * @param duration operation duration (optional)
   * @param success whether the operation succeeded
   */
  public static void recordWasiOperation(final String operation,
                                        final String filePath,
                                        final Duration duration,
                                        final boolean success) {
    try {
      final WasmObservability observability = WasmObservability.getInstance();
      final WasmRuntimeLogger logger = observability.getLogger();
      final WasmRuntimeMetrics metrics = observability.getMetrics();

      logger.logWasiEvent(operation, filePath,
                         duration != null ? duration.toNanos() / 1000 : 0L,
                         success ? "WASI operation completed" : "WASI operation failed");

      if (duration != null) {
        metrics.recordWasiOperation(operation, duration);
      }

      if (!success) {
        metrics.recordWasiError(operation, "operation_failed");
      }

      if (filePath != null) {
        metrics.recordWasiFileOperation(operation, filePath);
      }
    } catch (final Exception e) {
      LOGGER.fine("Could not record WASI operation: " + e.getMessage());
    }
  }

  /**
   * Records error events with rich context.
   *
   * @param component component where error occurred
   * @param operation operation that failed
   * @param errorType error type
   * @param errorMessage error message
   * @param throwable associated exception (optional)
   */
  public static void recordError(final String component,
                                final String operation,
                                final String errorType,
                                final String errorMessage,
                                final Throwable throwable) {
    try {
      final WasmObservability observability = WasmObservability.getInstance();
      final WasmRuntimeLogger logger = observability.getLogger();

      final WasmRuntimeLogger.LogComponent logComponent = parseComponent(component);
      logger.logError(logComponent, operation, errorType, errorMessage, throwable, null);

      // Record specific error metrics
      final WasmRuntimeMetrics metrics = observability.getMetrics();
      if ("module".equals(component) && "compile".equals(operation)) {
        metrics.recordModuleCompilationError("unknown", errorType);
      } else if ("instance".equals(component) && "create".equals(operation)) {
        metrics.recordInstanceCreationError("unknown", "unknown", errorType);
      } else if ("function".equals(component)) {
        metrics.recordFunctionCallError("unknown", "unknown", errorType);
      }
    } catch (final Exception e) {
      LOGGER.fine("Could not record error: " + e.getMessage());
    }
  }

  /**
   * Records performance metrics.
   *
   * @param metricName metric name
   * @param value metric value
   * @param threshold performance threshold (optional)
   */
  public static void recordPerformanceMetric(final String metricName,
                                           final double value,
                                           final Double threshold) {
    try {
      final WasmObservability observability = WasmObservability.getInstance();
      final WasmRuntimeLogger logger = observability.getLogger();

      logger.logPerformanceEvent("measurement", metricName, value,
                                threshold != null ? threshold : Double.MAX_VALUE,
                                "Performance metric recorded");
    } catch (final Exception e) {
      LOGGER.fine("Could not record performance metric: " + e.getMessage());
    }
  }

  /**
   * Records GC events.
   *
   * @param gcType GC type
   * @param duration GC duration
   * @param memoryBefore memory before GC
   * @param memoryAfter memory after GC
   */
  public static void recordGcEvent(final String gcType,
                                  final Duration duration,
                                  final long memoryBefore,
                                  final long memoryAfter) {
    try {
      final WasmObservability observability = WasmObservability.getInstance();
      final WasmRuntimeLogger logger = observability.getLogger();
      final WasmRuntimeMetrics metrics = observability.getMetrics();

      logger.logGcEvent(gcType, duration.toMillis(), memoryBefore, memoryAfter,
                       "GC collection completed");
      metrics.recordGcTriggered(gcType, duration);
    } catch (final Exception e) {
      LOGGER.fine("Could not record GC event: " + e.getMessage());
    }
  }

  /**
   * Creates a simple span for manual instrumentation.
   *
   * @param operationName operation name
   * @param attributes span attributes (optional)
   * @return traced operation that must be closed
   */
  public static WasmRuntimeTracer.TracedOperation startSpan(final String operationName,
                                                           final Attributes attributes) {
    try {
      final WasmObservability observability = WasmObservability.getInstance();
      final WasmRuntimeTracer tracer = observability.getTracer();

      final WasmRuntimeTracer.TracedOperation operation =
          tracer.startEngineOperation(operationName, "manual");

      if (attributes != null) {
        attributes.forEach((key, value) -> {
          if (key.getType() == io.opentelemetry.api.common.AttributeKey.stringKey("").getType()) {
            operation.setAttribute((io.opentelemetry.api.common.AttributeKey<String>) key, (String) value);
          } else if (key.getType() == io.opentelemetry.api.common.AttributeKey.longKey("").getType()) {
            operation.setAttribute((io.opentelemetry.api.common.AttributeKey<Long>) key, (Long) value);
          } else if (key.getType() == io.opentelemetry.api.common.AttributeKey.booleanKey("").getType()) {
            operation.setAttribute((io.opentelemetry.api.common.AttributeKey<Boolean>) key, (Boolean) value);
          }
        });
      }

      return operation;
    } catch (final Exception e) {
      LOGGER.fine("Could not start span: " + e.getMessage());
      // Return a no-op span
      return new NoOpTracedOperation();
    }
  }

  /** Parses component string to log component enum. */
  private static WasmRuntimeLogger.LogComponent parseComponent(final String component) {
    switch (component.toLowerCase()) {
      case "engine": return WasmRuntimeLogger.LogComponent.ENGINE;
      case "store": return WasmRuntimeLogger.LogComponent.STORE;
      case "module": return WasmRuntimeLogger.LogComponent.MODULE;
      case "instance": return WasmRuntimeLogger.LogComponent.INSTANCE;
      case "function": return WasmRuntimeLogger.LogComponent.FUNCTION;
      case "memory": return WasmRuntimeLogger.LogComponent.MEMORY;
      case "wasi": return WasmRuntimeLogger.LogComponent.WASI;
      case "security": return WasmRuntimeLogger.LogComponent.SECURITY;
      case "performance": return WasmRuntimeLogger.LogComponent.PERFORMANCE;
      case "gc": return WasmRuntimeLogger.LogComponent.GC;
      default: return WasmRuntimeLogger.LogComponent.ENGINE;
    }
  }

  /**
   * No-op traced operation implementation for fallback.
   */
  private static final class NoOpTracedOperation extends WasmRuntimeTracer.TracedOperation {
    private NoOpTracedOperation() {
      super(io.opentelemetry.api.trace.Span.getInvalid(), "noop");
    }

    @Override
    public WasmRuntimeTracer.TracedOperation setAttribute(
        final io.opentelemetry.api.common.AttributeKey<String> key, final String value) {
      return this;
    }

    @Override
    public WasmRuntimeTracer.TracedOperation setAttribute(
        final io.opentelemetry.api.common.AttributeKey<Long> key, final long value) {
      return this;
    }

    @Override
    public WasmRuntimeTracer.TracedOperation setAttribute(
        final io.opentelemetry.api.common.AttributeKey<Boolean> key, final boolean value) {
      return this;
    }

    @Override
    public WasmRuntimeTracer.TracedOperation recordEvent(final String name) {
      return this;
    }

    @Override
    public WasmRuntimeTracer.TracedOperation recordEvent(final String name, final Attributes attributes) {
      return this;
    }

    @Override
    public WasmRuntimeTracer.TracedOperation recordException(final Throwable exception) {
      return this;
    }

    @Override
    public void close() {
      // No-op
    }
  }
}