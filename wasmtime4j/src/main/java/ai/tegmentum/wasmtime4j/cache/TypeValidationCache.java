package ai.tegmentum.wasmtime4j.cache;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * High-performance cache for WebAssembly type validation results.
 *
 * <p>This cache accelerates WebAssembly module instantiation by storing validation results for
 * function signatures, parameter types, and type compatibility checks. By avoiding repeated
 * validation of identical type structures, this cache can significantly improve performance for
 * applications that instantiate many modules with similar type signatures.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Content-based caching using cryptographic hashes of type signatures
 *   <li>Fast lookup with O(1) average case performance
 *   <li>Automatic cache size management with LRU eviction
 *   <li>Thread-safe concurrent operations
 *   <li>Comprehensive validation result storage including errors and warnings
 *   <li>Performance monitoring and detailed statistics
 * </ul>
 *
 * <p>The cache stores validation results for:
 *
 * <ul>
 *   <li>Function signature compatibility
 *   <li>Parameter type validation
 *   <li>Return type validation
 *   <li>Import/export type matching
 *   <li>Host function binding validation
 *   <li>Memory and table type constraints
 * </ul>
 *
 * @since 1.0.0
 */
public final class TypeValidationCache {

  private static final Logger LOGGER = Logger.getLogger(TypeValidationCache.class.getName());

  /** Singleton instance. */
  private static volatile TypeValidationCache instance;

  /** Lock for singleton initialization. */
  private static final Object INSTANCE_LOCK = new Object();

  /** Whether type validation caching is enabled. */
  private static volatile boolean enabled =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.type.cache.enabled", "true"));

  /** Maximum cache size. */
  private static final int MAX_CACHE_SIZE =
      Integer.parseInt(System.getProperty("wasmtime4j.type.cache.maxSize", "50000"));

  /** Cache TTL in milliseconds. */
  private static final long CACHE_TTL_MS =
      Long.parseLong(System.getProperty("wasmtime4j.type.cache.ttl", "7200000")); // 2 hours

  /** Main validation result cache. */
  private final ConcurrentHashMap<String, ValidationCacheEntry> validationCache =
      new ConcurrentHashMap<>();

  /** Function signature compatibility cache. */
  private final ConcurrentHashMap<String, CompatibilityCacheEntry> compatibilityCache =
      new ConcurrentHashMap<>();

  /** Statistics tracking. */
  private final AtomicLong validationHits = new AtomicLong(0);

  private final AtomicLong validationMisses = new AtomicLong(0);
  private final AtomicLong compatibilityHits = new AtomicLong(0);
  private final AtomicLong compatibilityMisses = new AtomicLong(0);
  private final AtomicLong evictions = new AtomicLong(0);
  private final AtomicLong totalValidationTimeSaved = new AtomicLong(0);

  /** Performance monitoring. */
  private final AtomicLong totalCacheAccessTime = new AtomicLong(0);

  private final AtomicLong totalCacheAccesses = new AtomicLong(0);

  /** Validation result cache entry. */
  private static final class ValidationCacheEntry {
    final String key;
    final ValidationResult result;
    final long createdTime;
    volatile long lastAccessTime;
    volatile int accessCount;
    final long validationTimeMs;

    ValidationCacheEntry(
        final String key, final ValidationResult result, final long validationTimeMs) {
      this.key = key;
      this.result = result;
      this.createdTime = System.currentTimeMillis();
      this.lastAccessTime = createdTime;
      this.accessCount = 1;
      this.validationTimeMs = validationTimeMs;
    }

    void recordAccess() {
      lastAccessTime = System.currentTimeMillis();
      accessCount++;
    }

    boolean isExpired() {
      return (System.currentTimeMillis() - createdTime) > CACHE_TTL_MS;
    }
  }

  /** Type compatibility cache entry. */
  private static final class CompatibilityCacheEntry {
    final String key;
    final boolean compatible;
    final String reason;
    final long createdTime;
    volatile long lastAccessTime;
    volatile int accessCount;

