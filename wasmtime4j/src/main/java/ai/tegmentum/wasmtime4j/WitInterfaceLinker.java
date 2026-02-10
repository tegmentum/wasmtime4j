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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * WIT interface linker for component composition and linking.
 *
 * <p>This class provides comprehensive support for composing and linking WebAssembly components
 * through their WIT interfaces, including dependency resolution, interface compatibility
 * validation, and runtime linking.
 *
 * @since 1.0.0
 */
public final class WitInterfaceLinker {

  private static final Logger LOGGER = Logger.getLogger(WitInterfaceLinker.class.getName());

  private final WitTypeValidator validator;
  private final WitFunctionBinder functionBinder;
  private final Map<String, InterfaceBinding> interfaceBindings;
  private final Map<String, ComponentLink> componentLinks;

  /** Creates a new WIT interface linker. */
  public WitInterfaceLinker() {
    this.validator = new WitTypeValidator();
    this.functionBinder = new WitFunctionBinder();
    this.interfaceBindings = new ConcurrentHashMap<>();
    this.componentLinks = new ConcurrentHashMap<>();
  }

  /**
   * Links a list of components together by resolving their import/export dependencies.
   *
   * @param components the components to link
   * @return the linking result
   * @throws WasmException if linking fails
   */
  public ComponentLinkResult linkComponents(final List<Component> components)
      throws WasmException {
    Objects.requireNonNull(components, "components");
    if (components.isEmpty()) {
      throw new IllegalArgumentException("Components list cannot be empty");
    }

    try {
      LOGGER.info("Starting component linking for " + components.size() + " components");

      // Analyze dependencies
      final DependencyAnalysis analysis = analyzeDependencies(components);

      // Validate compatibility
      final LinkCompatibilityResult compatibility = validateLinkCompatibility(analysis);
      if (!compatibility.isCompatible()) {
        throw new WasmException(
            "Component linking incompatible: " + compatibility.getErrorMessage());
      }

      // Create component links
      final List<ComponentLink> links = createComponentLinks(analysis);

      // Generate linking manifest
      final LinkingManifest manifest = createLinkingManifest(components, links);

      LOGGER.info(
          "Successfully linked "
              + components.size()
              + " components with "
              + links.size()
              + " interface links");

      return new ComponentLinkResult(true, "Linking successful", components, links, manifest);

    } catch (final Exception e) {
      LOGGER.severe("Component linking failed: " + e.getMessage());
      throw new WasmException("Failed to link components", e);
    }
  }

  /**
   * Validates interface compatibility between two components.
   *
   * @param provider the provider component (exports interface)
   * @param consumer the consumer component (imports interface)
   * @param interfaceName the interface name
   * @return compatibility result
   * @throws WasmException if validation fails
   */
  public InterfaceCompatibilityResult validateInterfaceCompatibility(
      final Component provider, final Component consumer, final String interfaceName)
      throws WasmException {

    Objects.requireNonNull(provider, "provider");
    Objects.requireNonNull(consumer, "consumer");
    Objects.requireNonNull(interfaceName, "interfaceName");

    try {
      // Check if provider exports the interface
      if (!provider.exportsInterface(interfaceName)) {
        return new InterfaceCompatibilityResult(
            false, "Provider does not export interface: " + interfaceName, List.of());
      }

      // Check if consumer imports the interface
      if (!consumer.importsInterface(interfaceName)) {
        return new InterfaceCompatibilityResult(
            false, "Consumer does not import interface: " + interfaceName, List.of());
      }

      // Get interface definitions
      final WitInterfaceDefinition providerInterface =
          extractInterfaceDefinition(provider, interfaceName);
      final WitInterfaceDefinition consumerInterface =
          extractInterfaceDefinition(consumer, interfaceName);

      // Validate interface compatibility
      final WitCompatibilityResult witCompatibility =
          providerInterface.isCompatibleWith(consumerInterface);
      if (!witCompatibility.isCompatible()) {
        return new InterfaceCompatibilityResult(
            false,
            "Interface definitions are incompatible: " + witCompatibility.getDetails(),
            List.of());
      }

      // Detailed function compatibility check
      final List<String> issues =
          validateFunctionCompatibility(providerInterface, consumerInterface);

      final boolean compatible = issues.isEmpty();
      final String message =
          compatible ? "Interfaces are compatible" : "Compatibility issues found";

      return new InterfaceCompatibilityResult(compatible, message, issues);

    } catch (final Exception e) {
      throw new WasmException("Failed to validate interface compatibility", e);
    }
  }

