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

package ai.tegmentum.wasmtime4j.simd;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for v128 (SIMD) value support. Verifies that v128 values can be constructed in the Java
 * API, that WASM SIMD modules compile and execute, and documents current limitations around passing
 * v128 values through the FFI boundary.
 *
 * @since 1.0.0
 */
@DisplayName("V128 Value Passing Tests")
public class V128ValuePassingTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(V128ValuePassingTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WasmValue.v128() API creates valid v128 values")
  void v128ApiCreation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing v128 API creation");

    // Create v128 from byte array
    final byte[] inputBytes =
        new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    final WasmValue byteValue = WasmValue.v128(inputBytes);
    assertNotNull(byteValue, "v128 from bytes should not be null");
    assertEquals(WasmValueType.V128, byteValue.getType(), "Type should be V128");
    assertArrayEquals(inputBytes, byteValue.asV128(), "Bytes should round-trip through API");
    LOGGER.info("[" + runtime + "] v128 from byte[] created successfully");

    // Create v128 from high/low longs
    final long high = 0x0123456789ABCDEFL;
    final long low = 0xFEDCBA9876543210L;
    final WasmValue longValue = WasmValue.v128(high, low);
    assertNotNull(longValue, "v128 from high/low should not be null");
    assertEquals(WasmValueType.V128, longValue.getType(), "Type should be V128");
    assertTrue(longValue.isV128(), "isV128() should return true");
    LOGGER.info("[" + runtime + "] v128 from high/low created successfully");

    // Create all-zeros
    final WasmValue zeros = WasmValue.v128(new byte[16]);
    assertArrayEquals(new byte[16], zeros.asV128(), "All-zeros v128 should round-trip");
    LOGGER.info("[" + runtime + "] All-zeros v128 created successfully");

    // Create all-ones
    final byte[] ones = new byte[16];
    for (int i = 0; i < 16; i++) {
      ones[i] = (byte) 0xFF;
    }
    final WasmValue allOnes = WasmValue.v128(ones);
    assertArrayEquals(ones, allOnes.asV128(), "All-ones v128 should round-trip");
    LOGGER.info("[" + runtime + "] All-ones v128 created successfully");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("v128 requires exactly 16 bytes")
  void v128RequiresExact16Bytes(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing v128 byte length validation");

    assertThrows(
        Exception.class,
        () -> WasmValue.v128(new byte[0]),
        "0 bytes should be rejected");
    assertThrows(
        Exception.class,
        () -> WasmValue.v128(new byte[15]),
        "15 bytes should be rejected");
    assertThrows(
        Exception.class,
        () -> WasmValue.v128(new byte[17]),
        "17 bytes should be rejected");
    LOGGER.info("[" + runtime + "] v128 byte length validation works");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("SIMD module compiles and executes internally")
  void simdModuleCompilesAndExecutes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing SIMD module compilation and execution");

    // Module uses SIMD internally without passing v128 across FFI boundary
    final String wat =
        """
        (module
          (func (export "simd_add") (result i32)
            ;; Create two i32x4 vectors and add them
            i32.const 10
            i32x4.splat
            i32.const 20
            i32x4.splat
            i32x4.add

            ;; Extract lane 0 to verify the computation
            i32x4.extract_lane 0))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "SIMD module should compile");

      final Instance instance = module.instantiate(store);
      final WasmValue[] result = instance.callFunction("simd_add");
      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Should return one value");
      assertEquals(30, result[0].asInt(), "10 + 20 = 30 via SIMD");
      LOGGER.info("[" + runtime + "] SIMD add result: " + result[0].asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("SIMD operations produce correct lane values")
  void simdLaneOperationsCorrect(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing SIMD lane operations");

    // Module performs SIMD operations and extracts individual lanes
    final String wat =
        """
        (module
          ;; Splat 42 into all i32x4 lanes, extract each
          (func (export "lane0") (result i32)
            i32.const 42
            i32x4.splat
            i32x4.extract_lane 0)
          (func (export "lane1") (result i32)
            i32.const 42
            i32x4.splat
            i32x4.extract_lane 1)
          (func (export "lane2") (result i32)
            i32.const 42
            i32x4.splat
            i32x4.extract_lane 2)
          (func (export "lane3") (result i32)
            i32.const 42
            i32x4.splat
            i32x4.extract_lane 3))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      for (int lane = 0; lane < 4; lane++) {
        final WasmValue[] result = instance.callFunction("lane" + lane);
        assertEquals(42, result[0].asInt(), "Lane " + lane + " should be 42");
      }
      LOGGER.info("[" + runtime + "] All 4 SIMD lanes correct");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Passing v128 as function argument is not yet supported at FFI layer")
  void v128PassThroughNotSupported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Documenting v128 FFI pass-through limitation");

    final String wat =
        """
        (module
          (func (export "identity") (param v128) (result v128)
            local.get 0))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Module with v128 param/return should compile");

      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Module with v128 param/return should instantiate");

      // Passing v128 across FFI boundary may or may not work depending on runtime.
      // The critical requirement is the JVM does NOT crash.
      final WasmValue input = WasmValue.v128(new byte[16]);
      try {
        final WasmValue[] result = instance.callFunction("identity", input);
        LOGGER.info("[" + runtime + "] v128 pass-through succeeded, result type: "
            + result[0].getType());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] v128 pass-through threw (expected on some runtimes): "
            + e.getClass().getName() + " - " + e.getMessage());
        assertNotNull(e.getMessage(), "Exception should have a message");
      }

      instance.close();
      module.close();
    }
  }
}
