package ai.tegmentum.wasmtime4j;

/**
 * Phases of WebAssembly compilation during streaming compilation.
 *
 * <p>CompilationPhase represents the different stages of WebAssembly module compilation, from
 * initial parsing through final optimization and code generation.
 *
 * @since 1.0.0
 */
public enum CompilationPhase {
  /**
   * Initial parsing of WebAssembly bytecode.
   *
   * <p>This phase validates the basic structure of the WebAssembly module and extracts module
   * metadata.
   */
  PARSING("Parsing WebAssembly bytecode", 0.1),

  /**
   * Validation of WebAssembly module structure and semantics.
   *
   * <p>This phase performs comprehensive validation of the module according to WebAssembly
   * specification.
   */
  VALIDATION("Validating module structure", 0.2),

  /**
   * Import resolution and linking preparation.
   *
   * <p>This phase prepares for import resolution and validates import requirements.
   */
  IMPORT_RESOLUTION("Resolving imports", 0.3),

  /**
   * Function signature analysis and type checking.
   *
   * <p>This phase analyzes function signatures and performs type checking across the module.
   */
  TYPE_ANALYSIS("Analyzing types", 0.4),

  /**
   * Code generation and initial compilation.
   *
   * <p>This phase generates machine code for WebAssembly functions.
   */
  CODE_GENERATION("Generating code", 0.7),

  /**
   * Code optimization and finalization.
   *
   * <p>This phase performs optimization passes on the generated code.
   */
  OPTIMIZATION("Optimizing code", 0.9),

  /**
   * Final linking and module preparation.
   *
   * <p>This phase finalizes the compiled module and prepares it for instantiation.
   */
  FINALIZATION("Finalizing module", 1.0);

  private final String description;
  private final double progressWeight;

  CompilationPhase(final String description, final double progressWeight) {
    this.description = description;
    this.progressWeight = progressWeight;
  }

  /**
   * Gets the human-readable description of this compilation phase.
   *
   * @return phase description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the progress weight for this phase (0.0 to 1.0).
   *
   * <p>This weight is used to calculate overall compilation progress.
   *
   * @return progress weight
   */
  public double getProgressWeight() {
    return progressWeight;
  }

  /**
   * Checks if this phase comes before another phase.
   *
   * @param other the phase to compare against
   * @return true if this phase comes before the other phase
   */
  public boolean isBefore(final CompilationPhase other) {
    return this.ordinal() < other.ordinal();
  }

  /**
   * Checks if this phase comes after another phase.
   *
   * @param other the phase to compare against
   * @return true if this phase comes after the other phase
   */
  public boolean isAfter(final CompilationPhase other) {
    return this.ordinal() > other.ordinal();
  }
}
