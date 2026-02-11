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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.DebugFrame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DebugFrame} class.
 *
 * <p>DebugFrame provides detailed information about a single stack frame during WebAssembly
 * execution.
 */
@DisplayName("DebugFrame Tests")
class DebugFrameTest {

  @Nested
  @DisplayName("Full Constructor Tests")
  class FullConstructorTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final List<WasmValue> locals = List.of(WasmValue.i32(10), WasmValue.i64(20L));
      final List<WasmValue> stack = List.of(WasmValue.f32(1.5f));
      final Map<String, Object> attrs = Map.of("key", "value");

      final DebugFrame frame = new DebugFrame(3, "compute", "myModule", 0xFF, locals, stack, attrs);

      assertEquals(3, frame.getFunctionIndex(), "functionIndex should be 3");
      assertEquals("compute", frame.getFunctionName(), "functionName should be 'compute'");
      assertEquals("myModule", frame.getModuleName(), "moduleName should be 'myModule'");
      assertEquals(0xFF, frame.getInstructionOffset(), "instructionOffset should be 0xFF");
      assertEquals(2, frame.getLocalCount(), "should have 2 locals");
      assertEquals(1, frame.getStackDepth(), "stack depth should be 1");
      assertTrue(frame.hasAttribute("key"), "should have attribute 'key'");
      assertEquals("value", frame.getAttribute("key"), "attribute 'key' should be 'value'");
    }

    @Test
    @DisplayName("should handle null for optional collections")
    void shouldHandleNullForOptionalCollections() {
      final DebugFrame frame = new DebugFrame(0, "fn", "mod", 0, null, null, null);

      assertEquals(0, frame.getLocalCount(), "locals should be empty when null");
      assertEquals(0, frame.getStackDepth(), "operandStack should be empty when null");
      assertFalse(frame.hasAttribute("any"), "attributes should be empty when null");
    }
  }

  @Nested
  @DisplayName("Minimal Constructor Tests")
  class MinimalConstructorTests {

    @Test
    @DisplayName("should create instance with minimal parameters")
    void shouldCreateInstanceWithMinimalParameters() {
      final DebugFrame frame = new DebugFrame(5, 0x1A);

      assertEquals(5, frame.getFunctionIndex(), "functionIndex should be 5");
      assertNull(frame.getFunctionName(), "functionName should be null");
      assertNull(frame.getModuleName(), "moduleName should be null");
      assertEquals(0x1A, frame.getInstructionOffset(), "instructionOffset should be 0x1A");
      assertEquals(0, frame.getLocalCount(), "locals should be empty");
      assertEquals(0, frame.getStackDepth(), "operandStack should be empty");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should build DebugFrame with all fields set via builder")
    void shouldBuildDebugFrameWithAllFieldsViaBuilder() {
      final List<WasmValue> locals = List.of(WasmValue.i32(42));
      final List<WasmValue> stack = List.of(WasmValue.f64(3.14));
      final Map<String, Object> attrs = Map.of("debug", true);

      final DebugFrame frame =
          DebugFrame.builder()
              .functionIndex(7)
              .functionName("process")
              .moduleName("worker")
              .instructionOffset(0xAB)
              .locals(locals)
              .operandStack(stack)
              .attributes(attrs)
              .build();

      assertEquals(7, frame.getFunctionIndex(), "functionIndex should be 7");
      assertEquals("process", frame.getFunctionName(), "functionName should be 'process'");
      assertEquals("worker", frame.getModuleName(), "moduleName should be 'worker'");
      assertEquals(0xAB, frame.getInstructionOffset(), "instructionOffset should be 0xAB");
      assertEquals(1, frame.getLocalCount(), "should have 1 local");
      assertEquals(1, frame.getStackDepth(), "stack depth should be 1");
      assertTrue(frame.hasAttribute("debug"), "should have attribute 'debug'");
    }

    @Test
    @DisplayName("should build DebugFrame with default values when nothing set")
    void shouldBuildDebugFrameWithDefaultValues() {
      final DebugFrame frame = DebugFrame.builder().build();

      assertEquals(0, frame.getFunctionIndex(), "default functionIndex should be 0");
      assertNull(frame.getFunctionName(), "default functionName should be null");
      assertNull(frame.getModuleName(), "default moduleName should be null");
      assertEquals(0L, frame.getInstructionOffset(), "default instructionOffset should be 0");
      assertEquals(0, frame.getLocalCount(), "default locals should be empty");
      assertEquals(0, frame.getStackDepth(), "default operandStack should be empty");
    }

    @Test
    @DisplayName("builder should return same builder for chaining")
    void builderShouldReturnSameBuilderForChaining() {
      final DebugFrame.Builder builder = DebugFrame.builder();
      final DebugFrame.Builder result = builder.functionIndex(1);

      assertEquals(builder, result, "Builder methods should return the same builder for chaining");
    }

    @Test
    @DisplayName("builder should handle null collections gracefully")
    void builderShouldHandleNullCollectionsGracefully() {
      final DebugFrame frame =
          DebugFrame.builder().locals(null).operandStack(null).attributes(null).build();

      assertEquals(0, frame.getLocalCount(), "null locals should result in empty");
      assertEquals(0, frame.getStackDepth(), "null operandStack should result in empty");
    }
  }

  @Nested
  @DisplayName("Locals Tests")
  class LocalsTests {

    @Test
    @DisplayName("getLocals should return a defensive copy")
    void getLocalsShouldReturnDefensiveCopy() {
      final List<WasmValue> origLocals = new ArrayList<>();
      origLocals.add(WasmValue.i32(1));
      final DebugFrame frame = new DebugFrame(0, "fn", null, 0, origLocals, null, null);

      final List<WasmValue> returned = frame.getLocals();

      assertNotSame(returned, frame.getLocals(), "getLocals should return a new list each time");
    }

    @Test
    @DisplayName("getLocal should return specific local by index")
    void getLocalShouldReturnSpecificLocalByIndex() {
      final List<WasmValue> locals = List.of(WasmValue.i32(10), WasmValue.i64(20L));
      final DebugFrame frame = new DebugFrame(0, null, null, 0, locals, null, null);

      final WasmValue local0 = frame.getLocal(0);
      final WasmValue local1 = frame.getLocal(1);

      assertEquals(WasmValueType.I32, local0.getType(), "local[0] should be I32");
      assertEquals(10, local0.asI32(), "local[0] value should be 10");
      assertEquals(WasmValueType.I64, local1.getType(), "local[1] should be I64");
      assertEquals(20L, local1.asI64(), "local[1] value should be 20");
    }

    @Test
    @DisplayName("getLocal should throw IndexOutOfBoundsException for invalid index")
    void getLocalShouldThrowForInvalidIndex() {
      final DebugFrame frame = new DebugFrame(0, 0);

      assertThrows(
          IndexOutOfBoundsException.class,
          () -> frame.getLocal(0),
          "getLocal should throw IndexOutOfBoundsException for invalid index");
    }

    @Test
    @DisplayName("getLocalCount should return number of locals")
    void getLocalCountShouldReturnNumberOfLocals() {
      final List<WasmValue> locals = List.of(WasmValue.i32(1), WasmValue.i32(2), WasmValue.i32(3));
      final DebugFrame frame = new DebugFrame(0, null, null, 0, locals, null, null);

      assertEquals(3, frame.getLocalCount(), "getLocalCount should return 3");
    }
  }

  @Nested
  @DisplayName("Operand Stack Tests")
  class OperandStackTests {

    @Test
    @DisplayName("getOperandStack should return a defensive copy")
    void getOperandStackShouldReturnDefensiveCopy() {
      final List<WasmValue> stack = new ArrayList<>();
      stack.add(WasmValue.f32(1.0f));
      final DebugFrame frame = new DebugFrame(0, null, null, 0, null, stack, null);

      final List<WasmValue> returned = frame.getOperandStack();

      assertNotSame(
          returned, frame.getOperandStack(), "getOperandStack should return a new list each time");
    }

    @Test
    @DisplayName("getStackDepth should return number of stack values")
    void getStackDepthShouldReturnNumberOfStackValues() {
      final List<WasmValue> stack = List.of(WasmValue.i32(1), WasmValue.i32(2));
      final DebugFrame frame = new DebugFrame(0, null, null, 0, null, stack, null);

      assertEquals(2, frame.getStackDepth(), "getStackDepth should return 2");
    }

    @Test
    @DisplayName("getStackDepth should return 0 when stack is empty")
    void getStackDepthShouldReturnZeroWhenEmpty() {
      final DebugFrame frame = new DebugFrame(0, 0);

      assertEquals(0, frame.getStackDepth(), "getStackDepth should return 0 for empty stack");
    }
  }

  @Nested
  @DisplayName("Attributes Tests")
  class AttributesTests {

    @Test
    @DisplayName("getAttribute should return value for existing key")
    void getAttributeShouldReturnValueForExistingKey() {
      final Map<String, Object> attrs = Map.of("source", "debug_info");
      final DebugFrame frame = new DebugFrame(0, null, null, 0, null, null, attrs);

      assertEquals("debug_info", frame.getAttribute("source"), "getAttribute should return value");
    }

    @Test
    @DisplayName("getAttribute should return null for missing key")
    void getAttributeShouldReturnNullForMissingKey() {
      final Map<String, Object> attrs = Map.of("key1", "val1");
      final DebugFrame frame = new DebugFrame(0, null, null, 0, null, null, attrs);

      assertNull(frame.getAttribute("nonexistent"), "getAttribute should return null for missing");
    }

    @Test
    @DisplayName("hasAttribute should return true for existing key")
    void hasAttributeShouldReturnTrueForExistingKey() {
      final Map<String, Object> attrs = Map.of("present", 42);
      final DebugFrame frame = new DebugFrame(0, null, null, 0, null, null, attrs);

      assertTrue(frame.hasAttribute("present"), "hasAttribute should return true");
    }

    @Test
    @DisplayName("hasAttribute should return false for missing key")
    void hasAttributeShouldReturnFalseForMissingKey() {
      final DebugFrame frame = new DebugFrame(0, 0);

      assertFalse(frame.hasAttribute("missing"), "hasAttribute should return false");
    }

    @Test
    @DisplayName("getAttributes should return a defensive copy")
    void getAttributesShouldReturnDefensiveCopy() {
      final Map<String, Object> attrs = new HashMap<>();
      attrs.put("k", "v");
      final DebugFrame frame = new DebugFrame(0, null, null, 0, null, null, attrs);

      final Map<String, Object> returned = frame.getAttributes();

      assertNotSame(
          returned, frame.getAttributes(), "getAttributes should return a new map each time");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should format as moduleName!functionName+0xoffset")
    void toStringShouldFormatWithModuleAndFunction() {
      final DebugFrame frame = new DebugFrame(0, "add", "math", 0x1A, null, null, null);

      final String result = frame.toString();

      assertEquals("math!add+0x1a", result, "toString should format as 'moduleName!funcName+0x'");
    }

    @Test
    @DisplayName("toString should format as func[index]+0xoffset when name is null")
    void toStringShouldFormatWithIndexWhenNameNull() {
      final DebugFrame frame = new DebugFrame(5, 0x2B);

      final String result = frame.toString();

      assertEquals("func[5]+0x2b", result, "toString should use func[index] when name is null");
    }

    @Test
    @DisplayName("toString should omit module prefix when moduleName is null")
    void toStringShouldOmitModuleWhenNull() {
      final DebugFrame frame = new DebugFrame(0, "doWork", null, 0, null, null, null);

      final String result = frame.toString();

      assertEquals("doWork+0x0", result, "toString should omit module prefix when null");
    }

    @Test
    @DisplayName("toString should format offset as hex")
    void toStringShouldFormatOffsetAsHex() {
      final DebugFrame frame = new DebugFrame(0, "fn", null, 255, null, null, null);

      final String result = frame.toString();

      assertTrue(result.contains("+0xff"), "toString should format offset as hex: +0xff");
    }
  }
}
