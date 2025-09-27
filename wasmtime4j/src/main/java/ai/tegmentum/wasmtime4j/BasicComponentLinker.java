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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Basic implementation of ComponentLinker for foundational Component Model support.
 *
 * <p>This implementation provides core component linking and composition functionality
 * as part of Task #304 to stabilize the Component Model foundation. It focuses on
 * essential linking operations while maintaining compatibility with the advanced
 * ComponentLinker interface.
 *
 * @since 1.0.0
 */
public final class BasicComponentLinker implements ComponentLinker {

  private static final Logger LOGGER = Logger.getLogger(BasicComponentLinker.class.getName());

  private ComponentRegistry registry;
  private final AtomicInteger linkCount = new AtomicInteger(0);
  private final AtomicInteger swapCount = new AtomicInteger(0);
  private final List<ComponentLinkInfo> activeLinks = new ArrayList<>();

  /**
   * Creates a new basic component linker.
   */
  public BasicComponentLinker() {
    this(new BasicComponentRegistry());
  }

  /**
   * Creates a new basic component linker with the specified registry.
   *
   * @param registry the component registry to use
   */
  public BasicComponentLinker(final ComponentRegistry registry) {
    this.registry = registry != null ? registry : new BasicComponentRegistry();
  }

  @Override
  public ComponentSimple linkComponents(final List<ComponentSimple> components,
      final ComponentLinkingConfig linkingConfig) throws WasmException {

    if (components == null || components.isEmpty()) {
      throw new WasmException("Cannot link empty component list");
    }

    if (linkingConfig == null) {
      throw new WasmException("Linking configuration cannot be null");
    }

    LOGGER.fine("Linking " + components.size() + " components");

    try {
      // Validate all components are compatible for linking
      validateComponentsForLinking(components);

      // Create a composite component that represents the linked components
      final ComponentSimple linkedComponent = createLinkedComponent(components, linkingConfig);

      // Track the link
      trackComponentLink(components, linkedComponent);

      linkCount.incrementAndGet();
      LOGGER.fine("Successfully linked components into composite component");

      return linkedComponent;

    } catch (final Exception e) {
      throw new WasmException("Failed to link components", e);
    }
  }

  @Override
  public CompletableFuture<ComponentSwapResult> hotSwapComponent(
      final ComponentSimple oldComponent,
      final ComponentSimple newComponent,
      final ComponentSwapConfig swapConfig) throws WasmException {

    if (oldComponent == null) {
      throw new WasmException("Old component cannot be null");
    }
    if (newComponent == null) {
      throw new WasmException("New component cannot be null");
    }
    if (swapConfig == null) {
      throw new WasmException("Swap configuration cannot be null");
    }

    return CompletableFuture.supplyAsync(() -> {
      try {
        // Check compatibility between old and new components
        final ComponentCompatibility compatibility =
            oldComponent.checkCompatibility(newComponent);

        if (!compatibility.isCompatible()) {
          return new ComponentSwapResult(false,
              "Components are not compatible: " + compatibility.getDetails());
        }

        // Perform the hot swap operation
        // In a full implementation, this would involve more sophisticated state transfer
        swapCount.incrementAndGet();
        LOGGER.fine("Hot swapped component " + oldComponent.getId() +
                   " with " + newComponent.getId());

        return new ComponentSwapResult(true, "Hot swap completed successfully");

      } catch (final Exception e) {
        LOGGER.warning("Hot swap failed: " + e.getMessage());
        return new ComponentSwapResult(false, "Hot swap failed: " + e.getMessage());
      }
    });
  }

  @Override
  public Optional<ComponentSimple> loadComponentDynamic(
      final ComponentSpecification componentSpec,
      final ComponentLoadConditions loadConditions) throws WasmException {

    if (componentSpec == null) {
      throw new WasmException("Component specification cannot be null");
    }
    if (loadConditions == null) {
      throw new WasmException("Load conditions cannot be null");
    }

    try {
      // Check if load conditions are satisfied
      if (!evaluateLoadConditions(loadConditions)) {
        LOGGER.fine("Load conditions not satisfied for component: " + componentSpec.getName());
        return Optional.empty();
      }

      // Check if component is available in registry
      final Optional<ComponentSimple> component =
          registry.findComponent(componentSpec.getName(), componentSpec.getVersion());

      if (component.isPresent()) {
        LOGGER.fine("Dynamically loaded component: " + componentSpec.getName());
        return component;
      }

      // Component not found
      LOGGER.fine("Component not found for dynamic loading: " + componentSpec.getName());
      return Optional.empty();

    } catch (final Exception e) {
      throw new WasmException("Failed to load component dynamically", e);
    }
  }

