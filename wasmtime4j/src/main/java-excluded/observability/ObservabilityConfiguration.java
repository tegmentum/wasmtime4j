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
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration for OpenTelemetry observability in wasmtime4j.
 *
 * <p>This class provides comprehensive configuration options for observability including:
 *
 * <ul>
 *   <li>Tracing configuration (samplers, exporters, processors)
 *   <li>Metrics configuration (exporters, collection intervals)
 *   <li>Logging configuration (exporters, log levels)
 *   <li>Resource detection and attribute configuration
 *   <li>Export batching and performance tuning
 *   <li>Environment-specific settings (development, staging, production)
 * </ul>
 *
 * @since 1.0.0
 */
public final class ObservabilityConfiguration {

  private static final Logger LOGGER =
      Logger.getLogger(ObservabilityConfiguration.class.getName());

  /** Default export timeout in milliseconds. */
  public static final long DEFAULT_EXPORT_TIMEOUT_MS = 30000L; // 30 seconds

  /** Default batch size for processors. */
  public static final int DEFAULT_BATCH_SIZE = 512;

  /** Default batch delay in milliseconds. */
  public static final long DEFAULT_BATCH_DELAY_MS = 2000L; // 2 seconds

  /** Default metric export interval in milliseconds. */
  public static final long DEFAULT_METRIC_EXPORT_INTERVAL_MS = 60000L; // 1 minute

  // Configuration fields
  private final boolean autoConfigurationEnabled;
  private final boolean globalProviderEnabled;
  private final boolean resourceDetectionEnabled;
  private final boolean useBatchProcessors;
  private final long exportTimeoutMs;
  private final int batchSize;
  private final long batchDelayMs;
  private final long metricExportIntervalMs;
  private final Sampler tracingSampler;
  private final Map<String, SpanExporter> spanExporters;
  private final Map<String, MetricExporter> metricExporters;
  private final Map<String, LogRecordExporter> logExporters;
  private final Map<AttributeKey<String>, String> resourceAttributes;

  /** Builder for observability configuration. */
  public static final class Builder {
    private boolean autoConfigurationEnabled = false;
    private boolean globalProviderEnabled = true;
    private boolean resourceDetectionEnabled = true;
    private boolean useBatchProcessors = true;
    private long exportTimeoutMs = DEFAULT_EXPORT_TIMEOUT_MS;
    private int batchSize = DEFAULT_BATCH_SIZE;
    private long batchDelayMs = DEFAULT_BATCH_DELAY_MS;
    private long metricExportIntervalMs = DEFAULT_METRIC_EXPORT_INTERVAL_MS;
    private Sampler tracingSampler = Sampler.parentBased(Sampler.traceIdRatioBased(1.0));
    private Map<String, SpanExporter> spanExporters = new HashMap<>();
    private Map<String, MetricExporter> metricExporters = new HashMap<>();
    private Map<String, LogRecordExporter> logExporters = new HashMap<>();
    private Map<AttributeKey<String>, String> resourceAttributes = new HashMap<>();

    /**
     * Enables or disables OpenTelemetry auto-configuration.
     *
     * @param enabled true to use auto-configuration
     * @return this builder
     */
    public Builder setAutoConfigurationEnabled(final boolean enabled) {
      this.autoConfigurationEnabled = enabled;
      return this;
    }

    /**
     * Enables or disables setting OpenTelemetry as global provider.
     *
     * @param enabled true to set as global provider
     * @return this builder
     */
    public Builder setGlobalProviderEnabled(final boolean enabled) {
      this.globalProviderEnabled = enabled;
      return this;
    }

    /**
     * Enables or disables automatic resource detection.
     *
     * @param enabled true to enable resource detection
     * @return this builder
     */
    public Builder setResourceDetectionEnabled(final boolean enabled) {
      this.resourceDetectionEnabled = enabled;
      return this;
    }

    /**
     * Sets whether to use batch processors.
     *
     * @param enabled true to use batch processors
     * @return this builder
     */
    public Builder setUseBatchProcessors(final boolean enabled) {
      this.useBatchProcessors = enabled;
      return this;
    }

    /**
     * Sets the export timeout.
     *
     * @param timeout export timeout
     * @return this builder
     */
    public Builder setExportTimeout(final Duration timeout) {
      this.exportTimeoutMs = timeout.toMillis();
      return this;
    }

