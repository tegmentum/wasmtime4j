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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Breakpoint} interface.
 *
 * <p>Breakpoint provides breakpoint management for WebAssembly debugging including enabling,
 * disabling, conditions, and hit counts.
 */
@DisplayName("Breakpoint Tests")
class BreakpointTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Breakpoint.class.isInterface(), "Breakpoint should be an interface");
    }

    @Test
    @DisplayName("should have getBreakpointId method")
    void shouldHaveGetBreakpointIdMethod() throws NoSuchMethodException {
      final Method method = Breakpoint.class.getMethod("getBreakpointId");
      assertNotNull(method, "getBreakpointId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      final Method method = Breakpoint.class.getMethod("getFunctionName");
      assertNotNull(method, "getFunctionName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getLineNumber method")
    void shouldHaveGetLineNumberMethod() throws NoSuchMethodException {
      final Method method = Breakpoint.class.getMethod("getLineNumber");
      assertNotNull(method, "getLineNumber method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getColumnNumber method")
    void shouldHaveGetColumnNumberMethod() throws NoSuchMethodException {
      final Method method = Breakpoint.class.getMethod("getColumnNumber");
      assertNotNull(method, "getColumnNumber method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = Breakpoint.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have setEnabled method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      final Method method = Breakpoint.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getCondition method")
    void shouldHaveGetConditionMethod() throws NoSuchMethodException {
      final Method method = Breakpoint.class.getMethod("getCondition");
      assertNotNull(method, "getCondition method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have setCondition method")
    void shouldHaveSetConditionMethod() throws NoSuchMethodException {
      final Method method = Breakpoint.class.getMethod("setCondition", String.class);
      assertNotNull(method, "setCondition method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getHitCount method")
    void shouldHaveGetHitCountMethod() throws NoSuchMethodException {
      final Method method = Breakpoint.class.getMethod("getHitCount");
      assertNotNull(method, "getHitCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have resetHitCount method")
    void shouldHaveResetHitCountMethod() throws NoSuchMethodException {
      final Method method = Breakpoint.class.getMethod("resetHitCount");
      assertNotNull(method, "resetHitCount method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock breakpoint should return breakpoint ID")
    void mockBreakpointShouldReturnBreakpointId() {
      final MockBreakpoint breakpoint = new MockBreakpoint("bp-123", "myFunction", 42, 5);

      assertEquals("bp-123", breakpoint.getBreakpointId(), "Breakpoint ID should match");
    }

    @Test
    @DisplayName("mock breakpoint should return function name")
    void mockBreakpointShouldReturnFunctionName() {
      final MockBreakpoint breakpoint = new MockBreakpoint("bp-1", "testFunction", 10, 0);

      assertEquals("testFunction", breakpoint.getFunctionName(), "Function name should match");
    }

    @Test
    @DisplayName("mock breakpoint should return line and column")
    void mockBreakpointShouldReturnLineAndColumn() {
      final MockBreakpoint breakpoint = new MockBreakpoint("bp-1", "func", 100, 25);

      assertEquals(100, breakpoint.getLineNumber(), "Line number should match");
      assertEquals(25, breakpoint.getColumnNumber(), "Column number should match");
    }

    @Test
    @DisplayName("mock breakpoint should be enabled by default")
    void mockBreakpointShouldBeEnabledByDefault() {
      final MockBreakpoint breakpoint = new MockBreakpoint("bp-1", "func", 1, 0);

      assertTrue(breakpoint.isEnabled(), "Breakpoint should be enabled by default");
    }

    @Test
    @DisplayName("mock breakpoint should toggle enabled state")
    void mockBreakpointShouldToggleEnabledState() {
      final MockBreakpoint breakpoint = new MockBreakpoint("bp-1", "func", 1, 0);

      assertTrue(breakpoint.isEnabled(), "Initially enabled");
      breakpoint.setEnabled(false);
      assertFalse(breakpoint.isEnabled(), "Should be disabled");
      breakpoint.setEnabled(true);
      assertTrue(breakpoint.isEnabled(), "Should be enabled again");
    }

    @Test
    @DisplayName("mock breakpoint should have no condition initially")
    void mockBreakpointShouldHaveNoConditionInitially() {
      final MockBreakpoint breakpoint = new MockBreakpoint("bp-1", "func", 1, 0);

      assertNull(breakpoint.getCondition(), "Condition should be null initially");
    }

    @Test
    @DisplayName("mock breakpoint should set and get condition")
    void mockBreakpointShouldSetAndGetCondition() {
      final MockBreakpoint breakpoint = new MockBreakpoint("bp-1", "func", 1, 0);

      breakpoint.setCondition("x > 5");
      assertEquals("x > 5", breakpoint.getCondition(), "Condition should match");

      breakpoint.setCondition(null);
      assertNull(breakpoint.getCondition(), "Condition should be null");
    }

    @Test
    @DisplayName("mock breakpoint should track hit count")
    void mockBreakpointShouldTrackHitCount() {
      final MockBreakpoint breakpoint = new MockBreakpoint("bp-1", "func", 1, 0);

      assertEquals(0, breakpoint.getHitCount(), "Hit count should be 0 initially");

      breakpoint.incrementHitCount();
      breakpoint.incrementHitCount();
      breakpoint.incrementHitCount();
      assertEquals(3, breakpoint.getHitCount(), "Hit count should be 3");
    }

    @Test
    @DisplayName("mock breakpoint should reset hit count")
    void mockBreakpointShouldResetHitCount() {
      final MockBreakpoint breakpoint = new MockBreakpoint("bp-1", "func", 1, 0);

      breakpoint.incrementHitCount();
      breakpoint.incrementHitCount();
      assertEquals(2, breakpoint.getHitCount(), "Hit count should be 2");

      breakpoint.resetHitCount();
      assertEquals(0, breakpoint.getHitCount(), "Hit count should be 0 after reset");
    }
  }

  /** Mock implementation of Breakpoint for testing. */
  private static class MockBreakpoint implements Breakpoint {
    private final String breakpointId;
    private final String functionName;
    private final int lineNumber;
    private final int columnNumber;
    private boolean enabled = true;
    private String condition;
    private int hitCount;

    MockBreakpoint(
        final String breakpointId,
        final String functionName,
        final int lineNumber,
        final int columnNumber) {
      this.breakpointId = breakpointId;
      this.functionName = functionName;
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
    }

    @Override
    public String getBreakpointId() {
      return breakpointId;
    }

    @Override
    public String getFunctionName() {
      return functionName;
    }

    @Override
    public int getLineNumber() {
      return lineNumber;
    }

    @Override
    public int getColumnNumber() {
      return columnNumber;
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
      this.enabled = enabled;
    }

    @Override
    public String getCondition() {
      return condition;
    }

    @Override
    public void setCondition(final String condition) {
      this.condition = condition;
    }

    @Override
    public int getHitCount() {
      return hitCount;
    }

    @Override
    public void resetHitCount() {
      hitCount = 0;
    }

    public void incrementHitCount() {
      hitCount++;
    }
  }
}
