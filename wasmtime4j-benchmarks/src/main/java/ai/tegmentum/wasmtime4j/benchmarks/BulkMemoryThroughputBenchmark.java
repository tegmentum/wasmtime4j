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
import java.util.logging.Logger;
import org.openjdk.jmh.annotations.*;

/** Additional benchmark class for specific bulk memory scenarios. */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 1,
    jvmArgs = {"--enable-native-access=ALL-UNNAMED"})
public class BulkMemoryThroughputBenchmark {

  private static final Logger LOGGER =
      Logger.getLogger(BulkMemoryThroughputBenchmark.class.getName());

  @Param({"JNI", "PANAMA"})
  private String runtimeType;

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private WasmMemory memory;

  // Fixed large size for throughput testing
  private static final int LARGE_SIZE = 65536; // 64KB

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

      // Create larger memory for throughput testing (8 pages = 512KB, max 16 pages)
      memory = store.createMemory(8, 16);

      // Initialize with test data
      byte[] testData = new byte[LARGE_SIZE];
      for (int i = 0; i < LARGE_SIZE; i++) {
        testData[i] = (byte) (i % 256);
      }
      memory.writeBytes(0, testData, 0, LARGE_SIZE);

    } catch (Exception e) {
      throw new RuntimeException("Throughput benchmark setup failed", e);
    }
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() {
    try {
      if (store != null) store.close();
      if (engine != null) engine.close();
      if (runtime != null) runtime.close();
    } catch (Exception e) {
      LOGGER.warning("Error during throughput benchmark cleanup: " + e.getMessage());
    }
  }

  /** Throughput benchmark for large bulk copy operations. */
  @Benchmark
  public void largeBulkCopyThroughput() {
    memory.copy(LARGE_SIZE, 0, LARGE_SIZE);
  }

  /** Throughput benchmark for large bulk fill operations. */
  @Benchmark
  public void largeBulkFillThroughput() {
    memory.fill(LARGE_SIZE * 2, (byte) 0x55, LARGE_SIZE);
  }

  /** Throughput benchmark for streaming copy pattern. */
  @Benchmark
  public void streamingCopyThroughput() {
    int chunkSize = 4096; // 4KB chunks
    int srcBase = 0;
    int destBase = LARGE_SIZE * 3;

    for (int offset = 0; offset < LARGE_SIZE; offset += chunkSize) {
      int size = Math.min(chunkSize, LARGE_SIZE - offset);
      memory.copy(destBase + offset, srcBase + offset, size);
    }
  }

  /** Throughput benchmark for parallel operation simulation. */
  @Benchmark
  public void parallelOperationsThroughput() {
    // Simulate multiple independent operations
    int numOperations = 16;
    int operationSize = LARGE_SIZE / numOperations;

    for (int i = 0; i < numOperations; i++) {
      int offset = i * operationSize;
      byte fillValue = (byte) (i % 256);

      memory.fill(LARGE_SIZE * 2 + offset, fillValue, operationSize);
    }
  }
}
