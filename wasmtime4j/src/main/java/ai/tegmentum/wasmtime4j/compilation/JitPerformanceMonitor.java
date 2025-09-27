package ai.tegmentum.wasmtime4j.compilation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Comprehensive performance monitor for JIT compilation activities.
 *
 * <p>This monitor tracks detailed metrics about compilation performance,
 * optimization effectiveness, resource usage, and system health across
 * all compilation strategies and runtime implementations.
 *
 * @since 1.0.0
 */
public final class JitPerformanceMonitor {

  private static final Logger LOGGER = Logger.getLogger(JitPerformanceMonitor.class.getName());

  private final JitPerformanceMonitorConfig config;
  private final Map<String, CompilationMetrics> moduleMetrics;
  private final Map<String, FunctionMetrics> functionMetrics;
  private final SystemResourceMonitor systemResourceMonitor;
  private final PerformanceAggregator performanceAggregator;
  private final AlertManager alertManager;
  private final ScheduledExecutorService scheduler;
  private final AtomicReference<MonitoringState> state;
  private final AtomicLong totalCompilations;
  private final AtomicLong totalCompilationTimeMs;

  /**
   * Creates a new JIT performance monitor with the specified configuration.
   *
   * @param config the monitor configuration
   * @throws IllegalArgumentException if config is null
   */
  public JitPerformanceMonitor(final JitPerformanceMonitorConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("JIT performance monitor configuration cannot be null");
    }
    this.config = config;
    this.moduleMetrics = new ConcurrentHashMap<>();
    this.functionMetrics = new ConcurrentHashMap<>();
    this.systemResourceMonitor = new SystemResourceMonitor(config);
    this.performanceAggregator = new PerformanceAggregator(config);
    this.alertManager = new AlertManager(config);
    this.scheduler = Executors.newScheduledThreadPool(2);
    this.state = new AtomicReference<>(MonitoringState.STOPPED);
    this.totalCompilations = new AtomicLong(0);
    this.totalCompilationTimeMs = new AtomicLong(0);
  }

  /**
   * Starts the performance monitoring.
   */
  public void start() {
    if (state.compareAndSet(MonitoringState.STOPPED, MonitoringState.STARTING)) {
      try {
        systemResourceMonitor.start();

        // Schedule periodic performance aggregation
        scheduler.scheduleAtFixedRate(
            this::performPeriodicAggregation,
            config.getAggregationIntervalMs(),
            config.getAggregationIntervalMs(),
            TimeUnit.MILLISECONDS
        );

        // Schedule periodic system health checks
        scheduler.scheduleAtFixedRate(
            this::performSystemHealthCheck,
            config.getHealthCheckIntervalMs(),
            config.getHealthCheckIntervalMs(),
            TimeUnit.MILLISECONDS
        );

        state.set(MonitoringState.RUNNING);
        LOGGER.info("JIT performance monitoring started");

      } catch (final Exception e) {
        state.set(MonitoringState.STOPPED);
        throw new JitMonitoringException("Failed to start JIT performance monitoring", e);
      }
    }
  }

  /**
   * Stops the performance monitoring.
   */
  public void stop() {
    if (state.compareAndSet(MonitoringState.RUNNING, MonitoringState.STOPPING)) {
      try {
        scheduler.shutdown();
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
        }

        systemResourceMonitor.stop();
        state.set(MonitoringState.STOPPED);
        LOGGER.info("JIT performance monitoring stopped");

      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        scheduler.shutdownNow();
        state.set(MonitoringState.STOPPED);
      }
    }
  }

  /**
   * Records the start of a compilation.
   *
   * @param moduleId the module identifier
   * @param functionName the function name (null for module-level compilation)
   * @param compilationType the type of compilation
   * @param tier the compilation tier
   * @return compilation session handle
   * @throws IllegalArgumentException if any required parameter is null
   */
  public CompilationSession startCompilation(final String moduleId,
                                             final String functionName,
                                             final CompilationType compilationType,
                                             final CompilationTier tier) {
    if (moduleId == null) {
      throw new IllegalArgumentException("Module ID cannot be null");
    }
    if (compilationType == null) {
      throw new IllegalArgumentException("Compilation type cannot be null");
    }
    if (tier == null) {
      throw new IllegalArgumentException("Compilation tier cannot be null");
    }

    final CompilationSession session = new CompilationSession(
        moduleId, functionName, compilationType, tier, System.currentTimeMillis()
    );

    totalCompilations.incrementAndGet();

    LOGGER.fine(String.format("Started compilation session: %s", session));
    return session;
  }

  /**
   * Records the completion of a compilation.
   *
   * @param session the compilation session
   * @param result the compilation result
   * @throws IllegalArgumentException if any parameter is null
   */
  public void endCompilation(final CompilationSession session, final CompilationResult result) {
    if (session == null) {
      throw new IllegalArgumentException("Compilation session cannot be null");
    }
    if (result == null) {
      throw new IllegalArgumentException("Compilation result cannot be null");
    }

    final long compilationTimeMs = System.currentTimeMillis() - session.getStartTimeMs();
    totalCompilationTimeMs.addAndGet(compilationTimeMs);

    recordModuleMetrics(session, result, compilationTimeMs);
    recordFunctionMetrics(session, result, compilationTimeMs);

    performanceAggregator.recordCompilation(session, result, compilationTimeMs);
    alertManager.checkCompilationAlerts(session, result, compilationTimeMs);

    LOGGER.fine(String.format("Completed compilation session: %s in %dms", session, compilationTimeMs));
  }

  /**
   * Records optimization metrics.
   *
   * @param moduleId the module identifier
   * @param functionName the function name
   * @param optimizationType the optimization type
   * @param metrics the optimization metrics
   * @throws IllegalArgumentException if any parameter is null
   */
  public void recordOptimizationMetrics(final String moduleId,
                                        final String functionName,
                                        final String optimizationType,
                                        final OptimizationMetrics metrics) {
    if (moduleId == null) {
      throw new IllegalArgumentException("Module ID cannot be null");
    }
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (optimizationType == null) {
      throw new IllegalArgumentException("Optimization type cannot be null");
    }
    if (metrics == null) {
      throw new IllegalArgumentException("Optimization metrics cannot be null");
    }

    final String functionKey = moduleId + "::" + functionName;
    functionMetrics.compute(functionKey, (key, existing) -> {
      if (existing == null) {
        existing = new FunctionMetrics(moduleId, functionName);
      }
      existing.addOptimizationMetrics(optimizationType, metrics);
      return existing;
    });

    performanceAggregator.recordOptimization(moduleId, functionName, optimizationType, metrics);
  }

  /**
   * Records deoptimization event.
   *
   * @param moduleId the module identifier
   * @param functionName the function name
   * @param reason the deoptimization reason
   * @param context additional context information
   * @throws IllegalArgumentException if any required parameter is null
   */
  public void recordDeoptimization(final String moduleId,
                                   final String functionName,
                                   final String reason,
                                   final Map<String, Object> context) {
    if (moduleId == null) {
      throw new IllegalArgumentException("Module ID cannot be null");
    }
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (reason == null) {
      throw new IllegalArgumentException("Deoptimization reason cannot be null");
    }

    final String functionKey = moduleId + "::" + functionName;
    functionMetrics.compute(functionKey, (key, existing) -> {
      if (existing == null) {
        existing = new FunctionMetrics(moduleId, functionName);
      }
      existing.recordDeoptimization(reason, context);
      return existing;
    });

    alertManager.checkDeoptimizationAlerts(moduleId, functionName, reason);
    LOGGER.warning(String.format("Deoptimization recorded for %s::%s: %s", moduleId, functionName, reason));
  }

  /**
   * Gets comprehensive performance metrics.
   *
   * @return current performance metrics
   */
  public JitPerformanceMetrics getMetrics() {
    return new JitPerformanceMetrics(
        totalCompilations.get(),
        totalCompilationTimeMs.get(),
        moduleMetrics.size(),
        functionMetrics.size(),
        performanceAggregator.getAggregatedMetrics(),
        systemResourceMonitor.getCurrentResourceUsage(),
        alertManager.getActiveAlerts()
    );
  }

  /**
   * Gets metrics for a specific module.
   *
   * @param moduleId the module identifier
   * @return module metrics or null if not found
   * @throws IllegalArgumentException if moduleId is null
   */
  public CompilationMetrics getModuleMetrics(final String moduleId) {
    if (moduleId == null) {
      throw new IllegalArgumentException("Module ID cannot be null");
    }
    return moduleMetrics.get(moduleId);
  }

  /**
   * Gets metrics for a specific function.
   *
   * @param moduleId the module identifier
   * @param functionName the function name
   * @return function metrics or null if not found
   * @throws IllegalArgumentException if any parameter is null
   */
  public FunctionMetrics getFunctionMetrics(final String moduleId, final String functionName) {
    if (moduleId == null) {
      throw new IllegalArgumentException("Module ID cannot be null");
    }
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    return functionMetrics.get(moduleId + "::" + functionName);
  }

  /**
   * Resets all metrics and statistics.
   */
  public void reset() {
    moduleMetrics.clear();
    functionMetrics.clear();
    performanceAggregator.reset();
    alertManager.reset();
    totalCompilations.set(0);
    totalCompilationTimeMs.set(0);
    LOGGER.info("JIT performance monitor reset");
  }

  /**
   * Gets the current monitoring state.
   *
   * @return current monitoring state
   */
  public MonitoringState getState() {
    return state.get();
  }

  /**
   * Creates a default JIT performance monitor.
   *
   * @return default JIT performance monitor
   */
  public static JitPerformanceMonitor createDefault() {
    return new JitPerformanceMonitor(JitPerformanceMonitorConfig.createDefault());
  }

  private void recordModuleMetrics(final CompilationSession session,
                                   final CompilationResult result,
                                   final long compilationTimeMs) {
    moduleMetrics.compute(session.getModuleId(), (moduleId, existing) -> {
      if (existing == null) {
        existing = new CompilationMetrics(moduleId);
      }
      existing.recordCompilation(session.getCompilationType(), session.getTier(),
          result.isSuccess(), compilationTimeMs, result.getCodeSizeBytes());
      return existing;
    });
  }

  private void recordFunctionMetrics(final CompilationSession session,
                                     final CompilationResult result,
                                     final long compilationTimeMs) {
    if (session.getFunctionName() != null) {
      final String functionKey = session.getModuleId() + "::" + session.getFunctionName();
      functionMetrics.compute(functionKey, (key, existing) -> {
        if (existing == null) {
          existing = new FunctionMetrics(session.getModuleId(), session.getFunctionName());
        }
        existing.recordCompilation(session.getCompilationType(), session.getTier(),
            result.isSuccess(), compilationTimeMs, result.getCodeSizeBytes());
        return existing;
      });
    }
  }

  private void performPeriodicAggregation() {
    try {
      performanceAggregator.performAggregation();
      LOGGER.fine("Performed periodic performance aggregation");
    } catch (final Exception e) {
      LOGGER.warning("Failed to perform periodic aggregation: " + e.getMessage());
    }
  }

  private void performSystemHealthCheck() {
    try {
      systemResourceMonitor.performHealthCheck();
      alertManager.checkSystemHealthAlerts(systemResourceMonitor.getCurrentResourceUsage());
      LOGGER.fine("Performed system health check");
    } catch (final Exception e) {
      LOGGER.warning("Failed to perform system health check: " + e.getMessage());
    }
  }
}

