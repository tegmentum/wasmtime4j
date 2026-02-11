package ai.tegmentum.wasmtime4j.config;

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.config.profiles.OptimizationTemplate;
import ai.tegmentum.wasmtime4j.config.profiles.PerformanceProfile;
import java.lang.management.ManagementFactory;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Automatic configuration detection and optimization recommendation system.
 *
 * <p>This class analyzes the runtime environment, workload characteristics, and system capabilities
 * to automatically select optimal WebAssembly engine configurations.
 *
 * @since 1.0.0
 */
public final class AutoConfig {

  private static final Logger LOGGER = Logger.getLogger(AutoConfig.class.getName());

  // Runtime environment detection
  private final RuntimeEnvironment runtimeEnvironment;
  private final SystemCapabilities systemCapabilities;
  private final WorkloadCharacteristics workloadCharacteristics;

  // Configuration recommendation cache
  private final Map<String, ConfigurationRecommendation> recommendationCache;
  private long lastDetectionTime;
  private static final long DETECTION_CACHE_DURATION = TimeUnit.MINUTES.toMillis(5);

  /** Private constructor for builder pattern. */
  private AutoConfig() {
    this.runtimeEnvironment = detectRuntimeEnvironment();
    this.systemCapabilities = detectSystemCapabilities();
    this.workloadCharacteristics = new WorkloadCharacteristics();
    this.recommendationCache = new HashMap<>();
    this.lastDetectionTime = System.currentTimeMillis();
  }

  /**
   * Create a new auto-configuration instance with current environment detection.
   *
   * @return new auto-configuration instance
   */
  public static AutoConfig create() {
    return new AutoConfig();
  }

  /**
   * Get the optimal engine configuration for the current environment.
   *
   * @return recommended engine configuration
   */
  public EngineConfig getOptimalConfiguration() {
    return getOptimalConfiguration(WorkloadType.GENERAL_PURPOSE);
  }

  /**
   * Get the optimal engine configuration for a specific workload type.
   *
   * @param workloadType the type of workload
   * @return recommended engine configuration
   */
  public EngineConfig getOptimalConfiguration(final WorkloadType workloadType) {
    final ConfigurationRecommendation recommendation = getConfigurationRecommendation(workloadType);
    return recommendation.toEngineConfig();
  }

  /**
   * Get a detailed configuration recommendation with explanations.
   *
   * @param workloadType the type of workload
   * @return detailed configuration recommendation
   */
  public ConfigurationRecommendation getConfigurationRecommendation(
      final WorkloadType workloadType) {
    // Check cache first
    final String cacheKey = workloadType.name() + "_" + getEnvironmentFingerprint();
    final long currentTime = System.currentTimeMillis();

    if (recommendationCache.containsKey(cacheKey)
        && (currentTime - lastDetectionTime) < DETECTION_CACHE_DURATION) {
      return recommendationCache.get(cacheKey);
    }

    // Generate new recommendation
    final ConfigurationRecommendation recommendation = generateRecommendation(workloadType);
    recommendationCache.put(cacheKey, recommendation);
    lastDetectionTime = currentTime;

    LOGGER.log(
        Level.INFO,
        "Generated configuration recommendation for {0}: {1}",
        new Object[] {workloadType, recommendation.getProfile().getDisplayName()});

    return recommendation;
  }

  /**
   * Analyze workload performance and suggest configuration adjustments.
   *
   * @param executionMetrics runtime execution metrics
   * @return configuration adjustment recommendations
   */
  public ConfigurationAdjustment analyzeAndAdjust(final ExecutionMetrics executionMetrics) {
    updateWorkloadCharacteristics(executionMetrics);
    return generateConfigurationAdjustment(executionMetrics);
  }

  /**
   * Get the detected runtime environment information.
   *
   * @return runtime environment details
   */
  public RuntimeEnvironment getRuntimeEnvironment() {
    return runtimeEnvironment;
  }

  /**
   * Get the detected system capabilities.
   *
   * @return system capabilities details
   */
  public SystemCapabilities getSystemCapabilities() {
    return systemCapabilities;
  }

  /**
   * Export configuration recommendations for deployment.
   *
   * @param workloadType the workload type to export configuration for
   * @return serializable configuration export
   */
  public ConfigurationExport exportConfiguration(final WorkloadType workloadType) {
    final ConfigurationRecommendation recommendation = getConfigurationRecommendation(workloadType);
    return new ConfigurationExport(recommendation, runtimeEnvironment, systemCapabilities);
  }

