package ai.tegmentum.wasmtime4j.toolchain;

/**
 * Profiling modes for WebAssembly execution.
 *
 * <p>Defines the different approaches available for profiling
 * WebAssembly code execution and performance analysis.
 *
 * @since 1.0.0
 */
public enum ProfilingMode {
  /** Statistical sampling profiling */
  SAMPLING,

  /** Instrumentation-based profiling */
  INSTRUMENTATION,

  /** Event-based profiling */
  EVENT_BASED,

  /** Memory allocation profiling */
  MEMORY_ALLOCATION,

  /** Function call profiling */
  FUNCTION_CALLS,

  /** Instruction-level profiling */
  INSTRUCTION_LEVEL;

  /**
   * Checks if this profiling mode is performance-intensive.
   *
   * @return true if performance-intensive
   */
  public boolean isPerformanceIntensive() {
    return this == INSTRUMENTATION || this == INSTRUCTION_LEVEL;
  }

  /**
   * Checks if this profiling mode tracks memory.
   *
   * @return true if memory tracking is involved
   */
  public boolean tracksMemory() {
    return this == MEMORY_ALLOCATION;
  }

  /**
   * Gets a description of this profiling mode.
   *
   * @return description string
   */
  public String getDescription() {
    return switch (this) {
      case SAMPLING -> "Statistical sampling of execution";
      case INSTRUMENTATION -> "Code instrumentation for detailed metrics";
      case EVENT_BASED -> "Event-driven profiling";
      case MEMORY_ALLOCATION -> "Memory allocation and usage tracking";
      case FUNCTION_CALLS -> "Function call frequency and timing";
      case INSTRUCTION_LEVEL -> "Individual instruction execution tracking";
    };
  }
}