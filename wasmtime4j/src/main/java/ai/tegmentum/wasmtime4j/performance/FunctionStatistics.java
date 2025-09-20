package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Detailed compilation statistics for an individual WebAssembly function.
 *
 * <p>This class provides comprehensive metrics about the compilation process of a specific
 * function, including size information, compilation time, and optimization details.
 *
 * @since 1.0.0
 */
public final class FunctionStatistics {
  private final String name;
  private final int index;
  private final long bytecodeSize;
  private final long compiledSize;
  private final Duration compilationTime;
  private final int basicBlockCount;
  private final Map<String, Object> optimizations;

  /**
   * Creates function statistics.
   *
   * @param name the function name or identifier
   * @param index the function index in the module
   * @param bytecodeSize the size of the function bytecode in bytes
   * @param compiledSize the size of the compiled function in bytes
   * @param compilationTime the time spent compiling this function
   * @param basicBlockCount the number of basic blocks in the function
   * @param optimizations map of optimization details
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public FunctionStatistics(
      final String name,
      final int index,
      final long bytecodeSize,
      final long compiledSize,
      final Duration compilationTime,
      final int basicBlockCount,
      final Map<String, Object> optimizations) {
    this.name = Objects.requireNonNull(name, "name cannot be null");
    this.index = index;
    this.bytecodeSize = Math.max(0, bytecodeSize);
    this.compiledSize = Math.max(0, compiledSize);
    this.compilationTime =
        Objects.requireNonNull(compilationTime, "compilationTime cannot be null");
    this.basicBlockCount = Math.max(0, basicBlockCount);
    this.optimizations =
        Map.copyOf(Objects.requireNonNull(optimizations, "optimizations cannot be null"));

    if (index < 0) {
      throw new IllegalArgumentException("index cannot be negative: " + index);
    }
  }

  /**
   * Gets the function name or identifier.
   *
   * @return function name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the function index in the module.
   *
   * @return function index
   */
  public int getIndex() {
    return index;
  }

  /**
   * Gets the size of the function bytecode.
   *
   * @return bytecode size in bytes
   */
  public long getBytecodeSize() {
    return bytecodeSize;
  }

  /**
   * Gets the size of the compiled function.
   *
   * @return compiled size in bytes
   */
  public long getCompiledSize() {
    return compiledSize;
  }

  /**
   * Gets the time spent compiling this function.
   *
   * @return compilation time
   */
  public Duration getCompilationTime() {
    return compilationTime;
  }

  /**
   * Gets the number of basic blocks in the function.
   *
   * @return basic block count
   */
  public int getBasicBlockCount() {
    return basicBlockCount;
  }

  /**
   * Gets the optimization details for this function.
   *
   * @return map of optimization details
   */
  public Map<String, Object> getOptimizations() {
    return optimizations;
  }

  /**
   * Gets the code expansion ratio (compiled size / bytecode size).
   *
   * @return code expansion ratio
   */
  public double getCodeExpansionRatio() {
    return bytecodeSize > 0 ? (double) compiledSize / bytecodeSize : 0.0;
  }

  /**
   * Gets the compilation rate in bytes per second.
   *
   * @return compilation rate for this function
   */
  public double getCompilationRate() {
    if (compilationTime.isZero()) {
      return 0.0;
    }
    final double seconds = compilationTime.toNanos() / 1_000_000_000.0;
    return bytecodeSize / seconds;
  }

  /**
   * Gets the compilation time per basic block.
   *
   * @return average compilation time per basic block
   */
  public Duration getCompilationTimePerBasicBlock() {
    return basicBlockCount > 0 ? compilationTime.dividedBy(basicBlockCount) : Duration.ZERO;
  }

  /**
   * Checks if this function had efficient compilation.
   *
   * <p>Returns true if compilation time is less than 100μs per basic block.
   *
   * @return true if compilation was efficient
   */
  public boolean isEfficientCompilation() {
    if (basicBlockCount == 0) {
      return true;
    }
    final long microsPerBlock = compilationTime.toNanos() / (basicBlockCount * 1000);
    return microsPerBlock < 100;
  }

  /**
   * Checks if this function is likely performance-critical.
   *
   * <p>Returns true if the function has a high number of basic blocks or large bytecode size.
   *
   * @return true if function is likely performance-critical
   */
  public boolean isPerformanceCritical() {
    return basicBlockCount > 50 || bytecodeSize > 1024;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final FunctionStatistics that = (FunctionStatistics) obj;
    return index == that.index
        && bytecodeSize == that.bytecodeSize
        && compiledSize == that.compiledSize
        && basicBlockCount == that.basicBlockCount
        && Objects.equals(name, that.name)
        && Objects.equals(compilationTime, that.compilationTime)
        && Objects.equals(optimizations, that.optimizations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        name, index, bytecodeSize, compiledSize, compilationTime, basicBlockCount, optimizations);
  }

  @Override
  public String toString() {
    return String.format(
        "FunctionStatistics{name='%s', index=%d, bytecodeSize=%d, compiledSize=%d, "
            + "compilationTime=%s, basicBlockCount=%d, optimizations=%s}",
        name, index, bytecodeSize, compiledSize, compilationTime, basicBlockCount, optimizations);
  }
}
