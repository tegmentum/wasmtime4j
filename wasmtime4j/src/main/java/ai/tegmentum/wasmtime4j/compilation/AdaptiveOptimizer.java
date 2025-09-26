package ai.tegmentum.wasmtime4j.compilation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Adaptive optimizer that adjusts compilation strategies based on runtime profiling data.
 *
 * <p>This optimizer monitors execution patterns, performance characteristics, and resource
 * utilization to make informed decisions about when and how to optimize WebAssembly code.
 * It uses machine learning-like heuristics to adapt to changing workload patterns.
 *
 * @since 1.0.0
 */
public final class AdaptiveOptimizer {

  private static final Logger LOGGER = Logger.getLogger(AdaptiveOptimizer.class.getName());

  private final AdaptiveOptimizerConfig config;
  private final Map&lt;String, FunctionProfile&gt; functionProfiles;
  private final Map&lt;String, ModuleProfile&gt; moduleProfiles;
  private final AtomicReference&lt;SystemProfile&gt; systemProfile;
  private final OptimizationDecisionEngine decisionEngine;
  private final AtomicLong totalOptimizations;
  private final AtomicLong successfulOptimizations;

  /**
   * Creates a new adaptive optimizer with the specified configuration.
   *
   * @param config the optimizer configuration
   * @throws IllegalArgumentException if config is null
   */
  public AdaptiveOptimizer(final AdaptiveOptimizerConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Adaptive optimizer configuration cannot be null");
    }
    this.config = config;
    this.functionProfiles = new ConcurrentHashMap&lt;&gt;();
    this.moduleProfiles = new ConcurrentHashMap&lt;&gt;();
    this.systemProfile = new AtomicReference&lt;&gt;(new SystemProfile());
    this.decisionEngine = new OptimizationDecisionEngine(config);
    this.totalOptimizations = new AtomicLong(0);
    this.successfulOptimizations = new AtomicLong(0);
  }

  /**
   * Records execution data for a function.
   *
   * @param moduleId the module identifier
   * @param functionName the function name
   * @param executionData the execution data
   * @throws IllegalArgumentException if any parameter is null
   */
  public void recordExecution(final String moduleId,
                              final String functionName,
                              final ExecutionData executionData) {
    if (moduleId == null) {
      throw new IllegalArgumentException("Module ID cannot be null");
    }
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (executionData == null) {
      throw new IllegalArgumentException("Execution data cannot be null");
    }

    final String functionKey = moduleId + "::" + functionName;
    functionProfiles.compute(functionKey, (key, profile) -&gt; {
      if (profile == null) {
        profile = new FunctionProfile(moduleId, functionName);
      }
      profile.addExecutionData(executionData);
      return profile;
    });

    moduleProfiles.compute(moduleId, (key, profile) -&gt; {
      if (profile == null) {
        profile = new ModuleProfile(moduleId);
      }
      profile.addExecutionData(functionName, executionData);
      return profile;
    });

    updateSystemProfile(executionData);
  }

  /**
   * Determines if a function should be optimized based on its profile.
   *
   * @param moduleId the module identifier
   * @param functionName the function name
   * @return optimization decision
   * @throws IllegalArgumentException if any parameter is null
   */
  public OptimizationDecision shouldOptimize(final String moduleId, final String functionName) {
    if (moduleId == null) {
      throw new IllegalArgumentException("Module ID cannot be null");
    }
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }

    final String functionKey = moduleId + "::" + functionName;
    final FunctionProfile functionProfile = functionProfiles.get(functionKey);
    final ModuleProfile moduleProfile = moduleProfiles.get(moduleId);
    final SystemProfile currentSystemProfile = systemProfile.get();

    if (functionProfile == null) {
      return OptimizationDecision.noOptimization("No profile data available");
    }

    return decisionEngine.makeDecision(functionProfile, moduleProfile, currentSystemProfile);
  }

  /**
   * Records the result of an optimization attempt.
   *
   * @param moduleId the module identifier
   * @param functionName the function name
   * @param result the optimization result
   * @throws IllegalArgumentException if any parameter is null
   */
  public void recordOptimizationResult(final String moduleId,
                                       final String functionName,
                                       final OptimizationResult result) {
    if (moduleId == null) {
      throw new IllegalArgumentException("Module ID cannot be null");
    }
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (result == null) {
      throw new IllegalArgumentException("Optimization result cannot be null");
    }

    totalOptimizations.incrementAndGet();
    if (result.isSuccessful()) {
      successfulOptimizations.incrementAndGet();
    }

    final String functionKey = moduleId + "::" + functionName;
    final FunctionProfile functionProfile = functionProfiles.get(functionKey);
    if (functionProfile != null) {
      functionProfile.recordOptimizationResult(result);
    }

    // Update decision engine with feedback
    decisionEngine.learnFromResult(functionProfile, result);

    LOGGER.fine(String.format("Optimization %s for %s: %s",
        result.isSuccessful() ? "succeeded" : "failed",
        functionKey,
        result.getDescription()));
  }

  /**
   * Gets the current optimization statistics.
   *
   * @return optimization statistics
   */
  public OptimizationStatistics getStatistics() {
    return new OptimizationStatistics(
        totalOptimizations.get(),
        successfulOptimizations.get(),
        functionProfiles.size(),
        moduleProfiles.size(),
        systemProfile.get().getCurrentLoad()
    );
  }

  /**
   * Gets the profile for a specific function.
   *
   * @param moduleId the module identifier
   * @param functionName the function name
   * @return function profile or null if not found
   * @throws IllegalArgumentException if any parameter is null
   */
  public FunctionProfile getFunctionProfile(final String moduleId, final String functionName) {
    if (moduleId == null) {
      throw new IllegalArgumentException("Module ID cannot be null");
    }
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }

    return functionProfiles.get(moduleId + "::" + functionName);
  }

  /**
   * Resets all profiling data and statistics.
   */
  public void reset() {
    functionProfiles.clear();
    moduleProfiles.clear();
    systemProfile.set(new SystemProfile());
    totalOptimizations.set(0);
    successfulOptimizations.set(0);
    decisionEngine.reset();
    LOGGER.info("Adaptive optimizer reset");
  }

  /**
   * Creates a default adaptive optimizer.
   *
   * @return default adaptive optimizer
   */
  public static AdaptiveOptimizer createDefault() {
    return new AdaptiveOptimizer(AdaptiveOptimizerConfig.createDefault());
  }

  /**
   * Creates an aggressive adaptive optimizer.
   *
   * @return aggressive adaptive optimizer
   */
  public static AdaptiveOptimizer createAggressive() {
    return new AdaptiveOptimizer(AdaptiveOptimizerConfig.createAggressive());
  }

  private void updateSystemProfile(final ExecutionData executionData) {
    systemProfile.updateAndGet(profile -&gt; profile.addExecutionData(executionData));
  }
}

