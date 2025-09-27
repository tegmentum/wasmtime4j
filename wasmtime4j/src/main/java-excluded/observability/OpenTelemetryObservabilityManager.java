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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.instrumentation.resources.ContainerResource;
import io.opentelemetry.instrumentation.resources.HostResource;
import io.opentelemetry.instrumentation.resources.OsResource;
import io.opentelemetry.instrumentation.resources.ProcessResource;
import io.opentelemetry.instrumentation.resources.ProcessRuntimeResource;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.ResourceAttributes;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized OpenTelemetry observability manager for wasmtime4j.
 *
 * <p>This class provides comprehensive observability capabilities including:
 *
 * <ul>
 *   <li>Distributed tracing with context propagation
 *   <li>Metrics collection and aggregation
 *   <li>Structured logging with correlation
 *   <li>Resource detection and attribute enrichment
 *   <li>Multiple exporter support (OTLP, Jaeger, Prometheus)
 *   <li>Production-ready configuration management
 * </ul>
 *
 * @since 1.0.0
 */
public final class OpenTelemetryObservabilityManager {

  private static final Logger LOGGER =
      Logger.getLogger(OpenTelemetryObservabilityManager.class.getName());

  /** Service name for wasmtime4j telemetry data. */
  public static final String SERVICE_NAME = "wasmtime4j";

  /** Service namespace for wasmtime4j telemetry data. */
  public static final String SERVICE_NAMESPACE = "ai.tegmentum";

  /** Service version from system property or default. */
  public static final String SERVICE_VERSION =
      System.getProperty("wasmtime4j.version", "1.0.0-SNAPSHOT");

  /** Instrumentation library name. */
  public static final String INSTRUMENTATION_NAME = "ai.tegmentum.wasmtime4j";

  /** Instrumentation library version. */
  public static final String INSTRUMENTATION_VERSION = SERVICE_VERSION;

  /** Singleton instance. */
  private static volatile OpenTelemetryObservabilityManager instance;

  /** Initialization lock. */
  private static final Object LOCK = new Object();

  /** OpenTelemetry SDK instance. */
  private volatile OpenTelemetry openTelemetry;

  /** Configuration for observability. */
  private volatile ObservabilityConfiguration configuration;

  /** Initialization status. */
  private final AtomicBoolean initialized = new AtomicBoolean(false);

  /** Shutdown status. */
  private final AtomicBoolean shutdown = new AtomicBoolean(false);

  /** Active exporters for cleanup. */
  private final Map<String, AutoCloseable> activeExporters = new ConcurrentHashMap<>();

  /** Private constructor for singleton pattern. */
  private OpenTelemetryObservabilityManager() {
    // Initialize with default configuration
    this.configuration = ObservabilityConfiguration.createDefault();
  }

  /**
   * Gets the singleton instance of the observability manager.
   *
   * @return observability manager instance
   */
  public static OpenTelemetryObservabilityManager getInstance() {
    if (instance == null) {
      synchronized (LOCK) {
        if (instance == null) {
          instance = new OpenTelemetryObservabilityManager();
          instance.initializeWithDefaults();
        }
      }
    }
    return instance;
  }

  /**
   * Initializes the observability manager with the specified configuration.
   *
   * @param config observability configuration
   * @return this instance for method chaining
   */
  public OpenTelemetryObservabilityManager initialize(final ObservabilityConfiguration config) {
    if (shutdown.get()) {
      throw new IllegalStateException("Observability manager has been shutdown");
    }

    synchronized (LOCK) {
      if (initialized.get()) {
        LOGGER.warning("Observability manager already initialized, ignoring re-initialization");
        return this;
      }

      this.configuration = config != null ? config : ObservabilityConfiguration.createDefault();

      try {
        // Initialize based on configuration mode
        if (configuration.isAutoConfigurationEnabled()) {
          initializeWithAutoConfiguration();
        } else {
          initializeWithManualConfiguration();
        }

        initialized.set(true);
        LOGGER.info(
            String.format(
                "OpenTelemetry observability initialized - service=%s, version=%s, mode=%s",
                SERVICE_NAME,
                SERVICE_VERSION,
                configuration.isAutoConfigurationEnabled() ? "auto" : "manual"));

      } catch (final Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to initialize OpenTelemetry observability", e);
        throw new RuntimeException("OpenTelemetry initialization failed", e);
      }
    }

    return this;
  }

  /** Initializes with default configuration for backwards compatibility. */
  private void initializeWithDefaults() {
    initialize(ObservabilityConfiguration.createDefault());
  }

