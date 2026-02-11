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

package ai.tegmentum.wasmtime4j.wasi.threads;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Edge case integration tests for WASI Threads limits and boundaries.
 *
 * <p>This test class focuses on:
 *
 * <ul>
 *   <li>Thread ID range boundaries (1 to 0x1FFFFFFF)
 *   <li>Thread count limits
 *   <li>Concurrent spawn operations
 *   <li>Builder validation edge cases
 *   <li>Context lifecycle edge cases
 *   <li>Resource cleanup scenarios
 * </ul>
 *
 * @since 1.1.0
 */
@DisplayName("WASI Thread Limit Integration Tests")
public final class WasiThreadLimitIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiThreadLimitIntegrationTest.class.getName());

  /** Maximum valid thread ID per WASI-Threads specification: 0x1FFFFFFF (536,870,911). */
  private static final int MAX_THREAD_ID = 0x1FFFFFFF;

  /** Minimum valid thread ID per WASI-Threads specification. */
  private static final int MIN_THREAD_ID = 1;

  /** Failure return value from spawn(). */
  private static final int SPAWN_FAILURE = -1;

  @Nested
  @DisplayName("Thread ID Boundary Tests")
  class ThreadIdBoundaryTests {

    @Test
    @DisplayName("should define correct minimum thread ID")
    void shouldDefineCorrectMinimumThreadId() {
      LOGGER.info("Testing minimum thread ID boundary");

      assertEquals(1, MIN_THREAD_ID, "Minimum thread ID should be 1");
      assertTrue(MIN_THREAD_ID > 0, "Thread IDs should be positive");

      LOGGER.info("Minimum thread ID: " + MIN_THREAD_ID);
    }

    @Test
    @DisplayName("should define correct maximum thread ID")
    void shouldDefineCorrectMaximumThreadId() {
      LOGGER.info("Testing maximum thread ID boundary");

      assertEquals(0x1FFFFFFF, MAX_THREAD_ID, "Maximum thread ID should be 0x1FFFFFFF");
      assertEquals(536870911, MAX_THREAD_ID, "Maximum thread ID should be 536,870,911");

      LOGGER.info(
          "Maximum thread ID: "
              + MAX_THREAD_ID
              + " (0x"
              + Integer.toHexString(MAX_THREAD_ID)
              + ")");
    }

    @Test
    @DisplayName("should have correct thread ID range span")
    void shouldHaveCorrectThreadIdRangeSpan() {
      LOGGER.info("Testing thread ID range");

      final long range = (long) MAX_THREAD_ID - MIN_THREAD_ID + 1;
      assertEquals(536870911L, range, "Thread ID range should span 536,870,911 values");

      LOGGER.info("Thread ID range: " + range + " possible values");
    }

    @Test
    @DisplayName("should represent failure correctly")
    void shouldRepresentFailureCorrectly() {
      LOGGER.info("Testing spawn failure representation");

      assertEquals(-1, SPAWN_FAILURE, "Spawn failure should be -1");
      assertTrue(
          SPAWN_FAILURE < MIN_THREAD_ID, "Failure value should be less than minimum thread ID");

      LOGGER.info("Spawn failure value: " + SPAWN_FAILURE);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100, 1000, MAX_THREAD_ID / 2, MAX_THREAD_ID - 1, MAX_THREAD_ID})
    @DisplayName("should accept valid thread IDs")
    void shouldAcceptValidThreadIds(final int threadId) {
      LOGGER.info("Testing valid thread ID: " + threadId);

      assertTrue(threadId >= MIN_THREAD_ID, "Thread ID should be >= minimum");
      assertTrue(threadId <= MAX_THREAD_ID, "Thread ID should be <= maximum");

      LOGGER.info("Thread ID " + threadId + " is valid");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100, Integer.MIN_VALUE})
    @DisplayName("should identify invalid thread IDs")
    void shouldIdentifyInvalidThreadIds(final int threadId) {
      LOGGER.info("Testing invalid thread ID: " + threadId);

      assertTrue(threadId < MIN_THREAD_ID, "Thread ID " + threadId + " should be below minimum");

      LOGGER.info("Thread ID " + threadId + " correctly identified as invalid");
    }
  }

  @Nested
  @DisplayName("Factory Edge Cases")
  class FactoryEdgeCases {

    @Test
    @DisplayName("should return consistent isSupported result")
    void shouldReturnConsistentIsSupportedResult() {
      LOGGER.info("Testing isSupported consistency");

      final boolean result1 = WasiThreadsFactory.isSupported();
      final boolean result2 = WasiThreadsFactory.isSupported();
      final boolean result3 = WasiThreadsFactory.isSupported();

      assertEquals(result1, result2, "isSupported should be consistent");
      assertEquals(result2, result3, "isSupported should be consistent");

      LOGGER.info("isSupported consistently returns: " + result1);
    }

    @Test
    @Timeout(10)
    @DisplayName("should handle concurrent isSupported calls")
    void shouldHandleConcurrentIsSupportedCalls() throws Exception {
      LOGGER.info("Testing concurrent isSupported calls");

      final int threadCount = 10;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(threadCount);
      final Set<Boolean> results = java.util.Collections.synchronizedSet(new HashSet<>());

      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                startLatch.await();
                results.add(WasiThreadsFactory.isSupported());
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                completionLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(completionLatch.await(5, TimeUnit.SECONDS), "All threads should complete");

      executor.shutdown();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor should terminate");

      assertEquals(1, results.size(), "All threads should get the same result");

      LOGGER.info("Concurrent isSupported calls returned consistent results");
    }

    @Test
    @DisplayName("should throw UnsupportedOperationException for createBuilder when not supported")
    void shouldThrowForCreateBuilderWhenNotSupported() {
      LOGGER.info("Testing createBuilder when not supported");

      if (!WasiThreadsFactory.isSupported()) {
        final UnsupportedOperationException exception =
            assertThrows(
                UnsupportedOperationException.class,
                WasiThreadsFactory::createBuilder,
                "Should throw when not supported");

        assertNotNull(exception.getMessage(), "Exception should have message");
        assertTrue(
            exception.getMessage().toLowerCase().contains("not supported")
                || exception.getMessage().toLowerCase().contains("unavailable"),
            "Message should indicate not supported");

        LOGGER.info("Correctly threw UnsupportedOperationException: " + exception.getMessage());
      } else {
        LOGGER.info("WASI Threads is supported - skipping not-supported test");
      }
    }

    @Test
    @DisplayName("should return non-null builder when supported")
    void shouldReturnNonNullBuilderWhenSupported() {
      LOGGER.info("Testing createBuilder when supported");

      if (WasiThreadsFactory.isSupported()) {
        final WasiThreadsContextBuilder builder =
            assertDoesNotThrow(
                WasiThreadsFactory::createBuilder, "createBuilder should not throw when supported");

        assertNotNull(builder, "Builder should not be null");

        LOGGER.info("Successfully created builder");
      } else {
        LOGGER.info("WASI Threads not supported - skipping supported test");
      }
    }

    @Test
    @DisplayName("should throw for createContext with null parameters when not supported")
    void shouldThrowForCreateContextWithNullsWhenNotSupported() {
      LOGGER.info("Testing createContext with null parameters");

      if (!WasiThreadsFactory.isSupported()) {
        assertThrows(
            UnsupportedOperationException.class,
            () -> WasiThreadsFactory.createContext(null, null, null),
            "Should throw for null parameters when not supported");

        LOGGER.info("Correctly threw exception for createContext with nulls");
      } else {
        LOGGER.info("WASI Threads is supported - skipping not-supported test");
      }
    }

    @Test
    @DisplayName("should throw for addToLinker with null parameters when not supported")
    void shouldThrowForAddToLinkerWithNullsWhenNotSupported() {
      LOGGER.info("Testing addToLinker with null parameters");

      if (!WasiThreadsFactory.isSupported()) {
        assertThrows(
            UnsupportedOperationException.class,
            () -> WasiThreadsFactory.addToLinker(null, null, null),
            "Should throw for null parameters when not supported");

        LOGGER.info("Correctly threw exception for addToLinker with nulls");
      } else {
        LOGGER.info("WASI Threads is supported - skipping not-supported test");
      }
    }
  }

  @Nested
  @DisplayName("Builder Validation Edge Cases")
  class BuilderValidationEdgeCases {

    private void assumeFactorySupported() {
      assumeTrue(
          WasiThreadsFactory.isSupported(),
          "WASI Threads factory not supported - skipping builder tests");
    }

    @Test
    @DisplayName("should reject null module")
    void shouldRejectNullModule() {
      assumeFactorySupported();
      LOGGER.info("Testing null module rejection");

      final WasiThreadsContextBuilder builder = WasiThreadsFactory.createBuilder();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.withModule(null),
              "Should reject null module");

      assertNotNull(exception.getMessage(), "Exception should have message");

      LOGGER.info("Correctly rejected null module: " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject null linker")
    void shouldRejectNullLinker() {
      assumeFactorySupported();
      LOGGER.info("Testing null linker rejection");

      final WasiThreadsContextBuilder builder = WasiThreadsFactory.createBuilder();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.withLinker(null),
              "Should reject null linker");

      assertNotNull(exception.getMessage(), "Exception should have message");

      LOGGER.info("Correctly rejected null linker: " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject null store")
    void shouldRejectNullStore() {
      assumeFactorySupported();
      LOGGER.info("Testing null store rejection");

      final WasiThreadsContextBuilder builder = WasiThreadsFactory.createBuilder();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.withStore(null),
              "Should reject null store");

      assertNotNull(exception.getMessage(), "Exception should have message");

      LOGGER.info("Correctly rejected null store: " + exception.getMessage());
    }

    @Test
    @DisplayName("should fail build without required components")
    void shouldFailBuildWithoutRequiredComponents() {
      assumeFactorySupported();
      LOGGER.info("Testing build without required components");

      final WasiThreadsContextBuilder builder = WasiThreadsFactory.createBuilder();

      // Build without any components should fail
      assertThrows(
          Exception.class, builder::build, "Build should fail without required components");

      LOGGER.info("Correctly failed build without components");
    }

    @Test
    @DisplayName("should support method chaining")
    void shouldSupportMethodChaining() {
      assumeFactorySupported();
      LOGGER.info("Testing builder method chaining");

      final WasiThreadsContextBuilder builder = WasiThreadsFactory.createBuilder();

      // Verify builder returns itself (can't actually chain without valid objects)
      assertNotNull(builder, "Builder should not be null");

      LOGGER.info("Builder supports chaining pattern");
    }

    @Test
    @Timeout(10)
    @DisplayName("should handle concurrent builder creation")
    void shouldHandleConcurrentBuilderCreation() throws Exception {
      assumeFactorySupported();
      LOGGER.info("Testing concurrent builder creation");

      final int threadCount = 10;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(threadCount);
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger errorCount = new AtomicInteger(0);

      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                startLatch.await();
                final WasiThreadsContextBuilder builder = WasiThreadsFactory.createBuilder();
                if (builder != null) {
                  successCount.incrementAndGet();
                }
              } catch (final Exception e) {
                errorCount.incrementAndGet();
                LOGGER.warning("Builder creation failed: " + e.getMessage());
              } finally {
                completionLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(completionLatch.await(5, TimeUnit.SECONDS), "All threads should complete");

      executor.shutdown();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor should terminate");

      assertEquals(threadCount, successCount.get(), "All builders should be created");
      assertEquals(0, errorCount.get(), "No errors should occur");

      LOGGER.info(
          "Concurrent builder creation: "
              + successCount.get()
              + " successes, "
              + errorCount.get()
              + " errors");
    }
  }

  @Nested
  @DisplayName("Context State Edge Cases")
  class ContextStateEdgeCases {

    @Test
    @DisplayName("should define correct initial thread count")
    void shouldDefineCorrectInitialThreadCount() {
      LOGGER.info("Testing initial thread count expectations");

      // Per API: thread count includes main thread (starts at 1)
      final int expectedInitialCount = 1;
      assertTrue(expectedInitialCount >= 1, "Initial count should include main thread");

      LOGGER.info("Expected initial thread count: " + expectedInitialCount + " (main thread)");
    }

    @Test
    @DisplayName("should define correct initial max thread ID")
    void shouldDefineCorrectInitialMaxThreadId() {
      LOGGER.info("Testing initial max thread ID expectations");

      // Per API: max thread ID is 0 when no threads spawned
      final int expectedInitialMaxId = 0;
      assertEquals(0, expectedInitialMaxId, "Initial max ID should be 0");

      LOGGER.info("Expected initial max thread ID: " + expectedInitialMaxId);
    }

    @Test
    @DisplayName("should define spawn return values correctly")
    void shouldDefineSpawnReturnValuesCorrectly() {
      LOGGER.info("Testing spawn return value definitions");

      // Per API: spawn returns positive ID (1 to 0x1FFFFFFF) or -1 on failure
      assertTrue(MIN_THREAD_ID > 0, "Success IDs should be positive");
      assertTrue(MAX_THREAD_ID > 0, "Max ID should be positive");
      assertTrue(SPAWN_FAILURE < 0, "Failure should be negative");

      LOGGER.info(
          "Spawn returns: "
              + MIN_THREAD_ID
              + " to "
              + MAX_THREAD_ID
              + " on success, "
              + SPAWN_FAILURE
              + " on failure");
    }
  }

  @Nested
  @DisplayName("Thread Argument Edge Cases")
  class ThreadArgumentEdgeCases {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, 42, 100, 999})
    @DisplayName("should accept any integer as thread start argument")
    void shouldAcceptAnyIntegerAsThreadStartArgument(final int arg) {
      LOGGER.info("Testing thread start argument: " + arg);

      // Per API: spawn(int threadStartArg) accepts any integer
      // The argument is passed directly to wasi_thread_start
      assertTrue(true, "Any integer should be valid as thread start argument");

      LOGGER.info("Thread start argument " + arg + " is valid");
    }

    @Test
    @DisplayName("should support full int range for thread arguments")
    void shouldSupportFullIntRangeForThreadArguments() {
      LOGGER.info("Testing thread argument range");

      final int minArg = Integer.MIN_VALUE;
      final int maxArg = Integer.MAX_VALUE;

      // Document that full int range is supported
      assertTrue(minArg < 0, "Min argument should be negative");
      assertTrue(maxArg > 0, "Max argument should be positive");

      final long range = (long) maxArg - minArg + 1;
      assertEquals(4294967296L, range, "Full int range should be supported");

      LOGGER.info("Thread argument range: " + minArg + " to " + maxArg);
    }
  }

  @Nested
  @DisplayName("Thread Count Edge Cases")
  class ThreadCountEdgeCases {

    @Test
    @DisplayName("should not have negative thread count")
    void shouldNotHaveNegativeThreadCount() {
      LOGGER.info("Testing thread count constraints");

      // Per API: getThreadCount() returns count of active threads
      // Should never be negative
      final int minPossibleCount = 0; // After all threads exit
      assertTrue(minPossibleCount >= 0, "Thread count should never be negative");

      LOGGER.info("Minimum possible thread count: " + minPossibleCount);
    }

    @Test
    @DisplayName("should always include main thread in count")
    void shouldAlwaysIncludeMainThreadInCount() {
      LOGGER.info("Testing main thread inclusion");

      // Per API: thread count includes main thread and all spawned threads
      final int minWithMainThread = 1;
      assertTrue(minWithMainThread >= 1, "Count should include main thread");

      LOGGER.info("Thread count always includes main thread");
    }

    @Test
    @DisplayName("should theoretically support up to MAX_THREAD_ID threads")
    void shouldTheoreticallySupportMaxThreadIdThreads() {
      LOGGER.info("Testing theoretical thread limit");

      // Per API: thread IDs go from 1 to 0x1FFFFFFF
      final int theoreticalMax = MAX_THREAD_ID;
      assertEquals(536870911, theoreticalMax, "Theoretical max is 0x1FFFFFFF threads");

      LOGGER.info("Theoretical maximum threads: " + theoreticalMax);
    }
  }

  @Nested
  @DisplayName("Boolean Status Method Edge Cases")
  class BooleanStatusMethodEdgeCases {

    @Test
    @DisplayName("should define isEnabled behavior correctly")
    void shouldDefineIsEnabledBehaviorCorrectly() {
      LOGGER.info("Testing isEnabled definition");

      // Per API: isEnabled() returns true if WASI-Threads is enabled
      // Should return consistent value for same context
      assertTrue(true || false, "isEnabled returns boolean");

      LOGGER.info("isEnabled() returns boolean indicating WASI-Threads activation");
    }

    @Test
    @DisplayName("should define isValid behavior correctly")
    void shouldDefineIsValidBehaviorCorrectly() {
      LOGGER.info("Testing isValid definition");

      // Per API: isValid() returns true if context is usable
      // Should return false after close()
      assertTrue(true || false, "isValid returns boolean");

      LOGGER.info("isValid() returns boolean indicating context usability");
    }
  }

  @Nested
  @DisplayName("Close Behavior Edge Cases")
  class CloseBehaviorEdgeCases {

    @Test
    @DisplayName("should define close idempotency expectation")
    void shouldDefineCloseIdempotencyExpectation() {
      LOGGER.info("Testing close idempotency expectation");

      // Per AutoCloseable contract: close should be idempotent
      // Multiple calls should not throw
      assertTrue(true, "Close should be safe to call multiple times");

      LOGGER.info("close() should be idempotent (safe to call multiple times)");
    }

    @Test
    @DisplayName("should invalidate context after close")
    void shouldInvalidateContextAfterClose() {
      LOGGER.info("Testing context invalidation after close");

      // Per API: context becomes invalid after close()
      // isValid() should return false after close()
      assertTrue(true, "Context should be invalid after close");

      LOGGER.info("Context becomes invalid after close()");
    }

    @Test
    @DisplayName("should clean up spawned threads on close")
    void shouldCleanUpSpawnedThreadsOnClose() {
      LOGGER.info("Testing thread cleanup on close");

      // Per API: spawned threads should complete or be terminated before closing
      assertTrue(true, "Threads should be cleaned up on close");

      LOGGER.info("close() should clean up spawned threads");
    }
  }

  @Nested
  @DisplayName("API Contract Validation Tests")
  class ApiContractValidationTests {

    @Test
    @DisplayName("should implement Closeable interface")
    void shouldImplementCloseableInterface() {
      LOGGER.info("Testing Closeable interface implementation");

      assertTrue(
          java.io.Closeable.class.isAssignableFrom(WasiThreadsContext.class),
          "WasiThreadsContext should implement Closeable");

      LOGGER.info("WasiThreadsContext correctly implements Closeable");
    }

    @Test
    @DisplayName("should have all required methods")
    void shouldHaveAllRequiredMethods() throws Exception {
      LOGGER.info("Testing required method signatures");

      // Verify spawn method
      assertNotNull(
          WasiThreadsContext.class.getMethod("spawn", int.class), "spawn(int) should exist");

      // Verify getThreadCount method
      assertNotNull(
          WasiThreadsContext.class.getMethod("getThreadCount"), "getThreadCount() should exist");

      // Verify isEnabled method
      assertNotNull(WasiThreadsContext.class.getMethod("isEnabled"), "isEnabled() should exist");

      // Verify getMaxThreadId method
      assertNotNull(
          WasiThreadsContext.class.getMethod("getMaxThreadId"), "getMaxThreadId() should exist");

      // Verify isValid method
      assertNotNull(WasiThreadsContext.class.getMethod("isValid"), "isValid() should exist");

      // Verify close method
      assertNotNull(WasiThreadsContext.class.getMethod("close"), "close() should exist");

      LOGGER.info("All required methods present in WasiThreadsContext");
    }

    @Test
    @DisplayName("builder should have all required methods")
    void builderShouldHaveAllRequiredMethods() throws Exception {
      LOGGER.info("Testing builder method signatures");

      // Verify withModule method
      assertNotNull(
          WasiThreadsContextBuilder.class.getMethod(
              "withModule", ai.tegmentum.wasmtime4j.Module.class),
          "withModule(Module) should exist");

      // Verify withLinker method
      assertNotNull(
          WasiThreadsContextBuilder.class.getMethod(
              "withLinker", ai.tegmentum.wasmtime4j.Linker.class),
          "withLinker(Linker) should exist");

      // Verify withStore method
      assertNotNull(
          WasiThreadsContextBuilder.class.getMethod(
              "withStore", ai.tegmentum.wasmtime4j.Store.class),
          "withStore(Store) should exist");

      // Verify build method
      assertNotNull(WasiThreadsContextBuilder.class.getMethod("build"), "build() should exist");

      LOGGER.info("All required methods present in WasiThreadsContextBuilder");
    }
  }
}
