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

import java.util.concurrent.ConcurrentHashMap;
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
 * Benchmarks simulating the host function callback dispatch overhead.
 *
 * <p>Measures the cost of the Java-side dispatch in invokeHostFunctionCallback:
 * ConcurrentHashMap lookup (with Long autoboxing), logging with string concatenation,
 * and the overall dispatch overhead excluding the actual host function execution.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1g", "-Xmx1g"})
public class HostCallbackBenchmark {

  private static final Logger LOGGER = Logger.getLogger(HostCallbackBenchmark.class.getName());

  // Simulate HOST_FUNCTION_CALLBACKS with Long keys (current implementation)
  private ConcurrentHashMap<Long, Object> mapWithBoxedKeys;

  // Simulate with pre-cached Long keys
  private Long[] preBoxedKeys;

  private long[] rawCallbackIds;
  private String moduleName;
  private String funcName;

  @Setup
  public void setup() {
    // Disable fine logging (production default)
    LOGGER.setLevel(Level.WARNING);

    mapWithBoxedKeys = new ConcurrentHashMap<>();
    rawCallbackIds = new long[100];
    preBoxedKeys = new Long[100];

    moduleName = "wasi_snapshot_preview1";
    funcName = "fd_write";

    for (int i = 0; i < 100; i++) {
      long id = i + 1;
      rawCallbackIds[i] = id;
      preBoxedKeys[i] = Long.valueOf(id);
      mapWithBoxedKeys.put(id, new Object());
    }
  }

  // ==========================================
  // Map lookup with autoboxing (current code)
  // ==========================================

  @Benchmark
  public void mapLookupAutoboxing(Blackhole bh) {
    // Simulates: HOST_FUNCTION_CALLBACKS.get(callbackId) where callbackId is long
    bh.consume(mapWithBoxedKeys.get(rawCallbackIds[42]));
  }

  @Benchmark
  public void mapLookupPreBoxed(Blackhole bh) {
    // Simulates: lookup with pre-boxed Long key
    bh.consume(mapWithBoxedKeys.get(preBoxedKeys[42]));
  }

  // ==========================================
  // Logging overhead (current vs guarded)
  // ==========================================

  @Benchmark
  public void loggingUnguardedConcat(Blackhole bh) {
    // Current code: string concatenation happens even when FINE is disabled
    LOGGER.fine(
        "invokeHostFunctionCallback - Called with callbackId="
            + rawCallbackIds[42]
            + ", params.length="
            + 2);
    LOGGER.fine("Executing host function: " + moduleName + "::" + funcName);
    LOGGER.fine(
        "invokeHostFunctionCallback - Completed successfully with " + 1 + " results");
  }

  @Benchmark
  public void loggingGuarded(Blackhole bh) {
    // Optimized: check log level before concatenation
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(
          "invokeHostFunctionCallback - Called with callbackId="
              + rawCallbackIds[42]
              + ", params.length="
              + 2);
    }
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Executing host function: " + moduleName + "::" + funcName);
    }
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(
          "invokeHostFunctionCallback - Completed successfully with " + 1 + " results");
    }
  }

  // ==========================================
  // Full simulated callback dispatch
  // ==========================================

  @Benchmark
  public void fullDispatchCurrent(Blackhole bh) {
    long callbackId = rawCallbackIds[42];

    // 1. Unguarded logging with concatenation
    LOGGER.fine(
        "invokeHostFunctionCallback - Called with callbackId="
            + callbackId
            + ", params.length="
            + 2);

    // 2. Map lookup with autoboxing
    Object wrapper = mapWithBoxedKeys.get(callbackId);

    // 3. More unguarded logging
    LOGGER.fine("Executing host function: " + moduleName + "::" + funcName);

    // 4. Simulate callback execution (just consume wrapper)
    bh.consume(wrapper);

    // 5. Result logging
    LOGGER.fine(
        "invokeHostFunctionCallback - Completed successfully with " + 1 + " results");
  }

  @Benchmark
  public void fullDispatchOptimized(Blackhole bh) {
    long callbackId = rawCallbackIds[42];

    // 1. Guarded logging
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(
          "invokeHostFunctionCallback - Called with callbackId="
              + callbackId
              + ", params.length="
              + 2);
    }

    // 2. Map lookup (autoboxing still happens, but logging overhead removed)
    Object wrapper = mapWithBoxedKeys.get(callbackId);

    // 3. Guarded logging
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Executing host function: " + moduleName + "::" + funcName);
    }

    // 4. Simulate callback execution
    bh.consume(wrapper);

    // 5. Guarded result logging
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine(
          "invokeHostFunctionCallback - Completed successfully with " + 1 + " results");
    }
  }
}
