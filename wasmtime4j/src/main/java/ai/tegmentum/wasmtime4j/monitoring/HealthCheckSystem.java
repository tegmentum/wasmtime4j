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
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Comprehensive health checking system with automatic recovery and configurable health indicators.
 *
 * <p>This system provides:
 *
 * <ul>
 *   <li>System health indicators (memory, threads, resources)
 *   <li>Component health monitoring (runtime, modules, instances)
 *   <li>Automatic health recovery mechanisms
 *   <li>Health status reporting with detailed diagnostics
 *   <li>Configurable health thresholds and recovery policies
 *   <li>Health check scheduling and coordination
 * </ul>
 *
 * @since 1.0.0
 */
public final class HealthCheckSystem {

  private static final Logger LOGGER = Logger.getLogger(HealthCheckSystem.class.getName());

  /** Health status levels. */
  public enum HealthStatus {
    HEALTHY("All systems operational", 0),
    DEGRADED("Some non-critical issues detected", 1),
    UNHEALTHY("Critical issues detected", 2),
    CRITICAL("System stability at risk", 3),
    UNKNOWN("Health status cannot be determined", 4);

    private final String description;
    private final int severity;

    HealthStatus(final String description, final int severity) {
      this.description = description;
      this.severity = severity;
    }

    public String getDescription() {
      return description;
    }

    public int getSeverity() {
      return severity;
    }

    public boolean isWorseThan(final HealthStatus other) {
      return this.severity > other.severity;
    }
  }

  /** Health check result. */
  public static final class HealthCheckResult {
    private final String componentId;
    private final HealthStatus status;
    private final String message;
    private final Instant timestamp;
    private final Duration executionTime;
    private final Map<String, Object> details;
    private final Throwable error;

    /**
     * Creates a new HealthCheckResult.
     *
     * @param componentId the component identifier
     * @param status the health status
     * @param message the status message
     * @param executionTime the check execution time
     * @param details additional details
     */
    public HealthCheckResult(
        final String componentId,
        final HealthStatus status,
        final String message,
        final Duration executionTime,
        final Map<String, Object> details,
        final Throwable error) {
      this.componentId = componentId;
      this.status = status;
      this.message = message;
      this.timestamp = Instant.now();
      this.executionTime = executionTime;
      this.details = Map.copyOf(details != null ? details : Map.of());
      this.error = error;
    }

    public String getComponentId() {
      return componentId;
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

    public Duration getExecutionTime() {
      return executionTime;
    }

    public Map<String, Object> getDetails() {
      return details;
    }

    public Throwable getError() {
      return error;
    }

    public boolean isHealthy() {
      return status == HealthStatus.HEALTHY;
    }

    @Override
    public String toString() {
      return String.format(
          "%s[%s]: %s (%s in %dms)",
          componentId, status, message, timestamp, executionTime.toMillis());
    }
  }

  /** Health check interface. */
  @FunctionalInterface
  public interface HealthCheck {
    /**
     * Performs a health check.
     *
     * @return health check result
     * @throws Exception if health check fails
     */
    HealthCheckResult check() throws Exception;
  }

  /** Recovery action interface. */
  @FunctionalInterface
  public interface RecoveryAction {
    /**
     * Attempts to recover from unhealthy state.
     *
     * @param healthResult the failed health check result
     * @return true if recovery was successful
     * @throws Exception if recovery fails
     */
    boolean recover(HealthCheckResult healthResult) throws Exception;
  }

  /** Health check configuration. */
  public static final class HealthCheckConfig {
    private final String componentId;
    private final HealthCheck healthCheck;
    private final Duration checkInterval;
    private final Duration timeout;
    private final int maxFailures;
    private final boolean autoRecovery;
    private final RecoveryAction recoveryAction;

    /**
     * Creates a new HealthCheckConfig.
     *
     * @param componentId the component identifier
     * @param healthCheck the health check implementation
     * @param checkInterval the interval between checks
     * @param timeout the check timeout
     * @param maxFailures the maximum failures before unhealthy
     */
    public HealthCheckConfig(
        final String componentId,
        final HealthCheck healthCheck,
        final Duration checkInterval,
        final Duration timeout,
        final int maxFailures,
        final boolean autoRecovery,
        final RecoveryAction recoveryAction) {
      this.componentId = componentId;
      this.healthCheck = healthCheck;
      this.checkInterval = checkInterval != null ? checkInterval : Duration.ofMinutes(1);
      this.timeout = timeout != null ? timeout : Duration.ofSeconds(30);
      this.maxFailures = maxFailures > 0 ? maxFailures : 3;
      this.autoRecovery = autoRecovery;
      this.recoveryAction = recoveryAction;
    }

