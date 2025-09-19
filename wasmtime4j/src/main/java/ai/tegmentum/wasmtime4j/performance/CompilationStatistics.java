package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Detailed compilation statistics for individual WebAssembly modules.
 *
 * <p>This interface provides comprehensive metrics about the compilation process of a specific
 * WebAssembly module, including function-level statistics, compilation phases, and optimization
 * metrics.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Get compilation statistics for a module
 * CompilationStatistics stats = CompilationStatistics.forModule(module);
 * System.out.println("Compilation time: " + stats.getCompilationTime());
 * System.out.println("Function count: " + stats.getFunctionCount());
 *
 * // Analyze individual functions
 * for (FunctionStatistics funcStats : stats.getFunctionStatistics().values()) {
 *   System.out.println("Function " + funcStats.getName() + ": " + funcStats.getCompilationTime());
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface CompilationStatistics {

  /**
   * Creates compilation statistics for the specified module.
   *
   * @param module the module to analyze
   * @return compilation statistics for the module
   * @throws IllegalArgumentException if module is null or not compiled
   */
  static CompilationStatistics forModule(final ai.tegmentum.wasmtime4j.Module module) {
    throw new UnsupportedOperationException("Implementation must be provided by runtime factory");
  }

  /**
   * Gets the name or identifier of the module.
   *
   * @return module name, or a generated identifier if not available
   */
  String getModuleName();

  /**
   * Gets the size of the original WebAssembly bytecode.
   *
   * @return bytecode size in bytes
   */
  long getBytecodeSize();

  /**
   * Gets the size of the compiled native code.
   *
   * @return compiled code size in bytes
   */
  long getCompiledCodeSize();

  /**
   * Gets the total time spent compiling this module.
   *
   * @return compilation time
   */
  Duration getCompilationTime();

  /**
   * Gets the number of functions in the module.
   *
   * @return function count
   */
  int getFunctionCount();

  /**
   * Gets the number of imports in the module.
   *
   * @return import count
   */
  int getImportCount();

  /**
   * Gets the number of exports in the module.
   *
   * @return export count
   */
  int getExportCount();

  /**
   * Gets detailed statistics for each function in the module.
   *
   * @return map of function name to function statistics
   */
  Map<String, FunctionStatistics> getFunctionStatistics();

  /**
   * Gets the compilation phases and their durations.
   *
   * @return list of compilation phases
   */
  List<CompilationPhase> getCompilationPhases();

  /**
   * Gets optimization metrics applied during compilation.
   *
   * @return map of optimization name to metric value
   */
  Map<String, Object> getOptimizationMetrics();

  /**
   * Gets the compiler configuration used for this module.
   *
   * @return compiler configuration
   */
  CompilerConfig getCompilerConfig();

  /**
   * Gets the target platform information.
   *
   * @return target platform details
   */
  Target getTargetPlatform();

  /**
   * Gets the code expansion ratio (compiled size / bytecode size).
   *
   * @return code expansion ratio
   */
  default double getCodeExpansionRatio() {
    final long bytecodeSize = getBytecodeSize();
    return bytecodeSize > 0 ? (double) getCompiledCodeSize() / bytecodeSize : 0.0;
  }

  /**
   * Gets the compilation rate in bytes per second.
   *
   * @return compilation rate
   */
  default double getCompilationRate() {
    final Duration compilationTime = getCompilationTime();
    if (compilationTime.isZero()) {
      return 0.0;
    }
    final double seconds = compilationTime.toNanos() / 1_000_000_000.0;
    return getBytecodeSize() / seconds;
  }

  /**
   * Checks if the compilation was efficient.
   *
   * <p>Returns true if compilation time is less than 1ms per KB of bytecode.
   *
   * @return true if compilation was efficient
   */
  default boolean isEfficientCompilation() {
    final long bytecodeKb = getBytecodeSize() / 1024;
    if (bytecodeKb == 0) {
      return true;
    }
    final long compilationMs = getCompilationTime().toMillis();
    return compilationMs / (double) bytecodeKb < 1.0;
  }

  /**
   * Gets the average compilation time per function.
   *
   * @return average function compilation time
   */
  default Duration getAverageFunctionCompilationTime() {
    final int functionCount = getFunctionCount();
    if (functionCount == 0) {
      return Duration.ZERO;
    }
    return getCompilationTime().dividedBy(functionCount);
  }
}