/*
 * Copyright 2024 Tegmentum Technology, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.experimental;

import ai.tegmentum.wasmtime4j.PlatformDetector;
import java.util.EnumSet;
import java.util.Set;

/**
 * Relaxed SIMD operations for platform-specific optimizations.
 *
 * <p><strong>EXPERIMENTAL:</strong> This API is experimental and subject to change. It provides
 * access to relaxed SIMD operations that allow for platform-specific optimizations and may produce
 * slightly different results across different hardware.
 *
 * @since 1.0.0
 */
@ExperimentalApi(feature = ExperimentalFeatures.Feature.RELAXED_SIMD)
public final class RelaxedSimd {

  /** Relaxed SIMD operation types. */
  public enum RelaxedOperation {
    /** Relaxed floating-point multiply-add. */
    RELAXED_FMADD,

    /** Relaxed floating-point multiply-subtract. */
    RELAXED_FMSUB,

    /** Relaxed integer dot product. */
    RELAXED_DOT_PRODUCT,

    /** Relaxed floating-point minimum. */
    RELAXED_MIN,

    /** Relaxed floating-point maximum. */
    RELAXED_MAX,

    /** Relaxed lane selection. */
    RELAXED_LANESELECT,

    /** Relaxed swizzle operation. */
    RELAXED_SWIZZLE
  }

  /** Platform-specific SIMD capabilities. */
  public enum PlatformCapability {
    /** Advanced Vector Extensions (AVX) on x86. */
    AVX,

    /** AVX2 support. */
    AVX2,

    /** AVX-512 support. */
    AVX512,

    /** NEON support on ARM. */
    NEON,

    /** Scalable Vector Extension (SVE) on ARM. */
    SVE,

    /** Vector Extension Facility on s390x. */
    VECTOR_FACILITY
  }

  /** Relaxed SIMD configuration. */
  public static final class RelaxedSimdConfig {
    private final Set<RelaxedOperation> enabledOperations;
    private final Set<PlatformCapability> enabledCapabilities;
    private final boolean allowPlatformSpecificResults;
    private final boolean validateOperands;
    private final boolean enableFallbackImplementations;

    private RelaxedSimdConfig(final Builder builder) {
      this.enabledOperations = EnumSet.copyOf(builder.enabledOperations);
      this.enabledCapabilities = EnumSet.copyOf(builder.enabledCapabilities);
      this.allowPlatformSpecificResults = builder.allowPlatformSpecificResults;
      this.validateOperands = builder.validateOperands;
      this.enableFallbackImplementations = builder.enableFallbackImplementations;
    }

    /**
     * Gets the enabled relaxed operations.
     *
     * @return immutable set of enabled operations
     */
    public Set<RelaxedOperation> getEnabledOperations() {
      return EnumSet.copyOf(enabledOperations);
    }

    /**
     * Gets the enabled platform capabilities.
     *
     * @return immutable set of enabled capabilities
     */
    public Set<PlatformCapability> getEnabledCapabilities() {
      return EnumSet.copyOf(enabledCapabilities);
    }

    /**
     * Checks if platform-specific results are allowed.
     *
     * @return true if platform-specific results are allowed
     */
    public boolean isPlatformSpecificResultsAllowed() {
      return allowPlatformSpecificResults;
    }

    /**
     * Checks if operand validation is enabled.
     *
     * @return true if operand validation is enabled
     */
    public boolean isOperandValidationEnabled() {
      return validateOperands;
    }

    /**
     * Checks if fallback implementations are enabled.
     *
     * @return true if fallback implementations are enabled
     */
    public boolean isFallbackImplementationsEnabled() {
      return enableFallbackImplementations;
    }

    /**
     * Checks if a specific operation is enabled.
     *
     * @param operation the operation to check
     * @return true if the operation is enabled
     */
    public boolean isOperationEnabled(final RelaxedOperation operation) {
      return enabledOperations.contains(operation);
    }

    /**
     * Checks if a specific capability is enabled.
     *
     * @param capability the capability to check
     * @return true if the capability is enabled
     */
    public boolean isCapabilityEnabled(final PlatformCapability capability) {
      return enabledCapabilities.contains(capability);
    }

    /**
     * Creates a new builder for relaxed SIMD configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
      return new Builder();
    }

    /** Builder for relaxed SIMD configuration. */
    public static final class Builder {
      private final Set<RelaxedOperation> enabledOperations =
          EnumSet.noneOf(RelaxedOperation.class);
      private final Set<PlatformCapability> enabledCapabilities =
          EnumSet.noneOf(PlatformCapability.class);
      private boolean allowPlatformSpecificResults = true;
      private boolean validateOperands = true;
      private boolean enableFallbackImplementations = true;

      private Builder() {}

