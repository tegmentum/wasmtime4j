package ai.tegmentum.wasmtime4j.cache.impl;

import ai.tegmentum.wasmtime4j.cache.CacheConfiguration;
import ai.tegmentum.wasmtime4j.cache.CacheStatistics;
import ai.tegmentum.wasmtime4j.cache.ModuleCache;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheKey;
import ai.tegmentum.wasmtime4j.serialization.SerializedModule;
import ai.tegmentum.wasmtime4j.serialization.impl.SerializedModuleImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File-based implementation of ModuleCache.
 *
 * <p>This implementation persists cached modules to the file system, providing durability across
 * application restarts. It supports configurable expiration, size limits, and maintenance
 * operations.
 *
 * <p>Cache files are organized in a directory structure and include both the serialized module data
 * and metadata for validation and management.
 *
 * @since 1.0.0
 */
public final class FileBasedModuleCache implements ModuleCache {

  private static final Logger LOGGER = Logger.getLogger(FileBasedModuleCache.class.getName());

  private static final String CACHE_FILE_EXTENSION = ".wasm4j";
  private static final String METADATA_FILE_EXTENSION = ".meta";

  private final Path cacheDirectory;
  private final CacheConfiguration configuration;
  private final CacheStatisticsImpl statistics;
  private final ConcurrentHashMap<String, CacheEntry> indexMap;
  private final ReadWriteLock cacheLock;
  private volatile boolean closed = false;

  /**
   * Creates a new FileBasedModuleCache with the specified configuration.
   *
   * @param configuration the cache configuration
   * @throws IllegalArgumentException if configuration is null or invalid
   * @throws RuntimeException if the cache directory cannot be created
   */
  public FileBasedModuleCache(final CacheConfiguration configuration) {
    this.configuration = validateConfiguration(configuration);
    this.cacheDirectory = createCacheDirectory();
    this.statistics = new CacheStatisticsImpl(configuration.isStatisticsEnabled());
    this.indexMap = new ConcurrentHashMap<>();
    this.cacheLock = new ReentrantReadWriteLock();

    // Load existing cache entries
    try {
      loadExistingEntries();
    } catch (final IOException e) {
      LOGGER.log(Level.WARNING, "Failed to load existing cache entries", e);
    }

    LOGGER.info("FileBasedModuleCache initialized at: " + cacheDirectory);
  }

  @Override
  public Optional<SerializedModule> get(final ModuleCacheKey key) {
    if (key == null) {
      throw new IllegalArgumentException("Cache key cannot be null");
    }

    ensureNotClosed();

    cacheLock.readLock().lock();
    try {
      final long startTime = System.nanoTime();
      final String keyStr = key.toStringRepresentation();
      final CacheEntry entry = indexMap.get(keyStr);

      if (entry == null) {
        statistics.recordMiss();
        return Optional.empty();
      }

      // Check expiration
      if (isExpired(entry)) {
        // Remove expired entry
        cacheLock.readLock().unlock();
        cacheLock.writeLock().lock();
        try {
          removeEntryUnsafe(keyStr, entry);
          statistics.recordMiss();
          return Optional.empty();
        } finally {
          cacheLock.readLock().lock();
          cacheLock.writeLock().unlock();
        }
      }

      // Load module from file
      try {
        final SerializedModule module =
            loadModuleFromFile(entry.cacheFilePath, entry.metadataFilePath);
        entry.lastAccessTime = Instant.now();
        statistics.recordHit();
        statistics.recordOperationTime(System.nanoTime() - startTime);
        return Optional.of(module);
      } catch (final IOException e) {
        LOGGER.log(
            Level.WARNING, "Failed to load module from cache file: " + entry.cacheFilePath, e);
        // Remove corrupted entry
        cacheLock.readLock().unlock();
        cacheLock.writeLock().lock();
        try {
          removeEntryUnsafe(keyStr, entry);
          statistics.recordMiss();
          statistics.recordIntegrityFailure();
          return Optional.empty();
        } finally {
          cacheLock.readLock().lock();
          cacheLock.writeLock().unlock();
        }
      }
    } finally {
      cacheLock.readLock().unlock();
    }
  }

