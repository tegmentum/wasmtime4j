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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for the module serialization cache system.
 *
 * <p>This class defines the configuration parameters for multi-level caching including memory,
 * disk, and distributed cache settings with appropriate defaults for production use.
 *
 * @since 1.0.0
 */
public final class CacheConfiguration {

  // Memory cache configuration
  private final int maxMemoryEntries;
  private final Duration memoryCacheTtl;
  private final long maxMemoryUsageBytes;

  // Disk cache configuration
  private final Path diskCacheDirectory;
  private final Duration diskCacheTtl;
  private final long maxDiskUsageBytes;
  private final boolean diskCacheEnabled;

  // Distributed cache configuration
  private final ModuleSerializationCache.DistributedCacheConnector distributedCacheConnector;
  private final Duration distributedCacheTtl;
  private final boolean distributedCacheEnabled;

  // Performance configuration
  private final boolean preloadCacheOnStartup;
  private final Duration maintenanceInterval;
  private final int maintenanceThreads;

  // Security configuration
  private final boolean encryptCacheEntries;
  private final String encryptionAlgorithm;

  /**
   * Creates cache configuration with the specified builder.
   *
   * @param builder the configuration builder
   */
  private CacheConfiguration(final Builder builder) {
    this.maxMemoryEntries = builder.maxMemoryEntries;
    this.memoryCacheTtl = builder.memoryCacheTtl;
    this.maxMemoryUsageBytes = builder.maxMemoryUsageBytes;
    this.diskCacheDirectory = builder.diskCacheDirectory;
    this.diskCacheTtl = builder.diskCacheTtl;
    this.maxDiskUsageBytes = builder.maxDiskUsageBytes;
    this.diskCacheEnabled = builder.diskCacheEnabled;
    this.distributedCacheConnector = builder.distributedCacheConnector;
    this.distributedCacheTtl = builder.distributedCacheTtl;
    this.distributedCacheEnabled = builder.distributedCacheEnabled;
    this.preloadCacheOnStartup = builder.preloadCacheOnStartup;
    this.maintenanceInterval = builder.maintenanceInterval;
    this.maintenanceThreads = builder.maintenanceThreads;
    this.encryptCacheEntries = builder.encryptCacheEntries;
    this.encryptionAlgorithm = builder.encryptionAlgorithm;
  }

  // Getter methods

  public int getMaxMemoryEntries() {
    return maxMemoryEntries;
  }

  public Duration getMemoryCacheTtl() {
    return memoryCacheTtl;
  }

  public long getMaxMemoryUsageBytes() {
    return maxMemoryUsageBytes;
  }

  public Path getDiskCacheDirectory() {
    return diskCacheDirectory;
  }

  public Duration getDiskCacheTtl() {
    return diskCacheTtl;
  }

  public long getMaxDiskUsageBytes() {
    return maxDiskUsageBytes;
  }

