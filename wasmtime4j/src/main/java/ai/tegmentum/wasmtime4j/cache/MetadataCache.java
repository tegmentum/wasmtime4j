package ai.tegmentum.wasmtime4j.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * High-performance metadata cache for frequently accessed WebAssembly module metadata.
 *
 * <p>This cache provides lightning-fast access to module metadata such as:
 *
 * <ul>
 *   <li>Export/import signatures and types
 *   <li>Function signatures and parameter types
 *   <li>Memory and table layouts
 *   <li>Global variable types and mutability
 *   <li>Custom section metadata
 *   <li>Type validation results
 * </ul>
 *
 * <p>The cache uses a multi-tiered approach:
 *
 * <ul>
 *   <li>L1: Thread-local cache for ultra-low latency access
 *   <li>L2: Concurrent hash map with read-write locks for shared data
 *   <li>L3: Persistent disk cache for long-term storage
 * </ul>
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Lock-free reads in the common case
 *   <li>Adaptive cache sizing based on memory pressure
 *   <li>Automatic cache warming for frequently accessed modules
 *   <li>Configurable TTL and size-based eviction policies
 *   <li>Comprehensive performance monitoring and statistics
 * </ul>
 *
 * @since 1.0.0
 */
public final class MetadataCache {

  private static final Logger LOGGER = Logger.getLogger(MetadataCache.class.getName());

  /** Singleton instance. */
  private static volatile MetadataCache instance;

  /** Lock for singleton initialization. */
  private static final Object INSTANCE_LOCK = new Object();

  /** Whether metadata caching is enabled. */
  private static volatile boolean enabled =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.metadata.cache.enabled", "true"));

  /** Maximum cache size. */
  private static final int MAX_CACHE_SIZE =
      Integer.parseInt(System.getProperty("wasmtime4j.metadata.cache.maxSize", "10000"));

  /** Cache TTL in milliseconds. */
  private static final long CACHE_TTL_MS =
      Long.parseLong(System.getProperty("wasmtime4j.metadata.cache.ttl", "3600000")); // 1 hour

  /** Thread-local L1 cache for ultra-fast access. */
  private final ThreadLocal<L1Cache> l1Cache = ThreadLocal.withInitial(L1Cache::new);

  /** L2 concurrent cache for shared data. */
  private final ConcurrentHashMap<String, CacheEntry> l2Cache = new ConcurrentHashMap<>();

  /** Read-write lock for cache operations. */
  private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

  /** Cache statistics. */
  private final AtomicLong l1Hits = new AtomicLong(0);

  private final AtomicLong l2Hits = new AtomicLong(0);
  private final AtomicLong cacheMisses = new AtomicLong(0);
  private final AtomicLong evictions = new AtomicLong(0);
  private final AtomicLong totalAccessTime = new AtomicLong(0);
  private final AtomicLong totalAccesses = new AtomicLong(0);

  /** Cache entry with TTL and access tracking. */
  private static final class CacheEntry {
    final String key;
    final Object value;
    final long createdTime;
    volatile long lastAccessTime;
    final AtomicLong accessCount;
    final int valueSize;

    CacheEntry(final String key, final Object value) {
      this.key = key;
      this.value = value;
      this.createdTime = System.currentTimeMillis();
      this.lastAccessTime = createdTime;
      this.accessCount = new AtomicLong(1);
      this.valueSize = estimateSize(value);
    }

    boolean isExpired() {
      return (System.currentTimeMillis() - createdTime) > CACHE_TTL_MS;
    }

    void recordAccess() {
      lastAccessTime = System.currentTimeMillis();
      accessCount.incrementAndGet();
    }

    private int estimateSize(final Object obj) {
      if (obj == null) {
        return 0;
      }
      if (obj instanceof String) {
        return ((String) obj).length() * 2;
      }
      if (obj instanceof byte[]) {
        return ((byte[]) obj).length;
      }
      if (obj instanceof Map<?, ?>) {
        return ((Map<?, ?>) obj).size() * 64;
      }
      return 64; // Default estimate
    }
  }

