/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.nativeloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Concurrency and thread-safety tests for {@link PlatformDetector}.
 *
 * <p>This test class focuses on:
 *
 * <ul>
 *   <li>Thread-safe caching mechanisms
 *   <li>Concurrent access to platform detection
 *   <li>Race condition prevention
 *   <li>Performance under concurrent load
 *   <li>Memory consistency guarantees
 * </ul>
 */

final class ConcurrencyTest {

  private ExecutorService executorService;

  @BeforeEach
  void setUp() {
    PlatformDetectorTestUtils.clearCache();
    executorService = Executors.newFixedThreadPool(20);
  }

  @AfterEach
  void tearDown() {
    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (final InterruptedException e) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  @Test

  @Timeout(10)
  void testConcurrentPlatformDetection() throws InterruptedException, ExecutionException {
    final int threadCount = 50;
    final List<Future<PlatformDetector.PlatformInfo>> futures = new ArrayList<>();

    // Start multiple threads simultaneously
    for (int i = 0; i < threadCount; i++) {
      final Future<PlatformDetector.PlatformInfo> future =
          executorService.submit(PlatformDetector::detect);
      futures.add(future);
    }

    // Collect all results
    final List<PlatformDetector.PlatformInfo> results = new ArrayList<>();
    for (final Future<PlatformDetector.PlatformInfo> future : futures) {
      final PlatformDetector.PlatformInfo result = future.get();
      assertNotNull(result, "Each thread should get a non-null result");
      results.add(result);
    }

    // Verify all results are identical (due to caching)
    final PlatformDetector.PlatformInfo firstResult = results.get(0);
    for (int i = 1; i < results.size(); i++) {
      assertSame(
          firstResult,
          results.get(i),
          "All concurrent calls should return the same cached instance");
    }
  }

  @Test

  @Timeout(10)
  void testConcurrentPlatformInfoAccess() throws InterruptedException, ExecutionException {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
    final int threadCount = 30;
    final List<Future<String>> futures = new ArrayList<>();

    // Test concurrent access to various PlatformInfo methods
    for (int i = 0; i < threadCount; i++) {
      final int threadIndex = i;
      final Future<String> future =
          executorService.submit(
              () -> {
                final StringBuilder result = new StringBuilder();
                result.append("Thread-").append(threadIndex).append(": ");
                result.append(platformInfo.getPlatformId()).append(", ");
                result.append(platformInfo.getLibraryFileName("test")).append(", ");
                result.append(platformInfo.getLibraryResourcePath("test")).append(", ");
                result.append(platformInfo.getOperatingSystem().getName()).append(", ");
                result.append(platformInfo.getArchitecture().getName());
                return result.toString();
              });
      futures.add(future);
    }

    // Collect all results and verify consistency
    final List<String> results = new ArrayList<>();
    for (final Future<String> future : futures) {
      results.add(future.get());
    }

    // All results should contain the same platform information
    final String expectedPlatformId = platformInfo.getPlatformId();
    final String expectedFileName = platformInfo.getLibraryFileName("test");
    final String expectedResourcePath = platformInfo.getLibraryResourcePath("test");

    for (final String result : results) {
      assertTrue(
          result.contains(expectedPlatformId), "Result should contain consistent platform ID");
      assertTrue(result.contains(expectedFileName), "Result should contain consistent file name");
      assertTrue(
          result.contains(expectedResourcePath), "Result should contain consistent resource path");
    }
  }

  @Test

  @Timeout(15)
  void testCacheInitializationRaceConditions() throws InterruptedException {
    final int iterations = 10;
    final int threadsPerIteration = 20;

    for (int iteration = 0; iteration < iterations; iteration++) {
      // Clear cache before each iteration
      PlatformDetectorTestUtils.clearCache();

      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(threadsPerIteration);
      final List<PlatformDetector.PlatformInfo> results =
          Collections.synchronizedList(new ArrayList<>());
      final AtomicInteger errorCount = new AtomicInteger(0);

      // Create threads that will all start simultaneously
      for (int i = 0; i < threadsPerIteration; i++) {
        executorService.submit(
            () -> {
              try {
                startLatch.await(); // Wait for signal to start
                final PlatformDetector.PlatformInfo result = PlatformDetector.detect();
                results.add(result);
              } catch (final Exception e) {
                errorCount.incrementAndGet();
              } finally {
                completionLatch.countDown();
              }
            });
      }

      // Release all threads simultaneously
      startLatch.countDown();

      // Wait for all threads to complete
      assertTrue(
          completionLatch.await(5, TimeUnit.SECONDS), "All threads should complete within timeout");

      // Verify results
      assertEquals(
          0,
          errorCount.get(),
          "No threads should have encountered errors in iteration " + iteration);
      assertEquals(
          threadsPerIteration,
          results.size(),
          "Should have results from all threads in iteration " + iteration);

      // All results should be the same instance due to proper caching
      final PlatformDetector.PlatformInfo firstResult = results.get(0);
      for (int i = 1; i < results.size(); i++) {
        assertSame(
            firstResult,
            results.get(i),
            "All results should be the same instance in iteration " + iteration);
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {10, 25, 50, 100})

  @Timeout(20)
  void testVaryingConcurrentLoad(final int threadCount)
      throws InterruptedException, ExecutionException {
    final List<CompletableFuture<PlatformDetector.PlatformInfo>> futures = new ArrayList<>();
    final long startTime = System.nanoTime();

    // Submit all tasks
    for (int i = 0; i < threadCount; i++) {
      final CompletableFuture<PlatformDetector.PlatformInfo> future =
          CompletableFuture.supplyAsync(PlatformDetector::detect, executorService);
      futures.add(future);
    }

    // Wait for all to complete and collect results
    final List<PlatformDetector.PlatformInfo> results = new ArrayList<>();
    for (final CompletableFuture<PlatformDetector.PlatformInfo> future : futures) {
      results.add(future.get());
    }

    final long endTime = System.nanoTime();
    final long durationMs = (endTime - startTime) / 1_000_000;

    // Verify correctness
    assertEquals(threadCount, results.size(), "Should have results from all threads");
    final PlatformDetector.PlatformInfo firstResult = results.get(0);
    for (final PlatformDetector.PlatformInfo result : results) {
      assertSame(firstResult, result, "All results should be the same cached instance");
    }

    // Performance assertion - should complete reasonably quickly
    assertTrue(
        durationMs < 10000, // 10 seconds max
        String.format(
            "Operation with %d threads should complete within 10 seconds (took %d ms)",
            threadCount, durationMs));
  }

  @RepeatedTest(value = 5, name = "Stress test iteration {currentRepetition} of {totalRepetitions}")

  @Timeout(10)
  void testRepeatedConcurrentStress() throws InterruptedException {
    final int batchSize = 15;
    final int batchCount = 3;
    final List<PlatformDetector.PlatformInfo> allResults =
        Collections.synchronizedList(new ArrayList<>());

    for (int batch = 0; batch < batchCount; batch++) {
      final CountDownLatch batchLatch = new CountDownLatch(batchSize);

      for (int i = 0; i < batchSize; i++) {
        executorService.submit(
            () -> {
              try {
                final PlatformDetector.PlatformInfo result = PlatformDetector.detect();
                allResults.add(result);
              } finally {
                batchLatch.countDown();
              }
            });
      }

      assertTrue(
          batchLatch.await(3, TimeUnit.SECONDS),
          "Batch " + batch + " should complete within timeout");
    }

    // Verify all results are consistent
    final int expectedResultCount = batchSize * batchCount;
    assertEquals(expectedResultCount, allResults.size(), "Should have all expected results");

    final PlatformDetector.PlatformInfo firstResult = allResults.get(0);
    for (final PlatformDetector.PlatformInfo result : allResults) {
      assertSame(firstResult, result, "All stress test results should be identical");
    }
  }

  @Test

  @Timeout(10)
  void testMemoryConsistency() throws InterruptedException, ExecutionException {
    final int threadCount = 25;
    final AtomicReference<PlatformDetector.PlatformInfo> sharedReference = new AtomicReference<>();
    final List<Future<Boolean>> futures = new ArrayList<>();

    // First thread sets the shared reference
    final Future<Boolean> setterFuture =
        executorService.submit(
            () -> {
              final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
              sharedReference.set(info);
              return true;
            });
    setterFuture.get(); // Wait for setter to complete

    // Other threads verify they see the same instance
    for (int i = 0; i < threadCount; i++) {
      final Future<Boolean> future =
          executorService.submit(
              () -> {
                final PlatformDetector.PlatformInfo detected = PlatformDetector.detect();
                final PlatformDetector.PlatformInfo shared = sharedReference.get();
                return detected == shared; // Identity comparison
              });
      futures.add(future);
    }

    // All threads should see memory-consistent values
    for (final Future<Boolean> future : futures) {
      assertTrue(
          future.get(), "All threads should see memory-consistent platform detection results");
    }
  }

  @Test

  @Timeout(10)
  void testConcurrentMethodVariants() throws InterruptedException, ExecutionException {
    final int threadCount = 30;
    final List<Future<String>> futures = new ArrayList<>();

    // Test concurrent access to different detection methods
    for (int i = 0; i < threadCount; i++) {
      final int methodIndex = i % 4; // Rotate through 4 different method calls
      final Future<String> future =
          executorService.submit(
              () -> {
                switch (methodIndex) {
                  case 0:
                    return PlatformDetector.detect().toString();
                  case 1:
                    return PlatformDetector.detectOperatingSystem().toString();
                  case 2:
                    return PlatformDetector.detectArchitecture().toString();
                  case 3:
                    return Boolean.toString(PlatformDetector.isPlatformSupported());
                  default:
                    return PlatformDetector.getPlatformDescription();
                }
              });
      futures.add(future);
    }

    // Collect all results
    final List<String> results = new ArrayList<>();
    for (final Future<String> future : futures) {
      results.add(future.get());
    }

    // Verify we got results from all threads
    assertEquals(threadCount, results.size(), "Should have results from all threads");

    // Verify consistency - group results by method type and check they're consistent
    final PlatformDetector.PlatformInfo expectedInfo = PlatformDetector.detect();
    for (int i = 0; i < threadCount; i++) {
      final int methodIndex = i % 4;
      final String result = results.get(i);
      assertNotNull(result, "Result " + i + " should not be null");

      switch (methodIndex) {
        case 0:
          assertEquals(expectedInfo.toString(), result, "detect() results should be consistent");
          break;
        case 1:
          assertEquals(
              expectedInfo.getOperatingSystem().toString(),
              result,
              "detectOperatingSystem() results should be consistent");
          break;
        case 2:
          assertEquals(
              expectedInfo.getArchitecture().toString(),
              result,
              "detectArchitecture() results should be consistent");
          break;
        case 3:
          assertEquals(
              "true", result, "isPlatformSupported() should return true for current platform");
          break;

        default:
          // Should not reach here with known method indices
          throw new IllegalStateException("Unknown method index: " + methodIndex);
      }
    }
  }

  @Test

  @Timeout(10)
  void testInterruptionHandling() throws InterruptedException {
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch interruptedLatch = new CountDownLatch(1);
    final AtomicReference<Exception> caughtException = new AtomicReference<>();

    final Thread workerThread =
        new Thread(
            () -> {
              try {
                startLatch.await();
                // This should complete quickly due to caching, but test interruption handling
                final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
                // If we reach here without interruption, that's also fine
              } catch (final InterruptedException e) {
                caughtException.set(e);
                Thread.currentThread().interrupt(); // Restore interrupted status
              } finally {
                interruptedLatch.countDown();
              }
            });

    workerThread.start();
    startLatch.countDown();

    // Interrupt the thread shortly after it starts
    Thread.sleep(10);
    workerThread.interrupt();

    assertTrue(
        interruptedLatch.await(2, TimeUnit.SECONDS),
        "Worker thread should complete or be interrupted within timeout");

    // Thread should handle interruption gracefully (either complete normally or handle
    // interruption)
    // No assertion needed - the test passes if no exceptions are thrown
  }
}
