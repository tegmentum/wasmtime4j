package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Component;
import ai.tegmentum.wasmtime4j.ComponentCompatibility;
import ai.tegmentum.wasmtime4j.ComponentDependencyGraph;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.ComponentLifecycleState;
import ai.tegmentum.wasmtime4j.ComponentMetadata;
import ai.tegmentum.wasmtime4j.ComponentRegistry;
import ai.tegmentum.wasmtime4j.ComponentResourceUsage;
import ai.tegmentum.wasmtime4j.ComponentValidationConfig;
import ai.tegmentum.wasmtime4j.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.ComponentVersion;
import ai.tegmentum.wasmtime4j.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the Component interface.
 *
 * <p>This class wraps a native WebAssembly component handle and provides Component Model
 * functionality through Panama FFI calls to the native Wasmtime library. It uses arena-based
 * resource management for optimal performance and automatic cleanup.
 *
 * @since 1.0.0
 */
public final class PanamaComponentImpl implements Component {

  private static final Logger LOGGER = Logger.getLogger(PanamaComponentImpl.class.getName());

  private final PanamaComponent.PanamaComponentHandle nativeComponent;
  private final PanamaComponent.PanamaComponentEngine engine;
  private final ArenaResourceManager resourceManager;
  private final ComponentMetadata metadata;
  private final String componentId;
  private final ComponentVersion version;

  /**
   * Creates a new Panama component implementation.
   *
   * @param nativeComponent the native component handle
   * @param engine the component engine that created this component
   * @param resourceManager the arena resource manager
   */
  public PanamaComponentImpl(
      final PanamaComponent.PanamaComponentHandle nativeComponent,
      final PanamaComponent.PanamaComponentEngine engine,
      final ArenaResourceManager resourceManager) {
    this(nativeComponent, engine, resourceManager, createDefaultMetadata());
  }

  /**
   * Creates a new Panama component implementation with metadata.
   *
   * @param nativeComponent the native component handle
   * @param engine the component engine that created this component
   * @param resourceManager the arena resource manager
   * @param metadata the component metadata
   */
  public PanamaComponentImpl(
      final PanamaComponent.PanamaComponentHandle nativeComponent,
      final PanamaComponent.PanamaComponentEngine engine,
      final ArenaResourceManager resourceManager,
      final ComponentMetadata metadata) {
    this.nativeComponent = Objects.requireNonNull(nativeComponent, "nativeComponent");
    this.engine = Objects.requireNonNull(engine, "engine");
    this.resourceManager = Objects.requireNonNull(resourceManager, "resourceManager");
    this.metadata = metadata != null ? metadata : createDefaultMetadata();
    this.componentId = "panama-component-" + System.nanoTime();
    this.version = this.metadata.getVersion();
  }

  @Override
  public String getId() {
    return componentId;
  }

  @Override
  public ComponentVersion getVersion() {
    return version;
  }

  @Override
  public long getSize() throws WasmException {
    ensureValid();

    try {
      return nativeComponent.getSize();
    } catch (final Exception e) {
      throw new WasmException("Failed to get component size", e);
    }
  }

  @Override
  public ComponentMetadata getMetadata() {
    return metadata;
  }

  @Override
  public boolean exportsInterface(final String interfaceName) throws WasmException {
    Objects.requireNonNull(interfaceName, "interfaceName");
    ensureValid();

    try {
      return nativeComponent.exportsInterface(interfaceName);
    } catch (final Exception e) {
      throw new WasmException("Failed to check exported interface", e);
    }
  }

  @Override
  public boolean importsInterface(final String interfaceName) throws WasmException {
    Objects.requireNonNull(interfaceName, "interfaceName");
    ensureValid();

    try {
      return nativeComponent.importsInterface(interfaceName);
    } catch (final Exception e) {
      throw new WasmException("Failed to check imported interface", e);
    }
  }

  @Override
  public Set<String> getExportedInterfaces() throws WasmException {
    ensureValid();

    try {
      // For now, return empty set - full implementation would enumerate actual exports
      // This would use native FFI calls to get export names from the component
      return new HashSet<>();
    } catch (final Exception e) {
      throw new WasmException("Failed to get exported interfaces", e);
    }
  }