/**
 * Configuration for the JIT performance monitor.
 */
final class JitPerformanceMonitorConfig {
  private final long aggregationIntervalMs;
  private final long healthCheckIntervalMs;
  private final boolean enableDetailedMetrics;
  private final boolean enableResourceMonitoring;
  private final boolean enableAlerts;
  private final int maxMetricsHistory;
  private final double compilationTimeAlertThresholdMs;
  private final double memoryUsageAlertThreshold;
  private final double cpuUsageAlertThreshold;

  private JitPerformanceMonitorConfig(final Builder builder) {
    this.aggregationIntervalMs = builder.aggregationIntervalMs;
    this.healthCheckIntervalMs = builder.healthCheckIntervalMs;
    this.enableDetailedMetrics = builder.enableDetailedMetrics;
    this.enableResourceMonitoring = builder.enableResourceMonitoring;
    this.enableAlerts = builder.enableAlerts;
    this.maxMetricsHistory = builder.maxMetricsHistory;
    this.compilationTimeAlertThresholdMs = builder.compilationTimeAlertThresholdMs;
    this.memoryUsageAlertThreshold = builder.memoryUsageAlertThreshold;
    this.cpuUsageAlertThreshold = builder.cpuUsageAlertThreshold;
  }

  public long getAggregationIntervalMs() { return aggregationIntervalMs; }
  public long getHealthCheckIntervalMs() { return healthCheckIntervalMs; }
  public boolean isEnableDetailedMetrics() { return enableDetailedMetrics; }
  public boolean isEnableResourceMonitoring() { return enableResourceMonitoring; }
  public boolean isEnableAlerts() { return enableAlerts; }
  public int getMaxMetricsHistory() { return maxMetricsHistory; }
  public double getCompilationTimeAlertThresholdMs() { return compilationTimeAlertThresholdMs; }
  public double getMemoryUsageAlertThreshold() { return memoryUsageAlertThreshold; }
  public double getCpuUsageAlertThreshold() { return cpuUsageAlertThreshold; }

