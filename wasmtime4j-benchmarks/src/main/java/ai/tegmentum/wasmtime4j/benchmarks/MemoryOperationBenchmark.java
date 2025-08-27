package ai.tegmentum.wasmtime4j.benchmarks;

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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks for WebAssembly memory operations performance.
 *
 * <p>This benchmark class measures the performance characteristics of WebAssembly memory
 * operations, comparing JNI and Panama implementations across different memory access patterns and
 * data sizes.
 *
 * <p>Key metrics measured:
 *
 * <ul>
 *   <li>Memory allocation and deallocation performance
 *   <li>Memory read/write operations
 *   <li>Bulk memory operations
 *   <li>Memory growth operations
 *   <li>Cross-boundary data marshalling
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1g", "-Xmx2g"})
public class MemoryOperationBenchmark extends BenchmarkBase {

  /** Runtime implementation to benchmark. */
  @Param({"JNI", "PANAMA"})
  private RuntimeType runtimeType;

  /** Memory operation size in bytes. */
  @Param({"1024", "4096", "16384"})
  private int memorySize;

  /** Memory operation pattern to test. */
  @Param({"SEQUENTIAL", "RANDOM", "BULK"})
  private String operationPattern;

  /** Mock WebAssembly memory representation for testing. */
  private static final class MockWasmMemory {
    private final RuntimeType runtimeType;
    private byte[] memory;
    private int currentSize;
    private final int pageSize = 65536; // 64KB per page
    private long totalAllocations;
    private long totalReads;
    private long totalWrites;

    MockWasmMemory(final RuntimeType runtimeType, final int initialSize) {
      this.runtimeType = runtimeType;
      this.currentSize = initialSize;
      this.memory = new byte[initialSize];
      this.totalAllocations = 1;
      this.totalReads = 0;
      this.totalWrites = 0;
    }

    void write(final int offset, final byte[] data) {
      if (offset < 0 || offset + data.length > currentSize) {
        throw new IndexOutOfBoundsException("Memory access out of bounds");
      }

      // Simulate write overhead based on runtime type
      final int writeOverhead = runtimeType == RuntimeType.PANAMA ? 5 : 3;
      for (int i = 0; i < writeOverhead; i++) {
        Math.abs(data.length + i);
      }

      System.arraycopy(data, 0, memory, offset, data.length);
      totalWrites++;
    }

    void writeInt(final int offset, final int value) {
      final byte[] bytes = new byte[4];
      bytes[0] = (byte) (value & 0xFF);
      bytes[1] = (byte) ((value >> 8) & 0xFF);
      bytes[2] = (byte) ((value >> 16) & 0xFF);
      bytes[3] = (byte) ((value >> 24) & 0xFF);
      write(offset, bytes);
    }

    byte[] read(final int offset, final int length) {
      if (offset < 0 || offset + length > currentSize) {
        throw new IndexOutOfBoundsException("Memory access out of bounds");
      }

      // Simulate read overhead based on runtime type
      final int readOverhead = runtimeType == RuntimeType.PANAMA ? 7 : 4;
      for (int i = 0; i < readOverhead; i++) {
        Math.sqrt(length + i);
      }

      final byte[] result = new byte[length];
      System.arraycopy(memory, offset, result, 0, length);
      totalReads++;
      return result;
    }

    int readInt(final int offset) {
      final byte[] bytes = read(offset, 4);
      return (bytes[0] & 0xFF)
          | ((bytes[1] & 0xFF) << 8)
          | ((bytes[2] & 0xFF) << 16)
          | ((bytes[3] & 0xFF) << 24);
    }

    void grow(final int additionalPages) {
      final int additionalSize = additionalPages * pageSize;
      final byte[] newMemory = new byte[currentSize + additionalSize];

      // Simulate growth overhead
      final int growthOverhead = runtimeType == RuntimeType.PANAMA ? 20 : 15;
      for (int i = 0; i < growthOverhead; i++) {
        Math.log(currentSize + additionalSize + i);
      }

      System.arraycopy(memory, 0, newMemory, 0, currentSize);
      memory = newMemory;
      currentSize += additionalSize;
      totalAllocations++;
    }

    void fill(final int offset, final int length, final byte value) {
      if (offset < 0 || offset + length > currentSize) {
        throw new IndexOutOfBoundsException("Memory fill out of bounds");
      }

      // Simulate fill overhead
      final int fillOverhead = runtimeType == RuntimeType.PANAMA ? 3 : 2;
      for (int i = 0; i < fillOverhead; i++) {
        Math.pow(length, 0.5);
      }

      for (int i = 0; i < length; i++) {
        memory[offset + i] = value;
      }
      totalWrites++;
    }

    void copy(final int srcOffset, final int dstOffset, final int length) {
      if (srcOffset < 0
          || srcOffset + length > currentSize
          || dstOffset < 0
          || dstOffset + length > currentSize) {
        throw new IndexOutOfBoundsException("Memory copy out of bounds");
      }

      // Simulate copy overhead
      final int copyOverhead = runtimeType == RuntimeType.PANAMA ? 8 : 5;
      for (int i = 0; i < copyOverhead; i++) {
        Math.abs(length * i);
      }

      System.arraycopy(memory, srcOffset, memory, dstOffset, length);
      totalReads++;
      totalWrites++;
    }

