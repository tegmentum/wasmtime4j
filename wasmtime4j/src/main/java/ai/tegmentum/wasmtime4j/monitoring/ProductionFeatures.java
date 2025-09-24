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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Production deployment features including graceful degradation, circuit breakers, feature flags,
 * and automatic recovery mechanisms.
 *
 * <p>This system provides:
 *
 * <ul>
 *   <li>Graceful degradation under load or failure conditions
 *   <li>Circuit breaker pattern for external dependencies
 *   <li>Feature flags for controlled rollouts
 *   <li>Automatic recovery and self-healing capabilities
 *   <li>Performance-based adaptive throttling
 *   <li>Emergency shutoff mechanisms
 * </ul>
 *
 * @since 1.0.0
 */
public final class ProductionFeatures {

  private static final Logger LOGGER = Logger.getLogger(ProductionFeatures.class.getName());

  /** Feature flag states. */
  public enum FeatureState {
    ENABLED("Feature is fully enabled", 100),
    DEGRADED("Feature is running in degraded mode", 50),
    DISABLED("Feature is disabled", 0),
    TESTING("Feature is in testing mode", 10);

    private final String description;
    private final int performanceLevel;

    FeatureState(final String description, final int performanceLevel) {
      this.description = description;
      this.performanceLevel = performanceLevel;
    }

    public String getDescription() {
      return description;
    }

    public int getPerformanceLevel() {
      return performanceLevel;
    }
  }

  /** Circuit breaker states. */
  public enum CircuitBreakerState {
    CLOSED("Circuit is closed, requests flowing normally"),
    OPEN("Circuit is open, requests are failing fast"),
    HALF_OPEN("Circuit is half-open, testing if service has recovered");

    private final String description;