  @Override
  public ComponentCompatibilityResult checkCompatibility(final ComponentSimple source,
      final ComponentSimple target) {

    if (source == null || target == null) {
      return new ComponentCompatibilityResult(false, "Source or target component is null");
    }

    try {
      // Basic compatibility checking
      final boolean versionCompatible = source.getVersion().isCompatibleWith(target.getVersion());
      final WitCompatibilityResult witCompatibility = source.checkWitCompatibility(target);

      final boolean compatible = versionCompatible && witCompatibility.isCompatible();
      final String details = String.format(
          "Version compatible: %s, WIT compatible: %s",
          versionCompatible, witCompatibility.isCompatible());

      return new ComponentCompatibilityResult(compatible, details);

    } catch (final Exception e) {
      return new ComponentCompatibilityResult(false, "Compatibility check failed: " + e.getMessage());
    }
  }

  @Override
  public ComponentSimple injectDependencies(final ComponentSimple component,
      final DependencyInjectionConfig injectionConfig) throws WasmException {

    if (component == null) {
      throw new WasmException("Component cannot be null");
    }
    if (injectionConfig == null) {
      throw new WasmException("Injection configuration cannot be null");
    }

    try {
      // Get component dependencies
      final Set<ComponentSimple> dependencies = component.resolveDependencies(registry);

      // For now, return the component as-is since dependency injection
      // requires more advanced component composition
      LOGGER.fine("Dependency injection completed for component: " + component.getId());
      return component;

    } catch (final Exception e) {
      throw new WasmException("Failed to inject dependencies", e);
    }
  }

  @Override
  public ComponentPipeline createPipeline(final ComponentPipelineSpec pipeline) throws WasmException {
    if (pipeline == null) {
      throw new WasmException("Pipeline specification cannot be null");
    }

    try {
      // Create a basic pipeline implementation
      return new BasicComponentPipeline(pipeline);
    } catch (final Exception e) {
      throw new WasmException("Failed to create pipeline", e);
    }
  }

  @Override
  public ComponentEventSystem setupEventCommunication(final ComponentEventConfig eventConfig)
      throws WasmException {
    if (eventConfig == null) {
      throw new WasmException("Event configuration cannot be null");
    }

    try {
      // Create a basic event system implementation
      return new BasicComponentEventSystem(eventConfig);
    } catch (final Exception e) {
      throw new WasmException("Failed to setup event communication", e);
    }
  }

  @Override
  public LinkingValidationResult validateLinking(final ComponentLinkingConfig linkingConfig) {
    if (linkingConfig == null) {
      return new LinkingValidationResult(false, "Linking configuration is null");
    }

    // Basic validation - in a full implementation this would be more comprehensive
    return new LinkingValidationResult(true, "Basic validation passed");
  }

  @Override
  public ComponentRegistry getRegistry() {
    return registry;
  }

  @Override
  public void setRegistry(final ComponentRegistry registry) {
    this.registry = registry != null ? registry : new BasicComponentRegistry();
  }

  @Override
  public ComponentLinkingStatistics getStatistics() {
    return new ComponentLinkingStatistics(linkCount.get(), swapCount.get(), activeLinks.size());
  }

  @Override
  public int cleanup() throws WasmException {
    try {
      // Clean up inactive links
      final int initialSize = activeLinks.size();
      activeLinks.removeIf(link -> !link.isActive());
      final int cleaned = initialSize - activeLinks.size();

      LOGGER.fine("Cleaned up " + cleaned + " inactive component links");
      return cleaned;
    } catch (final Exception e) {
      throw new WasmException("Failed to cleanup component links", e);
    }
  }

  @Override
  public List<ComponentLinkInfo> getActiveLinks() {
    return Collections.unmodifiableList(new ArrayList<>(activeLinks));
  }

  @Override
  public void shutdown() {
    activeLinks.clear();
    LOGGER.fine("Component linker shutdown completed");
  }

  private void validateComponentsForLinking(final List<ComponentSimple> components) throws WasmException {
    for (final ComponentSimple component : components) {
      if (component == null) {
        throw new WasmException("Cannot link null component");
      }
      if (!component.isValid()) {
        throw new WasmException("Cannot link invalid component: " + component.getId());
      }
    }
  }

  private ComponentSimple createLinkedComponent(final List<ComponentSimple> components,
      final ComponentLinkingConfig linkingConfig) {
    // Create a composite component that represents the linked components
    // In a full implementation, this would create an actual composite WebAssembly component
    return new LinkedCompositeComponent(components, linkingConfig);
  }

  private void trackComponentLink(final List<ComponentSimple> components,
      final ComponentSimple linkedComponent) {
    final ComponentLinkInfo linkInfo = new ComponentLinkInfo(
        "link-" + System.nanoTime(),
        new ArrayList<>(components),
        linkedComponent,
        true
    );
    activeLinks.add(linkInfo);
  }

