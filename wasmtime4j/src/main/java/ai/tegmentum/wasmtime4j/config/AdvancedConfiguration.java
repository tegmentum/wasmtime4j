package ai.tegmentum.wasmtime4j.config;

/**
 * Advanced configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface AdvancedConfiguration {

  /**
   * Gets the configuration name.
   *
   * @return the configuration name
   */
  String getConfigurationName();

  /**
   * Gets the configuration type.
   *
   * @return the configuration type
   */
  String getConfigurationType();

  /**
   * Checks if the configuration is enabled.
   *
   * @return true if the configuration is enabled
   */
  boolean isEnabled();

  /**
   * Gets the configuration value.
   *
   * @return the configuration value as a string
   */
  String getValue();
}