      /**
       * Enables a relaxed operation.
       *
       * @param operation the operation to enable
       * @return this builder
       * @throws IllegalArgumentException if operation is null
       */
      public Builder enableOperation(final RelaxedOperation operation) {
        if (operation == null) {
          throw new IllegalArgumentException("Operation cannot be null");
        }
        enabledOperations.add(operation);
        return this;
      }

      /**
       * Enables all relaxed operations.
       *
       * @return this builder
       */
      public Builder enableAllOperations() {
        enabledOperations.addAll(EnumSet.allOf(RelaxedOperation.class));
        return this;
      }

      /**
       * Enables a platform capability.
       *
       * @param capability the capability to enable
       * @return this builder
       * @throws IllegalArgumentException if capability is null
       */
      public Builder enableCapability(final PlatformCapability capability) {
        if (capability == null) {
          throw new IllegalArgumentException("Capability cannot be null");
        }
        enabledCapabilities.add(capability);
        return this;
      }

      /**
       * Auto-detects and enables available platform capabilities.
       *
       * @return this builder
       */
      public Builder autoDetectCapabilities() {
        final PlatformDetector.Architecture arch = PlatformDetector.detectArchitecture();

        switch (arch) {
          case X86_64:
            // Add x86 capabilities based on runtime detection
            enabledCapabilities.add(PlatformCapability.AVX);
            enabledCapabilities.add(PlatformCapability.AVX2);
            break;
          case AARCH64:
            // Add ARM capabilities
            enabledCapabilities.add(PlatformCapability.NEON);
            break;
          default:
            // No specific capabilities for other architectures
            break;
        }
        return this;
      }

      /**
       * Sets whether platform-specific results are allowed.
       *
       * @param allow true to allow, false to disallow
       * @return this builder
       */
      public Builder allowPlatformSpecificResults(final boolean allow) {
        this.allowPlatformSpecificResults = allow;
        return this;
      }

      /**
       * Sets whether operand validation is enabled.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder validateOperands(final boolean enable) {
        this.validateOperands = enable;
        return this;
      }

      /**
       * Sets whether fallback implementations are enabled.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder enableFallbackImplementations(final boolean enable) {
        this.enableFallbackImplementations = enable;
        return this;
      }

      /**
       * Builds the relaxed SIMD configuration.
       *
       * @return the configuration
       */
      public RelaxedSimdConfig build() {
        return new RelaxedSimdConfig(this);
      }
    }
  }

  /** Relaxed SIMD operation result. */
  public static final class RelaxedResult {
    private final SimdOperations.V128 result;
    private final boolean isPlatformOptimized;
    private final PlatformCapability usedCapability;

    /**
     * Creates a new relaxed SIMD result.
     *
     * @param result the result vector
     * @param isPlatformOptimized true if platform optimization was used
     * @param usedCapability the platform capability used (may be null)
     */
    public RelaxedResult(
        final SimdOperations.V128 result,
        final boolean isPlatformOptimized,
        final PlatformCapability usedCapability) {
      this.result = result;
      this.isPlatformOptimized = isPlatformOptimized;
      this.usedCapability = usedCapability;
    }

    /**
     * Gets the result vector.
     *
     * @return the result vector
     */
    public SimdOperations.V128 getResult() {
      return result;
    }

    /**
     * Checks if platform optimization was used.
     *
     * @return true if platform optimization was used
     */
    public boolean isPlatformOptimized() {
      return isPlatformOptimized;
    }

    /**
     * Gets the platform capability that was used.
     *
     * @return the platform capability (may be null if no specific capability was used)
     */
    public PlatformCapability getUsedCapability() {
      return usedCapability;
    }
  }

  private final RelaxedSimdConfig config;
  private final long nativeHandle;

  /**
   * Creates a new relaxed SIMD handler.
   *
   * @param config the relaxed SIMD configuration
   * @throws IllegalArgumentException if config is null
   * @throws UnsupportedOperationException if relaxed SIMD feature is not enabled
   */
  public RelaxedSimd(final RelaxedSimdConfig config) {
    ExperimentalFeatures.validateFeatureSupport(ExperimentalFeatures.Feature.RELAXED_SIMD);

    if (config == null) {
      throw new IllegalArgumentException("Relaxed SIMD config cannot be null");
    }

    this.config = config;
    this.nativeHandle = createNativeRelaxedSimdHandler(config);
  }

  /**
   * Performs relaxed floating-point multiply-add operation.
   *
   * @param a the first operand
   * @param b the second operand
   * @param c the third operand (addend)
   * @return the relaxed result
   * @throws IllegalArgumentException if any operand is null
   * @throws UnsupportedOperationException if operation is not enabled
   */
  public RelaxedResult relaxedFmadd(
      final SimdOperations.V128 a, final SimdOperations.V128 b, final SimdOperations.V128 c) {
    validateOperation(RelaxedOperation.RELAXED_FMADD);
    validateOperands(a, b, c);

    final Object[] result = relaxedFmaddNative(nativeHandle, a.getData(), b.getData(), c.getData());
    return createRelaxedResult(result);
  }

