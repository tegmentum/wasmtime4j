package ai.tegmentum.wasmtime4j.config.profiles;

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import java.util.HashMap;
import java.util.Map;

/**
 * Predefined performance optimization profiles for common use cases.
 *
 * <p>This enum provides carefully tuned configuration profiles that balance performance, memory
 * usage, compilation time, and debugging capabilities for different application scenarios.
 *
 * @since 1.0.0
 */
public enum PerformanceProfile {

  /**
   * Maximum performance profile optimized for CPU-intensive workloads. Enables aggressive
   * optimizations with higher memory and compilation overhead. Use this for production workloads
   * requiring maximum speed, including real-time, latency-sensitive, and throughput-oriented
   * applications.
   */
  MAXIMUM_PERFORMANCE(
      "Maximum Performance", "Aggressive optimizations for CPU-intensive workloads") {
    @Override
    public EngineConfig applyTo(final EngineConfig config) {
      if (config == null) {
        throw new IllegalArgumentException("Engine configuration cannot be null");
      }
      return config
          .copy()
          .optimizationLevel(OptimizationLevel.SPEED)
          .parallelCompilation(true)
          .craneliftDebugVerifier(false)
          .setGenerateDebugInfo(false)
          .setFuelConsumption(false)
          .setEpochInterruption(false)
          .setCraneliftSettings(createMaxPerformanceSettings());
    }

    private Map<String, String> createMaxPerformanceSettings() {
      final Map<String, String> settings = new HashMap<>();
      settings.put("opt_level", "speed");
      settings.put("enable_verifier", "false");
      settings.put("enable_nan_canonicalization", "false");
      settings.put("enable_jump_threading", "true");
      settings.put("enable_alias_analysis", "true");
      settings.put("enable_llvm_abi_extensions", "true");
      settings.put("regalloc", "backtracking");
      settings.put("enable_probestack", "false");
      return settings;
    }
  },

  /**
   * Balanced performance profile suitable for most production workloads. Provides good performance
   * with reasonable resource usage. This is the recommended default for general-purpose
   * applications.
   */
  BALANCED("Balanced Performance", "Optimized balance of performance and resource usage") {
    @Override
    public EngineConfig applyTo(final EngineConfig config) {
      if (config == null) {
        throw new IllegalArgumentException("Engine configuration cannot be null");
      }
      return config
          .copy()
          .optimizationLevel(OptimizationLevel.SPEED)
          .parallelCompilation(true)
          .craneliftDebugVerifier(false)
          .setGenerateDebugInfo(false)
          .setFuelConsumption(false)
          .setEpochInterruption(false)
          .setCraneliftSettings(createBalancedSettings());
    }

    private Map<String, String> createBalancedSettings() {
      final Map<String, String> settings = new HashMap<>();
      settings.put("opt_level", "speed");
      settings.put("enable_verifier", "false");
      settings.put("enable_nan_canonicalization", "true");
      settings.put("enable_jump_threading", "true");
      settings.put("enable_alias_analysis", "true");
      settings.put("regalloc", "linear_scan");
      return settings;
    }
  },

  /**
   * Memory-optimized profile for resource-constrained environments. Minimizes memory usage and
   * compilation overhead. Suitable for embedded systems, containers with limited memory, mobile
   * applications, and edge computing scenarios.
   */
  MEMORY_OPTIMIZED("Memory Optimized", "Minimized memory usage and compilation overhead") {
    @Override
    public EngineConfig applyTo(final EngineConfig config) {
      if (config == null) {
        throw new IllegalArgumentException("Engine configuration cannot be null");
      }
      return config
          .copy()
          .optimizationLevel(OptimizationLevel.SIZE)
          .parallelCompilation(false)
          .craneliftDebugVerifier(false)
          .setGenerateDebugInfo(false)
          .setFuelConsumption(false)
          .setEpochInterruption(false)
          .setMaxWasmStack(512 * 1024) // 512KB stack limit
          .setCraneliftSettings(createMemoryOptimizedSettings());
    }

    private Map<String, String> createMemoryOptimizedSettings() {
      final Map<String, String> settings = new HashMap<>();
      settings.put("opt_level", "size");
      settings.put("enable_verifier", "false");
      settings.put("enable_nan_canonicalization", "false");
      settings.put("enable_jump_threading", "false");
      settings.put("enable_alias_analysis", "false");
      settings.put("regalloc", "linear_scan");
      settings.put("enable_probestack", "true");
      return settings;
    }
  },

