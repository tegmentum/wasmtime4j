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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmCoreDump} class.
 *
 * <p>WasmCoreDump is the default implementation of the WasmCoreDump interface.
 */
@DisplayName("WasmCoreDump Tests")
class WasmCoreDumpTest {

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create builder via static method")
    void shouldCreateBuilderViaStaticMethod() {
      final WasmCoreDump.Builder builder = WasmCoreDump.builder();
      assertNotNull(builder, "Builder should not be null");
    }

    @Test
    @DisplayName("should build with name")
    void shouldBuildWithName() {
      final WasmCoreDump coreDump = WasmCoreDump.builder().name("test-coredump").build();
      assertEquals("test-coredump", coreDump.getName(), "Name should match");
    }

    @Test
    @DisplayName("should build with trap message")
    void shouldBuildWithTrapMessage() {
      final WasmCoreDump coreDump =
          WasmCoreDump.builder().trapMessage("unreachable executed").build();
      assertEquals("unreachable executed", coreDump.getTrapMessage(), "Trap message should match");
    }

    @Test
    @DisplayName("should build with serialized data")
    void shouldBuildWithSerializedData() {
      final byte[] data = {0x00, 0x61, 0x73, 0x6D};
      final WasmCoreDump coreDump = WasmCoreDump.builder().serializedData(data).build();
      assertArrayEquals(data, coreDump.serialize(), "Serialized data should match");
    }

    @Test
    @DisplayName("should build with modules")
    void shouldBuildWithModules() {
      final WasmCoreDump coreDump =
          WasmCoreDump.builder().addModule("module1.wasm").addModule("module2.wasm").build();
      assertEquals(2, coreDump.getModules().size(), "Should have 2 modules");
      assertEquals("module1.wasm", coreDump.getModules().get(0), "First module should match");
    }

    @Test
    @DisplayName("should build with modules list")
    void shouldBuildWithModulesList() {
      final List<String> modules = Arrays.asList("a.wasm", "b.wasm", "c.wasm");
      final WasmCoreDump coreDump = WasmCoreDump.builder().addModules(modules).build();
      assertEquals(3, coreDump.getModules().size(), "Should have 3 modules");
    }

    @Test
    @DisplayName("should build with frames")
    void shouldBuildWithFrames() {
      final CoreDumpFrame frame = CoreDumpFrame.builder().funcIndex(0).funcName("main").build();
      final WasmCoreDump coreDump = WasmCoreDump.builder().addFrame(frame).build();
      assertEquals(1, coreDump.getFrames().size(), "Should have 1 frame");
    }

    @Test
    @DisplayName("should build with frames list")
    void shouldBuildWithFramesList() {
      final List<CoreDumpFrame> frames =
          Arrays.asList(
              CoreDumpFrame.builder().funcIndex(0).build(),
              CoreDumpFrame.builder().funcIndex(1).build());
      final WasmCoreDump coreDump = WasmCoreDump.builder().addFrames(frames).build();
      assertEquals(2, coreDump.getFrames().size(), "Should have 2 frames");
    }

    @Test
    @DisplayName("should build with instances")
    void shouldBuildWithInstances() {
      final CoreDumpInstance instance =
          CoreDumpInstance.builder().index(0).name("instance0").build();
      final WasmCoreDump coreDump = WasmCoreDump.builder().addInstance(instance).build();
      assertEquals(1, coreDump.getInstances().size(), "Should have 1 instance");
    }

    @Test
    @DisplayName("should build with globals")
    void shouldBuildWithGlobals() {
      final CoreDumpGlobal global = CoreDumpGlobal.builder().globalIndex(0).i32Value(42).build();
      final WasmCoreDump coreDump = WasmCoreDump.builder().addGlobal(global).build();
      assertEquals(1, coreDump.getGlobals().size(), "Should have 1 global");
    }

    @Test
    @DisplayName("should build with memories")
    void shouldBuildWithMemories() {
      final CoreDumpMemory memory = CoreDumpMemory.builder().memoryIndex(0).sizeInPages(1).build();
      final WasmCoreDump coreDump = WasmCoreDump.builder().addMemory(memory).build();
      assertEquals(1, coreDump.getMemories().size(), "Should have 1 memory");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("modules list should be immutable")
    void modulesListShouldBeImmutable() {
      final WasmCoreDump coreDump = WasmCoreDump.builder().addModule("module.wasm").build();
      assertThrows(
          UnsupportedOperationException.class,
          () -> coreDump.getModules().add("another.wasm"),
          "Modules list should be immutable");
    }

    @Test
    @DisplayName("frames list should be immutable")
    void framesListShouldBeImmutable() {
      final CoreDumpFrame frame = CoreDumpFrame.builder().build();
      final WasmCoreDump coreDump = WasmCoreDump.builder().addFrame(frame).build();
      assertThrows(
          UnsupportedOperationException.class,
          () -> coreDump.getFrames().add(frame),
          "Frames list should be immutable");
    }

    @Test
    @DisplayName("instances list should be immutable")
    void instancesListShouldBeImmutable() {
      final CoreDumpInstance instance = CoreDumpInstance.builder().build();
      final WasmCoreDump coreDump = WasmCoreDump.builder().addInstance(instance).build();
      assertThrows(
          UnsupportedOperationException.class,
          () -> coreDump.getInstances().add(instance),
          "Instances list should be immutable");
    }

    @Test
    @DisplayName("globals list should be immutable")
    void globalsListShouldBeImmutable() {
      final CoreDumpGlobal global = CoreDumpGlobal.builder().valueType(WasmValueType.I32).build();
      final WasmCoreDump coreDump = WasmCoreDump.builder().addGlobal(global).build();
      assertThrows(
          UnsupportedOperationException.class,
          () -> coreDump.getGlobals().add(global),
          "Globals list should be immutable");
    }

    @Test
    @DisplayName("memories list should be immutable")
    void memoriesListShouldBeImmutable() {
      final CoreDumpMemory memory = CoreDumpMemory.builder().build();
      final WasmCoreDump coreDump = WasmCoreDump.builder().addMemory(memory).build();
      assertThrows(
          UnsupportedOperationException.class,
          () -> coreDump.getMemories().add(memory),
          "Memories list should be immutable");
    }
  }

