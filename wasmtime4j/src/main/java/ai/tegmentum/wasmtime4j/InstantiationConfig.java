package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for streaming WebAssembly instantiation.
 *
 * <p>InstantiationConfig allows fine-tuning of the streaming instantiation process, including
 * function compilation priorities, memory allocation strategies, and performance optimizations.
 *
 * @since 1.0.0
 */
public final class InstantiationConfig {

  /** Default timeout for instantiation operations (15 seconds). */
  public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

  /** Default maximum memory usage for instantiation (128MB). */
  public static final long DEFAULT_MAX_MEMORY = 128L * 1024 * 1024;

  private final Duration timeout;
  private final long maxMemoryUsage;
  private final InstantiationPriority priority;
  private final boolean enableLazyFunctionCompilation;
  private final boolean enableHotFunctionPriority;
  private final List<String> priorityFunctions;
  private final boolean enableProgressiveMemoryAllocation;
  private final int maxConcurrentThreads;
  private final Duration progressReportingInterval;
  private final boolean enableInstancePooling;
  private final Optional<String> instanceNameHint;
  private final boolean enableStartFunctionExecution;
  private final long initialMemoryPages;
  private final long maximumMemoryPages;

  private InstantiationConfig(final Builder builder) {
    this.timeout = builder.timeout;
    this.maxMemoryUsage = builder.maxMemoryUsage;
    this.priority = builder.priority;
    this.enableLazyFunctionCompilation = builder.enableLazyFunctionCompilation;
    this.enableHotFunctionPriority = builder.enableHotFunctionPriority;
    this.priorityFunctions = List.copyOf(builder.priorityFunctions);
    this.enableProgressiveMemoryAllocation = builder.enableProgressiveMemoryAllocation;
    this.maxConcurrentThreads = builder.maxConcurrentThreads;
    this.progressReportingInterval = builder.progressReportingInterval;
    this.enableInstancePooling = builder.enableInstancePooling;
    this.instanceNameHint = builder.instanceNameHint;
    this.enableStartFunctionExecution = builder.enableStartFunctionExecution;
    this.initialMemoryPages = builder.initialMemoryPages;
    this.maximumMemoryPages = builder.maximumMemoryPages;
  }

  /**
   * Gets the timeout for instantiation operations.
   *
   * @return timeout duration
   */
  public Duration getTimeout() {
    return timeout;
  }

  /**
   * Gets the maximum memory usage allowed during instantiation.
   *
   * @return maximum memory usage in bytes
   */
  public long getMaxMemoryUsage() {
    return maxMemoryUsage;
  }

  /**
   * Gets the instantiation priority.
   *
   * @return instantiation priority
   */
  public InstantiationPriority getPriority() {
    return priority;
  }

  /**
   * Checks if lazy function compilation is enabled.
   *
   * <p>Lazy function compilation defers compilation of functions until they are first called,
   * reducing instantiation time for modules with many functions.
   *
   * @return true if lazy function compilation is enabled
   */
  public boolean isLazyFunctionCompilationEnabled() {
    return enableLazyFunctionCompilation;
  }

  /**
   * Checks if hot function priority is enabled.
   *
   * <p>Hot function priority prioritizes compilation of frequently called functions based on
   * profiling data or hints.
   *
   * @return true if hot function priority is enabled
   */
  public boolean isHotFunctionPriorityEnabled() {
    return enableHotFunctionPriority;
  }

  /**
   * Gets the list of priority functions to compile first.
   *
   * @return list of function names to prioritize
   */
  public List<String> getPriorityFunctions() {
    return priorityFunctions;
  }

  /**
   * Checks if progressive memory allocation is enabled.
   *
   * <p>Progressive memory allocation allocates WebAssembly linear memory incrementally as needed,
   * reducing initial memory overhead.
   *
   * @return true if progressive memory allocation is enabled
   */
  public boolean isProgressiveMemoryAllocationEnabled() {
    return enableProgressiveMemoryAllocation;
  }

  /**
   * Gets the maximum number of concurrent threads for instantiation.
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
   * Checks if instance pooling is enabled.
   *
   * <p>Instance pooling reuses pre-allocated instance resources to accelerate instantiation.
   *
   * @return true if instance pooling is enabled
   */
  public boolean isInstancePoolingEnabled() {
    return enableInstancePooling;
  }

