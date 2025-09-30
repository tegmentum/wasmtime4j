package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Execution context for tail call operations.
 *
 * <p>The tail call context provides access to the runtime facilities needed for proper tail call
 * elimination, including stack frame management, function resolution, and optimization state.
 *
 * @since 1.1.0
 */
public interface TailCallContext {

  /**
   * Checks if tail calls are supported in this context.
   *
   * @return true if tail calls are supported
   */
  boolean supportsTailCalls();

  /**
   * Resolves a function by its index.
   *
   * @param functionIndex the function index
   * @return the function reference, or null if not found
   * @throws WasmException if function resolution fails
   */
  FunctionReference resolveFunction(final int functionIndex) throws WasmException;

  /**
   * Resolves an indirect function through a table.
   *
   * @param tableIndex the table index
   * @param functionIndex the function index within the table
   * @param expectedType the expected function type for validation
   * @return the function reference, or null if not found
   * @throws WasmException if function resolution fails
   */
  FunctionReference resolveIndirectFunction(
      final int tableIndex, final int functionIndex, final FunctionType expectedType)
      throws WasmException;

  /**
   * Prepares for stack frame replacement in tail call optimization.
   *
   * <p>This method sets up the runtime state to reuse the current stack frame instead of creating a
   * new one for the tail call.
   */
  void prepareFrameReplacement();

  /**
   * Performs a return operation after a tail call.
   *
   * @param results the return values
   * @throws WasmException if the return operation fails
   */
  void performReturn(final WasmValue... results) throws WasmException;

  /**
   * Gets the current function context.
   *
   * @return the current function context
   */
  FunctionContext getCurrentFunction();

  /**
   * Gets tail call optimization statistics.
   *
   * @return optimization statistics
   */
  TailCallStatistics getStatistics();

  /**
   * Enables or disables tail call optimization.
   *
   * @param enabled true to enable tail call optimization
   */
  void setTailCallOptimizationEnabled(final boolean enabled);

  /**
   * Checks if tail call optimization is currently enabled.
   *
   * @return true if tail call optimization is enabled
   */
  boolean isTailCallOptimizationEnabled();

  /**
   * Gets the maximum recursion depth before stack overflow.
   *
   * @return maximum recursion depth
   */
  int getMaxRecursionDepth();

  /**
   * Sets the maximum recursion depth.
   *
   * @param maxDepth the maximum recursion depth
   */
  void setMaxRecursionDepth(final int maxDepth);

  /** Statistics for tail call optimization. */
  interface TailCallStatistics {
    /** Total number of tail calls attempted. */
    long getTotalTailCalls();

    /** Number of successful tail call optimizations. */
    long getOptimizedTailCalls();

    /** Number of tail calls that fell back to regular calls. */
    long getFallbackTailCalls();

    /** Total stack space saved through tail call optimization (bytes). */
    long getTotalStackSpaceSaved();

    /** Average stack space saved per optimized tail call (bytes). */
    double getAverageStackSpaceSaved();

    /** Most frequently tail-called functions. */
    TailCallHotSpot[] getHotSpots();

    /** Represents a tail call optimization hot spot. */
    interface TailCallHotSpot {
      String getFunctionName();

      int getFunctionIndex();

      long getTailCallCount();

      long getStackSpaceSaved();

      double getOptimizationRate();
    }
  }
}
