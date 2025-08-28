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
  private RuntimeType runtimeType;

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

  // Simple WebAssembly module that adds two i32 values
  private static final byte[] SIMPLE_ADD_WASM = {
    0x00,
    0x61,
    0x73,
    0x6d, // WASM magic
    0x01,
    0x00,
    0x00,
    0x00, // WASM version
    // Type section
    0x01,
    0x07,
    0x01,
    0x60,
    0x02,
    0x7f,
    0x7f,
    0x01,
    0x7f,
    // Function section
    0x03,
    0x02,
    0x01,
    0x00,
    // Export section
    0x07,
    0x07,
    0x01,
    0x03,
    0x61,
    0x64,
    0x64,
    0x00,
    0x00,
    // Code section
    0x0a,
    0x09,
    0x01,
    0x07,
    0x00,
    0x20,
    0x00,
    0x20,
    0x01,
    0x6a,
    0x0b
  };

  // WebAssembly module with memory operations
  private static final byte[] MEMORY_WASM = {
    0x00,
    0x61,
    0x73,
    0x6d, // WASM magic
    0x01,
    0x00,
    0x00,
    0x00, // WASM version
    // Type section
    0x01,
    0x0a,
    0x02,
    0x60,
    0x02,
    0x7f,
    0x7f,
    0x00, // (i32, i32) -> ()
    0x60,
    0x01,
    0x7f,
    0x01,
    0x7f, // (i32) -> i32
    // Function section
    0x03,
    0x03,
    0x02,
    0x00,
    0x01,
    // Memory section
    0x05,
    0x03,
    0x01,
    0x00,
    0x01,
    // Export section
    0x07,
    0x11,
    0x02,
    0x06,
    0x6d,
    0x65,
    0x6d,
    0x6f,
    0x72,
    0x79,
    0x02,
    0x00,
    0x05,
    0x77,
    0x72,
    0x69,
    0x74,
    0x65,
    0x00,
    0x00,
    // Code section
    0x0a,
    0x0e,
    0x02,
    0x07,
    0x00,
    0x20,
    0x00,
    0x20,
    0x01,
    0x36,
    0x02,
    0x00,
    0x0b,
    0x05,
    0x00,
    0x20,
    0x00,
    0x28,
    0x02,
    0x00,
    0x0b
  };

  @Setup(Level.Trial)
  public void setupTrial() throws Exception {
    System.out.println("Setting up benchmark trial with runtime: " + runtimeType);

    // Check if the requested runtime is available
    if (!WasmRuntimeFactory.isRuntimeAvailable(runtimeType)) {
      throw new RuntimeException("Runtime not available: " + runtimeType);
    }

    runtime = WasmRuntimeFactory.create(runtimeType);
    engine = runtime.createEngine();

    // Compile test modules
    simpleModule = engine.compileModule(SIMPLE_ADD_WASM);
    memoryModule = engine.compileModule(MEMORY_WASM);

    // Create instances
    simpleInstance = runtime.instantiate(simpleModule);
    memoryInstance = runtime.instantiate(memoryModule);

    // Get exported functions and memory
    addFunction =
        simpleInstance
            .getExportedFunction("add")
            .orElseThrow(() -> new RuntimeException("Add function not found"));

    wasmMemory =
        memoryInstance
            .getExportedMemory("memory")
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

  @TearDown(Level.Trial)
  public void teardownTrial() throws Exception {
    if (simpleInstance != null) simpleInstance.close();
    if (memoryInstance != null) memoryInstance.close();
    if (simpleModule != null) simpleModule.close();
    if (memoryModule != null) memoryModule.close();
    if (engine != null) engine.close();
    if (runtime != null) runtime.close();

    System.out.println("Trial teardown completed for " + runtimeType);
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
      bh.consume(results[0].asI32());
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
        bh.consume(results[0].asI32());
      }
    }
  }

  /**
   * Benchmark memory read operations. Tests performance of reading data from WebAssembly linear
   * memory.
   */
  @Benchmark
  public void memoryReads(final Blackhole bh) throws Exception {
    final int memorySize = (int) wasmMemory.size();
    final int maxOffset = Math.max(1, memorySize - 4);

    for (int i = 0; i < operationCount; i++) {
      final int offset = (testData[i] % maxOffset) & ~3; // Align to 4 bytes
      final ByteBuffer buffer = ByteBuffer.allocate(4);
      wasmMemory.read(offset, buffer);
      bh.consume(buffer.getInt(0));
    }
  }

  /**
   * Benchmark memory write operations. Tests performance of writing data to WebAssembly linear
   * memory.
   */
  @Benchmark
  public void memoryWrites(final Blackhole bh) throws Exception {
    final int memorySize = (int) wasmMemory.size();
    final int maxOffset = Math.max(1, memorySize - 4);

    for (int i = 0; i < operationCount; i++) {
      final int offset = (testData[i] % maxOffset) & ~3; // Align to 4 bytes
      final ByteBuffer buffer = ByteBuffer.allocate(4);
      buffer.putInt(testData[i]);
      buffer.flip();
      wasmMemory.write(offset, buffer);
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
      final int offset = (i * chunkSize) % Math.max(1, (int) wasmMemory.size() - chunkSize);

      // Write chunk
      chunk.rewind();
      wasmMemory.write(offset, chunk);

      // Read it back
      final ByteBuffer readBuffer = ByteBuffer.allocate(chunkSize);
      wasmMemory.read(offset, readBuffer);

      bh.consume(readBuffer.getInt(0));
    }
  }

  /**
   * Benchmark module compilation performance. Tests the overhead of compiling WebAssembly modules.
   */
  @Benchmark
  public void moduleCompilation(final Blackhole bh) throws Exception {
    for (int i = 0; i < Math.min(operationCount, 100); i++) { // Limit to avoid excessive overhead
      try (Module module = engine.compileModule(SIMPLE_ADD_WASM)) {
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
      try (Instance instance = runtime.instantiate(simpleModule)) {
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
    final int memorySize = (int) wasmMemory.size();
    final int maxOffset = Math.max(1, memorySize - 4);

    for (int i = 0; i < operationCount; i++) {
      // Alternate between function calls and memory operations
      if (i % 3 == 0) {
        // Function call
        final int a = testData[i];
        final int b = testData[(i + 1) % testData.length];
        final WasmValue[] results = addFunction.call(WasmValue.i32(a), WasmValue.i32(b));
        bh.consume(results[0].asI32());
      } else if (i % 3 == 1) {
        // Memory write
        final int offset = (testData[i] % maxOffset) & ~3;
        final ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(testData[i]);
        buffer.flip();
        wasmMemory.write(offset, buffer);
        bh.consume(offset);
      } else {
        // Memory read
        final int offset = (testData[i] % maxOffset) & ~3;
        final ByteBuffer buffer = ByteBuffer.allocate(4);
        wasmMemory.read(offset, buffer);
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
        if (index >= testData.length) break;

        final int a = testData[index];
        final int b = testData[(index + 1) % testData.length];
        final WasmValue[] results = addFunction.call(WasmValue.i32(a), WasmValue.i32(b));
        bh.consume(results[0].asI32());
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
          Module testModule = testEngine.compileModule(SIMPLE_ADD_WASM);
          Instance testInstance = runtime.instantiate(testModule)) {

        final WasmFunction func =
            testInstance
                .getExportedFunction("add")
                .orElseThrow(() -> new RuntimeException("Function not found"));

        final WasmValue[] results = func.call(WasmValue.i32(i), WasmValue.i32(i + 1));
        bh.consume(results[0].asI32());
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
      bh.consume(results[0].asI32());
    }
  }
}