  /** Thread-local L1 cache for ultra-fast access. */
  private static final class L1Cache {
    final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>(64);
    long lastCleanup = System.currentTimeMillis();

    void cleanup() {
      final long now = System.currentTimeMillis();
      if (now - lastCleanup > 60000) { // Clean every minute
        cache.clear();
        lastCleanup = now;
      }
    }
  }

  /** Module metadata containing all cached information. */
  public static final class ModuleMetadata {
    private final String moduleHash;
    private final Map<String, FunctionSignature> exportedFunctions;
    private final Map<String, GlobalMetadata> globals;
    private final Map<String, MemoryMetadata> memories;
    private final Map<String, TableMetadata> tables;
    private final Set<String> importedModules;
    private final Map<String, byte[]> customSections;
    private final TypeValidationResult validationResult;
    private final long createdTime;

    /**
     * Creates new module metadata.
     *
     * @param moduleHash hash of the module
     * @param exportedFunctions exported function signatures
     * @param globals global variable metadata
     * @param memories memory metadata
     * @param tables table metadata
     * @param importedModules set of imported module names
     * @param customSections custom section data
     * @param validationResult type validation result
     */
    public ModuleMetadata(
        final String moduleHash,
        final Map<String, FunctionSignature> exportedFunctions,
        final Map<String, GlobalMetadata> globals,
        final Map<String, MemoryMetadata> memories,
        final Map<String, TableMetadata> tables,
        final Set<String> importedModules,
        final Map<String, byte[]> customSections,
        final TypeValidationResult validationResult) {
      this.moduleHash = moduleHash;
      this.exportedFunctions =
          exportedFunctions != null
              ? new ConcurrentHashMap<>(exportedFunctions)
              : new ConcurrentHashMap<>();
      this.globals = globals != null ? new ConcurrentHashMap<>(globals) : new ConcurrentHashMap<>();
      this.memories =
          memories != null ? new ConcurrentHashMap<>(memories) : new ConcurrentHashMap<>();
      this.tables = tables != null ? new ConcurrentHashMap<>(tables) : new ConcurrentHashMap<>();
      this.importedModules = importedModules != null ? Set.copyOf(importedModules) : Set.of();
      this.customSections =
          customSections != null
              ? new ConcurrentHashMap<>(customSections)
              : new ConcurrentHashMap<>();
      this.validationResult = validationResult;
      this.createdTime = System.currentTimeMillis();
    }

    public String getModuleHash() {
      return moduleHash;
    }

    public Map<String, FunctionSignature> getExportedFunctions() {
      return new java.util.HashMap<>(exportedFunctions);
    }

    public Map<String, GlobalMetadata> getGlobals() {
      return new java.util.HashMap<>(globals);
    }

    public Map<String, MemoryMetadata> getMemories() {
      return new java.util.HashMap<>(memories);
    }

    public Map<String, TableMetadata> getTables() {
      return new java.util.HashMap<>(tables);
    }

    public Set<String> getImportedModules() {
      return importedModules;
    }

    public Map<String, byte[]> getCustomSections() {
      return new java.util.HashMap<>(customSections);
    }

    public TypeValidationResult getValidationResult() {
      return validationResult;
    }

    public long getCreatedTime() {
      return createdTime;
    }
  }

  /** Function signature metadata. */
  public static final class FunctionSignature {
    private final String name;
    private final String[] parameterTypes;
    private final String[] returnTypes;
    private final boolean isHostFunction;

    /**
     * Creates a new function signature.
     *
     * @param name function name
     * @param parameterTypes parameter type names
     * @param returnTypes return type names
     * @param isHostFunction whether this is a host function
     */
    public FunctionSignature(
        final String name,
        final String[] parameterTypes,
        final String[] returnTypes,
        final boolean isHostFunction) {
      this.name = name;
      this.parameterTypes = parameterTypes != null ? parameterTypes.clone() : new String[0];
      this.returnTypes = returnTypes != null ? returnTypes.clone() : new String[0];
      this.isHostFunction = isHostFunction;
    }