  public static JitPerformanceMonitorConfig createDefault() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private long aggregationIntervalMs = 5000; // 5 seconds
    private long healthCheckIntervalMs = 10000; // 10 seconds
    private boolean enableDetailedMetrics = true;
    private boolean enableResourceMonitoring = true;
    private boolean enableAlerts = true;
    private int maxMetricsHistory = 1000;
    private double compilationTimeAlertThresholdMs = 5000; // 5 seconds
    private double memoryUsageAlertThreshold = 0.8; // 80%
    private double cpuUsageAlertThreshold = 0.9; // 90%

    public Builder aggregationIntervalMs(final long intervalMs) {
      this.aggregationIntervalMs = intervalMs;
      return this;
    }

    public Builder healthCheckIntervalMs(final long intervalMs) {
      this.healthCheckIntervalMs = intervalMs;
      return this;
    }

    public Builder enableDetailedMetrics(final boolean enable) {
      this.enableDetailedMetrics = enable;
      return this;
    }

    public Builder enableResourceMonitoring(final boolean enable) {
      this.enableResourceMonitoring = enable;
      return this;
    }

    public Builder enableAlerts(final boolean enable) {
      this.enableAlerts = enable;
      return this;
    }

    public Builder maxMetricsHistory(final int maxHistory) {
      this.maxMetricsHistory = maxHistory;
      return this;
    }