    public String getComponentId() {
      return componentId;
    }

    public HealthCheck getHealthCheck() {
      return healthCheck;
    }

    public Duration getCheckInterval() {
      return checkInterval;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public int getMaxFailures() {
      return maxFailures;
    }

    public boolean isAutoRecovery() {
      return autoRecovery;
    }

    public RecoveryAction getRecoveryAction() {
      return recoveryAction;
    }
  }

  /** Health check execution state. */
  private static final class HealthCheckState {
    volatile HealthCheckResult lastResult;
    volatile int consecutiveFailures = 0;
    volatile long totalChecks = 0;
    volatile long successfulChecks = 0;
    volatile Instant lastRecoveryAttempt;
    volatile boolean recoveryInProgress = false;
    final AtomicLong totalExecutionTime = new AtomicLong(0);

    double getSuccessRate() {
      return totalChecks > 0 ? (double) successfulChecks / totalChecks : 1.0;
    }

    Duration getAverageExecutionTime() {
      return totalChecks > 0
          ? Duration.ofNanos(totalExecutionTime.get() / totalChecks)
          : Duration.ZERO;
    }
  }

  /** System health configuration. */
  public static final class SystemHealthConfig {
    private final double memoryThresholdWarning;
    private final double memoryThresholdCritical;
    private final int threadCountThresholdWarning;
    private final int threadCountThresholdCritical;
    private final Duration healthCheckTimeout;
    private final Duration overallTimeout;
    private final boolean enableAutoRecovery;

    public SystemHealthConfig() {
      this(0.8, 0.95, 200, 500, Duration.ofSeconds(10), Duration.ofMinutes(1), true);
    }

    /**
     * Creates a new SystemHealthConfig with custom thresholds.
     *
     * @param memoryThresholdWarning the memory usage warning threshold
     * @param memoryThresholdCritical the memory usage critical threshold
     * @param threadCountThresholdWarning the thread count warning threshold
     * @param threadCountThresholdCritical the thread count critical threshold
     * @param healthCheckTimeout the health check timeout
     * @param overallTimeout the overall timeout
     * @param enableAutoRecovery whether to enable automatic recovery
     */
    public SystemHealthConfig(
        final double memoryThresholdWarning,
        final double memoryThresholdCritical,
        final int threadCountThresholdWarning,
        final int threadCountThresholdCritical,
        final Duration healthCheckTimeout,
        final Duration overallTimeout,
        final boolean enableAutoRecovery) {
      this.memoryThresholdWarning = memoryThresholdWarning;
      this.memoryThresholdCritical = memoryThresholdCritical;
      this.threadCountThresholdWarning = threadCountThresholdWarning;
      this.threadCountThresholdCritical = threadCountThresholdCritical;
      this.healthCheckTimeout = healthCheckTimeout;
      this.overallTimeout = overallTimeout;
      this.enableAutoRecovery = enableAutoRecovery;
    }

    public double getMemoryThresholdWarning() {
      return memoryThresholdWarning;
    }

    public double getMemoryThresholdCritical() {
      return memoryThresholdCritical;
    }

    public int getThreadCountThresholdWarning() {
      return threadCountThresholdWarning;
    }

    public int getThreadCountThresholdCritical() {
      return threadCountThresholdCritical;
    }

    public Duration getHealthCheckTimeout() {
      return healthCheckTimeout;
    }

    public Duration getOverallTimeout() {
      return overallTimeout;
    }

    public boolean isEnableAutoRecovery() {
      return enableAutoRecovery;
    }
  }

  /** Health check configurations and states. */
  private final ConcurrentHashMap<String, HealthCheckConfig> healthCheckConfigs =
      new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, HealthCheckState> healthCheckStates =
      new ConcurrentHashMap<>();

  /** Overall system health state. */
  private final AtomicReference<HealthStatus> overallHealthStatus =
      new AtomicReference<>(HealthStatus.UNKNOWN);

