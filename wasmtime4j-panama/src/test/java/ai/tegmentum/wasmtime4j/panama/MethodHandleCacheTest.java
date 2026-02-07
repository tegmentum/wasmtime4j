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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for MethodHandleCache.
 *
 * <p>These tests exercise actual code execution to improve JaCoCo coverage. Note: Some methods
 * require actual FFI symbols which we cannot test without native library.
 */
@DisplayName("Method Handle Cache Integration Tests")
public class MethodHandleCacheTest {

  private static final Logger LOGGER = Logger.getLogger(MethodHandleCacheTest.class.getName());

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create cache with default settings")
    void shouldCreateCacheWithDefaultSettings() {
      LOGGER.info("Testing default constructor");

      final MethodHandleCache cache = new MethodHandleCache();

      assertNotNull(cache, "Cache should not be null");
      assertEquals(1000, cache.getMaxSize(), "Default max size should be 1000");
      assertEquals(0, cache.size(), "Initial size should be 0");
      assertTrue(cache.isEmpty(), "Cache should be empty");

      LOGGER.info(
          "Default cache created: maxSize=" + cache.getMaxSize() + ", size=" + cache.size());
    }

    @Test
    @DisplayName("Should create cache with custom max size")
    void shouldCreateCacheWithCustomMaxSize() {
      LOGGER.info("Testing custom max size constructor");

      final MethodHandleCache cache = new MethodHandleCache(500, true);

      assertEquals(500, cache.getMaxSize(), "Max size should be 500");
      assertEquals(0, cache.size(), "Initial size should be 0");
      assertTrue(cache.isEmpty(), "Cache should be empty");

      LOGGER.info("Custom cache created: maxSize=" + cache.getMaxSize());
    }

    @Test
    @DisplayName("Should create cache with statistics disabled")
    void shouldCreateCacheWithStatisticsDisabled() {
      LOGGER.info("Testing cache with statistics disabled");

      final MethodHandleCache cache = new MethodHandleCache(100, false);

      assertEquals(100, cache.getMaxSize(), "Max size should be 100");
      assertFalse(
          cache.getStatistics().isPresent(), "Statistics should not be present when disabled");

      LOGGER.info("Cache with disabled statistics created");
    }

    @Test
    @DisplayName("Should reject zero max size")
    void shouldRejectZeroMaxSize() {
      LOGGER.info("Testing zero max size rejection");

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new MethodHandleCache(0, true));

      assertTrue(
          ex.getMessage().contains("positive"),
          "Error should mention positive: " + ex.getMessage());

      LOGGER.info("Correctly rejected zero max size: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject negative max size")
    void shouldRejectNegativeMaxSize() {
      LOGGER.info("Testing negative max size rejection");

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new MethodHandleCache(-10, true));

      assertTrue(
          ex.getMessage().contains("positive"),
          "Error should mention positive: " + ex.getMessage());

