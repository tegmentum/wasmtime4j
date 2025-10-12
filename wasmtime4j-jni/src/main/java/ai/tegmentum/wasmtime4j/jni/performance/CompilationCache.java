package ai.tegmentum.wasmtime4j.jni.performance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Compilation cache for WebAssembly modules to improve startup performance.
 *
 * <p>This class implements a persistent compilation cache that stores pre-compiled WebAssembly
 * modules to disk, significantly reducing startup time for frequently used modules.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>SHA-256 based content hashing for cache invalidation
 *   <li>Configurable cache size limits and eviction policies
 *   <li>Thread-safe cache operations with minimal contention
 *   <li>Automatic cache cleanup and garbage collection
 *   <li>Platform and engine version aware caching
 *   <li>Performance monitoring and hit rate tracking
 * </ul>
 *
 * <p>The cache stores compiled modules on disk using a content-addressed scheme where each cached
 * module is identified by the SHA-256 hash of its WebAssembly bytecode combined with compilation
 * settings.
 *
 * <p>Cache directory structure:
 *
 * <pre>
 * ~/.wasmtime4j/cache/
 * ├── modules/
 * │   ├── &lt;sha256-hash&gt;.wasm    # Original bytecode
 * │   └── &lt;sha256-hash&gt;.cache   # Compiled module
 * └── metadata.properties       # Cache metadata
 * </pre>
 *
 * @since 1.0.0
 */
public final class CompilationCache {

  private static final Logger LOGGER = Logger.getLogger(CompilationCache.class.getName());

  /** Default cache directory under user home. */
  private static final String DEFAULT_CACHE_DIR =
      System.getProperty("user.home") + "/.wasmtime4j/cache";

  /** Current cache directory. */
  private static final String CACHE_DIR =
      System.getProperty("wasmtime4j.cache.dir", DEFAULT_CACHE_DIR);

  /** Maximum cache size in bytes. */
  private static final long MAX_CACHE_SIZE =
      Long.parseLong(
          System.getProperty(
              "wasmtime4j.cache.maxSize", String.valueOf(256 * 1024 * 1024))); // 256MB

  /** Maximum number of cached modules. */
  private static final int MAX_CACHED_MODULES =
      Integer.parseInt(System.getProperty("wasmtime4j.cache.maxModules", "1000"));

  /** Whether compilation caching is enabled. */
  private static volatile boolean enabled =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.cache.enabled", "true"));

  /** In-memory cache of module metadata. */
  private static final ConcurrentHashMap<String, CacheEntry> MODULE_CACHE =
      new ConcurrentHashMap<>();

  /** Cache statistics. */
  private static final AtomicLong CACHE_HITS = new AtomicLong(0);

  private static final AtomicLong CACHE_MISSES = new AtomicLong(0);
  private static final AtomicLong CACHE_STORES = new AtomicLong(0);
  private static final AtomicLong CACHE_EVICTIONS = new AtomicLong(0);

  /** Performance monitoring. */
  private static final AtomicLong TOTAL_CACHE_LOAD_TIME_NS = new AtomicLong(0);

  private static final AtomicLong TOTAL_CACHE_STORE_TIME_NS = new AtomicLong(0);
  private static final AtomicLong COMPILATION_TIME_SAVED_NS = new AtomicLong(0);
  private static final AtomicLong CACHE_SIZE_BYTES = new AtomicLong(0);

  /** Advanced caching features. */
  private static final AtomicInteger CACHE_GENERATIONS = new AtomicInteger(1);

  private static final ConcurrentHashMap<String, Long> COMPILATION_TIMES =
      new ConcurrentHashMap<>();
  private static final AtomicLong LAST_MAINTENANCE_TIME =
      new AtomicLong(System.currentTimeMillis());
  private static final long MAINTENANCE_INTERVAL_MS = 300_000; // 5 minutes

  /** Cache initialization flag. */
  private static volatile boolean initialized = false;

  /** Cache entry metadata. */
  private static final class CacheEntry {
    final String hash;
    final long size;
    final long createdTime;
    volatile long lastAccessTime;
    final AtomicInteger accessCount;
    final long originalCompilationTimeNs;
    final int generation;
    volatile boolean verified;
    volatile long verificationTime;

    CacheEntry(final String hash, final long size, final long createdTime) {
      this(hash, size, createdTime, 0, CACHE_GENERATIONS.get());
    }

