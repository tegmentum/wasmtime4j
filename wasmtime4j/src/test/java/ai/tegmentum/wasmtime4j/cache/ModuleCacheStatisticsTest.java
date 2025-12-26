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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleCacheStatistics} class.
 *
 * <p>ModuleCacheStatistics provides cache performance metrics.
 */
@DisplayName("ModuleCacheStatistics Tests")
class ModuleCacheStatisticsTest {

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create builder via static method")
    void shouldCreateBuilderViaStaticMethod() {
      final ModuleCacheStatistics.Builder builder = ModuleCacheStatistics.builder();
      assertNotNull(builder, "Builder should not be null");
    }

    @Test
    @DisplayName("should build with default values")
    void shouldBuildWithDefaultValues() {
      final ModuleCacheStatistics stats = ModuleCacheStatistics.builder().build();
      assertNotNull(stats, "Stats should not be null");
      assertEquals(0, stats.getCacheHits(), "Cache hits should default to 0");
      assertEquals(0, stats.getCacheMisses(), "Cache misses should default to 0");
      assertEquals(0, stats.getEntriesCount(), "Entries count should default to 0");
      assertEquals(0, stats.getStorageBytesUsed(), "Storage bytes should default to 0");
      assertEquals(
          0.0, stats.getCompressionRatio(), 0.001, "Compression ratio should default to 0");
      assertEquals(0, stats.getEvictionCount(), "Eviction count should default to 0");
    }

    @Test
    @DisplayName("should build with cache hits")
    void shouldBuildWithCacheHits() {
      final ModuleCacheStatistics stats = ModuleCacheStatistics.builder().cacheHits(100).build();
      assertEquals(100, stats.getCacheHits(), "Cache hits should match");
    }

    @Test
    @DisplayName("should build with cache misses")
    void shouldBuildWithCacheMisses() {
      final ModuleCacheStatistics stats = ModuleCacheStatistics.builder().cacheMisses(50).build();
      assertEquals(50, stats.getCacheMisses(), "Cache misses should match");
    }

    @Test
    @DisplayName("should build with entries count")
    void shouldBuildWithEntriesCount() {
      final ModuleCacheStatistics stats = ModuleCacheStatistics.builder().entriesCount(25).build();
      assertEquals(25, stats.getEntriesCount(), "Entries count should match");
    }

    @Test
    @DisplayName("should build with storage bytes used")
    void shouldBuildWithStorageBytesUsed() {
      final ModuleCacheStatistics stats =
          ModuleCacheStatistics.builder().storageBytesUsed(1024000).build();
      assertEquals(1024000, stats.getStorageBytesUsed(), "Storage bytes should match");
    }

    @Test
    @DisplayName("should build with compression ratio")
    void shouldBuildWithCompressionRatio() {
      final ModuleCacheStatistics stats =
          ModuleCacheStatistics.builder().compressionRatio(0.6).build();
      assertEquals(0.6, stats.getCompressionRatio(), 0.001, "Compression ratio should match");
    }

