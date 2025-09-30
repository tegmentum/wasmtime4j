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
 * Speculative optimizer that performs aggressive optimizations based on assumptions
 * about execution patterns, with support for deoptimization when assumptions fail.
 *
 * <p>This optimizer applies speculative optimizations like type specialization,
 * constant folding, and branch elimination based on observed execution patterns.
 * When speculation fails, it gracefully deoptimizes back to a safe baseline.
 *
 * @since 1.0.0
 */
public final class SpeculativeOptimizer {

  private static final Logger LOGGER = Logger.getLogger(SpeculativeOptimizer.class.getName());

  private final SpeculativeOptimizerConfig config;
  private final Map<String, SpeculationProfile> speculationProfiles;
  private final DeoptimizationManager deoptimizationManager;
  private final SpeculationDecisionEngine decisionEngine;
  private final AtomicLong totalSpeculations;
  private final AtomicLong successfulSpeculations;
  private final AtomicLong deoptimizations;

  /**
   * Creates a new speculative optimizer with the specified configuration.
   *
   * @param config the optimizer configuration
   * @throws IllegalArgumentException if config is null
   */
  public SpeculativeOptimizer(final SpeculativeOptimizerConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Speculative optimizer configuration cannot be null");
    }
    this.config = config;
    this.speculationProfiles = new ConcurrentHashMap<>();
    this.deoptimizationManager = new DeoptimizationManager(config);
    this.decisionEngine = new SpeculationDecisionEngine(config);
    this.totalSpeculations = new AtomicLong(0);
    this.successfulSpeculations = new AtomicLong(0);
    this.deoptimizations = new AtomicLong(0);
  }

  /**
   * Determines speculative optimizations to apply for a function.
   *
   * @param moduleId the module identifier
   * @param functionName the function name
   * @param executionProfile the execution profile
   * @return list of speculative optimizations to apply
   * @throws IllegalArgumentException if any parameter is null
   */
  public List<SpeculativeOptimization> determineSpeculations(final String moduleId,
                                                             final String functionName,
                                                             final ExecutionProfile executionProfile) {
    if (moduleId == null) {
      throw new IllegalArgumentException("Module ID cannot be null");
    }
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (executionProfile == null) {
      throw new IllegalArgumentException("Execution profile cannot be null");
    }

    final String functionKey = moduleId + "::" + functionName;
    final SpeculationProfile profile = speculationProfiles.computeIfAbsent(
        functionKey, k -> new SpeculationProfile(moduleId, functionName));

    return decisionEngine.determineSpeculations(profile, executionProfile);
  }

  /**
   * Records the result of a speculation attempt.
   *
   * @param moduleId the module identifier
   * @param functionName the function name
   * @param speculation the speculation that was applied
   * @param result the speculation result
   * @throws IllegalArgumentException if any parameter is null
   */
  public void recordSpeculationResult(final String moduleId,
                                      final String functionName,
                                      final SpeculativeOptimization speculation,
                                      final SpeculationResult result) {
    if (moduleId == null) {
      throw new IllegalArgumentException("Module ID cannot be null");
    }
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (speculation == null) {
      throw new IllegalArgumentException("Speculation cannot be null");
    }
    if (result == null) {
      throw new IllegalArgumentException("Speculation result cannot be null");
    }

    final String functionKey = moduleId + "::" + functionName;
    totalSpeculations.incrementAndGet();

    if (result.isSuccessful()) {
      successfulSpeculations.incrementAndGet();
    }

    final SpeculationProfile profile = speculationProfiles.get(functionKey);
    if (profile != null) {
      profile.recordSpeculationResult(speculation, result);
    }

    // Update decision engine with feedback
    decisionEngine.learnFromResult(speculation, result);

    LOGGER.fine(String.format("Speculation %s for %s::%s: %s",
        speculation.getType(),
        moduleId,
        functionName,
        result.isSuccessful() ? "succeeded" : "failed"));
  }

  /**
   * Records a deoptimization event.
   *
   * @param moduleId the module identifier
   * @param functionName the function name
   * @param reason the deoptimization reason
   * @param context additional context information
   * @throws IllegalArgumentException if any parameter is null
   */
  public void recordDeoptimization(final String moduleId,
                                   final String functionName,
                                   final DeoptimizationReason reason,
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

    deoptimizations.incrementAndGet();
    deoptimizationManager.recordDeoptimization(moduleId, functionName, reason, context);

    final String functionKey = moduleId + "::" + functionName;
    final SpeculationProfile profile = speculationProfiles.get(functionKey);
    if (profile != null) {
      profile.recordDeoptimization(reason);
    }

    LOGGER.info(String.format("Deoptimization for %s::%s: %s",
        moduleId, functionName, reason));
  }

  /**
   * Checks if a function should be deoptimized based on current conditions.
   *
   * @param moduleId the module identifier
   * @param functionName the function name
   * @param currentConditions the current runtime conditions
   * @return deoptimization decision
   * @throws IllegalArgumentException if any parameter is null
   */
  public DeoptimizationDecision shouldDeoptimize(final String moduleId,
                                                  final String functionName,
                                                  final RuntimeConditions currentConditions) {
    if (moduleId == null) {
      throw new IllegalArgumentException("Module ID cannot be null");
    }
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (currentConditions == null) {
      throw new IllegalArgumentException("Runtime conditions cannot be null");
    }

    return deoptimizationManager.shouldDeoptimize(moduleId, functionName, currentConditions);
  }

  /**
   * Gets the current speculation statistics.
   *
   * @return speculation statistics
   */
  public SpeculationStatistics getStatistics() {
    return new SpeculationStatistics(
        totalSpeculations.get(),
        successfulSpeculations.get(),
        deoptimizations.get(),
        speculationProfiles.size(),
        deoptimizationManager.getActiveSpeculations()
    );
  }

  /**
   * Resets all speculation data and statistics.
   */
  public void reset() {
    speculationProfiles.clear();
    deoptimizationManager.reset();
    decisionEngine.reset();
    totalSpeculations.set(0);
    successfulSpeculations.set(0);
    deoptimizations.set(0);
    LOGGER.info("Speculative optimizer reset");
  }

  /**
   * Creates a default speculative optimizer.
   *
   * @return default speculative optimizer
   */
  public static SpeculativeOptimizer createDefault() {
    return new SpeculativeOptimizer(SpeculativeOptimizerConfig.createDefault());
  }

  /**
   * Creates an aggressive speculative optimizer.
   *
   * @return aggressive speculative optimizer
   */
  public static SpeculativeOptimizer createAggressive() {
    return new SpeculativeOptimizer(SpeculativeOptimizerConfig.createAggressive());
  }
}

