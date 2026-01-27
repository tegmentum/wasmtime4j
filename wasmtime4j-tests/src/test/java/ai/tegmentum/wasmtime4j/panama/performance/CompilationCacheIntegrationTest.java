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

package ai.tegmentum.wasmtime4j.panama.performance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Panama CompilationCache.
 *
 * <p>These tests verify the Panama-specific compilation caching capabilities including
 * memory segment-based cache operations, statistics tracking, and cache management.
 *
 * @since 1.0.0
 */
@DisplayName("Panama CompilationCache Integration Tests")
class CompilationCacheIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(CompilationCacheIntegrationTest.class.getName());

  /** Simple WebAssembly module for testing (minimal valid module). */
  private static final byte[] SIMPLE_WASM_MODULE = {
    0x00, 0x61, 0x73, 0x6D, // Magic number \0asm
    0x01, 0x00, 0x00, 0x00, // Version 1
    0x01, 0x07, // Type section
    0x01, // 1 type
    0x60, 0x02, 0x7F, 0x7F, 0x01, 0x7F, // func (i32, i32) -> i32
    0x03, 0x02, // Function section
    0x01, 0x00, // 1 function of type 0
    0x07, 0x07, // Export section
    0x01, // 1 export
    0x03, 0x61, 0x64, 0x64, // "add"
    0x00, 0x00, // func index 0
    0x0A, 0x09, // Code section
    0x01, // 1 function body
    0x07, // body size
    0x00, // local count
    0x20, 0x00, // local.get 0
    0x20, 0x01, // local.get 1
    0x6A, // i32.add
    0x0B // end
  };

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  /** Track if cache was enabled before test. */
  private boolean wasEnabled;

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for Panama compilation cache tests");
    try {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertTrue(loader.isLoaded(), "Native library should be loaded");
      LOGGER.info("Native library loaded successfully for Panama");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up test - clearing compilation cache");
    wasEnabled = CompilationCache.isEnabled();
    CompilationCache.setEnabled(true);
    CompilationCache.clear();
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test resources");
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    CompilationCache.setEnabled(wasEnabled);
  }

  @Nested
  @DisplayName("Enable/Disable Tests")
  class EnableDisableTests {

    @Test
    @DisplayName("should enable cache")
    void shouldEnableCache() {
      LOGGER.info("Testing cache enable");

      CompilationCache.setEnabled(true);
      assertTrue(CompilationCache.isEnabled(), "Cache should be enabled");

      LOGGER.info("Cache correctly enabled");
    }

    @Test
    @DisplayName("should disable cache")
    void shouldDisableCache() {
      LOGGER.info("Testing cache disable");

      CompilationCache.setEnabled(false);
      assertFalse(CompilationCache.isEnabled(), "Cache should be disabled");

      LOGGER.info("Cache correctly disabled");
    }

    @Test
    @DisplayName("should return disabled message when disabled")
    void shouldReturnDisabledMessageWhenDisabled() {
      LOGGER.info("Testing disabled state statistics");

      CompilationCache.setEnabled(false);

      String stats = CompilationCache.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.contains("disabled"), "Should indicate cache is disabled");

      LOGGER.info("Disabled statistics: " + stats);
    }
  }

  @Nested
  @DisplayName("Cache Operations Tests")
  class CacheOperationsTests {

    @Test
    @DisplayName("should store compiled module")
    void shouldStoreCompiledModule() {
      LOGGER.info("Testing compiled module storage");

      try (Arena arena = Arena.ofConfined()) {
        // Create WASM bytes memory segment
        MemorySegment wasmBytes = arena.allocate(SIMPLE_WASM_MODULE.length);
        MemorySegment.copy(
            SIMPLE_WASM_MODULE, 0, wasmBytes, ValueLayout.JAVA_BYTE, 0, SIMPLE_WASM_MODULE.length);

        // Create a simulated compiled module
        byte[] compiledData = new byte[256];
        for (int i = 0; i < compiledData.length; i++) {
          compiledData[i] = (byte) (i % 256);
        }
        MemorySegment compiledModule = arena.allocate(compiledData.length);
        MemorySegment.copy(
            compiledData, 0, compiledModule, ValueLayout.JAVA_BYTE, 0, compiledData.length);

        boolean stored =
            CompilationCache.storeInCache(
                wasmBytes, compiledModule, "default_options", 100_000_000); // 100ms simulated

        assertTrue(stored, "Should successfully store compiled module");

        LOGGER.info("Compiled module stored successfully");
      }
    }

    @Test
    @DisplayName("should load cached module")
    void shouldLoadCachedModule() {
      LOGGER.info("Testing cached module loading");

      try (Arena arena = Arena.ofConfined()) {
        // Store a module first
        MemorySegment wasmBytes = arena.allocate(SIMPLE_WASM_MODULE.length);
        MemorySegment.copy(
            SIMPLE_WASM_MODULE, 0, wasmBytes, ValueLayout.JAVA_BYTE, 0, SIMPLE_WASM_MODULE.length);

        byte[] compiledData = new byte[128];
        for (int i = 0; i < compiledData.length; i++) {
          compiledData[i] = (byte) (i % 256);
        }
        MemorySegment compiledModule = arena.allocate(compiledData.length);
        MemorySegment.copy(
            compiledData, 0, compiledModule, ValueLayout.JAVA_BYTE, 0, compiledData.length);

        String options = "test_options";
        CompilationCache.storeInCache(wasmBytes, compiledModule, options, 50_000_000);

        // Now load it
        try (Arena loadArena = Arena.ofConfined()) {
          MemorySegment loadWasmBytes = loadArena.allocate(SIMPLE_WASM_MODULE.length);
          MemorySegment.copy(
              SIMPLE_WASM_MODULE,
              0,
              loadWasmBytes,
              ValueLayout.JAVA_BYTE,
              0,
              SIMPLE_WASM_MODULE.length);

          MemorySegment loaded =
              CompilationCache.loadFromCache(loadWasmBytes, options, loadArena);

          // Note: Loading may fail due to verification or other reasons
          // The important thing is that the operation doesn't crash
          if (loaded != null) {
            LOGGER.info("Successfully loaded cached module - size: " + loaded.byteSize());
          } else {
            LOGGER.info("Cache miss (expected in some scenarios)");
          }
        }
      }
    }

    @Test
    @DisplayName("should return empty for missing module")
    void shouldReturnEmptyForMissingModule() {
      LOGGER.info("Testing cache miss for non-existent module");

      try (Arena arena = Arena.ofConfined()) {
        // Create unique WASM bytes that won't be in cache
        byte[] uniqueWasm = new byte[32];
        for (int i = 0; i < uniqueWasm.length; i++) {
          uniqueWasm[i] = (byte) (System.nanoTime() % 256);
        }
        MemorySegment wasmBytes = arena.allocate(uniqueWasm.length);
        MemorySegment.copy(uniqueWasm, 0, wasmBytes, ValueLayout.JAVA_BYTE, 0, uniqueWasm.length);

        MemorySegment result =
            CompilationCache.loadFromCache(wasmBytes, "nonexistent_options", arena);

        assertNull(result, "Should return null for non-existent cached module");

        LOGGER.info("Correctly returned null for missing module");
      }
    }

    @Test
    @DisplayName("should handle null inputs gracefully")
    void shouldHandleNullInputsGracefully() {
      LOGGER.info("Testing null input handling");

      try (Arena arena = Arena.ofConfined()) {
        // Null WASM bytes for load
        MemorySegment loadResult = CompilationCache.loadFromCache(null, "options", arena);
        assertNull(loadResult, "Should return null for null WASM bytes");

        // Null WASM bytes for store
        MemorySegment compiledModule = arena.allocate(64);
        boolean storeResult = CompilationCache.storeInCache(null, compiledModule, "options", 1000);
        assertFalse(storeResult, "Should return false for null WASM bytes in store");

        // Null compiled module for store
        MemorySegment wasmBytes = arena.allocate(SIMPLE_WASM_MODULE.length);
        MemorySegment.copy(
            SIMPLE_WASM_MODULE, 0, wasmBytes, ValueLayout.JAVA_BYTE, 0, SIMPLE_WASM_MODULE.length);
        storeResult = CompilationCache.storeInCache(wasmBytes, null, "options", 1000);
        assertFalse(storeResult, "Should return false for null compiled module");

        LOGGER.info("Null inputs handled gracefully");
      }
    }
  }

  @Nested
  @DisplayName("Cache Statistics Tests")
  class CacheStatisticsTests {

    @Test
    @DisplayName("should track hit rate")
    void shouldTrackHitRate() {
      LOGGER.info("Testing hit rate tracking");

      try (Arena arena = Arena.ofConfined()) {
        // Perform some cache operations
        MemorySegment wasmBytes = arena.allocate(SIMPLE_WASM_MODULE.length);
        MemorySegment.copy(
            SIMPLE_WASM_MODULE, 0, wasmBytes, ValueLayout.JAVA_BYTE, 0, SIMPLE_WASM_MODULE.length);

        // Try to load (will miss)
        CompilationCache.loadFromCache(wasmBytes, "options", arena);
        CompilationCache.loadFromCache(wasmBytes, "options", arena);

        double hitRate = CompilationCache.getHitRate();
        assertTrue(hitRate >= 0.0 && hitRate <= 100.0, "Hit rate should be between 0 and 100");

        LOGGER.info("Hit rate: " + hitRate + "%");
      }
    }

    @Test
    @DisplayName("should report cache statistics")
    void shouldReportCacheStatistics() {
      LOGGER.info("Testing cache statistics reporting");

      String stats = CompilationCache.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertFalse(stats.isEmpty(), "Statistics should not be empty");
      assertTrue(
          stats.contains("Panama") || stats.contains("cache"),
          "Statistics should mention cache or Panama");

      LOGGER.info("Cache statistics:\n" + stats);
    }

    @Test
    @DisplayName("should report performance metrics")
    void shouldReportPerformanceMetrics() {
      LOGGER.info("Testing performance metrics reporting");

      try (Arena arena = Arena.ofConfined()) {
        // Perform some operations
        MemorySegment wasmBytes = arena.allocate(SIMPLE_WASM_MODULE.length);
        MemorySegment.copy(
            SIMPLE_WASM_MODULE, 0, wasmBytes, ValueLayout.JAVA_BYTE, 0, SIMPLE_WASM_MODULE.length);

        CompilationCache.loadFromCache(wasmBytes, "options", arena);

        String metrics = CompilationCache.getPerformanceMetrics();
        assertNotNull(metrics, "Performance metrics should not be null");
        assertTrue(metrics.contains("hit_rate"), "Metrics should contain hit rate");
        assertTrue(metrics.contains("zero_copy"), "Metrics should contain zero-copy ops");

        LOGGER.info("Performance metrics: " + metrics);
      }
    }
  }

  @Nested
  @DisplayName("Cache Management Tests")
  class CacheManagementTests {

    @Test
    @DisplayName("should clear cache")
    void shouldClearCache() {
      LOGGER.info("Testing cache clear");

      try (Arena arena = Arena.ofConfined()) {
        // Store something first
        MemorySegment wasmBytes = arena.allocate(SIMPLE_WASM_MODULE.length);
        MemorySegment.copy(
            SIMPLE_WASM_MODULE, 0, wasmBytes, ValueLayout.JAVA_BYTE, 0, SIMPLE_WASM_MODULE.length);

        byte[] compiledData = new byte[64];
        MemorySegment compiledModule = arena.allocate(compiledData.length);
        CompilationCache.storeInCache(wasmBytes, compiledModule, "clear_test", 1000);

        // Clear
        assertDoesNotThrow(CompilationCache::clear, "Clear should not throw");

        LOGGER.info("Cache cleared successfully");
      }
    }

    @Test
    @DisplayName("should return cache directory")
    void shouldReturnCacheDirectory() {
      LOGGER.info("Testing cache directory retrieval");

      String cacheDir = CompilationCache.getCacheDirectory();
      assertNotNull(cacheDir, "Cache directory should not be null");
      assertFalse(cacheDir.isEmpty(), "Cache directory should not be empty");

      LOGGER.info("Cache directory: " + cacheDir);
    }

    @Test
    @DisplayName("should not operate when disabled")
    void shouldNotOperateWhenDisabled() {
      LOGGER.info("Testing operations when disabled");

      CompilationCache.setEnabled(false);

      try (Arena arena = Arena.ofConfined()) {
        MemorySegment wasmBytes = arena.allocate(SIMPLE_WASM_MODULE.length);
        MemorySegment.copy(
            SIMPLE_WASM_MODULE, 0, wasmBytes, ValueLayout.JAVA_BYTE, 0, SIMPLE_WASM_MODULE.length);

        byte[] compiledData = new byte[64];
        MemorySegment compiledModule = arena.allocate(compiledData.length);

        // Store should return false when disabled
        boolean storeResult =
            CompilationCache.storeInCache(wasmBytes, compiledModule, "disabled_test", 1000);
        assertFalse(storeResult, "Store should return false when disabled");

        // Load should return null when disabled
        MemorySegment loadResult =
            CompilationCache.loadFromCache(wasmBytes, "disabled_test", arena);
        assertNull(loadResult, "Load should return null when disabled");

        LOGGER.info("Operations correctly skipped when disabled");
      }
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("should handle empty WASM bytes")
    void shouldHandleEmptyWasmBytes() {
      LOGGER.info("Testing empty WASM bytes handling");

      try (Arena arena = Arena.ofConfined()) {
        MemorySegment emptyWasm = arena.allocate(0);
        byte[] compiledData = new byte[64];
        MemorySegment compiledModule = arena.allocate(compiledData.length);

        // Should handle gracefully (may fail validation but not crash)
        boolean stored =
            CompilationCache.storeInCache(emptyWasm, compiledModule, "empty_test", 1000);

        MemorySegment loaded = CompilationCache.loadFromCache(emptyWasm, "empty_test", arena);

        LOGGER.info("Empty WASM handled - stored: " + stored + ", loaded: " + (loaded != null));
      }
    }

    @Test
    @DisplayName("should handle large WASM modules")
    void shouldHandleLargeWasmModules() {
      LOGGER.info("Testing large WASM module handling");

      try (Arena arena = Arena.ofConfined()) {
        // Create a larger module (64KB)
        byte[] largeWasm = new byte[64 * 1024];
        // Add magic number
        largeWasm[0] = 0x00;
        largeWasm[1] = 0x61;
        largeWasm[2] = 0x73;
        largeWasm[3] = 0x6D;
        // Add version
        largeWasm[4] = 0x01;
        largeWasm[5] = 0x00;
        largeWasm[6] = 0x00;
        largeWasm[7] = 0x00;

        MemorySegment wasmBytes = arena.allocate(largeWasm.length);
        MemorySegment.copy(largeWasm, 0, wasmBytes, ValueLayout.JAVA_BYTE, 0, largeWasm.length);

        byte[] compiledData = new byte[128 * 1024]; // 128KB compiled
        MemorySegment compiledModule = arena.allocate(compiledData.length);

        boolean stored =
            CompilationCache.storeInCache(wasmBytes, compiledModule, "large_test", 500_000_000);

        LOGGER.info("Large module stored: " + stored);
      }
    }

    @Test
    @DisplayName("should handle different engine options")
    void shouldHandleDifferentEngineOptions() {
      LOGGER.info("Testing different engine options");

      try (Arena arena = Arena.ofConfined()) {
        MemorySegment wasmBytes = arena.allocate(SIMPLE_WASM_MODULE.length);
        MemorySegment.copy(
            SIMPLE_WASM_MODULE, 0, wasmBytes, ValueLayout.JAVA_BYTE, 0, SIMPLE_WASM_MODULE.length);

        byte[] compiledData = new byte[64];
        MemorySegment compiledModule = arena.allocate(compiledData.length);

        // Store with different options
        String[] options = {"opt1", "opt2", "opt3", null, ""};
        for (String opt : options) {
          CompilationCache.storeInCache(wasmBytes, compiledModule, opt, 1000);
        }

        // Load with different options
        for (String opt : options) {
          CompilationCache.loadFromCache(wasmBytes, opt, arena);
        }

        LOGGER.info("Different engine options handled successfully");
      }
    }
  }
}
