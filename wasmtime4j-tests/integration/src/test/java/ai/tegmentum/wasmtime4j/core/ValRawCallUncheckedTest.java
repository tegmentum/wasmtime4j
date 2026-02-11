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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.type.ValRaw;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests {@link WasmFunction#callUnchecked(ValRaw...)} and the {@link ValRaw} type. The
 * callUnchecked method bypasses type checking and warns of "undefined behavior, memory corruption,
 * or JVM crashes" if types are wrong. These tests verify correct behavior with correct types.
 *
 * @since 1.0.0
 */
@DisplayName("ValRaw and callUnchecked Tests")
public class ValRawCallUncheckedTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(ValRawCallUncheckedTest.class.getName());

  private static final String WAT =
      """
      (module
        (func (export "add_i32") (param i32 i32) (result i32)
          local.get 0 local.get 1 i32.add)
        (func (export "add_i64") (param i64 i64) (result i64)
          local.get 0 local.get 1 i64.add)
        (func (export "mul_f32") (param f32 f32) (result f32)
          local.get 0 local.get 1 f32.mul)
        (func (export "div_f64") (param f64 f64) (result f64)
          local.get 0 local.get 1 f64.div)
        (func (export "multi_return") (param i32 i64) (result i32 i64)
          local.get 0 local.get 1)
        (func (export "nop")))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callUnchecked i32 round-trip with add_i32")
  void callUncheckedI32RoundTrip(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callUnchecked i32 round-trip");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmFunction> funcOpt = instance.getFunction("add_i32");
      assert funcOpt.isPresent() : "add_i32 export must be present";
      final WasmFunction addI32 = funcOpt.get();

      final ValRaw[] results =
          addI32.callUnchecked(ValRaw.i32(10), ValRaw.i32(32));

      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should have exactly 1 result");
      assertEquals(42, results[0].asI32(), "10 + 32 should equal 42");
      LOGGER.info("[" + runtime + "] callUnchecked i32 result: " + results[0].asI32());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callUnchecked i64 round-trip with Long.MAX_VALUE edge case")
  void callUncheckedI64RoundTrip(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callUnchecked i64 round-trip");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmFunction> funcOpt = instance.getFunction("add_i64");
      assert funcOpt.isPresent() : "add_i64 export must be present";
      final WasmFunction addI64 = funcOpt.get();

      final ValRaw[] results =
          addI64.callUnchecked(ValRaw.i64(Long.MAX_VALUE), ValRaw.i64(0L));

      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should have exactly 1 result");
      assertEquals(Long.MAX_VALUE, results[0].asI64(),
          "MAX_VALUE + 0 should equal MAX_VALUE");
      LOGGER.info("[" + runtime + "] callUnchecked i64 result: " + results[0].asI64());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callUnchecked f32 round-trip with mul_f32")
  void callUncheckedF32RoundTrip(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callUnchecked f32 round-trip");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmFunction> funcOpt = instance.getFunction("mul_f32");
      assert funcOpt.isPresent() : "mul_f32 export must be present";
      final WasmFunction mulF32 = funcOpt.get();

      final ValRaw[] results =
          mulF32.callUnchecked(ValRaw.f32(3.0f), ValRaw.f32(7.0f));

      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should have exactly 1 result");
      assertEquals(21.0f, results[0].asF32(), 0.001f,
          "3.0 * 7.0 should equal 21.0");
      LOGGER.info("[" + runtime + "] callUnchecked f32 result: " + results[0].asF32());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callUnchecked f64 round-trip with div_f64")
  void callUncheckedF64RoundTrip(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callUnchecked f64 round-trip");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmFunction> funcOpt = instance.getFunction("div_f64");
      assert funcOpt.isPresent() : "div_f64 export must be present";
      final WasmFunction divF64 = funcOpt.get();

      final ValRaw[] results =
          divF64.callUnchecked(ValRaw.f64(22.0), ValRaw.f64(7.0));

      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should have exactly 1 result");
      assertEquals(22.0 / 7.0, results[0].asF64(), 1e-10,
          "22.0 / 7.0 should be approximately pi");
      LOGGER.info("[" + runtime + "] callUnchecked f64 result: " + results[0].asF64());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callUnchecked multi-return with (i32, i64)")
  void callUncheckedMultiReturn(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callUnchecked multi-return");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmFunction> funcOpt = instance.getFunction("multi_return");
      assert funcOpt.isPresent() : "multi_return export must be present";
      final WasmFunction multiReturn = funcOpt.get();

      final ValRaw[] results =
          multiReturn.callUnchecked(ValRaw.i32(42), ValRaw.i64(9999L));

      assertNotNull(results, "Results should not be null");
      assertEquals(2, results.length, "Should have exactly 2 results");
      assertEquals(42, results[0].asI32(), "First result should be 42");
      assertEquals(9999L, results[1].asI64(), "Second result should be 9999");
      LOGGER.info("[" + runtime + "] callUnchecked multi-return: "
          + results[0].asI32() + ", " + results[1].asI64());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callUnchecked no params on nop function")
  void callUncheckedNoParams(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callUnchecked no params");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      final Optional<WasmFunction> funcOpt = instance.getFunction("nop");
      assert funcOpt.isPresent() : "nop export must be present";
      final WasmFunction nop = funcOpt.get();

      final ValRaw[] results = nop.callUnchecked();

      assertNotNull(results, "Results should not be null");
      assertEquals(0, results.length, "nop should have 0 results");
      LOGGER.info("[" + runtime + "] callUnchecked nop returned " + results.length + " results");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ValRaw.fromWasmValue identity for i32")
  void valRawFromWasmValueIdentityI32(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ValRaw.fromWasmValue identity for i32");

    final WasmValue original = WasmValue.i32(42);
    final ValRaw raw = ValRaw.fromWasmValue(original);
    final WasmValue roundTripped = raw.toWasmValue(WasmValueType.I32);

    assertEquals(42, roundTripped.asInt(), "Round-tripped i32 should be 42");
    assertEquals(42, raw.asI32(), "Raw i32 should be 42");
    LOGGER.info("[" + runtime + "] ValRaw i32 round-trip: " + roundTripped.asInt());
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ValRaw.fromWasmValue identity for i64")
  void valRawFromWasmValueIdentityI64(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ValRaw.fromWasmValue identity for i64");

    final WasmValue original = WasmValue.i64(Long.MIN_VALUE);
    final ValRaw raw = ValRaw.fromWasmValue(original);
    final WasmValue roundTripped = raw.toWasmValue(WasmValueType.I64);

    assertEquals(Long.MIN_VALUE, roundTripped.asLong(),
        "Round-tripped i64 should be Long.MIN_VALUE");
    assertEquals(Long.MIN_VALUE, raw.asI64(), "Raw i64 should be Long.MIN_VALUE");
    LOGGER.info("[" + runtime + "] ValRaw i64 round-trip: " + roundTripped.asLong());
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ValRaw.fromWasmValue identity for f32")
  void valRawFromWasmValueIdentityF32(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ValRaw.fromWasmValue identity for f32");

    final WasmValue original = WasmValue.f32(3.14f);
    final ValRaw raw = ValRaw.fromWasmValue(original);
    final WasmValue roundTripped = raw.toWasmValue(WasmValueType.F32);

    assertEquals(3.14f, roundTripped.asFloat(), 0.001f,
        "Round-tripped f32 should be 3.14");
    assertEquals(3.14f, raw.asF32(), 0.001f, "Raw f32 should be 3.14");
    LOGGER.info("[" + runtime + "] ValRaw f32 round-trip: " + roundTripped.asFloat());
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ValRaw.fromWasmValue identity for f64")
  void valRawFromWasmValueIdentityF64(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ValRaw.fromWasmValue identity for f64");

    final WasmValue original = WasmValue.f64(2.718281828);
    final ValRaw raw = ValRaw.fromWasmValue(original);
    final WasmValue roundTripped = raw.toWasmValue(WasmValueType.F64);

    assertEquals(2.718281828, roundTripped.asDouble(), 1e-9,
        "Round-tripped f64 should be e");
    assertEquals(2.718281828, raw.asF64(), 1e-9, "Raw f64 should be e");
    LOGGER.info("[" + runtime + "] ValRaw f64 round-trip: " + roundTripped.asDouble());
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ValRaw v128 creation with low and high bits")
  void valRawV128Creation(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ValRaw v128 creation");

    final long low = 0xDEAD_BEEF_CAFE_BABEL;
    final long high = 0x0123_4567_89AB_CDEFL;
    final ValRaw v128 = ValRaw.v128(low, high);

    assertEquals(low, v128.asV128Low(), "Low bits should match");
    assertEquals(high, v128.asV128High(), "High bits should match");
    assertEquals(low, v128.getLowBits(), "getLowBits should match low");
    assertEquals(high, v128.getHighBits(), "getHighBits should match high");
    LOGGER.info("[" + runtime + "] ValRaw v128 low=0x"
        + Long.toHexString(v128.asV128Low()) + " high=0x"
        + Long.toHexString(v128.asV128High()));
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ValRaw null funcref and externref factory methods")
  void valRawNullFuncrefAndExternref(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ValRaw null funcref and externref");

    final ValRaw nullFunc = ValRaw.nullFuncref();
    final ValRaw nullExtern = ValRaw.nullExternref();

    assertNotNull(nullFunc, "nullFuncref should not return null");
    assertNotNull(nullExtern, "nullExternref should not return null");
    LOGGER.info("[" + runtime + "] nullFuncref lowBits=" + nullFunc.getLowBits()
        + " nullExternref lowBits=" + nullExtern.getLowBits());
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("ValRaw equals and hashCode contract")
  void valRawEqualsAndHashCode(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing ValRaw equals and hashCode");

    final ValRaw a = ValRaw.i32(42);
    final ValRaw b = ValRaw.i32(42);
    final ValRaw c = ValRaw.i32(99);

    assertEquals(a, b, "ValRaw with same i32 value should be equal");
    assertEquals(a.hashCode(), b.hashCode(),
        "Equal ValRaw should have same hashCode");
    assertNotEquals(a, c, "ValRaw with different i32 values should not be equal");

    final ValRaw d = ValRaw.i64(42L);
    final ValRaw e = ValRaw.i64(42L);
    assertEquals(d, e, "ValRaw with same i64 value should be equal");
    assertEquals(d.hashCode(), e.hashCode(),
        "Equal ValRaw i64 should have same hashCode");

    LOGGER.info("[" + runtime + "] ValRaw equals/hashCode contract verified");
  }
}
