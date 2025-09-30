/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j;

import java.util.Set;

/**
 * Requirements specification for interface compatibility checking.
 *
 * <p>This class defines the requirements and constraints for finding compatible interface versions,
 * including version ranges, feature requirements, and compatibility levels.
 *
 * @since 1.0.0
 */
public final class CompatibilityRequirements {

  private final VersionRange versionRange;
  private final Set<String> requiredFeatures;
  private final Set<String> optionalFeatures;
  private final Set<String> excludedVersions;
  private final CompatibilityLevel minimumCompatibilityLevel;
  private final boolean allowPreRelease;
  private final boolean allowBuildMetadata;
  private final Set<String> requiredFunctions;
  private final Set<String> requiredTypes;
  private final boolean strictFunctionSignatures;
  private final boolean strictTypeDefinitions;
  private final boolean allowBackwardCompatible;
  private final boolean allowForwardCompatible;
  private final int maxMajorVersionDifference;
  private final int maxMinorVersionDifference;

  private CompatibilityRequirements(Builder builder) {
    this.versionRange = builder.versionRange;
    this.requiredFeatures = Set.copyOf(builder.requiredFeatures);
    this.optionalFeatures = Set.copyOf(builder.optionalFeatures);
    this.excludedVersions = Set.copyOf(builder.excludedVersions);
    this.minimumCompatibilityLevel = builder.minimumCompatibilityLevel;
    this.allowPreRelease = builder.allowPreRelease;
    this.allowBuildMetadata = builder.allowBuildMetadata;
    this.requiredFunctions = Set.copyOf(builder.requiredFunctions);
    this.requiredTypes = Set.copyOf(builder.requiredTypes);
    this.strictFunctionSignatures = builder.strictFunctionSignatures;
    this.strictTypeDefinitions = builder.strictTypeDefinitions;
    this.allowBackwardCompatible = builder.allowBackwardCompatible;
    this.allowForwardCompatible = builder.allowForwardCompatible;
    this.maxMajorVersionDifference = builder.maxMajorVersionDifference;
    this.maxMinorVersionDifference = builder.maxMinorVersionDifference;
  }

  /**
   * Gets the version range requirement.
   *
   * @return version range
   */
  public VersionRange getVersionRange() {
    return versionRange;
  }

  /**
   * Gets the required features.
   *
   * @return required features set
   */
  public Set<String> getRequiredFeatures() {
    return requiredFeatures;
  }

  /**
   * Gets the optional features.
   *
   * @return optional features set
   */
  public Set<String> getOptionalFeatures() {
    return optionalFeatures;
  }

  /**
   * Gets the excluded versions.
   *
   * @return excluded versions set
   */
  public Set<String> getExcludedVersions() {
    return excludedVersions;
  }

  /**
   * Gets the minimum compatibility level required.
   *
   * @return minimum compatibility level
   */
  public CompatibilityLevel getMinimumCompatibilityLevel() {
    return minimumCompatibilityLevel;
  }

  /**
   * Gets whether pre-release versions are allowed.
   *
   * @return true if pre-release versions are allowed
   */
  public boolean isAllowPreRelease() {
    return allowPreRelease;
  }

  /**
   * Gets whether build metadata is allowed.
   *
   * @return true if build metadata is allowed
   */
  public boolean isAllowBuildMetadata() {
    return allowBuildMetadata;
  }

  /**
   * Gets the required functions.
   *
   * @return required functions set
   */
  public Set<String> getRequiredFunctions() {
    return requiredFunctions;
  }

  /**
   * Gets the required types.
   *
   * @return required types set
   */
  public Set<String> getRequiredTypes() {
    return requiredTypes;
  }

  /**
   * Gets whether function signatures must match strictly.
   *
   * @return true if strict function signature matching is required
   */
  public boolean isStrictFunctionSignatures() {
    return strictFunctionSignatures;
  }

  /**
   * Gets whether type definitions must match strictly.
   *
   * @return true if strict type definition matching is required
   */
  public boolean isStrictTypeDefinitions() {
    return strictTypeDefinitions;
  }

  /**
   * Gets whether backward compatible versions are allowed.
   *
   * @return true if backward compatible versions are allowed
   */
  public boolean isAllowBackwardCompatible() {
    return allowBackwardCompatible;
  }

  /**
   * Gets whether forward compatible versions are allowed.
   *
   * @return true if forward compatible versions are allowed
   */
  public boolean isAllowForwardCompatible() {
    return allowForwardCompatible;
  }

  /**
   * Gets the maximum allowed major version difference.
   *
   * @return maximum major version difference
   */
  public int getMaxMajorVersionDifference() {
    return maxMajorVersionDifference;
  }

  /**
   * Gets the maximum allowed minor version difference.
   *
   * @return maximum minor version difference
   */
  public int getMaxMinorVersionDifference() {
    return maxMinorVersionDifference;
  }