    /**
     * Sets the batch size for processors.
     *
     * @param size batch size
     * @return this builder
     */
    public Builder setBatchSize(final int size) {
      this.batchSize = size;
      return this;
    }

    /**
     * Sets the batch delay for processors.
     *
     * @param delay batch delay
     * @return this builder
     */
    public Builder setBatchDelay(final Duration delay) {
      this.batchDelayMs = delay.toMillis();
      return this;
    }

    /**
     * Sets the metric export interval.
     *
     * @param interval metric export interval
     * @return this builder
     */
    public Builder setMetricExportInterval(final Duration interval) {
      this.metricExportIntervalMs = interval.toMillis();
      return this;
    }

    /**
     * Sets the tracing sampler.
     *
     * @param sampler tracing sampler
     * @return this builder
     */
    public Builder setTracingSampler(final Sampler sampler) {
      this.tracingSampler = sampler;
      return this;
    }

    /**
     * Adds a span exporter.
     *
     * @param name exporter name
     * @param exporter span exporter
     * @return this builder
     */
    public Builder addSpanExporter(final String name, final SpanExporter exporter) {
      this.spanExporters.put(name, exporter);
      return this;
    }

    /**
     * Adds a metric exporter.
     *
     * @param name exporter name
     * @param exporter metric exporter
     * @return this builder
     */
    public Builder addMetricExporter(final String name, final MetricExporter exporter) {
      this.metricExporters.put(name, exporter);
      return this;
    }

    /**
     * Adds a log record exporter.
     *
     * @param name exporter name
     * @param exporter log record exporter
     * @return this builder
     */
    public Builder addLogExporter(final String name, final LogRecordExporter exporter) {
      this.logExporters.put(name, exporter);
      return this;
    }

    /**
     * Adds a resource attribute.
     *
     * @param key attribute key
     * @param value attribute value
     * @return this builder
     */
    public Builder addResourceAttribute(final AttributeKey<String> key, final String value) {
      this.resourceAttributes.put(key, value);
      return this;
    }

    /**
     * Adds an OTLP exporter for all signals.
     *
     * @param endpoint OTLP endpoint
     * @return this builder
     */
    public Builder addOtlpExporter(final String endpoint) {
      return addOtlpExporter(endpoint, null);
    }

    /**
     * Adds an OTLP exporter for all signals with headers.
     *
     * @param endpoint OTLP endpoint
     * @param headers HTTP headers
     * @return this builder
     */
    public Builder addOtlpExporter(final String endpoint, final Map<String, String> headers) {
      final OtlpHttpSpanExporter.Builder spanBuilder = OtlpHttpSpanExporter.builder()
          .setEndpoint(endpoint + "/v1/traces");
      final OtlpHttpMetricExporter.Builder metricBuilder = OtlpHttpMetricExporter.builder()
          .setEndpoint(endpoint + "/v1/metrics");
      final OtlpHttpLogRecordExporter.Builder logBuilder = OtlpHttpLogRecordExporter.builder()
          .setEndpoint(endpoint + "/v1/logs");

      if (headers != null) {
        spanBuilder.setHeaders(() -> headers);
        metricBuilder.setHeaders(() -> headers);
        logBuilder.setHeaders(() -> headers);
      }

      this.spanExporters.put("otlp", spanBuilder.build());
      this.metricExporters.put("otlp", metricBuilder.build());
      this.logExporters.put("otlp", logBuilder.build());

      return this;
    }

    /**
     * Adds a Jaeger exporter for tracing.
     *
     * @param endpoint Jaeger endpoint
     * @return this builder
     */
    public Builder addJaegerExporter(final String endpoint) {
      this.spanExporters.put("jaeger",
          JaegerGrpcSpanExporter.builder()
              .setEndpoint(endpoint)
              .build());
      return this;
    }

    /**
     * Adds a Zipkin exporter for tracing.
     *
     * @param endpoint Zipkin endpoint
     * @return this builder
     */
    public Builder addZipkinExporter(final String endpoint) {
      this.spanExporters.put("zipkin",
          ZipkinSpanExporter.builder()
              .setEndpoint(endpoint)
              .build());
      return this;
    }

    /**
     * Adds a Prometheus exporter for metrics.
     *
     * @param port Prometheus server port
     * @return this builder
     */
    public Builder addPrometheusExporter(final int port) {
      try {
        this.metricExporters.put("prometheus",
            PrometheusHttpServer.builder()
                .setPort(port)
                .build());
      } catch (final Exception e) {
        LOGGER.warning("Failed to create Prometheus exporter: " + e.getMessage());
      }
      return this;
    }

