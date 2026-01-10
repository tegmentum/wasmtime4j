package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** Specialized benchmark for memory operation patterns. */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 1,
    jvmArgs = {"--enable-native-access=ALL-UNNAMED"})
public class BulkMemoryPatternBenchmark {

  @Param({"JNI", "PANAMA"})
  private String runtimeType;

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private WasmMemory memory;

  @Setup(Level.Trial)
  public void setupTrial() {
    try {
      RuntimeType type = RuntimeType.valueOf(runtimeType);
      if (!WasmRuntimeFactory.isRuntimeAvailable(type)) {
        throw new RuntimeException("Runtime not available: " + type);
      }
      runtime = WasmRuntimeFactory.create(type);
      EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.BULK_MEMORY);
      engine = runtime.createEngine(config);
      store = engine.createStore();

      // Create memory with sufficient size (4 pages = 256KB, max 8 pages)
      memory = store.createMemory(4, 8);

    } catch (Exception e) {
      throw new RuntimeException("Pattern benchmark setup failed", e);
    }
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() {
    try {
      if (store != null) store.close();
      if (engine != null) engine.close();
      if (runtime != null) runtime.close();
    } catch (Exception e) {
      // Log but don't fail
    }
  }

  /** Benchmark memory initialization pattern (common in WASM modules). */
  @Benchmark
  public void memoryInitializationPattern() {
    // Clear memory
    memory.fill(0, (byte) 0, 4096);

    // Initialize various sections with different patterns
    memory.fill(0, (byte) 0xFF, 256); // Header section
    memory.fill(256, (byte) 0x00, 768); // Zero section
    memory.fill(1024, (byte) 0xAA, 1024); // Data section
    memory.fill(2048, (byte) 0x55, 2048); // Buffer section
  }

  /** Benchmark data shuffling pattern. */
  @Benchmark
  public void dataShufflingPattern() {
    // Initialize source data
    memory.fill(0, (byte) 0x42, 1024);

    // Shuffle data around (simulates array operations)
    memory.copy(2048, 0, 512); // Copy first half
    memory.copy(2560, 512, 512); // Copy second half
    memory.copy(0, 2048, 1024); // Copy back
  }

  /** Benchmark memory compaction pattern. */
  @Benchmark
  public void memoryCompactionPattern() {
    // Initialize fragmented data
    for (int i = 0; i < 16; i++) {
      memory.fill(i * 128, (byte) (i % 256), 64); // 64 bytes data
      memory.fill(i * 128 + 64, (byte) 0, 64); // 64 bytes gap
    }

    // Compact by removing gaps
    for (int i = 0; i < 16; i++) {
      memory.copy(i * 64, i * 128, 64);
    }

    // Clear remaining space
    memory.fill(16 * 64, (byte) 0, 2048 - 16 * 64);
  }
}