/**
 * Configuration for the speculative optimizer.
 */
final class SpeculativeOptimizerConfig {
  private final double speculationThreshold;
  private final int maxActiveSpeculations;
  private final long deoptimizationCooldownMs;
  private final double failureToleranceRate;
  private final boolean enableTypeSpecialization;
  private final boolean enableBranchElimination;
  private final boolean enableConstantFolding;
  private final boolean enableInlining;
  private final int maxSpeculationDepth;

  private SpeculativeOptimizerConfig(final Builder builder) {
    this.speculationThreshold = builder.speculationThreshold;
    this.maxActiveSpeculations = builder.maxActiveSpeculations;
    this.deoptimizationCooldownMs = builder.deoptimizationCooldownMs;
    this.failureToleranceRate = builder.failureToleranceRate;
    this.enableTypeSpecialization = builder.enableTypeSpecialization;
    this.enableBranchElimination = builder.enableBranchElimination;
    this.enableConstantFolding = builder.enableConstantFolding;
    this.enableInlining = builder.enableInlining;
    this.maxSpeculationDepth = builder.maxSpeculationDepth;
  }

  public double getSpeculationThreshold() { return speculationThreshold; }
  public int getMaxActiveSpeculations() { return maxActiveSpeculations; }
  public long getDeoptimizationCooldownMs() { return deoptimizationCooldownMs; }
  public double getFailureToleranceRate() { return failureToleranceRate; }
  public boolean isEnableTypeSpecialization() { return enableTypeSpecialization; }
  public boolean isEnableBranchElimination() { return enableBranchElimination; }
  public boolean isEnableConstantFolding() { return enableConstantFolding; }
  public boolean isEnableInlining() { return enableInlining; }
  public int getMaxSpeculationDepth() { return maxSpeculationDepth; }

