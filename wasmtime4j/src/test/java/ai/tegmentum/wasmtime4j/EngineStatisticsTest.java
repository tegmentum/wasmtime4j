package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for EngineStatistics interface default methods.
 *
 * <p>Tests the calculated statistics methods (cache hit rate and average compilation time) to
 * ensure correct mathematical operations and edge case handling.
 */
@DisplayName("EngineStatistics Tests")
final class EngineStatisticsTest {

  @Nested
  @DisplayName("Cache Hit Rate Tests")
  class CacheHitRateTests {

    @Test
    @DisplayName("Cache hit rate with normal values")
    void testCacheHitRateNormal() {
      final EngineStatistics stats = createMockStatistics(80L, 20L, 0L, 0L, 0L, 0L);

      final double hitRate = stats.getCacheHitRate();
      assertEquals(80.0, hitRate, 0.001, "Hit rate should be 80% (80 hits / 100 total)");
    }

    @Test
    @DisplayName("Cache hit rate with 100% hits")
    void testCacheHitRateAllHits() {
      final EngineStatistics stats = createMockStatistics(100L, 0L, 0L, 0L, 0L, 0L);

      final double hitRate = stats.getCacheHitRate();
      assertEquals(100.0, hitRate, 0.001, "Hit rate should be 100% with no misses");
    }

    @Test
    @DisplayName("Cache hit rate with 0% hits")
    void testCacheHitRateNoHits() {
      final EngineStatistics stats = createMockStatistics(0L, 50L, 0L, 0L, 0L, 0L);

      final double hitRate = stats.getCacheHitRate();
      assertEquals(0.0, hitRate, 0.001, "Hit rate should be 0% with no hits");
    }

    @Test
    @DisplayName("Cache hit rate with no cache activity")
    void testCacheHitRateNoActivity() {
      final EngineStatistics stats = createMockStatistics(0L, 0L, 0L, 0L, 0L, 0L);

      final double hitRate = stats.getCacheHitRate();
      assertEquals(0.0, hitRate, 0.001, "Hit rate should be 0% with no cache activity");
    }

    @Test
    @DisplayName("Cache hit rate with partial hits")
    void testCacheHitRatePartial() {
      final EngineStatistics stats = createMockStatistics(33L, 67L, 0L, 0L, 0L, 0L);

      final double hitRate = stats.getCacheHitRate();
      assertEquals(33.0, hitRate, 0.001, "Hit rate should be 33% (33 hits / 100 total)");
    }

    @Test
    @DisplayName("Cache hit rate with large numbers")
    void testCacheHitRateLargeNumbers() {
      final long hits = 999_999L;
      final long misses = 1L;
      final EngineStatistics stats = createMockStatistics(hits, misses, 0L, 0L, 0L, 0L);

      final double hitRate = stats.getCacheHitRate();
      final double expected = 99.9999; // 999999 / 1000000 * 100
      assertEquals(expected, hitRate, 0.001, "Hit rate should handle large numbers correctly");
    }
  }

  @Nested
  @DisplayName("Average Compilation Time Tests")
  class AverageCompilationTimeTests {

    @Test
    @DisplayName("Average compilation time with normal values")
    void testAverageCompilationTimeNormal() {
      final EngineStatistics stats = createMockStatistics(0L, 0L, 10L, 0L, 0L, 5000L);

      final double avgTime = stats.getAverageCompilationTimeMs();
      assertEquals(500.0, avgTime, 0.001, "Average should be 500ms (5000ms / 10 modules)");
    }

    @Test
    @DisplayName("Average compilation time with no modules")
    void testAverageCompilationTimeNoModules() {
      final EngineStatistics stats = createMockStatistics(0L, 0L, 0L, 0L, 0L, 0L);

      final double avgTime = stats.getAverageCompilationTimeMs();
      assertEquals(0.0, avgTime, 0.001, "Average should be 0 with no compiled modules");
    }

    @Test
    @DisplayName("Average compilation time with single module")
    void testAverageCompilationTimeSingle() {
      final EngineStatistics stats = createMockStatistics(0L, 0L, 1L, 0L, 0L, 1500L);

      final double avgTime = stats.getAverageCompilationTimeMs();
      assertEquals(1500.0, avgTime, 0.001, "Average should equal total time for single module");
    }

    @Test
    @DisplayName("Average compilation time with fast compilations")
    void testAverageCompilationTimeFast() {
      final EngineStatistics stats = createMockStatistics(0L, 0L, 1000L, 0L, 0L, 100L);

      final double avgTime = stats.getAverageCompilationTimeMs();
      assertEquals(0.1, avgTime, 0.001, "Average should be 0.1ms (100ms / 1000 modules)");
    }

    @Test
    @DisplayName("Average compilation time with slow compilations")
    void testAverageCompilationTimeSlow() {
      final EngineStatistics stats = createMockStatistics(0L, 0L, 2L, 0L, 0L, 10000L);

      final double avgTime = stats.getAverageCompilationTimeMs();
      assertEquals(5000.0, avgTime, 0.001, "Average should be 5000ms (10000ms / 2 modules)");
    }

