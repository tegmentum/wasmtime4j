package ai.tegmentum.wasmtime4j.compilation;

import ai.tegmentum.wasmtime4j.OptimizationLevel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for tiered compilation with multiple optimization levels.
 *
 * <p>Tiered compilation allows for progressively optimizing WebAssembly modules
 * based on execution frequency and performance characteristics. Code starts at
 * lower optimization levels for fast startup and is recompiled at higher levels
 * as it becomes hot.
 *
 * @since 1.0.0
 */
public final class TieredCompilationConfig {

  private final Map<CompilationTier, TierConfig> tierConfigs;
  private final boolean enabled;
  private final long initialCompilationTimeoutMs;
  private final long recompilationTimeoutMs;
  private final int maxConcurrentRecompilations;
  private final CompilationBackgroundThreads backgroundThreads;
  private final boolean profileBasedTriggers;
  private final boolean adaptiveThresholds;
  private final DeoptimizationConfig deoptimizationConfig;

  private TieredCompilationConfig(final Builder builder) {
    this.enabled = builder.enabled;
    this.initialCompilationTimeoutMs = builder.initialCompilationTimeoutMs;
    this.recompilationTimeoutMs = builder.recompilationTimeoutMs;
    this.maxConcurrentRecompilations = builder.maxConcurrentRecompilations;
    this.backgroundThreads = builder.backgroundThreads;
    this.profileBasedTriggers = builder.profileBasedTriggers;
    this.adaptiveThresholds = builder.adaptiveThresholds;
    this.deoptimizationConfig = builder.deoptimizationConfig;

    this.tierConfigs = new EnumMap<>(CompilationTier.class);
    for (final Map.Entry<CompilationTier, TierConfig> entry : builder.tierConfigs.entrySet()) {
      this.tierConfigs.put(entry.getKey(), new TierConfig(entry.getValue()));
    }
  }

  /**
   * Gets the configuration for a specific compilation tier.
   *
   * @param tier the compilation tier
   * @return configuration for the tier
   * @throws IllegalArgumentException if tier is null
   */
  public TierConfig getTierConfig(final CompilationTier tier) {
    if (tier == null) {
      throw new IllegalArgumentException("Compilation tier cannot be null");
    }
    return tierConfigs.get(tier);
  }

  /**
   * Checks if tiered compilation is enabled.
   *
   * @return true if tiered compilation is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Gets the initial compilation timeout.
   *
   * @return timeout in milliseconds
   */
  public long getInitialCompilationTimeoutMs() {
    return initialCompilationTimeoutMs;
  }

  /**
   * Gets the recompilation timeout.
   *
   * @return timeout in milliseconds
   */
  public long getRecompilationTimeoutMs() {
    return recompilationTimeoutMs;
  }

  /**
   * Gets the maximum number of concurrent recompilations.
   *
   * @return maximum concurrent recompilations
   */
  public int getMaxConcurrentRecompilations() {
    return maxConcurrentRecompilations;
  }

  /**
   * Gets the background thread configuration.
   *
   * @return background thread configuration
   */
  public CompilationBackgroundThreads getBackgroundThreads() {
    return backgroundThreads;
  }

  /**
   * Checks if profile-based triggers are enabled.
   *
   * @return true if profile-based triggers are enabled
   */
  public boolean isProfileBasedTriggers() {
    return profileBasedTriggers;
  }

  /**
   * Checks if adaptive thresholds are enabled.
   *
   * @return true if adaptive thresholds are enabled
   */
  public boolean isAdaptiveThresholds() {
    return adaptiveThresholds;
  }

  /**
   * Gets the deoptimization configuration.
   *
   * @return deoptimization configuration
   */
  public DeoptimizationConfig getDeoptimizationConfig() {
    return deoptimizationConfig;
  }

  /**
   * Gets all configured compilation tiers.
   *
   * @return list of compilation tiers
   */
  public List<CompilationTier> getConfiguredTiers() {
    final List<CompilationTier> tiers = new ArrayList<>(tierConfigs.keySet());
    Collections.sort(tiers);
    return Collections.unmodifiableList(tiers);
  }

  /**
   * Creates a new builder for tiered compilation configuration.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default tiered compilation configuration.
   *
   * @return default configuration
   */
  public static TieredCompilationConfig createDefault() {
    return builder()
        .enabled(true)
        .addTier(CompilationTier.BASELINE, TierConfig.createBaseline())
        .addTier(CompilationTier.OPTIMIZED, TierConfig.createOptimized())
        .addTier(CompilationTier.HIGHLY_OPTIMIZED, TierConfig.createHighlyOptimized())
        .build();
  }