    public Builder compilationTimeAlertThresholdMs(final double thresholdMs) {
      this.compilationTimeAlertThresholdMs = thresholdMs;
      return this;
    }

    public Builder memoryUsageAlertThreshold(final double threshold) {
      this.memoryUsageAlertThreshold = threshold;
      return this;
    }

    public Builder cpuUsageAlertThreshold(final double threshold) {
      this.cpuUsageAlertThreshold = threshold;
      return this;
    }

    public JitPerformanceMonitorConfig build() {
      return new JitPerformanceMonitorConfig(this);
    }
  }
}

/**
 * Monitoring states.
 */
enum MonitoringState {
  STOPPED,
  STARTING,
  RUNNING,
  STOPPING
}

/**
 * Types of compilation.
 */
enum CompilationType {
  BASELINE,
  OPTIMIZING,
  TIERED,
  SPECULATIVE,
  PROFILE_GUIDED
}

/**
 * Represents an active compilation session.
 */
final class CompilationSession {
  private final String moduleId;
  private final String functionName; // null for module-level compilation
  private final CompilationType compilationType;
  private final CompilationTier tier;
  private final long startTimeMs;

  public CompilationSession(final String moduleId,
                            final String functionName,
                            final CompilationType compilationType,
                            final CompilationTier tier,
                            final long startTimeMs) {
    this.moduleId = Objects.requireNonNull(moduleId);
    this.functionName = functionName;
    this.compilationType = Objects.requireNonNull(compilationType);
    this.tier = Objects.requireNonNull(tier);
    this.startTimeMs = startTimeMs;
  }

  public String getModuleId() { return moduleId; }
  public String getFunctionName() { return functionName; }
  public CompilationType getCompilationType() { return compilationType; }
  public CompilationTier getTier() { return tier; }
  public long getStartTimeMs() { return startTimeMs; }

  @Override
  public String toString() {
    return String.format("CompilationSession{module=%s, function=%s, type=%s, tier=%s}",
        moduleId, functionName, compilationType, tier);
  }
}

