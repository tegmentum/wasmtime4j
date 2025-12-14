/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.cache;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Configuration for the WebAssembly module cache.
 *
 * <p>This class provides settings for configuring the module cache behavior including cache
 * directory, size limits, compression settings, and eviction policies.
 *
 * @since 1.0.0
 */
public final class ModuleCacheConfig {

  /** Default maximum cache size in bytes (512 MB). */
  public static final long DEFAULT_MAX_CACHE_SIZE = 512L * 1024L * 1024L;

  /** Default maximum number of entries in the cache. */
  public static final int DEFAULT_MAX_ENTRIES = 1000;

  /** Default compression level (6 - balanced). */
  public static final int DEFAULT_COMPRESSION_LEVEL = 6;

  /** Default cache directory name. */
  public static final String DEFAULT_CACHE_DIR = "wasmtime4j_cache";

  private final Path cacheDir;
  private final long maxCacheSize;
  private final int maxEntries;
  private final boolean compressionEnabled;
  private final int compressionLevel;

  private ModuleCacheConfig(final Builder builder) {
    this.cacheDir = builder.cacheDir;
    this.maxCacheSize = builder.maxCacheSize;
    this.maxEntries = builder.maxEntries;
    this.compressionEnabled = builder.compressionEnabled;
    this.compressionLevel = builder.compressionLevel;
  }

  /**
   * Returns a new builder for creating ModuleCacheConfig instances.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns a default configuration.
   *
   * @return default ModuleCacheConfig
   */
  public static ModuleCacheConfig defaultConfig() {
    return builder().build();
  }

  /**
   * Gets the cache directory path.
   *
   * @return the cache directory
   */
  public Path getCacheDir() {
    return cacheDir;
  }

  /**
   * Gets the maximum cache size in bytes.
   *
   * @return maximum cache size
   */
  public long getMaxCacheSize() {
    return maxCacheSize;
  }

  /**
   * Gets the maximum number of entries allowed in the cache.
   *
   * @return maximum entry count
   */
  public int getMaxEntries() {
    return maxEntries;
  }

  /**
   * Returns whether compression is enabled for cached modules.
   *
   * @return true if compression is enabled
   */
  public boolean isCompressionEnabled() {
    return compressionEnabled;
  }

  /**
   * Gets the compression level (1-22 for zstd).
   *
   * @return compression level
   */
  public int getCompressionLevel() {
    return compressionLevel;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ModuleCacheConfig)) {
      return false;
    }
    final ModuleCacheConfig other = (ModuleCacheConfig) obj;
    return maxCacheSize == other.maxCacheSize
        && maxEntries == other.maxEntries
        && compressionEnabled == other.compressionEnabled
        && compressionLevel == other.compressionLevel
        && Objects.equals(cacheDir, other.cacheDir);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cacheDir, maxCacheSize, maxEntries, compressionEnabled, compressionLevel);
  }

  @Override
  public String toString() {
    return "ModuleCacheConfig{"
        + "cacheDir="
        + cacheDir
        + ", maxCacheSize="
        + maxCacheSize
        + ", maxEntries="
        + maxEntries
        + ", compressionEnabled="
        + compressionEnabled
        + ", compressionLevel="
        + compressionLevel
        + '}';
  }

  /** Builder for ModuleCacheConfig instances. */
  public static final class Builder {
    private Path cacheDir = Paths.get(DEFAULT_CACHE_DIR);
    private long maxCacheSize = DEFAULT_MAX_CACHE_SIZE;
    private int maxEntries = DEFAULT_MAX_ENTRIES;
    private boolean compressionEnabled = true;
    private int compressionLevel = DEFAULT_COMPRESSION_LEVEL;

    private Builder() {}

    /**
     * Sets the cache directory path.
     *
     * @param cacheDir the directory for storing cached modules
     * @return this builder
     */
    public Builder cacheDir(final Path cacheDir) {
      this.cacheDir = Objects.requireNonNull(cacheDir, "cacheDir cannot be null");
      return this;
    }

    /**
     * Sets the cache directory path from a string.
     *
     * @param cacheDir the directory path string
     * @return this builder
     */
    @SuppressFBWarnings(
        value = "PATH_TRAVERSAL_IN",
        justification =
            "This is a configuration API where users intentionally specify the cache directory "
                + "location. Path traversal is a feature, not a vulnerability - users should be "
                + "able to configure any valid path for their module cache. The path is not from "
                + "untrusted external input but from application configuration.")
    public Builder cacheDir(final String cacheDir) {
      return cacheDir(Paths.get(Objects.requireNonNull(cacheDir, "cacheDir cannot be null")));
    }

    /**
     * Sets the maximum cache size in bytes.
     *
     * @param maxCacheSize maximum size in bytes
     * @return this builder
     * @throws IllegalArgumentException if maxCacheSize is negative
     */
    public Builder maxCacheSize(final long maxCacheSize) {
      if (maxCacheSize < 0) {
        throw new IllegalArgumentException("maxCacheSize cannot be negative");
      }
      this.maxCacheSize = maxCacheSize;
      return this;
    }

    /**
     * Sets the maximum number of entries in the cache.
     *
     * @param maxEntries maximum entry count
     * @return this builder
     * @throws IllegalArgumentException if maxEntries is not positive
     */
    public Builder maxEntries(final int maxEntries) {
      if (maxEntries <= 0) {
        throw new IllegalArgumentException("maxEntries must be positive");
      }
      this.maxEntries = maxEntries;
      return this;
    }

    /**
     * Enables or disables compression for cached modules.
     *
     * @param enabled true to enable compression
     * @return this builder
     */
    public Builder compressionEnabled(final boolean enabled) {
      this.compressionEnabled = enabled;
      return this;
    }

    /**
     * Sets the compression level (1-22 for zstd, higher = better compression but slower).
     *
     * @param level compression level
     * @return this builder
     * @throws IllegalArgumentException if level is outside valid range
     */
    public Builder compressionLevel(final int level) {
      if (level < 1 || level > 22) {
        throw new IllegalArgumentException("compressionLevel must be between 1 and 22");
      }
      this.compressionLevel = level;
      return this;
    }

    /**
     * Builds the ModuleCacheConfig instance.
     *
     * @return new ModuleCacheConfig
     */
    public ModuleCacheConfig build() {
      return new ModuleCacheConfig(this);
    }
  }
}
