package ai.tegmentum.wasmtime4j.performance;

/**
 * Represents a single frame in a call stack.
 *
 * <p>Stack frames contain information about the executing function, its location, and performance
 * characteristics at the time of sampling.
 *
 * @since 1.0.0
 */
public interface StackFrame {

  /**
   * Gets the name of the function in this stack frame.
   *
   * @return function name, or "[unknown]" if not available
   */
  String getFunctionName();

  /**
   * Gets the module name containing this function.
   *
   * @return module name, or null if not available
   */
  String getModuleName();

  /**
   * Gets the instruction offset within the function.
   *
   * @return instruction offset, or -1 if not available
   */
  long getInstructionOffset();

  /**
   * Gets the source location information if available.
   *
   * @return source location, or null if debug information not available
   */
  SourceLocation getSourceLocation();

  /**
   * Checks if this frame represents a WebAssembly function.
   *
   * @return true if this is a WebAssembly function frame
   */
  boolean isWasmFunction();

  /**
   * Checks if this frame represents a host function call.
   *
   * @return true if this is a host function frame
   */
  boolean isHostFunction();

  /**
   * Gets the execution time spent in this specific frame.
   *
   * <p>This represents the time spent in this function excluding time spent in called functions.
   *
   * @return exclusive execution time in nanoseconds
   */
  long getExclusiveTimeNanos();

  /**
   * Gets the total execution time including called functions.
   *
   * <p>This represents the time spent in this function including time spent in all called
   * functions.
   *
   * @return inclusive execution time in nanoseconds
   */
  long getInclusiveTimeNanos();

  /**
   * Gets the number of instructions executed in this frame.
   *
   * @return instruction count, or -1 if not available
   */
  long getInstructionCount();

  /**
   * Gets memory allocation information for this frame.
   *
   * @return frame memory information
   */
  FrameMemoryInfo getMemoryInfo();
}
