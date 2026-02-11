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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasiContext;
import ai.tegmentum.wasmtime4j.WasiLinker;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
class WasiClockIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WasiClockIntegrationTest.class.getName());

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

  private static boolean wasiClockAvailable = false;
  private static WasmRuntime sharedRuntime;
  private static Engine sharedEngine;

  private Store store;
  private WasiContext wasiContext;
  private Module module;
  private Instance instance;
  private Linker<WasiContext> linker;

  @BeforeAll
  static void checkWasiClockAvailable() {
    LOGGER.info("Checking WASI clock availability");
    try {
      sharedRuntime = WasmRuntimeFactory.create();
      sharedEngine = sharedRuntime.createEngine();

      // Check if WASI is available by trying to compile the clock module
      final Module testModule = sharedRuntime.compileModuleWat(sharedEngine, CLOCK_MODULE_WAT);
      testModule.close();

      wasiClockAvailable = true;
      LOGGER.info("WASI clock is available");
    } catch (final Exception e) {
      LOGGER.warning("WASI clock not available: " + e.getMessage());
      wasiClockAvailable = false;
    }
  }

  @AfterAll
  static void cleanupSharedResources() {
    if (sharedEngine != null) {
      try {
        sharedEngine.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing shared engine: " + e.getMessage());
      }
    }
    if (sharedRuntime != null) {
      try {
        sharedRuntime.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing shared runtime: " + e.getMessage());
      }
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    assumeTrue(wasiClockAvailable, "WASI clock must be available for these tests");

    LOGGER.info("Setting up clock test resources");

    wasiContext = WasiContext.create().inheritStdio();

    store = sharedRuntime.createStore(sharedEngine);
    module = sharedRuntime.compileModuleWat(sharedEngine, CLOCK_MODULE_WAT);

    // Link WASI and instantiate
    linker = WasiLinker.createLinker(sharedEngine, wasiContext);
    instance = linker.instantiate(store, module);

    LOGGER.info("Clock test resources created");
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up clock test resources");

    closeQuietly(instance, "instance");
    closeQuietly(linker, "linker");
    closeQuietly(module, "module");
    closeQuietly(store, "store");
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

  @Nested
  @DisplayName("Realtime Clock Tests")
  class RealtimeClockTests {

    @Test
    @DisplayName("should return positive realtime clock value")
    void shouldReturnPositiveRealtimeValue() throws Exception {
      final WasmFunction getRealtime = instance.getFunction("get_realtime").orElseThrow();
      final WasmValue[] result = getRealtime.call();

      final long timeNanos = result[0].asLong();

      LOGGER.info("Realtime clock value: " + timeNanos + " ns");

      // Time should be positive and reasonable (after year 2020)
      // 2020-01-01 in nanoseconds since epoch is approximately 1577836800000000000
      assertThat(timeNanos).isGreaterThan(1577836800000000000L);
    }

    @Test
    @DisplayName("should return increasing realtime clock values")
    void shouldReturnIncreasingRealtimeValues() throws Exception {
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

      assertThat(time2).isGreaterThan(time1);
    }

    @Test
    @DisplayName("should return realtime clock resolution")
    void shouldReturnRealtimeClockResolution() throws Exception {
      final WasmFunction getResolution =
          instance.getFunction("get_realtime_resolution").orElseThrow();
      final WasmValue[] result = getResolution.call();

      final long resolution = result[0].asLong();

      LOGGER.info("Realtime clock resolution: " + resolution + " ns");

      // Resolution should be positive and reasonable (nanoseconds to seconds)
      assertThat(resolution).isGreaterThan(0);
      assertThat(resolution).isLessThanOrEqualTo(1000000000L); // At most 1 second
    }
  }

  @Nested
  @DisplayName("Monotonic Clock Tests")
  class MonotonicClockTests {

    @Test
    @DisplayName("should return positive monotonic clock value")
    void shouldReturnPositiveMonotonicValue() throws Exception {
      final WasmFunction getMonotonic = instance.getFunction("get_monotonic").orElseThrow();
      final WasmValue[] result = getMonotonic.call();

      final long timeNanos = result[0].asLong();

      LOGGER.info("Monotonic clock value: " + timeNanos + " ns");

      // Monotonic clock should be non-negative
      assertThat(timeNanos).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("should return monotonically increasing values")
    void shouldReturnMonotonicallyIncreasingValues() throws Exception {
      final WasmFunction getMonotonic = instance.getFunction("get_monotonic").orElseThrow();

      long previousTime = 0;

      for (int i = 0; i < 10; i++) {
        final WasmValue[] result = getMonotonic.call();
        final long currentTime = result[0].asLong();

        LOGGER.info("Monotonic clock iteration " + i + ": " + currentTime + " ns");

        assertThat(currentTime).isGreaterThanOrEqualTo(previousTime);
        previousTime = currentTime;
      }
    }

    @Test
    @DisplayName("should return monotonic clock resolution")
    void shouldReturnMonotonicClockResolution() throws Exception {
      final WasmFunction getResolution =
          instance.getFunction("get_monotonic_resolution").orElseThrow();
      final WasmValue[] result = getResolution.call();

      final long resolution = result[0].asLong();

      LOGGER.info("Monotonic clock resolution: " + resolution + " ns");

      // Resolution should be positive and reasonable
      assertThat(resolution).isGreaterThan(0);
      assertThat(resolution).isLessThanOrEqualTo(1000000000L); // At most 1 second
    }

    @Test
    @DisplayName("should measure elapsed time accurately")
    void shouldMeasureElapsedTimeAccurately() throws Exception {
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
      assertThat(elapsedMillis).isGreaterThanOrEqualTo(40);
      assertThat(elapsedMillis).isLessThan(200);
    }
  }

  @Nested
  @DisplayName("Clock Comparison Tests")
  class ClockComparisonTests {

    @Test
    @DisplayName("realtime clock should be larger than monotonic (epoch vs uptime)")
    void realtimeShouldBeLargerThanMonotonic() throws Exception {
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
      assertThat(realtimeNanos).isGreaterThan(monotonicNanos);
    }
  }
}
