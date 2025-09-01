package ai.tegmentum.wasmtime4j.jni;

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
 * JNI implementation of the WasiComponent interface.
 *
 * <p>This class provides a concrete implementation of WASI component functionality using JNI
 * bindings to the native Wasmtime component model. It manages component lifecycle, metadata
 * extraction, and instantiation through JNI calls.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Component loading from bytes with validation
 *   <li>Interface metadata extraction and caching
 *   <li>Component instantiation with configuration
 *   <li>Comprehensive error handling and resource cleanup
 *   <li>Thread-safe operations with defensive programming
 * </ul>
 *
 * <p>This implementation follows the unified API pattern while delegating to JNI-specific component
 * wrappers for native interactions.
 *
 * @since 1.0.0
 */
public final class JniWasiComponent implements WasiComponent {

  private static final Logger LOGGER = Logger.getLogger(JniWasiComponent.class.getName());

  private final JniComponent.JniComponentEngine componentEngine;
  private final JniComponent.JniComponentHandle componentHandle;
  private final String name;
  private volatile boolean closed = false;

  // Cached metadata to avoid repeated native calls
  private volatile List<String> cachedExports;
  private volatile List<String> cachedImports;
  private volatile WasiComponentStats cachedStats;

  /**
   * Creates a new JNI WASI component with the specified engine and component handle.
   *
   * @param componentEngine the component engine that loaded this component
   * @param componentHandle the native component handle
   * @param name the optional component name
   * @throws IllegalArgumentException if componentEngine or componentHandle is null
   */
  public JniWasiComponent(
      final JniComponent.JniComponentEngine componentEngine,
      final JniComponent.JniComponentHandle componentHandle,
      final String name) {
    this.componentEngine =
        Objects.requireNonNull(componentEngine, "Component engine cannot be null");
    this.componentHandle =
        Objects.requireNonNull(componentHandle, "Component handle cannot be null");
    this.name = name; // Can be null

    LOGGER.fine("Created JNI WASI component with name: " + (name != null ? name : "unnamed"));
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

      // Create component instance through JNI
      JniComponent.JniComponentInstanceHandle instanceHandle =
          componentEngine.instantiateComponent(componentHandle);

      // Create unified WasiInstance wrapper
      return new JniWasiInstance(this, instanceHandle, config);

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

      LOGGER.fine("Closed JNI WASI component: " + (name != null ? name : "unnamed"));
    }
  }

  /**
   * Gets the underlying JNI component handle for internal use.
   *
   * @return the JNI component handle
   */
  JniComponent.JniComponentHandle getComponentHandle() {
    ensureNotClosed();
    return componentHandle;
  }

  /**
   * Gets the component engine for internal use.
   *
   * @return the component engine
   */
  JniComponent.JniComponentEngine getComponentEngine() {
    return componentEngine;
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

      // This would be replaced with actual native calls to extract exports
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

      // This would be replaced with actual native calls to extract imports
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
        Objects.requireNonNull(functionName, "Function name cannot be null");
        if (functionName.trim().isEmpty()) {
          throw new IllegalArgumentException("Function name cannot be empty");
        }
        return Optional.empty(); // Not extracted yet
      }

      @Override
      public List<WasiResourceTypeMetadata> getResourceTypes() {
        return new ArrayList<>(); // Not extracted yet
      }

      @Override
      public Optional<WasiResourceTypeMetadata> getResourceType(final String typeName) {
        Objects.requireNonNull(typeName, "Type name cannot be null");
        if (typeName.trim().isEmpty()) {
          throw new IllegalArgumentException("Type name cannot be empty");
        }
        return Optional.empty(); // Not extracted yet
      }

      @Override
      public Map<String, WasiTypeDefinition> getCustomTypes() {
        return new HashMap<>(); // Not extracted yet
      }

      @Override
      public Optional<WasiTypeDefinition> getCustomType(final String typeName) {
        Objects.requireNonNull(typeName, "Type name cannot be null");
        if (typeName.trim().isEmpty()) {
          throw new IllegalArgumentException("Type name cannot be empty");
        }
        return Optional.empty(); // Not extracted yet
      }

      @Override
      public Map<String, Object> getConstants() {
        return new HashMap<>(); // Not extracted yet
      }

      @Override
      public Optional<Object> getConstant(final String constantName) {
        Objects.requireNonNull(constantName, "Constant name cannot be null");
        if (constantName.trim().isEmpty()) {
          throw new IllegalArgumentException("Constant name cannot be empty");
        }
        return Optional.empty(); // Not extracted yet
      }

      @Override
      public List<String> getDependencies() {
        return new ArrayList<>(); // Not extracted yet
      }

      @Override
      public boolean isCompatibleWith(final WasiInterfaceMetadata other) {
        Objects.requireNonNull(other, "Other interface cannot be null");
        // Basic compatibility check - same name for now
        return getName().equals(other.getName());
      }

      @Override
      public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("isExport", isExport);
        return properties;
      }

      @Override
      public void validate() {
        if (interfaceName == null || interfaceName.trim().isEmpty()) {
          throw new IllegalArgumentException("Interface name cannot be null or empty");
        }
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

      // TODO: Extract more detailed statistics from native layer
      return new WasiComponentStats() {
        private final Instant collectedAt = Instant.now();

        @Override
        public Instant getCollectedAt() {
          return collectedAt;
        }

        @Override
        public String getComponentName() {
          return name;
        }

        @Override
        public long getBytecodeSize() {
          return size;
        }

        @Override
        public long getCompiledSize() {
          return size; // Placeholder - same as bytecode for now
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
          return size; // Placeholder
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
          return new HashMap<>(); // Not tracked yet
        }

        @Override
        public long getTotalExecutionTimeMs() {
          return 0; // Not tracked yet
        }

        @Override
        public Map<String, Long> getFunctionExecutionTimeStats() {
          return new HashMap<>(); // Not tracked yet
        }

        @Override
        public WasiErrorStats getErrorStats() {
          return createBasicErrorStats();
        }

        @Override
        public WasiResourceUsageStats getResourceUsageStats() {
          return createBasicResourceUsageStats();
        }

        @Override
        public WasiPerformanceMetrics getPerformanceMetrics() {
          return createBasicPerformanceMetrics();
        }

        @Override
        public List<String> getExportedInterfaces() {
          try {
            return new ArrayList<>(getExports());
          } catch (WasmException e) {
            return new ArrayList<>();
          }
        }

        @Override
        public List<String> getImportedInterfaces() {
          try {
            return new ArrayList<>(getImports());
          } catch (WasmException e) {
            return new ArrayList<>();
          }
        }

        @Override
        public Map<String, Object> getCustomProperties() {
          Map<String, Object> properties = new HashMap<>();
          properties.put("componentValid", isValid());
          properties.put("isClosed", closed);
          return properties;
        }

        @Override
        public String getSummary() {
          return String.format(
              "Component %s: %d bytes, %d exports, %d imports",
              name != null ? name : "unnamed",
              getBytecodeSize(),
              getExportedInterfaceCount(),
              getImportedInterfaceCount());
        }
      };

    } catch (final Exception e) {
      // Return minimal stats on error
      return new WasiComponentStats() {
        private final Instant collectedAt = Instant.now();

        @Override
        public Instant getCollectedAt() {
          return collectedAt;
        }

        @Override
        public String getComponentName() {
          return name;
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
          return createBasicErrorStats();
        }

        @Override
        public WasiResourceUsageStats getResourceUsageStats() {
          return createBasicResourceUsageStats();
        }

        @Override
        public WasiPerformanceMetrics getPerformanceMetrics() {
          return createBasicPerformanceMetrics();
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
          return "Component statistics unavailable due to error";
        }
      };
    }
  }

  /**
   * Creates basic error statistics for placeholder purposes.
   *
   * @return basic error statistics
   */
  private WasiErrorStats createBasicErrorStats() {
    return new WasiErrorStats() {
      @Override
      public long getTotalErrors() {
        return 0; // Not tracked yet
      }

      @Override
      public Map<String, Long> getErrorsByType() {
        return new HashMap<>(); // Not tracked yet
      }

      @Override
      public long getFatalErrors() {
        return 0; // Not tracked yet
      }

      @Override
      public long getRecoverableErrors() {
        return 0; // Not tracked yet
      }
    };
  }

  /**
   * Creates basic resource usage statistics for placeholder purposes.
   *
   * @return basic resource usage statistics
   */
  private WasiResourceUsageStats createBasicResourceUsageStats() {
    return new WasiResourceUsageStats() {
      @Override
      public long getTotalResourcesCreated() {
        return 0; // Not tracked yet
      }

      @Override
      public int getCurrentResourceCount() {
        return 0; // Not tracked yet
      }

      @Override
      public int getPeakResourceCount() {
        return 0; // Not tracked yet
      }

      @Override
      public Map<String, Integer> getResourceCountsByType() {
        return new HashMap<>(); // Not tracked yet
      }

      @Override
      public Map<String, Long> getResourceCreationsByType() {
        return new HashMap<>(); // Not tracked yet
      }
    };
  }

  /**
   * Creates basic performance metrics for placeholder purposes.
   *
   * @return basic performance metrics
   */
  private WasiPerformanceMetrics createBasicPerformanceMetrics() {
    return new WasiPerformanceMetrics() {
      @Override
      public Duration getAverageExecutionTime() {
        return Duration.ZERO; // Not tracked yet
      }

      @Override
      public Duration getMedianExecutionTime() {
        return Duration.ZERO; // Not tracked yet
      }

      @Override
      public Duration getP95ExecutionTime() {
        return Duration.ZERO; // Not tracked yet
      }

      @Override
      public Duration getP99ExecutionTime() {
        return Duration.ZERO; // Not tracked yet
      }

      @Override
      public double getThroughput() {
        return 0.0; // Not tracked yet
      }

      @Override
      public double getMemoryEfficiency() {
        return 0.0; // Not tracked yet
      }
    };
  }
}