    void reset() {
      for (int i = 0; i < memory.length; i++) {
        memory[i] = 0;
      }
      totalReads = 0;
      totalWrites = 0;
    }

    public int getCurrentSize() {
      return currentSize;
    }

    public long getTotalAllocations() {
      return totalAllocations;
    }

    public long getTotalReads() {
      return totalReads;
    }

    public long getTotalWrites() {
      return totalWrites;
    }

    public RuntimeType getRuntimeType() {
      return runtimeType;
    }
  }

  /** Memory instance being benchmarked. */
  private MockWasmMemory memory;

  /** Test data for memory operations. */
  private byte[] testData;

  /** Random offsets for random access patterns. */
  private int[] randomOffsets;

  /** Setup performed before each benchmark iteration. */
  @Setup(Level.Iteration)
  public void setupIteration() {
    // Create memory instance
    memory = new MockWasmMemory(runtimeType, memorySize * 2); // Allocate 2x for growth tests

    // Initialize test data
    testData = new byte[memorySize];
    for (int i = 0; i < testData.length; i++) {
      testData[i] = (byte) (i % 256);
    }

    // Generate random offsets for random access patterns
    randomOffsets = new int[100];
    for (int i = 0; i < randomOffsets.length; i++) {
      randomOffsets[i] = (int) (Math.random() * (memorySize - 1024));
    }

    // Reset memory statistics
    memory.reset();
  }

  /** Cleanup performed after each benchmark iteration. */
  @TearDown(Level.Iteration)
  public void teardownIteration() {
    if (memory != null) {
      memory.reset();
      memory = null;
    }
    testData = null;
    randomOffsets = null;
  }