  /**
   * Creates a new builder for CompatibilityRequirements.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates default compatibility requirements.
   *
   * @return default requirements
   */
  public static CompatibilityRequirements defaults() {
    return new Builder().build();
  }

  /**
   * Creates strict compatibility requirements.
   *
   * @return strict requirements
   */
  public static CompatibilityRequirements strict() {
    return new Builder()
        .strictFunctionSignatures(true)
        .strictTypeDefinitions(true)
        .minimumCompatibilityLevel(CompatibilityLevel.FULL)
        .allowPreRelease(false)
        .maxMajorVersionDifference(0)
        .maxMinorVersionDifference(0)
        .build();
  }

  /**
   * Creates lenient compatibility requirements.
   *
   * @return lenient requirements
   */
  public static CompatibilityRequirements lenient() {
    return new Builder()
        .strictFunctionSignatures(false)
        .strictTypeDefinitions(false)
        .minimumCompatibilityLevel(CompatibilityLevel.PARTIAL)
        .allowPreRelease(true)
        .allowBackwardCompatible(true)
        .allowForwardCompatible(true)
        .maxMajorVersionDifference(1)
        .maxMinorVersionDifference(5)
        .build();
  }

  /** Builder for CompatibilityRequirements. */
  public static final class Builder {
    private VersionRange versionRange = VersionRange.any();
    private Set<String> requiredFeatures = Set.of();
    private Set<String> optionalFeatures = Set.of();
    private Set<String> excludedVersions = Set.of();
    private CompatibilityLevel minimumCompatibilityLevel = CompatibilityLevel.PARTIAL;
    private boolean allowPreRelease = false;
    private boolean allowBuildMetadata = true;
    private Set<String> requiredFunctions = Set.of();
    private Set<String> requiredTypes = Set.of();
    private boolean strictFunctionSignatures = false;
    private boolean strictTypeDefinitions = false;
    private boolean allowBackwardCompatible = true;
    private boolean allowForwardCompatible = false;
    private int maxMajorVersionDifference = 0;
    private int maxMinorVersionDifference = Integer.MAX_VALUE;

    /**
     * Sets the version range.
     *
     * @param versionRange the version range
     * @return this builder
     */
    public Builder versionRange(VersionRange versionRange) {
      this.versionRange = versionRange != null ? versionRange : VersionRange.any();
      return this;
    }

    /**
     * Sets the required features.
     *
     * @param requiredFeatures the required features
     * @return this builder
     */
    public Builder requiredFeatures(Set<String> requiredFeatures) {
      this.requiredFeatures = requiredFeatures != null ? Set.copyOf(requiredFeatures) : Set.of();
      return this;
    }

    /**
     * Sets the optional features.
     *
     * @param optionalFeatures the optional features
     * @return this builder
     */
    public Builder optionalFeatures(Set<String> optionalFeatures) {
      this.optionalFeatures = optionalFeatures != null ? Set.copyOf(optionalFeatures) : Set.of();
      return this;
    }

    /**
     * Sets the excluded versions.
     *
     * @param excludedVersions the excluded versions
     * @return this builder
     */
    public Builder excludedVersions(Set<String> excludedVersions) {
      this.excludedVersions = excludedVersions != null ? Set.copyOf(excludedVersions) : Set.of();
      return this;
    }

    /**
     * Sets the minimum compatibility level.
     *
     * @param minimumCompatibilityLevel the minimum compatibility level
     * @return this builder
     */
    public Builder minimumCompatibilityLevel(CompatibilityLevel minimumCompatibilityLevel) {
      this.minimumCompatibilityLevel =
          minimumCompatibilityLevel != null
              ? minimumCompatibilityLevel
              : CompatibilityLevel.PARTIAL;
      return this;
    }

    /**
     * Sets whether to allow pre-release versions.
     *
     * @param allowPreRelease true to allow pre-release versions
     * @return this builder
     */
    public Builder allowPreRelease(boolean allowPreRelease) {
      this.allowPreRelease = allowPreRelease;
      return this;
    }

    /**
     * Sets whether to allow build metadata.
     *
     * @param allowBuildMetadata true to allow build metadata
     * @return this builder
     */
    public Builder allowBuildMetadata(boolean allowBuildMetadata) {
      this.allowBuildMetadata = allowBuildMetadata;
      return this;
    }

    /**
     * Sets the required functions.
     *
     * @param requiredFunctions the required functions
     * @return this builder
     */
    public Builder requiredFunctions(Set<String> requiredFunctions) {
      this.requiredFunctions = requiredFunctions != null ? Set.copyOf(requiredFunctions) : Set.of();
      return this;
    }

    /**
     * Sets the required types.
     *
     * @param requiredTypes the required types
     * @return this builder
     */
    public Builder requiredTypes(Set<String> requiredTypes) {
      this.requiredTypes = requiredTypes != null ? Set.copyOf(requiredTypes) : Set.of();
      return this;
    }

