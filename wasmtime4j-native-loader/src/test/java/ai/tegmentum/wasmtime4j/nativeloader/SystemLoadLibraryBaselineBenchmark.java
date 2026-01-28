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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * JMH benchmarks for baseline comparisons with System.loadLibrary() and related operations.
 *
 * <p>This benchmark establishes performance baselines for comparison with the NativeLoader
 * implementation. It measures:
 *
 * <ul>
 *   <li>Direct System.loadLibrary() performance (baseline reference)
 *   <li>Manual library extraction from JAR resources
 *   <li>File I/O operations used in library loading
 *   <li>ClassLoader resource access operations
 *   <li>No-op operations for timing calibration
 * </ul>
 *
 * <p>These baselines are critical for validating the ≤5% performance overhead target specified in
 * the requirements.
 *
 * <p><strong>Important:</strong> Some benchmarks may fail if test libraries are not available, but
 * they provide valuable baseline data for comparison when libraries are present.
 */
@SuppressWarnings({"exports", "module"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class SystemLoadLibraryBaselineBenchmark {

  private static final String SAFE_LIBRARY_NAME = "java";
  private static final String TEST_LIBRARY_NAME = "wasmtime4j";
  private static final String NON_EXISTENT_LIBRARY = "nonexistent_benchmark_lib_12345";

  private PlatformDetector.PlatformInfo platformInfo;
  private ClassLoader classLoader;

  /** Public constructor required for JMH. */
  public SystemLoadLibraryBaselineBenchmark() {
    // JMH will instantiate this class
  }

  /** Sets up the benchmark environment. */
  @Setup(Level.Trial)
  public void setupTrial() {
    this.platformInfo = PlatformDetector.detect();
    this.classLoader = Thread.currentThread().getContextClassLoader();

    System.out.printf(
        "System baseline benchmark initialized for platform: %s%n", platformInfo.getPlatformId());

    // Attempt to identify available system libraries for testing
    System.out.printf("Java library path: %s%n", System.getProperty("java.library.path"));
  }

  /** Clean up after benchmark trial. */
  @TearDown(Level.Trial)
  public void tearDownTrial() {
    // No explicit cleanup needed for system operations
  }

  /**
   * Benchmark: No-op operation for timing calibration. This provides the absolute baseline for JMH
   * measurement overhead.
   */
  @Benchmark
  public void benchmarkNoOp(final Blackhole blackhole) {
    blackhole.consume(System.nanoTime());
  }

  /**
   * Benchmark: System property access (used in library loading). This measures the overhead of
   * accessing system properties like java.library.path.
   */
  @Benchmark
  public void benchmarkSystemPropertyAccess(final Blackhole blackhole) {
    final String javaLibraryPath = System.getProperty("java.library.path");
    final String osName = System.getProperty("os.name");
    final String osArch = System.getProperty("os.arch");

    blackhole.consume(javaLibraryPath);
    blackhole.consume(osName);
    blackhole.consume(osArch);
  }

  /**
   * Benchmark: System.loadLibrary() failure (library not found). This measures the cost of a failed
   * library loading attempt.
   */
  @Benchmark
  public void benchmarkSystemLoadLibraryFailure(final Blackhole blackhole) {
    try {
      System.loadLibrary(NON_EXISTENT_LIBRARY);
      blackhole.consume("unexpected_success");
    } catch (final UnsatisfiedLinkError e) {
      // Expected failure - measure the exception handling overhead
      blackhole.consume(e.getMessage());
    }
  }

  /**
   * Benchmark: ClassLoader resource lookup. This measures the cost of looking up resources in JAR
   * files.
   */
  @Benchmark
  public void benchmarkClassLoaderResourceLookup(final Blackhole blackhole) {
    // Test common resource paths that might exist
    final String[] resourcePaths = {
      "/META-INF/MANIFEST.MF",
      "/natives/" + platformInfo.getPlatformId() + "/" + TEST_LIBRARY_NAME,
      "/lib/" + platformInfo.getLibraryFileName(TEST_LIBRARY_NAME),
      "/nonexistent/path/test.so"
    };

    for (final String resourcePath : resourcePaths) {
      final InputStream stream = classLoader.getResourceAsStream(resourcePath);
      blackhole.consume(stream);

      if (stream != null) {
        try {
          stream.close();
        } catch (final Exception e) {
          // Ignore cleanup errors in benchmark
        }
      }
    }
  }

  /**
   * Benchmark: Temporary file creation and cleanup. This measures the I/O overhead of creating
   * temporary files for library extraction.
   */
  @Benchmark
  public void benchmarkTempFileOperations(final Blackhole blackhole) {
    try {
      // Create temporary file
      final Path tempFile = Files.createTempFile("benchmark", ".tmp");
      blackhole.consume(tempFile);

      // Write some data
      Files.write(tempFile, "benchmark data".getBytes());

      // Read it back
      final byte[] data = Files.readAllBytes(tempFile);
      blackhole.consume(data.length);

      // Clean up
      Files.deleteIfExists(tempFile);

    } catch (final Exception e) {
      blackhole.consume(e.getMessage());
    }
  }

  /**
   * Benchmark: File system operations used in library loading. This measures the overhead of file
   * existence checks and path operations.
   */
  @Benchmark
  public void benchmarkFileSystemOperations(final Blackhole blackhole) {
    try {
      // Check various system paths
      final String tempDir = System.getProperty("java.io.tmpdir");
      final Path tempPath = Path.of(tempDir);

      final boolean exists = Files.exists(tempPath);
      final boolean readable = Files.isReadable(tempPath);
      final boolean writable = Files.isWritable(tempPath);

      blackhole.consume(exists);
      blackhole.consume(readable);
      blackhole.consume(writable);

    } catch (final Exception e) {
      blackhole.consume(e.getMessage());
    }
  }

  /**
   * Benchmark: String manipulation operations used in path resolution. This measures the overhead
   * of string operations common in library loading.
   */
  @Benchmark
  public void benchmarkStringManipulations(final Blackhole blackhole) {
    final String libraryName = TEST_LIBRARY_NAME;
    final String platformId = platformInfo.getPlatformId();

    // Operations similar to those in library loading
    final String fileName = platformInfo.getLibraryFileName(libraryName);
    final String resourcePath = "/natives/" + platformId + "/" + fileName;
    final String normalizedPath = resourcePath.replaceAll("//", "/");
    final String tempFileName = "temp-" + libraryName + "-" + System.nanoTime();

    blackhole.consume(fileName);
    blackhole.consume(resourcePath);
    blackhole.consume(normalizedPath);
    blackhole.consume(tempFileName);
  }

  /**
   * Benchmark: Exception creation and handling. This measures the overhead of exception operations
   * used in error cases.
   */
  @Benchmark
  public void benchmarkExceptionHandling(final Blackhole blackhole) {
    try {
      // Simulate throwing and catching exceptions similar to library loading
      throw new UnsatisfiedLinkError("Benchmark exception for testing");
    } catch (final UnsatisfiedLinkError e) {
      final String message = e.getMessage();
      final StackTraceElement[] stackTrace = e.getStackTrace();

      blackhole.consume(message);
      blackhole.consume(stackTrace.length);
    }
  }

  /**
   * Benchmark: Concurrent system property access. This tests the thread safety and performance of
   * system property access under load.
   */
  @Benchmark
  @Threads(4)
  public void benchmarkConcurrentSystemPropertyAccess4Threads(final Blackhole blackhole) {
    final String osName = System.getProperty("os.name");
    final String osArch = System.getProperty("os.arch");

    blackhole.consume(osName);
    blackhole.consume(osArch);
  }

  /** Benchmark: Concurrent system property access with 10 threads. */
  @Benchmark
  @Threads(10)
  public void benchmarkConcurrentSystemPropertyAccess10Threads(final Blackhole blackhole) {
    final String osName = System.getProperty("os.name");
    final String osArch = System.getProperty("os.arch");

    blackhole.consume(osName);
    blackhole.consume(osArch);
  }

  /**
   * Benchmark: Concurrent ClassLoader resource access. This tests the performance of resource
   * lookup under concurrent load.
   */
  @Benchmark
  @Threads(8)
  public void benchmarkConcurrentResourceAccess(final Blackhole blackhole) {
    final InputStream stream = classLoader.getResourceAsStream("/META-INF/MANIFEST.MF");
    blackhole.consume(stream);

    if (stream != null) {
      try {
        stream.close();
      } catch (final Exception e) {
        // Ignore cleanup errors in benchmark
      }
    }
  }

  /**
   * Benchmark: Simulated library loading workflow (without actual loading). This measures the
   * combined overhead of operations used in library loading.
   */
  @Benchmark
  public void benchmarkSimulatedLoadingWorkflow(final Blackhole blackhole) {
    final String libraryName = TEST_LIBRARY_NAME;

    // Step 1: Platform detection (cached)
    final PlatformDetector.PlatformInfo platform = PlatformDetector.detect();

    // Step 2: Generate file name
    final String fileName = platform.getLibraryFileName(libraryName);

    // Step 3: Generate resource path
    final String resourcePath = "/natives/" + platform.getPlatformId() + "/" + fileName;

    // Step 4: Try to find resource
    final InputStream stream = classLoader.getResourceAsStream(resourcePath);

    // Step 5: Generate temp file name
    final String tempFileName = "temp-" + libraryName + "-" + System.nanoTime();

    blackhole.consume(platform);
    blackhole.consume(fileName);
    blackhole.consume(resourcePath);
    blackhole.consume(stream);
    blackhole.consume(tempFileName);

    if (stream != null) {
      try {
        stream.close();
      } catch (final Exception e) {
        // Ignore cleanup errors
      }
    }
  }

  /**
   * Benchmark: Raw data processing similar to library extraction. This measures the I/O performance
   * baseline for file operations.
   */
  @Benchmark
  public void benchmarkRawDataProcessing(final Blackhole blackhole) {
    try {
      final byte[] testData = "benchmark test data for library loading simulation".getBytes();

      // Create temp file
      final Path tempFile = Files.createTempFile("benchmark", ".tmp");

      // Write data
      Files.write(tempFile, testData);

      // Read data back
      final byte[] readData = Files.readAllBytes(tempFile);

      // Verify data
      final boolean matches = java.util.Arrays.equals(testData, readData);

      blackhole.consume(matches);
      blackhole.consume(readData.length);

      // Cleanup
      Files.deleteIfExists(tempFile);

    } catch (final Exception e) {
      blackhole.consume(e.getMessage());
    }
  }
}
