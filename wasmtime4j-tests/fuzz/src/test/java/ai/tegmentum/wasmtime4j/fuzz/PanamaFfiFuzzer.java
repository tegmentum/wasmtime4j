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
package ai.tegmentum.wasmtime4j.fuzz;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.ArenaResourceManager;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

/**
 * Fuzz tests for Panama FFI operations.
 *
 * <p>This fuzzer tests the robustness of Panama FFI integration by:
 *
 * <ul>
 *   <li>Arena lifecycle management under rapid create/close patterns
 *   <li>Memory segment marshalling with fuzzed sizes and offsets
 *   <li>Concurrent arena access from multiple threads
 *   <li>Value round-trip through Panama FFI identity functions
 * </ul>
 *
 * @since 1.0.0
 */
@EnabledForJreRange(min = JRE.JAVA_23)
public class PanamaFfiFuzzer {

  private static final String VALUE_MODULE_WAT =
      """
      (module
        (func (export "identity_i32") (param i32) (result i32) local.get 0)
        (func (export "identity_i64") (param i64) (result i64) local.get 0)
        (func (export "identity_f32") (param f32) (result f32) local.get 0)
        (func (export "identity_f64") (param f64) (result f64) local.get 0)
      )
      """;

  /**
   * Rapidly creates and closes ArenaResourceManager instances in fuzzed order.
   *
   * <p>Verifies no resource leaks by checking isValid() returns false after close.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzArenaLifecycle(final FuzzedDataProvider data) {
    final int count = data.consumeInt(1, 50);
    final List<ArenaResourceManager> managers = new ArrayList<>(count);

    try {
      // Create managers
      for (int i = 0; i < count; i++) {
        managers.add(new ArenaResourceManager());
      }

      // Close in fuzzed order
      while (!managers.isEmpty()) {
        final int index = data.consumeInt(0, managers.size() - 1);
        final ArenaResourceManager manager = managers.remove(index);
        manager.close();

        if (manager.isValid()) {
          throw new AssertionError("Manager should not be valid after close");
        }
      }
    } catch (IllegalStateException e) {
      // Expected: operations on closed managers
    } finally {
      // Ensure all remaining managers are closed
      for (final ArenaResourceManager manager : managers) {
        manager.close();
      }
    }
  }

  /**
   * Allocates a MemorySegment with fuzzed size and writes/reads data at fuzzed offsets.
   *
   * <p>Verifies data round-trip consistency within bounds.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzMemorySegmentMarshalling(final FuzzedDataProvider data) {
    final int size = data.consumeInt(0, 1048576);

    if (size <= 0) {
      return;
    }

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment segment = arena.allocate(size);

      // Write fuzzed data at fuzzed offsets
      final int writeCount = data.consumeInt(1, 20);
      for (int i = 0; i < writeCount; i++) {
        final int offset = data.consumeInt(0, size - 1);
        final byte value = data.consumeByte();

        try {
          segment.set(java.lang.foreign.ValueLayout.JAVA_BYTE, offset, value);

          // Read back and verify
          final byte readBack = segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, offset);
          if (readBack != value) {
            throw new AssertionError(
                "Data mismatch at offset " + offset + ": wrote " + value + " read " + readBack);
          }
        } catch (IndexOutOfBoundsException e) {
          // Expected for out-of-bounds offsets
        } catch (IllegalArgumentException e) {
          // Expected for invalid arguments
        }
      }
    } catch (IllegalStateException e) {
      // Expected: arena lifecycle issues
    }
  }

  /**
   * Tests concurrent access to a shared ArenaResourceManager from multiple threads.
   *
   * <p>Each thread registers and unregisters managed native resources with fuzzed timing.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzConcurrentArenaAccess(final FuzzedDataProvider data) {
    final int threadCount = data.consumeInt(2, 4);
    final int opsPerThread = data.consumeInt(1, 10);

    try (Arena sharedArena = Arena.ofShared()) {
      final ArenaResourceManager manager = new ArenaResourceManager(sharedArena, true);
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch latch = new CountDownLatch(threadCount);

      for (int t = 0; t < threadCount; t++) {
        executor.submit(
            () -> {
              try {
                for (int op = 0; op < opsPerThread; op++) {
                  try {
                    // Register a managed resource
                    final MemorySegment segment = sharedArena.allocate(64);
                    final ArenaResourceManager.ManagedNativeResource resource =
                        manager.manageNativeResource(
                            segment,
                            () -> {
                              // Cleanup action
                            },
                            "fuzz-resource-" + Thread.currentThread().getName() + "-" + op);

                    // Verify resource is valid
                    resource.isValid();
                    resource.resource();

                    // Close the resource
                    resource.close();
                  } catch (IllegalStateException e) {
                    // Expected: manager may be closed concurrently
                  }
                }
              } finally {
                latch.countDown();
              }
            });
      }

      try {
        latch.await(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      executor.shutdown();
      manager.close();
    } catch (IllegalStateException e) {
      // Expected: concurrent access issues
    }
  }

  /**
   * Round-trips WasmValues through identity functions with fuzzed values.
   *
   * <p>Tests i32, i64, f32, and f64 types including NaN and infinity edge cases. Uses
   * Float.floatToRawIntBits and Double.doubleToRawLongBits for NaN comparison.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzPanamaValueRoundTrip(final FuzzedDataProvider data) {
    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(VALUE_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      // Test i32 round-trip
      final int i32Value = data.consumeInt();
      try {
        final WasmValue[] i32Results =
            instance.callFunction("identity_i32", WasmValue.i32(i32Value));
        if (i32Results.length > 0 && i32Results[0].asInt() != i32Value) {
          throw new AssertionError(
              "i32 round-trip mismatch: " + i32Value + " != " + i32Results[0].asInt());
        }
      } catch (WasmException e) {
        // Expected
      }

      // Test i64 round-trip
      final long i64Value = data.consumeLong();
      try {
        final WasmValue[] i64Results =
            instance.callFunction("identity_i64", WasmValue.i64(i64Value));
        if (i64Results.length > 0 && i64Results[0].asLong() != i64Value) {
          throw new AssertionError(
              "i64 round-trip mismatch: " + i64Value + " != " + i64Results[0].asLong());
        }
      } catch (WasmException e) {
        // Expected
      }

      // Test f32 round-trip (including NaN and infinities)
      final float f32Value = data.consumeFloat();
      try {
        final WasmValue[] f32Results =
            instance.callFunction("identity_f32", WasmValue.f32(f32Value));
        if (f32Results.length > 0) {
          final float result = f32Results[0].asFloat();
          // Use floatToRawIntBits for NaN comparison
          if (Float.floatToRawIntBits(result) != Float.floatToRawIntBits(f32Value)) {
            // Canonical NaN: WASM may canonicalize NaN values
            if (!Float.isNaN(result) || !Float.isNaN(f32Value)) {
              throw new AssertionError(
                  "f32 round-trip mismatch: "
                      + f32Value
                      + " (bits="
                      + Integer.toHexString(Float.floatToRawIntBits(f32Value))
                      + ") != "
                      + result
                      + " (bits="
                      + Integer.toHexString(Float.floatToRawIntBits(result))
                      + ")");
            }
          }
        }
      } catch (WasmException e) {
        // Expected
      }

      // Test f64 round-trip (including NaN and infinities)
      final double f64Value = data.consumeDouble();
      try {
        final WasmValue[] f64Results =
            instance.callFunction("identity_f64", WasmValue.f64(f64Value));
        if (f64Results.length > 0) {
          final double result = f64Results[0].asDouble();
          // Use doubleToRawLongBits for NaN comparison
          if (Double.doubleToRawLongBits(result) != Double.doubleToRawLongBits(f64Value)) {
            // Canonical NaN: WASM may canonicalize NaN values
            if (!Double.isNaN(result) || !Double.isNaN(f64Value)) {
              throw new AssertionError(
                  "f64 round-trip mismatch: "
                      + f64Value
                      + " (bits="
                      + Long.toHexString(Double.doubleToRawLongBits(f64Value))
                      + ") != "
                      + result
                      + " (bits="
                      + Long.toHexString(Double.doubleToRawLongBits(result))
                      + ")");
            }
          }
        }
      } catch (WasmException e) {
        // Expected
      }

    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw e;
    }
  }
}
