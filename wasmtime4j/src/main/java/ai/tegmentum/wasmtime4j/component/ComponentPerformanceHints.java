package ai.tegmentum.wasmtime4j.component;

import java.util.Map;

/**
 * Performance hints and optimization characteristics for a WebAssembly component.
 *
 * <p>ComponentPerformanceHints provides guidance to the runtime about optimal execution strategies,
 * resource allocation patterns, and performance characteristics of a component. These hints can be
 * used by the runtime for better scheduling, memory management, and optimization decisions.
 *
 * @since 1.0.0
 */
public interface ComponentPerformanceHints {

  /**
   * Gets the expected execution frequency.
   *
   * <p>Returns a hint about how frequently this component is expected to be executed, which can
   * influence optimization and caching strategies.
   *
   * @return execution frequency hint
   */
  ExecutionFrequency getExecutionFrequency();

  /**
   * Gets the expected memory access pattern.
   *
   * <p>Returns information about how the component typically accesses memory, which can help with
   * memory layout optimization and prefetching strategies.
   *
   * @return memory access pattern hint
   */
  MemoryAccessPattern getMemoryAccessPattern();

  /**
   * Gets the CPU intensity level.
   *
   * <p>Returns a hint about the computational intensity of this component's operations.
   *
   * @return CPU intensity level
   */
  CpuIntensity getCpuIntensity();

  /**
   * Gets the I/O characteristics.
   *
   * <p>Returns information about the component's I/O patterns and requirements.
   *
   * @return I/O characteristics
   */
  IoCharacteristics getIoCharacteristics();

  /**
   * Checks if the component benefits from pre-compilation.
   *
   * <p>Returns true if this component would benefit from ahead-of-time compilation strategies.
   *
   * @return true if pre-compilation is beneficial, false otherwise
   */
  boolean benefitsFromPrecompilation();

  /**
   * Checks if the component is suitable for parallel execution.
   *
   * <p>Returns true if this component can safely execute multiple instances in parallel.
   *
   * @return true if parallel execution is safe, false otherwise
   */
  boolean supportsParallelExecution();

  /**
   * Gets the recommended thread pool size.
   *
   * <p>Returns the optimal number of threads for executing this component, or 0 if no specific
   * recommendation is available.
   *
   * @return recommended thread pool size
   */
  int getRecommendedThreadPoolSize();

  /**
   * Gets custom performance hints.
   *
   * <p>Returns additional performance hints that may be specific to particular runtime
   * implementations or optimization strategies.
   *
   * @return map of custom performance hints
   */
  Map<String, Object> getCustomHints();

  /** Execution frequency categories for performance hints. */
  enum ExecutionFrequency {
    /** Component is executed very rarely */
    RARE,
    /** Component is executed occasionally */
    OCCASIONAL,
    /** Component is executed regularly */
    REGULAR,
    /** Component is executed frequently */
    FREQUENT,
    /** Component is executed continuously or in tight loops */
    CONTINUOUS
  }

  /** Memory access pattern categories for performance optimization. */
  enum MemoryAccessPattern {
    /** Sequential memory access patterns */
    SEQUENTIAL,
    /** Random memory access patterns */
    RANDOM,
    /** Localized memory access with good spatial locality */
    LOCALIZED,
    /** Streaming access patterns with high throughput */
    STREAMING,
    /** Mixed access patterns */
    MIXED
  }

  /** CPU intensity levels for resource allocation. */
  enum CpuIntensity {
    /** Low CPU usage, mostly I/O or waiting */
    LOW,
    /** Moderate CPU usage with balanced computation */
    MODERATE,
    /** High CPU usage with intensive computation */
    HIGH,
    /** Very high CPU usage, may benefit from dedicated cores */
    VERY_HIGH
  }

  /** I/O characteristics for performance optimization. */
  enum IoCharacteristics {
    /** Minimal I/O operations */
    MINIMAL,
    /** Read-heavy I/O patterns */
    READ_HEAVY,
    /** Write-heavy I/O patterns */
    WRITE_HEAVY,
    /** Balanced read/write I/O */
    BALANCED,
    /** Network I/O intensive */
    NETWORK_INTENSIVE,
    /** File system I/O intensive */
    FILESYSTEM_INTENSIVE
  }
}
