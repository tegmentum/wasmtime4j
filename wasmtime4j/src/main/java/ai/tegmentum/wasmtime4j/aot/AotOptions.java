package ai.tegmentum.wasmtime4j.aot;

import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.serialization.OptimizationLevel;
import java.util.Set;

/**
 * Configuration options for Ahead-of-Time (AOT) compilation.
 *
 * <p>AotOptions control various aspects of the AOT compilation process including optimization
 * levels, runtime features, and platform-specific settings. These options affect both the
 * compilation process and the runtime characteristics of the resulting compiled module.
 *
 * @since 1.0.0
 */
public interface AotOptions {

  /**
   * Gets the optimization level to apply during AOT compilation.
   *
   * <p>The optimization level affects compilation time, code size, and runtime performance.
   * Higher optimization levels typically produce faster code but increase compilation time.
   *
   * @return the optimization level
   */
  OptimizationLevel getOptimizationLevel();

  /**
   * Checks if interrupt handling should be enabled in the compiled module.
   *
   * <p>Enabling interrupts allows the WebAssembly execution to be interrupted for control flow
   * purposes, but adds overhead to the compiled code.
   *
   * @return true if interrupts should be enabled, false otherwise
   */
  boolean isEnableInterrupts();

  /**
   * Checks if fuel consumption tracking should be enabled.
   *
   * <p>Fuel consumption provides a mechanism to limit execution time and resources. Enabling fuel
   * tracking adds overhead but allows fine-grained control over execution limits.
   *
   * @return true if fuel consumption should be enabled, false otherwise
   */
  boolean isEnableFuelConsumption();

  /**
   * Gets the set of WebAssembly features that should be enabled during compilation.
   *
   * <p>Different WebAssembly features affect the compilation process and the capabilities of the
   * resulting module. Only features supported by the target runtime should be enabled.
   *
   * @return the set of enabled WebAssembly features
   */
  Set<WasmFeature> getEnabledFeatures();

  /**
   * Checks if debug information should be preserved in the compiled module.
   *
   * <p>Debug information enables better error reporting and debugging capabilities but increases
   * the size of the compiled module.
   *
   * @return true if debug information should be preserved, false otherwise
   */
  boolean isPreserveDebugInfo();

  /**
   * Checks if stack probes should be enabled for stack overflow detection.
   *
   * <p>Stack probes add safety checks to prevent stack overflow but introduce runtime overhead.
   * This is recommended for production environments where safety is critical.
   *
   * @return true if stack probes should be enabled, false otherwise
   */
  boolean isEnableStackProbes();

  /**
   * Checks if bounds checking should be enabled for memory accesses.
   *
   * <p>Bounds checking ensures memory safety by validating all memory accesses, but adds runtime
   * overhead. Disabling bounds checking can improve performance but reduces safety.
   *
   * @return true if bounds checking should be enabled, false otherwise
   */
  boolean isEnableBoundsChecking();

  /**
   * Gets the maximum number of locals allowed in functions.
   *
   * <p>This limit prevents compilation of functions with excessive local variables that could
   * cause performance issues or stack overflow.
   *
   * @return the maximum number of locals, or -1 for no limit
   */
  int getMaxLocals();

  /**
   * Gets the maximum number of function parameters allowed.
   *
   * <p>This limit prevents compilation of functions with excessive parameters that could cause
   * performance issues or calling convention problems.
   *
   * @return the maximum number of parameters, or -1 for no limit
   */
  int getMaxParams();

  /**
   * Gets the maximum number of function returns allowed.
   *
   * <p>This limit applies to multi-value returns in WebAssembly functions.
   *
   * @return the maximum number of returns, or -1 for no limit
   */
  int getMaxReturns();

  /**
   * Gets the maximum memory size allowed in bytes.
   *
   * <p>This limit prevents modules from allocating excessive memory that could cause system
   * instability.
   *
   * @return the maximum memory size in bytes, or -1 for no limit
   */
  long getMaxMemorySize();

  /**
   * Gets the maximum number of table elements allowed.
   *
   * <p>This limit applies to WebAssembly tables used for function references and other purposes.
   *
   * @return the maximum number of table elements, or -1 for no limit
   */
  int getMaxTableElements();

  /**
   * Checks if epoch-based interruption should be enabled.
   *
   * <p>Epoch interruption provides a mechanism to interrupt long-running WebAssembly execution
   * based on time or other criteria.
   *
   * @return true if epoch interruption should be enabled, false otherwise
   */
  boolean isEnableEpochInterruption();

  /**
   * Creates a builder for constructing AotOptions.
   *
   * @return a new AotOptions builder
   */
  static AotOptionsBuilder builder() {
    return new AotOptionsBuilder();
  }

  /**
   * Creates AotOptions with default settings.
   *
   * <p>Default settings provide reasonable values for most use cases: basic optimization, enabled
   * safety features, standard limits.
   *
   * @return AotOptions with default settings
   */
  static AotOptions defaults() {
    return builder().build();
  }

  /**
   * Creates AotOptions optimized for production use.
   *
   * <p>Production settings prioritize performance and safety: higher optimization levels, enabled
   * safety features, reasonable limits.
   *
   * @return AotOptions optimized for production
   */
  static AotOptions production() {
    return builder()
        .optimizationLevel(OptimizationLevel.SPEED)
        .enableStackProbes()
        .enableBoundsChecking()
        .build();
  }

  /**
   * Creates AotOptions optimized for development use.
   *
   * <p>Development settings prioritize debugging and compilation speed: lower optimization,
   * preserved debug info, relaxed limits.
   *
   * @return AotOptions optimized for development
   */
  static AotOptions development() {
    return builder()
        .optimizationLevel(OptimizationLevel.BASIC)
        .preserveDebugInfo()
        .enableInterrupts()
        .build();
  }
}