  public static SpeculativeOptimizerConfig createDefault() {
    return builder().build();
  }

  public static SpeculativeOptimizerConfig createAggressive() {
    return builder()
        .speculationThreshold(0.6) // Lower threshold for aggressive speculation
        .maxActiveSpeculations(20)
        .failureToleranceRate(0.3) // Higher tolerance for failures
        .enableTypeSpecialization(true)
        .enableBranchElimination(true)
        .enableConstantFolding(true)
        .enableInlining(true)
        .maxSpeculationDepth(5)
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private double speculationThreshold = 0.8; // 80% confidence threshold
    private int maxActiveSpeculations = 10;
    private long deoptimizationCooldownMs = 5000; // 5 seconds
    private double failureToleranceRate = 0.1; // 10% failure tolerance
    private boolean enableTypeSpecialization = true;
    private boolean enableBranchElimination = false;
    private boolean enableConstantFolding = true;
    private boolean enableInlining = false;
    private int maxSpeculationDepth = 3;

    public Builder speculationThreshold(final double threshold) {
      this.speculationThreshold = threshold;
      return this;
    }

    public Builder maxActiveSpeculations(final int max) {
      this.maxActiveSpeculations = max;
      return this;
    }

    public Builder deoptimizationCooldownMs(final long cooldownMs) {
      this.deoptimizationCooldownMs = cooldownMs;
      return this;
    }

    public Builder failureToleranceRate(final double rate) {
      this.failureToleranceRate = rate;
      return this;
    }

    public Builder enableTypeSpecialization(final boolean enable) {
      this.enableTypeSpecialization = enable;
      return this;
    }

    public Builder enableBranchElimination(final boolean enable) {
      this.enableBranchElimination = enable;
      return this;
    }

    public Builder enableConstantFolding(final boolean enable) {
      this.enableConstantFolding = enable;
      return this;
    }

    public Builder enableInlining(final boolean enable) {
      this.enableInlining = enable;
      return this;
    }

    public Builder maxSpeculationDepth(final int depth) {
      this.maxSpeculationDepth = depth;
      return this;
    }

    public SpeculativeOptimizerConfig build() {
      return new SpeculativeOptimizerConfig(this);
    }
  }
}

/**
 * Represents a speculative optimization that can be applied.
 */
final class SpeculativeOptimization {
  private final SpeculationType type;
  private final String description;
  private final Map<String, Object> parameters;
  private final double confidence;
  private final List<String> assumptions;

  public SpeculativeOptimization(final SpeculationType type,
                                 final String description,
                                 final Map<String, Object> parameters,
                                 final double confidence,
                                 final List<String> assumptions) {
    this.type = Objects.requireNonNull(type);
    this.description = Objects.requireNonNull(description);
    this.parameters = parameters != null ?
        Collections.unmodifiableMap(new HashMap<>(parameters)) : Collections.emptyMap();
    this.confidence = confidence;
    this.assumptions = assumptions != null ?
        Collections.unmodifiableList(new ArrayList<>(assumptions)) : Collections.emptyList();
  }

  public SpeculationType getType() { return type; }
  public String getDescription() { return description; }
  public Map<String, Object> getParameters() { return parameters; }
  public double getConfidence() { return confidence; }
  public List<String> getAssumptions() { return assumptions; }

  @Override
  public String toString() {
    return String.format("SpeculativeOptimization{type=%s, confidence=%.2f, description='%s'}",
        type, confidence, description);
  }
}

/**
 * Types of speculative optimizations.
 */
enum SpeculationType {
  TYPE_SPECIALIZATION,
  BRANCH_ELIMINATION,
  CONSTANT_FOLDING,
  FUNCTION_INLINING,
  LOOP_UNROLLING,
  DEAD_CODE_ELIMINATION,
  VALUE_NUMBERING
}

/**
 * Result of a speculation attempt.
 */
final class SpeculationResult {
  private final boolean successful;
  private final String description;
  private final long compilationTimeMs;
  private final double performanceImprovement;
  private final List<String> violatedAssumptions;

