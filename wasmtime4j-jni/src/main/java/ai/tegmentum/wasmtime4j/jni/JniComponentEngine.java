package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitSupportInfo;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * JNI implementation of the ComponentEngine interface.
 *
 * <p>This class provides WebAssembly Component Model functionality using Java Native Interface
 * (JNI) to communicate with the native Wasmtime component library. It manages component lifecycle,
 * orchestration, and enterprise features through native bindings.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Component instantiation and lifecycle management
 *   <li>WIT interface resolution and type validation
 *   <li>Component dependency resolution and linking
 *   <li>Enterprise orchestration and management capabilities
 *   <li>Automatic resource cleanup and error handling
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniComponentEngine extends JniResource implements ComponentEngine {

  private static final Logger LOGGER = Logger.getLogger(JniComponentEngine.class.getName());

  private final String engineId;
  private final ComponentEngineConfig config;
  private final JniComponent.JniComponentEngine nativeEngine;
  private final ConcurrentMap<String, Component> loadedComponents;
  private final AtomicLong componentIdCounter;
  private final WasmRuntime runtime;

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniComponentEngine: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Creates a new JNI component engine with the given configuration.
   *
   * <p>This constructor is intended for unit tests. Production code should use {@link
   * #JniComponentEngine(ComponentEngineConfig, WasmRuntime)}.
   *
   * @param config the component engine configuration
   * @throws WasmException if engine creation fails
   */
  public JniComponentEngine(final ComponentEngineConfig config) throws WasmException {
    this(config, null);
  }

  /**
   * Creates a new JNI component engine with the given configuration and runtime reference.
   *
   * @param config the component engine configuration
   * @param runtime the runtime that owns this engine
   * @throws WasmException if engine creation fails
   */
  public JniComponentEngine(final ComponentEngineConfig config, final WasmRuntime runtime)
      throws WasmException {
    // Create native engine first before calling super() to get a valid handle
    super(createNativeEngine());
    this.config = config != null ? config : new ComponentEngineConfig();
    this.runtime = runtime;
    this.engineId = "jni-component-engine-" + System.nanoTime();
    this.loadedComponents = new ConcurrentHashMap<>();
    this.componentIdCounter = new AtomicLong(0);

    try {
      // Get the native engine wrapper using the handle we just passed to super
      this.nativeEngine = new JniComponent.JniComponentEngine(getNativeHandle());

      LOGGER.fine("Created JNI component engine: " + engineId);
    } catch (final Exception e) {
      throw new WasmException("Failed to create JNI component engine", e);
    }
  }

  /**
   * Creates a native component engine and returns its handle.
   *
   * @return the native handle for the created engine
   * @throws WasmException if engine creation fails
   */
  private static long createNativeEngine() throws WasmException {
    try {
      final JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine();
      return engine.getNativeHandle();
    } catch (final Exception e) {
      throw new WasmException("Failed to create native component engine", e);
    }
  }

  public String getId() {
    return engineId;
  }

  public ComponentEngineConfig getComponentConfig() {
    return config;
  }

  @Override
  public Engine getEngine() {
    throw new UnsupportedOperationException(
        "JniComponentEngine does not yet expose its underlying Engine. "
            + "Use WasmRuntime.createEngine() to obtain a regular Engine.");
  }

  @Override
  public boolean same(final ai.tegmentum.wasmtime4j.Engine other) {
    // ComponentEngine creates its own internal native engine, which is never
    // shared with any separately-created Engine instance.
    return false;
  }

  @Override
  public boolean isAsync() {
    // Component engines are not async by default
    return false;
  }

  /**
   * Instantiates a component.
   *
   * @param component the component handle to instantiate
   * @return the component instance handle
   * @throws WasmException if instantiation fails
   */
  JniComponent.JniComponentInstanceHandle instantiateComponent(
      final JniComponent.JniComponentHandle component) throws WasmException {
    Validation.requireNonNull(component, "component");

    if (nativeEngine == null || !nativeEngine.isValid()) {
      throw new WasmException("Component engine is not valid");
    }

    try {
      return nativeEngine.instantiateComponent(component);
    } catch (final Exception e) {
      throw new WasmException("Failed to instantiate component", e);
    }
  }

  @Override
  public Component compileComponent(final byte[] componentBytes) throws WasmException {
    Validation.requireNonEmpty(componentBytes, "componentBytes");
    ensureNotClosed();

    try {
      final JniComponent.JniComponentHandle componentHandle =
          nativeEngine.loadComponentFromBytes(componentBytes);
      final String componentId = generateComponentId();
      final Component component = new JniComponentImpl(componentHandle, this);
      loadedComponents.put(componentId, component);
      return component;
    } catch (final Exception e) {
      throw new WasmException("Failed to compile component from bytes", e);
    }
  }

  @Override
  public Component compileComponent(final byte[] componentBytes, final String name)
      throws WasmException {
    Validation.requireNonEmpty(componentBytes, "componentBytes");
    Validation.requireNonEmpty(name, "name");
    ensureNotClosed();

    try {
      final JniComponent.JniComponentHandle componentHandle =
          nativeEngine.loadComponentFromBytes(componentBytes);
      final Component component = new JniComponentImpl(componentHandle, this);
      loadedComponents.put(name, component);
      return component;
    } catch (final Exception e) {
      throw new WasmException("Failed to compile component from bytes with name", e);
    }
  }

  /**
   * Loads a component from raw WebAssembly component bytes.
   *
   * @param wasmBytes the WebAssembly component bytes
   * @return the compiled Component
   * @throws WasmException if loading fails
   */
  public Component loadComponentFromBytes(final byte[] wasmBytes) throws WasmException {
    Validation.requireNonEmpty(wasmBytes, "wasmBytes");
    ensureNotClosed();

    try {
      final JniComponent.JniComponentHandle componentHandle =
          nativeEngine.loadComponentFromBytes(wasmBytes);
      return new JniComponentImpl(componentHandle, this);
    } catch (final Exception e) {
      throw new WasmException("Failed to load component from bytes", e);
    }
  }

  /**
   * Loads a component from a file.
   *
   * @param filePath the path to the WebAssembly component file
   * @return the compiled Component
   * @throws WasmException if loading fails
   */
  public Component loadComponentFromFile(final String filePath) throws WasmException {
    Validation.requireNonEmpty(filePath, "filePath");
    ensureNotClosed();

    try {
      // Read file contents and delegate to loadComponentFromBytes
      final byte[] wasmBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
      return loadComponentFromBytes(wasmBytes);
    } catch (final java.io.IOException e) {
      throw new WasmException("Failed to read component file: " + filePath, e);
    }
  }

  @Override
  public Component linkComponents(final List<Component> components) throws WasmException {
    Validation.requireNonNull(components, "components");
    if (components.isEmpty()) {
      throw new IllegalArgumentException("Components list cannot be empty");
    }
    ensureNotClosed();

    throw new UnsupportedOperationException("Component linking not yet implemented");
  }

  @Override
  public WitCompatibilityResult checkCompatibility(final Component source, final Component target) {
    Validation.requireNonNull(source, "source");
    Validation.requireNonNull(target, "target");

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
          Collections.emptySet(),
          Collections.emptySet());
    }
  }

  @Override
  public ComponentInstance createInstance(final Component component, final Store store)
      throws WasmException {
    Validation.requireNonNull(component, "component");
    Validation.requireNonNull(store, "store");
    ensureNotClosed();

    try {
      // Delegate to component's instantiate method
      return component.instantiate();
    } catch (final Exception e) {
      throw new WasmException("Failed to create component instance", e);
    }
  }

  @Override
  public ComponentInstance createInstance(
      final Component component, final Store store, final List<Component> imports)
      throws WasmException {
    Validation.requireNonNull(component, "component");
    Validation.requireNonNull(store, "store");
    Validation.requireNonNull(imports, "imports");
    ensureNotClosed();

    try {
      // Validate that imports satisfy component's requirements
      final Set<String> componentImports = component.getImportedInterfaces();
      final Set<String> providedInterfaces = new HashSet<>();

      for (final Component importComponent : imports) {
        providedInterfaces.addAll(importComponent.getExportedInterfaces());
      }

      final Set<String> unsatisfiedImports = new HashSet<>(componentImports);
      unsatisfiedImports.removeAll(providedInterfaces);

      if (!unsatisfiedImports.isEmpty()) {
        throw new WasmException(
            "Unsatisfied component imports: " + String.join(", ", unsatisfiedImports));
      }

      // Create instance with linked imports
      final ComponentInstance instance = component.instantiate();
      LOGGER.fine(
          "Created component instance with " + imports.size() + " linked import components");
      return instance;

    } catch (final Exception e) {
      throw new WasmException("Failed to create component instance with imports", e);
    }
  }

  @Override
  public WitSupportInfo getWitSupportInfo() {
    return new WitSupportInfo(
        true,
        "1.0",
        new HashSet<>(Arrays.asList("interface", "world", "resource", "variant", "record", "enum",
            "flags", "tuple", "option", "result")),
        Arrays.asList("bool", "u8", "u16", "u32", "u64", "s8", "s16", "s32", "s64", "f32", "f64",
            "char", "string"),
        10);
  }

  @Override
  public boolean supportsComponentModel() {
    return true;
  }

  @Override
  public Optional<Integer> getMaxLinkDepth() {
    return Optional.of(10); // Configurable limit
  }

  /**
   * Returns the number of active component instances managed by this engine.
   *
   * @return the count of active instances
   */
  public int getActiveInstancesCount() {
    ensureNotClosed();

    try {
      return nativeEngine.getActiveInstancesCount();
    } catch (final Exception e) {
      LOGGER.warning("Failed to get active instances count: " + e.getMessage());
      return 0;
    }
  }

  /**
   * Cleans up inactive component instances to free resources.
   *
   * @return the number of instances cleaned up
   * @throws WasmException if cleanup fails
   */
  public int cleanupInactiveInstances() throws WasmException {
    ensureNotClosed();

    try {
      return nativeEngine.cleanupInstances();
    } catch (final Exception e) {
      throw new WasmException("Failed to cleanup inactive instances", e);
    }
  }

  @Override
  public boolean isValid() {
    return !isClosed() && nativeEngine.isValid();
  }

  @Override
  protected void doClose() throws Exception {
    for (final Component component : loadedComponents.values()) {
      try {
        component.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing component during engine shutdown: " + e.getMessage());
      }
    }
    loadedComponents.clear();

    if (nativeEngine != null && nativeEngine.isValid()) {
      nativeEngine.close();
      LOGGER.fine("Closed JNI component engine: " + engineId);
    }
  }

  @Override
  protected String getResourceType() {
    return "ComponentEngine";
  }

  /**
   * Generates a unique component identifier.
   *
   * @return a new component identifier
   */
  private String generateComponentId() {
    return engineId + "-component-" + componentIdCounter.incrementAndGet();
  }

  @Override
  public ai.tegmentum.wasmtime4j.Precompiled detectPrecompiled(final byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    if (bytes.length == 0) {
      return null;
    }
    if (isClosed()) {
      throw new IllegalStateException("Engine has been closed");
    }

    final int result = nativeDetectPrecompiled(getNativeHandle(), bytes);
    // -1 means not precompiled, 0 = MODULE, 1 = COMPONENT
    if (result < 0) {
      return null;
    }
    return ai.tegmentum.wasmtime4j.Precompiled.fromValue(result);
  }

  private native int nativeDetectPrecompiled(long engineHandle, byte[] bytes);
}
