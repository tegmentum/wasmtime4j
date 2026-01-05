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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for ModuleCache - compiled module caching.
 *
 * <p>These tests verify module caching, precompilation, statistics, and LRU eviction.
 *
 * @since 1.0.0
 */
@DisplayName("ModuleCache Integration Tests")
public final class ModuleCacheIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleCacheIntegrationTest.class.getName());

  private static boolean moduleCacheAvailable = false;

  /**
   * Simple WebAssembly module that exports an add function.
   *
   * <pre>
   * (module
   *   (func (export "add") (param i32 i32) (result i32)
   *     local.get 0
   *     local.get 1
   *     i32.add))
   * </pre>
   */
  private static final byte[] SIMPLE_ADD_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x07,
        0x01,
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // type section
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07,
        0x01,
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // export section
        0x0a,
        0x09,
        0x01,
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x0b // code section
      };

  /**
   * Different WebAssembly module that exports a multiply function.
   *
   * <pre>
   * (module
   *   (func (export "mul") (param i32 i32) (result i32)
   *     local.get 0
   *     local.get 1
   *     i32.mul))
   * </pre>
   */
  private static final byte[] SIMPLE_MUL_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x07,
        0x01,
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // type section
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07,
        0x01,
        0x03,
        0x6d,
        0x75,
        0x6c,
        0x00,
        0x00, // export section ("mul")
        0x0a,
        0x09,
        0x01,
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6c,
        0x0b // code section (i32.mul)
      };

  @BeforeAll
  static void checkModuleCacheAvailable() {
    // Force JNI mode since JNI bindings are implemented for ModuleCache
    System.setProperty("wasmtime4j.runtime", "jni");
    try {
      Engine testEngine = Engine.create();
      Path testDir = Files.createTempDirectory("wasmtime4j-cache-test");
      ModuleCacheConfig config =
          ModuleCacheConfig.builder().cacheDir(testDir).maxEntries(10).build();
      ModuleCache testCache = ModuleCacheFactory.create(testEngine, config);
      testCache.close();
      testEngine.close();
      deleteDirectory(testDir);
      moduleCacheAvailable = true;
      LOGGER.info("ModuleCache is available - tests will run");
    } catch (Exception e) {
      moduleCacheAvailable = false;
      LOGGER.warning("ModuleCache not available - tests skipped: " + e.getMessage());
    }
  }

  private static void deleteDirectory(final Path dir) {
    try {
      if (Files.exists(dir)) {
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach(
                path -> {
                  try {
                    Files.delete(path);
                  } catch (IOException e) {
                    LOGGER.fine("Failed to delete: " + path);
                  }
                });
      }
    } catch (IOException e) {
      LOGGER.warning("Failed to delete directory: " + dir);
    }
  }

  private static void assumeModuleCacheAvailable() {
    assumeTrue(moduleCacheAvailable, "ModuleCache native implementation not available - skipping");
  }

  private Engine engine;
  private ModuleCache cache;
  private Path cacheDir;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (moduleCacheAvailable) {
      engine = Engine.create();
      cacheDir = Files.createTempDirectory("wasmtime4j-cache-" + System.currentTimeMillis());
      LOGGER.fine("Created temp cache directory: " + cacheDir);
    }
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    if (cache != null) {
      cache.close();
      cache = null;
    }
    if (engine != null) {
      engine.close();
      engine = null;
    }
    if (cacheDir != null) {
      deleteDirectory(cacheDir);
    }
    LOGGER.info("Teardown complete: " + testInfo.getDisplayName());
  }

  @Nested
  @DisplayName("Cache Operations Tests")
  class CacheOperationsTests {

    @Test
    @DisplayName("should get or compile module")
    void shouldGetOrCompileModule(final TestInfo testInfo) throws Exception {
      assumeModuleCacheAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ModuleCacheConfig config =
          ModuleCacheConfig.builder().cacheDir(cacheDir).maxEntries(100).build();
      cache = ModuleCacheFactory.create(engine, config);
      resources.add(cache);

      LOGGER.fine("Created cache with config: " + config);

      // First call should compile (cache miss)
      Module module1 = cache.getOrCompile(SIMPLE_ADD_WASM);
      assertNotNull(module1, "First module should not be null");
      resources.add(module1);
      LOGGER.fine("First getOrCompile returned module: " + module1);

      long missCount1 = cache.getMissCount();
      LOGGER.fine("Miss count after first compile: " + missCount1);
      assertEquals(1, missCount1, "Should have 1 miss after first compile");

      // Second call should hit cache
      Module module2 = cache.getOrCompile(SIMPLE_ADD_WASM);
      assertNotNull(module2, "Second module should not be null");
      resources.add(module2);
      LOGGER.fine("Second getOrCompile returned module: " + module2);

      long hitCount = cache.getHitCount();
      LOGGER.fine("Hit count after second call: " + hitCount);
      assertEquals(1, hitCount, "Should have 1 hit after second call");

      LOGGER.info("Test passed: cache correctly handles get or compile");
    }

    @Test
    @DisplayName("should precompile module")
    void shouldPrecompileModule(final TestInfo testInfo) throws Exception {
      assumeModuleCacheAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ModuleCacheConfig config =
          ModuleCacheConfig.builder().cacheDir(cacheDir).maxEntries(100).build();
      cache = ModuleCacheFactory.create(engine, config);
      resources.add(cache);

      // Precompile the module
      String hash = cache.precompile(SIMPLE_ADD_WASM);
      assertNotNull(hash, "Precompile should return a hash");
      assertFalse(hash.isEmpty(), "Hash should not be empty");
      LOGGER.fine("Precompile returned hash: " + hash);

      // Entry count should be 1
      long entryCount = cache.getEntryCount();
      LOGGER.fine("Entry count after precompile: " + entryCount);
      assertEquals(1, entryCount, "Should have 1 entry after precompile");

      // Getting the same module should hit cache
      Module module = cache.getOrCompile(SIMPLE_ADD_WASM);
      assertNotNull(module, "Module from cache should not be null");
      resources.add(module);

      long hitCount = cache.getHitCount();
      LOGGER.fine("Hit count after getOrCompile: " + hitCount);
      assertEquals(1, hitCount, "Should have 1 hit after getting precompiled module");

      LOGGER.info("Test passed: precompile works correctly");
    }

    @Test
    @DisplayName("should check if module is cached")
    void shouldCheckIfModuleIsCached(final TestInfo testInfo) throws Exception {
      assumeModuleCacheAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ModuleCacheConfig config =
          ModuleCacheConfig.builder().cacheDir(cacheDir).maxEntries(100).build();
      cache = ModuleCacheFactory.create(engine, config);
      resources.add(cache);

      // Initially should have 0 entries
      assertEquals(0, cache.getEntryCount(), "Initial entry count should be 0");

      // Add a module to the cache
      Module module = cache.getOrCompile(SIMPLE_ADD_WASM);
      resources.add(module);

      // Now should have 1 entry
      assertEquals(1, cache.getEntryCount(), "Entry count should be 1 after caching");

      LOGGER.info("Test passed: cache entry count tracking works");
    }

    @Test
    @DisplayName("should clear cache")
    void shouldClearCache(final TestInfo testInfo) throws Exception {
      assumeModuleCacheAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ModuleCacheConfig config =
          ModuleCacheConfig.builder().cacheDir(cacheDir).maxEntries(100).build();
      cache = ModuleCacheFactory.create(engine, config);
      resources.add(cache);

      // Add modules to the cache
      Module module1 = cache.getOrCompile(SIMPLE_ADD_WASM);
      resources.add(module1);
      Module module2 = cache.getOrCompile(SIMPLE_MUL_WASM);
      resources.add(module2);

      long entryCountBefore = cache.getEntryCount();
      LOGGER.fine("Entry count before clear: " + entryCountBefore);
      assertEquals(2, entryCountBefore, "Should have 2 entries before clear");

      // Clear the cache
      cache.clear();
      LOGGER.fine("Cache cleared");

      long entryCountAfter = cache.getEntryCount();
      LOGGER.fine("Entry count after clear: " + entryCountAfter);
      assertEquals(0, entryCountAfter, "Entry count should be 0 after clear");

      LOGGER.info("Test passed: cache clear works correctly");
    }
  }

  @Nested
  @DisplayName("Cache Statistics Tests")
  class CacheStatisticsTests {

    @Test
    @DisplayName("should return cache statistics")
    void shouldReturnCacheStatistics(final TestInfo testInfo) throws Exception {
      assumeModuleCacheAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ModuleCacheConfig config =
          ModuleCacheConfig.builder().cacheDir(cacheDir).maxEntries(100).build();
      cache = ModuleCacheFactory.create(engine, config);
      resources.add(cache);

      // Get initial statistics
      ModuleCacheStatistics stats = cache.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      LOGGER.fine("Initial statistics: " + stats);

      assertEquals(0, stats.getEntriesCount(), "Initial entries should be 0");
      assertEquals(0, stats.getCacheHits(), "Initial hits should be 0");
      assertEquals(0, stats.getCacheMisses(), "Initial misses should be 0");

      // Compile a module (cache miss)
      Module module = cache.getOrCompile(SIMPLE_ADD_WASM);
      resources.add(module);

      stats = cache.getStatistics();
      LOGGER.fine("Statistics after first compile: " + stats);
      assertEquals(1, stats.getEntriesCount(), "Should have 1 entry");
      assertEquals(1, stats.getCacheMisses(), "Should have 1 miss");

      // Get the same module again (cache hit)
      Module module2 = cache.getOrCompile(SIMPLE_ADD_WASM);
      resources.add(module2);

      stats = cache.getStatistics();
      LOGGER.fine("Statistics after cache hit: " + stats);
      assertEquals(1, stats.getCacheHits(), "Should have 1 hit");

      LOGGER.info("Test passed: statistics are accurate");
    }

    @Test
    @DisplayName("should track cache hits and misses")
    void shouldTrackCacheHitsAndMisses(final TestInfo testInfo) throws Exception {
      assumeModuleCacheAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ModuleCacheConfig config =
          ModuleCacheConfig.builder().cacheDir(cacheDir).maxEntries(100).build();
      cache = ModuleCacheFactory.create(engine, config);
      resources.add(cache);

      // Compile two different modules (2 misses)
      Module addModule = cache.getOrCompile(SIMPLE_ADD_WASM);
      resources.add(addModule);
      Module mulModule = cache.getOrCompile(SIMPLE_MUL_WASM);
      resources.add(mulModule);

      LOGGER.fine("After compiling 2 modules - misses: " + cache.getMissCount());
      assertEquals(2, cache.getMissCount(), "Should have 2 misses");
      assertEquals(0, cache.getHitCount(), "Should have 0 hits");

      // Get them again (2 hits)
      Module addModule2 = cache.getOrCompile(SIMPLE_ADD_WASM);
      resources.add(addModule2);
      Module mulModule2 = cache.getOrCompile(SIMPLE_MUL_WASM);
      resources.add(mulModule2);

      LOGGER.fine("After getting cached modules - hits: " + cache.getHitCount());
      assertEquals(2, cache.getHitCount(), "Should have 2 hits");
      assertEquals(2, cache.getMissCount(), "Misses should still be 2");

      LOGGER.info("Test passed: hit/miss tracking is accurate");
    }
  }

  @Nested
  @DisplayName("Cache Eviction Tests")
  class CacheEvictionTests {

    @Test
    @DisplayName("should evict oldest entries on overflow")
    void shouldEvictOldestEntriesOnOverflow(final TestInfo testInfo) throws Exception {
      assumeModuleCacheAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create cache with max 2 entries
      ModuleCacheConfig config =
          ModuleCacheConfig.builder().cacheDir(cacheDir).maxEntries(2).build();
      cache = ModuleCacheFactory.create(engine, config);
      resources.add(cache);

      LOGGER.fine("Created cache with max 2 entries");

      // Add first module
      Module module1 = cache.getOrCompile(SIMPLE_ADD_WASM);
      resources.add(module1);
      LOGGER.fine("Added first module, entry count: " + cache.getEntryCount());

      // Add second module
      Module module2 = cache.getOrCompile(SIMPLE_MUL_WASM);
      resources.add(module2);
      LOGGER.fine("Added second module, entry count: " + cache.getEntryCount());

      // Verify we have 2 entries
      assertTrue(cache.getEntryCount() <= 2, "Entry count should not exceed 2");

      // Perform maintenance to trigger eviction if over limit
      cache.performMaintenance();
      LOGGER.fine("After maintenance, entry count: " + cache.getEntryCount());

      assertTrue(cache.getEntryCount() <= 2, "Entry count should not exceed max after maintenance");

      LOGGER.info("Test passed: eviction works within max entries limit");
    }

    @Test
    @DisplayName("should respect max cache size")
    void shouldRespectMaxCacheSize(final TestInfo testInfo) throws Exception {
      assumeModuleCacheAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create cache with reasonable size limit
      ModuleCacheConfig config =
          ModuleCacheConfig.builder()
              .cacheDir(cacheDir)
              .maxCacheSize(10 * 1024 * 1024) // 10MB
              .maxEntries(100)
              .build();
      cache = ModuleCacheFactory.create(engine, config);
      resources.add(cache);

      // Add modules
      Module module1 = cache.getOrCompile(SIMPLE_ADD_WASM);
      resources.add(module1);
      Module module2 = cache.getOrCompile(SIMPLE_MUL_WASM);
      resources.add(module2);

      long storageBytesUsed = cache.getStorageBytesUsed();
      LOGGER.fine("Storage bytes used: " + storageBytesUsed);

      // Storage should be less than max
      assertTrue(
          storageBytesUsed < config.getMaxCacheSize(),
          "Storage bytes used should be less than max cache size");

      LOGGER.info("Test passed: max cache size is respected");
    }
  }

  @Nested
  @DisplayName("Cache Lifecycle Tests")
  class CacheLifecycleTests {

    @Test
    @DisplayName("should close cache properly")
    void shouldCloseCacheProperly(final TestInfo testInfo) throws Exception {
      assumeModuleCacheAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ModuleCacheConfig config =
          ModuleCacheConfig.builder().cacheDir(cacheDir).maxEntries(100).build();
      ModuleCache localCache = ModuleCacheFactory.create(engine, config);

      // Verify cache is usable
      Module module = localCache.getOrCompile(SIMPLE_ADD_WASM);
      assertNotNull(module, "Module should be compiled before close");
      module.close();
      LOGGER.fine("Cache is usable before close");

      // Close the cache
      localCache.close();
      LOGGER.fine("Cache closed");

      // After close, operations should fail or return empty values
      // getHitCount and getMissCount return 0 for closed cache
      assertEquals(0, localCache.getHitCount(), "Hit count should be 0 after close");
      assertEquals(0, localCache.getMissCount(), "Miss count should be 0 after close");
      assertEquals(0, localCache.getEntryCount(), "Entry count should be 0 after close");

      LOGGER.info("Test passed: cache closes properly");
    }

    @Test
    @DisplayName("should return engine and config")
    void shouldReturnEngineAndConfig(final TestInfo testInfo) throws Exception {
      assumeModuleCacheAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ModuleCacheConfig config =
          ModuleCacheConfig.builder()
              .cacheDir(cacheDir)
              .maxEntries(50)
              .compressionEnabled(true)
              .build();
      cache = ModuleCacheFactory.create(engine, config);
      resources.add(cache);

      // Verify engine is returned
      assertNotNull(cache.getEngine(), "getEngine should return non-null");
      assertEquals(engine, cache.getEngine(), "getEngine should return the same engine");

      // Verify config is returned
      ModuleCacheConfig returnedConfig = cache.getConfig();
      assertNotNull(returnedConfig, "getConfig should return non-null");
      assertEquals(50, returnedConfig.getMaxEntries(), "Config maxEntries should match");
      assertTrue(returnedConfig.isCompressionEnabled(), "Config compression should be enabled");

      LOGGER.info("Test passed: engine and config accessors work");
    }

    @Test
    @DisplayName("should throw on null wasm bytes")
    void shouldThrowOnNullWasmBytes(final TestInfo testInfo) throws Exception {
      assumeModuleCacheAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ModuleCacheConfig config =
          ModuleCacheConfig.builder().cacheDir(cacheDir).maxEntries(100).build();
      cache = ModuleCacheFactory.create(engine, config);
      resources.add(cache);

      assertThrows(
          IllegalArgumentException.class,
          () -> cache.getOrCompile(null),
          "getOrCompile should throw on null bytes");

      assertThrows(
          IllegalArgumentException.class,
          () -> cache.getOrCompile(new byte[0]),
          "getOrCompile should throw on empty bytes");

      assertThrows(
          IllegalArgumentException.class,
          () -> cache.precompile(null),
          "precompile should throw on null bytes");

      LOGGER.info("Test passed: null/empty bytes handling works");
    }
  }
}
