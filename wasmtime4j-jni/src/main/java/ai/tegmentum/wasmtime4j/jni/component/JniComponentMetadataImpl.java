package ai.tegmentum.wasmtime4j.jni.component;

import ai.tegmentum.wasmtime4j.component.ComponentMetadata;
import ai.tegmentum.wasmtime4j.component.ComponentPerformanceHints;
import ai.tegmentum.wasmtime4j.jni.JniComponent;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * JNI implementation of ComponentMetadata interface.
 *
 * <p>This class provides component metadata by making JNI calls to retrieve information from the
 * native Wasmtime component representation. It caches metadata to avoid repeated native calls.
 *
 * @since 1.0.0
 */
public final class JniComponentMetadataImpl implements ComponentMetadata {

  private static final Logger LOGGER = Logger.getLogger(JniComponentMetadataImpl.class.getName());

  private final JniComponent.JniComponentHandle componentHandle;
  private final Map<String, Object> cache = new ConcurrentHashMap<>();
  private volatile MetadataStruct cachedMetadata;

  /**
   * Creates a new JNI component metadata implementation.
   *
   * @param componentHandle the component handle to get metadata for
   * @throws IllegalArgumentException if componentHandle is null
   */
  public JniComponentMetadataImpl(final JniComponent.JniComponentHandle componentHandle) {
    JniValidation.requireNonNull(componentHandle, "componentHandle");
    this.componentHandle = componentHandle;
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
    return cache.computeIfAbsent("customProperties", k -> {
      try {
        if (componentHandle.isClosed()) {
          return Collections.emptyMap();
        }

        final String[] keys = nativeGetCustomPropertyKeys(componentHandle.getNativeHandle());
        if (keys == null || keys.length == 0) {
          return Collections.emptyMap();
        }

        final Map<String, Object> properties = new HashMap<>();
        for (final String key : keys) {
          if (key != null && !key.trim().isEmpty()) {
            final String value = nativeGetCustomProperty(componentHandle.getNativeHandle(), key);
            if (value != null) {
              properties.put(key, value);
            }
          }
        }

        return Collections.unmodifiableMap(properties);

      } catch (final Exception e) {
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
    return (List<String>) cache.computeIfAbsent("wasiInterfaces", k -> {
      try {
        if (componentHandle.isClosed()) {
          return Collections.emptyList();
        }

        final String[] interfaces = nativeGetWasiInterfaces(componentHandle.getNativeHandle());
        if (interfaces == null || interfaces.length == 0) {
          return Collections.emptyList();
        }

        final List<String> interfaceList = new java.util.ArrayList<>();
        for (final String interfaceName : interfaces) {
          if (interfaceName != null && !interfaceName.trim().isEmpty()) {
            interfaceList.add(interfaceName);
          }
        }

        return Collections.unmodifiableList(interfaceList);

      } catch (final Exception e) {
        LOGGER.warning("Failed to get WASI interfaces: " + e.getMessage());
        return Collections.emptyList();
      }
    });
  }

  @Override
  public ComponentPerformanceHints getPerformanceHints() {
    return (ComponentPerformanceHints) cache.computeIfAbsent("performanceHints", k -> {
      try {
        if (componentHandle.isClosed()) {
          return new JniComponentPerformanceHintsImpl(null);
        }

        final long hintsHandle = nativeGetPerformanceHints(componentHandle.getNativeHandle());
        return new JniComponentPerformanceHintsImpl(hintsHandle);

      } catch (final Exception e) {
        LOGGER.warning("Failed to get performance hints: " + e.getMessage());
        return new JniComponentPerformanceHintsImpl(null);
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
              cachedMetadata = JniComponentImpl.nativeGetComponentMetadata(
                  componentHandle.getNativeHandle());
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

  // Native method declarations

  /**
   * Gets custom property keys.
   *
   * @param componentHandle the native component handle
   * @return array of property keys or null on failure
   */
  private static native String[] nativeGetCustomPropertyKeys(long componentHandle);

  /**
   * Gets a custom property value.
   *
   * @param componentHandle the native component handle
   * @param key the property key
   * @return property value or null if not found
   */
  private static native String nativeGetCustomProperty(long componentHandle, String key);

  /**
   * Gets WASI interface names.
   *
   * @param componentHandle the native component handle
   * @return array of WASI interface names or null on failure
   */
  private static native String[] nativeGetWasiInterfaces(long componentHandle);

  /**
   * Gets performance hints handle.
   *
   * @param componentHandle the native component handle
   * @return native performance hints handle or 0 on failure
   */
  private static native long nativeGetPerformanceHints(long componentHandle);

  /**
   * Structure containing component metadata from native code.
   */
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
}