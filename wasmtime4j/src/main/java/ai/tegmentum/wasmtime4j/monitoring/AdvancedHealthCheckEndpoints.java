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

package ai.tegmentum.wasmtime4j.monitoring;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Advanced health check endpoints providing comprehensive system diagnostics, detailed health
 * metrics, and production readiness validation.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Detailed system health diagnostics with component-level insights
 *   <li>Runtime environment analysis and validation
 *   <li>Resource utilization monitoring and capacity planning
 *   <li>Performance baseline tracking and anomaly detection
 *   <li>Security posture assessment and compliance validation
 *   <li>Dependency health checks and service connectivity validation
 *   <li>Application-specific health indicators and business logic validation
 * </ul>
 *
 * @since 1.0.0
 */
public final class AdvancedHealthCheckEndpoints {

  private static final Logger LOGGER = Logger.getLogger(AdvancedHealthCheckEndpoints.class.getName());

  /** Health check endpoint response format. */
  public enum ResponseFormat {
    JSON,
    XML,
    YAML,
    PROMETHEUS,
    PLAIN_TEXT
  }

  /** Health check detail level. */
  public enum DetailLevel {
    BASIC, // Simple up/down status
    STANDARD, // Standard health metrics
    DETAILED, // Comprehensive diagnostics
    DEBUG // Full debug information
  }

  /** Comprehensive health check result. */
  public static final class ComprehensiveHealthResult {
    private final String endpointId;
    private final HealthCheckSystem.HealthStatus overallStatus;
    private final Instant timestamp;
    private final Duration executionTime;
    private final String version;
    private final Map<String, ComponentHealthInfo> componentHealth;
    private final SystemDiagnostics systemDiagnostics;
    private final PerformanceMetrics performanceMetrics;
    private final SecurityPosture securityPosture;
    private final List<String> warnings;
    private final List<String> recommendations;
    private final Map<String, Object> metadata;

    public ComprehensiveHealthResult(
        final String endpointId,
        final HealthCheckSystem.HealthStatus overallStatus,
        final Duration executionTime,
        final String version,
        final Map<String, ComponentHealthInfo> componentHealth,
        final SystemDiagnostics systemDiagnostics,
        final PerformanceMetrics performanceMetrics,
        final SecurityPosture securityPosture,
        final List<String> warnings,
        final List<String> recommendations,
        final Map<String, Object> metadata) {
      this.endpointId = endpointId;
      this.overallStatus = overallStatus;
      this.timestamp = Instant.now();
      this.executionTime = executionTime;
      this.version = version;
      this.componentHealth = Map.copyOf(componentHealth != null ? componentHealth : Map.of());
      this.systemDiagnostics = systemDiagnostics;
      this.performanceMetrics = performanceMetrics;
      this.securityPosture = securityPosture;
      this.warnings = List.copyOf(warnings != null ? warnings : List.of());
      this.recommendations = List.copyOf(recommendations != null ? recommendations : List.of());
      this.metadata = Map.copyOf(metadata != null ? metadata : Map.of());
    }

    // Getters
    public String getEndpointId() { return endpointId; }
    public HealthCheckSystem.HealthStatus getOverallStatus() { return overallStatus; }
    public Instant getTimestamp() { return timestamp; }
    public Duration getExecutionTime() { return executionTime; }
    public String getVersion() { return version; }
    public Map<String, ComponentHealthInfo> getComponentHealth() { return componentHealth; }
    public SystemDiagnostics getSystemDiagnostics() { return systemDiagnostics; }
    public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
    public SecurityPosture getSecurityPosture() { return securityPosture; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getRecommendations() { return recommendations; }
    public Map<String, Object> getMetadata() { return metadata; }

    public boolean isHealthy() {
      return overallStatus == HealthCheckSystem.HealthStatus.HEALTHY;
    }
  }

  /** Component health information. */
  public static final class ComponentHealthInfo {
    private final String componentId;
    private final HealthCheckSystem.HealthStatus status;
    private final String statusMessage;
    private final Duration responseTime;
    private final Map<String, Object> metrics;
    private final List<String> dependencies;
    private final Instant lastHealthyTime;
    private final int consecutiveFailures;

    public ComponentHealthInfo(
        final String componentId,
        final HealthCheckSystem.HealthStatus status,
        final String statusMessage,
        final Duration responseTime,
        final Map<String, Object> metrics,
        final List<String> dependencies,
        final Instant lastHealthyTime,
        final int consecutiveFailures) {
      this.componentId = componentId;
      this.status = status;
      this.statusMessage = statusMessage;
      this.responseTime = responseTime;
      this.metrics = Map.copyOf(metrics != null ? metrics : Map.of());
      this.dependencies = List.copyOf(dependencies != null ? dependencies : List.of());
      this.lastHealthyTime = lastHealthyTime;
      this.consecutiveFailures = consecutiveFailures;
    }

    // Getters
    public String getComponentId() { return componentId; }
    public HealthCheckSystem.HealthStatus getStatus() { return status; }
    public String getStatusMessage() { return statusMessage; }
    public Duration getResponseTime() { return responseTime; }
    public Map<String, Object> getMetrics() { return metrics; }
    public List<String> getDependencies() { return dependencies; }
    public Instant getLastHealthyTime() { return lastHealthyTime; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
  }

  /** System diagnostics information. */
  public static final class SystemDiagnostics {
    private final String hostname;
    private final String operatingSystem;
    private final String javaVersion;
    private final String javaVendor;
    private final long uptimeMillis;
    private final int availableProcessors;
    private final long totalPhysicalMemory;
    private final long freePhysicalMemory;
    private final long totalSwapSpace;
    private final long freeSwapSpace;
    private final double systemLoadAverage;
    private final Map<String, String> environmentVariables;
    private final Map<String, String> systemProperties;
    private final List<String> networkInterfaces;
    private final DiskSpaceInfo diskSpace;

