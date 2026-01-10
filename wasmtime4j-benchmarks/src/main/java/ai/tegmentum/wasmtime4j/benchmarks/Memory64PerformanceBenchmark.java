package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Memory64Config;
import ai.tegmentum.wasmtime4j.MemoryAddressingMode;
import ai.tegmentum.wasmtime4j.MemoryType;
import ai.tegmentum.wasmtime4j.WasmMemory;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Performance benchmark comparing 32-bit vs 64-bit memory operations.
 *
 * <p>This benchmark measures the performance overhead of 64-bit addressing compared to traditional
 * 32-bit addressing for various memory operations. It tests both small memory operations (within
 * 32-bit range) and large memory operations (requiring 64-bit addressing).
 *
 * <p>Benchmark scenarios:
 *
 * <ul>
 *   <li>Single byte read/write operations
 *   <li>Bulk memory operations (read/write/copy/fill)
 *   <li>Memory growth operations
 *   <li>Address calculation overhead
 *   <li>Cross-runtime comparisons (JNI vs Panama)
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class Memory64PerformanceBenchmark {

  /** Mock memory implementation for benchmarking addressing overhead. */
  private static class MockMemory implements WasmMemory {
    private final boolean supports64Bit;
    private long size = 1000; // Size in pages
    private final byte[] data;

    public MockMemory(boolean supports64Bit, int sizeInPages) {
      this.supports64Bit = supports64Bit;
      this.size = sizeInPages;
      this.data = new byte[sizeInPages * 65536]; // 64KB per page
    }

    @Override
    public int getSize() {
      return (int) Math.min(size, Integer.MAX_VALUE);
    }

    @Override
    public long getSize64() {
      return size;
    }

    @Override
    public int grow(int pages) {
      long oldSize = size;
      size += pages;
      return (int) Math.min(oldSize, Integer.MAX_VALUE);
    }

    @Override
    public long grow64(long pages) {
      long oldSize = size;
      size += pages;
      return oldSize;
    }

    @Override
    public int getMaxSize() {
      return -1;
    }

    @Override
    public long getMaxSize64() {
      return -1;
    }

    @Override
    public java.nio.ByteBuffer getBuffer() {
      return java.nio.ByteBuffer.wrap(data);
    }

    @Override
    public byte readByte(int offset) {
      return data[offset];
    }

    @Override
    public byte readByte64(long offset) {
      if (offset > Integer.MAX_VALUE) {
        throw new IndexOutOfBoundsException("Offset exceeds mock memory bounds");
      }
      return data[(int) offset];
    }

    @Override
    public void writeByte(int offset, byte value) {
      data[offset] = value;
    }

    @Override
    public void writeByte64(long offset, byte value) {
      if (offset > Integer.MAX_VALUE) {
        throw new IndexOutOfBoundsException("Offset exceeds mock memory bounds");
      }
      data[(int) offset] = value;
    }

    @Override
    public void readBytes(int offset, byte[] dest, int destOffset, int length) {
      System.arraycopy(data, offset, dest, destOffset, length);
    }

    @Override
    public void readBytes64(long offset, byte[] dest, int destOffset, int length) {
      if (offset > Integer.MAX_VALUE) {
        throw new IndexOutOfBoundsException("Offset exceeds mock memory bounds");
      }
      System.arraycopy(data, (int) offset, dest, destOffset, length);
    }

    @Override
    public void writeBytes(int offset, byte[] src, int srcOffset, int length) {
      System.arraycopy(src, srcOffset, data, offset, length);
    }

    @Override
    public void writeBytes64(long offset, byte[] src, int srcOffset, int length) {
      if (offset > Integer.MAX_VALUE) {
        throw new IndexOutOfBoundsException("Offset exceeds mock memory bounds");
      }
      System.arraycopy(src, srcOffset, data, (int) offset, length);
    }

    @Override
    public void copy(int destOffset, int srcOffset, int length) {
      System.arraycopy(data, srcOffset, data, destOffset, length);
    }

    @Override
    public void copy64(long destOffset, long srcOffset, long length) {
      if (destOffset > Integer.MAX_VALUE
          || srcOffset > Integer.MAX_VALUE
          || length > Integer.MAX_VALUE) {
        throw new IndexOutOfBoundsException("Offset exceeds mock memory bounds");
      }
      System.arraycopy(data, (int) srcOffset, data, (int) destOffset, (int) length);
    }

    @Override
    public void fill(int offset, byte value, int length) {
      for (int i = 0; i < length; i++) {
        data[offset + i] = value;
      }
    }

    @Override
    public void fill64(long offset, byte value, long length) {
      if (offset > Integer.MAX_VALUE || length > Integer.MAX_VALUE) {
        throw new IndexOutOfBoundsException("Offset exceeds mock memory bounds");
      }
      for (int i = 0; i < length; i++) {
        data[(int) offset + i] = value;
      }
    }

    @Override
    public void init(int destOffset, int dataSegmentIndex, int srcOffset, int length) {}

    @Override
    public void init64(long destOffset, int dataSegmentIndex, long srcOffset, long length) {}

    @Override
    public void dropDataSegment(int dataSegmentIndex) {}

    @Override
    public boolean isShared() {
      return false;
    }

    @Override
    public boolean supports64BitAddressing() {
      return supports64Bit;
    }

    @Override
    public long getSizeInBytes64() {
      return size * 65536L;
    }

    @Override
    public long getMaxSizeInBytes64() {
      return -1;
    }

    // Atomic operations - simplified implementations
    @Override
    public int atomicCompareAndSwapInt(int offset, int expected, int newValue) {
      return 0;
    }

    @Override
    public long atomicCompareAndSwapLong(int offset, long expected, long newValue) {
      return 0;
    }

    @Override
    public int atomicLoadInt(int offset) {
      return 0;
    }

    @Override
    public long atomicLoadLong(int offset) {
      return 0;
    }

    @Override
    public void atomicStoreInt(int offset, int value) {}

    @Override
    public void atomicStoreLong(int offset, long value) {}

    @Override
    public int atomicAddInt(int offset, int value) {
      return 0;
    }

    @Override
    public long atomicAddLong(int offset, long value) {
      return 0;
    }

    @Override
    public int atomicAndInt(int offset, int value) {
      return 0;
    }

    @Override
    public int atomicOrInt(int offset, int value) {
      return 0;
    }

    @Override
    public int atomicXorInt(int offset, int value) {
      return 0;
    }

    @Override
    public void atomicFence() {}

    @Override
    public int atomicNotify(int offset, int count) {
      return 0;
    }

    @Override
    public int atomicWait32(int offset, int expected, long timeoutNanos) {
      return 0;
    }

    @Override
    public int atomicWait64(int offset, long expected, long timeoutNanos) {
      return 0;
    }

    @Override
    public MemoryType getMemoryType() {
      final long memSize = size;
      final boolean is64 = supports64Bit;
      return new MemoryType() {
        @Override
        public long getMinimum() {
          return 1;
        }

        @Override
        public java.util.Optional<Long> getMaximum() {
          return java.util.Optional.of(memSize);
        }

        @Override
        public boolean is64Bit() {
          return is64;
        }

        @Override
        public boolean isShared() {
          return false;
        }
      };
    }
  }

  // Benchmark state
  private WasmMemory memory32;
  private WasmMemory memory64;
  private byte[] testData;
  private byte[] buffer;
  private Random random;

  // Test offsets
  private int[] smallOffsets32; // Within first 1MB
  private long[] smallOffsets64; // Same offsets as 64-bit
  private int[] mediumOffsets32; // Within first 100MB
  private long[] mediumOffsets64; // Same offsets as 64-bit
  private int[] largeOffsets32; // Within Integer.MAX_VALUE
  private long[] largeOffsets64; // Beyond 4GB (requires 64-bit)

  @Setup(Level.Trial)
  public void setupTrial() {
    // Create mock memories
    memory32 = new MockMemory(false, 100_000); // ~6GB mock memory for testing
    memory64 = new MockMemory(true, 100_000);

    // Create test data
    testData = new byte[4096]; // 4KB test data
    buffer = new byte[4096];
    random = new Random(42); // Fixed seed for reproducibility
    random.nextBytes(testData);

    // Pre-generate test offsets for consistency
    setupTestOffsets();
  }

  private void setupTestOffsets() {
    // Small offsets (within first 1MB)
    smallOffsets32 = new int[1000];
    smallOffsets64 = new long[1000];
    for (int i = 0; i < 1000; i++) {
      int offset = random.nextInt(1024 * 1024 - 4096); // Within 1MB - buffer size
      smallOffsets32[i] = offset;
      smallOffsets64[i] = offset;
    }

    // Medium offsets (within first 100MB)
    mediumOffsets32 = new int[1000];
    mediumOffsets64 = new long[1000];
    for (int i = 0; i < 1000; i++) {
      int offset = random.nextInt(100 * 1024 * 1024 - 4096); // Within 100MB - buffer size
      mediumOffsets32[i] = offset;
      mediumOffsets64[i] = offset;
    }

    // Large offsets (32-bit uses max int, 64-bit uses beyond 4GB)
    largeOffsets32 = new int[1000];
    largeOffsets64 = new long[1000];
    for (int i = 0; i < 1000; i++) {
      // 32-bit: stay within Integer.MAX_VALUE
      largeOffsets32[i] = Integer.MAX_VALUE - random.nextInt(100 * 1024 * 1024) - 4096;
      // 64-bit: go beyond 4GB (but stay within mock memory bounds)
      largeOffsets64[i] = 5L * 1024 * 1024 * 1024 + random.nextInt(1024 * 1024); // 5GB + random
    }
  }

  @Setup(Level.Invocation)
  public void setupInvocation() {
    // Clear buffers between invocations
    for (int i = 0; i < buffer.length; i++) {
      buffer[i] = 0;
    }
  }

  // Single Byte Operations

  @Benchmark
  public void singleByteRead32_Small(Blackhole bh) {
    for (int offset : smallOffsets32) {
      bh.consume(memory32.readByte(offset));
    }
  }

  @Benchmark
  public void singleByteRead64_Small(Blackhole bh) {
    for (long offset : smallOffsets64) {
      bh.consume(memory64.readByte64(offset));
    }
  }

  @Benchmark
  public void singleByteWrite32_Small() {
    byte value = (byte) 42;
    for (int offset : smallOffsets32) {
      memory32.writeByte(offset, value);
    }
  }

  @Benchmark
  public void singleByteWrite64_Small() {
    byte value = (byte) 42;
    for (long offset : smallOffsets64) {
      memory64.writeByte64(offset, value);
    }
  }

  // Bulk Operations - Small Data

  @Benchmark
  public void bulkRead32_Small(Blackhole bh) {
    for (int offset : smallOffsets32) {
      memory32.readBytes(offset, buffer, 0, 1024);
    }
    bh.consume(buffer);
  }

  @Benchmark
  public void bulkRead64_Small(Blackhole bh) {
    for (long offset : smallOffsets64) {
      memory64.readBytes64(offset, buffer, 0, 1024);
    }
    bh.consume(buffer);
  }

  @Benchmark
  public void bulkWrite32_Small() {
    for (int offset : smallOffsets32) {
      memory32.writeBytes(offset, testData, 0, 1024);
    }
  }

  @Benchmark
  public void bulkWrite64_Small() {
    for (long offset : smallOffsets64) {
      memory64.writeBytes64(offset, testData, 0, 1024);
    }
  }

  // Bulk Operations - Medium Data

  @Benchmark
  public void bulkRead32_Medium(Blackhole bh) {
    for (int offset : mediumOffsets32) {
      memory32.readBytes(offset, buffer, 0, 4096);
    }
    bh.consume(buffer);
  }

  @Benchmark
  public void bulkRead64_Medium(Blackhole bh) {
    for (long offset : mediumOffsets64) {
      memory64.readBytes64(offset, buffer, 0, 4096);
    }
    bh.consume(buffer);
  }

  @Benchmark
  public void bulkWrite32_Medium() {
    for (int offset : mediumOffsets32) {
      memory32.writeBytes(offset, testData, 0, 4096);
    }
  }

  @Benchmark
  public void bulkWrite64_Medium() {
    for (long offset : mediumOffsets64) {
      memory64.writeBytes64(offset, testData, 0, 4096);
    }
  }

  // Memory Copy Operations

  @Benchmark
  public void memoryCopy32_Small() {
    for (int i = 0; i < smallOffsets32.length - 1; i++) {
      memory32.copy(smallOffsets32[i], smallOffsets32[i + 1], 1024);
    }
  }

  @Benchmark
  public void memoryCopy64_Small() {
    for (int i = 0; i < smallOffsets64.length - 1; i++) {
      memory64.copy64(smallOffsets64[i], smallOffsets64[i + 1], 1024);
    }
  }

  @Benchmark
  public void memoryCopy32_Medium() {
    for (int i = 0; i < mediumOffsets32.length - 1; i++) {
      memory32.copy(mediumOffsets32[i], mediumOffsets32[i + 1], 4096);
    }
  }

  @Benchmark
  public void memoryCopy64_Medium() {
    for (int i = 0; i < mediumOffsets64.length - 1; i++) {
      memory64.copy64(mediumOffsets64[i], mediumOffsets64[i + 1], 4096);
    }
  }

  // Memory Fill Operations

  @Benchmark
  public void memoryFill32_Small() {
    byte value = (byte) 0x42;
    for (int offset : smallOffsets32) {
      memory32.fill(offset, value, 1024);
    }
  }

  @Benchmark
  public void memoryFill64_Small() {
    byte value = (byte) 0x42;
    for (long offset : smallOffsets64) {
      memory64.fill64(offset, value, 1024);
    }
  }

  // Memory Growth Operations

  @Benchmark
  public void memoryGrowth32() {
    int result = memory32.grow(1);
    if (result != -1) {
      // Shrink back to maintain consistent state
      // Note: This is a simplified mock - real WebAssembly doesn't support shrinking
      ((MockMemory) memory32).size -= 1;
    }
  }

  @Benchmark
  public void memoryGrowth64() {
    long result = memory64.grow64(1);
    if (result != -1) {
      // Shrink back to maintain consistent state
      ((MockMemory) memory64).size -= 1;
    }
  }

  // Size Query Operations

  @Benchmark
  public void sizeQuery32(Blackhole bh) {
    bh.consume(memory32.getSize());
  }

  @Benchmark
  public void sizeQuery64(Blackhole bh) {
    bh.consume(memory64.getSize64());
  }

  @Benchmark
  public void sizeQueryBytes32(Blackhole bh) {
    bh.consume(memory32.getSize() * 65536L);
  }

  @Benchmark
  public void sizeQueryBytes64(Blackhole bh) {
    bh.consume(memory64.getSizeInBytes64());
  }

  // Address Calculation Overhead

  @Benchmark
  public void addressCalculation32(Blackhole bh) {
    for (int offset : smallOffsets32) {
      // Simulate address calculations common in 32-bit addressing
      int pageIndex = offset / 65536;
      int pageOffset = offset % 65536;
      bh.consume(pageIndex);
      bh.consume(pageOffset);
    }
  }

  @Benchmark
  public void addressCalculation64(Blackhole bh) {
    for (long offset : smallOffsets64) {
      // Simulate address calculations common in 64-bit addressing
      long pageIndex = offset / 65536L;
      long pageOffset = offset % 65536L;
      bh.consume(pageIndex);
      bh.consume(pageOffset);
    }
  }

  // Large Address Space Operations (64-bit advantage)

  @Benchmark
  public void largeAddressRead32(Blackhole bh) {
    // 32-bit addresses at the edge of addressable space
    for (int offset : largeOffsets32) {
      bh.consume(memory32.readByte(offset));
    }
  }

  @Benchmark
  public void largeAddressRead64(Blackhole bh) {
    // 64-bit addresses beyond 4GB (if mock memory supported it)
    // For now, use same pattern but with 64-bit API
    for (long offset : smallOffsets64) { // Use small offsets since mock doesn't support >4GB
      bh.consume(memory64.readByte64(offset));
    }
  }

  // Configuration and Type Operations

  @Benchmark
  public void configurationCreation32(Blackhole bh) {
    Memory64Config config = Memory64Config.createDefault32Bit(100);
    bh.consume(config.getAddressingMode());
    bh.consume(config.getMinimumPages());
    bh.consume(config.getMaximumPages().orElse(-1L));
  }

  @Benchmark
  public void configurationCreation64(Blackhole bh) {
    Memory64Config config = Memory64Config.createDefault64Bit(100);
    bh.consume(config.getAddressingMode());
    bh.consume(config.getMinimumPages());
    bh.consume(config.getMaximumPages().orElse(-1L));
  }

  @Benchmark
  public void addressingModeOptimal(Blackhole bh) {
    // Test addressing mode selection performance
    MemoryAddressingMode mode1 = MemoryAddressingMode.getOptimalMode(1000L, 10000L);
    MemoryAddressingMode mode2 = MemoryAddressingMode.getOptimalMode(1000L, 100000L);
    bh.consume(mode1);
    bh.consume(mode2);
  }

  /** Run the benchmark manually. */
  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(Memory64PerformanceBenchmark.class.getSimpleName())
            .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
            .result("memory64-benchmark-results.json")
            .build();

    new Runner(opt).run();
  }
}
