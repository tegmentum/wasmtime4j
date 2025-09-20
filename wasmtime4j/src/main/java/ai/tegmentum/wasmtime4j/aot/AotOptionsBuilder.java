package ai.tegmentum.wasmtime4j.aot;

import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.aot.impl.AotOptionsImpl;
import ai.tegmentum.wasmtime4j.serialization.OptimizationLevel;
import java.util.HashSet;
import java.util.Set;

/**
 * Builder for constructing AotOptions instances.
 *
 * <p>This builder provides a fluent interface for configuring AOT compilation options with
 * validation and sensible defaults.
 *
 * @since 1.0.0
 */
public final class AotOptionsBuilder {

  private OptimizationLevel optimizationLevel = OptimizationLevel.BASIC;
  private boolean enableInterrupts = false;
  private boolean enableFuelConsumption = false;
  private Set<WasmFeature> enabledFeatures = new HashSet<>();
  private boolean preserveDebugInfo = false;
  private boolean enableStackProbes = true;
  private boolean enableBoundsChecking = true;
  private int maxLocals = -1;
  private int maxParams = -1;
  private int maxReturns = -1;
  private long maxMemorySize = -1;
  private int maxTableElements = -1;
  private boolean enableEpochInterruption = false;

  AotOptionsBuilder() {
    // Package-private constructor
  }

  /**
   * Sets the optimization level for AOT compilation.
   *
   * @param optimizationLevel the optimization level to use
   * @return this builder for method chaining
   * @throws IllegalArgumentException if optimizationLevel is null
   */
  public AotOptionsBuilder optimizationLevel(final OptimizationLevel optimizationLevel) {
    if (optimizationLevel == null) {
      throw new IllegalArgumentException("Optimization level cannot be null");
    }
    this.optimizationLevel = optimizationLevel;
    return this;
  }

  /**
   * Enables or disables interrupt handling.
   *
   * @param enableInterrupts true to enable interrupts, false otherwise
   * @return this builder for method chaining
   */
  public AotOptionsBuilder enableInterrupts(final boolean enableInterrupts) {
    this.enableInterrupts = enableInterrupts;
    return this;
  }

  /**
   * Enables interrupt handling.
   *
   * @return this builder for method chaining
   */
  public AotOptionsBuilder enableInterrupts() {
    return enableInterrupts(true);
  }

  /**
   * Enables or disables fuel consumption tracking.
   *
   * @param enableFuelConsumption true to enable fuel consumption, false otherwise
   * @return this builder for method chaining
   */
  public AotOptionsBuilder enableFuelConsumption(final boolean enableFuelConsumption) {
    this.enableFuelConsumption = enableFuelConsumption;
    return this;
  }

  /**
   * Enables fuel consumption tracking.
   *
   * @return this builder for method chaining
   */
  public AotOptionsBuilder enableFuelConsumption() {
    return enableFuelConsumption(true);
  }

  /**
   * Sets the WebAssembly features to enable.
   *
   * @param enabledFeatures the features to enable
   * @return this builder for method chaining
   * @throws IllegalArgumentException if enabledFeatures is null
   */
  public AotOptionsBuilder enabledFeatures(final Set<WasmFeature> enabledFeatures) {
    if (enabledFeatures == null) {
      throw new IllegalArgumentException("Enabled features set cannot be null");
    }
    this.enabledFeatures = new HashSet<>(enabledFeatures);
    return this;
  }

