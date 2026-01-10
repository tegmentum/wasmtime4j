package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Comprehensive benchmark to validate module caching performance claims.
 *
 * <p>This benchmark validates the claimed >50% compilation time reduction from module caching by
 * comparing compilation performance with and without caching across various scenarios and module
 * types.
 *
 * <p>Test scenarios include:
 *
 * <ul>
 *   <li>First-time compilation vs cached compilation performance
 *   <li>Different module sizes and complexity levels
 *   <li>Concurrent access to cached modules
 *   <li>Cache warming and persistence testing
 *   <li>Real-world compilation patterns and cache hit rates
 * </ul>
 *
 * <p>Performance targets based on specification requirements:
 *
 * <ul>
 *   <li>Cached compilation should be >50% faster than fresh compilation
 *   <li>Cache warming should provide immediate compilation benefits
 *   <li>Multi-threaded access should maintain cache efficiency
 *   <li>Large module caching should provide proportionally greater benefits
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@State(Scope.Benchmark)
public class ModuleCachingPerformanceBenchmark extends BenchmarkBase {

  /** Simple WebAssembly module for basic caching tests. */
  private static final byte[] TEST_WASM_SIMPLE =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // WASM magic
        0x01,
        0x00,
        0x00,
        0x00, // Version
        0x01,
        0x04,
        0x01,
        0x60,
        0x00,
        0x00, // Type section: () -> ()
        0x03,
        0x02,
        0x01,
        0x00, // Function section: function 0 has type 0
        0x0a,
        0x04,
        0x01,
        0x02,
        0x00,
        0x0b // Code section: function 0 is empty
      };

  /** Complex WebAssembly module for advanced caching tests. */
  private static final byte[] TEST_WASM_COMPLEX = generateComplexWasm();

  /** Large WebAssembly module for large file caching tests. */
  private static final byte[] TEST_WASM_LARGE = generateLargeWasm();

  @Param({"true", "false"})
  private boolean enableCaching;

  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  @Param({"SIMPLE", "COMPLEX", "LARGE"})
  private String moduleType;

  private Engine engineWithCache;
  private Engine engineWithoutCache;
  private byte[] currentTestWasm;

  @Setup(Level.Trial)
  public void setupTrial() throws WasmException {
    super.setupRuntime(runtimeTypeName);

    // Create engines with different caching strategies
    engineWithCache =
        Engine.builder()
            .moduleCaching(true)
            .cacheDirectory("wasmtime4j_benchmark_cache")
            .maxCacheSize(500L * 1024 * 1024) // 500MB
            .maxCacheEntries(1000)
            .enableCacheCompression(true)
            .build();

    engineWithoutCache = Engine.builder().moduleCaching(false).build();

    // Select test module based on parameter
    switch (moduleType) {
      case "SIMPLE":
        currentTestWasm = TEST_WASM_SIMPLE;
        break;
      case "COMPLEX":
        currentTestWasm = TEST_WASM_COMPLEX;
        break;
      case "LARGE":
        currentTestWasm = TEST_WASM_LARGE;
        break;
      default:
        throw new IllegalArgumentException("Unknown module type: " + moduleType);
    }

    logInfo(
        "Benchmark setup completed for runtime: "
            + runtimeTypeName
            + ", caching: "
            + enableCaching
            + ", module: "
            + moduleType);
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() {
    closeQuietly(engineWithoutCache);
    closeQuietly(engineWithCache);

    super.tearDownRuntime();
    logInfo("Benchmark teardown completed for runtime: " + runtimeTypeName);
  }

  /**
   * Benchmarks basic module compilation performance. This is the primary test for validating the
   * >50% improvement claim.
   */
  @Benchmark
  public Module benchmarkBasicModuleCompilation() throws WasmException {
    final Engine engine = enableCaching ? engineWithCache : engineWithoutCache;
    return Module.fromBinary(engine, currentTestWasm);
  }

  /**
   * Benchmarks repeated compilation of the same module. Tests caching effectiveness for identical
   * modules.
   */
  @Benchmark
  public Module benchmarkRepeatedCompilation() throws WasmException {
    final Engine engine = enableCaching ? engineWithCache : engineWithoutCache;

    // Compile the same module multiple times to test cache hits
    final Module firstCompilation = Module.fromBinary(engine, currentTestWasm);
    closeQuietly(firstCompilation);

    return Module.fromBinary(engine, currentTestWasm);
  }

  /**
   * Benchmarks compilation of slightly different modules. Tests cache behavior with module
   * variations.
   */
  @Benchmark
  public Module benchmarkVariantModuleCompilation() throws WasmException {
    final Engine engine = enableCaching ? engineWithCache : engineWithoutCache;

    // Create a slight variation of the module for cache miss testing
    final byte[] variantWasm = createModuleVariant(currentTestWasm);
    return Module.fromBinary(engine, variantWasm);
  }

  /**
   * Benchmarks concurrent module compilation. Tests caching efficiency under multi-threaded load.
   */
  @Benchmark
  @Threads(4)
  public Module benchmarkConcurrentCompilation() throws WasmException {
    final Engine engine = enableCaching ? engineWithCache : engineWithoutCache;
    return Module.fromBinary(engine, currentTestWasm);
  }

  /**
   * Benchmarks high-concurrency compilation stress test. Maximum stress test for module cache
   * performance.
   */
  @Benchmark
  @Threads(16)
  public Module benchmarkHighConcurrencyCompilation() throws WasmException {
    final Engine engine = enableCaching ? engineWithCache : engineWithoutCache;
    return Module.fromBinary(engine, currentTestWasm);
  }

  /**
   * Benchmarks cache warming effectiveness. Tests pre-compilation and cache population strategies.
   */
  @Benchmark
  public Module benchmarkCacheWarmingEffectiveness() throws WasmException {
    if (!enableCaching) {
      return Module.fromBinary(engineWithoutCache, currentTestWasm);
    }

    // Pre-warm cache with compilation
    final Module preWarmModule = Module.fromBinary(engineWithCache, currentTestWasm);
    closeQuietly(preWarmModule);

    // This compilation should benefit from warm cache
    return Module.fromBinary(engineWithCache, currentTestWasm);
  }

  /**
   * Benchmarks batch compilation performance. Tests cache efficiency with multiple different
   * modules.
   */
  @Benchmark
  public void benchmarkBatchCompilation() throws WasmException {
    final Engine engine = enableCaching ? engineWithCache : engineWithoutCache;

    final byte[][] modules = {
      currentTestWasm,
      createModuleVariant(currentTestWasm),
      createModuleVariant(createModuleVariant(currentTestWasm))
    };

    final Module[] compiledModules = new Module[modules.length];
    try {
      for (int i = 0; i < modules.length; i++) {
        compiledModules[i] = Module.fromBinary(engine, modules[i]);
      }
    } finally {
      for (final Module module : compiledModules) {
        closeQuietly(module);
      }
    }
  }

  /**
   * Benchmarks cache persistence across engine instances. Tests persistent cache behavior and
   * cross-session benefits.
   */
  @Benchmark
  public Module benchmarkCrossPersistenceCompilation() throws WasmException {
    if (!enableCaching) {
      return Module.fromBinary(engineWithoutCache, currentTestWasm);
    }

    // Create new engine instance that should benefit from persistent cache
    final Engine newEngine =
        Engine.builder()
            .moduleCaching(true)
            .cacheDirectory("wasmtime4j_benchmark_cache") // Same directory
            .build();

    try {
      return Module.fromBinary(newEngine, currentTestWasm);
    } finally {
      closeQuietly(newEngine);
    }
  }

  /**
   * Benchmarks compilation with mixed cache states. Simulates realistic usage patterns with cache
   * hits and misses.
   */
  @Benchmark
  public void benchmarkMixedCacheStateCompilation() throws WasmException {
    final Engine engine = enableCaching ? engineWithCache : engineWithoutCache;

    // Mix of cached and non-cached compilations
    final Module[] modules = new Module[4];
    try {
      // First compilation (cache miss)
      modules[0] = Module.fromBinary(engine, currentTestWasm);

      // Second compilation of same module (cache hit)
      modules[1] = Module.fromBinary(engine, currentTestWasm);

      // Different module (cache miss)
      modules[2] = Module.fromBinary(engine, createModuleVariant(currentTestWasm));

      // Repeat different module (cache hit)
      modules[3] = Module.fromBinary(engine, createModuleVariant(currentTestWasm));

    } finally {
      for (final Module module : modules) {
        closeQuietly(module);
      }
    }
  }

  /**
   * Benchmarks cache efficiency under memory pressure. Tests cache behavior when approaching size
   * limits.
   */
  @Benchmark
  public Module benchmarkMemoryPressureCompilation() throws WasmException {
    if (!enableCaching) {
      return Module.fromBinary(engineWithoutCache, currentTestWasm);
    }

    // Create engine with smaller cache to trigger eviction
    final Engine smallCacheEngine =
        Engine.builder()
            .moduleCaching(true)
            .maxCacheSize(1024 * 1024L) // 1MB only
            .maxCacheEntries(5)
            .build();

    try {
      // Fill cache with multiple modules
      for (int i = 0; i < 10; i++) {
        final byte[] variant = createIndexedModuleVariant(currentTestWasm, i);
        final Module module = Module.fromBinary(smallCacheEngine, variant);
        closeQuietly(module);
      }

      // This compilation should test cache eviction behavior
      return Module.fromBinary(smallCacheEngine, currentTestWasm);
    } finally {
      closeQuietly(smallCacheEngine);
    }
  }

  /**
   * Benchmarks long-running compilation patterns. Tests cache effectiveness over extended periods.
   */
  @Benchmark
  public void benchmarkLongRunningCompilation() throws WasmException {
    final Engine engine = enableCaching ? engineWithCache : engineWithoutCache;

    // Simulate long-running application with periodic recompilations
    for (int batch = 0; batch < 3; batch++) {
      final Module[] modules = new Module[5];
      try {
        for (int i = 0; i < modules.length; i++) {
          // Mix of same and different modules to test cache behavior
          final byte[] wasm = (i % 2 == 0) ? currentTestWasm : createModuleVariant(currentTestWasm);
          modules[i] = Module.fromBinary(engine, wasm);
        }

        // Simulate some work
        Thread.yield();

      } finally {
        for (final Module module : modules) {
          closeQuietly(module);
        }
      }
    }
  }

  private static byte[] generateComplexWasm() {
    // Generate a more complex WebAssembly module with functions, imports, and exports
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6d, // WASM magic
      0x01,
      0x00,
      0x00,
      0x00, // Version
      0x01,
      0x08,
      0x02, // Type section
      0x60,
      0x01,
      0x7f,
      0x01,
      0x7f, // Type 0: (i32) -> i32
      0x60,
      0x00,
      0x00, // Type 1: () -> ()
      0x03,
      0x03,
      0x02,
      0x00,
      0x01, // Function section: 2 functions
      0x0a,
      0x0c,
      0x02, // Code section: 2 function bodies
      0x07,
      0x00,
      0x20,
      0x00,
      0x41,
      0x01,
      0x6a,
      0x0b, // Function 0: local.get 0, i32.const 1, i32.add
      0x02,
      0x00,
      0x0b // Function 1: empty
    };
  }

  private static byte[] generateLargeWasm() {
    // Generate a larger WebAssembly module for testing cache performance with bigger files
    final byte[] base = TEST_WASM_SIMPLE;
    final byte[] large = new byte[base.length + 1000];
    System.arraycopy(base, 0, large, 0, base.length);

    // Fill remaining with padding data (simulating larger module)
    for (int i = base.length; i < large.length; i++) {
      large[i] = (byte) (i % 256);
    }

    return large;
  }

  private byte[] createModuleVariant(final byte[] originalWasm) {
    final byte[] variant = originalWasm.clone();
    // Create a simple variation by modifying a non-critical byte
    if (variant.length > 10) {
      variant[variant.length - 1] = (byte) ((variant[variant.length - 1] + 1) % 256);
    }
    return variant;
  }

  private byte[] createIndexedModuleVariant(final byte[] originalWasm, final int index) {
    final byte[] variant = originalWasm.clone();
    // Create indexed variations for cache testing
    if (variant.length > 10) {
      variant[variant.length - 1] = (byte) (index % 256);
    }
    return variant;
  }

  private void closeQuietly(final AutoCloseable resource) {
    if (resource != null) {
      try {
        resource.close();
      } catch (final Exception e) {
        logWarn("Error closing resource: " + e.getMessage());
      }
    }
  }
}