    /**
     * Sets whether function signatures must match strictly.
     *
     * @param strictFunctionSignatures true for strict function signature matching
     * @return this builder
     */
    public Builder strictFunctionSignatures(boolean strictFunctionSignatures) {
      this.strictFunctionSignatures = strictFunctionSignatures;
      return this;
    }

    /**
     * Sets whether type definitions must match strictly.
     *
     * @param strictTypeDefinitions true for strict type definition matching
     * @return this builder
     */
    public Builder strictTypeDefinitions(boolean strictTypeDefinitions) {
      this.strictTypeDefinitions = strictTypeDefinitions;
      return this;
    }

    /**
     * Sets whether backward compatible versions are allowed.
     *
     * @param allowBackwardCompatible true to allow backward compatible versions
     * @return this builder
     */
    public Builder allowBackwardCompatible(boolean allowBackwardCompatible) {
      this.allowBackwardCompatible = allowBackwardCompatible;
      return this;
    }

    /**
     * Sets whether forward compatible versions are allowed.
     *
     * @param allowForwardCompatible true to allow forward compatible versions
     * @return this builder
     */
    public Builder allowForwardCompatible(boolean allowForwardCompatible) {
      this.allowForwardCompatible = allowForwardCompatible;
      return this;
    }

    /**
     * Sets the maximum major version difference.
     *
     * @param maxMajorVersionDifference the maximum major version difference
     * @return this builder
     */
    public Builder maxMajorVersionDifference(int maxMajorVersionDifference) {
      this.maxMajorVersionDifference = Math.max(0, maxMajorVersionDifference);
      return this;
    }

    /**
     * Sets the maximum minor version difference.
     *
     * @param maxMinorVersionDifference the maximum minor version difference
     * @return this builder
     */
    public Builder maxMinorVersionDifference(int maxMinorVersionDifference) {
      this.maxMinorVersionDifference = Math.max(0, maxMinorVersionDifference);
      return this;
    }

    /**
     * Builds the CompatibilityRequirements.
     *
     * @return the configured CompatibilityRequirements
     */
    public CompatibilityRequirements build() {
      return new CompatibilityRequirements(this);
    }
  }

  /** Version range specification. */
  public static final class VersionRange {
    private final WitInterfaceVersion minimum;
    private final WitInterfaceVersion maximum;
    private final boolean includeMinimum;
    private final boolean includeMaximum;

    private VersionRange(
        WitInterfaceVersion minimum,
        WitInterfaceVersion maximum,
        boolean includeMinimum,
        boolean includeMaximum) {
      this.minimum = minimum;
      this.maximum = maximum;
      this.includeMinimum = includeMinimum;
      this.includeMaximum = includeMaximum;
    }

    /**
     * Creates a version range that accepts any version.
     *
     * @return version range accepting any version
     */
    public static VersionRange any() {
      return new VersionRange(null, null, true, true);
    }

    /**
     * Creates a version range with minimum and maximum bounds.
     *
     * @param minimum the minimum version (inclusive)
     * @param maximum the maximum version (inclusive)
     * @return version range
     */
    public static VersionRange between(WitInterfaceVersion minimum, WitInterfaceVersion maximum) {
      return new VersionRange(minimum, maximum, true, true);
    }

    /**
     * Creates a version range with only a minimum bound.
     *
     * @param minimum the minimum version (inclusive)
     * @return version range
     */
    public static VersionRange atLeast(WitInterfaceVersion minimum) {
      return new VersionRange(minimum, null, true, true);
    }

    /**
     * Creates a version range with only a maximum bound.
     *
     * @param maximum the maximum version (inclusive)
     * @return version range
     */
    public static VersionRange atMost(WitInterfaceVersion maximum) {
      return new VersionRange(null, maximum, true, true);
    }

    /**
     * Checks if a version is within this range.
     *
     * @param version the version to check
     * @return true if the version is within this range
     */
    public boolean contains(WitInterfaceVersion version) {
      if (version == null) {
        return false;
      }

      if (minimum != null) {
        int cmp = version.compareTo(minimum);
        if (cmp < 0 || (cmp == 0 && !includeMinimum)) {
          return false;
        }
      }

      if (maximum != null) {
        int cmp = version.compareTo(maximum);
        if (cmp > 0 || (cmp == 0 && !includeMaximum)) {
          return false;
        }
      }

      return true;
    }

    public WitInterfaceVersion getMinimum() {
      return minimum;
    }

    public WitInterfaceVersion getMaximum() {
      return maximum;
    }

    public boolean isIncludeMinimum() {
      return includeMinimum;
    }

    public boolean isIncludeMaximum() {
      return includeMaximum;
    }
  }

  /** Compatibility levels enum. */
  public enum CompatibilityLevel {
    /** Full compatibility required. */
    FULL,
    /** Partial compatibility acceptable. */
    PARTIAL,
    /** Limited compatibility acceptable. */
    LIMITED,
    /** Any level acceptable (not recommended for production). */
    ANY
  }
}
