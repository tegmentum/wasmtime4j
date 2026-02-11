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

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a capability that a component can provide or require.
 *
 * <p>Capabilities are used for conditional component loading and compatibility checking. They
 * represent features, interfaces, or resources that components can provide or depend on.
 *
 * @since 1.0.0
 */
public final class ComponentCapability {

  private final String name;
  private final CapabilityType type;
  private final Optional<ComponentVersion> version;
  private final Set<String> attributes;
  private final CapabilityLevel level;

  private ComponentCapability(
      String name,
      CapabilityType type,
      Optional<ComponentVersion> version,
      Set<String> attributes,
      CapabilityLevel level) {
    this.name = Objects.requireNonNull(name, "Capability name cannot be null");
    this.type = Objects.requireNonNull(type, "Capability type cannot be null");
    this.version = Objects.requireNonNull(version, "Version cannot be null");
    this.attributes = Set.copyOf(attributes);
    this.level = Objects.requireNonNull(level, "Capability level cannot be null");
  }

  /**
   * Gets the capability name.
   *
   * @return the capability name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the capability type.
   *
   * @return the capability type
   */
  public CapabilityType getType() {
    return type;
  }

  /**
   * Gets the capability version.
   *
   * @return the capability version, if specified
   */
  public Optional<ComponentVersion> getVersion() {
    return version;
  }

  /**
   * Gets the capability attributes.
   *
   * @return set of capability attributes
   */
  public Set<String> getAttributes() {
    return attributes;
  }

  /**
   * Gets the capability level.
   *
   * @return the capability level
   */
  public CapabilityLevel getLevel() {
    return level;
  }

  /**
   * Checks if this capability is compatible with another capability.
   *
   * @param other the other capability to check against
   * @return true if capabilities are compatible
   */
  public boolean isCompatibleWith(ComponentCapability other) {
    if (!name.equals(other.name) || type != other.type) {
      return false;
    }

    // Version compatibility check
    if (version.isPresent() && other.version.isPresent()) {
      ComponentVersion thisVersion = version.get();
      ComponentVersion otherVersion = other.version.get();

      // Major version must match for compatibility
      if (thisVersion.getMajor() != otherVersion.getMajor()) {
        return false;
      }

      // Minor version should be backward compatible
      if (thisVersion.getMinor() > otherVersion.getMinor()) {
        return false;
      }
    }

    // Level compatibility check
    return level.isCompatibleWith(other.level);
  }

  /**
   * Creates a new capability builder.
   *
   * @param name the capability name
   * @param type the capability type
   * @return a new capability builder
   */
  public static Builder builder(String name, CapabilityType type) {
    return new Builder(name, type);
  }

  /**
   * Creates a simple interface capability.
   *
   * @param interfaceName the interface name
   * @return the interface capability
   */
  public static ComponentCapability interfaceCapability(String interfaceName) {
    return builder(interfaceName, CapabilityType.INTERFACE).level(CapabilityLevel.REQUIRED).build();
  }

  /**
   * Creates a versioned interface capability.
   *
   * @param interfaceName the interface name
   * @param version the interface version
   * @return the versioned interface capability
   */
  public static ComponentCapability interfaceCapability(
      String interfaceName, ComponentVersion version) {
    return builder(interfaceName, CapabilityType.INTERFACE)
        .version(version)
        .level(CapabilityLevel.REQUIRED)
        .build();
  }

  /**
   * Creates a feature capability.
   *
   * @param featureName the feature name
   * @return the feature capability
   */
  public static ComponentCapability featureCapability(String featureName) {
    return builder(featureName, CapabilityType.FEATURE).level(CapabilityLevel.OPTIONAL).build();
  }

  /**
   * Creates a resource capability.
   *
   * @param resourceName the resource name
   * @return the resource capability
   */
  public static ComponentCapability resourceCapability(String resourceName) {
    return builder(resourceName, CapabilityType.RESOURCE).level(CapabilityLevel.REQUIRED).build();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ComponentCapability that = (ComponentCapability) obj;
    return Objects.equals(name, that.name)
        && type == that.type
        && Objects.equals(version, that.version)
        && Objects.equals(attributes, that.attributes)
        && level == that.level;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, version, attributes, level);
  }

  @Override
  public String toString() {
    return String.format(
        "ComponentCapability{name='%s', type=%s, version=%s, level=%s}",
        name, type, version.map(ComponentVersion::toString).orElse("any"), level);
  }

  /** Builder for ComponentCapability. */
  public static final class Builder {
    private final String name;
    private final CapabilityType type;
    private Optional<ComponentVersion> version = Optional.empty();
    private Set<String> attributes = Set.of();
    private CapabilityLevel level = CapabilityLevel.REQUIRED;

    private Builder(String name, CapabilityType type) {
      this.name = name;
      this.type = type;
    }

    public Builder version(ComponentVersion version) {
      this.version = Optional.of(version);
      return this;
    }

    public Builder attributes(Set<String> attributes) {
      this.attributes = attributes != null ? Set.copyOf(attributes) : Set.of();
      return this;
    }

    public Builder level(CapabilityLevel level) {
      this.level = level;
      return this;
    }

    public ComponentCapability build() {
      return new ComponentCapability(name, type, version, attributes, level);
    }
  }

  /** Types of capabilities. */
  public enum CapabilityType {
    /** WIT interface capability. */
    INTERFACE,
    /** WebAssembly feature capability. */
    FEATURE,
    /** System resource capability. */
    RESOURCE,
    /** Runtime service capability. */
    SERVICE,
    /** Security capability. */
    SECURITY,
    /** Performance capability. */
    PERFORMANCE,
    /** Custom capability. */
    CUSTOM
  }

  /** Capability requirement levels. */
  public enum CapabilityLevel {
    /** Capability is absolutely required. */
    REQUIRED,
    /** Capability is preferred but optional. */
    PREFERRED,
    /** Capability is optional. */
    OPTIONAL,
    /** Capability is prohibited. */
    PROHIBITED;

    /**
     * Checks if this level is compatible with another level.
     *
     * @param other the other capability level
     * @return true if levels are compatible
     */
    public boolean isCompatibleWith(CapabilityLevel other) {
      // Required capabilities must be provided
      if (this == REQUIRED && other == PROHIBITED) {
        return false;
      }

      // Prohibited capabilities cannot be provided
      if (this == PROHIBITED && other == REQUIRED) {
        return false;
      }

      return true;
    }
  }
}