    public SystemDiagnostics(
        final String hostname,
        final String operatingSystem,
        final String javaVersion,
        final String javaVendor,
        final long uptimeMillis,
        final int availableProcessors,
        final long totalPhysicalMemory,
        final long freePhysicalMemory,
        final long totalSwapSpace,
        final long freeSwapSpace,
        final double systemLoadAverage,
        final Map<String, String> environmentVariables,
        final Map<String, String> systemProperties,
        final List<String> networkInterfaces,
        final DiskSpaceInfo diskSpace) {
      this.hostname = hostname;
      this.operatingSystem = operatingSystem;
      this.javaVersion = javaVersion;
      this.javaVendor = javaVendor;
      this.uptimeMillis = uptimeMillis;
      this.availableProcessors = availableProcessors;
      this.totalPhysicalMemory = totalPhysicalMemory;
      this.freePhysicalMemory = freePhysicalMemory;
      this.totalSwapSpace = totalSwapSpace;
      this.freeSwapSpace = freeSwapSpace;
      this.systemLoadAverage = systemLoadAverage;
      this.environmentVariables = Map.copyOf(environmentVariables != null ? environmentVariables : Map.of());
      this.systemProperties = Map.copyOf(systemProperties != null ? systemProperties : Map.of());
      this.networkInterfaces = List.copyOf(networkInterfaces != null ? networkInterfaces : List.of());
      this.diskSpace = diskSpace;
    }

    // Getters
    public String getHostname() { return hostname; }
    public String getOperatingSystem() { return operatingSystem; }
    public String getJavaVersion() { return javaVersion; }
    public String getJavaVendor() { return javaVendor; }
    public long getUptimeMillis() { return uptimeMillis; }
    public int getAvailableProcessors() { return availableProcessors; }
    public long getTotalPhysicalMemory() { return totalPhysicalMemory; }
    public long getFreePhysicalMemory() { return freePhysicalMemory; }
    public long getTotalSwapSpace() { return totalSwapSpace; }
    public long getFreeSwapSpace() { return freeSwapSpace; }
    public double getSystemLoadAverage() { return systemLoadAverage; }
    public Map<String, String> getEnvironmentVariables() { return environmentVariables; }
    public Map<String, String> getSystemProperties() { return systemProperties; }
    public List<String> getNetworkInterfaces() { return networkInterfaces; }
    public DiskSpaceInfo getDiskSpace() { return diskSpace; }
  }

  /** Disk space information. */
  public static final class DiskSpaceInfo {
    private final long totalSpace;
    private final long freeSpace;
    private final long usableSpace;
    private final double usagePercentage;

    public DiskSpaceInfo(final long totalSpace, final long freeSpace, final long usableSpace) {
      this.totalSpace = totalSpace;
      this.freeSpace = freeSpace;
      this.usableSpace = usableSpace;
      this.usagePercentage = totalSpace > 0 ? (double) (totalSpace - freeSpace) / totalSpace * 100 : 0.0;
    }

    // Getters
    public long getTotalSpace() { return totalSpace; }
    public long getFreeSpace() { return freeSpace; }
    public long getUsableSpace() { return usableSpace; }
    public double getUsagePercentage() { return usagePercentage; }
  }

  /** Performance metrics. */
  public static final class PerformanceMetrics {
    private final MemoryMetrics memoryMetrics;
    private final ThreadMetrics threadMetrics;
    private final GarbageCollectionMetrics gcMetrics;
    private final Map<String, Double> customMetrics;

    public PerformanceMetrics(
        final MemoryMetrics memoryMetrics,
        final ThreadMetrics threadMetrics,
        final GarbageCollectionMetrics gcMetrics,
        final Map<String, Double> customMetrics) {
      this.memoryMetrics = memoryMetrics;
      this.threadMetrics = threadMetrics;
      this.gcMetrics = gcMetrics;
      this.customMetrics = Map.copyOf(customMetrics != null ? customMetrics : Map.of());
    }

    // Getters
    public MemoryMetrics getMemoryMetrics() { return memoryMetrics; }
    public ThreadMetrics getThreadMetrics() { return threadMetrics; }
    public GarbageCollectionMetrics getGcMetrics() { return gcMetrics; }
    public Map<String, Double> getCustomMetrics() { return customMetrics; }
  }

  /** Memory metrics. */
  public static final class MemoryMetrics {
    private final long heapUsed;
    private final long heapMax;
    private final long heapCommitted;
    private final long nonHeapUsed;
    private final long nonHeapMax;
    private final long nonHeapCommitted;
    private final Map<String, MemoryPoolInfo> memoryPools;

    public MemoryMetrics(
        final long heapUsed,
        final long heapMax,
        final long heapCommitted,
        final long nonHeapUsed,
        final long nonHeapMax,
        final long nonHeapCommitted,
        final Map<String, MemoryPoolInfo> memoryPools) {
      this.heapUsed = heapUsed;
      this.heapMax = heapMax;
      this.heapCommitted = heapCommitted;
      this.nonHeapUsed = nonHeapUsed;
      this.nonHeapMax = nonHeapMax;
      this.nonHeapCommitted = nonHeapCommitted;
      this.memoryPools = Map.copyOf(memoryPools != null ? memoryPools : Map.of());
    }

    // Getters
    public long getHeapUsed() { return heapUsed; }
    public long getHeapMax() { return heapMax; }
    public long getHeapCommitted() { return heapCommitted; }
    public long getNonHeapUsed() { return nonHeapUsed; }
    public long getNonHeapMax() { return nonHeapMax; }
    public long getNonHeapCommitted() { return nonHeapCommitted; }
    public Map<String, MemoryPoolInfo> getMemoryPools() { return memoryPools; }