    CacheEntry(
        final String hash,
        final long size,
        final long createdTime,
        final long compilationTimeNs,
        final int generation) {
      this.hash = hash;
      this.size = size;
      this.createdTime = createdTime;
      this.lastAccessTime = createdTime;
      this.accessCount = new AtomicInteger(0);
      this.originalCompilationTimeNs = compilationTimeNs;
      this.generation = generation;
      this.verified = false;
      this.verificationTime = 0;
    }

    void recordAccess() {
      lastAccessTime = System.currentTimeMillis();
      accessCount.incrementAndGet();
    }

    void markVerified() {
      verified = true;
      verificationTime = System.currentTimeMillis();
    }

    boolean isCurrentGeneration() {
      return generation == CACHE_GENERATIONS.get();
    }

    double getAccessFrequency() {
      final long age = System.currentTimeMillis() - createdTime;
      return age > 0 ? (accessCount.get() * 86400000.0) / age : 0.0; // accesses per day
    }
  }

  // Private constructor - utility class
  private CompilationCache() {}

  /** Initializes the compilation cache. This method is called automatically when needed. */
  private static synchronized void initialize() {
    if (initialized) {
      return;
    }

    if (!enabled) {
      LOGGER.info("Compilation cache is disabled");
      initialized = true;
      return;
    }

    try {
      // Create cache directories
      final Path cacheDir = Paths.get(CACHE_DIR);
      final Path modulesDir = cacheDir.resolve("modules");

      Files.createDirectories(modulesDir);

      // Load existing cache metadata
      loadCacheMetadata();

      initialized = true;
      LOGGER.info("Compilation cache initialized at: " + cacheDir);

    } catch (final IOException e) {
      LOGGER.severe("Failed to initialize compilation cache: " + e.getMessage());
      enabled = false;
      initialized = true;
    }
  }

  /**
   * Attempts to load a compiled module from cache.
   *
   * @param wasmBytes the WebAssembly bytecode
   * @param engineOptions engine compilation options (affects cache key)
   * @return cached compiled module data or null if not found
   */
  public static byte[] loadFromCache(final byte[] wasmBytes, final String engineOptions) {
    return loadFromCache(wasmBytes, engineOptions, 0);
  }

  /**
   * Attempts to load a compiled module from cache with compilation time tracking.
   *
   * @param wasmBytes the WebAssembly bytecode
   * @param engineOptions engine compilation options (affects cache key)
   * @param expectedCompilationTimeNs expected compilation time for performance measurement
   * @return cached compiled module data or null if not found
   */
  public static byte[] loadFromCache(
      final byte[] wasmBytes, final String engineOptions, final long expectedCompilationTimeNs) {
    if (!enabled) {
      return null;
    }

    if (!initialized) {
      initialize();
    }

    if (wasmBytes == null || wasmBytes.length == 0) {
      return null;
    }

    performMaintenanceIfNeeded();

    final long startTime = System.nanoTime();
    final long startTimeForPerf = PerformanceMonitor.startOperation("cache_load");
    try {
      // Generate cache key
      final String cacheKey = generateCacheKey(wasmBytes, engineOptions);
      final Path cacheFile = getCacheFilePath(cacheKey);

      // Check if cached file exists
      if (!Files.exists(cacheFile)) {
        CACHE_MISSES.incrementAndGet();
        return null;
      }

      // Verify cache integrity if needed
      if (!verifyCacheIntegrity(cacheKey, wasmBytes)) {
        CACHE_MISSES.incrementAndGet();
        return null;
      }

      // Load cached module
      final byte[] cachedModule = Files.readAllBytes(cacheFile);

      // Update cache metadata
      final CacheEntry entry = MODULE_CACHE.get(cacheKey);
      if (entry != null) {
        entry.recordAccess();

        // Track compilation time saved
        final long timeSaved =
            entry.originalCompilationTimeNs > 0
                ? entry.originalCompilationTimeNs
                : expectedCompilationTimeNs;
        if (timeSaved > 0) {
          COMPILATION_TIME_SAVED_NS.addAndGet(timeSaved);
        }
      }

      CACHE_HITS.incrementAndGet();
      LOGGER.fine(
          "Cache hit for module: "
              + cacheKey.substring(0, 8)
              + "... "
              + "(saved "
              + (expectedCompilationTimeNs / 1_000_000)
              + "ms compilation time)");

      return cachedModule;

    } catch (final IOException e) {
      LOGGER.warning("Failed to load from cache: " + e.getMessage());
      CACHE_MISSES.incrementAndGet();
      return null;
    } finally {
      final long loadTime = System.nanoTime() - startTime;
      TOTAL_CACHE_LOAD_TIME_NS.addAndGet(loadTime);
      PerformanceMonitor.endOperation("cache_load", startTimeForPerf);
    }
  }

