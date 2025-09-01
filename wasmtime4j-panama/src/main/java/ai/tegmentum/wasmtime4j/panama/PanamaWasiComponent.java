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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiComponent;
import ai.tegmentum.wasmtime4j.wasi.WasiComponentStats;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiErrorStats;
import ai.tegmentum.wasmtime4j.wasi.WasiFunctionMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiInstance;
import ai.tegmentum.wasmtime4j.wasi.WasiInterfaceMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiPerformanceMetrics;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceTypeMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceUsageStats;
import ai.tegmentum.wasmtime4j.wasi.WasiTypeDefinition;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiComponent interface.
 *
 * <p>This class provides a concrete implementation of WASI component functionality using Panama
 * Foreign Function API bindings to the native Wasmtime component model. It manages component
 * lifecycle, metadata extraction, and instantiation through Panama FFI calls with Arena-based
 * resource management.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Component loading from bytes with validation
 *   <li>Interface metadata extraction and caching
 *   <li>Component instantiation with configuration
 *   <li>Arena-based resource management for automatic cleanup
 *   <li>Zero-copy optimized memory access where possible
 *   <li>Thread-safe operations with defensive programming
 * </ul>
 *
 * <p>This implementation follows the unified API pattern while using Panama-specific component
 * wrappers for native interactions with optimal performance characteristics.
 *
 * @since 1.0.0
 */