/**
 * Configuration for the adaptive optimizer.
 */
final class AdaptiveOptimizerConfig {
  private final double hotFunctionThreshold;
  private final long minExecutionCount;
  private final double cpuUtilizationThreshold;
  private final long memoryThreshold;
  private final boolean enableMachineLearning;
  private final double learningRate;
  private final int maxProfileHistory;
  private final long adaptationIntervalMs;

  private AdaptiveOptimizerConfig(final Builder builder) {
    this.hotFunctionThreshold = builder.hotFunctionThreshold;
    this.minExecutionCount = builder.minExecutionCount;
    this.cpuUtilizationThreshold = builder.cpuUtilizationThreshold;
    this.memoryThreshold = builder.memoryThreshold;
    this.enableMachineLearning = builder.enableMachineLearning;
    this.learningRate = builder.learningRate;
    this.maxProfileHistory = builder.maxProfileHistory;
    this.adaptationIntervalMs = builder.adaptationIntervalMs;
  }

  public double getHotFunctionThreshold() { return hotFunctionThreshold; }
  public long getMinExecutionCount() { return minExecutionCount; }
  public double getCpuUtilizationThreshold() { return cpuUtilizationThreshold; }
  public long getMemoryThreshold() { return memoryThreshold; }
  public boolean isEnableMachineLearning() { return enableMachineLearning; }
  public double getLearningRate() { return learningRate; }
  public int getMaxProfileHistory() { return maxProfileHistory; }
  public long getAdaptationIntervalMs() { return adaptationIntervalMs; }