  public SpeculationResult(final boolean successful,
                           final String description,
                           final long compilationTimeMs,
                           final double performanceImprovement,
                           final List<String> violatedAssumptions) {
    this.successful = successful;
    this.description = Objects.requireNonNull(description);
    this.compilationTimeMs = compilationTimeMs;
    this.performanceImprovement = performanceImprovement;
    this.violatedAssumptions = violatedAssumptions != null ?
        Collections.unmodifiableList(new ArrayList<>(violatedAssumptions)) : Collections.emptyList();
  }

  public boolean isSuccessful() { return successful; }
  public String getDescription() { return description; }
  public long getCompilationTimeMs() { return compilationTimeMs; }
  public double getPerformanceImprovement() { return performanceImprovement; }
  public List<String> getViolatedAssumptions() { return violatedAssumptions; }

  public static SpeculationResult success(final String description,
                                          final long compilationTimeMs,
                                          final double performanceImprovement) {
    return new SpeculationResult(true, description, compilationTimeMs, performanceImprovement, null);
  }

  public static SpeculationResult failure(final String description,
                                          final List<String> violatedAssumptions) {
    return new SpeculationResult(false, description, 0, 0.0, violatedAssumptions);
  }
}

/**
 * Profile data for speculation tracking.
 */
final class SpeculationProfile {
  private final String moduleId;
  private final String functionName;
  private final Map<SpeculationType, SpeculationTypeStats> typeStats;
  private final List<DeoptimizationEvent> deoptimizationHistory;
  private final AtomicLong lastDeoptimizationTime;

  public SpeculationProfile(final String moduleId, final String functionName) {
    this.moduleId = Objects.requireNonNull(moduleId);
    this.functionName = Objects.requireNonNull(functionName);
    this.typeStats = new ConcurrentHashMap<>();
    this.deoptimizationHistory = Collections.synchronizedList(new ArrayList<>());
    this.lastDeoptimizationTime = new AtomicLong(0);
  }

  public void recordSpeculationResult(final SpeculativeOptimization speculation,
                                      final SpeculationResult result) {
    typeStats.compute(speculation.getType(), (type, stats) -> {
      if (stats == null) {
        stats = new SpeculationTypeStats();
      }
      stats.recordResult(result);
      return stats;
    });
  }

  public void recordDeoptimization(final DeoptimizationReason reason) {
    final DeoptimizationEvent event = new DeoptimizationEvent(reason, System.currentTimeMillis());
    deoptimizationHistory.add(event);
    lastDeoptimizationTime.set(event.getTimestamp());
  }

  public String getModuleId() { return moduleId; }
  public String getFunctionName() { return functionName; }
  public Map<SpeculationType, SpeculationTypeStats> getTypeStats() { return Collections.unmodifiableMap(typeStats); }
  public List<DeoptimizationEvent> getDeoptimizationHistory() { return Collections.unmodifiableList(deoptimizationHistory); }
  public long getLastDeoptimizationTime() { return lastDeoptimizationTime.get(); }

  public double getSuccessRateForType(final SpeculationType type) {
    final SpeculationTypeStats stats = typeStats.get(type);
    return stats != null ? stats.getSuccessRate() : 0.0;
  }
}

/**
 * Statistics for a specific speculation type.
 */
final class SpeculationTypeStats {
  private final AtomicLong totalAttempts;
  private final AtomicLong successfulAttempts;
  private final AtomicReference<Double> averagePerformanceGain;

  public SpeculationTypeStats() {
    this.totalAttempts = new AtomicLong(0);
    this.successfulAttempts = new AtomicLong(0);
    this.averagePerformanceGain = new AtomicReference<>(0.0);
  }

  public void recordResult(final SpeculationResult result) {
    totalAttempts.incrementAndGet();
    if (result.isSuccessful()) {
      successfulAttempts.incrementAndGet();
      // Update rolling average of performance gain
      averagePerformanceGain.updateAndGet(currentAvg -> {
        final long successful = successfulAttempts.get();
        return ((currentAvg * (successful - 1)) + result.getPerformanceImprovement()) / successful;
      });
    }
  }

