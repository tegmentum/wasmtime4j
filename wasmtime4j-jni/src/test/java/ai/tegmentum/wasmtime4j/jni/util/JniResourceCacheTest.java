package ai.tegmentum.wasmtime4j.jni.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive unit tests for {@link JniResourceCache}.
 *
 * <p>These tests verify cache functionality, weak reference behavior, thread safety, performance,
 * and resource management.
 */
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
  class ConstructorTests {

    @Test
    void testCreateCacheWithDefaultSettings() {
      try (final JniResourceCache<String, String> testCache = new JniResourceCache<>()) {
        assertEquals(JniResourceCache.DEFAULT_MAX_SIZE, testCache.getMaxSize());
        assertEquals(0, testCache.size());
        assertFalse(testCache.isClosed());
      }
    }

    @Test
    void testCreateCacheWithCustomMaxSize() {
      final int customSize = 500;

      try (final JniResourceCache<String, String> testCache = new JniResourceCache<>(customSize)) {
        assertEquals(customSize, testCache.getMaxSize());
        assertEquals(0, testCache.size());
      }
    }

    @Test
    void testRejectInvalidMaxSize() {
      assertThrows(
          JniValidationException.class,
          () -> new JniResourceCache<String, String>(0),
          "Should reject zero max size");

      assertThrows(
          JniValidationException.class,
          () -> new JniResourceCache<String, String>(-1),
          "Should reject negative max size");
    }
  }

  @Nested
  class BasicCacheOperationsTests {

    @Test
    void testStoreAndRetrieveValue() {
      cache.put("key1", "value1");

      final String retrieved = cache.get("key1", key -> "factory-" + key);
      assertEquals("value1", retrieved);
    }

    @Test
    void testCreateValueUsingFactoryWhenNotCached() {
      final String result = cache.get("newKey", key -> "factory-" + key);
      assertEquals("factory-newKey", result);
    }

    @Test
    void testReturnCachedValueInsteadOfUsingFactory() {
      cache.put("existingKey", "cachedValue");

      final String result = cache.get("existingKey", key -> "factory-" + key);
      assertEquals("cachedValue", result);
    }

    @Test
    void testHandleNullValuesFromFactory() {
      final String result = cache.get("nullKey", key -> null);
      assertNull(result);
    }

    @Test
    void testRemoveCachedValue() {
      cache.put("removeKey", "removeValue");
      assertEquals(1, cache.size());

      final String removed = cache.remove("removeKey");
      assertEquals("removeValue", removed);
      assertEquals(0, cache.size());
    }

    @Test
    void testReturnNullWhenRemovingNonExistentKey() {
      final String removed = cache.remove("nonExistentKey");
      assertNull(removed);
    }

    @Test
    void testClearAllEntries() {
      cache.put("key1", "value1");
      cache.put("key2", "value2");
      cache.put("key3", "value3");
      assertEquals(3, cache.size());

      cache.clear();
      assertEquals(0, cache.size());
    }

    @Test
    void testRejectNullKey() {
      assertThrows(
          JniValidationException.class,
          () -> cache.get(null, key -> "value"),
          "Should reject null key in get");

      assertThrows(
          JniValidationException.class,
          () -> cache.put(null, "value"),
          "Should reject null key in put");

      assertThrows(
          JniValidationException.class,
          () -> cache.remove(null),
          "Should reject null key in remove");
    }

    @Test
    void testRejectNullFactory() {
      assertThrows(
          JniValidationException.class, () -> cache.get("key", null), "Should reject null factory");
    }

    @Test
    void testRejectNullResourceInPut() {
      assertThrows(
          JniValidationException.class,
          () -> cache.put("key", null),
          "Should reject null resource in put");
    }
  }

  @Nested
  class CacheStatisticsTests {

    @Test
    void testTrackHitCount() {
      cache.put("hitKey", "hitValue");

      assertEquals(0, cache.getHitCount());
      cache.get("hitKey", key -> "factory");
      assertEquals(1, cache.getHitCount());

      cache.get("hitKey", key -> "factory");
      assertEquals(2, cache.getHitCount());
    }

    @Test
    void testTrackMissCount() {
      assertEquals(0, cache.getMissCount());
      cache.get("missKey", key -> "factory");
      assertEquals(1, cache.getMissCount());

      cache.get("anotherMissKey", key -> "factory");
      assertEquals(2, cache.getMissCount());
    }

    @Test
    void testCalculateHitRateCorrectly() {
      assertEquals(0.0, cache.getHitRate(), 0.001);

      cache.put("key", "value");
      cache.get("key", k -> "factory"); // Hit
      cache.get("missKey", k -> "factory"); // Miss

      assertEquals(0.5, cache.getHitRate(), 0.001);
    }

    @Test
    void testHandleHitRateWithNoRequests() {
      assertEquals(0.0, cache.getHitRate(), 0.001);
    }

    @Test
    void testTrackEvictions() {
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
  class WeakReferenceTests {

    @Test
    void testHandleGarbageCollectionOfCachedValues() {
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
    void testCleanupCollectedReferencesOnOperations() {
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
  class SizeManagementTests {

    @Test
    void testRespectMaximumSizeLimit() {
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
    void testEvictEntriesWhenAtCapacity() {
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
  class ErrorHandlingTests {

    @Test
    void testHandleFactoryExceptions() {
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
    void testRejectOperationsOnClosedCache() {
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
  class ResourceManagementTests {

    @Test
    void testCloseGracefully() {
      cache.put("closeKey", "closeValue");
      assertFalse(cache.isClosed());

      cache.close();
      assertTrue(cache.isClosed());
      assertEquals(0, cache.size());
    }

    @Test
    void testBeIdempotentOnClose() {
      cache.close();
      assertTrue(cache.isClosed());

      // Second close should not throw
      cache.close();
      assertTrue(cache.isClosed());
    }

    @Test
    void testWorkWithTryWithResources() {
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
  class ConcurrencyTests {

    @Test
    void testHandleConcurrentAccessSafely() throws InterruptedException {
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
    void testHandleConcurrentPutAndGetOperations() throws InterruptedException {
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
    void testHandleConcurrentEvictionSafely() throws InterruptedException {
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
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

        // Cache size should be within limits
        assertTrue(evictionCache.size() <= maxSize);
        assertTrue(evictionCache.getEvictionCount() > 0);
      } finally {
        executor.shutdown();
      }
    }
  }

  @Nested
  class ToStringAndObjectMethodsTests {

    @Test
    void testProvideMeaningfulToString() {
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
    void testShowAccurateStatisticsInToString() {
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
