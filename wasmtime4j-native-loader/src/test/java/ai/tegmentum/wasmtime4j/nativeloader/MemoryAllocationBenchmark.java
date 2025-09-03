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

import java.util.ArrayList;
import java.util.List;
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
 * JMH benchmarks for memory allocation and garbage collection overhead.
 *
 * <p>This benchmark measures the memory allocation patterns and garbage collection impact of native
 * library loading operations. Key measurements include:
 *
 * <ul>
 *   <li>Memory allocation during library loading operations
 *   <li>Garbage collection impact of repeated operations
 *   <li>Memory footprint of cached data structures
 *   <li>Memory efficiency of configuration objects
 *   <li>Long-term memory behavior patterns
 * </ul>
 *
 * <p>Performance targets from requirements:
 *
 * <ul>
 *   <li>Memory overhead: <1MB additional heap usage per library
 *   <li>GC impact: Minimal garbage generation during normal operations
 *   <li>Memory efficiency: >90% reuse of cached objects
 *   <li>Long-term stability: No memory leaks over extended usage
 * </ul>
 *
 * <p><strong>Note:</strong> This benchmark focuses on allocation rates and patterns rather than
 * absolute memory usage, as JMH provides better control over allocation measurement than direct
 * heap inspection.
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 3,
    jvmArgs = {"-XX:+UseG1GC", "-Xmx2g", "-Xms1g"})
@State(Scope.Benchmark)
public class MemoryAllocationBenchmark {

  private static final String TEST_LIBRARY_NAME = "wasmtime4j";
  private static final int ALLOCATION_ITERATIONS = 100;

  private Runtime runtime;
  private List<Object> retainedObjects;

  /** Sets up the benchmark environment with memory monitoring. */
  @Setup(Level.Trial)
  public void setupTrial() {
    this.runtime = Runtime.getRuntime();
    this.retainedObjects = new ArrayList<>();

    // Force initial GC to establish clean baseline
    forceGarbageCollection();

    final long initialMemory = runtime.totalMemory() - runtime.freeMemory();
    System.out.printf("Memory allocation benchmark starting with %d bytes in use%n", initialMemory);
  }

  /** Clean up after benchmark trial. */
  @TearDown(Level.Trial)
  public void tearDownTrial() {
    // Clear retained objects and force GC
    retainedObjects.clear();
    forceGarbageCollection();

    final long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    System.out.printf("Memory allocation benchmark completed with %d bytes in use%n", finalMemory);
  }

  /** Setup for each iteration to ensure consistent memory state. */
  @Setup(Level.Iteration)
  public void setupIteration() {
    // Clear any iteration-specific state
    retainedObjects.clear();

    // Suggest garbage collection (but don't force it during iterations)
    System.gc();
  }

  /**
   * Benchmark: Memory allocation during simple library loading. This measures the allocation
   * overhead of basic NativeLoader operations.
   */
  @Benchmark
  public void benchmarkSimpleLoadingAllocation(final Blackhole blackhole) {
    final NativeLibraryUtils.LibraryLoadInfo loadInfo = NativeLoader.loadLibrary(TEST_LIBRARY_NAME);
    blackhole.consume(loadInfo);
  }

  /**
   * Benchmark: Memory allocation during configuration creation. This measures the allocation
   * overhead of creating configuration objects.
   */
  @Benchmark
  public void benchmarkConfigurationAllocation(final Blackhole blackhole) {
    final NativeLibraryConfig config =
        NativeLibraryConfig.builder()
            .libraryName(TEST_LIBRARY_NAME)
            .tempFilePrefix("benchmark-")
            .pathConventions(PathConvention.MAVEN_NATIVE, PathConvention.GRAALVM_NATIVE_IMAGE)
            .build();
    blackhole.consume(config);
  }

  /**
   * Benchmark: Memory allocation during platform detection. This measures the allocation overhead
   * of platform detection operations.
   */
  @Benchmark
  public void benchmarkPlatformDetectionAllocation(final Blackhole blackhole) {
    // Clear cache to force new allocation
    PlatformDetectorTestUtils.clearCache();

    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
    blackhole.consume(platformInfo);
  }

  /**
   * Benchmark: Repeated allocations with potential caching. This tests memory efficiency when
   * operations can benefit from caching.
   */
  @Benchmark
  public void benchmarkRepeatedAllocationsWithCaching(final Blackhole blackhole) {
    for (int i = 0; i < 10; i++) {
      final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();

      blackhole.consume(platformInfo);
      blackhole.consume(config);
    }
  }

  /**
   * Benchmark: Bulk object creation to test allocation rate. This measures sustained allocation
   * performance.
   */
  @Benchmark
  public void benchmarkBulkObjectCreation(final Blackhole blackhole) {
    final List<NativeLibraryConfig> configs = new ArrayList<>();

    for (int i = 0; i < ALLOCATION_ITERATIONS; i++) {
      final NativeLibraryConfig config =
          NativeLibraryConfig.builder()
              .libraryName(TEST_LIBRARY_NAME + i)
              .tempFilePrefix("bulk-" + i + "-")
              .build();
      configs.add(config);
    }

    blackhole.consume(configs);
  }

