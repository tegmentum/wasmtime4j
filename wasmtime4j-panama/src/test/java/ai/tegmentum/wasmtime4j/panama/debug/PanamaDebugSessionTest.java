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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.DebugSession;
import ai.tegmentum.wasmtime4j.debug.DebugSession.StepType;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link PanamaDebugSession}.
 */
@DisplayName("PanamaDebugSession Tests")
class PanamaDebugSessionTest {

  private Arena arena;

  @BeforeEach
  void setUp() {
    arena = Arena.ofConfined();
  }

  @AfterEach
  void tearDown() {
    if (arena != null) {
      arena.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaDebugSession should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaDebugSession.class.getModifiers()),
          "PanamaDebugSession should be final");
    }

    @Test
    @DisplayName("PanamaDebugSession should implement DebugSession")
    void shouldImplementDebugSession() {
      assertTrue(
          DebugSession.class.isAssignableFrom(PanamaDebugSession.class),
          "PanamaDebugSession should implement DebugSession");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with nativeHandle and arena should create session")
    void constructorWithNativeHandleAndArenaShouldCreateSession() {
      final PanamaDebugSession session = new PanamaDebugSession(MemorySegment.NULL, arena);

      assertNotNull(session.getSessionId(), "Session ID should not be null");
      assertTrue(session.getSessionId().startsWith("session-"), "Session ID should have prefix");
      assertFalse(session.isActive(), "Session should not be active initially");
      assertFalse(session.isPaused(), "Session should not be paused initially");
      assertFalse(session.isClosed(), "Session should not be closed initially");
    }

    @Test
    @DisplayName("Constructor with debugger and instance handles should create active session")
    void constructorWithDebuggerAndInstanceHandleShouldCreateActiveSession() {
      final PanamaDebugSession session = new PanamaDebugSession(
          MemorySegment.NULL, MemorySegment.NULL, arena);

      assertTrue(session.isActive(), "Session should be active initially");
      assertEquals(1, session.getInstanceHandles().length, "Should have 1 instance handle");
    }

    @Test
    @DisplayName("Constructor with multiple instance handles should store all handles")
    void constructorWithMultipleInstanceHandlesShouldStoreAllHandles() {
      final MemorySegment[] handles = new MemorySegment[] {
          MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL
      };

      final PanamaDebugSession session = new PanamaDebugSession(
          MemorySegment.NULL, handles, arena);

      assertEquals(3, session.getInstanceHandles().length, "Should have 3 instance handles");
    }

    @Test
    @DisplayName("Constructor with config should store config")
    void constructorWithConfigShouldStoreConfig() {
      final PanamaDebugConfig config = PanamaDebugConfig.getDefault();

      final PanamaDebugSession session = new PanamaDebugSession(
          MemorySegment.NULL, MemorySegment.NULL, config, arena);

      assertNotNull(session.getConfig(), "Config should not be null");
    }

    @Test
    @DisplayName("createLocal should create local session with null handle")
    void createLocalShouldCreateLocalSessionWithNullHandle() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);

      assertNotNull(session, "Session should not be null");
      assertEquals(MemorySegment.NULL, session.getNativeHandle(), "Handle should be NULL");
    }

    @Test
    @DisplayName("Constructor should create defensive copy of instance handles")
    void constructorShouldCreateDefensiveCopyOfInstanceHandles() {
      final MemorySegment[] handles = new MemorySegment[] {MemorySegment.NULL};

      final PanamaDebugSession session = new PanamaDebugSession(
          MemorySegment.NULL, handles, arena);

      final MemorySegment[] retrievedHandles = session.getInstanceHandles();

      // Modify retrieved array
      retrievedHandles[0] = arena.allocate(8);

      // Original should be unchanged
      assertEquals(MemorySegment.NULL, session.getInstanceHandles()[0],
          "Should return defensive copy");
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("start should activate session")
    void startShouldActivateSession() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);

      assertFalse(session.isActive(), "Should not be active before start");

      session.start();

      assertTrue(session.isActive(), "Should be active after start");
    }

    @Test
    @DisplayName("start should be idempotent")
    void startShouldBeIdempotent() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);

      session.start();
      session.start(); // Second call should be no-op

      assertTrue(session.isActive(), "Should still be active");
    }

    @Test
    @DisplayName("stop should deactivate session")
    void stopShouldDeactivateSession() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();

      assertTrue(session.isActive(), "Should be active before stop");

      session.stop();

      assertFalse(session.isActive(), "Should not be active after stop");
    }

    @Test
    @DisplayName("stop should clear paused state")
    void stopShouldClearPausedState() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();
      session.onBreakpointHit("bp-1"); // Sets paused to true

      assertTrue(session.isPaused(), "Should be paused before stop");

      session.stop();

      assertFalse(session.isPaused(), "Should not be paused after stop");
    }

    @Test
    @DisplayName("stop should clear pending step")
    void stopShouldClearPendingStep() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();
      session.step(StepType.STEP_INTO);

      session.stop();

      assertNull(session.getPendingStep(), "Pending step should be cleared");
    }

    @Test
    @DisplayName("close should deactivate and mark session as closed")
    void closeShouldDeactivateAndMarkAsClosed() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();

      session.close();

      assertFalse(session.isActive(), "Should not be active after close");
      assertTrue(session.isClosed(), "Should be closed");
    }

    @Test
    @DisplayName("close should clear breakpoints and call stack")
    void closeShouldClearBreakpointsAndCallStack() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();
      session.addBreakpoint(new PanamaBreakpoint("bp-1", "func", 10, 5, 0x1000L));
      session.updateCallStack(Arrays.asList(
          PanamaStackFrame.builder().frameIndex(0).build()
      ));

      session.close();

      assertTrue(session.getBreakpoints().isEmpty(), "Breakpoints should be cleared");
      assertTrue(session.getCallStack().isEmpty(), "Call stack should be cleared");
    }

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();

      session.close();
      session.close(); // Second call should be no-op

      assertTrue(session.isClosed(), "Should still be closed");
    }
  }

  @Nested
  @DisplayName("Breakpoint Tests")
  class BreakpointTests {

    @Test
    @DisplayName("addBreakpoint should add breakpoint to session")
    void addBreakpointShouldAddBreakpointToSession() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      final PanamaBreakpoint breakpoint = new PanamaBreakpoint("bp-1", "func", 10, 5, 0x1000L);

      session.addBreakpoint(breakpoint);

      assertEquals(1, session.getBreakpoints().size(), "Should have 1 breakpoint");
      assertEquals("bp-1", session.getBreakpoints().get(0).getBreakpointId(),
          "Breakpoint ID should match");
    }

    @Test
    @DisplayName("addBreakpoint should throw on null breakpoint")
    void addBreakpointShouldThrowOnNullBreakpoint() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);

      assertThrows(NullPointerException.class,
          () -> session.addBreakpoint(null),
          "Should throw on null breakpoint");
    }

    @Test
    @DisplayName("removeBreakpoint should remove breakpoint from session")
    void removeBreakpointShouldRemoveBreakpointFromSession() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      final PanamaBreakpoint breakpoint = new PanamaBreakpoint("bp-1", "func", 10, 5, 0x1000L);
      session.addBreakpoint(breakpoint);

      session.removeBreakpoint(breakpoint);

      assertTrue(session.getBreakpoints().isEmpty(), "Breakpoints should be empty");
    }

    @Test
    @DisplayName("removeBreakpoint should throw on null breakpoint")
    void removeBreakpointShouldThrowOnNullBreakpoint() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);

      assertThrows(NullPointerException.class,
          () -> session.removeBreakpoint(null),
          "Should throw on null breakpoint");
    }

    @Test
    @DisplayName("getBreakpoints should return unmodifiable list")
    void getBreakpointsShouldReturnUnmodifiableList() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.addBreakpoint(new PanamaBreakpoint("bp-1", "func", 10, 5, 0x1000L));

      final List<PanamaBreakpoint> breakpoints = session.getBreakpoints();

      assertThrows(UnsupportedOperationException.class,
          () -> breakpoints.add(new PanamaBreakpoint("bp-2", "func2", 20, 3, 0x2000L)),
          "Should not be able to modify returned list");
    }
  }

  @Nested
  @DisplayName("Stepping Tests")
  class SteppingTests {

    @Test
    @DisplayName("step should set pending step and clear paused")
    void stepShouldSetPendingStepAndClearPaused() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();
      session.onBreakpointHit("bp-1"); // Pauses session

      assertTrue(session.isPaused(), "Should be paused before step");

      session.step(StepType.STEP_INTO);

      assertEquals(StepType.STEP_INTO, session.getPendingStep(), "Should have pending step");
      assertFalse(session.isPaused(), "Should not be paused after step");
    }

    @Test
    @DisplayName("step should throw on null step type")
    void stepShouldThrowOnNullStepType() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();

      assertThrows(NullPointerException.class,
          () -> session.step(null),
          "Should throw on null step type");
    }

    @Test
    @DisplayName("step should throw when session not active")
    void stepShouldThrowWhenSessionNotActive() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);

      assertThrows(IllegalStateException.class,
          () -> session.step(StepType.STEP_INTO),
          "Should throw when session not active");
    }

    @Test
    @DisplayName("step should handle all step types")
    void stepShouldHandleAllStepTypes() {
      for (final StepType stepType : StepType.values()) {
        final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
        session.start();

        session.step(stepType);

        assertEquals(stepType, session.getPendingStep(),
            "Should handle step type: " + stepType);
      }
    }
  }

  @Nested
  @DisplayName("Continue Execution Tests")
  class ContinueExecutionTests {

    @Test
    @DisplayName("continueExecution should clear pending step and paused")
    void continueExecutionShouldClearPendingStepAndPaused() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();
      session.onBreakpointHit("bp-1");
      session.step(StepType.STEP_INTO);

      session.continueExecution();

      assertNull(session.getPendingStep(), "Pending step should be cleared");
      assertFalse(session.isPaused(), "Should not be paused after continue");
    }

    @Test
    @DisplayName("continueExecution should throw when session not active")
    void continueExecutionShouldThrowWhenSessionNotActive() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);

      assertThrows(IllegalStateException.class,
          session::continueExecution,
          "Should throw when session not active");
    }
  }

  @Nested
  @DisplayName("Breakpoint Hit Callback Tests")
  class BreakpointHitCallbackTests {

    @Test
    @DisplayName("onBreakpointHit should pause session")
    void onBreakpointHitShouldPauseSession() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();

      session.onBreakpointHit("bp-1");

      assertTrue(session.isPaused(), "Session should be paused after breakpoint hit");
    }

    @Test
    @DisplayName("onBreakpointHit should increment hit count for matching breakpoint")
    void onBreakpointHitShouldIncrementHitCountForMatchingBreakpoint() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      final PanamaBreakpoint breakpoint = new PanamaBreakpoint("bp-1", "func", 10, 5, 0x1000L);
      session.addBreakpoint(breakpoint);

      session.onBreakpointHit("bp-1");

      assertEquals(1, breakpoint.getHitCount(), "Hit count should be incremented");
    }

    @Test
    @DisplayName("onBreakpointHit should not fail for unknown breakpoint")
    void onBreakpointHitShouldNotFailForUnknownBreakpoint() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();

      // Should not throw
      session.onBreakpointHit("unknown-bp");

      assertTrue(session.isPaused(), "Session should still be paused");
    }
  }

  @Nested
  @DisplayName("Step Complete Callback Tests")
  class StepCompleteCallbackTests {

    @Test
    @DisplayName("onStepComplete should pause session and clear pending step")
    void onStepCompleteShouldPauseSessionAndClearPendingStep() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();
      session.step(StepType.STEP_INTO);

      session.onStepComplete(StepType.STEP_INTO);

      assertTrue(session.isPaused(), "Session should be paused after step complete");
      assertNull(session.getPendingStep(), "Pending step should be cleared");
    }
  }

  @Nested
  @DisplayName("Call Stack Tests")
  class CallStackTests {

    @Test
    @DisplayName("updateCallStack should replace call stack")
    void updateCallStackShouldReplaceCallStack() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);

      final List<PanamaStackFrame> frames = Arrays.asList(
          PanamaStackFrame.builder().frameIndex(0).functionName("main").build(),
          PanamaStackFrame.builder().frameIndex(1).functionName("helper").build()
      );

      session.updateCallStack(frames);

      assertEquals(2, session.getCallStack().size(), "Should have 2 frames");
      assertEquals("main", session.getCallStack().get(0).getFunctionName(),
          "First frame should be main");
    }

    @Test
    @DisplayName("updateCallStack should handle null")
    void updateCallStackShouldHandleNull() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.updateCallStack(Arrays.asList(
          PanamaStackFrame.builder().frameIndex(0).build()
      ));

      session.updateCallStack(null);

      assertTrue(session.getCallStack().isEmpty(), "Call stack should be empty");
    }

    @Test
    @DisplayName("getCallStack should return unmodifiable list")
    void getCallStackShouldReturnUnmodifiableList() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.updateCallStack(Arrays.asList(
          PanamaStackFrame.builder().frameIndex(0).build()
      ));

      final List<PanamaStackFrame> callStack = session.getCallStack();

      assertThrows(UnsupportedOperationException.class,
          () -> callStack.add(PanamaStackFrame.builder().frameIndex(1).build()),
          "Should not be able to modify returned list");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include session info")
    void toStringShouldIncludeSessionInfo() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      session.start();
      session.addBreakpoint(new PanamaBreakpoint("bp-1", "func", 10, 5, 0x1000L));

      final String str = session.toString();

      assertTrue(str.contains("sessionId="), "Should contain session ID");
      assertTrue(str.contains("active=true"), "Should contain active state");
      assertTrue(str.contains("paused=false"), "Should contain paused state");
      assertTrue(str.contains("breakpoints=1"), "Should contain breakpoint count");
    }
  }

  @Nested
  @DisplayName("Accessor Tests")
  class AccessorTests {

    @Test
    @DisplayName("getArena should return the arena")
    void getArenaShouldReturnTheArena() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);

      assertEquals(arena, session.getArena(), "Should return the arena");
    }

    @Test
    @DisplayName("getDebuggerHandle should return debugger handle")
    void getDebuggerHandleShouldReturnDebuggerHandle() {
      final PanamaDebugSession session = new PanamaDebugSession(
          MemorySegment.NULL, MemorySegment.NULL, arena);

      assertNotNull(session.getDebuggerHandle(), "Debugger handle should not be null");
    }

    @Test
    @DisplayName("getConfig should return null when not set")
    void getConfigShouldReturnNullWhenNotSet() {
      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);

      assertNull(session.getConfig(), "Config should be null when not set");
    }
  }
}