  /**
   * Adds a WebAssembly feature to enable.
   *
   * @param feature the feature to enable
   * @return this builder for method chaining
   * @throws IllegalArgumentException if feature is null
   */
  public AotOptionsBuilder enableFeature(final WasmFeature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Feature cannot be null");
    }
    this.enabledFeatures.add(feature);
    return this;
  }

  /**
   * Removes a WebAssembly feature.
   *
   * @param feature the feature to disable
   * @return this builder for method chaining
   * @throws IllegalArgumentException if feature is null
   */
  public AotOptionsBuilder disableFeature(final WasmFeature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Feature cannot be null");
    }
    this.enabledFeatures.remove(feature);
    return this;
  }

  /**
   * Enables or disables preservation of debug information.
   *
   * @param preserveDebugInfo true to preserve debug info, false otherwise
   * @return this builder for method chaining
   */
  public AotOptionsBuilder preserveDebugInfo(final boolean preserveDebugInfo) {
    this.preserveDebugInfo = preserveDebugInfo;
    return this;
  }

  /**
   * Enables preservation of debug information.
   *
   * @return this builder for method chaining
   */
  public AotOptionsBuilder preserveDebugInfo() {
    return preserveDebugInfo(true);
  }

  /**
   * Enables or disables stack probes.
   *
   * @param enableStackProbes true to enable stack probes, false otherwise
   * @return this builder for method chaining
   */
  public AotOptionsBuilder enableStackProbes(final boolean enableStackProbes) {
    this.enableStackProbes = enableStackProbes;
    return this;
  }

  /**
   * Enables stack probes.
   *
   * @return this builder for method chaining
   */
  public AotOptionsBuilder enableStackProbes() {
    return enableStackProbes(true);
  }

  /**
   * Disables stack probes.
   *
   * @return this builder for method chaining
   */
  public AotOptionsBuilder disableStackProbes() {
    return enableStackProbes(false);
  }

  /**
   * Enables or disables bounds checking.
   *
   * @param enableBoundsChecking true to enable bounds checking, false otherwise
   * @return this builder for method chaining
   */
  public AotOptionsBuilder enableBoundsChecking(final boolean enableBoundsChecking) {
    this.enableBoundsChecking = enableBoundsChecking;
    return this;
  }

  /**
   * Enables bounds checking.
   *
   * @return this builder for method chaining
   */
  public AotOptionsBuilder enableBoundsChecking() {
    return enableBoundsChecking(true);
  }

  /**
   * Disables bounds checking.
   *
   * @return this builder for method chaining
   */
  public AotOptionsBuilder disableBoundsChecking() {
    return enableBoundsChecking(false);
  }

  /**
   * Sets the maximum number of locals allowed in functions.
   *
   * @param maxLocals the maximum number of locals, or -1 for no limit
   * @return this builder for method chaining
   * @throws IllegalArgumentException if maxLocals is negative and not -1
   */
  public AotOptionsBuilder maxLocals(final int maxLocals) {
    if (maxLocals < -1) {
      throw new IllegalArgumentException("Max locals must be positive or -1 for no limit");
    }
    this.maxLocals = maxLocals;
    return this;
  }

  /**
   * Sets the maximum number of function parameters allowed.
   *
   * @param maxParams the maximum number of parameters, or -1 for no limit
   * @return this builder for method chaining
   * @throws IllegalArgumentException if maxParams is negative and not -1
   */
  public AotOptionsBuilder maxParams(final int maxParams) {
    if (maxParams < -1) {
      throw new IllegalArgumentException("Max params must be positive or -1 for no limit");
    }
    this.maxParams = maxParams;
    return this;
  }

  /**
   * Sets the maximum number of function returns allowed.
   *
   * @param maxReturns the maximum number of returns, or -1 for no limit
   * @return this builder for method chaining
   * @throws IllegalArgumentException if maxReturns is negative and not -1
   */
  public AotOptionsBuilder maxReturns(final int maxReturns) {
    if (maxReturns < -1) {
      throw new IllegalArgumentException("Max returns must be positive or -1 for no limit");
    }
    this.maxReturns = maxReturns;
    return this;
  }

  /**
   * Sets the maximum memory size allowed.
   *
   * @param maxMemorySize the maximum memory size in bytes, or -1 for no limit
   * @return this builder for method chaining
   * @throws IllegalArgumentException if maxMemorySize is negative and not -1
   */
  public AotOptionsBuilder maxMemorySize(final long maxMemorySize) {
    if (maxMemorySize < -1) {
      throw new IllegalArgumentException("Max memory size must be positive or -1 for no limit");
    }
    this.maxMemorySize = maxMemorySize;
    return this;
  }

  /**
   * Sets the maximum number of table elements allowed.
   *
   * @param maxTableElements the maximum number of table elements, or -1 for no limit
   * @return this builder for method chaining
   * @throws IllegalArgumentException if maxTableElements is negative and not -1
   */
  public AotOptionsBuilder maxTableElements(final int maxTableElements) {
    if (maxTableElements < -1) {
      throw new IllegalArgumentException("Max table elements must be positive or -1 for no limit");
    }
    this.maxTableElements = maxTableElements;
    return this;
  }

  /**
   * Enables or disables epoch-based interruption.
   *
   * @param enableEpochInterruption true to enable epoch interruption, false otherwise
   * @return this builder for method chaining
   */
  public AotOptionsBuilder enableEpochInterruption(final boolean enableEpochInterruption) {
    this.enableEpochInterruption = enableEpochInterruption;
    return this;
  }

  /**
   * Enables epoch-based interruption.
   *
   * @return this builder for method chaining
   */
  public AotOptionsBuilder enableEpochInterruption() {
    return enableEpochInterruption(true);
  }

  /**
   * Builds the AotOptions instance.
   *
   * @return a new AotOptions instance with the configured settings
   */
  public AotOptions build() {
    return new AotOptionsImpl(
        optimizationLevel,
        enableInterrupts,
        enableFuelConsumption,
        new HashSet<>(enabledFeatures),
        preserveDebugInfo,
        enableStackProbes,
        enableBoundsChecking,
        maxLocals,
        maxParams,
        maxReturns,
        maxMemorySize,
        maxTableElements,
        enableEpochInterruption);
  }

  /**
   * Implementation of AotOptions.
   */
  private static final class AotOptionsImpl implements AotOptions {
    private final OptimizationLevel optimizationLevel;
    private final boolean enableInterrupts;
    private final boolean enableFuelConsumption;
    private final Set<WasmFeature> enabledFeatures;
    private final boolean preserveDebugInfo;
    private final boolean enableStackProbes;
    private final boolean enableBoundsChecking;
    private final int maxLocals;
    private final int maxParams;
    private final int maxReturns;
    private final long maxMemorySize;
    private final int maxTableElements;
    private final boolean enableEpochInterruption;

    AotOptionsImpl(
        final OptimizationLevel optimizationLevel,
        final boolean enableInterrupts,
        final boolean enableFuelConsumption,
        final Set<WasmFeature> enabledFeatures,
        final boolean preserveDebugInfo,
        final boolean enableStackProbes,
        final boolean enableBoundsChecking,
        final int maxLocals,
        final int maxParams,
        final int maxReturns,
        final long maxMemorySize,
        final int maxTableElements,
        final boolean enableEpochInterruption) {
      this.optimizationLevel = optimizationLevel;
      this.enableInterrupts = enableInterrupts;
      this.enableFuelConsumption = enableFuelConsumption;
      this.enabledFeatures = Set.copyOf(enabledFeatures);
      this.preserveDebugInfo = preserveDebugInfo;
      this.enableStackProbes = enableStackProbes;
      this.enableBoundsChecking = enableBoundsChecking;
      this.maxLocals = maxLocals;
      this.maxParams = maxParams;
      this.maxReturns = maxReturns;
      this.maxMemorySize = maxMemorySize;
      this.maxTableElements = maxTableElements;
      this.enableEpochInterruption = enableEpochInterruption;
    }

    @Override
    public OptimizationLevel getOptimizationLevel() {
      return optimizationLevel;
    }

    @Override
    public boolean isEnableInterrupts() {
      return enableInterrupts;
    }

    @Override
    public boolean isEnableFuelConsumption() {
      return enableFuelConsumption;
    }

    @Override
    public Set<WasmFeature> getEnabledFeatures() {
      return enabledFeatures;
    }

    @Override
    public boolean isPreserveDebugInfo() {
      return preserveDebugInfo;
    }

    @Override
    public boolean isEnableStackProbes() {
      return enableStackProbes;
    }

    @Override
    public boolean isEnableBoundsChecking() {
      return enableBoundsChecking;
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
    public int getMaxReturns() {
      return maxReturns;
    }

    @Override
    public long getMaxMemorySize() {
      return maxMemorySize;
    }

    @Override
    public int getMaxTableElements() {
      return maxTableElements;
    }

    @Override
    public boolean isEnableEpochInterruption() {
      return enableEpochInterruption;
    }

    @Override
    public String toString() {
      return "AotOptions{"
          + "optimizationLevel="
          + optimizationLevel
          + ", enableInterrupts="
          + enableInterrupts
          + ", enableFuelConsumption="
          + enableFuelConsumption
          + ", enabledFeatures="
          + enabledFeatures
          + ", preserveDebugInfo="
          + preserveDebugInfo
          + ", enableStackProbes="
          + enableStackProbes
          + ", enableBoundsChecking="
          + enableBoundsChecking
          + ", maxLocals="
          + maxLocals
          + ", maxParams="
          + maxParams
          + ", maxReturns="
          + maxReturns
          + ", maxMemorySize="
          + maxMemorySize
          + ", maxTableElements="
          + maxTableElements
          + ", enableEpochInterruption="
          + enableEpochInterruption
          + '}';
    }
  }
}