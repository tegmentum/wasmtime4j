/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.platform;

import ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector;
import ai.tegmentum.wasmtime4j.nativeloader.PlatformFeatureDetector;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Platform-specific optimizations and performance tuning for Wasmtime4j.
 *
 * <p>This class provides platform-specific optimization strategies, performance tuning
 * recommendations, and runtime configuration adjustments based on the detected platform
 * characteristics and capabilities.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Platform-specific performance optimizations
 *   <li>Runtime configuration tuning based on platform capabilities
 *   <li>Memory management optimizations
 *   <li>Thread pool sizing recommendations
 *   <li>Compilation strategy adjustments
 *   <li>Platform-specific feature enablement
 * </ul>
 */
public final class PlatformOptimizations {

  private static final Logger LOGGER = Logger.getLogger(PlatformOptimizations.class.getName());

  /** Cache for optimization configurations. */
  private static volatile OptimizationConfiguration cachedConfiguration;

  /** Platform-specific optimization profiles. */
  public enum OptimizationProfile {
    /** High-performance server configuration */
    HIGH_PERFORMANCE_SERVER,
    /** Development workstation configuration */
    DEVELOPMENT_WORKSTATION,
    /** Container/cloud deployment configuration */
    CONTAINER_OPTIMIZED,
    /** Embedded/IoT device configuration */
    EMBEDDED_DEVICE,
    /** Desktop application configuration */
    DESKTOP_APPLICATION,
    /** Serverless/function execution configuration */
    SERVERLESS_FUNCTION,
    /** Testing/CI environment configuration */
    TESTING_ENVIRONMENT
  }

  /** Memory management strategies. */
  public enum MemoryStrategy {
    /** Aggressive memory usage for maximum performance */
    AGGRESSIVE,
    /** Balanced memory usage */
    BALANCED,
    /** Conservative memory usage for constrained environments */
    CONSERVATIVE,
    /** Adaptive memory usage based on available resources */
    ADAPTIVE
  }

  /** Compilation strategies. */
  public enum CompilationStrategy {
    /** Fast compilation for development */
    FAST_COMPILE,
    /** Balanced compilation time and performance */
    BALANCED_COMPILE,
    /** Aggressive optimization for production */
    OPTIMIZED_COMPILE,
    /** Adaptive compilation based on usage patterns */
    ADAPTIVE_COMPILE
  }

  /** Thread pool strategies. */
  public enum ThreadPoolStrategy {
    /** Fixed thread pool size */
    FIXED,
    /** Dynamic thread pool that adjusts to load */
    DYNAMIC,
    /** Work-stealing thread pool */
    WORK_STEALING,
    /** Single-threaded for constrained environments */
    SINGLE_THREADED
  }

  /** Comprehensive optimization configuration. */
  public static final class OptimizationConfiguration {
    private final OptimizationProfile profile;
    private final MemoryStrategy memoryStrategy;
    private final CompilationStrategy compilationStrategy;
    private final ThreadPoolStrategy threadPoolStrategy;
    private final int compilationThreads;
    private final int executionThreads;
    private final long maxMemoryUsage;
    private final long heapSize;
    private final Duration compilationTimeout;
    private final Duration executionTimeout;
    private final boolean enableSimdOptimizations;
    private final boolean enableAsyncCompilation;
    private final boolean enableModuleCaching;
    private final boolean enableCodeCaching;
    private final boolean enableJitOptimizations;
    private final boolean enableProfileGuidedOptimization;
    private final int optimizationLevel;
    private final Map<String, Object> platformSpecificSettings;
    private final Map<String, String> jvmFlags;
    private final List<String> optimizationHints;

    OptimizationConfiguration(final Builder builder) {
      this.profile = builder.profile;
      this.memoryStrategy = builder.memoryStrategy;
      this.compilationStrategy = builder.compilationStrategy;
      this.threadPoolStrategy = builder.threadPoolStrategy;
      this.compilationThreads = builder.compilationThreads;
      this.executionThreads = builder.executionThreads;
      this.maxMemoryUsage = builder.maxMemoryUsage;
      this.heapSize = builder.heapSize;
      this.compilationTimeout = builder.compilationTimeout;
      this.executionTimeout = builder.executionTimeout;
      this.enableSimdOptimizations = builder.enableSimdOptimizations;
      this.enableAsyncCompilation = builder.enableAsyncCompilation;
      this.enableModuleCaching = builder.enableModuleCaching;
      this.enableCodeCaching = builder.enableCodeCaching;
      this.enableJitOptimizations = builder.enableJitOptimizations;
      this.enableProfileGuidedOptimization = builder.enableProfileGuidedOptimization;
      this.optimizationLevel = builder.optimizationLevel;
      this.platformSpecificSettings = Map.copyOf(builder.platformSpecificSettings);
      this.jvmFlags = Map.copyOf(builder.jvmFlags);
      this.optimizationHints = List.copyOf(builder.optimizationHints);
    }