  public boolean isDiskCacheEnabled() {
    return diskCacheEnabled;
  }

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification = "Connector is intentionally shared as the configured cache connector instance")
  public ModuleSerializationCache.DistributedCacheConnector getDistributedCacheConnector() {
    return distributedCacheConnector;
  }

  public Duration getDistributedCacheTtl() {
    return distributedCacheTtl;
  }

  public boolean isDistributedCacheEnabled() {
    return distributedCacheEnabled;
  }

  public boolean isPreloadCacheOnStartup() {
    return preloadCacheOnStartup;
  }

  public Duration getMaintenanceInterval() {
    return maintenanceInterval;
  }

  public int getMaintenanceThreads() {
    return maintenanceThreads;
  }

  public boolean isEncryptCacheEntries() {
    return encryptCacheEntries;
  }

  public String getEncryptionAlgorithm() {
    return encryptionAlgorithm;
  }

  /**
   * Creates a default configuration suitable for development.
   *
   * @return default cache configuration
   */
  public static CacheConfiguration createDefault() {
    return new Builder().build();
  }

  /**
   * Creates a production-optimized configuration.
   *
   * @return production cache configuration
   */
  public static CacheConfiguration createProduction() {
    return new Builder()
        .setMaxMemoryEntries(10_000)
        .setMaxMemoryUsage(256 * 1024 * 1024) // 256 MB
        .setMemoryCacheTtl(Duration.ofHours(2))
        .enableDiskCache(Paths.get(System.getProperty("java.io.tmpdir"), "wasmtime4j-cache"))
        .setDiskCacheTtl(Duration.ofDays(7))
        .setMaxDiskUsage(2L * 1024 * 1024 * 1024) // 2 GB
        .setMaintenanceInterval(Duration.ofMinutes(10))
        .build();
  }

  /**
   * Creates a memory-only configuration for resource-constrained environments.
   *
   * @return memory-only cache configuration
   */
  public static CacheConfiguration createMemoryOnly() {
    return new Builder()
        .setMaxMemoryEntries(1000)
        .setMaxMemoryUsage(64 * 1024 * 1024) // 64 MB
        .setMemoryCacheTtl(Duration.ofMinutes(30))
        .disableDiskCache()
        .disableDistributedCache()
        .build();
  }

  /**
   * Creates a high-performance configuration for enterprise use.
   *
   * @return high-performance cache configuration
   */
  public static CacheConfiguration createHighPerformance() {
    return new Builder()
        .setMaxMemoryEntries(50_000)
        .setMaxMemoryUsage(1024 * 1024 * 1024) // 1 GB
        .setMemoryCacheTtl(Duration.ofHours(6))
        .enableDiskCache(Paths.get(System.getProperty("java.io.tmpdir"), "wasmtime4j-cache-hp"))
        .setDiskCacheTtl(Duration.ofDays(30))
        .setMaxDiskUsage(10L * 1024 * 1024 * 1024) // 10 GB
        .setMaintenanceInterval(Duration.ofMinutes(5))
        .setMaintenanceThreads(2)
        .enableCachePreloading()
        .build();
  }

  @Override
  public String toString() {
    return String.format(
        "CacheConfiguration{memory=%d entries (%.1f MB, %s TTL), "
            + "disk=%s (%.1f GB, %s TTL), distributed=%s, maintenance=%s}",
        maxMemoryEntries,
        maxMemoryUsageBytes / (1024.0 * 1024.0),
        memoryCacheTtl,
        diskCacheEnabled ? diskCacheDirectory : "disabled",
        diskCacheEnabled ? maxDiskUsageBytes / (1024.0 * 1024.0 * 1024.0) : 0.0,
        diskCacheEnabled ? diskCacheTtl : "N/A",
        distributedCacheEnabled ? "enabled" : "disabled",
        maintenanceInterval);
  }

  /** Builder for creating CacheConfiguration instances. */
  public static final class Builder {
    // Default values
    private int maxMemoryEntries = 1000;
    private Duration memoryCacheTtl = Duration.ofHours(1);
    private long maxMemoryUsageBytes = 128 * 1024 * 1024; // 128 MB

    private Path diskCacheDirectory = null;
    private Duration diskCacheTtl = null;
    private long maxDiskUsageBytes = 1024 * 1024 * 1024; // 1 GB
    private boolean diskCacheEnabled = false;

    private ModuleSerializationCache.DistributedCacheConnector distributedCacheConnector = null;
    private Duration distributedCacheTtl = Duration.ofDays(1);
    private boolean distributedCacheEnabled = false;

    private boolean preloadCacheOnStartup = false;
    private Duration maintenanceInterval = Duration.ofMinutes(15);
    private int maintenanceThreads = 1;

    private boolean encryptCacheEntries = false;
    private String encryptionAlgorithm = "AES/GCM/NoPadding";

    public Builder setMaxMemoryEntries(final int maxEntries) {
      this.maxMemoryEntries = requirePositive(maxEntries, "Max memory entries must be positive");
      return this;
    }

    public Builder setMaxMemoryUsage(final long bytes) {
      this.maxMemoryUsageBytes = requirePositive(bytes, "Max memory usage must be positive");
      return this;
    }

    public Builder setMemoryCacheTtl(final Duration ttl) {
      this.memoryCacheTtl = Objects.requireNonNull(ttl, "Memory cache TTL cannot be null");
      return this;
    }

    /**
     * Enables disk cache with specified directory.
     *
     * @param directory directory for disk cache
     * @return this builder
     */
    public Builder enableDiskCache(final Path directory) {
      this.diskCacheDirectory =
          Objects.requireNonNull(directory, "Disk cache directory cannot be null");
      this.diskCacheEnabled = true;
      return this;
    }

    /**
     * Disables disk cache.
     *
     * @return this builder
     */
    public Builder disableDiskCache() {
      this.diskCacheDirectory = null;
      this.diskCacheEnabled = false;
      return this;
    }

    public Builder setDiskCacheTtl(final Duration ttl) {
      this.diskCacheTtl = ttl;
      return this;
    }

    public Builder setMaxDiskUsage(final long bytes) {
      this.maxDiskUsageBytes = requirePositive(bytes, "Max disk usage must be positive");
      return this;
    }

    /**
     * Enables distributed cache with connector.
     *
     * @param connector distributed cache connector
     * @return this builder
     */
    public Builder enableDistributedCache(
        final ModuleSerializationCache.DistributedCacheConnector connector) {
      this.distributedCacheConnector =
          Objects.requireNonNull(connector, "Distributed cache connector cannot be null");
      this.distributedCacheEnabled = true;
      return this;
    }

    /**
     * Disables distributed cache.
     *
     * @return this builder
     */
    public Builder disableDistributedCache() {
      this.distributedCacheConnector = null;
      this.distributedCacheEnabled = false;
      return this;
    }

    /**
     * Sets distributed cache time-to-live.
     *
     * @param ttl time-to-live duration
     * @return this builder
     */
    public Builder setDistributedCacheTtl(final Duration ttl) {
      this.distributedCacheTtl =
          Objects.requireNonNull(ttl, "Distributed cache TTL cannot be null");
      return this;
    }

    public Builder enableCachePreloading() {
      this.preloadCacheOnStartup = true;
      return this;
    }

    public Builder disableCachePreloading() {
      this.preloadCacheOnStartup = false;
      return this;
    }

    /**
     * Sets cache maintenance interval.
     *
     * @param interval maintenance interval duration
     * @return this builder
     */
    public Builder setMaintenanceInterval(final Duration interval) {
      this.maintenanceInterval =
          Objects.requireNonNull(interval, "Maintenance interval cannot be null");
      return this;
    }

    public Builder setMaintenanceThreads(final int threads) {
      this.maintenanceThreads = requirePositive(threads, "Maintenance threads must be positive");
      return this;
    }

    /**
     * Enables cache entry encryption.
     *
     * @param algorithm encryption algorithm to use
     * @return this builder
     */
    public Builder enableEncryption(final String algorithm) {
      this.encryptCacheEntries = true;
      this.encryptionAlgorithm =
          Objects.requireNonNull(algorithm, "Encryption algorithm cannot be null");
      return this;
    }

    public Builder disableEncryption() {
      this.encryptCacheEntries = false;
      return this;
    }

    public CacheConfiguration build() {
      return new CacheConfiguration(this);
    }

    private static int requirePositive(final int value, final String message) {
      if (value <= 0) {
        throw new IllegalArgumentException(message + " (was: " + value + ")");
      }
      return value;
    }

    private static long requirePositive(final long value, final String message) {
      if (value <= 0) {
        throw new IllegalArgumentException(message + " (was: " + value + ")");
      }
      return value;
    }
  }
}