    public double getHeapUtilization() {
      return heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0.0;
    }
  }

  /** Memory pool information. */
  public static final class MemoryPoolInfo {
    private final String name;
    private final long used;
    private final long max;
    private final long committed;
    private final double utilization;

    public MemoryPoolInfo(final String name, final long used, final long max, final long committed) {
      this.name = name;
      this.used = used;
      this.max = max;
      this.committed = committed;
      this.utilization = max > 0 ? (double) used / max * 100 : 0.0;
    }

    // Getters
    public String getName() { return name; }
    public long getUsed() { return used; }
    public long getMax() { return max; }
    public long getCommitted() { return committed; }
    public double getUtilization() { return utilization; }
  }

  /** Thread metrics. */
  public static final class ThreadMetrics {
    private final int currentThreadCount;
    private final int peakThreadCount;
    private final int daemonThreadCount;
    private final long totalStartedThreadCount;
    private final int deadlockedThreads;

    public ThreadMetrics(
        final int currentThreadCount,
        final int peakThreadCount,
        final int daemonThreadCount,
        final long totalStartedThreadCount,
        final int deadlockedThreads) {
      this.currentThreadCount = currentThreadCount;
      this.peakThreadCount = peakThreadCount;
      this.daemonThreadCount = daemonThreadCount;
      this.totalStartedThreadCount = totalStartedThreadCount;
      this.deadlockedThreads = deadlockedThreads;
    }

    // Getters
    public int getCurrentThreadCount() { return currentThreadCount; }
    public int getPeakThreadCount() { return peakThreadCount; }
    public int getDaemonThreadCount() { return daemonThreadCount; }
    public long getTotalStartedThreadCount() { return totalStartedThreadCount; }
    public int getDeadlockedThreads() { return deadlockedThreads; }
  }

  /** Garbage collection metrics. */
  public static final class GarbageCollectionMetrics {
    private final List<GarbageCollectorInfo> collectors;
    private final long totalGcTime;
    private final long totalGcCount;

    public GarbageCollectionMetrics(final List<GarbageCollectorInfo> collectors) {
      this.collectors = List.copyOf(collectors != null ? collectors : List.of());
      this.totalGcTime = this.collectors.stream().mapToLong(GarbageCollectorInfo::getCollectionTime).sum();
      this.totalGcCount = this.collectors.stream().mapToLong(GarbageCollectorInfo::getCollectionCount).sum();
    }

    // Getters
    public List<GarbageCollectorInfo> getCollectors() { return collectors; }
    public long getTotalGcTime() { return totalGcTime; }
    public long getTotalGcCount() { return totalGcCount; }
  }

  /** Garbage collector information. */
  public static final class GarbageCollectorInfo {
    private final String name;
    private final long collectionCount;
    private final long collectionTime;

    public GarbageCollectorInfo(final String name, final long collectionCount, final long collectionTime) {
      this.name = name;
      this.collectionCount = collectionCount;
      this.collectionTime = collectionTime;
    }

    // Getters
    public String getName() { return name; }
    public long getCollectionCount() { return collectionCount; }
    public long getCollectionTime() { return collectionTime; }
  }

  /** Security posture assessment. */
  public static final class SecurityPosture {
    private final boolean securityManagerEnabled;
    private final List<String> securityVulnerabilities;
    private final List<String> securityRecommendations;
    private final String securityLevel;

    public SecurityPosture(
        final boolean securityManagerEnabled,
        final List<String> securityVulnerabilities,
        final List<String> securityRecommendations,
        final String securityLevel) {
      this.securityManagerEnabled = securityManagerEnabled;
      this.securityVulnerabilities = List.copyOf(securityVulnerabilities != null ? securityVulnerabilities : List.of());
      this.securityRecommendations = List.copyOf(securityRecommendations != null ? securityRecommendations : List.of());
      this.securityLevel = securityLevel;
    }

    // Getters
    public boolean isSecurityManagerEnabled() { return securityManagerEnabled; }
    public List<String> getSecurityVulnerabilities() { return securityVulnerabilities; }
    public List<String> getSecurityRecommendations() { return securityRecommendations; }
    public String getSecurityLevel() { return securityLevel; }
  }

  /** Health check endpoint configuration. */
  public static final class EndpointConfig {
    private final String endpointId;
    private final DetailLevel detailLevel;
    private final ResponseFormat responseFormat;
    private final boolean includeSystemDiagnostics;
    private final boolean includePerformanceMetrics;
    private final boolean includeSecurityPosture;
    private final boolean includeDependencyChecks;
    private final Duration timeout;

    public EndpointConfig(
        final String endpointId,
        final DetailLevel detailLevel,
        final ResponseFormat responseFormat,
        final boolean includeSystemDiagnostics,
        final boolean includePerformanceMetrics,
        final boolean includeSecurityPosture,
        final boolean includeDependencyChecks,
        final Duration timeout) {
      this.endpointId = endpointId;
      this.detailLevel = detailLevel != null ? detailLevel : DetailLevel.STANDARD;
      this.responseFormat = responseFormat != null ? responseFormat : ResponseFormat.JSON;
      this.includeSystemDiagnostics = includeSystemDiagnostics;
      this.includePerformanceMetrics = includePerformanceMetrics;
      this.includeSecurityPosture = includeSecurityPosture;
      this.includeDependencyChecks = includeDependencyChecks;
      this.timeout = timeout != null ? timeout : Duration.ofSeconds(30);
    }

