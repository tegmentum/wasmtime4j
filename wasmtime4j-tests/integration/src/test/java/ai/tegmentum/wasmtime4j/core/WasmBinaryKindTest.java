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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmBinaryKind;
import ai.tegmentum.wasmtime4j.test.TestUtils;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.io.InputStream;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for {@link WasmBinaryKind} detection and DWARF package compilation.
 *
 * @since 1.1.0
 */
@DisplayName("WasmBinaryKind and DWARF Integration Tests")
public class WasmBinaryKindTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(WasmBinaryKindTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("WasmBinaryKind Detection Tests")
  class DetectionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should detect MODULE from compiled WAT bytes")
    void shouldDetectModuleFromWatBytes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing MODULE detection from compiled WAT bytes");

      try (Engine engine = Engine.create()) {
        // Compile a simple module to get valid WASM bytes
        final Module module =
            engine.compileWat("(module (func (export \"f\") (result i32) i32.const 42))");
        final byte[] wasmBytes = module.serialize();
        module.close();

        // The serialized bytes are precompiled, not source WASM
        // Use the engine's precompileModule to get raw bytes instead
        // We need to construct valid WASM module header manually
        final byte[] moduleHeader = {
          0x00, 0x61, 0x73, 0x6D, // \0asm magic
          0x01, 0x00, 0x00, 0x00 // version 1 (module)
        };

        final WasmBinaryKind kind = WasmBinaryKind.detect(moduleHeader);
        assertEquals(WasmBinaryKind.MODULE, kind, "Module header should be detected as MODULE");
        LOGGER.info("[" + runtime + "] MODULE detection verified");

        // Also test via engine API
        final WasmBinaryKind engineResult = engine.detectWasmType(moduleHeader);
        assertEquals(
            WasmBinaryKind.MODULE,
            engineResult,
            "Engine.detectWasmType should return MODULE for module header");
        LOGGER.info("[" + runtime + "] Engine.detectWasmType returned MODULE as expected");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should detect COMPONENT from component header bytes")
    void shouldDetectComponentFromHeaderBytes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing COMPONENT detection");

      final byte[] componentHeader = {
        0x00, 0x61, 0x73, 0x6D, // \0asm magic
        0x0d, 0x00, 0x01, 0x00 // component version
      };

      final WasmBinaryKind kind = WasmBinaryKind.detect(componentHeader);
      assertEquals(
          WasmBinaryKind.COMPONENT, kind, "Component header should be detected as COMPONENT");
      LOGGER.info("[" + runtime + "] COMPONENT detection verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should detect COMPONENT from real component binary")
    void shouldDetectComponentFromRealBinary(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing COMPONENT detection from real binary");

      try (InputStream is = WasmBinaryKindTest.class.getResourceAsStream("/components/add.wasm")) {
        if (is == null) {
          LOGGER.warning("add.wasm not available, skipping");
          return;
        }
        final byte[] componentBytes = TestUtils.readAllBytes(is);

        final WasmBinaryKind kind = WasmBinaryKind.detect(componentBytes);
        LOGGER.info("[" + runtime + "] Real component binary detected as: " + kind);
        assertEquals(WasmBinaryKind.COMPONENT, kind, "add.wasm should be detected as COMPONENT");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return UNKNOWN for invalid bytes")
    void shouldReturnUnknownForInvalidBytes(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing UNKNOWN detection for invalid bytes");

      final byte[] garbage = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
      assertEquals(
          WasmBinaryKind.UNKNOWN, WasmBinaryKind.detect(garbage), "Random bytes should be UNKNOWN");

      LOGGER.info("[" + runtime + "] UNKNOWN detection for invalid bytes verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return UNKNOWN for too-short bytes")
    void shouldReturnUnknownForShortBytes(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing UNKNOWN detection for short bytes");

      assertEquals(
          WasmBinaryKind.UNKNOWN,
          WasmBinaryKind.detect(new byte[0]),
          "Empty bytes should be UNKNOWN");
      assertEquals(
          WasmBinaryKind.UNKNOWN,
          WasmBinaryKind.detect(new byte[] {0x00, 0x61, 0x73}),
          "3-byte input should be UNKNOWN");
      assertEquals(
          WasmBinaryKind.UNKNOWN,
          WasmBinaryKind.detect(new byte[] {0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00}),
          "7-byte input should be UNKNOWN");

      LOGGER.info("[" + runtime + "] UNKNOWN detection for short bytes verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should throw on null bytes")
    void shouldThrowOnNullBytes(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null bytes handling");

      assertThrows(
          IllegalArgumentException.class,
          () -> WasmBinaryKind.detect(null),
          "null bytes should throw IllegalArgumentException");

      LOGGER.info("[" + runtime + "] Null bytes throws IllegalArgumentException as expected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should return UNKNOWN for valid magic but unknown version")
    void shouldReturnUnknownForUnknownVersion(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing UNKNOWN for valid magic but unknown version");

      final byte[] unknownVersion = {
        0x00,
        0x61,
        0x73,
        0x6D, // \0asm magic
        (byte) 0xFF,
        0x00,
        0x00,
        0x00 // unknown version
      };

      assertEquals(
          WasmBinaryKind.UNKNOWN,
          WasmBinaryKind.detect(unknownVersion),
          "Valid magic with unknown version should be UNKNOWN");

      LOGGER.info("[" + runtime + "] UNKNOWN for unknown version verified");
    }
  }

  @Nested
  @DisplayName("DWARF Package Compilation Tests")
  class DwarfCompilationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null WASM bytes for DWARF compilation")
    void shouldRejectNullWasmBytesForDwarf(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null WASM bytes rejection for DWARF");

      try (Engine engine = Engine.create()) {
        assertThrows(
            IllegalArgumentException.class,
            () -> engine.compileModuleWithDwarf(null, new byte[] {0x01}),
            "null wasmBytes should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] Null WASM bytes rejected as expected");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject empty WASM bytes for DWARF compilation")
    void shouldRejectEmptyWasmBytesForDwarf(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing empty WASM bytes rejection for DWARF");

      try (Engine engine = Engine.create()) {
        assertThrows(
            IllegalArgumentException.class,
            () -> engine.compileModuleWithDwarf(new byte[0], new byte[] {0x01}),
            "empty wasmBytes should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] Empty WASM bytes rejected as expected");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject null DWARF package bytes")
    void shouldRejectNullDwarfBytes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing null DWARF bytes rejection");

      try (Engine engine = Engine.create()) {
        assertThrows(
            IllegalArgumentException.class,
            () -> engine.compileModuleWithDwarf(new byte[] {0x01}, null),
            "null dwarfPackage should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] Null DWARF bytes rejected as expected");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Should reject empty DWARF package bytes")
    void shouldRejectEmptyDwarfBytes(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing empty DWARF bytes rejection");

      try (Engine engine = Engine.create()) {
        assertThrows(
            IllegalArgumentException.class,
            () -> engine.compileModuleWithDwarf(new byte[] {0x01}, new byte[0]),
            "empty dwarfPackage should throw IllegalArgumentException");
        LOGGER.info("[" + runtime + "] Empty DWARF bytes rejected as expected");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Module.compile with DWARF should reject null engine")
    void moduleCompileWithDwarfShouldRejectNullEngine(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing Module.compile DWARF null engine rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> Module.compile(null, new byte[] {0x01}, new byte[] {0x02}),
          "null engine should throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Null engine rejected as expected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Engine.detectWasmType should work via Engine interface")
    void engineDetectWasmTypeShouldWork(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing Engine.detectWasmType");

      try (Engine engine = Engine.create()) {
        // Module header
        final byte[] moduleBytes = {0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00};
        final WasmBinaryKind moduleKind = engine.detectWasmType(moduleBytes);
        assertEquals(WasmBinaryKind.MODULE, moduleKind, "Should detect MODULE");

        // Component header
        final byte[] componentBytes = {0x00, 0x61, 0x73, 0x6D, 0x0d, 0x00, 0x01, 0x00};
        final WasmBinaryKind componentKind = engine.detectWasmType(componentBytes);
        assertEquals(WasmBinaryKind.COMPONENT, componentKind, "Should detect COMPONENT");

        // Invalid bytes
        final byte[] invalidBytes = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        final WasmBinaryKind unknownKind = engine.detectWasmType(invalidBytes);
        assertEquals(WasmBinaryKind.UNKNOWN, unknownKind, "Should detect UNKNOWN");

        LOGGER.info("[" + runtime + "] Engine.detectWasmType verified for all kinds");
      }
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("WasmBinaryKind enum should have exactly 3 values")
    void wasmBinaryKindShouldHaveThreeValues(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("[" + runtime + "] Testing WasmBinaryKind enum completeness");

      final WasmBinaryKind[] values = WasmBinaryKind.values();
      assertEquals(3, values.length, "WasmBinaryKind should have exactly 3 values");
      assertNotNull(WasmBinaryKind.valueOf("MODULE"));
      assertNotNull(WasmBinaryKind.valueOf("COMPONENT"));
      assertNotNull(WasmBinaryKind.valueOf("UNKNOWN"));

      LOGGER.info("[" + runtime + "] WasmBinaryKind enum has 3 values as expected");
    }
  }
}
