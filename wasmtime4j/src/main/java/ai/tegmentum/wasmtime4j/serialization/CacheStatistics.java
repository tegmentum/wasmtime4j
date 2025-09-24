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

/**
 * Comprehensive statistics for the module serialization cache.
 *
 * <p>This class provides detailed metrics about cache performance including hit ratios,
 * storage utilization, and performance indicators for optimization and monitoring.
 *
 * @since 1.0.0
 */
public final class CacheStatistics {

  private final long memoryHits;
  private final long diskHits;
  private final long distributedHits;
  private final long misses;
  private final long evictions;
  private final double hitRatio;
  private final long memoryCacheEntries;
  private final long diskCacheSizeBytes;
  private final long memoryCacheSizeBytes;

  /**
   * Creates comprehensive cache statistics.
   *
   * @param memoryHits number of memory cache hits
   * @param diskHits number of disk cache hits
   * @param distributedHits number of distributed cache hits
   * @param misses number of cache misses
   * @param evictions number of cache evictions
   * @param hitRatio overall cache hit ratio (0.0 to 1.0)
   * @param memoryCacheEntries number of entries in memory cache
   * @param diskCacheSizeBytes size of disk cache in bytes
   * @param memoryCacheSizeBytes size of memory cache in bytes
   */
  public CacheStatistics(final long memoryHits, final long diskHits, final long distributedHits,
                        final long misses, final long evictions, final double hitRatio,
                        final long memoryCacheEntries, final long diskCacheSizeBytes,
                        final long memoryCacheSizeBytes) {
    this.memoryHits = memoryHits;
    this.diskHits = diskHits;
    this.distributedHits = distributedHits;
    this.misses = misses;
    this.evictions = evictions;
    this.hitRatio = hitRatio;
    this.memoryCacheEntries = memoryCacheEntries;
    this.diskCacheSizeBytes = diskCacheSizeBytes;
    this.memoryCacheSizeBytes = memoryCacheSizeBytes;
  }

  public long getMemoryHits() {
    return memoryHits;
  }

  public long getDiskHits() {
    return diskHits;
  }

  public long getDistributedHits() {
    return distributedHits;
  }

  public long getTotalHits() {
    return memoryHits + diskHits + distributedHits;
  }

  public long getMisses() {
    return misses;
  }

  public long getTotalRequests() {
    return getTotalHits() + misses;
  }

  public long getEvictions() {
    return evictions;
  }

  public double getHitRatio() {
    return hitRatio;
  }

  public double getMemoryHitRatio() {
    final long totalRequests = getTotalRequests();
    return totalRequests > 0 ? (double) memoryHits / totalRequests : 0.0;
  }

  public double getDiskHitRatio() {
    final long totalRequests = getTotalRequests();
    return totalRequests > 0 ? (double) diskHits / totalRequests : 0.0;
  }

  public double getDistributedHitRatio() {
    final long totalRequests = getTotalRequests();
    return totalRequests > 0 ? (double) distributedHits / totalRequests : 0.0;
  }

  public long getMemoryCacheEntries() {
    return memoryCacheEntries;
  }

  public long getDiskCacheSizeBytes() {
    return diskCacheSizeBytes;
  }

  public long getMemoryCacheSizeBytes() {
    return memoryCacheSizeBytes;
  }

  public double getDiskCacheSizeMB() {
    return diskCacheSizeBytes / (1024.0 * 1024.0);
  }

  public double getMemoryCacheSizeMB() {
    return memoryCacheSizeBytes / (1024.0 * 1024.0);
  }

  public long getTotalCacheSizeBytes() {
    return diskCacheSizeBytes + memoryCacheSizeBytes;
  }

  public double getTotalCacheSizeMB() {
    return getTotalCacheSizeBytes() / (1024.0 * 1024.0);
  }

  @Override
  public String toString() {
    return String.format(
        "CacheStatistics{hitRatio=%.2f%%, hits=%d (memory=%d, disk=%d, distributed=%d), " +
        "misses=%d, evictions=%d, entries=%d, size=%.1fMB (memory=%.1fMB, disk=%.1fMB)}",
        hitRatio * 100,
        getTotalHits(),
        memoryHits,
        diskHits,
        distributedHits,
        misses,
        evictions,
        memoryCacheEntries,
        getTotalCacheSizeMB(),
        getMemoryCacheSizeMB(),
        getDiskCacheSizeMB()
    );
  }
}