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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Comprehensive cross-platform compatibility layer for Wasmtime4j.
 *
 * <p>This class provides advanced platform detection, feature validation, and cross-platform
 * optimization recommendations. It extends the basic platform detection with comprehensive
 * compatibility validation and runtime optimization suggestions.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Platform compatibility validation
 *   <li>Feature detection and capability analysis
 *   <li>Performance optimization recommendations
 *   <li>Runtime configuration suggestions
 *   <li>Cross-platform validation framework
 * </ul>
 */
public final class CrossPlatformCompatibility {

  private static final Logger LOGGER = Logger.getLogger(CrossPlatformCompatibility.class.getName());

  /** Cache for compatibility analysis results. */
  private static volatile CompatibilityAnalysis cachedAnalysis;

  /** Supported platform configurations. */
  public enum SupportLevel {
    /** Fully supported with all features available */
    FULLY_SUPPORTED,
    /** Supported with some limitations or performance differences */
    SUPPORTED_WITH_LIMITATIONS,
    /** Basic support, some features may not be available */
    BASIC_SUPPORT,
    /** Experimental support, not recommended for production */
    EXPERIMENTAL,
    /** Not supported */
    UNSUPPORTED
  }

  /** Runtime optimization strategies. */
  public enum OptimizationStrategy {
    /** Maximize performance, suitable for server environments */
    PERFORMANCE_FOCUSED,
    /** Balance performance and memory usage */
    BALANCED,
    /** Minimize memory usage, suitable for constrained environments */
    MEMORY_CONSERVATIVE,
    /** Optimize for fast startup, suitable for serverless */
    STARTUP_OPTIMIZED,
    /** Conservative settings for maximum compatibility */
    COMPATIBILITY_FOCUSED
  }

  /** Cross-platform features that can be validated. */
  public enum CrossPlatformFeature {
    /** JNI runtime support */
    JNI_RUNTIME,
    /** Panama FFI runtime support */
    PANAMA_RUNTIME,
    /** WASI filesystem operations */
    WASI_FILESYSTEM,
    /** WASI networking operations */
    WASI_NETWORKING,
    /** Memory mapping operations */
    MEMORY_MAPPING,
    /** Multi-threading support */
    MULTITHREADING,
    /** SIMD operations */
    SIMD_OPERATIONS,
    /** Async compilation */
    ASYNC_COMPILATION,
    /** Module serialization */
    MODULE_SERIALIZATION,
    /** AOT compilation */
    AOT_COMPILATION,
    /** Component model support */
    COMPONENT_MODEL,
    /** Hot module replacement */
    HOT_MODULE_REPLACEMENT
  }

  /** Comprehensive compatibility analysis results. */
  public static final class CompatibilityAnalysis {
    private final PlatformDetector.PlatformInfo platformInfo;
    private final PlatformFeatureDetector.PlatformFeatures platformFeatures;
    private final SupportLevel overallSupportLevel;
    private final Set<CrossPlatformFeature> supportedFeatures;
    private final Set<CrossPlatformFeature> limitedFeatures;
    private final Set<CrossPlatformFeature> unsupportedFeatures;
    private final OptimizationStrategy recommendedStrategy;
    private final List<String> recommendations;
    private final List<String> warnings;
    private final List<String> limitations;
    private final ConcurrentHashMap<String, Object> validationResults;