    CompatibilityCacheEntry(final String key, final boolean compatible, final String reason) {
      this.key = key;
      this.compatible = compatible;
      this.reason = reason;
      this.createdTime = System.currentTimeMillis();
      this.lastAccessTime = createdTime;
      this.accessCount = 1;
    }

    void recordAccess() {
      lastAccessTime = System.currentTimeMillis();
      accessCount++;
    }

    boolean isExpired() {
      return (System.currentTimeMillis() - createdTime) > CACHE_TTL_MS;
    }
  }

  /** WebAssembly type validation result. */
  public static final class ValidationResult {
    private final boolean valid;
    private final String[] errors;
    private final String[] warnings;
    private final TypeInfo typeInfo;
    private final long validationTimeMs;

    public ValidationResult(
        final boolean valid,
        final String[] errors,
        final String[] warnings,
        final TypeInfo typeInfo,
        final long validationTimeMs) {
      this.valid = valid;
      this.errors = errors != null ? errors.clone() : new String[0];
      this.warnings = warnings != null ? warnings.clone() : new String[0];
      this.typeInfo = typeInfo;
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

    public TypeInfo getTypeInfo() {
      return typeInfo;
    }

    public long getValidationTimeMs() {
      return validationTimeMs;
    }
  }

  /** Type information for cached validation results. */
  public static final class TypeInfo {
    private final String[] parameterTypes;
    private final String[] returnTypes;
    private final boolean isHostFunction;
    private final String moduleContext;
    private final String[] constraints;

    public TypeInfo(
        final String[] parameterTypes,
        final String[] returnTypes,
        final boolean isHostFunction,
        final String moduleContext,
        final String[] constraints) {
      this.parameterTypes = parameterTypes != null ? parameterTypes.clone() : new String[0];
      this.returnTypes = returnTypes != null ? returnTypes.clone() : new String[0];
      this.isHostFunction = isHostFunction;
      this.moduleContext = moduleContext;
      this.constraints = constraints != null ? constraints.clone() : new String[0];
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

    public String getModuleContext() {
      return moduleContext;
    }

    public String[] getConstraints() {
      return constraints.clone();
    }
  }

  /** Type compatibility result. */
  public static final class CompatibilityResult {
    private final boolean compatible;
    private final String reason;
    private final String[] suggestions;

    public CompatibilityResult(
        final boolean compatible, final String reason, final String[] suggestions) {
      this.compatible = compatible;
      this.reason = reason;
      this.suggestions = suggestions != null ? suggestions.clone() : new String[0];
    }

    public boolean isCompatible() {
      return compatible;
    }

    public String getReason() {
      return reason;
    }

    public String[] getSuggestions() {
      return suggestions.clone();
    }
  }

  // Private constructor for singleton
  private TypeValidationCache() {
    LOGGER.info("Type validation cache initialized with max size: " + MAX_CACHE_SIZE);
  }

  /**
   * Gets the singleton cache instance.
   *
   * @return the type validation cache instance
   */
  public static TypeValidationCache getInstance() {
    if (instance == null) {
      synchronized (INSTANCE_LOCK) {
        if (instance == null) {
          instance = new TypeValidationCache();
        }
      }
    }
    return instance;
  }

  /**
   * Gets cached validation result for given type signature.
   *
   * @param parameterTypes function parameter types
   * @param returnTypes function return types
   * @param moduleContext module context for validation
   * @return cached validation result or null if not found
   */
  public ValidationResult getValidationResult(
      final String[] parameterTypes, final String[] returnTypes, final String moduleContext) {
    if (!enabled) {
      return null;
    }

    final long startTime = System.nanoTime();
    totalCacheAccesses.incrementAndGet();

    try {
      final String cacheKey = generateValidationKey(parameterTypes, returnTypes, moduleContext);
      final ValidationCacheEntry entry = validationCache.get(cacheKey);

      if (entry != null && !entry.isExpired()) {
        entry.recordAccess();
        validationHits.incrementAndGet();
        totalValidationTimeSaved.addAndGet(entry.validationTimeMs);
        return entry.result;
      }

      validationMisses.incrementAndGet();
      return null;

    } finally {
      totalCacheAccessTime.addAndGet(System.nanoTime() - startTime);
    }
  }

