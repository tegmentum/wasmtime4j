package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmMemory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Performance benchmarks for WebAssembly bulk memory operations.
 *
 * <p>Compares the performance of: - Bulk memory.copy vs individual byte copying - Bulk memory.fill
 * vs individual byte filling - Different data sizes and memory layouts - JNI vs Panama
 * implementation performance
 *
 * <p>Run with: mvn exec:java
 * -Dexec.mainClass="ai.tegmentum.wasmtime4j.benchmarks.BulkMemoryOperationsBenchmark"
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
public class BulkMemoryOperationsBenchmark {

  private static final Logger LOGGER =
      Logger.getLogger(BulkMemoryOperationsBenchmark.class.getName());

  private Engine engine;
  private Store store;
  private WasmMemory memory;

  // Test data sizes
  @Param({"64", "256", "1024", "4096", "16384", "65536"})
  public int dataSize;

  @Setup(Level.Trial)
  public void setupTrial() {
    try {
      LOGGER.info("Setting up benchmark environment");

      // Create engine with bulk memory feature enabled
      EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.BULK_MEMORY);
      engine = Engine.create(config);
      store = engine.createStore();

      // Create memory with sufficient size (4 pages = 256KB, max 8 pages)
      memory = store.createMemory(4, 8);

      LOGGER.info("Benchmark environment setup completed");
    } catch (Exception e) {
      LOGGER.severe("Failed to setup benchmark environment: " + e.getMessage());
      throw new RuntimeException("Benchmark setup failed", e);
    }
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() {
    try {
      if (store != null) store.close();
      if (engine != null) engine.close();
    } catch (Exception e) {
      LOGGER.warning("Error during benchmark cleanup: " + e.getMessage());
    }
  }

  @Setup(Level.Iteration)
  public void setupIteration() {
    // Initialize memory with test pattern
    byte[] testData = new byte[dataSize];
    for (int i = 0; i < dataSize; i++) {
      testData[i] = (byte) (i % 256);
    }
    memory.writeBytes(0, testData, 0, dataSize);
  }

  /** Benchmark bulk memory.copy operation. */
  @Benchmark
  public void benchmarkBulkMemoryCopy(Blackhole bh) {
    int srcOffset = 0;
    int destOffset = dataSize * 2;

    memory.copy(destOffset, srcOffset, dataSize);
    bh.consume(destOffset);
  }

  /** Benchmark individual byte copying for comparison with bulk copy. */
  @Benchmark
  public void benchmarkIndividualByteCopy(Blackhole bh) {
    int srcOffset = 0;
    int destOffset = dataSize * 2;

    for (int i = 0; i < dataSize; i++) {
      byte value = memory.readByte(srcOffset + i);
      memory.writeByte(destOffset + i, value);
    }
    bh.consume(destOffset);
  }

  /** Benchmark array-based copying for comparison with bulk copy. */
  @Benchmark
  public void benchmarkArrayBasedCopy(Blackhole bh) {
    int srcOffset = 0;
    int destOffset = dataSize * 2;

    byte[] tempBuffer = new byte[dataSize];
    memory.readBytes(srcOffset, tempBuffer, 0, dataSize);
    memory.writeBytes(destOffset, tempBuffer, 0, dataSize);
    bh.consume(destOffset);
  }

  /** Benchmark bulk memory.fill operation. */
  @Benchmark
  public void benchmarkBulkMemoryFill(Blackhole bh) {
    int offset = dataSize * 3;
    byte fillValue = (byte) 0xAA;

    memory.fill(offset, fillValue, dataSize);
    bh.consume(offset);
  }

  /** Benchmark individual byte filling for comparison with bulk fill. */
  @Benchmark
  public void benchmarkIndividualByteFill(Blackhole bh) {
    int offset = dataSize * 3;
    byte fillValue = (byte) 0xAA;

    for (int i = 0; i < dataSize; i++) {
      memory.writeByte(offset + i, fillValue);
    }
    bh.consume(offset);
  }

  /** Benchmark array-based filling for comparison with bulk fill. */
  @Benchmark
  public void benchmarkArrayBasedFill(Blackhole bh) {
    int offset = dataSize * 3;
    byte fillValue = (byte) 0xAA;

    byte[] fillBuffer = new byte[dataSize];
    java.util.Arrays.fill(fillBuffer, fillValue);
    memory.writeBytes(offset, fillBuffer, 0, dataSize);
    bh.consume(offset);
  }

  /** Benchmark overlapping memory copy (forward overlap). */
  @Benchmark
  public void benchmarkOverlappingCopyForward(Blackhole bh) {
    int srcOffset = 0;
    int destOffset = dataSize / 2; // 50% overlap
    int copyLength = dataSize / 2;

    memory.copy(destOffset, srcOffset, copyLength);
    bh.consume(destOffset);
  }

  /** Benchmark overlapping memory copy (backward overlap). */
  @Benchmark
  public void benchmarkOverlappingCopyBackward(Blackhole bh) {
    int srcOffset = dataSize / 2;
    int destOffset = 0; // 50% overlap
    int copyLength = dataSize / 2;

    memory.copy(destOffset, srcOffset, copyLength);
    bh.consume(destOffset);
  }

  /** Benchmark memory operations across page boundaries. */
  @Benchmark
  public void benchmarkCrossPageBoundary(Blackhole bh) {
    int pageSize = 65536;
    int srcOffset = pageSize - dataSize / 2; // Straddles page boundary
    int destOffset = pageSize * 2 - dataSize / 2;

    memory.copy(destOffset, srcOffset, dataSize);
    bh.consume(destOffset);
  }

  /** Benchmark memory fill with pattern data. */
  @Benchmark
  public void benchmarkPatternFill(Blackhole bh) {
    int offset = 0;

    // Fill with alternating pattern (simulates more complex scenarios)
    for (int i = 0; i < dataSize; i += 64) {
      int fillSize = Math.min(64, dataSize - i);
      byte fillValue = (byte) ((i / 64) % 256);
      memory.fill(offset + i, fillValue, fillSize);
    }
    bh.consume(offset);
  }

  /** Benchmark combined operations (realistic usage pattern). */
  @Benchmark
  public void benchmarkCombinedOperations(Blackhole bh) {
    int baseOffset = 0;

    // Realistic pattern: fill, copy, fill again
    memory.fill(baseOffset, (byte) 0x00, dataSize / 4);
    memory.copy(baseOffset + dataSize / 4, baseOffset, dataSize / 4);
    memory.fill(baseOffset + dataSize / 2, (byte) 0xFF, dataSize / 2);

    bh.consume(baseOffset);
  }

  /** Benchmark ByteBuffer-based operations for comparison. */
  @Benchmark
  public void benchmarkByteBufferOperations(Blackhole bh) {
    java.nio.ByteBuffer buffer = memory.getBuffer();
    int srcOffset = 0;
    int destOffset = dataSize * 2;

    // Use ByteBuffer bulk operations
    buffer.position(srcOffset);
    byte[] temp = new byte[dataSize];
    buffer.get(temp);

    buffer.position(destOffset);
    buffer.put(temp);

    bh.consume(destOffset);
  }

  /** Main method to run the benchmarks. */
  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(BulkMemoryOperationsBenchmark.class.getSimpleName())
            .jvmArgs("-Xmx2g", "-XX:+UseG1GC")
            .build();

    new Runner(opt).run();
  }
}
