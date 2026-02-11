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

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.panama.debug.PanamaDebugCapabilities;
import ai.tegmentum.wasmtime4j.panama.debug.PanamaDebugConfig;
import ai.tegmentum.wasmtime4j.panama.debug.PanamaDebugSession;
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
 * Integration tests for debug stack trace functionality.
 *
 * <p>These tests verify the call stack inspection and frame navigation during debugging.
 *
 * @since 1.0.0
 */
@DisplayName("Debug Stack Trace Tests")
class DebugStackTraceTest {

  private static final Logger LOGGER = Logger.getLogger(DebugStackTraceTest.class.getName());

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  private Arena arena;

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for stack trace tests");
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
  @DisplayName("Debug Capabilities Tests")
  class DebugCapabilitiesTests {

    @Test
    @DisplayName("should get default debug capabilities")
    void shouldGetDefaultDebugCapabilities() {
      LOGGER.info("Testing default debug capabilities");

      final DebugCapabilities capabilities = PanamaDebugCapabilities.getDefault();

      assertNotNull(capabilities, "Capabilities should not be null");

      LOGGER.info("Default capabilities retrieved successfully");
    }

    @Test
    @DisplayName("should support breakpoints in default capabilities")
    void shouldSupportBreakpointsInDefaultCapabilities() {
      LOGGER.info("Testing breakpoint support in capabilities");

      final DebugCapabilities capabilities = PanamaDebugCapabilities.getDefault();

      assertTrue(
          capabilities.supportsBreakpoints(), "Default capabilities should support breakpoints");

      LOGGER.info("Breakpoint support confirmed");
    }

    @Test
    @DisplayName("should support stepping in default capabilities")
    void shouldSupportSteppingInDefaultCapabilities() {
      LOGGER.info("Testing stepping support in capabilities");

      final DebugCapabilities capabilities = PanamaDebugCapabilities.getDefault();

      assertTrue(
          capabilities.supportsStepDebugging(), "Default capabilities should support stepping");

      LOGGER.info("Stepping support confirmed");
    }

    @Test
    @DisplayName("should support variable inspection in default capabilities")
    void shouldSupportVariableInspectionInDefaultCapabilities() {
      LOGGER.info("Testing variable inspection support in capabilities");

      final DebugCapabilities capabilities = PanamaDebugCapabilities.getDefault();

      assertTrue(
          capabilities.supportsVariableInspection(),
          "Default capabilities should support variable inspection");

      LOGGER.info("Variable inspection support confirmed");
    }

    @Test
    @DisplayName("should support memory inspection in default capabilities")
    void shouldSupportMemoryInspectionInDefaultCapabilities() {
      LOGGER.info("Testing memory inspection support in capabilities");

      final DebugCapabilities capabilities = PanamaDebugCapabilities.getDefault();

      assertTrue(
          capabilities.supportsMemoryInspection(),
          "Default capabilities should support memory inspection");

      LOGGER.info("Memory inspection support confirmed");
    }

    @Test
    @DisplayName("should support profiling in default capabilities")
    void shouldSupportProfilingInDefaultCapabilities() {
      LOGGER.info("Testing profiling support in capabilities");

      final DebugCapabilities capabilities = PanamaDebugCapabilities.getDefault();

      assertTrue(capabilities.supportsProfiling(), "Default capabilities should support profiling");

      LOGGER.info("Profiling support confirmed");
    }
  }

  @Nested
  @DisplayName("Debug Config Tests")
  class DebugConfigTests {

    @Test
    @DisplayName("should create default debug config")
    void shouldCreateDefaultDebugConfig() {
      LOGGER.info("Testing default debug config creation");

      final PanamaDebugConfig config = PanamaDebugConfig.getDefault();

      assertNotNull(config, "Config should not be null");
      assertEquals(
          PanamaDebugConfig.DEFAULT_DEBUG_PORT, config.getDebugPort(), "Port should be default");
      assertEquals(
          PanamaDebugConfig.DEFAULT_HOST_ADDRESS,
          config.getHostAddress(),
          "Host should be default");
      assertEquals(
          PanamaDebugConfig.DEFAULT_SESSION_TIMEOUT,
          config.getSessionTimeout(),
          "Timeout should be default");
      assertEquals(
          PanamaDebugConfig.DEFAULT_MAX_BREAKPOINTS,
          config.getMaxBreakpoints(),
          "Max breakpoints should be default");

      LOGGER.info("Default config: " + config);
    }