  /**
   * Debug profile for development and troubleshooting. Enables debug information and verification
   * with minimal optimizations. Also suitable for fast compilation during development and CI
   * pipelines.
   */
  DEBUG("Debug", "Development and debugging with fast compilation") {
    @Override
    public EngineConfig applyTo(final EngineConfig config) {
      if (config == null) {
        throw new IllegalArgumentException("Engine configuration cannot be null");
      }
      return config
          .copy()
          .optimizationLevel(OptimizationLevel.NONE)
          .parallelCompilation(true)
          .craneliftDebugVerifier(true)
          .setGenerateDebugInfo(true)
          .setFuelConsumption(true)
          .setEpochInterruption(true)
          .setCraneliftSettings(createDebugSettings());
    }

    private Map<String, String> createDebugSettings() {
      final Map<String, String> settings = new HashMap<>();
      settings.put("opt_level", "none");
      settings.put("enable_verifier", "true");
      settings.put("enable_nan_canonicalization", "true");
      settings.put("enable_jump_threading", "false");
      settings.put("enable_alias_analysis", "false");
      settings.put("regalloc", "linear_scan");
      settings.put("enable_probestack", "true");
      settings.put("enable_bounds_checks", "true");
      return settings;
    }
  };

  private final String displayName;
  private final String description;

  PerformanceProfile(final String displayName, final String description) {
    this.displayName = displayName;
    this.description = description;
  }

  /**
   * Apply this performance profile to an engine configuration.
   *
   * @param config the engine configuration to modify
   * @return the modified engine configuration
   * @throws IllegalArgumentException if config is null
   */
  public abstract EngineConfig applyTo(EngineConfig config);

  /**
   * Get the display name of this performance profile.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Get the description of this performance profile.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Create a new engine configuration with this performance profile applied.
   *
   * @return a new engine configuration with this profile
   */
  public EngineConfig createConfig() {
    return applyTo(new EngineConfig());
  }

  /**
   * Get estimated performance characteristics for this profile.
   *
   * @return performance characteristics
   */
  public PerformanceCharacteristics getPerformanceCharacteristics() {
    switch (this) {
      case MAXIMUM_PERFORMANCE:
        return new PerformanceCharacteristics(95, 30, 80, 70, 40);
      case BALANCED:
        return new PerformanceCharacteristics(80, 60, 70, 75, 65);
      case MEMORY_OPTIMIZED:
        return new PerformanceCharacteristics(60, 90, 85, 50, 85);
      case DEBUG:
        return new PerformanceCharacteristics(30, 50, 95, 90, 70);
      default:
        return new PerformanceCharacteristics(50, 50, 50, 50, 50);
    }
  }

  /**
   * Get the recommended use cases for this performance profile.
   *
   * @return array of recommended use cases
   */
  public String[] getRecommendedUseCases() {
    switch (this) {
      case MAXIMUM_PERFORMANCE:
        return new String[] {
          "High-performance computing applications",
          "Game engines and real-time simulations",
          "Scientific computing workloads",
          "Video/audio processing applications",
          "Real-time trading systems",
          "Batch processing and ETL pipelines"
        };
      case BALANCED:
        return new String[] {
          "General-purpose web applications",
          "Business logic processing",
          "Microservices and API servers",
          "Data processing pipelines"
        };
      case MEMORY_OPTIMIZED:
        return new String[] {
          "Embedded systems and IoT devices",
          "Container environments with limited memory",
          "Mobile applications",
          "Edge computing scenarios"
        };
      case DEBUG:
        return new String[] {
          "Development and debugging",
          "Continuous integration pipelines",
          "Testing and quality assurance",
          "Performance profiling",
          "Rapid prototyping"
        };
      default:
        return new String[] {"General purpose applications"};
    }
  }

  /**
   * Check if this profile is suitable for production environments.
   *
   * @return true if suitable for production
   */
  public boolean isProductionSuitable() {
    switch (this) {
      case MAXIMUM_PERFORMANCE:
      case BALANCED:
      case MEMORY_OPTIMIZED:
        return true;
      case DEBUG:
        return false;
      default:
        return true;
    }
  }

