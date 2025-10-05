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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Performance baseline tests for native library loading components.
 *
 * <p>This test class establishes performance baselines and prepares for JMH benchmarks by:
 *
 * <ul>
 *   <li>Measuring platform detection performance
 *   <li>Testing cache efficiency
 *   <li>Establishing performance bounds
 *   <li>Identifying performance bottlenecks
 *   <li>Preparing test scenarios for JMH benchmarks
 * </ul>
 *
 * <p>These tests are designed to be deterministic and provide consistent baseline measurements
 * across different environments.
 */
final class PerformanceBaselineTest {

  private static final int WARMUP_ITERATIONS = 1000;
  private static final int MEASUREMENT_ITERATIONS = 10000;
  private static final long MAX_SINGLE_DETECTION_NANOS =
      TimeUnit.MILLISECONDS.toNanos(100); // 100ms
  private static final long MAX_CACHED_DETECTION_NANOS =
      TimeUnit.MICROSECONDS.toNanos(100); // 100μs

  @BeforeEach
  void setUp() {
    // Ensure clean state for each test
    PlatformDetectorTestUtils.clearCache();
  }

  @AfterEach
  void tearDown() {
    PlatformDetectorTestUtils.clearCache();
  }

  @Test
  @Timeout(30)
  void testInitialDetectionPerformanceBaseline() {
    // Warmup JVM
    for (int i = 0; i < 100; i++) {
      PlatformDetectorTestUtils.clearCache();
      PlatformDetector.detect();
    }

    // Clear cache for actual measurement
    PlatformDetectorTestUtils.clearCache();

    // Measure initial detection time (cold cache)
    final Instant startTime = Instant.now();
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    final Instant endTime = Instant.now();
    final long durationNanos = Duration.between(startTime, endTime).toNanos();

    // Verify the detection worked
    assertTrue(info != null, "Platform detection should succeed");
    assertTrue(info.getOperatingSystem() != null, "Operating system should be detected");
    assertTrue(info.getArchitecture() != null, "Architecture should be detected");

    // Performance assertion
    assertTrue(
        durationNanos < MAX_SINGLE_DETECTION_NANOS,
        String.format(
            "Initial platform detection should complete within %d ms (took %d ns)",
            TimeUnit.NANOSECONDS.toMillis(MAX_SINGLE_DETECTION_NANOS), durationNanos));

    // Log performance for analysis
    System.out.printf(
        "Initial platform detection baseline: %d ns (%.2f ms)%n",
        durationNanos, durationNanos / 1_000_000.0);
  }

  @Test
  @Timeout(10)
  void testCachedDetectionPerformanceBaseline() {
    // Initialize cache
    PlatformDetector.detect();

    // Warmup cached access
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      PlatformDetector.detect();
    }

    // Measure cached detection times
    final List<Long> measurements = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      final long startTime = System.nanoTime();
      final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
      final long endTime = System.nanoTime();
      final long duration = endTime - startTime;
      measurements.add(duration);

