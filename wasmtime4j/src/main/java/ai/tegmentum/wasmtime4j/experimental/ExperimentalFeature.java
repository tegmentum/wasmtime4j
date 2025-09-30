package ai.tegmentum.wasmtime4j.experimental;

/**
 * Experimental feature interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExperimentalFeature {

  /**
   * Gets the feature name.
   *
   * @return feature name
   */
  String getFeatureName();

  /**
   * Gets the feature version.
   *
   * @return feature version
   */
  String getVersion();

  /**
   * Checks if the feature is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Enables or disables the feature.
   *
   * @param enabled enabled state
   */
  void setEnabled(boolean enabled);

  /**
   * Gets the feature status.
   *
   * @return feature status
   */
  FeatureStatus getStatus();

  /**
   * Gets the feature description.
   *
   * @return feature description
   */
  String getDescription();

  /**
   * Gets feature dependencies.
   *
   * @return list of dependency names
   */
  java.util.List<String> getDependencies();

  /**
   * Checks if the feature is stable.
   *
   * @return true if stable
   */
  boolean isStable();

  /** Feature status enumeration. */
  enum FeatureStatus {
    /** Feature is experimental. */
    EXPERIMENTAL,
    /** Feature is deprecated. */
    DEPRECATED,
    /** Feature is stable. */
    STABLE,
    /** Feature is disabled. */
    DISABLED
  }
}