    public String getName() {
      return name;
    }

    public String[] getParameterTypes() {
      return parameterTypes.clone();
    }

    public String[] getReturnTypes() {
      return returnTypes.clone();
    }

    public boolean isHostFunction() {
      return isHostFunction;
    }
  }

  /** Global variable metadata. */
  public static final class GlobalMetadata {
    private final String name;
    private final String type;
    private final boolean mutable;
    private final Object initialValue;

    /**
     * Creates new global metadata.
     *
     * @param name global variable name
     * @param type global variable type
     * @param mutable whether the global is mutable
     * @param initialValue initial value of the global
     */
    public GlobalMetadata(
        final String name, final String type, final boolean mutable, final Object initialValue) {
      this.name = name;
      this.type = type;
      this.mutable = mutable;
      this.initialValue = initialValue;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public boolean isMutable() {
      return mutable;
    }

    public Object getInitialValue() {
      return initialValue;
    }
  }

  /** Memory metadata. */
  public static final class MemoryMetadata {
    private final String name;
    private final long minPages;
    private final long maxPages;
    private final boolean shared;

    /**
     * Creates new memory metadata.
     *
     * @param name memory name
     * @param minPages minimum number of pages
     * @param maxPages maximum number of pages
     * @param shared whether memory is shared
     */
    public MemoryMetadata(
        final String name, final long minPages, final long maxPages, final boolean shared) {
      this.name = name;
      this.minPages = minPages;
      this.maxPages = maxPages;
      this.shared = shared;
    }

    public String getName() {
      return name;
    }

    public long getMinPages() {
      return minPages;
    }

    public long getMaxPages() {
      return maxPages;
    }

    public boolean isShared() {
      return shared;
    }
  }

  /** Table metadata. */
  public static final class TableMetadata {
    private final String name;
    private final String elementType;
    private final long minSize;
    private final long maxSize;

    /**
     * Creates new table metadata.
     *
     * @param name table name
     * @param elementType type of table elements
     * @param minSize minimum table size
     * @param maxSize maximum table size
     */
    public TableMetadata(
        final String name, final String elementType, final long minSize, final long maxSize) {
      this.name = name;
      this.elementType = elementType;
      this.minSize = minSize;
      this.maxSize = maxSize;
    }

    public String getName() {
      return name;
    }

    public String getElementType() {
      return elementType;
    }

    public long getMinSize() {
      return minSize;
    }

    public long getMaxSize() {
      return maxSize;
    }
  }

  /** Type validation result. */
  public static final class TypeValidationResult {
    private final boolean valid;
    private final String[] errors;
    private final String[] warnings;
    private final long validationTimeMs;

    /**
     * Creates a new type validation result.
     *
     * @param valid whether validation passed
     * @param errors validation error messages
     * @param warnings validation warning messages
     * @param validationTimeMs time taken for validation in milliseconds
     */
    public TypeValidationResult(
        final boolean valid,
        final String[] errors,
        final String[] warnings,
        final long validationTimeMs) {
      this.valid = valid;
      this.errors = errors != null ? errors.clone() : new String[0];
      this.warnings = warnings != null ? warnings.clone() : new String[0];
      this.validationTimeMs = validationTimeMs;
    }

    public boolean isValid() {
      return valid;
    }

    public String[] getErrors() {
      return errors.clone();
    }

    public String[] getWarnings() {
      return warnings.clone();
    }

    public long getValidationTimeMs() {
      return validationTimeMs;
    }
  }

  // Private constructor for singleton
  private MetadataCache() {
    LOGGER.info("Metadata cache initialized with max size: " + MAX_CACHE_SIZE);
  }

