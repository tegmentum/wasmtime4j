package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Memory64Config;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Performance benchmark comparing 32-bit vs 64-bit WebAssembly memory operations using real
 * Wasmtime memory instances.
 *
 * <p>This benchmark measures the performance overhead of 64-bit addressing compared to traditional
 * 32-bit addressing for various memory operations against actual Wasmtime linear memory. Both
 * memory instances use 16 initial pages (1MB) with a maximum of 256 pages (16MB).
 *
 * <p>Benchmark scenarios:
 *
 * <ul>
 *   <li>Single byte read/write operations (32-bit vs 64-bit API)
 *   <li>Bulk memory operations (readBytes/writeBytes vs readBytes64/writeBytes64)
 *   <li>Memory copy and fill operations (copy/fill vs copy64/fill64)
 *   <li>Memory growth operations (grow vs grow64)
 *   <li>Size query operations (getSize vs getSize64)
 *   <li>Configuration creation overhead (Memory64Config)
 *   <li>Cross-runtime comparisons (JNI vs Panama)
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms2g", "-Xmx2g"})
public class Memory64PerformanceBenchmark extends BenchmarkBase {

  /** Logger for this benchmark class. */
  private static final Logger LOGGER =
      Logger.getLogger(Memory64PerformanceBenchmark.class.getName());

  /** WAT module exporting a 32-bit memory with 16 initial pages, max 256 pages (16MB). */
  private static final String MEMORY32_WAT =
      "(module\n" + "  (memory (export \"memory\") 16 256))\n";

  /** WAT module exporting a 64-bit memory with 16 initial pages, max 256 pages (16MB). */
  private static final String MEMORY64_WAT =
      "(module\n" + "  (memory (export \"memory\") i64 16 256))\n";

  /** Number of pre-generated test offsets for consistent benchmarking. */
  private static final int OFFSET_COUNT = 1000;

  /** Size of test data and buffer in bytes (4KB). */
  private static final int DATA_SIZE = 4096;

  /**
   * Maximum safe offset within a 16-page (1MB) memory, leaving room for a 4KB buffer at the end.
   */
  private static final int MAX_SAFE_OFFSET = 16 * 65536 - DATA_SIZE;

  /** Fixed random seed for reproducible benchmark results. */
  private static final long RANDOM_SEED = 42L;

  /** Runtime implementation to benchmark. */
  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  // 32-bit memory resources
  private WasmRuntime runtime;
  private Engine engine32;
  private Store store32;
  private Module module32;
  private Instance instance32;
  private WasmMemory memory32;

  // 64-bit memory resources (null if memory64 is not supported)
  private Engine engine64;
  private Store store64;
  private Module module64;
  private Instance instance64;
  private WasmMemory memory64;

  // Test data
  private byte[] testData;
  private byte[] buffer;

  // Pre-generated offsets for deterministic benchmarks
  private int[] offsets32;
  private long[] offsets64;

