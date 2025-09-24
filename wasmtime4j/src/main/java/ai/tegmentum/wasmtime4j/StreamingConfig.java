package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for streaming WebAssembly compilation.
 *
 * <p>StreamingConfig allows fine-tuning of the streaming compilation process, including buffer
 * sizes, compilation priorities, optimization levels, and resource limits.
 *
 * @since 1.0.0
 */
public final class StreamingConfig {

  /** Default buffer size for streaming compilation (64KB). */
  public static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

  /** Default maximum memory usage for streaming compilation (256MB). */
  public static final long DEFAULT_MAX_MEMORY = 256L * 1024 * 1024;

  /** Default timeout for streaming operations (30 seconds). */
  public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

  private final int bufferSize;
  private final long maxMemoryUsage;
  private final Duration timeout;
  private final CompilationPriority priority;
  private final OptimizationLevel optimizationLevel;
  private final boolean enableProgressiveValidation;
  private final boolean enableHotFunctionDetection;
  private final boolean enableIncrementalCaching;
  private final int maxConcurrentThreads;
  private final Duration progressReportingInterval;
  private final boolean enableSecurityValidation;
  private final Optional<String> moduleNameHint;

  private StreamingConfig(final Builder builder) {
    this.bufferSize = builder.bufferSize;
    this.maxMemoryUsage = builder.maxMemoryUsage;
    this.timeout = builder.timeout;
    this.priority = builder.priority;
    this.optimizationLevel = builder.optimizationLevel;
    this.enableProgressiveValidation = builder.enableProgressiveValidation;
    this.enableHotFunctionDetection = builder.enableHotFunctionDetection;
    this.enableIncrementalCaching = builder.enableIncrementalCaching;
    this.maxConcurrentThreads = builder.maxConcurrentThreads;
    this.progressReportingInterval = builder.progressReportingInterval;
    this.enableSecurityValidation = builder.enableSecurityValidation;
    this.moduleNameHint = builder.moduleNameHint;
  }

  /**
   * Gets the buffer size for streaming operations.
   *
   * @return buffer size in bytes
   */
  public int getBufferSize() {
    return bufferSize;
  }

  /**
   * Gets the maximum memory usage allowed during streaming compilation.
   *
   * @return maximum memory usage in bytes
   */
  public long getMaxMemoryUsage() {
    return maxMemoryUsage;
  }

  /**
   * Gets the timeout for streaming operations.
   *
   * @return timeout duration
   */
  public Duration getTimeout() {
    return timeout;
  }

  /**
   * Gets the compilation priority.
   *
   * @return compilation priority
   */
  public CompilationPriority getPriority() {
    return priority;
  }

  /**
   * Gets the optimization level.
   *
   * @return optimization level
   */
  public OptimizationLevel getOptimizationLevel() {
    return optimizationLevel;
  }

  /**
   * Checks if progressive validation is enabled.
   *
   * <p>Progressive validation validates WebAssembly bytecode as it arrives, allowing early
   * detection of invalid modules.
   *
   * @return true if progressive validation is enabled
   */
  public boolean isProgressiveValidationEnabled() {
    return enableProgressiveValidation;
  }

  /**
   * Checks if hot function detection is enabled.
   *
   * <p>Hot function detection attempts to identify frequently called functions for priority
   * compilation.
   *
   * @return true if hot function detection is enabled
   */
  public boolean isHotFunctionDetectionEnabled() {
    return enableHotFunctionDetection;
  }

  /**
   * Checks if incremental caching is enabled.
   *
   * <p>Incremental caching stores partial compilation results to accelerate subsequent
   * compilations of similar modules.
   *
   * @return true if incremental caching is enabled
   */
  public boolean isIncrementalCachingEnabled() {
    return enableIncrementalCaching;
  }

  /**
   * Gets the maximum number of concurrent threads for compilation.
   *
   * @return maximum concurrent threads
   */
  public int getMaxConcurrentThreads() {
    return maxConcurrentThreads;
  }

  /**
   * Gets the progress reporting interval.
   *
   * @return interval between progress reports
   */
  public Duration getProgressReportingInterval() {
    return progressReportingInterval;
  }