  private final AtomicReference<Instant> lastOverallCheck = new AtomicReference<>(Instant.now());
  private final AtomicLong totalHealthChecks = new AtomicLong(0);
  private final AtomicLong totalRecoveryAttempts = new AtomicLong(0);
  private final AtomicLong successfulRecoveries = new AtomicLong(0);

  /** System components for monitoring. */
  private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

  private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

  /** Configuration and scheduling. */
  private final SystemHealthConfig systemConfig;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
  private volatile boolean enabled = true;

  /** Health listeners for notifications. */
  private final List<HealthStatusListener> healthListeners = new CopyOnWriteArrayList<>();

  /** Health status change listener. */
  @FunctionalInterface
  public interface HealthStatusListener {
    /**
     * Called when overall health status changes.
     *
     * @param previousStatus previous health status
     * @param currentStatus current health status
     * @param trigger what triggered the change
     */
    void onHealthStatusChange(
        HealthStatus previousStatus, HealthStatus currentStatus, String trigger);
  }

  /** Creates a health check system with default configuration. */
  public HealthCheckSystem() {
    this(new SystemHealthConfig());
  }

  /**
   * Creates a health check system with custom configuration.
   *
   * @param systemConfig system health configuration
   */
  public HealthCheckSystem(final SystemHealthConfig systemConfig) {
    this.systemConfig = systemConfig;
    initializeBuiltInHealthChecks();
    startHealthCheckScheduler();
    LOGGER.info("Health check system initialized");
  }

  /** Initializes built-in health checks. */
  private void initializeBuiltInHealthChecks() {
    // JVM memory health check
    registerHealthCheck(
        new HealthCheckConfig(
            "jvm_memory",
            this::checkMemoryHealth,
            Duration.ofMinutes(1),
            Duration.ofSeconds(5),
            2,
            systemConfig.isEnableAutoRecovery(),
            this::recoverMemoryIssues));

    // JVM thread health check
    registerHealthCheck(
        new HealthCheckConfig(
            "jvm_threads",
            this::checkThreadHealth,
            Duration.ofMinutes(1),
            Duration.ofSeconds(5),
            2,
            false,
            null));

    // System resources health check
    registerHealthCheck(
        new HealthCheckConfig(
            "system_resources",
            this::checkSystemResources,
            Duration.ofMinutes(2),
            Duration.ofSeconds(10),
            3,
            false,
            null));
  }

  /** Starts the health check scheduler. */
  private void startHealthCheckScheduler() {
    // Schedule individual health checks
    scheduler.scheduleAtFixedRate(this::runScheduledHealthChecks, 0, 30, TimeUnit.SECONDS);

    // Schedule overall health assessment
    scheduler.scheduleAtFixedRate(this::assessOverallHealth, 0, 60, TimeUnit.SECONDS);

    // Schedule cleanup of old states
    scheduler.scheduleAtFixedRate(this::cleanupHealthCheckStates, 1, 1, TimeUnit.HOURS);
  }

  /**
   * Registers a health check configuration.
   *
   * @param config health check configuration
   */
  public void registerHealthCheck(final HealthCheckConfig config) {
    if (config.getComponentId() == null || config.getHealthCheck() == null) {
      throw new IllegalArgumentException("Component ID and health check are required");
    }

    healthCheckConfigs.put(config.getComponentId(), config);
    healthCheckStates.put(config.getComponentId(), new HealthCheckState());
    LOGGER.info("Registered health check: " + config.getComponentId());
  }

  /**
   * Unregisters a health check.
   *
   * @param componentId component identifier
   */
  public void unregisterHealthCheck(final String componentId) {
    healthCheckConfigs.remove(componentId);
    healthCheckStates.remove(componentId);
    LOGGER.info("Unregistered health check: " + componentId);
  }

  /**
   * Performs health check for a specific component.
   *
   * @param componentId component identifier
   * @return health check result
   */
  public HealthCheckResult checkComponentHealth(final String componentId) {
    final HealthCheckConfig config = healthCheckConfigs.get(componentId);
    if (config == null) {
      return new HealthCheckResult(
          componentId,
          HealthStatus.UNKNOWN,
          "Health check not registered",
          Duration.ZERO,
          null,
          null);
    }

    return executeHealthCheck(config);
  }

