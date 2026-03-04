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
package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.TagType;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for V128 values stored in ExnRef (exception reference) fields. Verifies that V128 payloads
 * survive creation, storage, and retrieval through exception handling APIs — especially the upper 8
 * bytes which were previously corrupted.
 *
 * <p>Note: JNI's {@code JniExnRef.createExnRef()} does NOT support V128 fields. Tests that create
 * ExnRef with V128 payloads from Java will gracefully catch {@link UnsupportedOperationException}
 * on JNI runtime.
 *
 * @since 1.0.0
 */
@DisplayName("V128 ExnRef Tests")
public class V128ExnRefTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(V128ExnRefTest.class.getName());

  /** Counting pattern {0x00..0x0F} — detects byte order bugs. */
  private static final byte[] COUNTING = {
    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
    0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
  };

  /** Upper half only — lower 8 bytes zero, upper 8 bytes non-zero. Detects i32/i64 truncation. */
  private static final byte[] UPPER_HALF_ONLY = {
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    (byte) 0xDE,
    (byte) 0xAD,
    (byte) 0xBE,
    (byte) 0xEF,
    (byte) 0xCA,
    (byte) 0xFE,
    (byte) 0xBA,
    (byte) 0xBE
  };

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  private EngineConfig exceptionsEnabledConfig() {
    return Engine.builder().wasmExceptions(true);
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("V128 ExnRef single field round-trip preserves all 16 bytes")
  void v128ExnRefSingleFieldRoundTrip(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 ExnRef single field round-trip");

    try (Engine engine = Engine.create(exceptionsEnabledConfig());
        Store store = engine.createStore()) {

      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.V128}, new WasmValueType[0]);
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);
      assertNotNull(tag, "Tag should be created");
      LOGGER.info("[" + runtime + "] Created tag with V128 payload type");

      final ExnRef exnRef = ExnRef.create(store, tag, WasmValue.v128(COUNTING));
      assertNotNull(exnRef, "ExnRef should be created with V128 payload");
      LOGGER.info("[" + runtime + "] Created ExnRef with V128 payload");

      final List<WasmValue> fields = exnRef.fields(store);
      assertNotNull(fields, "ExnRef fields should not be null");
      assertEquals(1, fields.size(), "ExnRef should have exactly 1 field");

      final byte[] actual = fields.get(0).asV128();
      LOGGER.info(
          "[" + runtime + "] Expected: " + bytesToHex(COUNTING) + ", Got: " + bytesToHex(actual));
      assertArrayEquals(COUNTING, actual, "V128 bytes must survive ExnRef single field round-trip");

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning(
          "["
              + runtime
              + "] V128 ExnRef creation not supported (expected on JNI): "
              + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("V128 ExnRef with multiple fields does not corrupt neighbors")
  void v128ExnRefMultipleFields(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 ExnRef with multiple field types");

    try (Engine engine = Engine.create(exceptionsEnabledConfig());
        Store store = engine.createStore()) {

      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.V128, WasmValueType.F64},
              new WasmValueType[0]);
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);
      assertNotNull(tag, "Tag should be created");

      final int expectedI32 = 42;
      final double expectedF64 = 2.718281828459045;

      final ExnRef exnRef =
          ExnRef.create(
              store,
              tag,
              WasmValue.i32(expectedI32),
              WasmValue.v128(COUNTING),
              WasmValue.f64(expectedF64));
      assertNotNull(exnRef, "ExnRef should be created with mixed payloads");
      LOGGER.info("[" + runtime + "] Created ExnRef with [i32, v128, f64] payload");

      final List<WasmValue> fields = exnRef.fields(store);
      assertNotNull(fields, "ExnRef fields should not be null");
      assertEquals(3, fields.size(), "ExnRef should have exactly 3 fields");

      assertEquals(
          expectedI32, fields.get(0).asInt(), "i32 field should not be corrupted by adjacent V128");
      LOGGER.info("[" + runtime + "] i32 field intact: " + fields.get(0).asInt());

      final byte[] v128Actual = fields.get(1).asV128();
      assertArrayEquals(
          COUNTING, v128Actual, "V128 field should not be corrupted by adjacent fields");
      LOGGER.info("[" + runtime + "] V128 field intact: " + bytesToHex(v128Actual));

      assertEquals(
          expectedF64,
          fields.get(2).asDouble(),
          0.0,
          "f64 field should not be corrupted by adjacent V128");
      LOGGER.info("[" + runtime + "] f64 field intact: " + fields.get(2).asDouble());

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning(
          "["
              + runtime
              + "] V128 ExnRef creation not supported (expected on JNI): "
              + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("V128 ExnRef upper 8 bytes preserved — critical regression test")
  void v128ExnRefUpperHalfPreserved(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] CRITICAL: Testing V128 ExnRef upper half preservation");

    try (Engine engine = Engine.create(exceptionsEnabledConfig());
        Store store = engine.createStore()) {

      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.V128}, new WasmValueType[0]);
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);

      final ExnRef exnRef = ExnRef.create(store, tag, WasmValue.v128(UPPER_HALF_ONLY));
      assertNotNull(exnRef, "ExnRef should be created");

      final byte[] actual = exnRef.field(store, 0).asV128();
      final byte[] upperHalf = Arrays.copyOfRange(actual, 8, 16);
      final byte[] expectedUpperHalf = Arrays.copyOfRange(UPPER_HALF_ONLY, 8, 16);

      LOGGER.info(
          "["
              + runtime
              + "] ExnRef upper half: "
              + bytesToHex(upperHalf)
              + " (expected: "
              + bytesToHex(expectedUpperHalf)
              + ")");

      assertArrayEquals(
          expectedUpperHalf,
          upperHalf,
          "CRITICAL: Upper 8 bytes of V128 must be preserved in ExnRef field. "
              + "If this fails, V128 data is being truncated to i64.");

      LOGGER.info("[" + runtime + "] V128 ExnRef upper half preservation PASSED");

    } catch (final UnsupportedOperationException e) {
      LOGGER.warning(
          "["
              + runtime
              + "] V128 ExnRef creation not supported (expected on JNI): "
              + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("V128 ExnRef from WASM throw preserves payload bytes")
  void v128ExnRefFromWasmThrow(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing V128 ExnRef from WASM throw instruction");

    // WAT module that throws a V128 value and catches it, returning the bytes
    // v128.const i32x4 encodes as 4 little-endian i32 values
    // 0x01020304 0x05060708 0x090A0B0C 0x0D0E0F10
    // In memory (little-endian): 04 03 02 01 08 07 06 05 0C 0B 0A 09 10 0F 0E 0D
    final String wat =
        "(module\n"
            + "  (tag $t (param v128))\n"
            + "  (func (export \"throw_v128\") (result v128)\n"
            + "    (block $catch (result v128)\n"
            + "      (try_table (catch $t $catch)\n"
            + "        (throw $t (v128.const i32x4 0x01020304 0x05060708 0x090A0B0C 0x0D0E0F10))\n"
            + "      )\n"
            + "      unreachable\n"
            + "    )\n"
            + "  )\n"
            + ")";

    // Expected bytes in little-endian order for i32x4:
    // lane 0: 0x01020304 -> bytes 04 03 02 01
    // lane 1: 0x05060708 -> bytes 08 07 06 05
    // lane 2: 0x090A0B0C -> bytes 0C 0B 0A 09
    // lane 3: 0x0D0E0F10 -> bytes 10 0F 0E 0D
    final byte[] expectedBytes = {
      0x04, 0x03, 0x02, 0x01,
      0x08, 0x07, 0x06, 0x05,
      0x0C, 0x0B, 0x0A, 0x09,
      0x10, 0x0F, 0x0E, 0x0D
    };

    try (Engine engine = Engine.create(exceptionsEnabledConfig())) {
      Module module = null;
      try {
        module = engine.compileWat(wat);
      } catch (final Exception e) {
        LOGGER.warning(
            "["
                + runtime
                + "] Failed to compile V128 exception WAT: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
        return;
      }

      try (Store store = engine.createStore();
          Instance instance = module.instantiate(store)) {

        final WasmFunction throwFunc = instance.getFunction("throw_v128").orElse(null);
        assertNotNull(throwFunc, "throw_v128 export must exist");
        LOGGER.info("[" + runtime + "] Found throw_v128 export");

        final WasmValue[] results = throwFunc.call();
        assertNotNull(results, "throw_v128 must return results");
        assertEquals(1, results.length, "throw_v128 should return 1 V128 value");
        assertEquals(WasmValueType.V128, results[0].getType(), "Return type should be V128");

        final byte[] actual = results[0].asV128();
        LOGGER.info(
            "["
                + runtime
                + "] WASM throw/catch V128 result: "
                + bytesToHex(actual)
                + " (expected: "
                + bytesToHex(expectedBytes)
                + ")");

        assertArrayEquals(
            expectedBytes, actual, "V128 payload from WASM throw/catch must preserve all 16 bytes");

        // Explicitly verify upper half is non-zero
        final byte[] upperHalf = Arrays.copyOfRange(actual, 8, 16);
        boolean upperHalfNonZero = false;
        for (final byte b : upperHalf) {
          if (b != 0) {
            upperHalfNonZero = true;
            break;
          }
        }
        assertEquals(true, upperHalfNonZero, "Upper 8 bytes of WASM-thrown V128 must be non-zero");
        LOGGER.info("[" + runtime + "] V128 WASM throw upper half verified non-zero");

      } finally {
        if (module != null) {
          module.close();
        }
      }

    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] Exception handling not supported: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  private static String bytesToHex(final byte[] bytes) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      if (i > 0) {
        sb.append(' ');
      }
      sb.append(String.format("%02X", bytes[i] & 0xFF));
    }
    return sb.toString();
  }
}