  /**
   * Binds an interface between two components.
   *
   * @param provider the provider component
   * @param consumer the consumer component
   * @param interfaceName the interface name
   * @throws WasmException if binding fails
   */
  public void bindInterface(
      final Component provider, final Component consumer, final String interfaceName)
      throws WasmException {

    Objects.requireNonNull(provider, "provider");
    Objects.requireNonNull(consumer, "consumer");
    Objects.requireNonNull(interfaceName, "interfaceName");

    // Validate compatibility first
    final InterfaceCompatibilityResult compatibility =
        validateInterfaceCompatibility(provider, consumer, interfaceName);
    if (!compatibility.isCompatible()) {
      throw new WasmException("Cannot bind incompatible interfaces: " + compatibility.getMessage());
    }

    // Create interface binding
    final InterfaceBinding binding =
        new InterfaceBinding(
            provider.getId(),
            consumer.getId(),
            interfaceName,
            extractInterfaceDefinition(provider, interfaceName),
            extractInterfaceDefinition(consumer, interfaceName));

    // Register binding
    final String bindingKey = createBindingKey(provider.getId(), consumer.getId(), interfaceName);
    interfaceBindings.put(bindingKey, binding);

    LOGGER.fine(
        "Bound interface '"
            + interfaceName
            + "' from "
            + provider.getId()
            + " to "
            + consumer.getId());
  }

  /**
   * Unbinds an interface between two components.
   *
   * @param providerId the provider component ID
   * @param consumerId the consumer component ID
   * @param interfaceName the interface name
   * @return true if binding was removed, false if not found
   */
  public boolean unbindInterface(
      final String providerId, final String consumerId, final String interfaceName) {
    final String bindingKey = createBindingKey(providerId, consumerId, interfaceName);
    final InterfaceBinding removed = interfaceBindings.remove(bindingKey);
    if (removed != null) {
      LOGGER.fine(
          "Unbound interface '" + interfaceName + "' from " + providerId + " to " + consumerId);
      return true;
    }
    return false;
  }

  /**
   * Gets all interface bindings.
   *
   * @return list of interface bindings
   */
  public List<InterfaceBinding> getAllBindings() {
    return new ArrayList<>(interfaceBindings.values());
  }

  /**
   * Gets interface bindings for a specific component.
   *
   * @param componentId the component ID
   * @return list of interface bindings
   */
  public List<InterfaceBinding> getBindingsForComponent(final String componentId) {
    return interfaceBindings.values().stream()
        .filter(
            binding ->
                binding.getProviderId().equals(componentId)
                    || binding.getConsumerId().equals(componentId))
        .toList();
  }

  /** Clears all interface bindings. */
  public void clearBindings() {
    interfaceBindings.clear();
    componentLinks.clear();
    LOGGER.info("Cleared all interface bindings and component links");
  }

  /**
   * Analyzes component dependencies.
   *
   * @param components the components to analyze
   * @return dependency analysis result
   * @throws WasmException if analysis fails
   */
  private DependencyAnalysis analyzeDependencies(final List<Component> components)
      throws WasmException {
    final Map<String, Set<String>> exports = new HashMap<>();
    final Map<String, Set<String>> imports = new HashMap<>();
    final Map<String, Component> componentMap = new HashMap<>();

    // Collect imports and exports for each component
    for (final Component component : components) {
      final String componentId = component.getId();
      componentMap.put(componentId, component);

      // Collect exports
      final Set<String> componentExports = component.getExportedInterfaces();
      exports.put(componentId, new HashSet<>(componentExports));

      // Collect imports
      final Set<String> componentImports = component.getImportedInterfaces();
      imports.put(componentId, new HashSet<>(componentImports));
    }

    // Find unsatisfied imports
    final Set<String> allExports = new HashSet<>();
    exports.values().forEach(allExports::addAll);

    final Set<String> allImports = new HashSet<>();
    imports.values().forEach(allImports::addAll);

    final Set<String> unsatisfiedImports = new HashSet<>(allImports);
    unsatisfiedImports.removeAll(allExports);

    return new DependencyAnalysis(componentMap, exports, imports, unsatisfiedImports);
  }

