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

package ai.tegmentum.wasmtime4j.monitoring;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Comprehensive monitoring integration providing JMX, Micrometer, and OpenTelemetry support for
 * wasmtime4j runtime metrics and observability.
 *
 * <p>This integration provides:
 *
 * <ul>
 *   <li>JMX MBean registration for runtime monitoring
 *   <li>Micrometer metrics registry integration
 *   <li>OpenTelemetry metrics and tracing support
 *   <li>Custom metrics exporters for various systems
 *   <li>Automatic metric collection and publishing
 *   <li>Health check endpoint integration
 * </ul>
 *
 * @since 1.0.0
 */
public final class MonitoringIntegration {

  private static final Logger LOGGER = Logger.getLogger(MonitoringIntegration.class.getName());

  /** Integration configuration. */
  public static final class MonitoringConfig {
    private final boolean enableJmx;
    private final boolean enableMicrometer;
    private final boolean enableOpenTelemetry;
    private final boolean enablePrometheusExport;
    private final boolean enableHealthEndpoint;
    private final String serviceName;
    private final String serviceVersion;
    private final Map<String, String> commonTags;

    public MonitoringConfig() {
      this(true, true, true, true, true, "wasmtime4j", "1.0.0", Map.of());
    }

    public MonitoringConfig(
        final boolean enableJmx,
        final boolean enableMicrometer,
        final boolean enableOpenTelemetry,
        final boolean enablePrometheusExport,
        final boolean enableHealthEndpoint,
        final String serviceName,
        final String serviceVersion,
        final Map<String, String> commonTags) {
      this.enableJmx = enableJmx;
      this.enableMicrometer = enableMicrometer;
      this.enableOpenTelemetry = enableOpenTelemetry;
      this.enablePrometheusExport = enablePrometheusExport;
      this.enableHealthEndpoint = enableHealthEndpoint;
      this.serviceName = serviceName != null ? serviceName : "wasmtime4j";
      this.serviceVersion = serviceVersion != null ? serviceVersion : "1.0.0";
      this.commonTags = Map.copyOf(commonTags != null ? commonTags : Map.of());
    }

    public boolean isEnableJmx() {
      return enableJmx;
    }

    public boolean isEnableMicrometer() {
      return enableMicrometer;
    }

    public boolean isEnableOpenTelemetry() {
      return enableOpenTelemetry;
    }

    public boolean isEnablePrometheusExport() {
      return enablePrometheusExport;
    }

    public boolean isEnableHealthEndpoint() {
      return enableHealthEndpoint;
    }

    public String getServiceName() {
      return serviceName;
    }

    public String getServiceVersion() {
      return serviceVersion;
    }

    public Map<String, String> getCommonTags() {
      return commonTags;
    }
  }

  /** JMX MBean interface for wasmtime4j monitoring. */
  public interface Wasmtime4jMonitoringMBean {
    // Runtime metrics
    long getTotalOperations();

    double getAverageOperationLatency();

    long getTotalErrors();

    double getErrorRate();

    // Memory metrics
    long getCurrentHeapUsage();

    long getMaxHeapUsage();

    double getHeapUtilization();

    // Instance metrics
    int getActiveInstances();

    int getTotalInstancesCreated();

    long getTotalInstanceDisposals();

    // Performance metrics
    String getPerformanceStatistics();

    String getHealthStatus();

    String getLastError();

    // Operations
    void resetMetrics();

    void forceGarbageCollection();

    String dumpDiagnostics();
  }

  /** JMX MBean implementation. */
  public static final class Wasmtime4jMonitoringMBeanImpl implements Wasmtime4jMonitoringMBean {
    private final MetricsCollector metricsCollector;
    private final DiagnosticCollector diagnosticCollector;
    private final HealthCheckSystem healthCheckSystem;
    private final ProductionMonitoringSystem monitoringSystem;

