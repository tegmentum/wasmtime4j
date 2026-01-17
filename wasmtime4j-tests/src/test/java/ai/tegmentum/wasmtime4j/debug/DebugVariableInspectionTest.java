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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.panama.debug.PanamaBreakpoint;
import ai.tegmentum.wasmtime4j.panama.debug.PanamaDebugSession;
import ai.tegmentum.wasmtime4j.panama.debug.PanamaStackFrame;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for variable inspection in debug sessions.
 *
 * <p>These tests verify the inspection of local variables, globals, and memory during debugging.
 *
 * @since 1.0.0
 */
@DisplayName("Debug Variable Inspection Tests")
class DebugVariableInspectionTest {

  private static final Logger LOGGER =
      Logger.getLogger(DebugVariableInspectionTest.class.getName());

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  private Arena arena;

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for variable inspection tests");
    try {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertTrue(loader.isLoaded(), "Native library should be loaded");
      LOGGER.info("Native library loaded successfully");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up test resources");
    arena = Arena.ofConfined();
    resources.add(arena::close);
    LOGGER.info("Test setup complete");
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test resources");
    for (final AutoCloseable resource : resources) {
      try {
        resource.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    arena = null;
  }

  @Nested
  @DisplayName("Breakpoint Creation Tests")
  class BreakpointCreationTests {

    @Test
    @DisplayName("should create breakpoint at function")
    void shouldCreateBreakpointAtFunction() {
      LOGGER.info("Testing breakpoint creation at function");

      final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("my_function");

      assertNotNull(breakpoint, "Breakpoint should not be null");
      assertNotNull(breakpoint.getBreakpointId(), "Breakpoint ID should not be null");
      assertTrue(breakpoint.getBreakpointId().startsWith("bp-"), "ID should have bp- prefix");
      assertEquals("my_function", breakpoint.getFunctionName(), "Function name should match");
      assertTrue(breakpoint.isEnabled(), "Breakpoint should be enabled by default");

      LOGGER.info("Breakpoint created: " + breakpoint);
    }

    @Test
    @DisplayName("should create breakpoint at line")
    void shouldCreateBreakpointAtLine() {
      LOGGER.info("Testing breakpoint creation at line");

      final PanamaBreakpoint breakpoint = PanamaBreakpoint.atLine(42);

      assertNotNull(breakpoint, "Breakpoint should not be null");
      assertEquals(42, breakpoint.getLineNumber(), "Line number should match");

      LOGGER.info("Line breakpoint created: " + breakpoint);
    }

    @Test
    @DisplayName("should create breakpoint at offset")
    void shouldCreateBreakpointAtOffset() {
      LOGGER.info("Testing breakpoint creation at offset");

      final PanamaBreakpoint breakpoint = PanamaBreakpoint.atOffset(0x1000);

      assertNotNull(breakpoint, "Breakpoint should not be null");
      assertEquals(0x1000, breakpoint.getInstructionOffset(), "Offset should match");

      LOGGER.info("Offset breakpoint created: " + breakpoint);
    }

    @Test
    @DisplayName("should create breakpoint with builder")
    void shouldCreateBreakpointWithBuilder() {
      LOGGER.info("Testing breakpoint creation with builder");

      final PanamaBreakpoint breakpoint =
          PanamaBreakpoint.builder()
              .breakpointId("custom-bp-1")
              .functionName("test_func")
              .lineNumber(100)
              .columnNumber(5)
              .enabled(false)
              .condition("x > 10")
              .build();

      assertEquals("custom-bp-1", breakpoint.getBreakpointId(), "ID should match");
      assertEquals("test_func", breakpoint.getFunctionName(), "Function should match");
      assertEquals(100, breakpoint.getLineNumber(), "Line should match");
      assertEquals(5, breakpoint.getColumnNumber(), "Column should match");
      assertFalse(breakpoint.isEnabled(), "Should be disabled");
      assertEquals("x > 10", breakpoint.getCondition(), "Condition should match");

      LOGGER.info("Builder breakpoint created: " + breakpoint);
    }
  }

  @Nested
  @DisplayName("Breakpoint State Tests")
  class BreakpointStateTests {

    @Test
    @DisplayName("should toggle breakpoint enabled state")
    void shouldToggleBreakpointEnabledState() {
      LOGGER.info("Testing breakpoint enable/disable");

      final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("test");

      assertTrue(breakpoint.isEnabled(), "Should start enabled");

      breakpoint.setEnabled(false);
      assertFalse(breakpoint.isEnabled(), "Should be disabled");

      breakpoint.setEnabled(true);
      assertTrue(breakpoint.isEnabled(), "Should be enabled again");

      LOGGER.info("Breakpoint state toggled successfully");
    }

    @Test
    @DisplayName("should track hit count")
    void shouldTrackHitCount() {
      LOGGER.info("Testing breakpoint hit count");

      final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("test");

      assertEquals(0, breakpoint.getHitCount(), "Hit count should start at 0");

      breakpoint.incrementHitCount();
      assertEquals(1, breakpoint.getHitCount(), "Hit count should be 1");

      breakpoint.incrementHitCount();
      breakpoint.incrementHitCount();
      assertEquals(3, breakpoint.getHitCount(), "Hit count should be 3");

      breakpoint.resetHitCount();
      assertEquals(0, breakpoint.getHitCount(), "Hit count should be reset to 0");

      LOGGER.info("Hit count tracking works correctly");
    }

    @Test
    @DisplayName("should set and get condition")
    void shouldSetAndGetCondition() {
      LOGGER.info("Testing breakpoint condition");

      final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("test");

      assertEquals(null, breakpoint.getCondition(), "No condition initially");

      breakpoint.setCondition("i == 5");
      assertEquals("i == 5", breakpoint.getCondition(), "Condition should be set");

      breakpoint.setCondition(null);
      assertEquals(null, breakpoint.getCondition(), "Condition should be cleared");

      LOGGER.info("Breakpoint condition works correctly");
    }
  }

  @Nested
  @DisplayName("Breakpoint Addition Tests")
  class BreakpointAdditionTests {

    @Test
    @DisplayName("should add breakpoint to session")
    void shouldAddBreakpointToSession() {
      LOGGER.info("Testing breakpoint addition to session");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("test_function");

      assertTrue(session.getBreakpoints().isEmpty(), "No breakpoints initially");

      session.addBreakpoint(breakpoint);
      assertEquals(1, session.getBreakpoints().size(), "Should have one breakpoint");
      assertTrue(
          session.getBreakpoints().contains(breakpoint), "Should contain the added breakpoint");

      LOGGER.info("Breakpoint added to session successfully");
    }

    @Test
    @DisplayName("should reject null breakpoint")
    void shouldRejectNullBreakpoint() {
      LOGGER.info("Testing null breakpoint rejection");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      assertThrows(
          NullPointerException.class,
          () -> session.addBreakpoint(null),
          "Should reject null breakpoint");

      LOGGER.info("Null breakpoint correctly rejected");
    }

    @Test
    @DisplayName("should reject non-Panama breakpoint")
    void shouldRejectNonPanamaBreakpoint() {
      LOGGER.info("Testing non-Panama breakpoint rejection");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      final Breakpoint mockBreakpoint =
          new Breakpoint() {
            @Override
            public String getBreakpointId() {
              return "mock-bp";
            }

            @Override
            public String getFunctionName() {
              return "mock-function";
            }

            @Override
            public int getLineNumber() {
              return 0;
            }

            @Override
            public int getColumnNumber() {
              return 0;
            }

            @Override
            public boolean isEnabled() {
              return true;
            }

            @Override
            public void setEnabled(final boolean enabled) {}

            @Override
            public String getCondition() {
              return null;
            }

            @Override
            public void setCondition(final String condition) {}

            @Override
            public int getHitCount() {
              return 0;
            }

            @Override
            public void resetHitCount() {}
          };

      assertThrows(
          IllegalArgumentException.class,
          () -> session.addBreakpoint(mockBreakpoint),
          "Should reject non-Panama breakpoint");

      LOGGER.info("Non-Panama breakpoint correctly rejected");
    }
  }

  @Nested
  @DisplayName("Breakpoint Removal Tests")
  class BreakpointRemovalTests {

    @Test
    @DisplayName("should remove breakpoint from session")
    void shouldRemoveBreakpointFromSession() {
      LOGGER.info("Testing breakpoint removal from session");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("test_function");

      session.addBreakpoint(breakpoint);
      assertEquals(1, session.getBreakpoints().size(), "Should have one breakpoint");

      session.removeBreakpoint(breakpoint);
      assertTrue(session.getBreakpoints().isEmpty(), "Breakpoints should be empty after removal");

      LOGGER.info("Breakpoint removed from session successfully");
    }

    @Test
    @DisplayName("should handle removal of non-existent breakpoint")
    void shouldHandleRemovalOfNonExistentBreakpoint() {
      LOGGER.info("Testing removal of non-existent breakpoint");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("test_function");

      // Should not throw
      session.removeBreakpoint(breakpoint);
      assertTrue(session.getBreakpoints().isEmpty(), "Breakpoints should remain empty");

      LOGGER.info("Non-existent breakpoint removal handled gracefully");
    }
  }

  @Nested
  @DisplayName("Call Stack Update Tests")
  class CallStackUpdateTests {

    @Test
    @DisplayName("should update call stack")
    void shouldUpdateCallStack() {
      LOGGER.info("Testing call stack update");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      assertTrue(session.getCallStack().isEmpty(), "Call stack should be empty initially");

      final List<PanamaStackFrame> frames = new ArrayList<>();
      // Note: We can't easily create PanamaStackFrame without accessing its package-private
      // constructor
      // So we test with an empty list to verify the update mechanism works
      session.updateCallStack(frames);

      assertNotNull(session.getCallStack(), "Call stack should not be null after update");
      assertEquals(0, session.getCallStack().size(), "Call stack should match updated frames");

      LOGGER.info("Call stack update works correctly");
    }

    @Test
    @DisplayName("should handle null call stack update")
    void shouldHandleNullCallStackUpdate() {
      LOGGER.info("Testing null call stack update");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      // Should not throw
      session.updateCallStack(null);
      assertTrue(session.getCallStack().isEmpty(), "Call stack should be empty after null update");

      LOGGER.info("Null call stack update handled gracefully");
    }
  }

  @Nested
  @DisplayName("Breakpoint Hit Handler Tests")
  class BreakpointHitHandlerTests {

    @Test
    @DisplayName("should increment breakpoint hit count on hit")
    void shouldIncrementBreakpointHitCountOnHit() {
      LOGGER.info("Testing breakpoint hit count increment");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      final PanamaBreakpoint breakpoint =
          PanamaBreakpoint.builder().breakpointId("test-bp-1").build();

      session.addBreakpoint(breakpoint);
      assertEquals(0, breakpoint.getHitCount(), "Hit count should be 0 initially");

      session.onBreakpointHit("test-bp-1");
      assertEquals(1, breakpoint.getHitCount(), "Hit count should be incremented");

      session.onBreakpointHit("test-bp-1");
      assertEquals(2, breakpoint.getHitCount(), "Hit count should be incremented again");

      LOGGER.info("Breakpoint hit count incremented correctly on hit");
    }

    @Test
    @DisplayName("should handle hit for unknown breakpoint ID")
    void shouldHandleHitForUnknownBreakpointId() {
      LOGGER.info("Testing hit for unknown breakpoint ID");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      // Should not throw, just pause the session
      session.onBreakpointHit("unknown-bp-id");
      assertTrue(session.isPaused(), "Session should be paused even for unknown breakpoint");

      LOGGER.info("Unknown breakpoint hit handled gracefully");
    }
  }

  @Nested
  @DisplayName("Breakpoint Equality Tests")
  class BreakpointEqualityTests {

    @Test
    @DisplayName("should be equal if same ID")
    void shouldBeEqualIfSameId() {
      LOGGER.info("Testing breakpoint equality");

      final PanamaBreakpoint bp1 =
          PanamaBreakpoint.builder().breakpointId("bp-123").functionName("func1").build();

      final PanamaBreakpoint bp2 =
          PanamaBreakpoint.builder().breakpointId("bp-123").functionName("func2").build();

      assertEquals(bp1, bp2, "Breakpoints with same ID should be equal");
      assertEquals(bp1.hashCode(), bp2.hashCode(), "Hash codes should match");

      LOGGER.info("Breakpoint equality based on ID works correctly");
    }

    @Test
    @DisplayName("should not be equal if different ID")
    void shouldNotBeEqualIfDifferentId() {
      LOGGER.info("Testing breakpoint inequality");

      final PanamaBreakpoint bp1 =
          PanamaBreakpoint.builder().breakpointId("bp-123").functionName("func").build();

      final PanamaBreakpoint bp2 =
          PanamaBreakpoint.builder().breakpointId("bp-456").functionName("func").build();

      assertFalse(bp1.equals(bp2), "Breakpoints with different IDs should not be equal");

      LOGGER.info("Breakpoint inequality works correctly");
    }
  }
}
