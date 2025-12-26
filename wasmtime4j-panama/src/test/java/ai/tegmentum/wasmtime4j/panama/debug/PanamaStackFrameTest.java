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

package ai.tegmentum.wasmtime4j.panama.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link PanamaStackFrame}.
 */
@DisplayName("PanamaStackFrame Tests")
class PanamaStackFrameTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaStackFrame should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaStackFrame.class.getModifiers()),
          "PanamaStackFrame should be final");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should create stack frame with all fields")
    void builderShouldCreateStackFrameWithAllFields() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("testFunction")
          .functionIndex(5)
          .moduleIndex(1)
          .moduleName("testModule")
          .instructionOffset(0x1000L)
          .lineNumber(42)
          .columnNumber(10)
          .sourceFile("test.wat")
          .build();

      assertEquals(0, frame.getFrameIndex(), "Frame index should match");
      assertEquals("testFunction", frame.getFunctionName(), "Function name should match");
      assertEquals(5, frame.getFunctionIndex(), "Function index should match");
      assertEquals(1, frame.getModuleIndex(), "Module index should match");
      assertEquals("testModule", frame.getModuleName(), "Module name should match");
      assertEquals(0x1000L, frame.getInstructionOffset(), "Instruction offset should match");
      assertEquals(42, frame.getLineNumber(), "Line number should match");
      assertEquals(10, frame.getColumnNumber(), "Column number should match");
      assertEquals("test.wat", frame.getSourceFile(), "Source file should match");
    }

    @Test
    @DisplayName("Builder should create stack frame with default values")
    void builderShouldCreateStackFrameWithDefaultValues() {
      final PanamaStackFrame frame = PanamaStackFrame.builder().build();

      assertEquals(0, frame.getFrameIndex(), "Default frame index should be 0");
      assertNull(frame.getFunctionName(), "Default function name should be null");
      assertEquals(0, frame.getFunctionIndex(), "Default function index should be 0");
      assertEquals(0, frame.getModuleIndex(), "Default module index should be 0");
      assertNull(frame.getModuleName(), "Default module name should be null");
      assertEquals(0L, frame.getInstructionOffset(), "Default offset should be 0");
      assertEquals(-1, frame.getLineNumber(), "Default line number should be -1");
      assertEquals(-1, frame.getColumnNumber(), "Default column number should be -1");
      assertNull(frame.getSourceFile(), "Default source file should be null");
    }

    @Test
    @DisplayName("Builder should allow chaining all methods")
    void builderShouldAllowChainingAllMethods() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .frameIndex(1)
          .functionName("func")
          .functionIndex(2)
          .moduleIndex(3)
          .moduleName("mod")
          .instructionOffset(100L)
          .lineNumber(10)
          .columnNumber(5)
          .sourceFile("file.wat")
          .build();

      assertNotNull(frame, "Chained builder should produce frame");
    }

    @Test
    @DisplayName("Builder should be reusable for multiple builds")
    void builderShouldBeReusableForMultipleBuilds() {
      final PanamaStackFrame.Builder builder = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("func1");

      final PanamaStackFrame frame1 = builder.build();

      builder.frameIndex(1).functionName("func2");

      final PanamaStackFrame frame2 = builder.build();

      assertEquals(0, frame1.getFrameIndex(), "First frame should have index 0");
      assertEquals("func1", frame1.getFunctionName(), "First frame should have name func1");
      assertEquals(1, frame2.getFrameIndex(), "Second frame should have index 1");
      assertEquals("func2", frame2.getFunctionName(), "Second frame should have name func2");
    }
  }

  @Nested
  @DisplayName("hasSourceInfo Tests")
  class HasSourceInfoTests {

    @Test
    @DisplayName("hasSourceInfo should return true when source file and line are set")
    void hasSourceInfoShouldReturnTrueWhenSourceFileAndLineAreSet() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .sourceFile("test.wat")
          .lineNumber(10)
          .build();

      assertTrue(frame.hasSourceInfo(), "Should have source info");
    }

    @Test
    @DisplayName("hasSourceInfo should return false when source file is null")
    void hasSourceInfoShouldReturnFalseWhenSourceFileIsNull() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .lineNumber(10)
          .build();

      assertFalse(frame.hasSourceInfo(), "Should not have source info without file");
    }

    @Test
    @DisplayName("hasSourceInfo should return false when line number is negative")
    void hasSourceInfoShouldReturnFalseWhenLineNumberIsNegative() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .sourceFile("test.wat")
          .lineNumber(-1)
          .build();

      assertFalse(frame.hasSourceInfo(), "Should not have source info with negative line");
    }

    @Test
    @DisplayName("hasSourceInfo should return true when line number is zero")
    void hasSourceInfoShouldReturnTrueWhenLineNumberIsZero() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .sourceFile("test.wat")
          .lineNumber(0)
          .build();

      assertTrue(frame.hasSourceInfo(), "Should have source info with line 0");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include frame index and function name")
    void toStringShouldIncludeFrameIndexAndFunctionName() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("myFunction")
          .instructionOffset(0x100L)
          .build();

      final String str = frame.toString();
      assertTrue(str.contains("#0"), "Should contain frame index");
      assertTrue(str.contains("myFunction"), "Should contain function name");
      assertTrue(str.contains("offset"), "Should contain offset");
    }

    @Test
    @DisplayName("toString should show function index when name is null")
    void toStringShouldShowFunctionIndexWhenNameIsNull() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .frameIndex(1)
          .functionIndex(42)
          .build();

      final String str = frame.toString();
      assertTrue(str.contains("#1"), "Should contain frame index");
      assertTrue(str.contains("<function 42>"), "Should contain function index placeholder");
    }

    @Test
    @DisplayName("toString should include source info when available")
    void toStringShouldIncludeSourceInfoWhenAvailable() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("func")
          .instructionOffset(100L)
          .sourceFile("test.wat")
          .lineNumber(42)
          .columnNumber(10)
          .build();

      final String str = frame.toString();
      assertTrue(str.contains("test.wat"), "Should contain source file");
      assertTrue(str.contains(":42"), "Should contain line number");
      assertTrue(str.contains(":10"), "Should contain column number");
    }

    @Test
    @DisplayName("toString should not include source info when not available")
    void toStringShouldNotIncludeSourceInfoWhenNotAvailable() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("func")
          .build();

      final String str = frame.toString();
      assertFalse(str.contains("("), "Should not contain source info parentheses");
    }

    @Test
    @DisplayName("toString should handle line without column")
    void toStringShouldHandleLineWithoutColumn() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("func")
          .instructionOffset(100L)
          .sourceFile("test.wat")
          .lineNumber(42)
          .columnNumber(-1)
          .build();

      final String str = frame.toString();
      assertTrue(str.contains("test.wat:42"), "Should contain file and line");
      // Column should not appear after line 42 (should be just :42, not :42:-1)
      assertFalse(str.contains(":-1"), "Should not contain negative column");
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("equals should return true for same values")
    void equalsShouldReturnTrueForSameValues() {
      final PanamaStackFrame frame1 = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("func")
          .functionIndex(5)
          .moduleIndex(1)
          .instructionOffset(0x1000L)
          .build();

      final PanamaStackFrame frame2 = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("func")
          .functionIndex(5)
          .moduleIndex(1)
          .instructionOffset(0x1000L)
          .build();

      assertEquals(frame1, frame2, "Frames with same values should be equal");
    }

    @Test
    @DisplayName("equals should ignore non-key fields")
    void equalsShouldIgnoreNonKeyFields() {
      final PanamaStackFrame frame1 = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("func")
          .functionIndex(5)
          .moduleIndex(1)
          .instructionOffset(0x1000L)
          .sourceFile("file1.wat")
          .lineNumber(10)
          .build();

      final PanamaStackFrame frame2 = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("func")
          .functionIndex(5)
          .moduleIndex(1)
          .instructionOffset(0x1000L)
          .sourceFile("file2.wat")
          .lineNumber(20)
          .build();

      assertEquals(frame1, frame2,
          "Frames should be equal regardless of source info (by design)");
    }

    @Test
    @DisplayName("equals should return false for different frame index")
    void equalsShouldReturnFalseForDifferentFrameIndex() {
      final PanamaStackFrame frame1 = PanamaStackFrame.builder().frameIndex(0).build();
      final PanamaStackFrame frame2 = PanamaStackFrame.builder().frameIndex(1).build();

      assertNotEquals(frame1, frame2, "Frames with different indices should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different function name")
    void equalsShouldReturnFalseForDifferentFunctionName() {
      final PanamaStackFrame frame1 = PanamaStackFrame.builder().functionName("func1").build();
      final PanamaStackFrame frame2 = PanamaStackFrame.builder().functionName("func2").build();

      assertNotEquals(frame1, frame2, "Frames with different names should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different function index")
    void equalsShouldReturnFalseForDifferentFunctionIndex() {
      final PanamaStackFrame frame1 = PanamaStackFrame.builder().functionIndex(1).build();
      final PanamaStackFrame frame2 = PanamaStackFrame.builder().functionIndex(2).build();

      assertNotEquals(frame1, frame2,
          "Frames with different function indices should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different module index")
    void equalsShouldReturnFalseForDifferentModuleIndex() {
      final PanamaStackFrame frame1 = PanamaStackFrame.builder().moduleIndex(0).build();
      final PanamaStackFrame frame2 = PanamaStackFrame.builder().moduleIndex(1).build();

      assertNotEquals(frame1, frame2, "Frames with different module indices should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different instruction offset")
    void equalsShouldReturnFalseForDifferentInstructionOffset() {
      final PanamaStackFrame frame1 = PanamaStackFrame.builder().instructionOffset(100L).build();
      final PanamaStackFrame frame2 = PanamaStackFrame.builder().instructionOffset(200L).build();

      assertNotEquals(frame1, frame2, "Frames with different offsets should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final PanamaStackFrame frame = PanamaStackFrame.builder().build();
      assertNotEquals(null, frame, "Should not be equal to null");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final PanamaStackFrame frame = PanamaStackFrame.builder().build();
      assertNotEquals("frame", frame, "Should not be equal to String");
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final PanamaStackFrame frame = PanamaStackFrame.builder().build();
      assertEquals(frame, frame, "Should be equal to itself");
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void hashCodeShouldBeConsistentWithEquals() {
      final PanamaStackFrame frame1 = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("func")
          .functionIndex(5)
          .moduleIndex(1)
          .instructionOffset(0x1000L)
          .build();

      final PanamaStackFrame frame2 = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("func")
          .functionIndex(5)
          .moduleIndex(1)
          .instructionOffset(0x1000L)
          .build();

      assertEquals(frame1.hashCode(), frame2.hashCode(),
          "Equal frames should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should be stable across multiple calls")
    void hashCodeShouldBeStableAcrossMultipleCalls() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .frameIndex(0)
          .functionName("func")
          .build();

      final int hash1 = frame.hashCode();
      final int hash2 = frame.hashCode();
      final int hash3 = frame.hashCode();

      assertEquals(hash1, hash2, "Hash should be stable");
      assertEquals(hash2, hash3, "Hash should be stable");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle negative frame index")
    void shouldHandleNegativeFrameIndex() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .frameIndex(-1)
          .build();

      assertEquals(-1, frame.getFrameIndex(), "Should handle negative frame index");
    }

    @Test
    @DisplayName("Should handle maximum instruction offset")
    void shouldHandleMaximumInstructionOffset() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .instructionOffset(Long.MAX_VALUE)
          .build();

      assertEquals(Long.MAX_VALUE, frame.getInstructionOffset(),
          "Should handle max long offset");
    }

    @Test
    @DisplayName("Should handle empty strings")
    void shouldHandleEmptyStrings() {
      final PanamaStackFrame frame = PanamaStackFrame.builder()
          .functionName("")
          .moduleName("")
          .sourceFile("")
          .lineNumber(0)
          .build();

      assertEquals("", frame.getFunctionName(), "Should handle empty function name");
      assertEquals("", frame.getModuleName(), "Should handle empty module name");
      assertEquals("", frame.getSourceFile(), "Should handle empty source file");
      // Note: hasSourceInfo returns true for empty string sourceFile with line >= 0
      assertTrue(frame.hasSourceInfo(), "Should have source info with empty file and line 0");
    }
  }
}
