package ai.tegmentum.wasmtime4j.dev;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * WebAssembly module optimizer with comprehensive optimization strategies and analysis. Provides
 * automated optimization recommendations and manual optimization controls.
 */
public final class ModuleOptimizer {

  private final Engine engine;
  private final OptimizationConfiguration configuration;
  private final long optimizerHandle;

  /**
   * Creates a module optimizer with the given engine.
   *
   * @param engine The engine to use for optimization
   * @throws IllegalArgumentException if engine is null
   */
  public ModuleOptimizer(final Engine engine) {
    this(engine, OptimizationConfiguration.getDefault());
  }

  /**
   * Creates a module optimizer with the given engine and configuration.
   *
   * @param engine The engine to use for optimization
   * @param configuration The optimization configuration
   * @throws IllegalArgumentException if engine or configuration is null
   */
  public ModuleOptimizer(final Engine engine, final OptimizationConfiguration configuration) {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }
    this.engine = engine;
    this.configuration = configuration;
    this.optimizerHandle = initializeOptimizer(engine, configuration);
  }

  /**
   * Analyzes a module and provides optimization recommendations.
   *
   * @param module The module to analyze
   * @return Optimization analysis result
   */
  public OptimizationAnalysis analyzeModule(final Module module) {
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }

    final ModuleInspector inspector = new ModuleInspector(module);
    final ModuleInspector.ModuleAnalysis moduleAnalysis = inspector.analyze();
    final List<ModuleInspector.OptimizationRecommendation> recommendations =
        inspector.getOptimizationRecommendations();

    final List<OptimizationOpportunity> opportunities =
        identifyOptimizationOpportunities(optimizerHandle, moduleAnalysis);
    final OptimizationMetrics metrics = calculateOptimizationMetrics(optimizerHandle, module);
    final List<OptimizationStrategy> strategies =
        generateOptimizationStrategies(opportunities, metrics, configuration);

    return new OptimizationAnalysis(
        moduleAnalysis, recommendations, opportunities, metrics, strategies);
  }

  /**
   * Optimizes a module using the specified strategy.
   *
   * @param module The module to optimize
   * @param strategy The optimization strategy to apply
   * @return Optimization result
   */
  public CompletableFuture<OptimizationResult> optimizeModule(
      final Module module, final OptimizationStrategy strategy) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long startTime = System.nanoTime();
            final byte[] originalBytes = getModuleBytes(module);
            final OptimizationMetrics originalMetrics =
                calculateOptimizationMetrics(optimizerHandle, module);

            final byte[] optimizedBytes =
                applyOptimizationStrategy(optimizerHandle, originalBytes, strategy);

            final Module optimizedModule = Module.fromBinary(engine, optimizedBytes);
            final OptimizationMetrics optimizedMetrics =
                calculateOptimizationMetrics(optimizerHandle, optimizedModule);

            final long optimizationTime = System.nanoTime() - startTime;
            final OptimizationImpact impact =
                calculateOptimizationImpact(originalMetrics, optimizedMetrics);

            return OptimizationResult.successful(
                module,
                optimizedModule,
                strategy,
                originalMetrics,
                optimizedMetrics,
                impact,
                optimizationTime);

          } catch (final Exception e) {
            return OptimizationResult.failed(
                module, strategy, "Optimization failed: " + e.getMessage());
          }
        });
  }

  /**
   * Applies automatic optimization using the best available strategy.
   *
   * @param module The module to optimize
   * @return Optimization result
   */
  public CompletableFuture<OptimizationResult> autoOptimize(final Module module) {
    return CompletableFuture.supplyAsync(
        () -> {
          final OptimizationAnalysis analysis = analyzeModule(module);
          final OptimizationStrategy bestStrategy = selectBestStrategy(analysis.getStrategies());

          if (bestStrategy == null) {
            return OptimizationResult.failed(
                module, null, "No suitable optimization strategy found");
          }

          return optimizeModule(module, bestStrategy).join();
        });
  }

  /**
   * Performs A/B testing between different optimization strategies.
   *
   * @param module The module to optimize
   * @param strategies The strategies to compare
   * @return A/B testing result
   */
  public CompletableFuture<ABTestingResult> performABTesting(
      final Module module, final List<OptimizationStrategy> strategies) {
    return CompletableFuture.supplyAsync(
        () -> {
          final List<OptimizationResult> results = new ArrayList<>();
          final OptimizationMetrics baseline =
              calculateOptimizationMetrics(optimizerHandle, module);

          for (final OptimizationStrategy strategy : strategies) {
            try {
              final OptimizationResult result = optimizeModule(module, strategy).join();
              results.add(result);
            } catch (final Exception e) {
              results.add(OptimizationResult.failed(module, strategy, e.getMessage()));
            }
          }

          final OptimizationResult winner = selectBestResult(results);
          final Map<String, Double> performanceComparison = comparePerformance(results, baseline);

          return new ABTestingResult(
              module, strategies, results, winner, baseline, performanceComparison);
        });
  }

  /**
   * Validates optimization results for correctness.
   *
   * @param original The original module
   * @param optimized The optimized module
   * @return Validation result
   */
  public OptimizationValidation validateOptimization(
      final Module original, final Module optimized) {
    final List<ValidationIssue> issues = new ArrayList<>();

    try {
      // Validate module structure
      final ModuleInspector originalInspector = new ModuleInspector(original);
      final ModuleInspector optimizedInspector = new ModuleInspector(optimized);

      final ModuleInspector.ModuleAnalysis originalAnalysis = originalInspector.analyze();
      final ModuleInspector.ModuleAnalysis optimizedAnalysis = optimizedInspector.analyze();

      // Check function signatures
      if (originalAnalysis.getFunctions().size() != optimizedAnalysis.getFunctions().size()) {
        issues.add(
            new ValidationIssue(
                ValidationSeverity.ERROR,
                "Function count mismatch",
                "Original: "
                    + originalAnalysis.getFunctions().size()
                    + ", Optimized: "
                    + optimizedAnalysis.getFunctions().size()));
      }

      // Check exports
      if (originalAnalysis.getExports().size() != optimizedAnalysis.getExports().size()) {
        issues.add(
            new ValidationIssue(
                ValidationSeverity.WARNING,
                "Export count mismatch",
                "Some exports may have been removed during optimization"));
      }

      // Validate semantic equivalence
      final boolean semanticallyEquivalent =
          validateSemanticEquivalence(optimizerHandle, original, optimized);

      if (!semanticallyEquivalent) {
        issues.add(
            new ValidationIssue(
                ValidationSeverity.ERROR,
                "Semantic equivalence validation failed",
                "Optimized module may not behave identically to original"));
      }

      return new OptimizationValidation(issues, semanticallyEquivalent);

    } catch (final Exception e) {
      issues.add(
          new ValidationIssue(ValidationSeverity.ERROR, "Validation failed", e.getMessage()));
      return new OptimizationValidation(issues, false);
    }
  }

  /**
   * Generates a comprehensive optimization report.
   *
   * @param module The module to analyze
   * @return Optimization report
   */
  public OptimizationReport generateReport(final Module module) {
    final OptimizationAnalysis analysis = analyzeModule(module);
    final List<OptimizationBenchmark> benchmarks = performOptimizationBenchmarks(module);
    final OptimizationRecommendations recommendations = generateDetailedRecommendations(analysis);

    return new OptimizationReport(
        module, analysis, benchmarks, recommendations, System.currentTimeMillis());
  }

  private OptimizationStrategy selectBestStrategy(final List<OptimizationStrategy> strategies) {
    return strategies.stream()
        .filter(
            strategy ->
                strategy.getExpectedImpact().ordinal() >= OptimizationImpactLevel.MEDIUM.ordinal())
        .max((s1, s2) -> s1.getExpectedImpact().compareTo(s2.getExpectedImpact()))
        .orElse(strategies.isEmpty() ? null : strategies.get(0));
  }

  private OptimizationResult selectBestResult(final List<OptimizationResult> results) {
    return results.stream()
        .filter(OptimizationResult::isSuccessful)
        .max(
            (r1, r2) ->
                Double.compare(
                    r1.getImpact().getOverallImprovement(), r2.getImpact().getOverallImprovement()))
        .orElse(null);
  }

  private Map<String, Double> comparePerformance(
      final List<OptimizationResult> results, final OptimizationMetrics baseline) {
    final Map<String, Double> comparison = new HashMap<>();

    for (final OptimizationResult result : results) {
      if (result.isSuccessful()) {
        final double improvement = result.getImpact().getOverallImprovement();
        comparison.put(result.getStrategy().getName(), improvement);
      }
    }

    return comparison;
  }

  private List<OptimizationBenchmark> performOptimizationBenchmarks(final Module module) {
    final List<OptimizationBenchmark> benchmarks = new ArrayList<>();

    // Benchmark different optimization levels
    for (final OptimizationLevel level : OptimizationLevel.values()) {
      try {
        final long startTime = System.nanoTime();
        final EngineConfig config = EngineConfig.newConfig().withOptimizationLevel(level);
        final Engine testEngine = Engine.newEngine(config);
        final Module optimizedModule = Module.fromBinary(testEngine, getModuleBytes(module));
        final long optimizationTime = System.nanoTime() - startTime;

        final OptimizationMetrics metrics =
            calculateOptimizationMetrics(optimizerHandle, optimizedModule);

        benchmarks.add(
            new OptimizationBenchmark(
                "OptimizationLevel_" + level.name(), optimizationTime, metrics));

      } catch (final Exception e) {
        // Skip failed benchmarks
      }
    }

    return benchmarks;
  }

  private OptimizationRecommendations generateDetailedRecommendations(
      final OptimizationAnalysis analysis) {
    final List<DetailedRecommendation> recommendations = new ArrayList<>();

    for (final OptimizationOpportunity opportunity : analysis.getOpportunities()) {
      final DetailedRecommendation recommendation =
          new DetailedRecommendation(
              opportunity.getType(),
              opportunity.getDescription(),
              opportunity.getExpectedBenefit(),
              opportunity.getImplementationComplexity(),
              generateImplementationSteps(opportunity));
      recommendations.add(recommendation);
    }

    return new OptimizationRecommendations(recommendations);
  }

  private List<String> generateImplementationSteps(final OptimizationOpportunity opportunity) {
    final List<String> steps = new ArrayList<>();

    switch (opportunity.getType()) {
      case DEAD_CODE_ELIMINATION:
        steps.add("Identify unused functions and variables");
        steps.add("Remove unreachable code paths");
        steps.add("Update function index mappings");
        break;

      case FUNCTION_INLINING:
        steps.add("Identify small, frequently called functions");
        steps.add("Inline function bodies at call sites");
        steps.add("Remove original function definitions");
        break;

      case LOOP_OPTIMIZATION:
        steps.add("Analyze loop structures and iteration patterns");
        steps.add("Apply loop unrolling where beneficial");
        steps.add("Optimize memory access patterns within loops");
        break;

      case CONSTANT_FOLDING:
        steps.add("Identify compile-time constant expressions");
        steps.add("Evaluate constants at compile time");
        steps.add("Replace expressions with computed values");
        break;

      default:
        steps.add("Apply optimization strategy");
        break;
    }

    return steps;
  }

  private native long initializeOptimizer(Engine engine, OptimizationConfiguration config);

  private native List<OptimizationOpportunity> identifyOptimizationOpportunities(
      long handle, ModuleInspector.ModuleAnalysis analysis);

  private native OptimizationMetrics calculateOptimizationMetrics(long handle, Module module);

  private native List<OptimizationStrategy> generateOptimizationStrategies(
      List<OptimizationOpportunity> opportunities,
      OptimizationMetrics metrics,
      OptimizationConfiguration config);

  private native byte[] getModuleBytes(Module module);

  private native byte[] applyOptimizationStrategy(
      long handle, byte[] moduleBytes, OptimizationStrategy strategy);

  private native OptimizationImpact calculateOptimizationImpact(
      OptimizationMetrics original, OptimizationMetrics optimized);

  private native boolean validateSemanticEquivalence(
      long handle, Module original, Module optimized);

  /** Optimization configuration. */
  public static final class OptimizationConfiguration {
    private final boolean enableDeadCodeElimination;
    private final boolean enableFunctionInlining;
    private final boolean enableLoopOptimization;
    private final boolean enableConstantFolding;
    private final OptimizationLevel optimizationLevel;
    private final int maxOptimizationPasses;

    public OptimizationConfiguration(
        final boolean enableDeadCodeElimination,
        final boolean enableFunctionInlining,
        final boolean enableLoopOptimization,
        final boolean enableConstantFolding,
        final OptimizationLevel optimizationLevel,
        final int maxOptimizationPasses) {
      this.enableDeadCodeElimination = enableDeadCodeElimination;
      this.enableFunctionInlining = enableFunctionInlining;
      this.enableLoopOptimization = enableLoopOptimization;
      this.enableConstantFolding = enableConstantFolding;
      this.optimizationLevel = optimizationLevel;
      this.maxOptimizationPasses = maxOptimizationPasses;
    }

    public static OptimizationConfiguration getDefault() {
      return new OptimizationConfiguration(true, true, true, true, OptimizationLevel.SPEED, 3);
    }

    public boolean isEnableDeadCodeElimination() {
      return enableDeadCodeElimination;
    }

    public boolean isEnableFunctionInlining() {
      return enableFunctionInlining;
    }

    public boolean isEnableLoopOptimization() {
      return enableLoopOptimization;
    }

    public boolean isEnableConstantFolding() {
      return enableConstantFolding;
    }

    public OptimizationLevel getOptimizationLevel() {
      return optimizationLevel;
    }

    public int getMaxOptimizationPasses() {
      return maxOptimizationPasses;
    }
  }

  /** Optimization analysis result. */
  public static final class OptimizationAnalysis {
    private final ModuleInspector.ModuleAnalysis moduleAnalysis;
    private final List<ModuleInspector.OptimizationRecommendation> recommendations;
    private final List<OptimizationOpportunity> opportunities;
    private final OptimizationMetrics metrics;
    private final List<OptimizationStrategy> strategies;

    public OptimizationAnalysis(
        final ModuleInspector.ModuleAnalysis moduleAnalysis,
        final List<ModuleInspector.OptimizationRecommendation> recommendations,
        final List<OptimizationOpportunity> opportunities,
        final OptimizationMetrics metrics,
        final List<OptimizationStrategy> strategies) {
      this.moduleAnalysis = moduleAnalysis;
      this.recommendations = Collections.unmodifiableList(new ArrayList<>(recommendations));
      this.opportunities = Collections.unmodifiableList(new ArrayList<>(opportunities));
      this.metrics = metrics;
      this.strategies = Collections.unmodifiableList(new ArrayList<>(strategies));
    }

    public ModuleInspector.ModuleAnalysis getModuleAnalysis() {
      return moduleAnalysis;
    }

    public List<ModuleInspector.OptimizationRecommendation> getRecommendations() {
      return recommendations;
    }

    public List<OptimizationOpportunity> getOpportunities() {
      return opportunities;
    }

    public OptimizationMetrics getMetrics() {
      return metrics;
    }

    public List<OptimizationStrategy> getStrategies() {
      return strategies;
    }
  }

  /** Optimization opportunity. */
  public static final class OptimizationOpportunity {
    private final OptimizationType type;
    private final String description;
    private final OptimizationImpactLevel expectedBenefit;
    private final ImplementationComplexity implementationComplexity;
    private final Map<String, Object> parameters;

    public OptimizationOpportunity(
        final OptimizationType type,
        final String description,
        final OptimizationImpactLevel expectedBenefit,
        final ImplementationComplexity implementationComplexity,
        final Map<String, Object> parameters) {
      this.type = type;
      this.description = description;
      this.expectedBenefit = expectedBenefit;
      this.implementationComplexity = implementationComplexity;
      this.parameters = Collections.unmodifiableMap(new HashMap<>(parameters));
    }

    public OptimizationType getType() {
      return type;
    }

    public String getDescription() {
      return description;
    }

    public OptimizationImpactLevel getExpectedBenefit() {
      return expectedBenefit;
    }

    public ImplementationComplexity getImplementationComplexity() {
      return implementationComplexity;
    }

    public Map<String, Object> getParameters() {
      return parameters;
    }
  }

  /** Optimization strategy. */
  public static final class OptimizationStrategy {
    private final String name;
    private final String description;
    private final List<OptimizationType> optimizations;
    private final OptimizationImpactLevel expectedImpact;
    private final Map<String, Object> parameters;

    public OptimizationStrategy(
        final String name,
        final String description,
        final List<OptimizationType> optimizations,
        final OptimizationImpactLevel expectedImpact,
        final Map<String, Object> parameters) {
      this.name = name;
      this.description = description;
      this.optimizations = Collections.unmodifiableList(new ArrayList<>(optimizations));
      this.expectedImpact = expectedImpact;
      this.parameters = Collections.unmodifiableMap(new HashMap<>(parameters));
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public List<OptimizationType> getOptimizations() {
      return optimizations;
    }

    public OptimizationImpactLevel getExpectedImpact() {
      return expectedImpact;
    }

    public Map<String, Object> getParameters() {
      return parameters;
    }
  }

  /** Optimization metrics. */
  public static final class OptimizationMetrics {
    private final long moduleSize;
    private final int functionCount;
    private final int instructionCount;
    private final long memoryUsage;
    private final double complexityScore;
    private final double performanceScore;

    public OptimizationMetrics(
        final long moduleSize,
        final int functionCount,
        final int instructionCount,
        final long memoryUsage,
        final double complexityScore,
        final double performanceScore) {
      this.moduleSize = moduleSize;
      this.functionCount = functionCount;
      this.instructionCount = instructionCount;
      this.memoryUsage = memoryUsage;
      this.complexityScore = complexityScore;
      this.performanceScore = performanceScore;
    }

    public long getModuleSize() {
      return moduleSize;
    }

    public int getFunctionCount() {
      return functionCount;
    }

    public int getInstructionCount() {
      return instructionCount;
    }

    public long getMemoryUsage() {
      return memoryUsage;
    }

    public double getComplexityScore() {
      return complexityScore;
    }

    public double getPerformanceScore() {
      return performanceScore;
    }
  }

  /** Optimization impact analysis. */
  public static final class OptimizationImpact {
    private final double sizeReduction;
    private final double performanceImprovement;
    private final double memoryReduction;
    private final double complexityReduction;
    private final double overallImprovement;

    public OptimizationImpact(
        final double sizeReduction,
        final double performanceImprovement,
        final double memoryReduction,
        final double complexityReduction,
        final double overallImprovement) {
      this.sizeReduction = sizeReduction;
      this.performanceImprovement = performanceImprovement;
      this.memoryReduction = memoryReduction;
      this.complexityReduction = complexityReduction;
      this.overallImprovement = overallImprovement;
    }

    public double getSizeReduction() {
      return sizeReduction;
    }

    public double getPerformanceImprovement() {
      return performanceImprovement;
    }

    public double getMemoryReduction() {
      return memoryReduction;
    }

    public double getComplexityReduction() {
      return complexityReduction;
    }

    public double getOverallImprovement() {
      return overallImprovement;
    }
  }

  /** Optimization result. */
  public static final class OptimizationResult {
    private final Module originalModule;
    private final Module optimizedModule;
    private final OptimizationStrategy strategy;
    private final OptimizationMetrics originalMetrics;
    private final OptimizationMetrics optimizedMetrics;
    private final OptimizationImpact impact;
    private final long optimizationTimeNs;
    private final boolean successful;
    private final String error;

    private OptimizationResult(
        final Module originalModule,
        final Module optimizedModule,
        final OptimizationStrategy strategy,
        final OptimizationMetrics originalMetrics,
        final OptimizationMetrics optimizedMetrics,
        final OptimizationImpact impact,
        final long optimizationTimeNs,
        final boolean successful,
        final String error) {
      this.originalModule = originalModule;
      this.optimizedModule = optimizedModule;
      this.strategy = strategy;
      this.originalMetrics = originalMetrics;
      this.optimizedMetrics = optimizedMetrics;
      this.impact = impact;
      this.optimizationTimeNs = optimizationTimeNs;
      this.successful = successful;
      this.error = error;
    }

    public static OptimizationResult successful(
        final Module originalModule,
        final Module optimizedModule,
        final OptimizationStrategy strategy,
        final OptimizationMetrics originalMetrics,
        final OptimizationMetrics optimizedMetrics,
        final OptimizationImpact impact,
        final long optimizationTimeNs) {
      return new OptimizationResult(
          originalModule,
          optimizedModule,
          strategy,
          originalMetrics,
          optimizedMetrics,
          impact,
          optimizationTimeNs,
          true,
          null);
    }

    public static OptimizationResult failed(
        final Module originalModule, final OptimizationStrategy strategy, final String error) {
      return new OptimizationResult(
          originalModule, null, strategy, null, null, null, 0, false, error);
    }

    public Module getOriginalModule() {
      return originalModule;
    }

    public Module getOptimizedModule() {
      return optimizedModule;
    }

    public OptimizationStrategy getStrategy() {
      return strategy;
    }

    public OptimizationMetrics getOriginalMetrics() {
      return originalMetrics;
    }

    public OptimizationMetrics getOptimizedMetrics() {
      return optimizedMetrics;
    }

    public OptimizationImpact getImpact() {
      return impact;
    }

    public long getOptimizationTimeNs() {
      return optimizationTimeNs;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getError() {
      return error;
    }
  }

  /** A/B testing result. */
  public static final class ABTestingResult {
    private final Module originalModule;
    private final List<OptimizationStrategy> strategies;
    private final List<OptimizationResult> results;
    private final OptimizationResult winner;
    private final OptimizationMetrics baseline;
    private final Map<String, Double> performanceComparison;

    public ABTestingResult(
        final Module originalModule,
        final List<OptimizationStrategy> strategies,
        final List<OptimizationResult> results,
        final OptimizationResult winner,
        final OptimizationMetrics baseline,
        final Map<String, Double> performanceComparison) {
      this.originalModule = originalModule;
      this.strategies = Collections.unmodifiableList(new ArrayList<>(strategies));
      this.results = Collections.unmodifiableList(new ArrayList<>(results));
      this.winner = winner;
      this.baseline = baseline;
      this.performanceComparison =
          Collections.unmodifiableMap(new HashMap<>(performanceComparison));
    }

    public Module getOriginalModule() {
      return originalModule;
    }

    public List<OptimizationStrategy> getStrategies() {
      return strategies;
    }

    public List<OptimizationResult> getResults() {
      return results;
    }

    public OptimizationResult getWinner() {
      return winner;
    }

    public OptimizationMetrics getBaseline() {
      return baseline;
    }

    public Map<String, Double> getPerformanceComparison() {
      return performanceComparison;
    }
  }

  /** Optimization validation. */
  public static final class OptimizationValidation {
    private final List<ValidationIssue> issues;
    private final boolean semanticallyEquivalent;

    public OptimizationValidation(
        final List<ValidationIssue> issues, final boolean semanticallyEquivalent) {
      this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
      this.semanticallyEquivalent = semanticallyEquivalent;
    }

    public List<ValidationIssue> getIssues() {
      return issues;
    }

    public boolean isSemanticallyEquivalent() {
      return semanticallyEquivalent;
    }

    public boolean hasErrors() {
      return issues.stream().anyMatch(issue -> issue.getSeverity() == ValidationSeverity.ERROR);
    }
  }

  /** Validation issue. */
  public static final class ValidationIssue {
    private final ValidationSeverity severity;
    private final String message;
    private final String description;

    public ValidationIssue(
        final ValidationSeverity severity, final String message, final String description) {
      this.severity = severity;
      this.message = message;
      this.description = description;
    }

    public ValidationSeverity getSeverity() {
      return severity;
    }

    public String getMessage() {
      return message;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Optimization benchmark. */
  public static final class OptimizationBenchmark {
    private final String name;
    private final long optimizationTimeNs;
    private final OptimizationMetrics metrics;

    public OptimizationBenchmark(
        final String name, final long optimizationTimeNs, final OptimizationMetrics metrics) {
      this.name = name;
      this.optimizationTimeNs = optimizationTimeNs;
      this.metrics = metrics;
    }

    public String getName() {
      return name;
    }

    public long getOptimizationTimeNs() {
      return optimizationTimeNs;
    }

    public OptimizationMetrics getMetrics() {
      return metrics;
    }
  }

  /** Detailed optimization recommendations. */
  public static final class OptimizationRecommendations {
    private final List<DetailedRecommendation> recommendations;

    public OptimizationRecommendations(final List<DetailedRecommendation> recommendations) {
      this.recommendations = Collections.unmodifiableList(new ArrayList<>(recommendations));
    }

    public List<DetailedRecommendation> getRecommendations() {
      return recommendations;
    }
  }

  /** Detailed recommendation. */
  public static final class DetailedRecommendation {
    private final OptimizationType type;
    private final String description;
    private final OptimizationImpactLevel expectedBenefit;
    private final ImplementationComplexity complexity;
    private final List<String> implementationSteps;

    public DetailedRecommendation(
        final OptimizationType type,
        final String description,
        final OptimizationImpactLevel expectedBenefit,
        final ImplementationComplexity complexity,
        final List<String> implementationSteps) {
      this.type = type;
      this.description = description;
      this.expectedBenefit = expectedBenefit;
      this.complexity = complexity;
      this.implementationSteps = Collections.unmodifiableList(new ArrayList<>(implementationSteps));
    }

    public OptimizationType getType() {
      return type;
    }

    public String getDescription() {
      return description;
    }

    public OptimizationImpactLevel getExpectedBenefit() {
      return expectedBenefit;
    }

    public ImplementationComplexity getComplexity() {
      return complexity;
    }

    public List<String> getImplementationSteps() {
      return implementationSteps;
    }
  }

  /** Optimization report. */
  public static final class OptimizationReport {
    private final Module module;
    private final OptimizationAnalysis analysis;
    private final List<OptimizationBenchmark> benchmarks;
    private final OptimizationRecommendations recommendations;
    private final long timestamp;

    public OptimizationReport(
        final Module module,
        final OptimizationAnalysis analysis,
        final List<OptimizationBenchmark> benchmarks,
        final OptimizationRecommendations recommendations,
        final long timestamp) {
      this.module = module;
      this.analysis = analysis;
      this.benchmarks = Collections.unmodifiableList(new ArrayList<>(benchmarks));
      this.recommendations = recommendations;
      this.timestamp = timestamp;
    }

    public Module getModule() {
      return module;
    }

    public OptimizationAnalysis getAnalysis() {
      return analysis;
    }

    public List<OptimizationBenchmark> getBenchmarks() {
      return benchmarks;
    }

    public OptimizationRecommendations getRecommendations() {
      return recommendations;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }

  public enum OptimizationType {
    DEAD_CODE_ELIMINATION,
    FUNCTION_INLINING,
    LOOP_OPTIMIZATION,
    CONSTANT_FOLDING,
    INSTRUCTION_COMBINING,
    MEMORY_OPTIMIZATION,
    CONTROL_FLOW_OPTIMIZATION
  }

  public enum OptimizationImpactLevel {
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
  }

  public enum ImplementationComplexity {
    SIMPLE,
    MODERATE,
    COMPLEX,
    VERY_COMPLEX
  }

  public enum ValidationSeverity {
    INFO,
    WARNING,
    ERROR
  }
}
