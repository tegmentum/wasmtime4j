package ai.tegmentum.wasmtime4j.aot.impl;

import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.aot.AotOptions;
import ai.tegmentum.wasmtime4j.serialization.OptimizationLevel;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Default implementation of AotOptions.
 *
 * <p>This implementation provides immutable AOT compilation configuration with validation and
 * consistent behavior across all use cases.
 *
 * @since 1.0.0
 */
public final class AotOptionsImpl implements AotOptions {

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
  private final long maxTableElements;
  private final boolean enableEpochInterruption;

  /**
   * Creates a new AotOptionsImpl.
   *
   * @param optimizationLevel the optimization level to apply
   * @param enableInterrupts whether to enable interrupt handling
   * @param enableFuelConsumption whether to enable fuel consumption tracking
   * @param enabledFeatures the set of WebAssembly features to enable
   * @param preserveDebugInfo whether to preserve debug information
   * @param enableStackProbes whether to enable stack probes
   * @param enableBoundsChecking whether to enable bounds checking
   * @param maxLocals maximum number of local variables
   * @param maxParams maximum number of function parameters
   * @param maxReturns maximum number of function return values
   * @param maxMemorySize maximum memory size in bytes
   * @param maxTableElements maximum number of table elements
   * @param enableEpochInterruption whether to enable epoch-based interruption
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public AotOptionsImpl(
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
      final long maxTableElements,
      final boolean enableEpochInterruption) {

    this.optimizationLevel = Objects.requireNonNull(optimizationLevel, "Optimization level cannot be null");
    this.enableInterrupts = enableInterrupts;
    this.enableFuelConsumption = enableFuelConsumption;
    this.enabledFeatures = Collections.unmodifiableSet(
        Objects.requireNonNull(enabledFeatures, "Enabled features set cannot be null"));
    this.preserveDebugInfo = preserveDebugInfo;
    this.enableStackProbes = enableStackProbes;
    this.enableBoundsChecking = enableBoundsChecking;

    if (maxLocals < 0) {
      throw new IllegalArgumentException("Max locals must be non-negative: " + maxLocals);
    }
    this.maxLocals = maxLocals;

    if (maxParams < 0) {
      throw new IllegalArgumentException("Max params must be non-negative: " + maxParams);
    }
    this.maxParams = maxParams;

    if (maxReturns < 0) {
      throw new IllegalArgumentException("Max returns must be non-negative: " + maxReturns);
    }
    this.maxReturns = maxReturns;

    if (maxMemorySize < 0) {
      throw new IllegalArgumentException("Max memory size must be non-negative: " + maxMemorySize);
    }
    this.maxMemorySize = maxMemorySize;

    if (maxTableElements < 0) {
      throw new IllegalArgumentException("Max table elements must be non-negative: " + maxTableElements);
    }
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
  public long getMaxTableElements() {
    return maxTableElements;
  }

  @Override
  public boolean isEnableEpochInterruption() {
    return enableEpochInterruption;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final AotOptionsImpl other = (AotOptionsImpl) obj;
    return enableInterrupts == other.enableInterrupts
        && enableFuelConsumption == other.enableFuelConsumption
        && preserveDebugInfo == other.preserveDebugInfo
        && enableStackProbes == other.enableStackProbes
        && enableBoundsChecking == other.enableBoundsChecking
        && maxLocals == other.maxLocals
        && maxParams == other.maxParams
        && maxReturns == other.maxReturns
        && maxMemorySize == other.maxMemorySize
        && maxTableElements == other.maxTableElements
        && enableEpochInterruption == other.enableEpochInterruption
        && optimizationLevel == other.optimizationLevel
        && Objects.equals(enabledFeatures, other.enabledFeatures);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        optimizationLevel,
        enableInterrupts,
        enableFuelConsumption,
        enabledFeatures,
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

  @Override
  public String toString() {
    return "AotOptions{"
        + "optimizationLevel=" + optimizationLevel
        + ", enableInterrupts=" + enableInterrupts
        + ", enableFuelConsumption=" + enableFuelConsumption
        + ", enabledFeatures=" + enabledFeatures
        + ", preserveDebugInfo=" + preserveDebugInfo
        + ", enableStackProbes=" + enableStackProbes
        + ", enableBoundsChecking=" + enableBoundsChecking
        + ", maxLocals=" + maxLocals
        + ", maxParams=" + maxParams
        + ", maxReturns=" + maxReturns
        + ", maxMemorySize=" + maxMemorySize
        + ", maxTableElements=" + maxTableElements
        + ", enableEpochInterruption=" + enableEpochInterruption
        + '}';
  }
}