  /**
   * Validates link compatibility.
   *
   * @param analysis the dependency analysis
   * @return compatibility result
   */
  private LinkCompatibilityResult validateLinkCompatibility(final DependencyAnalysis analysis) {
    final List<String> errors = new ArrayList<>();

    // Check for unsatisfied imports
    if (!analysis.getUnsatisfiedImports().isEmpty()) {
      errors.add("Unsatisfied imports: " + String.join(", ", analysis.getUnsatisfiedImports()));
    }

    // Check for circular dependencies
    final Set<String> circularDeps = detectCircularDependencies(analysis);
    if (!circularDeps.isEmpty()) {
      errors.add("Circular dependencies detected: " + String.join(", ", circularDeps));
    }

    final boolean compatible = errors.isEmpty();
    final String message =
        compatible ? "Components are compatible for linking" : String.join("; ", errors);

    return new LinkCompatibilityResult(compatible, message, errors);
  }

  /**
   * Detects circular dependencies.
   *
   * @param analysis the dependency analysis
   * @return set of components involved in circular dependencies
   */
  private Set<String> detectCircularDependencies(final DependencyAnalysis analysis) {
    // Simplified circular dependency detection
    // In a full implementation, this would use a proper graph cycle detection algorithm
    return Set.of(); // No circular dependencies detected for now
  }

  /**
   * Creates component links based on dependency analysis.
   *
   * @param analysis the dependency analysis
   * @return list of component links
   * @throws WasmException if link creation fails
   */
  private List<ComponentLink> createComponentLinks(final DependencyAnalysis analysis)
      throws WasmException {
    final List<ComponentLink> links = new ArrayList<>();

    final Map<String, Set<String>> exports = analysis.getExports();
    final Map<String, Set<String>> imports = analysis.getImports();

    // Create links for each import that has a matching export
    for (final Map.Entry<String, Set<String>> importEntry : imports.entrySet()) {
      final String consumerId = importEntry.getKey();
      final Set<String> consumerImports = importEntry.getValue();

      for (final String interfaceName : consumerImports) {
        // Find a provider for this interface
        for (final Map.Entry<String, Set<String>> exportEntry : exports.entrySet()) {
          final String providerId = exportEntry.getKey();
          final Set<String> providerExports = exportEntry.getValue();

          if (providerExports.contains(interfaceName) && !providerId.equals(consumerId)) {
            final ComponentLink link =
                new ComponentLink(
                    providerId,
                    consumerId,
                    interfaceName,
                    ComponentLinkType.INTERFACE_BINDING,
                    Map.of("linkId", createLinkId(providerId, consumerId, interfaceName)));

            links.add(link);
            componentLinks.put(link.getLinkId(), link);
            break; // Found a provider for this interface
          }
        }
      }
    }

    return links;
  }

  /**
   * Creates a linking manifest.
   *
   * @param components the linked components
   * @param links the component links
   * @return linking manifest
   */
  private LinkingManifest createLinkingManifest(
      final List<Component> components, final List<ComponentLink> links) {

    final Map<String, Object> metadata =
        Map.of(
            "componentCount", components.size(),
            "linkCount", links.size(),
            "timestamp", System.currentTimeMillis());

    return new LinkingManifest(
        components.stream().map(Component::getId).toList(),
        links.stream().map(ComponentLink::getLinkId).toList(),
        metadata);
  }

  /**
   * Extracts interface definition from a component.
   *
   * @param component the component
   * @param interfaceName the interface name
   * @return the interface definition
   * @throws WasmException if extraction fails
   */
  private WitInterfaceDefinition extractInterfaceDefinition(
      final Component component, final String interfaceName) throws WasmException {

    // Get the WIT interface from the component
    final WitInterfaceDefinition witInterface = component.getWitInterface();

    // For now, return the main interface
    // In a full implementation, this would extract the specific named interface
    return witInterface;
  }