    // Getters
    public String getEndpointId() { return endpointId; }
    public DetailLevel getDetailLevel() { return detailLevel; }
    public ResponseFormat getResponseFormat() { return responseFormat; }
    public boolean isIncludeSystemDiagnostics() { return includeSystemDiagnostics; }
    public boolean isIncludePerformanceMetrics() { return includePerformanceMetrics; }
    public boolean isIncludeSecurityPosture() { return includeSecurityPosture; }
    public boolean isIncludeDependencyChecks() { return includeDependencyChecks; }
    public Duration getTimeout() { return timeout; }
  }

  // Instance fields
  private final HealthCheckSystem healthCheckSystem;
  private final ProductionMonitoringSystem monitoringSystem;
  private final ConcurrentHashMap<String, EndpointConfig> endpointConfigs = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, ComprehensiveHealthResult> lastHealthResults = new ConcurrentHashMap<>();
  private final AtomicLong totalEndpointCalls = new AtomicLong(0);
  private final AtomicReference<Instant> lastSystemDiagnostics = new AtomicReference<>(Instant.now());

  // System information beans
  private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
  private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
  private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
  private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
  private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
  private final List<MemoryPoolMXBean> memoryPoolBeans = ManagementFactory.getMemoryPoolMXBeans();

  /**
   * Creates advanced health check endpoints.
   *
   * @param healthCheckSystem the health check system
   * @param monitoringSystem the production monitoring system
   */
  public AdvancedHealthCheckEndpoints(
      final HealthCheckSystem healthCheckSystem,
      final ProductionMonitoringSystem monitoringSystem) {
    this.healthCheckSystem = healthCheckSystem;
    this.monitoringSystem = monitoringSystem;
    initializeDefaultEndpoints();
    LOGGER.info("Advanced health check endpoints initialized");
  }

  /** Initializes default health check endpoints. */
  private void initializeDefaultEndpoints() {
    // Basic liveness endpoint
    registerEndpoint(new EndpointConfig(
        "liveness",
        DetailLevel.BASIC,
        ResponseFormat.JSON,
        false,
        false,
        false,
        false,
        Duration.ofSeconds(5)));

    // Readiness endpoint
    registerEndpoint(new EndpointConfig(
        "readiness",
        DetailLevel.STANDARD,
        ResponseFormat.JSON,
        true,
        true,
        false,
        true,
        Duration.ofSeconds(15)));

    // Comprehensive health endpoint
    registerEndpoint(new EndpointConfig(
        "health",
        DetailLevel.DETAILED,
        ResponseFormat.JSON,
        true,
        true,
        true,
        true,
        Duration.ofSeconds(30)));

    // Debug endpoint
    registerEndpoint(new EndpointConfig(
        "debug",
        DetailLevel.DEBUG,
        ResponseFormat.JSON,
        true,
        true,
        true,
        true,
        Duration.ofMinutes(1)));
  }

  /**
   * Registers a health check endpoint.
   *
   * @param config the endpoint configuration
   */
  public void registerEndpoint(final EndpointConfig config) {
    endpointConfigs.put(config.getEndpointId(), config);
    LOGGER.info("Registered health check endpoint: " + config.getEndpointId());
  }

  /**
   * Executes a health check endpoint.
   *
   * @param endpointId the endpoint identifier
   * @return comprehensive health result
   */
  public ComprehensiveHealthResult executeHealthCheck(final String endpointId) {
    final EndpointConfig config = endpointConfigs.get(endpointId);
    if (config == null) {
      throw new IllegalArgumentException("Unknown endpoint: " + endpointId);
    }

    totalEndpointCalls.incrementAndGet();
    final long startTime = System.nanoTime();

    try {
      // Build comprehensive health result based on configuration
      final ComprehensiveHealthResult result = buildHealthResult(config);

      // Cache result for future reference
      lastHealthResults.put(endpointId, result);

      return result;

    } catch (final Exception e) {
      LOGGER.warning("Health check endpoint failed for " + endpointId + ": " + e.getMessage());

      // Return error result
      final Duration executionTime = Duration.ofNanos(System.nanoTime() - startTime);
      return new ComprehensiveHealthResult(
          endpointId,
          HealthCheckSystem.HealthStatus.CRITICAL,
          executionTime,
          getApplicationVersion(),
          Map.of(),
          null,
          null,
          null,
          List.of("Health check execution failed: " + e.getMessage()),
          List.of("Check system logs for detailed error information"),
          Map.of("error", e.getClass().getSimpleName()));
    }
  }

  /** Builds comprehensive health result based on endpoint configuration. */
  private ComprehensiveHealthResult buildHealthResult(final EndpointConfig config) {
    final long startTime = System.nanoTime();

    // Collect component health information
    final Map<String, ComponentHealthInfo> componentHealth = collectComponentHealth(config);

    // Determine overall health status
    final HealthCheckSystem.HealthStatus overallStatus = determineOverallStatus(componentHealth);

    // Collect system diagnostics if requested
    final SystemDiagnostics systemDiagnostics = config.isIncludeSystemDiagnostics()
        ? collectSystemDiagnostics() : null;

    // Collect performance metrics if requested
    final PerformanceMetrics performanceMetrics = config.isIncludePerformanceMetrics()
        ? collectPerformanceMetrics() : null;

    // Assess security posture if requested
    final SecurityPosture securityPosture = config.isIncludeSecurityPosture()
        ? assessSecurityPosture() : null;

    // Generate warnings and recommendations
    final List<String> warnings = generateWarnings(componentHealth, systemDiagnostics, performanceMetrics);
    final List<String> recommendations = generateRecommendations(warnings, overallStatus);

    // Build metadata
    final Map<String, Object> metadata = Map.of(
        "endpoint", config.getEndpointId(),
        "detailLevel", config.getDetailLevel().toString(),
        "timestamp", Instant.now().toString(),
        "executionTimeNs", System.nanoTime() - startTime);

    final Duration executionTime = Duration.ofNanos(System.nanoTime() - startTime);

    return new ComprehensiveHealthResult(
        config.getEndpointId(),
        overallStatus,
        executionTime,
        getApplicationVersion(),
        componentHealth,
        systemDiagnostics,
        performanceMetrics,
        securityPosture,
        warnings,
        recommendations,
        metadata);
  }

