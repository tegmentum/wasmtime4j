package ai.tegmentum.wasmtime4j.compilation;

/**
 * Priority levels for WebAssembly compilation tasks.
 *
 * <p>Defines the scheduling priority for compilation work, allowing
 * hot functions to be compiled before cold ones.
 *
 * @since 1.0.0
 */
public enum CompilationPriority {
  /** Lowest priority - compile when resources are available */
  LOW(0),

  /** Normal priority - standard compilation scheduling */
  NORMAL(1),

  /** High priority - compile ahead of normal tasks */
  HIGH(2),

  /** Critical priority - compile immediately */
  CRITICAL(3),

  /** Emergency priority - preempt other tasks */
  EMERGENCY(4);

  private final int value;

  CompilationPriority(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric priority value.
   *
   * @return priority value (higher numbers = higher priority)
   */
  public int getValue() {
    return value;
  }

  /**
   * Determines priority based on execution frequency.
   *
   * @param executionCount the number of times the function has been executed
   * @return recommended priority level
   */
  public static CompilationPriority fromExecutionCount(final long executionCount) {
    if (executionCount >= 10000) {
      return CRITICAL;
    } else if (executionCount >= 1000) {
      return HIGH;
    } else if (executionCount >= 100) {
      return NORMAL;
    } else {
      return LOW;
    }
  }

  /**
   * Determines priority based on function hotness.
   *
   * @param callsPerSecond the rate of function calls
   * @return recommended priority level
   */
  public static CompilationPriority fromHotness(final double callsPerSecond) {
    if (callsPerSecond >= 100.0) {
      return EMERGENCY;
    } else if (callsPerSecond >= 50.0) {
      return CRITICAL;
    } else if (callsPerSecond >= 10.0) {
      return HIGH;
    } else if (callsPerSecond >= 1.0) {
      return NORMAL;
    } else {
      return LOW;
    }
  }
}