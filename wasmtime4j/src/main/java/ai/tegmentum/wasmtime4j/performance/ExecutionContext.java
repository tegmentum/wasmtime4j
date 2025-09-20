package ai.tegmentum.wasmtime4j.performance;

/**
 * Provides execution context information for performance analysis.
 *
 * <p>Execution context includes information about the runtime state, compilation state, and other
 * contextual factors that may affect performance.
 *
 * @since 1.0.0
 */
public interface ExecutionContext {

  /**
   * Gets the current compilation tier for the executing code.
   *
   * @return compilation tier (interpreted, baseline, optimized, etc.)
   */
  CompilationTier getCompilationTier();

  /**
   * Gets the current optimization level being used.
   *
   * @return optimization level
   */
  OptimizationLevel getOptimizationLevel();

  /**
   * Checks if JIT compilation is active.
   *
   * @return true if JIT compilation is active
   */
  boolean isJitActive();

  /**
   * Gets the current CPU usage percentage.
   *
   * @return CPU usage percentage (0.0 to 100.0)
   */
  double getCpuUsage();

  /**
   * Gets the current memory pressure level.
   *
   * @return memory pressure level
   */
  MemoryPressureLevel getMemoryPressure();

  /**
   * Gets the number of active WebAssembly instances.
   *
   * @return active instance count
   */
  int getActiveInstanceCount();

  /**
   * Gets the current garbage collection state.
   *
   * @return GC state information
   */
  GCState getGCState();

  /**
   * Checks if the system is under thermal throttling.
   *
   * @return true if thermal throttling is active
   */
  boolean isThermalThrottling();

  /**
   * Gets additional context properties.
   *
   * @return context properties map
   */
  java.util.Map<String, Object> getProperties();
}
