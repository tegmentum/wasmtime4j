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
 * JMH benchmarks for resource path resolution performance.
 *
 * <p>This benchmark measures the overhead of resolving resource paths for native libraries across
 * different path conventions and library names. Key measurements include:
 *
 * <ul>
 *   <li>Resource path resolution for different conventions
 *   <li>Path validation and normalization overhead
 *   <li>Multi-convention path resolution performance
 *   <li>Path caching and reuse efficiency
 *   <li>String manipulation and formatting overhead
 * </ul>
 *
 * <p>Performance targets:
 *
 * <ul>
 *   <li>Single path resolution: < 5μs per resolution
 *   <li>Multi-convention resolution: < 20μs for all conventions
 *   <li>Path validation: < 1μs per validation
 *   <li>Caching efficiency: > 90% hit rate for repeated paths
 * </ul>
 */
@SuppressWarnings({"exports", "module"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class ResourcePathResolverBenchmark {

  private static final String[] TEST_LIBRARY_NAMES = {
    "wasmtime4j",
    "mylib",
    "testlib-with-dashes",
    "complex_library_name",
    "lib",
    "verylonglibraryname",
    "a",
    "123numeric"
  };

  private PlatformDetector.PlatformInfo platformInfo;

  /** Public constructor required for JMH. */
  public ResourcePathResolverBenchmark() {
    // JMH will instantiate this class
  }

  /** Sets up the benchmark environment with platform info. */
  @Setup(Level.Trial)
  public void setupTrial() {
    this.platformInfo = PlatformDetector.detect();

    System.out.printf(
        "ResourcePathResolver benchmark initialized for platform: %s%n",
        platformInfo.getPlatformId());
  }

  /** Clean up after benchmark trial. */
  @TearDown(Level.Trial)
  public void tearDownTrial() {
    // No explicit cleanup needed
  }

  /**
   * Benchmark: Resource path resolution with MAVEN_NATIVE convention. This tests the most common
   * path convention used in Maven projects.
   */
  @Benchmark
  public void benchmarkMavenNativePathResolution(final Blackhole blackhole) {
    for (final String libraryName : TEST_LIBRARY_NAMES) {
      final String path =
          PathConvention.MAVEN_NATIVE.generatePath(libraryName, platformInfo);
      blackhole.consume(path);
    }
  }

  /**
   * Benchmark: Resource path resolution with MAVEN_NATIVE convention. This tests the
   * GraalVM native image path convention.
   */
  @Benchmark
  public void benchmarkGraalVmPathResolution(final Blackhole blackhole) {
    for (final String libraryName : TEST_LIBRARY_NAMES) {
      final String path =
          PathConvention.MAVEN_NATIVE.generatePath(libraryName, platformInfo);
      blackhole.consume(path);
    }
  }

  /**
   * Benchmark: Resource path resolution with JNA convention. This tests the Spring
   * Boot fat JAR path convention.
   */
  @Benchmark
  public void benchmarkSpringBootPathResolution(final Blackhole blackhole) {
    for (final String libraryName : TEST_LIBRARY_NAMES) {
      final String path =
          PathConvention.JNA.generatePath(libraryName, platformInfo);
      blackhole.consume(path);
    }
  }

  /**
   * Benchmark: All path conventions for single library. This tests the overhead of resolving all
   * conventions for one library.
   */
  @Benchmark
  public void benchmarkAllConventionsSingleLibrary(final Blackhole blackhole) {
    final String libraryName = "wasmtime4j";

    for (final PathConvention convention : PathConvention.values()) {
      final String path = convention.generatePath(libraryName, platformInfo);
      blackhole.consume(path);
    }
  }

  /**
   * Benchmark: Path resolution with repeated library names. This tests caching efficiency and
   * repeated access patterns.
   */
  @Benchmark
  public void benchmarkRepeatedPathResolution(final Blackhole blackhole) {
    final String libraryName = "wasmtime4j";

    // Resolve the same library multiple times to test caching
    for (int i = 0; i < 10; i++) {
      final String path =
          PathConvention.MAVEN_NATIVE.generatePath(libraryName, platformInfo);
      blackhole.consume(path);
    }
  }

  /**
   * Benchmark: Multi-convention path resolution with caching. This tests the performance when
   * multiple conventions are used repeatedly.
   */
  @Benchmark
  public void benchmarkMultiConventionWithCaching(final Blackhole blackhole) {
    final String libraryName = "wasmtime4j";

    // Cycle through conventions multiple times
    for (int cycle = 0; cycle < 3; cycle++) {
      for (final PathConvention convention : PathConvention.values()) {
        final String path = convention.generatePath(libraryName, platformInfo);
        blackhole.consume(path);
      }
    }
  }

  /**
   * Benchmark: Path validation performance. This tests the overhead of validating resolved paths.
   */
  @Benchmark
  public void benchmarkPathValidation(final Blackhole blackhole) {
    for (final String libraryName : TEST_LIBRARY_NAMES) {
      final String path =
          PathConvention.MAVEN_NATIVE.generatePath(libraryName, platformInfo);

      // Simulate path validation operations
      final boolean isValid = path != null && !path.isEmpty() && path.contains("/");
      blackhole.consume(isValid);
    }
  }

  /** Benchmark: Path normalization overhead. This tests the cost of normalizing resolved paths. */
  @Benchmark
  public void benchmarkPathNormalization(final Blackhole blackhole) {
    for (final String libraryName : TEST_LIBRARY_NAMES) {
      final String path =
          PathConvention.MAVEN_NATIVE.generatePath(libraryName, platformInfo);

      // Simulate path normalization
      final String normalized = path.replaceAll("//", "/").replaceAll("\\\\", "/");
      blackhole.consume(normalized);
    }
  }

  /**
   * Benchmark: Concurrent path resolution with 4 threads. This tests thread safety and concurrent
   * access performance.
   */
  @Benchmark
  @Threads(4)
  public void benchmarkConcurrentPathResolution4Threads(final Blackhole blackhole) {
    final String libraryName = "wasmtime4j";
    final String path =
        PathConvention.MAVEN_NATIVE.generatePath(libraryName, platformInfo);
    blackhole.consume(path);
  }

  /**
   * Benchmark: Concurrent path resolution with 10 threads. This tests performance under high
   * concurrent load.
   */
  @Benchmark
  @Threads(10)
  public void benchmarkConcurrentPathResolution10Threads(final Blackhole blackhole) {
    final String libraryName = "wasmtime4j";
    final String path =
        PathConvention.MAVEN_NATIVE.generatePath(libraryName, platformInfo);
    blackhole.consume(path);
  }

  /**
   * Benchmark: Mixed workload with different library names and conventions. This simulates a
   * realistic usage pattern with varied inputs.
   */
  @Benchmark
  public void benchmarkMixedWorkload(final Blackhole blackhole) {
    final PathConvention[] conventions = PathConvention.values();

    for (int i = 0; i < TEST_LIBRARY_NAMES.length; i++) {
      final String libraryName = TEST_LIBRARY_NAMES[i];
      final PathConvention convention = conventions[i % conventions.length];

      final String path = convention.generatePath(libraryName, platformInfo);
      blackhole.consume(path);
    }
  }

  /**
   * Benchmark: Complex library names with special characters. This tests the overhead of handling
   * complex library names.
   */
  @Benchmark
  public void benchmarkComplexLibraryNames(final Blackhole blackhole) {
    final String[] complexNames = {
      "lib-with-many-dashes-and-numbers-123",
      "library_with_underscores_and_dots.so",
      "CamelCaseLibraryName",
      "mixed-Case_Library.Name.123"
    };

    for (final String libraryName : complexNames) {
      final String path =
          PathConvention.MAVEN_NATIVE.generatePath(libraryName, platformInfo);
      blackhole.consume(path);
    }
  }

  /**
   * Benchmark: Resource path resolution with immediate usage. This tests the combined overhead of
   * resolution and immediate path operations.
   */
  @Benchmark
  public void benchmarkPathResolutionWithImmediateUsage(final Blackhole blackhole) {
    final String libraryName = "wasmtime4j";

    final String path =
        PathConvention.MAVEN_NATIVE.generatePath(libraryName, platformInfo);

    // Immediate usage operations
    final int length = path.length();
    final boolean hasExtension = path.contains(".");
    final String fileName = path.substring(path.lastIndexOf('/') + 1);

    blackhole.consume(length);
    blackhole.consume(hasExtension);
    blackhole.consume(fileName);
  }
}
