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

package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.FrameInfo;
import ai.tegmentum.wasmtime4j.FrameSymbol;
import ai.tegmentum.wasmtime4j.WasmBacktrace;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link BacktraceDeserializer} utility class.
 *
 * <p>This test class verifies the backtrace deserialization functionality.
 */
@DisplayName("BacktraceDeserializer Tests")
class BacktraceDeserializerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("BacktraceDeserializer should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(BacktraceDeserializer.class.getModifiers()),
          "BacktraceDeserializer should be final");
    }

    @Test
    @DisplayName("BacktraceDeserializer should have private constructor")
    void shouldHavePrivateConstructor() throws Exception {
      final var constructor = BacktraceDeserializer.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");
    }
  }

  @Nested
  @DisplayName("deserialize Validation Tests")
  class DeserializeValidationTests {

    @Test
    @DisplayName("deserialize should throw for null data")
    void deserializeShouldThrowForNullData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> BacktraceDeserializer.deserialize(null),
          "Should throw for null data");
    }

    @Test
    @DisplayName("deserialize should throw for empty data")
    void deserializeShouldThrowForEmptyData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> BacktraceDeserializer.deserialize(new byte[0]),
          "Should throw for empty data");
    }

    @Test
    @DisplayName("deserialize should throw for data too short")
    void deserializeShouldThrowForDataTooShort() {
      assertThrows(
          IllegalArgumentException.class,
          () -> BacktraceDeserializer.deserialize(new byte[] {1, 2, 3, 4}),
          "Should throw for data too short (less than 5 bytes)");
    }
  }

  @Nested
  @DisplayName("deserialize Empty Backtrace Tests")
  class DeserializeEmptyBacktraceTests {

    @Test
    @DisplayName("deserialize should handle empty backtrace with forceCapture false")
    void deserializeShouldHandleEmptyBacktraceWithoutForceCapture() {
      final ByteBuffer buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(0); // frame count = 0
      buffer.put((byte) 0); // forceCapture = false

      final WasmBacktrace backtrace = BacktraceDeserializer.deserialize(buffer.array());

      assertNotNull(backtrace, "Backtrace should not be null");
      assertTrue(backtrace.getFrames().isEmpty(), "Frames should be empty");
      assertFalse(backtrace.isForceCapture(), "ForceCapture should be false");
    }

    @Test
    @DisplayName("deserialize should handle empty backtrace with forceCapture true")
    void deserializeShouldHandleEmptyBacktraceWithForceCapture() {
      final ByteBuffer buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(0); // frame count = 0
      buffer.put((byte) 1); // forceCapture = true

      final WasmBacktrace backtrace = BacktraceDeserializer.deserialize(buffer.array());

      assertNotNull(backtrace, "Backtrace should not be null");
      assertTrue(backtrace.getFrames().isEmpty(), "Frames should be empty");
      assertTrue(backtrace.isForceCapture(), "ForceCapture should be true");
    }
  }

  @Nested
  @DisplayName("deserialize Single Frame Tests")
  class DeserializeSingleFrameTests {

    @Test
    @DisplayName("deserialize should handle frame with no optional fields")
    void deserializeShouldHandleFrameWithNoOptionalFields() {
      final ByteBuffer buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);

      // Header
      buffer.putInt(1); // frame count = 1
      buffer.put((byte) 0); // forceCapture = false

      // Frame
      buffer.putInt(42); // func_index = 42
      buffer.put((byte) 0); // has_func_name = false
      buffer.put((byte) 0); // has_module_offset = false
      buffer.put((byte) 0); // has_func_offset = false
      buffer.putInt(0); // symbol_count = 0

      final byte[] data = new byte[buffer.position()];
      buffer.flip();
      buffer.get(data);

      final WasmBacktrace backtrace = BacktraceDeserializer.deserialize(data);

      assertNotNull(backtrace, "Backtrace should not be null");
      assertEquals(1, backtrace.getFrames().size(), "Should have 1 frame");

      final FrameInfo frame = backtrace.getFrames().get(0);
      assertEquals(42, frame.getFuncIndex(), "Function index should be 42");
      assertTrue(frame.getFuncName().isEmpty(), "Function name should be empty");
      assertTrue(frame.getModuleOffset().isEmpty(), "Module offset should be empty");
      assertTrue(frame.getFuncOffset().isEmpty(), "Function offset should be empty");
      assertTrue(frame.getSymbols().isEmpty(), "Symbols should be empty");
    }

    @Test
    @DisplayName("deserialize should handle frame with function name")
    void deserializeShouldHandleFrameWithFunctionName() {
      final String funcName = "my_function";
      final byte[] funcNameBytes = funcName.getBytes(StandardCharsets.UTF_8);
      final ByteBuffer buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);

      // Header
      buffer.putInt(1); // frame count = 1
      buffer.put((byte) 0); // forceCapture = false

      // Frame
      buffer.putInt(10); // func_index = 10
      buffer.put((byte) 1); // has_func_name = true
      buffer.putInt(funcNameBytes.length);
      buffer.put(funcNameBytes);
      buffer.put((byte) 0); // has_module_offset = false
      buffer.put((byte) 0); // has_func_offset = false
      buffer.putInt(0); // symbol_count = 0

      final byte[] data = new byte[buffer.position()];
      buffer.flip();
      buffer.get(data);

      final WasmBacktrace backtrace = BacktraceDeserializer.deserialize(data);
      final FrameInfo frame = backtrace.getFrames().get(0);

      assertTrue(frame.getFuncName().isPresent(), "Function name should be present");
      assertEquals(funcName, frame.getFuncName().get(), "Function name should match");
    }

    @Test
    @DisplayName("deserialize should handle frame with offsets")
    void deserializeShouldHandleFrameWithOffsets() {
      final ByteBuffer buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);

      // Header
      buffer.putInt(1); // frame count = 1
      buffer.put((byte) 0); // forceCapture = false

      // Frame
      buffer.putInt(5); // func_index = 5
      buffer.put((byte) 0); // has_func_name = false
      buffer.put((byte) 1); // has_module_offset = true
      buffer.putInt(1024); // module_offset = 1024
      buffer.put((byte) 1); // has_func_offset = true
      buffer.putInt(256); // func_offset = 256
      buffer.putInt(0); // symbol_count = 0

      final byte[] data = new byte[buffer.position()];
      buffer.flip();
      buffer.get(data);

      final WasmBacktrace backtrace = BacktraceDeserializer.deserialize(data);
      final FrameInfo frame = backtrace.getFrames().get(0);

      assertTrue(frame.getModuleOffset().isPresent(), "Module offset should be present");
      assertEquals(Integer.valueOf(1024), frame.getModuleOffset().get(), "Module offset should be 1024");
      assertTrue(frame.getFuncOffset().isPresent(), "Function offset should be present");
      assertEquals(Integer.valueOf(256), frame.getFuncOffset().get(), "Function offset should be 256");
    }
  }

  @Nested
  @DisplayName("deserialize Symbol Tests")
  class DeserializeSymbolTests {

    @Test
    @DisplayName("deserialize should handle symbol with all fields")
    void deserializeShouldHandleSymbolWithAllFields() {
      final String symbolName = "test_symbol";
      final String sourceFile = "test.wasm";
      final byte[] symbolNameBytes = symbolName.getBytes(StandardCharsets.UTF_8);
      final byte[] sourceFileBytes = sourceFile.getBytes(StandardCharsets.UTF_8);

      final ByteBuffer buffer = ByteBuffer.allocate(200).order(ByteOrder.LITTLE_ENDIAN);

      // Header
      buffer.putInt(1); // frame count = 1
      buffer.put((byte) 0); // forceCapture = false

      // Frame
      buffer.putInt(1); // func_index
      buffer.put((byte) 0); // has_func_name = false
      buffer.put((byte) 0); // has_module_offset = false
      buffer.put((byte) 0); // has_func_offset = false
      buffer.putInt(1); // symbol_count = 1

      // Symbol
      buffer.put((byte) 1); // has_name = true
      buffer.putInt(symbolNameBytes.length);
      buffer.put(symbolNameBytes);
      buffer.put((byte) 1); // has_file = true
      buffer.putInt(sourceFileBytes.length);
      buffer.put(sourceFileBytes);
      buffer.put((byte) 1); // has_line = true
      buffer.putInt(42); // line = 42
      buffer.put((byte) 1); // has_column = true
      buffer.putInt(10); // column = 10

      final byte[] data = new byte[buffer.position()];
      buffer.flip();
      buffer.get(data);

      final WasmBacktrace backtrace = BacktraceDeserializer.deserialize(data);
      final FrameInfo frame = backtrace.getFrames().get(0);

      assertEquals(1, frame.getSymbols().size(), "Should have 1 symbol");
      final FrameSymbol symbol = frame.getSymbols().get(0);

      assertTrue(symbol.getName().isPresent(), "Symbol name should be present");
      assertEquals(symbolName, symbol.getName().get(), "Symbol name should match");
      assertTrue(symbol.getFile().isPresent(), "Source file should be present");
      assertEquals(sourceFile, symbol.getFile().get(), "Source file should match");
      assertTrue(symbol.getLine().isPresent(), "Line should be present");
      assertEquals(Integer.valueOf(42), symbol.getLine().get(), "Line should be 42");
      assertTrue(symbol.getColumn().isPresent(), "Column should be present");
      assertEquals(Integer.valueOf(10), symbol.getColumn().get(), "Column should be 10");
    }

    @Test
    @DisplayName("deserialize should handle symbol with no optional fields")
    void deserializeShouldHandleSymbolWithNoOptionalFields() {
      final ByteBuffer buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);

      // Header
      buffer.putInt(1); // frame count = 1
      buffer.put((byte) 0); // forceCapture = false

      // Frame
      buffer.putInt(1); // func_index
      buffer.put((byte) 0); // has_func_name = false
      buffer.put((byte) 0); // has_module_offset = false
      buffer.put((byte) 0); // has_func_offset = false
      buffer.putInt(1); // symbol_count = 1

      // Symbol with all optional fields absent
      buffer.put((byte) 0); // has_name = false
      buffer.put((byte) 0); // has_file = false
      buffer.put((byte) 0); // has_line = false
      buffer.put((byte) 0); // has_column = false

      final byte[] data = new byte[buffer.position()];
      buffer.flip();
      buffer.get(data);

      final WasmBacktrace backtrace = BacktraceDeserializer.deserialize(data);
      final FrameSymbol symbol = backtrace.getFrames().get(0).getSymbols().get(0);

      assertTrue(symbol.getName().isEmpty(), "Name should be empty");
      assertTrue(symbol.getFile().isEmpty(), "File should be empty");
      assertTrue(symbol.getLine().isEmpty(), "Line should be empty");
      assertTrue(symbol.getColumn().isEmpty(), "Column should be empty");
    }
  }

  @Nested
  @DisplayName("deserialize Multiple Frames Tests")
  class DeserializeMultipleFramesTests {

    @Test
    @DisplayName("deserialize should handle multiple frames")
    void deserializeShouldHandleMultipleFrames() {
      final ByteBuffer buffer = ByteBuffer.allocate(200).order(ByteOrder.LITTLE_ENDIAN);

      // Header
      buffer.putInt(3); // frame count = 3
      buffer.put((byte) 1); // forceCapture = true

      // Frame 1
      buffer.putInt(10); // func_index = 10
      buffer.put((byte) 0); // has_func_name = false
      buffer.put((byte) 0); // has_module_offset = false
      buffer.put((byte) 0); // has_func_offset = false
      buffer.putInt(0); // symbol_count = 0

      // Frame 2
      buffer.putInt(20); // func_index = 20
      buffer.put((byte) 0); // has_func_name = false
      buffer.put((byte) 0); // has_module_offset = false
      buffer.put((byte) 0); // has_func_offset = false
      buffer.putInt(0); // symbol_count = 0

      // Frame 3
      buffer.putInt(30); // func_index = 30
      buffer.put((byte) 0); // has_func_name = false
      buffer.put((byte) 0); // has_module_offset = false
      buffer.put((byte) 0); // has_func_offset = false
      buffer.putInt(0); // symbol_count = 0

      final byte[] data = new byte[buffer.position()];
      buffer.flip();
      buffer.get(data);

      final WasmBacktrace backtrace = BacktraceDeserializer.deserialize(data);

      assertEquals(3, backtrace.getFrames().size(), "Should have 3 frames");
      assertTrue(backtrace.isForceCapture(), "ForceCapture should be true");

      assertEquals(
          10, backtrace.getFrames().get(0).getFuncIndex(), "First frame index should be 10");
      assertEquals(
          20, backtrace.getFrames().get(1).getFuncIndex(), "Second frame index should be 20");
      assertEquals(
          30, backtrace.getFrames().get(2).getFuncIndex(), "Third frame index should be 30");
    }
  }

  @Nested
  @DisplayName("deserialize Multiple Symbols Tests")
  class DeserializeMultipleSymbolsTests {

    @Test
    @DisplayName("deserialize should handle frame with multiple symbols")
    void deserializeShouldHandleFrameWithMultipleSymbols() {
      final ByteBuffer buffer = ByteBuffer.allocate(300).order(ByteOrder.LITTLE_ENDIAN);

      // Header
      buffer.putInt(1); // frame count = 1
      buffer.put((byte) 0); // forceCapture = false

      // Frame
      buffer.putInt(1); // func_index
      buffer.put((byte) 0); // has_func_name = false
      buffer.put((byte) 0); // has_module_offset = false
      buffer.put((byte) 0); // has_func_offset = false
      buffer.putInt(2); // symbol_count = 2

      // Symbol 1
      buffer.put((byte) 0); // has_name = false
      buffer.put((byte) 0); // has_file = false
      buffer.put((byte) 1); // has_line = true
      buffer.putInt(100); // line = 100
      buffer.put((byte) 0); // has_column = false

      // Symbol 2
      buffer.put((byte) 0); // has_name = false
      buffer.put((byte) 0); // has_file = false
      buffer.put((byte) 1); // has_line = true
      buffer.putInt(200); // line = 200
      buffer.put((byte) 0); // has_column = false

      final byte[] data = new byte[buffer.position()];
      buffer.flip();
      buffer.get(data);

      final WasmBacktrace backtrace = BacktraceDeserializer.deserialize(data);
      final FrameInfo frame = backtrace.getFrames().get(0);

      assertEquals(2, frame.getSymbols().size(), "Should have 2 symbols");
      assertTrue(frame.getSymbols().get(0).getLine().isPresent(), "First symbol line should be present");
      assertEquals(
          Integer.valueOf(100),
          frame.getSymbols().get(0).getLine().get(),
          "First symbol line should be 100");
      assertTrue(frame.getSymbols().get(1).getLine().isPresent(), "Second symbol line should be present");
      assertEquals(
          Integer.valueOf(200),
          frame.getSymbols().get(1).getLine().get(),
          "Second symbol line should be 200");
    }
  }
}