  @Override
  public Set<String> getImportedInterfaces() throws WasmException {
    ensureValid();

    try {
      // For now, return empty set - full implementation would enumerate actual imports
      // This would use native FFI calls to get import names from the component
      return new HashSet<>();
    } catch (final Exception e) {
      throw new WasmException("Failed to get imported interfaces", e);
    }
  }

  @Override
  public ComponentInstance instantiate() throws WasmException {
    return instantiate(new ComponentInstanceConfig());
  }

  @Override
  public ComponentInstance instantiate(final ComponentInstanceConfig config) throws WasmException {
    Objects.requireNonNull(config, "config");
    ensureValid();

    try {
      final PanamaComponent.PanamaComponentInstanceHandle instanceHandle =
          engine.instantiateComponent(nativeComponent);
      return new PanamaComponentInstanceImpl(instanceHandle, this, resourceManager, config);
    } catch (final Exception e) {
      throw new WasmException("Failed to instantiate component", e);
    }
  }

  @Override
  public ComponentDependencyGraph getDependencyGraph() throws WasmException {
    ensureValid();
    // Return empty dependency graph for now
    return new ComponentDependencyGraph(this);
  }

  @Override
  public Set<Component> resolveDependencies(final ComponentRegistry registry) throws WasmException {
    Objects.requireNonNull(registry, "registry");
    ensureValid();
    // Return empty set for now
    return new HashSet<>();
  }

  @Override
  public ComponentCompatibility checkCompatibility(final Component other) throws WasmException {
    Objects.requireNonNull(other, "other");
    ensureValid();

    // Basic compatibility check based on version
    final boolean compatible = this.version.isCompatibleWith(other.getVersion());
    return new ComponentCompatibility(compatible, compatible ? "Compatible" : "Incompatible");
  }

  @Override
  public WitInterfaceDefinition getWitInterface() throws WasmException {
    ensureValid();

    try {
      // Create a basic WIT interface definition based on component metadata
      // In a full implementation, this would parse actual WIT definitions from the component
      return new PanamaWitInterfaceDefinition(
          "component-interface-" + componentId,
          "1.0.0",
          "ai.tegmentum.wasmtime4j",
          getExportedInterfaces(),
          getImportedInterfaces());
    } catch (final Exception e) {
      throw new WasmException("Failed to get WIT interface", e);
    }
  }

  @Override
  public WitCompatibilityResult checkWitCompatibility(final Component other) throws WasmException {
    Objects.requireNonNull(other, "other");
    ensureValid();

    try {
      final WitInterfaceDefinition thisInterface = this.getWitInterface();
      final WitInterfaceDefinition otherInterface = other.getWitInterface();
      return thisInterface.isCompatibleWith(otherInterface);
    } catch (final Exception e) {
      throw new WasmException("Failed to check WIT compatibility", e);
    }
  }

  @Override
  public ComponentResourceUsage getResourceUsage() {
    return new ComponentResourceUsage(componentId);
  }

  @Override
  public ComponentLifecycleState getLifecycleState() {
    return ComponentLifecycleState.ACTIVE;
  }

  @Override
  public boolean isValid() {
    return nativeComponent.isValid() && engine.isValid();
  }

  @Override
  public ComponentValidationResult validate(final ComponentValidationConfig validationConfig)
      throws WasmException {
    Objects.requireNonNull(validationConfig, "validationConfig");
    ensureValid();

    final ComponentValidationResult.ValidationContext context =
        new ComponentValidationResult.ValidationContext(componentId, version);
    return ComponentValidationResult.success(context);
  }

  @Override
  public void close() {
    try {
      if (nativeComponent != null) {
        nativeComponent.close();
        LOGGER.fine("Closed Panama component: " + componentId);
      }
    } catch (final Exception e) {
      LOGGER.warning("Error closing component: " + e.getMessage());
    }
  }

  private void ensureValid() throws WasmException {
    if (!isValid()) {
      throw new WasmException("Component is no longer valid");
    }
  }

  private static ComponentMetadata createDefaultMetadata() {
    final ComponentVersion version = new ComponentVersion(1, 0, 0);
    return new ComponentMetadata("unknown", version, "Panama Component");
  }
}
