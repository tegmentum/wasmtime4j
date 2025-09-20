package ai.tegmentum.wasmtime4j.cache;

import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.serialization.OptimizationLevel;
import java.util.Objects;
import java.util.Set;

/**
 * Represents compilation settings that affect module compilation and caching.
 *
 * <p>CompilationSettings capture the various configuration options that influence how a
 * WebAssembly module is compiled. These settings are used as part of cache keys to ensure
 * that modules compiled with different settings are cached separately.
 *
 * @since 1.0.0
 */
public interface CompilationSettings {

  /**
   * Gets the optimization level used for compilation.
   *
   * @return the optimization level
   */
  OptimizationLevel getOptimizationLevel();

  /**
   * Gets the set of enabled WebAssembly features.
   *
   * @return the set of enabled features
   */
  Set<WasmFeature> getEnabledFeatures();

  /**
   * Checks if debug information is enabled.
   *
   * @return true if debug information is enabled, false otherwise
   */
  boolean isDebugInfoEnabled();

  /**
   * Checks if fuel consumption tracking is enabled.
   *
   * @return true if fuel consumption is enabled, false otherwise
   */
  boolean isFuelEnabled();

  /**
   * Checks if epoch interruption is enabled.
   *
   * @return true if epoch interruption is enabled, false otherwise
   */
  boolean isEpochInterruptionEnabled();

  /**
   * Gets the maximum number of locals allowed per function.
   *
   * @return the maximum locals limit, or -1 if unlimited
   */
  int getMaxLocals();

  /**
   * Gets the maximum number of parameters allowed per function.
   *
   * @return the maximum parameters limit, or -1 if unlimited
   */
  int getMaxParams();

  /**
   * Gets the maximum memory size allowed in bytes.
   *
   * @return the maximum memory size, or -1 if unlimited
   */
  long getMaxMemorySize();

  /**
   * Checks if stack probes are enabled.
   *
   * @return true if stack probes are enabled, false otherwise
   */
  boolean areStackProbesEnabled();

  /**
   * Checks if bounds checking is enabled.
   *
   * @return true if bounds checking is enabled, false otherwise
   */
  boolean isBoundsCheckingEnabled();

  /**
   * Creates a CompilationSettings instance with the given parameters.
   *
   * @param optimizationLevel the optimization level
   * @param enabledFeatures the set of enabled features
   * @param debugInfoEnabled whether debug information is enabled
   * @param fuelEnabled whether fuel consumption is enabled
   * @param epochInterruptionEnabled whether epoch interruption is enabled
   * @param maxLocals the maximum locals limit
   * @param maxParams the maximum parameters limit
   * @param maxMemorySize the maximum memory size
   * @param stackProbesEnabled whether stack probes are enabled
   * @param boundsCheckingEnabled whether bounds checking is enabled
   * @return a new CompilationSettings instance
   */
  static CompilationSettings create(
      final OptimizationLevel optimizationLevel,
      final Set<WasmFeature> enabledFeatures,
      final boolean debugInfoEnabled,
      final boolean fuelEnabled,
      final boolean epochInterruptionEnabled,
      final int maxLocals,
      final int maxParams,
      final long maxMemorySize,
      final boolean stackProbesEnabled,
      final boolean boundsCheckingEnabled) {
    return new CompilationSettingsImpl(
        optimizationLevel,
        enabledFeatures,
        debugInfoEnabled,
        fuelEnabled,
        epochInterruptionEnabled,
        maxLocals,
        maxParams,
        maxMemorySize,
        stackProbesEnabled,
        boundsCheckingEnabled);
  }

  /**
   * Creates a CompilationSettings instance with default values.
   *
   * @return a new CompilationSettings instance with default values
   */
  static CompilationSettings defaults() {
    return create(
        OptimizationLevel.BASIC,
        Set.of(),
        false,
        false,
        false,
        -1,
        -1,
        -1L,
        true,
        true);
  }

