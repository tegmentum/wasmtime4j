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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.common.Attributes;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests for {@link WasmObservability}.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
final class WasmObservabilityTest {

  private WasmObservability observability;

  @BeforeEach
  void setUp() {
    observability = WasmObservability.getInstance();
  }

  @AfterEach
  void tearDown() {
    if (observability != null) {
      observability.shutdown();
    }
  }

  @Test
  void shouldInitializeWithDefaultConfiguration() {
    // When
    WasmObservability.initialize();

    // Then
    assertThat(observability.isHealthy()).isTrue();
    assertThat(observability.getTracer()).isNotNull();
    assertThat(observability.getMetrics()).isNotNull();
    assertThat(observability.getLogger()).isNotNull();
  }

  @Test
  void shouldInitializeWithCustomConfiguration() {
    // Given
    final ObservabilityConfiguration config = ObservabilityConfiguration.builder()
        .setResourceDetectionEnabled(true)
        .addLoggingExporters()
        .build();

    // When
    WasmObservability.initialize(config);

    // Then
    assertThat(observability.isHealthy()).isTrue();
    assertThat(observability.getManager().getConfiguration().isResourceDetectionEnabled()).isTrue();
  }

  @Test
  void shouldProvideAccessToComponents() {
    // Given
    WasmObservability.initialize();

    // When
    final WasmRuntimeTracer tracer = observability.getTracer();
    final WasmRuntimeMetrics metrics = observability.getMetrics();
    final WasmRuntimeLogger logger = observability.getLogger();
    final OpenTelemetryObservabilityManager manager = observability.getManager();

    // Then
    assertThat(tracer).isNotNull();
    assertThat(metrics).isNotNull();
    assertThat(logger).isNotNull();
    assertThat(manager).isNotNull();
  }

  @Test
  void shouldObserveOperation() {
    // Given
    WasmObservability.initialize();
    final AtomicBoolean operationCalled = new AtomicBoolean(false);

    // When
    final String result = observability.observeOperation("test_operation", () -> {
      operationCalled.set(true);
      return "success";
    });

    // Then
    assertThat(result).isEqualTo("success");
    assertThat(operationCalled.get()).isTrue();
  }

  @Test
  void shouldObserveOperationWithException() {
    // Given
    WasmObservability.initialize();
    final RuntimeException testException = new RuntimeException("Test exception");

    // When / Then
    assertThatThrownBy(() -> {
      observability.observeOperation("failing_operation", () -> {
        throw testException;
      });
    }).isSameAs(testException);
  }

  @Test
  void shouldObserveAsyncOperation() throws Exception {
    // Given
    WasmObservability.initialize();
    final AtomicBoolean operationCalled = new AtomicBoolean(false);

    // When
    final CompletableFuture<String> result = observability.observeAsyncOperation("async_test", () -> {
      operationCalled.set(true);
      return CompletableFuture.completedFuture("async_success");
    });

    // Then
    assertThat(result.get()).isEqualTo("async_success");
    assertThat(operationCalled.get()).isTrue();
  }

  @Test
  void shouldObserveEngineOperation() {
    // Given
    WasmObservability.initialize();
    final AtomicInteger operationCount = new AtomicInteger(0);

    // When
    final String result = observability.observeEngineOperation("jni", "create", () -> {
      operationCount.incrementAndGet();
      return "engine_created";
    });

    // Then
    assertThat(result).isEqualTo("engine_created");
    assertThat(operationCount.get()).isEqualTo(1);
  }

  @Test
  void shouldObserveModuleOperation() {
    // Given
    WasmObservability.initialize();
    final AtomicBoolean operationCalled = new AtomicBoolean(false);

    // When
    final String result = observability.observeModuleOperation("compile", "test_module", 1024L, () -> {
      operationCalled.set(true);
      return "module_compiled";
    });

    // Then
    assertThat(result).isEqualTo("module_compiled");
    assertThat(operationCalled.get()).isTrue();
  }

  @Test
  void shouldObserveFunctionCall() {
    // Given
    WasmObservability.initialize();
    final AtomicBoolean operationCalled = new AtomicBoolean(false);

    // When
    final Object result = observability.observeFunctionCall("add", "(i32, i32) -> i32", false, () -> {
      operationCalled.set(true);
      return 42;
    });

    // Then
    assertThat(result).isEqualTo(42);
    assertThat(operationCalled.get()).isTrue();
  }

  @Test
  void shouldObserveHostFunctionCall() {
    // Given
    WasmObservability.initialize();
    final AtomicBoolean operationCalled = new AtomicBoolean(false);

    // When
    final String result = observability.observeFunctionCall("host_log", "(i32, i32)", true, () -> {
      operationCalled.set(true);
      return "logged";
    });

    // Then
    assertThat(result).isEqualTo("logged");
    assertThat(operationCalled.get()).isTrue();
  }

  @Test
  void shouldProvideObservabilityStatus() {
    // Given
    WasmObservability.initialize();

    // When
    final String status = observability.getObservabilityStatus();

    // Then
    assertThat(status).isNotNull();
    assertThat(status).contains("WebAssembly Observability Status");
    assertThat(status).contains("Manager Initialized: true");
    assertThat(status).contains("Components Initialized: true");
  }

  @Test
  void shouldReportHealthyWhenInitialized() {
    // Given
    WasmObservability.initialize();

    // When
    final boolean healthy = observability.isHealthy();

    // Then
    assertThat(healthy).isTrue();
  }

  @Test
  void shouldReportUnhealthyAfterShutdown() {
    // Given
    WasmObservability.initialize();
    assertThat(observability.isHealthy()).isTrue();

    // When
    observability.shutdown();

    // Then
    assertThat(observability.isHealthy()).isFalse();
  }

  @Test
  void shouldHandleMultipleInitializationCalls() {
    // When
    WasmObservability.initialize();
    final WasmRuntimeTracer firstTracer = observability.getTracer();

    WasmObservability.initialize(); // Second call
    final WasmRuntimeTracer secondTracer = observability.getTracer();

    // Then
    assertThat(firstTracer).isSameAs(secondTracer);
    assertThat(observability.isHealthy()).isTrue();
  }

  @Test
  void shouldShutdownProperly() {
    // Given
    WasmObservability.initialize();
    assertThat(observability.isHealthy()).isTrue();

    // When
    observability.shutdown();

    // Then
    assertThat(observability.getManager().isShutdown()).isTrue();
  }
}