  /**
   * Import and apply a previously exported configuration.
   *
   * @param configurationExport the exported configuration
   * @return engine configuration from the import
   */
  public EngineConfig importConfiguration(final ConfigurationExport configurationExport) {
    // Validate compatibility with current environment
    if (!isCompatibleWithCurrentEnvironment(configurationExport)) {
      LOGGER.log(
          Level.WARNING, "Imported configuration may not be optimal for current environment");
    }

    return configurationExport.getRecommendation().toEngineConfig();
  }

  private ConfigurationRecommendation generateRecommendation(final WorkloadType workloadType) {
    // Select base profile based on workload type and environment
    final PerformanceProfile baseProfile = selectBaseProfile(workloadType);

    // Apply environment-specific adjustments
    final EngineConfig adjustedConfig = applyEnvironmentAdjustments(baseProfile.createConfig());

    // Apply workload-specific optimizations
    final EngineConfig optimizedConfig = applyWorkloadOptimizations(adjustedConfig, workloadType);

    // Create recommendation with explanations
    return new ConfigurationRecommendation(
        baseProfile,
        optimizedConfig,
        generateRecommendationReasons(workloadType, baseProfile),
        calculateConfidenceScore(workloadType));
  }

  private PerformanceProfile selectBaseProfile(final WorkloadType workloadType) {
    // Consider memory constraints
    if (systemCapabilities.isMemoryConstrained()) {
      return PerformanceProfile.MEMORY_OPTIMIZED;
    }

    // Consider development vs production
    if (runtimeEnvironment.isDevelopmentMode()) {
      return PerformanceProfile.DEBUG;
    }

    // Select based on workload type
    switch (workloadType) {
      case CPU_INTENSIVE:
      case MACHINE_LEARNING:
      case BATCH_PROCESSING:
        return systemCapabilities.getProcessorCount() >= 4
            ? PerformanceProfile.MAXIMUM_PERFORMANCE
            : PerformanceProfile.BALANCED;

      case IO_INTENSIVE:
      case WEB_APPLICATION:
        return PerformanceProfile.BALANCED;

      case REAL_TIME:
        return PerformanceProfile.MAXIMUM_PERFORMANCE;

      case SECURITY_CRITICAL:
        return PerformanceProfile.BALANCED;

      case DEVELOPMENT_TESTING:
        return PerformanceProfile.DEBUG;

      case GENERAL_PURPOSE:
      default:
        return PerformanceProfile.BALANCED;
    }
  }

  private EngineConfig applyEnvironmentAdjustments(final EngineConfig baseConfig) {
    EngineConfig adjustedConfig = baseConfig;

    // Adjust for container environments
    if (runtimeEnvironment.isContainerEnvironment()) {
      adjustedConfig =
          adjustedConfig
              .setMaxWasmStack(Math.min(1024 * 1024, adjustedConfig.getMaxWasmStack())) // 1MB max
              .parallelCompilation(systemCapabilities.getProcessorCount() > 2);
    }

    // Adjust for cloud environments
    if (runtimeEnvironment.isCloudEnvironment()) {
      adjustedConfig =
          adjustedConfig
              .setFuelConsumption(true) // Enable for resource limiting
              .setEpochInterruption(true); // Enable for cooperative scheduling
    }

    // Adjust for low-memory environments
    if (systemCapabilities.getAvailableMemoryMB() < 512) {
      adjustedConfig =
          adjustedConfig
              .optimizationLevel(OptimizationLevel.SIZE)
              .parallelCompilation(false)
              .setMaxWasmStack(256 * 1024); // 256KB stack
    }

    // Adjust for single-core systems
    if (systemCapabilities.getProcessorCount() == 1) {
      adjustedConfig = adjustedConfig.parallelCompilation(false);
    }

    return adjustedConfig;
  }

  private EngineConfig applyWorkloadOptimizations(
      final EngineConfig baseConfig, final WorkloadType workloadType) {
    // Apply appropriate optimization template
    final OptimizationTemplate template = getTemplateForWorkload(workloadType);
    if (template != null) {
      return template.applyTo(baseConfig);
    }

    // Apply workload-specific WebAssembly features
    final Set<WasmFeature> features = getRecommendedFeaturesForWorkload(workloadType);
    if (!features.isEmpty()) {
      return baseConfig.setWasmFeatures(features);
    }

    return baseConfig;
  }