  /**
   * Performs relaxed integer dot product operation.
   *
   * @param a the first operand
   * @param b the second operand
   * @return the relaxed result
   * @throws IllegalArgumentException if any operand is null
   * @throws UnsupportedOperationException if operation is not enabled
   */
  public RelaxedResult relaxedDotProduct(final SimdOperations.V128 a, final SimdOperations.V128 b) {
    validateOperation(RelaxedOperation.RELAXED_DOT_PRODUCT);
    validateOperands(a, b);

    final Object[] result = relaxedDotProductNative(nativeHandle, a.getData(), b.getData());
    return createRelaxedResult(result);
  }

  /**
   * Performs relaxed lane selection operation.
   *
   * @param condition the condition vector
   * @param a the first value vector
   * @param b the second value vector
   * @return the relaxed result
   * @throws IllegalArgumentException if any operand is null
   * @throws UnsupportedOperationException if operation is not enabled
   */
  public RelaxedResult relaxedLaneSelect(
      final SimdOperations.V128 condition,
      final SimdOperations.V128 a,
      final SimdOperations.V128 b) {
    validateOperation(RelaxedOperation.RELAXED_LANESELECT);
    validateOperands(condition, a, b);

    final Object[] result =
        relaxedLaneSelectNative(nativeHandle, condition.getData(), a.getData(), b.getData());
    return createRelaxedResult(result);
  }

  /**
   * Checks if platform-specific optimizations are available for an operation.
   *
   * @param operation the operation to check
   * @return true if platform optimizations are available
   * @throws IllegalArgumentException if operation is null
   */
  public boolean isPlatformOptimized(final RelaxedOperation operation) {
    if (operation == null) {
      throw new IllegalArgumentException("Operation cannot be null");
    }

    return isPlatformOptimizedNative(nativeHandle, operation);
  }

  /**
   * Gets the available platform capabilities.
   *
   * @return set of available platform capabilities
   */
  public Set<PlatformCapability> getAvailableCapabilities() {
    final PlatformCapability[] capabilities = getAvailableCapabilitiesNative(nativeHandle);
    return EnumSet.of(capabilities[0], capabilities);
  }

  /**
   * Validates that an operation is enabled.
   *
   * @param operation the operation to validate
   * @throws UnsupportedOperationException if operation is not enabled
   */
  private void validateOperation(final RelaxedOperation operation) {
    if (!config.isOperationEnabled(operation)) {
      throw new UnsupportedOperationException("Relaxed operation " + operation + " is not enabled");
    }
  }

  /**
   * Validates operands if validation is enabled.
   *
   * @param operands the operands to validate
   * @throws IllegalArgumentException if any operand is null and validation is enabled
   */
  private void validateOperands(final SimdOperations.V128... operands) {
    if (config.isOperandValidationEnabled()) {
      for (final SimdOperations.V128 operand : operands) {
        if (operand == null) {
          throw new IllegalArgumentException("SIMD operand cannot be null");
        }
      }
    }
  }

  /**
   * Creates a relaxed result from native return values.
   *
   * @param nativeResult the native result array
   * @return the relaxed result
   */
  private RelaxedResult createRelaxedResult(final Object[] nativeResult) {
    final byte[] resultData = (byte[]) nativeResult[0];
    final boolean isPlatformOptimized = (boolean) nativeResult[1];
    final PlatformCapability usedCapability = (PlatformCapability) nativeResult[2];

    return new RelaxedResult(
        new SimdOperations.V128(resultData), isPlatformOptimized, usedCapability);
  }

  /**
   * Gets the relaxed SIMD configuration.
   *
   * @return the configuration
   */
  public RelaxedSimdConfig getConfig() {
    return config;
  }

  /**
   * Gets the native handle for this relaxed SIMD handler.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  /** Closes this relaxed SIMD handler and releases native resources. */
  public void close() {
    if (nativeHandle != 0) {
      closeNativeRelaxedSimdHandler(nativeHandle);
    }
  }

  // Native method declarations - implementations in wasmtime4j-native
  private static native long createNativeRelaxedSimdHandler(RelaxedSimdConfig config);

  private static native Object[] relaxedFmaddNative(
      long handlerHandle, byte[] a, byte[] b, byte[] c);

  private static native Object[] relaxedDotProductNative(long handlerHandle, byte[] a, byte[] b);

  private static native Object[] relaxedLaneSelectNative(
      long handlerHandle, byte[] condition, byte[] a, byte[] b);

  private static native boolean isPlatformOptimizedNative(
      long handlerHandle, RelaxedOperation operation);

  private static native PlatformCapability[] getAvailableCapabilitiesNative(long handlerHandle);

  private static native void closeNativeRelaxedSimdHandler(long handle);

  @Override
  protected void finalize() throws Throwable {
    try {
      close();
    } finally {
      super.finalize();
    }
  }
}