    // Getters
    public OptimizationProfile getProfile() {
      return profile;
    }

    public MemoryStrategy getMemoryStrategy() {
      return memoryStrategy;
    }

    public CompilationStrategy getCompilationStrategy() {
      return compilationStrategy;
    }

    public ThreadPoolStrategy getThreadPoolStrategy() {
      return threadPoolStrategy;
    }

    public int getCompilationThreads() {
      return compilationThreads;
    }

    public int getExecutionThreads() {
      return executionThreads;
    }

    public long getMaxMemoryUsage() {
      return maxMemoryUsage;
    }

    public long getHeapSize() {
      return heapSize;
    }

    public Duration getCompilationTimeout() {
      return compilationTimeout;
    }

    public Duration getExecutionTimeout() {
      return executionTimeout;
    }

    public boolean isSimdOptimizationsEnabled() {
      return enableSimdOptimizations;
    }

    public boolean isAsyncCompilationEnabled() {
      return enableAsyncCompilation;
    }

    public boolean isModuleCachingEnabled() {
      return enableModuleCaching;
    }

    public boolean isCodeCachingEnabled() {
      return enableCodeCaching;
    }

    public boolean isJitOptimizationsEnabled() {
      return enableJitOptimizations;
    }

    public boolean isProfileGuidedOptimizationEnabled() {
      return enableProfileGuidedOptimization;
    }

    public int getOptimizationLevel() {
      return optimizationLevel;
    }

    public Map<String, Object> getPlatformSpecificSettings() {
      return platformSpecificSettings;
    }

    public Map<String, String> getJvmFlags() {
      return jvmFlags;
    }

    public List<String> getOptimizationHints() {
      return optimizationHints;
    }

    public Optional<Object> getPlatformSpecificSetting(final String key) {
      return Optional.ofNullable(platformSpecificSettings.get(key));
    }

    public Optional<String> getJvmFlag(final String flag) {
      return Optional.ofNullable(jvmFlags.get(flag));
    }

    @Override
    public String toString() {
      return String.format(
          "OptimizationConfiguration{profile=%s, memory=%s, compilation=%s, threads=%s, "
              + "compilationThreads=%d, executionThreads=%d, optimizationLevel=%d}",
          profile,
          memoryStrategy,
          compilationStrategy,
          threadPoolStrategy,
          compilationThreads,
          executionThreads,
          optimizationLevel);
    }

    /** Builder for optimization configuration. */
    public static final class Builder {
      private OptimizationProfile profile = OptimizationProfile.BALANCED;
      private MemoryStrategy memoryStrategy = MemoryStrategy.BALANCED;
      private CompilationStrategy compilationStrategy = CompilationStrategy.BALANCED_COMPILE;
      private ThreadPoolStrategy threadPoolStrategy = ThreadPoolStrategy.DYNAMIC;
      private int compilationThreads = 2;
      private int executionThreads = 4;
      private long maxMemoryUsage = 1024L * 1024 * 1024; // 1GB
      private long heapSize = 512L * 1024 * 1024; // 512MB
      private Duration compilationTimeout = Duration.ofMinutes(5);
      private Duration executionTimeout = Duration.ofSeconds(30);
      private boolean enableSimdOptimizations = false;
      private boolean enableAsyncCompilation = true;
      private boolean enableModuleCaching = true;
      private boolean enableCodeCaching = true;
      private boolean enableJitOptimizations = true;
      private boolean enableProfileGuidedOptimization = false;
      private int optimizationLevel = 2;
      private final Map<String, Object> platformSpecificSettings = new HashMap<>();
      private final Map<String, String> jvmFlags = new HashMap<>();
      private final List<String> optimizationHints = new ArrayList<>();

      public Builder setProfile(final OptimizationProfile profile) {
        this.profile = Objects.requireNonNull(profile);
        return this;
      }

      public Builder setMemoryStrategy(final MemoryStrategy memoryStrategy) {
        this.memoryStrategy = Objects.requireNonNull(memoryStrategy);
        return this;
      }

      public Builder setCompilationStrategy(final CompilationStrategy compilationStrategy) {
        this.compilationStrategy = Objects.requireNonNull(compilationStrategy);
        return this;
      }

      public Builder setThreadPoolStrategy(final ThreadPoolStrategy threadPoolStrategy) {
        this.threadPoolStrategy = Objects.requireNonNull(threadPoolStrategy);
        return this;
      }

      public Builder setCompilationThreads(final int compilationThreads) {
        this.compilationThreads = Math.max(1, compilationThreads);
        return this;
      }

      public Builder setExecutionThreads(final int executionThreads) {
        this.executionThreads = Math.max(1, executionThreads);
        return this;
      }

