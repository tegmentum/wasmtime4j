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

package ai.tegmentum.wasmtime4j.jni.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link JniStackFrame}. */
@DisplayName("JniStackFrame Tests")
class JniStackFrameTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniStackFrame should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniStackFrame.class.getModifiers()),
          "JniStackFrame should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should create stack frame with all fields")
    void constructorShouldCreateStackFrameWithAllFields() {
      final List<JniVariable> variables =
          Arrays.asList(
              JniVariable.local("x", "i32", JniVariableValue.i32(42), 0),
              JniVariable.local("y", "i64", JniVariableValue.i64(100L), 1));

      final JniStackFrame frame =
          new JniStackFrame(5, "testFunction", 0x1000L, 42, 10, "test.wat", variables);

      assertEquals(5, frame.getFunctionIndex(), "Function index should match");
      assertEquals("testFunction", frame.getFunctionName(), "Function name should match");
      assertEquals(0x1000L, frame.getInstructionOffset(), "Instruction offset should match");
      assertEquals(42, frame.getLineNumber(), "Line number should match");
      assertEquals(10, frame.getColumnNumber(), "Column number should match");
      assertEquals("test.wat", frame.getSourceFile(), "Source file should match");
      assertEquals(2, frame.getVariables().size(), "Should have 2 variables");
    }

    @Test
    @DisplayName("Constructor should accept null functionName")
    void constructorShouldAcceptNullFunctionName() {
      final JniStackFrame frame = new JniStackFrame(0, null, 0L, 0, 0, null, null);

      assertNull(frame.getFunctionName(), "Function name should be null");
    }

    @Test
    @DisplayName("Constructor should accept null sourceFile")
    void constructorShouldAcceptNullSourceFile() {
      final JniStackFrame frame = new JniStackFrame(0, "func", 0L, 0, 0, null, null);

      assertNull(frame.getSourceFile(), "Source file should be null");
    }

    @Test
    @DisplayName("Constructor should accept null variables list")
    void constructorShouldAcceptNullVariablesList() {
      final JniStackFrame frame = new JniStackFrame(0, "func", 0L, 0, 0, null, null);

      assertNotNull(frame.getVariables(), "Variables list should not be null");
      assertTrue(frame.getVariables().isEmpty(), "Variables list should be empty");
    }

    @Test
    @DisplayName("Constructor should create defensive copy of variables list")
    void constructorShouldCreateDefensiveCopyOfVariablesList() {
      final List<JniVariable> variables = new ArrayList<>();
      variables.add(JniVariable.local("x", "i32", JniVariableValue.i32(1), 0));

      final JniStackFrame frame = new JniStackFrame(0, "func", 0L, 0, 0, null, variables);

      // Modify original list
      variables.add(JniVariable.local("y", "i32", JniVariableValue.i32(2), 1));

      assertEquals(1, frame.getVariables().size(), "Frame should have 1 variable (defensive copy)");
    }

    @Test
    @DisplayName("getVariables should return unmodifiable list")
    void getVariablesShouldReturnUnmodifiableList() {
      final JniStackFrame frame =
          new JniStackFrame(
              0,
              "func",
              0L,
              0,
              0,
              null,
              Collections.singletonList(JniVariable.local("x", "i32", JniVariableValue.i32(1), 0)));

      assertThrows(
          UnsupportedOperationException.class,
          () -> frame.getVariables().add(JniVariable.local("y", "i32", JniVariableValue.i32(2), 1)),
          "Should not be able to modify returned list");
    }
  }

  @Nested
  @DisplayName("fromNative Factory Tests")
  class FromNativeTests {

    @Test
    @DisplayName("fromNative should create stack frame with empty variables")
    void fromNativeShouldCreateStackFrameWithEmptyVariables() {
      final JniStackFrame frame =
          JniStackFrame.fromNative(10, "nativeFunc", 0x2000L, 100, 5, "native.wat");

      assertEquals(10, frame.getFunctionIndex(), "Function index should match");
      assertEquals("nativeFunc", frame.getFunctionName(), "Function name should match");
      assertEquals(0x2000L, frame.getInstructionOffset(), "Instruction offset should match");
      assertEquals(100, frame.getLineNumber(), "Line number should match");
      assertEquals(5, frame.getColumnNumber(), "Column number should match");
      assertEquals("native.wat", frame.getSourceFile(), "Source file should match");
      assertTrue(frame.getVariables().isEmpty(), "Variables should be empty from native");
    }

    @Test
    @DisplayName("fromNative should handle null functionName")
    void fromNativeShouldHandleNullFunctionName() {
      final JniStackFrame frame = JniStackFrame.fromNative(0, null, 0L, 0, 0, null);

      assertNull(frame.getFunctionName(), "Function name should be null");
    }

    @Test
    @DisplayName("fromNative should handle large instruction offset")
    void fromNativeShouldHandleLargeInstructionOffset() {
      final JniStackFrame frame = JniStackFrame.fromNative(0, "func", Long.MAX_VALUE, 0, 0, null);

      assertEquals(Long.MAX_VALUE, frame.getInstructionOffset(), "Should handle max long offset");
    }

    @Test
    @DisplayName("fromNative should handle negative function index")
    void fromNativeShouldHandleNegativeFunctionIndex() {
      final JniStackFrame frame = JniStackFrame.fromNative(-1, "func", 0L, 0, 0, null);

      assertEquals(-1, frame.getFunctionIndex(), "Should handle negative function index");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should create stack frame with all fields")
    void builderShouldCreateStackFrameWithAllFields() {
      final JniStackFrame frame =
          JniStackFrame.builder()
              .functionIndex(5)
              .functionName("builderFunc")
              .instructionOffset(0x3000L)
              .lineNumber(50)
              .columnNumber(8)
              .sourceFile("builder.wat")
              .build();

      assertEquals(5, frame.getFunctionIndex(), "Function index should match");
      assertEquals("builderFunc", frame.getFunctionName(), "Function name should match");
      assertEquals(0x3000L, frame.getInstructionOffset(), "Instruction offset should match");
      assertEquals(50, frame.getLineNumber(), "Line number should match");
      assertEquals(8, frame.getColumnNumber(), "Column number should match");
      assertEquals("builder.wat", frame.getSourceFile(), "Source file should match");
    }

    @Test
    @DisplayName("Builder should create stack frame with default values")
    void builderShouldCreateStackFrameWithDefaultValues() {
      final JniStackFrame frame = JniStackFrame.builder().build();

      assertEquals(0, frame.getFunctionIndex(), "Default function index should be 0");
      assertNull(frame.getFunctionName(), "Default function name should be null");
      assertEquals(0L, frame.getInstructionOffset(), "Default offset should be 0");
      assertEquals(0, frame.getLineNumber(), "Default line number should be 0");
      assertEquals(0, frame.getColumnNumber(), "Default column number should be 0");
      assertNull(frame.getSourceFile(), "Default source file should be null");
      assertTrue(frame.getVariables().isEmpty(), "Default variables should be empty");
    }

    @Test
    @DisplayName("Builder should add individual variables")
    void builderShouldAddIndividualVariables() {
      final JniVariable var1 = JniVariable.local("a", "i32", JniVariableValue.i32(1), 0);
      final JniVariable var2 = JniVariable.local("b", "i32", JniVariableValue.i32(2), 1);

      final JniStackFrame frame =
          JniStackFrame.builder().addVariable(var1).addVariable(var2).build();

      assertEquals(2, frame.getVariables().size(), "Should have 2 variables");
      assertEquals(var1, frame.getVariables().get(0), "First variable should match");
      assertEquals(var2, frame.getVariables().get(1), "Second variable should match");
    }

    @Test
    @DisplayName("Builder should set variables list")
    void builderShouldSetVariablesList() {
      final List<JniVariable> vars =
          Arrays.asList(
              JniVariable.local("x", "i32", JniVariableValue.i32(10), 0),
              JniVariable.parameter("y", "i64", JniVariableValue.i64(20L), 0),
              JniVariable.global("g", "f32", JniVariableValue.f32(3.14f), 0, false));

      final JniStackFrame frame = JniStackFrame.builder().variables(vars).build();

      assertEquals(3, frame.getVariables().size(), "Should have 3 variables");
    }

    @Test
    @DisplayName("Builder variables() should create defensive copy")
    void builderVariablesShouldCreateDefensiveCopy() {
      final List<JniVariable> vars = new ArrayList<>();
      vars.add(JniVariable.local("x", "i32", JniVariableValue.i32(1), 0));

      final JniStackFrame.Builder builder = JniStackFrame.builder().variables(vars);

      // Modify original list
      vars.add(JniVariable.local("y", "i32", JniVariableValue.i32(2), 1));

      final JniStackFrame frame = builder.build();
      assertEquals(1, frame.getVariables().size(), "Should have 1 variable (defensive copy)");
    }

    @Test
    @DisplayName("Builder variables() should handle null")
    void builderVariablesShouldHandleNull() {
      final JniStackFrame frame = JniStackFrame.builder().variables(null).build();

      assertNotNull(frame.getVariables(), "Variables should not be null");
      assertTrue(frame.getVariables().isEmpty(), "Variables should be empty");
    }

    @Test
    @DisplayName("Builder should be reusable for multiple builds")
    void builderShouldBeReusableForMultipleBuilds() {
      final JniStackFrame.Builder builder =
          JniStackFrame.builder().functionIndex(1).functionName("func1");

      final JniStackFrame frame1 = builder.build();

      builder.functionIndex(2).functionName("func2");

      final JniStackFrame frame2 = builder.build();

      assertEquals(1, frame1.getFunctionIndex(), "First frame should have index 1");
      assertEquals("func1", frame1.getFunctionName(), "First frame should have name func1");
      assertEquals(2, frame2.getFunctionIndex(), "Second frame should have index 2");
      assertEquals("func2", frame2.getFunctionName(), "Second frame should have name func2");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include function index and offset")
    void toStringShouldIncludeFunctionIndexAndOffset() {
      final JniStackFrame frame =
          JniStackFrame.builder().functionIndex(5).instructionOffset(0x1000L).build();

      final String str = frame.toString();
      assertTrue(str.contains("functionIndex=5"), "Should contain function index");
      assertTrue(
          str.contains("offset=4096") || str.contains("offset=0x1000"), "Should contain offset");
    }

    @Test
    @DisplayName("toString should include function name when set")
    void toStringShouldIncludeFunctionNameWhenSet() {
      final JniStackFrame frame = JniStackFrame.builder().functionName("myFunction").build();

      final String str = frame.toString();
      assertTrue(str.contains("functionName='myFunction'"), "Should contain function name");
    }

    @Test
    @DisplayName("toString should not include function name when null")
    void toStringShouldNotIncludeFunctionNameWhenNull() {
      final JniStackFrame frame = JniStackFrame.builder().build();

      final String str = frame.toString();
      assertFalse(str.contains("functionName"), "Should not contain function name field");
    }

    @Test
    @DisplayName("toString should include source location when set")
    void toStringShouldIncludeSourceLocationWhenSet() {
      final JniStackFrame frame =
          JniStackFrame.builder().sourceFile("test.wat").lineNumber(42).columnNumber(10).build();

      final String str = frame.toString();
      assertTrue(str.contains("source='test.wat:42:10'"), "Should contain source location");
    }

    @Test
    @DisplayName("toString should include variable count when variables exist")
    void toStringShouldIncludeVariableCountWhenVariablesExist() {
      final JniStackFrame frame =
          JniStackFrame.builder()
              .addVariable(JniVariable.local("x", "i32", JniVariableValue.i32(1), 0))
              .addVariable(JniVariable.local("y", "i32", JniVariableValue.i32(2), 1))
              .build();

      final String str = frame.toString();
      assertTrue(str.contains("vars=2"), "Should contain variable count");
    }

    @Test
    @DisplayName("toString should not include vars when empty")
    void toStringShouldNotIncludeVarsWhenEmpty() {
      final JniStackFrame frame = JniStackFrame.builder().build();

      final String str = frame.toString();
      assertFalse(str.contains("vars="), "Should not contain vars field");
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("equals should return true for same values")
    void equalsShouldReturnTrueForSameValues() {
      final JniStackFrame frame1 =
          JniStackFrame.builder()
              .functionIndex(5)
              .functionName("func")
              .instructionOffset(0x1000L)
              .lineNumber(10)
              .columnNumber(5)
              .sourceFile("test.wat")
              .build();

      final JniStackFrame frame2 =
          JniStackFrame.builder()
              .functionIndex(5)
              .functionName("func")
              .instructionOffset(0x1000L)
              .lineNumber(10)
              .columnNumber(5)
              .sourceFile("test.wat")
              .build();

      assertEquals(frame1, frame2, "Frames with same values should be equal");
    }

    @Test
    @DisplayName("equals should ignore variables")
    void equalsShouldIgnoreVariables() {
      final JniStackFrame frame1 =
          JniStackFrame.builder()
              .functionIndex(5)
              .addVariable(JniVariable.local("x", "i32", JniVariableValue.i32(1), 0))
              .build();

      final JniStackFrame frame2 =
          JniStackFrame.builder()
              .functionIndex(5)
              .addVariable(JniVariable.local("y", "i64", JniVariableValue.i64(2L), 1))
              .build();

      assertEquals(frame1, frame2, "Frames should be equal regardless of variables (by design)");
    }

    @Test
    @DisplayName("equals should return false for different function index")
    void equalsShouldReturnFalseForDifferentFunctionIndex() {
      final JniStackFrame frame1 = JniStackFrame.builder().functionIndex(1).build();
      final JniStackFrame frame2 = JniStackFrame.builder().functionIndex(2).build();

      assertNotEquals(frame1, frame2, "Frames with different indices should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different instruction offset")
    void equalsShouldReturnFalseForDifferentInstructionOffset() {
      final JniStackFrame frame1 = JniStackFrame.builder().instructionOffset(100L).build();
      final JniStackFrame frame2 = JniStackFrame.builder().instructionOffset(200L).build();

      assertNotEquals(frame1, frame2, "Frames with different offsets should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different function name")
    void equalsShouldReturnFalseForDifferentFunctionName() {
      final JniStackFrame frame1 = JniStackFrame.builder().functionName("func1").build();
      final JniStackFrame frame2 = JniStackFrame.builder().functionName("func2").build();

      assertNotEquals(frame1, frame2, "Frames with different names should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final JniStackFrame frame = JniStackFrame.builder().build();
      assertNotEquals(null, frame, "Should not be equal to null");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final JniStackFrame frame = JniStackFrame.builder().build();
      assertNotEquals("frame", frame, "Should not be equal to String");
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final JniStackFrame frame = JniStackFrame.builder().build();
      assertEquals(frame, frame, "Should be equal to itself");
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void hashCodeShouldBeConsistentWithEquals() {
      final JniStackFrame frame1 =
          JniStackFrame.builder()
              .functionIndex(5)
              .functionName("func")
              .instructionOffset(0x1000L)
              .lineNumber(10)
              .columnNumber(5)
              .build();

      final JniStackFrame frame2 =
          JniStackFrame.builder()
              .functionIndex(5)
              .functionName("func")
              .instructionOffset(0x1000L)
              .lineNumber(10)
              .columnNumber(5)
              .build();

      assertEquals(frame1.hashCode(), frame2.hashCode(), "Equal frames should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should be stable across multiple calls")
    void hashCodeShouldBeStableAcrossMultipleCalls() {
      final JniStackFrame frame =
          JniStackFrame.builder().functionIndex(5).functionName("func").build();

      final int hash1 = frame.hashCode();
      final int hash2 = frame.hashCode();
      final int hash3 = frame.hashCode();

      assertEquals(hash1, hash2, "Hash should be stable");
      assertEquals(hash2, hash3, "Hash should be stable");
    }
  }
}
