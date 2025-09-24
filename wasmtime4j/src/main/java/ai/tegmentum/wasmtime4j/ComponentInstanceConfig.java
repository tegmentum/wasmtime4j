package ai.tegmentum.wasmtime4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration options for WebAssembly component instance creation.
 *
 * <p>This class provides options to customize the behavior of WebAssembly component instances,
 * including resource limits, security settings, and runtime behavior.
 *
 * @since 1.0.0
 */
public final class ComponentInstanceConfig {

  // Resource Management
  private long maxMemorySize = 0; // 0 means unlimited
  private long maxStackSize = 0; // 0 means unlimited
  private int maxInstanceCount = 0; // 0 means unlimited
  private long executionTimeoutMs = 0; // 0 means no timeout

  // Security Settings
  private boolean enableSandbox = true;
  private boolean strictValidation = true;
  private boolean allowHostCalls = false;
  private SecurityLevel securityLevel = SecurityLevel.STRICT;

  // Performance Settings
  private boolean enableOptimizations = true;
  private boolean enableCaching = true;
  private int threadPoolSize = Runtime.getRuntime().availableProcessors();

  // Configuration Properties
  private final Map<String, Object> properties = new HashMap<>();

  /** Creates a new component instance configuration with default settings. */
  public ComponentInstanceConfig() {
    // Default configuration
  }

  /**
   * Creates a new builder for component instance configuration.
   *
   * @return a new ComponentInstanceConfigBuilder
   */
  public static ComponentInstanceConfigBuilder builder() {
    return new ComponentInstanceConfigBuilder();
  }

  /**
   * Sets the maximum memory size for the component instance.
   *
   * @param bytes maximum memory size in bytes (0 for unlimited)
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if bytes is negative
   */
  public ComponentInstanceConfig maxMemorySize(final long bytes) {
    if (bytes < 0) {
      throw new IllegalArgumentException("Maximum memory size cannot be negative");
    }
    this.maxMemorySize = bytes;
    return this;
  }

  /**
   * Sets the execution timeout for the component instance.
   *
   * @param timeoutMs timeout in milliseconds (0 for no timeout)
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if timeoutMs is negative
   */
  public ComponentInstanceConfig executionTimeout(final long timeoutMs) {
    if (timeoutMs < 0) {
      throw new IllegalArgumentException("Execution timeout cannot be negative");
    }
    this.executionTimeoutMs = timeoutMs;
    return this;
  }

  /**
   * Sets the security level for the component instance.
   *
   * @param level the security level
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if level is null
   */
  public ComponentInstanceConfig securityLevel(final SecurityLevel level) {
    if (level == null) {
      throw new IllegalArgumentException("Security level cannot be null");
    }
    this.securityLevel = level;
    return this;
  }

  /**
   * Enables or disables sandbox enforcement.
   *
   * @param enabled true to enable sandbox
   * @return this configuration for method chaining
   */
  public ComponentInstanceConfig enableSandbox(final boolean enabled) {
    this.enableSandbox = enabled;
    return this;
  }

  /**
   * Sets a configuration property.
   *
   * @param key the property key
   * @param value the property value
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if key is null
   */
  public ComponentInstanceConfig setProperty(final String key, final Object value) {
    if (key == null) {
      throw new IllegalArgumentException("Property key cannot be null");
    }
    this.properties.put(key, value);
    return this;
  }

  // Getters

  public long getMaxMemorySize() {
    return maxMemorySize;
  }

  public long getMaxStackSize() {
    return maxStackSize;
  }

  public long getExecutionTimeoutMs() {
    return executionTimeoutMs;
  }

  public SecurityLevel getSecurityLevel() {
    return securityLevel;
  }

  public boolean isEnableSandbox() {
    return enableSandbox;
  }

  public boolean isStrictValidation() {
    return strictValidation;
  }

  public boolean isAllowHostCalls() {
    return allowHostCalls;
  }

  public Map<String, Object> getProperties() {
    return new HashMap<>(properties);
  }

  /** Builder for ComponentInstanceConfig with fluent API. */
  public static final class ComponentInstanceConfigBuilder {
    private final ComponentInstanceConfig config = new ComponentInstanceConfig();

    private ComponentInstanceConfigBuilder() {}

    public ComponentInstanceConfigBuilder maxMemorySize(final long bytes) {
      config.maxMemorySize(bytes);
      return this;
    }

    public ComponentInstanceConfigBuilder executionTimeout(final long timeoutMs) {
      config.executionTimeout(timeoutMs);
      return this;
    }

    public ComponentInstanceConfigBuilder securityLevel(final SecurityLevel level) {
      config.securityLevel(level);
      return this;
    }

    public ComponentInstanceConfigBuilder enableSandbox(final boolean enabled) {
      config.enableSandbox(enabled);
      return this;
    }

    public ComponentInstanceConfigBuilder setProperty(final String key, final Object value) {
      config.setProperty(key, value);
      return this;
    }

    public ComponentInstanceConfig build() {
      return config;
    }
  }

  /** Security levels for component instances. */
  public enum SecurityLevel {
    /** Minimal security checks, maximum performance. */
    PERMISSIVE,

    /** Balanced security and performance. */
    STANDARD,

    /** Maximum security checks, may impact performance. */
    STRICT
  }
}