  private OptimizationTemplate getTemplateForWorkload(final WorkloadType workloadType) {
    switch (workloadType) {
      case CPU_INTENSIVE:
      case MACHINE_LEARNING:
      case BATCH_PROCESSING:
        return OptimizationTemplate.findTemplate("CPU Intensive");
      case IO_INTENSIVE:
      case WEB_APPLICATION:
        return OptimizationTemplate.findTemplate("I/O Intensive");
      case REAL_TIME:
        return OptimizationTemplate.findTemplate("Real-time");
      default:
        return null;
    }
  }

  private Set<WasmFeature> getRecommendedFeaturesForWorkload(final WorkloadType workloadType) {
    final Set<WasmFeature> features = EnumSet.noneOf(WasmFeature.class);

    // Always recommend these stable features
    features.add(WasmFeature.REFERENCE_TYPES);
    features.add(WasmFeature.BULK_MEMORY);
    features.add(WasmFeature.MULTI_VALUE);

    // Add workload-specific features
    switch (workloadType) {
      case MACHINE_LEARNING:
      case CPU_INTENSIVE:
      case BATCH_PROCESSING:
        if (systemCapabilities.hasSimdSupport()) {
          features.add(WasmFeature.SIMD);
        }
        break;

      case REAL_TIME:
        // Avoid features that might introduce non-determinism
        features.remove(WasmFeature.THREADS);
        break;

      case WEB_APPLICATION:
      case IO_INTENSIVE:
        if (systemCapabilities.hasSimdSupport()) {
          features.add(WasmFeature.SIMD);
        }
        break;

      default:
        // Conservative feature set
        break;
    }

    return features;
  }

  private String[] generateRecommendationReasons(
      final WorkloadType workloadType, final PerformanceProfile profile) {
    final java.util.List<String> reasons = new java.util.ArrayList<>();

    // Environment-based reasons
    if (systemCapabilities.isMemoryConstrained()) {
      reasons.add("Memory-optimized profile selected due to limited available memory");
    }

    if (runtimeEnvironment.isDevelopmentMode()) {
      reasons.add("Development-friendly settings enabled based on debug mode detection");
    }

    if (runtimeEnvironment.isContainerEnvironment()) {
      reasons.add("Container-optimized settings applied for containerized environment");
    }

    // Workload-based reasons
    reasons.add(
        String.format(
            "Profile '%s' selected for %s workload type",
            profile.getDisplayName(), workloadType.getDisplayName()));

    // System capability reasons
    if (systemCapabilities.getProcessorCount() > 4) {
      reasons.add("Parallel compilation enabled due to multi-core system");
    }

    if (systemCapabilities.hasSimdSupport()
        && (workloadType == WorkloadType.MACHINE_LEARNING
            || workloadType == WorkloadType.CPU_INTENSIVE)) {
      reasons.add("SIMD optimizations enabled based on CPU support and workload requirements");
    }

    return reasons.toArray(new String[0]);
  }

  private int calculateConfidenceScore(final WorkloadType workloadType) {
    int confidence = 70; // Base confidence

    // Increase confidence for well-defined workload types
    switch (workloadType) {
      case DEVELOPMENT_TESTING:
      case SECURITY_CRITICAL:
        confidence += 20;
        break;
      case CPU_INTENSIVE:
      case MACHINE_LEARNING:
        confidence += 15;
        break;
      case WEB_APPLICATION:
      case BATCH_PROCESSING:
        confidence += 10;
        break;
      default:
        break;
    }

    // Adjust based on environment certainty
    if (runtimeEnvironment.isContainerEnvironment() || runtimeEnvironment.isCloudEnvironment()) {
      confidence += 10;
    }

    // Adjust based on system information availability
    if (systemCapabilities.hasDetailedCpuInfo()) {
      confidence += 5;
    }

    return Math.min(100, confidence);
  }

