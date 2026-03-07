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

import ai.tegmentum.wasmtime4j.ResourcesRequired;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentExportIndex;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.component.ComponentTypeCodec;
import ai.tegmentum.wasmtime4j.component.ComponentTypeInfo;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Optional;
import java.util.Set;

/**
 * Panama implementation of Component.
 *
 * @since 1.0.0
 */
final class PanamaComponentImpl implements Component {

  private static final NativeComponentBindings NATIVE_BINDINGS =
      NativeComponentBindings.getInstance();

  private final MemorySegment componentHandle;
  private final String componentId;
  private final PanamaComponentEngine engine;
  private final NativeResourceHandle resourceHandle;

  PanamaComponentImpl(
      final MemorySegment componentHandle,
      final String componentId,
      final PanamaComponentEngine engine) {
    this.componentHandle = componentHandle;
    this.componentId = componentId;
    this.engine = engine;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaComponentImpl",
            () -> {
              if (componentHandle != null && !componentHandle.equals(MemorySegment.NULL)) {
                try {
                  NATIVE_BINDINGS.destroyComponent(componentHandle);
                } catch (final Throwable t) {
                  throw new Exception("Error closing PanamaComponentImpl", t);
                }
              }
            });
  }

  @Override
  public String getId() {
    return componentId;
  }

  @Override
  public long getSize() throws WasmException {
    resourceHandle.beginOperation();
    try {
      return NATIVE_BINDINGS.componentGetSize(componentHandle);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean exportsInterface(final String interfaceName) throws WasmException {
    resourceHandle.beginOperation();
    try {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment nameSegment = arena.allocateFrom(interfaceName);
        return NATIVE_BINDINGS.componentExportsInterface(componentHandle, nameSegment) != 0;
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean importsInterface(final String interfaceName) throws WasmException {
    resourceHandle.beginOperation();
    try {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment nameSegment = arena.allocateFrom(interfaceName);
        return NATIVE_BINDINGS.componentImportsInterface(componentHandle, nameSegment) != 0;
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public Set<String> getExportedInterfaces() throws WasmException {
    resourceHandle.beginOperation();
    try {
      final long exportCount = NATIVE_BINDINGS.componentExportCount(componentHandle);
      final Set<String> exports = new java.util.HashSet<>();

      try (Arena arena = Arena.ofConfined()) {
        for (long i = 0; i < exportCount; i++) {
          final MemorySegment nameOut = arena.allocate(ValueLayout.ADDRESS);
          final int errorCode = NATIVE_BINDINGS.componentGetExportName(componentHandle, i, nameOut);

          if (errorCode != 0) {
            continue; // Skip this export if we can't get the name
          }

          final MemorySegment namePtr = nameOut.get(ValueLayout.ADDRESS, 0);
          if (namePtr != null && !namePtr.equals(MemorySegment.NULL)) {
            try {
              // Reinterpret as unbounded segment to read C string
              final MemorySegment unboundedPtr = namePtr.reinterpret(Long.MAX_VALUE);
              final String exportName = unboundedPtr.getString(0);
              exports.add(exportName);
            } finally {
              NATIVE_BINDINGS.componentFreeString(namePtr);
            }
          }
        }
      }

      return Set.copyOf(exports);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public Set<String> getImportedInterfaces() throws WasmException {
    resourceHandle.beginOperation();
    try {
      final long importCount = NATIVE_BINDINGS.componentImportCount(componentHandle);
      final Set<String> imports = new java.util.HashSet<>();

      try (Arena arena = Arena.ofConfined()) {
        for (long i = 0; i < importCount; i++) {
          final MemorySegment nameOut = arena.allocate(ValueLayout.ADDRESS);
          final int errorCode = NATIVE_BINDINGS.componentGetImportName(componentHandle, i, nameOut);

          if (errorCode != 0) {
            continue; // Skip this import if we can't get the name
          }

          final MemorySegment namePtr = nameOut.get(ValueLayout.ADDRESS, 0);
          if (namePtr != null && !namePtr.equals(MemorySegment.NULL)) {
            try {
              // Reinterpret as unbounded segment to read C string
              final MemorySegment unboundedPtr = namePtr.reinterpret(Long.MAX_VALUE);
              final String importName = unboundedPtr.getString(0);
              imports.add(importName);
            } finally {
              NATIVE_BINDINGS.componentFreeString(namePtr);
            }
          }
        }
      }

      return Set.copyOf(imports);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ComponentTypeInfo componentType() throws WasmException {
    resourceHandle.beginOperation();
    try {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment jsonOut = arena.allocate(ValueLayout.ADDRESS);
        final int errorCode =
            NATIVE_BINDINGS.componentGetFullTypeJson(
                componentHandle, engine.getNativeHandle(), jsonOut);

        if (errorCode != 0) {
          // Fall back to name-only default
          return Component.super.componentType();
        }

        final MemorySegment jsonPtr = jsonOut.get(ValueLayout.ADDRESS, 0);
        if (jsonPtr == null || jsonPtr.equals(MemorySegment.NULL)) {
          return Component.super.componentType();
        }

        try {
          final MemorySegment unbounded = jsonPtr.reinterpret(Long.MAX_VALUE);
          final String json = unbounded.getString(0);
          return ComponentTypeCodec.deserialize(json);
        } finally {
          NATIVE_BINDINGS.componentFreeString(jsonPtr);
        }
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ComponentInstance instantiate() throws WasmException {
    return instantiate(new ComponentInstanceConfig());
  }

  @Override
  public ComponentInstance instantiate(final ComponentInstanceConfig config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("config cannot be null");
    }
    resourceHandle.beginOperation();
    try {

      try (Arena tempArena = Arena.ofConfined()) {
        final MemorySegment instanceIdOut =
            tempArena.allocate(java.lang.foreign.ValueLayout.JAVA_LONG);

        final int errorCode =
            NATIVE_BINDINGS.enhancedComponentInstantiate(
                engine.getNativeHandle(), componentHandle, instanceIdOut);

        if (errorCode != 0) {
          throw new WasmException(
              "Failed to instantiate component: native error code " + errorCode);
        }

        final long instanceId = instanceIdOut.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);

        if (instanceId == 0) {
          throw new WasmException("Failed to instantiate component: invalid instance ID returned");
        }

        return new PanamaComponentInstance(engine.getNativeHandle(), instanceId, this, null);
      } catch (final WasmException e) {
        throw e;
      } catch (final Exception e) {
        throw new WasmException("Failed to instantiate component", e);
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public WitInterfaceDefinition getWitInterface() throws WasmException {
    resourceHandle.beginOperation();
    try {

      // Build WIT interface from component exports and imports
      final StringBuilder witBuilder = new StringBuilder();
      witBuilder.append("interface ").append(componentId).append(" {\n");

      // Add exported interfaces
      for (final String export : getExportedInterfaces()) {
        witBuilder.append("  export ").append(export).append(";\n");
      }

      // Add imported interfaces
      for (final String imp : getImportedInterfaces()) {
        witBuilder.append("  import ").append(imp).append(";\n");
      }

      witBuilder.append("}\n");

      return new PanamaWitInterfaceDefinition(
          componentId,
          "1.0.0",
          "", // Package name not available from component metadata
          witBuilder.toString());
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public WitCompatibilityResult checkWitCompatibility(final Component other) throws WasmException {
    if (other == null) {
      throw new IllegalArgumentException("other cannot be null");
    }
    resourceHandle.beginOperation();
    try {

      // Get WIT interfaces for both components
      final WitInterfaceDefinition myInterface = getWitInterface();
      final WitInterfaceDefinition otherInterface = other.getWitInterface();

      // Use the WitInterfaceDefinition's built-in compatibility check
      return myInterface.isCompatibleWith(otherInterface);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public Optional<ComponentExportIndex> exportIndex(
      final ComponentExportIndex instanceIndex, final String name) throws WasmException {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }
    resourceHandle.beginOperation();
    try {

      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment nameSegment = arena.allocateFrom(name);
        final MemorySegment indexOut = arena.allocate(ValueLayout.ADDRESS);

        // Pass parent index if provided, otherwise NULL
        final MemorySegment parentPtr =
            instanceIndex != null
                ? MemorySegment.ofAddress(instanceIndex.getNativeHandle())
                : MemorySegment.NULL;

        final int result =
            NATIVE_BINDINGS.componentGetExportIndex(
                componentHandle, parentPtr, nameSegment, indexOut);

        if (result != 0) {
          // 1 = not found, -1 = error
          return Optional.empty();
        }

        final MemorySegment indexPtr = indexOut.get(ValueLayout.ADDRESS, 0);
        if (indexPtr == null || indexPtr.equals(MemorySegment.NULL)) {
          return Optional.empty();
        }

        return Optional.of(new PanamaComponentExportIndex(indexPtr));
      } catch (final Exception e) {
        throw new WasmException("Failed to get export index for '" + name + "'", e);
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public byte[] serialize() throws WasmException {
    resourceHandle.beginOperation();
    try {

      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment dataPtrOut = arena.allocate(ValueLayout.ADDRESS);
        final MemorySegment lenOut = arena.allocate(ValueLayout.JAVA_LONG);

        final int errorCode =
            NATIVE_BINDINGS.componentSerialize(componentHandle, dataPtrOut, lenOut);
        if (errorCode != 0) {
          throw new WasmException("Failed to serialize component: native error code " + errorCode);
        }

        final MemorySegment dataPtr = dataPtrOut.get(ValueLayout.ADDRESS, 0);
        final long len = lenOut.get(ValueLayout.JAVA_LONG, 0);

        if (dataPtr == null || dataPtr.equals(MemorySegment.NULL) || len <= 0) {
          throw new WasmException("Failed to serialize component: null data returned");
        }

        try {
          final MemorySegment unbounded = dataPtr.reinterpret(len);
          final byte[] result = unbounded.toArray(ValueLayout.JAVA_BYTE);
          return result;
        } finally {
          NATIVE_BINDINGS.componentFreeSerializedData(dataPtr, len);
        }
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public Optional<ResourcesRequired> resourcesRequired() throws WasmException {
    resourceHandle.beginOperation();
    try {

      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment numMemoriesOut = arena.allocate(ValueLayout.JAVA_INT);
        final MemorySegment maxMemoryOut = arena.allocate(ValueLayout.JAVA_LONG);
        final MemorySegment numTablesOut = arena.allocate(ValueLayout.JAVA_INT);
        final MemorySegment maxTableOut = arena.allocate(ValueLayout.JAVA_LONG);

        final int errorCode =
            NATIVE_BINDINGS.panamaComponentResourcesRequired(
                componentHandle, numMemoriesOut, maxMemoryOut, numTablesOut, maxTableOut);

        if (errorCode != 0) {
          throw new WasmException(
              "Failed to get component resources required: native error code " + errorCode);
        }

        final int numMemories = numMemoriesOut.get(ValueLayout.JAVA_INT, 0);

        // -2 sentinel means resources_required() returned None
        if (numMemories == -2) {
          return Optional.empty();
        }

        final long maxMemory = maxMemoryOut.get(ValueLayout.JAVA_LONG, 0);
        final int numTables = numTablesOut.get(ValueLayout.JAVA_INT, 0);
        final long maxTable = maxTableOut.get(ValueLayout.JAVA_LONG, 0);

        return Optional.of(
            new ResourcesRequired(
                0L, // minimumMemoryBytes - not available for components
                maxMemory, // maximumMemoryBytes (-1 if unbounded)
                0, // minimumTableElements - not available for components
                maxTable > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxTable,
                numMemories,
                numTables,
                0, // numGlobals - not available for components
                0)); // numFunctions - not available for components
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.ImageRange imageRange() throws WasmException {
    resourceHandle.beginOperation();
    try {

      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment startPtr = arena.allocate(ValueLayout.JAVA_LONG);
        final MemorySegment endPtr = arena.allocate(ValueLayout.JAVA_LONG);

        final int errorCode =
            NATIVE_BINDINGS.componentImageRange(componentHandle, startPtr, endPtr);
        if (errorCode != 0) {
          throw new WasmException(
              "Failed to get component image range: native error code " + errorCode);
        }

        return new ai.tegmentum.wasmtime4j.ImageRange(
            startPtr.get(ValueLayout.JAVA_LONG, 0), endPtr.get(ValueLayout.JAVA_LONG, 0));
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void initializeCopyOnWriteImage() throws WasmException {
    resourceHandle.beginOperation();
    try {

      final int errorCode = NATIVE_BINDINGS.componentInitializeCowImage(componentHandle);
      if (errorCode != 0) {
        throw new WasmException(
            "Failed to initialize copy-on-write image: native error code " + errorCode);
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.component.ComponentEngine getComponentEngine() {
    return engine;
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed()
        && componentHandle != null
        && !componentHandle.equals(MemorySegment.NULL);
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  MemorySegment getNativeHandle() {
    return componentHandle;
  }

  String getComponentId() {
    return componentId;
  }

  PanamaComponentEngine getEngine() {
    return engine;
  }
}
