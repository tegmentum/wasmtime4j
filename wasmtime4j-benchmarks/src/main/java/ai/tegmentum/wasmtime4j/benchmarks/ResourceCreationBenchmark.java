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

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks for JNI resource creation overhead patterns.
 *
 * <p>Measures two optimization targets:
 *
 * <ul>
 *   <li>Null reference singleton caching: funcref(null) / externref(null) should return cached
 *       singletons instead of allocating new objects
 *   <li>Constructor logging overhead: unguarded LOGGER.fine() with string concatenation and
 *       Long.toHexString() in JNI resource constructors
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1g", "-Xmx1g"})
public class ResourceCreationBenchmark {

  private static final Logger LOGGER = Logger.getLogger(ResourceCreationBenchmark.class.getName());

  private long nativeHandle;

  @Setup
  public void setup() {
    LOGGER.setLevel(Level.WARNING);
    nativeHandle = 0x7F00DEADBEEFL;
  }

  // ==========================================
  // Null reference allocation vs singleton
  // ==========================================

  @Benchmark
  public void funcrefNullCreate(Blackhole bh) {
    // Current: always allocates new WasmValue
    bh.consume(WasmValue.funcref(null));
  }

  @Benchmark
  public void externrefNullCreate(Blackhole bh) {
    // Current: always allocates new WasmValue
    bh.consume(WasmValue.externref(null));
  }

  @Benchmark
  public void nullFuncrefSingleton(Blackhole bh) {
    // Cached singleton (already exists)
    bh.consume(WasmValue.nullFuncref());
  }

  @Benchmark
  public void nullExternrefSingleton(Blackhole bh) {
    // Cached singleton (already exists)
    bh.consume(WasmValue.nullExternref());
  }

  @Benchmark
  public void funcrefNonNull(Blackhole bh) {
    // Non-null funcref — should always allocate
    bh.consume(WasmValue.funcref("some_func"));
  }

  // ==========================================
  // Simulated unmarshal path for ref types
  // ==========================================

  @Benchmark
  public void simulatedUnmarshalRefTypes(Blackhole bh) {
    // Simulates unmarshalValue for FUNCREF + EXTERNREF
    // Before: always allocates
    bh.consume(WasmValue.funcref(null));
    bh.consume(WasmValue.externref(null));
    bh.consume(WasmValue.funcref(null));
    bh.consume(WasmValue.externref(null));
  }

  // ==========================================
  // Constructor logging patterns
  // ==========================================

  @Benchmark
  public void constructorLoggingUnguarded(Blackhole bh) {
    // Simulates JniFunction/JniStore/JniInstance constructors:
    // LOGGER.fine("Created JNI ... with handle: 0x" + Long.toHexString(nativeHandle))
    LOGGER.fine("Created JNI function with handle: 0x" + Long.toHexString(nativeHandle));
  }

  @Benchmark
  public void constructorLoggingGuarded(Blackhole bh) {
    // Optimized: check level before concatenation
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Created JNI function with handle: 0x" + Long.toHexString(nativeHandle));
    }
  }

  @Benchmark
  public void multipleConstructorLoggingUnguarded(Blackhole bh) {
    // Simulates creating engine + store + module + instance + function
    LOGGER.fine("Created JNI runtime with handle: 0x" + Long.toHexString(nativeHandle));
    LOGGER.fine("Created engine with handle: 0x" + Long.toHexString(nativeHandle));
    LOGGER.fine("Created JNI store with handle: 0x" + Long.toHexString(nativeHandle));
    LOGGER.fine("Created JNI instance with handle: " + nativeHandle);
    LOGGER.fine(
        "Created JNI function 'add' with handle: 0x"
            + Long.toHexString(nativeHandle)
            + ", module: 0x"
            + Long.toHexString(nativeHandle));
  }

  @Benchmark
  public void multipleConstructorLoggingGuarded(Blackhole bh) {
    // Same sequence but with guards
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Created JNI runtime with handle: 0x" + Long.toHexString(nativeHandle));
    }
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Created engine with handle: 0x" + Long.toHexString(nativeHandle));
    }
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Created JNI store with handle: 0x" + Long.toHexString(nativeHandle));
    }
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Created JNI instance with handle: " + nativeHandle);
    }
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(
          "Created JNI function 'add' with handle: 0x"
              + Long.toHexString(nativeHandle)
              + ", module: 0x"
              + Long.toHexString(nativeHandle));
    }
  }
}