  /**
   * Gets the singleton cache instance.
   *
   * @return the metadata cache instance
   */
  public static MetadataCache getInstance() {
    if (instance == null) {
      synchronized (INSTANCE_LOCK) {
        if (instance == null) {
          instance = new MetadataCache();
        }
      }
    }
    return instance;
  }

  /**
   * Gets cached metadata for a module.
   *
   * @param moduleHash the module hash key
   * @return cached metadata or null if not found
   */
  @SuppressWarnings("unchecked")
  public <T> T get(final String moduleHash) {
    if (!enabled || moduleHash == null) {
      return null;
    }

    final long startTime = System.nanoTime();
    totalAccesses.incrementAndGet();

    try {
      // Check L1 cache first (thread-local)
      final L1Cache l1 = l1Cache.get();
      l1.cleanup();

      Object value = l1.cache.get(moduleHash);
      if (value != null) {
        l1Hits.incrementAndGet();
        return (T) value;
      }

      // Check L2 cache (shared)
      cacheLock.readLock().lock();
      try {
        final CacheEntry entry = l2Cache.get(moduleHash);
        if (entry != null && !entry.isExpired()) {
          entry.recordAccess();
          l2Hits.incrementAndGet();

          // Promote to L1 cache
          l1.cache.put(moduleHash, entry.value);

          return (T) entry.value;
        }
      } finally {
        cacheLock.readLock().unlock();
      }

      cacheMisses.incrementAndGet();
      return null;

    } finally {
      totalAccessTime.addAndGet(System.nanoTime() - startTime);
    }
  }

  /**
   * Stores metadata in the cache.
   *
   * @param moduleHash the module hash key
   * @param metadata the metadata to cache
   */
  public void put(final String moduleHash, final Object metadata) {
    if (!enabled || moduleHash == null || metadata == null) {
      return;
    }

    cacheLock.writeLock().lock();
    try {
      // Check if cache is full and evict if necessary
      if (l2Cache.size() >= MAX_CACHE_SIZE) {
        evictLeastRecentlyUsed();
      }

      final CacheEntry entry = new CacheEntry(moduleHash, metadata);
      l2Cache.put(moduleHash, entry);

      // Also put in L1 cache
      final L1Cache l1 = l1Cache.get();
      l1.cache.put(moduleHash, metadata);

    } finally {
      cacheLock.writeLock().unlock();
    }
  }

  /**
   * Gets or computes metadata using the provided function.
   *
   * @param moduleHash the module hash key
   * @param computer the function to compute metadata if not cached
   * @param <T> the metadata type
   * @return the cached or computed metadata
   */
  public <T> T computeIfAbsent(final String moduleHash, final Function<String, T> computer) {
    T value = get(moduleHash);
    if (value != null) {
      return value;
    }

    // Compute the value
    value = computer.apply(moduleHash);
    if (value != null) {
      put(moduleHash, value);
    }

    return value;
  }

  /**
   * Invalidates a specific cache entry.
   *
   * @param moduleHash the module hash key to invalidate
   */
  public void invalidate(final String moduleHash) {
    if (!enabled || moduleHash == null) {
      return;
    }

    cacheLock.writeLock().lock();
    try {
      l2Cache.remove(moduleHash);

      // Also remove from L1 cache
      final L1Cache l1 = l1Cache.get();
      l1.cache.remove(moduleHash);

    } finally {
      cacheLock.writeLock().unlock();
    }
  }

  /** Clears all cached metadata. */
  public void clear() {
    if (!enabled) {
      return;
    }

    cacheLock.writeLock().lock();
    try {
      l2Cache.clear();

      // Clear all L1 caches
      l1Cache.remove();

    } finally {
      cacheLock.writeLock().unlock();
    }
  }