/**
 * Result of a compilation.
 */
final class CompilationResult {
  private final boolean success;
  private final String errorMessage;
  private final int codeSizeBytes;
  private final Map<String, Object> additionalMetrics;

  public CompilationResult(final boolean success,
                           final String errorMessage,
                           final int codeSizeBytes,
                           final Map<String, Object> additionalMetrics) {
    this.success = success;
    this.errorMessage = errorMessage;
    this.codeSizeBytes = codeSizeBytes;
    this.additionalMetrics = additionalMetrics != null ?
        Collections.unmodifiableMap(new HashMap<>(additionalMetrics)) : Collections.emptyMap();
  }

  public boolean isSuccess() { return success; }
  public String getErrorMessage() { return errorMessage; }
  public int getCodeSizeBytes() { return codeSizeBytes; }
  public Map<String, Object> getAdditionalMetrics() { return additionalMetrics; }

  public static CompilationResult success(final int codeSizeBytes) {
    return new CompilationResult(true, null, codeSizeBytes, null);
  }

  public static CompilationResult success(final int codeSizeBytes,
                                          final Map<String, Object> additionalMetrics) {
    return new CompilationResult(true, null, codeSizeBytes, additionalMetrics);
  }

  public static CompilationResult failure(final String errorMessage) {
    return new CompilationResult(false, errorMessage, 0, null);
  }
}

/**
 * Compilation metrics for a module.
 */
final class CompilationMetrics {
  private final String moduleId;
  private final AtomicLong totalCompilations;
  private final AtomicLong successfulCompilations;
  private final AtomicLong totalCompilationTimeMs;
  private final AtomicLong totalCodeSizeBytes;
  private final Map<CompilationType, AtomicLong> compilationsByType;
  private final Map<CompilationTier, AtomicLong> compilationsByTier;

  public CompilationMetrics(final String moduleId) {
    this.moduleId = Objects.requireNonNull(moduleId);
    this.totalCompilations = new AtomicLong(0);
    this.successfulCompilations = new AtomicLong(0);
    this.totalCompilationTimeMs = new AtomicLong(0);
    this.totalCodeSizeBytes = new AtomicLong(0);
    this.compilationsByType = new ConcurrentHashMap<>();
    this.compilationsByTier = new ConcurrentHashMap<>();
  }

  public void recordCompilation(final CompilationType type,
                                final CompilationTier tier,
                                final boolean success,
                                final long compilationTimeMs,
                                final int codeSizeBytes) {
    totalCompilations.incrementAndGet();
    if (success) {
      successfulCompilations.incrementAndGet();
      totalCodeSizeBytes.addAndGet(codeSizeBytes);
    }
    totalCompilationTimeMs.addAndGet(compilationTimeMs);

    compilationsByType.computeIfAbsent(type, k -> new AtomicLong(0)).incrementAndGet();
    compilationsByTier.computeIfAbsent(tier, k -> new AtomicLong(0)).incrementAndGet();
  }

  public String getModuleId() { return moduleId; }
  public long getTotalCompilations() { return totalCompilations.get(); }
  public long getSuccessfulCompilations() { return successfulCompilations.get(); }
  public long getTotalCompilationTimeMs() { return totalCompilationTimeMs.get(); }
  public long getTotalCodeSizeBytes() { return totalCodeSizeBytes.get(); }

  public double getSuccessRate() {
    final long total = totalCompilations.get();
    return total == 0 ? 0.0 : (double) successfulCompilations.get() / total;
  }

  public double getAverageCompilationTimeMs() {
    final long total = totalCompilations.get();
    return total == 0 ? 0.0 : (double) totalCompilationTimeMs.get() / total;
  }
}

/**
 * Function-specific compilation and optimization metrics.
 */
final class FunctionMetrics {
  private final String moduleId;
  private final String functionName;
  private final CompilationMetrics compilationMetrics;
  private final Map<String, OptimizationMetrics> optimizationMetrics;
  private final List<DeoptimizationEvent> deoptimizationEvents;

  public FunctionMetrics(final String moduleId, final String functionName) {
    this.moduleId = Objects.requireNonNull(moduleId);
    this.functionName = Objects.requireNonNull(functionName);
    this.compilationMetrics = new CompilationMetrics(moduleId + "::" + functionName);
    this.optimizationMetrics = new ConcurrentHashMap<>();
    this.deoptimizationEvents = Collections.synchronizedList(new ArrayList<>());
  }