  /**
   * Stores a compiled module in the cache.
   *
   * @param wasmBytes the original WebAssembly bytecode
   * @param compiledModule the compiled module data
   * @param engineOptions engine compilation options
   * @return true if successfully cached
   */
  public static boolean storeInCache(
      final byte[] wasmBytes, final byte[] compiledModule, final String engineOptions) {
    return storeInCache(wasmBytes, compiledModule, engineOptions, 0);
  }

  /**
   * Stores a compiled module in the cache with compilation time tracking.
   *
   * @param wasmBytes the original WebAssembly bytecode
   * @param compiledModule the compiled module data
   * @param engineOptions engine compilation options
   * @param compilationTimeNs compilation time in nanoseconds
   * @return true if successfully cached
   */
  public static boolean storeInCache(
      final byte[] wasmBytes,
      final byte[] compiledModule,
      final String engineOptions,
      final long compilationTimeNs) {
    if (!enabled) {
      return false;
    }

    if (!initialized) {
      initialize();
    }

    if (wasmBytes == null || compiledModule == null) {
      return false;
    }

    final long startTime = System.nanoTime();
    final long startTimeForPerf = PerformanceMonitor.startOperation("cache_store");
    try {
      // Check cache size limits
      if (MODULE_CACHE.size() >= MAX_CACHED_MODULES) {
        evictOldEntries();
      }

      // Generate cache key
      final String cacheKey = generateCacheKey(wasmBytes, engineOptions);
      final Path cacheFile = getCacheFilePath(cacheKey);
      final Path wasmFile = getWasmFilePath(cacheKey);

      // Store compiled module
      Files.write(cacheFile, compiledModule, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

      // Store original bytecode for verification
      Files.write(wasmFile, wasmBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

      // Update metadata with compilation time
      final CacheEntry entry =
          new CacheEntry(
              cacheKey,
              compiledModule.length,
              System.currentTimeMillis(),
              compilationTimeNs,
              CACHE_GENERATIONS.get());
      MODULE_CACHE.put(cacheKey, entry);

      // Track compilation time for future optimizations
      if (compilationTimeNs > 0) {
        COMPILATION_TIMES.put(cacheKey, compilationTimeNs);
      }

      // Update cache size tracking
      CACHE_SIZE_BYTES.addAndGet(compiledModule.length + wasmBytes.length);

      CACHE_STORES.incrementAndGet();
      LOGGER.fine(
          "Cached compiled module: "
              + cacheKey.substring(0, 8)
              + "... ("
              + compiledModule.length
              + " bytes, "
              + (compilationTimeNs / 1_000_000)
              + "ms compilation time)");

      return true;

    } catch (final IOException e) {
      LOGGER.warning("Failed to store in cache: " + e.getMessage());
      return false;
    } finally {
      final long storeTime = System.nanoTime() - startTime;
      TOTAL_CACHE_STORE_TIME_NS.addAndGet(storeTime);
      PerformanceMonitor.endOperation("cache_store", startTimeForPerf);
    }
  }

  /**
   * Generates a cache key for the given WebAssembly bytecode and options.
   *
   * @param wasmBytes the WebAssembly bytecode
   * @param engineOptions engine compilation options
   * @return SHA-256 based cache key
   */
  private static String generateCacheKey(final byte[] wasmBytes, final String engineOptions) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");

      // Include bytecode
      digest.update(wasmBytes);

      // Include engine options
      if (engineOptions != null) {
        digest.update(engineOptions.getBytes());
      }

      // Include system architecture and version info for platform-specific caching
      final String systemInfo = System.getProperty("os.name") + "_" + System.getProperty("os.arch");
      digest.update(systemInfo.getBytes());

      // Convert to hex string
      final byte[] hashBytes = digest.digest();
      final StringBuilder hexString = new StringBuilder();
      for (final byte b : hashBytes) {
        hexString.append(String.format("%02x", b));
      }

      return hexString.toString();

    } catch (final NoSuchAlgorithmException e) {
      // Fallback to simple hash
      return "fallback_"
          + Math.abs((wasmBytes.length + (engineOptions != null ? engineOptions.hashCode() : 0)));
    }
  }