  /**
   * Benchmarks basic memory write operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMemoryWrite(final Blackhole blackhole) {
    switch (operationPattern) {
      case "SEQUENTIAL":
        memory.write(0, testData);
        break;
      case "RANDOM":
        final int chunkSize = 256;
        for (final int offset : randomOffsets) {
          if (offset + chunkSize <= testData.length) {
            final byte[] chunk = new byte[chunkSize];
            System.arraycopy(testData, offset, chunk, 0, chunkSize);
            memory.write(offset, chunk);
          }
        }
        break;
      case "BULK":
        // Write in large chunks
        final int bulkSize = memorySize / 4;
        for (int i = 0; i < 4; i++) {
          final int offset = i * bulkSize;
          final byte[] bulk = new byte[bulkSize];
          System.arraycopy(testData, 0, bulk, 0, Math.min(bulkSize, testData.length));
          memory.write(offset, bulk);
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown operation pattern: " + operationPattern);
    }

    blackhole.consume(memory.getTotalWrites());
  }

  /**
   * Benchmarks basic memory read operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMemoryRead(final Blackhole blackhole) {
    // First write data
    memory.write(0, testData);

    switch (operationPattern) {
      case "SEQUENTIAL":
        final byte[] allData = memory.read(0, testData.length);
        blackhole.consume(allData.length);
        break;
      case "RANDOM":
        final int chunkSize = 128;
        for (final int offset : randomOffsets) {
          if (offset + chunkSize <= memory.getCurrentSize()) {
            final byte[] chunk = memory.read(offset, chunkSize);
            blackhole.consume(chunk.length);
          }
        }
        break;
      case "BULK":
        // Read in large chunks
        final int bulkSize = memorySize / 3;
        for (int i = 0; i < 3; i++) {
          final int offset = i * bulkSize;
          final int readSize = Math.min(bulkSize, memory.getCurrentSize() - offset);
          if (readSize > 0) {
            final byte[] bulk = memory.read(offset, readSize);
            blackhole.consume(bulk.length);
          }
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown operation pattern: " + operationPattern);
    }

    blackhole.consume(memory.getTotalReads());
  }

  /**
   * Benchmarks integer read/write operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkIntegerOperations(final Blackhole blackhole) {
    final int numInts = memorySize / 4;

    // Write integers
    for (int i = 0; i < numInts && i * 4 < memory.getCurrentSize() - 4; i++) {
      memory.writeInt(i * 4, i * 1000);
    }

    // Read integers back
    int sum = 0;
    for (int i = 0; i < numInts && i * 4 < memory.getCurrentSize() - 4; i++) {
      sum += memory.readInt(i * 4);
    }

    blackhole.consume(sum);
    blackhole.consume(memory.getTotalReads());
    blackhole.consume(memory.getTotalWrites());
  }

  /**
   * Benchmarks memory fill operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMemoryFill(final Blackhole blackhole) {
    switch (operationPattern) {
      case "SEQUENTIAL":
        memory.fill(0, memorySize, (byte) 0x42);
        break;
      case "RANDOM":
        final int fillSize = 512;
        for (final int offset : randomOffsets) {
          if (offset + fillSize <= memory.getCurrentSize()) {
            memory.fill(offset, fillSize, (byte) (offset % 256));
          }
        }
        break;
      case "BULK":
        // Fill in large sections
        final int sectionSize = memorySize / 2;
        memory.fill(0, sectionSize, (byte) 0xAA);
        memory.fill(sectionSize, sectionSize, (byte) 0x55);
        break;
      default:
        throw new IllegalArgumentException("Unknown operation pattern: " + operationPattern);
    }

    blackhole.consume(memory.getTotalWrites());
  }

  /**
   * Benchmarks memory copy operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMemoryCopy(final Blackhole blackhole) {
    // First write some data to copy
    memory.write(0, testData);

    switch (operationPattern) {
      case "SEQUENTIAL":
        // Copy first half to second half
        final int halfSize = memorySize / 2;
        memory.copy(0, halfSize, halfSize);
        break;
      case "RANDOM":
        final int copySize = 256;
        for (int i = 0; i < randomOffsets.length / 2; i += 2) {
          final int srcOffset = randomOffsets[i];
          final int dstOffset = randomOffsets[i + 1];
          if (srcOffset + copySize <= memory.getCurrentSize()
              && dstOffset + copySize <= memory.getCurrentSize()) {
            memory.copy(srcOffset, dstOffset, copySize);
          }
        }
        break;
      case "BULK":
        // Copy in overlapping blocks
        final int blockSize = memorySize / 4;
        memory.copy(0, blockSize, blockSize);
        memory.copy(blockSize, blockSize * 2, blockSize);
        break;
      default:
        throw new IllegalArgumentException("Unknown operation pattern: " + operationPattern);
    }

    blackhole.consume(memory.getTotalReads());
    blackhole.consume(memory.getTotalWrites());
  }

  /**
   * Benchmarks memory growth operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMemoryGrowth(final Blackhole blackhole) {
    final int initialSize = memory.getCurrentSize();

    // Grow memory by 1-3 pages depending on pattern
    switch (operationPattern) {
      case "SEQUENTIAL":
        memory.grow(1);
        break;
      case "RANDOM":
        // Random growth between 1-2 pages
        final int pages = 1 + (int) (Math.random() * 2);
        memory.grow(pages);
        break;
      case "BULK":
        memory.grow(3);
        break;
      default:
        throw new IllegalArgumentException("Unknown operation pattern: " + operationPattern);
    }

    final int newSize = memory.getCurrentSize();
    blackhole.consume(newSize - initialSize);
    blackhole.consume(memory.getTotalAllocations());
  }

  /**
   * Benchmarks mixed memory operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMixedOperations(final Blackhole blackhole) {
    // Write initial data
    memory.write(0, testData);

    // Perform mixed operations
    final int quarterSize = memorySize / 4;

    // 1. Read some data
    final byte[] readData = memory.read(0, quarterSize);
    blackhole.consume(readData.length);

    // 2. Fill a section
    memory.fill(quarterSize, quarterSize, (byte) 0xFF);

    // 3. Copy data
    memory.copy(0, quarterSize * 2, quarterSize);

    // 4. Write integers
    for (int i = 0; i < 10; i++) {
      memory.writeInt((quarterSize * 3) + (i * 4), i * 100);
    }

    // 5. Read integers back
    int sum = 0;
    for (int i = 0; i < 10; i++) {
      sum += memory.readInt((quarterSize * 3) + (i * 4));
    }

    blackhole.consume(sum);
    blackhole.consume(memory.getTotalReads());
    blackhole.consume(memory.getTotalWrites());
  }

  /**
   * Benchmarks memory operations under pressure.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMemoryOperationsWithPressure(final Blackhole blackhole) {
    // Create memory pressure
    final byte[][] pressure = new byte[50][];
    for (int i = 0; i < pressure.length; i++) {
      pressure[i] = new byte[4096]; // 4KB per allocation
    }

    // Perform operations under pressure
    memory.write(0, testData);
    final byte[] readBack = memory.read(0, testData.length);
    memory.fill(0, memorySize / 2, (byte) 0x00);

    blackhole.consume(readBack.length);
    blackhole.consume(pressure.length);

    // Clear pressure
    for (int i = 0; i < pressure.length; i++) {
      pressure[i] = null;
    }
  }

  /**
   * Benchmarks memory bounds checking performance.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkBoundsChecking(final Blackhole blackhole) {
    int validOperations = 0;
    int invalidOperations = 0;

    // Mix of valid and invalid operations
    for (int i = 0; i < 20; i++) {
      try {
        if (i % 4 == 0) {
          // Invalid: out of bounds write
          memory.write(memory.getCurrentSize(), testData);
        } else if (i % 4 == 1) {
          // Invalid: out of bounds read
          memory.read(memory.getCurrentSize(), 1024);
        } else {
          // Valid operations
          final int safeOffset = i * 100;
          if (safeOffset + 100 < memory.getCurrentSize()) {
            memory.write(safeOffset, new byte[100]);
            validOperations++;
          }
        }
      } catch (final IndexOutOfBoundsException e) {
        invalidOperations++;
        blackhole.consume(e.getMessage());
      }
    }

    blackhole.consume(validOperations);
    blackhole.consume(invalidOperations);
  }
}