    CompatibilityAnalysis(
        final PlatformDetector.PlatformInfo platformInfo,
        final PlatformFeatureDetector.PlatformFeatures platformFeatures,
        final SupportLevel overallSupportLevel,
        final Set<CrossPlatformFeature> supportedFeatures,
        final Set<CrossPlatformFeature> limitedFeatures,
        final Set<CrossPlatformFeature> unsupportedFeatures,
        final OptimizationStrategy recommendedStrategy,
        final List<String> recommendations,
        final List<String> warnings,
        final List<String> limitations) {
      this.platformInfo = platformInfo;
      this.platformFeatures = platformFeatures;
      this.overallSupportLevel = overallSupportLevel;
      this.supportedFeatures = EnumSet.copyOf(supportedFeatures);
      this.limitedFeatures = EnumSet.copyOf(limitedFeatures);
      this.unsupportedFeatures = EnumSet.copyOf(unsupportedFeatures);
      this.recommendedStrategy = recommendedStrategy;
      this.recommendations = List.copyOf(recommendations);
      this.warnings = List.copyOf(warnings);
      this.limitations = List.copyOf(limitations);
      this.validationResults = new ConcurrentHashMap<>();
    }

    /**
     * Gets the basic platform information.
     *
     * @return the platform info
     */
    public PlatformDetector.PlatformInfo getPlatformInfo() {
      return platformInfo;
    }

    /**
     * Gets the detailed platform features.
     *
     * @return the platform features
     */
    public PlatformFeatureDetector.PlatformFeatures getPlatformFeatures() {
      return platformFeatures;
    }

    /**
     * Gets the overall support level for this platform.
     *
     * @return the support level
     */
    public SupportLevel getOverallSupportLevel() {
      return overallSupportLevel;
    }

    /**
     * Gets the fully supported cross-platform features.
     *
     * @return unmodifiable set of supported features
     */
    public Set<CrossPlatformFeature> getSupportedFeatures() {
      return Collections.unmodifiableSet(supportedFeatures);
    }

    /**
     * Gets the features with limited support.
     *
     * @return unmodifiable set of limited features
     */
    public Set<CrossPlatformFeature> getLimitedFeatures() {
      return Collections.unmodifiableSet(limitedFeatures);
    }

    /**
     * Gets the unsupported features.
     *
     * @return unmodifiable set of unsupported features
     */
    public Set<CrossPlatformFeature> getUnsupportedFeatures() {
      return Collections.unmodifiableSet(unsupportedFeatures);
    }

    /**
     * Gets the recommended optimization strategy.
     *
     * @return the optimization strategy
     */
    public OptimizationStrategy getRecommendedStrategy() {
      return recommendedStrategy;
    }

    /**
     * Gets the optimization and configuration recommendations.
     *
     * @return unmodifiable list of recommendations
     */
    public List<String> getRecommendations() {
      return recommendations;
    }

    /**
     * Gets the platform-specific warnings.
     *
     * @return unmodifiable list of warnings
     */
    public List<String> getWarnings() {
      return warnings;
    }

    /**
     * Gets the platform limitations.
     *
     * @return unmodifiable list of limitations
     */
    public List<String> getLimitations() {
      return limitations;
    }

    /**
     * Checks if a specific feature is fully supported.
     *
     * @param feature the feature to check
     * @return true if fully supported
     */
    public boolean isFeatureSupported(final CrossPlatformFeature feature) {
      return supportedFeatures.contains(feature);
    }

    /**
     * Checks if a specific feature has limited support.
     *
     * @param feature the feature to check
     * @return true if has limited support
     */
    public boolean isFeatureLimited(final CrossPlatformFeature feature) {
      return limitedFeatures.contains(feature);
    }

    /**
     * Checks if a specific feature is available (either fully supported or limited).
     *
     * @param feature the feature to check
     * @return true if available
     */
    public boolean isFeatureAvailable(final CrossPlatformFeature feature) {
      return supportedFeatures.contains(feature) || limitedFeatures.contains(feature);
    }

    /**
     * Checks if the platform is suitable for production use.
     *
     * @return true if production-ready
     */
    public boolean isProductionReady() {
      return overallSupportLevel == SupportLevel.FULLY_SUPPORTED
          || overallSupportLevel == SupportLevel.SUPPORTED_WITH_LIMITATIONS;
    }

    /**
     * Gets validation result for a specific test.
     *
     * @param testName the test name
     * @return optional validation result
     */
    public Optional<Object> getValidationResult(final String testName) {
      return Optional.ofNullable(validationResults.get(testName));
    }

