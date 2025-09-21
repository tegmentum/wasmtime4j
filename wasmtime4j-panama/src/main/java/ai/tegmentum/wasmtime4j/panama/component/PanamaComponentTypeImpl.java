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

package ai.tegmentum.wasmtime4j.panama.component;

import ai.tegmentum.wasmtime4j.component.ComponentType;
import ai.tegmentum.wasmtime4j.panama.ArenaResourceManager;
import ai.tegmentum.wasmtime4j.panama.MethodHandleCache;
import ai.tegmentum.wasmtime4j.panama.PanamaComponent;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of ComponentType interface.
 *
 * <p>This class provides component type information by making Panama FFI calls to retrieve type data
 * from the native Wasmtime component representation. It uses Arena-based memory management and
 * caches type information for optimal performance.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Arena-based memory management for efficient resource handling
 *   <li>MethodHandle caching for optimized native function calls
 *   <li>Lazy loading and caching of type information
 *   <li>Defensive programming with proper validation
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaComponentTypeImpl implements ComponentType {

  private static final Logger LOGGER = Logger.getLogger(PanamaComponentTypeImpl.class.getName());

  // Core infrastructure
  private final MethodHandleCache methodHandleCache;
  private final ArenaResourceManager resourceManager;
  private final PanamaComponent.PanamaComponentHandle componentHandle;

  // Cached data
  private volatile List<String> exportNames;
  private volatile List<String> importNames;

  // Cached method handles for performance
  private final MethodHandle getExportNames;
  private final MethodHandle getImportNames;
  private final MethodHandle getComponentName;
  private final MethodHandle getComponentVersion;

  /**
   * Creates a new Panama component type implementation.
   *
   * @param componentHandle the component handle to get type information for
   * @param resourceManager the arena resource manager for memory operations
   * @throws IllegalArgumentException if componentHandle or resourceManager is null
   */
  public PanamaComponentTypeImpl(
      final PanamaComponent.PanamaComponentHandle componentHandle,
      final ArenaResourceManager resourceManager) {
    PanamaValidation.requireNonNull(componentHandle, "componentHandle");
    PanamaValidation.requireNonNull(resourceManager, "resourceManager");

    this.componentHandle = componentHandle;
    this.resourceManager = resourceManager;
    this.methodHandleCache = MethodHandleCache.getInstance();

    try {
      // Cache method handles for type operations
      this.getExportNames =
          methodHandleCache.getMethodHandle(
              "wasmtime4j_component_get_export_names",
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS, // return: string array pointer
                  ValueLayout.ADDRESS, // component handle
                  ValueLayout.ADDRESS // count output
                  ));

      this.getImportNames =
          methodHandleCache.getMethodHandle(
              "wasmtime4j_component_get_import_names",
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS, // return: string array pointer
                  ValueLayout.ADDRESS, // component handle
                  ValueLayout.ADDRESS // count output
                  ));

      this.getComponentName =
          methodHandleCache.getMethodHandle(
              "wasmtime4j_component_get_name",
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS, // return: string pointer
                  ValueLayout.ADDRESS // component handle
                  ));

      this.getComponentVersion =
          methodHandleCache.getMethodHandle(
              "wasmtime4j_component_get_version",
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS, // return: string pointer
                  ValueLayout.ADDRESS // component handle
                  ));

      LOGGER.fine("Created Panama component type with cached method handles");

    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize component type method handles", e);
    }
  }

  @Override
  public List<String> getExportNames() {
    if (exportNames == null) {
      synchronized (this) {
        if (exportNames == null) {
          exportNames = loadExportNames();
        }
      }
    }
    return exportNames;
  }

  @Override
  public List<String> getImportNames() {
    if (importNames == null) {
      synchronized (this) {
        if (importNames == null) {
          importNames = loadImportNames();
        }
      }
    }
    return importNames;
  }

  @Override
  public String getName() {
    try {
      if (componentHandle.isClosed()) {
        return "unknown";
      }

      MemorySegment namePtr = (MemorySegment) getComponentName.invoke(componentHandle.getNativeHandle());
      if (namePtr != null && !namePtr.equals(MemorySegment.NULL)) {
        String name = namePtr.getString(0);
        return name != null ? name : "anonymous";
      }

      return "anonymous";

    } catch (final Throwable e) {
      LOGGER.warning("Failed to get component name: " + e.getMessage());
      return "unknown";
    }
  }

  @Override
  public String getVersion() {
    try {
      if (componentHandle.isClosed()) {
        return "0.0.0";
      }

      MemorySegment versionPtr = (MemorySegment) getComponentVersion.invoke(componentHandle.getNativeHandle());
      if (versionPtr != null && !versionPtr.equals(MemorySegment.NULL)) {
        String version = versionPtr.getString(0);
        return version != null ? version : "0.0.0";
      }

      return "0.0.0";

    } catch (final Throwable e) {
      LOGGER.warning("Failed to get component version: " + e.getMessage());
      return "0.0.0";
    }
  }

  @Override
  public boolean hasExport(final String name) {
    PanamaValidation.requireNonEmpty(name, "name");
    return getExportNames().contains(name);
  }

  @Override
  public boolean hasImport(final String name) {
    PanamaValidation.requireNonEmpty(name, "name");
    return getImportNames().contains(name);
  }

  /**
   * Loads export names from the native component.
   *
   * @return list of export names
   */
  private List<String> loadExportNames() {
    try {
      if (componentHandle.isClosed()) {
        return Collections.emptyList();
      }

      try (Arena arena = Arena.ofConfined()) {
        // Allocate memory for count output
        MemorySegment countPtr = arena.allocate(ValueLayout.JAVA_INT);

        // Get export names
        MemorySegment namesPtr =
            (MemorySegment) getExportNames.invoke(componentHandle.getNativeHandle(), countPtr);

        if (namesPtr == null || namesPtr.equals(MemorySegment.NULL)) {
          return Collections.emptyList();
        }

        int nameCount = countPtr.get(ValueLayout.JAVA_INT, 0);
        if (nameCount <= 0) {
          return Collections.emptyList();
        }

        final List<String> nameList = new ArrayList<>();

        // Read string array
        for (int i = 0; i < nameCount; i++) {
          MemorySegment nameStrPtr = namesPtr.getAtIndex(ValueLayout.ADDRESS, i);
          if (nameStrPtr != null && !nameStrPtr.equals(MemorySegment.NULL)) {
            String name = nameStrPtr.getString(0);
            if (name != null && !name.trim().isEmpty()) {
              nameList.add(name);
            }
          }
        }

        return Collections.unmodifiableList(nameList);
      }

    } catch (final Throwable e) {
      LOGGER.warning("Failed to load export names: " + e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Loads import names from the native component.
   *
   * @return list of import names
   */
  private List<String> loadImportNames() {
    try {
      if (componentHandle.isClosed()) {
        return Collections.emptyList();
      }

      try (Arena arena = Arena.ofConfined()) {
        // Allocate memory for count output
        MemorySegment countPtr = arena.allocate(ValueLayout.JAVA_INT);

        // Get import names
        MemorySegment namesPtr =
            (MemorySegment) getImportNames.invoke(componentHandle.getNativeHandle(), countPtr);

        if (namesPtr == null || namesPtr.equals(MemorySegment.NULL)) {
          return Collections.emptyList();
        }

        int nameCount = countPtr.get(ValueLayout.JAVA_INT, 0);
        if (nameCount <= 0) {
          return Collections.emptyList();
        }

        final List<String> nameList = new ArrayList<>();

        // Read string array
        for (int i = 0; i < nameCount; i++) {
          MemorySegment nameStrPtr = namesPtr.getAtIndex(ValueLayout.ADDRESS, i);
          if (nameStrPtr != null && !nameStrPtr.equals(MemorySegment.NULL)) {
            String name = nameStrPtr.getString(0);
            if (name != null && !name.trim().isEmpty()) {
              nameList.add(name);
            }
          }
        }

        return Collections.unmodifiableList(nameList);
      }

    } catch (final Throwable e) {
      LOGGER.warning("Failed to load import names: " + e.getMessage());
      return Collections.emptyList();
    }
  }
}