  private ConfigurationAdjustment generateConfigurationAdjustment(final ExecutionMetrics metrics) {
    final java.util.List<String> recommendations = new java.util.ArrayList<>();
    final Map<String, Object> adjustments = new HashMap<>();

    // Analyze compilation time
    if (metrics.getCompilationTimeMs() >= 5000) { // >= 5 seconds
      recommendations.add("Consider using DEBUG profile for faster development iterations");
      adjustments.put("optimization_level", OptimizationLevel.NONE);
    }

    // Analyze memory usage
    if (metrics.getMemoryUsageMB() > systemCapabilities.getAvailableMemoryMB() * 0.8) {
      recommendations.add("High memory usage detected, consider MEMORY_OPTIMIZED profile");
      adjustments.put("max_stack_size", 256 * 1024L);
      adjustments.put("optimization_level", OptimizationLevel.SIZE);
    }

    // Analyze execution performance
    if (metrics.getExecutionTimeMs() > metrics.getExpectedExecutionTimeMs() * 1.5) {
      recommendations.add("Poor execution performance, consider MAXIMUM_PERFORMANCE profile");
      adjustments.put("optimization_level", OptimizationLevel.SPEED);
      adjustments.put("parallel_compilation", true);
    }

    // Analyze resource contention
    if (metrics.getCpuUtilization() > 0.9 && systemCapabilities.getProcessorCount() > 1) {
      recommendations.add("High CPU utilization, consider disabling parallel compilation");
      adjustments.put("parallel_compilation", false);
    }

    return new ConfigurationAdjustment(recommendations, adjustments, metrics);
  }

  private void updateWorkloadCharacteristics(final ExecutionMetrics metrics) {
    workloadCharacteristics.updateFromMetrics(metrics);
  }

  private String getEnvironmentFingerprint() {
    return String.format(
        "%s_%d_%d_%s",
        runtimeEnvironment.getJavaVersion(),
        systemCapabilities.getProcessorCount(),
        systemCapabilities.getAvailableMemoryMB(),
        systemCapabilities.getOperatingSystem());
  }

  private boolean isCompatibleWithCurrentEnvironment(final ConfigurationExport export) {
    // Check Java version compatibility
    if (!export
        .getRuntimeEnvironment()
        .getJavaVersion()
        .equals(runtimeEnvironment.getJavaVersion())) {
      return false;
    }

    // Check basic system compatibility
    return export
        .getSystemCapabilities()
        .getOperatingSystem()
        .equals(systemCapabilities.getOperatingSystem());
  }

  private static RuntimeEnvironment detectRuntimeEnvironment() {
    return new RuntimeEnvironment();
  }

  private static SystemCapabilities detectSystemCapabilities() {
    return new SystemCapabilities();
  }

  /** Workload types for automatic configuration selection. */
  public enum WorkloadType {
    GENERAL_PURPOSE("General Purpose"),
    CPU_INTENSIVE("CPU Intensive"),
    IO_INTENSIVE("I/O Intensive"),
    REAL_TIME("Real-time"),
    BATCH_PROCESSING("Batch Processing"),
    WEB_APPLICATION("Web Application"),
    MACHINE_LEARNING("Machine Learning"),
    SECURITY_CRITICAL("Security Critical"),
    DEVELOPMENT_TESTING("Development/Testing");

    private final String displayName;

