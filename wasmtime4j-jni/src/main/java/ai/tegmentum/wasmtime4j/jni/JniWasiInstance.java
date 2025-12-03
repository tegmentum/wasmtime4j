package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WitValueException;
import ai.tegmentum.wasmtime4j.wasi.WasiComponent;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiFileSystemStats;
import ai.tegmentum.wasmtime4j.wasi.WasiFunctionMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiInstance;
import ai.tegmentum.wasmtime4j.wasi.WasiInstanceState;
import ai.tegmentum.wasmtime4j.wasi.WasiInstanceStats;
import ai.tegmentum.wasmtime4j.wasi.WasiMemoryInfo;
import ai.tegmentum.wasmtime4j.wasi.WasiNetworkStats;
import ai.tegmentum.wasmtime4j.wasi.WasiParameterMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiResource;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceState;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceStats;
import ai.tegmentum.wasmtime4j.wasi.WasiTypeMetadata;
import ai.tegmentum.wasmtime4j.wit.WitBool;
import ai.tegmentum.wasmtime4j.wit.WitChar;
import ai.tegmentum.wasmtime4j.wit.WitFloat64;
import ai.tegmentum.wasmtime4j.wit.WitRecord;
import ai.tegmentum.wasmtime4j.wit.WitS32;
import ai.tegmentum.wasmtime4j.wit.WitS64;
import ai.tegmentum.wasmtime4j.wit.WitString;
import ai.tegmentum.wasmtime4j.wit.WitValue;
import ai.tegmentum.wasmtime4j.wit.WitValueMarshaller;
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
 * <p>This implementation follows the unified API pattern while delegating to JNI-specific component
 * instance wrappers for native interactions.
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

      LOGGER.fine(
          "Calling function: "
              + functionName
              + " with "
              + (parameters != null ? parameters.length : 0)
              + " parameters");

      // Convert parameters to WitValues
      final List<WitValue> witValues = new ArrayList<>();
      if (parameters != null) {
        for (final Object param : parameters) {
          witValues.add(convertToWitValue(param));
        }
      }

      // Marshal WitValues for native call
      final List<WitValueMarshaller.MarshalledValue> marshalledParams =
          WitValueMarshaller.marshalAll(witValues);

      // Prepare arrays for native call
      final int[] typeDiscriminators = new int[marshalledParams.size()];
      final byte[][] paramData = new byte[marshalledParams.size()][];
      for (int i = 0; i < marshalledParams.size(); i++) {
        typeDiscriminators[i] = marshalledParams.get(i).getTypeDiscriminator();
        paramData[i] = marshalledParams.get(i).getData();
      }

      // Call native function with engine handle and instance handle
      final Object[] nativeResult =
          JniComponent.nativeComponentInvokeFunction(
              component.getComponentEngine().getNativeHandle(),
              instanceHandle.getNativeHandle(),
              functionName,
              typeDiscriminators,
              paramData);

      Object result = null;
      if (nativeResult != null && nativeResult.length >= 2) {
        // Result format: [typeDiscriminator (Integer), data (byte[])]
        final int resultTypeDiscriminator = (Integer) nativeResult[0];
        final byte[] resultData = (byte[]) nativeResult[1];

        if (resultData != null && resultData.length > 0) {
          // Unmarshal result using WitValueMarshaller
          final WitValue resultWitValue =
              WitValueMarshaller.unmarshal(resultTypeDiscriminator, resultData);
          result = resultWitValue.toJava();
        }
      }

      setState(WasiInstanceState.CREATED); // Return to ready state
      return result;

    } catch (final WitValueException e) {
      setState(WasiInstanceState.ERROR);
      throw new WasmException("WIT value marshalling failed: " + e.getMessage(), e);
    } catch (final Exception e) {
      setState(WasiInstanceState.ERROR);
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Function call failed: " + functionName, e);
    }
  }

  /**
   * Converts a Java object to a WIT value.
   *
   * @param obj the Java object to convert
   * @return the WIT value
   * @throws WitValueException if conversion fails
   */
  private WitValue convertToWitValue(final Object obj) throws WitValueException {
    if (obj == null) {
      throw new WitValueException(
          "Cannot convert null to WIT value", WitValueException.ErrorCode.NULL_VALUE);
    }

    if (obj instanceof WitValue) {
      return (WitValue) obj;
    }

    if (obj instanceof Boolean) {
      return WitBool.of((Boolean) obj);
    }

    if (obj instanceof Integer) {
      return WitS32.of((Integer) obj);
    }

    if (obj instanceof Long) {
      return WitS64.of((Long) obj);
    }

    if (obj instanceof Double || obj instanceof Float) {
      return WitFloat64.of(((Number) obj).doubleValue());
    }

    if (obj instanceof Character) {
      return WitChar.of((Character) obj);
    }

    if (obj instanceof String) {
      return WitString.of((String) obj);
    }

    if (obj instanceof java.util.Map) {
      @SuppressWarnings("unchecked")
      final java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
      final WitRecord.Builder builder = WitRecord.builder();
      for (final java.util.Map.Entry<String, Object> entry : map.entrySet()) {
        final WitValue fieldValue = convertToWitValue(entry.getValue());
        builder.field(entry.getKey(), fieldValue);
      }
      return builder.build();
    }

    throw new WitValueException(
        "Unsupported Java type for WIT conversion: " + obj.getClass().getName(),
        WitValueException.ErrorCode.TYPE_MISMATCH);
  }

  @Override
  public CompletableFuture<Object> callAsync(
      final String functionName, final Object... parameters) {
    Objects.requireNonNull(functionName, "Function name cannot be null");
    if (functionName.trim().isEmpty()) {
      throw new IllegalArgumentException("Function name cannot be empty");
    }
    ensureNotClosed();
    ensureCallableState();

    return CompletableFuture.supplyAsync(
        () -> {
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
      return resources.stream().filter(resource -> resource.getId() == resourceId).findFirst();
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

    if (!state.isSuspendable()) {
      throw new IllegalStateException(
          "Instance cannot be suspended in state: " + state.getDescription());
    }

    try {
      // Mark as suspended - execution will check state on next yield point
      // Note: Component engines don't support epoch-based interruption
      // If currently running, the state change will be detected at the next yield point
      if (state == WasiInstanceState.RUNNING) {
        LOGGER.fine("Suspending running instance: " + instanceId);
      }

      setState(WasiInstanceState.SUSPENDED);
      LOGGER.fine("Suspended instance: " + instanceId);
    } catch (final Exception e) {
      throw new WasmException("Failed to suspend instance", e);
    }
  }

  @Override
  public void resume() throws WasmException {
    ensureNotClosed();
    if (!state.isResumable()) {
      throw new IllegalStateException(
          "Instance cannot be resumed in state: " + state.getDescription());
    }

    try {
      // Transition from SUSPENDED to CREATED (ready state)
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

    // Already terminated or in terminal state
    if (state.isTerminal()) {
      LOGGER.fine("Instance already in terminal state: " + state);
      return;
    }

    try {
      // Mark state transition - component engines don't support epoch-based interruption
      // If currently running, the state change will be detected at the next yield point
      if (state == WasiInstanceState.RUNNING) {
        LOGGER.fine("Terminating running instance: " + instanceId);
      }

      // Clean up all resources
      for (final WasiResource resource : resources) {
        try {
          if (resource.getState() != WasiResourceState.CLOSED) {
            resource.close();
          }
        } catch (final Exception e) {
          LOGGER.warning("Failed to close resource during termination: " + e.getMessage());
        }
      }
      resources.clear();

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
    return new ConcurrentHashMap<>(properties);
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
   * <p>For WASI component instances, the exported functions come from the component's exports. Each
   * instance provides the same set of exported functions as its parent component.
   *
   * @return list of exported function names
   * @throws WasmException if extraction fails
   */
  private List<String> extractExportedFunctions() throws WasmException {
    try {
      // Component instances export the same functions as their parent component
      final List<String> functions = component.getExports();
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
      public Optional<String> getDocumentation() {
        return Optional.empty(); // No documentation available yet
      }

      @Override
      public List<WasiParameterMetadata> getParameters() {
        return new ArrayList<>(); // Not extracted yet
      }

      @Override
      public Optional<WasiTypeMetadata> getReturnType() {
        return Optional.empty(); // Not extracted yet
      }

      @Override
      public boolean canThrow() {
        return true; // Assume functions can throw for safety
      }

      @Override
      public List<String> getThrownExceptionTypes() {
        return new ArrayList<>(); // Not extracted yet
      }

      @Override
      public void validateParameters(final Object... parameters) {
        // Basic validation - check for null parameters
        if (parameters == null) {
          throw new IllegalArgumentException("Parameters cannot be null");
        }
        for (Object param : parameters) {
          if (param == null) {
            throw new IllegalArgumentException("Function parameters cannot be null");
          }
        }
      }
    };
  }

  // Shared resource ID counter for all placeholder resources
  private static final AtomicLong NEXT_RESOURCE_ID = new AtomicLong(1);

  /**
   * Creates a placeholder resource for testing purposes.
   *
   * @param resourceType the resource type
   * @param parameters creation parameters
   * @return a placeholder resource
   */
  private WasiResource createPlaceholderResource(
      final String resourceType, final Object... parameters) {
    // TODO: Implement actual resource creation
    // For now, create a basic placeholder resource
    return new WasiResource() {
      private final AtomicLong resourceIdGenerator = new AtomicLong(1);
      private final long resourceId = resourceIdGenerator.getAndIncrement();
      private final Instant createdAt = Instant.now();
      private volatile boolean resourceClosed = false;
      private volatile Instant lastAccessedAt = null;

      @Override
      public long getId() {
        updateLastAccessed();
        return resourceId;
      }

      @Override
      public String getType() {
        updateLastAccessed();
        return resourceType;
      }

      @Override
      public WasiInstance getOwner() {
        updateLastAccessed();
        return JniWasiInstance.this;
      }

      @Override
      public boolean isOwned() {
        return true; // Assume all created resources are owned
      }

      @Override
      public boolean isValid() {
        return !resourceClosed && JniWasiInstance.this.isValid();
      }

      @Override
      public Instant getCreatedAt() {
        return createdAt;
      }

      @Override
      public Optional<Instant> getLastAccessedAt() {
        return Optional.ofNullable(lastAccessedAt);
      }

      @Override
      public WasiResourceMetadata getMetadata() throws WasmException {
        updateLastAccessed();
        return createPlaceholderResourceMetadata();
      }

      @Override
      public WasiResourceState getState() throws WasmException {
        updateLastAccessed();
        return resourceClosed ? WasiResourceState.CLOSED : WasiResourceState.ACTIVE;
      }

      @Override
      public WasiResourceStats getStats() {
        updateLastAccessed();
        return createPlaceholderResourceStats();
      }

      @Override
      public Object invoke(final String operation, final Object... parameters)
          throws WasmException {
        Objects.requireNonNull(operation, "Operation cannot be null");
        updateLastAccessed();
        throw new WasmException("Operation not supported: " + operation);
      }

      @Override
      public List<String> getAvailableOperations() {
        updateLastAccessed();
        return new ArrayList<>(); // No operations available for placeholder resources
      }

      @Override
      public WasiResourceHandle createHandle() throws WasmException {
        updateLastAccessed();
        throw new WasmException("Handle creation not supported for placeholder resources");
      }

      @Override
      public void transferOwnership(final WasiInstance targetInstance) throws WasmException {
        Objects.requireNonNull(targetInstance, "Target instance cannot be null");
        updateLastAccessed();
        throw new WasmException("Ownership transfer not supported for placeholder resources");
      }

      @Override
      public void close() {
        if (!resourceClosed) {
          resourceClosed = true;
          updateLastAccessed();
        }
      }

      private void updateLastAccessed() {
        lastAccessedAt = Instant.now();
      }
    };
  }

  /**
   * Creates placeholder resource metadata.
   *
   * @return placeholder resource metadata
   */
  private WasiResourceMetadata createPlaceholderResourceMetadata() {
    return new WasiResourceMetadata() {
      @Override
      public String getResourceType() {
        return "placeholder";
      }

      @Override
      public long getResourceId() {
        return instanceId; // Use instance ID as placeholder
      }

      @Override
      public Instant getCreatedAt() {
        return createdAt;
      }

      @Override
      public Optional<Instant> getLastModifiedAt() {
        return Optional.empty();
      }

      @Override
      public Optional<Long> getSize() {
        return Optional.empty(); // Size not applicable for placeholder
      }

      @Override
      public Map<String, Object> getProperties() {
        return new ConcurrentHashMap<>();
      }

      @Override
      public boolean hasCapability(final String capability) {
        return false; // No capabilities for placeholder resources
      }
    };
  }

  /**
   * Creates placeholder resource statistics.
   *
   * @return placeholder resource statistics
   */
  private WasiResourceStats createPlaceholderResourceStats() {
    return new WasiResourceStats() {
      @Override
      public long getAccessCount() {
        return 0;
      }

      @Override
      public Duration getTotalUsageTime() {
        return Duration.ZERO;
      }

      @Override
      public long getOperationCount() {
        return 0;
      }

      @Override
      public long getErrorCount() {
        return 0;
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
      private final Instant collectedAt = Instant.now();

      @Override
      public Instant getCollectedAt() {
        return collectedAt;
      }

      @Override
      public long getInstanceId() {
        return instanceId;
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
      public Duration getUptime() {
        return Duration.between(createdAt, Instant.now());
      }

      @Override
      public Duration getExecutionTime() {
        return Duration.ZERO; // Not tracked yet
      }

      @Override
      public long getFunctionCallCount() {
        return 0; // Not tracked yet
      }

      @Override
      public Map<String, Long> getFunctionCallStats() {
        return new ConcurrentHashMap<>(); // Not tracked yet
      }

      @Override
      public Map<String, Duration> getFunctionExecutionTimeStats() {
        return new ConcurrentHashMap<>(); // Not tracked yet
      }

      @Override
      public long getCurrentMemoryUsage() {
        return 0; // Not tracked yet
      }

      @Override
      public long getPeakMemoryUsage() {
        return 0; // Not tracked yet
      }

      @Override
      public long getMemoryAllocationCount() {
        return 0; // Not tracked yet
      }

      @Override
      public long getTotalMemoryAllocated() {
        return 0; // Not tracked yet
      }

      @Override
      public int getCurrentResourceCount() {
        synchronized (resources) {
          return resources.size();
        }
      }

      @Override
      public int getPeakResourceCount() {
        return getCurrentResourceCount(); // Simplified for now
      }

      @Override
      public long getTotalResourcesCreated() {
        return getCurrentResourceCount(); // Simplified for now
      }

      @Override
      public Map<String, Integer> getResourceUsageByType() {
        Map<String, Integer> usage = new ConcurrentHashMap<>();
        synchronized (resources) {
          for (WasiResource resource : resources) {
            usage.merge(resource.getType(), 1, Integer::sum);
          }
        }
        return usage;
      }

      @Override
      public long getErrorCount() {
        return 0; // Not tracked yet
      }

      @Override
      public Map<String, Long> getErrorStats() {
        return new ConcurrentHashMap<>(); // Not tracked yet
      }

      @Override
      public long getSuspensionCount() {
        return 0; // Not tracked yet
      }

      @Override
      public Duration getTotalSuspensionTime() {
        return Duration.ZERO; // Not tracked yet
      }

      @Override
      public long getAsyncOperationCount() {
        return 0; // Not tracked yet
      }

      @Override
      public int getPendingAsyncOperationCount() {
        return 0; // Not tracked yet
      }

      @Override
      public WasiFileSystemStats getFileSystemStats() {
        return createPlaceholderFileSystemStats();
      }

      @Override
      public WasiNetworkStats getNetworkStats() {
        return createPlaceholderNetworkStats();
      }

      @Override
      public Duration getAverageExecutionTime() {
        long callCount = getFunctionCallCount();
        if (callCount == 0) {
          return Duration.ZERO;
        }
        return getExecutionTime().dividedBy(callCount);
      }

      @Override
      public double getThroughput() {
        Duration executionTime = getExecutionTime();
        if (executionTime.isZero()) {
          return 0.0;
        }
        double seconds = executionTime.toMillis() / 1000.0;
        if (seconds <= 0.0) {
          return 0.0;
        }
        return (double) getFunctionCallCount() / seconds;
      }

      @Override
      public double getMemoryEfficiency() {
        long memoryUsage = getCurrentMemoryUsage();
        if (memoryUsage == 0) {
          return 1.0; // Perfect efficiency if no memory used
        }
        return (double) getFunctionCallCount() / memoryUsage;
      }

      @Override
      public Map<String, Object> getCustomProperties() {
        return new ConcurrentHashMap<>();
      }

      @Override
      public String getSummary() {
        return String.format(
            "Instance %d: state=%s, uptime=%s, calls=%d, resources=%d, memory=%d bytes",
            getInstanceId(),
            getState(),
            getUptime(),
            getFunctionCallCount(),
            getCurrentResourceCount(),
            getCurrentMemoryUsage());
      }

      @Override
      public void reset() {
        // For now, no-op since we're not tracking detailed statistics
        // TODO: Implement actual statistics reset when tracking is added
      }
    };
  }

  /**
   * Creates placeholder file system statistics.
   *
   * @return placeholder file system statistics
   */
  private WasiFileSystemStats createPlaceholderFileSystemStats() {
    return new WasiFileSystemStats() {
      @Override
      public long getReadOperations() {
        return 0;
      }

      @Override
      public long getWriteOperations() {
        return 0;
      }

      @Override
      public long getBytesRead() {
        return 0;
      }

      @Override
      public long getBytesWritten() {
        return 0;
      }

      @Override
      public long getFileOpenCount() {
        return 0;
      }

      @Override
      public int getCurrentOpenFiles() {
        return 0;
      }
    };
  }

  /**
   * Creates placeholder network statistics.
   *
   * @return placeholder network statistics
   */
  private WasiNetworkStats createPlaceholderNetworkStats() {
    return new WasiNetworkStats() {
      @Override
      public long getConnectionCount() {
        return 0;
      }

      @Override
      public int getCurrentConnections() {
        return 0;
      }

      @Override
      public long getBytesSent() {
        return 0;
      }

      @Override
      public long getBytesReceived() {
        return 0;
      }

      @Override
      public long getNetworkErrors() {
        return 0;
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
      public long getCurrentUsage() {
        return 0; // Not tracked yet
      }

      @Override
      public long getPeakUsage() {
        return 0; // Not tracked yet
      }

      @Override
      public Optional<Long> getLimit() {
        return config.getMemoryLimit();
      }

      @Override
      public Optional<Double> getUsagePercentage() {
        Optional<Long> limitOpt = getLimit();
        if (!limitOpt.isPresent()) {
          return Optional.empty();
        }
        long limit = limitOpt.get();
        if (limit <= 0) {
          return Optional.empty();
        }
        double percentage = (double) getCurrentUsage() / limit * 100.0;
        return Optional.of(percentage);
      }

      @Override
      public boolean isNearLimit() {
        Optional<Double> percentageOpt = getUsagePercentage();
        return percentageOpt.isPresent() && percentageOpt.get() > 80.0;
      }
    };
  }
}
