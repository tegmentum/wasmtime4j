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

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link PanamaDebugConfig} class. */
@DisplayName("PanamaDebugConfig Tests")
public class PanamaDebugConfigTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaDebugConfigTest.class.getName());

  @Test
  @DisplayName("Create default configuration")
  public void testDefaultConfiguration() {
    LOGGER.info("Testing default configuration");

    final PanamaDebugConfig config = PanamaDebugConfig.getDefault();

    assertNotNull(config, "Config should not be null");
    assertEquals(
        PanamaDebugConfig.DEFAULT_DEBUG_PORT,
        config.getDebugPort(),
        "Debug port should match default");
    assertEquals(
        PanamaDebugConfig.DEFAULT_HOST_ADDRESS,
        config.getHostAddress(),
        "Host address should match default");
    assertFalse(
        config.isRemoteDebuggingEnabled(), "Remote debugging should be disabled by default");
    assertEquals(
        PanamaDebugConfig.DEFAULT_SESSION_TIMEOUT,
        config.getSessionTimeout(),
        "Session timeout should match default");
    assertTrue(config.isBreakpointsEnabled(), "Breakpoints should be enabled by default");
    assertEquals(
        PanamaDebugConfig.DEFAULT_MAX_BREAKPOINTS,
        config.getMaxBreakpoints(),
        "Max breakpoints should match default");
    assertTrue(config.isStepDebuggingEnabled(), "Step debugging should be enabled by default");
    assertEquals(
        PanamaDebugConfig.DEFAULT_LOG_LEVEL,
        config.getLogLevel(),
        "Log level should match default");

    LOGGER.info("Default configuration test passed: " + config);
  }

  @Test
  @DisplayName("Verify default constant values")
  public void testDefaultConstantValues() {
    LOGGER.info("Testing default constant values");

    assertEquals(9229, PanamaDebugConfig.DEFAULT_DEBUG_PORT, "Default debug port should be 9229");
    assertEquals(
        "127.0.0.1",
        PanamaDebugConfig.DEFAULT_HOST_ADDRESS,
        "Default host address should be 127.0.0.1");
    assertEquals(
        300_000L,
        PanamaDebugConfig.DEFAULT_SESSION_TIMEOUT,
        "Default session timeout should be 300000ms (5 minutes)");
    assertEquals(
        1024, PanamaDebugConfig.DEFAULT_MAX_BREAKPOINTS, "Default max breakpoints should be 1024");
    assertEquals("INFO", PanamaDebugConfig.DEFAULT_LOG_LEVEL, "Default log level should be INFO");

    LOGGER.info("Default constant values test passed");
  }

  @Test
  @DisplayName("Create configuration with custom debug port")
  public void testCustomDebugPort() {
    LOGGER.info("Testing custom debug port");

    final PanamaDebugConfig config = PanamaDebugConfig.builder().debugPort(8080).build();

    assertEquals(8080, config.getDebugPort(), "Debug port should be 8080");

    LOGGER.info("Custom debug port test passed");
  }

  @Test
  @DisplayName("Create configuration with custom host address")
  public void testCustomHostAddress() {
    LOGGER.info("Testing custom host address");

    final PanamaDebugConfig config = PanamaDebugConfig.builder().hostAddress("0.0.0.0").build();

    assertEquals("0.0.0.0", config.getHostAddress(), "Host address should be 0.0.0.0");

    LOGGER.info("Custom host address test passed");
  }

  @Test
  @DisplayName("Enable remote debugging")
  public void testEnableRemoteDebugging() {
    LOGGER.info("Testing remote debugging enabled");

    final PanamaDebugConfig config =
        PanamaDebugConfig.builder().remoteDebuggingEnabled(true).build();

    assertTrue(config.isRemoteDebuggingEnabled(), "Remote debugging should be enabled");

    LOGGER.info("Remote debugging enabled test passed");
  }

  @Test
  @DisplayName("Create configuration with custom session timeout")
  public void testCustomSessionTimeout() {
    LOGGER.info("Testing custom session timeout");

    final PanamaDebugConfig config = PanamaDebugConfig.builder().sessionTimeout(60000L).build();

    assertEquals(60000L, config.getSessionTimeout(), "Session timeout should be 60000ms");

    LOGGER.info("Custom session timeout test passed");
  }

  @Test
  @DisplayName("Disable breakpoints")
  public void testDisableBreakpoints() {
    LOGGER.info("Testing breakpoints disabled");

    final PanamaDebugConfig config = PanamaDebugConfig.builder().breakpointsEnabled(false).build();

    assertFalse(config.isBreakpointsEnabled(), "Breakpoints should be disabled");

    LOGGER.info("Breakpoints disabled test passed");
  }

  @Test
  @DisplayName("Create configuration with custom max breakpoints")
  public void testCustomMaxBreakpoints() {
    LOGGER.info("Testing custom max breakpoints");

    final PanamaDebugConfig config = PanamaDebugConfig.builder().maxBreakpoints(512).build();

    assertEquals(512, config.getMaxBreakpoints(), "Max breakpoints should be 512");

    LOGGER.info("Custom max breakpoints test passed");
  }

  @Test
  @DisplayName("Disable step debugging")
  public void testDisableStepDebugging() {
    LOGGER.info("Testing step debugging disabled");

    final PanamaDebugConfig config =
        PanamaDebugConfig.builder().stepDebuggingEnabled(false).build();

    assertFalse(config.isStepDebuggingEnabled(), "Step debugging should be disabled");

    LOGGER.info("Step debugging disabled test passed");
  }

  @Test
  @DisplayName("Create configuration with custom log level")
  public void testCustomLogLevel() {
    LOGGER.info("Testing custom log level");

    final PanamaDebugConfig config = PanamaDebugConfig.builder().logLevel("DEBUG").build();

    assertEquals("DEBUG", config.getLogLevel(), "Log level should be DEBUG");

    LOGGER.info("Custom log level test passed");
  }

  @Test
  @DisplayName("Create configuration with all custom values")
  public void testAllCustomValues() {
    LOGGER.info("Testing configuration with all custom values");

    final PanamaDebugConfig config =
        PanamaDebugConfig.builder()
            .debugPort(9999)
            .hostAddress("192.168.1.100")
            .remoteDebuggingEnabled(true)
            .sessionTimeout(120000L)
            .breakpointsEnabled(false)
            .maxBreakpoints(2048)
            .stepDebuggingEnabled(false)
            .logLevel("FINE")
            .build();

    assertEquals(9999, config.getDebugPort(), "Debug port should match");
    assertEquals("192.168.1.100", config.getHostAddress(), "Host address should match");
    assertTrue(config.isRemoteDebuggingEnabled(), "Remote debugging should be enabled");
    assertEquals(120000L, config.getSessionTimeout(), "Session timeout should match");
    assertFalse(config.isBreakpointsEnabled(), "Breakpoints should be disabled");
    assertEquals(2048, config.getMaxBreakpoints(), "Max breakpoints should match");
    assertFalse(config.isStepDebuggingEnabled(), "Step debugging should be disabled");
    assertEquals("FINE", config.getLogLevel(), "Log level should match");

    LOGGER.info("All custom values test passed: " + config);
  }

  @Test
  @DisplayName("Reject invalid debug port - negative")
  public void testRejectNegativeDebugPort() {
    LOGGER.info("Testing rejection of negative debug port");

    assertThrows(
        IllegalArgumentException.class,
        () -> PanamaDebugConfig.builder().debugPort(-1),
        "Should reject negative port");

    LOGGER.info("Negative debug port rejection test passed");
  }

  @Test
  @DisplayName("Reject invalid debug port - too high")
  public void testRejectTooHighDebugPort() {
    LOGGER.info("Testing rejection of too high debug port");

    assertThrows(
        IllegalArgumentException.class,
        () -> PanamaDebugConfig.builder().debugPort(65536),
        "Should reject port above 65535");

    LOGGER.info("Too high debug port rejection test passed");
  }

  @Test
  @DisplayName("Accept edge case debug ports")
  public void testEdgeCaseDebugPorts() {
    LOGGER.info("Testing edge case debug ports");

    final PanamaDebugConfig configMin = PanamaDebugConfig.builder().debugPort(0).build();
    assertEquals(0, configMin.getDebugPort(), "Port 0 should be accepted");

    final PanamaDebugConfig configMax = PanamaDebugConfig.builder().debugPort(65535).build();
    assertEquals(65535, configMax.getDebugPort(), "Port 65535 should be accepted");

    LOGGER.info("Edge case debug ports test passed");
  }

  @Test
  @DisplayName("Reject null host address")
  public void testRejectNullHostAddress() {
    LOGGER.info("Testing rejection of null host address");

    assertThrows(
        NullPointerException.class,
        () -> PanamaDebugConfig.builder().hostAddress(null),
        "Should reject null host address");

    LOGGER.info("Null host address rejection test passed");
  }

  @Test
  @DisplayName("Reject negative session timeout")
  public void testRejectNegativeSessionTimeout() {
    LOGGER.info("Testing rejection of negative session timeout");

    assertThrows(
        IllegalArgumentException.class,
        () -> PanamaDebugConfig.builder().sessionTimeout(-1),
        "Should reject negative timeout");

    LOGGER.info("Negative session timeout rejection test passed");
  }

  @Test
  @DisplayName("Accept zero session timeout")
  public void testAcceptZeroSessionTimeout() {
    LOGGER.info("Testing acceptance of zero session timeout");

    final PanamaDebugConfig config = PanamaDebugConfig.builder().sessionTimeout(0).build();
    assertEquals(0, config.getSessionTimeout(), "Zero timeout should be accepted");

    LOGGER.info("Zero session timeout test passed");
  }

  @Test
  @DisplayName("Reject negative max breakpoints")
  public void testRejectNegativeMaxBreakpoints() {
    LOGGER.info("Testing rejection of negative max breakpoints");

    assertThrows(
        IllegalArgumentException.class,
        () -> PanamaDebugConfig.builder().maxBreakpoints(-1),
        "Should reject negative max breakpoints");

    LOGGER.info("Negative max breakpoints rejection test passed");
  }

  @Test
  @DisplayName("Accept zero max breakpoints")
  public void testAcceptZeroMaxBreakpoints() {
    LOGGER.info("Testing acceptance of zero max breakpoints");

    final PanamaDebugConfig config = PanamaDebugConfig.builder().maxBreakpoints(0).build();
    assertEquals(0, config.getMaxBreakpoints(), "Zero max breakpoints should be accepted");

    LOGGER.info("Zero max breakpoints test passed");
  }

  @Test
  @DisplayName("Reject null log level")
  public void testRejectNullLogLevel() {
    LOGGER.info("Testing rejection of null log level");

    assertThrows(
        NullPointerException.class,
        () -> PanamaDebugConfig.builder().logLevel(null),
        "Should reject null log level");

    LOGGER.info("Null log level rejection test passed");
  }

  @Test
  @DisplayName("Test toString contains all fields")
  public void testToStringContainsFields() {
    LOGGER.info("Testing toString contains all fields");

    final PanamaDebugConfig config =
        PanamaDebugConfig.builder()
            .debugPort(8080)
            .hostAddress("localhost")
            .remoteDebuggingEnabled(true)
            .build();

    final String str = config.toString();

    assertTrue(str.contains("8080"), "toString should contain debug port");
    assertTrue(str.contains("localhost"), "toString should contain host address");
    assertTrue(str.contains("remoteDebugging"), "toString should contain remote debugging field");

    LOGGER.info("toString test passed: " + str);
  }

  @Test
  @DisplayName("Test equality for same configuration")
  public void testEquality() {
    LOGGER.info("Testing configuration equality");

    final PanamaDebugConfig config1 =
        PanamaDebugConfig.builder()
            .debugPort(9229)
            .hostAddress("127.0.0.1")
            .remoteDebuggingEnabled(true)
            .sessionTimeout(60000L)
            .build();

    final PanamaDebugConfig config2 =
        PanamaDebugConfig.builder()
            .debugPort(9229)
            .hostAddress("127.0.0.1")
            .remoteDebuggingEnabled(true)
            .sessionTimeout(60000L)
            .build();

    assertEquals(config1, config2, "Configs with same values should be equal");

    LOGGER.info("Equality test passed");
  }

  @Test
  @DisplayName("Test inequality for different configuration")
  public void testInequality() {
    LOGGER.info("Testing configuration inequality");

    final PanamaDebugConfig config1 = PanamaDebugConfig.builder().debugPort(8080).build();

    final PanamaDebugConfig config2 = PanamaDebugConfig.builder().debugPort(9229).build();

    assertNotEquals(config1, config2, "Configs with different ports should not be equal");

    LOGGER.info("Inequality test passed");
  }

  @Test
  @DisplayName("Test equals edge cases")
  public void testEqualsEdgeCases() {
    LOGGER.info("Testing equals edge cases");

    final PanamaDebugConfig config = PanamaDebugConfig.getDefault();

    assertTrue(config.equals(config), "Config should equal itself");
    assertFalse(config.equals(null), "Config should not equal null");
    assertFalse(config.equals("not a config"), "Config should not equal string");

    LOGGER.info("Equals edge cases test passed");
  }

  @Test
  @DisplayName("Test hashCode consistency")
  public void testHashCodeConsistency() {
    LOGGER.info("Testing hashCode consistency");

    final PanamaDebugConfig config1 =
        PanamaDebugConfig.builder().debugPort(8080).hostAddress("localhost").build();

    final PanamaDebugConfig config2 =
        PanamaDebugConfig.builder().debugPort(8080).hostAddress("localhost").build();

    assertEquals(config1.hashCode(), config2.hashCode(), "Equal configs should have same hashCode");

    LOGGER.info("hashCode consistency test passed");
  }

  @Test
  @DisplayName("Builder can be reused with different values")
  public void testBuilderReuse() {
    LOGGER.info("Testing builder reuse");

    final PanamaDebugConfig.Builder builder = PanamaDebugConfig.builder();

    final PanamaDebugConfig config1 = builder.debugPort(8080).build();
    final PanamaDebugConfig config2 = builder.debugPort(9229).build();

    assertEquals(8080, config1.getDebugPort(), "First config should have port 8080");
    assertEquals(9229, config2.getDebugPort(), "Second config should have port 9229");

    LOGGER.info("Builder reuse test passed");
  }
}