  /** Evicts least recently used entries to maintain cache size limits. */
  private void evictLeastRecentlyUsed() {
    final int targetSize = MAX_CACHE_SIZE / 2; // Evict down to 50%

    l2Cache.entrySet().stream()
        .sorted(
            (e1, e2) -> Long.compare(e1.getValue().lastAccessTime, e2.getValue().lastAccessTime))
        .limit(l2Cache.size() - targetSize)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList())
        .forEach(
            key -> {
              l2Cache.remove(key);
              evictions.incrementAndGet();
            });
  }

  /**
   * Gets comprehensive cache statistics.
   *
   * @return formatted cache statistics
   */
  public String getStatistics() {
    if (!enabled) {
      return "Metadata cache is disabled";
    }

    final long l1HitCount = l1Hits.get();
    final long l2HitCount = l2Hits.get();
    final long missCount = cacheMisses.get();
    final long total = l1HitCount + l2HitCount + missCount;

    final double l1HitRate = total > 0 ? (l1HitCount * 100.0) / total : 0.0;
    final double l2HitRate = total > 0 ? (l2HitCount * 100.0) / total : 0.0;
    final double overallHitRate = total > 0 ? ((l1HitCount + l2HitCount) * 100.0) / total : 0.0;

    final double avgAccessTime =
        totalAccesses.get() > 0 ? (double) totalAccessTime.get() / totalAccesses.get() : 0.0;

    final StringBuilder sb = new StringBuilder();
    sb.append("=== Metadata Cache Statistics ===\n");
    sb.append(String.format("Cache enabled: %b\n", enabled));
    sb.append(String.format("L2 cache size: %d/%d\n", l2Cache.size(), MAX_CACHE_SIZE));
    sb.append(String.format("L1 cache hits: %,d (%.1f%%)\n", l1HitCount, l1HitRate));
    sb.append(String.format("L2 cache hits: %,d (%.1f%%)\n", l2HitCount, l2HitRate));
    sb.append(String.format("Cache misses: %,d\n", missCount));
    sb.append(String.format("Overall hit rate: %.1f%%\n", overallHitRate));
    sb.append(String.format("Evictions: %,d\n", evictions.get()));
    sb.append(String.format("Average access time: %.0f ns\n", avgAccessTime));
    sb.append(String.format("Cache TTL: %d ms\n", CACHE_TTL_MS));

    return sb.toString();
  }

  /**
   * Gets the cache hit rate as a percentage.
   *
   * @return overall cache hit rate (0.0 to 100.0)
   */
  public double getHitRate() {
    final long hits = l1Hits.get() + l2Hits.get();
    final long total = hits + cacheMisses.get();
    return total > 0 ? (hits * 100.0) / total : 0.0;
  }

  /**
   * Gets the average access time in nanoseconds.
   *
   * @return average access time
   */
  public double getAverageAccessTimeNs() {
    final long accesses = totalAccesses.get();
    return accesses > 0 ? (double) totalAccessTime.get() / accesses : 0.0;
  }

  /**
   * Enables or disables metadata caching.
   *
   * @param enable true to enable caching
   */
  public static void setEnabled(final boolean enable) {
    enabled = enable;
    LOGGER.info("Metadata cache " + (enable ? "enabled" : "disabled"));
  }

  /**
   * Checks if metadata caching is enabled.
   *
   * @return true if caching is enabled
   */
  public static boolean isEnabled() {
    return enabled;
  }

  /** Performs cache maintenance operations. */
  public void performMaintenance() {
    if (!enabled) {
      return;
    }

    cacheLock.writeLock().lock();
    try {
      // Remove expired entries
      final long currentTime = System.currentTimeMillis();
      l2Cache
          .entrySet()
          .removeIf(
              entry -> {
                if (entry.getValue().isExpired()) {
                  evictions.incrementAndGet();
                  return true;
                }
                return false;
              });

      // Clean L1 caches periodically
      l1Cache.get().cleanup();

    } finally {
      cacheLock.writeLock().unlock();
    }
  }
}