    @Test
    @DisplayName("should build with eviction count")
    void shouldBuildWithEvictionCount() {
      final ModuleCacheStatistics stats = ModuleCacheStatistics.builder().evictionCount(10).build();
      assertEquals(10, stats.getEvictionCount(), "Eviction count should match");
    }
  }

  @Nested
  @DisplayName("Calculated Statistics Tests")
  class CalculatedStatisticsTests {

    @Test
    @DisplayName("should calculate total requests")
    void shouldCalculateTotalRequests() {
      final ModuleCacheStatistics stats =
          ModuleCacheStatistics.builder().cacheHits(80).cacheMisses(20).build();
      assertEquals(100, stats.getTotalRequests(), "Total requests should be hits + misses");
    }

    @Test
    @DisplayName("should calculate hit rate correctly")
    void shouldCalculateHitRateCorrectly() {
      final ModuleCacheStatistics stats =
          ModuleCacheStatistics.builder().cacheHits(80).cacheMisses(20).build();
      assertEquals(0.8, stats.getHitRate(), 0.001, "Hit rate should be 80%");
    }

    @Test
    @DisplayName("should return 0 hit rate when no requests")
    void shouldReturn0HitRateWhenNoRequests() {
      final ModuleCacheStatistics stats = ModuleCacheStatistics.builder().build();
      assertEquals(0.0, stats.getHitRate(), 0.001, "Hit rate should be 0 when no requests");
    }

    @Test
    @DisplayName("should return 100% hit rate when all hits")
    void shouldReturn100PercentHitRateWhenAllHits() {
      final ModuleCacheStatistics stats =
          ModuleCacheStatistics.builder().cacheHits(100).cacheMisses(0).build();
      assertEquals(1.0, stats.getHitRate(), 0.001, "Hit rate should be 100%");
    }

    @Test
    @DisplayName("should return 0% hit rate when all misses")
    void shouldReturn0PercentHitRateWhenAllMisses() {
      final ModuleCacheStatistics stats =
          ModuleCacheStatistics.builder().cacheHits(0).cacheMisses(100).build();
      assertEquals(0.0, stats.getHitRate(), 0.001, "Hit rate should be 0%");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final ModuleCacheStatistics stats = ModuleCacheStatistics.builder().cacheHits(10).build();
      assertEquals(stats, stats, "Stats should be equal to itself");
    }

    @Test
    @DisplayName("should be equal to stats with same values")
    void shouldBeEqualToStatsWithSameValues() {
      final ModuleCacheStatistics stats1 =
          ModuleCacheStatistics.builder().cacheHits(50).cacheMisses(20).entriesCount(10).build();
      final ModuleCacheStatistics stats2 =
          ModuleCacheStatistics.builder().cacheHits(50).cacheMisses(20).entriesCount(10).build();
      assertEquals(stats1, stats2, "Stats with same values should be equal");
    }

    @Test
    @DisplayName("should not be equal to stats with different values")
    void shouldNotBeEqualToStatsWithDifferentValues() {
      final ModuleCacheStatistics stats1 = ModuleCacheStatistics.builder().cacheHits(50).build();
      final ModuleCacheStatistics stats2 = ModuleCacheStatistics.builder().cacheHits(60).build();
      assertNotEquals(stats1, stats2, "Stats with different values should not be equal");
    }

    @Test
    @DisplayName("should have same hash code for equal stats")
    void shouldHaveSameHashCodeForEqualStats() {
      final ModuleCacheStatistics stats1 =
          ModuleCacheStatistics.builder().cacheHits(50).cacheMisses(20).build();
      final ModuleCacheStatistics stats2 =
          ModuleCacheStatistics.builder().cacheHits(50).cacheMisses(20).build();
      assertEquals(stats1.hashCode(), stats2.hashCode(), "Equal stats should have same hash code");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final ModuleCacheStatistics stats = ModuleCacheStatistics.builder().build();
      assertNotEquals(null, stats, "Stats should not be equal to null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final ModuleCacheStatistics stats = ModuleCacheStatistics.builder().build();
      assertNotEquals("not stats", stats, "Stats should not be equal to different type");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return meaningful string representation")
    void shouldReturnMeaningfulStringRepresentation() {
      final ModuleCacheStatistics stats =
          ModuleCacheStatistics.builder()
              .cacheHits(80)
              .cacheMisses(20)
              .entriesCount(15)
              .storageBytesUsed(512000)
              .compressionRatio(0.5)
              .evictionCount(5)
              .build();
      final String str = stats.toString();
      assertTrue(str.contains("ModuleCacheStatistics"), "Should contain class name");
      assertTrue(str.contains("hits=80"), "Should contain hits");
      assertTrue(str.contains("misses=20"), "Should contain misses");
      assertTrue(str.contains("entries=15"), "Should contain entries");
      assertTrue(str.contains("512000"), "Should contain storage bytes");
      assertTrue(str.contains("0.5"), "Should contain compression ratio");
      assertTrue(str.contains("evictions=5"), "Should contain evictions");
    }

    @Test
    @DisplayName("should include hit rate percentage in toString")
    void shouldIncludeHitRatePercentageInToString() {
      final ModuleCacheStatistics stats =
          ModuleCacheStatistics.builder().cacheHits(75).cacheMisses(25).build();
      final String str = stats.toString();
      assertTrue(str.contains("75.00%"), "Should contain 75% hit rate");
    }
  }

  @Nested
  @DisplayName("Full Integration Tests")
  class FullIntegrationTests {

    @Test
    @DisplayName("should build complete statistics")
    void shouldBuildCompleteStatistics() {
      final ModuleCacheStatistics stats =
          ModuleCacheStatistics.builder()
              .cacheHits(1000)
              .cacheMisses(200)
              .entriesCount(50)
              .storageBytesUsed(10 * 1024 * 1024) // 10 MB
              .compressionRatio(0.45)
              .evictionCount(30)
              .build();

      assertEquals(1000, stats.getCacheHits(), "Cache hits should match");
      assertEquals(200, stats.getCacheMisses(), "Cache misses should match");
      assertEquals(1200, stats.getTotalRequests(), "Total requests should match");
      assertEquals(0.833, stats.getHitRate(), 0.001, "Hit rate should be ~83%");
      assertEquals(50, stats.getEntriesCount(), "Entries count should match");
      assertEquals(10 * 1024 * 1024, stats.getStorageBytesUsed(), "Storage bytes should match");
      assertEquals(0.45, stats.getCompressionRatio(), 0.001, "Compression ratio should match");
      assertEquals(30, stats.getEvictionCount(), "Eviction count should match");
    }
  }
}