  /**
   * Caches a validation result.
   *
   * @param parameterTypes function parameter types
   * @param returnTypes function return types
   * @param moduleContext module context
   * @param result validation result
   * @param validationTimeMs time spent on validation
   */
  public void putValidationResult(
      final String[] parameterTypes,
      final String[] returnTypes,
      final String moduleContext,
      final ValidationResult result,
      final long validationTimeMs) {
    if (!enabled || result == null) {
      return;
    }

    // Check cache size and evict if necessary
    if (validationCache.size() >= MAX_CACHE_SIZE) {
      evictLeastRecentlyUsedValidationEntries();
    }

    final String cacheKey = generateValidationKey(parameterTypes, returnTypes, moduleContext);
    final ValidationCacheEntry entry = new ValidationCacheEntry(cacheKey, result, validationTimeMs);
    validationCache.put(cacheKey, entry);
  }

  /**
   * Gets or computes validation result using the provided supplier.
   *
   * @param parameterTypes function parameter types
   * @param returnTypes function return types
   * @param moduleContext module context
   * @param validator function to compute validation if not cached
   * @return cached or computed validation result
   */
  public ValidationResult computeValidationIfAbsent(
      final String[] parameterTypes,
      final String[] returnTypes,
      final String moduleContext,
      final Supplier<ValidationResult> validator) {
    ValidationResult result = getValidationResult(parameterTypes, returnTypes, moduleContext);
    if (result != null) {
      return result;
    }

    // Compute validation result
    final long startTime = System.currentTimeMillis();
    result = validator.get();
    final long validationTime = System.currentTimeMillis() - startTime;

    if (result != null) {
      putValidationResult(parameterTypes, returnTypes, moduleContext, result, validationTime);
    }

    return result;
  }

  /**
   * Gets cached type compatibility result.
   *
   * @param sourceTypes source type signature
   * @param targetTypes target type signature
   * @param context compatibility context
   * @return compatibility result or null if not cached
   */
  public CompatibilityResult getCompatibilityResult(
      final String[] sourceTypes, final String[] targetTypes, final String context) {
    if (!enabled) {
      return null;
    }

    final long startTime = System.nanoTime();
    totalCacheAccesses.incrementAndGet();

    try {
      final String cacheKey = generateCompatibilityKey(sourceTypes, targetTypes, context);
      final CompatibilityCacheEntry entry = compatibilityCache.get(cacheKey);

      if (entry != null && !entry.isExpired()) {
        entry.recordAccess();
        compatibilityHits.incrementAndGet();
        return new CompatibilityResult(entry.compatible, entry.reason, null);
      }

      compatibilityMisses.incrementAndGet();
      return null;

    } finally {
      totalCacheAccessTime.addAndGet(System.nanoTime() - startTime);
    }
  }

  /**
   * Caches a type compatibility result.
   *
   * @param sourceTypes source type signature
   * @param targetTypes target type signature
   * @param context compatibility context
   * @param result compatibility result
   */
  public void putCompatibilityResult(
      final String[] sourceTypes,
      final String[] targetTypes,
      final String context,
      final CompatibilityResult result) {
    if (!enabled || result == null) {
      return;
    }

    // Check cache size and evict if necessary
    if (compatibilityCache.size() >= MAX_CACHE_SIZE) {
      evictLeastRecentlyUsedCompatibilityEntries();
    }

    final String cacheKey = generateCompatibilityKey(sourceTypes, targetTypes, context);
    final CompatibilityCacheEntry entry =
        new CompatibilityCacheEntry(cacheKey, result.isCompatible(), result.getReason());
    compatibilityCache.put(cacheKey, entry);
  }