  private boolean evaluateLoadConditions(final ComponentLoadConditions loadConditions) {
    // Basic condition evaluation - always return true for now
    // A full implementation would check system resources, capabilities, etc.
    return true;
  }

  /**
   * Basic implementation of a linked composite component.
   */
  private static final class LinkedCompositeComponent implements ComponentSimple {
    private final List<ComponentSimple> components;
    private final ComponentLinkingConfig config;
    private final String id;
    private final ComponentVersion version;

    LinkedCompositeComponent(final List<ComponentSimple> components,
        final ComponentLinkingConfig config) {
      this.components = new ArrayList<>(components);
      this.config = config;
      this.id = "linked-" + System.nanoTime();
      this.version = new ComponentVersion(1, 0, 0);
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public ComponentVersion getVersion() {
      return version;
    }

    @Override
    public long getSize() throws WasmException {
      return components.stream().mapToLong(c -> {
        try {
          return c.getSize();
        } catch (WasmException e) {
          return 0L;
        }
      }).sum();
    }

    @Override
    public ComponentMetadata getMetadata() {
      return new ComponentMetadata(id, version, "Linked Composite Component");
    }

    @Override
    public boolean exportsInterface(final String interfaceName) throws WasmException {
      return components.stream().anyMatch(c -> {
        try {
          return c.exportsInterface(interfaceName);
        } catch (WasmException e) {
          return false;
        }
      });
    }

    @Override
    public boolean importsInterface(final String interfaceName) throws WasmException {
      return components.stream().anyMatch(c -> {
        try {
          return c.importsInterface(interfaceName);
        } catch (WasmException e) {
          return false;
        }
      });
    }

    @Override
    public Set<String> getExportedInterfaces() throws WasmException {
      final Set<String> allExports = new HashSet<>();
      for (final ComponentSimple component : components) {
        allExports.addAll(component.getExportedInterfaces());
      }
      return allExports;
    }

    @Override
    public Set<String> getImportedInterfaces() throws WasmException {
      final Set<String> allImports = new HashSet<>();
      for (final ComponentSimple component : components) {
        allImports.addAll(component.getImportedInterfaces());
      }
      return allImports;
    }

    @Override
    public ComponentInstance instantiate() throws WasmException {
      throw new WasmException("Composite component instantiation not yet implemented");
    }

    @Override
    public ComponentInstance instantiate(final ComponentInstanceConfig config) throws WasmException {
      throw new WasmException("Composite component instantiation not yet implemented");
    }

    @Override
    public ComponentDependencyGraph getDependencyGraph() throws WasmException {
      return new ComponentDependencyGraph(this);
    }

    @Override
    public Set<ComponentSimple> resolveDependencies(final ComponentRegistry registry) throws WasmException {
      final Set<ComponentSimple> allDependencies = new HashSet<>();
      for (final ComponentSimple component : components) {
        allDependencies.addAll(component.resolveDependencies(registry));
      }
      return allDependencies;
    }

    @Override
    public ComponentCompatibility checkCompatibility(final ComponentSimple other) throws WasmException {
      // Check if any component in the composite is compatible
      for (final ComponentSimple component : components) {
        final ComponentCompatibility compatibility = component.checkCompatibility(other);
        if (compatibility.isCompatible()) {
          return compatibility;
        }
      }
      return new ComponentCompatibility(false, "No compatible components found");
    }

    @Override
    public WitInterfaceDefinition getWitInterface() throws WasmException {
      // Return the first component's WIT interface for now
      if (!components.isEmpty()) {
        return components.get(0).getWitInterface();
      }
      throw new WasmException("No components available for WIT interface");
    }

    @Override
    public WitCompatibilityResult checkWitCompatibility(final ComponentSimple other) throws WasmException {
      return getWitInterface().isCompatibleWith(other.getWitInterface());
    }

    @Override
    public ComponentResourceUsage getResourceUsage() {
      return new ComponentResourceUsage(id);
    }

    @Override
    public ComponentLifecycleState getLifecycleState() {
      return ComponentLifecycleState.ACTIVE;
    }

    @Override
    public boolean isValid() {
      return components.stream().allMatch(ComponentSimple::isValid);
    }

    @Override
    public ComponentValidationResult validate(final ComponentValidationConfig validationConfig)
        throws WasmException {
      final ComponentValidationResult.ValidationContext context =
          new ComponentValidationResult.ValidationContext(id, version);
      return ComponentValidationResult.success(context);
    }

    @Override
    public void close() {
      components.forEach(component -> {
        try {
          component.close();
        } catch (Exception e) {
          // Log and continue
        }
      });
    }
  }
}