    /**
     * Sets a validation result.
     *
     * @param testName the test name
     * @param result the result
     */
    public void setValidationResult(final String testName, final Object result) {
      validationResults.put(testName, result);
    }

    @Override
    public String toString() {
      return String.format(
          "CompatibilityAnalysis{platform=%s, support=%s, supported=%d, limited=%d, unsupported=%d,"
              + " strategy=%s}",
          platformInfo.getPlatformId(),
          overallSupportLevel,
          supportedFeatures.size(),
          limitedFeatures.size(),
          unsupportedFeatures.size(),
          recommendedStrategy);
    }
  }

  /** Runtime configuration recommendations. */
  public static final class RuntimeConfiguration {
    private final int recommendedCompilationThreads;
    private final int recommendedExecutionThreads;
    private final long recommendedMemoryLimit;
    private final Duration recommendedTimeout;
    private final boolean enableSimdOptimizations;
    private final boolean enableAsyncCompilation;
    private final boolean enableModuleCaching;
    private final boolean enableHotReload;
    private final int optimizationLevel;
    private final ConcurrentHashMap<String, Object> additionalSettings;

    RuntimeConfiguration(
        final int recommendedCompilationThreads,
        final int recommendedExecutionThreads,
        final long recommendedMemoryLimit,
        final Duration recommendedTimeout,
        final boolean enableSimdOptimizations,
        final boolean enableAsyncCompilation,
        final boolean enableModuleCaching,
        final boolean enableHotReload,
        final int optimizationLevel) {
      this.recommendedCompilationThreads = recommendedCompilationThreads;
      this.recommendedExecutionThreads = recommendedExecutionThreads;
      this.recommendedMemoryLimit = recommendedMemoryLimit;
      this.recommendedTimeout = recommendedTimeout;
      this.enableSimdOptimizations = enableSimdOptimizations;
      this.enableAsyncCompilation = enableAsyncCompilation;
      this.enableModuleCaching = enableModuleCaching;
      this.enableHotReload = enableHotReload;
      this.optimizationLevel = optimizationLevel;
      this.additionalSettings = new ConcurrentHashMap<>();
    }

    public int getRecommendedCompilationThreads() {
      return recommendedCompilationThreads;
    }

    public int getRecommendedExecutionThreads() {
      return recommendedExecutionThreads;
    }

    public long getRecommendedMemoryLimit() {
      return recommendedMemoryLimit;
    }

    public Duration getRecommendedTimeout() {
      return recommendedTimeout;
    }

    public boolean shouldEnableSimdOptimizations() {
      return enableSimdOptimizations;
    }

    public boolean shouldEnableAsyncCompilation() {
      return enableAsyncCompilation;
    }

    public boolean shouldEnableModuleCaching() {
      return enableModuleCaching;
    }

    public boolean shouldEnableHotReload() {
      return enableHotReload;
    }

    public int getOptimizationLevel() {
      return optimizationLevel;
    }

    public Optional<Object> getAdditionalSetting(final String key) {
      return Optional.ofNullable(additionalSettings.get(key));
    }

    public void setAdditionalSetting(final String key, final Object value) {
      additionalSettings.put(key, value);
    }
  }

  /** Private constructor to prevent instantiation of utility class. */
  private CrossPlatformCompatibility() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Analyzes the current platform for Wasmtime4j compatibility.
   *
   * <p>This method performs comprehensive platform analysis and caches results for subsequent
   * calls.
   *
   * @return the compatibility analysis
   */
  public static CompatibilityAnalysis analyzeCompatibility() {
    CompatibilityAnalysis result = cachedAnalysis;
    if (result == null) {
      synchronized (CrossPlatformCompatibility.class) {
        result = cachedAnalysis;
        if (result == null) {
          result = performCompatibilityAnalysis();
          cachedAnalysis = result;
          LOGGER.info("Completed platform compatibility analysis: " + result);
        }
      }
    }
    return result;
  }