  /**
   * Gets or computes compatibility result using the provided supplier.
   *
   * @param sourceTypes source type signature
   * @param targetTypes target type signature
   * @param context compatibility context
   * @param checker function to compute compatibility if not cached
   * @return cached or computed compatibility result
   */
  public CompatibilityResult computeCompatibilityIfAbsent(
      final String[] sourceTypes,
      final String[] targetTypes,
      final String context,
      final Supplier<CompatibilityResult> checker) {
    CompatibilityResult result = getCompatibilityResult(sourceTypes, targetTypes, context);
    if (result != null) {
      return result;
    }

    // Compute compatibility result
    result = checker.get();
    if (result != null) {
      putCompatibilityResult(sourceTypes, targetTypes, context, result);
    }

    return result;
  }

  /** Generates a cache key for validation results. */
  private String generateValidationKey(
      final String[] parameterTypes, final String[] returnTypes, final String moduleContext) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");

      // Add parameter types
      if (parameterTypes != null) {
        for (final String type : parameterTypes) {
          if (type != null) {
            digest.update(type.getBytes());
          }
        }
      }

      // Add return types
      if (returnTypes != null) {
        for (final String type : returnTypes) {
          if (type != null) {
            digest.update(type.getBytes());
          }
        }
      }

      // Add context
      if (moduleContext != null) {
        digest.update(moduleContext.getBytes());
      }

      // Convert to hex
      final byte[] hash = digest.digest();
      final StringBuilder hexString = new StringBuilder();
      for (final byte b : hash) {
        final String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }

      return "val_" + hexString.toString();

    } catch (final NoSuchAlgorithmException e) {
      // Fallback to simple hash
      return "val_fallback_"
          + Arrays.hashCode(parameterTypes)
          + "_"
          + Arrays.hashCode(returnTypes)
          + "_"
          + (moduleContext != null ? moduleContext.hashCode() : 0);
    }
  }

  /** Generates a cache key for compatibility results. */
  private String generateCompatibilityKey(
      final String[] sourceTypes, final String[] targetTypes, final String context) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");

      // Add source types
      if (sourceTypes != null) {
        for (final String type : sourceTypes) {
          if (type != null) {
            digest.update(("src_" + type).getBytes());
          }
        }
      }

      // Add target types
      if (targetTypes != null) {
        for (final String type : targetTypes) {
          if (type != null) {
            digest.update(("tgt_" + type).getBytes());
          }
        }
      }

      // Add context
      if (context != null) {
        digest.update(context.getBytes());
      }

      // Convert to hex
      final byte[] hash = digest.digest();
      final StringBuilder hexString = new StringBuilder();
      for (final byte b : hash) {
        final String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }

      return "comp_" + hexString.toString();

    } catch (final NoSuchAlgorithmException e) {
      // Fallback to simple hash
      return "comp_fallback_"
          + Arrays.hashCode(sourceTypes)
          + "_"
          + Arrays.hashCode(targetTypes)
          + "_"
          + (context != null ? context.hashCode() : 0);
    }
  }

  /** Evicts least recently used validation entries. */
  private void evictLeastRecentlyUsedValidationEntries() {
    final int targetSize = MAX_CACHE_SIZE / 2;

    validationCache.entrySet().stream()
        .sorted(
            (e1, e2) -> Long.compare(e1.getValue().lastAccessTime, e2.getValue().lastAccessTime))
        .limit(validationCache.size() - targetSize)
        .map(entry -> entry.getKey())
        .forEach(
            key -> {
              validationCache.remove(key);
              evictions.incrementAndGet();
            });
  }

  /** Evicts least recently used compatibility entries. */
  private void evictLeastRecentlyUsedCompatibilityEntries() {
    final int targetSize = MAX_CACHE_SIZE / 2;

    compatibilityCache.entrySet().stream()
        .sorted(
            (e1, e2) -> Long.compare(e1.getValue().lastAccessTime, e2.getValue().lastAccessTime))
        .limit(compatibilityCache.size() - targetSize)
        .map(entry -> entry.getKey())
        .forEach(
            key -> {
              compatibilityCache.remove(key);
              evictions.incrementAndGet();
            });
  }

  /** Clears all cached validation and compatibility results. */
  public void clear() {
    validationCache.clear();
    compatibilityCache.clear();

    // Reset statistics
    validationHits.set(0);
    validationMisses.set(0);
    compatibilityHits.set(0);
    compatibilityMisses.set(0);
    evictions.set(0);
    totalValidationTimeSaved.set(0);
    totalCacheAccessTime.set(0);
    totalCacheAccesses.set(0);
  }

  /**
   * Gets comprehensive cache statistics.
   *
   * @return formatted cache statistics
   */
  public String getStatistics() {
    if (!enabled) {
      return "Type validation cache is disabled";
    }

    final long valHits = validationHits.get();
    final long valMisses = validationMisses.get();
    final long compHits = compatibilityHits.get();
    final long compMisses = compatibilityMisses.get();

    final long totalHits = valHits + compHits;
    final long totalMisses = valMisses + compMisses;
    final long total = totalHits + totalMisses;

    final double hitRate = total > 0 ? (totalHits * 100.0) / total : 0.0;
    final double avgAccessTime =
        totalCacheAccesses.get() > 0
            ? (double) totalCacheAccessTime.get() / totalCacheAccesses.get()
            : 0.0;

    final StringBuilder sb = new StringBuilder();
    sb.append("=== Type Validation Cache Statistics ===\n");
    sb.append(String.format("Cache enabled: %b\n", enabled));
    sb.append(
        String.format("Validation cache size: %d/%d\n", validationCache.size(), MAX_CACHE_SIZE));
    sb.append(
        String.format(
            "Compatibility cache size: %d/%d\n", compatibilityCache.size(), MAX_CACHE_SIZE));
    sb.append(String.format("Validation hits: %,d, misses: %,d\n", valHits, valMisses));
    sb.append(String.format("Compatibility hits: %,d, misses: %,d\n", compHits, compMisses));
    sb.append(String.format("Overall hit rate: %.1f%%\n", hitRate));
    sb.append(String.format("Evictions: %,d\n", evictions.get()));
    sb.append(
        String.format("Total validation time saved: %,d ms\n", totalValidationTimeSaved.get()));
    sb.append(String.format("Average access time: %.0f ns\n", avgAccessTime));
    sb.append(String.format("Cache TTL: %,d ms\n", CACHE_TTL_MS));

    return sb.toString();
  }

  /**
   * Gets the overall cache hit rate.
   *
   * @return hit rate as a percentage (0.0 to 100.0)
   */
  public double getHitRate() {
    final long hits = validationHits.get() + compatibilityHits.get();
    final long total = hits + validationMisses.get() + compatibilityMisses.get();
    return total > 0 ? (hits * 100.0) / total : 0.0;
  }

  /**
   * Gets the total validation time saved by caching.
   *
   * @return total time saved in milliseconds
   */
  public long getTotalValidationTimeSaved() {
    return totalValidationTimeSaved.get();
  }

  /**
   * Enables or disables type validation caching.
   *
   * @param enable true to enable caching
   */
  public static void setEnabled(final boolean enable) {
    enabled = enable;
    LOGGER.info("Type validation cache " + (enable ? "enabled" : "disabled"));
  }

  /**
   * Checks if type validation caching is enabled.
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

    // Remove expired entries
    validationCache
        .entrySet()
        .removeIf(
            entry -> {
              if (entry.getValue().isExpired()) {
                evictions.incrementAndGet();
                return true;
              }
              return false;
            });

    compatibilityCache
        .entrySet()
        .removeIf(
            entry -> {
              if (entry.getValue().isExpired()) {
                evictions.incrementAndGet();
                return true;
              }
              return false;
            });
  }
}
