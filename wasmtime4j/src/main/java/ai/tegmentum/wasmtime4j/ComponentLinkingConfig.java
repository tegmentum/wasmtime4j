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

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for advanced component linking operations.
 *
 * <p>This configuration controls various aspects of component linking including dynamic loading
 * strategies, resource sharing policies, and compatibility requirements.
 *
 * @since 1.0.0
 */
public final class ComponentLinkingConfig {

  private final LinkingStrategy strategy;
  private final SharedResourcePolicy sharedResourcePolicy;
  private final Set<ComponentCapability> requiredCapabilities;
  private final Map<String, Object> linkingParameters;
  private final Duration linkingTimeout;
  private final boolean allowVersionMismatch;
  private final CompatibilityLevel compatibilityLevel;
  private final boolean enableInterfaceAdaptation;
  private final ResourceIsolationLevel resourceIsolation;
  private final boolean enableHotSwapping;

  private ComponentLinkingConfig(Builder builder) {
    this.strategy = builder.strategy;
    this.sharedResourcePolicy = builder.sharedResourcePolicy;
    this.requiredCapabilities = Set.copyOf(builder.requiredCapabilities);
    this.linkingParameters = Map.copyOf(builder.linkingParameters);
    this.linkingTimeout = builder.linkingTimeout;
    this.allowVersionMismatch = builder.allowVersionMismatch;
    this.compatibilityLevel = builder.compatibilityLevel;
    this.enableInterfaceAdaptation = builder.enableInterfaceAdaptation;
    this.resourceIsolation = builder.resourceIsolation;
    this.enableHotSwapping = builder.enableHotSwapping;
  }

  /**
   * Gets the linking strategy to use.
   *
   * @return the linking strategy
   */
  public LinkingStrategy getStrategy() {
    return strategy;
  }

  /**
   * Gets the shared resource policy.
   *
   * @return the shared resource policy
   */
  public SharedResourcePolicy getSharedResourcePolicy() {
    return sharedResourcePolicy;
  }

  /**
   * Gets the required capabilities for linking.
   *
   * @return set of required capabilities
   */
  public Set<ComponentCapability> getRequiredCapabilities() {
    return requiredCapabilities;
  }

  /**
   * Gets the linking parameters.
   *
   * @return map of linking parameters
   */
  public Map<String, Object> getLinkingParameters() {
    return linkingParameters;
  }

  /**
   * Gets the linking timeout.
   *
   * @return the linking timeout
   */
  public Duration getLinkingTimeout() {
    return linkingTimeout;
  }

  /**
   * Checks if version mismatch is allowed.
   *
   * @return true if version mismatch is allowed
   */
  public boolean isAllowVersionMismatch() {
    return allowVersionMismatch;
  }

  /**
   * Gets the required compatibility level.
   *
   * @return the compatibility level
   */
  public CompatibilityLevel getCompatibilityLevel() {
    return compatibilityLevel;
  }

  /**
   * Checks if interface adaptation is enabled.
   *
   * @return true if interface adaptation is enabled
   */
  public boolean isInterfaceAdaptationEnabled() {
    return enableInterfaceAdaptation;
  }

  /**
   * Gets the resource isolation level.
   *
   * @return the resource isolation level
   */
  public ResourceIsolationLevel getResourceIsolation() {
    return resourceIsolation;
  }

  /**
   * Checks if hot swapping is enabled.
   *
   * @return true if hot swapping is enabled
   */
  public boolean isHotSwappingEnabled() {
    return enableHotSwapping;
  }

  /**
   * Creates a new builder for ComponentLinkingConfig.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default configuration for basic linking.
   *
   * @return default configuration
   */
  public static ComponentLinkingConfig defaultConfig() {
    return builder().build();
  }

  /**
   * Creates a configuration optimized for production use.
   *
   * @return production configuration
   */
  public static ComponentLinkingConfig productionConfig() {
    return builder()
        .strategy(LinkingStrategy.SHARED_EVERYTHING)
        .sharedResourcePolicy(SharedResourcePolicy.AGGRESSIVE_SHARING)
        .compatibilityLevel(CompatibilityLevel.STRICT)
        .resourceIsolation(ResourceIsolationLevel.MODERATE)
        .linkingTimeout(Duration.ofSeconds(30))
        .build();
  }

