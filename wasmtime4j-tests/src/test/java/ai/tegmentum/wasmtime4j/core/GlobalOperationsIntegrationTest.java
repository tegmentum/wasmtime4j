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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.CodeBuilder;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FuncType;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WebAssembly global variable operations.
 *
 * <p>These tests verify global variable creation, type checking, mutability, and access from both
 * host and WebAssembly contexts.
 *
 * @since 1.0.0
 */
@DisplayName("Global Operations Integration Tests")
public final class GlobalOperationsIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(GlobalOperationsIntegrationTest.class.getName());

  /** Helper method to create a FuncType without a factory method. */
  private static FuncType funcType(
      final List<WasmValueType> params, final List<WasmValueType> results) {
    return new FuncType() {
      @Override
      public List<WasmValueType> getParams() {
        return Collections.unmodifiableList(params);
      }

      @Override
      public List<WasmValueType> getResults() {
        return Collections.unmodifiableList(results);
      }
    };
  }

  /**
   * Creates a WebAssembly module with a mutable i32 global and get/set/inc functions.
   *
   * <pre>
   * (module
   *   (global $counter (export "counter") (mut i32) (i32.const 0))
   *   (func (export "get") (result i32) global.get $counter)
   *   (func (export "set") (param i32) local.get 0 global.set $counter)
   *   (func (export "inc") global.get $counter i32.const 1 i32.add global.set $counter))
   * </pre>
   */
  private static byte[] createMutableGlobalModule() throws WasmException {
    return new CodeBuilder()
        .addType(funcType(List.of(), List.of(WasmValueType.I32))) // type 0: () -> i32
        .addType(funcType(List.of(WasmValueType.I32), List.of())) // type 1: (i32) -> ()
        .addType(funcType(List.of(), List.of())) // type 2: () -> ()
        .addGlobal(WasmValueType.I32, true, 0) // global 0: mut i32 = 0
        .addFunction(0, List.of(), new byte[] {0x23, 0x00}) // get: global.get 0
        .addFunction(
            1, List.of(), new byte[] {0x20, 0x00, 0x24, 0x00}) // set: local.get 0, global.set 0
        .addFunction(
            2,
            List.of(),
            new byte[] {
              0x23, 0x00, 0x41, 0x01, 0x6a, 0x24, 0x00
            }) // inc: global.get 0, i32.const 1, i32.add, global.set 0
        .addExport("counter", CodeBuilder.ExportKind.GLOBAL, 0)
        .addExport("get", CodeBuilder.ExportKind.FUNCTION, 0)
        .addExport("set", CodeBuilder.ExportKind.FUNCTION, 1)
        .addExport("inc", CodeBuilder.ExportKind.FUNCTION, 2)
        .build();
  }

  /**
   * Creates a WebAssembly module with an immutable i32 global.
   *
   * <pre>
   * (module
   *   (global $value (export "value") i32 (i32.const 42))
   *   (func (export "get") (result i32) global.get $value))
   * </pre>
   */
  private static byte[] createImmutableGlobalModule() throws WasmException {
    return new CodeBuilder()
        .addType(funcType(List.of(), List.of(WasmValueType.I32))) // type 0: () -> i32
        .addGlobal(WasmValueType.I32, false, 42) // global 0: i32 = 42
        .addFunction(0, List.of(), new byte[] {0x23, 0x00}) // get: global.get 0
        .addExport("value", CodeBuilder.ExportKind.GLOBAL, 0)
        .addExport("get", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  @Nested
  @DisplayName("Mutable Global Tests")
  class MutableGlobalTests {

    @Test
    @DisplayName("should read and write mutable global from WASM")
    void shouldReadAndWriteMutableGlobalFromWasm() throws Exception {
      LOGGER.info("Testing mutable global read/write via WASM functions");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileModule(createMutableGlobalModule());
        final Instance instance = store.createInstance(module);

        final Optional<WasmFunction> getFunc = instance.getFunction("get");
        final Optional<WasmFunction> setFunc = instance.getFunction("set");
        final Optional<WasmFunction> incFunc = instance.getFunction("inc");

        assertTrue(getFunc.isPresent());
        assertTrue(setFunc.isPresent());
        assertTrue(incFunc.isPresent());

        // Initial value
        assertEquals(0, getFunc.get().call()[0].asInt());

        // Set and verify
        setFunc.get().call(WasmValue.i32(42));
        assertEquals(42, getFunc.get().call()[0].asInt());

        // Increment and verify
        incFunc.get().call();
        assertEquals(43, getFunc.get().call()[0].asInt());

        LOGGER.info("Mutable global WASM access verified");
      }
    }

    @Test
    @DisplayName("should access mutable global from host")
    void shouldAccessMutableGlobalFromHost() throws Exception {
      LOGGER.info("Testing mutable global access from host");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileModule(createMutableGlobalModule());
        final Instance instance = store.createInstance(module);

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("counter");
        assertTrue(globalOpt.isPresent(), "counter global should be exported");

        final WasmGlobal global = globalOpt.get();
        assertTrue(global.isMutable(), "Global should be mutable");
        assertEquals(WasmValueType.I32, global.getType());

        // Read initial value
        assertEquals(0, global.get().asI32());

        // Write from host
        global.set(WasmValue.i32(100));
        assertEquals(100, global.get().asI32());

        // Verify WASM sees the change
        final Optional<WasmFunction> getFunc = instance.getFunction("get");
        assertEquals(100, getFunc.get().call()[0].asInt());

        LOGGER.info("Host global access verified");
      }
    }

    @Test
    @DisplayName("should create mutable global from host")
    void shouldCreateMutableGlobalFromHost() throws Exception {
      LOGGER.info("Testing host-created mutable global");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(999));

        assertNotNull(global);
        assertTrue(global.isMutable());
        assertEquals(999, global.get().asI32());

        global.set(WasmValue.i32(1000));
        assertEquals(1000, global.get().asI32());

        LOGGER.info("Host-created mutable global verified");
      }
    }
  }

  @Nested
  @DisplayName("Immutable Global Tests")
  class ImmutableGlobalTests {

    @Test
    @DisplayName("should read immutable global")
    void shouldReadImmutableGlobal() throws Exception {
      LOGGER.info("Testing immutable global read");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileModule(createImmutableGlobalModule());
        final Instance instance = store.createInstance(module);

        final Optional<WasmGlobal> globalOpt = instance.getGlobal("value");
        assertTrue(globalOpt.isPresent());

        final WasmGlobal global = globalOpt.get();
        assertFalse(global.isMutable(), "Global should be immutable");
        assertEquals(42, global.get().asI32());

        LOGGER.info("Immutable global read verified");
      }
    }

    @Test
    @DisplayName("should create immutable global from host")
    void shouldCreateImmutableGlobalFromHost() throws Exception {
      LOGGER.info("Testing host-created immutable global");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final WasmGlobal global =
            store.createGlobal(WasmValueType.I64, false, WasmValue.i64(12345L));

        assertNotNull(global);
        assertFalse(global.isMutable());
        assertEquals(12345L, global.get().asI64());

        LOGGER.info("Host-created immutable global verified");
      }
    }
  }

  @Nested
  @DisplayName("Multi-Type Global Tests")
  class MultiTypeGlobalTests {

    @Test
    @DisplayName("should handle i32 globals")
    void shouldHandleI32Globals() throws Exception {
      LOGGER.info("Testing i32 global");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final WasmGlobal global = store.createGlobal(WasmValueType.I32, true, WasmValue.i32(42));
        assertEquals(42, global.get().asI32());

        global.set(WasmValue.i32(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, global.get().asI32());

        global.set(WasmValue.i32(Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, global.get().asI32());

        LOGGER.info("i32 global verified");
      }
    }

    @Test
    @DisplayName("should handle i64 globals")
    void shouldHandleI64Globals() throws Exception {
      LOGGER.info("Testing i64 global");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final WasmGlobal global =
            store.createGlobal(WasmValueType.I64, true, WasmValue.i64(123456789L));
        assertEquals(123456789L, global.get().asI64());

        global.set(WasmValue.i64(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, global.get().asI64());

        global.set(WasmValue.i64(Long.MIN_VALUE));
        assertEquals(Long.MIN_VALUE, global.get().asI64());

        LOGGER.info("i64 global verified");
      }
    }

    @Test
    @DisplayName("should handle f32 globals")
    void shouldHandleF32Globals() throws Exception {
      LOGGER.info("Testing f32 global");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final WasmGlobal global = store.createGlobal(WasmValueType.F32, true, WasmValue.f32(3.14f));
        assertEquals(3.14f, global.get().asF32(), 0.001f);

        global.set(WasmValue.f32(Float.MAX_VALUE));
        assertEquals(Float.MAX_VALUE, global.get().asF32(), 0.001f);

        LOGGER.info("f32 global verified");
      }
    }

    @Test
    @DisplayName("should handle f64 globals")
    void shouldHandleF64Globals() throws Exception {
      LOGGER.info("Testing f64 global");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final WasmGlobal global =
            store.createGlobal(WasmValueType.F64, true, WasmValue.f64(2.718281828));
        assertEquals(2.718281828, global.get().asF64(), 0.0000001);

        global.set(WasmValue.f64(Double.MAX_VALUE));
        assertEquals(Double.MAX_VALUE, global.get().asF64(), 0.0000001);

        LOGGER.info("f64 global verified");
      }
    }
  }

  @Nested
  @DisplayName("Global Bidirectional Tests")
  class GlobalBidirectionalTests {

    @Test
    @DisplayName("should share global modifications between WASM and host")
    void shouldShareGlobalModificationsBetweenWasmAndHost() throws Exception {
      LOGGER.info("Testing bidirectional global modifications");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileModule(createMutableGlobalModule());
        final Instance instance = store.createInstance(module);

        final WasmGlobal global = instance.getGlobal("counter").get();
        final WasmFunction setFunc = instance.getFunction("set").get();
        final WasmFunction getFunc = instance.getFunction("get").get();

        // Host sets, WASM reads
        global.set(WasmValue.i32(50));
        assertEquals(50, getFunc.call()[0].asInt());

        // WASM sets, host reads
        setFunc.call(WasmValue.i32(75));
        assertEquals(75, global.get().asI32());

        // Interleaved modifications
        global.set(WasmValue.i32(100));
        assertEquals(100, getFunc.call()[0].asInt());
        setFunc.call(WasmValue.i32(200));
        assertEquals(200, global.get().asI32());

        LOGGER.info("Bidirectional modifications verified");
      }
    }
  }
}