  public static AdaptiveOptimizerConfig createDefault() {
    return builder().build();
  }

  public static AdaptiveOptimizerConfig createAggressive() {
    return builder()
        .hotFunctionThreshold(0.05) // Lower threshold for aggressive optimization
        .minExecutionCount(50)
        .cpuUtilizationThreshold(0.4)
        .enableMachineLearning(true)
        .learningRate(0.1)
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private double hotFunctionThreshold = 0.1; // 10% of execution time
    private long minExecutionCount = 100;
    private double cpuUtilizationThreshold = 0.6;
    private long memoryThreshold = 1024 * 1024 * 100; // 100MB
    private boolean enableMachineLearning = false;
    private double learningRate = 0.05;
    private int maxProfileHistory = 1000;
    private long adaptationIntervalMs = 5000; // 5 seconds

    public Builder hotFunctionThreshold(final double threshold) {
      this.hotFunctionThreshold = threshold;
      return this;
    }

    public Builder minExecutionCount(final long count) {
      this.minExecutionCount = count;
      return this;
    }

    public Builder cpuUtilizationThreshold(final double threshold) {
      this.cpuUtilizationThreshold = threshold;
      return this;
    }

    public Builder memoryThreshold(final long threshold) {
      this.memoryThreshold = threshold;
      return this;
    }

    public Builder enableMachineLearning(final boolean enable) {
      this.enableMachineLearning = enable;
      return this;
    }

    public Builder learningRate(final double rate) {
      this.learningRate = rate;
      return this;
    }

    public Builder maxProfileHistory(final int maxHistory) {
      this.maxProfileHistory = maxHistory;
      return this;
    }

    public Builder adaptationIntervalMs(final long intervalMs) {
      this.adaptationIntervalMs = intervalMs;
      return this;
    }

    public AdaptiveOptimizerConfig build() {
      return new AdaptiveOptimizerConfig(this);
    }
  }
}

/**
 * Runtime execution data for a single execution.
 */
final class ExecutionData {
  private final long executionTimeNs;
  private final long memoryUsed;
  private final double cpuUtilization;
  private final long timestamp;
  private final Map&lt;String, Object&gt; additionalMetrics;

  public ExecutionData(final long executionTimeNs,
                       final long memoryUsed,
                       final double cpuUtilization,
                       final Map&lt;String, Object&gt; additionalMetrics) {
    this.executionTimeNs = executionTimeNs;
    this.memoryUsed = memoryUsed;
    this.cpuUtilization = cpuUtilization;
    this.timestamp = System.currentTimeMillis();
    this.additionalMetrics = additionalMetrics != null ?
        Collections.unmodifiableMap(new HashMap&lt;&gt;(additionalMetrics)) : Collections.emptyMap();
  }

  public long getExecutionTimeNs() { return executionTimeNs; }
  public long getMemoryUsed() { return memoryUsed; }
  public double getCpuUtilization() { return cpuUtilization; }
  public long getTimestamp() { return timestamp; }
  public Map&lt;String, Object&gt; getAdditionalMetrics() { return additionalMetrics; }

  public double getExecutionTimeMs() {
    return executionTimeNs / 1_000_000.0;
  }
}

/**
 * Profile data for a specific function.
 */
final class FunctionProfile {
  private final String moduleId;
  private final String functionName;
  private final List&lt;ExecutionData&gt; executionHistory;
  private final AtomicLong totalExecutions;
  private final AtomicLong totalExecutionTimeNs;
  private final List&lt;OptimizationResult&gt; optimizationHistory;
  private volatile double averageExecutionTimeNs;
  private volatile boolean isHot;