    WorkloadType(final String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  /** Runtime environment detection and characteristics. */
  public static final class RuntimeEnvironment {
    private final String javaVersion;
    private final String javaVendor;
    private final boolean developmentMode;
    private final boolean containerEnvironment;
    private final boolean cloudEnvironment;
    private final Map<String, String> environmentProperties;

    private RuntimeEnvironment() {
      // Note: ManagementFactory.getRuntimeMXBean() is used in detectDevelopmentMode()
      this.javaVersion = System.getProperty("java.version");
      this.javaVendor = System.getProperty("java.vendor");
      this.developmentMode = detectDevelopmentMode();
      this.containerEnvironment = detectContainerEnvironment();
      this.cloudEnvironment = detectCloudEnvironment();
      this.environmentProperties = collectEnvironmentProperties();
    }

    private boolean detectDevelopmentMode() {
      // Check for debug mode
      final String debug = System.getProperty("wasmtime4j.debug");
      if (debug != null && Boolean.parseBoolean(debug)) {
        return true;
      }

      // Check for JDWP (Java Debug Wire Protocol)
      return ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("jdwp");
    }

    private boolean detectContainerEnvironment() {
      // Check for container-specific environment variables
      return System.getenv("KUBERNETES_SERVICE_HOST") != null
          || System.getenv("DOCKER_CONTAINER") != null
          || System.getProperty("java.class.path").contains("container");
    }

    private boolean detectCloudEnvironment() {
      // Check for cloud provider environment variables
      return System.getenv("AWS_REGION") != null
          || System.getenv("GOOGLE_CLOUD_PROJECT") != null
          || System.getenv("AZURE_CLIENT_ID") != null;
    }

    private Map<String, String> collectEnvironmentProperties() {
      final Map<String, String> properties = new HashMap<>();
      properties.put("java.version", javaVersion);
      properties.put("java.vendor", javaVendor);
      properties.put("os.name", System.getProperty("os.name"));
      properties.put("os.arch", System.getProperty("os.arch"));
      properties.put("os.version", System.getProperty("os.version"));
      return properties;
    }

    public String getJavaVersion() {
      return javaVersion;
    }

    public String getJavaVendor() {
      return javaVendor;
    }

    public boolean isDevelopmentMode() {
      return developmentMode;
    }

    public boolean isContainerEnvironment() {
      return containerEnvironment;
    }

    public boolean isCloudEnvironment() {
      return cloudEnvironment;
    }

    public Map<String, String> getEnvironmentProperties() {
      return Map.copyOf(environmentProperties);
    }
  }

  /** System capabilities detection and analysis. */
  public static final class SystemCapabilities {
    private final int processorCount;
    private final long availableMemoryMB;
    private final String operatingSystem;
    private final String cpuArchitecture;
    private final boolean simdSupport;
    private final boolean serverEnvironment;
    private final boolean detailedCpuInfo;

    private SystemCapabilities() {
      // Use Runtime directly for processor count and memory
      // OperatingSystemMXBean and MemoryMXBean not needed for current implementation
      this.processorCount = Runtime.getRuntime().availableProcessors();
      this.availableMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
      this.operatingSystem = System.getProperty("os.name");
      this.cpuArchitecture = System.getProperty("os.arch");
      this.simdSupport = detectSimdSupport();
      this.serverEnvironment = detectServerEnvironment();
      this.detailedCpuInfo = true; // Assume we have basic info
    }

    private boolean detectSimdSupport() {
      // Basic heuristic based on architecture
      final String arch = cpuArchitecture.toLowerCase(Locale.ROOT);
      return arch.contains("x86")
          || arch.contains("amd64")
          || arch.contains("aarch64")
          || arch.contains("arm64");
    }

    private boolean detectServerEnvironment() {
      // Heuristics for server environment detection
      return processorCount >= 4 && availableMemoryMB >= 2048;
    }

    public int getProcessorCount() {
      return processorCount;
    }

    public long getAvailableMemoryMB() {
      return availableMemoryMB;
    }

    public String getOperatingSystem() {
      return operatingSystem;
    }

    public String getCpuArchitecture() {
      return cpuArchitecture;
    }

    public boolean hasSimdSupport() {
      return simdSupport;
    }

    public boolean isServerEnvironment() {
      return serverEnvironment;
    }

    public boolean hasDetailedCpuInfo() {
      return detailedCpuInfo;
    }

    public boolean isMemoryConstrained() {
      return availableMemoryMB < 1024; // < 1GB
    }

    public boolean isPowerConstrained() {
      // Heuristic for mobile/embedded environments
      return processorCount <= 2 && availableMemoryMB <= 512;
    }
  }

  /** Workload characteristics tracking. */
  private static final class WorkloadCharacteristics {
    private long totalExecutions = 0;
    private double avgExecutionTime = 0.0;
    private double avgMemoryUsage = 0.0;
    private double avgCpuUtilization = 0.0;

    void updateFromMetrics(final ExecutionMetrics metrics) {
      totalExecutions++;

      // Update rolling averages
      final double weight = 1.0 / Math.min(totalExecutions, 100); // Last 100 executions
      avgExecutionTime = (1 - weight) * avgExecutionTime + weight * metrics.getExecutionTimeMs();
      avgMemoryUsage = (1 - weight) * avgMemoryUsage + weight * metrics.getMemoryUsageMB();
      avgCpuUtilization = (1 - weight) * avgCpuUtilization + weight * metrics.getCpuUtilization();
    }
  }

  /** Configuration recommendation with detailed explanations. */
  public static final class ConfigurationRecommendation {
    private final PerformanceProfile profile;
    private final EngineConfig engineConfig;
    private final String[] reasons;
    private final int confidenceScore;

    ConfigurationRecommendation(
        final PerformanceProfile profile,
        final EngineConfig engineConfig,
        final String[] reasons,
        final int confidenceScore) {
      this.profile = profile;
      this.engineConfig = engineConfig;
      this.reasons = reasons.clone();
      this.confidenceScore = confidenceScore;
    }

    public PerformanceProfile getProfile() {
      return profile;
    }

    public EngineConfig toEngineConfig() {
      return engineConfig;
    }

    public String[] getReasons() {
      return reasons.clone();
    }

    public int getConfidenceScore() {
      return confidenceScore;
    }
  }

  /** Configuration adjustment recommendations based on performance analysis. */
  public static final class ConfigurationAdjustment {
    private final String[] recommendations;
    private final Map<String, Object> adjustments;
    private final ExecutionMetrics metrics;

    ConfigurationAdjustment(
        final java.util.List<String> recommendations,
        final Map<String, Object> adjustments,
        final ExecutionMetrics metrics) {
      this.recommendations = recommendations.toArray(new String[0]);
      this.adjustments = Map.copyOf(adjustments);
      this.metrics = metrics;
    }

    public String[] getRecommendations() {
      return recommendations.clone();
    }

    public Map<String, Object> getAdjustments() {
      return adjustments;
    }

    public ExecutionMetrics getMetrics() {
      return metrics;
    }
  }

  /** Serializable configuration export for deployment. */
  public static final class ConfigurationExport {
    private final ConfigurationRecommendation recommendation;
    private final RuntimeEnvironment runtimeEnvironment;
    private final SystemCapabilities systemCapabilities;
    private final long exportTimestamp;

    ConfigurationExport(
        final ConfigurationRecommendation recommendation,
        final RuntimeEnvironment runtimeEnvironment,
        final SystemCapabilities systemCapabilities) {
      this.recommendation = recommendation;
      this.runtimeEnvironment = runtimeEnvironment;
      this.systemCapabilities = systemCapabilities;
      this.exportTimestamp = System.currentTimeMillis();
    }

    public ConfigurationRecommendation getRecommendation() {
      return recommendation;
    }

    public RuntimeEnvironment getRuntimeEnvironment() {
      return runtimeEnvironment;
    }

    public SystemCapabilities getSystemCapabilities() {
      return systemCapabilities;
    }

    public long getExportTimestamp() {
      return exportTimestamp;
    }
  }

  /** Execution metrics for performance analysis. */
  public static final class ExecutionMetrics {
    private final long compilationTimeMs;
    private final long executionTimeMs;
    private final long expectedExecutionTimeMs;
    private final long memoryUsageMB;
    private final double cpuUtilization;
    private final int instanceCount;

    /**
     * Creates new execution metrics.
     *
     * @param compilationTimeMs compilation time in milliseconds
     * @param executionTimeMs actual execution time in milliseconds
     * @param expectedExecutionTimeMs expected execution time in milliseconds
     * @param memoryUsageMB memory usage in megabytes
     * @param cpuUtilization CPU utilization percentage (0.0 to 1.0)
     * @param instanceCount number of instances created
     */
    public ExecutionMetrics(
        final long compilationTimeMs,
        final long executionTimeMs,
        final long expectedExecutionTimeMs,
        final long memoryUsageMB,
        final double cpuUtilization,
        final int instanceCount) {
      this.compilationTimeMs = compilationTimeMs;
      this.executionTimeMs = executionTimeMs;
      this.expectedExecutionTimeMs = expectedExecutionTimeMs;
      this.memoryUsageMB = memoryUsageMB;
      this.cpuUtilization = cpuUtilization;
      this.instanceCount = instanceCount;
    }

    public long getCompilationTimeMs() {
      return compilationTimeMs;
    }

    public long getExecutionTimeMs() {
      return executionTimeMs;
    }

    public long getExpectedExecutionTimeMs() {
      return expectedExecutionTimeMs;
    }

    public long getMemoryUsageMB() {
      return memoryUsageMB;
    }

    public double getCpuUtilization() {
      return cpuUtilization;
    }

    public int getInstanceCount() {
      return instanceCount;
    }
  }
}