  /**
   * Creates a performance-focused tiered compilation configuration.
   *
   * @return performance-focused configuration
   */
  public static TieredCompilationConfig createPerformanceFocused() {
    return builder()
        .enabled(true)
        .addTier(CompilationTier.BASELINE, TierConfig.createFastBaseline())
        .addTier(CompilationTier.OPTIMIZED, TierConfig.createOptimized())
        .addTier(CompilationTier.HIGHLY_OPTIMIZED, TierConfig.createHighlyOptimized())
        .addTier(CompilationTier.MAXIMUM_OPTIMIZATION, TierConfig.createMaximumOptimization())
        .profileBasedTriggers(true)
        .adaptiveThresholds(true)
        .maxConcurrentRecompilations(4)
        .build();
  }

  /**
   * Builder for TieredCompilationConfig.
   */
  public static final class Builder {
    private final Map<CompilationTier, TierConfig> tierConfigs = new EnumMap<>(CompilationTier.class);
    private boolean enabled = true;
    private long initialCompilationTimeoutMs = TimeUnit.SECONDS.toMillis(30);
    private long recompilationTimeoutMs = TimeUnit.MINUTES.toMillis(5);
    private int maxConcurrentRecompilations = 2;
    private CompilationBackgroundThreads backgroundThreads = CompilationBackgroundThreads.AUTO;
    private boolean profileBasedTriggers = false;
    private boolean adaptiveThresholds = false;
    private DeoptimizationConfig deoptimizationConfig = DeoptimizationConfig.createDefault();

    private Builder() {}

    /**
     * Enables or disables tiered compilation.
     *
     * @param enabled true to enable tiered compilation
     * @return this builder for method chaining
     */
    public Builder enabled(final boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Adds a tier configuration.
     *
     * @param tier the compilation tier
     * @param config the tier configuration
     * @return this builder for method chaining
     * @throws IllegalArgumentException if tier or config is null
     */
    public Builder addTier(final CompilationTier tier, final TierConfig config) {
      if (tier == null) {
        throw new IllegalArgumentException("Compilation tier cannot be null");
      }
      if (config == null) {
        throw new IllegalArgumentException("Tier configuration cannot be null");
      }
      this.tierConfigs.put(tier, config);
      return this;
    }

    /**
     * Sets the initial compilation timeout.
     *
     * @param timeoutMs timeout in milliseconds
     * @return this builder for method chaining
     * @throws IllegalArgumentException if timeout is negative
     */
    public Builder initialCompilationTimeout(final long timeoutMs) {
      if (timeoutMs < 0) {
        throw new IllegalArgumentException("Initial compilation timeout cannot be negative");
      }
      this.initialCompilationTimeoutMs = timeoutMs;
      return this;
    }

    /**
     * Sets the recompilation timeout.
     *
     * @param timeoutMs timeout in milliseconds
     * @return this builder for method chaining
     * @throws IllegalArgumentException if timeout is negative
     */
    public Builder recompilationTimeout(final long timeoutMs) {
      if (timeoutMs < 0) {
        throw new IllegalArgumentException("Recompilation timeout cannot be negative");
      }
      this.recompilationTimeoutMs = timeoutMs;
      return this;
    }

    /**
     * Sets the maximum number of concurrent recompilations.
     *
     * @param maxConcurrent maximum concurrent recompilations
     * @return this builder for method chaining
     * @throws IllegalArgumentException if maxConcurrent is non-positive
     */
    public Builder maxConcurrentRecompilations(final int maxConcurrent) {
      if (maxConcurrent <= 0) {
        throw new IllegalArgumentException("Maximum concurrent recompilations must be positive");
      }
      this.maxConcurrentRecompilations = maxConcurrent;
      return this;
    }

    /**
     * Sets the background thread configuration.
     *
     * @param backgroundThreads background thread configuration
     * @return this builder for method chaining
     * @throws IllegalArgumentException if backgroundThreads is null
     */
    public Builder backgroundThreads(final CompilationBackgroundThreads backgroundThreads) {
      if (backgroundThreads == null) {
        throw new IllegalArgumentException("Background threads configuration cannot be null");
      }
      this.backgroundThreads = backgroundThreads;
      return this;
    }

    /**
     * Enables or disables profile-based triggers.
     *
     * @param enabled true to enable profile-based triggers
     * @return this builder for method chaining
     */
    public Builder profileBasedTriggers(final boolean enabled) {
      this.profileBasedTriggers = enabled;
      return this;
    }

    /**
     * Enables or disables adaptive thresholds.
     *
     * @param enabled true to enable adaptive thresholds
     * @return this builder for method chaining
     */
    public Builder adaptiveThresholds(final boolean enabled) {
      this.adaptiveThresholds = enabled;
      return this;
    }

    /**
     * Sets the deoptimization configuration.
     *
     * @param config deoptimization configuration
     * @return this builder for method chaining
     * @throws IllegalArgumentException if config is null
     */
    public Builder deoptimizationConfig(final DeoptimizationConfig config) {
      if (config == null) {
        throw new IllegalArgumentException("Deoptimization configuration cannot be null");
      }
      this.deoptimizationConfig = config;
      return this;
    }

    /**
     * Builds the tiered compilation configuration.
     *
     * @return tiered compilation configuration
     * @throws IllegalStateException if no tiers are configured and compilation is enabled
     */
    public TieredCompilationConfig build() {
      if (enabled && tierConfigs.isEmpty()) {
        throw new IllegalStateException("At least one tier must be configured when tiered compilation is enabled");
      }
      return new TieredCompilationConfig(this);
    }
  }
}


/**
 * Configuration for a specific compilation tier.
 */
final class TierConfig {
  private final OptimizationLevel optimizationLevel;
  private final long executionThreshold;
  private final long timeThresholdMs;
  private final double cpuUtilizationThreshold;
  private final List<String> enabledOptimizations;
  private final Map<String, String> craneliftFlags;
  private final boolean enableSpeculativeOptimization;
  private final boolean enableVectorization;
  private final boolean enableInlining;

