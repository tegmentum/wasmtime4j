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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests for {@link OpenTelemetryObservabilityManager}.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
final class OpenTelemetryObservabilityManagerTest {

  private OpenTelemetryObservabilityManager manager;

  @BeforeEach
  void setUp() {
    manager = new OpenTelemetryObservabilityManager();
  }

  @AfterEach
  void tearDown() {
    if (manager != null && manager.isInitialized()) {
      manager.shutdown();
    }
  }

  @Test
  void shouldInitializeWithDefaultConfiguration() {
    // When
    manager.initialize(ObservabilityConfiguration.createDefault());

    // Then
    assertThat(manager.isInitialized()).isTrue();
    assertThat(manager.isShutdown()).isFalse();
    assertThat(manager.getOpenTelemetry()).isNotNull();
    assertThat(manager.getTracerProvider()).isNotNull();
    assertThat(manager.getMeterProvider()).isNotNull();
    assertThat(manager.getLoggerProvider()).isNotNull();
  }

  @Test
  void shouldInitializeWithAutoConfiguration() {
    // Given
    final ObservabilityConfiguration config = ObservabilityConfiguration.createAutoConfigured();

    // When
    manager.initialize(config);

    // Then
    assertThat(manager.isInitialized()).isTrue();
    assertThat(manager.getConfiguration().isAutoConfigurationEnabled()).isTrue();
  }

  @Test
  void shouldInitializeWithProductionConfiguration() {
    // Given
    final ObservabilityConfiguration config = ObservabilityConfiguration
        .createProduction("http://localhost:4317");

    // When
    manager.initialize(config);

    // Then
    assertThat(manager.isInitialized()).isTrue();
    assertThat(manager.getConfiguration().getSpanExporters()).containsKey("otlp");
    assertThat(manager.getConfiguration().getMetricExporters()).containsKey("otlp");
    assertThat(manager.getConfiguration().getLogExporters()).containsKey("otlp");
  }

  @Test
  void shouldInitializeWithCustomConfiguration() {
    // Given
    final ObservabilityConfiguration config = ObservabilityConfiguration.builder()
        .setGlobalProviderEnabled(false)
        .setResourceDetectionEnabled(true)
        .setUseBatchProcessors(false)
        .addLoggingExporters()
        .build();

    // When
    manager.initialize(config);

    // Then
    assertThat(manager.isInitialized()).isTrue();
    assertThat(manager.getConfiguration().isGlobalProviderEnabled()).isFalse();
    assertThat(manager.getConfiguration().isResourceDetectionEnabled()).isTrue();
    assertThat(manager.getConfiguration().useBatchProcessors()).isFalse();
  }

  @Test
  void shouldNotReinitializeWhenAlreadyInitialized() {
    // Given
    manager.initialize(ObservabilityConfiguration.createDefault());
    final OpenTelemetry firstInstance = manager.getOpenTelemetry();

    // When
    manager.initialize(ObservabilityConfiguration.builder().addLoggingExporters().build());

    // Then
    assertThat(manager.getOpenTelemetry()).isSameAs(firstInstance);
  }

  @Test
  void shouldThrowExceptionWhenAccessingBeforeInitialization() {
    // When / Then
    assertThatThrownBy(() -> manager.getOpenTelemetry())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not initialized");
  }

  @Test
  void shouldThrowExceptionWhenInitializingAfterShutdown() {
    // Given
    manager.initialize(ObservabilityConfiguration.createDefault());
    manager.shutdown();

    // When / Then
    assertThatThrownBy(() -> manager.initialize(ObservabilityConfiguration.createDefault()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("shutdown");
  }

  @Test
  void shouldShutdownProperly() {
    // Given
    manager.initialize(ObservabilityConfiguration.createDefault());
    assertThat(manager.isInitialized()).isTrue();

    // When
    manager.shutdown();

    // Then
    assertThat(manager.isShutdown()).isTrue();
  }

  @Test
  void shouldHandleMultipleShutdownCalls() {
    // Given
    manager.initialize(ObservabilityConfiguration.createDefault());

    // When
    manager.shutdown();
    manager.shutdown(); // Second call

    // Then
    assertThat(manager.isShutdown()).isTrue();
  }

  @Test
  void shouldProvideAccessToProviders() {
    // Given
    manager.initialize(ObservabilityConfiguration.createDefault());

    // When
    final TracerProvider tracerProvider = manager.getTracerProvider();
    final MeterProvider meterProvider = manager.getMeterProvider();
    final LoggerProvider loggerProvider = manager.getLoggerProvider();

    // Then
    assertThat(tracerProvider).isNotNull();
    assertThat(meterProvider).isNotNull();
    assertThat(loggerProvider).isNotNull();
  }

  @Test
  void shouldProvideAccessToConfiguration() {
    // Given
    final ObservabilityConfiguration config = ObservabilityConfiguration.builder()
        .setResourceDetectionEnabled(false)
        .addLoggingExporters()
        .build();
    manager.initialize(config);

    // When
    final ObservabilityConfiguration retrievedConfig = manager.getConfiguration();

    // Then
    assertThat(retrievedConfig).isNotNull();
    assertThat(retrievedConfig.isResourceDetectionEnabled()).isFalse();
  }

  @Test
  void shouldReturnCorrectInitializationStatus() {
    // Initially not initialized
    assertThat(manager.isInitialized()).isFalse();
    assertThat(manager.isShutdown()).isFalse();

    // After initialization
    manager.initialize(ObservabilityConfiguration.createDefault());
    assertThat(manager.isInitialized()).isTrue();
    assertThat(manager.isShutdown()).isFalse();

    // After shutdown
    manager.shutdown();
    assertThat(manager.isInitialized()).isFalse();
    assertThat(manager.isShutdown()).isTrue();
  }
}