  /**
   * Benchmark: Memory allocation with immediate deallocation. This tests garbage collection
   * efficiency of short-lived objects.
   */
  @Benchmark
  public void benchmarkAllocationWithImmediateDeallocation(final Blackhole blackhole) {
    for (int i = 0; i < 50; i++) {
      final NativeLibraryConfig config =
          NativeLibraryConfig.builder()
              .libraryName(TEST_LIBRARY_NAME)
              .tempFilePrefix("temp-" + i + "-")
              .build();

      // Use the object briefly then let it become eligible for GC
      blackhole.consume(config.getLibraryName());
    }
  }

  /**
   * Benchmark: Long-lived object retention. This tests the memory impact of retaining objects
   * across operations.
   */
  @Benchmark
  public void benchmarkLongLivedObjectRetention(final Blackhole blackhole) {
    // Create objects and retain references
    final NativeLibraryConfig config =
        NativeLibraryConfig.builder()
            .libraryName(TEST_LIBRARY_NAME)
            .tempFilePrefix("retained-")
            .build();

    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();

    // Retain objects to prevent GC
    retainedObjects.add(config);
    retainedObjects.add(platformInfo);

    blackhole.consume(config);
    blackhole.consume(platformInfo);
  }

  /**
   * Benchmark: String allocation during path operations. This measures the string allocation
   * overhead of path resolution.
   */
  @Benchmark
  public void benchmarkStringAllocationDuringPathOperations(final Blackhole blackhole) {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();

    // Operations that create new strings
    final String platformId = platformInfo.getPlatformId();
    final String fileName = platformInfo.getLibraryFileName(TEST_LIBRARY_NAME);
    final String resourcePath = platformInfo.getLibraryResourcePath(TEST_LIBRARY_NAME);
    final String tempPrefix = "temp-" + System.nanoTime() + "-";

    blackhole.consume(platformId);
    blackhole.consume(fileName);
    blackhole.consume(resourcePath);
    blackhole.consume(tempPrefix);
  }

  /**
   * Benchmark: Concurrent allocation with multiple threads. This tests allocation performance and
   * contention under concurrent load.
   */
  @Benchmark
  @Threads(4)
  public void benchmarkConcurrentAllocation4Threads(final Blackhole blackhole) {
    final NativeLibraryConfig config =
        NativeLibraryConfig.builder()
            .libraryName(TEST_LIBRARY_NAME)
            .tempFilePrefix("concurrent-")
            .build();

    blackhole.consume(config);
  }

  /**
   * Benchmark: Heavy concurrent allocation with 10 threads. This tests allocation performance under
   * high concurrent load.
   */
  @Benchmark
  @Threads(10)
  public void benchmarkConcurrentAllocation10Threads(final Blackhole blackhole) {
    final NativeLibraryConfig config =
        NativeLibraryConfig.builder()
            .libraryName(TEST_LIBRARY_NAME)
            .tempFilePrefix("heavy-concurrent-")
            .pathConventions(PathConvention.MAVEN_NATIVE)
            .build();

    blackhole.consume(config);
  }

  /**
   * Benchmark: Mixed allocation patterns simulating real usage. This tests a realistic mixture of
   * allocation patterns.
   */
  @Benchmark
  public void benchmarkMixedAllocationPatterns(final Blackhole blackhole) {
    // Short-lived platform detection
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();

    // Medium-lived configuration
    final NativeLibraryConfig config =
        NativeLibraryConfig.builder().libraryName(TEST_LIBRARY_NAME).build();

    // Short-lived library loading attempt
    final NativeLibraryUtils.LibraryLoadInfo loadInfo = NativeLoader.loadLibrary(TEST_LIBRARY_NAME);

    // String operations
    final String summary =
        String.format(
            "Platform: %s, Library: %s, Success: %b",
            platformInfo.getPlatformId(), config.getLibraryName(), loadInfo.isSuccessful());

    blackhole.consume(summary);
  }

  /**
   * Benchmark: Array allocation during path convention handling. This tests the allocation overhead
   * of working with path convention arrays.
   */
  @Benchmark
  public void benchmarkArrayAllocationPathConventions(final Blackhole blackhole) {
    // Test various array sizes
    final PathConvention[] single = {PathConvention.MAVEN_NATIVE};
    final PathConvention[] multiple = {
      PathConvention.MAVEN_NATIVE, PathConvention.GRAALVM_NATIVE_IMAGE
    };
    final PathConvention[] all = PathConvention.values();

    blackhole.consume(single);
    blackhole.consume(multiple);
    blackhole.consume(all);
  }

  /**
   * Benchmark: Allocation with forced garbage collection. This measures the impact of garbage
   * collection on allocation performance.
   */
  @Benchmark
  public void benchmarkAllocationWithForcedGC(final Blackhole blackhole) {
    // Allocate objects
    final List<NativeLibraryConfig> configs = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      configs.add(NativeLibraryConfig.builder().libraryName(TEST_LIBRARY_NAME + i).build());
    }

    // Force garbage collection
    System.gc();

    // Create more objects after GC
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();

    blackhole.consume(configs);
    blackhole.consume(platformInfo);
  }

  /**
   * Forces garbage collection and waits for it to complete. Used in setup/teardown to ensure clean
   * memory state.
   */
  private void forceGarbageCollection() {
    System.gc();
    Thread.yield();
    System.gc();
    Thread.yield();
    System.gc();
  }
}
