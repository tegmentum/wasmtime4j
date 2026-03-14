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
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks measuring per-operation overhead of single-value memory accesses.
 *
 * <p>This benchmark isolates the overhead of individual readByte/writeByte/readInt/writeInt
 * operations to measure the impact of removing redundant validateOffset JNI calls and streamlining
 * the guard pattern.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class MemoryAccessOverheadBenchmark extends BenchmarkBase {

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private Module module;
  private Instance instance;
  private WasmMemory memory;

  @Setup(Level.Trial)
  public void setup() throws WasmException {
    runtime = createRuntime(RuntimeType.JNI);
    engine = createEngine(runtime);
    store = createStore(engine);
    module = compileWatModule(engine, COMPLEX_WAT_MODULE);
    instance = instantiateModule(store, module);
    memory = instance.getMemory("memory").orElseThrow(() -> new WasmException("Memory not found"));

    // Pre-write some data so reads return non-zero values
    byte[] initData = new byte[1024];
    for (int i = 0; i < initData.length; i++) {
      initData[i] = (byte) (i % 256);
    }
    memory.writeBytes(0, initData, 0, initData.length);
  }

  @TearDown(Level.Trial)
  public void teardown() {
    closeQuietly(instance);
    closeQuietly(module);
    closeQuietly(store);
    closeQuietly(engine);
    closeQuietly(runtime);
  }

  @Benchmark
  public byte readSingleByte(Blackhole bh) {
    return memory.readByte(42);
  }

  @Benchmark
  public void writeSingleByte(Blackhole bh) {
    memory.writeByte(42, (byte) 0x7F);
  }

  @Benchmark
  public void readByteBurst(Blackhole bh) {
    // 10 sequential single-byte reads to amplify per-call overhead
    bh.consume(memory.readByte(0));
    bh.consume(memory.readByte(1));
    bh.consume(memory.readByte(2));
    bh.consume(memory.readByte(3));
    bh.consume(memory.readByte(4));
    bh.consume(memory.readByte(5));
    bh.consume(memory.readByte(6));
    bh.consume(memory.readByte(7));
    bh.consume(memory.readByte(8));
    bh.consume(memory.readByte(9));
  }

  @Benchmark
  public void writeByteBurst(Blackhole bh) {
    // 10 sequential single-byte writes to amplify per-call overhead
    memory.writeByte(0, (byte) 0);
    memory.writeByte(1, (byte) 1);
    memory.writeByte(2, (byte) 2);
    memory.writeByte(3, (byte) 3);
    memory.writeByte(4, (byte) 4);
    memory.writeByte(5, (byte) 5);
    memory.writeByte(6, (byte) 6);
    memory.writeByte(7, (byte) 7);
    memory.writeByte(8, (byte) 8);
    memory.writeByte(9, (byte) 9);
  }

  @Benchmark
  public int readIntValue(Blackhole bh) {
    return memory.readInt32(0);
  }

  @Benchmark
  public void writeIntValue(Blackhole bh) {
    memory.writeInt32(0, 42);
  }

  @Benchmark
  public void readSmallBytes(Blackhole bh) {
    // Read 16 bytes via readBytes to measure bulk small-read overhead
    byte[] buf = new byte[16];
    memory.readBytes(0, buf, 0, 16);
    bh.consume(buf);
  }
}
