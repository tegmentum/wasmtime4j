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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.DebugSession;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link JniDebugSession}.
 */
@DisplayName("JniDebugSession Tests")
class JniDebugSessionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniDebugSession should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniDebugSession.class.getModifiers()),
          "JniDebugSession should be final");
    }

    @Test
    @DisplayName("JniDebugSession should implement DebugSession")
    void shouldImplementDebugSession() {
      assertTrue(
          DebugSession.class.isAssignableFrom(JniDebugSession.class),
          "JniDebugSession should implement DebugSession");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with nativeHandle should create inactive session")
    void constructorWithNativeHandleShouldCreateInactiveSession() {
      final JniDebugSession session = new JniDebugSession(0L);

      assertNotNull(session.getSessionId(), "Session ID should not be null");
      assertTrue(session.getSessionId().startsWith("session-"), "Session ID should start with session-");
      assertEquals(0L, session.getNativeHandle(), "Native handle should be 0");
      assertFalse(session.isActive(), "Session should be inactive");
      assertFalse(session.isPaused(), "Session should not be paused");
      assertFalse(session.isClosed(), "Session should not be closed");
    }

    @Test
    @DisplayName("Constructor with debuggerHandle and instanceHandle should create active session")
    void constructorWithDebuggerAndInstanceHandleShouldCreateActiveSession() {
      final JniDebugSession session = new JniDebugSession(100L, 200L);

      assertNotNull(session.getSessionId(), "Session ID should not be null");
      assertEquals(100L, session.getNativeHandle(), "Native handle should be 100");
      assertEquals(100L, session.getDebuggerHandle(), "Debugger handle should be 100");
      assertArrayEquals(new long[]{200L}, session.getInstanceHandles(),
          "Instance handles should contain 200");
      assertTrue(session.isActive(), "Session should be active");
    }

    @Test
    @DisplayName("Constructor with multiple instanceHandles should store all handles")
    void constructorWithMultipleInstanceHandlesShouldStoreAllHandles() {
      final long[] handles = {100L, 200L, 300L};
      final JniDebugSession session = new JniDebugSession(50L, handles);

      assertArrayEquals(handles, session.getInstanceHandles(),
          "Instance handles should match");
    }

    @Test
    @DisplayName("Constructor with null instanceHandles should use empty array")
    void constructorWithNullInstanceHandlesShouldUseEmptyArray() {
      final JniDebugSession session = new JniDebugSession(50L, (long[]) null);

      assertEquals(0, session.getInstanceHandles().length,
          "Instance handles should be empty");
    }

    @Test
    @DisplayName("Constructor with config should store config")
    void constructorWithConfigShouldStoreConfig() {
      final JniDebugConfig config = JniDebugConfig.getDefault();
      final JniDebugSession session = new JniDebugSession(100L, 200L, config);

      assertEquals(config, session.getConfig(), "Config should match");
    }
  }

  @Nested
  @DisplayName("createLocal Factory Tests")
  class CreateLocalTests {

    @Test
    @DisplayName("createLocal should create session with 0 native handle")
    void createLocalShouldCreateSessionWithZeroNativeHandle() {
      final JniDebugSession session = JniDebugSession.createLocal();

      assertNotNull(session, "Session should not be null");
      assertEquals(0L, session.getNativeHandle(), "Native handle should be 0");
      assertFalse(session.isActive(), "Session should be inactive");
    }
  }

  @Nested
  @DisplayName("Session Lifecycle Tests")
  class SessionLifecycleTests {

    @Test
    @DisplayName("start should activate session")
    void startShouldActivateSession() {
      final JniDebugSession session = JniDebugSession.createLocal();

      assertFalse(session.isActive(), "Session should be inactive initially");

      session.start();

      assertTrue(session.isActive(), "Session should be active after start");
    }

    @Test
    @DisplayName("start should be idempotent")
    void startShouldBeIdempotent() {
      final JniDebugSession session = JniDebugSession.createLocal();

      session.start();
      session.start(); // Second start should be no-op

      assertTrue(session.isActive(), "Session should remain active");
    }

    @Test
    @DisplayName("stop should deactivate session")
    void stopShouldDeactivateSession() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.start();

      assertTrue(session.isActive(), "Session should be active");

      session.stop();

      assertFalse(session.isActive(), "Session should be inactive after stop");
    }

    @Test
    @DisplayName("stop should clear pending step")
    void stopShouldClearPendingStep() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.start();
      session.step(DebugSession.StepType.STEP_INTO);

      session.stop();

      assertNull(session.getPendingStep(), "Pending step should be cleared");
    }

    @Test
    @DisplayName("close should deactivate and mark as closed")
    void closeShouldDeactivateAndMarkAsClosed() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.start();

      session.close();

      assertFalse(session.isActive(), "Session should be inactive");
      assertTrue(session.isClosed(), "Session should be closed");
    }

    @Test
    @DisplayName("close should clear breakpoints and call stack")
    void closeShouldClearBreakpointsAndCallStack() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.start();
      session.addBreakpoint(new JniBreakpoint("bp-1", "func", 1, 1, 0L));

      session.close();

      assertTrue(session.getBreakpoints().isEmpty(), "Breakpoints should be cleared");
      assertTrue(session.getCallStack().isEmpty(), "Call stack should be cleared");
    }

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      final JniDebugSession session = JniDebugSession.createLocal();

      session.close();
      session.close(); // Second close should be no-op

      assertTrue(session.isClosed(), "Session should remain closed");
    }
  }

  @Nested
  @DisplayName("Breakpoint Tests")
  class BreakpointTests {

    @Test
    @DisplayName("addBreakpoint should add breakpoint to list")
    void addBreakpointShouldAddBreakpointToList() {
      final JniDebugSession session = JniDebugSession.createLocal();
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);

      session.addBreakpoint(bp);

      assertEquals(1, session.getBreakpoints().size(), "Should have 1 breakpoint");
      assertEquals(bp, session.getBreakpoints().get(0), "Breakpoint should match");
    }

    @Test
    @DisplayName("addBreakpoint should throw on null breakpoint")
    void addBreakpointShouldThrowOnNullBreakpoint() {
      final JniDebugSession session = JniDebugSession.createLocal();

      assertThrows(NullPointerException.class, () -> session.addBreakpoint(null),
          "Should throw on null breakpoint");
    }

    @Test
    @DisplayName("addBreakpoint should throw for non-JniBreakpoint")
    void addBreakpointShouldThrowForNonJniBreakpoint() {
      final JniDebugSession session = JniDebugSession.createLocal();

      // Anonymous breakpoint implementation
      final ai.tegmentum.wasmtime4j.debug.Breakpoint nonJniBp =
          new ai.tegmentum.wasmtime4j.debug.Breakpoint() {
            @Override
            public String getBreakpointId() {
              return "bp-1";
            }

            @Override
            public String getFunctionName() {
              return "func";
            }

            @Override
            public int getLineNumber() {
              return 1;
            }

            @Override
            public int getColumnNumber() {
              return 1;
            }

            @Override
            public boolean isEnabled() {
              return true;
            }

            @Override
            public void setEnabled(final boolean enabled) {
            }

            @Override
            public String getCondition() {
              return null;
            }

            @Override
            public void setCondition(final String condition) {
            }

            @Override
            public int getHitCount() {
              return 0;
            }

            @Override
            public void resetHitCount() {
            }
          };

      assertThrows(IllegalArgumentException.class, () -> session.addBreakpoint(nonJniBp),
          "Should throw for non-JniBreakpoint");
    }

    @Test
    @DisplayName("removeBreakpoint should remove breakpoint by ID")
    void removeBreakpointShouldRemoveBreakpointById() {
      final JniDebugSession session = JniDebugSession.createLocal();
      final JniBreakpoint bp1 = new JniBreakpoint("bp-1", "func1", 1, 1, 0L);
      final JniBreakpoint bp2 = new JniBreakpoint("bp-2", "func2", 2, 2, 0L);

      session.addBreakpoint(bp1);
      session.addBreakpoint(bp2);
      assertEquals(2, session.getBreakpoints().size(), "Should have 2 breakpoints");

      session.removeBreakpoint(bp1);

      assertEquals(1, session.getBreakpoints().size(), "Should have 1 breakpoint");
      assertEquals("bp-2", session.getBreakpoints().get(0).getBreakpointId(),
          "Remaining breakpoint should be bp-2");
    }

    @Test
    @DisplayName("removeBreakpoint should throw on null breakpoint")
    void removeBreakpointShouldThrowOnNullBreakpoint() {
      final JniDebugSession session = JniDebugSession.createLocal();

      assertThrows(NullPointerException.class, () -> session.removeBreakpoint(null),
          "Should throw on null breakpoint");
    }

    @Test
    @DisplayName("getBreakpoints should return unmodifiable list")
    void getBreakpointsShouldReturnUnmodifiableList() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.addBreakpoint(new JniBreakpoint("bp-1", "func", 1, 1, 0L));

      final List<JniBreakpoint> breakpoints = session.getBreakpoints();

      assertThrows(UnsupportedOperationException.class,
          () -> breakpoints.add(new JniBreakpoint("bp-2", "func", 1, 1, 0L)),
          "List should be unmodifiable");
    }
  }

  @Nested
  @DisplayName("Step Tests")
  class StepTests {

    @Test
    @DisplayName("step should set pending step type")
    void stepShouldSetPendingStepType() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.start();

      session.step(DebugSession.StepType.STEP_INTO);

      assertEquals(DebugSession.StepType.STEP_INTO, session.getPendingStep(),
          "Pending step should be STEP_INTO");
    }

    @Test
    @DisplayName("step should throw on null stepType")
    void stepShouldThrowOnNullStepType() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.start();

      assertThrows(NullPointerException.class, () -> session.step(null),
          "Should throw on null stepType");
    }

    @Test
    @DisplayName("step should throw when session not active")
    void stepShouldThrowWhenSessionNotActive() {
      final JniDebugSession session = JniDebugSession.createLocal();

      assertThrows(IllegalStateException.class,
          () -> session.step(DebugSession.StepType.STEP_INTO),
          "Should throw when session not active");
    }

    @Test
    @DisplayName("step should clear paused state")
    void stepShouldClearPausedState() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.start();
      session.onBreakpointHit("bp-1"); // Sets paused to true

      assertTrue(session.isPaused(), "Session should be paused");

      session.step(DebugSession.StepType.STEP_OVER);

      assertFalse(session.isPaused(), "Session should not be paused after step");
    }
  }

  @Nested
  @DisplayName("Continue Execution Tests")
  class ContinueExecutionTests {

    @Test
    @DisplayName("continueExecution should clear pending step")
    void continueExecutionShouldClearPendingStep() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.start();
      session.step(DebugSession.StepType.STEP_INTO);

      session.continueExecution();

      assertNull(session.getPendingStep(), "Pending step should be cleared");
    }

    @Test
    @DisplayName("continueExecution should clear paused state")
    void continueExecutionShouldClearPausedState() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.start();
      session.onBreakpointHit("bp-1");

      assertTrue(session.isPaused(), "Session should be paused");

      session.continueExecution();

      assertFalse(session.isPaused(), "Session should not be paused after continue");
    }

    @Test
    @DisplayName("continueExecution should throw when session not active")
    void continueExecutionShouldThrowWhenSessionNotActive() {
      final JniDebugSession session = JniDebugSession.createLocal();

      assertThrows(IllegalStateException.class, session::continueExecution,
          "Should throw when session not active");
    }
  }

  @Nested
  @DisplayName("Breakpoint Hit Tests")
  class BreakpointHitTests {

    @Test
    @DisplayName("onBreakpointHit should set paused state")
    void onBreakpointHitShouldSetPausedState() {
      final JniDebugSession session = JniDebugSession.createLocal();

      session.onBreakpointHit("bp-1");

      assertTrue(session.isPaused(), "Session should be paused");
    }

    @Test
    @DisplayName("onBreakpointHit should increment hit count for matching breakpoint")
    void onBreakpointHitShouldIncrementHitCount() {
      final JniDebugSession session = JniDebugSession.createLocal();
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);
      session.addBreakpoint(bp);

      assertEquals(0, bp.getHitCount(), "Hit count should be 0 initially");

      session.onBreakpointHit("bp-1");

      assertEquals(1, bp.getHitCount(), "Hit count should be 1 after hit");
    }

    @Test
    @DisplayName("onBreakpointHit should not affect other breakpoints")
    void onBreakpointHitShouldNotAffectOtherBreakpoints() {
      final JniDebugSession session = JniDebugSession.createLocal();
      final JniBreakpoint bp1 = new JniBreakpoint("bp-1", "func1", 1, 1, 0L);
      final JniBreakpoint bp2 = new JniBreakpoint("bp-2", "func2", 2, 2, 0L);
      session.addBreakpoint(bp1);
      session.addBreakpoint(bp2);

      session.onBreakpointHit("bp-1");

      assertEquals(1, bp1.getHitCount(), "bp-1 hit count should be 1");
      assertEquals(0, bp2.getHitCount(), "bp-2 hit count should be 0");
    }
  }

  @Nested
  @DisplayName("Step Complete Tests")
  class StepCompleteTests {

    @Test
    @DisplayName("onStepComplete should set paused state")
    void onStepCompleteShouldSetPausedState() {
      final JniDebugSession session = JniDebugSession.createLocal();

      session.onStepComplete(DebugSession.StepType.STEP_INTO);

      assertTrue(session.isPaused(), "Session should be paused");
    }

    @Test
    @DisplayName("onStepComplete should clear pending step")
    void onStepCompleteShouldClearPendingStep() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.start();
      session.step(DebugSession.StepType.STEP_OVER);

      session.onStepComplete(DebugSession.StepType.STEP_OVER);

      assertNull(session.getPendingStep(), "Pending step should be cleared");
    }
  }

  @Nested
  @DisplayName("Call Stack Tests")
  class CallStackTests {

    @Test
    @DisplayName("updateCallStack should update call stack")
    void updateCallStackShouldUpdateCallStack() {
      final JniDebugSession session = JniDebugSession.createLocal();
      final List<JniStackFrame> frames = Arrays.asList(
          JniStackFrame.fromNative(0, "func1", 100L, 10, 1, "file1.wasm"),
          JniStackFrame.fromNative(1, "func2", 200L, 20, 2, "file2.wasm"));

      session.updateCallStack(frames);

      assertEquals(2, session.getCallStack().size(), "Call stack should have 2 frames");
      assertEquals("func1", session.getCallStack().get(0).getFunctionName(),
          "First frame function should be func1");
    }

    @Test
    @DisplayName("updateCallStack with null should clear call stack")
    void updateCallStackWithNullShouldClearCallStack() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.updateCallStack(Arrays.asList(
          JniStackFrame.fromNative(0, "func", 100L, 10, 1, "file.wasm")));

      assertEquals(1, session.getCallStack().size(), "Call stack should have 1 frame");

      session.updateCallStack(null);

      assertTrue(session.getCallStack().isEmpty(), "Call stack should be empty");
    }

    @Test
    @DisplayName("getCallStack should return unmodifiable list")
    void getCallStackShouldReturnUnmodifiableList() {
      final JniDebugSession session = JniDebugSession.createLocal();

      final List<JniStackFrame> callStack = session.getCallStack();

      assertThrows(UnsupportedOperationException.class,
          () -> callStack.add(JniStackFrame.fromNative(0, "func", 0L, 0, 0, null)),
          "List should be unmodifiable");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include session state")
    void toStringShouldIncludeSessionState() {
      final JniDebugSession session = JniDebugSession.createLocal();
      session.start();
      session.addBreakpoint(new JniBreakpoint("bp-1", "func", 1, 1, 0L));

      final String str = session.toString();

      assertTrue(str.contains("session-"), "Should contain session ID prefix");
      assertTrue(str.contains("active=true"), "Should contain active state");
      assertTrue(str.contains("paused=false"), "Should contain paused state");
      assertTrue(str.contains("breakpoints=1"), "Should contain breakpoint count");
    }
  }

  @Nested
  @DisplayName("Instance Handles Defensive Copy Tests")
  class InstanceHandlesDefensiveCopyTests {

    @Test
    @DisplayName("getInstanceHandles should return defensive copy")
    void getInstanceHandlesShouldReturnDefensiveCopy() {
      final long[] handles = {100L, 200L};
      final JniDebugSession session = new JniDebugSession(50L, handles);

      final long[] retrieved = session.getInstanceHandles();
      retrieved[0] = 999L;

      assertEquals(100L, session.getInstanceHandles()[0],
          "Original handles should not be modified");
    }
  }
}