  /** Gets the cache file path for a given cache key. */
  private static Path getCacheFilePath(final String cacheKey) {
    return Paths.get(CACHE_DIR, "modules", cacheKey + ".cache");
  }

  /** Gets the WASM file path for a given cache key. */
  private static Path getWasmFilePath(final String cacheKey) {
    return Paths.get(CACHE_DIR, "modules", cacheKey + ".wasm");
  }

  /** Loads cache metadata from disk. */
  private static void loadCacheMetadata() {
    final Path modulesDir = Paths.get(CACHE_DIR, "modules");
    if (!Files.exists(modulesDir)) {
      return;
    }

    try {
      Files.list(modulesDir)
          .filter(path -> path.toString().endsWith(".cache"))
          .forEach(
              cacheFile -> {
                try {
                  final String fileName = cacheFile.getFileName().toString();
                  final String cacheKey =
                      fileName.substring(0, fileName.length() - 6); // Remove .cache
                  final long size = Files.size(cacheFile);
                  final long createdTime = Files.getLastModifiedTime(cacheFile).toMillis();

                  final CacheEntry entry = new CacheEntry(cacheKey, size, createdTime);
                  MODULE_CACHE.put(cacheKey, entry);

                } catch (final IOException e) {
                  LOGGER.warning("Failed to load cache metadata for: " + cacheFile);
                }
              });

      LOGGER.fine("Loaded " + MODULE_CACHE.size() + " cached modules");

    } catch (final IOException e) {
      LOGGER.warning("Failed to load cache metadata: " + e.getMessage());
    }
  }

  /** Evicts old cache entries to maintain size limits. */
  private static void evictOldEntries() {
    final int targetSize = MAX_CACHED_MODULES / 2; // Evict down to 50% of max

    // Sort entries by access time and frequency
    MODULE_CACHE.entrySet().stream()
        .sorted(
            (e1, e2) -> {
              final CacheEntry entry1 = e1.getValue();
              final CacheEntry entry2 = e2.getValue();

              // Prefer recently accessed entries
              final long timeDiff = entry2.lastAccessTime - entry1.lastAccessTime;
              if (Math.abs(timeDiff) > 3600_000) { // 1 hour difference
                return Long.compare(entry1.lastAccessTime, entry2.lastAccessTime);
              }

              // Then prefer frequently accessed entries
              return Integer.compare(entry1.accessCount.get(), entry2.accessCount.get());
            })
        .limit(MODULE_CACHE.size() - targetSize)
        .forEach(
            entry -> {
              final String cacheKey = entry.getKey();
              MODULE_CACHE.remove(cacheKey);

              // Remove files
              try {
                Files.deleteIfExists(getCacheFilePath(cacheKey));
                Files.deleteIfExists(getWasmFilePath(cacheKey));
                CACHE_EVICTIONS.incrementAndGet();
              } catch (final IOException e) {
                LOGGER.warning("Failed to delete cached file: " + cacheKey);
              }
            });

    LOGGER.fine("Evicted cache entries, remaining: " + MODULE_CACHE.size());
  }

  /**
   * Gets cache statistics.
   *
   * @return formatted cache statistics
   */
  public static String getStatistics() {
    if (!enabled) {
      return "Compilation cache is disabled";
    }

    final long hits = CACHE_HITS.get();
    final long misses = CACHE_MISSES.get();
    final long total = hits + misses;
    final double hitRate = total > 0 ? (hits * 100.0) / total : 0.0;

    final StringBuilder sb = new StringBuilder();
    sb.append(String.format("=== Compilation Cache Statistics ===%n"));
    sb.append(String.format("Cache directory: %s%n", CACHE_DIR));
    sb.append(String.format("Cache enabled: %b%n", enabled));
    sb.append(String.format("Cached modules: %d/%d%n", MODULE_CACHE.size(), MAX_CACHED_MODULES));
    sb.append(String.format("Cache hits: %,d%n", hits));
    sb.append(String.format("Cache misses: %,d%n", misses));
    sb.append(String.format("Hit rate: %.1f%%%n", hitRate));
    sb.append(String.format("Stores: %,d%n", CACHE_STORES.get()));
    sb.append(String.format("Evictions: %,d%n", CACHE_EVICTIONS.get()));

    // Calculate total cache size
    final long totalSize = MODULE_CACHE.values().stream().mapToLong(entry -> entry.size).sum();
    sb.append(
        String.format(
            "Total cache size: %,d bytes (%.1f MB)%n", totalSize, totalSize / (1024.0 * 1024.0)));

    return sb.toString();
  }