      public Builder setMaxMemoryUsage(final long maxMemoryUsage) {
        this.maxMemoryUsage = Math.max(1024 * 1024, maxMemoryUsage); // Minimum 1MB
        return this;
      }

      public Builder setHeapSize(final long heapSize) {
        this.heapSize = Math.max(1024 * 1024, heapSize); // Minimum 1MB
        return this;
      }

      public Builder setCompilationTimeout(final Duration compilationTimeout) {
        this.compilationTimeout = Objects.requireNonNull(compilationTimeout);
        return this;
      }

      public Builder setExecutionTimeout(final Duration executionTimeout) {
        this.executionTimeout = Objects.requireNonNull(executionTimeout);
        return this;
      }

      public Builder setSimdOptimizations(final boolean enable) {
        this.enableSimdOptimizations = enable;
        return this;
      }

      public Builder setAsyncCompilation(final boolean enable) {
        this.enableAsyncCompilation = enable;
        return this;
      }

      public Builder setModuleCaching(final boolean enable) {
        this.enableModuleCaching = enable;
        return this;
      }

      public Builder setCodeCaching(final boolean enable) {
        this.enableCodeCaching = enable;
        return this;
      }

      public Builder setJitOptimizations(final boolean enable) {
        this.enableJitOptimizations = enable;
        return this;
      }

      public Builder setProfileGuidedOptimization(final boolean enable) {
        this.enableProfileGuidedOptimization = enable;
        return this;
      }

      public Builder setOptimizationLevel(final int level) {
        this.optimizationLevel = Math.max(0, Math.min(3, level));
        return this;
      }

      public Builder addPlatformSpecificSetting(final String key, final Object value) {
        this.platformSpecificSettings.put(key, value);
        return this;
      }

      public Builder addJvmFlag(final String flag, final String value) {
        this.jvmFlags.put(flag, value);
        return this;
      }

      public Builder addOptimizationHint(final String hint) {
        this.optimizationHints.add(hint);
        return this;
      }

      public OptimizationConfiguration build() {
        return new OptimizationConfiguration(this);
      }
    }
  }

  /** Platform performance characteristics. */
  public static final class PerformanceCharacteristics {
    private final double cpuPerformanceScore;
    private final double memoryBandwidthScore;
    private final double storagePerformanceScore;
    private final double networkPerformanceScore;
    private final boolean hasHighLatencyCpuFeatures;
    private final boolean hasLowLatencyMemory;
    private final boolean hasVectorInstructions;
    private final int recommendedConcurrencyLevel;
    private final Map<String, Double> benchmarkScores;

    PerformanceCharacteristics(
        final double cpuPerformanceScore,
        final double memoryBandwidthScore,
        final double storagePerformanceScore,
        final double networkPerformanceScore,
        final boolean hasHighLatencyCpuFeatures,
        final boolean hasLowLatencyMemory,
        final boolean hasVectorInstructions,
        final int recommendedConcurrencyLevel,
        final Map<String, Double> benchmarkScores) {
      this.cpuPerformanceScore = cpuPerformanceScore;
      this.memoryBandwidthScore = memoryBandwidthScore;
      this.storagePerformanceScore = storagePerformanceScore;
      this.networkPerformanceScore = networkPerformanceScore;
      this.hasHighLatencyCpuFeatures = hasHighLatencyCpuFeatures;
      this.hasLowLatencyMemory = hasLowLatencyMemory;
      this.hasVectorInstructions = hasVectorInstructions;
      this.recommendedConcurrencyLevel = recommendedConcurrencyLevel;
      this.benchmarkScores = Map.copyOf(benchmarkScores);
    }

    public double getCpuPerformanceScore() {
      return cpuPerformanceScore;
    }

    public double getMemoryBandwidthScore() {
      return memoryBandwidthScore;
    }

    public double getStoragePerformanceScore() {
      return storagePerformanceScore;
    }

    public double getNetworkPerformanceScore() {
      return networkPerformanceScore;
    }

    public boolean hasHighLatencyCpuFeatures() {
      return hasHighLatencyCpuFeatures;
    }

    public boolean hasLowLatencyMemory() {
      return hasLowLatencyMemory;
    }

    public boolean hasVectorInstructions() {
      return hasVectorInstructions;
    }

    public int getRecommendedConcurrencyLevel() {
      return recommendedConcurrencyLevel;
    }

    public Map<String, Double> getBenchmarkScores() {
      return benchmarkScores;
    }

    public double getOverallPerformanceScore() {
      return (cpuPerformanceScore
              + memoryBandwidthScore
              + storagePerformanceScore
              + networkPerformanceScore)
          / 4.0;
    }
  }

