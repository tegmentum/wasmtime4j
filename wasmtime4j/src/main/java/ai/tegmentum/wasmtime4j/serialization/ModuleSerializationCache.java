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

package ai.tegmentum.wasmtime4j.serialization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Content-addressed caching system for serialized WebAssembly modules.
 *
 * <p>This cache provides multi-level storage with memory, disk, and distributed tiers using SHA-256
 * content addressing for integrity and deduplication. Features include automatic eviction,
 * performance monitoring, and cache warming strategies.
 *
 * @since 1.0.0
 */
public final class ModuleSerializationCache implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(ModuleSerializationCache.class.getName());

  // Cache configuration
  private final CacheConfiguration config;

  // Multi-level cache storage
  private final Map<String, CacheEntry> memoryCache;
  private final Path diskCacheDirectory;
  private final DistributedCacheConnector distributedCache;

  // Cache statistics
  private final AtomicLong memoryHits = new AtomicLong(0);
  private final AtomicLong diskHits = new AtomicLong(0);
  private final AtomicLong distributedHits = new AtomicLong(0);
  private final AtomicLong misses = new AtomicLong(0);
  private final AtomicLong evictions = new AtomicLong(0);

  // Background maintenance
  private final ScheduledExecutorService maintenanceExecutor;
  private volatile boolean closed = false;

  /**
   * Creates a new module serialization cache with the specified configuration.
   *
   * @param config the cache configuration
   * @throws IOException if disk cache initialization fails
   */
  public ModuleSerializationCache(final CacheConfiguration config) throws IOException {
    this.config = Objects.requireNonNull(config, "Configuration cannot be null");
    this.memoryCache = new ConcurrentHashMap<>(config.getMaxMemoryEntries());

    // Initialize disk cache directory
    this.diskCacheDirectory = config.getDiskCacheDirectory();
    if (diskCacheDirectory != null) {
      Files.createDirectories(diskCacheDirectory);
      LOGGER.info("Initialized disk cache directory: " + diskCacheDirectory);
    }

    // Initialize distributed cache if configured
    this.distributedCache = config.getDistributedCacheConnector();
    if (distributedCache != null) {
      LOGGER.info("Initialized distributed cache connector");
    }

    // Start background maintenance
    this.maintenanceExecutor =
        Executors.newScheduledThreadPool(
            1,
            r -> {
              final Thread t = new Thread(r, "module-cache-maintenance");
              t.setDaemon(true);
              return t;
            });

    scheduleMaintenanceTasks();
    LOGGER.info("Module serialization cache initialized with configuration: " + config);
  }

  /**
   * Stores a serialized module in the cache using content-addressed storage.
   *
   * @param moduleData the serialized module data
   * @param metadata the module metadata
   * @return the content hash used as the cache key
   * @throws IllegalArgumentException if parameters are null
   * @throws IOException if storage fails
   */
  public String store(final byte[] moduleData, final SerializedModuleMetadata metadata)
      throws IOException {
    Objects.requireNonNull(moduleData, "Module data cannot be null");
    Objects.requireNonNull(metadata, "Metadata cannot be null");
    ensureNotClosed();

    final String contentHash = calculateContentHash(moduleData);
    final CacheEntry entry = new CacheEntry(moduleData, metadata, Instant.now());

    // Store in memory cache
    storeInMemoryCache(contentHash, entry);

    // Store in disk cache if configured
    if (diskCacheDirectory != null) {
      storeInDiskCache(contentHash, entry);
    }

    // Store in distributed cache if configured
    if (distributedCache != null && distributedCache.isAvailable()) {
      try {
        distributedCache.store(contentHash, entry);
        LOGGER.fine("Stored module in distributed cache: " + contentHash);
      } catch (Exception e) {
        LOGGER.warning("Failed to store in distributed cache: " + e.getMessage());
      }
    }

    LOGGER.fine(
        "Stored serialized module with hash: " + contentHash + ", size: " + moduleData.length);
    return contentHash;
  }

  /**
   * Retrieves a cached serialized module by content hash.
   *
   * @param contentHash the content hash key
   * @return the cached entry if found
   * @throws IllegalArgumentException if contentHash is null
   */
  public Optional<CacheEntry> retrieve(final String contentHash) {
    Objects.requireNonNull(contentHash, "Content hash cannot be null");
    ensureNotClosed();

    // Try memory cache first
    final CacheEntry memoryEntry = memoryCache.get(contentHash);
    if (memoryEntry != null && !memoryEntry.isExpired(config.getMemoryCacheTtl())) {
      memoryHits.incrementAndGet();
      memoryEntry.updateAccessTime();
      LOGGER.fine("Memory cache hit for hash: " + contentHash);
      return Optional.of(memoryEntry);
    }

    // Try disk cache
    if (diskCacheDirectory != null) {
      final Optional<CacheEntry> diskEntry = retrieveFromDiskCache(contentHash);
      if (diskEntry.isPresent()) {
        diskHits.incrementAndGet();
        // Promote to memory cache
        storeInMemoryCache(contentHash, diskEntry.get());
        LOGGER.fine("Disk cache hit for hash: " + contentHash);
        return diskEntry;
      }
    }

    // Try distributed cache
    if (distributedCache != null && distributedCache.isAvailable()) {
      try {
        final Optional<CacheEntry> distributedEntry = distributedCache.retrieve(contentHash);
        if (distributedEntry.isPresent()) {
          distributedHits.incrementAndGet();
          final CacheEntry entry = distributedEntry.get();

          // Promote to local caches
          storeInMemoryCache(contentHash, entry);
          if (diskCacheDirectory != null) {
            storeInDiskCache(contentHash, entry);
          }

          LOGGER.fine("Distributed cache hit for hash: " + contentHash);
          return distributedEntry;
        }
      } catch (Exception e) {
        LOGGER.warning("Failed to retrieve from distributed cache: " + e.getMessage());
      }
    }

    misses.incrementAndGet();
    LOGGER.fine("Cache miss for hash: " + contentHash);
    return Optional.empty();
  }

  /**
   * Checks if a module is cached without retrieving it.
   *
   * @param contentHash the content hash key
   * @return true if the module is cached
   */
  public boolean contains(final String contentHash) {
    Objects.requireNonNull(contentHash, "Content hash cannot be null");
    ensureNotClosed();

    // Check memory cache
    if (memoryCache.containsKey(contentHash)) {
      final CacheEntry entry = memoryCache.get(contentHash);
      if (entry != null && !entry.isExpired(config.getMemoryCacheTtl())) {
        return true;
      }
    }

    // Check disk cache
    if (diskCacheDirectory != null) {
      final Path cacheFile = getCacheFilePath(contentHash);
      if (Files.exists(cacheFile)) {
        return true;
      }
    }

    // Check distributed cache
    if (distributedCache != null && distributedCache.isAvailable()) {
      try {
        return distributedCache.contains(contentHash);
      } catch (Exception e) {
        LOGGER.warning("Failed to check distributed cache: " + e.getMessage());
      }
    }

    return false;
  }

  /**
   * Removes a cached module by content hash.
   *
   * @param contentHash the content hash key
   * @return true if the module was cached and removed
   */
  public boolean remove(final String contentHash) {
    Objects.requireNonNull(contentHash, "Content hash cannot be null");
    ensureNotClosed();

    boolean removed = false;

    // Remove from memory cache
    if (memoryCache.remove(contentHash) != null) {
      removed = true;
    }

    // Remove from disk cache
    if (diskCacheDirectory != null) {
      final Path cacheFile = getCacheFilePath(contentHash);
      final Path metadataFile = getCacheMetadataPath(contentHash);

      try {
        removed |= Files.deleteIfExists(cacheFile);
        Files.deleteIfExists(metadataFile);
      } catch (IOException e) {
        LOGGER.warning("Failed to remove disk cache entry: " + e.getMessage());
      }
    }

    // Remove from distributed cache
    if (distributedCache != null && distributedCache.isAvailable()) {
      try {
        removed |= distributedCache.remove(contentHash);
      } catch (Exception e) {
        LOGGER.warning("Failed to remove from distributed cache: " + e.getMessage());
      }
    }

    if (removed) {
      LOGGER.fine("Removed cached module: " + contentHash);
    }

    return removed;
  }

  /** Clears all cached modules. */
  public void clear() {
    ensureNotClosed();

    // Clear memory cache
    memoryCache.clear();

    // Clear disk cache
    if (diskCacheDirectory != null) {
      try {
        Files.walk(diskCacheDirectory)
            .filter(Files::isRegularFile)
            .forEach(
                file -> {
                  try {
                    Files.delete(file);
                  } catch (IOException e) {
                    LOGGER.warning("Failed to delete cache file: " + file + " - " + e.getMessage());
                  }
                });
      } catch (IOException e) {
        LOGGER.warning("Failed to clear disk cache: " + e.getMessage());
      }
    }

    // Clear distributed cache
    if (distributedCache != null && distributedCache.isAvailable()) {
      try {
        distributedCache.clear();
      } catch (Exception e) {
        LOGGER.warning("Failed to clear distributed cache: " + e.getMessage());
      }
    }

    LOGGER.info("Cleared all cache entries");
  }

  /**
   * Gets comprehensive cache statistics.
   *
   * @return the cache statistics
   */
  public CacheStatistics getStatistics() {
    final long totalHits = memoryHits.get() + diskHits.get() + distributedHits.get();
    final long totalRequests = totalHits + misses.get();
    final double hitRatio = totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;

    return new CacheStatistics(
        memoryHits.get(),
        diskHits.get(),
        distributedHits.get(),
        misses.get(),
        evictions.get(),
        hitRatio,
        memoryCache.size(),
        calculateDiskCacheSize(),
        calculateMemoryCacheSize());
  }

  /**
   * Preloads cache with commonly used modules for warming.
   *
   * @param modules map of content hash to module data
   * @param metadata map of content hash to metadata
   */
  public void warmCache(
      final Map<String, byte[]> modules, final Map<String, SerializedModuleMetadata> metadata) {
    Objects.requireNonNull(modules, "Modules cannot be null");
    Objects.requireNonNull(metadata, "Metadata cannot be null");
    ensureNotClosed();

    LOGGER.info("Warming cache with " + modules.size() + " modules");

    modules.entrySet().parallelStream()
        .forEach(
            entry -> {
              final String hash = entry.getKey();
              final byte[] data = entry.getValue();
              final SerializedModuleMetadata meta = metadata.get(hash);

              if (meta != null) {
                try {
                  store(data, meta);
                } catch (IOException e) {
                  LOGGER.warning("Failed to warm cache entry " + hash + ": " + e.getMessage());
                }
              }
            });

    LOGGER.info("Cache warming completed");
  }

  /**
   * Stores an entry in the memory cache with LRU eviction.
   *
   * @param contentHash the content hash
   * @param entry the cache entry
   */
  private void storeInMemoryCache(final String contentHash, final CacheEntry entry) {
    // Check if we need to evict entries
    if (memoryCache.size() >= config.getMaxMemoryEntries()) {
      evictLeastRecentlyUsed();
    }

    memoryCache.put(contentHash, entry);
  }

  /**
   * Stores an entry in the disk cache.
   *
   * @param contentHash the content hash
   * @param entry the cache entry
   * @throws IOException if storage fails
   */
  private void storeInDiskCache(final String contentHash, final CacheEntry entry)
      throws IOException {
    final Path cacheFile = getCacheFilePath(contentHash);
    final Path metadataFile = getCacheMetadataPath(contentHash);

    // Create subdirectories for better file system performance
    final Path parentDir = cacheFile.getParent();
    if (parentDir != null) {
      Files.createDirectories(parentDir);
    }

    // Write module data
    Files.write(cacheFile, entry.getModuleData());

    // Write metadata (simplified serialization for now)
    final String metadataJson = serializeMetadata(entry.getMetadata());
    Files.write(metadataFile, metadataJson.getBytes("UTF-8"));
  }

  /**
   * Retrieves an entry from the disk cache.
   *
   * @param contentHash the content hash
   * @return the cache entry if found
   */
  private Optional<CacheEntry> retrieveFromDiskCache(final String contentHash) {
    final Path cacheFile = getCacheFilePath(contentHash);
    final Path metadataFile = getCacheMetadataPath(contentHash);

    if (!Files.exists(cacheFile) || !Files.exists(metadataFile)) {
      return Optional.empty();
    }

    try {
      // Check TTL
      if (config.getDiskCacheTtl() != null) {
        final Instant lastModified = Files.getLastModifiedTime(cacheFile).toInstant();
        if (Instant.now().isAfter(lastModified.plus(config.getDiskCacheTtl()))) {
          // Expired - delete files
          Files.deleteIfExists(cacheFile);
          Files.deleteIfExists(metadataFile);
          return Optional.empty();
        }
      }

      // Read module data
      final byte[] moduleData = Files.readAllBytes(cacheFile);

      // Read metadata
      final String metadataJson = new String(Files.readAllBytes(metadataFile), "UTF-8");
      final SerializedModuleMetadata metadata = deserializeMetadata(metadataJson);

      return Optional.of(new CacheEntry(moduleData, metadata, Instant.now()));

    } catch (IOException e) {
      LOGGER.warning("Failed to read disk cache entry " + contentHash + ": " + e.getMessage());
      return Optional.empty();
    }
  }

  /** Evicts the least recently used entry from memory cache. */
  private void evictLeastRecentlyUsed() {
    String oldestKey = null;
    Instant oldestAccess = Instant.now();

    for (final Map.Entry<String, CacheEntry> entry : memoryCache.entrySet()) {
      final Instant accessTime = entry.getValue().getLastAccessTime();
      if (accessTime.isBefore(oldestAccess)) {
        oldestAccess = accessTime;
        oldestKey = entry.getKey();
      }
    }

    if (oldestKey != null) {
      memoryCache.remove(oldestKey);
      evictions.incrementAndGet();
      LOGGER.fine("Evicted LRU cache entry: " + oldestKey);
    }
  }

  /**
   * Gets the file path for a cached module.
   *
   * @param contentHash the content hash
   * @return the cache file path
   */
  private Path getCacheFilePath(final String contentHash) {
    // Use subdirectories for better file system performance
    final String subdir = contentHash.substring(0, 2);
    return diskCacheDirectory.resolve(subdir).resolve(contentHash + ".module");
  }

  /**
   * Gets the metadata file path for a cached module.
   *
   * @param contentHash the content hash
   * @return the metadata file path
   */
  private Path getCacheMetadataPath(final String contentHash) {
    final String subdir = contentHash.substring(0, 2);
    return diskCacheDirectory.resolve(subdir).resolve(contentHash + ".meta");
  }

  /**
   * Calculates the SHA-256 content hash of module data.
   *
   * @param data the module data
   * @return the content hash as hex string
   */
  private String calculateContentHash(final byte[] data) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(data);
      return bytesToHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }

  /**
   * Converts byte array to hexadecimal string.
   *
   * @param bytes the byte array
   * @return hexadecimal representation
   */
  private String bytesToHex(final byte[] bytes) {
    final StringBuilder result = new StringBuilder();
    for (final byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  /**
   * Calculates the disk cache size in bytes.
   *
   * @return the disk cache size
   */
  private long calculateDiskCacheSize() {
    if (diskCacheDirectory == null || !Files.exists(diskCacheDirectory)) {
      return 0;
    }

    try {
      return Files.walk(diskCacheDirectory)
          .filter(Files::isRegularFile)
          .mapToLong(
              file -> {
                try {
                  return Files.size(file);
                } catch (IOException e) {
                  return 0;
                }
              })
          .sum();
    } catch (IOException e) {
      LOGGER.warning("Failed to calculate disk cache size: " + e.getMessage());
      return 0;
    }
  }

  /**
   * Calculates the memory cache size in bytes (estimated).
   *
   * @return the estimated memory cache size
   */
  private long calculateMemoryCacheSize() {
    return memoryCache.values().stream()
        .mapToLong(entry -> entry.getModuleData().length + 1024) // Add overhead estimate
        .sum();
  }

  /**
   * Simplified metadata serialization (in production would use proper JSON library).
   *
   * @param metadata the metadata to serialize
   * @return JSON string
   */
  private String serializeMetadata(final SerializedModuleMetadata metadata) {
    // Simplified JSON serialization - in production would use Jackson or similar
    return String.format(
        "{\"format\":\"%s\",\"size\":%d,\"hash\":\"%s\",\"timestamp\":\"%s\"}",
        metadata.getFormat().getIdentifier(),
        metadata.getSerializedSize(),
        metadata.getSha256Hash(),
        metadata.getSerializationTimestamp().toString());
  }

  /**
   * Simplified metadata deserialization.
   *
   * @param json the JSON string
   * @return the deserialized metadata
   */
  private SerializedModuleMetadata deserializeMetadata(final String json) {
    // Simplified JSON deserialization - in production would use Jackson or similar
    // For now, return basic metadata
    return new SerializedModuleMetadata.Builder()
        .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
        .setSerializedSize(0)
        .setOriginalSize(0)
        .setSha256Hash("")
        .build();
  }

  /** Schedules background maintenance tasks. */
  private void scheduleMaintenanceTasks() {
    // Cleanup expired entries
    maintenanceExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 5, 5, TimeUnit.MINUTES);

    // Log statistics periodically
    maintenanceExecutor.scheduleAtFixedRate(this::logStatistics, 1, 1, TimeUnit.HOURS);
  }

  /** Cleans up expired cache entries. */
  private void cleanupExpiredEntries() {
    if (closed) {
      return;
    }

    try {
      // Cleanup memory cache
      final Instant memoryExpiry = Instant.now().minus(config.getMemoryCacheTtl());
      memoryCache
          .entrySet()
          .removeIf(entry -> entry.getValue().getCreationTime().isBefore(memoryExpiry));

      // Cleanup disk cache
      if (diskCacheDirectory != null && config.getDiskCacheTtl() != null) {
        final Instant diskExpiry = Instant.now().minus(config.getDiskCacheTtl());

        Files.walk(diskCacheDirectory)
            .filter(Files::isRegularFile)
            .filter(
                file -> {
                  try {
                    return Files.getLastModifiedTime(file).toInstant().isBefore(diskExpiry);
                  } catch (IOException e) {
                    return false;
                  }
                })
            .forEach(
                file -> {
                  try {
                    Files.delete(file);
                  } catch (IOException e) {
                    LOGGER.fine("Failed to delete expired cache file: " + file);
                  }
                });
      }

    } catch (Exception e) {
      LOGGER.warning("Cache cleanup failed: " + e.getMessage());
    }
  }

  /** Logs cache statistics. */
  private void logStatistics() {
    if (closed) {
      return;
    }

    final CacheStatistics stats = getStatistics();
    LOGGER.info("Cache statistics: " + stats);
  }

  /**
   * Ensures the cache is not closed.
   *
   * @throws IllegalStateException if the cache is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Cache has been closed");
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    synchronized (this) {
      if (closed) {
        return;
      }

      closed = true;

      try {
        maintenanceExecutor.shutdown();
        if (!maintenanceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
          maintenanceExecutor.shutdownNow();
        }
      } catch (InterruptedException e) {
        maintenanceExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }

      if (distributedCache != null) {
        try {
          distributedCache.close();
        } catch (Exception e) {
          LOGGER.warning("Failed to close distributed cache: " + e.getMessage());
        }
      }

      LOGGER.info("Module serialization cache closed");
    }
  }

  /** Cache entry containing module data and metadata. */
  public static final class CacheEntry {
    private final byte[] moduleData;
    private final SerializedModuleMetadata metadata;
    private final Instant creationTime;
    private volatile Instant lastAccessTime;

    /**
     * Creates a new cache entry.
     *
     * @param moduleData the serialized module data
     * @param metadata the module metadata
     * @param creationTime the time when the entry was created
     */
    public CacheEntry(
        final byte[] moduleData,
        final SerializedModuleMetadata metadata,
        final Instant creationTime) {
      this.moduleData = Objects.requireNonNull(moduleData, "Module data cannot be null").clone();
      this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
      this.creationTime = Objects.requireNonNull(creationTime, "Creation time cannot be null");
      this.lastAccessTime = creationTime;
    }

    public byte[] getModuleData() {
      return moduleData.clone();
    }

    public SerializedModuleMetadata getMetadata() {
      return metadata;
    }

    public Instant getCreationTime() {
      return creationTime;
    }

    public Instant getLastAccessTime() {
      return lastAccessTime;
    }

    public void updateAccessTime() {
      lastAccessTime = Instant.now();
    }

    /**
     * Checks if this cache entry has expired based on the given TTL.
     *
     * @param ttl the time-to-live duration, null means never expires
     * @return true if the entry has expired, false otherwise
     */
    public boolean isExpired(final Duration ttl) {
      if (ttl == null) {
        return false;
      }
      return Instant.now().isAfter(creationTime.plus(ttl));
    }
  }

  /** Interface for distributed cache connectors. */
  public interface DistributedCacheConnector extends AutoCloseable {
    boolean isAvailable();

    void store(String key, CacheEntry entry) throws IOException;

    Optional<CacheEntry> retrieve(String key) throws IOException;

    boolean contains(String key) throws IOException;

    boolean remove(String key) throws IOException;

    void clear() throws IOException;

    @Override
    void close() throws IOException;
  }
}