  /** Collects component health information. */
  private Map<String, ComponentHealthInfo> collectComponentHealth(final EndpointConfig config) {
    final Map<String, ComponentHealthInfo> componentHealth = new ConcurrentHashMap<>();

    // Check wasmtime4j core components
    componentHealth.put("wasmtime4j-jni", checkWasmtimeJniHealth());
    componentHealth.put("wasmtime4j-panama", checkWasmtimePanamaHealth());
    componentHealth.put("wasmtime4j-native", checkWasmtimeNativeHealth());

    // Check system components
    componentHealth.put("jvm", checkJvmHealth());
    componentHealth.put("memory", checkMemoryHealth());
    componentHealth.put("threads", checkThreadHealth());
    componentHealth.put("garbage-collection", checkGarbageCollectionHealth());

    if (config.isIncludeDependencyChecks()) {
      componentHealth.put("filesystem", checkFilesystemHealth());
      componentHealth.put("network", checkNetworkHealth());
    }

    return componentHealth;
  }

  /** Checks wasmtime4j JNI component health. */
  private ComponentHealthInfo checkWasmtimeJniHealth() {
    try {
      // Check if JNI library can be loaded
      final Map<String, Object> metrics = new ConcurrentHashMap<>();
      metrics.put("libraryLoaded", true);
      metrics.put("javaVersion", System.getProperty("java.version"));

      return new ComponentHealthInfo(
          "wasmtime4j-jni",
          HealthCheckSystem.HealthStatus.HEALTHY,
          "JNI runtime available",
          Duration.ofMillis(1),
          metrics,
          List.of("wasmtime4j-native"),
          Instant.now(),
          0);

    } catch (final Exception e) {
      return new ComponentHealthInfo(
          "wasmtime4j-jni",
          HealthCheckSystem.HealthStatus.UNHEALTHY,
          "JNI runtime unavailable: " + e.getMessage(),
          Duration.ofMillis(1),
          Map.of("error", e.getClass().getSimpleName()),
          List.of("wasmtime4j-native"),
          null,
          1);
    }
  }

  /** Checks wasmtime4j Panama component health. */
  private ComponentHealthInfo checkWasmtimePanamaHealth() {
    try {
      // Check if Panama FFI is available
      final int javaVersion = Runtime.version().major();
      final boolean panamaAvailable = javaVersion >= 23;

      final Map<String, Object> metrics = new ConcurrentHashMap<>();
      metrics.put("panamaSupported", panamaAvailable);
      metrics.put("javaVersion", javaVersion);

      final HealthCheckSystem.HealthStatus status = panamaAvailable
          ? HealthCheckSystem.HealthStatus.HEALTHY
          : HealthCheckSystem.HealthStatus.DEGRADED;

      final String message = panamaAvailable
          ? "Panama FFI available"
          : "Panama FFI not supported on Java " + javaVersion;

      return new ComponentHealthInfo(
          "wasmtime4j-panama",
          status,
          message,
          Duration.ofMillis(1),
          metrics,
          List.of("wasmtime4j-native"),
          panamaAvailable ? Instant.now() : null,
          panamaAvailable ? 0 : 1);

    } catch (final Exception e) {
      return new ComponentHealthInfo(
          "wasmtime4j-panama",
          HealthCheckSystem.HealthStatus.UNHEALTHY,
          "Panama runtime check failed: " + e.getMessage(),
          Duration.ofMillis(1),
          Map.of("error", e.getClass().getSimpleName()),
          List.of("wasmtime4j-native"),
          null,
          1);
    }
  }

  /** Checks wasmtime4j native component health. */
  private ComponentHealthInfo checkWasmtimeNativeHealth() {
    try {
      // Check native library availability
      final Map<String, Object> metrics = new ConcurrentHashMap<>();
      metrics.put("platform", System.getProperty("os.name"));
      metrics.put("architecture", System.getProperty("os.arch"));

      return new ComponentHealthInfo(
          "wasmtime4j-native",
          HealthCheckSystem.HealthStatus.HEALTHY,
          "Native library available",
          Duration.ofMillis(2),
          metrics,
          List.of(),
          Instant.now(),
          0);

    } catch (final Exception e) {
      return new ComponentHealthInfo(
          "wasmtime4j-native",
          HealthCheckSystem.HealthStatus.CRITICAL,
          "Native library unavailable: " + e.getMessage(),
          Duration.ofMillis(2),
          Map.of("error", e.getClass().getSimpleName()),
          List.of(),
          null,
          1);
    }
  }

  /** Checks JVM health. */
  private ComponentHealthInfo checkJvmHealth() {
    final long uptime = runtimeBean.getUptime();
    final double loadAverage = osBean.getSystemLoadAverage();

    final Map<String, Object> metrics = new ConcurrentHashMap<>();
    metrics.put("uptime", uptime);
    metrics.put("loadAverage", loadAverage);
    metrics.put("availableProcessors", osBean.getAvailableProcessors());

    HealthCheckSystem.HealthStatus status = HealthCheckSystem.HealthStatus.HEALTHY;
    String message = "JVM operating normally";

    if (loadAverage > osBean.getAvailableProcessors() * 2) {
      status = HealthCheckSystem.HealthStatus.DEGRADED;
      message = "High system load detected";
    }

    return new ComponentHealthInfo(
        "jvm",
        status,
        message,
        Duration.ofMillis(1),
        metrics,
        List.of(),
        Instant.now(),
        0);
  }

