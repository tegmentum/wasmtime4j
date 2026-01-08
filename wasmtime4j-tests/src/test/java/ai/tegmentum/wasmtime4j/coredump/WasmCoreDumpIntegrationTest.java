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

package ai.tegmentum.wasmtime4j.coredump;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.coredump.DefaultCoreDumpFrame;
import ai.tegmentum.wasmtime4j.coredump.DefaultWasmCoreDump;
import ai.tegmentum.wasmtime4j.coredump.WasmCoreDump;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for WasmCoreDump - trap diagnostics and debugging.
 *
 * <p>These tests verify core dump building, frame construction, content access, and serialization.
 *
 * @since 1.0.0
 */
@DisplayName("WasmCoreDump Integration Tests")
public final class WasmCoreDumpIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasmCoreDumpIntegrationTest.class.getName());

  @Nested
  @DisplayName("CoreDump Builder Tests")
  class CoreDumpBuilderTests {

    @Test
    @DisplayName("should create empty core dump")
    void shouldCreateEmptyCoreDump(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final DefaultWasmCoreDump coreDump = DefaultWasmCoreDump.builder().build();

      assertNotNull(coreDump, "Core dump should not be null");
      assertTrue(coreDump.getFrames().isEmpty(), "Frames should be empty");
      assertTrue(coreDump.getModules().isEmpty(), "Modules should be empty");
      assertTrue(coreDump.getInstances().isEmpty(), "Instances should be empty");
      assertTrue(coreDump.getGlobals().isEmpty(), "Globals should be empty");
      assertTrue(coreDump.getMemories().isEmpty(), "Memories should be empty");

      LOGGER.info("Created empty core dump: " + coreDump);
    }

    @Test
    @DisplayName("should create core dump with name and trap message")
    void shouldCreateCoreDumpWithNameAndTrapMessage(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final DefaultWasmCoreDump coreDump = DefaultWasmCoreDump.builder()
          .name("test-coredump")
          .trapMessage("Unreachable instruction executed")
          .build();

      assertEquals("test-coredump", coreDump.getName(), "Name should match");
      assertEquals("Unreachable instruction executed", coreDump.getTrapMessage(),
          "Trap message should match");

      LOGGER.info("Created named core dump: " + coreDump);
    }

    @Test
    @DisplayName("should create core dump with modules")
    void shouldCreateCoreDumpWithModules(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final DefaultWasmCoreDump coreDump = DefaultWasmCoreDump.builder()
          .addModule("main.wasm")
          .addModule("lib.wasm")
          .addModules(Arrays.asList("util1.wasm", "util2.wasm"))
          .build();

      assertEquals(4, coreDump.getModules().size(), "Should have 4 modules");
      assertEquals("main.wasm", coreDump.getModules().get(0), "First module");
      assertEquals("lib.wasm", coreDump.getModules().get(1), "Second module");
      assertEquals("util1.wasm", coreDump.getModules().get(2), "Third module");
      assertEquals("util2.wasm", coreDump.getModules().get(3), "Fourth module");

      LOGGER.info("Created core dump with modules: " + coreDump.getModules());
    }

    @Test
    @DisplayName("should reject null frame")
    void shouldRejectNullFrame(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final NullPointerException exception = assertThrows(
          NullPointerException.class,
          () -> DefaultWasmCoreDump.builder().addFrame(null),
          "Should reject null frame");
      assertNotNull(exception.getMessage(), "Exception should have message");
      LOGGER.info("Rejected null frame: " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject null module")
    void shouldRejectNullModule(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final NullPointerException exception = assertThrows(
          NullPointerException.class,
          () -> DefaultWasmCoreDump.builder().addModule(null),
          "Should reject null module");
      assertNotNull(exception.getMessage(), "Exception should have message");
      LOGGER.info("Rejected null module: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("CoreDumpFrame Builder Tests")
  class CoreDumpFrameBuilderTests {

    @Test
    @DisplayName("should create frame with all fields")
    void shouldCreateFrameWithAllFields(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final DefaultCoreDumpFrame frame = DefaultCoreDumpFrame.builder()
          .funcIndex(5)
          .funcName("test_function")
          .moduleIndex(0)
          .moduleName("main.wasm")
          .offset(0x1234)
          .trapFrame(true)
          .build();

      assertEquals(5, frame.getFuncIndex(), "Function index");
      assertTrue(frame.getFuncName().isPresent(), "Function name should be present");
      assertEquals("test_function", frame.getFuncName().get(), "Function name value");
      assertEquals(0, frame.getModuleIndex(), "Module index");
      assertTrue(frame.getModuleName().isPresent(), "Module name should be present");
      assertEquals("main.wasm", frame.getModuleName().get(), "Module name value");
      assertEquals(0x1234, frame.getOffset(), "Offset");
      assertTrue(frame.isTrapFrame(), "Should be trap frame");

      LOGGER.info("Created frame: " + frame);
    }

    @Test
    @DisplayName("should create frame without optional fields")
    void shouldCreateFrameWithoutOptionalFields(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final DefaultCoreDumpFrame frame = DefaultCoreDumpFrame.builder()
          .funcIndex(3)
          .moduleIndex(1)
          .offset(0x5678)
          .build();

      assertEquals(3, frame.getFuncIndex(), "Function index");
      assertFalse(frame.getFuncName().isPresent(), "Function name should be absent");
      assertEquals(1, frame.getModuleIndex(), "Module index");
      assertFalse(frame.getModuleName().isPresent(), "Module name should be absent");
      assertEquals(0x5678, frame.getOffset(), "Offset");
      assertFalse(frame.isTrapFrame(), "Should not be trap frame by default");

      LOGGER.info("Created frame without optional fields: " + frame);
    }

    @Test
    @DisplayName("should create frame with local variables")
    void shouldCreateFrameWithLocalVariables(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final byte[] local1 = new byte[]{0x01, 0x02, 0x03, 0x04};
      final byte[] local2 = new byte[]{0x10, 0x20, 0x30, 0x40};

      final DefaultCoreDumpFrame frame = DefaultCoreDumpFrame.builder()
          .funcIndex(0)
          .moduleIndex(0)
          .offset(0)
          .addLocal(local1)
          .addLocal(local2)
          .build();

      assertEquals(2, frame.getLocals().size(), "Should have 2 locals");
      assertArrayEquals(local1, frame.getLocals().get(0), "First local");
      assertArrayEquals(local2, frame.getLocals().get(1), "Second local");

      LOGGER.info("Created frame with " + frame.getLocals().size() + " locals");
    }

    @Test
    @DisplayName("should create frame with stack values")
    void shouldCreateFrameWithStackValues(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final byte[] stack1 = new byte[]{(byte) 0xFF, (byte) 0xFE};
      final byte[] stack2 = new byte[]{(byte) 0xAA, (byte) 0xBB};
      final byte[] stack3 = new byte[]{(byte) 0xCC, (byte) 0xDD};

      final DefaultCoreDumpFrame frame = DefaultCoreDumpFrame.builder()
          .funcIndex(0)
          .moduleIndex(0)
          .offset(0)
          .addStackValue(stack1)
          .addStackValue(stack2)
          .addStackValue(stack3)
          .build();

      assertEquals(3, frame.getStack().size(), "Should have 3 stack values");
      assertArrayEquals(stack1, frame.getStack().get(0), "First stack value");
      assertArrayEquals(stack2, frame.getStack().get(1), "Second stack value");
      assertArrayEquals(stack3, frame.getStack().get(2), "Third stack value");

      LOGGER.info("Created frame with " + frame.getStack().size() + " stack values");
    }
  }

  @Nested
  @DisplayName("CoreDump Content Tests")
  class CoreDumpContentTests {

    @Test
    @DisplayName("should return frames from core dump")
    void shouldReturnFramesFromCoreDump(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final DefaultCoreDumpFrame frame1 = DefaultCoreDumpFrame.builder()
          .funcIndex(0)
          .funcName("main")
          .moduleIndex(0)
          .offset(0x100)
          .build();
      final DefaultCoreDumpFrame frame2 = DefaultCoreDumpFrame.builder()
          .funcIndex(1)
          .funcName("helper")
          .moduleIndex(0)
          .offset(0x200)
          .trapFrame(true)
          .build();

      final DefaultWasmCoreDump coreDump = DefaultWasmCoreDump.builder()
          .name("test")
          .addFrame(frame1)
          .addFrame(frame2)
          .build();

      assertEquals(2, coreDump.getFrames().size(), "Should have 2 frames");
      assertEquals("main", coreDump.getFrames().get(0).getFuncName().orElse(null), "First frame");
      assertEquals("helper", coreDump.getFrames().get(1).getFuncName().orElse(null), "Second frame");
      assertTrue(coreDump.getFrames().get(1).isTrapFrame(), "Second frame should be trap frame");

      LOGGER.info("Core dump frames: " + coreDump.getFrames().size());
    }

    @Test
    @DisplayName("should calculate core dump size")
    void shouldCalculateCoreDumpSize(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a core dump with serialized data
      final byte[] serializedData = new byte[1024];
      final DefaultWasmCoreDump coreDump = DefaultWasmCoreDump.builder()
          .name("sized-dump")
          .serializedData(serializedData)
          .build();

      final long size = coreDump.getSize();
      assertTrue(size >= 1024, "Size should include serialized data: " + size);

      LOGGER.info("Core dump size: " + size + " bytes");
    }

    @Test
    @DisplayName("should return immutable lists")
    void shouldReturnImmutableLists(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final DefaultWasmCoreDump coreDump = DefaultWasmCoreDump.builder()
          .addModule("test.wasm")
          .build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> coreDump.getModules().add("another.wasm"),
          "Module list should be immutable");

      assertThrows(
          UnsupportedOperationException.class,
          () -> coreDump.getFrames().add(null),
          "Frame list should be immutable");

      LOGGER.info("Lists are correctly immutable");
    }
  }

  @Nested
  @DisplayName("CoreDump Serialization Tests")
  class CoreDumpSerializationTests {

    @Test
    @DisplayName("should serialize core dump with data")
    void shouldSerializeCoreDumpWithData(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final byte[] originalData = new byte[]{0x00, 0x61, 0x73, 0x6D}; // WASM magic
      final DefaultWasmCoreDump coreDump = DefaultWasmCoreDump.builder()
          .name("serializable")
          .serializedData(originalData)
          .build();

      final byte[] serialized = coreDump.serialize();

      assertNotNull(serialized, "Serialized data should not be null");
      assertArrayEquals(originalData, serialized, "Serialized data should match original");

      LOGGER.info("Serialized core dump: " + serialized.length + " bytes");
    }

    @Test
    @DisplayName("should throw when serializing without data")
    void shouldThrowWhenSerializingWithoutData(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final DefaultWasmCoreDump coreDump = DefaultWasmCoreDump.builder()
          .name("no-serialization")
          .build();

      final UnsupportedOperationException exception = assertThrows(
          UnsupportedOperationException.class,
          () -> coreDump.serialize(),
          "Should throw when serialized data not available");
      assertNotNull(exception.getMessage(), "Exception should have message");

      LOGGER.info("Correctly threw: " + exception.getMessage());
    }

    @Test
    @DisplayName("should return copy of serialized data")
    void shouldReturnCopyOfSerializedData(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final byte[] originalData = new byte[]{0x01, 0x02, 0x03};
      final DefaultWasmCoreDump coreDump = DefaultWasmCoreDump.builder()
          .serializedData(originalData)
          .build();

      final byte[] serialized1 = coreDump.serialize();
      final byte[] serialized2 = coreDump.serialize();

      // Modify first copy
      serialized1[0] = (byte) 0xFF;

      // Second copy should be unchanged
      assertEquals(0x01, serialized2[0], "Serialized data should be defensive copy");

      LOGGER.info("Serialization returns defensive copies");
    }
  }

  @Nested
  @DisplayName("CoreDump toString Tests")
  class CoreDumpToStringTests {

    @Test
    @DisplayName("should produce readable toString")
    void shouldProduceReadableToString(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final DefaultCoreDumpFrame frame = DefaultCoreDumpFrame.builder()
          .funcIndex(5)
          .funcName("test")
          .moduleIndex(0)
          .moduleName("main")
          .offset(0x100)
          .trapFrame(true)
          .build();

      final DefaultWasmCoreDump coreDump = DefaultWasmCoreDump.builder()
          .name("test-dump")
          .trapMessage("Test trap")
          .addFrame(frame)
          .addModule("main.wasm")
          .build();

      final String frameString = frame.toString();
      assertNotNull(frameString, "Frame toString should not be null");
      assertTrue(frameString.contains("funcIndex=5"), "Should contain funcIndex");
      assertTrue(frameString.contains("test"), "Should contain funcName");

      final String dumpString = coreDump.toString();
      assertNotNull(dumpString, "Core dump toString should not be null");
      assertTrue(dumpString.contains("test-dump"), "Should contain name");
      assertTrue(dumpString.contains("Test trap"), "Should contain trap message");
      assertTrue(dumpString.contains("frames=1"), "Should contain frame count");

      LOGGER.info("Frame toString: " + frameString);
      LOGGER.info("CoreDump toString: " + dumpString);
    }
  }
}
