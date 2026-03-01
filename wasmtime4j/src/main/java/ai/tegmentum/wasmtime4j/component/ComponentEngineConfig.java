/*
 * Copyright 2025 Tegmentum AI
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

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Configuration options for WebAssembly Component Model engine creation.
 *
 * <p>This class provides options to customize the behavior of WebAssembly component engines,
 * including orchestration settings, distributed support, enterprise management features, and WIT
 * interface enhancements.
 *
 * <p>The component engine configuration extends the basic engine configuration with Component Model
 * specific features for building enterprise-grade WebAssembly applications.
 *
 * @since 1.0.0
 */
@SuppressFBWarnings(
    value = "URF_UNREAD_FIELD",
    justification =
        "Configuration fields are part of the public API and will be used as"
            + " implementation expands. Fields store builder state for future native bindings.")
public final class ComponentEngineConfig {

  // Core Component Model Features
  private boolean componentModelEnabled = true;
  private boolean witInterfaceValidation = true;
  private boolean componentLinking = true;
  private boolean componentComposition = true;

  // Advanced Orchestration Features
  private boolean advancedOrchestration = false;
  private boolean dependencyResolution = true;
  private boolean circularDependencyDetection = true;
  private boolean componentVersioning = true;

  // Distributed Component Support
  private boolean distributedSupport = false;
  private boolean distributedDiscovery = false;
  private boolean secureCommunication = false;
  private boolean distributedStateSynchronization = false;
  private boolean componentClustering = false;

  // Enterprise Management Features
  private boolean enterpriseManagement = false;
  private boolean auditLogging = false;
  private boolean securityPolicies = true;
  private boolean configurationManagement = false;
  private boolean monitoring = false;
  private boolean diagnostics = false;

  // WIT Interface Enhancement
  private boolean witInterfaceEnhancement = false;
  private boolean witCompatibilityChecking = true;
  private boolean witInterfaceMigration = false;
  private boolean witIntrospection = false;

  // Resource Management
  private boolean resourceManagement = true;
  private boolean resourceSharing = false;
  private boolean resourceIsolation = true;
  private boolean resourceOptimization = false;

  // Security Features
  private boolean capabilityBasedSecurity = true;
  private boolean sandboxEnforcement = true;
  private boolean permissionValidation = true;
  private boolean threatDetection = false;

  // Performance Features
  private boolean componentCaching = true;
  private boolean performanceOptimization = false;
  private boolean loadBalancing = false;
  private boolean autoScaling = false;

  /** Creates a new component engine configuration with default settings. */
  public ComponentEngineConfig() {
    // Default configuration with basic Component Model support
  }

  /**
   * Creates a new builder for component engine configuration.
   *
   * @return a new ComponentEngineConfigBuilder
   */
  public static ComponentEngineConfigBuilder builder() {
    return new ComponentEngineConfigBuilder();
  }

  /**
   * Enables or disables the Component Model feature.
   *
   * @param enabled true to enable Component Model support
   * @return this configuration for method chaining
   */
  public ComponentEngineConfig componentModelEnabled(final boolean enabled) {
    this.componentModelEnabled = enabled;
    return this;
  }

  /**
   * Enables or disables advanced orchestration features.
   *
   * @param enabled true to enable advanced orchestration
   * @return this configuration for method chaining
   */
  public ComponentEngineConfig enableAdvancedOrchestration(final boolean enabled) {
    this.advancedOrchestration = enabled;
    return this;
  }

  /**
   * Enables or disables distributed component support.
   *
   * @param enabled true to enable distributed support
   * @return this configuration for method chaining
   */
  public ComponentEngineConfig enableDistributedSupport(final boolean enabled) {
    this.distributedSupport = enabled;
    return this;
  }

  /**
   * Enables or disables enterprise management features.
   *
   * @param enabled true to enable enterprise management
   * @return this configuration for method chaining
   */
  public ComponentEngineConfig enableEnterpriseManagement(final boolean enabled) {
    this.enterpriseManagement = enabled;
    return this;
  }

  /**
   * Enables or disables WIT interface enhancement features.
   *
   * @param enabled true to enable WIT interface enhancement
   * @return this configuration for method chaining
   */
  public ComponentEngineConfig enableWitInterfaceEnhancement(final boolean enabled) {
    this.witInterfaceEnhancement = enabled;
    return this;
  }

  /**
   * Enables or disables capability-based security model.
   *
   * @param enabled true to enable capability-based security
   * @return this configuration for method chaining
   */
  public ComponentEngineConfig enableCapabilityBasedSecurity(final boolean enabled) {
    this.capabilityBasedSecurity = enabled;
    return this;
  }

  // Getters for all configuration options

  public boolean isComponentModelEnabled() {
    return componentModelEnabled;
  }

  public boolean isWitInterfaceValidation() {
    return witInterfaceValidation;
  }

  public boolean isAdvancedOrchestration() {
    return advancedOrchestration;
  }

  public boolean isDistributedSupport() {
    return distributedSupport;
  }

  public boolean isEnterpriseManagement() {
    return enterpriseManagement;
  }

  public boolean isWitInterfaceEnhancement() {
    return witInterfaceEnhancement;
  }

  public boolean isCapabilityBasedSecurity() {
    return capabilityBasedSecurity;
  }

  public boolean isResourceManagement() {
    return resourceManagement;
  }

  public boolean isComponentCaching() {
    return componentCaching;
  }

  /** Builder for ComponentEngineConfig with fluent API. */
  public static final class ComponentEngineConfigBuilder {
    private final ComponentEngineConfig config = new ComponentEngineConfig();

    private ComponentEngineConfigBuilder() {}

    public ComponentEngineConfigBuilder enableAdvancedOrchestration(final boolean enabled) {
      config.enableAdvancedOrchestration(enabled);
      return this;
    }

    public ComponentEngineConfigBuilder enableDistributedSupport(final boolean enabled) {
      config.enableDistributedSupport(enabled);
      return this;
    }

    public ComponentEngineConfigBuilder enableEnterpriseManagement(final boolean enabled) {
      config.enableEnterpriseManagement(enabled);
      return this;
    }

    public ComponentEngineConfigBuilder enableWitInterfaceEnhancement(final boolean enabled) {
      config.enableWitInterfaceEnhancement(enabled);
      return this;
    }

    public ComponentEngineConfigBuilder enableCapabilityBasedSecurity(final boolean enabled) {
      config.enableCapabilityBasedSecurity(enabled);
      return this;
    }

    public ComponentEngineConfig build() {
      return config;
    }
  }

  /**
   * Converts this ComponentEngineConfig to a basic EngineConfig for interface compatibility.
   *
   * @return a basic EngineConfig with default settings suitable for component execution
   */
  public EngineConfig toEngineConfig() {
    // Create a default EngineConfig suitable for component model execution
    return new EngineConfig();
  }
}