    @Test
    @DisplayName("Average compilation time handles precision correctly")
    void testAverageCompilationTimePrecision() {
      final EngineStatistics stats = createMockStatistics(0L, 0L, 3L, 0L, 0L, 10L);

      final double avgTime = stats.getAverageCompilationTimeMs();
      final double expected = 10.0 / 3.0; // Should be 3.333...
      assertEquals(expected, avgTime, 0.001, "Average should handle fractional results correctly");
    }
  }

  @Nested
  @DisplayName("Combined Statistics Tests")
  class CombinedStatisticsTests {

    @Test
    @DisplayName("Realistic engine statistics scenario")
    void testRealisticScenario() {
      // Simulate an engine that has compiled 100 modules, with 85% cache hit rate
      // and average compilation time of 250ms
      final EngineStatistics stats =
          createMockStatistics(
              85L, // cache hits
              15L, // cache misses
              100L, // compiled modules
              1024L * 1024L, // 1MB memory usage
              2048L * 1024L, // 2MB peak memory usage
              25000L // 25 seconds total compilation time
              );

      // Test cache hit rate
      final double hitRate = stats.getCacheHitRate();
      assertEquals(85.0, hitRate, 0.001, "Hit rate should be 85%");

      // Test average compilation time
      final double avgTime = stats.getAverageCompilationTimeMs();
      assertEquals(250.0, avgTime, 0.001, "Average compilation time should be 250ms");

      // Test direct getters work as expected
      assertEquals(85L, stats.getCacheHits(), "Cache hits should match");
      assertEquals(15L, stats.getCacheMisses(), "Cache misses should match");
      assertEquals(100L, stats.getCompiledModuleCount(), "Module count should match");
      assertEquals(1024L * 1024L, stats.getMemoryUsage(), "Memory usage should match");
      assertEquals(2048L * 1024L, stats.getPeakMemoryUsage(), "Peak memory usage should match");
      assertEquals(
          25000L, stats.getTotalCompilationTimeMs(), "Total compilation time should match");
    }

    @Test
    @DisplayName("High-performance engine statistics")
    void testHighPerformanceScenario() {
      // Simulate a high-performance engine with excellent cache performance
      final EngineStatistics stats =
          createMockStatistics(
              9950L, // cache hits
              50L, // cache misses (99.5% hit rate)
              10000L, // compiled modules
              512L * 1024L, // 512KB memory usage
              1024L * 1024L, // 1MB peak memory usage
              5000L // 5 seconds total compilation time (0.5ms average)
              );

      final double hitRate = stats.getCacheHitRate();
      assertTrue(hitRate > 99.0, "High-performance engine should have >99% hit rate");
      assertEquals(99.5, hitRate, 0.001, "Hit rate should be exactly 99.5%");

      final double avgTime = stats.getAverageCompilationTimeMs();
      assertTrue(avgTime < 1.0, "High-performance engine should have <1ms average");
      assertEquals(0.5, avgTime, 0.001, "Average time should be exactly 0.5ms");
    }

    @Test
    @DisplayName("Development engine statistics")
    void testDevelopmentScenario() {
      // Simulate a development engine with poor cache performance but detailed tracking
      final EngineStatistics stats =
          createMockStatistics(
              10L, // cache hits
              90L, // cache misses (10% hit rate - lots of code changes)
              100L, // compiled modules
              4096L * 1024L, // 4MB memory usage (debug builds)
              8192L * 1024L, // 8MB peak memory usage
              50000L // 50 seconds total compilation time (500ms average - debug builds)
              );

      final double hitRate = stats.getCacheHitRate();
      assertTrue(hitRate < 20.0, "Development engine should have low hit rate");
      assertEquals(10.0, hitRate, 0.001, "Hit rate should be exactly 10%");

      final double avgTime = stats.getAverageCompilationTimeMs();
      assertTrue(avgTime > 100.0, "Development engine should have slower compilation");
      assertEquals(500.0, avgTime, 0.001, "Average time should be exactly 500ms");

      // Memory usage should be higher for debug builds
      assertTrue(stats.getMemoryUsage() > 1024L * 1024L, "Should use more than 1MB");
      assertTrue(stats.getPeakMemoryUsage() > stats.getMemoryUsage(), "Peak should exceed current");
    }
  }

  /** Creates a mock EngineStatistics instance for testing. */
  private EngineStatistics createMockStatistics(
      final long cacheHits,
      final long cacheMisses,
      final long compiledModuleCount,
      final long memoryUsage,
      final long peakMemoryUsage,
      final long totalCompilationTimeMs) {
    return new EngineStatistics() {
      @Override
      public long getCompiledModuleCount() {
        return compiledModuleCount;
      }

      @Override
      public long getCacheHits() {
        return cacheHits;
      }

      @Override
      public long getCacheMisses() {
        return cacheMisses;
      }

      @Override
      public long getMemoryUsage() {
        return memoryUsage;
      }

      @Override
      public long getPeakMemoryUsage() {
        return peakMemoryUsage;
      }

      @Override
      public long getTotalCompilationTimeMs() {
        return totalCompilationTimeMs;
      }
    };
  }
}
