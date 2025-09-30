package ai.tegmentum.wasmtime4j.profiling;

/**
 * Flame graph generator interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface FlameGraphGenerator {

  /** Flame graph data interface. */
  interface FlameGraphData {
    /**
     * Gets the function name.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets the execution time.
     *
     * @return execution time in nanoseconds
     */
    long getExecutionTime();

    /**
     * Gets the call count.
     *
     * @return call count
     */
    long getCallCount();
  }

  /**
   * Generates flame graph data.
   *
   * @return flame graph data
   */
  FlameGraphData generateFlameGraph();

  /**
   * Exports flame graph as SVG.
   *
   * @return SVG string
   */
  String exportAsSvg();

  /**
   * Checks if generation is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /** Flame frame interface representing a stack frame in the flame graph. */
  interface FlameFrame {
    /**
     * Gets the frame function name.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets the frame duration.
     *
     * @return duration in nanoseconds
     */
    long getDuration();

    /**
     * Gets the frame depth in the call stack.
     *
     * @return stack depth
     */
    int getDepth();

    /**
     * Gets child frames.
     *
     * @return list of child frames
     */
    java.util.List<FlameFrame> getChildren();
  }
}