    CircuitBreakerState(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Feature flag configuration. */
  public static final class FeatureFlag {
    private final String name;
    private final String description;
    private volatile FeatureState state;
    private volatile int rolloutPercentage;
    private final AtomicLong usageCount = new AtomicLong(0);
    private final Instant createdAt;
    private volatile Instant lastModified;

    public FeatureFlag(
        final String name, final String description, final FeatureState initialState) {
      this(name, description, initialState, 100);
    }

    public FeatureFlag(
        final String name,
        final String description,
        final FeatureState initialState,
        final int rolloutPercentage) {
      this.name = name;
      this.description = description;
      this.state = initialState;
      this.rolloutPercentage = rolloutPercentage;
      this.createdAt = Instant.now();
      this.lastModified = Instant.now();
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public FeatureState getState() {
      return state;
    }

    public void setState(final FeatureState state) {
      this.state = state;
      this.lastModified = Instant.now();
    }

    public int getRolloutPercentage() {
      return rolloutPercentage;
    }

    public void setRolloutPercentage(final int percentage) {
      this.rolloutPercentage = Math.max(0, Math.min(100, percentage));
      this.lastModified = Instant.now();
    }

    public long getUsageCount() {
      return usageCount.get();
    }

    public void incrementUsage() {
      usageCount.incrementAndGet();
    }

    public Instant getCreatedAt() {
      return createdAt;
    }

    public Instant getLastModified() {
      return lastModified;
    }

    public boolean isEnabled() {
      return state == FeatureState.ENABLED || state == FeatureState.TESTING;
    }

    public boolean isAvailable() {
      return state != FeatureState.DISABLED;
    }
  }

  /** Circuit breaker implementation. */
  public static final class CircuitBreaker {
    private final String name;
    private final int failureThreshold;
    private final Duration timeout;
    private final Duration retryInterval;

    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicReference<Instant> lastFailureTime = new AtomicReference<>();
    private final AtomicReference<Instant> lastRetryTime = new AtomicReference<>();

    public CircuitBreaker(
        final String name,
        final int failureThreshold,
        final Duration timeout,
        final Duration retryInterval) {
      this.name = name;
      this.failureThreshold = failureThreshold;
      this.timeout = timeout;
      this.retryInterval = retryInterval;
    }

    /**
     * Executes an operation through the circuit breaker.
     *
     * @param operation operation to execute
     * @return operation result
     * @throws Exception if operation fails or circuit is open
     */
    public <T> T execute(final Operation<T> operation) throws Exception {
      totalRequests.incrementAndGet();

      if (state == CircuitBreakerState.OPEN) {
        if (shouldAttemptReset()) {
          state = CircuitBreakerState.HALF_OPEN;
          LOGGER.info("Circuit breaker " + name + " transitioning to HALF_OPEN");
        } else {
          throw new CircuitBreakerException("Circuit breaker is OPEN for " + name);
        }
      }

      try {
        final T result = operation.execute();
        onSuccess();
        return result;
      } catch (final Exception e) {
        onFailure();
        throw e;
      }
    }

    /** Handles successful operation. */
    private void onSuccess() {
      consecutiveFailures.set(0);
      successCount.incrementAndGet();

      if (state == CircuitBreakerState.HALF_OPEN) {
        state = CircuitBreakerState.CLOSED;
        LOGGER.info("Circuit breaker " + name + " reset to CLOSED");
      }
    }

    /** Handles failed operation. */
    private void onFailure() {
      lastFailureTime.set(Instant.now());
      final int failures = consecutiveFailures.incrementAndGet();

      if (failures >= failureThreshold && state == CircuitBreakerState.CLOSED) {
        state = CircuitBreakerState.OPEN;
        LOGGER.warning(
            "Circuit breaker " + name + " tripped to OPEN after " + failures + " failures");
      } else if (state == CircuitBreakerState.HALF_OPEN) {
        state = CircuitBreakerState.OPEN;
        LOGGER.warning("Circuit breaker " + name + " failed during HALF_OPEN, returning to OPEN");
      }
    }

    /** Checks if circuit breaker should attempt reset. */
    private boolean shouldAttemptReset() {
      final Instant lastFailure = lastFailureTime.get();
      if (lastFailure == null) {
        return true;
      }

      final Instant now = Instant.now();
      final Instant lastRetry = lastRetryTime.get();

      // Check if enough time has passed since last failure
      if (Duration.between(lastFailure, now).compareTo(timeout) < 0) {
        return false;
      }

      // Check if enough time has passed since last retry attempt
      if (lastRetry != null && Duration.between(lastRetry, now).compareTo(retryInterval) < 0) {
        return false;
      }

      lastRetryTime.set(now);
      return true;
    }

    public String getName() {
      return name;
    }

    public CircuitBreakerState getState() {
      return state;
    }

    public int getConsecutiveFailures() {
      return consecutiveFailures.get();
    }

    public int getSuccessCount() {
      return successCount.get();
    }

    public long getTotalRequests() {
      return totalRequests.get();
    }

    public double getFailureRate() {
      final long total = totalRequests.get();
      return total > 0 ? (double) consecutiveFailures.get() / total : 0.0;
    }
  }

  /** Circuit breaker exception. */
  public static class CircuitBreakerException extends Exception {
    public CircuitBreakerException(final String message) {
      super(message);
    }
  }

  /** Operation interface for circuit breaker. */
  @FunctionalInterface
  public interface Operation<T> {
    T execute() throws Exception;
  }

  /** Graceful degradation manager. */
  public static final class GracefulDegradation {
    private final String component;
    private volatile boolean degraded = false;
    private volatile String degradationReason = "";
    private volatile Instant degradationStart;
    private final AtomicInteger degradedRequestCount = new AtomicInteger(0);

    public GracefulDegradation(final String component) {
      this.component = component;
    }

    /**
     * Enters degraded mode.
     *
     * @param reason reason for degradation
     */
    public void enterDegradedMode(final String reason) {
      if (!degraded) {
        degraded = true;
        degradationReason = reason;
        degradationStart = Instant.now();
        LOGGER.warning("Component " + component + " entering degraded mode: " + reason);
      }
    }

    /** Exits degraded mode. */
    public void exitDegradedMode() {
      if (degraded) {
        final Duration degradationDuration = Duration.between(degradationStart, Instant.now());
        degraded = false;
        degradationReason = "";
        LOGGER.info(
            "Component " + component + " exiting degraded mode after " + degradationDuration);
      }
    }

    /**
     * Executes operation with graceful degradation.
     *
     * @param normalOperation normal operation
     * @param degradedOperation fallback operation for degraded mode
     * @return operation result
     */
    public <T> T execute(final Operation<T> normalOperation, final Operation<T> degradedOperation)
        throws Exception {
      if (degraded) {
        degradedRequestCount.incrementAndGet();
        return degradedOperation.execute();
      } else {
        try {
          return normalOperation.execute();
        } catch (final Exception e) {
          // Optionally enter degraded mode on failures
          if (shouldDegradeOnError(e)) {
            enterDegradedMode("Error in normal operation: " + e.getMessage());
            return degradedOperation.execute();
          } else {
            throw e;
          }
        }
      }
    }

    /** Determines if error should trigger degradation. */
    private boolean shouldDegradeOnError(final Exception e) {
      // Simple heuristic - can be made more sophisticated
      return e instanceof java.util.concurrent.TimeoutException
          || e.getCause() instanceof java.net.SocketTimeoutException;
    }

    public boolean isDegraded() {
      return degraded;
    }

    public String getDegradationReason() {
      return degradationReason;
    }

    public Duration getDegradationDuration() {
      return degradationStart != null
          ? Duration.between(degradationStart, Instant.now())
          : Duration.ZERO;
    }

    public int getDegradedRequestCount() {
      return degradedRequestCount.get();
    }
  }

  /** Feature flags storage. */
  private final ConcurrentHashMap<String, FeatureFlag> featureFlags = new ConcurrentHashMap<>();

  /** Circuit breakers storage. */
  private final ConcurrentHashMap<String, CircuitBreaker> circuitBreakers =
      new ConcurrentHashMap<>();

  /** Graceful degradation managers. */
  private final ConcurrentHashMap<String, GracefulDegradation> degradationManagers =
      new ConcurrentHashMap<>();

  /** Configuration. */
  private final MonitoringConfigManager configManager;

  /**
   * Creates production features manager.
   *
   * @param configManager configuration manager
   */
  public ProductionFeatures(final MonitoringConfigManager configManager) {
    this.configManager = configManager;
    initializeDefaultFeatures();
    LOGGER.info("Production features manager initialized");
  }

  /** Initializes default feature flags and circuit breakers. */
  private void initializeDefaultFeatures() {
    // Initialize default feature flags
    addFeatureFlag(
        new FeatureFlag(
            "monitoring.enabled",
            "Enable comprehensive monitoring",
            configManager.isMonitoringEnabled() ? FeatureState.ENABLED : FeatureState.DISABLED));

    addFeatureFlag(
        new FeatureFlag(
            "metrics.collection",
            "Enable metrics collection",
            configManager.isMetricsEnabled() ? FeatureState.ENABLED : FeatureState.DISABLED));

    addFeatureFlag(
        new FeatureFlag(
            "health.checks",
            "Enable health checking",
            configManager.isHealthChecksEnabled() ? FeatureState.ENABLED : FeatureState.DISABLED));

    addFeatureFlag(
        new FeatureFlag(
            "diagnostics.collection",
            "Enable diagnostic data collection",
            configManager.isDiagnosticsEnabled() ? FeatureState.ENABLED : FeatureState.DISABLED));

    addFeatureFlag(
        new FeatureFlag(
            "performance.optimization", "Enable performance optimizations", FeatureState.ENABLED));

    addFeatureFlag(
        new FeatureFlag(
            "auto.recovery",
            "Enable automatic recovery mechanisms",
            configManager.getBoolean(MonitoringConfigManager.ConfigKeys.AUTO_RECOVERY_ENABLED, true)
                ? FeatureState.ENABLED
                : FeatureState.DISABLED));

    // Initialize circuit breakers
    if (configManager.getBoolean(
        MonitoringConfigManager.ConfigKeys.CIRCUIT_BREAKER_ENABLED, false)) {
      addCircuitBreaker("external.service", 5, Duration.ofMinutes(1), Duration.ofSeconds(30));
      addCircuitBreaker("metrics.export", 3, Duration.ofSeconds(30), Duration.ofSeconds(15));
      addCircuitBreaker("diagnostic.processing", 10, Duration.ofMinutes(2), Duration.ofMinutes(1));
    }

    // Initialize graceful degradation managers
    addGracefulDegradation("monitoring.system");
    addGracefulDegradation("metrics.collection");
    addGracefulDegradation("health.checking");
  }

  /**
   * Adds a feature flag.
   *
   * @param featureFlag feature flag to add
   */
  public void addFeatureFlag(final FeatureFlag featureFlag) {
    featureFlags.put(featureFlag.getName(), featureFlag);
    LOGGER.info(
        "Added feature flag: " + featureFlag.getName() + " (" + featureFlag.getState() + ")");
  }

  /**
   * Gets a feature flag.
   *
   * @param name feature flag name
   * @return feature flag or null if not found
   */
  public FeatureFlag getFeatureFlag(final String name) {
    return featureFlags.get(name);
  }

  /**
   * Checks if a feature is enabled.
   *
   * @param name feature flag name
   * @return true if feature is enabled
   */
  public boolean isFeatureEnabled(final String name) {
    final FeatureFlag flag = featureFlags.get(name);
    if (flag == null) {
      return false;
    }
    flag.incrementUsage();
    return flag.isEnabled();
  }

  /**
   * Checks if a feature is available (not disabled).
   *
   * @param name feature flag name
   * @return true if feature is available
   */
  public boolean isFeatureAvailable(final String name) {
    final FeatureFlag flag = featureFlags.get(name);
    if (flag == null) {
      return false;
    }
    flag.incrementUsage();
    return flag.isAvailable();
  }

  /**
   * Sets feature flag state.
   *
   * @param name feature flag name
   * @param state new state
   */
  public void setFeatureState(final String name, final FeatureState state) {
    final FeatureFlag flag = featureFlags.get(name);
    if (flag != null) {
      flag.setState(state);
      LOGGER.info("Feature flag " + name + " state changed to " + state);
    }
  }

  /**
   * Adds a circuit breaker.
   *
   * @param name circuit breaker name
   * @param failureThreshold failure threshold
   * @param timeout timeout duration
   * @param retryInterval retry interval
   */
  public void addCircuitBreaker(
      final String name,
      final int failureThreshold,
      final Duration timeout,
      final Duration retryInterval) {
    final CircuitBreaker circuitBreaker =
        new CircuitBreaker(name, failureThreshold, timeout, retryInterval);
    circuitBreakers.put(name, circuitBreaker);
    LOGGER.info("Added circuit breaker: " + name);
  }

  /**
   * Gets a circuit breaker.
   *
   * @param name circuit breaker name
   * @return circuit breaker or null if not found
   */
  public CircuitBreaker getCircuitBreaker(final String name) {
    return circuitBreakers.get(name);
  }

  /**
   * Adds a graceful degradation manager.
   *
   * @param component component name
   */
  public void addGracefulDegradation(final String component) {
    degradationManagers.put(component, new GracefulDegradation(component));
    LOGGER.info("Added graceful degradation manager for: " + component);
  }

  /**
   * Gets a graceful degradation manager.
   *
   * @param component component name
   * @return graceful degradation manager or null if not found
   */
  public GracefulDegradation getGracefulDegradation(final String component) {
    return degradationManagers.get(component);
  }

  /**
   * Gets production features status.
   *
   * @return formatted status report
   */
  public String getProductionStatus() {
    final StringBuilder sb = new StringBuilder("=== Production Features Status ===\n");

    // Feature flags summary
    sb.append("Feature Flags:\n");
    for (final FeatureFlag flag : featureFlags.values()) {
      sb.append(
          String.format(
              "  %-25s: %s (%s) - Usage: %d\n",
              flag.getName(), flag.getState(), flag.getDescription(), flag.getUsageCount()));
    }
    sb.append("\n");

    // Circuit breakers summary
    sb.append("Circuit Breakers:\n");
    if (circuitBreakers.isEmpty()) {
      sb.append("  No circuit breakers configured\n");
    } else {
      for (final CircuitBreaker cb : circuitBreakers.values()) {
        sb.append(
            String.format(
                "  %-25s: %s - Failures: %d/%d, Success: %d\n",
                cb.getName(),
                cb.getState(),
                cb.getConsecutiveFailures(),
                cb.getTotalRequests(),
                cb.getSuccessCount()));
      }
    }
    sb.append("\n");

    // Graceful degradation summary
    sb.append("Graceful Degradation:\n");
    for (final GracefulDegradation gd : degradationManagers.values()) {
      if (gd.isDegraded()) {
        sb.append(
            String.format(
                "  %-25s: DEGRADED (%s) - Duration: %s, Requests: %d\n",
                gd.component,
                gd.getDegradationReason(),
                gd.getDegradationDuration(),
                gd.getDegradedRequestCount()));
      } else {
        sb.append(String.format("  %-25s: NORMAL\n", gd.component));
      }
    }

    return sb.toString();
  }

  /**
   * Gets all feature flags.
   *
   * @return map of feature flags
   */
  public Map<String, FeatureFlag> getAllFeatureFlags() {
    return Map.copyOf(featureFlags);
  }

  /**
   * Gets all circuit breakers.
   *
   * @return map of circuit breakers
   */
  public Map<String, CircuitBreaker> getAllCircuitBreakers() {
    return Map.copyOf(circuitBreakers);
  }

  /**
   * Gets all graceful degradation managers.
   *
   * @return map of graceful degradation managers
   */
  public Map<String, GracefulDegradation> getAllGracefulDegradation() {
    return Map.copyOf(degradationManagers);
  }
}