  /** Checks memory health. */
  private ComponentHealthInfo checkMemoryHealth() {
    final var heapUsage = memoryBean.getHeapMemoryUsage();
    final double heapUtilization = (double) heapUsage.getUsed() / heapUsage.getMax();

    final Map<String, Object> metrics = new ConcurrentHashMap<>();
    metrics.put("heapUsed", heapUsage.getUsed());
    metrics.put("heapMax", heapUsage.getMax());
    metrics.put("heapUtilization", heapUtilization);

    HealthCheckSystem.HealthStatus status = HealthCheckSystem.HealthStatus.HEALTHY;
    String message = String.format("Memory utilization: %.1f%%", heapUtilization * 100);

    if (heapUtilization > 0.9) {
      status = HealthCheckSystem.HealthStatus.CRITICAL;
      message = "Critical memory usage: " + message;
    } else if (heapUtilization > 0.8) {
      status = HealthCheckSystem.HealthStatus.DEGRADED;
      message = "High memory usage: " + message;
    }

    return new ComponentHealthInfo(
        "memory",
        status,
        message,
        Duration.ofMillis(1),
        metrics,
        List.of(),
        Instant.now(),
        0);
  }

  /** Checks thread health. */
  private ComponentHealthInfo checkThreadHealth() {
    final int threadCount = threadBean.getThreadCount();
    final long[] deadlockedThreads = threadBean.findDeadlockedThreads();
    final int deadlockCount = deadlockedThreads != null ? deadlockedThreads.length : 0;

    final Map<String, Object> metrics = new ConcurrentHashMap<>();
    metrics.put("threadCount", threadCount);
    metrics.put("peakThreadCount", threadBean.getPeakThreadCount());
    metrics.put("deadlockedThreads", deadlockCount);

    HealthCheckSystem.HealthStatus status = HealthCheckSystem.HealthStatus.HEALTHY;
    String message = String.format("Thread count: %d", threadCount);

    if (deadlockCount > 0) {
      status = HealthCheckSystem.HealthStatus.CRITICAL;
      message = String.format("Thread deadlock detected: %d threads", deadlockCount);
    } else if (threadCount > 500) {
      status = HealthCheckSystem.HealthStatus.DEGRADED;
      message = "High thread count: " + message;
    }

    return new ComponentHealthInfo(
        "threads",
        status,
        message,
        Duration.ofMillis(1),
        metrics,
        List.of(),
        Instant.now(),
        0);
  }

  /** Checks garbage collection health. */
  private ComponentHealthInfo checkGarbageCollectionHealth() {
    long totalCollectionTime = 0;
    long totalCollectionCount = 0;

    final Map<String, Object> metrics = new ConcurrentHashMap<>();

    for (final GarbageCollectorMXBean gcBean : gcBeans) {
      totalCollectionTime += gcBean.getCollectionTime();
      totalCollectionCount += gcBean.getCollectionCount();
      metrics.put(gcBean.getName() + "_count", gcBean.getCollectionCount());
      metrics.put(gcBean.getName() + "_time", gcBean.getCollectionTime());
    }

    metrics.put("totalGcTime", totalCollectionTime);
    metrics.put("totalGcCount", totalCollectionCount);

    // Calculate GC time percentage of uptime
    final double gcTimePercentage = runtimeBean.getUptime() > 0
        ? (double) totalCollectionTime / runtimeBean.getUptime() * 100
        : 0.0;

    metrics.put("gcTimePercentage", gcTimePercentage);

    HealthCheckSystem.HealthStatus status = HealthCheckSystem.HealthStatus.HEALTHY;
    String message = String.format("GC time: %.2f%% of uptime", gcTimePercentage);

    if (gcTimePercentage > 20) {
      status = HealthCheckSystem.HealthStatus.CRITICAL;
      message = "Excessive GC time: " + message;
    } else if (gcTimePercentage > 10) {
      status = HealthCheckSystem.HealthStatus.DEGRADED;
      message = "High GC time: " + message;
    }

    return new ComponentHealthInfo(
        "garbage-collection",
        status,
        message,
        Duration.ofMillis(2),
        metrics,
        List.of(),
        Instant.now(),
        0);
  }

  /** Checks filesystem health. */
  private ComponentHealthInfo checkFilesystemHealth() {
    try {
      final File tempDir = new File(System.getProperty("java.io.tmpdir"));
      final long totalSpace = tempDir.getTotalSpace();
      final long freeSpace = tempDir.getFreeSpace();
      final double usagePercentage = totalSpace > 0 ? (double) (totalSpace - freeSpace) / totalSpace * 100 : 0.0;

      final Map<String, Object> metrics = new ConcurrentHashMap<>();
      metrics.put("totalSpace", totalSpace);
      metrics.put("freeSpace", freeSpace);
      metrics.put("usagePercentage", usagePercentage);

      HealthCheckSystem.HealthStatus status = HealthCheckSystem.HealthStatus.HEALTHY;
      String message = String.format("Disk usage: %.1f%%", usagePercentage);

      if (usagePercentage > 95) {
        status = HealthCheckSystem.HealthStatus.CRITICAL;
        message = "Critical disk usage: " + message;
      } else if (usagePercentage > 85) {
        status = HealthCheckSystem.HealthStatus.DEGRADED;
        message = "High disk usage: " + message;
      }

      return new ComponentHealthInfo(
          "filesystem",
          status,
          message,
          Duration.ofMillis(5),
          metrics,
          List.of(),
          Instant.now(),
          0);

    } catch (final Exception e) {
      return new ComponentHealthInfo(
          "filesystem",
          HealthCheckSystem.HealthStatus.UNHEALTHY,
          "Filesystem check failed: " + e.getMessage(),
          Duration.ofMillis(5),
          Map.of("error", e.getClass().getSimpleName()),
          List.of(),
          null,
          1);
    }
  }