  private TierConfig(final Builder builder) {
    this.optimizationLevel = builder.optimizationLevel;
    this.executionThreshold = builder.executionThreshold;
    this.timeThresholdMs = builder.timeThresholdMs;
    this.cpuUtilizationThreshold = builder.cpuUtilizationThreshold;
    this.enabledOptimizations = Collections.unmodifiableList(new ArrayList<>(builder.enabledOptimizations));
    this.craneliftFlags = Collections.unmodifiableMap(new HashMap<>(builder.craneliftFlags));
    this.enableSpeculativeOptimization = builder.enableSpeculativeOptimization;
    this.enableVectorization = builder.enableVectorization;
    this.enableInlining = builder.enableInlining;
  }

  public TierConfig(final TierConfig other) {
    this.optimizationLevel = other.optimizationLevel;
    this.executionThreshold = other.executionThreshold;
    this.timeThresholdMs = other.timeThresholdMs;
    this.cpuUtilizationThreshold = other.cpuUtilizationThreshold;
    this.enabledOptimizations = other.enabledOptimizations;
    this.craneliftFlags = other.craneliftFlags;
    this.enableSpeculativeOptimization = other.enableSpeculativeOptimization;
    this.enableVectorization = other.enableVectorization;
    this.enableInlining = other.enableInlining;
  }

  public OptimizationLevel getOptimizationLevel() { return optimizationLevel; }
  public long getExecutionThreshold() { return executionThreshold; }
  public long getTimeThresholdMs() { return timeThresholdMs; }
  public double getCpuUtilizationThreshold() { return cpuUtilizationThreshold; }
  public List<String> getEnabledOptimizations() { return enabledOptimizations; }
  public Map<String, String> getCraneliftFlags() { return craneliftFlags; }
  public boolean isEnableSpeculativeOptimization() { return enableSpeculativeOptimization; }
  public boolean isEnableVectorization() { return enableVectorization; }
  public boolean isEnableInlining() { return enableInlining; }

  public static TierConfig createBaseline() {
    return builder()
        .optimizationLevel(OptimizationLevel.NONE)
        .executionThreshold(0)
        .timeThresholdMs(0)
        .build();
  }

  public static TierConfig createFastBaseline() {
    return builder()
        .optimizationLevel(OptimizationLevel.NONE)
        .executionThreshold(0)
        .timeThresholdMs(0)
        .addCraneliftFlag("opt_level", "none")
        .addCraneliftFlag("enable_heap_access_spectre_mitigation", "false")
        .build();
  }

  public static TierConfig createOptimized() {
    return builder()
        .optimizationLevel(OptimizationLevel.SPEED)
        .executionThreshold(100)
        .timeThresholdMs(1000)
        .cpuUtilizationThreshold(0.1)
        .enableVectorization(true)
        .enableInlining(true)
        .build();
  }