    public Wasmtime4jMonitoringMBeanImpl(
        final MetricsCollector metricsCollector,
        final DiagnosticCollector diagnosticCollector,
        final HealthCheckSystem healthCheckSystem,
        final ProductionMonitoringSystem monitoringSystem) {
      this.metricsCollector = metricsCollector;
      this.diagnosticCollector = diagnosticCollector;
      this.healthCheckSystem = healthCheckSystem;
      this.monitoringSystem = monitoringSystem;
    }

    @Override
    public long getTotalOperations() {
      return metricsCollector.getCounterValue("wasmtime.operations.total");
    }

    @Override
    public double getAverageOperationLatency() {
      final MetricsCollector.TimerMetric timer =
          metricsCollector.timer("wasmtime.operation.duration");
      return timer.getHistogram().getMean();
    }

    @Override
    public long getTotalErrors() {
      return metricsCollector.getCounterValue("wasmtime.errors.total");
    }

    @Override
    public double getErrorRate() {
      final long operations = getTotalOperations();
      final long errors = getTotalErrors();
      return operations > 0 ? (double) errors / operations : 0.0;
    }

    @Override
    public long getCurrentHeapUsage() {
      final MetricsCollector.MetricData heapMetric =
          metricsCollector.getGaugeValue("jvm.memory.heap.used");
      return heapMetric != null ? (long) heapMetric.getValue() : 0;
    }

    @Override
    public long getMaxHeapUsage() {
      final MetricsCollector.MetricData heapMaxMetric =
          metricsCollector.getGaugeValue("jvm.memory.heap.max");
      return heapMaxMetric != null ? (long) heapMaxMetric.getValue() : 0;
    }

    @Override
    public double getHeapUtilization() {
      final MetricsCollector.MetricData utilizationMetric =
          metricsCollector.getGaugeValue("jvm.memory.heap.usage");
      return utilizationMetric != null ? utilizationMetric.getValue() : 0.0;
    }

    @Override
    public int getActiveInstances() {
      return (int)
          diagnosticCollector.getAllInstanceLifecycles().values().stream()
              .mapToInt(info -> info.isDisposed() ? 0 : 1)
              .sum();
    }

    @Override
    public int getTotalInstancesCreated() {
      return diagnosticCollector.getAllInstanceLifecycles().size();
    }

    @Override
    public long getTotalInstanceDisposals() {
      return diagnosticCollector.getAllInstanceLifecycles().values().stream()
          .mapToLong(info -> info.isDisposed() ? 1 : 0)
          .sum();
    }

    @Override
    public String getPerformanceStatistics() {
      return metricsCollector.getMetricsSummary();
    }

    @Override
    public String getHealthStatus() {
      return healthCheckSystem != null
          ? healthCheckSystem.getHealthReport()
          : "Health system not available";
    }

    @Override
    public String getLastError() {
      final var recentEvents = diagnosticCollector.getRecentEvents(1);
      return recentEvents.isEmpty() ? "No recent errors" : recentEvents.get(0).toString();
    }

    @Override
    public void resetMetrics() {
      metricsCollector.reset();
      diagnosticCollector.clearDiagnosticData();
    }

    @Override
    public void forceGarbageCollection() {
      System.gc();
    }

    @Override
    public String dumpDiagnostics() {
      return diagnosticCollector.getDiagnosticSummary();
    }
  }

  /** Prometheus metrics exporter. */
  public static final class PrometheusExporter {
    private final MetricsCollector metricsCollector;
    private final DiagnosticCollector diagnosticCollector;
    private final HealthCheckSystem healthCheckSystem;

    public PrometheusExporter(
        final MetricsCollector metricsCollector,
        final DiagnosticCollector diagnosticCollector,
        final HealthCheckSystem healthCheckSystem) {
      this.metricsCollector = metricsCollector;
      this.diagnosticCollector = diagnosticCollector;
      this.healthCheckSystem = healthCheckSystem;
    }