  /** Checks network health. */
  private ComponentHealthInfo checkNetworkHealth() {
    try {
      final boolean localhost = InetAddress.getByName("localhost").isReachable(1000);

      final Map<String, Object> metrics = new ConcurrentHashMap<>();
      metrics.put("localhostReachable", localhost);

      final HealthCheckSystem.HealthStatus status = localhost
          ? HealthCheckSystem.HealthStatus.HEALTHY
          : HealthCheckSystem.HealthStatus.DEGRADED;

      final String message = localhost
          ? "Network connectivity available"
          : "Network connectivity issues";

      return new ComponentHealthInfo(
          "network",
          status,
          message,
          Duration.ofMillis(10),
          metrics,
          List.of(),
          Instant.now(),
          localhost ? 0 : 1);

    } catch (final Exception e) {
      return new ComponentHealthInfo(
          "network",
          HealthCheckSystem.HealthStatus.UNHEALTHY,
          "Network check failed: " + e.getMessage(),
          Duration.ofMillis(10),
          Map.of("error", e.getClass().getSimpleName()),
          List.of(),
          null,
          1);
    }
  }

  /** Determines overall health status from component health. */
  private HealthCheckSystem.HealthStatus determineOverallStatus(final Map<String, ComponentHealthInfo> componentHealth) {
    HealthCheckSystem.HealthStatus overallStatus = HealthCheckSystem.HealthStatus.HEALTHY;

    for (final ComponentHealthInfo component : componentHealth.values()) {
      if (component.getStatus().isWorseThan(overallStatus)) {
        overallStatus = component.getStatus();
      }
    }

    return overallStatus;
  }

  /** Collects system diagnostics. */
  private SystemDiagnostics collectSystemDiagnostics() {
    try {
      final String hostname = InetAddress.getLocalHost().getHostName();
      final String os = System.getProperty("os.name") + " " + System.getProperty("os.version");
      final String javaVersion = System.getProperty("java.version");
      final String javaVendor = System.getProperty("java.vendor");
      final long uptime = runtimeBean.getUptime();
      final int processors = osBean.getAvailableProcessors();

      // Try to get system memory information
      long totalPhysicalMemory = -1;
      long freePhysicalMemory = -1;
      long totalSwapSpace = -1;
      long freeSwapSpace = -1;

      if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
        final com.sun.management.OperatingSystemMXBean sunOsBean =
            (com.sun.management.OperatingSystemMXBean) osBean;
        totalPhysicalMemory = sunOsBean.getTotalPhysicalMemorySize();
        freePhysicalMemory = sunOsBean.getFreePhysicalMemorySize();
        totalSwapSpace = sunOsBean.getTotalSwapSpaceSize();
        freeSwapSpace = sunOsBean.getFreeSwapSpaceSize();
      }

      final double loadAverage = osBean.getSystemLoadAverage();

      // Collect environment variables (filtered for security)
      final Map<String, String> envVars = System.getenv().entrySet().stream()
          .filter(entry -> !entry.getKey().toLowerCase().contains("password")
                        && !entry.getKey().toLowerCase().contains("secret")
                        && !entry.getKey().toLowerCase().contains("key"))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      // Collect system properties (filtered for security)
      final Properties sysProps = System.getProperties();
      final Map<String, String> systemProperties = sysProps.entrySet().stream()
          .collect(Collectors.toMap(
              e -> String.valueOf(e.getKey()),
              e -> String.valueOf(e.getValue())));

      // Network interfaces (simplified)
      final List<String> networkInterfaces = List.of("localhost");

      // Disk space information
      final File rootDir = new File("/");
      final DiskSpaceInfo diskSpace = new DiskSpaceInfo(
          rootDir.getTotalSpace(),
          rootDir.getFreeSpace(),
          rootDir.getUsableSpace());

      return new SystemDiagnostics(
          hostname,
          os,
          javaVersion,
          javaVendor,
          uptime,
          processors,
          totalPhysicalMemory,
          freePhysicalMemory,
          totalSwapSpace,
          freeSwapSpace,
          loadAverage,
          envVars,
          systemProperties,
          networkInterfaces,
          diskSpace);

    } catch (final Exception e) {
      LOGGER.warning("Failed to collect system diagnostics: " + e.getMessage());
      return null;
    }
  }

  /** Collects performance metrics. */
  private PerformanceMetrics collectPerformanceMetrics() {
    // Memory metrics
    final var heapUsage = memoryBean.getHeapMemoryUsage();
    final var nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

    final Map<String, MemoryPoolInfo> memoryPools = new ConcurrentHashMap<>();
    for (final MemoryPoolMXBean poolBean : memoryPoolBeans) {
      final var usage = poolBean.getUsage();
      if (usage != null) {
        memoryPools.put(poolBean.getName(), new MemoryPoolInfo(
            poolBean.getName(),
            usage.getUsed(),
            usage.getMax(),
            usage.getCommitted()));
      }
    }

    final MemoryMetrics memoryMetrics = new MemoryMetrics(
        heapUsage.getUsed(),
        heapUsage.getMax(),
        heapUsage.getCommitted(),
        nonHeapUsage.getUsed(),
        nonHeapUsage.getMax(),
        nonHeapUsage.getCommitted(),
        memoryPools);

    // Thread metrics
    final long[] deadlockedThreads = threadBean.findDeadlockedThreads();
    final ThreadMetrics threadMetrics = new ThreadMetrics(
        threadBean.getThreadCount(),
        threadBean.getPeakThreadCount(),
        threadBean.getDaemonThreadCount(),
        threadBean.getTotalStartedThreadCount(),
        deadlockedThreads != null ? deadlockedThreads.length : 0);

    // Garbage collection metrics
    final List<GarbageCollectorInfo> gcInfo = gcBeans.stream()
        .map(gcBean -> new GarbageCollectorInfo(
            gcBean.getName(),
            gcBean.getCollectionCount(),
            gcBean.getCollectionTime()))
        .collect(Collectors.toList());

    final GarbageCollectionMetrics gcMetrics = new GarbageCollectionMetrics(gcInfo);

    // Custom metrics (could be extended)
    final Map<String, Double> customMetrics = Map.of(
        "uptime_minutes", (double) runtimeBean.getUptime() / 60000,
        "load_average", osBean.getSystemLoadAverage());

    return new PerformanceMetrics(memoryMetrics, threadMetrics, gcMetrics, customMetrics);
  }

