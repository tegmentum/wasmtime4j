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

import java.util.Objects;

/**
 * Statistics for the WebAssembly module cache.
 *
 * <p>This immutable class provides cache performance metrics including hit/miss counts, storage
 * usage, and compression statistics.
 *
 * @since 1.0.0
 */
public final class ModuleCacheStatistics {

  private final long cacheHits;
  private final long cacheMisses;
  private final long entriesCount;
  private final long storageBytesUsed;
  private final double compressionRatio;
  private final long evictionCount;

  private ModuleCacheStatistics(final Builder builder) {
    this.cacheHits = builder.cacheHits;
    this.cacheMisses = builder.cacheMisses;
    this.entriesCount = builder.entriesCount;
    this.storageBytesUsed = builder.storageBytesUsed;
    this.compressionRatio = builder.compressionRatio;
    this.evictionCount = builder.evictionCount;
  }

  /**
   * Returns a new builder for creating ModuleCacheStatistics instances.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the number of cache hits.
   *
   * @return cache hit count
   */
  public long getCacheHits() {
    return cacheHits;
  }

  /**
   * Gets the number of cache misses.
   *
   * @return cache miss count
   */
  public long getCacheMisses() {
    return cacheMisses;
  }

  /**
   * Gets the total number of requests (hits + misses).
   *
   * @return total request count
   */
  public long getTotalRequests() {
    return cacheHits + cacheMisses;
  }

  /**
   * Gets the cache hit rate as a ratio between 0.0 and 1.0.
   *
   * @return hit rate, or 0.0 if no requests have been made
   */
  public double getHitRate() {
    final long total = getTotalRequests();
    return total > 0 ? (double) cacheHits / total : 0.0;
  }

  /**
   * Gets the number of entries currently in the cache.
   *
   * @return entry count
   */
  public long getEntriesCount() {
    return entriesCount;
  }

  /**
   * Gets the total bytes used by cached modules.
   *
   * @return storage bytes used
   */
  public long getStorageBytesUsed() {
    return storageBytesUsed;
  }

  /**
   * Gets the average compression ratio achieved.
   *
   * <p>A ratio of 0.5 means the compressed size is half the original size.
   *
   * @return compression ratio
   */
  public double getCompressionRatio() {
    return compressionRatio;
  }

  /**
   * Gets the number of entries that have been evicted from the cache.
   *
   * @return eviction count
   */
  public long getEvictionCount() {
    return evictionCount;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ModuleCacheStatistics)) {
      return false;
    }
    final ModuleCacheStatistics other = (ModuleCacheStatistics) obj;
    return cacheHits == other.cacheHits
        && cacheMisses == other.cacheMisses
        && entriesCount == other.entriesCount
        && storageBytesUsed == other.storageBytesUsed
        && Double.compare(compressionRatio, other.compressionRatio) == 0
        && evictionCount == other.evictionCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        cacheHits, cacheMisses, entriesCount, storageBytesUsed, compressionRatio, evictionCount);
  }

  @Override
  public String toString() {
    return String.format(
        "ModuleCacheStatistics{hits=%d, misses=%d, hitRate=%.2f%%, entries=%d, "
            + "storage=%d bytes, compression=%.2f, evictions=%d}",
        cacheHits,
        cacheMisses,
        getHitRate() * 100,
        entriesCount,
        storageBytesUsed,
        compressionRatio,
        evictionCount);
  }

  /** Builder for ModuleCacheStatistics instances. */
  public static final class Builder {
    private long cacheHits;
    private long cacheMisses;
    private long entriesCount;
    private long storageBytesUsed;
    private double compressionRatio;
    private long evictionCount;

    private Builder() {}

    /**
     * Sets the cache hit count.
     *
     * @param cacheHits hit count
     * @return this builder
     */
    public Builder cacheHits(final long cacheHits) {
      this.cacheHits = cacheHits;
      return this;
    }

    /**
     * Sets the cache miss count.
     *
     * @param cacheMisses miss count
     * @return this builder
     */
    public Builder cacheMisses(final long cacheMisses) {
      this.cacheMisses = cacheMisses;
      return this;
    }

    /**
     * Sets the number of entries in the cache.
     *
     * @param entriesCount entry count
     * @return this builder
     */
    public Builder entriesCount(final long entriesCount) {
      this.entriesCount = entriesCount;
      return this;
    }

    /**
     * Sets the storage bytes used.
     *
     * @param storageBytesUsed bytes used
     * @return this builder
     */
    public Builder storageBytesUsed(final long storageBytesUsed) {
      this.storageBytesUsed = storageBytesUsed;
      return this;
    }

    /**
     * Sets the compression ratio.
     *
     * @param compressionRatio ratio value
     * @return this builder
     */
    public Builder compressionRatio(final double compressionRatio) {
      this.compressionRatio = compressionRatio;
      return this;
    }

    /**
     * Sets the eviction count.
     *
     * @param evictionCount eviction count
     * @return this builder
     */
    public Builder evictionCount(final long evictionCount) {
      this.evictionCount = evictionCount;
      return this;
    }

    /**
     * Builds the ModuleCacheStatistics instance.
     *
     * @return new ModuleCacheStatistics
     */
    public ModuleCacheStatistics build() {
      return new ModuleCacheStatistics(this);
    }
  }
}
