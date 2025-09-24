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

package ai.tegmentum.wasmtime4j.component;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Metadata information for WebAssembly components.
 *
 * <p>This class contains descriptive information about components including:
 *
 * <ul>
 *   <li>Component identification and versioning
 *   <li>Author and license information
 *   <li>Dependencies and compatibility requirements
 *   <li>Build and deployment metadata
 *   <li>Custom properties and tags
 * </ul>
 *
 * @since 1.0.0
 */
public final class ComponentMetadata {

  private final String name;
  private final String description;
  private final ComponentVersion version;
  private final String author;
  private final String license;
  private final Instant createdAt;
  private final Instant modifiedAt;
  private final String checksum;
  private final Set<String> tags;
  private final Map<String, String> properties;
  private final Set<ComponentDependency> dependencies;
  private final ComponentCompatibilityInfo compatibilityInfo;
  private final ComponentBuildInfo buildInfo;

  /**
   * Creates new component metadata.
   *
   * @param builder the metadata builder
   */
  private ComponentMetadata(final Builder builder) {
    this.name = Objects.requireNonNull(builder.name, "Component name cannot be null");
    this.description = builder.description;
    this.version = Objects.requireNonNull(builder.version, "Component version cannot be null");
    this.author = builder.author;
    this.license = builder.license;
    this.createdAt = Objects.requireNonNull(builder.createdAt, "Created timestamp cannot be null");
    this.modifiedAt = builder.modifiedAt != null ? builder.modifiedAt : builder.createdAt;
    this.checksum = builder.checksum;
    this.tags = Collections.unmodifiableSet(builder.tags);
    this.properties = Collections.unmodifiableMap(builder.properties);
    this.dependencies = Collections.unmodifiableSet(builder.dependencies);
    this.compatibilityInfo = builder.compatibilityInfo;
    this.buildInfo = builder.buildInfo;
  }

  /**
   * Creates a new metadata builder.
   *
   * @param name the component name
   * @param version the component version
   * @return a new metadata builder
   */
  public static Builder builder(final String name, final ComponentVersion version) {
    return new Builder(name, version);
  }

  /**
   * Gets the component name.
   *
   * @return the component name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the component description.
   *
   * @return the component description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the component version.
   *
   * @return the component version
   */
  public ComponentVersion getVersion() {
    return version;
  }

  /**
   * Gets the component author.
   *
   * @return the component author
   */
  public String getAuthor() {
    return author;
  }

  /**
   * Gets the component license.
   *
   * @return the component license
   */
  public String getLicense() {
    return license;
  }

  /**
   * Gets the creation timestamp.
   *
   * @return the creation timestamp
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Gets the last modification timestamp.
   *
   * @return the last modification timestamp
   */
  public Instant getModifiedAt() {
    return modifiedAt;
  }

  /**
   * Gets the component checksum.
   *
   * @return the component checksum
   */
  public String getChecksum() {
    return checksum;
  }

  /**
   * Gets the component tags.
   *
   * @return the component tags
   */
  public Set<String> getTags() {
    return tags;
  }

  /**
   * Gets the custom properties.
   *
   * @return the custom properties
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Gets a custom property value.
   *
   * @param key the property key
   * @return the property value, or null if not found
   */
  public String getProperty(final String key) {
    return properties.get(key);
  }

  /**
   * Gets the component dependencies.
   *
   * @return the component dependencies
   */
  public Set<ComponentDependency> getDependencies() {
    return dependencies;
  }

  /**
   * Gets the compatibility information.
   *
   * @return the compatibility information
   */
  public ComponentCompatibilityInfo getCompatibilityInfo() {
    return compatibilityInfo;
  }

  /**
   * Gets the build information.
   *
   * @return the build information
   */
  public ComponentBuildInfo getBuildInfo() {
    return buildInfo;
  }

  /**
   * Checks if this component has a specific tag.
   *
   * @param tag the tag to check for
   * @return true if the component has the tag
   */
  public boolean hasTag(final String tag) {
    return tags.contains(tag);
  }