  public void recordCompilation(final CompilationType type,
                                final CompilationTier tier,
                                final boolean success,
                                final long compilationTimeMs,
                                final int codeSizeBytes) {
    compilationMetrics.recordCompilation(type, tier, success, compilationTimeMs, codeSizeBytes);
  }

  public void addOptimizationMetrics(final String optimizationType, final OptimizationMetrics metrics) {
    optimizationMetrics.put(optimizationType, metrics);
  }

  public void recordDeoptimization(final String reason, final Map<String, Object> context) {
    deoptimizationEvents.add(new DeoptimizationEvent(reason, context, System.currentTimeMillis()));
  }

  public String getModuleId() { return moduleId; }
  public String getFunctionName() { return functionName; }
  public CompilationMetrics getCompilationMetrics() { return compilationMetrics; }
  public Map<String, OptimizationMetrics> getOptimizationMetrics() { return Collections.unmodifiableMap(optimizationMetrics); }
  public List<DeoptimizationEvent> getDeoptimizationEvents() { return Collections.unmodifiableList(deoptimizationEvents); }
}

/**
 * Optimization-specific metrics.
 */
final class OptimizationMetrics {
  private final String optimizationType;
  private final long optimizationTimeMs;
  private final double performanceImprovement;
  private final int codeSizeChange;
  private final boolean successful;
  private final Map<String, Object> additionalMetrics;

  public OptimizationMetrics(final String optimizationType,
                             final long optimizationTimeMs,
                             final double performanceImprovement,
                             final int codeSizeChange,
                             final boolean successful,
                             final Map<String, Object> additionalMetrics) {
    this.optimizationType = Objects.requireNonNull(optimizationType);
    this.optimizationTimeMs = optimizationTimeMs;
    this.performanceImprovement = performanceImprovement;
    this.codeSizeChange = codeSizeChange;
    this.successful = successful;
    this.additionalMetrics = additionalMetrics != null ?
        Collections.unmodifiableMap(new HashMap<>(additionalMetrics)) : Collections.emptyMap();
  }

  public String getOptimizationType() { return optimizationType; }
  public long getOptimizationTimeMs() { return optimizationTimeMs; }
  public double getPerformanceImprovement() { return performanceImprovement; }
  public int getCodeSizeChange() { return codeSizeChange; }
  public boolean isSuccessful() { return successful; }
  public Map<String, Object> getAdditionalMetrics() { return additionalMetrics; }
}


/**
 * System resource monitoring.
 */
final class SystemResourceMonitor {
  private final JitPerformanceMonitorConfig config;
  private final AtomicReference<ResourceUsage> currentUsage;

  public SystemResourceMonitor(final JitPerformanceMonitorConfig config) {
    this.config = Objects.requireNonNull(config);
    this.currentUsage = new AtomicReference<>(new ResourceUsage(0.0, 0.0, 0L));
  }

  public void start() {
    // Initialize resource monitoring
    updateResourceUsage();
  }

  public void stop() {
    // Clean up resource monitoring
  }

  public void performHealthCheck() {
    updateResourceUsage();
  }

  public ResourceUsage getCurrentResourceUsage() {
    return currentUsage.get();
  }

  private void updateResourceUsage() {
    if (config.isEnableResourceMonitoring()) {
      // Simulate resource usage monitoring
      final Runtime runtime = Runtime.getRuntime();
      final double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
      final double cpuUsage = 0.5; // Placeholder - would use actual CPU monitoring
      final long timestamp = System.currentTimeMillis();

      currentUsage.set(new ResourceUsage(cpuUsage, memoryUsage, timestamp));
    }
  }
}

/**
 * Current system resource usage.
 */
final class ResourceUsage {
  private final double cpuUsage;
  private final double memoryUsage;
  private final long timestamp;

  public ResourceUsage(final double cpuUsage, final double memoryUsage, final long timestamp) {
    this.cpuUsage = cpuUsage;
    this.memoryUsage = memoryUsage;
    this.timestamp = timestamp;
  }