  /** Executes a health check with timeout and error handling. */
  private HealthCheckResult executeHealthCheck(final HealthCheckConfig config) {
    if (!enabled) {
      return new HealthCheckResult(
          config.getComponentId(),
          HealthStatus.UNKNOWN,
          "Health checking disabled",
          Duration.ZERO,
          null,
          null);
    }

    final long startTime = System.nanoTime();
    final HealthCheckState state = healthCheckStates.get(config.getComponentId());

    try {
      // Execute health check with timeout
      final HealthCheckResult result = config.getHealthCheck().check();
      final Duration executionTime = Duration.ofNanos(System.nanoTime() - startTime);

      // Update state
      state.lastResult = result;
      state.totalChecks++;
      state.totalExecutionTime.addAndGet(executionTime.toNanos());

      if (result.isHealthy()) {
        state.successfulChecks++;
        state.consecutiveFailures = 0;
      } else {
        state.consecutiveFailures++;

        // Attempt recovery if configured and thresholds met
        if (config.isAutoRecovery()
            && state.consecutiveFailures >= config.getMaxFailures()
            && config.getRecoveryAction() != null
            && !state.recoveryInProgress) {
          attemptRecovery(config, result, state);
        }
      }

      totalHealthChecks.incrementAndGet();
      return result;

    } catch (final Exception e) {
      final Duration executionTime = Duration.ofNanos(System.nanoTime() - startTime);
      final HealthCheckResult errorResult =
          new HealthCheckResult(
              config.getComponentId(),
              HealthStatus.CRITICAL,
              "Health check failed: " + e.getMessage(),
              executionTime,
              Map.of("error", e.getClass().getSimpleName()),
              e);

      state.lastResult = errorResult;
      state.totalChecks++;
      state.consecutiveFailures++;
      state.totalExecutionTime.addAndGet(executionTime.toNanos());

      LOGGER.warning("Health check failed for " + config.getComponentId() + ": " + e.getMessage());
      return errorResult;
    }
  }

  /** Attempts recovery for a failed health check. */
  private void attemptRecovery(
      final HealthCheckConfig config,
      final HealthCheckResult failedResult,
      final HealthCheckState state) {

    if (state.lastRecoveryAttempt != null
        && Duration.between(state.lastRecoveryAttempt, Instant.now())
                .compareTo(Duration.ofMinutes(5))
            < 0) {
      return; // Too soon since last recovery attempt
    }

    state.recoveryInProgress = true;
    state.lastRecoveryAttempt = Instant.now();
    totalRecoveryAttempts.incrementAndGet();

    scheduler.execute(
        () -> {
          try {
            LOGGER.info("Attempting recovery for " + config.getComponentId());
            final boolean recovered = config.getRecoveryAction().recover(failedResult);

            if (recovered) {
              successfulRecoveries.incrementAndGet();
              state.consecutiveFailures = 0;
              LOGGER.info("Successfully recovered " + config.getComponentId());
            } else {
              LOGGER.warning("Recovery failed for " + config.getComponentId());
            }

          } catch (final Exception e) {
            LOGGER.severe(
                "Recovery attempt failed for " + config.getComponentId() + ": " + e.getMessage());
          } finally {
            state.recoveryInProgress = false;
          }
        });
  }

  /** Runs scheduled health checks. */
  private void runScheduledHealthChecks() {
    if (!enabled) {
      return;
    }

    try {
      final Instant now = Instant.now();
      for (final HealthCheckConfig config : healthCheckConfigs.values()) {
        final HealthCheckState state = healthCheckStates.get(config.getComponentId());

        // Check if it's time for this health check
        if (state.lastResult == null
            || Duration.between(state.lastResult.getTimestamp(), now)
                    .compareTo(config.getCheckInterval())
                >= 0) {

          scheduler.execute(() -> executeHealthCheck(config));
        }
      }
    } catch (final Exception e) {
      LOGGER.warning("Error running scheduled health checks: " + e.getMessage());
    }
  }

