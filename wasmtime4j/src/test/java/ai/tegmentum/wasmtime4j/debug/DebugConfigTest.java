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

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DebugConfig} interface.
 *
 * <p>DebugConfig provides configuration options for WebAssembly debugging sessions.
 */
@DisplayName("DebugConfig Tests")
class DebugConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(DebugConfig.class.isInterface(), "DebugConfig should be an interface");
    }

    @Test
    @DisplayName("should have getDebugPort method")
    void shouldHaveGetDebugPortMethod() throws NoSuchMethodException {
      final Method method = DebugConfig.class.getMethod("getDebugPort");
      assertNotNull(method, "getDebugPort method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getHostAddress method")
    void shouldHaveGetHostAddressMethod() throws NoSuchMethodException {
      final Method method = DebugConfig.class.getMethod("getHostAddress");
      assertNotNull(method, "getHostAddress method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have isRemoteDebuggingEnabled method")
    void shouldHaveIsRemoteDebuggingEnabledMethod() throws NoSuchMethodException {
      final Method method = DebugConfig.class.getMethod("isRemoteDebuggingEnabled");
      assertNotNull(method, "isRemoteDebuggingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getSessionTimeout method")
    void shouldHaveGetSessionTimeoutMethod() throws NoSuchMethodException {
      final Method method = DebugConfig.class.getMethod("getSessionTimeout");
      assertNotNull(method, "getSessionTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have isBreakpointsEnabled method")
    void shouldHaveIsBreakpointsEnabledMethod() throws NoSuchMethodException {
      final Method method = DebugConfig.class.getMethod("isBreakpointsEnabled");
      assertNotNull(method, "isBreakpointsEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMaxBreakpoints method")
    void shouldHaveGetMaxBreakpointsMethod() throws NoSuchMethodException {
      final Method method = DebugConfig.class.getMethod("getMaxBreakpoints");
      assertNotNull(method, "getMaxBreakpoints method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isStepDebuggingEnabled method")
    void shouldHaveIsStepDebuggingEnabledMethod() throws NoSuchMethodException {
      final Method method = DebugConfig.class.getMethod("isStepDebuggingEnabled");
      assertNotNull(method, "isStepDebuggingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getLogLevel method")
    void shouldHaveGetLogLevelMethod() throws NoSuchMethodException {
      final Method method = DebugConfig.class.getMethod("getLogLevel");
      assertNotNull(method, "getLogLevel method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock config should return debug port")
    void mockConfigShouldReturnDebugPort() {
      final MockDebugConfig config = new MockDebugConfig();
      assertEquals(9229, config.getDebugPort(), "Should return default debug port");
    }

    @Test
    @DisplayName("mock config should return host address")
    void mockConfigShouldReturnHostAddress() {
      final MockDebugConfig config = new MockDebugConfig();
      assertEquals("localhost", config.getHostAddress(), "Should return localhost");
    }

    @Test
    @DisplayName("mock config should return remote debugging status")
    void mockConfigShouldReturnRemoteDebuggingStatus() {
      final MockDebugConfig config = new MockDebugConfig();
      assertFalse(config.isRemoteDebuggingEnabled(), "Remote debugging should be disabled");
    }

    @Test
    @DisplayName("mock config should return session timeout")
    void mockConfigShouldReturnSessionTimeout() {
      final MockDebugConfig config = new MockDebugConfig();
      assertEquals(30000L, config.getSessionTimeout(), "Should return 30 second timeout");
    }

    @Test
    @DisplayName("mock config should return breakpoints enabled status")
    void mockConfigShouldReturnBreakpointsEnabled() {
      final MockDebugConfig config = new MockDebugConfig();
      assertTrue(config.isBreakpointsEnabled(), "Breakpoints should be enabled");
    }

    @Test
    @DisplayName("mock config should return max breakpoints")
    void mockConfigShouldReturnMaxBreakpoints() {
      final MockDebugConfig config = new MockDebugConfig();
      assertEquals(100, config.getMaxBreakpoints(), "Should return 100 max breakpoints");
    }

    @Test
    @DisplayName("mock config should return step debugging enabled status")
    void mockConfigShouldReturnStepDebuggingEnabled() {
      final MockDebugConfig config = new MockDebugConfig();
      assertTrue(config.isStepDebuggingEnabled(), "Step debugging should be enabled");
    }

    @Test
    @DisplayName("mock config should return log level")
    void mockConfigShouldReturnLogLevel() {
      final MockDebugConfig config = new MockDebugConfig();
      assertEquals("INFO", config.getLogLevel(), "Should return INFO log level");
    }
  }

  /** Mock implementation of DebugConfig for testing. */
  private static class MockDebugConfig implements DebugConfig {

    @Override
    public int getDebugPort() {
      return 9229;
    }

    @Override
    public String getHostAddress() {
      return "localhost";
    }

    @Override
    public boolean isRemoteDebuggingEnabled() {
      return false;
    }

    @Override
    public long getSessionTimeout() {
      return 30000L;
    }

    @Override
    public boolean isBreakpointsEnabled() {
      return true;
    }

    @Override
    public int getMaxBreakpoints() {
      return 100;
    }

    @Override
    public boolean isStepDebuggingEnabled() {
      return true;
    }

    @Override
    public String getLogLevel() {
      return "INFO";
    }
  }
}
