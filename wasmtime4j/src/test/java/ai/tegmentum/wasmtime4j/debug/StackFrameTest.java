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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.StackFrame.SourceLocation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StackFrame} interface.
 *
 * <p>StackFrame provides stack frame inspection for WebAssembly debugging including function names,
 * source locations, and variable access.
 */
@DisplayName("StackFrame Tests")
class StackFrameTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(StackFrame.class.isInterface(), "StackFrame should be an interface");
    }

    @Test
    @DisplayName("should have getFrameIndex method")
    void shouldHaveGetFrameIndexMethod() throws NoSuchMethodException {
      final Method method = StackFrame.class.getMethod("getFrameIndex");
      assertNotNull(method, "getFrameIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      final Method method = StackFrame.class.getMethod("getFunctionName");
      assertNotNull(method, "getFunctionName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getSourceLocation method")
    void shouldHaveGetSourceLocationMethod() throws NoSuchMethodException {
      final Method method = StackFrame.class.getMethod("getSourceLocation");
      assertNotNull(method, "getSourceLocation method should exist");
      assertEquals(SourceLocation.class, method.getReturnType(), "Should return SourceLocation");
    }

    @Test
    @DisplayName("should have getVariables method")
    void shouldHaveGetVariablesMethod() throws NoSuchMethodException {
      final Method method = StackFrame.class.getMethod("getVariables");
      assertNotNull(method, "getVariables method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getDepth method")
    void shouldHaveGetDepthMethod() throws NoSuchMethodException {
      final Method method = StackFrame.class.getMethod("getDepth");
      assertNotNull(method, "getDepth method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("SourceLocation Interface Tests")
  class SourceLocationInterfaceTests {

    @Test
    @DisplayName("SourceLocation should be a nested interface")
    void sourceLocationShouldBeNestedInterface() {
      assertTrue(SourceLocation.class.isInterface(), "SourceLocation should be an interface");
    }

    @Test
    @DisplayName("SourceLocation should have getFilePath method")
    void sourceLocationShouldHaveGetFilePathMethod() throws NoSuchMethodException {
      final Method method = SourceLocation.class.getMethod("getFilePath");
      assertNotNull(method, "getFilePath method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("SourceLocation should have getLineNumber method")
    void sourceLocationShouldHaveGetLineNumberMethod() throws NoSuchMethodException {
      final Method method = SourceLocation.class.getMethod("getLineNumber");
      assertNotNull(method, "getLineNumber method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("SourceLocation should have getColumnNumber method")
    void sourceLocationShouldHaveGetColumnNumberMethod() throws NoSuchMethodException {
      final Method method = SourceLocation.class.getMethod("getColumnNumber");
      assertNotNull(method, "getColumnNumber method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock stack frame should return frame index")
    void mockStackFrameShouldReturnFrameIndex() {
      final MockStackFrame frame = new MockStackFrame(0, "main", 3);

      assertEquals(0, frame.getFrameIndex(), "Frame index should be 0");
    }

    @Test
    @DisplayName("mock stack frame should return function name")
    void mockStackFrameShouldReturnFunctionName() {
      final MockStackFrame frame = new MockStackFrame(0, "processData", 1);

      assertEquals("processData", frame.getFunctionName(), "Function name should match");
    }

    @Test
    @DisplayName("mock stack frame should return depth")
    void mockStackFrameShouldReturnDepth() {
      final MockStackFrame frame = new MockStackFrame(2, "helper", 5);

      assertEquals(5, frame.getDepth(), "Depth should be 5");
    }

    @Test
    @DisplayName("mock stack frame should have null source location when not set")
    void mockStackFrameShouldHaveNullSourceLocationWhenNotSet() {
      final MockStackFrame frame = new MockStackFrame(0, "func", 1);

      assertNull(frame.getSourceLocation(), "Source location should be null");
    }

    @Test
    @DisplayName("mock stack frame should return source location when set")
    void mockStackFrameShouldReturnSourceLocationWhenSet() {
      final MockStackFrame frame = new MockStackFrame(0, "func", 1);
      frame.setSourceLocation(new MockSourceLocation("test.wat", 42, 10));

      final SourceLocation location = frame.getSourceLocation();
      assertNotNull(location, "Source location should not be null");
      assertEquals("test.wat", location.getFilePath(), "File path should match");
      assertEquals(42, location.getLineNumber(), "Line number should match");
      assertEquals(10, location.getColumnNumber(), "Column number should match");
    }

    @Test
    @DisplayName("mock stack frame should return empty variables list initially")
    void mockStackFrameShouldReturnEmptyVariablesListInitially() {
      final MockStackFrame frame = new MockStackFrame(0, "func", 1);

      assertNotNull(frame.getVariables(), "Variables should not be null");
      assertTrue(frame.getVariables().isEmpty(), "Variables should be empty");
    }

    @Test
    @DisplayName("mock stack frame should return variables when added")
    void mockStackFrameShouldReturnVariablesWhenAdded() {
      final MockStackFrame frame = new MockStackFrame(0, "func", 1);
      frame.addVariable(new MockVariable("x"));
      frame.addVariable(new MockVariable("y"));

      assertEquals(2, frame.getVariables().size(), "Should have 2 variables");
    }
  }

  /** Mock implementation of StackFrame for testing. */
  private static class MockStackFrame implements StackFrame {
    private final int frameIndex;
    private final String functionName;
    private final int depth;
    private SourceLocation sourceLocation;
    private final List<Variable> variables = new ArrayList<>();

    MockStackFrame(final int frameIndex, final String functionName, final int depth) {
      this.frameIndex = frameIndex;
      this.functionName = functionName;
      this.depth = depth;
    }

    @Override
    public int getFrameIndex() {
      return frameIndex;
    }

    @Override
    public String getFunctionName() {
      return functionName;
    }

    @Override
    public SourceLocation getSourceLocation() {
      return sourceLocation;
    }

    @Override
    public List<Variable> getVariables() {
      return variables;
    }

    @Override
    public int getDepth() {
      return depth;
    }

    public void setSourceLocation(final SourceLocation sourceLocation) {
      this.sourceLocation = sourceLocation;
    }

    public void addVariable(final Variable variable) {
      variables.add(variable);
    }
  }

  /** Mock implementation of SourceLocation for testing. */
  private static class MockSourceLocation implements SourceLocation {
    private final String filePath;
    private final int lineNumber;
    private final int columnNumber;

    MockSourceLocation(final String filePath, final int lineNumber, final int columnNumber) {
      this.filePath = filePath;
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
    }

    @Override
    public String getFilePath() {
      return filePath;
    }

    @Override
    public int getLineNumber() {
      return lineNumber;
    }

    @Override
    public int getColumnNumber() {
      return columnNumber;
    }
  }

  /** Mock implementation of Variable for testing. */
  private static class MockVariable implements Variable {
    private final String name;

    MockVariable(final String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getType() {
      return "i32";
    }

    @Override
    public VariableValue getValue() {
      return null;
    }

    @Override
    public VariableScope getScope() {
      return VariableScope.LOCAL;
    }

    @Override
    public boolean isMutable() {
      return true;
    }
  }
}