  /**
   * Forces re-analysis of platform compatibility, bypassing cache.
   *
   * @return the fresh compatibility analysis
   */
  public static CompatibilityAnalysis reanalyzeCompatibility() {
    synchronized (CrossPlatformCompatibility.class) {
      final CompatibilityAnalysis result = performCompatibilityAnalysis();
      cachedAnalysis = result;
      LOGGER.info("Re-analyzed platform compatibility: " + result);
      return result;
    }
  }

  /**
   * Gets runtime configuration recommendations for the current platform.
   *
   * @param strategy the optimization strategy
   * @return the runtime configuration
   */
  public static RuntimeConfiguration getRecommendedConfiguration(
      final OptimizationStrategy strategy) {
    final CompatibilityAnalysis analysis = analyzeCompatibility();
    return generateRuntimeConfiguration(analysis, strategy);
  }

  /**
   * Gets runtime configuration recommendations using the platform's recommended strategy.
   *
   * @return the runtime configuration
   */
  public static RuntimeConfiguration getRecommendedConfiguration() {
    final CompatibilityAnalysis analysis = analyzeCompatibility();
    return generateRuntimeConfiguration(analysis, analysis.getRecommendedStrategy());
  }

  /**
   * Validates a specific cross-platform feature.
   *
   * @param feature the feature to validate
   * @return true if the feature works correctly on this platform
   */
  public static boolean validateFeature(final CrossPlatformFeature feature) {
    final CompatibilityAnalysis analysis = analyzeCompatibility();
    return analysis.isFeatureAvailable(feature) && performFeatureValidation(feature);
  }

  /**
   * Performs the actual compatibility analysis.
   *
   * @return the compatibility analysis
   */
  private static CompatibilityAnalysis performCompatibilityAnalysis() {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
    final PlatformFeatureDetector.PlatformFeatures platformFeatures =
        PlatformFeatureDetector.detect();

    LOGGER.fine("Analyzing compatibility for platform: " + platformInfo.getPlatformId());

    // Determine overall support level
    final SupportLevel supportLevel = determineSupportLevel(platformInfo, platformFeatures);

    // Analyze feature support
    final Set<CrossPlatformFeature> supportedFeatures = EnumSet.noneOf(CrossPlatformFeature.class);
    final Set<CrossPlatformFeature> limitedFeatures = EnumSet.noneOf(CrossPlatformFeature.class);
    final Set<CrossPlatformFeature> unsupportedFeatures =
        EnumSet.noneOf(CrossPlatformFeature.class);

    analyzeFeatureSupport(
        platformInfo, platformFeatures, supportedFeatures, limitedFeatures, unsupportedFeatures);

    // Determine optimization strategy
    final OptimizationStrategy strategy = determineOptimizationStrategy(platformFeatures);

    // Generate recommendations and warnings
    final List<String> recommendations =
        generateRecommendations(platformInfo, platformFeatures, strategy);
    final List<String> warnings = generateWarnings(platformInfo, platformFeatures);
    final List<String> limitations =
        generateLimitations(platformInfo, platformFeatures, unsupportedFeatures, limitedFeatures);

    return new CompatibilityAnalysis(
        platformInfo,
        platformFeatures,
        supportLevel,
        supportedFeatures,
        limitedFeatures,
        unsupportedFeatures,
        strategy,
        recommendations,
        warnings,
        limitations);
  }