  public static TierConfig createHighlyOptimized() {
    return builder()
        .optimizationLevel(OptimizationLevel.SPEED_AND_SIZE)
        .executionThreshold(1000)
        .timeThresholdMs(10000)
        .cpuUtilizationThreshold(0.3)
        .enableSpeculativeOptimization(true)
        .enableVectorization(true)
        .enableInlining(true)
        .addOptimization("loop_unrolling")
        .addOptimization("dead_code_elimination")
        .build();
  }

  public static TierConfig createMaximumOptimization() {
    return builder()
        .optimizationLevel(OptimizationLevel.SPEED_AND_SIZE)
        .executionThreshold(10000)
        .timeThresholdMs(60000)
        .cpuUtilizationThreshold(0.5)
        .enableSpeculativeOptimization(true)
        .enableVectorization(true)
        .enableInlining(true)
        .addOptimization("aggressive_loop_unrolling")
        .addOptimization("function_inlining")
        .addOptimization("constant_folding")
        .addOptimization("dead_code_elimination")
        .addOptimization("register_coalescing")
        .addCraneliftFlag("enable_incremental_compilation_cache_checks", "true")
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private OptimizationLevel optimizationLevel = OptimizationLevel.SPEED;
    private long executionThreshold = 100;
    private long timeThresholdMs = 1000;
    private double cpuUtilizationThreshold = 0.1;
    private final List<String> enabledOptimizations = new ArrayList<>();
    private final Map<String, String> craneliftFlags = new HashMap<>();
    private boolean enableSpeculativeOptimization = false;
    private boolean enableVectorization = false;
    private boolean enableInlining = false;

    public Builder optimizationLevel(final OptimizationLevel level) {
      this.optimizationLevel = Objects.requireNonNull(level);
      return this;
    }

    public Builder executionThreshold(final long threshold) {
      this.executionThreshold = threshold;
      return this;
    }

    public Builder timeThresholdMs(final long thresholdMs) {
      this.timeThresholdMs = thresholdMs;
      return this;
    }

    public Builder cpuUtilizationThreshold(final double threshold) {
      this.cpuUtilizationThreshold = threshold;
      return this;
    }

    public Builder addOptimization(final String optimization) {
      this.enabledOptimizations.add(Objects.requireNonNull(optimization));
      return this;
    }

    public Builder addCraneliftFlag(final String name, final String value) {
      this.craneliftFlags.put(Objects.requireNonNull(name), Objects.requireNonNull(value));
      return this;
    }

    public Builder enableSpeculativeOptimization(final boolean enable) {
      this.enableSpeculativeOptimization = enable;
      return this;
    }

    public Builder enableVectorization(final boolean enable) {
      this.enableVectorization = enable;
      return this;
    }

    public Builder enableInlining(final boolean enable) {
      this.enableInlining = enable;
      return this;
    }

    public TierConfig build() {
      return new TierConfig(this);
    }
  }
}

/**
 * Background thread configuration for compilation.
 */
enum CompilationBackgroundThreads {
  /** Automatically determine thread count based on system resources. */
  AUTO,

  /** Use a single background thread for compilation. */
  SINGLE,

  /** Use multiple background threads for compilation. */
  MULTIPLE,

  /** Disable background compilation (compile synchronously). */
  DISABLED
}

/**
 * Deoptimization configuration for speculative optimization.
 */
final class DeoptimizationConfig {
  private final boolean enabled;
  private final long deoptimizationThreshold;
  private final double performanceDegradationThreshold;
  private final boolean enableDeoptimizationTracing;

  private DeoptimizationConfig(final boolean enabled,
                               final long deoptimizationThreshold,
                               final double performanceDegradationThreshold,
                               final boolean enableDeoptimizationTracing) {
    this.enabled = enabled;
    this.deoptimizationThreshold = deoptimizationThreshold;
    this.performanceDegradationThreshold = performanceDegradationThreshold;
    this.enableDeoptimizationTracing = enableDeoptimizationTracing;
  }

  public boolean isEnabled() { return enabled; }
  public long getDeoptimizationThreshold() { return deoptimizationThreshold; }
  public double getPerformanceDegradationThreshold() { return performanceDegradationThreshold; }
  public boolean isEnableDeoptimizationTracing() { return enableDeoptimizationTracing; }

  public static DeoptimizationConfig createDefault() {
    return new DeoptimizationConfig(true, 10, 0.2, false);
  }

  public static DeoptimizationConfig createAggressive() {
    return new DeoptimizationConfig(true, 5, 0.1, true);
  }

  public static DeoptimizationConfig disabled() {
    return new DeoptimizationConfig(false, 0, 0.0, false);
  }
}