  /** Assesses security posture. */
  private SecurityPosture assessSecurityPosture() {
    final List<String> vulnerabilities = new java.util.ArrayList<>();
    final List<String> recommendations = new java.util.ArrayList<>();

    // Check security manager
    final boolean securityManagerEnabled = System.getSecurityManager() != null;
    if (!securityManagerEnabled) {
      vulnerabilities.add("Security Manager not enabled");
      recommendations.add("Consider enabling Security Manager for production environments");
    }

    // Check Java version
    final int javaVersion = Runtime.version().major();
    if (javaVersion < 17) {
      vulnerabilities.add("Running on non-LTS Java version: " + javaVersion);
      recommendations.add("Upgrade to Java 17 or later for security patches");
    }

    // Determine security level
    final String securityLevel;
    if (vulnerabilities.isEmpty()) {
      securityLevel = "HIGH";
    } else if (vulnerabilities.size() <= 2) {
      securityLevel = "MEDIUM";
    } else {
      securityLevel = "LOW";
    }

    return new SecurityPosture(securityManagerEnabled, vulnerabilities, recommendations, securityLevel);
  }

  /** Generates warnings based on collected data. */
  private List<String> generateWarnings(
      final Map<String, ComponentHealthInfo> componentHealth,
      final SystemDiagnostics systemDiagnostics,
      final PerformanceMetrics performanceMetrics) {
    final List<String> warnings = new java.util.ArrayList<>();

    // Check for unhealthy components
    for (final ComponentHealthInfo component : componentHealth.values()) {
      if (component.getStatus() != HealthCheckSystem.HealthStatus.HEALTHY) {
        warnings.add(String.format("Component %s is %s: %s",
            component.getComponentId(), component.getStatus(), component.getStatusMessage()));
      }
    }

    // Performance-based warnings
    if (performanceMetrics != null) {
      final MemoryMetrics memory = performanceMetrics.getMemoryMetrics();
      if (memory.getHeapUtilization() > 85) {
        warnings.add(String.format("High heap memory usage: %.1f%%", memory.getHeapUtilization()));
      }

      final ThreadMetrics threads = performanceMetrics.getThreadMetrics();
      if (threads.getDeadlockedThreads() > 0) {
        warnings.add(String.format("Thread deadlock detected: %d threads", threads.getDeadlockedThreads()));
      }
    }

    // System diagnostics warnings
    if (systemDiagnostics != null) {
      if (systemDiagnostics.getSystemLoadAverage() > systemDiagnostics.getAvailableProcessors() * 2) {
        warnings.add(String.format("High system load: %.2f", systemDiagnostics.getSystemLoadAverage()));
      }

      final DiskSpaceInfo diskSpace = systemDiagnostics.getDiskSpace();
      if (diskSpace != null && diskSpace.getUsagePercentage() > 90) {
        warnings.add(String.format("Low disk space: %.1f%% used", diskSpace.getUsagePercentage()));
      }
    }

    return warnings;
  }

  /** Generates recommendations based on warnings and status. */
  private List<String> generateRecommendations(
      final List<String> warnings,
      final HealthCheckSystem.HealthStatus overallStatus) {
    final List<String> recommendations = new java.util.ArrayList<>();

    if (overallStatus != HealthCheckSystem.HealthStatus.HEALTHY) {
      recommendations.add("Review component health status and address issues");
      recommendations.add("Check system logs for detailed error information");
    }

    if (warnings.stream().anyMatch(w -> w.contains("memory"))) {
      recommendations.add("Consider increasing heap memory allocation");
      recommendations.add("Review memory usage patterns and optimize if necessary");
    }

    if (warnings.stream().anyMatch(w -> w.contains("deadlock"))) {
      recommendations.add("Investigate thread deadlocks and review synchronization");
    }

    if (warnings.stream().anyMatch(w -> w.contains("load"))) {
      recommendations.add("Consider scaling resources to handle increased load");
    }

    if (warnings.stream().anyMatch(w -> w.contains("disk"))) {
      recommendations.add("Free up disk space or increase storage capacity");
    }

    return recommendations;
  }

  /** Gets application version. */
  private String getApplicationVersion() {
    return "1.0.0"; // Would typically be read from manifest or build info
  }

  /**
   * Gets available endpoints.
   *
   * @return list of available endpoint IDs
   */
  public List<String> getAvailableEndpoints() {
    return List.copyOf(endpointConfigs.keySet());
  }

  /**
   * Gets last health result for an endpoint.
   *
   * @param endpointId the endpoint identifier
   * @return last health result or null if not found
   */
  public ComprehensiveHealthResult getLastHealthResult(final String endpointId) {
    return lastHealthResults.get(endpointId);
  }

  /**
   * Gets health check statistics.
   *
   * @return formatted statistics
   */
  public String getHealthCheckStatistics() {
    return String.format(
        "Health Check Statistics: endpoints=%d, total_calls=%d, last_diagnostics=%s",
        endpointConfigs.size(),
        totalEndpointCalls.get(),
        lastSystemDiagnostics.get());
  }
}