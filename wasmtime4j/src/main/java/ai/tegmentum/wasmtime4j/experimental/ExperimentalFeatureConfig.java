package ai.tegmentum.wasmtime4j.experimental;

/**
 * Experimental feature configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExperimentalFeatureConfig {

  /**
   * Checks if experimental features are globally enabled.
   *
   * @return true if globally enabled
   */
  boolean isGloballyEnabled();

  /**
   * Sets the global experimental features flag.
   *
   * @param enabled global enabled state
   */
  void setGloballyEnabled(boolean enabled);

  /**
   * Gets enabled experimental features.
   *
   * @return list of enabled features
   */
  java.util.List<String> getEnabledFeatures();

  /**
   * Enables a specific experimental feature.
   *
   * @param featureName feature to enable
   */
  void enableFeature(String featureName);

  /**
   * Disables a specific experimental feature.
   *
   * @param featureName feature to disable
   */
  void disableFeature(String featureName);

  /**
   * Checks if a specific feature is enabled.
   *
   * @param featureName feature name
   * @return true if enabled
   */
  boolean isFeatureEnabled(String featureName);

  /**
   * Gets the configuration version.
   *
   * @return configuration version
   */
  String getConfigVersion();

  /**
   * Loads configuration from properties.
   *
   * @param properties configuration properties
   */
  void loadFromProperties(java.util.Properties properties);

  /**
   * Saves configuration to properties.
   *
   * @return configuration properties
   */
  java.util.Properties saveToProperties();

  /**
   * Validates the current configuration.
   *
   * @return validation result
   */
  ValidationResult validate();

  /** Validation result interface. */
  interface ValidationResult {
    /**
     * Checks if validation passed.
     *
     * @return true if valid
     */
    boolean isValid();

    /**
     * Gets validation errors.
     *
     * @return list of error messages
     */
    java.util.List<String> getErrors();

    /**
     * Gets validation warnings.
     *
     * @return list of warning messages
     */
    java.util.List<String> getWarnings();
  }
}