  /**
   * Gets the cache hit rate as a percentage.
   *
   * @return hit rate percentage (0.0 to 100.0)
   */
  public static double getHitRate() {
    final long hits = CACHE_HITS.get();
    final long misses = CACHE_MISSES.get();
    final long total = hits + misses;
    return total > 0 ? (hits * 100.0) / total : 0.0;
  }

  /** Clears the entire compilation cache. */
  public static void clear() {
    if (!enabled) {
      return;
    }

    try {
      final Path modulesDir = Paths.get(CACHE_DIR, "modules");
      if (Files.exists(modulesDir)) {
        Files.list(modulesDir)
            .forEach(
                file -> {
                  try {
                    Files.deleteIfExists(file);
                  } catch (final IOException e) {
                    LOGGER.warning("Failed to delete cache file: " + file);
                  }
                });
      }

      MODULE_CACHE.clear();
      CACHE_HITS.set(0);
      CACHE_MISSES.set(0);
      CACHE_STORES.set(0);
      CACHE_EVICTIONS.set(0);

      LOGGER.info("Compilation cache cleared");

    } catch (final IOException e) {
      LOGGER.severe("Failed to clear cache: " + e.getMessage());
    }
  }

  /**
   * Enables or disables compilation caching.
   *
   * @param enable true to enable caching
   */
  public static void setEnabled(final boolean enable) {
    enabled = enable;
    LOGGER.info("Compilation cache " + (enable ? "enabled" : "disabled"));
  }

  /**
   * Checks if compilation caching is enabled.
   *
   * @return true if caching is enabled
   */
  public static boolean isEnabled() {
    return enabled;
  }

  /**
   * Gets the cache directory path.
   *
   * @return cache directory path
   */
  public static String getCacheDirectory() {
    return CACHE_DIR;
  }

  /** Performs cache maintenance (cleanup, optimization). */
  public static void performMaintenance() {
    if (!enabled) {
      return;
    }

    final long startTime = PerformanceMonitor.startOperation("cache_maintenance");
    try {
      // Remove orphaned files
      final Path modulesDir = Paths.get(CACHE_DIR, "modules");
      if (Files.exists(modulesDir)) {
        Files.list(modulesDir)
            .filter(
                file -> {
                  final String fileName = file.getFileName().toString();
                  if (fileName.endsWith(".cache")) {
                    final String key = fileName.substring(0, fileName.length() - 6);
                    return !MODULE_CACHE.containsKey(key);
                  } else if (fileName.endsWith(".wasm")) {
                    final String key = fileName.substring(0, fileName.length() - 5);
                    return !MODULE_CACHE.containsKey(key);
                  }
                  return false;
                })
            .forEach(
                orphanFile -> {
                  try {
                    Files.deleteIfExists(orphanFile);
                    LOGGER.fine("Removed orphaned cache file: " + orphanFile.getFileName());
                  } catch (final IOException e) {
                    LOGGER.warning("Failed to remove orphaned file: " + orphanFile);
                  }
                });
      }

      // Check for cache size limits
      if (MODULE_CACHE.size() > MAX_CACHED_MODULES * 0.9) {
        evictOldEntries();
      }

      LOGGER.fine("Cache maintenance completed");

    } catch (final IOException e) {
      LOGGER.warning("Cache maintenance failed: " + e.getMessage());
    } finally {
      PerformanceMonitor.endOperation("cache_maintenance", startTime);
    }
  }

  /** Performs maintenance if needed based on time interval. */
  private static void performMaintenanceIfNeeded() {
    final long currentTime = System.currentTimeMillis();
    final long lastMaintenance = LAST_MAINTENANCE_TIME.get();

    if (currentTime - lastMaintenance > MAINTENANCE_INTERVAL_MS) {
      if (LAST_MAINTENANCE_TIME.compareAndSet(lastMaintenance, currentTime)) {
        performMaintenance();
      }
    }
  }