  /**
   * Sets up all Wasmtime resources and pre-generates test data.
   *
   * <p>Creates two separate engine/store/instance/memory chains: one for 32-bit memory using
   * default engine config, and one for 64-bit memory using an engine config with
   * WasmFeature.MEMORY64 enabled. If the memory64 feature is not supported by the runtime, the
   * 64-bit resources are left null and 64-bit benchmarks will be skipped.
   *
   * @throws Exception if 32-bit resource creation fails
   */
  @Setup(Level.Trial)
  public void setupTrial() throws Exception {
    final RuntimeType runtimeType = RuntimeType.valueOf(runtimeTypeName);
    final Random random = new Random(RANDOM_SEED);

    // Create test data with deterministic content
    testData = new byte[DATA_SIZE];
    buffer = new byte[DATA_SIZE];
    random.nextBytes(testData);

    // Pre-generate offsets within the safe range for a 16-page memory
    offsets32 = new int[OFFSET_COUNT];
    offsets64 = new long[OFFSET_COUNT];
    for (int i = 0; i < OFFSET_COUNT; i++) {
      final int offset = random.nextInt(MAX_SAFE_OFFSET);
      offsets32[i] = offset;
      offsets64[i] = (long) offset;
    }

    // Setup 32-bit memory chain
    runtime = WasmRuntimeFactory.create(runtimeType);
    engine32 = runtime.createEngine();
    store32 = engine32.createStore();
    module32 = engine32.compileWat(MEMORY32_WAT);
    instance32 = module32.instantiate(store32);
    final Optional<WasmMemory> mem32Opt = instance32.getMemory("memory");
    if (!mem32Opt.isPresent()) {
      throw new IllegalStateException("32-bit memory export not found in compiled module");
    }
    memory32 = mem32Opt.get();

    // Setup 64-bit memory chain (may fail if memory64 is not supported)
    try {
      final EngineConfig config64 = new EngineConfig().addWasmFeature(WasmFeature.MEMORY64);
      engine64 = runtime.createEngine(config64);
      store64 = engine64.createStore();
      module64 = engine64.compileWat(MEMORY64_WAT);
      instance64 = module64.instantiate(store64);
      final Optional<WasmMemory> mem64Opt = instance64.getMemory("memory");
      if (!mem64Opt.isPresent()) {
        throw new IllegalStateException("64-bit memory export not found in compiled module");
      }
      memory64 = mem64Opt.get();
    } catch (final Exception e) {
      LOGGER.warning(
          "Memory64 feature not supported by runtime, 64-bit benchmarks will be skipped: "
              + e.getMessage());
      closeQuietly(instance64);
      closeQuietly(module64);
      closeQuietly(store64);
      closeQuietly(engine64);
      instance64 = null;
      module64 = null;
      store64 = null;
      engine64 = null;
      memory64 = null;
    }
  }

  /** Clears the read buffer between invocations for consistent benchmark results. */
  @Setup(Level.Invocation)
  public void setupInvocation() {
    Arrays.fill(buffer, (byte) 0);
  }

  /**
   * Releases all Wasmtime resources after the benchmark trial completes.
   *
   * @throws Exception if cleanup fails
   */
  @TearDown(Level.Trial)
  public void teardownTrial() throws Exception {
    // 64-bit resources
    closeQuietly(instance64);
    closeQuietly(module64);
    closeQuietly(store64);
    closeQuietly(engine64);

    // 32-bit resources
    closeQuietly(instance32);
    closeQuietly(module32);
    closeQuietly(store32);
    closeQuietly(engine32);
    closeQuietly(runtime);

    memory64 = null;
    memory32 = null;
  }

  // ---------------------------------------------------------------------------
  // Single Byte Operations
  // ---------------------------------------------------------------------------

