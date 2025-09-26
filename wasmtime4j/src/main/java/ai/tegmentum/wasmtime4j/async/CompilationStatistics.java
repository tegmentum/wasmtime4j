package ai.tegmentum.wasmtime4j.async;

/**
 * Detailed statistics about WebAssembly module compilation.
 *
 * <p>These statistics provide insight into resource usage and
 * performance characteristics of the compilation process.
 *
 * @since 1.0.0
 */
public final class CompilationStatistics {

  private final long bytesProcessed;
  private final int functionsCompiled;
  private final int parallelThreadsUsed;
  private final long peakMemoryUsage;
  private final long totalCpuTime;
  private final double cpuUtilization;
  private final int optimizationPasses;
  private final String compilerBackend;

  /**
   * Creates new compilation statistics.
   *
   * @param bytesProcessed total bytes of WebAssembly processed
   * @param functionsCompiled number of functions compiled
   * @param parallelThreadsUsed maximum parallel threads used
   * @param peakMemoryUsage peak memory usage during compilation
   * @param totalCpuTime total CPU time used across all threads
   * @param cpuUtilization overall CPU utilization percentage
   * @param optimizationPasses number of optimization passes performed
   * @param compilerBackend compiler backend used (e.g., "cranelift", "winch")
   */
  public CompilationStatistics(
      final long bytesProcessed,
      final int functionsCompiled,
      final int parallelThreadsUsed,
      final long peakMemoryUsage,
      final long totalCpuTime,
      final double cpuUtilization,
      final int optimizationPasses,
      final String compilerBackend) {
    this.bytesProcessed = bytesProcessed;
    this.functionsCompiled = functionsCompiled;
    this.parallelThreadsUsed = parallelThreadsUsed;
    this.peakMemoryUsage = peakMemoryUsage;
    this.totalCpuTime = totalCpuTime;
    this.cpuUtilization = cpuUtilization;
    this.optimizationPasses = optimizationPasses;
    this.compilerBackend = compilerBackend;
  }

  /**
   * Creates default statistics for basic compilation.
   *
   * @param bytesProcessed bytes processed
   * @return basic statistics
   */
  public static CompilationStatistics basic(final long bytesProcessed) {
    return new CompilationStatistics(
        bytesProcessed, 0, 1, 0L, 0L, 0.0, 0, "cranelift");
  }

  /**
   * Gets the total bytes of WebAssembly processed.
   *
   * @return bytes processed
   */
  public long getBytesProcessed() {
    return bytesProcessed;
  }

  /**
   * Gets the number of functions compiled.
   *
   * @return functions compiled count
   */
  public int getFunctionsCompiled() {
    return functionsCompiled;
  }

  /**
   * Gets the maximum number of parallel threads used.
   *
   * @return parallel threads used
   */
  public int getParallelThreadsUsed() {
    return parallelThreadsUsed;
  }

  /**
   * Gets the peak memory usage during compilation.
   *
   * @return peak memory usage in bytes
   */
  public long getPeakMemoryUsage() {
    return peakMemoryUsage;
  }

  /**
   * Gets the total CPU time used across all threads.
   *
   * @return total CPU time in nanoseconds
   */
  public long getTotalCpuTime() {
    return totalCpuTime;
  }

  /**
   * Gets the overall CPU utilization percentage.
   *
   * @return CPU utilization (0.0 - 1.0)
   */
  public double getCpuUtilization() {
    return cpuUtilization;
  }

  /**
   * Gets the number of optimization passes performed.
   *
   * @return optimization passes count
   */
  public int getOptimizationPasses() {
    return optimizationPasses;
  }

  /**
   * Gets the compiler backend used.
   *
   * @return compiler backend name
   */
  public String getCompilerBackend() {
    return compilerBackend;
  }

  /**
   * Calculates the parallelization efficiency.
   *
   * @return efficiency ratio (0.0 - 1.0)
   */
  public double getParallelizationEfficiency() {
    return parallelThreadsUsed > 0 ? cpuUtilization / parallelThreadsUsed : 0.0;
  }

  /**
   * Gets the average bytes processed per function.
   *
   * @return bytes per function
   */
  public double getBytesPerFunction() {
    return functionsCompiled > 0 ? (double) bytesProcessed / functionsCompiled : 0.0;
  }

  @Override
  public String toString() {
    return String.format(
        "CompilationStatistics{bytes=%d, functions=%d, threads=%d, backend='%s', efficiency=%.2f}",
        bytesProcessed, functionsCompiled, parallelThreadsUsed, compilerBackend,
        getParallelizationEfficiency());
  }
}