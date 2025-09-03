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

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for native library loading performance.
 *
 * <p>This benchmark measures the performance overhead of the NativeLoader compared to direct
 * System.loadLibrary() calls, focusing on:
 *
 * <ul>
 *   <li>Initial library loading (cold start)
 *   <li>Cached library access (warm start)
 *   <li>Builder pattern configuration overhead
 *   <li>Multi-threaded loading scenarios
 * </ul>
 *
 * <p>Performance targets from requirements:
 *
 * <ul>
 *   <li>Loading Overhead: <5% compared to System.loadLibrary()
 *   <li>Concurrent Performance: <10% degradation under 10 concurrent threads
 *   <li>Cache Hit Performance: <1% overhead for cached library access
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class NativeLoaderLoadingBenchmark {

  private static final String TEST_LIBRARY_NAME = "wasmtime4j";
  private static final String ALT_LIBRARY_NAME = "wasmtime4j-test";

  /** Sets up the benchmark environment with clean state. */
  @Setup(Level.Trial)
  public void setupTrial() {
    // Clear any cached data to ensure clean state
    PlatformDetectorTestUtils.clearCache();

    // Pre-warm the platform detection to avoid one-time initialization overhead
    PlatformDetector.detect();

    System.out.printf(
        "Starting NativeLoaderLoadingBenchmark on platform: %s%n",
        PlatformDetector.detect().getPlatformId());
  }

  /** Clean up after benchmark trial. */
  @TearDown(Level.Trial)
  public void tearDownTrial() {
    PlatformDetectorTestUtils.clearCache();
  }

  /** Setup for each iteration to ensure consistent state. */
  @Setup(Level.Iteration)
  public void setupIteration() {
    // Clear platform detector cache to test cold loading
    PlatformDetectorTestUtils.clearCache();
  }

  /**
   * Benchmark: Simple library loading using static method (cold cache). This tests the most common
   * usage pattern with no caching benefits.
   */
  @Benchmark
  public void benchmarkSimpleLoadLibraryColdCache(final Blackhole blackhole) {
    // Clear cache to simulate cold start
    PlatformDetectorTestUtils.clearCache();

    final NativeLibraryUtils.LibraryLoadInfo loadInfo = NativeLoader.loadLibrary(TEST_LIBRARY_NAME);

    blackhole.consume(loadInfo);
  }

  /**
   * Benchmark: Simple library loading using static method (warm cache). This tests performance with
   * platform detection already cached.
   */
  @Benchmark
  public void benchmarkSimpleLoadLibraryWarmCache(final Blackhole blackhole) {
    // Ensure platform detection is cached
    PlatformDetector.detect();

    final NativeLibraryUtils.LibraryLoadInfo loadInfo = NativeLoader.loadLibrary(TEST_LIBRARY_NAME);

    blackhole.consume(loadInfo);
  }

  /**
   * Benchmark: Builder pattern with default configuration. This tests the overhead of the builder
   * pattern compared to static methods.
   */
  @Benchmark
  public void benchmarkBuilderDefaultConfiguration(final Blackhole blackhole) {
    final NativeLibraryUtils.LibraryLoadInfo loadInfo =
        NativeLoader.builder().libraryName(TEST_LIBRARY_NAME).load();

    blackhole.consume(loadInfo);
  }

  /**
   * Benchmark: Builder pattern with custom configuration. This tests the full builder configuration
   * overhead.
   */
  @Benchmark
  public void benchmarkBuilderCustomConfiguration(final Blackhole blackhole) {
    final NativeLibraryUtils.LibraryLoadInfo loadInfo =
        NativeLoader.builder()
            .libraryName(TEST_LIBRARY_NAME)
            .tempFilePrefix("benchmark-")
            .resourcePathConvention(PathConvention.MAVEN_NATIVE)
            .load();

    blackhole.consume(loadInfo);
  }

  /**
   * Benchmark: Repeated loads of the same library. This tests caching efficiency and repeated
   * access performance.
   */
  @Benchmark
  public void benchmarkRepeatedLoads(final Blackhole blackhole) {
    // Load the same library multiple times to test caching
    for (int i = 0; i < 5; i++) {
      final NativeLibraryUtils.LibraryLoadInfo loadInfo =
          NativeLoader.loadLibrary(TEST_LIBRARY_NAME);
      blackhole.consume(loadInfo);
    }
  }

  /**
   * Benchmark: Loading different libraries. This tests the overhead when no caching benefits apply.
   */
  @Benchmark
  public void benchmarkDifferentLibraries(final Blackhole blackhole) {
    final NativeLibraryUtils.LibraryLoadInfo loadInfo1 =
        NativeLoader.loadLibrary(TEST_LIBRARY_NAME);
    final NativeLibraryUtils.LibraryLoadInfo loadInfo2 = NativeLoader.loadLibrary(ALT_LIBRARY_NAME);

    blackhole.consume(loadInfo1);
    blackhole.consume(loadInfo2);
  }

  /**
   * Benchmark: Multi-threaded library loading with 4 threads. This tests concurrent performance
   * characteristics.
   */
  @Benchmark
  @Threads(4)
  public void benchmarkConcurrentLoading4Threads(final Blackhole blackhole) {
    final NativeLibraryUtils.LibraryLoadInfo loadInfo = NativeLoader.loadLibrary(TEST_LIBRARY_NAME);

    blackhole.consume(loadInfo);
  }

  /**
   * Benchmark: Multi-threaded library loading with 10 threads. This tests the target scenario from
   * requirements (10 concurrent threads).
   */
  @Benchmark
  @Threads(10)
  public void benchmarkConcurrentLoading10Threads(final Blackhole blackhole) {
    final NativeLibraryUtils.LibraryLoadInfo loadInfo = NativeLoader.loadLibrary(TEST_LIBRARY_NAME);

    blackhole.consume(loadInfo);
  }

  /**
   * Benchmark: Platform detection performance separately. This isolates the platform detection
   * overhead from library loading.
   */
  @Benchmark
  public void benchmarkPlatformDetectionOnly(final Blackhole blackhole) {
    // Clear cache to test detection performance
    PlatformDetectorTestUtils.clearCache();

    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();

    blackhole.consume(platformInfo);
  }

  /**
   * Benchmark: Platform detection with warm cache. This tests cached platform detection
   * performance.
   */
  @Benchmark
  public void benchmarkCachedPlatformDetection(final Blackhole blackhole) {
    // Ensure cache is warmed
    PlatformDetector.detect();

    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();

    blackhole.consume(platformInfo);
  }

  /**
   * Benchmark: Configuration object creation overhead. This tests the cost of creating
   * NativeLibraryConfig instances.
   */
  @Benchmark
  public void benchmarkConfigurationCreation(final Blackhole blackhole) {
    final NativeLibraryConfig config =
        NativeLibraryConfig.builder()
            .libraryName(TEST_LIBRARY_NAME)
            .tempFilePrefix("benchmark-")
            .pathConventions(PathConvention.MAVEN_NATIVE, PathConvention.GRAALVM_NATIVE_IMAGE)
            .build();

    blackhole.consume(config);
  }
}
