package ai.tegmentum.wasmtime4j.validation;

import ai.tegmentum.wasmtime4j.EngineConfig;
import java.util.Objects;

/** Result of engine validation containing validity status and configuration. */
public final class EngineValidationResult {
  private final boolean isValid;
  private final EngineConfig config;

  private EngineValidationResult(final boolean isValid, final EngineConfig config) {
    this.isValid = isValid;
    this.config = config;
  }

  /**
   * Creates an engine validation result.
   *
   * @param isValid whether the engine is valid
   * @param config the engine configuration
   * @return the validation result
   */
  public static EngineValidationResult of(final boolean isValid, final EngineConfig config) {
    return new EngineValidationResult(isValid, config);
  }

  /**
   * Gets whether the engine is valid.
   *
   * @return true if valid
   */
  public boolean isValid() {
    return isValid;
  }

  /**
   * Gets the engine configuration.
   *
   * @return the configuration
   */
  public EngineConfig getConfig() {
    return config;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final EngineValidationResult that = (EngineValidationResult) o;
    return isValid == that.isValid && Objects.equals(config, that.config);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isValid, config);
  }

  @Override
  public String toString() {
    return "EngineValidationResult{isValid=" + isValid + ", config=" + config + '}';
  }
}