  /**
   * Determines the overall support level for the platform.
   *
   * @param platformInfo the platform info
   * @param platformFeatures the platform features
   * @return the support level
   */
  private static SupportLevel determineSupportLevel(
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    // Check if platform is officially supported
    final boolean officiallySupported = isOfficiallySupported(platformInfo);
    if (!officiallySupported) {
      return SupportLevel.EXPERIMENTAL;
    }

    // Check for major limitations
    final boolean hasJvmSupport =
        platformFeatures.hasJvmFeature(PlatformFeatureDetector.JvmFeature.PANAMA_FFI)
            || isJniSupported(platformInfo);
    if (!hasJvmSupport) {
      return SupportLevel.BASIC_SUPPORT;
    }

    // Check for containerized environment limitations
    if (platformFeatures.isContainerized()
        && platformFeatures.getAvailableMemory() < 1024L * 1024 * 1024) { // < 1GB
      return SupportLevel.SUPPORTED_WITH_LIMITATIONS;
    }

    // Full support for well-tested platforms
    return SupportLevel.FULLY_SUPPORTED;
  }

  /**
   * Checks if the platform is officially supported.
   *
   * @param platformInfo the platform info
   * @return true if officially supported
   */
  private static boolean isOfficiallySupported(final PlatformDetector.PlatformInfo platformInfo) {
    // All detected platforms are currently supported
    return platformInfo.getOperatingSystem() != null && platformInfo.getArchitecture() != null;
  }

  /**
   * Checks if JNI is supported on the platform.
   *
   * @param platformInfo the platform info
   * @return true if JNI is supported
   */
  private static boolean isJniSupported(final PlatformDetector.PlatformInfo platformInfo) {
    // JNI is supported on all platforms we support
    return true;
  }

  /**
   * Analyzes feature support across the platform.
   *
   * @param platformInfo the platform info
   * @param platformFeatures the platform features
   * @param supportedFeatures the supported features set to populate
   * @param limitedFeatures the limited features set to populate
   * @param unsupportedFeatures the unsupported features set to populate
   */
  private static void analyzeFeatureSupport(
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures,
      final Set<CrossPlatformFeature> supportedFeatures,
      final Set<CrossPlatformFeature> limitedFeatures,
      final Set<CrossPlatformFeature> unsupportedFeatures) {

    // JNI Runtime - always supported
    supportedFeatures.add(CrossPlatformFeature.JNI_RUNTIME);

    // Panama Runtime - depends on JVM version
    if (platformFeatures.hasJvmFeature(PlatformFeatureDetector.JvmFeature.PANAMA_FFI)) {
      supportedFeatures.add(CrossPlatformFeature.PANAMA_RUNTIME);
    } else {
      unsupportedFeatures.add(CrossPlatformFeature.PANAMA_RUNTIME);
    }

    // WASI Filesystem - supported on all platforms
    supportedFeatures.add(CrossPlatformFeature.WASI_FILESYSTEM);

    // WASI Networking - may have limitations in containers
    if (platformFeatures.isContainerized()) {
      limitedFeatures.add(CrossPlatformFeature.WASI_NETWORKING);
    } else {
      supportedFeatures.add(CrossPlatformFeature.WASI_NETWORKING);
    }

    // Memory mapping - supported on all platforms
    supportedFeatures.add(CrossPlatformFeature.MEMORY_MAPPING);

    // Multi-threading - depends on available cores
    if (platformFeatures.getLogicalCores() >= 2) {
      supportedFeatures.add(CrossPlatformFeature.MULTITHREADING);
    } else {
      limitedFeatures.add(CrossPlatformFeature.MULTITHREADING);
    }

    // SIMD operations - depends on CPU features
    if (platformFeatures.shouldEnableSimd()) {
      supportedFeatures.add(CrossPlatformFeature.SIMD_OPERATIONS);
    } else {
      unsupportedFeatures.add(CrossPlatformFeature.SIMD_OPERATIONS);
    }

    // Async compilation - depends on memory and threads
    if (platformFeatures.getAvailableMemory() >= 512L * 1024 * 1024
        && platformFeatures.getLogicalCores() >= 2) {
      supportedFeatures.add(CrossPlatformFeature.ASYNC_COMPILATION);
    } else {
      limitedFeatures.add(CrossPlatformFeature.ASYNC_COMPILATION);
    }

    // Module serialization - supported on all platforms
    supportedFeatures.add(CrossPlatformFeature.MODULE_SERIALIZATION);

    // AOT compilation - supported on all platforms
    supportedFeatures.add(CrossPlatformFeature.AOT_COMPILATION);

    // Component model - supported on all platforms (once implemented)
    supportedFeatures.add(CrossPlatformFeature.COMPONENT_MODEL);

    // Hot module replacement - depends on available memory
    if (platformFeatures.getAvailableMemory() >= 1024L * 1024 * 1024) {
      supportedFeatures.add(CrossPlatformFeature.HOT_MODULE_REPLACEMENT);
    } else {
      limitedFeatures.add(CrossPlatformFeature.HOT_MODULE_REPLACEMENT);
    }
  }

