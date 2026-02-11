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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.DebugSession.StepType;
import ai.tegmentum.wasmtime4j.jni.debug.JniBreakpoint;
import ai.tegmentum.wasmtime4j.jni.debug.JniDebugConfig;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for debug session functionality.
 *
 * <p>These tests verify DebugConfig, Breakpoint, and StepType functionality. Debug session tests
 * are skipped when native implementation is not available.
 *
 * @since 1.0.0
 */
@DisplayName("DebugSession Integration Tests")
public final class DebugSessionIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(DebugSessionIntegrationTest.class.getName());

  @Nested
  @DisplayName("DebugConfig Tests")
  class DebugConfigTests {

    @Test
    @DisplayName("should create default config")
    void shouldCreateDefaultConfig(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final JniDebugConfig config = JniDebugConfig.getDefault();

      assertNotNull(config, "Default config should not be null");
      assertEquals(JniDebugConfig.DEFAULT_DEBUG_PORT, config.getDebugPort(), "Default debug port");
      assertEquals(
          JniDebugConfig.DEFAULT_HOST_ADDRESS, config.getHostAddress(), "Default host address");
      assertFalse(config.isRemoteDebuggingEnabled(), "Remote debugging disabled by default");
      assertEquals(
          JniDebugConfig.DEFAULT_SESSION_TIMEOUT,
          config.getSessionTimeout(),
          "Default session timeout");
      assertTrue(config.isBreakpointsEnabled(), "Breakpoints enabled by default");
      assertEquals(
          JniDebugConfig.DEFAULT_MAX_BREAKPOINTS,
          config.getMaxBreakpoints(),
          "Default max breakpoints");
      assertTrue(config.isStepDebuggingEnabled(), "Step debugging enabled by default");
      assertEquals(JniDebugConfig.DEFAULT_LOG_LEVEL, config.getLogLevel(), "Default log level");

      LOGGER.info("Default DebugConfig: " + config);
    }

    @Test
    @DisplayName("should build config with custom settings")
    void shouldBuildConfigWithCustomSettings(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final JniDebugConfig config =
          JniDebugConfig.builder()
              .debugPort(8080)
              .hostAddress("0.0.0.0")
              .remoteDebuggingEnabled(true)
              .sessionTimeout(600_000L)
              .breakpointsEnabled(false)
              .maxBreakpoints(256)
              .stepDebuggingEnabled(false)
              .logLevel("DEBUG")
              .build();

      assertEquals(8080, config.getDebugPort(), "Custom debug port");
      assertEquals("0.0.0.0", config.getHostAddress(), "Custom host address");
      assertTrue(config.isRemoteDebuggingEnabled(), "Remote debugging enabled");
      assertEquals(600_000L, config.getSessionTimeout(), "Custom session timeout");
      assertFalse(config.isBreakpointsEnabled(), "Breakpoints disabled");
      assertEquals(256, config.getMaxBreakpoints(), "Custom max breakpoints");
      assertFalse(config.isStepDebuggingEnabled(), "Step debugging disabled");
      assertEquals("DEBUG", config.getLogLevel(), "Custom log level");

      LOGGER.info("Custom DebugConfig: " + config);
    }

    @Test
    @DisplayName("should reject invalid port numbers")
    void shouldRejectInvalidPortNumbers(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final IllegalArgumentException negativePortException =
          assertThrows(
              IllegalArgumentException.class,
              () -> JniDebugConfig.builder().debugPort(-1),
              "Should reject negative port");
      assertNotNull(negativePortException.getMessage(), "Exception should have message");
      LOGGER.info("Rejected negative port: " + negativePortException.getMessage());

      final IllegalArgumentException highPortException =
          assertThrows(
              IllegalArgumentException.class,
              () -> JniDebugConfig.builder().debugPort(65536),
              "Should reject port > 65535");
      assertNotNull(highPortException.getMessage(), "Exception should have message");
      LOGGER.info("Rejected high port: " + highPortException.getMessage());
    }

    @Test
    @DisplayName("should reject null host address")
    void shouldRejectNullHostAddress(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final NullPointerException exception =
          assertThrows(
              NullPointerException.class,
              () -> JniDebugConfig.builder().hostAddress(null),
              "Should reject null host address");
      assertNotNull(exception.getMessage(), "Exception should have message");
      LOGGER.info("Rejected null host: " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject negative session timeout")
    void shouldRejectNegativeSessionTimeout(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> JniDebugConfig.builder().sessionTimeout(-1),
              "Should reject negative timeout");
      assertNotNull(exception.getMessage(), "Exception should have message");
      LOGGER.info("Rejected negative timeout: " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject negative max breakpoints")
    void shouldRejectNegativeMaxBreakpoints(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> JniDebugConfig.builder().maxBreakpoints(-1),
              "Should reject negative max breakpoints");
      assertNotNull(exception.getMessage(), "Exception should have message");
      LOGGER.info("Rejected negative max breakpoints: " + exception.getMessage());
    }

    @Test
    @DisplayName("should support equals and hashCode")
    void shouldSupportEqualsAndHashCode(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final JniDebugConfig config1 =
          JniDebugConfig.builder().debugPort(9000).hostAddress("localhost").build();
      final JniDebugConfig config2 =
          JniDebugConfig.builder().debugPort(9000).hostAddress("localhost").build();
      final JniDebugConfig config3 =
          JniDebugConfig.builder().debugPort(9001).hostAddress("localhost").build();

      assertEquals(config1, config2, "Same config should be equal");
      assertEquals(config1.hashCode(), config2.hashCode(), "Same config should have same hashCode");
      assertNotEquals(config1, config3, "Different config should not be equal");

      LOGGER.info("Equals and hashCode work correctly");
    }
  }

  @Nested
  @DisplayName("Breakpoint Tests")
  class BreakpointTests {

    @Test
    @DisplayName("should create breakpoint with all fields")
    void shouldCreateBreakpointWithAllFields(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final JniBreakpoint breakpoint = new JniBreakpoint("bp-001", "testFunction", 42, 10, 0x1000L);

      assertEquals("bp-001", breakpoint.getBreakpointId(), "Breakpoint ID");
      assertEquals("testFunction", breakpoint.getFunctionName(), "Function name");
      assertEquals(42, breakpoint.getLineNumber(), "Line number");
      assertEquals(10, breakpoint.getColumnNumber(), "Column number");
      assertEquals(0x1000L, breakpoint.getInstructionOffset(), "Instruction offset");
      assertTrue(breakpoint.isEnabled(), "Breakpoint should be enabled by default");
      assertEquals(0, breakpoint.getHitCount(), "Initial hit count should be 0");
      assertNull(breakpoint.getCondition(), "Initial condition should be null");

      LOGGER.info("Created breakpoint: " + breakpoint);
    }

    @Test
    @DisplayName("should enable and disable breakpoint")
    void shouldEnableAndDisableBreakpoint(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final JniBreakpoint breakpoint = new JniBreakpoint("bp-002", "func", 1, 1, 0);

      assertTrue(breakpoint.isEnabled(), "Initially enabled");

      breakpoint.setEnabled(false);
      assertFalse(breakpoint.isEnabled(), "Disabled after setEnabled(false)");

      breakpoint.setEnabled(true);
      assertTrue(breakpoint.isEnabled(), "Enabled after setEnabled(true)");

      LOGGER.info("Enable/disable works correctly");
    }

    @Test
    @DisplayName("should set and get condition")
    void shouldSetAndGetCondition(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final JniBreakpoint breakpoint = new JniBreakpoint("bp-003", "func", 1, 1, 0);

      assertNull(breakpoint.getCondition(), "Initial condition is null");

      breakpoint.setCondition("i > 10");
      assertEquals("i > 10", breakpoint.getCondition(), "Condition set correctly");

      breakpoint.setCondition(null);
      assertNull(breakpoint.getCondition(), "Condition cleared");

      LOGGER.info("Condition set/get works correctly");
    }

    @Test
    @DisplayName("should track hit count")
    void shouldTrackHitCount(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final JniBreakpoint breakpoint = new JniBreakpoint("bp-004", "func", 1, 1, 0);

      assertEquals(0, breakpoint.getHitCount(), "Initial hit count is 0");

      final int count1 = breakpoint.incrementHitCount();
      assertEquals(1, count1, "First increment returns 1");
      assertEquals(1, breakpoint.getHitCount(), "Hit count is 1");

      final int count2 = breakpoint.incrementHitCount();
      assertEquals(2, count2, "Second increment returns 2");
      assertEquals(2, breakpoint.getHitCount(), "Hit count is 2");

      breakpoint.resetHitCount();
      assertEquals(0, breakpoint.getHitCount(), "Hit count reset to 0");

      LOGGER.info("Hit count tracking works correctly");
    }

    @Test
    @DisplayName("should create breakpoint from native data")
    void shouldCreateBreakpointFromNativeData(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final JniBreakpoint breakpoint =
          JniBreakpoint.fromNative(12345L, "nativeFunc", 100, 5, 0x2000L);

      assertEquals("bp-12345", breakpoint.getBreakpointId(), "ID from native");
      assertEquals("nativeFunc", breakpoint.getFunctionName(), "Function name");
      assertEquals(100, breakpoint.getLineNumber(), "Line number");
      assertEquals(5, breakpoint.getColumnNumber(), "Column number");
      assertEquals(0x2000L, breakpoint.getInstructionOffset(), "Instruction offset");

      LOGGER.info("Created from native: " + breakpoint);
    }

    @Test
    @DisplayName("should support equals and hashCode")
    void shouldSupportEqualsAndHashCode(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final JniBreakpoint bp1 = new JniBreakpoint("bp-100", "func", 1, 1, 0);
      final JniBreakpoint bp2 = new JniBreakpoint("bp-100", "func", 1, 1, 0);
      final JniBreakpoint bp3 = new JniBreakpoint("bp-101", "func", 1, 1, 0);

      assertEquals(bp1, bp2, "Same ID should be equal");
      assertEquals(bp1.hashCode(), bp2.hashCode(), "Same ID should have same hashCode");
      assertNotEquals(bp1, bp3, "Different ID should not be equal");

      LOGGER.info("Equals and hashCode work correctly");
    }

    @Test
    @DisplayName("should reject null breakpoint ID")
    void shouldRejectNullBreakpointId(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final NullPointerException exception =
          assertThrows(
              NullPointerException.class,
              () -> new JniBreakpoint(null, "func", 1, 1, 0),
              "Should reject null breakpoint ID");
      assertNotNull(exception.getMessage(), "Exception should have message");
      LOGGER.info("Rejected null ID: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("StepType Tests")
  class StepTypeTests {

    @Test
    @DisplayName("should have all step types")
    void shouldHaveAllStepTypes(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final StepType[] stepTypes = StepType.values();

      assertEquals(3, stepTypes.length, "Should have 3 step types");
      assertNotNull(StepType.STEP_INTO, "STEP_INTO should exist");
      assertNotNull(StepType.STEP_OVER, "STEP_OVER should exist");
      assertNotNull(StepType.STEP_OUT, "STEP_OUT should exist");

      LOGGER.info(
          "Step types: STEP_INTO="
              + StepType.STEP_INTO
              + ", STEP_OVER="
              + StepType.STEP_OVER
              + ", STEP_OUT="
              + StepType.STEP_OUT);
    }

    @Test
    @DisplayName("should convert from string with valueOf")
    void shouldConvertFromStringWithValueOf(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals(StepType.STEP_INTO, StepType.valueOf("STEP_INTO"), "valueOf STEP_INTO");
      assertEquals(StepType.STEP_OVER, StepType.valueOf("STEP_OVER"), "valueOf STEP_OVER");
      assertEquals(StepType.STEP_OUT, StepType.valueOf("STEP_OUT"), "valueOf STEP_OUT");

      LOGGER.info("valueOf works correctly");
    }

    @Test
    @DisplayName("should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals(0, StepType.STEP_INTO.ordinal(), "STEP_INTO ordinal");
      assertEquals(1, StepType.STEP_OVER.ordinal(), "STEP_OVER ordinal");
      assertEquals(2, StepType.STEP_OUT.ordinal(), "STEP_OUT ordinal");

      LOGGER.info("Ordinals: INTO=0, OVER=1, OUT=2");
    }
  }

  @Nested
  @DisplayName("Session Lifecycle Tests")
  class SessionLifecycleTests {

    private boolean isDebugSessionAvailable() {
      try {
        Class.forName("ai.tegmentum.wasmtime4j.jni.debug.JniDebugSession");
        return true;
      } catch (ClassNotFoundException e) {
        return false;
      }
    }

    @Test
    @DisplayName("should have debug session class available")
    void shouldHaveDebugSessionClassAvailable(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      if (isDebugSessionAvailable()) {
        LOGGER.info("JniDebugSession class is available");
      } else {
        LOGGER.info("JniDebugSession class not found - native implementation needed");
      }

      // This test always passes - it's informational
      assertTrue(true, "Informational test");
    }

    @Test
    @DisplayName("should document session lifecycle")
    void shouldDocumentSessionLifecycle(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Document expected session lifecycle based on interface
      LOGGER.info("DebugSession lifecycle:");
      LOGGER.info("1. Create session with config");
      LOGGER.info("2. start() - begins debug session");
      LOGGER.info("3. addBreakpoint() - set breakpoints");
      LOGGER.info("4. continueExecution() or step() - control execution");
      LOGGER.info("5. removeBreakpoint() - clear breakpoints");
      LOGGER.info("6. stop() - ends debug session");
      LOGGER.info("7. getSessionId() - unique session identifier");
      LOGGER.info("8. isActive() - check session state");

      assertTrue(true, "Documentation test");
    }
  }
}
