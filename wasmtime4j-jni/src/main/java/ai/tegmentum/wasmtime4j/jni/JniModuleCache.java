package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.cache.CacheConfiguration;
import ai.tegmentum.wasmtime4j.cache.CacheStatistics;
import ai.tegmentum.wasmtime4j.cache.ModuleCache;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheKey;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniExceptionHandler;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.serialization.SerializedModule;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * JNI implementation of the module cache interface.
 *
 * <p>This class provides JNI bindings to the native persistent module cache functionality,
 * enabling efficient storage and retrieval of compiled WebAssembly modules.
 *
 * <p>All methods in this class are thread-safe and implement defensive programming patterns
 * to prevent JVM crashes.
 *
 * @since 1.0.0
 */
public final class JniModuleCache implements ModuleCache {

  private static final Logger LOGGER = Logger.getLogger(JniModuleCache.class.getName());

  // Native library loading
  static {
    NativeLibraryLoader.loadNativeLibrary();
  }

  // Native cache handle
  private final long nativeHandle;

  // Cache configuration
  private final CacheConfiguration configuration;

  // In-memory key tracking for faster lookups
  private final Set<String> keyTracker;

  // Flag to track if this cache has been closed
  private volatile boolean closed = false;

  /**
   * Creates a new JNI module cache with the given configuration.
   *
   * @param configuration the cache configuration
   * @throws WasmException if the native cache cannot be created
   * @throws IllegalArgumentException if configuration is null
   */
  public JniModuleCache(final CacheConfiguration configuration) throws WasmException {
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }

    this.configuration = configuration;
    this.keyTracker = ConcurrentHashMap.newKeySet();

    // Create native cache
    this.nativeHandle = nativeCreateCache(
        configuration.getCacheDirectory().toString(),
        configuration.getMaxSizeMB(),
        configuration.getTtlHours()
    );

    if (this.nativeHandle == 0) {
      throw new WasmException("Failed to create native module cache");
    }

    // Load existing keys from native cache
    loadExistingKeys();

