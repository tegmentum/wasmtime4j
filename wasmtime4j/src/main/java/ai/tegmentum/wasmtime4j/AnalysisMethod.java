package ai.tegmentum.wasmtime4j;

/**
 * Methods available for analyzing WebAssembly execution and performance.
 *
 * <p>Defines different approaches for gathering insights about function behavior, call patterns,
 * and optimization opportunities.
 *
 * @since 1.0.0
 */
public enum AnalysisMethod {
  /** Statistical sampling of execution patterns. */
  SAMPLING_ANALYSIS,

  /** Instrumentation-based detailed tracking. */
  INSTRUMENTATION_ANALYSIS,

  /** Call frequency and pattern analysis. */
  CALL_FREQUENCY_ANALYSIS,

  /** Execution time and performance analysis. */
  EXECUTION_TIME_ANALYSIS,

  /** Memory usage and allocation analysis. */
  MEMORY_USAGE_ANALYSIS,

  /** Function hotness detection. */
  HOTNESS_ANALYSIS,

  /** Dead code and unused function detection. */
  DEAD_CODE_ANALYSIS,

  /** Call graph structure analysis. */
  CALL_GRAPH_ANALYSIS,

  /** Control flow analysis. */
  CONTROL_FLOW_ANALYSIS,

  /** Data flow analysis. */
  DATA_FLOW_ANALYSIS,

  /** Loop detection and analysis. */
  LOOP_ANALYSIS,

  /** Recursive function analysis. */
  RECURSION_ANALYSIS,

  /** Cache behavior analysis. */
  CACHE_ANALYSIS,

  /** Branch prediction analysis. */
  BRANCH_PREDICTION_ANALYSIS,

  /** Compiler optimization opportunity analysis. */
  OPTIMIZATION_ANALYSIS;

  /**
   * Checks if this analysis method requires runtime execution.
   *
   * @return true if runtime execution is required
   */
  public boolean requiresExecution() {
    switch (this) {
      case SAMPLING_ANALYSIS:
      case INSTRUMENTATION_ANALYSIS:
      case CALL_FREQUENCY_ANALYSIS:
      case EXECUTION_TIME_ANALYSIS:
      case MEMORY_USAGE_ANALYSIS:
      case HOTNESS_ANALYSIS:
      case CACHE_ANALYSIS:
      case BRANCH_PREDICTION_ANALYSIS:
        return true;
      case DEAD_CODE_ANALYSIS:
      case CALL_GRAPH_ANALYSIS:
      case CONTROL_FLOW_ANALYSIS:
      case DATA_FLOW_ANALYSIS:
      case LOOP_ANALYSIS:
      case RECURSION_ANALYSIS:
      case OPTIMIZATION_ANALYSIS:
        return false;
      default:
        throw new IllegalArgumentException("Unknown analysis method: " + this);
    }
  }

  /**
   * Checks if this analysis method is performance-intensive.
   *
   * @return true if performance-intensive
   */
  public boolean isPerformanceIntensive() {
    switch (this) {
      case INSTRUMENTATION_ANALYSIS:
      case MEMORY_USAGE_ANALYSIS:
      case CACHE_ANALYSIS:
        return true;
      default:
        return false;
    }
  }

  /**
   * Checks if this analysis method provides timing information.
   *
   * @return true if timing information is provided
   */
  public boolean providesTimingInfo() {
    switch (this) {
      case SAMPLING_ANALYSIS:
      case INSTRUMENTATION_ANALYSIS:
      case EXECUTION_TIME_ANALYSIS:
      case HOTNESS_ANALYSIS:
      case CACHE_ANALYSIS:
        return true;
      default:
        return false;
    }
  }

  /**
   * Checks if this analysis method works on static code.
   *
   * @return true if static analysis
   */
  public boolean isStaticAnalysis() {
    return !requiresExecution();
  }

  /**
   * Checks if this analysis method provides memory information.
   *
   * @return true if memory information is provided
   */
  public boolean providesMemoryInfo() {
    return this == MEMORY_USAGE_ANALYSIS || this == CACHE_ANALYSIS;
  }

  /**
   * Gets the relative overhead of this analysis method.
   *
   * @return overhead level (LOW, MEDIUM, HIGH)
   */
  public OverheadLevel getOverheadLevel() {
    switch (this) {
      case SAMPLING_ANALYSIS:
      case CALL_FREQUENCY_ANALYSIS:
      case DEAD_CODE_ANALYSIS:
      case CALL_GRAPH_ANALYSIS:
      case CONTROL_FLOW_ANALYSIS:
      case DATA_FLOW_ANALYSIS:
      case LOOP_ANALYSIS:
      case RECURSION_ANALYSIS:
      case OPTIMIZATION_ANALYSIS:
        return OverheadLevel.LOW;
      case EXECUTION_TIME_ANALYSIS:
      case HOTNESS_ANALYSIS:
      case BRANCH_PREDICTION_ANALYSIS:
        return OverheadLevel.MEDIUM;
      case INSTRUMENTATION_ANALYSIS:
      case MEMORY_USAGE_ANALYSIS:
      case CACHE_ANALYSIS:
        return OverheadLevel.HIGH;
      default:
        return OverheadLevel.LOW;
    }
  }

  /**
   * Gets a description of this analysis method.
   *
   * @return description string
   */
  public String getDescription() {
    switch (this) {
      case SAMPLING_ANALYSIS:
        return "Statistical sampling of execution patterns and behavior";
      case INSTRUMENTATION_ANALYSIS:
        return "Detailed instrumentation-based tracking of all operations";
      case CALL_FREQUENCY_ANALYSIS:
        return "Analysis of function call frequencies and patterns";
      case EXECUTION_TIME_ANALYSIS:
        return "Measurement and analysis of execution times";
      case MEMORY_USAGE_ANALYSIS:
        return "Analysis of memory allocation and usage patterns";
      case HOTNESS_ANALYSIS:
        return "Detection of frequently executed (hot) code sections";
      case DEAD_CODE_ANALYSIS:
        return "Detection of unused or unreachable code";
      case CALL_GRAPH_ANALYSIS:
        return "Analysis of function call relationships and structure";
      case CONTROL_FLOW_ANALYSIS:
        return "Analysis of program control flow and branching";
      case DATA_FLOW_ANALYSIS:
        return "Analysis of data dependencies and flow";
      case LOOP_ANALYSIS:
        return "Detection and analysis of loop structures";
      case RECURSION_ANALYSIS:
        return "Analysis of recursive function calls and patterns";
      case CACHE_ANALYSIS:
        return "Analysis of cache behavior and performance";
      case BRANCH_PREDICTION_ANALYSIS:
        return "Analysis of branch prediction effectiveness";
      case OPTIMIZATION_ANALYSIS:
        return "Analysis of compiler optimization opportunities";
      default:
        return "Unknown analysis method";
    }
  }

  /** Overhead levels for analysis methods. */
  public enum OverheadLevel {
    /** Minimal performance impact. */
    LOW,

    /** Moderate performance impact. */
    MEDIUM,

    /** Significant performance impact. */
    HIGH
  }
}