  /** Initializes using OpenTelemetry auto-configuration. */
  private void initializeWithAutoConfiguration() {
    LOGGER.info("Initializing OpenTelemetry with auto-configuration");

    // Use OpenTelemetry auto-configuration
    final AutoConfiguredOpenTelemetrySdk autoConfiguredSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addResourceCustomizer(this::customizeResource)
            .setResultAsGlobal(configuration.isGlobalProviderEnabled())
            .build();

    this.openTelemetry = autoConfiguredSdk.getOpenTelemetrySdk();
  }

  /** Initializes using manual configuration. */
  private void initializeWithManualConfiguration() {
    LOGGER.info("Initializing OpenTelemetry with manual configuration");

    final Resource resource = buildResource();

    // Build tracer provider
    final SdkTracerProvider tracerProvider = buildTracerProvider(resource);

    // Build meter provider
    final SdkMeterProvider meterProvider = buildMeterProvider(resource);

    // Build logger provider
    final SdkLoggerProvider loggerProvider = buildLoggerProvider(resource);

    // Build OpenTelemetry SDK
    final OpenTelemetrySdkBuilder sdkBuilder =
        OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setMeterProvider(meterProvider)
            .setLoggerProvider(loggerProvider);

    this.openTelemetry = sdkBuilder.build();

    // Set as global if configured
    if (configuration.isGlobalProviderEnabled()) {
      GlobalOpenTelemetry.set(this.openTelemetry);
    }
  }

  /** Builds the resource with appropriate attributes. */
  private Resource buildResource() {
    final Resource.Builder resourceBuilder =
        Resource.getDefault()
            .merge(
                Resource.builder()
                    .put(ResourceAttributes.SERVICE_NAME, SERVICE_NAME)
                    .put(ResourceAttributes.SERVICE_NAMESPACE, SERVICE_NAMESPACE)
                    .put(ResourceAttributes.SERVICE_VERSION, SERVICE_VERSION)
                    .put(ResourceAttributes.SERVICE_INSTANCE_ID, generateInstanceId())
                    .build());

    // Add runtime and environment resources
    if (configuration.isResourceDetectionEnabled()) {
      resourceBuilder
          .merge(ProcessRuntimeResource.get())
          .merge(ProcessResource.get())
          .merge(OsResource.get())
          .merge(HostResource.get())
          .merge(ContainerResource.get());
    }

    // Add custom attributes
    configuration
        .getResourceAttributes()
        .forEach((key, value) -> resourceBuilder.put(key, value));

    return resourceBuilder.build();
  }

  /** Customizes the resource for auto-configuration. */
  private Resource customizeResource(final Resource existingResource,
                                   final io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties config) {
    final Resource.Builder resourceBuilder =
        Resource.builder()
            .put(ResourceAttributes.SERVICE_NAME, SERVICE_NAME)
            .put(ResourceAttributes.SERVICE_NAMESPACE, SERVICE_NAMESPACE)
            .put(ResourceAttributes.SERVICE_VERSION, SERVICE_VERSION)
            .put(ResourceAttributes.SERVICE_INSTANCE_ID, generateInstanceId());

    // Add custom attributes
    configuration
        .getResourceAttributes()
        .forEach((key, value) -> resourceBuilder.put(key, value));

    return existingResource.merge(resourceBuilder.build());
  }

  /** Builds the tracer provider with configured exporters. */
  private SdkTracerProvider buildTracerProvider(final Resource resource) {
    final SdkTracerProviderBuilder builder =
        SdkTracerProvider.builder().setResource(resource);

    // Configure sampling
    if (configuration.getTracingSampler() != null) {
      builder.setSampler(configuration.getTracingSampler());
    }

    // Add span processors for each configured exporter
    for (final Map.Entry<String, SpanExporter> entry :
         configuration.getSpanExporters().entrySet()) {
      final SpanExporter exporter = entry.getValue();
      activeExporters.put("trace_" + entry.getKey(), exporter);

      if (configuration.useBatchProcessors()) {
        builder.addSpanProcessor(
            BatchSpanProcessor.builder(exporter)
                .setMaxExportBatchSize(configuration.getBatchSize())
                .setExportTimeout(Duration.ofMillis(configuration.getExportTimeoutMs()))
                .setScheduleDelay(Duration.ofMillis(configuration.getBatchDelayMs()))
                .build());
      } else {
        builder.addSpanProcessor(SimpleSpanProcessor.create(exporter));
      }
    }

    return builder.build();
  }

