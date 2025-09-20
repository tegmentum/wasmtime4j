package ai.tegmentum.wasmtime4j.aot;

import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.serialization.OptimizationLevel;
import ai.tegmentum.wasmtime4j.serialization.TargetPlatform;
import java.util.Set;

/**
 * Information about the AOT compiler capabilities and configuration.
 *
 * <p>AotCompilerInfo provides details about the compiler version, supported features, and
 * platform capabilities to help with compatibility checking and feature detection.
 *
 * @since 1.0.0
 */
public interface AotCompilerInfo {

  /**
   * Gets the version of the AOT compiler.
   *
   * @return the compiler version string
   */
  String getVersion();

  /**
   * Gets the version of Wasmtime used by the compiler.
   *
   * @return the Wasmtime version string
   */
  String getWasmtimeVersion();

  /**
   * Gets the supported target platforms for AOT compilation.
   *
   * @return the set of supported target platforms
   */
  Set<TargetPlatform> getSupportedPlatforms();

  /**
   * Gets the supported WebAssembly features for AOT compilation.
   *
   * @return the set of supported WebAssembly features
   */
  Set<WasmFeature> getSupportedFeatures();

  /**
   * Gets the supported optimization levels.
   *
   * @return the set of supported optimization levels
   */
  Set<OptimizationLevel> getSupportedOptimizationLevels();

  /**
   * Checks if the compiler supports cross-compilation.
   *
   * <p>Cross-compilation allows compiling for target platforms different from the current
   * platform.
   *
   * @return true if cross-compilation is supported, false otherwise
   */
  boolean supportsCrossCompilation();

  /**
   * Checks if the compiler supports debug information preservation.
   *
   * @return true if debug information is supported, false otherwise
   */
  boolean supportsDebugInfo();

  /**
   * Checks if the compiler supports fuel consumption tracking.
   *
   * @return true if fuel consumption is supported, false otherwise
   */
  boolean supportsFuelConsumption();

  /**
   * Checks if the compiler supports epoch-based interruption.
   *
   * @return true if epoch interruption is supported, false otherwise
   */
  boolean supportsEpochInterruption();

  /**
   * Gets the maximum number of locals supported per function.
   *
   * @return the maximum number of locals, or -1 if unlimited
   */
  int getMaxLocalsSupported();

  /**
   * Gets the maximum number of parameters supported per function.
   *
   * @return the maximum number of parameters, or -1 if unlimited
   */
  int getMaxParamsSupported();

  /**
   * Gets the maximum memory size supported in bytes.
   *
   * @return the maximum memory size, or -1 if unlimited
   */
  long getMaxMemorySizeSupported();

  /**
   * Gets additional compiler information as a formatted string.
   *
   * @return additional compiler information
   */
  String getAdditionalInfo();
}