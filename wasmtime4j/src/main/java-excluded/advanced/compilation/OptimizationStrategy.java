package ai.tegmentum.wasmtime4j.compilation;

import java.util.List;
import java.util.Map;

/**
 * Interface for WebAssembly compilation optimization strategies.
 *
 * <p>Defines the contract for implementing sophisticated optimization techniques
 * including inlining, dead code elimination, loop optimization, and vectorization.
 *
 * @since 1.0.0
 */
public interface OptimizationStrategy {

  /**
   * Gets the optimization strategy name.
   *
   * @return strategy name
   */
  String getName();

  /**
   * Gets the optimization strategy description.
   *
   * @return strategy description
   */
  String getDescription();

  /**
   * Gets the optimization level (higher = more aggressive).
   *
   * @return optimization level (0-10)
   */
  int getOptimizationLevel();

  /**
   * Estimates the compilation overhead for this optimization.
   *
   * @param moduleSize size of the WebAssembly module in bytes
   * @return estimated compilation overhead factor (1.0 = no overhead)
   */
  double estimateCompilationOverhead(long moduleSize);

  /**
   * Estimates the performance improvement for this optimization.
   *
   * @param executionProfile execution profile data
   * @return estimated performance improvement factor (1.0 = no improvement)
   */
  double estimatePerformanceImprovement(ExecutionProfile executionProfile);

  /**
   * Determines if this optimization is applicable to the given execution profile.
   *
   * @param profile execution profile data
   * @return true if optimization is applicable
   */
  boolean isApplicable(ExecutionProfile profile);

  /**
   * Gets the Cranelift configuration flags for this optimization.
   *
   * @return map of Cranelift flags
   */
  Map<String, String> getCraneliftFlags();

  /**
   * Gets the optimization dependencies (optimizations that should be run first).
   *
   * @return list of prerequisite optimization strategy names
   */
  List<String> getDependencies();

  /**
   * Gets the optimization conflicts (optimizations that are incompatible).
   *
   * @return list of conflicting optimization strategy names
   */
  List<String> getConflicts();
}

/**
 * Execution profile data for optimization decision making.
 */
final class ExecutionProfile {
  private final long executionCount;
  private final double averageExecutionTimeMs;
  private final long totalExecutionTimeMs;
  private final double cpuUtilization;
  private final long memoryUsage;
  private final boolean hasLoops;
  private final boolean hasVectorOperations;
  private final boolean hasRecursion;
  private final int functionCount;
  private final long moduleSize;

  public ExecutionProfile(final long executionCount,
                          final double averageExecutionTimeMs,
                          final long totalExecutionTimeMs,
                          final double cpuUtilization,
                          final long memoryUsage,
                          final boolean hasLoops,
                          final boolean hasVectorOperations,
                          final boolean hasRecursion,
                          final int functionCount,
                          final long moduleSize) {
    this.executionCount = executionCount;
    this.averageExecutionTimeMs = averageExecutionTimeMs;
    this.totalExecutionTimeMs = totalExecutionTimeMs;
    this.cpuUtilization = cpuUtilization;
    this.memoryUsage = memoryUsage;
    this.hasLoops = hasLoops;
    this.hasVectorOperations = hasVectorOperations;
    this.hasRecursion = hasRecursion;
    this.functionCount = functionCount;
    this.moduleSize = moduleSize;
  }

  public long getExecutionCount() { return executionCount; }
  public double getAverageExecutionTimeMs() { return averageExecutionTimeMs; }
  public long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }
  public double getCpuUtilization() { return cpuUtilization; }
  public long getMemoryUsage() { return memoryUsage; }
  public boolean hasLoops() { return hasLoops; }
  public boolean hasVectorOperations() { return hasVectorOperations; }
  public boolean hasRecursion() { return hasRecursion; }
  public int getFunctionCount() { return functionCount; }
  public long getModuleSize() { return moduleSize; }

  /**
   * Determines if this is a hot path based on execution characteristics.
   */
  public boolean isHotPath() {
    return executionCount > 1000 || totalExecutionTimeMs > 5000 ||
           (averageExecutionTimeMs > 10.0 && executionCount > 100);
  }

  /**
   * Determines if this is compute-intensive based on CPU utilization.
   */
  public boolean isComputeIntensive() {
    return cpuUtilization > 0.7 && averageExecutionTimeMs > 5.0;
  }

  /**
   * Determines if this is memory-intensive based on memory usage.
   */
  public boolean isMemoryIntensive() {
    return memoryUsage > 1024 * 1024; // 1MB threshold
  }
}

/**
 * Abstract base class for optimization strategies.
 */
abstract class BaseOptimizationStrategy implements OptimizationStrategy {

  protected final String name;
  protected final String description;
  protected final int optimizationLevel;

  protected BaseOptimizationStrategy(final String name,
                                     final String description,
                                     final int optimizationLevel) {
    this.name = name;
    this.description = description;
    this.optimizationLevel = optimizationLevel;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public int getOptimizationLevel() {
    return optimizationLevel;
  }

  @Override
  public List<String> getDependencies() {
    return List.of(); // No dependencies by default
  }

  @Override
  public List<String> getConflicts() {
    return List.of(); // No conflicts by default
  }

  @Override
  public String toString() {
    return String.format("%s (level %d): %s", name, optimizationLevel, description);
  }
}