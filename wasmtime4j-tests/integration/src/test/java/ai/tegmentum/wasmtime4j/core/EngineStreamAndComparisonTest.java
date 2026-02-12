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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests Engine stream compilation and comparison APIs: {@link
 * Engine#compileFromStream(InputStream)}, {@link Engine#isPulley()}, and {@link
 * Engine#same(Engine)}.
 *
 * @since 1.0.0
 */
@DisplayName("Engine Stream Compilation and Comparison Tests")
public class EngineStreamAndComparisonTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(EngineStreamAndComparisonTest.class.getName());

  private static final String ADD_WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32)
          local.get 0 local.get 1 i32.add))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("compileFromStream produces working module")
  void compileFromStreamProducesWorkingModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing compileFromStream");

    // Minimal WASM binary for: (module (func (export "add") (param i32 i32) (result i32)
    //   local.get 0 local.get 1 i32.add))
    final byte[] wasmBytes = {
      0x00,
      0x61,
      0x73,
      0x6D, // magic: \0asm
      0x01,
      0x00,
      0x00,
      0x00, // version: 1
      // Type section (1): 1 type [i32, i32] -> [i32]
      0x01,
      0x07,
      0x01,
      0x60,
      0x02,
      0x7F,
      0x7F,
      0x01,
      0x7F,
      // Function section (3): 1 function, type index 0
      0x03,
      0x02,
      0x01,
      0x00,
      // Export section (7): export "add" as func 0
      0x07,
      0x07,
      0x01,
      0x03,
      0x61,
      0x64,
      0x64,
      0x00,
      0x00,
      // Code section (10): 1 function body
      0x0A,
      0x09,
      0x01, // section, size, count
      0x07,
      0x00, // body size, 0 locals
      0x20,
      0x00, // local.get 0
      0x20,
      0x01, // local.get 1
      0x6A, // i32.add
      0x0B // end
    };

    try (Engine engine = Engine.create()) {
      final InputStream stream = new ByteArrayInputStream(wasmBytes);
      final Module streamModule = engine.compileFromStream(stream);

      assertNotNull(streamModule, "Stream-compiled module should not be null");

      try (Store store = engine.createStore()) {
        final Instance instance = streamModule.instantiate(store);

        final WasmValue[] result = instance.callFunction("add", WasmValue.i32(5), WasmValue.i32(7));

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.length, "Should have exactly 1 result");
        assertEquals(12, result[0].asInt(), "add(5, 7) should return 12");
        LOGGER.info("[" + runtime + "] compileFromStream add(5, 7) = " + result[0].asInt());

        instance.close();
      }
      streamModule.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("compileFromStream with empty stream throws")
  void compileFromEmptyStreamThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing compileFromStream with empty stream");

    try (Engine engine = Engine.create()) {
      final InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

      assertThrows(
          Exception.class,
          () -> engine.compileFromStream(emptyStream),
          "compileFromStream with empty stream should throw");
      LOGGER.info("[" + runtime + "] compileFromStream with empty stream threw as expected");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("isPulley returns boolean without crash")
  void isPulleyReturnsBooleanWithoutCrash(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing isPulley");

    try (Engine engine = Engine.create()) {
      final boolean isPulley = engine.isPulley();

      LOGGER.info("[" + runtime + "] isPulley() = " + isPulley);
      // Just verify it returns without crash -- the value depends on configuration
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("same with self returns true")
  void sameWithSelfReturnsTrue(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing same with self");

    try (Engine engine = Engine.create()) {
      final boolean result = engine.same(engine);

      assertTrue(result, "engine.same(engine) should return true");
      LOGGER.info("[" + runtime + "] engine.same(engine) = " + result);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("same with different engine logs result")
  void sameWithDifferentEngineResult(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing same with different engine");

    try (Engine engine1 = Engine.create();
        Engine engine2 = Engine.create()) {

      final boolean result = engine1.same(engine2);

      LOGGER.info("[" + runtime + "] engine1.same(engine2) = " + result + " (two default engines)");
      // Two separately created engines may or may not be "same" depending on implementation
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("same with different config returns false")
  void sameWithDifferentConfigReturnsFalse(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing same with different config");

    final EngineConfig fuelConfig = Engine.builder().consumeFuel(true);
    try (Engine defaultEngine = Engine.create();
        Engine fuelEngine = Engine.create(fuelConfig)) {

      final boolean result = defaultEngine.same(fuelEngine);

      assertFalse(result, "Engines with different configs should not be 'same'");
      LOGGER.info("[" + runtime + "] defaultEngine.same(fuelEngine) = " + result);
    }
  }
}
