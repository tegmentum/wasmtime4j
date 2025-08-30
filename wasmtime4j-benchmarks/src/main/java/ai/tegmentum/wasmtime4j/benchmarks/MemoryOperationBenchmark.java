package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.ByteBuffer;
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
  private String runtimeTypeName;

  /** Memory operation size in bytes. */
  @Param({"1024", "4096", "16384"})
  private int memorySize;

  /** Memory operation pattern to test. */
  @Param({"SEQUENTIAL", "RANDOM", "BULK"})
  private String operationPattern;

  /** WebAssembly runtime components. */
  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private Module module;
  private Instance instance;
  
  /** WebAssembly memory instance. */
  private WasmMemory wasmMemory;
  
  /** Current module bytecode. */
  private byte[] moduleBytes;

  /** Test data for memory operations. */
  private byte[] testData;

  /** Random offsets for random access patterns. */
  private int[] randomOffsets;

  /** GC monitoring beans for analyzing garbage collection impact. */
  private MemoryMXBean memoryBean;
  private java.util.List<GarbageCollectorMXBean> gcBeans;

  /** Setup performed before each benchmark iteration. */
  @Setup(Level.Iteration)
  public void setupIteration() throws WasmException {
    // Create runtime components
    final RuntimeType runtimeType = RuntimeType.valueOf(runtimeTypeName);
    runtime = createRuntime(runtimeType);
    engine = createEngine(runtime);
    store = createStore(engine);
    
    // Use complex module which includes memory
    moduleBytes = COMPLEX_WASM_MODULE.clone();
    
    // Compile and instantiate module
    module = compileModule(engine, moduleBytes);
    instance = instantiateModule(store, module);
    
    // Get WebAssembly memory
    final java.util.Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
    if (!memoryOpt.isPresent()) {
      throw new WasmException("WebAssembly memory not found");
    }
    wasmMemory = memoryOpt.get();

    // Initialize test data
    final int memoryByteSize = wasmMemory.getSize() * 65536; // Convert pages to bytes
    testData = new byte[Math.min(memorySize, memoryByteSize)];
    for (int i = 0; i < testData.length; i++) {
      testData[i] = (byte) (i % 256);
    }

    // Generate random offsets for random access patterns
    randomOffsets = new int[100];
    final int maxOffset = Math.max(1, memoryByteSize - 1024);
    for (int i = 0; i < randomOffsets.length; i++) {
      randomOffsets[i] = (int) (Math.random() * maxOffset);
    }

    // Initialize GC monitoring beans
    memoryBean = ManagementFactory.getMemoryMXBean();
    gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
  }

  /** Cleanup performed after each benchmark iteration. */
  @TearDown(Level.Iteration)
  public void teardownIteration() {
    cleanup();
    testData = null;
    randomOffsets = null;
    moduleBytes = null;
  }
  
  /** Helper method to clean up WebAssembly resources. */
  private void cleanup() {
    try {
      // WasmMemory does not implement AutoCloseable, no need to close
      wasmMemory = null;
      if (instance != null) {
        instance.close();
        instance = null;
      }
      if (module != null) {
        module.close();
        module = null;
      }
      if (store != null) {
        store.close();
        store = null;
      }
      if (engine != null) {
        engine.close();
        engine = null;
      }
      if (runtime != null) {
        runtime.close();
        runtime = null;
      }
    } catch (final Exception e) {
      // Ignore cleanup errors in benchmarks
    }
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
        wasmMemory.writeBytes(0, testData, 0, testData.length);
        break;
      case "RANDOM":
        final int chunkSize = 256;
        for (final int offset : randomOffsets) {
          if (offset + chunkSize <= testData.length && offset + chunkSize <= wasmMemory.getSize()) {
            final byte[] chunk = new byte[chunkSize];
            System.arraycopy(testData, 0, chunk, 0, chunkSize);
            writeToMemory(wasmMemory, offset, chunk);
          }
        }
        break;
      case "BULK":
        // Write in large chunks
        final int bulkSize = Math.min(memorySize / 4, (int) wasmMemory.getSize() / 4);
        for (int i = 0; i < 4; i++) {
          final int offset = i * bulkSize;
          if (offset + bulkSize <= wasmMemory.getSize()) {
            final byte[] bulk = new byte[bulkSize];
            System.arraycopy(testData, 0, bulk, 0, Math.min(bulkSize, testData.length));
            writeToMemory(wasmMemory, offset, bulk);
          }
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown operation pattern: " + operationPattern);
    }
    blackhole.consume(wasmMemory.getSize());
  }

  /**
   * Benchmarks basic memory read operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMemoryRead(final Blackhole blackhole) {
    // First write data
    writeToMemory(wasmMemory, 0, testData);

    switch (operationPattern) {
      case "SEQUENTIAL":
        final byte[] allData = readFromMemory(wasmMemory, 0, testData.length);
        blackhole.consume(allData.length);
        break;
      case "RANDOM":
        final int chunkSize = 128;
        for (final int offset : randomOffsets) {
          if (offset + chunkSize <= wasmMemory.getSize()) {
            final byte[] chunk = readFromMemory(wasmMemory, offset, chunkSize);
            blackhole.consume(chunk.length);
          }
        }
        break;
      case "BULK":
        // Read in large chunks
        final int bulkSize = Math.min(memorySize / 3, (int) wasmMemory.getSize() / 3);
        for (int i = 0; i < 3; i++) {
          final int offset = i * bulkSize;
          final int readSize = Math.min(bulkSize, (int) wasmMemory.getSize() - offset);
          if (readSize > 0) {
            final byte[] bulk = readFromMemory(wasmMemory, offset, readSize);
            blackhole.consume(bulk.length);
          }
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown operation pattern: " + operationPattern);
    }
    blackhole.consume(wasmMemory.getSize());
  }

  /**
   * Benchmarks integer read/write operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkIntegerOperations(final Blackhole blackhole) {
    final int numInts = Math.min(memorySize / 4, (int) wasmMemory.getSize() / 4);

    // Write integers using ByteBuffer
    final ByteBuffer writeBuffer = ByteBuffer.allocate(4);
    for (int i = 0; i < numInts && i * 4 < wasmMemory.getSize() - 4; i++) {
      writeBuffer.clear();
      writeBuffer.putInt(i * 1000);
      writeToMemory(wasmMemory, i * 4, writeBuffer.array());
    }

    // Read integers back
    long sum = 0;
    for (int i = 0; i < numInts && i * 4 < wasmMemory.getSize() - 4; i++) {
      final byte[] intBytes = readFromMemory(wasmMemory, i * 4, 4);
      final ByteBuffer readBuffer = ByteBuffer.wrap(intBytes);
      sum += readBuffer.getInt();
    }

    blackhole.consume(sum);
    blackhole.consume(numInts);
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
        final int fillSize = Math.min(memorySize, (int) wasmMemory.getSize());
        final byte[] fillData = new byte[fillSize];
        for (int i = 0; i < fillData.length; i++) {
          fillData[i] = (byte) 0x42;
        }
        writeToMemory(wasmMemory, 0, fillData);
        break;
      case "RANDOM":
        final int chunkFillSize = 512;
        final byte[] chunkFillData = new byte[chunkFillSize];
        for (final int offset : randomOffsets) {
          if (offset + chunkFillSize <= wasmMemory.getSize()) {
            for (int i = 0; i < chunkFillData.length; i++) {
              chunkFillData[i] = (byte) (offset % 256);
            }
            writeToMemory(wasmMemory, offset, chunkFillData);
          }
        }
        break;
      case "BULK":
        // Fill in large sections
        final int sectionSize = Math.min(memorySize / 2, (int) wasmMemory.getSize() / 2);
        final byte[] section1 = new byte[sectionSize];
        final byte[] section2 = new byte[sectionSize];
        for (int i = 0; i < sectionSize; i++) {
          section1[i] = (byte) 0xAA;
          section2[i] = (byte) 0x55;
        }
        writeToMemory(wasmMemory, 0, section1);
        if (sectionSize * 2 <= wasmMemory.getSize()) {
          writeToMemory(wasmMemory, sectionSize, section2);
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown operation pattern: " + operationPattern);
    }
    blackhole.consume(wasmMemory.getSize());
  }

  /**
   * Benchmarks memory copy operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMemoryCopy(final Blackhole blackhole) {
    // First write some data to copy
    writeToMemory(wasmMemory, 0, testData);

    switch (operationPattern) {
      case "SEQUENTIAL":
        // Copy first half to second half
        final int halfSize = Math.min(memorySize / 2, (int) wasmMemory.getSize() / 2);
        if (halfSize * 2 <= wasmMemory.getSize()) {
          final byte[] copyData = readFromMemory(wasmMemory, 0, halfSize);
          writeToMemory(wasmMemory, halfSize, copyData);
        }
        break;
      case "RANDOM":
        final int copySize = 256;
        for (int i = 0; i < randomOffsets.length / 2; i += 2) {
          final int srcOffset = randomOffsets[i];
          final int dstOffset = randomOffsets[i + 1];
          if (srcOffset + copySize <= wasmMemory.getSize()
              && dstOffset + copySize <= wasmMemory.getSize()) {
            final byte[] copyData = readFromMemory(wasmMemory, srcOffset, copySize);
            writeToMemory(wasmMemory, dstOffset, copyData);
          }
        }
        break;
      case "BULK":
        // Copy in non-overlapping blocks
        final int blockSize = Math.min(memorySize / 4, (int) wasmMemory.getSize() / 4);
        if (blockSize * 3 <= wasmMemory.getSize()) {
          final byte[] block1 = readFromMemory(wasmMemory, 0, blockSize);
          final byte[] block2 = readFromMemory(wasmMemory, blockSize, blockSize);
          writeToMemory(wasmMemory, blockSize, block1);
          writeToMemory(wasmMemory, blockSize * 2, block2);
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown operation pattern: " + operationPattern);
    }
    blackhole.consume(wasmMemory.getSize());
  }

  /**
   * Benchmarks memory growth operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMemoryGrowth(final Blackhole blackhole) {
    final long initialSize = wasmMemory.getSize();

    // Grow memory by 1-3 pages depending on pattern
    int pages;
    switch (operationPattern) {
      case "SEQUENTIAL":
        pages = 1;
        break;
      case "RANDOM":
        // Random growth between 1-2 pages
        pages = 1 + (int) (Math.random() * 2);
        break;
      case "BULK":
        pages = 3;
        break;
      default:
        throw new IllegalArgumentException("Unknown operation pattern: " + operationPattern);
    }
    
    final long oldSize = wasmMemory.getSize();
    final int growthResult = wasmMemory.grow(pages);
    final long newSize = wasmMemory.getSize();
    
    blackhole.consume(newSize - oldSize);
    blackhole.consume(growthResult);
  }

  /**
   * Benchmarks mixed memory operations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMixedOperations(final Blackhole blackhole) {
    // Write initial data
    writeToMemory(wasmMemory, 0, testData);

    // Perform mixed operations
    final int quarterSize = Math.min(memorySize / 4, (int) wasmMemory.getSize() / 4);

    // 1. Read some data
    final byte[] readData = readFromMemory(wasmMemory, 0, quarterSize);
    blackhole.consume(readData.length);

    // 2. Fill a section
    if (quarterSize * 2 <= wasmMemory.getSize()) {
      final byte[] fillData = new byte[quarterSize];
      for (int i = 0; i < fillData.length; i++) {
        fillData[i] = (byte) 0xFF;
      }
      writeToMemory(wasmMemory, quarterSize, fillData);
    }

    // 3. Copy data
    if (quarterSize * 3 <= wasmMemory.getSize()) {
      final byte[] copyData = readFromMemory(wasmMemory, 0, quarterSize);
      writeToMemory(wasmMemory, quarterSize * 2, copyData);
    }

    // 4. Write integers
    final ByteBuffer intBuffer = ByteBuffer.allocate(4);
    for (int i = 0; i < 10; i++) {
      final int offset = (quarterSize * 3) + (i * 4);
      if (offset + 4 <= wasmMemory.getSize()) {
        intBuffer.clear();
        intBuffer.putInt(i * 100);
        writeToMemory(wasmMemory, offset, intBuffer.array());
      }
    }

    // 5. Read integers back
    long sum = 0;
    for (int i = 0; i < 10; i++) {
      final int offset = (quarterSize * 3) + (i * 4);
      if (offset + 4 <= wasmMemory.getSize()) {
        final byte[] intBytes = readFromMemory(wasmMemory, offset, 4);
        final ByteBuffer readBuffer = ByteBuffer.wrap(intBytes);
        sum += readBuffer.getInt();
      }
    }

    blackhole.consume(sum);
    blackhole.consume(quarterSize);
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

    try {
      // Perform operations under pressure
      writeToMemory(wasmMemory, 0, testData);
      final byte[] readBack = readFromMemory(wasmMemory, 0, testData.length);
      
      final int fillSize = Math.min(memorySize / 2, (int) wasmMemory.getSize() / 2);
      final byte[] fillData = new byte[fillSize];
      writeToMemory(wasmMemory, 0, fillData);

      blackhole.consume(readBack.length);
      blackhole.consume(pressure.length);
    } finally {
      // Clear pressure
      for (int i = 0; i < pressure.length; i++) {
        pressure[i] = null;
      }
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
      if (i % 4 == 0) {
        // Invalid: out of bounds write
        writeToMemory(wasmMemory, Math.toIntExact(wasmMemory.getSize()), testData);
      } else if (i % 4 == 1) {
        // Invalid: out of bounds read
        readFromMemory(wasmMemory, Math.toIntExact(wasmMemory.getSize()), 1024);
      } else {
        // Valid operations
        final int safeOffset = i * 100;
        if (safeOffset + 100 < wasmMemory.getSize()) {
          writeToMemory(wasmMemory, safeOffset, new byte[100]);
          validOperations++;
        }
      }
    }

    blackhole.consume(validOperations);
    blackhole.consume(invalidOperations);
  }

  /**
   * Benchmarks memory operations with GC impact analysis.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMemoryWithGcAnalysis(final Blackhole blackhole) {
    // Capture GC state before operations
    final GcStatistics gcBefore = captureGcStatistics();
    final MemoryUsage heapBefore = memoryBean.getHeapMemoryUsage();

    // Perform intensive memory operations that may trigger GC
    final int iterations = 1000;
    final byte[] buffer = new byte[4096];

    for (int i = 0; i < iterations; i++) {
      // Write data
      final int offset = i % (Math.toIntExact(wasmMemory.getSize()) - buffer.length);
      for (int j = 0; j < buffer.length; j++) {
        buffer[j] = (byte) ((i + j) % 256);
      }
      writeToMemory(wasmMemory, offset, buffer);

      // Read data back
      final byte[] readData = readFromMemory(wasmMemory, offset, buffer.length);
      
      // Create some garbage to stress GC
      if (i % 50 == 0) {
        final byte[] garbage = new byte[8192];
        blackhole.consume(garbage.length);
      }

      blackhole.consume(readData.length);
    }

    // Capture GC state after operations
    final GcStatistics gcAfter = captureGcStatistics();
    final MemoryUsage heapAfter = memoryBean.getHeapMemoryUsage();

    // Calculate GC impact metrics
    final long gcCollections = gcAfter.totalCollections - gcBefore.totalCollections;
    final long gcTime = gcAfter.totalGcTime - gcBefore.totalGcTime;
    final long heapUsedChange = heapAfter.getUsed() - heapBefore.getUsed();

    blackhole.consume(gcCollections);
    blackhole.consume(gcTime);
    blackhole.consume(heapUsedChange);
    blackhole.consume(iterations);
  }

  /**
   * Benchmarks memory allocation patterns and GC pressure.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkAllocationPatternsWithGc(final Blackhole blackhole) {
    final GcStatistics gcBefore = captureGcStatistics();

    switch (operationPattern) {
      case "SEQUENTIAL":
        benchmarkSequentialAllocations(blackhole);
        break;
      case "RANDOM":
        benchmarkRandomAllocations(blackhole);
        break;
      case "BULK":
        benchmarkBulkAllocations(blackhole);
        break;
      default:
        throw new IllegalArgumentException("Unknown pattern: " + operationPattern);
    }

    final GcStatistics gcAfter = captureGcStatistics();
    final long gcImpact = gcAfter.totalCollections - gcBefore.totalCollections;

    blackhole.consume(gcImpact);
    blackhole.consume(operationPattern);
  }

  /**
   * Benchmarks memory operations under different GC strategies.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkGcResistantOperations(final Blackhole blackhole) {
    // Strategy 1: Reuse buffers to minimize allocations
    final byte[] reusableBuffer = new byte[1024];
    for (int i = 0; i < 100; i++) {
      // Modify buffer in place
      for (int j = 0; j < reusableBuffer.length; j++) {
        reusableBuffer[j] = (byte) ((i + j) % 256);
      }
      
      final int offset = i * 512;
      if (offset + reusableBuffer.length <= wasmMemory.getSize()) {
        writeToMemory(wasmMemory, offset, reusableBuffer);
        
        // Read back using same buffer space
        final byte[] readData = readFromMemory(wasmMemory, offset, Math.min(512, reusableBuffer.length));
        blackhole.consume(readData.length);
      }
    }

    // Strategy 2: Batch operations to reduce call overhead
    final int batchSize = 10;
    final byte[][] batches = new byte[batchSize][];
    for (int i = 0; i < batchSize; i++) {
      batches[i] = new byte[512];
      for (int j = 0; j < batches[i].length; j++) {
        batches[i][j] = (byte) ((i * 100 + j) % 256);
      }
    }

    // Write all batches
    for (int i = 0; i < batchSize; i++) {
      final int offset = i * 512;
      if (offset + batches[i].length <= wasmMemory.getSize()) {
        writeToMemory(wasmMemory, offset, batches[i]);
      }
    }

    // Read all batches
    for (int i = 0; i < batchSize; i++) {
      final int offset = i * 512;
      if (offset + 512 <= wasmMemory.getSize()) {
        final byte[] readData = readFromMemory(wasmMemory, offset, 512);
        blackhole.consume(readData.length);
      }
    }

    blackhole.consume(batchSize);
    blackhole.consume(reusableBuffer.length);
  }

  /** Helper method for sequential allocation pattern. */
  private void benchmarkSequentialAllocations(final Blackhole blackhole) {
    for (int i = 0; i < 200; i++) {
      final byte[] data = new byte[256];
      for (int j = 0; j < data.length; j++) {
        data[j] = (byte) (i + j);
      }
      
      final int offset = i * 128;
      if (offset + data.length <= wasmMemory.getSize()) {
        writeToMemory(wasmMemory, offset, data);
        final byte[] readBack = readFromMemory(wasmMemory, offset, data.length);
        blackhole.consume(readBack.length);
      }
    }
  }

  /** Helper method for random allocation pattern. */
  private void benchmarkRandomAllocations(final Blackhole blackhole) {
    for (int i = 0; i < 150; i++) {
      // Random size allocation
      final int size = 128 + (int) (Math.random() * 512);
      final byte[] data = new byte[size];
      
      for (int j = 0; j < data.length; j++) {
        data[j] = (byte) (Math.random() * 256);
      }
      
      final int offset = randomOffsets[i % randomOffsets.length];
      if (offset + data.length <= wasmMemory.getSize()) {
        writeToMemory(wasmMemory, offset, data);
        final byte[] readBack = readFromMemory(wasmMemory, offset, Math.min(size, 256));
        blackhole.consume(readBack.length);
      }
    }
  }

  /** Helper method for bulk allocation pattern. */
  private void benchmarkBulkAllocations(final Blackhole blackhole) {
    // Large allocations
    final int bulkCount = 20;
    final int bulkSize = Math.min(8192, Math.toIntExact(wasmMemory.getSize()) / bulkCount);
    
    for (int i = 0; i < bulkCount; i++) {
      final byte[] bulkData = new byte[bulkSize];
      for (int j = 0; j < bulkData.length; j++) {
        bulkData[j] = (byte) ((i * 10 + j) % 256);
      }
      
      final int offset = i * bulkSize;
      if (offset + bulkSize <= wasmMemory.getSize()) {
        writeToMemory(wasmMemory, offset, bulkData);
        
        // Read back a portion to verify
        final byte[] sample = readFromMemory(wasmMemory, offset, Math.min(1024, bulkSize));
        blackhole.consume(sample.length);
      }
    }
  }

  /** Helper method to write data to WASM memory using the correct API. */
  private static void writeToMemory(final WasmMemory memory, final int offset, final byte[] data) {
    memory.writeBytes(offset, data, 0, data.length);
  }
  
  /** Helper method to read data from WASM memory using the correct API. */
  private static byte[] readFromMemory(final WasmMemory memory, final int offset, final int length) {
    final byte[] result = new byte[length];
    memory.readBytes(offset, result, 0, length);
    return result;
  }

  /** Captures current garbage collection statistics. */
  private GcStatistics captureGcStatistics() {
    long totalCollections = 0;
    long totalGcTime = 0;
    
    for (final GarbageCollectorMXBean gcBean : gcBeans) {
      totalCollections += gcBean.getCollectionCount();
      totalGcTime += gcBean.getCollectionTime();
    }
    
    return new GcStatistics(totalCollections, totalGcTime);
  }

  /** Helper class to hold GC statistics. */
  private static final class GcStatistics {
    final long totalCollections;
    final long totalGcTime;
    
    GcStatistics(final long totalCollections, final long totalGcTime) {
      this.totalCollections = totalCollections;
      this.totalGcTime = totalGcTime;
    }
  }
}