  /**
   * Validates function compatibility between two interfaces.
   *
   * @param provider the provider interface
   * @param consumer the consumer interface
   * @return list of compatibility issues
   */
  private List<String> validateFunctionCompatibility(
      final WitInterfaceDefinition provider, final WitInterfaceDefinition consumer) {

    final List<String> issues = new ArrayList<>();
    final List<String> providerFunctions = provider.getFunctionNames();
    final List<String> consumerFunctions = consumer.getFunctionNames();

    // Check if all consumer functions are provided
    for (final String consumerFunction : consumerFunctions) {
      if (!providerFunctions.contains(consumerFunction)) {
        issues.add("Provider missing function: " + consumerFunction);
      }
    }

    // Additional detailed function signature validation would go here
    // For now, this provides basic function name compatibility

    return issues;
  }

  /**
   * Creates a binding key for caching.
   *
   * @param providerId the provider ID
   * @param consumerId the consumer ID
   * @param interfaceName the interface name
   * @return the binding key
   */
  private String createBindingKey(
      final String providerId, final String consumerId, final String interfaceName) {
    return providerId + ":" + consumerId + ":" + interfaceName;
  }

  /**
   * Creates a link ID.
   *
   * @param providerId the provider ID
   * @param consumerId the consumer ID
   * @param interfaceName the interface name
   * @return the link ID
   */
  private String createLinkId(
      final String providerId, final String consumerId, final String interfaceName) {
    return "link-" + providerId + "-" + consumerId + "-" + interfaceName;
  }

  // Result and data classes

  /** Dependency analysis result. */
  private static final class DependencyAnalysis {
    private final Map<String, Component> components;
    private final Map<String, Set<String>> exports;
    private final Map<String, Set<String>> imports;
    private final Set<String> unsatisfiedImports;

    public DependencyAnalysis(
        final Map<String, Component> components,
        final Map<String, Set<String>> exports,
        final Map<String, Set<String>> imports,
        final Set<String> unsatisfiedImports) {
      this.components = Map.copyOf(components);
      this.exports = Map.copyOf(exports);
      this.imports = Map.copyOf(imports);
      this.unsatisfiedImports = Set.copyOf(unsatisfiedImports);
    }

    public Map<String, Component> getComponents() {
      return components;
    }

    public Map<String, Set<String>> getExports() {
      return exports;
    }

    public Map<String, Set<String>> getImports() {
      return imports;
    }

    public Set<String> getUnsatisfiedImports() {
      return unsatisfiedImports;
    }
  }

  /** Link compatibility result. */
  public static final class LinkCompatibilityResult {
    private final boolean compatible;
    private final String message;
    private final List<String> errors;

    /**
     * Creates a new link compatibility result.
     *
     * @param compatible whether the link is compatible
     * @param message descriptive message about compatibility
     * @param errors list of compatibility errors
     */
    public LinkCompatibilityResult(
        final boolean compatible, final String message, final List<String> errors) {
      this.compatible = compatible;
      this.message = message;
      this.errors = List.copyOf(errors);
    }

    public boolean isCompatible() {
      return compatible;
    }

    public String getMessage() {
      return message;
    }

    public List<String> getErrors() {
      return errors;
    }

    public String getErrorMessage() {
      return compatible ? "" : String.join("; ", errors);
    }
  }

  /** Interface compatibility result. */
  public static final class InterfaceCompatibilityResult {
    private final boolean compatible;
    private final String message;
    private final List<String> issues;

    /**
     * Creates a new interface compatibility result.
     *
     * @param compatible whether the interface is compatible
     * @param message descriptive message about compatibility
     * @param issues list of compatibility issues
     */
    public InterfaceCompatibilityResult(
        final boolean compatible, final String message, final List<String> issues) {
      this.compatible = compatible;
      this.message = message;
      this.issues = List.copyOf(issues);
    }

    public boolean isCompatible() {
      return compatible;
    }

    public String getMessage() {
      return message;
    }

    public List<String> getIssues() {
      return issues;
    }
  }

  /** Component link result. */
  public static final class ComponentLinkResult {
    private final boolean success;
    private final String message;
    private final List<Component> components;
    private final List<ComponentLink> links;
    private final LinkingManifest manifest;

