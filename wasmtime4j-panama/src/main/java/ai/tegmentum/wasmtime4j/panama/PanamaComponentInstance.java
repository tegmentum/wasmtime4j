package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ComponentFunction;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.ComponentInstanceState;
import ai.tegmentum.wasmtime4j.ComponentResourceUsage;
import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WitValueException;
import ai.tegmentum.wasmtime4j.panama.wit.PanamaWitValueMarshaller;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

  /**
   * Creates a new Panama component instance from a linker-based instantiation.
   *
   * <p>This constructor is used when instantiating through a ComponentLinker, which creates its own
   * internal store. The instance ID is derived from the native instance pointer address.
   *
   * @param nativeInstancePtr the native instance pointer returned from linker instantiation
   * @param component the parent component
   * @param store the store (may be null for linker-based instantiation)
   */
  PanamaComponentInstance(
      final MemorySegment nativeInstancePtr,
      final PanamaComponentSimple component,
      final PanamaStore store) {
    // Use the native instance pointer address as the instance ID
    this.enhancedEngineHandle = nativeInstancePtr;
    this.instanceId = nativeInstancePtr.address();
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

    try (var arena = Arena.ofConfined()) {
      // Allocate C string for function name
      final MemorySegment funcNameSegment = arena.allocateFrom(functionName);

      // Marshal parameters
      final MemorySegment paramsPtr;
      final int paramsCount;

      if (args != null && args.length > 0) {
        // Convert arguments to WIT values and marshal
        final List<MarshalledValue> marshalledParams = new ArrayList<>(args.length);

        for (final Object arg : args) {
          final WitValue witValue = convertToWitValue(arg);
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

      System.err.println(
          "JAVA: About to call enhancedComponentInvoke: function="
              + functionName
              + ", instanceId="
              + instanceId
              + ", paramsCount="
              + paramsCount);

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

      System.err.println("JAVA: enhancedComponentInvoke returned errorCode=" + errorCode);

      if (errorCode != 0) {
        System.err.println("JAVA: Error invoking function, throwing exception");
        throw new WasmException(
            "Failed to invoke component function '"
                + functionName
                + "' (error code: "
                + errorCode
                + ")");
      }

      // Unmarshal results
      final int resultCount = resultsCountOut.get(ValueLayout.JAVA_INT, 0);

      if (resultCount == 0) {
        return null;
      }

      final MemorySegment resultsArrayPtr = resultsOut.get(ValueLayout.ADDRESS, 0);

      if (resultsArrayPtr == null || resultsArrayPtr.equals(MemorySegment.NULL)) {
        return null;
      }

      // Read WitValueFFI results
      final int ffiStructSize = 16; // sizeof(WitValueFFI): i32 + i32 + ptr (8 bytes on 64-bit)
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

      // Return single result or list based on count
      if (resultCount == 1) {
        return results.get(0);
      } else {
        return results;
      }

    } catch (final WitValueException e) {
      throw new WasmException("WIT value marshalling failed: " + e.getMessage(), e);
    }
  }

  /**
   * Converts a Java object to a WIT value.
   *
   * @param obj the Java object to convert
   * @return the WIT value
   * @throws WitValueException if conversion fails
   */
  private static WitValue convertToWitValue(final Object obj) throws WitValueException {
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
  public Optional<ComponentFunction> getFunc(final String functionName) throws WasmException {
    Objects.requireNonNull(functionName, "functionName cannot be null");
    ensureNotClosed();

    // Check if the function exists in exported functions
    final Set<String> exportedFunctions = getExportedFunctions();
    if (exportedFunctions.contains(functionName)) {
      return Optional.of(new PanamaComponentFunction(functionName, this));
    }

    return Optional.empty();
  }

  @Override
  public Set<String> getExportedFunctions() {
    ensureNotClosed();

    try (var arena = java.lang.foreign.Arena.ofConfined()) {
      // Allocate output parameters
      final MemorySegment functionsOut = arena.allocate(java.lang.foreign.ValueLayout.ADDRESS);
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
      final MemorySegment stringsPtr = functionsOut.get(java.lang.foreign.ValueLayout.ADDRESS, 0);

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
