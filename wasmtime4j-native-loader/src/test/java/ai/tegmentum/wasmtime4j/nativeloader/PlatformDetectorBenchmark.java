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
 * JMH benchmarks for platform detection overhead.
 *
 * <p>This benchmark measures the performance characteristics of platform detection operations,
 * which are critical for native library loading performance. Key measurements include:
 *
 * <ul>
 *   <li>Cold platform detection (first invocation)
 *   <li>Cached platform detection (subsequent invocations)
 *   <li>PlatformInfo method call overhead
 *   <li>Concurrent platform detection performance
 *   <li>Memory allocation patterns during detection
 * </ul>
 *
 * <p>Performance targets:
 *
 * <ul>
 *   <li>Cache hit rate: > 95% for repeated detection calls
 *   <li>Cached detection overhead: < 1μs per call
 *   <li>Cold detection: < 100ms first call
 *   <li>Concurrent performance: < 10% degradation under load
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class PlatformDetectorBenchmark {

  private static final String[] TEST_LIBRARY_NAMES = {
    "wasmtime4j", "testlib", "mylib", "nativelib", "benchmark"
  };

  private PlatformDetector.PlatformInfo platformInfo;

  /** Sets up the benchmark environment. */
  @Setup(Level.Trial)
  public void setupTrial() {
    PlatformDetectorTestUtils.clearCache();

    // Pre-warm the platform detection once to establish baseline
    this.platformInfo = PlatformDetector.detect();

    System.out.printf(
        "Platform detection benchmark running on: %s%n", platformInfo.getPlatformId());
  }

  /** Clean up after benchmark trial. */
  @TearDown(Level.Trial)
  public void tearDownTrial() {
    PlatformDetectorTestUtils.clearCache();
  }

  /**
   * Benchmark: Cold platform detection (cache cleared). This measures the cost of the first
   * platform detection call.
   */
  @Benchmark
  public void benchmarkColdPlatformDetection(final Blackhole blackhole) {
    PlatformDetectorTestUtils.clearCache();
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    blackhole.consume(info);
  }

  /**
   * Benchmark: Warm platform detection (cached result). This measures the overhead of accessing
   * cached platform information.
   */
  @Benchmark
  public void benchmarkWarmPlatformDetection(final Blackhole blackhole) {
    // Ensure cache is populated
    PlatformDetector.detect();

    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    blackhole.consume(info);
  }

  /**
   * Benchmark: getPlatformId method performance. This tests the overhead of the most commonly used
   * platform info method.
   */
  @Benchmark
  public void benchmarkGetPlatformId(final Blackhole blackhole) {
    final String platformId = platformInfo.getPlatformId();
    blackhole.consume(platformId);
  }

  /** Benchmark: getOperatingSystem method performance. */
  @Benchmark
  public void benchmarkGetOperatingSystem(final Blackhole blackhole) {
    final PlatformDetector.OperatingSystem os = platformInfo.getOperatingSystem();
    blackhole.consume(os);
  }

  /** Benchmark: getArchitecture method performance. */
  @Benchmark
  public void benchmarkGetArchitecture(final Blackhole blackhole) {
    final PlatformDetector.Architecture arch = platformInfo.getArchitecture();
    blackhole.consume(arch);
  }

  /**
   * Benchmark: getLibraryFileName method with various library names. This tests the string
   * manipulation overhead for different library names.
   */
  @Benchmark
  public void benchmarkGetLibraryFileName(final Blackhole blackhole) {
    for (final String libraryName : TEST_LIBRARY_NAMES) {
      final String fileName = platformInfo.getLibraryFileName(libraryName);
      blackhole.consume(fileName);
    }
  }

  /**
   * Benchmark: getLibraryResourcePath method with various library names. This tests the resource
   * path generation overhead.
   */
  @Benchmark
  public void benchmarkGetLibraryResourcePath(final Blackhole blackhole) {
    for (final String libraryName : TEST_LIBRARY_NAMES) {
      final String resourcePath = platformInfo.getLibraryResourcePath(libraryName);
      blackhole.consume(resourcePath);
    }
  }

  /**
   * Benchmark: toString method performance. This tests the overhead of string representation
   * generation.
   */
  @Benchmark
  public void benchmarkToString(final Blackhole blackhole) {
    final String str = platformInfo.toString();
    blackhole.consume(str);
  }

  /** Benchmark: hashCode method performance. This tests the overhead of hash code computation. */
  @Benchmark
  public void benchmarkHashCode(final Blackhole blackhole) {
    final int hash = platformInfo.hashCode();
    blackhole.consume(hash);
  }

  /** Benchmark: equals method performance. This tests the overhead of equality comparison. */
  @Benchmark
  public void benchmarkEquals(final Blackhole blackhole) {
    final boolean equals = platformInfo.equals(platformInfo);
    blackhole.consume(equals);
  }

  /**
   * Benchmark: Concurrent platform detection with 4 threads. This tests thread safety and
   * concurrent access performance.
   */
  @Benchmark
  @Threads(4)
  public void benchmarkConcurrentDetection4Threads(final Blackhole blackhole) {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    blackhole.consume(info);
  }

  /**
   * Benchmark: Concurrent platform detection with 10 threads. This tests the performance under high
   * concurrent load.
   */
  @Benchmark
  @Threads(10)
  public void benchmarkConcurrentDetection10Threads(final Blackhole blackhole) {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    blackhole.consume(info);
  }

  /**
   * Benchmark: Concurrent method access on shared PlatformInfo. This tests the thread safety of
   * PlatformInfo method calls.
   */
  @Benchmark
  @Threads(8)
  public void benchmarkConcurrentMethodAccess(final Blackhole blackhole) {
    final String platformId = platformInfo.getPlatformId();
    final String fileName = platformInfo.getLibraryFileName("testlib");
    final String resourcePath = platformInfo.getLibraryResourcePath("testlib");

    blackhole.consume(platformId);
    blackhole.consume(fileName);
    blackhole.consume(resourcePath);
  }

  /**
   * Benchmark: Repeated detection calls with cache management. This simulates a realistic usage
   * pattern of detection with periodic cache clearing.
   */
  @Benchmark
  public void benchmarkRepeatedDetectionWithCacheCycling(final Blackhole blackhole) {
    // Simulate cache cycling every 10 calls
    for (int i = 0; i < 10; i++) {
      if (i == 0) {
        PlatformDetectorTestUtils.clearCache();
      }
      final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
      blackhole.consume(info);
    }
  }

  /**
   * Benchmark: Platform detection with immediate method usage. This tests the combined overhead of
   * detection and immediate method access.
   */
  @Benchmark
  public void benchmarkDetectionWithImmediateUsage(final Blackhole blackhole) {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();

    final String platformId = info.getPlatformId();
    final String fileName = info.getLibraryFileName("testlib");
    final String resourcePath = info.getLibraryResourcePath("testlib");

    blackhole.consume(platformId);
    blackhole.consume(fileName);
    blackhole.consume(resourcePath);
  }
}
