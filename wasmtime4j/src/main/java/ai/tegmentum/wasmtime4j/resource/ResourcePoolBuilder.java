package ai.tegmentum.wasmtime4j.resource;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;

/**
 * Builder for creating ResourcePool instances with custom configuration.
 *
 * <p>ResourcePoolBuilder provides a fluent API for configuring and creating resource pools with
 * specific requirements and policies.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ResourcePool pool = ResourcePool.builder()
 *     .withConfiguration(PoolConfiguration.builder()
 *         .withMaxPoolSize(20)
 *         .withMinPoolSize(5)
 *         .withAcquisitionTimeout(Duration.ofSeconds(30))
 *         .build())
 *     .withMonitoring(true)
 *     .withJmxExport(true)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class ResourcePoolBuilder {

  private PoolConfiguration configuration = PoolConfiguration.defaultConfiguration();
  private boolean monitoringEnabled = true;
  private boolean jmxExportEnabled = false;
  private String poolName = "DefaultResourcePool";
  private Duration shutdownTimeout = Duration.ofSeconds(30);
  private boolean prePopulate = false;
  private int threadPoolSize = Runtime.getRuntime().availableProcessors();

  /** Package-private constructor - use ResourcePool.builder() to create instances. */
  ResourcePoolBuilder() {}

  /**
   * Sets the pool configuration.
   *
   * @param configuration the pool configuration
   * @return this builder
   * @throws IllegalArgumentException if configuration is null
   */
  public ResourcePoolBuilder withConfiguration(final PoolConfiguration configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }
    this.configuration = configuration;
    return this;
  }

  /**
   * Sets the maximum pool size.
   *
   * @param maxPoolSize the maximum pool size
   * @return this builder
   * @throws IllegalArgumentException if maxPoolSize is less than 1
   */
  public ResourcePoolBuilder withMaxPoolSize(final int maxPoolSize) {
    this.configuration = configuration.toBuilder().withMaxPoolSize(maxPoolSize).build();
    return this;
  }

  /**
   * Sets the minimum pool size.
   *
   * @param minPoolSize the minimum pool size
   * @return this builder
   * @throws IllegalArgumentException if minPoolSize is negative
   */
  public ResourcePoolBuilder withMinPoolSize(final int minPoolSize) {
    this.configuration = configuration.toBuilder().withMinPoolSize(minPoolSize).build();
    return this;
  }

  /**
   * Sets the acquisition timeout.
   *
   * @param acquisitionTimeout the acquisition timeout
   * @return this builder
   * @throws IllegalArgumentException if acquisitionTimeout is null or negative
   */
  public ResourcePoolBuilder withAcquisitionTimeout(final Duration acquisitionTimeout) {
    this.configuration =
        configuration.toBuilder().withAcquisitionTimeout(acquisitionTimeout).build();
    return this;
  }

  /**
   * Sets the maximum idle time for resources.
   *
   * @param maxIdleTime the maximum idle time
   * @return this builder
   * @throws IllegalArgumentException if maxIdleTime is null or negative
   */
  public ResourcePoolBuilder withMaxIdleTime(final Duration maxIdleTime) {
    this.configuration = configuration.toBuilder().withMaxIdleTime(maxIdleTime).build();
    return this;
  }

  /**
   * Sets the validation interval.
   *
   * @param validationInterval the validation interval
   * @return this builder
   * @throws IllegalArgumentException if validationInterval is null or negative
   */
  public ResourcePoolBuilder withValidationInterval(final Duration validationInterval) {
    this.configuration =
        configuration.toBuilder().withValidationInterval(validationInterval).build();
    return this;
  }

  /**
   * Sets the cleanup interval.
   *
   * @param cleanupInterval the cleanup interval
   * @return this builder
   * @throws IllegalArgumentException if cleanupInterval is null or negative
   */
  public ResourcePoolBuilder withCleanupInterval(final Duration cleanupInterval) {
    this.configuration = configuration.toBuilder().withCleanupInterval(cleanupInterval).build();
    return this;
  }

  /**
   * Enables or disables monitoring for the pool.
   *
   * @param monitoringEnabled true to enable monitoring, false to disable
   * @return this builder
   */
  public ResourcePoolBuilder withMonitoring(final boolean monitoringEnabled) {
    this.monitoringEnabled = monitoringEnabled;
    return this;
  }

  /**
   * Enables or disables JMX export for pool metrics.
   *
   * @param jmxExportEnabled true to enable JMX export, false to disable
   * @return this builder
   */
  public ResourcePoolBuilder withJmxExport(final boolean jmxExportEnabled) {
    this.jmxExportEnabled = jmxExportEnabled;
    return this;
  }

  /**
   * Sets the pool name for identification and monitoring.
   *
   * @param poolName the pool name
   * @return this builder
   * @throws IllegalArgumentException if poolName is null or empty
   */
  public ResourcePoolBuilder withPoolName(final String poolName) {
    if (poolName == null || poolName.trim().isEmpty()) {
      throw new IllegalArgumentException("Pool name cannot be null or empty");
    }
    this.poolName = poolName.trim();
    return this;
  }

  /**
   * Sets the shutdown timeout for graceful pool shutdown.
   *
   * @param shutdownTimeout the shutdown timeout
   * @return this builder
   * @throws IllegalArgumentException if shutdownTimeout is null or negative
   */
  public ResourcePoolBuilder withShutdownTimeout(final Duration shutdownTimeout) {
    if (shutdownTimeout == null || shutdownTimeout.isNegative()) {
      throw new IllegalArgumentException("Shutdown timeout must be positive");
    }
    this.shutdownTimeout = shutdownTimeout;
    return this;
  }

  /**
   * Enables or disables pre-population of the pool with minimum resources.
   *
   * @param prePopulate true to pre-populate the pool, false otherwise
   * @return this builder
   */
  public ResourcePoolBuilder withPrePopulate(final boolean prePopulate) {
    this.prePopulate = prePopulate;
    return this;
  }

  /**
   * Sets the size of the internal thread pool for background operations.
   *
   * @param threadPoolSize the thread pool size
   * @return this builder
   * @throws IllegalArgumentException if threadPoolSize is less than 1
   */
  public ResourcePoolBuilder withThreadPoolSize(final int threadPoolSize) {
    if (threadPoolSize < 1) {
      throw new IllegalArgumentException("Thread pool size must be at least 1");
    }
    this.threadPoolSize = threadPoolSize;
    return this;
  }

  /**
   * Enables test on acquire for resources.
   *
   * @return this builder
   */
  public ResourcePoolBuilder withTestOnAcquire() {
    this.configuration = configuration.toBuilder().withTestOnAcquire(true).build();
    return this;
  }

  /**
   * Enables test on return for resources.
   *
   * @return this builder
   */
  public ResourcePoolBuilder withTestOnReturn() {
    this.configuration = configuration.toBuilder().withTestOnReturn(true).build();
    return this;
  }

  /**
   * Enables test while idle for resources.
   *
   * @return this builder
   */
  public ResourcePoolBuilder withTestWhileIdle() {
    this.configuration = configuration.toBuilder().withTestWhileIdle(true).build();
    return this;
  }

  /**
   * Enables fair queueing for waiting threads.
   *
   * @return this builder
   */
  public ResourcePoolBuilder withFairness() {
    this.configuration = configuration.toBuilder().withFairness(true).build();
    return this;
  }

  /**
   * Sets the maximum number of threads that can wait for resources.
   *
   * @param maxWaitingThreads the maximum number of waiting threads
   * @return this builder
   * @throws IllegalArgumentException if maxWaitingThreads is less than 1
   */
  public ResourcePoolBuilder withMaxWaitingThreads(final int maxWaitingThreads) {
    this.configuration = configuration.toBuilder().withMaxWaitingThreads(maxWaitingThreads).build();
    return this;
  }

  /**
   * Builds the resource pool with the configured settings.
   *
   * @return a new resource pool instance
   * @throws WasmException if pool creation fails
   */
  public ResourcePool build() throws WasmException {
    try {
      // This would be implemented to create the actual pool implementation
      // For now, return a placeholder implementation
      throw new WasmException(
          "ResourcePool implementation not yet available - " + "this is a placeholder interface");
    } catch (Exception e) {
      throw new WasmException("Failed to create resource pool: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the current configuration being built.
   *
   * @return the current pool configuration
   */
  public PoolConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Checks if monitoring is enabled.
   *
   * @return true if monitoring is enabled
   */
  public boolean isMonitoringEnabled() {
    return monitoringEnabled;
  }

  /**
   * Checks if JMX export is enabled.
   *
   * @return true if JMX export is enabled
   */
  public boolean isJmxExportEnabled() {
    return jmxExportEnabled;
  }

  /**
   * Gets the pool name.
   *
   * @return the pool name
   */
  public String getPoolName() {
    return poolName;
  }

  /**
   * Gets the shutdown timeout.
   *
   * @return the shutdown timeout
   */
  public Duration getShutdownTimeout() {
    return shutdownTimeout;
  }

  /**
   * Checks if pre-population is enabled.
   *
   * @return true if pre-population is enabled
   */
  public boolean isPrePopulate() {
    return prePopulate;
  }

  /**
   * Gets the thread pool size.
   *
   * @return the thread pool size
   */
  public int getThreadPoolSize() {
    return threadPoolSize;
  }
}