  @Nested
  @DisplayName("Serialize Tests")
  class SerializeTests {

    @Test
    @DisplayName("should throw when no serialized data")
    void shouldThrowWhenNoSerializedData() {
      final WasmCoreDump coreDump = WasmCoreDump.builder().build();
      assertThrows(
          UnsupportedOperationException.class,
          coreDump::serialize,
          "Should throw when no serialized data");
    }

    @Test
    @DisplayName("should return copy of serialized data")
    void shouldReturnCopyOfSerializedData() {
      final byte[] original = {0x01, 0x02, 0x03};
      final WasmCoreDump coreDump = WasmCoreDump.builder().serializedData(original).build();
      final byte[] retrieved = coreDump.serialize();
      retrieved[0] = (byte) 0xFF;
      assertArrayEquals(original, coreDump.serialize(), "Internal data should not be modified");
    }
  }

  @Nested
  @DisplayName("GetSize Tests")
  class GetSizeTests {

    @Test
    @DisplayName("should return zero for empty coredump")
    void shouldReturnZeroForEmptyCoredump() {
      final WasmCoreDump coreDump = WasmCoreDump.builder().build();
      assertEquals(0, coreDump.getSize(), "Size should be zero");
    }

    @Test
    @DisplayName("should include serialized data size")
    void shouldIncludeSerializedDataSize() {
      final byte[] data = new byte[100];
      final WasmCoreDump coreDump = WasmCoreDump.builder().serializedData(data).build();
      assertEquals(100, coreDump.getSize(), "Size should include serialized data");
    }

    @Test
    @DisplayName("should include memory segment sizes")
    void shouldIncludeMemorySegmentSizes() {
      final CoreDumpMemory memory =
          CoreDumpMemory.builder()
              .sizeInPages(1)
              .addSegment(0, new byte[50])
              .addSegment(100, new byte[25])
              .build();
      final WasmCoreDump coreDump = WasmCoreDump.builder().addMemory(memory).build();
      assertEquals(75, coreDump.getSize(), "Size should include memory segments");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return meaningful string representation")
    void shouldReturnMeaningfulStringRepresentation() {
      final WasmCoreDump coreDump =
          WasmCoreDump.builder()
              .name("test-dump")
              .trapMessage("trap occurred")
              .addModule("module.wasm")
              .build();
      final String str = coreDump.toString();
      assertTrue(str.contains("WasmCoreDump"), "Should contain class name");
      assertTrue(str.contains("test-dump"), "Should contain name");
      assertTrue(str.contains("trap occurred"), "Should contain trap message");
    }
  }

  @Nested
  @DisplayName("Full Integration Tests")
  class FullIntegrationTests {

    @Test
    @DisplayName("should build complete coredump")
    void shouldBuildCompleteCoredump() {
      final CoreDumpFrame frame =
          CoreDumpFrame.builder()
              .funcIndex(0)
              .funcName("main")
              .moduleIndex(0)
              .moduleName("test.wasm")
              .offset(100)
              .trapFrame(true)
              .build();

      final CoreDumpInstance instance =
          CoreDumpInstance.builder()
              .index(0)
              .moduleIndex(0)
              .name("test-instance")
              .memoryCount(1)
              .globalCount(2)
              .tableCount(0)
              .build();

      final CoreDumpGlobal global =
          CoreDumpGlobal.builder()
              .instanceIndex(0)
              .globalIndex(0)
              .name("counter")
              .i32Value(42)
              .mutable(true)
              .build();

      final CoreDumpMemory memory =
          CoreDumpMemory.builder()
              .instanceIndex(0)
              .memoryIndex(0)
              .name("memory")
              .sizeInPages(1)
              .minPages(1)
              .maxPages(10L)
              .addSegment(0, new byte[] {0x01, 0x02, 0x03})
              .build();

      final WasmCoreDump coreDump =
          WasmCoreDump.builder()
              .name("test-coredump")
              .trapMessage("unreachable")
              .addModule("test.wasm")
              .addFrame(frame)
              .addInstance(instance)
              .addGlobal(global)
              .addMemory(memory)
              .serializedData(new byte[] {0x00, 0x61, 0x73, 0x6D})
              .build();

      assertEquals("test-coredump", coreDump.getName(), "Name should match");
      assertEquals("unreachable", coreDump.getTrapMessage(), "Trap message should match");
      assertEquals(1, coreDump.getModules().size(), "Should have 1 module");
      assertEquals(1, coreDump.getFrames().size(), "Should have 1 frame");
      assertEquals(1, coreDump.getInstances().size(), "Should have 1 instance");
      assertEquals(1, coreDump.getGlobals().size(), "Should have 1 global");
      assertEquals(1, coreDump.getMemories().size(), "Should have 1 memory");
      assertTrue(coreDump.getSize() > 0, "Size should be positive");
    }
  }
}