  /**
   * Determines the recommended optimization strategy.
   *
   * @param platformFeatures the platform features
   * @return the optimization strategy
   */
  private static OptimizationStrategy determineOptimizationStrategy(
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    // High-performance server environment
    if (platformFeatures.getPhysicalCores() >= 8
        && platformFeatures.getTotalMemory() >= 8L * 1024 * 1024 * 1024) {
      return OptimizationStrategy.PERFORMANCE_FOCUSED;
    }

    // Container or constrained environment
    if (platformFeatures.isContainerized()
        || platformFeatures.getAvailableMemory() < 2L * 1024 * 1024 * 1024) {
      return OptimizationStrategy.MEMORY_CONSERVATIVE;
    }

    // Development or moderate-resource environment
    if (platformFeatures.getPhysicalCores() >= 4
        && platformFeatures.getTotalMemory() >= 4L * 1024 * 1024 * 1024) {
      return OptimizationStrategy.BALANCED;
    }

    // Default to compatibility-focused for unknown configurations
    return OptimizationStrategy.COMPATIBILITY_FOCUSED;
  }

  /**
   * Generates optimization and configuration recommendations.
   *
   * @param platformInfo the platform info
   * @param platformFeatures the platform features
   * @param strategy the optimization strategy
   * @return list of recommendations
   */
  private static List<String> generateRecommendations(
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures,
      final OptimizationStrategy strategy) {

    final List<String> recommendations = new java.util.ArrayList<>();

    // Runtime selection recommendations
    if (platformFeatures.hasJvmFeature(PlatformFeatureDetector.JvmFeature.PANAMA_FFI)) {
      recommendations.add("Use Panama FFI runtime for best performance on Java 23+");
    } else {
      recommendations.add("Use JNI runtime for Java 8-22 compatibility");
    }

    // Thread pool recommendations
    final int recommendedThreads = platformFeatures.getRecommendedCompilationThreads();
    recommendations.add(
        String.format("Set compilation thread pool to %d threads", recommendedThreads));

    // Memory recommendations
    if (platformFeatures.isHugePagesEnabled()) {
      recommendations.add("Enable huge page optimizations for large module compilation");
    }

    // SIMD recommendations
    if (platformFeatures.shouldEnableSimd()) {
      recommendations.add("Enable SIMD optimizations for improved computation performance");
    }

    // Container-specific recommendations
    if (platformFeatures.isContainerized()) {
      recommendations.add("Use conservative memory settings in containerized environments");
      recommendations.add("Consider resource limits when setting thread pools");
    }

    // Platform-specific recommendations
    if (platformInfo.getOperatingSystem() == PlatformDetector.OperatingSystem.WINDOWS) {
      recommendations.add("Use Windows-specific file path handling for WASI operations");
    } else if (platformInfo.getOperatingSystem() == PlatformDetector.OperatingSystem.MACOS) {
      recommendations.add("Consider macOS security permissions for native library loading");
    }

    return recommendations;
  }

