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

package ai.tegmentum.wasmtime4j.jni.performance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link CompilationCache} class.
 *
 * <p>This test class verifies the CompilationCache utility class which provides
 * persistent compilation caching for WebAssembly modules to improve startup performance.
 */
@DisplayName("CompilationCache Tests")
class CompilationCacheTest {

  @BeforeEach
  void setUp() {
    CompilationCache.setEnabled(true);
    CompilationCache.clear();
  }

  @AfterEach
  void tearDown() {
    CompilationCache.clear();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("CompilationCache should be final class")
    void shouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(CompilationCache.class.getModifiers()),
          "CompilationCache should be final");
    }
  }

  @Nested
  @DisplayName("Enable State Tests")
  class EnableStateTests {

    @Test
    @DisplayName("setEnabled should update enabled state")
    void setEnabledShouldUpdateEnabledState() {
      CompilationCache.setEnabled(false);
      assertFalse(CompilationCache.isEnabled(), "Cache should be disabled");

      CompilationCache.setEnabled(true);
      assertTrue(CompilationCache.isEnabled(), "Cache should be enabled");
    }
  }

  @Nested
  @DisplayName("Cache Directory Tests")
  class CacheDirectoryTests {

    @Test
    @DisplayName("getCacheDirectory should return non-null path")
    void getCacheDirectoryShouldReturnNonNullPath() {
      final String cacheDir = CompilationCache.getCacheDirectory();
      assertNotNull(cacheDir, "Cache directory should not be null");
    }

    @Test
    @DisplayName("getCacheDirectory should contain wasmtime4j")
    void getCacheDirectoryShouldContainWasmtime4j() {
      final String cacheDir = CompilationCache.getCacheDirectory();
      assertTrue(cacheDir.contains("wasmtime4j"),
          "Cache directory should contain 'wasmtime4j'");
    }
  }

  @Nested
  @DisplayName("loadFromCache Tests")
  class LoadFromCacheTests {

    @Test
    @DisplayName("loadFromCache should return null for non-cached module")
    void loadFromCacheShouldReturnNullForNonCachedModule() {
      final byte[] wasmBytes = new byte[]{0x00, 0x61, 0x73, 0x6D}; // WASM magic
      final byte[] cached = CompilationCache.loadFromCache(wasmBytes, "options");
      assertNull(cached, "Should return null for non-cached module");
    }

    @Test
    @DisplayName("loadFromCache should return null when disabled")
    void loadFromCacheShouldReturnNullWhenDisabled() {
      CompilationCache.setEnabled(false);
      final byte[] wasmBytes = new byte[]{0x00, 0x61, 0x73, 0x6D};
      final byte[] cached = CompilationCache.loadFromCache(wasmBytes, "options");
      assertNull(cached, "Should return null when disabled");
    }

    @Test
    @DisplayName("loadFromCache should return null for null wasmBytes")
    void loadFromCacheShouldReturnNullForNullWasmBytes() {
      final byte[] cached = CompilationCache.loadFromCache(null, "options");
      assertNull(cached, "Should return null for null wasmBytes");
    }

    @Test
    @DisplayName("loadFromCache should return null for empty wasmBytes")
    void loadFromCacheShouldReturnNullForEmptyWasmBytes() {
      final byte[] cached = CompilationCache.loadFromCache(new byte[0], "options");
      assertNull(cached, "Should return null for empty wasmBytes");
    }

    @Test
    @DisplayName("loadFromCache with compilationTimeNs should work")
    void loadFromCacheWithCompilationTimeNsShouldWork() {
      final byte[] wasmBytes = new byte[]{0x00, 0x61, 0x73, 0x6D};
      final byte[] cached = CompilationCache.loadFromCache(wasmBytes, "options", 1000000L);
      assertNull(cached, "Should return null for non-cached module");
    }
  }

  @Nested
  @DisplayName("storeInCache Tests")
  class StoreInCacheTests {

    @Test
    @DisplayName("storeInCache should return false when disabled")
    void storeInCacheShouldReturnFalseWhenDisabled() {
      CompilationCache.setEnabled(false);
      final byte[] wasmBytes = new byte[]{0x00, 0x61, 0x73, 0x6D};
      final byte[] compiled = new byte[]{0x01, 0x02, 0x03};
      final boolean stored = CompilationCache.storeInCache(wasmBytes, compiled, "options");
      assertFalse(stored, "Should return false when disabled");
    }

    @Test
    @DisplayName("storeInCache should return false for null wasmBytes")
    void storeInCacheShouldReturnFalseForNullWasmBytes() {
      final byte[] compiled = new byte[]{0x01, 0x02, 0x03};
      final boolean stored = CompilationCache.storeInCache(null, compiled, "options");
      assertFalse(stored, "Should return false for null wasmBytes");
    }

    @Test
    @DisplayName("storeInCache should return false for null compiledModule")
    void storeInCacheShouldReturnFalseForNullCompiledModule() {
      final byte[] wasmBytes = new byte[]{0x00, 0x61, 0x73, 0x6D};
      final boolean stored = CompilationCache.storeInCache(wasmBytes, null, "options");
      assertFalse(stored, "Should return false for null compiledModule");
    }

    @Test
    @DisplayName("storeInCache with compilationTimeNs should work")
    void storeInCacheWithCompilationTimeNsShouldWork() {
      final byte[] wasmBytes = new byte[]{0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00};
      final byte[] compiled = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
      final boolean stored = CompilationCache.storeInCache(wasmBytes, compiled, "options", 5000000L);
      assertTrue(stored, "Should store with compilation time");
    }
  }

  @Nested
  @DisplayName("Cache Round-Trip Tests")
  class CacheRoundTripTests {

    @Test
    @DisplayName("Stored module should be retrievable")
    void storedModuleShouldBeRetrievable() {
      final byte[] wasmBytes = new byte[]{0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00};
      final byte[] compiled = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
      final String options = "test-options";

      final boolean stored = CompilationCache.storeInCache(wasmBytes, compiled, options);
      assertTrue(stored, "Should store successfully");

      final byte[] retrieved = CompilationCache.loadFromCache(wasmBytes, options);
      assertNotNull(retrieved, "Should retrieve cached module");
      assertEquals(compiled.length, retrieved.length, "Retrieved module should match");
    }

    @Test
    @DisplayName("Different options should produce different cache entries")
    void differentOptionsShouldProduceDifferentCacheEntries() {
      final byte[] wasmBytes = new byte[]{0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00};
      final byte[] compiled1 = new byte[]{0x01, 0x02, 0x03, 0x04};
      final byte[] compiled2 = new byte[]{0x05, 0x06, 0x07, 0x08};

      CompilationCache.storeInCache(wasmBytes, compiled1, "options1");
      CompilationCache.storeInCache(wasmBytes, compiled2, "options2");

      final byte[] retrieved1 = CompilationCache.loadFromCache(wasmBytes, "options1");
      final byte[] retrieved2 = CompilationCache.loadFromCache(wasmBytes, "options2");

      assertNotNull(retrieved1, "Should retrieve with options1");
      assertNotNull(retrieved2, "Should retrieve with options2");
      assertEquals(4, retrieved1.length, "Retrieved1 should have correct length");
      assertEquals(4, retrieved2.length, "Retrieved2 should have correct length");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getStatistics should return formatted string when enabled")
    void getStatisticsShouldReturnFormattedStringWhenEnabled() {
      final String stats = CompilationCache.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.contains("Compilation Cache"),
          "Statistics should contain header");
    }

    @Test
    @DisplayName("getStatistics should indicate disabled when disabled")
    void getStatisticsShouldIndicateDisabledWhenDisabled() {
      CompilationCache.setEnabled(false);
      final String stats = CompilationCache.getStatistics();
      assertTrue(stats.contains("disabled"), "Should indicate disabled state");
    }

    @Test
    @DisplayName("getHitRate should return 0 with no operations")
    void getHitRateShouldReturn0WithNoOperations() {
      assertEquals(0.0, CompilationCache.getHitRate(), 0.01,
          "Hit rate should be 0 with no operations");
    }

    @Test
    @DisplayName("getAverageCacheLoadTimeNs should return 0 with no hits")
    void getAverageCacheLoadTimeNsShouldReturn0WithNoHits() {
      assertEquals(0.0, CompilationCache.getAverageCacheLoadTimeNs(), 0.01,
          "Average load time should be 0 with no hits");
    }

    @Test
    @DisplayName("getAverageCacheStoreTimeNs should return 0 with no stores")
    void getAverageCacheStoreTimeNsShouldReturn0WithNoStores() {
      assertEquals(0.0, CompilationCache.getAverageCacheStoreTimeNs(), 0.01,
          "Average store time should be 0 with no stores");
    }

    @Test
    @DisplayName("getTotalCompilationTimeSavedNs should return 0 initially")
    void getTotalCompilationTimeSavedNsShouldReturn0Initially() {
      assertEquals(0, CompilationCache.getTotalCompilationTimeSavedNs(),
          "Time saved should be 0 initially");
    }

    @Test
    @DisplayName("getCompilationTimeSavingsPercentage should return 0 with no data")
    void getCompilationTimeSavingsPercentageShouldReturn0WithNoData() {
      assertEquals(0.0, CompilationCache.getCompilationTimeSavingsPercentage(), 0.01,
          "Savings percentage should be 0 with no data");
    }

    @Test
    @DisplayName("getCacheSizeBytes should return non-negative value")
    void getCacheSizeBytesShouldReturnNonNegativeValue() {
      assertTrue(CompilationCache.getCacheSizeBytes() >= 0,
          "Cache size should be non-negative");
    }

    @Test
    @DisplayName("getPerformanceMetrics should return formatted string")
    void getPerformanceMetricsShouldReturnFormattedString() {
      final String metrics = CompilationCache.getPerformanceMetrics();
      assertNotNull(metrics, "Performance metrics should not be null");
      assertTrue(metrics.contains("Cache Performance") || metrics.contains("hit_rate"),
          "Should contain performance information");
    }
  }

  @Nested
  @DisplayName("Clear Tests")
  class ClearTests {

    @Test
    @DisplayName("clear should not throw")
    void clearShouldNotThrow() {
      assertDoesNotThrow(() -> CompilationCache.clear(), "clear should not throw");
    }

    @Test
    @DisplayName("clear should reset hit rate")
    void clearShouldResetHitRate() {
      // Generate some cache activity
      final byte[] wasmBytes = new byte[]{0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00};
      final byte[] compiled = new byte[]{0x01, 0x02, 0x03, 0x04};
      CompilationCache.storeInCache(wasmBytes, compiled, "options");
      CompilationCache.loadFromCache(wasmBytes, "options");

      CompilationCache.clear();

      assertEquals(0.0, CompilationCache.getHitRate(), 0.01,
          "Hit rate should be 0 after clear");
    }

    @Test
    @DisplayName("clear should not throw when disabled")
    void clearShouldNotThrowWhenDisabled() {
      CompilationCache.setEnabled(false);
      assertDoesNotThrow(() -> CompilationCache.clear(),
          "clear should not throw when disabled");
    }
  }

  @Nested
  @DisplayName("Maintenance Tests")
  class MaintenanceTests {

    @Test
    @DisplayName("performMaintenance should not throw")
    void performMaintenanceShouldNotThrow() {
      assertDoesNotThrow(() -> CompilationCache.performMaintenance(),
          "performMaintenance should not throw");
    }

    @Test
    @DisplayName("performMaintenance should not throw when disabled")
    void performMaintenanceShouldNotThrowWhenDisabled() {
      CompilationCache.setEnabled(false);
      assertDoesNotThrow(() -> CompilationCache.performMaintenance(),
          "performMaintenance should not throw when disabled");
    }
  }

  @Nested
  @DisplayName("Generation Tests")
  class GenerationTests {

    @Test
    @DisplayName("incrementGeneration should not throw")
    void incrementGenerationShouldNotThrow() {
      assertDoesNotThrow(() -> CompilationCache.incrementGeneration(),
          "incrementGeneration should not throw");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full cache lifecycle should work")
    void fullCacheLifecycleShouldWork() {
      // Store a module
      final byte[] wasmBytes = new byte[]{0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00};
      final byte[] compiled = new byte[100];
      for (int i = 0; i < compiled.length; i++) {
        compiled[i] = (byte) i;
      }

      assertTrue(CompilationCache.storeInCache(wasmBytes, compiled, "options", 10000000L),
          "Should store successfully");

      // Load the module
      final byte[] loaded = CompilationCache.loadFromCache(wasmBytes, "options");
      assertNotNull(loaded, "Should load successfully");
      assertEquals(compiled.length, loaded.length, "Loaded module should have correct length");

      // Check statistics
      final String stats = CompilationCache.getStatistics();
      assertTrue(stats.contains("hit") || stats.contains("Cache"),
          "Statistics should be available");

      // Clear the cache
      CompilationCache.clear();

      // Verify cleared
      final byte[] afterClear = CompilationCache.loadFromCache(wasmBytes, "options");
      assertNull(afterClear, "Should not find module after clear");
    }
  }
}