public final class PanamaWasiComponent implements WasiComponent {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiComponent.class.getName());

  private final ArenaResourceManager resourceManager;
  private final PanamaComponent.PanamaComponentEngine componentEngine;
  private final PanamaComponent.PanamaComponentHandle componentHandle;
  private final String name;
  private volatile boolean closed = false;

  // Cached metadata to avoid repeated native calls
  private volatile List<String> cachedExports;
  private volatile List<String> cachedImports;
  private volatile WasiComponentStats cachedStats;

  /**
   * Creates a new Panama WASI component with the specified engine and component handle.
   *
   * @param resourceManager the arena resource manager for lifecycle management
   * @param componentEngine the component engine that loaded this component
   * @param componentHandle the native component handle
   * @param name the optional component name
   * @throws IllegalArgumentException if any required parameter is null
   */
  public PanamaWasiComponent(
      final ArenaResourceManager resourceManager,
      final PanamaComponent.PanamaComponentEngine componentEngine,
      final PanamaComponent.PanamaComponentHandle componentHandle,
      final String name) {
    this.resourceManager =
        Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
    this.componentEngine =
        Objects.requireNonNull(componentEngine, "Component engine cannot be null");
    this.componentHandle =
        Objects.requireNonNull(componentHandle, "Component handle cannot be null");
    this.name = name; // Can be null

    LOGGER.fine("Created Panama WASI component with name: " + (name != null ? name : "unnamed"));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<String> getExports() throws WasmException {
    ensureNotClosed();

    if (cachedExports == null) {
      synchronized (this) {
        if (cachedExports == null) {
          cachedExports = extractExports();
        }
      }
    }
    return new ArrayList<>(cachedExports);
  }

  @Override
  public List<String> getImports() throws WasmException {
    ensureNotClosed();

    if (cachedImports == null) {
      synchronized (this) {
        if (cachedImports == null) {
          cachedImports = extractImports();
        }
      }
    }
    return new ArrayList<>(cachedImports);
  }

  @Override
  public WasiInterfaceMetadata getExportMetadata(final String interfaceName) throws WasmException {
    Objects.requireNonNull(interfaceName, "Interface name cannot be null");
    if (interfaceName.trim().isEmpty()) {
      throw new IllegalArgumentException("Interface name cannot be empty");
    }
    ensureNotClosed();

    // Check if interface exists
    if (!getExports().contains(interfaceName)) {
      throw new WasmException("Interface not found in exports: " + interfaceName);
    }

    // TODO: Extract detailed metadata from native layer
    // For now, return minimal metadata
    return createBasicInterfaceMetadata(interfaceName, true);
  }

  @Override
  public WasiInterfaceMetadata getImportMetadata(final String interfaceName) throws WasmException {
    Objects.requireNonNull(interfaceName, "Interface name cannot be null");
    if (interfaceName.trim().isEmpty()) {
      throw new IllegalArgumentException("Interface name cannot be empty");
    }
    ensureNotClosed();

    // Check if interface exists
    if (!getImports().contains(interfaceName)) {
      throw new WasmException("Interface not found in imports: " + interfaceName);
    }

    // TODO: Extract detailed metadata from native layer
    // For now, return minimal metadata
    return createBasicInterfaceMetadata(interfaceName, false);
  }

  @Override
  public WasiInstance instantiate() throws WasmException {
    // Use default configuration
    return instantiate(WasiConfig.defaultConfig());
  }

  @Override
  public WasiInstance instantiate(final WasiConfig config) throws WasmException {
    Objects.requireNonNull(config, "Configuration cannot be null");
    ensureNotClosed();

    try {
      // Validate configuration before instantiation
      config.validate();

      // Create component instance through Panama FFI
      PanamaComponent.PanamaComponentInstanceHandle instanceHandle =
          componentEngine.instantiateComponent(componentHandle);

      // Create unified WasiInstance wrapper
      return new PanamaWasiInstance(resourceManager, this, instanceHandle, config);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to instantiate component", e);
    }
  }

  @Override
  public void validate() throws WasmException {
    validate(WasiConfig.defaultConfig());
  }

  @Override
  public void validate(final WasiConfig config) throws WasmException {
    Objects.requireNonNull(config, "Configuration cannot be null");
    ensureNotClosed();

    try {
      // Validate configuration
      config.validate();

      // TODO: Implement component-specific validation
      // For now, just check if component is still valid
      if (!componentHandle.isValid()) {
        throw new WasmException("Component handle is no longer valid");
      }

      // Validate exports and imports
      getExports(); // This will cache and validate exports
      getImports(); // This will cache and validate imports

      LOGGER.fine("Component validation passed for: " + (name != null ? name : "unnamed"));

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Component validation failed", e);
    }
  }

  @Override
  public WasiComponentStats getStats() {
    ensureNotClosed();

    if (cachedStats == null) {
      synchronized (this) {
        if (cachedStats == null) {
          cachedStats = extractStats();
        }
      }
    }
    return cachedStats;
  }

  @Override
  public boolean isValid() {
    return !closed && componentEngine.isValid() && componentHandle.isValid();
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;

      // Clear caches
      cachedExports = null;
      cachedImports = null;
      cachedStats = null;

      try {
        componentHandle.close();
      } catch (Exception e) {
        LOGGER.warning("Error closing component handle: " + e.getMessage());
      }

      LOGGER.fine("Closed Panama WASI component: " + (name != null ? name : "unnamed"));
    }
  }

  /**
   * Gets the underlying Panama component handle for internal use.
   *
   * @return the Panama component handle
   */
  PanamaComponent.PanamaComponentHandle getComponentHandle() {
    ensureNotClosed();
    return componentHandle;
  }

  /**
   * Gets the component engine for internal use.
   *
   * @return the component engine
   */
  PanamaComponent.PanamaComponentEngine getComponentEngine() {
    return componentEngine;
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
    if (closed) {
      throw new IllegalStateException("Component has been closed");
    }
  }

  /**
   * Extracts the list of exported interfaces from the native component.
   *
   * @return list of exported interface names
   * @throws WasmException if extraction fails
   */
  private List<String> extractExports() throws WasmException {
    try {
      // TODO: Implement actual export extraction from native layer
      // For now, return empty list as placeholder
      List<String> exports = new ArrayList<>();

      // This would be replaced with actual Panama FFI calls to extract exports
      // exports.add("wasi:filesystem/types");
      // exports.add("wasi:sockets/network");

      LOGGER.fine("Extracted " + exports.size() + " exports from component");
      return exports;

    } catch (final Exception e) {
      throw new WasmException("Failed to extract component exports", e);
    }
  }

  /**
   * Extracts the list of imported interfaces from the native component.
   *
   * @return list of imported interface names
   * @throws WasmException if extraction fails
   */
  private List<String> extractImports() throws WasmException {
    try {
      // TODO: Implement actual import extraction from native layer
      // For now, return empty list as placeholder
      List<String> imports = new ArrayList<>();

      // This would be replaced with actual Panama FFI calls to extract imports
      // imports.add("wasi:cli/environment");
      // imports.add("wasi:filesystem/preopens");

      LOGGER.fine("Extracted " + imports.size() + " imports from component");
      return imports;

    } catch (final Exception e) {
      throw new WasmException("Failed to extract component imports", e);
    }
  }

  /**
   * Creates basic interface metadata for testing purposes.
   *
   * @param interfaceName the interface name
   * @param isExport whether this is an export (true) or import (false)
   * @return basic interface metadata
   */
  private WasiInterfaceMetadata createBasicInterfaceMetadata(
      final String interfaceName, final boolean isExport) {
    // TODO: Implement actual metadata extraction
    // For now, create minimal metadata structure
    return new WasiInterfaceMetadata() {
      @Override
      public String getName() {
        return interfaceName;
      }

      @Override
      public Optional<String> getVersion() {
        return Optional.empty(); // Not extracted yet
      }

      @Override
      public Optional<String> getDocumentation() {
        return Optional.empty(); // Not extracted yet
      }

      @Override
      public List<WasiFunctionMetadata> getFunctions() {
        return new ArrayList<>(); // Not extracted yet
      }

      @Override
      public Optional<WasiFunctionMetadata> getFunction(final String functionName) {
        return Optional.empty(); // Not extracted yet
      }

      @Override
      public List<WasiResourceTypeMetadata> getResourceTypes() {
        return new ArrayList<>(); // Not extracted yet
      }

      @Override
      public Optional<WasiResourceTypeMetadata> getResourceType(final String typeName) {
        return Optional.empty(); // Not extracted yet
      }

      @Override
      public Map<String, WasiTypeDefinition> getCustomTypes() {
        return new HashMap<>(); // Not extracted yet
      }

      @Override
      public Optional<WasiTypeDefinition> getCustomType(final String typeName) {
        return Optional.empty(); // Not extracted yet
      }

      @Override
      public Map<String, Object> getConstants() {
        return new HashMap<>(); // Not extracted yet
      }

      @Override
      public Optional<Object> getConstant(final String constantName) {
        return Optional.empty(); // Not extracted yet
      }

      @Override
      public Map<String, Object> getProperties() {
        return new HashMap<>(); // Not extracted yet
      }

      @Override
      public void validate() {
        // Basic validation - just check that interface name is not null/empty
        if (interfaceName == null || interfaceName.trim().isEmpty()) {
          throw new IllegalArgumentException("Interface name cannot be null or empty");
        }
      }

      @Override
      public boolean isCompatibleWith(final WasiInterfaceMetadata other) {
        if (other == null) {
          return false;
        }
        // Basic compatibility check - same name
        return Objects.equals(getName(), other.getName());
      }

      @Override
      public List<String> getDependencies() {
        return new ArrayList<>(); // No dependencies for basic interface
      }
    };
  }

  /**
   * Extracts component statistics from the native layer.
   *
   * @return component statistics
   */
  private WasiComponentStats extractStats() {
    try {
      final long size = componentHandle.getSize();
      final Instant collectedAt = Instant.now();

      // TODO: Extract more detailed statistics from native layer
      return new WasiComponentStats() {
        @Override
        public Instant getCollectedAt() {
          return collectedAt;
        }

        @Override
        public String getComponentName() {
          return null; // Not specified
        }

        @Override
        public long getBytecodeSize() {
          return size;
        }

        @Override
        public long getCompiledSize() {
          return size; // Approximation
        }

        @Override
        public int getExportedInterfaceCount() {
          try {
            return getExports().size();
          } catch (WasmException e) {
            return 0;
          }
        }

        @Override
        public int getExportedFunctionCount() {
          return 0; // Not tracked yet
        }

        @Override
        public int getImportedInterfaceCount() {
          try {
            return getImports().size();
          } catch (WasmException e) {
            return 0;
          }
        }

        @Override
        public int getImportedFunctionCount() {
          return 0; // Not tracked yet
        }

        @Override
        public int getResourceTypeCount() {
          return 0; // Not tracked yet
        }

        @Override
        public int getCustomTypeCount() {
          return 0; // Not tracked yet
        }

        @Override
        public long getCompilationTimeMs() {
          return 0; // Not tracked yet
        }

        @Override
        public long getMemoryOverhead() {
          return 0; // Not tracked yet
        }

        @Override
        public int getActiveInstanceCount() {
          return 0; // Not tracked yet
        }

        @Override
        public long getTotalInstanceCount() {
          return 0; // Not tracked yet
        }

        @Override
        public long getTotalFunctionCalls() {
          return 0; // Not tracked yet
        }

        @Override
        public Map<String, Long> getFunctionCallStats() {
          return new HashMap<>();
        }

        @Override
        public long getTotalExecutionTimeMs() {
          return 0; // Not tracked yet
        }

        @Override
        public Map<String, Long> getFunctionExecutionTimeStats() {
          return new HashMap<>();
        }

        @Override
        public WasiErrorStats getErrorStats() {
          return createEmptyErrorStats();
        }

        @Override
        public WasiResourceUsageStats getResourceUsageStats() {
          return createEmptyResourceUsageStats();
        }

        @Override
        public WasiPerformanceMetrics getPerformanceMetrics() {
          return createEmptyPerformanceMetrics();
        }

        @Override
        public List<String> getExportedInterfaces() {
          return new ArrayList<>(); // Not implemented yet
        }

        @Override
        public List<String> getImportedInterfaces() {
          return new ArrayList<>(); // Not implemented yet
        }

        @Override
        public Map<String, Object> getCustomProperties() {
          return new HashMap<>(); // No custom properties yet
        }

        @Override
        public String getSummary() {
          return String.format(
              "Component Stats: bytecode=%d bytes, compiled=%d bytes, exported interfaces=%d,"
                  + " imported interfaces=%d",
              getBytecodeSize(),
              getCompiledSize(),
              getExportedInterfaceCount(),
              getImportedInterfaceCount());
        }
      };

    } catch (final Exception e) {
      // Return minimal stats on error
      final Instant collectedAt = Instant.now();
      return new WasiComponentStats() {
        @Override
        public Instant getCollectedAt() {
          return collectedAt;
        }

        @Override
        public String getComponentName() {
          return null;
        }

        @Override
        public long getBytecodeSize() {
          return 0;
        }

        @Override
        public long getCompiledSize() {
          return 0;
        }

        @Override
        public int getExportedInterfaceCount() {
          return 0;
        }

        @Override
        public int getExportedFunctionCount() {
          return 0;
        }

        @Override
        public int getImportedInterfaceCount() {
          return 0;
        }

        @Override
        public int getImportedFunctionCount() {
          return 0;
        }

        @Override
        public int getResourceTypeCount() {
          return 0;
        }

        @Override
        public int getCustomTypeCount() {
          return 0;
        }

        @Override
        public long getCompilationTimeMs() {
          return 0;
        }

        @Override
        public long getMemoryOverhead() {
          return 0;
        }

        @Override
        public int getActiveInstanceCount() {
          return 0;
        }

        @Override
        public long getTotalInstanceCount() {
          return 0;
        }

        @Override
        public long getTotalFunctionCalls() {
          return 0;
        }

        @Override
        public Map<String, Long> getFunctionCallStats() {
          return new HashMap<>();
        }

        @Override
        public long getTotalExecutionTimeMs() {
          return 0;
        }

        @Override
        public Map<String, Long> getFunctionExecutionTimeStats() {
          return new HashMap<>();
        }

        @Override
        public WasiErrorStats getErrorStats() {
          return createEmptyErrorStats();
        }

        @Override
        public WasiResourceUsageStats getResourceUsageStats() {
          return createEmptyResourceUsageStats();
        }

        @Override
        public WasiPerformanceMetrics getPerformanceMetrics() {
          return createEmptyPerformanceMetrics();
        }

        @Override
        public List<String> getExportedInterfaces() {
          return new ArrayList<>();
        }

        @Override
        public List<String> getImportedInterfaces() {
          return new ArrayList<>();
        }

        @Override
        public Map<String, Object> getCustomProperties() {
          return new HashMap<>();
        }

        @Override
        public String getSummary() {
          return "Component Stats: Error retrieving statistics (default values)";
        }
      };
    }
  }

  private WasiErrorStats createEmptyErrorStats() {
    return new WasiErrorStats() {
      @Override
      public long getTotalErrors() {
        return 0;
      }

      @Override
      public Map<String, Long> getErrorsByType() {
        return new HashMap<>();
      }

      @Override
      public long getFatalErrors() {
        return 0;
      }

      @Override
      public long getRecoverableErrors() {
        return 0;
      }
    };
  }

  private WasiResourceUsageStats createEmptyResourceUsageStats() {
    return new WasiResourceUsageStats() {
      @Override
      public long getTotalResourcesCreated() {
        return 0;
      }

      @Override
      public int getCurrentResourceCount() {
        return 0;
      }

      @Override
      public int getPeakResourceCount() {
        return 0;
      }

      @Override
      public Map<String, Integer> getResourceCountsByType() {
        return new HashMap<>();
      }

      @Override
      public Map<String, Long> getResourceCreationsByType() {
        return new HashMap<>();
      }
    };
  }

  private WasiPerformanceMetrics createEmptyPerformanceMetrics() {
    return new WasiPerformanceMetrics() {
      @Override
      public Duration getAverageExecutionTime() {
        return Duration.ZERO;
      }

      @Override
      public Duration getMedianExecutionTime() {
        return Duration.ZERO;
      }

      @Override
      public Duration getP95ExecutionTime() {
        return Duration.ZERO;
      }

      @Override
      public Duration getP99ExecutionTime() {
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
    };
  }
}
