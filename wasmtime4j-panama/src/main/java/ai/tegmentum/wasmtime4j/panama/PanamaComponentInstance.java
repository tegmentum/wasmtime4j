package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.ComponentInstanceState;
import ai.tegmentum.wasmtime4j.ComponentResourceUsage;
import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Panama implementation of a WebAssembly component instance.
 *
 * <p>This class wraps a native component instance handle and provides lifecycle management for
 * instantiated components.
 *
 * @since 1.0.0
 */
final class PanamaComponentInstance implements ComponentInstance {

  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final MemorySegment enhancedEngineHandle;
  private final long instanceId;
  private final PanamaComponentSimple component;
  private final PanamaStore store;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new Panama component instance using enhanced component engine.
   *
   * @param enhancedEngineHandle the enhanced component engine handle
   * @param instanceId the instance ID returned from enhanced instantiation
   * @param component the parent component
   * @param store the store
   */
  PanamaComponentInstance(
      final MemorySegment enhancedEngineHandle,
      final long instanceId,
      final PanamaComponentSimple component,
      final PanamaStore store) {
    this.enhancedEngineHandle = enhancedEngineHandle;
    this.instanceId = instanceId;
    this.component = component;
    this.store = store;
  }

  @Override
  public String getId() {
    return component.getComponentId();
  }

  @Override
  public ComponentSimple getComponent() {
    return component;
  }

  @Override
  public ComponentInstanceState getState() {
    ensureNotClosed();
    return ComponentInstanceState.ACTIVE;
  }

  @Override
  public Object invoke(final String functionName, final Object... args) throws WasmException {
    Objects.requireNonNull(functionName, "functionName cannot be null");
    ensureNotClosed();

    try (var arena = java.lang.foreign.Arena.ofConfined()) {
      // Allocate C string for function name
      final MemorySegment funcNameSegment = arena.allocateFrom(functionName);

      // For now, pass null params (no parameters)
      final MemorySegment paramsPtr = MemorySegment.NULL;
      final int paramsCount = 0;

      // Allocate output parameters
      final MemorySegment resultsOut =
          arena.allocate(java.lang.foreign.ValueLayout.ADDRESS);
      final MemorySegment resultsCountOut =
          arena.allocate(java.lang.foreign.ValueLayout.JAVA_INT);

      // Call enhanced component invoke with instance ID
      final int errorCode =
          NATIVE_BINDINGS.enhancedComponentInvoke(
              enhancedEngineHandle,
              instanceId,
              funcNameSegment,
              paramsPtr,
              paramsCount,
              resultsOut,
              resultsCountOut);

      if (errorCode != 0) {
        throw new WasmException(
            "Failed to invoke component function '"
                + functionName
                + "' (error code: "
                + errorCode
                + ")");
      }

      // TODO: Marshal results from native format to Java objects
      // For now, return null as placeholder
      return null;
    }
  }

  @Override
  public boolean hasFunction(final String functionName) {
    Objects.requireNonNull(functionName, "functionName cannot be null");
    ensureNotClosed();
    // Check if the function name is in the list of exported interfaces
    try {
      return component.getExportedInterfaces().contains(functionName);
    } catch (final WasmException e) {
      return false;
    }
  }

  @Override
  public Set<String> getExportedFunctions() {
    ensureNotClosed();

    try (var arena = java.lang.foreign.Arena.ofConfined()) {
      // Allocate output parameters
      final MemorySegment functionsOut =
          arena.allocate(java.lang.foreign.ValueLayout.ADDRESS);
      final MemorySegment countOut = arena.allocate(java.lang.foreign.ValueLayout.JAVA_INT);

      // Call enhanced component get exports with instance ID
      final int errorCode =
          NATIVE_BINDINGS.enhancedComponentGetExports(
              enhancedEngineHandle, instanceId, functionsOut, countOut);

      if (errorCode != 0) {
        // If we can't get exports, return empty set
        return Set.of();
      }

      // Read the count
      final int count = countOut.get(java.lang.foreign.ValueLayout.JAVA_INT, 0);

      if (count == 0) {
        return Set.of();
      }

      // Read the array of string pointers
      final MemorySegment stringsPtr =
          functionsOut.get(java.lang.foreign.ValueLayout.ADDRESS, 0);

      if (stringsPtr == null || stringsPtr.equals(MemorySegment.NULL)) {
        return Set.of();
      }

      // Extract function names
      final Set<String> functionNames = new java.util.HashSet<>();
      for (int i = 0; i < count; i++) {
        final MemorySegment strPtr =
            stringsPtr.getAtIndex(java.lang.foreign.ValueLayout.ADDRESS, i);
        if (strPtr != null && !strPtr.equals(MemorySegment.NULL)) {
          functionNames.add(strPtr.getString(0));
        }
      }

      // Free the string array
      NATIVE_BINDINGS.componentFreeStringArray(stringsPtr, count);

      return functionNames;
    } catch (final Exception e) {
      // If we can't get exports, return empty set
      return Set.of();
    }
  }

