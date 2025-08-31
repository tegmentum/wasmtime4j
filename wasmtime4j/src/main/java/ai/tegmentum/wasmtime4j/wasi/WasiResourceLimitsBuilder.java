package ai.tegmentum.wasmtime4j.wasi;

import java.time.Duration;

/**
 * Builder for creating WASI resource limits configuration.
 *
 * <p>This builder provides a fluent API for configuring resource limits including memory, CPU
 * time, file handles, network connections, and other system resources.
 *
 * <p>All methods return the builder instance for method chaining. By default, all limits are
 * unlimited unless explicitly configured.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiResourceLimits limits = WasiResourceLimits.builder()
 *     .withMemoryLimit(50 * 1024 * 1024) // 50MB
 *     .withExecutionTimeout(Duration.ofSeconds(10))
 *     .withTotalExecutionTimeout(Duration.ofMinutes(5))
 *     .withFileHandleLimit(20)
 *     .withNetworkConnectionLimit(5)
 *     .withStackDepthLimit(1000)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiResourceLimitsBuilder {

  /**
   * Sets the maximum memory limit in bytes.
   *
   * <p>This limits the total memory that can be allocated by component instances. Memory includes
   * linear memory, stack space, and internal data structures.
   *
   * @param bytes maximum memory in bytes
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is negative or zero
   */
  WasiResourceLimitsBuilder withMemoryLimit(final long bytes);

  /**
   * Removes the memory limit, allowing unlimited memory usage.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutMemoryLimit();

  /**
   * Sets the maximum execution time per function call.
   *
   * <p>Individual function calls that exceed this timeout will be terminated. This helps prevent
   * infinite loops and runaway computations.
   *
   * @param timeout maximum execution time for individual function calls
   * @return this builder for method chaining
   * @throws IllegalArgumentException if timeout is null or negative
   */
  WasiResourceLimitsBuilder withExecutionTimeout(final Duration timeout);

  /**
   * Removes the per-function execution timeout.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutExecutionTimeout();

  /**
   * Sets the maximum total execution time for the entire instance lifetime.
   *
   * <p>This limits the cumulative execution time across all function calls for an instance.
   *
   * @param timeout maximum total execution time for the instance
   * @return this builder for method chaining
   * @throws IllegalArgumentException if timeout is null or negative
   */
  WasiResourceLimitsBuilder withTotalExecutionTimeout(final Duration timeout);

  /**
   * Removes the total execution timeout.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutTotalExecutionTimeout();

  /**
   * Sets the maximum number of file handles that can be open simultaneously.
   *
   * <p>This includes all types of file handles: regular files, directories, pipes, and sockets.
   *
   * @param limit maximum number of open file handles
   * @return this builder for method chaining
   * @throws IllegalArgumentException if limit is negative or zero
   */
  WasiResourceLimitsBuilder withFileHandleLimit(final int limit);

  /**
   * Removes the file handle limit.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutFileHandleLimit();

  /**
   * Sets the maximum number of network connections that can be open simultaneously.
   *
   * <p>This includes TCP connections, UDP sockets, and other network resources.
   *
   * @param limit maximum number of open network connections
   * @return this builder for method chaining
   * @throws IllegalArgumentException if limit is negative or zero
   */
  WasiResourceLimitsBuilder withNetworkConnectionLimit(final int limit);

  /**
   * Removes the network connection limit.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutNetworkConnectionLimit();

  /**
   * Sets the maximum number of threads that can be created.
   *
   * <p>This limits thread creation for components that support threading.
   *
   * @param limit maximum number of threads
   * @return this builder for method chaining
   * @throws IllegalArgumentException if limit is negative or zero
   */
  WasiResourceLimitsBuilder withThreadLimit(final int limit);

  /**
   * Removes the thread limit.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutThreadLimit();

  /**
   * Sets the maximum depth for function call stack.
   *
   * <p>This prevents stack overflow by limiting nested function calls.
   *
   * @param limit maximum stack depth
   * @return this builder for method chaining
   * @throws IllegalArgumentException if limit is negative or zero
   */
  WasiResourceLimitsBuilder withStackDepthLimit(final int limit);

  /**
   * Removes the stack depth limit.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutStackDepthLimit();

  /**
   * Sets the maximum number of resources that can be created.
   *
   * <p>This limits the total number of WASI resources (files, sockets, etc.) that can exist
   * simultaneously.
   *
   * @param limit maximum number of resources
   * @return this builder for method chaining
   * @throws IllegalArgumentException if limit is negative or zero
   */
  WasiResourceLimitsBuilder withResourceCountLimit(final int limit);

  /**
   * Removes the resource count limit.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutResourceCountLimit();

  /**
   * Sets the maximum amount of data that can be written to files.
   *
   * <p>This is a cumulative limit across all file write operations.
   *
   * @param bytes maximum write amount in bytes
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is negative or zero
   */
  WasiResourceLimitsBuilder withFileWriteLimit(final long bytes);

  /**
   * Removes the file write limit.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutFileWriteLimit();

  /**
   * Sets the maximum amount of data that can be read from files.
   *
   * <p>This is a cumulative limit across all file read operations.
   *
   * @param bytes maximum read amount in bytes
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is negative or zero
   */
  WasiResourceLimitsBuilder withFileReadLimit(final long bytes);

  /**
   * Removes the file read limit.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutFileReadLimit();

  /**
   * Sets the maximum amount of data that can be sent over network.
   *
   * <p>This is a cumulative limit across all network send operations.
   *
   * @param bytes maximum send amount in bytes
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is negative or zero
   */
  WasiResourceLimitsBuilder withNetworkSendLimit(final long bytes);

  /**
   * Removes the network send limit.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutNetworkSendLimit();

  /**
   * Sets the maximum amount of data that can be received over network.
   *
   * <p>This is a cumulative limit across all network receive operations.
   *
   * @param bytes maximum receive amount in bytes
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is negative or zero
   */
  WasiResourceLimitsBuilder withNetworkReceiveLimit(final long bytes);

  /**
   * Removes the network receive limit.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutNetworkReceiveLimit();

  /**
   * Sets the maximum CPU time that can be consumed.
   *
   * <p>CPU time is the actual processing time, excluding I/O wait time and other delays.
   *
   * @param timeout maximum CPU time
   * @return this builder for method chaining
   * @throws IllegalArgumentException if timeout is null or negative
   */
  WasiResourceLimitsBuilder withCpuTimeLimit(final Duration timeout);

  /**
   * Removes the CPU time limit.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withoutCpuTimeLimit();

  /**
   * Sets whether to use unlimited resources (removes all limits).
   *
   * <p><b>Warning:</b> Unlimited resources can lead to resource exhaustion and system instability.
   * Use with caution and only in trusted environments.
   *
   * @param unlimited true to remove all limits, false to keep current limits
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder withUnlimited(final boolean unlimited);

  /**
   * Removes all configured limits, making everything unlimited.
   *
   * <p><b>Warning:</b> This removes all resource protection. Use with caution.
   *
   * @return this builder for method chaining
   */
  WasiResourceLimitsBuilder clearAllLimits();

  /**
   * Creates the configured resource limits.
   *
   * <p>This method creates an immutable WasiResourceLimits instance with all the configured
   * settings. The builder can continue to be used to create additional configurations.
   *
   * @return a configured WasiResourceLimits instance
   * @throws IllegalArgumentException if the configuration is invalid
   */
  WasiResourceLimits build();

  /**
   * Validates the current configuration without building it.
   *
   * <p>This method performs validation of all limit settings without creating the actual
   * configuration instance. Useful for early error detection.
   *
   * @throws IllegalArgumentException if validation fails with details about specific issues
   */
  void validate();
}