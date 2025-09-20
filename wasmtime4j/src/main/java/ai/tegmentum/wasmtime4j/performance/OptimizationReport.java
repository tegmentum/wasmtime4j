package ai.tegmentum.wasmtime4j.performance;

import ai.tegmentum.wasmtime4j.Module;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive report of performance optimization analysis.
 *
 * <p>OptimizationReport provides detailed analysis of a WebAssembly module's performance
 * characteristics, identifying bottlenecks, optimization opportunities, and providing
 * actionable recommendations for performance improvements.
 *
 * @since 1.0.0
 */
public interface OptimizationReport {

  /**
   * Gets the module that was analyzed.
   *
   * @return analyzed WebAssembly module
   */
  Module getModule();

  /**
   * Gets the timestamp when the analysis was performed.
   *
   * @return analysis timestamp
   */
  Instant getAnalysisTime();

  /**
   * Gets the duration of the optimization analysis.
   *
   * @return analysis duration
   */
  Duration getAnalysisDuration();

  /**
   * Gets the overall performance score of the module.
   *
   * <p>Performance score is a normalized value from 0.0 (worst) to 1.0 (best)
   * indicating the overall optimization level of the module.
   *
   * @return performance score between 0.0 and 1.0
   */
  double getPerformanceScore();

  /**
   * Gets identified performance bottlenecks in the module.
   *
   * @return list of performance bottlenecks ordered by severity
   */
  List<PerformanceBottleneck> getBottlenecks();

  /**
   * Gets optimization opportunities identified in the module.
   *
   * @return list of optimization opportunities ordered by potential impact
   */
  List<OptimizationOpportunity> getOptimizationOpportunities();

  /**
   * Gets specific optimization recommendations.
   *
   * @return list of actionable optimization recommendations
   */
  List<OptimizationRecommendation> getRecommendations();

  /**
   * Gets function-level performance analysis.
   *
   * @return map of function names to their performance analysis
   */
  Map<String, FunctionPerformanceAnalysis> getFunctionAnalysis();

  /**
   * Gets memory usage analysis for the module.
   *
   * @return memory usage optimization analysis
   */
  MemoryOptimizationAnalysis getMemoryAnalysis();

  /**
   * Gets compilation optimization analysis.
   *
   * @return compilation and JIT optimization insights
   */
  CompilationOptimizationAnalysis getCompilationAnalysis();

  /**
   * Gets instruction-level optimization analysis.
   *
   * @return instruction optimization insights
   */
  InstructionOptimizationAnalysis getInstructionAnalysis();

  /**
   * Gets the most critical performance issues.
   *
   * @param count maximum number of issues to return
   * @return most critical performance issues ordered by impact
   */
  List<PerformanceIssue> getCriticalIssues(final int count);

  /**
   * Gets potential performance improvements and their estimated impact.
   *
   * @return map of optimization strategies to estimated performance gains
   */
  Map<OptimizationStrategy, PerformanceImpact> getImpactEstimates();

  /**
   * Gets complexity analysis of the module.
   *
   * @return module complexity metrics
   */
  ComplexityAnalysis getComplexityAnalysis();

  /**
   * Gets resource usage projections under different optimization levels.
   *
   * @return resource usage projections
   */
  Map<OptimizationLevel, ResourceUsageProjection> getResourceProjections();

  /**
   * Gets compatibility information for different optimization strategies.
   *
   * @return optimization compatibility matrix
   */
  OptimizationCompatibility getCompatibilityInfo();

  /**
   * Gets benchmarking data if available from previous optimizations.
   *
   * @return historical benchmark data, or null if not available
   */
  HistoricalBenchmarkData getHistoricalData();

  /**
   * Gets the confidence level of the analysis results.
   *
   * @return confidence level from 0.0 (low confidence) to 1.0 (high confidence)
   */
  double getConfidenceLevel();

  /**
   * Gets analysis limitations and caveats.
   *
   * @return list of analysis limitations
   */
  List<AnalysisLimitation> getLimitations();

  /**
   * Gets a human-readable summary of the optimization analysis.
   *
   * @return textual summary of key findings and recommendations
   */
  String getSummary();

  /**
   * Exports the optimization report in the specified format.
   *
   * @param format export format specification
   * @return exported report data
   * @throws IllegalArgumentException if format is not supported
   */
  String exportReport(final ReportFormat format);

  /**
   * Validates the optimization analysis results.
   *
   * @return validation results for the analysis
   */
  AnalysisValidationResult validateAnalysis();
}