  public FunctionProfile(final String moduleId, final String functionName) {
    this.moduleId = Objects.requireNonNull(moduleId);
    this.functionName = Objects.requireNonNull(functionName);
    this.executionHistory = Collections.synchronizedList(new ArrayList&lt;&gt;());
    this.totalExecutions = new AtomicLong(0);
    this.totalExecutionTimeNs = new AtomicLong(0);
    this.optimizationHistory = Collections.synchronizedList(new ArrayList&lt;&gt;());
    this.averageExecutionTimeNs = 0.0;
    this.isHot = false;
  }

  public void addExecutionData(final ExecutionData data) {
    executionHistory.add(data);
    totalExecutions.incrementAndGet();
    totalExecutionTimeNs.addAndGet(data.getExecutionTimeNs());

    // Update rolling average
    averageExecutionTimeNs = (double) totalExecutionTimeNs.get() / totalExecutions.get();

    // Simple hot function detection
    isHot = totalExecutions.get() &gt; 1000 && averageExecutionTimeNs &gt; 5_000_000; // 5ms average
  }

  public void recordOptimizationResult(final OptimizationResult result) {
    optimizationHistory.add(result);
  }

  public String getModuleId() { return moduleId; }
  public String getFunctionName() { return functionName; }
  public long getTotalExecutions() { return totalExecutions.get(); }
  public double getAverageExecutionTimeNs() { return averageExecutionTimeNs; }
  public boolean isHot() { return isHot; }
  public List&lt;ExecutionData&gt; getExecutionHistory() { return Collections.unmodifiableList(executionHistory); }
  public List&lt;OptimizationResult&gt; getOptimizationHistory() { return Collections.unmodifiableList(optimizationHistory); }

  public double getExecutionTimePercentage(final double totalSystemExecutionTime) {
    return totalSystemExecutionTime == 0.0 ? 0.0 :
        ((double) totalExecutionTimeNs.get()) / (totalSystemExecutionTime * 1_000_000.0);
  }
}

/**
 * Profile data for a WebAssembly module.
 */
final class ModuleProfile {
  private final String moduleId;
  private final Map&lt;String, FunctionProfile&gt; functionProfiles;
  private final AtomicLong totalModuleExecutions;
  private final AtomicLong totalModuleExecutionTimeNs;

  public ModuleProfile(final String moduleId) {
    this.moduleId = Objects.requireNonNull(moduleId);
    this.functionProfiles = new ConcurrentHashMap&lt;&gt;();
    this.totalModuleExecutions = new AtomicLong(0);
    this.totalModuleExecutionTimeNs = new AtomicLong(0);
  }

  public void addExecutionData(final String functionName, final ExecutionData data) {
    totalModuleExecutions.incrementAndGet();
    totalModuleExecutionTimeNs.addAndGet(data.getExecutionTimeNs());
  }

  public String getModuleId() { return moduleId; }
  public long getTotalExecutions() { return totalModuleExecutions.get(); }
  public long getTotalExecutionTimeNs() { return totalModuleExecutionTimeNs.get(); }

  public double getAverageExecutionTimeNs() {
    final long executions = totalModuleExecutions.get();
    return executions == 0 ? 0.0 : (double) totalModuleExecutionTimeNs.get() / executions;
  }
}

/**
 * System-wide profile data.
 */
final class SystemProfile {
  private final AtomicLong totalSystemExecutions;
  private final AtomicLong totalSystemExecutionTimeNs;
  private final AtomicReference&lt;Double&gt; currentLoad;

  public SystemProfile() {
    this.totalSystemExecutions = new AtomicLong(0);
    this.totalSystemExecutionTimeNs = new AtomicLong(0);
    this.currentLoad = new AtomicReference&lt;&gt;(0.0);
  }

  public SystemProfile addExecutionData(final ExecutionData data) {
    totalSystemExecutions.incrementAndGet();
    totalSystemExecutionTimeNs.addAndGet(data.getExecutionTimeNs());
    currentLoad.set(data.getCpuUtilization());
    return this;
  }