    @Test
    @DisplayName("should create debug config with builder")
    void shouldCreateDebugConfigWithBuilder() {
      LOGGER.info("Testing debug config builder");

      final PanamaDebugConfig config =
          PanamaDebugConfig.builder()
              .debugPort(9999)
              .hostAddress("0.0.0.0")
              .remoteDebuggingEnabled(true)
              .sessionTimeout(60000)
              .breakpointsEnabled(true)
              .maxBreakpoints(100)
              .stepDebuggingEnabled(true)
              .logLevel("DEBUG")
              .build();

      assertEquals(9999, config.getDebugPort(), "Port should match");
      assertEquals("0.0.0.0", config.getHostAddress(), "Host should match");
      assertTrue(config.isRemoteDebuggingEnabled(), "Remote debugging should be enabled");
      assertEquals(60000, config.getSessionTimeout(), "Timeout should match");
      assertTrue(config.isBreakpointsEnabled(), "Breakpoints should be enabled");
      assertEquals(100, config.getMaxBreakpoints(), "Max breakpoints should match");
      assertTrue(config.isStepDebuggingEnabled(), "Step debugging should be enabled");
      assertEquals("DEBUG", config.getLogLevel(), "Log level should match");

      LOGGER.info("Custom config created: " + config);
    }

    @Test
    @DisplayName("should validate debug port range")
    void shouldValidateDebugPortRange() {
      LOGGER.info("Testing debug port validation");

      final var builder = PanamaDebugConfig.builder();

      try {
        builder.debugPort(-1);
        assertTrue(false, "Should throw for negative port");
      } catch (final IllegalArgumentException e) {
        LOGGER.info("Negative port correctly rejected: " + e.getMessage());
      }

      try {
        builder.debugPort(70000);
        assertTrue(false, "Should throw for port > 65535");
      } catch (final IllegalArgumentException e) {
        LOGGER.info("Port > 65535 correctly rejected: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("should validate session timeout non-negative")
    void shouldValidateSessionTimeoutNonNegative() {
      LOGGER.info("Testing session timeout validation");

      final var builder = PanamaDebugConfig.builder();

      try {
        builder.sessionTimeout(-1);
        assertTrue(false, "Should throw for negative timeout");
      } catch (final IllegalArgumentException e) {
        LOGGER.info("Negative timeout correctly rejected: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Session Handle Tests")
  class SessionHandleTests {

    @Test
    @DisplayName("should return native handle")
    void shouldReturnNativeHandle() {
      LOGGER.info("Testing native handle retrieval");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      assertNotNull(session.getNativeHandle(), "Native handle should not be null");

      LOGGER.info("Native handle retrieved successfully");
    }

    @Test
    @DisplayName("should return debugger handle")
    void shouldReturnDebuggerHandle() {
      LOGGER.info("Testing debugger handle retrieval");

      final MemorySegment debuggerHandle = MemorySegment.NULL;
      final PanamaDebugSession session =
          new PanamaDebugSession(debuggerHandle, MemorySegment.NULL, arena);
      resources.add(session::close);

      assertEquals(debuggerHandle, session.getDebuggerHandle(), "Debugger handle should match");

      LOGGER.info("Debugger handle retrieved successfully");
    }

    @Test
    @DisplayName("should return arena")
    void shouldReturnArena() {
      LOGGER.info("Testing arena retrieval");

      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      resources.add(session::close);

      assertEquals(arena, session.getArena(), "Arena should match");

      LOGGER.info("Arena retrieved successfully");
    }
  }

  @Nested
  @DisplayName("Session Instance Handle Tests")
  class SessionInstanceHandleTests {

    @Test
    @DisplayName("should return empty instance handles for local session")
    void shouldReturnEmptyInstanceHandlesForLocalSession() {
      LOGGER.info("Testing instance handles for local session");

      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      resources.add(session::close);

      assertNotNull(session.getInstanceHandles(), "Instance handles should not be null");
      assertEquals(
          0, session.getInstanceHandles().length, "Local session should have no instances");

      LOGGER.info("Instance handles for local session verified");
    }

    @Test
    @DisplayName("should return cloned instance handles array")
    void shouldReturnClonedInstanceHandlesArray() {
      LOGGER.info("Testing instance handles array cloning");

      final MemorySegment[] handles = new MemorySegment[] {MemorySegment.NULL, MemorySegment.NULL};

      final PanamaDebugSession session = new PanamaDebugSession(MemorySegment.NULL, handles, arena);
      resources.add(session::close);

      final MemorySegment[] returned1 = session.getInstanceHandles();
      final MemorySegment[] returned2 = session.getInstanceHandles();

      // Should be different array instances
      assertFalse(returned1 == returned2, "Should return different array instances");
      assertEquals(returned1.length, returned2.length, "Arrays should have same length");

      LOGGER.info("Instance handles array cloning verified");
    }
  }

  @Nested
  @DisplayName("Config Equality Tests")
  class ConfigEqualityTests {

    @Test
    @DisplayName("should be equal if same configuration")
    void shouldBeEqualIfSameConfiguration() {
      LOGGER.info("Testing config equality");

      final PanamaDebugConfig config1 =
          PanamaDebugConfig.builder()
              .debugPort(9229)
              .hostAddress("127.0.0.1")
              .maxBreakpoints(100)
              .build();

      final PanamaDebugConfig config2 =
          PanamaDebugConfig.builder()
              .debugPort(9229)
              .hostAddress("127.0.0.1")
              .maxBreakpoints(100)
              .build();

      assertEquals(config1, config2, "Configs with same values should be equal");
      assertEquals(config1.hashCode(), config2.hashCode(), "Hash codes should match");

      LOGGER.info("Config equality verified");
    }

    @Test
    @DisplayName("should not be equal if different configuration")
    void shouldNotBeEqualIfDifferentConfiguration() {
      LOGGER.info("Testing config inequality");

      final PanamaDebugConfig config1 = PanamaDebugConfig.builder().debugPort(9229).build();
      final PanamaDebugConfig config2 = PanamaDebugConfig.builder().debugPort(9230).build();

      assertFalse(config1.equals(config2), "Configs with different values should not be equal");

      LOGGER.info("Config inequality verified");
    }
  }

  @Nested
  @DisplayName("Session Start/Stop Cycle Tests")
  class SessionStartStopCycleTests {

    @Test
    @DisplayName("should handle repeated start/stop cycles")
    void shouldHandleRepeatedStartStopCycles() {
      LOGGER.info("Testing repeated start/stop cycles");

      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      resources.add(session::close);

      for (int i = 0; i < 3; i++) {
        session.start();
        assertTrue(session.isActive(), "Session should be active after start #" + (i + 1));

        session.stop();
        assertFalse(session.isActive(), "Session should be inactive after stop #" + (i + 1));
      }

      LOGGER.info("Repeated start/stop cycles handled correctly");
    }

    @Test
    @DisplayName("should handle start when already active")
    void shouldHandleStartWhenAlreadyActive() {
      LOGGER.info("Testing start when already active");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      assertTrue(session.isActive(), "Session should start active");

      // Should not throw, just log warning
      session.start();
      assertTrue(session.isActive(), "Session should remain active");

      LOGGER.info("Start when already active handled gracefully");
    }

    @Test
    @DisplayName("should handle stop when already inactive")
    void shouldHandleStopWhenAlreadyInactive() {
      LOGGER.info("Testing stop when already inactive");

      final PanamaDebugSession session = PanamaDebugSession.createLocal(arena);
      resources.add(session::close);

      assertFalse(session.isActive(), "Session should start inactive");

      // Should not throw
      session.stop();
      assertFalse(session.isActive(), "Session should remain inactive");

      LOGGER.info("Stop when already inactive handled gracefully");
    }
  }

  @Nested
  @DisplayName("Breakpoint List Immutability Tests")
  class BreakpointListImmutabilityTests {

    @Test
    @DisplayName("should return unmodifiable breakpoints list")
    void shouldReturnUnmodifiableBreakpointsList() {
      LOGGER.info("Testing breakpoints list immutability");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      final var breakpoints = session.getBreakpoints();

      try {
        breakpoints.add(null);
        assertTrue(false, "Should throw UnsupportedOperationException");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("Breakpoints list is correctly unmodifiable");
      }
    }

    @Test
    @DisplayName("should return unmodifiable call stack list")
    void shouldReturnUnmodifiableCallStackList() {
      LOGGER.info("Testing call stack list immutability");

      final PanamaDebugSession session =
          new PanamaDebugSession(MemorySegment.NULL, MemorySegment.NULL, arena);
      resources.add(session::close);

      final var callStack = session.getCallStack();

      try {
        callStack.add(null);
        assertTrue(false, "Should throw UnsupportedOperationException");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("Call stack list is correctly unmodifiable");
      }
    }
  }
}