  /** Assesses overall system health. */
  private void assessOverallHealth() {
    if (!enabled) {
      return;
    }

    try {
      final HealthStatus previousStatus = overallHealthStatus.get();
      HealthStatus newStatus = HealthStatus.HEALTHY;

      // Aggregate all component health statuses
      for (final HealthCheckState state : healthCheckStates.values()) {
        if (state.lastResult != null) {
          if (state.lastResult.getStatus().isWorseThan(newStatus)) {
            newStatus = state.lastResult.getStatus();
          }
        }
      }

      // If no health checks have run, status is unknown
      if (healthCheckStates.values().stream().noneMatch(s -> s.lastResult != null)) {
        newStatus = HealthStatus.UNKNOWN;
      }

      // Update status and notify listeners if changed
      if (overallHealthStatus.compareAndSet(previousStatus, newStatus)) {
        lastOverallCheck.set(Instant.now());

        if (previousStatus != newStatus) {
          notifyHealthStatusListeners(previousStatus, newStatus, "assessment");
          LOGGER.info(
              String.format("Overall health status changed: %s -> %s", previousStatus, newStatus));
        }
      }

    } catch (final Exception e) {
      LOGGER.warning("Error assessing overall health: " + e.getMessage());
      overallHealthStatus.set(HealthStatus.UNKNOWN);
    }
  }

  /** Notifies health status listeners. */
  private void notifyHealthStatusListeners(
      final HealthStatus previous, final HealthStatus current, final String trigger) {
    for (final HealthStatusListener listener : healthListeners) {
      try {
        listener.onHealthStatusChange(previous, current, trigger);
      } catch (final Exception e) {
        LOGGER.warning("Health status listener error: " + e.getMessage());
      }
    }
  }

  /** Cleans up old health check states. */
  private void cleanupHealthCheckStates() {
    // Remove states for unregistered health checks
    healthCheckStates.entrySet().removeIf(entry -> !healthCheckConfigs.containsKey(entry.getKey()));
  }

  /** Memory health check implementation. */
  private HealthCheckResult checkMemoryHealth() {
    final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    final double heapUtilization = (double) heapUsage.getUsed() / heapUsage.getMax();

    final Map<String, Object> details =
        Map.of(
            "heapUsed", heapUsage.getUsed(),
            "heapMax", heapUsage.getMax(),
            "heapUtilization", heapUtilization,
            "threshold_warning", systemConfig.getMemoryThresholdWarning(),
            "threshold_critical", systemConfig.getMemoryThresholdCritical());

    HealthStatus status = HealthStatus.HEALTHY;
    String message = String.format("Heap utilization: %.1f%%", heapUtilization * 100);

    if (heapUtilization >= systemConfig.getMemoryThresholdCritical()) {
      status = HealthStatus.CRITICAL;
      message = "Critical heap memory usage: " + message;
    } else if (heapUtilization >= systemConfig.getMemoryThresholdWarning()) {
      status = HealthStatus.DEGRADED;
      message = "High heap memory usage: " + message;
    }

    return new HealthCheckResult(
        "jvm_memory", status, message, Duration.ofMillis(1), details, null);
  }

  /** Thread health check implementation. */
  private HealthCheckResult checkThreadHealth() {
    final int threadCount = threadBean.getThreadCount();
    final int daemonCount = threadBean.getDaemonThreadCount();
    final int peakCount = threadBean.getPeakThreadCount();

    final Map<String, Object> details =
        Map.of(
            "threadCount", threadCount,
            "daemonCount", daemonCount,
            "peakCount", peakCount,
            "threshold_warning", systemConfig.getThreadCountThresholdWarning(),
            "threshold_critical", systemConfig.getThreadCountThresholdCritical());

    HealthStatus status = HealthStatus.HEALTHY;
    String message = String.format("Thread count: %d (daemon: %d)", threadCount, daemonCount);

    if (threadCount >= systemConfig.getThreadCountThresholdCritical()) {
      status = HealthStatus.CRITICAL;
      message = "Critical thread count: " + message;
    } else if (threadCount >= systemConfig.getThreadCountThresholdWarning()) {
      status = HealthStatus.DEGRADED;
      message = "High thread count: " + message;
    }

    return new HealthCheckResult(
        "jvm_threads", status, message, Duration.ofMillis(1), details, null);
  }