      LOGGER.info("Correctly rejected negative max size: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("Basic Operation Tests")
  class BasicOperationTests {

    @Test
    @DisplayName("Should report empty status correctly")
    void shouldReportEmptyStatusCorrectly() {
      LOGGER.info("Testing isEmpty and size");

      final MethodHandleCache cache = new MethodHandleCache();

      assertTrue(cache.isEmpty(), "New cache should be empty");
      assertEquals(0, cache.size(), "New cache size should be 0");

      LOGGER.info("Empty status verified: isEmpty=" + cache.isEmpty() + ", size=" + cache.size());
    }

    @Test
    @DisplayName("Should clear cache")
    void shouldClearCache() {
      LOGGER.info("Testing clear");

      final MethodHandleCache cache = new MethodHandleCache();

      cache.clear();

      assertTrue(cache.isEmpty(), "Cache should be empty after clear");
      assertEquals(0, cache.size(), "Cache size should be 0 after clear");

      LOGGER.info("Cache cleared successfully");
    }

    @Test
    @DisplayName("Should get max size")
    void shouldGetMaxSize() {
      LOGGER.info("Testing getMaxSize");

      final MethodHandleCache cache1 = new MethodHandleCache();
      assertEquals(1000, cache1.getMaxSize(), "Default max size should be 1000");

      final MethodHandleCache cache2 = new MethodHandleCache(250, true);
      assertEquals(250, cache2.getMaxSize(), "Custom max size should be 250");

      LOGGER.info("Max size values verified");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("Should return statistics when enabled")
    void shouldReturnStatisticsWhenEnabled() {
      LOGGER.info("Testing statistics when enabled");

      final MethodHandleCache cache = new MethodHandleCache(100, true);

      final Optional<MethodHandleCache.CacheStatistics> stats = cache.getStatistics();

      assertTrue(stats.isPresent(), "Statistics should be present");

      final MethodHandleCache.CacheStatistics s = stats.get();
      assertEquals(0, s.getHitCount(), "Hit count should be 0");
      assertEquals(0, s.getMissCount(), "Miss count should be 0");
      assertEquals(0, s.getEvictionCount(), "Eviction count should be 0");
      assertEquals(0, s.getTotalLoadTime(), "Total load time should be 0");
      assertEquals(0, s.getCurrentSize(), "Current size should be 0");
      assertEquals(0.0, s.getHitRate(), 0.001, "Hit rate should be 0");
      assertEquals(0.0, s.getAverageLoadTime(), 0.001, "Average load time should be 0");

      LOGGER.info("Statistics retrieved: " + s);
    }

    @Test
    @DisplayName("Should return empty when statistics disabled")
    void shouldReturnEmptyWhenStatisticsDisabled() {
      LOGGER.info("Testing statistics when disabled");

      final MethodHandleCache cache = new MethodHandleCache(100, false);

      final Optional<MethodHandleCache.CacheStatistics> stats = cache.getStatistics();

      assertFalse(stats.isPresent(), "Statistics should not be present");

      LOGGER.info("Statistics correctly not present when disabled");
    }

    @Test
    @DisplayName("Should reset statistics")
    void shouldResetStatistics() {
      LOGGER.info("Testing resetStatistics");

      final MethodHandleCache cache = new MethodHandleCache(100, true);

      cache.resetStatistics();

      final Optional<MethodHandleCache.CacheStatistics> stats = cache.getStatistics();
      assertTrue(stats.isPresent(), "Statistics should still be present");

      final MethodHandleCache.CacheStatistics s = stats.get();
      assertEquals(0, s.getHitCount(), "Hit count should be 0 after reset");
      assertEquals(0, s.getMissCount(), "Miss count should be 0 after reset");
      assertEquals(0, s.getEvictionCount(), "Eviction count should be 0 after reset");
      assertEquals(0, s.getTotalLoadTime(), "Total load time should be 0 after reset");

      LOGGER.info("Statistics reset successfully");
    }

    @Test
    @DisplayName("Should not fail when resetting disabled statistics")
    void shouldNotFailWhenResettingDisabledStatistics() {
      LOGGER.info("Testing resetStatistics when disabled");

      final MethodHandleCache cache = new MethodHandleCache(100, false);

      // Should not throw
      cache.resetStatistics();

      assertFalse(cache.getStatistics().isPresent(), "Statistics should still not be present");

      LOGGER.info("Reset with disabled statistics succeeded");
    }
  }

  @Nested
  @DisplayName("CacheStatistics Tests")
  class CacheStatisticsTests {

    @Test
    @DisplayName("Should calculate hit rate correctly")
    void shouldCalculateHitRateCorrectly() {
      LOGGER.info("Testing hit rate calculation");

      // Test with zero requests
      final MethodHandleCache cache = new MethodHandleCache(100, true);
      final MethodHandleCache.CacheStatistics stats = cache.getStatistics().orElseThrow();

      assertEquals(0.0, stats.getHitRate(), 0.001, "Hit rate should be 0 with no requests");

      LOGGER.info("Hit rate calculation verified");
    }

    @Test
    @DisplayName("Should calculate average load time correctly")
    void shouldCalculateAverageLoadTimeCorrectly() {
      LOGGER.info("Testing average load time calculation");

      // Test with zero loads
      final MethodHandleCache cache = new MethodHandleCache(100, true);
      final MethodHandleCache.CacheStatistics stats = cache.getStatistics().orElseThrow();

      assertEquals(
          0.0, stats.getAverageLoadTime(), 0.001, "Average load time should be 0 with no loads");

      LOGGER.info("Average load time calculation verified");
    }

    @Test
    @DisplayName("Should produce readable toString")
    void shouldProduceReadableToString() {
      LOGGER.info("Testing statistics toString");

      final MethodHandleCache cache = new MethodHandleCache(100, true);
      final MethodHandleCache.CacheStatistics stats = cache.getStatistics().orElseThrow();

      final String str = stats.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("CacheStatistics"), "toString should contain class name");
      assertTrue(str.contains("hitCount"), "toString should contain hitCount");
      assertTrue(str.contains("missCount"), "toString should contain missCount");
      assertTrue(str.contains("hitRate"), "toString should contain hitRate");

      LOGGER.info("Statistics toString: " + str);
    }

    @Test
    @DisplayName("Should have correct initial values")
    void shouldHaveCorrectInitialValues() {
      LOGGER.info("Testing initial statistics values");

      final MethodHandleCache cache = new MethodHandleCache(100, true);
      final MethodHandleCache.CacheStatistics stats = cache.getStatistics().orElseThrow();

      assertEquals(0, stats.getHitCount(), "Initial hit count should be 0");
      assertEquals(0, stats.getMissCount(), "Initial miss count should be 0");
      assertEquals(0, stats.getEvictionCount(), "Initial eviction count should be 0");
      assertEquals(0, stats.getTotalLoadTime(), "Initial total load time should be 0");
      assertEquals(0, stats.getCurrentSize(), "Initial current size should be 0");

      LOGGER.info("Initial statistics values verified");
    }
  }

  @Nested
  @DisplayName("Multiple Cache Instance Tests")
  class MultipleCacheInstanceTests {

    @Test
    @DisplayName("Should maintain independent caches")
    void shouldMaintainIndependentCaches() {
      LOGGER.info("Testing independent cache instances");

      final MethodHandleCache cache1 = new MethodHandleCache(100, true);
      final MethodHandleCache cache2 = new MethodHandleCache(200, false);
      final MethodHandleCache cache3 = new MethodHandleCache(300, true);

      assertEquals(100, cache1.getMaxSize(), "Cache1 max size should be 100");
      assertEquals(200, cache2.getMaxSize(), "Cache2 max size should be 200");
      assertEquals(300, cache3.getMaxSize(), "Cache3 max size should be 300");

      assertTrue(cache1.getStatistics().isPresent(), "Cache1 statistics should be present");
      assertFalse(cache2.getStatistics().isPresent(), "Cache2 statistics should not be present");
      assertTrue(cache3.getStatistics().isPresent(), "Cache3 statistics should be present");

      // Clear one cache shouldn't affect others
      cache1.clear();
      assertEquals(0, cache1.size(), "Cache1 should be empty after clear");

      LOGGER.info("Independent cache instances verified");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle minimum max size")
    void shouldHandleMinimumMaxSize() {
      LOGGER.info("Testing minimum max size");

      final MethodHandleCache cache = new MethodHandleCache(1, true);

      assertEquals(1, cache.getMaxSize(), "Max size should be 1");
      assertTrue(cache.isEmpty(), "Cache should be empty");

      LOGGER.info("Minimum max size (1) handled correctly");
    }

    @Test
    @DisplayName("Should handle large max size")
    void shouldHandleLargeMaxSize() {
      LOGGER.info("Testing large max size");

      final MethodHandleCache cache = new MethodHandleCache(100000, true);

      assertEquals(100000, cache.getMaxSize(), "Max size should be 100000");
      assertTrue(cache.isEmpty(), "Cache should be empty");

      LOGGER.info("Large max size (100000) handled correctly");
    }

    @Test
    @DisplayName("Should handle repeated clears")
    void shouldHandleRepeatedClears() {
      LOGGER.info("Testing repeated clears");

      final MethodHandleCache cache = new MethodHandleCache();

      for (int i = 0; i < 10; i++) {
        cache.clear();
        assertTrue(cache.isEmpty(), "Cache should be empty after clear " + (i + 1));
      }

      LOGGER.info("Repeated clears handled correctly");
    }

    @Test
    @DisplayName("Should handle repeated statistics resets")
    void shouldHandleRepeatedStatisticsResets() {
      LOGGER.info("Testing repeated statistics resets");

      final MethodHandleCache cache = new MethodHandleCache(100, true);

      for (int i = 0; i < 10; i++) {
        cache.resetStatistics();
        final MethodHandleCache.CacheStatistics stats = cache.getStatistics().orElseThrow();
        assertEquals(0, stats.getHitCount(), "Hit count should be 0 after reset " + (i + 1));
      }

      LOGGER.info("Repeated statistics resets handled correctly");
    }
  }
}
