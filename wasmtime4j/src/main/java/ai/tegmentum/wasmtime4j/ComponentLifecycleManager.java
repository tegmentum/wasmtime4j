package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Component lifecycle management interface.
 *
 * <p>This interface provides comprehensive lifecycle management for WebAssembly components,
 * including start, stop, restart, pause, and resume operations with dependency management.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Component state management and transition validation
 *   <li>Dependency-aware lifecycle operations
 *   <li>Graceful shutdown with configurable timeouts
 *   <li>Health monitoring and automatic recovery
 *   <li>Resource cleanup and isolation
 * </ul>
 *
 * @since 1.0.0
 */
public interface ComponentLifecycleManager {

  /**
   * Starts a component and its dependencies.
   *
   * <p>This method ensures that all dependencies are started before starting the target component.
   * It performs dependency validation and follows the correct startup order.
   *
   * @param componentId the component to start
   * @throws WasmException if the component cannot be started
   * @throws IllegalArgumentException if componentId is null or empty
   */
  void startComponent(String componentId) throws WasmException;

  /**
   * Starts a component with specific configuration.
   *
   * @param componentId the component to start
   * @param config the startup configuration
   * @throws WasmException if the component cannot be started
   * @throws IllegalArgumentException if componentId or config is null
   */
  void startComponent(String componentId, ComponentStartupConfig config) throws WasmException;

  /**
   * Starts multiple components in parallel where possible.
   *
   * @param componentIds the components to start
   * @return future that completes when all components are started
   * @throws WasmException if any component cannot be started
   * @throws IllegalArgumentException if componentIds is null or empty
   */
  CompletableFuture<Void> startComponents(List<String> componentIds) throws WasmException;

  /**
   * Stops a component gracefully.
   *
   * <p>This method checks for dependent components and may refuse to stop if dependents are running
   * (unless forced). It performs graceful shutdown with configurable timeout.
   *
   * @param componentId the component to stop
   * @param force whether to force stop even if dependents are running
   * @throws WasmException if the component cannot be stopped
   * @throws IllegalArgumentException if componentId is null or empty
   */
  void stopComponent(String componentId, boolean force) throws WasmException;

  /**
   * Stops a component with specific configuration.
   *
   * @param componentId the component to stop
   * @param config the shutdown configuration
   * @throws WasmException if the component cannot be stopped
   * @throws IllegalArgumentException if componentId or config is null
   */
  void stopComponent(String componentId, ComponentShutdownConfig config) throws WasmException;

  /**
   * Stops multiple components in reverse dependency order.
   *
   * @param componentIds the components to stop
   * @return future that completes when all components are stopped
   * @throws WasmException if any component cannot be stopped
   * @throws IllegalArgumentException if componentIds is null or empty
   */
  CompletableFuture<Void> stopComponents(List<String> componentIds) throws WasmException;

  /**
   * Restarts a component.
   *
   * <p>This method stops the component gracefully and then starts it again. Dependencies are
   * handled appropriately during the restart process.
   *
   * @param componentId the component to restart
   * @throws WasmException if the component cannot be restarted
   * @throws IllegalArgumentException if componentId is null or empty
   */
  void restartComponent(String componentId) throws WasmException;

  /**
   * Restarts a component with specific configuration.
   *
   * @param componentId the component to restart
   * @param config the restart configuration
   * @throws WasmException if the component cannot be restarted
   * @throws IllegalArgumentException if componentId or config is null
   */
  void restartComponent(String componentId, ComponentRestartConfig config) throws WasmException;

  /**
   * Pauses a component temporarily.
   *
   * <p>This method suspends component execution while maintaining its state. The component can be
   * resumed later without losing state.
   *
   * @param componentId the component to pause
   * @throws WasmException if the component cannot be paused
   * @throws IllegalArgumentException if componentId is null or empty
   */
  void pauseComponent(String componentId) throws WasmException;

  /**
   * Resumes a paused component.
   *
   * @param componentId the component to resume
   * @throws WasmException if the component cannot be resumed
   * @throws IllegalArgumentException if componentId is null or empty
   */
  void resumeComponent(String componentId) throws WasmException;

  /**
   * Gets the current lifecycle state of a component.
   *
   * @param componentId the component to check
   * @return the current lifecycle state
   * @throws IllegalArgumentException if componentId is null or empty
   */
  ComponentLifecycleState getComponentState(String componentId);

  /**
   * Gets the lifecycle states of all managed components.
   *
   * @return map of component IDs to their lifecycle states
   */
  Map<String, ComponentLifecycleState> getAllComponentStates();

