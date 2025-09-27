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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.SemanticAttributes;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Distributed tracing implementation for WebAssembly runtime operations.
 *
 * <p>This class provides comprehensive tracing capabilities for all WebAssembly operations
 * including:
 *
 * <ul>
 *   <li>Engine and store operations
 *   <li>Module compilation and validation
 *   <li>Instance creation and function invocation
 *   <li>Memory operations and host function calls
 *   <li>WASI operations and component interactions
 *   <li>Context propagation across boundaries
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasmRuntimeTracer {

  private static final Logger LOGGER = Logger.getLogger(WasmRuntimeTracer.class.getName());

  /** Tracer instance from OpenTelemetry. */
  private final Tracer tracer;

  /** Active span tracking for correlation. */
  private final Map<String, Span> activeSpans = new ConcurrentHashMap<>();

  /** Span ID generator for correlation. */
  private final AtomicLong spanIdGenerator = new AtomicLong(1);

  // WebAssembly-specific attribute keys
  public static final AttributeKey<String> WASM_RUNTIME = AttributeKey.stringKey("wasm.runtime");
  public static final AttributeKey<String> WASM_ENGINE_TYPE = AttributeKey.stringKey("wasm.engine.type");
  public static final AttributeKey<String> WASM_MODULE_NAME = AttributeKey.stringKey("wasm.module.name");
  public static final AttributeKey<String> WASM_MODULE_HASH = AttributeKey.stringKey("wasm.module.hash");
  public static final AttributeKey<Long> WASM_MODULE_SIZE = AttributeKey.longKey("wasm.module.size");
  public static final AttributeKey<String> WASM_FUNCTION_NAME = AttributeKey.stringKey("wasm.function.name");
  public static final AttributeKey<String> WASM_FUNCTION_TYPE = AttributeKey.stringKey("wasm.function.type");
  public static final AttributeKey<Long> WASM_MEMORY_SIZE = AttributeKey.longKey("wasm.memory.size");
  public static final AttributeKey<Long> WASM_MEMORY_PAGES = AttributeKey.longKey("wasm.memory.pages");
  public static final AttributeKey<String> WASM_INSTANCE_ID = AttributeKey.stringKey("wasm.instance.id");
  public static final AttributeKey<String> WASM_STORE_ID = AttributeKey.stringKey("wasm.store.id");
  public static final AttributeKey<Boolean> WASM_IS_HOST_FUNCTION = AttributeKey.booleanKey("wasm.is_host_function");
  public static final AttributeKey<String> WASM_ERROR_TYPE = AttributeKey.stringKey("wasm.error.type");
  public static final AttributeKey<String> WASM_OPTIMIZATION_LEVEL = AttributeKey.stringKey("wasm.optimization.level");
  public static final AttributeKey<String> WASM_COMPILER_BACKEND = AttributeKey.stringKey("wasm.compiler.backend");

  // WASI-specific attribute keys
  public static final AttributeKey<String> WASI_OPERATION = AttributeKey.stringKey("wasi.operation");
  public static final AttributeKey<String> WASI_FILE_PATH = AttributeKey.stringKey("wasi.file.path");
  public static final AttributeKey<Long> WASI_FILE_SIZE = AttributeKey.longKey("wasi.file.size");
  public static final AttributeKey<String> WASI_PROCESS_ID = AttributeKey.stringKey("wasi.process.id");
  public static final AttributeKey<String> WASI_COMPONENT_ID = AttributeKey.stringKey("wasi.component.id");

  /**
   * Creates a new WebAssembly runtime tracer.
   *
   * @param tracer OpenTelemetry tracer instance
   */
  public WasmRuntimeTracer(final Tracer tracer) {
    this.tracer = tracer;
  }

  /**
   * Starts a span for engine operations.
   *
   * @param operationName the operation name
   * @param engineType the engine type (JNI, Panama)
   * @return traced operation context
   */
  public TracedOperation startEngineOperation(final String operationName, final String engineType) {
    final SpanBuilder spanBuilder = tracer.spanBuilder("wasm.engine." + operationName)
        .setSpanKind(SpanKind.INTERNAL)
        .setAttribute(WASM_RUNTIME, "wasmtime")
        .setAttribute(WASM_ENGINE_TYPE, engineType);

    return new TracedOperation(spanBuilder.startSpan(), generateSpanId());
  }

  /**
   * Starts a span for store operations.
   *
   * @param operationName the operation name
   * @param storeId the store identifier
   * @return traced operation context
   */
  public TracedOperation startStoreOperation(final String operationName, final String storeId) {
    final SpanBuilder spanBuilder = tracer.spanBuilder("wasm.store." + operationName)
        .setSpanKind(SpanKind.INTERNAL)
        .setAttribute(WASM_RUNTIME, "wasmtime")
        .setAttribute(WASM_STORE_ID, storeId);

    return new TracedOperation(spanBuilder.startSpan(), generateSpanId());
  }

  /**
   * Starts a span for module operations.
   *
   * @param operationName the operation name
   * @param moduleName the module name (optional)
   * @param moduleSize the module size in bytes (optional)
   * @return traced operation context
   */
  public TracedOperation startModuleOperation(final String operationName,
                                            final String moduleName,
                                            final Long moduleSize) {
    final SpanBuilder spanBuilder = tracer.spanBuilder("wasm.module." + operationName)
        .setSpanKind(SpanKind.INTERNAL)
        .setAttribute(WASM_RUNTIME, "wasmtime");

    if (moduleName != null) {
      spanBuilder.setAttribute(WASM_MODULE_NAME, moduleName);
    }
    if (moduleSize != null) {
      spanBuilder.setAttribute(WASM_MODULE_SIZE, moduleSize);
    }

    return new TracedOperation(spanBuilder.startSpan(), generateSpanId());
  }

  /**
   * Starts a span for instance operations.
   *
   * @param operationName the operation name
   * @param instanceId the instance identifier
   * @param moduleName the associated module name (optional)
   * @return traced operation context
   */
  public TracedOperation startInstanceOperation(final String operationName,
                                              final String instanceId,
                                              final String moduleName) {
    final SpanBuilder spanBuilder = tracer.spanBuilder("wasm.instance." + operationName)
        .setSpanKind(SpanKind.INTERNAL)
        .setAttribute(WASM_RUNTIME, "wasmtime")
        .setAttribute(WASM_INSTANCE_ID, instanceId);

    if (moduleName != null) {
      spanBuilder.setAttribute(WASM_MODULE_NAME, moduleName);
    }

    return new TracedOperation(spanBuilder.startSpan(), generateSpanId());
  }

  /**
   * Starts a span for function operations.
   *
   * @param functionName the function name
   * @param functionType the function signature (optional)
   * @param isHostFunction whether this is a host function
   * @return traced operation context
   */
  public TracedOperation startFunctionOperation(final String functionName,
                                              final String functionType,
                                              final boolean isHostFunction) {
    final String operationType = isHostFunction ? "host_function" : "wasm_function";
    final SpanBuilder spanBuilder = tracer.spanBuilder("wasm.function." + operationType + ".call")
        .setSpanKind(isHostFunction ? SpanKind.SERVER : SpanKind.INTERNAL)
        .setAttribute(WASM_RUNTIME, "wasmtime")
        .setAttribute(WASM_FUNCTION_NAME, functionName)
        .setAttribute(WASM_IS_HOST_FUNCTION, isHostFunction);

    if (functionType != null) {
      spanBuilder.setAttribute(WASM_FUNCTION_TYPE, functionType);
    }

    return new TracedOperation(spanBuilder.startSpan(), generateSpanId());
  }

  /**
   * Starts a span for memory operations.
   *
   * @param operationName the operation name (read, write, grow, etc.)
   * @param memorySize current memory size in bytes (optional)
   * @param memoryPages current memory pages (optional)
   * @return traced operation context
   */
  public TracedOperation startMemoryOperation(final String operationName,
                                            final Long memorySize,
                                            final Long memoryPages) {
    final SpanBuilder spanBuilder = tracer.spanBuilder("wasm.memory." + operationName)
        .setSpanKind(SpanKind.INTERNAL)
        .setAttribute(WASM_RUNTIME, "wasmtime");

    if (memorySize != null) {
      spanBuilder.setAttribute(WASM_MEMORY_SIZE, memorySize);
    }
    if (memoryPages != null) {
      spanBuilder.setAttribute(WASM_MEMORY_PAGES, memoryPages);
    }

    return new TracedOperation(spanBuilder.startSpan(), generateSpanId());
  }

  /**
   * Starts a span for WASI operations.
   *
   * @param operation the WASI operation name
   * @param filePath file path for file operations (optional)
   * @param processId process ID for process operations (optional)
   * @return traced operation context
   */
  public TracedOperation startWasiOperation(final String operation,
                                          final String filePath,
                                          final String processId) {
    final SpanBuilder spanBuilder = tracer.spanBuilder("wasi." + operation)
        .setSpanKind(SpanKind.INTERNAL)
        .setAttribute(WASM_RUNTIME, "wasmtime")
        .setAttribute(WASI_OPERATION, operation);

    if (filePath != null) {
      spanBuilder.setAttribute(WASI_FILE_PATH, filePath);
    }
    if (processId != null) {
      spanBuilder.setAttribute(WASI_PROCESS_ID, processId);
    }

    return new TracedOperation(spanBuilder.startSpan(), generateSpanId());
  }

  /**
   * Starts a span for component operations (WebAssembly Component Model).
   *
   * @param operationName the operation name
   * @param componentId the component identifier
   * @return traced operation context
   */
  public TracedOperation startComponentOperation(final String operationName, final String componentId) {
    final SpanBuilder spanBuilder = tracer.spanBuilder("wasm.component." + operationName)
        .setSpanKind(SpanKind.INTERNAL)
        .setAttribute(WASM_RUNTIME, "wasmtime")
        .setAttribute(WASI_COMPONENT_ID, componentId);

    return new TracedOperation(spanBuilder.startSpan(), generateSpanId());
  }

  /**
   * Traces a supplier function execution.
   *
   * @param <T> return type
   * @param operationName operation name for the span
   * @param supplier supplier to trace
   * @return result of supplier execution
   */
  public <T> T trace(final String operationName, final Supplier<T> supplier) {
    final Span span = tracer.spanBuilder(operationName)
        .setSpanKind(SpanKind.INTERNAL)
        .setAttribute(WASM_RUNTIME, "wasmtime")
        .startSpan();

    try (final Scope scope = span.makeCurrent()) {
      return supplier.get();
    } catch (final Exception e) {
      span.setStatus(StatusCode.ERROR, e.getMessage());
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  /**
   * Traces a runnable execution.
   *
   * @param operationName operation name for the span
   * @param runnable runnable to trace
   */
  public void trace(final String operationName, final Runnable runnable) {
    final Span span = tracer.spanBuilder(operationName)
        .setSpanKind(SpanKind.INTERNAL)
        .setAttribute(WASM_RUNTIME, "wasmtime")
        .startSpan();

    try (final Scope scope = span.makeCurrent()) {
      runnable.run();
    } catch (final Exception e) {
      span.setStatus(StatusCode.ERROR, e.getMessage());
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  /**
   * Gets the current active span.
   *
   * @return current span or null if none
   */
  public Span getCurrentSpan() {
    return Span.fromContext(Context.current());
  }

  /**
   * Adds attributes to the current span.
   *
   * @param attributes attributes to add
   */
  public void addAttributesToCurrentSpan(final Attributes attributes) {
    final Span currentSpan = getCurrentSpan();
    if (currentSpan != null && !currentSpan.equals(Span.getInvalid())) {
      currentSpan.setAllAttributes(attributes);
    }
  }

  /**
   * Records an event on the current span.
   *
   * @param name event name
   * @param attributes event attributes
   */
  public void recordEvent(final String name, final Attributes attributes) {
    final Span currentSpan = getCurrentSpan();
    if (currentSpan != null && !currentSpan.equals(Span.getInvalid())) {
      currentSpan.addEvent(name, attributes);
    }
  }

  /** Generates a unique span ID for correlation. */
  private String generateSpanId() {
    return "wasm_" + spanIdGenerator.getAndIncrement();
  }

  /**
   * Context for a traced operation that manages span lifecycle.
   */
  public static final class TracedOperation implements AutoCloseable {
    private final Span span;
    private final String spanId;
    private final Scope scope;
    private volatile boolean closed = false;

    /**
     * Creates a new traced operation context.
     *
     * @param span the span instance
     * @param spanId the span identifier
     */
    TracedOperation(final Span span, final String spanId) {
      this.span = span;
      this.spanId = spanId;
      this.scope = span.makeCurrent();
    }

    /**
     * Gets the span for this operation.
     *
     * @return span instance
     */
    public Span getSpan() {
      return span;
    }

    /**
     * Gets the span ID for this operation.
     *
     * @return span ID
     */
    public String getSpanId() {
      return spanId;
    }

    /**
     * Adds an attribute to the span.
     *
     * @param key attribute key
     * @param value attribute value
     * @return this context for chaining
     */
    public TracedOperation setAttribute(final AttributeKey<String> key, final String value) {
      if (value != null) {
        span.setAttribute(key, value);
      }
      return this;
    }

    /**
     * Adds a numeric attribute to the span.
     *
     * @param key attribute key
     * @param value attribute value
     * @return this context for chaining
     */
    public TracedOperation setAttribute(final AttributeKey<Long> key, final long value) {
      span.setAttribute(key, value);
      return this;
    }

    /**
     * Adds a boolean attribute to the span.
     *
     * @param key attribute key
     * @param value attribute value
     * @return this context for chaining
     */
    public TracedOperation setAttribute(final AttributeKey<Boolean> key, final boolean value) {
      span.setAttribute(key, value);
      return this;
    }

    /**
     * Records an event on the span.
     *
     * @param name event name
     * @return this context for chaining
     */
    public TracedOperation recordEvent(final String name) {
      span.addEvent(name);
      return this;
    }

    /**
     * Records an event with attributes on the span.
     *
     * @param name event name
     * @param attributes event attributes
     * @return this context for chaining
     */
    public TracedOperation recordEvent(final String name, final Attributes attributes) {
      span.addEvent(name, attributes);
      return this;
    }

    /**
     * Records an exception on the span.
     *
     * @param exception exception to record
     * @return this context for chaining
     */
    public TracedOperation recordException(final Throwable exception) {
      span.setStatus(StatusCode.ERROR, exception.getMessage());
      span.recordException(exception);
      return this;
    }

    /**
     * Sets the span status to error.
     *
     * @param errorMessage error message
     * @return this context for chaining
     */
    public TracedOperation setError(final String errorMessage) {
      span.setStatus(StatusCode.ERROR, errorMessage);
      return this;
    }

    /**
     * Sets the span status to ok.
     *
     * @return this context for chaining
     */
    public TracedOperation setOk() {
      span.setStatus(StatusCode.OK);
      return this;
    }

    /**
     * Finishes the span and closes the scope.
     */
    @Override
    public void close() {
      if (!closed) {
        try {
          scope.close();
        } finally {
          span.end();
          closed = true;
        }
      }
    }
  }
}