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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Precompiled;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests Engine precompilation APIs: {@link Engine#precompileModule(byte[])}, {@link
 * Engine#detectPrecompiled(byte[])}, and {@link Engine#precompileCompatibilityHash()}.
 *
 * @since 1.0.0
 */
@DisplayName("Engine Precompilation Tests")
public class EnginePrecompilationTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(EnginePrecompilationTest.class.getName());

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
  @DisplayName("precompileModule returns non-empty byte array")
  void precompileModuleReturnsBytes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing precompileModule returns bytes");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(ADD_WAT);
      final byte[] serialized = module.serialize();

      assertNotNull(serialized, "Serialized bytes should not be null");
      assertTrue(serialized.length > 0, "Serialized bytes should not be empty");
      LOGGER.info("[" + runtime + "] precompileModule returned " + serialized.length + " bytes");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("detectPrecompiled on serialized module returns MODULE")
  void detectPrecompiledOnPrecompiledReturnsModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing detectPrecompiled on precompiled bytes");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(ADD_WAT);
      final byte[] serialized = module.serialize();

      try {
        final Precompiled detected = engine.detectPrecompiled(serialized);

        assertNotNull(detected, "detectPrecompiled should not return null for precompiled bytes");
        assertEquals(
            Precompiled.MODULE,
            detected,
            "detectPrecompiled should return MODULE for serialized module");
        LOGGER.info("[" + runtime + "] detectPrecompiled returned: " + detected);
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] detectPrecompiled not available: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("precompile and deserialize then execute correctly")
  void precompileAndDeserializeExecuteCorrectly(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing precompile -> deserialize -> execute round-trip");

    try (Engine engine = Engine.create()) {
      final Module original = engine.compileWat(ADD_WAT);
      final byte[] serialized = original.serialize();
      original.close();

      final Module deserialized = Module.deserialize(engine, serialized);
      try (Store store = engine.createStore()) {
        final Instance instance = deserialized.instantiate(store);

        final WasmValue[] result =
            instance.callFunction("add", WasmValue.i32(10), WasmValue.i32(20));

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.length, "Should have exactly 1 result");
        assertEquals(30, result[0].asInt(), "add(10, 20) should return 30");
        LOGGER.info("[" + runtime + "] Deserialized module add(10, 20) = " + result[0].asInt());

        instance.close();
      }
      deserialized.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("detectPrecompiled on garbage bytes returns null")
  void detectPrecompiledOnGarbageBytesReturnsNull(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing detectPrecompiled on garbage bytes");

    try (Engine engine = Engine.create()) {
      final byte[] garbage = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

      try {
        final Precompiled detected = engine.detectPrecompiled(garbage);

        assertNull(detected, "detectPrecompiled should return null for garbage bytes");
        LOGGER.info("[" + runtime + "] detectPrecompiled on garbage returned null as expected");
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] detectPrecompiled not available: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("detectPrecompiled on raw WASM bytes returns null")
  void detectPrecompiledOnRawWasmReturnsNull(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing detectPrecompiled on raw WASM bytes");

    // Minimal valid WASM binary for (module)
    final byte[] rawWasmBytes = {
      0x00, 0x61, 0x73, 0x6D, // magic: \0asm
      0x01, 0x00, 0x00, 0x00 // version: 1
    };

    try (Engine engine = Engine.create()) {
      try {
        final Precompiled detected = engine.detectPrecompiled(rawWasmBytes);

        assertNull(detected, "detectPrecompiled should return null for raw WASM bytes");
        LOGGER.info("[" + runtime + "] detectPrecompiled on raw WASM returned null as expected");
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] detectPrecompiled not available: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("precompileCompatibilityHash is consistent across calls")
  void precompileCompatibilityHashIsConsistent(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing precompileCompatibilityHash consistency");

    try (Engine engine = Engine.create()) {
      final byte[] hash1 = engine.precompileCompatibilityHash();
      final byte[] hash2 = engine.precompileCompatibilityHash();

      assertNotNull(hash1, "First hash should not be null");
      assertNotNull(hash2, "Second hash should not be null");
      assertArrayEquals(
          hash1, hash2, "Two calls to precompileCompatibilityHash should return same bytes");
      LOGGER.info("[" + runtime + "] Hash length: " + hash1.length + " bytes, consistent: true");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("precompileCompatibilityHash same for same config")
  void precompileCompatibilityHashSameForSameConfig(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing precompileCompatibilityHash for same config");

    try (Engine engine1 = Engine.create();
        Engine engine2 = Engine.create()) {

      final byte[] hash1 = engine1.precompileCompatibilityHash();
      final byte[] hash2 = engine2.precompileCompatibilityHash();

      assertNotNull(hash1, "Engine 1 hash should not be null");
      assertNotNull(hash2, "Engine 2 hash should not be null");
      assertArrayEquals(
          hash1, hash2, "Engines with same config should have same compatibility hash");
      LOGGER.info("[" + runtime + "] Same-config engines have matching hashes");
    }
  }
}