  @Override
  public void put(final ModuleCacheKey key, final SerializedModule module) {
    if (key == null) {
      throw new IllegalArgumentException("Cache key cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Serialized module cannot be null");
    }

    ensureNotClosed();

    cacheLock.writeLock().lock();
    try {
      final long startTime = System.nanoTime();
      final String keyStr = key.toStringRepresentation();

      // Check size limits before adding
      if (configuration.getMaxSize() > 0 && indexMap.size() >= configuration.getMaxSize()) {
        evictEntries(1);
      }

      // Create cache file paths
      final Path cacheFilePath = cacheDirectory.resolve(keyStr + CACHE_FILE_EXTENSION);
      final Path metadataFilePath = cacheDirectory.resolve(keyStr + METADATA_FILE_EXTENSION);

      try {
        // Write module data to file
        Files.write(
            cacheFilePath,
            module.getData(),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING);

        // Write metadata (simplified for now - in production this would be proper serialization)
        final String metadataJson = createMetadataJson(module, key);
        Files.write(
            metadataFilePath,
            metadataJson.getBytes(),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING);

        // Add to index
        final CacheEntry entry =
            new CacheEntry(cacheFilePath, metadataFilePath, Instant.now(), key);
        indexMap.put(keyStr, entry);

        statistics.recordPut();
        statistics.recordOperationTime(System.nanoTime() - startTime);

        LOGGER.fine("Cached module with key: " + keyStr);
      } catch (final IOException e) {
        LOGGER.log(Level.WARNING, "Failed to write module to cache", e);
        // Clean up partial files
        try {
          Files.deleteIfExists(cacheFilePath);
          Files.deleteIfExists(metadataFilePath);
        } catch (final IOException cleanupEx) {
          LOGGER.log(Level.WARNING, "Failed to clean up partial cache files", cleanupEx);
        }
        throw new RuntimeException("Failed to cache module", e);
      }
    } finally {
      cacheLock.writeLock().unlock();
    }
  }

  @Override
  public void invalidate(final ModuleCacheKey key) {
    if (key == null) {
      throw new IllegalArgumentException("Cache key cannot be null");
    }

    ensureNotClosed();

    cacheLock.writeLock().lock();
    try {
      final String keyStr = key.toStringRepresentation();
      final CacheEntry entry = indexMap.get(keyStr);
      if (entry != null) {
        removeEntryUnsafe(keyStr, entry);
      }
    } finally {
      cacheLock.writeLock().unlock();
    }
  }

  @Override
  public void clear() {
    ensureNotClosed();

    cacheLock.writeLock().lock();
    try {
      // Remove all cache files
      for (final CacheEntry entry : indexMap.values()) {
        try {
          Files.deleteIfExists(entry.cacheFilePath);
          Files.deleteIfExists(entry.metadataFilePath);
        } catch (final IOException e) {
          LOGGER.log(Level.WARNING, "Failed to delete cache file: " + entry.cacheFilePath, e);
        }
      }

      indexMap.clear();
      statistics.reset();
      LOGGER.info("Cache cleared");
    } finally {
      cacheLock.writeLock().unlock();
    }
  }

  @Override
  public boolean containsKey(final ModuleCacheKey key) {
    if (key == null) {
      throw new IllegalArgumentException("Cache key cannot be null");
    }

    ensureNotClosed();

    cacheLock.readLock().lock();
    try {
      final String keyStr = key.toStringRepresentation();
      final CacheEntry entry = indexMap.get(keyStr);
      return entry != null && !isExpired(entry);
    } finally {
      cacheLock.readLock().unlock();
    }
  }