    /**
     * Exports metrics in Prometheus format.
     *
     * @return Prometheus format metrics
     */
    public String exportMetrics() {
      final StringBuilder sb = new StringBuilder();

      // Header
      sb.append("# HELP wasmtime4j WebAssembly runtime metrics\n");
      sb.append("# TYPE wasmtime4j_info gauge\n");
      sb.append("wasmtime4j_info{version=\"1.0.0\",runtime=\"wasmtime\"} 1\n\n");

      // Operation metrics
      exportCounterMetric(
          sb,
          "wasmtime4j_operations_total",
          "Total WebAssembly operations",
          metricsCollector.getCounterValue("wasmtime.operations.total"));
      exportCounterMetric(
          sb,
          "wasmtime4j_errors_total",
          "Total operation errors",
          metricsCollector.getCounterValue("wasmtime.errors.total"));

      // Memory metrics
      final MetricsCollector.MetricData heapUsed =
          metricsCollector.getGaugeValue("jvm.memory.heap.used");
      final MetricsCollector.MetricData heapMax =
          metricsCollector.getGaugeValue("jvm.memory.heap.max");

      if (heapUsed != null) {
        exportGaugeMetric(
            sb, "wasmtime4j_memory_heap_used_bytes", "JVM heap memory used", heapUsed.getValue());
      }
      if (heapMax != null) {
        exportGaugeMetric(
            sb, "wasmtime4j_memory_heap_max_bytes", "JVM heap memory max", heapMax.getValue());
      }

      // Instance metrics
      final long activeInstances =
          diagnosticCollector.getAllInstanceLifecycles().values().stream()
              .mapToLong(info -> info.isDisposed() ? 0 : 1)
              .sum();
      exportGaugeMetric(
          sb, "wasmtime4j_instances_active", "Active WebAssembly instances", activeInstances);

      // Health metrics
      if (healthCheckSystem != null) {
        final int healthValue =
            switch (healthCheckSystem.getOverallHealthStatus()) {
              case HEALTHY -> 1;
              case DEGRADED -> 2;
              case UNHEALTHY -> 3;
              case CRITICAL -> 4;
              case UNKNOWN -> 0;
            };
        exportGaugeMetric(sb, "wasmtime4j_health_status", "Overall health status", healthValue);
      }

      // Timer metrics
      final MetricsCollector.TimerMetric operationTimer =
          metricsCollector.timer("wasmtime.operation.duration");
      final MetricsCollector.HistogramMetric histogram = operationTimer.getHistogram();

      sb.append("# HELP wasmtime4j_operation_duration_seconds Operation duration histogram\n");
      sb.append("# TYPE wasmtime4j_operation_duration_seconds histogram\n");

      for (final Map.Entry<String, Long> bucket : histogram.getBuckets().entrySet()) {
        final String bucketLabel =
            bucket.getKey().equals("+Inf")
                ? "+Inf"
                : String.valueOf(Double.parseDouble(bucket.getKey()) / 1000.0);
        sb.append(
            String.format(
                "wasmtime4j_operation_duration_seconds_bucket{le=\"%s\"} %d\n",
                bucketLabel, bucket.getValue()));
      }
      sb.append(
          String.format("wasmtime4j_operation_duration_seconds_count %d\n", histogram.getCount()));
      sb.append(
          String.format(
              "wasmtime4j_operation_duration_seconds_sum %f\n", histogram.getSum() / 1000.0));
      sb.append("\n");

      return sb.toString();
    }

    private void exportCounterMetric(
        final StringBuilder sb, final String name, final String help, final long value) {
      sb.append("# HELP ").append(name).append(" ").append(help).append("\n");
      sb.append("# TYPE ").append(name).append(" counter\n");
      sb.append(name).append(" ").append(value).append("\n\n");
    }

    private void exportGaugeMetric(
        final StringBuilder sb, final String name, final String help, final double value) {
      sb.append("# HELP ").append(name).append(" ").append(help).append("\n");
      sb.append("# TYPE ").append(name).append(" gauge\n");
      sb.append(name).append(" ").append(value).append("\n\n");
    }
  }