  public double getCpuUsage() { return cpuUsage; }
  public double getMemoryUsage() { return memoryUsage; }
  public long getTimestamp() { return timestamp; }
}

/**
 * Aggregates performance data over time.
 */
final class PerformanceAggregator {
  private final JitPerformanceMonitorConfig config;
  private final Map<String, Object> aggregatedMetrics;

  public PerformanceAggregator(final JitPerformanceMonitorConfig config) {
    this.config = Objects.requireNonNull(config);
    this.aggregatedMetrics = new ConcurrentHashMap<>();
  }

  public void recordCompilation(final CompilationSession session,
                                final CompilationResult result,
                                final long compilationTimeMs) {
    // Record compilation data for aggregation
  }

  public void recordOptimization(final String moduleId,
                                 final String functionName,
                                 final String optimizationType,
                                 final OptimizationMetrics metrics) {
    // Record optimization data for aggregation
  }

  public void performAggregation() {
    // Perform periodic aggregation of collected data
  }

  public Map<String, Object> getAggregatedMetrics() {
    return Collections.unmodifiableMap(aggregatedMetrics);
  }

  public void reset() {
    aggregatedMetrics.clear();
  }
}

/**
 * Manages performance alerts and thresholds.
 */
final class AlertManager {
  private final JitPerformanceMonitorConfig config;
  private final List<Alert> activeAlerts;

  public AlertManager(final JitPerformanceMonitorConfig config) {
    this.config = Objects.requireNonNull(config);
    this.activeAlerts = Collections.synchronizedList(new ArrayList<>());
  }

  public void checkCompilationAlerts(final CompilationSession session,
                                     final CompilationResult result,
                                     final long compilationTimeMs) {
    if (config.isEnableAlerts()) {
      if (compilationTimeMs > config.getCompilationTimeAlertThresholdMs()) {
        addAlert(Alert.compilationTimeAlert(session, compilationTimeMs));
      }
      if (!result.isSuccess()) {
        addAlert(Alert.compilationFailureAlert(session, result));
      }
    }
  }

  public void checkDeoptimizationAlerts(final String moduleId,
                                        final String functionName,
                                        final String reason) {
    if (config.isEnableAlerts()) {
      addAlert(Alert.deoptimizationAlert(moduleId, functionName, reason));
    }
  }

  public void checkSystemHealthAlerts(final ResourceUsage resourceUsage) {
    if (config.isEnableAlerts()) {
      if (resourceUsage.getMemoryUsage() > config.getMemoryUsageAlertThreshold()) {
        addAlert(Alert.memoryUsageAlert(resourceUsage.getMemoryUsage()));
      }
      if (resourceUsage.getCpuUsage() > config.getCpuUsageAlertThreshold()) {
        addAlert(Alert.cpuUsageAlert(resourceUsage.getCpuUsage()));
      }
    }
  }

  public List<Alert> getActiveAlerts() {
    return Collections.unmodifiableList(activeAlerts);
  }

  public void reset() {
    activeAlerts.clear();
  }

  private void addAlert(final Alert alert) {
    activeAlerts.add(alert);
    // Limit the number of active alerts
    while (activeAlerts.size() > 100) {
      activeAlerts.remove(0);
    }
  }
}

/**
 * Performance alert.
 */
final class Alert {
  private final AlertType type;
  private final String message;
  private final long timestamp;
  private final Map<String, Object> context;

  private Alert(final AlertType type,
                final String message,
                final Map<String, Object> context) {
    this.type = Objects.requireNonNull(type);
    this.message = Objects.requireNonNull(message);
    this.timestamp = System.currentTimeMillis();
    this.context = context != null ?
        Collections.unmodifiableMap(new HashMap<>(context)) : Collections.emptyMap();
  }

  public AlertType getType() { return type; }
  public String getMessage() { return message; }
  public long getTimestamp() { return timestamp; }
  public Map<String, Object> getContext() { return context; }

  public static Alert compilationTimeAlert(final CompilationSession session, final long timeMs) {
    final Map<String, Object> context = new HashMap<>();
    context.put("module_id", session.getModuleId());
    context.put("function_name", session.getFunctionName());
    context.put("compilation_time_ms", timeMs);
    return new Alert(AlertType.COMPILATION_TIME,
        String.format("Long compilation time: %dms for %s", timeMs, session.getModuleId()),
        context);
  }

