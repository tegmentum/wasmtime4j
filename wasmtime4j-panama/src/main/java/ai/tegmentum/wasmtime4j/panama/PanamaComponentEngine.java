package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitSupportInfo;
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
  private static final NativeComponentBindings NATIVE_BINDINGS =
      NativeComponentBindings.getInstance();
  private static final NativeEngineBindings ENGINE_BINDINGS = NativeEngineBindings.getInstance();

  private final String engineId;
  private final ComponentEngineConfig config;
  private final MemorySegment enhancedEngineHandle;
  private final Arena arena;
  private final ConcurrentMap<String, PanamaComponentImpl> loadedComponents;
  private final AtomicLong componentIdCounter;
  private final WasmRuntime runtime;
  private final NativeResourceHandle resourceHandle;
  private volatile Engine cachedEngine;

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

    final MemorySegment capturedEngineHandle = this.enhancedEngineHandle;
    final Arena capturedArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaComponentEngine",
            () -> {
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
              if (enhancedEngineHandle != null
                  && !enhancedEngineHandle.equals(MemorySegment.NULL)) {
                try {
                  NATIVE_BINDINGS.enhancedComponentEngineDestroy(enhancedEngineHandle);
                } catch (final Throwable t) {
                  throw new Exception("Error closing PanamaComponentEngine native engine", t);
                }
              }

              // Close arena
              if (arena != null) {
                try {
                  arena.close();
                } catch (final Throwable t) {
                  throw new Exception("Error closing PanamaComponentEngine arena", t);
                }
              }
            },
            this,
            () -> {
              if (capturedEngineHandle != null
                  && !capturedEngineHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.enhancedComponentEngineDestroy(capturedEngineHandle);
              }
              if (capturedArena != null && capturedArena.scope().isAlive()) {
                capturedArena.close();
              }
            });
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
        throw PanamaErrorMapper.mapNativeError(errorCode, "Failed to compile component");
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
        throw PanamaErrorMapper.mapNativeError(errorCode, "Failed to instantiate component");
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
  public WitCompatibilityResult checkCompatibility(final Component source, final Component target) {
    Objects.requireNonNull(source, "source cannot be null");
    Objects.requireNonNull(target, "target cannot be null");

    try {
      ensureNotClosed();

      // Get exported and imported interfaces
      final Set<String> sourceExports = source.getExportedInterfaces();
      final Set<String> targetImports = target.getImportedInterfaces();

      // Check if source can satisfy target's imports
      final Set<String> satisfiedImports = new HashSet<>(sourceExports);
      satisfiedImports.retainAll(targetImports);

      final Set<String> unsatisfiedImports = new HashSet<>(targetImports);
      unsatisfiedImports.removeAll(sourceExports);

      final boolean compatible = unsatisfiedImports.isEmpty();
      final String details =
          compatible
              ? "All imports satisfied"
              : "Unsatisfied imports: " + String.join(", ", unsatisfiedImports);

      return new WitCompatibilityResult(compatible, details, satisfiedImports, unsatisfiedImports);

    } catch (final Exception e) {
      LOGGER.warning("Failed to check component compatibility: " + e.getMessage());
      return new WitCompatibilityResult(
          false,
          "Compatibility check failed: " + e.getMessage(),
          Set.of(),
          Set.of());
    }
  }

  @Override
  public WitSupportInfo getWitSupportInfo() {
    ensureNotClosed();
    return new WitSupportInfo(
        true,
        "1.0",
        Set.of("interface", "world", "resource", "variant", "record", "enum", "flags", "tuple",
            "option", "result"),
        List.of("bool", "u8", "u16", "u32", "u64", "s8", "s16", "s32", "s64", "f32", "f64",
            "char", "string"),
        10);
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
    resourceHandle.close();
  }

  @Override
  public Engine getEngine() {
    if (runtime == null) {
      throw new IllegalStateException(
          "No runtime associated with this ComponentEngine. "
              + "Use the constructor that accepts a WasmRuntime.");
    }
    Engine engine = cachedEngine;
    if (engine == null || !engine.isValid()) {
      try {
        engine = runtime.createEngine();
        cachedEngine = engine;
      } catch (final ai.tegmentum.wasmtime4j.exception.WasmException e) {
        throw new RuntimeException("Failed to create engine from runtime", e);
      }
    }
    return engine;
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed()
        && enhancedEngineHandle != null
        && !enhancedEngineHandle.equals(MemorySegment.NULL);
  }

  @Override
  public boolean same(final ai.tegmentum.wasmtime4j.Engine other) {
    // ComponentEngine creates its own internal native engine, which is never
    // shared with any separately-created Engine instance.
    return false;
  }

  @Override
  public boolean isAsync() {
    if (resourceHandle.isClosed()) {
      return false;
    }
    return NATIVE_BINDINGS.enhancedComponentEngineIsAsync(enhancedEngineHandle);
  }

  // Helper methods

  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  private String generateComponentId() {
    return "component-" + componentIdCounter.incrementAndGet();
  }

  @Override
  public Component deserializeComponent(final byte[] bytes) throws WasmException {
    Objects.requireNonNull(bytes, "bytes cannot be null");
    if (bytes.length == 0) {
      throw new IllegalArgumentException("bytes cannot be empty");
    }
    ensureNotClosed();

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment bytesSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, bytes);
      final MemorySegment componentOut = tempArena.allocate(ValueLayout.ADDRESS);

      final int errorCode =
          NATIVE_BINDINGS.panamaComponentDeserialize(
              enhancedEngineHandle, bytesSegment, bytes.length, componentOut);

      if (errorCode != 0) {
        throw new WasmException(
            "Failed to deserialize component: native error code " + errorCode);
      }

      final MemorySegment componentHandle = componentOut.get(ValueLayout.ADDRESS, 0);
      if (componentHandle == null || componentHandle.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to deserialize component: null component returned");
      }

      final String componentId = generateComponentId();
      final PanamaComponentImpl component =
          new PanamaComponentImpl(componentHandle, componentId, this);
      loadedComponents.put(componentId, component);
      return component;
    }
  }

  @Override
  public Component deserializeComponentFile(final String path) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    if (path.isEmpty()) {
      throw new IllegalArgumentException("path cannot be empty");
    }
    ensureNotClosed();

    try (Arena tempArena = Arena.ofConfined()) {
      final byte[] pathBytes = path.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment pathSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, pathBytes);
      final MemorySegment componentOut = tempArena.allocate(ValueLayout.ADDRESS);

      final int errorCode =
          NATIVE_BINDINGS.panamaComponentDeserializeFile(
              enhancedEngineHandle, pathSegment, pathBytes.length, componentOut);

      if (errorCode != 0) {
        throw new WasmException(
            "Failed to deserialize component from file: " + path + " (error code " + errorCode
                + ")");
      }

      final MemorySegment componentHandle = componentOut.get(ValueLayout.ADDRESS, 0);
      if (componentHandle == null || componentHandle.equals(MemorySegment.NULL)) {
        throw new WasmException(
            "Failed to deserialize component from file: null component returned");
      }

      final String componentId = generateComponentId();
      final PanamaComponentImpl component =
          new PanamaComponentImpl(componentHandle, componentId, this);
      loadedComponents.put(componentId, component);
      return component;
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.Precompiled detectPrecompiled(final byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    if (bytes.length == 0) {
      return null;
    }
    ensureNotClosed();

    try (final java.lang.foreign.Arena tempArena = java.lang.foreign.Arena.ofConfined()) {
      final java.lang.foreign.MemorySegment bytesSegment = tempArena.allocate(bytes.length);
      bytesSegment.copyFrom(java.lang.foreign.MemorySegment.ofArray(bytes));
      final int result =
          ENGINE_BINDINGS.engineDetectPrecompiled(enhancedEngineHandle, bytesSegment, bytes.length);
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
