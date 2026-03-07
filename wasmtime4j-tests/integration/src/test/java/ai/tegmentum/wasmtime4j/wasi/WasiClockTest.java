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
package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for WASI clock operations.
 *
 * <p>These tests verify:
 *
 * <ul>
 *   <li>Realtime clock access
 *   <li>Monotonic clock access
 *   <li>Clock resolution queries
 *   <li>Time progression
 * </ul>
 */
@DisplayName("WASI Clock Integration Tests")
@Tag("integration")
class WasiClockTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(WasiClockTest.class.getName());

  /** WASM module that uses WASI clock functions. */
  private static final String CLOCK_MODULE_WAT =
      "(module\n"
          + "  ;; Import WASI clock functions\n"
          + "  (import \"wasi_snapshot_preview1\" \"clock_time_get\"\n"
          + "    (func $clock_time_get (param i32 i64 i32) (result i32)))\n"
          + "  (import \"wasi_snapshot_preview1\" \"clock_res_get\"\n"
          + "    (func $clock_res_get (param i32 i32) (result i32)))\n"
          + "\n"
          + "  ;; Memory for storing results\n"
          + "  (memory (export \"memory\") 1)\n"
          + "\n"
          + "  ;; Get realtime clock (clock_id = 0)\n"
          + "  (func (export \"get_realtime\") (result i64)\n"
          + "    ;; Call clock_time_get with realtime clock (0), precision 1, store at offset 0\n"
          + "    i32.const 0     ;; clock_id = CLOCK_REALTIME\n"
          + "    i64.const 1     ;; precision\n"
          + "    i32.const 0     ;; result pointer\n"
          + "    call $clock_time_get\n"
          + "    drop            ;; ignore error code\n"
          + "    ;; Load result from memory\n"
          + "    i32.const 0\n"
          + "    i64.load)\n"
          + "\n"
          + "  ;; Get monotonic clock (clock_id = 1)\n"
          + "  (func (export \"get_monotonic\") (result i64)\n"
          + "    i32.const 1     ;; clock_id = CLOCK_MONOTONIC\n"
          + "    i64.const 1     ;; precision\n"
          + "    i32.const 0     ;; result pointer\n"
          + "    call $clock_time_get\n"
          + "    drop\n"
          + "    i32.const 0\n"
          + "    i64.load)\n"
          + "\n"
          + "  ;; Get realtime clock resolution\n"
          + "  (func (export \"get_realtime_resolution\") (result i64)\n"
          + "    i32.const 0     ;; clock_id = CLOCK_REALTIME\n"
          + "    i32.const 0     ;; result pointer\n"
          + "    call $clock_res_get\n"
          + "    drop\n"
          + "    i32.const 0\n"
          + "    i64.load)\n"
          + "\n"
          + "  ;; Get monotonic clock resolution\n"
          + "  (func (export \"get_monotonic_resolution\") (result i64)\n"
          + "    i32.const 1     ;; clock_id = CLOCK_MONOTONIC\n"
          + "    i32.const 0     ;; result pointer\n"
          + "    call $clock_res_get\n"
          + "    drop\n"
          + "    i32.const 0\n"
          + "    i64.load)\n"
          + ")";

  private Engine engine;
  private Store store;
  private WasiContext wasiContext;
  private Module module;
  private Instance instance;
  private Linker<WasiContext> linker;

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up clock test resources");

    closeQuietly(instance, "instance");
    closeQuietly(linker, "linker");
    closeQuietly(module, "module");
    closeQuietly(store, "store");
    closeQuietly(engine, "engine");
    clearRuntimeSelection();
  }

  private void closeQuietly(final AutoCloseable resource, final String name) {
    if (resource != null) {
      try {
        resource.close();
        LOGGER.info(name + " closed");
      } catch (final Exception e) {
        LOGGER.warning("Error closing " + name + ": " + e.getMessage());
      }
    }
  }

  private void setUpClockResources(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    LOGGER.info("Setting up clock test resources");

    engine = Engine.create();
    wasiContext = WasiContext.create().inheritStdio();

    store = engine.createStore();
    module = engine.compileWat(CLOCK_MODULE_WAT);

    // Link WASI and instantiate
    linker = WasiLinkerUtils.createLinker(engine, wasiContext);
    instance = linker.instantiate(store, module);

    LOGGER.info("Clock test resources created");
  }

  @Nested
  @DisplayName("Realtime Clock Tests")
  class RealtimeClockTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return positive realtime clock value")
    void shouldReturnPositiveRealtimeValue(final RuntimeType runtime) throws Exception {
      setUpClockResources(runtime);

      final WasmFunction getRealtime = instance.getFunction("get_realtime").orElseThrow();
      final WasmValue[] result = getRealtime.call();

      final long timeNanos = result[0].asLong();

      LOGGER.info("Realtime clock value: " + timeNanos + " ns");

      // Time should be positive and reasonable (after year 2020)
      // 2020-01-01 in nanoseconds since epoch is approximately 1577836800000000000
      assertTrue(timeNanos > 1577836800000000000L, "Realtime should be after year 2020");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return increasing realtime clock values")
    void shouldReturnIncreasingRealtimeValues(final RuntimeType runtime) throws Exception {
      setUpClockResources(runtime);

      final WasmFunction getRealtime = instance.getFunction("get_realtime").orElseThrow();

      final WasmValue[] result1 = getRealtime.call();
      final long time1 = result1[0].asLong();

      // Small delay
      Thread.sleep(10);

      final WasmValue[] result2 = getRealtime.call();
      final long time2 = result2[0].asLong();

      LOGGER.info("Realtime clock value 1: " + time1 + " ns");
      LOGGER.info("Realtime clock value 2: " + time2 + " ns");
      LOGGER.info("Difference: " + (time2 - time1) + " ns");

      assertTrue(time2 > time1, "Second realtime reading should be greater than first");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return realtime clock resolution")
    void shouldReturnRealtimeClockResolution(final RuntimeType runtime) throws Exception {
      setUpClockResources(runtime);

      final WasmFunction getResolution =
          instance.getFunction("get_realtime_resolution").orElseThrow();
      final WasmValue[] result = getResolution.call();

      final long resolution = result[0].asLong();

      LOGGER.info("Realtime clock resolution: " + resolution + " ns");

      // Resolution should be positive and reasonable (nanoseconds to seconds)
      assertTrue(resolution > 0, "Resolution should be positive");
      assertTrue(resolution <= 1000000000L, "Resolution should be at most 1 second");
    }
  }

  @Nested
  @DisplayName("Monotonic Clock Tests")
  class MonotonicClockTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return positive monotonic clock value")
    void shouldReturnPositiveMonotonicValue(final RuntimeType runtime) throws Exception {
      setUpClockResources(runtime);

      final WasmFunction getMonotonic = instance.getFunction("get_monotonic").orElseThrow();
      final WasmValue[] result = getMonotonic.call();

      final long timeNanos = result[0].asLong();

      LOGGER.info("Monotonic clock value: " + timeNanos + " ns");

      // Monotonic clock should be non-negative
      assertTrue(timeNanos >= 0, "Monotonic clock should be non-negative");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return monotonically increasing values")
    void shouldReturnMonotonicallyIncreasingValues(final RuntimeType runtime) throws Exception {
      setUpClockResources(runtime);

      final WasmFunction getMonotonic = instance.getFunction("get_monotonic").orElseThrow();

      long previousTime = 0;

      for (int i = 0; i < 10; i++) {
        final WasmValue[] result = getMonotonic.call();
        final long currentTime = result[0].asLong();

        LOGGER.info("Monotonic clock iteration " + i + ": " + currentTime + " ns");

        assertTrue(currentTime >= previousTime, "Monotonic clock should not go backwards");
        previousTime = currentTime;
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return monotonic clock resolution")
    void shouldReturnMonotonicClockResolution(final RuntimeType runtime) throws Exception {
      setUpClockResources(runtime);

      final WasmFunction getResolution =
          instance.getFunction("get_monotonic_resolution").orElseThrow();
      final WasmValue[] result = getResolution.call();

      final long resolution = result[0].asLong();

      LOGGER.info("Monotonic clock resolution: " + resolution + " ns");

      // Resolution should be positive and reasonable
      assertTrue(resolution > 0, "Monotonic resolution should be positive");
      assertTrue(resolution <= 1000000000L, "Monotonic resolution should be at most 1 second");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should measure elapsed time accurately")
    void shouldMeasureElapsedTimeAccurately(final RuntimeType runtime) throws Exception {
      setUpClockResources(runtime);

      final WasmFunction getMonotonic = instance.getFunction("get_monotonic").orElseThrow();

      final WasmValue[] startResult = getMonotonic.call();
      final long startTime = startResult[0].asLong();

      // Sleep for approximately 50ms
      Thread.sleep(50);

      final WasmValue[] endResult = getMonotonic.call();
      final long endTime = endResult[0].asLong();

      final long elapsedNanos = endTime - startTime;
      final long elapsedMillis = elapsedNanos / 1000000;

      LOGGER.info("Measured elapsed time: " + elapsedMillis + " ms");

      // Should be at least 40ms (allowing for scheduling variance)
      // and less than 200ms (reasonable upper bound)
      assertTrue(elapsedMillis >= 40, "Elapsed time should be at least 40ms");
      assertTrue(elapsedMillis < 200, "Elapsed time should be less than 200ms");
    }
  }

  @Nested
  @DisplayName("Clock Comparison Tests")
  class ClockComparisonTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("realtime clock should be larger than monotonic (epoch vs uptime)")
    void realtimeShouldBeLargerThanMonotonic(final RuntimeType runtime) throws Exception {
      setUpClockResources(runtime);

      final WasmFunction getRealtime = instance.getFunction("get_realtime").orElseThrow();
      final WasmFunction getMonotonic = instance.getFunction("get_monotonic").orElseThrow();

      final WasmValue[] realtimeResult = getRealtime.call();
      final long realtimeNanos = realtimeResult[0].asLong();

      final WasmValue[] monotonicResult = getMonotonic.call();
      final long monotonicNanos = monotonicResult[0].asLong();

      LOGGER.info("Realtime: " + realtimeNanos + " ns");
      LOGGER.info("Monotonic: " + monotonicNanos + " ns");

      // Realtime is since Unix epoch (1970), monotonic is since system boot
      // So realtime should be much larger
      assertTrue(realtimeNanos > monotonicNanos, "Realtime should be greater than monotonic");
    }
  }
}