    LOGGER.fine("JNI module cache created with handle: " + this.nativeHandle);
  }

  /**
   * Creates a new JNI module cache with default configuration.
   *
   * @throws WasmException if the native cache cannot be created
   */
  public JniModuleCache() throws WasmException {
    this(CacheConfiguration.defaultConfiguration());
  }

  @Override
  public Optional<SerializedModule> get(final ModuleCacheKey key) {
    if (key == null) {
      throw new IllegalArgumentException("Key cannot be null");
    }

    validateNotClosed();

    final String keyString = key.toString();

    // Quick check using key tracker
    if (!keyTracker.contains(keyString)) {
      incrementMisses();
      return Optional.empty();
    }

    try {
      // Call native cache load
      final long serializedHandle = nativeLoad(nativeHandle, keyString);

      if (serializedHandle == 0) {
        // Remove from tracker if not found in native cache
        keyTracker.remove(keyString);
        incrementMisses();
        return Optional.empty();
      }

      // Create serialized module wrapper
      final SerializedModule serialized = new JniSerializedModule(
          serializedHandle,
          configuration.getDefaultSerializationOptions()
      );

      incrementHits();
      LOGGER.fine("Cache hit for key: " + keyString);
      return Optional.of(serialized);

    } catch (final Exception e) {
      LOGGER.warning("Failed to load from cache: " + e.getMessage());
      incrementMisses();
      return Optional.empty();
    }
  }

  @Override
  public void put(final ModuleCacheKey key, final SerializedModule module) {
    if (key == null) {
      throw new IllegalArgumentException("Key cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }

    validateNotClosed();

    final String keyString = key.toString();

    try {
      // Get native handle if available
      long serializedHandle = 0;
      if (module instanceof JniSerializedModule) {
        serializedHandle = ((JniSerializedModule) module).getNativeHandle();
      }

      boolean success;
      if (serializedHandle != 0) {
        // Store using native handle
        success = nativeStore(nativeHandle, keyString, serializedHandle);
      } else {
        // Store using raw data
        final byte[] data = module.getData();
        success = nativeStoreData(nativeHandle, keyString, data);
      }

      if (success) {
        keyTracker.add(keyString);
        incrementStores();
        LOGGER.fine("Stored module in cache: " + keyString);
      } else {
        LOGGER.warning("Failed to store module in cache: " + keyString);
      }

    } catch (final Exception e) {
      LOGGER.warning("Failed to store in cache: " + e.getMessage());
    }
  }

  @Override
  public void invalidate(final ModuleCacheKey key) {
    if (key == null) {
      throw new IllegalArgumentException("Key cannot be null");
    }

    validateNotClosed();

    final String keyString = key.toString();

    try {
      final boolean success = nativeEvict(nativeHandle, keyString);
      if (success) {
        keyTracker.remove(keyString);
        incrementEvictions();
        LOGGER.fine("Invalidated cache entry: " + keyString);
      }
    } catch (final Exception e) {
      LOGGER.warning("Failed to invalidate cache entry: " + e.getMessage());
    }
  }

  @Override
  public void clear() {
    validateNotClosed();

    try {
      final boolean success = nativeClear(nativeHandle);
      if (success) {
        keyTracker.clear();
        LOGGER.info("Cache cleared");
      } else {
        LOGGER.warning("Failed to clear cache");
      }
    } catch (final Exception e) {
      LOGGER.warning("Failed to clear cache: " + e.getMessage());
    }
  }

  @Override
  public boolean containsKey(final ModuleCacheKey key) {
    if (key == null) {
      throw new IllegalArgumentException("Key cannot be null");
    }

    validateNotClosed();

    final String keyString = key.toString();

    // Quick check using key tracker
    if (!keyTracker.contains(keyString)) {
      return false;
    }

    // Verify with native cache
    try {
      final boolean exists = nativeContainsKey(nativeHandle, keyString);
      if (!exists) {
        // Remove from tracker if not in native cache
        keyTracker.remove(keyString);
      }
      return exists;
    } catch (final Exception e) {
      LOGGER.warning("Failed to check key existence: " + e.getMessage());
      return false;
    }
  }

  @Override
  public long size() {
    validateNotClosed();

    try {
      return nativeGetSize(nativeHandle);
    } catch (final Exception e) {
      LOGGER.warning("Failed to get cache size: " + e.getMessage());
      return keyTracker.size(); // Fallback to tracker size
    }
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public Set<ModuleCacheKey> keySet() {
    validateNotClosed();

    try {
      final String[] keyStrings = nativeGetKeys(nativeHandle);
      final Set<ModuleCacheKey> keys = ConcurrentHashMap.newKeySet();

      for (final String keyString : keyStrings) {
        try {
          final ModuleCacheKey key = ModuleCacheKey.fromString(keyString);
          if (key != null) {
            keys.add(key);
          }
        } catch (final Exception e) {
          LOGGER.warning("Failed to parse cache key: " + keyString);
        }
      }

      return keys;
    } catch (final Exception e) {
      LOGGER.warning("Failed to get cache keys: " + e.getMessage());
      return Set.of(); // Return empty set on failure
    }
  }

  @Override
  public CacheStatistics getStatistics() {
    validateNotClosed();

    try {
      final long[] stats = nativeGetStatistics(nativeHandle);
      if (stats != null && stats.length >= 6) {
        return new CacheStatistics() {
          @Override
          public long getHits() {
            return stats[0];
          }

          @Override
          public long getMisses() {
            return stats[1];
          }

          @Override
          public long getStores() {
            return stats[2];
          }

          @Override
          public long getEvictions() {
            return stats[3];
          }

          @Override
          public long getCurrentEntries() {
            return stats[4];
          }

          @Override
          public long getCurrentSizeBytes() {
            return stats[5];
          }

          @Override
          public double getHitRatio() {
            final long total = getHits() + getMisses();
            return total == 0 ? 0.0 : (double) getHits() / total * 100.0;
          }

          @Override
          public long getAverageEntrySize() {
            final long entries = getCurrentEntries();
            return entries == 0 ? 0 : getCurrentSizeBytes() / entries;
          }
        };
      }
    } catch (final Exception e) {
      LOGGER.warning("Failed to get cache statistics: " + e.getMessage());
    }

    // Return default statistics on failure
    return new CacheStatistics() {
      @Override
      public long getHits() {
        return 0;
      }

      @Override
      public long getMisses() {
        return 0;
      }

      @Override
      public long getStores() {
        return 0;
      }

      @Override
      public long getEvictions() {
        return 0;
      }

      @Override
      public long getCurrentEntries() {
        return keyTracker.size();
      }

      @Override
      public long getCurrentSizeBytes() {
        return 0;
      }

      @Override
      public double getHitRatio() {
        return 0.0;
      }

      @Override
      public long getAverageEntrySize() {
        return 0;
      }
    };
  }

  @Override
  public void performMaintenance() {
    validateNotClosed();

    try {
      final boolean success = nativePerformMaintenance(nativeHandle);
      if (success) {
        // Refresh key tracker after maintenance
        refreshKeyTracker();
        LOGGER.fine("Cache maintenance completed");
      } else {
        LOGGER.warning("Cache maintenance failed");
      }
    } catch (final Exception e) {
      LOGGER.warning("Failed to perform cache maintenance: " + e.getMessage());
    }
  }

  @Override
  public CacheConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public long estimateMemoryUsage() {
    return getStatistics().getCurrentSizeBytes();
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      keyTracker.clear();
      if (nativeHandle != 0) {
        nativeDestroyCache(nativeHandle);
      }
      LOGGER.fine("JNI module cache closed");
    }
  }

  // Private helper methods

  private void validateNotClosed() {
    if (closed) {
      throw new IllegalStateException("Module cache has been closed");
    }
  }

  private void loadExistingKeys() {
    try {
      final String[] keyStrings = nativeGetKeys(nativeHandle);
      for (final String keyString : keyStrings) {
        keyTracker.add(keyString);
      }
      LOGGER.fine("Loaded " + keyStrings.length + " existing cache keys");
    } catch (final Exception e) {
      LOGGER.warning("Failed to load existing cache keys: " + e.getMessage());
    }
  }

  private void refreshKeyTracker() {
    keyTracker.clear();
    loadExistingKeys();
  }

  private void incrementHits() {
    try {
      nativeIncrementHits(nativeHandle);
    } catch (final Exception e) {
      LOGGER.fine("Failed to increment hit counter: " + e.getMessage());
    }
  }

  private void incrementMisses() {
    try {
      nativeIncrementMisses(nativeHandle);
    } catch (final Exception e) {
      LOGGER.fine("Failed to increment miss counter: " + e.getMessage());
    }
  }

  private void incrementStores() {
    try {
      nativeIncrementStores(nativeHandle);
    } catch (final Exception e) {
      LOGGER.fine("Failed to increment store counter: " + e.getMessage());
    }
  }

  private void incrementEvictions() {
    try {
      nativeIncrementEvictions(nativeHandle);
    } catch (final Exception e) {
      LOGGER.fine("Failed to increment eviction counter: " + e.getMessage());
    }
  }

  // Native method declarations

  /**
   * Creates a new native module cache.
   *
   * @param cacheDir the cache directory path
   * @param maxSizeMB maximum cache size in megabytes
   * @param ttlHours time-to-live in hours
   * @return native cache handle, or 0 on failure
   */
  private static native long nativeCreateCache(String cacheDir, long maxSizeMB, long ttlHours);

  /**
   * Destroys a native module cache.
   *
   * @param cacheHandle the cache handle to destroy
   */
  private static native void nativeDestroyCache(long cacheHandle);

  /**
   * Stores a serialized module in the cache.
   *
   * @param cacheHandle the cache handle
   * @param key the cache key
   * @param serializedHandle the serialized module handle
   * @return true on success, false on failure
   */
  private static native boolean nativeStore(long cacheHandle, String key, long serializedHandle);

  /**
   * Stores raw data in the cache.
   *
   * @param cacheHandle the cache handle
   * @param key the cache key
   * @param data the data to store
   * @return true on success, false on failure
   */
  private static native boolean nativeStoreData(long cacheHandle, String key, byte[] data);

  /**
   * Loads a serialized module from the cache.
   *
   * @param cacheHandle the cache handle
   * @param key the cache key
   * @return serialized module handle, or 0 if not found
   */
  private static native long nativeLoad(long cacheHandle, String key);

  /**
   * Evicts an entry from the cache.
   *
   * @param cacheHandle the cache handle
   * @param key the cache key
   * @return true on success, false on failure
   */
  private static native boolean nativeEvict(long cacheHandle, String key);

  /**
   * Clears the entire cache.
   *
   * @param cacheHandle the cache handle
   * @return true on success, false on failure
   */
  private static native boolean nativeClear(long cacheHandle);

  /**
   * Checks if a key exists in the cache.
   *
   * @param cacheHandle the cache handle
   * @param key the cache key
   * @return true if key exists, false otherwise
   */
  private static native boolean nativeContainsKey(long cacheHandle, String key);

  /**
   * Gets the number of entries in the cache.
   *
   * @param cacheHandle the cache handle
   * @return number of entries
   */
  private static native long nativeGetSize(long cacheHandle);

  /**
   * Gets all cache keys.
   *
   * @param cacheHandle the cache handle
   * @return array of cache keys
   */
  private static native String[] nativeGetKeys(long cacheHandle);

  /**
   * Gets cache statistics.
   *
   * @param cacheHandle the cache handle
   * @return statistics array [hits, misses, stores, evictions, entries, size]
   */
  private static native long[] nativeGetStatistics(long cacheHandle);

  /**
   * Performs cache maintenance.
   *
   * @param cacheHandle the cache handle
   * @return true on success, false on failure
   */
  private static native boolean nativePerformMaintenance(long cacheHandle);

  /**
   * Increments the hit counter.
   *
   * @param cacheHandle the cache handle
   */
  private static native void nativeIncrementHits(long cacheHandle);

  /**
   * Increments the miss counter.
   *
   * @param cacheHandle the cache handle
   */
  private static native void nativeIncrementMisses(long cacheHandle);

  /**
   * Increments the store counter.
   *
   * @param cacheHandle the cache handle
   */
  private static native void nativeIncrementStores(long cacheHandle);

  /**
   * Increments the eviction counter.
   *
   * @param cacheHandle the cache handle
   */
  private static native void nativeIncrementEvictions(long cacheHandle);
}