  /** Private constructor to prevent instantiation of utility class. */
  private PlatformOptimizations() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Gets optimized configuration for the current platform.
   *
   * @return the optimization configuration
   */
  public static OptimizationConfiguration getOptimizedConfiguration() {
    OptimizationConfiguration result = cachedConfiguration;
    if (result == null) {
      synchronized (PlatformOptimizations.class) {
        result = cachedConfiguration;
        if (result == null) {
          result = generateOptimizedConfiguration();
          cachedConfiguration = result;
          LOGGER.info("Generated optimized configuration: " + result);
        }
      }
    }
    return result;
  }

  /**
   * Gets optimized configuration for a specific profile.
   *
   * @param profile the optimization profile
   * @return the optimization configuration
   */
  public static OptimizationConfiguration getOptimizedConfiguration(
      final OptimizationProfile profile) {
    return generateOptimizedConfiguration(profile);
  }

  /**
   * Analyzes platform performance characteristics.
   *
   * @return the performance characteristics
   */
  public static PerformanceCharacteristics analyzePerformanceCharacteristics() {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
    final PlatformFeatureDetector.PlatformFeatures platformFeatures =
        PlatformFeatureDetector.detect();

    return generatePerformanceCharacteristics(platformInfo, platformFeatures);
  }

  /**
   * Applies platform-specific JVM optimizations.
   *
   * @return list of recommended JVM flags
   */
  public static List<String> getRecommendedJvmFlags() {
    final OptimizationConfiguration config = getOptimizedConfiguration();
    final List<String> flags = new ArrayList<>();

    // Add JVM flags from configuration
    config
        .getJvmFlags()
        .forEach(
            (flag, value) -> {
              if (value != null && !value.isEmpty()) {
                flags.add("-" + flag + "=" + value);
              } else {
                flags.add("-" + flag);
              }
            });

    return flags;
  }

  /**
   * Gets platform-specific compiler optimization hints.
   *
   * @return list of optimization hints
   */
  public static List<String> getCompilerOptimizationHints() {
    final OptimizationConfiguration config = getOptimizedConfiguration();
    return config.getOptimizationHints();
  }

  /**
   * Determines the optimal optimization profile for the current platform.
   *
   * @return the optimal profile
   */
  public static OptimizationProfile determineOptimalProfile() {
    final PlatformFeatureDetector.PlatformFeatures features = PlatformFeatureDetector.detect();

    // High-performance server
    if (features.getPhysicalCores() >= 8 && features.getTotalMemory() >= 16L * 1024 * 1024 * 1024) {
      return OptimizationProfile.HIGH_PERFORMANCE_SERVER;
    }

    // Container optimized
    if (features.isContainerized()) {
      return OptimizationProfile.CONTAINER_OPTIMIZED;
    }

    // Development workstation
    if (features.getPhysicalCores() >= 4 && features.getTotalMemory() >= 8L * 1024 * 1024 * 1024) {
      return OptimizationProfile.DEVELOPMENT_WORKSTATION;
    }

    // Embedded device
    if (features.getPhysicalCores() <= 2 && features.getTotalMemory() <= 2L * 1024 * 1024 * 1024) {
      return OptimizationProfile.EMBEDDED_DEVICE;
    }

    // Default to desktop application
    return OptimizationProfile.DESKTOP_APPLICATION;
  }

  /**
   * Generates optimized configuration for the current platform.
   *
   * @return the optimization configuration
   */
  private static OptimizationConfiguration generateOptimizedConfiguration() {
    final OptimizationProfile profile = determineOptimalProfile();
    return generateOptimizedConfiguration(profile);
  }

  /**
   * Generates optimized configuration for a specific profile.
   *
   * @param profile the optimization profile
   * @return the optimization configuration
   */
  private static OptimizationConfiguration generateOptimizedConfiguration(
      final OptimizationProfile profile) {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
    final PlatformFeatureDetector.PlatformFeatures platformFeatures =
        PlatformFeatureDetector.detect();
    final OptimizationConfiguration.Builder builder = new OptimizationConfiguration.Builder();

    builder.setProfile(profile);

    // Configure based on profile
    switch (profile) {
      case HIGH_PERFORMANCE_SERVER:
        configureHighPerformanceServer(builder, platformInfo, platformFeatures);
        break;

      case DEVELOPMENT_WORKSTATION:
        configureDevelopmentWorkstation(builder, platformInfo, platformFeatures);
        break;

      case CONTAINER_OPTIMIZED:
        configureContainerOptimized(builder, platformInfo, platformFeatures);
        break;

      case EMBEDDED_DEVICE:
        configureEmbeddedDevice(builder, platformInfo, platformFeatures);
        break;

      case DESKTOP_APPLICATION:
        configureDesktopApplication(builder, platformInfo, platformFeatures);
        break;

      case SERVERLESS_FUNCTION:
        configureServerlessFunction(builder, platformInfo, platformFeatures);
        break;

      case TESTING_ENVIRONMENT:
        configureTestingEnvironment(builder, platformInfo, platformFeatures);
        break;
    }

    // Apply platform-specific optimizations
    applyPlatformSpecificOptimizations(builder, platformInfo, platformFeatures);

    return builder.build();
  }