      // Quick sanity check
      assertTrue(info != null, "Cached detection should always succeed");
    }

    // Calculate statistics
    Collections.sort(measurements);
    final long minTime = measurements.get(0);
    final long maxTime = measurements.get(measurements.size() - 1);
    final long medianTime = measurements.get(measurements.size() / 2);
    final double averageTime =
        measurements.stream().mapToLong(Long::longValue).average().orElse(0.0);
    final long p95Time = measurements.get((int) (measurements.size() * 0.95));

    // Performance assertions
    assertTrue(
        medianTime < MAX_CACHED_DETECTION_NANOS,
        String.format(
            "Median cached detection should be under %d μs (was %d ns)",
            TimeUnit.NANOSECONDS.toMicros(MAX_CACHED_DETECTION_NANOS), medianTime));

    assertTrue(
        p95Time < MAX_CACHED_DETECTION_NANOS * 2,
        String.format(
            "95th percentile cached detection should be reasonable (was %d ns)", p95Time));

    // Log performance statistics
    System.out.printf(
        "Cached detection baseline - Min: %d ns, Median: %d ns, Avg: %.1f ns, P95: %d ns, Max: %d"
            + " ns%n",
        minTime, medianTime, averageTime, p95Time, maxTime);
  }

  @Test
  @Timeout(10)
  void testPlatformInfoMethodPerformanceBaseline() {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    final String testLibraryName = "wasmtime4j";

    // Warmup
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      info.getPlatformId();
      info.getLibraryFileName(testLibraryName);
      info.getLibraryResourcePath(testLibraryName);
      info.toString();
      info.hashCode();
    }

    // Test getPlatformId performance
    final long platformIdStartTime = System.nanoTime();
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      info.getPlatformId();
    }
    final long platformIdTime = System.nanoTime() - platformIdStartTime;

    // Test getLibraryFileName performance
    final long fileNameStartTime = System.nanoTime();
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      info.getLibraryFileName(testLibraryName);
    }
    final long fileNameTime = System.nanoTime() - fileNameStartTime;

    // Test getLibraryResourcePath performance
    final long resourcePathStartTime = System.nanoTime();
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      info.getLibraryResourcePath(testLibraryName);
    }
    final long resourcePathTime = System.nanoTime() - resourcePathStartTime;

    // Calculate per-operation times
    final long platformIdPerOp = platformIdTime / MEASUREMENT_ITERATIONS;
    final long fileNamePerOp = fileNameTime / MEASUREMENT_ITERATIONS;
    final long resourcePathPerOp = resourcePathTime / MEASUREMENT_ITERATIONS;

    // Performance assertions (these should be very fast)
    final long maxMethodTime = TimeUnit.MICROSECONDS.toNanos(1); // 1μs per operation
    assertTrue(
        platformIdPerOp < maxMethodTime,
        String.format("getPlatformId should be under 1μs per call (was %d ns)", platformIdPerOp));
    assertTrue(
        fileNamePerOp < maxMethodTime * 2,
        String.format(
            "getLibraryFileName should be under 2μs per call (was %d ns)", fileNamePerOp));
    assertTrue(
        resourcePathPerOp < maxMethodTime * 3,
        String.format(
            "getLibraryResourcePath should be under 3μs per call (was %d ns)", resourcePathPerOp));

    System.out.printf(
        "PlatformInfo method baselines - getPlatformId: %d ns, getLibraryFileName: %d ns,"
            + " getLibraryResourcePath: %d ns%n",
        platformIdPerOp, fileNamePerOp, resourcePathPerOp);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 10, 50, 100, 500})
  @Timeout(30)
  void testRepeatedDetectionBaseline(final int iterations) {
    // Clear cache
    PlatformDetectorTestUtils.clearCache();

    final List<Long> detectionTimes = new ArrayList<>();

    for (int i = 0; i < iterations; i++) {
      if (i > 0) {
        // Clear cache for each iteration except the first to test cache initialization
        PlatformDetectorTestUtils.clearCache();
      }

      final long startTime = System.nanoTime();
      final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
      final long endTime = System.nanoTime();
      final long duration = endTime - startTime;
      detectionTimes.add(duration);

      // Verify detection worked
      assertTrue(info != null, "Detection should succeed in iteration " + i);
    }

    // Calculate statistics
    final long totalTime = detectionTimes.stream().mapToLong(Long::longValue).sum();
    final double averageTime = (double) totalTime / iterations;
    final long minTime = Collections.min(detectionTimes);
    final long maxTime = Collections.max(detectionTimes);

    // Performance bounds scale with iteration count
    final long maxTotalTime = MAX_SINGLE_DETECTION_NANOS * iterations;
    assertTrue(
        totalTime < maxTotalTime,
        String.format(
            "Total time for %d iterations should be under %d ms (was %d ns)",
            iterations, TimeUnit.NANOSECONDS.toMillis(maxTotalTime), totalTime));

    System.out.printf(
        "Repeated detection (%d iterations) - Total: %d ns, Avg: %.1f ns, Min: %d ns, Max: %d ns%n",
        iterations, totalTime, averageTime, minTime, maxTime);
  }

  @Test
  @Timeout(15)
  void testConcurrentDetectionBaseline() {
    final int threadCount = Runtime.getRuntime().availableProcessors();
    final Thread[] threads = new Thread[threadCount];
    final long[] threadTimes = new long[threadCount];
    final PlatformDetector.PlatformInfo[] results = new PlatformDetector.PlatformInfo[threadCount];

    // Clear cache before concurrent test
    PlatformDetectorTestUtils.clearCache();

    final long overallStartTime = System.nanoTime();

    // Start all threads
    for (int i = 0; i < threadCount; i++) {
      final int threadIndex = i;
      threads[i] =
          new Thread(
              () -> {
                final long threadStartTime = System.nanoTime();
                results[threadIndex] = PlatformDetector.detect();
                threadTimes[threadIndex] = System.nanoTime() - threadStartTime;
              });
      threads[i].start();
    }

    // Wait for all threads to complete
    for (final Thread thread : threads) {
      try {
        thread.join();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Thread interrupted during concurrent baseline test", e);
      }
    }

    final long overallEndTime = System.nanoTime();
    final long overallTime = overallEndTime - overallStartTime;

    // Verify all threads succeeded
    for (int i = 0; i < threadCount; i++) {
      assertTrue(results[i] != null, "Thread " + i + " should have successful detection result");
      assertTrue(
          threadTimes[i] >= 0, "Thread " + i + " should have recorded non-negative execution time");
    }

    // Calculate thread time statistics
    long minThreadTime = threadTimes[0];
    long maxThreadTime = threadTimes[0];
    long totalThreadTime = threadTimes[0];

    for (int i = 1; i < threadCount; i++) {
      minThreadTime = Math.min(minThreadTime, threadTimes[i]);
      maxThreadTime = Math.max(maxThreadTime, threadTimes[i]);
      totalThreadTime += threadTimes[i];
    }

    final double averageThreadTime = (double) totalThreadTime / threadCount;

    // Performance assertion - concurrent access should be reasonably efficient
    final long maxConcurrentOverhead = MAX_SINGLE_DETECTION_NANOS * 2; // Allow some overhead
    assertTrue(
        overallTime < maxConcurrentOverhead,
        String.format(
            "Concurrent detection with %d threads should complete within %d ms (took %d ns)",
            threadCount, TimeUnit.NANOSECONDS.toMillis(maxConcurrentOverhead), overallTime));

    System.out.printf(
        "Concurrent detection baseline (%d threads) - Overall: %d ns, Thread times - Min: %d ns,"
            + " Avg: %.1f ns, Max: %d ns%n",
        threadCount, overallTime, minThreadTime, averageThreadTime, maxThreadTime);
  }

  @Test
  @Timeout(10)
  void testMemoryAllocationBaseline() {
    // Force garbage collection before measurement
    System.gc();
    Thread.yield();
    System.gc();

    final Runtime runtime = Runtime.getRuntime();
    final long initialMemory = runtime.totalMemory() - runtime.freeMemory();

    // Perform many detection operations to observe memory patterns
    final int iterations = 1000;
    for (int i = 0; i < iterations; i++) {
      if (i % 100 == 0) {
        PlatformDetectorTestUtils.clearCache();
      }
      final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
      // Use the result to prevent dead code elimination
      info.getPlatformId();
    }

    // Force garbage collection after operations
    System.gc();
    Thread.yield();
    System.gc();

    final long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    final long memoryDelta = finalMemory - initialMemory;

    // Memory usage should be reasonable (allowing for some JVM overhead)
    final long maxMemoryIncrease = 1024 * 1024; // 1MB
    assertTrue(
        memoryDelta < maxMemoryIncrease,
        String.format("Memory usage increase should be under 1MB (was %d bytes)", memoryDelta));

    System.out.printf(
        "Memory allocation baseline - Initial: %d bytes, Final: %d bytes, Delta: %d bytes%n",
        initialMemory, finalMemory, memoryDelta);
  }

  @Test
  @Timeout(5)
  void testJmhBenchmarkPreparation() {
    // This test prepares scenarios and collects data for JMH benchmarks
    System.out.println("=== JMH Benchmark Preparation Data ===");

    // Platform detection scenario
    PlatformDetectorTestUtils.clearCache();
    final long coldDetectionTime = System.nanoTime();
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    final long coldDetectionDuration = System.nanoTime() - coldDetectionTime;

    final long warmDetectionTime = System.nanoTime();
    PlatformDetector.detect();
    final long warmDetectionDuration = System.nanoTime() - warmDetectionTime;

    System.out.printf("Cold detection: %d ns%n", coldDetectionDuration);
    System.out.printf("Warm detection: %d ns%n", warmDetectionDuration);
    System.out.printf("Platform: %s%n", info.getPlatformId());
    System.out.printf("Available processors: %d%n", Runtime.getRuntime().availableProcessors());
    System.out.printf("Java version: %s%n", System.getProperty("java.version"));
    System.out.printf("JVM name: %s%n", System.getProperty("java.vm.name"));
    System.out.println("========================================");

    // Basic assertions to ensure the preparation worked
    assertTrue(coldDetectionDuration > 0, "Cold detection should take measurable time");
    assertTrue(warmDetectionDuration >= 0, "Warm detection time should be non-negative");
    assertTrue(
        coldDetectionDuration >= warmDetectionDuration,
        "Cold detection should take at least as long as warm detection");
  }
}