  public double getSuccessRate() {
    final long total = totalAttempts.get();
    return total == 0 ? 0.0 : (double) successfulAttempts.get() / total;
  }

  public double getAveragePerformanceGain() {
    return averagePerformanceGain.get();
  }
}

/**
 * Deoptimization reasons.
 */
enum DeoptimizationReason {
  TYPE_ASSUMPTION_VIOLATED,
  BRANCH_ASSUMPTION_VIOLATED,
  CONSTANT_ASSUMPTION_VIOLATED,
  PERFORMANCE_REGRESSION,
  RUNTIME_ERROR,
  SPECULATION_LIMIT_EXCEEDED
}

/**
 * Represents a deoptimization event.
 */
final class DeoptimizationEvent {
  private final DeoptimizationReason reason;
  private final long timestamp;

  public DeoptimizationEvent(final DeoptimizationReason reason, final long timestamp) {
    this.reason = Objects.requireNonNull(reason);
    this.timestamp = timestamp;
  }

  public DeoptimizationReason getReason() { return reason; }
  public long getTimestamp() { return timestamp; }
}

/**
 * Current runtime conditions for deoptimization decisions.
 */
final class RuntimeConditions {
  private final Map<String, Object> typeObservations;
  private final Map<String, Object> branchPatterns;
  private final double currentPerformance;
  private final long memoryUsage;

  public RuntimeConditions(final Map<String, Object> typeObservations,
                           final Map<String, Object> branchPatterns,
                           final double currentPerformance,
                           final long memoryUsage) {
    this.typeObservations = typeObservations != null ?
        Collections.unmodifiableMap(new HashMap<>(typeObservations)) : Collections.emptyMap();
    this.branchPatterns = branchPatterns != null ?
        Collections.unmodifiableMap(new HashMap<>(branchPatterns)) : Collections.emptyMap();
    this.currentPerformance = currentPerformance;
    this.memoryUsage = memoryUsage;
  }

  public Map<String, Object> getTypeObservations() { return typeObservations; }
  public Map<String, Object> getBranchPatterns() { return branchPatterns; }
  public double getCurrentPerformance() { return currentPerformance; }
  public long getMemoryUsage() { return memoryUsage; }
}

/**
 * Decision engine for speculation decisions.
 */
final class SpeculationDecisionEngine {
  private final SpeculativeOptimizerConfig config;

  public SpeculationDecisionEngine(final SpeculativeOptimizerConfig config) {
    this.config = Objects.requireNonNull(config);
  }

  public List<SpeculativeOptimization> determineSpeculations(final SpeculationProfile profile,
                                                             final ExecutionProfile executionProfile) {
    final List<SpeculativeOptimization> speculations = new ArrayList<>();

    // Check if we're in cooldown period after deoptimization
    final long timeSinceLastDeopt = System.currentTimeMillis() - profile.getLastDeoptimizationTime();
    if (timeSinceLastDeopt < config.getDeoptimizationCooldownMs()) {
      return speculations; // No speculations during cooldown
    }

    // Type specialization
    if (config.isEnableTypeSpecialization() && shouldSpeculateTypeSpecialization(profile, executionProfile)) {
      speculations.add(createTypeSpecializationOptimization(executionProfile));
    }

    // Constant folding
    if (config.isEnableConstantFolding() && shouldSpeculateConstantFolding(profile, executionProfile)) {
      speculations.add(createConstantFoldingOptimization(executionProfile));
    }

    // Branch elimination
    if (config.isEnableBranchElimination() && shouldSpeculateBranchElimination(profile, executionProfile)) {
      speculations.add(createBranchEliminationOptimization(executionProfile));
    }

    // Function inlining
    if (config.isEnableInlining() && shouldSpeculateInlining(profile, executionProfile)) {
      speculations.add(createInliningOptimization(executionProfile));
    }

    return speculations.subList(0, Math.min(speculations.size(), config.getMaxActiveSpeculations()));
  }

  public void learnFromResult(final SpeculativeOptimization speculation, final SpeculationResult result) {
    // Simple learning logic - could be extended with ML techniques
    // For now, just log the result for future analysis
  }