  /** Configures high-performance server settings. */
  private static void configureHighPerformanceServer(
      final OptimizationConfiguration.Builder builder,
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    builder
        .setMemoryStrategy(MemoryStrategy.AGGRESSIVE)
        .setCompilationStrategy(CompilationStrategy.OPTIMIZED_COMPILE)
        .setThreadPoolStrategy(ThreadPoolStrategy.WORK_STEALING)
        .setCompilationThreads(Math.max(8, platformFeatures.getPhysicalCores()))
        .setExecutionThreads(platformFeatures.getLogicalCores())
        .setMaxMemoryUsage(platformFeatures.getAvailableMemory() * 3 / 4)
        .setHeapSize(platformFeatures.getAvailableMemory() / 2)
        .setCompilationTimeout(Duration.ofMinutes(15))
        .setExecutionTimeout(Duration.ofMinutes(5))
        .setSimdOptimizations(platformFeatures.shouldEnableSimd())
        .setAsyncCompilation(true)
        .setModuleCaching(true)
        .setCodeCaching(true)
        .setJitOptimizations(true)
        .setProfileGuidedOptimization(true)
        .setOptimizationLevel(3);

    builder
        .addJvmFlag("XX:+UseG1GC", null)
        .addJvmFlag("XX:MaxGCPauseMillis", "200")
        .addJvmFlag("XX:+UseStringDeduplication", null)
        .addJvmFlag("XX:+UseCompressedOops", null);

    builder
        .addOptimizationHint("Enable aggressive inlining")
        .addOptimizationHint("Use SIMD instructions for data parallel operations")
        .addOptimizationHint("Enable profile-guided optimization for hot code paths");
  }

  /** Configures development workstation settings. */
  private static void configureDevelopmentWorkstation(
      final OptimizationConfiguration.Builder builder,
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    builder
        .setMemoryStrategy(MemoryStrategy.BALANCED)
        .setCompilationStrategy(CompilationStrategy.FAST_COMPILE)
        .setThreadPoolStrategy(ThreadPoolStrategy.DYNAMIC)
        .setCompilationThreads(Math.max(2, platformFeatures.getPhysicalCores() / 2))
        .setExecutionThreads(Math.max(4, platformFeatures.getLogicalCores() * 3 / 4))
        .setMaxMemoryUsage(platformFeatures.getAvailableMemory() / 2)
        .setHeapSize(platformFeatures.getAvailableMemory() / 3)
        .setCompilationTimeout(Duration.ofMinutes(5))
        .setExecutionTimeout(Duration.ofSeconds(30))
        .setSimdOptimizations(platformFeatures.shouldEnableSimd())
        .setAsyncCompilation(true)
        .setModuleCaching(true)
        .setCodeCaching(true)
        .setJitOptimizations(true)
        .setProfileGuidedOptimization(false)
        .setOptimizationLevel(1);

    builder.addJvmFlag("XX:+UseParallelGC", null).addJvmFlag("XX:+UseCompressedOops", null);

    builder
        .addOptimizationHint("Prioritize fast compilation for development iteration")
        .addOptimizationHint("Enable module caching to speed up repeated builds");
  }

  /** Configures container-optimized settings. */
  private static void configureContainerOptimized(
      final OptimizationConfiguration.Builder builder,
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    builder
        .setMemoryStrategy(MemoryStrategy.CONSERVATIVE)
        .setCompilationStrategy(CompilationStrategy.BALANCED_COMPILE)
        .setThreadPoolStrategy(ThreadPoolStrategy.FIXED)
        .setCompilationThreads(Math.max(1, platformFeatures.getPhysicalCores() / 2))
        .setExecutionThreads(Math.max(2, platformFeatures.getLogicalCores() / 2))
        .setMaxMemoryUsage(platformFeatures.getAvailableMemory() / 3)
        .setHeapSize(platformFeatures.getAvailableMemory() / 4)
        .setCompilationTimeout(Duration.ofMinutes(3))
        .setExecutionTimeout(Duration.ofSeconds(30))
        .setSimdOptimizations(false) // Conservative for container compatibility
        .setAsyncCompilation(false)
        .setModuleCaching(false) // Avoid persistent state in containers
        .setCodeCaching(true)
        .setJitOptimizations(true)
        .setProfileGuidedOptimization(false)
        .setOptimizationLevel(1);

    builder
        .addJvmFlag("XX:+UseSerialGC", null) // Predictable for containers
        .addJvmFlag("XX:+UnlockExperimentalVMOptions", null)
        .addJvmFlag("XX:+UseContainerSupport", null);

    builder
        .addOptimizationHint("Use conservative memory settings for container deployment")
        .addOptimizationHint("Avoid features that require persistent state");
  }