  @Override
  public long size() {
    ensureNotClosed();

    cacheLock.readLock().lock();
    try {
      return indexMap.size();
    } finally {
      cacheLock.readLock().unlock();
    }
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public Set<ModuleCacheKey> keySet() {
    ensureNotClosed();

    cacheLock.readLock().lock();
    try {
      return indexMap.values().stream()
          .filter(entry -> !isExpired(entry))
          .map(entry -> entry.originalKey)
          .collect(Collectors.toSet());
    } finally {
      cacheLock.readLock().unlock();
    }
  }

  @Override
  public CacheStatistics getStatistics() {
    return statistics.snapshot();
  }

  @Override
  public void performMaintenance() {
    ensureNotClosed();

    cacheLock.writeLock().lock();
    try {
      final long startTime = System.nanoTime();

      // Remove expired entries
      final long expiredCount = removeExpiredEntries();

      // Check size limits and evict if necessary
      final long evictedCount = enforceSizeLimits();

      statistics.recordMaintenanceOperation();
      statistics.recordOperationTime(System.nanoTime() - startTime);

      if (expiredCount > 0 || evictedCount > 0) {
        LOGGER.info(
            String.format(
                "Maintenance completed: %d expired entries removed, %d entries evicted",
                expiredCount, evictedCount));
      }
    } finally {
      cacheLock.writeLock().unlock();
    }
  }

  @Override
  public CacheConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public long estimateMemoryUsage() {
    // File-based cache uses minimal memory (just the index)
    return indexMap.size() * 1024L; // Rough estimate
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    cacheLock.writeLock().lock();
    try {
      closed = true;
      LOGGER.info("FileBasedModuleCache closed");
    } finally {
      cacheLock.writeLock().unlock();
    }
  }

  private CacheConfiguration validateConfiguration(final CacheConfiguration config) {
    if (config == null) {
      throw new IllegalArgumentException("Cache configuration cannot be null");
    }
    if (!config.isPersistenceEnabled() || config.getPersistencePath() == null) {
      throw new IllegalArgumentException(
          "File-based cache requires persistence to be enabled with a valid path");
    }
    return config;
  }

  private Path createCacheDirectory() {
    final Path dir = Paths.get(configuration.getPersistencePath());
    try {
      Files.createDirectories(dir);
      return dir;
    } catch (final IOException e) {
      throw new RuntimeException("Failed to create cache directory: " + dir, e);
    }
  }

  private void loadExistingEntries() throws IOException {
    if (!Files.exists(cacheDirectory)) {
      return;
    }

    try (final Stream<Path> files = Files.list(cacheDirectory)) {
      files
          .filter(path -> path.toString().endsWith(CACHE_FILE_EXTENSION))
          .forEach(this::loadCacheEntry);
    }
  }

  private void loadCacheEntry(final Path cacheFilePath) {
    try {
      final String fileName = cacheFilePath.getFileName().toString();
      final String keyStr =
          fileName.substring(0, fileName.length() - CACHE_FILE_EXTENSION.length());
      final Path metadataFilePath = cacheDirectory.resolve(keyStr + METADATA_FILE_EXTENSION);

      if (Files.exists(metadataFilePath)) {
        final FileTime lastModified = Files.getLastModifiedTime(cacheFilePath);
        // For now, create a dummy key - in production we'd deserialize the actual key
        final ModuleCacheKey key = createDummyKey(keyStr);
        final CacheEntry entry =
            new CacheEntry(cacheFilePath, metadataFilePath, lastModified.toInstant(), key);
        indexMap.put(keyStr, entry);
      }
    } catch (final IOException e) {
      LOGGER.log(Level.WARNING, "Failed to load cache entry: " + cacheFilePath, e);
    }
  }

  private ModuleCacheKey createDummyKey(final String keyStr) {
    // This is a simplified implementation - in production we'd properly deserialize the key
    return ModuleCacheKey.create(
        keyStr.getBytes(),
        null, // Would be loaded from metadata
        null, // Would be loaded from metadata
        "unknown",
        "unknown");
  }

  private SerializedModule loadModuleFromFile(final Path cacheFilePath, final Path metadataFilePath)
      throws IOException {
    final byte[] data = Files.readAllBytes(cacheFilePath);
    // In production, we'd deserialize the actual metadata
    final ai.tegmentum.wasmtime4j.serialization.impl.ModuleMetadataImpl metadata =
        ai.tegmentum.wasmtime4j.serialization.impl.ModuleMetadataImpl.builder().build();
    return new SerializedModuleImpl(data, metadata);
  }

  private String createMetadataJson(final SerializedModule module, final ModuleCacheKey key) {
    // Simplified JSON creation - in production we'd use a proper JSON library
    return String.format(
        "{\"key\":\"%s\",\"size\":%d,\"checksum\":\"%s\",\"timestamp\":\"%s\"}",
        key.toStringRepresentation(), module.getSize(), module.getChecksum(), Instant.now());
  }

  private boolean isExpired(final CacheEntry entry) {
    if (configuration.getExpirationDuration() == null) {
      return false;
    }

    final Instant expirationTime = entry.creationTime.plus(configuration.getExpirationDuration());
    return Instant.now().isAfter(expirationTime);
  }

  private void removeEntryUnsafe(final String keyStr, final CacheEntry entry) {
    try {
      Files.deleteIfExists(entry.cacheFilePath);
      Files.deleteIfExists(entry.metadataFilePath);
      indexMap.remove(keyStr);
      statistics.recordRemoval();
    } catch (final IOException e) {
      LOGGER.log(Level.WARNING, "Failed to remove cache entry: " + entry.cacheFilePath, e);
    }
  }

  private long removeExpiredEntries() {
    if (configuration.getExpirationDuration() == null) {
      return 0;
    }

    long count = 0;
    final var iterator = indexMap.entrySet().iterator();
    while (iterator.hasNext()) {
      final var entry = iterator.next();
      if (isExpired(entry.getValue())) {
        try {
          Files.deleteIfExists(entry.getValue().cacheFilePath);
          Files.deleteIfExists(entry.getValue().metadataFilePath);
          iterator.remove();
          statistics.recordRemoval();
          count++;
        } catch (final IOException e) {
          LOGGER.log(Level.WARNING, "Failed to remove expired cache entry", e);
        }
      }
    }
    return count;
  }

  private long enforceSizeLimits() {
    if (configuration.getMaxSize() <= 0) {
      return 0;
    }

    final long currentSize = indexMap.size();
    if (currentSize <= configuration.getMaxSize()) {
      return 0;
    }

    final long toEvict = currentSize - configuration.getMaxSize();
    return evictEntries(toEvict);
  }

  private long evictEntries(final long count) {
    // Use LRU eviction policy
    final var entries =
        indexMap.entrySet().stream()
            .sorted((a, b) -> a.getValue().lastAccessTime.compareTo(b.getValue().lastAccessTime))
            .limit(count)
            .collect(Collectors.toList());

    long evicted = 0;
    for (final var entry : entries) {
      removeEntryUnsafe(entry.getKey(), entry.getValue());
      statistics.recordEviction();
      evicted++;
    }

    return evicted;
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Cache has been closed");
    }
  }

  /** Represents a cache entry with file paths and timing information. */
  private static final class CacheEntry {
    final Path cacheFilePath;
    final Path metadataFilePath;
    final Instant creationTime;
    volatile Instant lastAccessTime;
    final ModuleCacheKey originalKey;

    CacheEntry(
        final Path cacheFilePath,
        final Path metadataFilePath,
        final Instant creationTime,
        final ModuleCacheKey originalKey) {
      this.cacheFilePath = cacheFilePath;
      this.metadataFilePath = metadataFilePath;
      this.creationTime = creationTime;
      this.lastAccessTime = creationTime;
      this.originalKey = originalKey;
    }
  }
}