  /**
   * Gets components in a specific lifecycle state.
   *
   * @param state the lifecycle state to filter by
   * @return set of component IDs in the specified state
   * @throws IllegalArgumentException if state is null
   */
  Set<String> getComponentsInState(ComponentLifecycleState state);

  /**
   * Checks if a component is currently running.
   *
   * @param componentId the component to check
   * @return true if the component is running, false otherwise
   * @throws IllegalArgumentException if componentId is null or empty
   */
  boolean isComponentRunning(String componentId);

  /**
   * Checks if a component is healthy.
   *
   * @param componentId the component to check
   * @return true if the component is healthy, false otherwise
   * @throws IllegalArgumentException if componentId is null or empty
   */
  boolean isComponentHealthy(String componentId);

  /**
   * Performs health check on a component.
   *
   * @param componentId the component to check
   * @return health check result
   * @throws WasmException if health check fails
   * @throws IllegalArgumentException if componentId is null or empty
   */
  ComponentHealthCheckResult performHealthCheck(String componentId) throws WasmException;

  /**
   * Performs health checks on all managed components.
   *
   * @return map of component IDs to their health check results
   * @throws WasmException if health checks fail
   */
  Map<String, ComponentHealthCheckResult> performHealthCheckAll() throws WasmException;

  /**
   * Enables automatic restart for a component when it fails.
   *
   * @param componentId the component to enable auto-restart for
   * @param policy the restart policy
   * @throws WasmException if auto-restart cannot be enabled
   * @throws IllegalArgumentException if componentId or policy is null
   */
  void enableAutoRestart(String componentId, ComponentRestartPolicy policy) throws WasmException;

  /**
   * Disables automatic restart for a component.
   *
   * @param componentId the component to disable auto-restart for
   * @throws WasmException if auto-restart cannot be disabled
   * @throws IllegalArgumentException if componentId is null or empty
   */
  void disableAutoRestart(String componentId) throws WasmException;

  /**
   * Gets the restart policy for a component.
   *
   * @param componentId the component to check
   * @return the restart policy, or empty if not set
   * @throws IllegalArgumentException if componentId is null or empty
   */
  Optional<ComponentRestartPolicy> getRestartPolicy(String componentId);

  /**
   * Gets lifecycle statistics for a component.
   *
   * @param componentId the component to get statistics for
   * @return lifecycle statistics
   * @throws IllegalArgumentException if componentId is null or empty
   */
  ComponentLifecycleStatistics getLifecycleStatistics(String componentId);

  /**
   * Gets overall lifecycle statistics for all managed components.
   *
   * @return overall lifecycle statistics
   */
  OverallLifecycleStatistics getOverallStatistics();

  /**
   * Shuts down the lifecycle manager and all managed components.
   *
   * @throws WasmException if shutdown fails
   */
  void shutdown() throws WasmException;

  /**
   * Shuts down the lifecycle manager with specific configuration.
   *
   * @param config the shutdown configuration
   * @throws WasmException if shutdown fails
   * @throws IllegalArgumentException if config is null
   */
  void shutdown(LifecycleManagerShutdownConfig config) throws WasmException;

  /** Configuration for component startup. */
  final class ComponentStartupConfig {
    private final long timeoutMillis;
    private final boolean validateDependencies;
    private final boolean waitForDependencies;

    public ComponentStartupConfig(
        long timeoutMillis, boolean validateDependencies, boolean waitForDependencies) {
      this.timeoutMillis = timeoutMillis;
      this.validateDependencies = validateDependencies;
      this.waitForDependencies = waitForDependencies;
    }

    public long getTimeoutMillis() {
      return timeoutMillis;
    }

    public boolean isValidateDependencies() {
      return validateDependencies;
    }

    public boolean isWaitForDependencies() {
      return waitForDependencies;
    }
  }

  /** Configuration for component shutdown. */
  final class ComponentShutdownConfig {
    private final long timeoutMillis;
    private final boolean graceful;
    private final boolean force;

    public ComponentShutdownConfig(long timeoutMillis, boolean graceful, boolean force) {
      this.timeoutMillis = timeoutMillis;
      this.graceful = graceful;
      this.force = force;
    }

    public long getTimeoutMillis() {
      return timeoutMillis;
    }

    public boolean isGraceful() {
      return graceful;
    }

    public boolean isForce() {
      return force;
    }
  }

