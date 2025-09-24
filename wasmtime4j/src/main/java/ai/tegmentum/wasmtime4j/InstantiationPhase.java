package ai.tegmentum.wasmtime4j;

/**
 * Phases of WebAssembly instantiation during streaming instantiation.
 *
 * <p>InstantiationPhase represents the different stages of WebAssembly module instantiation, from
 * initial preparation through function compilation and final linking.
 *
 * @since 1.0.0
 */
public enum InstantiationPhase {
  /**
   * Initial preparation for instantiation.
   *
   * <p>This phase sets up the instantiation context and prepares for resource allocation.
   */
  PREPARATION("Preparing for instantiation", 0.1),

  /**
   * Import resolution and validation.
   *
   * <p>This phase resolves and validates all imports required by the module.
   */
  IMPORT_RESOLUTION("Resolving imports", 0.2),

  /**
   * Memory allocation and initialization.
   *
   * <p>This phase allocates and initializes the WebAssembly linear memory.
   */
  MEMORY_ALLOCATION("Allocating memory", 0.3),

  /**
   * Table allocation and initialization.
   *
   * <p>This phase allocates and initializes WebAssembly tables.
   */
  TABLE_ALLOCATION("Allocating tables", 0.4),

  /**
   * Global allocation and initialization.
   *
   * <p>This phase allocates and initializes WebAssembly globals.
   */
  GLOBAL_ALLOCATION("Allocating globals", 0.5),

  /**
   * Function compilation and linking.
   *
   * <p>This phase compiles functions and links them with imports and resources.
   */
  FUNCTION_COMPILATION("Compiling functions", 0.8),

  /**
   * Start function execution.
   *
   * <p>This phase executes the module's start function (if present).
   */
  START_EXECUTION("Executing start function", 0.9),

  /**
   * Final instance preparation.
   *
   * <p>This phase finalizes the instance and makes it ready for use.
   */
  FINALIZATION("Finalizing instance", 1.0);

  private final String description;
  private final double progressWeight;

  InstantiationPhase(final String description, final double progressWeight) {
    this.description = description;
    this.progressWeight = progressWeight;
  }

  /**
   * Gets the human-readable description of this instantiation phase.
   *
   * @return phase description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the progress weight for this phase (0.0 to 1.0).
   *
   * <p>This weight is used to calculate overall instantiation progress.
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
  public boolean isBefore(final InstantiationPhase other) {
    return this.ordinal() < other.ordinal();
  }

  /**
   * Checks if this phase comes after another phase.
   *
   * @param other the phase to compare against
   * @return true if this phase comes after the other phase
   */
  public boolean isAfter(final InstantiationPhase other) {
    return this.ordinal() > other.ordinal();
  }
}