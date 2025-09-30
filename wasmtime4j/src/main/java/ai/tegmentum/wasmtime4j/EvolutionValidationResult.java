package ai.tegmentum.wasmtime4j;

/**
 * Evolution validation result interface.
 *
 * @since 1.0.0
 */
public interface EvolutionValidationResult {
  /**
   * Checks if validation passed.
   *
   * @return true if valid
   */
  boolean isValid();

  /**
   * Gets validation errors.
   *
   * @return list of errors
   */
  java.util.List<String> getErrors();
}
