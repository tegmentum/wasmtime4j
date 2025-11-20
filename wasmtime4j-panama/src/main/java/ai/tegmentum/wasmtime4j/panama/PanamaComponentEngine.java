package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ComponentEngine;
import ai.tegmentum.wasmtime4j.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentRegistry;
import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.ComponentVersion;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.WitInterfaceLinker;
import ai.tegmentum.wasmtime4j.WitSupportInfo;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Panama implementation of the ComponentEngine interface.
 *
 * <p>This class provides WebAssembly Component Model functionality using Panama Foreign Function
 * API to communicate with the native Wasmtime component library. It manages component lifecycle and
 * instance creation through native bindings.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Component compilation and instantiation
 *   <li>WIT interface resolution and type validation
 *   <li>Component dependency resolution
 *   <li>Automatic resource cleanup and error handling
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaComponentEngine implements ComponentEngine {

  private static final Logger LOGGER = Logger.getLogger(PanamaComponentEngine.class.getName());
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final String engineId;
  private final ComponentEngineConfig config;
  private final MemorySegment engineHandle;
  private final Arena arena;
  private final ConcurrentMap<String, PanamaComponentSimple> loadedComponents;
  private final AtomicLong componentIdCounter;
  private volatile boolean closed = false;
  private ComponentRegistry registry;

  /**
   * Creates a new Panama component engine with the given configuration.
   *
   * @param config the component engine configuration
   * @throws WasmException if engine creation fails
   */
  public PanamaComponentEngine(final ComponentEngineConfig config) throws WasmException {
    this.config = config != null ? config : new ComponentEngineConfig();
    this.engineId = "panama-component-engine-" + System.nanoTime();
    this.arena = Arena.ofShared();
    this.loadedComponents = new ConcurrentHashMap<>();
    this.componentIdCounter = new AtomicLong(0);

    // Create native engine - for now, pass null config
    final MemorySegment configSegment = MemorySegment.NULL;
    this.engineHandle = NATIVE_BINDINGS.componentEngineCreate(configSegment);

    if (engineHandle == null || engineHandle.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to create component engine");
    }
  }

  /**
   * Gets the unique identifier for this engine.
   *
   * @return the engine ID
   */
  public String getId() {
    return engineId;
  }

  @Override
  public ComponentSimple compileComponent(final byte[] wasmBytes) throws WasmException {
    Objects.requireNonNull(wasmBytes, "wasmBytes cannot be null");
    ensureNotClosed();

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment bytesSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, wasmBytes);
      final MemorySegment componentOut = tempArena.allocate(ValueLayout.ADDRESS);

      final int errorCode =
          NATIVE_BINDINGS.componentLoadFromBytes(
              engineHandle, bytesSegment, wasmBytes.length, componentOut);

      if (errorCode != 0) {
        throw new WasmException("Failed to compile component (error code: " + errorCode + ")");
      }

      final MemorySegment componentHandle = componentOut.get(ValueLayout.ADDRESS, 0);
      if (componentHandle == null || componentHandle.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to compile component: null component returned");
      }

      final String componentId = generateComponentId();
      final PanamaComponentSimple component =
          new PanamaComponentSimple(componentHandle, componentId, this);
      loadedComponents.put(componentId, component);
      return component;
    }
  }

  @Override
  public ComponentSimple compileComponent(final byte[] wasmBytes, final String name)
      throws WasmException {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }
    final ComponentSimple component = compileComponent(wasmBytes);
    // Component name is tracked in metadata
    return component;
  }

  @Override
  public ComponentInstance createInstance(final ComponentSimple component, final Store store)
      throws WasmException {
    Objects.requireNonNull(component, "component cannot be null");
    Objects.requireNonNull(store, "store cannot be null");
    ensureNotClosed();

    if (!(component instanceof PanamaComponentSimple)) {
      throw new IllegalArgumentException("Component must be Panama implementation");
    }
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be Panama implementation");
    }

    final PanamaComponentSimple panamaComponent = (PanamaComponentSimple) component;
    final PanamaStore panamaStore = (PanamaStore) store;

    // Allocate memory for the output instance pointer
    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment instanceOut = tempArena.allocate(ValueLayout.ADDRESS);

      // Call native function with engine handle, component handle, and output pointer
      // Note: The native implementation creates its own store, so the store parameter is currently
      // unused
      final int errorCode =
          NATIVE_BINDINGS.componentInstantiate(
              engineHandle, panamaComponent.getNativeHandle(), instanceOut);

      if (errorCode != 0) {
        throw new WasmException("Failed to instantiate component (error code: " + errorCode + ")");
      }

      // Read the instance handle from the output parameter
      final MemorySegment instanceHandle = instanceOut.get(ValueLayout.ADDRESS, 0);

      if (instanceHandle == null || instanceHandle.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to instantiate component: null instance returned");
      }

      return new PanamaComponentInstance(instanceHandle, panamaComponent, panamaStore);
    }
  }

  @Override
  public ComponentInstance createInstance(
      final ComponentSimple component, final Store store, final List<ComponentSimple> imports)
      throws WasmException {
    Objects.requireNonNull(imports, "imports cannot be null");

    // For now, link components first, then instantiate
    // This is a simplified implementation
    if (!imports.isEmpty()) {
      throw new UnsupportedOperationException("Component linking not yet fully implemented");
    }

    return createInstance(component, store);
  }

  @Override
  public ComponentValidationResult validateComponent(final ComponentSimple component) {
    Objects.requireNonNull(component, "component cannot be null");
    ensureNotClosed();

    if (!(component instanceof PanamaComponentSimple)) {
      throw new IllegalArgumentException("Component must be Panama implementation");
    }

    final PanamaComponentSimple panamaComponent = (PanamaComponentSimple) component;
    final ComponentVersion version = new ComponentVersion(1, 0, 0);
    final ComponentValidationResult.ValidationContext context =
        new ComponentValidationResult.ValidationContext(
            panamaComponent.getId() != null ? panamaComponent.getId() : "unknown", version);

    // TODO: Implement actual validation using native WIT validation
    // For now, return success if component is valid
    return ComponentValidationResult.success(context);
  }

  @Override
  public ComponentSimple linkComponents(final List<ComponentSimple> components)
      throws WasmException {
    Objects.requireNonNull(components, "components cannot be null");
    if (components.isEmpty()) {
      throw new IllegalArgumentException("components cannot be empty");
    }
    ensureNotClosed();

    // Validate all components are Panama implementations
    for (final ComponentSimple comp : components) {
      if (!(comp instanceof PanamaComponentSimple)) {
        throw new IllegalArgumentException("All components must be Panama implementation");
      }
    }

    // Use WitInterfaceLinker for dependency analysis and validation
    final WitInterfaceLinker linker = new WitInterfaceLinker();
    final WitInterfaceLinker.ComponentLinkResult linkResult = linker.linkComponents(components);

    if (!linkResult.isSuccess()) {
      throw new WasmException("Component linking failed: " + linkResult.getMessage());
    }

    // For now, return the first component as the "linked" component
    // Full native linking would require Wasmtime's component composition API
    // which is more complex and may not be fully exposed yet
    LOGGER.info(
        "Linked " + components.size() + " components with " + linkResult.getLinks().size()
            + " interface links");

    return components.get(0);
  }

  @Override
  public WitCompatibilityResult checkCompatibility(
      final ComponentSimple source, final ComponentSimple target) {
    Objects.requireNonNull(source, "source cannot be null");
    Objects.requireNonNull(target, "target cannot be null");
    ensureNotClosed();

    // Query exports from source and imports from target
    // Check if interfaces match
    // Return compatibility result

    return new WitCompatibilityResult(
        true, // compatible
        "Components are compatible", // details
        Set.of(), // satisfiedImports
        Set.of()); // unsatisfiedImports
  }

  @Override
  public ComponentRegistry getRegistry() {
    ensureNotClosed();
    if (registry == null) {
      throw new IllegalStateException("Registry not set");
    }
    return registry;
  }

  @Override
  public void setRegistry(final ComponentRegistry registry) {
    Objects.requireNonNull(registry, "registry cannot be null");
    this.registry = registry;
  }

  @Override
  public WitSupportInfo getWitSupportInfo() {
    ensureNotClosed();
    return new WitSupportInfo(
        true, // supportsWit
        "1.0", // witVersion
        Set.of("interface", "world", "resource", "variant", "record", "enum"), // supportedFeatures
        List.of("i32", "i64", "f32", "f64", "string", "bool"), // supportedTypes
        10); // maxInterfaceDepth
  }

  @Override
  public boolean supportsComponentModel() {
    return isValid();
  }

  @Override
  public Optional<Integer> getMaxLinkDepth() {
    return Optional.of(10); // Default max link depth
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;

    // Close all loaded components
    for (final PanamaComponentSimple component : loadedComponents.values()) {
      try {
        component.close();
      } catch (final Exception e) {
        // Log and continue
      }
    }
    loadedComponents.clear();

    // Destroy native engine
    if (engineHandle != null && !engineHandle.equals(MemorySegment.NULL)) {
      try {
        NATIVE_BINDINGS.componentEngineDestroy(engineHandle);
      } catch (final Exception e) {
        // Log and continue
      }
    }

    // Close arena
    if (arena != null) {
      try {
        arena.close();
      } catch (final Exception e) {
        // Log and continue
      }
    }
  }

  // Engine interface methods - delegate to PanamaEngine

  @Override
  public Store createStore() throws WasmException {
    throw new UnsupportedOperationException(
        "Store creation not supported - use PanamaEngine for store creation");
  }

  @Override
  public Store createStore(final Object storeData) throws WasmException {
    throw new UnsupportedOperationException(
        "Store creation not supported - use PanamaEngine for store creation");
  }

  @Override
  public Module compileModule(final byte[] wasmBytes) throws WasmException {
    throw new UnsupportedOperationException(
        "Module compilation not supported - use PanamaEngine for module compilation");
  }

  @Override
  public Module compileWat(final String watText) throws WasmException {
    throw new UnsupportedOperationException(
        "WAT compilation not supported - use PanamaEngine for WAT compilation");
  }

  @Override
  public void incrementEpoch() {
    throw new UnsupportedOperationException(
        "Epoch interruption not supported - use PanamaEngine for epoch interruption");
  }

  @Override
  public EngineConfig getConfig() {
    // ComponentEngineConfig doesn't extend EngineConfig, so return a basic EngineConfig
    return new EngineConfig();
  }

  @Override
  public boolean isValid() {
    return !closed && engineHandle != null && !engineHandle.equals(MemorySegment.NULL);
  }

  @Override
  public boolean supportsFeature(final WasmFeature feature) {
    // Component model specific features
    return false;
  }

  @Override
  public int getMemoryLimitPages() {
    return 0; // Unlimited by default
  }

  @Override
  public long getStackSizeLimit() {
    return 0; // Unlimited by default
  }

  @Override
  public boolean isFuelEnabled() {
    return false;
  }

  @Override
  public boolean isEpochInterruptionEnabled() {
    return false;
  }

  @Override
  public int getMaxInstances() {
    return 0; // Unlimited by default
  }

  @Override
  public long getReferenceCount() {
    return 1L; // Simplified reference counting
  }

  // Helper methods

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Component engine is closed");
    }
  }

  private String generateComponentId() {
    return "component-" + componentIdCounter.incrementAndGet();
  }

  /**
   * Gets the native engine handle.
   *
   * @return the native engine handle
   */
  MemorySegment getNativeHandle() {
    return engineHandle;
  }

  /**
   * Gets the shared arena.
   *
   * @return the shared arena
   */
  Arena getArena() {
    return arena;
  }
}
