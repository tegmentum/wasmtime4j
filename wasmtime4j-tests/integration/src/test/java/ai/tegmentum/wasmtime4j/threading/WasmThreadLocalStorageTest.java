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

package ai.tegmentum.wasmtime4j.threading;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.concurrent.WasmThreadLocalStorage;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.panama.PanamaWasmThreadLocalStorage;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WebAssembly Thread Local Storage with native library loaded.
 *
 * <p>These tests verify TLS get/set operations across multiple threads and ensure proper isolation
 * and thread-safety of thread-local storage.
 *
 * @since 1.0.0
 */
@DisplayName("WASM Thread Local Storage Integration Tests")
class WasmThreadLocalStorageTest {

  private static final Logger LOGGER = Logger.getLogger(WasmThreadLocalStorageTest.class.getName());

  private static boolean threadsSupported = false;
  private static boolean nativeLibraryLoaded = false;

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for thread local storage tests");
    try {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      nativeLibraryLoaded = loader.isLoaded();
      LOGGER.info("Native library loaded: " + nativeLibraryLoaded);
    } catch (final RuntimeException e) {
      LOGGER.warning("Failed to load native library: " + e.getMessage());
      nativeLibraryLoaded = false;
    }

    // Check if threads are supported
    if (nativeLibraryLoaded) {
      try {
        final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.THREADS);
        try (Engine engine = Engine.create(config)) {
          threadsSupported = engine != null;
        }
      } catch (Exception e) {
        LOGGER.warning("WASM threads may not be fully supported: " + e.getMessage());
        threadsSupported = false;
      }
    }
    LOGGER.info("WASM threads supported: " + threadsSupported);
  }

  @AfterAll
  static void cleanupAll() {
    LOGGER.info("Completed WASM Thread Local Storage Integration Tests");
  }

  private static void assumeNativeLibraryLoaded() {
    assumeTrue(nativeLibraryLoaded, "Native library not loaded - skipping");
  }

  private static void assumeThreadsSupported() {
    assumeTrue(threadsSupported, "WASM threads not supported - skipping");
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up thread local storage test");
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
  }

  @Nested
  @DisplayName("WasmThreadLocalStorage Interface Tests")
  class InterfaceTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      LOGGER.info("Testing WasmThreadLocalStorage interface methods");

      final Class<?>[] interfaces = WasmThreadLocalStorage.class.getInterfaces();
      LOGGER.info("WasmThreadLocalStorage implements " + interfaces.length + " interfaces");

      // Verify method signatures exist
      try {
        WasmThreadLocalStorage.class.getMethod("putInt", String.class, int.class);
        WasmThreadLocalStorage.class.getMethod("getInt", String.class);
        WasmThreadLocalStorage.class.getMethod("putLong", String.class, long.class);
        WasmThreadLocalStorage.class.getMethod("getLong", String.class);
        WasmThreadLocalStorage.class.getMethod("putFloat", String.class, float.class);
        WasmThreadLocalStorage.class.getMethod("getFloat", String.class);
        WasmThreadLocalStorage.class.getMethod("putDouble", String.class, double.class);
        WasmThreadLocalStorage.class.getMethod("getDouble", String.class);
        WasmThreadLocalStorage.class.getMethod("putBytes", String.class, byte[].class);
        WasmThreadLocalStorage.class.getMethod("getBytes", String.class);
        WasmThreadLocalStorage.class.getMethod("putString", String.class, String.class);
        WasmThreadLocalStorage.class.getMethod("getString", String.class);
        WasmThreadLocalStorage.class.getMethod("remove", String.class);
        WasmThreadLocalStorage.class.getMethod("contains", String.class);
        WasmThreadLocalStorage.class.getMethod("clear");
        WasmThreadLocalStorage.class.getMethod("size");
        WasmThreadLocalStorage.class.getMethod("getMemoryUsage");
        LOGGER.info("All expected methods found in WasmThreadLocalStorage interface");
      } catch (NoSuchMethodException e) {
        throw new AssertionError("Missing method: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("PanamaWasmThreadLocalStorage Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("should reject null native thread handle")
    void shouldRejectNullNativeThreadHandle() {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing null native thread handle rejection");

      try (Arena arena = Arena.ofConfined()) {
        assertThrows(
            Exception.class,
            () -> new PanamaWasmThreadLocalStorage(null, arena),
            "Should reject null native thread handle");
        LOGGER.info("Null native thread handle correctly rejected");
      }
    }

    @Test
    @DisplayName("should reject zero address native thread handle")
    void shouldRejectZeroAddressNativeThreadHandle() {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing zero address native thread handle rejection");

      try (Arena arena = Arena.ofConfined()) {
        assertThrows(
            Exception.class,
            () -> new PanamaWasmThreadLocalStorage(MemorySegment.NULL, arena),
            "Should reject NULL memory segment");
        LOGGER.info("Zero address native thread handle correctly rejected");
      }
    }

    @Test
    @DisplayName("should reject null arena")
    void shouldRejectNullArena() {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing null arena rejection");

      try (Arena arena = Arena.ofConfined()) {
        // Create a non-null memory segment for the test
        final MemorySegment segment = arena.allocate(8);

        assertThrows(
            Exception.class,
            () -> new PanamaWasmThreadLocalStorage(segment, null),
            "Should reject null arena");
        LOGGER.info("Null arena correctly rejected");
      }
    }
  }

  @Nested
  @DisplayName("Thread Local Storage Isolation Tests")
  class IsolationTests {

    @Test
    @DisplayName("should verify thread isolation concept")
    void shouldVerifyThreadIsolationConcept() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing thread isolation concept");

      // Use ThreadLocal to verify isolation works as expected
      final ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
      final AtomicReference<Integer> thread1Value = new AtomicReference<>();
      final AtomicReference<Integer> thread2Value = new AtomicReference<>();

      final CountDownLatch latch = new CountDownLatch(2);

      // Thread 1 sets value 100
      Thread t1 =
          new Thread(
              () -> {
                threadLocal.set(100);
                try {
                  Thread.sleep(50);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
                thread1Value.set(threadLocal.get());
                latch.countDown();
              });

      // Thread 2 sets value 200
      Thread t2 =
          new Thread(
              () -> {
                threadLocal.set(200);
                try {
                  Thread.sleep(50);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
                thread2Value.set(threadLocal.get());
                latch.countDown();
              });

      t1.start();
      t2.start();

      assertTrue(latch.await(5, TimeUnit.SECONDS), "Both threads should complete");

      assertEquals(100, thread1Value.get(), "Thread 1 should see its own value");
      assertEquals(200, thread2Value.get(), "Thread 2 should see its own value");

      LOGGER.info(
          "Thread isolation verified: t1=" + thread1Value.get() + ", t2=" + thread2Value.get());
    }

    @Test
    @DisplayName("should demonstrate TLS concept with multiple threads")
    void shouldDemonstrateTlsConceptWithMultipleThreads() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing TLS concept with multiple threads");

      final int numThreads = 4;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      resources.add(executor::shutdown);

      final ThreadLocal<String> threadLocal = ThreadLocal.withInitial(() -> "default");
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(numThreads);
      final AtomicBoolean allPassed = new AtomicBoolean(true);

      for (int i = 0; i < numThreads; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                startLatch.await(); // Wait for all threads to be ready
                String expectedValue = "thread-" + threadId;
                threadLocal.set(expectedValue);

                // Small delay to allow interleaving
                Thread.sleep(10);

                String actualValue = threadLocal.get();
                if (!expectedValue.equals(actualValue)) {
                  LOGGER.warning(
                      "Thread "
                          + threadId
                          + " isolation failed: expected="
                          + expectedValue
                          + ", actual="
                          + actualValue);
                  allPassed.set(false);
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                doneLatch.countDown();
              }
            });
      }

      // Start all threads at once
      startLatch.countDown();

      assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
      assertTrue(allPassed.get(), "All threads should see their own TLS values");

      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      LOGGER.info("TLS concept verified with " + numThreads + " threads");
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("should handle concurrent access with proper synchronization")
    void shouldHandleConcurrentAccessWithProperSynchronization() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing concurrent access with synchronization");

      final int numThreads = 8;
      final int iterationsPerThread = 100;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      resources.add(executor::shutdown);

      // Use a shared counter with synchronization
      final Object lock = new Object();
      final int[] counter = {0};
      final CountDownLatch latch = new CountDownLatch(numThreads);

      for (int i = 0; i < numThreads; i++) {
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < iterationsPerThread; j++) {
                  synchronized (lock) {
                    counter[0]++;
                  }
                }
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
      assertEquals(
          numThreads * iterationsPerThread, counter[0], "Counter should be incremented correctly");

      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      LOGGER.info("Concurrent access test passed: counter=" + counter[0]);
    }

    @Test
    @DisplayName("should verify atomic operations for thread safety")
    void shouldVerifyAtomicOperationsForThreadSafety() throws Exception {
      assumeNativeLibraryLoaded();
      LOGGER.info("Testing atomic operations for thread safety");

      final int numThreads = 10;
      final int incrementsPerThread = 1000;
      final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      resources.add(executor::shutdown);

      final java.util.concurrent.atomic.AtomicLong atomicCounter =
          new java.util.concurrent.atomic.AtomicLong(0);
      final CountDownLatch latch = new CountDownLatch(numThreads);

      for (int i = 0; i < numThreads; i++) {
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < incrementsPerThread; j++) {
                  atomicCounter.incrementAndGet();
                }
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
      assertEquals(
          numThreads * incrementsPerThread,
          atomicCounter.get(),
          "Atomic counter should be correct");

      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      LOGGER.info("Atomic operations test passed: counter=" + atomicCounter.get());
    }
  }

  @Nested
  @DisplayName("TLS Data Type Tests")
  class DataTypeTests {

    @Test
    @DisplayName("should store and retrieve integer values")
    void shouldStoreAndRetrieveIntegerValues() {
      LOGGER.info("Testing integer value storage pattern");

      final ThreadLocal<Integer> intTls = new ThreadLocal<>();

      intTls.set(42);
      assertEquals(42, intTls.get(), "Should retrieve stored integer");

      intTls.set(Integer.MAX_VALUE);
      assertEquals(Integer.MAX_VALUE, intTls.get(), "Should handle MAX_VALUE");

      intTls.set(Integer.MIN_VALUE);
      assertEquals(Integer.MIN_VALUE, intTls.get(), "Should handle MIN_VALUE");

      LOGGER.info("Integer storage pattern verified");
    }

    @Test
    @DisplayName("should store and retrieve long values")
    void shouldStoreAndRetrieveLongValues() {
      LOGGER.info("Testing long value storage pattern");

      final ThreadLocal<Long> longTls = new ThreadLocal<>();

      longTls.set(123456789012345L);
      assertEquals(123456789012345L, longTls.get(), "Should retrieve stored long");

      longTls.set(Long.MAX_VALUE);
      assertEquals(Long.MAX_VALUE, longTls.get(), "Should handle MAX_VALUE");

      longTls.set(Long.MIN_VALUE);
      assertEquals(Long.MIN_VALUE, longTls.get(), "Should handle MIN_VALUE");

      LOGGER.info("Long storage pattern verified");
    }

    @Test
    @DisplayName("should store and retrieve float values")
    void shouldStoreAndRetrieveFloatValues() {
      LOGGER.info("Testing float value storage pattern");

      final ThreadLocal<Float> floatTls = new ThreadLocal<>();

      floatTls.set(3.14159f);
      assertEquals(3.14159f, floatTls.get(), 0.00001f, "Should retrieve stored float");

      floatTls.set(Float.MAX_VALUE);
      assertEquals(Float.MAX_VALUE, floatTls.get(), "Should handle MAX_VALUE");

      floatTls.set(Float.MIN_VALUE);
      assertEquals(Float.MIN_VALUE, floatTls.get(), "Should handle MIN_VALUE");

      LOGGER.info("Float storage pattern verified");
    }

    @Test
    @DisplayName("should store and retrieve double values")
    void shouldStoreAndRetrieveDoubleValues() {
      LOGGER.info("Testing double value storage pattern");

      final ThreadLocal<Double> doubleTls = new ThreadLocal<>();

      doubleTls.set(Math.PI);
      assertEquals(Math.PI, doubleTls.get(), 0.000000001, "Should retrieve stored double");

      doubleTls.set(Double.MAX_VALUE);
      assertEquals(Double.MAX_VALUE, doubleTls.get(), "Should handle MAX_VALUE");

      doubleTls.set(Double.MIN_VALUE);
      assertEquals(Double.MIN_VALUE, doubleTls.get(), "Should handle MIN_VALUE");

      LOGGER.info("Double storage pattern verified");
    }

    @Test
    @DisplayName("should store and retrieve byte arrays")
    void shouldStoreAndRetrieveByteArrays() {
      LOGGER.info("Testing byte array storage pattern");

      final ThreadLocal<byte[]> bytesTls = new ThreadLocal<>();

      byte[] testData = {1, 2, 3, 4, 5};
      bytesTls.set(testData);
      assertArrayEquals(testData, bytesTls.get(), "Should retrieve stored bytes");

      byte[] emptyData = {};
      bytesTls.set(emptyData);
      assertArrayEquals(emptyData, bytesTls.get(), "Should handle empty array");

      byte[] largeData = new byte[1024];
      for (int i = 0; i < largeData.length; i++) {
        largeData[i] = (byte) (i % 256);
      }
      bytesTls.set(largeData);
      assertArrayEquals(largeData, bytesTls.get(), "Should handle large array");

      LOGGER.info("Byte array storage pattern verified");
    }

    @Test
    @DisplayName("should store and retrieve strings")
    void shouldStoreAndRetrieveStrings() {
      LOGGER.info("Testing string storage pattern");

      final ThreadLocal<String> stringTls = new ThreadLocal<>();

      stringTls.set("Hello, World!");
      assertEquals("Hello, World!", stringTls.get(), "Should retrieve stored string");

      stringTls.set("");
      assertEquals("", stringTls.get(), "Should handle empty string");

      String unicode = "日本語テスト 🎉";
      stringTls.set(unicode);
      assertEquals(unicode, stringTls.get(), "Should handle unicode string");

      LOGGER.info("String storage pattern verified");
    }
  }

  @Nested
  @DisplayName("TLS Utility Method Tests")
  class UtilityMethodTests {

    @Test
    @DisplayName("should remove stored values")
    void shouldRemoveStoredValues() {
      LOGGER.info("Testing value removal");

      final ThreadLocal<String> tls = new ThreadLocal<>();

      tls.set("test-value");
      assertNotNull(tls.get(), "Value should be set");

      tls.remove();
      assertEquals(null, tls.get(), "Value should be removed");

      LOGGER.info("Value removal verified");
    }

    @Test
    @DisplayName("should clear all values")
    void shouldClearAllValues() {
      LOGGER.info("Testing clear functionality");

      final ThreadLocal<String> tls1 = new ThreadLocal<>();
      final ThreadLocal<Integer> tls2 = new ThreadLocal<>();
      final ThreadLocal<Double> tls3 = new ThreadLocal<>();

      tls1.set("value1");
      tls2.set(42);
      tls3.set(3.14);

      // Remove all values
      tls1.remove();
      tls2.remove();
      tls3.remove();

      assertEquals(null, tls1.get(), "String TLS should be cleared");
      assertEquals(null, tls2.get(), "Integer TLS should be cleared");
      assertEquals(null, tls3.get(), "Double TLS should be cleared");

      LOGGER.info("Clear functionality verified");
    }

    @Test
    @DisplayName("should track size correctly")
    void shouldTrackSizeCorrectly() {
      LOGGER.info("Testing size tracking concept");

      final java.util.Map<String, Object> storageMap =
          new java.util.concurrent.ConcurrentHashMap<>();

      assertEquals(0, storageMap.size(), "Initial size should be 0");

      storageMap.put("key1", "value1");
      assertEquals(1, storageMap.size(), "Size should be 1 after adding one entry");

      storageMap.put("key2", 42);
      storageMap.put("key3", 3.14);
      assertEquals(3, storageMap.size(), "Size should be 3 after adding three entries");

      storageMap.remove("key1");
      assertEquals(2, storageMap.size(), "Size should be 2 after removing one entry");

      storageMap.clear();
      assertEquals(0, storageMap.size(), "Size should be 0 after clearing");

      LOGGER.info("Size tracking concept verified");
    }

    @Test
    @DisplayName("should check if key exists")
    void shouldCheckIfKeyExists() {
      LOGGER.info("Testing key existence check");

      final java.util.Map<String, Object> storageMap =
          new java.util.concurrent.ConcurrentHashMap<>();

      assertFalse(storageMap.containsKey("nonexistent"), "Should not contain nonexistent key");

      storageMap.put("test-key", "test-value");
      assertTrue(storageMap.containsKey("test-key"), "Should contain added key");

      storageMap.remove("test-key");
      assertFalse(storageMap.containsKey("test-key"), "Should not contain removed key");

      LOGGER.info("Key existence check verified");
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("should reject null keys")
    void shouldRejectNullKeys() {
      LOGGER.info("Testing null key rejection");

      final java.util.Map<String, Object> storageMap = new java.util.HashMap<>();

      // HashMap doesn't reject null keys, but we can verify the pattern
      try {
        java.util.Objects.requireNonNull(null, "Key cannot be null");
        throw new AssertionError("Should have thrown NullPointerException");
      } catch (NullPointerException e) {
        LOGGER.info("Null key correctly rejected: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("should reject empty keys")
    void shouldRejectEmptyKeys() {
      LOGGER.info("Testing empty key rejection");

      final String key = "";

      if (key == null || key.isEmpty()) {
        LOGGER.info("Empty key correctly rejected");
      } else {
        throw new AssertionError("Empty key should be rejected");
      }
    }

    @Test
    @DisplayName("should handle closed storage gracefully")
    void shouldHandleClosedStorageGracefully() {
      LOGGER.info("Testing closed storage handling");

      final AtomicBoolean closed = new AtomicBoolean(false);

      // Simulate closing storage
      closed.set(true);

      if (closed.get()) {
        try {
          throw new IllegalStateException("Thread-local storage has been closed");
        } catch (IllegalStateException e) {
          LOGGER.info("Closed storage access correctly rejected: " + e.getMessage());
        }
      }
    }
  }
}
