package ai.tegmentum.wasmtime4j.metadata;

import java.util.List;
import java.util.Set;

/**
 * Represents a WebAssembly "target_features" custom section.
 *
 * <p>The target features section specifies WebAssembly features required by the module and provides
 * compatibility information for runtime engines. This helps engines determine if they can safely
 * execute the module.
 *
 * @since 1.0.0
 */
public final class TargetFeaturesSection {

  private final List<FeatureEntry> features;

  private TargetFeaturesSection(final Builder builder) {
    this.features =
        builder.features == null
            ? java.util.Collections.emptyList()
            : java.util.Collections.unmodifiableList(builder.features);
  }

  /**
   * Gets all feature entries.
   *
   * @return an immutable list of feature entries
   */
  public List<FeatureEntry> getFeatures() {
    return new java.util.ArrayList<>(features);
  }

  /**
   * Gets all required features.
   *
   * @return an immutable list of required features
   */
  public List<FeatureEntry> getRequiredFeatures() {
    return features.stream()
        .filter(FeatureEntry::isRequired)
        .collect(
            java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(), java.util.Collections::unmodifiableList));
  }

  /**
   * Gets all optional features.
   *
   * @return an immutable list of optional features
   */
  public List<FeatureEntry> getOptionalFeatures() {
    return features.stream()
        .filter(feature -> !feature.isRequired())
        .collect(
            java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(), java.util.Collections::unmodifiableList));
  }

  /**
   * Gets all used features.
   *
   * @return an immutable list of used features
   */
  public List<FeatureEntry> getUsedFeatures() {
    return features.stream()
        .filter(FeatureEntry::isUsed)
        .collect(
            java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(), java.util.Collections::unmodifiableList));
  }

  /**
   * Gets all disabled features.
   *
   * @return an immutable list of disabled features
   */
  public List<FeatureEntry> getDisabledFeatures() {
    return features.stream()
        .filter(FeatureEntry::isDisabled)
        .collect(
            java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(), java.util.Collections::unmodifiableList));
  }

  /**
   * Gets all feature names.
   *
   * @return an immutable set of feature names
   */
  public Set<String> getFeatureNames() {
    return features.stream()
        .map(FeatureEntry::getName)
        .collect(
            java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toSet(), java.util.Collections::unmodifiableSet));
  }

  /**
   * Checks if a specific feature is present.
   *
   * @param featureName the feature name to check
   * @return true if the feature is present
   * @throws IllegalArgumentException if featureName is null
   */
  public boolean hasFeature(final String featureName) {
    if (featureName == null) {
      throw new IllegalArgumentException("Feature name cannot be null");
    }

    return features.stream().anyMatch(feature -> featureName.equals(feature.getName()));
  }

  /**
   * Checks if a specific feature is required.
   *
   * @param featureName the feature name to check
   * @return true if the feature is required
   * @throws IllegalArgumentException if featureName is null
   */
  public boolean isFeatureRequired(final String featureName) {
    if (featureName == null) {
      throw new IllegalArgumentException("Feature name cannot be null");
    }

    return features.stream()
        .anyMatch(feature -> featureName.equals(feature.getName()) && feature.isRequired());
  }

  /**
   * Checks if a specific feature is used.
   *
   * @param featureName the feature name to check
   * @return true if the feature is used
   * @throws IllegalArgumentException if featureName is null
   */
  public boolean isFeatureUsed(final String featureName) {
    if (featureName == null) {
      throw new IllegalArgumentException("Feature name cannot be null");
    }

    return features.stream()
        .anyMatch(feature -> featureName.equals(feature.getName()) && feature.isUsed());
  }

  /**
   * Checks if a specific feature is disabled.
   *
   * @param featureName the feature name to check
   * @return true if the feature is disabled
   * @throws IllegalArgumentException if featureName is null
   */
  public boolean isFeatureDisabled(final String featureName) {
    if (featureName == null) {
      throw new IllegalArgumentException("Feature name cannot be null");
    }

    return features.stream()
        .anyMatch(feature -> featureName.equals(feature.getName()) && feature.isDisabled());
  }

  /**
   * Gets a feature entry by name.
   *
   * @param featureName the feature name to find
   * @return the feature entry, or empty if not found
   * @throws IllegalArgumentException if featureName is null
   */
  public java.util.Optional<FeatureEntry> getFeature(final String featureName) {
    if (featureName == null) {
      throw new IllegalArgumentException("Feature name cannot be null");
    }

    return features.stream().filter(feature -> featureName.equals(feature.getName())).findFirst();
  }

  /**
   * Checks if this target features section is empty.
   *
   * @return true if no features are defined
   */
  public boolean isEmpty() {
    return features.isEmpty();
  }

  /**
   * Gets a summary of this target features section.
   *
   * @return a human-readable summary
   */
  public String getSummary() {
    final long required = features.stream().filter(FeatureEntry::isRequired).count();
    final long used = features.stream().filter(FeatureEntry::isUsed).count();
    final long disabled = features.stream().filter(FeatureEntry::isDisabled).count();

    return String.format(
        "TargetFeaturesSection{total=%d, required=%d, used=%d, disabled=%d}",
        features.size(), required, used, disabled);
  }

  /**
   * Validates feature compatibility with a runtime engine.
   *
   * @param supportedFeatures set of features supported by the runtime
   * @return validation result
   * @throws IllegalArgumentException if supportedFeatures is null
   */
  public FeatureCompatibilityResult validateCompatibility(final Set<String> supportedFeatures) {
    if (supportedFeatures == null) {
      throw new IllegalArgumentException("Supported features cannot be null");
    }

    final List<String> missingRequired =
        getRequiredFeatures().stream()
            .map(FeatureEntry::getName)
            .filter(name -> !supportedFeatures.contains(name))
            .collect(java.util.stream.Collectors.toList());

    final List<String> unsupportedUsed =
        getUsedFeatures().stream()
            .map(FeatureEntry::getName)
            .filter(name -> !supportedFeatures.contains(name))
            .collect(java.util.stream.Collectors.toList());

    return new FeatureCompatibilityResult(missingRequired, unsupportedUsed);
  }

  /**
   * Creates a new builder for constructing a TargetFeaturesSection.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for constructing TargetFeaturesSection instances. */
  public static final class Builder {
    private List<FeatureEntry> features;

    private Builder() {}

    /**
     * Sets the feature entries.
     *
     * @param features list of feature entries
     * @return this builder
     */
    public Builder setFeatures(final List<FeatureEntry> features) {
      this.features = features == null ? null : new java.util.ArrayList<>(features);
      return this;
    }

    /**
     * Adds a feature entry.
     *
     * @param feature the feature entry to add
     * @return this builder
     * @throws IllegalArgumentException if feature is null
     */
    public Builder addFeature(final FeatureEntry feature) {
      if (feature == null) {
        throw new IllegalArgumentException("Feature entry cannot be null");
      }
      if (features == null) {
        features = new java.util.ArrayList<>();
      }
      features.add(feature);
      return this;
    }

    /**
     * Adds a required feature.
     *
     * @param featureName the feature name
     * @return this builder
     * @throws IllegalArgumentException if featureName is null or empty
     */
    public Builder addRequiredFeature(final String featureName) {
      return addFeature(FeatureEntry.required(featureName));
    }

    /**
     * Adds a used feature.
     *
     * @param featureName the feature name
     * @return this builder
     * @throws IllegalArgumentException if featureName is null or empty
     */
    public Builder addUsedFeature(final String featureName) {
      return addFeature(FeatureEntry.used(featureName));
    }

    /**
     * Adds a disabled feature.
     *
     * @param featureName the feature name
     * @return this builder
     * @throws IllegalArgumentException if featureName is null or empty
     */
    public Builder addDisabledFeature(final String featureName) {
      return addFeature(FeatureEntry.disabled(featureName));
    }

    /**
     * Builds the TargetFeaturesSection.
     *
     * @return a new TargetFeaturesSection instance
     */
    public TargetFeaturesSection build() {
      return new TargetFeaturesSection(this);
    }
  }

  /** Represents a single feature entry in the target features section. */
  public static final class FeatureEntry {
    private final String name;
    private final FeatureStatus status;

    /**
     * Creates a new feature entry.
     *
     * @param name the feature name
     * @param status the feature status
     * @throws IllegalArgumentException if name is null or empty, or status is null
     */
    public FeatureEntry(final String name, final FeatureStatus status) {
      if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Feature name cannot be null or empty");
      }
      if (status == null) {
        throw new IllegalArgumentException("Feature status cannot be null");
      }
      this.name = name;
      this.status = status;
    }

    /**
     * Creates a required feature entry.
     *
     * @param name the feature name
     * @return a new required feature entry
     * @throws IllegalArgumentException if name is null or empty
     */
    public static FeatureEntry required(final String name) {
      return new FeatureEntry(name, FeatureStatus.REQUIRED);
    }

    /**
     * Creates a used feature entry.
     *
     * @param name the feature name
     * @return a new used feature entry
     * @throws IllegalArgumentException if name is null or empty
     */
    public static FeatureEntry used(final String name) {
      return new FeatureEntry(name, FeatureStatus.USED);
    }

    /**
     * Creates a disabled feature entry.
     *
     * @param name the feature name
     * @return a new disabled feature entry
     * @throws IllegalArgumentException if name is null or empty
     */
    public static FeatureEntry disabled(final String name) {
      return new FeatureEntry(name, FeatureStatus.DISABLED);
    }

    /**
     * Gets the feature name.
     *
     * @return the feature name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the feature status.
     *
     * @return the feature status
     */
    public FeatureStatus getStatus() {
      return status;
    }

    /**
     * Checks if this feature is required.
     *
     * @return true if the feature is required
     */
    public boolean isRequired() {
      return status == FeatureStatus.REQUIRED;
    }

    /**
     * Checks if this feature is used.
     *
     * @return true if the feature is used
     */
    public boolean isUsed() {
      return status == FeatureStatus.USED;
    }

    /**
     * Checks if this feature is disabled.
     *
     * @return true if the feature is disabled
     */
    public boolean isDisabled() {
      return status == FeatureStatus.DISABLED;
    }

    @Override
    public String toString() {
      return String.format("FeatureEntry{name='%s', status=%s}", name, status);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final FeatureEntry that = (FeatureEntry) obj;
      return name.equals(that.name) && status == that.status;
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(name, status);
    }
  }

  /** Status of a WebAssembly feature. */
  public enum FeatureStatus {
    /** Feature is required for the module to work. */
    REQUIRED,
    /** Feature is used by the module. */
    USED,
    /** Feature is explicitly disabled. */
    DISABLED
  }

  /** Result of feature compatibility validation. */
  public static final class FeatureCompatibilityResult {
    private final List<String> missingRequiredFeatures;
    private final List<String> unsupportedUsedFeatures;

    /**
     * Creates a new feature compatibility result.
     *
     * @param missingRequiredFeatures list of required features that are missing
     * @param unsupportedUsedFeatures list of used features that are unsupported
     */
    public FeatureCompatibilityResult(
        final List<String> missingRequiredFeatures, final List<String> unsupportedUsedFeatures) {
      this.missingRequiredFeatures =
          missingRequiredFeatures == null
              ? java.util.Collections.emptyList()
              : java.util.Collections.unmodifiableList(missingRequiredFeatures);
      this.unsupportedUsedFeatures =
          unsupportedUsedFeatures == null
              ? java.util.Collections.emptyList()
              : java.util.Collections.unmodifiableList(unsupportedUsedFeatures);
    }

    /**
     * Gets the list of missing required features.
     *
     * @return list of missing required features
     */
    public List<String> getMissingRequiredFeatures() {
      return new java.util.ArrayList<>(missingRequiredFeatures);
    }

    /**
     * Gets the list of unsupported used features.
     *
     * @return list of unsupported used features
     */
    public List<String> getUnsupportedUsedFeatures() {
      return new java.util.ArrayList<>(unsupportedUsedFeatures);
    }

    /**
     * Checks if the module is compatible.
     *
     * @return true if no required features are missing
     */
    public boolean isCompatible() {
      return missingRequiredFeatures.isEmpty();
    }

    /**
     * Checks if there are warnings.
     *
     * @return true if there are unsupported used features
     */
    public boolean hasWarnings() {
      return !unsupportedUsedFeatures.isEmpty();
    }

    @Override
    public String toString() {
      return String.format(
          "FeatureCompatibilityResult{compatible=%s, missingRequired=%d, unsupportedUsed=%d}",
          isCompatible(), missingRequiredFeatures.size(), unsupportedUsedFeatures.size());
    }
  }

  @Override
  public String toString() {
    return getSummary();
  }
}