    /**
     * Adds logging exporters for all signals.
     *
     * @return this builder
     */
    public Builder addLoggingExporters() {
      this.spanExporters.put("logging", LoggingSpanExporter.create());
      this.metricExporters.put("logging", LoggingMetricExporter.create());
      this.logExporters.put("logging", SystemOutLogRecordExporter.create());
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return observability configuration
     */
    public ObservabilityConfiguration build() {
      return new ObservabilityConfiguration(
          autoConfigurationEnabled,
          globalProviderEnabled,
          resourceDetectionEnabled,
          useBatchProcessors,
          exportTimeoutMs,
          batchSize,
          batchDelayMs,
          metricExportIntervalMs,
          tracingSampler,
          Collections.unmodifiableMap(new HashMap<>(spanExporters)),
          Collections.unmodifiableMap(new HashMap<>(metricExporters)),
          Collections.unmodifiableMap(new HashMap<>(logExporters)),
          Collections.unmodifiableMap(new HashMap<>(resourceAttributes)));
    }
  }

  /** Private constructor - use builder. */
  private ObservabilityConfiguration(
      final boolean autoConfigurationEnabled,
      final boolean globalProviderEnabled,
      final boolean resourceDetectionEnabled,
      final boolean useBatchProcessors,
      final long exportTimeoutMs,
      final int batchSize,
      final long batchDelayMs,
      final long metricExportIntervalMs,
      final Sampler tracingSampler,
      final Map<String, SpanExporter> spanExporters,
      final Map<String, MetricExporter> metricExporters,
      final Map<String, LogRecordExporter> logExporters,
      final Map<AttributeKey<String>, String> resourceAttributes) {
    this.autoConfigurationEnabled = autoConfigurationEnabled;
    this.globalProviderEnabled = globalProviderEnabled;
    this.resourceDetectionEnabled = resourceDetectionEnabled;
    this.useBatchProcessors = useBatchProcessors;
    this.exportTimeoutMs = exportTimeoutMs;
    this.batchSize = batchSize;
    this.batchDelayMs = batchDelayMs;
    this.metricExportIntervalMs = metricExportIntervalMs;
    this.tracingSampler = tracingSampler;
    this.spanExporters = spanExporters;
    this.metricExporters = metricExporters;
    this.logExporters = logExporters;
    this.resourceAttributes = resourceAttributes;
  }

  /**
   * Creates a new configuration builder.
   *
   * @return configuration builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default configuration suitable for development.
   *
   * @return default configuration
   */
  public static ObservabilityConfiguration createDefault() {
    return builder()
        .addLoggingExporters()
        .build();
  }

  /**
   * Creates a configuration suitable for production use.
   *
   * @param otlpEndpoint OTLP endpoint for production
   * @return production configuration
   */
  public static ObservabilityConfiguration createProduction(final String otlpEndpoint) {
    return builder()
        .setTracingSampler(Sampler.parentBased(Sampler.traceIdRatioBased(0.1))) // 10% sampling
        .addOtlpExporter(otlpEndpoint)
        .build();
  }

  /**
   * Creates a configuration with auto-configuration enabled.
   *
   * @return auto-configured configuration
   */
  public static ObservabilityConfiguration createAutoConfigured() {
    return builder()
        .setAutoConfigurationEnabled(true)
        .build();
  }

  // Getters
  public boolean isAutoConfigurationEnabled() {
    return autoConfigurationEnabled;
  }

  public boolean isGlobalProviderEnabled() {
    return globalProviderEnabled;
  }

  public boolean isResourceDetectionEnabled() {
    return resourceDetectionEnabled;
  }

  public boolean useBatchProcessors() {
    return useBatchProcessors;
  }

  public long getExportTimeoutMs() {
    return exportTimeoutMs;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public long getBatchDelayMs() {
    return batchDelayMs;
  }

  public long getMetricExportIntervalMs() {
    return metricExportIntervalMs;
  }

  public Sampler getTracingSampler() {
    return tracingSampler;
  }

  public Map<String, SpanExporter> getSpanExporters() {
    return spanExporters;
  }

  public Map<String, MetricExporter> getMetricExporters() {
    return metricExporters;
  }

  public Map<String, LogRecordExporter> getLogExporters() {
    return logExporters;
  }

  public Map<AttributeKey<String>, String> getResourceAttributes() {
    return resourceAttributes;
  }
}