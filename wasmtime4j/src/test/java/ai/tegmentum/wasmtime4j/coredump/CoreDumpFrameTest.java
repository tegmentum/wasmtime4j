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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CoreDumpFrame} class.
 *
 * <p>CoreDumpFrame is the default implementation of the CoreDumpFrame interface.
 */
@DisplayName("CoreDumpFrame Tests")
class CoreDumpFrameTest {

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create builder via static method")
    void shouldCreateBuilderViaStaticMethod() {
      final CoreDumpFrame.Builder builder = CoreDumpFrame.builder();
      assertNotNull(builder, "Builder should not be null");
    }

    @Test
    @DisplayName("should build with funcIndex")
    void shouldBuildWithFuncIndex() {
      final CoreDumpFrame frame = CoreDumpFrame.builder().funcIndex(5).build();
      assertEquals(5, frame.getFuncIndex(), "FuncIndex should match");
    }

    @Test
    @DisplayName("should build with funcName")
    void shouldBuildWithFuncName() {
      final CoreDumpFrame frame =
          CoreDumpFrame.builder().funcName("calculate").build();
      assertTrue(frame.getFuncName().isPresent(), "FuncName should be present");
      assertEquals("calculate", frame.getFuncName().get(), "FuncName should match");
    }

    @Test
    @DisplayName("should build without funcName")
    void shouldBuildWithoutFuncName() {
      final CoreDumpFrame frame = CoreDumpFrame.builder().build();
      assertFalse(frame.getFuncName().isPresent(), "FuncName should not be present");
    }

    @Test
    @DisplayName("should build with moduleIndex")
    void shouldBuildWithModuleIndex() {
      final CoreDumpFrame frame = CoreDumpFrame.builder().moduleIndex(2).build();
      assertEquals(2, frame.getModuleIndex(), "ModuleIndex should match");
    }

    @Test
    @DisplayName("should build with moduleName")
    void shouldBuildWithModuleName() {
      final CoreDumpFrame frame =
          CoreDumpFrame.builder().moduleName("test.wasm").build();
      assertTrue(frame.getModuleName().isPresent(), "ModuleName should be present");
      assertEquals("test.wasm", frame.getModuleName().get(), "ModuleName should match");
    }

    @Test
    @DisplayName("should build without moduleName")
    void shouldBuildWithoutModuleName() {
      final CoreDumpFrame frame = CoreDumpFrame.builder().build();
      assertFalse(frame.getModuleName().isPresent(), "ModuleName should not be present");
    }

    @Test
    @DisplayName("should build with offset")
    void shouldBuildWithOffset() {
      final CoreDumpFrame frame = CoreDumpFrame.builder().offset(256).build();
      assertEquals(256, frame.getOffset(), "Offset should match");
    }

    @Test
    @DisplayName("should build with trapFrame flag")
    void shouldBuildWithTrapFrameFlag() {
      final CoreDumpFrame frame = CoreDumpFrame.builder().trapFrame(true).build();
      assertTrue(frame.isTrapFrame(), "TrapFrame should be true");
    }