  public static Alert compilationFailureAlert(final CompilationSession session, final CompilationResult result) {
    final Map<String, Object> context = new HashMap<>();
    context.put("module_id", session.getModuleId());
    context.put("function_name", session.getFunctionName());
    context.put("error_message", result.getErrorMessage());
    return new Alert(AlertType.COMPILATION_FAILURE,
        String.format("Compilation failed for %s: %s", session.getModuleId(), result.getErrorMessage()),
        context);
  }

  public static Alert deoptimizationAlert(final String moduleId,
                                          final String functionName,
                                          final String reason) {
    final Map<String, Object> context = new HashMap<>();
    context.put("module_id", moduleId);
    context.put("function_name", functionName);
    context.put("reason", reason);
    return new Alert(AlertType.DEOPTIMIZATION,
        String.format("Deoptimization occurred: %s::%s - %s", moduleId, functionName, reason),
        context);
  }

  public static Alert memoryUsageAlert(final double usage) {
    final Map<String, Object> context = new HashMap<>();
    context.put("memory_usage", usage);
    return new Alert(AlertType.MEMORY_USAGE,
        String.format("High memory usage: %.2f%%", usage * 100),
        context);
  }

  public static Alert cpuUsageAlert(final double usage) {
    final Map<String, Object> context = new HashMap<>();
    context.put("cpu_usage", usage);
    return new Alert(AlertType.CPU_USAGE,
        String.format("High CPU usage: %.2f%%", usage * 100),
        context);
  }
}

/**
 * Types of alerts.
 */
enum AlertType {
  COMPILATION_TIME,
  COMPILATION_FAILURE,
  DEOPTIMIZATION,
  MEMORY_USAGE,
  CPU_USAGE,
  SYSTEM_HEALTH
}

/**
 * Comprehensive JIT performance metrics.
 */
final class JitPerformanceMetrics {
  private final long totalCompilations;
  private final long totalCompilationTimeMs;
  private final int trackedModules;
  private final int trackedFunctions;
  private final Map<String, Object> aggregatedMetrics;
  private final ResourceUsage currentResourceUsage;
  private final List<Alert> activeAlerts;

  public JitPerformanceMetrics(final long totalCompilations,
                               final long totalCompilationTimeMs,
                               final int trackedModules,
                               final int trackedFunctions,
                               final Map<String, Object> aggregatedMetrics,
                               final ResourceUsage currentResourceUsage,
                               final List<Alert> activeAlerts) {
    this.totalCompilations = totalCompilations;
    this.totalCompilationTimeMs = totalCompilationTimeMs;
    this.trackedModules = trackedModules;
    this.trackedFunctions = trackedFunctions;
    this.aggregatedMetrics = Collections.unmodifiableMap(new HashMap<>(aggregatedMetrics));
    this.currentResourceUsage = currentResourceUsage;
    this.activeAlerts = Collections.unmodifiableList(new ArrayList<>(activeAlerts));
  }

  public long getTotalCompilations() { return totalCompilations; }
  public long getTotalCompilationTimeMs() { return totalCompilationTimeMs; }
  public int getTrackedModules() { return trackedModules; }
  public int getTrackedFunctions() { return trackedFunctions; }
  public Map<String, Object> getAggregatedMetrics() { return aggregatedMetrics; }
  public ResourceUsage getCurrentResourceUsage() { return currentResourceUsage; }
  public List<Alert> getActiveAlerts() { return activeAlerts; }

  public double getAverageCompilationTimeMs() {
    return totalCompilations == 0 ? 0.0 : (double) totalCompilationTimeMs / totalCompilations;
  }

  @Override
  public String toString() {
    return String.format("JitPerformanceMetrics{compilations=%d, avg_time=%.2fms, " +
                         "modules=%d, functions=%d, alerts=%d}",
        totalCompilations, getAverageCompilationTimeMs(), trackedModules, trackedFunctions, activeAlerts.size());
  }
}

/**
 * Exception thrown by JIT monitoring operations.
 */
final class JitMonitoringException extends RuntimeException {
  public JitMonitoringException(final String message) {
    super(message);
  }

  public JitMonitoringException(final String message, final Throwable cause) {
    super(message, cause);
  }
}