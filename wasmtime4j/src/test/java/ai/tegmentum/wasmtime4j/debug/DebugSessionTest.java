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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.DebugSession.StepType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DebugSession} interface.
 *
 * <p>DebugSession provides session management for WebAssembly debugging including stepping,
 * breakpoints, and execution control.
 */
@DisplayName("DebugSession Tests")
class DebugSessionTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(DebugSession.class.isInterface(), "DebugSession should be an interface");
    }

    @Test
    @DisplayName("should have start method")
    void shouldHaveStartMethod() throws NoSuchMethodException {
      final Method method = DebugSession.class.getMethod("start");
      assertNotNull(method, "start method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have stop method")
    void shouldHaveStopMethod() throws NoSuchMethodException {
      final Method method = DebugSession.class.getMethod("stop");
      assertNotNull(method, "stop method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have step method")
    void shouldHaveStepMethod() throws NoSuchMethodException {
      final Method method = DebugSession.class.getMethod("step", StepType.class);
      assertNotNull(method, "step method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have continueExecution method")
    void shouldHaveContinueExecutionMethod() throws NoSuchMethodException {
      final Method method = DebugSession.class.getMethod("continueExecution");
      assertNotNull(method, "continueExecution method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have addBreakpoint method")
    void shouldHaveAddBreakpointMethod() throws NoSuchMethodException {
      final Method method = DebugSession.class.getMethod("addBreakpoint", Breakpoint.class);
      assertNotNull(method, "addBreakpoint method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have removeBreakpoint method")
    void shouldHaveRemoveBreakpointMethod() throws NoSuchMethodException {
      final Method method = DebugSession.class.getMethod("removeBreakpoint", Breakpoint.class);
      assertNotNull(method, "removeBreakpoint method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getSessionId method")
    void shouldHaveGetSessionIdMethod() throws NoSuchMethodException {
      final Method method = DebugSession.class.getMethod("getSessionId");
      assertNotNull(method, "getSessionId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have isActive method")
    void shouldHaveIsActiveMethod() throws NoSuchMethodException {
      final Method method = DebugSession.class.getMethod("isActive");
      assertNotNull(method, "isActive method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("StepType Enum Tests")
  class StepTypeEnumTests {

    @Test
    @DisplayName("should have STEP_INTO value")
    void shouldHaveStepIntoValue() {
      assertNotNull(StepType.valueOf("STEP_INTO"), "STEP_INTO should exist");
    }

    @Test
    @DisplayName("should have STEP_OVER value")
    void shouldHaveStepOverValue() {
      assertNotNull(StepType.valueOf("STEP_OVER"), "STEP_OVER should exist");
    }

    @Test
    @DisplayName("should have STEP_OUT value")
    void shouldHaveStepOutValue() {
      assertNotNull(StepType.valueOf("STEP_OUT"), "STEP_OUT should exist");
    }

    @Test
    @DisplayName("should have exactly three values")
    void shouldHaveExactlyThreeValues() {
      assertEquals(3, StepType.values().length, "Should have exactly 3 StepType values");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock session should start and become active")
    void mockSessionShouldStartAndBecomeActive() {
      final MockDebugSession session = new MockDebugSession("test-session");

      assertFalse(session.isActive(), "Session should not be active initially");
      session.start();
      assertTrue(session.isActive(), "Session should be active after start");
    }

    @Test
    @DisplayName("mock session should stop and become inactive")
    void mockSessionShouldStopAndBecomeInactive() {
      final MockDebugSession session = new MockDebugSession("test-session");

      session.start();
      assertTrue(session.isActive(), "Session should be active");
      session.stop();
      assertFalse(session.isActive(), "Session should be inactive after stop");
    }

    @Test
    @DisplayName("mock session should return session ID")
    void mockSessionShouldReturnSessionId() {
      final MockDebugSession session = new MockDebugSession("my-session-id");

      assertEquals("my-session-id", session.getSessionId(), "Session ID should match");
    }

    @Test
    @DisplayName("mock session should handle all step types")
    void mockSessionShouldHandleAllStepTypes() {
      final MockDebugSession session = new MockDebugSession("test-session");
      session.start();

      for (final StepType stepType : StepType.values()) {
        session.step(stepType);
        assertEquals(stepType, session.getLastStepType(), "Last step type should match");
      }
    }

    @Test
    @DisplayName("mock session should manage breakpoints")
    void mockSessionShouldManageBreakpoints() {
      final MockDebugSession session = new MockDebugSession("test-session");
      final MockBreakpoint breakpoint = new MockBreakpoint("bp-1");

      session.addBreakpoint(breakpoint);
      assertEquals(1, session.getBreakpointCount(), "Should have one breakpoint");

      session.removeBreakpoint(breakpoint);
      assertEquals(0, session.getBreakpointCount(), "Should have no breakpoints");
    }
  }

  /** Mock implementation of DebugSession for testing. */
  private static class MockDebugSession implements DebugSession {
    private final String sessionId;
    private boolean active;
    private StepType lastStepType;
    private final List<Breakpoint> breakpoints = new ArrayList<>();

    MockDebugSession(final String sessionId) {
      this.sessionId = sessionId;
      this.active = false;
    }

    @Override
    public void start() {
      active = true;
    }

    @Override
    public void stop() {
      active = false;
    }

    @Override
    public void step(final StepType stepType) {
      lastStepType = stepType;
    }

    @Override
    public void continueExecution() {
      // Mock continue
    }

    @Override
    public void addBreakpoint(final Breakpoint breakpoint) {
      breakpoints.add(breakpoint);
    }

    @Override
    public void removeBreakpoint(final Breakpoint breakpoint) {
      breakpoints.remove(breakpoint);
    }

    @Override
    public String getSessionId() {
      return sessionId;
    }

    @Override
    public boolean isActive() {
      return active;
    }

    public StepType getLastStepType() {
      return lastStepType;
    }

    public int getBreakpointCount() {
      return breakpoints.size();
    }
  }

  /** Mock implementation of Breakpoint for testing. */
  private static class MockBreakpoint implements Breakpoint {
    private final String breakpointId;
    private boolean enabled = true;
    private String condition;
    private int hitCount;

    MockBreakpoint(final String breakpointId) {
      this.breakpointId = breakpointId;
    }

    @Override
    public String getBreakpointId() {
      return breakpointId;
    }

    @Override
    public String getFunctionName() {
      return "mockFunction";
    }

    @Override
    public int getLineNumber() {
      return 1;
    }

    @Override
    public int getColumnNumber() {
      return 0;
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
  }
}