  public long getTotalExecutions() { return totalSystemExecutions.get(); }
  public long getTotalExecutionTimeNs() { return totalSystemExecutionTimeNs.get(); }
  public double getCurrentLoad() { return currentLoad.get(); }
}

/**
 * Decision engine for optimization decisions.
 */
final class OptimizationDecisionEngine {
  private final AdaptiveOptimizerConfig config;
  private final Map&lt;String, Double&gt; optimizationWeights;

  public OptimizationDecisionEngine(final AdaptiveOptimizerConfig config) {
    this.config = Objects.requireNonNull(config);
    this.optimizationWeights = new ConcurrentHashMap&lt;&gt;();
    initializeWeights();
  }

  public OptimizationDecision makeDecision(final FunctionProfile functionProfile,
                                           final ModuleProfile moduleProfile,
                                           final SystemProfile systemProfile) {
    if (functionProfile == null) {
      return OptimizationDecision.noOptimization("No function profile available");
    }

    // Check minimum execution threshold
    if (functionProfile.getTotalExecutions() &lt; config.getMinExecutionCount()) {
      return OptimizationDecision.noOptimization("Insufficient execution count");
    }

    // Check if system is under load
    if (systemProfile.getCurrentLoad() &gt; config.getCpuUtilizationThreshold()) {
      return OptimizationDecision.noOptimization("System under high load");
    }

    // Check if function is hot
    if (functionProfile.isHot()) {
      return OptimizationDecision.optimize(CompilationTier.HIGHLY_OPTIMIZED,
          "Hot function detected");
    }

    // Calculate optimization score
    final double score = calculateOptimizationScore(functionProfile, moduleProfile, systemProfile);

    if (score &gt; 0.8) {
      return OptimizationDecision.optimize(CompilationTier.HIGHLY_OPTIMIZED,
          "High optimization score: " + score);
    } else if (score &gt; 0.5) {
      return OptimizationDecision.optimize(CompilationTier.OPTIMIZED,
          "Medium optimization score: " + score);
    } else {
      return OptimizationDecision.noOptimization("Low optimization score: " + score);
    }
  }

  public void learnFromResult(final FunctionProfile functionProfile, final OptimizationResult result) {
    if (config.isEnableMachineLearning() && functionProfile != null) {
      // Simple learning: adjust weights based on success/failure
      final String functionKey = functionProfile.getModuleId() + "::" + functionProfile.getFunctionName();
      final double currentWeight = optimizationWeights.getOrDefault(functionKey, 1.0);
      final double adjustment = result.isSuccessful() ?
          config.getLearningRate() : -config.getLearningRate();
      optimizationWeights.put(functionKey, Math.max(0.1, Math.min(2.0, currentWeight + adjustment)));
    }
  }

  public void reset() {
    optimizationWeights.clear();
    initializeWeights();
  }

  private void initializeWeights() {
    // Initialize default weights for different optimization strategies
    optimizationWeights.put("default", 1.0);
  }

  private double calculateOptimizationScore(final FunctionProfile functionProfile,
                                            final ModuleProfile moduleProfile,
                                            final SystemProfile systemProfile) {
    double score = 0.0;

    // Factor 1: Execution frequency
    final double executionScore = Math.min(1.0, functionProfile.getTotalExecutions() / 10000.0);
    score += executionScore * 0.4;

    // Factor 2: Average execution time
    final double timeScore = Math.min(1.0, functionProfile.getAverageExecutionTimeNs() / 10_000_000.0); // 10ms threshold
    score += timeScore * 0.3;

    // Factor 3: System load (inverse)
    final double loadScore = 1.0 - systemProfile.getCurrentLoad();
    score += loadScore * 0.2;

    // Factor 4: Machine learning weight
    if (config.isEnableMachineLearning()) {
      final String functionKey = functionProfile.getModuleId() + "::" + functionProfile.getFunctionName();
      final double mlWeight = optimizationWeights.getOrDefault(functionKey, 1.0);
      score *= mlWeight;
    }

    return Math.max(0.0, Math.min(1.0, score));
  }
}

