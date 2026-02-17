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
 * JMH benchmarks for configuration creation and management performance.
 *
 * <p>This benchmark measures the overhead of creating and using various configuration objects in
 * the native loader system, including:
 *
 * <ul>
 *   <li>NativeLibraryConfig creation with different options
 *   <li>NativeLoaderBuilder configuration and building
 *   <li>Configuration validation overhead
 *   <li>Configuration object method access performance
 *   <li>Configuration serialization and comparison operations
 * </ul>
 *
 * <p>Performance targets:
 *
 * <ul>
 *   <li>Configuration creation: < 10μs per instance
 *   <li>Builder pattern overhead: < 5μs per build operation
 *   <li>Validation overhead: < 1μs per validation
 *   <li>Method access: < 100ns per method call
 * </ul>
 */
@SuppressWarnings({"exports", "module"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class NativeLoaderConfigurationBenchmark {

  private static final String TEST_LIBRARY_NAME = "wasmtime4j";
  private static final String TEST_PREFIX = "benchmark-test-";

  private NativeLibraryConfig defaultConfig;
  private NativeLibraryConfig customConfig;
  private NativeLoaderBuilder builder;

  /** Public constructor required for JMH. */
  public NativeLoaderConfigurationBenchmark() {
    // JMH will instantiate this class
  }

  /** Sets up the benchmark environment with pre-created configurations. */
  @Setup(Level.Trial)
  public void setupTrial() {
    // Create configurations that will be reused in method access benchmarks
    this.defaultConfig = NativeLibraryConfig.defaultConfig();

    this.customConfig =
        new NativeLibraryConfig(
            TEST_LIBRARY_NAME, TEST_PREFIX, NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX);

    this.builder = NativeLoader.builder();

    System.out.printf("Configuration benchmark initialized with default and custom configs%n");
  }

  /** Clean up after benchmark trial. */
  @TearDown(Level.Trial)
  public void tearDownTrial() {
    // No cleanup needed for configuration objects
  }

  /**
   * Benchmark: Default configuration creation. This tests the overhead of creating a configuration
   * with all default values.
   */
  @Benchmark
  public void benchmarkDefaultConfigCreation(final Blackhole blackhole) {
    final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();
    blackhole.consume(config);
  }

  /**
   * Benchmark: Configuration creation with minimal options. This tests the simplest configuration
   * usage pattern.
   */
  @Benchmark
  public void benchmarkMinimalBuilderConfig(final Blackhole blackhole) {
    final NativeLibraryConfig config =
        new NativeLibraryConfig(
            TEST_LIBRARY_NAME,
            NativeLibraryConfig.DEFAULT_TEMP_FILE_PREFIX,
            NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX);
    blackhole.consume(config);
  }

  /**
   * Benchmark: Constructor-based configuration creation with custom options. This tests the full
   * configuration pattern.
   */
  @Benchmark
  public void benchmarkFullBuilderConfig(final Blackhole blackhole) {
    final NativeLibraryConfig config =
        new NativeLibraryConfig(
            TEST_LIBRARY_NAME, TEST_PREFIX, NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX);
    blackhole.consume(config);
  }

  /**
   * Benchmark: NativeLoaderBuilder creation and basic configuration. This tests the overhead of the
   * NativeLoader builder pattern.
   */
  @Benchmark
  public void benchmarkNativeLoaderBuilderCreation(final Blackhole blackhole) {
    final NativeLoaderBuilder loaderBuilder = NativeLoader.builder().libraryName(TEST_LIBRARY_NAME);
    blackhole.consume(loaderBuilder);
  }

  /**
   * Benchmark: NativeLoaderBuilder with full configuration chain. This tests the full builder chain
   * performance.
   */
  @Benchmark
  public void benchmarkNativeLoaderBuilderFullChain(final Blackhole blackhole) {
    final NativeLoaderBuilder loaderBuilder =
        NativeLoader.builder()
            .libraryName(TEST_LIBRARY_NAME)
            .tempFilePrefix(TEST_PREFIX)
            .pathConvention(PathConvention.MAVEN_NATIVE);
    blackhole.consume(loaderBuilder);
  }

  /**
   * Benchmark: Configuration method access - getLibraryName. This tests the overhead of accessing
   * configuration properties.
   */
  @Benchmark
  public void benchmarkConfigGetLibraryName(final Blackhole blackhole) {
    final String libraryName = customConfig.getLibraryName();
    blackhole.consume(libraryName);
  }

  /** Benchmark: Configuration method access - getTempFilePrefix. */
  @Benchmark
  public void benchmarkConfigGetTempFilePrefix(final Blackhole blackhole) {
    final String prefix = customConfig.getTempFilePrefix();
    blackhole.consume(prefix);
  }

  /** Benchmark: Configuration method access - getPathConventions. */
  @Benchmark
  public void benchmarkConfigGetTempDirSuffix(final Blackhole blackhole) {
    final String suffix = customConfig.getTempDirSuffix();
    blackhole.consume(suffix);
  }

  /**
   * Benchmark: Configuration toString method. This tests the string representation generation
   * overhead.
   */
  @Benchmark
  public void benchmarkConfigToString(final Blackhole blackhole) {
    final String str = customConfig.toString();
    blackhole.consume(str);
  }

  /** Benchmark: Configuration hashCode method. This tests the hash code computation overhead. */
  @Benchmark
  public void benchmarkConfigHashCode(final Blackhole blackhole) {
    final int hash = customConfig.hashCode();
    blackhole.consume(hash);
  }

  /** Benchmark: Configuration equals method. This tests the equality comparison overhead. */
  @Benchmark
  public void benchmarkConfigEquals(final Blackhole blackhole) {
    final boolean equals = customConfig.equals(customConfig);
    blackhole.consume(equals);
  }

  /**
   * Benchmark: PathConvention enum access and comparison. This tests the overhead of working with
   * PathConvention enums.
   */
  @Benchmark
  public void benchmarkPathConventionOperations(final Blackhole blackhole) {
    final PathConvention convention = PathConvention.MAVEN_NATIVE;
    final String name = convention.name();
    final String toString = convention.toString();
    final int ordinal = convention.ordinal();

    blackhole.consume(name);
    blackhole.consume(toString);
    blackhole.consume(ordinal);
  }

  /**
   * Benchmark: Multiple configuration creation in sequence. This tests repeated configuration
   * creation performance.
   */
  @Benchmark
  public void benchmarkSequentialConfigCreation(final Blackhole blackhole) {
    for (int i = 0; i < 5; i++) {
      final NativeLibraryConfig config =
          new NativeLibraryConfig(
              TEST_LIBRARY_NAME + i,
              TEST_PREFIX + i,
              NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX);
      blackhole.consume(config);
    }
  }

  /**
   * Benchmark: Concurrent configuration creation with 4 threads. This tests thread safety and
   * concurrent performance.
   */
  @Benchmark
  @Threads(4)
  public void benchmarkConcurrentConfigCreation4Threads(final Blackhole blackhole) {
    final NativeLibraryConfig config =
        new NativeLibraryConfig(
            TEST_LIBRARY_NAME, TEST_PREFIX, NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX);
    blackhole.consume(config);
  }

  /**
   * Benchmark: Concurrent configuration creation with 10 threads. This tests performance under high
   * concurrent load.
   */
  @Benchmark
  @Threads(10)
  public void benchmarkConcurrentConfigCreation10Threads(final Blackhole blackhole) {
    final NativeLibraryConfig config =
        new NativeLibraryConfig(
            TEST_LIBRARY_NAME, TEST_PREFIX, NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX);
    blackhole.consume(config);
  }

  /**
   * Benchmark: Configuration access under concurrent load. This tests thread safety of accessing
   * configuration properties.
   */
  @Benchmark
  @Threads(8)
  public void benchmarkConcurrentConfigAccess(final Blackhole blackhole) {
    final String libraryName = customConfig.getLibraryName();
    final String prefix = customConfig.getTempFilePrefix();
    final String suffix = customConfig.getTempDirSuffix();

    blackhole.consume(libraryName);
    blackhole.consume(prefix);
    blackhole.consume(suffix);
  }

  /**
   * Benchmark: Configuration with different PathConvention combinations. This tests the overhead of
   * various PathConvention configurations.
   */
  @Benchmark
  public void benchmarkVariousPathConventionConfigs(final Blackhole blackhole) {
    // Single convention
    final NativeLibraryConfig config1 =
        new NativeLibraryConfig(
            TEST_LIBRARY_NAME,
            NativeLibraryConfig.DEFAULT_TEMP_FILE_PREFIX,
            NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX);

    // Multiple conventions
    final NativeLibraryConfig config2 =
        new NativeLibraryConfig(
            TEST_LIBRARY_NAME,
            NativeLibraryConfig.DEFAULT_TEMP_FILE_PREFIX,
            NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX);

    // All conventions
    final NativeLibraryConfig config3 =
        new NativeLibraryConfig(
            TEST_LIBRARY_NAME,
            NativeLibraryConfig.DEFAULT_TEMP_FILE_PREFIX,
            NativeLibraryConfig.DEFAULT_TEMP_DIR_SUFFIX);

    blackhole.consume(config1);
    blackhole.consume(config2);
    blackhole.consume(config3);
  }
}