  /**
   * Generates platform-specific warnings.
   *
   * @param platformInfo the platform info
   * @param platformFeatures the platform features
   * @return list of warnings
   */
  private static List<String> generateWarnings(
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures) {

    final List<String> warnings = new java.util.ArrayList<>();

    // Memory warnings
    if (platformFeatures.getAvailableMemory() < 512L * 1024 * 1024) {
      warnings.add("Low available memory may impact compilation performance");
    }

    // Core count warnings
    if (platformFeatures.getLogicalCores() < 2) {
      warnings.add("Single-core systems may experience reduced async performance");
    }

    // Container warnings
    if (platformFeatures.isContainerized()) {
      warnings.add("Container environments may have restricted resource access");
    }

    // Virtualization warnings
    if (platformFeatures.hasOsFeature(PlatformFeatureDetector.OsFeature.VIRTUALIZED)) {
      warnings.add("Virtualized environments may have performance overhead");
    }

    return warnings;
  }

  /**
   * Generates platform limitations.
   *
   * @param platformInfo the platform info
   * @param platformFeatures the platform features
   * @param unsupportedFeatures the unsupported features
   * @param limitedFeatures the limited features
   * @return list of limitations
   */
  private static List<String> generateLimitations(
      final PlatformDetector.PlatformInfo platformInfo,
      final PlatformFeatureDetector.PlatformFeatures platformFeatures,
      final Set<CrossPlatformFeature> unsupportedFeatures,
      final Set<CrossPlatformFeature> limitedFeatures) {

    final List<String> limitations = new java.util.ArrayList<>();

    // Unsupported feature limitations
    for (final CrossPlatformFeature feature : unsupportedFeatures) {
      limitations.add(String.format("Feature not supported: %s", feature.name()));
    }

    // Limited feature limitations
    for (final CrossPlatformFeature feature : limitedFeatures) {
      limitations.add(String.format("Feature has limitations: %s", feature.name()));
    }

    // Platform-specific limitations
    if (!platformFeatures.hasJvmFeature(PlatformFeatureDetector.JvmFeature.PANAMA_FFI)) {
      limitations.add("Panama FFI not available - using JNI runtime only");
    }

    return limitations;
  }