  /** Builds the meter provider with configured exporters. */
  private SdkMeterProvider buildMeterProvider(final Resource resource) {
    final SdkMeterProviderBuilder builder =
        SdkMeterProvider.builder().setResource(resource);

    // Add metric readers for each configured exporter
    for (final Map.Entry<String, MetricExporter> entry :
         configuration.getMetricExporters().entrySet()) {
      final MetricExporter exporter = entry.getValue();
      activeExporters.put("metrics_" + entry.getKey(), exporter);

      builder.registerMetricReader(
          PeriodicMetricReader.builder(exporter)
              .setInterval(Duration.ofMillis(configuration.getMetricExportIntervalMs()))
              .build());
    }

    return builder.build();
  }

  /** Builds the logger provider with configured exporters. */
  private SdkLoggerProvider buildLoggerProvider(final Resource resource) {
    final SdkLoggerProviderBuilder builder =
        SdkLoggerProvider.builder().setResource(resource);

    // Add log processors for each configured exporter
    for (final Map.Entry<String, LogRecordExporter> entry :
         configuration.getLogExporters().entrySet()) {
      final LogRecordExporter exporter = entry.getValue();
      activeExporters.put("logs_" + entry.getKey(), exporter);

      if (configuration.useBatchProcessors()) {
        builder.addLogRecordProcessor(
            BatchLogRecordProcessor.builder(exporter)
                .setMaxExportBatchSize(configuration.getBatchSize())
                .setExportTimeout(Duration.ofMillis(configuration.getExportTimeoutMs()))
                .setScheduleDelay(Duration.ofMillis(configuration.getBatchDelayMs()))
                .build());
      } else {
        builder.addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter));
      }
    }

    return builder.build();
  }

  /** Generates a unique instance ID for this service instance. */
  private String generateInstanceId() {
    return System.getProperty("wasmtime4j.instance.id",
                             java.util.UUID.randomUUID().toString());
  }

  /**
   * Gets the OpenTelemetry instance.
   *
   * @return OpenTelemetry instance
   * @throws IllegalStateException if not initialized
   */
  public OpenTelemetry getOpenTelemetry() {
    if (!initialized.get()) {
      throw new IllegalStateException("Observability manager not initialized");
    }
    return openTelemetry;
  }

  /**
   * Gets the tracer provider.
   *
   * @return tracer provider
   */
  public TracerProvider getTracerProvider() {
    return getOpenTelemetry().getTracerProvider();
  }

  /**
   * Gets the meter provider.
   *
   * @return meter provider
   */
  public MeterProvider getMeterProvider() {
    return getOpenTelemetry().getMeterProvider();
  }

  /**
   * Gets the logger provider.
   *
   * @return logger provider
   */
  public LoggerProvider getLoggerProvider() {
    return getOpenTelemetry().getLoggerProvider();
  }

  /**
   * Gets the current configuration.
   *
   * @return observability configuration
   */
  public ObservabilityConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Checks if the manager is initialized.
   *
   * @return true if initialized
   */
  public boolean isInitialized() {
    return initialized.get();
  }

  /**
   * Checks if the manager is shutdown.
   *
   * @return true if shutdown
   */
  public boolean isShutdown() {
    return shutdown.get();
  }

  /**
   * Shuts down the observability manager and releases all resources.
   */
  public void shutdown() {
    if (shutdown.getAndSet(true)) {
      return; // Already shutdown
    }

    synchronized (LOCK) {
      try {
        LOGGER.info("Shutting down OpenTelemetry observability manager");

        // Close all active exporters
        for (final Map.Entry<String, AutoCloseable> entry : activeExporters.entrySet()) {
          try {
            entry.getValue().close();
          } catch (final Exception e) {
            LOGGER.log(
                Level.WARNING,
                "Error closing exporter " + entry.getKey(),
                e);
          }
        }
        activeExporters.clear();

        // Shutdown OpenTelemetry SDK if it's an SDK instance
        if (openTelemetry instanceof OpenTelemetrySdk) {
          final OpenTelemetrySdk sdk = (OpenTelemetrySdk) openTelemetry;
          sdk.getSdkTracerProvider().shutdown().join(10, TimeUnit.SECONDS);
          sdk.getSdkMeterProvider().shutdown().join(10, TimeUnit.SECONDS);
          sdk.getSdkLoggerProvider().shutdown().join(10, TimeUnit.SECONDS);
        }

        initialized.set(false);
        LOGGER.info("OpenTelemetry observability manager shutdown completed");

      } catch (final Exception e) {
        LOGGER.log(Level.SEVERE, "Error during observability manager shutdown", e);
      }
    }
  }

  /** Shutdown hook for cleanup. */
  static {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (instance != null && instance.isInitialized()) {
                    instance.shutdown();
                  }
                }));
  }
}