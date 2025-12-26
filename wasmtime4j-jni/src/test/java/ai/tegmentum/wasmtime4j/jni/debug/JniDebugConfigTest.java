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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.DebugConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link JniDebugConfig}.
 */
@DisplayName("JniDebugConfig Tests")
class JniDebugConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniDebugConfig should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniDebugConfig.class.getModifiers()),
          "JniDebugConfig should be final");
    }

    @Test
    @DisplayName("JniDebugConfig should implement DebugConfig")
    void shouldImplementDebugConfig() {
      assertTrue(
          DebugConfig.class.isAssignableFrom(JniDebugConfig.class),
          "JniDebugConfig should implement DebugConfig");
    }
  }

  @Nested
  @DisplayName("Default Constants Tests")
  class DefaultConstantsTests {

    @Test
    @DisplayName("DEFAULT_DEBUG_PORT should be 9229")
    void defaultDebugPortShouldBe9229() {
      assertEquals(9229, JniDebugConfig.DEFAULT_DEBUG_PORT,
          "Default debug port should be 9229");
    }

    @Test
    @DisplayName("DEFAULT_HOST_ADDRESS should be 127.0.0.1")
    void defaultHostAddressShouldBeLocalhost() {
      assertEquals("127.0.0.1", JniDebugConfig.DEFAULT_HOST_ADDRESS,
          "Default host address should be 127.0.0.1");
    }

    @Test
    @DisplayName("DEFAULT_SESSION_TIMEOUT should be 300000ms")
    void defaultSessionTimeoutShouldBe5Minutes() {
      assertEquals(300_000L, JniDebugConfig.DEFAULT_SESSION_TIMEOUT,
          "Default session timeout should be 300000ms (5 minutes)");
    }

    @Test
    @DisplayName("DEFAULT_MAX_BREAKPOINTS should be 1024")
    void defaultMaxBreakpointsShouldBe1024() {
      assertEquals(1024, JniDebugConfig.DEFAULT_MAX_BREAKPOINTS,
          "Default max breakpoints should be 1024");
    }

    @Test
    @DisplayName("DEFAULT_LOG_LEVEL should be INFO")
    void defaultLogLevelShouldBeInfo() {
      assertEquals("INFO", JniDebugConfig.DEFAULT_LOG_LEVEL,
          "Default log level should be INFO");
    }
  }

  @Nested
  @DisplayName("getDefault() Tests")
  class GetDefaultTests {

    @Test
    @DisplayName("getDefault should return config with default values")
    void getDefaultShouldReturnConfigWithDefaultValues() {
      final JniDebugConfig config = JniDebugConfig.getDefault();

      assertNotNull(config, "Default config should not be null");
      assertEquals(9229, config.getDebugPort(), "Default debug port");
      assertEquals("127.0.0.1", config.getHostAddress(), "Default host address");
      assertFalse(config.isRemoteDebuggingEnabled(), "Remote debugging disabled by default");
      assertEquals(300_000L, config.getSessionTimeout(), "Default session timeout");
      assertTrue(config.isBreakpointsEnabled(), "Breakpoints enabled by default");
      assertEquals(1024, config.getMaxBreakpoints(), "Default max breakpoints");
      assertTrue(config.isStepDebuggingEnabled(), "Step debugging enabled by default");
      assertEquals("INFO", config.getLogLevel(), "Default log level");
    }

    @Test
    @DisplayName("getDefault should return new instance each time")
    void getDefaultShouldReturnNewInstanceEachTime() {
      final JniDebugConfig config1 = JniDebugConfig.getDefault();
      final JniDebugConfig config2 = JniDebugConfig.getDefault();

      // They should be equal but not the same instance
      assertEquals(config1, config2, "Default configs should be equal");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should create config with all custom values")
    void builderShouldCreateConfigWithAllCustomValues() {
      final JniDebugConfig config = JniDebugConfig.builder()
          .debugPort(8080)
          .hostAddress("0.0.0.0")
          .remoteDebuggingEnabled(true)
          .sessionTimeout(60_000L)
          .breakpointsEnabled(false)
          .maxBreakpoints(512)
          .stepDebuggingEnabled(false)
          .logLevel("DEBUG")
          .build();

      assertEquals(8080, config.getDebugPort(), "Custom debug port");
      assertEquals("0.0.0.0", config.getHostAddress(), "Custom host address");
      assertTrue(config.isRemoteDebuggingEnabled(), "Remote debugging enabled");
      assertEquals(60_000L, config.getSessionTimeout(), "Custom session timeout");
      assertFalse(config.isBreakpointsEnabled(), "Breakpoints disabled");
      assertEquals(512, config.getMaxBreakpoints(), "Custom max breakpoints");
      assertFalse(config.isStepDebuggingEnabled(), "Step debugging disabled");
      assertEquals("DEBUG", config.getLogLevel(), "Custom log level");
    }

    @Test
    @DisplayName("Builder should use default values when not set")
    void builderShouldUseDefaultValuesWhenNotSet() {
      final JniDebugConfig config = JniDebugConfig.builder().build();

      assertEquals(JniDebugConfig.DEFAULT_DEBUG_PORT, config.getDebugPort());
      assertEquals(JniDebugConfig.DEFAULT_HOST_ADDRESS, config.getHostAddress());
      assertEquals(JniDebugConfig.DEFAULT_SESSION_TIMEOUT, config.getSessionTimeout());
      assertEquals(JniDebugConfig.DEFAULT_MAX_BREAKPOINTS, config.getMaxBreakpoints());
      assertEquals(JniDebugConfig.DEFAULT_LOG_LEVEL, config.getLogLevel());
    }

    @Test
    @DisplayName("Builder should allow chaining")
    void builderShouldAllowChaining() {
      final JniDebugConfig config = JniDebugConfig.builder()
          .debugPort(9000)
          .hostAddress("localhost")
          .remoteDebuggingEnabled(true)
          .sessionTimeout(120_000L)
          .breakpointsEnabled(true)
          .maxBreakpoints(256)
          .stepDebuggingEnabled(true)
          .logLevel("TRACE")
          .build();

      assertNotNull(config, "Chained builder should produce config");
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("debugPort should reject negative port")
    void debugPortShouldRejectNegativePort() {
      final JniDebugConfig.Builder builder = JniDebugConfig.builder();

      assertThrows(IllegalArgumentException.class,
          () -> builder.debugPort(-1),
          "Should reject negative port");
    }

    @Test
    @DisplayName("debugPort should reject port greater than 65535")
    void debugPortShouldRejectPortGreaterThan65535() {
      final JniDebugConfig.Builder builder = JniDebugConfig.builder();

      assertThrows(IllegalArgumentException.class,
          () -> builder.debugPort(65536),
          "Should reject port > 65535");
    }

    @Test
    @DisplayName("debugPort should accept port 0")
    void debugPortShouldAcceptPort0() {
      final JniDebugConfig config = JniDebugConfig.builder()
          .debugPort(0)
          .build();

      assertEquals(0, config.getDebugPort(), "Should accept port 0");
    }

    @Test
    @DisplayName("debugPort should accept port 65535")
    void debugPortShouldAcceptPort65535() {
      final JniDebugConfig config = JniDebugConfig.builder()
          .debugPort(65535)
          .build();

      assertEquals(65535, config.getDebugPort(), "Should accept port 65535");
    }

    @Test
    @DisplayName("hostAddress should reject null")
    void hostAddressShouldRejectNull() {
      final JniDebugConfig.Builder builder = JniDebugConfig.builder();

      assertThrows(NullPointerException.class,
          () -> builder.hostAddress(null),
          "Should reject null host address");
    }

    @Test
    @DisplayName("sessionTimeout should reject negative timeout")
    void sessionTimeoutShouldRejectNegativeTimeout() {
      final JniDebugConfig.Builder builder = JniDebugConfig.builder();

      assertThrows(IllegalArgumentException.class,
          () -> builder.sessionTimeout(-1L),
          "Should reject negative timeout");
    }

    @Test
    @DisplayName("sessionTimeout should accept zero timeout")
    void sessionTimeoutShouldAcceptZeroTimeout() {
      final JniDebugConfig config = JniDebugConfig.builder()
          .sessionTimeout(0L)
          .build();

      assertEquals(0L, config.getSessionTimeout(), "Should accept zero timeout");
    }

    @Test
    @DisplayName("maxBreakpoints should reject negative value")
    void maxBreakpointsShouldRejectNegativeValue() {
      final JniDebugConfig.Builder builder = JniDebugConfig.builder();

      assertThrows(IllegalArgumentException.class,
          () -> builder.maxBreakpoints(-1),
          "Should reject negative max breakpoints");
    }

    @Test
    @DisplayName("maxBreakpoints should accept zero")
    void maxBreakpointsShouldAcceptZero() {
      final JniDebugConfig config = JniDebugConfig.builder()
          .maxBreakpoints(0)
          .build();

      assertEquals(0, config.getMaxBreakpoints(), "Should accept zero max breakpoints");
    }

    @Test
    @DisplayName("logLevel should reject null")
    void logLevelShouldRejectNull() {
      final JniDebugConfig.Builder builder = JniDebugConfig.builder();

      assertThrows(NullPointerException.class,
          () -> builder.logLevel(null),
          "Should reject null log level");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include all fields")
    void toStringShouldIncludeAllFields() {
      final JniDebugConfig config = JniDebugConfig.builder()
          .debugPort(9229)
          .hostAddress("127.0.0.1")
          .remoteDebuggingEnabled(true)
          .sessionTimeout(300_000L)
          .breakpointsEnabled(true)
          .maxBreakpoints(1024)
          .stepDebuggingEnabled(true)
          .logLevel("INFO")
          .build();

      final String str = config.toString();

      assertTrue(str.contains("debugPort=9229"), "Should contain debug port");
      assertTrue(str.contains("hostAddress='127.0.0.1'"), "Should contain host address");
      assertTrue(str.contains("remoteDebugging=true"), "Should contain remote debugging");
      assertTrue(str.contains("sessionTimeout=300000"), "Should contain session timeout");
      assertTrue(str.contains("breakpoints=true"), "Should contain breakpoints enabled");
      assertTrue(str.contains("maxBreakpoints=1024"), "Should contain max breakpoints");
      assertTrue(str.contains("stepDebugging=true"), "Should contain step debugging");
      assertTrue(str.contains("logLevel='INFO'"), "Should contain log level");
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("equals should return true for same values")
    void equalsShouldReturnTrueForSameValues() {
      final JniDebugConfig config1 = JniDebugConfig.builder()
          .debugPort(8080)
          .hostAddress("0.0.0.0")
          .remoteDebuggingEnabled(true)
          .sessionTimeout(60_000L)
          .breakpointsEnabled(false)
          .maxBreakpoints(512)
          .stepDebuggingEnabled(false)
          .logLevel("DEBUG")
          .build();

      final JniDebugConfig config2 = JniDebugConfig.builder()
          .debugPort(8080)
          .hostAddress("0.0.0.0")
          .remoteDebuggingEnabled(true)
          .sessionTimeout(60_000L)
          .breakpointsEnabled(false)
          .maxBreakpoints(512)
          .stepDebuggingEnabled(false)
          .logLevel("DEBUG")
          .build();

      assertEquals(config1, config2, "Configs with same values should be equal");
    }

    @Test
    @DisplayName("equals should return false for different debug port")
    void equalsShouldReturnFalseForDifferentDebugPort() {
      final JniDebugConfig config1 = JniDebugConfig.builder().debugPort(8080).build();
      final JniDebugConfig config2 = JniDebugConfig.builder().debugPort(9000).build();

      assertNotEquals(config1, config2, "Configs with different ports should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different host address")
    void equalsShouldReturnFalseForDifferentHostAddress() {
      final JniDebugConfig config1 = JniDebugConfig.builder()
          .hostAddress("127.0.0.1").build();
      final JniDebugConfig config2 = JniDebugConfig.builder()
          .hostAddress("0.0.0.0").build();

      assertNotEquals(config1, config2, "Configs with different hosts should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different remote debugging setting")
    void equalsShouldReturnFalseForDifferentRemoteDebuggingSetting() {
      final JniDebugConfig config1 = JniDebugConfig.builder()
          .remoteDebuggingEnabled(true).build();
      final JniDebugConfig config2 = JniDebugConfig.builder()
          .remoteDebuggingEnabled(false).build();

      assertNotEquals(config1, config2,
          "Configs with different remote debugging should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different session timeout")
    void equalsShouldReturnFalseForDifferentSessionTimeout() {
      final JniDebugConfig config1 = JniDebugConfig.builder().sessionTimeout(60_000L).build();
      final JniDebugConfig config2 = JniDebugConfig.builder().sessionTimeout(120_000L).build();

      assertNotEquals(config1, config2, "Configs with different timeouts should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different log level")
    void equalsShouldReturnFalseForDifferentLogLevel() {
      final JniDebugConfig config1 = JniDebugConfig.builder().logLevel("INFO").build();
      final JniDebugConfig config2 = JniDebugConfig.builder().logLevel("DEBUG").build();

      assertNotEquals(config1, config2, "Configs with different log levels should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final JniDebugConfig config = JniDebugConfig.getDefault();
      assertNotEquals(null, config, "Should not be equal to null");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final JniDebugConfig config = JniDebugConfig.getDefault();
      assertNotEquals("config", config, "Should not be equal to String");
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final JniDebugConfig config = JniDebugConfig.getDefault();
      assertEquals(config, config, "Should be equal to itself");
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void hashCodeShouldBeConsistentWithEquals() {
      final JniDebugConfig config1 = JniDebugConfig.builder()
          .debugPort(8080)
          .hostAddress("localhost")
          .build();

      final JniDebugConfig config2 = JniDebugConfig.builder()
          .debugPort(8080)
          .hostAddress("localhost")
          .build();

      assertEquals(config1.hashCode(), config2.hashCode(),
          "Equal configs should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should be stable across multiple calls")
    void hashCodeShouldBeStableAcrossMultipleCalls() {
      final JniDebugConfig config = JniDebugConfig.getDefault();

      final int hash1 = config.hashCode();
      final int hash2 = config.hashCode();
      final int hash3 = config.hashCode();

      assertEquals(hash1, hash2, "Hash should be stable");
      assertEquals(hash2, hash3, "Hash should be stable");
    }
  }
}
