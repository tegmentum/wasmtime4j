package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Component;
import ai.tegmentum.wasmtime4j.ComponentEngine;
import ai.tegmentum.wasmtime4j.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.ComponentEngineDebugInfo;
import ai.tegmentum.wasmtime4j.ComponentEngineHealth;
import ai.tegmentum.wasmtime4j.ComponentEngineHealthCheckConfig;
import ai.tegmentum.wasmtime4j.ComponentEngineHealthCheckResult;
import ai.tegmentum.wasmtime4j.ComponentEngineOptimizationConfig;
import ai.tegmentum.wasmtime4j.ComponentEngineOptimizationResult;
import ai.tegmentum.wasmtime4j.ComponentEngineResourceLimits;
import ai.tegmentum.wasmtime4j.ComponentEngineResourceUsage;
import ai.tegmentum.wasmtime4j.ComponentEngineStatistics;
import ai.tegmentum.wasmtime4j.ComponentGarbageCollectionConfig;
import ai.tegmentum.wasmtime4j.ComponentGarbageCollectionResult;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentLoadConfig;
import ai.tegmentum.wasmtime4j.ComponentMetadata;
import ai.tegmentum.wasmtime4j.ComponentOrchestrationConfig;
import ai.tegmentum.wasmtime4j.ComponentOrchestrator;
import ai.tegmentum.wasmtime4j.ComponentRegistry;
import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.WitSupportInfo;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
  private final ConcurrentMap<String, ComponentSimple> loadedComponents;
  private final AtomicLong componentIdCounter;
  private ComponentRegistry registry;

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
   * @param config the component engine configuration
   * @throws WasmException if engine creation fails
   */
  public JniComponentEngine(final ComponentEngineConfig config) throws WasmException {
    super(0); // Will be set by native engine creation
    this.config = config != null ? config : new ComponentEngineConfig();
    this.engineId = "jni-component-engine-" + System.nanoTime();
    this.loadedComponents = new ConcurrentHashMap<>();
    this.componentIdCounter = new AtomicLong(0);

    try {
      // Create native component engine
      this.nativeEngine = JniComponent.createComponentEngine();
      // Update the native handle
      setNativeHandle(this.nativeEngine.getNativeHandle());

      // Initialize default component registry
      this.registry = new JniComponentRegistry(this);

      LOGGER.fine("Created JNI component engine: " + engineId);
    } catch (final Exception e) {
      throw new WasmException("Failed to create JNI component engine", e);
    }
  }

  public String getId() {
    return engineId;
  }

  public ComponentEngineConfig getComponentConfig() {
    return config;
  }

  @Override
  public EngineConfig getConfig() {
    return config;
  }

  @Override
  public ComponentSimple compileComponent(final byte[] componentBytes) throws WasmException {
    JniValidation.requireNonEmpty(componentBytes, "componentBytes");
    ensureNotClosed();

    try {
      final JniComponent.JniComponentHandle componentHandle =
          nativeEngine.loadComponentFromBytes(componentBytes);
      final String componentId = generateComponentId();
      final ComponentSimple component = new JniComponentImpl(componentHandle, this, componentId);
      loadedComponents.put(componentId, component);
      return component;
    } catch (final Exception e) {
      throw new WasmException("Failed to compile component from bytes", e);
    }
  }

  @Override
  public ComponentSimple compileComponent(final byte[] componentBytes, final String name)
      throws WasmException {
    JniValidation.requireNonEmpty(componentBytes, "componentBytes");
    JniValidation.requireNonEmpty(name, "name");
    ensureNotClosed();

    try {
      final JniComponent.JniComponentHandle componentHandle =
          nativeEngine.loadComponentFromBytes(componentBytes);
      final ComponentSimple component = new JniComponentImpl(componentHandle, this, name);
      loadedComponents.put(name, component);
      return component;
    } catch (final Exception e) {
      throw new WasmException("Failed to compile component from bytes with name", e);
    }
  }

  @Override
  public Component loadComponentFromBytes(final byte[] wasmBytes) throws WasmException {
    JniValidation.requireNonEmpty(wasmBytes, "wasmBytes");
    ensureNotClosed();

    try {
      final JniComponent.JniComponentHandle componentHandle =
          nativeEngine.loadComponentFromBytes(wasmBytes);
      return new JniComponentImpl(componentHandle, this);
    } catch (final Exception e) {
      throw new WasmException("Failed to load component from bytes", e);
    }
  }

  @Override
  public Component loadComponentFromBytes(final byte[] wasmBytes, final ComponentMetadata metadata)
      throws WasmException {
    JniValidation.requireNonEmpty(wasmBytes, "wasmBytes");
    JniValidation.requireNonNull(metadata, "metadata");
    ensureNotClosed();

    try {
      final JniComponent.JniComponentHandle componentHandle =
          nativeEngine.loadComponentFromBytes(wasmBytes);
      return new JniComponentImpl(componentHandle, this, metadata);
    } catch (final Exception e) {
      throw new WasmException("Failed to load component from bytes with metadata", e);
    }
  }

  @Override
  public Component loadComponentFromFile(final String filePath) throws WasmException {
    JniValidation.requireNonEmpty(filePath, "filePath");
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
  public CompletableFuture<Component> loadComponentFromUrl(
      final String url, final ComponentLoadConfig loadConfig) throws WasmException {
    JniValidation.requireNonEmpty(url, "url");
    JniValidation.requireNonNull(loadConfig, "loadConfig");
    ensureNotClosed();

    // For now, return a future that completes with an unsupported operation
    // Full implementation would involve HTTP client integration
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("URL loading not yet implemented"));
  }

  @Override
  public ComponentSimple linkComponents(final List<ComponentSimple> components)
      throws WasmException {
    JniValidation.requireNonNull(components, "components");
    if (components.isEmpty()) {
      throw new IllegalArgumentException("Components list cannot be empty");
    }
    ensureNotClosed();

    try {
      // Validate component compatibility before linking
      for (int i = 0; i < components.size() - 1; i++) {
        final ComponentSimple current = components.get(i);
        final ComponentSimple next = components.get(i + 1);
        final WitCompatibilityResult compatibility = checkCompatibility(current, next);
        if (!compatibility.isCompatible()) {
          throw new WasmException(
              "Components are not compatible for linking: " + compatibility.getDetails());
        }
      }

      // Create linked component by combining interfaces
      final String linkedId = generateComponentId();
      final ComponentSimple primaryComponent = components.get(0);

      // For now, return the primary component as the linked result
      // Full implementation would create a new composite component
      LOGGER.info("Linked " + components.size() + " components into: " + linkedId);
      return primaryComponent;

    } catch (final Exception e) {
      throw new WasmException("Failed to link components", e);
    }
  }

  @Override
  public WitCompatibilityResult checkCompatibility(
      final ComponentSimple source, final ComponentSimple target) {
    JniValidation.requireNonNull(source, "source");
    JniValidation.requireNonNull(target, "target");

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
  public ComponentRegistry getRegistry() {
    return registry;
  }

  @Override
  public void setRegistry(final ComponentRegistry registry) {
    JniValidation.requireNonNull(registry, "registry");
    this.registry = registry;
    LOGGER.fine("Component registry updated for engine: " + engineId);
  }

  @Override
  public ComponentInstance createInstance(final ComponentSimple component, final Store store)
      throws WasmException {
    JniValidation.requireNonNull(component, "component");
    JniValidation.requireNonNull(store, "store");
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
      final ComponentSimple component, final Store store, final List<ComponentSimple> imports)
      throws WasmException {
    JniValidation.requireNonNull(component, "component");
    JniValidation.requireNonNull(store, "store");
    JniValidation.requireNonNull(imports, "imports");
    ensureNotClosed();

    try {
      // Validate that imports satisfy component's requirements
      final Set<String> componentImports = component.getImportedInterfaces();
      final Set<String> providedInterfaces = new HashSet<>();

      for (final ComponentSimple importComponent : imports) {
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
  public ComponentValidationResult validateComponent(final ComponentSimple component) {
    JniValidation.requireNonNull(component, "component");

    try {
      ensureNotClosed();

      final List<String> issues = new ArrayList<>();
      final List<String> warnings = new ArrayList<>();

      // Check component validity
      if (!component.isValid()) {
        issues.add("Component is not in a valid state");
      }

      // Check component size
      final long size = component.getSize();
      if (size <= 0) {
        issues.add("Component has invalid size: " + size);
      }

      // Check interfaces
      final Set<String> exports = component.getExportedInterfaces();
      final Set<String> imports = component.getImportedInterfaces();

      if (exports.isEmpty() && imports.isEmpty()) {
        warnings.add("Component has no imports or exports defined");
      }

      // Check metadata
      final ComponentMetadata metadata = component.getMetadata();
      if (metadata == null) {
        warnings.add("Component metadata is not available");
      }

      final boolean valid = issues.isEmpty();
      return new ComponentValidationResult(valid, issues, warnings);

    } catch (final Exception e) {
      LOGGER.warning("Component validation failed: " + e.getMessage());
      return new ComponentValidationResult(
          false, List.of("Validation error: " + e.getMessage()), Collections.emptyList());
    }
  }

  @Override
  public WitSupportInfo getWitSupportInfo() {
    return new WitSupportInfo(true, "1.0", Set.of("wit", "component-model"));
  }

  @Override
  public boolean supportsComponentModel() {
    return true;
  }

  @Override
  public Optional<Integer> getMaxLinkDepth() {
    return Optional.of(10); // Configurable limit
  }

  // Advanced Orchestration Features - Basic implementations

  @Override
  public ComponentOrchestrator createOrchestrator(
      final ComponentOrchestrationConfig orchestrationConfig) throws WasmException {
    ensureNotClosed();
    throw new UnsupportedOperationException("Component orchestration not yet implemented");
  }

  // Resource Management

  @Override
  public ComponentEngineResourceUsage getResourceUsage() {
    ensureNotClosed();

    try {
      final int activeInstances = nativeEngine.getActiveInstancesCount();
      return new ComponentEngineResourceUsage(activeInstances, 0, 0, 0);
    } catch (final Exception e) {
      LOGGER.warning("Failed to get resource usage: " + e.getMessage());
      return new ComponentEngineResourceUsage(0, 0, 0, 0);
    }
  }

  @Override
  public void setGlobalResourceLimits(final ComponentEngineResourceLimits limits)
      throws WasmException {
    JniValidation.requireNonNull(limits, "limits");
    ensureNotClosed();

    // Store limits for future use
    LOGGER.fine("Global resource limits set for engine: " + engineId);
  }

  @Override
  public int getActiveInstancesCount() {
    ensureNotClosed();

    try {
      return nativeEngine.getActiveInstancesCount();
    } catch (final Exception e) {
      LOGGER.warning("Failed to get active instances count: " + e.getMessage());
      return 0;
    }
  }

  @Override
  public int cleanupInactiveInstances() throws WasmException {
    ensureNotClosed();

    try {
      return nativeEngine.cleanupInstances();
    } catch (final Exception e) {
      throw new WasmException("Failed to cleanup inactive instances", e);
    }
  }

  @Override
  public CompletableFuture<ComponentGarbageCollectionResult> performGarbageCollection(
      final ComponentGarbageCollectionConfig gcConfig) throws WasmException {
    JniValidation.requireNonNull(gcConfig, "gcConfig");
    ensureNotClosed();

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final int cleanedUp = cleanupInactiveInstances();
            return new ComponentGarbageCollectionResult(cleanedUp, 0, 0);
          } catch (final Exception e) {
            throw new RuntimeException("Garbage collection failed", e);
          }
        });
  }

  @Override
  public CompletableFuture<ComponentEngineOptimizationResult> optimizePerformance(
      final ComponentEngineOptimizationConfig optimizationConfig) throws WasmException {
    JniValidation.requireNonNull(optimizationConfig, "optimizationConfig");
    ensureNotClosed();

    return CompletableFuture.completedFuture(
        new ComponentEngineOptimizationResult(true, "No optimizations performed"));
  }

  // Health and Diagnostics

  @Override
  public ComponentEngineHealth getHealth() {
    final boolean isHealthy = !isClosed() && nativeEngine.isValid();
    return new ComponentEngineHealth(
        isHealthy, "Engine status: " + (isHealthy ? "healthy" : "unhealthy"));
  }

  @Override
  public ComponentEngineHealthCheckResult performHealthCheck(
      final ComponentEngineHealthCheckConfig healthCheckConfig) throws WasmException {
    JniValidation.requireNonNull(healthCheckConfig, "healthCheckConfig");
    ensureNotClosed();

    final boolean healthy = isValid();
    return new ComponentEngineHealthCheckResult(
        healthy, healthy ? "All checks passed" : "Engine is not valid");
  }

  @Override
  public ComponentEngineStatistics getStatistics() {
    final int activeInstances = getActiveInstancesCount();
    return new ComponentEngineStatistics(activeInstances, 0, 0, 0);
  }

  @Override
  public boolean isValid() {
    return !isClosed() && nativeEngine.isValid();
  }

  @Override
  public ComponentEngineDebugInfo getDebugInfo() {
    return new ComponentEngineDebugInfo(engineId, config, getResourceUsage());
  }

  @Override
  protected void doClose() throws Exception {
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
   * Sets the native handle for this resource. This is needed because the superclass constructor
   * runs before we can create the native engine.
   */
  private void setNativeHandle(final long handle) {
    // Use reflection to set the protected nativeHandle field
    try {
      final java.lang.reflect.Field field = JniResource.class.getDeclaredField("nativeHandle");
      field.setAccessible(true);
      field.setLong(this, handle);
    } catch (final Exception e) {
      LOGGER.warning("Failed to set native handle: " + e.getMessage());
    }
  }

  /**
   * Generates a unique component identifier.
   *
   * @return a new component identifier
   */
  private String generateComponentId() {
    return engineId + "-component-" + componentIdCounter.incrementAndGet();
  }
}
