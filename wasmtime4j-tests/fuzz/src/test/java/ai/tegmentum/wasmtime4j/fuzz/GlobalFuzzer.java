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
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.util.Optional;

/**
 * Fuzz tests for WebAssembly global variable operations.
 *
 * <p>This fuzzer tests the robustness of global operations including:
 *
 * <ul>
 *   <li>Getting and setting global values
 *   <li>Testing mutability constraints
 *   <li>Testing various value types (i32, i64, f32, f64)
 * </ul>
 *
 * @since 1.0.0
 */
public class GlobalFuzzer {

  /** A module with mutable and immutable globals of various types. */
  private static final String GLOBALS_MODULE_WAT =
      """
        (module
            (global $g_i32_mut (export "g_i32_mut") (mut i32) (i32.const 0))
            (global $g_i64_mut (export "g_i64_mut") (mut i64) (i64.const 0))
            (global $g_f32_mut (export "g_f32_mut") (mut f32) (f32.const 0.0))
            (global $g_f64_mut (export "g_f64_mut") (mut f64) (f64.const 0.0))
            (global $g_i32_const (export "g_i32_const") i32 (i32.const 42))
            (global $g_i64_const (export "g_i64_const") i64 (i64.const 100))
        )
        """;

  /** List of mutable global names. */
  private static final String[] MUTABLE_GLOBALS = {
    "g_i32_mut", "g_i64_mut", "g_f32_mut", "g_f64_mut"
  };

  /** List of immutable global names. */
  private static final String[] IMMUTABLE_GLOBALS = {"g_i32_const", "g_i64_const"};

  /**
   * Fuzz test for getting and setting global values.
   *
   * <p>This test sets and gets global values with fuzzed data. The runtime should handle all valid
   * values and properly enforce mutability constraints.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzGlobalGetSet(final FuzzedDataProvider data) {
    final int globalChoice = data.consumeInt(0, MUTABLE_GLOBALS.length - 1);
    final String globalName = MUTABLE_GLOBALS[globalChoice];

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(GLOBALS_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmGlobal> globalOpt = instance.getGlobal(globalName);
      if (globalOpt.isEmpty()) {
        return;
      }

      final WasmGlobal global = globalOpt.get();

      // Verify it's mutable
      if (!global.isMutable()) {
        throw new AssertionError("Expected mutable global: " + globalName);
      }

      // Set and get based on global type
      try {
        switch (globalChoice) {
          case 0 -> {
            // i32 global
            final int value = data.consumeInt();
            global.set(WasmValue.i32(value));
            final WasmValue readBack = global.get();
            if (readBack.asInt() != value) {
              throw new AssertionError(
                  "i32 global value mismatch: " + value + " vs " + readBack.asInt());
            }
          }
          case 1 -> {
            // i64 global
            final long value = data.consumeLong();
            global.set(WasmValue.i64(value));
            final WasmValue readBack = global.get();
            if (readBack.asLong() != value) {
              throw new AssertionError(
                  "i64 global value mismatch: " + value + " vs " + readBack.asLong());
            }
          }
          case 2 -> {
            // f32 global
            final float value = data.consumeFloat();
            global.set(WasmValue.f32(value));
            final WasmValue readBack = global.get();
            // Use Float.compare for NaN handling
            if (Float.compare(readBack.asFloat(), value) != 0) {
              throw new AssertionError(
                  "f32 global value mismatch: " + value + " vs " + readBack.asFloat());
            }
          }
          case 3 -> {
            // f64 global
            final double value = data.consumeDouble();
            global.set(WasmValue.f64(value));
            final WasmValue readBack = global.get();
            // Use Double.compare for NaN handling
            if (Double.compare(readBack.asDouble(), value) != 0) {
              throw new AssertionError(
                  "f64 global value mismatch: " + value + " vs " + readBack.asDouble());
            }
          }
          default -> {
            // Unexpected case
          }
        }
      } catch (RuntimeException e) {
        // Expected for type mismatches or validation errors
      }

    } catch (WasmException e) {
      // Expected for various global errors
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for mutability constraints.
   *
   * <p>This test attempts to modify immutable globals. The runtime should reject such modifications
   * with appropriate exceptions.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzGlobalMutability(final FuzzedDataProvider data) {
    final int globalChoice = data.consumeInt(0, IMMUTABLE_GLOBALS.length - 1);
    final String globalName = IMMUTABLE_GLOBALS[globalChoice];
    final int setValue = data.consumeInt();

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(GLOBALS_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmGlobal> globalOpt = instance.getGlobal(globalName);
      if (globalOpt.isEmpty()) {
        return;
      }

      final WasmGlobal global = globalOpt.get();

      // Verify it's immutable
      if (global.isMutable()) {
        throw new AssertionError("Expected immutable global: " + globalName);
      }

      // Try to set the immutable global - this should fail
      try {
        global.set(WasmValue.i32(setValue));
        // If we reach here, the immutability wasn't enforced
        throw new AssertionError("Immutable global accepted set: " + globalName);
      } catch (RuntimeException e) {
        // Expected - immutable globals should reject modifications
      }

      // Reading should still work
      final WasmValue value = global.get();
      if (value == null) {
        throw new AssertionError("Immutable global returned null value: " + globalName);
      }

    } catch (WasmException e) {
      // Expected for various global errors
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for global type compatibility.
   *
   * <p>This test attempts to set global values with mismatched types. The runtime should reject
   * type mismatches gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzGlobalTypes(final FuzzedDataProvider data) {
    final int globalChoice = data.consumeInt(0, MUTABLE_GLOBALS.length - 1);
    final String globalName = MUTABLE_GLOBALS[globalChoice];
    final int wrongTypeChoice = data.consumeInt(0, 3);

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(GLOBALS_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmGlobal> globalOpt = instance.getGlobal(globalName);
      if (globalOpt.isEmpty()) {
        return;
      }

      final WasmGlobal global = globalOpt.get();

      // Try to set with a potentially wrong type
      try {
        switch (wrongTypeChoice) {
          case 0 -> global.set(WasmValue.i32(data.consumeInt()));
          case 1 -> global.set(WasmValue.i64(data.consumeLong()));
          case 2 -> global.set(WasmValue.f32(data.consumeFloat()));
          case 3 -> global.set(WasmValue.f64(data.consumeDouble()));
          default -> {
            // Unexpected case
          }
        }
        // If the types match, this should succeed
      } catch (RuntimeException e) {
        // Expected for type mismatches
      }

    } catch (WasmException e) {
      // Expected for various global errors
    } catch (Exception e) {
      throw e;
    }
  }
}