  @Override
  public Map<String, WitInterfaceDefinition> getExportedInterfaces() throws WasmException {
    ensureNotClosed();
    // Interface definition extraction requires full WIT type system implementation including:
    // 1. WIT parser for component metadata
    // 2. Interface definition extraction with full type information
    // 3. Type hierarchy and relationship resolution
    // Currently returns empty map - use getExportedFunctions() for interface names only
    return Map.of();
  }

  @Override
  public void bindInterface(final String interfaceName, final Object implementation)
      throws WasmException {
    Objects.requireNonNull(interfaceName, "interfaceName cannot be null");
    Objects.requireNonNull(implementation, "implementation cannot be null");
    ensureNotClosed();
    // Interface binding requires full WIT type system implementation including:
    // 1. WIT interface parsing and validation
    // 2. Java-to-WIT type mapping
    // 3. Canonical ABI encoding/decoding
    // 4. Host function registration and lifecycle management
    // 5. Resource handle tracking
    throw new UnsupportedOperationException(
        "Component Model interface binding not yet implemented - "
            + "requires full WIT type system and host function support");
  }

  @Override
  public ComponentInstanceConfig getConfig() {
    return new ComponentInstanceConfig();
  }

  @Override
  public ComponentResourceUsage getResourceUsage() {
    return new ComponentResourceUsage(component.getComponentId());
  }

  @Override
  public boolean isValid() {
    return !closed.get()
        && enhancedEngineHandle != null
        && !enhancedEngineHandle.equals(MemorySegment.NULL)
        && instanceId != 0;
  }

  @Override
  public void pause() throws WasmException {
    ensureNotClosed();
    // Component instance lifecycle control requires native runtime support for:
    // 1. Instance state suspension and serialization
    // 2. Execution context preservation
    // 3. Resource handle freezing
    // 4. Thread-safe state transitions
    throw new UnsupportedOperationException(
        "Component instance pause not yet implemented - "
            + "requires native runtime lifecycle support");
  }

  @Override
  public void resume() throws WasmException {
    ensureNotClosed();
    // Component instance lifecycle control requires native runtime support for:
    // 1. Instance state restoration and deserialization
    // 2. Execution context reconstruction
    // 3. Resource handle reactivation
    // 4. Thread-safe state transitions
    throw new UnsupportedOperationException(
        "Component instance resume not yet implemented - "
            + "requires native runtime lifecycle support");
  }

  @Override
  public void stop() throws WasmException {
    ensureNotClosed();
    // Component instance lifecycle control requires native runtime support for:
    // 1. Graceful instance termination
    // 2. Resource cleanup and release
    // 3. Execution context teardown
    // 4. Thread-safe state transitions
    throw new UnsupportedOperationException(
        "Component instance stop not yet implemented - "
            + "requires native runtime lifecycle support");
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      // Enhanced component engine manages instance lifecycle
      // Instances are automatically cleaned up when engine is destroyed
      // No need to manually destroy individual instances
    }
  }

  /**
   * Gets the instance ID.
   *
   * @return the instance ID
   */
  long getInstanceId() {
    return instanceId;
  }

  /**
   * Gets the enhanced engine handle.
   *
   * @return the enhanced engine handle
   */
  MemorySegment getEnhancedEngineHandle() {
    return enhancedEngineHandle;
  }

  /**
   * Gets the store.
   *
   * @return the store
   */
  PanamaStore getStore() {
    return store;
  }

  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("Component instance is closed");
    }
  }
}