  /**
   * Checks if this component is compatible with a specific platform.
   *
   * @param platform the platform to check
   * @return true if compatible with the platform
   */
  public boolean isCompatibleWith(final String platform) {
    return compatibilityInfo == null
        || compatibilityInfo.getSupportedPlatforms().isEmpty()
        || compatibilityInfo.getSupportedPlatforms().contains(platform);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ComponentMetadata that = (ComponentMetadata) obj;
    return Objects.equals(name, that.name)
        && Objects.equals(version, that.version)
        && Objects.equals(checksum, that.checksum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version, checksum);
  }

  @Override
  public String toString() {
    return String.format(
        "ComponentMetadata{name='%s', version=%s, author='%s'}", name, version, author);
  }

  /** Builder for component metadata. */
  public static final class Builder {
    private final String name;
    private final ComponentVersion version;
    private final Instant createdAt;

    private String description;
    private String author;
    private String license;
    private Instant modifiedAt;
    private String checksum;
    private Set<String> tags = Collections.emptySet();
    private Map<String, String> properties = Collections.emptyMap();
    private Set<ComponentDependency> dependencies = Collections.emptySet();
    private ComponentCompatibilityInfo compatibilityInfo;
    private ComponentBuildInfo buildInfo;

    private Builder(final String name, final ComponentVersion version) {
      this.name = Objects.requireNonNull(name, "Component name cannot be null");
      this.version = Objects.requireNonNull(version, "Component version cannot be null");
      this.createdAt = Instant.now();
    }

    /**
     * Sets the component description.
     *
     * @param description the component description
     * @return this builder
     */
    public Builder description(final String description) {
      this.description = description;
      return this;
    }

    /**
     * Sets the component author.
     *
     * @param author the component author
     * @return this builder
     */
    public Builder author(final String author) {
      this.author = author;
      return this;
    }

    /**
     * Sets the component license.
     *
     * @param license the component license
     * @return this builder
     */
    public Builder license(final String license) {
      this.license = license;
      return this;
    }

    /**
     * Sets the last modification timestamp.
     *
     * @param modifiedAt the last modification timestamp
     * @return this builder
     */
    public Builder modifiedAt(final Instant modifiedAt) {
      this.modifiedAt = modifiedAt;
      return this;
    }

    /**
     * Sets the component checksum.
     *
     * @param checksum the component checksum
     * @return this builder
     */
    public Builder checksum(final String checksum) {
      this.checksum = checksum;
      return this;
    }

    /**
     * Sets the component tags.
     *
     * @param tags the component tags
     * @return this builder
     */
    public Builder tags(final Set<String> tags) {
      this.tags = Objects.requireNonNull(tags, "Tags cannot be null");
      return this;
    }

    /**
     * Sets the custom properties.
     *
     * @param properties the custom properties
     * @return this builder
     */
    public Builder properties(final Map<String, String> properties) {
      this.properties = Objects.requireNonNull(properties, "Properties cannot be null");
      return this;
    }

    /**
     * Sets the component dependencies.
     *
     * @param dependencies the component dependencies
     * @return this builder
     */
    public Builder dependencies(final Set<ComponentDependency> dependencies) {
      this.dependencies = Objects.requireNonNull(dependencies, "Dependencies cannot be null");
      return this;
    }

    /**
     * Sets the compatibility information.
     *
     * @param compatibilityInfo the compatibility information
     * @return this builder
     */
    public Builder compatibilityInfo(final ComponentCompatibilityInfo compatibilityInfo) {
      this.compatibilityInfo = compatibilityInfo;
      return this;
    }

    /**
     * Sets the build information.
     *
     * @param buildInfo the build information
     * @return this builder
     */
    public Builder buildInfo(final ComponentBuildInfo buildInfo) {
      this.buildInfo = buildInfo;
      return this;
    }

    /**
     * Builds the component metadata.
     *
     * @return the component metadata
     */
    public ComponentMetadata build() {
      return new ComponentMetadata(this);
    }
  }
}