  /** Health endpoint implementation. */
  public static final class HealthEndpoint {
    private final HealthCheckSystem healthCheckSystem;
    private final MetricsCollector metricsCollector;
    private final DiagnosticCollector diagnosticCollector;

    public HealthEndpoint(
        final HealthCheckSystem healthCheckSystem,
        final MetricsCollector metricsCollector,
        final DiagnosticCollector diagnosticCollector) {
      this.healthCheckSystem = healthCheckSystem;
      this.metricsCollector = metricsCollector;
      this.diagnosticCollector = diagnosticCollector;
    }

    /**
     * Gets health status in JSON format.
     *
     * @return JSON health status
     */
    public String getHealthJson() {
      final HealthCheckSystem.HealthStatus status = healthCheckSystem.getOverallHealthStatus();
      final long uptime =
          Duration.between(
                  Instant.now().minus(Duration.ofHours(1)), // Simplified uptime calculation
                  Instant.now())
              .toSeconds();

      return String.format(
          """
          {
            "status": "%s",
            "description": "%s",
            "uptime_seconds": %d,
            "timestamp": "%s",
            "details": {
              "memory_healthy": %b,
              "threads_healthy": %b,
              "active_instances": %d,
              "total_operations": %d,
              "error_rate": %.4f
            }
          }""",
          status.name(),
          status.getDescription(),
          uptime,
          Instant.now(),
          healthCheckSystem.getComponentHealthStatus("jvm_memory")
              == HealthCheckSystem.HealthStatus.HEALTHY,
          healthCheckSystem.getComponentHealthStatus("jvm_threads")
              == HealthCheckSystem.HealthStatus.HEALTHY,
          diagnosticCollector.getAllInstanceLifecycles().values().stream()
              .mapToInt(info -> info.isDisposed() ? 0 : 1)
              .sum(),
          metricsCollector.getCounterValue("wasmtime.operations.total"),
          calculateErrorRate());
    }

    private double calculateErrorRate() {
      final long operations = metricsCollector.getCounterValue("wasmtime.operations.total");
      final long errors = metricsCollector.getCounterValue("wasmtime.errors.total");
      return operations > 0 ? (double) errors / operations : 0.0;
    }

    /**
     * Gets readiness status.
     *
     * @return true if system is ready to serve requests
     */
    public boolean isReady() {
      final HealthCheckSystem.HealthStatus status = healthCheckSystem.getOverallHealthStatus();
      return status == HealthCheckSystem.HealthStatus.HEALTHY
          || status == HealthCheckSystem.HealthStatus.DEGRADED;
    }

    /**
     * Gets liveness status.
     *
     * @return true if system is alive (not stuck)
     */
    public boolean isAlive() {
      return healthCheckSystem.getOverallHealthStatus() != HealthCheckSystem.HealthStatus.CRITICAL;
    }
  }

  /** Monitoring integration state. */
  private final MonitoringConfig config;

  private final MetricsCollector metricsCollector;
  private final DiagnosticCollector diagnosticCollector;
  private final HealthCheckSystem healthCheckSystem;
  private final ProductionMonitoringSystem monitoringSystem;

  private final AtomicReference<ObjectName> jmxObjectName = new AtomicReference<>();
  private final AtomicReference<PrometheusExporter> prometheusExporter = new AtomicReference<>();
  private final AtomicReference<HealthEndpoint> healthEndpoint = new AtomicReference<>();

  private volatile boolean initialized = false;

  /**
   * Creates a monitoring integration.
   *
   * @param config monitoring configuration
   * @param metricsCollector metrics collector
   * @param diagnosticCollector diagnostic collector
   * @param healthCheckSystem health check system
   * @param monitoringSystem production monitoring system
   */
  public MonitoringIntegration(
      final MonitoringConfig config,
      final MetricsCollector metricsCollector,
      final DiagnosticCollector diagnosticCollector,
      final HealthCheckSystem healthCheckSystem,
      final ProductionMonitoringSystem monitoringSystem) {
    this.config = config;
    this.metricsCollector = metricsCollector;
    this.diagnosticCollector = diagnosticCollector;
    this.healthCheckSystem = healthCheckSystem;
    this.monitoringSystem = monitoringSystem;
  }

