package ai.tegmentum.wasmtime4j.resource;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Enterprise-grade resource monitor providing real-time monitoring and metrics collection.
 *
 * <p>ResourceMonitor provides comprehensive monitoring capabilities for tracking resource
 * usage, performance metrics, health status, and operational patterns. It supports
 * configurable monitoring intervals, alerting thresholds, and historical data collection.
 *
 * <p>Key features:
 * - Real-time resource usage monitoring and metrics collection
 * - Configurable alert thresholds and notification mechanisms
 * - Historical data collection and trend analysis
 * - Health checks and status monitoring
 * - Performance metrics and bottleneck identification
 * - Integration with external monitoring systems
 * - Thread-safe concurrent monitoring operations
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ResourceMonitor monitor = ResourceMonitor.builder()
 *     .withMonitoringInterval(Duration.ofSeconds(30))
 *     .withHistoryRetention(Duration.ofHours(24))
 *     .withMetricsCollection(true)
 *     .withAlertThresholds(AlertThresholds.builder()
 *         .withMemoryUsageThreshold(0.8)
 *         .withResourceCountThreshold(1000)
 *         .build())
 *     .build();
 *
 * // Start monitoring
 * monitor.startMonitoring();
 *
 * // Register alert listeners
 * monitor.addAlertListener(alert -> {
 *     logger.warn("Resource alert: {} - {}", alert.getType(), alert.getMessage());
 * });
 *
 * // Get current metrics
 * ResourceMetrics metrics = monitor.getCurrentMetrics();
 * logger.info("Memory usage: {}MB, Active resources: {}",
 *             metrics.getMemoryUsage() / 1024 / 1024,
 *             metrics.getActiveResourceCount());
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ResourceMonitor extends AutoCloseable {

    /**
     * Creates a new resource monitor builder with default configuration.
     *
     * @return a new builder instance
     */
    static ResourceMonitorBuilder builder() {
        return new ResourceMonitorBuilder();
    }

    /**
     * Creates a resource monitor with default configuration.
     *
     * @return a new resource monitor instance
     * @throws WasmException if monitor creation fails
     */
    static ResourceMonitor create() throws WasmException {
        return builder().build();
    }

    /**
     * Starts the resource monitoring process.
     *
     * <p>This method begins periodic collection of resource metrics and
     * health status information. Monitoring continues until explicitly
     * stopped or the monitor is closed.
     *
     * @throws WasmException if monitoring startup fails
     * @throws IllegalStateException if monitoring is already started
     */
    void startMonitoring() throws WasmException;

    /**
     * Stops the resource monitoring process.
     *
     * <p>This method stops periodic metric collection but retains
     * historical data according to the retention policy.
     *
     * @throws WasmException if monitoring shutdown fails
     * @throws IllegalStateException if monitoring is not started
     */
    void stopMonitoring() throws WasmException;

    /**
     * Checks if monitoring is currently active.
     *
     * @return true if monitoring is active, false otherwise
     */
    boolean isMonitoring();

    /**
     * Gets the current resource metrics snapshot.
     *
     * @return current resource metrics
     * @throws WasmException if metrics collection fails
     */
    ResourceMetrics getCurrentMetrics() throws WasmException;

    /**
     * Gets resource metrics for a specific time range.
     *
     * @param startTime the start of the time range
     * @param endTime the end of the time range
     * @return list of resource metrics snapshots within the time range
     * @throws WasmException if metrics retrieval fails
     * @throws IllegalArgumentException if startTime or endTime is null, or startTime is after endTime
     */
    List<ResourceMetrics> getMetricsHistory(final Instant startTime, final Instant endTime)
            throws WasmException;

    /**
     * Gets recent resource metrics for the specified duration.
     *
     * @param duration the duration to look back from now
     * @return list of resource metrics snapshots within the duration
     * @throws WasmException if metrics retrieval fails
     * @throws IllegalArgumentException if duration is null or negative
     */
    List<ResourceMetrics> getRecentMetrics(final Duration duration) throws WasmException;

    /**
     * Gets aggregated metrics for a specific time period.
     *
     * @param startTime the start of the time range
     * @param endTime the end of the time range
     * @param aggregationType the type of aggregation to perform
     * @return aggregated resource metrics for the time range
     * @throws WasmException if metrics aggregation fails
     * @throws IllegalArgumentException if any parameter is null or startTime is after endTime
     */
    ResourceMetrics getAggregatedMetrics(final Instant startTime, final Instant endTime,
                                        final MetricsAggregationType aggregationType) throws WasmException;

    /**
     * Gets the current health status of all monitored resources.
     *
     * @return current health status
     * @throws WasmException if health check fails
     */
    HealthStatus getCurrentHealthStatus() throws WasmException;

    /**
     * Gets health status for a specific resource type.
     *
     * @param resourceType the resource type to check
     * @return health status for the specified resource type
     * @throws WasmException if health check fails
     * @throws IllegalArgumentException if resourceType is null
     */
    HealthStatus getHealthStatus(final ResourceType resourceType) throws WasmException;

    /**
     * Performs a comprehensive health check of all monitored resources.
     *
     * @return detailed health check results
     * @throws WasmException if health check fails
     */
    HealthCheckResult performHealthCheck() throws WasmException;

    /**
     * Performs an asynchronous health check.
     *
     * @return future containing health check results
     */
    CompletableFuture<HealthCheckResult> performHealthCheckAsync();

    /**
     * Gets performance metrics for resource operations.
     *
     * @return current performance metrics
     * @throws WasmException if performance metrics collection fails
     */
    PerformanceMetrics getPerformanceMetrics() throws WasmException;

    /**
     * Gets performance metrics for a specific resource type.
     *
     * @param resourceType the resource type
     * @return performance metrics for the specified resource type
     * @throws WasmException if performance metrics collection fails
     * @throws IllegalArgumentException if resourceType is null
     */
    PerformanceMetrics getPerformanceMetrics(final ResourceType resourceType) throws WasmException;

    /**
     * Gets current alert status and active alerts.
     *
     * @return list of currently active alerts
     * @throws WasmException if alert status retrieval fails
     */
    List<ResourceAlert> getActiveAlerts() throws WasmException;

    /**
     * Gets alert history for a specific time range.
     *
     * @param startTime the start of the time range
     * @param endTime the end of the time range
     * @return list of alerts within the time range
     * @throws WasmException if alert history retrieval fails
     * @throws IllegalArgumentException if startTime or endTime is null, or startTime is after endTime
     */
    List<ResourceAlert> getAlertHistory(final Instant startTime, final Instant endTime) throws WasmException;

    /**
     * Adds a listener for resource alerts.
     *
     * @param listener the alert listener to add
     * @throws IllegalArgumentException if listener is null
     */
    void addAlertListener(final ResourceAlertListener listener);

    /**
     * Removes a listener for resource alerts.
     *
     * @param listener the alert listener to remove
     * @throws IllegalArgumentException if listener is null
     */
    void removeAlertListener(final ResourceAlertListener listener);

    /**
     * Sets custom alert thresholds for monitoring.
     *
     * @param thresholds the alert thresholds to set
     * @throws WasmException if threshold configuration fails
     * @throws IllegalArgumentException if thresholds is null
     */
    void setAlertThresholds(final AlertThresholds thresholds) throws WasmException;

    /**
     * Gets the current alert thresholds.
     *
     * @return current alert thresholds
     */
    AlertThresholds getAlertThresholds();

    /**
     * Registers a resource manager to be monitored.
     *
     * @param resourceManager the resource manager to monitor
     * @throws WasmException if registration fails
     * @throws IllegalArgumentException if resourceManager is null
     */
    void registerResourceManager(final ResourceManager resourceManager) throws WasmException;

    /**
     * Registers a resource pool to be monitored.
     *
     * @param resourcePool the resource pool to monitor
     * @throws WasmException if registration fails
     * @throws IllegalArgumentException if resourcePool is null
     */
    void registerResourcePool(final ResourcePool resourcePool) throws WasmException;

    /**
     * Registers a resource cache to be monitored.
     *
     * @param resourceCache the resource cache to monitor
     * @throws WasmException if registration fails
     * @throws IllegalArgumentException if resourceCache is null
     */
    void registerResourceCache(final ResourceCache resourceCache) throws WasmException;

    /**
     * Unregisters a resource manager from monitoring.
     *
     * @param resourceManager the resource manager to unregister
     * @throws WasmException if unregistration fails
     * @throws IllegalArgumentException if resourceManager is null
     */
    void unregisterResourceManager(final ResourceManager resourceManager) throws WasmException;

    /**
     * Gets a list of all registered resource managers.
     *
     * @return list of registered resource managers
     */
    List<ResourceManager> getRegisteredResourceManagers();

    /**
     * Gets monitoring statistics and operational metrics.
     *
     * @return monitoring statistics
     */
    MonitoringStatistics getMonitoringStatistics();

    /**
     * Exports monitoring data in various formats.
     *
     * @param format the export format
     * @param startTime the start time for data export
     * @param endTime the end time for data export
     * @return exported monitoring data
     * @throws WasmException if data export fails
     * @throws IllegalArgumentException if any parameter is null or startTime is after endTime
     */
    String exportMonitoringData(final ExportFormat format, final Instant startTime,
                               final Instant endTime) throws WasmException;

    /**
     * Imports monitoring data from external sources.
     *
     * @param format the import format
     * @param data the monitoring data to import
     * @throws WasmException if data import fails
     * @throws IllegalArgumentException if format or data is null
     */
    void importMonitoringData(final ExportFormat format, final String data) throws WasmException;

    /**
     * Clears historical monitoring data.
     *
     * @param olderThan clear data older than this time
     * @return the number of records that were cleared
     * @throws WasmException if data clearing fails
     * @throws IllegalArgumentException if olderThan is null
     */
    int clearHistoricalData(final Instant olderThan) throws WasmException;

    /**
     * Gets the current monitoring configuration.
     *
     * @return the monitoring configuration
     */
    MonitoringConfiguration getConfiguration();

    /**
     * Updates the monitoring configuration.
     *
     * <p>Configuration changes take effect immediately and may trigger
     * restart of monitoring processes if necessary.
     *
     * @param configuration the new monitoring configuration
     * @throws WasmException if configuration update fails
     * @throws IllegalArgumentException if configuration is null
     */
    void updateConfiguration(final MonitoringConfiguration configuration) throws WasmException;

    /**
     * Triggers manual collection of metrics outside the normal schedule.
     *
     * @return the collected metrics
     * @throws WasmException if manual collection fails
     */
    ResourceMetrics collectMetrics() throws WasmException;

    /**
     * Triggers manual collection of metrics asynchronously.
     *
     * @return future containing the collected metrics
     */
    CompletableFuture<ResourceMetrics> collectMetricsAsync();

    /**
     * Checks if the monitor is currently operational.
     *
     * @return true if the monitor is operational, false otherwise
     */
    boolean isOperational();

    /**
     * Gets the time when monitoring was last started.
     *
     * @return the monitoring start time, or null if never started
     */
    Instant getMonitoringStartTime();

    /**
     * Gets the uptime of the monitoring process.
     *
     * @return the monitoring uptime, or Duration.ZERO if not started
     */
    Duration getMonitoringUptime();

    /**
     * Gracefully shuts down the resource monitor.
     *
     * <p>This method will stop monitoring, save any pending data, and
     * clean up resources.
     *
     * @param timeout the maximum time to wait for shutdown
     * @throws WasmException if shutdown fails or times out
     * @throws IllegalArgumentException if timeout is null or negative
     */
    void shutdown(final Duration timeout) throws WasmException;

    /**
     * Asynchronously shuts down the resource monitor.
     *
     * @param timeout the maximum time to wait for shutdown
     * @return future that completes when shutdown is finished
     * @throws IllegalArgumentException if timeout is null or negative
     */
    CompletableFuture<Void> shutdownAsync(final Duration timeout);

    /**
     * Closes the resource monitor and releases all resources immediately.
     */
    @Override
    void close();
}