/**
 * Optimization decision result.
 */
final class OptimizationDecision {
  private final boolean shouldOptimize;
  private final CompilationTier recommendedTier;
  private final String reason;

  private OptimizationDecision(final boolean shouldOptimize,
                               final CompilationTier recommendedTier,
                               final String reason) {
    this.shouldOptimize = shouldOptimize;
    this.recommendedTier = recommendedTier;
    this.reason = Objects.requireNonNull(reason);
  }

  public boolean shouldOptimize() { return shouldOptimize; }
  public CompilationTier getRecommendedTier() { return recommendedTier; }
  public String getReason() { return reason; }

  public static OptimizationDecision optimize(final CompilationTier tier, final String reason) {
    return new OptimizationDecision(true, tier, reason);
  }

  public static OptimizationDecision noOptimization(final String reason) {
    return new OptimizationDecision(false, null, reason);
  }

  @Override
  public String toString() {
    return String.format("OptimizationDecision{optimize=%s, tier=%s, reason='%s'}",
        shouldOptimize, recommendedTier, reason);
  }
}

/**
 * Result of an optimization attempt.
 */
final class OptimizationResult {
  private final boolean successful;
  private final String description;
  private final long compilationTimeMs;
  private final double performanceImprovement;
  private final String errorMessage;

  public OptimizationResult(final boolean successful,
                            final String description,
                            final long compilationTimeMs,
                            final double performanceImprovement,
                            final String errorMessage) {
    this.successful = successful;
    this.description = Objects.requireNonNull(description);
    this.compilationTimeMs = compilationTimeMs;
    this.performanceImprovement = performanceImprovement;
    this.errorMessage = errorMessage;
  }

  public boolean isSuccessful() { return successful; }
  public String getDescription() { return description; }
  public long getCompilationTimeMs() { return compilationTimeMs; }
  public double getPerformanceImprovement() { return performanceImprovement; }
  public String getErrorMessage() { return errorMessage; }

  public static OptimizationResult success(final String description,
                                           final long compilationTimeMs,
                                           final double performanceImprovement) {
    return new OptimizationResult(true, description, compilationTimeMs, performanceImprovement, null);
  }

  public static OptimizationResult failure(final String description, final String errorMessage) {
    return new OptimizationResult(false, description, 0, 0.0, errorMessage);
  }
}

/**
 * Statistics about optimization activities.
 */
final class OptimizationStatistics {
  private final long totalOptimizations;
  private final long successfulOptimizations;
  private final int trackedFunctions;
  private final int trackedModules;
  private final double currentSystemLoad;

  public OptimizationStatistics(final long totalOptimizations,
                                final long successfulOptimizations,
                                final int trackedFunctions,
                                final int trackedModules,
                                final double currentSystemLoad) {
    this.totalOptimizations = totalOptimizations;
    this.successfulOptimizations = successfulOptimizations;
    this.trackedFunctions = trackedFunctions;
    this.trackedModules = trackedModules;
    this.currentSystemLoad = currentSystemLoad;
  }

  public long getTotalOptimizations() { return totalOptimizations; }
  public long getSuccessfulOptimizations() { return successfulOptimizations; }
  public int getTrackedFunctions() { return trackedFunctions; }
  public int getTrackedModules() { return trackedModules; }
  public double getCurrentSystemLoad() { return currentSystemLoad; }

  public double getSuccessRate() {
    return totalOptimizations == 0 ? 0.0 : (double) successfulOptimizations / totalOptimizations;
  }

  @Override
  public String toString() {
    return String.format("OptimizationStatistics{total=%d, successful=%d, success_rate=%.2f%%, " +
                         "functions=%d, modules=%d, system_load=%.2f%%}",
        totalOptimizations, successfulOptimizations, getSuccessRate() * 100,
        trackedFunctions, trackedModules, currentSystemLoad * 100);
  }
}