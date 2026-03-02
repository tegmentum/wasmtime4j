/*
 * Copyright 2025 Tegmentum AI
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

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentExportIndex;
import ai.tegmentum.wasmtime4j.component.ComponentFunction;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.component.ComponentVal;
import ai.tegmentum.wasmtime4j.component.ConcurrentCall;
import ai.tegmentum.wasmtime4j.component.ConcurrentCallCodec;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Panama implementation of a WebAssembly component instance.
 *
 * <p>This class wraps a native component instance handle and provides lifecycle management for
 * instantiated components.
 *
 * @since 1.0.0
 */
final class PanamaComponentInstance implements ComponentInstance {

  private static final NativeComponentBindings NATIVE_BINDINGS =
      NativeComponentBindings.getInstance();

  private final MemorySegment enhancedEngineHandle;
  private final long instanceId;
  private final PanamaComponentImpl component;
  private final PanamaStore store;
  private final NativeResourceHandle resourceHandle;

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
      final PanamaComponentImpl component,
      final PanamaStore store) {
    this.enhancedEngineHandle = enhancedEngineHandle;
    this.instanceId = instanceId;
    this.component = component;
    this.store = store;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaComponentInstance",
            () -> {
              // Enhanced component engine manages instance lifecycle
              // Instances are automatically cleaned up when engine is destroyed
              // No need to manually destroy individual instances
            });
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
      final PanamaComponentImpl component,
      final PanamaStore store) {
    // Use the native instance pointer address as the instance ID
    this.enhancedEngineHandle = nativeInstancePtr;
    this.instanceId = nativeInstancePtr.address();
    this.component = component;
    this.store = store;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaComponentInstance",
            () -> {
              // Enhanced component engine manages instance lifecycle
              // Instances are automatically cleaned up when engine is destroyed
              // No need to manually destroy individual instances
            });
  }

  @Override
  public String getId() {
    return component.getComponentId();
  }

  @Override
  public Component getComponent() {
    return component;
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
        throw PanamaErrorMapper.mapNativeError(
            errorCode, "Failed to invoke component function '" + functionName + "'");
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

    } catch (final ValidationException e) {
      throw new WasmException("WIT value marshalling failed: " + e.getMessage(), e);
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
  public boolean hasFunction(final String functionName) {
    Objects.requireNonNull(functionName, "functionName cannot be null");
    ensureNotClosed();
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(functionName);
      final int result =
          NATIVE_BINDINGS.enhancedComponentInstanceHasFunc(
              enhancedEngineHandle, instanceId, nameSegment);
      return result == 1;
    }
  }

  @Override
  public List<List<ComponentVal>> runConcurrent(final List<ConcurrentCall> calls)
      throws WasmException {
    if (calls == null || calls.isEmpty()) {
      throw new IllegalArgumentException("calls cannot be null or empty");
    }
    ensureNotClosed();

    final String jsonInput = ConcurrentCallCodec.serializeCalls(calls);
    final byte[] jsonBytes = jsonInput.getBytes(StandardCharsets.UTF_8);

    try (final Arena arena = Arena.ofConfined()) {
      // Allocate input JSON bytes
      final MemorySegment jsonSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, jsonBytes);

      // Allocate output parameters
      final MemorySegment resultPtrOut = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment resultLenOut = arena.allocate(ValueLayout.JAVA_LONG);

      final int errorCode =
          NATIVE_BINDINGS.enhancedComponentRunConcurrent(
              enhancedEngineHandle,
              instanceId,
              jsonSegment,
              jsonBytes.length,
              resultPtrOut,
              resultLenOut);

      // Read result pointer and length
      final MemorySegment resultPtr = resultPtrOut.get(ValueLayout.ADDRESS, 0);
      final long resultLen = resultLenOut.get(ValueLayout.JAVA_LONG, 0);

      try {
        if (errorCode != 0) {
          // On error, the result buffer contains an error message
          if (resultPtr != null && !resultPtr.equals(MemorySegment.NULL) && resultLen > 0) {
            final MemorySegment resultWithSize = resultPtr.reinterpret(resultLen);
            final byte[] errorBytes = new byte[(int) resultLen];
            MemorySegment.copy(
                resultWithSize, ValueLayout.JAVA_BYTE, 0, errorBytes, 0, (int) resultLen);
            final String errorMsg = new String(errorBytes, StandardCharsets.UTF_8);
            throw new WasmException("Concurrent call failed: " + errorMsg);
          }
          throw new WasmException("Concurrent call failed with error code: " + errorCode);
        }

        if (resultPtr == null || resultPtr.equals(MemorySegment.NULL) || resultLen == 0) {
          throw new WasmException("Native concurrent call returned null result");
        }

        // Read result JSON
        final MemorySegment resultWithSize = resultPtr.reinterpret(resultLen);
        final byte[] resultBytes = new byte[(int) resultLen];
        MemorySegment.copy(
            resultWithSize, ValueLayout.JAVA_BYTE, 0, resultBytes, 0, (int) resultLen);
        final String jsonResult = new String(resultBytes, StandardCharsets.UTF_8);

        return ConcurrentCallCodec.deserializeResults(jsonResult);
      } finally {
        // Always free the result buffer
        if (resultPtr != null && !resultPtr.equals(MemorySegment.NULL) && resultLen > 0) {
          NATIVE_BINDINGS.freeConcurrentResult(resultPtr, resultLen);
        }
      }
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Concurrent call execution failed: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean hasResource(final String resourceName) throws WasmException {
    Objects.requireNonNull(resourceName, "resourceName cannot be null");
    ensureNotClosed();
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(resourceName);
      final int result =
          NATIVE_BINDINGS.enhancedComponentInstanceHasResource(
              enhancedEngineHandle, instanceId, nameSegment);
      return result == 1;
    }
  }

  @Override
  public Optional<Module> getModule(final String moduleName) throws WasmException {
    Objects.requireNonNull(moduleName, "moduleName cannot be null");
    ensureNotClosed();
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(moduleName);
      final MemorySegment moduleOut = arena.allocate(ValueLayout.ADDRESS);
      final int result =
          NATIVE_BINDINGS.enhancedComponentInstanceGetModule(
              enhancedEngineHandle, instanceId, nameSegment, moduleOut);
      if (result != 0) {
        return Optional.empty();
      }
      final MemorySegment modulePtr = moduleOut.get(ValueLayout.ADDRESS, 0);
      if (modulePtr == null || modulePtr.equals(MemorySegment.NULL)) {
        return Optional.empty();
      }
      return Optional.of(new PanamaModule(modulePtr));
    } catch (final Exception e) {
      throw new WasmException("Failed to get module '" + moduleName + "': " + e.getMessage(), e);
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
  public Optional<ComponentFunction> getFunc(final ComponentExportIndex exportIndex)
      throws WasmException {
    if (exportIndex == null) {
      throw new IllegalArgumentException("exportIndex cannot be null");
    }
    ensureNotClosed();

    try {
      final MemorySegment indexPtr = MemorySegment.ofAddress(exportIndex.getNativeHandle());
      final int found =
          NATIVE_BINDINGS.enhancedComponentInstanceHasFuncByIndex(
              enhancedEngineHandle, instanceId, indexPtr);
      if (found != 1) {
        return Optional.empty();
      }
      return Optional.of(new PanamaComponentFunction("__indexed_export__", this));
    } catch (final Exception e) {
      throw new WasmException("Failed to get function by export index", e);
    }
  }

  @Override
  public Optional<ai.tegmentum.wasmtime4j.component.ComponentExportItem> getExport(
      final String name) throws WasmException {
    return getExport(null, name);
  }

  @Override
  public Optional<ai.tegmentum.wasmtime4j.component.ComponentExportItem> getExport(
      final ComponentExportIndex parentIndex, final String name) throws WasmException {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }
    ensureNotClosed();

    try (final Arena arena = Arena.ofConfined()) {
      final byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
      final MemorySegment nameSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, nameBytes);
      final MemorySegment outIndexPtr = arena.allocate(ValueLayout.ADDRESS);

      final MemorySegment parentPtr =
          (parentIndex != null)
              ? MemorySegment.ofAddress(parentIndex.getNativeHandle())
              : MemorySegment.NULL;

      final int kindCode =
          NATIVE_BINDINGS.enhancedComponentInstanceGetExport(
              enhancedEngineHandle,
              instanceId,
              parentPtr,
              nameSegment,
              nameBytes.length,
              outIndexPtr);

      if (kindCode == -1) {
        return Optional.empty();
      }
      if (kindCode == -2) {
        throw new WasmException("Native error looking up export: " + name);
      }

      final MemorySegment indexPtr = outIndexPtr.get(ValueLayout.ADDRESS, 0);
      final ai.tegmentum.wasmtime4j.component.ComponentItemKind kind =
          ai.tegmentum.wasmtime4j.component.ComponentExportItem.kindFromCode(kindCode);
      final ComponentExportIndex exportIndex = new PanamaComponentExportIndex(indexPtr);
      return Optional.of(
          new ai.tegmentum.wasmtime4j.component.ComponentExportItem(kind, exportIndex));
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Failed to get export: " + name, e);
    }
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
  public ComponentInstanceConfig getConfig() {
    return new ComponentInstanceConfig();
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed()
        && enhancedEngineHandle != null
        && !enhancedEngineHandle.equals(MemorySegment.NULL)
        && instanceId != 0;
  }

  @Override
  public void close() {
    resourceHandle.close();
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
    resourceHandle.ensureNotClosed();
  }
}
