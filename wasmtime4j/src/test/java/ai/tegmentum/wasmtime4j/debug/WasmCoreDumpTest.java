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

package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmCoreDump} class.
 *
 * <p>WasmCoreDump captures the state of WebAssembly execution at the time of a trap.
 */
@DisplayName("WasmCoreDump Tests")
class WasmCoreDumpTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create core dump with all parameters")
    void shouldCreateCoreDumpWithAllParameters() {
      final Instant timestamp = Instant.now();
      final Map<String, Long> globals = new HashMap<>();
      globals.put("g1", 42L);

      final WasmCoreDump coreDump =
          new WasmCoreDump(
              "trap message",
              null,
              timestamp,
              Collections.emptyList(),
              globals,
              Collections.emptyList());

      assertNotNull(coreDump, "Core dump should not be null");
      assertEquals("trap message", coreDump.getTrapMessage(), "Trap message should match");
      assertEquals(timestamp, coreDump.getTimestamp(), "Timestamp should match");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("should return trap message")
    void shouldReturnTrapMessage() {
      final WasmCoreDump coreDump = createTestCoreDump();
      assertEquals("Test trap", coreDump.getTrapMessage(), "Trap message should match");
    }

    @Test
    @DisplayName("should return timestamp")
    void shouldReturnTimestamp() {
      final WasmCoreDump coreDump = createTestCoreDump();
      assertNotNull(coreDump.getTimestamp(), "Timestamp should not be null");
    }

    @Test
    @DisplayName("should return memory dumps")
    void shouldReturnMemoryDumps() {
      final WasmCoreDump coreDump = createTestCoreDump();
      final List<WasmCoreDump.MemoryDump> memoryDumps = coreDump.getMemoryDumps();
      assertNotNull(memoryDumps, "Memory dumps should not be null");
      assertEquals(1, memoryDumps.size(), "Should have 1 memory dump");
    }

    @Test
    @DisplayName("should return globals")
    void shouldReturnGlobals() {
      final WasmCoreDump coreDump = createTestCoreDump();
      final Map<String, Long> globals = coreDump.getGlobals();
      assertNotNull(globals, "Globals should not be null");
      assertEquals(2, globals.size(), "Should have 2 globals");
      assertEquals(100L, globals.get("counter"), "Counter should be 100");
    }

    @Test
    @DisplayName("should return stack frames")
    void shouldReturnStackFrames() {
      final WasmCoreDump coreDump = createTestCoreDump();
      final List<WasmCoreDump.StackFrame> stackFrames = coreDump.getStackFrames();
      assertNotNull(stackFrames, "Stack frames should not be null");
    }
  }

  @Nested
  @DisplayName("Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("should serialize to bytes")
    void shouldSerializeToBytes() {
      final WasmCoreDump coreDump = createTestCoreDump();
      final byte[] serialized = coreDump.serialize();
      assertNotNull(serialized, "Serialized data should not be null");
      assertTrue(serialized.length > 0, "Serialized data should not be empty");
      final String content = new String(serialized);
      assertTrue(content.contains("WASMCOREDUMP"), "Should contain header");
      assertTrue(content.contains("Test trap"), "Should contain trap message");
    }

    @Test
    @DisplayName("should deserialize from bytes")
    void shouldDeserializeFromBytes() {
      final WasmCoreDump original = createTestCoreDump();
      final byte[] serialized = original.serialize();
      final WasmCoreDump deserialized = WasmCoreDump.deserialize(serialized);
      assertNotNull(deserialized, "Deserialized core dump should not be null");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return meaningful string representation")
    void shouldReturnMeaningfulStringRepresentation() {
      final WasmCoreDump coreDump = createTestCoreDump();
      final String str = coreDump.toString();
      assertNotNull(str, "String representation should not be null");
      assertTrue(str.contains("WasmCoreDump"), "Should contain class name");
      assertTrue(str.contains("Test trap"), "Should contain trap message");
    }
  }

  @Nested
  @DisplayName("MemoryDump Tests")
  class MemoryDumpTests {

    @Test
    @DisplayName("should create memory dump with all parameters")
    void shouldCreateMemoryDumpWithAllParameters() {
      final byte[] data = {0x01, 0x02, 0x03, 0x04};
      final WasmCoreDump.MemoryDump memoryDump = new WasmCoreDump.MemoryDump(0, data, 0x1000);

      assertEquals(0, memoryDump.getMemoryIndex(), "Memory index should be 0");
      assertEquals(0x1000, memoryDump.getBaseAddress(), "Base address should match");
      assertEquals(4, memoryDump.getSize(), "Size should be 4");
    }

    @Test
    @DisplayName("should return copy of data")
    void shouldReturnCopyOfData() {
      final byte[] original = {0x01, 0x02, 0x03};
      final WasmCoreDump.MemoryDump memoryDump = new WasmCoreDump.MemoryDump(0, original, 0);

      final byte[] retrieved = memoryDump.getData();
      assertArrayEquals(original, retrieved, "Data should match");

      // Modify retrieved data - should not affect internal state
      retrieved[0] = (byte) 0x99;
      assertArrayEquals(original, memoryDump.getData(), "Internal data should not be modified");
    }
  }

  @Nested
  @DisplayName("StackFrame Tests")
  class StackFrameTests {

    @Test
    @DisplayName("should create stack frame with all parameters")
    void shouldCreateStackFrameWithAllParameters() {
      final long[] locals = {1L, 2L, 3L};
      final WasmCoreDump.StackFrame frame =
          new WasmCoreDump.StackFrame("testFunc", 5, 0x200, locals);

      assertEquals("testFunc", frame.getFunctionName(), "Function name should match");
      assertEquals(5, frame.getFunctionIndex(), "Function index should be 5");
      assertEquals(0x200, frame.getInstructionOffset(), "Instruction offset should match");
      assertArrayEquals(locals, frame.getLocals(), "Locals should match");
    }

    @Test
    @DisplayName("should handle null locals")
    void shouldHandleNullLocals() {
      final WasmCoreDump.StackFrame frame = new WasmCoreDump.StackFrame("func", 0, 0, null);

      assertNotNull(frame.getLocals(), "Locals should not be null");
      assertEquals(0, frame.getLocals().length, "Locals should be empty");
    }

    @Test
    @DisplayName("should return copy of locals")
    void shouldReturnCopyOfLocals() {
      final long[] original = {10L, 20L};
      final WasmCoreDump.StackFrame frame = new WasmCoreDump.StackFrame("func", 0, 0, original);

      final long[] retrieved = frame.getLocals();
      retrieved[0] = 999L;
      assertEquals(10L, frame.getLocals()[0], "Internal locals should not be modified");
    }

    @Test
    @DisplayName("should return meaningful string representation")
    void shouldReturnMeaningfulStringRepresentation() {
      final WasmCoreDump.StackFrame frame =
          new WasmCoreDump.StackFrame("main", 0, 100, new long[2]);

      final String str = frame.toString();
      assertTrue(str.contains("main"), "Should contain function name");
      assertTrue(str.contains("100"), "Should contain offset");
    }
  }

  private WasmCoreDump createTestCoreDump() {
    final Map<String, Long> globals = new HashMap<>();
    globals.put("counter", 100L);
    globals.put("flag", 1L);

    final byte[] memData = {0x00, 0x01, 0x02, 0x03};
    final List<WasmCoreDump.MemoryDump> memoryDumps =
        Arrays.asList(new WasmCoreDump.MemoryDump(0, memData, 0));

    return new WasmCoreDump(
        "Test trap", null, Instant.now(), memoryDumps, globals, Collections.emptyList());
  }
}
