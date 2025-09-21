package ai.tegmentum.wasmtime4j.wasi;

import java.time.Duration;
import java.util.Optional;

/**
 * Resource limits configuration for WASI components and instances.
 *
 * <p>Resource limits define constraints on system resource usage including memory, CPU time, file
 * handles, network connections, and other resources. These limits help ensure system stability and
 * security by preventing resource exhaustion.
 *
 * <p>Limits are applied during component instantiation and enforced throughout the instance
 * lifecycle. Violations of resource limits result in appropriate exceptions or termination.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiResourceLimits limits = WasiResourceLimits.builder()
 *     .withMemoryLimit(100 * 1024 * 1024) // 100MB
 *     .withExecutionTimeout(Duration.ofSeconds(30))
 *     .withFileHandleLimit(50)
 *     .withNetworkConnectionLimit(10)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiResourceLimits {

  /**
   * Creates a new resource limits builder.
   *
   * @return a new WasiResourceLimitsBuilder instance
   */
  static WasiResourceLimitsBuilder builder() {
    // Use runtime selection pattern to find appropriate implementation
    try {
      // Try Panama implementation first
      final Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.wasi.permission.WasiResourceLimitsBuilder");
      return (WasiResourceLimitsBuilder) builderClass.getDeclaredConstructor().newInstance();
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> builderClass =
            Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.JniWasiResourceLimitsBuilder");
        return (WasiResourceLimitsBuilder) builderClass.getDeclaredConstructor().newInstance();
      } catch (final ClassNotFoundException e2) {
        throw new ai.tegmentum.wasmtime4j.exception.ResourceException(
            "No WasiResourceLimitsBuilder implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create WASI resource limits builder", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create WASI resource limits builder", e);
    }
  }

  /**
   * Creates default resource limits with reasonable constraints.
   *
   * @return default resource limits
   */
  static WasiResourceLimits defaultLimits() {
    return builder().build();
  }

  /**
   * Creates unlimited resource limits (no constraints).
   *
   * <p><b>Warning:</b> Unlimited resources can lead to resource exhaustion and system instability.
   * Use with caution and only in trusted environments.
   *
   * @return unlimited resource limits
   */
  static WasiResourceLimits unlimited() {
    return builder().withUnlimited(true).build();
  }

  /**
   * Gets the maximum memory limit in bytes.
   *
   * @return memory limit in bytes, or empty if unlimited
   */
  Optional<Long> getMemoryLimit();

  /**
   * Gets the maximum execution time per function call.
   *
   * @return execution timeout, or empty if unlimited
   */
  Optional<Duration> getExecutionTimeout();

  /**
   * Gets the maximum total execution time for the instance.
   *
   * @return total execution timeout, or empty if unlimited
   */
  Optional<Duration> getTotalExecutionTimeout();

  /**
   * Gets the maximum number of file handles that can be open simultaneously.
   *
   * @return file handle limit, or empty if unlimited
   */
  Optional<Integer> getFileHandleLimit();

  /**
   * Gets the maximum number of network connections that can be open simultaneously.
   *
   * @return network connection limit, or empty if unlimited
   */
  Optional<Integer> getNetworkConnectionLimit();

  /**
   * Gets the maximum number of threads that can be created.
   *
   * @return thread limit, or empty if unlimited
   */
  Optional<Integer> getThreadLimit();

  /**
   * Gets the maximum depth for function call stack.
   *
   * @return stack depth limit, or empty if unlimited
   */
  Optional<Integer> getStackDepthLimit();

  /**
   * Gets the maximum number of resources that can be created.
   *
   * @return resource count limit, or empty if unlimited
   */
  Optional<Integer> getResourceCountLimit();

  /**
   * Gets the maximum amount of data that can be written to files.
   *
   * @return write limit in bytes, or empty if unlimited
   */
  Optional<Long> getFileWriteLimit();

  /**
   * Gets the maximum amount of data that can be read from files.
   *
   * @return read limit in bytes, or empty if unlimited
   */
  Optional<Long> getFileReadLimit();

  /**
   * Gets the maximum amount of data that can be sent over network.
   *
   * @return network send limit in bytes, or empty if unlimited
   */
  Optional<Long> getNetworkSendLimit();

  /**
   * Gets the maximum amount of data that can be received over network.
   *
   * @return network receive limit in bytes, or empty if unlimited
   */
  Optional<Long> getNetworkReceiveLimit();

  /**
   * Gets the maximum CPU time that can be consumed.
   *
   * @return CPU time limit, or empty if unlimited
   */
  Optional<Duration> getCpuTimeLimit();

  /**
   * Checks if all resource limits are unlimited.
   *
   * @return true if no limits are applied, false otherwise
   */
  boolean isUnlimited();

  /**
   * Checks if this configuration has any limits defined.
   *
   * @return true if at least one limit is defined, false if all are unlimited
   */
  boolean hasLimits();

  /**
   * Creates a new builder based on this configuration.
   *
   * <p>This allows creating modified versions of existing limits while preserving the original.
   *
   * @return a new WasiResourceLimitsBuilder initialized with this configuration's values
   */
  WasiResourceLimitsBuilder toBuilder();

  /**
   * Validates this resource limits configuration.
   *
   * <p>Validation checks include ensuring limits are positive, timeouts are valid, and
   * configuration consistency.
   *
   * @throws IllegalArgumentException if the configuration is invalid
   */
  void validate();

  /**
   * Creates a summary string representation of these limits.
   *
   * <p>The summary includes all configured limits in a human-readable format.
   *
   * @return formatted summary string
   */
  String getSummary();
}
