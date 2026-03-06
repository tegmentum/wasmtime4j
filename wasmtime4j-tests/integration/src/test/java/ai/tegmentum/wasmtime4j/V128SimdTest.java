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
package ai.tegmentum.wasmtime4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for V128/SIMD value round-trips through WebAssembly.
 *
 * <p>Validates that 128-bit vector values can be correctly passed to and returned from WASM
 * functions using the SIMD proposal across both JNI and Panama implementations.
 */
class V128SimdTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(V128SimdTest.class.getName());

  /** WAT module with a function that takes a v128 and returns it unchanged. */
  private static final String V128_IDENTITY_WAT =
      "(module\n"
          + "  (func (export \"identity\") (param v128) (result v128)\n"
          + "    local.get 0\n"
          + "  )\n"
          + "  (func (export \"zero\") (result v128)\n"
          + "    v128.const i32x4 0 0 0 0\n"
          + "  )\n"
          + "  (func (export \"all_ones\") (result v128)\n"
          + "    v128.const i32x4 -1 -1 -1 -1\n"
          + "  )\n"
          + "  (func (export \"add_i32x4\") (param v128 v128) (result v128)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32x4.add\n"
          + "  )\n"
          + ")";

  @AfterEach
  void tearDown() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testV128IdentityRoundTrip(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 identity round-trip");
    try (Engine engine = Engine.create();
        Store store = Store.create(engine)) {
      final Module module =
          Module.compile(engine, V128_IDENTITY_WAT.getBytes(StandardCharsets.UTF_8));
      final Instance instance = Instance.create(store, module);
      final WasmFunction identity = instance.getFunction("identity").orElseThrow();

      final byte[] input = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
      final WasmValue[] results = identity.call(WasmValue.v128(input));

      assertThat(results).as("Should return one result").hasSize(1);
      assertThat(results[0].isV128()).as("Result should be V128").isTrue();
      assertThat(results[0].asV128()).as("V128 identity should return same bytes").isEqualTo(input);

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testV128ZeroVector(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 zero vector");
    try (Engine engine = Engine.create();
        Store store = Store.create(engine)) {
      final Module module =
          Module.compile(engine, V128_IDENTITY_WAT.getBytes(StandardCharsets.UTF_8));
      final Instance instance = Instance.create(store, module);
      final WasmFunction zero = instance.getFunction("zero").orElseThrow();

      final WasmValue[] results = zero.call();
      assertThat(results).hasSize(1);
      assertThat(results[0].isV128()).isTrue();
      assertThat(results[0].asV128()).isEqualTo(new byte[16]);

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testV128AllOnes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 all-ones vector");
    try (Engine engine = Engine.create();
        Store store = Store.create(engine)) {
      final Module module =
          Module.compile(engine, V128_IDENTITY_WAT.getBytes(StandardCharsets.UTF_8));
      final Instance instance = Instance.create(store, module);
      final WasmFunction allOnes = instance.getFunction("all_ones").orElseThrow();

      final WasmValue[] results = allOnes.call();
      assertThat(results).hasSize(1);
      assertThat(results[0].isV128()).isTrue();

      final byte[] expected = new byte[16];
      java.util.Arrays.fill(expected, (byte) 0xFF);
      assertThat(results[0].asV128())
          .as("All-ones vector should have all bytes set to 0xFF")
          .isEqualTo(expected);

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testV128AddI32x4(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 i32x4 addition");
    try (Engine engine = Engine.create();
        Store store = Store.create(engine)) {
      final Module module =
          Module.compile(engine, V128_IDENTITY_WAT.getBytes(StandardCharsets.UTF_8));
      final Instance instance = Instance.create(store, module);
      final WasmFunction add = instance.getFunction("add_i32x4").orElseThrow();

      // Create two v128 values representing i32x4 [1, 2, 3, 4] and [10, 20, 30, 40]
      final WasmValue a = WasmValue.v128(intArrayToV128(1, 2, 3, 4));
      final WasmValue b = WasmValue.v128(intArrayToV128(10, 20, 30, 40));

      final WasmValue[] results = add.call(a, b);
      assertThat(results).hasSize(1);
      assertThat(results[0].isV128()).isTrue();

      final byte[] expected = intArrayToV128(11, 22, 33, 44);
      assertThat(results[0].asV128())
          .as("i32x4.add([1,2,3,4], [10,20,30,40]) should equal [11,22,33,44]")
          .isEqualTo(expected);

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testV128FromHighLow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 creation from high/low longs");
    try (Engine engine = Engine.create();
        Store store = Store.create(engine)) {
      final Module module =
          Module.compile(engine, V128_IDENTITY_WAT.getBytes(StandardCharsets.UTF_8));
      final Instance instance = Instance.create(store, module);
      final WasmFunction identity = instance.getFunction("identity").orElseThrow();

      final long high = 0x0102030405060708L;
      final long low = 0x090A0B0C0D0E0F10L;
      final WasmValue input = WasmValue.v128(high, low);

      final WasmValue[] results = identity.call(input);
      assertThat(results).hasSize(1);
      assertThat(results[0].isV128()).isTrue();
      assertThat(results[0].asV128())
          .as("Round-trip of high/low v128 should preserve bytes")
          .isEqualTo(input.asV128());

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testV128InvalidSizeThrows(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 creation with invalid size throws");
    assertThrows(
        IllegalArgumentException.class,
        () -> WasmValue.v128(new byte[] {1, 2, 3}),
        "Creating v128 with non-16-byte array should throw");
    assertThrows(
        IllegalArgumentException.class,
        () -> WasmValue.v128((byte[]) null),
        "Creating v128 with null should throw");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  void testV128Equality(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 WasmValue equality");
    final byte[] bytes = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    final WasmValue v1 = WasmValue.v128(bytes);
    final WasmValue v2 = WasmValue.v128(bytes.clone());
    final WasmValue v3 = WasmValue.v128(new byte[16]);

    assertThat(v1).as("V128 with same bytes should be equal").isEqualTo(v2);
    assertThat(v1).as("V128 with different bytes should not be equal").isNotEqualTo(v3);
    assertThat(v1.hashCode())
        .as("Equal V128 values should have same hashCode")
        .isEqualTo(v2.hashCode());
  }

  /**
   * Converts 4 ints to a 16-byte v128 in little-endian format (i32x4 lane order).
   *
   * @param a first i32 lane
   * @param b second i32 lane
   * @param c third i32 lane
   * @param d fourth i32 lane
   * @return 16-byte array representing the v128 value
   */
  private static byte[] intArrayToV128(final int a, final int b, final int c, final int d) {
    final byte[] result = new byte[16];
    writeI32LE(result, 0, a);
    writeI32LE(result, 4, b);
    writeI32LE(result, 8, c);
    writeI32LE(result, 12, d);
    return result;
  }

  private static void writeI32LE(final byte[] buf, final int offset, final int value) {
    buf[offset] = (byte) (value & 0xFF);
    buf[offset + 1] = (byte) ((value >>> 8) & 0xFF);
    buf[offset + 2] = (byte) ((value >>> 16) & 0xFF);
    buf[offset + 3] = (byte) ((value >>> 24) & 0xFF);
  }
}