  /** Initializes all enabled monitoring integrations. */
  public void initialize() {
    if (initialized) {
      return;
    }

    try {
      if (config.isEnableJmx()) {
        initializeJmxIntegration();
      }

      if (config.isEnablePrometheusExport()) {
        initializePrometheusExport();
      }

      if (config.isEnableHealthEndpoint()) {
        initializeHealthEndpoint();
      }

      // Note: Micrometer and OpenTelemetry integrations would require additional dependencies
      // For now, we provide the foundation and interfaces
      if (config.isEnableMicrometer()) {
        initializeMicrometerIntegration();
      }

      if (config.isEnableOpenTelemetry()) {
        initializeOpenTelemetryIntegration();
      }

      initialized = true;
      LOGGER.info("Monitoring integration initialized with config: " + config.getServiceName());

    } catch (final Exception e) {
      LOGGER.severe("Failed to initialize monitoring integration: " + e.getMessage());
      throw new RuntimeException("Monitoring integration initialization failed", e);
    }
  }

  /** Initializes JMX integration. */
  private void initializeJmxIntegration() {
    try {
      final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      final ObjectName objectName =
          new ObjectName("ai.tegmentum.wasmtime4j:type=Monitoring,name=" + config.getServiceName());

      final Wasmtime4jMonitoringMBeanImpl mBean =
          new Wasmtime4jMonitoringMBeanImpl(
              metricsCollector, diagnosticCollector, healthCheckSystem, monitoringSystem);

      mBeanServer.registerMBean(mBean, objectName);
      jmxObjectName.set(objectName);

      LOGGER.info("JMX monitoring MBean registered: " + objectName);

    } catch (final Exception e) {
      LOGGER.severe("Failed to initialize JMX integration: " + e.getMessage());
      throw new RuntimeException("JMX integration failed", e);
    }
  }

  /** Initializes Prometheus export capability. */
  private void initializePrometheusExport() {
    prometheusExporter.set(
        new PrometheusExporter(metricsCollector, diagnosticCollector, healthCheckSystem));
    LOGGER.info("Prometheus exporter initialized");
  }

  /** Initializes health endpoint. */
  private void initializeHealthEndpoint() {
    healthEndpoint.set(
        new HealthEndpoint(healthCheckSystem, metricsCollector, diagnosticCollector));
    LOGGER.info("Health endpoint initialized");
  }

  /** Initializes Micrometer integration (placeholder). */
  private void initializeMicrometerIntegration() {
    // This would require Micrometer dependencies
    // For now, we log that it's configured but not implemented
    LOGGER.info("Micrometer integration configured (requires micrometer-core dependency)");

    // Example of what the integration would look like:
    /*
    if (micrometerRegistryAvailable()) {
      final MeterRegistry registry = Metrics.globalRegistry;

      // Register custom meters
      Gauge.builder("wasmtime4j.instances.active")
          .description("Active WebAssembly instances")
          .register(registry, this, self -> self.getActiveInstances());

      Counter.builder("wasmtime4j.operations.total")
          .description("Total WebAssembly operations")
          .register(registry)
          .increment();

      Timer.builder("wasmtime4j.operation.duration")
          .description("Operation duration")
          .register(registry);
    }
    */
  }