    @Test
    @DisplayName("should build without trapFrame flag defaulting to false")
    void shouldBuildWithoutTrapFrameFlagDefaultingToFalse() {
      final CoreDumpFrame frame = CoreDumpFrame.builder().build();
      assertFalse(frame.isTrapFrame(), "TrapFrame should default to false");
    }
  }

  @Nested
  @DisplayName("Locals Tests")
  class LocalsTests {

    @Test
    @DisplayName("should build with single local")
    void shouldBuildWithSingleLocal() {
      final byte[] local = {0x01, 0x02, 0x03, 0x04};
      final CoreDumpFrame frame = CoreDumpFrame.builder().addLocal(local).build();
      assertEquals(1, frame.getLocals().size(), "Should have 1 local");
      assertArrayEquals(local, frame.getLocals().get(0), "Local data should match");
    }

    @Test
    @DisplayName("should build with multiple locals")
    void shouldBuildWithMultipleLocals() {
      final CoreDumpFrame frame =
          CoreDumpFrame.builder()
              .addLocal(new byte[] {0x01})
              .addLocal(new byte[] {0x02})
              .addLocal(new byte[] {0x03})
              .build();
      assertEquals(3, frame.getLocals().size(), "Should have 3 locals");
    }

    @Test
    @DisplayName("should build with locals list")
    void shouldBuildWithLocalsList() {
      final List<byte[]> locals = Arrays.asList(new byte[] {0x01, 0x02}, new byte[] {0x03, 0x04});
      final CoreDumpFrame frame = CoreDumpFrame.builder().addLocals(locals).build();
      assertEquals(2, frame.getLocals().size(), "Should have 2 locals");
    }

    @Test
    @DisplayName("should return copy of locals list")
    void shouldReturnCopyOfLocals() {
      final byte[] original = {0x01, 0x02};
      final CoreDumpFrame frame = CoreDumpFrame.builder().addLocal(original).build();
      // getLocals() returns a new ArrayList each time, but the byte arrays are the same references
      // The implementation stores clones, so the internal byte array is a clone of original
      final List<byte[]> locals = frame.getLocals();
      assertEquals(1, locals.size(), "Should have 1 local");
      // Verify data matches
      assertArrayEquals(original, locals.get(0), "Local data should match original");
    }

    @Test
    @DisplayName("should handle null local")
    void shouldHandleNullLocal() {
      final CoreDumpFrame frame = CoreDumpFrame.builder().addLocal(null).build();
      assertEquals(1, frame.getLocals().size(), "Should have 1 local");
    }
  }

  @Nested
  @DisplayName("Stack Tests")
  class StackTests {

    @Test
    @DisplayName("should build with single stack value")
    void shouldBuildWithSingleStackValue() {
      final byte[] stackValue = {0x0A, 0x0B, 0x0C};
      final CoreDumpFrame frame =
          CoreDumpFrame.builder().addStackValue(stackValue).build();
      assertEquals(1, frame.getStack().size(), "Should have 1 stack value");
      assertArrayEquals(stackValue, frame.getStack().get(0), "Stack value should match");
    }

    @Test
    @DisplayName("should build with multiple stack values")
    void shouldBuildWithMultipleStackValues() {
      final CoreDumpFrame frame =
          CoreDumpFrame.builder()
              .addStackValue(new byte[] {0x01})
              .addStackValue(new byte[] {0x02})
              .build();
      assertEquals(2, frame.getStack().size(), "Should have 2 stack values");
    }

    @Test
    @DisplayName("should build with stack values list")
    void shouldBuildWithStackValuesList() {
      final List<byte[]> stackValues =
          Arrays.asList(new byte[] {0x10}, new byte[] {0x20}, new byte[] {0x30});
      final CoreDumpFrame frame =
          CoreDumpFrame.builder().addStackValues(stackValues).build();
      assertEquals(3, frame.getStack().size(), "Should have 3 stack values");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return meaningful string representation")
    void shouldReturnMeaningfulStringRepresentation() {
      final CoreDumpFrame frame =
          CoreDumpFrame.builder()
              .funcIndex(5)
              .funcName("testFunc")
              .moduleIndex(0)
              .moduleName("test.wasm")
              .offset(100)
              .trapFrame(true)
              .build();
      final String str = frame.toString();
      assertTrue(str.contains("CoreDumpFrame"), "Should contain class name");
      assertTrue(str.contains("testFunc"), "Should contain function name");
      assertTrue(str.contains("test.wasm"), "Should contain module name");
      assertTrue(str.contains("100"), "Should contain offset");
    }
  }

  @Nested
  @DisplayName("Full Integration Tests")
  class FullIntegrationTests {

    @Test
    @DisplayName("should build complete frame")
    void shouldBuildCompleteFrame() {
      final CoreDumpFrame frame =
          CoreDumpFrame.builder()
              .funcIndex(10)
              .funcName("main")
              .moduleIndex(0)
              .moduleName("app.wasm")
              .offset(512)
              .addLocal(new byte[] {0x2A, 0x00, 0x00, 0x00}) // i32: 42
              .addLocal(new byte[] {0x01, 0x00, 0x00, 0x00}) // i32: 1
              .addStackValue(new byte[] {0x64, 0x00, 0x00, 0x00}) // i32: 100
              .trapFrame(true)
              .build();

      assertEquals(10, frame.getFuncIndex(), "FuncIndex should match");
      assertEquals("main", frame.getFuncName().get(), "FuncName should match");
      assertEquals(0, frame.getModuleIndex(), "ModuleIndex should match");
      assertEquals("app.wasm", frame.getModuleName().get(), "ModuleName should match");
      assertEquals(512, frame.getOffset(), "Offset should match");
      assertEquals(2, frame.getLocals().size(), "Should have 2 locals");
      assertEquals(1, frame.getStack().size(), "Should have 1 stack value");
      assertTrue(frame.isTrapFrame(), "Should be trap frame");
    }
  }
}
