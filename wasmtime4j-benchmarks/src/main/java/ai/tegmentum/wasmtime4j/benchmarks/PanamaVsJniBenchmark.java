/*
 * Copyright 2025 Tegmentum AI
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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.nio.ByteBuffer;
import java.util.Random;
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
 * Comprehensive performance benchmark comparing Panama FFI vs JNI implementations.
 *
 * <p>This benchmark suite provides detailed performance analysis across various WebAssembly
 * operations to demonstrate the performance advantages of Panama FFI over traditional JNI bindings.
 * The benchmarks cover function calls, memory operations, module compilation, and instance creation
 * scenarios.
 *
 * <p>Key benchmark categories:
 *
 * <ul>
 *   <li>Function call overhead and throughput
 *   <li>Memory access patterns and bulk operations
 *   <li>Module compilation and caching performance
 *   <li>Instance creation and resource management
 *   <li>Concurrent access patterns and scaling
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 3,
    jvmArgs = {"-Xms4g", "-Xmx4g", "--enable-native-access=ALL-UNNAMED"})
public class PanamaVsJniBenchmark extends BenchmarkBase {

  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  @Param({"1", "10", "100", "1000"})
  private int operationCount;

  private WasmRuntime runtime;
  private Engine engine;
  private Module simpleModule;
  private Module memoryModule;
  private Instance simpleInstance;
  private Instance memoryInstance;
  private WasmFunction addFunction;
  private WasmFunction memoryFunction;
  private WasmMemory wasmMemory;

  private Random random;
  private int[] testData;
  private ByteBuffer testBuffer;



  /**
   * Sets up the benchmark trial by initializing WebAssembly runtime, modules, and test data.
   *
   * @throws Exception if setup fails
   */
  @Setup(Level.Trial)
  public void setupTrial() throws Exception {
    System.out.println("Setting up benchmark trial with runtime: " + runtimeTypeName);

    // Convert string to RuntimeType
    final RuntimeType runtimeType = RuntimeType.valueOf(runtimeTypeName);

    // Check if the requested runtime is available
    if (!WasmRuntimeFactory.isRuntimeAvailable(runtimeType)) {
      throw new RuntimeException("Runtime not available: " + runtimeType);
    }

    runtime = WasmRuntimeFactory.create(runtimeType);
    engine = runtime.createEngine();

    // Compile test modules from WAT format
    simpleModule = engine.compileWat(SIMPLE_WAT_MODULE);
    memoryModule = engine.compileWat(COMPLEX_WAT_MODULE);

    // Create store and instances
    final Store store = engine.createStore();
    simpleInstance = simpleModule.instantiate(store);
    memoryInstance = memoryModule.instantiate(store);

    // Get exported functions and memory
    addFunction =
        simpleInstance
            .getFunction("add")
            .orElseThrow(() -> new RuntimeException("Add function not found"));

    wasmMemory =
        memoryInstance
            .getMemory("memory")
            .orElseThrow(() -> new RuntimeException("Memory export not found"));

    // Initialize test data
    random = new Random(42); // Fixed seed for reproducible results
    testData = new int[operationCount];
    for (int i = 0; i < operationCount; i++) {
      testData[i] = random.nextInt(10000);
    }

    testBuffer = ByteBuffer.allocateDirect(operationCount * 4);
    for (int value : testData) {
      testBuffer.putInt(value);
    }
    testBuffer.flip();

    System.out.println(
        "Trial setup completed for " + runtimeType + " with " + operationCount + " operations");
  }

  /**
   * Tears down the benchmark trial by cleaning up WebAssembly resources.
   *
   * @throws Exception if cleanup fails
   */
  @TearDown(Level.Trial)
  public void teardownTrial() throws Exception {
    if (simpleInstance != null) {
      simpleInstance.close();
    }
    if (memoryInstance != null) {
      memoryInstance.close();
    }
    if (simpleModule != null) {
      simpleModule.close();
    }
    if (memoryModule != null) {
      memoryModule.close();
    }
    if (engine != null) {
      engine.close();
    }
    if (runtime != null) {
      runtime.close();
    }

    System.out.println("Trial teardown completed for " + runtimeTypeName);
  }

  /**
   * Benchmark function call performance. Measures the overhead of calling WebAssembly functions
   * through FFI.
   */
  @Benchmark
  public void functionCalls(final Blackhole bh) throws Exception {
    for (int i = 0; i < operationCount; i++) {
      final int a = testData[i];
      final int b = testData[(i + 1) % testData.length];
      final WasmValue[] results = addFunction.call(WasmValue.i32(a), WasmValue.i32(b));
      bh.consume(results[0].asInt());
    }
  }

  /** Benchmark bulk function calls. Tests performance of batched function invocations. */
  @Benchmark
  public void bulkFunctionCalls(final Blackhole bh) throws Exception {
    final WasmValue[] params = new WasmValue[2];
    final int batchSize = Math.min(100, operationCount);

    for (int batch = 0; batch < operationCount; batch += batchSize) {
      for (int i = 0; i < batchSize && (batch + i) < operationCount; i++) {
        final int index = batch + i;
        params[0] = WasmValue.i32(testData[index]);
        params[1] = WasmValue.i32(testData[(index + 1) % testData.length]);

        final WasmValue[] results = addFunction.call(params);
        bh.consume(results[0].asInt());
      }
    }
  }

  /**
   * Benchmark memory read operations. Tests performance of reading data from WebAssembly linear
   * memory.
   */
  @Benchmark
  public void memoryReads(final Blackhole bh) throws Exception {
    final int memorySize = Math.toIntExact(wasmMemory.getSize());
    final int maxOffset = Math.max(1, memorySize - 4);

    for (int i = 0; i < operationCount; i++) {
      final int offset = (testData[i] % maxOffset) & ~3; // Align to 4 bytes
      final int value = readIntFromMemory(wasmMemory, offset);
      bh.consume(value);
    }
  }

  /**
   * Benchmark memory write operations. Tests performance of writing data to WebAssembly linear
   * memory.
   */
  @Benchmark
  public void memoryWrites(final Blackhole bh) throws Exception {
    final int memorySize = Math.toIntExact(wasmMemory.getSize());
    final int maxOffset = Math.max(1, memorySize - 4);

    for (int i = 0; i < operationCount; i++) {
      final int offset = (testData[i] % maxOffset) & ~3; // Align to 4 bytes
      writeIntToMemory(wasmMemory, offset, testData[i]);
      bh.consume(offset);
    }
  }

  /** Benchmark bulk memory operations. Tests performance of large memory transfers. */
  @Benchmark
  public void bulkMemoryOperations(final Blackhole bh) throws Exception {
    final int chunkSize = Math.min(1024, operationCount * 4);
    final ByteBuffer chunk = ByteBuffer.allocate(chunkSize);

    // Fill with test data
    for (int i = 0; i < chunkSize / 4; i++) {
      chunk.putInt(testData[i % testData.length]);
    }
    chunk.flip();

    final int iterations = Math.max(1, operationCount / (chunkSize / 4));

    for (int i = 0; i < iterations; i++) {
      final int offset =
          (i * chunkSize) % Math.max(1, Math.toIntExact(wasmMemory.getSize()) - chunkSize);

      // Write chunk
      chunk.rewind();
      writeBufferToMemory(wasmMemory, offset, chunk);

      // Read it back
      final ByteBuffer readBuffer = ByteBuffer.allocate(chunkSize);
      readBufferFromMemory(wasmMemory, offset, readBuffer);

      bh.consume(readBuffer.getInt(0));
    }
  }

  /**
   * Benchmark module compilation performance. Tests the overhead of compiling WebAssembly modules.
   */
  @Benchmark
  public void moduleCompilation(final Blackhole bh) throws Exception {
    for (int i = 0; i < Math.min(operationCount, 100); i++) { // Limit to avoid excessive overhead
      try (Module module = engine.compileWat(SIMPLE_WAT_MODULE)) {
        bh.consume(module);
      }
    }
  }

  /**
   * Benchmark instance creation performance. Tests the overhead of instantiating WebAssembly
   * modules.
   */
  @Benchmark
  public void instanceCreation(final Blackhole bh) throws Exception {
    for (int i = 0; i < Math.min(operationCount, 50); i++) { // Limit to avoid excessive overhead
      try (Store store = engine.createStore();
          Instance instance = simpleModule.instantiate(store)) {
        bh.consume(instance);
      }
    }
  }

  /**
   * Benchmark mixed operations performance. Tests realistic usage patterns combining function calls
   * and memory operations.
   */
  @Benchmark
  public void mixedOperations(final Blackhole bh) throws Exception {
    final int memorySize = Math.toIntExact(wasmMemory.getSize());
    final int maxOffset = Math.max(1, memorySize - 4);

    for (int i = 0; i < operationCount; i++) {
      // Alternate between function calls and memory operations
      if (i % 3 == 0) {
        // Function call
        final int a = testData[i];
        final int b = testData[(i + 1) % testData.length];
        final WasmValue[] results = addFunction.call(WasmValue.i32(a), WasmValue.i32(b));
        bh.consume(results[0].asInt());
      } else if (i % 3 == 1) {
        // Memory write
        final int offset = (testData[i] % maxOffset) & ~3;
        final ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(testData[i]);
        buffer.flip();
        writeBufferToMemory(wasmMemory, offset, buffer);
        bh.consume(offset);
      } else {
        // Memory read
        final int offset = (testData[i] % maxOffset) & ~3;
        final ByteBuffer buffer = ByteBuffer.allocate(4);
        readBufferFromMemory(wasmMemory, offset, buffer);
        bh.consume(buffer.getInt(0));
      }
    }
  }

  /**
   * Benchmark concurrent operations performance. Tests performance under concurrent access
   * patterns.
   */
  @Benchmark
  public void concurrentOperations(final Blackhole bh) throws Exception {
    // Simulate concurrent-like access patterns within single thread
    final int threads = 4;
    final int opsPerThread = operationCount / threads;

    for (int t = 0; t < threads; t++) {
      for (int i = 0; i < opsPerThread; i++) {
        final int index = t * opsPerThread + i;
        if (index >= testData.length) {
          break;
        }

        final int a = testData[index];
        final int b = testData[(index + 1) % testData.length];
        final WasmValue[] results = addFunction.call(WasmValue.i32(a), WasmValue.i32(b));
        bh.consume(results[0].asInt());
      }
    }
  }

  /**
   * Benchmark resource allocation and cleanup performance. Tests the overhead of creating and
   * destroying WebAssembly resources.
   */
  @Benchmark
  public void resourceManagement(final Blackhole bh) throws Exception {
    final int iterations = Math.min(operationCount, 20); // Limit to avoid excessive overhead

    for (int i = 0; i < iterations; i++) {
      try (Engine testEngine = runtime.createEngine();
          Store testStore = testEngine.createStore();
          Module testModule = testEngine.compileWat(SIMPLE_WAT_MODULE);
          Instance testInstance = testModule.instantiate(testStore)) {

        final WasmFunction func =
            testInstance
                .getFunction("add")
                .orElseThrow(() -> new RuntimeException("Function not found"));

        final WasmValue[] results = func.call(WasmValue.i32(i), WasmValue.i32(i + 1));
        bh.consume(results[0].asInt());
      }
    }
  }

  /**
   * Baseline benchmark measuring Java operation overhead. Provides comparison baseline for pure
   * Java operations.
   */
  @Benchmark
  public void javaBaseline(final Blackhole bh) {
    for (int i = 0; i < operationCount; i++) {
      final int a = testData[i];
      final int b = testData[(i + 1) % testData.length];
      final int result = a + b;
      bh.consume(result);
    }
  }

  /**
   * Benchmark measuring FFI call overhead only. Tests the minimum overhead of crossing the FFI
   * boundary.
   */
  @Benchmark
  public void ffiOverhead(final Blackhole bh) throws Exception {
    // Use the simplest possible WebAssembly function call
    final WasmValue zero = WasmValue.i32(0);
    final WasmValue one = WasmValue.i32(1);

    for (int i = 0; i < operationCount; i++) {
      final WasmValue[] results = addFunction.call(zero, one);
      bh.consume(results[0].asInt());
    }
  }

  /** Helper to read 4 bytes as int from memory. */
  private static int readIntFromMemory(final WasmMemory memory, final int offset) {
    final byte[] bytes = new byte[4];
    memory.readBytes(offset, bytes, 0, 4);
    return java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
  }

  /** Helper to write int as 4 bytes to memory. */
  private static void writeIntToMemory(final WasmMemory memory, final int offset, final int value) {
    final byte[] bytes =
        java.nio.ByteBuffer.allocate(4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .putInt(value)
            .array();
    memory.writeBytes(offset, bytes, 0, 4);
  }

  /** Helper to write ByteBuffer to memory. */
  private static void writeBufferToMemory(
      final WasmMemory memory, final int offset, final java.nio.ByteBuffer buffer) {
    final byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    memory.writeBytes(offset, bytes, 0, bytes.length);
  }

  /** Helper to read from memory into ByteBuffer. */
  private static void readBufferFromMemory(
      final WasmMemory memory, final int offset, final java.nio.ByteBuffer buffer) {
    final byte[] bytes = new byte[buffer.remaining()];
    memory.readBytes(offset, bytes, 0, bytes.length);
    buffer.put(bytes);
  }
}
