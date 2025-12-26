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

package ai.tegmentum.wasmtime4j.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CacheStatistics} class.
 *
 * <p>CacheStatistics provides comprehensive metrics about cache performance including hit ratios,
 * storage utilization, and performance indicators.
 */
@DisplayName("CacheStatistics Tests")
class CacheStatisticsTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create statistics with all parameters")
    void shouldCreateStatisticsWithAllParameters() {
      final CacheStatistics stats =
          new CacheStatistics(
              100L, // memoryHits
              50L, // diskHits
              25L, // distributedHits
              20L, // misses
              10L, // evictions
              0.875, // hitRatio
              500L, // memoryCacheEntries
              1024 * 1024 * 100L, // diskCacheSizeBytes (100 MB)
              1024 * 1024 * 50L // memoryCacheSizeBytes (50 MB)
              );

      assertNotNull(stats);
      assertEquals(100L, stats.getMemoryHits());
      assertEquals(50L, stats.getDiskHits());
      assertEquals(25L, stats.getDistributedHits());
      assertEquals(20L, stats.getMisses());
      assertEquals(10L, stats.getEvictions());
      assertEquals(0.875, stats.getHitRatio(), 0.001);
      assertEquals(500L, stats.getMemoryCacheEntries());
    }

    @Test
    @DisplayName("should create statistics with zero values")
    void shouldCreateStatisticsWithZeroValues() {
      final CacheStatistics stats = new CacheStatistics(0L, 0L, 0L, 0L, 0L, 0.0, 0L, 0L, 0L);

      assertNotNull(stats);
      assertEquals(0L, stats.getMemoryHits());
      assertEquals(0L, stats.getDiskHits());
      assertEquals(0L, stats.getDistributedHits());
      assertEquals(0L, stats.getMisses());
      assertEquals(0L, stats.getEvictions());
      assertEquals(0.0, stats.getHitRatio(), 0.001);
    }
  }

  @Nested
  @DisplayName("Total Hits Tests")
  class TotalHitsTests {

    @Test
    @DisplayName("getTotalHits should return sum of all hit types")
    void getTotalHitsShouldReturnSumOfAllHitTypes() {
      final CacheStatistics stats =
          new CacheStatistics(100L, 50L, 25L, 20L, 10L, 0.875, 500L, 0L, 0L);

      assertEquals(175L, stats.getTotalHits());
    }

    @Test
    @DisplayName("getTotalHits should return zero when no hits")
    void getTotalHitsShouldReturnZeroWhenNoHits() {
      final CacheStatistics stats = new CacheStatistics(0L, 0L, 0L, 100L, 0L, 0.0, 0L, 0L, 0L);

      assertEquals(0L, stats.getTotalHits());
    }
  }

  @Nested
  @DisplayName("Total Requests Tests")
  class TotalRequestsTests {

    @Test
    @DisplayName("getTotalRequests should return sum of hits and misses")
    void getTotalRequestsShouldReturnSumOfHitsAndMisses() {
      final CacheStatistics stats =
          new CacheStatistics(100L, 50L, 25L, 20L, 10L, 0.875, 500L, 0L, 0L);

      // Total hits (100+50+25=175) + misses (20) = 195
      assertEquals(195L, stats.getTotalRequests());
    }

    @Test
    @DisplayName("getTotalRequests should return only misses when no hits")
    void getTotalRequestsShouldReturnOnlyMissesWhenNoHits() {
      final CacheStatistics stats = new CacheStatistics(0L, 0L, 0L, 100L, 0L, 0.0, 0L, 0L, 0L);

      assertEquals(100L, stats.getTotalRequests());
    }
  }

  @Nested
  @DisplayName("Hit Ratio Tests")
  class HitRatioTests {

    @Test
    @DisplayName("getMemoryHitRatio should calculate correctly")
    void getMemoryHitRatioShouldCalculateCorrectly() {
      final CacheStatistics stats =
          new CacheStatistics(100L, 50L, 25L, 20L, 10L, 0.875, 500L, 0L, 0L);

      // Memory hits (100) / total requests (195) = ~0.513
      final double expectedRatio = 100.0 / 195.0;
      assertEquals(expectedRatio, stats.getMemoryHitRatio(), 0.001);
    }

    @Test
    @DisplayName("getDiskHitRatio should calculate correctly")
    void getDiskHitRatioShouldCalculateCorrectly() {
      final CacheStatistics stats =
          new CacheStatistics(100L, 50L, 25L, 20L, 10L, 0.875, 500L, 0L, 0L);

      // Disk hits (50) / total requests (195) = ~0.256
      final double expectedRatio = 50.0 / 195.0;
      assertEquals(expectedRatio, stats.getDiskHitRatio(), 0.001);
    }

    @Test
    @DisplayName("getDistributedHitRatio should calculate correctly")
    void getDistributedHitRatioShouldCalculateCorrectly() {
      final CacheStatistics stats =
          new CacheStatistics(100L, 50L, 25L, 20L, 10L, 0.875, 500L, 0L, 0L);

      // Distributed hits (25) / total requests (195) = ~0.128
      final double expectedRatio = 25.0 / 195.0;
      assertEquals(expectedRatio, stats.getDistributedHitRatio(), 0.001);
    }

    @Test
    @DisplayName("hit ratios should return zero when no requests")
    void hitRatiosShouldReturnZeroWhenNoRequests() {
      final CacheStatistics stats = new CacheStatistics(0L, 0L, 0L, 0L, 0L, 0.0, 0L, 0L, 0L);

      assertEquals(0.0, stats.getMemoryHitRatio(), 0.001);
      assertEquals(0.0, stats.getDiskHitRatio(), 0.001);
      assertEquals(0.0, stats.getDistributedHitRatio(), 0.001);
    }
  }

  @Nested
  @DisplayName("Cache Size Tests")
  class CacheSizeTests {

    @Test
    @DisplayName("getDiskCacheSizeBytes should return configured value")
    void getDiskCacheSizeBytesShouldReturnConfiguredValue() {
      final long diskSize = 1024L * 1024L * 100L; // 100 MB
      final CacheStatistics stats = new CacheStatistics(0L, 0L, 0L, 0L, 0L, 0.0, 0L, diskSize, 0L);

      assertEquals(diskSize, stats.getDiskCacheSizeBytes());
    }

    @Test
    @DisplayName("getMemoryCacheSizeBytes should return configured value")
    void getMemoryCacheSizeBytesShouldReturnConfiguredValue() {
      final long memorySize = 1024L * 1024L * 50L; // 50 MB
      final CacheStatistics stats =
          new CacheStatistics(0L, 0L, 0L, 0L, 0L, 0.0, 0L, 0L, memorySize);

      assertEquals(memorySize, stats.getMemoryCacheSizeBytes());
    }

    @Test
    @DisplayName("getDiskCacheSizeMB should convert bytes to megabytes")
    void getDiskCacheSizeMbShouldConvertBytesToMegabytes() {
      final long diskSize = 1024L * 1024L * 100L; // 100 MB
      final CacheStatistics stats = new CacheStatistics(0L, 0L, 0L, 0L, 0L, 0.0, 0L, diskSize, 0L);

      assertEquals(100.0, stats.getDiskCacheSizeMB(), 0.001);
    }

    @Test
    @DisplayName("getMemoryCacheSizeMB should convert bytes to megabytes")
    void getMemoryCacheSizeMbShouldConvertBytesToMegabytes() {
      final long memorySize = 1024L * 1024L * 50L; // 50 MB
      final CacheStatistics stats =
          new CacheStatistics(0L, 0L, 0L, 0L, 0L, 0.0, 0L, 0L, memorySize);

      assertEquals(50.0, stats.getMemoryCacheSizeMB(), 0.001);
    }

    @Test
    @DisplayName("getTotalCacheSizeBytes should return sum of disk and memory")
    void getTotalCacheSizeBytesShouldReturnSumOfDiskAndMemory() {
      final long diskSize = 1024L * 1024L * 100L; // 100 MB
      final long memorySize = 1024L * 1024L * 50L; // 50 MB
      final CacheStatistics stats =
          new CacheStatistics(0L, 0L, 0L, 0L, 0L, 0.0, 0L, diskSize, memorySize);

      assertEquals(diskSize + memorySize, stats.getTotalCacheSizeBytes());
    }

    @Test
    @DisplayName("getTotalCacheSizeMB should return total in megabytes")
    void getTotalCacheSizeMbShouldReturnTotalInMegabytes() {
      final long diskSize = 1024L * 1024L * 100L; // 100 MB
      final long memorySize = 1024L * 1024L * 50L; // 50 MB
      final CacheStatistics stats =
          new CacheStatistics(0L, 0L, 0L, 0L, 0L, 0.0, 0L, diskSize, memorySize);

      assertEquals(150.0, stats.getTotalCacheSizeMB(), 0.001);
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final CacheStatistics stats =
          new CacheStatistics(
              100L, 50L, 25L, 20L, 10L, 0.875, 500L, 1024L * 1024L * 100L, 1024L * 1024L * 50L);

      final String result = stats.toString();

      assertNotNull(result);
      assertTrue(result.contains("CacheStatistics"));
      assertTrue(result.contains("hitRatio=87.50%"));
      assertTrue(result.contains("hits=175"));
      assertTrue(result.contains("memory=100"));
      assertTrue(result.contains("disk=50"));
      assertTrue(result.contains("distributed=25"));
      assertTrue(result.contains("misses=20"));
      assertTrue(result.contains("evictions=10"));
      assertTrue(result.contains("entries=500"));
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("CacheStatistics should be final")
    void cacheStatisticsShouldBeFinal() {
      assertTrue(java.lang.reflect.Modifier.isFinal(CacheStatistics.class.getModifiers()));
    }
  }
}