  /** Configures embedded device settings. */
  private static void configureEmbeddedDevice(
      final OptimizationConfiguration.Builder builder,
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    builder
        .setMemoryStrategy(MemoryStrategy.CONSERVATIVE)
        .setCompilationStrategy(CompilationStrategy.FAST_COMPILE)
        .setThreadPoolStrategy(ThreadPoolStrategy.SINGLE_THREADED)
        .setCompilationThreads(1)
        .setExecutionThreads(Math.max(1, platformFeatures.getLogicalCores()))
        .setMaxMemoryUsage(platformFeatures.getAvailableMemory() / 4)
        .setHeapSize(platformFeatures.getAvailableMemory() / 6)
        .setCompilationTimeout(Duration.ofMinutes(2))
        .setExecutionTimeout(Duration.ofSeconds(10))
        .setSimdOptimizations(false)
        .setAsyncCompilation(false)
        .setModuleCaching(false)
        .setCodeCaching(false)
        .setJitOptimizations(false)
        .setProfileGuidedOptimization(false)
        .setOptimizationLevel(0);

    builder.addJvmFlag("XX:+UseSerialGC", null).addJvmFlag("XX:-UseBiasedLocking", null);

    builder
        .addOptimizationHint("Minimize memory footprint for embedded deployment")
        .addOptimizationHint("Disable advanced features to reduce resource usage");
  }

  /** Configures desktop application settings. */
  private static void configureDesktopApplication(
      final OptimizationConfiguration.Builder builder,
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    builder
        .setMemoryStrategy(MemoryStrategy.BALANCED)
        .setCompilationStrategy(CompilationStrategy.BALANCED_COMPILE)
        .setThreadPoolStrategy(ThreadPoolStrategy.DYNAMIC)
        .setCompilationThreads(Math.max(2, platformFeatures.getPhysicalCores() / 2))
        .setExecutionThreads(Math.max(2, platformFeatures.getLogicalCores() / 2))
        .setMaxMemoryUsage(platformFeatures.getAvailableMemory() / 2)
        .setHeapSize(platformFeatures.getAvailableMemory() / 3)
        .setCompilationTimeout(Duration.ofMinutes(5))
        .setExecutionTimeout(Duration.ofSeconds(30))
        .setSimdOptimizations(platformFeatures.shouldEnableSimd())
        .setAsyncCompilation(true)
        .setModuleCaching(true)
        .setCodeCaching(true)
        .setJitOptimizations(true)
        .setProfileGuidedOptimization(false)
        .setOptimizationLevel(2);

    builder
        .addJvmFlag("XX:+UseG1GC", null)
        .addJvmFlag("XX:MaxGCPauseMillis", "100")
        .addJvmFlag("XX:+UseCompressedOops", null);

    builder
        .addOptimizationHint("Balance performance and resource usage for desktop use")
        .addOptimizationHint("Enable responsive UI thread management");
  }

  /** Configures serverless function settings. */
  private static void configureServerlessFunction(
      final OptimizationConfiguration.Builder builder,
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    builder
        .setMemoryStrategy(MemoryStrategy.ADAPTIVE)
        .setCompilationStrategy(CompilationStrategy.FAST_COMPILE)
        .setThreadPoolStrategy(ThreadPoolStrategy.FIXED)
        .setCompilationThreads(Math.max(1, platformFeatures.getPhysicalCores() / 2))
        .setExecutionThreads(Math.max(2, platformFeatures.getLogicalCores()))
        .setMaxMemoryUsage(platformFeatures.getAvailableMemory() / 2)
        .setHeapSize(platformFeatures.getAvailableMemory() / 3)
        .setCompilationTimeout(Duration.ofSeconds(30))
        .setExecutionTimeout(Duration.ofSeconds(15))
        .setSimdOptimizations(false) // Fast startup priority
        .setAsyncCompilation(false)
        .setModuleCaching(false)
        .setCodeCaching(true)
        .setJitOptimizations(false) // Fast startup priority
        .setProfileGuidedOptimization(false)
        .setOptimizationLevel(0);

    builder
        .addJvmFlag("XX:+UseSerialGC", null)
        .addJvmFlag("XX:TieredStopAtLevel", "1"); // Fast startup

    builder
        .addOptimizationHint("Optimize for fast startup in serverless environments")
        .addOptimizationHint("Minimize initialization overhead");
  }