    /**
     * Creates a new component link result.
     *
     * @param success whether the linking was successful
     * @param message descriptive message about the result
     * @param components list of linked components
     * @param links list of component links
     * @param manifest linking manifest
     */
    public ComponentLinkResult(
        final boolean success,
        final String message,
        final List<Component> components,
        final List<ComponentLink> links,
        final LinkingManifest manifest) {
      this.success = success;
      this.message = message;
      this.components = List.copyOf(components);
      this.links = List.copyOf(links);
      this.manifest = manifest;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    public List<Component> getComponents() {
      return components;
    }

    public List<ComponentLink> getLinks() {
      return links;
    }

    public LinkingManifest getManifest() {
      return manifest;
    }
  }

  /** Interface binding. */
  public static final class InterfaceBinding {
    private final String providerId;
    private final String consumerId;
    private final String interfaceName;
    private final WitInterfaceDefinition providerInterface;
    private final WitInterfaceDefinition consumerInterface;

    /**
     * Creates a new interface binding.
     *
     * @param providerId identifier of the provider component
     * @param consumerId identifier of the consumer component
     * @param interfaceName name of the interface being bound
     * @param providerInterface provider interface definition
     * @param consumerInterface consumer interface definition
     */
    public InterfaceBinding(
        final String providerId,
        final String consumerId,
        final String interfaceName,
        final WitInterfaceDefinition providerInterface,
        final WitInterfaceDefinition consumerInterface) {
      this.providerId = providerId;
      this.consumerId = consumerId;
      this.interfaceName = interfaceName;
      this.providerInterface = providerInterface;
      this.consumerInterface = consumerInterface;
    }

    public String getProviderId() {
      return providerId;
    }

    public String getConsumerId() {
      return consumerId;
    }

    public String getInterfaceName() {
      return interfaceName;
    }

    public WitInterfaceDefinition getProviderInterface() {
      return providerInterface;
    }

    public WitInterfaceDefinition getConsumerInterface() {
      return consumerInterface;
    }
  }

  /** Component link. */
  public static final class ComponentLink {
    private final String providerId;
    private final String consumerId;
    private final String interfaceName;
    private final ComponentLinkType linkType;
    private final Map<String, Object> metadata;

    /**
     * Creates a new component link.
     *
     * @param providerId identifier of the provider component
     * @param consumerId identifier of the consumer component
     * @param interfaceName name of the interface being linked
     * @param linkType type of the component link
     * @param metadata additional link metadata
     */
    public ComponentLink(
        final String providerId,
        final String consumerId,
        final String interfaceName,
        final ComponentLinkType linkType,
        final Map<String, Object> metadata) {
      this.providerId = providerId;
      this.consumerId = consumerId;
      this.interfaceName = interfaceName;
      this.linkType = linkType;
      this.metadata = Map.copyOf(metadata);
    }

    public String getProviderId() {
      return providerId;
    }

    public String getConsumerId() {
      return consumerId;
    }

    public String getInterfaceName() {
      return interfaceName;
    }

    public ComponentLinkType getLinkType() {
      return linkType;
    }

    public Map<String, Object> getMetadata() {
      return metadata;
    }

    public String getLinkId() {
      return (String) metadata.get("linkId");
    }
  }

  /** Component link type. */
  public enum ComponentLinkType {
    /** Interface binding between components. */
    INTERFACE_BINDING,

    /** Resource sharing between components. */
    RESOURCE_SHARING,

    /** Event communication between components. */
    EVENT_COMMUNICATION
  }

  /** Linking manifest. */
  public static final class LinkingManifest {
    private final List<String> componentIds;
    private final List<String> linkIds;
    private final Map<String, Object> metadata;

    /**
     * Creates a new linking manifest.
     *
     * @param componentIds list of component identifiers
     * @param linkIds list of link identifiers
     * @param metadata additional manifest metadata
     */
    public LinkingManifest(
        final List<String> componentIds,
        final List<String> linkIds,
        final Map<String, Object> metadata) {
      this.componentIds = List.copyOf(componentIds);
      this.linkIds = List.copyOf(linkIds);
      this.metadata = Map.copyOf(metadata);
    }

    public List<String> getComponentIds() {
      return componentIds;
    }

    public List<String> getLinkIds() {
      return linkIds;
    }

    public Map<String, Object> getMetadata() {
      return metadata;
    }
  }
}
