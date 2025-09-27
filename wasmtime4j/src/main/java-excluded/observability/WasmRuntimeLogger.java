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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Structured logging implementation with OpenTelemetry integration for WebAssembly runtime.
 *
 * <p>This class provides comprehensive logging capabilities that integrate with OpenTelemetry logs,
 * ensuring correlation between traces, metrics, and logs for complete observability.
 *
 * <ul>
 *   <li>Structured logging with consistent attribute schemas
 *   <li>Automatic trace correlation and context propagation
 *   <li>Performance-optimized logging with sampling
 *   <li>Rich context for WebAssembly-specific operations
 *   <li>Integration with existing java.util.logging
 *   <li>Log correlation across runtime boundaries
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasmRuntimeLogger {

  /** OpenTelemetry logger instance. */
  private final Logger otelLogger;

  /** Java util logger for fallback. */
  private final java.util.logging.Logger julLogger;

  /** Log sequence number for ordering. */
  private final AtomicLong logSequence = new AtomicLong(1);

  // WebAssembly-specific log attribute keys
  public static final AttributeKey<String> LOG_COMPONENT = AttributeKey.stringKey("log.component");
  public static final AttributeKey<String> LOG_OPERATION = AttributeKey.stringKey("log.operation");
  public static final AttributeKey<String> LOG_WASM_ENGINE = AttributeKey.stringKey("log.wasm.engine");
  public static final AttributeKey<String> LOG_WASM_MODULE = AttributeKey.stringKey("log.wasm.module");
  public static final AttributeKey<String> LOG_WASM_FUNCTION = AttributeKey.stringKey("log.wasm.function");
  public static final AttributeKey<String> LOG_WASM_INSTANCE = AttributeKey.stringKey("log.wasm.instance");
  public static final AttributeKey<String> LOG_WASM_STORE = AttributeKey.stringKey("log.wasm.store");
  public static final AttributeKey<Long> LOG_SEQUENCE = AttributeKey.longKey("log.sequence");

  /** Log components for categorization. */
  public enum LogComponent {
    ENGINE("engine"),
    STORE("store"),
    MODULE("module"),
    INSTANCE("instance"),
    FUNCTION("function"),
    MEMORY("memory"),
    WASI("wasi"),
    SECURITY("security"),
    PERFORMANCE("performance"),
    GC("gc");

    private final String value;

    LogComponent(final String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * Creates a new WebAssembly runtime logger.
   *
   * @param otelLogger OpenTelemetry logger instance
   * @param loggerName Java util logger name
   */
  public WasmRuntimeLogger(final Logger otelLogger, final String loggerName) {
    this.otelLogger = otelLogger;
    this.julLogger = java.util.logging.Logger.getLogger(loggerName);
  }

  /**
   * Logs a debug message with WebAssembly context.
   *
   * @param component the component generating the log
   * @param operation the operation being performed
   * @param message the log message
   * @param attributes additional attributes
   */
  public void debug(final LogComponent component,
                   final String operation,
                   final String message,
                   final Attributes attributes) {
    log(Severity.DEBUG, component, operation, message, null, attributes);
  }

  /**
   * Logs an info message with WebAssembly context.
   *
   * @param component the component generating the log
   * @param operation the operation being performed
   * @param message the log message
   * @param attributes additional attributes
   */
  public void info(final LogComponent component,
                  final String operation,
                  final String message,
                  final Attributes attributes) {
    log(Severity.INFO, component, operation, message, null, attributes);
  }

  /**
   * Logs a warning message with WebAssembly context.
   *
   * @param component the component generating the log
   * @param operation the operation being performed
   * @param message the log message
   * @param attributes additional attributes
   */
  public void warn(final LogComponent component,
                  final String operation,
                  final String message,
                  final Attributes attributes) {
    log(Severity.WARN, component, operation, message, null, attributes);
  }

  /**
   * Logs an error message with WebAssembly context.
   *
   * @param component the component generating the log
   * @param operation the operation being performed
   * @param message the log message
   * @param throwable associated exception
   * @param attributes additional attributes
   */
  public void error(final LogComponent component,
                   final String operation,
                   final String message,
                   final Throwable throwable,
                   final Attributes attributes) {
    log(Severity.ERROR, component, operation, message, throwable, attributes);
  }

  /**
   * Logs a message at the specified severity level.
   *
   * @param severity log severity
   * @param component the component generating the log
   * @param operation the operation being performed
   * @param message the log message
   * @param throwable associated exception (optional)
   * @param attributes additional attributes (optional)
   */
  public void log(final Severity severity,
                 final LogComponent component,
                 final String operation,
                 final String message,
                 final Throwable throwable,
                 final Attributes attributes) {

    final Instant timestamp = Instant.now();
    final long sequence = logSequence.getAndIncrement();

    // Build base attributes
    final Attributes.Builder attributesBuilder = Attributes.builder()
        .put(LOG_COMPONENT, component.getValue())
        .put(LOG_OPERATION, operation)
        .put(LOG_SEQUENCE, sequence);

    // Add additional attributes if provided
    if (attributes != null) {
      attributes.forEach((key, value) -> {
        if (key.getType() == AttributeKey.stringKey("").getType()) {
          attributesBuilder.put((AttributeKey<String>) key, (String) value);
        } else if (key.getType() == AttributeKey.longKey("").getType()) {
          attributesBuilder.put((AttributeKey<Long>) key, (Long) value);
        } else if (key.getType() == AttributeKey.doubleKey("").getType()) {
          attributesBuilder.put((AttributeKey<Double>) key, (Double) value);
        } else if (key.getType() == AttributeKey.booleanKey("").getType()) {
          attributesBuilder.put((AttributeKey<Boolean>) key, (Boolean) value);
        }
      });
    }

    final Attributes finalAttributes = attributesBuilder.build();

    // Create OpenTelemetry log record
    final LogRecordBuilder logRecordBuilder = otelLogger.logRecordBuilder()
        .setTimestamp(timestamp)
        .setSeverity(severity)
        .setBody(message)
        .setAllAttributes(finalAttributes);

    // Add trace correlation if available
    final Span currentSpan = Span.fromContext(Context.current());
    if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
      final SpanContext spanContext = currentSpan.getSpanContext();
      logRecordBuilder.setContext(Context.current());
    }

    // Emit the log record
    logRecordBuilder.emit();

    // Also log to Java util logging for compatibility
    logToJul(severity, component, operation, message, throwable, finalAttributes);
  }

  /**
   * Logs engine lifecycle events.
   *
   * @param operation engine operation (create, destroy, configure)
   * @param engineType engine type (JNI, Panama)
   * @param message descriptive message
   */
  public void logEngineEvent(final String operation, final String engineType, final String message) {
    info(LogComponent.ENGINE, operation, message,
         Attributes.builder().put(LOG_WASM_ENGINE, engineType).build());
  }

  /**
   * Logs store lifecycle events.
   *
   * @param operation store operation (create, destroy)
   * @param storeId store identifier
   * @param message descriptive message
   */
  public void logStoreEvent(final String operation, final String storeId, final String message) {
    info(LogComponent.STORE, operation, message,
         Attributes.builder().put(LOG_WASM_STORE, storeId).build());
  }

  /**
   * Logs module compilation events.
   *
   * @param operation module operation (compile, validate, cache)
   * @param moduleName module name
   * @param size module size in bytes
   * @param duration compilation duration in milliseconds
   * @param message descriptive message
   */
  public void logModuleEvent(final String operation,
                            final String moduleName,
                            final long size,
                            final long duration,
                            final String message) {
    info(LogComponent.MODULE, operation, message,
         Attributes.builder()
             .put(LOG_WASM_MODULE, moduleName)
             .put("wasm.module.size", size)
             .put("duration_ms", duration)
             .build());
  }

  /**
   * Logs instance lifecycle events.
   *
   * @param operation instance operation (create, destroy)
   * @param instanceId instance identifier
   * @param moduleName associated module name
   * @param message descriptive message
   */
  public void logInstanceEvent(final String operation,
                              final String instanceId,
                              final String moduleName,
                              final String message) {
    info(LogComponent.INSTANCE, operation, message,
         Attributes.builder()
             .put(LOG_WASM_INSTANCE, instanceId)
             .put(LOG_WASM_MODULE, moduleName)
             .build());
  }

  /**
   * Logs function call events.
   *
   * @param functionName function name
   * @param functionType function signature
   * @param isHost whether this is a host function
   * @param duration call duration in microseconds
   * @param message descriptive message
   */
  public void logFunctionCall(final String functionName,
                             final String functionType,
                             final boolean isHost,
                             final long duration,
                             final String message) {
    debug(LogComponent.FUNCTION, "call", message,
          Attributes.builder()
              .put(LOG_WASM_FUNCTION, functionName)
              .put("wasm.function.type", functionType)
              .put("wasm.is_host_function", isHost)
              .put("duration_us", duration)
              .build());
  }

  /**
   * Logs memory operation events.
   *
   * @param operation memory operation (grow, read, write)
   * @param pages memory pages
   * @param bytes bytes involved
   * @param message descriptive message
   */
  public void logMemoryEvent(final String operation,
                            final long pages,
                            final long bytes,
                            final String message) {
    debug(LogComponent.MEMORY, operation, message,
          Attributes.builder()
              .put("wasm.memory.pages", pages)
              .put("wasm.memory.bytes", bytes)
              .build());
  }

  /**
   * Logs WASI operation events.
   *
   * @param operation WASI operation name
   * @param filePath file path for file operations
   * @param duration operation duration in microseconds
   * @param message descriptive message
   */
  public void logWasiEvent(final String operation,
                          final String filePath,
                          final long duration,
                          final String message) {
    debug(LogComponent.WASI, operation, message,
          Attributes.builder()
              .put("wasi.operation", operation)
              .put("wasi.file.path", filePath != null ? filePath : "")
              .put("duration_us", duration)
              .build());
  }

  /**
   * Logs security-related events.
   *
   * @param operation security operation
   * @param resourceType type of resource involved
   * @param allowed whether the operation was allowed
   * @param message descriptive message
   */
  public void logSecurityEvent(final String operation,
                              final String resourceType,
                              final boolean allowed,
                              final String message) {
    final Severity severity = allowed ? Severity.INFO : Severity.WARN;
    log(severity, LogComponent.SECURITY, operation, message, null,
        Attributes.builder()
            .put("security.resource_type", resourceType)
            .put("security.allowed", allowed)
            .build());
  }

  /**
   * Logs performance-related events.
   *
   * @param operation performance-related operation
   * @param metricName performance metric name
   * @param value metric value
   * @param threshold performance threshold
   * @param message descriptive message
   */
  public void logPerformanceEvent(final String operation,
                                 final String metricName,
                                 final double value,
                                 final double threshold,
                                 final String message) {
    final Severity severity = value > threshold ? Severity.WARN : Severity.INFO;
    log(severity, LogComponent.PERFORMANCE, operation, message, null,
        Attributes.builder()
            .put("performance.metric", metricName)
            .put("performance.value", value)
            .put("performance.threshold", threshold)
            .build());
  }

  /**
   * Logs garbage collection events.
   *
   * @param gcType type of GC operation
   * @param duration GC duration in milliseconds
   * @param memoryBefore memory usage before GC
   * @param memoryAfter memory usage after GC
   * @param message descriptive message
   */
  public void logGcEvent(final String gcType,
                        final long duration,
                        final long memoryBefore,
                        final long memoryAfter,
                        final String message) {
    info(LogComponent.GC, gcType, message,
         Attributes.builder()
             .put("gc.type", gcType)
             .put("gc.duration_ms", duration)
             .put("gc.memory_before", memoryBefore)
             .put("gc.memory_after", memoryAfter)
             .put("gc.memory_freed", memoryBefore - memoryAfter)
             .build());
  }

  /**
   * Logs error events with rich context.
   *
   * @param component component where error occurred
   * @param operation operation that failed
   * @param errorType type/category of error
   * @param errorMessage error message
   * @param throwable associated exception
   * @param context additional context attributes
   */
  public void logError(final LogComponent component,
                      final String operation,
                      final String errorType,
                      final String errorMessage,
                      final Throwable throwable,
                      final Attributes context) {
    final Attributes.Builder attributesBuilder = Attributes.builder()
        .put("error.type", errorType);

    if (context != null) {
      context.forEach((key, value) -> {
        if (key.getType() == AttributeKey.stringKey("").getType()) {
          attributesBuilder.put((AttributeKey<String>) key, (String) value);
        } else if (key.getType() == AttributeKey.longKey("").getType()) {
          attributesBuilder.put((AttributeKey<Long>) key, (Long) value);
        } else if (key.getType() == AttributeKey.doubleKey("").getType()) {
          attributesBuilder.put((AttributeKey<Double>) key, (Double) value);
        } else if (key.getType() == AttributeKey.booleanKey("").getType()) {
          attributesBuilder.put((AttributeKey<Boolean>) key, (Boolean) value);
        }
      });
    }

    error(component, operation, errorMessage, throwable, attributesBuilder.build());
  }

  /** Maps OpenTelemetry severity to Java util logging level. */
  private void logToJul(final Severity severity,
                       final LogComponent component,
                       final String operation,
                       final String message,
                       final Throwable throwable,
                       final Attributes attributes) {
    final Level julLevel = mapSeverityToLevel(severity);
    if (julLogger.isLoggable(julLevel)) {
      final String formattedMessage = String.format("[%s:%s] %s %s",
          component.getValue(), operation, message, formatAttributes(attributes));

      if (throwable != null) {
        julLogger.log(julLevel, formattedMessage, throwable);
      } else {
        julLogger.log(julLevel, formattedMessage);
      }
    }
  }

  /** Maps OpenTelemetry severity to Java util logging level. */
  private Level mapSeverityToLevel(final Severity severity) {
    switch (severity) {
      case TRACE:
      case DEBUG:
        return Level.FINE;
      case INFO:
        return Level.INFO;
      case WARN:
        return Level.WARNING;
      case ERROR:
      case FATAL:
        return Level.SEVERE;
      default:
        return Level.INFO;
    }
  }

  /** Formats attributes for Java util logging. */
  private String formatAttributes(final Attributes attributes) {
    if (attributes.isEmpty()) {
      return "";
    }

    final StringBuilder sb = new StringBuilder("(");
    final boolean[] first = {true};

    attributes.forEach((key, value) -> {
      if (!first[0]) {
        sb.append(", ");
      }
      sb.append(key.getKey()).append("=").append(value);
      first[0] = false;
    });

    sb.append(")");
    return sb.toString();
  }
}