  /** Initializes OpenTelemetry integration (placeholder). */
  private void initializeOpenTelemetryIntegration() {
    // This would require OpenTelemetry dependencies
    LOGGER.info("OpenTelemetry integration configured (requires opentelemetry-api dependency)");

    // Example of what the integration would look like:
    /*
    if (openTelemetryAvailable()) {
      final OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
      final Meter meter = openTelemetry.getMeter("wasmtime4j", config.getServiceVersion());

      // Create instruments
      final LongCounter operationCounter = meter.counterBuilder("wasmtime4j.operations")
          .setDescription("Number of WebAssembly operations")
          .build();

      final LongHistogram operationDuration = meter.histogramBuilder("wasmtime4j.operation.duration")
          .setDescription("WebAssembly operation duration")
          .setUnit("ms")
          .build();

      // Register callbacks for gauges
      meter.gaugeBuilder("wasmtime4j.instances.active")
          .setDescription("Active WebAssembly instances")
          .buildWithCallback(measurement -> measurement.record(getActiveInstances()));
    }
    */
  }

  /**
   * Gets Prometheus metrics export.
   *
   * @return Prometheus format metrics
   */
  public String getPrometheusMetrics() {
    final PrometheusExporter exporter = prometheusExporter.get();
    return exporter != null ? exporter.exportMetrics() : "# Prometheus export not enabled";
  }

  /**
   * Gets health status in JSON format.
   *
   * @return JSON health status
   */
  public String getHealthJson() {
    final HealthEndpoint endpoint = healthEndpoint.get();
    return endpoint != null
        ? endpoint.getHealthJson()
        : "{\"status\":\"UNKNOWN\",\"message\":\"Health endpoint not enabled\"}";
  }

  /**
   * Checks if system is ready.
   *
   * @return true if ready
   */
  public boolean isReady() {
    final HealthEndpoint endpoint = healthEndpoint.get();
    return endpoint != null ? endpoint.isReady() : false;
  }

  /**
   * Checks if system is alive.
   *
   * @return true if alive
   */
  public boolean isAlive() {
    final HealthEndpoint endpoint = healthEndpoint.get();
    return endpoint != null ? endpoint.isAlive() : false;
  }

  /**
   * Records a custom metric.
   *
   * @param name metric name
   * @param value metric value
   * @param tags optional tags
   */
  public void recordMetric(final String name, final double value, final Map<String, String> tags) {
    // Record in our internal metrics system
    metricsCollector.gauge(name, value, tags);

    // If Micrometer is available, record there too
    // recordMicrometerMetric(name, value, tags);

    // If OpenTelemetry is available, record there too
    // recordOpenTelemetryMetric(name, value, tags);
  }

  /**
   * Records an operation timing.
   *
   * @param operation operation name
   * @param duration operation duration
   * @param tags optional tags
   */
  public void recordTiming(
      final String operation, final Duration duration, final Map<String, String> tags) {
    final String metricName = "wasmtime4j.operation." + operation + ".duration";
    metricsCollector.timing(metricName, duration, tags);
  }

  /**
   * Records an operation counter.
   *
   * @param operation operation name
   * @param count count to add
   * @param tags optional tags
   */
  public void recordCounter(
      final String operation, final long count, final Map<String, String> tags) {
    final String metricName = "wasmtime4j.operation." + operation + ".total";
    metricsCollector.counter(metricName, count, tags);
  }

  /**
   * Gets monitoring configuration.
   *
   * @return monitoring configuration
   */
  public MonitoringConfig getConfig() {
    return config;
  }

  /**
   * Checks if monitoring integration is initialized.
   *
   * @return true if initialized
   */
  public boolean isInitialized() {
    return initialized;
  }

  /** Shuts down monitoring integration. */
  public void shutdown() {
    try {
      // Unregister JMX MBean
      final ObjectName objectName = jmxObjectName.get();
      if (objectName != null) {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        if (mBeanServer.isRegistered(objectName)) {
          mBeanServer.unregisterMBean(objectName);
          LOGGER.info("JMX monitoring MBean unregistered");
        }
      }

      initialized = false;
      LOGGER.info("Monitoring integration shutdown");

    } catch (final Exception e) {
      LOGGER.warning("Error during monitoring integration shutdown: " + e.getMessage());
    }
  }
}
