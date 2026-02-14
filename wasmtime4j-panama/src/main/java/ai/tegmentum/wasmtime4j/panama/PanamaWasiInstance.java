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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.wit.PanamaWitValueMarshaller;
import ai.tegmentum.wasmtime4j.wasi.WasiComponent;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
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
import ai.tegmentum.wasmtime4j.wit.WitValueMarshaller.MarshalledValue;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiInstance interface.
 *
 * <p>This class provides a concrete implementation of WASI component instance functionality using
 * Panama Foreign Function API bindings to the native Wasmtime component model. It manages instance
 * lifecycle, function calling, resource management, and execution state through Panama FFI calls
 * with Arena-based resource management.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Function calling with parameter marshaling
 *   <li>Resource lifecycle management with Arena-based cleanup
 *   <li>Execution state tracking and control
 *   <li>Memory usage monitoring with zero-copy access
 *   <li>Comprehensive error handling and cleanup
 *   <li>Thread-safe operations with defensive programming
 * </ul>
 *
 * <p>This implementation follows the unified API pattern while using Panama-specific component
 * instance wrappers for native interactions with optimal performance characteristics.
 *
 * @since 1.0.0
 */
public final class PanamaWasiInstance implements WasiInstance {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiInstance.class.getName());
  private static final NativeWasiBindings NATIVE_BINDINGS = NativeWasiBindings.getInstance();
  private static final NativeComponentBindings COMPONENT_BINDINGS =
      NativeComponentBindings.getInstance();

  private static final AtomicLong NEXT_INSTANCE_ID = new AtomicLong(1);

  private final long instanceId;
  private final ArenaResourceManager resourceManager;
  private final PanamaWasiComponent component;
  private final PanamaComponent.PanamaComponentInstanceHandle instanceHandle;
  private final WasiConfig config;
  private final Instant createdAt;
  private final Map<String, Object> properties;
  private final List<WasiResource> resources;

  private final NativeResourceHandle resourceHandle;

  private volatile WasiInstanceState state;
  private volatile Instant lastActivityAt;

  // Cached metadata to avoid repeated native calls
  private volatile List<String> cachedExportedFunctions;
  private volatile List<String> cachedExportedInterfaces;

  /**
   * Creates a new Panama WASI instance with the specified component and instance handle.
   *
   * @param resourceManager the arena resource manager for lifecycle management
   * @param component the parent component that created this instance
   * @param instanceHandle the native component instance handle
   * @param config the configuration used to create this instance
   * @throws IllegalArgumentException if any parameter is null
   */
  public PanamaWasiInstance(
      final ArenaResourceManager resourceManager,
      final PanamaWasiComponent component,
      final PanamaComponent.PanamaComponentInstanceHandle instanceHandle,
      final WasiConfig config) {
    this.instanceId = NEXT_INSTANCE_ID.getAndIncrement();
    this.resourceManager =
        Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
    this.component = Objects.requireNonNull(component, "Component cannot be null");
    this.instanceHandle = Objects.requireNonNull(instanceHandle, "Instance handle cannot be null");
    this.config = Objects.requireNonNull(config, "Configuration cannot be null");
    this.createdAt = Instant.now();
    this.properties = new ConcurrentHashMap<>();
    this.resources = new ArrayList<>();
    this.state = WasiInstanceState.CREATED;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaWasiInstance",
            () -> {
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
                    LOGGER.warning(
                        "Error closing resource " + resource.getId() + ": " + e.getMessage());
                  }
                }
                resources.clear();
              }

              // Close instance handle (managed by Arena)
              try {
                instanceHandle.close();
              } catch (Exception e) {
                LOGGER.warning("Error closing instance handle: " + e.getMessage());
              }

              properties.clear();
              LOGGER.fine("Closed Panama WASI instance: " + instanceId);
            });

    LOGGER.fine("Created Panama WASI instance with ID: " + instanceId);
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

    try (Arena arena = Arena.ofConfined()) {
      setState(WasiInstanceState.RUNNING);

      LOGGER.fine(
          "Calling function: " + functionName + " with " + parameters.length + " parameters");

      // Allocate C string for function name
      final MemorySegment funcNameSegment = arena.allocateFrom(functionName);

      // Marshal parameters
      final MemorySegment paramsPtr;
      final int paramsCount;

      if (parameters != null && parameters.length > 0) {
        // Convert arguments to WIT values and marshal
        final List<MarshalledValue> marshalledParams = new ArrayList<>(parameters.length);

        for (final Object param : parameters) {
          final WitValue witValue = convertToWitValue(param);
          final MarshalledValue marshalled = PanamaWitValueMarshaller.marshal(witValue, arena);
          marshalledParams.add(marshalled);
        }

        // Allocate WitValueFFI array
        final int ffiStructSize = 16; // sizeof(WitValueFFI): i32 + i32 + ptr (8 bytes on 64-bit)
        paramsPtr = arena.allocate(ffiStructSize * marshalledParams.size());

        // Fill WitValueFFI structures
        for (int i = 0; i < marshalledParams.size(); i++) {
          final MarshalledValue marshalled = marshalledParams.get(i);
          final long offset = (long) i * ffiStructSize;

          // WitValueFFI { type_discriminator: i32, data_length: i32, data_ptr: *const u8 }
          paramsPtr.set(ValueLayout.JAVA_INT, offset, marshalled.getTypeDiscriminator());
          paramsPtr.set(ValueLayout.JAVA_INT, offset + 4, marshalled.getData().length);

          // Allocate and copy data
          final MemorySegment dataSegment =
              arena.allocateFrom(ValueLayout.JAVA_BYTE, marshalled.getData());
          paramsPtr.set(ValueLayout.ADDRESS, offset + 8, dataSegment);
        }

        paramsCount = marshalledParams.size();
      } else {
        paramsPtr = MemorySegment.NULL;
        paramsCount = 0;
      }

      // Allocate output parameters
      final MemorySegment resultsOut = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment resultsCountOut = arena.allocate(ValueLayout.JAVA_INT);

      // Call native component invoke
      final int errorCode =
          COMPONENT_BINDINGS.componentInvoke(
              instanceHandle.getResource(),
              funcNameSegment,
              paramsPtr,
              paramsCount,
              resultsOut,
              resultsCountOut);

      if (errorCode != 0) {
        setState(WasiInstanceState.ERROR);
        throw new WasmException(
            "Failed to invoke function '" + functionName + "' (error code: " + errorCode + ")");
      }

      // Unmarshal results
      final int resultCount = resultsCountOut.get(ValueLayout.JAVA_INT, 0);

      if (resultCount == 0) {
        setState(WasiInstanceState.CREATED);
        return null;
      }

      final MemorySegment resultsArrayPtr = resultsOut.get(ValueLayout.ADDRESS, 0);

      if (resultsArrayPtr == null || resultsArrayPtr.equals(MemorySegment.NULL)) {
        setState(WasiInstanceState.CREATED);
        return null;
      }

      // Read WitValueFFI results
      final int ffiStructSize = 16;
      final MemorySegment resultsArrayWithSize =
          resultsArrayPtr.reinterpret(ffiStructSize * resultCount);
      final List<Object> results = new ArrayList<>(resultCount);

      for (int i = 0; i < resultCount; i++) {
        final long offset = (long) i * ffiStructSize;

        final int typeDiscriminator = resultsArrayWithSize.get(ValueLayout.JAVA_INT, offset);
        final int dataLength = resultsArrayWithSize.get(ValueLayout.JAVA_INT, offset + 4);
        final MemorySegment dataPtr = resultsArrayWithSize.get(ValueLayout.ADDRESS, offset + 8);

        // Reinterpret pointer with correct size
        final MemorySegment dataPtrWithSize = dataPtr.reinterpret(dataLength);

        // Copy data from native memory
        final byte[] data = new byte[dataLength];
        MemorySegment.copy(dataPtrWithSize, ValueLayout.JAVA_BYTE, 0, data, 0, dataLength);

        // Unmarshal using PanamaWitValueMarshaller
        final WitValue witValue =
            PanamaWitValueMarshaller.unmarshal(typeDiscriminator, data, arena);
        results.add(witValue.toJava());
      }

      setState(WasiInstanceState.CREATED);

      // Return single result or list based on count
      if (resultCount == 1) {
        return results.get(0);
      } else {
        return results;
      }

    } catch (final ValidationException e) {
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
   * @throws ValidationException if conversion fails
   */
  private static WitValue convertToWitValue(final Object obj) throws ValidationException {
    if (obj == null) {
      throw new ValidationException("Cannot convert null to WIT value");
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

    throw new ValidationException(
        "Unsupported Java type for WIT conversion: " + obj.getClass().getName());
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
      // TODO: Implement actual resource creation through Panama FFI layer
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
      // TODO: Implement actual suspension through Panama FFI layer
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
      // TODO: Implement actual resumption through Panama FFI layer
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
      // TODO: Implement actual termination through Panama FFI layer
      setState(WasiInstanceState.TERMINATED);
      LOGGER.fine("Terminated instance: " + instanceId);
    } catch (final Exception e) {
      throw new WasmException("Failed to terminate instance", e);
    }
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed() && instanceHandle.isValid() && component.isValid();
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
    resourceHandle.close();
  }

  /**
   * Gets the underlying Panama component instance handle for internal use.
   *
   * @return the Panama component instance handle
   */
  PanamaComponent.PanamaComponentInstanceHandle getInstanceHandle() {
    ensureNotClosed();
    return instanceHandle;
  }

  /**
   * Gets the resource manager for internal use.
   *
   * @return the arena resource manager
   */
  ArenaResourceManager getResourceManager() {
    return resourceManager;
  }

  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
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
    try (Arena arena = Arena.ofConfined()) {
      // Allocate output parameters
      final MemorySegment functionsOut = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment countOut = arena.allocate(ValueLayout.JAVA_INT);

      // Call native function to get exported functions
      final int errorCode =
          COMPONENT_BINDINGS.componentGetExportedFunctions(
              instanceHandle.getResource(), functionsOut, countOut);

      if (errorCode != 0) {
        LOGGER.warning("Failed to get exported functions (error code: " + errorCode + ")");
        return new ArrayList<>();
      }

      // Read the count
      final int count = countOut.get(ValueLayout.JAVA_INT, 0);

      if (count == 0) {
        return new ArrayList<>();
      }

      // Read the array of string pointers
      final MemorySegment stringsPtr = functionsOut.get(ValueLayout.ADDRESS, 0);

      if (stringsPtr == null || stringsPtr.equals(MemorySegment.NULL)) {
        return new ArrayList<>();
      }

      // Reinterpret with proper size for array of pointers
      final MemorySegment stringsPtrWithSize =
          stringsPtr.reinterpret(ValueLayout.ADDRESS.byteSize() * count);

      // Extract function names
      final List<String> functions = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
        final MemorySegment strPtr = stringsPtrWithSize.getAtIndex(ValueLayout.ADDRESS, i);
        if (strPtr != null && !strPtr.equals(MemorySegment.NULL)) {
          functions.add(strPtr.reinterpret(Long.MAX_VALUE).getString(0));
        }
      }

      // Free the string array
      COMPONENT_BINDINGS.componentFreeStringArray(stringsPtr, count);

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
        return Optional.empty();
      }

      @Override
      public List<WasiParameterMetadata> getParameters() {
        return new ArrayList<>();
      }

      @Override
      public Optional<WasiTypeMetadata> getReturnType() {
        return Optional.empty();
      }

      @Override
      public boolean canThrow() {
        return false;
      }

      @Override
      public List<String> getThrownExceptionTypes() {
        return new ArrayList<>();
      }

      @Override
      public void validateParameters(final Object... parameters) {
        // Basic validation - accept all for now
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
  private WasiResource createPlaceholderResource(
      final String resourceType, final Object... parameters) {
    // TODO: Implement actual resource creation with Arena-based management
    // For now, create a basic placeholder resource
    return new WasiResource() {
      private static final AtomicLong NEXT_RESOURCE_ID = new AtomicLong(1);
      private final long resourceId = NEXT_RESOURCE_ID.getAndIncrement();
      private volatile boolean resourceClosed = false;
      private final Instant createdAt = Instant.now();
      private volatile Instant lastAccessedAt = null;

      @Override
      public long getId() {
        return resourceId;
      }

      @Override
      public String getType() {
        return resourceType;
      }

      @Override
      public WasiInstance getOwner() {
        return PanamaWasiInstance.this;
      }

      @Override
      public boolean isOwned() {
        return true; // All our resources are owned
      }

      @Override
      public boolean isValid() {
        return !resourceClosed;
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
        return createEmptyResourceMetadata();
      }

      @Override
      public WasiResourceState getState() throws WasmException {
        return createEmptyResourceState();
      }

      @Override
      public WasiResourceStats getStats() {
        return createEmptyResourceStats();
      }

      @Override
      public Object invoke(final String operation, final Object... parameters)
          throws WasmException {
        throw new WasmException("Operation not supported: " + operation);
      }

      @Override
      public List<String> getAvailableOperations() {
        return new ArrayList<>();
      }

      @Override
      public WasiResourceHandle createHandle() throws WasmException {
        throw new WasmException("Handle creation not supported yet");
      }

      @Override
      public void transferOwnership(final WasiInstance targetInstance) throws WasmException {
        throw new WasmException("Ownership transfer not supported yet");
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
    final Instant collectedAt = Instant.now();
    return new WasiInstanceStats() {
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
        return new HashMap<>();
      }

      @Override
      public Map<String, Duration> getFunctionExecutionTimeStats() {
        return new HashMap<>();
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
        return 0; // Not tracked yet
      }

      @Override
      public long getTotalResourcesCreated() {
        return 0; // Not tracked yet
      }

      @Override
      public Map<String, Integer> getResourceUsageByType() {
        return new HashMap<>();
      }

      @Override
      public long getErrorCount() {
        return 0; // Not tracked yet
      }

      @Override
      public Map<String, Long> getErrorStats() {
        return new HashMap<>();
      }

      @Override
      public long getSuspensionCount() {
        return 0; // Not tracked yet
      }

      @Override
      public Duration getTotalSuspensionTime() {
        return Duration.ZERO;
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
      public WasiNetworkStats getNetworkStats() {
        return createEmptyNetworkStats();
      }

      @Override
      public Duration getAverageExecutionTime() {
        return Duration.ZERO;
      }

      @Override
      public double getThroughput() {
        return 0.0;
      }

      @Override
      public double getMemoryEfficiency() {
        return 0.0;
      }

      @Override
      public Map<String, Object> getCustomProperties() {
        return new HashMap<>();
      }

      @Override
      public String getSummary() {
        return String.format(
            "Instance Stats: ID=%d, State=%s, Uptime=%s, Resources=%d",
            getInstanceId(), getState(), getUptime(), getCurrentResourceCount());
      }

      @Override
      public void reset() {
        // Reset operation not supported yet
        throw new IllegalStateException("Reset not supported yet");
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
        Optional<Long> limit = getLimit();
        if (limit.isEmpty()) {
          return Optional.empty();
        }
        long current = getCurrentUsage();
        if (current <= 0) {
          return Optional.of(0.0);
        }
        return Optional.of((double) current / limit.get() * 100.0);
      }

      @Override
      public boolean isNearLimit() {
        Optional<Double> percentage = getUsagePercentage();
        return percentage.isPresent() && percentage.get() > 80.0;
      }
    };
  }

  private WasiResourceMetadata createEmptyResourceMetadata() {
    return new WasiResourceMetadata() {
      @Override
      public String getResourceType() {
        return "unknown";
      }

      @Override
      public long getResourceId() {
        return 0;
      }

      @Override
      public Instant getCreatedAt() {
        return Instant.now();
      }

      @Override
      public Optional<Instant> getLastModifiedAt() {
        return Optional.empty();
      }

      @Override
      public Optional<Long> getSize() {
        return Optional.empty();
      }

      @Override
      public Map<String, Object> getProperties() {
        return new HashMap<>();
      }

      @Override
      public boolean hasCapability(final String capability) {
        return false;
      }
    };
  }

  private WasiResourceState createEmptyResourceState() {
    return WasiResourceState.CREATED;
  }

  private WasiResourceStats createEmptyResourceStats() {
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

  private WasiNetworkStats createEmptyNetworkStats() {
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
}