  /** Configuration for component restart. */
  final class ComponentRestartConfig {
    private final ComponentShutdownConfig shutdownConfig;
    private final ComponentStartupConfig startupConfig;
    private final long delayBetweenMillis;

    public ComponentRestartConfig(
        ComponentShutdownConfig shutdownConfig,
        ComponentStartupConfig startupConfig,
        long delayBetweenMillis) {
      this.shutdownConfig = shutdownConfig;
      this.startupConfig = startupConfig;
      this.delayBetweenMillis = delayBetweenMillis;
    }

    public ComponentShutdownConfig getShutdownConfig() {
      return shutdownConfig;
    }

    public ComponentStartupConfig getStartupConfig() {
      return startupConfig;
    }

    public long getDelayBetweenMillis() {
      return delayBetweenMillis;
    }
  }

  /** Component restart policy. */
  enum ComponentRestartPolicy {
    NEVER,
    ALWAYS,
    ON_FAILURE,
    UNLESS_STOPPED
  }

  /** Component health check result. */
  final class ComponentHealthCheckResult {
    private final boolean healthy;
    private final String message;
    private final Instant timestamp;
    private final Map<String, Object> details;

    public ComponentHealthCheckResult(
        boolean healthy, String message, Instant timestamp, Map<String, Object> details) {
      this.healthy = healthy;
      this.message = message;
      this.timestamp = timestamp;
      this.details = details;
    }

    public boolean isHealthy() {
      return healthy;
    }

    public String getMessage() {
      return message;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public Map<String, Object> getDetails() {
      return details;
    }
  }

  /** Component lifecycle statistics. */
  final class ComponentLifecycleStatistics {
    private final String componentId;
    private final int startCount;
    private final int stopCount;
    private final int restartCount;
    private final int failureCount;
    private final Instant lastStartTime;
    private final Instant lastStopTime;
    private final long totalUptime;

    public ComponentLifecycleStatistics(
        String componentId,
        int startCount,
        int stopCount,
        int restartCount,
        int failureCount,
        Instant lastStartTime,
        Instant lastStopTime,
        long totalUptime) {
      this.componentId = componentId;
      this.startCount = startCount;
      this.stopCount = stopCount;
      this.restartCount = restartCount;
      this.failureCount = failureCount;
      this.lastStartTime = lastStartTime;
      this.lastStopTime = lastStopTime;
      this.totalUptime = totalUptime;
    }

    public String getComponentId() {
      return componentId;
    }

    public int getStartCount() {
      return startCount;
    }

    public int getStopCount() {
      return stopCount;
    }

    public int getRestartCount() {
      return restartCount;
    }

    public int getFailureCount() {
      return failureCount;
    }

    public Instant getLastStartTime() {
      return lastStartTime;
    }

    public Instant getLastStopTime() {
      return lastStopTime;
    }

    public long getTotalUptime() {
      return totalUptime;
    }
  }

  /** Overall lifecycle statistics. */
  final class OverallLifecycleStatistics {
    private final int totalComponents;
    private final int runningComponents;
    private final int failedComponents;
    private final long totalUptime;
    private final int totalRestarts;

    public OverallLifecycleStatistics(
        int totalComponents,
        int runningComponents,
        int failedComponents,
        long totalUptime,
        int totalRestarts) {
      this.totalComponents = totalComponents;
      this.runningComponents = runningComponents;
      this.failedComponents = failedComponents;
      this.totalUptime = totalUptime;
      this.totalRestarts = totalRestarts;
    }

    public int getTotalComponents() {
      return totalComponents;
    }

    public int getRunningComponents() {
      return runningComponents;
    }

    public int getFailedComponents() {
      return failedComponents;
    }

    public long getTotalUptime() {
      return totalUptime;
    }

    public int getTotalRestarts() {
      return totalRestarts;
    }
  }

  /** Configuration for lifecycle manager shutdown. */
  final class LifecycleManagerShutdownConfig {
    private final long timeoutMillis;
    private final boolean graceful;
    private final boolean forceStopComponents;

    public LifecycleManagerShutdownConfig(
        long timeoutMillis, boolean graceful, boolean forceStopComponents) {
      this.timeoutMillis = timeoutMillis;
      this.graceful = graceful;
      this.forceStopComponents = forceStopComponents;
    }

    public long getTimeoutMillis() {
      return timeoutMillis;
    }

    public boolean isGraceful() {
      return graceful;
    }

    public boolean isForceStopComponents() {
      return forceStopComponents;
    }
  }
}