  public void reset() {
    // Reset any learned parameters
  }

  private boolean shouldSpeculateTypeSpecialization(final SpeculationProfile profile,
                                                    final ExecutionProfile executionProfile) {
    final double successRate = profile.getSuccessRateForType(SpeculationType.TYPE_SPECIALIZATION);
    return successRate > config.getSpeculationThreshold() || successRate == 0.0; // Try once if no history
  }

  private boolean shouldSpeculateConstantFolding(final SpeculationProfile profile,
                                                 final ExecutionProfile executionProfile) {
    final double successRate = profile.getSuccessRateForType(SpeculationType.CONSTANT_FOLDING);
    return successRate > config.getSpeculationThreshold() || successRate == 0.0;
  }

  private boolean shouldSpeculateBranchElimination(final SpeculationProfile profile,
                                                   final ExecutionProfile executionProfile) {
    final double successRate = profile.getSuccessRateForType(SpeculationType.BRANCH_ELIMINATION);
    return successRate > config.getSpeculationThreshold() || successRate == 0.0;
  }

  private boolean shouldSpeculateInlining(final SpeculationProfile profile,
                                          final ExecutionProfile executionProfile) {
    final double successRate = profile.getSuccessRateForType(SpeculationType.FUNCTION_INLINING);
    return (successRate > config.getSpeculationThreshold() || successRate == 0.0) &&
           executionProfile.getFunctionCount() < 100; // Don't inline in very large modules
  }

  private SpeculativeOptimization createTypeSpecializationOptimization(final ExecutionProfile executionProfile) {
    final Map<String, Object> parameters = new HashMap<>();
    parameters.put("target_types", List.of("i32", "i64", "f32", "f64"));

    final List<String> assumptions = List.of(
        "Function parameters maintain observed types",
        "Return values maintain observed types"
    );

    return new SpeculativeOptimization(
        SpeculationType.TYPE_SPECIALIZATION,
        "Specialize function for observed parameter and return types",
        parameters,
        0.85,
        assumptions
    );
  }

  private SpeculativeOptimization createConstantFoldingOptimization(final ExecutionProfile executionProfile) {
    final Map<String, Object> parameters = new HashMap<>();
    parameters.put("fold_arithmetic", true);
    parameters.put("fold_comparisons", true);

    final List<String> assumptions = List.of(
        "Constant values remain constant across executions",
        "No side effects in constant expressions"
    );

    return new SpeculativeOptimization(
        SpeculationType.CONSTANT_FOLDING,
        "Fold constant expressions based on observed values",
        parameters,
        0.9,
        assumptions
    );
  }

  private SpeculativeOptimization createBranchEliminationOptimization(final ExecutionProfile executionProfile) {
    final Map<String, Object> parameters = new HashMap<>();
    parameters.put("eliminate_unlikely_branches", true);
    parameters.put("min_branch_probability", 0.95);

    final List<String> assumptions = List.of(
        "Branch patterns remain stable",
        "Unlikely branches continue to be unlikely"
    );

    return new SpeculativeOptimization(
        SpeculationType.BRANCH_ELIMINATION,
        "Eliminate branches with consistent patterns",
        parameters,
        0.8,
        assumptions
    );
  }

  private SpeculativeOptimization createInliningOptimization(final ExecutionProfile executionProfile) {
    final Map<String, Object> parameters = new HashMap<>();
    parameters.put("max_inline_size", 100);
    parameters.put("max_inline_depth", 3);

    final List<String> assumptions = List.of(
        "Hot functions benefit from inlining",
        "Code size increase is acceptable"
    );

    return new SpeculativeOptimization(
        SpeculationType.FUNCTION_INLINING,
        "Inline frequently called small functions",
        parameters,
        0.75,
        assumptions
    );
  }
}

/**
 * Manages deoptimization decisions and tracking.
 */
final class DeoptimizationManager {
  private final SpeculativeOptimizerConfig config;
  private final Map<String, Integer> activeSpeculations;

  public DeoptimizationManager(final SpeculativeOptimizerConfig config) {
    this.config = Objects.requireNonNull(config);
    this.activeSpeculations = new ConcurrentHashMap<>();
  }

