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

import ai.tegmentum.wasmtime4j.component.ComponentMetadata;
import ai.tegmentum.wasmtime4j.component.ComponentPerformanceHints;
import ai.tegmentum.wasmtime4j.panama.ArenaResourceManager;
import ai.tegmentum.wasmtime4j.panama.MethodHandleCache;
import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import ai.tegmentum.wasmtime4j.panama.PanamaComponent;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of ComponentMetadata interface.
 *
 * <p>This class provides component metadata by making Panama FFI calls to retrieve information from
 * the native Wasmtime component representation. It uses Arena-based memory management and caches
 * metadata to avoid repeated native calls.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Arena-based memory management for efficient resource handling
 *   <li>MethodHandle caching for optimized native function calls
 *   <li>Comprehensive metadata caching to minimize native calls
 *   <li>Defensive programming with proper validation
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaComponentMetadataImpl implements ComponentMetadata {

  private static final Logger LOGGER = Logger.getLogger(PanamaComponentMetadataImpl.class.getName());

  // Core infrastructure
  private final MethodHandleCache methodHandleCache;
  private final ArenaResourceManager resourceManager;
  private final PanamaComponent.PanamaComponentHandle componentHandle;
  private final Map<String, Object> cache = new ConcurrentHashMap<>();
  private volatile MetadataStruct cachedMetadata;

  // Cached method handles for performance
  private final MethodHandle getComponentMetadata;
  private final MethodHandle getCustomPropertyKeys;
  private final MethodHandle getCustomProperty;
  private final MethodHandle getWasiInterfaces;
  private final MethodHandle getPerformanceHints;

  /**
   * Creates a new Panama component metadata implementation.
   *
   * @param componentHandle the component handle to get metadata for
   * @param resourceManager the arena resource manager for memory operations
   * @throws IllegalArgumentException if componentHandle or resourceManager is null
   */
  public PanamaComponentMetadataImpl(
      final PanamaComponent.PanamaComponentHandle componentHandle,
      final ArenaResourceManager resourceManager) {
    PanamaValidation.requireNonNull(componentHandle, "componentHandle");
    PanamaValidation.requireNonNull(resourceManager, "resourceManager");

    this.componentHandle = componentHandle;
    this.resourceManager = resourceManager;
    this.methodHandleCache = MethodHandleCache.getInstance();

    try {
      // Cache method handles for metadata operations
      this.getComponentMetadata =
          methodHandleCache.getMethodHandle(
              "wasmtime4j_component_get_metadata",
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS, // return: metadata struct pointer
                  ValueLayout.ADDRESS // component handle
                  ));

      this.getCustomPropertyKeys =
          methodHandleCache.getMethodHandle(
              "wasmtime4j_component_get_custom_property_keys",
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS, // return: string array pointer
                  ValueLayout.ADDRESS, // component handle
                  ValueLayout.ADDRESS // count output
                  ));

      this.getCustomProperty =
          methodHandleCache.getMethodHandle(
              "wasmtime4j_component_get_custom_property",
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS, // return: string pointer
                  ValueLayout.ADDRESS, // component handle
                  ValueLayout.ADDRESS // key string
                  ));

      this.getWasiInterfaces =
          methodHandleCache.getMethodHandle(
              "wasmtime4j_component_get_wasi_interfaces",
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS, // return: string array pointer
                  ValueLayout.ADDRESS, // component handle
                  ValueLayout.ADDRESS // count output
                  ));

      this.getPerformanceHints =
          methodHandleCache.getMethodHandle(
              "wasmtime4j_component_get_performance_hints",
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS, // return: hints handle
                  ValueLayout.ADDRESS // component handle
                  ));

      LOGGER.fine("Created Panama component metadata with cached method handles");

    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize component metadata method handles", e);
    }
  }

  @Override
  public long getSize() {
    return getMetadataStruct().size;
  }

  @Override
  public int getExportCount() {
    return getMetadataStruct().exportCount;
  }

  @Override
  public int getImportCount() {
    return getMetadataStruct().importCount;
  }

  @Override
  public int getInterfaceCount() {
    return getMetadataStruct().interfaceCount;
  }

  @Override
  public int getResourceTypeCount() {
    return getMetadataStruct().resourceTypeCount;
  }

  @Override
  public int getComplexityScore() {
    return getMetadataStruct().complexityScore;
  }

  @Override
  public Instant getCompilationTime() {
    final long timestamp = getMetadataStruct().compilationTimeMillis;
    return timestamp > 0 ? Instant.ofEpochMilli(timestamp) : Instant.now();
  }

  @Override
  public String getComponentModelVersion() {
    final String version = getMetadataStruct().componentModelVersion;
    return version != null ? version : "unknown";
  }

  @Override
  public String getEngineInfo() {
    final String info = getMetadataStruct().engineInfo;
    return info != null ? info : "wasmtime";
  }

  @Override
  public String getOptimizationLevel() {
    final String level = getMetadataStruct().optimizationLevel;
    return level != null ? level : "default";
  }

  @Override
  public Map<String, Object> getCustomProperties() {
    return cache.computeIfAbsent(
        "customProperties",
        k -> {
          try {
            if (componentHandle.isClosed()) {
              return Collections.emptyMap();
            }

            try (Arena arena = Arena.ofConfined()) {
              // Allocate memory for count output
              MemorySegment countPtr = arena.allocate(ValueLayout.JAVA_INT);

              // Get custom property keys
              MemorySegment keysPtr =
                  (MemorySegment) getCustomPropertyKeys.invoke(
                      componentHandle.getNativeHandle(), countPtr);

              if (keysPtr == null || keysPtr.equals(MemorySegment.NULL)) {
                return Collections.emptyMap();
              }

              int keyCount = countPtr.get(ValueLayout.JAVA_INT, 0);
              if (keyCount <= 0) {
                return Collections.emptyMap();
              }

              final Map<String, Object> properties = new HashMap<>();

              // Read string array
              for (int i = 0; i < keyCount; i++) {
                MemorySegment keyStrPtr = keysPtr.getAtIndex(ValueLayout.ADDRESS, i);
                if (keyStrPtr != null && !keyStrPtr.equals(MemorySegment.NULL)) {
                  String key = keyStrPtr.getString(0);
                  if (key != null && !key.trim().isEmpty()) {
                    // Get property value
                    MemorySegment keySegment = arena.allocateFrom(key);
                    MemorySegment valuePtr =
                        (MemorySegment) getCustomProperty.invoke(
                            componentHandle.getNativeHandle(), keySegment);

                    if (valuePtr != null && !valuePtr.equals(MemorySegment.NULL)) {
                      String value = valuePtr.getString(0);
                      if (value != null) {
                        properties.put(key, value);
                      }
                    }
                  }
                }
              }

              return Collections.unmodifiableMap(properties);
            }

          } catch (final Throwable e) {
            LOGGER.warning("Failed to get custom properties: " + e.getMessage());
            return Collections.emptyMap();
          }
        });
  }

  @Override
  public long getEstimatedMemoryUsage() {
    return getMetadataStruct().estimatedMemoryUsage;
  }

  @Override
  public boolean supportsAsyncExecution() {
    return getMetadataStruct().supportsAsyncExecution;
  }

  @Override
  public boolean usesWasiP2Features() {
    return getMetadataStruct().usesWasiP2Features;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> getWasiInterfaces() {
    return (List<String>)
        cache.computeIfAbsent(
            "wasiInterfaces",
            k -> {
              try {
                if (componentHandle.isClosed()) {
                  return Collections.emptyList();
                }

                try (Arena arena = Arena.ofConfined()) {
                  // Allocate memory for count output
                  MemorySegment countPtr = arena.allocate(ValueLayout.JAVA_INT);

                  // Get WASI interfaces
                  MemorySegment interfacesPtr =
                      (MemorySegment) getWasiInterfaces.invoke(
                          componentHandle.getNativeHandle(), countPtr);

                  if (interfacesPtr == null || interfacesPtr.equals(MemorySegment.NULL)) {
                    return Collections.emptyList();
                  }

                  int interfaceCount = countPtr.get(ValueLayout.JAVA_INT, 0);
                  if (interfaceCount <= 0) {
                    return Collections.emptyList();
                  }

                  final List<String> interfaceList = new ArrayList<>();

                  // Read string array
                  for (int i = 0; i < interfaceCount; i++) {
                    MemorySegment interfaceStrPtr = interfacesPtr.getAtIndex(ValueLayout.ADDRESS, i);
                    if (interfaceStrPtr != null && !interfaceStrPtr.equals(MemorySegment.NULL)) {
                      String interfaceName = interfaceStrPtr.getString(0);
                      if (interfaceName != null && !interfaceName.trim().isEmpty()) {
                        interfaceList.add(interfaceName);
                      }
                    }
                  }

                  return Collections.unmodifiableList(interfaceList);
                }

              } catch (final Throwable e) {
                LOGGER.warning("Failed to get WASI interfaces: " + e.getMessage());
                return Collections.emptyList();
              }
            });
  }

  @Override
  public ComponentPerformanceHints getPerformanceHints() {
    return (ComponentPerformanceHints)
        cache.computeIfAbsent(
            "performanceHints",
            k -> {
              try {
                if (componentHandle.isClosed()) {
                  return new PanamaComponentPerformanceHintsImpl(MemorySegment.NULL, resourceManager);
                }

                MemorySegment hintsHandle =
                    (MemorySegment) getPerformanceHints.invoke(componentHandle.getNativeHandle());
                return new PanamaComponentPerformanceHintsImpl(hintsHandle, resourceManager);

              } catch (final Throwable e) {
                LOGGER.warning("Failed to get performance hints: " + e.getMessage());
                return new PanamaComponentPerformanceHintsImpl(MemorySegment.NULL, resourceManager);
              }
            });
  }

  /**
   * Gets the cached metadata struct, loading it if necessary.
   *
   * @return the metadata struct
   */
  private MetadataStruct getMetadataStruct() {
    if (cachedMetadata == null) {
      synchronized (this) {
        if (cachedMetadata == null) {
          try {
            if (componentHandle.isClosed()) {
              cachedMetadata = new MetadataStruct();
            } else {
              cachedMetadata = loadMetadataFromNative();
              if (cachedMetadata == null) {
                cachedMetadata = new MetadataStruct();
              }
            }
          } catch (final Exception e) {
            LOGGER.warning("Failed to get component metadata: " + e.getMessage());
            cachedMetadata = new MetadataStruct();
          }
        }
      }
    }
    return cachedMetadata;
  }

  /**
   * Loads metadata from native component.
   *
   * @return metadata struct or null if failed
   */
  private MetadataStruct loadMetadataFromNative() {
    try {
      MemorySegment metadataPtr =
          (MemorySegment) getComponentMetadata.invoke(componentHandle.getNativeHandle());

      if (metadataPtr == null || metadataPtr.equals(MemorySegment.NULL)) {
        return new MetadataStruct();
      }

      // Read metadata struct from native memory
      MetadataStruct metadata = new MetadataStruct();

      long offset = 0;
      metadata.size = metadataPtr.get(ValueLayout.JAVA_LONG, offset);
      offset += 8;

      metadata.exportCount = metadataPtr.get(ValueLayout.JAVA_INT, offset);
      offset += 4;

      metadata.importCount = metadataPtr.get(ValueLayout.JAVA_INT, offset);
      offset += 4;

      metadata.interfaceCount = metadataPtr.get(ValueLayout.JAVA_INT, offset);
      offset += 4;

      metadata.resourceTypeCount = metadataPtr.get(ValueLayout.JAVA_INT, offset);
      offset += 4;

      metadata.complexityScore = metadataPtr.get(ValueLayout.JAVA_INT, offset);
      offset += 4;

      metadata.compilationTimeMillis = metadataPtr.get(ValueLayout.JAVA_LONG, offset);
      offset += 8;

      // Read string pointers and convert to Java strings
      MemorySegment versionPtr = metadataPtr.get(ValueLayout.ADDRESS, offset);
      metadata.componentModelVersion =
          (versionPtr != null && !versionPtr.equals(MemorySegment.NULL))
              ? versionPtr.getString(0) : "0.2.1";
      offset += 8;

      MemorySegment enginePtr = metadataPtr.get(ValueLayout.ADDRESS, offset);
      metadata.engineInfo =
          (enginePtr != null && !enginePtr.equals(MemorySegment.NULL))
              ? enginePtr.getString(0) : "wasmtime";
      offset += 8;

      MemorySegment optimizationPtr = metadataPtr.get(ValueLayout.ADDRESS, offset);
      metadata.optimizationLevel =
          (optimizationPtr != null && !optimizationPtr.equals(MemorySegment.NULL))
              ? optimizationPtr.getString(0) : "default";
      offset += 8;

      metadata.estimatedMemoryUsage = metadataPtr.get(ValueLayout.JAVA_LONG, offset);
      offset += 8;

      metadata.supportsAsyncExecution = metadataPtr.get(ValueLayout.JAVA_BOOLEAN, offset);
      offset += 1;

      metadata.usesWasiP2Features = metadataPtr.get(ValueLayout.JAVA_BOOLEAN, offset);

      return metadata;

    } catch (Throwable e) {
      LOGGER.warning("Failed to load metadata from native: " + e.getMessage());
      return new MetadataStruct();
    }
  }

  /** Structure containing component metadata from native code. */
  public static final class MetadataStruct {
    public long size;
    public int exportCount;
    public int importCount;
    public int interfaceCount;
    public int resourceTypeCount;
    public int complexityScore;
    public long compilationTimeMillis;
    public String componentModelVersion;
    public String engineInfo;
    public String optimizationLevel;
    public long estimatedMemoryUsage;
    public boolean supportsAsyncExecution;
    public boolean usesWasiP2Features;

    public MetadataStruct() {
      // Default values
      this.size = 0;
      this.exportCount = 0;
      this.importCount = 0;
      this.interfaceCount = 0;
      this.resourceTypeCount = 0;
      this.complexityScore = 0;
      this.compilationTimeMillis = System.currentTimeMillis();
      this.componentModelVersion = "0.2.1";
      this.engineInfo = "wasmtime";
      this.optimizationLevel = "default";
      this.estimatedMemoryUsage = 0;
      this.supportsAsyncExecution = false;
      this.usesWasiP2Features = false;
    }
  }

  /**
   * Placeholder implementation of performance hints for Panama.
   * This would be a full implementation in a real scenario.
   */
  private static final class PanamaComponentPerformanceHintsImpl implements ComponentPerformanceHints {
    private final MemorySegment hintsHandle;
    private final ArenaResourceManager resourceManager;

    PanamaComponentPerformanceHintsImpl(
        final MemorySegment hintsHandle, final ArenaResourceManager resourceManager) {
      this.hintsHandle = hintsHandle;
      this.resourceManager = resourceManager;
    }

    @Override
    public boolean isComputeIntensive() {
      return false; // Placeholder implementation
    }

    @Override
    public boolean isMemoryIntensive() {
      return false; // Placeholder implementation
    }

    @Override
    public boolean isIOIntensive() {
      return false; // Placeholder implementation
    }

    @Override
    public int getRecommendedThreadPoolSize() {
      return Runtime.getRuntime().availableProcessors(); // Placeholder implementation
    }

    @Override
    public long getEstimatedExecutionTimeMs() {
      return 100; // Placeholder implementation
    }

    @Override
    public boolean benefitsFromPrecompilation() {
      return true; // Placeholder implementation
    }

    @Override
    public List<String> getOptimizationHints() {
      return Collections.emptyList(); // Placeholder implementation
    }
  }
}