  /** Configures testing environment settings. */
  private static void configureTestingEnvironment(
      final OptimizationConfiguration.Builder builder,
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    builder
        .setMemoryStrategy(MemoryStrategy.CONSERVATIVE)
        .setCompilationStrategy(CompilationStrategy.FAST_COMPILE)
        .setThreadPoolStrategy(ThreadPoolStrategy.FIXED)
        .setCompilationThreads(Math.max(1, platformFeatures.getPhysicalCores() / 4))
        .setExecutionThreads(Math.max(2, platformFeatures.getLogicalCores() / 2))
        .setMaxMemoryUsage(platformFeatures.getAvailableMemory() / 4)
        .setHeapSize(platformFeatures.getAvailableMemory() / 6)
        .setCompilationTimeout(Duration.ofMinutes(2))
        .setExecutionTimeout(Duration.ofSeconds(10))
        .setSimdOptimizations(false)
        .setAsyncCompilation(false)
        .setModuleCaching(false)
        .setCodeCaching(false)
        .setJitOptimizations(false)
        .setProfileGuidedOptimization(false)
        .setOptimizationLevel(0);

    builder.addJvmFlag("XX:+UseSerialGC", null);

    builder
        .addOptimizationHint("Use deterministic settings for testing")
        .addOptimizationHint("Minimize resource usage to allow parallel test execution");
  }

  /** Applies platform-specific optimizations. */
  private static void applyPlatformSpecificOptimizations(
      final OptimizationConfiguration.Builder builder,
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    // Platform-specific optimizations
    switch (platformInfo.getOperatingSystem()) {
      case LINUX:
        applyLinuxOptimizations(builder, platformFeatures);
        break;

      case WINDOWS:
        applyWindowsOptimizations(builder, platformFeatures);
        break;

      case MACOS:
        applyMacOsOptimizations(builder, platformFeatures);
        break;
    }

    // Architecture-specific optimizations
    switch (platformInfo.getArchitecture()) {
      case X86_64:
        applyX86_64Optimizations(builder, platformFeatures);
        break;

      case AARCH64:
        applyAarch64Optimizations(builder, platformFeatures);
        break;
    }
  }

  /** Applies Linux-specific optimizations. */
  private static void applyLinuxOptimizations(
      final OptimizationConfiguration.Builder builder,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    builder
        .addPlatformSpecificSetting("linux.use_huge_pages", platformFeatures.isHugePagesEnabled())
        .addPlatformSpecificSetting(
            "linux.numa_aware",
            platformFeatures.hasMemoryFeature(PlatformFeatureDetector.MemoryFeature.NUMA));

    if (platformFeatures.hasMemoryFeature(PlatformFeatureDetector.MemoryFeature.HUGE_PAGES)) {
      builder.addJvmFlag("XX:+UseLargePages", null);
      builder.addOptimizationHint("Enable huge pages for improved memory performance");
    }

    if (platformFeatures.hasOsFeature(PlatformFeatureDetector.OsFeature.CGROUPS)) {
      builder.addOptimizationHint("Respect cgroup memory limits in containerized environments");
    }
  }

  /** Applies Windows-specific optimizations. */
  private static void applyWindowsOptimizations(
      final OptimizationConfiguration.Builder builder,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    builder.addPlatformSpecificSetting("windows.use_large_pages", true);

    builder.addJvmFlag("XX:+UseLargePages", null);
    builder.addOptimizationHint("Use Windows large page support for memory performance");
    builder.addOptimizationHint("Consider Windows-specific thread affinity settings");
  }

  /** Applies macOS-specific optimizations. */
  private static void applyMacOsOptimizations(
      final OptimizationConfiguration.Builder builder,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    builder.addPlatformSpecificSetting("macos.use_unified_memory", true);

    // macOS-specific GC tuning
    builder.addJvmFlag("XX:+UseG1GC", null);
    builder.addOptimizationHint("Optimize for macOS unified memory architecture");
    builder.addOptimizationHint("Consider macOS security sandbox restrictions");
  }

  /** Applies x86_64-specific optimizations. */
  private static void applyX86_64Optimizations(
      final OptimizationConfiguration.Builder builder,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    if (platformFeatures.hasCpuFeature(PlatformFeatureDetector.CpuFeature.AVX2)) {
      builder.addPlatformSpecificSetting("x86_64.enable_avx2", true);
      builder.addOptimizationHint("Enable AVX2 optimizations for vector operations");
    }

    if (platformFeatures.hasCpuFeature(PlatformFeatureDetector.CpuFeature.AES)) {
      builder.addPlatformSpecificSetting("x86_64.enable_aes_ni", true);
      builder.addOptimizationHint("Enable AES-NI for cryptographic operations");
    }
  }

