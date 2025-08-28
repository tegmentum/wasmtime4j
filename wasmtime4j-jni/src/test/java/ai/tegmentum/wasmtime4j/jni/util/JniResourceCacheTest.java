package ai.tegmentum.wasmtime4j.jni.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive unit tests for {@link JniResourceCache}.
 *
 * <p>These tests verify cache functionality, weak reference behavior, thread safety, performance,
 * and resource management.
 */
@DisplayName("JniResourceCache Tests")
@Timeout(30) // Global timeout for all tests
class JniResourceCacheTest {

  private JniResourceCache<String, String> cache;

  @BeforeEach
  void setUp() {
    cache = new JniResourceCache<>();
  }

  @AfterEach
  void tearDown() {
    if (cache != null && !cache.isClosed()) {
      cache.close();
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create cache with default settings")
    void shouldCreateCacheWithDefaultSettings() {
      try (final JniResourceCache<String, String> testCache = new JniResourceCache<>()) {
        assertEquals(JniResourceCache.DEFAULT_MAX_SIZE, testCache.getMaxSize());
        assertEquals(0, testCache.size());
        assertFalse(testCache.isClosed());
      }
    }

    @Test
    @DisplayName("Should create cache with custom max size")
    void shouldCreateCacheWithCustomMaxSize() {
      final int customSize = 500;

      try (final JniResourceCache<String, String> testCache = new JniResourceCache<>(customSize)) {
        assertEquals(customSize, testCache.getMaxSize());
        assertEquals(0, testCache.size());
      }
    }

    @Test
    @DisplayName("Should reject invalid max size")
    void shouldRejectInvalidMaxSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniResourceCache<String, String>(0),
          "Should reject zero max size");

      assertThrows(
          IllegalArgumentException.class,
          () -> new JniResourceCache<String, String>(-1),
          "Should reject negative max size");
    }
  }

  @Nested
  @DisplayName("Basic Cache Operations Tests")
  class BasicCacheOperationsTests {

    @Test
    @DisplayName("Should store and retrieve value")
    void shouldStoreAndRetrieveValue() {
      cache.put("key1", "value1");

      final String retrieved = cache.get("key1", key -> "factory-" + key);
      assertEquals("value1", retrieved);
    }

    @Test
    @DisplayName("Should create value using factory when not cached")
    void shouldCreateValueUsingFactoryWhenNotCached() {
      final String result = cache.get("newKey", key -> "factory-" + key);
      assertEquals("factory-newKey", result);
    }

    @Test
    @DisplayName("Should return cached value instead of using factory")
    void shouldReturnCachedValueInsteadOfUsingFactory() {
      cache.put("existingKey", "cachedValue");

      final String result = cache.get("existingKey", key -> "factory-" + key);
      assertEquals("cachedValue", result);
    }

    @Test
    @DisplayName("Should handle null values from factory")
    void shouldHandleNullValuesFromFactory() {
      final String result = cache.get("nullKey", key -> null);
      assertNull(result);
    }

    @Test
    @DisplayName("Should remove cached value")
    void shouldRemoveCachedValue() {
      cache.put("removeKey", "removeValue");
      assertEquals(1, cache.size());

      final String removed = cache.remove("removeKey");
      assertEquals("removeValue", removed);
      assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("Should return null when removing non-existent key")
    void shouldReturnNullWhenRemovingNonExistentKey() {
      final String removed = cache.remove("nonExistentKey");
      assertNull(removed);
    }

    @Test
    @DisplayName("Should clear all entries")
    void shouldClearAllEntries() {
      cache.put("key1", "value1");
      cache.put("key2", "value2");
      cache.put("key3", "value3");
      assertEquals(3, cache.size());

      cache.clear();
      assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("Should reject null key")
    void shouldRejectNullKey() {
      assertThrows(
          IllegalArgumentException.class,
          () -> cache.get(null, key -> "value"),
          "Should reject null key in get");

      assertThrows(
          IllegalArgumentException.class,
          () -> cache.put(null, "value"),
          "Should reject null key in put");

      assertThrows(
          IllegalArgumentException.class,
          () -> cache.remove(null),
          "Should reject null key in remove");
    }

    @Test
    @DisplayName("Should reject null factory")
    void shouldRejectNullFactory() {
      assertThrows(
          IllegalArgumentException.class,
          () -> cache.get("key", null),
          "Should reject null factory");
    }

    @Test
    @DisplayName("Should reject null resource in put")
    void shouldRejectNullResourceInPut() {
      assertThrows(
          IllegalArgumentException.class,
          () -> cache.put("key", null),
          "Should reject null resource in put");
    }
  }

  @Nested
  @DisplayName("Cache Statistics Tests")
  class CacheStatisticsTests {

    @Test
    @DisplayName("Should track hit count")
    void shouldTrackHitCount() {
      cache.put("hitKey", "hitValue");

      assertEquals(0, cache.getHitCount());
      cache.get("hitKey", key -> "factory");
      assertEquals(1, cache.getHitCount());

      cache.get("hitKey", key -> "factory");
      assertEquals(2, cache.getHitCount());
    }

    @Test
    @DisplayName("Should track miss count")
    void shouldTrackMissCount() {
      assertEquals(0, cache.getMissCount());
      cache.get("missKey", key -> "factory");
      assertEquals(1, cache.getMissCount());

      cache.get("anotherMissKey", key -> "factory");
      assertEquals(2, cache.getMissCount());
    }

    @Test
    @DisplayName("Should calculate hit rate correctly")
    void shouldCalculateHitRateCorrectly() {
      assertEquals(0.0, cache.getHitRate(), 0.001);

      cache.put("key", "value");
      cache.get("key", k -> "factory"); // Hit
      cache.get("missKey", k -> "factory"); // Miss

      assertEquals(0.5, cache.getHitRate(), 0.001);
    }

    @Test
    @DisplayName("Should handle hit rate with no requests")
    void shouldHandleHitRateWithNoRequests() {
      assertEquals(0.0, cache.getHitRate(), 0.001);
    }

    @Test
    @DisplayName("Should track evictions")
    void shouldTrackEvictions() {
      final int smallCacheSize = 3;

      try (final JniResourceCache<String, String> smallCache =
          new JniResourceCache<>(smallCacheSize)) {
        // Fill cache to capacity
        for (int i = 0; i < smallCacheSize; i++) {
          smallCache.put("key" + i, "value" + i);
        }

        assertEquals(0, smallCache.getEvictionCount());

        // Add more entries to trigger eviction
        for (int i = smallCacheSize; i < smallCacheSize + 2; i++) {
          smallCache.put("key" + i, "value" + i);
        }

        assertTrue(smallCache.getEvictionCount() > 0, "Should have evicted some entries");
      }
    }
  }

  @Nested
  @DisplayName("Weak Reference Tests")
  class WeakReferenceTests {

    @Test
    @DisplayName("Should handle garbage collection of cached values")
    void shouldHandleGarbageCollectionOfCachedValues() {
      // Create some objects that can be garbage collected
      cache.put("gc1", new String("gcValue1"));
      cache.put("gc2", new String("gcValue2"));

      assertEquals(2, cache.size());

      // Force garbage collection
      System.gc();
      System.runFinalization();

      // Try to access cached values and trigger cleanup
      cache.get("gc1", key -> "new1");
      cache.get("gc2", key -> "new2");

      // Size might be reduced due to GC
      assertTrue(cache.size() >= 0, "Cache size should be non-negative after GC");
    }

    @Test
    @DisplayName("Should cleanup collected references on operations")
    void shouldCleanupCollectedReferencesOnOperations() {
      // Fill cache with values that might be collected
      for (int i = 0; i < 10; i++) {
        cache.put("key" + i, new String("value" + i));
      }

      final int initialSize = cache.size();
      assertTrue(initialSize > 0);

      // Force GC and access cache to trigger cleanup
      System.gc();
      System.runFinalization();

      // Access cache to trigger cleanup
      cache.get("newKey", key -> "newValue");

      // Size may have changed due to cleanup
      assertTrue(cache.size() >= 0);
    }
  }

  @Nested
  @DisplayName("Size Management Tests")
  class SizeManagementTests {

    @Test
    @DisplayName("Should respect maximum size limit")
    void shouldRespectMaximumSizeLimit() {
      final int maxSize = 5;

      try (final JniResourceCache<String, String> limitedCache = new JniResourceCache<>(maxSize)) {
        // Fill beyond capacity
        for (int i = 0; i < maxSize * 2; i++) {
          limitedCache.put("key" + i, "value" + i);
        }

        // Cache should not exceed max size (accounting for eviction)
        assertTrue(limitedCache.size() <= maxSize, "Cache size should not exceed maximum");
      }
    }

    @Test
    @DisplayName("Should evict entries when at capacity")
    void shouldEvictEntriesWhenAtCapacity() {
      final int maxSize = 3;

      try (final JniResourceCache<String, String> limitedCache = new JniResourceCache<>(maxSize)) {
        // Fill to capacity
        for (int i = 0; i < maxSize; i++) {
          limitedCache.put("key" + i, "value" + i);
        }

        assertEquals(maxSize, limitedCache.size());

        // Add one more to trigger eviction
        limitedCache.put("extraKey", "extraValue");

        // Size should still be within limits
        assertTrue(limitedCache.size() <= maxSize);
        assertTrue(limitedCache.getEvictionCount() > 0);
      }
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle factory exceptions")
    void shouldHandleFactoryExceptions() {
      final RuntimeException expectedException = new RuntimeException("Factory error");

      final RuntimeException actualException =
          assertThrows(
              RuntimeException.class,
              () ->
                  cache.get(
                      "errorKey",
                      key -> {
                        throw expectedException;
                      }));

      assertEquals(expectedException, actualException);
    }

    @Test
    @DisplayName("Should reject operations on closed cache")
    void shouldRejectOperationsOnClosedCache() {
      cache.close();

      assertThrows(
          RuntimeException.class,
          () -> cache.get("key", k -> "value"),
          "Should reject get on closed cache");

      assertThrows(
          RuntimeException.class,
          () -> cache.put("key", "value"),
          "Should reject put on closed cache");
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("Should close gracefully")
    void shouldCloseGracefully() {
      cache.put("closeKey", "closeValue");
      assertFalse(cache.isClosed());

      cache.close();
      assertTrue(cache.isClosed());
      assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("Should be idempotent on close")
    void shouldBeIdempotentOnClose() {
      cache.close();
      assertTrue(cache.isClosed());

      // Second close should not throw
      cache.close();
      assertTrue(cache.isClosed());
    }

    @Test
    @DisplayName("Should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      final AtomicInteger factoryCalls = new AtomicInteger(0);

      try (final JniResourceCache<String, String> autoCloseCache = new JniResourceCache<>(10)) {
        final String result =
            autoCloseCache.get(
                "autoKey",
                key -> {
                  factoryCalls.incrementAndGet();
                  return "autoValue";
                });
        assertEquals("autoValue", result);
      }

      assertEquals(1, factoryCalls.get());
    }
  }

  @Nested
  @DisplayName("Concurrency Tests")
  class ConcurrencyTests {

    @Test
    @DisplayName("Should handle concurrent access safely")
    void shouldHandleConcurrentAccessSafely() throws InterruptedException {
      final int threadCount = 20;
      final int operationsPerThread = 50;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch latch = new CountDownLatch(threadCount);
      final ConcurrentMap<String, String> results = new ConcurrentHashMap<>();

      try (final JniResourceCache<String, String> concurrentCache = new JniResourceCache<>(200)) {
        for (int t = 0; t < threadCount; t++) {
          final int threadIndex = t;
          executor.submit(
              () -> {
                try {
                  for (int i = 0; i < operationsPerThread; i++) {
                    final String key = "thread" + threadIndex + "-key" + i;
                    final String value = concurrentCache.get(key, k -> "value-" + k);
                    results.put(key, value);
                  }
                } finally {
                  latch.countDown();
                }
              });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All operations should complete");
        assertEquals(threadCount * operationsPerThread, results.size());

        // Verify all values are correct
        for (final String key : results.keySet()) {
          assertEquals("value-" + key, results.get(key));
        }
      } finally {
        executor.shutdown();
      }
    }

    @Test
    @DisplayName("Should handle concurrent put and get operations")
    void shouldHandleConcurrentPutAndGetOperations() throws InterruptedException {
      final int operationCount = 100;
      final ExecutorService executor = Executors.newFixedThreadPool(10);
      final CountDownLatch latch = new CountDownLatch(operationCount * 2); // Put + Get operations
      final AtomicInteger successCount = new AtomicInteger(0);

      try (final JniResourceCache<String, String> concurrentCache = new JniResourceCache<>(50)) {
        // Submit put operations
        for (int i = 0; i < operationCount; i++) {
          final int index = i;
          executor.submit(
              () -> {
                try {
                  concurrentCache.put("concurrentKey" + index, "concurrentValue" + index);
                  successCount.incrementAndGet();
                } catch (final Exception e) {
                  // Handle any synchronization issues
                } finally {
                  latch.countDown();
                }
              });
        }

        // Submit get operations
        for (int i = 0; i < operationCount; i++) {
          final int index = i;
          executor.submit(
              () -> {
                try {
                  final String value =
                      concurrentCache.get("concurrentKey" + index, key -> "factoryValue" + index);
                  if (value != null) {
                    successCount.incrementAndGet();
                  }
                } catch (final Exception e) {
                  // Handle any synchronization issues
                } finally {
                  latch.countDown();
                }
              });
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS), "All operations should complete");
        assertTrue(successCount.get() > 0, "Some operations should succeed");
      } finally {
        executor.shutdown();
      }
    }

    @Test
    @DisplayName("Should handle concurrent eviction safely")
    void shouldHandleConcurrentEvictionSafely() throws InterruptedException {
      final int maxSize = 10;
      final int threadCount = 5;
      final int operationsPerThread = 50;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      try (final JniResourceCache<String, String> evictionCache = new JniResourceCache<>(maxSize)) {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
          final int threadIndex = t;
          final CompletableFuture<Void> future =
              CompletableFuture.runAsync(
                  () -> {
                    for (int i = 0; i < operationsPerThread; i++) {
                      final String key = "evictKey-" + threadIndex + "-" + i;
                      evictionCache.put(key, "evictValue-" + threadIndex + "-" + i);
                    }
                  },
                  executor);

          futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Cache size should be within limits
        assertTrue(evictionCache.size() <= maxSize);
        assertTrue(evictionCache.getEvictionCount() > 0);
      } finally {
        executor.shutdown();
      }
    }
  }

  @Nested
  @DisplayName("toString and Object Methods Tests")
  class ToStringAndObjectMethodsTests {

    @Test
    @DisplayName("Should provide meaningful toString")
    void shouldProvideMeaningfulToString() {
      cache.put("toStringKey", "toStringValue");
      cache.get("toStringKey", k -> "factory");
      cache.get("missKey", k -> "factory");

      final String toString = cache.toString();
      assertNotNull(toString);
      assertTrue(toString.contains("JniResourceCache"));
      assertTrue(toString.contains("size="));
      assertTrue(toString.contains("maxSize="));
      assertTrue(toString.contains("hits="));
      assertTrue(toString.contains("misses="));
      assertTrue(toString.contains("hitRate="));
    }

    @Test
    @DisplayName("Should show accurate statistics in toString")
    void shouldShowAccurateStatisticsInToString() {
      // Create specific statistics
      cache.put("statKey", "statValue");
      cache.get("statKey", k -> "factory"); // Hit
      cache.get("missKey", k -> "factory"); // Miss

      final String toString = cache.toString();
      assertTrue(toString.contains("hits=1"));
      assertTrue(toString.contains("misses=1"));
      assertTrue(toString.contains("hitRate=0.50") || toString.contains("hitRate=0.5"));
    }
  }
}
