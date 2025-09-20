package ai.tegmentum.wasmtime4j.performance;

import java.time.Instant;
import java.util.List;

/**
 * Represents a call stack sample captured during profiling.
 *
 * <p>Call stacks provide insight into execution flow and help identify performance bottlenecks
 * in complex call hierarchies. Each stack frame contains information about the executing function
 * and its position in the call chain.
 *
 * @since 1.0.0
 */
public interface CallStack {

  /**
   * Gets the timestamp when this call stack was sampled.
   *
   * @return sampling timestamp
   */
  Instant getTimestamp();

  /**
   * Gets the thread ID where this call stack was captured.
   *
   * @return thread identifier
   */
  long getThreadId();

  /**
   * Gets the list of stack frames from top (current) to bottom (root).
   *
   * <p>The first frame represents the currently executing function, and subsequent frames
   * represent the call hierarchy leading to that function.
   *
   * @return ordered list of stack frames
   */
  List<StackFrame> getFrames();

  /**
   * Gets the total depth of the call stack.
   *
   * @return number of frames in the stack
   */
  int getDepth();

  /**
   * Gets the execution context when this stack was sampled.
   *
   * @return execution context information
   */
  ExecutionContext getExecutionContext();

  /**
   * Checks if this call stack represents a hot path.
   *
   * <p>Hot paths are frequently executed code paths that may benefit from optimization.
   *
   * @return true if this is a hot execution path
   */
  boolean isHotPath();

  /**
   * Gets the estimated execution time for this call stack.
   *
   * <p>This represents the cumulative time spent in this call chain at the time of sampling.
   *
   * @return estimated execution time in nanoseconds
   */
  long getExecutionTimeNanos();

  /**
   * Gets memory usage information for this call stack.
   *
   * @return memory usage at the time of sampling
   */
  StackMemoryInfo getMemoryInfo();

  /**
   * Gets any additional metadata associated with this call stack sample.
   *
   * @return call stack metadata, or null if none available
   */
  CallStackMetadata getMetadata();
}