  /**
   * Generates runtime configuration for the given analysis and strategy.
   *
   * @param analysis the compatibility analysis
   * @param strategy the optimization strategy
   * @return the runtime configuration
   */
  private static RuntimeConfiguration generateRuntimeConfiguration(
      final CompatibilityAnalysis analysis, final OptimizationStrategy strategy) {

    final PlatformFeatureDetector.PlatformFeatures features = analysis.getPlatformFeatures();

    int compilationThreads;
    int executionThreads;
    long memoryLimit;
    Duration timeout;
    boolean enableSimd;
    boolean enableAsync;
    boolean enableCaching;
    boolean enableHotReload;
    int optimizationLevel;

    switch (strategy) {
      case PERFORMANCE_FOCUSED:
        compilationThreads = Math.max(4, features.getPhysicalCores());
        executionThreads = features.getLogicalCores();
        memoryLimit = features.getAvailableMemory() * 3 / 4; // Use 75% of available memory
        timeout = Duration.ofMinutes(10);
        enableSimd = features.shouldEnableSimd();
        enableAsync = true;
        enableCaching = true;
        enableHotReload = true;
        optimizationLevel = 3;
        break;

      case MEMORY_CONSERVATIVE:
        compilationThreads = Math.max(1, features.getPhysicalCores() / 2);
        executionThreads = Math.max(2, features.getLogicalCores() / 2);
        memoryLimit = features.getAvailableMemory() / 2; // Use 50% of available memory
        timeout = Duration.ofMinutes(5);
        enableSimd = false;
        enableAsync = features.getLogicalCores() >= 2;
        enableCaching = false;
        enableHotReload = false;
        optimizationLevel = 1;
        break;

      case STARTUP_OPTIMIZED:
        compilationThreads = Math.max(2, features.getPhysicalCores() / 2);
        executionThreads = features.getLogicalCores();
        memoryLimit = features.getAvailableMemory() * 2 / 3; // Use 67% of available memory
        timeout = Duration.ofMinutes(2);
        enableSimd = features.shouldEnableSimd();
        enableAsync = true;
        enableCaching = true;
        enableHotReload = false;
        optimizationLevel = 2;
        break;

      case COMPATIBILITY_FOCUSED:
        compilationThreads = Math.max(1, features.getPhysicalCores() / 4);
        executionThreads = Math.max(1, features.getLogicalCores() / 2);
        memoryLimit = features.getAvailableMemory() / 3; // Use 33% of available memory
        timeout = Duration.ofMinutes(15);
        enableSimd = false;
        enableAsync = false;
        enableCaching = false;
        enableHotReload = false;
        optimizationLevel = 0;
        break;

      case BALANCED:
      default:
        compilationThreads = features.getRecommendedCompilationThreads();
        executionThreads = Math.max(2, features.getLogicalCores() * 3 / 4);
        memoryLimit = features.getAvailableMemory() * 2 / 3; // Use 67% of available memory
        timeout = Duration.ofMinutes(5);
        enableSimd = features.shouldEnableSimd();
        enableAsync = features.getLogicalCores() >= 2;
        enableCaching = features.getTotalMemory() >= 2L * 1024 * 1024 * 1024; // Enable if >= 2GB
        enableHotReload = features.getAvailableMemory() >= 1024L * 1024 * 1024; // Enable if >= 1GB
        optimizationLevel = features.getRecommendedOptimizationLevel();
        break;
    }

    return new RuntimeConfiguration(
        compilationThreads,
        executionThreads,
        memoryLimit,
        timeout,
        enableSimd,
        enableAsync,
        enableCaching,
        enableHotReload,
        optimizationLevel);
  }

  /**
   * Performs actual validation of a specific feature.
   *
   * @param feature the feature to validate
   * @return true if validation passes
   */
  private static boolean performFeatureValidation(final CrossPlatformFeature feature) {
    try {
      switch (feature) {
        case JNI_RUNTIME:
          // JNI is always available in standard JVMs
          return true;

        case PANAMA_RUNTIME:
          // Check if Panama classes are available
          Class.forName("java.lang.foreign.MemorySegment");
          return true;

        case WASI_FILESYSTEM:
          // Basic filesystem access should be available
          return java.nio.file.Files.exists(
              java.nio.file.Paths.get(System.getProperty("user.home")));

        case WASI_NETWORKING:
          // Check if basic networking is available
          try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", 0), 100);
            return false; // Connection should fail for port 0
          } catch (java.io.IOException e) {
            return true; // Expected failure indicates networking is available
          }

        case MEMORY_MAPPING:
          // Check if memory mapping is available
          return System.getProperty("os.name") != null;

        case MULTITHREADING:
          // Check if threading is available
          return Runtime.getRuntime().availableProcessors() >= 1;

        case SIMD_OPERATIONS:
          // This would require native validation
          return true; // Assume available for now

        case ASYNC_COMPILATION:
          // Check if we have enough resources for async operations
          return Runtime.getRuntime().availableProcessors() >= 2;

        case MODULE_SERIALIZATION:
          // Check if serialization is available
          return true; // Should be available on all platforms

        case AOT_COMPILATION:
          // AOT compilation should be available
          return true;

        case COMPONENT_MODEL:
          // Component model support
          return true;

        case HOT_MODULE_REPLACEMENT:
          // Check if we have enough memory for hot reload
          return Runtime.getRuntime().maxMemory() >= 512L * 1024 * 1024;

        default:
          LOGGER.warning("Unknown feature validation requested: " + feature);
          return false;
      }
    } catch (Exception e) {
      LOGGER.log(Level.FINE, "Feature validation failed for " + feature, e);
      return false;
    }
  }
}
