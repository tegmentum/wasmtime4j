package ai.tegmentum.wasmtime4j;

import java.util.Set;

/**
 * SIMD (Single Instruction, Multiple Data) support for WebAssembly vector operations.
 *
 * <p>This interface provides access to WebAssembly SIMD instructions that enable high-performance
 * vector operations on 128-bit vectors. SIMD operations are essential for applications requiring
 * intensive data processing, mathematical computations, or multimedia processing.
 *
 * <p>All operations include comprehensive validation to ensure SIMD support is available and
 * operations are performed safely within the WebAssembly runtime.
 *
 * @since 1.0.0
 */
public interface SimdSupport {

  /**
   * Checks if SIMD instructions are supported by the current runtime and platform.
   *
   * @return true if SIMD operations are supported, false otherwise
   */
  boolean isSimdSupported();

  /**
   * Gets the set of SIMD instructions supported by the current runtime.
   *
   * @return an immutable set of supported SIMD instructions, never null
   * @throws RuntimeException if unable to determine supported instructions
   */
  Set<SimdInstruction> getSupportedSimdInstructions();

  /**
   * Checks if 128-bit vector operations are supported.
   *
   * @return true if v128 vector type is supported, false otherwise
   */
  boolean supportsV128();

  /**
   * Checks if 8x16 integer vector operations are supported.
   *
   * @return true if i8x16 vector operations are supported, false otherwise
   */
  boolean supportsI8x16();

  /**
   * Checks if 16x8 integer vector operations are supported.
   *
   * @return true if i16x8 vector operations are supported, false otherwise
   */
  boolean supportsI16x8();

  /**
   * Checks if 32x4 integer vector operations are supported.
   *
   * @return true if i32x4 vector operations are supported, false otherwise
   */
  boolean supportsI32x4();

  /**
   * Checks if 64x2 integer vector operations are supported.
   *
   * @return true if i64x2 vector operations are supported, false otherwise
   */
  boolean supportsI64x2();

  /**
   * Checks if 32x4 floating-point vector operations are supported.
   *
   * @return true if f32x4 vector operations are supported, false otherwise
   */
  boolean supportsF32x4();

  /**
   * Checks if 64x2 floating-point vector operations are supported.
   *
   * @return true if f64x2 vector operations are supported, false otherwise
   */
  boolean supportsF64x2();

  /**
   * Enables specific SIMD instructions for module compilation.
   *
   * @param instructions the set of SIMD instructions to enable
   * @throws IllegalArgumentException if instructions is null or contains unsupported instructions
   * @throws RuntimeException if unable to enable the specified instructions
   */
  void enableSimdInstructions(final Set<SimdInstruction> instructions);

  /**
   * Disables specific SIMD instructions for module compilation.
   *
   * @param instructions the set of SIMD instructions to disable
   * @throws IllegalArgumentException if instructions is null
   * @throws RuntimeException if unable to disable the specified instructions
   */
  void disableSimdInstructions(final Set<SimdInstruction> instructions);

  /**
   * Validates a WebAssembly module for SIMD instruction usage.
   *
   * @param wasmBytes the WebAssembly module bytecode
   * @return validation result containing SIMD usage information
   * @throws IllegalArgumentException if wasmBytes is null or empty
   * @throws RuntimeException if validation fails
   */
  ValidationResult validateSimdModule(final byte[] wasmBytes);

  /**
   * Gets the platform-specific SIMD optimization level.
   *
   * @return the optimization level for SIMD operations
   */
  SimdOptimizationLevel getSimdOptimizationLevel();

  /**
   * Sets the platform-specific SIMD optimization level.
   *
   * @param level the optimization level to use for SIMD operations
   * @throws IllegalArgumentException if level is null
   * @throws RuntimeException if unable to set the optimization level
   */
  void setSimdOptimizationLevel(final SimdOptimizationLevel level);

  /**
   * Checks if a specific SIMD instruction is supported.
   *
   * @param instruction the SIMD instruction to check
   * @return true if the instruction is supported, false otherwise
   * @throws IllegalArgumentException if instruction is null
   */
  boolean isInstructionSupported(final SimdInstruction instruction);

  /**
   * Gets the native SIMD feature flags for the current platform.
   *
   * @return a set of native SIMD feature flags
   */
  Set<String> getNativeSimdFeatures();

  /** Enum representing SIMD optimization levels. */
  enum SimdOptimizationLevel {
    /** No SIMD optimization. */
    NONE,
    /** Basic SIMD optimization. */
    BASIC,
    /** Aggressive SIMD optimization. */
    AGGRESSIVE,
    /** Platform-specific optimal SIMD optimization. */
    NATIVE
  }

  /** Validation result for SIMD module validation. */
  final class ValidationResult {
    private final boolean isValid;
    private final boolean usesSimd;
    private final Set<SimdInstruction> detectedInstructions;
    private final Set<String> errors;

    /**
     * Creates a new validation result.
     *
     * @param isValid whether the module is valid
     * @param usesSimd whether the module uses SIMD instructions
     * @param detectedInstructions the SIMD instructions detected in the module
     * @param errors any validation errors
     */
    public ValidationResult(
        final boolean isValid,
        final boolean usesSimd,
        final Set<SimdInstruction> detectedInstructions,
        final Set<String> errors) {
      this.isValid = isValid;
      this.usesSimd = usesSimd;
      this.detectedInstructions = Set.copyOf(detectedInstructions);
      this.errors = Set.copyOf(errors);
    }

    /**
     * Checks if the module is valid.
     *
     * @return true if the module is valid, false otherwise
     */
    public boolean isValid() {
      return isValid;
    }

    /**
     * Checks if the module uses SIMD instructions.
     *
     * @return true if the module uses SIMD instructions, false otherwise
     */
    public boolean usesSimd() {
      return usesSimd;
    }

    /**
     * Gets the SIMD instructions detected in the module.
     *
     * @return an immutable set of detected SIMD instructions
     */
    public Set<SimdInstruction> getDetectedInstructions() {
      return detectedInstructions;
    }

    /**
     * Gets any validation errors.
     *
     * @return an immutable set of validation errors
     */
    public Set<String> getErrors() {
      return errors;
    }
  }
}