  /**
   * Get profiles suitable for the current runtime environment.
   *
   * @return array of suitable profiles
   */
  public static PerformanceProfile[] getSuitableProfiles() {
    // Detect runtime characteristics and return suitable profiles
    final boolean isLowMemory = Runtime.getRuntime().maxMemory() < 512 * 1024 * 1024; // < 512MB
    final boolean isHighCpu = Runtime.getRuntime().availableProcessors() >= 8;
    final boolean isDevelopment = isDebugModeEnabled();

    if (isDevelopment) {
      return new PerformanceProfile[] {DEBUG, BALANCED};
    } else if (isLowMemory) {
      return new PerformanceProfile[] {MEMORY_OPTIMIZED, BALANCED};
    } else if (isHighCpu) {
      return new PerformanceProfile[] {MAXIMUM_PERFORMANCE, BALANCED};
    } else {
      return new PerformanceProfile[] {BALANCED, MEMORY_OPTIMIZED};
    }
  }

  /**
   * Get the best profile for the current environment.
   *
   * @return recommended profile
   */
  public static PerformanceProfile getRecommendedProfile() {
    final PerformanceProfile[] suitable = getSuitableProfiles();
    return suitable.length > 0 ? suitable[0] : BALANCED;
  }

  /**
   * Compare two profiles by their performance score.
   *
   * @param other the other profile to compare
   * @return comparison result based on performance score
   */
  public int comparePerformance(final PerformanceProfile other) {
    final int thisScore = this.getPerformanceCharacteristics().getPerformanceScore();
    final int otherScore = other.getPerformanceCharacteristics().getPerformanceScore();
    return Integer.compare(thisScore, otherScore);
  }

  private static boolean isDebugModeEnabled() {
    final String debug = System.getProperty("wasmtime4j.debug");
    if (debug != null) {
      return Boolean.parseBoolean(debug);
    }

    // Check for common debug indicators
    return java.lang.management.ManagementFactory.getRuntimeMXBean()
        .getInputArguments()
        .toString()
        .contains("jdwp");
  }

  /** Performance characteristics for a profile. */
  public static final class PerformanceCharacteristics {
    private final int performanceScore;
    private final int memoryEfficiency;
    private final int compilationSpeed;
    private final int securityLevel;
    private final int powerEfficiency;

    /**
     * Creates new performance characteristics.
     *
     * @param performanceScore overall performance score (0-100)
     * @param memoryEfficiency memory efficiency score (0-100)
     * @param compilationSpeed compilation speed score (0-100)
     * @param securityLevel security level score (0-100)
     * @param powerEfficiency power efficiency score (0-100)
     */
    public PerformanceCharacteristics(
        final int performanceScore,
        final int memoryEfficiency,
        final int compilationSpeed,
        final int securityLevel,
        final int powerEfficiency) {
      this.performanceScore = performanceScore;
      this.memoryEfficiency = memoryEfficiency;
      this.compilationSpeed = compilationSpeed;
      this.securityLevel = securityLevel;
      this.powerEfficiency = powerEfficiency;
    }

    /**
     * Get the runtime performance score (0-100, higher is better).
     *
     * @return performance score
     */
    public int getPerformanceScore() {
      return performanceScore;
    }

    /**
     * Get the memory efficiency score (0-100, higher is better).
     *
     * @return memory efficiency score
     */
    public int getMemoryEfficiency() {
      return memoryEfficiency;
    }

    /**
     * Get the compilation speed score (0-100, higher is better).
     *
     * @return compilation speed score
     */
    public int getCompilationSpeed() {
      return compilationSpeed;
    }

    /**
     * Get the security level score (0-100, higher is better).
     *
     * @return security level score
     */
    public int getSecurityLevel() {
      return securityLevel;
    }

    /**
     * Get the power efficiency score (0-100, higher is better).
     *
     * @return power efficiency score
     */
    public int getPowerEfficiency() {
      return powerEfficiency;
    }

    /**
     * Get the overall quality score as a weighted average.
     *
     * @return overall quality score
     */
    public int getOverallScore() {
      // Weighted average: performance 30%, memory 20%, compilation 15%, security 20%, power 15%
      return (int)
          ((performanceScore * 0.30)
              + (memoryEfficiency * 0.20)
              + (compilationSpeed * 0.15)
              + (securityLevel * 0.20)
              + (powerEfficiency * 0.15));
    }

    @Override
    public String toString() {
      return String.format(
          "PerformanceCharacteristics{performance=%d, memory=%d, compilation=%d, security=%d,"
              + " power=%d, overall=%d}",
          performanceScore,
          memoryEfficiency,
          compilationSpeed,
          securityLevel,
          powerEfficiency,
          getOverallScore());
    }
  }
}
