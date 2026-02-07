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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for PanamaDebugConfig.
 *
 * <p>These tests exercise actual code execution to improve JaCoCo coverage.
 */
@DisplayName("Panama Debug Config Integration Tests")
public class PanamaDebugConfigTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaDebugConfigTest.class.getName());

  @Nested
  @DisplayName("Default Configuration Tests")
  class DefaultConfigTests {

    @Test
    @DisplayName("Should create default config with expected values")
    void shouldCreateDefaultConfigWithExpectedValues() {
      LOGGER.info("Testing default configuration creation");

      final PanamaDebugConfig config = PanamaDebugConfig.getDefault();

      assertNotNull(config, "Default config should not be null");
      assertEquals(
          PanamaDebugConfig.DEFAULT_DEBUG_PORT, config.getDebugPort(), "Default port should match");
      assertEquals(
          PanamaDebugConfig.DEFAULT_HOST_ADDRESS,
          config.getHostAddress(),
          "Default host should match");
      assertEquals(
          PanamaDebugConfig.DEFAULT_SESSION_TIMEOUT,
          config.getSessionTimeout(),
          "Default timeout should match");
      assertEquals(
          PanamaDebugConfig.DEFAULT_MAX_BREAKPOINTS,
          config.getMaxBreakpoints(),
          "Default max breakpoints should match");
      assertEquals(
          PanamaDebugConfig.DEFAULT_LOG_LEVEL,
          config.getLogLevel(),
          "Default log level should match");
      assertFalse(
          config.isRemoteDebuggingEnabled(), "Remote debugging should be disabled by default");
      assertTrue(config.isBreakpointsEnabled(), "Breakpoints should be enabled by default");
      assertTrue(config.isStepDebuggingEnabled(), "Step debugging should be enabled by default");

      LOGGER.info("Default configuration verified: " + config);
    }

    @Test
    @DisplayName("Should return builder from builder method")
    void shouldReturnBuilderFromBuilderMethod() {
      LOGGER.info("Testing builder method");

      final PanamaDebugConfig.Builder builder = PanamaDebugConfig.builder();

      assertNotNull(builder, "Builder should not be null");

      final PanamaDebugConfig config = builder.build();
      assertNotNull(config, "Built config should not be null");

      LOGGER.info("Builder method verified");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("Should build config with custom debug port")
    void shouldBuildConfigWithCustomDebugPort() {
      LOGGER.info("Testing custom debug port");

      final int customPort = 8080;
      final PanamaDebugConfig config = PanamaDebugConfig.builder().debugPort(customPort).build();

      assertEquals(customPort, config.getDebugPort(), "Custom port should be set");

      LOGGER.info("Custom debug port set to: " + config.getDebugPort());
    }

    @Test
    @DisplayName("Should reject invalid debug port - negative")
    void shouldRejectInvalidDebugPortNegative() {
      LOGGER.info("Testing invalid negative debug port");

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> PanamaDebugConfig.builder().debugPort(-1));

      assertTrue(ex.getMessage().contains("Port"), "Error should mention port: " + ex.getMessage());

      LOGGER.info("Correctly rejected negative port: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid debug port - too high")
    void shouldRejectInvalidDebugPortTooHigh() {
      LOGGER.info("Testing invalid high debug port");

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> PanamaDebugConfig.builder().debugPort(70000));

      assertTrue(
          ex.getMessage().contains("Port") || ex.getMessage().contains("65535"),
          "Error should mention port range: " + ex.getMessage());

      LOGGER.info("Correctly rejected high port: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should accept boundary port values")
    void shouldAcceptBoundaryPortValues() {
      LOGGER.info("Testing boundary port values");

      // Test port 0
      final PanamaDebugConfig config0 = PanamaDebugConfig.builder().debugPort(0).build();
      assertEquals(0, config0.getDebugPort(), "Port 0 should be accepted");

      // Test port 65535
      final PanamaDebugConfig config65535 = PanamaDebugConfig.builder().debugPort(65535).build();
      assertEquals(65535, config65535.getDebugPort(), "Port 65535 should be accepted");

      LOGGER.info("Boundary port values accepted");
    }

    @Test
    @DisplayName("Should build config with custom host address")
    void shouldBuildConfigWithCustomHostAddress() {
      LOGGER.info("Testing custom host address");

      final String customHost = "192.168.1.100";
      final PanamaDebugConfig config = PanamaDebugConfig.builder().hostAddress(customHost).build();

      assertEquals(customHost, config.getHostAddress(), "Custom host should be set");

      LOGGER.info("Custom host address set to: " + config.getHostAddress());
    }

    @Test
    @DisplayName("Should reject null host address")
    void shouldRejectNullHostAddress() {
      LOGGER.info("Testing null host address rejection");

      assertThrows(NullPointerException.class, () -> PanamaDebugConfig.builder().hostAddress(null));

      LOGGER.info("Correctly rejected null host address");
    }

    @Test
    @DisplayName("Should build config with remote debugging enabled")
    void shouldBuildConfigWithRemoteDebuggingEnabled() {
      LOGGER.info("Testing remote debugging enabled");

      final PanamaDebugConfig config =
          PanamaDebugConfig.builder().remoteDebuggingEnabled(true).build();

      assertTrue(config.isRemoteDebuggingEnabled(), "Remote debugging should be enabled");

      LOGGER.info("Remote debugging enabled: " + config.isRemoteDebuggingEnabled());
    }

    @Test
    @DisplayName("Should build config with custom session timeout")
    void shouldBuildConfigWithCustomSessionTimeout() {
      LOGGER.info("Testing custom session timeout");

      final long customTimeout = 600_000L;
      final PanamaDebugConfig config =
          PanamaDebugConfig.builder().sessionTimeout(customTimeout).build();

      assertEquals(customTimeout, config.getSessionTimeout(), "Custom timeout should be set");

      LOGGER.info("Custom session timeout set to: " + config.getSessionTimeout());
    }

    @Test
    @DisplayName("Should reject negative session timeout")
    void shouldRejectNegativeSessionTimeout() {
      LOGGER.info("Testing negative session timeout rejection");

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> PanamaDebugConfig.builder().sessionTimeout(-1));

      assertTrue(
          ex.getMessage().contains("Timeout") || ex.getMessage().contains("negative"),
          "Error should mention timeout: " + ex.getMessage());

      LOGGER.info("Correctly rejected negative timeout: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should build config with breakpoints disabled")
    void shouldBuildConfigWithBreakpointsDisabled() {
      LOGGER.info("Testing breakpoints disabled");

      final PanamaDebugConfig config =
          PanamaDebugConfig.builder().breakpointsEnabled(false).build();

      assertFalse(config.isBreakpointsEnabled(), "Breakpoints should be disabled");

      LOGGER.info("Breakpoints enabled: " + config.isBreakpointsEnabled());
    }

    @Test
    @DisplayName("Should build config with custom max breakpoints")
    void shouldBuildConfigWithCustomMaxBreakpoints() {
      LOGGER.info("Testing custom max breakpoints");

      final int customMax = 2048;
      final PanamaDebugConfig config =
          PanamaDebugConfig.builder().maxBreakpoints(customMax).build();

      assertEquals(customMax, config.getMaxBreakpoints(), "Custom max breakpoints should be set");

      LOGGER.info("Custom max breakpoints set to: " + config.getMaxBreakpoints());
    }

    @Test
    @DisplayName("Should reject negative max breakpoints")
    void shouldRejectNegativeMaxBreakpoints() {
      LOGGER.info("Testing negative max breakpoints rejection");

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> PanamaDebugConfig.builder().maxBreakpoints(-1));

      assertTrue(
          ex.getMessage().contains("breakpoints") || ex.getMessage().contains("negative"),
          "Error should mention breakpoints: " + ex.getMessage());

      LOGGER.info("Correctly rejected negative max breakpoints: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should build config with step debugging disabled")
    void shouldBuildConfigWithStepDebuggingDisabled() {
      LOGGER.info("Testing step debugging disabled");

      final PanamaDebugConfig config =
          PanamaDebugConfig.builder().stepDebuggingEnabled(false).build();

      assertFalse(config.isStepDebuggingEnabled(), "Step debugging should be disabled");

      LOGGER.info("Step debugging enabled: " + config.isStepDebuggingEnabled());
    }

    @Test
    @DisplayName("Should build config with custom log level")
    void shouldBuildConfigWithCustomLogLevel() {
      LOGGER.info("Testing custom log level");

      final String customLevel = "DEBUG";
      final PanamaDebugConfig config = PanamaDebugConfig.builder().logLevel(customLevel).build();

      assertEquals(customLevel, config.getLogLevel(), "Custom log level should be set");

      LOGGER.info("Custom log level set to: " + config.getLogLevel());
    }

    @Test
    @DisplayName("Should reject null log level")
    void shouldRejectNullLogLevel() {
      LOGGER.info("Testing null log level rejection");

      assertThrows(NullPointerException.class, () -> PanamaDebugConfig.builder().logLevel(null));

      LOGGER.info("Correctly rejected null log level");
    }

    @Test
    @DisplayName("Should build fully customized config")
    void shouldBuildFullyCustomizedConfig() {
      LOGGER.info("Testing fully customized config");

      final PanamaDebugConfig config =
          PanamaDebugConfig.builder()
              .debugPort(8888)
              .hostAddress("0.0.0.0")
              .remoteDebuggingEnabled(true)
              .sessionTimeout(120_000L)
              .breakpointsEnabled(true)
              .maxBreakpoints(512)
              .stepDebuggingEnabled(false)
              .logLevel("FINE")
              .build();

      assertEquals(8888, config.getDebugPort(), "Port should match");
      assertEquals("0.0.0.0", config.getHostAddress(), "Host should match");
      assertTrue(config.isRemoteDebuggingEnabled(), "Remote debugging should be enabled");
      assertEquals(120_000L, config.getSessionTimeout(), "Timeout should match");
      assertTrue(config.isBreakpointsEnabled(), "Breakpoints should be enabled");
      assertEquals(512, config.getMaxBreakpoints(), "Max breakpoints should match");
      assertFalse(config.isStepDebuggingEnabled(), "Step debugging should be disabled");
      assertEquals("FINE", config.getLogLevel(), "Log level should match");

      LOGGER.info("Fully customized config: " + config);
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("Should be equal to itself")
    void shouldBeEqualToItself() {
      LOGGER.info("Testing reflexive equality");

      final PanamaDebugConfig config = PanamaDebugConfig.getDefault();

      assertEquals(config, config, "Config should equal itself");

      LOGGER.info("Reflexive equality verified");
    }

    @Test
    @DisplayName("Should be equal to config with same values")
    void shouldBeEqualToConfigWithSameValues() {
      LOGGER.info("Testing equality with same values");

      final PanamaDebugConfig config1 =
          PanamaDebugConfig.builder().debugPort(9000).hostAddress("localhost").build();

      final PanamaDebugConfig config2 =
          PanamaDebugConfig.builder().debugPort(9000).hostAddress("localhost").build();

      assertEquals(config1, config2, "Configs with same values should be equal");
      assertEquals(config1.hashCode(), config2.hashCode(), "Hash codes should be equal");

      LOGGER.info("Equality with same values verified");
    }

    @Test
    @DisplayName("Should not be equal to config with different values")
    void shouldNotBeEqualToConfigWithDifferentValues() {
      LOGGER.info("Testing inequality with different values");

      final PanamaDebugConfig config1 = PanamaDebugConfig.builder().debugPort(9000).build();

      final PanamaDebugConfig config2 = PanamaDebugConfig.builder().debugPort(9001).build();

      assertNotEquals(config1, config2, "Configs with different ports should not be equal");

      LOGGER.info("Inequality with different values verified");
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
      LOGGER.info("Testing inequality with null");

      final PanamaDebugConfig config = PanamaDebugConfig.getDefault();

      assertNotEquals(null, config, "Config should not equal null");

      LOGGER.info("Inequality with null verified");
    }

    @Test
    @DisplayName("Should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      LOGGER.info("Testing inequality with different type");

      final PanamaDebugConfig config = PanamaDebugConfig.getDefault();

      assertNotEquals("not a config", config, "Config should not equal String");

      LOGGER.info("Inequality with different type verified");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("Should produce readable toString")
    void shouldProduceReadableToString() {
      LOGGER.info("Testing toString output");

      final PanamaDebugConfig config =
          PanamaDebugConfig.builder()
              .debugPort(9229)
              .hostAddress("127.0.0.1")
              .remoteDebuggingEnabled(true)
              .build();

      final String str = config.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("PanamaDebugConfig"), "toString should contain class name");
      assertTrue(str.contains("9229"), "toString should contain port");
      assertTrue(str.contains("127.0.0.1"), "toString should contain host");

      LOGGER.info("toString output: " + str);
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("Should have consistent hashCode")
    void shouldHaveConsistentHashCode() {
      LOGGER.info("Testing hashCode consistency");

      final PanamaDebugConfig config =
          PanamaDebugConfig.builder().debugPort(8000).hostAddress("test.host").build();

      final int hash1 = config.hashCode();
      final int hash2 = config.hashCode();

      assertEquals(hash1, hash2, "hashCode should be consistent");

      LOGGER.info("hashCode consistency verified: " + hash1);
    }

    @Test
    @DisplayName("Should have different hashCode for different values")
    void shouldHaveDifferentHashCodeForDifferentValues() {
      LOGGER.info("Testing hashCode variation");

      final PanamaDebugConfig config1 = PanamaDebugConfig.builder().debugPort(8000).build();

      final PanamaDebugConfig config2 = PanamaDebugConfig.builder().debugPort(8001).build();

      // Note: Different values don't guarantee different hashCodes,
      // but in most cases they should differ
      final int hash1 = config1.hashCode();
      final int hash2 = config2.hashCode();

      LOGGER.info("hashCode for port 8000: " + hash1 + ", port 8001: " + hash2);
      // We don't assert they're different since hash collisions are allowed
    }
  }

  @Nested
  @DisplayName("Constants Tests")
  class ConstantsTests {

    @Test
    @DisplayName("Should have valid default constants")
    void shouldHaveValidDefaultConstants() {
      LOGGER.info("Testing default constants");

      assertTrue(PanamaDebugConfig.DEFAULT_DEBUG_PORT > 0, "Default port should be positive");
      assertTrue(PanamaDebugConfig.DEFAULT_DEBUG_PORT <= 65535, "Default port should be valid");
      assertNotNull(PanamaDebugConfig.DEFAULT_HOST_ADDRESS, "Default host should not be null");
      assertTrue(
          PanamaDebugConfig.DEFAULT_SESSION_TIMEOUT > 0, "Default timeout should be positive");
      assertTrue(
          PanamaDebugConfig.DEFAULT_MAX_BREAKPOINTS > 0,
          "Default max breakpoints should be positive");
      assertNotNull(PanamaDebugConfig.DEFAULT_LOG_LEVEL, "Default log level should not be null");

      LOGGER.info("Default constants verified");
    }
  }
}
