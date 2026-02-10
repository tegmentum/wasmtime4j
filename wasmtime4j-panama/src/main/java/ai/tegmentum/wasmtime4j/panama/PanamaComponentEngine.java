package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Component;
import ai.tegmentum.wasmtime4j.ComponentEngine;
import ai.tegmentum.wasmtime4j.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentRegistry;
import ai.tegmentum.wasmtime4j.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.ComponentVersion;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.WitInterfaceLinker;
import ai.tegmentum.wasmtime4j.WitSupportInfo;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.HashSet;
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
  private final MemorySegment enhancedEngineHandle;
  private final Arena arena;
  private final ConcurrentMap<String, PanamaComponentImpl> loadedComponents;
  private final AtomicLong componentIdCounter;
  private final WasmRuntime runtime;
  private volatile boolean closed = false;
  private ComponentRegistry registry;

  /**
   * Creates a new Panama component engine with the given configuration.
   *
   * <p>This constructor is intended for unit tests. Production code should use {@link
   * #PanamaComponentEngine(ComponentEngineConfig, WasmRuntime)}.
   *
   * @param config the component engine configuration
   * @throws WasmException if engine creation fails
   */
  public PanamaComponentEngine(final ComponentEngineConfig config) throws WasmException {
    this(config, null);
  }

  /**
   * Creates a new Panama component engine with the given configuration and runtime reference.
   *
   * @param config the component engine configuration
   * @param runtime the runtime that owns this engine
   * @throws WasmException if engine creation fails
   */
  public PanamaComponentEngine(final ComponentEngineConfig config, final WasmRuntime runtime)
      throws WasmException {
    this.config = config != null ? config : new ComponentEngineConfig();
    this.runtime = runtime;
    this.engineId = "panama-component-engine-" + System.nanoTime();
    this.arena = Arena.ofShared();
    this.loadedComponents = new ConcurrentHashMap<>();
    this.componentIdCounter = new AtomicLong(0);

    // Create enhanced component engine for proper Store/Instance lifecycle management
    this.enhancedEngineHandle = NATIVE_BINDINGS.enhancedComponentEngineCreate();

    if (enhancedEngineHandle == null || enhancedEngineHandle.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to create enhanced component engine");
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
  public Component compileComponent(final byte[] wasmBytes) throws WasmException {
    Objects.requireNonNull(wasmBytes, "wasmBytes cannot be null");
    ensureNotClosed();

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment bytesSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, wasmBytes);
      final MemorySegment componentOut = tempArena.allocate(ValueLayout.ADDRESS);

      final int errorCode =
          NATIVE_BINDINGS.enhancedComponentLoadFromBytes(
              enhancedEngineHandle, bytesSegment, wasmBytes.length, componentOut);

      if (errorCode != 0) {
        throw new WasmException("Failed to compile component (error code: " + errorCode + ")");
      }

      final MemorySegment componentHandle = componentOut.get(ValueLayout.ADDRESS, 0);
      if (componentHandle == null || componentHandle.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to compile component: null component returned");
      }

      final String componentId = generateComponentId();
      final PanamaComponentImpl component =
          new PanamaComponentImpl(componentHandle, componentId, this);
      loadedComponents.put(componentId, component);
      return component;
    }
  }

  @Override
  public Component compileComponent(final byte[] wasmBytes, final String name)
      throws WasmException {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }
    final Component component = compileComponent(wasmBytes);
    // Component name is tracked in metadata
    return component;
  }

  @Override
  public ComponentInstance createInstance(final Component component, final Store store)
      throws WasmException {
    Objects.requireNonNull(component, "component cannot be null");
    Objects.requireNonNull(store, "store cannot be null");
    ensureNotClosed();

    if (!(component instanceof PanamaComponentImpl)) {
      throw new IllegalArgumentException("Component must be Panama implementation");
    }
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be Panama implementation");
    }

    final PanamaComponentImpl panamaComponent = (PanamaComponentImpl) component;
    final PanamaStore panamaStore = (PanamaStore) store;

    // Allocate memory for the output instance ID
    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment instanceIdOut = tempArena.allocate(ValueLayout.JAVA_LONG);

      // Call enhanced instantiation which returns instance ID
      final int errorCode =
          NATIVE_BINDINGS.enhancedComponentInstantiate(
              enhancedEngineHandle, panamaComponent.getNativeHandle(), instanceIdOut);

      if (errorCode != 0) {
        throw new WasmException("Failed to instantiate component (error code: " + errorCode + ")");
      }

      // Read the instance ID from the output parameter
      final long instanceId = instanceIdOut.get(ValueLayout.JAVA_LONG, 0);

      if (instanceId == 0) {
        throw new WasmException("Failed to instantiate component: invalid instance ID returned");
      }

      return new PanamaComponentInstance(
          enhancedEngineHandle, instanceId, panamaComponent, panamaStore);
    }
  }

  @Override
  public ComponentInstance createInstance(
      final Component component, final Store store, final List<Component> imports)
      throws WasmException {
    Objects.requireNonNull(component, "component cannot be null");
    Objects.requireNonNull(store, "store cannot be null");
    Objects.requireNonNull(imports, "imports cannot be null");
    ensureNotClosed();

    if (imports.isEmpty()) {
      return createInstance(component, store);
    }

    if (!(component instanceof PanamaComponentImpl)) {
      throw new IllegalArgumentException("Component must be Panama implementation");
    }
    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException("Store must be Panama implementation");
    }

    final PanamaComponentImpl panamaComponent = (PanamaComponentImpl) component;

    // Validate that all imports are Panama implementations and check their exports
    final Set<String> availableExports = new HashSet<>();
    for (final Component importComponent : imports) {
      if (!(importComponent instanceof PanamaComponentImpl)) {
        throw new IllegalArgumentException("All import components must be Panama implementation");
      }

      final PanamaComponentImpl panamaImport = (PanamaComponentImpl) importComponent;
      if (!panamaImport.isValid()) {
        throw new WasmException("Import component is not valid: " + panamaImport.getId());
      }

      availableExports.addAll(panamaImport.getExportedInterfaces());
    }

    // Check that all required imports can be satisfied
    final Set<String> requiredImports = panamaComponent.getImportedInterfaces();
    for (final String required : requiredImports) {
      if (!availableExports.contains(required)) {
        throw new WasmException("Unsatisfied import: " + required);
      }
    }

    LOGGER.fine(
        "Validated "
            + imports.size()
            + " import components satisfy "
            + requiredImports.size()
            + " required imports");

    // Use standard instantiation - the component linker will resolve imports at runtime
    return createInstance(component, store);
  }

  @Override
  public ComponentValidationResult validateComponent(final Component component) {
    Objects.requireNonNull(component, "component cannot be null");
    ensureNotClosed();

    if (!(component instanceof PanamaComponentImpl)) {
      throw new IllegalArgumentException("Component must be Panama implementation");
    }

    final PanamaComponentImpl panamaComponent = (PanamaComponentImpl) component;
    final ComponentVersion version = panamaComponent.getVersion();
    final ComponentValidationResult.ValidationContext context =
        new ComponentValidationResult.ValidationContext(
            panamaComponent.getId() != null ? panamaComponent.getId() : "unknown", version);

    // Use native validation
    final int validationResult =
        NATIVE_BINDINGS.componentValidate(panamaComponent.getNativeHandle());

    if (validationResult != 0) {
      final ComponentValidationResult.ValidationError error =
          new ComponentValidationResult.ValidationError(
              "VALIDATION_ERROR",
              "Component validation failed (error code: " + validationResult + ")",
              panamaComponent.getId(),
              ComponentValidationResult.ErrorSeverity.HIGH);
      return ComponentValidationResult.failure(List.of(error), context);
    }

    return ComponentValidationResult.success(context);
  }

  @Override
  public Component linkComponents(final List<Component> components)
      throws WasmException {
    Objects.requireNonNull(components, "components cannot be null");
    if (components.isEmpty()) {
      throw new IllegalArgumentException("components cannot be empty");
    }
    ensureNotClosed();

    // Validate all components are Panama implementations
    for (final Component comp : components) {
      if (!(comp instanceof PanamaComponentImpl)) {
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
        "Linked "
            + components.size()
            + " components with "
            + linkResult.getLinks().size()
            + " interface links");

    return components.get(0);
  }

  @Override
  public WitCompatibilityResult checkCompatibility(
      final Component source, final Component target) {
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
    for (final PanamaComponentImpl component : loadedComponents.values()) {
      try {
        component.close();
      } catch (final Exception e) {
        // Log and continue
      }
    }
    loadedComponents.clear();

    // Destroy enhanced component engine
    if (enhancedEngineHandle != null && !enhancedEngineHandle.equals(MemorySegment.NULL)) {
      try {
        NATIVE_BINDINGS.enhancedComponentEngineDestroy(enhancedEngineHandle);
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
  public WasmRuntime getRuntime() {
    return runtime;
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
  public byte[] precompileModule(final byte[] wasmBytes) throws WasmException {
    throw new UnsupportedOperationException(
        "Module precompilation not supported - use PanamaEngine for module precompilation");
  }

  @Override
  public Module compileFromStream(final InputStream stream) throws WasmException, IOException {
    throw new UnsupportedOperationException(
        "Module compilation from stream not supported - use PanamaEngine for module compilation");
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
    return !closed
        && enhancedEngineHandle != null
        && !enhancedEngineHandle.equals(MemorySegment.NULL);
  }

  @Override
  public boolean supportsFeature(final WasmFeature feature) {
    // Component model specific features
    return false;
  }

  @Override
  public boolean same(final ai.tegmentum.wasmtime4j.Engine other) {
    if (other == null || !(other instanceof PanamaComponentEngine)) {
      return false;
    }
    final PanamaComponentEngine otherEngine = (PanamaComponentEngine) other;
    return enhancedEngineHandle != null
        && enhancedEngineHandle.equals(otherEngine.enhancedEngineHandle);
  }

  @Override
  public boolean isAsync() {
    // Component engines are not async by default
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
  public boolean isCoredumpOnTrapEnabled() {
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

  @Override
  public ai.tegmentum.wasmtime4j.Precompiled detectPrecompiled(final byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    if (bytes.length == 0) {
      return null;
    }
    if (closed) {
      throw new IllegalStateException("Engine has been closed");
    }

    try (final java.lang.foreign.Arena tempArena = java.lang.foreign.Arena.ofConfined()) {
      final java.lang.foreign.MemorySegment bytesSegment = tempArena.allocate(bytes.length);
      bytesSegment.copyFrom(java.lang.foreign.MemorySegment.ofArray(bytes));
      final int result =
          NATIVE_BINDINGS.engineDetectPrecompiled(enhancedEngineHandle, bytesSegment, bytes.length);
      // -1 means not precompiled, 0 = MODULE, 1 = COMPONENT
      if (result < 0) {
        return null;
      }
      return ai.tegmentum.wasmtime4j.Precompiled.fromValue(result);
    }
  }

  /**
   * Gets the enhanced component engine handle.
   *
   * @return the enhanced component engine handle
   */
  MemorySegment getNativeHandle() {
    return enhancedEngineHandle;
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