  public DeoptimizationDecision shouldDeoptimize(final String moduleId,
                                                  final String functionName,
                                                  final RuntimeConditions currentConditions) {
    final String functionKey = moduleId + "::" + functionName;
    final Integer activeCount = activeSpeculations.get(functionKey);

    // Check if we have too many active speculations
    if (activeCount != null && activeCount > config.getMaxActiveSpeculations()) {
      return DeoptimizationDecision.deoptimize(DeoptimizationReason.SPECULATION_LIMIT_EXCEEDED,
          "Too many active speculations: " + activeCount);
    }

    // Check performance regression
    if (currentConditions.getCurrentPerformance() < 0.8) { // 20% performance loss
      return DeoptimizationDecision.deoptimize(DeoptimizationReason.PERFORMANCE_REGRESSION,
          "Performance regression detected: " + currentConditions.getCurrentPerformance());
    }

    return DeoptimizationDecision.noDeoptimization("Conditions stable");
  }

  public void recordDeoptimization(final String moduleId,
                                   final String functionName,
                                   final DeoptimizationReason reason,
                                   final Map<String, Object> context) {
    final String functionKey = moduleId + "::" + functionName;
    activeSpeculations.compute(functionKey, (key, count) -> Math.max(0, (count != null ? count : 0) - 1));
  }

  public int getActiveSpeculations() {
    return activeSpeculations.values().stream().mapToInt(Integer::intValue).sum();
  }

  public void reset() {
    activeSpeculations.clear();
  }
}

/**
 * Decision about whether to deoptimize.
 */
final class DeoptimizationDecision {
  private final boolean shouldDeoptimize;
  private final DeoptimizationReason reason;
  private final String description;

  private DeoptimizationDecision(final boolean shouldDeoptimize,
                                 final DeoptimizationReason reason,
                                 final String description) {
    this.shouldDeoptimize = shouldDeoptimize;
    this.reason = reason;
    this.description = Objects.requireNonNull(description);
  }

  public boolean shouldDeoptimize() { return shouldDeoptimize; }
  public DeoptimizationReason getReason() { return reason; }
  public String getDescription() { return description; }

  public static DeoptimizationDecision deoptimize(final DeoptimizationReason reason,
                                                   final String description) {
    return new DeoptimizationDecision(true, reason, description);
  }

  public static DeoptimizationDecision noDeoptimization(final String description) {
    return new DeoptimizationDecision(false, null, description);
  }
}

/**
 * Statistics about speculation activities.
 */
final class SpeculationStatistics {
  private final long totalSpeculations;
  private final long successfulSpeculations;
  private final long deoptimizations;
  private final int trackedFunctions;
  private final int activeSpeculations;

  public SpeculationStatistics(final long totalSpeculations,
                               final long successfulSpeculations,
                               final long deoptimizations,
                               final int trackedFunctions,
                               final int activeSpeculations) {
    this.totalSpeculations = totalSpeculations;
    this.successfulSpeculations = successfulSpeculations;
    this.deoptimizations = deoptimizations;
    this.trackedFunctions = trackedFunctions;
    this.activeSpeculations = activeSpeculations;
  }

  public long getTotalSpeculations() { return totalSpeculations; }
  public long getSuccessfulSpeculations() { return successfulSpeculations; }
  public long getDeoptimizations() { return deoptimizations; }
  public int getTrackedFunctions() { return trackedFunctions; }
  public int getActiveSpeculations() { return activeSpeculations; }

  public double getSuccessRate() {
    return totalSpeculations == 0 ? 0.0 : (double) successfulSpeculations / totalSpeculations;
  }

  public double getDeoptimizationRate() {
    return totalSpeculations == 0 ? 0.0 : (double) deoptimizations / totalSpeculations;
  }

  @Override
  public String toString() {
    return String.format("SpeculationStatistics{total=%d, successful=%d, success_rate=%.2f%%, " +
                         "deopt=%d, deopt_rate=%.2f%%, functions=%d, active=%d}",
        totalSpeculations, successfulSpeculations, getSuccessRate() * 100,
        deoptimizations, getDeoptimizationRate() * 100, trackedFunctions, activeSpeculations);
  }
}