package ai.tegmentum.wasmtime4j;

/**
 * Component optimization result interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentOptimizationResult {

  /**
   * Gets the component ID.
   *
   * @return component ID
   */
  String getComponentId();

  /**
   * Gets optimization status.
   *
   * @return optimization status
   */
  OptimizationStatus getStatus();

  /**
   * Gets optimization start time.
   *
   * @return start timestamp
   */
  long getStartTime();

  /**
   * Gets optimization end time.
   *
   * @return end timestamp
   */
  long getEndTime();

  /**
   * Gets optimization duration.
   *
   * @return duration in milliseconds
   */
  long getDuration();

  /**
   * Gets performance improvements.
   *
   * @return performance improvements
   */
  PerformanceImprovement getPerformanceImprovement();

  /**
   * Gets memory optimizations.
   *
   * @return memory optimizations
   */
  MemoryOptimizationResult getMemoryOptimization();

  /**
   * Gets compilation optimizations.
   *
   * @return compilation optimizations
   */
  CompilationOptimizationResult getCompilationOptimization();

  /**
   * Gets runtime optimizations.
   *
   * @return runtime optimizations
   */
  RuntimeOptimizationResult getRuntimeOptimization();

  /**
   * Gets optimization errors.
   *
   * @return list of errors
   */
  java.util.List<OptimizationError> getErrors();

  /**
   * Gets optimization warnings.
   *
   * @return list of warnings
   */
  java.util.List<OptimizationWarning> getWarnings();

  /**
   * Gets optimization metrics.
   *
   * @return optimization metrics
   */
  OptimizationMetrics getMetrics();

  /**
   * Gets optimization summary.
   *
   * @return optimization summary
   */
  OptimizationSummary getSummary();

  /** Performance improvement interface. */
  interface PerformanceImprovement {
    /**
     * Gets execution time improvement.
     *
     * @return improvement percentage
     */
    double getExecutionTimeImprovement();

    /**
     * Gets throughput improvement.
     *
     * @return improvement percentage
     */
    double getThroughputImprovement();

    /**
     * Gets latency reduction.
     *
     * @return reduction percentage
     */
    double getLatencyReduction();

    /**
     * Gets CPU utilization improvement.
     *
     * @return improvement percentage
     */
    double getCpuUtilizationImprovement();

    /**
     * Gets energy efficiency improvement.
     *
     * @return improvement percentage
     */
    double getEnergyEfficiencyImprovement();

    /**
     * Gets overall performance score.
     *
     * @return performance score (0.0-1.0)
     */
    double getOverallScore();
  }

  /** Memory optimization result interface. */
  interface MemoryOptimizationResult {
    /**
     * Gets memory usage reduction.
     *
     * @return reduction percentage
     */
    double getMemoryReduction();

    /**
     * Gets allocation count reduction.
     *
     * @return reduction percentage
     */
    double getAllocationReduction();

    /**
     * Gets GC frequency improvement.
     *
     * @return improvement percentage
     */
    double getGcFrequencyImprovement();

    /**
     * Gets memory fragmentation reduction.
     *
     * @return reduction percentage
     */
    double getFragmentationReduction();

    /**
     * Gets memory pool efficiency.
     *
     * @return efficiency percentage
     */
    double getPoolEfficiency();

    /**
     * Gets compression savings.
     *
     * @return compression statistics
     */
    CompressionSavings getCompressionSavings();
  }

  /** Compilation optimization result interface. */
  interface CompilationOptimizationResult {
    /**
     * Gets compilation time reduction.
     *
     * @return reduction percentage
     */
    double getCompilationTimeReduction();

    /**
     * Gets code size reduction.
     *
     * @return reduction percentage
     */
    double getCodeSizeReduction();

    /**
     * Gets dead code eliminated.
     *
     * @return eliminated code percentage
     */
    double getDeadCodeEliminated();

    /**
     * Gets function inlining statistics.
     *
     * @return inlining statistics
     */
    InliningStatistics getInliningStatistics();

    /**
     * Gets loop optimization results.
     *
     * @return loop optimization results
     */
    LoopOptimizationResults getLoopOptimization();

    /**
     * Gets vectorization results.
     *
     * @return vectorization results
     */
    VectorizationResults getVectorization();
  }

  /** Runtime optimization result interface. */
  interface RuntimeOptimizationResult {
    /**
     * Gets JIT compilation effectiveness.
     *
     * @return effectiveness percentage
     */
    double getJitEffectiveness();

    /**
     * Gets profile-guided optimization benefit.
     *
     * @return benefit percentage
     */
    double getPgoBenefit();

    /**
     * Gets adaptive optimization results.
     *
     * @return adaptive results
     */
    AdaptiveOptimizationResults getAdaptiveResults();

    /**
     * Gets cache optimization results.
     *
     * @return cache results
     */
    CacheOptimizationResults getCacheResults();

    /**
     * Gets prefetch effectiveness.
     *
     * @return prefetch effectiveness
     */
    PrefetchEffectiveness getPrefetchEffectiveness();
  }

  /** Compression savings interface. */
  interface CompressionSavings {
    /**
     * Gets compression ratio.
     *
     * @return compression ratio
     */
    double getCompressionRatio();

    /**
     * Gets space saved.
     *
     * @return bytes saved
     */
    long getSpaceSaved();

    /**
     * Gets compression overhead.
     *
     * @return overhead in milliseconds
     */
    long getOverhead();
  }

  /** Inlining statistics interface. */
  interface InliningStatistics {
    /**
     * Gets functions inlined.
     *
     * @return inlined function count
     */
    int getFunctionsInlined();

    /**
     * Gets inlining success rate.
     *
     * @return success rate percentage
     */
    double getSuccessRate();

    /**
     * Gets code size impact.
     *
     * @return size impact percentage
     */
    double getCodeSizeImpact();
  }

  /** Loop optimization results interface. */
  interface LoopOptimizationResults {
    /**
     * Gets loops optimized.
     *
     * @return optimized loop count
     */
    int getLoopsOptimized();

    /**
     * Gets unrolling benefit.
     *
     * @return benefit percentage
     */
    double getUnrollingBenefit();

    /**
     * Gets vectorization opportunities.
     *
     * @return opportunity count
     */
    int getVectorizationOpportunities();
  }

  /** Vectorization results interface. */
  interface VectorizationResults {
    /**
     * Gets vectorized operations.
     *
     * @return vectorized operation count
     */
    int getVectorizedOperations();

    /**
     * Gets speedup achieved.
     *
     * @return speedup factor
     */
    double getSpeedup();

    /**
     * Gets SIMD utilization.
     *
     * @return utilization percentage
     */
    double getSimdUtilization();
  }

  /** Adaptive optimization results interface. */
  interface AdaptiveOptimizationResults {
    /**
     * Gets adaptations made.
     *
     * @return adaptation count
     */
    int getAdaptationsMade();

    /**
     * Gets adaptation accuracy.
     *
     * @return accuracy percentage
     */
    double getAccuracy();

    /**
     * Gets learning effectiveness.
     *
     * @return effectiveness percentage
     */
    double getLearningEffectiveness();
  }

  /** Cache optimization results interface. */
  interface CacheOptimizationResults {
    /**
     * Gets cache hit rate improvement.
     *
     * @return improvement percentage
     */
    double getHitRateImprovement();

    /**
     * Gets cache efficiency.
     *
     * @return efficiency percentage
     */
    double getEfficiency();

    /**
     * Gets eviction rate reduction.
     *
     * @return reduction percentage
     */
    double getEvictionReduction();
  }

  /** Prefetch effectiveness interface. */
  interface PrefetchEffectiveness {
    /**
     * Gets prefetch accuracy.
     *
     * @return accuracy percentage
     */
    double getAccuracy();

    /**
     * Gets cache miss reduction.
     *
     * @return reduction percentage
     */
    double getMissReduction();

    /**
     * Gets bandwidth utilization.
     *
     * @return utilization percentage
     */
    double getBandwidthUtilization();
  }

  /** Optimization error interface. */
  interface OptimizationError {
    /**
     * Gets error code.
     *
     * @return error code
     */
    String getCode();

    /**
     * Gets error message.
     *
     * @return error message
     */
    String getMessage();

    /**
     * Gets error severity.
     *
     * @return error severity
     */
    ErrorSeverity getSeverity();

    /**
     * Gets error context.
     *
     * @return error context
     */
    String getContext();
  }

  /** Optimization warning interface. */
  interface OptimizationWarning {
    /**
     * Gets warning code.
     *
     * @return warning code
     */
    String getCode();

    /**
     * Gets warning message.
     *
     * @return warning message
     */
    String getMessage();

    /**
     * Gets warning category.
     *
     * @return warning category
     */
    WarningCategory getCategory();
  }

  /** Optimization metrics interface. */
  interface OptimizationMetrics {
    /**
     * Gets metrics collected during optimization.
     *
     * @return metrics map
     */
    java.util.Map<String, Object> getMetrics();

    /**
     * Gets performance counters.
     *
     * @return performance counters
     */
    java.util.Map<String, Long> getPerformanceCounters();

    /**
     * Gets resource usage.
     *
     * @return resource usage statistics
     */
    ResourceUsageStatistics getResourceUsage();
  }

  /** Resource usage statistics interface. */
  interface ResourceUsageStatistics {
    /**
     * Gets CPU usage.
     *
     * @return CPU usage percentage
     */
    double getCpuUsage();

    /**
     * Gets memory usage.
     *
     * @return memory usage in bytes
     */
    long getMemoryUsage();

    /**
     * Gets I/O operations.
     *
     * @return I/O operation count
     */
    long getIoOperations();
  }

  /** Optimization summary interface. */
  interface OptimizationSummary {
    /**
     * Gets optimization recommendation.
     *
     * @return recommendation
     */
    String getRecommendation();

    /**
     * Gets key achievements.
     *
     * @return achievements list
     */
    java.util.List<String> getAchievements();

    /**
     * Gets improvement areas.
     *
     * @return improvement areas
     */
    java.util.List<String> getImprovementAreas();

    /**
     * Gets overall effectiveness.
     *
     * @return effectiveness score (0.0-1.0)
     */
    double getOverallEffectiveness();
  }

  /** Optimization status enumeration. */
  enum OptimizationStatus {
    /** Optimization pending. */
    PENDING,
    /** Optimization running. */
    RUNNING,
    /** Optimization completed. */
    COMPLETED,
    /** Optimization failed. */
    FAILED,
    /** Optimization cancelled. */
    CANCELLED,
    /** Optimization partially completed. */
    PARTIAL
  }

  /** Error severity enumeration. */
  enum ErrorSeverity {
    /** Low severity. */
    LOW,
    /** Medium severity. */
    MEDIUM,
    /** High severity. */
    HIGH,
    /** Critical severity. */
    CRITICAL
  }

  /** Warning category enumeration. */
  enum WarningCategory {
    /** Performance warning. */
    PERFORMANCE,
    /** Memory warning. */
    MEMORY,
    /** Compatibility warning. */
    COMPATIBILITY,
    /** Configuration warning. */
    CONFIGURATION
  }
}