  /**
   * Creates a configuration for development and testing.
   *
   * @return development configuration
   */
  public static ComponentLinkingConfig developmentConfig() {
    return builder()
        .strategy(LinkingStrategy.STATIC_LINKING)
        .allowVersionMismatch(true)
        .enableInterfaceAdaptation(true)
        .enableHotSwapping(true)
        .compatibilityLevel(CompatibilityLevel.LENIENT)
        .linkingTimeout(Duration.ofMinutes(5))
        .build();
  }

  /** Builder for ComponentLinkingConfig. */
  public static final class Builder {
    private LinkingStrategy strategy = LinkingStrategy.ADAPTIVE;
    private SharedResourcePolicy sharedResourcePolicy = SharedResourcePolicy.CONSERVATIVE;
    private Set<ComponentCapability> requiredCapabilities = Set.of();
    private Map<String, Object> linkingParameters = Map.of();
    private Duration linkingTimeout = Duration.ofMinutes(1);
    private boolean allowVersionMismatch = false;
    private CompatibilityLevel compatibilityLevel = CompatibilityLevel.MODERATE;
    private boolean enableInterfaceAdaptation = false;
    private ResourceIsolationLevel resourceIsolation = ResourceIsolationLevel.MODERATE;
    private boolean enableHotSwapping = false;

    private Builder() {}

    public Builder strategy(LinkingStrategy strategy) {
      this.strategy = strategy;
      return this;
    }

    public Builder sharedResourcePolicy(SharedResourcePolicy policy) {
      this.sharedResourcePolicy = policy;
      return this;
    }

    /**
     * Sets the required capabilities for component linking.
     *
     * @param capabilities the set of required capabilities
     * @return this builder for method chaining
     */
    public Builder requiredCapabilities(Set<ComponentCapability> capabilities) {
      this.requiredCapabilities =
          capabilities == null ? null : new java.util.HashSet<>(capabilities);
      return this;
    }

    public Builder linkingParameters(Map<String, Object> parameters) {
      this.linkingParameters = parameters == null ? null : new java.util.HashMap<>(parameters);
      return this;
    }

    public Builder linkingTimeout(Duration timeout) {
      this.linkingTimeout = timeout;
      return this;
    }

    public Builder allowVersionMismatch(boolean allow) {
      this.allowVersionMismatch = allow;
      return this;
    }

    public Builder compatibilityLevel(CompatibilityLevel level) {
      this.compatibilityLevel = level;
      return this;
    }

    public Builder enableInterfaceAdaptation(boolean enable) {
      this.enableInterfaceAdaptation = enable;
      return this;
    }

    public Builder resourceIsolation(ResourceIsolationLevel level) {
      this.resourceIsolation = level;
      return this;
    }

    public Builder enableHotSwapping(boolean enable) {
      this.enableHotSwapping = enable;
      return this;
    }

    public ComponentLinkingConfig build() {
      return new ComponentLinkingConfig(this);
    }
  }

  /** Linking strategy options. */
  public enum LinkingStrategy {
    /** Static linking with all dependencies bundled. */
    STATIC_LINKING,
    /** Dynamic linking with runtime resolution. */
    DYNAMIC_LINKING,
    /** Shared-everything dynamic linking for efficient resource usage. */
    SHARED_EVERYTHING,
    /** Adaptive strategy that chooses based on runtime conditions. */
    ADAPTIVE
  }

  /** Resource sharing policy options. */
  public enum SharedResourcePolicy {
    /** No resource sharing between components. */
    NO_SHARING,
    /** Conservative sharing of safe resources only. */
    CONSERVATIVE,
    /** Moderate sharing with safety checks. */
    MODERATE,
    /** Aggressive sharing for maximum efficiency. */
    AGGRESSIVE_SHARING
  }

  /** Compatibility level requirements. */
  public enum CompatibilityLevel {
    /** Strict compatibility checking. */
    STRICT,
    /** Moderate compatibility with some flexibility. */
    MODERATE,
    /** Lenient compatibility allowing more mismatches. */
    LENIENT
  }

  /** Resource isolation levels. */
  public enum ResourceIsolationLevel {
    /** No isolation - components share all resources. */
    NONE,
    /** Basic isolation of critical resources. */
    BASIC,
    /** Moderate isolation with balanced sharing. */
    MODERATE,
    /** Strong isolation with minimal sharing. */
    STRONG,
    /** Complete isolation - no resource sharing. */
    COMPLETE
  }
}