  /**
   * Checks if security validation is enabled.
   *
   * <p>Security validation performs additional checks for potentially malicious WebAssembly code.
   *
   * @return true if security validation is enabled
   */
  public boolean isSecurityValidationEnabled() {
    return enableSecurityValidation;
  }

  /**
   * Gets the module name hint for better error reporting and debugging.
   *
   * @return optional module name hint
   */
  public Optional<String> getModuleNameHint() {
    return moduleNameHint;
  }

  /**
   * Creates a new builder for StreamingConfig.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a StreamingConfig with default settings.
   *
   * @return a StreamingConfig with default values
   */
  public static StreamingConfig defaultConfig() {
    return builder().build();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final StreamingConfig that = (StreamingConfig) obj;
    return bufferSize == that.bufferSize
        && maxMemoryUsage == that.maxMemoryUsage
        && enableProgressiveValidation == that.enableProgressiveValidation
        && enableHotFunctionDetection == that.enableHotFunctionDetection
        && enableIncrementalCaching == that.enableIncrementalCaching
        && maxConcurrentThreads == that.maxConcurrentThreads
        && enableSecurityValidation == that.enableSecurityValidation
        && Objects.equals(timeout, that.timeout)
        && priority == that.priority
        && optimizationLevel == that.optimizationLevel
        && Objects.equals(progressReportingInterval, that.progressReportingInterval)
        && Objects.equals(moduleNameHint, that.moduleNameHint);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        bufferSize,
        maxMemoryUsage,
        timeout,
        priority,
        optimizationLevel,
        enableProgressiveValidation,
        enableHotFunctionDetection,
        enableIncrementalCaching,
        maxConcurrentThreads,
        progressReportingInterval,
        enableSecurityValidation,
        moduleNameHint);
  }

  @Override
  public String toString() {
    return "StreamingConfig{"
        + "bufferSize="
        + bufferSize
        + ", maxMemoryUsage="
        + maxMemoryUsage
        + ", timeout="
        + timeout
        + ", priority="
        + priority
        + ", optimizationLevel="
        + optimizationLevel
        + ", enableProgressiveValidation="
        + enableProgressiveValidation
        + ", enableHotFunctionDetection="
        + enableHotFunctionDetection
        + ", enableIncrementalCaching="
        + enableIncrementalCaching
        + ", maxConcurrentThreads="
        + maxConcurrentThreads
        + ", progressReportingInterval="
        + progressReportingInterval
        + ", enableSecurityValidation="
        + enableSecurityValidation
        + ", moduleNameHint="
        + moduleNameHint
        + '}';
  }

  /** Builder for StreamingConfig. */
  public static final class Builder {
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private long maxMemoryUsage = DEFAULT_MAX_MEMORY;
    private Duration timeout = DEFAULT_TIMEOUT;
    private CompilationPriority priority = CompilationPriority.NORMAL;
    private OptimizationLevel optimizationLevel = OptimizationLevel.SPEED;
    private boolean enableProgressiveValidation = true;
    private boolean enableHotFunctionDetection = false;
    private boolean enableIncrementalCaching = false;
    private int maxConcurrentThreads = Runtime.getRuntime().availableProcessors();
    private Duration progressReportingInterval = Duration.ofMillis(100);
    private boolean enableSecurityValidation = true;
    private Optional<String> moduleNameHint = Optional.empty();

    private Builder() {}

    /**
     * Sets the buffer size for streaming operations.
     *
     * @param bufferSize buffer size in bytes (must be positive)
     * @return this builder
     * @throws IllegalArgumentException if bufferSize is not positive
     */
    public Builder bufferSize(final int bufferSize) {
      if (bufferSize <= 0) {
        throw new IllegalArgumentException("Buffer size must be positive");
      }
      this.bufferSize = bufferSize;
      return this;
    }

    /**
     * Sets the maximum memory usage for streaming compilation.
     *
     * @param maxMemoryUsage maximum memory usage in bytes (must be positive)
     * @return this builder
     * @throws IllegalArgumentException if maxMemoryUsage is not positive
     */
    public Builder maxMemoryUsage(final long maxMemoryUsage) {
      if (maxMemoryUsage <= 0) {
        throw new IllegalArgumentException("Max memory usage must be positive");
      }
      this.maxMemoryUsage = maxMemoryUsage;
      return this;
    }

    /**
     * Sets the timeout for streaming operations.
     *
     * @param timeout timeout duration (must not be null or negative)
     * @return this builder
     * @throws IllegalArgumentException if timeout is null or negative
     */
    public Builder timeout(final Duration timeout) {
      if (timeout == null) {
        throw new IllegalArgumentException("Timeout cannot be null");
      }
      if (timeout.isNegative()) {
        throw new IllegalArgumentException("Timeout cannot be negative");
      }
      this.timeout = timeout;
      return this;
    }

    /**
     * Sets the compilation priority.
     *
     * @param priority compilation priority (must not be null)
     * @return this builder
     * @throws IllegalArgumentException if priority is null
     */
    public Builder priority(final CompilationPriority priority) {
      if (priority == null) {
        throw new IllegalArgumentException("Priority cannot be null");
      }
      this.priority = priority;
      return this;
    }

    /**
     * Sets the optimization level.
     *
     * @param optimizationLevel optimization level (must not be null)
     * @return this builder
     * @throws IllegalArgumentException if optimizationLevel is null
     */
    public Builder optimizationLevel(final OptimizationLevel optimizationLevel) {
      if (optimizationLevel == null) {
        throw new IllegalArgumentException("Optimization level cannot be null");
      }
      this.optimizationLevel = optimizationLevel;
      return this;
    }

    /**
     * Enables or disables progressive validation.
     *
     * @param enable whether to enable progressive validation
     * @return this builder
     */
    public Builder progressiveValidation(final boolean enable) {
      this.enableProgressiveValidation = enable;
      return this;
    }

    /**
     * Enables or disables hot function detection.
     *
     * @param enable whether to enable hot function detection
     * @return this builder
     */
    public Builder hotFunctionDetection(final boolean enable) {
      this.enableHotFunctionDetection = enable;
      return this;
    }

    /**
     * Enables or disables incremental caching.
     *
     * @param enable whether to enable incremental caching
     * @return this builder
     */
    public Builder incrementalCaching(final boolean enable) {
      this.enableIncrementalCaching = enable;
      return this;
    }

    /**
     * Sets the maximum number of concurrent threads for compilation.
     *
     * @param maxConcurrentThreads maximum concurrent threads (must be positive)
     * @return this builder
     * @throws IllegalArgumentException if maxConcurrentThreads is not positive
     */
    public Builder maxConcurrentThreads(final int maxConcurrentThreads) {
      if (maxConcurrentThreads <= 0) {
        throw new IllegalArgumentException("Max concurrent threads must be positive");
      }
      this.maxConcurrentThreads = maxConcurrentThreads;
      return this;
    }

    /**
     * Sets the progress reporting interval.
     *
     * @param interval interval between progress reports (must not be null or negative)
     * @return this builder
     * @throws IllegalArgumentException if interval is null or negative
     */
    public Builder progressReportingInterval(final Duration interval) {
      if (interval == null) {
        throw new IllegalArgumentException("Progress reporting interval cannot be null");
      }
      if (interval.isNegative()) {
        throw new IllegalArgumentException("Progress reporting interval cannot be negative");
      }
      this.progressReportingInterval = interval;
      return this;
    }

    /**
     * Enables or disables security validation.
     *
     * @param enable whether to enable security validation
     * @return this builder
     */
    public Builder securityValidation(final boolean enable) {
      this.enableSecurityValidation = enable;
      return this;
    }

    /**
     * Sets a module name hint for better error reporting.
     *
     * @param moduleNameHint optional module name hint (can be null)
     * @return this builder
     */
    public Builder moduleNameHint(final String moduleNameHint) {
      this.moduleNameHint = Optional.ofNullable(moduleNameHint);
      return this;
    }

    /**
     * Builds the StreamingConfig instance.
     *
     * @return a new StreamingConfig
     */
    public StreamingConfig build() {
      return new StreamingConfig(this);
    }
  }
}