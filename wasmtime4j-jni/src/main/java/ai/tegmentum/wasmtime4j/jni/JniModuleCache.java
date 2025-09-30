package ai.tegmentum.wasmtime4j.jni.util;

import ai.tegmentum.wasmtime4j.jni.JniModule;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Advanced module cache with bytecode validation and serialization support.
 *
 * <p>This cache provides efficient storage and retrieval of compiled WebAssembly modules with the
 * following features:
 *
 * <ul>
 *   <li>SHA-256 based content hashing for cache keys
 *   <li>Automatic bytecode validation before caching
 *   <li>Module serialization for persistent storage
 *   <li>Thread-safe concurrent access
 *   <li>Configurable cache size limits
 *   <li>Automatic cleanup of invalid modules
 * </ul>
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * JniModuleCache cache = new JniModuleCache(100); // Max 100 modules
 *
 * // Compile or retrieve from cache
 * JniModule module = cache.getOrCompile(engine, wasmBytes);
 *
 * // Explicitly cache a pre-compiled module
 * cache.put(wasmBytes, module);
 *
 * // Clear invalid modules
 * cache.cleanup();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class JniModuleCache {

  private static final Logger LOGGER = Logger.getLogger(JniModuleCache.class.getName());

  /** Default maximum cache size. */
  private static final int DEFAULT_MAX_SIZE = 50;

  /** Cache entry containing module and metadata. */
  private static final class CacheEntry {
    final JniModule module;
    final long timestamp;
    final int bytecodeHash;

    CacheEntry(final JniModule module, final int bytecodeHash) {
      this.module = module;
      this.timestamp = System.currentTimeMillis();
      this.bytecodeHash = bytecodeHash;
    }

    boolean isValid() {
      return module != null && module.isValid();
    }
  }

  /** Thread-safe cache storage. */
  private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

  /** Maximum cache size. */
  private final int maxSize;

  /** Message digest for creating content hashes. */
  private final MessageDigest digest;

  /** Creates a new module cache with default maximum size. */
  public JniModuleCache() {
    this(DEFAULT_MAX_SIZE);
  }

  /**
   * Creates a new module cache with specified maximum size.
   *
   * @param maxSize maximum number of modules to cache
   * @throws IllegalArgumentException if maxSize is not positive
   */
  public JniModuleCache(final int maxSize) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("Cache size must be positive, got: " + maxSize);
    }

    this.maxSize = maxSize;

    try {
      this.digest = MessageDigest.getInstance("SHA-256");
    } catch (final NoSuchAlgorithmException e) {
      throw new JniException("SHA-256 algorithm not available", e);
    }

    LOGGER.fine("Created JNI module cache with max size: " + maxSize);
  }

  /**
   * Gets a module from cache or compiles it if not found.
   *
   * <p>This method first validates the bytecode, then checks the cache using a content-based hash.
   * If not found, compiles the module and caches the result.
   *
   * @param engine the engine to use for compilation
   * @param bytecode the WebAssembly bytecode
   * @return compiled module (never null)
   * @throws JniException if compilation fails or bytecode is invalid
   */
  public JniModule getOrCompile(final JniEngine engine, final byte[] bytecode) {
    JniValidation.requireNonNull(engine, "engine");
    JniValidation.requireNonEmpty(bytecode, "bytecode");

    // Validate bytecode first
    if (!JniModule.validate(bytecode)) {
      throw new JniException("Invalid WebAssembly bytecode");
    }

    final String cacheKey = createCacheKey(bytecode);
    final int bytecodeHash = bytecode.hashCode();

    // Check cache first
    final CacheEntry cached = cache.get(cacheKey);
    if (cached != null && cached.isValid() && cached.bytecodeHash == bytecodeHash) {
      LOGGER.fine("Cache hit for module: " + cacheKey);
      return cached.module;
    }

    // Compile new module
    LOGGER.fine("Cache miss, compiling module: " + cacheKey);
    final JniModule module;
    try {
      module = (JniModule) engine.compileModule(bytecode);
    } catch (final Exception e) {
      throw new JniException("Failed to compile module for cache", e);
    }

    // Add to cache with size limit enforcement
    put(cacheKey, module, bytecodeHash);

    return module;
  }

  /**
   * Explicitly puts a module in the cache.
   *
   * @param bytecode the original WebAssembly bytecode
   * @param module the compiled module
   * @throws JniException if caching fails
   */
  public void put(final byte[] bytecode, final JniModule module) {
    JniValidation.requireNonEmpty(bytecode, "bytecode");
    JniValidation.requireNonNull(module, "module");

    final String cacheKey = createCacheKey(bytecode);
    final int bytecodeHash = bytecode.hashCode();

    put(cacheKey, module, bytecodeHash);
  }

  /** Internal put method with cache key. */
  private void put(final String cacheKey, final JniModule module, final int bytecodeHash) {
    if (!module.isValid()) {
      LOGGER.warning("Attempting to cache invalid module: " + cacheKey);
      return;
    }

    // Enforce cache size limit
    if (cache.size() >= maxSize) {
      evictOldest();
    }

    final CacheEntry entry = new CacheEntry(module, bytecodeHash);
    cache.put(cacheKey, entry);

    LOGGER.fine("Cached module: " + cacheKey + " (size: " + cache.size() + "/" + maxSize + ")");
  }

  /**
   * Gets a module from cache without compilation.
   *
   * @param bytecode the WebAssembly bytecode to look up
   * @return cached module or null if not found or invalid
   */
  public JniModule get(final byte[] bytecode) {
    JniValidation.requireNonEmpty(bytecode, "bytecode");

    final String cacheKey = createCacheKey(bytecode);
    final int bytecodeHash = bytecode.hashCode();

    final CacheEntry cached = cache.get(cacheKey);
    if (cached != null && cached.isValid() && cached.bytecodeHash == bytecodeHash) {
      return cached.module;
    }

    return null;
  }

  /**
   * Removes a module from the cache.
   *
   * @param bytecode the WebAssembly bytecode
   * @return true if module was removed, false if not found
   */
  public boolean remove(final byte[] bytecode) {
    JniValidation.requireNonEmpty(bytecode, "bytecode");

    final String cacheKey = createCacheKey(bytecode);
    final CacheEntry removed = cache.remove(cacheKey);

    if (removed != null) {
      LOGGER.fine("Removed cached module: " + cacheKey);
      return true;
    }

    return false;
  }

  /**
   * Cleans up invalid modules from the cache.
   *
   * @return number of modules removed
   */
  public int cleanup() {
    int removedCount = 0;

    for (final String key : cache.keySet()) {
      final CacheEntry entry = cache.get(key);
      if (entry == null || !entry.isValid()) {
        if (cache.remove(key, entry)) {
          removedCount++;
          LOGGER.fine("Cleaned up invalid module: " + key);
        }
      }
    }

    if (removedCount > 0) {
      LOGGER.fine("Cleaned up " + removedCount + " invalid modules");
    }

    return removedCount;
  }

  /** Clears all modules from the cache. */
  public void clear() {
    final int size = cache.size();
    cache.clear();
    LOGGER.fine("Cleared module cache (" + size + " modules)");
  }

  /**
   * Gets the current number of cached modules.
   *
   * @return cache size
   */
  public int size() {
    return cache.size();
  }

  /**
   * Gets the maximum cache size.
   *
   * @return maximum cache size
   */
  public int getMaxSize() {
    return maxSize;
  }

  /**
   * Checks if the cache is empty.
   *
   * @return true if cache is empty
   */
  public boolean isEmpty() {
    return cache.isEmpty();
  }

  /** Creates a content-based cache key from bytecode. */
  private String createCacheKey(final byte[] bytecode) {
    synchronized (digest) {
      digest.reset();
      digest.update(bytecode);
      final byte[] hash = digest.digest();

      // Convert hash to hex string
      final StringBuilder sb = new StringBuilder();
      for (final byte b : hash) {
        sb.append(String.format("%02x", b));
      }

      return sb.toString();
    }
  }

  /** Evicts the oldest entry from the cache. */
  private void evictOldest() {
    String oldestKey = null;
    long oldestTime = Long.MAX_VALUE;

    for (final String key : cache.keySet()) {
      final CacheEntry entry = cache.get(key);
      if (entry != null && entry.timestamp < oldestTime) {
        oldestTime = entry.timestamp;
        oldestKey = key;
      }
    }

    if (oldestKey != null) {
      final CacheEntry removed = cache.remove(oldestKey);
      if (removed != null) {
        LOGGER.fine("Evicted oldest cached module: " + oldestKey);
      }
    }
  }
}
