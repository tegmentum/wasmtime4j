package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiComponent;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiInstance;
import ai.tegmentum.wasmtime4j.wasi.WasiInstanceState;
import ai.tegmentum.wasmtime4j.wasi.WasiInstanceStats;
import ai.tegmentum.wasmtime4j.wasi.WasiFunctionMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiResource;
import ai.tegmentum.wasmtime4j.wasi.WasiMemoryInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiInstance interface.
 *
 * <p>This class provides a concrete implementation of WASI component instance functionality using
 * JNI bindings to the native Wasmtime component model. It manages instance lifecycle, function
 * calling, resource management, and execution state through JNI calls.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Function calling with parameter marshaling
 *   <li>Resource lifecycle management
 *   <li>Execution state tracking and control
 *   <li>Memory usage monitoring
 *   <li>Comprehensive error handling and cleanup
 *   <li>Thread-safe operations with defensive programming
 * </ul>
 *
 * <p>This implementation follows the unified API pattern while delegating to JNI-specific
 * component instance wrappers for native interactions.
 *
 * @since 1.0.0
 */
public final class JniWasiInstance implements WasiInstance {

  private static final Logger LOGGER = Logger.getLogger(JniWasiInstance.class.getName());
  
  private static final AtomicLong NEXT_INSTANCE_ID = new AtomicLong(1);

  private final long instanceId;
  private final JniWasiComponent component;
  private final JniComponent.JniComponentInstanceHandle instanceHandle;
  private final WasiConfig config;
  private final Instant createdAt;
  private final Map<String, Object> properties;
  private final List<WasiResource> resources;
  
  private volatile WasiInstanceState state;
  private volatile Instant lastActivityAt;
  private volatile boolean closed = false;

  // Cached metadata to avoid repeated native calls
  private volatile List<String> cachedExportedFunctions;
  private volatile List<String> cachedExportedInterfaces;

  /**
   * Creates a new JNI WASI instance with the specified component and instance handle.
   *
   * @param component the parent component that created this instance
   * @param instanceHandle the native component instance handle
   * @param config the configuration used to create this instance
   * @throws IllegalArgumentException if any parameter is null
   */
  public JniWasiInstance(
      final JniWasiComponent component,
      final JniComponent.JniComponentInstanceHandle instanceHandle,
      final WasiConfig config) {
    this.instanceId = NEXT_INSTANCE_ID.getAndIncrement();
    this.component = Objects.requireNonNull(component, "Component cannot be null");
    this.instanceHandle = Objects.requireNonNull(instanceHandle, "Instance handle cannot be null");
    this.config = Objects.requireNonNull(config, "Configuration cannot be null");
    this.createdAt = Instant.now();
    this.properties = new ConcurrentHashMap<>();
    this.resources = new ArrayList<>();
    this.state = WasiInstanceState.CREATED;

    LOGGER.fine("Created JNI WASI instance with ID: " + instanceId);
  }

  @Override
  public long getId() {
    return instanceId;
  }

  @Override
  public WasiComponent getComponent() {
    return component;
  }

  @Override
  public WasiConfig getConfig() {
    return config;
  }

  @Override
  public WasiInstanceState getState() {
    return state;
  }

  @Override
  public Instant getCreatedAt() {
    return createdAt;
  }

  @Override
  public Optional<Instant> getLastActivityAt() {
    return Optional.ofNullable(lastActivityAt);
  }

  @Override
  public Object call(final String functionName, final Object... parameters) throws WasmException {
    return call(functionName, null, parameters);
  }

