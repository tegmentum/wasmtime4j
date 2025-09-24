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
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Health check integration for observability system.
 *
 * <p>This class provides comprehensive health monitoring for the OpenTelemetry observability
 * system, ensuring that telemetry data is being properly collected, processed, and exported.
 *
 * @since 1.0.0
 */
public final class ObservabilityHealthCheck {

  private static final Logger LOGGER = Logger.getLogger(ObservabilityHealthCheck.class.getName());

  /** Health check status enumeration. */
  public enum HealthStatus {
    HEALTHY(1),
    DEGRADED(0),
    UNHEALTHY(-1);

    private final int value;

    HealthStatus(final int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  /** Health check result with details. */
  public static final class HealthCheckResult {
    private final HealthStatus status;
    private final String message;
    private final Instant timestamp;
    private final Duration checkDuration;
    private final String details;

    public HealthCheckResult(final HealthStatus status,
                           final String message,
                           final Duration checkDuration,
                           final String details) {
      this.status = status;
      this.message = message;
      this.timestamp = Instant.now();
      this.checkDuration = checkDuration;
      this.details = details;
    }

    public HealthStatus getStatus() {
      return status;
    }

    public String getMessage() {
      return message;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public Duration getCheckDuration() {
      return checkDuration;
    }

    public String getDetails() {
      return details;
    }

    @Override
    public String toString() {
      return String.format("HealthCheck{status=%s, message='%s', duration=%dms, timestamp=%s}",
          status, message, checkDuration.toMillis(), timestamp);
    }
  }

  /** Observability manager reference. */
  private final OpenTelemetryObservabilityManager manager;

  /** Health check scheduler. */
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  /** Current health status. */
  private final AtomicReference<HealthStatus> currentStatus =
      new AtomicReference<>(HealthStatus.HEALTHY);

  /** Last health check result. */
  private final AtomicReference<HealthCheckResult> lastResult = new AtomicReference<>();

  /** Health check metrics. */
  private final AtomicLong healthCheckCount = new AtomicLong(0);
  private final AtomicLong healthCheckFailures = new AtomicLong(0);
  private final LongGauge healthStatusGauge;

  /** Health check configuration. */
  private volatile Duration checkInterval = Duration.ofMinutes(1);
  private volatile Duration checkTimeout = Duration.ofSeconds(30);
  private volatile boolean enabled = true;

  /**
   * Creates a new observability health check.
   *
   * @param manager observability manager to monitor
   * @param meter metrics meter for health metrics
   */
  public ObservabilityHealthCheck(final OpenTelemetryObservabilityManager manager,
                                 final Meter meter) {
    this.manager = manager;

    // Initialize health metrics
    this.healthStatusGauge = meter
        .gaugeBuilder("observability_health_status")
        .setDescription("Observability system health status (1=healthy, 0=degraded, -1=unhealthy)")
        .setUnit("1")
        .ofLongs()
        .buildWithCallback(measurement ->
            measurement.record(currentStatus.get().getValue()));

    // Start periodic health checks
    startPeriodicHealthChecks();

    LOGGER.info("Observability health check initialized");
  }

  /** Starts periodic health checks. */
  private void startPeriodicHealthChecks() {
    scheduler.scheduleWithFixedDelay(
        this::performHealthCheck,
        checkInterval.toSeconds(),
        checkInterval.toSeconds(),
        TimeUnit.SECONDS);
  }

  /**
   * Performs a comprehensive health check of the observability system.
   *
   * @return health check result
   */
  public HealthCheckResult performHealthCheck() {
    if (!enabled) {
      return new HealthCheckResult(HealthStatus.DEGRADED, "Health checks disabled",
                                  Duration.ZERO, "Health checking has been disabled");
    }

    final Instant startTime = Instant.now();
    healthCheckCount.incrementAndGet();

    try {
      final StringBuilder details = new StringBuilder();
      HealthStatus overallStatus = HealthStatus.HEALTHY;
      String message = "Observability system is healthy";

      // Check if manager is initialized
      if (!manager.isInitialized()) {
        overallStatus = HealthStatus.UNHEALTHY;
        message = "Observability manager not initialized";
        details.append("Manager initialization: FAILED\n");
      } else {
        details.append("Manager initialization: OK\n");
      }

      // Check if manager is shut down
      if (manager.isShutdown()) {
        overallStatus = HealthStatus.UNHEALTHY;
        message = "Observability manager has been shut down";
        details.append("Manager status: SHUTDOWN\n");
      } else {
        details.append("Manager status: ACTIVE\n");
      }

      // Check provider availability
      try {
        final boolean tracerProviderOk = manager.getTracerProvider() != null;
        final boolean meterProviderOk = manager.getMeterProvider() != null;
        final boolean loggerProviderOk = manager.getLoggerProvider() != null;

        details.append("Tracer provider: ").append(tracerProviderOk ? "OK" : "FAILED").append("\n");
        details.append("Meter provider: ").append(meterProviderOk ? "OK" : "FAILED").append("\n");
        details.append("Logger provider: ").append(loggerProviderOk ? "OK" : "FAILED").append("\n");

        if (!tracerProviderOk || !meterProviderOk || !loggerProviderOk) {
          overallStatus = HealthStatus.DEGRADED;
          message = "Some observability providers are not available";
        }
      } catch (final Exception e) {
        overallStatus = HealthStatus.UNHEALTHY;
        message = "Error accessing observability providers: " + e.getMessage();
        details.append("Provider access: ERROR - ").append(e.getMessage()).append("\n");
      }

      // Check exporter configuration
      try {
        final int spanExporters = manager.getConfiguration().getSpanExporters().size();
        final int metricExporters = manager.getConfiguration().getMetricExporters().size();
        final int logExporters = manager.getConfiguration().getLogExporters().size();

        details.append("Span exporters: ").append(spanExporters).append("\n");
        details.append("Metric exporters: ").append(metricExporters).append("\n");
        details.append("Log exporters: ").append(logExporters).append("\n");

        if (spanExporters == 0 && metricExporters == 0 && logExporters == 0) {
          overallStatus = HealthStatus.DEGRADED;
          message = "No observability exporters configured";
          details.append("Export capability: DEGRADED - No exporters configured\n");
        } else {
          details.append("Export capability: OK\n");
        }
      } catch (final Exception e) {
        overallStatus = HealthStatus.DEGRADED;
        message = "Error checking exporter configuration: " + e.getMessage();
        details.append("Exporter check: ERROR - ").append(e.getMessage()).append("\n");
      }

      // Check resource detection
      try {
        final boolean resourceDetectionEnabled = manager.getConfiguration().isResourceDetectionEnabled();
        details.append("Resource detection: ").append(resourceDetectionEnabled ? "ENABLED" : "DISABLED").append("\n");
      } catch (final Exception e) {
        details.append("Resource detection check: ERROR - ").append(e.getMessage()).append("\n");
      }

      // Performance health check
      final Duration checkDuration = Duration.between(startTime, Instant.now());
      if (checkDuration.compareTo(checkTimeout) > 0) {
        overallStatus = HealthStatus.DEGRADED;
        message = "Health check took too long: " + checkDuration.toMillis() + "ms";
        details.append("Performance: SLOW - Check took ").append(checkDuration.toMillis()).append("ms\n");
      } else {
        details.append("Performance: OK - Check took ").append(checkDuration.toMillis()).append("ms\n");
      }

      final HealthCheckResult result = new HealthCheckResult(overallStatus, message,
                                                           checkDuration, details.toString());

      // Update current status
      currentStatus.set(overallStatus);
      lastResult.set(result);

      // Log health status changes
      final HealthStatus previousStatus = currentStatus.get();
      if (overallStatus != previousStatus) {
        LOGGER.info("Observability health status changed from " + previousStatus + " to " + overallStatus);
      }

      if (overallStatus != HealthStatus.HEALTHY) {
        LOGGER.warning("Observability health check: " + result.toString());
      } else {
        LOGGER.fine("Observability health check passed: " + result.toString());
      }

      return result;

    } catch (final Exception e) {
      healthCheckFailures.incrementAndGet();
      final Duration checkDuration = Duration.between(startTime, Instant.now());
      final String errorMessage = "Health check failed with exception: " + e.getMessage();
      final String errorDetails = "Exception: " + e.getClass().getSimpleName() +
                                 "\nMessage: " + e.getMessage() +
                                 "\nDuration: " + checkDuration.toMillis() + "ms";

      final HealthCheckResult errorResult = new HealthCheckResult(HealthStatus.UNHEALTHY,
                                                                errorMessage, checkDuration, errorDetails);

      currentStatus.set(HealthStatus.UNHEALTHY);
      lastResult.set(errorResult);

      LOGGER.log(Level.SEVERE, "Observability health check failed", e);
      return errorResult;
    }
  }

  /**
   * Gets the current health status.
   *
   * @return current health status
   */
  public HealthStatus getCurrentStatus() {
    return currentStatus.get();
  }

  /**
   * Gets the last health check result.
   *
   * @return last health check result or null if no checks performed
   */
  public HealthCheckResult getLastResult() {
    return lastResult.get();
  }

  /**
   * Gets health check statistics.
   *
   * @return health check statistics
   */
  public String getHealthStatistics() {
    final long totalChecks = healthCheckCount.get();
    final long failures = healthCheckFailures.get();
    final double failureRate = totalChecks > 0 ? (double) failures / totalChecks * 100.0 : 0.0;

    return String.format("Health Statistics: total_checks=%d, failures=%d, failure_rate=%.2f%%, current_status=%s",
        totalChecks, failures, failureRate, getCurrentStatus());
  }

  /**
   * Forces an immediate health check.
   *
   * @return health check result
   */
  public HealthCheckResult forceHealthCheck() {
    LOGGER.info("Forcing immediate observability health check");
    return performHealthCheck();
  }

  /**
   * Configures the health check interval.
   *
   * @param interval check interval
   */
  public void setCheckInterval(final Duration interval) {
    this.checkInterval = interval;

    // Restart scheduler with new interval
    restartScheduler();

    LOGGER.info("Health check interval set to " + interval.toMinutes() + " minutes");
  }

  /**
   * Configures the health check timeout.
   *
   * @param timeout check timeout
   */
  public void setCheckTimeout(final Duration timeout) {
    this.checkTimeout = timeout;
    LOGGER.info("Health check timeout set to " + timeout.toSeconds() + " seconds");
  }

  /**
   * Enables or disables health checks.
   *
   * @param enabled true to enable health checks
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("Health checks " + (enabled ? "enabled" : "disabled"));
  }

  /** Restarts the scheduler with current interval. */
  private void restartScheduler() {
    scheduler.shutdownNow();
    startPeriodicHealthChecks();
  }

  /**
   * Shuts down the health check system.
   */
  public void shutdown() {
    enabled = false;
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (final InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Observability health check system shutdown");
  }
}