  /**
   * Gets the instance name hint for better debugging and monitoring.
   *
   * @return optional instance name hint
   */
  public Optional<String> getInstanceNameHint() {
    return instanceNameHint;
  }

  /**
   * Checks if start function execution is enabled.
   *
   * <p>When enabled, the module's start function (if present) will be executed during
   * instantiation.
   *
   * @return true if start function execution is enabled
   */
  public boolean isStartFunctionExecutionEnabled() {
    return enableStartFunctionExecution;
  }

  /**
   * Gets the initial number of memory pages to allocate.
   *
   * @return initial memory pages (0 means use module default)
   */
  public long getInitialMemoryPages() {
    return initialMemoryPages;
  }

  /**
   * Gets the maximum number of memory pages allowed.
   *
   * @return maximum memory pages (0 means use module default)
   */
  public long getMaximumMemoryPages() {
    return maximumMemoryPages;
  }

  /**
   * Creates a new builder for InstantiationConfig.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates an InstantiationConfig with default settings.
   *
   * @return an InstantiationConfig with default values
   */
  public static InstantiationConfig defaultConfig() {
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
    final InstantiationConfig that = (InstantiationConfig) obj;
    return maxMemoryUsage == that.maxMemoryUsage
        && enableLazyFunctionCompilation == that.enableLazyFunctionCompilation
        && enableHotFunctionPriority == that.enableHotFunctionPriority
        && enableProgressiveMemoryAllocation == that.enableProgressiveMemoryAllocation
        && maxConcurrentThreads == that.maxConcurrentThreads
        && enableInstancePooling == that.enableInstancePooling
        && enableStartFunctionExecution == that.enableStartFunctionExecution
        && initialMemoryPages == that.initialMemoryPages
        && maximumMemoryPages == that.maximumMemoryPages
        && Objects.equals(timeout, that.timeout)
        && priority == that.priority
        && Objects.equals(priorityFunctions, that.priorityFunctions)
        && Objects.equals(progressReportingInterval, that.progressReportingInterval)
        && Objects.equals(instanceNameHint, that.instanceNameHint);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        timeout,
        maxMemoryUsage,
        priority,
        enableLazyFunctionCompilation,
        enableHotFunctionPriority,
        priorityFunctions,
        enableProgressiveMemoryAllocation,
        maxConcurrentThreads,
        progressReportingInterval,
        enableInstancePooling,
        instanceNameHint,
        enableStartFunctionExecution,
        initialMemoryPages,
        maximumMemoryPages);
  }

  @Override
  public String toString() {
    return "InstantiationConfig{"
        + "timeout="
        + timeout
        + ", maxMemoryUsage="
        + maxMemoryUsage
        + ", priority="
        + priority
        + ", enableLazyFunctionCompilation="
        + enableLazyFunctionCompilation
        + ", enableHotFunctionPriority="
        + enableHotFunctionPriority
        + ", priorityFunctions="
        + priorityFunctions
        + ", enableProgressiveMemoryAllocation="
        + enableProgressiveMemoryAllocation
        + ", maxConcurrentThreads="
        + maxConcurrentThreads
        + ", progressReportingInterval="
        + progressReportingInterval
        + ", enableInstancePooling="
        + enableInstancePooling
        + ", instanceNameHint="
        + instanceNameHint
        + ", enableStartFunctionExecution="
        + enableStartFunctionExecution
        + ", initialMemoryPages="
        + initialMemoryPages
        + ", maximumMemoryPages="
        + maximumMemoryPages
        + '}';
  }

  /** Builder for InstantiationConfig. */
  public static final class Builder {
    private Duration timeout = DEFAULT_TIMEOUT;
    private long maxMemoryUsage = DEFAULT_MAX_MEMORY;
    private InstantiationPriority priority = InstantiationPriority.NORMAL;
    private boolean enableLazyFunctionCompilation = true;
    private boolean enableHotFunctionPriority = false;
    private List<String> priorityFunctions = List.of();
    private boolean enableProgressiveMemoryAllocation = true;
    private int maxConcurrentThreads = Runtime.getRuntime().availableProcessors();
    private Duration progressReportingInterval = Duration.ofMillis(50);
    private boolean enableInstancePooling = false;
    private Optional<String> instanceNameHint = Optional.empty();
    private boolean enableStartFunctionExecution = true;
    private long initialMemoryPages = 0;
    private long maximumMemoryPages = 0;

    private Builder() {}

    /**
     * Sets the timeout for instantiation operations.
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
     * Sets the maximum memory usage for instantiation.
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
     * Sets the instantiation priority.
     *
     * @param priority instantiation priority (must not be null)
     * @return this builder
     * @throws IllegalArgumentException if priority is null
     */
    public Builder priority(final InstantiationPriority priority) {
      if (priority == null) {
        throw new IllegalArgumentException("Priority cannot be null");
      }
      this.priority = priority;
      return this;
    }

    /**
     * Enables or disables lazy function compilation.
     *
     * @param enable whether to enable lazy function compilation
     * @return this builder
     */
    public Builder lazyFunctionCompilation(final boolean enable) {
      this.enableLazyFunctionCompilation = enable;
      return this;
    }

    /**
     * Enables or disables hot function priority.
     *
     * @param enable whether to enable hot function priority
     * @return this builder
     */
    public Builder hotFunctionPriority(final boolean enable) {
      this.enableHotFunctionPriority = enable;
      return this;
    }

    /**
     * Sets the list of priority functions to compile first.
     *
     * @param priorityFunctions list of function names (must not be null)
     * @return this builder
     * @throws IllegalArgumentException if priorityFunctions is null
     */
    public Builder priorityFunctions(final List<String> priorityFunctions) {
      if (priorityFunctions == null) {
        throw new IllegalArgumentException("Priority functions list cannot be null");
      }
      this.priorityFunctions = priorityFunctions;
      return this;
    }

    /**
     * Enables or disables progressive memory allocation.
     *
     * @param enable whether to enable progressive memory allocation
     * @return this builder
     */
    public Builder progressiveMemoryAllocation(final boolean enable) {
      this.enableProgressiveMemoryAllocation = enable;
      return this;
    }

    /**
     * Sets the maximum number of concurrent threads for instantiation.
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
     * Enables or disables instance pooling.
     *
     * @param enable whether to enable instance pooling
     * @return this builder
     */
    public Builder instancePooling(final boolean enable) {
      this.enableInstancePooling = enable;
      return this;
    }

    /**
     * Sets an instance name hint for better debugging and monitoring.
     *
     * @param instanceNameHint optional instance name hint (can be null)
     * @return this builder
     */
    public Builder instanceNameHint(final String instanceNameHint) {
      this.instanceNameHint = Optional.ofNullable(instanceNameHint);
      return this;
    }

    /**
     * Enables or disables start function execution.
     *
     * @param enable whether to enable start function execution
     * @return this builder
     */
    public Builder startFunctionExecution(final boolean enable) {
      this.enableStartFunctionExecution = enable;
      return this;
    }

    /**
     * Sets the initial number of memory pages to allocate.
     *
     * @param initialMemoryPages initial memory pages (must not be negative)
     * @return this builder
     * @throws IllegalArgumentException if initialMemoryPages is negative
     */
    public Builder initialMemoryPages(final long initialMemoryPages) {
      if (initialMemoryPages < 0) {
        throw new IllegalArgumentException("Initial memory pages cannot be negative");
      }
      this.initialMemoryPages = initialMemoryPages;
      return this;
    }

    /**
     * Sets the maximum number of memory pages allowed.
     *
     * @param maximumMemoryPages maximum memory pages (must not be negative)
     * @return this builder
     * @throws IllegalArgumentException if maximumMemoryPages is negative
     */
    public Builder maximumMemoryPages(final long maximumMemoryPages) {
      if (maximumMemoryPages < 0) {
        throw new IllegalArgumentException("Maximum memory pages cannot be negative");
      }
      this.maximumMemoryPages = maximumMemoryPages;
      return this;
    }

    /**
     * Builds the InstantiationConfig instance.
     *
     * @return a new InstantiationConfig
     */
    public InstantiationConfig build() {
      return new InstantiationConfig(this);
    }
  }
}