  /** System resources health check implementation. */
  private HealthCheckResult checkSystemResources() {
    final Runtime runtime = Runtime.getRuntime();
    final long freeMemory = runtime.freeMemory();
    final long totalMemory = runtime.totalMemory();
    final long maxMemory = runtime.maxMemory();
    final int processors = runtime.availableProcessors();

    final Map<String, Object> details =
        Map.of(
            "freeMemory", freeMemory,
            "totalMemory", totalMemory,
            "maxMemory", maxMemory,
            "processors", processors);

    return new HealthCheckResult(
        "system_resources",
        HealthStatus.HEALTHY,
        String.format("Processors: %d, Free memory: %d MB", processors, freeMemory / (1024 * 1024)),
        Duration.ofMillis(2),
        details,
        null);
  }

  /** Memory recovery action implementation. */
  private boolean recoverMemoryIssues(final HealthCheckResult healthResult) {
    try {
      LOGGER.info("Attempting memory recovery: triggering GC");
      System.gc();

      // Wait a moment for GC to complete
      Thread.sleep(1000);

      // Check if memory situation improved
      final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
      final double newUtilization = (double) heapUsage.getUsed() / heapUsage.getMax();

      return newUtilization < systemConfig.getMemoryThresholdCritical();

    } catch (final Exception e) {
      LOGGER.warning("Memory recovery failed: " + e.getMessage());
      return false;
    }
  }

  /**
   * Gets overall health status.
   *
   * @return overall health status
   */
  public HealthStatus getOverallHealthStatus() {
    return overallHealthStatus.get();
  }

  /**
   * Gets health status for a specific component.
   *
   * @param componentId component identifier
   * @return health status or null if not found
   */
  public HealthStatus getComponentHealthStatus(final String componentId) {
    final HealthCheckState state = healthCheckStates.get(componentId);
    return state != null && state.lastResult != null
        ? state.lastResult.getStatus()
        : HealthStatus.UNKNOWN;
  }

  /**
   * Gets detailed health report.
   *
   * @return formatted health report
   */
  public String getHealthReport() {
    final StringBuilder sb = new StringBuilder("=== Health Check Report ===\n");

    sb.append(String.format("Overall Status: %s\n", overallHealthStatus.get()));
    sb.append(String.format("Last Assessment: %s\n", lastOverallCheck.get()));
    sb.append(String.format("Total Health Checks: %,d\n", totalHealthChecks.get()));
    sb.append(
        String.format(
            "Recovery Attempts: %,d (successful: %,d)\n",
            totalRecoveryAttempts.get(), successfulRecoveries.get()));
    sb.append("\n");

    sb.append("Component Health:\n");
    for (final HealthCheckState state : healthCheckStates.values()) {
      if (state.lastResult != null) {
        sb.append(
            String.format(
                "  %s: %s - %s\n",
                state.lastResult.getComponentId(),
                state.lastResult.getStatus(),
                state.lastResult.getMessage()));
        sb.append(
            String.format(
                "    Success Rate: %.1f%% (%d/%d)\n",
                state.getSuccessRate() * 100, state.successfulChecks, state.totalChecks));
        sb.append(
            String.format("    Avg Execution: %dms\n", state.getAverageExecutionTime().toMillis()));
        if (state.consecutiveFailures > 0) {
          sb.append(String.format("    Consecutive Failures: %d\n", state.consecutiveFailures));
        }
      }
    }

    return sb.toString();
  }

  /**
   * Adds a health status listener.
   *
   * @param listener health status listener
   */
  public void addHealthStatusListener(final HealthStatusListener listener) {
    if (listener != null) {
      healthListeners.add(listener);
    }
  }

  /**
   * Removes a health status listener.
   *
   * @param listener health status listener
   */
  public void removeHealthStatusListener(final HealthStatusListener listener) {
    healthListeners.remove(listener);
  }

  /**
   * Enables or disables health checking.
   *
   * @param enabled true to enable health checking
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("Health checking " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Checks if health checking is enabled.
   *
   * @return true if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /** Forces immediate health check of all components. */
  public void forceHealthCheck() {
    scheduler.execute(
        () -> {
          for (final HealthCheckConfig config : healthCheckConfigs.values()) {
            executeHealthCheck(config);
          }
          assessOverallHealth();
        });
  }

  /** Shuts down the health check system. */
  public void shutdown() {
    enabled = false;
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (final InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Health check system shutdown");
  }
}
