package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Comprehensive statistics collection for WebAssembly engine operations.
 *
 * <p>This interface provides detailed metrics about engine performance including compilation,
 * execution, memory usage, and JIT compilation statistics. Statistics can be captured as snapshots
 * or continuously monitored for performance analysis.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Capture current engine statistics
 * EngineStatistics stats = EngineStatistics.capture(engine);
 * System.out.println("Modules compiled: " + stats.getModulesCompiled());
 * System.out.println("Average compilation time: " + stats.getAverageCompilationTime());
 *
 * // Capture and reset for delta measurements
 * EngineStatistics deltaStats = EngineStatistics.captureAndReset(engine);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface EngineStatistics {

  /**
   * Captures current engine statistics without affecting existing counters.
   *
   * @param engine the engine to capture statistics from
   * @return engine statistics snapshot
   * @throws IllegalArgumentException if engine is null
   */
  static EngineStatistics capture(final ai.tegmentum.wasmtime4j.Engine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }

    // Use runtime-specific engine statistics implementation
    try {
      // First try Panama implementation
      final Class<?> panamaClass = Class.forName("ai.tegmentum.wasmtime4j.panama.PanamaEngineStatistics");
      return (EngineStatistics) panamaClass.getDeclaredMethod("capture", ai.tegmentum.wasmtime4j.Engine.class)
          .invoke(null, engine);
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> jniClass = Class.forName("ai.tegmentum.wasmtime4j.jni.JniEngineStatistics");
        return (EngineStatistics) jniClass.getDeclaredMethod("capture", ai.tegmentum.wasmtime4j.Engine.class)
            .invoke(null, engine);
      } catch (final ClassNotFoundException e2) {
        // No specific implementation found
        throw new RuntimeException(
            "No EngineStatistics implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create JNI EngineStatistics instance", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create Panama EngineStatistics instance", e);
    }
  }

  /**
   * Captures current engine statistics and resets all counters to zero.
   *
   * <p>This method is useful for measuring performance deltas between specific time periods.
   *
   * @param engine the engine to capture statistics from
   * @return engine statistics snapshot before reset
   * @throws IllegalArgumentException if engine is null
   */
  static EngineStatistics captureAndReset(final ai.tegmentum.wasmtime4j.Engine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }

    // Use runtime-specific engine statistics implementation
    try {
      // First try Panama implementation
      final Class<?> panamaClass = Class.forName("ai.tegmentum.wasmtime4j.panama.PanamaEngineStatistics");
      return (EngineStatistics) panamaClass.getDeclaredMethod("captureAndReset", ai.tegmentum.wasmtime4j.Engine.class)
          .invoke(null, engine);
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> jniClass = Class.forName("ai.tegmentum.wasmtime4j.jni.JniEngineStatistics");
        return (EngineStatistics) jniClass.getDeclaredMethod("captureAndReset", ai.tegmentum.wasmtime4j.Engine.class)
            .invoke(null, engine);
      } catch (final ClassNotFoundException e2) {
        // No specific implementation found
        throw new RuntimeException(
            "No EngineStatistics implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create JNI EngineStatistics instance", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create Panama EngineStatistics instance", e);
    }
  }

  // Compilation metrics

  /**
   * Gets the total number of modules compiled by this engine.
   *
   * @return number of modules compiled
   */
  long getModulesCompiled();

  /**
   * Gets the cumulative time spent compiling modules.
   *
   * @return total compilation time
   */
  Duration getTotalCompilationTime();

  /**
   * Gets the average compilation time per module.
   *
   * @return average compilation time, or Duration.ZERO if no modules compiled
   */
  Duration getAverageCompilationTime();

  /**
   * Gets the total number of WebAssembly bytes compiled.
   *
   * @return bytes compiled
   */
  long getBytesCompiled();

  /**
   * Gets the compilation throughput in bytes per second.
   *
   * @return compilation throughput
   */
  double getCompilationThroughput();

  // Execution metrics

  /**
   * Gets the total number of WebAssembly functions executed.
   *
   * @return number of functions executed
   */
  long getFunctionsExecuted();

  /**
   * Gets the cumulative time spent executing WebAssembly functions.
   *
   * @return total execution time
   */
  Duration getTotalExecutionTime();

  /**
   * Gets the total number of WebAssembly instructions executed.
   *
   * @return number of instructions executed
   */
  long getInstructionsExecuted();

  /**
   * Gets the execution throughput in instructions per second.
   *
   * @return execution throughput
   */
  double getExecutionThroughput();

  // Memory metrics

  /**
   * Gets the peak memory usage by the engine.
   *
   * @return peak memory usage in bytes
   */
  long getPeakMemoryUsage();

  /**
   * Gets the current memory usage by the engine.
   *
   * @return current memory usage in bytes
   */
  long getCurrentMemoryUsage();

  /**
   * Gets the total number of memory allocations performed.
   *
   * @return total allocations
   */
  long getTotalAllocations();

  /**
   * Gets the total number of memory deallocations performed.
   *
   * @return total deallocations
   */
  long getTotalDeallocations();

  // Cache metrics

  /**
   * Gets the number of cache hits for compiled code.
   *
   * @return cache hits
   */
  long getCacheHits();

  /**
   * Gets the number of cache misses for compiled code.
   *
   * @return cache misses
   */
  long getCacheMisses();

  /**
   * Gets the cache hit ratio as a percentage.
   *
   * @return cache hit ratio (0.0 to 1.0)
   */
  double getCacheHitRatio();

  // JIT metrics

  /**
   * Gets the number of JIT compilations performed.
   *
   * @return JIT compilations
   */
  long getJitCompilations();

  /**
   * Gets the cumulative time spent in JIT compilation.
   *
   * @return JIT compilation time
   */
  Duration getJitCompilationTime();

  /**
   * Gets the total size of JIT compiled code.
   *
   * @return JIT code size in bytes
   */
  long getJitCodeSize();

  // Metadata

  /**
   * Gets the timestamp when these statistics were captured.
   *
   * @return capture timestamp
   */
  Instant getCaptureTime();

  /**
   * Gets the engine uptime at the time of capture.
   *
   * @return engine uptime
   */
  Duration getUptime();

  /**
   * Resets all statistics counters to zero.
   *
   * <p>This method allows for delta measurements between specific time periods.
   */
  void reset();

  /**
   * Gets extended statistics as a map of key-value pairs.
   *
   * <p>This method provides access to implementation-specific metrics that may not be covered by
   * the standard interface methods.
   *
   * @return map of extended statistics
   */
  Map<String, Object> getExtendedStatistics();

  /**
   * Checks if the engine has high compilation overhead.
   *
   * <p>Returns true if compilation time represents more than 50% of total engine time.
   *
   * @return true if compilation overhead is high
   */
  default boolean hasHighCompilationOverhead() {
    final Duration totalTime = getTotalCompilationTime().plus(getTotalExecutionTime());
    if (totalTime.isZero()) {
      return false;
    }
    final double compilationRatio =
        (double) getTotalCompilationTime().toNanos() / totalTime.toNanos();
    return compilationRatio > 0.5;
  }

  /**
   * Checks if the engine has efficient memory utilization.
   *
   * <p>Returns true if current memory usage is less than 80% of peak usage.
   *
   * @return true if memory utilization is efficient
   */
  default boolean hasEfficientMemoryUtilization() {
    final long peak = getPeakMemoryUsage();
    if (peak == 0) {
      return true;
    }
    final double utilization = (double) getCurrentMemoryUsage() / peak;
    return utilization < 0.8;
  }

  /**
   * Gets the overall performance score based on multiple metrics.
   *
   * <p>The score is calculated from compilation efficiency, execution throughput, memory
   * utilization, and cache performance. Score ranges from 0 (poor) to 100 (excellent).
   *
   * @return performance score (0-100)
   */
  default double getPerformanceScore() {
    double score = 0.0;
    int factors = 0;

    // Cache performance (25 points)
    final double cacheRatio = getCacheHitRatio();
    if (cacheRatio >= 0) {
      score += cacheRatio * 25;
      factors++;
    }

    // Compilation efficiency (25 points)
    if (!hasHighCompilationOverhead()) {
      score += 25;
    }
    factors++;

    // Memory efficiency (25 points)
    if (hasEfficientMemoryUtilization()) {
      score += 25;
    }
    factors++;

    // Execution throughput (25 points)
    final double throughput = getExecutionThroughput();
    if (throughput > 1_000_000) { // > 1M instructions/sec
      score += 25;
    } else if (throughput > 100_000) { // > 100K instructions/sec
      score += 15;
    } else if (throughput > 10_000) { // > 10K instructions/sec
      score += 10;
    }
    factors++;

    return factors > 0 ? score / factors * 4 : 0.0; // Normalize to 0-100
  }
}