  /**
   * Verifies cache integrity for a given cache key.
   *
   * @param cacheKey the cache key to verify
   * @param originalWasmBytes the original WebAssembly bytes for verification
   * @return true if cache entry is valid
   */
  private static boolean verifyCacheIntegrity(
      final String cacheKey, final byte[] originalWasmBytes) {
    try {
      final CacheEntry entry = MODULE_CACHE.get(cacheKey);
      if (entry == null) {
        return false;
      }

      // Skip verification if recently verified
      if (entry.verified
          && (System.currentTimeMillis() - entry.verificationTime) < 300_000) { // 5 minutes
        return true;
      }

      final Path wasmFile = getWasmFilePath(cacheKey);
      if (!Files.exists(wasmFile)) {
        LOGGER.warning("Cache integrity check failed: missing WASM file for " + cacheKey);
        MODULE_CACHE.remove(cacheKey);
        return false;
      }

      // Verify original bytes match
      final byte[] cachedWasmBytes = Files.readAllBytes(wasmFile);
      if (!java.util.Arrays.equals(originalWasmBytes, cachedWasmBytes)) {
        LOGGER.warning("Cache integrity check failed: WASM bytes mismatch for " + cacheKey);
        MODULE_CACHE.remove(cacheKey);
        Files.deleteIfExists(wasmFile);
        Files.deleteIfExists(getCacheFilePath(cacheKey));
        return false;
      }

      entry.markVerified();
      return true;

    } catch (final IOException e) {
      LOGGER.warning("Cache integrity verification failed for " + cacheKey + ": " + e.getMessage());
      return false;
    }
  }

  /**
   * Gets the average cache load time in nanoseconds.
   *
   * @return average load time
   */
  public static double getAverageCacheLoadTimeNs() {
    final long hits = CACHE_HITS.get();
    return hits > 0 ? (double) TOTAL_CACHE_LOAD_TIME_NS.get() / hits : 0.0;
  }

  /**
   * Gets the average cache store time in nanoseconds.
   *
   * @return average store time
   */
  public static double getAverageCacheStoreTimeNs() {
    final long stores = CACHE_STORES.get();
    return stores > 0 ? (double) TOTAL_CACHE_STORE_TIME_NS.get() / stores : 0.0;
  }

  /**
   * Gets the total compilation time saved by caching in nanoseconds.
   *
   * @return total time saved
   */
  public static long getTotalCompilationTimeSavedNs() {
    return COMPILATION_TIME_SAVED_NS.get();
  }

  /**
   * Gets the compilation time savings percentage.
   *
   * @return savings percentage (0.0 to 100.0)
   */
  public static double getCompilationTimeSavingsPercentage() {
    final long totalSaved = COMPILATION_TIME_SAVED_NS.get();
    final long totalCompilationTime =
        COMPILATION_TIMES.values().stream().mapToLong(Long::longValue).sum();

    if (totalCompilationTime > 0) {
      return (totalSaved * 100.0) / (totalSaved + totalCompilationTime);
    }
    return 0.0;
  }

  /**
   * Gets the current cache size in bytes.
   *
   * @return cache size in bytes
   */
  public static long getCacheSizeBytes() {
    return CACHE_SIZE_BYTES.get();
  }

  /** Increments the cache generation to invalidate old entries. */
  public static void incrementGeneration() {
    final int newGeneration = CACHE_GENERATIONS.incrementAndGet();
    LOGGER.info("Cache generation incremented to " + newGeneration);
  }

  /**
   * Gets comprehensive performance metrics for the cache.
   *
   * @return performance metrics string
   */
  public static String getPerformanceMetrics() {
    final long hits = CACHE_HITS.get();
    final long misses = CACHE_MISSES.get();
    final long total = hits + misses;
    final double hitRate = total > 0 ? (hits * 100.0) / total : 0.0;
    final double avgLoadTime = getAverageCacheLoadTimeNs();
    final double avgStoreTime = getAverageCacheStoreTimeNs();
    final long timeSaved = getTotalCompilationTimeSavedNs();
    final double savingsPercentage = getCompilationTimeSavingsPercentage();

    return String.format(
        "Cache Performance: hit_rate=%.1f%%, avg_load=%.0fns, avg_store=%.0fns, "
            + "time_saved=%dms (%.1f%% reduction), cache_size=%d bytes",
        hitRate,
        avgLoadTime,
        avgStoreTime,
        timeSaved / 1_000_000,
        savingsPercentage,
        getCacheSizeBytes());
  }
}
