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
package ai.tegmentum.wasmtime4j.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the CacheStore interface and EngineConfig integration.
 *
 * <p>Validates the CacheStore API surface, EngineConfig wiring, and basic implementations.
 *
 * @since 1.0.0
 */
@DisplayName("CacheStore Tests")
class CacheStoreTest {

  private static final Logger LOGGER = Logger.getLogger(CacheStoreTest.class.getName());

  /** Simple in-memory CacheStore for testing. */
  private static CacheStore createInMemoryStore() {
    final ConcurrentHashMap<ByteBuffer, byte[]> cache = new ConcurrentHashMap<>();
    return new CacheStore() {
      @Override
      public byte[] get(final byte[] key) {
        return cache.get(ByteBuffer.wrap(key));
      }

      @Override
      public boolean insert(final byte[] key, final byte[] value) {
        cache.put(ByteBuffer.wrap(key), value.clone());
        return true;
      }
    };
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("get returns null for unknown key")
    void getReturnsNullForUnknownKey() {
      final CacheStore store = createInMemoryStore();
      final byte[] result = store.get(new byte[] {1, 2, 3});
      LOGGER.info("get() for unknown key returned: " + result);
      assertNull(result, "get() should return null for unknown key");
    }

    @Test
    @DisplayName("insert returns true on success")
    void insertReturnsTrueOnSuccess() {
      final CacheStore store = createInMemoryStore();
      final boolean result = store.insert(new byte[] {1, 2, 3}, new byte[] {4, 5, 6});
      LOGGER.info("insert() returned: " + result);
      assertTrue(result, "insert() should return true on success");
    }

    @Test
    @DisplayName("get returns previously inserted value")
    void getReturnsPreviouslyInsertedValue() {
      final CacheStore store = createInMemoryStore();
      final byte[] key = {1, 2, 3};
      final byte[] value = {4, 5, 6};

      store.insert(key, value);
      final byte[] result = store.get(key);

      LOGGER.info("get() after insert returned " + result.length + " bytes");
      assertNotNull(result, "get() should return non-null after insert");
      assertArrayEquals(value, result, "get() should return the inserted value");
    }

    @Test
    @DisplayName("insert overwrites existing value")
    void insertOverwritesExistingValue() {
      final CacheStore store = createInMemoryStore();
      final byte[] key = {1, 2, 3};
      final byte[] value1 = {4, 5, 6};
      final byte[] value2 = {7, 8, 9};

      store.insert(key, value1);
      store.insert(key, value2);
      final byte[] result = store.get(key);

      LOGGER.info("get() after overwrite returned: " + java.util.Arrays.toString(result));
      assertArrayEquals(value2, result, "get() should return the latest inserted value");
    }

    @Test
    @DisplayName("insert can return false to indicate failure")
    void insertCanReturnFalseToIndicateFailure() {
      final CacheStore store =
          new CacheStore() {
            @Override
            public byte[] get(final byte[] key) {
              return null;
            }

            @Override
            public boolean insert(final byte[] key, final byte[] value) {
              return false;
            }
          };

      final boolean result = store.insert(new byte[] {1}, new byte[] {2});
      LOGGER.info("insert() on failing store returned: " + result);
      assertEquals(false, result, "insert() should return false on failure");
    }
  }

  @Nested
  @DisplayName("EngineConfig Integration Tests")
  class EngineConfigIntegrationTests {

    @Test
    @DisplayName("getIncrementalCacheStore returns null by default")
    void getIncrementalCacheStoreReturnsNullByDefault() {
      final EngineConfig config = new EngineConfig();
      LOGGER.info("Default incrementalCacheStore: " + config.getIncrementalCacheStore());
      assertNull(config.getIncrementalCacheStore(), "Default should be null");
    }

    @Test
    @DisplayName("enableIncrementalCompilation stores the cache store")
    void enableIncrementalCompilationStoresTheCacheStore() {
      final CacheStore store = createInMemoryStore();
      final EngineConfig config = new EngineConfig();

      config.enableIncrementalCompilation(store);

      LOGGER.info(
          "After enableIncrementalCompilation: store is " + config.getIncrementalCacheStore());
      assertSame(store, config.getIncrementalCacheStore(), "Should return the same store instance");
    }

    @Test
    @DisplayName("enableIncrementalCompilation returns this for method chaining")
    void enableIncrementalCompilationReturnsThisForMethodChaining() {
      final CacheStore store = createInMemoryStore();
      final EngineConfig config = new EngineConfig();

      final EngineConfig result = config.enableIncrementalCompilation(store);

      LOGGER.info("Method chaining: same instance = " + (result == config));
      assertSame(config, result, "Should return same config instance for chaining");
    }

    @Test
    @DisplayName("enableIncrementalCompilation throws on null")
    void enableIncrementalCompilationThrowsOnNull() {
      final EngineConfig config = new EngineConfig();

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> config.enableIncrementalCompilation(null),
              "Should throw on null cacheStore");
      LOGGER.info("Null argument exception: " + ex.getMessage());
      assertTrue(ex.getMessage().contains("null"), "Message should mention null");
    }

    @Test
    @DisplayName("copy preserves the cache store")
    void copyPreservesTheCacheStore() {
      final CacheStore store = createInMemoryStore();
      final EngineConfig config = new EngineConfig();
      config.enableIncrementalCompilation(store);

      final EngineConfig copy = config.copy();

      LOGGER.info("Copy has same store: " + (copy.getIncrementalCacheStore() == store));
      assertSame(
          store,
          copy.getIncrementalCacheStore(),
          "copy() should preserve the cache store reference");
    }

    @Test
    @DisplayName("toJson does not include cache store")
    void toJsonDoesNotIncludeCacheStore() {
      final CacheStore store = createInMemoryStore();
      final EngineConfig config = new EngineConfig();
      config.enableIncrementalCompilation(store);

      final byte[] json = config.toJson();
      final String jsonStr = new String(json, java.nio.charset.StandardCharsets.UTF_8);

      LOGGER.info("JSON output length: " + jsonStr.length());
      assertEquals(
          false,
          jsonStr.contains("incrementalCacheStore"),
          "toJson() should not include transient CacheStore field");
      assertEquals(
          false,
          jsonStr.contains("cacheStore"),
          "toJson() should not include any cache store reference");
    }

    @Test
    @DisplayName("enableIncrementalCompilation can be combined with other config")
    void enableIncrementalCompilationCanBeCombinedWithOtherConfig() {
      final CacheStore store = createInMemoryStore();
      final EngineConfig config =
          new EngineConfig()
              .debugInfo(true)
              .consumeFuel(true)
              .enableIncrementalCompilation(store)
              .optimizationLevel(OptimizationLevel.SPEED);

      LOGGER.info(
          "Combined config: debugInfo=true, consumeFuel=true, store="
              + config.getIncrementalCacheStore()
              + ", optLevel="
              + config.getOptimizationLevel());
      assertSame(store, config.getIncrementalCacheStore());
      assertEquals(OptimizationLevel.SPEED, config.getOptimizationLevel());
    }
  }
}