  /**
   * Benchmarks single byte reads from 32-bit memory at pre-generated random offsets.
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void singleByteRead32(final Blackhole bh) {
    for (final int offset : offsets32) {
      bh.consume(memory32.readByte(offset));
    }
  }

  /**
   * Benchmarks single byte reads from 64-bit memory using the readByte64 API.
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void singleByteRead64(final Blackhole bh) {
    if (memory64 == null) {
      bh.consume(0);
      return;
    }
    for (final long offset : offsets64) {
      bh.consume(memory64.readByte64(offset));
    }
  }

  /**
   * Benchmarks single byte writes to 32-bit memory at pre-generated random offsets.
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void singleByteWrite32(final Blackhole bh) {
    final byte value = (byte) 42;
    for (final int offset : offsets32) {
      memory32.writeByte(offset, value);
    }
    bh.consume(value);
  }

  /**
   * Benchmarks single byte writes to 64-bit memory using the writeByte64 API.
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void singleByteWrite64(final Blackhole bh) {
    if (memory64 == null) {
      bh.consume(0);
      return;
    }
    final byte value = (byte) 42;
    for (final long offset : offsets64) {
      memory64.writeByte64(offset, value);
    }
    bh.consume(value);
  }

  // ---------------------------------------------------------------------------
  // Bulk Read/Write Operations
  // ---------------------------------------------------------------------------

  /**
   * Benchmarks bulk reads from 32-bit memory using readBytes.
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void bulkRead32(final Blackhole bh) {
    for (final int offset : offsets32) {
      memory32.readBytes(offset, buffer, 0, DATA_SIZE);
    }
    bh.consume(buffer);
  }

  /**
   * Benchmarks bulk reads from 64-bit memory using readBytes64.
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void bulkRead64(final Blackhole bh) {
    if (memory64 == null) {
      bh.consume(0);
      return;
    }
    for (final long offset : offsets64) {
      memory64.readBytes64(offset, buffer, 0, DATA_SIZE);
    }
    bh.consume(buffer);
  }

  /**
   * Benchmarks bulk writes to 32-bit memory using writeBytes.
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void bulkWrite32(final Blackhole bh) {
    for (final int offset : offsets32) {
      memory32.writeBytes(offset, testData, 0, DATA_SIZE);
    }
    bh.consume(testData);
  }

  /**
   * Benchmarks bulk writes to 64-bit memory using writeBytes64.
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void bulkWrite64(final Blackhole bh) {
    if (memory64 == null) {
      bh.consume(0);
      return;
    }
    for (final long offset : offsets64) {
      memory64.writeBytes64(offset, testData, 0, DATA_SIZE);
    }
    bh.consume(testData);
  }

  // ---------------------------------------------------------------------------
  // Memory Copy Operations
  // ---------------------------------------------------------------------------
  // Memory Growth Operations
  // ---------------------------------------------------------------------------

  /**
   * Benchmarks growing 32-bit memory by one page.
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void memoryGrowth32(final Blackhole bh) {
    bh.consume(memory32.grow(1));
  }

  /**
   * Benchmarks growing 64-bit memory by one page using grow64.
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void memoryGrowth64(final Blackhole bh) {
    if (memory64 == null) {
      bh.consume(0L);
      return;
    }
    bh.consume(memory64.grow64(1L));
  }

  // ---------------------------------------------------------------------------
  // Size Query Operations
  // ---------------------------------------------------------------------------

  /**
   * Benchmarks querying the size of 32-bit memory via getSize().
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void sizeQuery32(final Blackhole bh) {
    bh.consume(memory32.getSize());
  }

  /**
   * Benchmarks querying the size of 64-bit memory via getSize64().
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void sizeQuery64(final Blackhole bh) {
    if (memory64 == null) {
      bh.consume(0L);
      return;
    }
    bh.consume(memory64.getSize64());
  }

  // ---------------------------------------------------------------------------
  // Configuration Creation Operations
  // ---------------------------------------------------------------------------

  /**
   * Benchmarks creating a default 32-bit Memory64Config and querying its properties.
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void configurationCreation32(final Blackhole bh) {
    final Memory64Config config = Memory64Config.createDefault32Bit(100);
    bh.consume(config.getAddressingMode());
    bh.consume(config.getMinimumPages());
    bh.consume(config.getMaximumPages().orElse(-1L));
  }

  /**
   * Benchmarks creating a default 64-bit Memory64Config and querying its properties.
   *
   * @param bh JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void configurationCreation64(final Blackhole bh) {
    final Memory64Config config = Memory64Config.createDefault64Bit(100);
    bh.consume(config.getAddressingMode());
    bh.consume(config.getMinimumPages());
    bh.consume(config.getMaximumPages().orElse(-1L));
  }

  /**
   * Runs the benchmark from the command line.
   *
   * @param args command line arguments (unused)
   * @throws RunnerException if the benchmark runner fails
   */
  public static void main(final String[] args) throws RunnerException {
    final Options opt =
        new OptionsBuilder().include(Memory64PerformanceBenchmark.class.getSimpleName()).build();

    new Runner(opt).run();
  }
}