  /**
   * Implementation of CompilationSettings.
   */
  final class CompilationSettingsImpl implements CompilationSettings {
    private final OptimizationLevel optimizationLevel;
    private final Set<WasmFeature> enabledFeatures;
    private final boolean debugInfoEnabled;
    private final boolean fuelEnabled;
    private final boolean epochInterruptionEnabled;
    private final int maxLocals;
    private final int maxParams;
    private final long maxMemorySize;
    private final boolean stackProbesEnabled;
    private final boolean boundsCheckingEnabled;

    CompilationSettingsImpl(
        final OptimizationLevel optimizationLevel,
        final Set<WasmFeature> enabledFeatures,
        final boolean debugInfoEnabled,
        final boolean fuelEnabled,
        final boolean epochInterruptionEnabled,
        final int maxLocals,
        final int maxParams,
        final long maxMemorySize,
        final boolean stackProbesEnabled,
        final boolean boundsCheckingEnabled) {
      this.optimizationLevel =
          Objects.requireNonNull(optimizationLevel, "Optimization level cannot be null");
      this.enabledFeatures = Set.copyOf(enabledFeatures);
      this.debugInfoEnabled = debugInfoEnabled;
      this.fuelEnabled = fuelEnabled;
      this.epochInterruptionEnabled = epochInterruptionEnabled;
      this.maxLocals = maxLocals;
      this.maxParams = maxParams;
      this.maxMemorySize = maxMemorySize;
      this.stackProbesEnabled = stackProbesEnabled;
      this.boundsCheckingEnabled = boundsCheckingEnabled;
    }

    @Override
    public OptimizationLevel getOptimizationLevel() {
      return optimizationLevel;
    }

    @Override
    public Set<WasmFeature> getEnabledFeatures() {
      return enabledFeatures;
    }

    @Override
    public boolean isDebugInfoEnabled() {
      return debugInfoEnabled;
    }

    @Override
    public boolean isFuelEnabled() {
      return fuelEnabled;
    }

    @Override
    public boolean isEpochInterruptionEnabled() {
      return epochInterruptionEnabled;
    }

    @Override
    public int getMaxLocals() {
      return maxLocals;
    }

    @Override
    public int getMaxParams() {
      return maxParams;
    }

    @Override
    public long getMaxMemorySize() {
      return maxMemorySize;
    }

    @Override
    public boolean areStackProbesEnabled() {
      return stackProbesEnabled;
    }

    @Override
    public boolean isBoundsCheckingEnabled() {
      return boundsCheckingEnabled;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      final CompilationSettingsImpl other = (CompilationSettingsImpl) obj;
      return debugInfoEnabled == other.debugInfoEnabled
          && fuelEnabled == other.fuelEnabled
          && epochInterruptionEnabled == other.epochInterruptionEnabled
          && maxLocals == other.maxLocals
          && maxParams == other.maxParams
          && maxMemorySize == other.maxMemorySize
          && stackProbesEnabled == other.stackProbesEnabled
          && boundsCheckingEnabled == other.boundsCheckingEnabled
          && Objects.equals(optimizationLevel, other.optimizationLevel)
          && Objects.equals(enabledFeatures, other.enabledFeatures);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          optimizationLevel,
          enabledFeatures,
          debugInfoEnabled,
          fuelEnabled,
          epochInterruptionEnabled,
          maxLocals,
          maxParams,
          maxMemorySize,
          stackProbesEnabled,
          boundsCheckingEnabled);
    }

    @Override
    public String toString() {
      return "CompilationSettings{"
          + "optimizationLevel="
          + optimizationLevel
          + ", enabledFeatures="
          + enabledFeatures
          + ", debugInfoEnabled="
          + debugInfoEnabled
          + ", fuelEnabled="
          + fuelEnabled
          + ", epochInterruptionEnabled="
          + epochInterruptionEnabled
          + ", maxLocals="
          + maxLocals
          + ", maxParams="
          + maxParams
          + ", maxMemorySize="
          + maxMemorySize
          + ", stackProbesEnabled="
          + stackProbesEnabled
          + ", boundsCheckingEnabled="
          + boundsCheckingEnabled
          + '}';
    }
  }
}