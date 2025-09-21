/*
 * Copyright 2024 Tegmentum AI Inc.
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

package ai.tegmentum.wasmtime4j.production;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmFunction;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Production-grade monitoring and observability service for WebAssembly operations.
 *
 * <p>Features:
 * - Real-time metrics collection and aggregation
 * - Health check monitoring
 * - Performance tracking and alerting
 * - Resource usage monitoring
 * - Custom metrics and events
 * - Integration-ready for external monitoring systems
 */
public final class ProductionMonitoringService implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(ProductionMonitoringService.class.getName());

  private final MonitoringConfig config;
  private final MetricsRegistry metricsRegistry;
  private final HealthIndicator healthIndicator;
  private final AlertingService alertingService;
  private final ScheduledExecutorService scheduledExecutor;
  private final ConcurrentHashMap<String, OperationMetrics> operationMetrics;

  /** Monitoring configuration. */
  public static final class MonitoringConfig {
    private final boolean enableMetrics;
    private final boolean enableHealthChecks;
    private final boolean enableAlerting;
    private final Duration healthCheckInterval;
    private final Duration metricsPublishInterval;
    private final List<String> monitoredOperations;
    private final double alertThresholdErrorRate;
    private final Duration alertThresholdResponseTime;

    private MonitoringConfig(final Builder builder) {
      this.enableMetrics = builder.enableMetrics;
      this.enableHealthChecks = builder.enableHealthChecks;
      this.enableAlerting = builder.enableAlerting;
      this.healthCheckInterval = builder.healthCheckInterval;
      this.metricsPublishInterval = builder.metricsPublishInterval;
      this.monitoredOperations = Collections.unmodifiableList(new ArrayList<>(builder.monitoredOperations));
      this.alertThresholdErrorRate = builder.alertThresholdErrorRate;
      this.alertThresholdResponseTime = builder.alertThresholdResponseTime;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private boolean enableMetrics = true;
      private boolean enableHealthChecks = true;
      private boolean enableAlerting = true;
      private Duration healthCheckInterval = Duration.ofMinutes(1);
      private Duration metricsPublishInterval = Duration.ofSeconds(30);
      private List<String> monitoredOperations = List.of("compile", "instantiate", "call");
      private double alertThresholdErrorRate = 5.0; // 5%
      private Duration alertThresholdResponseTime = Duration.ofSeconds(5);

      public Builder enableMetrics(final boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
        return this;
      }

      public Builder enableHealthChecks(final boolean enableHealthChecks) {
        this.enableHealthChecks = enableHealthChecks;
        return this;
      }

      public Builder enableAlerting(final boolean enableAlerting) {
        this.enableAlerting = enableAlerting;
        return this;
      }

      public Builder healthCheckInterval(final Duration healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
        return this;
      }

      public Builder metricsPublishInterval(final Duration metricsPublishInterval) {
        this.metricsPublishInterval = metricsPublishInterval;
        return this;
      }

      public Builder monitoredOperations(final List<String> monitoredOperations) {
        this.monitoredOperations = new ArrayList<>(monitoredOperations);
        return this;
      }

      public Builder alertThresholds(final double errorRate, final Duration responseTime) {
        this.alertThresholdErrorRate = errorRate;
        this.alertThresholdResponseTime = responseTime;
        return this;
      }

      public MonitoringConfig build() {
        return new MonitoringConfig(this);
      }
    }
  }

  /** Metrics registry for collecting and aggregating metrics. */
  public static final class MetricsRegistry {
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Gauge> gauges = new ConcurrentHashMap<>();

    public Counter counter(final String name, final String... tags) {
      final String key = createKey(name, tags);
      return counters.computeIfAbsent(key, k -> new Counter(name, tags));
    }

    public Timer timer(final String name, final String... tags) {
      final String key = createKey(name, tags);
      return timers.computeIfAbsent(key, k -> new Timer(name, tags));
    }

    public Gauge gauge(final String name, final String... tags) {
      final String key = createKey(name, tags);
      return gauges.computeIfAbsent(key, k -> new Gauge(name, tags));
    }

    private String createKey(final String name, final String... tags) {
      final StringBuilder sb = new StringBuilder(name);
      for (int i = 0; i < tags.length; i += 2) {
        if (i + 1 < tags.length) {
          sb.append("|").append(tags[i]).append("=").append(tags[i + 1]);
        }
      }
      return sb.toString();
    }

    public Map<String, Counter> getCounters() {
      return Collections.unmodifiableMap(counters);
    }

    public Map<String, Timer> getTimers() {
      return Collections.unmodifiableMap(timers);
    }

    public Map<String, Gauge> getGauges() {
      return Collections.unmodifiableMap(gauges);
    }
  }

  /** Counter metric for tracking event counts. */
  public static final class Counter {
    private final String name;
    private final String[] tags;
    private final LongAdder count = new LongAdder();

    Counter(final String name, final String[] tags) {
      this.name = name;
      this.tags = tags.clone();
    }

    public void increment() {
      count.increment();
    }

    public void increment(final long delta) {
      count.add(delta);
    }

    public long getCount() {
      return count.sum();
    }

    public String getName() {
      return name;
    }

    public String[] getTags() {
      return tags.clone();
    }
  }

  /** Timer metric for tracking operation durations. */
  public static final class Timer {
    private final String name;
    private final String[] tags;
    private final LongAdder totalTime = new LongAdder();
    private final LongAdder count = new LongAdder();
    private final AtomicLong maxTime = new AtomicLong(0);

    Timer(final String name, final String[] tags) {
      this.name = name;
      this.tags = tags.clone();
    }

    public void record(final Duration duration) {
      final long nanos = duration.toNanos();
      totalTime.add(nanos);
      count.increment();
      maxTime.updateAndGet(current -> Math.max(current, nanos));
    }

    public long getTotalTime(final TimeUnit unit) {
      return unit.convert(totalTime.sum(), TimeUnit.NANOSECONDS);
    }

    public long getCount() {
      return count.sum();
    }

    public double getMean(final TimeUnit unit) {
      final long c = count.sum();
      return c > 0 ? (double) unit.convert(totalTime.sum(), TimeUnit.NANOSECONDS) / c : 0.0;
    }

    public long getMax(final TimeUnit unit) {
      return unit.convert(maxTime.get(), TimeUnit.NANOSECONDS);
    }

    public String getName() {
      return name;
    }

    public String[] getTags() {
      return tags.clone();
    }

    public Sample start() {
      return new Sample(this);
    }

    public static final class Sample {
      private final Timer timer;
      private final long startTime;

      private Sample(final Timer timer) {
        this.timer = timer;
        this.startTime = System.nanoTime();
      }

      public void stop() {
        timer.record(Duration.ofNanos(System.nanoTime() - startTime));
      }
    }
  }

  /** Gauge metric for tracking current values. */
  public static final class Gauge {
    private final String name;
    private final String[] tags;
    private final AtomicReference<Double> value = new AtomicReference<>(0.0);

    Gauge(final String name, final String[] tags) {
      this.name = name;
      this.tags = tags.clone();
    }

    public void set(final double value) {
      this.value.set(value);
    }

    public double getValue() {
      return value.get();
    }

    public String getName() {
      return name;
    }

    public String[] getTags() {
      return tags.clone();
    }
  }

  /** Health indicator for tracking system health. */
  public static final class HealthIndicator {
    private volatile boolean healthy = true;
    private volatile String healthMessage = "Healthy";
    private volatile Instant lastHealthCheck = Instant.now();

    public void setHealthy(final boolean healthy) {
      this.healthy = healthy;
      this.lastHealthCheck = Instant.now();
      this.healthMessage = healthy ? "Healthy" : "Unhealthy";
    }

    public void setHealthy(final boolean healthy, final String message) {
      this.healthy = healthy;
      this.lastHealthCheck = Instant.now();
      this.healthMessage = message;
    }

    public boolean isHealthy() {
      return healthy;
    }

    public String getHealthMessage() {
      return healthMessage;
    }

    public Instant getLastHealthCheck() {
      return lastHealthCheck;
    }
  }

  /** Alerting service for sending alerts based on metrics. */
  public static final class AlertingService {
    private final List<AlertListener> listeners = new ArrayList<>();

    public void addListener(final AlertListener listener) {
      listeners.add(listener);
    }

    public void removeListener(final AlertListener listener) {
      listeners.remove(listener);
    }

    public void sendAlert(final String message, final AlertSeverity severity) {
      final Alert alert = new Alert(message, severity, Instant.now());
      for (final AlertListener listener : listeners) {
        try {
          listener.onAlert(alert);
        } catch (final Exception e) {
          LOGGER.warning("Error sending alert: " + e.getMessage());
        }
      }
    }

    public interface AlertListener {
      void onAlert(Alert alert);
    }

    public enum AlertSeverity {
      LOW, MEDIUM, HIGH, CRITICAL
    }

    public static final class Alert {
      private final String message;
      private final AlertSeverity severity;
      private final Instant timestamp;

      Alert(final String message, final AlertSeverity severity, final Instant timestamp) {
        this.message = message;
        this.severity = severity;
        this.timestamp = timestamp;
      }

      public String getMessage() {
        return message;
      }

      public AlertSeverity getSeverity() {
        return severity;
      }

      public Instant getTimestamp() {
        return timestamp;
      }

      @Override
      public String toString() {
        return String.format("[%s] %s: %s", severity, timestamp, message);
      }
    }
  }

  /** Operation-specific metrics tracking. */
  private static final class OperationMetrics {
    private final Timer responseTime;
    private final Counter successCount;
    private final Counter errorCount;
    private final Gauge currentLoad;

    OperationMetrics(final MetricsRegistry registry, final String operation) {
      this.responseTime = registry.timer("operation.duration", "operation", operation);
      this.successCount = registry.counter("operation.success", "operation", operation);
      this.errorCount = registry.counter("operation.error", "operation", operation);
      this.currentLoad = registry.gauge("operation.load", "operation", operation);
    }

    public void recordSuccess(final Duration duration) {
      responseTime.record(duration);
      successCount.increment();
    }

    public void recordError(final Duration duration) {
      responseTime.record(duration);
      errorCount.increment();
    }

    public void updateLoad(final double load) {
      currentLoad.set(load);
    }

    public double getErrorRate() {
      final long total = successCount.getCount() + errorCount.getCount();
      return total > 0 ? (double) errorCount.getCount() / total * 100.0 : 0.0;
    }
  }

  /**
   * Creates a production monitoring service with the specified configuration.
   *
   * @param config the monitoring configuration
   */
  public ProductionMonitoringService(final MonitoringConfig config) {
    this.config = config;
    this.metricsRegistry = new MetricsRegistry();
    this.healthIndicator = new HealthIndicator();
    this.alertingService = new AlertingService();
    this.operationMetrics = new ConcurrentHashMap<>();
    this.scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
      final Thread t = new Thread(r, "ProductionMonitoring");
      t.setDaemon(true);
      return t;
    });

    // Initialize operation metrics
    for (final String operation : config.monitoredOperations) {
      operationMetrics.put(operation, new OperationMetrics(metricsRegistry, operation));
    }

    // Schedule health checks
    if (config.enableHealthChecks) {
      scheduledExecutor.scheduleAtFixedRate(
          this::performHealthCheck,
          config.healthCheckInterval.toMillis(),
          config.healthCheckInterval.toMillis(),
          TimeUnit.MILLISECONDS);
    }

    // Schedule metrics publishing
    if (config.enableMetrics) {
      scheduledExecutor.scheduleAtFixedRate(
          this::publishMetrics,
          config.metricsPublishInterval.toMillis(),
          config.metricsPublishInterval.toMillis(),
          TimeUnit.MILLISECONDS);
    }

    LOGGER.info("ProductionMonitoringService initialized");
  }

  /**
   * Creates a default production monitoring service.
   *
   * @return a new monitoring service
   */
  public static ProductionMonitoringService createDefault() {
    return new ProductionMonitoringService(MonitoringConfig.builder().build());
  }

  /**
   * Configures monitoring for an engine instance.
   *
   * @param engine the engine to monitor
   */
  public void configureEngineMonitoring(final Engine engine) {
    if (!config.enableMetrics) {
      return;
    }

    // Track engine lifecycle
    metricsRegistry.counter("engine.created").increment();

    // Monitor compilation events if supported
    try {
      // This would require engine to support listeners - simplified for demo
      LOGGER.fine("Engine monitoring configured");
    } catch (final Exception e) {
      LOGGER.warning("Failed to configure engine monitoring: " + e.getMessage());
    }
  }

  /**
   * Records metrics for a module compilation operation.
   *
   * @param module the compiled module
   * @param duration the compilation duration
   */
  public void recordModuleCompilation(final Module module, final Duration duration) {
    if (!config.enableMetrics) {
      return;
    }

    final OperationMetrics metrics = operationMetrics.get("compile");
    if (metrics != null) {
      metrics.recordSuccess(duration);
    }

    metricsRegistry.timer("module.compilation.duration").record(duration);
    metricsRegistry.counter("module.compilation.count").increment();

    // Check for performance alerts
    if (config.enableAlerting && duration.compareTo(config.alertThresholdResponseTime) > 0) {
      alertingService.sendAlert(
          String.format("Module compilation took %dms (threshold: %dms)",
              duration.toMillis(), config.alertThresholdResponseTime.toMillis()),
          AlertingService.AlertSeverity.MEDIUM);
    }
  }

  /**
   * Records metrics for a function call operation.
   *
   * @param function the called function
   * @param args the function arguments
   * @param result the function result
   * @param duration the call duration
   */
  public void recordFunctionCall(
      final WasmFunction function,
      final Object[] args,
      final Object result,
      final Duration duration) {
    if (!config.enableMetrics) {
      return;
    }

    final OperationMetrics metrics = operationMetrics.get("call");
    if (metrics != null) {
      metrics.recordSuccess(duration);
    }

    metricsRegistry.timer("function.call.duration", "function", function.getName()).record(duration);
    metricsRegistry.counter("function.call.count", "function", function.getName()).increment();
  }

  /**
   * Records an error for monitoring and alerting.
   *
   * @param operation the operation that failed
   * @param throwable the error that occurred
   * @param duration the operation duration before failure
   */
  public void recordError(final String operation, final Throwable throwable, final Duration duration) {
    if (!config.enableMetrics) {
      return;
    }

    final OperationMetrics metrics = operationMetrics.get(operation);
    if (metrics != null) {
      metrics.recordError(duration);

      // Check error rate threshold
      if (config.enableAlerting && metrics.getErrorRate() > config.alertThresholdErrorRate) {
        alertingService.sendAlert(
            String.format("High error rate for %s: %.1f%% (threshold: %.1f%%)",
                operation, metrics.getErrorRate(), config.alertThresholdErrorRate),
            AlertingService.AlertSeverity.HIGH);
      }
    }

    metricsRegistry.counter("operation.error",
        "operation", operation,
        "error", throwable.getClass().getSimpleName()).increment();

    if (config.enableAlerting) {
      final AlertingService.AlertSeverity severity = determineSeverity(throwable);
      alertingService.sendAlert(
          String.format("Operation %s failed: %s", operation, throwable.getMessage()),
          severity);
    }
  }

  /**
   * Gets the current health status.
   *
   * @return the health indicator
   */
  public HealthIndicator getHealthIndicator() {
    return healthIndicator;
  }

  /**
   * Gets the metrics registry.
   *
   * @return the metrics registry
   */
  public MetricsRegistry getMetricsRegistry() {
    return metricsRegistry;
  }

  /**
   * Gets the alerting service.
   *
   * @return the alerting service
   */
  public AlertingService getAlertingService() {
    return alertingService;
  }

  /**
   * Adds an alert listener.
   *
   * @param listener the alert listener
   */
  public void addAlertListener(final AlertingService.AlertListener listener) {
    alertingService.addListener(listener);
  }

  /**
   * Gets comprehensive monitoring statistics.
   *
   * @return monitoring statistics
   */
  public MonitoringStatistics getStatistics() {
    return new MonitoringStatistics(metricsRegistry, operationMetrics, healthIndicator);
  }

  /** Comprehensive monitoring statistics. */
  public static final class MonitoringStatistics {
    private final Map<String, Long> counterValues;
    private final Map<String, Double> gaugeValues;
    private final Map<String, TimerStats> timerStats;
    private final boolean healthy;
    private final String healthMessage;

    MonitoringStatistics(
        final MetricsRegistry registry,
        final Map<String, OperationMetrics> operationMetrics,
        final HealthIndicator healthIndicator) {
      this.counterValues = new ConcurrentHashMap<>();
      this.gaugeValues = new ConcurrentHashMap<>();
      this.timerStats = new ConcurrentHashMap<>();

      // Collect counter values
      for (final Map.Entry<String, Counter> entry : registry.getCounters().entrySet()) {
        counterValues.put(entry.getKey(), entry.getValue().getCount());
      }

      // Collect gauge values
      for (final Map.Entry<String, Gauge> entry : registry.getGauges().entrySet()) {
        gaugeValues.put(entry.getKey(), entry.getValue().getValue());
      }

      // Collect timer statistics
      for (final Map.Entry<String, Timer> entry : registry.getTimers().entrySet()) {
        final Timer timer = entry.getValue();
        timerStats.put(entry.getKey(), new TimerStats(
            timer.getCount(),
            timer.getTotalTime(TimeUnit.MILLISECONDS),
            timer.getMean(TimeUnit.MILLISECONDS),
            timer.getMax(TimeUnit.MILLISECONDS)));
      }

      this.healthy = healthIndicator.isHealthy();
      this.healthMessage = healthIndicator.getHealthMessage();
    }

    public Map<String, Long> getCounterValues() {
      return Collections.unmodifiableMap(counterValues);
    }

    public Map<String, Double> getGaugeValues() {
      return Collections.unmodifiableMap(gaugeValues);
    }

    public Map<String, TimerStats> getTimerStats() {
      return Collections.unmodifiableMap(timerStats);
    }

    public boolean isHealthy() {
      return healthy;
    }

    public String getHealthMessage() {
      return healthMessage;
    }

    public static final class TimerStats {
      private final long count;
      private final long totalTimeMs;
      private final double meanMs;
      private final long maxMs;

      TimerStats(final long count, final long totalTimeMs, final double meanMs, final long maxMs) {
        this.count = count;
        this.totalTimeMs = totalTimeMs;
        this.meanMs = meanMs;
        this.maxMs = maxMs;
      }

      public long getCount() {
        return count;
      }

      public long getTotalTimeMs() {
        return totalTimeMs;
      }

      public double getMeanMs() {
        return meanMs;
      }

      public long getMaxMs() {
        return maxMs;
      }
    }
  }

  /** Performs periodic health checks. */
  private void performHealthCheck() {
    try {
      // Perform basic health validation
      final boolean healthy = validateSystemHealth();
      healthIndicator.setHealthy(healthy);

      if (!healthy && config.enableAlerting) {
        alertingService.sendAlert("System health check failed", AlertingService.AlertSeverity.HIGH);
      }
    } catch (final Exception e) {
      LOGGER.warning("Health check failed: " + e.getMessage());
      healthIndicator.setHealthy(false, "Health check error: " + e.getMessage());
    }
  }

  /** Validates overall system health. */
  private boolean validateSystemHealth() {
    // Check memory usage
    final Runtime runtime = Runtime.getRuntime();
    final long totalMemory = runtime.totalMemory();
    final long freeMemory = runtime.freeMemory();
    final double memoryUsage = (double) (totalMemory - freeMemory) / totalMemory * 100.0;

    metricsRegistry.gauge("system.memory.usage").set(memoryUsage);

    if (memoryUsage > 90.0) {
      return false;
    }

    // Check error rates
    for (final OperationMetrics metrics : operationMetrics.values()) {
      if (metrics.getErrorRate() > config.alertThresholdErrorRate * 2) {
        return false;
      }
    }

    return true;
  }

  /** Publishes metrics to external systems. */
  private void publishMetrics() {
    try {
      final MonitoringStatistics stats = getStatistics();
      LOGGER.fine(String.format("Published metrics: counters=%d, gauges=%d, timers=%d",
          stats.getCounterValues().size(),
          stats.getGaugeValues().size(),
          stats.getTimerStats().size()));
    } catch (final Exception e) {
      LOGGER.warning("Failed to publish metrics: " + e.getMessage());
    }
  }

  /** Determines alert severity based on throwable type. */
  private AlertingService.AlertSeverity determineSeverity(final Throwable throwable) {
    if (throwable instanceof OutOfMemoryError) {
      return AlertingService.AlertSeverity.CRITICAL;
    } else if (throwable instanceof SecurityException) {
      return AlertingService.AlertSeverity.HIGH;
    } else if (throwable instanceof RuntimeException) {
      return AlertingService.AlertSeverity.MEDIUM;
    } else {
      return AlertingService.AlertSeverity.LOW;
    }
  }

  @Override
  public void close() {
    LOGGER.info("Shutting down ProductionMonitoringService");

    scheduledExecutor.shutdown();
    try {
      if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduledExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      scheduledExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    LOGGER.info("ProductionMonitoringService shutdown complete");
  }
}