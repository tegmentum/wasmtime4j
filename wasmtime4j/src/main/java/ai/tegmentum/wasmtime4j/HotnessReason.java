package ai.tegmentum.wasmtime4j;

/**
 * Reasons why a function was identified as hot during analysis.
 *
 * <p>HotnessReason provides context about how and why a function was classified as hot,
 * which can be useful for understanding optimization decisions and debugging performance issues.
 *
 * @since 1.0.0
 */
public enum HotnessReason {
  /**
   * Function identified as hot through static analysis.
   *
   * <p>Static analysis examines code structure, call graphs, and patterns without runtime data.
   */
  STATIC_ANALYSIS,

  /**
   * Function identified as hot through runtime profiling data.
   *
   * <p>Runtime profiling provides actual execution statistics from previous runs.
   */
  RUNTIME_PROFILING,

  /**
   * Function identified as hot due to frequent calls from other hot functions.
   *
   * <p>Call graph analysis shows this function is called by other frequently executed functions.
   */
  HOT_CALLER_PROPAGATION,

  /**
   * Function identified as hot due to being in critical execution paths.
   *
   * <p>Critical path analysis identifies functions that are essential for core application flow.
   */
  CRITICAL_PATH,

  /**
   * Function identified as hot due to being in loops or recursive calls.
   *
   * <p>Loop analysis identifies functions that are executed repeatedly within loops.
   */
  LOOP_ANALYSIS,

  /**
   * Function identified as hot through heuristic-based analysis.
   *
   * <p>Heuristics use patterns and rules-of-thumb to identify likely hot functions.
   */
  HEURISTIC,

  /**
   * Function manually marked as hot by user annotation or configuration.
   *
   * <p>User-provided hints override automatic analysis results.
   */
  USER_ANNOTATION,

  /**
   * Function identified as hot based on machine learning predictions.
   *
   * <p>ML models predict function hotness based on code features and patterns.
   */
  MACHINE_LEARNING
}