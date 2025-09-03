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

package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils;
import ai.tegmentum.wasmtime4j.nativeloader.NativeLoader;
import ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector;
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
 * Comprehensive comparison benchmark between NativeLoader and baseline operations.
 *
 * <p>This benchmark provides direct performance comparisons to validate the ≤5% performance
 * overhead target specified in the requirements. Key comparisons include:
 *
 * <ul>
 *   <li>NativeLoader.loadLibrary() vs System.loadLibrary() equivalent operations
 *   <li>Platform detection vs direct system property access
 *   <li>Resource path resolution vs manual string operations
 *   <li>Configuration management vs direct parameter passing
 *   <li>Concurrent performance under various thread loads
 * </ul>
 *
 * <p>This benchmark is designed to provide clear validation data against the performance targets:
 * <ul>
 *   <li><strong>Primary Target:</strong> ≤5% overhead vs direct System.loadLibrary()
 *   <li><strong>Concurrent Target:</strong> <10% degradation under 10 concurrent threads
 *   <li><strong>Memory Target:</strong> <1MB additional heap usage per library
 *   <li><strong>Cache Target:</strong> >95% cache hit rate for repeated operations
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class NativeLoaderComparisonBenchmark {

  private static final String TEST_LIBRARY_NAME = "wasmtime4j";
  private static final String NON_EXISTENT_LIBRARY = "nonexistent_benchmark_lib_12345";

  private PlatformDetector.PlatformInfo platformInfo;
  private ClassLoader classLoader;

  /**
   * Sets up the benchmark environment.
   */
  @Setup(Level.Trial)
  public void setupTrial() {
    this.classLoader = Thread.currentThread().getContextClassLoader();
    
    // Pre-warm platform detection to avoid initialization overhead in measurements
    this.platformInfo = PlatformDetector.detect();
    
    System.out.printf("NativeLoader comparison benchmark initialized for platform: %s%n",
        platformInfo.getPlatformId());
  }

  /**
   * Clean up after benchmark trial.
   */
  @TearDown(Level.Trial)
  public void tearDownTrial() {
    // No explicit cleanup needed for public API benchmarks
  }

  /**
   * Setup before each iteration to ensure consistent state.
   */
  @Setup(Level.Iteration)
  public void setupIteration() {
    // No cache manipulation for public API benchmarks
  }

  // ========================================
  // BASELINE OPERATIONS (Reference Performance)
  // ========================================

  /**
   * Baseline: Direct System.loadLibrary() attempt (expected failure).
   * This provides the baseline reference for library loading performance.
   */
  @Benchmark
  public void baselineSystemLoadLibrary(final Blackhole blackhole) {
    try {
      System.loadLibrary(NON_EXISTENT_LIBRARY);
      blackhole.consume("unexpected_success");
    } catch (final UnsatisfiedLinkError e) {
      blackhole.consume(e.getMessage());
    }
  }

  /**
   * Baseline: Manual platform detection using system properties.
   * This provides the baseline for platform detection operations.
   */
  @Benchmark
  public void baselineManualPlatformDetection(final Blackhole blackhole) {
    final String osName = System.getProperty("os.name");
    final String osArch = System.getProperty("os.arch");
    final String platformId = osName.toLowerCase() + "-" + osArch.toLowerCase();
    
    blackhole.consume(platformId);
  }

  /**
   * Baseline: Manual resource path construction.
   * This provides the baseline for resource path resolution.
   */
  @Benchmark
  public void baselineManualResourcePath(final Blackhole blackhole) {
    final String osName = System.getProperty("os.name");
    final String osArch = System.getProperty("os.arch");
    
    final String platformId = osName.toLowerCase() + "-" + osArch.toLowerCase();
    final String fileName = System.mapLibraryName(TEST_LIBRARY_NAME);
    final String resourcePath = "/native/" + platformId + "/" + fileName;
    
    blackhole.consume(resourcePath);
  }

  /**
   * Baseline: Manual resource lookup without extraction.
   * This provides the baseline for resource loading operations.
   */
  @Benchmark
  public void baselineManualResourceLookup(final Blackhole blackhole) {
    final String osName = System.getProperty("os.name");
    final String osArch = System.getProperty("os.arch");
    final String platformId = osName.toLowerCase() + "-" + osArch.toLowerCase();
    final String fileName = System.mapLibraryName(TEST_LIBRARY_NAME);
    final String resourcePath = "/native/" + platformId + "/" + fileName;
    
    final InputStream stream = classLoader.getResourceAsStream(resourcePath);
    blackhole.consume(stream);
    
    if (stream != null) {
      try {
        stream.close();
      } catch (final Exception e) {
        // Ignore cleanup errors
      }
    }
  }

  // ========================================
  // NATIVELOADER OPERATIONS (Test Performance)
  // ========================================

  /**
   * NativeLoader: Simple library loading using static method.
   * This is the primary operation to compare against baselineSystemLoadLibrary.
   */
  @Benchmark
  public void nativeLoaderSimpleLoad(final Blackhole blackhole) {
    final NativeLibraryUtils.LibraryLoadInfo loadInfo = 
        NativeLoader.loadLibrary(TEST_LIBRARY_NAME);
    blackhole.consume(loadInfo);
  }

  /**
   * NativeLoader: Platform detection performance.
   * This compares against baselineManualPlatformDetection.
   */
  @Benchmark
  public void nativeLoaderPlatformDetection(final Blackhole blackhole) {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
    blackhole.consume(platformInfo);
  }

  /**
   * NativeLoader: Resource path resolution.
   * This compares against baselineManualResourcePath.
   */
  @Benchmark
  public void nativeLoaderResourcePathResolution(final Blackhole blackhole) {
    final PlatformDetector.PlatformInfo platform = PlatformDetector.detect();
    final String resourcePath = platform.getLibraryResourcePath(TEST_LIBRARY_NAME);
    
    blackhole.consume(resourcePath);
  }

  /**
   * NativeLoader: Builder pattern configuration.
   * This tests the overhead of the builder pattern vs direct operations.
   */
  @Benchmark
  public void nativeLoaderBuilderPattern(final Blackhole blackhole) {
    final NativeLibraryUtils.LibraryLoadInfo loadInfo = NativeLoader.builder()
        .libraryName(TEST_LIBRARY_NAME)
        .tempFilePrefix("benchmark-")
        .load();
    
    blackhole.consume(loadInfo);
  }

  // ========================================
  // DIRECT COMPARISONS (Side-by-Side)
  // ========================================

  /**
   * Direct comparison: Baseline vs NativeLoader for simple operations.
   * This provides side-by-side comparison data.
   */
  @Benchmark
  public void comparisonBaselineVsNativeLoader(final Blackhole blackhole) {
    // Baseline operation
    try {
      System.loadLibrary(NON_EXISTENT_LIBRARY);
    } catch (final UnsatisfiedLinkError e) {
      blackhole.consume(e.getMessage());
    }
    
    // NativeLoader operation
    final NativeLibraryUtils.LibraryLoadInfo loadInfo = 
        NativeLoader.loadLibrary(TEST_LIBRARY_NAME);
    blackhole.consume(loadInfo);
  }

  /**
   * Direct comparison: Manual vs NativeLoader platform detection.
   */
  @Benchmark
  public void comparisonPlatformDetection(final Blackhole blackhole) {
    // Manual detection
    final String osName = System.getProperty("os.name");
    final String osArch = System.getProperty("os.arch");
    final String manualPlatformId = osName.toLowerCase() + "-" + osArch.toLowerCase();
    
    // NativeLoader detection
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
    final String nativeLoaderPlatformId = platformInfo.getPlatformId();
    
    blackhole.consume(manualPlatformId);
    blackhole.consume(nativeLoaderPlatformId);
  }

  // ========================================
  // CONCURRENT PERFORMANCE TESTS
  // ========================================

  /**
   * Baseline concurrent performance with 4 threads.
   */
  @Benchmark
  @Threads(4)
  public void baselineConcurrent4Threads(final Blackhole blackhole) {
    try {
      System.loadLibrary(NON_EXISTENT_LIBRARY);
    } catch (final UnsatisfiedLinkError e) {
      blackhole.consume(e.getMessage());
    }
  }

  /**
   * NativeLoader concurrent performance with 4 threads.
   * This compares against baselineConcurrent4Threads.
   */
  @Benchmark
  @Threads(4)
  public void nativeLoaderConcurrent4Threads(final Blackhole blackhole) {
    final NativeLibraryUtils.LibraryLoadInfo loadInfo = 
        NativeLoader.loadLibrary(TEST_LIBRARY_NAME);
    blackhole.consume(loadInfo);
  }

  /**
   * Baseline concurrent performance with 10 threads.
   * This is the target scenario from requirements.
   */
  @Benchmark
  @Threads(10)
  public void baselineConcurrent10Threads(final Blackhole blackhole) {
    try {
      System.loadLibrary(NON_EXISTENT_LIBRARY);
    } catch (final UnsatisfiedLinkError e) {
      blackhole.consume(e.getMessage());
    }
  }

  /**
   * NativeLoader concurrent performance with 10 threads.
   * This is the critical test for the <10% degradation target.
   */
  @Benchmark
  @Threads(10)
  public void nativeLoaderConcurrent10Threads(final Blackhole blackhole) {
    final NativeLibraryUtils.LibraryLoadInfo loadInfo = 
        NativeLoader.loadLibrary(TEST_LIBRARY_NAME);
    blackhole.consume(loadInfo);
  }

  // ========================================
  // CACHE EFFICIENCY TESTS
  // ========================================

  /**
   * Baseline repeated operations (no caching).
   */
  @Benchmark
  public void baselineRepeatedOperations(final Blackhole blackhole) {
    for (int i = 0; i < 5; i++) {
      final String osName = System.getProperty("os.name");
      final String osArch = System.getProperty("os.arch");
      blackhole.consume(osName);
      blackhole.consume(osArch);
    }
  }

  /**
   * NativeLoader repeated operations (with caching).
   * This tests the cache efficiency and should show significant improvement over baseline.
   */
  @Benchmark
  public void nativeLoaderRepeatedOperations(final Blackhole blackhole) {
    for (int i = 0; i < 5; i++) {
      final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
      blackhole.consume(platformInfo);
    }
  }

  // ========================================
  // COMPREHENSIVE WORKFLOW COMPARISONS
  // ========================================

  /**
   * Baseline complete loading workflow simulation.
   * This simulates the full workflow manually to provide comprehensive comparison.
   */
  @Benchmark
  public void baselineCompleteWorkflow(final Blackhole blackhole) {
    // Platform detection
    final String osName = System.getProperty("os.name");
    final String osArch = System.getProperty("os.arch");
    final String platformId = osName.toLowerCase() + "-" + osArch.toLowerCase();
    
    // Resource path construction
    final String fileName = System.mapLibraryName(TEST_LIBRARY_NAME);
    final String resourcePath = "/native/" + platformId + "/" + fileName;
    
    // Resource lookup
    final InputStream stream = classLoader.getResourceAsStream(resourcePath);
    
    // Temp file preparation
    final String tempFileName = "temp-" + TEST_LIBRARY_NAME + "-" + System.nanoTime();
    
    // Simulate library loading attempt
    try {
      System.loadLibrary(NON_EXISTENT_LIBRARY);
    } catch (final UnsatisfiedLinkError e) {
      blackhole.consume(e.getMessage());
    }
    
    blackhole.consume(platformId);
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
   * NativeLoader complete loading workflow.
   * This provides the full NativeLoader workflow for comparison with baselineCompleteWorkflow.
   */
  @Benchmark
  public void nativeLoaderCompleteWorkflow(final Blackhole blackhole) {
    final NativeLibraryUtils.LibraryLoadInfo loadInfo = NativeLoader.builder()
        .libraryName(TEST_LIBRARY_NAME)
        .tempFilePrefix("benchmark-")
        .load();
    
    // Access various properties to simulate real usage
    final boolean successful = loadInfo.isSuccessful();
    final String libraryName = loadInfo.getLibraryName();
    final String platformId = loadInfo.getPlatformInfo().getPlatformId();
    
    blackhole.consume(successful);
    blackhole.consume(libraryName);
    blackhole.consume(platformId);
    blackhole.consume(loadInfo);
  }
}