  /** Applies ARM64-specific optimizations. */
  private static void applyAarch64Optimizations(
      final OptimizationConfiguration.Builder builder,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    if (platformFeatures.hasCpuFeature(PlatformFeatureDetector.CpuFeature.NEON)) {
      builder.addPlatformSpecificSetting("aarch64.enable_neon", true);
      builder.addOptimizationHint("Enable NEON SIMD optimizations");
    }

    if (platformFeatures.hasCpuFeature(PlatformFeatureDetector.CpuFeature.SVE)) {
      builder.addPlatformSpecificSetting("aarch64.enable_sve", true);
      builder.addOptimizationHint("Enable SVE for advanced vector operations");
    }

    builder.addOptimizationHint("Optimize for ARM64 memory architecture");
  }

  /** Generates performance characteristics for the platform. */
  private static PerformanceCharacteristics generatePerformanceCharacteristics(
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    // Simplified performance scoring based on platform characteristics
    final double cpuScore = calculateCpuPerformanceScore(platformFeatures);
    final double memoryScore = calculateMemoryPerformanceScore(platformFeatures);
    final double storageScore = 0.8; // Placeholder - would need actual storage benchmarking
    final double networkScore = 0.8; // Placeholder - would need actual network benchmarking

    final boolean hasHighLatencyCpuFeatures =
        platformFeatures.hasCpuFeature(PlatformFeatureDetector.CpuFeature.AVX512);
    final boolean hasLowLatencyMemory = !platformFeatures.isVirtualized();
    final boolean hasVectorInstructions = platformFeatures.shouldEnableSimd();

    final int concurrencyLevel = Math.max(2, platformFeatures.getLogicalCores());

    final Map<String, Double> benchmarkScores = new HashMap<>();
    benchmarkScores.put("cpu_score", cpuScore);
    benchmarkScores.put("memory_score", memoryScore);
    benchmarkScores.put("storage_score", storageScore);
    benchmarkScores.put("network_score", networkScore);

    return new PerformanceCharacteristics(
        cpuScore,
        memoryScore,
        storageScore,
        networkScore,
        hasHighLatencyCpuFeatures,
        hasLowLatencyMemory,
        hasVectorInstructions,
        concurrencyLevel,
        benchmarkScores);
  }

  /** Calculates CPU performance score. */
  private static double calculateCpuPerformanceScore(
      final PlatformFeatureDetector.PlatformFeatures features) {
    double score = 0.5; // Base score

    // Add score based on core count
    score += Math.min(0.3, features.getPhysicalCores() * 0.05);

    // Add score for SIMD features
    if (features.hasCpuFeature(PlatformFeatureDetector.CpuFeature.AVX2)) {
      score += 0.1;
    }
    if (features.hasCpuFeature(PlatformFeatureDetector.CpuFeature.NEON)) {
      score += 0.1;
    }

    // Virtualization penalty
    if (features.hasOsFeature(PlatformFeatureDetector.OsFeature.VIRTUALIZED)) {
      score *= 0.9;
    }

    return Math.min(1.0, score);
  }

  /** Calculates memory performance score. */
  private static double calculateMemoryPerformanceScore(
      final PlatformFeatureDetector.PlatformFeatures features) {
    double score = 0.5; // Base score

    // Add score based on memory size
    final long memoryGB = features.getTotalMemory() / (1024L * 1024 * 1024);
    score += Math.min(0.3, memoryGB * 0.02);

    // Add score for memory features
    if (features.hasMemoryFeature(PlatformFeatureDetector.MemoryFeature.HUGE_PAGES)) {
      score += 0.1;
    }
    if (features.hasMemoryFeature(PlatformFeatureDetector.MemoryFeature.NUMA)) {
      score += 0.1;
    }

    // Container penalty
    if (features.isContainerized()) {
      score *= 0.9;
    }

    return Math.min(1.0, score);
  }

  /** Clears the optimization configuration cache. */
  public static void clearOptimizationCache() {
    cachedConfiguration = null;
    LOGGER.fine("Cleared optimization configuration cache");
  }

  /**
   * Gets diagnostic information about platform optimizations.
   *
   * @return diagnostic information
   */
  public static String getDiagnosticInfo() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Platform Optimizations:\n");

    final OptimizationProfile optimalProfile = determineOptimalProfile();
    sb.append("  Optimal profile: ").append(optimalProfile).append("\n");

    final PerformanceCharacteristics performance = analyzePerformanceCharacteristics();
    sb.append("  Performance score: ")
        .append(String.format("%.2f", performance.getOverallPerformanceScore()))
        .append("\n");
    sb.append("  CPU score: ")
        .append(String.format("%.2f", performance.getCpuPerformanceScore()))
        .append("\n");
    sb.append("  Memory score: ")
        .append(String.format("%.2f", performance.getMemoryBandwidthScore()))
        .append("\n");
    sb.append("  Concurrency level: ")
        .append(performance.getRecommendedConcurrencyLevel())
        .append("\n");

    if (cachedConfiguration != null) {
      sb.append("  Cached configuration: ").append(cachedConfiguration).append("\n");
    }

    return sb.toString();
  }
}