  @Override
  public Object call(final String functionName, final Duration timeout, final Object... parameters)
      throws WasmException {
    Objects.requireNonNull(functionName, "Function name cannot be null");
    if (functionName.trim().isEmpty()) {
      throw new IllegalArgumentException("Function name cannot be empty");
    }
    ensureNotClosed();
    ensureCallableState();

    updateLastActivity();

    try {
      setState(WasiInstanceState.RUNNING);
      
      // TODO: Implement actual function calling through native layer
      // For now, simulate basic function call
      LOGGER.fine("Calling function: " + functionName + " with " + parameters.length + " parameters");
      
      // This would be replaced with actual JNI calls to invoke the function
      // Object result = nativeCallFunction(instanceHandle.getNativeHandle(), functionName, parameters, timeout);
      
      // For now, return null to indicate successful void call
      Object result = null;
      
      setState(WasiInstanceState.CREATED); // Return to ready state
      return result;

    } catch (final Exception e) {
      setState(WasiInstanceState.ERROR);
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Function call failed: " + functionName, e);
    }
  }

  @Override
  public CompletableFuture<Object> callAsync(final String functionName, final Object... parameters) {
    Objects.requireNonNull(functionName, "Function name cannot be null");
    if (functionName.trim().isEmpty()) {
      throw new IllegalArgumentException("Function name cannot be empty");
    }
    ensureNotClosed();
    ensureCallableState();

    return CompletableFuture.supplyAsync(() -> {
      try {
        return call(functionName, parameters);
      } catch (WasmException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public List<String> getExportedFunctions() throws WasmException {
    ensureNotClosed();

    if (cachedExportedFunctions == null) {
      synchronized (this) {
        if (cachedExportedFunctions == null) {
          cachedExportedFunctions = extractExportedFunctions();
        }
      }
    }
    return new ArrayList<>(cachedExportedFunctions);
  }

  @Override
  public List<String> getExportedInterfaces() throws WasmException {
    ensureNotClosed();

    if (cachedExportedInterfaces == null) {
      synchronized (this) {
        if (cachedExportedInterfaces == null) {
          cachedExportedInterfaces = extractExportedInterfaces();
        }
      }
    }
    return new ArrayList<>(cachedExportedInterfaces);
  }

  @Override
  public WasiFunctionMetadata getFunctionMetadata(final String functionName) throws WasmException {
    Objects.requireNonNull(functionName, "Function name cannot be null");
    if (functionName.trim().isEmpty()) {
      throw new IllegalArgumentException("Function name cannot be empty");
    }
    ensureNotClosed();

    // Check if function exists
    if (!getExportedFunctions().contains(functionName)) {
      throw new WasmException("Function not found in exports: " + functionName);
    }

    // TODO: Extract detailed metadata from native layer
    // For now, return basic metadata
    return createBasicFunctionMetadata(functionName);
  }

  @Override
  public List<WasiResource> getResources() {
    ensureNotClosed();
    synchronized (resources) {
      return new ArrayList<>(resources);
    }
  }

  @Override
  public List<WasiResource> getResources(final String resourceType) {
    Objects.requireNonNull(resourceType, "Resource type cannot be null");
    if (resourceType.trim().isEmpty()) {
      throw new IllegalArgumentException("Resource type cannot be empty");
    }
    ensureNotClosed();

    synchronized (resources) {
      return resources.stream()
          .filter(resource -> resourceType.equals(resource.getType()))
          .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
  }

  @Override
  public Optional<WasiResource> getResource(final long resourceId) {
    ensureNotClosed();
    synchronized (resources) {
      return resources.stream()
          .filter(resource -> resource.getId() == resourceId)
          .findFirst();
    }
  }

  @Override
  public WasiResource createResource(final String resourceType, final Object... parameters)
      throws WasmException {
    Objects.requireNonNull(resourceType, "Resource type cannot be null");
    if (resourceType.trim().isEmpty()) {
      throw new IllegalArgumentException("Resource type cannot be empty");
    }
    ensureNotClosed();

    updateLastActivity();

    try {
      // TODO: Implement actual resource creation through native layer
      // For now, create a placeholder resource
      WasiResource resource = createPlaceholderResource(resourceType, parameters);
      
      synchronized (resources) {
        resources.add(resource);
      }
      
      LOGGER.fine("Created resource of type: " + resourceType + " with ID: " + resource.getId());
      return resource;

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create resource: " + resourceType, e);
    }
  }

  @Override
  public WasiInstanceStats getStats() {
    ensureNotClosed();
    return createInstanceStats();
  }

  @Override
  public WasiMemoryInfo getMemoryInfo() {
    ensureNotClosed();
    return createMemoryInfo();
  }

  @Override
  public void suspend() throws WasmException {
    ensureNotClosed();
    if (state == WasiInstanceState.SUSPENDED) {
      return; // Already suspended
    }
    
    try {
      // TODO: Implement actual suspension through native layer
      setState(WasiInstanceState.SUSPENDED);
      LOGGER.fine("Suspended instance: " + instanceId);
    } catch (final Exception e) {
      throw new WasmException("Failed to suspend instance", e);
    }
  }

  @Override
  public void resume() throws WasmException {
    ensureNotClosed();
    if (state != WasiInstanceState.SUSPENDED) {
      throw new IllegalStateException("Instance is not suspended");
    }
    
    try {
      // TODO: Implement actual resumption through native layer
      setState(WasiInstanceState.CREATED);
      updateLastActivity();
      LOGGER.fine("Resumed instance: " + instanceId);
    } catch (final Exception e) {
      throw new WasmException("Failed to resume instance", e);
    }
  }

  @Override
  public void terminate() throws WasmException {
    ensureNotClosed();
    
    try {
      // TODO: Implement actual termination through native layer
      setState(WasiInstanceState.TERMINATED);
      LOGGER.fine("Terminated instance: " + instanceId);
    } catch (final Exception e) {
      throw new WasmException("Failed to terminate instance", e);
    }
  }

  @Override
  public boolean isValid() {
    return !closed && instanceHandle.isValid() && component.isValid();
  }

  @Override
  public boolean isExecuting() {
    return state == WasiInstanceState.RUNNING;
  }

  @Override
  public void setProperty(final String key, final Object value) {
    Objects.requireNonNull(key, "Property key cannot be null");
    if (key.trim().isEmpty()) {
      throw new IllegalArgumentException("Property key cannot be empty");
    }
    
    properties.put(key, value);
  }

  @Override
  public Optional<Object> getProperty(final String key) {
    Objects.requireNonNull(key, "Property key cannot be null");
    if (key.trim().isEmpty()) {
      throw new IllegalArgumentException("Property key cannot be empty");
    }
    
    return Optional.ofNullable(properties.get(key));
  }

  @Override
  public Map<String, Object> getProperties() {
    return Map.copyOf(properties);
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      setState(WasiInstanceState.TERMINATED);

      // Clear caches
      cachedExportedFunctions = null;
      cachedExportedInterfaces = null;

      // Close all resources
      synchronized (resources) {
        for (WasiResource resource : resources) {
          try {
            resource.close();
          } catch (Exception e) {
            LOGGER.warning("Error closing resource " + resource.getId() + ": " + e.getMessage());
          }
        }
        resources.clear();
      }

      // Close instance handle
      try {
        instanceHandle.close();
      } catch (Exception e) {
        LOGGER.warning("Error closing instance handle: " + e.getMessage());
      }

      properties.clear();
      LOGGER.fine("Closed JNI WASI instance: " + instanceId);
    }
  }

  /**
   * Gets the underlying JNI component instance handle for internal use.
   *
   * @return the JNI component instance handle
   */
  JniComponent.JniComponentInstanceHandle getInstanceHandle() {
    ensureNotClosed();
    return instanceHandle;
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Instance has been closed");
    }
  }

  private void ensureCallableState() {
    if (state == WasiInstanceState.TERMINATED) {
      throw new IllegalStateException("Instance has been terminated");
    }
    if (state == WasiInstanceState.ERROR) {
      throw new IllegalStateException("Instance is in error state");
    }
  }

  private void updateLastActivity() {
    lastActivityAt = Instant.now();
  }

  private void setState(final WasiInstanceState newState) {
    this.state = newState;
  }

  /**
   * Extracts the list of exported functions from the instance.
   *
   * @return list of exported function names
   * @throws WasmException if extraction fails
   */
  private List<String> extractExportedFunctions() throws WasmException {
    try {
      // TODO: Implement actual function extraction from native layer
      // For now, return empty list as placeholder
      List<String> functions = new ArrayList<>();
      
      // This would be replaced with actual native calls to extract functions
      // functions.add("process");
      // functions.add("initialize");
      
      LOGGER.fine("Extracted " + functions.size() + " exported functions from instance");
      return functions;

    } catch (final Exception e) {
      throw new WasmException("Failed to extract exported functions", e);
    }
  }

  /**
   * Extracts the list of exported interfaces from the instance.
   *
   * @return list of exported interface names
   * @throws WasmException if extraction fails
   */
  private List<String> extractExportedInterfaces() throws WasmException {
    try {
      // Use the component's exports as the basis for instance interfaces
      return component.getExports();
    } catch (final Exception e) {
      throw new WasmException("Failed to extract exported interfaces", e);
    }
  }

  /**
   * Creates basic function metadata for testing purposes.
   *
   * @param functionName the function name
   * @return basic function metadata
   */
  private WasiFunctionMetadata createBasicFunctionMetadata(final String functionName) {
    // TODO: Implement actual metadata extraction
    // For now, create minimal metadata structure
    return new WasiFunctionMetadata() {
      @Override
      public String getName() {
        return functionName;
      }

      @Override
      public List<String> getParameterTypes() {
        return new ArrayList<>(); // Not extracted yet
      }

      @Override
      public List<String> getReturnTypes() {
        return new ArrayList<>(); // Not extracted yet
      }

      @Override
      public boolean isAsync() {
        return false; // Assume synchronous for now
      }
    };
  }

  /**
   * Creates a placeholder resource for testing purposes.
   *
   * @param resourceType the resource type
   * @param parameters creation parameters
   * @return a placeholder resource
   */
  private WasiResource createPlaceholderResource(final String resourceType, final Object... parameters) {
    // TODO: Implement actual resource creation
    // For now, create a basic placeholder resource
    return new WasiResource() {
      private static final AtomicLong NEXT_RESOURCE_ID = new AtomicLong(1);
      private final long resourceId = NEXT_RESOURCE_ID.getAndIncrement();
      private volatile boolean resourceClosed = false;

      @Override
      public long getId() {
        return resourceId;
      }

      @Override
      public String getType() {
        return resourceType;
      }

      @Override
      public boolean isValid() {
        return !resourceClosed;
      }

      @Override
      public void close() {
        resourceClosed = true;
      }
    };
  }

  /**
   * Creates instance statistics.
   *
   * @return instance statistics
   */
  private WasiInstanceStats createInstanceStats() {
    return new WasiInstanceStats() {
      @Override
      public long getExecutionTimeNanos() {
        return 0; // Not tracked yet
      }

      @Override
      public long getFunctionCallCount() {
        return 0; // Not tracked yet
      }

      @Override
      public long getResourceCount() {
        synchronized (resources) {
          return resources.size();
        }
      }

      @Override
      public long getMemoryUsageBytes() {
        return 0; // Not tracked yet
      }

      @Override
      public long getErrorCount() {
        return 0; // Not tracked yet
      }
    };
  }

  /**
   * Creates memory information.
   *
   * @return memory information
   */
  private WasiMemoryInfo createMemoryInfo() {
    return new WasiMemoryInfo() {
      @Override
      public long getAllocatedBytes() {
        return 0; // Not tracked yet
      }

      @Override
      public long getPeakBytes() {
        return 0; // Not tracked yet
      }

      @Override
      public long getLimitBytes() {
        return config.getMemoryLimit().orElse(0L);
      }

      @Override
      public double getUsageRatio() {
        long limit = getLimitBytes();
        if (limit <= 0) {
          return 0.0;
        }
        return (double) getAllocatedBytes() / limit;
      }
    };
  }
}